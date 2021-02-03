package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.util.Date;

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
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.event.dao.BidManagementEventDAO;
import sa.elm.ob.utility.util.Utility;

public class BidManagementEvent extends EntityPersistenceEventObserver {

  /**
   * This class is used to handle the events in Bid Management header table
   */

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(EscmBidMgmt.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      String publishOn = OBMessageUtils.messageBD("Escm_Publishon");
      EscmBidMgmt bidmgmt = (EscmBidMgmt) event.getTargetInstance();
      final Property BidNo = entities[0].getProperty(EscmBidMgmt.PROPERTY_BIDNO);
      final Property returnaddlines = entities[0].getProperty(EscmBidMgmt.PROPERTY_PRADDLINES);
      String sequence = "";
      Boolean sequenceexists = false;

      // set new Spec No
      // sequence = Utility.getTransactionSequence(bidmgmt.getOrganization().getId(), "BM");
      // sequenceexists = Utility.chkTransactionSequence(bidmgmt.getOrganization().getId(), "BM",
      // sequence);

      if (bidmgmt.getBidtype().equals("DR")) {
        sequence = Utility.getTransactionSequence(bidmgmt.getOrganization().getId(), "BM-DR");
        sequenceexists = Utility.chkTransactionSequence(bidmgmt.getOrganization().getId(), "BM-DR",
            sequence);
      }

      if (bidmgmt.getBidtype().equals("TR") || bidmgmt.getBidtype().equals("LD")) {
        sequence = Utility.getTransactionSequence(bidmgmt.getOrganization().getId(), "BM-TR-LD");
        sequenceexists = Utility.chkTransactionSequence(bidmgmt.getOrganization().getId(),
            "BM-TR-LD", sequence);
      }

      if (!sequenceexists) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Duplicate_Sequence"));
      }
      // set new Spec No
      if (sequence.equals("false") || StringUtils.isEmpty(sequence)) {
        throw new OBException(OBMessageUtils.messageBD("Escm_NoSequence"));
      } else {
        event.setCurrentState(BidNo, sequence);
        event.setCurrentState(returnaddlines, true);
      }

