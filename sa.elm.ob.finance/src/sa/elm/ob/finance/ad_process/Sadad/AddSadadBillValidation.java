package sa.elm.ob.finance.ad_process.Sadad;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfSaddadBillDetail;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.SaddadBillDetail;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.AddSaddadBillRequest;
import sa.elm.ob.utility.sadad.consumer.org.tempuri.AddSaddadBillResponse;

/*
 * This class contains some methods for validation while creating bill in sadad
 * 
 * @author Sathishkumar
 *
 */

public class AddSadadBillValidation {

  /**
   * This method is used to validate all fields has valid values
   * 
   * @param arINvoice
   * @return ErrorMsg if some values are not valid else null
   */

  private static final Logger log = LoggerFactory.getLogger(AddBillInSadad.class);

  static StringBuilder errorMsg = new StringBuilder("");

  // private final static QName _AddSaddadBillRequestMobileNo_QNAME = new
  // QName("http://elm.sa/grp/soap",
  // "MobileNo");
  // private final static QName _AddSaddadBillRequestIDNo_QNAME = new
  // QName("http://elm.sa/grp/soap",
  // "IDNo");
  // private final static QName _AddSaddadBillRequestNotes_QNAME = new
  // QName("http://elm.sa/grp/soap",
  // "Notes");
  // private final static QName _AddSaddadBillRequestCustomerName_QNAME = new QName(
  // "http://elm.sa/grp/soap", "CustomerName");
  // private final static QName _AddSaddadBillRequestSaddadBillDetails_QNAME = new QName(
  // "http://elm.sa/grp/soap", "SadadDetails");

  private final static QName _AddSaddadBillRequestMobileNo_QNAME = new QName("http://tempuri.org/",
      "MobileNo");

  private final static QName _AddSaddadBillRequestNotes_QNAME = new QName("http://tempuri.org/",
      "Notes");
  private final static QName _AddSaddadBillRequestCustomerName_QNAME = new QName(
      "http://tempuri.org/", "CustomerName");

  private final static QName _AddSaddadBillRequestIDNo_QNAME = new QName("http://tempuri.org/",
      "IDNo");
  private final static QName _AddSaddadBillRequestSaddadBillDetails_QNAME = new QName(
      "http://tempuri.org/", "SaddadBillDetails");

  public static String validateFields(Invoice arINvoice) {

    // intialize message
    errorMsg = new StringBuilder("");

    // Application Type Field validation
    if (arINvoice.getEfinApplicationtype() != null) {
      checkField(arINvoice.getEfinApplicationtype().getSearchKey(),
          ReceiveBillErrorConstant.EFIN_APPLICATION_TYPE);
    } else {
      errorMsg.append(
          ReceiveBillErrorConstant.COMMMA_SEPEARATOR + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.EFIN_APPLICATION_TYPE)
              + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
    }

    // CustomerId Type Field validation
    if (arINvoice.getEfinCustomeridtype() != null) {
      checkField(arINvoice.getEfinCustomeridtype().getCertificateName().getItemvalue() != null
          ? arINvoice.getEfinCustomeridtype().getCertificateName().getItemvalue().toString()
          : "", ReceiveBillErrorConstant.CUSTOMERIDVALUE);
    } else {
      errorMsg.append(
          ReceiveBillErrorConstant.COMMMA_SEPEARATOR + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.CUSTOMERIDTYPE)
              + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
    }

    // Customer Type Field validation
    if (arINvoice.getEfinCustomertype() != null) {
      checkField(arINvoice.getEfinCustomertype().getSearchKey(),
          ReceiveBillErrorConstant.EFIN_CUSTOMERTYPE);
    } else {
      errorMsg.append(
          ReceiveBillErrorConstant.COMMMA_SEPEARATOR + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.EFIN_CUSTOMERTYPE)
              + ReceiveBillErrorConstant.WORD_SEPEARATOR
              + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
    }

    // Customer Type Field validation
    if (arINvoice.getEfinElementvalue() != null) {
      checkField(arINvoice.getEfinElementvalue().getSearchKey(),
          ReceiveBillErrorConstant.MAINACCOUNT);
    } else {
      errorMsg.append(ReceiveBillErrorConstant.COMMMA_SEPEARATOR
          + ReceiveBillErrorConstant.WORD_SEPEARATOR
          + OBMessageUtils.messageBD(ReceiveBillErrorConstant.MAINACCOUNT)
          + ReceiveBillErrorConstant.WORD_SEPEARATOR
          + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY) + System.lineSeparator());
    }

