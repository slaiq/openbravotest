package sa.elm.ob.utility.ad_forms.nextrolebyrule;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.utility.TreeNode;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.finance.Efin_UserManager;
import sa.elm.ob.hcm.EhcmOrgManager;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.EutDelegateRoleCheck;
import sa.elm.ob.utility.EutLookupAccess;
import sa.elm.ob.utility.ad_forms.delegation.dao.ApprovalDelegationDAO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;
import sa.elm.ob.utility.util.UtilityVO;

public class NextRoleByRuleDAO {
  private Logger log4j = Logger.getLogger(NextRoleByRuleDAO.class);
  private static NextRoleByRuleDAO nextRoleByRuleDAO = null;

  public static NextRoleByRuleDAO getInstance() {
    if (nextRoleByRuleDAO == null) {
      nextRoleByRuleDAO = new NextRoleByRuleDAO();
    }
    return nextRoleByRuleDAO;
  }

  /**
   * Check Access to Window
   * 
   * @param conn
   * @param clientId
   * @param orgId
   * @param roleID
   * @param documentType
   * @return boolean
   */
  @SuppressWarnings("resource")
  public boolean haveAccesstoWindow(String clientId, String paramOrgId, String roleId,
      String userId, String documentNo) {
    Connection conn = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    String orgId = paramOrgId;
    try {
      conn = OBDal.getInstance().getConnection();

      // Getting Organization
      st = conn.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentNo);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      if (orgId.equals("-1")) {
        log4j.debug("Document Rule not set for the document: " + documentNo + ", org: " + orgId);
        return false;
      }

      st = conn.prepareStatement(
          "select qrl.eut_documentrule_lines_id from eut_documentrule_lines qrl join eut_documentrule_header qrh on qrl.eut_documentrule_header_id = qrh.eut_documentrule_header_id "
              + "where qrh.ad_client_id = ? and qrh.ad_org_id = ? and qrl.ad_role_id = ? and qrh.document_type = ?");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, roleId);
      st.setString(4, documentNo);
      rs = st.executeQuery();
      if (rs.next())
        return true;
      String toUserId = new ApprovalDelegationDAO(conn).getIsWindowDelegated(userId, documentNo);
      if (toUserId != null && toUserId.length() == 32)
        return true;
    } catch (final Exception e) {
      log4j.error("Error in haveAccesstoWindow() Method : ", e);
      return false;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {

      }
    }
    return false;
  }

  /**
   * This method is used to get Next role list
   * 
   * @param con
   * @param clientId
   * @param parmOrgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return list
   */
  @SuppressWarnings("resource")
  public List<NextRoleByRuleVO> getNextRoleList(Connection con, String clientId, String parmOrgId,
      String roleId, String userId, String documentType, Object pValue) {
    ResultSet rs = null;
    PreparedStatement st = null;
    NextRoleByRuleVO vo = null;
    List<NextRoleByRuleVO> list = new ArrayList<NextRoleByRuleVO>();
    BigDecimal value = new BigDecimal(0);
    String orgId = parmOrgId;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      if (pValue instanceof Double)
        value = new BigDecimal((Double) pValue);
      else if (pValue instanceof Float)
        value = new BigDecimal((Float) pValue);
      else
        value = new BigDecimal(pValue.toString());

      /*
       * st = con.prepareStatement(
       * "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
       * +
       * "join (select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno, qdrl.ad_role_id from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
       * +
       * "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ? and qdrh.rulevalue <= ? "
       * +
       * " order by qdrh.rulevalue desc limit 1) qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
       * +
       * "join (select ln.eut_documentrule_header_id,ln.rolesequenceno,ln.ad_role_id from eut_documentrule_lines ln where ad_role_id=?) ln "
       * + " on qdrh.eut_documentrule_header_id = ln.eut_documentrule_header_id " +
       * "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where qdrl.ad_client_id = ? and qdrl.ad_org_id= ? and qdrl.rolesequenceno = (ln.rolesequenceno)+1 order by qdrl.roleorderno;"
       * );
       */

      st = con.prepareStatement(
          "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno,"
              + " qdrl.roleorderno from eut_documentrule_lines qdrl "
              + "join (select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno, qdrl.ad_role_id "
              + "from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ? and qdrh.rulevalue <= ? and qdrl.ad_role_id=? "
              + "order by qdrh.rulevalue desc limit 1) qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where qdrl.ad_client_id = ? and qdrl.ad_org_id= ? "
              + "and qdrl.rolesequenceno = (qdrh.rolesequenceno)+1 order by qdrl.roleorderno;");

      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setBigDecimal(4, value);
      st.setString(5, roleId);
      st.setString(6, clientId);
      st.setString(7, orgId);
      log4j.debug("getNextRoleList qry:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        vo = new NextRoleByRuleVO();
        vo.setNextRoleId(rs.getString("ad_role_id"));
        vo.setNextRoleName(rs.getString("name"));
        vo.setRuleSeqNo(rs.getInt("ruleseqno"));
        vo.setRoleSeqNo(rs.getInt("roleorderno"));
        list.add(vo);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRoleByRule() Method", e);
      return null;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {

      }
    }
    return list;
  }

  /**
   * This method is used to check backward delegation
   * 
   * @param con
   * @param clientId
   * @param paramOrgId
   * @param roleId
   * @param toRoleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return
   */
  @SuppressWarnings("resource")
  public boolean isBackwardDelegation(Connection con, String clientId, String paramOrgId,
      String roleId, String toRoleId, String userId, String documentType, Object pValue) {
    ResultSet rs = null;
    PreparedStatement st = null;
    BigDecimal value = new BigDecimal(0);
    String orgId = paramOrgId;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      if (pValue instanceof Double)
        value = new BigDecimal((Double) pValue);
      else if (pValue instanceof Float)
        value = new BigDecimal((Float) pValue);
      else
        value = new BigDecimal(pValue.toString());

      /*
       * st = con.prepareStatement(
       * "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
       * +
       * "join (select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno, qdrl.ad_role_id from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
       * +
       * "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ? and qdrh.rulevalue <= ? "
       * +
       * " order by qdrh.rulevalue desc limit 1) qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
       * +
       * "join (select ln.eut_documentrule_header_id,ln.rolesequenceno,ln.ad_role_id from eut_documentrule_lines ln where ad_role_id=?) ln "
       * + " on qdrh.eut_documentrule_header_id = ln.eut_documentrule_header_id " +
       * "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where qdrl.ad_client_id = ? and qdrl.ad_org_id= ? and qdrl.rolesequenceno < (ln.rolesequenceno) order by qdrl.roleorderno;"
       * );
       */

      st = con.prepareStatement(
          "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
              + "join (select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno, qdrl.ad_role_id from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ? and qdrh.rulevalue <= ? and qdrl.ad_role_id= ? "
              + "order by qdrh.rulevalue desc limit 1) qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where qdrl.ad_client_id = ? and qdrl.ad_org_id= ? and qdrl.rolesequenceno < (qdrh.rolesequenceno) order by qdrl.roleorderno;");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setBigDecimal(4, value);
      st.setString(5, roleId);
      st.setString(6, clientId);
      st.setString(7, orgId);
      log4j.debug("isBackwardDelegation qry:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        if (rs.getString("ad_role_id").equals(toRoleId)) {
          return true;
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRoleByRule() Method", e);
      return false;
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {

      }
    }
    return false;
  }

  /**
   * This method is used to get next role
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return
   */
  public NextRoleByRuleVO getNextRole(Connection con, String clientId, String orgId, String roleId,
      String userId, String documentType, Object pValue) {
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    try {
      ruleVO = new NextRoleByRuleVO();
      List<NextRoleByRuleVO> list = getNextRoleList(con, clientId, orgId, roleId, userId,
          documentType, pValue);
      if (list == null || list.size() == 0) {
        ruleVO.setStatus(Constants.sAPPROVED);
        ruleVO.setShortStatus(Constants.vAPPROVED);
        ruleVO.setFullStatus(Constants.sAPPROVED);
        ruleVO.setNextRoleId(null);
        ruleVO.setApproval(false);
      } else {
        String status = "";

        // Create QU Next Role
        String headerId = SequenceIdData.getUUID();
        st = con.prepareStatement(
            "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
        st.setString(1, headerId);
        st.setString(2, clientId);
        st.setString(3, orgId);
        st.setString(4, userId);
        st.setString(5, userId);
        st.setString(6, documentType);
        st.executeUpdate();

        st = con.prepareStatement(
            "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, rolesequenceno) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?);");
        for (NextRoleByRuleVO vo : list) {
          st.setString(1, clientId);
          st.setString(2, orgId);
          st.setString(3, userId);
          st.setString(4, userId);
          st.setString(5, headerId);
          st.setString(6, vo.getNextRoleId());
          st.setInt(7, vo.getRoleSeqNo());
          st.addBatch();
          // status += (" / " + vo.getNextRoleName());
          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + vo.getNextRoleId() + "'");
          if (ur != null && ur.list().size() > 0) {
            if (ur.list().size() == 1) {
              status += (" / " + ur.list().get(0).getRole().getName() + " / "
                  + ur.list().get(0).getUserContact().getName());
            } else {
              status += (" / " + ur.list().get(0).getRole().getName());
            }

          }
          // Role does not have user associated throw error to user
          else {
            Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
            ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
            ruleVO.setRoleName(objRole.getName());
            // OBMessageUtils.messageBD("EUT_NOUser_ForRoles").replaceAll("@", objRole.getName()));
            return ruleVO;
          }

        }
        st.executeBatch();
        ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        log4j.debug("getNextRoleList qry:" + ruleVO.getStatus());

        ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
        ruleVO.setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setNextRoleId(headerId);
        ruleVO.setApproval(true);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * This method is used to get invoice next role
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @param invoiceId
   * @return
   */
  public NextRoleByRuleVO getInvNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, Object pValue, String invoiceId) {
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    Boolean chkDeptRole = false, chkUserIsDeptHead = false, chkRoleIsInDocRule = false,
        chkappdelegation = false, chkMoThanOneDeptHeadRole = false;
    String DeptRoleId = null, DeptManagerId = null, DeptHeadRoleId = null;
    ResultSet rs = null;
    List<NextRoleByRuleVO> list = null;
    int count = 0;
    NextRoleByRuleVO nextApproval = null;
    Boolean lineManagerRole = false;
    Boolean lineDepMansame = false;
    try {
      ruleVO = new NextRoleByRuleVO();

      list = getNextRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      Invoice header = OBDal.getInstance().get(Invoice.class, invoiceId);
      String costCenterId = header.getEfinCSalesregion().getId();
      chkRoleIsInDocRule = UtilityDAO.chkRoleIsInDocRul(con, clientId, orgId, userId, roleId,
          documentType, pValue);
      if (list.size() == 0) {
        // based on current user check that particular user is department head or not
        chkUserIsDeptHead = UtilityDAO.chkUserIsDeptHead(clientId, orgId, userId, costCenterId);
        // if (chkUserIsDeptHead) {
        // st = con.prepareStatement(
        // " select count(c_invoice_id) from c_invoice where c_invoice_id='" + invoiceId
        // + "' and em_efin_c_salesregion_id in (select c_salesregion_id from c_salesregion where
        // em_efin_user_id "
        // + " ='" + userId + "')");
        // rs = st.executeQuery();
        // if (rs.next()) {
        // if (rs.getInt("count") > 0) {
        // // chk more than one department head role in client
        // chkMoThanOneDeptHeadRole = UtilityDAO.chkMoThanOneDeptHeadRole(clientId, orgId);
        // if (!chkMoThanOneDeptHeadRole) {
        // DeptHeadRoleId = UtilityDAO.getDeptHeadRole(clientId, orgId, documentType, pValue);
        // if (DeptHeadRoleId != null) {
        // list = getNextRoleList(con, clientId, orgId, DeptHeadRoleId, userId, documentType,
        // pValue);
        // chkRoleIsInDocRule = UtilityDAO.chkRoleIsInDocRul(con, clientId, orgId, userId,
        // DeptHeadRoleId, documentType, pValue);
        // }
        // } else {
        // ruleVO.setNextRoleId(null);
        // ruleVO.setErrorMsg("Efin_PurInv_MoreThanDeptRole");
        // }
        //
        // }
        // }
        // }
      }
      // Line manager
      if (list != null) {
        for (NextRoleByRuleVO vo : list) {
          Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
          if (objRole.isEscmIslinemanager()) {
            lineManagerRole = true;
          }
        }
      }

      if (!chkUserIsDeptHead) {
        // chk more than one department head role in client
        chkMoThanOneDeptHeadRole = UtilityDAO.chkMoThanOneDeptHeadRole(clientId, orgId);
        if (!chkMoThanOneDeptHeadRole) {
          // check next role is department head or not
          for (NextRoleByRuleVO vo : list) {
            chkDeptRole = UtilityDAO.chkRoleIsDeptMang(clientId, vo.getNextRoleId());

            if (chkDeptRole) {
              DeptRoleId = vo.getNextRoleId();
              break;
            }
          }

          //
          if (chkDeptRole && DeptRoleId != null) {
            /*
             * for (NextRoleByRuleVO vo : list) { list.remove(vo); }
             */
            list.clear();

            // get department manager userId
            st = con.prepareStatement(
                " select em_efin_user_id  as userId from c_salesregion where c_salesregion_id in ( select c_salesregion_id from efin_user_manager where ad_user_id ='"
                    + header.getCreatedBy().getId()
                    + "' and document_type ='API' and ad_client_id ='" + header.getClient().getId()
                    + "' )");
            rs = st.executeQuery();
            if (rs.next()) {
              DeptManagerId = rs.getString("userId");
            }
            if (DeptManagerId != null) {
              st = con.prepareStatement(
                  " select url.ad_role_id,rl.name, rl.em_efin_departmenthead from ad_user_roles  url left join ad_role rl on rl.ad_role_id=url.ad_role_id  where ad_user_id='"
                      + DeptManagerId + "' and em_efin_departmenthead ='Y' ");
              rs = st.executeQuery();
              NextRoleByRuleVO vo = null;
              while (rs.next()) {
                count++;
                vo = new NextRoleByRuleVO();
                vo.setNextRoleId(rs.getString("ad_role_id"));
                vo.setNextRoleName(rs.getString("name"));
                vo.setRoleSeqNo(count);
                list.add(vo);
              }
              if (count == 0) {
                ruleVO.setNextRoleId(null);
                ruleVO.setErrorMsg("EFIN_DeptManagRoleNotAssoicated");
              }
            } else {
              ruleVO.setNextRoleId(null);
              ruleVO.setErrorMsg("EFIN_DeptManagNotAssoicated");
            }
          }
        } else {
          ruleVO.setNextRoleId(null);
          ruleVO.setErrorMsg("Efin_PurInv_MoreThanDeptRole");
        }
      }

      if (lineManagerRole) {
        if (chkDeptRole) {
          lineDepMansame = true;
        }
      }
      if (lineManagerRole) {
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, orgId, roleId, userId, documentType,
            pValue, userId, false, header.getDocumentStatus());

        if (nextApproval != null && nextApproval.getErrorMsg() != null
            && (nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")
                || (nextApproval.getErrorMsg().equals("Managernotdefined")))) {
          ruleVO.setErrorMsg("Escm_No_LineManager");
          return ruleVO;
        } else if (nextApproval != null && nextApproval.getErrorMsg() == null) {
          if (!lineDepMansame) {
            return nextApproval;
          }
        }
      }

      if ((!lineManagerRole) || (lineDepMansame)) {
        chkappdelegation = UtilityDAO.chkRoleIsInAppDelegation(con, clientId, orgId, userId, roleId,
            invoiceId);
        if (!chkRoleIsInDocRule && ruleVO.getErrorMsg() == null) {
          if (!chkappdelegation) {
            ruleVO.setNextRoleId(null);
            ruleVO.setErrorMsg("Efin_PurInv_CantComplete");
          }
        }
        if ((chkRoleIsInDocRule && ruleVO.getErrorMsg() == null) || (chkappdelegation)) {
          if (list == null || list.size() == 0) {
            ruleVO.setStatus(Constants.sAPPROVED);
            ruleVO.setShortStatus(Constants.vAPPROVED);
            ruleVO.setFullStatus(Constants.sAPPROVED);
            ruleVO.setNextRoleId(null);
            ruleVO.setApproval(false);
            ruleVO.setErrorMsg(null);

          } else {
            String status = "";
            String headerId = SequenceIdData.getUUID();
            // Create QU Next Role
            if (!lineDepMansame) {

              st = con.prepareStatement(
                  "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
              st.setString(1, headerId);
              st.setString(2, clientId);
              st.setString(3, orgId);
              st.setString(4, userId);
              st.setString(5, userId);
              st.setString(6, documentType);
              st.executeUpdate();
            }
            st = con.prepareStatement(
                "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, rolesequenceno,ad_user_id) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?,?);");
            for (NextRoleByRuleVO vo : list) {
              st.setString(1, clientId);
              st.setString(2, orgId);
              st.setString(3, userId);
              st.setString(4, userId);
              if (!lineDepMansame) {
                st.setString(5, headerId);
              } else {
                st.setString(5, nextApproval.getNextRoleId());
              }
              st.setString(6, vo.getNextRoleId());
              st.setInt(7, vo.getRoleSeqNo());
              if (DeptManagerId != null) {
                st.setString(8, DeptManagerId);
              } else
                st.setString(8, null);

              st.addBatch();
              status += (" / " + vo.getNextRoleName());
            }
            st.executeBatch();

            if (!lineDepMansame) {
              ruleVO
                  .setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
            } else {
              ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL,
                  status.substring(3) + "/" + nextApproval.getStatus()));

            }
            ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
            ruleVO.setFullStatus(
                String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
            if (!lineDepMansame) {
              ruleVO.setNextRoleId(headerId);

            } else {
              ruleVO.setNextRoleId(nextApproval.getNextRoleId());

            }
            ruleVO.setApproval(true);
            ruleVO.setNextApprover(status.substring(3));
            ruleVO.setErrorMsg(null);
          }
        }
      }
    } catch (

    final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * This method is used to get custody transfer next role
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param header
   * @param NextUserId
   * @param approvalFlow
   * @param approvalLevel
   * @return
   */
  public NextRoleByRuleVO getCustTranNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, ShipmentInOut header, String NextUserId,
      String approvalFlow, Long approvalLevel) {
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    String status = "", userid = "";
    List<UtilityVO> ls = new ArrayList<UtilityVO>();
    try {
      ruleVO = new NextRoleByRuleVO();

      // get Next Approver UserId

      if (approvalFlow.equals("1")) {
        if (header.getEutNextRole() == null) {
          // CurrentUserId= header.getEscmCtsendlinemng().getId());
          ls = UtilityDAO.getUserRole(header.getEscmCtsender().getId());
          status = header.getEscmCtsender().getBusinessPartner().getName();
          userid = header.getEscmCtsender().getName();
        } else if (NextUserId.equals(header.getEscmCtsender().getId())
            && approvalLevel == Long.valueOf(1)) {
          // CurrentUserId= header.getEscmCtreceiver().getId());
          ls = UtilityDAO.getUserRole(header.getEscmCtsendlinemng().getId());
          status = header.getEscmCtsendlinemng().getBusinessPartner().getName();
          userid = header.getEscmCtsendlinemng().getName();
        } else if (NextUserId.equals(header.getEscmCtsendlinemng().getId())
            && approvalLevel == Long.valueOf(2)) {
          // CurrentUserId= header.getEscmCtreceiver().getId());
          ls = UtilityDAO.getUserRole(header.getEscmCtreceiver().getId());
          status = header.getEscmCtreceiver().getBusinessPartner().getName();
          userid = header.getEscmCtreceiver().getName();
        } else if (NextUserId.equals(header.getEscmCtreceiver().getId())
            && approvalLevel == Long.valueOf(3)) {
          // CurrentUserId= header.getEscmCtreclinemng().getId());
          ls = UtilityDAO.getUserRole(header.getEscmCtreclinemng().getId());
          status = header.getEscmCtreclinemng().getBusinessPartner().getName();
          userid = header.getEscmCtreclinemng().getName();
        }
      } else {
        if (header.getEutNextRole() == null) {
          // CurrentUserId= header.getEscmCtsendlinemng().getId());
          ls = UtilityDAO.getUserRole(header.getEscmCtsendlinemng().getId());
          status = header.getEscmCtsendlinemng().getBusinessPartner().getName();
          userid = header.getEscmCtsendlinemng().getName();
        } else if (NextUserId.equals(header.getEscmCtsendlinemng().getId())
            && approvalLevel.equals(Long.valueOf(2))) {
          // CurrentUserId= header.getEscmCtreceiver().getId());
          ls = UtilityDAO.getUserRole(header.getEscmCtreceiver().getId());
          status = header.getEscmCtreceiver().getBusinessPartner().getName();
          userid = header.getEscmCtreceiver().getName();
        } else if (NextUserId.equals(header.getEscmCtreceiver().getId())
            && approvalLevel.equals(Long.valueOf(2))) {
          // CurrentUserId= header.getEscmCtreclinemng().getId());
          ls = UtilityDAO.getUserRole(header.getEscmCtreclinemng().getId());
          status = header.getEscmCtreclinemng().getBusinessPartner().getName();
          userid = header.getEscmCtreclinemng().getName();
        }
      }
      if (ls.size() == 0) {
        // user does not have role associated throw error
        ruleVO.setErrorMsg("EUT_NORole_ForUser");
        ruleVO.setUserName(userid);
        return ruleVO;
      }

      // Create QU Next Role
      String headerId = SequenceIdData.getUUID();
      st = con.prepareStatement(
          "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
      st.setString(1, headerId);
      st.setString(2, clientId);
      st.setString(3, orgId);
      st.setString(4, userId);
      st.setString(5, userId);
      st.setString(6, documentType);
      st.executeUpdate();
      for (UtilityVO vo : ls) {
        st = con.prepareStatement(
            "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_user_id,ad_role_id) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?,?);");
        st.setString(1, clientId);
        st.setString(2, orgId);
        st.setString(3, userId);
        st.setString(4, userId);
        st.setString(5, headerId);
        st.setString(6, vo.getUserId());
        st.setString(7, vo.getRoleId());

        st.executeUpdate();
      }
      ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status));
      ruleVO.setNextRoleId(headerId);
      ruleVO.setApproval(true);
    } catch (final Exception e) {
      log4j.error("Exception in getCustTranNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  public List<NextRoleByRuleVO> getDelegatedNextRoleList(Connection con, String clientId,
      String paramOrgId, String fromUserRoleId, String toUserRoleId, String userId,
      String documentType, Object pValue) {
    ResultSet rs = null;
    PreparedStatement st = null, st1 = null, st2 = null, st3 = null;
    List<NextRoleByRuleVO> list = new ArrayList<NextRoleByRuleVO>();
    BigDecimal value = new BigDecimal(0);
    NextRoleByRuleVO vo = null;
    String orgId = paramOrgId;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      if (pValue instanceof Double)
        value = new BigDecimal((Double) pValue);
      else if (pValue instanceof Float)
        value = new BigDecimal((Float) pValue);
      else
        value = new BigDecimal(pValue.toString());

      // Getting Next Role
      // Getting Rolesequenceno of From User Role Id
      String sql = " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
          + " where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ?   ";
      sql += " and qdrh.rulevalue <= ? and qdrl.ad_role_id= ? order by qdrh.rulevalue desc limit 1 ";
      st = con.prepareStatement(sql);
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setBigDecimal(4, value);
      st.setString(5, fromUserRoleId);// From User Role Id
      rs = st.executeQuery();
      if (rs.next()) {
        sql = " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
            + " where qdrh.eut_documentrule_header_id = ? and qdrl.ad_role_id= ? and qdrl.rolesequenceno > ?";
        st = con.prepareStatement(sql);
        st.setString(1, rs.getString("eut_documentrule_header_id"));
        st.setString(2, toUserRoleId);// To User Role Id
        st.setInt(3, rs.getInt("rolesequenceno"));
        ResultSet rs1 = st.executeQuery();
        if (rs1.next()) {
          st = con.prepareStatement(
              "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
                  + "join eut_documentrule_header qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
                  + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where  "
                  + " qdrh.eut_documentrule_header_id = ? and qdrl.rolesequenceno = ? order by qdrl.roleorderno;");
          st.setString(1, rs1.getString("eut_documentrule_header_id"));
          if (rs1.getInt("rolesequenceno") == (rs.getInt("rolesequenceno") + 1))
            st.setInt(2, rs1.getInt("rolesequenceno") + 1);
          else
            st.setInt(2, rs.getInt("rolesequenceno") + 1);
          ResultSet rs2 = st.executeQuery();
          while (rs2.next()) {
            // Delegated User - To User Roles Next Role Details
            vo = new NextRoleByRuleVO();
            vo.setNextRoleId(rs2.getString("ad_role_id"));
            vo.setNextRoleName(rs2.getString("name"));
            vo.setRuleSeqNo(rs2.getInt("ruleseqno"));
            vo.setRoleSeqNo(rs2.getInt("roleorderno"));
            list.add(vo);
          }
        } else {
          // To User Role does not exist in Document Rule for higher approval. So follow document
          // rule of from user role
          st1 = con.prepareStatement(
              "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
                  + "join eut_documentrule_header qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
                  + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where  "
                  + " qdrh.eut_documentrule_header_id = ? and qdrl.rolesequenceno = ? order by qdrl.roleorderno;");
          st1.setString(1, rs.getString("eut_documentrule_header_id"));
          st1.setInt(2, rs.getInt("rolesequenceno") + 1);
          ResultSet rs2 = st1.executeQuery();
          while (rs2.next()) {
            // Delegated User - To User Roles Next Role Details
            vo = new NextRoleByRuleVO();
            vo.setNextRoleId(rs2.getString("ad_role_id"));
            vo.setNextRoleName(rs2.getString("name"));
            vo.setRuleSeqNo(rs2.getInt("ruleseqno"));
            vo.setRoleSeqNo(rs2.getInt("roleorderno"));
            list.add(vo);
          }
        }
      } else {
        sql = " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
            + " where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ? and qdrh.rulevalue <= ? and qdrl.ad_role_id= ? order by qdrh.rulevalue desc limit 1 ";
        st1 = con.prepareStatement(sql);
        st1.setString(1, clientId);
        st1.setString(2, orgId);
        st1.setString(3, documentType);
        st1.setBigDecimal(4, value);
        st1.setString(5, fromUserRoleId);// From User Role Id
        rs = st1.executeQuery();
        if (rs.next()) {
          sql = " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + " where qdrh.eut_documentrule_header_id = ? and qdrl.ad_role_id= ? and qdrl.rolesequenceno > ?";
          st2 = con.prepareStatement(sql);
          st2.setString(1, rs.getString("eut_documentrule_header_id"));
          st2.setString(2, toUserRoleId);// To User Role Id
          st2.setInt(3, rs.getInt("rolesequenceno"));
          ResultSet rs1 = st2.executeQuery();
          if (rs1.next()) {
            st3 = con.prepareStatement(
                "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
                    + "join eut_documentrule_header qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
                    + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where  "
                    + " qdrh.eut_documentrule_header_id = ? and qdrl.rolesequenceno = ? order by qdrl.roleorderno;");

            st3.setString(1, rs1.getString("eut_documentrule_header_id"));
            st3.setInt(2, rs1.getInt("rolesequenceno") + 1);
            ResultSet rs2 = st3.executeQuery();
            while (rs2.next()) {
              // Delegated User - To User Roles Next Role Details
              vo = new NextRoleByRuleVO();
              vo.setNextRoleId(rs2.getString("ad_role_id"));
              vo.setNextRoleName(rs2.getString("name"));
              vo.setRuleSeqNo(rs2.getInt("ruleseqno"));
              vo.setRoleSeqNo(rs2.getInt("roleorderno"));
              list.add(vo);
            }
          } else {
            // To User Role does not exist in Document Rule for higher approval. So follow document
            // rule of from user role
            st3 = con.prepareStatement(
                "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
                    + "join eut_documentrule_header qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
                    + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where  "
                    + " qdrh.eut_documentrule_header_id = ? and qdrl.rolesequenceno = ? order by qdrl.roleorderno;");
            st3.setString(1, rs.getString("eut_documentrule_header_id"));
            st3.setInt(2, rs.getInt("rolesequenceno") + 1);
            ResultSet rs2 = st3.executeQuery();
            while (rs2.next()) {
              // Delegated User - To User Roles Next Role Details
              vo = new NextRoleByRuleVO();
              vo.setNextRoleId(rs2.getString("ad_role_id"));
              vo.setNextRoleName(rs2.getString("name"));
              vo.setRuleSeqNo(rs2.getInt("ruleseqno"));
              vo.setRoleSeqNo(rs2.getInt("roleorderno"));
              list.add(vo);
            }
          }
        }
      }

    } catch (final Exception e) {
      log4j.error("Exception in getNextRoleByRule() Method", e);
      return null;
    }
    return list;
  }

  /**
   * This method is used to get requester delegated next role list
   * 
   * @param con
   * @param clientId
   * @param paramOrgId
   * @param fromUserRoleId
   * @param toUserRoleId
   * @param userId
   * @param documentType
   * @param reqRole
   * @return
   */
  public List<NextRoleByRuleVO> getRequesterDelegatedNextRoleList(Connection con, String clientId,
      String paramOrgId, String fromUserRoleId, String toUserRoleId, String userId,
      String documentType, String reqRole) {
    ResultSet rs = null;
    PreparedStatement st = null, st1 = null, st2 = null, st3 = null;
    List<NextRoleByRuleVO> list = new ArrayList<NextRoleByRuleVO>();
    NextRoleByRuleVO vo = null;
    String orgId = paramOrgId;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      // Getting Next Role
      // Getting Rolesequenceno of From User Role Id
      String sql = " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
          + " where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ?   ";
      sql += " and qdrh.requester_role = ? and qdrl.ad_role_id= ? order by qdrh.rulevalue desc limit 1 ";
      st = con.prepareStatement(sql);
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setString(4, reqRole);
      st.setString(5, fromUserRoleId);// From User Role Id
      rs = st.executeQuery();
      if (rs.next()) {
        sql = " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
            + " where qdrh.eut_documentrule_header_id = ? and qdrl.ad_role_id= ? and qdrl.rolesequenceno > ?";
        st = con.prepareStatement(sql);
        st.setString(1, rs.getString("eut_documentrule_header_id"));
        st.setString(2, toUserRoleId);// To User Role Id
        st.setInt(3, rs.getInt("rolesequenceno"));
        ResultSet rs1 = st.executeQuery();
        if (rs1.next()) {
          st = con.prepareStatement(
              "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
                  + "join eut_documentrule_header qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
                  + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where  "
                  + " qdrh.eut_documentrule_header_id = ? and qdrl.rolesequenceno = ? order by qdrl.roleorderno;");
          st.setString(1, rs1.getString("eut_documentrule_header_id"));
          if (rs1.getInt("rolesequenceno") == (rs.getInt("rolesequenceno") + 1))
            st.setInt(2, rs1.getInt("rolesequenceno") + 1);
          else
            st.setInt(2, rs.getInt("rolesequenceno") + 1);
          ResultSet rs2 = st.executeQuery();
          while (rs2.next()) {
            // Delegated User - To User Roles Next Role Details
            vo = new NextRoleByRuleVO();
            vo.setNextRoleId(rs2.getString("ad_role_id"));
            vo.setNextRoleName(rs2.getString("name"));
            vo.setRuleSeqNo(rs2.getInt("ruleseqno"));
            vo.setRoleSeqNo(rs2.getInt("roleorderno"));
            list.add(vo);
          }
        } else {
          // To User Role does not exist in Document Rule for higher approval. So follow document
          // rule of from user role
          st1 = con.prepareStatement(
              "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
                  + "join eut_documentrule_header qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
                  + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where  "
                  + " qdrh.eut_documentrule_header_id = ? and qdrl.rolesequenceno = ? order by qdrl.roleorderno;");
          st1.setString(1, rs.getString("eut_documentrule_header_id"));
          st1.setInt(2, rs.getInt("rolesequenceno") + 1);
          ResultSet rs2 = st1.executeQuery();
          while (rs2.next()) {
            // Delegated User - To User Roles Next Role Details
            vo = new NextRoleByRuleVO();
            vo.setNextRoleId(rs2.getString("ad_role_id"));
            vo.setNextRoleName(rs2.getString("name"));
            vo.setRuleSeqNo(rs2.getInt("ruleseqno"));
            vo.setRoleSeqNo(rs2.getInt("roleorderno"));
            list.add(vo);
          }
        }
      } else {
        sql = " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
            + " where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ? and qdrh.rulevalue <= ? and qdrl.ad_role_id= ? order by qdrh.rulevalue desc limit 1 ";
        st1 = con.prepareStatement(sql);
        st1.setString(1, clientId);
        st1.setString(2, orgId);
        st1.setString(3, documentType);
        st.setString(4, reqRole);
        st1.setString(5, fromUserRoleId);// From User Role Id
        rs = st1.executeQuery();
        if (rs.next()) {
          sql = " select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + " where qdrh.eut_documentrule_header_id = ? and qdrl.ad_role_id= ? and qdrl.rolesequenceno > ?";
          st2 = con.prepareStatement(sql);
          st2.setString(1, rs.getString("eut_documentrule_header_id"));
          st2.setString(2, toUserRoleId);// To User Role Id
          st2.setInt(3, rs.getInt("rolesequenceno"));
          ResultSet rs1 = st2.executeQuery();
          if (rs1.next()) {
            st3 = con.prepareStatement(
                "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
                    + "join eut_documentrule_header qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
                    + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where  "
                    + " qdrh.eut_documentrule_header_id = ? and qdrl.rolesequenceno = ? order by qdrl.roleorderno;");

            st3.setString(1, rs1.getString("eut_documentrule_header_id"));
            st3.setInt(2, rs1.getInt("rolesequenceno") + 1);
            ResultSet rs2 = st3.executeQuery();
            while (rs2.next()) {
              // Delegated User - To User Roles Next Role Details
              vo = new NextRoleByRuleVO();
              vo.setNextRoleId(rs2.getString("ad_role_id"));
              vo.setNextRoleName(rs2.getString("name"));
              vo.setRuleSeqNo(rs2.getInt("ruleseqno"));
              vo.setRoleSeqNo(rs2.getInt("roleorderno"));
              list.add(vo);
            }
          } else {
            // To User Role does not exist in Document Rule for higher approval. So follow document
            // rule of from user role
            st3 = con.prepareStatement(
                "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
                    + "join eut_documentrule_header qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
                    + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where  "
                    + " qdrh.eut_documentrule_header_id = ? and qdrl.rolesequenceno = ? order by qdrl.roleorderno;");
            st3.setString(1, rs.getString("eut_documentrule_header_id"));
            st3.setInt(2, rs.getInt("rolesequenceno") + 1);
            ResultSet rs2 = st3.executeQuery();
            while (rs2.next()) {
              // Delegated User - To User Roles Next Role Details
              vo = new NextRoleByRuleVO();
              vo.setNextRoleId(rs2.getString("ad_role_id"));
              vo.setNextRoleName(rs2.getString("name"));
              vo.setRuleSeqNo(rs2.getInt("ruleseqno"));
              vo.setRoleSeqNo(rs2.getInt("roleorderno"));
              list.add(vo);
            }
          }
        }
      }

    } catch (final Exception e) {
      log4j.error("Exception in getNextRoleByRule() Method", e);
      return null;
    }
    return list;
  }

  /**
   * This method is used to get delegated next role
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param fromUserRoleId
   * @param toUserRoleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return
   */
  public NextRoleByRuleVO getDelegatedNextRole(Connection con, String clientId, String orgId,
      String fromUserRoleId, String toUserRoleId, String userId, String documentType,
      Object pValue) {
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    try {
      ruleVO = new NextRoleByRuleVO();
      List<NextRoleByRuleVO> list = getDelegatedNextRoleList(con, clientId, orgId, fromUserRoleId,
          toUserRoleId, userId, documentType, pValue);
      if (list == null || list.size() == 0) {
        ruleVO.setStatus(Constants.sAPPROVED);
        ruleVO.setShortStatus(Constants.vAPPROVED);
        ruleVO.setFullStatus(Constants.sAPPROVED);
        ruleVO.setNextRoleId(null);
        ruleVO.setApproval(false);
      } else {
        String status = "";

        // Create QU Next Role
        String headerId = SequenceIdData.getUUID();
        st = con.prepareStatement(
            "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
        st.setString(1, headerId);
        st.setString(2, clientId);
        st.setString(3, orgId);
        st.setString(4, userId);
        st.setString(5, userId);
        st.setString(6, documentType);
        st.executeUpdate();

        st = con.prepareStatement(
            "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, rolesequenceno) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?);");
        for (NextRoleByRuleVO vo : list) {
          st.setString(1, clientId);
          st.setString(2, orgId);
          st.setString(3, userId);
          st.setString(4, userId);
          st.setString(5, headerId);
          st.setString(6, vo.getNextRoleId());
          st.setInt(7, vo.getRoleSeqNo());
          st.addBatch();
          // status += (" / " + vo.getNextRoleName());
          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + vo.getNextRoleId() + "'");
          if (ur != null && ur.list().size() > 0) {
            if (ur.list().size() == 1) {
              status += (" / " + ur.list().get(0).getRole().getName() + " / "
                  + ur.list().get(0).getUserContact().getName());
            } else {
              status += (" / " + ur.list().get(0).getRole().getName());
            }

          }
          // Role does not have user associated throw error to user
          else {
            Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
            ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
            ruleVO.setRoleName(objRole.getName());
            return ruleVO;
          }
        }
        st.executeBatch();

        ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
        ruleVO.setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setNextRoleId(headerId);
        ruleVO.setApproval(true);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * This method is used to get requester delegated next role
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param fromUserRoleId
   * @param toUserRoleId
   * @param userId
   * @param documentType
   * @param reqRole
   * @return
   */
  public NextRoleByRuleVO getRequesterDelegatedNextRole(Connection con, String clientId,
      String orgId, String fromUserRoleId, String toUserRoleId, String userId, String documentType,
      String reqRole) {
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    try {
      ruleVO = new NextRoleByRuleVO();
      List<NextRoleByRuleVO> list = getRequesterDelegatedNextRoleList(con, clientId, orgId,
          fromUserRoleId, toUserRoleId, userId, documentType, reqRole);
      if (list == null || list.size() == 0) {
        ruleVO.setStatus(Constants.sAPPROVED);
        ruleVO.setShortStatus(Constants.vAPPROVED);
        ruleVO.setFullStatus(Constants.sAPPROVED);
        ruleVO.setNextRoleId(null);
        ruleVO.setApproval(false);
      } else {
        String status = "";

        // Create QU Next Role
        String headerId = SequenceIdData.getUUID();
        st = con.prepareStatement(
            "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
        st.setString(1, headerId);
        st.setString(2, clientId);
        st.setString(3, orgId);
        st.setString(4, userId);
        st.setString(5, userId);
        st.setString(6, documentType);
        st.executeUpdate();

        st = con.prepareStatement(
            "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, rolesequenceno) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?);");
        for (NextRoleByRuleVO vo : list) {
          st.setString(1, clientId);
          st.setString(2, orgId);
          st.setString(3, userId);
          st.setString(4, userId);
          st.setString(5, headerId);
          st.setString(6, vo.getNextRoleId());
          st.setInt(7, vo.getRoleSeqNo());
          st.addBatch();
          // status += (" / " + vo.getNextRoleName());
          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + vo.getNextRoleId() + "'");
          if (ur != null && ur.list().size() > 0) {
            if (ur.list().size() == 1) {
              status += (" / " + ur.list().get(0).getRole().getName() + " / "
                  + ur.list().get(0).getUserContact().getName());
            } else {
              status += (" / " + ur.list().get(0).getRole().getName());
            }
          }
          // Role does not have user associated throw error to user
          else {
            Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
            ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
            ruleVO.setRoleName(objRole.getName());
            return ruleVO;
          }
        }
        st.executeBatch();

        ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
        ruleVO.setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setNextRoleId(headerId);
        ruleVO.setApproval(true);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * This method is used to get delegated from and to roles
   * 
   * @param con
   * @param clientId
   * @param paramOrgId
   * @param userId
   * @param documentType
   * @param quNextRoleId
   * @return
   */
  public HashMap<String, String> getDelegatedFromAndToRoles(Connection con, String clientId,
      String paramOrgId, String userId, String documentType, String quNextRoleId) {
    PreparedStatement st = null, st1 = null;
    ResultSet rs = null, rs1 = null;
    HashMap<String, String> roleId = new HashMap<String, String>();
    String orgId = paramOrgId;
    try { // qdd.ad_role_id -> From Role Id , qddl.ad_user_id -> To User Id
          // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      st1 = con.prepareStatement(
          " select qdd.ad_role_id as fromuserrole, qddl.ad_role_id as touserrole, rlne.rolesequenceno from eut_docapp_delegate qdd "
              + " join eut_docapp_delegateln qddl on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id "
              + " join eut_documentrule_lines rlne on rlne.ad_role_id=qdd.ad_role_id "
              + " join eut_documentrule_header rhdr on rhdr.eut_documentrule_header_id=rlne.eut_documentrule_header_id "
              + " and rhdr.document_type = ? and rhdr.ad_org_id= ?  "
              + " where qddl.ad_user_id = ? and qddl.document_type = ? "
              + " and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) "
              // + " and qdd.ad_role_id in (select ad_role_id from eut_next_role qnr left join
              // eut_next_role_line qnrl on qnr.eut_next_role_id = qnrl.eut_next_role_id where
              // qnr.eut_next_role_id = ?) "
              + " order by rolesequenceno desc limit 1 ");
      st1.setString(1, documentType);
      st1.setString(2, orgId);
      st1.setString(3, userId);
      st1.setString(4, documentType);
      log4j.debug("St:" + st1.toString());
      // st.setString(5, quNextRoleId);
      rs1 = st1.executeQuery();
      if (rs1.next()) {
        roleId.put("FromUserRoleId", rs1.getString("fromuserrole"));
        roleId.put("ToUserRoleId", rs1.getString("touserrole"));
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
        if (st1 != null)
          st1.close();
        if (rs1 != null)
          rs1.close();
      } catch (SQLException e) {
      }
    }
    return roleId;
  }

  /**
   * This method is used to get backward delegated from and to roles
   * 
   * @param con
   * @param clientId
   * @param paramOrgId
   * @param userId
   * @param documentType
   * @param quNextRoleId
   * @return
   */
  public HashMap<String, String> getbackwardDelegatedFromAndToRoles(Connection con, String clientId,
      String paramOrgId, String userId, String documentType, String quNextRoleId) {
    PreparedStatement st = null, st1 = null;
    ResultSet rs = null, rs1 = null;
    String orgId = paramOrgId;
    HashMap<String, String> roleId = new HashMap<String, String>();

    try { // qdd.ad_role_id -> From Role Id , qddl.ad_user_id -> To User Id
          // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      st1 = con.prepareStatement(
          " select qdd.ad_role_id as fromuserrole, qddl.ad_role_id as touserrole, rlne.rolesequenceno from eut_docapp_delegate qdd "
              + " join eut_docapp_delegateln qddl on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id "
              + " join eut_documentrule_lines rlne on rlne.ad_role_id=qdd.ad_role_id "
              + " join eut_documentrule_header rhdr on rhdr.eut_documentrule_header_id=rlne.eut_documentrule_header_id "
              + " and rhdr.document_type = ? and rhdr.ad_org_id= ?  "
              + " where qdd.ad_user_id = ? and qddl.document_type = ? "
              + " and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) "
              // + " and qdd.ad_role_id in (select ad_role_id from eut_next_role qnr left join
              // eut_next_role_line qnrl on qnr.eut_next_role_id = qnrl.eut_next_role_id where
              // qnr.eut_next_role_id = ?) "
              + " order by rolesequenceno desc limit 1 ");
      st1.setString(1, documentType);
      st1.setString(2, orgId);
      st1.setString(3, userId);
      st1.setString(4, documentType);
      // st.setString(5, quNextRoleId);
      log4j.debug("delegate from and to role :" + st1.toString());
      rs1 = st1.executeQuery();
      if (rs1.next()) {
        roleId.put("FromUserRoleId", rs1.getString("fromuserrole"));
        roleId.put("ToUserRoleId", rs1.getString("touserrole"));
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
        if (st1 != null)
          st1.close();
        if (rs1 != null)
          rs1.close();
      } catch (SQLException e) {
      }
    }
    return roleId;
  }

  /**
   * This method is used to get delegated from and to roles
   * 
   * @param con
   * @param clientId
   * @param paramOrgId
   * @param userId
   * @param documentType
   * @param quNextRoleId
   * @param roleid
   * @param delegationRole
   * @param delegationuser
   * @param pValue
   * @param costCenterId
   * @return
   */
  public HashMap<String, String> getDelegatedFromAndToRolesInvoice(Connection con, String clientId,
      String paramOrgId, String userId, String documentType, String quNextRoleId, String roleid,
      String delegationRole, String delegationuser, Object pValue, String costCenterId) {
    PreparedStatement st = null, st1 = null;
    ResultSet rs = null, rs1 = null;
    boolean isdepthead = false;
    String deptHeadRole = "", orgId = paramOrgId;
    HashMap<String, String> roleId = new HashMap<String, String>();

    try { // qdd.ad_role_id -> From Role Id , qddl.ad_user_id -> To User Id
          // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");
      if (!StringUtils.isEmpty(delegationRole))
        isdepthead = UtilityDAO.chkUserIsDeptHead(clientId, orgId, delegationuser, costCenterId);

      if (isdepthead) {
        deptHeadRole = UtilityDAO.getDeptHeadRole(clientId, orgId, documentType, pValue);
        roleId.put("FromUserRoleId", deptHeadRole);
        roleId.put("ToUserRoleId", roleid);
      } else {
        st1 = con.prepareStatement(
            " select qdd.ad_role_id as fromuserrole, qddl.ad_role_id as touserrole, rlne.rolesequenceno from eut_docapp_delegate qdd "
                + " join eut_docapp_delegateln qddl on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id "
                + " join eut_documentrule_lines rlne on rlne.ad_role_id=qdd.ad_role_id "
                + " join eut_documentrule_header rhdr on rhdr.eut_documentrule_header_id=rlne.eut_documentrule_header_id "
                + " and rhdr.document_type = ? and rhdr.ad_org_id= ?  "
                + " where qddl.ad_user_id = ? and qddl.document_type = ? "
                + " and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) "
                // + " and qdd.ad_role_id in (select ad_role_id from eut_next_role qnr left join
                // eut_next_role_line qnrl on qnr.eut_next_role_id = qnrl.eut_next_role_id where
                // qnr.eut_next_role_id = ?) "
                + " order by rolesequenceno desc limit 1 ");
        st1.setString(1, documentType);
        st1.setString(2, orgId);
        st1.setString(3, userId);
        st1.setString(4, documentType);
        // st.setString(5, quNextRoleId);
        rs1 = st1.executeQuery();
        if (rs1.next()) {
          roleId.put("FromUserRoleId", rs1.getString("fromuserrole"));
          roleId.put("ToUserRoleId", rs1.getString("touserrole"));
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getIsDelegated :", e);
      return roleId;
    } finally {
      try {
        if (st != null)
          st.close();
        if (st1 != null)
          st1.close();
        if (rs != null)
          rs.close();
        if (rs1 != null)
          rs1.close();
      } catch (SQLException e) {
      }
    }
    return roleId;
  }

  /**
   * This method is used to get requester next role
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return
   */
  public NextRoleByRuleVO getRequesterNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, Object pValue) {
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    try {
      ruleVO = new NextRoleByRuleVO();
      List<NextRoleByRuleVO> list = getNextRequesterRoleList(con, clientId, orgId, roleId, userId,
          documentType, pValue);
      if (list == null || list.size() == 0) {
        ruleVO.setStatus(Constants.sAPPROVED);
        ruleVO.setShortStatus(Constants.vAPPROVED);
        ruleVO.setFullStatus(Constants.sAPPROVED);
        ruleVO.setNextRoleId(null);
        ruleVO.setApproval(false);
      } else {
        String status = "";

        // Create QU Next Role
        String headerId = SequenceIdData.getUUID();
        st = con.prepareStatement(
            "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
        st.setString(1, headerId);
        st.setString(2, clientId);
        st.setString(3, orgId);
        st.setString(4, userId);
        st.setString(5, userId);
        st.setString(6, documentType);
        st.executeUpdate();

        st = con.prepareStatement(
            "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, rolesequenceno) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?);");
        for (NextRoleByRuleVO vo : list) {
          st.setString(1, clientId);
          st.setString(2, orgId);
          st.setString(3, userId);
          st.setString(4, userId);
          st.setString(5, headerId);
          st.setString(6, vo.getNextRoleId());
          st.setInt(7, vo.getRoleSeqNo());
          st.addBatch();
          // status += (" / " + vo.getNextRoleName());
          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + vo.getNextRoleId() + "'");
          if (ur != null && ur.list().size() > 0) {
            if (ur.list().size() == 1) {
              status += (" / " + ur.list().get(0).getRole().getName() + " / "
                  + ur.list().get(0).getUserContact().getName());
            } else {
              status += (" / " + ur.list().get(0).getRole().getName());
            }

          }
          // Role does not have user associated throw error to user
          else {
            Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
            ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
            ruleVO.setRoleName(objRole.getName());
            return ruleVO;
          }
        }
        st.executeBatch();

        ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
        ruleVO.setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setNextRoleId(headerId);
        ruleVO.setApproval(true);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * This method is used to get next requester role list
   * 
   * @param con
   * @param clientId
   * @param paramOrgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return
   */
  public List<NextRoleByRuleVO> getNextRequesterRoleList(Connection con, String clientId,
      String paramOrgId, String roleId, String userId, String documentType, Object pValue) {
    ResultSet rs = null;
    PreparedStatement st = null;
    NextRoleByRuleVO vo = null;
    List<NextRoleByRuleVO> list = new ArrayList<NextRoleByRuleVO>();
    String value = "", orgId = paramOrgId;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      value = pValue.toString();

      st = con.prepareStatement(
          "select qdrl.ad_role_id, qr.name as name, qdrl.rolesequenceno as roleseqno, qdrh.rulesequenceno as ruleseqno, qdrl.roleorderno from eut_documentrule_lines qdrl "
              + "join (select qdrh.eut_documentrule_header_id, qdrh.rulesequenceno, qdrl.rolesequenceno, qdrl.ad_role_id from eut_documentrule_header qdrh join eut_documentrule_lines qdrl on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + "where qdrh.ad_client_id = ? and qdrh.ad_org_id= ? and qdrh.document_type = ? and qdrh.requester_role = ? "
              + " order by qdrh.rulevalue desc limit 1) qdrh on qdrh.eut_documentrule_header_id = qdrl.eut_documentrule_header_id "
              + "join (select ln.eut_documentrule_header_id,ln.rolesequenceno,ln.ad_role_id from eut_documentrule_lines ln where ad_role_id=?) ln "
              + " on qdrh.eut_documentrule_header_id = ln.eut_documentrule_header_id "
              + "left join ad_role qr on qdrl.ad_role_id = qr.ad_role_id where qdrl.ad_client_id = ? and qdrl.ad_org_id= ? and qdrl.rolesequenceno = (ln.rolesequenceno)+1 order by qdrl.roleorderno;");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      st.setString(4, value);
      st.setString(5, roleId);
      st.setString(6, clientId);
      st.setString(7, orgId);
      log4j.debug("getNextRoleList query:" + st.toString());
      rs = st.executeQuery();
      while (rs.next()) {
        vo = new NextRoleByRuleVO();
        vo.setNextRoleId(rs.getString("ad_role_id"));
        vo.setNextRoleName(rs.getString("name"));
        vo.setRuleSeqNo(rs.getInt("ruleseqno"));
        vo.setRoleSeqNo(rs.getInt("roleorderno"));

        list.add(vo);
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRoleByRule() Method", e);
      return null;
    }
    return list;
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return nextRole header Object
   */
  public NextRoleByRuleVO getLineManagerBasedNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, Object pValue, String requesterId,
      Boolean isMultiRule, String strstatus) {
    String docStatus = strstatus;
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    Boolean managerFlag = Boolean.FALSE, hasInvoiceManager = Boolean.TRUE;
    Boolean flagLm = Boolean.FALSE;
    List<NextRoleByRuleVO> list = null;
    Map<String, List<String>> hrLineManagerMap = null;
    String userName = "", strInvoiceManager = "";
    try {
      ruleVO = new NextRoleByRuleVO();
      User objManageruser = null;
      User currentUser = OBDal.getInstance().get(User.class, requesterId);
      if (currentUser != null) {
        userName = currentUser.getName();
      }
      if (isMultiRule) {
        list = getNextRequesterRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      } else {
        list = getNextRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      }
      // Check Role has user associated if yes proceed if not the throw error to user
      if (documentType.equals("EUT_117")
          && (strstatus.equals("INC") || strstatus.equals("REJ") || strstatus.equals("REA"))) {
        docStatus = "DR";
      } else if (documentType.equals("EUT_116") && (strstatus.equals("ESCM_REJ"))) {
        docStatus = "DR";
      } else if (documentType.equals("EUT_108") && (strstatus.equals("ESCM_REJ"))) {
        docStatus = "DR";
      } else if (documentType.equals("EUT_105") || documentType.equals("EUT_104")
          || documentType.equals("EUT_119") || documentType.equals("EUT_106")
          || documentType.equals("EUT_124") || documentType.equals("EUT_101")
          || documentType.equals("EUT_110") || documentType.equals("EUT_109")) {
        docStatus = "DR";
      }
      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + checkVO.getNextRoleId() + "'");
          log4j.debug("next role id :" + checkVO.getNextRoleId());
          if (ur == null || ur.list().size() == 0) {
            Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
            if ((objRole.isEscmIshrlinemanager() != null && !objRole.isEscmIshrlinemanager())
                || (!documentType.equals("EUT_113"))) {
              ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
              ruleVO.setRoleName(objRole.getName());
              // OBMessageUtils.messageBD("EUT_NOUser_ForRoles").replaceAll("@",
              // objRole.getName()));
              return ruleVO;
            }
          }
        }
      }
      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          // check role is line manager
          Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
          Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());

          if (objRole.isEscmIslinemanager() && !checkIsForwardedRole) {
            BusinessPartner objManager = null;
            flagLm = Boolean.TRUE;
            // check role associated requester line manager
            // get user details
            User objUser = OBDal.getInstance().get(User.class, requesterId);
            BusinessPartner objBp = objUser.getBusinessPartner();
            if (objBp != null) {
              String strManangerCode = objBp.getEhcmManager();
              // get Manager Details

              if (documentType.equals("EUT_101") || documentType.equals("EUT_110")
                  || documentType.equals("EUT_109")) {
                strInvoiceManager = getInvoiceManager(objUser);
                if (StringUtils.isEmpty(strInvoiceManager)) {
                  managerFlag = Boolean.FALSE;
                  hasInvoiceManager = Boolean.FALSE;
                } else
                  managerFlag = Boolean.TRUE;

              }
              // encumbrance
              else if (documentType.equals("EUT_105")) {
                strInvoiceManager = getUserManager(objUser, Constants.ENCUMBRANCE_DOCTYPE);
                if (StringUtils.isEmpty(strInvoiceManager)) {
                  managerFlag = Boolean.FALSE;
                  hasInvoiceManager = Boolean.FALSE;
                } else
                  managerFlag = Boolean.TRUE;

              } else {
                OBQuery<BusinessPartner> objManagerQuery = null;
                List<BusinessPartner> manager = new ArrayList<BusinessPartner>();
                objManagerQuery = OBDal.getInstance().createQuery(BusinessPartner.class,
                    "as e where e.efinDocumentno='" + strManangerCode + "'");
                if (objManagerQuery != null) {
                  manager = objManagerQuery.list();
                  if (manager.size() > 0) {
                    objManager = manager.get(0);
                    if (objManager != null) {
                      if (objManager.getADUserList() != null
                          && objManager.getADUserList().size() > 0) {
                        objManageruser = objManager.getADUserList().get(0);
                      }
                    }
                  }
                }
              }
              if (StringUtils.isNotEmpty(strInvoiceManager)) {
                objManageruser = Utility.getObject(User.class, strInvoiceManager);
              }
              if (objManageruser != null) {
                // check role associated with Manager
                OBQuery<UserRoles> userRoleList = OBDal.getInstance().createQuery(UserRoles.class,
                    "as e where e.role.id='" + objRole.getId() + "' and e.userContact.id='"
                        + objManageruser.getId() + "'");
                if (userRoleList.list().size() > 0) {
                  managerFlag = Boolean.TRUE;
                  break;
                } else {
                  managerFlag = Boolean.FALSE;
                }
              }
            }
          }
        }
      }

      if (documentType.equals("EUT_113")) {
        // Check Line manager user is associated for requester if nextrole is dummy role
        for (NextRoleByRuleVO vo : list) {
          Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
          if (objRole.isEscmIshrlinemanager() != null && objRole.isEscmIshrlinemanager()) {
            hrLineManagerMap = hrLineManagerUserAndRole(vo.getNextRoleId(), requesterId);
            log4j.debug("hrLineManagerMapintial" + hrLineManagerMap);

            if (hrLineManagerMap == null || hrLineManagerMap.isEmpty()
                || hrLineManagerMap.size() == 0) {
              log4j.debug("hrLineManagerMap" + hrLineManagerMap.size());
              ruleVO.setErrorMsg("EUT_NOlineMngr_requester");
              ruleVO.setUserName(userName);
              return ruleVO;
            }
          }

        }
      }
      if (flagLm && !managerFlag && docStatus.equals("DR")) {
        if (hasInvoiceManager)
          ruleVO.setErrorMsg("NoManagerAssociatedWithRole");
        else
          ruleVO.setErrorMsg("Managernotdefined");
      } else {
        if (list == null || list.size() == 0) {
          ruleVO.setStatus(Constants.sAPPROVED);
          ruleVO.setShortStatus(Constants.vAPPROVED);
          ruleVO.setFullStatus(Constants.sAPPROVED);
          ruleVO.setNextRoleId(null);
          ruleVO.setApproval(false);
        } else {
          String status = "";

          // Create QU Next Role
          String headerId = SequenceIdData.getUUID();
          st = con.prepareStatement(
              "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) "
                  + "VALUES (?, ?, ?, ?, ?, ?);");
          st.setString(1, headerId);
          st.setString(2, clientId);
          st.setString(3, orgId);
          st.setString(4, userId);
          st.setString(5, userId);
          st.setString(6, documentType);
          st.executeUpdate();

          st = con.prepareStatement(
              "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby,"
                  + " eut_next_role_id, ad_role_id, rolesequenceno,ad_user_id) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?,?);");
          for (NextRoleByRuleVO vo : list) {
            // check role is line manager
            Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
            Boolean isHrLineMgr = Boolean.FALSE;
            String hrLineMgrUserId = "";
            String managerUser = "";
            Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());
            if (objRole.isEscmIslinemanager() && docStatus.equals("DR") && !checkIsForwardedRole) {
              if (documentType.equals("EUT_101") || documentType.equals("EUT_110")
                  || documentType.equals("EUT_109") || documentType.equals("EUT_105")) {
                managerUser = strInvoiceManager;
              } else {
                managerUser = LineManagerUser(vo.getNextRoleId(), requesterId);
              }
              if (StringUtils.isNotEmpty(managerUser)) {
                st.setString(1, clientId);
                st.setString(2, orgId);
                st.setString(3, userId);
                st.setString(4, userId);
                st.setString(5, headerId);
                st.setString(6, vo.getNextRoleId());
                st.setInt(7, vo.getRoleSeqNo());
                st.setString(8, managerUser);
                st.addBatch();

              }
            } else if (objRole.isEscmIshrlinemanager() != null && objRole.isEscmIshrlinemanager()
                && documentType.equals("EUT_113")) {
              log4j.debug("insertion of eutline");

              st = con.prepareStatement(
                  "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, "
                      + "createdby, updatedby, eut_next_role_id, ad_role_id, ad_user_id, dummy_role) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?, ?);");

              if (hrLineManagerMap != null) {
                isHrLineMgr = Boolean.TRUE;
                for (Entry<String, List<String>> user : hrLineManagerMap.entrySet()) {
                  HashMap<String, String> role = NextRoleByRule.getDelegatedUserAndRole(
                      OBDal.getInstance().getConnection(), clientId, orgId, user.getKey(),
                      Resource.Return_Transaction);
                  // check delegated role and insert into eut role line
                  if (!role.isEmpty()) {
                    st.setString(1, clientId);
                    st.setString(2, orgId);
                    st.setString(3, userId);
                    st.setString(4, userId);
                    st.setString(5, headerId);
                    st.setString(6, role.get("ToUserRoleId"));
                    st.setString(7, role.get("FromUser"));
                    st.setString(8, objRole.getId());
                    st.addBatch();
                  }
                  hrLineMgrUserId = user.getKey();
                  List<String> roles = user.getValue();
                  for (String ro : roles) {
                    st.setString(1, clientId);
                    st.setString(2, orgId);
                    st.setString(3, userId);
                    st.setString(4, userId);
                    st.setString(5, headerId);
                    st.setString(6, ro);
                    st.setString(7, user.getKey());
                    st.setString(8, objRole.getId());

                    st.addBatch();
                  }
                }
              }
            } else {
              st.setString(1, clientId);
              st.setString(2, orgId);
              st.setString(3, userId);
              st.setString(4, userId);
              st.setString(5, headerId);
              st.setString(6, vo.getNextRoleId());
              st.setInt(7, vo.getRoleSeqNo());
              st.setString(8, null);
              st.addBatch();
            }

            // status += (" / " + vo.getNextRoleName());

            if (!isHrLineMgr) {
              OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                  "role.id='" + vo.getNextRoleId() + "'");
              if (ur != null && ur.list().size() > 0) {
                if (ur.list().size() == 1) {
                  status += (" / " + ur.list().get(0).getRole().getName() + " / "
                      + ur.list().get(0).getUserContact().getName());
                } else {
                  if (StringUtils.isNotEmpty(managerUser)) {
                    User obManagerUser = OBDal.getInstance().get(User.class, managerUser);
                    status += (" / " + ur.list().get(0).getRole().getName() + " / "
                        + obManagerUser.getName());
                  } else
                    status += (" / " + ur.list().get(0).getRole().getName());
                }

              }
            } else {
              User hrUser = OBDal.getInstance().get(User.class, hrLineMgrUserId);
              if (hrUser != null) {
                status += (" / " + hrUser.getName());
              }
            }

          }
          st.executeBatch();

          ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
          ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
          ruleVO
              .setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
          ruleVO.setNextRoleId(headerId);
          ruleVO.setApproval(true);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return nextRole header Object
   */
  public NextRoleByRuleVO getUserManagerBasedNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, Object pValue, String requesterId,
      Boolean isMultiRule, String strstatus) {
    String docStatus = strstatus;
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    Boolean managerFlag = Boolean.FALSE, hasInvoiceManager = Boolean.TRUE,
        hasLineManagerRole = Boolean.FALSE, hasNoRole = Boolean.FALSE;
    Boolean flagLm = Boolean.FALSE;
    Role nextApprovalRole = null;
    List<NextRoleByRuleVO> list = null;
    String userName = "", strInvoiceManager = "";
    try {
      ruleVO = new NextRoleByRuleVO();
      User objManageruser = null;
      User currentUser = OBDal.getInstance().get(User.class, requesterId);
      if (currentUser != null) {
        userName = currentUser.getName();
      }
      if (isMultiRule) {
        list = getNextRequesterRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      } else {
        list = getNextRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      }

      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          // check role is line manager
          Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
          Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());

          if (objRole.isEscmIslinemanager() && !checkIsForwardedRole) {
            flagLm = Boolean.TRUE;

            // check role associated requester line manager
            // get user details
            User objUser = OBDal.getInstance().get(User.class, requesterId);
            BusinessPartner objBp = objUser.getBusinessPartner();
            if (objBp != null) {
              // get Manager Details
              if (documentType.equals("EUT_111") || documentType.equals("EUT_118")) {
                strInvoiceManager = getUserManager(objUser, Constants.REQUISITION_DOCTYPE);
                if (StringUtils.isEmpty(strInvoiceManager)) {
                  managerFlag = Boolean.FALSE;
                  hasInvoiceManager = Boolean.FALSE;
                } else
                  managerFlag = Boolean.TRUE;

              }
              if (StringUtils.isNotEmpty(strInvoiceManager)) {
                objManageruser = Utility.getObject(User.class, strInvoiceManager);
              }
              if (objManageruser != null) {
                // check role associated with Manager
                OBQuery<UserRoles> userRoleList = OBDal.getInstance().createQuery(UserRoles.class,
                    "as e where e.role.id='" + objRole.getId() + "' and e.userContact.id='"
                        + objManageruser.getId() + "'");
                if (userRoleList.list().size() > 0) {
                  hasLineManagerRole = Boolean.TRUE;
                  managerFlag = Boolean.TRUE;
                  break;
                } else {
                  hasLineManagerRole = Boolean.FALSE;
                  if (objManageruser.getDefaultRole() != null) {
                    nextApprovalRole = objManageruser.getDefaultRole();
                  } else {
                    OBQuery<UserRoles> userDefRoleListQry = OBDal.getInstance()
                        .createQuery(UserRoles.class, "as e where  e.userContact.id='"
                            + objManageruser.getId() + "' order by e.created desc");
                    if (userDefRoleListQry.list().size() > 0) {
                      nextApprovalRole = userDefRoleListQry.list().get(0).getRole();
                    } else {
                      hasNoRole = Boolean.TRUE;
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (flagLm && !managerFlag && !hasInvoiceManager && docStatus.equals("DR")) {
        ruleVO.setErrorMsg("ESCM_ReqDefMangDetail");
      } else if (flagLm && hasNoRole && docStatus.equals("DR")) {
        ruleVO.setErrorMsg("EUT_NoRoleDefineForUser");
      } else {
        if (list == null || list.size() == 0) {
          ruleVO.setStatus(Constants.sAPPROVED);
          ruleVO.setShortStatus(Constants.vAPPROVED);
          ruleVO.setFullStatus(Constants.sAPPROVED);
          ruleVO.setNextRoleId(null);
          ruleVO.setApproval(false);
        } else {
          String status = "";

          // Create QU Next Role
          String headerId = SequenceIdData.getUUID();
          st = con.prepareStatement(
              "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) "
                  + "VALUES (?, ?, ?, ?, ?, ?);");
          st.setString(1, headerId);
          st.setString(2, clientId);
          st.setString(3, orgId);
          st.setString(4, userId);
          st.setString(5, userId);
          st.setString(6, documentType);
          st.executeUpdate();

          st = con.prepareStatement(
              "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby,"
                  + " eut_next_role_id, ad_role_id, rolesequenceno,ad_user_id,dummy_role) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?,?,?);");
          for (NextRoleByRuleVO vo : list) {
            // check role is line manager
            Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
            String managerUser = "";
            Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());
            if (objRole.isEscmIslinemanager() && docStatus.equals("DR") && !checkIsForwardedRole) {
              managerUser = strInvoiceManager;
              if (StringUtils.isNotEmpty(managerUser)) {
                st.setString(1, clientId);
                st.setString(2, orgId);
                st.setString(3, userId);
                st.setString(4, userId);
                st.setString(5, headerId);
                if (hasLineManagerRole)
                  st.setString(6, vo.getNextRoleId());
                else
                  st.setString(6, nextApprovalRole.getId());
                st.setInt(7, vo.getRoleSeqNo());
                st.setString(8, managerUser);
                st.setString(9, vo.getNextRoleId());
                st.addBatch();

              }
            } else {
              st.setString(1, clientId);
              st.setString(2, orgId);
              st.setString(3, userId);
              st.setString(4, userId);
              st.setString(5, headerId);
              st.setString(6, vo.getNextRoleId());
              st.setInt(7, vo.getRoleSeqNo());
              st.setString(8, null);
              st.setString(9, null);
              st.addBatch();
            }
            if (StringUtils.isNotEmpty(managerUser)) {
              User obManagerUser = OBDal.getInstance().get(User.class, managerUser);

              if (hasLineManagerRole) {
                status += (" / " + objRole.getName() + " / " + obManagerUser.getName());
              } else {
                status += (" / " + nextApprovalRole.getName() + " / " + obManagerUser.getName());
              }
            } else {
              status += (" / " + objRole.getName());
            }
          }
          st.executeBatch();

          ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
          ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
          ruleVO
              .setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
          ruleVO.setNextRoleId(headerId);
          ruleVO.setApproval(true);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * This method is used to get invoice manager
   * 
   * @param objUser
   * @return
   */
  public String getInvoiceManager(User objUser) {
    String strInvoiceManager = "";
    try {
      OBQuery<Efin_UserManager> managerQuery = OBDal.getInstance().createQuery(
          Efin_UserManager.class, " where userContact.id = :requesterId  and documentType='API' ");
      managerQuery.setNamedParameter("requesterId", objUser.getId());
      List<Efin_UserManager> managers = new ArrayList<Efin_UserManager>();
      if (managerQuery != null) {
        managers = managerQuery.list();

        if (managers.size() > 0) {
          strInvoiceManager = managers.get(0).getLineManager().getId();
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
    }
    return strInvoiceManager;
  }

  /**
   * 
   * @param objUser
   * @param docType
   * @return get user manager from User manager screen
   */
  public String getUserManager(User objUser, String docType) {
    String strUserManager = "";
    try {
      OBQuery<Efin_UserManager> managerQuery = OBDal.getInstance().createQuery(
          Efin_UserManager.class,
          " where userContact.id = :requesterId  and documentType=:docType ");

      managerQuery.setNamedParameter("requesterId", objUser.getId());
      managerQuery.setNamedParameter("docType", docType);
      List<Efin_UserManager> managers = new ArrayList<Efin_UserManager>();
      if (managerQuery != null) {
        managers = managerQuery.list();

        if (managers.size() > 0) {
          strUserManager = managers.get(0).getLineManager().getId();
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getUserManager Method", e);
    }
    return strUserManager;
  }

  /**
   * 
   * @param roleId
   * @param userId
   * @return managerId
   */
  public String LineManagerUser(String roleId, String userId) {
    String strManager = "";

    try {
      // check role is line manager
      Role objRole = OBDal.getInstance().get(Role.class, roleId);
      Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());
      if (objRole.isEscmIslinemanager() && !checkIsForwardedRole) {
        BusinessPartner objManager = null;
        // check role associated requester line manager
        // get user details
        User objUser = OBDal.getInstance().get(User.class, userId);
        BusinessPartner objBp = objUser.getBusinessPartner();
        if (objBp != null) {
          String strManangerCode = objBp.getEhcmManager();
          // get Manager Details
          OBQuery<BusinessPartner> objManagerQuery = OBDal.getInstance().createQuery(
              BusinessPartner.class, "as e where e.efinDocumentno='" + strManangerCode + "'");
          if (objManagerQuery.list().size() > 0) {
            objManager = objManagerQuery.list().get(0);
            if (objManager != null) {
              User objManageruser = objManager.getADUserList().get(0);
              if (objManageruser != null) {
                // check role associated with Manager
                OBQuery<UserRoles> userRoleList = OBDal.getInstance().createQuery(UserRoles.class,
                    "as e where e.role.id='" + roleId + "' and e.userContact.id='"
                        + objManageruser.getId() + "'");
                if (userRoleList.list().size() > 0) {
                  strManager = objManageruser.getId();
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in LineManagerUser Method", e);
    }
    return strManager;
  }

  /**
   * 
   * @param roleId
   * @param userId
   * @return map with user and role list
   */
  public Map<String, List<String>> hrLineManagerUserAndRole(String roleId, String userId) {

    HashMap<String, List<String>> hrLineManagerMap = new HashMap<String, List<String>>();

    try {
      List<String> roleList = new ArrayList<String>();
      BusinessPartner objLineManager = null;

      User objUser = OBDal.getInstance().get(User.class, userId);
      BusinessPartner objBp = objUser.getBusinessPartner();
      if (objBp != null) {
        String strManangerCode = objBp.getEhcmManager();
        log4j.debug("strManangerCode" + strManangerCode);

        // get Manager Details
        OBQuery<BusinessPartner> objManagerQuery = OBDal.getInstance().createQuery(
            BusinessPartner.class, "as e where e.efinDocumentno ='" + strManangerCode + "'");
        log4j.debug("objManagerQuery" + objManagerQuery);

        if (objManagerQuery.list().size() > 0) {
          objLineManager = objManagerQuery.list().get(0);
          if (objLineManager != null) {
            User objManageruser = objLineManager.getADUserList() == null ? null
                : objLineManager.getADUserList().get(0);
            if (objManageruser != null) {
              // check role associated with Manager
              OBQuery<UserRoles> userRoleList = OBDal.getInstance().createQuery(UserRoles.class,
                  "as e where e.userContact.id='" + objManageruser.getId() + "'");
              if (userRoleList.list().size() > 0) {
                roleList = userRoleList.list().stream()
                    .filter(a -> a.getRole().isEscmIslinemanager()).map(a -> a.getRole().getId())
                    .collect(Collectors.toList());
                if (roleList == null || roleList.isEmpty()) {
                  roleList = userRoleList.list().stream().map(a -> a.getRole().getId())
                      .collect(Collectors.toList());
                }
                hrLineManagerMap.put(objManageruser.getId(), roleList);
              }
            }
          }

        }

      }

    } catch (Exception e) {
      if (log4j.isDebugEnabled()) {
        log4j.error("Exception in LineManagerUser Method", e);
      }
    }
    return hrLineManagerMap;
  }

  /**
   * This will return delegated role and user details for given user
   * 
   * @param con
   * @param clientId
   * @param paramOrgId
   * @param userId
   * @param documentType
   * @return delegated role and user
   */
  @SuppressWarnings("resource")
  public HashMap<String, String> getDelegatedUserAndRole(Connection con, String clientId,
      String paramOrgId, String userId, String documentType) {
    PreparedStatement st = null;
    ResultSet rs = null;
    HashMap<String, String> roleId = new HashMap<String, String>();
    String orgId = paramOrgId;
    try {
      // Getting Organization
      st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
      st.setString(1, clientId);
      st.setString(2, orgId);
      st.setString(3, documentType);
      rs = st.executeQuery();
      if (rs.next())
        orgId = rs.getString("eut_documentrule_parentorg");

      st = con.prepareStatement(
          " select qdd.ad_role_id as fromuserrole, qddl.ad_role_id as touserrole, rlne.rolesequenceno,qddl.ad_user_id as touser from eut_docapp_delegate qdd "
              + " left join eut_docapp_delegateln qddl on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id "
              + " left join eut_documentrule_lines rlne on rlne.ad_role_id=qdd.ad_role_id "
              + " left join eut_documentrule_header rhdr on rhdr.eut_documentrule_header_id=rlne.eut_documentrule_header_id "
              + " and rhdr.document_type = ? and rhdr.ad_org_id= ?  "
              + " where qdd.ad_user_id = ? and qddl.document_type = ? "
              + " and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) "
              + " order by rolesequenceno desc limit 1 ");
      st.setString(1, documentType);
      st.setString(2, orgId);
      st.setString(3, userId);
      st.setString(4, documentType);
      log4j.debug("deluserroleqry>" + st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        roleId.put("FromUser", rs.getString("touser"));
        roleId.put("ToUserRoleId", rs.getString("touserrole"));
      }
    } catch (final Exception e) {
      log4j.error("Exception in getDelegatedUserAndRole :", e);
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
   * This method is used to get budget distribution next role
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return
   */
  public NextRoleByRuleVO getBudgetDistNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, Object pValue) {
    NextRoleByRuleVO ruleVO = null;
    String orgbcumanager = null;
    Boolean orgManager = false, orgBudgManager = false, BCUBudgManager = false;
    ruleVO = new NextRoleByRuleVO();
    JSONObject json = null, json1 = null;
    PreparedStatement st = null;
    String status = "";
    String headerId = null;
    int seq = 0;
    List<NextRoleByRuleVO> list = null;
    try {
      Organization org = OBDal.getInstance().get(Organization.class, orgId);

      list = getNextRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);

      /*
       * // chk if list size is zero then chk that particular user will be org manager / org budget
       * // manager if (list.size() == 0) { if (userId.equals(org.getEfinOrgmanager().getId())) {
       * orgUserManager = true; } else if (userId.equals(org.getEfinOrgbudgmanager().getId()))
       * orgUserBudgManager = true;
       * 
       * // get org manager / org budget manager role if (orgUserManager || orgUserBudgManager) {
       * OBQuery<Role> role = OBDal.getInstance().createQuery(Role.class,
       * " as e where e.efinOrgbcumanger='" + (orgUserManager ? "ORGM" : "ORGBM") + "'");
       * role.setMaxResult(1); if (role.list().size() > 0) { orgBCURoleId =
       * role.list().get(0).getId(); } }
       * 
       * // pass org manager / org budget manager role to get the next role list list =
       * getNextRoleList(con, clientId, orgId, orgBCURoleId, userId, documentType, pValue); }
       */

      // if list size is zero then it should be final approver
      if (list == null || list.size() == 0) {
        ruleVO.setStatus(Constants.sAPPROVED);
        ruleVO.setShortStatus(Constants.vAPPROVED);
        ruleVO.setFullStatus(Constants.sAPPROVED);
        ruleVO.setNextRoleId(null);
        ruleVO.setApproval(false);
      } else {

        // if we get the next approver list pass the next approver role and chk role contain org
        // manager or org bcu maanger
        for (NextRoleByRuleVO vo1 : list) {

          // chk next role contain org manager / org budget manager flag then get user and role list
          // JSONObject
          log4j.debug("next role from doc rule>" + vo1.getNextRoleId());
          Role role = OBDal.getInstance().get(Role.class, vo1.getNextRoleId());

          if (role.getEfinOrgbcumanger() != null) {
            orgbcumanager = role.getEfinOrgbcumanger();
            if (orgbcumanager.equals("ORGM"))
              orgManager = true;
            else if (orgbcumanager.equals("ORGBM"))
              orgBudgManager = true;

            // get user and role List object to insert the next role line
            json = getOrgBCUBudgManager(orgManager, orgBudgManager, BCUBudgManager, clientId,
                orgId);
            if (json != null) {
              // if result get one then will insert next user and role based on org manager or org
              // budget manager
              if (json.getString("result").equals("1")) {
                JSONArray jsonArray = json.getJSONArray("roleList");

                headerId = SequenceIdData.getUUID();
                st = con.prepareStatement(
                    "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
                st.setString(1, headerId);
                st.setString(2, clientId);
                st.setString(3, orgId);
                st.setString(4, userId);
                st.setString(5, userId);
                st.setString(6, documentType);
                st.executeUpdate();

                st = con.prepareStatement(
                    "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id,ad_user_id, rolesequenceno,dummy_role) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?,?, ?,?);");
                for (int i = 0; i < jsonArray.length(); i++) {
                  json1 = jsonArray.getJSONObject(i);

                  st.setString(1, clientId);
                  st.setString(2, orgId);
                  st.setString(3, userId);
                  st.setString(4, userId);
                  st.setString(5, headerId);
                  st.setString(6, json1.getString("roleId"));
                  st.setString(7, json.getString("userId"));
                  st.setInt(8, seq++);
                  st.setString(9, vo1.getNextRoleId());
                  st.addBatch();
                }
                /*
                 * HashMap<String, String> delrole = NextRoleByRule.getDelegatedUserAndRole(
                 * OBDal.getInstance().getConnection(), clientId, orgId, json.getString("userId"),
                 * documentType); // check delegated role and insert into eut role line if
                 * (!delrole.isEmpty()) { st.setString(1, clientId); st.setString(2, orgId);
                 * st.setString(3, userId); st.setString(4, userId); st.setString(5, headerId);
                 * st.setString(6, delrole.get("ToUserRoleId")); st.setString(7,
                 * delrole.get("FromUser")); st.setInt(8, seq++); st.setString(9,
                 * vo1.getNextRoleId()); st.addBatch(); }
                 */
                st.executeBatch();

                status += (" / " + json.getString("userName"));

                ruleVO.setStatus(
                    String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
                ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
                ruleVO.setFullStatus(
                    String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
                ruleVO.setNextRoleId(headerId);
                ruleVO.setApproval(true);
              }
              // if result get -2 then didnt assign the org manager / org budget manager under
              // organisation
              else if (json.getString("result").equals("-2")) {
                if (orgManager || orgBudgManager)
                  ruleVO.setErrorMsg("EFIN_OrgBudMangisNotAssociated");
                ruleVO.setOrgName(org.getName());
                return ruleVO;
              }
              // if result get -1 then didnt assign the role for the user

              else if (json.getString("result").equals("-1")) {
                ruleVO.setErrorMsg("EUT_NoRoleDefineForUser");
                ruleVO.setUserName(json.getString("userName"));
                return ruleVO;
              }
            }
          } else {
            // Create EUT Next Role
            headerId = SequenceIdData.getUUID();
            st = con.prepareStatement(
                "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
            st.setString(1, headerId);
            st.setString(2, clientId);
            st.setString(3, orgId);
            st.setString(4, userId);
            st.setString(5, userId);
            st.setString(6, documentType);
            st.executeUpdate();

            if (headerId != null) {
              st = con.prepareStatement(
                  "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, rolesequenceno) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?);");
              st.setString(1, clientId);
              st.setString(2, orgId);
              st.setString(3, userId);
              st.setString(4, userId);
              st.setString(5, headerId);
              st.setString(6, vo1.getNextRoleId());
              st.setInt(7, vo1.getRoleSeqNo());
              st.addBatch();
              // status += (" / " + vo.getNextRoleName());
              OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                  "role.id='" + vo1.getNextRoleId() + "'");
              if (ur != null && ur.list().size() > 0) {
                if (ur.list().size() == 1) {
                  status += (" / " + ur.list().get(0).getRole().getName() + " / "
                      + ur.list().get(0).getUserContact().getName());
                } else {
                  status += (" / " + ur.list().get(0).getRole().getName());
                }

              }
              // Role does not have user associated throw error to user
              else {
                Role objRole = OBDal.getInstance().get(Role.class, vo1.getNextRoleId());
                ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
                ruleVO.setRoleName(objRole.getName());
                return ruleVO;
              }
            }

            st.executeBatch();

            ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
            ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
            ruleVO.setFullStatus(
                String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
            ruleVO.setNextRoleId(headerId);
            ruleVO.setApproval(true);
          }
        }
      }
    } catch (

    Exception e) {
      if (log4j.isDebugEnabled()) {
        log4j.error("Exception in getBudgetDistNextRole Method", e);
      }
    }
    return ruleVO;
  }

  /**
   * 
   * @param orgManager
   * @param orgBudgManager
   * @param BCUBudgManager
   * @param clientId
   * @param orgId
   * @return
   */
  public JSONObject getOrgBCUBudgManager(Boolean orgManager, Boolean orgBudgManager,
      Boolean BCUBudgManager, String clientId, String orgId) {
    User user = null;
    JSONObject json = null, jsonrole = null;
    new JSONArray();
    JSONArray rolearray = new JSONArray();
    try {
      OBContext.setAdminMode();
      if (orgManager) {
        Organization orgmanager = OBDal.getInstance().get(Organization.class, orgId);
        if (orgmanager.getEfinOrgmanager() != null)
          user = orgmanager.getEfinOrgmanager();

      }
      if (orgBudgManager) {
        Organization orgmanager = OBDal.getInstance().get(Organization.class, orgId);
        if (orgmanager.getEfinOrgbudgmanager() != null)
          user = orgmanager.getEfinOrgbudgmanager();
      }
      /*
       * if (BCUBudgManager) { OBQuery<EfinBudgetControlParam> controlParamObj =
       * OBDal.getInstance().createQuery( EfinBudgetControlParam.class, " as e where e.client.id='"
       * + clientId + "'"); controlParamObj.setMaxResult(1); if (controlParamObj.list().size() > 0)
       * { controlPrmObj = controlParamObj.list().get(0); if (controlPrmObj.getBCUBudgmanager() !=
       * null) user = controlPrmObj.getBCUBudgmanager(); } }
       */
      if (user != null) {
        OBQuery<UserRoles> userRoleList = OBDal.getInstance().createQuery(UserRoles.class,
            "as e where e.userContact.id='" + user.getId() + "'");
        json = new JSONObject();
        json.put("userId", user.getId());
        json.put("userName", user.getName());
        if (userRoleList.list().size() > 0) {
          for (UserRoles role : userRoleList.list()) {
            json.put("result", "1");
            jsonrole = new JSONObject();
            jsonrole.put("roleId", role.getRole().getId());
            jsonrole.put("roleName", role.getRole().getName());
            rolearray.put(jsonrole);
          }
          json.put("roleList", rolearray);
        } else {
          json.put("result", "-1");
        }
      } else {
        json = new JSONObject();
        json.put("result", "-2");
      }
      log4j.debug("json:" + json);
      return json;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      if (log4j.isDebugEnabled()) {
        log4j.error("Exception in getOrgBCUBudgManager Method", e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

  /**
   * This method is used to get dummy role gave delegation need to get dummy role for funds request
   * management
   * 
   * @param con
   * @param clientId
   * @param paramOrgId
   * @param userId
   * @param documentType
   * @param quNextRoleId
   * @param transOrgId
   * @return
   */
  @SuppressWarnings({ "resource", "null" })
  public HashMap<String, String> getDelegatedFromAndToRolesForDummyRoles(Connection con,
      String clientId, String paramOrgId, String userId, String documentType, String quNextRoleId,
      String transOrgId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    HashMap<String, String> roleId = new HashMap<String, String>();
    String orgId = paramOrgId, fromRoleId = null;
    Boolean orgManager = false, orgBudgManager = false;
    HashMap<String, String> role = new HashMap<String, String>();

    try { // qdd.ad_role_id -> From Role Id , qddl.ad_user_id -> To User Id

      roleId = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
          clientId, orgId, userId, documentType, quNextRoleId);

      if (roleId.get("FromUserRoleId") != null) {
        return roleId;
      } else {

        // Getting Organization
        st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);");
        st.setString(1, clientId);
        st.setString(2, orgId);
        st.setString(3, documentType);
        rs = st.executeQuery();
        if (rs.next())
          orgId = rs.getString("eut_documentrule_parentorg");

        st = con.prepareStatement(
            " select qdd.ad_user_id  as fromUserId, qdd.ad_role_id as fromuserrole, qddl.ad_role_id as touserrole, rlne.rolesequenceno from eut_docapp_delegate qdd "
                + " join eut_docapp_delegateln qddl on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id "
                + " left join eut_documentrule_lines rlne on rlne.ad_role_id=qdd.ad_role_id "
                + " left join eut_documentrule_header rhdr on rhdr.eut_documentrule_header_id=rlne.eut_documentrule_header_id "
                + " and rhdr.document_type = ? and rhdr.ad_org_id= ?  "
                + " where qddl.ad_user_id = ? and qddl.document_type = ? "
                + " and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) "
                + " order by rolesequenceno desc limit 1 ");
        st.setString(1, documentType);
        st.setString(2, orgId);
        st.setString(3, userId);
        st.setString(4, documentType);
        log4j.debug("St123:" + st.toString());
        // st.setString(5, quNextRoleId);
        rs = st.executeQuery();
        if (rs.next()) {
          role.put("FromUserRoleId", rs.getString("fromuserrole"));
          role.put("ToUserRoleId", rs.getString("touserrole"));
          role.put("FromUserId", rs.getString("fromUserId"));

        }

        if (role.get("FromUserId") != null) {
          st = con.prepareStatement(
              " select  em_efin_orgmanager from ad_org where  em_efin_orgmanager = ? and ad_org_id = ? ");
          st.setString(1, role.get("FromUserId"));
          st.setString(2, transOrgId);
          log4j.debug("St124:" + st.toString());
          rs = st.executeQuery();
          if (rs.next()) {
            orgManager = true;
          } else {
            st = con.prepareStatement(
                " select  em_efin_orgmanager from ad_org where     em_efin_orgbudgmanager = ?  and ad_org_id = ? ");
            st.setString(1, role.get("FromUserId"));
            st.setString(2, transOrgId);
            log4j.debug("St125:" + st.toString());
            st.setString(1, role.get("FromUserId"));
            rs = st.executeQuery();
            if (rs.next()) {
              orgBudgManager = true;
            }
          }
          fromRoleId = UtilityDAO.getOrgMangOrBudgManagerRole(clientId, orgManager, orgBudgManager);
          if (fromRoleId != null) {
            roleId.put("FromUserRoleId", fromRoleId);
            roleId.put("ToUserRoleId", role.get("ToUserRoleId"));
          }
        }
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
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return nextRole header Object
   */
  public NextRoleByRuleVO getSpecializedDeptBasedNextRole(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue,
      String requesterId, Boolean isMultiRule, Requisition specializedDept) {
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    Boolean managerFlag = Boolean.FALSE;
    Boolean flagLm = Boolean.FALSE;
    List<NextRoleByRuleVO> list = null;
    List<String> roles = null;

    try {
      ruleVO = new NextRoleByRuleVO();
      if (isMultiRule) {
        list = getNextRequesterRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      } else {
        list = getNextRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      }
      // Check Role has user associated if yes proceed if not the throw error to user

      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + checkVO.getNextRoleId() + "'");
          log4j.debug("next role id :" + checkVO.getNextRoleId());
          log4j.debug("ur :" + ur);
          if (ur == null || ur.list().size() == 0) {
            Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
            if ((objRole.isEscmIsspecializeddept() != null && !objRole.isEscmIsspecializeddept())) {
              ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
              ruleVO.setRoleName(objRole.getName());
              return ruleVO;
            }
          }

        }

      }

      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          // check role is line manager
          Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
          Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());
          if (specializedDept.getEscmDocStatus().equals("DR")) {
            if (objRole.isEscmIslinemanager() && !checkIsForwardedRole) {
              BusinessPartner objManager = null;
              flagLm = Boolean.TRUE;
              // check role associated requester line manager
              // get user details
              User objUser = OBDal.getInstance().get(User.class, requesterId);
              BusinessPartner objBp = objUser.getBusinessPartner();
              if (objBp != null) {
                String strManangerCode = objBp.getEhcmManager();
                // get Manager Details
                OBQuery<BusinessPartner> objManagerQuery = OBDal.getInstance().createQuery(
                    BusinessPartner.class, "as e where e.efinDocumentno='" + strManangerCode + "'");
                if (objManagerQuery.list().size() > 0) {
                  objManager = objManagerQuery.list().get(0);
                  if (objManager != null) {
                    User objManageruser = objManager.getADUserList().get(0);
                    if (objManageruser != null) {
                      // check role associated with Manager
                      OBQuery<UserRoles> userRoleList = OBDal.getInstance()
                          .createQuery(UserRoles.class, "as e where e.role.id='" + objRole.getId()
                              + "' and e.userContact.id='" + objManageruser.getId() + "'");
                      if (userRoleList.list().size() > 0) {
                        managerFlag = Boolean.TRUE;
                      } else {
                        managerFlag = Boolean.FALSE;
                      }
                    }
                  }
                }
              }
            }
          }

          // if nextrole is specialized is specialized department role then get role using
          // preferences

          if (objRole.isEscmIsspecializeddept() != null && objRole.isEscmIsspecializeddept()) {
            roles = getRolebasedOnPreference(specializedDept);

            if (roles.contains("ESCM_NoRoleAssociated")) {
              ruleVO.setErrorMsg("ESCM_NoRoleAssociated");
              return ruleVO;
            }
          }

        }
      }

      if (flagLm && !managerFlag) {
        ruleVO.setErrorMsg("NoManagerAssociatedWithRole");
      } else {
        if (list == null || list.size() == 0) {
          ruleVO.setStatus("");
          ruleVO.setShortStatus(Constants.vAPPROVED);
          ruleVO.setFullStatus(Constants.sAPPROVED);
          ruleVO.setNextRoleId(null);
          ruleVO.setApproval(false);
        } else {
          String status = "";

          // Create QU Next Role
          String headerId = SequenceIdData.getUUID();
          st = con.prepareStatement(
              "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
          st.setString(1, headerId);
          st.setString(2, clientId);
          st.setString(3, orgId);
          st.setString(4, userId);
          st.setString(5, userId);
          st.setString(6, documentType);
          st.executeUpdate();

          st = con.prepareStatement(
              "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, rolesequenceno,ad_user_id) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?,?);");
          for (NextRoleByRuleVO vo : list) {
            // check role is line manager
            Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
            Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());
            if (objRole.isEscmIslinemanager() && specializedDept.getEscmDocStatus().equals("DR")
                && !checkIsForwardedRole) {
              String managerUser = LineManagerUser(vo.getNextRoleId(), requesterId);
              if (StringUtils.isNotEmpty(managerUser)) {
                st.setString(1, clientId);
                st.setString(2, orgId);
                st.setString(3, userId);
                st.setString(4, userId);
                st.setString(5, headerId);
                st.setString(6, vo.getNextRoleId());
                st.setInt(7, vo.getRoleSeqNo());
                st.setString(8, managerUser);
                st.addBatch();

              }
            } else if (objRole.isEscmIsspecializeddept() != null
                && objRole.isEscmIsspecializeddept()) {
              st = con.prepareStatement(
                  "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, ad_user_id, dummy_role) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?, ?);");

              for (String role : roles) {
                st.setString(1, clientId);
                st.setString(2, orgId);
                st.setString(3, userId);
                st.setString(4, userId);
                st.setString(5, headerId);
                st.setString(6, role);
                st.setString(7, null);
                st.setString(8, objRole.getId());
                st.addBatch();
                Role nextPerformer = OBDal.getInstance().get(Role.class, role);
                if (nextPerformer != null) {
                  status += (" / " + nextPerformer.getName());
                } else {
                  status += (" / ");
                }

              }
            } else {
              st.setString(1, clientId);
              st.setString(2, orgId);
              st.setString(3, userId);
              st.setString(4, userId);
              st.setString(5, headerId);
              st.setString(6, vo.getNextRoleId());
              st.setInt(7, vo.getRoleSeqNo());
              st.setString(8, null);
              st.addBatch();
            }

            // if it is Specialized dept role then get role name from map else get by usual way
            if (!(objRole.isEscmIsspecializeddept() != null && objRole.isEscmIsspecializeddept())) {
              OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                  "role.id='" + vo.getNextRoleId() + "'");
              if (ur != null && ur.list().size() > 0) {
                if (ur.list().size() == 1) {
                  status += (" / " + ur.list().get(0).getRole().getName() + " / "
                      + ur.list().get(0).getUserContact().getName());
                } else {
                  status += (" / " + ur.list().get(0).getRole().getName());
                }
              }
            }

          }
          st.executeBatch();

          ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
          ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
          ruleVO
              .setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
          ruleVO.setNextRoleId(headerId);
          ruleVO.setApproval(true);
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * 
   * Based on Specialized Department get role from Preferences
   * 
   * @param Requistion
   * @return list of roles
   */

  @SuppressWarnings("unchecked")
  public List<String> getRolebasedOnPreference(Requisition requisition) {
    List<String> roles = new ArrayList<String>();
    String property = "";
    String sqlQuery = null;
    SQLQuery query = null;

    if (requisition.getEscmSpecializeddept().equals("IT")) {
      property = "ESCM_ITRole";
    } else if (requisition.getEscmSpecializeddept().equals("GS")) {
      property = "ESCM_GeneralServices";
    } else {
      // it wont happen
      return roles;
    }

    sqlQuery = "select distinct coalesce(rol.ad_role_id,prrol.ad_role_id) from ad_preference pref "
        + " left join ad_user usr on usr.ad_user_id=pref.ad_user_id and pref.visibleat_role_id is  null"
        + " left join ad_user_roles usroles on usroles.ad_user_id=usr.ad_user_id "
        + " left join ad_role rol on rol.ad_role_id=usroles.ad_role_id "
        + " left join ad_role prrol on prrol.ad_role_id=pref.visibleat_role_id "
        + " where property='" + property + "' and value='Y' and pref.ad_client_id='"
        + requisition.getClient().getId()
        + "'  and (ad_window_id='800092' or ad_window_id is null) ";

    query = OBDal.getInstance().getSession().createSQLQuery(sqlQuery);
    roles = (ArrayList<String>) query.list();

    // get role from preferences
    // final OBQuery<Preference> preference = OBDal.getInstance().createQuery(Preference.class,
    // "client.id='" + requisition.getClient().getId() + "' and property= '" + property
    // + "' and searchKey = 'Y' and eUTDelegationLine is null ");
    //
    // List<Preference> prefList = preference.list();
    // if (prefList.size() > 0) {
    // roles = prefList.stream().filter(a -> a.getVisibleAtRole() != null)
    // .map(a -> a.getVisibleAtRole().getId()).collect(Collectors.toList());
    // }
    if (roles.size() == 0) {
      roles.add("ESCM_NoRoleAssociated");
      return roles;
    }

    return roles;

  }

  /**
   * This method is to insert the NextRoleLine based on following condition
   * 
   * If next RoleList contains role with Agency manager flag checked, then get the Organization
   * manager defined for the agency organization present in Proposal Management.
   * 
   * If We have more than one Organization Manager then we have to get the organization manager with
   * latest updated
   * 
   * If no Agency Organization is defined for the Proposal then we have to skip the level in
   * document rule and go to next level defined in document rule
   * 
   * If No Organization Manager is present for Agency Organization, then we have to check the deputy
   * Minister Organization(ParentOrganization) - Organization Manager. If again not present, then
   * check minister Organization(parent Organization of deputy Minister) - Organization Manager.
   * 
   * If No Organization Manager is present in All the level then we should throw the error
   * 
   * Organization manager should have the Agency manager Role, if not throw error
   * 
   * @param connection
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return nextRole header Object
   */
  public NextRoleByRuleVO getAgencyManagerBasedNextRole(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue,
      String requesterId, Boolean isMultiRule, String strstatus, EscmProposalMgmt proposal) {

    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    List<NextRoleByRuleVO> list = null;
    ArrayList<String> agencyManagerList = null;

    try {
      ruleVO = new NextRoleByRuleVO();

      list = getNextRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);

      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + checkVO.getNextRoleId() + "'");
          log4j.debug("next role id :" + checkVO.getNextRoleId());
          log4j.debug("ur :" + ur);
          if (ur == null || ur.list().size() == 0) {
            Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
            ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
            ruleVO.setRoleName(objRole.getName());
            return ruleVO;
          }
        }
      }

      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          // check role is line manager
          Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
          Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());
          if (objRole.isEscmIsagencymanager() != null && objRole.isEscmIsagencymanager()
              && !checkIsForwardedRole) {
            agencyManagerList = getOrganizationManager(proposal.getAgencyorg().getId(), objRole);
            if (agencyManagerList != null && agencyManagerList.size() > 0) {
              if (agencyManagerList
                  .contains(Constants.ESCMERROR + "-" + Constants.ESCMORGANIZATIONMANAGER)) {
                ruleVO.setErrorMsg(agencyManagerList.get(0));
                return ruleVO;
              }
              for (String agency : agencyManagerList) {
                if (agency.contains("ESCMROLENOTEXIST")) {
                  ruleVO.setErrorMsg(agency);
                  return ruleVO;
                } else if (agency.contains("ESCMUSERNOTEXIST")) {
                  ruleVO.setErrorMsg(agency);
                  return ruleVO;
                }
              }

            }
          }
        }
      }

      if (list == null || list.size() == 0) {
        ruleVO.setStatus("");
        ruleVO.setShortStatus(Constants.vAPPROVED);
        ruleVO.setFullStatus(Constants.sAPPROVED);
        ruleVO.setNextRoleId(null);
        ruleVO.setApproval(false);
      } else {
        String status = "";

        // Create Next Role
        String headerId = SequenceIdData.getUUID();
        st = con.prepareStatement(
            "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) VALUES (?, ?, ?, ?, ?, ?);");
        st.setString(1, headerId);
        st.setString(2, clientId);
        st.setString(3, orgId);
        st.setString(4, userId);
        st.setString(5, userId);
        st.setString(6, documentType);
        st.executeUpdate();

        st = con.prepareStatement(
            "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby, eut_next_role_id, ad_role_id, rolesequenceno,ad_user_id) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?,?);");
        for (NextRoleByRuleVO vo : list) {
          // check role is Agency manager
          Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
          if (objRole.isEscmIsagencymanager() != null && objRole.isEscmIsagencymanager()) {
            for (String managerUser : agencyManagerList) {
              st.setString(1, clientId);
              st.setString(2, orgId);
              st.setString(3, userId);
              st.setString(4, userId);
              st.setString(5, headerId);
              st.setString(6, vo.getNextRoleId());
              st.setInt(7, vo.getRoleSeqNo());
              st.setString(8, managerUser);
              st.addBatch();
            }
          } else {
            st.setString(1, clientId);
            st.setString(2, orgId);
            st.setString(3, userId);
            st.setString(4, userId);
            st.setString(5, headerId);
            st.setString(6, vo.getNextRoleId());
            st.setInt(7, vo.getRoleSeqNo());
            st.setString(8, null);
            st.addBatch();
          }

          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + vo.getNextRoleId() + "'");
          if (ur != null && ur.list().size() > 0) {
            if (ur.list().size() == 1) {
              status += (" / " + ur.list().get(0).getRole().getName() + " / "
                  + ur.list().get(0).getUserContact().getName());
            } else {
              status += (" / " + ur.list().get(0).getRole().getName());
            }
          }

        }
        st.executeBatch();

        ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
        ruleVO.setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
        ruleVO.setNextRoleId(headerId);
        ruleVO.setApproval(true);
      }

    } catch (final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * This method to get the Organization Manager for the Agency organization
   * 
   * @param proposal
   * @return List with Organization manager(User)
   */
  private ArrayList<String> getOrganizationManager(String orgId, Role objRole) {

    Organization agencyOrg = OBDal.getInstance().get(Organization.class, orgId);
    List<EhcmOrgManager> orgManagerList = agencyOrg.getEhcmOrgManagerList();
    List<EhcmOrgManager> sortedOrgManagerList = agencyOrg.getEhcmOrgManagerList();
    ArrayList<String> organizationManager = new ArrayList<String>();
    BusinessPartner bp = null;
    String ministerOrgId = null;

    if (orgManagerList != null && orgManagerList.size() > 0) {
      sortedOrgManagerList = orgManagerList.stream()
          .sorted((e1, e2) -> e2.getCreationDate().compareTo(e1.getCreationDate()))
          .collect(Collectors.toList());
      if (sortedOrgManagerList.get(0).getBusinessPartner() != null) {
        bp = OBDal.getInstance().get(BusinessPartner.class,
            sortedOrgManagerList.get(0).getBusinessPartner().getId());
      }

      OBQuery<User> user = OBDal.getInstance().createQuery(User.class,
          "as e where e.businessPartner.id='"
              + sortedOrgManagerList.get(0).getBusinessPartner().getId() + "'");
      List<User> userList = user.list();

      if (userList != null && userList.size() > 0) {
        for (User userId : userList) {
          OBQuery<UserRoles> userRoleList = OBDal.getInstance().createQuery(UserRoles.class,
              "as e where e.role.id='" + objRole.getId() + "' and e.userContact.id='"
                  + userId.getId() + "'");
          if (userRoleList.list().size() > 0) {
            organizationManager.add(userId.getId());
          } else {
            organizationManager.add(Constants.ESCMROLENOTEXIST + " " + userId.getName());
          }
        }
      } else {
        organizationManager
            .add(Constants.ESCMUSERNOTEXIST + " " + (bp != null ? bp.getName() : ""));
      }
    } else {
      ministerOrgId = getParentOrganization(agencyOrg.getId(), agencyOrg.getClient().getId());
      if (!ministerOrgId.equals(Constants.ESCMERROR)) {
        return getOrganizationManager(ministerOrgId, objRole);
      } else {
        organizationManager.add(Constants.ESCMERROR + "-" + Constants.ESCMORGANIZATIONMANAGER);
      }
    }
    return organizationManager;

  }

  /**
   * This Method is to get the Parent Organization
   * 
   * @param id
   * @param clientId
   * @return minister or deputy minister org id
   */
  private String getParentOrganization(String id, String clientId) {
    StringBuffer whereClause = new StringBuffer();

    whereClause.append("  as node ");
    whereClause.append(" join  node.tree tree ");
    whereClause.append(
        " where  node.node  =:nodeId and tree.typeArea ='OO' and tree.client.id =:clientId ");

    OBQuery<TreeNode> treeNodeList = OBDal.getInstance().createQuery(TreeNode.class,
        whereClause.toString());
    treeNodeList.setNamedParameter("nodeId", id);
    treeNodeList.setNamedParameter("clientId", clientId);

    List<TreeNode> orgList = treeNodeList.list();

    if (orgList.size() > 0) {
      if (orgList.get(0).getReportSet() != null) {
        Organization org = OBDal.getInstance().get(Organization.class,
            orgList.get(0).getReportSet());
        if (org.getEhcmOrgtyp() != null) {
          if (org.getEhcmOrgtyp().getSearchKey().contains("DEPUTYMINISTER")
              || org.getEhcmOrgtyp().getSearchKey().contains("MINISTER")) {
            return org.getId();
          } else {
            return getParentOrganization(org.getId(), clientId);
          }
        } else {
          return getParentOrganization(org.getId(), clientId);
        }
      } else {
        return Constants.ESCMERROR;
      }
    }
    return Constants.ESCMERROR;
  }

  /**
   * 
   * @param clientId
   * @param paramOrgId
   * @param userId
   * @param documentType
   * @param quNextRoleId
   * @return
   */
  public HashMap<String, String> getDelegatedFromAndToRolesandUsers(String clientId,
      String paramOrgId, String userId, String documentType, String quNextRoleId) {
    PreparedStatement st = null, st1 = null;
    ResultSet rs = null, rs1 = null;
    HashMap<String, String> roleId = new HashMap<String, String>();
    String orgId = paramOrgId;
    SQLQuery qry = null;
    try { // qdd.ad_role_id -> From Role Id , qddl.ad_user_id -> To User Id
          // Getting Organization
      qry = OBDal.getInstance().getSession()
          .createSQLQuery(" select eut_documentrule_parentorg(?, ?, ?); ");
      qry.setParameter(0, clientId);
      qry.setParameter(1, orgId);
      qry.setParameter(2, documentType);
      if (qry.list().size() > 0) {
        orgId = (String) qry.list().get(0);
      }

      qry = OBDal.getInstance().getSession().createSQLQuery(
          " select qdd.ad_role_id as fromuserrole, qddl.ad_role_id as touserrole, rlne.rolesequenceno , qdd.ad_user_id as fromuserid , qddl.ad_user_id as touserid from eut_docapp_delegate qdd "
              + " join eut_docapp_delegateln qddl on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id "
              + " join eut_documentrule_lines rlne on rlne.ad_role_id=qdd.ad_role_id "
              + " join eut_documentrule_header rhdr on rhdr.eut_documentrule_header_id=rlne.eut_documentrule_header_id "
              + " and rhdr.document_type = ? and rhdr.ad_org_id= ?  "
              + " where qddl.ad_user_id = ? and qddl.document_type = ? "
              + " and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) "
              // + " and qdd.ad_role_id in (select ad_role_id from eut_next_role qnr left join
              // eut_next_role_line qnrl on qnr.eut_next_role_id = qnrl.eut_next_role_id where
              // qnr.eut_next_role_id = ?) "
              + " order by rolesequenceno desc limit 1 ; ");

      qry.setParameter(0, documentType);
      qry.setParameter(1, orgId);
      qry.setParameter(2, userId);
      qry.setParameter(3, documentType);
      if (qry.list().size() > 0) {
        Object[] row = (Object[]) qry.list().get(0);
        roleId.put("FromUserRoleId", row[0] == null ? "" : row[0].toString());
        roleId.put("ToUserRoleId", row[1] == null ? "" : row[1].toString());
        roleId.put("ToUserId", row[4] == null ? "" : row[4].toString());
        roleId.put("FromUserId", row[3] == null ? "" : row[3].toString());
      }

      /*
       * st = con.prepareStatement("select eut_documentrule_parentorg(?, ?, ?);"); st.setString(1,
       * clientId); st.setString(2, orgId); st.setString(3, documentType); rs = st.executeQuery();
       * if (rs.next()) orgId = rs.getString("eut_documentrule_parentorg");
       * 
       * st1 = con.prepareStatement(
       * " select qdd.ad_role_id as fromuserrole, qddl.ad_role_id as touserrole, rlne.rolesequenceno , qdd.ad_user_id as fromuserid , qddl.ad_user_id as touserid from eut_docapp_delegate qdd "
       * +
       * " join eut_docapp_delegateln qddl on qdd.eut_docapp_delegate_id = qddl.eut_docapp_delegate_id "
       * + " join eut_documentrule_lines rlne on rlne.ad_role_id=qdd.ad_role_id " +
       * " join eut_documentrule_header rhdr on rhdr.eut_documentrule_header_id=rlne.eut_documentrule_header_id "
       * + " and rhdr.document_type = ? and rhdr.ad_org_id= ?  " +
       * " where qddl.ad_user_id = ? and qddl.document_type = ? " +
       * " and cast(now() as date) between cast(qdd.from_date as date) and cast(coalesce(qdd.to_date, to_date('31-12-9999 23:59:59','dd-MM-yyyy HH24:mi:ss')) as date) "
       * // + " and qdd.ad_role_id in (select ad_role_id from eut_next_role qnr left join //
       * eut_next_role_line qnrl on qnr.eut_next_role_id = qnrl.eut_next_role_id where //
       * qnr.eut_next_role_id = ?) " + " order by rolesequenceno desc limit 1 "); st1.setString(1,
       * documentType); st1.setString(2, orgId); st1.setString(3, userId); st1.setString(4,
       * documentType); log4j.debug("St:" + st1.toString()); // st.setString(5, quNextRoleId); rs1 =
       * st1.executeQuery(); if (rs1.next()) { roleId.put("FromUserRoleId",
       * rs1.getString("fromuserrole")); roleId.put("ToUserRoleId", rs1.getString("touserrole"));
       * roleId.put("ToUserId", rs1.getString("touserid")); roleId.put("FromUserId",
       * rs1.getString("fromuserid")); }
       */
    } catch (final Exception e) {
      log4j.error("Exception in getDelegatedFromAndToRolesandUsers :", e);
      return roleId;
    } finally {
      try {
        if (st != null)
          st.close();
        if (rs != null)
          rs.close();
        if (st1 != null)
          st1.close();
        if (rs1 != null)
          rs1.close();
      } catch (SQLException e) {
      }
    }
    return roleId;
  }

  /**
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param documentType
   * @param pValue
   * @return nextRole header Object
   */
  public NextRoleByRuleVO getLineManagerBasedNextRoleRDVLastVersion(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue,
      String requesterId, Boolean isMultiRule, String strstatus, boolean isContractCategoryRole,
      String contractCategoryId) {
    String docStatus = strstatus;
    NextRoleByRuleVO ruleVO = null;
    PreparedStatement st = null;
    Boolean managerFlag = Boolean.FALSE, hasInvoiceManager = Boolean.TRUE;
    Boolean flagLm = Boolean.FALSE;
    List<NextRoleByRuleVO> list = null;
    List<NextRoleByRuleVO> newList = new ArrayList<>();
    Map<String, List<String>> hrLineManagerMap = null;
    String userName = "", strInvoiceManager = "";
    try {
      ruleVO = new NextRoleByRuleVO();
      User objManageruser = null;
      User currentUser = OBDal.getInstance().get(User.class, requesterId);
      if (currentUser != null) {
        userName = currentUser.getName();
      }
      if (isMultiRule) {
        list = getNextRequesterRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      } else {
        list = getNextRoleList(con, clientId, orgId, roleId, userId, documentType, pValue);
      }

      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + checkVO.getNextRoleId() + "'");
          log4j.debug("next role id :" + checkVO.getNextRoleId());
          if (ur == null || ur.list().size() == 0) {
            Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
            if ((objRole.isEscmIshrlinemanager() != null && !objRole.isEscmIshrlinemanager())
                || (!documentType.equals("EUT_113"))) {
              ruleVO.setErrorMsg("EUT_NOUser_ForRoles");
              ruleVO.setRoleName(objRole.getName());
              // OBMessageUtils.messageBD("EUT_NOUser_ForRoles").replaceAll("@",
              // objRole.getName()));
              return ruleVO;
            }
          }
        }
      }

      // Check next role has contract category access
      if (list != null && list.size() > 0 && isContractCategoryRole) {
        for (NextRoleByRuleVO checkVO : list) {

          Role role = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());
          if (role != null) {
            Boolean contractCategoryPresent = false;
            for (EutLookupAccess access : role.getEutLookupAccessList()) {
              if (access.getEscmDeflookupsTypeln().getId().equals(contractCategoryId)
                  && access.isActive() && access.getEscmDeflookupsTypeln().isActive()) {
                contractCategoryPresent = true;
                break;
              }
            }

            if (contractCategoryPresent) {
              newList.add(checkVO);
            }

          }
        }

        if (newList.size() == 0) {
          log4j.debug("list size" + list.size());
          ruleVO.setErrorMsg("EUT_Nocontractcategoryaccess");
          return ruleVO;
        } else {
          list = newList;
        }

      }

      if (list != null && list.size() > 0) {
        for (NextRoleByRuleVO checkVO : list) {
          // check role is line manager
          Role objRole = OBDal.getInstance().get(Role.class, checkVO.getNextRoleId());

          if (objRole.isEscmIslinemanager()) {
            BusinessPartner objManager = null;
            flagLm = Boolean.TRUE;
            // check role associated requester line manager
            // get user details
            User objUser = OBDal.getInstance().get(User.class, requesterId);
            BusinessPartner objBp = objUser.getBusinessPartner();
            if (objBp != null) {
              String strManangerCode = objBp.getEhcmManager();
              // get Manager Details

              if (documentType.equals("EUT_101") || documentType.equals("EUT_110")
                  || documentType.equals("EUT_109")) {
                strInvoiceManager = getInvoiceManager(objUser);
                if (StringUtils.isEmpty(strInvoiceManager)) {
                  managerFlag = Boolean.FALSE;
                  hasInvoiceManager = Boolean.FALSE;
                } else
                  managerFlag = Boolean.TRUE;

              }
              // encumbrance
              else if (documentType.equals("EUT_105")) {
                strInvoiceManager = getUserManager(objUser, Constants.ENCUMBRANCE_DOCTYPE);
                if (StringUtils.isEmpty(strInvoiceManager)) {
                  managerFlag = Boolean.FALSE;
                  hasInvoiceManager = Boolean.FALSE;
                } else
                  managerFlag = Boolean.TRUE;

              } // rdv lastversion
              else if (documentType.equals("EUT_127")) {
                strInvoiceManager = getUserManager(objUser, "RDV");
                if (StringUtils.isEmpty(strInvoiceManager)) {
                  managerFlag = Boolean.FALSE;
                  hasInvoiceManager = Boolean.FALSE;
                } else
                  managerFlag = Boolean.TRUE;
              } else {
                OBQuery<BusinessPartner> objManagerQuery = null;
                List<BusinessPartner> manager = new ArrayList<BusinessPartner>();
                objManagerQuery = OBDal.getInstance().createQuery(BusinessPartner.class,
                    "as e where e.efinDocumentno='" + strManangerCode + "'");
                if (objManagerQuery != null) {
                  manager = objManagerQuery.list();
                  if (manager.size() > 0) {
                    objManager = manager.get(0);
                    if (objManager != null) {
                      if (objManager.getADUserList() != null
                          && objManager.getADUserList().size() > 0) {
                        objManageruser = objManager.getADUserList().get(0);
                      }
                    }
                  }
                }
              }
              if (StringUtils.isNotEmpty(strInvoiceManager)) {
                objManageruser = Utility.getObject(User.class, strInvoiceManager);
              }
              if (objManageruser != null) {
                // check role associated with Manager
                OBQuery<UserRoles> userRoleList = OBDal.getInstance().createQuery(UserRoles.class,
                    "as e where e.role.id='" + objRole.getId() + "' and e.userContact.id='"
                        + objManageruser.getId() + "'");
                if (userRoleList.list().size() > 0) {
                  managerFlag = Boolean.TRUE;
                  break;
                } else {
                  managerFlag = Boolean.FALSE;
                }
              }
            }
          }
        }
      }

      if (documentType.equals("EUT_113")) {
        // Check Line manager user is associated for requester if nextrole is dummy role
        for (NextRoleByRuleVO vo : list) {
          Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
          if (objRole.isEscmIshrlinemanager() != null && objRole.isEscmIshrlinemanager()) {
            hrLineManagerMap = hrLineManagerUserAndRole(vo.getNextRoleId(), requesterId);
            log4j.debug("hrLineManagerMapintial" + hrLineManagerMap);

            if (hrLineManagerMap == null || hrLineManagerMap.isEmpty()
                || hrLineManagerMap.size() == 0) {
              log4j.debug("hrLineManagerMap" + hrLineManagerMap.size());
              ruleVO.setErrorMsg("EUT_NOlineMngr_requester");
              ruleVO.setUserName(userName);
              return ruleVO;
            }
          }
        }
      }

      if (flagLm && !managerFlag && (docStatus.equals("DR") || docStatus.equals("REJ"))) {
        if (hasInvoiceManager)
          ruleVO.setErrorMsg("NoManagerAssociatedWithRole");
        else
          ruleVO.setErrorMsg("Managernotdefined");
      } else {
        if (list == null || list.size() == 0) {
          ruleVO.setStatus(Constants.sAPPROVED);
          ruleVO.setShortStatus(Constants.vAPPROVED);
          ruleVO.setFullStatus(Constants.sAPPROVED);
          ruleVO.setNextRoleId(null);
          ruleVO.setApproval(false);
        } else {
          String status = "";

          // Create QU Next Role
          String headerId = SequenceIdData.getUUID();
          st = con.prepareStatement(
              "INSERT INTO eut_next_role(eut_next_role_id, ad_client_id, ad_org_id, createdby, updatedby, document_type) "
                  + "VALUES (?, ?, ?, ?, ?, ?);");
          st.setString(1, headerId);
          st.setString(2, clientId);
          st.setString(3, orgId);
          st.setString(4, userId);
          st.setString(5, userId);
          st.setString(6, documentType);
          st.executeUpdate();

          st = con.prepareStatement(
              "INSERT INTO eut_next_role_line(eut_next_role_line_id, ad_client_id, ad_org_id, createdby, updatedby,"
                  + " eut_next_role_id, ad_role_id, rolesequenceno,ad_user_id) VALUES (get_uuid(), ?, ?, ?, ?, ?, ?, ?,?);");
          for (NextRoleByRuleVO vo : list) {
            // check role is line manager
            Role objRole = OBDal.getInstance().get(Role.class, vo.getNextRoleId());
            Boolean isHrLineMgr = Boolean.FALSE;
            String managerUser = "";
            Boolean checkIsForwardedRole = NextRoleByRuleDAO.checkIsForwardedRole(objRole.getId());
            if (objRole.isEscmIslinemanager() && (docStatus.equals("DR") || docStatus.equals("REJ"))
                && !checkIsForwardedRole) {
              if (documentType.equals("EUT_101") || documentType.equals("EUT_110")
                  || documentType.equals("EUT_109") || documentType.equals("EUT_105")
                  || documentType.equals("EUT_127")) {
                managerUser = strInvoiceManager;
              } else {
                managerUser = LineManagerUser(vo.getNextRoleId(), requesterId);
              }
              if (StringUtils.isNotEmpty(managerUser)) {
                st.setString(1, clientId);
                st.setString(2, orgId);
                st.setString(3, userId);
                st.setString(4, userId);
                st.setString(5, headerId);
                st.setString(6, vo.getNextRoleId());
                st.setInt(7, vo.getRoleSeqNo());
                st.setString(8, managerUser);
                st.addBatch();

              }
            } else {
              st.setString(1, clientId);
              st.setString(2, orgId);
              st.setString(3, userId);
              st.setString(4, userId);
              st.setString(5, headerId);
              st.setString(6, vo.getNextRoleId());
              st.setInt(7, vo.getRoleSeqNo());
              st.setString(8, null);
              st.addBatch();
            }

            if (!isHrLineMgr) {
              OBQuery<UserRoles> ur = OBDal.getInstance().createQuery(UserRoles.class,
                  "role.id='" + vo.getNextRoleId() + "'");
              if (ur != null && ur.list().size() > 0) {
                if (ur.list().size() == 1) {
                  status += (" / " + ur.list().get(0).getRole().getName() + " / "
                      + ur.list().get(0).getUserContact().getName());
                } else {
                  if (StringUtils.isNotEmpty(managerUser)) {
                    User obManagerUser = OBDal.getInstance().get(User.class, managerUser);
                    status += (" / " + ur.list().get(0).getRole().getName() + " / "
                        + obManagerUser.getName());
                  } else
                    status += (" / " + ur.list().get(0).getRole().getName());
                }

              }
            }

          }
          st.executeBatch();

          ruleVO.setStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
          ruleVO.setShortStatus(Constants.vWAITINGFORAPPROVAL);
          ruleVO
              .setFullStatus(String.format(Constants.sWAITINGFOR_S_APPROVAL, status.substring(3)));
          ruleVO.setNextRoleId(headerId);
          ruleVO.setApproval(true);
        }
      }
    } catch (

    final Exception e) {
      log4j.error("Exception in getNextRole() Method", e);
      return null;
    }
    return ruleVO;
  }

  /**
   * 
   * @param objRole
   * @return true if it is forwarded or delegated user
   */

  public static boolean checkIsForwardedRole(String objRole) {
    try {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
      Date date = new Date();
      OBQuery<EutDelegateRoleCheck> check = OBDal.getInstance().createQuery(
          EutDelegateRoleCheck.class,
          "as e where e. newValue='Y' and columname='LM' and (e. endingDate is null or e. endingDate> '"
              + formatter.format(date) + "') and e.role.id ='" + objRole + "'");
      if (check.list().size() > 0) {
        return true;
      }
    } catch (OBException e) {

    }
    return false;
  }
}