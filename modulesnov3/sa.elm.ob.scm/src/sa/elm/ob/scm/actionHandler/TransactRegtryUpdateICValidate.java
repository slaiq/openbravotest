package sa.elm.ob.scm.actionHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

public class TransactRegtryUpdateICValidate implements Process {
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(TransactRegtryUpdateICValidate.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    final String userId = (String) bundle.getContext().getUser();
    final String specNo = vars.getStringParameter("inptransactionType");
    /*
     * EscmTranRegICValidateV tranReg = OBDal.getInstance().get(EscmTranRegICValidateV.class,
     * transactionId); log.info("transactionId>" + transactionId); final String specNo =
     * tranReg.getSpecno(); final String orgId = tranReg.getInvoiceOrganization().getId();
     */

    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    int update = 0;
    try {
      OBContext.setAdminMode(true);
      Connection conn = OBDal.getInstance().getConnection();
      if (specNo == null || specNo.equals("")) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_SelectTranType@");
        bundle.setResult(result);
        return;
      }
      sql = "select m_transaction_id from " + " m_transaction tr  "
          + " left join escm_material_reqln re on tr.em_escm_material_reqln_id=re.escm_material_reqln_id "
          + " left join  escm_material_request mr on mr.escm_material_request_id=re.escm_material_request_id "
          + " left join m_inoutline io on io.m_inoutline_id=tr.m_inoutline_id "
          + " left join m_inout ioh on ioh.m_inout_id = io.m_inout_id "
          + " where em_escm_ic='N' and coalesce(mr.specno,coalesce(ioh.em_escm_specno,''))=? and tr.ad_client_id=? ";

      st = conn.prepareStatement(sql);
      st.setString(1, specNo);
      st.setString(2, vars.getClient());
      // st.setString(3, orgId);
      rs = st.executeQuery();
      while (rs.next()) {
        st = conn.prepareStatement(
            "update m_transaction set em_escm_ic = 'Y', updated=now(), updatedBy=? where m_transaction_id =?");
        st.setString(1, userId);
        st.setString(2, rs.getString("m_transaction_id"));
        update = st.executeUpdate();
      }
      if (update == 1) {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_ICValUpdated@");
        bundle.setResult(result);
        return;
      } else {
        bundle.setResult(obError);
      }
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log.error("connection close exception :", e);
      }
      OBContext.restorePreviousMode();
    }
  }
}
