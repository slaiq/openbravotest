package sa.elm.ob.utility.ad_process.Forward;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.ui.WindowTrl;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinPropertyCompensation;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_process.ForwardRmiRoleAccess.ForwardRoleAccessDAO;
import sa.elm.ob.utility.ad_process.ForwardRmiRoleAccess.ForwardRoleAccessDAOImpl;
import sa.elm.ob.utility.ad_process.RequestMoreInformation.RequestMoreInformationVO;
import sa.elm.ob.utility.util.ActionHistoryE;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Preferences;
import sa.elm.ob.utility.util.PrimaryKeyDocumentTypeE;
import sa.elm.ob.utility.util.Utility;

public class ForwardRequestMoreInfoDAOImpl implements ForwardRequestMoreInfoDAO {
  private static final Logger log = Logger.getLogger(ForwardRequestMoreInfoDAOImpl.class);

  @SuppressWarnings("rawtypes")
  @Override
  public JSONObject getUserList(String clientId, String searchTerm, int pagelimit, int page,
      String loggedUserId, String forwardedUserId, String inpRecordId, String windowReference,
      String docType, String roleId, String forwardRMI) throws Exception {
    JSONObject jsob = null, delJson = null, delUserJson = null;
    JSONArray jsonArray = new JSONArray();
    String whereclause = "";
    String sql = "";
    Query query = null;
    Date currentDate = new Date();
    String originalUserId = null;
    try {
      OBContext.setAdminMode();
      jsob = new JSONObject();
      delJson = new JSONObject();
      delUserJson = new JSONObject();
      if (searchTerm != null && !searchTerm.equals(""))
        whereclause = " and (ad_user.name ilike :name or bp.value ilike :bpvalue )";
      query = OBDal.getInstance().getSession().createSQLQuery(
          " select   distinct ad_user.ad_user_id, (bp.value||' - '||ad_user.name) as username from ad_user "
              + " join c_bpartner bp on bp.c_bpartner_id= ad_user.c_bpartner_id  "
              + "  where ad_user.ad_client_id =:clientId and ad_user.isactive='Y' and bp.isemployee='Y' and ad_user."
              + "ad_user_id<>:loggedUserId" + sql + whereclause);
      query.setParameter("clientId", clientId);
      query.setParameter("loggedUserId", loggedUserId);
      if (searchTerm != null && !searchTerm.equals("")) {
        query.setParameter("name", "%" + searchTerm + "%");
        query.setParameter("bpvalue", "%" + searchTerm + "%");
      }
      List totalList = query.list();
      jsob.put("totalRecords", totalList.size());
      query.setFirstResult((page - 1) * pagelimit); // equivalent to OFFSET
      query.setMaxResults(pagelimit);

      /**
       * checking current user is trying to do forward or rmi instead of delegated from user
       **/
      /** checking current user is delegated to user through eut_nextroleline table **/
      delJson = delegatedFromUserValidation(inpRecordId, windowReference, docType, clientId,
          currentDate, loggedUserId, roleId);
      /** checking current user is delegated to user through forward or rmi object **/
      delUserJson = isDelegatedUser(inpRecordId, windowReference, loggedUserId, roleId, forwardRMI);

      if (delJson != null && delJson.length() > 0) {
        if (delJson.has("OriginalUserId")) {
          originalUserId = (String) delJson.get("OriginalUserId");
        }
      } else if (delUserJson != null && delUserJson.length() > 0) {
        if (delUserJson.has("OriginalUserId")) {
          originalUserId = (String) delUserJson.get("OriginalUserId");
        }
      }
      List userList = query.list();
      if (userList != null && userList.size() > 0) {
        for (Object user : userList) {
          Object[] row = (Object[]) user;
          JSONObject jsonData = new JSONObject();
          /** Do not add actual user in list when forward/rmi is done by the delegated user **/
          if (originalUserId == null
              || (originalUserId != null && !originalUserId.equals(row[0].toString()))) {
            jsonData.put("id", row[0].toString());
            jsonData.put("recordIdentifier", row[1].toString());
            jsonArray.put(jsonData);
          }
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");
    } catch (Exception e) {
      log.error("Exception in getUserList :", e);
    }
    return jsob;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public JSONObject getUserRole(String clientId, String searchTerm, int pagelimit, int page,
      String userId, String windowReference, String inpRecordId) throws Exception {
    JSONObject jsob = null;
    JSONArray jsonArray = new JSONArray();
    String whereclause = "";
    String sql = "";
    Query query = null;
    Boolean haveOrgAccess = false;
    try {
      OBContext.setAdminMode();
      jsob = new JSONObject();
      if (searchTerm != null && !searchTerm.equals("")) {
        whereclause = " and ad_role.name ilike :name ";
      }
      query = OBDal.getInstance().getSession()
          .createSQLQuery("select ad_role.ad_role_id,ad_role.name from ad_role join ad_user_roles "
              + " on ad_role.ad_role_id=ad_user_roles.ad_role_id  where ad_user_roles.ad_user_id=:userId "
              + " and ad_user_roles.isactive ='Y' " + sql + whereclause);
      query.setParameter("userId", userId);
      if (searchTerm != null && !searchTerm.equals("")) {
        query.setParameter("name", "%" + searchTerm + "%");
      }
      List totalList = query.list();
      jsob.put("totalRecords", totalList.size());
      query.setFirstResult((page - 1) * pagelimit); // equivalent to OFFSET
      query.setMaxResults(pagelimit);
      List roleList = query.list();
      if (roleList != null && roleList.size() > 0) {
        for (Object role : roleList) {
          Object[] row = (Object[]) role;
          JSONObject jsonData = new JSONObject();
          haveOrgAccess = checkFrwdRmiToRoleHaveOrgAccess(inpRecordId, windowReference,
              row[0].toString());
          if (haveOrgAccess) {
            jsonData.put("id", row[0].toString());
            jsonData.put("recordIdentifier", row[1].toString());
            jsonArray.put(jsonData);
          }
        }
      }
      if (jsonArray.length() > 0)
        jsob.put("data", jsonArray);
      else
        jsob.put("data", "");
    } catch (Exception e) {
      log.error("Exception in getUserRole :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsob;
  }

  public EutForwardReqMoreInfo insertOrUpdateRecord(HttpServletRequest request,
      VariablesSecureApp vars, String inpRecordId, String windowReference, String docType)
      throws Exception {
    EutForwardReqMoreInfo forwardrmi = null;
    EutForwardReqMoreInfo forwardId = null;
    JSONObject json = null, delJson = null;
    Date currentDate = new Date();
    try {
      OBContext.setAdminMode();
      // need to insert a record in Forward_ReqMoreInfo table
      forwardrmi = OBProvider.getInstance().get(EutForwardReqMoreInfo.class);
      forwardrmi.setClient(OBDal.getInstance().get(Client.class, vars.getClient()));
      forwardrmi.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      forwardrmi.setCreationDate(new java.util.Date());
      forwardrmi.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
      forwardrmi.setUpdated(new java.util.Date());
      forwardrmi.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
      forwardrmi.setRecordid(inpRecordId);
      forwardrmi.setDocruleid(windowReference);
      forwardrmi.setForwardRmi(Constants.FORWARD);
      forwardrmi.setStatus(Constants.COMPLETE);
      forwardrmi.setMessage(request.getParameter("inpcomments"));
      // Check whether current user is delegated user
      json = delegatedFromUserValidation(inpRecordId, windowReference, docType, vars.getClient(),
          currentDate, vars.getUser(), vars.getRole());
      delJson = isDelegatedUser(inpRecordId, windowReference, vars.getUser(), vars.getRole(),
          forwardrmi.getForwardRmi());

      if (json != null && json.length() > 0) {
        if (json.has("OriginalUserId") && json.has("OriginalRoleId")) {
          forwardrmi
              .setUserContact(OBDal.getInstance().get(User.class, json.get("OriginalUserId")));
          forwardrmi.setRole(OBDal.getInstance().get(Role.class, json.get("OriginalRoleId")));
          forwardrmi.setDelegatedRole(OBDal.getInstance().get(Role.class, vars.getRole()));
          forwardrmi.setDelegatedUser(OBDal.getInstance().get(User.class, vars.getUser()));
          forwardrmi.setDelegated(true);
        }
      } else if (delJson != null && delJson.length() > 0) {
        if (delJson.has("isDelegated") && delJson.has("OriginalUserId")
            && delJson.has("OriginalRoleId")) {
          forwardrmi
              .setUserContact(OBDal.getInstance().get(User.class, delJson.get("OriginalUserId")));
          forwardrmi.setRole(OBDal.getInstance().get(Role.class, delJson.get("OriginalRoleId")));
          forwardrmi.setDelegatedRole(OBDal.getInstance().get(Role.class, vars.getRole()));
          forwardrmi.setDelegatedUser(OBDal.getInstance().get(User.class, vars.getUser()));
          forwardrmi.setDelegated(true);
        }
      } else {
        forwardrmi.setUserContact(OBDal.getInstance().get(User.class, vars.getUser()));
        forwardrmi.setRole(OBDal.getInstance().get(Role.class, vars.getRole()));
      }
      forwardId = getForwardObj(inpRecordId, windowReference);
      forwardrmi.setRecuser(OBDal.getInstance().get(User.class, request.getParameter("inpToUser")));
      forwardrmi.setRECRole(OBDal.getInstance().get(Role.class, request.getParameter("inpToRole")));

      OBDal.getInstance().save(forwardrmi);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(forwardrmi);
      if (forwardId != null) {
        forwardrmi.setRequest(forwardId.getRequest());
      } else {
        forwardrmi.setRequest(forwardrmi.getId());
      }

    } catch (final Exception e) {
      log.error("Exception in insertOrUpdateRecord", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return forwardrmi;
  }

  public EutForwardReqMoreInfo insertOrUpdateRecordRMI(HttpServletRequest request,
      VariablesSecureApp vars, String inpRecordId, String recordType, String windowReference,
      String doctype) throws Exception {
    EutForwardReqMoreInfo forwardrmi = null;
    EutForwardReqMoreInfo rmi = null;
    EutForwardReqMoreInfo rmiRequestObj = null;
    JSONObject json = null, delJson = null;
    Date currentDate = new Date();
    try {
      OBContext.setAdminMode();
      rmi = getReqMoreInfo(inpRecordId, windowReference);

      // need to insert a record in Forward_ReqMoreInfo table
      forwardrmi = OBProvider.getInstance().get(EutForwardReqMoreInfo.class);
      forwardrmi.setClient(OBDal.getInstance().get(Client.class, vars.getClient()));
      forwardrmi.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      forwardrmi.setCreationDate(new java.util.Date());
      forwardrmi.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
      forwardrmi.setUpdated(new java.util.Date());
      forwardrmi.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
      forwardrmi.setRecordid(inpRecordId);
      forwardrmi.setDocruleid(windowReference);
      forwardrmi.setForwardRmi(Constants.REQUEST_MORE_INFORMATION);
      if (recordType.equals(Constants.REQUEST)) {
        forwardrmi.setREQResponse(Constants.REQUEST);
        if (request.getParameter("inprequest") != null
            && !request.getParameter("inprequest").equals("")) {
          forwardrmi.setMessage(StringUtils.substring(request.getParameter("inprequest"), 0, 2000));
        }
      }
      if (recordType.equals(Constants.RESPONSE)) {
        forwardrmi.setREQResponse(Constants.RESPONSE);
        if (request.getParameter("inpresponse") != null
            && !request.getParameter("inpresponse").equals("")) {
          forwardrmi
              .setMessage(StringUtils.substring(request.getParameter("inpresponse"), 0, 2000));
        }
        forwardrmi.setProcessed(true);
        // get requester object
        // rmi = getReqMoreInfo(inpRecordId, windowReference);
        // set request id in response record
        rmiRequestObj = getRMIRequestId(request.getParameter("inpToUser"),
            request.getParameter("inpToRole"), rmi.getRequest(), doctype);
        if (rmiRequestObj != null) {
          forwardrmi.setRequest(rmiRequestObj.getId());
        }
        // make processed flag as 'Y' for request id
        rmi.setProcessed(true);
        OBDal.getInstance().save(rmi);

      }
      forwardrmi.setStatus(Constants.COMPLETE);
      // get old rmi object
      // rmi = getReqMoreInfo(inpRecordId, windowReference);
      // if (rmi == null) {

      // Check whether current user is delegated user
      if (recordType.equals(Constants.REQUEST)) {
        json = delegatedFromUserValidation(inpRecordId, windowReference, doctype, vars.getClient(),
            currentDate, vars.getUser(), vars.getRole());
        delJson = isDelegatedUser(inpRecordId, windowReference, vars.getUser(), vars.getRole(),
            forwardrmi.getForwardRmi());

        if (json != null && json.length() > 0) {
          if (json.has("OriginalUserId") && json.has("OriginalRoleId")) {
            forwardrmi
                .setUserContact(OBDal.getInstance().get(User.class, json.get("OriginalUserId")));
            forwardrmi.setRole(OBDal.getInstance().get(Role.class, json.get("OriginalRoleId")));
            forwardrmi.setDelegatedRole(OBDal.getInstance().get(Role.class, vars.getRole()));
            forwardrmi.setDelegatedUser(OBDal.getInstance().get(User.class, vars.getUser()));
            forwardrmi.setDelegated(true);
          }
        } else if (delJson != null && delJson.length() > 0) {
          if (delJson.has("isDelegated") && delJson.has("OriginalUserId")
              && delJson.has("OriginalRoleId")) {
            forwardrmi
                .setUserContact(OBDal.getInstance().get(User.class, delJson.get("OriginalUserId")));
            forwardrmi.setRole(OBDal.getInstance().get(Role.class, delJson.get("OriginalRoleId")));
            forwardrmi.setDelegatedRole(OBDal.getInstance().get(Role.class, vars.getRole()));
            forwardrmi.setDelegatedUser(OBDal.getInstance().get(User.class, vars.getUser()));
            forwardrmi.setDelegated(true);
          }
        } else {
          forwardrmi.setUserContact(OBDal.getInstance().get(User.class, vars.getUser()));
          forwardrmi.setRole(OBDal.getInstance().get(Role.class, vars.getRole()));
        }
      } else {
        forwardrmi.setUserContact(OBDal.getInstance().get(User.class, vars.getUser()));
        forwardrmi.setRole(OBDal.getInstance().get(Role.class, vars.getRole()));
      }
      forwardrmi.setRecuser(OBDal.getInstance().get(User.class, request.getParameter("inpToUser")));
      forwardrmi.setRECRole(OBDal.getInstance().get(Role.class, request.getParameter("inpToRole")));
      OBDal.getInstance().save(forwardrmi);
      OBDal.getInstance().flush();

      // setting request id for rmi -req
      if (forwardrmi.getREQResponse() != null
          && forwardrmi.getREQResponse().equals(Constants.REQUEST)) {
        if (rmi != null && rmi.getRequest() != null) {
          forwardrmi.setRequest(rmi.getRequest());
        } else {
          forwardrmi.setRequest(forwardrmi.getId());
        }
      }

    } catch (final Exception e) {
      log.error("Exception in insertOrUpdateRecordRMI", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return forwardrmi;
  }

  @Override
  public EutNextRoleLine insertEutNextRoleLine(HttpServletRequest request, VariablesSecureApp vars,
      String inpRecordId, EutForwardReqMoreInfo forwardrmi, String windowReference)
      throws Exception {
    EutNextRole eutNextRole = null;
    EutNextRoleLine line = null;
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    MaterialIssueRequest mir = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    ShipmentInOut returnTran = null;
    List<EutNextRoleLine> nextrolelinelist = new ArrayList<EutNextRoleLine>();
    List<EutNextRoleLine> dummynextrolelinelist = new ArrayList<EutNextRoleLine>();
    List<EutNextRoleLine> removenextrolelinelist = new ArrayList<EutNextRoleLine>();
    String oldforwardId = null;
    String eutNextRoleId = null;
    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole object
        if (windowReference.equals(Constants.BID_MANAGEMENT)) {
          bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
          eutNextRole = bid.getEUTNextRole();
          if (bid.getEUTForwardReqmoreinfo() != null)
            oldforwardId = bid.getEUTForwardReqmoreinfo().getId();
          // update forward_rmi id in transaction screen
          bid.setEUTForwardReqmoreinfo(forwardrmi);
          OBDal.getInstance().save(bid);
        } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)
            || windowReference.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
          purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
          eutNextRole = purchaseReq.getEutNextRole();
          if (purchaseReq.getEutForward() != null)
            oldforwardId = purchaseReq.getEutForward().getId();
          // update forward id in transaction screen
          purchaseReq.setEutForward(forwardrmi);
          OBDal.getInstance().save(purchaseReq);
        } else if (windowReference.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
            || windowReference.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
          proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
          eutNextRole = proMgmt.getEUTNextRole();
          if (proMgmt.getEUTForwardReqmoreinfo() != null)
            oldforwardId = proMgmt.getEUTForwardReqmoreinfo().getId();
          // update forward_rmi id in transaction screen
          proMgmt.setEUTForwardReqmoreinfo(forwardrmi);
          OBDal.getInstance().save(proMgmt);
        } else if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
          purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
          eutNextRole = purchaseOrder.getEutNextRole();
          if (purchaseOrder.getEutForward() != null)
            oldforwardId = purchaseOrder.getEutForward().getId();
          // update forward id in transaction screen
          purchaseOrder.setEutForward(forwardrmi);
          OBDal.getInstance().save(purchaseOrder);
        } else if (windowReference.equals(Constants.Custody_Transfer)) {
          custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
          eutNextRole = custodyTransfer.getEutNextRole();
          if (custodyTransfer.getEutForward() != null)
            oldforwardId = custodyTransfer.getEutForward().getId();
          // update forward id in transaction screen
          custodyTransfer.setEutForward(forwardrmi);
          OBDal.getInstance().save(custodyTransfer);
        } else if (windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST)
            || windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
            || windowReference.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
          mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
          eutNextRole = mir.getEUTNextRole();
          if (mir.getEUTForward() != null)
            oldforwardId = mir.getEUTForward().getId();
          // update forward id in transaction screen
          mir.setEUTForward(forwardrmi);
          OBDal.getInstance().save(mir);
        } else if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
          returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
          eutNextRole = returnTran.getEutNextRole();
          if (returnTran.getEutForward() != null)
            oldforwardId = returnTran.getEutForward().getId();
          // update forward id in transaction screen
          returnTran.setEutForward(forwardrmi);
          OBDal.getInstance().save(returnTran);
        } else if (windowReference.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
          technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
          eutNextRole = technicalEvlEvent.getEUTNextRole();
          if (technicalEvlEvent.getEUTForward() != null)
            oldforwardId = technicalEvlEvent.getEUTForward().getId();
          // update forward id in transaction screen
          technicalEvlEvent.setEUTForward(forwardrmi);
          OBDal.getInstance().save(technicalEvlEvent);
        } else {
          HashMap<String, String> nextRoleMap = new HashMap<String, String>();
          nextRoleMap = insertFinanceEutNextRoleLine(inpRecordId, forwardrmi, windowReference);
          eutNextRoleId = nextRoleMap.get("eutNextRole");
          oldforwardId = nextRoleMap.get("oldforwardId");
          eutNextRole = OBDal.getInstance().get(EutNextRole.class, eutNextRoleId);
        }
      }
      OBQuery<EutNextRoleLine> nextroleline = OBDal.getInstance().createQuery(EutNextRoleLine.class,
          "as e where e.eUTNextRole.id=:eutNextRoleId and e.role.id =:roleId and e.userContact.id =:userId and e.eUTReqmoreinfo.id is not null ");
      nextroleline.setNamedParameter("eutNextRoleId",
          eutNextRole != null ? eutNextRole.getId() : "");
      nextroleline.setNamedParameter("roleId", forwardrmi.getRECRole().getId());
      nextroleline.setNamedParameter("userId", forwardrmi.getRecuser().getId());
      nextrolelinelist = nextroleline.list();
      if (nextrolelinelist.size() > 0) {
        for (EutNextRoleLine nextroleln : nextrolelinelist) {
          if (nextroleln.getEUTReqmoreinfo() != null) {
            nextroleln.setEUTForwardReqmoreinfo(forwardrmi);
          }
        }
      } else {
        // need to insert a record in eut_nextrole_line table
        line = OBProvider.getInstance().get(EutNextRoleLine.class);
        line.setClient(OBDal.getInstance().get(Client.class, vars.getClient()));
        line.setEUTNextRole(eutNextRole);
        line.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
        line.setCreationDate(new java.util.Date());
        line.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        line.setUpdated(new java.util.Date());
        line.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        line.setRole(forwardrmi.getRECRole());
        line.setUserContact(forwardrmi.getRecuser());
        line.setFromUser(forwardrmi.getUserContact());
        line.setEUTForwardReqmoreinfo(forwardrmi);
        if (forwardrmi.getForwardRmi().equals(Constants.FORWARD)) {
          OBQuery<EutNextRoleLine> dummynextroleline = OBDal.getInstance().createQuery(
              EutNextRoleLine.class,
              "as e where e.eUTNextRole.id=:eutNextRoleId and e.dummyRole is not null");
          dummynextroleline.setNamedParameter("eutNextRoleId", eutNextRole.getId());
          dummynextroleline.setMaxResult(1);
          dummynextrolelinelist = dummynextroleline.list();
          if (dummynextrolelinelist.size() > 0) {
            line.setDummyRole(dummynextrolelinelist.get(0).getDummyRole());
          }
        }
        OBDal.getInstance().save(line);
        OBDal.getInstance().flush();
      }
      // remove old nextrole obj
      OBQuery<EutNextRoleLine> removenextroleline = OBDal.getInstance().createQuery(
          EutNextRoleLine.class,
          "as e where e.eUTNextRole.id=:eutNextRoleId and e.eUTForwardReqmoreinfo.id = :oldforwardId ");
      removenextroleline.setNamedParameter("eutNextRoleId",
          eutNextRole != null ? eutNextRole.getId() : "");
      removenextroleline.setNamedParameter("oldforwardId", oldforwardId);
      removenextrolelinelist = removenextroleline.list();
      if (removenextrolelinelist.size() > 0) {
        EutNextRoleLine nextrolelineobj = removenextrolelinelist.get(0);
        OBDal.getInstance().remove(nextrolelineobj);
      }

    } catch (final Exception e) {
      log.error("Exception in insertEutNextRoleLine", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return line;

  }

  @Override
  public HashMap<String, String> insertFinanceEutNextRoleLine(String inpRecordId,
      EutForwardReqMoreInfo forwardrmi, String windowReference) throws Exception {
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EFINFundsReq fundsReq = null;
    EfinBudgetManencum manencum = null;
    BudgetAdjustment budgetAdjustment = null;
    EfinRDVTransaction rdv = null;
    Invoice purchaseinv = null;
    EfinPropertyCompensation propComp = null;
    HashMap<String, String> nextRoleMap = new HashMap<String, String>();
    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole object
        if (windowReference.equals(Constants.BUDGET)) {
          budget = OBDal.getInstance().get(EFINBudget.class, inpRecordId);
          nextRoleMap.put("eutNextRole", budget.getEUTNextRole().getId());
          if (budget.getEUTForwardReqmoreinfo() != null)
            nextRoleMap.put("oldforwardId", budget.getEUTForwardReqmoreinfo().getId());
          // update forward_rmi id in transaction screen
          budget.setEUTForwardReqmoreinfo(forwardrmi);
          OBDal.getInstance().save(budget);
        } else if (windowReference.equals(Constants.BUDGET_REVISION)) {
          budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, inpRecordId);
          nextRoleMap.put("eutNextRole", budgetRevision.getNextRole().getId());
          if (budgetRevision.getEUTForwardReqmoreinfo() != null)
            nextRoleMap.put("oldforwardId", budgetRevision.getEUTForwardReqmoreinfo().getId());
          // update forward_rmi id in transaction screen
          budgetRevision.setEUTForwardReqmoreinfo(forwardrmi);
          OBDal.getInstance().save(budgetRevision);
        } else if (windowReference.equals(Constants.BUDGETADJUSTMENT)) {
          budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, inpRecordId);
          nextRoleMap.put("eutNextRole", budgetAdjustment.getNextRole().getId());
          if (budgetAdjustment.getEUTForward() != null)
            nextRoleMap.put("oldforwardId", budgetAdjustment.getEUTForward().getId());
          // update forward_rmi id in transaction screen
          budgetAdjustment.setEUTForward(forwardrmi);
          OBDal.getInstance().save(budgetAdjustment);
        }

        else if (windowReference.equals(Constants.FundsReqMgmt)
            || windowReference.equals(Constants.FundsReqMgmt_ORG)) {
          fundsReq = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
          nextRoleMap.put("eutNextRole", fundsReq.getNextRole().getId());
          if (fundsReq.getEUTForward() != null)
            nextRoleMap.put("oldforwardId", fundsReq.getEUTForward().getId());
          // update forward_rmi id in transaction screen
          fundsReq.setEUTForward(forwardrmi);
          OBDal.getInstance().save(fundsReq);
        } else if (windowReference.equals(Constants.ENCUMBRANCE)) {
          manencum = OBDal.getInstance().get(EfinBudgetManencum.class, inpRecordId);
          nextRoleMap.put("eutNextRole", manencum.getNextRole().getId());
          if (manencum.getEUTForwardReqmoreinfo() != null)
            nextRoleMap.put("oldforwardId", manencum.getEUTForwardReqmoreinfo().getId());
          // update forward_rmi id in transaction screen
          manencum.setEUTForwardReqmoreinfo(forwardrmi);
          OBDal.getInstance().save(manencum);

        } else if (windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION)) {
          rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
          nextRoleMap.put("eutNextRole", rdv.getNextRole().getId());
          if (rdv.getEUTForwardReqmoreinfo() != null)
            nextRoleMap.put("oldforwardId", rdv.getEUTForwardReqmoreinfo().getId());
          // update forward_rmi id in transaction screen
          rdv.setEUTForwardReqmoreinfo(forwardrmi);
          OBDal.getInstance().save(rdv);
        } else if (windowReference.equals(Constants.AP_INVOICE)
            || windowReference.equals(Constants.PREPAYMENT_APPLICATION)
            || windowReference.equals(Constants.AP_PREPAYMENT_INVOICE)) {
          purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
          nextRoleMap.put("eutNextRole", purchaseinv.getEutNextRole().getId());
          if (purchaseinv.getEutForward() != null)
            nextRoleMap.put("oldforwardId", purchaseinv.getEutForward().getId());
          // update forward_rmi id in transaction screen
          purchaseinv.setEutForward(forwardrmi);
          OBDal.getInstance().save(purchaseinv);
        } else if (windowReference.equals(Constants.PROPERTY_COMP)) {
          propComp = OBDal.getInstance().get(EfinPropertyCompensation.class, inpRecordId);
          nextRoleMap.put("eutNextRole", propComp.getNextRole().getId());
          if (propComp.getEUTForward() != null)
            nextRoleMap.put("oldforwardId", propComp.getEUTForward().getId());
          // update forward_rmi id in transaction screen
          propComp.setEUTForward(forwardrmi);
          OBDal.getInstance().save(propComp);
        }
      }

    } catch (

    final Exception e) {
      log.error("Exception in insertFinanceEutNextRoleLine", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return nextRoleMap;
  }

  public EutNextRoleLine insertEutNextRoleLineRMI(HttpServletRequest request,
      VariablesSecureApp vars, String inpRecordId, EutForwardReqMoreInfo forwardrmi,
      String documenttype) throws Exception {
    EutNextRole eutNextRole = null;
    EutNextRoleLine line = null;
    EutForwardReqMoreInfo oldRmiObj = null;
    List<EutNextRoleLine> nextrolelinelist = new ArrayList<EutNextRoleLine>();
    List<EutNextRoleLine> removenextrolelinelist = new ArrayList<EutNextRoleLine>();

    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole object
        eutNextRole = getNextRole(inpRecordId, documenttype);
        oldRmiObj = getReqMoreInfo(inpRecordId, documenttype);
        setReqMoreInfoID(inpRecordId, documenttype, forwardrmi);
      }
      OBQuery<EutNextRoleLine> nextroleline = OBDal.getInstance().createQuery(EutNextRoleLine.class,
          "as e where e.eUTNextRole.id=:eutNextRoleId and e.role.id =:roleId and e.userContact.id =:userId and e.eUTForwardReqmoreinfo.id is not null ");
      nextroleline.setNamedParameter("eutNextRoleId",
          eutNextRole != null ? eutNextRole.getId() : "");
      nextroleline.setNamedParameter("roleId", forwardrmi.getRECRole().getId());
      nextroleline.setNamedParameter("userId", forwardrmi.getRecuser().getId());
      nextrolelinelist = nextroleline.list();
      if (nextrolelinelist.size() > 0) {
        for (EutNextRoleLine nextroleln : nextrolelinelist) {
          if (nextroleln.getEUTForwardReqmoreinfo() != null) {
            nextroleln.setEUTReqmoreinfo(forwardrmi);
          }
        }
      } else {
        // need to insert a record in eut_nextrole_line table
        line = OBProvider.getInstance().get(EutNextRoleLine.class);
        line.setClient(OBDal.getInstance().get(Client.class, vars.getClient()));
        if (eutNextRole != null)
          line.setEUTNextRole(eutNextRole);
        line.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
        line.setCreationDate(new java.util.Date());
        line.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        line.setUpdated(new java.util.Date());
        line.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        line.setRole(forwardrmi.getRECRole());
        line.setUserContact(forwardrmi.getRecuser());
        line.setFromUser(forwardrmi.getUserContact());
        line.setEUTReqmoreinfo(forwardrmi);
        OBDal.getInstance().save(line);
        OBDal.getInstance().flush();
      }
      // remove old nextrole obj
      OBQuery<EutNextRoleLine> removenextroleline = OBDal.getInstance().createQuery(
          EutNextRoleLine.class,
          "as e where e.eUTNextRole.id=:eutNextRoleId and e.eUTReqmoreinfo=:oldRmi ");
      removenextroleline.setNamedParameter("eutNextRoleId",
          eutNextRole != null ? eutNextRole.getId() : "");
      removenextroleline.setNamedParameter("oldRmi", oldRmiObj);
      removenextrolelinelist = removenextroleline.list();
      if (removenextrolelinelist != null && removenextrolelinelist.size() > 0) {
        EutNextRoleLine nextrolelineobj = removenextrolelinelist.get(0);
        OBDal.getInstance().remove(nextrolelineobj);
      }

    } catch (final Exception e) {
      log.error("Exception in insertEutNextRoleLine", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return line;
  }

  @Override
  public void insertActionHistory(HttpServletRequest request, VariablesSecureApp vars,
      String inpRecordId, String documenttype, String forwardRevoketype,
      EutForwardReqMoreInfo forwardrmi, boolean revoke) throws Exception {
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    ShipmentInOut returnTran = null;
    MaterialIssueRequest mir = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    String clientId = null;
    String orgId = null;

    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole object
        if (documenttype.equals(Constants.BID_MANAGEMENT)) {
          bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
          clientId = bid.getClient().getId();
          orgId = bid.getOrganization().getId();

        } else if (documenttype.equals(Constants.PURCHASE_REQUISITION_DIRECT)
            || documenttype.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
          purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
          clientId = purchaseReq.getClient().getId();
          orgId = purchaseReq.getOrganization().getId();

        } else if (documenttype.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
            || documenttype.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
          proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
          clientId = proMgmt.getClient().getId();
          orgId = proMgmt.getOrganization().getId();

        } else if (documenttype.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
          purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
          clientId = purchaseOrder.getClient().getId();
          orgId = purchaseOrder.getOrganization().getId();

        } else if (documenttype.equals(Constants.Custody_Transfer)) {
          custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
          clientId = custodyTransfer.getClient().getId();
          orgId = custodyTransfer.getOrganization().getId();

        } else if (documenttype.equals(Constants.MATERIAL_ISSUE_REQUEST)
            || documenttype.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
            || documenttype.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
          mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
          clientId = mir.getClient().getId();
          orgId = mir.getOrganization().getId();
        } else if (documenttype.equals(Constants.RETURN_TRANSACTION)) {
          returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
          clientId = returnTran.getClient().getId();
          orgId = returnTran.getOrganization().getId();
        } else if (documenttype.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
          technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
          clientId = technicalEvlEvent.getClient().getId();
          orgId = technicalEvlEvent.getOrganization().getId();
        } else {
          HashMap<String, String> clientOrgMap = new HashMap<String, String>();
          clientOrgMap = getClientIdOrgIdFinance(inpRecordId, documenttype);
          clientId = clientOrgMap.get("clientId");
          orgId = clientOrgMap.get("orgId");
        }
      }
      if (!StringUtils.isEmpty(inpRecordId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", vars.getRole());
        historyData.put("UserId", vars.getUser());
        historyData.put("HeaderId", inpRecordId);
        if (forwardRevoketype.equals(Constants.FORWARD)) {
          historyData.put("Status", Constants.FORWARD);
          if (!revoke)
            historyData.put("NextApprover",
                forwardrmi.getRecuser().getName()
                    + (forwardrmi.getRECRole() != null ? " / " + forwardrmi.getRECRole().getName()
                        : ""));
          else
            historyData.put("NextApprover", getUserName(request.getParameter("inpToUser"))
                + getRoleName(request.getParameter("inpToRole")));
          if (forwardrmi.getMessage() != null)
            historyData.put("Comments", forwardrmi.getMessage());
          else
            historyData.put("Comments", "");
        }
        if (forwardRevoketype.equals(Constants.FORWARD_REVOKE)) {
          historyData.put("Status", Constants.FORWARD_REVOKE);
          historyData.put("NextApprover", "");
          if (forwardrmi.getMessage() != null)
            historyData.put("Comments", forwardrmi.getMessage());
          else
            historyData.put("Comments", "");
        }

        ActionHistoryE e = ActionHistoryE.getColumnNames(documenttype);
        historyData.put("HistoryTable", e.getHistoryTable());
        historyData.put("HeaderColumn", e.getHeaderColumn());
        historyData.put("ActionColumn", e.getActionColumn());
        if (documenttype.equals(Constants.ENCUMBRANCE)) {
          InsertEncumbranceApprovalHistory(historyData);
        } else {
          Utility.InsertApprovalHistory(historyData);
        }
      }
    } catch (final Exception e) {
      log.error("Exception in insertActionHistory", e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public void insertActionHistoryRMIRevoke(HttpServletRequest request, VariablesSecureApp vars,
      String inpRecordId, String documenttype, String RMIRevoketype,
      EutForwardReqMoreInfo forwardrmi) throws Exception {

    JSONObject json = null;
    String clientId = null;
    String orgId = null;
    try {
      if (inpRecordId != null) {
        // get eut_nextrole object
        json = getClientIdOrgId(inpRecordId, documenttype);
        if (json != null && json.length() > 0) {
          if (json.has("clientId")) {
            clientId = json.getString("clientId");
          }
          if (json.has("orgId")) {
            orgId = json.getString("orgId");
          }
        }
      }
      if (!StringUtils.isEmpty(inpRecordId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", forwardrmi.getRole().getId());
        historyData.put("UserId", forwardrmi.getUserContact().getId());
        historyData.put("HeaderId", inpRecordId);
        historyData.put("Comments", "");
        if (RMIRevoketype.equals(Constants.REQUEST_MORE_INFORMATION)) {
          historyData.put("Status", Constants.REQUEST_MORE_INFORMATION);
          historyData.put("NextApprover", forwardrmi.getRecuser().getName());
        }
        if (RMIRevoketype.equals(Constants.REQUEST_MORE_INFORMATION_REVOKE)) {
          historyData.put("Status", Constants.REQUEST_MORE_INFORMATION_REVOKE);
          historyData.put("NextApprover", "");
          if (forwardrmi.getMessage() != null)
            historyData.put("Comments", forwardrmi.getMessage());
          else
            historyData.put("Comments", "");
        }

        ActionHistoryE e = ActionHistoryE.getColumnNames(documenttype);
        historyData.put("HistoryTable", e.getHistoryTable());
        historyData.put("HeaderColumn", e.getHeaderColumn());
        historyData.put("ActionColumn", e.getActionColumn());
        if (documenttype.equals(Constants.ENCUMBRANCE)) {
          InsertEncumbranceApprovalHistory(historyData);
        } else {
          Utility.InsertApprovalHistory(historyData);
        }
      }
    } catch (final Exception e) {
      log.error("Exception in insertActionHistoryRMIRevoke", e);
    }

  }

  @Override
  public EutForwardReqMoreInfo getReqMoreInfo(String inpRecordId, String windowReference)
      throws Exception {
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    MaterialIssueRequest mir = null;
    Order purchaseOrder = null;
    EutForwardReqMoreInfo rmi = null;
    ShipmentInOut custodyTransfer = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    ShipmentInOut returnTran = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BID_MANAGEMENT)) {
        bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
        rmi = bid.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || windowReference.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
        rmi = purchaseReq.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || windowReference.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        rmi = proMgmt.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        rmi = purchaseOrder.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        rmi = custodyTransfer.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST)
          || windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || windowReference.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        rmi = mir.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        rmi = returnTran.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
        rmi = technicalEvlEvent.getEUTReqmoreinfo();
      } else {
        rmi = getFinanceReqMoreInfo(inpRecordId, windowReference);
      }
    } catch (final Exception e) {
      log.error("Exception in getReqMoreInfo", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return rmi;
  }

  @Override
  public EutForwardReqMoreInfo getFinanceReqMoreInfo(String inpRecordId, String windowReference)
      throws Exception {
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EfinBudgetManencum manencum = null;
    EutForwardReqMoreInfo rmi = null;
    EFINFundsReq fundsReq = null;
    BudgetAdjustment budgetAdjustment = null;
    EfinRDVTransaction rdv = null;
    Invoice purchaseinv = null;
    EfinPropertyCompensation propertComp = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BUDGET)) {
        budget = OBDal.getInstance().get(EFINBudget.class, inpRecordId);
        rmi = budget.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.BUDGET_REVISION)) {
        budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, inpRecordId);
        rmi = budgetRevision.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.FundsReqMgmt)
          || windowReference.equals(Constants.FundsReqMgmt_ORG)) {
        fundsReq = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
        rmi = fundsReq.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.ENCUMBRANCE)) {
        manencum = OBDal.getInstance().get(EfinBudgetManencum.class, inpRecordId);
        rmi = manencum.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.BUDGETADJUSTMENT)) {
        budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, inpRecordId);
        rmi = budgetAdjustment.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION)
          || windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE)) {
        rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
        rmi = rdv.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.AP_INVOICE)
          || windowReference.equals(Constants.PREPAYMENT_APPLICATION)
          || windowReference.equals(Constants.AP_PREPAYMENT_INVOICE)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
        rmi = purchaseinv.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.PROPERTY_COMP)) {
        propertComp = OBDal.getInstance().get(EfinPropertyCompensation.class, inpRecordId);
        rmi = propertComp.getEUTReqmoreinfo();
      }

    } catch (final Exception e) {
      log.error("Exception in getFinanceReqMoreInfo", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return rmi;
  }

  @Override
  public EutNextRole getNextRole(String inpRecordId, String windowReference) throws Exception {
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    MaterialIssueRequest mir = null;
    EutNextRole nextRole = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    ShipmentInOut returnTran = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BID_MANAGEMENT)) {
        bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
        nextRole = bid.getEUTNextRole();
      } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || windowReference.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
        nextRole = purchaseReq.getEutNextRole();
      } else if (windowReference.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || windowReference.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        nextRole = proMgmt.getEUTNextRole();
      } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        nextRole = purchaseOrder.getEutNextRole();
      } else if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        nextRole = purchaseOrder.getEutNextRole();
      } else if (windowReference.equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        nextRole = custodyTransfer.getEutNextRole();
      } else if (windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST)
          || windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || windowReference.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        nextRole = mir.getEUTNextRole();
      } else if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        nextRole = returnTran.getEutNextRole();
      } else if (windowReference.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
        nextRole = technicalEvlEvent.getEUTNextRole();
      } else {
        nextRole = getFinanceNextRole(inpRecordId, windowReference);
      }
    } catch (final Exception e) {
      log.error("Exception in getNextRole", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return nextRole;
  }

  @Override
  public EutNextRole getFinanceNextRole(String inpRecordId, String windowReference)
      throws Exception {
    EutNextRole nextRole = null;
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EfinBudgetManencum manencum = null;
    EFINFundsReq fundsReq = null;
    BudgetAdjustment budgetAdjustment = null;
    EfinRDVTransaction rdv = null;
    Invoice purchaseinv = null;
    EfinPropertyCompensation propertyComp = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BUDGET)) {
        budget = OBDal.getInstance().get(EFINBudget.class, inpRecordId);
        nextRole = budget.getEUTNextRole();
      } else if (windowReference.equals(Constants.BUDGET_REVISION)) {
        budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, inpRecordId);
        nextRole = budgetRevision.getNextRole();
      } else if (windowReference.equals(Constants.FundsReqMgmt)
          || windowReference.equals(Constants.FundsReqMgmt_ORG)) {
        fundsReq = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
        nextRole = fundsReq.getNextRole();
      } else if (windowReference.equals(Constants.ENCUMBRANCE)) {
        manencum = OBDal.getInstance().get(EfinBudgetManencum.class, inpRecordId);
        nextRole = manencum.getNextRole();
      } else if (windowReference.equals(Constants.BUDGETADJUSTMENT)) {
        budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, inpRecordId);
        nextRole = budgetAdjustment.getNextRole();

      } else if (windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION)
          || windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE)) {
        rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
        nextRole = rdv.getNextRole();
      } else if (windowReference.equals(Constants.AP_INVOICE)
          || windowReference.equals(Constants.PREPAYMENT_APPLICATION)
          || windowReference.equals(Constants.AP_PREPAYMENT_INVOICE)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
        nextRole = purchaseinv.getEutNextRole();
      } else if (windowReference.equals(Constants.PROPERTY_COMP)) {
        propertyComp = OBDal.getInstance().get(EfinPropertyCompensation.class, inpRecordId);
        nextRole = propertyComp.getNextRole();
      }
    } catch (final Exception e) {
      log.error("Exception in getFinanceNextRole", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return nextRole;

  }

  @Override
  public void setReqMoreInfoID(String inpRecordId, String windowReference,
      EutForwardReqMoreInfo rmi) throws Exception {
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    ShipmentInOut returnTran = null;
    MaterialIssueRequest mir = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BID_MANAGEMENT)) {
        bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);

        bid.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          bid.setRequestMoreInformation("Y");
        } else {
          bid.setRequestMoreInformation("N");
        }
        OBDal.getInstance().save(bid);
      } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || windowReference.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
        purchaseReq.setEutReqmoreinfo(rmi);
        if (rmi != null) {
          purchaseReq.setEscmReqMoreInfo("Y");
        } else {
          purchaseReq.setEscmReqMoreInfo("N");
        }
        OBDal.getInstance().save(purchaseReq);
      } else if (windowReference.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || windowReference.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        proMgmt.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          proMgmt.setRequestMoreInformation("Y");
        } else {
          proMgmt.setRequestMoreInformation("N");
        }
        OBDal.getInstance().save(proMgmt);
      } else if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        purchaseOrder.setEutReqmoreinfo(rmi);
        if (rmi != null) {
          purchaseOrder.setEscmReqMoreInfo("Y");
        } else {
          purchaseOrder.setEscmReqMoreInfo("N");
        }
        OBDal.getInstance().save(purchaseOrder);
      } else if (windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST)
          || windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || windowReference.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        mir.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          mir.setRequestMoreInformation("Y");
        } else {
          mir.setRequestMoreInformation("N");
        }
        OBDal.getInstance().save(mir);
      } else if (windowReference.equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        custodyTransfer.setEutReqmoreinfo(rmi);
        if (rmi != null) {
          custodyTransfer.setEscmReqMoreInfo("Y");
        } else {
          custodyTransfer.setEscmReqMoreInfo("N");
        }
        OBDal.getInstance().save(custodyTransfer);
      } else if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        returnTran.setEutReqmoreinfo(rmi);
        if (rmi != null) {
          returnTran.setEscmReqMoreInfo("Y");
        } else {
          returnTran.setEscmReqMoreInfo("N");
        }
        OBDal.getInstance().save(returnTran);
      } else if (windowReference.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
        technicalEvlEvent.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          technicalEvlEvent.setEscmReqMoreInfo("Y");
        } else {
          technicalEvlEvent.setEscmReqMoreInfo("N");
        }
        OBDal.getInstance().save(technicalEvlEvent);
      } else {
        setFinanceReqMoreInfoID(inpRecordId, windowReference, rmi);
      }
    } catch (final Exception e) {
      log.error("Exception in getEUTNextRole", e);
    }
  }

  @Override
  public void setFinanceReqMoreInfoID(String inpRecordId, String windowReference,
      EutForwardReqMoreInfo rmi) throws Exception {
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EFINFundsReq fundsReq = null;
    EfinBudgetManencum manencum = null;
    BudgetAdjustment budgetAdjustment = null;
    EfinRDVTransaction rdv = null;
    Invoice purchaseinv = null;
    EfinPropertyCompensation propComp = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BUDGET)) {
        budget = OBDal.getInstance().get(EFINBudget.class, inpRecordId);

        budget.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          budget.setRequestMoreInformation("Y");
        } else {
          budget.setRequestMoreInformation("N");
        }
        OBDal.getInstance().save(budget);
      } else if (windowReference.equals(Constants.BUDGET_REVISION)) {
        budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, inpRecordId);

        budgetRevision.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          budgetRevision.setRequestMoreInformation("Y");
        } else {
          budgetRevision.setRequestMoreInformation("N");
        }
        OBDal.getInstance().save(budgetRevision);
      } else if (windowReference.equals(Constants.FundsReqMgmt)
          || windowReference.equals(Constants.FundsReqMgmt_ORG)) {
        fundsReq = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);

        fundsReq.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          fundsReq.setRequestMoreInformation("Y");
        } else {
          fundsReq.setRequestMoreInformation("N");
        }
        OBDal.getInstance().save(fundsReq);
      } else if (windowReference.equals(Constants.ENCUMBRANCE)) {
        manencum = OBDal.getInstance().get(EfinBudgetManencum.class, inpRecordId);
        manencum.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          manencum.setRequestMoreInformation("Y");
        } else {
          manencum.setRequestMoreInformation("N");
        }
        OBDal.getInstance().save(manencum);

      } else if (windowReference.equals(Constants.BUDGETADJUSTMENT)) {
        budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, inpRecordId);
        budgetAdjustment.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          budgetAdjustment.setREQMoreInfo("Y");
        } else {
          budgetAdjustment.setREQMoreInfo("N");
        }
        OBDal.getInstance().save(budgetAdjustment);
      } else if (windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION)
          || windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE)) {
        rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
        rdv.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          rdv.setRequestMoreInformation("Y");
        } else {
          rdv.setRequestMoreInformation("N");
        }
        OBDal.getInstance().save(rdv);
      }

      else if (windowReference.equals(Constants.AP_INVOICE)
          || windowReference.equals(Constants.PREPAYMENT_APPLICATION)
          || windowReference.equals(Constants.AP_PREPAYMENT_INVOICE)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);

        purchaseinv.setEutReqmoreinfo(rmi);
        if (rmi != null) {
          purchaseinv.setEfinReqMoreInfo("Y");
        } else {
          purchaseinv.setEfinReqMoreInfo("N");
        }
        OBDal.getInstance().save(purchaseinv);
      } else if (windowReference.equals(Constants.PROPERTY_COMP)) {
        propComp = OBDal.getInstance().get(EfinPropertyCompensation.class, inpRecordId);
        propComp.setEUTReqmoreinfo(rmi);
        if (rmi != null) {
          propComp.setRequestMoreInformation(true);
        } else {
          propComp.setRequestMoreInformation(false);
        }
        OBDal.getInstance().save(propComp);

      }

    } catch (final Exception e) {
      log.error("Exception in setFinanceReqMoreInfoID", e);
    }
  }

  @Override
  public JSONObject getClientIdOrgId(String inpRecordId, String docType) throws Exception {
    JSONObject jsob = null;
    try {
      OBContext.setAdminMode();
      jsob = new JSONObject();
      EscmBidMgmt bid = null;
      Requisition purchaseReq = null;
      EscmProposalMgmt proMgmt = null;
      Order purchaseOrder = null;
      ShipmentInOut custodyTransfer = null;
      MaterialIssueRequest mir = null;
      EscmTechnicalevlEvent technicalEvlEvent = null;
      ShipmentInOut returnTran = null;
      String clientId = null, orgId = null;

      if (docType.equals(Constants.BID_MANAGEMENT)) {
        bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
        clientId = bid.getClient().getId();
        orgId = bid.getOrganization().getId();

      } else if (docType.equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || docType.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
        clientId = purchaseReq.getClient().getId();
        orgId = purchaseReq.getOrganization().getId();

      } else if (docType.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || docType.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        clientId = proMgmt.getClient().getId();
        orgId = proMgmt.getOrganization().getId();
      } else if (docType.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        clientId = purchaseOrder.getClient().getId();
        orgId = purchaseOrder.getOrganization().getId();
      } else if (docType.equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        clientId = custodyTransfer.getClient().getId();
        orgId = custodyTransfer.getOrganization().getId();
      } else if (docType.equals(Constants.MATERIAL_ISSUE_REQUEST)
          || docType.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || docType.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        clientId = mir.getClient().getId();
        orgId = mir.getOrganization().getId();
      } else if (docType.equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        clientId = returnTran.getClient().getId();
        orgId = returnTran.getOrganization().getId();
      } else if (docType.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
        clientId = technicalEvlEvent.getClient().getId();
        orgId = technicalEvlEvent.getOrganization().getId();
      } else {
        HashMap<String, String> clientOrgMap = new HashMap<String, String>();
        clientOrgMap = getClientIdOrgIdFinance(inpRecordId, docType);
        clientId = clientOrgMap.get("clientId");
        orgId = clientOrgMap.get("orgId");
      }
      if (clientId != null && orgId != null) {
        jsob.put("clientId", clientId);
        jsob.put("orgId", orgId);
      }
    } catch (Exception e) {
      log.error("Exception in getClientIdOrgId :", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return jsob;
  }

  @Override
  public HashMap<String, String> getClientIdOrgIdFinance(String inpRecordId, String docType)
      throws Exception {
    HashMap<String, String> clientOrgMap = new HashMap<String, String>();
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EFINFundsReq fundsReq = null;
    EfinBudgetManencum manencum = null;
    BudgetAdjustment budgetAdjustment = null;
    EfinRDVTransaction rdv = null;
    Invoice purchaseinv = null;
    EfinPropertyCompensation propertyComp = null;
    try {
      OBContext.setAdminMode();

      if (docType.equals(Constants.BUDGET)) {
        budget = OBDal.getInstance().get(EFINBudget.class, inpRecordId);
        clientOrgMap.put("clientId", budget.getClient().getId());
        clientOrgMap.put("orgId", budget.getOrganization().getId());
      } else if (docType.equals(Constants.BUDGET_REVISION)) {
        budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, inpRecordId);
        clientOrgMap.put("clientId", budgetRevision.getClient().getId());
        clientOrgMap.put("orgId", budgetRevision.getOrganization().getId());
      } else if (docType.equals(Constants.FundsReqMgmt)
          || docType.equals(Constants.FundsReqMgmt_ORG)) {
        fundsReq = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
        clientOrgMap.put("clientId", fundsReq.getClient().getId());
        clientOrgMap.put("orgId", fundsReq.getOrganization().getId());
      } else if (docType.equals(Constants.ENCUMBRANCE)) {
        manencum = OBDal.getInstance().get(EfinBudgetManencum.class, inpRecordId);
        clientOrgMap.put("clientId", manencum.getClient().getId());
        clientOrgMap.put("orgId", manencum.getOrganization().getId());
      } else if (docType.equals(Constants.BUDGETADJUSTMENT)) {
        budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, inpRecordId);
        clientOrgMap.put("clientId", budgetAdjustment.getClient().getId());
        clientOrgMap.put("orgId", budgetAdjustment.getOrganization().getId());
      } else if (docType.equals(Constants.RECEIPT_DELIVERY_VERIFICATION)
          || docType.equals(Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE)) {
        rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
        clientOrgMap.put("clientId", rdv.getClient().getId());
        clientOrgMap.put("orgId", rdv.getOrganization().getId());
      } else if (docType.equals(Constants.AP_INVOICE)
          || docType.equals(Constants.PREPAYMENT_APPLICATION)
          || docType.equals(Constants.AP_PREPAYMENT_INVOICE)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
        clientOrgMap.put("clientId", purchaseinv.getClient().getId());
        clientOrgMap.put("orgId", purchaseinv.getOrganization().getId());
      } else if (docType.equals(Constants.PROPERTY_COMP)) {
        propertyComp = OBDal.getInstance().get(EfinPropertyCompensation.class, inpRecordId);
        clientOrgMap.put("clientId", propertyComp.getClient().getId());
        clientOrgMap.put("orgId", propertyComp.getOrganization().getId());
      }
    } catch (Exception e) {
      log.error("Exception in getClientIdOrgIdFinance :", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return clientOrgMap;
  }

  public void insertActionHistoryRMI(HttpServletRequest request, VariablesSecureApp vars,
      String inpRecordId, EutForwardReqMoreInfo forwardrmi, String documenttype,
      String rmiRevoketype, Boolean isOriginalUser) throws Exception {
    JSONObject json = null;
    String clientId = null;
    String orgId = null;
    try {
      if (inpRecordId != null) {
        json = getClientIdOrgId(inpRecordId, documenttype);
        if (json != null && json.length() > 0) {
          if (json.has("clientId")) {
            clientId = json.getString("clientId");
          }
          if (json.has("orgId")) {
            orgId = json.getString("orgId");
          }
        }
      }
      if (!StringUtils.isEmpty(inpRecordId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", vars.getRole());
        historyData.put("UserId", vars.getUser());
        historyData.put("HeaderId", inpRecordId);

        if (rmiRevoketype.equals(Constants.REQUEST_MORE_INFORMATION)) {
          if (forwardrmi.getREQResponse().equals("REQ")) {
            historyData.put("Status", Constants.REQUEST_MORE_INFORMATION_REQUEST);
          }
          if (forwardrmi.getREQResponse().equals("RES")) {
            historyData.put("Status", Constants.REQUEST_MORE_INFORMATION_RESPONSE);
          }
          if (isOriginalUser) {
            historyData.put("NextApprover", getUserName(request.getParameter("inpToUser"))
                + getRoleName(request.getParameter("inpToRole")));
          } else {
            historyData.put("NextApprover",
                forwardrmi.getRecuser().getName()
                    + (forwardrmi.getRECRole() != null ? " / " + forwardrmi.getRECRole().getName()
                        : ""));
          }
          if (forwardrmi.getMessage() != null)
            historyData.put("Comments", forwardrmi.getMessage());
          else
            historyData.put("Comments", "");
        }
        if (rmiRevoketype.equals(Constants.REQUEST_MORE_INFORMATION_REVOKE)) {
          historyData.put("Status", Constants.REQUEST_MORE_INFORMATION_REVOKE);
          historyData.put("NextApprover", "");
          if (forwardrmi.getMessage() != null)
            historyData.put("Comments", forwardrmi.getMessage());
          else
            historyData.put("Comments", "");
        }
        ActionHistoryE e = ActionHistoryE.getColumnNames(documenttype);
        historyData.put("HistoryTable", e.getHistoryTable());
        historyData.put("HeaderColumn", e.getHeaderColumn());
        historyData.put("ActionColumn", e.getActionColumn());
        if (documenttype.equals(Constants.ENCUMBRANCE)) {
          InsertEncumbranceApprovalHistory(historyData);
        } else {
          Utility.InsertApprovalHistory(historyData);
        }
      }
    } catch (final Exception e) {
      log.error("Exception in insertActionHistoryRMI", e);
    }
  }

  public List<EutNextRoleLine> getNextRoleLine(String eutNextRoleHeader) throws Exception {

    List<EutNextRoleLine> nextrollist = null;
    try {
      OBQuery<EutNextRoleLine> nextRoleLine = OBDal.getInstance().createQuery(EutNextRoleLine.class,
          "as e where e.eUTNextRole.id=:eutNextRoleHeader and e.eUTForwardReqmoreinfo is null "
              + "and e.eUTReqmoreinfo is null ");
      nextRoleLine.setNamedParameter("eutNextRoleHeader", eutNextRoleHeader);
      if (nextRoleLine.list().size() > 0) {
        nextrollist = nextRoleLine.list();
      }

    } catch (final Exception e) {
      log.error("Exception in getNextRoleLine", e);
    }
    return nextrollist;
  }

  @Override
  public EutForwardReqMoreInfo revokeForwardUpdateRecord(String docType, VariablesSecureApp vars,
      String inpRecordId, String comments, boolean revoke) throws Exception {
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    ShipmentInOut returnTran = null;
    MaterialIssueRequest mir = null;
    String forwardRmiId = null;
    EutForwardReqMoreInfo forwardRmi = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    List<EutForwardReqMoreInfo> initialForwardList = new ArrayList<EutForwardReqMoreInfo>();
    try {
      OBContext.setAdminMode();
      if (docType.equals(Constants.BID_MANAGEMENT)) {
        bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
        forwardRmiId = bid.getEUTForwardReqmoreinfo().getId();
      } else if (docType.equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || docType.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
        forwardRmiId = purchaseReq.getEutForward().getId();
      } else if (docType.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || docType.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        forwardRmiId = proMgmt.getEUTForwardReqmoreinfo().getId();
      } else if (docType.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        forwardRmiId = purchaseOrder.getEutForward().getId();
      } else if (docType.equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        forwardRmiId = custodyTransfer.getEutForward().getId();
      } else if (docType.equals(Constants.MATERIAL_ISSUE_REQUEST)
          || docType.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || docType.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        forwardRmiId = mir.getEUTForward().getId();
      } else if (docType.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
        forwardRmiId = technicalEvlEvent.getEUTForward().getId();
      } else if (docType.equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        forwardRmiId = returnTran.getEutForward().getId();
      } else {
        forwardRmiId = revokeFinanceForwardUpdateRecord(docType, inpRecordId);
      }
      // update record in Forward_ReqMoreInfo table with status Draft 'DR'
      if (forwardRmiId != null) {
        forwardRmi = OBDal.getInstance().get(EutForwardReqMoreInfo.class, forwardRmiId);
        forwardRmi.setRevoke(true);
        forwardRmi.setStatus(Constants.DRAFT);
        forwardRmi.setProcessed(true);
        if (comments != null) {
          forwardRmi.setMessage(comments);
        } else {
          forwardRmi.setMessage("");
        }
        OBDal.getInstance().save(forwardRmi);
        OBDal.getInstance().flush();
      }
      if (revoke) {
        if (forwardRmi.getRequest() != null) {
          OBQuery<EutForwardReqMoreInfo> initialForwardObj = OBDal.getInstance().createQuery(
              EutForwardReqMoreInfo.class,
              "as e where e.request = :request and e.recordid = :recordId");
          initialForwardObj.setNamedParameter("request", forwardRmi.getRequest());
          initialForwardObj.setNamedParameter("recordId", forwardRmi.getRecordid());
          initialForwardList = initialForwardObj.list();
          if (initialForwardList != null && initialForwardList.size() > 0) {
            for (EutForwardReqMoreInfo forward : initialForwardList) {
              forward.setProcessed(true);
              forward.setStatus("DR");
              forward.setRevoke(true);
              OBDal.getInstance().save(forward);
            }
          }
        }
      }

    } catch (final Exception e) {
      log.error("Exception in insertOrUpdateRecord", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return forwardRmi;
  }

  @Override
  public String revokeFinanceForwardUpdateRecord(String docType, String inpRecordId)
      throws Exception {
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EfinBudgetManencum manencum = null;
    String forwardRmiId = null;
    EFINFundsReq fundsReq = null;
    BudgetAdjustment budgetAdjustment = null;
    EfinRDVTransaction rdv = null;
    Invoice purchaseinv = null;
    try {
      OBContext.setAdminMode();
      if (docType.equals(Constants.BUDGET)) {
        budget = OBDal.getInstance().get(EFINBudget.class, inpRecordId);
        forwardRmiId = budget.getEUTForwardReqmoreinfo().getId();
      } else if (docType.equals(Constants.BUDGET_REVISION)) {
        budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, inpRecordId);
        forwardRmiId = budgetRevision.getEUTForwardReqmoreinfo().getId();
      } else if (docType.equals(Constants.FundsReqMgmt)
          || docType.equals(Constants.FundsReqMgmt_ORG)) {
        fundsReq = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
        forwardRmiId = fundsReq.getEUTForward().getId();
      } else if (docType.equals(Constants.ENCUMBRANCE)) {
        manencum = OBDal.getInstance().get(EfinBudgetManencum.class, inpRecordId);
        forwardRmiId = manencum.getEUTForwardReqmoreinfo().getId();
      } else if (docType.equals(Constants.BUDGETADJUSTMENT)) {
        budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, inpRecordId);
        forwardRmiId = budgetAdjustment.getEUTForward().getId();
      } else if (docType.equals(Constants.RECEIPT_DELIVERY_VERIFICATION)
          || docType.equals(Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE)) {
        rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
        forwardRmiId = rdv.getEUTForwardReqmoreinfo().getId();
      } else if (docType.equals(Constants.AP_INVOICE)
          || docType.equals(Constants.PREPAYMENT_APPLICATION)
          || docType.equals(Constants.AP_PREPAYMENT_INVOICE)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
        forwardRmiId = purchaseinv.getEutForward().getId();
      }
    } catch (final Exception e) {
      log.error("Exception in revokeFinanceForwardUpdateRecord", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return forwardRmiId;
  }

  public EutForwardReqMoreInfo revokeForwardUpdateRecordRMI(String windowReference,
      VariablesSecureApp vars, String inpRecordId, String comments, boolean isrevoke,
      String documentType, boolean chkRevokeCdn, String action) throws Exception {
    String forwardRmiId = null;
    EutForwardReqMoreInfo forwardRmi = null, rmi = null, forwardObj = null;
    List<EutForwardReqMoreInfo> allRmiReqList = new ArrayList<EutForwardReqMoreInfo>();
    JSONObject delegateFromUserRoleJson = new JSONObject();
    String delegateFromRoleId = null;
    String delegateFromUserId = null;
    String delegateToUserId = null;
    String delegateToRoleId = null;
    try {
      OBContext.setAdminMode();
      rmi = getReqMoreInfo(inpRecordId, windowReference);
      forwardObj = getForwardObj(inpRecordId, windowReference);
      if (rmi != null) {
        forwardRmiId = rmi.getId();
      }
      // update record in Forward_ReqMoreInfo table with status Draft 'DR'
      if (forwardRmiId != null) {
        forwardRmi = OBDal.getInstance().get(EutForwardReqMoreInfo.class, forwardRmiId);
        forwardRmi.setRevoke(isrevoke);
        forwardRmi.setStatus(Constants.DRAFT);
        forwardRmi.setProcessed(true);
        if (comments != null) {
          forwardRmi.setMessage(comments);
        } else {
          forwardRmi.setMessage("");
        }

        if (forwardRmi.getRequest() != null) {
          EutForwardReqMoreInfo initalRmiReqObj = OBDal.getInstance()
              .get(EutForwardReqMoreInfo.class, forwardRmi.getRequest());

          delegateFromUserRoleJson = getDelegateFromUserandRole(vars.getUser(), vars.getRole(),
              documentType);
          if (delegateFromUserRoleJson != null && delegateFromUserRoleJson.length() > 0) {
            if (delegateFromUserRoleJson.has("delegatedFromRoleId")
                && delegateFromUserRoleJson.has("delegatedFromUserId")) {
              delegateFromRoleId = delegateFromUserRoleJson.getString("delegatedFromRoleId");
              delegateFromUserId = delegateFromUserRoleJson.getString("delegatedFromUserId");
            }
            if (delegateFromUserRoleJson.has("delegatedToRoleId")
                && delegateFromUserRoleJson.has("delegatedToUserId")) {
              delegateToRoleId = delegateFromUserRoleJson.getString("delegatedToRoleId");
              delegateToUserId = delegateFromUserRoleJson.getString("delegatedToUserId");
            }
          }
          /**
           * case 1: forward user if we do forward revoke when the time of rmi is done by forward
           * user case 2: rmi user or rmi delegated user trying to revoke rmi
           */
          if (chkRevokeCdn || (forwardObj != null && action.equals(Constants.FORWARD_REVOKE)
              && ((forwardObj.getUserContact().getId().equals(vars.getUser())
                  && forwardObj.getRole().getId().equals(vars.getRole()))
                  || (delegateFromUserId != null
                      && delegateFromUserId.equals(forwardObj.getUserContact().getId())
                      && delegateFromRoleId != null
                      && delegateFromRoleId.equals(forwardObj.getRole().getId()))))) {
            OBQuery<EutForwardReqMoreInfo> allRmiReqQry = OBDal.getInstance().createQuery(
                EutForwardReqMoreInfo.class,
                "as e where e.request = :request and e.recordid = :recordId");
            allRmiReqQry.setNamedParameter("request", forwardRmi.getRequest());
            allRmiReqQry.setNamedParameter("recordId", forwardRmi.getRecordid());
            allRmiReqList = allRmiReqQry.list();
            if (allRmiReqList.size() > 0) {
              for (EutForwardReqMoreInfo rmiObj : allRmiReqList) {
                rmiObj.setProcessed(true);
                rmiObj.setRevoke(isrevoke);
                rmiObj.setStatus("DR");
                OBDal.getInstance().save(rmiObj);
              }
            }
          }
          /*
           * case 3: rmi initiator / rmi initiator delegated user is trying to revoke then make the
           * processed falg as 'Y' for all record upto initial rmi case 4: if b del to d , d rmi to
           * x , x rmi to d and d rmi to y now d doing rmi revoke then d only will get response
           * button. so make the flag as 'Y' upto delegated to user
           */
          if (initalRmiReqObj != null
              && ((initalRmiReqObj.getUserContact().getId().equals(vars.getUser())
                  && initalRmiReqObj.getRole().getId().equals(vars.getRole()))
                  || (delegateFromUserId != null
                      && delegateFromUserId.equals(initalRmiReqObj.getUserContact().getId())
                      && delegateFromRoleId != null
                      && delegateFromRoleId.equals(initalRmiReqObj.getRole().getId())))) {
            OBQuery<EutForwardReqMoreInfo> allRmiReqQry = OBDal.getInstance().createQuery(
                EutForwardReqMoreInfo.class,
                "as e where e.request = :request and e.recordid = :recordId order by e.creationDate desc ");
            allRmiReqQry.setNamedParameter("request", forwardRmi.getRequest());
            allRmiReqQry.setNamedParameter("recordId", forwardRmi.getRecordid());
            allRmiReqList = allRmiReqQry.list();
            if (allRmiReqList.size() > 0) {
              for (EutForwardReqMoreInfo rmiObj : allRmiReqList) {
                rmiObj.setProcessed(true);
                rmiObj.setRevoke(isrevoke);
                rmiObj.setStatus("DR");
                OBDal.getInstance().save(rmiObj);
                if (rmiObj.getUserContact().getId().equals(delegateToUserId)
                    && rmiObj.getRole().getId().equals(delegateToRoleId)) {
                  break;
                }

              }
            }
          }
        }
        OBDal.getInstance().save(forwardRmi);
        OBDal.getInstance().flush();
      }

    } catch (final Exception e) {
      log.error("Exception in insertOrUpdateRecord", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return forwardRmi;
  }

  @Override
  public void revokeForwardDeleteEutNextRoleLine(HttpServletRequest request,
      EutForwardReqMoreInfo forwardrmi, VariablesSecureApp vars, String inpRecordId,
      String documenttype) throws Exception {
    EutNextRole eutNextRole = null;
    List<EutNextRoleLine> nextrolelinelist = new ArrayList<EutNextRoleLine>();
    String sql = "";
    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole object
        eutNextRole = getNextRole(inpRecordId, documenttype);
      }
      if (forwardrmi.getForwardRmi().equals(Constants.FORWARD)) {
        sql = " and e.eUTForwardReqmoreinfo.id =:forwardrmiId ";
      } else if (forwardrmi.getForwardRmi().equals(Constants.REQUEST_MORE_INFORMATION)) {
        sql = " and e.eUTReqmoreinfo.id =:forwardrmiId ";
      }
      OBQuery<EutNextRoleLine> nextroleline = OBDal.getInstance().createQuery(EutNextRoleLine.class,
          "as e where e.eUTNextRole.id=:eutNextRoleId " + sql);
      nextroleline.setNamedParameter("eutNextRoleId",
          eutNextRole != null ? eutNextRole.getId() : "");
      nextroleline.setNamedParameter("forwardrmiId", forwardrmi.getId());
      nextrolelinelist = nextroleline.list();
      if (nextrolelinelist.size() > 0) {
        for (EutNextRoleLine forwardedRole : nextrolelinelist) {
          if (forwardrmi.getForwardRmi().equals(Constants.FORWARD)) {
            if (forwardedRole.getEUTReqmoreinfo() == null) {
              OBDal.getInstance().remove(forwardedRole);
            } else {
              forwardedRole.setEUTForwardReqmoreinfo(null);
            }
          } else if (forwardrmi.getForwardRmi().equals(Constants.REQUEST_MORE_INFORMATION)) {
            if (forwardedRole.getEUTForwardReqmoreinfo() == null) {
              OBDal.getInstance().remove(forwardedRole);
            } else {
              forwardedRole.setEUTReqmoreinfo(null);
            }
          }
        }
      }
    } catch (final Exception e) {
      log.error("Exception in revokeForwardDeleteEutNextRoleLine", e);
    }
  }

  @Override
  public void revokeForwardDeleteEutNextRoleLineRMI(HttpServletRequest request,
      EutForwardReqMoreInfo forwardrmi, VariablesSecureApp vars, String inpRecordId,
      String windowReference) throws Exception {
    EutNextRole eutNextRole = null;
    List<EutNextRoleLine> nextrolelinelist = new ArrayList<EutNextRoleLine>();
    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole and forwardRMI object object
        eutNextRole = getNextRole(inpRecordId, windowReference);
      }
      OBQuery<EutNextRoleLine> nextroleline = OBDal.getInstance().createQuery(EutNextRoleLine.class,
          "as e where e.eUTNextRole.id=:eutNextRoleId and e.eUTReqmoreinfo.id =:forwardrmiId ");
      nextroleline.setNamedParameter("eutNextRoleId",
          eutNextRole != null ? eutNextRole.getId() : "");
      nextroleline.setNamedParameter("forwardrmiId", forwardrmi.getId());
      nextrolelinelist = nextroleline.list();
      if (nextrolelinelist.size() > 0) {
        for (EutNextRoleLine forwardedRole : nextrolelinelist) {
          if (forwardedRole.getEUTForwardReqmoreinfo() == null) {
            OBDal.getInstance().remove(forwardedRole);
          } else {
            forwardedRole.setEUTReqmoreinfo(null);
          }
        }

      }
    } catch (final Exception e) {
      log.error("Exception in revokeForwardDeleteEutNextRoleLine", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public void revokeRemoveForwardRmiFromWindows(String inpRecordId, String windowReference)
      throws Exception {
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    ShipmentInOut returnTran = null;
    MaterialIssueRequest mir = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole object
        if (windowReference.equals(Constants.BID_MANAGEMENT)) {
          bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
          bid.setEUTForwardReqmoreinfo(null);
          OBDal.getInstance().save(bid);
        } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)
            || windowReference.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
          purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
          purchaseReq.setEutForward(null);
          OBDal.getInstance().save(purchaseReq);
        } else if (windowReference.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
            || windowReference.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
          proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
          proMgmt.setEUTForwardReqmoreinfo(null);
          OBDal.getInstance().save(proMgmt);
        } else if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
          purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
          purchaseOrder.setEutForward(null);
          OBDal.getInstance().save(purchaseOrder);
        } else if (windowReference.equals(Constants.Custody_Transfer)) {
          custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
          custodyTransfer.setEutForward(null);
          OBDal.getInstance().save(custodyTransfer);
        } else if (windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST)
            || windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
            || windowReference.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
          mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
          mir.setEUTForward(null);
          OBDal.getInstance().save(mir);
        } else if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
          returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
          returnTran.setEutForward(null);
          OBDal.getInstance().save(returnTran);
        } else if (windowReference.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
          technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
          technicalEvlEvent.setEUTForward(null);
          OBDal.getInstance().save(technicalEvlEvent);
        } else {
          revokeFinanceRemoveForwardRmi(inpRecordId, windowReference);
        }
      }

    } catch (final Exception e) {
      log.error("Exception in revokeForwardDeleteEutNextRoleLine", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  @Override
  public void revokeFinanceRemoveForwardRmi(String inpRecordId, String windowReference)
      throws Exception {
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EFINFundsReq fundsReq = null;
    EfinBudgetManencum manencum = null;
    BudgetAdjustment budgetAdjustment = null;
    EfinRDVTransaction rdv = null;
    Invoice purchaseinv = null;
    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole object
        if (windowReference.equals(Constants.BUDGET)) {
          budget = OBDal.getInstance().get(EFINBudget.class, inpRecordId);
          budget.setEUTForwardReqmoreinfo(null);
          OBDal.getInstance().save(budget);
        } else if (windowReference.equals(Constants.BUDGET_REVISION)) {
          budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, inpRecordId);
          budgetRevision.setEUTForwardReqmoreinfo(null);
          OBDal.getInstance().save(budgetRevision);
        } else if (windowReference.equals(Constants.FundsReqMgmt)
            || windowReference.equals(Constants.FundsReqMgmt_ORG)) {
          fundsReq = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
          fundsReq.setEUTForward(null);
          OBDal.getInstance().save(fundsReq);
        } else if (windowReference.equals(Constants.ENCUMBRANCE)) {
          manencum = OBDal.getInstance().get(EfinBudgetManencum.class, inpRecordId);
          manencum.setEUTForwardReqmoreinfo(null);
          OBDal.getInstance().save(manencum);
        } else if (windowReference.equals(Constants.BUDGETADJUSTMENT)) {
          budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, inpRecordId);
          budgetAdjustment.setEUTForward(null);
          OBDal.getInstance().save(budgetAdjustment);
        } else if (windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION)
            || windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE)) {
          rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
          rdv.setEUTForwardReqmoreinfo(null);
          OBDal.getInstance().save(rdv);
        } else if (windowReference.equals(Constants.AP_INVOICE)
            || windowReference.equals(Constants.PREPAYMENT_APPLICATION)
            || windowReference.equals(Constants.AP_PREPAYMENT_INVOICE)) {
          purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
          purchaseinv.setEutForward(null);
          OBDal.getInstance().save(purchaseinv);
        }
      }
    } catch (final Exception e) {
      log.error("Exception in revokeFinanceRemoveForwardRmi", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  @Override
  public void giveRoleAccess(String clientId, String userId, String forwardrmiId, String doctype,
      String windowId, Connection conn) throws Exception {
    ForwardRoleAccessDAO dao = new ForwardRoleAccessDAOImpl(conn);
    try {
      int preferencecount = dao.forwardAccessPreference(clientId, forwardrmiId, windowId);
      if (preferencecount == 1) {
        log.debug("Success: all forward access given to preferences ");
      } else {
        log.debug("Failure: all forward access not given to preferences");
      }
      int windowCount = dao.forwardAccessWindow(clientId, userId, forwardrmiId, doctype);
      if (windowCount == 1) {
        log.debug("Success: all forward access given to window,form,process,from,list ");
      } else {
        log.debug("Failure: all forward access not given to window,form,process,from,list");
      }
      int checkCount = dao.forwardCheckBoxAccess(clientId, forwardrmiId);
      if (checkCount == 1) {
        log.debug("Success : all checkbox based forward access given");
      } else {
        log.debug("Failure : all checkbox based forward access not given ");
      }
    } catch (final Exception e) {
      log.error("Exception in giveRoleAccess", e);
    }
  }

  @Override
  public void removeRoleAccess(String clientId, String forwardrmiId, Connection conn)
      throws Exception {
    ForwardRoleAccessDAO dao = new ForwardRoleAccessDAOImpl(conn);
    try {
      int count = 0;
      count = dao.forwardRoleAccessRemove(clientId, forwardrmiId);
      if (count == 1) {
        log.debug("Success : all forward access was removed");
      } else {
        log.debug("Failure : all forward access was not removed");
      }

    } catch (final Exception e) {
      log.error("Exception in removeRoleAccess", e);
    }
  }

  @Override
  public void alertprocess(String clientId, String alertWindow,
      EutForwardReqMoreInfo forReqMoreInfo, String Forward_or_Revoke, String Lang, String UserName,
      String WindowName, String docType, Boolean isOriginalUser, HttpServletRequest request,
      String windowReference) throws Exception {
    EscmBidMgmt bidmgmt = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    MaterialIssueRequest mir = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    ShipmentInOut returnTran = null;
    String oldRmiId = null;
    String alterKey = null;
    String accessKey = null;
    String windowName = WindowName;
    String documentNo = null;
    String req_res = forReqMoreInfo.getREQResponse();
    String descriptionTemp = null;
    String description = null;
    String recRole = null;
    String recRoleId = null;
    String recUserId = null;
    String alertWindowType = null;
    String frwdAlertType = null, rmiAlertType = null;
    Boolean isTodelegated = false, isFinance = false;
    JSONObject result = new JSONObject();
    EutForwardReqMoreInfo oldRmi = null;
    ArrayList<ForwardAlertVO> alertUsers = new ArrayList<ForwardAlertVO>();
    try {
      OBContext.setAdminMode();
      if (isOriginalUser) {
        Role roleobj = OBDal.getInstance().get(Role.class, request.getParameter("inpToRole"));
        recRole = roleobj.getName();
        recRoleId = request.getParameter("inpToRole");
        recUserId = request.getParameter("inpToUser");
      } else {
        recRole = forReqMoreInfo.getRECRole().getName();
        recRoleId = forReqMoreInfo.getRECRole().getId();
        recUserId = forReqMoreInfo.getRecuser().getId();
      }
      // getting alert window type
      if (alertWindow.split("_").length > 1) {
        frwdAlertType = alertWindow.split("_")[0].toString();
        rmiAlertType = alertWindow.split("_")[1].toString();
        if (Forward_or_Revoke.equals(Constants.FORWARD)
            || Forward_or_Revoke.equals(Constants.FORWARD_REVOKE)) {
          alertWindowType = frwdAlertType;
        } else {
          alertWindowType = rmiAlertType;
        }
      } else {
        alertWindowType = alertWindow;
      }

      if (forReqMoreInfo.getDocruleid().equals(Constants.BID_MANAGEMENT)) {
        bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class, forReqMoreInfo.getRecordid());
        documentNo = bidmgmt.getBidno() + "-" + bidmgmt.getBidname();
        oldRmi = bidmgmt.getEUTReqmoreinfo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || forReqMoreInfo.getDocruleid().equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, forReqMoreInfo.getRecordid());
        documentNo = purchaseReq.getDocumentNo() + "-" + purchaseReq.getDescription();
        oldRmi = purchaseReq.getEutReqmoreinfo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || forReqMoreInfo.getDocruleid().equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, forReqMoreInfo.getRecordid());
        documentNo = proMgmt.getProposalno() + "-" + proMgmt.getBidName();
        oldRmi = proMgmt.getEUTReqmoreinfo();
      } else if (forReqMoreInfo.getDocruleid()
          .equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, forReqMoreInfo.getRecordid());
        documentNo = purchaseOrder.getDocumentNo()
            + ((purchaseOrder.getEscmNotes() != null && !purchaseOrder.getEscmNotes().equals("null")
                && !purchaseOrder.getEscmNotes().equals("")) ? "-" + purchaseOrder.getEscmNotes()
                    : "");
        oldRmi = purchaseOrder.getEutReqmoreinfo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class,
            forReqMoreInfo.getRecordid());
        documentNo = custodyTransfer.getDocumentNo();
        oldRmi = custodyTransfer.getEutReqmoreinfo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.MATERIAL_ISSUE_REQUEST)
          || forReqMoreInfo.getDocruleid().equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || forReqMoreInfo.getDocruleid().equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, forReqMoreInfo.getRecordid());
        documentNo = mir.getDocumentNo();
        oldRmi = mir.getEUTReqmoreinfo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, forReqMoreInfo.getRecordid());
        documentNo = returnTran.getDocumentNo();
        oldRmi = returnTran.getEutReqmoreinfo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class,
            forReqMoreInfo.getRecordid());
        documentNo = technicalEvlEvent.getEventNo();
        oldRmi = technicalEvlEvent.getEUTReqmoreinfo();
      } else {
        HashMap<String, String> alertMap = new HashMap<String, String>();
        alertMap = alertFinanceprocess(forReqMoreInfo);
        documentNo = alertMap.get("documentNo");
        oldRmiId = alertMap.get("oldRmi");
        if (oldRmiId != null)
          oldRmi = OBDal.getInstance().get(EutForwardReqMoreInfo.class, oldRmiId);
        isFinance = true;
      }
      // alert process
      ArrayList<ForwardAlertVO> includereceipient = new ArrayList<ForwardAlertVO>();
      ForwardAlertVO vo = null;
      String alertRuleId = "";

      // getting alert ruleId
      alertRuleId = getAlertRuleId(clientId, alertWindowType, isFinance);

      String alertFilter = "";
      // check selected user and role is delegated
      Date currentDate = new Date();
      isTodelegated = checkSelectedUserisDelegatedorNot(currentDate, docType,
          request.getParameter("inpToRole"), request.getParameter("inpToUser"), alertRuleId,
          alertFilter);

      result = getDelegatedUserandRole(forReqMoreInfo.getRecordid(), windowReference, recRoleId,
          recUserId, docType);

      if ((forReqMoreInfo.getForwardRmi() != null
          && forReqMoreInfo.getForwardRmi().equals(Constants.FORWARD)
          && !Forward_or_Revoke.equals(Constants.FORWARD_REVOKE))
          || (forReqMoreInfo.getREQResponse() != null
              && forReqMoreInfo.getREQResponse().equals("REQ"))) {
        if (!isOriginalUser && !isTodelegated && result.length() == 0) {
          alertFilter = " and e.alertRule.id=:alertRuleId ";
        } else if (!isOriginalUser && isTodelegated && result.length() == 0) {
          alertFilter = " and e.userContact.id=:toUserId and e.role.id=:toRoleId";
        } else if (!isOriginalUser && !isTodelegated && result.length() > 0) {
          if (result.has("delegatedRoleId") && result.has("delegatedUserId")) {
            alertFilter = " and ((e.userContact.id=:currentUserId and e.role.id=:currentRoleId)"
                + " or  (e.userContact.id=:delegatedUserId and e.role.id=:delegatedRoleId))";
          }
        } else if (isOriginalUser && (Forward_or_Revoke.equals(Constants.FORWARD)
            || Forward_or_Revoke.equals(Constants.REQUEST_MORE_INFORMATION))) {
          alertFilter = " and e.alertRule.id=:alertRuleId ";
        }
      } else if (Forward_or_Revoke.equals(Constants.FORWARD_REVOKE)) {
        alertFilter = "and (e.alertRule.id=:frwdAlertRuleId or e.alertRule.id=:rmiAlertRuleId)";
      }

      OBQuery<Alert> alertnew = OBDal.getInstance().createQuery(Alert.class,
          " as e  where e.referenceSearchKey=:PinpRecordId  and e.alertStatus='NEW'" + alertFilter);
      alertnew.setNamedParameter("PinpRecordId", forReqMoreInfo.getRecordid());

      if ((forReqMoreInfo.getForwardRmi() != null
          && forReqMoreInfo.getForwardRmi().equals(Constants.FORWARD)
          && !Forward_or_Revoke.equals(Constants.FORWARD_REVOKE))
          || (forReqMoreInfo.getREQResponse() != null
              && forReqMoreInfo.getREQResponse().equals("REQ"))) {
        if (!isOriginalUser && !isTodelegated && result.length() == 0) {
          alertnew.setNamedParameter("alertRuleId", alertRuleId);
        } else if (!isOriginalUser && isTodelegated && result.length() == 0) {
          alertnew.setNamedParameter("toUserId", request.getParameter("inpToUser"));
          alertnew.setNamedParameter("toRoleId", request.getParameter("inpToRole"));
        } else if (!isOriginalUser && !isTodelegated && result.length() > 0) {
          if (result.has("delegatedRoleId") && result.has("delegatedUserId")) {
            alertnew.setNamedParameter("currentUserId", result.get("currentUserId"));
            alertnew.setNamedParameter("currentRoleId", result.get("currentRoleId"));
            alertnew.setNamedParameter("delegatedUserId", result.get("delegatedUserId"));
            alertnew.setNamedParameter("delegatedRoleId", result.get("delegatedRoleId"));
          }
        } else if (isOriginalUser && (Forward_or_Revoke.equals(Constants.FORWARD)
            || Forward_or_Revoke.equals(Constants.REQUEST_MORE_INFORMATION))) {
          alertnew.setNamedParameter("alertRuleId", alertRuleId);
        }
      } else if (Forward_or_Revoke.equals(Constants.FORWARD_REVOKE)) {
        String rmiAlertRule = getAlertRuleId(clientId, rmiAlertType, isFinance);
        alertnew.setNamedParameter("frwdAlertRuleId", alertRuleId);
        alertnew.setNamedParameter("rmiAlertRuleId", rmiAlertRule);
      }
      log.debug("getWhereAndOrderBy:" + alertnew.getWhereAndOrderBy());
      log.debug("alertnew:" + alertnew.list().size());
      // set all the previous new status as solved even though the alertrule id's are different
      if (alertnew.list().size() > 0) {
        for (Alert alert : alertnew.list()) {
          alert.setAlertStatus("SOLVED");
          OBDal.getInstance().save(alert);
          OBDal.getInstance().flush();
        }
      }
      if (!isOriginalUser) {
        OBQuery<AlertRecipient> alertrec = OBDal.getInstance().createQuery(AlertRecipient.class,
            " as e where e.alertRule.id=:PalertRuleId ");
        alertrec.setNamedParameter("PalertRuleId", alertRuleId);

        if (alertrec.list().size() > 0) {
          for (AlertRecipient rec : alertrec.list()) {
            if (rec.getUserContact() != null) {
              OBCriteria<UserRoles> userRolesCriteria = OBDal.getInstance()
                  .createCriteria(UserRoles.class);
              userRolesCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, rec.getRole()));
              userRolesCriteria
                  .add(Restrictions.eq(AlertRecipient.PROPERTY_USERCONTACT, rec.getUserContact()));

              if (userRolesCriteria.list() != null && userRolesCriteria.list().size() > 0) {
                vo = new ForwardAlertVO(rec.getRole().getId(), rec.getUserContact().getId());
              } else {
                vo = new ForwardAlertVO(rec.getRole().getId(), "0");
              }

            } else {
              vo = new ForwardAlertVO(rec.getRole().getId(), "0");
            }
            includereceipient.add(vo);
            OBDal.getInstance().remove(rec);
          }
        }

        vo = new ForwardAlertVO(recRoleId, recUserId);
        includereceipient.add(vo);

        alertUsers.add(vo);
        // set alert for requester
        if (Forward_or_Revoke.equals(Constants.FORWARD)) {
          alterKey = "utility.forwardby";
          accessKey = "Utility.access.role";
        } else if (Forward_or_Revoke.equals(Constants.REQUEST_MORE_INFORMATION)) {
          alterKey = "utility.RequestedInformationby";
          accessKey = "Utility.access.role";
        } else if (Forward_or_Revoke.equals(Constants.REQUEST_MORE_INFORMATION_REVOKE)) {
          alterKey = "utility.RequestedInformationrevoked";
        } else {
          alterKey = "utility.revokeby";
        }

        if (alterKey.equals("utility.forwardby")) {
          descriptionTemp = sa.elm.ob.utility.properties.Resource.getProperty(alterKey, Lang) + " "
              + UserName + " " + sa.elm.ob.utility.properties.Resource.getProperty(accessKey, Lang)
              + " " + recRole;
        } else if (alterKey.equals("utility.revokeby")
            || alterKey.equals("utility.RequestedInformationrevoked")) {
          descriptionTemp = sa.elm.ob.utility.properties.Resource.getProperty(alterKey, Lang) + " "
              + UserName;
        } else {
          if (req_res.equals(Constants.REQUEST)) {
            descriptionTemp = sa.elm.ob.utility.properties.Resource.getProperty(alterKey, Lang)
                + " "
                + sa.elm.ob.utility.properties.Resource.getProperty("utility.reqby.message", Lang)
                + " " + UserName + " "
                + sa.elm.ob.utility.properties.Resource.getProperty(accessKey, Lang) + " "
                + recRole;
          } else {
            descriptionTemp = sa.elm.ob.utility.properties.Resource.getProperty(alterKey, Lang)
                + " "
                + sa.elm.ob.utility.properties.Resource.getProperty("utility.respby.message", Lang)
                + " " + UserName;
          }

        }
        description = windowName.concat(" ").concat(descriptionTemp);

        // alert for delegated users
        ArrayList<ForwardAlertVO> delegationrecipient = new ArrayList<ForwardAlertVO>();
        delegationrecipient = getDelegatedRecipients(forReqMoreInfo, documentNo, docType,
            description, alertWindowType, alterKey, isOriginalUser, request, isFinance);
        includereceipient.addAll(delegationrecipient);
        alertUsers.addAll(delegationrecipient);
        // avoid duplicate recipient
        Set<ForwardAlertVO> s = new HashSet<ForwardAlertVO>();
        s.addAll(includereceipient);
        includereceipient = new ArrayList<ForwardAlertVO>();
        includereceipient.addAll(s);

        // insert alert receipients
        for (ForwardAlertVO vo1 : includereceipient) {
          if (isFinance) {
            if (vo1.getUserId().equals("0")) {
              sa.elm.ob.finance.util.AlertUtility.insertAlertRecipient(vo1.getRoleId(), null,
                  clientId, alertWindowType);
            } else {
              sa.elm.ob.finance.util.AlertUtility.insertAlertRecipient(vo1.getRoleId(),
                  vo1.getUserId(), clientId, alertWindowType);
            }
          } else {
            if (vo1.getUserId().equals("0")) {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), null, clientId, alertWindowType);
            } else {
              AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                  alertWindowType);
            }
          }
        }
        if (isFinance) {
          sa.elm.ob.finance.util.AlertUtility.alertInsertionRole(forReqMoreInfo.getRecordid(),
              documentNo, recRoleId, recUserId, forReqMoreInfo.getClient().getId(), description,
              "NEW", alertWindowType, alterKey, Constants.GENERIC_TEMPLATE);

        } else {
          AlertUtility.alertInsertionRole(forReqMoreInfo.getRecordid(), documentNo, recRoleId,
              recUserId, forReqMoreInfo.getClient().getId(), description, "NEW", alertWindowType,
              alterKey, Constants.GENERIC_TEMPLATE);
        }
        if (forReqMoreInfo.getForwardRmi() != null && forReqMoreInfo.getForwardRmi().equals("RMI")
            && forReqMoreInfo.getREQResponse() != null
            && forReqMoreInfo.getREQResponse().equals(Constants.RESPONSE)) {
          sendAlertToByPassRMIUsers(documentNo, clientId, description, alertWindowType, alterKey,
              forReqMoreInfo, includereceipient, oldRmi, alertUsers, isFinance);
        }

      }
    } catch (final Exception e) {
      log.error("Exception in alertprocess", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  @Override
  public HashMap<String, String> alertFinanceprocess(EutForwardReqMoreInfo forReqMoreInfo)
      throws Exception {
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EFINFundsReq fundsReq = null;
    EfinBudgetManencum manencum = null;
    BudgetAdjustment budgetAdjustment = null;
    Invoice purchaseinv = null;
    EfinPropertyCompensation propComp = null;
    HashMap<String, String> alertMap = new HashMap<String, String>();
    EfinRDVTransaction rdv = null;
    try {
      OBContext.setAdminMode(true);
      alertMap.put("oldRmi", null);
      if (forReqMoreInfo.getDocruleid().equals(Constants.BUDGET)) {
        budget = OBDal.getInstance().get(EFINBudget.class, forReqMoreInfo.getRecordid());
        alertMap.put("documentNo", budget.getDocumentNo());
        if (budget.getEUTReqmoreinfo() != null)
          alertMap.put("oldRmi", budget.getEUTReqmoreinfo().getId());
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.BUDGET_REVISION)) {
        budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
            forReqMoreInfo.getRecordid());
        alertMap.put("documentNo", budgetRevision.getDocumentNo());
        if (budgetRevision.getEUTReqmoreinfo() != null)
          alertMap.put("oldRmi", budgetRevision.getEUTReqmoreinfo().getId());
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.FundsReqMgmt)
          || forReqMoreInfo.getDocruleid().equals(Constants.FundsReqMgmt_ORG)) {
        fundsReq = OBDal.getInstance().get(EFINFundsReq.class, forReqMoreInfo.getRecordid());
        alertMap.put("documentNo", fundsReq.getDocumentNo());
        if (fundsReq.getEUTReqmoreinfo() != null)
          alertMap.put("oldRmi", fundsReq.getEUTReqmoreinfo().getId());
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.ENCUMBRANCE)) {
        manencum = OBDal.getInstance().get(EfinBudgetManencum.class, forReqMoreInfo.getRecordid());
        alertMap.put("documentNo", manencum.getDocumentNo());
        if (manencum.getEUTForwardReqmoreinfo() != null)
          alertMap.put("oldRmi", manencum.getEUTForwardReqmoreinfo().getId());

      } else if (forReqMoreInfo.getDocruleid().equals(Constants.BUDGETADJUSTMENT)) {
        budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class,
            forReqMoreInfo.getRecordid());
        alertMap.put("documentNo", budgetAdjustment.getDocno());
        if (budgetAdjustment.getEUTReqmoreinfo() != null)
          alertMap.put("oldRmi", budgetAdjustment.getEUTReqmoreinfo().getId());
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.AP_INVOICE)
          || forReqMoreInfo.getDocruleid().equals(Constants.PREPAYMENT_APPLICATION)
          || forReqMoreInfo.getDocruleid().equals(Constants.AP_PREPAYMENT_INVOICE)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, forReqMoreInfo.getRecordid());
        alertMap.put("documentNo", purchaseinv.getDocumentNo());
        if (purchaseinv.getEutReqmoreinfo() != null)
          alertMap.put("oldRmi", purchaseinv.getEutReqmoreinfo().getId());
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.RECEIPT_DELIVERY_VERIFICATION)
          || forReqMoreInfo.getDocruleid()
              .equals(Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE)) {
        rdv = OBDal.getInstance().get(EfinRDVTransaction.class, forReqMoreInfo.getRecordid());
        alertMap.put("documentNo", rdv.getTXNVersion()
            + (StringUtils.isEmpty(rdv.getCertificateNo()) ? "" : "-" + rdv.getCertificateNo()));
        if (rdv.getEUTReqmoreinfo() != null)
          alertMap.put("oldRmi", rdv.getEUTReqmoreinfo().getId());
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.PROPERTY_COMP)) {
        propComp = OBDal.getInstance().get(EfinPropertyCompensation.class,
            forReqMoreInfo.getRecordid());
        alertMap.put("documentNo", propComp.getDocumentNo());
        if (propComp.getEUTReqmoreinfo() != null)
          alertMap.put("oldRmi", propComp.getEUTReqmoreinfo().getId());
      }
    } catch (final Exception e) {
      log.error("Exception in alertprocess", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return alertMap;
  }

  public HashMap<String, String> getCreatedUser(String recordId, String windowReference) {
    HashMap<String, String> createdUser = new HashMap<String, String>();
    ShipmentInOut returnTran = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    MaterialIssueRequest mir = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
      returnTran = OBDal.getInstance().get(ShipmentInOut.class, recordId);
      createdUser.put(returnTran.getCreatedBy().getId(), returnTran.getEscmAdRole().getId());
    } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)
        || windowReference.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
      purchaseReq = OBDal.getInstance().get(Requisition.class, recordId);
      createdUser.put(purchaseReq.getCreatedBy().getId(), purchaseReq.getEscmAdRole().getId());
    } else if (windowReference.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
        || windowReference.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
      proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
      createdUser.put(proMgmt.getCreatedBy().getId(), proMgmt.getRole().getId());
    } else if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
      purchaseOrder = OBDal.getInstance().get(Order.class, recordId);
      createdUser.put(purchaseOrder.getCreatedBy().getId(), purchaseOrder.getEscmAdRole().getId());
    } else if (windowReference.equals(Constants.Custody_Transfer)) {
      custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, recordId);
      createdUser.put(custodyTransfer.getCreatedBy().getId(),
          custodyTransfer.getEscmAdRole().getId());
    } else if (windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST)
        || windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
        || windowReference.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
      mir = OBDal.getInstance().get(MaterialIssueRequest.class, recordId);
      createdUser.put(mir.getCreatedBy().getId(), mir.getRole().getId());
    } else if (windowReference.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
      technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, recordId);
      createdUser.put(technicalEvlEvent.getCreatedBy().getId(),
          technicalEvlEvent.getRole().getId());
    } else {
      createdUser = getFinanceCreatedUser(recordId, windowReference);
    }
    return createdUser;
  }

  public HashMap<String, String> getFinanceCreatedUser(String recordId, String windowReference) {
    HashMap<String, String> createdUser = new HashMap<String, String>();

    BudgetAdjustment budgetAdjustment = null;
    EFINFundsReq fundsReq = null;
    EFINBudget budget = null;
    EfinBudgetTransfertrx revision = null;
    EfinBudgetManencum manEncum = null;

    if (windowReference.equals(Constants.BUDGET)) {
      budget = OBDal.getInstance().get(EFINBudget.class, recordId);
      createdUser.put(budget.getCreatedBy().getId(), budget.getRole().getId());
    } else if (windowReference.equals(Constants.BUDGET_REVISION)) {
      revision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, recordId);
      createdUser.put(revision.getCreatedBy().getId(), revision.getRole().getId());
    } else if (windowReference.equals(Constants.FundsReqMgmt)
        || windowReference.equals(Constants.FundsReqMgmt_ORG)) {
      fundsReq = OBDal.getInstance().get(EFINFundsReq.class, recordId);
      createdUser.put(fundsReq.getCreatedBy().getId(), fundsReq.getRole().getId());
    } else if (windowReference.equals(Constants.ENCUMBRANCE)) {
      manEncum = OBDal.getInstance().get(EfinBudgetManencum.class, recordId);
      createdUser.put(manEncum.getCreatedBy().getId(), manEncum.getRole().getId());
    } else if (windowReference.equals(Constants.BUDGETADJUSTMENT)) {
      budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, recordId);
      createdUser.put(budgetAdjustment.getCreatedBy().getId(), budgetAdjustment.getRole().getId());
    }

    return createdUser;
  }

  // public ShipmentInOut getCreatedUser(String recordId, String windowReference) {
  // ShipmentInOut returnTran = null;
  // @SuppressWarnings("unused")
  // String createdUser = null;
  // if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
  // returnTran = OBDal.getInstance().get(ShipmentInOut.class, recordId);
  // }
  // return returnTran;
  // }

  public String getAlertRuleId(String clientId, String alertWindowType, Boolean isFinance)
      throws Exception {
    String alertRuleId = null;
    String whereclause = "";
    try {
      OBContext.setAdminMode();
      if (isFinance) {
        whereclause = " and e.efinProcesstype=:PalertWindowType ";
      } else {
        whereclause = " and e.eSCMProcessType=:PalertWindowType ";

      }
      OBQuery<AlertRule> alertrule = OBDal.getInstance().createQuery(AlertRule.class,
          " as e where e.client.id=:PclientId " + whereclause);
      alertrule.setNamedParameter("PclientId", clientId);
      alertrule.setNamedParameter("PalertWindowType", alertWindowType);

      if (alertrule.list().size() > 0) {
        alertRuleId = alertrule.list().get(0).getId();
      }
    } catch (final Exception e) {
      log.error("Exception in getAlertRuleId", e);
    }
    return alertRuleId;
  }

  @SuppressWarnings("unused")
  public void getAlertForForwardedUser(String recordId, String alertWindowType, String alertRuleId,
      User objuser, String clientId, String action, String documentNumber, String Lang,
      String roleId, EutForwardReqMoreInfo forwardObj, String docType,
      HashMap<String, String> alertReceiversMap) {

    String description = null;
    String alterKey = null;
    boolean checkValidation = false;
    boolean isDelegated = false;
    String recRoleId = null;
    String recUserId = null;
    Date currentDate = new Date();
    JSONObject result = new JSONObject();
    boolean isFinance = false;
    try {
      OBContext.setAdminMode();

      // alert process
      ArrayList<ForwardAlertVO> includereceipient = new ArrayList<ForwardAlertVO>();
      ForwardAlertVO vo = null;

      // getting alert receipient
      OBQuery<Alert> alertnew = OBDal.getInstance().createQuery(Alert.class,
          " as e  where e.referenceSearchKey=:PinpRecordId and e.alertRule.id=:alertRuleId "
              + " and e.alertStatus='NEW' ");
      alertnew.setNamedParameter("PinpRecordId", recordId);
      alertnew.setNamedParameter("alertRuleId", alertRuleId);
      List<Alert> alertList = alertnew.list();
      // setting previous alerts to solved
      setAlertToSolved(recordId);

      /** get forward from user and role **/
      // get recent forward record
      if (forwardObj != null) {
        recRoleId = forwardObj.getRECRole().getId();
        recUserId = forwardObj.getRecuser().getId();

        if (recUserId.equals(objuser.getId()) && recRoleId.equals(roleId)) {
          checkValidation = Boolean.TRUE;
        } else {

          // check whether the current user is delegated by the receiver
          isDelegated = checkIsDelegatedUserOrRole(currentDate, docType, recRoleId, recUserId,
              roleId, objuser.getId());
          if (isDelegated) {
            checkValidation = Boolean.TRUE;
          }
        }
      }
      PrimaryKeyDocumentTypeE windowReference = null;
      ShipmentInOut returnTransaction = null;
      HashMap<String, String> createdUserRoleMap = new HashMap<String, String>();
      String rtCreatedRole = "";
      String rtCreatedUser = "";
      String rtRole = null;
      windowReference = PrimaryKeyDocumentTypeE.getWindowReference(docType);
      createdUserRoleMap = getCreatedUser(recordId, windowReference.getwindowReference());
      for (String createdUsrRole : createdUserRoleMap.keySet()) {
        rtCreatedRole = createdUserRoleMap.get(createdUsrRole);
        rtCreatedUser = createdUsrRole;
      }
      if (checkValidation) {
        // HashMap<String, String> alertReceiversMap = new HashMap<String, String>();
        String recUserIdTemp = "";
        String recRoleIdTemp = "";
        String delgaterUserId = "";
        String delgaterroleId = "";
        /*
         * if (alertList.size() > 0) { for (Alert alert : alertList) { if
         * (alert.getEutAlertKey().contains("wfa")) { if (alert.getUserContact() != null) {
         * alertReceiversMap.put(alert.getRole().getId(), alert.getUserContact().getId()); } else {
         * alertReceiversMap.put(alert.getRole().getId(), "0");
         * 
         * } } } }
         */
        recUserIdTemp = forwardObj.getUserContact().getId();
        recRoleIdTemp = forwardObj.getRole().getId();
        result = getDelegateToUserandToRole(recUserIdTemp, recRoleIdTemp, docType);
        if (result != null && result.length() > 0) {
          delgaterroleId = result.getString("delegatedToRoleId");
          delgaterUserId = result.getString("delegatedToUserId");
          alertReceiversMap.put(delgaterroleId, delgaterUserId);
        }

        alertReceiversMap.put(recRoleIdTemp, recUserIdTemp);
        if (alertReceiversMap.size() > 0) {
          for (String alertRecieve : alertReceiversMap.keySet()) {
            recUserIdTemp = alertReceiversMap.get(alertRecieve);
            recRoleIdTemp = alertRecieve;
            if (!(rtCreatedUser.equals(recUserIdTemp) || (rtCreatedRole.equals(recRoleIdTemp)))) {

              User objrecUserIdTemp = Utility.getObject(User.class, recUserIdTemp);
              Role objrecRoleIdTemp = Utility.getObject(Role.class, recRoleIdTemp);

              if (alertRecieve != null) {
                OBCriteria<UserRoles> userRolesCriteria = OBDal.getInstance()
                    .createCriteria(UserRoles.class);
                userRolesCriteria
                    .add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, objrecRoleIdTemp));
                userRolesCriteria
                    .add(Restrictions.eq(AlertRecipient.PROPERTY_USERCONTACT, objrecUserIdTemp));

                if (userRolesCriteria.list() != null && userRolesCriteria.list().size() > 0) {
                  vo = new ForwardAlertVO(recRoleIdTemp, recUserIdTemp);
                } else {
                  vo = new ForwardAlertVO(recRoleIdTemp, "0");
                }

              } else {
                vo = new ForwardAlertVO(recRoleIdTemp, "0");
              }
              includereceipient.add(vo);

              vo = new ForwardAlertVO(recRoleIdTemp, recUserIdTemp);
              includereceipient.add(vo);

              // set alert for requester
              if (action.equals(Constants.APPROVE)) {
                if (alertWindowType.equals("BM")) {
                  alterKey = "scm.BidMgmt.approved";
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();
                } else if (alertWindowType.equals("PMA")) {
                  alterKey = "scm.pm.approved";
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();
                } else if (alertWindowType.equals("PR")) {
                  alterKey = "scm.pr.approved";
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();
                } else if (alertWindowType.equals("POC")) {
                  alterKey = "scm.poc.approved";
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();
                } else if (alertWindowType.equals("CT")) {
                  alterKey = "scm.ct.approved";
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();
                } else if (alertWindowType.equals("IR")) {
                  alterKey = "scm.mir.approved";
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();
                } else if (alertWindowType.equals("SIR")) {
                  alterKey = "scm.smir.approved";
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();

                } else if (alertWindowType.equals("INR")) {
                  alterKey = "scm.Returntrans.approved";
                  // String Description = sa.elm.ob.scm.properties.Resource
                  // .getProperty("scm.Returntrans.approved", Lang) + " " + objUser.getName();
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();
                } else if (alertWindowType.equals("TEE")) {
                  alterKey = "scm.techevaluation.event.approved";
                  description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang) + " "
                      + objuser.getName();
                } else {
                  alterKey = getAlertForForwardedUserFinance(action, alertWindowType);
                  description = sa.elm.ob.finance.properties.Resource.getProperty(alterKey, Lang)
                      + " " + objuser.getName();
                  isFinance = true;
                }
              } else {
                if (action.equals(Constants.REJECT)) {
                  if (alertWindowType.equals("BM")) {
                    alterKey = "scm.BidMgmt.rejected";
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else if (alertWindowType.equals("PMA")) {
                    alterKey = "scm.pm.rejected";
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else if (alertWindowType.equals("PR")) {
                    alterKey = "scm.pr.rejected";
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else if (alertWindowType.equals("POC")) {
                    alterKey = "scm.poc.rejected";
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else if (alertWindowType.equals("CT")) {
                    alterKey = "scm.ct.rejected";
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else if (alertWindowType.equals("IR")) {
                    alterKey = " scm.materialissuerequest.rejected";
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else if (alertWindowType.equals("SIR")) {
                    alterKey = "scm.smir.rejected";
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else if (alertWindowType.equals("INR")) {
                    alterKey = "scm.Returntrans.rejected";
                    // String Description = sa.elm.ob.scm.properties.Resource
                    // .getProperty("scm.Returntrans.approved", Lang) + " " + objUser.getName();
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else if (alertWindowType.equals("TEE")) {
                    alterKey = "scm.techevaluation.event.rejected";
                    // String Description = sa.elm.ob.scm.properties.Resource
                    // .getProperty("scm.Returntrans.approved", Lang) + " " + objUser.getName();
                    description = sa.elm.ob.scm.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                  } else {
                    alterKey = getAlertForForwardedUserFinance(action, alertWindowType);
                    description = sa.elm.ob.finance.properties.Resource.getProperty(alterKey, Lang)
                        + " " + objuser.getName();
                    isFinance = true;
                  }
                }
              }

              // avoid duplicate recipient
              Set<ForwardAlertVO> s = new HashSet<ForwardAlertVO>();
              s.addAll(includereceipient);
              includereceipient = new ArrayList<ForwardAlertVO>();
              includereceipient.addAll(s);

              // insert alert receipients
              for (ForwardAlertVO vo1 : includereceipient) {
                if (isFinance) {
                  if (vo1.getUserId().equals("0")) {
                    sa.elm.ob.finance.util.AlertUtility.insertAlertRecipient(vo1.getRoleId(), null,
                        clientId, alertWindowType);
                  } else {
                    sa.elm.ob.finance.util.AlertUtility.insertAlertRecipient(vo1.getRoleId(),
                        vo1.getUserId(), clientId, alertWindowType);
                  }
                } else {
                  if (vo1.getUserId().equals("0")) {
                    AlertUtility.insertAlertRecipient(vo1.getRoleId(), null, clientId,
                        alertWindowType);
                  } else {
                    AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), clientId,
                        alertWindowType);
                  }
                }
              }
              if (isFinance) {
                if (recUserIdTemp.equals("0")) {
                  sa.elm.ob.finance.util.AlertUtility.alertInsertionRole(recordId, documentNumber,
                      recRoleIdTemp, "", clientId, description, "NEW", alertWindowType, alterKey,
                      Constants.GENERIC_TEMPLATE);
                } else {
                  sa.elm.ob.finance.util.AlertUtility.alertInsertionRole(recordId, documentNumber,
                      recRoleIdTemp, recUserIdTemp, clientId, description, "NEW", alertWindowType,
                      alterKey, Constants.GENERIC_TEMPLATE);
                }
              } else {
                if (recUserIdTemp.equals("0")) {
                  AlertUtility.alertInsertionRole(recordId, documentNumber, recRoleIdTemp, "",
                      clientId, description, "NEW", alertWindowType, alterKey,
                      Constants.GENERIC_TEMPLATE);
                } else {
                  AlertUtility.alertInsertionRole(recordId, documentNumber, recRoleIdTemp,
                      recUserIdTemp, clientId, description, "NEW", alertWindowType, alterKey,
                      Constants.GENERIC_TEMPLATE);
                }
              }

            }
          }
        }
      }
    } catch (final Exception e) {
      log.error("Exception in alertprocess", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  public void sendAlertToByPassRMIUsers(String documentNumber, String clientId, String description,
      String alertWindowType, String alterKey, EutForwardReqMoreInfo rmi,
      ArrayList<ForwardAlertVO> includereceipient, EutForwardReqMoreInfo oldRmi,
      ArrayList<ForwardAlertVO> alertUsers, boolean isFinance) {
    List<EutForwardReqMoreInfo> rmiList = new ArrayList<EutForwardReqMoreInfo>();
    ForwardAlertVO vo = null;
    ArrayList<ForwardAlertVO> rmireceipient = new ArrayList<ForwardAlertVO>();
    ArrayList<ForwardAlertVO> finalRmiReceipient = new ArrayList<ForwardAlertVO>();
    boolean isexist = false;
    boolean isalertuserexists = false;
    try {
      OBContext.setAdminMode();
      if (rmi != null && rmi.getRequest() != null) {
        OBQuery<EutForwardReqMoreInfo> rmiReqQry = OBDal.getInstance().createQuery(
            EutForwardReqMoreInfo.class,
            " as e where e.recordid=:recordId and e.rEQResponse='REQ' and (( e.request in (select req.request from  eut_forward_reqmoreinfo req  where req.id=:rmiReqId )  and e.processed='N') or (e.id=:oldRmiId))   order by created desc ");
        rmiReqQry.setNamedParameter("recordId", rmi.getRecordid());
        rmiReqQry.setNamedParameter("rmiReqId", rmi.getRequest());
        rmiReqQry.setNamedParameter("oldRmiId", oldRmi);
        rmiList = rmiReqQry.list();
        if (rmiList.size() > 0) {
          for (EutForwardReqMoreInfo rmiObj : rmiList) {
            if (!rmiObj.getUserContact().getId().equals(rmi.getRecuser().getId())
                || (rmiObj.getUserContact().getId().equals(rmi.getRecuser().getId())
                    && !rmiObj.getRole().getId().equals(rmi.getRECRole().getId()))) {
              vo = new ForwardAlertVO(rmiObj.getRole().getId(), rmiObj.getUserContact().getId());
              rmireceipient.add(vo);
            } else {
              break;
            }
          }
        }

        // avoid duplicate recipient
        Set<ForwardAlertVO> s = new HashSet<ForwardAlertVO>();
        s.addAll(rmireceipient);
        rmireceipient = new ArrayList<ForwardAlertVO>();
        rmireceipient.addAll(s);

        if (rmireceipient.size() > 0) {
          for (ForwardAlertVO rmiReceipient : rmireceipient) {
            isexist = false;
            for (ForwardAlertVO existingReceipient : includereceipient) {
              if (existingReceipient.getUserId().equals(rmiReceipient.getUserId())
                  && existingReceipient.getRoleId().equals(rmiReceipient.getRoleId())) {
                isexist = true;
              }
            }
            if (!isexist) {
              finalRmiReceipient.add(rmiReceipient);
            }
          }
        }
        for (ForwardAlertVO finalrmiReceipient : finalRmiReceipient) {
          if (isFinance) {
            sa.elm.ob.finance.util.AlertUtility.insertAlertRecipient(finalrmiReceipient.getRoleId(),
                finalrmiReceipient.getUserId(), clientId, alertWindowType);
          } else {
            AlertUtility.insertAlertRecipient(finalrmiReceipient.getRoleId(),
                finalrmiReceipient.getUserId(), clientId, alertWindowType);
          }
        }
        for (ForwardAlertVO rmiAlertReceipient : rmireceipient) {
          isalertuserexists = false;
          for (ForwardAlertVO alertRoleAndUsers : alertUsers) {
            if (alertRoleAndUsers.getUserId().equals(rmiAlertReceipient.getUserId())) {
              isalertuserexists = true;
            }
          }
          if (!isalertuserexists) {
            if (isFinance) {
              sa.elm.ob.finance.util.AlertUtility.alertInsertionRole(rmi.getRecordid(),
                  documentNumber, rmiAlertReceipient.getRoleId(), rmiAlertReceipient.getUserId(),
                  clientId, description, "NEW", alertWindowType, alterKey,
                  Constants.GENERIC_TEMPLATE);
            } else {
              AlertUtility.alertInsertionRole(rmi.getRecordid(), documentNumber,
                  rmiAlertReceipient.getRoleId(), rmiAlertReceipient.getUserId(), clientId,
                  description, "NEW", alertWindowType, alterKey, Constants.GENERIC_TEMPLATE);
            }
          }

        }
      }

    } catch (final Exception e) {
      log.error("Exception in sendAlertToByPassRMIUsers", e);
    } finally {
    }
  }

  @Override
  public String getAlertForForwardedUserFinance(String action, String alertWindowType) {
    String alertKey = null;
    try {
      OBContext.setAdminMode(true);
      if (action.equals(Constants.APPROVE)) {
        if (alertWindowType.equals("BUD")) {
          alertKey = "finance.Budget.approved";
        } else if (alertWindowType.equals(Constants.BUDGET_REVISION)) {
          alertKey = "finance.revision.approved";
        } else if (alertWindowType.equals("BD")) {
          alertKey = "finance.fundsReq.approved";
        } else if (alertWindowType.equals("ENCUM")) {
          alertKey = "finance.encumbrance.approvedby";
        } else if (alertWindowType.equals("BA")) {
          alertKey = "finance.ba.approved";
        } else if (alertWindowType.equals("RDV") || alertWindowType.equals("RDVADV")) {
          alertKey = "finance.rdv.approved";
        } else if (alertWindowType.equals("API")) {
          alertKey = "finance.purchaseinvoice.approvedby";
        } else if (alertWindowType.equals("APPI")) {
          alertKey = "finance.apprepaymentinvoice.approvedby";
        } else if (alertWindowType.equals("APPA")) {
          alertKey = "finance.apprepaymentapplication.approvedby";
        } else if (alertWindowType.equals(Constants.PROPERTY_COMP)) {
          alertKey = "finance.pc.approved";
        }
      } else if (action.equals(Constants.REJECT)) {
        if (alertWindowType.equals("BUD")) {
          alertKey = "finance.Budget.rejected";
        } else if (alertWindowType.equals(Constants.BUDGET_REVISION)) {
          alertKey = "finance.revision.rejected";
        } else if (alertWindowType.equals("BD")) {
          alertKey = "finance.fundsReq.reject";
        } else if (alertWindowType.equals("ENCUM")) {
          alertKey = "finance.encumbrance.rejected";
        } else if (alertWindowType.equals("BA")) {
          alertKey = "finance.ba.rejected";
        } else if (alertWindowType.equals("RDV") || alertWindowType.equals("RDVADV")) {
          alertKey = "efin.rdv.rejected";
        } else if (alertWindowType.equals("API")) {
          alertKey = "finance.purchaseinvoice.rejected";
        } else if (alertWindowType.equals("APPI")) {
          alertKey = "finance.apprepaymentinvoice.rejected";
        } else if (alertWindowType.equals("APPA")) {
          alertKey = "finance.apprepaymentapplication.rejected";
        } else if (alertWindowType.equals(Constants.PROPERTY_COMP)) {
          alertKey = "finance.pc.rejected";
        }
      }
    } catch (final Exception e) {
      log.error("Exception in getAlertForForwardedUserFinance", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return alertKey;
  }

  public static void setAlertToSolved(String recordId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          "as e where e.referenceSearchKey='" + recordId + "' and e.alertStatus='NEW'");
      if (alertQuery.list().size() > 0) {
        for (Alert objAlert : alertQuery.list()) {
          objAlert.setAlertStatus("SOLVED");
        }
      }
    } catch (OBException e) {
      log.error("Exception while getAlert:" + e);
      throw new OBException(e.getMessage());
    } finally {
    }
  }

  private ArrayList<ForwardAlertVO> getDelegatedRecipients(EutForwardReqMoreInfo forReqMoreInfo,
      String documentNo, String docType, String description, String alertWindowType,
      String alterKey, Boolean isOriginalUser, HttpServletRequest request, boolean isFinance) {
    ArrayList<ForwardAlertVO> delegationrecipient = new ArrayList<ForwardAlertVO>();
    try {
      OBContext.setAdminMode();
      Date currentDate = new Date();
      ForwardAlertVO vo = null;
      String recRole = null;
      String recUser = null;
      String recRoleName = "";
      String delDescription = "";
      if (isOriginalUser) {
        Role roleobj = OBDal.getInstance().get(Role.class, request.getParameter("inpToRole"));
        recRoleName = roleobj.getName();
        recRole = request.getParameter("inpToRole");
        recUser = request.getParameter("inpToUser");
      } else {
        recRoleName = forReqMoreInfo.getRECRole().getName();
        recRole = forReqMoreInfo.getRECRole().getId();
        recUser = forReqMoreInfo.getRecuser().getId();
      }

      OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
          EutDocappDelegateln.class,
          " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleID   and hd.userContact.id=:userID "
              + " and hd.fromDate <='" + currentDate + "' and hd.date >='" + currentDate
              + "' and e.documentType=:documentType");
      delegationln.setNamedParameter("roleID", recRole);
      delegationln.setNamedParameter("userID", recUser);
      delegationln.setNamedParameter("documentType", docType);
      if (delegationln != null && delegationln.list().size() > 0) {
        if (description.contains(recRoleName)) {
          delDescription = description.replace(recRoleName,
              delegationln.list().get(0).getRole().getName());
        } else {
          delDescription = description;
        }
        if (isFinance) {
          sa.elm.ob.finance.util.AlertUtility.alertInsertionRole(forReqMoreInfo.getRecordid(),
              documentNo, delegationln.list().get(0).getRole().getId(),
              delegationln.list().get(0).getUserContact().getId(),
              forReqMoreInfo.getClient().getId(), delDescription, "NEW", alertWindowType, alterKey,
              Constants.GENERIC_TEMPLATE);
        } else {
          AlertUtility.alertInsertionRole(forReqMoreInfo.getRecordid(), documentNo,
              delegationln.list().get(0).getRole().getId(),
              delegationln.list().get(0).getUserContact().getId(),
              forReqMoreInfo.getClient().getId(), delDescription, "NEW", alertWindowType, alterKey,
              Constants.GENERIC_TEMPLATE);
        }
        vo = new ForwardAlertVO(delegationln.list().get(0).getRole().getId(),
            delegationln.list().get(0).getUserContact().getId());
        delegationrecipient.add(vo);
      }
    } catch (final Exception e) {
      log.error("Exception in getDelegatedRecipients", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return delegationrecipient;

  }

  @Override
  public JSONObject getForwardFromUserFromRole(EutNextRole eutNextRole, String currentUser,
      String currentRole, String clientId) throws Exception {
    JSONObject jsonObj = new JSONObject();
    EutNextRoleLine nextRoleLnObj = null;
    List<EutNextRoleLine> nextRoleLine = new ArrayList<EutNextRoleLine>();
    EutForwardReqMoreInfo forwardObj = null;
    EutForwardReqMoreInfo initialForwardObj = null;
    List<EutForwardReqMoreInfo> forwardList = new ArrayList<EutForwardReqMoreInfo>();
    String forwardId = null;
    try {
      OBContext.setAdminMode();
      // getForwardID from eutNextroleLine
      OBQuery<EutNextRoleLine> nextRoleln = OBDal.getInstance().createQuery(EutNextRoleLine.class,
          " as e where e.eUTNextRole.id=:eutNextRoleId and e.client.id = :clientId and e.eUTForwardReqmoreinfo.id is not null ");
      nextRoleln.setNamedParameter("eutNextRoleId", eutNextRole.getId());
      nextRoleln.setNamedParameter("clientId", clientId);
      nextRoleLine = nextRoleln.list();
      if (nextRoleLine.size() > 0) {
        nextRoleLnObj = nextRoleLine.get(0);
        forwardId = nextRoleLnObj.getEUTForwardReqmoreinfo().getId();
      }
      // get initial forwardId and initial From User and To Role
      OBQuery<EutForwardReqMoreInfo> eutForward = OBDal.getInstance().createQuery(
          EutForwardReqMoreInfo.class, " as e where e.id=:forwardId and e.client.id = :clientId ");
      eutForward.setNamedParameter("forwardId", forwardId);
      eutForward.setNamedParameter("clientId", clientId);
      forwardList = eutForward.list();
      if (forwardList != null && forwardList.size() > 0) {
        forwardObj = forwardList.get(0);
        initialForwardObj = OBDal.getInstance().get(EutForwardReqMoreInfo.class,
            forwardObj.getRequest());
        jsonObj.put("fromUser", initialForwardObj.getUserContact().getId());
        jsonObj.put("fromRole", initialForwardObj.getRole().getId());

      }

    } catch (final Exception e) {
      log.error("Exception in getForwardFromUserFromRole", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return jsonObj;
  }

  @Override
  public boolean isForwardRMIIdIsNull(EutNextRole eutNextRole, String currentRole, String clientId)
      throws Exception {

    List<EutNextRoleLine> nextRoleLine = new ArrayList<EutNextRoleLine>();
    boolean checkRmiId = false;
    try {
      OBContext.setAdminMode();
      OBQuery<EutNextRoleLine> nextRoleln = OBDal.getInstance().createQuery(EutNextRoleLine.class,
          " as e where e.eUTNextRole.id=:eutNextRoleId and e.client.id=:clientId and e.role.id=:roleId and e.eUTReqmoreinfo.id is null ");
      nextRoleln.setNamedParameter("eutNextRoleId", eutNextRole.getId());
      nextRoleln.setNamedParameter("clientId", clientId);
      nextRoleln.setNamedParameter("roleId", currentRole);

      nextRoleLine = nextRoleln.list();
      if (nextRoleLine.size() > 0) {
        checkRmiId = true;
      }

    } catch (final Exception e) {
      log.error("Exception in isForwardRMIIdIsNull", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return checkRmiId;
  }

  @Override
  public void giveReqMoreInfoRoleAccess(String clientId, String userId, String forwardrmiId,
      String doctype, String windowId, Connection conn) throws Exception {
    ForwardRoleAccessDAO dao = new ForwardRoleAccessDAOImpl(conn);
    try {
      int windowCount = dao.requestMoreInfoAccessWindow(clientId, userId, forwardrmiId, doctype);
      if (windowCount == 1) {
        log.debug("Success: all request more information access given to window,process ");
      } else {
        log.debug("Failure: all request more information access not given to window,process");
      }
    } catch (final Exception e) {
      log.error("Exception in giveReqMoreInfoRoleAccess", e);
    }
  }

  @Override
  public List<Preference> getPreferences(String property, boolean isListProperty, String client,
      String org, String user, String role, String window, boolean exactMatch, boolean checkWindow,
      boolean activeFilterEnabled) throws Exception {
    List<Object> parameters = new ArrayList<Object>();
    StringBuilder hql = new StringBuilder();
    hql.append(" as p ");
    hql.append(" where ");
    if (exactMatch) {
      if (client != null) {
        hql.append(" p.visibleAtClient.id = ? ");
        parameters.add(client);
      } else {
        hql.append(" p.visibleAtClient is null");
      }
      if (org != null) {
        hql.append(" and p.visibleAtOrganization.id = ? ");
        parameters.add(org);
      } else {
        hql.append(" and p.visibleAtOrganization is null ");
      }

      if (user != null) {
        hql.append(" and p.userContact.id = ? ");
        parameters.add(user);
      } else {
        hql.append(" and p.userContact is null ");
      }

      if (role != null) {
        hql.append(" and p.visibleAtRole.id = ? ");
        parameters.add(role);
      } else {
        hql.append(" and p.visibleAtRole is null");
      }

      if (window != null) {
        hql.append(" and p.window.id = ? ");
        parameters.add(window);
      } else {
        hql.append(" and p.window is null");
      }
    } else {
      if (client != null) {
        hql.append(" (p.visibleAtClient.id = ? or ");
        parameters.add(client);
      } else {
        hql.append(" (");
      }
      hql.append(" coalesce(p.visibleAtClient, '0')='0') ");

      if (role != null) {
        hql.append(" and   (p.visibleAtRole.id = ? or ");
        parameters.add(role);
      } else {
        hql.append(" and (");
      }
      hql.append("        p.visibleAtRole is null) ");

      if (org == null) {
        hql.append("     and (coalesce(p.visibleAtOrganization, '0')='0'))");
      }

      if (user != null) {
        hql.append("  and (p.userContact.id = ? or ");
        parameters.add(user);
      } else {
        hql.append(" and (");
      }
      hql.append("         p.userContact is null) ");
      if (checkWindow) {
        if (window != null) {
          hql.append(" and  (p.window.id = ? or ");
          parameters.add(window);
        } else {
          hql.append(" and (");
        }
        hql.append("        p.window is null) ");
      }
    }

    if (property != null) {
      hql.append(" and p.propertyList = '" + (isListProperty ? "Y" : "N") + "'");
      if (isListProperty) {
        hql.append(" and p.property = ? ");
      } else {
        hql.append(" and p.attribute = ? ");
      }
      parameters.add(property);
    }

    hql.append(" order by p.id");

    OBQuery<Preference> qPref = OBDal.getInstance().createQuery(Preference.class, hql.toString());
    qPref.setParameters(parameters);
    qPref.setFilterOnActive(activeFilterEnabled);
    List<Preference> preferences = qPref.list();

    if (org != null) {
      // Remove from list organization that are not visible
      List<String> parentTree = OBContext.getOBContext().getOrganizationStructureProvider(client)
          .getParentList(org, true);
      List<Preference> auxPreferences = new ArrayList<Preference>();
      for (Preference pref : preferences) {
        if (pref.getVisibleAtOrganization() == null
            || parentTree.contains(pref.getVisibleAtOrganization().getId())) {
          auxPreferences.add(pref);
        }
      }
      return auxPreferences;
    } else {
      return preferences;
    }
  }

  @Override
  public void removeReqMoreInfoRoleAccess(String clientId, String forwardrmiId, Connection conn)
      throws Exception {
    ForwardRoleAccessDAO dao = new ForwardRoleAccessDAOImpl(conn);
    try {
      int count = 0;
      count = dao.requestMoreInforRoleAccessRemove(clientId, forwardrmiId);
      if (count == 1) {
        log.debug("Success : all forward access was removed");
      } else {
        log.debug("Failure : all forward access was not removed");
      }

    } catch (final Exception e) {
      log.error("Exception in removeReqMoreInfoRoleAccess", e);
    }
  }

  public List<RequestMoreInformationVO> getRequestDetails(String inpRecordId, String docType) {

    List<RequestMoreInformationVO> ls = new ArrayList<RequestMoreInformationVO>();
    EutForwardReqMoreInfo forwardRmiId = null, rmi = null;
    try {
      OBContext.setAdminMode();
      rmi = getReqMoreInfo(inpRecordId, docType);
      if (rmi != null) {
        forwardRmiId = OBDal.getInstance().get(EutForwardReqMoreInfo.class, rmi.getId());
        RequestMoreInformationVO rmiVO = new RequestMoreInformationVO();
        rmiVO.setUserId(Utility.nullToEmpty(forwardRmiId.getUserContact().getId()));
        rmiVO.setRoleId(Utility.nullToEmpty(forwardRmiId.getRole().getId()));
        rmiVO.setrmiRequest(Utility.nullToEmpty(forwardRmiId.getMessage()).replace("\n", "\\n")
            .replace("\r", "\\r"));
        rmiVO.setUserName(Utility.nullToEmpty(forwardRmiId.getUserContact().getName()));
        rmiVO.setRoleName(Utility.nullToEmpty(forwardRmiId.getRole().getName()));
        ls.add(rmiVO);
      }
    } catch (final Exception e) {
      log.error("Exception in getRequestDetails", e);
      return ls;
    } finally {
      OBContext.restorePreviousMode();
    }
    return ls;
  }

  public EutForwardReqMoreInfo getRmiIdReqRes(String inpRecordId, String windowReference)
      throws Exception {
    @SuppressWarnings("unused")
    EscmBidMgmt bid = null;
    EutForwardReqMoreInfo forwardreq = null;
    List<EutForwardReqMoreInfo> rmiList = null;
    try {
      OBContext.setAdminMode();

      OBQuery<EutForwardReqMoreInfo> reqRes = OBDal.getInstance().createQuery(
          EutForwardReqMoreInfo.class,
          "as e where e.status='CO' and e.recordid=:recordId and e.forwardRmi='RMI' order by e.creationDate desc limit 1 ");
      reqRes.setNamedParameter("recordId", inpRecordId);
      rmiList = reqRes.list();
      if (rmiList.size() > 0) {
        forwardreq = rmiList.get(0);
      }

    } catch (final Exception e) {
      log.error("Exception in getRmiIdReqRes", e);
    }
    return forwardreq;
  }

  public EutForwardReqMoreInfo getRmiIdReqResRMI(String inpRecordId, String windowReference)
      throws Exception {
    @SuppressWarnings("unused")
    EscmBidMgmt bid = null;
    EutForwardReqMoreInfo forwardreq = null;
    List<EutForwardReqMoreInfo> rmiList = null;
    try {
      OBContext.setAdminMode();

      OBQuery<EutForwardReqMoreInfo> reqRes = OBDal.getInstance().createQuery(
          EutForwardReqMoreInfo.class,
          "as e where e.status='DR' and e.recordid=:recordId and e.forwardRmi='RMI' order by e.creationDate desc limit 1 ");
      reqRes.setNamedParameter("recordId", inpRecordId);
      rmiList = reqRes.list();
      if (rmiList.size() > 0) {
        forwardreq = rmiList.get(0);
      }

    } catch (final Exception e) {
      log.error("Exception in getRmiIdReqRes", e);
    }
    return forwardreq;
  }

  @Override
  public JSONObject getUserDepartment(String userId) throws Exception {
    StringBuffer query = null;
    Query deptQuery = null;
    JSONObject jsob = null;
    try {
      OBContext.setAdminMode();
      jsob = new JSONObject();
      query = new StringBuffer();
      query.append(
          " select org.ad_org_id,org.name from ad_user usr join c_bpartner bp on usr.c_bpartner_id = bp.c_bpartner_id "
              + " join ad_org org on bp.EM_Ehcm_Department_Code = org.value where usr.ad_user_id = :userId ");
      deptQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
      deptQuery.setParameter("userId", userId);
      log.debug("query" + deptQuery.toString());
      if (deptQuery != null) {
        @SuppressWarnings("rawtypes")
        List deptList = deptQuery.list();
        if (deptList != null && deptList.size() > 0) {
          Object[] row = (Object[]) deptList.get(0);
          jsob.put("DeptId", row[0].toString());
          jsob.put("DeptName", row[1].toString());
        }
      }
    } catch (Exception e) {
      log.error("Exception in getUserDepartment :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsob;
  }

  @Override
  public JSONObject getUserPosition(String userId) throws Exception {
    StringBuffer query = null;
    Query posQuery = null;
    JSONObject jsob = null;
    try {
      OBContext.setAdminMode();
      jsob = new JSONObject();
      query = new StringBuffer();
      query.append(
          " select EM_Ehcm_Position from ad_user usr join c_bpartner bp on usr.c_bpartner_id = bp.c_bpartner_id "
              + " where usr.ad_user_id = :userId ");
      posQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
      posQuery.setParameter("userId", userId);
      log.debug("query" + posQuery.toString());
      if (posQuery != null) {
        @SuppressWarnings("rawtypes")
        List posList = posQuery.list();
        if (posList != null && posList.size() > 0 && posList.get(0) != null) {
          String posName = posList.get(0).toString();
          jsob.put("PositionName", posName);
        }
      }

    } catch (Exception e) {
      log.error("Exception in getUserPosition :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsob;
  }

  @Override
  public void revokeRemoveRmiFromWindows(String inpRecordId, String windowReference)
      throws Exception {
    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        // get eut_nextrole object
        setReqMoreInfoID(inpRecordId, windowReference, null);
      }

    } catch (final Exception e) {
      log.error("Exception in revokeForwardDeleteEutNextRoleLine", e);
    }
  }

  @Override
  public JSONObject isDelegatedFromToUserRole(Date currentDate, String docType, String nextRoleId,
      String nextUserId, String currentRoleId, String currentUserId) throws Exception {
    JSONObject json = null;
    StringBuffer query = null;
    Query delQuery = null;
    try {
      json = new JSONObject();
      query = new StringBuffer();
      query.append(
          "select dl.role.id,dl.userContact.id,dll.role.id,dll.userContact.id from Eut_Docapp_Delegateln dll join dll.eUTDocappDelegate dl"
              + " where dl.fromDate <=:currentDate and dl.date >=:currentDate and dll.documentType=:docType "
              + "and dl.processed='Y' ");
      if (currentUserId != null) {
        query.append("and dll.userContact.id=:currentUserId ");
      }
      if (currentRoleId != null) {
        query.append("and dll.role.id=:currentRoleId ");
      }
      if (nextUserId != null) {
        query.append("and dl.userContact.id=:hdrUserId ");
      }
      if (nextRoleId != null) {
        query.append("and dl.role.id=:hdrRoleId ");
      }
      delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delQuery.setParameter("currentDate", currentDate);
      delQuery.setParameter("docType", docType);
      if (nextUserId != null) {
        delQuery.setParameter("hdrUserId", nextUserId);
      }
      if (nextRoleId != null) {
        delQuery.setParameter("hdrRoleId", nextRoleId);
      }
      if (currentUserId != null) {
        delQuery.setParameter("currentUserId", currentUserId);
      }
      if (currentRoleId != null) {
        delQuery.setParameter("currentRoleId", currentRoleId);
      }
      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          Object[] row = (Object[]) delQuery.list().get(0);
          json.put("DelegatedFromRoleID", row[0].toString());
          json.put("DelegatedFromUserID", row[1].toString());
          json.put("DelegatedToRoleID", row[2].toString());
          json.put("DelegatedToUserID", row[3].toString());
        }
      }
    } catch (final Exception e) {
      log.error("Exception in isDelegatedFromToUserRole() ", e);
    }
    return json;
  }

  @Override
  public EutForwardReqMoreInfo getForwardObj(String inpRecordId, String windowReference)
      throws Exception {
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    ShipmentInOut returnTran = null;
    MaterialIssueRequest mir = null;
    EutForwardReqMoreInfo forwardObj = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BID_MANAGEMENT)) {
        bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
        forwardObj = bid.getEUTForwardReqmoreinfo();
      } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || windowReference.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
        forwardObj = purchaseReq.getEutForward();
      } else if (windowReference.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || windowReference.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        forwardObj = proMgmt.getEUTForwardReqmoreinfo();
      } else if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        forwardObj = purchaseOrder.getEutForward();
      } else if (windowReference.equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        forwardObj = custodyTransfer.getEutForward();
      } else if (windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST)
          || windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || windowReference.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        forwardObj = mir.getEUTForward();
      } else if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        forwardObj = returnTran.getEutForward();
      } else if (windowReference.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
        forwardObj = technicalEvlEvent.getEUTForward();
      } else {
        forwardObj = getFinanceForwardObj(inpRecordId, windowReference);
      }
    } catch (final Exception e) {
      log.error("Exception in getReqMoreInfo", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return forwardObj;
  }

  @Override
  public EutForwardReqMoreInfo getFinanceForwardObj(String inpRecordId, String windowReference)
      throws Exception {
    EFINBudget budget = null;
    EfinBudgetTransfertrx budgetRevision = null;
    EfinBudgetManencum manEncum = null;
    EutForwardReqMoreInfo forwardObj = null;
    EFINFundsReq fundsReq = null;
    BudgetAdjustment budgetAdjustment = null;
    EfinRDVTransaction rdv = null;
    Invoice purchaseinv = null;
    EfinPropertyCompensation propertyComp = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BUDGET)) {
        budget = OBDal.getInstance().get(EFINBudget.class, inpRecordId);
        forwardObj = budget.getEUTForwardReqmoreinfo();
      } else if (windowReference.equals(Constants.BUDGET_REVISION)) {
        budgetRevision = OBDal.getInstance().get(EfinBudgetTransfertrx.class, inpRecordId);
        forwardObj = budgetRevision.getEUTForwardReqmoreinfo();
      } else if (windowReference.equals(Constants.FundsReqMgmt)
          || windowReference.equals(Constants.FundsReqMgmt_ORG)) {
        fundsReq = OBDal.getInstance().get(EFINFundsReq.class, inpRecordId);
        forwardObj = fundsReq.getEUTForward();
      } else if (windowReference.equals(Constants.ENCUMBRANCE)) {
        manEncum = OBDal.getInstance().get(EfinBudgetManencum.class, inpRecordId);
        forwardObj = manEncum.getEUTForwardReqmoreinfo();
      } else if (windowReference.equals(Constants.BUDGETADJUSTMENT)) {
        budgetAdjustment = OBDal.getInstance().get(BudgetAdjustment.class, inpRecordId);
        forwardObj = budgetAdjustment.getEUTForward();
      } else if (windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION)
          || windowReference.equals(Constants.RECEIPT_DELIVERY_VERIFICATION_ADVANCE)) {
        rdv = OBDal.getInstance().get(EfinRDVTransaction.class, inpRecordId);
        forwardObj = rdv.getEUTForwardReqmoreinfo();
      } else if (windowReference.equals(Constants.AP_INVOICE)
          || windowReference.equals(Constants.PREPAYMENT_APPLICATION)
          || windowReference.equals(Constants.AP_PREPAYMENT_INVOICE)) {
        purchaseinv = OBDal.getInstance().get(Invoice.class, inpRecordId);
        forwardObj = purchaseinv.getEutForward();
      } else if (windowReference.equals(Constants.PROPERTY_COMP)) {
        propertyComp = OBDal.getInstance().get(EfinPropertyCompensation.class, inpRecordId);
        forwardObj = propertyComp.getEUTForward();
      }
    } catch (final Exception e) {
      log.error("Exception in getFinanceForwardObj", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return forwardObj;
  }

  @Override
  public EutForwardReqMoreInfo setForwardStatusAsDraft(EutForwardReqMoreInfo forwardRequestMoreInfo)
      throws Exception {
    EutForwardReqMoreInfo forwardObj = null;
    List<EutForwardReqMoreInfo> initialForwardList = new ArrayList<EutForwardReqMoreInfo>();
    try {
      OBContext.setAdminMode();
      forwardObj = forwardRequestMoreInfo;
      forwardObj.setStatus("DR");
      forwardObj.setProcessed(true);
      OBDal.getInstance().save(forwardObj);
      if (forwardObj.getRequest() != null) {
        OBQuery<EutForwardReqMoreInfo> initialForwardObj = OBDal.getInstance().createQuery(
            EutForwardReqMoreInfo.class,
            "as e where e.request = :request and e.recordid = :recordId");
        initialForwardObj.setNamedParameter("request", forwardObj.getRequest());
        initialForwardObj.setNamedParameter("recordId", forwardObj.getRecordid());
        initialForwardList = initialForwardObj.list();
        if (initialForwardList != null && initialForwardList.size() > 0) {
          for (EutForwardReqMoreInfo forward : initialForwardList) {
            forward.setProcessed(true);
            forward.setStatus("DR");
            OBDal.getInstance().save(forward);
          }
        }
      }
      OBDal.getInstance().flush();
    } catch (final Exception e) {
      log.error("Exception in getReqMoreInfo", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return forwardObj;
  }

  @Override
  public JSONObject getDefaultRole(String userId, String windowReference, String inpRecordId)
      throws Exception {
    StringBuffer query = null;
    Query defRoleQuery = null;
    JSONObject jsob = null;
    String sql = "";
    int i = 0;
    try {
      OBContext.setAdminMode();
      jsob = new JSONObject();
      query = new StringBuffer();
      Boolean haveOrgAccess = false;
      query.append(" select distinct Default_Ad_Role_ID, rol.name  from ad_user usr "
          + "join ad_user_roles usrol on usrol.ad_role_id = usr.Default_Ad_Role_ID "
          + "and  usr.ad_user_id= usrol.ad_user_id "
          + "join ad_role rol on rol.ad_role_id = usrol.ad_role_id "
          + "where usr.ad_user_id = :userId and usr.isactive='Y' and usrol.isactive='Y'  " + sql);
      defRoleQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
      defRoleQuery.setParameter("userId", userId);

      if (defRoleQuery != null) {
        @SuppressWarnings("rawtypes")
        List defRoleList = defRoleQuery.list();
        if (defRoleList != null && defRoleList.size() > 0) {
          Object[] row = (Object[]) defRoleList.get(0);
          String roleId = row[0].toString();
          haveOrgAccess = checkFrwdRmiToRoleHaveOrgAccess(inpRecordId, windowReference, roleId);
        }
        if (haveOrgAccess) {
          Object[] row = (Object[]) defRoleList.get(0);
          jsob.put("defaultRoleId", row[0].toString());
          jsob.put("defaultRoleName", row[1].toString());
        } else {
          List<UserRoles> userRoleList = null;
          OBQuery<UserRoles> userRoleObj = OBDal.getInstance().createQuery(UserRoles.class,
              " as e where e.userContact.id = :userId and e.active = true ");
          userRoleObj.setNamedParameter("userId", userId);
          userRoleList = userRoleObj.list();

          if (userRoleList.size() > 0) {
            while (!haveOrgAccess) {
              Role role = userRoleList.get(i).getRole();
              haveOrgAccess = checkFrwdRmiToRoleHaveOrgAccess(inpRecordId, windowReference,
                  role.getId());
              if (haveOrgAccess) {
                jsob.put("defaultRoleId", role.getId());
                jsob.put("defaultRoleName", role.getName());
              }
              i++;
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getDefaultRole :", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsob;
  }

  public List<Preference> getPreferenceObj(String clientId, boolean isRMI) {

    List<Preference> ls = new ArrayList<Preference>();
    try {
      OBContext.setAdminMode();
      OBQuery<Preference> preference = OBDal.getInstance().createQuery(Preference.class,
          "as e where e.searchKey = 'Y' and e.client.id = :clientId and e.property = :disable");
      preference.setNamedParameter("clientId", clientId);
      if (isRMI)
        preference.setNamedParameter("disable", "EUT_DRMI");
      else
        preference.setNamedParameter("disable", "EUT_DFWD");

      ls = preference.list();
      log.error("preference" + preference.getWhereAndOrderBy());
    } catch (final Exception e) {
      log.error("Exception in getPreferenceObj", e);
      return ls;
    } finally {
      OBContext.restorePreviousMode();
    }
    return ls;
  }

  public JSONObject getFromuserAndFromRoleWhileApprove(EutNextRole nextRoleObj, String userId,
      String roleId, String clientId, String orgId, String documentType, boolean isDummyRole,
      boolean isDirectApproval) {
    JSONObject forwardJsonObj = new JSONObject();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    String fromUser = userId;
    String fromRole = roleId;
    HashMap<String, String> frowardDelgrole = null;
    String delegatedFromRole = null;
    String delegatedToRole = null;
    String delegatedFromUserId = null;
    JSONObject result = new JSONObject();
    boolean isDirectApprover = isDirectApproval;
    try {

      /**
       * forward case (if current role and user is forwarded then for getting requesterNextRole pass
       * fromUser and fromRole)
       **/
      /**
       * check forwarder from user and roles if next role id is not null or else assign from user is
       * current userid and from role is current role
       **/
      if (nextRoleObj != null) {

        /** get forward from user and role **/
        forwardJsonObj = forwardDao.getForwardFromUserFromRole(nextRoleObj, userId, roleId,
            clientId);

        /**
         * if current user is forwarder user then assign from user is forwarder from user and role
         * is forwarder from role , before assign need to check dummay roleer is approving or not
         * based on dummy role flag
         **/
        if (forwardJsonObj != null && forwardJsonObj.length() > 0 && !isDummyRole) {

          /**
           * case1 : A-B-C ( document rule) B -- forward to D then D approving getting B role as
           * from role B user as from user
           **/

          /** assign from user and from role based on forwarder record **/
          if (forwardJsonObj.has("fromUser"))
            fromUser = forwardJsonObj.getString("fromUser");
          if (forwardJsonObj.has("fromRole"))
            fromRole = forwardJsonObj.getString("fromRole");

          /** case 1 end **/

          /**
           * if forwarder from user and role not getting assign from user is current userid and from
           * role is current role
           **/
          if (fromUser == null && fromRole == null) {
            fromUser = userId;
            fromRole = roleId;
          }
          /**
           * if got forwarder user and role , need to check record is forwarded by delegated user
           * and role , if delegated forwarder then assign from user is delegated user and from role
           * is delegated role and make isdirect approval flag as false
           **/
          else {

            // delegation
            /**
             * case1 : A-B-C ( document rule) B -- delgate to D then D forward to E now E approving
             * then checking Delegate is there based on forwarder from user , if exists then
             * checking forwarder from role equal to delegater to role making
             * isDirectApproval="false"
             */
            /** check current user is delegated user or not **/
            //
            // frowardDelgrole = NextRoleByRule.getDelegatedFromAndToRolesandUsers(clientId, orgId,
            // fromUser, documentType, null);
            // /** if current user is delegater then making isdirectapproval flag as "false" **/
            // if (frowardDelgrole.size() > 0 && frowardDelgrole != null) {
            // delegatedFromRole = frowardDelgrole.get("FromUserRoleId");
            // delegatedToRole = frowardDelgrole.get("ToUserRoleId");
            //
            // if (delegatedToRole != null && delegatedToRole.equals(fromRole)) {
            // isDirectApprover = false;
            // }
            // }

            // delegation
            /**
             * case 3: A-B-C --> A approved now Waiting for approver of B and B- forward to X and X
             * Forward to C and C is delegated to D then D approving finding original from user and
             * role that is B and B role and making direct approval flag "true" .
             **/
            /**
             * Case 4: A-B-C --> -A Approved , B approved now Waiting for approver of C and C
             * forward to X and X Forward to B and B is delegated to D then D approving finding
             * original from user and role that is C and C role and making direct approval flag
             * "true"
             **/
            /** if forwarder user is delegated to some one and delegated user is approving **/
            if (!isDirectApprover) {
              frowardDelgrole = NextRoleByRule.getDelegatedFromAndToRolesandUsers(clientId, orgId,
                  userId, documentType, null);
              if (frowardDelgrole != null && frowardDelgrole.size() > 0) {
                delegatedFromRole = frowardDelgrole.get("FromUserRoleId");
                delegatedToRole = frowardDelgrole.get("ToUserRoleId");
                delegatedFromUserId = frowardDelgrole.get("FromUserId");

                if (delegatedToRole != null && delegatedFromRole != null) {
                  if (nextRoleObj.getEutNextRoleLineList().size() > 0) {
                    for (EutNextRoleLine lineObj : nextRoleObj.getEutNextRoleLineList()) {
                      if (lineObj.getEUTForwardReqmoreinfo() != null
                          && lineObj.getRole().getId().equals(delegatedFromRole)
                          && lineObj.getUserContact() != null
                          && lineObj.getUserContact().getId().equals(delegatedFromUserId)) {
                        isDirectApprover = true;

                      }
                    }
                  }
                }
              }
            }

          }

        } else {

          // delegation
          /**
           * case 3: A-B-C --> B- forward to C and C is delegated to D then D approving finding
           * original from user and role that is B and B role and making direct approval flag "true"
           **/
          /** if forwarder user is delegated to some one and delegated user is approving **/
          frowardDelgrole = NextRoleByRule.getDelegatedFromAndToRolesandUsers(clientId, orgId,
              userId, documentType, null);
          if (frowardDelgrole != null && frowardDelgrole.size() > 0) {
            delegatedFromRole = frowardDelgrole.get("FromUserRoleId");
            delegatedToRole = frowardDelgrole.get("ToUserRoleId");
            delegatedFromUserId = frowardDelgrole.get("FromUserId");

            if (delegatedToRole != null && delegatedFromRole != null) {

              /**
               * get forwarder record based on delegated from user and delegated from role and
               * asssign from role forward from role and from user is forward user
               **/
              forwardJsonObj = forwardDao.getForwardFromUserFromRole(nextRoleObj,
                  delegatedFromUserId, delegatedFromRole, clientId);
              if (forwardJsonObj != null && forwardJsonObj.length() > 0 && !isDummyRole) {
                if (forwardJsonObj.has("fromUser"))
                  fromUser = forwardJsonObj.getString("fromUser");
                if (forwardJsonObj.has("fromRole"))
                  fromRole = forwardJsonObj.getString("fromRole");

                if (fromUser == null && fromRole == null) {
                  fromUser = userId;
                  fromRole = roleId;
                } else {
                  isDirectApprover = true;
                }

              } else {
                fromUser = userId;
                fromRole = roleId;
              }
            }
          } else {
            fromUser = userId;
            fromRole = roleId;
          }
        }
      }
      if (fromUser != null) {
        result.put("fromUser", fromUser);
      } else {
        result.put("fromUser", userId);
      }
      if (fromRole != null) {
        result.put("fromRole", fromRole);
      } else {
        result.put("fromUser", roleId);
      }
      result.put("isDirectApproval", isDirectApprover);
      return result;

    }

    catch (

    final Exception e) {
      log.error("Exception in getPreferenceObj", e);
    } finally {
    }
    return result;
  }

  @Override
  public String getWindowTranslationName(Window windowObj, String lang) throws Exception {
    String windowName = null;
    List<WindowTrl> windowTrnlist = null;
    try {
      OBContext.setAdminMode();

      if (windowObj != null) {
        windowTrnlist = windowObj.getADWindowTrlList();
        if (windowTrnlist.size() > 0) {
          for (WindowTrl windowTrlObj : windowTrnlist) {
            if (windowTrlObj.getLanguage().getLanguage().equals(lang)) {
              windowName = windowTrlObj.getName();
              break;
            }
          }
        } else {
          windowName = windowObj.getName();
        }
      }

    } catch (Exception e) {
      log.error("Exception in getDefaultRole :", e);
    } finally {
    }
    return windowName;
  }

  @Override
  public Boolean isRmiRevoked(String inpRecordId, String clientId, String cUserId, String cRoleId,
      String orgId, String docType) throws Exception {
    Boolean isRevoked = false;
    try {
      OBContext.setAdminMode();
      List<EutForwardReqMoreInfo> rmiList = null;
      EutForwardReqMoreInfo forwardreq = null;
      HashMap<String, String> forwardDelgrole = null;
      String delegatedFromUserId = null, delegatedFromRole = null;

      OBQuery<EutForwardReqMoreInfo> rmiobj = OBDal.getInstance().createQuery(
          EutForwardReqMoreInfo.class,
          " as e where e.recordid=:recordID and e.forwardRmi='RMI' order by updated desc ");
      rmiobj.setNamedParameter("recordID", inpRecordId);
      rmiobj.setMaxResult(1);
      rmiList = rmiobj.list();
      if (rmiList.size() > 0) {
        forwardreq = rmiList.get(0);
      }
      if (rmiobj != null && forwardreq != null) {
        if (forwardreq.getStatus().equals("DR") && forwardreq.getREQResponse().equals("REQ")
            && forwardreq.getRecuser().getId().equals(cUserId)
            && forwardreq.getRECRole().getId().equals(cRoleId)) {
          isRevoked = true;
        } else {
          forwardDelgrole = NextRoleByRule.getDelegatedFromAndToRolesandUsers(clientId, orgId,
              cUserId, docType, null);
          delegatedFromRole = forwardDelgrole.get("FromUserRoleId");
          delegatedFromUserId = forwardDelgrole.get("FromUserId");
          if (forwardreq.getStatus().equals("DR") && forwardreq.getREQResponse().equals("REQ")
              && forwardreq.getRecuser().getId().equals(delegatedFromUserId)
              && forwardreq.getRECRole().getId().equals(delegatedFromRole)) {
            isRevoked = true;
          }
        }

      }
    } catch (Exception e) {
      log.error("Exception in isRmiRevoked :", e);
    } finally {
    }
    return isRevoked;
  }

  @Override
  public JSONObject getFrowardedOrDelegatedFromUserFromRole(String recordId, String clientId,
      String doctype, String orgId) throws Exception {
    List<EutForwardReqMoreInfo> forwardlist = new ArrayList<EutForwardReqMoreInfo>();
    EutForwardReqMoreInfo forwardRecentObj = null;
    HashMap<String, String> frowardDelgrole = null;
    String delegatedFromRole = null;
    String delegatedToRole = null;
    String delegatedFromUserId = null;
    String fromUser = null;
    String fromRole = null;
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();
      // get recent forward record
      OBQuery<EutForwardReqMoreInfo> forwardobj = OBDal.getInstance().createQuery(
          EutForwardReqMoreInfo.class,
          " as e where e.recordid=:recordId and e.forwardRmi='F' and e.client.id=:clientId order by creationDate desc ");

      forwardobj.setNamedParameter("recordId", recordId);
      forwardobj.setNamedParameter("clientId", clientId);
      forwardobj.setMaxResult(1);
      forwardlist = forwardobj.list();
      if (forwardlist.size() > 0) {
        forwardRecentObj = forwardlist.get(0);
        // get from user and from role
        fromUser = forwardRecentObj.getUserContact().getId();
        fromRole = forwardRecentObj.getRole().getId();
      }
      if (forwardRecentObj != null) {
        if (forwardRecentObj.getUserContact() != null) {
          /** check from user is delegated user or not **/
          frowardDelgrole = NextRoleByRule.getDelegatedFromAndToRolesandUsers(clientId, orgId,
              fromUser, doctype, null);
          /** if current user is delegater **/
          if (frowardDelgrole != null) {
            delegatedFromRole = frowardDelgrole.get("FromUserRoleId");
            delegatedToRole = frowardDelgrole.get("ToUserRoleId");
            delegatedFromUserId = frowardDelgrole.get("FromUserId");

            if (delegatedToRole != null
                && delegatedToRole.equals(forwardRecentObj.getRole().getId())) {
              // get delegate from user and from role
              fromRole = delegatedFromRole;
              fromUser = delegatedFromUserId;
            }
          }
        }
      }
      result.put("fromUser", fromUser);
      result.put("fromRole", fromRole);
    } catch (final Exception e) {
      log.error("Exception in getforwardRecentRecordObj", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  @Override
  public Boolean responsePreValidation(String inpRecordId, String clientId, String userId,
      String roleId, String orgId, String docType, String windowReference, Boolean isResponse)
      throws Exception {
    Boolean checkValidation = Boolean.FALSE, isDelegated = Boolean.FALSE;
    try {
      OBContext.setAdminMode();
      EutNextRole eutNextRole = null;
      List<EutNextRoleLine> nextRoleLine = null;
      Date currentDate = new Date();
      String nxtRoleId = null, nxtUserId = null;
      String recUserId = null, recRoleId = null;
      EutForwardReqMoreInfo reqMoreinfo = getReqMoreInfo(inpRecordId, windowReference);
      EutForwardReqMoreInfo forwardObj = getForwardObj(inpRecordId, windowReference);
      if (reqMoreinfo != null) {
        recUserId = reqMoreinfo.getRecuser().getId();
        recRoleId = reqMoreinfo.getRECRole().getId();

        // allow if the current user is RMI receiver or the delegated by the receiver
        if (recUserId.equals(userId) && recRoleId.equals(roleId)) {
          checkValidation = Boolean.TRUE;
        } else {
          // check whether the current user is delegated by the receiver
          isDelegated = checkIsDelegatedUserOrRole(currentDate, docType, recRoleId, recUserId,
              roleId, userId);
          if (isDelegated) {
            checkValidation = Boolean.TRUE;
          }
        }
      } else {
        if (isResponse) {
          checkValidation = Boolean.FALSE;
        } else if (forwardObj != null) {
          recUserId = forwardObj.getRecuser().getId();
          recRoleId = forwardObj.getRECRole().getId();
          // allow if the current user is forward ToUser
          if (recUserId.equals(userId) && recRoleId.equals(roleId)) {
            checkValidation = Boolean.TRUE;
          } else {
            // check whether the current user is delegated by the forward ToUser
            isDelegated = checkIsDelegatedUserOrRole(currentDate, docType, recRoleId, recUserId,
                roleId, userId);
            if (isDelegated) {
              checkValidation = Boolean.TRUE;
            }
          }
        } else {
          // getting eutNextRole
          eutNextRole = getNextRole(inpRecordId, windowReference);
          if (eutNextRole != null) {
            // getting eutNextRoleLine
            nextRoleLine = getNextRoleLine(eutNextRole.getId());
            for (EutNextRoleLine nxtRoleLine : nextRoleLine) {
              nxtRoleId = nxtRoleLine.getRole().getId();
              if (nxtRoleLine.getUserContact() != null)
                nxtUserId = nxtRoleLine.getUserContact().getId();
              // check whether the current role is next approver
              if (roleId.equals(nxtRoleLine.getRole().getId())) {
                if (nxtUserId == null || (nxtUserId != null && nxtUserId.equals(userId)))
                  checkValidation = Boolean.TRUE;
              } else {
                // check whether the current user is delegated by the next approver
                isDelegated = checkIsDelegatedUserOrRole(currentDate, docType, nxtRoleId, nxtUserId,
                    roleId, userId);
                if (isDelegated) {
                  checkValidation = Boolean.TRUE;
                }
              }
            }
          }
        }
      }
    } catch (final Exception e) {
      log.error("Exception in responsePreValidation", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return checkValidation;
  }

  @Override
  public Boolean checkIsDelegatedUserOrRole(Date currentDate, String docType, String nextRoleId,
      String nextUserId, String currentRoleId, String currentUserId) throws Exception {
    boolean allowDelegation = Boolean.FALSE;
    StringBuffer query = null;
    Query delQuery = null;
    try {
      query = new StringBuffer();
      query.append("select dll.role.id from Eut_Docapp_Delegateln dll join dll.eUTDocappDelegate dl"
          + " where dl.fromDate <=:currentDate and dl.date >=:currentDate and dll.documentType=:docType "
          + "and dl.processed='Y' ");
      if (currentUserId != null) {
        query.append("and dll.userContact.id=:currentUserId ");
      }
      if (currentRoleId != null) {
        query.append("and dll.role.id=:currentRoleId ");
      }
      if (nextUserId != null) {
        query.append("and dl.userContact.id=:hdrUserId ");
      }
      if (nextRoleId != null) {
        query.append("and dl.role.id=:hdrRoleId ");
      }
      delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delQuery.setParameter("currentDate", currentDate);
      delQuery.setParameter("docType", docType);
      if (nextUserId != null) {
        delQuery.setParameter("hdrUserId", nextUserId);
      }
      if (nextRoleId != null) {
        delQuery.setParameter("hdrRoleId", nextRoleId);
      }
      if (currentUserId != null) {
        delQuery.setParameter("currentUserId", currentUserId);
      }
      if (currentRoleId != null) {
        delQuery.setParameter("currentRoleId", currentRoleId);
      }
      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          allowDelegation = Boolean.TRUE;
        }
      }
    } catch (final Exception e) {
      log.error("Exception in checkIsDelegatedUserOrRole() ", e);
    }
    return allowDelegation;
  }

  public Boolean checkFwdandRmiDelegatedUserOrRole(Date currentDate, String docType,
      String nextRoleId, String nextUserId, String currentRoleId, String currentUserId)
      throws Exception {
    boolean allowDelegation = Boolean.FALSE;
    StringBuffer query = null;
    Query delQuery = null;
    try {
      query = new StringBuffer();
      query.append("select dll.role.id from Eut_Docapp_Delegateln dll join dll.eUTDocappDelegate "
          + "dl where dl.fromDate <=:currentDate and dl.date >=:currentDate and "
          + "dll.documentType=:docType and dl.processed='Y' and ((dll.userContact.id=:currentUserId "
          + "and dll.role.id=:currentRoleId and dl.userContact.id=:hdrUserId )"
          + "or  (dl.userContact.id=:currentUserId  and dl.role.id=:currentRoleId and "
          + "dll.userContact.id=:hdrUserId   )) ");

      delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delQuery.setParameter("currentDate", currentDate);
      delQuery.setParameter("docType", docType);
      if (nextUserId != null) {
        delQuery.setParameter("hdrUserId", nextUserId);
      }
      if (nextRoleId != null) {
        delQuery.setParameter("hdrRoleId", nextRoleId);
      }
      if (currentUserId != null) {
        delQuery.setParameter("currentUserId", currentUserId);
      }
      if (currentRoleId != null) {
        delQuery.setParameter("currentRoleId", currentRoleId);
      }
      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          allowDelegation = Boolean.TRUE;
        }
      }
    } catch (final Exception e) {
      log.error("Exception in checkIsDelegatedUserOrRole() ", e);
    }
    return allowDelegation;
  }

  @Override
  public Boolean forwardPreValidation(String inpRecordId, String clientId, String userId,
      String roleId, String orgId, String docType, String windowReference) throws Exception {

    Boolean checkValidation = Boolean.FALSE, isDelegated = Boolean.FALSE;
    try {
      OBContext.setAdminMode();
      EutNextRole eutNextRole = null;
      List<EutNextRoleLine> nextRoleLine = null;
      Date currentDate = new Date();
      String nxtRoleId = null, nxtUserId = null;
      String recUserId = null, recRoleId = null;
      EutForwardReqMoreInfo forwardObj = getForwardObj(inpRecordId, windowReference);
      if (forwardObj != null) {
        recUserId = forwardObj.getRecuser().getId();
        recRoleId = forwardObj.getRECRole().getId();
        // allow if the current user is forward ToUser
        if (recUserId.equals(userId) && recRoleId.equals(roleId)) {
          checkValidation = Boolean.TRUE;
        } else {
          // check whether the current user is delegated by the forward ToUser
          isDelegated = checkIsDelegatedUserOrRole(currentDate, docType, recRoleId, recUserId,
              roleId, userId);
          if (isDelegated) {
            checkValidation = Boolean.TRUE;
          }
        }
      } else {
        // getting eutNextRole
        eutNextRole = getNextRole(inpRecordId, windowReference);
        if (eutNextRole != null) {
          // getting eutNextRoleLine
          nextRoleLine = getNextRoleLine(eutNextRole.getId());
          for (EutNextRoleLine nxtRoleLine : nextRoleLine) {
            nxtRoleId = nxtRoleLine.getRole().getId();
            if (nxtRoleLine.getUserContact() != null)
              nxtUserId = nxtRoleLine.getUserContact().getId();
            // check whether the current role is next approver
            if (roleId.equals(nxtRoleLine.getRole().getId())) {
              checkValidation = Boolean.TRUE;
            } else {
              // check whether the current user is delegated by the next approver
              isDelegated = checkIsDelegatedUserOrRole(currentDate, docType, nxtRoleId, nxtUserId,
                  roleId, userId);
              if (isDelegated) {
                checkValidation = Boolean.TRUE;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in forwardPreValidation", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return checkValidation;
  }

  @Override
  public Boolean allowApproveReject(EutForwardReqMoreInfo forwardId, String userId, String roleId,
      String docType) throws Exception {
    Boolean allowAppRej = Boolean.FALSE, isDelegated = Boolean.FALSE;
    try {
      String recUserId = forwardId.getRecuser().getId();
      String recRoleId = forwardId.getRECRole().getId();
      Date currentDate = new Date();

      // check whether the current user is the receiver
      if (recUserId.equals(userId) && recRoleId.equals(roleId)) {
        allowAppRej = Boolean.TRUE;
      } else {

        // check whether the current user is delegated by the receiver
        isDelegated = checkIsDelegatedUserOrRole(currentDate, docType, recRoleId, recUserId, roleId,
            userId);
        if (isDelegated) {
          allowAppRej = Boolean.TRUE;
        }
      }
    } catch (final Exception e) {
      log.error("Exception in allowApproveReject", e);
    }
    return allowAppRej;
  }

  @Override
  public String checkIsNestedForwardRmi(String recordId, String forwardRmiId) throws Exception {
    String isNestedId = null;
    StringBuffer query = null;
    Query delQuery = null;
    try {
      query = new StringBuffer();
      query.append(
          "SELECT recuser.id,isrevoke FROM eut_forward_reqmoreinfo  WHERE recordid=:recordId AND id<>:forwardRmiId  ORDER BY created DESC");
      delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delQuery.setParameter("recordId", recordId);
      delQuery.setParameter("forwardRmiId", forwardRmiId);
      delQuery.setMaxResults(1);
      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          Object[] row = (Object[]) delQuery.list().get(0);
          if (row[1].toString().equals("false"))
            isNestedId = (String) row[0].toString();
        }
      }
    } catch (final Exception e) {
      log.error("Exception in checkIsNestedForwardOrRmi() ", e);
    }
    return isNestedId;
  }

  @Override
  public Boolean checkFrwdRmiToRoleHaveOrgAccess(String inpRecordId, String windowReference,
      String roleId) throws Exception {
    Boolean haveOrgAccess = Boolean.FALSE;
    JSONObject json = null;
    String clientId = null;
    String orgId = null;
    try {
      OBContext.setAdminMode();
      if (inpRecordId != null) {
        json = getClientIdOrgId(inpRecordId, windowReference);
        if (json != null && json.length() > 0) {
          if (json.has("clientId")) {
            clientId = json.getString("clientId");
          }
          if (json.has("orgId")) {
            orgId = json.getString("orgId");
          }
        }
        if (orgId.equals("0")) {
          haveOrgAccess = Boolean.TRUE;
        } else {
          OBQuery<RoleOrganization> roleOrg = OBDal.getInstance().createQuery(
              RoleOrganization.class,
              " as e where e.role.id=:roleID and e.organization.id=:orgID and e.client.id=:clientID");
          roleOrg.setNamedParameter("roleID", roleId);
          roleOrg.setNamedParameter("clientID", clientId);
          roleOrg.setNamedParameter("orgID", orgId);
          if (roleOrg.list().size() > 0) {
            haveOrgAccess = Boolean.TRUE;
          }
        }
      }
    } catch (final Exception e) {
      log.error("Exception in isFrwdRmiToRoleHaveOrgAccess", e);
    }
    return haveOrgAccess;
  }

  @Override
  public String checkTransactionValidation(String windowReference, String inpRecordId)
      throws Exception {
    Order purchaseOrder = null;
    String message = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);

        // Net Total amount should not be zero
        if (purchaseOrder.getGrandTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
          message = "@ESCM_OrderAmount_Not_Zero@";
        }
      }
    } catch (final Exception e) {
      log.error("Exception in checkTransactionValidation", e);

    }

    return message;
  }

  @Override
  public Boolean checkSelectedUserisDelegatedorNot(Date currentDate, String docType,
      String currentRoleId, String currentUserId, String delegatedFromRole,
      String delegatedFromUser) throws Exception {
    boolean allowDelegation = Boolean.FALSE;
    StringBuffer query = null;
    Query delQuery = null;
    try {
      query = new StringBuffer();
      query.append("select dll.role.id from Eut_Docapp_Delegateln dll join dll.eUTDocappDelegate "
          + "dl where dl.fromDate <=:currentDate and dl.date >=:currentDate and "
          + "dll.documentType=:docType and dl.processed='Y' and (dll.userContact.id=:currentUserId "
          + "and dll.role.id=:currentRoleId)  ");

      delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delQuery.setParameter("currentDate", currentDate);
      delQuery.setParameter("docType", docType);

      delQuery.setParameter("currentUserId", currentUserId);

      delQuery.setParameter("currentRoleId", currentRoleId);

      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          allowDelegation = Boolean.TRUE;
        }
      }
    } catch (final Exception e) {
      log.error("Exception in checkSelectedUserisDelegatedorNot() ", e);
    }
    return allowDelegation;
  }

  public String getUserName(String UserId) {
    String userName = null;
    try {
      OBContext.setAdminMode();
      User userObj = OBDal.getInstance().get(User.class, UserId);
      userName = userObj.getName();

    } catch (final Exception e) {
      log.error("Exception in getUserName  ", e);
    }
    return userName;
  }

  public String getRoleName(String roleId) {
    String roleName = "";
    try {
      OBContext.setAdminMode();
      Role roleObj = OBDal.getInstance().get(Role.class, roleId);
      if (roleObj != null) {
        roleName = " / " + roleObj.getName();
      }

    } catch (final Exception e) {
      log.error("Exception in getRoleName  ", e);
    }
    return roleName;
  }

  @Override
  public String getOriginalNextRoleLine(String eutNextRoleId, String clientId) throws Exception {
    String fromRoleId = null;
    List<EutNextRoleLine> roleLineList = new ArrayList<EutNextRoleLine>();
    try {
      OBContext.setAdminMode();
      OBQuery<EutNextRoleLine> roleOrg = OBDal.getInstance().createQuery(EutNextRoleLine.class,
          " as e where e.eUTNextRole.id=:eutNextRoleId and e.client.id=:clientId and e.eUTForwardReqmoreinfo.id is null and e.eUTReqmoreinfo.id is null ");
      roleOrg.setNamedParameter("eutNextRoleId", eutNextRoleId);
      roleOrg.setNamedParameter("clientId", clientId);
      roleLineList = roleOrg.list();
      if (roleLineList != null && roleLineList.size() > 0) {
        fromRoleId = roleLineList.get(0).getRole().getId();
      }

    } catch (final Exception e) {
      log.error("Exception in getOriginalNextRoleLine", e);
    }

    return fromRoleId;
  }

  public JSONObject getDelegatedUserandRole(String inpRecordId, String windowReference,
      String roleId, String userId, String docType) {
    EutNextRole eutNextRole = null;
    List<EutNextRoleLine> nextRoleLine = null;
    String nxtUserId = null;
    JSONObject result = new JSONObject();
    Date currentDate = new Date();
    StringBuffer query = null;
    Query delQuery = null;
    query = new StringBuffer();

    try {
      OBContext.setAdminMode();
      eutNextRole = getNextRole(inpRecordId, windowReference);
      if (eutNextRole != null) {
        // getting eutNextRoleLine
        nextRoleLine = getNextRoleLine(eutNextRole.getId());
        for (EutNextRoleLine nxtRoleLine : nextRoleLine) {
          if (nxtRoleLine.getUserContact() != null)
            nxtUserId = nxtRoleLine.getUserContact().getId();
          // check whether the current role is next approver
          if (roleId.equals(nxtRoleLine.getRole().getId())
              && (nxtUserId == null || (nxtUserId != null && userId.equals(nxtUserId)))) {
            query.append(
                "select dll.role.id,dll.userContact.id from Eut_Docapp_Delegateln dll join dll.eUTDocappDelegate "
                    + "dl where dl.fromDate <=:currentDate and dl.date >=:currentDate and "
                    + "dll.documentType=:docType and dl.processed='Y' and (dl.userContact.id=:currentUserId "
                    + "and dl.role.id=:currentRoleId)  ");

            delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
            delQuery.setParameter("currentDate", currentDate);
            delQuery.setParameter("docType", docType);

            delQuery.setParameter("currentUserId", userId);

            delQuery.setParameter("currentRoleId", roleId);

            if (delQuery != null) {
              if (delQuery.list().size() > 0) {
                Object[] row = (Object[]) delQuery.list().get(0);
                result.put("delegatedRoleId", row[0]);
                result.put("delegatedUserId", row[1]);
                result.put("currentUserId", userId);
                result.put("currentRoleId", roleId);
              }
            }

          }
        }
      }

    } catch (final Exception e) {
      log.error("Exception in getDelegatedUserandRole", e);
    }
    return result;

  }

  public JSONObject getDelegateToUserandToRole(String userId, String roleId, String documentType) {
    Date currentDate = new Date();
    String query = null;
    Query delQuery = null;
    JSONObject result = new JSONObject();
    try {

      query = "select dll.role.id,dll.userContact.id from Eut_Docapp_Delegateln dll join dll.eUTDocappDelegate "
          + "dl where dl.fromDate <=:currentDate and dl.date >=:currentDate and "
          + "dll.documentType=:docType and dl.processed='Y' ";
      if (userId != null && userId != "0") {
        query += " and dl.userContact.id=:currentUserId  and dl.role.id=:currentRoleId ";
      } else {
        query += "  and dl.role.id=:currentRoleId ";
      }

      delQuery = OBDal.getInstance().getSession().createQuery(query);
      delQuery.setParameter("currentDate", currentDate);
      delQuery.setParameter("docType", documentType);
      if (userId != null && userId != "0") {
        delQuery.setParameter("currentUserId", userId);
        delQuery.setParameter("currentRoleId", roleId);
      } else {
        delQuery.setParameter("currentRoleId", roleId);
      }

      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          Object[] row = (Object[]) delQuery.list().get(0);
          if (row[0] != null)
            result.put("delegatedToRoleId", row[0]);
          if (row[1] != null)
            result.put("delegatedToUserId", row[1]);

        }
      }
    } catch (final Exception e) {
      log.error("Exception in getDelegateToUserandToRole", e);
    }
    return result;

  }

  @Override
  public EutForwardReqMoreInfo getPreviousForwardObj(String requestId, String userId, String roleId,
      String docType) throws Exception {
    List<EutForwardReqMoreInfo> forwardls = new ArrayList<EutForwardReqMoreInfo>();
    EutForwardReqMoreInfo previousForward = null;
    JSONObject delegateFromUserRoleJson = new JSONObject();
    String currentUserId = userId;
    String currentRoleId = roleId;
    String hql = "";
    try {
      OBContext.setAdminMode();
      delegateFromUserRoleJson = getDelegateFromUserandRole(userId, roleId, docType);
      if (delegateFromUserRoleJson != null && delegateFromUserRoleJson.length() > 0) {
        if (delegateFromUserRoleJson.has("delegatedFromRoleId")
            && delegateFromUserRoleJson.has("delegatedFromUserId")) {
          currentRoleId = delegateFromUserRoleJson.getString("delegatedFromRoleId");
          currentUserId = delegateFromUserRoleJson.getString("delegatedFromUserId");
        }
      }
      if (requestId != null) {
        hql = " as e where e.request=:recentRequestId   and e.processed='N'  and ((e.recuser.id=:currentUserId and e.rECRole.id=:currentRoleId ) ";
        if (delegateFromUserRoleJson != null && delegateFromUserRoleJson.length() > 0) {
          if (delegateFromUserRoleJson.has("delegatedToUserId")
              && delegateFromUserRoleJson.has("delegatedToRoleId")) {
            hql += "  or (e.recuser.id=:delegatedToUserId and e.rECRole.id=:delegatedToRoleId )  ";
          }
        }
        hql += "  ) order by creationDate desc  ";
        OBQuery<EutForwardReqMoreInfo> forward = OBDal.getInstance()
            .createQuery(EutForwardReqMoreInfo.class, hql);
        forward.setNamedParameter("recentRequestId", requestId);
        forward.setNamedParameter("currentUserId", currentUserId);
        forward.setNamedParameter("currentRoleId", currentRoleId);
        if (delegateFromUserRoleJson != null && delegateFromUserRoleJson.length() > 0) {
          if (delegateFromUserRoleJson.has("delegatedToUserId")
              && delegateFromUserRoleJson.has("delegatedToRoleId")) {
            forward.setNamedParameter("delegatedToUserId",
                delegateFromUserRoleJson.getString("delegatedToUserId"));
            forward.setNamedParameter("delegatedToRoleId",
                delegateFromUserRoleJson.getString("delegatedToRoleId"));
          }
        }
        forward.setMaxResult(1);
        forwardls = forward.list();
        if (forwardls != null && forwardls.size() > 0) {
          previousForward = forwardls.get(0);
        }
      }
    } catch (final Exception e) {
      log.error("Exception in getPreviousForwardObj", e);
    }

    return previousForward;
  }

  @Override
  public JSONObject getDelegateFromUserandRole(String userId, String roleId, String documentType) {
    Date currentDate = new Date();
    String query = null;
    Query delQuery = null;
    JSONObject result = new JSONObject();
    try {

      query = "select dl.role.id,dl.userContact.id,dll.role.id,dll.userContact.id from Eut_Docapp_Delegateln dll join dll.eUTDocappDelegate "
          + "dl where dl.fromDate <=:currentDate and dl.date >=:currentDate and "
          + "dll.documentType=:docType and dl.processed='Y' and (dll.userContact.id=:currentUserId "
          + "and dll.role.id=:currentRoleId)  ";

      delQuery = OBDal.getInstance().getSession().createQuery(query);
      delQuery.setParameter("currentDate", currentDate);
      delQuery.setParameter("docType", documentType);

      delQuery.setParameter("currentUserId", userId);

      delQuery.setParameter("currentRoleId", roleId);

      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          Object[] row = (Object[]) delQuery.list().get(0);
          if (row[0] != null)
            result.put("delegatedFromRoleId", row[0]);
          if (row[1] != null)
            result.put("delegatedFromUserId", row[1]);
          if (row[2] != null)
            result.put("delegatedToRoleId", row[2]);
          if (row[3] != null)
            result.put("delegatedToUserId", row[3]);

        }
      }
    } catch (final Exception e) {
      log.error("Exception in getDelegateFromUserandRole", e);
    }
    return result;

  }

  @Override
  public Boolean isCurrentRoleandUserCanRevoke(String currentForwardId, String userId,
      String roleId, String docType) throws Exception {
    List<EutForwardReqMoreInfo> forwardls = new ArrayList<EutForwardReqMoreInfo>();
    JSONObject delegateFromUserRoleJson = new JSONObject();
    String currentUserId = userId;
    String currentRoleId = roleId;
    String delegatedToUserId = userId;
    String delegatedToRoleId = roleId;
    Boolean isCurrentRoleUserCanRevoke = false;
    try {
      OBContext.setAdminMode();
      delegateFromUserRoleJson = getDelegateFromUserandRole(userId, roleId, docType);
      if (delegateFromUserRoleJson != null && delegateFromUserRoleJson.length() > 0) {
        if (delegateFromUserRoleJson.has("delegatedFromRoleId")
            && delegateFromUserRoleJson.has("delegatedFromUserId")
            && delegateFromUserRoleJson.has("delegatedToRoleId")
            && delegateFromUserRoleJson.has("delegatedToUserId")) {
          currentRoleId = delegateFromUserRoleJson.getString("delegatedFromRoleId");
          currentUserId = delegateFromUserRoleJson.getString("delegatedFromUserId");
          delegatedToUserId = delegateFromUserRoleJson.getString("delegatedToUserId");
          delegatedToRoleId = delegateFromUserRoleJson.getString("delegatedToRoleId");
        }
      }

      OBQuery<EutForwardReqMoreInfo> forward = OBDal.getInstance().createQuery(
          EutForwardReqMoreInfo.class,
          " as e where e.id=:currentForwardId and ((e.userContact.id=:currentUserId and e.role.id=:currentRoleId ) or (e.userContact.id=:delegatedToUserId and e.role.id=:delegatedToRoleId)) ");
      forward.setNamedParameter("currentForwardId", currentForwardId);
      forward.setNamedParameter("currentUserId", currentUserId);
      forward.setNamedParameter("currentRoleId", currentRoleId);
      forward.setNamedParameter("delegatedToUserId", delegatedToUserId);
      forward.setNamedParameter("delegatedToRoleId", delegatedToRoleId);
      forwardls = forward.list();
      if (forwardls != null && forwardls.size() > 0) {
        isCurrentRoleUserCanRevoke = true;
      }

    } catch (final Exception e) {
      log.error("Exception in isCurrentRoleandUserCanRevoke", e);
    }

    return isCurrentRoleUserCanRevoke;
  }

  public EutForwardReqMoreInfo getPreviousRMIRecordBasedOnRequestId(String userId, String roleId,
      EutForwardReqMoreInfo responseObj, String documentType, boolean isrevoke) {
    List<EutForwardReqMoreInfo> forwardObjList = new ArrayList<EutForwardReqMoreInfo>();
    List<EutForwardReqMoreInfo> forwardRequestList = new ArrayList<EutForwardReqMoreInfo>();
    EutForwardReqMoreInfo previousForwardObj = null;
    JSONObject delegateFromUserRoleJson = new JSONObject();
    String currentUserId = userId;
    String currentRoleId = roleId;
    String delegateToUserId = userId;
    String delegateToRoleId = roleId;
    String hql = "";
    try {
      OBContext.setAdminMode();
      delegateFromUserRoleJson = getDelegateFromUserandRole(userId, roleId, documentType);
      if (delegateFromUserRoleJson != null && delegateFromUserRoleJson.length() > 0) {// && isrevoke
        if (delegateFromUserRoleJson.has("delegatedFromRoleId")
            && delegateFromUserRoleJson.has("delegatedFromUserId")) {
          currentRoleId = delegateFromUserRoleJson.getString("delegatedFromRoleId");
          currentUserId = delegateFromUserRoleJson.getString("delegatedFromUserId");
        }
        if (delegateFromUserRoleJson.has("delegatedToRoleId")
            && delegateFromUserRoleJson.has("delegatedToUserId")) {
          delegateToRoleId = delegateFromUserRoleJson.getString("delegatedToRoleId");
          delegateToUserId = delegateFromUserRoleJson.getString("delegatedToUserId");
        }
      }

      if (responseObj != null && responseObj.getRequest() != null) {
        OBQuery<EutForwardReqMoreInfo> forwardRequestQry = OBDal.getInstance().createQuery(
            EutForwardReqMoreInfo.class,
            " as e where  e.forwardRmi='RMI' and e.rEQResponse='REQ' and e.id=:requestId ");
        forwardRequestQry.setNamedParameter("requestId", responseObj.getRequest());
        forwardRequestQry.setMaxResult(1);
        forwardRequestList = forwardRequestQry.list();

        if (forwardRequestList.size() > 0) {
          EutForwardReqMoreInfo forwardRequestobj = forwardRequestList.get(0);
          if (forwardRequestobj.getRequest() != null) {
            hql = " as e where  e.forwardRmi='RMI' and e.rEQResponse='REQ' and ((e.recuser.id=:userId and e.rECRole.id=:roleId  and e.request=:requestId ) ";

            if (delegateFromUserRoleJson.has("delegatedToRoleId")
                && delegateFromUserRoleJson.has("delegatedToUserId")) {
              hql += " or (e.recuser.id=:delegatedToUserId and e.rECRole.id=:delegatedToRoleId  and e.request=:requestId )) ";
            } else {
              hql += " )  ";
            }

            hql += " and e.processed='N' order by created desc ";
            OBQuery<EutForwardReqMoreInfo> previousForwardQry = OBDal.getInstance()
                .createQuery(EutForwardReqMoreInfo.class, hql);
            // or (e.userContact.id=:userId and e.role.id=:roleId and e.id=:requestId))
            previousForwardQry.setNamedParameter("userId", currentUserId);
            previousForwardQry.setNamedParameter("roleId", currentRoleId);
            previousForwardQry.setNamedParameter("requestId", forwardRequestobj.getRequest());
            if (delegateFromUserRoleJson.has("delegatedToRoleId")
                && delegateFromUserRoleJson.has("delegatedToUserId")) {
              previousForwardQry.setNamedParameter("delegatedToUserId", delegateToUserId);
              previousForwardQry.setNamedParameter("delegatedToRoleId", delegateToRoleId);
            }
            previousForwardQry.setMaxResult(1);
            forwardObjList = previousForwardQry.list();
            if (forwardObjList.size() > 0) {
              previousForwardObj = forwardObjList.get(0);
              return previousForwardObj;
            }
          }
        }
      }

    } catch (final Exception e) {
      log.error("Exception in getPreviousRMIRecordBasedOnRequestId", e);
    }
    return previousForwardObj;

  }

  public EutForwardReqMoreInfo getRMIRequestId(String userId, String roleId, String requestId,
      String documentType) {
    List<EutForwardReqMoreInfo> rmiRequestIdList = new ArrayList<EutForwardReqMoreInfo>();
    EutForwardReqMoreInfo rmiRequestIdObj = null;
    JSONObject delegateFromUserRoleJson = new JSONObject();
    String currentUserId = userId;
    String currentRoleId = roleId;
    String delegateToUserId = userId;
    String delegateToRoleId = roleId;
    String hql = "";
    try {
      OBContext.setAdminMode();
      delegateFromUserRoleJson = getDelegateFromUserandRole(userId, roleId, documentType);
      if (delegateFromUserRoleJson != null && delegateFromUserRoleJson.length() > 0) {
        if (delegateFromUserRoleJson.has("delegatedFromRoleId")
            && delegateFromUserRoleJson.has("delegatedFromUserId")) {
          currentRoleId = delegateFromUserRoleJson.getString("delegatedFromRoleId");
          currentUserId = delegateFromUserRoleJson.getString("delegatedFromUserId");
        }
        if (delegateFromUserRoleJson.has("delegatedToRoleId")
            && delegateFromUserRoleJson.has("delegatedToUserId")) {
          delegateToRoleId = delegateFromUserRoleJson.getString("delegatedToRoleId");
          delegateToUserId = delegateFromUserRoleJson.getString("delegatedToUserId");
        }
      }

      if (requestId != null) {
        hql = " as e where  e.forwardRmi='RMI' and e.rEQResponse='REQ' "
            + " and ((e.userContact.id=:userId and e.role.id=:roleId  and e.request=:requestId ) or (e.userContact.id=:userId and e.role.id=:roleId  and e.id=:requestId) ";
        if (delegateFromUserRoleJson.has("delegatedToRoleId")
            && delegateFromUserRoleJson.has("delegatedToUserId")) {
          hql += " or (e.userContact.id=:delegateToUserId and e.role.id=:delegateToRoleId  and e.request=:requestId ) or (e.userContact.id=:delegateToUserId and e.role.id=:delegateToRoleId  and e.id=:requestId)) ";
        } else {
          hql += " )  ";
        }
        hql += "  and e.processed='N' order by created desc ";
        OBQuery<EutForwardReqMoreInfo> rmiRequestIdQry = OBDal.getInstance()
            .createQuery(EutForwardReqMoreInfo.class, hql);
        rmiRequestIdQry.setNamedParameter("userId", currentUserId);
        rmiRequestIdQry.setNamedParameter("roleId", currentRoleId);
        rmiRequestIdQry.setNamedParameter("requestId", requestId);
        if (delegateFromUserRoleJson.has("delegatedToRoleId")
            && delegateFromUserRoleJson.has("delegatedToUserId")) {
          rmiRequestIdQry.setNamedParameter("delegateToUserId", delegateToUserId);
          rmiRequestIdQry.setNamedParameter("delegateToRoleId", delegateToRoleId);
        }
        rmiRequestIdQry.setMaxResult(1);
        rmiRequestIdList = rmiRequestIdQry.list();
        if (rmiRequestIdList.size() > 0) {
          rmiRequestIdObj = rmiRequestIdList.get(0);
          return rmiRequestIdObj;
        }
      }

    } catch (final Exception e) {
      log.error("Exception in getRMIRequestId", e);
    }
    return rmiRequestIdObj;

  }

  public EutForwardReqMoreInfo getRecentResponseMsgForRequestId(EutForwardReqMoreInfo rmi) {
    List<EutForwardReqMoreInfo> responseMsgList = new ArrayList<EutForwardReqMoreInfo>();
    EutForwardReqMoreInfo responseMsg = null;
    try {
      OBContext.setAdminMode();
      if (rmi != null && rmi.getRequest() != null) {
        OBQuery<EutForwardReqMoreInfo> responseMsgQry = OBDal.getInstance().createQuery(
            EutForwardReqMoreInfo.class,
            " as e where  e.forwardRmi='RMI' and e.rEQResponse='RES' "
                + "  and e.request in ( select req.id from eut_forward_reqmoreinfo req where req.forwardRmi='RMI'"
                + " and req.rEQResponse='REQ' and req.request=:requestId) order by created desc  ");
        responseMsgQry.setNamedParameter("requestId", rmi.getRequest());
        responseMsgQry.setMaxResult(1);
        responseMsgList = responseMsgQry.list();
        if (responseMsgList.size() > 0) {
          responseMsg = responseMsgList.get(0);
          return responseMsg;
        }
      }

    } catch (final Exception e) {
      log.error("Exception in getRecentResponseMsgForRequestId", e);
    }
    return responseMsg;

  }

  @Override
  public Boolean checkRMIRevokeUser(EutForwardReqMoreInfo rmi, String userId, String roleId,
      String documentType) {
    boolean checkRMIRevokeUser = false;
    JSONObject delegateFromUserRoleJson = new JSONObject();
    String delegateFromRoleId = null;
    String delegateFromUserId = null;
    try {
      OBContext.setAdminMode();
      if (rmi != null) {

        EutForwardReqMoreInfo initalRmiReqObj = OBDal.getInstance().get(EutForwardReqMoreInfo.class,
            rmi.getRequest());

        delegateFromUserRoleJson = getDelegateFromUserandRole(userId, roleId, documentType);
        if (delegateFromUserRoleJson != null && delegateFromUserRoleJson.length() > 0) {
          if (delegateFromUserRoleJson.has("delegatedFromRoleId")
              && delegateFromUserRoleJson.has("delegatedFromUserId")) {
            delegateFromRoleId = delegateFromUserRoleJson.getString("delegatedFromRoleId");
            delegateFromUserId = delegateFromUserRoleJson.getString("delegatedFromUserId");
          }
        }

        if (initalRmiReqObj != null && ((initalRmiReqObj.getUserContact().getId().equals(userId)
            && initalRmiReqObj.getRole().getId().equals(roleId))
            || (delegateFromUserId != null
                && delegateFromUserId.equals(initalRmiReqObj.getUserContact().getId())
                && delegateFromRoleId != null
                && delegateFromRoleId.equals(initalRmiReqObj.getRole().getId()))
            || (rmi != null && rmi.getUserContact().getId().equals(userId)
                && rmi.getRole().getId().equals(roleId))
            || (delegateFromUserId != null
                && delegateFromUserId.equals(rmi.getUserContact().getId())
                && delegateFromRoleId != null
                && delegateFromRoleId.equals(rmi.getRole().getId())))) {
          checkRMIRevokeUser = true;
        }
      }
    } catch (final Exception e) {
      log.error("Exception in checkRMIRevokeUser", e);
    }
    return checkRMIRevokeUser;
  }

  public HashMap<String, String> getNextRoleLineList(EutNextRole nextrole, String docType) {
    HashMap<String, String> alertReceiversMap = new HashMap<String, String>();
    String userId = "";
    String roleId = "";
    String delgaterUserId = "";
    String delgaterroleId = "";
    JSONObject delegateJson = new JSONObject();
    try {
      if (nextrole != null) {
        for (EutNextRoleLine nextRoleLine : nextrole.getEutNextRoleLineList()) {
          if (nextRoleLine.getEUTForwardReqmoreinfo() == null
              && nextRoleLine.getEUTReqmoreinfo() == null) {
            if (nextRoleLine.getUserContact() != null) {
              userId = nextRoleLine.getUserContact().getId();
            } else {
              userId = "0";
            }
            roleId = nextRoleLine.getRole().getId();
            alertReceiversMap.put(roleId, userId);
            delegateJson = getDelegateToUserandToRole(userId, roleId, docType);
            if (delegateJson != null && delegateJson.length() > 0) {
              delgaterroleId = delegateJson.getString("delegatedToRoleId");
              delgaterUserId = delegateJson.getString("delegatedToUserId");
              alertReceiversMap.put(delgaterroleId, delgaterUserId);
            }
          }
        }
      }

    } catch (final Exception e) {
      log.error("Exception in getNextRoleLineList", e);
    }
    return alertReceiversMap;
  }

  @Override
  public JSONObject getResponseUserAndRoles(EutForwardReqMoreInfo rmi, boolean isgetRole,
      String inpRecord, String userId) {
    JSONObject json = new JSONObject(), userJson = null, roleJson = null, duplicateJson = null;
    List<EutForwardReqMoreInfo> allRmiReqList = new ArrayList<EutForwardReqMoreInfo>();
    JSONArray jsonArray = new JSONArray();
    User user = null;
    BusinessPartner bpartner = null;
    Role role = null;
    String hql = "";
    boolean isduplicate = false;
    try {
      OBContext.setAdminMode();
      hql = "  as e where e.request = :request and e.recordid = :recordId  and e.rEQResponse='REQ'   and e.processed='N' ";
      if (StringUtils.isNotEmpty(userId) && isgetRole) {
        hql += " and e.userContact.id=:userId ";
      }
      hql += " order by created desc  ";
      if (rmi != null && rmi.getRequest() != null) {
        OBQuery<EutForwardReqMoreInfo> allRmiReqQry = OBDal.getInstance()
            .createQuery(EutForwardReqMoreInfo.class, hql);
        allRmiReqQry.setNamedParameter("request", rmi.getRequest());
        allRmiReqQry.setNamedParameter("recordId", inpRecord);
        if (StringUtils.isNotEmpty(userId) && isgetRole) {
          allRmiReqQry.setNamedParameter("userId", userId);
        }
        allRmiReqList = allRmiReqQry.list();
        if (allRmiReqList.size() > 0) {
          for (EutForwardReqMoreInfo rmiObj : allRmiReqList) {
            isduplicate = false;
            if (!isgetRole) {
              user = rmiObj.getUserContact();
              if (jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                  duplicateJson = jsonArray.getJSONObject(i);
                  if (duplicateJson.getString("id").equals(user.getId())) {
                    isduplicate = true;
                    break;
                  }
                }
              }
              if (!isduplicate && (!user.getId().equals(OBContext.getOBContext().getUser().getId()))
                  && !user.getId().equals(rmi.getRecuser().getId())) {
                userJson = new JSONObject();
                bpartner = user.getBusinessPartner();
                userJson.put("id", user.getId());
                userJson.put("text", bpartner.getSearchKey() + "-" + user.getName());
                jsonArray.put(userJson);
              }
            } else if (StringUtils.isNotEmpty(userId) && isgetRole) {
              role = rmiObj.getRole();
              roleJson = new JSONObject();
              roleJson.put("id", role.getId());
              roleJson.put("text", role.getName());
              jsonArray.put(roleJson);
            }
            json.put("List", jsonArray);
          }
        }
      }
    } catch (final Exception e) {
      log.error("Exception in getResponseUserAndRoles", e);
    }
    return json;
  }

  @Override
  public String getRmiRequestDetail(EutForwardReqMoreInfo rmi, String inpRecord, String userId,
      String roleId) {
    List<EutForwardReqMoreInfo> allRmiReqList = new ArrayList<EutForwardReqMoreInfo>();
    String reqMessage = "";
    try {
      OBContext.setAdminMode();

      if (rmi != null && rmi.getRequest() != null) {
        OBQuery<EutForwardReqMoreInfo> allRmiReqQry = OBDal.getInstance().createQuery(
            EutForwardReqMoreInfo.class,
            " as e where e.request = :request and e.recordid = :recordId  and e.rEQResponse='REQ'  and e.processed='N' and e.userContact.id=:userId and e.role.id=:roleId order by created desc ");
        allRmiReqQry.setNamedParameter("request", rmi.getRequest());
        allRmiReqQry.setNamedParameter("recordId", inpRecord);
        allRmiReqQry.setNamedParameter("userId", userId);
        allRmiReqQry.setNamedParameter("roleId", roleId);
        allRmiReqQry.setMaxResult(1);
        allRmiReqList = allRmiReqQry.list();
        if (allRmiReqList.size() > 0) {
          EutForwardReqMoreInfo reqObj = allRmiReqList.get(0);
          reqMessage = StringUtils.isNotEmpty(reqObj.getMessage()) ? reqObj.getMessage() : "";
        }
      }
    } catch (final Exception e) {
      log.error("Exception in getRmiRequestDetail", e);
    }
    return reqMessage;
  }

  @Override
  public EutForwardReqMoreInfo setRMIRecordStatusAsDraft(EutForwardReqMoreInfo rmi, String userId,
      String roleId) throws Exception {
    EutForwardReqMoreInfo forwardObj = null;
    List<EutForwardReqMoreInfo> initialForwardList = new ArrayList<EutForwardReqMoreInfo>();
    try {
      OBContext.setAdminMode();
      if (rmi != null && rmi.getRequest() != null) {
        OBQuery<EutForwardReqMoreInfo> initialForwardObj = OBDal.getInstance().createQuery(
            EutForwardReqMoreInfo.class,
            "as e where e.request = :request and e.recordid = :recordId  and e.rEQResponse='REQ'  order by created desc ");
        initialForwardObj.setNamedParameter("request", rmi.getRequest());
        initialForwardObj.setNamedParameter("recordId", rmi.getRecordid());
        initialForwardList = initialForwardObj.list();
        if (initialForwardList != null && initialForwardList.size() > 0) {
          for (EutForwardReqMoreInfo rmiObj : initialForwardList) {
            if (!rmiObj.isProcessed()) {
              if (!userId.equals(rmiObj.getRecuser().getId())
                  || (userId.equals(rmiObj.getRecuser().getId())
                      && !roleId.equals(rmiObj.getRECRole().getId()))) {
                rmiObj.setProcessed(true);
                rmiObj.setStatus("DR");
                OBDal.getInstance().save(rmiObj);
                OBDal.getInstance().flush();
              } else {
                break;
              }
            }
          }
        }
      }

    } catch (final Exception e) {
      log.error("Exception in getReqMoreInfo", e);
    } finally {
    }
    return forwardObj;
  }

  @Override
  public void InsertEncumbranceApprovalHistory(JSONObject data) throws Exception {
    try {
      OBContext.setAdminMode();
      StringBuilder queryBuilder = new StringBuilder();
      String historyId = SequenceIdData.getUUID();
      String strTableName = data.getString("HistoryTable");

      queryBuilder.append(" INSERT INTO  ").append(strTableName);
      queryBuilder.append(" ( ").append(strTableName.concat("_id"))
          .append(", ad_client_id, ad_org_id,");
      queryBuilder.append(" createdby, updatedby,   ").append(data.getString("HeaderColumn"))
          .append(" , approveddate, ");
      queryBuilder.append(" comments, ").append(data.getString("ActionColumn"))
          .append(" , pendingapproval)");
      queryBuilder.append(" VALUES (?, ?, ?, ");
      queryBuilder.append(" ?, ?, ?, ?,");
      queryBuilder.append(" ?, ?, ?);");
      PreparedStatement query = OBDal.getInstance().getConnection()
          .prepareStatement(queryBuilder.toString());
      query.setString(1, historyId);
      query.setString(2, data.getString("ClientId"));
      query.setString(3, data.getString("OrgId"));
      query.setString(4, data.getString("UserId"));
      query.setString(5, data.getString("UserId"));
      query.setString(6, data.getString("HeaderId"));
      query.setDate(7, new java.sql.Date(System.currentTimeMillis()));
      query.setString(8, data.getString("Comments"));
      query.setString(9, data.getString("Status"));
      query.setString(10, data.optString("NextApprover"));

      log.debug("History Query: " + query.toString());
      query.executeUpdate();
    } catch (Exception e) {
      log.error("Exception in InsertEncumbranceApprovalHistory ", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  @Override
  public JSONObject delegatedFromUserValidation(String inpRecordId, String windowReference,
      String docType, String clientId, Date currentDate, String userId, String roleId) {
    JSONObject json = null, delJson = null;
    EutNextRole eutNextRole = null;
    EutForwardReqMoreInfo forwardObj = null;
    EutForwardReqMoreInfo rmiObj = null;
    List<EutNextRoleLine> roleLineList = new ArrayList<EutNextRoleLine>();
    try {
      OBContext.setAdminMode();
      json = new JSONObject();
      forwardObj = getForwardObj(inpRecordId, windowReference);
      rmiObj = getReqMoreInfo(inpRecordId, windowReference);
      if (forwardObj == null && rmiObj == null) {
        eutNextRole = getNextRole(inpRecordId, windowReference);
        if (eutNextRole != null) {
          // getting eutNextRoleLine without forward/rmi
          OBQuery<EutNextRoleLine> roleLine = OBDal.getInstance().createQuery(EutNextRoleLine.class,
              " as e where e.eUTNextRole.id=:eutNextRoleId and e.client.id=:clientId and e.eUTForwardReqmoreinfo.id is null and e.eUTReqmoreinfo.id is null ");
          roleLine.setNamedParameter("eutNextRoleId", eutNextRole.getId());
          roleLine.setNamedParameter("clientId", clientId);
          roleLineList = roleLine.list();
          if (roleLineList != null && roleLineList.size() > 0) {
            for (EutNextRoleLine line : roleLineList) {
              delJson = isDelegatedFromToUserRole(currentDate, docType, line.getRole().getId(),
                  (line.getUserContact() != null ? line.getUserContact().getId() : null), roleId,
                  userId);
              if (delJson != null && delJson.length() > 0) {
                json.put("OriginalUserId", delJson.get("DelegatedFromUserID"));
                json.put("OriginalRoleId", delJson.get("DelegatedFromRoleID"));
              }
            }
          }
        }
      }
    } catch (final Exception e) {
      log.error("Exception in delegatedFromUserValidation", e);
    }
    return json;
  }

  public JSONObject isDelegatedUser(String inpRecordId, String windowReference, String userId,
      String roleId, String forwardOrRmi) {
    JSONObject json = null;
    String recUserId = null, recRoleId = null;
    EutForwardReqMoreInfo rmi = null;
    EutForwardReqMoreInfo forward = null;
    try {
      OBContext.setAdminMode();
      json = new JSONObject();
      if (forwardOrRmi.equals(Constants.FORWARD)) {
        forward = getForwardObj(inpRecordId, windowReference);
      }
      if (forwardOrRmi.equals(Constants.REQUEST_MORE_INFORMATION)) {
        rmi = getReqMoreInfo(inpRecordId, windowReference);
        if (rmi == null) {
          forward = getForwardObj(inpRecordId, windowReference);
        }
      }

      if (rmi != null) {
        recUserId = rmi.getRecuser().getId();
        recRoleId = rmi.getRECRole().getId();
      }
      if (forward != null) {
        recUserId = forward.getRecuser().getId();
        recRoleId = forward.getRECRole().getId();
      }
      // If current user is not equal to forward/rmi ToUser, it is delegated user
      if (recUserId != null && recRoleId != null && !userId.equals(recUserId)
          && !roleId.equals(recRoleId)) {
        json.put("isDelegated", true);
        json.put("OriginalUserId", recUserId);
        json.put("OriginalRoleId", recRoleId);
      }

    } catch (final Exception e) {
      log.error("Exception in isDelegatedUser", e);
    }
    return json;
  }

  @Override
  public String checkAndReturnTemporaryPreference(String preferece_name, String roleId,
      String userId, String clientId, String org_id, String windowId, String requester_user_id,
      String requester_role_id) {

    String preferenceValue = "N";
    String requester_preference_value = "N";
    try {
      // Check Temporary Preference
      preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true, clientId, org_id,
          userId, roleId, windowId, "Y");
      preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;
      if (preferenceValue.equals("Y")) {
        // then check the forwarded user have the budget controller
        try {
          requester_preference_value = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
              clientId, org_id, requester_user_id, requester_role_id, windowId, "N");
          requester_preference_value = (requester_preference_value == null) ? "N"
              : requester_preference_value;
        } catch (PropertyException e) {
          requester_preference_value = "N";
          // log.error("Exception in getting budget controller :", e);
        }
        // Incase the requester may be forwarded from temporary budget controller preference
        // so first check the normal preference from above then check temporary preference
        if (requester_preference_value.equals("N")) {
          try {
            requester_preference_value = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
                clientId, org_id, requester_user_id, requester_role_id, windowId, "Y");
            requester_preference_value = (requester_preference_value == null) ? "N"
                : requester_preference_value;
          } catch (PropertyException e) {
            requester_preference_value = "N";
            // log.error("Exception in getting budget controller :", e);
          }
        }

        if (requester_preference_value.equals("Y") && preferenceValue.equals("Y")) {
          preferenceValue = "Y";
        } else {
          preferenceValue = "N";
        }
      }
    } catch (Exception e) {
      preferenceValue = "N";
      log.error("Exception in checkAndReturnTemporaryPreference", e);
    }
    return preferenceValue;
  }

  @Override
  public EutForwardReqMoreInfo findForwardReferenceAgainstTheRecord(String recordId, String userId,
      String roleId) {

    EutForwardReqMoreInfo objForwardReqMoreInfo = null;
    try {
      // get the next line no based on bid management id
      OBQuery<EutForwardReqMoreInfo> query = OBDal.getInstance().createQuery(
          EutForwardReqMoreInfo.class,
          " as e where e.recuser.id=:userId and e.rECRole.id=:roleId and e.forwardRmi=:forward and e.recordid=:recordId "
              // + " and e.processed=:processed "
              + " order by e.creationDate desc ");
      query.setNamedParameter("userId", userId);
      query.setNamedParameter("roleId", roleId);
      query.setNamedParameter("forward", "F");
      query.setNamedParameter("recordId", recordId);
      // query.setNamedParameter("processed", false);
      query.setMaxResult(1);
      if (query.list().size() > 0) {
        objForwardReqMoreInfo = query.list().get(0);
      }
    }

    catch (Exception e) {
      log.error("exception in findForwardReferenceAgainstTheRecord: ", e);
    } finally {
    }
    return objForwardReqMoreInfo;
  }

  @Override
  public void setOriginalAlert(String recordId, EutNextRole nextrole) {

    try {
      OBContext.setAdminMode();

      OBQuery<Alert> alertnew = OBDal.getInstance().createQuery(Alert.class,
          " as e  where e.referenceSearchKey=:PinpRecordId  and e.alertStatus='NEW' and e.eutAlertKey in ('scm.pr.wfa','scm.BidMgmt.wfa','scm.pm.wfa','scm.poc.wfa','scm.ct.wfa','scm.pac.wfa','scm.Returntrans.wfa','scm.techevaluation.event.wfa','scm.mir.wfa','scm.smir.wfa')");
      alertnew.setNamedParameter("PinpRecordId", recordId);

      List<Alert> alertList = alertnew.list();
      List<Alert> orginalAlertList = new ArrayList<>();

      if (alertList.size() > 0) {

        for (EutNextRoleLine line : nextrole.getEutNextRoleLineList()) {
          Role role = line.getRole();
          User user = line.getUserContact();
          orginalAlertList.addAll(
              alertList.stream().filter(a -> a.getRole() == role && a.getUserContact() == user)
                  .collect(Collectors.toList()));
        }

        for (Alert alert : orginalAlertList) {
          alert.setEutIsoriginalalert(true);
          alert.setEutNextrole(nextrole.getId());
          alert.setAlertStatus("SOLVED");
          OBDal.getInstance().save(alert);
        }

        if (alertList.size() > 0) {
          OBDal.getInstance().flush();
        }
      }

    } catch (Exception e) {
      log.error("Error while finding original alert" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  @Override
  public List<Alert> findOriginalAlert(String recordId, EutNextRole nextrole, String reference,
      boolean fwd_Rmi) {

    List<Alert> alertList = new ArrayList<>();
    try {
      OBContext.setAdminMode();
      if (getforwardId(recordId, reference, fwd_Rmi) == null) {

        OBQuery<Alert> alertnew = OBDal.getInstance().createQuery(Alert.class,
            " as e  where e.referenceSearchKey=:PinpRecordId  and e.alertStatus='SOLVED' and e.eutIsoriginalalert='Y' and e.eutNextrole = :nextroleId");
        alertnew.setNamedParameter("PinpRecordId", recordId);
        alertnew.setNamedParameter("nextroleId", nextrole.getId());

        alertList = alertnew.list();
      }

    } catch (Exception e) {
      log.error("Error while finding original alert" + e.getMessage());
      return alertList;
    } finally {
      OBContext.restorePreviousMode();
    }

    return alertList;

  }

  public EutForwardReqMoreInfo getforwardId(String inpRecordId, String windowReference,
      boolean fwd_Rmi) throws Exception {
    EscmBidMgmt bid = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    MaterialIssueRequest mir = null;
    EutForwardReqMoreInfo nextRole = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    ShipmentInOut returnTran = null;
    try {
      OBContext.setAdminMode();
      if (windowReference.equals(Constants.BID_MANAGEMENT)) {
        bid = OBDal.getInstance().get(EscmBidMgmt.class, inpRecordId);
        if (fwd_Rmi)
          nextRole = bid.getEUTForwardReqmoreinfo();
        else
          nextRole = bid.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || windowReference.equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, inpRecordId);
        OBDal.getInstance().refresh(purchaseReq);
        if (fwd_Rmi)
          nextRole = purchaseReq.getEutForward();
        else
          nextRole = purchaseReq.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || windowReference.equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, inpRecordId);
        if (fwd_Rmi)
          nextRole = proMgmt.getEUTForwardReqmoreinfo();
        else
          nextRole = proMgmt.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.PURCHASE_REQUISITION_DIRECT)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        if (fwd_Rmi)
          nextRole = purchaseOrder.getEutForward();
        else
          nextRole = purchaseOrder.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, inpRecordId);
        if (fwd_Rmi)
          nextRole = purchaseOrder.getEutForward();
        else
          nextRole = purchaseOrder.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        if (fwd_Rmi)
          nextRole = custodyTransfer.getEutForward();
        else
          nextRole = custodyTransfer.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST)
          || windowReference.equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || windowReference.equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpRecordId);
        if (fwd_Rmi)
          nextRole = mir.getEUTForward();
        else
          nextRole = mir.getEUTReqmoreinfo();
      } else if (windowReference.equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, inpRecordId);
        if (fwd_Rmi)
          nextRole = returnTran.getEutForward();
        else
          nextRole = returnTran.getEutReqmoreinfo();
      } else if (windowReference.equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class, inpRecordId);
        if (fwd_Rmi)
          nextRole = technicalEvlEvent.getEUTForward();
        else
          nextRole = technicalEvlEvent.getEUTReqmoreinfo();
      }
    } catch (final Exception e) {
      log.error("Exception in getNextRole", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
    return nextRole;
  }

  public static void insertForward_Rmialert(String clientId, String alertWindow,
      EutForwardReqMoreInfo forReqMoreInfo, String Lang, String windowName, boolean fwd_Rmi)
      throws Exception {
    EscmBidMgmt bidmgmt = null;
    Requisition purchaseReq = null;
    EscmProposalMgmt proMgmt = null;
    Order purchaseOrder = null;
    ShipmentInOut custodyTransfer = null;
    MaterialIssueRequest mir = null;
    EscmTechnicalevlEvent technicalEvlEvent = null;
    ShipmentInOut returnTran = null;
    String alterKey = null;
    String accessKey = null;
    String documentNo = null;
    String UserName = forReqMoreInfo.getUserContact().getName();
    String description = null;
    String recRole = forReqMoreInfo.getRECRole().getName();
    String recRoleId = forReqMoreInfo.getRECRole().getId();
    String recUserId = forReqMoreInfo.getRecuser().getId();
    String alertWindowType = null;
    ArrayList<ForwardAlertVO> alertUsers = new ArrayList<ForwardAlertVO>();
    try {
      OBContext.setAdminMode();
      if (fwd_Rmi)
        alterKey = "utility.forwardby";
      else
        alterKey = "utility.RequestedInformationby";
      accessKey = "Utility.access.role";

      description = windowName + " "
          + sa.elm.ob.utility.properties.Resource.getProperty(alterKey, Lang) + " " + UserName + " "
          + sa.elm.ob.utility.properties.Resource.getProperty(accessKey, Lang) + " " + recRole;

      if (forReqMoreInfo.getDocruleid().equals(Constants.BID_MANAGEMENT)) {
        bidmgmt = OBDal.getInstance().get(EscmBidMgmt.class, forReqMoreInfo.getRecordid());
        documentNo = bidmgmt.getBidno() + "-" + bidmgmt.getBidname();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.PURCHASE_REQUISITION_DIRECT)
          || forReqMoreInfo.getDocruleid().equals(Constants.PURCHASE_REQUISITION_LIMITED_TENDER)) {
        purchaseReq = OBDal.getInstance().get(Requisition.class, forReqMoreInfo.getRecordid());
        documentNo = purchaseReq.getDocumentNo() + "-" + purchaseReq.getDescription();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.PROPOSAL_MANAGEMENT_DIRECT)
          || forReqMoreInfo.getDocruleid().equals(Constants.PROPOSAL_MANAGEMENT_LIMITED_TENDER)) {
        proMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, forReqMoreInfo.getRecordid());
        documentNo = proMgmt.getProposalno() + "-" + proMgmt.getBidName();
      } else if (forReqMoreInfo.getDocruleid()
          .equals(Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY)) {
        purchaseOrder = OBDal.getInstance().get(Order.class, forReqMoreInfo.getRecordid());
        documentNo = purchaseOrder.getDocumentNo()
            + ((purchaseOrder.getEscmNotes() != null && !purchaseOrder.getEscmNotes().equals("null")
                && !purchaseOrder.getEscmNotes().equals("")) ? "-" + purchaseOrder.getEscmNotes()
                    : "");
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.Custody_Transfer)) {
        custodyTransfer = OBDal.getInstance().get(ShipmentInOut.class,
            forReqMoreInfo.getRecordid());
        documentNo = custodyTransfer.getDocumentNo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.MATERIAL_ISSUE_REQUEST)
          || forReqMoreInfo.getDocruleid().equals(Constants.MATERIAL_ISSUE_REQUEST_IT)
          || forReqMoreInfo.getDocruleid().equals(Constants.SITE_MATERIAL_ISSUE_REQUEST)) {
        mir = OBDal.getInstance().get(MaterialIssueRequest.class, forReqMoreInfo.getRecordid());
        documentNo = mir.getDocumentNo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.RETURN_TRANSACTION)) {
        returnTran = OBDal.getInstance().get(ShipmentInOut.class, forReqMoreInfo.getRecordid());
        documentNo = returnTran.getDocumentNo();
      } else if (forReqMoreInfo.getDocruleid().equals(Constants.TECHNICAL_EVALUATION_EVENT)) {
        technicalEvlEvent = OBDal.getInstance().get(EscmTechnicalevlEvent.class,
            forReqMoreInfo.getRecordid());
        documentNo = technicalEvlEvent.getEventNo();
      }

      // getting alert window type
      if (alertWindow.split("_").length > 1) {
        if (fwd_Rmi)
          alertWindowType = alertWindow.split("_")[0].toString();
        else
          alertWindowType = alertWindow.split("_")[1].toString();
      }

      if (StringUtils.isNotBlank(documentNo)) {
        AlertUtility.alertInsertionRole(forReqMoreInfo.getRecordid(), documentNo, recRoleId,
            recUserId, forReqMoreInfo.getClient().getId(), description, "NEW", alertWindowType,
            alterKey, Constants.GENERIC_TEMPLATE);
      }

    } catch (final Exception e) {
      log.error("Exception in alertprocess", e);
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

}
