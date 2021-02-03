package sa.elm.ob.finance.ad_process.RDVProcess;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.dms.service.DMSRDVService;
import sa.elm.ob.finance.dms.serviceimplementation.DMSRDVServiceImpl;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Priyanka Ranjan on 28th Feb 2019
 */

public class RDVReactivateProcess implements Process {

  private static final Logger log = Logger.getLogger(RDVReactivateProcess.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode();
      String rdvTxnId = (String) bundle.getParams().get("Efin_Rdvtxn_ID");
      EfinRDVTransaction transaction = OBDal.getInstance().get(EfinRDVTransaction.class, rdvTxnId);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = transaction.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      String comments = (String) bundle.getParams().get("comments").toString();
      boolean checkEncumbranceAmountZero = false;
      List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
      boolean errorFlag = false;
      String VersionList = "";

      // chk already reactivated or not and check amarsaraf already created
      if (transaction.getAppstatus().equals("DR") || (transaction.getTxnverStatus().equals("INV")
          && transaction.isAmarsaraf() && transaction.getAppstatus().equals("APP"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_AlreadyPreocessed_Approve@");
        bundle.setResult(result);
        return;
      }

      // Should not allow to reactivate rdv versions, if release happened in next versions

      List<EfinRdvHoldAction> holdActionList = new ArrayList<EfinRdvHoldAction>();
      transaction.getEfinRDVTxnlineList().forEach(a -> {
        a.getEfinRdvHoldActionList().forEach(b -> {
          b.getEfinRdvHoldActionRDVHoldRelIDList().forEach(c -> {
            if (c.getEfinRdvtxnline().getEfinRdvtxn().getTXNVersion() != transaction
                .getTXNVersion()) {
              holdActionList.add(c);
            }
          });
        });
      });

      List<EfinPenaltyAction> penaltyActionList = new ArrayList<EfinPenaltyAction>();
      transaction.getEfinRDVTxnlineList().forEach(a -> {
        a.getEfinPenaltyActionList().forEach(b -> {
          b.getEfinPenaltyActionPenaltyRelIDList().forEach(c -> {
            if (c.getEfinRdvtxnline().getEfinRdvtxn().getTXNVersion() != transaction
                .getTXNVersion()) {
              penaltyActionList.add(c);
            }
          });
        });
      });

      // List<EfinRdvHoldAction> holdActionList = new ArrayList<EfinRdvHoldAction>();
      // transaction.getEfinRDVTxnlineList().forEach(a -> {
      // a.getEfinRdvHoldActionList().forEach(b -> {
      // if (b.getEfinRdvHoldActionRDVHoldRelIDList().size() > 0 ) {
      // holdActionList.addAll(b.getEfinRdvHoldActionRDVHoldRelIDList());
      // }
      // });
      // });
      // List<EfinPenaltyAction> penaltyActionList = new ArrayList<EfinPenaltyAction>();
      // transaction.getEfinRDVTxnlineList().forEach(a -> {
      // a.getEfinPenaltyActionList().forEach(b -> {
      // if (b.getEfinPenaltyActionPenaltyRelIDList().size() > 0) {
      // penaltyActionList.addAll(b.getEfinPenaltyActionPenaltyRelIDList());
      // }
      // });
      // });

      if (holdActionList.size() > 0) {
        List<Long> versionNo = new ArrayList<>();
        for (EfinRdvHoldAction hldAction : holdActionList) {
          Long verNo = hldAction.getEfinRdvtxnline().getEfinRdvtxn().getTXNVersion();
          if (!versionNo.contains(verNo)) {
            VersionList = VersionList + ", " + verNo.toString();
            versionNo.add(verNo);
          }
        }
        VersionList = VersionList.replaceFirst(",", "");
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_ReactivateNotAllowedHld").replace("%", VersionList));
        bundle.setResult(result);
        OBDal.getInstance().rollbackAndClose();
        return;
      }
      if (penaltyActionList.size() > 0) {
        List<Long> versionNo = new ArrayList<>();
        for (EfinPenaltyAction pltyAction : penaltyActionList) {
          Long verNo = pltyAction.getEfinRdvtxnline().getEfinRdvtxn().getTXNVersion();
          if (!versionNo.contains(verNo)) {
            VersionList = VersionList + ", " + verNo.toString();
            versionNo.add(verNo);
          }
        }
        VersionList = VersionList.replaceFirst(",", "");
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            OBMessageUtils.messageBD("Efin_ReactivateNotAllowedPen").replace("%", VersionList));
        bundle.setResult(result);
        OBDal.getInstance().rollbackAndClose();
        return;
      }

      /*
       * OBQuery<Order> ord = OBDal.getInstance().createQuery(Order.class,
       * " as e where e.escmOldOrder=:OrderId"); ord.setNamedParameter("OrderId",
       * transaction.getEfinRdv().getSalesOrder());
       * 
       * if (ord.list().size() > 0) { OBError result = OBErrorBuilder.buildMessage(null, "error",
       * "@Efin_RDVCannotReactivate@"); bundle.setResult(result); return; }
       */

      // because of multiple draft version below condition is removed.

      // if new version is created and trying to reactivate previous version then should not allow
      // to reactivate
      /*
       * long version = transaction.getTXNVersion(); OBQuery<EfinRDVTransaction> trxVerListQry =
       * OBDal.getInstance().createQuery( EfinRDVTransaction.class,
       * " as e where e.efinRdv.id=:rdvId order by e.tXNVersion desc ");
       * 
       * trxVerListQry.setNamedParameter("rdvId", transaction.getEfinRdv().getId());
       * trxVerListQry.setMaxResult(1); if (trxVerListQry.list().size() > 0) { long maxVersion =
       * trxVerListQry.list().get(0).getTXNVersion(); if (maxVersion > version) {
       * OBDal.getInstance().rollbackAndClose(); OBError result = OBErrorBuilder.buildMessage(null,
       * "error", "@Efin_CannotReactivate@"); bundle.setResult(result); return;
       * 
       * } }
       */

      // change the status
      if (transaction.getAppstatus().equals("APP")) {
        if (!errorFlag) {
          transaction.setUpdated(new java.util.Date());
          transaction.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          transaction.setAppstatus("DR");
          transaction.setAction("CO");
          transaction.setTxnverStatus("DR");
          transaction.setNextRole(null);
          // transaction.setLastversion(false);
          transaction.setSubmitroleid(null);
          if (transaction.isContractcategoryRolePassed()) {
            transaction.setContractcategoryRolePassed(false);
          }
          OBDal.getInstance().save(transaction);

          if (transaction.isAdvancetransaction()
              && transaction.getEfinRDVTxnlineList().size() > 0) {
            EfinRDVTxnline txnLineObj = transaction.getEfinRDVTxnlineList().get(0);
            txnLineObj.setAction("CO");
            txnLineObj.setApprovalStatus("DR");
            txnLineObj.setTxnverStatus("DR");
            OBDal.getInstance().save(txnLineObj);
          }

          // insert approval history
          if (!StringUtils.isEmpty(transaction.getId())) {
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", transaction.getId());
            historyData.put("Comments", comments);
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.RDV_Txn_History);
            historyData.put("HeaderColumn", ApprovalTables.RDV_Txn_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.RDV_Txn_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);

          }

          // Solve approval alerts
          OBQuery<Alert> alertObjApp = OBDal.getInstance().createQuery(Alert.class,
              " as e where e.referenceSearchKey =:referenceSearchKey  and e.alertStatus = 'NEW' and e.eutAlertKey = 'finance.rdv.approved' ");
          alertObjApp.setNamedParameter("referenceSearchKey", transaction.getId());
          List<Alert> alertListApp = alertObjApp.list();
          if (alertListApp.size() > 0) {
            for (Alert objAlert : alertListApp) {
              objAlert.setAlertStatus("SOLVED");
              OBDal.getInstance().save(objAlert);
            }
          }

          // remove RDV hold alert
          if (!transaction.getEfinRdv().getTXNType().equals("POD")
              && transaction.getHoldamount().compareTo(BigDecimal.ZERO) > 0) {
            OBQuery<org.openbravo.model.ad.alert.Alert> alertObj = OBDal.getInstance().createQuery(
                org.openbravo.model.ad.alert.Alert.class,
                " as e where e.referenceSearchKey =:referenceSearchKey  and e.alertStatus = 'NEW' and e.eutAlertKey = 'finance.hold.amount' ");
            alertObj.setNamedParameter("referenceSearchKey", transaction.getId());
            List<org.openbravo.model.ad.alert.Alert> alertList = alertObj.list();
            if (alertList.size() > 0) {
              for (org.openbravo.model.ad.alert.Alert itr : alertList) {
                org.openbravo.model.ad.alert.Alert alert = OBDal.getInstance()
                    .get(org.openbravo.model.ad.alert.Alert.class, itr.getId());
                OBDal.getInstance().remove(alert);
              }
            }
          }

          // Check Encumbrance Amount is Zero Or Negative
          if (transaction.getEfinRdv() != null) {
            if (transaction.getEfinRdv().getManualEncumbrance() != null) {
              encumLinelist = transaction.getEfinRdv().getManualEncumbrance()
                  .getEfinBudgetManencumlinesList();
            }
          }
          if (encumLinelist.size() > 0)
            checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

          if (checkEncumbranceAmountZero) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
            bundle.setResult(result);
            return;
          }

          try {
            // DMS integration
            DMSRDVService dmsService = new DMSRDVServiceImpl();
            dmsService.rejectAndReactivateOperations(transaction);
          } catch (Exception e) {
            log.error("Error while deleting the record in dms revoke" + e.getMessage());
          }

          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        }
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in RDVReactivate:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBDal.getInstance().flush();
      OBContext.restorePreviousMode();
    }

  }

}
