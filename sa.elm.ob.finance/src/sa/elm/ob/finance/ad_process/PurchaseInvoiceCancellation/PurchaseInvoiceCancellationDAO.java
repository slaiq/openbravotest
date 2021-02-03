package sa.elm.ob.finance.ad_process.PurchaseInvoiceCancellation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;

import sa.elm.ob.finance.AppliedPrepaymentInvoice;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetActual;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncumInvoiceRef;
import sa.elm.ob.finance.EfinManualEncumInvoice;
import sa.elm.ob.finance.EfinPrepaymentInvoice;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.efinbudgetencum;
import sa.elm.ob.finance.actionHandler.InvoiceRevokeDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmit;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.utility.util.Utility;

public class PurchaseInvoiceCancellationDAO {
  private Connection conn = null;
  private static Logger log = Logger.getLogger(PurchaseInvoiceCancellationDAO.class);

  private static final String API_DOCUMENT = "API";
  private static final String PPI_DOCUMENT = "PPI";
  private static final String PPA_DOCUMENT = "PPA";
  private static final String RDV_DOCUMENT = "RDV";
  private static final String PO_DOCUMENT = "POM";

  private String strInvoiceType = "";

  public PurchaseInvoiceCancellationDAO(Connection con) {
    this.conn = con;
  }

  public Connection getConnection() {
    return conn;
  }

