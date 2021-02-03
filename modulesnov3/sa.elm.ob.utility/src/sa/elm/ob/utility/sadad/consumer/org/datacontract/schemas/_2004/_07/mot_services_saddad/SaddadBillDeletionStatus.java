
package sa.elm.ob.utility.sadad.consumer.org.datacontract.schemas._2004._07.mot_services_saddad;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for SaddadBillDeletionStatus.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * <p>
 * 
 * <pre>
 * &lt;simpleType name="SaddadBillDeletionStatus"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="DeletedSuccessfully"/&gt;
 *     &lt;enumeration value="Error_NotDeleted"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "SaddadBillDeletionStatus")
@XmlEnum
public enum SaddadBillDeletionStatus {

  @XmlEnumValue("DeletedSuccessfully")
  DELETED_SUCCESSFULLY("DeletedSuccessfully"), @XmlEnumValue("Error_NotDeleted")
  ERROR_NOT_DELETED("Error_NotDeleted");
  private final String value;

  SaddadBillDeletionStatus(String v) {
    value = v;
  }

  public String value() {
    return value;
  }

  public static SaddadBillDeletionStatus fromValue(String v) {
    for (SaddadBillDeletionStatus c : SaddadBillDeletionStatus.values()) {
      if (c.value.equals(v)) {
        return c;
      }
    }
    throw new IllegalArgumentException(v);
  }

}
