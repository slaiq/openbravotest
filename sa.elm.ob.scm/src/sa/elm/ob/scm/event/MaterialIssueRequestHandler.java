package sa.elm.ob.scm.event;

import java.util.List;

import javax.enterprise.event.Observes;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.domain.Preference;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopalakrishnan on 10/03/2017
 * 
 */
public class MaterialIssueRequestHandler extends EntityPersistenceEventObserver {
  /**
   * This Class is responsible for business events in Escm_Material_Request Table
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(MaterialIssueRequest.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      // Long code = Long.valueOf("0001");
      String sequence = "";
      boolean sequenceexists = false;
      MaterialIssueRequest objRequest = (MaterialIssueRequest) event.getTargetInstance();
      // final Property DocNo = entities[0].getProperty(MaterialIssueRequest.PROPERTY_DOCNO);
      final Property SpecRef = entities[0].getProperty(MaterialIssueRequest.PROPERTY_DOCUMENTNO);
      final Property requesterRole = entities[0].getProperty(MaterialIssueRequest.PROPERTY_ROLE);
      HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      // get role
      event.setCurrentState(requesterRole, OBContext.getOBContext().getRole());
      UserRoles objUserRole = objRequest.getUpdatedBy().getADUserRolesList().get(0);
      // get user
      // String userId = objRequest.getCreatedBy().getId();
      Boolean isSiteMir = objRequest.isSiteissuereq();
      String preferenceValue = null;
      if (objUserRole != null) {
        Role objRole = objUserRole.getRole();
        log.debug("objRole:" + objRole.getName());
        // check role is warehouse Keeper
        /*
         * OBQuery<Preference> preQuery = OBDal.getInstance().createQuery(Preference.class,
         * "as e where e.property='ESCM_WarehouseKeeper' and e.searchKey='Y' and (e.userContact is null or e.userContact.id='"
         * + userId + "') and e.visibleAtRole.id='" + OBContext.getOBContext().getRole().getId() +
         * "' and e.eutForwardReqmoreinfo.id is null ");
         * 
         * if (preQuery.list().size() > 0 && objRequest.getWarehouse() == null) { throw new
         * OBException(OBMessageUtils.messageBD("ESCM_WarehouseEmpty")); }
         */

        if (!isSiteMir) {
          try {
            preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
                OBContext.getOBContext().getCurrentClient().getId(),
                OBContext.getOBContext().getCurrentOrganization().getId(),
                OBContext.getOBContext().getUser().getId(),
                OBContext.getOBContext().getRole().getId(), Constants.MATERIAL_ISSUE_REQUEST_W);
          } catch (PropertyException e) {
            // Do not catch anything.
          }

          // check preference is given by forward then restrict to give access while submit
          if (objRequest.getAlertStatus().equals("DR") && preferenceValue != null
              && preferenceValue.equals("Y")) {
            List<Preference> prefs = forwardDao.getPreferences("ESCM_WarehouseKeeper", true,
                vars.getClient(), objRequest.getOrganization().getId(), vars.getUser(),
                vars.getRole(), Constants.MATERIAL_ISSUE_REQUEST_W, false, true, true);
            for (Preference preference : prefs) {
              if (preference.getEutForwardReqmoreinfo() != null) {
                preferenceValue = "N";
              }
            }
          }