  /**
   * This method is to reserve reservation
   * 
   * @param vars
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param invoice
   * @param comments
   * @return
   */
  public boolean reverseReservation(VariablesSecureApp vars, String clientId, String orgId,
      String roleId, String userId, Invoice invoice, String comments) {
    String sql = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    BigDecimal GrandTotal = BigDecimal.ZERO, conversionrate = BigDecimal.ZERO;
    Boolean posted = Boolean.FALSE;

    try {
      OBContext.setAdminMode();

      strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

      if ("Y".equals(invoice.getPosted()))
        posted = Boolean.TRUE;

      if (log.isDebugEnabled()) {
        log.debug("getEfinEncumtype:" + invoice.getEfinEncumtype());
        log.debug("Invoice type : " + strInvoiceType);
        log.debug("Posted: " + posted);
      }

      Currency currency = null;

      // GET PARENT ORG CURRENCY
      currency = FinanceUtils.getCurrency(orgId, invoice);
      boolean fromPO = false;
      EfinBudgetManencum encum = null;
      String EncumId = "";
      // get conversion rate
      conversionrate = FinanceUtils.getConversionRate(conn, orgId, invoice, currency);

      // for Manual and AP Invoice or AP Prepayment Application
      if ((invoice.getEfinEncumtype().equals("M") && (API_DOCUMENT.equals(strInvoiceType)))
          || (RDV_DOCUMENT.equals(strInvoiceType) && invoice.getEfinManualencumbrance() != null
              && !invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE"))
          || PO_DOCUMENT.equals(strInvoiceType)) {
        // update the manual Encumbrance Invoice
        List<EfinManualEncumInvoice> reservedInvoices = PurchaseInvoiceSubmitUtils
            .getReservedInvoices(invoice.getId());

        log.debug("encuminvoice:" + reservedInvoices.size());
        if (RDV_DOCUMENT.equals(strInvoiceType)) {
          if (invoice.getEfinManualencumbrance() != null) {

            // Task 7847: PO to RDV Invoice not creating split encumbrance
            // If Invoice encumbrance is different from Order encumbrance, then split case
            boolean isSplitEncumbrance = false;
            if (invoice.getEfinManualencumbrance() != null && invoice.getEfinCOrder() != null) {
              isSplitEncumbrance = PurchaseInvoiceSubmitUtils.isEncumbranceDifferent(
                  invoice.getEfinManualencumbrance().getId(), invoice.getEfinCOrder().getId());
            }

            if (invoice.getGrandTotalAmount().compareTo(
                invoice.getEfinRdvtxn().getEfinRdv().getSalesOrder().getGrandTotalAmount()) != 0
                || isSplitEncumbrance) {
              encum = invoice.getEfinRdvtxn().getEfinRdv().getSalesOrder().getEfinBudgetManencum();
              fromPO = true;

              EncumId = invoice.getEfinManualencumbrance().getId();
              EfinBudgetManencum newEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  EncumId);
              if (!newEncum.getEncumMethod().equals("M"))
                newEncum.setDocumentStatus("CA");
              OBDal.getInstance().save(newEncum);
            } else {
              // stage move
              EfinBudgetManencum manEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  invoice.getEfinManualencumbrance().getId());
              manEncum.setEncumStage("POE");
              OBDal.getInstance().save(manEncum);
            }
            if (invoice.getEfinRdvtxnline() != null) {
              invoice.getEfinRdvtxnline().setTxnverStatus("DR");
              invoice.getEfinRdvtxnline().setApprovalStatus("DR");
              invoice.getEfinRdvtxnline().setAction("CO");
              invoice.getEfinRdvtxnline().setAmarsaraf(false);
            } else {
              invoice.getEfinRdvtxn().setTxnverStatus("DR");
              invoice.getEfinRdvtxn().setAppstatus("DR");
              invoice.getEfinRdvtxn().setAction("CO");
              invoice.getEfinRdvtxn().setAmarsaraf(false);
              if (invoice.getEfinRdvtxn().isAdvancetransaction()
                  && invoice.getEfinRdvtxn().getEfinRDVTxnlineList().size() > 0) {
                EfinRDVTxnline txnLineObj = invoice.getEfinRdvtxn().getEfinRDVTxnlineList().get(0);
                txnLineObj.setAmarsaraf(false);
                txnLineObj.setTxnverStatus("DR");
                txnLineObj.setInvoice(null);
                OBDal.getInstance().save(txnLineObj);
              }
            }
            OBDal.getInstance().save(invoice);
          }
        }

        if (PO_DOCUMENT.equals(strInvoiceType)) {
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
              encum = po.getEfinBudgetManencum();
              fromPO = true;
              EncumId = invoice.getEfinManualencumbrance().getId();
              EfinBudgetManencum newEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  EncumId);
              if (!newEncum.getEncumMethod().equals("M"))
                newEncum.setDocumentStatus("CA");
              OBDal.getInstance().save(newEncum);
            } else {
              // stage move
              EfinBudgetManencum manEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                  invoice.getEfinManualencumbrance().getId());
              manEncum.setEncumStage("POE");
              OBDal.getInstance().save(manEncum);
            }
          }

          if (invoice.getEfinFundsEncumbrance() != null) {
            EfinBudgetManencum fundsencumbrance = Utility.getObject(EfinBudgetManencum.class,
                invoice.getEfinFundsEncumbrance().getId());
            if (fundsencumbrance != null) {
              fundsencumbrance.setUpdated(new java.util.Date());
              fundsencumbrance.setUpdatedBy(OBContext.getOBContext().getUser());
              OBDal.getInstance().save(fundsencumbrance);
            }
          }
        }

        if (reservedInvoices.size() > 0) {
          for (EfinManualEncumInvoice inv : reservedInvoices) {

            log.debug("encuminvoice.id:" + inv.getId());

            AccountingCombination uniqueCode = inv.getManualEncumbranceLines()
                .getAccountingCombination();

            StringBuilder whereClause = new StringBuilder();
            whereClause.append(" where efinCValidcombination.id = :combinationId ");
            whereClause.append("   and invoice.id =:invoiceId ");

            OBQuery<InvoiceLine> linesQuery = OBDal.getInstance().createQuery(InvoiceLine.class,
                whereClause.toString());

            if (uniqueCode != null) {
              linesQuery.setNamedParameter("combinationId",
                  inv.getManualEncumbranceLines().getAccountingCombination().getId());
            } else {
              linesQuery.setNamedParameter("combinationId", "");
            }
            linesQuery.setNamedParameter("invoiceId", invoice.getId());

            List<InvoiceLine> linesList = new ArrayList<InvoiceLine>();

            if (linesQuery != null) {
              linesList = linesQuery.list();

              if (linesList.size() > 0) {
                // if same unique code is present in different line, then we must group it and then
                // do changes in encumbrance.

                InvoiceLine invoiceLine = linesList.get(0);
                if (!((API_DOCUMENT.equals(strInvoiceType) || PO_DOCUMENT.equals(strInvoiceType))
                    && "C".equals(invoice.getEfinBudgetType())
                    && "F".equals(inv.getManualEncumbranceLines().getManualEncumbrance()
                        .getSalesCampaign().getEfinBudgettype()))) {
                  OBDal.getInstance().remove(inv);
                }

                BigDecimal totalLineNetAmt = linesList.stream().map(a -> a.getLineNetAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                // if its rdv and splitted encum then increase amt in old encum
                if (fromPO) {
                  // -ve in new encum
                  EfinBudgetManencum newEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                      invoice.getEfinManualencumbrance().getId());

                  for (EfinBudgetManencumlines encLine : newEncum
                      .getEfinBudgetManencumlinesList()) {
                    if (invoiceLine.getEfinCValidcombination() == encLine
                        .getAccountingCombination()) {
                      if (!PO_DOCUMENT.equals(strInvoiceType)) {
                        encLine.setAPPAmt(encLine.getAPPAmt().subtract(totalLineNetAmt));
                      }

                      OBDal.getInstance().save(encLine);

                      EfinBudManencumRev manEncumRev = OBProvider.getInstance()
                          .get(EfinBudManencumRev.class);
                      manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
                      manEncumRev.setOrganization(OBDal.getInstance().get(Organization.class,
                          encLine.getOrganization().getId()));
                      manEncumRev.setActive(true);
                      manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
                      manEncumRev.setCreationDate(new java.util.Date());
                      manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
                      manEncumRev.setUpdated(new java.util.Date());
                      manEncumRev.setUniqueCode(
                          invoiceLine.getEfinCValidcombination().getEfinUniqueCode());
                      manEncumRev.setManualEncumbranceLines(encLine);
                      manEncumRev.setRevdate(new java.util.Date());
                      manEncumRev.setStatus("APP");
                      manEncumRev.setAuto(true);
                      manEncumRev.setSystem(true);
                      manEncumRev.setRevamount(totalLineNetAmt.negate());
                      manEncumRev.setAccountingCombination(invoiceLine.getEfinCValidcombination());
                      manEncumRev.setEncumbranceType("POE");
                      OBDal.getInstance().save(manEncumRev);
                    }
                  }
                  // +ve in old encum.
                  if (encum != null && encum.getEfinBudgetManencumlinesList() != null
                      && encum.getEfinBudgetManencumlinesList().size() > 0) {
                    for (EfinBudgetManencumlines manEncum : encum
                        .getEfinBudgetManencumlinesList()) {

                      if (invoiceLine.getEfinCValidcombination() == manEncum
                          .getAccountingCombination()) {
                        EfinBudManencumRev manEncumRev = OBProvider.getInstance()
                            .get(EfinBudManencumRev.class);
                        // insert into Manual Encumbrance Revision Table
                        manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
                        manEncumRev.setOrganization(OBDal.getInstance().get(Organization.class,
                            manEncum.getOrganization().getId()));
                        manEncumRev.setActive(true);
                        manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
                        manEncumRev.setCreationDate(new java.util.Date());
                        manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
                        manEncumRev.setUpdated(new java.util.Date());
                        manEncumRev.setUniqueCode(
                            invoiceLine.getEfinCValidcombination().getEfinUniqueCode());
                        manEncumRev.setManualEncumbranceLines(manEncum);
                        manEncumRev.setRevdate(new java.util.Date());
                        manEncumRev.setStatus("APP");
                        manEncumRev.setAuto(true);
                        manEncumRev.setSystem(true);
                        manEncumRev.setRevamount(totalLineNetAmt);
                        manEncumRev
                            .setAccountingCombination(invoiceLine.getEfinCValidcombination());
                        manEncumRev.setEncumbranceType("AAE");
                        manEncumRev.setSRCManencumline(invoiceLine.getEfinBudgmanuencumln());
                        manEncum.setAPPAmt(manEncum.getAPPAmt().add(totalLineNetAmt));
                        OBDal.getInstance().save(manEncumRev);

                        Order invoicePO = OBDal.getInstance().get(Order.class,
                            invoice.getEfinCOrder().getId());
                        Order latestOrder = PurchaseInvoiceSubmitUtils
                            .getLatestOrderComplete(invoicePO);
                        /** encumbrance lines updated amt **/
                        BigDecimal encuUpdateAmt = manEncum.getSystemUpdatedAmt();
                        /** order total line net amt based on encumbrance line uniquecode **/
                        BigDecimal grandTotal = latestOrder.getOrderLineList().stream()
                            .filter(a -> !a.isEscmIssummarylevel() && a.getEFINUniqueCode() != null
                                && a.getEFINUniqueCode().getId()
                                    .equals(manEncum.getAccountingCombination().getId()))
                            .map(a -> a.getLineNetAmount())
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Get grand total of other POs linked with the same encumbrance
                        BigDecimal otherPoTotal = InvoiceRevokeDAO.getGrandTotalOtherPO(latestOrder,
                            manEncum.getAccountingCombination().getId());

                        /**
                         * find the differenct amt which we manually added in modification or
                         * remaining amt
                         **/
                        BigDecimal incAmt = encuUpdateAmt.subtract(grandTotal.add(otherPoTotal));

                        /**
                         * diff amt(enc update amt - po amt) greater than zero and po encumbrance is
                         * manual and diff amt not presented in remaining amt place then have to
                         * give the amt back to in the place of remaining
                         **/
                        if (incAmt.compareTo(BigDecimal.ZERO) > 0
                            && incAmt.compareTo(manEncum.getRemainingAmount()) != 0 && encum != null
                            && encum.getEncumMethod().equals("M")) {
                          /** subtract remaining amt in the difference **/
                          incAmt = incAmt.subtract(manEncum.getRemainingAmount());
                          /**
                           * diff amt compare with deleted amt if greater than and equal add reduce
                           * amt itself in remaining amt not to update the app amt
                           **/
                          if (incAmt.compareTo(totalLineNetAmt) >= 0) {
                            manEncum.setRemainingAmount(
                                manEncum.getRemainingAmount().add(totalLineNetAmt));
                            manEncum.setAPPAmt(manEncum.getAPPAmt().subtract(totalLineNetAmt));
                          }
                          /**
                           * diff amt compare with deleted amt if lesser than add diff amt in
                           * remaining amt and update app amt also
                           **/
                          else if (incAmt.compareTo(totalLineNetAmt) < 0) {
                            // BigDecimal updRemaAmt = incAmt.subtract(lines.getRemainingAmount());
                            manEncum.setRemainingAmount(manEncum.getRemainingAmount().add(incAmt));
                            manEncum.setAPPAmt((manEncum.getAPPAmt().subtract(totalLineNetAmt))
                                .add(totalLineNetAmt.subtract(incAmt)));
                          }
                        }

                      }
                    }

                  }
                  // revert the extra amount which is taken from budget enquiry for tax in po
                  // encum.
                  if (encum.getEncumMethod().equals("A")) {
                    if (RDV_DOCUMENT.equals(strInvoiceType)) {
                      PurchaseInvoiceSubmitUtils.removePoExtraAmount(encum, invoice, true,
                          uniqueCode);
                    }
                    if (PO_DOCUMENT.equals(strInvoiceType)) {
                      PurchaseInvoiceSubmitUtils.removePoMatchExtraBudgetAmount(encum, invoice,
                          uniqueCode);
                    }

                  }
                }

                // if its cost encum then Cancel the funds encumbrance
                EfinBudgetManencum fundsEncum = invoice.getEfinFundsEncumbrance();
                if (fundsEncum != null && !fundsEncum.getDocumentStatus().equals("CA")) {
                  fundsEncum.setDocumentStatus("CA");
                  OBDal.getInstance().save(fundsEncum);

                  for (EfinBudgetManencumlines fundLine : fundsEncum
                      .getEfinBudgetManencumlinesList()) {
                    @SuppressWarnings("unused")
                    PurchaseInvoiceSubmitUtils utils = new PurchaseInvoiceSubmitUtils(invoice);

                    // update department level funds ='N' and except 990,999 dept
                    PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(
                        fundLine.getAccountingCombination(), fundLine.getAmount(),
                        invoice.getEfinBudgetint(), posted, Boolean.TRUE);

                    PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(
                        fundLine.getAccountingCombination(), fundLine.getAmount(),
                        invoice.getEfinBudgetint(), posted, Boolean.TRUE);

                    if (!posted) {
                      utils = new PurchaseInvoiceSubmitUtils(invoice);
                      // --990 dept
                      PurchaseInvoiceSubmitUtils.updateBCUBudgetEnquiry(
                          fundLine.getAccountingCombination(), fundLine.getAmount(),
                          invoice.getEfinBudgetint(), posted, Boolean.TRUE);
                    }

                  }
                  OBDal.getInstance().flush();

                }

              }
              OBDal.getInstance().flush();

              if (posted && inv.getManualEncumbranceLines().getManualEncumbrance()
                  .getSalesCampaign().getEfinBudgettype().equals("F")) {
                if (invoice.getEfinBudgetType().equals("F")) {
                  // update department level funds ='N' and except 990,999 dept

                  PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(
                      inv.getManualEncumbranceLines().getAccountingCombination(),
                      inv.getInvamount(), invoice.getEfinBudgetint(), posted, Boolean.TRUE);

                  // update department level funds ='Y' and 990,999 dept

                  PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(
                      inv.getManualEncumbranceLines().getAccountingCombination(),
                      inv.getInvamount(), invoice.getEfinBudgetint(), posted, Boolean.TRUE);
                }
              }

              OBDal.getInstance().flush();
            }
          }
        }
      }

      // for Auto
      if ((invoice.getEfinEncumtype().equals("A")
          && (API_DOCUMENT.equals(strInvoiceType) || PPI_DOCUMENT.equals(strInvoiceType)))
          || (RDV_DOCUMENT.equals(strInvoiceType) && invoice.getEfinManualencumbrance() != null
              && invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE"))) {

        if (invoice.getEfinManualencumbrance() != null) {

          AccountingCombination combination = null;

          Map<String, BigDecimal> reservationMap = getUniqueCodeReservationAmount(
              invoice.getEfinManualencumbrance().getId());

          if (RDV_DOCUMENT.equals(strInvoiceType)) {

            if ("C".equals(invoice.getEfinBudgetType())) {

              // in rdv for cost while cancel even it is post we have to send posted ='N' to update
              // only encumbrance because for cost actual wont update in budget enquiry
              if (invoice.getEfinManualencumbrance() != null) {
                EfinBudgetManencum costEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                    invoice.getEfinManualencumbrance().getId());
                if (costEncum != null) {
                  for (EfinBudgetManencumlines lines : costEncum.getEfinBudgetManencumlinesList()) {
                    @SuppressWarnings("unused")
                    PurchaseInvoiceSubmitUtils utils = new PurchaseInvoiceSubmitUtils(invoice);

                    // update department level funds ='N' and except 990,999 dept
                    PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(
                        lines.getAccountingCombination(), lines.getAmount(),
                        invoice.getEfinBudgetint(), false, Boolean.TRUE);

                    // update department level funds ='Y' and 990,999 dept
                    // --999 dept
                    PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(lines.getAccountingCombination(),
                        lines.getAmount(), invoice.getEfinBudgetint(), false, Boolean.TRUE);

                    // --990 dept
                    PurchaseInvoiceSubmitUtils.updateBCUBudgetEnquiry(
                        lines.getAccountingCombination(), lines.getAmount(),
                        invoice.getEfinBudgetint(), false, Boolean.TRUE);

                  }
                }
              }

              if (invoice.getEfinFundsEncumbrance() != null) {
                for (EfinBudgetManencumlines lines : invoice.getEfinFundsEncumbrance()
                    .getEfinBudgetManencumlinesList()) {
                  @SuppressWarnings("unused")
                  PurchaseInvoiceSubmitUtils utils = new PurchaseInvoiceSubmitUtils(invoice);

                  // update department level funds ='N' and except 990,999 dept
                  PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(
                      lines.getAccountingCombination(), lines.getAmount(),
                      invoice.getEfinBudgetint(), posted, Boolean.TRUE);

                  // update department level funds ='Y' and 990,999 dept
                  // --999 dept
                  PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(lines.getAccountingCombination(),
                      lines.getAmount(), invoice.getEfinBudgetint(), posted, Boolean.TRUE);

                  if (!posted) {

                    // --990 dept
                    PurchaseInvoiceSubmitUtils.updateBCUBudgetEnquiry(
                        lines.getAccountingCombination(), lines.getAmount(),
                        invoice.getEfinBudgetint(), posted, Boolean.TRUE);
                  }

                }
              }

            } else {
              if (invoice.getEfinManualencumbrance() != null) {
                EfinBudgetManencum costEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                    invoice.getEfinManualencumbrance().getId());
                if (costEncum != null) {
                  for (EfinBudgetManencumlines lines : costEncum.getEfinBudgetManencumlinesList()) {
                    @SuppressWarnings("unused")
                    PurchaseInvoiceSubmitUtils utils = new PurchaseInvoiceSubmitUtils(invoice);

                    // update department level funds ='N' and except 990,999 dept
                    PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(
                        lines.getAccountingCombination(), lines.getAmount(),
                        invoice.getEfinBudgetint(), posted, Boolean.TRUE);

                    // update department level funds ='Y' and 990,999 dept
                    // --999 dept
                    PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(lines.getAccountingCombination(),
                        lines.getAmount(), invoice.getEfinBudgetint(), posted, Boolean.TRUE);

                    if (!posted) {

                      // --990 dept
                      PurchaseInvoiceSubmitUtils.updateBCUBudgetEnquiry(
                          lines.getAccountingCombination(), lines.getAmount(),
                          invoice.getEfinBudgetint(), posted, Boolean.TRUE);
                    }
                  }
                }
              }
            }

          } else {

            @SuppressWarnings("unused")
            PurchaseInvoiceSubmitUtils utils = new PurchaseInvoiceSubmitUtils(invoice);
            for (Map.Entry<String, BigDecimal> entry : reservationMap.entrySet()) {
              combination = Utility.getObject(AccountingCombination.class, entry.getKey());

              if (PPI_DOCUMENT.equals(strInvoiceType)) {
                posted = false;
              }

              // Update encumbrance in budget enquiry if deptfund N
              PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(combination, entry.getValue(),
                  invoice.getEfinBudgetint(), posted, Boolean.TRUE);

              // update department level funds ='Y' and 990,999 dept
              PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(combination, entry.getValue(),
                  invoice.getEfinBudgetint(), posted, Boolean.TRUE);

              if (!posted) {
                // --990 dept
                PurchaseInvoiceSubmitUtils.updateBCUBudgetEnquiry(combination, entry.getValue(),
                    invoice.getEfinBudgetint(), posted, Boolean.TRUE);
              }
            }

            OBDal.getInstance().flush();

          }

          EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
              invoice.getEfinManualencumbrance().getId());
          if (encumbrance != null) {
            encumbrance.setUpdated(new java.util.Date());
            encumbrance.setUpdatedBy(OBContext.getOBContext().getUser());
            encumbrance.setDocumentStatus("CA");
            OBDal.getInstance().save(encumbrance);
            if (RDV_DOCUMENT.equals(strInvoiceType)) {
              if (invoice.getEfinRdvtxnline() != null) {
                invoice.getEfinRdvtxnline().setTxnverStatus("DR");
                invoice.getEfinRdvtxnline().setApprovalStatus("DR");
                invoice.getEfinRdvtxnline().setAction("CO");
                invoice.getEfinRdvtxnline().setAmarsaraf(false);
              } else {
                invoice.getEfinRdvtxn().setTxnverStatus("DR");
                invoice.getEfinRdvtxn().setAppstatus("DR");
                invoice.getEfinRdvtxn().setAction("CO");
                invoice.getEfinRdvtxn().setAmarsaraf(false);
                if (invoice.getEfinRdvtxn().isAdvancetransaction()
                    && invoice.getEfinRdvtxn().getEfinRDVTxnlineList().size() > 0) {
                  EfinRDVTxnline txnLineObj = invoice.getEfinRdvtxn().getEfinRDVTxnlineList()
                      .get(0);
                  txnLineObj.setAmarsaraf(false);
                  txnLineObj.setTxnverStatus("DR");
                  txnLineObj.setInvoice(null);
                  OBDal.getInstance().save(txnLineObj);
                }
              }
            }
            OBDal.getInstance().flush();
          }
        }

        if (invoice.getEfinFundsEncumbrance() != null) {
          EfinBudgetManencum fundsencumbrance = Utility.getObject(EfinBudgetManencum.class,
              invoice.getEfinFundsEncumbrance().getId());
          if (fundsencumbrance != null) {
            fundsencumbrance.setUpdated(new java.util.Date());
            fundsencumbrance.setUpdatedBy(OBContext.getOBContext().getUser());
            fundsencumbrance.setDocumentStatus("CA");
            OBDal.getInstance().save(fundsencumbrance);
            OBDal.getInstance().flush();
          }
        }
      }

      // update the applied prepayment table for ap Prepayment Application
      if (PPA_DOCUMENT.equals(strInvoiceType)) {
        invoice.setDocumentStatus("EFIN_CA");
        OBDal.getInstance().save(invoice);

        sql = " select apppay.efin_applied_prepayment_id, apppay.applied_amount,appinv.em_efin_pre_usedamount as usedamount, "
            + " appinv.em_efin_pre_remainingamount as remainamt,appinv.c_invoice_id "
            + " from efin_applied_prepayment apppay join c_invoice appinv on apppay.efin_applied_invoice=appinv.c_invoice_id "
            + " where  apppay.c_invoice_id='" + invoice.getId() + "'";

        ps = conn.prepareStatement(sql);
        rs = ps.executeQuery();
        while (rs.next()) {

          Invoice apprepayInvoice = Utility.getObject(Invoice.class, rs.getString("c_invoice_id"));
          BigDecimal appliedAmt = new BigDecimal(rs.getInt("applied_amount"));

          if (new BigDecimal(rs.getInt("usedamount")).compareTo(BigDecimal.ZERO) == 1) {

            apprepayInvoice
                .setEfinPreUsedamount(apprepayInvoice.getEfinPreUsedamount().subtract(appliedAmt));
            apprepayInvoice.setEfinPreRemainingamount(
                apprepayInvoice.getEfinPreRemainingamount().add(appliedAmt));

            OBDal.getInstance().save(apprepayInvoice);

            // remove the records in applyprepayment invoice
            AppliedPrepaymentInvoice prepayinvoice = OBDal.getInstance()
                .get(AppliedPrepaymentInvoice.class, rs.getString("efin_applied_prepayment_id"));

            OBDal.getInstance().remove(prepayinvoice);

            Map<String, BigDecimal> invoiceReservationAmount = getInvoiceLineAmount(
                invoice.getInvoiceLineList());

            EfinBudgetManencumlines encumbranceLine = null;
            for (Map.Entry<String, BigDecimal> entry : invoiceReservationAmount.entrySet()) {
              encumbranceLine = PurchaseInvoiceSubmitUtils
                  .getEncumbranceLine(invoice.getEfinManualencumbrance().getId(), entry.getKey());
              if (encumbranceLine != null) {
                updateEncumbranceInvoiceRef(encumbranceLine, entry.getValue(), posted, invoice);

                revertAppliedAmount(encumbranceLine, entry.getValue(), posted, invoice);
              }
            }

            OBDal.getInstance().flush();

          }
        }
      }

      // update the applied prepayment table for ap Prepayment invoice
      if (PPI_DOCUMENT.equals(strInvoiceType)) {

        sql = " select apppay.efin_applied_prepayment_id, apppay.applied_amount,appinv.em_efin_pre_usedamount as usedamount, "
            + " appinv.em_efin_pre_remainingamount as remainamt,apppay.c_invoice_id "
            + " from efin_applied_prepayment apppay join c_invoice appinv on apppay.efin_applied_invoice=appinv.c_invoice_id "
            + " where  apppay.efin_applied_invoice='" + invoice.getId() + "'";

        ps = conn.prepareStatement(sql);
        log.debug("psapplied:" + ps.toString());
        rs = ps.executeQuery();
        while (rs.next()) {

          Invoice apprepayInvoice = Utility.getObject(Invoice.class, rs.getString("c_invoice_id"));

          if (apprepayInvoice.getPosted() != null && apprepayInvoice.getPosted().equals("Y")
              && PPA_DOCUMENT.equals(strInvoiceType)) {
            // create the reverse journal entries
            reversejournal(vars, conn, clientId, orgId, roleId, userId, apprepayInvoice, null);
            // delete the entries in budget actual
            // reverseActual(vars, clientId, orgId, roleId, userId, apprepayInvoice);
          }

          // reverse the reservation
          reverseReservation(vars, clientId, orgId, roleId, userId, apprepayInvoice, comments);

          // update the invoice Header
          updateInvHeader(apprepayInvoice);
          PurchaseInvoiceSubmit.insertInvoiceApprover(apprepayInvoice, comments, "CA", null);
        }
      }
      // for AP Prepayment Invoice
      if (invoice.getEfinEncumtype().equals("M") && invoice.getEfinManualencumbrance() != null
          && invoice.getEfinDistribution() != null && PPI_DOCUMENT.equals(strInvoiceType)) {

        GrandTotal = invoice.getGrandTotalAmount();
        if (!invoice.getCurrency().getId()
            .equals(invoice.getOrganization().getCurrency().getId())) {
          GrandTotal = conversionrate.multiply(GrandTotal);
        }

        List<EfinManualEncumInvoice> reservedInvoices = PurchaseInvoiceSubmitUtils
            .getReservedInvoices(invoice.getId());
        for (EfinManualEncumInvoice inv : reservedInvoices) {
          inv.getManualEncumbranceLines().getEfinManualEncumInvoiceList().clear();
          OBDal.getInstance().remove(inv);

        }

        // update manual encumbrance amt
        EfinBudgetManencum headermanencum = OBDal.getInstance().get(EfinBudgetManencum.class,
            invoice.getEfinManualencumbrance().getId());
        headermanencum.setUpdated(new java.util.Date());
        headermanencum.setUpdatedBy(OBContext.getOBContext().getUser());
        headermanencum.setUsedamount(headermanencum.getUsedamount().subtract(GrandTotal));
        headermanencum.setRemainingamt(headermanencum.getRemainingamt().add(GrandTotal));

        OBDal.getInstance().save(headermanencum);
        OBDal.getInstance().flush();
      }

      return true;
    } catch (final Exception e) {
      log.error("Exception in reverseReservation :", e);

      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * While cancelling the prepayment application, we should change the paid ='N' and update the
   * invoice column = prepaymentinvoiceid
   * 
   * @param encumbranceLine
   * @param value
   * @param posted
   * @param invoice
   */

  private void updateEncumbranceInvoiceRef(EfinBudgetManencumlines encumbranceLine,
      BigDecimal value, Boolean posted, Invoice invoice) {

    for (EfinManualEncumInvoice invRef : encumbranceLine.getEfinManualEncumInvoiceList()) {
      if (invRef.isPaymentComplete()) {
        invRef.setInvoice(invRef.getPrepaymeninvoice());
        invRef.setPaymentComplete(false);
        OBDal.getInstance().save(invRef);
      }
    }

  }

  /**
   * Ths method is used to revert applied amount
   * 
   * @param encumbranceLine
   * @param encumberedAmount
   * @param posted
   * @param invoice
   */
  private void revertAppliedAmount(EfinBudgetManencumlines encumbranceLine,
      BigDecimal encumberedAmount, Boolean posted, Invoice invoice) {
    try {

      encumbranceLine.setUsedAmount(encumbranceLine.getUsedAmount().subtract(encumberedAmount));
      encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(encumberedAmount));

      OBDal.getInstance().save(encumbranceLine);
      OBDal.getInstance().flush();
      if (posted) {
        // update department level funds ='N' and except 990,999 dept

        PurchaseInvoiceSubmitUtils.updateDeptNBudgetEnquiry(
            encumbranceLine.getAccountingCombination(), encumberedAmount,
            invoice.getEfinBudgetint(), posted, Boolean.TRUE);

        // update department level funds ='Y' and 990,999 dept

        PurchaseInvoiceSubmitUtils.updateBudgetEnquiry(encumbranceLine.getAccountingCombination(),
            encumberedAmount, invoice.getEfinBudgetint(), posted, Boolean.TRUE);

        PurchaseInvoiceSubmitUtils.updateBCUBudgetEnquiry(
            encumbranceLine.getAccountingCombination(), encumberedAmount,
            invoice.getEfinBudgetint(), posted, Boolean.TRUE);
      }

    } catch (Exception e) {
      log.error("Exception in revertAppliedAmount :", e);

    }
  }

  /**
   * This method is used to get invoice line amount
   * 
   * @param invoiceLineList
   * @return
   */
  private Map<String, BigDecimal> getInvoiceLineAmount(List<InvoiceLine> invoiceLineList) {
    Map<String, BigDecimal> lineAmountMap = new HashMap<String, BigDecimal>();
    try {

      String strValidCombinationId = "";
      BigDecimal lineNetAmount = BigDecimal.ZERO;

      if (invoiceLineList.size() > 0) {
        for (InvoiceLine line : invoiceLineList) {

          lineNetAmount = BigDecimal.ZERO;
          strValidCombinationId = line.getEfinCValidcombination().getId();

          if (lineAmountMap.containsKey(strValidCombinationId)) {

            lineNetAmount = lineAmountMap.get(strValidCombinationId);
            lineNetAmount = lineNetAmount.add(line.getLineNetAmount());
            lineAmountMap.put(strValidCombinationId, lineNetAmount);

          } else {
            lineAmountMap.put(strValidCombinationId, line.getLineNetAmount());
          }
        }
      }
    } catch (Exception e) {

      log.error("Exception in getInvoiceLineAmount :", e);

    }
    return lineAmountMap;
  }

  /**
   * This method is used to get unique codeF reservation amountF
   * 
   * @param encumbranceVId
   * @return
   */
  private Map<String, BigDecimal> getUniqueCodeReservationAmount(String encumbranceVId) {
    Map<String, BigDecimal> lineReservations = new HashMap<String, BigDecimal>();
    BigDecimal reservedAmount = BigDecimal.ZERO;

    try {

      EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class, encumbranceVId);
      if (encumbrance != null) {

        for (EfinBudgetManencumlines line : encumbrance.getEfinBudgetManencumlinesList()) {

          reservedAmount = getReservedAmount(line);
          lineReservations.put(line.getAccountingCombination().getId(), reservedAmount);

        }
      }
    } catch (Exception e) {

      log.error("Exception in getUniqueCodeReservationAmount :", e);
    }
    return lineReservations;
  }

  /**
   * This method is used to get reserved amountF
   * 
   * @param line
   * @return
   */
  private BigDecimal getReservedAmount(EfinBudgetManencumlines line) {
    BigDecimal reservedAmount = BigDecimal.ZERO;
    try {
      reservedAmount = line.getEfinManualEncumInvoiceList().stream().map(a -> a.getInvamount())
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    } catch (Exception e) {
      log.error("Exception in getReservedAmount :", e);
    }
    return reservedAmount;
  }

  /**
   * This method is used to update invoice header
   * 
   * @param invoice
   * @return
   */
  public int updateInvHeader(Invoice invoice) {
    String headerId = null;
    try {
      OBContext.setAdminMode(true);
      Invoice header = OBDal.getInstance().get(Invoice.class, invoice.getId());

      header.setUpdated(new java.util.Date());
      header.setUpdatedBy(OBContext.getOBContext().getUser());
      header.setDocumentStatus("EFIN_CA");
      header.setEfinPreRemainingamount(BigDecimal.ZERO);
      header.setEutNextRole(null);
      OBDal.getInstance().save(header);
      headerId = header.getId();
      if (headerId != null)
        return 1;
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception in insertBudgetApprover: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

  /**
   * This method is used to remive prepay invoice
   * 
   * @param invoice
   * @return
   */
  public int removePrepayInvoice(Invoice invoice) {

    try {
      if (invoice.getTransactionDocument().isEfinIsprepayinv()) {
        OBQuery<EfinPrepaymentInvoice> prepayinvoice = OBDal.getInstance().createQuery(
            EfinPrepaymentInvoice.class,
            " manualEncumbrance.id= '" + invoice.getEfinManualencumbrance().getId() + "'");

        for (EfinPrepaymentInvoice vo : prepayinvoice.list()) {
          OBDal.getInstance().remove(vo);
        }
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      log.error("Exception in insertBudgetApprover: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }
    return 1;
  }

  /**
   * This method is used to reserve actual
   * 
   * @param invoice
   * @return
   */
  public boolean reverseActual(Invoice invoice) {
    try {
      // remove the entry in actual
      OBContext.setAdminMode();
      OBQuery<EfinBudgetActual> actual = OBDal.getInstance().createQuery(EfinBudgetActual.class,
          " invoice.id='" + invoice.getId() + "'");

      if (actual.list().size() > 0) {
        for (EfinBudgetActual act : actual.list()) {
          EFINBudgetLines budgetlines = OBDal.getInstance().get(EFINBudgetLines.class,
              act.getBudgetLines().getId());
          budgetlines.setAmountSpent(budgetlines.getAmountSpent().subtract(act.getAmount()));
          budgetlines.setFundsAvailable(budgetlines.getFundsAvailable().add(act.getAmount()));
          OBDal.getInstance().save(budgetlines);
          OBDal.getInstance().flush();
          if (invoice.getEfinEncumtype().equals("M")) {
            OBQuery<EfinEncumInvoiceRef> ref = OBDal.getInstance().createQuery(
                EfinEncumInvoiceRef.class, " efinBudgetActual.id='" + act.getId() + "'");
            log.debug("manuallines:" + ref.list().size());
            if (ref.list().size() > 0) {
              for (EfinEncumInvoiceRef encuminvoice : ref.list()) {
                log.debug("encuminvoice:" + encuminvoice.getEncumbranceTransaction().getId());
                OBQuery<efinbudgetencum> encum = OBDal.getInstance().createQuery(
                    efinbudgetencum.class,
                    " id='" + encuminvoice.getEncumbranceTransaction().getId() + "'");
                if (encum.list().size() > 0) {
                  for (efinbudgetencum encumline : encum.list()) {
                    efinbudgetencum enm = OBDal.getInstance().get(efinbudgetencum.class,
                        encumline.getId());
                    enm.setUpdated(new java.util.Date());
                    enm.setUpdatedBy(OBContext.getOBContext().getUser());
                    log.debug("getAmount:" + enm.getAmount());
                    log.debug("getAmountact:" + act.getAmount());
                    enm.setAmount(enm.getAmount().add(encuminvoice.getManexpamount()));
                    log.debug("getAmountafter:" + enm.getAmount());
                    OBDal.getInstance().save(enm);

                  }
                }

                else {
                  efinbudgetencum efinencum = OBProvider.getInstance().get(efinbudgetencum.class);
                  efinencum
                      .setClient(OBDal.getInstance().get(Client.class, act.getClient().getId()));
                  efinencum.setOrganization(
                      OBDal.getInstance().get(Organization.class, act.getOrganization().getId()));
                  efinencum.setActive(true);
                  efinencum.setUpdatedBy(
                      OBDal.getInstance().get(User.class, act.getCreatedBy().getId()));
                  efinencum.setCreationDate(new java.util.Date());
                  efinencum.setCreatedBy(
                      OBDal.getInstance().get(User.class, act.getCreatedBy().getId()));
                  efinencum.setUpdated(new java.util.Date());
                  efinencum.setAmount(encuminvoice.getManexpamount());
                  log.debug("efinencumgetAmounta:" + efinencum.getAmount());
                  efinencum.setTransactionDate(act.getTrxdate());
                  efinencum.setAccountingDate(act.getAccountingDate());
                  efinencum.setDescription(act.getDescription());
                  efinencum.setManualEncumbranceLines(
                      OBDal.getInstance().get(EfinBudgetManencumlines.class,
                          encuminvoice.getManualEncumbranceLines().getId()));
                  efinencum.setBudgetLines(
                      OBDal.getInstance().get(EFINBudgetLines.class, act.getBudgetLines().getId()));
                  efinencum.setAppstatus("APP");
                  efinencum.setDoctype("MEI");
                  efinencum.setUniqueCode(act.getUniqueCode());
                  efinencum.setManualEncumbrance(OBDal.getInstance().get(EfinBudgetManencum.class,
                      invoice.getEfinManualencumbrance().getId()));
                  efinencum.setInvoice(OBDal.getInstance().get(Invoice.class, invoice.getId()));
                  OBDal.getInstance().save(efinencum);
                  log.debug("efinencum:" + efinencum.getId());
                  OBDal.getInstance().flush();
                }
                OBDal.getInstance().remove(encuminvoice);
                OBDal.getInstance().flush();
              }
            }

          }
          /*
           * else if(invoice.getEfinEncumtype().equals("M")) { OBQuery<EfinEncumInvoiceRef> ref =
           * OBDal.getInstance().createQuery(EfinEncumInvoiceRef.class, " efinBudgetActual.id='" +
           * act.getId() + "'"); for (EfinEncumInvoiceRef encuminvoice : ref.list()) {
           * OBDal.getInstance().remove(encuminvoice); } }
           */
          OBDal.getInstance().remove(act);
        }
        OBDal.getInstance().flush();
      }
      return true;
    }

    catch (final Exception e) {
      log.error("Exception in reverseReservation :", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to reserve journal
   * 
   * @param vars
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param invoice
   * @param transaction
   * @return
   */
  @SuppressWarnings("unused")
  public boolean reversejournal(VariablesSecureApp vars, Connection con, String clientId,
      String orgId, String roleId, String userId, Invoice invoice,
      FIN_FinaccTransaction transaction) {
    String sql = null, factAcctGroupId = null, description = "";
    // description = " Reversal of " + invoice.getDocumentNo();
    PreparedStatement ps = null;
    ResultSet rs = null;
    int count = 0;
    try {
      factAcctGroupId = SequenceIdData.getUUID();

      // create reverse journal entry IN FACT ACCT
      sql = " select fact_acct_id,em_efin_uniquecode,gl_category_id, acctdescription, ad_client_id,ad_org_id,isactive,created,createdby,updated, updatedby,c_acctschema_id,account_id,datetrx,dateacct,c_period_id,ad_table_id,record_id,line_id, "
          + "postingtype,c_currency_id,amtsourcedr,amtsourcecr,amtacctdr,amtacctcr,c_bpartner_id,ad_orgtrx_id,c_salesregion_id,c_project_id,c_campaign_id,c_activity_id,user1_id,user2_id,fact_acct_group_id,seqno, "
          + "factaccttype,docbasetype,acctvalue,record_id2,c_doctype_id,description "
          + " from fact_Acct where ad_client_id ='" + clientId + "' and ad_org_id='" + orgId + "'";
      if (invoice != null && transaction == null)
        sql += " and record_id='" + invoice.getId() + "'";
      else if (invoice != null && transaction != null)
        sql += " and record_id='" + transaction.getId() + "'";
      ps = con.prepareStatement(sql);
      log.debug("reversejournal:" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        sql = "	insert into fact_acct(fact_acct_id,  ad_client_id,ad_org_id,isactive,created,createdby,updated,updatedby,c_acctschema_id,account_id,datetrx,dateacct,c_period_id,ad_table_id,record_id,line_id, "
            + "						postingtype,c_currency_id,amtsourcedr,amtsourcecr,amtacctdr,amtacctcr,c_bpartner_id,ad_orgtrx_id,c_salesregion_id,c_project_id,c_campaign_id,c_activity_id,user1_id,user2_id,fact_acct_group_id,seqno, "
            + "					factaccttype,docbasetype,acctvalue,record_id2,c_doctype_id,description,em_efin_uniquecode,acctdescription,gl_category_id)"
            + "	values (get_uuid(),	?,?,'Y',now(),?,now(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        ps = con.prepareStatement(sql);
        ps.setString(1, rs.getString("ad_client_id"));
        ps.setString(2, rs.getString("ad_org_id"));
        ps.setString(3, rs.getString("createdby"));
        ps.setString(4, rs.getString("updatedby"));
        ps.setString(5, rs.getString("c_acctschema_id"));
        ps.setString(6, rs.getString("account_id"));
        ps.setTimestamp(7, new Timestamp(new java.util.Date().getTime()));
        ps.setTimestamp(8, new Timestamp(new java.util.Date().getTime()));
        ps.setString(9, rs.getString("c_period_id"));
        ps.setString(10, rs.getString("ad_table_id"));
        ps.setString(11, rs.getString("record_id"));
        ps.setString(12, rs.getString("line_id"));
        ps.setString(13, rs.getString("postingtype"));
        ps.setString(14, rs.getString("c_currency_id"));
        ps.setBigDecimal(15, rs.getBigDecimal("amtsourcecr"));
        ps.setBigDecimal(16, rs.getBigDecimal("amtsourcedr"));
        ps.setBigDecimal(17, rs.getBigDecimal("amtacctcr"));
        ps.setBigDecimal(18, rs.getBigDecimal("amtacctdr"));
        ps.setString(19, rs.getString("c_bpartner_id"));
        ps.setString(20, rs.getString("ad_orgtrx_id"));
        ps.setString(21, rs.getString("c_salesregion_id"));
        ps.setString(22, rs.getString("c_project_id"));
        ps.setString(23, rs.getString("c_campaign_id"));
        ps.setString(24, rs.getString("c_activity_id"));
        ps.setString(25, rs.getString("user1_id"));
        ps.setString(26, rs.getString("user2_id"));
        ps.setString(27, factAcctGroupId);
        ps.setInt(28, rs.getInt("seqno"));
        ps.setString(29, rs.getString("factaccttype"));
        /*
         * if (invoice != null && invoice.getTransactionDocument().isEfinIsprepayinvapp())
         * ps.setString(30, "API"); else
         */
        ps.setString(30, "PRJ");
        ps.setString(31, rs.getString("acctvalue"));
        ps.setString(32, rs.getString("record_id2"));
        ps.setString(33, rs.getString("c_doctype_id"));
        ps.setString(34, " Reversal of " + rs.getString("description"));
        ps.setString(35, rs.getString("em_efin_uniquecode"));
        ps.setString(36, rs.getString("acctdescription"));
        ps.setString(37, rs.getString("gl_category_id"));
        log.debug("insertjournal:" + ps.toString());
        count = ps.executeUpdate();

      }
      log.debug("countjournal:" + count);

      if (count > 1)
        return true;
    }

    catch (final Exception e) {
      log.error("Exception in reverseReservation :", e);
      return false;
    }
    return false;
  }

  /**
   * returns a list of prepayment application invoices for a particular prepayment.
   * 
   * @param prepayment
   *          prepayment invoice
   * @return {@link List} list of prepayment applications
   */

  public static List<AppliedPrepaymentInvoice> getAppliedPrepayments(Invoice prepayment) {
    List<AppliedPrepaymentInvoice> appliedPrepayments = new ArrayList<AppliedPrepaymentInvoice>();

    try {
      OBQuery<AppliedPrepaymentInvoice> appliedInvoicesQuery = OBDal.getInstance()
          .createQuery(AppliedPrepaymentInvoice.class, "efinAppliedInvoice.id= :prepaymentId");

      appliedInvoicesQuery.setNamedParameter("prepaymentId", prepayment.getId());
      appliedPrepayments = appliedInvoicesQuery.list();

    } catch (Exception e) {
      log.error("Exception in getAppliedPrepayments :", e);
    }

    return appliedPrepayments;
  }

  /**
   *
   * Check if any of the prepayment application invoice is waiting for approval
   * 
   * @param appliedPrepayments
   *          list of prepayment application records
   * @return {@link Boolean} true if there is any invoice waiting for approval
   * 
   */

  public static Boolean hasInProcessInvoices(List<AppliedPrepaymentInvoice> appliedPrepayments) {
    Boolean hasInProcessInvoices = Boolean.FALSE;
    try {

      if (appliedPrepayments.size() > 0) {

        for (AppliedPrepaymentInvoice ppaInvoice : appliedPrepayments) {
          Invoice applyinv = ppaInvoice.getInvoice();

          if (applyinv.getDocumentStatus().equals("EFIN_WFA")) {
            hasInProcessInvoices = Boolean.TRUE;

            return hasInProcessInvoices;
          }
        }
      }
    } catch (Exception e) {

      log.error("Exception in getAppliedPrepayments :", e);

    }

    return hasInProcessInvoices;
  }
}