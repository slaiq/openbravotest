
package sa.elm.ob.utility.gsb.adf;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for LanguageType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="LanguageType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="EN"/>
 *     &lt;enumeration value="AR"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "LanguageType", namespace = "http://yefi.gov.sa/CommonTypes/xml/schemas/version2.0")
@XmlEnum
public enum LanguageType {

    EN,
    AR;

    public String value() {
        return name();
    }

    public static LanguageType fromValue(String v) {
        return valueOf(v);
    }

}
