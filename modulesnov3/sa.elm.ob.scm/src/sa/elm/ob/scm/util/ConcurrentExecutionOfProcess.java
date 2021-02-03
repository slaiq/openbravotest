
package sa.elm.ob.scm.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.scheduling.hook.ConcurrentExecuteProcessHook;
import org.openbravo.service.db.QueryTimeOutUtil;

public class ConcurrentExecutionOfProcess implements ConcurrentExecuteProcessHook {
  static Logger log4j = Logger.getLogger(ConcurrentExecutionOfProcess.class);

  public static final String C_ORDER_ID = "C_Order_ID";
  public static final String C_ORDERLINE_ID = "C_OrderLine_ID";
  public static final String ESCM_BIDMGMT_ID = "Escm_Bidmgmt_ID";
  public static final String M_REQUISIITON_ID = "M_Requisition_ID";
  public static final String ESCM_TECHNICALEVAL_EVENT_ID = "Escm_Technicalevl_Event_ID";
  public static final String PROPOSAL_MANAGEMENT_ID = "Escm_Proposalmgmt_ID";
  public static final String OPEN_ENVCOMMITTEE_ID = "Escm_Openenvcommitee_ID";
  public static final String PROPOSALEVL_EVENT_ID = "Escm_Proposalevl_Event_ID";
  public static final String PROPOSALMGMT_LINE_ID = "Escm_Proposalmgmt_Line_ID";
  public static final String PO_RECEIPT_ID = "M_InOut_ID";
  public static final String BG_WORKBENCH_ID = "Escm_Bgworkbench_ID";
  public static final String BG_EXTENSION_ID = "Escm_Bg_Extension_ID";
  public static final String BG_RELEASE_ID = "Escm_Bg_Release_ID";
  public static final String BG_CONFISCATION_ID = "Escm_Bg_Confiscation_ID";
  public static final String MATERIAL_REQUEST_ID = "Escm_Material_Request_ID";

