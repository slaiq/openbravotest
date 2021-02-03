package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class RequisitionCancelHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(RequisitionCancelHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
      User user = OBDal.getInstance().get(User.class, vars.getUser());
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      JSONObject lines = jsonparams.getJSONObject("m_requisitionline_id");
      JSONArray selectedlines = lines.getJSONArray("_selection");
      String headerReason = jsonparams.getString("reason");
      String inpdocumentno = jsonRequest.getString("inpdocumentno");
      String clientId = vars.getClient();
      String alertWindow = AlertWindow.PurchaseRequisition;
      String appstatus = "";
      String LineNo = "";
      String UsedLineNo = "";
      boolean errorproposal = false;
      boolean errororder = false;
      Requisition requisition = null;
      List<EfinBudgetManencumlines> encumLinesList = null;
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      // find all
      int Count = 0;
      String HeaderId = "";
      String windowId = "800092";

      // End Task No.5925

      if (selectedlines.length() > 0) {
        JSONObject selectedRowall = selectedlines.getJSONObject(0);
        HeaderId = selectedRowall.getString("requisition");
        Requisition requisitionObj = OBDal.getInstance().get(Requisition.class, HeaderId);
        // check pr encumbrance type is enable or not .. Task No.5925
        OBQuery<EfinEncControl> encumcontrol = OBDal.getInstance().createQuery(EfinEncControl.class,
            " as e where e.encumbranceType='PRE' and e.client.id='"
                + requisitionObj.getClient().getId() + "' and e.active='Y' and e.typeList ='"
                + requisitionObj.getEscmProcesstype() + "'");
        encumcontrol.setFilterOnActive(true);
        encumcontrol.setMaxResult(1);
        enccontrollist = encumcontrol.list();

        // Task No.5925

        OBQuery<RequisitionLine> Lines = OBDal.getInstance().createQuery(RequisitionLine.class,
            "requisition.id='" + HeaderId + "' and escmStatus='ESCM_AP'");
        if (Lines.list() != null) {
          Count = Lines.list().size();
          requisition = Lines.list().get(0).getRequisition();
          if (enccontrollist.size() > 0 && !requisitionObj.isEfinSkipencumbrance()) {
            OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance().createQuery(
                EfinBudgetManencumlines.class,
                " manualEncumbrance.id='" + requisition.getEfinBudgetManencum().getId() + "'");
            if (encumLines.list() != null && encumLines.list().size() > 0) {
              encumLinesList = encumLines.list();
            }
          }
        }
        // ENd Task No.5925

        boolean error = false;
        Date currentDate = new Date();
        int a = 0;
        for (a = 0; a < selectedlines.length(); a++) {
          JSONObject selectedRow = selectedlines.getJSONObject(a);
          log.debug("selectedRow:" + selectedRow);
          String LineId = selectedRow.getString("id");
          RequisitionLine Line = OBDal.getInstance().get(RequisitionLine.class, LineId);
          if (Line.getEscmBidmgmtQty().compareTo(BigDecimal.ZERO) > 0) {
            error = true;
            if (!UsedLineNo.equals(""))
              UsedLineNo = UsedLineNo + "," + Line.getLineNo().toString();
            else
              UsedLineNo = Line.getLineNo().toString();
          }
          if (Line.getEscmAwardedQty().compareTo(BigDecimal.ZERO) > 0) {
            errorproposal = true;
            if (!UsedLineNo.equals(""))
              UsedLineNo = UsedLineNo + "," + Line.getLineNo().toString();
            else
              UsedLineNo = Line.getLineNo().toString();
          }
          if (Line.getEscmPoQty().compareTo(BigDecimal.ZERO) > 0) {
            errororder = true;
            if (!UsedLineNo.equals(""))
              UsedLineNo = UsedLineNo + "," + Line.getLineNo().toString();
            else
              UsedLineNo = Line.getLineNo().toString();
          }

          else {
            RequisitionCancelHandlerDAO.updateChildAsCancel(Line, selectedRow, LineNo, currentDate,
                user);
            // End Task No.5925
            // Task No.5925
            if (enccontrollist.size() > 0 && !requisitionObj.isEfinSkipencumbrance()) {
              if (requisition.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                // call method to reduce amt in maual encumbrance.
                RequisitionDao.updateManualEncumAmountRej(requisition, encumLinesList, true,
                    LineId);
              } else {
                // call method to reduce amt in auto encumbrance.
                RequisitionDao.updateAmtInEnquiryRej(requisition.getId(), encumLinesList, true,
                    LineId);
              }
            }
          }
        }
        if (error) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject ErrorMessage = new JSONObject();
          ErrorMessage.put("severity", "error");
          ErrorMessage.put("text",
              OBMessageUtils.messageBD("Escm_PR_Used").replace("%", UsedLineNo));
          json.put("message", ErrorMessage);
          return json;
        } else if (errorproposal) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject ErrorMessage = new JSONObject();
          ErrorMessage.put("severity", "error");
          ErrorMessage.put("text",
              OBMessageUtils.messageBD("Escm_Proposal_Used").replace("%", UsedLineNo));
          json.put("message", ErrorMessage);
          return json;
        } else if (errororder) {
          OBDal.getInstance().rollbackAndClose();
          JSONObject ErrorMessage = new JSONObject();
          ErrorMessage.put("severity", "error");
          ErrorMessage.put("text",
              OBMessageUtils.messageBD("Escm_PO_Used").replace("%", UsedLineNo));
          json.put("message", ErrorMessage);
          return json;
        }

        String sql = null;
        Query qry = null;
        BigInteger countLne = BigInteger.ZERO;
        sql = "select count(m_requisitionline_id) from m_requisitionline where m_requisition_id ='"
            + requisition.getId() + "' and em_escm_status NOT IN ('ESCM_CA') ";
        qry = OBDal.getInstance().getSession().createSQLQuery(sql);
        if (qry != null && qry.list().size() > 0) {
          countLne = (BigInteger) qry.list().get(0);
        }

        if (countLne.compareTo(BigInteger.ZERO) == 0) {
          Requisition Header = OBDal.getInstance().get(Requisition.class, HeaderId);
          Header.setEscmCancelDate(currentDate);
          Header.setEscmCancelledby(user);
          Header.setEscmCancelReason(headerReason);
          Header.setEscmDocStatus("ESCM_CA");
          // Header.setDocumentStatus("ESCM_CA");
          OBDal.getInstance().save(Header);

          // Cancel encumbrance in case of AUTO
          if (StringUtils.isNotEmpty(Header.getEfinEncumMethod())
              && "A".equals(Header.getEfinEncumMethod())) {
            if (Header.getEfinBudgetManencum() != null) {
              EfinBudgetManencum encumbrance = Header.getEfinBudgetManencum();
              encumbrance.setDocumentStatus("CA");
              OBDal.getInstance().save(encumbrance);
            }
          }
        }
        OBDal.getInstance().flush();
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION);
        DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
            Resource.PURCHASE_REQUISITION_LIMITED);

        // delete alert for approval alerts
        OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
            "as e where e.referenceSearchKey='" + HeaderId + "' and e.alertStatus='NEW'");
        if (alertQuery.list().size() > 0) {
          for (Alert objAlert : alertQuery.list()) {
            OBDal.getInstance().remove(objAlert);
          }
        }

        // get Requisition Alert
        OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
            "as e where e.client.id='" + clientId + "' and e.eSCMProcessType='" + alertWindow
                + "'");
        if (queryAlertRule.list().size() > 0) {
          AlertRule objRule = queryAlertRule.list().get(0);
          objRule.getId();
        }
        Requisition objRequisition = OBDal.getInstance().get(Requisition.class, HeaderId);

        String description = "";

        if (a == Count) {
          description = OBMessageUtils.messageBD("Escm_Requisition_CalcelledAlert");
          appstatus = "CA";
          JSONObject historyData = new JSONObject();
          historyData.put("ClientId", clientId);
          historyData.put("OrgId", objRequisition.getOrganization().getId());
          historyData.put("RoleId", vars.getRole());
          historyData.put("UserId", vars.getUser());
          historyData.put("HeaderId", HeaderId);
          historyData.put("Comments", headerReason);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", "");
          historyData.put("HistoryTable", ApprovalTables.REQUISITION_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.REQUISITION_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.REQUISITION_DOCACTION_COLUMN);
          Utility.InsertApprovalHistory(historyData);
          // set alert for Procurement Director
          AlertUtility.alertInsertionPreference(objRequisition.getId(),
              objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
              "ESCM_ProcurementDirector", objRequisition.getClient().getId(), description, "NEW",
              alertWindow, "scm.pr.cancelled", Constants.GENERIC_TEMPLATE, windowId, null);
        } else {
          description = OBMessageUtils.messageBD("Escm_Requisition_Alert_Replace").replace("%",
              inpdocumentno) + LineNo;
        }
        // set alert for Budget Controller
        if (objRequisition.isEfinEncumbered() != null && objRequisition.isEfinEncumbered()) {
          AlertUtility.alertInsertionPreference(objRequisition.getId(),
              objRequisition.getDocumentNo() + "-" + objRequisition.getDescription(),
              "ESCM_BudgetControl", objRequisition.getClient().getId(), description, "NEW",
              alertWindow, "scm.pr.cancelled", Constants.GENERIC_TEMPLATE, windowId, null);
        }
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "success");
        successMessage.put("text", OBMessageUtils.messageBD("ESCM_Requisition_Cancelled"));
        json.put("message", successMessage);
        return json;
      } else {
        JSONObject successMessage = new JSONObject();
        successMessage.put("severity", "error");
        successMessage.put("text", OBMessageUtils.messageBD("Escm_NoLines_Selected"));
        json.put("message", successMessage);
        return json;
      }
    } catch (Exception e) {
      log.error("Exception in RequisitionCancelHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      JSONObject errorMessage = new JSONObject();
      try {
        errorMessage.put("severity", "error");
        errorMessage.put("text", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
        json.put("message", errorMessage);
        return json;
      } catch (JSONException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
