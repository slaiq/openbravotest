
package sa.elm.ob.utility.dms.org.tempuri;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import sa.elm.ob.utility.dms.com.microsoft.schemas._2003._10.serialization.arrays.ArrayOfint;


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
 *         &lt;element name="AttachmentIDsList" type="{http://schemas.microsoft.com/2003/10/Serialization/Arrays}ArrayOfint" minOccurs="0"/>
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
    "attachmentIDsList"
})
@XmlRootElement(name = "GetAttachmentsBulkRequest")
public class GetAttachmentsBulkRequest {

    @XmlElementRef(name = "AttachmentIDsList", namespace = "http://tempuri.org/", type = JAXBElement.class, required = false)
    protected JAXBElement<sa.elm.ob.utility.dms.com.microsoft.schemas._2003._10.serialization.arrays.ArrayOfint> attachmentIDsList;

    /**
     * Gets the value of the attachmentIDsList property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfint }{@code >}
     *     
     */
    public JAXBElement<ArrayOfint> getAttachmentIDsList() {
        return attachmentIDsList;
    }

    /**
     * Sets the value of the attachmentIDsList property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfint }{@code >}
     *     
     */
    public void setAttachmentIDsList(JAXBElement<ArrayOfint> value) {
        this.attachmentIDsList = value;
    }

}
