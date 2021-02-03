package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.event.dao.ProposalManagementLineEventDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Preferences;

public class ProposalMgmtLineEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmProposalmgmtLine.ENTITY_NAME) };
  private static final String PROPOSAL_WINDOW_ID = "CAF2D3EEF3B241018C8F65E8F877B29F";
  ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  @SuppressWarnings("unchecked")
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      EscmProposalmgmtLine proposalmgmtLine = (EscmProposalmgmtLine) event.getTargetInstance();

      Query query = null;
      BigDecimal bidLineQty = BigDecimal.ZERO;

      final Property uniqueCode = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_EFINUNIQUECODE);

      final Property isManual = entities[0].getProperty(EscmProposalmgmtLine.PROPERTY_MANUAL);

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      EscmProposalMgmt objProposal = proposalmgmtLine.getEscmProposalmgmt();

      String budgetController = "N";

      // Check Budget Controller Preference
      try {
        budgetController = Preferences.getPreferenceValue("ESCM_BudgetControl", true, clientId,
            objProposal.getOrganization().getId(), userId, roldId, PROPOSAL_WINDOW_ID, "N");
        budgetController = (budgetController == null) ? "N" : budgetController;

      } catch (PropertyException e) {
        budgetController = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      if (!budgetController.equals("Y") && objProposal.getEUTForwardReqmoreinfo() != null) {// check
                                                                                            // for
        // temporary
        // preference
        String requester_user_id = objProposal.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = objProposal.getEUTForwardReqmoreinfo().getRole().getId();
        budgetController = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            roldId, userId, clientId, objProposal.getOrganization().getId(), PROPOSAL_WINDOW_ID,
            requester_user_id, requester_role_id);
      }
      // if Budget Controller Preference not enabled then the user will not able to change the
      // values of
      // Budget related fields
      if (!budgetController.equals("Y") && (event.getCurrentState(isManual) == Boolean.TRUE)
          && objProposal.getEscmOldproposal() == null) {
        if (event.getCurrentState(uniqueCode) != null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }
      // insert lines in source reference tab
      if (proposalmgmtLine.isManual()
          && proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() == null) {
        final Entity proposalmgmtLineEntity = ModelProvider.getInstance()
            .getEntity(EscmProposalmgmtLine.ENTITY_NAME);
        final EscmProposalsourceRef srcref = OBProvider.getInstance()
            .get(EscmProposalsourceRef.class);
        srcref.setEscmProposalmgmtLine(proposalmgmtLine);
        srcref.setReservedQuantity((proposalmgmtLine.getMovementQuantity()));
        // if proposal mgmt line as parent then need to set reserved quantity as one for source ref
        // which is associated for that parent line
        if (proposalmgmtLine.getParentLineNo() != null) {
          EscmProposalmgmtLine parentProposalMgmtLine = OBDal.getInstance()
              .get(EscmProposalmgmtLine.class, proposalmgmtLine.getParentLineNo().getId());
          parentProposalMgmtLine.getEscmProposalsourceRefList().get(0)
              .setReservedQuantity(BigDecimal.ONE);
        }
        srcref.setLineNo(new Long(10));

        final Property refproperty = proposalmgmtLineEntity
            .getProperty(EscmProposalmgmtLine.PROPERTY_ESCMPROPOSALSOURCEREFLIST);
        final List<Object> srcls = (List<Object>) event.getCurrentState(refproperty);
        srcls.add(srcref);
      }

      // negative qty is not allowed
      if (proposalmgmtLine.getMovementQuantity().compareTo(BigDecimal.ZERO) <= 0
          || proposalmgmtLine.getPEEQty().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
      }

      // should not allow manual entry if bid is selected.
      if (proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() == null) {
        if (proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt() != null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_ProposalLine_Derived"));
        }
      }

      // check national product value is 1-100
      if (proposalmgmtLine.getNationalproduct() != null) {
        if (proposalmgmtLine.getNationalproduct() < 1
            || proposalmgmtLine.getNationalproduct() > 100) {
          throw new OBException(OBMessageUtils.messageBD("Escm_National_Product_value"));
        }
      }

      // chk proposal line match with bid qty or not
      if (proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt() != null) {
        if (proposalmgmtLine.getProduct() != null) {
          query = OBDal.getInstance().getSession().createSQLQuery(
              " select coalesce(movementqty,0) from escm_bidmgmt_line line where line.escm_bidmgmt_id= ? and line.m_product_id=? ");
          query.setParameter(0, proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt().getId());
          query.setParameter(1, proposalmgmtLine.getProduct().getId());
          if (query.list().size() > 0) {
            bidLineQty = (BigDecimal) query.list().get(0);
          }
        } else if (proposalmgmtLine.getDescription() != null) {
          query = OBDal.getInstance().getSession().createSQLQuery(
              " select coalesce(movementqty,0) from escm_bidmgmt_line line where line.escm_bidmgmt_id= ? and line.description=? ");
          query.setParameter(0, proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt().getId());
          query.setParameter(1, proposalmgmtLine.getDescription());
          if (query.list().size() > 0) {
            bidLineQty = (BigDecimal) query.list().get(0);
          }
        }

        if (bidLineQty != null
            && (bidLineQty.compareTo((proposalmgmtLine.getMovementQuantity())) < 0
                || bidLineQty.compareTo((proposalmgmtLine.getPEEQty())) < 0)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_PropQtyGrtBidQty"));
        }

      }

      if (!proposalmgmtLine.isSummary() && proposalmgmtLine.getNegotUnitPrice() != null)
        if ((proposalmgmtLine.getNegotUnitPrice().compareTo(proposalmgmtLine.getNetprice()) > 0)
            || (proposalmgmtLine.getPEENegotUnitPrice()
                .compareTo(proposalmgmtLine.getNetprice()) > 0)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NegPriceNotGrtNetPrice"));
        }
      if (proposalmgmtLine.getEscmProposalmgmt() != null
          && proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt() != null
          && !proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt().getBidtype().equals("DR")
          && proposalmgmtLine.getGrossUnitPrice() != null
          && proposalmgmtLine.getGrossUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
        if (!ProposalManagementLineEventDAO.chkopenvelopcomornot(
            proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt().getId())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_CantUpGrsPriceProsal"));
        }

      }

    } catch (OBException e) {
      log.error("exception while creating ProposalManagementLine Event", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating ProposalManagementLine Event", e);
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

      EscmProposalmgmtLine proposalmgmtLine = (EscmProposalmgmtLine) event.getTargetInstance();
      List<EscmProposalsourceRef> srcrefList = new ArrayList<EscmProposalsourceRef>();

      final Property movementQty = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_MOVEMENTQUANTITY);
      final Property grossprice = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_GROSSUNITPRICE);
      final Property techlinetotal = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_TECHLINETOTAL);
      final Property netprice = entities[0].getProperty(EscmProposalmgmtLine.PROPERTY_NETPRICE);
      final Property techUnitPrice = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_TECHUNITPRICE);
      final Property techQty = entities[0].getProperty(EscmProposalmgmtLine.PROPERTY_TECHLINEQTY);
      final Property peelinetotal = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_PEELINETOTAL);
      final Property peenegotiatePrice = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_PEENEGOTUNITPRICE);
      final Property peeqty = entities[0].getProperty(EscmProposalmgmtLine.PROPERTY_PEEQTY);
      final Property negotiatedPrice = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_NEGOTUNITPRICE);

      final Property uniqueCode = entities[0]
          .getProperty(EscmProposalmgmtLine.PROPERTY_EFINUNIQUECODE);

      BigDecimal srcrefqty = BigDecimal.ZERO;

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      EscmProposalMgmt objProposal = proposalmgmtLine.getEscmProposalmgmt();

      String budgetController = "N";
      // Check Budget Controller Preference
      try {
        budgetController = Preferences.getPreferenceValue("ESCM_BudgetControl", true, clientId,
            objProposal.getOrganization().getId(), userId, roldId, PROPOSAL_WINDOW_ID, "N");
        budgetController = (budgetController == null) ? "N" : budgetController;

      } catch (PropertyException e) {
        budgetController = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      if (!budgetController.equals("Y") && objProposal.getEUTForwardReqmoreinfo() != null) {// check
                                                                                            // for
        // temporary
        // preference
        String requester_user_id = objProposal.getEUTForwardReqmoreinfo().getUserContact().getId();
        String requester_role_id = objProposal.getEUTForwardReqmoreinfo().getRole().getId();
        budgetController = forwardDao.checkAndReturnTemporaryPreference("ESCM_BudgetControl",
            roldId, userId, clientId, objProposal.getOrganization().getId(), PROPOSAL_WINDOW_ID,
            requester_user_id, requester_role_id);
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

      // update source reference and qty

      BigDecimal newqty = ((BigDecimal) event.getCurrentState(movementQty));
      BigDecimal oldqty = ((BigDecimal) event.getPreviousState(movementQty));
      log.debug("newqty:" + newqty);
      log.debug("oldqty:" + oldqty);
      log.debug("oldqty" + oldqty);
      if ((event.getCurrentState(negotiatedPrice) != null && !event.getCurrentState(negotiatedPrice)
          .equals(event.getPreviousState(negotiatedPrice)))) {
        if (!proposalmgmtLine.isSummary() && proposalmgmtLine.getNegotUnitPrice() != null) {
          if ((proposalmgmtLine.getNegotUnitPrice()
              .compareTo(proposalmgmtLine.getNetprice()) > 0)) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_NegPriceNotGrtNetPrice"));
          }
        }
      }

      // // if pee qty is greater than proposal qty then should throw error
      // if (!event.getCurrentState(peeqty).equals(event.getPreviousState(peeqty))) {
      // if (new BigDecimal(event.getCurrentState(peeqty).toString())
      // .compareTo(new BigDecimal(event.getCurrentState(movementQty).toString())) > 0) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_OrderLine_Qty_Compaer(SrcQty)"));
      // }
      // }

      if (!event.getCurrentState(movementQty).equals(event.getPreviousState(movementQty))
          && newqty.compareTo(oldqty) < 0) {
        OBQuery<EscmProposalsourceRef> ref = OBDal.getInstance().createQuery(
            EscmProposalsourceRef.class, " as e where e.escmProposalmgmtLine.id=:proposalLineID");
        ref.setNamedParameter("proposalLineID", proposalmgmtLine.getId());
        srcrefList = ref.list();
        log.debug("list:" + srcrefList.size());
        if (srcrefList.size() > 0) {
          for (EscmProposalsourceRef reference : srcrefList) {
            srcrefqty = srcrefqty.add(reference.getReservedQuantity());
          }
        }
        log.debug("srcrefqty:" + srcrefqty);
        log.debug("qty:" + event.getCurrentState(movementQty));
        if (srcrefqty
            .compareTo(new BigDecimal(event.getCurrentState(movementQty).toString())) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_OrderLine_Qty_Compaer(SrcQty)"));
        }
      }
      // if proposal mgmt line as parent then need to set reserved quantity as one for source ref
      // which is associated for that parent line
      if (proposalmgmtLine.getParentLineNo() != null) {
        EscmProposalmgmtLine parentProposalMgmtLine = OBDal.getInstance()
            .get(EscmProposalmgmtLine.class, proposalmgmtLine.getParentLineNo().getId());
        if (parentProposalMgmtLine.getEscmProposalsourceRefList().size() > 0) {
          parentProposalMgmtLine.getEscmProposalsourceRefList().get(0)
              .setReservedQuantity(BigDecimal.ONE);
        }
      }

      if (!event.getCurrentState(movementQty).equals(event.getPreviousState(movementQty))
          && (newqty.compareTo(oldqty) > 0) && proposalmgmtLine.isManual()) {
        OBQuery<EscmProposalsourceRef> ref = OBDal.getInstance().createQuery(
            EscmProposalsourceRef.class,
            " as e where e.escmProposalmgmtLine.id=:proposalLineID and e.requisitionLine is null");
        ref.setNamedParameter("proposalLineID", proposalmgmtLine.getId());
        srcrefList = ref.list();
        log.debug("list:" + srcrefList.size());
        if (srcrefList.size() > 0) {
          EscmProposalsourceRef reference = srcrefList.get(0);
          reference.setReservedQuantity(newqty);
          OBDal.getInstance().save(reference);
        }
      }

      // Query query = null;
      BigDecimal bidLineQty = BigDecimal.ZERO;
      // negative qty is not allowed
      if (proposalmgmtLine.getMovementQuantity().compareTo(BigDecimal.ZERO) < 0
          || proposalmgmtLine.getPEEQty().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
      }

      // check national product value is 1-100
      if (proposalmgmtLine.getNationalproduct() != null) {
        if (proposalmgmtLine.getNationalproduct() < 1
            || proposalmgmtLine.getNationalproduct() > 100) {
          throw new OBException(OBMessageUtils.messageBD("Escm_National_Product_value"));
        }
      }

      // chk proposal line match with bid qty or not
      if ((!event.getPreviousState(movementQty).equals(event.getCurrentState(movementQty))
          || !event.getPreviousState(techQty).equals(event.getCurrentState(techQty))
          || !event.getPreviousState(peeqty).equals(event.getCurrentState(peeqty)))
          && proposalmgmtLine.getProduct() != null) {
        if (proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt() != null) {

          // Task No:7618 Note:20089 - Point 3
          bidLineQty = proposalmgmtLine.getEscmBidmgmtLine().getMovementQuantity();

          // query = OBDal.getInstance().getSession().createSQLQuery(
          // " select coalesce(movementqty,0) from escm_bidmgmt_line line where
          // line.escm_bidmgmt_id= ? and line.m_product_id=? ");
          // query.setParameter(0, proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt().getId());
          // query.setParameter(1, proposalmgmtLine.getProduct().getId());
          // if (query.list().size() > 0) {
          // bidLineQty = (BigDecimal) query.list().get(0);
          // }
          if (bidLineQty != null
              && (bidLineQty.compareTo((proposalmgmtLine.getMovementQuantity())) < 0
                  || bidLineQty.compareTo((proposalmgmtLine.getPEEQty())) < 0)) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PropQtyGrtBidQty"));
          }
          if (bidLineQty != null
              && (bidLineQty.compareTo((proposalmgmtLine.getTechLineQty())) < 0)) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PropQtyGrtBidQty"));
          }
        }
      }
      if (proposalmgmtLine.getTechLineQty().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Negative_Value_FinalQty"));
      }
      if (proposalmgmtLine.getTechUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Negative_Value_TechUnitPrice"));
      }
      // update header negotiated price in technical evaluation event header
      if (!event.getCurrentState(techlinetotal).equals(event.getPreviousState(techlinetotal))
          && (!ProposalManagementLineEventDAO
              .chkProsalAttlinkWithProornot(proposalmgmtLine.getEscmProposalmgmt()))) {
        ProposalManagementLineEventDAO.updateTechnicalNegPrice(proposalmgmtLine, event,
            techlinetotal);
      }
      // update header negotiated price in proposal header
      if (event.getPreviousState(peelinetotal) != null)
        if (!event.getCurrentState(peelinetotal).equals(event.getPreviousState(peelinetotal))
            && (!ProposalManagementLineEventDAO
                .chkProsalAttlinkWithProornot(proposalmgmtLine.getEscmProposalmgmt()))) {
          ProposalManagementLineEventDAO.updateProsalAttNegPrice(proposalmgmtLine, event,
              peenegotiatePrice, peelinetotal);
        }
      // change negotiatePrice to peenegotiatePrice
      if ((event.getCurrentState(peenegotiatePrice) != null && !event
          .getCurrentState(peenegotiatePrice).equals(event.getPreviousState(peenegotiatePrice)))
          || (event.getCurrentState(netprice) != null
              && !event.getCurrentState(netprice).equals(event.getPreviousState(netprice)))) {
        // proposalmgmtLine.getNegotUnitPrice() change to proposalmgmtLine.getPEENegotUnitPrice()
        if (proposalmgmtLine.getPEENegotUnitPrice().compareTo(proposalmgmtLine.getNetprice()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NegPriceNotGrtNetPrice"));
        }
      }
      if (((event.getCurrentState(techUnitPrice) != null
          && !event.getCurrentState(techUnitPrice).equals(event.getPreviousState(techUnitPrice)))
          || (event.getCurrentState(netprice) != null
              && !event.getCurrentState(netprice).equals(event.getPreviousState(netprice))))
          && proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt() != null && proposalmgmtLine
              .getEscmProposalmgmt().getEscmBidmgmt().getEscmTechnicalevlEventList().size() > 0) {
        if (proposalmgmtLine.getTechUnitPrice().compareTo(proposalmgmtLine.getNetprice()) > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_NegPriceNotGrtNetPrice"));
        }
      }

      if (event.getCurrentState(grossprice) != null
          && !event.getCurrentState(grossprice).equals(event.getPreviousState(grossprice))) {
        if (proposalmgmtLine.getEscmProposalmgmt() != null
            && proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt() != null
            && !proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt().getBidtype().equals("DR")
            && proposalmgmtLine.getGrossUnitPrice() != null
            && proposalmgmtLine.getGrossUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
          if (!ProposalManagementLineEventDAO.chkopenvelopcomornot(
              proposalmgmtLine.getEscmProposalmgmt().getEscmBidmgmt().getId())) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_CantUpGrsPriceProsal"));
          }
        }
      }
      if (proposalmgmtLine.getEscmProposalmgmt().isVersion()) {
        if (proposalmgmtLine.getDiscount().compareTo(new BigDecimal(0)) == 0
            && proposalmgmtLine.getDiscountmount().compareTo(new BigDecimal(0)) == 0) {
          proposalmgmtLine.getEscmProposalmgmt().setDiscountForTheDeal(BigDecimal.ZERO);
          proposalmgmtLine.getEscmProposalmgmt().setDiscountAmount(BigDecimal.ZERO);

        }

      }

    } catch (OBException e) {
      log.error("exception while updating ProposalManagementLine Event", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating ProposalManagementLine Event", e);
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
      EscmProposalmgmtLine proposalmgmtLine = (EscmProposalmgmtLine) event.getTargetInstance();
      EscmProposalMgmt obj_proposal = proposalmgmtLine.getEscmProposalmgmt();

      if (proposalmgmtLine.getEscmProposalmgmt().getProposalstatus().equals("AWD")) {
        if (proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() == null
            || (proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() != null
                && proposalmgmtLine.getEscmProposalmgmt().isVersion())
            || (proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() != null
                && !proposalmgmtLine.getEscmProposalmgmt().isVersion()
                && !proposalmgmtLine.getEscmProposalmgmt().getEscmDocaction().equals("SA")
                && !proposalmgmtLine.getEscmProposalmgmt().getProposalappstatus().equals("REA")))
          throw new OBException(OBMessageUtils.messageBD("Escm_proposal_deletelines"));
      }

      List<EscmProposalsourceRef> reflist = proposalmgmtLine.getEscmProposalsourceRefList();
      log.debug("reflist:" + reflist.size());
      if (obj_proposal != null) {
        List<EscmProposalmgmtLine> proposalmgmt_line_list = obj_proposal
            .getEscmProposalmgmtLineList();
        int listSize = proposalmgmt_line_list.size();
        if (listSize == 1) {
          obj_proposal.setADDRequisition(false);
          obj_proposal.setAgencyorg(null);

          OBDal.getInstance().save(obj_proposal);
        }
        obj_proposal.getEscmProposalmgmtLineList().remove(proposalmgmtLine);
      }
      if (proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() == null) {
        for (EscmProposalsourceRef ref : reflist) {
          if (ref.getRequisitionLine() != null) {
            RequisitionLine line = ref.getRequisitionLine();
            line.setUpdated(new java.util.Date());
            line.setUpdatedBy(OBContext.getOBContext().getUser());
            if (line.getEscmProposalsourceRefList().size() == 1) {
              line.setEscmIsproposal(false);
            }
            // line.setEscmProposalqty(line.getEscmProposalqty().subtract(ref.getReservedQuantity()));
            OBDal.getInstance().save(line);
          }
          ref.setReservedQuantity(BigDecimal.ZERO);
          OBDal.getInstance().save(ref);
        }
      }

      log.debug("reflist11:" + reflist.size());
      for (EscmProposalsourceRef ref1 : reflist) {
        OBDal.getInstance().remove(ref1);
      }

      if (!proposalmgmtLine.getEscmProposalmgmt().getProposalstatus().equals("DR")
          && proposalmgmtLine.getEscmBidmgmtLine() != null) {
        if (proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() == null
            || (proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() != null
                && proposalmgmtLine.getEscmProposalmgmt().isVersion())
            || (proposalmgmtLine.getEscmProposalmgmt().getEscmBaseproposal() != null
                && !proposalmgmtLine.getEscmProposalmgmt().isVersion()
                && !proposalmgmtLine.getEscmProposalmgmt().getEscmDocaction().equals("SA")
                && !proposalmgmtLine.getEscmProposalmgmt().getProposalappstatus().equals("REA")))
          throw new OBException(OBMessageUtils.messageBD("Escm_CannotDelete_Line"));
      }

    } catch (OBException e) {
      log.error("exception while deleting ProposalManagementLine Event: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while deleting ProposalManagementLine Event: ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
