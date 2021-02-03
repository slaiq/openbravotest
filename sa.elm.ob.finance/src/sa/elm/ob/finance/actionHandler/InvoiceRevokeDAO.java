
package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinManualEncumInvoice;
import sa.elm.ob.finance.EfinPrepaymentInvoice;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.actionHandler.budgetholdplandetails.BudgetHoldPlanReleaseDAOImpl;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerImpl;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.finance.util.DAO.EncumbranceProcessDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.InvoiceApprovalTable;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopinagh.R
 *
 */

public class InvoiceRevokeDAO {

  private static Logger log4j = Logger.getLogger(InvoiceRevokeDAO.class);
  private static final String DRAFT = "DR";
  private static final String COMPLETE = "CO";
  public static final String API_DOCUMENT = "API";
  public static final String PPI_DOCUMENT = "PPI";
  public static final String PPA_DOCUMENT = "PPA";
  public static final String RDV_DOCUMENT = "RDV";
  public static final String PO_DOCUMENT = "POM";

  private static final String REVOKE = "REV";

  /**
   * Checks if the invoice has been processed by any other user.
   * 
   * @param invoice
   *          {@link Invoice} Invoice object
   * @return false if the invoice is processed ( rework/completed) by some other user else true
   * 
   */
  public static Boolean checkIfValidInvoice(Invoice invoice) {

    Boolean isValid = Boolean.TRUE;

    try {
      String strStatus = "";

      strStatus = invoice.getDocumentStatus();
      if (DRAFT.equals(strStatus) || COMPLETE.equals(strStatus)) {
        isValid = Boolean.FALSE;
      }

    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception while checkIfValidInvoice: " + e);
    }

    return isValid;
  }

  /**
   * Updates record audit info.
   * 
   * @param invoice
   *          {@link Invoice} Invoice object
   */

  public static void updateInvoiceStatus(Invoice invoice) {
    try {

      invoice.setUpdated(new java.util.Date());
      invoice.setUpdatedBy(OBContext.getOBContext().getUser());
      invoice.setDocumentStatus(DRAFT);
      invoice.setDocumentAction(COMPLETE);
      invoice.setEfinDocaction(COMPLETE);
      invoice.setEfinDocactionfinal(COMPLETE);

      OBDal.getInstance().save(invoice);
    } catch (Exception e) {

      e.printStackTrace();
      log4j.error("Exception while updateInvoiceStatus: " + e);
    }

  }

  /**
   * get the document type of the invoice to be revoked.
   * 
   * @param invoice
   *          {@link Invoice} Invoice object
   * 
   * @return Document type as per the document rule.
   * 
   */

  public static String getInvoiceType(Invoice invoice) {
    String strInvoicetype = "";
    try {

      strInvoicetype = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

      switch (strInvoicetype) {
      case API_DOCUMENT:
        strInvoicetype = Resource.AP_INVOICE_RULE;
        break;

      case PPI_DOCUMENT:
        strInvoicetype = Resource.AP_Prepayment_Inv_RULE;
        break;

      case PPA_DOCUMENT:
        strInvoicetype = Resource.AP_Prepayment_App_RULE;
        break;

      case RDV_DOCUMENT:
        strInvoicetype = Resource.RDV_Transaction;

      default:
        strInvoicetype = Resource.AP_INVOICE_RULE;
        break;
      }

    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception while getInvoiceType: " + e);
    }
    return strInvoicetype;
  }

  /**
   * Get data to insert invoice history
   * 
   * @param invoice
   *          {@link Invoice} Invoice object
   * @param comments
   *          reason for revoke
   * 
   * @return {@link JSONObject} with history info
   * 
   */

  public static JSONObject getHistoryData(Invoice invoice, String comments) {
    JSONObject historyData = new JSONObject();

    try {

      historyData.put("ClientId", invoice.getClient().getId());
      historyData.put("OrgId", invoice.getOrganization().getId());
      historyData.put("RoleId", OBContext.getOBContext().getRole().getId());
      historyData.put("UserId", OBContext.getOBContext().getUser().getId());
      historyData.put("HeaderId", invoice.getId());
      historyData.put("Comments", comments);
      historyData.put("Status", REVOKE);
      historyData.put("NextApprover", "");
      historyData.put("HistoryTable", InvoiceApprovalTable.INVOICE_HISTORY);
      historyData.put("HeaderColumn", InvoiceApprovalTable.INVOICE_HEADER_COLUMN);
      historyData.put("ActionColumn", InvoiceApprovalTable.INVOICE_DOCACTION_COLUMN);

    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception while getHistoryData: " + e);
    }
    return historyData;
  }

