
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getLoanInfoResponseStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getLoanInfoResponseStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element ref="{http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3}ServiceError" minOccurs="0"/>
 *           &lt;element name="getLoanInfoResponseDetailObject" type="{http://yefi.gov.sa/SCSB/LoanInformation/xml/schemas/version1.0}LoanInfoListStructure" minOccurs="0" form="unqualified"/>
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
@XmlType(name = "getLoanInfoResponseStructure", namespace = "http://yesser.gov.sa/SCSB/LoanInformationService/version/1.0", propOrder = {
    "serviceError",
    "getLoanInfoResponseDetailObject"
})
public class GetLoanInfoResponseStructure {

    @XmlElement(name = "ServiceError", namespace = "http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3")
    protected CommonErrorStructure serviceError;
    @XmlElement(namespace = "")
    protected LoanInfoListStructure getLoanInfoResponseDetailObject;

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

    /**
     * Gets the value of the getLoanInfoResponseDetailObject property.
     * 
     * @return
     *     possible object is
     *     {@link LoanInfoListStructure }
     *     
     */
    public LoanInfoListStructure getGetLoanInfoResponseDetailObject() {
        return getLoanInfoResponseDetailObject;
    }

    /**
     * Sets the value of the getLoanInfoResponseDetailObject property.
     * 
     * @param value
     *     allowed object is
     *     {@link LoanInfoListStructure }
     *     
     */
    public void setGetLoanInfoResponseDetailObject(LoanInfoListStructure value) {
        this.getLoanInfoResponseDetailObject = value;
    }

}
