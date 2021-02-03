package sa.elm.ob.scm.event.dao;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmICExtension;
import sa.elm.ob.scm.EscmInsuranceCertificate;

/**
 * 
 * This class is used to handle dao activities of InsuranceCertificatEvent.
 */
public class InsuranceCertificateEventDAO {
  private static final Logger log = LoggerFactory.getLogger(InsuranceCertificateEventDAO.class);

  /**
   * Check Ic_No already exists
   * 
   * @param InsuranceCerificate
   * @return true if exists
   */
  public static boolean checkICNOExists(EscmInsuranceCertificate InsuranceCerificate) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmInsuranceCertificate> IC = OBDal.getInstance()
          .createQuery(EscmInsuranceCertificate.class, "no=:certfNO");
      IC.setNamedParameter("certfNO", InsuranceCerificate.getNo());
      if (IC.list() != null && IC.list().size() > 0) {
        return true;
      }
      return false;
    } catch (OBException e) {
      log.error("Exception while checking IC_NO already exists:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get Order object
   * 
   * @param OrderId
   * @return
   */
  public static Order getOrder(String OrderId) {
    try {
      OBContext.setAdminMode();
      Order order = OBDal.getInstance().get(Order.class, OrderId);
      return order;
    } catch (OBException e) {
      log.error("Exception while getOrder object:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Check old record cerificate reference and date is filled.
   * 
   * @param icdetails
   * @return
   */
  public static Boolean chkICExtProcessornot(EscmInsuranceCertificate icdetails) {
    try {
      OBQuery<EscmICExtension> icextension = OBDal.getInstance().createQuery(EscmICExtension.class,
          " as e where e.escmInsuranceCertificate.id=:certfID "
              + " and ( e.letterRef is null or e.letterReferenceDateH is null ) order by creationDate desc  ");
      icextension.setNamedParameter("certfID", icdetails.getId());
      icextension.setMaxResult(1);
      if (icextension.list().size() > 0) {
        return true;
      } else
        return false;
    }

    catch (final Exception e) {
      log.error("Exception in chkICExtProcessornot() Method : ", e);
    }
    return false;
  }

  /**
   * 
   * @param insuranceCompany
   * @param DocNo
   * @return
   */
  public static boolean checkDocNoAndInsComCombinationExists(String insuranceCompany,
      String DocNo) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmInsuranceCertificate> IC = OBDal.getInstance().createQuery(
          EscmInsuranceCertificate.class,
          "as e where e.insuranceCompany.id=:compID and e.salesOrder.id=:orderID ");
      IC.setNamedParameter("compID", insuranceCompany);
      IC.setNamedParameter("orderID", DocNo);

      if (IC.list() != null && IC.list().size() > 0) {
        return true;
      }
      return false;
    } catch (OBException e) {
      log.error("Exception while checking DocNoAndInsComCombination already exists:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}