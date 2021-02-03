package sa.elm.ob.scm.ad_process.BidManagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class BidClose extends DalBaseProcess {
  private ProcessLogger logger = null;
  private Connection connection = null;
  private static Logger log4j = Logger.getLogger(BidClose.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    logger = bundle.getLogger();
    ProcessContext context = bundle.getContext();
    connection = bundle.getConnection().getConnection();
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    logger.logln("closeBid process started.");
    closeBid(context.getClient(), context.getOrganization(), bundle, vars);
    logger.logln("closeBid process finished.");
  }

  private void closeBid(String clientId, String orgId, ProcessBundle bundle,
      VariablesSecureApp vars) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    // DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    // Date systemDateAndTime = Calendar.getInstance().getTime();
    Calendar cl = Calendar.getInstance();

    try {
      String systemDateStr = dateFormat.format(new Date());
      Date systemDate = dateFormat.parse(systemDateStr);
      logger.logln("systemDateStr>" + systemDate);
      String openEnvDateStr = "";
      Date openEnvDate = null;

      ps = connection.prepareStatement(
          "select bidmgmt.escm_bidmgmt_id, bidtype, bidstatus, bidappstatus, openenvday, openenvdaytime "
              + " from escm_bidmgmt bidmgmt left join escm_biddates biddt on biddt.escm_bidmgmt_id=bidmgmt.escm_bidmgmt_id where bidmgmt.ad_client_id=? "
              + " and bidstatus='ACT' and openenvday = (select max(openenvday) from escm_biddates where escm_bidmgmt_id=bidmgmt.escm_bidmgmt_id group by escm_bidmgmt_id)");
      ps.setString(1, clientId);
      log4j.info("bid qry>" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        /* Format Date */
        openEnvDateStr = rs.getString("openenvday");
        openEnvDate = dateFormat.parse(openEnvDateStr);
        cl.setTime(openEnvDate);
        openEnvDateStr = dateFormat.format(cl.getTime());
        /* Format Time */
        // openEnvTimestr = rs.getString("openenvdaytime");
        /*
         * openEnvTime = dateTimeFormat.parse(openEnvTimestr); cl.setTime(openEnvTime);
         * openEnvTimestr = timeFormat.format(cl.getTime());
         */
        /* Append Date and Time */
        /*
         * openEnvDateTimeStr = openEnvDateStr+" "+openEnvTimestr; openEnvDateTime =
         * dateTimeFormat.parse(openEnvDateTimeStr);
         * logger.logln("openEnvDateTime>"+openEnvDateTime);
         */
        /*
         * Compare OpenEnvelop date & time with sytem date and time and close bid if they are same
         */
        if (systemDate.compareTo(openEnvDate) == 0 || systemDate.compareTo(openEnvDate) > 0) {
          ps = connection
              .prepareStatement("update escm_bidmgmt set bidstatus='CD' where escm_bidmgmt_id =? ");
          ps.setString(1, rs.getString("escm_bidmgmt_id"));
          ps.executeUpdate();
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in closeBid() : ", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in closeBid() : ", e);
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    }
  }
}