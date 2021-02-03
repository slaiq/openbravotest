package sa.elm.ob.finance.ad_callouts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BankAccount;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.ad_callouts.dao.RdvLineCalloutDAO;
import sa.elm.ob.scm.event.PoReceiptEventDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gowtham V
 */

public class RdvHeaderCallout extends SimpleCallout {

  /**
   * Callout to update the Po related fields
   */
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(RdvHeaderCallout.class);

  @SuppressWarnings("unused")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String orderID = vars.getStringParameter("inpcOrderId");
    String inOutID = vars.getStringParameter("inpmInoutId");
    String bpBankID = vars.getStringParameter("inpcBpBankaccountId");
    String txnType = vars.getStringParameter("inptxnType");
    String txndate = vars.getStringParameter("inptxnDate");
    String txndategreg = vars.getStringParameter("inptxndategre");
    String inpadClientId = vars.getStringParameter("inpadClientId");
    String budgInitialId = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    String inpCurrency = "", contractDate = "", poDate = "", encumId = "", extAmendEndDate = "";
    try {
      if (inpLastFieldChanged.equals("inpadOrgId") || inpLastFieldChanged.equals("inptxnDate")) {

        if (!StringUtils.isNotEmpty(txndate)) {
          info.addResult("inptxndategre", "");
        }
        // To check Txn date is valid date format or not
        if (txndate.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")
            && UtilityDAO.Checkhijridate(txndate)) {
          String txnGreg = UtilityDAO.convertToGregorian_tochar(txndate);
          info.addResult("inptxndategre", txnGreg);
        } else {
          info.addResult("inptxndategre", "");
          info.addResult("ERROR", OBMessageUtils.messageBD("Efin_invalidtxndategreg"));
        }

        String dateacct = UtilityDAO.convertToGregorian(txndate);
        Date endDate = dateFormat.parse(dateacct);

        LOG.debug("endDate:" + dateFormat.format(endDate));
        // getting budget initial id based on transaction date
        budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            inpadClientId, "");
        if (budgInitialId != null)
          info.addResult("inpefinBudgetintId", budgInitialId);
        else
          info.addResult("inpefinBudgetintId", null);

      }

      if (inpLastFieldChanged.equals("inptxndategre")) {
        String day, month, year;
        if (!StringUtils.isNotEmpty(txndategreg)) {
          info.addResult("inptxnDate", "");
        }
        // To check Txn date(greg) is valid date format or not
        if (txndategreg.matches("([0-9]{2})-([0-9]{2})-([0-9]{4})")) {
          String[] hijiriDate = txndategreg.split("-");// splits the string based on "-"
          day = hijiriDate[0];
          month = hijiriDate[1];
          year = hijiriDate[2];
          if (UtilityDAO.isGregorianDateValid(day, month, year)) {
            txndategreg = year + "-" + month + "-" + day;
            String txnHijiri = UtilityDAO.convertTohijriDate(txndategreg);
            info.addResult("inptxnDate", txnHijiri);
          } else {
            info.addResult("inptxnDate", "");
            info.addResult("ERROR", OBMessageUtils.messageBD("Efin_invalidtxndategreg"));
          }

        } else {
          info.addResult("inptxnDate", "");
          info.addResult("ERROR", OBMessageUtils.messageBD("Efin_invalidtxndategreg"));
        }

      }

      // if po is selected then related fields from po we need to get.
      if (inpLastFieldChanged.equals("inpcOrderId")) {
        Order order = OBDal.getInstance().get(Order.class, orderID);
        info.addResult("inpadOrgId", order.getOrganization().getId());
        if (order.getEscmContractenddate() != null) {
          contractDate = UtilityDAO.convertTohijriDate(order.getEscmContractenddate().toString());
        }
        poDate = UtilityDAO.convertTohijriDate(order.getOrderDate().toString());
        inpCurrency = order.getEscmCurrency() == null ? "317" : order.getEscmCurrency().getId();
        encumId = order.getEfinBudgetManencum() == null ? ""
            : order.getEfinBudgetManencum().getId();
        info.addResult("inpcBpartnerId", order.getBusinessPartner().getId());
        info.addResult("inpefinBudgetManencumId", encumId);
        info.addResult("inpexpDate", contractDate);
        info.addResult("inppoStatus", order.getEscmAppstatus());
        info.addResult("inppoType", order.getEscmOrdertype());
        info.addResult("inppoDate", poDate);
        info.addResult("inppoDategreg",
            (order.getOrderDate().toString()).substring(8, 10) + "-"
                + (order.getOrderDate().toString()).substring(5, 7) + "-"
                + (order.getOrderDate().toString()).substring(0, 4));
        info.addResult("inpcCurrencyId", inpCurrency);
        info.addResult("inpcontractDuration", order.getEscmContractduration());
        info.addResult("inpperiodType", order.getEscmPeriodtype());
        info.addResult("inpcontractAmt", order.getGrandTotalAmount());
        BankAccount bpBank = order.getBusinessPartner().getBusinessPartnerBankAccountList().get(0);

        if (order.getEscmIban() != null) {
          info.addResult("inpcBpBankaccountId", order.getEscmIban().getId());
          if (order.getEscmIban().getEfinBank() != null) {
            info.addResult("inpefinBankId", order.getEscmIban().getEfinBank().getId());
          }
          info.addResult("inpiban", order.getEscmIban().getIBAN());
        }
        /*
         * else { info.addResult("inpcBpBankaccountId", bpBank.getId()); if (bpBank.getEfinBank() !=
         * null) { info.addResult("inpefinBankId", bpBank.getEfinBank().getId()); }
         * info.addResult("inpiban", bpBank.getIBAN()); }
         */

        info.addResult("inpadvDeductPercent", order.getEscmAdvpaymntPercntge());
        if (order.getEscmTaxMethod() != null && order.isEscmIstax()) {
          info.addResult("inpefinTaxMethodId", order.getEscmTaxMethod().getId());
        }
        Date extEndDate = RdvLineCalloutDAO.getPOAmendmentExtDate(order.getId());
        if (extEndDate != null) {
          extAmendEndDate = UtilityDAO.convertTohijriDate(extEndDate.toString());
          info.addResult("inpextendedExpDate", extAmendEndDate);
        }
        //contract category
        if(order.getEscmContactType()!=null)
          info.addResult("inpcontractCategory", order.getEscmContactType().getId());
          
      }
      /*
       * if (bpBankID != null) { BankAccount bpBank = OBDal.getInstance().get(BankAccount.class,
       * bpBankID);
       * 
       * info.addResult("inpefinBankId", bpBank.getEfinBank().getId()); info.addResult("inpiban",
       * bpBankID); }
       */

