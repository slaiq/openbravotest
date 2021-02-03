package sa.elm.ob.scm.charts.OnhandStatus;

import java.sql.Connection;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class OnhandStatusDao {

  Connection conn = null;
  private static Logger log4j = Logger.getLogger(OnhandStatusDao.class);

  public OnhandStatusDao(Connection con) {
    this.conn = con;
  }

  @SuppressWarnings("unchecked")
  public List<Object> getOnhandStatus(String clientId, String userId, String roleId) {
    // String hqlQuery =
    // "select Organization.name as store, Warehouse.name as warehouse,Organization.id as orgid, "
    // +
    // "Product.searchKey as identifier,Product.name as product,Product.id as pid, ProductCategory.searchKey as category,"
    // +
    // " ProductCategory.id as pcid,UOM.name as uom, sum(ps.quantityOnHand) as qtyonhand,AttributeSetInstance.description as attr,Warehouse.id as whid from"
    // +
    // " ProductStockView ps left outer join ps.product as Product left outer join ps.storageBin as Locator left outer join Locator.warehouse as Warehouse"
    // +
    // " left outer join Product.productCategory as ProductCategory left outer join Warehouse.organization as Organization"
    // +
    // " left outer join ps.uOM as UOM left outer join ps.attributeSetValue as AttributeSetInstance where ps.client.id='"
    // + clientId
    // + "' AND Product.stocked='Y' AND qtyonhand > 0 group by Organization.name,"
    // +
    // " Warehouse.name, Organization.id, Product.searchKey, Product.name, Product.id, ProductCategory.searchKey, ProductCategory.id, UOM.name, AttributeSetInstance.description,Warehouse.id "
    // + "order by Warehouse.name, Product.name, sum(ps.quantityOnHand) desc";
    Query productCat = null;
    String hqlQuery = "select mainprdcat.searchKey, sum(strdetail.quantityOnHand)  from ProductCategory mainprdcat "
        + "join mainprdcat.productCategoryEMEscmProductCategoryList  childprdcat "
        + "join childprdcat.productList prod "
        + "join prod.materialMgmtStorageDetailList strdetail "
        + "where mainprdcat.summaryLevel ='Y' and mainprdcat.client.id ='"
        + clientId
        + "' group by mainprdcat.searchKey";
    // OBQuery<ProductCategory> productCatagories = null;
    try {
      OBContext.setAdminMode(true);
      log4j.debug("Query: " + hqlQuery);
      // productCatagories = OBDal.getInstance().createQuery(ProductCategory.class, hqlQuery);
      productCat = OBDal.getInstance().getSession().createQuery(hqlQuery);
      // List<Object> list = productCat.list();
    } catch (Exception e) {
      log4j.error("Exception in getOnhandStatus() ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return productCat.list();
  }
}