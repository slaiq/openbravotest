package sa.elm.ob.finance.ad_process.RDVProcess;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinPenaltyTypes;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopinagh.R
 *
 */
public class AddDefaultPenaltyServiceImpl implements AddDefaultPenaltyService {

  private static final Logger log4j = Logger.getLogger(AddDefaultPenaltyServiceImpl.class);

  @Override
  public Boolean hasPenaltyApplied(String strRDVTrxLineID) {
    Boolean delayedPenaltyApplied = Boolean.FALSE;
    try {
      String strWhereClause = " where efinRdvtxnline.id =:strRDVTrxLineID and efinPenaltyTypes.deductiontype.code = :deductionType ";
      List<EfinPenaltyAction> penalties = new ArrayList<EfinPenaltyAction>();

      OBQuery<EfinPenaltyAction> penaltyQuery = OBDal.getInstance()
          .createQuery(EfinPenaltyAction.class, strWhereClause);

      penaltyQuery.setNamedParameter("strRDVTrxLineID", strRDVTrxLineID);
      penaltyQuery.setNamedParameter("deductionType", AddDefaultPenaltyDAOImpl.DELAYED_PENALTY);

      if (penaltyQuery != null) {

        penalties = penaltyQuery.list();
        if (penalties.size() > 0) {
          delayedPenaltyApplied = Boolean.TRUE;
        }
      }

    } catch (Exception e) {
      log4j.error("Exception while hasPenaltyApplied(): " + e);
      e.printStackTrace();
    }

    return delayedPenaltyApplied;
  }

  @Override
  public JSONObject addPenalty(String strRDVTrxLineID, String strMatchQty, String actionDate,
      String strAdvanceDeductionAmount, String strMatchAmt) {
    JSONObject addPenaltyObject = new JSONObject();
    Boolean penaltyAdded = Boolean.TRUE;
    try {
      AddDefaultPenaltyDAO dao = new AddDefaultPenaltyDAOImpl();
      EfinRDVTxnline rdvLine = Utility.getObject(EfinRDVTxnline.class, strRDVTrxLineID);
      if (rdvLine != null) {
        EfinPenaltyTypes penaltyType = dao.getDelayedPenaltyType(rdvLine.getClient().getId());

        if (penaltyType != null) {

          try {
            BigDecimal matchQty = new BigDecimal(strMatchQty);
            penaltyAdded = dao.addPenalty(strRDVTrxLineID, penaltyType, matchQty, actionDate,
                strAdvanceDeductionAmount, strMatchAmt);

            OBDal.getInstance().refresh(rdvLine);

            if (penaltyAdded) {
              addPenaltyObject.put("result", "true");
              addPenaltyObject.put("message", OBMessageUtils.messageBD("Efin_Penalty_Success"));
              addPenaltyObject.put("penaltyAmount", rdvLine.getPenaltyAmt());
            } else {
              addPenaltyObject.put("result", "false");
              addPenaltyObject.put("message", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
            }
          } catch (RDVException e) {
            try {
              addPenaltyObject.put("result", "false");
              addPenaltyObject.put("message", e.getMessage());
            } catch (JSONException e1) {
              e1.printStackTrace();
            }
          } catch (Exception e) {
            addPenaltyObject.put("result", "false");
            addPenaltyObject.put("message", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
          }

        } else {
          addPenaltyObject.put("result", "false");
          addPenaltyObject.put("message", OBMessageUtils.messageBD("Efin_NoPenalty"));
        }

      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      log4j.error("Exception while addPenalty(): " + e);
      try {
        addPenaltyObject.put("result", "false");
        addPenaltyObject.put("message", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
    }

    return addPenaltyObject;
  }

  @Override
  public JSONObject applicableForPenalty(String strRDVTrxLineID, String strMatchQty,
      String strMatchAmt) {
    Boolean canApplyPenalty = Boolean.TRUE, isPenaltyApplied = Boolean.FALSE;
    JSONObject resultObject = new JSONObject();
    try {
      AddDefaultPenaltyDAO dao = new AddDefaultPenaltyDAOImpl();
      resultObject = dao.defaultValidations(strRDVTrxLineID, strMatchQty, strMatchAmt);

      isPenaltyApplied = hasPenaltyApplied(strRDVTrxLineID);

      canApplyPenalty = !isPenaltyApplied;
      if (canApplyPenalty && resultObject.get("addPenalty").equals("true")) {
        canApplyPenalty = dao.isPenaltyApplicable(strRDVTrxLineID);
        if (canApplyPenalty) {
          resultObject.put("addPenalty", "true");
        } else {
          resultObject.put("addPenalty", "false");
        }
      } else {
        resultObject.put("addPenalty", "false");
      }
    } catch (Exception e) {
      log4j.error("Exception while applicableForPenalty(): " + e);
      try {
        resultObject.put("result", "false");
        resultObject.put("message", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      e.printStackTrace();
    }

    return resultObject;
  }

}
