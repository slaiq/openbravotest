package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;

import sa.elm.ob.scm.MaterialIssueRequest;

/**
 * 
 * @author Gopalakrishnan on 09/03/2017
 * 
 */
public class EscmMaterialIssueLineCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmMaterialIssueLineCallout.class);
  /**
   * Callout to update the line Details in
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    Connection conn = OBDal.getInstance().getConnection();
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strMProductID = vars.getStringParameter("inpmProductId"),
        strOrgId = vars.getStringParameter("inpadOrgId");
    Product objProduct = OBDal.getInstance().get(Product.class, strMProductID);
    String strQty = vars.getNumericParameter("inprequestedQty"), query = "",
        deliveredQty = vars.getNumericParameter("inpdeliveredQty");
    String inpmirId = info.vars.getStringParameter("inpescmMaterialRequestId");
    BigDecimal qtyonHand = BigDecimal.ZERO;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      log.debug("LastChanged:" + inpLastFieldChanged);
      if (inpLastFieldChanged.equals("inpmProductId")) {
        if (strMProductID != null) {
          MaterialIssueRequest mir = OBDal.getInstance().get(MaterialIssueRequest.class, inpmirId);
          Product product = OBDal.getInstance().get(Product.class, strMProductID);
          info.addResult("inpcUomId", product.getUOM().getId());
          info.addResult("inpdescription", product.getName());
          if (product.getImage() != null)
            info.addResult("inpadImageId", product.getImage().getId());
          else
            info.addResult("inpadImageId", "");
          if (product.getEscmStockType() != null)
            info.addResult("inpitemType", product.getEscmStockType().getSearchKey());
          else
            info.addResult("inpitemType", "");
          if (mir.getWarehouse() != null) {
            query = " select sum(qtyonhand) as qtyonhand from m_storage_detail strdt "
                + " left join m_locator loc on loc.m_locator_id=strdt.m_locator_id "
                + " where m_product_id = ? and loc.m_warehouse_id = ? "
                + " group by m_warehouse_id, m_product_id";
            ps = conn.prepareStatement(query);
            ps.setString(1, strMProductID);
            ps.setString(2, mir.getWarehouse().getId());
            rs = ps.executeQuery();
            if (rs.next()) {
              info.addResult("inponhandQty", rs.getBigDecimal("qtyonhand"));
            } else
              info.addResult("inponhandQty", BigDecimal.ZERO);
          } else
            info.addResult("inponhandQty", BigDecimal.ZERO);
          if (rs != null)
            rs.close();
          if (ps != null)
            ps.close();
        } else {
          info.addResult("inpcUomId", "");
          info.addResult("inpdescription", "");
          info.addResult("inpitemType", "");
          info.addResult("inpadImageId", "");
        }
      }
      if (inpLastFieldChanged.equals("inprequestedQty")) {
        // show stock waring only for Non generic Product
        if (!(objProduct.getEscmStockType().getSearchKey().equals("CUS") && !objProduct.isStocked()
            && !objProduct.isPurchase())) {
          // get available on hand qty
          query = " select sum(qtyonhand) as qtyonhand,m_product_id from m_storage_detail "
              + " where ad_org_id='" + strOrgId + "'  and m_product_id='" + strMProductID
              + "' group by m_product_id ";
          ps = conn.prepareStatement(query);
          rs = ps.executeQuery();
          if (rs.next()) {
            qtyonHand = rs.getBigDecimal("qtyonhand");
            log.debug("qtyonHand:" + qtyonHand);
            if (qtyonHand.compareTo(new BigDecimal(strQty)) >= 0) {
              info.addResult("inpdeliveredQty", strQty);
            } else if (qtyonHand.compareTo(BigDecimal.ZERO) < 0) {
              info.addResult("inpdeliveredQty", BigDecimal.ZERO);
            } else {
              info.addResult("WARNING", OBMessageUtils.messageBD("ESCM_IR_Qty_NotAvailable"));
              info.addResult("inpdeliveredQty", qtyonHand);
            }

          } else {
            info.addResult("WARNING", OBMessageUtils.messageBD("ESCM_IR_Qty_NotAvailable"));
            info.addResult("inpdeliveredQty", qtyonHand);
          }
        }

        if (objProduct.getSearchKey() != null) {
          if (objProduct.getEscmStockType().getSearchKey().equals("CUS")) {
            BigDecimal bigQty = new BigDecimal(strQty);
            BigDecimal fractionalPart = bigQty.remainder(BigDecimal.ONE);
            if (fractionalPart.compareTo(BigDecimal.ZERO) == 1) {
              info.addResult("ERROR", OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));

            }
          }
        }

      }
      if (inpLastFieldChanged.equals("inpdeliveredQty")) {
        if (objProduct.getSearchKey() != null) {
          if (objProduct.getEscmStockType().getSearchKey().equals("CUS")) {
            BigDecimal bigQty = new BigDecimal(deliveredQty);
            BigDecimal fractionalPart = bigQty.remainder(BigDecimal.ONE);
            if (fractionalPart.compareTo(BigDecimal.ZERO) == 1) {
              info.addResult("ERROR", OBMessageUtils.messageBD("ESCM_Fractional(Custody)"));

            }
          }
        }
      }

    } catch (Exception e) {
      log.debug("Exception in Requisition item callout:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in EscmBiddatesCallout ", e);
      }
    }

  }
}
