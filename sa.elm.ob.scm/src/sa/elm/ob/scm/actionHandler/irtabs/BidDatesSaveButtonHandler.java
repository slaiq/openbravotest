package sa.elm.ob.scm.actionHandler.irtabs;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.EscmBidMgmt;

/**
 * 
 * @author oalbader,gopalakrishnan
 *
 */
public class BidDatesSaveButtonHandler extends BaseActionHandler {
  Logger log4j = Logger.getLogger(BidDatesSaveButtonHandler.class);
  JSONObject json;

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    try {
      /* Get the data as json */
      final JSONObject jsonData = new JSONObject(data);

      json = new JSONObject();

      final String recordId = jsonData.getString("recordId");
      EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class, recordId);
      if (bidmgmt.getBidstatus().equals("ACT") && bidmgmt.getBidappstatus().equals("ESCM_AP")
          && bidmgmt.getTabadulTenderID() != null) {
        json.put("showAction", "true");
      } else if (bidmgmt.getBidstatus().equals("EXT") && bidmgmt.getBidappstatus().equals("ESCM_RA")
          && bidmgmt.getTabadulTenderID() != null) {
        json.put("showAction", "true");
      } else {
        json.put("showAction", "false");
      }

      log4j.debug("json:" + json);
    } catch (Exception e) {
      throw new OBException(e);
    }
    return json;
  }
}