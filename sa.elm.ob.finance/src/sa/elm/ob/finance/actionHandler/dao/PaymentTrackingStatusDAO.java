package sa.elm.ob.finance.actionHandler.dao;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinPaymentTrackingStatus;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Poongodi 18/12/2017
 *
 */

public class PaymentTrackingStatusDAO {
  private static final Logger LOG = LoggerFactory.getLogger(PaymentTrackingStatusDAO.class);

  /**
   * 
   * @param paymentId
   * @param bankName
   * @param chequeNo
   * @param chequeStatus
   * @param bankNote
   * @return
   */
  public static int insertlineinpaymenttracking(String paymentId, String bankName, String chequeNo,
      String chequeDate, String chequeStatus, String bankNote, String bankSentDate,
      String receiveChequeDate) {

    EfinPaymentTrackingStatus trackingStatus = null;
    int count = 0;
    DateFormat dateyearFormat = new SimpleDateFormat("dd-MM-yyyy");
    DateFormat dateyearForm = new SimpleDateFormat("yyyy-MM-dd");
    boolean errorFlag = true;
    Date now = new Date();
    Date todaydate = null;
    try {
      todaydate = dateyearFormat.parse(dateyearFormat.format(now));
    } catch (ParseException e2) {
      LOG.error("Exception in insertlineinpaymenttracking :  "+e2);
      e2.printStackTrace();
    }
    try {
      if (chequeDate != null && !chequeDate.equals("null") && !chequeDate.equals("")) {
        chequeDate = dateyearFormat.format(dateyearForm.parse(chequeDate));
      }
    } catch (ParseException e1) {
      LOG.error("Exception in insertlineinpaymenttracking :  "+e1);
    }

    try {
      if (bankSentDate != null && !bankSentDate.equals("null") && !bankSentDate.equals("")) {
        bankSentDate = dateyearFormat.format(dateyearForm.parse(bankSentDate));
      }
    } catch (ParseException e1) {
      LOG.error("Exception in insertlineinpaymenttracking :  "+e1);
    }

    try {
      if (receiveChequeDate != null && !receiveChequeDate.equals("null")
          && !receiveChequeDate.equals("")) {
        receiveChequeDate = dateyearFormat.format(dateyearForm.parse(receiveChequeDate));
      }
    } catch (ParseException e1) {
      LOG.error("Exception in insertlineinpaymenttracking :  "+e1);
    }

    try {
      OBContext.setAdminMode();
      FIN_Payment paymentOut = OBDal.getInstance().get(FIN_Payment.class, paymentId);
      Date paymentchequeDate = convertGregorian(chequeDate);
      Date paymentbankSentDate = convertGregorian(bankSentDate);
      Date paymentReceiveChequeDate = convertGregorian(receiveChequeDate);
      if (chequeStatus.equals("null")) {
        chequeStatus = null;
      }
      if ((paymentchequeDate != null && paymentchequeDate.compareTo(todaydate) == 1)
          || (paymentbankSentDate != null && paymentbankSentDate.compareTo(todaydate) == 1)
          || (paymentReceiveChequeDate != null
              && paymentReceiveChequeDate.compareTo(todaydate) == 1)) {
        count = -1;
        errorFlag = false;
      }
      if (errorFlag) {
        if (!StringUtils.isEmpty(chequeStatus) && chequeStatus.equals("PIM")) {
          if (bankName.equals("null") || chequeNo.equals("null")) {
            count = -2;
            errorFlag = false;
          }
        }
      }
      if (errorFlag) {
        if (bankName != null && !bankName.equals("null") && !bankName.equals("")) {
          paymentOut.setEfinMofbankname(bankName);
        } else {
          paymentOut.setEfinMofbankname(null);
        }
        if (chequeNo != null && !chequeNo.equals("null") && !chequeNo.equals("")) {
          paymentOut.setEfinMofchequeno(chequeNo);
        } else {
          paymentOut.setEfinMofchequeno(null);
        }

        paymentOut.setEfinMofchqstatus(chequeStatus);
        if (bankNote != null && !bankNote.equals("null") && !bankNote.equals("")) {
          paymentOut.setEfinBanknote(bankNote);
        } else {
          paymentOut.setEfinBanknote(null);
        }
        if (chequeDate != null && !chequeDate.equals("null") && !chequeDate.equals("")) {
          paymentOut.setEfinMofchequedate(convertGregorian(chequeDate));
        } else {
          paymentOut.setEfinMofchequedate(null);
        }
        if (bankSentDate != null && !bankSentDate.equals("null") && !bankSentDate.equals("")) {
          paymentOut.setEfinBanksentdate(convertGregorian(bankSentDate));
        } else {
          paymentOut.setEfinBanksentdate(null);
        }
        if (receiveChequeDate != null && !receiveChequeDate.equals("null")
            && !receiveChequeDate.equals("")) {
          paymentOut.setEfinReceiveChequeDate(convertGregorian(receiveChequeDate));
        } else {
          paymentOut.setEfinReceiveChequeDate(null);
        }
        OBDal.getInstance().save(paymentOut);
        OBDal.getInstance().flush();
        trackingStatus = OBProvider.getInstance().get(EfinPaymentTrackingStatus.class);
        trackingStatus.setOrganization(paymentOut.getOrganization());
        trackingStatus.setPayment(OBDal.getInstance().get(FIN_Payment.class, paymentId));
        if (bankName != null && !bankName.equals("null") && !bankName.equals("")) {
          trackingStatus.setMofbankname(bankName);
        }
        if (chequeNo != null && !chequeNo.equals("null") && !chequeNo.equals("")) {
          trackingStatus.setMofchequeno(chequeNo);
        }
        trackingStatus.setMofchqstatus(chequeStatus);
        if (chequeDate != null && !chequeDate.equals("null") && !chequeDate.equals("")) {
          trackingStatus.setMofchequedate(convertGregorian(chequeDate));
        }
        if (bankNote != null && !bankNote.equals("null") && !bankNote.equals("")) {
          trackingStatus.setBanknote(bankNote);
        }
        if (bankSentDate != null && !bankSentDate.equals("null") && !bankSentDate.equals("")) {
          trackingStatus.setBanksentdate(convertGregorian(bankSentDate));
        }
        if (receiveChequeDate != null && !receiveChequeDate.equals("null")
            && !receiveChequeDate.equals("")) {
          trackingStatus.setReceiveChequeDate(convertGregorian(receiveChequeDate));
        }

        OBDal.getInstance().save(trackingStatus);
        OBDal.getInstance().flush();
        count = 1;
      }
    } catch (

    Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while insertlineinpaymenttracking: ", e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }
/**
 * This method is used convert hijiri to Gregorian  
 * @param hijridate
 * @return
 */
  public static Date convertGregorian(String hijridate) {

    String gregDate = Utility.convertToGregorian(hijridate);
    Date greDate = null;
    try {
      DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
      greDate = df1.parse(gregDate);

    } catch (Exception e) {
      LOG.error("convertGregorian: ", e);
    }
    return greDate;

  }
}
