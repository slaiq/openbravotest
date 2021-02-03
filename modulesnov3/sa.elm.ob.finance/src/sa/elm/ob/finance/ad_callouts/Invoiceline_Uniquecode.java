package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.utils.FormatUtilities;

import sa.elm.ob.finance.EFIN_TaxMethod;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.vat.TaxLineHandlerImpl;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopinagh.R
 */

public class Invoiceline_Uniquecode extends SimpleCallout {

  private static final long serialVersionUID = 1L;
  @SuppressWarnings("unused")
  private static final RequestFilter filterYesNo = new ValueListFilter("Y", "N");

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpSalestransaction = vars.getStringParameter("inpissotrx");
    String inpReceiptType = vars.getStringParameter("inpemEfinReceiptType");
    String inpPrePaymentInvoice = vars.getStringParameter("inpemEfinPrepayment");
    String validcombination = vars.getStringParameter("inpemEfinCValidcombinationId");
    String budgetinitial = vars.getStringParameter("inpemEfinBudgetintId");
    String inpQtyInv = vars.getStringParameter("inpqtyinvoiced");
    String inpPrcActual = vars.getStringParameter("inppriceactual");
    String inpAmtInv = vars.getStringParameter("inpemEfinAmtinvoiced");
    String budgetType = vars.getStringParameter("inpemEfinBudgetType");
    String inpInvText = vars.getStringParameter("inpemEfinInvoicetypeTxt");
    String inpOrder = vars.getStringParameter("inpemEfinCOrderId");
    String inpdateacct = vars.getStringParameter("inpdateacct");
    String jscode = "";

    if (StringUtils.isNotEmpty(inpPrcActual)) {
      inpPrcActual = inpPrcActual.replace(",", "");
    }

    if (StringUtils.isNotEmpty(inpQtyInv)) {
      inpQtyInv = inpQtyInv.replace(",", "");
    }

    if (StringUtils.isNotEmpty(inpAmtInv)) {
      inpAmtInv = inpAmtInv.replace(",", "");
    }

    String strBudgetInitializationId = null;
    String inpadClientId = vars.getStringParameter("inpadClientId");
    BigDecimal fundsAvailable = BigDecimal.ZERO;
    JSONObject fundsCheckingObject = null;
    String strManualEncumbranceId = "", strEncumbranceType = "", strEncumbranceLineId = "";
    Boolean isValidCombination = Boolean.TRUE, hasTaxLines = Boolean.FALSE;
    String strErrorMessage = "", strInvoiceType = "", strInvoiceId = "", strIsTax = "";

    List<InvoiceLine> invoiceLineList = new ArrayList<InvoiceLine>();

