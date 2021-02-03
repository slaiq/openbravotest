
package sa.elm.ob.utility.gsb.adf;

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
 *         &lt;element name="GetLoanInformationResult" type="{http://yesser.gov.sa/ADF/ClientFinancialStatusService/version/1.0}getLoanInformationResponseStructure" minOccurs="0"/>
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
    "getLoanInformationResult"
})
@XmlRootElement(name = "GetLoanInformationResponse")
public class GetLoanInformationResponse {

    @XmlElement(name = "GetLoanInformationResult")
    protected GetLoanInformationResponseStructure getLoanInformationResult;

    /**
     * Gets the value of the getLoanInformationResult property.
     * 
     * @return
     *     possible object is
     *     {@link GetLoanInformationResponseStructure }
     *     
     */
    public GetLoanInformationResponseStructure getGetLoanInformationResult() {
        return getLoanInformationResult;
    }

    /**
     * Sets the value of the getLoanInformationResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link GetLoanInformationResponseStructure }
     *     
     */
    public void setGetLoanInformationResult(GetLoanInformationResponseStructure value) {
        this.getLoanInformationResult = value;
    }

}
