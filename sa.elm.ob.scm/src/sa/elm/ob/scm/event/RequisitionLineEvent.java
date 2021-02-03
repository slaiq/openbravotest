package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.util.Calendar;

import javax.enterprise.event.Observes;

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
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Constants;

/**
 * 
 * @author Gopalakrishnan on 16/02/2017
 * 
 */
public class RequisitionLineEvent extends EntityPersistenceEventObserver {
  /**
   * This Class was responsible for business events in M_Requisition_Line Table
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(RequisitionLine.ENTITY_NAME) };

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
      RequisitionLine objRequisitionLine = (RequisitionLine) event.getTargetInstance();
      String reqlineId = objRequisitionLine.getId();
      String reqId = objRequisitionLine.getRequisition().getId();
      final String userId = OBContext.getOBContext().getUser().getId();
      final String roleId = OBContext.getOBContext().getRole().getId();
      final String clientId = OBContext.getOBContext().getCurrentClient().getId();
      Requisition objRequisition = OBDal.getInstance().get(Requisition.class, reqId);
      log.debug("objRequisition:" + objRequisition);
      Boolean ispreference = false;
      /*
       * Preferences.existsPreference("ESCM_LineManager", true, null, null, null,
       * OBContext.getOBContext().getRole().getId(), null); log.debug("isprefer:" + ispreference);
       */
      String preferenceValue = "", preferenceValueOfIC = "";
      try {
        if (objRequisition != null) {
          preferenceValue = Preferences.getPreferenceValue("ESCM_LineManager", true, clientId,
              objRequisition.getOrganization().getId(), userId, roleId, "800092");
        }
        if (preferenceValue != null && preferenceValue.equals("Y"))
          ispreference = true;

      } catch (PropertyException e) {
        e.printStackTrace();
      }
      try {
        if (objRequisition != null) {
          preferenceValueOfIC = Preferences.getPreferenceValue("ESCM_Inventory_Control", true,
              clientId, objRequisition.getOrganization().getId(), userId, roleId, "800092");
        }
        if (preferenceValueOfIC != null && preferenceValueOfIC.equals("Y"))
          ispreference = true;
      } catch (PropertyException e) {
      }
      if (objRequisition != null) {
        OBQuery<RequisitionLine> linequery = OBDal.getInstance().createQuery(RequisitionLine.class,
            " as e where e.requisition.id=:reqID and e.id <>:reqLnID");
        linequery.setNamedParameter("reqID", reqId);
        linequery.setNamedParameter("reqLnID", reqlineId);

        if (linequery.list().size() == 0 && objRequisition != null
            && objRequisition.getEscmDocStatus().equals("ESCM_IP")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PurReq_AtleastOneRow"));
        }
      }

