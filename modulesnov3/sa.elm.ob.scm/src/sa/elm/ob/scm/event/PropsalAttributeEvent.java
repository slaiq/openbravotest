package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.tax.ProposalTaxCalculationDAOImpl;
import sa.elm.ob.scm.event.dao.ProposalAttributeEventDAO;

public class PropsalAttributeEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmProposalAttribute.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  // Should not allow negative value in gross price field
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      List<EscmProposalAttribute> proattlist = new ArrayList<EscmProposalAttribute>();
      EscmProposalAttribute proposalattribute = (EscmProposalAttribute) event.getTargetInstance();
      final Property rank = entities[0].getProperty(EscmProposalAttribute.PROPERTY_RANK);
      final Property netPrice = entities[0].getProperty(EscmProposalAttribute.PROPERTY_NETPRICE);

      final Property teeIsTax = entities[0].getProperty(EscmProposalAttribute.PROPERTY_TEEISTAX);
      final Property peeIsTax = entities[0].getProperty(EscmProposalAttribute.PROPERTY_PEEISTAX);
      final Property teeTaxmethod = entities[0]
          .getProperty(EscmProposalAttribute.PROPERTY_TEEEFINTAXMETHOD);
      final Property peeTaxmethod = entities[0]
          .getProperty(EscmProposalAttribute.PROPERTY_PEEEFINTAXMETHOD);

      final Property encumMethod = entities[0]
          .getProperty(EscmProposalAttribute.PROPERTY_EFINENCUMBRANCEMETHOD);
      final Property manEncum = entities[0]
          .getProperty(EscmProposalAttribute.PROPERTY_EFINMANUALENCUMBRANCE);
      final Property uniqueCode = entities[0]
          .getProperty(EscmProposalAttribute.PROPERTY_EFINUNIQUECODE);

      ProposalTaxCalculationDAO taxDao = new ProposalTaxCalculationDAOImpl();

      if (proposalattribute.getEscmOpenenvcommitee() != null) {
        if (proposalattribute.getEscmOpenenvcommitee().getBidNo() != null
            && ((proposalattribute.getEscmOpenenvcommitee().getBidNo().getBidtype().equals("TR"))
                || proposalattribute.getEscmOpenenvcommitee().getBidNo().getBidtype()
                    .equals("LD"))) {
          if (proposalattribute.getGrossPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Proposalattr_gross"));
          }

          if (proposalattribute.getGrossPrice().compareTo(BigDecimal.ZERO) == 0) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Proposalattr_Zero"));
          }
        }
      }
      if (proposalattribute.getProposalstatus() != null
          && proposalattribute.getProposalstatus().equals("DIS")
          && proposalattribute.getDiscardedReason() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_PropAttr_DisCardReasonMand"));
      }
      if (proposalattribute.getRank() != null && event.getCurrentState(rank) != null
          && !event.getCurrentState(rank).equals(event.getPreviousState(rank))) {
        OBQuery<EscmProposalAttribute> propattr = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class,
            " as e where e.rank=:ranK and e.escmProposalevlEvent.id=:proeventID");
        propattr.setNamedParameter("ranK", proposalattribute.getRank());
        propattr.setNamedParameter("proeventID",
            proposalattribute.getEscmProposalevlEvent().getId());

        proattlist = propattr.list();
        if (proattlist.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_ProposalSameRank"));
        }
      }

      // If isTax is checked and tax method is not given, throw error
      if (proposalattribute.isTEEIstax() && proposalattribute.getTEEEfinTaxMethod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoTaxMethod"));
      }
      if (proposalattribute.isPEEIstax() && proposalattribute.getPEEEfinTaxMethod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoTaxMethod"));
      }

      // If TEE istaxline is unchecked, set line tax amount as 0
      if (event.getPreviousState(teeIsTax) != null
          && !event.getPreviousState(teeIsTax).equals(event.getCurrentState(teeIsTax))) {
        if (!proposalattribute.isTEEIstax()) {

          List<EscmProposalmgmtLine> taxableLines = new ArrayList<EscmProposalmgmtLine>();
          taxableLines = taxDao.getProposalLines(proposalattribute.getEscmProposalmgmt());

          if (taxableLines.size() > 0) {
            for (EscmProposalmgmtLine line : taxableLines) {
              line.setTechUnitPrice(line.getNegotUnitPrice());
              line.setTEELineTaxamt(BigDecimal.ZERO);
              line.setTechLineTotal(line.getTechLineQty().multiply(line.getTechUnitPrice()));
              line.setTechDiscount(BigDecimal.ZERO);
              line.setTechDiscountamt(BigDecimal.ZERO);
            }
          }
        }
      }

      // If PEE istaxline is unchecked, set line tax amount as 0
      if (event.getPreviousState(peeIsTax) != null
          && !event.getPreviousState(peeIsTax).equals(event.getCurrentState(peeIsTax))) {
        if (!proposalattribute.isPEEIstax()) {

          List<EscmProposalmgmtLine> taxableLines = new ArrayList<EscmProposalmgmtLine>();
          taxableLines = taxDao.getProposalLines(proposalattribute.getEscmProposalmgmt());

          if (taxableLines.size() > 0) {
            for (EscmProposalmgmtLine line : taxableLines) {
              if (line.getEscmProposalmgmt().getProposalType().equals("DR"))
                line.setPEENegotUnitPrice(line.getNegotUnitPrice());
              else
                line.setPEENegotUnitPrice(line.getTechUnitPrice());
              line.setPEELineTaxamt(BigDecimal.ZERO);
              line.setPEELineTotal(line.getLineTotal());
              line.setPEETechDiscount(BigDecimal.ZERO);
              line.setPEETechDiscountamt(BigDecimal.ZERO);
              OBDal.getInstance().save(line);
            }
          }
        }
      }
      // If TEE tax method is changed
      if (event.getPreviousState(teeIsTax) != null
          && event.getPreviousState(teeIsTax).equals(event.getCurrentState(teeIsTax))) {
        if (proposalattribute.isTEEIstax()
            && !event.getPreviousState(teeTaxmethod).equals(event.getCurrentState(teeTaxmethod))) {

          List<EscmProposalmgmtLine> taxableLines = new ArrayList<EscmProposalmgmtLine>();
          taxableLines = taxDao.getProposalLines(proposalattribute.getEscmProposalmgmt());

          if (taxableLines.size() > 0) {
            for (EscmProposalmgmtLine line : taxableLines) {
              line.setTechUnitPrice(line.getNegotUnitPrice());
              line.setTEELineTaxamt(BigDecimal.ZERO);
              line.setTechLineTotal(line.getTechLineQty().multiply(line.getTechUnitPrice()));
              line.setTechDiscount(BigDecimal.ZERO);
              line.setTechDiscountamt(BigDecimal.ZERO);
            }
          }
        }
      }
      // If PEE tax method is changed
      if (event.getPreviousState(peeIsTax) != null
          && event.getPreviousState(peeIsTax).equals(event.getCurrentState(peeIsTax))) {
        if (proposalattribute.isPEEIstax()
            && !event.getPreviousState(peeTaxmethod).equals(event.getCurrentState(peeTaxmethod))) {

          List<EscmProposalmgmtLine> taxableLines = new ArrayList<EscmProposalmgmtLine>();
          taxableLines = taxDao.getProposalLines(proposalattribute.getEscmProposalmgmt());

          if (taxableLines.size() > 0) {
            for (EscmProposalmgmtLine line : taxableLines) {

              if (line.getEscmProposalmgmt().getProposalType().equals("DR"))
                line.setPEENegotUnitPrice(line.getNegotUnitPrice());
              else
                line.setPEENegotUnitPrice(line.getTechUnitPrice());

              line.setPEELineTaxamt(BigDecimal.ZERO);
              line.setPEELineTotal(line.getLineTotal());
              line.setPEETechDiscount(BigDecimal.ZERO);
              line.setPEETechDiscountamt(BigDecimal.ZERO);
              OBDal.getInstance().save(line);
            }
          }
        }
      }

      if (proposalattribute.getNetPrice() != null && event.getCurrentState(netPrice) != null
          && !event.getCurrentState(netPrice).equals(event.getPreviousState(netPrice))) {
        for (ESCMBGWorkbench bgWorkbench : proposalattribute.getESCMBGWorkbenchList()) {
          bgWorkbench.setDocumentAmount(proposalattribute.getNetPrice());
          OBDal.getInstance().save(bgWorkbench);
        }

      }

      // Update encumbrance method in Proposal Mgmt
      if ((event.getPreviousState(encumMethod) != null && event.getCurrentState(encumMethod) != null
          && !event.getPreviousState(encumMethod).equals(event.getCurrentState(encumMethod)))
          || (event.getPreviousState(encumMethod) == null
              && event.getCurrentState(encumMethod) != null)
          || (event.getPreviousState(encumMethod) != null
              && event.getCurrentState(encumMethod) == null)) {
        EscmProposalMgmt proposal = proposalattribute.getEscmProposalmgmt();
        proposal.setEFINEncumbranceMethod(proposalattribute.getEFINEncumbranceMethod());
      }

      // Update manual encumbrance in Proposal Mgmt
      if ((event.getPreviousState(manEncum) != null && event.getCurrentState(manEncum) != null
          && !event.getPreviousState(manEncum).equals(event.getCurrentState(manEncum)))
          || (event.getPreviousState(manEncum) == null && event.getCurrentState(manEncum) != null)
          || (event.getPreviousState(manEncum) != null
              && event.getCurrentState(manEncum) == null)) {
        EscmProposalMgmt proposal = proposalattribute.getEscmProposalmgmt();
        proposal.setEfinEncumbrance(proposalattribute.getEFINManualEncumbrance());
      }

      // Update unique code in Proposal Mgmt
      if ((event.getPreviousState(uniqueCode) != null && event.getCurrentState(uniqueCode) != null
          && !event.getPreviousState(uniqueCode).equals(event.getCurrentState(uniqueCode)))
          || (event.getPreviousState(uniqueCode) == null
              && event.getCurrentState(uniqueCode) != null)
          || (event.getPreviousState(uniqueCode) != null
              && event.getCurrentState(uniqueCode) == null)) {
        EscmProposalMgmt proposal = proposalattribute.getEscmProposalmgmt();
        proposal.setEFINUniqueCode(proposalattribute.getEFINUniqueCode());
      }

    } catch (OBException e) {
      log.error("exception while updating PropsalAttributeEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating PropsalAttributeEvent", e);
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
      EscmProposalAttribute proposalattribute = (EscmProposalAttribute) event.getTargetInstance();
      EscmProposalMgmt proposal = proposalattribute.getEscmProposalmgmt();

      // before delete the proposal attribute update the line details. already update the lines we
      // set the delete line flag as flase.
      log.debug("on delete");

      if (proposal.getEscmBidmgmt() == null) {
        if (proposalattribute.getEscmProposalevlEvent() != null
            && proposalattribute.getEscmProposalevlEvent().isDeletelines()) {
          log.debug("on update");

          ProposalAttributeEventDAO.updateProposalLineDetails(proposalattribute, proposal,
              proposalattribute.getEscmProposalevlEvent());
        }
      }

    } catch (OBException e) {
      log.error("Exception while updating PO Receipt:", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while updating PO Receipt:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