  /**
   * Revert the reserved invoice and update encumbrance , actual amount in budget enquiry
   * 
   * @param invoice
   * @param strInvoiceType
   * @return success message or error message if any
   */

  public static String revertReservedInvoice(Invoice invoice) {

    List<EfinManualEncumInvoice> reservedInvoices = new ArrayList<EfinManualEncumInvoice>();

    String strEncumbranceId;
    String strInvoiceType, encumid;
    Currency currency = null;
    BigDecimal conversionrate = BigDecimal.ZERO;

    try {
      strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);
      reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoice.getId());

      currency = FinanceUtils.getCurrency(invoice.getOrganization().getId(), invoice);
      conversionrate = FinanceUtils.getConversionRate(OBDal.getInstance().getConnection(),
          invoice.getOrganization().getId(), invoice, currency);

      // -------------------------------- APINVOICE BLOCK----------------------------------------

      if (API_DOCUMENT.equals(strInvoiceType) && invoice.getEfinEncumtype().equals("M")) {

        if (reservedInvoices.size() > 0) {

          removeInvoiceInEncumbrance(reservedInvoices, invoice);

          deleteFundsEncumIncaseCost(invoice);

        }

      }

      if (API_DOCUMENT.equals(strInvoiceType) && invoice.getEfinEncumtype().equals("A")) {

        if (reservedInvoices.size() > 0) {

          removeInvoiceInEncumbrance(reservedInvoices, invoice);

          strEncumbranceId = invoice.getEfinManualencumbrance().getId();
          EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
              strEncumbranceId);
          encumbrance.setDocumentStatus("DR");
          OBDal.getInstance().save(encumbrance);

          if (encumbrance != null) {
            for (EfinBudgetManencumlines manencumlines : encumbrance
                .getEfinBudgetManencumlinesList()) {
              OBDal.getInstance().remove(manencumlines);
            }
          }

          invoice.setEfinManualencumbrance(null);
          OBDal.getInstance().save(invoice);
          OBDal.getInstance().remove(encumbrance);
          OBDal.getInstance().flush();
        }

      }

      // ---------------------------- AP PREPAYMENT INVOICE BLOCK---------------------------------

      if (invoice.getEfinManualencumbrance() != null && invoice.getEfinDistribution() != null
          && PPI_DOCUMENT.equals(strInvoiceType)) {

        OBQuery<EfinPrepaymentInvoice> prepayinv = OBDal.getInstance().createQuery(
            EfinPrepaymentInvoice.class,
            " manualEncumbrance.id in ( select inv.efinManualencumbrance.id from  Invoice inv  where inv.id='"
                + invoice.getId() + "' ) ");
        if (prepayinv.list().size() > 0) {
          for (EfinPrepaymentInvoice pre : prepayinv.list()) {
            OBDal.getInstance().remove(pre);
          }
        }

        if (reservedInvoices.size() > 0) {
          for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {
            OBDal.getInstance().remove(reservedInvoice);
          }
        }

        if ("A".equals(invoice.getEfinEncumtype())) {
          strEncumbranceId = invoice.getEfinManualencumbrance().getId();
          EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
              strEncumbranceId);
          encumbrance.setDocumentStatus("DR");

          if (encumbrance != null) {
            for (EfinBudgetManencumlines manencumlines : encumbrance
                .getEfinBudgetManencumlinesList()) {
              OBDal.getInstance().remove(manencumlines);
            }
          }

          invoice.setEfinManualencumbrance(null);
          OBDal.getInstance().save(invoice);
          OBDal.getInstance().remove(encumbrance);
        }

        OBDal.getInstance().flush();
      }

      // -------------------------- AP PREPAYMENT APPLICATION BLOCK---------------------------------

      if (PPA_DOCUMENT.equals(strInvoiceType)) {

        if (invoice.getEfinEncumtype().equals("M")) {
          if (reservedInvoices.size() > 0) {
            removeInvoiceInEncumbrance(reservedInvoices, invoice);
          }
          deleteFundsEncumIncaseCost(invoice);
          PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionrate, true);
          PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(), conversionrate,
              true);

        } else {
          PurchaseInvoiceSubmitUtils.updateAppliedAmountToUsedAmount(invoice, conversionrate, true);
          PurchaseInvoiceSubmitUtils.updatePrepaymentUsedAmount(invoice.getId(), conversionrate,
              true);
        }
      }

      // -------------------------- RDV BLOCK---------------------------------

      if ((RDV_DOCUMENT.equals(strInvoiceType) && invoice.getEfinManualencumbrance() != null
          && !invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE"))) {

        if (reservedInvoices.size() > 0) {

          removeInvoiceInEncumbrance(reservedInvoices, invoice);

          deleteFundsEncumIncaseCost(invoice);

          TaxLineHandlerDAO taxHandler = new TaxLineHandlerImpl();
          // Add modification to nullify the already created modification in case of
          // ministry tax invoice

          Boolean isExclusiveTaxInvoice = taxHandler.isExclusiveTaxInvoice(invoice);

          if (isExclusiveTaxInvoice && invoice.getEfinRDVTxnList().size() > 0 && invoice
              .getEfinRDVTxnList().get(0).getLineTaxamt().compareTo(BigDecimal.ZERO) == 0) {
            Map<String, BigDecimal> taxLinesMap = new HashMap<String, BigDecimal>();
            taxLinesMap = taxHandler.getTaxLineCodesAndAmount(invoice);

            if (!taxLinesMap.isEmpty()) {

              EfinBudgetManencum poEncumbrance = PurchaseInvoiceSubmitUtils
                  .getPoEncumbranceFromInvoice(invoice);
              PurchaseInvoiceSubmitUtils.addEncumbranceModification(taxLinesMap, poEncumbrance,
                  invoice, Boolean.TRUE);
              // revert the extra amount which is taken from budget enquiry for tax in po
              // encum.
              if (poEncumbrance.getEncumMethod().equals("A")) {
                PurchaseInvoiceSubmitUtils.removePoExtraAmount(poEncumbrance, invoice, false, null);
              } else {
                for (Entry<String, BigDecimal> taxEntries : taxLinesMap.entrySet()) {
                  String strUniqueCodeId = taxEntries.getKey();
                  BigDecimal taxAmount = taxEntries.getValue().setScale(2, RoundingMode.HALF_UP);

                  EfinBudgetManencumlines lines = PurchaseInvoiceSubmitUtils
                      .getEncumbranceLine(poEncumbrance.getId(), strUniqueCodeId);

                  /**
                   * in case if we are taking amt from remaining amt when we dont have enought amt
                   * in app amt then while revoke have to give the amt back to remaining amt
                   **/
                  Order invoicePO = OBDal.getInstance().get(Order.class,
                      invoice.getSalesOrder().getId());
                  Order latestOrder = PurchaseInvoiceSubmitUtils.getLatestOrderComplete(invoicePO);
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
                   * manual and diff amt not presented in remaining amt place then have to give the
                   * amt back to in the place of remaining
                   **/
                  if (incAmt.compareTo(BigDecimal.ZERO) > 0
                      && incAmt.compareTo(lines.getRemainingAmount()) != 0 && poEncumbrance != null
                      && poEncumbrance.getEncumMethod().equals("M")) {

                    /** subtract remaining amt in the difference **/
                    incAmt = incAmt.subtract(lines.getRemainingAmount());
                    /**
                     * diff amt compare with tax amt if greater than and equal add reduce amt itself
                     * in remaining amt not to update the app amt
                     **/
                    if (incAmt.compareTo(taxAmount) >= 0) {
                      lines.setRemainingAmount(lines.getRemainingAmount().add(taxAmount));
                      lines.setAPPAmt(lines.getAPPAmt().subtract(taxAmount));
                    }
                    /**
                     * diff amt compare with tax amt if lesser than add diff amt in remaining amt
                     * and update app amt also
                     **/
                    else if (incAmt.compareTo(taxAmount) < 0) {
                      // BigDecimal updRemaAmt = incAmt.subtract(lines.getRemainingAmount());
                      lines.setRemainingAmount(lines.getRemainingAmount().add(incAmt));
                      lines.setAPPAmt(
                          (lines.getAPPAmt().subtract(taxAmount)).add(taxAmount.subtract(incAmt)));
                    }
                  }
                }
              }
              OBDal.getInstance().flush();
              OBDal.getInstance().refresh(invoice);

            }
          }

          if (RDV_DOCUMENT.equals(strInvoiceType)) {
            String encum = invoice.getEfinManualencumbrance().getManualEncumbrance();
            EfinBudgetManencum manEncum = OBDal.getInstance().get(EfinBudgetManencum.class, encum);
            manEncum.setEncumStage("MUS");
            OBDal.getInstance().save(manEncum);
            OBDal.getInstance().flush();
          }

        }

      }

      if ((RDV_DOCUMENT.equals(strInvoiceType) && invoice.getEfinManualencumbrance() != null
          && invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE"))) {

        if (reservedInvoices.size() > 0) {

          removeInvoiceInEncumbrance(reservedInvoices, invoice);

          strEncumbranceId = invoice.getEfinManualencumbrance().getId();
          EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
              strEncumbranceId);
          encumbrance.setDocumentStatus("DR");
          if (encumbrance != null) {
            for (EfinBudgetManencumlines manencumlines : encumbrance
                .getEfinBudgetManencumlinesList()) {
              OBDal.getInstance().remove(manencumlines);
            }
          }

          if (invoice.getEfinFundsEncumbrance() != null) {
            String strfundEncumbranceId = invoice.getEfinFundsEncumbrance().getId();
            EfinBudgetManencum fundEncumbrance = Utility.getObject(EfinBudgetManencum.class,
                strfundEncumbranceId);
            fundEncumbrance.setDocumentStatus("DR");

            if (fundEncumbrance != null) {
              for (EfinBudgetManencumlines manencumlines : fundEncumbrance
                  .getEfinBudgetManencumlinesList()) {
                OBDal.getInstance().remove(manencumlines);
              }
            }
            invoice.setEfinFundsEncumbrance(null);
            OBDal.getInstance().save(invoice);
            OBDal.getInstance().remove(fundEncumbrance);
          }

          invoice.setEfinManualencumbrance(null);
          OBDal.getInstance().save(invoice);
          OBDal.getInstance().remove(encumbrance);
          OBDal.getInstance().flush();
        }
      }

      // -------------------------- POM BLOCK---------------------------------

      if (PO_DOCUMENT.equals(strInvoiceType)) {
        invoice.setEfinEncumbranceType("POE");
        OBDal.getInstance().save(invoice);
        reservedInvoices = PurchaseInvoiceSubmitUtils.getReservedInvoices(invoice.getId());
        if (reservedInvoices.size() > 0) {
          for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {

            OBDal.getInstance().remove(reservedInvoice);
          }
          OBDal.getInstance().flush();

          // check Budget type of invoice is cost, if so delete funds encumbrance
          if ("C".equals(invoice.getEfinBudgetType())) {

            if (invoice.getEfinFundsEncumbrance() != null) {
              EfinBudgetManencum fundsEncum = invoice.getEfinFundsEncumbrance();
              fundsEncum.setDocumentStatus("DR");
              // empty the reference in invoice for funds
              invoice.setEfinFundsEncumbrance(null);
              OBDal.getInstance().save(invoice);
              OBDal.getInstance().remove(fundsEncum);
            }
          }

          if (invoice.getEfinManualencumbrance() != null) {
            BigDecimal invLineAmt = invoice.getInvoiceLineList().stream()
                .filter(a -> a.isEfinIspom() == true).map(a -> a.getLineNetAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            Order po = OBDal.getInstance().get(Order.class, invoice.getEfinCOrder().getId());

            // Task 7847: PO to PO Match Invoice not creating split encumbrance
            // If Invoice encumbrance is different from Order encumbrance, then split case
            boolean isSplitEncumbrance = false;
            if (invoice.getEfinManualencumbrance() != null && invoice.getEfinCOrder() != null) {
              isSplitEncumbrance = PurchaseInvoiceSubmitUtils.isEncumbranceDifferent(
                  invoice.getEfinManualencumbrance().getId(), invoice.getEfinCOrder().getId());
            }

            if (invLineAmt.compareTo(po.getGrandTotalAmount()) != 0 || isSplitEncumbrance) {

              // splitted enum
              encumid = invoice.getEfinManualencumbrance().getId();
              EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class, encumid);

              if (encumbrance != null) {
                OBQuery<InvoiceLine> invLine = OBDal.getInstance().createQuery(InvoiceLine.class,
                    "invoice.id='" + invoice.getId() + "' and efinBudgmanuencumln.id is not null ");
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

                TaxLineHandlerDAO taxDAO = new TaxLineHandlerImpl();
                Boolean isExclusiveTaxInvoice = taxDAO.isExclusiveTaxInvoice(invoice);
                Map<String, BigDecimal> taxLinesMap = taxDAO.getTaxLineCodesAndAmount(invoice);
                String strCombinationId = "";

                if (revQuery.list().size() > 0) {
                  for (EfinBudManencumRev rev : revQuery.list()) {

                    strCombinationId = rev.getManualEncumbranceLines().getAccountingCombination()
                        .getId();

                    EfinBudgetManencumlines srclines = rev.getSRCManencumline();
                    rev.setSRCManencumline(null);
                    OBDal.getInstance().save(rev);
                    EfinBudgetManencumlines lines = rev.getManualEncumbranceLines();
                    /**
                     * in case if we are taking amt from remaining amt when we dont have enought amt
                     * in app amt then while revoke have to give the amt back to remaining amt
                     **/
                    Order latestOrder = PurchaseInvoiceSubmitUtils.getLatestOrderComplete(po);
                    /** encumbrance lines updated amt **/
                    BigDecimal encuUpdateAmt = lines.getSystemUpdatedAmt();
                    /** order total line net amt based on encumbrance line uniquecode **/
                    BigDecimal grandTotal = latestOrder.getOrderLineList().stream()
                        .filter(a -> !a.isEscmIssummarylevel() && a.getEFINUniqueCode() != null
                            && a.getEFINUniqueCode().getId()
                                .equals(lines.getAccountingCombination().getId()))
                        .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                    // Get grand total of other POs linked with the same encumbrance
                    BigDecimal otherPoTotal = getGrandTotalOtherPO(latestOrder,
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
                        && po.getEfinBudgetManencum() != null
                        && po.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                      /** subtract remaining amt in the difference **/
                      incAmt = incAmt.subtract(lines.getRemainingAmount());
                      /**
                       * diff amt compare with reduce amt if greater than and equal add reduce amt
                       * itself in remaining amt not to update the app amt
                       **/
                      if (incAmt.compareTo(rev.getRevamount().negate()) >= 0) {
                        lines.setRemainingAmount(
                            lines.getRemainingAmount().add(rev.getRevamount().negate()));
                      }
                      /**
                       * diff amt compare with reduce amt if lesser than add diff amt in remaining
                       * amt and update app amt also
                       **/
                      else if (incAmt.compareTo(rev.getRevamount().negate()) < 0) {
                        // BigDecimal updRemaAmt = incAmt.subtract(lines.getRemainingAmount());
                        lines.setRemainingAmount(lines.getRemainingAmount().add(incAmt));
                        lines.setAPPAmt(
                            lines.getAPPAmt().add(rev.getRevamount().negate().subtract(incAmt)));
                      }
                    }
                    /** other cases **/
                    else {
                      lines.setAPPAmt(lines.getAPPAmt().add(rev.getRevamount().negate()));
                    }

                    if (isExclusiveTaxInvoice && rev.getManualEncumbranceLines()
                        .getManualEncumbrance().getEncumMethod().equals("M")) {
                      if (taxLinesMap.containsKey(strCombinationId)) {

                        // lines.setAPPAmt(lines.getAPPAmt().add(taxAmount.negate()));
                        // lines.setRemainingAmount(lines.getRemainingAmount().add(taxAmount));
                      }
                    }
                    lines.getEfinBudManencumRevList().remove(rev);
                    encumbrance.getEfinBudgetManencumlinesList().remove(srclines);
                  }

                  if (po.getEfinBudgetManencum().getEncumMethod().equals("A")) {
                    PurchaseInvoiceSubmitUtils
                        .removePoMatchExtraBudgetAmount(po.getEfinBudgetManencum(), invoice, null);
                  }
                }
                // event.setCurrentState(propencum, null);
                EfinBudgetManencumv POEncumbrance = Utility.getObject(EfinBudgetManencumv.class,
                    po.getEfinBudgetManencum().getId());
                invoice.setEfinManualencumbrance(POEncumbrance);
                invoice.setEfinEncumtype(POEncumbrance.getEncumbranceMethod());
                invoice.setEfinEncumbranceType(POEncumbrance.getEncumbranceType());
                OBDal.getInstance().save(invoice);
                encumbrance.setDocumentStatus("DR");
                OBDal.getInstance().remove(encumbrance);
              }

            } else {
              // stage alone revert it.
              encumid = invoice.getEfinManualencumbrance().getId();
              EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class, encumid);
              encum.setEncumStage("POE");
              OBDal.getInstance().save(encum);

              // update the encumbrance line id is null
              for (InvoiceLine line : invoice.getInvoiceLineList()) {
                line.setEfinBudgmanuencumln(null);
                OBDal.getInstance().save(line);
              }
            }
          }
        }
      }

    } catch (

    Exception e) {
      e.printStackTrace();
      log4j.error("Exception while revertReservedInvoice: " + e);
      throw new OBException(e);
    }
    return "";

  }

  /**
   * Delete newly created funds encumbrance in case of cost encumbrance is selected
   * 
   * @param invoice
   */

  private static void deleteFundsEncumIncaseCost(Invoice invoice) {

    try {
      if ("C".equals(invoice.getEfinBudgetType())) {

        if (invoice.getEfinFundsEncumbrance() != null) {

          EfinBudgetManencum fundsEncum = invoice.getEfinFundsEncumbrance();
          fundsEncum.setDocumentStatus("DR");
          OBDal.getInstance().save(fundsEncum);
          invoice.setEfinFundsEncumbrance(null);
          OBDal.getInstance().save(invoice);

          OBDal.getInstance().remove(fundsEncum);

        }

      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception while deleteFundsEncumIncaseCost: " + e);
      throw new OBException(e);

    }
  }

  /**
   * Remove invoice reference in encumbrance line a
   * 
   * @param reservedInvoices
   * @param invoice
   */
  private static void removeInvoiceInEncumbrance(List<EfinManualEncumInvoice> reservedInvoices,
      Invoice invoice) {
    try {
      for (EfinManualEncumInvoice reservedInvoice : reservedInvoices) {
        OBDal.getInstance().remove(reservedInvoice);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log4j.error("Exception while removeInvoiceInEncumbrance: " + e);
      throw new OBException(e);

    }
  }

  /**
   * Get grand total of other POs linked with the same encumbrance
   * 
   * @param order
   * @param lineUniqueCodeId
   * 
   * @return grandTotal
   * 
   */
  public static BigDecimal getGrandTotalOtherPO(Order order, String lineUniqueCodeId) {

    BigDecimal grandTotal = BigDecimal.ZERO;
    String documentNumber = order.getDocumentNo();

    OBQuery<Order> orderQry = OBDal.getInstance().createQuery(Order.class,
        " as e where e.documentNo != :documentNumber and e.efinBudgetManencum.id = :encumbranceId "
            + " and escmRevision = ( select max(escmRevision) from Order ord where ord.documentNo = e.documentNo ) ");
    orderQry.setNamedParameter("documentNumber", documentNumber);
    orderQry.setNamedParameter("encumbranceId", order.getEfinBudgetManencum().getId());

    if (orderQry != null) {
      List<Order> orderList = orderQry.list();
      if (orderList.size() > 0) {
        for (Order po : orderList) {
          grandTotal = grandTotal.add(po.getOrderLineList().stream()
              .filter(a -> !a.isEscmIssummarylevel() && a.getEFINUniqueCode() != null
                  && a.getEFINUniqueCode().getId().equals(lineUniqueCodeId))
              .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
      }
    }

    return grandTotal;
  }

  public static void releaseTempEncumbrance(Invoice invoice) {
    List<EfinRdvHoldAction> holdRelease = new ArrayList<EfinRdvHoldAction>();
    boolean skipTempEncumModify = false;
    try {
      // validation
      invoice.getEfinRdvtxn().getEfinRDVTxnlineList().stream().filter(y -> !y.isSummaryLevel())
          .collect(Collectors.toList()).forEach(ln -> {

            holdRelease.addAll(ln.getEfinRdvHoldActionList().stream()
                .filter(x -> x.getEfinBudgetTransfertrxline() != null)
                .collect(Collectors.toList()));

          });

      HashMap<AccountingCombination, BigDecimal> accountValueMap = new HashMap<AccountingCombination, BigDecimal>();
      if (holdRelease.size() > 0) {
        for (EfinRdvHoldAction release : holdRelease) {
          AccountingCombination account = release.getEfinRdvtxnline().getAccountingCombination();
          BigDecimal relamount = release.getRDVHoldAmount().negate();

          if (accountValueMap.containsKey(account)) {
            BigDecimal amount = accountValueMap.get(account).add(relamount);
            accountValueMap.put(account, amount);
          } else {
            accountValueMap.put(account, relamount);
          }
        }

        for (EfinRdvHoldAction release : holdRelease) {
          EfinBudgetManencum encum = release.getEfinBudgetTransfertrxline()
              .getEfinBudgetTransfertrx().getManualEncumbrance();
          List<EfinBudgetManencumlines> encumInvalidLine = encum.getEfinBudgetManencumlinesList()
              .stream()
              .filter(x -> accountValueMap.containsKey(x.getAccountingCombination()) && x
                  .getRevamount().compareTo(accountValueMap.get(x.getAccountingCombination())) < 0)
              .collect(Collectors.toList());
          if (encumInvalidLine.size() > 0) {
            skipTempEncumModify = true;
            break;
          }
        }

        // modify temp encum for budget revision - hold relase
        if (!skipTempEncumModify) {
          for (EfinRdvHoldAction release : holdRelease) {

            EfinBudgetManencum encum = release.getEfinBudgetTransfertrxline()
                .getEfinBudgetTransfertrx().getManualEncumbrance();
            AccountingCombination fundsCombination = null;
            AccountingCombination releaseCombination = release.getEfinRdvtxnline()
                .getAccountingCombination();
            if (releaseCombination.getSalesCampaign().getEfinBudgettype().equals("C")) {
              fundsCombination = releaseCombination.getEfinFundscombination();
            } else {
              fundsCombination = releaseCombination;
            }

            AccountingCombination acctCom999 = BudgetHoldPlanReleaseDAOImpl
                .get999AccountCombination(fundsCombination,
                    fundsCombination.getTrxOrganization().getId(), release.getClient().getId());
            encum.getEfinBudgetManencumlinesList().stream()
                .filter(x -> x.getAccountingCombination().getId().equals(acctCom999.getId()))
                .collect(Collectors.toList()).forEach(encumln -> {
                  // apply modification.
                  PurchaseInvoiceSubmitUtils.insertModification(encumln,
                      release.getRDVHoldAmount().negate());
                  // 999 update increase the encumbrance
                  EncumbranceProcessDAO.updateBudgetInquiry(encumln, encumln.getManualEncumbrance(),
                      release.getRDVHoldAmount().negate(), false, false);
                  // 990 update
                  EncumbranceProcessDAO.updateBudgetInquiryfor990(
                      encumln.getAccountingCombination(), encumln.getClient().getId(), encum,
                      release.getRDVHoldAmount().negate());

                });
          }
          OBDal.getInstance().flush();

        }
      }
    } catch (Exception e) {
      log4j.error("Exception while releaseTempEncumbrance: " + e);
      throw new OBException(e);

    }
  }

}
