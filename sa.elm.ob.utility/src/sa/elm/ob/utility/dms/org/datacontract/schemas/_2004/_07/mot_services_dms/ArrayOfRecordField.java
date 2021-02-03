
package sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ArrayOfRecordField complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ArrayOfRecordField">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RecordField" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}RecordField" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ArrayOfRecordField", propOrder = {
    "recordField"
})
public class ArrayOfRecordField {

    @XmlElement(name = "RecordField", nillable = true)
    protected List<RecordField> recordField;

    /**
     * Gets the value of the recordField property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the recordField property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRecordField().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RecordField }
     * 
     * 
     */
    public List<RecordField> getRecordField() {
        if (recordField == null) {
            recordField = new ArrayList<RecordField>();
        }
        return this.recordField;
    }

}
