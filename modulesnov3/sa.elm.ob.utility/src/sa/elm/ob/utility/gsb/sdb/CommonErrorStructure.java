
package sa.elm.ob.utility.gsb.sdb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CommonErrorStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CommonErrorStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SourceAgency" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ErrorType" type="{http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3}errorType"/>
 *         &lt;element name="RaisedBy" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Code" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="ErrorText" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Insert" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CommonErrorStructure", namespace = "http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3", propOrder = {
    "sourceAgency",
    "errorType",
    "raisedBy",
    "code",
    "errorText",
    "insert"
})
public class CommonErrorStructure {

    @XmlElement(name = "SourceAgency")
    protected String sourceAgency;
    @XmlElement(name = "ErrorType", required = true)
    @XmlSchemaType(name = "string")
    protected ErrorType errorType;
    @XmlElement(name = "RaisedBy")
    protected String raisedBy;
    @XmlElement(name = "Code")
    protected String code;
    @XmlElement(name = "ErrorText")
    protected String errorText;
    @XmlElement(name = "Insert")
    protected List<String> insert;

    /**
     * Gets the value of the sourceAgency property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSourceAgency() {
        return sourceAgency;
    }

    /**
     * Sets the value of the sourceAgency property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSourceAgency(String value) {
        this.sourceAgency = value;
    }

    /**
     * Gets the value of the errorType property.
     * 
     * @return
     *     possible object is
     *     {@link ErrorType }
     *     
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * Sets the value of the errorType property.
     * 
     * @param value
     *     allowed object is
     *     {@link ErrorType }
     *     
     */
    public void setErrorType(ErrorType value) {
        this.errorType = value;
    }

    /**
     * Gets the value of the raisedBy property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRaisedBy() {
        return raisedBy;
    }

    /**
     * Sets the value of the raisedBy property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRaisedBy(String value) {
        this.raisedBy = value;
    }

    /**
     * Gets the value of the code property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the value of the code property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCode(String value) {
        this.code = value;
    }

    /**
     * Gets the value of the errorText property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getErrorText() {
        return errorText;
    }

    /**
     * Sets the value of the errorText property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setErrorText(String value) {
        this.errorText = value;
    }

    /**
     * Gets the value of the insert property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the insert property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInsert().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getInsert() {
        if (insert == null) {
            insert = new ArrayList<String>();
        }
        return this.insert;
    }

}
