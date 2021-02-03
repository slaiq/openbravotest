package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
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
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Preferences;

/**
 * 
 * @author Gopalakrishnan 17/07/2017
 * 
 */

public class POContractLineEvent extends EntityPersistenceEventObserver {
  /**
   * This servlet class is responsible for business events in orderline table
   */
  private Logger log = Logger.getLogger(this.getClass());
  ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
  final String PO_Window_ID = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      OrderLine objOrderLine = (OrderLine) event.getTargetInstance();
      BigDecimal srcrefqty = BigDecimal.ZERO;
      String objOrderId = "";
      final Property qty = entities[0].getProperty(OrderLine.PROPERTY_ORDEREDQUANTITY);
      final Property product = entities[0].getProperty(OrderLine.PROPERTY_PRODUCT);
      final Property description = entities[0].getProperty(OrderLine.PROPERTY_ESCMPRODESCRIPTION);
      final Property poLineUpdatedAmt = entities[0].getProperty(OrderLine.PROPERTY_LINENETAMOUNT);
      final Property poLinePOChangeValue = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMPOCHANGEVALUE);
      final Property poLinePOChangeFact = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMPOCHANGEFACTOR);
      final Property grosslineTotal = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMLINETOTALUPDATED);
      final Property unitprice = entities[0].getProperty(OrderLine.PROPERTY_UNITPRICE);
      final Property poLinePOChangetype = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMPOCHANGETYPE);
      final Property taxAmount = entities[0].getProperty(OrderLine.PROPERTY_ESCMLINETAXAMT);
      final Property uniqueCode = entities[0].getProperty(OrderLine.PROPERTY_EFINUNIQUECODE);
      final Property initialUnitPrice = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMINITIALUNITPRICE);
      final Property roundOfdiffdueTax = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMROUNDDIFFTAX);
      final Property unitPriceAfterChange = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMUNITPRICEAFTERCHAG);
      final Property isPoChangeCal = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMISPOCHGEVALCALC);

      boolean isTaxChanges = false;
      boolean isPoFactorChanges = false;
      boolean isQtyOrUnitPriceChanges = false;
      BigDecimal changeValue = BigDecimal.ZERO;
      BigDecimal newqty = (BigDecimal) event.getCurrentState(qty);
      // BigDecimal oldqty = (BigDecimal) event.getPreviousState(qty);
      BigDecimal oldtaxAmt = (BigDecimal) event.getPreviousState(taxAmount);
      BigDecimal newTaxAmt = (BigDecimal) event.getCurrentState(taxAmount);
      BigDecimal grossNetAmt = BigDecimal.ZERO;
      BigDecimal oldChangeValue = BigDecimal.ZERO;
      BigDecimal newChangeValue = BigDecimal.ZERO;
      BigDecimal oldUnitPrice = (BigDecimal) event.getCurrentState(unitprice);
      BigDecimal newUnitPrice = (BigDecimal) event.getPreviousState(unitprice);
      BigDecimal remAmt = BigDecimal.ZERO;
      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      Order objOrder = objOrderLine.getSalesOrder();

      String preferenceValue = "N";
      // Check Budget Controller Preference
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true, clientId,
            objOrder.getOrganization().getId(), userId, roldId, PO_Window_ID, "N");
        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;

      } catch (PropertyException e) {
        preferenceValue = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      if (!preferenceValue.equals("Y") && objOrder.getEutForward() != null) {// check for
        // temporary
        // preference
        String requester_user_id = objOrder.getEutForward().getUserContact().getId();
        String requester_role_id = objOrder.getEutForward().getRole().getId();
        preferenceValue = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", roldId, userId, clientId, objOrder.getOrganization().getId(),
            PO_Window_ID, requester_user_id, requester_role_id);
      }
      // if Budget Controller Preference not enabled then the user will not able to change the
      // values of
      // Budget related fields
      if (!preferenceValue.equals("Y")) {
        if ((event.getCurrentState(uniqueCode) != null
            && !event.getCurrentState(uniqueCode).equals(event.getPreviousState(uniqueCode)))) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      if (newUnitPrice != null && (newUnitPrice.scale() > oldUnitPrice.scale())) {
        newUnitPrice = newUnitPrice.setScale(oldUnitPrice.scale(), RoundingMode.HALF_UP);
      } else {
        if (newUnitPrice != null)
          oldUnitPrice = oldUnitPrice.setScale(newUnitPrice.scale(), RoundingMode.HALF_UP);
      }
      if (event.getCurrentState(poLinePOChangeValue) != null) {
        newChangeValue = (BigDecimal) event.getCurrentState(poLinePOChangeValue);
        oldChangeValue = (BigDecimal) event.getPreviousState(poLinePOChangeValue);
        // if (newChangeValue.scale() > oldChangeValue.scale()) {
        // newChangeValue = newChangeValue.setScale(oldChangeValue.scale(), RoundingMode.HALF_UP);
        // } else {
        // oldChangeValue = oldChangeValue.setScale(newChangeValue.scale(), RoundingMode.HALF_UP);
        // }
      }
      log.debug("newqty:" + newqty);
      /*
       * if (!event.getCurrentState(qty).equals(event.getPreviousState(qty)) &&
       * newqty.compareTo(oldqty) < 0) { OBQuery<EscmOrderSourceRef> ref =
       * OBDal.getInstance().createQuery(EscmOrderSourceRef.class,
       * " as e where e.salesOrderLine.id='" + objOrderLine.getId() + "'"); log.debug("list:" +
       * ref.list().size()); if (ref.list().size() > 0) { for (EscmOrderSourceRef reference :
       * ref.list()) { srcrefqty = srcrefqty.add(reference.getReservedQuantity()); } }
       * log.debug("srcrefqty:" + srcrefqty); log.debug("qty:" + event.getCurrentState(qty)); if
       * (srcrefqty.compareTo(new BigDecimal(event.getCurrentState(qty).toString())) > 0) { throw
       * new OBException(OBMessageUtils.messageBD("ESCM_OrderLine_Qty_Compaer(SrcQty)")); } }
       */
      if (objOrderLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) == 0) {
        if (objOrderLine.getEscmOldOrderline() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_QtyCantBeZeroForNewLine"));
        }
      }

      if (!event.getCurrentState(qty).equals(event.getPreviousState(qty))
          && objOrderLine.isEscmIsmanual()) { // && (newqty.compareTo(oldqty) > 0)
        OBQuery<EscmOrderSourceRef> ref = OBDal.getInstance().createQuery(EscmOrderSourceRef.class,
            " as e where e.salesOrderLine.id=:orderLnId and e.requisitionLine is null");
        ref.setNamedParameter("orderLnId", objOrderLine.getId());
        log.debug("list:" + ref.list().size());
        if (ref.list().size() > 0) {
          EscmOrderSourceRef reference = ref.list().get(0);
          reference.setReservedQuantity(newqty);
          OBDal.getInstance().save(reference);
        }
        /*
         * OBQuery<Escmordershipment> shipment = OBDal.getInstance().createQuery(
         * Escmordershipment.class, " as e where e.salesOrderLine.id='" + objOrderLine.getId() +
         * "' "); log.debug("Shipment list:" + shipment.list().size()); if (shipment.list().size()
         * == 1) { Escmordershipment shipReference = shipment.list().get(0);
         * shipReference.setMovementQuantity(newqty); OBDal.getInstance().save(shipReference); }
         */
      }
      log.debug("srcrefqty:" + srcrefqty);
      /*
       * if (event.getCurrentState(product) != null && event.getPreviousState(product) != null) { if
       * (!event.getCurrentState(product).equals(event.getPreviousState(product))) { for
       * (Escmordershipment objShipment : objOrderLine.getEscmOrdershipmentList()) {
       * objShipment.setProduct(objOrderLine.getProduct());
       * objShipment.setDescription(objOrderLine.getProduct().getName());
       * OBDal.getInstance().save(objShipment); } } }
       */
      boolean canEdit = true;
      if ((event.getCurrentState(product) == null && event.getPreviousState(product) != null)
          || event.getCurrentState(product) != null && event.getPreviousState(product) == null) {
        if (objOrderLine.getEscmOldOrderline() != null) {
          canEdit = false;
        }
      } else if (event.getCurrentState(product) != null && event.getPreviousState(product) != null
          && !event.getCurrentState(product).equals(event.getPreviousState(product))) {
        if (objOrderLine.getEscmOldOrderline() != null) {
          canEdit = false;
        }
      }

      if ((event.getCurrentState(description) == null
          && event.getPreviousState(description) != null)
          || event.getCurrentState(description) != null
              && event.getPreviousState(description) == null) {
        if (objOrderLine.getEscmOldOrderline() != null) {
          canEdit = false;
        }
      } else if (event.getCurrentState(description) != null
          && event.getPreviousState(description) != null
          && !event.getCurrentState(description).equals(event.getPreviousState(description))) {
        if (objOrderLine.getEscmOldOrderline() != null) {
          canEdit = false;
        }
      }

      if (!canEdit) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_CannotChangeItem"));
      }

      // Get Line Percent Lookup Id
      String percentageChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
          "02");
      String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "01");
      // Get Line amount Lookup Id
      String amtChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP", "01");

      /**
       * if old tax amt and new tax amt changed then setting flag if "istaxchanges=true" to avoid
       * the calculation of change factor once again directely we can subtract the old tax amount
       * into line net amt and add new tax amount in to line net amt
       */
      if (event.getCurrentState(taxAmount) != null && newTaxAmt.compareTo(oldtaxAmt) != 0) {
        isTaxChanges = true;
      }
      /**
       * if we change the changeFactor/qty/unitPrice/taxAmount then have to calculate the line net
       * amount once again based on change factor and setting falg of "isPoFactorChanges=true " to
       * avoid the tax calculation once again
       **/
      if (((event.getCurrentState(poLinePOChangeValue) != null && newChangeValue != null
          && oldChangeValue != null && newChangeValue.compareTo(oldChangeValue) != 0)
          || (event.getCurrentState(poLinePOChangeFact) != null
              && !event.getCurrentState(poLinePOChangeFact)
                  .equals(event.getPreviousState((poLinePOChangeFact))))
          || (event.getCurrentState(poLinePOChangetype) != null
              && !event.getCurrentState(poLinePOChangetype)
                  .equals(event.getPreviousState((poLinePOChangetype))))
          || (event.getCurrentState(qty) != null
              && !event.getCurrentState(qty).equals(event.getPreviousState((qty))))
          || (event.getCurrentState(unitprice) != null
              && !event.getCurrentState(unitprice).equals(event.getPreviousState((unitprice))))
          || (event.getCurrentState(initialUnitPrice) != null
              && !event.getCurrentState(initialUnitPrice)
                  .equals(event.getPreviousState((initialUnitPrice)))))
          && !isTaxChanges) {
        // if(!objOrderLine.getSalesOrder().getDocumentStatus().equals("CO") &&
        // !objOrderLine.getSalesOrder().getEscmAppstatus().equals("ESCM_AP")&& !isTaxChanges) {
        /** setting isPoFactorChanges flag as true **/
        isPoFactorChanges = true;

        /**
         * if qty or unit price changed then setting isQtyOrUnitPriceChanges flag as true , to avoid
         * the tax calculation in the time of inclusive tax. exclusive only have to calculate the
         * tax based on changing unit price and qty
         **/
        if ((event.getCurrentState(qty) != null
            && !event.getCurrentState(qty).equals(event.getPreviousState((qty))))
            || (event.getCurrentState(unitprice) != null
                && newUnitPrice.compareTo(oldUnitPrice) != 0)) {
          isQtyOrUnitPriceChanges = true;
        }
        /** check change type is not null **/
        if (objOrderLine.getEscmPoChangeType() != null
            && objOrderLine.getEscmPoChangeValue() != null) {

          changeValue = objOrderLine.getEscmPoChangeValue();
          remAmt = POContractSummaryTotPOChangeDAO.remainingAmtLines(objOrderLine);

          /** check change percentage is more than 100 % **/
          if (objOrderLine.getEscmPoChangeType().getId().equals(percentageChangeType)
              && objOrderLine.getEscmPoChangeValue().compareTo(new BigDecimal(100)) > 0) {

            throw new OBException(OBMessageUtils.messageBD("ESCM_POChangeValueNotGreater"));

          }

          /** check change amount value should not be lesser than gross amount **/
          else if ((objOrderLine.getEscmPoChangeFactor() != null
              && objOrderLine.getEscmPoChangeFactor().getId().equals(decFactId))) {

            BigDecimal lineAmt = objOrderLine.getEscmLineTotalUpdated();

            lineAmt = remAmt;
            if (objOrderLine.getEscmPoChangeType() != null
                && objOrderLine.getEscmPoChangeType().getId().equals(amtChangeType)) {
              changeValue = objOrderLine.getEscmPoChangeValue();
            } else if (objOrderLine.getEscmPoChangeType() != null
                && objOrderLine.getEscmPoChangeType().getId().equals(percentageChangeType)) {
              changeValue = lineAmt
                  .multiply((objOrderLine.getEscmPoChangeValue()).divide(new BigDecimal("100")));
            }

            if (remAmt.compareTo(changeValue) < 0) {
              if (remAmt.compareTo(objOrderLine.getEscmLineTotalUpdated()) != 0) {
                String message = OBMessageUtils.messageBD("Escm_PoRemAmtDelivered");
                message = message.replace("%", remAmt.toString());
                throw new OBException(message);
              } else {
                throw new OBException(OBMessageUtils.messageBD("ESCM_POChangeValCantBeApplied"));
              }
            }
          }
          /** Calculate and update Net Line Amount from change type and change factor **/
          if (objOrderLine.getEscmPoChangeFactor() != null) {

            /** calcualte the line net amount based on the change factor **/
            /*
             * Task No.Tax BigDecimal lnPriceUpdatedAmt =
             * POContractSummaryTotPOChangeDAO.calculateLineUpdatedAmt(
             * objOrderLine.getEscmPoChangeType().getId(),
             * objOrderLine.getEscmPoChangeFactor().getId(), changeValue,
             * objOrderLine.getOrderedQuantity().multiply(objOrderLine.getUnitPrice()));
             */

            /**
             * if qty and unit price not changed or tax is exclusive then calculate tax amount ,
             * unit price & gross price based on tax
             **/
            /*
             * Task No: if ((isQtyOrUnitPriceChanges &&
             * objOrderLine.getSalesOrder().getEscmTaxMethod() != null &&
             * !objOrderLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax() &&
             * objOrderLine.getSalesOrder().isEscmIstax()) ||
             * (objOrderLine.getSalesOrder().getEscmTaxMethod() != null &&
             * objOrderLine.getSalesOrder().isEscmIstax())) {
             */

            JSONObject result = POContractSummaryDAO.getTaxandChangeValue(true,
                objOrderLine.getSalesOrder(), objOrderLine);
            if (result.length() > 0) {
              if (result.has("unitPriceAfterChange")) {
                event.setCurrentState(unitPriceAfterChange,
                    new BigDecimal(result.getString("unitPriceAfterChange")));
              }
              if (result.has("lineNetAmt")) {
                event.setCurrentState(poLineUpdatedAmt,
                    new BigDecimal(result.getString("lineNetAmt")));
              }
            }
            if (objOrderLine.getSalesOrder().isEscmIstax()) {

              /*
               * Task No: if (objOrderLine.getSalesOrder().getEscmTaxMethod() != null &&
               * objOrderLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) {
               */

              if (result.length() > 0) {
                if (result.has("unitPriceAfterChange")) {
                  event.setCurrentState(unitPriceAfterChange,
                      new BigDecimal(result.getString("unitPriceAfterChange")));
                }
                if (result.has("lineNetAmt")) {
                  event.setCurrentState(poLineUpdatedAmt,
                      new BigDecimal(result.getString("lineNetAmt")));
                }

                if (result.has("lineTaxAmt")) {
                  event.setCurrentState(taxAmount, new BigDecimal(result.getString("lineTaxAmt")));
                }
                if (result.has("negUnitPrice")) {
                  event.setCurrentState(unitprice,
                      new BigDecimal(result.getString("negUnitPrice")));
                }
                if (result.has("initialUnitPrice")) {
                  event.setCurrentState(initialUnitPrice,
                      new BigDecimal(result.getString("initialUnitPrice")));
                }
                if (result.has("roundOfTaxDiff")) {
                  event.setCurrentState(roundOfdiffdueTax,
                      new BigDecimal(result.getString("roundOfTaxDiff")));
                }
              }
              /*
               * Task No. } else { JSONObject taxObject =
               * POContractSummaryTotPOChangeDAO.calculateTax( objOrderLine.getOrderedQuantity(),
               * objOrderLine.getUnitPrice(), objOrderLine.getEscmPoChangeType().getId(),
               * changeValue, objOrderLine.getEscmPoChangeFactor().getId(),
               * objOrderLine.getEscmLineTaxamt(), objOrderLine.getSalesOrder().getId());
               * 
               * if (taxObject.length() > 0) { if (taxObject.has("lineNetAmt")) {
               * event.setCurrentState(poLineUpdatedAmt, new
               * BigDecimal(taxObject.getString("lineNetAmt"))); } if (taxObject.has("taxAmount")) {
               * event.setCurrentState(taxAmount, new BigDecimal(taxObject.getString("taxAmount")));
               * }
               * 
               * if (taxObject.has("calGrossPrice")) { event.setCurrentState(grosslineTotal, new
               * BigDecimal(taxObject.getString("calGrossPrice"))); } if
               * (taxObject.has("calUnitPrice")) { event.setCurrentState(unitprice, new
               * BigDecimal(taxObject.getString("calUnitPrice"))); } } else {
               * event.setCurrentState(poLineUpdatedAmt, lnPriceUpdatedAmt); } }
               */
            }
            /**
             * if qty or unit price changed then calculate line net amount and add existing tax
             * amount with line net amount
             **/
            else {
              if (objOrderLine.getSalesOrder().isEscmCalculateTaxlines() != null
                  && !objOrderLine.getSalesOrder().isEscmCalculateTaxlines()) {
                /*
                 * Task No. event.setCurrentState(poLineUpdatedAmt,
                 * lnPriceUpdatedAmt.add(objOrderLine.getEscmLineTaxamt()));
                 */
                event.setCurrentState(grosslineTotal,
                    objOrderLine.getOrderedQuantity().multiply(objOrderLine.getUnitPrice()));
              }
            }
          }
        }
        /**
         * if change factor removed then add tax amount into gross net amount and update the line
         * net amount
         **/
        else {
          if (objOrderLine.getSalesOrder().getEscmTaxMethod() != null
              && objOrderLine.getSalesOrder().isEscmIstax()) {
            grossNetAmt = objOrderLine.getEscmLineTotalUpdated()
                .add(objOrderLine.getEscmLineTaxamt());
          } else {
            grossNetAmt = objOrderLine.getEscmLineTotalUpdated();
          }
          event.setCurrentState(poLineUpdatedAmt, grossNetAmt);
        }
      }

      /**
       * if change factor not applied then add tax amount into gross net amount and update the line
       * net amount
       **/
      if ((objOrderLine.getEscmPoChangeType() == null
          || (objOrderLine.getEscmPoChangeValue().compareTo(BigDecimal.ZERO) == 0))
          && !isTaxChanges) {
        isPoFactorChanges = true;
        event.setCurrentState(poLinePOChangeValue, BigDecimal.ZERO);
        event.setCurrentState(poLinePOChangeFact, null);
        event.setCurrentState(isPoChangeCal, false);
        event.setCurrentState(unitPriceAfterChange, BigDecimal.ZERO);
        if (objOrderLine.getSalesOrder().getEscmTaxMethod() != null
            && objOrderLine.getSalesOrder().isEscmIstax()) {
          /*
           * Task No. && !objOrderLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) {
           * JSONObject taxObject = POContractSummaryTotPOChangeDAO.calculateTax(
           * objOrderLine.getOrderedQuantity(), objOrderLine.getUnitPrice(), null, BigDecimal.ZERO,
           * null, objOrderLine.getEscmLineTaxamt(), objOrderLine.getSalesOrder().getId()); if
           * (taxObject.length() > 0) { if (taxObject.has("lineNetAmt")) {
           * event.setCurrentState(poLineUpdatedAmt, new
           * BigDecimal(taxObject.getString("lineNetAmt"))); } if (taxObject.has("taxAmount")) {
           * event.setCurrentState(taxAmount, new BigDecimal(taxObject.getString("taxAmount"))); } }
           * } else {
           */

          grossNetAmt = objOrderLine.getEscmLineTotalUpdated()
              .add(objOrderLine.getEscmLineTaxamt());
          event.setCurrentState(poLineUpdatedAmt, grossNetAmt);

          JSONObject result = POContractSummaryDAO.getTaxandChangeValue(false,
              objOrderLine.getSalesOrder(), objOrderLine);
          if (result.length() > 0) {
            if (result.has("unitPriceAfterChange")) {
              event.setCurrentState(unitPriceAfterChange,
                  new BigDecimal(result.getString("unitPriceAfterChange")));
            }
            if (result.has("lineNetAmt")) {
              event.setCurrentState(poLineUpdatedAmt,
                  new BigDecimal(result.getString("lineNetAmt")));
            }

            if (result.has("lineTaxAmt")) {
              event.setCurrentState(taxAmount, new BigDecimal(result.getString("lineTaxAmt")));
            }
            if (result.has("negUnitPrice")) {
              event.setCurrentState(unitprice, new BigDecimal(result.getString("negUnitPrice")));
            }
            if (result.has("initialUnitPrice")) {
              event.setCurrentState(initialUnitPrice,
                  new BigDecimal(result.getString("initialUnitPrice")));
            }
            if (result.has("roundOfTaxDiff")) {
              event.setCurrentState(roundOfdiffdueTax,
                  new BigDecimal(result.getString("roundOfTaxDiff")));
            }
          }
        }
      }
      /**
       * if tax amount is changed then subtract the old tax amount into line net amt and add new tax
       * amount in to line net amt
       */
      if (event.getCurrentState(taxAmount) != null
          && !event.getCurrentState(taxAmount).equals(event.getPreviousState((taxAmount)))
          && !isPoFactorChanges && objOrderLine.getSalesOrder().isEscmCalculateTaxlines() != null
          && !objOrderLine.getSalesOrder().isEscmCalculateTaxlines()) {
        BigDecimal lnPriceUpdatedAmt = BigDecimal.ZERO;

        /** setting istaxchange flag as true **/
        isTaxChanges = true;

        /** calcualte the line net amount based on tax amount changes **/
        if (objOrderLine.getSalesOrder().isEscmIstax()) {
          lnPriceUpdatedAmt = objOrderLine.getLineNetAmount().subtract(oldtaxAmt)
              .add(objOrderLine.getEscmLineTaxamt());
          event.setCurrentState(poLineUpdatedAmt, lnPriceUpdatedAmt);
        }

      }

      if (objOrderLine.getEscmPoChangeValue() != null && objOrderLine.getEscmPoChangeType() != null
          && objOrderLine.getEscmPoChangeFactor() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangefacman"));
      } else if (objOrderLine.getEscmPoChangeType() != null
          && objOrderLine.getEscmPoChangeValue() == null
          && objOrderLine.getEscmPoChangeFactor() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangefacman"));
      }
      if (objOrderLine.getEscmLineTaxamt() != null
          && objOrderLine.getEscmLineTaxamt().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_OrdLineNoNeg"));
      }
      // If purchase agreement is selected then business partner & uniquecode is mandatory
      if (objOrderLine.getSalesOrder().isEscmIspurchaseagreement()
          && objOrderLine.getSalesOrder().getEscmOrdertype().equals("PUR_AG")) {
        if (objOrderLine.getBusinessPartner() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Supplier_Mandatory"));
        }
      }

      if (objOrderLine.getSalesOrder().isEscmIspurchaseagreement()
          && objOrderLine.getSalesOrder().getEscmOrdertype().equals("PUR_AG")) {
        if (!event.getCurrentState(poLineUpdatedAmt)
            .equals(event.getPreviousState(poLineUpdatedAmt))) {
          BigDecimal maxRelease = objOrderLine.getSalesOrder().getOrderLineList().stream()
              .filter(a -> !a.getId().equals(objOrderLine.getId())).map(x -> x.getLineNetAmount())
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .add((BigDecimal) event.getCurrentState(poLineUpdatedAmt));
          objOrderLine.getSalesOrder().setEscmMaxRelease(maxRelease);
        }
      }

    } catch (

    OBException e) {
      log.error("exception while updating PO and Contract OrderLine", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating PO and Contract OrderLine", e);
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
      OrderLine objOrderLine = (OrderLine) event.getTargetInstance();
      String objOrderId = "";
      if (objOrderLine.getSalesOrder() != null) {
        objOrderId = objOrderLine.getSalesOrder().getId();
      }
      Order objOrder = OBDal.getInstance().get(Order.class, objOrderId);
      if (objOrder != null) {
        if (objOrder.getDocumentStatus().equals("DR")) {
          List<OrderLine> orderLineList = objOrder.getOrderLineList();
          int listSize = orderLineList.size();
          System.out.println("listSize:" + listSize);
          if (listSize == 1) {
            objOrder.setEscmMaintenanceProject(null);
            objOrder.setEscmMaintenanceCntrctNo(null);
          }
        }
      }

      if (objOrder != null && objOrder.getEscmTotPoChangeType() != null
          && objOrderLine.getEscmPoChangeType() != null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangeTypDel"));
      }

      if (objOrder != null && objOrder.getEscmOldOrder() == null) {
        if (objOrder != null) {
          List<OrderLine> orderLineList = objOrder.getOrderLineList();
          int listSize = orderLineList.size();
          if ((listSize == 1 || (listSize == 2 && objOrderLine.getEscmParentline() != null))
              && (objOrder.isEscmAddproposal() || objOrder.isEscmAddrequisition())) {
            objOrder.setEscmAddproposal(false);
            objOrder.setEscmAddrequisition(false);
            objOrder.setEfinBudgetManencum(null);
            OBDal.getInstance().save(objOrder);
          }

          if (listSize == 1 || (listSize == 2 && objOrderLine.getEscmParentline() != null)) {
            if (objOrder.getDocumentStatus().equals("DR")
                && objOrder.getEscmProposalmgmt() != null) {
              objOrder.getEscmProposalmgmt().setDocumentNo(null);
              OBDal.getInstance().save(objOrder);

              // Updating the PO reference in PEE(Proposal Attribute)
              // Fetching the PEE irrespective of Proposal Version
              OBQuery<EscmProposalAttribute> proposalAttr = OBDal.getInstance().createQuery(
                  EscmProposalAttribute.class,
                  " as a  join a.escmProposalevlEvent b where b.status='CO' and a.escmProposalmgmt.proposalno= :proposalID ");
              proposalAttr.setNamedParameter("proposalID",
                  objOrder.getEscmProposalmgmt().getProposalno());
              List<EscmProposalAttribute> proposalAttrList = proposalAttr.list();
              if (proposalAttrList.size() > 0) {
                EscmProposalAttribute proposalAttrObj = proposalAttrList.get(0);
                proposalAttrObj.setOrder(null);
                OBDal.getInstance().save(proposalAttrObj);
              }
            }
          }
          // objOrder.getOrderLineList().remove(objOrderLine);

        }
        List<EscmOrderSourceRef> reflist = objOrderLine.getEscmOrdersourceRefList();
        // List<Escmordershipment> shipmentList = objOrderLine.getEscmOrdershipmentList();
        log.debug("reflist:" + reflist.size());
        for (EscmOrderSourceRef ref : reflist) {
          if (ref.getRequisitionLine() != null) {
            RequisitionLine line = ref.getRequisitionLine();
            line.setUpdated(new java.util.Date());
            line.setUpdatedBy(OBContext.getOBContext().getUser());
            line.setEscmPoQty(line.getEscmPoQty().subtract(ref.getReservedQuantity()));
            // if (line.isEscmIssummary()) {
            // line.setEscmPoQty(BigDecimal.ZERO);
            // }
            OBDal.getInstance().save(line);
            if (line.getEscmParentlineno() != null) {
              POContracctLineEventDAO.updatePoQtyForParent(line);
            }
          }

          ref.setReservedQuantity(BigDecimal.ZERO);
          OBDal.getInstance().save(ref);

        }
        log.debug("reflist11:" + reflist.size());
        for (EscmOrderSourceRef ref1 : reflist) {
          OBDal.getInstance().remove(ref1);
        }

        /*
         * for (Escmordershipment shipments : shipmentList) { OBDal.getInstance().remove(shipments);
         * }
         */

        // in order having proposal lines then chk any bank guarantee related with this order . if
        // there then dont allow to delete.
        if (objOrderLine.getEscmProposalmgmt() != null) {
          if (objOrderLine.getSalesOrder().getEscmBankguaranteeDetailList().size() > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_BefDelPropDelBG"));
          }
        }
      }
      if (objOrderLine.getEscmOldOrderline() != null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_CannotChangeItem"));
      }
      // Check and Set PO Change type and Change value if Total PO change type is Line Parent
      // Percentage
      // Get Line Parent Percent
      // String lineParentPercentChangeType = POContractSummaryTotPOChangeDAO
      // .getPOChangeLookUpId("TPOCHGTYP", "03");
      // if (objOrderLine.getSalesOrder().getEscmTotPoChangeType().getId()
      // .equals(lineParentPercentChangeType)) {
      // String poChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
      // "02");
      // BigDecimal poChangeValue = objOrderLine.getSalesOrder().getEscmTotPoChangeValue();
      // event.setCurrentState(poChangeTypeProp, poChangeType);
      // event.setCurrentState(poChangeValueProp, poChangeValue);
      // POContractSummaryTotPOChangeDAO.updateChildLines(objOrderLine.getSalesOrder().getId());
      // BigDecimal poUpdatedAmt = POContractSummaryTotPOChangeDAO
      // .getTopLevelParentAmt(objOrderLine.getSalesOrder().getId());
      // objOrderLine.getSalesOrder().setEscmTotPoUpdatedAmt(poUpdatedAmt);
      // }

      // Getting deleted object would be re-saved by cascade error
      if (objOrder != null) {
        objOrder.getOrderLineList().remove(objOrderLine);
      }

      if (objOrderLine.getSalesOrder().isEscmIspurchaseagreement()
          && objOrderLine.getSalesOrder().getEscmOrdertype().equals("PUR_AG")) {
        BigDecimal maxRelease = objOrderLine.getSalesOrder().getOrderLineList().stream()
            .filter(a -> !a.isEscmIssummarylevel()).map(x -> x.getLineNetAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        objOrderLine.getSalesOrder().setEscmMaxRelease(maxRelease);
      }
    } catch (OBException e) {
      log.error("exception while deleting OrderLineEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while deleting OrderLineEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings({ "unchecked" })
  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      OrderLine objOrderLine = (OrderLine) event.getTargetInstance();
      BigDecimal grossNetAmt = BigDecimal.ZERO;
      final Property poLineUpdatedAmt = entities[0].getProperty(OrderLine.PROPERTY_LINENETAMOUNT);
      final Property poLinePOChangeValue = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMPOCHANGEVALUE);
      final Property poLinePOChangeFact = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMPOCHANGEFACTOR);
      final Property grosslineTotal = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMLINETOTALUPDATED);
      final Property unitprice = entities[0].getProperty(OrderLine.PROPERTY_UNITPRICE);
      final Property taxAmount = entities[0].getProperty(OrderLine.PROPERTY_ESCMLINETAXAMT);
      final Property initialUnitPrice = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMINITIALUNITPRICE);
      final Property roundOfdiffdueTax = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMROUNDDIFFTAX);
      final Property unitPriceAfterChange = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMUNITPRICEAFTERCHAG);
      final Property uniqueCode = entities[0].getProperty(OrderLine.PROPERTY_EFINUNIQUECODE);
      final Property requisition_line = entities[0]
          .getProperty(OrderLine.PROPERTY_EFINMREQUISITIONLINE);
      final Property proposal_line = entities[0]
          .getProperty(OrderLine.PROPERTY_ESCMPROPOSALMGMTLINE);

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      Order objOrder = objOrderLine.getSalesOrder();

      String preferenceValue = "N";

      // Check Budget Controller Preference
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true, clientId,
            objOrder.getOrganization().getId(), userId, roldId, PO_Window_ID, "N");
        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;

      } catch (PropertyException e) {
        preferenceValue = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      if (!preferenceValue.equals("Y") && objOrder.getEutForward() != null) {// check for
        // temporary
        // preference
        String requester_user_id = objOrder.getEutForward().getUserContact().getId();
        String requester_role_id = objOrder.getEutForward().getRole().getId();
        preferenceValue = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", roldId, userId, clientId, objOrder.getOrganization().getId(),
            PO_Window_ID, requester_user_id, requester_role_id);
      }
      // if Budget Controller Preference not enabled then the user will not able to change the
      // values of
      // Budget related fields
      if (!preferenceValue.equals("Y") && event.getCurrentState(proposal_line) == null
          && event.getCurrentState(requisition_line) == null
          && objOrder.getEscmOldOrder() == null) {
        if (event.getCurrentState(uniqueCode) != null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      // Order order = OBDal.getInstance().get(Order.class, objOrderLine.getSalesOrder().getId());
      // if (order.isEscmAddproposal() && order.getOrderLineList().size() >= 1) {
      // throw new OBException(OBMessageUtils.messageBD("Escm_InsertNewProposal"));
      // }

      if (objOrderLine.getEscmLineTaxamt() != null
          && objOrderLine.getEscmLineTaxamt().compareTo(BigDecimal.ZERO) < 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_OrdLineNoNeg"));
      }
      if (objOrderLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) == 0) {
        if (objOrderLine.getEscmOldOrderline() == null) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_QtyCantBeZeroForNewLine"));
        }

        // if (objOrderLine.getSalesOrder().getEscmOldOrder() == null) {
        if (objOrderLine.isEscmIsmanual()) {
          final Entity orderLineEntity = ModelProvider.getInstance()
              .getEntity(OrderLine.ENTITY_NAME);

          final EscmOrderSourceRef srcref = OBProvider.getInstance().get(EscmOrderSourceRef.class);
          srcref.setSalesOrderLine(objOrderLine);
          srcref.setReservedQuantity(objOrderLine.getOrderedQuantity());
          srcref.setLineNo(new Long(10));

          /*
           * final Escmordershipment orderShipement = OBProvider.getInstance()
           * .get(Escmordershipment.class); orderShipement.setSalesOrderLine(objOrderLine);
           * orderShipement.setProduct(objOrderLine.getProduct());
           * orderShipement.setMovementQuantity(objOrderLine.getOrderedQuantity());
           * orderShipement.setUOM(objOrderLine.getUOM());
           * orderShipement.setDescription(objOrderLine.getEscmProdescription());
           * orderShipement.setNeedByDate(new Date()); orderShipement.setLineNo(new Long(10));
           */

          final Property refproperty = orderLineEntity
              .getProperty(OrderLine.PROPERTY_ESCMORDERSOURCEREFLIST);
          final List<Object> srcls = (List<Object>) event.getCurrentState(refproperty);
          srcls.add(srcref);

          // final Property shipmentRef = orderLineEntity
          // .getProperty(OrderLine.PROPERTY_ESCMORDERSHIPMENTLIST);
          /*
           * final List<Object> shipmentList = ((List<Object>) event.getCurrentState(shipmentRef));
           * shipmentList.add(orderShipement);
           */
        }
        // }
      }
      // // Check and Set PO Change type and Change value if Total PO change type is Line Parent
      // // Percentage
      // if (objOrderLine.getSalesOrder().getEscmTotPoChangeType() != null) {
      // // Get Line Parent Percent
      // String lineParentPercentChangeType = POContractSummaryTotPOChangeDAO
      // .getPOChangeLookUpId("TPOCHGTYP", "03");
      // if (objOrderLine.getSalesOrder().getEscmTotPoChangeType().getId()
      // .equals(lineParentPercentChangeType)) {
      // String poChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
      // "02");
      // ESCMDefLookupsTypeLn poChangeTypeLkup = Utility.getObject(ESCMDefLookupsTypeLn.class,
      // poChangeType);
      // BigDecimal poChangeValue = objOrderLine.getSalesOrder().getEscmTotPoChangeValue();
      // event.setCurrentState(poChangeTypeProp, poChangeTypeLkup);
      // event.setCurrentState(poChangeValueProp, poChangeValue);
      // POContractSummaryTotPOChangeDAO.updateChildLines(objOrderLine.getSalesOrder().getId());
      // BigDecimal poUpdatedAmt = POContractSummaryTotPOChangeDAO
      // .getTopLevelParentAmt(objOrderLine.getSalesOrder().getId());
      // objOrderLine.getSalesOrder().setEscmTotPoUpdatedAmt(poUpdatedAmt);
      // }
      // }
      // Get Line Percent Lookup Id
      String percentageChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP",
          "02");
      String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGFACT", "01");
      // Get Line amount Lookup Id
      String amtChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCHGTYP", "01");
      if (objOrderLine.getEscmPoChangeType() != null
          && objOrderLine.getEscmPoChangeValue() != null) {

        if (objOrderLine.getEscmPoChangeType().getId().equals(percentageChangeType)
            && objOrderLine.getEscmPoChangeValue().compareTo(new BigDecimal(100)) > 0) {

          throw new OBException(OBMessageUtils.messageBD("ESCM_POChangeValueNotGreater"));
        }

        /** check change amount value should not be lesser than gross amount **/
        else if ((objOrderLine.getEscmPoChangeFactor() != null
            && objOrderLine.getEscmPoChangeFactor().getId().equals(decFactId))) {

          BigDecimal changeValue = BigDecimal.ZERO;
          BigDecimal lineAmt = objOrderLine.getEscmLineTotalUpdated();

          if (objOrderLine.getEscmPoChangeType() != null
              && objOrderLine.getEscmPoChangeType().getId().equals(amtChangeType)) {
            changeValue = objOrderLine.getEscmPoChangeValue();
          } else if (objOrderLine.getEscmPoChangeType() != null
              && objOrderLine.getEscmPoChangeType().getId().equals(percentageChangeType)) {
            changeValue = lineAmt
                .multiply((objOrderLine.getEscmPoChangeValue()).divide(new BigDecimal("100")));
          }

          if (lineAmt.compareTo(changeValue) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_POChangeValCantBeApplied"));
          }
        }
        // Calculate and update Net Line Amount from change type and change factor
        if (objOrderLine.getEscmPoChangeFactor() != null) {

          JSONObject result = POContractSummaryDAO.getTaxandChangeValue(true,
              objOrderLine.getSalesOrder(), objOrderLine);
          if (result.length() > 0) {
            if (result.has("unitPriceAfterChange")) {
              event.setCurrentState(unitPriceAfterChange,
                  new BigDecimal(result.getString("unitPriceAfterChange")));
            }
            if (result.has("lineNetAmt")) {
              event.setCurrentState(poLineUpdatedAmt,
                  new BigDecimal(result.getString("lineNetAmt")));
            }
          }

          if (objOrderLine.getSalesOrder().getEscmTaxMethod() != null
              && objOrderLine.getSalesOrder().isEscmIstax()) {
            /*
             * Task No.if (objOrderLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) {
             */
            if (result.length() > 0) {
              if (result.has("unitPriceAfterChange")) {
                event.setCurrentState(unitPriceAfterChange,
                    new BigDecimal(result.getString("unitPriceAfterChange")));
              }
              if (result.has("lineNetAmt")) {
                event.setCurrentState(poLineUpdatedAmt,
                    new BigDecimal(result.getString("lineNetAmt")));
              }

              if (result.has("lineTaxAmt")) {
                event.setCurrentState(taxAmount, new BigDecimal(result.getString("lineTaxAmt")));
              }
              if (result.has("negUnitPrice")) {
                event.setCurrentState(unitprice, new BigDecimal(result.getString("negUnitPrice")));
              }
              if (result.has("initialUnitPrice")) {
                event.setCurrentState(initialUnitPrice,
                    new BigDecimal(result.getString("initialUnitPrice")));
              }
              if (result.has("roundOfTaxDiff")) {
                event.setCurrentState(roundOfdiffdueTax,
                    new BigDecimal(result.getString("roundOfTaxDiff")));
              }
            }
            /*
             * Task No. } else { BigDecimal lnPriceUpdatedAmt = POContractSummaryTotPOChangeDAO
             * .calculateLineUpdatedAmt(objOrderLine.getEscmPoChangeType().getId(),
             * objOrderLine.getEscmPoChangeFactor().getId(), objOrderLine.getEscmPoChangeValue(),
             * objOrderLine.getEscmLineTotalUpdated());
             * 
             * JSONObject taxObject = POContractSummaryTotPOChangeDAO.calculateTax(
             * objOrderLine.getOrderedQuantity(), objOrderLine.getUnitPrice(),
             * objOrderLine.getEscmPoChangeType().getId(), objOrderLine.getEscmPoChangeValue(),
             * objOrderLine.getEscmPoChangeFactor().getId(), objOrderLine.getEscmLineTaxamt(),
             * objOrderLine.getSalesOrder().getId());
             * 
             * if (taxObject.length() > 0) { if (taxObject.has("lineNetAmt")) {
             * event.setCurrentState(poLineUpdatedAmt, new
             * BigDecimal(taxObject.getString("lineNetAmt"))); } if (taxObject.has("taxAmount")) {
             * event.setCurrentState(taxAmount, new BigDecimal(taxObject.getString("taxAmount"))); }
             * if (taxObject.has("calGrossPrice")) { event.setCurrentState(grosslineTotal, new
             * BigDecimal(taxObject.getString("calGrossPrice"))); } if
             * (taxObject.has("calUnitPrice")) { event.setCurrentState(unitprice, new
             * BigDecimal(taxObject.getString("calUnitPrice"))); } } else {
             * event.setCurrentState(poLineUpdatedAmt, lnPriceUpdatedAmt); } }
             */
          }

        }
      }

      else {
        if (objOrderLine.getSalesOrder().getEscmTaxMethod() == null) {
          event.setCurrentState(poLineUpdatedAmt, objOrderLine.getEscmLineTotalUpdated());
        }
      }
      if (objOrderLine.getEscmPoChangeType() == null) {
        event.setCurrentState(poLinePOChangeValue, BigDecimal.ZERO);
        event.setCurrentState(poLinePOChangeFact, null);
        if (objOrderLine.getSalesOrder().isEscmCalculateTaxlines() != null
            && !objOrderLine.getSalesOrder().isEscmCalculateTaxlines()) {
          if (objOrderLine.getSalesOrder().getEscmTaxMethod() != null
              && objOrderLine.getSalesOrder().isEscmIstax()) {

            if (!objOrderLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) {
              JSONObject taxObject = POContractSummaryTotPOChangeDAO.calculateTax(
                  objOrderLine.getOrderedQuantity(), objOrderLine.getUnitPrice(), null,
                  BigDecimal.ZERO, null, objOrderLine.getEscmLineTaxamt(),
                  objOrderLine.getSalesOrder().getId());
              if (taxObject.length() > 0) {
                if (taxObject.has("lineNetAmt")) {
                  event.setCurrentState(poLineUpdatedAmt,
                      new BigDecimal(taxObject.getString("lineNetAmt")));
                }
                if (taxObject.has("taxAmount")) {
                  event.setCurrentState(taxAmount,
                      new BigDecimal(taxObject.getString("taxAmount")));
                }
              }
            } else {
              // grossNetAmt = objOrderLine.getEscmLineTotalUpdated()
              // .add(objOrderLine.getEscmLineTaxamt());
              // event.setCurrentState(poLineUpdatedAmt, grossNetAmt);
              JSONObject result = POContractSummaryDAO.getTaxandChangeValue(false,
                  objOrderLine.getSalesOrder(), objOrderLine);
              if (result.length() > 0) {
                if (result.has("unitPriceAfterChange")) {
                  event.setCurrentState(unitPriceAfterChange,
                      new BigDecimal(result.getString("unitPriceAfterChange")));
                }
                if (result.has("lineNetAmt")) {
                  event.setCurrentState(poLineUpdatedAmt,
                      new BigDecimal(result.getString("lineNetAmt")));
                }

                if (result.has("lineTaxAmt")) {
                  event.setCurrentState(taxAmount, new BigDecimal(result.getString("lineTaxAmt")));
                }
                if (result.has("negUnitPrice")) {
                  event.setCurrentState(unitprice,
                      new BigDecimal(result.getString("negUnitPrice")));
                }
                if (result.has("initialUnitPrice")) {
                  event.setCurrentState(initialUnitPrice,
                      new BigDecimal(result.getString("initialUnitPrice")));
                }
                if (result.has("roundOfTaxDiff")) {
                  event.setCurrentState(roundOfdiffdueTax,
                      new BigDecimal(result.getString("roundOfTaxDiff")));
                }
              }
            }
          }
        }
      }

      if (objOrderLine.getEscmPoChangeValue() != null && objOrderLine.getEscmPoChangeType() != null
          && objOrderLine.getEscmPoChangeFactor() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangefacman"));
      } else if (objOrderLine.getEscmPoChangeType() != null
          && objOrderLine.getEscmPoChangeValue() == null
          && objOrderLine.getEscmPoChangeFactor() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangefacman"));
      }
      // If purchase agreement is selected then business partner & uniquecode is mandatory
      if (objOrderLine.getSalesOrder().isEscmIspurchaseagreement()
          && objOrderLine.getSalesOrder().getEscmOrdertype().equals("PUR_AG")) {
        if (objOrderLine.getBusinessPartner() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Supplier_Mandatory"));
        }
      }
    } catch (OBException e) {
      log.error("exception while creating OrderLineEvent", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating OrderLineEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
