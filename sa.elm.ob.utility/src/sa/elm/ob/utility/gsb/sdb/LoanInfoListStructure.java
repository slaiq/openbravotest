
package sa.elm.ob.utility.gsb.sdb;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LoanInfoListStructure complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LoanInfoListStructure">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="LoanInfo" type="{http://yefi.gov.sa/SCSB/LoanInformation/xml/schemas/version1.0}LoanInfoStructure" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LoanInfoListStructure", propOrder = {
    "loanInfo"
})
public class LoanInfoListStructure {

    @XmlElement(name = "LoanInfo")
    protected List<LoanInfoStructure> loanInfo;

    /**
     * Gets the value of the loanInfo property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the loanInfo property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLoanInfo().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link LoanInfoStructure }
     * 
     * 
     */
    public List<LoanInfoStructure> getLoanInfo() {
        if (loanInfo == null) {
            loanInfo = new ArrayList<LoanInfoStructure>();
        }
        return this.loanInfo;
    }

}