    for (org.openbravo.model.common.invoice.InvoiceLine line : arINvoice.getInvoiceLineList()) {
      if (line.getEfinServiceitem() != null) {
        checkField(line.getEfinServiceitem().getSearchKey(), ReceiveBillErrorConstant.SERVICEITEM);
      } else {
        errorMsg.append(ReceiveBillErrorConstant.COMMMA_SEPEARATOR
            + OBMessageUtils.messageBD(ReceiveBillErrorConstant.SERVICEITEM) + " - "
            + +line.getLineNo() + ReceiveBillErrorConstant.WORD_SEPEARATOR
            + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
      }
    }
    log.debug("--Validation finish--");
    log.debug("Error Msg:" + errorMsg.toString());

    return errorMsg.toString().replaceFirst(",", " ");
  }

  /**
   * This method is use to create AddSaddadBillRequest from selected invoice
   * 
   * @param Invoice
   * @return AddSaddadBillRequest
   */

  public static AddSaddadBillRequest createSadadBillRequest(Invoice arINvoice) {

    log.debug("--createSadadBillRequest Block--");

    AddSaddadBillRequest request = new AddSaddadBillRequest();
    ArrayOfSaddadBillDetail arraySadad = new ArrayOfSaddadBillDetail();

    request.setApplicationType(Integer.parseInt(arINvoice.getEfinApplicationtype().getSearchKey()));
    request.setCustomerName(new JAXBElement<String>(_AddSaddadBillRequestCustomerName_QNAME,
        String.class, arINvoice.getBusinessPartner().getName()));
    request.setCustomerType(Integer.parseInt(arINvoice.getEfinCustomertype().getSearchKey()));
    request.setIDNo(new JAXBElement<String>(_AddSaddadBillRequestIDNo_QNAME, String.class,
        arINvoice.getEfinIdno()));
    request.setIDType(
        arINvoice.getEfinCustomeridtype().getCertificateName().getItemvalue().intValue());
    request.setMainAccount(Integer.parseInt(arINvoice.getEfinElementvalue().getSearchKey()));
    request.setMobileNo(new JAXBElement<String>(_AddSaddadBillRequestMobileNo_QNAME, String.class,
        arINvoice.getEfinMobileno()));
    request.setNotes(new JAXBElement<String>(_AddSaddadBillRequestNotes_QNAME, String.class,
        arINvoice.getDescription()));

    for (org.openbravo.model.common.invoice.InvoiceLine line : arINvoice.getInvoiceLineList()) {
      SaddadBillDetail detail = new SaddadBillDetail();
      detail.setBillAmount(line.getLineNetAmount());
      detail.setMOTSubAccount(Integer.parseInt(line.getEfinServiceitem().getSearchKey()));
      arraySadad.getSaddadBillDetail().add(detail);
    }
    request.setSaddadBillDetails(new JAXBElement<ArrayOfSaddadBillDetail>(
        _AddSaddadBillRequestSaddadBillDetails_QNAME, ArrayOfSaddadBillDetail.class, arraySadad));
    request.setTotalBillAmount(arINvoice.getGrandTotalAmount());

    log.debug("--createSadadBillRequest Block end--");
    log.debug("request:" + request);

    return request;
  }

  /**
   * This Method is used to form error message if any fields have invalid value
   * 
   * @param value
   * @param msgTxt
   */

  private static void checkField(String value, String msgTxt) {
    if (StringUtils.isNotEmpty(value)) {
      try {
        Integer.parseInt(value);
      } catch (NumberFormatException e) {
        errorMsg.append(ReceiveBillErrorConstant.COMMMA_SEPEARATOR
            + OBMessageUtils.messageBD(msgTxt) + ReceiveBillErrorConstant.WORD_SEPEARATOR
            + OBMessageUtils.messageBD(ReceiveBillErrorConstant.INTEGERTYPE));
      }
    } else {
      errorMsg.append(ReceiveBillErrorConstant.COMMMA_SEPEARATOR + OBMessageUtils.messageBD(msgTxt)
          + ReceiveBillErrorConstant.WORD_SEPEARATOR
          + OBMessageUtils.messageBD(ReceiveBillErrorConstant.NOTEMPTY));
    }
  }

  /**
   * This method is used to set fields from SADDAD response
   * 
   * @param request(AddSaddadBillResponse)
   * 
   * @return true or false
   */

  public static boolean setFieldValue(AddSaddadBillResponse response, Invoice invoice) {

    if (response.isHasError()) {
      log.debug("haserror =true");
      log.debug("error msg" + response.getErrorMessage().getValue());
      log.debug("new billnumber:" + response.getNewBillNumber());
      invoice.setEfinSadadhaserror(true);
      invoice.setEfinSadaderrormessage(
          response.getErrorMessage() != null ? response.getErrorMessage().getValue() : null);
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      return false;
    } else {
      log.debug("new billnumber:" + response.getNewBillNumber());
      invoice.setEfinSadadhaserror(false);
      invoice.setEfinSadaderrormessage(null);
      invoice.setEfinSadadnewbillnumber(
          response.getNewBillNumber() != null ? response.getNewBillNumber().toString() : "");
      invoice.setEfinSadadbillstatus("7");
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
    }

    return true;
  }

}
