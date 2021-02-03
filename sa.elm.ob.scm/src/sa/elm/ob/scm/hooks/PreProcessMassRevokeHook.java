package sa.elm.ob.scm.hooks;

import java.sql.Connection;

import javax.enterprise.context.ApplicationScoped;

import sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao.BidMgmtMassRevokeDAO;
import sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao.POMassRevokeDAO;
import sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao.ProposalMgmtMassRevokeDAO;
import sa.elm.ob.scm.ad_forms.ApprovalRevoke.dao.RequistionMassRevokeDAO;
import sa.elm.ob.utility.ad_forms.ApprovalRevoke.dao.ApprovalRevokeDAO;
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
    if ("PR".equalsIgnoreCase(windowId)) {
      return new RequistionMassRevokeDAO(con);
    } else if ("BID".equalsIgnoreCase(windowId)) {
      return new BidMgmtMassRevokeDAO(con);
    } else if ("PROP".equalsIgnoreCase(windowId)) {
      return new ProposalMgmtMassRevokeDAO(con);
    } else if ("PO".equalsIgnoreCase(windowId)) {
      return new POMassRevokeDAO(con);
    } else if (windowId.equals("MIR") || windowId.equals("SIR") || windowId.equals("CT")
        || windowId.equals("RT")) {
      return new ApprovalRevokeDAO(con);
    }
    return null;
  }

}