      // if po is selected then related fields from po we need to get.
      if (inpLastFieldChanged.equals("inpmInoutId")) {
        ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, inOutID);
        info.addResult("inpadOrgId", inout != null ? inout.getOrganization().getId() : null);
        if (inout != null) {
          info.addResult("inpcBpartnerId", inout.getBusinessPartner().getId());
          BankAccount bpBank = inout.getBusinessPartner().getBusinessPartnerBankAccountList()
              .get(0);

          info.addResult("inpcBpBankaccountId", bpBank.getId());
          if (bpBank.getEfinBank() != null) {
            info.addResult("inpefinBankId", bpBank.getEfinBank().getId());
          }
          info.addResult("inpiban", bpBank.getIBAN());
          if (txnType.equals("POS")) {
            Order order = PoReceiptEventDAO.getOrder(inout.getSalesOrder().getId());
            encumId = order.getEfinBudgetManencum() == null ? ""
                : order.getEfinBudgetManencum().getId();
            if (order.getEscmOrdertype().equals("CR")) {
              contractDate = UtilityDAO
                  .convertTohijriDate(order.getEscmContractenddate().toString());
            }
            poDate = UtilityDAO.convertTohijriDate(order.getOrderDate().toString());
            inpCurrency = order.getEscmCurrency() == null ? "317" : order.getEscmCurrency().getId();

            info.addResult("inpefinBudgetManencumId", encumId);
            info.addResult("inpexpDate", contractDate);
            info.addResult("inppoStatus", order.getEscmAppstatus());
            info.addResult("inppoType", order.getEscmOrdertype());
            info.addResult("inppoDate", poDate);
            info.addResult("inppoDategreg",
                (order.getOrderDate().toString()).substring(8, 10) + "-"
                    + (order.getOrderDate().toString()).substring(5, 7) + "-"
                    + (order.getOrderDate().toString()).substring(0, 4));
            info.addResult("inpcCurrencyId", inpCurrency);
            info.addResult("inpcontractDuration", order.getEscmContractduration());
            info.addResult("inpperiodType", order.getEscmPeriodtype());
            info.addResult("inpcOrderId", order.getId());
            info.addResult("inpcontractAmt", order.getGrandTotalAmount());
            info.addResult("inpadvDeductPercent", order.getEscmAdvpaymntPercntge());
            Date extEndDate = RdvLineCalloutDAO.getPOAmendmentExtDate(order.getId());
            if (extEndDate != null) {
              extAmendEndDate = UtilityDAO.convertTohijriDate(extEndDate.toString());
              info.addResult("inpextendedExpDate", extAmendEndDate);
            }
          }
        } else {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Bpartner_ID').setValue('')");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('C_Bp_Bankaccount_ID').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Efin_Bank_ID').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Iban').setValue('')");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('Efin_Budget_Manencum_ID').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EXP_Date').setValue(null)");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('PO_Status').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('PO_Type').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('PO_Date').setValue(null)");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('PO_Dategreg').setValue(null)");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Currency_ID').setValue('')");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('Contract_Duration').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Period_Type').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('M_Inout_ID').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Contract_Amt').setValue(0)");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('ADV_Deduct_Percent').setValue(0)");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Order_ID').setValue('')");

        }
      }

      // for relevant bank details should assigned.
      if (inpLastFieldChanged.equals("inpcBpBankaccountId")) {
        BankAccount bpBank = OBDal.getInstance().get(BankAccount.class, bpBankID);

        info.addResult("inpefinBankId", bpBank.getEfinBank().getId());
        info.addResult("inpiban", bpBank.getIBAN());
      }

      if (inpLastFieldChanged.equals("inptxnType")) {
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Order_ID').setValue('')");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Bpartner_ID').setValue('')");
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('Efin_Budget_Manencum_ID').setValue('')");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EXP_Date').setValue(null)");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('PO_Status').setValue('')");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('PO_Type').setValue('')");

        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('PO_Date').setValue(null)");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('PO_Dategreg').setValue(null)");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Currency_ID').setValue('')");

        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('C_Bp_Bankaccount_ID').setValue('')");

        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Efin_Bank_ID').setValue('')");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Iban').setValue('')");
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('Contract_Duration').setValue('')");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Period_Type').setValue('')");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('M_Inout_ID').setValue('')");
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Contract_Amt').setValue(0)");
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('ADV_Deduct_Percent').setValue(0)");

      }

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in insertBudegtLines() " + e, e);
      }
    }
  }
}
