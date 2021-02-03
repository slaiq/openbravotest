
package sa.elm.ob.utility.gsb.redf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="GetLoanDetailsResult" type="{http://yesser.gov.sa/REDF/LoanInquiryService/version/2.0}GetLoanDetailsResponseStructure" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "getLoanDetailsResult"
})
@XmlRootElement(name = "GetLoanDetailsResponse")
public class GetLoanDetailsResponse {

    @XmlElement(name = "GetLoanDetailsResult")
    protected GetLoanDetailsResponseStructure getLoanDetailsResult;

    /**
     * Gets the value of the getLoanDetailsResult property.
     * 
     * @return
     *     possible object is
     *     {@link GetLoanDetailsResponseStructure }
     *     
     */
    public GetLoanDetailsResponseStructure getGetLoanDetailsResult() {
        return getLoanDetailsResult;
    }

    /**
     * Sets the value of the getLoanDetailsResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link GetLoanDetailsResponseStructure }
     *     
     */
    public void setGetLoanDetailsResult(GetLoanDetailsResponseStructure value) {
        this.getLoanDetailsResult = value;
    }

}
