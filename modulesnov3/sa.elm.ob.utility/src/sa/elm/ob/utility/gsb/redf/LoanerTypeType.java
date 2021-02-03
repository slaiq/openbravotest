
package sa.elm.ob.utility.gsb.redf;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LoanerTypeType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LoanerTypeType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Loaner"/>
 *     &lt;enumeration value="Redemption"/>
 *     &lt;enumeration value="Sold Contract"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LoanerTypeType", namespace = "http://yefi.gov.sa/REDF/LoanInquirySchema/xml/schemas/version2.0")
@XmlEnum
public enum LoanerTypeType {

    @XmlEnumValue("Loaner")
    LOANER("Loaner"),
    @XmlEnumValue("Redemption")
    REDEMPTION("Redemption"),
    @XmlEnumValue("Sold Contract")
    SOLD_CONTRACT("Sold Contract");
    private final String value;

    LoanerTypeType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LoanerTypeType fromValue(String v) {
        for (LoanerTypeType c: LoanerTypeType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
