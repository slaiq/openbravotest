package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmbidmgmtline;

public class MergedProposalDao {

  private static Logger log = Logger.getLogger(MergedProposalDao.class);
  private static boolean isStatusUpdated = false;

  public static boolean validateQty(HashMap<String, BigDecimal> qtyMap,
      HashMap<String, BigDecimal> qtyLineMap) {

    try {
      OBContext.setAdminMode();

      for (String bidMgmtLineId : qtyMap.keySet()) {
        BigDecimal qty = qtyMap.get(bidMgmtLineId);
        BigDecimal awardedQty = BigDecimal.ZERO;

        Escmbidmgmtline line = OBDal.getInstance().get(Escmbidmgmtline.class, bidMgmtLineId);

        if (line != null) {
          // Entered quantity in selected line is greater than bid qty
          if (line.getMovementQuantity().compareTo(qty) < 0) {
            return false;
          }

          // Sum of all awarded qty in proposal line of bid line is greater than awarded qty
          for (EscmProposalmgmtLine lines : line.getEscmProposalmgmtLineList()) {
            if (qtyLineMap.containsKey(lines.getId())) {
              awardedQty = awardedQty.add(qtyLineMap.get(lines.getId()));
            } else {
              awardedQty = awardedQty.add(lines.getAwardedqty());
            }
          }

          if (awardedQty.compareTo(line.getMovementQuantity()) > 0) {
            return false;
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception while validating qty" + e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  public static void updateQty(HashMap<String, BigDecimal> qtyLineMap,
      HashMap<String, BigDecimal> qtyMap) {

    try {
      OBContext.setAdminMode();

      // update awarded quantity in proposal line
      for (String proposalMgmtLineId : qtyLineMap.keySet()) {
        EscmProposalmgmtLine line = OBDal.getInstance().get(EscmProposalmgmtLine.class,
            proposalMgmtLineId);
        if (line != null) {
          if (qtyLineMap.get(proposalMgmtLineId).compareTo(BigDecimal.ZERO) > 0) {
            line.getEscmProposalmgmt().setPartialAward(true);
            OBDal.getInstance().save(line.getEscmProposalmgmt());
          }
          line.setAwardedqty(qtyLineMap.get(proposalMgmtLineId));
          line.setAwardedamount(line.getNetUnitprice().multiply(line.getAwardedqty()).setScale(2,
              BigDecimal.ROUND_HALF_UP));
          OBDal.getInstance().save(line);
        }
      }
      OBDal.getInstance().flush();

      // update awarded quantity in bidline
      for (String bidMgmtLineId : qtyMap.keySet()) {
        Escmbidmgmtline bidLine = OBDal.getInstance().get(Escmbidmgmtline.class, bidMgmtLineId);
        if (bidLine != null) {
          bidLine.setAwardedqty(bidLine.getEscmProposalmgmtLineList().stream()
              .map(a -> a.getAwardedqty()).reduce(BigDecimal.ZERO, BigDecimal::add));
          OBDal.getInstance().save(bidLine);
        }
      }
      OBDal.getInstance().flush();

    } catch (Exception e) {
      log.error("Exception while updating qty" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void updateStatusInProposal(HashMap<String, BigDecimal> qtyLineMap,
      boolean awardfullqty) {
    try {
      OBContext.setAdminMode();
      // update proposal status in proposal header
      isStatusUpdated = false;
      String minimumDocumentno = "";
      for (String proposalMgmtId : qtyLineMap.keySet()) {
        EscmProposalMgmt header = OBDal.getInstance().get(EscmProposalMgmt.class, proposalMgmtId);
        if (header != null) {
          if (qtyLineMap.get(proposalMgmtId).compareTo(BigDecimal.ZERO) > 0) {

            if (!isStatusUpdated && awardfullqty) {
              List<String> documentnoList = header.getEscmBidmgmt().getEscmProposalManagementList()
                  .stream().map(a -> a.getProposalno()).collect(Collectors.toList());
              // sort it to get minimum proposal number
              Collections.sort(documentnoList);
              minimumDocumentno = documentnoList.get(0);
              OBQuery<EscmProposalMgmt> proposalQuery = OBDal.getInstance()
                  .createQuery(EscmProposalMgmt.class, "as e where e.proposalno =:proposalno");
              proposalQuery.setNamedParameter("proposalno", documentnoList.get(0));
              List<EscmProposalMgmt> proposalList = proposalQuery.list();
              if (proposalList.size() > 0) {
                EscmProposalMgmt proposal = proposalList.get(0);
                proposal.setProposalstatus("PAWD");
                OBDal.getInstance().save(proposal);
              }
              isStatusUpdated = true;
            }
            header.setProposalstatus("PAWD");
            BigDecimal awardAmount = header.getEscmProposalmgmtLineList().stream()
                .map(a -> a.getAwardedqty().multiply(a.getNetUnitprice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            header.setAwardamount(awardAmount);
          } else {
            if (!minimumDocumentno.equals(header.getProposalno())) {
              header.setProposalstatus("ANY");
              header.setAwardamount(BigDecimal.ZERO);
            }
          }
          OBDal.getInstance().save(header);
        }
      }
      OBDal.getInstance().flush();

    } catch (Exception e) {
      log.error("Exception while updating status in proposal" + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
