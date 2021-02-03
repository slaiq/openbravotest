package sa.elm.ob.scm.ad_process.BidManagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbidsourceref;

/**
 * @author Gokul on 24-05-2020
 */

public class BidExportDAOImpl implements BidExportDAO {
  private static final Logger log4j = LoggerFactory.getLogger(BidExportDAOImpl.class);

  public HashMap<Integer, String> getBidCellStyle(String bidLineId) {
    HashMap<Integer, String> txtStyleMap = new HashMap<>();
    Escmbidsourceref bidsrcref = null;
    List<Escmbidsourceref> bidsrcrefList = new ArrayList<Escmbidsourceref>();
    EscmBidMgmt bidmgmt = null;
    try {
      OBQuery<Escmbidsourceref> srcref = OBDal.getInstance().createQuery(Escmbidsourceref.class,
          "as e where escmBidmgmtLine.id=:bidLineId");
      srcref.setNamedParameter("bidLineId", bidLineId);
      bidsrcrefList = srcref.list();
      if (bidsrcrefList != null && bidsrcrefList.size() > 0) {
        bidsrcref = bidsrcrefList.get(0);
        bidmgmt = bidsrcref.getEscmBidmgmtLine().getEscmBidmgmt();

        for (int i = 0; i <= 9; i++) {

          // Add Requisition
          if (bidsrcref.getRequisition() != null) {
            if (i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 6 || i == 7 || i == 9) {
              if (i == 8 && bidsrcref.getEscmBidmgmtLine().isSummarylevel()) {
                txtStyleMap.put(i, "NumericLock");
              } else {
                txtStyleMap.put(i, "NumericUnlock");
              }
            } else if ((i == 8 && !bidsrcref.getEscmBidmgmtLine().isSummarylevel())) {
              txtStyleMap.put(i, "TextUnlock");
            } else {
              txtStyleMap.put(i, "TextLock");
            }
          } else {
            if (i == 1 || i == 2 || i == 3 || i == 4 || i == 5 || i == 6 || i == 7 || i == 9) {
              txtStyleMap.put(i, "NumericUnlock");
            }
            if (i == 8 && bidsrcref.getEscmBidmgmtLine().isSummarylevel()) {
              txtStyleMap.put(i, "TextLock");
            } else {
              txtStyleMap.put(i, "TextUnlock");
            }
          }
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in getBidCellStyle() Method : ", e);
    }
    return txtStyleMap;
  }

}
