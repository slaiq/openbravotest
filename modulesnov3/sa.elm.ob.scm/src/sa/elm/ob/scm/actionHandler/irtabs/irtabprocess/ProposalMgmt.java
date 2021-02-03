package sa.elm.ob.scm.actionHandler.irtabs.irtabprocess;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.utility.util.irtabsutils.IRTabIconVariables;

public class ProposalMgmt extends IRTabIconVariables {
  Logger log = Logger.getLogger(ProposalMgmt.class);

  public void getIconVariables(HttpServletRequest request, JSONObject jsonData) {
    try {
      OBContext.setAdminMode(true);
      final String recordId = jsonData.getString("recordId");
      final String tabId = jsonData.getString("tabId") == null ? "" : jsonData.getString("tabId");
      final String btnName = jsonData.optString("button") == "" ? null
          : jsonData.getString("button");
      VariablesSecureApp vars = new VariablesSecureApp(request);

      /* Proposal Management - Bank Guarantee Detail */
      if (tabId.equals("614665E1FB764B38A0EAA6153B110824")) {
        if (!recordId.equals("")) {
          EscmProposalMgmt propmgnt = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
          if (propmgnt.getBidType() == null || propmgnt.getBidType().equals("TR")
              || propmgnt.getBidType().equals("LD")) {
            enable = 1;
          }
        }
      }
      /* Proposal Management- Lines */
      else if (tabId.equals("88E026FD2D0446048C80E9D4749AB608")) {
        if (btnName == null) {
          if (!recordId.equals("")) {
            EscmProposalMgmt propmgnt = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
            if (!propmgnt.getProposalstatus().equals("DR") || propmgnt.getEscmBidmgmt() != null) {
              enable = 1;
            }
          }
        }
        /* proposal management tab. if bid is associated then we should not allow to delete. */
        else if (btnName != null && btnName.equals("delete")) {
          if (!recordId.equals("")) {
            EscmProposalMgmt propmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
            if (propmgmt.getEscmBidmgmt() != null
                || propmgmt.getEscmProposalmgmtLineList().size() == 0
                || propmgmt.getEscmBaseproposal() != null) {
              enable = 1;
            }
          }
        }
      }
      /*
       * Proposal Management-Proposal Management Print button enable for status-submitted and
       * Awarded
       */
      else if (tabId.equals("D6115C9AF1DD4C4C9811D2A69E42878B")) {
        if (btnName != null && btnName.equals("delete")) {
          if (!recordId.equals("")) {
            try {
              String preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
                  vars.getClient(), vars.getOrg(), vars.getUser(), vars.getRole(),
                  "CAF2D3EEF3B241018C8F65E8F877B29F");
              if (preferenceValue != null && preferenceValue.equals("Y"))
                ispreference = true;
            } catch (PropertyException e) {
              ispreference = false;
            }
            if (ispreference) {
              enable = 0;
            } else {
              enable = 1;
            }
          }
        } else if (btnName == null) {
          if (!recordId.equals("")) {
            EscmProposalMgmt propmgnt = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
            if (propmgnt != null)
              if ((propmgnt.getProposalstatus().equals("SUB"))) {
                enable = 1;
              } else if ((propmgnt.getProposalstatus().equals("AWD"))
                  && (propmgnt.getProposalappstatus().equals("APP"))) {
                enable = 1;
              }
          }
        } else if (btnName.equals("print")) {
          if (!recordId.equals("")) {
            EscmProposalMgmt propmgnt = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
            if (propmgnt != null)
              if ((propmgnt.getProposalstatus().equals("SUB"))) {
                enable = 1;
              } else if ((propmgnt.getProposalstatus().equals("AWD"))
                  && (propmgnt.getProposalappstatus().equals("APP"))) {
                enable = 1;
              }
          }
        }

      }
      /* Proposal Management-Source Reference */
      else if (tabId.equals("8876DC52E0214C1C8A442F88784A9ACD")) {
        if (!recordId.equals("")) {
          EscmProposalmgmtLine proposalLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
              recordId);
          if (proposalLine != null) {
            if ((proposalLine.getEscmProposalmgmt().getProposalstatus() != null
                && !(proposalLine.getEscmProposalmgmt().getProposalstatus().equals("DR"))))
              enable = 1;
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in getIconVariables(): ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
