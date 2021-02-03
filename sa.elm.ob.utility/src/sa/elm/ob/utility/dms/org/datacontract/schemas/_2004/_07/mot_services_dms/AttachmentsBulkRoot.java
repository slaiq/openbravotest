
package sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AttachmentsBulkRoot complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AttachmentsBulkRoot">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Information" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}Information" minOccurs="0"/>
 *         &lt;element name="Documents" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}ArrayOfDocument" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AttachmentsBulkRoot", propOrder = {
    "information",
    "documents"
})
public class AttachmentsBulkRoot {

    @XmlElementRef(name = "Information", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<Information> information;
    @XmlElementRef(name = "Documents", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<ArrayOfDocument> documents;

    /**
     * Gets the value of the information property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link Information }{@code >}
     *     
     */
    public JAXBElement<Information> getInformation() {
        return information;
    }

    /**
     * Sets the value of the information property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link Information }{@code >}
     *     
     */
    public void setInformation(JAXBElement<Information> value) {
        this.information = value;
    }

    /**
     * Gets the value of the documents property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfDocument }{@code >}
     *     
     */
    public JAXBElement<ArrayOfDocument> getDocuments() {
        return documents;
    }

    /**
     * Sets the value of the documents property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfDocument }{@code >}
     *     
     */
    public void setDocuments(JAXBElement<ArrayOfDocument> value) {
        this.documents = value;
    }

}
