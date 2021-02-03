package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

import sa.elm.ob.scm.MaterialIssueRequestLine;

public class CopyMIRLineHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(CopyMIRLineHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    final DataToJsonConverter jsonConverter = new DataToJsonConverter();
    JSONObject json = new JSONObject();

    try {
      OBContext.setAdminMode();
      // JSONObject jsondata = new JSONObject(content);
      String mirLineId = (String) parameters.get("mirLineId");// jsondata.getString("mirLineId");
      String userId = (String) parameters.get("userId");// jsondata.getString("userId");

      // Boolean ispreference = false;
      /*
       * Boolean ispreference = Preferences.existsPreference("ESCM_ProcurementDirector", true, null,
       * null, user.getId(), null, null);
       */

      MaterialIssueRequestLine origmirLine = OBDal.getInstance().get(MaterialIssueRequestLine.class,
          mirLineId);
      OBQuery<MaterialIssueRequestLine> lineQry = OBDal.getInstance()
          .createQuery(MaterialIssueRequestLine.class, " as e where e.escmMaterialRequest.id='"
              + origmirLine.getEscmMaterialRequest().getId() + "' order by e.lineNo desc");
      lineQry.setMaxResult(1);
      Long lineNo = (lineQry.list().size()) > 0 ? lineQry.list().get(0).getLineNo() + 10 : 10;
      MaterialIssueRequestLine copyLines = (MaterialIssueRequestLine) DalUtil.copy(origmirLine);
      System.out.println(origmirLine.isGeneric());
      // BigDecimal reqQty = origmirLine.getRequestedQty();
      // BigDecimal issuedQty = origmirLine.getDeliveredQantity();
      copyLines.setCreationDate(new java.util.Date());
      copyLines.setCreatedBy(OBDal.getInstance().get(User.class, userId));
      copyLines.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
      copyLines.setUpdated(new java.util.Date());
      copyLines.setNeedByDate(new java.util.Date());
      copyLines.setLineNo(lineNo);
      copyLines.setDeliveredQantity(BigDecimal.ZERO);
      copyLines.setCopy(true);
      OBDal.getInstance().save(copyLines);
      json = jsonConverter.toJsonObject(copyLines, DataResolvingMode.FULL);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      // json.put("message", "Success");
      return json;

    } catch (Exception e) {
      log.error("Exception in CopyPurchaseRequsitionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
