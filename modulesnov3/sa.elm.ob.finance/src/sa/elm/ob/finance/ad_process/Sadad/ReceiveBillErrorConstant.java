package sa.elm.ob.finance.ad_process.Sadad;

public class ReceiveBillErrorConstant {

  public final static String CLIENT_ERROR = "There is no client defined with the id defined in openbravo propoerties";
  public final static String CONFIGURATION_ERROR = "There is no Sadad Receive Bill Configuration for this client";
  public final static String USER_ERROR = "There is no user found in client based on the username";
  public final static String BP_ERROR = "There is no customer based on the idno from request";
  public final static String LOCATION_ERROR = "There is no location found for the customer";
  public final static String BUDGETDEF_ERROR = "There is no Budget defintion found for today's date";
  public final static String MAINACCOUNT_ERROR = "There is no main account found based on request";
  public final static String CUSTOMERTYPE_ERROR = "There is no customertype account found based on reques";
  public final static String CUSTOMERIDTYPE_ERROR = "There is no customerid type account found based on request";
  public final static String APPLICATION_ERROR = "There is no application type based on request";
  public final static String SERVICEITEM_ERROR = "There is no service item based on the request";
  public final static String CURRENCY_ERROR = "There is no currency defined ";
  public final static String UNIQUECODE_ERROR = "There is no unique matched on sadad defaults defined ";
  public final static String PRICELIST_ERROR = "There is no sales pricelist in the system";
  public final static String SEQUENCE_ERROR = "There is no sequence defined for document type selected in sadad defaults";

  public final static String DELETION_SUCCESS = "DeletedSuccessfully";
  public final static String DELETION_FAIL = "Error_NotDeleted";
  public final static String EFIN_DELETESUCCESS = "@Efin_deleteBill_Success@";
  public final static String EFIN_DELETENOBILL = "@Efin_cantdeletenobillnumber@";
  public final static String EFIN_IDSHOULDBENO = "@Efin_idshouldbeno@";
  public final static String EFIN_DELETION_SUCCESSTATUS = "1";
  public final static String EFIN_DELETION_FAILSTATUS = "2";

  public final static String USER_ID = "100";
  public final static String CUSTOMER_TYPE = "CTYPE";
  public final static String APPLICATION_TYPE = "APP";

  public final static String INTERNAL_SERVER_ERRORCODE = "500";
  public final static String NO_CONNECTION = "Failed to access the WSDL";

  public final static String INVOICE_ID = "C_Invoice_ID";
  public final static String ERROR = "Error";
  public final static String WARNING = "Warning";
  public final static String SUCCESS = "Success";

  public final static String ADDBILLFAILED = "@Efin_Addedbill_Failed@";
  public final static String ADDBILLERROR = "@Efin_Addedbill_witherror@";
  public final static String ADDBILLSUCCESS = "@Efin_Addedbill_Success@";

  public final static String SADDADCONNNECTIONFAILED = "@Efin_failedsadadconn@";
  public final static String SADDADCONNNECTIONFAILED_MSG = "@Efin_failedsadadconn@";

  public final static String EFIN_APPLICATION_TYPE = "Efin_applicationtype";
  public final static String NOTEMPTY = "Efin_Notempty";
  public final static String CUSTOMERIDVALUE = "Efin_Customeridvalue";
  public final static String CUSTOMERIDTYPE = "Efin_Customeridtype";
  public final static String EFIN_CUSTOMERTYPE = "Efin_Customertype";
  public final static String SERVICEITEM = "Efin_serviceitem";
  public final static String MAINACCOUNT = "Efin_Mainaccount";
  public final static String WORD_SEPEARATOR = " ";
  public final static String COMMMA_SEPEARATOR = ", ";

  public final static String INTEGERTYPE = "Efin_integertype";

  public final static String BILLNOTEMPTY = "@EFIN_billnoempty@";
  public final static String RESPONSEEMPTY = "EFIN_Getstatusresempty";
  public final static String RESPONSEEMPTY_MSG = "@EFIN_Getstatusresempty@";
  public final static String ERROR_MSG = "errormsg";
  public final static String STATUS_HASERROR = "EFIN_Getstatushaserror";
  public final static String STATUS_HASERRORMSG = "@EFIN_Getstatushaserror@";
  public final static String STATUS_SUCCESS = "Efin_getstatussuccess";
  public final static String STATUS_SUCCESS_MSG = "@Efin_getstatussuccess@";
  public final static String STATUS_ADDRECEIPT_FAIL = "Efin_getstatusaddreceipfail";
  public final static String STATUS_ADDRECEIPT_FAIL_MSG = "@Efin_getstatusaddreceipfail@";

  public final static String EMPTY_RESPONSE = "Efin_EmptyResponse";
  public final static String DOCUMENTNO_ERROR = "Document no is String it should be integer";

}
