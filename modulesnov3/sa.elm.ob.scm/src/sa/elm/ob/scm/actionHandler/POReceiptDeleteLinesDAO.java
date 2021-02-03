package sa.elm.ob.scm.actionHandler;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmAddreceipt;
import sa.elm.ob.scm.EscmInitialReceipt;

/**
 * 
 * @author Gokul 19/12/2018
 *
 */
public class POReceiptDeleteLinesDAO {
  private static final Logger log = LoggerFactory.getLogger(POReceiptDeleteLinesDAO.class);

  /**
   * Returns true after deleted
   * 
   * @param poReceipt
   * @return
   */
  public static boolean deletelines(ShipmentInOut poReceipt) {
    try {
      OBContext.setAdminMode();
      List<EscmInitialReceipt> poReceiptLine = new ArrayList<EscmInitialReceipt>();
      List<EscmAddreceipt> poAddReceipt = new ArrayList<EscmAddreceipt>();
      poReceiptLine = poReceipt.getEscmInitialReceiptList();
      poAddReceipt = poReceipt.getEscmAddreceiptList();

      Query qry = OBDal.getInstance().getSession().createSQLQuery(
          " update escm_initialreceipt set ismanual='Y' , parent_line=null where m_inout_id=? ");
      qry.setParameter(0, poReceipt.getId());
      qry.executeUpdate();
      for (EscmInitialReceipt lines : poReceiptLine) {
        // lines.setManual(true);
        // lines.setParentLine(null);
        poReceipt.setSalesOrder(null);
        OBDal.getInstance().save(lines);
      }
      OBDal.getInstance().flush();

      poReceipt.getEscmInitialReceiptList().removeAll(poReceiptLine);

      for (EscmInitialReceipt lines : poReceiptLine) {
        OBDal.getInstance().remove(lines);
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().save(poReceipt);

      if (poReceipt.getDocumentType().isEscmIsporeceipt()) {
        poReceipt.getEscmAddreceiptList().removeAll(poAddReceipt);
        for (EscmAddreceipt addReceipt : poAddReceipt) {
          OBDal.getInstance().remove(addReceipt);
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().save(poReceipt);
      }
      return true;
    } catch (final Exception e) {
      log.error("Exception in POReceiptDeleteLines : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
