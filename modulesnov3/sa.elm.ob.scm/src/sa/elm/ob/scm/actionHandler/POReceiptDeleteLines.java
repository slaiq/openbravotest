package sa.elm.ob.scm.actionHandler;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinRdvTxnLineRef;
import sa.elm.ob.scm.EscmInitialReceipt;

/**
 * This class is used to delete the POReceipt Lines.
 * 
 * @author Gokul 19/12/2018
 *
 */
public class POReceiptDeleteLines extends DalBaseProcess {
  private static final Logger log = LoggerFactory.getLogger(POReceiptDeleteLines.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    boolean Status = false;
    List<EscmInitialReceipt> poReceiptLine = new ArrayList<EscmInitialReceipt>();
    try {
      OBContext.setAdminMode();

      final String poReceiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut poReceipt = OBDal.getInstance().get(ShipmentInOut.class, poReceiptId);
      poReceiptLine = poReceipt.getEscmInitialReceiptList();
      for (EscmInitialReceipt lines : poReceiptLine) {
        OBQuery<EfinRdvTxnLineRef> rdvLine = OBDal.getInstance()
            .createQuery(EfinRdvTxnLineRef.class, " as e where e.escmInitialreceipt.id=:LineId");
        rdvLine.setNamedParameter("LineId", lines.getId());
        if (rdvLine != null && rdvLine.list().size() > 0) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PoReceipt_Used_RDV@");
          bundle.setResult(result);
          return;
        }
      }

      Status = POReceiptDeleteLinesDAO.deletelines(poReceipt);

      OBDal.getInstance().save(poReceipt);

      if (Status) {
        if (poReceipt.getDocumentType() != null
            && poReceipt.getDocumentType().isEscmIsporeceipt()) {
          poReceipt.setEscmReceivetype("QTY");
        }

        OBDal.getInstance().flush();
        OBError result = OBErrorBuilder.buildMessage(null, "success", "@Escm_Ir_complete_success@");
        bundle.setResult(result);
        return;
      } else {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ProcessFailed@");
        bundle.setResult(result);
        return;
      }
    } catch (Exception e) {
      log.error("Exeception in PO Receipt Lines delete:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
