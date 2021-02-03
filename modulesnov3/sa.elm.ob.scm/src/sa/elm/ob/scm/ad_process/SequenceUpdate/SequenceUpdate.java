package sa.elm.ob.scm.ad_process.SequenceUpdate;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.DocumentSequence;

public class SequenceUpdate extends BaseActionHandler {
  private static Logger log = Logger.getLogger(SequenceUpdate.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    Boolean errorFlag = Boolean.FALSE;

    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      String strOrganization = jsonparams.getString("ad_org_id");
      String strPrefix = jsonparams.getString("prefix");
      boolean checkPrefix = sa.elm.ob.utility.util.UtilityDAO.isNumber(strPrefix);
      if (checkPrefix) {
        OBQuery<DocumentSequence> sequenceQuery = OBDal.getInstance().createQuery(
            DocumentSequence.class,
            "as e where e.organization.id='" + strOrganization.toString() + "'");
        if (sequenceQuery.list().size() > 0) {
          for (DocumentSequence objSequence : sequenceQuery.list()) {
            if (new BigDecimal(objSequence.getPrefix())
                .compareTo(new BigDecimal(strPrefix.toString())) == 1) {
              errorFlag = Boolean.TRUE;
            }
            if (!errorFlag) {
              objSequence.setPrefix(strPrefix.toString());
              objSequence.setNextAssignedNumber(objSequence.getStartingNo());
              OBDal.getInstance().save(objSequence);
            }
          }
        }
        if (errorFlag) {
          JSONObject erroMessage = new JSONObject();
          erroMessage.put("severity", "error");
          erroMessage.put("text", OBMessageUtils.messageBD("ESCM_Update_Prefix(Error)"));
          json.put("message", erroMessage);
        } else {
          OBDal.getInstance().flush();
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "success");
          successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
          json.put("message", successMessage);
        }
      } else {
        JSONObject erroMessage = new JSONObject();
        erroMessage.put("severity", "error");
        erroMessage.put("text", OBMessageUtils.messageBD("ESCM_PrefixSeq_Number"));
        json.put("message", erroMessage);
      }
      return json;
    } catch (Exception e) {
      log.error("Exception in  SequenceUpdate:", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}