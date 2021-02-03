package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMPurchaseReqAppHist;
import sa.elm.ob.scm.event.dao.RequisitionEventDAO;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Preferences;

/**
 * 
 * @author Gopalakrishnan on 15/02/2017
 * 
 */
public class RequisitionEvent extends EntityPersistenceEventObserver {
  /**
   * This Class was responsible for business events in M_Requisition Table
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(Requisition.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());
  ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
  final String REQUISTION_WINDOW_ID = "800092";

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      List<RequisitionLine> reqLineList = null;
      Requisition objRequisition = (Requisition) event.getTargetInstance();
      OBQuery<ESCMPurchaseReqAppHist> appQuery = OBDal.getInstance().createQuery(
          ESCMPurchaseReqAppHist.class,
          "as e where e.requisition.id=:reqID order by creationDate desc");
      appQuery.setNamedParameter("reqID", objRequisition.getId());
      appQuery.setMaxResult(1);
      if (appQuery.list().size() > 0) {
        ESCMPurchaseReqAppHist objLastLine = appQuery.list().get(0);
        if (objLastLine.getPurchasereqaction().equals("REJ")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Requistion_Rejected"));
        }
      }

      // if purchase requisition contain line then should not allow to header
      OBQuery<RequisitionLine> reqLine = OBDal.getInstance().createQuery(RequisitionLine.class,
          "as e where e.requisition.id =:reqId");
      reqLine.setNamedParameter("reqId", objRequisition.getId());
      reqLineList = reqLine.list();
      if (objRequisition.getEscmDocStatus().equals("DR") && reqLineList.size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_PurReq_CantDelHead"));
      }

      if (objRequisition.getEscmDocStatus().equals("ESCM_IP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_InProgress"));
      }
      if (objRequisition.getEscmDocStatus().equals("ESCM_AP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_Approved"));
      }
      if (objRequisition.getESCMPurchaseReqAppHistList().size() > 0
          && !objRequisition.getEscmDocStatus().equals("DR")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_Submitted"));
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting Requisition : " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting Requisition : " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Requisition req = (Requisition) event.getTargetInstance();
      final Property PRRequiredDoc = entities[0].getProperty(Requisition.PROPERTY_ESCMREQUIREDOCS);
      final Property specDept = entities[0].getProperty(Requisition.PROPERTY_ESCMSPECIALIZEDDEPT);
      final Property encumbrance = entities[0].getProperty(Requisition.PROPERTY_EFINBUDGETMANENCUM);
      final Property uniqueCode = entities[0].getProperty(Requisition.PROPERTY_EFINUNIQUECODE);
      final Property encumMethod = entities[0].getProperty(Requisition.PROPERTY_EFINENCUMMETHOD);

      String preferenceValue = "N";

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();

      // Check Budget Controller Preference

      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true, clientId,
            req.getOrganization().getId(), userId, roldId, REQUISTION_WINDOW_ID, "N");
        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;

      } catch (PropertyException e) {
        preferenceValue = "N";
        // log.error("Exception in getting budget controller :", e);
      }

      if (!preferenceValue.equals("Y") && req.getEutForward() != null) {// check for
                                                                        // temporary
        // preference
        String requester_user_id = req.getEutForward().getUserContact().getId();
        String requester_role_id = req.getEutForward().getRole().getId();
        preferenceValue = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", roldId, userId, clientId, req.getOrganization().getId(),
            REQUISTION_WINDOW_ID, requester_user_id, requester_role_id);
      }

      // if Budget Controller Preference not enabled then the user will not able to change the
      // values of
      // Budget related fields
      if (!preferenceValue.equals("Y")) {
        if (event.getCurrentState(encumbrance) != null || event.getCurrentState(uniqueCode) != null
            || req.isEfinSkipencumbrance()) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      // update Required Documents from PR Required Documents based on process type
      if (req.getEscmProcesstype() != null) {
        if (req.getEscmProcesstype().equals("PB") || req.getEscmProcesstype().equals("LB")) {
          String PRReqDoc = RequisitionEventDAO.updatePRReqDoc(req);
          event.setCurrentState(PRRequiredDoc, PRReqDoc);
          event.setCurrentState(specDept, null);
        } else {
          event.setCurrentState(PRRequiredDoc, null);
        }
      }

      // for Process type = DirectPO, Specialized Dept is mandatory
      if (req.getEscmProcesstype() != null) {
        if (req.getEscmProcesstype().equals("DP")) {
          if (StringUtils.isEmpty(req.getEscmSpecializeddept())) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Requistion_SpclDept"));
          }
        }
      }

      // for encumbrance method - manual, encumbrance is mandatory

      if (req.getEfinEncumMethod() != null && req.getEfinEncumMethod().equals("M")
          && req.getEfinBudgetManencum() == null) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_Encum_Mandatory"));
      }
      // Handled Mandatory fields for Genral Routine/preventive maintenance PO
      if (req.getEscmContactType() != null) {
        ESCMDefLookupsTypeLn lookupLn = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            req.getEscmContactType().getId());
        if (lookupLn.isMaintenancecontract()) {
          if (req.getEscmMaintenanceProject() == null) {
            System.out.println(req.getEscmMaintenanceProject());
            throw new OBException(OBMessageUtils.messageBD("ESCM_GRPO_Mandatory_Field"));
          }
        }
      }

    } catch (OBException e) {
      log.debug("exception while creating PR Require Documents" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating PR Require Documents" + e);
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
      Requisition objRequisition = (Requisition) event.getTargetInstance();
      final Property PRRequiredDoc = entities[0].getProperty(Requisition.PROPERTY_ESCMREQUIREDOCS);
      final Property specDept = entities[0].getProperty(Requisition.PROPERTY_ESCMSPECIALIZEDDEPT);
      final Property skipEnc = entities[0].getProperty(Requisition.PROPERTY_EFINSKIPENCUMBRANCE);

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();

      final Property encumbrance = entities[0].getProperty(Requisition.PROPERTY_EFINBUDGETMANENCUM);
      final Property uniqueCode = entities[0].getProperty(Requisition.PROPERTY_EFINUNIQUECODE);
      final Property encumMethod = entities[0].getProperty(Requisition.PROPERTY_EFINENCUMMETHOD);
      final Property securedPR = entities[0].getProperty(Requisition.PROPERTY_ESCMISSECURED);

      String preferenceValue = "N";
      Boolean isSecured = objRequisition.isEscmIssecured();

      // Check Budget Controller Preference
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", Boolean.TRUE,
            clientId, objRequisition.getOrganization().getId(), userId, roldId,
            REQUISTION_WINDOW_ID, "N");
        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;
      } catch (PropertyException e) {
        preferenceValue = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      // find out the forward reference record against the PO
      EutForwardReqMoreInfo objForward = objRequisition.getEutForward();
      if (objForward == null) {
        objForward = forwardReqMoreInfoDAO
            .findForwardReferenceAgainstTheRecord(objRequisition.getId(), userId, roldId);
      }
      if (!preferenceValue.equals("Y") && objForward != null) {// check for
        // temporary
        // preference
        String requester_user_id = objForward.getUserContact().getId();
        String requester_role_id = objForward.getRole().getId();
        preferenceValue = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", roldId, userId, clientId,
            objRequisition.getOrganization().getId(), REQUISTION_WINDOW_ID, requester_user_id,
            requester_role_id);

      }

      // if Budget Controller Preference not
      // enabled then the user will not able to change the
      // values of
      // Budget related fields

      // if (!preferenceValue.equals("Y") && ((event.getCurrentState(forwardId) == null
      // && event.getPreviousState(forwardId) == null)
      // || (event.getCurrentState(forwardId) != null && event.getPreviousState(forwardId) != null
      // && event.getCurrentState(forwardId).equals(event.getPreviousState(forwardId)))))

      if (!preferenceValue.equals("Y")) {
        if ((event.getCurrentState(encumbrance) != null
            && !event.getCurrentState(encumbrance).equals(event.getPreviousState(encumbrance)))
            || (event.getCurrentState(encumMethod) != null
                && !event.getCurrentState(encumMethod).equals(event.getPreviousState(encumMethod)))
            || (event.getCurrentState(uniqueCode) != null
                && !event.getCurrentState(uniqueCode).equals(event.getPreviousState(uniqueCode)))
            || (event.getCurrentState(skipEnc) != null
                && !event.getCurrentState(skipEnc).equals(event.getPreviousState(skipEnc)))) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      for (RequisitionLine objLines : objRequisition.getProcurementRequisitionLineList()) {
        objLines.setEscmAccountnoAmt(objRequisition.getEscmAccountno());
      }
      // update Required Documents from PR Required Documents based on process type
      if (objRequisition.getEscmProcesstype().equals("PB")
          || objRequisition.getEscmProcesstype().equals("LB")) {
        String PRReqDoc = RequisitionEventDAO.updatePRReqDoc(objRequisition);
        event.setCurrentState(PRRequiredDoc, PRReqDoc);
        event.setCurrentState(specDept, null);
      } else if (objRequisition.getEscmProcesstype().equals("DP")) {
        event.setCurrentState(PRRequiredDoc, null);
        if (StringUtils.isEmpty(objRequisition.getEscmSpecializeddept())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Requistion_SpclDept"));
        }
      }

      // for Process type = DirectPO, Specialized Dept is mandatory
      /*
       * if (!event.getPreviousState(processType).equals(event.getCurrentState(processType))) { if
       * (objRequisition.getEscmProcesstype().equals("DP")) { if
       * (StringUtils.isEmpty(objRequisition.getEscmSpecializeddept())) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_Requistion_SpclDept")); } } }
       */

