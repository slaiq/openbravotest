package sa.elm.ob.scm.process.Stockupdate;

import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.quartz.JobExecutionException;

/**
 * 
 * @author Gopalakrishnan on 04/04/2017
 * 
 */

public class StockupdateBackground extends DalBaseProcess {
  /**
   * This Process Class is responsible to update physical on hand quantity in transactions
   * 
   */
  static int counter = 0;

  private ProcessLogger logger;

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    String transactionQry = "", updateQry = "";
    BigDecimal updateQty = null;
    final Session session = OBDal.getInstance().getSession();
    SQLQuery query = null, transactionQuery = null, updateQuery = null;
    logger.log("Starting background product transaction Loop " + counter + "\n");

    try {

      final OBCriteria<Product> productList = OBDal.getInstance().createCriteria(Product.class);

      logger.log("No of products = " + productList.list().size() + "\n");

      // loop through all products that are transacted calculate stock
      // for each
      for (Product product : productList.list()) {

        Date lastTrDate = null;
        // select recent transaction date with physical qty is null and its transaction date
        String qry = "select movementdate,created from m_transaction where  em_escm_physcicalonhand is null "
            + "and m_product_id= '"
            + product.getId()
            + "' "
            + "order by movementdate asc,created asc limit 1";
        query = session.createSQLQuery(qry);
        // query.setParameter("product", product.getId());
        if (query.list().size() > 0) {
          logger.log("Product Have Transactions =" + product.getName() + "\n");
          for (Object resultObj : query.list()) {
            if (resultObj.getClass().isArray()) {
              final Object[] values = (Object[]) resultObj;
              lastTrDate = (Date) values[0];
              transactionQry = "select m_transaction_id from m_transaction where  movementdate >= '"
                  + lastTrDate + "'  and m_product_id='" + product.getId() + "'";
            }
          }
          /*
           * else { transactionQry =
           * "select m_transaction_id from m_transaction where m_product_id='" + product.getId() +
           * "' order by movementdate asc,created asc"; }
           */

          // get all transaction was done after that recent transaction date
          transactionQuery = session.createSQLQuery(transactionQry);
          if (transactionQuery.list().size() > 0) {
            for (Object resultObj : transactionQuery.list()) {
              String transactionId = (String) resultObj;
              // get Transaction(Individual)
              MaterialTransaction objMatTransaction = OBDal.getInstance().get(
                  MaterialTransaction.class, transactionId);
              // sum all previous transaction
              updateQry = "select sum(movementqty) as qty from m_transaction where M_PRODUCT_ID ='"
                  + objMatTransaction.getProduct().getId() + "' AND M_LOCATOR_ID ='"
                  + objMatTransaction.getStorageBin().getId() + "' "
                  + " AND M_ATTRIBUTESETINSTANCE_ID ='"
                  + objMatTransaction.getAttributeSetValue().getId() + "' AND C_UOM_ID ='"
                  + objMatTransaction.getUOM().getId() + "' " + " and ( (movementdate <='"
                  + objMatTransaction.getMovementDate() + "' and created <='"
                  + objMatTransaction.getCreationDate() + "') or  movementdate < '"
                  + objMatTransaction.getMovementDate() + "' ) ";
              updateQuery = session.createSQLQuery(updateQry);
              if (updateQuery.list().size() > 0) {
                for (Object updateObj : updateQuery.list())
                  updateQty = (BigDecimal) updateObj;
              }
              objMatTransaction.setEscmPhyscicalonhand(updateQty);
              OBDal.getInstance().flush();
            }
          }
        }
      }

    } catch (Exception e) {
      // catch any possible exception and throw it as a Quartz
      // JobExecutionException
      throw new JobExecutionException(e.getMessage(), e);
    }
  }
}
