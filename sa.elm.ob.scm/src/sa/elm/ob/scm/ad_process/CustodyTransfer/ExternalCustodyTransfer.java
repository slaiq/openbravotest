package sa.elm.ob.scm.ad_process.CustodyTransfer;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gopalakrishnan on 09/06/2017
 */

public class ExternalCustodyTransfer extends DalBaseProcess {

  /**
   * This servlet class was responsible for External Custody Transfer Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(ExternalCustodyTransfer.class);
  private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    ResultSet rs = null;
    PreparedStatement st = null;
    boolean errorFlag = false;
    String lastproductid = "";

    log.debug("entering into External CustodyTransfer Submit");
    try {
      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String errorMsge = "";
      String query = null;
      // Connection con = OBDal.getInstance().getConnection();
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = inout.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      String pendingapproval = "", appstatus = "";
      String DocAction = inout.getEscmCtdocaction();
      String alertWindowType = AlertWindow.CustodyTransfer, alertRuleId = "";
      String Lang = vars.getLanguage();
      String status = null, product = null;
      String InvCtrl_Role = "";
      User usr = OBDal.getInstance().get(User.class, vars.getUser());

      // check lines to submit
      if (inout.getMaterialMgmtShipmentInOutLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTranAddLines@");
        bundle.setResult(result);
        return;
      }

      // check all custody line transaction belongs to current beneficiary
      if (inout.getMaterialMgmtShipmentInOutLineList().size() > 0) {
        for (ShipmentInOutLine objLine : inout.getMaterialMgmtShipmentInOutLineList()) {
          if (objLine.getMovementQuantity().compareTo(BigDecimal.ZERO) == 0) {
            errorFlag = true;
            errorMsge = OBMessageUtils.messageBD("ESCM_ZERO_CUSTODY");
          }
          if (objLine.getEscmCustodyTransactionList().size() == 0) {
            errorFlag = true;
            errorMsge = OBMessageUtils.messageBD("ESCM_ZERO_CUSTODY");
          }
          for (Escm_custody_transaction objTransaction : objLine.getEscmCustodyTransactionList()) {
            if (objTransaction.getEscmMrequestCustody().getBeneficiaryIDName() != inout
                .getEscmBname()) {
              errorFlag = true;
              objTransaction
                  .setErrorreason("Not Belongs to " + inout.getEscmBname().getCommercialName());
              OBDal.getInstance().save(objTransaction);
              OBDal.getInstance().flush();
              errorMsge = OBMessageUtils.messageBD("ESCM_Not_Beneficiary").replace("@",
                  inout.getEscmBname().getCommercialName());
            }
          }
        }
      }
      if (!errorFlag) {
        for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
          for (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) {
            status = null;
            OBQuery<Escm_custody_transaction> transaction = OBDal.getInstance().createQuery(
                Escm_custody_transaction.class,
                " as e where e.goodsShipmentLine.id in ( select line.id  from MaterialMgmtShipmentInOutLine line "
                    + "  left join line.shipmentReceipt hd  where (hd.escmReceivingtype='INR'  or hd.escmIscustodyTransfer='Y') "
                    + " and hd.escmDocstatus='ESCM_IP' and hd.id <>:inoutID ) and e.escmMrequestCustody.id=:custodyID ");
            transaction.setNamedParameter("inoutID", inout.getId());
            transaction.setNamedParameter("custodyID", tran.getEscmMrequestCustody().getId());

            log.debug("wherecl:" + transaction.getWhereAndOrderBy());
            log.debug("size:" + transaction.list().size());
            if (transaction.list().size() > 0) {
              errorFlag = true;
              for (Escm_custody_transaction trans : transaction.list()) {
                if (status == null) {
                  status = OBMessageUtils.messageBD("ESCM_CT_TagAlreadyUsed");
                  status = status.replace("%",
                      trans.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo());
                  if (product == null) {
                    lastproductid = trans.getGoodsShipmentLine().getProduct().getId();
                    product = trans.getGoodsShipmentLine().getProduct().getName();
                  } else {
                    if (!lastproductid.equals(trans.getGoodsShipmentLine().getProduct().getId())) {
                      lastproductid = trans.getGoodsShipmentLine().getProduct().getId();
                      product = product + "," + trans.getGoodsShipmentLine().getProduct().getName();
                    }
                  }
                } else {
                  status += trans.getGoodsShipmentLine().getShipmentReceipt().getDocumentNo();
                }
              }

            }
            tran.setErrorreason(status);
            OBDal.getInstance().save(line);
          }
        }
        if (errorFlag) {
          status = OBMessageUtils.messageBD("ESCM_ProcessFailed(Tag Status)");
          status = status.replace("%", product);
          // status += product;
          errorMsge = status;
        }
      }
      if (!errorFlag) {
        boolean sequenceexists = false;
        // final approval process
        if (DocAction.equals("CO")) {
          if (inout.getEscmSpecno() == null) {
            String sequence = Utility.getSpecificationSequence(inout.getOrganization().getId(),
                "CT");
            if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
              OBDal.getInstance().rollbackAndClose();
              errorFlag = true;
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_NoSpecSequence@");
              bundle.setResult(result);
              return;
            } else {
              sequenceexists = Utility.chkSpecificationSequence(inout.getOrganization().getId(),
                  "CT", sequence);
              if (!sequenceexists) {
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Escm_Duplicate_SpecNo@");
                bundle.setResult(result);
                return;
              }
              inout.setEscmSpecno(sequence);
            }
          }

          for (ShipmentInOutLine inoutline : inout.getMaterialMgmtShipmentInOutLineList()) {
            // update custody trnasaction and custody detail based on inoutline

            for (Escm_custody_transaction objCustodytran : inoutline
                .getEscmCustodyTransactionList()) {
              // update custody detail status
              MaterialIssueRequestCustody objCustody = objCustodytran.getEscmMrequestCustody();
              objCustody.setAlertStatus("IU");
              objCustody.setBeneficiaryType(inout.getEscmTobeneficiary());
              objCustody.setBeneficiaryIDName(inout.getEscmTobenefiName());
              OBDal.getInstance().save(objCustody);
              objCustodytran.setTransactiontype("TR");
              objCustodytran.setTransactionDate(inout.getMovementDate());
              query = " select escm_custody_transaction_id from escm_custody_transaction where "
                  + "  escm_custody_transaction_id not in (?) and escm_mrequest_custody_id =? "
                  + " and isprocessed='Y' order by created desc limit 1";
              st = conn.prepareStatement(query);
              st.setString(1, objCustodytran.getId());
              st.setString(2, objCustody.getId());
              rs = st.executeQuery();
              if (rs.next()) {
                Escm_custody_transaction updCustodytran = OBDal.getInstance().get(
                    Escm_custody_transaction.class, rs.getString("escm_custody_transaction_id"));
                updCustodytran.setReturnDate(inout.getMovementDate());
                OBDal.getInstance().save(updCustodytran);
              }
              objCustodytran.setProcessed(true);
              objCustodytran.setBname(inout.getEscmTobenefiName());
              objCustodytran.setBtype(inout.getEscmTobeneficiary());
              objCustody.setBeneficiaryIDName(inout.getEscmTobenefiName());
              objCustody.setBeneficiaryType(inout.getEscmTobeneficiary());
              objCustodytran.setDocumentNo(inout.getEscmSpecno());
              OBDal.getInstance().save(objCustodytran);
            }
          }
          inout.setUpdated(new java.util.Date());
          inout.setUpdatedBy(OBContext.getOBContext().getUser());
          inout.setEscmDocstatus("CO");
          inout.setDocumentStatus("CO");
          inout.setDocumentAction("--");
          inout.setEscmCtdocaction("PD");
          inout.setEutNextRole(null);
          OBDal.getInstance().save(inout);
          OBDal.getInstance().flush();
          appstatus = "AP";

          // alert process
          ArrayList<CustTransferAlertVO> includereceipient = new ArrayList<CustTransferAlertVO>();
          CustTransferAlertVO vo = null;
          // delete alert for approval alerts
          OBQuery<Alert> alertnew = OBDal.getInstance().createQuery(Alert.class,
              " as e  where e.referenceSearchKey=:inoutID and e.alertStatus='NEW'");
          alertnew.setNamedParameter("inoutID", inout.getId());
          log.debug("getWhereAndOrderBy:" + alertnew.getWhereAndOrderBy());
          log.debug("alertnew:" + alertnew.list().size());
          // set all the previoust new status as solved
          if (alertnew.list().size() > 0) {
            for (Alert alert : alertnew.list()) {
              alert.setAlertStatus("SOLVED");
            }
          }

          // getting alert receipient
          OBQuery<AlertRule> alertrule = OBDal.getInstance().createQuery(AlertRule.class,
              " as e where e.client.id=:clientID and e.eSCMProcessType=:processType ");
          alertrule.setNamedParameter("clientID", clientId);
          alertrule.setNamedParameter("processType", alertWindowType);
          if (alertrule.list().size() > 0) {
            alertRuleId = alertrule.list().get(0).getId();
          }
          OBQuery<AlertRecipient> alertrec = OBDal.getInstance().createQuery(AlertRecipient.class,
              " as e where e.alertRule.id=:alertRuleID ");
          alertrec.setNamedParameter("alertRuleID", alertRuleId);
          if (alertrec.list().size() > 0) {
            for (AlertRecipient rec : alertrec.list()) {

              if (rec.getUserContact() != null) {
                vo = new CustTransferAlertVO(rec.getRole().getId(), rec.getUserContact().getId());
              } else {
                vo = new CustTransferAlertVO(rec.getRole().getId(), "0");
              }
              includereceipient.add(vo);
              OBDal.getInstance().remove(rec);
            }
          }
          Role objCreatedRole = null;
          if (inout.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = inout.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          if (objCreatedRole != null) {
            vo = new CustTransferAlertVO(objCreatedRole.getId(), inout.getCreatedBy().getId());
            includereceipient.add(vo);
          }
          // final approved need to send alert to inventory control
          OBQuery<Preference> invRole = OBDal.getInstance().createQuery(Preference.class,
              "as e where e.property='ESCM_Inventory_Control' and e.searchKey='Y' and e.client.id=:clientID ");
          invRole.setNamedParameter("clientID", clientId);
          if (invRole.list().size() > 0) {
            InvCtrl_Role = invRole.list().get(0).getVisibleAtRole().getId();
            vo = new CustTransferAlertVO(invRole.list().get(0).getVisibleAtRole().getId(), "0");
            includereceipient.add(vo);
          }

          // avoid duplicate recipient
          Set<CustTransferAlertVO> s = new HashSet<CustTransferAlertVO>();
          s.addAll(includereceipient);
          includereceipient = new ArrayList<CustTransferAlertVO>();
          includereceipient.addAll(s);

          // insert alert receipients
          for (CustTransferAlertVO vo1 : includereceipient) {
            if (vo1.getUserId().equals("0")) {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), null, clientId, alertWindowType);
            }

            else {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                  alertWindowType);
            }
          }
          // set alert for requester
          String Description = sa.elm.ob.scm.properties.Resource.getProperty("scm.ct.approved",
              Lang) + " " + usr.getName();
          AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(),
              inout.getEscmAdRole().getId(), inout.getCreatedBy().getId(),
              inout.getClient().getId(), Description, "NEW", alertWindowType, "scm.ct.approved",
              Constants.GENERIC_TEMPLATE);
          // set alert for inventory control
          AlertUtility.alertInsertionRole(inout.getId(), inout.getDocumentNo(), InvCtrl_Role, "",
              inout.getClient().getId(), Description, "NEW", alertWindowType, "scm.ct.approved",
              Constants.GENERIC_TEMPLATE);
        }

        if (!StringUtils.isEmpty(inout.getId())) {
          JSONObject historyData = new JSONObject();

          historyData.put("ClientId", clientId);
          historyData.put("OrgId", orgId);
          historyData.put("RoleId", roleId);
          historyData.put("UserId", userId);
          historyData.put("HeaderId", inout.getId());
          historyData.put("Comments", comments);
          historyData.put("Status", appstatus);
          historyData.put("NextApprover", pendingapproval);
          historyData.put("HistoryTable", ApprovalTables.CUSTODYTRANSFER_HISTORY);
          historyData.put("HeaderColumn", ApprovalTables.CUSTODYTRANSFER_HEADER_COLUMN);
          historyData.put("ActionColumn", ApprovalTables.CUSTODYTRANSFER_DOCACTION_COLUMN);

          Utility.InsertApprovalHistory(historyData);

        }
      }
      if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("Process Failed:" + errorMsge);
      } else {
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      }
      bundle.setResult(obError);
      OBDal.getInstance().save(inout);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in External Custody Transfer Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }

  }
}
