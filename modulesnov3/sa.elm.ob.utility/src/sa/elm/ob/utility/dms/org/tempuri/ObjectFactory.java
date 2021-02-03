
package sa.elm.ob.utility.dms.org.tempuri;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

import sa.elm.ob.utility.dms.com.microsoft.schemas._2003._10.serialization.arrays.ArrayOfint;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ArrayOfXmlAttribute;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.AttachmentsBulkRoot;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ResponseRoot;
import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ServiceAccount;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.tempuri package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _ServiceAccount_QNAME = new QName("http://tempuri.org/", "ServiceAccount");
    private final static QName _Token_QNAME = new QName("http://tempuri.org/", "Token");
    private final static QName _DmsClientID_QNAME = new QName("http://tempuri.org/", "DmsClientID");
    private final static QName _IPAddress_QNAME = new QName("http://tempuri.org/", "IPAddress");
    private final static QName _CreateRecordWithAttachmentRequestDescription_QNAME = new QName("http://tempuri.org/", "Description");
    private final static QName _CreateRecordWithAttachmentRequestXmlAttributes_QNAME = new QName("http://tempuri.org/", "XmlAttributes");
    private final static QName _CreateRecordWithAttachmentRequestProfileURI_QNAME = new QName("http://tempuri.org/", "ProfileURI");
    private final static QName _CreateRecordWithAttachmentRequestAttachmentBase64_QNAME = new QName("http://tempuri.org/", "AttachmentBase64");
    private final static QName _CreateRecordWithAttachmentRequestNodeID_QNAME = new QName("http://tempuri.org/", "NodeID");
    private final static QName _CreateRecordWithAttachmentRequestDocumentName_QNAME = new QName("http://tempuri.org/", "DocumentName");
    private final static QName _ResponseResponseRoot_QNAME = new QName("http://tempuri.org/", "ResponseRoot");
    private final static QName _ResponseErrorMessage_QNAME = new QName("http://tempuri.org/", "ErrorMessage");
    private final static QName _GetAttachmentsBulkRequestAttachmentIDsList_QNAME = new QName("http://tempuri.org/", "AttachmentIDsList");
    private final static QName _AttachmentsBulkResponseRoot_QNAME = new QName("http://tempuri.org/", "root");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.tempuri
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link BuildTreeRequest }
     * 
     */
    public BuildTreeRequest createBuildTreeRequest() {
        return new BuildTreeRequest();
    }

    /**
     * Create an instance of {@link GetAttachmentsBulkRequest }
     * 
     */
    public GetAttachmentsBulkRequest createGetAttachmentsBulkRequest() {
        return new GetAttachmentsBulkRequest();
    }

    /**
     * Create an instance of {@link CreateRecordWithAttachmentRequest }
     * 
     */
    public CreateRecordWithAttachmentRequest createCreateRecordWithAttachmentRequest() {
        return new CreateRecordWithAttachmentRequest();
    }

    /**
     * Create an instance of {@link DeleteAttachmentRequest }
     * 
     */
    public DeleteAttachmentRequest createDeleteAttachmentRequest() {
        return new DeleteAttachmentRequest();
    }

    /**
     * Create an instance of {@link AddVersionAttachmentRequest }
     * 
     */
    public AddVersionAttachmentRequest createAddVersionAttachmentRequest() {
        return new AddVersionAttachmentRequest();
    }

    /**
     * Create an instance of {@link Response }
     * 
     */
    public Response createResponse() {
        return new Response();
    }

    /**
     * Create an instance of {@link CreateRecordRequest }
     * 
     */
    public CreateRecordRequest createCreateRecordRequest() {
        return new CreateRecordRequest();
    }

    /**
     * Create an instance of {@link DeleteRecordRequest }
     * 
     */
    public DeleteRecordRequest createDeleteRecordRequest() {
        return new DeleteRecordRequest();
    }

    /**
     * Create an instance of {@link AddAttachmentRequest }
     * 
     */
    public AddAttachmentRequest createAddAttachmentRequest() {
        return new AddAttachmentRequest();
    }

    /**
     * Create an instance of {@link AttachmentsBulkResponse }
     * 
     */
    public AttachmentsBulkResponse createAttachmentsBulkResponse() {
        return new AttachmentsBulkResponse();
    }

    /**
     * Create an instance of {@link ModifyRecordRequest }
     * 
     */
    public ModifyRecordRequest createModifyRecordRequest() {
        return new ModifyRecordRequest();
    }

    /**
     * Create an instance of {@link GetRecordRequest }
     * 
     */
    public GetRecordRequest createGetRecordRequest() {
        return new GetRecordRequest();
    }

    /**
     * Create an instance of {@link GetAttachmentRequest }
     * 
     */
    public GetAttachmentRequest createGetAttachmentRequest() {
        return new GetAttachmentRequest();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ServiceAccount }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ServiceAccount")
    public JAXBElement<ServiceAccount> createServiceAccount(ServiceAccount value) {
        return new JAXBElement<ServiceAccount>(_ServiceAccount_QNAME, ServiceAccount.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "Token")
    public JAXBElement<String> createToken(String value) {
        return new JAXBElement<String>(_Token_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "DmsClientID")
    public JAXBElement<String> createDmsClientID(String value) {
        return new JAXBElement<String>(_DmsClientID_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "IPAddress")
    public JAXBElement<String> createIPAddress(String value) {
        return new JAXBElement<String>(_IPAddress_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "Description", scope = CreateRecordWithAttachmentRequest.class)
    public JAXBElement<String> createCreateRecordWithAttachmentRequestDescription(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestDescription_QNAME, String.class, CreateRecordWithAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfXmlAttribute }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "XmlAttributes", scope = CreateRecordWithAttachmentRequest.class)
    public JAXBElement<ArrayOfXmlAttribute> createCreateRecordWithAttachmentRequestXmlAttributes(ArrayOfXmlAttribute value) {
        return new JAXBElement<ArrayOfXmlAttribute>(_CreateRecordWithAttachmentRequestXmlAttributes_QNAME, ArrayOfXmlAttribute.class, CreateRecordWithAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = CreateRecordWithAttachmentRequest.class)
    public JAXBElement<String> createCreateRecordWithAttachmentRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, CreateRecordWithAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "AttachmentBase64", scope = CreateRecordWithAttachmentRequest.class)
    public JAXBElement<String> createCreateRecordWithAttachmentRequestAttachmentBase64(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestAttachmentBase64_QNAME, String.class, CreateRecordWithAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "NodeID", scope = CreateRecordWithAttachmentRequest.class)
    public JAXBElement<String> createCreateRecordWithAttachmentRequestNodeID(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestNodeID_QNAME, String.class, CreateRecordWithAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "DocumentName", scope = CreateRecordWithAttachmentRequest.class)
    public JAXBElement<String> createCreateRecordWithAttachmentRequestDocumentName(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestDocumentName_QNAME, String.class, CreateRecordWithAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = GetAttachmentRequest.class)
    public JAXBElement<String> createGetAttachmentRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, GetAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = DeleteAttachmentRequest.class)
    public JAXBElement<String> createDeleteAttachmentRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, DeleteAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "Description", scope = AddAttachmentRequest.class)
    public JAXBElement<String> createAddAttachmentRequestDescription(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestDescription_QNAME, String.class, AddAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = AddAttachmentRequest.class)
    public JAXBElement<String> createAddAttachmentRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, AddAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "AttachmentBase64", scope = AddAttachmentRequest.class)
    public JAXBElement<String> createAddAttachmentRequestAttachmentBase64(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestAttachmentBase64_QNAME, String.class, AddAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "DocumentName", scope = AddAttachmentRequest.class)
    public JAXBElement<String> createAddAttachmentRequestDocumentName(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestDocumentName_QNAME, String.class, AddAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfXmlAttribute }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "XmlAttributes", scope = CreateRecordRequest.class)
    public JAXBElement<ArrayOfXmlAttribute> createCreateRecordRequestXmlAttributes(ArrayOfXmlAttribute value) {
        return new JAXBElement<ArrayOfXmlAttribute>(_CreateRecordWithAttachmentRequestXmlAttributes_QNAME, ArrayOfXmlAttribute.class, CreateRecordRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = CreateRecordRequest.class)
    public JAXBElement<String> createCreateRecordRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, CreateRecordRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "NodeID", scope = CreateRecordRequest.class)
    public JAXBElement<String> createCreateRecordRequestNodeID(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestNodeID_QNAME, String.class, CreateRecordRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = GetRecordRequest.class)
    public JAXBElement<String> createGetRecordRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, GetRecordRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfXmlAttribute }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "XmlAttributes", scope = ModifyRecordRequest.class)
    public JAXBElement<ArrayOfXmlAttribute> createModifyRecordRequestXmlAttributes(ArrayOfXmlAttribute value) {
        return new JAXBElement<ArrayOfXmlAttribute>(_CreateRecordWithAttachmentRequestXmlAttributes_QNAME, ArrayOfXmlAttribute.class, ModifyRecordRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = ModifyRecordRequest.class)
    public JAXBElement<String> createModifyRecordRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, ModifyRecordRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResponseRoot }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ResponseRoot", scope = Response.class)
    public JAXBElement<ResponseRoot> createResponseResponseRoot(ResponseRoot value) {
        return new JAXBElement<ResponseRoot>(_ResponseResponseRoot_QNAME, ResponseRoot.class, Response.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = Response.class)
    public JAXBElement<String> createResponseErrorMessage(String value) {
        return new JAXBElement<String>(_ResponseErrorMessage_QNAME, String.class, Response.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ArrayOfint }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "AttachmentIDsList", scope = GetAttachmentsBulkRequest.class)
    public JAXBElement<ArrayOfint> createGetAttachmentsBulkRequestAttachmentIDsList(ArrayOfint value) {
        return new JAXBElement<ArrayOfint>(_GetAttachmentsBulkRequestAttachmentIDsList_QNAME, ArrayOfint.class, GetAttachmentsBulkRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "Description", scope = AddVersionAttachmentRequest.class)
    public JAXBElement<String> createAddVersionAttachmentRequestDescription(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestDescription_QNAME, String.class, AddVersionAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = AddVersionAttachmentRequest.class)
    public JAXBElement<String> createAddVersionAttachmentRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, AddVersionAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "AttachmentBase64", scope = AddVersionAttachmentRequest.class)
    public JAXBElement<String> createAddVersionAttachmentRequestAttachmentBase64(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestAttachmentBase64_QNAME, String.class, AddVersionAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "DocumentName", scope = AddVersionAttachmentRequest.class)
    public JAXBElement<String> createAddVersionAttachmentRequestDocumentName(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestDocumentName_QNAME, String.class, AddVersionAttachmentRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ProfileURI", scope = DeleteRecordRequest.class)
    public JAXBElement<String> createDeleteRecordRequestProfileURI(String value) {
        return new JAXBElement<String>(_CreateRecordWithAttachmentRequestProfileURI_QNAME, String.class, DeleteRecordRequest.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttachmentsBulkRoot }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "root", scope = AttachmentsBulkResponse.class)
    public JAXBElement<AttachmentsBulkRoot> createAttachmentsBulkResponseRoot(AttachmentsBulkRoot value) {
        return new JAXBElement<AttachmentsBulkRoot>(_AttachmentsBulkResponseRoot_QNAME, AttachmentsBulkRoot.class, AttachmentsBulkResponse.class, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "ErrorMessage", scope = AttachmentsBulkResponse.class)
    public JAXBElement<String> createAttachmentsBulkResponseErrorMessage(String value) {
        return new JAXBElement<String>(_ResponseErrorMessage_QNAME, String.class, AttachmentsBulkResponse.class, value);
    }

}
