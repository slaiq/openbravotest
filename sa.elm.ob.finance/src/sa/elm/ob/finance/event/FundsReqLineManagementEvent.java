package sa.elm.ob.finance.event;

import java.math.BigDecimal;
import java.util.List;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;

/**
 * 
 * @author divya on 15-09-2017
 * 
 */
public class FundsReqLineManagementEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EFINFundsReqLine.ENTITY_NAME) };

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
      EFINFundsReqLine fundsLine = (EFINFundsReqLine) event.getTargetInstance();
      final Property frmUniquecodeName = entities[0]
          .getProperty(EFINFundsReqLine.PROPERTY_FROMUNIQUECODENAME);
      final Property toUniquecodeName = entities[0]
          .getProperty(EFINFundsReqLine.PROPERTY_TOUNIQUECODENAME);
      EfinBudgetControlParam controlPrmObj = FundsReqMangementDAO
          .getControlParam(fundsLine.getClient().getId());

      /*
       * JSONObject json = FundsReqLineManagementEventDAO.checkFundsAval(fundsLine); if
       * (json.get("is990Acct").equals("true")) { if (json.get("isFundGreater").equals("true")) { if
       * (json.get("isWarn").equals("false")) { throw new
       * OBException(OBMessageUtils.messageBD("EFIN_FRMAmtCantBeGreatThanBCU")); } else { throw new
       * OBException(OBMessageUtils.messageBD("EFIN_FRMAmtCantBeGreatThanBCU")); } } }
       */
      if (fundsLine.getEfinFundsreq().getEFINFundsReqLineList().size() > 0) {
        if (!fundsLine.getEfinFundsreq().getEFINFundsReqLineList().get(0).getREQType()
            .equals(fundsLine.getREQType())) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_FundsReq_EithDisRel"));
        }
      }

      if (fundsLine.getDistType().equals("DIST")) {
        if (fundsLine.getFromaccount() != null) {
          // check same "from account" exists for same FRM account in previous lines
          Boolean isSameAccountExists = FundsReqMangementDAO.checkSameAccountExistsForSameFRM(
              fundsLine.getFromaccount().getId(), fundsLine.getEfinFundsreq().getId());
          if (isSameAccountExists) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Same_From_Account"));
          }
        }
        if (fundsLine.getFromaccount() == null && fundsLine.getToaccount() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_SelAnyAcct"));
        }
        if (fundsLine.getFromaccount() != null && fundsLine.getToaccount() != null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_CantselBothAcct"));
        }
      } else {
        if (fundsLine.getFromaccount() == null || fundsLine.getToaccount() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_BothAcctMad"));
        }
        if (fundsLine.getFromaccount() != null && fundsLine.getToaccount() != null
            && fundsLine.getFromaccount().equals(fundsLine.getToaccount())) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_SameAcctNotAllow"));
        }
      }
      if (fundsLine.getToaccount() != null) {
        if (fundsLine.getDistType().equals("DIST")) {
          EFINFundsReqLine fromAcctLineobj = FundsReqMangementDAO
              .chkFrmAcctPrestorNot(fundsLine.getEfinFundsreq().getId(), fundsLine.getToaccount());
          if (fromAcctLineobj == null)
            throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_SelFromAccount"));
          if (fromAcctLineobj != null && fromAcctLineobj.getFromaccount() != null
              && fromAcctLineobj.getFromaccount().equals(fundsLine.getToaccount())) {
            throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_AlrdySameAcctInFrom"));
          }
        } else {
          Boolean combMatch = FundsReqMangementDAO.chkFrmAcctEquToAcct(fundsLine.getFromaccount(),
              fundsLine.getToaccount());
          if (!combMatch) {
            throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_SelSameDimeAct"));
          }
        }
      }

      if (fundsLine.getIncrease().compareTo(BigDecimal.ZERO) == 0
          && fundsLine.getDecrease().compareTo(BigDecimal.ZERO) == 0) {
        throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_EntIncOrDecAmt"));
      }
      if (fundsLine.getFromaccount() != null
          && fundsLine.getEfinFundsreq().getTransactionType().equals("ORGR")
          && fundsLine.getEfinFundsreq().getOrgreqFundsType() == null) {
        if (fundsLine.getREQType().equals("DIST") && controlPrmObj.getAgencyHqOrg() != null
            && (!fundsLine.getFromaccount().getOrganization().getId()
                .equals(controlPrmObj.getAgencyHqOrg().getId()))) { // fundsLine.getEfinFundsreq().getTransactionOrg().getId()
                                                                    // fundsLine.getFromaccount().getOrganization().getId()
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_CantDistTranOrg"));
        }
      }

      if (fundsLine.getFromaccount() != null
          && fundsLine.getFromaccount().getEfinUniquecodename() != null)
        event.setCurrentState(frmUniquecodeName,
            fundsLine.getFromaccount().getEfinUniquecodename());

      if (fundsLine.getToaccount() != null
          && fundsLine.getToaccount().getEfinUniquecodename() != null)
        event.setCurrentState(toUniquecodeName, fundsLine.getToaccount().getEfinUniquecodename());
      /*
       * if (fundsLine.getDecrease() != null && fundsLine.getFundsAvailable() != null &&
       * fundsLine.getDecrease().compareTo(fundsLine.getFundsAvailable()) < 0 &&
       * fundsLine.getEfinFundsreq().getTransactionType().equals("BCUR")) {
       * fundsLine.getEfinFundsreq().getClient(); throw new
       * OBException(OBMessageUtils.messageBD("Efin_FundsReq_DecMoreThanFundsAvail")); }
       */
    } catch (

    OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while creating FundsReqLine: " + e);
      throw new OBException(e.getMessage());
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
      EFINFundsReqLine fundsLine = (EFINFundsReqLine) event.getTargetInstance();
      final Property decrease = entities[0].getProperty(EFINFundsReqLine.PROPERTY_DECREASE);
      final Property increase = entities[0].getProperty(EFINFundsReqLine.PROPERTY_INCREASE);
      final Property fromAcct = entities[0].getProperty(EFINFundsReqLine.PROPERTY_FROMACCOUNT);
      final Property toAcct = entities[0].getProperty(EFINFundsReqLine.PROPERTY_TOACCOUNT);
      final Property reqtype = entities[0].getProperty(EFINFundsReqLine.PROPERTY_REQTYPE);
      final Property frmUniquecodeName = entities[0]
          .getProperty(EFINFundsReqLine.PROPERTY_FROMUNIQUECODENAME);
      final Property toUniquecodeName = entities[0]
          .getProperty(EFINFundsReqLine.PROPERTY_TOUNIQUECODENAME);
      // final Property percentage = entities[0].getProperty(EFINFundsReqLine.PROPERTY_PERCENTAGE);

      /*
       * JSONObject json = FundsReqLineManagementEventDAO.checkFundsAval(fundsLine);
       * log.info(json.get("is990Acct") + ", " + json.get("isFundGreater") + "," +
       * json.get("isWarn")); if (json.get("is990Acct").equals("true")) { if
       * (json.get("isFundGreater").equals("true")) { if (json.get("isWarn").equals("false")) {
       * throw new OBException(OBMessageUtils.messageBD("EFIN_FRMAmtCantBeGreatThanBCU")); } else {
       * throw new OBException(OBMessageUtils.messageBD("EFIN_FRMAmtCantBeGreatThanBCU")); } } }
       */
      EfinBudgetControlParam controlPrmObj = FundsReqMangementDAO
          .getControlParam(fundsLine.getClient().getId());

      if (fundsLine.getEfinFundsreq().getEFINFundsReqLineList().size() > 0) {
        if (!fundsLine.getEfinFundsreq().getEFINFundsReqLineList().get(0).getREQType()
            .equals(fundsLine.getREQType())) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_FundsReq_EithDisRel"));
        }
      }
      // check same "from account" exists for same FRM account in previous lines
      if (fundsLine.getDistType().equals("DIST")) {
        if (fundsLine.getFromaccount() != null) {
          if (!event.getPreviousState(fromAcct).equals(event.getCurrentState(fromAcct))) {
            Boolean isSameAccountExists = FundsReqMangementDAO.checkSameAccountExistsForSameFRM(
                fundsLine.getFromaccount().getId(), fundsLine.getEfinFundsreq().getId());
            if (isSameAccountExists) {
              throw new OBException(OBMessageUtils.messageBD("Efin_Same_From_Account"));
            }
          }
        }
        if (fundsLine.getFromaccount() == null && fundsLine.getToaccount() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_SelAnyAcct"));
        }
        if (fundsLine.getFromaccount() != null && fundsLine.getToaccount() != null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_CantselBothAcct"));
        }
      } else {
        if (fundsLine.getFromaccount() == null || fundsLine.getToaccount() == null) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_BothAcctMad"));
        }
        if (fundsLine.getFromaccount() != null && fundsLine.getToaccount() != null
            && fundsLine.getFromaccount().equals(fundsLine.getToaccount())) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_SameAcctNotAllow"));
        }
      }

      if (fundsLine.getToaccount() != null) {
        if (fundsLine.getDistType().equals("DIST")) {
          EFINFundsReqLine fromAcctLineobj = FundsReqMangementDAO
              .chkFrmAcctPrestorNot(fundsLine.getEfinFundsreq().getId(), fundsLine.getToaccount());
          if (fromAcctLineobj == null)
            throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_SelFromAccount"));
          if (fromAcctLineobj != null && fromAcctLineobj.getFromaccount() != null
              && fromAcctLineobj.getFromaccount().equals(fundsLine.getToaccount())) {
            throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_AlrdySameAcctInFrom"));
          }
        } else {
          Boolean combMatch = FundsReqMangementDAO.chkFrmAcctEquToAcct(fundsLine.getFromaccount(),
              fundsLine.getToaccount());
          if (!combMatch) {
            throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_SelSameDimeAct"));
          }
        }
      }
      if ((event.getCurrentState(decrease) != null
          && !event.getCurrentState(decrease).equals(event.getPreviousState(decrease)))
          || (event.getCurrentState(increase) != null
              && !event.getCurrentState(increase).equals(event.getPreviousState(increase)))) {
        if (fundsLine.getIncrease().compareTo(BigDecimal.ZERO) == 0
            && fundsLine.getDecrease().compareTo(BigDecimal.ZERO) == 0) {
          throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_EntIncOrDecAmt"));
        }
      }
      if (event.getCurrentState(reqtype) != null
          && !event.getCurrentState(reqtype).equals(event.getPreviousState(reqtype))) {
        if (fundsLine.getFromaccount() != null
            && fundsLine.getEfinFundsreq().getTransactionType().equals("ORGR")
            && fundsLine.getEfinFundsreq().getOrgreqFundsType() == null) {
          if (fundsLine.getREQType().equals("DIST") && controlPrmObj.getAgencyHqOrg() != null
              && (!fundsLine.getFromaccount().getOrganization().getId()
                  .equals(controlPrmObj.getAgencyHqOrg().getId()))) {
            throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_CantDistTranOrg"));
          }
        }
      }
      if (event.getCurrentState(fromAcct) != null
          && !event.getCurrentState(fromAcct).equals(event.getPreviousState(fromAcct))) {
        if (fundsLine.getFromaccount() != null
            && fundsLine.getFromaccount().getEfinUniquecodename() != null) {
          log.debug("uniquecode to :" + fundsLine.getFromaccount().getEfinUniquecodename());
          event.setCurrentState(frmUniquecodeName,
              fundsLine.getFromaccount().getEfinUniquecodename());
        }
      }

      if (event.getCurrentState(fromAcct) == null) {
        event.setCurrentState(frmUniquecodeName, null);
      }
      if (event.getCurrentState(toAcct) != null
          && !event.getCurrentState(toAcct).equals(event.getPreviousState(toAcct))) {
        if (fundsLine.getToaccount() != null
            && fundsLine.getToaccount().getEfinUniquecodename() != null) {
          log.debug("uniquecode to :" + fundsLine.getToaccount().getEfinUniquecodename());
          event.setCurrentState(toUniquecodeName, fundsLine.getToaccount().getEfinUniquecodename());
        }
      }
      if (event.getCurrentState(toAcct) == null) {
        event.setCurrentState(toUniquecodeName, null);
      }

      /*
       * if ((event.getCurrentState(decrease) != null &&
       * !event.getCurrentState(decrease).equals(event.getPreviousState(decrease)) || (event
       * .getCurrentState(fromAcct) != null && !event.getCurrentState(fromAcct).equals(
       * event.getPreviousState(fromAcct)))) &&
       * fundsLine.getEfinFundsreq().getTransactionType().equals("BCUR")) { if
       * (fundsLine.getDecrease().compareTo(fundsLine.getFundsAvailable()) > 0) { throw new
       * OBException(OBMessageUtils.messageBD("Efin_FundsReq_DecMoreThanFundsAvail")); } }
       */

      // If we change distribution decrease amount , we should calculate all corresponding to
      // account increase amount based on percentage

      if (fundsLine.getREQType().equals("DIST") && fundsLine.getDistType().equals("DIST")
          && fundsLine.getFromaccount() != null && (event.getCurrentState(decrease) != null
              && !event.getCurrentState(decrease).equals(event.getPreviousState(decrease)))) {

        EFINFundsReq header = fundsLine.getEfinFundsreq();

        if (fundsLine.getDistType().equals("DIST")) {

          List<EFINFundsReqLine> lineList = FundsReqMangementDAO
              .getToAccountBasedOnFromAcct(header.getId(), fundsLine.getFromaccount());

          for (EFINFundsReqLine line : lineList) {

            if (line.getDistType().equals("DIST") && line.getToaccount() != null) {

              line.setIncrease(fundsLine.getDecrease()
                  .multiply(line.getPercentage().divide(new BigDecimal(100))));
              OBDal.getInstance().save(line);

            }

          }

        }

      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while updating FundsReqLine: " + e);
      throw new OBException(e.getMessage());
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
      EFINFundsReqLine fundsLine = (EFINFundsReqLine) event.getTargetInstance();

      // OBQuery<EFINFundReqAppHist> fundsreqhist = OBDal.getInstance()
      // .createQuery(EFINFundReqAppHist.class, "as e where e.efinFundsreq.id='"
      // + fundsLine.getEfinFundsreq().getId() + "' order by creationDate desc");
      // fundsreqhist.setMaxResult(1);
      // if (fundsreqhist.list().size() > 0) {
      // EFINFundReqAppHist objLastLine = fundsreqhist.list().get(0);
      // if (fundsLine.getCreatedBy().getId().equals(OBContext.getOBContext().getUser().getId())) {
      // if (objLastLine.getFundsreqaction() != null
      // && (!objLastLine.getFundsreqaction().equals("REJ"))) {
      // throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_CantDelCom"));
      // }
      // } else {
      // throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_CantDelCom"));
      // }
      // }

      if (fundsLine.getEfinFundsreq() != null
          && (fundsLine.getEfinFundsreq().getDocumentStatus().equals("CO")
              || fundsLine.getEfinFundsreq().getDocumentStatus().equals("WFA"))) {
        throw new OBException(OBMessageUtils.messageBD("Efin_FundsReq_CantDelCom"));
      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while delete the FundsReq: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
