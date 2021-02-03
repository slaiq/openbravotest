package sa.elm.ob.scm.ad_process.OpenEnvlopCommitee;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmannoucements;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.Escmopenenvcommitee;

/**
 * 
 * @author qualian-Kousalya
 */
public class OpenEnvCommiteeReviewProposals implements Process {
  private static final Logger log = Logger.getLogger(OpenEnvCommiteeReviewProposals.class);
  private final OBError obError = new OBError();

  /**
   * This process will be delete the invalid Proposals and will add the proposals with valid .
   */
  @SuppressWarnings("unused")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Open Envelop Committe Review Proposals");
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    final String openenvid = (String) bundle.getParams().get("Escm_Openenvcommitee_ID").toString();
    Escmopenenvcommitee openenvcommitee = OBDal.getInstance().get(Escmopenenvcommitee.class,
        openenvid);

    Connection connection = null;
    try {
      ConnectionProvider provider = bundle.getConnection();
      connection = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }
    try {
      OBContext.setAdminMode(true);
      if (openenvcommitee.getAlertStatus().equals("DR") && openenvcommitee.getBidNo() != null) {
        BigDecimal lineTotal = BigDecimal.ZERO;
        // Delete cancelled proposal and add valid proposal for the bid
        List<EscmProposalAttribute> attrToDeleteList = new ArrayList<EscmProposalAttribute>();
        // getting proposal attribute list from proposal evaluation event.
        if (openenvcommitee.getEscmProposalAttrList().size() > 0) {
          openenvcommitee.setDeleteLines(false);
          OBDal.getInstance().save(openenvcommitee);
          OBDal.getInstance().flush();
          for (EscmProposalAttribute attr : openenvcommitee.getEscmProposalAttrList()) {
            EscmProposalMgmt proposal = attr.getEscmProposalmgmt();
            attr.setRank(null);
            attr.setDiscardedReason(null);
            attr.setProposalstatus(null);
            attr.setPEETechDiscount(new BigDecimal("0"));
            attr.setPEETechDiscountamt(new BigDecimal("0"));

            // line updation
            for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
              lineTotal = line.getNetprice().multiply((line.getMovementQuantity()));
              line.setGrossUnitPrice(BigDecimal.ZERO);
              // line.setNetprice(BigDecimal.ZERO);
              line.setPEETechDiscount(new BigDecimal(0));
              line.setPEETechDiscountamt(new BigDecimal(0));
              line.setDiscount(line.getTechDiscount());
              line.setDiscountmount(line.getTechDiscountamt());
              line.setPeestatus(null);
              if (line.getEscmProposalmgmt().getProposalstatus().equals("DR")
                  || line.getEscmProposalmgmt().getProposalstatus().equals("SUB")) {
                line.setPEEQty(line.getMovementQuantity());
                line.setPEELineTotal(line.getMovementQuantity().multiply(line.getGrossUnitPrice()));
              } else {
                line.setPEELineTotal(line.getTechLineTotal());
                // // after delete proposal in PEE revert qty also in proposal management line
                line.setPEEQty(line.getTechLineQty());
              }
              if ((line.getPEENegotUnitPrice().compareTo(line.getNetprice()) != 0)) {
                if (!line.getEscmProposalmgmt().getProposalType().equals("DR")
                    && line.getTechDiscountamt() != null) {
                  line.setPEENegotUnitPrice(line.getTechUnitPrice());
                } else {
                  line.setPEENegotUnitPrice(line.getGrossUnitPrice());
                }
                OBDal.getInstance().save(line);
              }
            }
            if (!proposal.getProposalstatus().equals("CL")) {
              proposal.setRank(null);
              proposal.setProposalstatus("SUB");
              OBDal.getInstance().save(proposal);
            }

            attrToDeleteList.add(attr);
            OBQuery<ESCMBGWorkbench> bgworkbench = OBDal.getInstance()
                .createQuery(ESCMBGWorkbench.class, " as e where e.escmProposalAttr.id=:attrId");
            bgworkbench.setNamedParameter("attrId", attr.getId());
            log.debug("listsize:" + bgworkbench.list().size());
            if (bgworkbench.list().size() > 0) {
              for (ESCMBGWorkbench bg : bgworkbench.list()) {
                for (Escmbankguaranteedetail bgdet : bg.getEscmBankguaranteeDetailList()) {
                  bgdet.setEscmProposalAttr(null);
                  OBDal.getInstance().save(bgdet);
                }
                bg.setEscmProposalAttr(null);
                OBDal.getInstance().save(bg);
              }
              OBDal.getInstance().flush();
            }
          }
          // remove the proposal attribute when proposal bid id direct or without bid
          openenvcommitee.getEscmProposalAttrList().removeAll(attrToDeleteList);
          for (EscmProposalAttribute attr : attrToDeleteList) {
            OBDal.getInstance().remove(attr);
          }
          OBDal.getInstance().flush();
        }
        Escmannoucements announ = openenvcommitee.getEscmAnnouncement();
        openenvcommitee.setEscmAnnouncement(null);
        openenvcommitee.setDeleteLines(true);
        OBDal.getInstance().flush();
        // Add Valid Proposal
        openenvcommitee.setEscmAnnouncement(announ);
        openenvcommitee.setDeleteLines(false);
        OBDal.getInstance().save(openenvcommitee);
        OBDal.getInstance().flush();
        if (openenvcommitee.getEscmProposalAttrList().size() == 0) {
          openenvcommitee.setDeleteLines(false);
        }
      }
      obError.setType("Success");
      obError.setTitle("Success");
      obError.setMessage(OBMessageUtils.messageBD("ESCM_PEE_ReviewSucess"));
      bundle.setResult(obError);
      // OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (

    Exception e) {
      // bundle.setResult(obError);
      log.error("exception :", e);
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
