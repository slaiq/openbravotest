package sa.elm.ob.finance.actionHandler.dao;

import java.math.BigDecimal;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;

/**
 * 
 * @author Priyanka Ranjan 22/11/2017
 *
 */

public class BudgetRevisionAddLineHandlerDAO {
  private static final Logger LOG = LoggerFactory.getLogger(BudgetRevisionAddLineHandlerDAO.class);

  /**
   * This method is used to insert lines in Budget Revision through add line process.
   * 
   * @param selectedlines
   * @param revisionId
   * @return 1,0
   */
  public static int insertBudgetRevisionLines(JSONArray selectedlines, String revisionId) {
    EfinBudgetTransfertrxline budRevLine = null;
    try {
      OBContext.setAdminMode();
      EfinBudgetTransfertrx budrev = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          revisionId);
      if (selectedlines.length() > 0) {
        for (int line = 0; line < selectedlines.length(); line++) {
          JSONObject selectedRow = selectedlines.getJSONObject(line);
          LOG.debug("selectedRow:" + selectedRow);
          OBQuery<EfinBudgetTransfertrxline> existLines = OBDal.getInstance().createQuery(
              EfinBudgetTransfertrxline.class,
              "efinBudgetTransfertrx.id = '" + revisionId + "' and accountingCombination.id = '"
                  + selectedRow.getString("accountingCombination") + "'");
          if (existLines.list() != null && existLines.list().size() > 0) {
            // updating existing line.
            budRevLine = existLines.list().get(0);
            if (new BigDecimal(selectedRow.getString("incamount")).compareTo(BigDecimal.ZERO) < 0) {
              budRevLine.setIncrease(BigDecimal.ZERO);
              budRevLine.setDecrease(new BigDecimal(selectedRow.getString("decamount")));
              budRevLine.setCurrentBudget(new BigDecimal(selectedRow.getString("currentbudget")));
            } else {
              budRevLine.setDecrease(BigDecimal.ZERO);
              budRevLine.setIncrease(new BigDecimal(selectedRow.getString("incamount")));
              budRevLine.setCurrentBudget(new BigDecimal(selectedRow.getString("currentbudget")));
            }
            OBDal.getInstance().save(budRevLine);

          } else {
            // inserting new line.
            EfinBudgetInquiry objBudLine = OBDal.getInstance().get(EfinBudgetInquiry.class,
                selectedRow.getString("budgetInquiryLine"));
            AccountingCombination objActCombination = OBDal.getInstance()
                .get(AccountingCombination.class, selectedRow.getString("accountingCombination"));
            budRevLine = OBProvider.getInstance().get(EfinBudgetTransfertrxline.class);
            budRevLine.setOrganization(budrev.getOrganization());
            budRevLine.setEfinBudgetTransfertrx(budrev);
            if (!"null".equals(selectedRow.getString("description"))) {
              budRevLine.setDescription(selectedRow.getString("description"));
            } else {
              budRevLine.setDescription("");
            }

            if (budrev.getSalesCampaign().getEfinBudgettype().equals("F")) {

              if (objActCombination.getEfinCostcombination() != null) {
                OBQuery<EfinBudgetInquiry> budinq = OBDal.getInstance().createQuery(
                    EfinBudgetInquiry.class,
                    "accountingCombination.id =:accountingCombinationID and efinBudgetint.id =:efinBudgetintID ");
                budinq.setNamedParameter("accountingCombinationID",
                    objActCombination.getEfinCostcombination().getId());
                budinq.setNamedParameter("efinBudgetintID", budrev.getEfinBudgetint().getId());
                if (budinq.list() != null && budinq.list().size() > 0) {
                  budRevLine.setCostcurrentbudget(budinq.list().get(0).getFundsAvailable());
                }

              }
              budRevLine.setFundsAvailable(new BigDecimal(selectedRow.getString("fundsavailable")));
            } else if (budrev.getSalesCampaign().getEfinBudgettype().equals("C")) {
              if (objActCombination.getEfinFundscombination() != null) {
                OBQuery<EfinBudgetInquiry> budinq = OBDal.getInstance().createQuery(
                    EfinBudgetInquiry.class,
                    "accountingCombination.id =:accountingCombinationID and efinBudgetint.id =:efinBudgetintID ");
                budinq.setNamedParameter("accountingCombinationID",
                    objActCombination.getEfinFundscombination().getId());
                budinq.setNamedParameter("efinBudgetintID", budrev.getEfinBudgetint().getId());
                if (budinq.list() != null && budinq.list().size() > 0) {
                  budRevLine.setFundsAvailable(budinq.list().get(0).getFundsAvailable());
                }
              }
              budRevLine
                  .setCostcurrentbudget(new BigDecimal(selectedRow.getString("fundsavailable")));
            }

            if (new BigDecimal(selectedRow.getString("incamount"))
                .compareTo(BigDecimal.ZERO) == 1) {
              budRevLine.setDecrease(BigDecimal.ZERO);
              budRevLine.setIncrease(new BigDecimal(selectedRow.getString("incamount")));
              budRevLine.setCurrentBudget(new BigDecimal(selectedRow.getString("currentbudget")));

            } else if (new BigDecimal(selectedRow.getString("decamount"))
                .compareTo(BigDecimal.ZERO) == 1) {
              budRevLine.setIncrease(BigDecimal.ZERO);
              budRevLine.setDecrease(new BigDecimal(selectedRow.getString("decamount")));
              budRevLine.setCurrentBudget(new BigDecimal(selectedRow.getString("currentbudget")));
            } else {
              budRevLine.setCurrentBudget(new BigDecimal(selectedRow.getString("currentbudget")));
            }
            budRevLine.setAccountingCombination(objActCombination);
            if (objBudLine != null) {
              budRevLine.setUniqueCodeName(objBudLine.getUniqueCodeName());
            } else {
              budRevLine.setUniqueCodeName(objActCombination.getEfinUniquecodename());
            }
            OBDal.getInstance().save(budRevLine);

          }
        }
      } else {
        return -1;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while inserting lines in Budget Revision add lines process : ", e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

}