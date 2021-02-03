package sa.elm.ob.scm.ad_process.ProposalAttribute.Tax;

import java.text.DecimalFormat;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;

/**
 * @author DivyaPrakash.J.S on 01-03-2019
 *
 */

public class ProposalEvalTaxCalc implements Process {

  private static final Logger log4j = Logger.getLogger(ProposalEvalTaxCalc.class);
  private OBError obError = new OBError();

  @Override
  public synchronized void execute(ProcessBundle bundle) throws Exception {
    try {
      OBContext.setAdminMode();
      ProposalEvalTaxCalculationDAOImpl dao = null;
      HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String proposalId = "";
      dao = new ProposalEvalTaxCalculationDAOImpl();

      String proposalAttrId = (String) bundle.getParams().get("Escm_Proposal_Attr_ID");
      EscmProposalAttribute proposalAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
          proposalAttrId);
      proposalId = proposalAttr.getEscmProposalmgmt().getId();

      if (StringUtils.isNotEmpty(proposalId)) {
        EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class, proposalId);

        DecimalFormat euroRelationFmt = org.openbravo.erpCommon.utility.Utility.getFormat(vars,
            "euroRelation");
        Integer decimalFormat = euroRelationFmt.getMaximumFractionDigits();

        obError = dao.insertTaxAmount(proposalAttr, proposalMgmt, decimalFormat);
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