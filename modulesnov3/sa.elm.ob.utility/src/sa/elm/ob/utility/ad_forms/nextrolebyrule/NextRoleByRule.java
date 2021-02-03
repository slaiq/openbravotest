package sa.elm.ob.utility.ad_forms.nextrolebyrule;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;

public class NextRoleByRule {
  public static boolean haveAccesstoWindow(String clientId, String orgId, String roleId,
      String userId, String documentType) {
    return NextRoleByRuleDAO.getInstance().haveAccesstoWindow(clientId, orgId, roleId, userId,
        documentType);
  }

  public static NextRoleByRuleVO getNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, Object pValue) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getNextRole(con, clientId, orgId, roleId, userId,
        documentType, pValue);
  }

  public static NextRoleByRuleVO getRequesterNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, Object pValue) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getRequesterNextRole(con, clientId, orgId, roleId,
        userId, documentType, pValue);
  }

  public static NextRoleByRuleVO getLineManagerBasedNextRole(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue,
      String requesterId, Boolean isMultiRule, String strStatus) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getLineManagerBasedNextRole(con, clientId, orgId, roleId,
        userId, documentType, pValue, requesterId, isMultiRule, strStatus);
  }

  public static NextRoleByRuleVO getUserManagerBasedNextRole(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue,
      String requesterId, Boolean isMultiRule, String strStatus) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getUserManagerBasedNextRole(con, clientId, orgId, roleId,
        userId, documentType, pValue, requesterId, isMultiRule, strStatus);
  }

  public static NextRoleByRuleVO getAgencyManagerBasedNextRole(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue,
      String requesterId, Boolean isMultiRule, String strStatus, EscmProposalMgmt Proposal) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getAgencyManagerBasedNextRole(con, clientId, orgId,
        roleId, userId, documentType, pValue, requesterId, isMultiRule, strStatus, Proposal);
  }

  public static NextRoleByRuleVO getSpecializedDeptBasedNextRole(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue,
      String requesterId, Boolean isMultiRule, Requisition dept) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getSpecializedDeptBasedNextRole(con, clientId, orgId,
        roleId, userId, documentType, pValue, requesterId, isMultiRule, dept);
  }

  public static NextRoleByRuleVO getInvNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, Object pValue, String recordId) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getInvNextRole(con, clientId, orgId, roleId, userId,
        documentType, pValue, recordId);
  }

  public static NextRoleByRuleVO getCustTranNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, String documentType, ShipmentInOut header, String NextUserId,
      String approvalFlow, Long approvalLevel) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getCustTranNextRole(con, clientId, orgId, roleId, userId,
        documentType, header, NextUserId, approvalFlow, approvalLevel);
  }

  public static List<NextRoleByRuleVO> getNextRoleList(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getNextRoleList(con, clientId, orgId, roleId, userId,
        documentType, pValue);
  }

  public static List<NextRoleByRuleVO> getNextRequesterRoleList(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getNextRequesterRoleList(con, clientId, orgId, roleId,
        userId, documentType, pValue);
  }

  public static boolean isBackwardDelegation(Connection con, String clientId, String orgId,
      String roleId, String toRoleId, String userId, String documentType, Object pValue) {
    return NextRoleByRuleDAO.getInstance().isBackwardDelegation(con, clientId, orgId, roleId,
        toRoleId, userId, documentType, pValue);
  }

  public static NextRoleByRuleVO getDelegatedNextRole(Connection con, String clientId, String orgId,
      String fromUserRoleId, String toUserRoleId, String userId, String documentType,
      Object pValue) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getDelegatedNextRole(con, clientId, orgId,
        fromUserRoleId, toUserRoleId, userId, documentType, pValue);
  }

  public static NextRoleByRuleVO getRequesterDelegatedNextRole(Connection con, String clientId,
      String orgId, String fromUserRoleId, String toUserRoleId, String userId, String documentType,
      String reqRole) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getRequesterDelegatedNextRole(con, clientId, orgId,
        fromUserRoleId, toUserRoleId, userId, documentType, reqRole);
  }

  public static List<NextRoleByRuleVO> getDelegatedNextRoleList(Connection con, String clientId,
      String orgId, String fromUserRoleId, String toUserRoleId, String userId, String documentType,
      Object pValue) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getDelegatedNextRoleList(con, clientId, orgId,
        fromUserRoleId, toUserRoleId, userId, documentType, pValue);
  }

  public static boolean deleteUnusedNextRoles(Connection con, String documentType) {
    return DocumentRuleDAO.deleteUnusedNextRoles(con, documentType);
  }

  public static HashMap<String, String> getDelegatedFromAndToRoles(Connection con, String clientId,
      String orgId, String userId, String documentType, String quNextRoleId) {
    return NextRoleByRuleDAO.getInstance().getDelegatedFromAndToRoles(con, clientId, orgId, userId,
        documentType, quNextRoleId);
  }

  public static HashMap<String, String> getDelegatedFromAndToRolesandUsers(String clientId,
      String orgId, String userId, String documentType, String quNextRoleId) {
    return NextRoleByRuleDAO.getInstance().getDelegatedFromAndToRolesandUsers(clientId, orgId,
        userId, documentType, quNextRoleId);
  }

  public static HashMap<String, String> getDelegatedUserAndRole(Connection con, String clientId,
      String orgId, String userId, String documentType) {
    return NextRoleByRuleDAO.getInstance().getDelegatedUserAndRole(con, clientId, orgId, userId,
        documentType);
  }

  public static NextRoleByRuleVO getLineManagerBasedNextRoleRDVLastVersion(Connection con,
      String clientId, String orgId, String roleId, String userId, String documentType,
      Object pValue, String requesterId, Boolean isMultiRule, String strstatus,
      boolean isContractCategoryRole, String contractCategoryId) {
    return NextRoleByRuleDAO.getInstance().getLineManagerBasedNextRoleRDVLastVersion(con, clientId,
        orgId, roleId, userId, documentType, pValue, requesterId, isMultiRule, strstatus,
        isContractCategoryRole, contractCategoryId);
  }

  public static HashMap<String, String> getDelegatedFromAndToRolesForDummyRoles(Connection con,
      String clientId, String orgId, String userId, String documentType, String quNextRoleId,
      String transOrgId) {
    return NextRoleByRuleDAO.getInstance().getDelegatedFromAndToRolesForDummyRoles(con, clientId,
        orgId, userId, documentType, quNextRoleId, transOrgId);
  }

  public static HashMap<String, String> getbackwardDelegatedFromAndToRoles(Connection con,
      String clientId, String orgId, String userId, String documentType, String quNextRoleId) {
    return NextRoleByRuleDAO.getInstance().getbackwardDelegatedFromAndToRoles(con, clientId, orgId,
        userId, documentType, quNextRoleId);
  }

  public static HashMap<String, String> getDelegatedFromAndToRolesInvoice(Connection con,
      String clientId, String orgId, String userId, String documentType, String quNextRoleId,
      String roleId, String delegationRole, String delegationuser, Object pValue,
      String costCenterId) {
    return NextRoleByRuleDAO.getInstance().getDelegatedFromAndToRolesInvoice(con, clientId, orgId,
        userId, documentType, quNextRoleId, roleId, delegationRole, delegationuser, pValue,
        costCenterId);
  }

  public static NextRoleByRuleVO getMIRRevokeRequesterNextRole(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getRequesterNextRole(con, clientId, orgId, roleId,
        userId, documentType, pValue);
  }

  public static NextRoleByRuleVO getBudgetDistNextRole(Connection con, String clientId,
      String orgId, String roleId, String userId, String documentType, Object pValue) {
    deleteUnusedNextRoles(con, documentType);
    return NextRoleByRuleDAO.getInstance().getBudgetDistNextRole(con, clientId, orgId, roleId,
        userId, documentType, pValue);
  }
}