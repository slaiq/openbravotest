
package sa.elm.ob.utility.sadad.consumer.org.tempuri;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

import org.tempuri.GetSaddadUnPaidBillsRequest;

import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfApplicationTypeInfo;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfCustomerTypeInfo;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfIdTypeInfo;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfMOTSubAccountInfo;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfMainAccountInfo;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfSaddadBillDetail;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ArrayOfSaddadBillInfo;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.SaddadBillInfo;
import sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad.ServiceAccount;

/**
 * This object contains factory methods for each Java content interface and Java element interface
 * generated in the org.tempuri package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation
 * for XML content. The Java representation of XML content can consist of schema derived interfaces
 * and classes representing the binding of schema type definitions, element declarations and model
 * groups. Factory methods for each of these are provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

  private final static QName _ServiceAccount_QNAME = new QName("http://tempuri.org/",
      "ServiceAccount");
  private final static QName _GetSaddadBillResponseErrorMessage_QNAME = new QName(
      "http://tempuri.org/", "ErrorMessage");
  private final static QName _GetSaddadBillResponseSaddadBillInfo_QNAME = new QName(
      "http://tempuri.org/", "SaddadBillInfo");
  private final static QName _AddSaddadBillRequestCustomerName_QNAME = new QName(
      "http://tempuri.org/", "CustomerName");
  private final static QName _AddSaddadBillRequestIDNo_QNAME = new QName("http://tempuri.org/",
      "IDNo");
  private final static QName _AddSaddadBillRequestMobileNo_QNAME = new QName("http://tempuri.org/",
      "MobileNo");
  private final static QName _AddSaddadBillRequestNotes_QNAME = new QName("http://tempuri.org/",
      "Notes");
  private final static QName _AddSaddadBillRequestSaddadBillDetails_QNAME = new QName(
      "http://tempuri.org/", "SaddadBillDetails");
  private final static QName _GetSaddadUnPaidBillsResponseSaddadUnPaidBills_QNAME = new QName(
      "http://tempuri.org/", "SaddadUnPaidBills");
  private final static QName _DeleteSaddadBillRequestDeleteReasons_QNAME = new QName(
      "http://tempuri.org/", "DeleteReasons");
  private final static QName _MainAccountsResponseMainAccounts_QNAME = new QName(
      "http://tempuri.org/", "MainAccounts");
  private final static QName _MOTSubAccountsResponseMOTSubAccounts_QNAME = new QName(
      "http://tempuri.org/", "MOT_SubAccounts");
  private final static QName _CustomerTypesResponseCustomerTypes_QNAME = new QName(
      "http://tempuri.org/", "CustomerTypes");
  private final static QName _IdTypesResponseIdTypes_QNAME = new QName("http://tempuri.org/",
      "IdTypes");
  private final static QName _ApplicationTypesResponseApplicationTypes_QNAME = new QName(
      "http://tempuri.org/", "ApplicationTypes");

  /**
   * Create a new ObjectFactory that can be used to create new instances of schema derived classes
   * for package: org.tempuri
   * 
   */
  public ObjectFactory() {
  }

  /**
   * Create an instance of {@link GetSaddadBillRequest }
   * 
   */
  public GetSaddadBillRequest createGetSaddadBillRequest() {
    return new GetSaddadBillRequest();
  }

  /**
   * Create an instance of {@link GetSaddadBillResponse }
   * 
   */
  public GetSaddadBillResponse createGetSaddadBillResponse() {
    return new GetSaddadBillResponse();
  }

  /**
   * Create an instance of {@link AddSaddadBillRequest }
   * 
   */
  public AddSaddadBillRequest createAddSaddadBillRequest() {
    return new AddSaddadBillRequest();
  }

  /**
   * Create an instance of {@link AddSaddadBillResponse }
   * 
   */
  public AddSaddadBillResponse createAddSaddadBillResponse() {
    return new AddSaddadBillResponse();
  }

  /**
   * Create an instance of {@link GetSaddadUnPaidBillsCountRequest }
   * 
   */
  public GetSaddadUnPaidBillsCountRequest createGetSaddadUnPaidBillsCountRequest() {
    return new GetSaddadUnPaidBillsCountRequest();
  }

  /**
   * Create an instance of {@link GetSaddadUnPaidBillsCountResponse }
   * 
   */
  public GetSaddadUnPaidBillsCountResponse createGetSaddadUnPaidBillsCountResponse() {
    return new GetSaddadUnPaidBillsCountResponse();
  }

  /**
   * Create an instance of {@link GetSaddadUnPaidBillsRequest }
   * 
   */
  public GetSaddadUnPaidBillsRequest createGetSaddadUnPaidBillsRequest() {
    return new GetSaddadUnPaidBillsRequest();
  }

  /**
   * Create an instance of {@link GetSaddadUnPaidBillsResponse }
   * 
   */
  public GetSaddadUnPaidBillsResponse createGetSaddadUnPaidBillsResponse() {
    return new GetSaddadUnPaidBillsResponse();
  }

  /**
   * Create an instance of {@link DeleteSaddadBillRequest }
   * 
   */
  public DeleteSaddadBillRequest createDeleteSaddadBillRequest() {
    return new DeleteSaddadBillRequest();
  }

  /**
   * Create an instance of {@link DeleteSaddadBillResponse }
   * 
   */
  public DeleteSaddadBillResponse createDeleteSaddadBillResponse() {
    return new DeleteSaddadBillResponse();
  }

  /**
   * Create an instance of {@link GetAllMainAccountsRequest }
   * 
   */
  public GetAllMainAccountsRequest createGetAllMainAccountsRequest() {
    return new GetAllMainAccountsRequest();
  }

  /**
   * Create an instance of {@link MainAccountsResponse }
   * 
   */
  public MainAccountsResponse createMainAccountsResponse() {
    return new MainAccountsResponse();
  }

  /**
   * Create an instance of {@link GetMainAccountByIdRequest }
   * 
   */
  public GetMainAccountByIdRequest createGetMainAccountByIdRequest() {
    return new GetMainAccountByIdRequest();
  }

  /**
   * Create an instance of {@link GetAllMOTSubAccountsRequest }
   * 
   */
  public GetAllMOTSubAccountsRequest createGetAllMOTSubAccountsRequest() {
    return new GetAllMOTSubAccountsRequest();
  }

  /**
   * Create an instance of {@link MOTSubAccountsResponse }
   * 
   */
  public MOTSubAccountsResponse createMOTSubAccountsResponse() {
    return new MOTSubAccountsResponse();
  }

  /**
   * Create an instance of {@link GetMOTSubAccountsByIdRequest }
   * 
   */
  public GetMOTSubAccountsByIdRequest createGetMOTSubAccountsByIdRequest() {
    return new GetMOTSubAccountsByIdRequest();
  }

  /**
   * Create an instance of {@link GetMOTSubAccountsByApplicationTypeRequest }
   * 
   */
  public GetMOTSubAccountsByApplicationTypeRequest createGetMOTSubAccountsByApplicationTypeRequest() {
    return new GetMOTSubAccountsByApplicationTypeRequest();
  }

  /**
   * Create an instance of {@link GetAllCustomerTypesRequest }
   * 
   */
  public GetAllCustomerTypesRequest createGetAllCustomerTypesRequest() {
    return new GetAllCustomerTypesRequest();
  }

  /**
   * Create an instance of {@link CustomerTypesResponse }
   * 
   */
  public CustomerTypesResponse createCustomerTypesResponse() {
    return new CustomerTypesResponse();
  }

  /**
   * Create an instance of {@link GetAllIdTypesRequest }
   * 
   */
  public GetAllIdTypesRequest createGetAllIdTypesRequest() {
    return new GetAllIdTypesRequest();
  }

  /**
   * Create an instance of {@link IdTypesResponse }
   * 
   */
  public IdTypesResponse createIdTypesResponse() {
    return new IdTypesResponse();
  }

  /**
   * Create an instance of {@link GetAllApplicationTypesRequest }
   * 
   */
  public GetAllApplicationTypesRequest createGetAllApplicationTypesRequest() {
    return new GetAllApplicationTypesRequest();
  }

  /**
   * Create an instance of {@link ApplicationTypesResponse }
   * 
   */
  public ApplicationTypesResponse createApplicationTypesResponse() {
    return new ApplicationTypesResponse();
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ServiceAccount }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ServiceAccount")
  public JAXBElement<ServiceAccount> createServiceAccount(ServiceAccount value) {
    return new JAXBElement<ServiceAccount>(_ServiceAccount_QNAME, ServiceAccount.class, null,
        value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = GetSaddadBillResponse.class)
  public JAXBElement<String> createGetSaddadBillResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        GetSaddadBillResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link SaddadBillInfo }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "SaddadBillInfo", scope = GetSaddadBillResponse.class)
  public JAXBElement<SaddadBillInfo> createGetSaddadBillResponseSaddadBillInfo(
      SaddadBillInfo value) {
    return new JAXBElement<SaddadBillInfo>(_GetSaddadBillResponseSaddadBillInfo_QNAME,
        SaddadBillInfo.class, GetSaddadBillResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "CustomerName", scope = AddSaddadBillRequest.class)
  public JAXBElement<String> createAddSaddadBillRequestCustomerName(String value) {
    return new JAXBElement<String>(_AddSaddadBillRequestCustomerName_QNAME, String.class,
        AddSaddadBillRequest.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "IDNo", scope = AddSaddadBillRequest.class)
  public JAXBElement<String> createAddSaddadBillRequestIDNo(String value) {
    return new JAXBElement<String>(_AddSaddadBillRequestIDNo_QNAME, String.class,
        AddSaddadBillRequest.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "MobileNo", scope = AddSaddadBillRequest.class)
  public JAXBElement<String> createAddSaddadBillRequestMobileNo(String value) {
    return new JAXBElement<String>(_AddSaddadBillRequestMobileNo_QNAME, String.class,
        AddSaddadBillRequest.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "Notes", scope = AddSaddadBillRequest.class)
  public JAXBElement<String> createAddSaddadBillRequestNotes(String value) {
    return new JAXBElement<String>(_AddSaddadBillRequestNotes_QNAME, String.class,
        AddSaddadBillRequest.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfSaddadBillDetail }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "SaddadBillDetails", scope = AddSaddadBillRequest.class)
  public JAXBElement<ArrayOfSaddadBillDetail> createAddSaddadBillRequestSaddadBillDetails(
      ArrayOfSaddadBillDetail value) {
    return new JAXBElement<ArrayOfSaddadBillDetail>(_AddSaddadBillRequestSaddadBillDetails_QNAME,
        ArrayOfSaddadBillDetail.class, AddSaddadBillRequest.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = AddSaddadBillResponse.class)
  public JAXBElement<String> createAddSaddadBillResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        AddSaddadBillResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "IDNo", scope = GetSaddadUnPaidBillsCountRequest.class)
  public JAXBElement<String> createGetSaddadUnPaidBillsCountRequestIDNo(String value) {
    return new JAXBElement<String>(_AddSaddadBillRequestIDNo_QNAME, String.class,
        GetSaddadUnPaidBillsCountRequest.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = GetSaddadUnPaidBillsCountResponse.class)
  public JAXBElement<String> createGetSaddadUnPaidBillsCountResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        GetSaddadUnPaidBillsCountResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "IDNo", scope = GetSaddadUnPaidBillsRequest.class)
  public JAXBElement<String> createGetSaddadUnPaidBillsRequestIDNo(String value) {
    return new JAXBElement<String>(_AddSaddadBillRequestIDNo_QNAME, String.class,
        GetSaddadUnPaidBillsRequest.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = GetSaddadUnPaidBillsResponse.class)
  public JAXBElement<String> createGetSaddadUnPaidBillsResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        GetSaddadUnPaidBillsResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfSaddadBillInfo }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "SaddadUnPaidBills", scope = GetSaddadUnPaidBillsResponse.class)
  public JAXBElement<ArrayOfSaddadBillInfo> createGetSaddadUnPaidBillsResponseSaddadUnPaidBills(
      ArrayOfSaddadBillInfo value) {
    return new JAXBElement<ArrayOfSaddadBillInfo>(
        _GetSaddadUnPaidBillsResponseSaddadUnPaidBills_QNAME, ArrayOfSaddadBillInfo.class,
        GetSaddadUnPaidBillsResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "DeleteReasons", scope = DeleteSaddadBillRequest.class)
  public JAXBElement<String> createDeleteSaddadBillRequestDeleteReasons(String value) {
    return new JAXBElement<String>(_DeleteSaddadBillRequestDeleteReasons_QNAME, String.class,
        DeleteSaddadBillRequest.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = DeleteSaddadBillResponse.class)
  public JAXBElement<String> createDeleteSaddadBillResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        DeleteSaddadBillResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = MainAccountsResponse.class)
  public JAXBElement<String> createMainAccountsResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        MainAccountsResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfMainAccountInfo }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "MainAccounts", scope = MainAccountsResponse.class)
  public JAXBElement<ArrayOfMainAccountInfo> createMainAccountsResponseMainAccounts(
      ArrayOfMainAccountInfo value) {
    return new JAXBElement<ArrayOfMainAccountInfo>(_MainAccountsResponseMainAccounts_QNAME,
        ArrayOfMainAccountInfo.class, MainAccountsResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = MOTSubAccountsResponse.class)
  public JAXBElement<String> createMOTSubAccountsResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        MOTSubAccountsResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfMOTSubAccountInfo }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "MOT_SubAccounts", scope = MOTSubAccountsResponse.class)
  public JAXBElement<ArrayOfMOTSubAccountInfo> createMOTSubAccountsResponseMOTSubAccounts(
      ArrayOfMOTSubAccountInfo value) {
    return new JAXBElement<ArrayOfMOTSubAccountInfo>(_MOTSubAccountsResponseMOTSubAccounts_QNAME,
        ArrayOfMOTSubAccountInfo.class, MOTSubAccountsResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfCustomerTypeInfo }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "CustomerTypes", scope = CustomerTypesResponse.class)
  public JAXBElement<ArrayOfCustomerTypeInfo> createCustomerTypesResponseCustomerTypes(
      ArrayOfCustomerTypeInfo value) {
    return new JAXBElement<ArrayOfCustomerTypeInfo>(_CustomerTypesResponseCustomerTypes_QNAME,
        ArrayOfCustomerTypeInfo.class, CustomerTypesResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = CustomerTypesResponse.class)
  public JAXBElement<String> createCustomerTypesResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        CustomerTypesResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = IdTypesResponse.class)
  public JAXBElement<String> createIdTypesResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        IdTypesResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfIdTypeInfo }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "IdTypes", scope = IdTypesResponse.class)
  public JAXBElement<ArrayOfIdTypeInfo> createIdTypesResponseIdTypes(ArrayOfIdTypeInfo value) {
    return new JAXBElement<ArrayOfIdTypeInfo>(_IdTypesResponseIdTypes_QNAME,
        ArrayOfIdTypeInfo.class, IdTypesResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfApplicationTypeInfo
   * }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ApplicationTypes", scope = ApplicationTypesResponse.class)
  public JAXBElement<ArrayOfApplicationTypeInfo> createApplicationTypesResponseApplicationTypes(
      ArrayOfApplicationTypeInfo value) {
    return new JAXBElement<ArrayOfApplicationTypeInfo>(
        _ApplicationTypesResponseApplicationTypes_QNAME, ArrayOfApplicationTypeInfo.class,
        ApplicationTypesResponse.class, value);
  }

  /**
   * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
   * 
   */
  @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = ApplicationTypesResponse.class)
  public JAXBElement<String> createApplicationTypesResponseErrorMessage(String value) {
    return new JAXBElement<String>(_GetSaddadBillResponseErrorMessage_QNAME, String.class,
        ApplicationTypesResponse.class, value);
  }

}
