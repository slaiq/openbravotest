package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;

public class PurchaseAgreementCalloutDAO {
  private static final Logger log4j = Logger.getLogger(PurchaseAgreementCalloutDAO.class);

  public static String getBusinessPartner(String orderId) {

    PreparedStatement st = null;
    ResultSet rs = null;
    String businessPartner = "";
    try {
      st = OBDal.getInstance().getConnection().prepareStatement(
          " select c_bpartner_id as c_bpartner_id from c_bpartner where  isvendor='Y' and isemployee='N' and c_bpartner_id in ( select c_orderline.c_bpartner_id from c_orderline "
              + " left join c_order ord on ord.c_order_id=c_orderline.c_order_id "
              + " where c_orderline.c_order_id=? and ((  ord.em_escm_receivetype='QTY' "
              + " and( coalesce(c_orderline.qtyordered,0) - coalesce(c_orderline.em_escm_releaseqty,0))>0) "
              + " or( ord.em_escm_receivetype='AMT' and  coalesce((c_orderline.linenetamt - c_orderline.em_escm_releaseamt),0) >0))) limit 1");
      log4j.debug(st.toString());
      st.setString(1, orderId);
      rs = st.executeQuery();
      if (rs.next()) {
        businessPartner = rs.getString("c_bpartner_id");
      }
    }

    catch (final Exception e) {
      log4j.error("Exception in get Business Partner Method : ", e);
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in PurchaseAgreementCalloutDAO ", e);
      }
    }

    return businessPartner;
  }

  /**
   * Get Po configuration object based on order type selected in PO
   * 
   * @param clientId
   * @param orgId
   * @param docVal
   * @param docType
   * @return true
   */
  public static EscmPurchaseOrderConfiguration checkDocTypeConfig(String clientId, String orgId,
      String orderType) {
    String query = " as cfig where cfig.ordertype=? and cfig.organization.id=? and cfig.client.id=? ";
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(orderType);
    parametersList.add(orgId);
    parametersList.add(clientId);
    OBQuery<EscmPurchaseOrderConfiguration> cfigLs = null;
    try {

      OBContext.setAdminMode();
      cfigLs = OBDal.getInstance().createQuery(EscmPurchaseOrderConfiguration.class, query,
          parametersList);
      cfigLs.setMaxResult(1);
      if (cfigLs != null && cfigLs.list().size() > 0) {

        return cfigLs.list().get(0);
      }

      else {
        EfinBudgetControlParam budgContrparam = FundsReqMangementDAO.getControlParam(clientId);
        String hqOrg = budgContrparam.getAgencyHqOrg().getId();
        if (!hqOrg.equals(orgId)) {
          return checkDocTypeConfig(clientId, budgContrparam.getAgencyHqOrg().getId(), orderType);
        }
      }
    } catch (OBException e) {
      log4j.error("Exception while checkDocTypeConfig:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  public static EscmPurchaseOrderConfiguration checkDocTypeConfigwithAmt(String clientId,
      String orgId, BigDecimal totalAmount) {
    String query = " as cfig where cfig.minValue <=? and cfig.organization.id=? and cfig.client.id=? order by cfig.minValue desc  ";
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(totalAmount);
    parametersList.add(orgId);
    parametersList.add(clientId);
    OBQuery<EscmPurchaseOrderConfiguration> cfigLs = null;
    try {
      OBContext.setAdminMode();
      cfigLs = OBDal.getInstance().createQuery(EscmPurchaseOrderConfiguration.class, query,
          parametersList);
      cfigLs.setMaxResult(1);
      if (cfigLs != null && cfigLs.list().size() > 0) {
        return cfigLs.list().get(0);
      } else {
        EfinBudgetControlParam budgContrparam = FundsReqMangementDAO.getControlParam(clientId);
        String hqOrg = budgContrparam.getAgencyHqOrg().getId();
        if (!hqOrg.equals(orgId)) {
          return checkDocTypeConfigwithAmt(clientId, budgContrparam.getAgencyHqOrg().getId(),
              totalAmount);
        }
      }
    } catch (OBException e) {
      log4j.error("Exception while checkDocTypeConfig:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

}
