package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;

/**
 * 
 * @author sathishkumar
 *
 *
 *         this callout is used to update the nine dimension based on unique code selection
 */

public class SimpleGLJournalLinesCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;
  final private static Logger log = Logger.getLogger(SimpleGLJournalLinesCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      VariablesSecureApp vars = info.vars;

      String inpcValidCombinationID = vars.getStringParameter("inpcValidcombinationId");
      String inpBudgetIntId = vars.getStringParameter("inpemEfinBudgetintId");
      String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
      BigDecimal fundAvailable = new BigDecimal("0");

      EfinBudgetIntialization budgetDef = null;
      EfinBudgetInquiry budgetInquiry = null;
      Boolean isSuccess = true;
      boolean isDepartmentfund = true;
      String strErrorMessage = "";

      if (inpLastFieldChanged.equals("inpcValidcombinationId")) {
        AccountingCombination combination = OBDal.getInstance().get(AccountingCombination.class,
            inpcValidCombinationID);

        if (StringUtils.isNotEmpty(inpBudgetIntId)) {
          budgetDef = OBDal.getInstance().get(EfinBudgetIntialization.class, inpBudgetIntId);
        }

        // Compute funds available based on uniquecode
        if (combination != null && combination.getEfinDimensiontype() != null) {
          if (combination.getEfinDimensiontype().equals("E")) {
            if (StringUtils.isNotEmpty(inpBudgetIntId)) {
              if (combination.isEFINDepartmentFund()) {
                final OBQuery<EfinBudgetInquiry> budgetInqQry = OBDal.getInstance()
                    .createQuery(EfinBudgetInquiry.class, "efinBudgetint.id='" + inpBudgetIntId
                        + "' and accountingCombination.id ='" + inpcValidCombinationID + "'");

                List<EfinBudgetInquiry> budgetInqList = budgetInqQry.list();
                if (budgetInqList.size() > 0) {
                  fundAvailable = budgetInqList.get(0).getFundsAvailable();
                } else {
                  isSuccess = false;
                }
              } else {
                List<AccountingCombination> combList = CommonValidationsDAO.getParentAccountCom(
                    combination, OBContext.getOBContext().getCurrentClient().getId());
                budgetInquiry = PurchaseInvoiceSubmitUtils.getBudgetInquiry(combList.get(0),
                    budgetDef);
                if (budgetInquiry != null) {
                  fundAvailable = budgetInquiry.getFundsAvailable();
                } else {
                  isSuccess = false;
                  isDepartmentfund = false;
                }

              }
            }
          }
        } else {
          isSuccess = false;
          info.addResult("ERROR", OBMessageUtils.messageBD("Efin_IncompleteUniquecode"));
        }

        // if success then updat all the dimensions else throw error

        if (isSuccess) {
          info.addResult("inpadOrgId", combination.getOrganization().getId());
          info.addResult("inpcBpartnerId",
              combination.getBusinessPartner() != null ? combination.getBusinessPartner().getId()
                  : null);

          info.addResult("inpcSalesregionId",
              combination.getSalesRegion() != null ? combination.getSalesRegion().getId() : null);
          info.addResult("inpcActivityId",
              combination.getActivity() != null ? combination.getActivity().getId() : null);
          info.addResult("inpcProjectId",
              combination.getProject() != null ? combination.getProject().getId() : null);
          info.addResult("inpcCampaignId",
              combination.getSalesCampaign() != null ? combination.getSalesCampaign().getId()
                  : null);
          info.addResult("inpuser1Id",
              combination.getStDimension() != null ? combination.getStDimension().getId() : null);
          info.addResult("inpuser2Id",
              combination.getNdDimension() != null ? combination.getNdDimension().getId() : null);
          info.addResult("inpemEfinAccount",
              combination.getAccount() != null ? combination.getAccount().getId() : null);
          info.addResult("inpemEfinUniquecodevalue",
              combination.getEfinUniqueCode() != null ? combination.getEfinUniquecodename() : null);
          info.addResult("inpemEfinFundsAvailable", fundAvailable);

          info.addResult("inpemEfinUniquecode",
              combination.getEfinUniqueCode() != null ? combination.getEfinUniqueCode() : null);

        } else {

          if (isDepartmentfund) {
            strErrorMessage = OBMessageUtils.messageBD("Efin_gl_depdistribution");
            info.addResult("ERROR", strErrorMessage.replace("@", combination.getEfinUniqueCode()));
          } else {
            strErrorMessage = OBMessageUtils.messageBD("Efin_gl_orgdistribution");
            info.addResult("ERROR", strErrorMessage.replace("@", combination.getEfinUniqueCode()));
          }

          info.addResult("inpadOrgId", null);
          info.addResult("inpcBpartnerId", null);
          info.addResult("inpcSalesregionId", null);
          info.addResult("inpcActivityId", null);
          info.addResult("inpcProjectId", null);
          info.addResult("inpcCampaignId", null);
          info.addResult("inpuser1Id", null);
          info.addResult("inpuser2Id", null);
          info.addResult("inpemEfinAccount", null);
          info.addResult("inpemEfinUniquecode", null);
          info.addResult("inpemEfinFundsAvailable", 0);

        }
      }
    } catch (Exception e) {
      log.error("Exception in SimpleGLJournalLinesCallout: " + e);
    }

  }
}
