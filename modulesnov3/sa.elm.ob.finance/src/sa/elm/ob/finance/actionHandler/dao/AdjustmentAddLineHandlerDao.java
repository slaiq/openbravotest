package sa.elm.ob.finance.actionHandler.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.marketing.Campaign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EfinBudgetInquiry;

/**
 * 
 * @author Gowtham.V
 *
 */
public class AdjustmentAddLineHandlerDao {
  private static final Logger LOG = LoggerFactory.getLogger(AdjustmentAddLineHandlerDao.class);

  /**
   * This method is used to insert lines in adjustment through add line process.
   * 
   * @param selectedlines
   * @param adjustmentId
   * @return 1,0
   */
  public static int insertAdjustmentLines(JSONArray selectedlines, String adjustmentId) {
    BudgetAdjustmentLine adjLine = null;
    try {
      OBContext.setAdminMode();
      BudgetAdjustment adjustment = OBDal.getInstance().get(BudgetAdjustment.class, adjustmentId);
      if (selectedlines.length() > 0) {
        for (int line = 0; line < selectedlines.length(); line++) {
          JSONObject selectedRow = selectedlines.getJSONObject(line);
          BigDecimal adjamount = new BigDecimal(selectedRow.getString("adjamount"));
          BigDecimal decadjamount = new BigDecimal(selectedRow.getString("decadjamount"));
          LOG.debug("selectedRow:" + selectedRow);
          OBQuery<BudgetAdjustmentLine> existLines = OBDal.getInstance().createQuery(
              BudgetAdjustmentLine.class,
              "efinBudgetadj.id =:adjustmentId and accountingCombination.id =:accountingCombinationId ");
          existLines.setNamedParameter("adjustmentId", adjustmentId);
          existLines.setNamedParameter("accountingCombinationId",
              selectedRow.getString("accountingCombination"));
          if (existLines.list() != null && existLines.list().size() > 0) {
            // updating existing line.
            adjLine = existLines.list().get(0);
            if (new BigDecimal(selectedRow.getString("adjamount")).compareTo(BigDecimal.ZERO) < 0) {
              adjLine.setIncrease(BigDecimal.ZERO);
              adjLine.setDecrease(new BigDecimal(selectedRow.getString("adjamount")));
              adjLine.setCurrentBudget(new BigDecimal(selectedRow.getString("currentbudget"))
                  .subtract(adjamount.negate()));
              adjLine.setORGBudgetRevised(
                  new BigDecimal(selectedRow.getString("amount")).subtract(adjamount.negate()));
            } else {
              adjLine.setDecrease(BigDecimal.ZERO);
              adjLine.setIncrease(new BigDecimal(selectedRow.getString("adjamount")));
              adjLine.setCurrentBudget(
                  new BigDecimal(selectedRow.getString("currentbudget")).add(adjamount));
              adjLine.setORGBudgetRevised(
                  new BigDecimal(selectedRow.getString("amount")).add(adjamount));
            }
            OBDal.getInstance().save(adjLine);
          } else {
            // inserting new line.
            EfinBudgetInquiry objBudLine = OBDal.getInstance().get(EfinBudgetInquiry.class,
                selectedRow.getString("efinBudgetinquiry"));
            AccountingCombination objActCombination = OBDal.getInstance()
                .get(AccountingCombination.class, selectedRow.getString("accountingCombination"));
            adjLine = OBProvider.getInstance().get(BudgetAdjustmentLine.class);
            adjLine.setOrganization(adjustment.getOrganization());
            adjLine.setEfinBudgetadj(adjustment);
            adjLine.setDescription(selectedRow.getString("description"));
            if (selectedRow.getString("efinBudgetinquiry").equals("null")) {
              adjLine.setBudgetInquiryLine(null);
            } else {
              adjLine.setBudgetInquiryLine(OBDal.getInstance().get(EfinBudgetInquiry.class,
                  selectedRow.getString("efinBudgetinquiry")));
            }
            if (new BigDecimal(selectedRow.getString("adjamount"))
                .compareTo(BigDecimal.ZERO) == 1) {
              adjLine.setDecrease(BigDecimal.ZERO);
              adjLine.setIncrease(new BigDecimal(selectedRow.getString("adjamount")));
              adjLine.setCurrentBudget(
                  new BigDecimal(selectedRow.getString("currentbudget")).add(adjamount));
              adjLine.setORGBudgetRevised(
                  new BigDecimal(selectedRow.getString("amount")).add(adjamount));

            } else if (new BigDecimal(selectedRow.getString("decadjamount"))
                .compareTo(BigDecimal.ZERO) == 1) {
              adjLine.setIncrease(BigDecimal.ZERO);
              adjLine.setDecrease(new BigDecimal(selectedRow.getString("decadjamount")));
              adjLine.setCurrentBudget(
                  new BigDecimal(selectedRow.getString("currentbudget")).subtract(decadjamount));
              adjLine.setORGBudgetRevised(
                  new BigDecimal(selectedRow.getString("amount")).subtract(decadjamount));
            } else {
              adjLine.setCurrentBudget(new BigDecimal(selectedRow.getString("currentbudget")));
              adjLine.setORGBudgetRevised(new BigDecimal(selectedRow.getString("amount")));
            }
            adjLine.setCostcurbudget(
                getCostCurrentBudget(objActCombination, adjustment.getEfinBudgetint().getId()));
            adjLine.setAccountingCombination(objActCombination);
            adjLine.setOriginalBudget(new BigDecimal(selectedRow.getString("amount")));
            // adjLine.setCostcurbudget(new BigDecimal(selectedRow.getString("costCurrentBudget")));
            adjLine.setCarryforward(new BigDecimal(selectedRow.getString("carryForward")));
            adjLine.setUniqueCodeName(objBudLine.getUniqueCodeName());
            adjLine.setOrgid(objBudLine.getOrganization());
            adjLine.setDepartment(objBudLine.getDepartment());
            adjLine.setAccount(objBudLine.getAccount());
            adjLine.setSubAccount(objBudLine.getProject());
            adjLine.setSalesCampaign(objBudLine.getSalesCampaign());
            adjLine.setBusinessPartner(objBudLine.getBusinessPartner());
            adjLine.setFunctionalClassfication(objBudLine.getFunctionalClassfication());
            adjLine.setFuture1(objBudLine.getFuture1());
            adjLine.setNdDimension(objBudLine.getNdDimension());
            OBDal.getInstance().save(adjLine);
          }
        }
      } else {
        return -1;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while inserting lines in adjustment add lines process : ", e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

  /**
   * get cost current budget value of selected unique Code
   * 
   * @param act
   * @param budgetInqId
   * @return cost current Budget value
   */
  public static BigDecimal getCostCurrentBudget(AccountingCombination act, String budgetInqId) {
    try {
      final List<Object> parameters = new ArrayList<Object>();
      OBQuery<Campaign> budgettype = OBDal.getInstance().createQuery(Campaign.class,
          "efinBudgettype='C'");
      if (budgettype.list() != null && budgettype.list().size() > 0) {
        OBQuery<EfinBudgetInquiry> inq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
            "organization.id = ? and department.id=? and account.id=? and businessPartner.id=? and salesCampaign.id=? and project.id=? and functionalClassfication.id=? and future1.id=? and ndDimension.id=? and efinBudgetint.id=?");
        parameters.add(act.getOrganization().getId());
        parameters.add(act.getSalesRegion().getId());
        parameters.add(act.getAccount().getId());
        parameters.add(act.getBusinessPartner().getId());
        parameters.add(budgettype.list().get(0));
        parameters.add(act.getProject().getId());
        parameters.add(act.getActivity().getId());
        parameters.add(act.getStDimension().getId());
        parameters.add(act.getNdDimension().getId());
        parameters.add(budgetInqId);
        inq.setParameters(parameters);
        if (inq.list() != null && inq.list().size() > 0) {
          return inq.list().get(0).getCurrentBudget();
        } else {
          return BigDecimal.ZERO;
        }
      } else {
        return BigDecimal.ZERO;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in getting costcurrent budget " + e, e);
      }
    }
    return BigDecimal.ZERO;
  }

}