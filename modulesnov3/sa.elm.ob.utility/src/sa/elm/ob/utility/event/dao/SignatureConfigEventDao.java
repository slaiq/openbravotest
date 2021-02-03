package sa.elm.ob.utility.event.dao;

import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.EUTSignatureConfig;

public class SignatureConfigEventDao {
  /**
   * 
   * This class is used to handle dao activities of Signature Configuration window.
   */
  private static final Logger log = LoggerFactory.getLogger(SignatureConfigEventDao.class);
  public static String AP_Invoice = "EUT_101";
  public static String AP_PrepaymentInv = "EUT_110";
  public static String AP_Prepayment_App = "EUT_109";

  /**
   * Get list of records in Signature Configuration for specific doctype
   * 
   * @param doctype
   * @return
   */
  public List<EUTSignatureConfig> getListofRecords(String doctype) {
    try {
      OBQuery<EUTSignatureConfig> signList = OBDal.getInstance()
          .createQuery(EUTSignatureConfig.class, "documentType=:docType");
      signList.setNamedParameter("docType", doctype);
      return signList.list();
    } catch (OBException e) {
      log.error("Exception while getting cost center from user manager:" + e);
      throw new OBException(e.getMessage());
    }
  }

  /**
   * check is document already digitally signed
   * 
   * @param doctype
   * @return
   */
  public boolean checkIsDocumentAlreadySigned(String doctype) {
    try {
      if (doctype.equals(AP_Invoice) || doctype.equals(AP_PrepaymentInv)
          || doctype.equals(AP_Prepayment_App)) {
        OBQuery<Invoice> invoiceList = OBDal.getInstance().createQuery(Invoice.class,
            " as e where e.documentType.documentCategory=:docType and e.eutAttachPath is not null ");
        invoiceList.setNamedParameter("docType", "API");
        if (invoiceList.list().size() > 0) {
          return true;
        }
      }
    } catch (OBException e) {
      log.error("Exception while checkIsDocumentAlreadySigned:" + e);
      throw new OBException(e.getMessage());
    }
    return false;
  }
}