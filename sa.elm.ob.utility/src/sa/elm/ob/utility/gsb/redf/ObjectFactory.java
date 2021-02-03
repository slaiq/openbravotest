
package sa.elm.ob.utility.gsb.redf;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the sa.elm.ob.utility.gsb.redf package. 
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

    private final static QName _CommonErrorElement_QNAME = new QName("http://tempuri.org/", "commonErrorElement");
    private final static QName _ServiceError_QNAME = new QName("http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3", "ServiceError");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: sa.elm.ob.utility.gsb.redf
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link CommonErrorStructure }
     * 
     */
    public CommonErrorStructure createCommonErrorStructure() {
        return new CommonErrorStructure();
    }

    /**
     * Create an instance of {@link LoanInformationStructure }
     * 
     */
    public LoanInformationStructure createLoanInformationStructure() {
        return new LoanInformationStructure();
    }

    /**
     * Create an instance of {@link GetLoanDetailsResponse }
     * 
     */
    public GetLoanDetailsResponse createGetLoanDetailsResponse() {
        return new GetLoanDetailsResponse();
    }

    /**
     * Create an instance of {@link GetLoanDetailsResponseStructure }
     * 
     */
    public GetLoanDetailsResponseStructure createGetLoanDetailsResponseStructure() {
        return new GetLoanDetailsResponseStructure();
    }

    /**
     * Create an instance of {@link GetLoanDetails }
     * 
     */
    public GetLoanDetails createGetLoanDetails() {
        return new GetLoanDetails();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CommonErrorStructure }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://tempuri.org/", name = "commonErrorElement")
    public JAXBElement<CommonErrorStructure> createCommonErrorElement(CommonErrorStructure value) {
        return new JAXBElement<CommonErrorStructure>(_CommonErrorElement_QNAME, CommonErrorStructure.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CommonErrorStructure }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3", name = "ServiceError")
    public JAXBElement<CommonErrorStructure> createServiceError(CommonErrorStructure value) {
        return new JAXBElement<CommonErrorStructure>(_ServiceError_QNAME, CommonErrorStructure.class, null, value);
    }

}
