package sa.elm.ob.scm.ad_process.RFPSalesVoucher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmsalesvoucher;

/**
 * @author Qualian This class is used to handle RFP salesvoucher window DAO process.
 */
public class RFPSalesvoucherDao {
  private static final Logger log = LoggerFactory.getLogger(RFPSalesVoucherAction.class);

  /**
   * 
   * @param salesRFP
   *          Escmsalesvoucher current object
   * @return true or false
   */
  public static boolean performAction(Escmsalesvoucher salesRFP, String bidno) {
    String documentNo = "";
    try {
      OBContext.setAdminMode();
      if (salesRFP.getEscmDocaction().equals("CO")) {
        salesRFP.setDocumentStatus("CO");
        salesRFP.setEscmDocaction("RE");
        if (salesRFP.getSupplierNumber() == null)
          salesRFP.setManualsupplier(true);
        else
          salesRFP.setManualsupplier(false);

        // get documentno +1 of records based on selected bid no. and update documentno with
        // documentno+1
        documentNo = RFPSalesvoucherDao.getCountOfRecordwithBidNo(salesRFP, bidno);
        salesRFP.setDocumentNo(documentNo);

      } else if (salesRFP.getEscmDocaction().equals("RE")) {
        String supplierNo = null;
        if (salesRFP.getSupplierNumber() == null) {
          supplierNo = "";
        } else {
          supplierNo = salesRFP.getSupplierNumber().getId();
        }
        OBQuery<EscmProposalMgmt> proposalmgmt = OBDal.getInstance().createQuery(
            EscmProposalMgmt.class, "escmBidmgmt.id=:bidId and supplier.id=:supplierno");
        proposalmgmt.setNamedParameter("bidId", salesRFP.getEscmBidmgmt().getId());
        proposalmgmt.setNamedParameter("supplierno", supplierNo);

        if (proposalmgmt.list().size() > 0) {
          return false;
        } else {
          salesRFP.setDocumentStatus("DR");
          salesRFP.setEscmDocaction("CO");
        }
      }
      OBDal.getInstance().save(salesRFP);
      return true;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in rfpsalesvoucherdao : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // Payment amount should not be zero
  public static boolean checkPaymentAmountValidation(Escmsalesvoucher salesRFP) {
    boolean status = false;
    try {
      OBContext.setAdminMode();
      if (salesRFP.getEscmDocaction().equals("CO")) {
        if (salesRFP.getAmountsar().compareTo(new BigDecimal(0)) == 0) {
          status = true;
        }
      }
      return status;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in checkPaymentAmountValidation : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param salesRFP
   *          Escmsalesvoucher current object
   * @return proposalno
   */
  public static String getProposalNo(Escmsalesvoucher salesRFP) {
    try {
      String supplierNo = null;
      if (salesRFP.getSupplierNumber() == null) {
        supplierNo = "";
      } else {
        supplierNo = salesRFP.getSupplierNumber().getId();
      }
      List<EscmProposalMgmt> proposalList = new ArrayList<EscmProposalMgmt>();
      OBQuery<EscmProposalMgmt> proposalmgmt = OBDal.getInstance()
          .createQuery(EscmProposalMgmt.class, "escmBidmgmt.id=:bidId and supplier.id=:supplierno");
      proposalmgmt.setNamedParameter("bidId", salesRFP.getEscmBidmgmt().getId());
      proposalmgmt.setNamedParameter("supplierno", supplierNo);
      proposalList = proposalmgmt.list();
      if (proposalList.size() > 0) {
        return proposalList.get(0).getProposalno();
      }
      return "";
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in rfpsalesvoucherdao : ", e);
      return "";
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Method to get the count of record using bid number
   * 
   * @param salesRFP
   * @param bidno
   * @return documentNo
   */
  private static String getCountOfRecordwithBidNo(Escmsalesvoucher salesRFP, String bidno) {
    int documentNo = 0;
    try {
      OBContext.setAdminMode();
      List<Escmsalesvoucher> salesvoucherList = new ArrayList<Escmsalesvoucher>();
      OBQuery<Escmsalesvoucher> salesvoucher = OBDal.getInstance()
          .createQuery(Escmsalesvoucher.class, "escmBidmgmt.id=:bidID and documentStatus='CO' "
              + " and id<>:rfpID order by creationDate desc");
      salesvoucher.setNamedParameter("bidID", bidno);
      salesvoucher.setNamedParameter("rfpID", salesRFP.getId());
      salesvoucher.setMaxResult(1);
      salesvoucherList = salesvoucher.list();
      if (salesvoucherList.size() > 0) {
        Escmsalesvoucher sales = salesvoucherList.get(0);
        int docno = Integer.parseInt(sales.getDocumentNo());
        documentNo = docno + 1;
      } else {
        documentNo = 1;
      }
      return Integer.toString(documentNo);

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in getCountOfRecordwithBidNo : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return Integer.toString(documentNo);
  }
}
