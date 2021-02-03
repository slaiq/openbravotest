package sa.elm.ob.finance.actionHandler.dao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;

/**
 * 
 * @author Kousalya 28/12/2017
 *
 */

public class SimpleGLJournalHandlerDAO {
  private static final Logger log = LoggerFactory.getLogger(SimpleGLJournalHandlerDAO.class);

  /**
   * This method is used to insert lines in adjustment through add line process.
   * 
   * @param selectedlines
   * @param adjustmentId
   * @return 1,0
   */
  @SuppressWarnings("rawtypes")
  public static Map<String, String> getUniqueCode(String roleId, String schemaId, String journalId,
      Boolean isMultigeneralLedger, String uniqueCodeLs, String inpBudgetIntId) {
    String uniqueCode = null;
    Map<String, String> uniquecodeMap = new HashMap<String, String>();
    String multiGL = "N";
    SQLQuery acctCombLs = null;
    StringBuilder sqlBuilder = null;
    try {
      OBContext.setAdminMode();
      uniquecodeMap.put("isvalid", "false");
      uniquecodeMap.put("fundAvailable", "0");
      uniquecodeMap.put("uniqueCode", null);
      if (isMultigeneralLedger)
        multiGL = "Y";
      else
        multiGL = "N";
      sqlBuilder = new StringBuilder();
      if (uniqueCodeLs != null && !uniqueCodeLs.equals(""))
        uniqueCodeLs = uniqueCodeLs.replaceFirst(",", "");
      sqlBuilder.append("select c_validcombination_id from c_validcombination vc "
          + " left join c_campaign camp on camp.c_campaign_id=vc.c_campaign_id "
          + " where account_id in (select c_elementvalue_id from efin_security_rules_act sra join efin_security_rules sr "
          + " on sr.efin_security_rules_id=sra.efin_security_rules_id "
          + " where efin_processbutton='Y' and sr.efin_security_rules_id=(select em_efin_security_rules_id from ad_role where ad_role_id='"
          + roleId + "')) " + " and ((c_acctschema_id='" + schemaId + "' and '" + multiGL
          + "'='N') or ('" + multiGL + "'='Y')) "
          + " and camp.em_efin_budgettype='F' and vc.ad_org_id=(select ad_org_id from gl_journal gl where gl.gl_journal_id='"
          + journalId + "') "
          + " and c_salesregion_id not in (select budgetcontrol_costcenter from efin_budget_ctrl_param ctrl1) "
          + " and c_salesregion_id not in (select hq_budgetcontrolunit from efin_budget_ctrl_param ctrl2) "
          + " and c_validcombination_id not in (select c_validcombination_id from gl_journal gl left join gl_journalline glln on gl.gl_journal_id=glln.gl_journal_id "
          + " where gl.gl_journal_id='" + journalId + "') ");
      if (uniqueCodeLs != null && !uniqueCodeLs.equals(""))
        sqlBuilder.append(" and c_validcombination_id not in (" + uniqueCodeLs + ")");
      acctCombLs = OBDal.getInstance().getSession().createSQLQuery(sqlBuilder.toString());

      if (acctCombLs != null && acctCombLs.list().size() > 0) {
        for (Iterator iterator = acctCombLs.list().iterator(); iterator.hasNext();) {
          uniqueCode = iterator.next().toString();
          Map<String, String> ucodeMap = checkUniqueCode(uniqueCode, inpBudgetIntId);
          if (ucodeMap.get("isvalid").equals("true")) {
            uniquecodeMap.put("isvalid", ucodeMap.get("isvalid"));
            uniquecodeMap.put("fundAvailable", ucodeMap.get("fundAvailable"));
            uniquecodeMap.put("uniqueCode", uniqueCode);
            return uniquecodeMap;
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception while getting unique code : ", e);
      }
      uniquecodeMap = null;
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
    return uniquecodeMap;
  }

  /**
   * This method is used to check unique code
   * 
   * @param inpcValidCombinationID
   * @param inpBudgetIntId
   * @return
   */
  public static Map<String, String> checkUniqueCode(String inpcValidCombinationID,
      String inpBudgetIntId) {
    String isvalid = "false";
    EfinBudgetIntialization budgetDef = null;
    EfinBudgetInquiry budgetInquiry = null;
    BigDecimal fundAvailable = new BigDecimal("0");
    Boolean isSuccess = true;
    Map<String, String> uniquecodeMap = new HashMap<String, String>();

    try {
      uniquecodeMap.put("isvalid", isvalid);
      uniquecodeMap.put("fundAvailable", "0");
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
              final OBQuery<EfinBudgetInquiry> budgetInqQry = OBDal.getInstance().createQuery(
                  EfinBudgetInquiry.class,
                  "efinBudgetint.id=:BudgetintID and accountingCombination.id =:accountingCombinationID");
              budgetInqQry.setNamedParameter("BudgetintID", inpBudgetIntId);
              budgetInqQry.setNamedParameter("accountingCombinationID", inpcValidCombinationID);
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
              }
            }
          }
        }
      } else {
        isSuccess = false;
      }
      if (isSuccess) {
        isvalid = "true";
        uniquecodeMap.put("isvalid", isvalid);
        uniquecodeMap.put("fundAvailable", fundAvailable.toString());
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception while checkUniqueCode : ", e);
      }
      isvalid = "false";
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
    return uniquecodeMap;
  }
}