  @Override
  public boolean exec(ConnectionProvider conn, Map<String, Object> bundleParam,
      String processRequestId) {
    String strSql = "", updateStrSql = "";
    List<String> resultList = new ArrayList<String>();
    PreparedStatement st = null;
    ResultSet result;
    JSONObject paramObject = null;
    String ids = null;
    List<String> idList = new ArrayList<String>();
    boolean concurrentExecute = false;
    String primaryKeyId = null;
    String id = null;
    try {
      strSql = " select params from ad_process_request where ad_process_id in ";

      if (bundleParam.containsKey(C_ORDER_ID)) {
        primaryKeyId = C_ORDER_ID;
        strSql = strSql
            + " ('06F0902CF5CB4FE1A70A994E34950FF7','478F7ABE3D0A4E3BBDD3DFC8FB1C93A1','FCD1C79F4814442EAF405FE0F7104138',"
            + " '03ABFE5912C64807BF946F9C8549285D','A9F7A95616D64E67BF1501FE05F6C003',"
            + "  '2D438A0506704D9C924398A8542E271E','B6D6404573E04572A4D011FBEC89A4AC'"
            + " ,'D5FF563B076F4C7CAFB610B8ED3B8A67')  ";
      } else if (bundleParam.containsKey(C_ORDERLINE_ID)) {
        primaryKeyId = C_ORDERLINE_ID;
        strSql = strSql + " ('5D4F0FD8070B4DADA344DE259BCFF197')  ";
      } else if (bundleParam.containsKey(ESCM_BIDMGMT_ID)) {
        primaryKeyId = ESCM_BIDMGMT_ID;
        strSql = strSql
            + " ('80211EFFB89A4C4891D22EDE8FE54F0B','BE578E3F82534020B78BF70313F61C24','093C796F7E214A14A70DA42693501B89',"
            + " '2D438A0506704D9C924398A8542E271E','A9F7A95616D64E67BF1501FE05F6C003') ";

      } else if (bundleParam.containsKey(M_REQUISIITON_ID)) {
        primaryKeyId = M_REQUISIITON_ID;
        strSql = strSql
            + " ('1D9C7D068EBE41FDACF1602BFD14CFC9','806DB0E8E5094AA9A303A430181B84D1','38718AB73F474CF4B1205D72A23047EB' ,"
            + " '2D438A0506704D9C924398A8542E271E','A9F7A95616D64E67BF1501FE05F6C003')";

      } else if (bundleParam.containsKey(ESCM_TECHNICALEVAL_EVENT_ID)) {
        primaryKeyId = ESCM_TECHNICALEVAL_EVENT_ID;
        strSql = strSql
            + " ('D1D99C7B68E14C58B5218FA2F18F1880','E5A18864DCF8434187A1960977497471','2D438A0506704D9C924398A8542E271E' , "
            + " 'A9F7A95616D64E67BF1501FE05F6C003')";

      } else if (bundleParam.containsKey(PROPOSAL_MANAGEMENT_ID)) {
        primaryKeyId = PROPOSAL_MANAGEMENT_ID;
        strSql = strSql
            + "  ('934EDED57B7040D98E295B05538F8F83','25FF9F943B7543518AE7365FCC1454A7','A88B2CF064664C0AAE117FADCB070751', "
            + " 'D9E65F8444854EAF8B4F347924CBEBE3','986FEB4FA93943B98AC6058D0F0427A8','207E047DA9854AFBA4D196889B20C1B1', "
            + " 'E3C5687154AC4B92A3F414E4704E92EE','CF3741287A2B4A9A8F5C59C60F7B3CED') ";
      } else if (bundleParam.containsKey(PROPOSALMGMT_LINE_ID)) {
        primaryKeyId = PROPOSALMGMT_LINE_ID;
        strSql = strSql + " ('7833E1618E754C59AFBCAE1C14BB1A93') ";
      }

      else if (bundleParam.containsKey(OPEN_ENVCOMMITTEE_ID)) {
        primaryKeyId = OPEN_ENVCOMMITTEE_ID;
        strSql = strSql
            + "  ('4753925FCB4A4831BB3EFC9CCDA75FDE','74998EE10A594F04A88EAFED7CD5B691') ";
      } else if (bundleParam.containsKey(PROPOSALEVL_EVENT_ID)) {
        primaryKeyId = PROPOSALEVL_EVENT_ID;
        strSql = strSql
            + " ('AC82649C4F5B4A0581C19C59E0DE0776','C839E66D63B34D67A65FE14A8BA24850','374710BCCE554A1EB66D243B7CBA7407')";
      }

      else if (bundleParam.containsKey(PO_RECEIPT_ID)) {
        primaryKeyId = PO_RECEIPT_ID;
        strSql = strSql
            + " ('D3DC5680B6EE4A5098151AC145CC9CA2','708D305269134EBF8A92E107D7EB6443' ,'66590FAFF644431CA350BBD39959C98D' "
            + " ,'2D438A0506704D9C924398A8542E271E','A9F7A95616D64E67BF1501FE05F6C003' ,'9211D7FFE3C141F392FF9394BA47FAAE',"
            + "  '55CE3C3D83CE460994382A4E8CF5ED7C','30D86BB771BC4077AA5874A8DF0B9F2D' , "
            + " 'B70BF480577044C7A5F09B2DC1588BF5','0DECBACC03C841B4966D0CCBCA76A5D7','5408B425EBE1436CB0E6294FA66BFBF2','D19D3492C9F34F159C825F74371EBE0F',"
            + " '223252D9C7EC40B5897D3AF0A7E9471D','109' ) ";
      } else if (bundleParam.containsKey(BG_WORKBENCH_ID)) {
        primaryKeyId = BG_WORKBENCH_ID;
        strSql = strSql + " ('423DFE9BDAC14FF7A15109DC5EDCB57C') ";
      } else if (bundleParam.containsKey(BG_EXTENSION_ID)) {
        primaryKeyId = BG_EXTENSION_ID;
        strSql = strSql + " ('6DE64DAB3D154FCC8D819EF645725B92') ";
      } else if (bundleParam.containsKey(BG_RELEASE_ID)) {
        primaryKeyId = BG_RELEASE_ID;
        strSql = strSql + " ('6DE64DAB3D154FCC8D819EF645725B92') ";
      } else if (bundleParam.containsKey(BG_CONFISCATION_ID)) {
        primaryKeyId = BG_CONFISCATION_ID;
        strSql = strSql + " ('6DE64DAB3D154FCC8D819EF645725B92') ";
      } else if (bundleParam.containsKey(MATERIAL_REQUEST_ID)) {
        primaryKeyId = MATERIAL_REQUEST_ID;
        strSql = strSql
            + " ('8A8E2D6AE70D4FD698251C06FA155BE1','267476CD57A6494D8FC8DDCED94CB7AE','7BE931924CC4485884AE219F175231F3',"
            + "  'C7884FE727E84484B613FA008001F53E','2D438A0506704D9C924398A8542E271E','A9F7A95616D64E67BF1501FE05F6C003','01A8838D70654DD7A0F1CFCE6301A14B' "
            + " ,'AAEC892AA5EA4BA78C02593ABEF9125C','B24158F0DB88404FAACAD2E2A860C9A6' ) ";
      }
      strSql = strSql
          + " and( status='SCH' or ( em_eut_recordid= ?  and created > now() - interval '2 s'  and em_eut_recordid is not null) )  ";
      if (primaryKeyId != null) {
        id = bundleParam.get(primaryKeyId).toString();

        updateStrSql = " update ad_process_request set em_eut_recordid = ? where  ad_process_request_id = ?  ";
        st = conn.getPreparedStatement(updateStrSql);
        st.setString(1, id);
        st.setString(2, processRequestId);
        QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
        log4j.debug("st:" + st.toString());
        log4j.debug("st:" + st.toString());
        st.executeUpdate();

        st = conn.getPreparedStatement(strSql);
        QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
        st.setString(1, id);
        log4j.debug("st:" + st.toString());
        result = st.executeQuery();
        while (result.next()) {
          resultList.add(UtilSql.getValue(result, "params"));
        }

        for (String params : resultList) {

          paramObject = new JSONObject(params);
          ids = paramObject.get(primaryKeyId).toString();
          if (id != null && id.equals(ids)) {
            if (!idList.contains(ids)) {
              idList.add(paramObject.get(primaryKeyId).toString());
            } else {
              concurrentExecute = true;
            }
          }
        }

        result.close();
      }
    } catch (SQLException e) {
      concurrentExecute = true;
      log4j.error("Exception in selectConcurrent:" + e);
    } catch (Exception ex) {
      concurrentExecute = true;
      log4j.error("Exception in selectConcurrent:" + ex);
    } finally {
      try {
        conn.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return concurrentExecute;

  }

}