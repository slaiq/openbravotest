package sa.elm.ob.scm.actionHandler.TransactionRegistry;

import java.sql.Connection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

import sa.elm.ob.utility.util.Utility;

public class UpdateICHandlerDAO {
  @SuppressWarnings("unused")
  private Connection conn = null;

  private static Logger log4j = Logger.getLogger(UpdateICHandlerDAO.class);

  public UpdateICHandlerDAO(Connection con) {
    this.conn = con;
  }

  public JSONObject ValidateTransactions(JSONArray transactionIds) {
    JSONObject jsonObject = new JSONObject();
    try {
      OBContext.setAdminMode();

      MaterialTransaction trx = null;
      int i = 0;
      for (i = 0; i < transactionIds.length(); i++) {
        trx = Utility.getObject(MaterialTransaction.class, transactionIds.get(i).toString());
        updateTrx(trx);
      }

      jsonObject.put("code", i);
      jsonObject.put("message",
          OBMessageUtils.messageBD("ESCM_Validated").replace("xx", String.valueOf(i)));

      OBDal.getInstance().flush();

    } catch (JSONException je) {
    } catch (Exception e) {
      try {
        jsonObject.put("code", "-1");
        jsonObject.put("message", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      log4j.error("Exception while validating : ", e);
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
    return jsonObject;
  }

  private void updateTrx(MaterialTransaction trx) {
    try {
      OBContext.setAdminMode();

      trx.setEscmIc(Boolean.TRUE);
      trx.setUpdated(new Date());
      trx.setUpdatedBy(OBContext.getOBContext().getUser());

      OBDal.getInstance().save(trx);

    } catch (Exception e) {
      log4j.error("Exception updateTrx: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
