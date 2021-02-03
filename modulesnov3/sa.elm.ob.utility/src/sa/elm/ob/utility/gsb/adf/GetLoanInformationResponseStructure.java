
package sa.elm.ob.utility.gsb.adf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getLoanInformationResponseStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getLoanInformationResponseStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element name="getLoanInformationResponseDetailObject" type="{http://yefi.gov.sa/ADF/ClientFinancialStatusSchema/xml/schemas/version1.0}LoanInfoListStructure" minOccurs="0" form="unqualified"/>
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
@XmlType(name = "getLoanInformationResponseStructure", namespace = "http://yesser.gov.sa/ADF/ClientFinancialStatusService/version/1.0", propOrder = {
    "getLoanInformationResponseDetailObject",
    "serviceError"
})
public class GetLoanInformationResponseStructure {

    @XmlElement(namespace = "")
    protected LoanInfoListStructure getLoanInformationResponseDetailObject;
    @XmlElement(name = "ServiceError", namespace = "http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3")
    protected CommonErrorStructure serviceError;

    /**
     * Gets the value of the getLoanInformationResponseDetailObject property.
     * 
     * @return
     *     possible object is
     *     {@link LoanInfoListStructure }
     *     
     */
    public LoanInfoListStructure getGetLoanInformationResponseDetailObject() {
        return getLoanInformationResponseDetailObject;
    }

    /**
     * Sets the value of the getLoanInformationResponseDetailObject property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoanInfoListStructure }
     *     
     */
    public void setGetLoanInformationResponseDetailObject(LoanInfoListStructure value) {
        this.getLoanInformationResponseDetailObject = value;
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
