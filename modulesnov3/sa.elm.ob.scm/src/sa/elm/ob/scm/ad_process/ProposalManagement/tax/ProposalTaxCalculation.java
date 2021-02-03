package sa.elm.ob.scm.ad_process.ProposalManagement.tax;

import java.text.DecimalFormat;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Rashika.V.S on 21-02-2019
 *
 */

public class ProposalTaxCalculation implements Process {

  private static final Logger log4j = Logger.getLogger(ProposalTaxCalculation.class);
  private OBError obError = new OBError();
  private static final String AWARDED = "AWD";
  private static final String CANCELLED = "CA";
  private static final String WITHDRAWN = "WD";
  private static final String DISCARDED = "DIS";

  @Override
  public synchronized void execute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      ProposalTaxCalculationDAO dao = null;
      HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      dao = new ProposalTaxCalculationDAOImpl();

      String proposalId = (String) bundle.getParams().get("Escm_Proposalmgmt_ID");

      if (StringUtils.isNotEmpty(proposalId)) {
        EscmProposalMgmt proposalMgmt = Utility.getObject(EscmProposalMgmt.class, proposalId);

        DecimalFormat euroRelationFmt = org.openbravo.erpCommon.utility.Utility.getFormat(vars,
            "euroRelation");
        Integer decimalFormat = euroRelationFmt.getMaximumFractionDigits();

        // if (!proposalMgmt.isVersion() && proposalMgmt.getProposalstatus().equals(AWARDED)
        // || proposalMgmt.getProposalstatus().equals(CANCELLED)
        // || proposalMgmt.getProposalstatus().equals(DISCARDED)
        // || proposalMgmt.getProposalstatus().equals(WITHDRAWN)) {
        // obError = OBErrorBuilder.buildMessage(null, "error",
        // "@Escm_AlreadyPreocessed_Approved@");
        // bundle.setResult(obError);
        // return;
        // }

        if (proposalMgmt.isTaxLine() && proposalMgmt.getEfinTaxMethod() == null) {
          obError = OBErrorBuilder.buildMessage(null, "error", "@Efin_NoTaxMethod@");
          bundle.setResult(obError);
          return;
        }

        obError = dao.insertTaxAmount(proposalMgmt, decimalFormat);
        List<EscmProposalmgmtLine> taxableLines = dao.getProposalLines(proposalMgmt);
        if (taxableLines.size() > 0) {
          for (EscmProposalmgmtLine line : taxableLines) {
            line.setProcess(false);
            OBDal.getInstance().save(line);
          }
        }
        bundle.setResult(obError);
        return;
      }

    } catch (Exception e) {
      log4j.error("Exception while adding tax lines : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}