package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.scm.properties.Resource;

/**
 * 
 * @author Gopalakrishnan on 08/05/2017
 * 
 */
public class MIRReturnTransactionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(MIRReturnTransactionHandler.class);

  @SuppressWarnings("resource")
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject custodyLines = jsonparams.getJSONObject("issue_return_mir");
      JSONArray selectedlines = custodyLines.getJSONArray("_selection");
      MaterialIssueRequestLine objRequestLine = null;

      Connection conn = OBDal.getInstance().getConnection();
      long line = 10, custline = 10, custline2 = 0;
      final String mRequestLineId = jsonRequest.getString("inpescmMaterialReqlnId");
      objRequestLine = OBDal.getInstance().get(MaterialIssueRequestLine.class, mRequestLineId);
      MaterialIssueRequest objRequest = objRequestLine.getEscmMaterialRequest();
      if (Integer.valueOf(selectedlines.length()) != objRequestLine.getDeliveredQantity()
          .intValue()) {
        if ((new BigDecimal(objRequestLine.getEscmCustodyTransactionList().size())
            .add(new BigDecimal(selectedlines.length())))
                .compareTo(new BigDecimal(objRequestLine.getDeliveredQantity().intValue())) != 0) {
          JSONObject successMessage = new JSONObject();
          successMessage.put("severity", "error");
          successMessage.put("text", OBMessageUtils.messageBD("Escm_Issued_Qty_Less").replace("@",
              String.valueOf(objRequestLine.getDeliveredQantity())));
          json.put("message", successMessage);
          return json;
        }
      }

      // delete exisiting lines
      if (selectedlines.length() > 0) {
        for (int a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);

          // getting max line no count for line
          ps = conn.prepareStatement(
              " select coalesce(max(line),0)+10 as lineno from escm_custody_transaction where escm_material_reqln_id  = ? ");
          ps.setString(1, mRequestLineId);
          rs = ps.executeQuery();
          if (rs.next()) {
            custline = rs.getLong("lineno");
            log.debug("line:" + line);
          }
          // getting max line no count for line2
          ps = conn.prepareStatement(
              " select coalesce(max(line2),0)+10 as lineno from escm_custody_transaction where escm_mrequest_custody_id  = ? ");
          ps.setString(1, selectedRow.getString("id"));
          rs = ps.executeQuery();
          if (rs.next()) {
            custline2 = rs.getLong("lineno");
            log.debug("line:" + custline2);
          }

          Escm_custody_transaction custtransaction = OBProvider.getInstance()
              .get(Escm_custody_transaction.class);
          MaterialIssueRequestCustody custodydetial = OBDal.getInstance()
              .get(MaterialIssueRequestCustody.class, selectedRow.getString("id"));
          custtransaction.setClient(objRequestLine.getClient());
          custtransaction.setOrganization(objRequestLine.getOrganization());
          custtransaction.setCreationDate(new java.util.Date());
          custtransaction.setCreatedBy(objRequestLine.getCreatedBy());
          custtransaction.setUpdated(new java.util.Date());
          custtransaction.setUpdatedBy(objRequestLine.getUpdatedBy());
          custtransaction.setActive(true);
          custtransaction.setEscmMaterialReqln(objRequestLine);
          custtransaction.setLineNo(custline);
          custtransaction.setDocumentNo(objRequestLine.getEscmMaterialRequest().getDocumentNo());
          custtransaction.setEscmMrequestCustody(custodydetial);
          // log.debug("reason:" + inout.getEscmIssuereason());
          custtransaction.setLine2(custline2);
          custtransaction
              .setTransactionreason(Resource.getProperty("scm.IssueReturnMIR", vars.getLanguage()));
          custtransaction.setBname(objRequest.getBeneficiaryIDName());
          custtransaction.setBtype(objRequest.getBeneficiaryType());
          custtransaction.setTransactionDate(objRequest.getTransactionDate());
          custline++;
          OBDal.getInstance().save(custtransaction);
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
      log.error("Exception in MIRReturnTransactionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      try {
        if (rs != null)
          rs.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }
  }
}
