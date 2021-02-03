
package sa.elm.ob.utility.gsb.redf;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for errorType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="errorType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="BusinessError"/>
 *     &lt;enumeration value="TechnicalError"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "errorType", namespace = "http://yefi.gov.sa/YEFIErrorStructure/xml/schemas/version2.3")
@XmlEnum
public enum ErrorType {

    @XmlEnumValue("BusinessError")
    BUSINESS_ERROR("BusinessError"),
    @XmlEnumValue("TechnicalError")
    TECHNICAL_ERROR("TechnicalError");
    private final String value;

    ErrorType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ErrorType fromValue(String v) {
        for (ErrorType c: ErrorType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
