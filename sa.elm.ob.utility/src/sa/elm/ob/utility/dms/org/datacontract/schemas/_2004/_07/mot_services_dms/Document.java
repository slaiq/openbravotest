
package sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Document complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Document">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Revisions" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}ArrayOfRevision" minOccurs="0"/>
 *         &lt;element name="URI" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="FileBase64" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="File" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}File" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Document", propOrder = {
    "revisions",
    "uri",
    "fileBase64",
    "file"
})
public class Document {

    @XmlElementRef(name = "Revisions", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<ArrayOfRevision> revisions;
    @XmlElementRef(name = "URI", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> uri;
    @XmlElementRef(name = "FileBase64", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> fileBase64;
    @XmlElementRef(name = "File", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<File> file;

    /**
     * Gets the value of the revisions property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfRevision }{@code >}
     *     
     */
    public JAXBElement<ArrayOfRevision> getRevisions() {
        return revisions;
    }

    /**
     * Sets the value of the revisions property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfRevision }{@code >}
     *     
     */
    public void setRevisions(JAXBElement<ArrayOfRevision> value) {
        this.revisions = value;
    }

    /**
     * Gets the value of the uri property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getURI() {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setURI(JAXBElement<String> value) {
        this.uri = value;
    }

    /**
     * Gets the value of the fileBase64 property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getFileBase64() {
        return fileBase64;
    }

    /**
     * Sets the value of the fileBase64 property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setFileBase64(JAXBElement<String> value) {
        this.fileBase64 = value;
    }

    /**
     * Gets the value of the file property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link File }{@code >}
     *     
     */
    public JAXBElement<File> getFile() {
        return file;
    }

    /**
     * Sets the value of the file property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link File }{@code >}
     *     
     */
    public void setFile(JAXBElement<File> value) {
        this.file = value;
    }

}
