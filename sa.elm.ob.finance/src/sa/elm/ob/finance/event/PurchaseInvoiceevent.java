package sa.elm.ob.finance.event;

import java.math.BigDecimal;
import java.util.Date;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao.MultipleInvoiceLineAgainstPOLineDAO;
import sa.elm.ob.finance.actionHandler.PurchaseInvoiceSplit.SplitLine.dao.MultipleInvoiceLineAgainstPOLineDAOImpl;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerImpl;
import sa.elm.ob.utility.util.Utility;

public class PurchaseInvoiceevent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(InvoiceLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      InvoiceLine invoiceline = (InvoiceLine) event.getTargetInstance();
      Invoice invoice = invoiceline.getInvoice();
      TaxLineHandlerDAO dao = new TaxLineHandlerImpl();
      Boolean isTaxCalculated = Boolean.FALSE;
      BigDecimal lineAmount = BigDecimal.ZERO;
      final Property linenetAmt = entities[0].getProperty(InvoiceLine.PROPERTY_LINENETAMOUNT);
      final Property paymentBeneficiary = entities[0]
          .getProperty(InvoiceLine.PROPERTY_BUSINESSPARTNER);
      final Property secondaryBeneficiary = entities[0]
          .getProperty(InvoiceLine.PROPERTY_EFINSECONDARYBENEFICIARY);
      final Property recalculateTax = entities[0]
          .getProperty(InvoiceLine.PROPERTY_EFINRECALCULATETAX);
      final Property invoicedQty = entities[0].getProperty(InvoiceLine.PROPERTY_INVOICEDQUANTITY);
      final Property lineNetAmt = entities[0].getProperty(InvoiceLine.PROPERTY_LINENETAMOUNT);
      MultipleInvoiceLineAgainstPOLineDAO splitdao = new MultipleInvoiceLineAgainstPOLineDAOImpl();
      Boolean isError = false;
      BigDecimal oldLineNetAmt = BigDecimal.ZERO;
      BigDecimal newLineNetAmt = BigDecimal.ZERO;
      String invoiceid = invoice.getId();
      if (invoice.getEfinInvoicetypeTxt().equals("POM") && invoice.getEfinCOrder() != null
          && invoice.getEfinCOrder().getEscmReceivetype().equals("AMT")
          && !invoiceline.isEFINIsTaxLine()) {
        event.setCurrentState(lineNetAmt, invoiceline.getEfinAmtinvoiced());
        lineAmount = invoiceline.getEfinAmtinvoiced();
      } else {
        lineAmount = invoiceline.getLineNetAmount();
      }
      if (invoice.getBusinessPartner().equals(event.getCurrentState(paymentBeneficiary))) {
        // throw new OBException(OBMessageUtils.messageBD("efin_taxline_update"));
        if (event.getCurrentState(secondaryBeneficiary) == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_PurInvSecondBenfCannotBeEmpty"));
        }
      }
      if (!invoice.isEfinIstax() && !invoiceline.isEfinIssplitedLine()) { // (!invoiceline.isEfinIssplitedLine()
                                                                          // for TaskNo - 7493)
        if (!invoiceline.isEFINIsTaxLine()) {
          isTaxCalculated = dao.isTaxCalculated(invoiceline);
          if (isTaxCalculated && (((event.getCurrentState(paymentBeneficiary) != event
              .getPreviousState(paymentBeneficiary)))
              || (event.getCurrentState(secondaryBeneficiary) != event
                  .getPreviousState(secondaryBeneficiary)))) {
            throw new OBException(OBMessageUtils.messageBD("efin_tax_calculated"));
          }

          if (isTaxCalculated
              && (event.getCurrentState(invoicedQty) != event.getPreviousState(invoicedQty))) {
            throw new OBException(OBMessageUtils.messageBD("efin_tax_calculated"));
          }
        }
      }

      String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoiceline.getInvoice());

      if ("PPI".equals(strInvoiceType)) {
        if (!(lineAmount.compareTo(BigDecimal.ZERO) > 0)) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_PPI_Amount"));
        }
      }

      Date ActDate = null;

      if (!StringUtils.isEmpty(invoiceid)) {
        Invoice purchase = OBDal.getInstance().get(Invoice.class, invoiceid);
        if (purchase != null) {
          ActDate = purchase.getInvoiceDate();
          purchase.isEfinIsrdv();
        }
      }

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
          "select * from efin_buget_process ( ?,?,?,?,?,?,?,?,to_date(?,'dd-MM-yyyy'), ?)");
      query.setParameter(0, invoiceline.getOrganization().getId());
      query.setParameter(1, invoiceline.getEfinCElementvalue().getId());
      query.setParameter(2, invoiceline.getProject().getId());
      query.setParameter(3, invoiceline.getEfinCSalesregion().getId());
      query.setParameter(4, invoiceline.getEfinCCampaign().getId());
      query.setParameter(5, invoiceline.getEfinCActivity().getId());
      query.setParameter(6, invoiceline.getStDimension().getId());
      query.setParameter(7, invoiceline.getNdDimension().getId());
      query.setParameter(8, Utility.formatDate(ActDate));
      query.setParameter(9, OBContext.getOBContext().getCurrentClient().getId());

      // check negative value in case of ap prepayment invoice
      oldLineNetAmt = (BigDecimal) event.getPreviousState(linenetAmt);
      newLineNetAmt = (BigDecimal) event.getCurrentState(linenetAmt);
      if (oldLineNetAmt.compareTo(newLineNetAmt) != 0) {

        if (invoiceline.getInvoice().getTransactionDocument().isEfinIsprepayinv()) {
          if (invoiceline.getLineNetAmount().signum() == -1) {
            throw new OBException(OBMessageUtils.messageBD("Efin_PurIn_NegValAPP"));
          }
        }

        if (!invoice.isProcessNow())
          event.setCurrentState(recalculateTax, Boolean.TRUE);
      }
      // invoice.setProcessNow(Boolean.FALSE);
      // OBDal.getInstance().save(invoice);

      /*
       * Checking this constraint during Submit process (issue #6552) // check Constraint based
       * uniquecode-businesspartner-amount(with one +ve and one -ve) Property isTaxLine =
       * entities[0].getProperty(InvoiceLine.PROPERTY_EFINISTAXLINE); if (!isRdv &&
       * event.getCurrentState(isTaxLine).equals(Boolean.FALSE)) {
       * 
       * List<InvoiceLine> invoiceLineList = invoiceline.getInvoice().getInvoiceLineList().stream()
       * .filter(a -> a.getEfinCValidcombination().equals(invoiceline.getEfinCValidcombination()) &&
       * (a.isEFINIsTaxLine() || a.getInvoice().isSalesTransaction()))
       * .collect(Collectors.toList());
       * 
       * for (InvoiceLine line : invoiceLineList) {
       * 
       * if (invoiceline.getBusinessPartner() == line.getBusinessPartner() &&
       * invoiceline.getEfinSecondaryBeneficiary() == line.getEfinSecondaryBeneficiary()) {
       * 
       * if (line.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) { posCnt = posCnt + 1; } else {
       * negCnt = negCnt + 1; }
       * 
       * if (posCnt > 1 || negCnt > 1) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_Nonexp_uniquecode")); }
       * 
       * } }
       * 
       * }
       */
      if (invoiceline.getLineNetAmount().compareTo(new BigDecimal(0)) == 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Amount_Zero"));
      }

      // adding manencumlineid in line
      if (!invoiceline.getInvoice().isSalesTransaction()) {
        if (invoiceline.getInvoice().getEfinEncumtype().equals("M")) {
          final Property lineencumid = entities[0]
              .getProperty(InvoiceLine.PROPERTY_EFINBUDGMANUENCUMLN);
          String uniqueCode = invoiceline.getEFINUniqueCode();
          String manualId = invoiceline.getInvoice().getEfinManualencumbrance().getId();
          OBQuery<EfinBudgetManencumlines> manualline2 = OBDal.getInstance().createQuery(
              EfinBudgetManencumlines.class,
              " uniquecode ='" + uniqueCode + "' and efin_budget_manencum_id = '" + manualId + "'");
          if (!(manualline2.list() == null || manualline2.list().size() == 0)) {
            event.setCurrentState(lineencumid, manualline2.list().get(0));
          }
        }
      }

      if (invoiceline.getInvoice().isSalesTransaction()) {

        if ((invoiceline.getEfinCElementvalue().getAccountType().equals("E"))) {

          BigDecimal funds = invoiceline.getEFINFundsAvailable();
          if (invoiceline.getLineNetAmount().signum() == -1) {
            if (invoiceline.getLineNetAmount().abs().compareTo(funds) > 0) {
              throw new OBException(OBMessageUtils.messageBD("Efin_funds"));
            } else if (invoiceline.getLineNetAmount().abs().compareTo(funds) < 0) {

            }

          }
        }
      }

      if (invoice.getDocumentStatus().equals("DR")
          && PurchaseInvoiceSubmitUtils.PO_DOCUMENT.equals(strInvoiceType)) {

        // FOR PO MAtch should not allow to insert qty more than order.
        if (invoiceline.isEfinIspom() && invoice.getEfinInvoicetypeTxt().equals("QTY")
            && invoiceline.getInvoicedQuantity()
                .compareTo(invoiceline.getSalesOrderLine().getOrderedQuantity()
                    .subtract(invoiceline.getSalesOrderLine().getInvoicedQuantity())) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Cant_Inc_POQty"));
        }

        // FOR PO MAtch should not allow to insert qty more than order.
        if (invoiceline.isEfinIspom() && invoice.getEfinInvoicetypeTxt().equals("AMT")
            && invoiceline.getEfinAmtinvoiced()
                .compareTo(invoiceline.getSalesOrderLine().getLineNetAmount()
                    .subtract(invoiceline.getSalesOrderLine().getEfinAmtinvoiced())) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Cant_Inc_POAmt"));
        }

        // FOR PO MAtch should not allow to insert -ve qty.
        if (invoiceline.isEfinIspom() && invoice.getEfinInvoicetypeTxt().equals("QTY")
            && invoiceline.getInvoicedQuantity().compareTo(BigDecimal.ZERO) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Qty_Negative"));
        }

        // FOR PO MAtch should not allow to insert -ve qty.cal
        if (invoiceline.isEfinIspom() && invoice.getEfinInvoicetypeTxt().equals("AMT")
            && invoiceline.getEfinAmtinvoiced().compareTo(BigDecimal.ZERO) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Amt_Negative"));
        }
      }

      // check already line is exist with same payment beneficiary and secondary beneficiary -
      // TaskNo - 7493
      if (invoice.getDocumentStatus().equals("DR") && invoiceline.getBusinessPartner() != null
          && invoiceline.getEfinSecondaryBeneficiary() != null
          && invoiceline.getSalesOrderLine() != null) {
        isError = splitdao.checkAlreadyBPCombinationExist(invoiceline.getBusinessPartner().getId(),
            invoiceline.getEfinSecondaryBeneficiary().getId(), invoiceline.getClient().getId(),
            invoiceline.getSalesOrderLine().getId(), invoiceline.getInvoice().getId(),
            invoiceline.getId());
        if (isError) {
          throw new OBException(OBMessageUtils.messageBD("Efin_Unique_Beneficiary"));
        }
      }
      // end TaskNo - 7493

    } catch (OBException e) {
      log.error(" Exception while updating record purchase invoice line: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      InvoiceLine invoiceLine = (InvoiceLine) event.getTargetInstance();
      final Property lineNetAmt = entities[0].getProperty(InvoiceLine.PROPERTY_LINENETAMOUNT);
      Invoice invoice = invoiceLine.getInvoice();
      final Property paymentBeneficiary = entities[0]
          .getProperty(InvoiceLine.PROPERTY_BUSINESSPARTNER);
      final Property secondaryBeneficiary = entities[0]
          .getProperty(InvoiceLine.PROPERTY_EFINSECONDARYBENEFICIARY);
      if (invoice.getBusinessPartner().equals(event.getCurrentState(paymentBeneficiary))) {
        if (event.getCurrentState(secondaryBeneficiary) == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_PurInvSecondBenfCannotBeEmpty"));
        }
      }
      if (!invoiceLine.getInvoice().isSalesTransaction()
          && invoiceLine.getInvoice().isEfinCreatedfromsadad() != null
          && !invoiceLine.getInvoice().isEfinCreatedfromsadad()) {

        String invoiceid = invoiceLine.getInvoice().getId();
        String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoiceLine.getInvoice());

        if (invoiceLine.getInvoice().getEfinInvoicetypeTxt().equals("POM")
            && invoiceLine.getInvoice().getEfinCOrder() != null
            && invoiceLine.getInvoice().getEfinCOrder().getEscmReceivetype().equals("AMT")) {
          event.setCurrentState(lineNetAmt, invoiceLine.getEfinAmtinvoiced());
          invoiceLine.setLineNetAmount(invoiceLine.getEfinAmtinvoiced());
        }

        if ("PPI".equals(strInvoiceType)) {
          if (!(invoiceLine.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0)) {
            throw new OBException(OBMessageUtils.messageBD("EFIN_PPI_Amount"));
          }
        }
        Date ActDate = null;
        if (!StringUtils.isEmpty(invoiceid)) {
          Invoice purchase = OBDal.getInstance().get(Invoice.class, invoiceid);
          ActDate = purchase.getInvoiceDate();
          purchase.isEfinIsrdv();

        }
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
            "select * from efin_buget_process ( ?,?,?,?,?,?,?,?,to_date(?,'dd-MM-yyyy'), ?)");
        query.setParameter(0, invoiceLine.getOrganization().getId());
        query.setParameter(1, invoiceLine.getEfinCElementvalue().getId());
        query.setParameter(2, invoiceLine.getProject().getId());
        query.setParameter(3, invoiceLine.getEfinCSalesregion().getId());
        query.setParameter(4, invoiceLine.getEfinCCampaign().getId());
        query.setParameter(5, invoiceLine.getEfinCActivity().getId());
        query.setParameter(6, invoiceLine.getStDimension().getId());
        query.setParameter(7, invoiceLine.getNdDimension().getId());
        query.setParameter(8, Utility.formatDate(ActDate));
        query.setParameter(9, OBContext.getOBContext().getCurrentClient().getId());

        // check negative value in case of ap prepayment invoice
        if (invoiceLine.getInvoice().getTransactionDocument().isEfinIsprepayinv()) {
          if (invoiceLine.getLineNetAmount().signum() == -1) {
            throw new OBException(OBMessageUtils.messageBD("Efin_PurIn_NegValAPP"));
          }
        }
        /*
         * Checking this constraint during Submit process (issue #6552) Property isTaxLine =
         * entities[0].getProperty(InvoiceLine.PROPERTY_EFINISTAXLINE); if (!isRdv &&
         * event.getCurrentState(isTaxLine).equals(Boolean.FALSE)) {
         * 
         * if (bpartner != null) {
         * 
         * String strWhereClause = " eFINUniqueCode='" + invoiceLine.getEFINUniqueCode() +
         * "' and businessPartner.id = '" + bpartner + "' and invoice.id='" +
         * invoiceLine.getInvoice().getId() + "'";
         * 
         * if (invoiceLine.getEfinSecondaryBeneficiary() != null) strWhereClause = strWhereClause +
         * "  and efinSecondaryBeneficiary.id =:secondaryBeneficiary  ";
         * 
         * OBQuery<InvoiceLine> duplicate = OBDal.getInstance().createQuery(InvoiceLine.class,
         * strWhereClause);
         * 
         * if (invoiceLine.getEfinSecondaryBeneficiary() != null)
         * duplicate.setNamedParameter("secondaryBeneficiary",
         * invoiceLine.getEfinSecondaryBeneficiary() == null ? "" :
         * invoiceLine.getEfinSecondaryBeneficiary().getId());
         * 
         * List<InvoiceLine> invLineList = duplicate.list(); if (invLineList.size() > 0) { for
         * (InvoiceLine invLine : invLineList) { if (invLine.getLineNetAmount().compareTo(new
         * BigDecimal("0")) < 0) { negCnt = negCnt + 1; } else { posCnt = posCnt + 1; }
         * 
         * if (invoiceLine.getBusinessPartner() != null) { if
         * (invoiceLine.getLineNetAmount().compareTo(new BigDecimal("0")) < 0) { negCnt = negCnt +
         * 1; } else { posCnt = posCnt + 1; } } } if (posCnt > 1 || negCnt > 1) { throw new
         * OBException(OBMessageUtils.messageBD("Efin_Nonexp_uniquecode")); } } } if
         * (bpartner.equals("")) {
         * 
         * OBQuery<InvoiceLine> duplicate = OBDal.getInstance().createQuery(InvoiceLine.class,
         * " eFINUniqueCode='" + invoiceLine.getEFINUniqueCode() + "' and  invoice.id='" +
         * invoiceLine.getInvoice().getId() + "' and businessPartner.id is null"); List<InvoiceLine>
         * invLineList = duplicate.list(); if (invLineList.size() > 0) { for (InvoiceLine invLine :
         * invLineList) { if (invLine.getLineNetAmount().compareTo(new BigDecimal("0")) < 0) {
         * negCnt = negCnt + 1; } else { posCnt = posCnt + 1; }
         * 
         * if (invoiceLine.getBusinessPartner() == null) { if
         * (invoiceLine.getLineNetAmount().compareTo(new BigDecimal("0")) < 0) { negCnt = negCnt +
         * 1; } else { posCnt = posCnt + 1; } } } if (posCnt > 1 || negCnt > 1) { throw new
         * OBException(OBMessageUtils.messageBD("Efin_Nonexp_uniquecode")); } } }
         * 
         * }
         */
        if (invoiceLine.getLineNetAmount().compareTo(new BigDecimal(0)) == 0
            && !invoiceLine.isEfinIssplitedLine()) { // //(!invoiceline.isEfinIssplitedLine() for
                                                     // TaskNo - 7493)
          throw new OBException(OBMessageUtils.messageBD("Efin_Amount_Zero"));
        }

        /*
         * if(BudgetLineId == null) { OBQuery<EfinNonexpenseLines> duplicate1 =
         * OBDal.getInstance().createQuery(EfinNonexpenseLines.class, " uniqueCode='" +
         * invoiceLine.getEFINUniqueCode() + "'");
         * 
         * if(duplicate1.list() == null || duplicate1.list().size() == 0) { throw new
         * OBException(OBMessageUtils.messageBD("Efin_Gljournal_check")); } }
         */

        /*
         * if (!invoiceLine.getInvoice().isSalesTransaction()) { if
         * (!invoiceLine.getInvoice().getEfinEncumtype().equals("M")) { if
         * ((!invoiceLine.getEfinCElementvalue().getAccountType().equals("E") &&
         * !invoiceLine.getEfinCElementvalue().getAccountType().equals("R")) &&
         * (invoiceLine.getEfinAcctBeneficiary() == null ||
         * invoiceLine.getEfinAcctBeneficiary().equals(""))) { throw new
         * OBException(OBMessageUtils.messageBD("Efin_GLJournalLine_AcctBeneficiary")); } } }
         */
        // adding manencum line id in line
        if (!invoiceLine.getInvoice().isSalesTransaction()) {
          if (invoiceLine.getInvoice().getEfinEncumtype().equals("M")) {
            final Property lineencumid = entities[0]
                .getProperty(InvoiceLine.PROPERTY_EFINBUDGMANUENCUMLN);

            String uniqueCode = invoiceLine.getEFINUniqueCode();
            String manualId = invoiceLine.getInvoice().getEfinManualencumbrance().getId();
            OBQuery<EfinBudgetManencumlines> manualline1 = OBDal.getInstance()
                .createQuery(EfinBudgetManencumlines.class, " uniquecode ='" + uniqueCode
                    + "' and efin_budget_manencum_id = '" + manualId + "'");

            if (!(manualline1.list() == null || manualline1.list().size() == 0)) {
              event.setCurrentState(lineencumid, manualline1.list().get(0));
            }
          }
        }
        // Don't allow to add prepayment unique codes (Unique codes which are added in any
        // distribution set) in AP Invoice, AP Prepayment Application.
        if (!invoiceLine.getInvoice().isSalesTransaction()) {
          if ((!invoiceLine.getInvoice().getTransactionDocument().isEfinIsprepayinv()
              && !invoiceLine.getInvoice().getTransactionDocument().isEfinIsprepayinvapp())
              || invoiceLine.getInvoice().getTransactionDocument().isEfinIsprepayinvapp()) {
            OBQuery<InvoiceLine> lines = OBDal.getInstance().createQuery(InvoiceLine.class,
                "efinDistributionLines is not null and eFINUniqueCode='"
                    + invoiceLine.getEFINUniqueCode() + "'");
            if (lines.list().size() > 0) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Uniquecode_Distribution"));
            }
          }
        }
        // checking the unique code present in manual lines
        /*
         * if(invoiceLine.getEfinCElementvalue().getAccountType().equals("E")) {
         * if(invoiceLine.getInvoice().getEfinEncumtype().equals("M")) { String uniqueCode =
         * invoiceLine.getEFINUniqueCode();
         * 
         * String manualId = invoiceLine.getInvoice().getEfinManualencumbrance().getId();
         * OBQuery<EfinBudgetManencumlines> manualline =
         * OBDal.getInstance().createQuery(EfinBudgetManencumlines.class, " uniquecode ='" +
         * uniqueCode + "' and efin_budget_manencum_id = '" + manualId + "'");
         * log.debug("sizeif"+manualline.list() == null || manualline.list().size() == 0);
         * 
         * if(manualline.list() == null || manualline.list().size() == 0) { throw new
         * OBException("@Efin_LineNotExistsIn_EncumLine@"); } } }
         */

        // checking acct benifieciary mandatory for ap invoice and non expense acct
        /*
         * if (!invoiceLine.getInvoice().isSalesTransaction()) {
         * 
         * if ((!invoiceLine.getEfinCElementvalue().getAccountType().equals("E") &&
         * !invoiceLine.getInvoice().getTransactionDocument().isEfinIsprepayinv() &&
         * !invoiceLine.getEfinCElementvalue().getAccountType().equals("R")) &&
         * (invoiceLine.getEfinAcctBeneficiary() == null ||
         * invoiceLine.getEfinAcctBeneficiary().equals(""))) { throw new
         * OBException(OBMessageUtils.messageBD("Efin_GLJournalLine_AcctBeneficiary")); } }
         */
        if (invoiceLine.getInvoice().isSalesTransaction()) {

          if ((invoiceLine.getEfinCElementvalue().getAccountType().equals("E"))) {

            BigDecimal funds = invoiceLine.getEFINFundsAvailable();
            if (invoiceLine.getLineNetAmount().signum() == -1) {
              if (invoiceLine.getLineNetAmount().abs().compareTo(funds) > 0) {
                throw new OBException(OBMessageUtils.messageBD("Efin_funds"));
              } else if (invoiceLine.getLineNetAmount().abs().compareTo(funds) < 0) {

              }

            }
          }
        }
      }
    } catch (OBException e) {
      e.printStackTrace();
      log.error(" Exception while creating invoiceline: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode(true);
      InvoiceLine invoiceLine = (InvoiceLine) event.getTargetInstance();
      String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoiceLine.getInvoice());
      // we should not allow to delete lines if from rdv.
      if (invoiceLine.getInvoice().isEfinIsrdv() && !invoiceLine.isEFINIsTaxLine()
          || (invoiceLine.getInvoice().isEfinIsrdv() && invoiceLine.isEFINIsTaxLine())) {
        throw new OBException(OBMessageUtils.messageBD("Efin_RdvInv_Delete"));
      }

      // if ((invoiceLine.getInvoice().isEfinIstaxpo() || strInvoiceType.equals("POM"))
      // && invoiceLine.isEFINIsTaxLine()) {
      // throw new OBException(OBMessageUtils.messageBD("Efin_invoicetax_dele"));
      // }

    } catch (Exception e) {
      log.error(" Exception while delete invoiceline: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
