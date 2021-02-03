package sa.elm.ob.finance.ad_forms.ApprovalRevoke.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.OrderLine;

import sa.elm.ob.finance.actionHandler.InvoiceRevokeDAO;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.dms.service.DMSInvoiceService;
import sa.elm.ob.finance.dms.serviceimplementation.DMSInvoiceServiceImpl;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ApprovalRevokeVO;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.MassRevoke;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gowtham
 *
 */
public class InvoiceMassRevokeDAO extends MassRevoke {
  private static Logger LOG = Logger.getLogger(InvoiceMassRevokeDAO.class);

  String fromClause = "  select main.id, main.org, main.requester,main.documentno,main.lastaction,main.em_eut_next_role_id,main.DocStatus from "
      + "(select org.name as org,inv.documentno as documentno,inv.DocStatus,inv.c_invoice_id as id,rr.name as requester, "
      + " (select (coalesce(trl.name,st.name)||' - '|| ur.name) from efin_purchasein_app_hist history, ad_user ur, ad_ref_list st "
      + " left join ad_ref_list_trl trl on trl.ad_ref_list_id=st.ad_ref_list_id and trl.ad_language=(select lang.ad_language "
      + " from ad_language lang  where lang.ad_language_id='112') where history.c_invoice_id = inv.c_invoice_id  and st.value = history.purchaseaction "
      + "  and st.ad_reference_id='9F2DC8F55FE9442895FCD3ED468B1D50' and ur.ad_user_id = history.createdby order by history.updated desc limit 1) as lastaction, "
      + "  pen.pending, inv.em_eut_next_role_id " + "  from c_invoice inv "
      + "           left join ad_user rr on rr.ad_user_id=inv.createdby "
      + "           left join ad_org org on org.ad_org_id=inv.ad_org_id  "
      + "           left join (select array_to_string(array_agg(role.name),' / ') as pending, lin.eut_next_role_id  from eut_next_role rl "
      + "           join eut_next_role_line lin on lin.eut_next_role_id=rl.eut_next_role_id  "
      + "           join ad_role role on role.ad_role_id=lin.ad_role_id  group by lin.eut_next_role_id ) as pen on pen.eut_next_role_id=inv.em_eut_next_role_id ";

  @SuppressWarnings("unchecked")
  public int getRevokeRecordsCount(VariablesSecureApp vars, String clientId, String windowId,
      String searchFlag, ApprovalRevokeVO vo) {
    int totalRecord = 0;
    String sqlQuery = "", whereClause = "", documentType = "";

    try {

      whereClause = " where inv.ad_client_id = '" + clientId + "'";

      whereClause += " and inv.DocStatus='EFIN_WFA' and inv.em_eut_next_role_id is not null "
          + " and inv.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
          + " left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id "
          + " left join ad_role role on role.ad_role_id = usrrole.ad_role_id "
          + " left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id "
          + " left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
          + vars.getUser() + "')";

      // get document type for invoice
      documentType = getDocumentType(windowId, clientId);
      if (StringUtils.isNotEmpty(documentType)) {
        whereClause += " and inv.C_DocTypeTarget_ID in( " + documentType + ")";
      }

      if (searchFlag.equals("true")) {
        if (vo.getOrgName() != null)
          whereClause += " and org.name ilike '%" + vo.getOrgName() + "%'";
        if (vo.getRequester() != null)
          whereClause += " and rr.name ilike '%" + vo.getRequester() + "%'";
        if (vo.getDocno() != null)
          whereClause += " and inv.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and inv.DocStatus='EFIN_WFA' ";
          } else {
            whereClause += " and 1=2";
          }
        }
      }

      sqlQuery += fromClause + whereClause + " ) main ";
      if (searchFlag.equals("true")) {
        if (vo.getLastperfomer() != null)
          sqlQuery += " where  main.lastaction ilike '%" + vo.getLastperfomer() + "%'";
      }