      if (bidmgmt.getBidtype().equals("TR") || bidmgmt.getBidtype().equals("LD")) {
        if (!bidmgmt.getOrganization().isSummaryLevel()) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BidCreation_Issummary"));
        }
      }

      if (bidmgmt.getBidtype().equals("TR") || bidmgmt.getBidtype().equals("LD")) {
        if (bidmgmt.getBidpurpose() == null || StringUtils.isEmpty(bidmgmt.getBidpurpose())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BidPurposeIsEmpty"));
        }
      }

      bidmgmt.setRfpprice(bidmgmt.getRfpprice() == null ? BigDecimal.ZERO : bidmgmt.getRfpprice());
      if (validateNonDecimalValue(bidmgmt)) {
        // BigDecimal rfpPr = bidmgmt.getRfpprice();
        // RFP price field should mandatory for tender and limited bid type && should not allow zero
        if (bidmgmt.getBidtype().equals("TR")) {
          if (bidmgmt.getRfpprice() == null) {
            throw new OBException(OBMessageUtils.messageBD("Escm_RfpPrice"));
          }
          // else if (rfpPr.compareTo(new BigDecimal(0)) == 0) {
          // throw new OBException(OBMessageUtils.messageBD("Escm_RfpPrice_Zero"));
          // }
        }
        // RFP price field allow the max length as 9 digit
        if (bidmgmt.getRfpprice() != null) {
          String rfpprice = bidmgmt.getRfpprice().toString();
          if (rfpprice.length() > 9) {
            throw new OBException(OBMessageUtils.messageBD("Escm_RFPPrice_Format"));
          }
        }
      } else {
        throw new OBException(OBMessageUtils.messageBD("Escm_RFPPrice_Format"));
      }

      if (bidmgmt.getTabadulTenderID() != null) {
        String bidStat = publishOn + " " + BidManagementEventDAO.getCurrentDateInHijri();
        BidManagementEventDAO.insertBidPublishmentAlert(bidmgmt, bidStat);
      }
    } catch (OBException e) {
      log.debug("exception while creating BidManagementEvent" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while creating BidManagementEvent" + e);
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
      String publishOn = OBMessageUtils.messageBD("Escm_Publishon");
      EscmBidMgmt bidmgmt = (EscmBidMgmt) event.getTargetInstance();

      final Property type = entities[0].getProperty(EscmBidMgmt.PROPERTY_BIDTYPE);
      final Property price = entities[0].getProperty(EscmBidMgmt.PROPERTY_RFPPRICE);
      final Property appStatus = entities[0].getProperty(EscmBidMgmt.PROPERTY_BIDAPPSTATUS);
      final Property bidStatus = entities[0].getProperty(EscmBidMgmt.PROPERTY_BIDSTATUS);
      final Property tabdulId = entities[0].getProperty(EscmBidMgmt.PROPERTY_TABADULTENDERID);
      final Property publishedOn = entities[0].getProperty(EscmBidMgmt.PROPERTY_TABADULPUBLISHEDON);
      final Property tabadulNumber = entities[0].getProperty(EscmBidMgmt.PROPERTY_TABADULTENDERID);

      if (!event.getPreviousState(type).equals(event.getCurrentState(type))) {
        if (bidmgmt.getBidtype().equals("TR") || bidmgmt.getBidtype().equals("LD")) {
          if (!bidmgmt.getOrganization().isSummaryLevel()) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_BidCreation_Issummary"));
          }
        }
      }
      if (bidmgmt.getBidtype().equals("TR") || bidmgmt.getBidtype().equals("LD")) {
        if (bidmgmt.getBidpurpose() == null || StringUtils.isEmpty(bidmgmt.getBidpurpose())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_BidPurposeIsEmpty"));
        }
      }
      if (!event.getCurrentState(appStatus).equals("DR")) {
        bidmgmt
            .setRfpprice(bidmgmt.getRfpprice() == null ? BigDecimal.ZERO : bidmgmt.getRfpprice());
        if (validateNonDecimalValue(bidmgmt)) {
          // BigDecimal rfpPr = bidmgmt.getRfpprice();
          // RFP price field should mandatory for tender and limited bid type && should not allow
          // zero
          if (bidmgmt.getBidtype().equals("TR")) {
            if (bidmgmt.getRfpprice() == null) {
              throw new OBException(OBMessageUtils.messageBD("Escm_RfpPrice"));
            }
            // else if (rfpPr.compareTo(new BigDecimal(0)) == 0) {
            // throw new OBException(OBMessageUtils.messageBD("Escm_RfpPrice_Zero"));
            // }
          }
          if (event.getCurrentState(price) != null
              && !event.getCurrentState(price).equals(event.getPreviousState(price))) {
            if (bidmgmt.getRfpprice() != null) {
              String rfpprice = bidmgmt.getRfpprice().toString();
              if (rfpprice.length() > 9) {
                throw new OBException(OBMessageUtils.messageBD("Escm_RFPPrice_Format"));
              }
            }

          }
        } else {
          throw new OBException(OBMessageUtils.messageBD("Escm_RFPPrice_Format"));
        }
      }

      if (event.getCurrentState(price) != null) {
        if (bidmgmt.getBidtype().equals("LD") || bidmgmt.getBidtype().equals("DR")) {
          event.setCurrentState(price, BigDecimal.ZERO);
        }
      }
      int insertAlert = 0;
      if (bidmgmt.getTabadulTenderID() != null) {
        if (event.getPreviousState(tabdulId) != null) {
          if (!event.getPreviousState(tabdulId).equals(event.getCurrentState(tabdulId)))
            insertAlert = 1;
        } else {
          insertAlert = 1;
        }
        if (insertAlert == 1 && event.getCurrentState(publishedOn) == null) {
          log.debug("Publish Alert Insertion Fired" + event.getCurrentState(publishedOn));
          String bidStat = publishOn + " " + BidManagementEventDAO.getCurrentDateInHijri();
          BidManagementEventDAO.insertBidPublishmentAlert(bidmgmt, bidStat);
          BidManagementEventDAO.insertBidAlertToPRPreparer(bidmgmt, bidStat);
          BidManagementEventDAO.insertBidActionChangeAlert("EUT_116", bidmgmt, BigDecimal.ZERO,
              bidStat);
        }
      } else {
        event.setCurrentState(publishedOn, null);
      }
      if (!event.getPreviousState(bidStatus).equals(event.getCurrentState(bidStatus))
          || !event.getPreviousState(appStatus).equals(event.getCurrentState(appStatus))) {
        /*
         * if (event.getCurrentState(bidStatus).equals("ACT") &&
         * event.getCurrentState(appStatus).equals("ESCM_AP")) { // Call Alert insertion
         * log.debug("Approve Alert Insertion Fired"); //
         * BidManagementEventDAO.insertBidAlertToPRPreparer(bidmgmt, " is Approved");
         * BidManagementEventDAO.insertBidActionChangeAlert("EUT_116", bidmgmt, BigDecimal.ZERO,
         * " is Approved"); } else
         */ if (!event.getPreviousState(bidStatus).equals(event.getCurrentState(bidStatus))
            && (event.getCurrentState(bidStatus).equals("WD")
                || event.getCurrentState(bidStatus).equals("CD")
                || event.getCurrentState(bidStatus).equals("PP")
                || event.getCurrentState(bidStatus).equals("RES")
                || event.getCurrentState(bidStatus).equals("CL"))) {
          // Call Alert insertion
          log.debug("Action Alert Insertion Fired");
          String bidStat = event.getCurrentState(bidStatus).equals("WD") ? "Withdrawn"
              : event.getCurrentState(bidStatus).equals("CD") ? "Closed"
                  : event.getCurrentState(bidStatus).equals("PP") ? "PostPoned"
                      : event.getCurrentState(bidStatus).equals("RES") ? "ReSubmitted"
                          : event.getCurrentState(bidStatus).equals("CL") ? "Cancelled" : "";

          bidStat = " is " + bidStat;
          BidManagementEventDAO.insertBidAlertToPRPreparer(bidmgmt, bidStat);
          BidManagementEventDAO.insertBidActionChangeAlert("EUT_116", bidmgmt, BigDecimal.ZERO,
              bidStat);
        }
      }
      if (event.getPreviousState(tabadulNumber) == null
          && event.getCurrentState(tabadulNumber) != null) {
        event.setCurrentState(publishedOn, new Date());
      }
    } catch (OBException e) {
      log.debug("exception while updaing BidManagementEvent" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.debug("exception while updaing BidManagementEvent" + e);
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
      EscmBidMgmt bidmgmt = (EscmBidMgmt) event.getTargetInstance();

      if (bidmgmt.getBidappstatus().equals("ESCM_REJ")) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Bid_Rejected"));
      }
    } catch (OBException e) {
      log.error("Exception while deleting Bid Management Record:" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception while deleting Bid Management Record:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  public boolean validateNonDecimalValue(EscmBidMgmt bidmgmt) {
    boolean number = false;
    try {
      if (bidmgmt.getRfpprice() != null) {
        String rfpPrice = bidmgmt.getRfpprice().toString();
        // check rfp Price has special characters
        boolean isValid = false;
        String regex = "\\d+";
        isValid = rfpPrice.matches(regex);
        if (!isValid) {
          number = false;
          // throw new OBException(OBMessageUtils.messageBD("ESCM_BPContactAuthNoNotValid"));
        } else {
          number = true;
        }
      }
    } catch (OBException e) {
      log.debug("exception while saving bid" + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return number;
  }
}
