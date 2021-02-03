package sa.elm.ob.scm.event;

import java.math.BigDecimal;
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
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.common.plm.Product;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestHistory;
import sa.elm.ob.scm.MaterialIssueRequestLine;
import sa.elm.ob.scm.event.dao.MaterialIssueRequestEventDAO;

/**
 * 
 * @author Gopalakrishnan on 13/03/2017
 * 
 */
public class MaterialIRLineHandler extends EntityPersistenceEventObserver {
  /**
   * This Class was responsible for business events in Escm_Material_Reqln Table
   */
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(MaterialIssueRequestLine.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  private Logger log = Logger.getLogger(this.getClass());

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      String strRecentAction = "";

      MaterialIssueRequestLine objRequestLine = (MaterialIssueRequestLine) event
          .getTargetInstance();
      MaterialIssueRequest objRequest = objRequestLine.getEscmMaterialRequest();
      OBQuery<MaterialIssueRequestHistory> appQuery = OBDal.getInstance().createQuery(
          MaterialIssueRequestHistory.class,
          "as e where e.escmMaterialRequest.id=:mirID order by creationDate desc");
      appQuery.setNamedParameter("mirID", objRequest.getId());
      appQuery.setMaxResult(1);
      if (appQuery.list().size() > 0) {
        MaterialIssueRequestHistory objLastLine = appQuery.list().get(0);
        if (objLastLine.getRequestreqaction().equals("REJ")) {
          strRecentAction = "REJ";
        }
        if (objLastLine.getRequestreqaction().equals("REA")) {
          strRecentAction = "REA";
        }
        if (objLastLine.getRequestreqaction().equals("REV")) {
          strRecentAction = "REV";
        }
      }
      Boolean ispreference = false;
      /*
       * Boolean ispreference = Preferences.existsPreference("ESCM_LineManager", true, null, null,
       * null, OBContext.getOBContext().getRole().getId(), null);
       */
      String preferenceValue = "";
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_LineManager", true,
            OBContext.getOBContext().getCurrentClient().getId(),
            objRequestLine.getOrganization().getId(), OBContext.getOBContext().getUser().getId(),
            OBContext.getOBContext().getRole().getId(), "D8BA0A87790B4B67A86A8DF714525736");

      } catch (PropertyException e) {
      } catch (Exception e) {
        throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      }
      if (preferenceValue != null && preferenceValue.equals("Y"))
        ispreference = true;
      if (!ispreference) {
        if (objRequest.getAlertStatus().equals("ESCM_IP")) {
          // if not copied line then throw the error
          if (!objRequestLine.isCopy())
            throw new OBException(OBMessageUtils.messageBD("ESCM_IR_InProgress"));
        }
      }
      if (objRequest.getAlertStatus().equals("ESCM_TR")) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Approved"));
      }
      if (!ispreference) {
        if (objRequest.getEscmMaterialrequestHistList().size() > 0
            && (!strRecentAction.equals("REJ") && !strRecentAction.equals("REA")
                && !strRecentAction.equals("REV"))) {
          if (objRequestLine.getEscmMaterialRequest().getREQParent() == null) {
            // if not copied line then throw the error
            if (!objRequestLine.isCopy())
              throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Submitted"));
          }
        }
        if (objRequest.getREQParent() != null) {
          if (objRequestLine.getParentLineid() != null) {
            String reqLineId = objRequestLine.getParentLineid().getId();
            BigDecimal deliveredQnt = objRequestLine.getDeliveredQantity();
            MaterialIssueRequestLine parentLine = OBDal.getInstance()
                .get(MaterialIssueRequestLine.class, reqLineId);
            if (parentLine != null) {
              parentLine.setPendingQty(parentLine.getPendingQty().add(deliveredQnt));
            }
          }
          objRequest.getEscmMaterialReqlnList().remove(objRequestLine);
        }
      }

    } catch (OBException e) {
      log.error(" Exception while Deleting IssueRequest Line: ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while Deleting IssueRequest Line: ", e);
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
      boolean isPreference = false;
      OBContext.setAdminMode();
      String status = null, strQuery = null;
      Query query = null;
      BigDecimal stockOnhandQty = BigDecimal.ZERO;
      MaterialIssueRequestLine objRequestLine = (MaterialIssueRequestLine) event
          .getTargetInstance();
      final Property productId = entities[0].getProperty(MaterialIssueRequestLine.PROPERTY_PRODUCT);
      final Property deliveredQty = entities[0]
          .getProperty(MaterialIssueRequestLine.PROPERTY_DELIVEREDQANTITY);// getting current
                                                                           // entered value
      Object currentDeliveredQty = event.getCurrentState(deliveredQty);
      Object previousDeliveredQty = event.getPreviousState(deliveredQty);
      final Property isgeneric = entities[0]
          .getProperty(MaterialIssueRequestLine.PROPERTY_ISGENERIC);
      final Property genericProduct = entities[0]
          .getProperty(MaterialIssueRequestLine.PROPERTY_GENERICPRODUCT);

      try {
        String preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
            OBContext.getOBContext().getCurrentClient().getId(),
            OBContext.getOBContext().getCurrentOrganization().getId(),
            OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
            "D8BA0A87790B4B67A86A8DF714525736");
        if (preferenceValue != null && preferenceValue.equals("Y"))
          isPreference = true;
      } catch (PropertyException e) {
        isPreference = false;
      }

      // if (objRequestLine.getEscmMaterialRequest().isProcessNow()) {
      // throw new OBException(OBMessageUtils.messageBD("Escm_Processing"));
      // }

      Product product = OBDal.getInstance().get(Product.class, objRequestLine.getProduct().getId());

      // same product should not be in lines

      strQuery = " select m_product_id from m_substitute where substitute_id=? and ad_client_id=?";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, product.getId());
      query.setParameter(1, objRequestLine.getClient().getId());

      MaterialIssueRequest objRequest = objRequestLine.getEscmMaterialRequest();

      if (objRequestLine.getDeliveredQantity().compareTo(objRequestLine.getRequestedQty()) == 1) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Delivered_Qty(High)"));
      }
      if (objRequest.getAlertStatus().equals("DR") && !(objRequestLine.isMultiissuance())) {
        if (!product.isStocked() && !product.isPurchase()
            && product.getProductSubstituteList().size() > 0
            && product.getEscmStockType().getSearchKey().equals("CUS")) {
          event.setCurrentState(isgeneric, true);
          event.setCurrentState(genericProduct, objRequestLine.getProduct());
        } else {
          event.setCurrentState(isgeneric, false);
          event.setCurrentState(genericProduct, null);
        }
      }

      if (objRequest.isSiteissuereq() != null && !objRequest.isSiteissuereq()) {
        OBQuery<MaterialIssueRequestLine> reqLine = OBDal.getInstance().createQuery(
            MaterialIssueRequestLine.class,
            "escmMaterialRequest.id=:mirId and product.id=:prdID and id<>:reqLnId");
        reqLine.setNamedParameter("mirId", objRequestLine.getEscmMaterialRequest().getId());
        reqLine.setNamedParameter("prdID", product.getId());
        reqLine.setNamedParameter("reqLnId", objRequestLine.getId());

        if (reqLine.list().size() > 0) {
          if (!isPreference) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Material_DuplicateProduct"));
          } else if (isPreference) {
            if (!(!product.isPurchase() && !product.isStocked()))
              throw new OBException(OBMessageUtils.messageBD("Escm_Material_DuplicateProduct"));
          }
        }
      }
      if (objRequest.isSiteissuereq() != null && objRequest.isSiteissuereq()) {
        if (objRequestLine.getRequestedQty().compareTo(BigDecimal.ZERO) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Qty_Zero"));
        }
      }
      if (product.getEscmStockType() != null) {
        if (product.getEscmStockType().getSearchKey().equals("CUS")) {
          BigDecimal fractionalPart = objRequestLine.getRequestedQty().remainder(BigDecimal.ONE);
          if (fractionalPart.compareTo(BigDecimal.ZERO) == 1) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));
          }
          BigDecimal deliveredqty = objRequestLine.getDeliveredQantity().remainder(BigDecimal.ONE);
          if (deliveredqty.compareTo(BigDecimal.ZERO) == 1) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));
          }
        }
      }
      if (objRequestLine.getEscmMaterialRequest().getREQParent() != null) {

        if (objRequestLine.getParentLineid() != null) {
          MaterialIssueRequestLine parentLine = OBDal.getInstance()
              .get(MaterialIssueRequestLine.class, objRequestLine.getParentLineid().getId());
          if (parentLine != null) {
            if ((parentLine.getPendingQty()
                .subtract(new BigDecimal(currentDeliveredQty.toString())
                    .subtract(new BigDecimal(previousDeliveredQty.toString())))
                .compareTo(BigDecimal.ZERO)) >= 0) {
              parentLine.setPendingQty(
                  parentLine.getPendingQty().subtract(new BigDecimal(currentDeliveredQty.toString())
                      .subtract(new BigDecimal(previousDeliveredQty.toString()))));
              OBDal.getInstance().save(parentLine);
            } else {
              throw new OBException(OBMessageUtils.messageBD("ESCM_PENDINGQTY(LESS)")
                  .replace("@", parentLine.getPendingQty().toPlainString())
                  .replace("$", currentDeliveredQty.toString()));
            }
          }
        }
      }

      if (objRequest.getEscmMaterialReqlnList().size() > 0 && objRequestLine.isGeneric()
          && objRequestLine.getGenericProduct() != null && !objRequest.isSiteissuereq()) {
        String currentSearchKey = objRequestLine.getGenericProduct().getSearchKey();
        BigDecimal reqQty = objRequestLine.getRequestedQty();
        BigDecimal sumIssuedQty = BigDecimal.ZERO;

        for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
          if (line.isGeneric() && line.getGenericProduct() != null) {
            if (line.getGenericProduct().getSearchKey().equals(currentSearchKey)) {
              sumIssuedQty = sumIssuedQty.add(line.getDeliveredQantity());
            }
          }
        }

        if (sumIssuedQty.compareTo(reqQty) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Material_IssuedGreaterRequested"));
        }
      }
      if (event.getCurrentState(productId) != null
          && (!event.getCurrentState(productId).equals(event.getPreviousState(productId)))) {
        stockOnhandQty = MaterialIssueRequestEventDAO.chkNegStockOnHandQty(
            objRequestLine.getOrganization().getId(), objRequestLine.getProduct().getId());
        if (stockOnhandQty.compareTo(BigDecimal.ZERO) < 0) {
          status = OBMessageUtils.messageBD("ESCM_MaterialIssLineNegStock");
          status = status.replace("%", stockOnhandQty.toString());
          status = status.replace("@", objRequestLine.getProduct().getName());
          log.debug("status:" + status);
          throw new OBException(status);
        }
      }
    } catch (OBException e) {
      log.error(" Exception while updating IssueRequest Line:  ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while updating IssueRequest Line:  ", e);
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
      boolean isPreference = false;
      OBContext.setAdminMode();
      String status = null, str_gen_product_id = "";
      BigDecimal stockOnhandQty = BigDecimal.ZERO;
      MaterialIssueRequestLine objRequestLine = (MaterialIssueRequestLine) event
          .getTargetInstance();
      final Property isgeneric = entities[0]
          .getProperty(MaterialIssueRequestLine.PROPERTY_ISGENERIC);
      final Property genericProduct = entities[0]
          .getProperty(MaterialIssueRequestLine.PROPERTY_GENERICPRODUCT);
      Product product = OBDal.getInstance().get(Product.class, objRequestLine.getProduct().getId());
      MaterialIssueRequest objRequest = objRequestLine.getEscmMaterialRequest();
      // same product should not be in lines
      if (objRequestLine.getDeliveredQantity().compareTo(objRequestLine.getRequestedQty()) == 1) {
        throw new OBException(OBMessageUtils.messageBD("Escm_Delivered_Qty(High)"));
      }

      // After transacted status we should not allow user to edit the record

      if ("ESCM_TR".equals(objRequest.getAlertStatus())) {
        throw new OBException(OBMessageUtils.messageBD("Escm_cannotedittransacted"));
      }

      try {
        String preferenceValue = Preferences.getPreferenceValue("ESCM_WarehouseKeeper", true,
            OBContext.getOBContext().getCurrentClient().getId(),
            OBContext.getOBContext().getCurrentOrganization().getId(),
            OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
            "D8BA0A87790B4B67A86A8DF714525736");
        if (preferenceValue != null && preferenceValue.equals("Y"))
          isPreference = true;
      } catch (PropertyException e) {
        isPreference = false;
      }

      // check product is generic then update is generic product flag to "yes"
      String strQuery = null;
      Query query = null;
      strQuery = " select m_product_id from m_substitute where substitute_id=? and ad_client_id=?";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, product.getId());
      query.setParameter(1, objRequestLine.getClient().getId());

      @SuppressWarnings("unchecked")
      List<Object> substituteList = query.list();
      if (substituteList.size() > 0) {
        Object objGenProduct = substituteList.get(0);
        str_gen_product_id = (String) objGenProduct;
      }

      if (objRequest.getAlertStatus().equals("DR")
          || objRequest.getAlertStatus().equals("ESCM_IP")) {
        if (!product.isStocked() && !product.isPurchase()
            && product.getProductSubstituteList().size() > 0
            && product.getEscmStockType().getSearchKey().equals("CUS")) {
          event.setCurrentState(isgeneric, true);
          event.setCurrentState(genericProduct, objRequestLine.getProduct());
        } else if (substituteList.size() > 0) {
          event.setCurrentState(isgeneric, true);
          if (StringUtils.isNotEmpty(strQuery) && StringUtils.isNotBlank(str_gen_product_id)) {
            event.setCurrentState(genericProduct,
                OBDal.getInstance().get(Product.class, str_gen_product_id));
          }

        } else {
          event.setCurrentState(isgeneric, false);
          event.setCurrentState(genericProduct, null);
        }
      }

      // prevent user from creating more than 12 lines
      if (objRequest.getEscmMaterialReqlnList().size() >= 12) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Max_Line"));
      }

      if (objRequest.getEscmMaterialReqlnList().size() > 0 && objRequestLine.isGeneric()
          && objRequestLine.getGenericProduct() != null && !objRequest.isSiteissuereq()) {
        String currentSearchKey = objRequestLine.getGenericProduct().getSearchKey();
        BigDecimal reqQty = objRequestLine.getRequestedQty();
        BigDecimal sumIssuedQty = BigDecimal.ZERO;

        for (MaterialIssueRequestLine line : objRequest.getEscmMaterialReqlnList()) {
          if (line.isGeneric() && line.getGenericProduct() != null) {
            if (line.getGenericProduct().getSearchKey().equals(currentSearchKey)) {
              sumIssuedQty = sumIssuedQty.add(line.getDeliveredQantity());
            }
          }
        }

        if (sumIssuedQty.compareTo(reqQty) > 0) {
          throw new OBException(OBMessageUtils.messageBD("Escm_Material_IssuedGreaterRequested"));
        }
      }

      if (objRequest.isSiteissuereq() != null && !objRequest.isSiteissuereq()) {
        OBQuery<MaterialIssueRequestLine> reqLine = OBDal.getInstance().createQuery(
            MaterialIssueRequestLine.class, "escmMaterialRequest.id=:mirId and product.id=:prdID ");
        reqLine.setNamedParameter("mirId", objRequestLine.getEscmMaterialRequest().getId());
        reqLine.setNamedParameter("prdID", product.getId());

        if (reqLine.list().size() > 0) {
          if (!isPreference) {
            throw new OBException(OBMessageUtils.messageBD("Escm_Material_DuplicateProduct"));
          } else if (isPreference) {
            if (!(!product.isPurchase() && !product.isStocked()))
              throw new OBException(OBMessageUtils.messageBD("Escm_Material_DuplicateProduct"));
          }
        }
      }
      if (objRequest.isSiteissuereq() != null && objRequest.isSiteissuereq()) {
        if (objRequestLine.getRequestedQty().compareTo(BigDecimal.ZERO) <= 0) {
          throw new OBException(OBMessageUtils.messageBD("ESCM_IR_Qty_Zero"));
        }
      }
      if (product.getEscmStockType() != null) {
        if (product.getEscmStockType().getSearchKey().equals("CUS")) {
          BigDecimal fractionalPart = objRequestLine.getRequestedQty().remainder(BigDecimal.ONE);
          if (fractionalPart.compareTo(BigDecimal.ZERO) == 1) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));
          }
          BigDecimal deliveredqty = objRequestLine.getDeliveredQantity().remainder(BigDecimal.ONE);
          if (deliveredqty.compareTo(BigDecimal.ZERO) == 1) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));
          }
        }
      }
      if (objRequest.getREQParent() != null
          && objRequest.getEscmMaterialrequestHistList().size() > 0) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_CannotAddNewLines_child"));
      }
      if (objRequestLine.getProduct() != null) {
        stockOnhandQty = MaterialIssueRequestEventDAO.chkNegStockOnHandQty(
            objRequestLine.getOrganization().getId(), objRequestLine.getProduct().getId());
        if (stockOnhandQty.compareTo(BigDecimal.ZERO) < 0) {
          status = OBMessageUtils.messageBD("ESCM_MaterialIssLineNegStock");
          status = status.replace("%", stockOnhandQty.toString());
          status = status.replace("@", objRequestLine.getProduct().getName());
          log.debug("status:" + status);
          throw new OBException(status);
        }
      }
    } catch (OBException e) {
      log.error(" Exception while saving IssueRequest Line:  ", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error(" Exception while saving IssueRequest Line:  ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