      SQLQuery queryList = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      LOG.debug("sqlQuery" + sqlQuery);
      if (queryList != null) {
        List<Object> rows = queryList.list();
        if (rows.size() > 0) {
          totalRecord = rows.size();
        }
      }

    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of Invoice for mass revoke", e);
    }
    return totalRecord;
  }

  @SuppressWarnings("rawtypes")
  public List<ApprovalRevokeVO> getRevokeRecordsList(VariablesSecureApp vars, String clientId,
      String windowId, ApprovalRevokeVO vo, int limit, int offset, String sortColName,
      String sortColType, String searchFlag, String lang) {
    LOG.debug("sort" + sortColType);
    List<ApprovalRevokeVO> ls = new ArrayList<ApprovalRevokeVO>();

    String sqlQuery = "", whereClause = "", orderClause = "", documentType = "";
    OBContext.setAdminMode();

    try {

      whereClause = " where inv.ad_client_id = '" + clientId + "'";

      whereClause += " and inv.DocStatus='EFIN_WFA' and inv.em_eut_next_role_id is not null "
          + " and inv.ad_org_id  in (  select org.ad_org_id from ad_user rr  "
          + " left join ad_user_roles usrrole on usrrole.ad_user_id = rr.ad_user_id "
          + " left join ad_role role on role.ad_role_id = usrrole.ad_role_id "
          + " left join ad_role_orgaccess orgrole on orgrole.ad_role_id= role.ad_role_id "
          + " left join ad_org org on org.ad_org_id= orgrole.ad_org_id where rr.ad_user_id='"
          + vars.getUser() + "')";

      // get document type for invoice
      documentType = getDocumentType(windowId, clientId);
      if (StringUtils.isNotEmpty(documentType)) {
        whereClause += " and inv.C_DocTypeTarget_ID in ( " + documentType + ")";
      }
      if (searchFlag.equals("true")) {
        if (vo.getOrgName() != null)
          whereClause += " and org.name ilike '%" + vo.getOrgName() + "%'";
        if (vo.getRequester() != null)
          whereClause += " and rr.name ilike '%" + vo.getRequester() + "%'";
        if (vo.getDocno() != null)
          whereClause += " and inv.documentno ilike '%" + vo.getDocno() + "%'";
        if (vo.getNextrole() != null)
          whereClause += " and pen.pending ilike '%" + vo.getNextrole() + "%'";
        if (vo.getStatus() != null) {
          if (("in progress").contains(vo.getStatus().toLowerCase()) || vo.getStatus().isEmpty()) {
            whereClause += " and inv.DocStatus='EFIN_WFA' ";
          } else {
            whereClause += " and 1=2";
          }
        }
      }

      if (sortColName != null && sortColName.equals("org"))
        orderClause += " order by org.name  " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("docno")) {
        orderClause += " order by inv.documentno " + sortColType + " limit " + limit + " offset "
            + offset;
      } else if (sortColName != null && sortColName.equals("requester"))
        orderClause += " order by rr.name " + sortColType + " limit " + limit + " offset " + offset;
      else if (sortColName != null && sortColName.equals("nextrole"))
        orderClause += " order by pen.pending " + sortColType + " limit " + limit + " offset "
            + offset;
      else if (sortColName != null && sortColName.equals("status")) {
        orderClause += " order by inv.DocStatus='EFIN_WFA' " + sortColType + " limit " + limit
            + " offset " + offset;
      } else {
        orderClause += " order by inv.documentno desc" + " limit " + limit + " offset " + offset;
      }

      sqlQuery = fromClause + whereClause + orderClause + ") main ";
      if (searchFlag.equals("true")) {
        if (vo.getLastperfomer() != null)
          sqlQuery += " where  main.lastaction ilike '%" + vo.getLastperfomer() + "%'";
      }
      if (sortColName != null && sortColName.equals("lastperformer"))
        sqlQuery += " order by main.lastaction  " + sortColType + " limit " + limit + " offset "
            + offset;
      LOG.debug("Inv mass revoke sqlQuery : " + sqlQuery);

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();

          // while (rs.next()) {
          String nextRole = "";
          ApprovalRevokeVO apVO = new ApprovalRevokeVO();
          apVO.setRecordId(Utility.nullToEmpty(row[0].toString()));
          apVO.setOrgName(Utility.nullToEmpty(row[1].toString()));
          apVO.setRequester(Utility.nullToEmpty(row[2].toString()));
          apVO.setDocno(Utility.nullToEmpty(row[3].toString()));
          apVO.setLastperfomer(Utility.nullToEmpty(row[4].toString()));

          EutNextRole objNxtRole = OBDal.getInstance().get(EutNextRole.class,
              Utility.nullToEmpty(row[5].toString()));
          if (objNxtRole != null && objNxtRole.getEutNextRoleLineList().size() > 0) {
            if (objNxtRole.getEutNextRoleLineList().size() == 1) {
              OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                  "role.id='" + objNxtRole.getEutNextRoleLineList().get(0).getRole().getId() + "'");
              if (ur != null && ur.list().size() > 0) {
                if (ur.list().size() == 1) {
                  if (StringUtils.isEmpty(nextRole))
                    nextRole += (ur.list().get(0).getUserContact().getName());
                  else
                    nextRole += (" / " + ur.list().get(0).getUserContact().getName());
                } else {
                  if (StringUtils.isEmpty(nextRole))
                    nextRole += (ur.list().get(0).getRole().getName());
                  else
                    nextRole += (" / " + ur.list().get(0).getRole().getName());
                }

              }
            } else {
              for (EutNextRoleLine objLine : objNxtRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objLine.getRole().getId() + "'");
                if (ur != null && ur.list().size() > 0) {
                  if (ur.list().size() == 1) {
                    if (StringUtils.isEmpty(nextRole))
                      nextRole += (ur.list().get(0).getUserContact().getName());
                    else
                      nextRole += (" / " + ur.list().get(0).getUserContact().getName());
                  } else {
                    if (StringUtils.isEmpty(nextRole))
                      nextRole += (ur.list().get(0).getRole().getName());
                    else
                      nextRole += (" / " + ur.list().get(0).getRole().getName());
                  }

                }

              }
            }
          }
          apVO.setNextrole(nextRole);
          if (Utility.nullToEmpty(row[6].toString()).equals("EFIN_WFA")) {
            apVO.setStatus(Resource.getProperty("utility.inprogress", lang));
          } else {
            apVO.setStatus("");
          }

          ls.add(apVO);
        }
      }

    } catch (final Exception e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Exception While Getting Records of Invoice for mass revoke", e);
    }
    return ls;
  }

  @Override
  public String updateRecord(VariablesSecureApp var, String selectIds, String inpWindowId) {
    String PO_DOCUMENT = "POM";
    String Result = "Success";
    String alertWindow = "", alertRuleId = "", comments = "", Description = "", strInvoiceType = "";
    ArrayList<String> includeRecipient = new ArrayList<String>();
    int count = 0;
    JSONObject historyData = new JSONObject();
    EutNextRole nextRole = null;
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

    try {
      List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
      for (int i = 0; i < result.size(); i++) {
        Invoice header = OBDal.getInstance().get(Invoice.class, result.get(i));
        nextRole = OBDal.getInstance().get(EutNextRole.class, header.getEutNextRole().getId());
        strInvoiceType = InvoiceRevokeDAO.getInvoiceType(header);

        // update header status
        header.setUpdated(new java.util.Date());
        header.setUpdatedBy(OBContext.getOBContext().getUser());
        header.setDocumentAction("CO");
        header.setEfinDocaction("CO");
        header.setDocumentStatus("DR");
        header.setEutNextRole(null);
        OBDal.getInstance().save(header);
        String invType = PurchaseInvoiceSubmitUtils.getInvoiceType(header);
        // // reduce qty in order for POM.
        if (PO_DOCUMENT.equals(invType)) {
          OBInterceptor.setPreventUpdateInfoChange(true);

          if (header.getEfinCOrder().getEscmReceivetype().equals("AMT")) {
            for (InvoiceLine invLine : header.getInvoiceLineList()) {
              if (invLine.isEfinIspom()) {
                OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                    invLine.getSalesOrderLine().getId());
                ordLine.setEfinAmtinvoiced(
                    ordLine.getEfinAmtinvoiced().subtract(invLine.getEfinAmtinvoiced()));
                OBDal.getInstance().save(ordLine);
              }
            }
          } else {
            for (InvoiceLine invLine : header.getInvoiceLineList()) {
              if (invLine.isEfinIspom() && !invLine.isEFINIsTaxLine()) {
                OrderLine ordLine = OBDal.getInstance().get(OrderLine.class,
                    invLine.getSalesOrderLine().getId());
                ordLine.setInvoicedQuantity(
                    ordLine.getInvoicedQuantity().subtract(invLine.getInvoicedQuantity()));
                OBDal.getInstance().save(ordLine);
              }
            }
          }
        }

        OBInterceptor.setPreventUpdateInfoChange(false);
        OBDal.getInstance().flush();

        // if invoice crossed reserved role then need to revert the encumbrance also.
        if (header.isEfinIsreserved()) {

          InvoiceRevokeDAO.revertReservedInvoice(header);
          header.setEfinIsreserved(false);

          // invoice created through the po hold plan
          if (InvoiceRevokeDAO.RDV_DOCUMENT.equals(invType)) {
            InvoiceRevokeDAO.releaseTempEncumbrance(header);
          }
        }

        // Insert approval history
        if (!StringUtils.isEmpty(header.getId())) {
          comments = Resource.getProperty("utility.massrevoke", var.getLanguage());
          historyData = InvoiceRevokeDAO.getHistoryData(header, comments);
          if (historyData != null) {
            count = Utility.InsertApprovalHistory(historyData);
          }
        }

        // alert process
        if (count > 0 && !StringUtils.isEmpty(header.getId())) {
          Role objCreatedRole = null;
          if (header.getCreatedBy().getADUserRolesList().size() > 0) {
            objCreatedRole = header.getCreatedBy().getADUserRolesList().get(0).getRole();
          }
          if (inpWindowId.equals(InvoiceRevokeDAO.API_DOCUMENT)
              || inpWindowId.equals(InvoiceRevokeDAO.RDV_DOCUMENT)) {
            alertWindow = AlertWindow.PurchaseInvoice;
          } else if (inpWindowId.equals(InvoiceRevokeDAO.PPI_DOCUMENT)) {
            alertWindow = AlertWindow.PIAPPrepaymentInvoice;
          } else if (inpWindowId.equals(InvoiceRevokeDAO.PPA_DOCUMENT)) {
            alertWindow = AlertWindow.PIAPPrepaymentApplication;
          }

          // get alert rule for invoice
          OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
              "as e where e.client.id='" + header.getClient().getId() + "' and e.efinProcesstype='"
                  + alertWindow + "'");
          if (queryAlertRule.list().size() > 0) {
            AlertRule objRule = queryAlertRule.list().get(0);
            alertRuleId = objRule.getId();
          }

          // remove approval alert
          OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
              "as e where e.referenceSearchKey='" + result.get(i) + "' and e.alertStatus='NEW'");
          if (alertQuery.list().size() > 0) {
            for (Alert objAlert : alertQuery.list()) {
              objAlert.setAlertStatus("SOLVED");
              OBDal.getInstance().save(objAlert);
            }
          }
          // check and insert alert recipient
          OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
          if (receipientQuery.list().size() > 0) {
            for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
              includeRecipient.add(objAlertReceipient.getRole().getId());
              OBDal.getInstance().remove(objAlertReceipient);
            }
          }
          includeRecipient.add(objCreatedRole.getId());
          // avoid duplicate recipient
          HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
          Iterator<String> iterator = incluedSet.iterator();
          while (iterator.hasNext()) {
            AlertUtility.insertAlertRecipient(iterator.next(), null, var.getClient(), alertWindow);
          }

          // set alert for next approver
          Description = Resource.getProperty("utility.purchaseinvoice.revoked", var.getLanguage())
              + " " + header.getCreatedBy().getName();
          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo(),
                objNextRoleLine.getRole().getId(),
                objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId(),
                header.getClient().getId(), Description, "NEW", alertWindow,
                "utility.purchaseinvoice.revoked", Constants.GENERIC_TEMPLATE);
          }

          // delete records from next role table
          DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
              strInvoiceType);

          // Removing Forward and RMI Id
          if (header.getEutForward() != null) {
            forwardDao.setForwardStatusAsDraft(header.getEutForward());
            forwardDao.revokeRemoveForwardRmiFromWindows(header.getId(), Constants.AP_INVOICE);
          }
          if (header.getEutReqmoreinfo() != null) {
            forwardDao.setForwardStatusAsDraft(header.getEutReqmoreinfo());
            forwardDao.revokeRemoveRmiFromWindows(header.getId(), Constants.AP_INVOICE);
          }

          Result = "Success";
          OBDal.getInstance().save(header);
          OBDal.getInstance().flush();

          try {
            // DMS integration
            DMSInvoiceService dmsService = new DMSInvoiceServiceImpl();
            dmsService.rejectAndReactivateOperations(header);
          } catch (Exception e) {

          }

        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (LOG.isDebugEnabled()) {
        LOG.error("Exception While Revoke Invoice :", e);
      }
      Result = "Error";
    }
    return Result;
  }

  @Override
  public String validateRecord(String selectIds, String inpWindowId) {
    String ids = null;
    List<String> result = Arrays.asList(selectIds.split("\\s*,\\s*"));
    for (int i = 0; i < result.size(); i++) {
      Invoice inv = OBDal.getInstance().get(Invoice.class, result.get(i));
      if (inv.getDocumentStatus().equals("DR") || inv.getDocumentStatus().equals("ESCM_TR")) {
        if (ids == null) {
          ids = inv.getDocumentNo();
        } else {
          ids = ids + ", " + inv.getDocumentNo();
        }
      }
    }
    return ids;
  }

  /**
   * This method is used to get document type id for invoice.
   * 
   * @param type
   * @param client
   * @return
   */
  private String getDocumentType(String type, String client) {
    List<DocumentType> docTypeList = null;
    String DocumentId = "";
    StringBuilder documentId = new StringBuilder();
    try {
      OBContext.setAdminMode();
      if (type.equals("API")) {
        OBQuery<DocumentType> doctype = OBDal.getInstance().createQuery(DocumentType.class,
            "efinIsprepayinv='N' and efinIsprepayinvapp='N' and efinIsrdvinv='N' and documentCategory='API' and client.id='"
                + client + "'");
        docTypeList = doctype.list();
        for (DocumentType docType : docTypeList) {
          documentId = documentId.append(",'" + docType.getId() + "'");
        }
      } else if (type.equals("PPI")) {
        OBQuery<DocumentType> doctype = OBDal.getInstance().createQuery(DocumentType.class,
            "efinIsprepayinv='Y' and efinIsprepayinvapp='N' and efinIsrdvinv='N' and documentCategory='API' and client.id='"
                + client + "'");
        docTypeList = doctype.list();
        for (DocumentType docType : docTypeList) {
          documentId = documentId.append(",'" + docType.getId() + "'");
        }
      } else if (type.equals("PPA")) {
        OBQuery<DocumentType> doctype = OBDal.getInstance().createQuery(DocumentType.class,
            "efinIsprepayinv='N' and efinIsprepayinvapp='Y' and efinIsrdvinv='N' and documentCategory='API' and client.id='"
                + client + "'");
        docTypeList = doctype.list();
        for (DocumentType docType : docTypeList) {
          documentId = documentId.append(",'" + docType.getId() + "'");
        }
      } else if (type.equals("RDV")) {
        OBQuery<DocumentType> doctype = OBDal.getInstance().createQuery(DocumentType.class,
            "efinIsprepayinv='N' and efinIsprepayinvapp='N' and efinIsrdvinv='Y' and documentCategory='API' and client.id='"
                + client + "'");
        docTypeList = doctype.list();
        for (DocumentType docType : docTypeList) {
          documentId = documentId.append(",'" + docType.getId() + "'");
        }
      }
      return documentId.toString().replaceFirst(",", "");
    } catch (Exception e) {
      e.printStackTrace();
      if (LOG.isDebugEnabled()) {
        LOG.error("Exception While Revoke Invoice :", e);
      }
      return DocumentId;
    }
  }

}
