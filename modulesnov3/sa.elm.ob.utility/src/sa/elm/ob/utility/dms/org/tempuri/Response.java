
package sa.elm.ob.utility.dms.org.tempuri;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms.ResponseRoot;


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
 *         &lt;element name="HasError" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="ErrorMessage" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ResponseRoot" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}ResponseRoot" minOccurs="0"/>
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
    "hasError",
    "errorMessage",
    "responseRoot"
})
@XmlRootElement(name = "Response")
public class Response {

    @XmlElement(name = "HasError")
    protected Boolean hasError;
    @XmlElementRef(name = "ErrorMessage", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
    protected JAXBElement<String> errorMessage;
    @XmlElementRef(name = "ResponseRoot", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
    protected JAXBElement<ResponseRoot> responseRoot;

    /**
     * Gets the value of the hasError property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isHasError() {
        return hasError;
    }

    /**
     * Sets the value of the hasError property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setHasError(Boolean value) {
        this.hasError = value;
    }

    /**
     * Gets the value of the errorMessage property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the value of the errorMessage property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setErrorMessage(JAXBElement<String> value) {
        this.errorMessage = value;
    }

    /**
     * Gets the value of the responseRoot property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ResponseRoot }{@code >}
     *     
     */
    public JAXBElement<ResponseRoot> getResponseRoot() {
        return responseRoot;
    }

    /**
     * Sets the value of the responseRoot property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ResponseRoot }{@code >}
     *     
     */
    public void setResponseRoot(JAXBElement<ResponseRoot> value) {
        this.responseRoot = value;
    }

}
