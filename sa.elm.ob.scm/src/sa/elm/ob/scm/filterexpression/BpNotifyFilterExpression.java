package sa.elm.ob.scm.filterexpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Query;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.common.businesspartner.BusinessPartner;

public class BpNotifyFilterExpression implements FilterExpression {

  public List<String> finalList = new ArrayList<String>();

  @SuppressWarnings("rawtypes")
  @Override
  public String getExpression(Map<String, String> requestMap) {
    StringBuilder whereClause = new StringBuilder();
    org.openbravo.model.ad.access.User userContact = null;
    userContact = OBContext.getOBContext().getUser();
    String UserId = OBContext.getOBContext().getUser().getId();
    String roleId = OBContext.getOBContext().getRole().getId();
    // To check whether the loggedrole has forUser
    OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
        "role.id =:role and userContact.id = :User");
    userRole.setNamedParameter("role", roleId);
    userRole.setNamedParameter("User", UserId);
    if (userRole != null && userRole.list().size() > 0) {
      if (userRole.list().get(0).getEscmAdUser() != null) {
        userContact = userRole.list().get(0).getEscmAdUser();
        UserId = userContact.getId();
      }
    }

    List userList = null;
    List<String> usrTmp = new ArrayList<>();
    Query query = null;
    String BpNo = userContact.getBusinessPartner().getEfinDocumentno();
    finalList.add(BpNo);
    getBpList(finalList);
    String sqlString = "select ad_user_id from ad_user usr join c_bpartner bp on bp.c_bpartner_id = usr.c_bpartner_id where bp.em_efin_documentno in (:docNo) and bp.ad_client_id=:clientId";
    query = OBDal.getInstance().getSession().createSQLQuery(sqlString);
    query.setParameterList("docNo", finalList);
    query.setParameter("clientId", OBContext.getOBContext().getCurrentClient().getId());

    userList = query.list();
    if (userList != null && userList.size() > 0) {
      for (Object usrObj : userList) {
        usrTmp.add(usrObj.toString());
      }
      usrTmp.remove(UserId);
    }
    String userId = usrTmp.stream().collect(Collectors.joining("','", "'", "'"));
    String resultString = "e.id in (" + userId + ") and e.id !='"
        + OBContext.getOBContext().getUser().getId() + "'";
    whereClause.append(resultString);
    return whereClause.toString();
  }

  public void getBpList(List<String> BpId) {
    List<String> BpList = new ArrayList<>();
    try {
      OBQuery<BusinessPartner> bp = OBDal.getInstance().createQuery(BusinessPartner.class,
          "ehcmManager in (:docNo)");
      bp.setNamedParameter("docNo", BpId);
      if (bp.list().size() > 0) {
        for (BusinessPartner bplist : bp.list()) {
          if (!finalList.contains(bplist.getEfinDocumentno())) {
            finalList.add(bplist.getEfinDocumentno());
            BpList.add(bplist.getEfinDocumentno());
          }
        }
        if (BpList.size() > 0) {
          getBpList(BpList);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}