      // for encumbrance method - manual, encumbrance is mandatory

      if (objRequisition.getEfinEncumMethod() != null
          && objRequisition.getEfinEncumMethod().equals("M")
          && objRequisition.getEfinBudgetManencum() == null) {
        throw new OBException(OBMessageUtils.messageBD("EFIN_Encum_Mandatory"));
      }

      // if skip encumbrance then make empty as lines uniquecode.
      if (event.getPreviousState(skipEnc) != event.getCurrentState(skipEnc)
          && objRequisition.isEfinSkipencumbrance()) {
        if (objRequisition.getProcurementRequisitionLineList() != null
            && objRequisition.getProcurementRequisitionLineList().size() > 0) {
          for (RequisitionLine objLines : objRequisition.getProcurementRequisitionLineList()) {
            objLines.setEfinUniquecodename("");
            objLines.setEfinCValidcombination(null);
          }
        }
      }
      // Handled Mandatory fields for Genral Routine/preventive maintenance PO
      if (objRequisition.getEscmContactType() != null) {
        ESCMDefLookupsTypeLn lookupLn = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            objRequisition.getEscmContactType().getId());
        if (lookupLn.isMaintenancecontract()) {
          if (objRequisition.getEscmMaintenanceProject() == null) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_GRPO_Mandatory_Field"));
          }
        }
      }

      // Secure pr block
      // If secured pr flag is updated, we have to copy value from original unit price to unit price
      if (event.getCurrentState(securedPR) != null
          && event.getCurrentState(securedPR) != event.getPreviousState(securedPR)) {
        for (RequisitionLine prLines : objRequisition.getProcurementRequisitionLineList()) {
          if (isSecured) {
            String originalunitpricestr = prLines.getUnitPrice().toString();
            prLines.setUnitPrice(BigDecimal.ZERO);
            prLines.setEscmUnitprice(
                new String(java.util.Base64.getEncoder().encode(originalunitpricestr.getBytes())));
            prLines.setLineNetAmount(BigDecimal.ZERO);
            OBDal.getInstance().save(prLines);
          } else {
            String originalunitpricestr = new String(
                java.util.Base64.getDecoder().decode(prLines.getEscmUnitprice()));
            prLines.setUnitPrice(new BigDecimal(originalunitpricestr));
            prLines.setEscmUnitprice(null);
            prLines.setLineNetAmount(prLines.getQuantity().multiply(prLines.getUnitPrice()));
            OBDal.getInstance().save(prLines);
          }
        }

      }

    } catch (OBException e) {
      log.error(" Exception while updating Requisition  : " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while updating Requisition  : " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
