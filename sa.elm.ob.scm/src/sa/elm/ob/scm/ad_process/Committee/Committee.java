package sa.elm.ob.scm.ad_process.Committee;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMCommittee;
import sa.elm.ob.scm.ad_process.Committee.dao.CommitteeDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Divya on 29/05/2017
 */

public class Committee extends DalBaseProcess {

  /**
   * This servlet class was responsible for Commitee Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(Committee.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false;
    try {
      OBContext.setAdminMode();
      final String committeeId = (String) bundle.getParams().get("Escm_Committee_ID").toString();
      ESCMCommittee committee = Utility.getObject(ESCMCommittee.class, committeeId);

      Boolean ispresident = true, ismembers = true, isPresRepl = true;
      Boolean isFinancialCtrl = true;
      Boolean isline = true;

      if (committee.getAlertStatus().equals("DR")) {
        ispresident = CommitteeDAO.getPresident(committeeId);
        ismembers = CommitteeDAO.getMembers(committeeId);
        isPresRepl = CommitteeDAO.getPresidentRepl(committeeId);

        if (!isPresRepl) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Com_One_PresRepl@");
          bundle.setResult(result);
          return;
        }

        isFinancialCtrl = CommitteeDAO.getFinanceCtrl(committeeId);
        isline = CommitteeDAO.getCommitteMemCount(committeeId);

        if (committee.getType().equals("OEC")) {
          if (!ismembers || !ispresident) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Com_MemCount@");
            bundle.setResult(result);
            return;
          }
        }
        if (committee.getType().equals("PC") || committee.getType().equals("TL")) {
          if (!ismembers || !ispresident || !isFinancialCtrl) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_MemCount_proposals_type@");
            bundle.setResult(result);
            return;
          }
        }

        if (committee.getType().equals("TC")) {
          if (!isline) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_Technical_Committee@");
            bundle.setResult(result);
            return;
          }
        }

        if (!errorFlag) {
          committee.setAlertStatus("CO");
          committee.setCommaction("RE");
          committee.setUpdated(new java.util.Date());
          committee.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          OBDal.getInstance().save(committee);
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } /*
           * else { obError.setType("Error"); obError.setTitle("Error");
           * obError.setMessage("Process Failed"); bundle.setResult(obError); return; }
           */

      } else if (committee.getAlertStatus().equals("CO")) {
        committee.setAlertStatus("DR");
        committee.setCommaction("CO");
        committee.setUpdated(new java.util.Date());
        committee.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
        OBDal.getInstance().save(committee);
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in Commitee Submit:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}