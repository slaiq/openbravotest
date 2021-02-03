
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LoanClassType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LoanClassType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="قرض مشروع"/>
 *     &lt;enumeration value="قرض اجتماعي"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LoanClassType")
@XmlEnum
public enum LoanClassType {

    @XmlEnumValue("\u0642\u0631\u0636 \u0645\u0634\u0631\u0648\u0639")
    قرض_مشروع("\u0642\u0631\u0636 \u0645\u0634\u0631\u0648\u0639"),
    @XmlEnumValue("\u0642\u0631\u0636 \u0627\u062c\u062a\u0645\u0627\u0639\u064a")
    قرض_اجتماعي("\u0642\u0631\u0636 \u0627\u062c\u062a\u0645\u0627\u0639\u064a");
    private final String value;

    LoanClassType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LoanClassType fromValue(String v) {
        for (LoanClassType c: LoanClassType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
