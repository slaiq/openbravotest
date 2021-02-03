package sa.elm.ob.finance.actionHandler;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;

/**
 * 
 * @author Divya.J-31-01-2018
 *
 */
public class PenaltyReleaseReadonlyActionHandler extends BaseActionHandler {

  private static final Logger LOG = LoggerFactory
      .getLogger(PenaltyReleaseReadonlyActionHandler.class);

  /**
   * This class is used to handle read only logic for Amount field in Penalty Release Popup
   */
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject json = new JSONObject();
    try {
      OBContext.setAdminMode();
      String rdvTxnLineId = null;
      JSONObject jsonRequest = new JSONObject(content);
      if (jsonRequest.has("rdvTxnlineId")) {
        rdvTxnLineId = jsonRequest.getString("rdvTxnlineId");
      }
      if (rdvTxnLineId != null) {
        // fetch rdv transaction version object and return the status Json Object
        EfinRDVTxnline rdvTxnLine = OBDal.getInstance().get(EfinRDVTxnline.class, rdvTxnLineId);
        EfinRDVTransaction rdvTransaction = null;
        OBQuery<EfinRDVTransaction> rdvtrxQry = OBDal.getInstance().createQuery(
            EfinRDVTransaction.class,
            " as e where e.id <> :ID and e.efinRdv.id= :rdvID and e.tXNVersion > :tXNVersion order by created desc   ");
        rdvtrxQry.setNamedParameter("ID", rdvTxnLine.getEfinRdvtxn().getId());
        rdvtrxQry.setNamedParameter("rdvID", rdvTxnLine.getEfinRdvtxn().getEfinRdv().getId());
        rdvtrxQry.setNamedParameter("tXNVersion", rdvTxnLine.getEfinRdvtxn().getTXNVersion());
        rdvtrxQry.setMaxResult(1);
        if (rdvtrxQry.list().size() > 0) {
          rdvTransaction = rdvtrxQry.list().get(0);
        }
        json.put("status", rdvTransaction.getAppstatus());
        return json;
      }
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Penalty Release read only action handler :", e);
      }
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }

}
