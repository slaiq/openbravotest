package sa.elm.ob.utility.ad_callouts;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditProcessCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(AuditProcessCallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    // declare the session variables

    String strChanged = info.vars.getStringParameter("inpLastFieldChanged");
    try {
      OBContext.setAdminMode();
      if (strChanged.equalsIgnoreCase("inpemEutIsfullyaudited")) {
        boolean currentRecordFullyAudited = info.vars.getStringParameter("inpemEutIsfullyaudited")
            .equals("Y");
        if (currentRecordFullyAudited) {
          SessionInfo.setAuditActive(true);
          info.addResult("inpisfullyaudited", true);

        } else {
          info.addResult("inpisfullyaudited", false);
          OBCriteria<Table> obc = OBDal.getInstance().createCriteria(Table.class);
          obc.add(Restrictions.eq(Table.PROPERTY_EUTISFULLYAUDITED, true));
          SessionInfo.setAuditActive(obc.list().size() > 0);
        }
      } else if (strChanged.equalsIgnoreCase("inpemEutIsexcludeaudit")) {
        boolean currentRecordExcludeAudit = info.vars.getStringParameter("inpemEutIsexcludeaudit")
            .equals("Y");
        if (currentRecordExcludeAudit) {
          SessionInfo.setAuditActive(true);
          info.addResult("inpisexcludeaudit", true);

        } else {
          info.addResult("inpisexcludeaudit", false);
          OBCriteria<Table> obc = OBDal.getInstance().createCriteria(Table.class);
          obc.add(Restrictions.eq(Table.PROPERTY_EUTISFULLYAUDITED, true));
          SessionInfo.setAuditActive(obc.list().size() > 0);
        }
      }
    } catch (Exception e) {
      log.error("Exception in AuditProcessCallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
