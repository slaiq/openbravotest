package sa.elm.ob.scm.process.ReceiptRemainQtyUpdate;

import java.math.BigDecimal;

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
 * @author Gopalakrishnan on 05/04/2017
 * 
 */

public class RemainQtyUpdateBackground extends DalBaseProcess {
  /**
   * This Process Class is responsible to update physical on hand quantity in transactions
   * 
   */
  static int counter = 0;

  private ProcessLogger logger;

  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    String transactionQry = "", transactionId;
    final Session session = OBDal.getInstance().getSession();
    SQLQuery query = null, transactionQuery = null, updateQuery = null;
    logger.log("Starting background product transaction Loop " + counter + "\n");

    try {

      final OBCriteria<Product> productList = OBDal.getInstance().createCriteria(Product.class);

      logger.log("No of products = " + productList.list().size() + "\n");

      // loop through all products that are transacted calculate stock
      // for each
      for (Product product : productList.list()) {
        BigDecimal onhandQty = BigDecimal.ZERO;
        // get onhand Qty of product

        String qry = "select sum(movementqty) as qty from m_transaction  where 1=1 "
            + " and m_product_id = :product ";
        query = session.createSQLQuery(qry);
        query.setParameter("product", product.getId());
        if (query.list().size() > 0) {
          for (Object resultObj : query.list()) {
            onhandQty = (BigDecimal) resultObj;
            logger.log("Product =" + product.getName() + " on hand Qty=" + onhandQty + "\n");
          }
        }
        transactionQry = " select tr.m_transaction_id from m_transaction tr join m_inoutline line on line.m_inoutline_id=tr.m_inoutline_id "
            + " where tr.m_product_id =:product order by tr.movementdate desc ";

        transactionQuery = session.createSQLQuery(transactionQry);
        transactionQuery.setParameter("product", product.getId());

        // update remaining Qty as Zero
        // receipt entries
        if (transactionQuery.list().size() > 0) {
          for (Object resultObj : transactionQuery.list()) {
            transactionId = (String) resultObj;
            MaterialTransaction objMatTransaction = OBDal.getInstance().get(
                MaterialTransaction.class, transactionId);
            objMatTransaction.setEscmRemainqty(BigDecimal.ZERO);
            OBDal.getInstance().save(objMatTransaction);
            OBDal.getInstance().flush();
          }
        }

        updateQuery = session.createSQLQuery(transactionQry);
        updateQuery.setParameter("product", product.getId());
        // calculate remaining qty and apply it
        if (transactionQuery.list().size() > 0) {
          for (Object resultObj : transactionQuery.list()) {
            transactionId = (String) resultObj;
            MaterialTransaction objMatTransaction = OBDal.getInstance().get(
                MaterialTransaction.class, transactionId);
            if (onhandQty.compareTo(BigDecimal.ZERO) == 1) {
              if (onhandQty.compareTo(objMatTransaction.getMovementQuantity()) >= 0) {
                objMatTransaction.setEscmRemainqty(objMatTransaction.getMovementQuantity());
                onhandQty = onhandQty.subtract(objMatTransaction.getMovementQuantity());
              } else if (onhandQty.compareTo(objMatTransaction.getMovementQuantity()) == -1) {
                objMatTransaction.setEscmRemainqty(onhandQty);
                onhandQty = BigDecimal.ZERO;
              }
              OBDal.getInstance().save(objMatTransaction);
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
