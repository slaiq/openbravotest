package sa.elm.ob.scm.ad_process.IssueRequest;

import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestLine;

//IssueRequestDAO   file
public class IssueRequestDAO {
  private static final Logger log = LoggerFactory.getLogger(IssueRequestDAO.class);

  // check any one of the line issue qty should be greater than zero
  public static boolean ChkAnyReqLinesGrtThanZero(MaterialIssueRequest objRequest) {
    try {
      OBContext.setAdminMode();
      OBQuery<MaterialIssueRequestLine> matReqLineObj = OBDal.getInstance().createQuery(
          MaterialIssueRequestLine.class,
          " as e where e.escmMaterialRequest.id=:mirID and deliveredQantity > 0 ");
      matReqLineObj.setNamedParameter("mirID", objRequest.getId());
      if (matReqLineObj.list().size() == 0) {
        return true;
      } else
        return false;

    } catch (OBException e) {
      log.error("Exception while ChkAnyReqLinesGrtThanZero:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  // check any one of the line issue qty should be greater than zero
  public static boolean ChkGenericProduct(MaterialIssueRequest objRequest) {
    try {
      OBContext.setAdminMode();
      OBQuery<MaterialIssueRequestLine> matReqLineObj = OBDal.getInstance().createQuery(
          MaterialIssueRequestLine.class, " as e where e.escmMaterialRequest.id=:mirID "
              + " and e.product.stocked='N' and e.product.purchase='N' ");
      matReqLineObj.setNamedParameter("mirID", objRequest.getId());
      matReqLineObj.setFilterOnReadableOrganization(false);
      if (matReqLineObj.list().size() >= 1) {
        return true;
      } else
        return false;

    } catch (OBException e) {
      log.error("Exception while ChkGenericProduct:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * 
   * @param RequestId
   * @param roleId
   * @return
   */
  public static boolean isDirectApproval(String RequestId, String roleId) {
    boolean isDirectApp = false;
    StringBuffer query = null;
    Query mirnxtrlQuery = null;
    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append(
          "select count(mir.id) as count from Escm_Material_Request mir join mir.eUTNextRole nxtrl join nxtrl.eutNextRoleLineList nxtrln ");
      query.append(" where mir.id=:mirId and nxtrln.role.id=:roleId");
      mirnxtrlQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      mirnxtrlQuery.setParameter("mirId", RequestId);
      mirnxtrlQuery.setParameter("roleId", roleId);
      log.debug(" Query : " + query.toString());
      if (mirnxtrlQuery != null) {
        if (mirnxtrlQuery.list().size() > 0) {
          if (mirnxtrlQuery.iterate().hasNext()) {
            String mirCount = mirnxtrlQuery.iterate().next().toString();
            int count = Integer.parseInt(mirCount);
            if (count > 0)
              isDirectApp = true;
            else
              isDirectApp = false;
          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while isDirectApproval:", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isDirectApp;
  }
}
