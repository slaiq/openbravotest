package sa.elm.ob.scm.webservice.vehiclesystem.util;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.scm.MaterialIssueRequestCustody;

public class StringValidationUtil {
  // validate string is empty
  public static boolean notNullAndEmpty(String text) {
    return text != null && text.length() > 0 ? true : false;
  }

  /**
   * Validate the Employee exists in system or not
   * 
   * @param employeeCode
   * @return
   */
  public static String getEmployeeDetails(String employeeCode) {

    String employeeId = "";
    if (StringUtils.isNotEmpty(employeeCode)) {
      OBQuery<BusinessPartner> obQry = OBDal.getInstance().createQuery(BusinessPartner.class,
          "as e where e.searchKey=:value");
      obQry.setNamedParameter("value", employeeCode);
      obQry.setFilterOnReadableClients(false);
      obQry.setFilterOnReadableOrganization(false);
      if (obQry.list().size() > 0) {
        employeeId = obQry.list().get(0).getId();
      }
    }
    return employeeId;
  }

  public static MaterialIssueRequestCustody findCustodyDetails(String tagNo, String employeeCode) {
    // TODO Auto-generated method stub
    MaterialIssueRequestCustody objCustody = null;
    if (StringUtils.isNotEmpty(tagNo) && StringUtils.isNotEmpty(employeeCode)) {
      String employeeId = getEmployeeDetails(employeeCode);
      OBQuery<MaterialIssueRequestCustody> obQry = OBDal.getInstance().createQuery(
          MaterialIssueRequestCustody.class,
          "as e where e.documentNo=:tagNo and e.beneficiaryIDName.id=:employeeId");
      obQry.setNamedParameter("tagNo", tagNo);
      obQry.setNamedParameter("employeeId", employeeId);
      obQry.setFilterOnReadableClients(false);
      obQry.setFilterOnReadableOrganization(false);
      if (obQry.list().size() > 0) {
        objCustody = obQry.list().get(0);
      }
    }
    return objCustody;
  }
}
