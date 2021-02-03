package sa.elm.ob.scm.webservice.approvedpo;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptLineDTO;
import sa.elm.ob.scm.webservice.exception.CreateReceiptException;

/**
 * This class is used Set POline Id based on Item Number or Item Description
 * 
 * @author DivyaPrakash JS
 */

public class SetPOLineIdDAO {

  private static final Logger log4j = LoggerFactory.getLogger(SetPOLineIdDAO.class);

  public static void setPOLineId(PoReceiptHeaderDTO orderDTO)
      throws CreateReceiptException, Exception {
    try {
      OBContext.setAdminMode();
      Order objOrder = OBDal.getInstance().get(Order.class, orderDTO.getOrderId());
      if (objOrder != null) {
        for (PoReceiptLineDTO lineDTO : orderDTO.getLineDTO()) {
          // if POLine ID is empty then need to Set POline Id based on Item number
          if (StringUtils.isEmpty(lineDTO.getPoLineId())) {
            // If Item Number also empty then need to set POLine ID based on item Description
            if (StringUtils.isEmpty(lineDTO.getItemNo())) {
              if (StringUtils.isEmpty(lineDTO.getItemDescription())) {
                throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_ItemNumDesEmpty"),
                    true);
              } else {
                OBQuery<OrderLine> orderLineObj = OBDal.getInstance().createQuery(OrderLine.class,
                    "as e where e.salesOrder.id =:OrderId  and escmProdescription =:description");
                orderLineObj.setNamedParameter("OrderId", objOrder.getId());
                orderLineObj.setNamedParameter("description", lineDTO.getItemDescription());
                orderLineObj.setFilterOnReadableClients(false);
                orderLineObj.setFilterOnReadableOrganization(false);
                List<OrderLine> orderLineList = orderLineObj.list();
                if (orderLineList.size() > 0) {
                  if (orderLineList.size() == 1) {
                    // set POline Id based on itemNo
                    lineDTO.setPoLineId(orderLineList.get(0).getId());
                  } else {
                    throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_InvDupItemDesc")
                        .replace("%", lineDTO.getItemDescription()), true);
                  }
                } else {
                  throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_InvDupItemDesc")
                      .replace("%", lineDTO.getItemDescription()), true);
                }

              }
            } else {
              // Get POlineid based on Item number and Order Id
              OBQuery<OrderLine> orderLineObj = OBDal.getInstance().createQuery(OrderLine.class,
                  " as e join e.product as p where e.salesOrder.id =:OrderId and p.searchKey =:ItemCode");
              orderLineObj.setNamedParameter("OrderId", objOrder.getId());
              orderLineObj.setNamedParameter("ItemCode", lineDTO.getItemNo());
              orderLineObj.setFilterOnReadableClients(false);
              orderLineObj.setFilterOnReadableOrganization(false);
              List<OrderLine> orderLineList = orderLineObj.list();
              if (orderLineList.size() > 0) {
                if (orderLineList.size() == 1) {
                  // set POline Id based on itemNo
                  lineDTO.setPoLineId(orderLineList.get(0).getId());
                } else {
                  throw new CreateReceiptException(OBMessageUtils.messageBD("ESCM_duplicateItem")
                      .replace("%", lineDTO.getItemNo()), true);
                }
              } else {
                throw new CreateReceiptException(
                    OBMessageUtils.messageBD("ESCM_InvalidItem").replace("%", lineDTO.getItemNo()),
                    true);
              }
            }
          }
        }
      } else {
        throw new CreateReceiptException(
            OBMessageUtils.messageBD("ESCM_InvalidOrderId").replace("%", orderDTO.getOrderId()),
            true);
      }
    } catch (CreateReceiptException e) {
      throw new CreateReceiptException(e.getMessage(), true);
    } catch (Exception e) {
      log4j.error("Exception in setPOLineId : " + e);
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
