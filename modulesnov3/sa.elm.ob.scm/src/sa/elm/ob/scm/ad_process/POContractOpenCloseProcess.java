package sa.elm.ob.scm.ad_process;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.utility.util.ApprovalTables;

/**
 * 
 * @author Gopalakrishnan
 * 
 *         This class is responsible to close or open the purchase order
 */
public class POContractOpenCloseProcess extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(POContractOpenCloseProcess.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();
      final String orderId = (String) bundle.getParams().get("C_Order_ID").toString();
      final String action = (String) bundle.getParams().get("action").toString();
      final String comments = (String) bundle.getParams().get("comments").toString();
      Order objOrder = OBDal.getInstance().get(Order.class, orderId);
      if (action.equals("CL")) {
        objOrder.setEscmAppstatus("ESCM_CL");
      } else {
        objOrder.setEscmAppstatus("ESCM_AP");
      }
      OBDal.getInstance().save(objOrder);
      OBDal.getInstance().flush();
      if (!StringUtils.isEmpty(orderId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", vars.getClient());
        historyData.put("OrgId", vars.getOrg());
        historyData.put("RoleId", vars.getRole());
        historyData.put("UserId", vars.getUser());
        historyData.put("HeaderId", orderId);
        historyData.put("Comments", comments);
        historyData.put("Status", action.equals("CL") ? "CD" : "OP");
        historyData.put("Revision", objOrder.getEscmRevision());
        historyData.put("NextApprover", "");
        historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
        historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);

        POContractSummaryDAO.purchaseOrderApprovalHistory(historyData);

      }
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_Process_Success@");
      bundle.setResult(result);
      return;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in POContractOpenCloseProcess:" + e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

}
