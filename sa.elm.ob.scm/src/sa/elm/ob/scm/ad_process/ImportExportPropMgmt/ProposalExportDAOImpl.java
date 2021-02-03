package sa.elm.ob.scm.ad_process.ImportExportPropMgmt;

import java.util.HashMap;

import org.openbravo.dal.service.OBDal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;

/**
 * @author Kiruthika on 28/05/2020
 */

public class ProposalExportDAOImpl implements ProposalExportDAO {
  private static final Logger log4j = LoggerFactory.getLogger(ProposalExportDAOImpl.class);

  public HashMap<Integer, String> getProposalCellStyle(String proposalLineId) {
    HashMap<Integer, String> txtStyleMap = new HashMap<>();
    try {
      EscmProposalmgmtLine proposalLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
          proposalLineId);

      String proposalLineType = getProposalLineType(proposalLine);

      for (int i = 0; i <= 18; i++) {
        if (proposalLineType != null && proposalLineType.equals("BID")) {
          if (i == 1 || i == 2 || i == 7 || i == 10 || i == 11 || i == 12 || i == 13) {
            txtStyleMap.put(i, "NumericLock");
          } else if (i == 3 || i == 4 || i == 5 || i == 6 || i == 15 || i == 16) {
            txtStyleMap.put(i, "TextLock");
          } else {
            if (i == 8) {
              txtStyleMap.put(i, "NumericUnlock");
            } else if (i == 14 || i == 17 || i == 18) {
              txtStyleMap.put(i, "TextUnlock");
            } else if (i == 9) {
              if (proposalLine.getEscmProposalmgmt().getProposalstatus().equals("DR")) {
                txtStyleMap.put(i, "NumericUnlock");
              } else {
                txtStyleMap.put(i, "NumericLock");
              }
            }
          }
        } else {
          if (i == 1 || i == 10 || i == 11 || i == 12 || i == 13) {
            txtStyleMap.put(i, "NumericLock");
          } else if (i == 16 || i == 14) {
            txtStyleMap.put(i, "TextLock");
          } else if (i == 4 || i == 5 || i == 6) {
            if (proposalLine.getProduct() != null) {
              txtStyleMap.put(i, "TextLock");
            } else {
              txtStyleMap.put(i, "TextUnlock");
            }
          } else {
            if (i == 2) {
              txtStyleMap.put(i, "NumericUnlock");
            } else if (i == 3 || i == 15 || i == 17 || i == 18) {
              txtStyleMap.put(i, "TextUnlock");
            } else if (i == 7 || i == 8) {
              if (!proposalLine.isSummary()
                  && proposalLine.getEscmProposalmgmt().getProposalstatus().equals("DR")) {
                txtStyleMap.put(i, "NumericUnlock");
              } else {
                txtStyleMap.put(i, "NumericLock");
              }
            } else if (i == 9) {
              if (proposalLine.getEscmProposalmgmt().getProposalstatus().equals("DR")
                  && !proposalLine.isSummary()) {
                txtStyleMap.put(i, "NumericUnlock");
              } else {
                txtStyleMap.put(i, "NumericLock");
              }
            }
          }
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getPoCellStyle() Method : ", e);
    }
    return txtStyleMap;
  }

  public static String getProposalLineType(EscmProposalmgmtLine proposalLine) {

    String proposalLineType = null;

    if (proposalLine.getEscmBidmgmtLine() != null) {
      proposalLineType = "BID";
    } else {
      for (EscmProposalsourceRef srcRef : proposalLine.getEscmProposalsourceRefList()) {
        if (srcRef.getRequisitionLine() != null) {
          proposalLineType = "PR";
          break;
        }
      }
      if (proposalLineType == null) {
        proposalLineType = "MANUAL";
      }
    }
    return proposalLineType;
  }

}
