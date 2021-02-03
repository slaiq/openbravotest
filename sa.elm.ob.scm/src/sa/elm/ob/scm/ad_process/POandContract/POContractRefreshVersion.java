package sa.elm.ob.scm.ad_process.POandContract;

import java.util.Date;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.EscmOrderSourceRef;
import sa.elm.ob.scm.Escmordershipment;

/**
 * 
 * @author qualian
 * 
 */

public class POContractRefreshVersion implements Process {
  /**
   * This servlet class is responsible to copy quantity from old version to new version.
   */
  private static Logger log = Logger.getLogger(POContractRefreshVersion.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode();
      String orderID = (String) bundle.getParams().get("C_Order_ID");
      Order order = OBDal.getInstance().get(Order.class, orderID);
      // removing and inserting new lines to update qty.
      order.getOrderLineList().removeAll(order.getOrderLineList());

      /*
       * for (Escmordershipment objOrderShipment : objOrderLine.getEscmOrdershipmentList()) {
       * OBDal.getInstance().remove(objOrderShipment); } for (EscmOrderSourceRef objOrderSourceRef :
       * objOrderLine.getEscmOrdersourceRefList()) { OBDal.getInstance().remove(objOrderSourceRef);
       * } OBDal.getInstance().remove(objOrderLine);
       */

      for (OrderLine objOrderLinenew : order.getEscmOldOrder().getOrderLineList()) {
        if (objOrderLinenew.getESCMCancelledBy() == null) {
          OBQuery<Escmordershipment> shipList = OBDal.getInstance()
              .createQuery(Escmordershipment.class, " as e where e.salesOrderLine.id='"
                  + objOrderLinenew.getId() + "' and e.cancelledby is  null");
          if (shipList.list().size() > 0) {
            OrderLine objCloneOrderLine = (OrderLine) DalUtil.copy(objOrderLinenew, false);
            objCloneOrderLine.setSalesOrder(order);
            objCloneOrderLine.setCreationDate(new Date());
            objCloneOrderLine.setUpdated(new Date());
            OBDal.getInstance().save(objCloneOrderLine);
            // insert shipment
            for (Escmordershipment objOrderShipmentnew : objOrderLinenew
                .getEscmOrdershipmentList()) {
              if (objOrderShipmentnew.getCancelledby() == null) {
                Escmordershipment objCloneOrderShipment = (Escmordershipment) DalUtil
                    .copy(objOrderShipmentnew, false);
                objCloneOrderShipment.setSalesOrderLine(objCloneOrderLine);
                objCloneOrderShipment.setCreationDate(new Date());
                objCloneOrderShipment.setUpdated(new Date());
                OBDal.getInstance().save(objCloneOrderShipment);
              }
            }
            for (EscmOrderSourceRef objOrderSourceRefnew : objOrderLinenew
                .getEscmOrdersourceRefList()) {
              EscmOrderSourceRef objCloneOrderSourceRef = (EscmOrderSourceRef) DalUtil
                  .copy(objOrderSourceRefnew, false);
              objCloneOrderSourceRef.setSalesOrderLine(objCloneOrderLine);
              objCloneOrderSourceRef.setCreationDate(new Date());
              objCloneOrderSourceRef.setUpdated(new Date());
              OBDal.getInstance().save(objCloneOrderSourceRef);
            }
          }
        }
      }
      OBDal.getInstance().flush();
      OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Order_Cancelled@");
      bundle.setResult(result);
      return;

    } catch (OBException e) {
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception in copy quantity from old version to new version.:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
      OBDal.getInstance().getSession().clear();
    }
  }
}