package sa.elm.ob.utility.ad_forms.delegation.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

import sa.elm.ob.utility.EutDocappDelegate;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.ad_forms.delegation.header.ApprovalDelegation;
import sa.elm.ob.utility.ad_forms.delegation.vo.ApprovalDelegationVO;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

@SuppressWarnings("rawtypes")
public class ApprovalDelegationDAO {
  private Connection conn = null;
  private static Logger log4j = Logger.getLogger(ApprovalDelegationDAO.class);

  public ApprovalDelegationDAO(Connection con) {
    this.conn = con;
  }

  /**
   * This method is used to get users
   * 
   * @param clientId
   * @param orgId
   * @param includeDocRule
   * @return
   */
  public List<JSONObject> getUsers(String clientId, String orgId, boolean includeDocRule) {
    List<JSONObject> userLs = null;
    JSONObject usrVO = null;
    Query usrQuery = null;
    StringBuffer query = null;

    try {
      OBContext.setAdminMode(true);
      String childOrgId = Utility.getChildOrg(clientId, orgId);
      query = new StringBuffer();
      query.append(
          "select usrrl.userContact.name, usrrl.userContact.id, coalesce(usrrl.userContact.businessPartner.searchKey, ''), coalesce(usrrl.userContact.businessPartner.name, '') from ADUserRoles usrrl where usrrl.userContact.client='"
              + clientId
              + "'  and  usrrl.userContact.active = 'Y' and usrrl.userContact.organization in ("
              + childOrgId + ") ");
      if (includeDocRule)
        query.append(" and usrrl.role.eutIncludeinrule='Y' ");
      query.append(
          " and usrrl.userContact.businessPartner is not null group by usrrl.userContact.name, usrrl.userContact.id, usrrl.userContact.businessPartner.searchKey,usrrl.userContact.businessPartner.name ");
      query.append(" order by usrrl.userContact.businessPartner.name asc ");
      usrQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      log4j.debug("getUsers->" + query.toString());
      if (usrQuery != null) {
        userLs = new ArrayList<JSONObject>();
        if (usrQuery.list().size() > 0) {
          for (Iterator iterator = usrQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            usrVO = new JSONObject();

            usrVO.put("id", objects[1].toString());
            usrVO.put("name", objects[2].toString() + " - " + objects[3].toString());
            userLs.add(usrVO);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in userLs ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return userLs;
  }

  /**
   * This method is used to get roles
   * 
   * @param userId
   * @param includeDocRule
   * @return
   */
  public List<JSONObject> getRoles(String userId, boolean includeDocRule) {
    List<JSONObject> roleLs = null;
    JSONObject rolVO = null;
    Query rolQuery = null;
    StringBuffer query = null;

    try {
      OBContext.setAdminMode(true);
      query = new StringBuffer();
      query.append(
          "select usrol.role.name, usrol.role.id from ADUserRoles usrol where usrol.userContact.id='"
              + userId + "' and usrol.role.active = 'Y' and usrol.active = 'Y' ");
      if (includeDocRule)
        query.append(" and usrol.role.eutIncludeinrule='Y' ");
      query.append(" order by usrol.role.name asc ");
      rolQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      log4j.debug("rolels>" + query.toString());
      if (rolQuery != null) {
        roleLs = new ArrayList<JSONObject>();
        if (rolQuery.list().size() > 0) {
          for (Iterator iterator = rolQuery.iterate(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            rolVO = new JSONObject();

            rolVO.put("name", objects[0].toString());
            rolVO.put("id", objects[1].toString());
            roleLs.add(rolVO);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getRoles ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return roleLs;
  }

  /**
   * This method is used to get documents
   * 
   * @param vars
   * @param headerId
   * @param clientId
   * @param orgId
   * @param sortColName
   * @param sortType
   * @param rows
   * @param paramPage
   * @param type
   * @param roleId
   * @param userId
   * @return
   */
  public List<ApprovalDelegationVO> getDocuments(VariablesSecureApp vars, String headerId,
      String clientId, String orgId, String sortColName, String sortType, int rows, int paramPage,
      String type, String roleId, String userId) {
    List<ApprovalDelegationVO> docLs = null;
    ApprovalDelegationVO docVO = null;
    SQLQuery docQuery = null;
    StringBuffer query = null;
    Boolean isLinemanager = false;
    Boolean isReturnTranscation = false;
    Boolean isPurchaseRequisition = false;
    String preferenceITRole = "N";
    String preferenceGeneralServices = "N";
    Boolean isOrgManager = false;
    Boolean isBudgetManager = false;
    Boolean isBcuFRM = false;
    Boolean isOrgFRM = false;
    Boolean isCustody = false;

    int count = 0, totalPages = 0, start = 0, page = paramPage;
    try {
      OBContext.setAdminMode(true);
      query = new StringBuffer();

      if (type.equals("E")) {
        Role role = OBDal.getInstance().get(Role.class, roleId);
        if (role.isEscmIslinemanager()) {
          isLinemanager = true;
        }

        if (headerId == null || headerId.equals("")) {
          /*
           * Removed For Task No #6253 Role can delegate his approval based on document rule
           */
          /*
           * docLs = new ArrayList<ApprovalDelegationVO>(); for (Map.Entry<String, String> entry :
           * ApprovalDelegation.mapDocType.entrySet()) { try { docVO = new ApprovalDelegationVO();
           * docVO.setPage(1); docVO.setTotalpages(1);
           * docVO.setCount(ApprovalDelegation.mapDocType.size());
           * docVO.setDocName(entry.getValue()); docVO.setDocType(entry.getKey());
           * docVO.setUserId(""); docVO.setType("N"); docVO.setUserName(""); docVO.setRoleId("");
           * docVO.setRoleName(""); docLs.add(docVO); } catch (Exception e) {
           * 
           * } } return docLs;
           */

          /*
           * Removed For Task no:2842 query.append(
           * "select distinct document_type,  CAST('' as text) as ad_user_id, CAST('N' as text) as type, CAST('' as text) as name, CAST('' as text) as ad_role_id, CAST('' as text) as rolename  from qu_documentrule_header where ad_client_id='"
           * + clientId + "' and ad_org_id='" + orgId + "' " + (Utility.haveConstructionModule() ?
           * " and em_qcs_project_id is null" : ""));
           */
          query.append("( select distinct document_type, CAST('' as text) "
              + " as ad_user_id, CAST('N' as text) as type, CAST('' as text) as name, "
              + " CAST('' as text) as ad_role_id, CAST('' as text) as rolename "
              + " from eut_documentrule_header " + " where ad_client_id='" + clientId + "' "
              + " and ad_org_id in ( select ad_org_id from AD_Role_OrgAccess where ad_role_id ='"
              + vars.getRole()
              + "') and document_type in(select header.document_type from eut_documentrule_header header join "
              + "eut_documentrule_lines lines on lines.eut_documentrule_header_id = header.eut_documentrule_header_id where lines.ad_role_id = '"
              + roleId + "' )and document_type!='EUT_114') ");
        } else {
          query.append(
              " select distinct document_type, CAST('' as text) as ad_user_id, CAST('N' as text) as type, CAST('' as text) as name, CAST('' as text) as ad_role_id, CAST('' as text) as rolename from eut_documentrule_header "
                  + "where ad_client_id='" + clientId + "' "

                  + " and document_type not in (select document_type from eut_docapp_delegateln delln"
                  + " left join eut_docapp_delegate delhdr on delhdr.eut_docapp_delegate_id = delln.eut_docapp_delegate_id"
                  + " where delhdr.eut_docapp_delegate_id='" + headerId
                  + "' and delhdr.ad_role_id = '" + roleId + "' and delhdr.ad_user_id = '" + userId
                  + "') " + "and ad_client_id='" + clientId + "'"
                  + " and ad_org_id in ( select ad_org_id from AD_Role_OrgAccess where ad_role_id='"
                  + vars.getRole()
                  + "') and document_type in(select header.document_type from eut_documentrule_header header join "
                  + " eut_documentrule_lines lines on lines.eut_documentrule_header_id = header.eut_documentrule_header_id where lines.ad_role_id = '"
                  + roleId + "' )and document_type!='EUT_114'"
                  + " union select document_type, delln.ad_user_id, CAST('U' as text) as type, usr.name, coalesce(delln.ad_role_id, ''), coalesce(rol.name, '') as rolename from eut_docapp_delegateln delln "
                  + "left join ad_user usr on usr.ad_user_id=delln.ad_user_id "
                  + "left join ad_role rol on rol.ad_role_id=delln.ad_role_id "
                  + "left join eut_docapp_delegate delhdr on delhdr.eut_docapp_delegate_id = delln.eut_docapp_delegate_id"
                  + " where delhdr.eut_docapp_delegate_id='" + headerId
                  + "' and  delhdr.ad_role_id = '" + roleId + "' and delhdr.ad_user_id = '" + userId
                  + "'");

        }
      } else if (type.equals("G")) {
        if (headerId != null && !headerId.equals("")) {
          query.append("select document_type,  delln.ad_user_id, "
              + "CAST('U' as text) as type, coalesce(bp.value, '')||'-'||coalesce(bp.name, '') "
              + "as name, coalesce(delln.ad_role_id, ''), coalesce(rol.name, '') as rolename  from eut_docapp_delegateln delln "
              + " left join ad_user usr on usr.ad_user_id=delln.ad_user_id "
              + " left join ad_role rol on rol.ad_role_id=delln.ad_role_id "
              + " left join c_bpartner bp on bp.c_bpartner_id=usr.c_bpartner_id "
              + " where eut_docapp_delegate_id='" + headerId + "'");
        }
      }
      if (sortColName != null)
        query.append(" order by  ").append(sortColName).append(" ").append(sortType);
      docQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
      log4j.debug("getDocuments->" + query.toString());
      count = docQuery.list().size();
      if (count > 0) {
        totalPages = (int) (count) / rows;

        if ((int) (count) % rows > 0)
          totalPages = totalPages + 1;
        start = (page - 1) * rows;

        if (page > totalPages) {
          page = totalPages;
          start = (page - 1) * rows;
        }
      } else {
        totalPages = 0;
        page = 0;
      }
      docQuery.setFirstResult(start);
      docQuery.setMaxResults(rows);

      if (docQuery != null) {
        docLs = new ArrayList<ApprovalDelegationVO>();
        if (docQuery.list().size() > 0) {
          for (Iterator iterator = docQuery.list().listIterator(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            docVO = new ApprovalDelegationVO();

            docVO.setPage(page);
            docVO.setTotalpages(totalPages);
            docVO.setCount(count);

            docVO.setDocName(ApprovalDelegation.mapLangDocType.get(vars.getLanguage())
                .get(objects[0].toString()));
            docVO.setDocType(objects[0].toString());
            if (docVO.getDocType() != null) {
              if (docVO.getDocType().equals("EUT_113")) {
                isReturnTranscation = true;
              }
              if (docVO.getDocType().equals("EUT_111")) {
                isPurchaseRequisition = true;
              }
              if (docVO.getDocType().equals("EUT_120")) {
                isBcuFRM = true;
              }
              if (docVO.getDocType().equals("EUT_121")) {
                isOrgFRM = true;
              }
              if (docVO.getDocType().equals("EUT_114")) {
                isCustody = true;
              }
            }
            docVO.setUserId(objects[1].toString());
            docVO.setType(objects[2].toString());
            docVO.setUserName(objects[3] == null ? "" : objects[3].toString());
            docVO.setRoleId(objects[4].toString());
            docVO.setRoleName(objects[5].toString());
            docLs.add(docVO);
          }
        }
      }

      // In Return Transaction - If user's role for delegation is Line Manager and HR Manager role
      // is added in document rule
      // then list return transaction document type for the user in delegation.
      if (isLinemanager && !isReturnTranscation) {

        getDocumentsForSpecialCaseRT(vars, paramPage, headerId, clientId, orgId, "E", roleId,
            userId, docLs, count, totalPages);
      }

      // In PR - If user's role is associated with
      // preference "IT Role" & "General Services" and Specialized Department role is added in
      // document rule then show PR document type
      // for the user.
      try {
        preferenceITRole = Preferences.getPreferenceValue("ESCM_ITRole", true, vars.getClient(),
            orgId, userId, roleId, "800092");
      }

      catch (PropertyException e) {
      }
      try {
        preferenceGeneralServices = Preferences.getPreferenceValue("ESCM_GeneralServices", true,
            vars.getClient(), orgId, userId, roleId, "800092");
      }

      catch (PropertyException e) {
      }

      if (preferenceITRole != null && !isPurchaseRequisition
          && (preferenceITRole.equals("Y") || preferenceGeneralServices.equals("Y"))) {
        getDocumentsForSpecialCasePR(vars, paramPage, headerId, clientId, orgId, "E", roleId,
            userId, docLs, count, totalPages);
      }
      // Added logic to list Funds request document type for Org Manager & Org Budget Manager
      String doctype = "EUT_120";
      String flag = "";
      isOrgManager = checkIsOrgManager(clientId, userId);
      isBudgetManager = checkIsOrgBudgetManager(clientId, userId);

      // BCU-FRM
      if (!isBcuFRM) {
        if (isOrgManager) {
          flag = "ORGM";
          getDocumentsForSpecialCaseFRM(vars, paramPage, headerId, clientId, orgId, "E", roleId,
              userId, docLs, count, totalPages, flag, doctype);
        }
        if (!isOrgManager && isBudgetManager) {
          flag = "ORGBM";
          getDocumentsForSpecialCaseFRM(vars, paramPage, headerId, clientId, orgId, "E", roleId,
              userId, docLs, count, totalPages, flag, doctype);
        }
      }
      // ORG-FRM

      doctype = "EUT_121";
      if (!isOrgFRM) {
        if (isOrgManager) {
          flag = "ORGM";
          getDocumentsForSpecialCaseFRM(vars, paramPage, headerId, clientId, orgId, "E", roleId,
              userId, docLs, count, totalPages, flag, doctype);
        }
        if (!isOrgManager && isBudgetManager) {
          flag = "ORGBM";
          getDocumentsForSpecialCaseFRM(vars, paramPage, headerId, clientId, orgId, "E", roleId,
              userId, docLs, count, totalPages, flag, doctype);
        }
      }

      // custody transfer validation call statement
      if (!isCustody) {
        getDocumentsForSpecialCaseCustody(vars, paramPage, headerId, clientId, orgId, "E", roleId,
            userId, docLs, count, totalPages);
      }

    } catch (Exception e) {
      log4j.error("Exception in docLs ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return docLs;
  }

  /**
   * This method is used to insert req Approval delegation
   * 
   * @param hdVO
   * @param clientId
   * @param orgId
   * @param userId
   * @return
   */
  public String insertReqAppDelegate(ApprovalDelegationVO hdVO, String clientId, String orgId,
      String userId) {
    Client client = null;
    Organization organization = null;
    User user = null;
    User fromUserId = null;
    Role fromRoleId = null;
    EutDocappDelegate header = null;
    String headerId = "";

    try {
      OBContext.setAdminMode(true);

      client = OBDal.getInstance().get(Client.class, clientId);
      organization = OBDal.getInstance().get(Organization.class, orgId);
      user = OBDal.getInstance().get(User.class, userId);
      fromUserId = OBDal.getInstance().get(User.class, hdVO.getUserId());
      fromRoleId = OBDal.getInstance().get(Role.class, hdVO.getRoleId());

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Date fromDate = null;
      Date toDate = null;

      Calendar calendar = new GregorianCalendar();
      fromDate = sdf.parse(hdVO.getFromDate());

      calendar.setTime(sdf.parse(hdVO.getToDate()));
      calendar.set(Calendar.HOUR_OF_DAY, 23);
      calendar.set(Calendar.MINUTE, 59);
      calendar.set(Calendar.SECOND, 59);
      toDate = calendar.getTime();

      header = OBProvider.getInstance().get(EutDocappDelegate.class);

      header.setClient(client);
      header.setOrganization(organization);
      header.setCreatedBy(user);
      header.setUpdatedBy(user);

      header.setFromDate(fromDate);
      header.setDate(toDate);
      header.setUserContact(fromUserId);
      header.setRole(fromRoleId);

      OBDal.getInstance().save(header);
      OBDal.getInstance().flush();
      headerId = header.getId();
    } catch (Exception e) {
      log4j.error("Exception in insertReqAppDelegate: ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return headerId;
  }

  /**
   *
   * @param docLs
   * @param clientId
   * @param orgId
   * @param userId
   * @param headerId
   * @return
   */
  public boolean insertReqAppDelegateLn(List<ApprovalDelegationVO> docLs, String clientId,
      String orgId, String userId, String headerId) {
    EutDocappDelegateln lines = null;
    Client client = null;
    Organization organization = null;
    User user = null;
    Role role = null;
    User toUserId = null;
    EutDocappDelegate header = null;

    try {
      OBContext.setAdminMode(true);
      if (deleteReqAppDelegateLn(headerId)) {
        client = OBDal.getInstance().get(Client.class, clientId);
        organization = OBDal.getInstance().get(Organization.class, orgId);
        user = OBDal.getInstance().get(User.class, userId);
        header = OBDal.getInstance().get(EutDocappDelegate.class, headerId);

        for (ApprovalDelegationVO docVO : docLs) {
          toUserId = OBDal.getInstance().get(User.class, docVO.getUserId());
          role = OBDal.getInstance().get(Role.class, docVO.getRoleId());

          lines = OBProvider.getInstance().get(EutDocappDelegateln.class);
          lines.setClient(client);
          lines.setOrganization(organization);
          lines.setCreatedBy(user);
          lines.setUpdatedBy(user);
          lines.setEUTDocappDelegate(header);
          lines.setDocumentType(docVO.getDocType());
          lines.setUserContact(toUserId);
          lines.setRole(role);

          OBDal.getInstance().save(lines);
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in insertReqAppDelegateLn: ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  /**
   * 
   * @param hdVO
   * @param paramHeaderId
   * @param clientId
   * @param orgId
   * @param userId
   * @return
   */
  public String updateReqAppDelegate(ApprovalDelegationVO hdVO, String paramHeaderId,
      String clientId, String orgId, String userId) {
    EutDocappDelegate header = null;
    User fromUserId = null;
    Role fromRoleId = null;
    User user = null;
    String headerId = paramHeaderId;
    try {
      OBContext.setAdminMode(true);
      header = OBDal.getInstance().get(EutDocappDelegate.class, headerId);
      user = OBDal.getInstance().get(User.class, userId);
      fromUserId = OBDal.getInstance().get(User.class, hdVO.getUserId());
      fromRoleId = OBDal.getInstance().get(Role.class, hdVO.getRoleId());

      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Date fromDate = null;
      Date toDate = null;
      Calendar calendar = new GregorianCalendar();
      fromDate = sdf.parse(hdVO.getFromDate());

      calendar.setTime(sdf.parse(hdVO.getToDate()));
      calendar.set(Calendar.HOUR_OF_DAY, 23);
      calendar.set(Calendar.MINUTE, 59);
      calendar.set(Calendar.SECOND, 59);
      toDate = calendar.getTime();

      header.setUpdated(new java.util.Date());
      header.setUpdatedBy(user);
      header.setFromDate(fromDate);
      header.setDate(toDate);
      header.setUserContact(fromUserId);
      header.setRole(fromRoleId);

      OBDal.getInstance().save(header);
      OBDal.getInstance().flush();
      headerId = header.getId();
    } catch (Exception e) {
      log4j.error("Exception in updateReqAppDelegate(): ", e);
      return "";
    } finally {
      OBContext.restorePreviousMode();
    }
    return headerId;
  }

  /**
   * 
   * @param headerId
   * @return
   */
  public boolean deleteReqAppDelegate(String headerId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    int count;
    try {
      st = conn.prepareStatement("\n"
          + "select count(coalesce(wa.em_eut_docapp_delegateln_id,coalesce(pa.em_eut_docapp_delegateln_id,\n"
          + "           coalesce(fa.em_eut_docapp_delegateln_id,\n"
          + "           coalesce(oa.em_eut_docapp_delegateln_id,coalesce(rc.eut_docapp_delegateln_id,et.docapp_delegateln_id)))))) as count from eut_docapp_delegate dl\n"
          + "            left join  eut_docapp_delegateln dll on \n"
          + "           dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id\n"
          + "           left join ad_window_access wa on wa.em_eut_docapp_delegateln_id = dll.eut_docapp_delegateln_id\n"
          + "           left join ad_process_access pa on pa.em_eut_docapp_delegateln_id = dll.eut_docapp_delegateln_id\n"
          + "           left join ad_form_access fa on fa.em_eut_docapp_delegateln_id = dll.eut_docapp_delegateln_id\n"
          + "           left join obuiapp_process_access oa on oa.em_eut_docapp_delegateln_id = dll.eut_docapp_delegateln_id\n"
          + "           left join eut_list_access et on et.docapp_delegateln_id = dll.eut_docapp_delegateln_id\n"
          + "           left join eut_delegate_role_check rc on rc.eut_docapp_delegateln_id = dll.eut_docapp_delegateln_id\n"
          + "           where dl.eut_docapp_delegate_id = ?");
      st.setString(1, headerId);
      rs = st.executeQuery();
      log4j.debug("delete check>" + st.toString());
      if (rs.next()) {
        count = rs.getInt("count");
        if (count > 0) {
          return false;
        } else {
          st = conn.prepareStatement("delete from ad_preference where em_eut_docapp_delegateln_id ="
              + "(select eut_docapp_delegateln_id from eut_docapp_delegateln where eut_docapp_delegate_id = ?)");
          st.setString(1, headerId);
          st.executeUpdate();
          if (deleteReqAppDelegateLn(headerId)) {
            st = conn
                .prepareStatement("delete from eut_docapp_delegate where eut_docapp_delegate_id=?");
            st.setString(1, headerId);
            st.executeUpdate();
          }
        }

      }
    } catch (final Exception e) {
      log4j.error("Exception in deleteReqAppDelegateLn() : ", e);
      return false;
    }
    return true;
  }

  /**
   * 
   * @param headerId
   * @return
   */
  public boolean deleteReqAppDelegateLn(String headerId) {
    PreparedStatement st = null;
    try {
      st = conn
          .prepareStatement("delete from eut_docapp_delegateln where eut_docapp_delegate_id=?");
      st.setString(1, headerId);
      st.executeUpdate();
    } catch (final Exception e) {
      log4j.error("Exception in deleteReqAppDelegateLn() : ", e);
      return false;
    }
    return true;
  }

  /**
   * This method is used to get user details
   * 
   * @param inpFromDate
   * @param inpToDate
   * @param inpUserId
   * @param inpRoleId
   * @param doctypLs
   * @param inpHeaderId
   * @return
   */
  public JSONObject userdetails(String inpFromDate, String inpToDate, String inpUserId,
      String inpRoleId, String doctypLs, String inpHeaderId) {

    final JSONObject response = new JSONObject();
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    String fromdate = "";
    log4j.debug("fromdate" + fromdate);
    String todate = "";
    log4j.debug("todate" + todate);
    try {

      String sql = "select count(ad_user_id) as count,to_char(a.fromdate,'dd-MM-yyyy') as fromdate,to_char(a.todate,'dd-MM-yyyy') as todate"
          + " from (select del.ad_user_id,del.from_date as fromdate,del.to_date as todate from  eut_docapp_delegate del "
          + " left join eut_docapp_delegateln delln on delln.eut_docapp_delegate_id=del.eut_docapp_delegate_id where del.ad_user_id='"
          + inpUserId + "' and del.ad_role_id='" + inpRoleId + "' and delln.document_type in ("
          + doctypLs + ") ";

      if (inpHeaderId != null && !inpHeaderId.equals("")) {
        sql += " and del.eut_docapp_delegate_id not in ('" + inpHeaderId + "')";
      }
      sql += " )as a  where (((to_date('" + inpFromDate + "','yyyy-MM-dd hh24:mi:ss')>=a.fromdate) "
          + " and (to_date('" + inpToDate + "','yyyy-MM-dd hh24:mi:ss')<=todate))  "
          + "  or ((to_date('" + inpToDate + "','yyyy-MM-dd hh24:mi:ss')>=a.fromdate) "
          + " and (to_date('" + inpFromDate
          + "','yyyy-MM-dd hh24:mi:ss')<=a.todate))) group by a.fromdate,a.todate ";
      st = conn.prepareStatement(sql);
      rs = st.executeQuery();
      log4j.debug("userdetails>" + sql.toString());
      if (rs.next()) {
        count = rs.getInt("count");
        log4j.debug("sqlquery" + sql.toString());
        fromdate = rs.getString("fromdate");
        log4j.debug("fromdate" + fromdate);
        todate = rs.getString("todate");
        log4j.debug("todate" + todate);
        response.put("count", count);
        response.put("fromdate", fromdate);
        response.put("todate", todate);
      } else {
        response.put("count", count);
      }
    } catch (Exception e) {
      return response;
    }
    return response;
  }

  /**
   * This method is used to get record details
   * 
   * @param inpDelegatId
   * @return
   */
  public JSONObject getRecordDetails(String inpDelegatId) {
    final JSONObject response = new JSONObject();
    PreparedStatement st = null;
    ResultSet rs = null;
    int count = 0;
    try {
      String sql = "select count(eut_docapp_delegate_id) as count from  eut_docapp_delegate where eut_docapp_delegate_id='"
          + inpDelegatId + "'";

      st = conn.prepareStatement(sql);
      rs = st.executeQuery();
      while (rs.next()) {
        count = rs.getInt("count");
        response.put("count", count);
      }
    } catch (Exception e) {
      return response;
    }
    return response;
  }

  /**
   * This method is used to check admin user
   * 
   * @param clientId
   * @param orgId
   * @param userId
   * @param roleId
   * @return
   */
  public boolean checkAdminUser(String clientId, String orgId, String userId, String roleId) {
    String preferenceValue = "";
    boolean isAdminUser = false;
    try {
      preferenceValue = Preferences.getPreferenceValue("EUT_DelegationDoc_List", true, clientId,
          orgId, userId, roleId, null);
      if (preferenceValue != null && preferenceValue.equals("Y"))
        isAdminUser = true;
      else
        isAdminUser = false;
    } catch (PropertyException e) {
    }
    return isAdminUser;
  }

  /**
   * This method is used to get delegate List
   * 
   * @param clientId
   * @param orgId
   * @param headerId
   * @param sortColName
   * @param sortType
   * @param rows
   * @param paramPage
   * @param searchVO
   * @param userId
   * @param roleId
   * @return
   */
  public List<ApprovalDelegationVO> getDelegateList(String clientId, String orgId, String headerId,
      String sortColName, String sortType, int rows, int paramPage, ApprovalDelegationVO searchVO,
      String userId, String roleId) {
    List<ApprovalDelegationVO> delegateLs = null;
    ApprovalDelegationVO delVO = null;
    StringBuffer sqlQuery = null;
    SQLQuery query = null;
    DateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
    String date = "";
    int count = 0, totalPages = 0, start = 0, page = paramPage;
    try {
      OBContext.setAdminMode(true);

      delegateLs = new ArrayList<ApprovalDelegationVO>();
      sqlQuery = new StringBuffer();
      boolean isAdminUser = checkAdminUser(clientId, orgId, userId, roleId);

      sqlQuery.append(
          " select eut_docapp_delegate_id, from_date, to_date, del.ad_user_id, usr.name, del.ad_role_id, rol.name as rolename, ");
      sqlQuery
          .append("  coalesce(bp.value, '')||'-'||coalesce(bp.name, '') as usrname, processed ");
      sqlQuery.append(
          " from eut_docapp_delegate del left join ad_user usr on usr.ad_user_id=del.ad_user_id ");
      sqlQuery.append(" left join ad_role rol on rol.ad_role_id=del.ad_role_id ");
      sqlQuery.append(" left join c_bpartner bp on bp.c_bpartner_id=usr.c_bpartner_id ");
      sqlQuery.append(" where del.ad_client_id='" + clientId + "'");

      if (headerId != null && !headerId.equals("")) {
        sqlQuery.append(" and del.eut_docapp_delegate_id='" + headerId + "' ");
      } else {
        sqlQuery.append(" and del.ad_org_id in ( select ad_org_id from AD_Role_OrgAccess ");
        sqlQuery.append(" where ad_role_id='" + roleId + "')");
      }
      if (!isAdminUser && StringUtils.isEmpty(headerId)) {
        sqlQuery
            .append(" and del.createdby = '" + userId + "' or del.ad_user_id = '" + userId + "'");
      }

      if (searchVO != null) {
        if (searchVO.getFromDate() != null && !searchVO.getFromDate().equals(""))
          sqlQuery.append("and to_date(to_char(from_date,'dd-MM-yyyy'),'dd-MM-yyyy')"
              + searchVO.getFromDate().split("##")[0] + "to_date('"
              + searchVO.getFromDate().split("##")[1] + "','yyyy-MM-dd')");
        if (searchVO.getToDate() != null && !searchVO.getToDate().equals(""))
          sqlQuery.append("and to_date(to_char(to_date,'dd-MM-yyyy'),'dd-MM-yyyy')"
              + searchVO.getToDate().split("##")[0] + "to_date('"
              + searchVO.getToDate().split("##")[1] + "','yyyy-MM-dd') ");
        if (searchVO.getUserName() != null && !searchVO.getUserName().equals(""))
          sqlQuery.append(" and (bp.name ilike '%" + searchVO.getUserName()
              + "%' or bp.value ilike '%" + searchVO.getUserName() + "%')");
        if (searchVO.getRoleName() != null && !searchVO.getRoleName().equals(""))
          sqlQuery.append(" and rol.name ilike '%" + searchVO.getRoleName() + "%'");
      }
      if (sortColName != null)
        sqlQuery.append(" order by  ").append(sortColName).append(" ").append(sortType);
      log4j.debug("getDelegateList->" + sqlQuery.toString());
      query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery.toString());
      log4j.debug("hdr ls>" + sqlQuery.toString());
      count = query.list().size();
      if (count > 0) {
        totalPages = (int) (count) / rows;

        if ((int) (count) % rows > 0)
          totalPages = totalPages + 1;
        start = (page - 1) * rows;

        if (page > totalPages) {
          page = totalPages;
          start = (page - 1) * rows;
        }
      } else {
        totalPages = 0;
        page = 0;
      }
      query.setFirstResult(start);
      query.setMaxResults(rows);

      delegateLs = new ArrayList<ApprovalDelegationVO>();

      if (query != null) {
        if (query.list().size() > 0) {
          for (Iterator iterator = query.list().listIterator(); iterator.hasNext();) {
            delVO = new ApprovalDelegationVO();
            delVO.setPage(page);
            delVO.setTotalpages(totalPages);
            delVO.setCount(count);
            Object[] row = (Object[]) iterator.next();
            delVO.setHeaderId(row[0].toString());
            if (row[1].toString() != null) {
              date = df.format(row[1]);
              date = dateYearFormat.format(df.parse(date));
              date = UtilityDAO.convertTohijriDate(date);
              delVO.setFromDate(date);
            } else
              delVO.setFromDate("");
            // delVO.setFromDate(row[1].toString().substring(8, 10) + "-" +
            // row[1].toString().substring(5, 7) + "-" + row[1].toString().substring(0, 4));
            if (row[2].toString() != null) {
              date = df.format(row[2]);
              date = dateYearFormat.format(df.parse(date));
              date = UtilityDAO.convertTohijriDate(date);
              delVO.setToDate(date);
            } else
              delVO.setToDate("");
            // delVO.setToDate(row[2].toString().substring(8, 10) + "-" +
            // row[2].toString().substring(5, 7) + "-" + row[2].toString().substring(0, 4));
            delVO.setUserId(row[3].toString());
            delVO.setUserName(row[7].toString());
            delVO.setRoleId(row[5] == null ? "" : row[5].toString());
            delVO.setRoleName(row[6] == null ? "" : row[6].toString());
            delVO.setProcessed(row[8].toString());
            delegateLs.add(delVO);
          }
        }
      }
    } catch (Exception e) {
      log4j.error(" Exception in delVO(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return delegateLs;
  }

  /**
   * This method is used to check user exists
   * 
   * @param hdVO
   * @param headerId
   * @param doctypLs
   * @return
   */
  public boolean checkUserExist(ApprovalDelegationVO hdVO, String headerId, String doctypLs) {
    PreparedStatement st = null;
    ResultSet rs = null, rs1 = null, rs2 = null;
    String result = "f";
    boolean exist = false;
    try {
      st = conn.prepareStatement(
          "select dlg.from_date, dlg.to_date from eut_docapp_delegate dlg left join eut_docapp_delegateln dlgln on dlg.eut_docapp_delegate_id=dlgln.eut_docapp_delegate_id "
              + " where dlg.ad_user_id=? and dlg.ad_role_id=? and dlg.eut_docapp_delegate_id<>? and document_type in ("
              + doctypLs + ") ");
      st.setString(1, hdVO.getUserId());
      st.setString(2, hdVO.getRoleId());
      st.setString(3, headerId);
      rs = st.executeQuery();
      while (rs.next()) {
        String fromDate = rs.getString("from_date").substring(8, 10) + "-"
            + rs.getString("from_date").substring(5, 7) + "-"
            + rs.getString("from_date").substring(0, 4);
        String toDate = rs.getString("to_date").substring(8, 10) + "-"
            + rs.getString("to_date").substring(5, 7) + "-"
            + rs.getString("to_date").substring(0, 4);
        st = conn.prepareStatement(
            "SELECT (to_date(?, 'dd-MM-yyyy'), to_date(?, 'dd-MM-yyyy')) OVERLAPS (to_date(?, 'dd-MM-yyyy'), to_date(?, 'dd-MM-yyyy')) as result;");
        st.setString(1, fromDate);
        st.setString(2, toDate);
        st.setString(3, hdVO.getFromDate());
        st.setString(4, hdVO.getToDate());
        rs1 = st.executeQuery();
        if (rs1.next()) {
          result = rs1.getString("result");
          if (result.equals("f")) {
            st = conn.prepareStatement(
                "select dlg.ad_user_id from eut_docapp_delegate dlg left join eut_docapp_delegateln dlgln on dlg.eut_docapp_delegate_id=dlgln.eut_docapp_delegate_id "
                    + " where to_date= to_date(?, 'dd-MM-yyyy') and dlg.ad_user_id=? and dlg.ad_role_id=? and document_type in ("
                    + doctypLs + ") and dlg.eut_docapp_delegate_id<>?");
            st.setString(1, hdVO.getToDate());
            st.setString(2, hdVO.getUserId());
            st.setString(3, hdVO.getRoleId());
            st.setString(4, headerId);
            rs2 = st.executeQuery();
            if (rs2.next()) {
              exist = true;
              return exist;
            } else
              exist = false;
          } else if (result.equals("t")) {
            exist = true;
            return exist;
          }
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in checkUserExist() : ", e);
      exist = true;
    }
    return exist;
  }

  /**
   * 
   * @param userId
   * @param documentType
   * @param quNextRoleLnId
   * @return
   */
  public HashMap<String, String> getIsDelegated(String userId, String documentType,
      String quNextRoleLnId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    HashMap<String, String> roleId = new HashMap<String, String>();
    try { // qdd.ad_role_id -> From Role Id , qddl.ad_user_id -> To User Id
      st = conn.prepareStatement(
          " select qdd.ad_role_id as fromuserrole, qddl.ad_role_id as touserrole from eut_docapp_delegate qdd join eut_docapp_delegateln qddl "
              + " on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id where qddl.ad_user_id = ? "
              + " and qddl.document_type = ? and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) "
              + " and qdd.ad_role_id in (select ad_role_id from eut_next_role qnr left join eut_next_role_line qnrl on qnr.eut_next_role_id = qnrl.eut_next_role_id "
              + " where qnr.eut_next_role_id = ?) ");
      st.setString(1, userId);
      st.setString(2, documentType);
      st.setString(3, quNextRoleLnId);
      rs = st.executeQuery();
      if (rs.next()) {
        roleId.put("FromUserRoleId", rs.getString("fromuserrole"));
        roleId.put("ToUserRoleId", rs.getString("touserrole"));
      }
    } catch (final Exception e) {
      log4j.error("Exception in getIsDelegated :", e);
      return roleId;
    } finally {
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
      } catch (SQLException e) {
      }
    }
    return roleId;
  }

  /**
   * This method is used to get window delegated
   * 
   * @param userId
   * @param documentType
   * @return
   */
  public String getIsWindowDelegated(String userId, String documentType) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String toUserId = "";
    try { // qdd.ad_role_id -> From Role Id , qddl.ad_user_id -> To User Id
      st = conn.prepareStatement(
          " select qddl.ad_user_id from eut_docapp_delegate qdd join eut_docapp_delegateln qddl "
              + " on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id where qddl.ad_user_id = ? "
              + " and qddl.document_type = ? and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) ");
      st.setString(1, userId);
      st.setString(2, documentType);
      rs = st.executeQuery();
      if (rs.next())
        toUserId = rs.getString("ad_user_id");
    } catch (final Exception e) {
      log4j.error("Exception in getIsWindowDelegated :", e);
      return toUserId;
    } finally {
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
      } catch (SQLException e) {
      }
    }
    return toUserId;
  }

  /**
   * 
   * @param headerId
   * @return
   */
  public int processedRecord(String headerId) {
    String strQuery = "";
    PreparedStatement ps = null;
    try {
      strQuery = "update eut_docapp_delegate set processed = 'N' where eut_docapp_delegate_id = '"
          + headerId + "'";
      ps = conn.prepareStatement(strQuery);
      log4j.debug("ps:" + ps.toString());
      ps.executeUpdate();
    } catch (Exception e) {
      return 0;
    }
    return 1;
  }

  /**
   * This method is used to get user delegated details
   * 
   * @param inpFromDate
   * @param inpToDate
   * @param inpUserId
   * @param doctypLs
   * @param fromUserId
   * @param inpHeaderId
   * @return
   */
  public JSONObject touserDelegatedDetails(String inpFromDate, String inpToDate, String inpUserId,
      String doctypLs, String fromUserId, String inpHeaderId) {

    final JSONObject response = new JSONObject();
    PreparedStatement st = null, st1 = null;
    ResultSet rs = null, rs1 = null;
    int count = 0;
    String fromdate = "";
    log4j.debug("fromdate" + fromdate);
    String todate = "";
    log4j.debug("todate" + todate);
    try {
      String sql = "select array_to_string(array_agg(a.uname),',') as uname ,to_char(a.fromdate,'dd-MM-yyyy') as fromdate,to_char(a.todate,'dd-MM-yyyy') as todate"
          + " from (select head.ad_user_id,head.from_date as fromdate,head.to_date as todate,u.name as uname  from  eut_docapp_delegate head "
          + " left join eut_docapp_delegateln ln on ln.eut_docapp_delegate_id =head.eut_docapp_delegate_id"
          + " left join ad_user u on u.ad_user_id=head.ad_user_id " + " where head.ad_user_id in ("
          + inpUserId + ") and ln.document_type in (" + doctypLs
          + ")  and head.eut_docapp_delegate_id <>'" + inpHeaderId + "' )as a "
          + " where (((to_date('" + inpFromDate
          + "','yyyy-MM-dd hh24:mi:ss')>=a.fromdate) and (to_date('" + inpToDate
          + "','yyyy-MM-dd hh24:mi:ss')<=todate)) " + "  or ((to_date('" + inpToDate
          + "','yyyy-MM-dd hh24:mi:ss')>=a.fromdate) and (to_date('" + inpFromDate
          + "','yyyy-MM-dd hh24:mi:ss')<=a.todate))) " + " group by a.fromdate,a.todate";

      st = conn.prepareStatement(sql);
      rs = st.executeQuery();
      log4j.debug("sqlquery" + sql.toString());
      if (rs.next()) {
        count = 1;
        log4j.debug("sqlquery" + sql.toString());
        fromdate = rs.getString("fromdate");
        log4j.debug("fromdate" + fromdate);
        todate = rs.getString("todate");
        log4j.debug("todate" + todate);
        response.put("count", count);
        response.put("fromdate", fromdate);
        response.put("todate", todate);
        response.put("uname", rs.getString("uname"));
        response.put("docname", "");
      } else {
        String sql1 = "select array_to_string(array_agg(a.uname),',') as uname ,array_to_string(array_agg(a.docname),',') as docname,to_char(a.fromdate,'dd-MM-yyyy') as fromdate,to_char(a.todate,'dd-MM-yyyy') as todate"
            + " from (select head.ad_user_id,head.from_date as fromdate,head.to_date as todate,u.name as uname ,ref.name as docname from  eut_docapp_delegate head "
            + " left join eut_docapp_delegateln ln on ln.eut_docapp_delegate_id =head.eut_docapp_delegate_id"
            + " left join ad_user u on u.ad_user_id=head.ad_user_id "
            + " left join ad_ref_list ref on ref.value=ln.document_type and ref.ad_reference_id ='295CB9ADDA424ADD92F7BF5B21D12F21' "
            + " where ln.ad_user_id in ('" + fromUserId + "') and ln.document_type in (" + doctypLs
            + ") and head.eut_docapp_delegate_id <>'" + inpHeaderId + "' )as a "
            + " where (((to_date('" + inpFromDate
            + "','yyyy-MM-dd hh24:mi:ss')>=a.fromdate) and (to_date('" + inpToDate
            + "','yyyy-MM-dd hh24:mi:ss')<=todate)) " + "  or ((to_date('" + inpToDate
            + "','yyyy-MM-dd hh24:mi:ss')>=a.fromdate) and (to_date('" + inpFromDate
            + "','yyyy-MM-dd hh24:mi:ss')<=a.todate))) " + " group by a.fromdate,a.todate";
        st1 = conn.prepareStatement(sql1);
        rs1 = st1.executeQuery();
        log4j.debug("sqlquery else>" + sql1.toString());
        if (rs1.next()) {
          count = 2;
          log4j.debug("sqlquery" + sql1.toString());
          fromdate = rs1.getString("fromdate");
          log4j.debug("fromdate" + fromdate);
          todate = rs1.getString("todate");
          log4j.debug("todate" + todate);
          response.put("count", count);
          response.put("fromdate", fromdate);
          response.put("todate", todate);
          response.put("uname", rs1.getString("uname"));
          response.put("docname", rs1.getString("docname"));
        }
      }
    } catch (Exception e) {
      return response;
    }
    return response;
  }

  /**
   * This method is used to get documents for special case RT
   * 
   * @param vars
   * @param paramPage
   * @param headerId
   * @param clientId
   * @param orgId
   * @param type
   * @param roleId
   * @param userId
   * @param docLs
   * @param count
   * @param totalPages
   * @return
   */
  public List<ApprovalDelegationVO> getDocumentsForSpecialCaseRT(VariablesSecureApp vars,
      int paramPage, String headerId, String clientId, String orgId, String type, String roleId,
      String userId, List<ApprovalDelegationVO> docLs, int count, int totalPages) {
    SQLQuery docQuery = null;
    StringBuffer query = null;
    ApprovalDelegationVO docVO = null;
    int page = paramPage;
    try {
      OBContext.setAdminMode(true);
      OBQuery<Role> roleid = OBDal.getInstance().createQuery(Role.class,
          "escmIshrlinemanager='Y' and client.id = :clientId order by creationDate desc");
      roleid.setNamedParameter("clientId", vars.getClient());
      List<Role> roleIdList = roleid.list();
      if (roleIdList.size() > 0) {
        String hrId = roleIdList.get(0).getId();
        query = new StringBuffer();
        query.append("select distinct document_type, CAST('' as text) "
            + " as ad_user_id, CAST('N' as text) as type, CAST('' as text) as name, "
            + " CAST('' as text) as ad_role_id, CAST('' as text) as rolename "
            + " from eut_documentrule_header " + " where ad_client_id='" + clientId + "' "
            + " and ad_org_id in ( select ad_org_id from AD_Role_OrgAccess where ad_role_id ='"
            + vars.getRole()
            + "') and document_type in(select header.document_type from eut_documentrule_header header join "
            + "eut_documentrule_lines lines on lines.eut_documentrule_header_id = header.eut_documentrule_header_id where lines.ad_role_id = '"
            + hrId + "' and header.document_type = 'EUT_113') ");
        docQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
        count = count + docQuery.list().size();
        if (count > 0) {
          Object[] objects = (Object[]) docQuery.list().get(0);
          docVO = new ApprovalDelegationVO();
          docVO.setPage(page);
          docVO.setTotalpages(totalPages);
          docVO.setCount(count);
          docVO.setDocName(
              ApprovalDelegation.mapLangDocType.get(vars.getLanguage()).get(objects[0].toString()));
          docVO.setDocType(objects[0].toString());
          docVO.setUserId(objects[1].toString());
          docVO.setType(objects[2].toString());
          docVO.setUserName(objects[3] == null ? "" : objects[3].toString());
          docVO.setRoleId(objects[4].toString());
          docVO.setRoleName(objects[5].toString());
          docLs.add(docVO);
        }

      }

    } catch (Exception e) {
      log4j.error("Exception in docLs ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return docLs;
  }

  /**
   * This method is used to get documents for special case PR
   * 
   * @param vars
   * @param paramPage
   * @param headerId
   * @param clientId
   * @param orgId
   * @param type
   * @param roleId
   * @param userId
   * @param docLs
   * @param count
   * @param totalPages
   * @return
   */
  public List<ApprovalDelegationVO> getDocumentsForSpecialCasePR(VariablesSecureApp vars,
      int paramPage, String headerId, String clientId, String orgId, String type, String roleId,
      String userId, List<ApprovalDelegationVO> docLs, int count, int totalPages) {
    SQLQuery docQuery = null;
    StringBuffer query = null;
    ApprovalDelegationVO docVO = null;
    int page = paramPage;
    try {
      OBContext.setAdminMode(true);
      OBQuery<Role> sdroleid = OBDal.getInstance().createQuery(Role.class,
          "escmIsspecializeddept='Y'");
      List<Role> roleIdList = sdroleid.list();
      if (roleIdList.size() > 0) {
        if (type.equals("E")) {
          String SpecializedDeptId = roleIdList.get(0).getId();
          query = new StringBuffer();
          query.append("select distinct document_type, CAST('' as text) "
              + " as ad_user_id, CAST('N' as text) as type, CAST('' as text) as name, "
              + " CAST('' as text) as ad_role_id, CAST('' as text) as rolename "
              + " from eut_documentrule_header " + " where ad_client_id='" + clientId + "' "
              + " and ad_org_id in ( select ad_org_id from AD_Role_OrgAccess where ad_role_id ='"
              + vars.getRole()
              + "') and document_type in(select header.document_type from eut_documentrule_header header join "
              + "eut_documentrule_lines lines on lines.eut_documentrule_header_id = header.eut_documentrule_header_id where lines.ad_role_id = '"
              + SpecializedDeptId + "' and header.document_type = 'EUT_111') ");

          docQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
          count = count + docQuery.list().size();
          if (docQuery != null) {
            if (docQuery.list().size() > 0) {
              for (Iterator iterator = docQuery.list().listIterator(); iterator.hasNext();) {
                Object[] objects = (Object[]) iterator.next();
                docVO = new ApprovalDelegationVO();
                docVO.setPage(page);
                docVO.setTotalpages(totalPages);
                docVO.setCount(count);

                docVO.setDocName(ApprovalDelegation.mapLangDocType.get(vars.getLanguage())
                    .get(objects[0].toString()));
                docVO.setDocType(objects[0].toString());
                docVO.setUserId(objects[1].toString());
                docVO.setType(objects[2].toString());
                docVO.setUserName(objects[3] == null ? "" : objects[3].toString());
                docVO.setRoleId(objects[4].toString());
                docVO.setRoleName(objects[5].toString());
                docLs.add(docVO);
              }
            }
          }

        }
      }

    } catch (Exception e) {
      log4j.error("Exception in docLs ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return docLs;
  }

  /**
   * This method is used to check Id org manager
   * 
   * @param clientId
   * @param userId
   * @return
   */
  private Boolean checkIsOrgManager(String clientId, String userId) {
    List<Organization> orgList = new ArrayList<Organization>();
    boolean orgmanager = false;
    try {
      OBQuery<Organization> orgquery = OBDal.getInstance().createQuery(Organization.class,
          " as e where e.efinOrgmanager.id=:userID and e.client.id=:clientID ");
      orgquery.setNamedParameter("userID", userId);
      orgquery.setNamedParameter("clientID", clientId);

      orgList = orgquery.list();
      if (orgList.size() > 0) {
        orgmanager = true;
      }
    } catch (final Exception e) {
      log4j.error("Exception in checkIsOrgManager", e);

    }
    return orgmanager;
  }

  /**
   * This method is to check is org budget manager
   * 
   * @param clientId
   * @param userId
   * @return
   */
  private Boolean checkIsOrgBudgetManager(String clientId, String userId) {
    List<Organization> orgList = new ArrayList<Organization>();
    boolean orgbudgetmanager = false;
    try {
      OBQuery<Organization> orgquery = OBDal.getInstance().createQuery(Organization.class,
          " as e where e.efinOrgbudgmanager.id=:userID and e.client.id =:clientID ORDER BY e.creationDate desc ");
      orgquery.setNamedParameter("userID", userId);
      orgquery.setNamedParameter("clientID", clientId);
      orgList = orgquery.list();
      if (orgList.size() > 0) {
        orgbudgetmanager = true;
      }
    } catch (final Exception e) {
      log4j.error("Exception in checkIsOrgBudgetManager", e);

    }
    return orgbudgetmanager;
  }

  /**
   * This method is used to get documents for Special case for FRM
   * 
   * @param vars
   * @param paramPage
   * @param headerId
   * @param clientId
   * @param orgId
   * @param type
   * @param roleId
   * @param userId
   * @param docLs
   * @param count
   * @param totalPages
   * @param flag
   * @param doctype
   * @return
   */
  public List<ApprovalDelegationVO> getDocumentsForSpecialCaseFRM(VariablesSecureApp vars,
      int paramPage, String headerId, String clientId, String orgId, String type, String roleId,
      String userId, List<ApprovalDelegationVO> docLs, int count, int totalPages, String flag,
      String doctype) {
    SQLQuery docQuery = null;
    StringBuffer query = null;
    ApprovalDelegationVO docVO = null;
    int page = paramPage;
    try {
      OBContext.setAdminMode(true);

      OBQuery<Role> orgManagerRole = OBDal.getInstance().createQuery(Role.class,
          " as e where e.efinOrgbcumanger=:flagValue and e.client.id =:clientID ORDER BY e.creationDate desc ");
      orgManagerRole.setNamedParameter("clientID", clientId);
      orgManagerRole.setNamedParameter("flagValue", flag);
      List<Role> orgRoleIdList = orgManagerRole.list();

      if (orgRoleIdList.size() > 0) {
        if (type.equals("E")) {
          String orgManagerId = orgRoleIdList.get(0).getId();
          query = new StringBuffer();
          query.append("select distinct document_type, CAST('' as text) "
              + " as ad_user_id, CAST('N' as text) as type, CAST('' as text) as name, "
              + " CAST('' as text) as ad_role_id, CAST('' as text) as rolename "
              + " from eut_documentrule_header " + " where ad_client_id='" + clientId + "' "
              + " and ad_org_id in ( select ad_org_id from AD_Role_OrgAccess where ad_role_id ='"
              + vars.getRole()
              + "') and document_type in(select header.document_type from eut_documentrule_header header join "
              + "eut_documentrule_lines lines on lines.eut_documentrule_header_id = header.eut_documentrule_header_id where lines.ad_role_id = '"
              + orgManagerId + "' and header.document_type = '" + doctype + "' )");
          docQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
          count = count + docQuery.list().size();
          if (docQuery != null) {
            if (docQuery.list().size() > 0) {
              for (Iterator iterator = docQuery.list().listIterator(); iterator.hasNext();) {
                Object[] objects = (Object[]) iterator.next();
                docVO = new ApprovalDelegationVO();
                docVO.setPage(page);
                docVO.setTotalpages(totalPages);
                docVO.setCount(count);

                docVO.setDocName(ApprovalDelegation.mapLangDocType.get(vars.getLanguage())
                    .get(objects[0].toString()));
                docVO.setDocType(objects[0].toString());
                docVO.setUserId(objects[1].toString());
                docVO.setType(objects[2].toString());
                docVO.setUserName(objects[3] == null ? "" : objects[3].toString());
                docVO.setRoleId(objects[4].toString());
                docVO.setRoleName(objects[5].toString());
                docLs.add(docVO);
              }
            }
          }

        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getDocumentsForSpecialCaseFRM docLs ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return docLs;
  }

  /**
   * This method is used to custody transfer listing validation
   * 
   * @param vars
   * @param paramPage
   * @param headerId
   * @param clientId
   * @param orgId
   * @param type
   * @param roleId
   * @param userId
   * @param docLs
   * @param count
   * @param totalPages
   * @return
   */
  public List<ApprovalDelegationVO> getDocumentsForSpecialCaseCustody(VariablesSecureApp vars,
      int paramPage, String headerId, String clientId, String orgId, String type, String roleId,
      String userId, List<ApprovalDelegationVO> docLs, int count, int totalPages) {
    SQLQuery docQuery = null;
    StringBuffer query = null;
    ApprovalDelegationVO docVO = null;
    int page = paramPage;
    try {
      OBContext.setAdminMode(true);

      if (type.equals("E")) {
        query = new StringBuffer();
        query.append(" (select cast('EUT_114' as character varying(40)), CAST('' as text) "
            + " as ad_user_id, CAST('N' as text) as type, CAST('' as text) as name, "
            + " CAST('' as text) as ad_role_id, CAST('' as text) as rolename from ad_user a "
            + " left join AD_User_Roles b on a.ad_user_id=b.ad_user_id "
            + " left join ad_role c on b.ad_role_id=c.ad_role_id "
            + " left join AD_Window_Access d on c.ad_role_id=d.ad_role_id " + " where "
            + " a.ad_user_id='" + userId + "' " + " and b.ad_role_id='" + roleId + "' "
            + " and a.ad_client_id='" + clientId + "' "
            + " and d.AD_Window_ID='D6F05B3A695E4D6BB357E1B6686E3D4D')");
        docQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
        count = count + docQuery.list().size();
        if (docQuery != null) {
          if (docQuery.list().size() > 0) {
            for (Iterator iterator = docQuery.list().listIterator(); iterator.hasNext();) {
              Object[] objects = (Object[]) iterator.next();
              docVO = new ApprovalDelegationVO();
              docVO.setPage(page);
              docVO.setTotalpages(totalPages);
              docVO.setCount(count);

              docVO.setDocName(ApprovalDelegation.mapLangDocType.get(vars.getLanguage())
                  .get(objects[0].toString()));
              docVO.setDocType(objects[0].toString());
              docVO.setUserId(objects[1].toString());
              docVO.setType(objects[2].toString());
              docVO.setUserName(objects[3] == null ? "" : objects[3].toString());
              docVO.setRoleId(objects[4].toString());
              docVO.setRoleName(objects[5].toString());
              docLs.add(docVO);
            }
          }
        }

      }

    } catch (Exception e) {
      log4j.error("Exception in getDocumentsForSpecialCaseFRM docLs ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return docLs;
  }

  /**
   * 
   * @param delegationId
   * @return
   */
  public JSONObject checkIsProcessed(String delegationId) {
    final JSONObject response = new JSONObject();
    List<EutDocappDelegate> delegationList = new ArrayList<EutDocappDelegate>();
    try {
      OBContext.setAdminMode(true);
      OBQuery<EutDocappDelegate> delegation = OBDal.getInstance()
          .createQuery(EutDocappDelegate.class, "as e where e.id=:delegationHdrId");
      delegation.setNamedParameter("delegationHdrId", delegationId);
      delegationList = delegation.list();
      if (delegation.list().size() > 0 && delegationList.get(0).isProcessed()) {
        response.put("isProcessed", "Y");
      } else {
        response.put("isProcessed", "N");
      }
    } catch (Exception e) {
      log4j.error("Exception in checkIsProcessed ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return response;
  }

  /**
   * This method is used to display From User, only when the business partner is not n
   * 
   * @param userId
   * @return
   */
  public Boolean checkBusinessPartner(String userId) {
    boolean bpExistsOrNot = false;
    try {
      OBContext.setAdminMode(true);
      List<User> userList = null;
      OBQuery<User> user = OBDal.getInstance().createQuery(User.class, "as e where e.id =:ID");
      user.setNamedParameter("ID", userId);
      userList = user.list();
      if (userList.size() > 0) {
        if (userList.get(0).getBusinessPartner() != null) {
          bpExistsOrNot = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in checkBusinessPartner ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return bpExistsOrNot;
  }

}