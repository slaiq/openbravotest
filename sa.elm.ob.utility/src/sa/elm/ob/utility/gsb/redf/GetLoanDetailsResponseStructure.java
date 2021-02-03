
package sa.elm.ob.utility.gsb.redf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for GetLoanDetailsResponseStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="GetLoanDetailsResponseStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="GetLoanDetailsResponseDetailObject" type="{http://yefi.gov.sa/REDF/LoanInquirySchema/xml/schemas/version2.0}LoanInformationStructure" minOccurs="0" form="unqualified"/>
 *           &lt;element ref="{http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3}ServiceError" minOccurs="0"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "GetLoanDetailsResponseStructure", namespace = "http://yesser.gov.sa/REDF/LoanInquiryService/version/2.0", propOrder = {
    "getLoanDetailsResponseDetailObject",
    "serviceError"
})
public class GetLoanDetailsResponseStructure {

    @XmlElement(name = "GetLoanDetailsResponseDetailObject", namespace = "")
    protected LoanInformationStructure getLoanDetailsResponseDetailObject;
    @XmlElement(name = "ServiceError", namespace = "http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3")
    protected CommonErrorStructure serviceError;

    /**
     * Gets the value of the getLoanDetailsResponseDetailObject property.
     * 
     * @return
     *     possible object is
     *     {@link LoanInformationStructure }
     *     
     */
    public LoanInformationStructure getGetLoanDetailsResponseDetailObject() {
        return getLoanDetailsResponseDetailObject;
    }

    /**
     * Sets the value of the getLoanDetailsResponseDetailObject property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoanInformationStructure }
     *     
     */
    public void setGetLoanDetailsResponseDetailObject(LoanInformationStructure value) {
        this.getLoanDetailsResponseDetailObject = value;
    }

    /**
     * Gets the value of the serviceError property.
     * 
     * @return
     *     possible object is
     *     {@link CommonErrorStructure }
     *     
     */
    public CommonErrorStructure getServiceError() {
        return serviceError;
    }

    /**
     * Sets the value of the serviceError property.
     * 
     * @param value
     *     allowed object is
     *     {@link CommonErrorStructure }
     *     
     */
    public void setServiceError(CommonErrorStructure value) {
        this.serviceError = value;
    }

}
