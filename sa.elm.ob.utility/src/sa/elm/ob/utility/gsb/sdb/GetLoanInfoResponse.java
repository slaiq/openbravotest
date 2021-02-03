
package sa.elm.ob.utility.gsb.sdb;

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
 *         &lt;element name="GetLoanInfoResult" type="{http://yesser.gov.sa/SCSB/LoanInformationService/version/1.0}getLoanInfoResponseStructure" minOccurs="0"/>
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
    "getLoanInfoResult"
})
@XmlRootElement(name = "GetLoanInfoResponse", namespace = "http://tempuri.org/")
public class GetLoanInfoResponse {

    @XmlElement(name = "GetLoanInfoResult", namespace = "http://tempuri.org/")
    protected GetLoanInfoResponseStructure getLoanInfoResult;

    /**
     * Gets the value of the getLoanInfoResult property.
     * 
     * @return
     *     possible object is
     *     {@link GetLoanInfoResponseStructure }
     *     
     */
    public GetLoanInfoResponseStructure getGetLoanInfoResult() {
        return getLoanInfoResult;
    }

    /**
     * Sets the value of the getLoanInfoResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link GetLoanInfoResponseStructure }
     *     
     */
    public void setGetLoanInfoResult(GetLoanInfoResponseStructure value) {
        this.getLoanInfoResult = value;
    }

}
