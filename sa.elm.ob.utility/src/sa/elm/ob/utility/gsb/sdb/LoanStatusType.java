
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LoanStatusType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LoanStatusType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="مفتوح"/>
 *     &lt;enumeration value="مغلق"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LoanStatusType")
@XmlEnum
public enum LoanStatusType {

    مفتوح,
    مغلق;

    public String value() {
        return name();
    }

    public static LoanStatusType fromValue(String v) {
        return valueOf(v);
    }

}
