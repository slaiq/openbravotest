package sa.elm.ob.finance.actionHandler;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.gl.GLJournalLine;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

import sa.elm.ob.finance.actionHandler.dao.SimpleGLJournalHandlerDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author Kousalya 28/12/2017
 *
 */
public class CopySimpleGLJournalHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(CopySimpleGLJournalHandler.class);

  @SuppressWarnings("static-access")
  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    final DataToJsonConverter jsonConverter = new DataToJsonConverter();
    JSONObject json = new JSONObject();
    try {
      SimpleGLJournalHandlerDAO dao = new SimpleGLJournalHandlerDAO();
      OBContext.setAdminMode();
      String journalId = (String) parameters.get("journalId");
      String userId = (String) parameters.get("userId");
      SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      User user = OBDal.getInstance().get(User.class, userId);
      GLJournal glJournal = OBDal.getInstance().get(GLJournal.class, journalId);

      Organization org = null;
      String CalId = null;
      org = OBDal.getInstance().get(Organization.class, glJournal.getOrganization().getId());
      if (org.getCalendar() != null) {
        CalId = org.getCalendar().getId();
      } else {
        // get parent organization list
        String[] orgIds = null;
        SQLQuery query = OBDal.getInstance().getSession().createSQLQuery("select eut_parent_org ('"
            + glJournal.getOrganization().getId() + "','" + glJournal.getClient().getId() + "')");
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

      GLJournal copyglJournal = (GLJournal) DalUtil.copy(glJournal, false);
      copyglJournal.setDocumentStatus("DR");
      copyglJournal.setDocumentAction("CO");
      copyglJournal.setProcessNow(false);
      copyglJournal.setProcessed(false);
      copyglJournal.setPosted("N");
      copyglJournal.setEfinPostreq("N");
      copyglJournal.setEfinUnpostreq(false);
      copyglJournal.setTotalCreditAmount(BigDecimal.ZERO);
      copyglJournal.setTotalDebitAmount(BigDecimal.ZERO);
      copyglJournal.setDocumentDate(new java.util.Date());
      copyglJournal.setAccountingDate(new java.util.Date());
      String seqno = UtilityDAO.getGeneralSequence(df.format(glJournal.getAccountingDate()), "GS",
          CalId, glJournal.getOrganization().getId(), true);

      copyglJournal.setDocumentNo(seqno);
      copyglJournal.setCreationDate(new java.util.Date());
      copyglJournal.setCreatedBy(user);
      copyglJournal.setUpdatedBy(user);
      copyglJournal.setUpdated(new java.util.Date());
      copyglJournal.setEutNextRole(null);
      OBDal.getInstance().save(copyglJournal);

      // String uniqueCodeLs = "", uniqueCode=null;
      if (glJournal.getFinancialMgmtGLJournalLineList().size() > 0) {
        for (GLJournalLine line : glJournal.getFinancialMgmtGLJournalLineList()) {
          GLJournalLine copyLines = (GLJournalLine) DalUtil.copy(line);
          copyLines.setJournalEntry(copyglJournal);
          copyLines.setCreationDate(new java.util.Date());
          copyLines.setCreatedBy(OBDal.getInstance().get(User.class, userId));
          copyLines.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          copyLines.setUpdated(new java.util.Date());
          copyLines.setEfinCheckingStaus(null);
          Map<String, String> ucodeMap = dao.checkUniqueCode(
              line.getAccountingCombination().getId(), glJournal.getEFINBudgetDefinition().getId());
          if (ucodeMap != null) {
            copyLines.setEfinFundsAvailable(new BigDecimal(ucodeMap.get("fundAvailable")));
          }

          /*
           * Map<String, String> ucodeMap = dao.getUniqueCode(vars.getRole(),
           * glJournal.getAccountingSchema().getId(), journalId, glJournal.isMultigeneralLedger(),
           * uniqueCodeLs, glJournal.getEFINBudgetDefinition().getId()); if(ucodeMap!=null){
           * uniqueCode = ucodeMap.get("uniqueCode"); uniqueCodeLs+=",'"+uniqueCode+"'";
           * 
           * AccountingCombination combination =
           * OBDal.getInstance().get(AccountingCombination.class, uniqueCode);
           * 
           * copyLines.setOrganization(OBDal.getInstance().get(Organization.class,
           * combination.getOrganization().getId()));
           * copyLines.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
           * combination.getBusinessPartner() != null ? combination.getBusinessPartner().getId():
           * null)); copyLines.setSalesRegion(OBDal.getInstance().get(SalesRegion.class,
           * combination.getSalesRegion() != null ? combination.getSalesRegion().getId() : null));
           * copyLines.setActivity(OBDal.getInstance().get(ABCActivity.class,
           * combination.getActivity() != null ? combination.getActivity().getId() : null));
           * copyLines.setProject(OBDal.getInstance().get(Project.class, combination.getProject() !=
           * null ? combination.getProject().getId() : null));
           * copyLines.setSalesCampaign(OBDal.getInstance().get(Campaign.class,
           * combination.getSalesCampaign() != null ? combination.getSalesCampaign().getId() :
           * null)); copyLines.setStDimension(OBDal.getInstance().get(UserDimension1.class,
           * combination.getStDimension() != null ? combination.getStDimension().getId() : null));
           * copyLines.setNdDimension(OBDal.getInstance().get(UserDimension2.class,
           * combination.getNdDimension() != null ? combination.getNdDimension().getId() : null));
           * copyLines.setEfinAccount(OBDal.getInstance().get(ElementValue.class,
           * combination.getAccount() != null ? combination.getAccount().getId() : null));
           * copyLines.setEfinUniquecodevalue(combination.getEfinUniquecodename() != null ?
           * combination.getEfinUniquecodename() : null);
           * copyLines.setEfinUniqueCode(combination.getEfinUniqueCode() != null ?
           * combination.getEfinUniqueCode() : null); copyLines.setEfinFundsAvailable(new
           * BigDecimal(ucodeMap.get("fundAvailable")));
           * copyLines.setAccountingCombination(OBDal.getInstance().get(AccountingCombination.class,
           * uniqueCode)); }
           */
          OBDal.getInstance().save(copyLines);
        }
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(copyglJournal);

      json = jsonConverter.toJsonObject(copyglJournal, DataResolvingMode.FULL);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      return json;

    } catch (Exception e) {
      log.error("Exception in CopySimpleGLJournalHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
