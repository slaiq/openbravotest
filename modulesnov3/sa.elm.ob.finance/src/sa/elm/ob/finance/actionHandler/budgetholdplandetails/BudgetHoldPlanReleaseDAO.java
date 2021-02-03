package sa.elm.ob.finance.actionHandler.budgetholdplandetails;

import java.sql.Connection;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import sa.elm.ob.finance.EFINRdvBudgHold;
import sa.elm.ob.finance.EfinBudgetTransfertrx;

public interface BudgetHoldPlanReleaseDAO {

  public JSONObject addHoldReleaseInRDV(Connection conn, EFINRdvBudgHold rdvBudgHold,
      JSONArray selectedLines, boolean ismanual);

  public JSONObject addBudgRevHoldReleaseInRDV(Connection conn, EfinBudgetTransfertrx transferTrx);

  public JSONObject releaseRevert(JSONArray selectedLines);

  public boolean releaseRevertValidatio(JSONArray selectedLines);
}
