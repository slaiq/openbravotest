package sa.elm.ob.finance.event;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.sales.SalesRegion;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinPropertyCompensation;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.actionHandler.InvoiceRevokeDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.utility.util.Utility;

public class PuchaseInvoiceHeaderEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };

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
      Invoice invoice = (Invoice) event.getTargetInstance();
      final Property encumTyp = entities[0].getProperty(Invoice.PROPERTY_EFINENCUMTYPE);
      final Property dept = entities[0].getProperty(Invoice.PROPERTY_EFINCSALESREGION);
      final Property encumbrance = entities[0].getProperty(Invoice.PROPERTY_EFINMANUALENCUMBRANCE);

      if (invoice.getEfinBudgetint() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Budget_Init_Mandatory"));
      }

      // Event has execute only if it is not sales transaction(ISSotrx ="N") -- ONly for Purchase
      // invoice
      if (!invoice.isSalesTransaction()) {
        final String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
        final Boolean isSalesInvoice = invoice.isSalesTransaction();

        if ("PPI".equals(strInvoiceType)) {
          if (invoice.getEfinDistribution() == null) {
            throw new OBException(OBMessageUtils.messageBD("Efin_AdvanceType_NotAvailable"));
          } else if (invoice.getEfinManualencumbrance() == null
              && "M".equals(invoice.getEfinEncumtype())) {
            throw new OBException(OBMessageUtils.messageBD("Efin_PurInv_ManEncMan"));
          }
        }
        if (!isSalesInvoice && !"POM".equals(strInvoiceType)) {
          if (event.getPreviousState(encumTyp).equals("M")
              && event.getCurrentState(encumTyp).equals("A")
              && invoice.getEfinManualencumbrance() != null) {
            event.setCurrentState(encumbrance, null);
          }
        }
        if (!isSalesInvoice) {
          if ((!invoice.getTransactionDocument().isEfinIsprepayinv()
              && !invoice.getTransactionDocument().isEfinIsprepayinvapp()
              && !invoice.getTransactionDocument().isEfinIsrdvinv())
              || invoice.getTransactionDocument().isEfinIsprepayinvapp()) {
            if (invoice.getEfinEncumtype().equals("M")
                && invoice.getEfinManualencumbrance() == null) {
              throw new OBException(OBMessageUtils.messageBD("Efin_PurInv_SelectManEncum"));
            }
          }

          if (invoice.getTransactionDocument().isEfinIsprepayinvapp()) {
            if (invoice.getEfinManualencumbrance() == null) {
              throw new OBException(OBMessageUtils.messageBD("Efin_PurInv_SelectManEncum"));
            }
          }
        }
        if (!isSalesInvoice) {
          if ((!(event.getPreviousState(dept).equals(event.getCurrentState(dept))))
              || (!(event.getPreviousState(encumTyp).equals(event.getCurrentState(encumTyp))))) {
            OBQuery<SalesRegion> header = OBDal.getInstance().createQuery(SalesRegion.class,
                "  id ='" + invoice.getEfinCSalesregion().getId() + "' and isDefault='Y'");
            if (header.list().size() > 0 && (!invoice.getEfinEncumtype().equals("N"))) {
              throw new OBException(OBMessageUtils.messageBD("Efin_DefaultDeptRestrict"));
            }

          }
        }
        // if encumburance method is manual then budget type is mandatory

        if (invoice.getEfinEncumtype().equals("M")) {
          if (StringUtils.isEmpty(invoice.getEfinBudgetType()))
            throw new OBException(OBMessageUtils.messageBD("Efin_BudgetTypeMan"));
        }

        // advance category mandatory for prepayment inv.
        if (invoice.getTransactionDocument().isEfinIsprepayinv()) {
          if (invoice.getEfinAdvancecategory() == null) {
            throw new OBException(OBMessageUtils.messageBD("Efin_AdvCat_Man"));
          }
        }
      }

    } catch (OBException e) {
      log.error(" Exception while updating record in Purchase Invoice: " + e);
      throw new OBException(e.getMessage());
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
      List<EfinBudgetControlParam> budgCtrlParamList = null;
      Invoice invoice = (Invoice) event.getTargetInstance();
      final Property encumbrance = entities[0].getProperty(Invoice.PROPERTY_EFINMANUALENCUMBRANCE);
      final Property ispaymentSchedule = entities[0].getProperty(Invoice.PROPERTY_EFINISPAYMENTSCH);
      final String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
      final Boolean isSalesInvoice = invoice.isSalesTransaction();
      String hqOrgId = null;
      // find order
      if (invoice.getEfinCOrder() != null) {
        Order objOriginalOrder = OBDal.getInstance().get(Order.class,
            invoice.getEfinCOrder().getId());
        if (objOriginalOrder != null && objOriginalOrder.isEscmIspaymentschedule()) {
          event.setCurrentState(ispaymentSchedule, true);
        } else {
          event.setCurrentState(ispaymentSchedule, false);
        }
      }
      // Task no:8055
      /*
       * if (strInvoiceType != null && strInvoiceType.equals("PPI") || strInvoiceType.equals("API")
       * || strInvoiceType.equals("PPA")) { // get HQ Org OBQuery<EfinBudgetControlParam>
       * budgCtrlParam = OBDal.getInstance() .createQuery(EfinBudgetControlParam.class,
       * " as a where a.client.id=:clientId "); budgCtrlParam.setNamedParameter("clientId",
       * invoice.getClient().getId()); budgCtrlParamList = budgCtrlParam.list(); if
       * (budgCtrlParamList.size() > 0) { Organization hqOrg =
       * budgCtrlParamList.get(0).getAgencyHqOrg(); hqOrgId = hqOrg.getId(); } if (hqOrgId != null
       * && !invoice.getOrganization().getId().equals(hqOrgId)) { throw new
       * OBException(OBMessageUtils.messageBD("EFIN_InvoiceNotAllow_RegionLevel")); } }
       */
      // Event has execute only if it is not sales transaction(ISSotrx ="N") -- ONly for Purchase
      // invoice

      // Task No : 7541 - Throw error if PO is in status Withdrawn / Hold
      if (invoice.getEfinCOrder() != null) {
        Order objOrder = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());
        if ("ESCM_WD".equals(objOrder.getEscmAppstatus())
            || "ESCM_OHLD".equals(objOrder.getEscmAppstatus())) {
          throw new OBException(OBMessageUtils.messageBD("EUT_HoldWithdrawnPO"));
        }
      }

      if (invoice.getEfinBudgetint() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Budget_Init_Mandatory"));
      }

      if (!invoice.isSalesTransaction()) {

        if ("PPI".equals(strInvoiceType)) {
          if (invoice.getEfinDistribution() == null) {
            throw new OBException(OBMessageUtils.messageBD("Efin_AdvanceType_NotAvailable"));
          } else if (invoice.getEfinManualencumbrance() == null
              && "M".equals(invoice.getEfinEncumtype())) {
            throw new OBException(OBMessageUtils.messageBD("Efin_PurInv_ManEncMan"));
          }
        }

        if (!isSalesInvoice) {
          if (!("POM".equals(strInvoiceType))) {
            if (invoice.getEfinEncumtype().equals("A") && !("PPA".equals(strInvoiceType))) {
              event.setCurrentState(encumbrance, null);
            }
          }
        }

        if (!isSalesInvoice) {
          if (("API".equals(strInvoiceType)) || "PPA".equals(strInvoiceType)) {
            if (invoice.getEfinEncumtype().equals("M")
                && invoice.getEfinManualencumbrance() == null) {
              throw new OBException(OBMessageUtils.messageBD("Efin_PurInv_SelectManEncum"));
            }
          }
          if (invoice.getTransactionDocument().isEfinIsprepayinvapp()) {
            if (invoice.getEfinManualencumbrance() == null) {
              throw new OBException(OBMessageUtils.messageBD("Efin_PurInv_SelectManEncum"));
            }
          }
        }

        // Added condition for default dept restriction for transaction
        if (!isSalesInvoice) {
          OBQuery<SalesRegion> header1 = OBDal.getInstance().createQuery(SalesRegion.class,
              "  id ='" + invoice.getEfinCSalesregion().getId() + "' and isDefault='Y'");

          if (header1.list().size() > 0 && (!invoice.getEfinEncumtype().equals("N"))) {
            throw new OBException(OBMessageUtils.messageBD("Efin_DefaultDeptRestrict"));
          }
        }

        // if encumburance method is manual then budget type is mandatory
        if (invoice.getEfinEncumtype().equals("M")) {
          if (StringUtils.isEmpty(invoice.getEfinBudgetType()))
            throw new OBException(OBMessageUtils.messageBD("Efin_BudgetTypeMan"));
        }

        // advance category mandatory for prepayment inv.
        if (invoice.getTransactionDocument().isEfinIsprepayinv()) {
          if (invoice.getEfinAdvancecategory() == null) {
            throw new OBException(OBMessageUtils.messageBD("Efin_AdvCat_Man"));
          }
        }
      }
    }

    catch (OBException e) {
      e.printStackTrace();
      log.error(" Exception while creating record in Purchase Invoice: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    String AccountDate = "";
    String seqName = null;

    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      EfinRDVTransaction version = null;
      EfinRDVTxnline txnlineObj = null;
      String encumid = "";
      List<EfinBudManencumRev> newEncumModRev = new ArrayList<EfinBudManencumRev>();
      Invoice inv = (Invoice) event.getTargetInstance();
      // Event has execute only if it is not sales transaction(ISSotrx ="N") -- ONly for Purchase
      // invoice
      String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(inv);

     if (!inv.isSalesTransaction()) {
// Update the Property Comp invoice reference as null
      if (inv.getDocumentStatus().equals("DR")
          && inv.getEfinPropertyCompensationList().size() > 0) {
        EfinPropertyCompensation objPropertyCompensation = inv.getEfinPropertyCompensationList()
            .get(0);
        objPropertyCompensation.setInvoice(null);

      }
        final Property propencum = entities[0].getProperty(Invoice.PROPERTY_EFINMANUALENCUMBRANCE);
       if (inv.getDocumentStatus().equals("DR") && !strInvoiceType.equals("POM")
            && !strInvoiceType.equals("RDV")){
          OBQuery<InvoiceLine> line = OBDal.getInstance().createQuery(InvoiceLine.class,
              "invoice.id='" + inv.getId() + "'");
          if (line.list().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Efin_PurInv_CantDelHead"));
          }
        }

        if (inv.getDocumentStatus().equals("DR") && inv.isEfinIsrdv()) {
          if (inv.getEfinRdvtxn() != null && inv.getEfinRdvtxn().isAdvancetransaction()) {

            OBQuery<EfinRDVTxnline> line = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
                "efinRdvtxn.id='" + inv.getEfinRdvtxn().getId() + "' " + " and trxlnNo > "
                    + (inv.getEfinRdvtxnline() == null ? "0" : inv.getEfinRdvtxnline().getTrxlnNo())
                    + " and isadvance='Y' ");
            if (line.list().size() > 0) {
              throw new OBException(OBMessageUtils.messageBD("Efin_PurinvCantDeleteNextAdvance"));
            }

          }
        }
        if (inv.isEfinIsrdv() && inv.getDocumentStatus().equals("DR")) {
          if (inv.getInvoiceLineList().size() > 0) {
            OBQuery<InvoiceLine> taxLine = OBDal.getInstance().createQuery(InvoiceLine.class,
                " as e where e.invoice.id='" + inv.getId() + "' and e.eFINIsTaxLine='Y'");
            if (taxLine.list().size() > 0) {
              InvoiceLine ln = taxLine.list().get(0);
              ln.setEfinCInvoiceline(null);
              OBDal.getInstance().save(ln);
            }
          }
          // if encum splitted and associated then reverse all
          if (inv.getEfinManualencumbrance() != null) {
            if (inv.getEfinRdvtxn().getEfinRdv().getSalesOrder().getEfinBudgetManencum()
                .getEncumStage().equals("POE")) {
              EfinBudgetManencum poEncumbrance = inv.getEfinRdvtxn().getEfinRdv().getSalesOrder()
                  .getEfinBudgetManencum();

              // splitted enum
              encumid = inv.getEfinManualencumbrance().getId();
              EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class, encumid);

              if (encumbrance != null) {
                OBQuery<InvoiceLine> invLine = OBDal.getInstance().createQuery(InvoiceLine.class,
                    "invoice.id='" + inv.getId() + "' and efinBudgmanuencumln.id is not null ");
                List<InvoiceLine> invlineList = invLine.list();
                if (invlineList != null && invlineList.size() > 0) {
                  for (InvoiceLine lineinv : invlineList) {
                    lineinv.setEfinBudgmanuencumln(null);
                    OBDal.getInstance().save(lineinv);
                  }
                }

                OBQuery<EfinBudManencumRev> revQuery = OBDal.getInstance().createQuery(
                    EfinBudManencumRev.class,
                    " as e where e.sRCManencumline.id in ( select e.id from Efin_Budget_Manencumlines e where e.manualEncumbrance.id='"
                        + encumbrance.getId() + "')");
                if (revQuery.list().size() > 0) {
                  for (EfinBudManencumRev rev : revQuery.list()) {
                    EfinBudgetManencumlines srclines = rev.getSRCManencumline();
                    rev.setSRCManencumline(null);
                    OBDal.getInstance().save(rev);
                    EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();

                    /**
                     * in case if we are taking amt from remaining amt when we dont have enought amt
                     * in app amt then while revoke have to give the amt back to remaining amt
                     **/

                    Order invoicePO = OBDal.getInstance().get(Order.class,
                        inv.getSalesOrder().getId());
                    Order latestOrder = PurchaseInvoiceSubmitUtils
                        .getLatestOrderComplete(invoicePO);
                    /** encumbrance lines updated amt **/
                    BigDecimal encuUpdateAmt = lines.getSystemUpdatedAmt();
                    /** order total line net amt based on encumbrance line uniquecode **/
                    BigDecimal grandTotal = latestOrder.getOrderLineList().stream()
                        .filter(a -> !a.isEscmIssummarylevel() && a.getEFINUniqueCode() != null
                            && a.getEFINUniqueCode().getId()
                                .equals(lines.getAccountingCombination().getId()))
                        .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Get grand total of other POs linked with the same encumbrance
                    BigDecimal otherPoTotal = InvoiceRevokeDAO.getGrandTotalOtherPO(latestOrder,
                        lines.getAccountingCombination().getId());

                    /**
                     * find the differenct amt which we manually added in modification or remaining
                     * amt
                     **/
                    BigDecimal incAmt = encuUpdateAmt.subtract(grandTotal.add(otherPoTotal));

                    /**
                     * diff amt(enc update amt - po amt) greater than zero and po encumbrance is
                     * manual and diff amt not presented in remaining amt place then have to give
                     * the amt back to in the place of remaining
                     **/
                    if (incAmt.compareTo(BigDecimal.ZERO) > 0
                        && incAmt.compareTo(lines.getRemainingAmount()) != 0
                        && poEncumbrance != null && poEncumbrance.getEncumMethod().equals("M")) {
                      /** subtract remaining amt in the difference **/
                      incAmt = incAmt.subtract(lines.getRemainingAmount());
                      /**
                       * diff amt compare with deleted amt if greater than and equal add reduce amt
                       * itself in remaining amt not to update the app amt
                       **/
                      if (incAmt.compareTo(rev.getRevamount().negate()) >= 0) {
                        lines.setRemainingAmount(
                            lines.getRemainingAmount().add(rev.getRevamount().negate()));
                      }
                      /**
                       * diff amt compare with deleted amt if lesser than add diff amt in remaining
                       * amt and update app amt also
                       **/
                      else if (incAmt.compareTo(rev.getRevamount().negate()) < 0) {
                        // BigDecimal updRemaAmt = incAmt.subtract(lines.getRemainingAmount());
                        lines.setRemainingAmount(lines.getRemainingAmount().add(incAmt));
                        lines.setAPPAmt(
                            lines.getAPPAmt().add(rev.getRevamount().negate().subtract(incAmt)));
                      }
                    } else {
                      lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
                    }

                    lines.getEfinBudManencumRevList().remove(rev);

                    // Task no : 7416
                    OBQuery<EfinBudManencumRev> newEncumModification = OBDal.getInstance()
                        .createQuery(EfinBudManencumRev.class,
                            " as e where e.manualEncumbranceLines.id=:newEncumLineId  order by e.revamount desc ");
                    newEncumModification.setNamedParameter("newEncumLineId", srclines.getId());
                    newEncumModRev = newEncumModification.list();
                    if (newEncumModRev.size() > 0) {
                      for (EfinBudManencumRev newEncumModiObj : newEncumModRev) {
                        OBDal.getInstance().remove(newEncumModiObj);
                        srclines.getEfinBudManencumRevList().remove(newEncumModiObj);
                      }
                    }

                    encumbrance.getEfinBudgetManencumlinesList().remove(srclines);

                  }
                }

                // Commented the below method "removePoExtraAmount" because we are restricting that
                // if
                // auto encumbrance doesnt have enough amount we are throwing error while submitting
                // the invoice itself. So taking extra amount for tax from budget enquiry will not
                // happen

                // revert the extra amount which is taken from budget enquiry for tax in po
                // encum.
                // if (poEncumbrance.getEncumMethod().equals("A")) {
                // PurchaseInvoiceSubmitUtils.removePoExtraAmount(poEncumbrance, inv, true, null);
                // }

                encumbrance.setDocumentStatus("DR");
                event.setCurrentState(propencum, null);
                inv.setEfinManualencumbrance(null);
                OBDal.getInstance().save(inv);
                OBDal.getInstance().remove(encumbrance);
              }
            } else {
              // stage alone revert it.
              encumid = inv.getEfinManualencumbrance().getId();
              inv.setEfinManualencumbrance(null);
              OBDal.getInstance().save(inv);
              EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class, encumid);
              encum.setEncumStage("POE");
              OBDal.getInstance().save(encum);
            }
          }
          inv.setEfinIsrdv(false);
          if (inv.getEfinRdvtxnline() != null) {
            txnlineObj = inv.getEfinRdvtxnline();
            txnlineObj.setAmarsaraf(false);
            txnlineObj.setTxnverStatus("DR");
            txnlineObj.setInvoice(null);
            OBDal.getInstance().save(txnlineObj);
          } else {
            version = inv.getEfinRdvtxn();
            version.setAmarsaraf(false);
            version.setTxnverStatus("DR");
            version.setInvoice(null);
            OBDal.getInstance().save(version);

            if (version.isAdvancetransaction() && version.getEfinRDVTxnlineList().size() > 0) {
              EfinRDVTxnline txnLineObj = version.getEfinRDVTxnlineList().get(0);
              txnLineObj.setAmarsaraf(false);
              txnLineObj.setTxnverStatus("DR");
              txnLineObj.setInvoice(null);
              OBDal.getInstance().save(txnLineObj);
            }

          }
          OBDal.getInstance().flush();
        }

      }
      // order to receive event
      else {
        // update 'next assigned no' with previous no. in document seq while delete the recent
        // record for reusing the document sequence task no - 7409

        // get document type
        DocumentType docType = OBDal.getInstance().get(DocumentType.class,
            inv.getTransactionDocument().getId());
        if (docType.getDocumentSequence() != null) {
          // get document sequence
          Sequence docseq = OBDal.getInstance().get(Sequence.class,
              docType.getDocumentSequence().getId());
          seqName = docseq.getName();

          AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(inv.getAccountingDate());

          Utility.setDocumentSequenceAfterDeleteRecord(AccountDate, seqName,
              inv.getOrganization().getId(), Long.parseLong(inv.getDocumentNo()), null, false);

        }
      }
    } catch (Exception e) {
      log.error(" Exception while Delete invoice line: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}