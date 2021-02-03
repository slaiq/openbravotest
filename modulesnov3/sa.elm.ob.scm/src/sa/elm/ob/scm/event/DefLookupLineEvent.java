package sa.elm.ob.scm.event;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
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
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.utility.util.Utility;

/**
 * @author qualian
 * 
 */
public class DefLookupLineEvent extends EntityPersistenceEventObserver {

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ESCMDefLookupsTypeLn.ENTITY_NAME) };

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
      ESCMDefLookupsTypeLn lookup = (ESCMDefLookupsTypeLn) event.getTargetInstance();

      OBQuery<ESCMDefLookupsTypeLn> typelist = OBDal.getInstance().createQuery(
          ESCMDefLookupsTypeLn.class,
          " as e where ( e.commercialName=:comName or e.searchKey =:searchKey ) "
              + " and e.client.id=:clientID and e.escmDeflookupsType.id=:lookupTypeID");
      typelist.setNamedParameter("comName", lookup.getCommercialName());
      typelist.setNamedParameter("searchKey", lookup.getSearchKey());
      typelist.setNamedParameter("clientID", lookup.getClient().getId());
      typelist.setNamedParameter("lookupTypeID", lookup.getEscmDeflookupsType().getId());

      log.debug("typelist.size" + typelist.list().size());
      if (typelist.list().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_LookupLine_Unique"));
      }

      if (validateMinValue(lookup)) {
        event.setCurrentState(entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_MINVALUE), "1");
      }
      if (validateMaxValue(lookup)) {
        event.setCurrentState(entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_MAXVALUE),
            "100");
      }
      if (lookup.getEscmDeflookupsType().getReference().equals("BC")
          && lookup.getBidType() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_RefLupBidClassBidTypMan"));
      }
      if (validateToleranceValue(lookup)) {
        event.setCurrentState(entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_TOLERANCEVALUE),
            "0");
      }
      if (lookup.getReceiveType() == null
          && lookup.getEscmDeflookupsType().getReference().equals("POCONCATG")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Receive_Type_Mandatory"));
      }
    } catch (OBException e) {
      log.error("exception while creating lookup", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating lookup", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unchecked")
  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();

      ESCMDefLookupsTypeLn lookup = (ESCMDefLookupsTypeLn) event.getTargetInstance();
      final Property name = entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_SEARCHKEY);
      final Property code = entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_COMMERCIALNAME);
      final Property bidtype = entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_BIDTYPE);
      final Property datatype = entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_DATATYPE);
      final Property receiveType = entities[0]
          .getProperty(ESCMDefLookupsTypeLn.PROPERTY_RECEIVETYPE);
      Query query = null;
      List<Object> poReceiptList = new ArrayList<>();
      List<Order> poContractCategoryList = null;

      if (!event.getCurrentState(name).equals(event.getPreviousState(name))
          || !event.getCurrentState(code).equals(event.getPreviousState(code))) {
        OBQuery<ESCMDefLookupsTypeLn> typelist = OBDal.getInstance().createQuery(
            ESCMDefLookupsTypeLn.class,
            " as e  where ( e.commercialName=:comName or e.searchKey =:searchKey ) "
                + " and e.client.id=:clientID and  e.escmDeflookupsType.id=:lookupTypeID and e.id <>:lookupId");
        typelist.setNamedParameter("comName", lookup.getCommercialName());
        typelist.setNamedParameter("searchKey", lookup.getSearchKey());
        typelist.setNamedParameter("clientID", lookup.getClient().getId());
        typelist.setNamedParameter("lookupTypeID", lookup.getEscmDeflookupsType().getId());
        typelist.setNamedParameter("lookupId", lookup.getId());
        log.debug("typelist.size" + typelist.list().size());
        if (typelist.list().size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_LookupLine_Unique"));
        }
      }

      if (validateMinValue(lookup)) {
        event.setCurrentState(entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_MINVALUE), "1");
      }
      if (validateMaxValue(lookup)) {
        event.setCurrentState(entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_MAXVALUE),
            "100");
      }

      if (lookup.getEscmBidtermcdnAttributenameList().size() > 0
          && !event.getCurrentState(datatype).equals(event.getPreviousState(datatype))) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup(WithBidTermsupdate)"));
      }

      if (lookup.getEscmDeflookupsType().getReference().equals("BC")
          && (event.getPreviousState(bidtype) != null && event.getCurrentState(bidtype) == null)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_RefLupBidClassBidTypMan"));
      }
      if (validateToleranceValue(lookup)) {
        event.setCurrentState(entities[0].getProperty(ESCMDefLookupsTypeLn.PROPERTY_TOLERANCEVALUE),
            "0");
      }
      if (lookup.getEscmDeflookupsType().getReference().equals("POCONCATG")) {
        if (event.getPreviousState(receiveType) != null
            && !event.getCurrentState(receiveType).equals(event.getPreviousState(receiveType))) {
          String sqlString = "select m_inout_id from m_inout where c_order_id is not null "
              + " and c_order_id in (select c_order_id from c_order where EM_Escm_Contact_Type "
              + " in (select Escm_Deflookups_Typeln_ID from Escm_Deflookups_Typeln"
              + " where ESCM_DefLookups_Type_id in (select ESCM_DefLookups_Type_id from ESCM_DefLookups_Type where Reference='POCONCATG')))";
          query = OBDal.getInstance().getSession().createSQLQuery(sqlString);
          poReceiptList = query.list();
          if (poReceiptList.size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_ContractCategory_Used"));
          }
        }
      }
      if (lookup.getReceiveType() == null
          && lookup.getEscmDeflookupsType().getReference().equals("POCONCATG")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Receive_Type_Mandatory"));
      }
      // to check where contract category is used in PO
      if (lookup.getEscmDeflookupsType().getReference().equals("POCONCATG")) {
        if (event.getPreviousState(receiveType) != null
            && !event.getCurrentState(receiveType).equals(event.getPreviousState(receiveType))) {
          OBQuery<Order> poContractCategory = OBDal.getInstance().createQuery(Order.class,
              " as e  where e.escmContactType.id =:defLookUpLineID");
          poContractCategory.setNamedParameter("defLookUpLineID", lookup.getId());
          poContractCategoryList = poContractCategory.list();
          if (poContractCategoryList.size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("Escm_ContractCatg_Used"));
          }

        }

      }
    } catch (OBException e) {
      log.error("exception while updating lookup", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating lookup", e);
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
      ESCMDefLookupsTypeLn lookup = (ESCMDefLookupsTypeLn) event.getTargetInstance();
      List<Order> poContractCategoryList = null;
      /*
       * if (lookup.getMaterialMgmtShipmentInOutEMEscmIssuereasonList().size() > 0) { throw new
       * OBException(OBMessageUtils.messageBD("ESCM_Lookup(withIssueReturn)")); }
       */
      if (lookup.getMaterialMgmtShipmentInOutEMEscmReturnreasonList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup(WithReturn)"));
      }
      if (lookup.getEscmBidtermcdnAttributenameList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup(WithBidTerms)"));
      }
      // to check where contract category is used in PO
      if (lookup.getEscmDeflookupsType().getReference().equals("POCONCATG")) {
        OBQuery<Order> poContractCategory = OBDal.getInstance().createQuery(Order.class,
            " as e  where e.escmContactType.id =:defLookUpLineID");
        poContractCategory.setNamedParameter("defLookUpLineID", lookup.getId());
        poContractCategoryList = poContractCategory.list();
        if (poContractCategoryList.size() > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_ContractCatg_Used"));
        }

      }
    } catch (OBException e) {
      log.error(" Exception while Deleting LookUp Line  : ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting LookUp Line  : ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Validates the min value for initial BG reference lookup If there is no minvalue then we will
   * return true to set minvalue as "1" in main method
   * 
   * @param referencelookup
   *          referencelookup object
   * @throws OBException
   *           based on different validation
   * 
   * @return true or false. If it is true then we will set minvalue as "1" in main method
   */
  private Boolean validateMinValue(ESCMDefLookupsTypeLn lookup) {
    if (lookup.getSearchKey().equals("ING")) {
      if (lookup.getDatatype().equalsIgnoreCase("number")) {
        if (lookup.getMinvalue() == null || StringUtils.isEmpty(lookup.getMinvalue())) {
          return true;
        } else if (lookup.getMinvalue().equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_Zero"));
        } else if (!lookup.getMinvalue().matches(Utility.twoDecimalCheck)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Nowithtwodec"));
        } else {
          if (lookup.getMinvalue().contains(".")) {
            if (Double.parseDouble(lookup.getMinvalue()) > 100) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_hundred"));
            } else if (Double.parseDouble(lookup.getMinvalue()) < 0) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_Zero"));
            }
          } else {
            if (Integer.parseInt(lookup.getMinvalue()) > 100) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_hundred"));
            }
          }
        }
      } else if (lookup.getDatatype().equalsIgnoreCase("integer")) {
        if (lookup.getMinvalue() == null || StringUtils.isEmpty(lookup.getMinvalue())) {
          return true;
        } else if (!StringUtils.isNumeric(lookup.getMinvalue())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_onlyInteger"));
        } else if (lookup.getMinvalue().equals("0") || Integer.parseInt(lookup.getMinvalue()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_Zero"));
        } else {
          if (Integer.parseInt(lookup.getMinvalue()) > 100) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_hundred"));
          }
        }
      }
    }
    return false;
  }

  /**
   * Validates the max value for initial BG reference lookup If there is no maxvalue then we will
   * return true to set maxvalue as "100" in main method
   * 
   * @param referencelookup
   *          referencelookup object
   * @throws OBException
   *           based on different validation
   * 
   * @return true or false. If it is true then we will set maxvalue as 100 in main method
   */
  private Boolean validateMaxValue(ESCMDefLookupsTypeLn lookup) {
    if (lookup.getSearchKey().equals("ING")) {
      if (lookup.getDatatype().equalsIgnoreCase("number")) {
        if (lookup.getMaxvalue() == null || StringUtils.isEmpty(lookup.getMaxvalue())) {
          return true;
        } else if (lookup.getMaxvalue().equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_Zero"));
        } else if (!lookup.getMaxvalue().matches(Utility.twoDecimalCheck)) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Nowithtwodec"));
        } else {
          if (lookup.getMaxvalue().contains(".")) {
            if (Double.parseDouble(lookup.getMaxvalue()) > 100) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_hundred"));
            } else if (Double.parseDouble(lookup.getMaxvalue()) < 0) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_Zero"));
            }
          } else {
            if (Integer.parseInt(lookup.getMaxvalue()) > 100) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_hundred"));
            }
          }

          if ((lookup.getMaxvalue().contains(".") ? Double.parseDouble(lookup.getMaxvalue())
              : Integer.parseInt(lookup.getMaxvalue())) <= (lookup.getMinvalue().contains(".")
                  ? Double.parseDouble(lookup.getMinvalue())
                  : Integer.parseInt(lookup.getMinvalue()))) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_Max"));
          }
        }
      } else if (lookup.getDatatype().equalsIgnoreCase("integer")) {
        if (lookup.getMaxvalue() == null || StringUtils.isEmpty(lookup.getMaxvalue())) {
          return true;
        } else if (!StringUtils.isNumeric(lookup.getMaxvalue())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_onlyInteger"));
        } else if (lookup.getMaxvalue().equals("0") || Integer.parseInt(lookup.getMaxvalue()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_Zero"));
        } else {
          if (Integer.parseInt(lookup.getMaxvalue()) > 100) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_hundred"));
          }
        }
        if (Integer.parseInt(lookup.getMaxvalue()) < Integer.parseInt(lookup.getMinvalue())) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_Lookup_Greater_Max"));
        }
      }
    }
    return false;
  }

  /**
   * Validates the tolerance value should be between 0 to 100 for Proc_Tolerance reference lookup
   * 
   * @param referencelookup
   * @throws OBException
   * @return true or false
   */
  private Boolean validateToleranceValue(ESCMDefLookupsTypeLn lookup) {
    // Tolerance reference Look Up
    if (lookup.getEscmDeflookupsType().getReference().equals("POTOL")) {
      if (lookup.getToleranceValue() == null || StringUtils.isEmpty(lookup.getToleranceValue())) {
        return true;
      } else if (!lookup.getToleranceValue().matches(Utility.twoDecimalCheck)) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_ToleranceValueInNumber"));
      }
      if (lookup.getToleranceValue().contains(".")) {
        if (Double.parseDouble(lookup.getToleranceValue()) > 100) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_ValueBetween_ZerotoHundred"));
        } else if (Double.parseDouble(lookup.getToleranceValue()) < 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_ValueBetween_ZerotoHundred"));
        }
      } else {
        if (Integer.parseInt(lookup.getToleranceValue()) > 100) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_ValueBetween_ZerotoHundred"));
        }
      }
    }
    return false;
  }
}
