package sa.elm.ob.scm.actionHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMAnnouSummaryBid;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmannoucements;
import sa.elm.ob.utility.util.UtilityDAO;

public class ActSummaryBidAddLinesHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(ActSummaryBidAddLinesHandler.class);
  PreparedStatement ps = null;
  ResultSet rs = null;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject purreqline = jsonparams.getJSONObject("Lines");
      JSONArray selectedlines = purreqline.getJSONArray("_selection");
      Connection conn = OBDal.getInstance().getConnection();
      DateFormat dateyearFormat = new SimpleDateFormat("yyyy-MM-dd");

      long lineno = 10;
      final String annoucementId = jsonRequest.getString("Escm_Annoucements_ID");
      Escmannoucements annoucement = OBDal.getInstance().get(Escmannoucements.class, annoucementId);
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);

          ps = conn.prepareStatement(
              " select coalesce(max(line),0)+10   as lineno from escm_annousummarybid where escm_annoucements_id=?");
          ps.setString(1, annoucement.getId());
          log.debug("rs:" + ps.toString());
          rs = ps.executeQuery();
          if (rs.next()) {
            lineno = rs.getLong("lineno");
          }

          ESCMAnnouSummaryBid bid = OBProvider.getInstance().get(ESCMAnnouSummaryBid.class);
          EscmBidMgmt bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class,
              selectedRow.getString("id"));

          bid.setClient(annoucement.getClient());
          bid.setOrganization(annoucement.getOrganization());
          bid.setCreationDate(new java.util.Date());
          bid.setCreatedBy(annoucement.getCreatedBy());
          bid.setUpdated(new java.util.Date());
          bid.setUpdatedBy(annoucement.getUpdatedBy());
          bid.setLineNo(lineno);
          bid.setEscmBidmgmt(
              OBDal.getInstance().get(EscmBidMgmt.class, selectedRow.getString("id")));
          bid.setEscmAnnoucements(annoucement);
          bid.setBidNo(bidmgmt.getBidno());
          bid.setBidName(bidmgmt.getBidname());
          bid.setBidStatus(bidmgmt.getBidstatus());
          if (bidmgmt.getRfpprice() != null)
            bid.setRFPPriceSAR(bidmgmt.getRfpprice());
          if (selectedRow.getString("openEnvelopDayTimeH") != null
              && !selectedRow.getString("openEnvelopDayTimeH").equals("")
              && !selectedRow.getString("openEnvelopDayTimeH").equals("null")) {
            String opendate = selectedRow.getString("openEnvelopDayTimeH");
            opendate = opendate.split(" ")[0];
            opendate = (opendate.split("-")[2] + opendate.split("-")[1] + opendate.split("-")[0]);// UtilityDAO.eventConvertToGregorian(
            log.debug("opendate:" + opendate); // .split("T")[0]
            opendate = UtilityDAO.convertToGregorian(opendate);
            log.debug("opendate123:" + opendate);
            bid.setOpenenvday(dateyearFormat.parse(opendate));
          }
          if (selectedRow.getString("openenvdaytime") != null
              && !selectedRow.getString("openenvdaytime").equals("")
              && !selectedRow.getString("openenvdaytime").equals("null")) {
            /*
             * Calendar calendar = Calendar.getInstance(); calendar.setTime(bid.getOpenenvday());
             * calendar.set(Calendar.MINUTE,
             * Integer.parseInt(selectedRow.getString("openenvdaytime").split(":")[1]));
             * calendar.set(Calendar.HOUR,
             * Integer.parseInt(selectedRow.getString("openenvdaytime").split(":")[0])); long date =
             * calendar.getTimeInMillis(); String opendaytime = dateyearFormatwithsec.format(date);
             * 
             * bid.setOpenenvday(dateyearFormatwithsec.parse(opendaytime));
             */
            bid.setOpenenvdaytime(selectedRow.getString("openenvdaytime"));
            // }
          }
          OBDal.getInstance().save(bid);
          OBDal.getInstance().flush();
        }
        OBDal.getInstance().flush();
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ProcessOK"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_POAddRecIns"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      log.error("Exception in ActSummaryBidAddLinesHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      try {
        // close connection
        if (rs != null) {
          rs.close();
        }
        if (ps != null) {
          ps.close();
        }
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }
  }
}
