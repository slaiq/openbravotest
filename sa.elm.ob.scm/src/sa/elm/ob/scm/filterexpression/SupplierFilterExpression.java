package sa.elm.ob.scm.filterexpression;

/**
 * @author Qualian
 */
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.scm.Escmsalesvoucher;

/**
 * This class is to apply default filter for supplier in Proposal management.
 */
@SuppressWarnings("unused")
public class SupplierFilterExpression implements FilterExpression {

  private final static Logger log4j = Logger.getLogger(SupplierFilterExpression.class);
  private Map<String, String> requestMap;
  private HttpSession httpSession;

  private String windowId;

  @Override
  public String getExpression(Map<String, String> _requestMap) {
    requestMap = _requestMap;
    httpSession = RequestContext.get().getSession();
    windowId = requestMap.get(OBBindingsConstants.WINDOW_ID_PARAM);
    String inpescmBidmgmtId = requestMap.get("inpescmBidmgmtId");
    String Bidtype = requestMap.get("inpbidtype");
    String DocNo = "";
    List<Escmsalesvoucher> voucherList = new ArrayList<Escmsalesvoucher>();

    if (inpescmBidmgmtId != null && Bidtype.equals("TR")) {
      OBQuery<Escmsalesvoucher> salesvoucher = OBDal.getInstance().createQuery(
          Escmsalesvoucher.class, "escmBidmgmt.id='" + inpescmBidmgmtId + "'");
      voucherList = salesvoucher.list();
      DocNo = voucherList.get(0).getSupplierNumber().getEfinDocumentno();

      /*
       * for (Escmsalesvoucher voucher : voucherList) { if (DocNo.equals("")) { DocNo =
       * voucher.getSupplierNumber().getEfinDocumentno(); } else { DocNo = DocNo + " or ==" +
       * voucher.getSupplierNumber().getEfinDocumentno(); } }
       */

      return DocNo;
    } else {
      return DocNo;
    }

  }

}