    try {
      OBContext.setAdminMode();

      /**
       * following lines were copied from core callout and has been commented.
       * 
       */

      /*
       * final String strinpissotrx = info.getStringParameter("inpissotrx", filterYesNo); final
       * String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance); final String
       * strBPartnerId = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance); final
       * String strBPartnerLocationId = info.getStringParameter("inpcBpartnerLocationId",
       * IsIDFilter.instance); info.addResult("inpiscashvat", CashVATUtil.isCashVAT(strinpissotrx,
       * strOrgId, strBPartnerId, strBPartnerLocationId));
       */
      strInvoiceId = vars.getStringParameter("inpcInvoiceId");
      Invoice invoice = Utility.getObject(Invoice.class, strInvoiceId);

      if ("inpemEfinCValidcombinationId".equals(inpLastFieldChanged)) {

        String strCombinationId = vars.getStringParameter("inpemEfinCValidcombinationId");
        strBudgetInitializationId = vars.getStringParameter("inpemEfinBudgetintId");
        strEncumbranceType = vars.getStringParameter("inpemEfinEncumtype");
        strManualEncumbranceId = vars.getStringParameter("inpemEfinManualencumbranceId");
        strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

        if (StringUtils.isNotBlank(strCombinationId)) {
          AccountingCombination combination = Utility.getObject(AccountingCombination.class,
              strCombinationId);

          if (combination != null) {
            EfinBudgetIntialization budgetIntialization = Utility
                .getObject(EfinBudgetIntialization.class, strBudgetInitializationId);

            if ("E".equals(combination.getEfinDimensiontype()) && !("PPA".equals(strInvoiceType))) {
              fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
                  combination);
              fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());

              if (fundsCheckingObject.has("errorFlag")) {
                if ("0".equals(fundsCheckingObject.get("errorFlag"))
                    || BigDecimal.ZERO.compareTo(fundsAvailable) == 0) {
                  isValidCombination = Boolean.FALSE;
                }
              }
            }

            if (isValidCombination) {
              info.addResult("inpadOrgId", invoice.getOrganization().getId());
              info.addResult("inpemEfinCSalesregionId", combination.getSalesRegion().getId());
              info.addResult("inpemEfinCElementvalueId", combination.getAccount().getId());
              info.addResult("inpemEfinCCampaignId", combination.getSalesCampaign().getId());
              info.addResult("inpcProjectId", combination.getProject().getId());
              info.addResult("inpemEfinCActivityId", combination.getActivity().getId());
              info.addResult("inpuser1Id", combination.getStDimension().getId());
              info.addResult("inpuser2Id", combination.getNdDimension().getId());
              info.addResult("inpemEfinCBpartnerId", combination.getBusinessPartner().getId());
              info.addResult("inpemEfinUniquecode", combination.getEfinUniqueCode());
              info.addResult("inpemEfinUniquecodename", combination.getEfinUniquecodename());
              info.addResult("JSEXECUTE",
                  "form.getFieldFromInpColumnName('inpemEfinFundsAvailable').setValue("
                      + fundsAvailable + ")");

              if ("M".equals(strEncumbranceType)) {
                strEncumbranceLineId = getEncumbranceLine(strManualEncumbranceId,
                    combination.getId());
                info.addResult("inpemEfinBudgmanuencumlnId", strEncumbranceLineId);
              }

              if (StringUtils.isNotEmpty(inpSalestransaction)
                  && StringUtils.isNotEmpty(inpReceiptType)
                  && StringUtils.isNotEmpty(inpPrePaymentInvoice)
                  && "Y".equals(inpSalestransaction)) {

                Invoice prePymentInvoice = OBDal.getInstance().get(Invoice.class,
                    inpPrePaymentInvoice);

                if (prePymentInvoice != null) {
                  invoiceLineList = prePymentInvoice.getInvoiceLineList().stream()
                      .filter(a -> a.getEfinCValidcombination().getId().equals(strCombinationId))
                      .collect(Collectors.toList());

                  if (invoiceLineList != null
                      && invoiceLineList.get(0).getEfinExpenseAccount() != null) {
                    info.addResult("inpemEfinExpenseAccount",
                        invoiceLineList.get(0).getEfinExpenseAccount().getId());

                  }

                }

              }
            } else {
              strErrorMessage = OBMessageUtils.messageBD("Efin_PI_NoFunds");
              info.addResult("ERROR",
                  strErrorMessage.replace("@@", combination.getEfinUniqueCode()));
              setNulls(info);
              info.addResult("inpadOrgId", invoice.getOrganization().getId());
            }
          }

        } else {
          setNulls(info);
          info.addResult("inpadOrgId", invoice.getOrganization().getId());
        }

