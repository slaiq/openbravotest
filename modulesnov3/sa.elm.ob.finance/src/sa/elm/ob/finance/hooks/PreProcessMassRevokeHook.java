package sa.elm.ob.finance.hooks;

import java.sql.Connection;

import javax.enterprise.context.ApplicationScoped;

import sa.elm.ob.finance.ad_forms.ApprovalRevoke.dao.AdjustmentMassRevokeDAO;
import sa.elm.ob.finance.ad_forms.ApprovalRevoke.dao.EncumMassRevokeDAO;
import sa.elm.ob.finance.ad_forms.ApprovalRevoke.dao.GLJournalMassRevokeDAO;
import sa.elm.ob.finance.ad_forms.ApprovalRevoke.dao.InvoiceMassRevokeDAO;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.MassRevoke;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.massrevoke.ProcessMassRevoke;

/**
 * 
 * @author sathish kumar.p
 * 
 *         This class is to implement the hook for Process Mass Revoke. This is Pre-execution hook
 *
 */

@ApplicationScoped
public class PreProcessMassRevokeHook implements ProcessMassRevoke {

  @Override
  public MassRevoke preProcess(String windowId, Connection con) {
    if ("API".equalsIgnoreCase(windowId) || "PPI".equalsIgnoreCase(windowId)
        || "PPA".equalsIgnoreCase(windowId) || "RDV".equalsIgnoreCase(windowId)) {
      return new InvoiceMassRevokeDAO();
    } else if ("ENC".equalsIgnoreCase(windowId)) {
      return new EncumMassRevokeDAO();
    } else if ("ADJ".equalsIgnoreCase(windowId)) {
      return new AdjustmentMassRevokeDAO();
    } else if ("GL".equalsIgnoreCase(windowId)) {
      return new GLJournalMassRevokeDAO();
    }
    return null;
  }

}
