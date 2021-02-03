package sa.elm.ob.utility.model.domaintype;

import org.openbravo.base.model.domaintype.BasePrimitiveDomainType;

public class SecuredStringDomainType extends BasePrimitiveDomainType {

  /**
   * @return class of the {@link String}
   */
  public Class<?> getPrimitiveType() {
    return String.class;
  }

  @Override
  public Object createFromString(String strValue) {
    if (strValue == null || strValue.length() == 0) {
      return null;
    }
    return strValue;
  }

  @Override
  public String getXMLSchemaType() {
    return "ob:string";
  }
}