        if (budgetType.equals("C")) {
          AccountingCombination accCombination = OBDal.getInstance()
              .get(AccountingCombination.class, validcombination);

          EfinBudgetIntialization budgetIntialization = Utility
              .getObject(EfinBudgetIntialization.class, budgetinitial);
          fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
              accCombination.getEfinFundscombination());
          fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());

          // OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
          // EfinBudgetInquiry.class,
          // "accountingCombination.id='" + validcombination + "' and efinBudgetint.id = '"
          // + budgetinitial + "' and salesCampaign.id = '"
          // + accCombination.getSalesCampaign().getId() + "' ");
          // if (budgetinquiry.list() != null && budgetinquiry.list().size() > 0) {
          // accountcombinationid = budgetinquiry.list().get(0).getId();
          //
          // }
          //
          // EfinBudgetInquiry budgetinquiryline = null;
          // budgetinquiryline = OBDal.getInstance().get(EfinBudgetInquiry.class,
          // accountcombinationid);
          if (fundsAvailable != null) {
            info.addResult("inpemEfinFbFundsAvailable", fundsAvailable);
          }
        } else {
          info.addResult("inpemEfinFbFundsAvailable", null);
        }
      }

      if ("inpadOrgId".equals(inpLastFieldChanged)) {

        Date endDate = new Date();
        // getting budget initial id based on transaction date
        strBudgetInitializationId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            inpadClientId, "");
        if (strBudgetInitializationId != null)
          info.addResult("inpemEfinBudgetintId", strBudgetInitializationId);
        else
          info.addResult("inpemEfinBudgetintId", null);
      }

      if ("inpdateinvoiced".equals(inpLastFieldChanged)
          || "inpdateacct".equals(inpLastFieldChanged)) {

        String strchangedDate = "";
        if ("inpdateinvoiced".equals(inpLastFieldChanged)) {

          strchangedDate = vars.getStringParameter("inpdateinvoiced");
          info.addResult("inpdateacct", FormatUtilities.replaceJS(strchangedDate));
        } else if ("inpdateacct".equals(inpLastFieldChanged)) {
          strchangedDate = vars.getStringParameter("inpdateacct");
        }

        if (inpInvText.equals("POM")) {
          if (inpOrder != null && inpOrder != "") {
            Order order = OBDal.getInstance().get(Order.class, inpOrder);

            // Set tax based on order date
            final List<Object> parameters = new ArrayList<Object>();
            parameters.add(order.getId());
            parameters.add(null);
            parameters.add(strchangedDate);

            String taxMethodId = (String) CallStoredProcedure.getInstance()
                .call("Efin_gettaxbasedonorder", parameters, null);

            EFIN_TaxMethod taxMethod = order.getEscmTaxMethod();

            if (StringUtils.isNotBlank(taxMethodId)) {
              EFIN_TaxMethod tax = OBDal.getInstance().get(EFIN_TaxMethod.class, taxMethodId);
              if (tax != null) {
                taxMethod = tax;
              }
            }

            if (taxMethod != null) {
              info.addResult("inpemEfinIstax", order.isEscmIstax());
              jscode = "form.getFieldFromColumnName('EM_Efin_Tax_Method_ID').setValue('"
                  + taxMethod.getId() + "');";
              info.addResult("JSEXECUTE", jscode);

              info.addResult("inpemEfinTaxMethodId", taxMethod.getId());
              if (taxMethod.isActive() && taxMethod.getValidToDate() == null) {
                info.addResult("JSEXECUTE",
                    "form.getFieldFromColumnName('EM_Efin_Tax_Method_ID').disable()");
                info.addResult("JSEXECUTE",
                    "form.getFieldFromColumnName('EM_Efin_Istax').disable()");
              }
              if (taxMethod.isActive() && taxMethod.getValidToDate() != null)
                info.addResult("JSEXECUTE",
                    "form.getFieldFromColumnName('EM_Efin_Istax').enable()");
            } else {
              info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EM_Efin_Istax').enable()");
            }
          }
        }

        String strConvertedDate = "";
        strConvertedDate = Utility.convertToGregorian(strchangedDate);
        Date invoiceDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS").parse(strConvertedDate);
        // getting budget initial id based on transaction date
        strBudgetInitializationId = BudgetAdjustmentCallout
            .getBudgetDefinitionForStartDate(invoiceDate, inpadClientId, "");
        if (strBudgetInitializationId != null)
          info.addResult("inpemEfinBudgetintId", strBudgetInitializationId);
        else
          info.addResult("inpemEfinBudgetintId", null);
      }

      if ("inpqtyinvoiced".equals(inpLastFieldChanged)) {
        info.addResult("inplinenetamt",
            new BigDecimal(inpPrcActual).multiply(new BigDecimal(inpQtyInv)));
      }

      if ("inpemEfinAmtinvoiced".equals(inpLastFieldChanged)) {
        info.addResult("inplinenetamt", new BigDecimal(inpAmtInv));
      }

      if ("inpemEfinIstax".equals(inpLastFieldChanged)) {
        strIsTax = vars.getStringParameter("inpemEfinIstax");
        TaxLineHandlerDAO taxHandler = new TaxLineHandlerImpl();
        if ("N".equals(strIsTax)) {
          hasTaxLines = taxHandler.hasTaxLines(invoice);
          if (hasTaxLines) {
            strErrorMessage = OBMessageUtils.messageBD("Efin_PI_NoFunds");
            info.addResult("ERROR", strErrorMessage);
          }
        }
      }

      /*
       * if ("inpcBpartnerId".equals(inpLastFieldChanged)) { if
       * (StringUtils.isEmpty(strPaymentBeneficiary)) { info.addResult("JSEXECUTE",
       * "form.getFieldFromColumnName('em_efin_beneficiary2_id').hide()"); } else {
       * info.addResult("JSEXECUTE",
       * "form.getFieldFromColumnName('em_efin_beneficiary2_id').show()"); } }
       */
    } catch (Exception e) {
      log4j.error("Exception in Invoiceline_Uniquecode: " + e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void setNulls(CalloutInfo info) {
    info.addResult("inpemEfinCSalesregionId", null);
    info.addResult("inpemEfinCElementvalueId", null);
    info.addResult("inpemEfinCCampaignId", null);
    info.addResult("inpcProjectId", null);
    info.addResult("inpemEfinCActivityId", null);
    info.addResult("inpuser1Id", null);
    info.addResult("inpuser2Id", null);
    info.addResult("inpemEfinCBpartnerId", null);
    info.addResult("inpemEfinUniquecode", null);
    info.addResult("inpemEfinFundsAvailable", 0);
    info.addResult("inpemEfinBudgetlinesId", null);
    info.addResult("inpemEfinBudgmanuencumlnId", null);

  }

  private String getEncumbranceLine(String strManualEncumbranceId, String strCombinationId) {
    String strLineId = "";
    try {
      strLineId = CommonValidations.getEncumbranceLineId(strManualEncumbranceId, strCombinationId);
    } catch (Exception e) {
      log4j.error("Exception in getEncumbranceLine: " + e);
    }
    return strLineId;
  }
}
