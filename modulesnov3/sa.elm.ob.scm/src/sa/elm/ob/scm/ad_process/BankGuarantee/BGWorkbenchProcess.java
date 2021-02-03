package sa.elm.ob.scm.ad_process.BankGuarantee;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMBGAmtRevision;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.event.dao.BankGuaranteeDetailEventDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author qualian
 * 
 */
public class BGWorkbenchProcess extends DalBaseProcess {

  /**
   * This servlet class was responsible for Annoucments Process
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(BGWorkbenchProcess.class);

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean errorFlag = false, isPeriodOpen = true;
    String status = null;
    EscmProposalMgmt proposal = null;
    Order ord = null;
    int count = 0;

    try {
      OBContext.setAdminMode();
      final String bgworkbenchId = (String) bundle.getParams().get("Escm_Bgworkbench_ID")
          .toString();
      ESCMBGWorkbench bgworkbenchObj = OBDal.getInstance().get(ESCMBGWorkbench.class,
          bgworkbenchId);
      Date transactionDate = bgworkbenchObj.getTransactionDate();
      String orgId = bgworkbenchObj.getOrganization().getId();
      if (bgworkbenchObj.getBghdstatus().equals("DR")) {

        // Check transaction period is opened or not -- ((orgId.equals("0")) ? vars.getOrg() :
        // orgId)
        isPeriodOpen = Utility.checkOpenPeriod(transactionDate,
            ((orgId.equals("0")) ? vars.getOrg() : orgId), vars.getClient());
        if (!isPeriodOpen) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
          bundle.setResult(result);
          return;
        }
      }
      if (bgworkbenchObj.getEscmBankguaranteeDetailList().size() == 0) {
        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "Error", "@ESCM_BGWorkbenchNoLines@");
        bundle.setResult(result);
        return;
      } else if (bgworkbenchObj.getBgaction().equals("CO")) {
        if (bgworkbenchObj.getBidNo() != null
            && !bgworkbenchObj.getBidNo().getBidtype().equals("DR"))
          // check open envelope event is completed

          // if (bgworkbenchObj != null && bgworkbenchObj.getEscmProposalAttr() != null
          // && bgworkbenchObj.getEscmProposalAttr().getEscmOpenenvcommitee() != null) {
          // if (!"CO".equals(
          // bgworkbenchObj.getEscmProposalAttr().getEscmOpenenvcommitee().getAlertStatus())) {
          // OBError result = OBErrorBuilder.buildMessage(null, "error",
          // "@escm_openenvnotcomplete@");
          // bundle.setResult(result);
          // return;
          // }
          // }

          log.debug("bgworkbenchId:" + bgworkbenchId);
        errorFlag = BGWorkbenchDAO.chkTotBGAmtCalAmt(bgworkbenchId, false);
        if (!errorFlag) {
          bgworkbenchObj.setBghdstatus("CO");
          bgworkbenchObj.setBgaction("RE");

          if (bgworkbenchObj.getEscmBankguaranteeDetailList().size() > 0) {
            for (Escmbankguaranteedetail bgdet : bgworkbenchObj.getEscmBankguaranteeDetailList()) {
              bgdet.setBgstatus("ACT");
              OBDal.getInstance().save(bgdet);
            }
          }
          OBDal.getInstance().save(bgworkbenchObj);
          OBDal.getInstance().flush();
          OBError result = OBErrorBuilder.buildMessage(null, "Success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else {
          if (bgworkbenchObj.getType().equals("IBG"))
            status = "@ESCM_BGTotBGAmtComCalAmtIB@";
          else
            status = "@ESCM_BGTotBGAmtComCalAmtFB@";
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "Error", status);
          bundle.setResult(result);
          return;
        }
      } else if (bgworkbenchObj.getBgaction().equals("RE")) {

        if (bgworkbenchObj.getDocumentType().equals("P")) {
          if (bgworkbenchObj.getDocumentNo() != null)
            proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
                bgworkbenchObj.getDocumentNo().getId());
          if (proposal != null && proposal.getProposalstatus() != null
              && proposal.getProposalstatus().equals("AWD")) {
            errorFlag = true;
          }
          if (bgworkbenchObj.getDocumentNo() != null) {
            OBQuery<EscmProposalAttribute> pattr = OBDal.getInstance()
                .createQuery(EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id='"
                    + bgworkbenchObj.getDocumentNo().getId() + "' ");
            if (pattr.list().size() > 0) {
              for (EscmProposalAttribute attr : pattr.list()) {
                if (attr.getEscmProposalevlEvent() != null)
                  errorFlag = true;
              }
            }
          }

        } else {
          if (bgworkbenchObj.getDocumentNo() != null)
            ord = OBDal.getInstance().get(Order.class, bgworkbenchObj.getDocumentNo().getId());
          count = BankGuaranteeDetailEventDAO.restrictReactivateBG(bgworkbenchObj);
          if (count > 0) {
            OBDal.getInstance().flush();
            OBError result = OBErrorBuilder.buildMessage(null, "Error",
                "@ESCM_BGWorkbenchPO_Transaction@");
            bundle.setResult(result);
            return;
          }

          // if (ord != null && ord.getEscmAppstatus() != null
          // && !ord.getEscmAppstatus().equals("DR")) {
          // errorFlag = true;
          // }
        }
        if (bgworkbenchObj.getEscmBankguaranteeDetailList().size() > 0) {
          for (Escmbankguaranteedetail bgdetail : bgworkbenchObj.getEscmBankguaranteeDetailList()) {
            if (!bgdetail.getBgstatus().equals("ACT") && !bgdetail.getBgstatus().equals("DR")) {
              errorFlag = true;
            }
            if (bgdetail.getESCMBGAmtRevisionList().size() > 0) {
              for (ESCMBGAmtRevision bgamtrevision : bgdetail.getESCMBGAmtRevisionList()) {
                if (bgamtrevision.getBankLetterReference() != null
                    && bgamtrevision.getLetterDate() != null) {
                  errorFlag = true;
                  break;
                }
              }
            }
          }
        }
        if (!errorFlag) {
          bgworkbenchObj.setBghdstatus("DR");
          bgworkbenchObj.setBgaction("CO");

          if (bgworkbenchObj.getEscmBankguaranteeDetailList().size() > 0) {
            for (Escmbankguaranteedetail bgdet : bgworkbenchObj.getEscmBankguaranteeDetailList()) {
              bgdet.setBgstatus("DR");
              OBDal.getInstance().save(bgdet);
            }
          }
          OBDal.getInstance().save(bgworkbenchObj);
          OBDal.getInstance().flush();
          OBError result = OBErrorBuilder.buildMessage(null, "Success", "@ESCM_BG_Reactive@");
          bundle.setResult(result);
          return;
        } else {
          OBDal.getInstance().save(bgworkbenchObj);
          OBDal.getInstance().flush();
          OBError result = OBErrorBuilder.buildMessage(null, "Error", "@ESCM_BGCantReactivate@");
          bundle.setResult(result);
          return;
        }
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in BGWorkbenchProcess Submit:" + e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }

  }
}