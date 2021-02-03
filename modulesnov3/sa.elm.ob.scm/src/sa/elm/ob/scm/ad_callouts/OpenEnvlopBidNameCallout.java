package sa.elm.ob.scm.ad_callouts;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.ad_callouts.dao.OpenEnvelopeDAO;

public class OpenEnvlopBidNameCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(OpenEnvlopBidNameCallout.class);
  /**
   * Callout to update the Bid name and Date.
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpbidno = vars.getStringParameter("inpescmBidmgmtId");
    List<EscmProposalMgmt> proposalList = new ArrayList<EscmProposalMgmt>();
    try {
      log.debug("LastChanged:" + inpLastFieldChanged);
      if (inpLastFieldChanged.equals("inpescmBidmgmtId")) {
        // set maximun open envelope date in date field from biddates.
        String Date = OpenEnvelopeDAO.getBidOpenenvelopday(inpbidno);
        info.addResult("inptodaydate", Date);
        // set bid name from bid.
        EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class, inpbidno);
        info.addResult("inpbidname", bid.getBidname());
        if (bid != null) {
          OBQuery<EscmProposalMgmt> proposalQry = OBDal.getInstance()
              .createQuery(EscmProposalMgmt.class, " as e where e.escmBidmgmt.id=:bidId and"
                  + " e.contractType is not null  order by e.creationDate desc  ");
          proposalQry.setNamedParameter("bidId", inpbidno);
          proposalList = proposalQry.list();
          if (proposalList.size() > 0) {
            info.addResult("inpcontractType", proposalList.get(0).getContractType().getId());
          } else if (bid.getContractType() != null) {
            info.addResult("inpcontractType", bid.getContractType().getId());
          }
        }
      }

      if (inpLastFieldChanged.equals("inpadOrgId")) {
        // Dont select any thing in contract category as default
        String jscode = " if(form.view.isShowingForm){ form.getFieldFromColumnName('Contract_Type').setValue('') }else {form.view.viewGrid.processColumnValue(form.view.viewGrid.data.indexOf(form.view.viewGrid.getSelectedRecord()),'Contract_Type',[])}";
        info.addResult("JSEXECUTE", jscode);
      }
    } catch (Exception e) {
      log.debug("Exception in OpenEnvlopBidNameCallout  callout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
