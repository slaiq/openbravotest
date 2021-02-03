
package sa.elm.ob.finance.util;

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
  public static final String C_INVOICE_ID = "C_Invoice_ID";
  public static final String RDV_TRANSACTION_ID = "Efin_Rdvtxn_ID";
  public static final String EFIN_BUDGET_ID = "Efin_Budget_ID";
  public static final String EFIN_BUDGET_TRANSFERTRX_ID = "Efin_Budget_Transfertrx_ID";
  public static final String EFIN_BUDGETADJ_ID = "Efin_Budgetadj_ID";
  public static final String EFIN_FUNDSREQ_ID = "Efin_Fundsreq_ID";
  public static final String EFIN_BUDGETMANENCUM_ID = "Efin_Budget_Manencum_ID";
  public static final String FIN_PAYMENT_ID = "Fin_Payment_ID";
  public static final String C_ELEMENTVALUE_ID = "C_ElementValue_ID";

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

      if (bundleParam.containsKey(C_INVOICE_ID)) {
        primaryKeyId = C_INVOICE_ID;
        strSql = strSql
            + "  ('9BD61A3D56974380B943C1AD137B5644','CD941950665F4649816DA8C9B7384717','81056F8B8DD046B99789B39B86832414') ";

      } else if (bundleParam.containsKey(RDV_TRANSACTION_ID)) {
        primaryKeyId = RDV_TRANSACTION_ID;
        strSql = strSql
            + "  ('B3C703889C3D494BA81602AA961F431A','4BFB215C0D6F48C4B4C88099D8155B90','24B8938EACAB412481F39CED60DBD28F', "
            + " '65F4767DCB3442C686F0F01E5C1A452B','8135B6C92A24440DA50FF248C3AB9C64','BDE2B53C21B1427D8F71D5929A68DE58',"
            + " 'A9F7A95616D64E67BF1501FE05F6C003','2D438A0506704D9C924398A8542E271E' )  ";

      } else if (bundleParam.containsKey(EFIN_BUDGET_ID)) {
        primaryKeyId = EFIN_BUDGET_ID;
        strSql = strSql
            + "  ('620E2824FC9546508528BF03C23015D1','F9CF8F5779E141D29407FC06C4771E4B','6C15B52962524559A6003B94A8A435F6','AEB46427356E4271949DFD10E621BF94',"
            + " '2D438A0506704D9C924398A8542E271E','A9F7A95616D64E67BF1501FE05F6C003','A474B7FBA6A24A05ABC4FB2E87202C7A','964FD7A8F58347538B105E3DA35A5216')  ";

      } else if (bundleParam.containsKey(EFIN_BUDGET_TRANSFERTRX_ID)) {
        primaryKeyId = EFIN_BUDGET_TRANSFERTRX_ID;
        strSql = strSql
            + "  ('BAC17885655E44D0A492B5277A69EE22','A80269ECBA054737A066B95625B67335','0E07D8C86F88456AAF6686517B4B919B','9EBCEB8367A64760B655627452E505F4' "
            + " ,'2D438A0506704D9C924398A8542E271E','A9F7A95616D64E67BF1501FE05F6C003')  ";

      } else if (bundleParam.containsKey(EFIN_BUDGETADJ_ID)) {
        primaryKeyId = EFIN_BUDGETADJ_ID;
        strSql = strSql
            + "  ('E1A9B466EA5940E9984B8D4A6FD50351','CCA0E69A1FE249E0B3DDBD5FEB545AB9','2C8E0874716340F7B35127768D26A27B','D7A637C64D7945C0906B70E8F71AB06D',"
            + " '2D438A0506704D9C924398A8542E271E','A9F7A95616D64E67BF1501FE05F6C003')  ";

      } else if (bundleParam.containsKey(EFIN_FUNDSREQ_ID)) {
        primaryKeyId = EFIN_FUNDSREQ_ID;
        strSql = strSql
            + "  ('2D438A0506704D9C924398A8542E271E','FA8C47FE858C4CF0A2A32526DB58DBC4','DECD897F8A774037BB0748C5561F592D','8BF0C9274A1B42968DDA7FB2950CC959' "
            + " ,'A9F7A95616D64E67BF1501FE05F6C003')  ";

      } else if (bundleParam.containsKey(EFIN_BUDGETMANENCUM_ID)) {
        primaryKeyId = EFIN_BUDGETMANENCUM_ID;
        strSql = strSql
            + "  ('12D616F02AF24195ADA8FB5C6DF85CC2','84746F13EE784666A573AE69019C6B8F','83984786D2484658B0AE3B3912DFCFC9','09C4B4E3F04D404BB8863527542D073C' "
            + " ,'B8EFEF7FBB2D46BC83445FA3ACE403AE','2D438A0506704D9C924398A8542E271E','53106DB7919540D29BB89217EA47077A','A9F7A95616D64E67BF1501FE05F6C003')  ";

      } else if (bundleParam.containsKey(FIN_PAYMENT_ID)) {
        primaryKeyId = FIN_PAYMENT_ID;
        strSql = strSql
            + "  ('D69944B5CE424A7CB179499D64C448EB','759FFDE23AB5495D9CE554985A159F71','E011F492B0814A74B63CD1F3B9FF0526','6255BE488882480599C81284B70CD9B3'"
            + " ,'29D17F515727436DBCE32BC6CA28382B')  ";

      } else if (bundleParam.containsKey(C_ELEMENTVALUE_ID)) {
        primaryKeyId = C_ELEMENTVALUE_ID;
        strSql = strSql
            + "  ('267ABDE7C7EA4FC9A2AD2655B86D59BB','7E3F31A49CE140AD99EE100B7C437AAC','E58C23A51B334C978C2D37244DEBE85A')  ";

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
      log4j.error("Exception in exec in ConcurrentExecutionOfProcess:" + e);
    } catch (Exception ex) {
      concurrentExecute = true;
      log4j.error("Exception in exec in ConcurrentExecutionOfProcess:" + ex);
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