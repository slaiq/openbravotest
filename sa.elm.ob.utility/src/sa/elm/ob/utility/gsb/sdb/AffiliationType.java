
package sa.elm.ob.utility.gsb.sdb;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for AffiliationType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="AffiliationType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="مقترض"/>
 *     &lt;enumeration value="كفيل"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "AffiliationType")
@XmlEnum
public enum AffiliationType {

    مقترض,
    كفيل;

    public String value() {
        return name();
    }

    public static AffiliationType fromValue(String v) {
        return valueOf(v);
    }

}
