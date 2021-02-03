package sa.elm.ob.utility.util;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

/**
 * 
 * @author Sathishkumar
 *
 */
public class SecuredStringDAO {

  private static Logger log = Logger.getLogger(SecuredStringDAO.class);

  /**
   * This method is used to identify whether the field can be displayed or not
   * 
   * @param req
   * @return true or false
   */

  public static boolean isAllowedToDisplay(String requistionId) {

    boolean allow = false;
    String currentUserId = OBContext.getOBContext().getUser().getId();
    Role currentRole = OBContext.getOBContext().getRole();

    try {
      OBContext.setAdminMode();

      Requisition req = OBDal.getInstance().get(Requisition.class, requistionId);

      if (req == null) {
        RequisitionLine reqline = OBDal.getInstance().get(RequisitionLine.class, requistionId);
        if (reqline != null) {
          req = reqline.getRequisition();
        }
      }

      if (req != null) {

        if (!req.isEscmIssecured()) {
          return false;
        }

        if (currentRole.isEscmIsExcludesecuredprice()) {
          return false;
        }

        // If user is same as record created user, then display the field
        if (currentUserId.equals(req.getCreatedBy().getId())) {
          allow = true;
        }

        // if current role is PEE committee member role
        if (currentRole.isEscmIsprocurecommitee()) {
          allow = true;
        }

        // if current user is notified user to create bid, then display the field
        if (currentUserId.equals(req.getEscmNotifyuser())) {
          allow = true;
        }

        if (!allow) {
          // Check delegation is present for any of the user to current user
          String delQuery = "select header.eut_docapp_delegate_id from eut_docapp_delegate header "
              + "join eut_docapp_delegateln line on header.eut_docapp_delegate_id = line.eut_docapp_delegate_id "
              + " where document_type in ('EUT_111','EUT_118')  "
              + " and ( header.ad_user_id =? or header.ad_user_id =? )  "
              + " and line.ad_user_id = ? and line.ad_role_id= ? ";

          Query query = OBDal.getInstance().getSession().createSQLQuery(delQuery);
          query.setParameter(0, req.getCreatedBy().getId());
          query.setParameter(1, req.getEscmNotifyuser() != null ? req.getEscmNotifyuser() : '1');
          query.setParameter(2, currentUserId);
          query.setParameter(3, currentRole.getId());

          @SuppressWarnings("unchecked")
          List<Object> queryList = query.list();
          if (queryList.size() > 0) {
            allow = true;
          }

          if (!allow) {

            // All nextrole users and roles should able to see the original price column
            String appQuery = "select ad_user_id, ad_role_id from eut_next_role header "
                + " join eut_next_role_line lines on lines.eut_next_role_id = header.eut_next_role_id "
                + " where header.eut_next_role_id =? and (ad_user_id =?  or (ad_user_id is null and ad_role_id=?) )";

            query = OBDal.getInstance().getSession().createSQLQuery(appQuery);
            query.setParameter(0, req.getEutNextRole() != null ? req.getEutNextRole().getId() : "");
            query.setParameter(1, currentUserId);
            query.setParameter(2, currentRole.getId());

            @SuppressWarnings("unchecked")
            List<Object> appqueryList = query.list();
            if (appqueryList.size() > 0) {
              allow = true;
            }

            if (!allow) {

              // It should be shown to user who approved the record
              String appHistQuery = "select createdby from escm_purchasereq_app_hist  where created >= (select  "
                  + " created from escm_purchasereq_app_hist where purchasereqaction='SUB' order by created desc limit 1) "
                  + " and m_requisition_id =?  and (purchasereqaction ='SUB' or purchasereqaction='AP') and createdby=?";

              query = OBDal.getInstance().getSession().createSQLQuery(appHistQuery);
              query.setParameter(0, req.getId());
              query.setParameter(1, currentUserId);

              @SuppressWarnings("unchecked")
              List<Object> appHistQueryList = query.list();
              if (appHistQueryList.size() > 0) {
                allow = true;
              }

              // if (!allow) {
              //
              // // Original unit price should shown to all the notified user
              //
              // String notifyQuery = " select ad_alert_id from ad_alert where referencekey_id =?
              // and "
              // + " created > (select created from escm_purchasereq_app_hist where
              // purchasereqaction='SUB' order by created desc limit 1) "
              // + " and ad_user_id = ? and em_eut_alert_key = 'scm.pr.approved' ";
              //
              // query = OBDal.getInstance().getSession().createSQLQuery(notifyQuery);
              // query.setParameter(0, req.getId());
              // query.setParameter(1, currentUserId);
              //
              // @SuppressWarnings("unchecked")
              // List<Object> notifyQueryList = query.list();
              // if (notifyQueryList.size() > 0) {
              // allow = true;
              // }
              //
              // }

            }

          }

        }

      }

    } catch (Exception e) {
      log.error("Error while finding display logic" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return allow;
  }

}
