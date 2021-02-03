
package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;

import sa.elm.ob.finance.Efin_Child_Sequence;
import sa.elm.ob.finance.Efin_Parent_Sequence;

/**
 * This class is used to split child sequence and create new one based on selection
 * 
 * This process will execute when clicking split button in child sequence tab
 * 
 * @author Sathishkumar.P
 *
 */

public class SplitChildSequenceActionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(SplitChildSequenceActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {

    JSONObject result = new JSONObject();
    JSONObject successMessage = new JSONObject();

    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");

      String parentSeqId = jsonRequest.getString("Efin_Parent_Sequence_ID");
      String childSeqId = jsonRequest.getString("Efin_Child_Sequence_ID");

      int from = jsonparams.getInt("From");
      int to = jsonparams.getInt("To");
      String userId = jsonparams.getString("ad_user_id");

      Efin_Child_Sequence childSeq = OBDal.getInstance().get(Efin_Child_Sequence.class, childSeqId);
      Efin_Parent_Sequence parentSeq = OBDal.getInstance().get(Efin_Parent_Sequence.class,
          parentSeqId);

      if (childSeq != null && parentSeq != null) {

        if (childSeq.getNextSequence() != null) {
          if (childSeq.getNextSequence().intValue() > from || childSeq.getFINTo().intValue() < to) {
            result.put("retryExecution", true);
            JSONObject msg = new JSONObject();
            msg.put("severity", "error");
            msg.put("text", OBMessageUtils.messageBD("efin_notvalidfromto"));
            result.put("message", msg);
            return result;
          }

          User user = OBDal.getInstance().get(User.class, userId);
          if (user != null) {
            if (childSeq.getFINFrom().intValue() == from) {
              childSeq.setFINTo(new BigDecimal(to));
              childSeq.setUserContact(user);
              OBDal.getInstance().save(childSeq);
            } else {
              childSeq.setFINTo(new BigDecimal(from).subtract(BigDecimal.ONE));
              OBDal.getInstance().save(childSeq);
              OBDal.getInstance().flush();

              Efin_Child_Sequence childSeqNew = OBProvider.getInstance()
                  .get(Efin_Child_Sequence.class);
              childSeqNew.setFINFrom(new BigDecimal(from));
              childSeqNew.setFINTo(new BigDecimal(to));
              childSeqNew.setEfinParentSequence(childSeq.getEfinParentSequence());
              childSeqNew.setEfinPaymentSequences(childSeq.getEfinPaymentSequences());
              childSeqNew.setNextSequence(new BigDecimal(from));
              childSeqNew.setUserContact(user);
              OBDal.getInstance().save(childSeqNew);
            }
            OBDal.getInstance().flush();
          }
        }
      }

      successMessage.put("severity", "success");
      successMessage.put("text", OBMessageUtils.messageBD("efin_splitchildsuccess"));
      result.put("message", successMessage);
      return result;

    } catch (Exception e) {
      log.debug("Exception while sending notifcation" + e.getMessage(), e);
      try {
        OBDal.getInstance().rollbackAndClose();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("processfailed"));
        result.put("message", successMessage);
        return result;
      } catch (Exception ex) {
        // this case wont happen
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return successMessage;
  }
}
