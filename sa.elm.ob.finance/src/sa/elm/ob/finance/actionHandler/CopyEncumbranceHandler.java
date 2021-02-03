package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author poongodi 03/01/2018
 *
 */
public class CopyEncumbranceHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(CopyEncumbranceHandler.class);

  @SuppressWarnings("static-access")
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    final DataToJsonConverter jsonConverter = new DataToJsonConverter();
    JSONObject json = new JSONObject();
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {

      OBContext.setAdminMode();
      String encumbranceId = (String) parameters.get("encumbranceId");
      String userId = (String) parameters.get("userId");
      SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      User user = OBDal.getInstance().get(User.class, userId);
      EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
          encumbranceId);

      Organization org = null;
      String CalId = null;
      org = OBDal.getInstance().get(Organization.class, encumbrance.getOrganization().getId());
      if (org.getCalendar() != null) {
        CalId = org.getCalendar().getId();
      } else {
        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession()
            .createSQLQuery("select eut_parent_org ('" + encumbrance.getOrganization().getId()
                + "','" + encumbrance.getClient().getId() + "')");
        @SuppressWarnings("unchecked")
        List<String> list = query.list();
        orgIds = list.get(0).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
          if (org.getCalendar() != null) {
            CalId = org.getCalendar().getId();
            break;
          }
        }
      }

      EfinBudgetManencum copyencumbrance = (EfinBudgetManencum) DalUtil.copy(encumbrance, false);
      copyencumbrance.setDocumentStatus("DR");
      copyencumbrance.setAction("CO");
      String seqno = UtilityDAO.getGeneralSequence(df.format(encumbrance.getAccountingDate()), "GS",
          CalId, encumbrance.getOrganization().getId(), true);

      copyencumbrance.setDocumentNo(seqno);
      copyencumbrance.setCreationDate(new java.util.Date());
      copyencumbrance.setCreatedBy(user);
      copyencumbrance.setUpdatedBy(user);
      copyencumbrance.setUpdated(new java.util.Date());
      copyencumbrance.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      copyencumbrance.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      copyencumbrance.setUpdated(new java.util.Date());
      copyencumbrance.setNextRole(null);
      copyencumbrance.setAmount(new BigDecimal(0));
      copyencumbrance.setAppliedAmount(new BigDecimal(0));
      copyencumbrance.setRevamount(new BigDecimal(0));
      copyencumbrance.setEncumStage(encumbrance.getEncumType());
      OBDal.getInstance().save(copyencumbrance);

      if (encumbrance.getEfinBudgetManencumlinesList().size() > 0) {
        for (EfinBudgetManencumlines line : encumbrance.getEfinBudgetManencumlinesList()) {
          EfinBudgetManencumlines copyLines = (EfinBudgetManencumlines) DalUtil.copy(line, false);
          copyLines.setManualEncumbrance(copyencumbrance);
          copyLines.setCreationDate(new java.util.Date());
          copyLines.setCreatedBy(OBDal.getInstance().get(User.class, userId));
          copyLines.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          copyLines.setUpdated(new java.util.Date());
          copyLines.setENCIncrease(new BigDecimal(0));
          copyLines.setENCDecrease(new BigDecimal(0));
          copyLines.setRevamount(line.getAmount());
          copyLines.setAPPAmt(new BigDecimal(0));
          copyLines.setUsedAmount(new BigDecimal(0));
          copyLines.setRemainingAmount(new BigDecimal(0));
          OBDal.getInstance().save(copyLines);

        }
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(copyencumbrance);

      json = jsonConverter.toJsonObject(copyencumbrance, DataResolvingMode.FULL);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      return json;

    } catch (Exception e) {
      log.error("Exception in CopyEncumbranceHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