      if (objRequisition != null && objRequisition.getEscmDocStatus().equals("ESCM_IP")
          && !ispreference) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_InProgress"));
      }
      if (objRequisition != null && objRequisition.getEscmDocStatus().equals("ESCM_AP")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Requisition_Approved"));
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting RequisitionLine  : " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting RequisitionLine  : " + e);
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
      RequisitionLine objRequisitionLine = (RequisitionLine) event.getTargetInstance();
      String allowPastDate = "";
      final Property qty = entities[0].getProperty(RequisitionLine.PROPERTY_QUANTITY);
      final Property needbydate = entities[0].getProperty(RequisitionLine.PROPERTY_NEEDBYDATE);
      final Property uniqueCode = entities[0]
          .getProperty(RequisitionLine.PROPERTY_EFINCVALIDCOMBINATION);
      final Property linenetamount = entities[0]
          .getProperty(RequisitionLine.PROPERTY_LINENETAMOUNT);

      final Property originalunitprice = entities[0]
          .getProperty(RequisitionLine.PROPERTY_ESCMUNITPRICE);
      final Property unitprice = entities[0].getProperty(RequisitionLine.PROPERTY_UNITPRICE);

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      Requisition objRequisition = objRequisitionLine.getRequisition();

      String budgetController = "N";
      // Check Budget Controller Preference
      try {
        budgetController = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", true, clientId, objRequisition.getOrganization().getId(), userId,
            roldId, REQUISTION_WINDOW_ID, "N");
        budgetController = (budgetController == null) ? "N" : budgetController;

      } catch (PropertyException e) {
        budgetController = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      if (!budgetController.equals("Y") && objRequisition.getEutForward() != null) {// check for
        // temporary
        // preference
        String requester_user_id = objRequisition.getEutForward().getUserContact().getId();
        String requester_role_id = objRequisition.getEutForward().getRole().getId();
        budgetController = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", roldId, userId, clientId,
            objRequisition.getOrganization().getId(), REQUISTION_WINDOW_ID, requester_user_id,
            requester_role_id);
      }
      // if Budget Controller Preference not enabled then the user will not able to change the
      // values of
      // Budget related fields
      if (!budgetController.equals("Y")) {
        if ((event.getCurrentState(uniqueCode) != null
            && !event.getCurrentState(uniqueCode).equals(event.getPreviousState(uniqueCode)))) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      if (!event.getCurrentState(qty).equals(event.getPreviousState(qty))) {
        if (objRequisitionLine.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
        }

      }
      if (objRequisitionLine.getUnitPrice() != null) {
        if (objRequisitionLine.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Unitprice_Negative"));
        }

        if (event.getCurrentState(unitprice) != event.getPreviousState(unitprice)
            && event.getCurrentState(originalunitprice) == event.getPreviousState(originalunitprice)
            && objRequisitionLine.getRequisition().isEscmIssecured()) {
          String originalunitpricestr = objRequisitionLine.getUnitPrice().toString();
          event.setCurrentState(originalunitprice,
              new String(java.util.Base64.getEncoder().encode(originalunitpricestr.getBytes())));
          event.setCurrentState(unitprice, BigDecimal.ZERO);
          event.setCurrentState(linenetamount, BigDecimal.ZERO);
        }

      }
      if (objRequisitionLine.getDescription() == null) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Desc_Mandatory"));
      }

      // Task no:7516 #19795
      // if (objRequisitionLine.getEscmParentlineno() == null) {
      // OBQuery<RequisitionLine> linequery = OBDal.getInstance().createQuery(RequisitionLine.class,
      // " as e where e.escmParentlineno ='" + objRequisitionLine.getId() + "'");
      // // if (linequery != null && linequery.list().size() != 0) {
      // // if (objRequisitionLine.getProduct() != null
      // // && objRequisitionLine.getProduct().getId() != null) {
      // // throw new OBException(OBMessageUtils.messageBD("ESCM_NoItemCode_Parent"));
      // // }
      // // }
      // }
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);

      // Skip Past Date validation for NeedByDate if SCM_AllowPastDate preference is present
      if (event.getCurrentState(needbydate) != null
          && !event.getCurrentState(needbydate).equals(event.getPreviousState(needbydate))) {
        try {
          allowPastDate = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
              objRequisitionLine.getClient().getId(), objRequisitionLine.getOrganization().getId(),
              OBContext.getOBContext().getUser().getId(),
              OBContext.getOBContext().getRole().getId(), Constants.PURCHASE_REQUISITION_W);
        } catch (PropertyException e) {
          allowPastDate = "N";
        }
        if (allowPastDate.equals("N")) {
          if (objRequisitionLine.getNeedByDate().before(cal.getTime())) {
            throw new OBException(OBMessageUtils.messageBD("Escm_PastDate_Requistion"));
          }
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating RequisitionLine  : " + e, e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while updating RequisitionLine  : " + e, e);
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
      RequisitionLine objRequisitionLine = (RequisitionLine) event.getTargetInstance();
      String allowPastDate = "";
      String budgetController = "N";
      final Property uniqueCode = entities[0]
          .getProperty(RequisitionLine.PROPERTY_EFINCVALIDCOMBINATION);

      final Property originalunitprice = entities[0]
          .getProperty(RequisitionLine.PROPERTY_ESCMUNITPRICE);
      final Property unitprice = entities[0].getProperty(RequisitionLine.PROPERTY_UNITPRICE);
      final Property linenetamount = entities[0]
          .getProperty(RequisitionLine.PROPERTY_LINENETAMOUNT);

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      Requisition objRequisition = objRequisitionLine.getRequisition();

      // Check Budget Controller Preference
      try {
        budgetController = sa.elm.ob.utility.util.Preferences.getPreferenceValue(
            "ESCM_BudgetControl", true, clientId, objRequisition.getOrganization().getId(), userId,
            roldId, REQUISTION_WINDOW_ID, "N");
        budgetController = (budgetController == null) ? "N" : budgetController;

      } catch (PropertyException e) {
        budgetController = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      if (!budgetController.equals("Y") && objRequisition.getEutForward() != null) {// check for
        // temporary
        // preference
        String requester_user_id = objRequisition.getEutForward().getUserContact().getId();
        String requester_role_id = objRequisition.getEutForward().getRole().getId();
        budgetController = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", roldId, userId, clientId,
            objRequisition.getOrganization().getId(), REQUISTION_WINDOW_ID, requester_user_id,
            requester_role_id);
      }
      // if Budget Controller Preference not enabled then the user will not able to change the
      // values of
      // Budget related fields
      if (!budgetController.equals("Y")) {
        if (event.getCurrentState(uniqueCode) != null
            && !objRequisition.getEFINUniqueCode().equals(event.getCurrentState(uniqueCode))) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      if (objRequisitionLine.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
      }
      if (objRequisitionLine.getUnitPrice() != null) {
        if (objRequisitionLine.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Unitprice_Negative"));
        }

        if (objRequisitionLine.getRequisition().isEscmIssecured()) {
          String originalunitpricestr = objRequisitionLine.getUnitPrice().toString();
          event.setCurrentState(originalunitprice,
              new String(java.util.Base64.getEncoder().encode(originalunitpricestr.getBytes())));
          event.setCurrentState(unitprice, BigDecimal.ZERO);
          event.setCurrentState(linenetamount, BigDecimal.ZERO);
        }

      }
      if (objRequisitionLine.getDescription() == null) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Desc_Mandatory"));
      }
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);

      // Past Date validation
      if (objRequisitionLine.getNeedByDate() != null) {
        try {
          allowPastDate = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
              objRequisitionLine.getClient().getId(), objRequisitionLine.getOrganization().getId(),
              OBContext.getOBContext().getUser().getId(),
              OBContext.getOBContext().getRole().getId(), Constants.PURCHASE_REQUISITION_W);
        } catch (PropertyException e) {
          allowPastDate = "N";
        }
        if (allowPastDate.equals("N")) {
          if (objRequisitionLine.getNeedByDate().before(cal.getTime())) {
            throw new OBException(OBMessageUtils.messageBD("Escm_PastDate_Requistion"));
          }
        }
      }

    } catch (OBException e) {
      log.error(" Exception while updating RequisitionLine  : " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while updating RequisitionLine  : " + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}