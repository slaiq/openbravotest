package sa.elm.ob.scm.ad_process.IssueRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;

public class MaterialIssueReqReactivate implements Process {
  private static Logger log = Logger.getLogger(MaterialIssueReqReactivate.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    PreparedStatement ps = null;

    try {
      OBContext.setAdminMode();
      Boolean isProcessed = true;
      String matReqId = (String) bundle.getParams().get("Escm_Material_Request_ID");
      MaterialIssueRequest objRequest = OBDal.getInstance().get(MaterialIssueRequest.class,
          matReqId);
      final String clientId = (String) bundle.getContext().getClient();
      final String orgId = objRequest.getOrganization().getId();
      final String userId = (String) bundle.getContext().getUser();
      final String roleId = (String) bundle.getContext().getRole();
      Connection conn = OBDal.getInstance().getConnection();
      if (objRequest != null) {
        for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
          for (MaterialIssueRequestCustody detail : line.getEscmMrequestCustodyList()) {
            OBQuery<Escm_custody_transaction> transaction = OBDal.getInstance().createQuery(
                Escm_custody_transaction.class, " as e where e.escmMrequestCustody.id=:custodyId");
            transaction.setNamedParameter("custodyId", detail.getId());
            if (transaction.list().size() > 1) {
              isProcessed = false;
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_MIR_CantReac@");
              bundle.setResult(result);
              return;
            }

          }
        }
        for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
          for (Escm_custody_transaction transaction : line.getEscmCustodyTransactionList()) {
            if (transaction.getReturnDate() != null) {
              isProcessed = false;
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_MIR_CantReac@");
              bundle.setResult(result);
              return;
            }
          }
        }
        if (objRequest.getWarehouse() != null
            && objRequest.getWarehouse().getEscmWarehouseType().equals("RTW")) {
          if (objRequest.getEscmMaterialReqlnList().size() > 0) {
            for (MaterialIssueRequestLine lineObj : objRequest.getEscmMaterialReqlnList()) {
              for (Escm_custody_transaction cusTranObj : lineObj.getEscmCustodyTransactionList()) {
                OBQuery<Escm_custody_transaction> cusTranQry = OBDal.getInstance().createQuery(
                    Escm_custody_transaction.class,
                    " as e where e.escmMrequestCustody.id=:custodyDetailId and e.creationDate >:currentCustTranCreated ");
                cusTranQry.setNamedParameter("custodyDetailId",
                    cusTranObj.getEscmMrequestCustody().getId());
                cusTranQry.setNamedParameter("currentCustTranCreated",
                    cusTranObj.getCreationDate());
                if (cusTranQry.list().size() > 0) {
                  isProcessed = false;
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_MIR_CantReac@");
                  bundle.setResult(result);
                  return;
                }
              }
            }
          }
        }

        OBQuery<Requisition> req = OBDal.getInstance().createQuery(Requisition.class,
            " as e where e.escmMaterialRequest.id=:mirID ");
        req.setNamedParameter("mirID", objRequest.getId());
        if (req.list().size() > 0) {
          isProcessed = false;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_MIR_CantReac@");
          bundle.setResult(result);
          return;
        }

        if (isProcessed) {

          OBQuery<MaterialIssueRequestLine> matline = OBDal.getInstance().createQuery(
              MaterialIssueRequestLine.class, " as e where e.escmMaterialRequest.id=:mirID ");
          matline.setNamedParameter("mirID", objRequest.getId());

          if (matline.list().size() > 0) {
            for (MaterialIssueRequestLine line : matline.list()) {

              OBQuery<MaterialTransaction> transaction = OBDal.getInstance().createQuery(
                  MaterialTransaction.class, " as e where e.escmMaterialReqln.id=:mirLnID");
              transaction.setNamedParameter("mirLnID", line.getId());
              if (transaction.list().size() > 0) {
                MaterialTransaction tran = transaction.list().get(0);
                OBDal.getInstance().remove(tran);
                OBDal.getInstance().flush();
              }

              OBQuery<MaterialIssueRequestCustody> details = OBDal.getInstance().createQuery(
                  MaterialIssueRequestCustody.class,
                  " as e where e.escmMaterialReqln.id=:mirLnID ");
              details.setNamedParameter("mirLnID", line.getId());
              List<MaterialIssueRequestCustody> detaillist = details.list();
              log.debug("detaillist:" + detaillist.size());
              if (detaillist.size() > 0) {
                for (MaterialIssueRequestCustody det : detaillist) {
                  MaterialIssueRequestCustody deldet = OBDal.getInstance()
                      .get(MaterialIssueRequestCustody.class, det.getId());
                  OBQuery<Escm_custody_transaction> custran = OBDal.getInstance().createQuery(
                      Escm_custody_transaction.class,
                      " as e where e.escmMrequestCustody.id=:custodyId order by creationDate Desc");
                  custran.setNamedParameter("custodyId", deldet.getId());
                  custran.setMaxResult(1);
                  if (custran.list().size() > 0) {
                    Escm_custody_transaction tran = custran.list().get(0);
                    OBDal.getInstance().remove(tran);
                    OBDal.getInstance().flush();
                  }
                  deldet.setAlertStatus("N");
                  OBDal.getInstance().save(deldet);
                  OBDal.getInstance().refresh(deldet);

                  ps = conn.prepareStatement(
                      " delete from escm_mrequest_custody where escm_mrequest_custody_id= ? ");
                  ps.setString(1, deldet.getId());
                  ps.executeUpdate();

                  /*
                   * OBDal.getInstance().remove(deldet); OBDal.getInstance().flush();
                   */
                }
              }
              // delete direct transaction entries
              if (objRequest.getWarehouse().getEscmWarehouseType().equals("RTW")) {
                // delete recent entry
                OBQuery<Escm_custody_transaction> custranList = OBDal.getInstance().createQuery(
                    Escm_custody_transaction.class,
                    " as e where e.escmMaterialReqln.id=:mirLnId order by creationDate Desc");
                custranList.setNamedParameter("mirLnId", line.getId());
                if (custranList.list().size() > 0) {
                  for (Escm_custody_transaction tran : custranList.list()) {
                    tran.setProcessed(false);
                    OBDal.getInstance().save(tran);
                    // update previous entry return date as null
                    OBQuery<Escm_custody_transaction> custranPrevList = OBDal.getInstance()
                        .createQuery(Escm_custody_transaction.class,
                            " as e where e.escmMrequestCustody.id=:custodyId  and e.id<>:transId "
                                + " order by creationDate Desc");
                    custranPrevList.setNamedParameter("custodyId",
                        tran.getEscmMrequestCustody().getId());
                    custranPrevList.setNamedParameter("transId", tran.getId());

                    custranPrevList.setMaxResult(1);
                    log.debug("custranPrevList:" + custranPrevList.list().size());
                    if (custranPrevList.list().size() > 0) {
                      Escm_custody_transaction prevTran = custranPrevList.list().get(0);
                      prevTran.setReturnDate(null);
                      OBDal.getInstance().save(prevTran);
                    }
                  }

                }

                OBDal.getInstance().flush();
              }
            }
          }
          if (objRequest.getWarehouse().getEscmWarehouseType().equals("RTW")) {
            for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
              for (Escm_custody_transaction tr : line.getEscmCustodyTransactionList()) {
                tr.getEscmMrequestCustody().setAlertStatus("RET");
                tr.getEscmMrequestCustody().setSalesDesc(null);
                tr.setProcessed(false);
                OBDal.getInstance().save(tr);

                OBQuery<Escm_custody_transaction> custransa = OBDal.getInstance().createQuery(
                    Escm_custody_transaction.class,
                    " as e where e.escmMrequestCustody.id=:custodyID "
                        + " and e.isProcessed = 'Y' order by e.creationDate desc ");
                custransa.setNamedParameter("custodyID", tr.getEscmMrequestCustody().getId());
                custransa.setMaxResult(1);
                if (custransa.list().size() > 0) {
                  Escm_custody_transaction custransaction = custransa.list().get(0);
                  custransaction.setReturnDate(null);
                  // custransaction.getEscmMrequestCustody()
                  // .setBeneficiaryIDName(custransaction.getBname());
                  // custransaction.getEscmMrequestCustody()
                  // .setBeneficiaryType(custransaction.getBtype());
                  custransaction.getEscmMrequestCustody().setBeneficiaryIDName(null);
                  custransaction.getEscmMrequestCustody().setBeneficiaryType(null);
                  OBDal.getInstance().save(custransaction);
                  OBDal.getInstance().flush();
                }
              }
            }

          }
          if (objRequest.isSiteissuereq()) {

            for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
              EscmInitialReceipt initial = line.getEscmInitialreceipt();
              initial.setSitereqissuedqty(
                  initial.getSitereqissuedqty().subtract(line.getDeliveredQantity()));
              OBDal.getInstance().save(initial);
              OBDal.getInstance().flush();
            }
          }
          objRequest.setUpdated(new java.util.Date());
          objRequest.setUpdatedBy(OBContext.getOBContext().getUser());
          objRequest.setAlertStatus("DR");
          objRequest.setEscmAction("CO");
          // set reactivated user
          objRequest.setReactivatedby(OBContext.getOBContext().getUser());
          objRequest.setEUTNextRole(null);
          if (objRequest.isSiteissuereq())
            objRequest.setEscmSmirAction("CO");
          OBDal.getInstance().save(objRequest);

          if (!StringUtils.isEmpty(objRequest.getId())) {
            JSONObject historyData = new JSONObject();

            historyData.put("ClientId", clientId);
            historyData.put("OrgId", orgId);
            historyData.put("RoleId", roleId);
            historyData.put("UserId", userId);
            historyData.put("HeaderId", objRequest.getId());
            historyData.put("Comments", "");
            historyData.put("Status", "REA");
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.ISSUE_REQUEST_HISTORY);
            historyData.put("HeaderColumn", ApprovalTables.ISSUE_REQUEST_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.ISSUE_REQUEST_DOCACTION_COLUMN);

            Utility.InsertApprovalHistory(historyData);

          }
          OBError result = OBErrorBuilder.buildMessage(null, "success", "@ESCM_MIR_Rea_Success@");
          bundle.setResult(result);
          return;
        }
      }
    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in MaterialIssueReqReactivate:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      try {
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        log.error("Exception while closing the statement in MaterialIssueReqReactivate ", e);
      }
      OBContext.restorePreviousMode();
    }
  }
}