package sa.elm.ob.utility.ad_forms.nextrolebyrule;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Kousalya
 */
public class DelegatedNextRoleDAOImpl implements DelegatedNextRoleDAO {

  private static final Logger log = LoggerFactory.getLogger(DelegatedNextRoleDAOImpl.class);
  public Boolean checkDelegation(Date delegationDate, String strRoleId, String documentType) {
    boolean allowDelegation = Boolean.FALSE;
    StringBuffer query = null;
    Query delQuery = null;
    try {
      query = new StringBuffer();
      query.append("select dll.role.id from Eut_Docapp_Delegateln dll "
          + "      join dll.eUTDocappDelegate dl "
          + "      where dl.fromDate <=:currentDate and dl.date >=:currentDate and dll.documentType=:docType and dll.role.id=:currentRoleId and dl.processed='Y' ");
      delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delQuery.setParameter("currentDate", delegationDate);
      delQuery.setParameter("currentDate", delegationDate);
      delQuery.setParameter("docType", documentType);
      delQuery.setParameter("currentRoleId", strRoleId);
      if (delQuery != null) {
        if (delQuery.list().size() > 0) {
          allowDelegation = Boolean.TRUE;
        }
      }

    } catch (Exception e) {
      log.error("Error in checkDelegation() ", e);
      return false;
    }
    return allowDelegation;
  }

  @SuppressWarnings("unchecked")
  @Override
  public String getDelegatedFromRole(String strRoleId, String strDocumentType, String strUserId)
      throws Exception {
    String delegatedFromRole = "";
    StringBuffer query = null;
    Query delQuery = null;
    List<Object> delegatedRoles = new ArrayList<Object>();

    try {
      query = new StringBuffer();
      query.append("select dl.role.id from Eut_Docapp_Delegateln dll "
          + "      join dll.eUTDocappDelegate dl "
          + "      where dl.fromDate <=:currentDate and dl.date >=:currentDate and dll.documentType=:docType and dll.role.id=:currentRoleId and dl.processed='Y' ");
      delQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      delQuery.setParameter("currentDate", new Date());
      delQuery.setParameter("currentDate", new Date());
      delQuery.setParameter("docType", strDocumentType);
      delQuery.setParameter("currentRoleId", strRoleId);

      if (delQuery != null) {
        delegatedRoles = delQuery.list();

        if (delegatedRoles.size() > 0) {
          delegatedFromRole = delegatedRoles.get(0).toString();
        }
      }
    } catch (Exception e) {
      log.error("Error in checkDelegation() ", e);
      delegatedFromRole = "";
    }

    return delegatedFromRole;
  }
}