          if (preferenceValue != null && preferenceValue.equals("Y")
              && objRequest.getWarehouse() == null) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_WarehouseEmpty"));
          }

        }
      }

      /*
       * // assign doc no // 1438H0001 DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
       * Date now = new Date(); Date todaydate = null; try { todaydate =
       * dateFormat.parse(dateFormat.format(now)); } catch (ParseException e) { // TODO
       * Auto-generated catch block e.printStackTrace(); } SimpleDateFormat dateYearFormat = new
       * SimpleDateFormat("yyyy-MM-dd"); String hijiridate =
       * convertTohijriDate(dateYearFormat.format(todaydate)); int year =
       * Integer.parseInt(hijiridate.split("-")[2]); log.debug("Year:" + year);
       * OBQuery<MaterialIssueRequest> objIrQry = OBDal.getInstance().createQuery(
       * MaterialIssueRequest.class, "as e where e.organization.id='" +
       * objRequest.getOrganization().getId() + "' order by e.creationDate desc");
       * objIrQry.setMaxResult(1); if (objIrQry.list().size() > 0) { code =
       * objIrQry.list().get(0).getDocno(); if (code != null) { newCode =
       * String.valueOf(year).concat("H").concat(String.format("%04d", code + 1));
       * event.setCurrentState(DocNo, code + 1); } else { newCode = String.valueOf(year).concat("H")
       * .concat(String.format("%04d", Long.valueOf("0001"))); event.setCurrentState(DocNo,
       * Long.valueOf("0001")); } } else { newCode =
       * String.valueOf(year).concat("H").concat(String.format("%04d", code));
       * event.setCurrentState(DocNo, code); }
       */
      // set new Spec No
      sequence = Utility.getTransactionSequence(objRequest.getOrganization().getId(), "MIR");
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        sequenceexists = Utility.chkTransactionSequence(objRequest.getOrganization().getId(), "MIR",
            sequence);
        if (!sequenceexists) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
        }
        event.setCurrentState(SpecRef, sequence);
      }

      if (objRequest.getBeneficiaryType().equals("D") || objRequest.getBeneficiaryType().equals("E")
          || objRequest.getBeneficiaryType().equals("S")) {
        if (objRequest.getBeneficiaryIDName() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_BenMandatory"));
        }
      }
      if (objRequest.getBeneficiaryType().equals("SA") && objRequest.getSalesDesc() == null) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Beneficiary_Sales_Empty"));
      }
      /*
       * try { String preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
       * OBContext.getOBContext().getCurrentClient().getId(),
       * OBContext.getOBContext().getCurrentOrganization().getId(),
       * OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
       * Constants.MATERIAL_ISSUE_REQUEST_W);
       */

      if (!isSiteMir) {
        if (preferenceValue != null && preferenceValue.equals("Y")) {
          if ((objRequest.getWarehouse() != null
              && objRequest.getWarehouse().getEscmWarehouseType().equals("RTW"))
              && (objRequest.getEscmIssuereason() == null
                  || objRequest.getEscmIssuereason().equals(""))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_IssueReason"));
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while creating MaterialIssueRequest : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while creating MaterialIssueRequest : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      MaterialIssueRequest objRequest = (MaterialIssueRequest) event.getTargetInstance();

      final Property status = entities[0].getProperty(MaterialIssueRequest.PROPERTY_ALERTSTATUS);
      // final Property specNo = entities[0].getProperty(MaterialIssueRequest.PROPERTY_SPECNO);
      final Property rmiId = entities[0].getProperty(MaterialIssueRequest.PROPERTY_EUTREQMOREINFO);
      final Property forwardId = entities[0].getProperty(MaterialIssueRequest.PROPERTY_EUTFORWARD);
      final Property docaction = entities[0].getProperty(MaterialIssueRequest.PROPERTY_ESCMACTION);
      final Property smirDocaction = entities[0]
          .getProperty(MaterialIssueRequest.PROPERTY_ESCMSMIRACTION);
      final Property nextRole = entities[0].getProperty(MaterialIssueRequest.PROPERTY_EUTNEXTROLE);
      final Property benType = entities[0]
          .getProperty(MaterialIssueRequest.PROPERTY_BENEFICIARYTYPE);
      HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      String fromRole = null;
      // User userObj = null;
      Boolean isSiteMir = objRequest.isSiteissuereq();
      String preferenceValue = null;
      // if ((objRequest.isProcessNow()
      // && event.getCurrentState(specNo) == event.getPreviousState(specNo))) {
      // throw new OBException(OBMessageUtils.messageBD("Escm_Processing"));
      // }

      // get user
      String userId = objRequest.getUpdatedBy().getId();
      // forward / delegated - get fromUser and fromRole
      String fromUser = userId;
      fromRole = OBContext.getOBContext().getRole().getId();

      // get role
      if (!event.getCurrentState(status).toString().equals("DR")
          && !((event.getPreviousState(forwardId) != null
              && event.getCurrentState(forwardId) == null)
              || (event.getPreviousState(rmiId) != null && event.getCurrentState(rmiId) == null)
              || (event.getCurrentState(forwardId) != null
                  && event.getPreviousState(forwardId) == null)
              || (event.getCurrentState(rmiId) != null && event.getPreviousState(rmiId) == null)
              || (event.getCurrentState(nextRole) != null
                  && !event.getCurrentState(nextRole).equals(event.getPreviousState(nextRole)))
              || (!event.getCurrentState(docaction).equals(event.getPreviousState(docaction)))
              || (!event.getCurrentState(smirDocaction)
                  .equals(event.getPreviousState(smirDocaction))))) {
        // UserRoles objUserRole = userObj.getADUserRolesList().get(0);
        if (fromRole != null) {
          // Role objRole = objUserRole.getRole();
          // checking warehouse keeper preference
          if (!isSiteMir) {
            try {
              preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
                  OBContext.getOBContext().getCurrentClient().getId(),
                  OBContext.getOBContext().getCurrentOrganization().getId(), fromUser, fromRole,
                  Constants.MATERIAL_ISSUE_REQUEST_W);
            } catch (PropertyException e) {
              // Do not catch anything.
            }

            if (preferenceValue != null && preferenceValue.equals("Y")
                && objRequest.getWarehouse() == null) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_WarehouseEmpty"));
            }
          }
        }
      }
      if (event.getCurrentState(benType).toString().equals("D")
          || event.getCurrentState(benType).toString().equals("E")
          || event.getCurrentState(benType).toString().equals("S")) {
        if (objRequest.getBeneficiaryIDName() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_BenMandatory"));
        }
      }

      if (objRequest.getBeneficiaryType().equals("SA") && objRequest.getSalesDesc() == null) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Beneficiary_Sales_Empty"));
      }

      // check preference is given by forward then restrict to give access while submit
      if (objRequest.getAlertStatus().equals("DR") && preferenceValue != null
          && preferenceValue.equals("Y")) {
        List<Preference> prefs = forwardDao.getPreferences("ESCM_WarehouseKeeper", true,
            vars.getClient(), objRequest.getOrganization().getId(), vars.getUser(), vars.getRole(),
            Constants.MATERIAL_ISSUE_REQUEST_W, false, true, true);
        for (Preference preference : prefs) {
          if (preference.getEutForwardReqmoreinfo() != null) {
            preferenceValue = "N";
          }
        }
      }

      if (!isSiteMir) {
        if (preferenceValue != null && preferenceValue.equals("Y")) {
          if ((!objRequest.isSiteissuereq() && objRequest.getWarehouse() != null
              && objRequest.getWarehouse().getEscmWarehouseType().equals("RTW"))
              && (objRequest.getEscmIssuereason() == null
                  || objRequest.getEscmIssuereason().equals(""))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_MIR_IssueReason"));
          }
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating MaterialIssueRequest: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating MaterialIssueRequest: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      MaterialIssueRequest objRequest = (MaterialIssueRequest) event.getTargetInstance();
      OBQuery<MaterialIssueRequestHistory> appQuery = OBDal.getInstance().createQuery(
          MaterialIssueRequestHistory.class,
          "as e where e.escmMaterialRequest.id=:mirID order by creationDate desc");
      appQuery.setNamedParameter("mirID", objRequest.getId());
      appQuery.setMaxResult(1);
      if (appQuery.list().size() > 0) {
        MaterialIssueRequestHistory objLastLine = appQuery.list().get(0);
        if (objLastLine.getRequestreqaction().equals("REJ")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Rejected"));
        }
      }
      if (objRequest.getSpecNo() != null && StringUtils.isNotEmpty(objRequest.getSpecNo())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Spec_Generated(Error)"));
      }
      if (objRequest.getAlertStatus().equals("ESCM_IP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_InProgress"));
      }
      if (objRequest.getAlertStatus().equals("ESCM_TR")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Approved"));
      }
      if (objRequest.getEscmMaterialrequestHistList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Submitted"));
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting IssueRequest : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting IssueRequest : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public String convertTohijriDate(String gregDate) {
    String hijriDate = "";
    try {
      SQLQuery gradeQuery = OBDal.getInstance().getSession()
          .createSQLQuery("select eut_convert_to_hijri(to_char(to_timestamp('" + gregDate
              + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      if (gradeQuery.list().size() > 0) {
        Object row = (Object) gradeQuery.list().get(0);
        hijriDate = (String) row;
        log.debug("ConvertedDate:" + (String) row);
      }
    }

    catch (final Exception e) {
      log.error("Exception in convertTohijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }
}
