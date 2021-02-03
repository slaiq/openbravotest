package sa.elm.ob.scm.ad_process.BankGuarantee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import sa.elm.ob.scm.Escmbankguaranteedetail;

/**
 * 
 * @author qualian
 * 
 */
public class BankGuaranteeBackgroundProcess extends DalBaseProcess {
  private ProcessLogger logger;
  private static final Logger log = Logger.getLogger(BankGuaranteeBackgroundProcess.class);

  /**
   * This process used to update the bank guarantee status as expired, when it reaches after three
   * days of expire date .
   */
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    logger = bundle.getLogger();
    OBError result = new OBError();
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();

      st = conn.prepareStatement(
          " select (coalesce(ext.reqexpiry_date,det.expirydateh)+ interval '3' day ),det.escm_bankguarantee_detail_id from escm_bankguarantee_detail det "
              + " left join escm_bg_extension ext on ext.escm_bankguarantee_detail_id=det.escm_bankguarantee_detail_id "
              + "  AND ext.created = ( SELECT max(extend.created) AS max       FROM escm_bg_extension extend"
              + "     WHERE extend.escm_bankguarantee_detail_id::text = ext.escm_bankguarantee_detail_id::text)  where   "
              + "     to_char((coalesce(ext.reqexpiry_date,det.expirydateh)+ interval '3' day ),'yyyy-MM-dd') <=to_char(now(),'yyyy-MM-dd')");
      log.debug("query:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        log.debug("background:" + rs.getString("escm_bankguarantee_detail_id"));
        Escmbankguaranteedetail bankguarantee = OBDal.getInstance()
            .get(Escmbankguaranteedetail.class, rs.getString("escm_bankguarantee_detail_id"));
        bankguarantee.setUpdated(new java.util.Date());
        bankguarantee.setBgstatus("EXP");
        OBDal.getInstance().save(bankguarantee);
      }
      addLog(OBMessageUtils.messageBD("OBUIAPP_Success"));

      bundle.setResult(result);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      result = OBMessageUtils.translateError(bundle.getConnection(), bundle.getContext().toVars(),
          OBContext.getOBContext().getLanguage().getLanguage(),
          OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      log.error(result.getMessage(), e);
      addLog(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log.error("Exception while closing the statement in BankGuaranteeBackgroundProcess ", e);
      }
      OBContext.restorePreviousMode();
      addLog("Ending background process.");
    }
  }

  /**
   * Adds a message to the log.
   * 
   * @param msg
   *          to add to the log
   */
  private void addLog(String msg) {
    addLog(msg, false);
  }

  /**
   * Add a message to the log.
   * 
   * @param msg
   * @param generalLog
   */
  private void addLog(String msg, boolean generalLog) {
    logger.log(msg + "\n");
  }

  public void kill(ProcessBundle processBundle) throws Exception {
    addLog("Process Killed");
  }
}
