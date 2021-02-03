package sa.elm.ob.finance.ad_process.RDVProcess;

/**
 * 
 * @author Kiruthika
 * 
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.RDVMatchAllDAO;;

/**
 * This class is used to update the Match Flag, Matched Quantity, Matched Amount and Net Match
 * Amount in RDV lines when Match All button is clicked
 */
public class RDVMatchAll extends DalBaseProcess {

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    JSONObject json = null;
    try {
      OBContext.setAdminMode();
      final String efin_Rdvtxn_ID = (String) bundle.getParams().get("Efin_Rdvtxn_ID");
      final String isDefaultPenalty = (String) bundle.getParams().get("defaultpenalty");
      PreparedStatement ps = null;
      ResultSet rs = null;
      Date now = new Date();
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      String todaydate = dateFormat.format(now);
      OBError result;
      if (efin_Rdvtxn_ID.isEmpty()) {
        result = OBErrorBuilder.buildMessage(null, "error", "@Efin_RdvtxnVersionError@");
        bundle.setResult(result);
      } else {
        EfinRDVTransaction txn = OBDal.getInstance().get(EfinRDVTransaction.class, efin_Rdvtxn_ID);

        result = RDVMatchAllDAO.matchAll(efin_Rdvtxn_ID, isDefaultPenalty);
        bundle.setResult(result);

        if (txn.isWebservice()) {
          setDoCommit(false);
        }

        if (!txn.getEfinRdv().getTXNType().equals("POD") && isDefaultPenalty.equals("Y")) {

          List<EfinRDVTxnline> txnline = txn.getEfinRDVTxnlineList();

          for (EfinRDVTxnline line : txnline) {

            AddDefaultPenaltyService addPenaltyService = new AddDefaultPenaltyServiceImpl();

            json = new JSONObject();
            json = addPenaltyService.applicableForPenalty(line.getId(),
                line.getMatchQty().toString(), line.getMatchAmt().toString());

            if (json.get("addPenalty").equals("true")) {
              ps = OBDal.getInstance().getConnection().prepareStatement(
                  " select eut_convert_to_hijri ('" + todaydate + "' ) as hijiri");

              rs = ps.executeQuery();
              if (rs.next()) {
                String strActionDate = rs.getString("hijiri");
                json = addPenaltyService.addPenalty(line.getId(), line.getMatchQty().toString(),
                    strActionDate, line.getADVDeduct().toString(), line.getMatchAmt().toString());
              }
            }
          }
        }
      }
    } catch (OBException e) {
      e.printStackTrace();
      throw new OBException(e.getMessage());
    }
  }
}