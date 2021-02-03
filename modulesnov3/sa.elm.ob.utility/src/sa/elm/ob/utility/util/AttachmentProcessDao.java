package sa.elm.ob.utility.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.procurement.Requisition;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;

/**
 * @author poongodi on 27/01/2020
 */
public class AttachmentProcessDao {
  private static final Logger log4j = Logger.getLogger(AttachmentProcessDao.class);

  /**
   * 
   * @param proposal
   * @param sessionRoleId
   * @param attachmentId
   * @param sessionUserId
   * @return
   */
  public static boolean chkPresentRoleIsWaitingApp(String tabId, String recordId,
      String sessionRoleId, String attachmentId, String sessionUserId) {
    boolean success = false;
    String roleId = null;
    EutNextRoleLine eutNextRole = null;
    String userContact = null;
    boolean allowDelegation = false;
    Date currentDate = new Date();
    DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
    String delegationLineId = null;
    EutDocappDelegateln delegationLine = null;
    DocumentTypeE DocumentType = null;
    String forward_RmiId = null;
    EscmProposalMgmt proposal = null;
    Order ord = null;
    Requisition requisition = null;
    String nextRoleID = null;
    try {

      if (tabId.equals(Constants.PROPOSAL_MANAGEMENT_TAB)) {
        proposal = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
      }
      if (tabId.equals(Constants.PURCHASE_ORDER_TAB)) {
        ord = OBDal.getInstance().get(Order.class, recordId);
      }
      if (tabId.equals(Constants.PURCHASE_REQUISITION_TAB)) {
        requisition = OBDal.getInstance().get(Requisition.class, recordId);
      }
      if (proposal != null) {
        if (proposal.getProposalType().equals("DR")) {
          DocumentType = DocumentTypeE.DIRECT_PROPOSAL;
        } else if (proposal.getProposalType().equals("LD")
            || proposal.getProposalType().equals("TR")) {
          DocumentType = DocumentTypeE.PROPOSAL_LIMITED_TENDER;
        }
        nextRoleID = proposal.getEUTNextRole().getId();
      } else if (ord != null) {
        DocumentType = DocumentTypeE.PURCHASE_ORDER;
        nextRoleID = ord.getEutNextRole().getId();
      } else if (requisition != null) {
        if (requisition.getEscmProcesstype().equals("DP")) {
          DocumentType = DocumentTypeE.PR_DIRECT;
        } else if (requisition.getEscmProcesstype().equals("LB")
            || requisition.getEscmProcesstype().equals("PB")) {
          DocumentType = DocumentTypeE.PR_LIMITED_TENDER;
        }
        nextRoleID = requisition.getEutNextRole().getId();
      }

      Attachment attachFile = OBDal.getInstance().get(Attachment.class, attachmentId);
      // nextRole check
      if (nextRoleID != null) {
        OBQuery<EutNextRoleLine> line = OBDal.getInstance().createQuery(EutNextRoleLine.class,
            " as e where e.eUTNextRole.id = :NextRoleID order by creationDate desc");
        line.setNamedParameter("NextRoleID", nextRoleID);
        line.setMaxResult(1);
        if (line != null && line.list().size() > 0) {
          eutNextRole = line.list().get(0);
          roleId = eutNextRole.getRole().getId();
        }
      }

      if (roleId != null && roleId.equals(sessionRoleId)) {
        if (attachFile != null && attachFile.getEutSourceid() == null
            && attachFile.getEutVersionNo() == null) {

          if (attachFile != null) {
            userContact = attachFile.getCreatedBy().getId();
            if (userContact.equals(sessionUserId)) {
              success = true;
            }
          }
        }

        if (attachFile != null && attachFile.getEutSourceid() != null
            && attachFile.getEutVersionNo() != null) {
          if (roleId != null && roleId.equals(sessionRoleId) && eutNextRole != null
              && (eutNextRole.getEUTReqmoreinfo() != null
                  || eutNextRole.getEUTForwardReqmoreinfo() != null)) {
            if (eutNextRole.getEUTReqmoreinfo() != null) {
              forward_RmiId = eutNextRole.getEUTReqmoreinfo().getId();
            } else if (eutNextRole.getEUTForwardReqmoreinfo() != null) {
              forward_RmiId = eutNextRole.getEUTForwardReqmoreinfo().getId();
            }
            EutForwardReqMoreInfo requestMoreInfo = OBDal.getInstance()
                .get(EutForwardReqMoreInfo.class, forward_RmiId);
            if (requestMoreInfo != null
                && requestMoreInfo.getRECRole().getId().equals(sessionRoleId)) {
              Attachment file = OBDal.getInstance().get(Attachment.class, attachmentId);
              if (file != null) {
                userContact = file.getCreatedBy().getId();
                if (userContact.equals(sessionUserId)) {
                  success = true;
                }
              }

            }
          }
        }
      }
      // delegation
      else {
        allowDelegation = delagationDao.checkDelegation(currentDate, sessionRoleId,
            DocumentType.getDocumentTypeCode());
        if (allowDelegation) {
          delegationLineId = getDelegationObj(currentDate, sessionRoleId,
              DocumentType.getDocumentTypeCode());
          if (delegationLineId != null)
            delegationLine = OBDal.getInstance().get(EutDocappDelegateln.class, delegationLineId);
          if (delegationLine != null && delegationLine.getRole().getId().equals(sessionRoleId)) {
            Attachment file = OBDal.getInstance().get(Attachment.class, attachmentId);
            if (file != null) {
              userContact = file.getCreatedBy().getId();
              if (userContact
                  .equals(delegationLine.getEUTDocappDelegate().getUserContact().getId())) {
                success = true;
              } else if (userContact.equals(sessionUserId)) {
                success = true;
              } else
                success = false;
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in chkPresentRoleIsWaitingApp : ", e);
    }
    return success;
  }

  /**
   * 
   * @param delegationDate
   * @param strRoleId
   * @param documentType
   * @return
   */
  public static String getDelegationObj(Date delegationDate, String strRoleId,
      String documentType) {
    StringBuffer query = null;
    Query delQuery = null;
    String delegationLineId = null;
    try {
      query = new StringBuffer();
      query.append("select dll.id from Eut_Docapp_Delegateln dll "
          + "      join dll.eUTDocappDelegate dl "
          + "      where dl.fromDate <=:currentDate and dl.date >=:currentDate and dll.documentType=:docType and dll.role.id=:currentRoleId and dl.processed='Y' ");
      delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delQuery.setParameter("currentDate", delegationDate);
      delQuery.setParameter("currentDate", delegationDate);
      delQuery.setParameter("docType", documentType);
      delQuery.setParameter("currentRoleId", strRoleId);
      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          delegationLineId = (String) delQuery.list().get(0);
        }
      }

    } catch (Exception e) {
      log4j.error("Error in getDelegationObj() ", e);
    }
    return delegationLineId;
  }

  /**
   * 
   * @param proposal
   * @param sessionRoleId
   * @param sessionUserId
   * @return
   */
  public static boolean chkPresentRoleIsWaitingAppforAddingFile(String strTab, String recordId,
      String sessionRoleId, String sessionUserId) {
    boolean success = false;
    String roleId = null;
    EutNextRoleLine eutNextRole = null;
    String userContact = null;
    boolean allowDelegation = false;
    Date currentDate = new Date();
    DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
    String delegationLineId = null;
    EutDocappDelegateln delegationLine = null;
    DocumentTypeE DocumentType = null;
    String nextRoleId = null;
    EscmProposalMgmt proposal = null;
    Requisition requisition = null;
    Order ord = null;
    try {
      OBContext.setAdminMode();

      if (strTab.equals(Constants.PROPOSAL_MANAGEMENT_TAB)) {
        proposal = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
      }
      if (strTab.equals(Constants.PURCHASE_ORDER_TAB)) {
        ord = OBDal.getInstance().get(Order.class, recordId);
      }
      if (strTab.equals(Constants.PURCHASE_REQUISITION_TAB)) {
        requisition = OBDal.getInstance().get(Requisition.class, recordId);
      }

      if (proposal != null) {
        if (proposal.getProposalType().equals("DR")) {
          DocumentType = DocumentTypeE.DIRECT_PROPOSAL;
        } else if (proposal.getProposalType().equals("LD")
            || proposal.getProposalType().equals("TR")) {
          DocumentType = DocumentTypeE.PROPOSAL_LIMITED_TENDER;
        }
        nextRoleId = proposal.getEUTNextRole().getId();
      } else if (ord != null) {
        DocumentType = DocumentTypeE.PURCHASE_ORDER;
        nextRoleId = ord.getEutNextRole().getId();
      } else if (requisition != null) {
        if (requisition.getEscmProcesstype().equals("DP")) {
          DocumentType = DocumentTypeE.PR_DIRECT;
        } else if (requisition.getEscmProcesstype().equals("LB")
            || requisition.getEscmProcesstype().equals("PB")) {
          DocumentType = DocumentTypeE.PR_LIMITED_TENDER;
        }
        nextRoleId = requisition.getEutNextRole().getId();
      }
      if (nextRoleId != null) {
        // nextRole check
        OBQuery<EutNextRoleLine> line = OBDal.getInstance().createQuery(EutNextRoleLine.class,
            " as e where e.eUTNextRole.id = :NextRoleID order by creationDate desc");
        line.setNamedParameter("NextRoleID", nextRoleId);
        line.setMaxResult(1);
        if (line != null && line.list().size() > 0) {
          eutNextRole = line.list().get(0);
          roleId = eutNextRole.getRole().getId();
        }
      }

      if (roleId != null && roleId.equals(sessionRoleId)) {
        success = true;
      }
      // delegation
      else {
        allowDelegation = delagationDao.checkDelegation(currentDate, sessionRoleId,
            DocumentType.getDocumentTypeCode());
        if (allowDelegation) {
          delegationLineId = getDelegationObj(currentDate, sessionRoleId,
              DocumentType.getDocumentTypeCode());
          if (delegationLineId != null)
            delegationLine = OBDal.getInstance().get(EutDocappDelegateln.class, delegationLineId);
          if (delegationLine != null && delegationLine.getRole().getId().equals(sessionRoleId)) {
            success = true;
          }
        }
      }

    } catch (

    Exception e) {
      OBContext.restorePreviousMode();
      log4j.error("Exception in chkPresentRoleIsWaitingApp_FileAdd : ", e);
    }
    return success;
  }

  /**
   * 
   * @param proposal
   * @param sessionRoleId
   * @param sessionUserId
   * @return
   */
  public static boolean chkPresentRoleIsWaitingAppForRemoveAll(String tabId, String recordId,
      String sessionRoleId, String sessionUserId, List<Attachment> fileList) {
    boolean success = false;
    String roleId = null;
    EutNextRoleLine eutNextRole = null;
    String userContact = null;
    boolean allowDelegation = false;
    Date currentDate = new Date();
    DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
    String delegationLineId = null;
    EutDocappDelegateln delegationLine = null;
    DocumentTypeE DocumentType = null;
    String forward_RmiId = null;
    EscmProposalMgmt proposal = null;
    Order ord = null;
    Requisition requisition = null;
    String nextRoleID = null;
    try {

      if (tabId.equals(Constants.PROPOSAL_MANAGEMENT_TAB)) {
        proposal = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
      }
      if (tabId.equals(Constants.PURCHASE_ORDER_TAB)) {
        ord = OBDal.getInstance().get(Order.class, recordId);
      }
      if (tabId.equals(Constants.PURCHASE_REQUISITION_TAB)) {
        requisition = OBDal.getInstance().get(Requisition.class, recordId);
      }
      if (proposal != null) {
        if (proposal.getProposalType().equals("DR")) {
          DocumentType = DocumentTypeE.DIRECT_PROPOSAL;
        } else if (proposal.getProposalType().equals("LD")
            || proposal.getProposalType().equals("TR")) {
          DocumentType = DocumentTypeE.PROPOSAL_LIMITED_TENDER;
        }
        nextRoleID = proposal.getEUTNextRole().getId();
      } else if (ord != null) {
        DocumentType = DocumentTypeE.PURCHASE_ORDER;
        nextRoleID = ord.getEutNextRole().getId();
      } else if (requisition != null) {
        if (requisition.getEscmProcesstype().equals("DP")) {
          DocumentType = DocumentTypeE.PR_DIRECT;
        } else if (requisition.getEscmProcesstype().equals("LB")
            || requisition.getEscmProcesstype().equals("PB")) {
          DocumentType = DocumentTypeE.PR_LIMITED_TENDER;
        }
        nextRoleID = requisition.getEutNextRole().getId();
      }

      // nextRole check
      if (nextRoleID != null) {
        OBQuery<EutNextRoleLine> line = OBDal.getInstance().createQuery(EutNextRoleLine.class,
            " as e where e.eUTNextRole.id = :NextRoleID order by creationDate desc");
        line.setNamedParameter("NextRoleID", proposal.getEUTNextRole().getId());
        line.setMaxResult(1);
        if (line != null && line.list().size() > 0) {
          eutNextRole = line.list().get(0);
          roleId = eutNextRole.getRole().getId();
        }
      }

      if (roleId != null && roleId.equals(sessionRoleId)) {
        if (fileList.size() > 0) {
          for (Attachment attachment : fileList) {
            Attachment attachFile = OBDal.getInstance().get(Attachment.class, attachment.getId());
            if (attachFile != null && attachFile.getEutSourceid() == null
                && attachFile.getEutVersionNo() == null) {
              userContact = attachFile.getCreatedBy().getId();
              if (userContact.equals(sessionUserId)) {
                success = true;
              }
            }
            if (attachFile != null && attachFile.getEutSourceid() != null
                && attachFile.getEutVersionNo() != null) {
              if (roleId != null && roleId.equals(sessionRoleId) && eutNextRole != null
                  && (eutNextRole.getEUTReqmoreinfo() != null
                      || eutNextRole.getEUTForwardReqmoreinfo() != null)) {
                if (eutNextRole.getEUTReqmoreinfo() != null) {
                  forward_RmiId = eutNextRole.getEUTReqmoreinfo().getId();
                } else if (eutNextRole.getEUTForwardReqmoreinfo() != null) {
                  forward_RmiId = eutNextRole.getEUTForwardReqmoreinfo().getId();
                }
                EutForwardReqMoreInfo requestMoreInfo = OBDal.getInstance()
                    .get(EutForwardReqMoreInfo.class, forward_RmiId);
                if (requestMoreInfo != null
                    && requestMoreInfo.getRECRole().getId().equals(sessionRoleId)) {
                  if (attachFile != null) {
                    userContact = attachFile.getCreatedBy().getId();
                    if (userContact.equals(sessionUserId)) {
                      success = true;
                    }
                  }

                }
              }
            } else {
              return false;
            }
          }
        }
      }
      // delegation
      else {
        allowDelegation = delagationDao.checkDelegation(currentDate, sessionRoleId,
            DocumentType.getDocumentTypeCode());
        if (allowDelegation) {
          delegationLineId = getDelegationObj(currentDate, sessionRoleId,
              DocumentType.getDocumentTypeCode());
          if (delegationLineId != null)
            delegationLine = OBDal.getInstance().get(EutDocappDelegateln.class, delegationLineId);
          if (delegationLine != null && delegationLine.getRole().getId().equals(sessionRoleId)) {
            if (fileList.size() > 0) {
              for (Attachment attachment : fileList) {
                Attachment file = OBDal.getInstance().get(Attachment.class, attachment.getId());
                if (file != null) {
                  userContact = file.getCreatedBy().getId();
                  if (userContact
                      .equals(delegationLine.getEUTDocappDelegate().getUserContact().getId())) {
                    success = true;
                  } else if (userContact.equals(sessionUserId)) {
                    success = true;
                  } else
                    success = false;
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in chkPresentRoleIsWaitingAppForRemoveAll : ", e);
    }
    return success;
  }

  public static List<Preference> getPreferences(String property, boolean isListProperty,
      String client, String org, String user, String role, String window, boolean exactMatch,
      boolean checkWindow, boolean activeFilterEnabled) throws Exception {
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
        hql.append(" or p.visibleAtRole is null");
      }

      if (window != null) {
        hql.append(" and p.window.id = ? ");
        parameters.add(window);
      } else {
        hql.append(" and p.window is null");
      }
    } else {
      if (client != null) {
        hql.append(" ((p.visibleAtClient.id = ? or ");
        parameters.add(client);
      } else {
        hql.append(" (");
      }
      hql.append(" coalesce(p.visibleAtClient, '0')='0') ");

      if (role != null) {
        hql.append(" and   (p.visibleAtRole.id = ? or ");
        parameters.add(role);
      } else {
        hql.append(" or (");
      }
      hql.append("        p.visibleAtRole is null) ");

      if (org == null) {
        hql.append("     and (coalesce(p.visibleAtOrganization, '0')='0')");
      }

      if (user != null) {
        hql.append("  and (p.userContact.id = ? or ");
        parameters.add(user);
      } else {
        hql.append(" or (");
      }
      hql.append("         p.userContact is null)) ");
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

  /**
   * 
   * @param proposal
   * @param sessionRoleId
   * @param attachmentId
   * @param sessionUserId
   * @return
   */
  public static boolean chkAttachmentFromOldVersion(String attachmentId) {
    boolean success = true;

    try {
      // check that attachment has copied from old version
      Attachment file = OBDal.getInstance().get(Attachment.class, attachmentId);
      if (file != null && file.getEutSourceid() != null && file.getEutVersionNo() != null) {
        success = false;
      }

    } catch (Exception e) {
      log4j.error("Exception in chkAttachmentFromOldVersion : ", e);
    }
    return success;
  }

  /**
   * 
   * @param fileList
   * @return
   */
  public static boolean chkAttachmentFromOldVersionUsingRemoveAll(List<Attachment> fileList) {
    boolean success = true;

    try {
      // check that attachment has copied from old version
      if (fileList.size() > 0) {
        for (Attachment attachment : fileList) {
          Attachment file = OBDal.getInstance().get(Attachment.class, attachment.getId());
          if (file != null && file.getEutSourceid() != null && file.getEutVersionNo() != null) {
            success = false;

          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in chkAttachmentFromOldVersion : ", e);
    }
    return success;
  }

  public static JSONObject getRecordDetails(String tabId, String recordId) {
    EscmProposalMgmt proposalMgmt = null;
    Order ord = null;
    Requisition requisition = null;

    JSONObject result = new JSONObject();
    try {
      if (tabId.equals(Constants.PROPOSAL_MANAGEMENT_TAB)) {
        proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
        if (proposalMgmt != null) {
          result.put("Object", proposalMgmt);
          result.put("window", Constants.PROPOSAL_MANAGEMENT_W);
          result.put("clientId", proposalMgmt.getClient().getId());
          result.put("appStatus", proposalMgmt.getProposalappstatus());
        }
      }
      if (tabId.equals(Constants.PURCHASE_ORDER_TAB)) {
        ord = OBDal.getInstance().get(Order.class, recordId);
        if (ord != null) {
          result.put("Object", proposalMgmt);
          result.put("window", Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W);
          result.put("clientId", ord.getClient().getId());
          result.put("appStatus", ord.getEscmAppstatus());
        }
      }
      if (tabId.equals(Constants.PURCHASE_REQUISITION_TAB)) {
        requisition = OBDal.getInstance().get(Requisition.class, recordId);
        if (requisition != null) {
          result.put("Object", requisition);
          result.put("window", Constants.PURCHASE_REQUISITION_W);
          result.put("clientId", requisition.getClient().getId());
          result.put("appStatus", requisition.getEscmDocStatus());
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in chkAttachmentFromOldVersion : ", e);
    }
    return result;
  }
}
