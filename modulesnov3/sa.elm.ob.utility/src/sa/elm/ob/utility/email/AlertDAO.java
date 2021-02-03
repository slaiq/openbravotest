package sa.elm.ob.utility.email;

import java.util.List;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.alert.Alert;

public class AlertDAO {


  public static List<Alert> getAlertsWithEmail() {

    // Changed query to not send unwanted alerts to user =100, role =0  -- as discussed with ASIF
    OBQuery<Alert> bpQuery = OBDal.getInstance().createQuery(Alert.class,
        " where (userContact.id is not null or role.id is not null) and userContact.id!=:userId and role.id !=:roleId and alertStatus=:alertStatus and (eutEmailstatus = :emailStatus or eutEmailstatus = :emailStatus_failed)");
    bpQuery.setNamedParameter("alertStatus", "NEW");
    bpQuery.setNamedParameter("emailStatus", "R");
    bpQuery.setNamedParameter("emailStatus_failed", "F");
    bpQuery.setNamedParameter("userId", "100");
    bpQuery.setNamedParameter("roleId", "0");
    return bpQuery.list();
  }

}
