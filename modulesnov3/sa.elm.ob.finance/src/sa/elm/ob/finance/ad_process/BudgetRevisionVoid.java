package sa.elm.ob.finance.ad_process;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.sales.SalesRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EfinBudgetTransfer;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.efinbudgetencum;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author Gopalakrishnan on 06/06/2106
 */
public class BudgetRevisionVoid extends BaseProcessActionHandler {
  /**
   * BudgetRevision Void Process Table(efin_budget_transfer)
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(BudgetRevisionVoid.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    log.debug("rework the budget");
    HttpServletRequest request = RequestContext.get().getRequest();
    JSONObject jsonResponse = new JSONObject();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps1 = null, ps2 = null, ps3 = null, ps4 = null;
    ResultSet rs1 = null, rs2 = null, rs3 = null, rs4 = null;
    String query = "", query1 = "", query2 = "", query3 = "";
    String errorMsg = "", appstatus = "";
    boolean errorFlag = false;
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy-MM-dd");
    try {
      OBContext.setAdminMode();
      JSONObject jsonRequest = new JSONObject(content);
      // Get the Params value
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String BudRevId = jsonRequest.getString("inpefinBudgetTransfertrxId");
      log.debug("BudRevId:" + BudRevId);
      log.debug("actDate:" + jsonparams.getString("Efin_Act_Date"));
      log.debug("TrxDate:" + jsonparams.getString("Efin_Act_Date"));

      // getting Account and Transaction Date (Convert hijri to Gregorian)
      String AccountinDate = UtilityDAO.convertToGregorian(
          dateFormat.format(yearFormat.parse(jsonparams.getString("Efin_Act_Date"))));
      String TransactionDate = UtilityDAO.convertToGregorian(
          dateFormat.format(yearFormat.parse(jsonparams.getString("Efin_Trx_Date"))));

      AccountinDate = dateFormat.format(yearFormat.parse(AccountinDate));
      TransactionDate = dateFormat.format(yearFormat.parse(TransactionDate));

      log.debug("After Parse AccountinDate:" + AccountinDate);
      Date AcctDate = null;
      Date trxDate = null;
      try {
        AcctDate = Utility.dateFormat.parse(AccountinDate);
        trxDate = Utility.dateFormat.parse(TransactionDate);
      } catch (ParseException e) {
      }

      final String clientId = OBDal.getInstance().get(EfinBudgetTransfertrx.class, BudRevId)
          .getClient().getId();
      final String orgId = OBDal.getInstance().get(EfinBudgetTransfertrx.class, BudRevId)
          .getOrganization().getId();
      final String userId = vars.getUser();
      final String roleId = vars.getRole();
      String ActYear = "";
      String TrxYear = "";

      log.debug("role Id:" + roleId + ", User Id:" + userId);
      int count = 0;
      log.debug("budgetId:" + BudRevId);
      EfinBudgetTransfertrx OrgRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class, BudRevId);
      String strYearId = OrgRev.getYear().getId();

      // getting Accounting date year
      query = "SELECT yr.c_year_id as year from c_period  pr  join c_year yr on yr.c_year_id= pr.c_year_id "
          + " where to_date('" + AccountinDate
          + "','dd-MM-yyyy') between pr.startdate and pr.enddate " + " and yr.c_year_id='"
          + strYearId + "'";
      log.debug("query Accounting date year:" + query.toString());
      ps1 = conn.prepareStatement(query);
      rs1 = ps1.executeQuery();
      if (rs1.next()) {
        ActYear = rs1.getString("year");
      }
      // getting Transaction date year
      query = "SELECT yr.c_year_id as year from c_period  pr  join c_year yr on yr.c_year_id= pr.c_year_id "
          + " where to_date('" + TransactionDate
          + "','dd-MM-yyyy') between pr.startdate and pr.enddate " + " and yr.c_year_id='"
          + strYearId + "'";
      ps1 = conn.prepareStatement(query);
      rs1 = ps1.executeQuery();
      if (rs1.next()) {
        TrxYear = rs1.getString("year");
      }

      // Compare transaction date year and accounting date year with in year
      if ((!ActYear.equals(strYearId)) | (!TrxYear.equals(strYearId))) {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", OBMessageUtils.messageBD("Efin_DatesYear_varying"));
        jsonResponse.put("message", errormsg);
        return jsonResponse;
      }
      OrgRev.setVoidProcess(true);

      /*
       * 
       * Create Void Entry for Budget Revision
       */
      EfinBudgetTransfertrx objCloneRev = (EfinBudgetTransfertrx) DalUtil.copy(OrgRev, false);
      objCloneRev.setAccountingDate(AcctDate);
      objCloneRev.setTrxdate(trxDate);
      objCloneRev.setDocStatus("DR");
      objCloneRev.setEfinBudgetRevVoid(OrgRev);
      objCloneRev.setAction("CO");
      objCloneRev.setVoidProcess(true);
      objCloneRev.setDescription(OBMessageUtils.messageBD("Efin_Budget_Revision_Voided")
          .replace("@", OrgRev.getDocumentNo()));
      objCloneRev
          .setDocumentNo(getSequenceNo(clientId, "DocumentNo_Efin_Budget_Transfertrx", true));
      OBDal.getInstance().save(objCloneRev);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(objCloneRev);
      /*
       * Copy line to the new Budget Revision
       *
       */
      for (EfinBudgetTransfertrxline RevLine : OrgRev.getEfinBudgetTransfertrxlineList()) {
        EfinBudgetTransfertrxline objCloneLines = (EfinBudgetTransfertrxline) DalUtil.copy(RevLine,
            false);
        objCloneLines.setEfinBudgetTransfertrx(objCloneRev);
        objCloneLines.setDecrease(RevLine.getIncrease());
        objCloneLines.setIncrease(RevLine.getDecrease());
        OBDal.getInstance().save(objCloneLines);
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(objCloneRev);

      /*
       * Revision void Funds Budget Pre validations
       */
      // Checking Cost Budget Available for funds budget
      if (objCloneRev.getSalesCampaign().getEfinBudgettype().equals("F")
          && (objCloneRev.getDocType().equals("REV") || objCloneRev.getDocType().equals("TRS"))) {
        query = "select bud.efin_budget_id from efin_budget bud "
            + "join c_campaign typ on typ.c_campaign_id=bud.c_campaign_id"
            + " where  typ.em_efin_budgettype ='C'  and bud.c_year_id='"
            + objCloneRev.getYear().getId() + "' ";
        log.debug("CostBudgetQuery:" + query.toString());
        ps1 = conn.prepareStatement(query);
        rs1 = ps1.executeQuery();
        if (rs1.next()) {

          // Process the correct FundsBudget lines
          query1 = " select funds.trxlineid,funds.efin_budget_transfertrx_id,cost.current_budget from (select line.current_budget as  fundscbug , line.funds_available, coalesce(ln.increase,0) as fundsincrease , ln.efin_budget_transfertrxline_id as trxlineid ,ln.efin_budget_transfertrx_id, line.efin_budgetlines_id ,line.ad_org_id,line.c_salesregion_id,line.c_campaign_id,line.c_elementvalue_id, "
              + " line.c_project_id,line.c_activity_id, line.user1_id,line.user2_id from efin_budget_transfertrxline ln join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
              + " join efin_budget bud on bud.efin_budget_id =line.efin_budget_id where bud.c_year_id='"
              + objCloneRev.getYear().getId() + "'" + "  )funds  "
              + "  join (select efin_budgetlines_id,ad_org_id,c_salesregion_id,user2_id,user1_id,c_activity_id,c_elementvalue_id,c_campaign_id,c_project_id,current_budget from efin_budgetlines where efin_budget_id='"
              + rs1.getString("efin_budget_id") + "' ) cost "
              + "  on (funds.ad_org_id||funds.c_salesregion_id||funds.c_elementvalue_id||funds.c_project_id||funds.c_activity_id|| funds.user1_id|| funds.user2_id) = (cost.ad_org_id||cost.c_salesregion_id||cost.c_elementvalue_id||cost.c_project_id||cost.c_activity_id|| cost.user1_id|| cost.user2_id) "
              + "  where 1=1 and (funds.fundscbug+funds.fundsincrease) <= (cost.current_budget) and funds.fundsincrease >0 and funds.efin_budget_transfertrx_id='"
              + objCloneRev.getId() + "'";
          ps2 = conn.prepareStatement(query1);
          rs2 = ps2.executeQuery();
          while (rs2.next()) {
            EfinBudgetTransfertrxline trxline = OBDal.getInstance()
                .get(EfinBudgetTransfertrxline.class, rs2.getString("trxlineid"));
            trxline.setStatus("Success");
            // All correct Combination Updated

          }
          // Checking Funds Budget lines amount exceed from Cost Budget lines amount
          query1 = " select funds.trxlineid,funds.efin_budget_transfertrx_id,cost.current_budget from (select line.current_budget as  fundscbug , line.funds_available, coalesce(ln.increase,0) as fundsincrease , ln.efin_budget_transfertrxline_id as trxlineid ,ln.efin_budget_transfertrx_id, line.efin_budgetlines_id ,line.ad_org_id,line.c_salesregion_id,line.c_campaign_id,line.c_elementvalue_id, "
              + " line.c_project_id,line.c_activity_id, line.user1_id,line.user2_id from efin_budget_transfertrxline ln join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
              + " join efin_budget bud on bud.efin_budget_id =line.efin_budget_id where bud.c_year_id='"
              + objCloneRev.getYear().getId() + "'" + "  )funds  "
              + "  join (select efin_budgetlines_id,ad_org_id,c_salesregion_id,user2_id,user1_id,c_activity_id,c_elementvalue_id,c_campaign_id,c_project_id,current_budget from efin_budgetlines where efin_budget_id='"
              + rs1.getString("efin_budget_id") + "' ) cost "
              + "  on (funds.ad_org_id||funds.c_salesregion_id||funds.c_elementvalue_id||funds.c_project_id||funds.c_activity_id|| funds.user1_id|| funds.user2_id) = (cost.ad_org_id||cost.c_salesregion_id||cost.c_elementvalue_id||cost.c_project_id||cost.c_activity_id|| cost.user1_id|| cost.user2_id) "
              + "  where 1=1 and (funds.fundscbug+funds.fundsincrease) > (cost.current_budget) and funds.fundsincrease >0 and funds.efin_budget_transfertrx_id='"
              + objCloneRev.getId() + "'";
          log.debug("Exceed cost budgetamount increase query:" + query1.toString());
          ps2 = conn.prepareStatement(query1);
          rs2 = ps2.executeQuery();
          while (rs2.next()) {
            EfinBudgetTransfertrxline trxline = OBDal.getInstance()
                .get(EfinBudgetTransfertrxline.class, rs2.getString("trxlineid"));
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_FundsCost-Void");
            trxline.setStatus(status.replace("@", rs2.getString("current_budget")));
            errorFlag = true;
            errorMsg = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Failed-Void").replace("@",
                objCloneRev.getDocumentNo());

          }

        }
        // checking funds budget decrease amount corrects combination

        query2 = " select ln.increase,ln.decrease,ln.efin_budget_transfertrxline_id,bln.funds_available from efin_budget_transfertrxline ln  "
            + " join efin_budgetlines bln on bln.efin_budgetlines_id=ln.unique_code "
            + " join efin_budget bu on bu.efin_budget_id = bln.efin_budget_id "
            + " where 1=1 and ln.efin_budget_transfertrx_id='" + objCloneRev.getId()
            + "' and ln.increase=0 and ln.decrease <=  bln.funds_available and bu.c_year_id='"
            + objCloneRev.getYear().getId() + "'";

        log.debug("funds decrease correct lines query:" + query2.toString());
        ps3 = conn.prepareStatement(query2);
        rs3 = ps3.executeQuery();
        while (rs3.next()) {
          EfinBudgetTransfertrxline trxline = OBDal.getInstance().get(
              EfinBudgetTransfertrxline.class, rs3.getString("efin_budget_transfertrxline_id"));
          trxline.setStatus("Success");
          // funds budget decrease all correct combination updated
        }
        // checking funds budget decrease amount exceeds funds available
        query2 = " select ln.increase,ln.decrease,ln.efin_budget_transfertrxline_id,bln.funds_available from efin_budget_transfertrxline ln  "
            + " join efin_budgetlines bln on bln.efin_budgetlines_id=ln.unique_code "
            + " join efin_budget bu on bu.efin_budget_id = bln.efin_budget_id "
            + " where 1=1 and ln.efin_budget_transfertrx_id='" + objCloneRev.getId()
            + "' and ln.increase=0 and ln.decrease > bln.funds_available and bu.c_year_id='"
            + objCloneRev.getYear().getId() + "'";
        log.debug("funds decrease exceed amount:" + query2.toString());
        ps3 = conn.prepareStatement(query2);
        rs3 = ps3.executeQuery();
        while (rs3.next()) {
          EfinBudgetTransfertrxline trxline = OBDal.getInstance().get(
              EfinBudgetTransfertrxline.class, rs3.getString("efin_budget_transfertrxline_id"));
          String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Funds -void");
          trxline.setStatus(status.replace("@", rs3.getString("funds_available")));
          errorFlag = true;
          errorMsg = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Failed-Void").replace("@",
              objCloneRev.getDocumentNo());
        }

      }
      /*
       * Revision Cost Budget Prevalidations
       */
      if (objCloneRev.getSalesCampaign().getEfinBudgettype().equals("C")
          && (objCloneRev.getDocType().equals("REV") || objCloneRev.getDocType().equals("TRS"))) {

        // update all correct cost budget decrease amount
        query3 = " select line.efin_budgetlines_id ,line.funds_available as available ,ln.decrease as decrease,ln.efin_budget_transfertrxline_id from efin_budget_transfertrxline ln  join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
            + " where ln.efin_budget_transfertrx_id  ='" + objCloneRev.getId()
            + "' and ln.decrease <=  line.funds_available  and ln.increase = 0  ";
        ps4 = conn.prepareStatement(query3);
        log.debug("lines:" + ps4.toString());
        rs4 = ps4.executeQuery();
        while (rs4.next()) {
          EfinBudgetTransfertrxline trxline = OBDal.getInstance().get(
              EfinBudgetTransfertrxline.class, rs4.getString("efin_budget_transfertrxline_id"));
          trxline.setStatus("Success");
          // All correct cost budget decrease values updated
        }

        // Brings Funds Budget
        query = "select bud.efin_budget_id from efin_budget bud "
            + "join c_campaign typ on typ.c_campaign_id=bud.c_campaign_id"
            + " where  typ.em_efin_budgettype ='F'  and bud.c_year_id='"
            + objCloneRev.getYear().getId() + "' ";
        log.debug("CostBudgetQuery:" + query.toString());
        ps1 = conn.prepareStatement(query);
        rs1 = ps1.executeQuery();
        while (rs1.next()) {

          // Checking Cost Budget lines Decrease amount exceed from Funds Budget Current Budget
          // amount
          query1 = " select cost.trxlineid,cost.efin_budget_transfertrx_id,funds.current_budget from (select line.current_budget as  costcbug , line.funds_available, coalesce(ln.decrease,0) as costdecrease , ln.efin_budget_transfertrxline_id as trxlineid ,ln.efin_budget_transfertrx_id, line.efin_budgetlines_id ,line.ad_org_id,line.c_salesregion_id,line.c_campaign_id,line.c_elementvalue_id, "
              + " line.c_project_id,line.c_activity_id, line.user1_id,line.user2_id from efin_budget_transfertrxline ln join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
              + " join efin_budget bud on bud.efin_budget_id =line.efin_budget_id where bud.c_year_id='"
              + objCloneRev.getYear().getId() + "'" + "  )cost  "
              + "  join (select efin_budgetlines_id,ad_org_id,c_salesregion_id,user2_id,user1_id,c_activity_id,c_elementvalue_id,c_campaign_id,c_project_id,current_budget from efin_budgetlines where efin_budget_id='"
              + rs1.getString("efin_budget_id") + "' ) funds "
              + "  on (funds.ad_org_id||funds.c_salesregion_id||funds.c_elementvalue_id||funds.c_project_id||funds.c_activity_id|| funds.user1_id|| funds.user2_id) = (cost.ad_org_id||cost.c_salesregion_id||cost.c_elementvalue_id||cost.c_project_id||cost.c_activity_id|| cost.user1_id|| cost.user2_id) "
              + "  where 1=1 and (cost.costcbug-cost.costdecrease) < (funds.current_budget) and cost.costdecrease >0 and cost.efin_budget_transfertrx_id='"
              + objCloneRev.getId() + "'";
          log.debug(
              "Cost Budget Decrease amount is lesser than the funds budget currentamount Query:"
                  + query1.toString());
          ps2 = conn.prepareStatement(query1);
          rs2 = ps2.executeQuery();
          while (rs2.next()) {
            EfinBudgetTransfertrxline trxline = OBDal.getInstance()
                .get(EfinBudgetTransfertrxline.class, rs2.getString("trxlineid"));
            String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_CostFunds-Void");
            trxline.setStatus(status.replace("@", rs2.getString("current_budget")));
            errorFlag = true;
            errorMsg = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Failed-Void").replace("@",
                objCloneRev.getDocumentNo());

          }
        }

        // check decrease amount exceed the cost budget funds available
        query = " select line.efin_budgetlines_id ,line.funds_available as available ,ln.decrease as decrease,ln.efin_budget_transfertrxline_id from efin_budget_transfertrxline ln  join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
            + " where ln.efin_budget_transfertrx_id  ='" + objCloneRev.getId()
            + "' and ln.decrease >  line.funds_available  and ln.increase = 0  ";
        ps1 = conn.prepareStatement(query);
        log.debug("lines:" + ps1.toString());
        rs1 = ps1.executeQuery();
        while (rs1.next()) {
          EfinBudgetTransfertrxline trxline = OBDal.getInstance().get(
              EfinBudgetTransfertrxline.class, rs1.getString("efin_budget_transfertrxline_id"));
          String status = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Cost-Void");
          trxline.setStatus(status.replace("@", rs1.getString("available")));
          errorFlag = true;
          errorMsg = OBMessageUtils.messageBD("Efin_budget_Rev_Lines_Failed-Void").replace("@",
              objCloneRev.getDocumentNo());
        }

        // update all cost budget increase amount
        query3 = " select line.efin_budgetlines_id ,line.funds_available as available ,ln.decrease as decrease,ln.efin_budget_transfertrxline_id from efin_budget_transfertrxline ln  join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
            + " where ln.efin_budget_transfertrx_id  ='" + objCloneRev.getId()
            + "' and ln.decrease = 0  ";
        ps4 = conn.prepareStatement(query3);
        log.debug("lines:" + ps4.toString());
        rs4 = ps4.executeQuery();
        while (rs4.next()) {
          EfinBudgetTransfertrxline trxline = OBDal.getInstance().get(
              EfinBudgetTransfertrxline.class, rs4.getString("efin_budget_transfertrxline_id"));
          trxline.setStatus("Success");
          // All correct cost budget increase amount values updated
        }

      }

      if (errorFlag) {
        JSONObject errormsg = new JSONObject();
        errormsg.put("severity", "error");
        errormsg.put("text", errorMsg);
        jsonResponse.put("message", errormsg);
      }
      if (!errorFlag) {
        // update the header status
        appstatus = "SUB";

        // count = insertTransfer(conn, clientId, orgId, roleId, userId, efinBudgetRev);

        count = updateHeaderStatus(conn, clientId, orgId, roleId, userId, objCloneRev, appstatus,
            "");
        if (count == 1) {
          insertTransferSubmit(conn, clientId, orgId, roleId, userId, objCloneRev.getId());
        } else if (count == 2) {
          insertTransferApproval(conn, clientId, orgId, roleId, userId, objCloneRev.getId());
        }
        if (count > 0) {
          JSONObject errormsg = new JSONObject();
          errormsg.put("severity", "success");
          errormsg.put("text", OBMessageUtils.messageBD("Efin_budgetRevision_Void: Success")
              .replace("@documentNo@", objCloneRev.getDocumentNo()));
          jsonResponse.put("message", errormsg);
        }
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      log.error("exception :", e);
      JSONObject errormsg = new JSONObject();
      try {
        errormsg.put("severity", "error");
        errormsg.put("text", "Process Failed");
        jsonResponse.put("message", errormsg);
      } catch (JSONException e1) {
        OBDal.getInstance().rollbackAndClose();
        log.error("exception :", e1);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }

  /**
   * Get Document no for Corresponding Sequence
   * 
   * @param selected
   *          clienTid, SequenceName , Update Sequence(Boolean)
   * @return DocumentNo
   */
  public static String getSequenceNo(String clientId, String seqName, boolean update) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String sequenceNo = "";
    try {
      OBContext.setAdminMode(true);
      st = OBDal.getInstance().getConnection().prepareStatement("select ad_sequence_doc(?, ?, ?);");
      st.setString(1, seqName);
      st.setString(2, clientId);
      st.setString(3, (update == true ? "Y" : "N"));
      rs = st.executeQuery();
      if (rs.next())
        sequenceNo = rs.getString("ad_sequence_doc") == null ? "" : rs.getString("ad_sequence_doc");
    } catch (final Exception e) {
    } finally {
      OBContext.restorePreviousMode();
    }
    return sequenceNo;
  }

  /**
   * This method is used to update heade status
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param efinBudgetRev
   * @param appstatus
   * @param comments
   * @return
   */
  public static int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, EfinBudgetTransfertrx efinBudgetRev, String appstatus, String comments) {
    String transferId = null;
    int count = 0;
    try {
      OBContext.setAdminMode(true);

      EfinBudgetTransfertrx transfertrx = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          efinBudgetRev.getId());

      NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId, roleId,
          userId, Resource.BUDGET_REVISION_RULE, 0.00);
      EutNextRole nextRole = null;
      log.debug("nextrole:" + nextApproval);
      if (nextApproval != null && nextApproval.hasApproval()) {
        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        transfertrx.setUpdated(new java.util.Date());
        transfertrx.setUpdatedBy(OBContext.getOBContext().getUser());
        if ((transfertrx.getDocStatus().equals("RW") || transfertrx.getDocStatus().equals("DR"))
            && transfertrx.getAction().equals("CO")) {
          transfertrx.setRevoke(true);
        } else
          transfertrx.setRevoke(false);
        transfertrx.setDocStatus("WFA");
        transfertrx.setNextRole(nextRole);
        transfertrx.setAction("AP");
        log.debug("doc sts:" + transfertrx.getDocStatus() + "action:" + transfertrx.getAction());
        count = 1; // Waiting For Approval flow

      } else {
        if (transfertrx.getEfinBudgetRevVoid() != null) {
          EfinBudgetTransfertrx VoidReference = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
              transfertrx.getEfinBudgetRevVoid().getId());
          VoidReference.setDocStatus("VO");
          OBDal.getInstance().save(VoidReference);
        }
        transfertrx.setUpdated(new java.util.Date());
        transfertrx.setUpdatedBy(OBContext.getOBContext().getUser());
        if (transfertrx.getEfinBudgetRevVoid() != null) {
          transfertrx.setDocStatus("VO");
        } else {
          transfertrx.setDocStatus("CO");
        }
        transfertrx.setNextRole(null);
        transfertrx.setAction("PD");
        transfertrx.setRevoke(false);
        count = 2; // Final Approval Flow

        /*
         * for(EfinBudgetTransfertrxline efinBudgetTransfer :
         * efinBudgetRev.getEfinBudgetTransfertrxlineList()){ OBQuery<EfinBudgetTransfer> transfer =
         * OBDal.getInstance().createQuery(EfinBudgetTransfer.class,
         * "as e where e.efinBudgetTransfertrxline.id='"+efinBudgetTransfer.getId()+"' ");
         * for(EfinBudgetTransfer upTransfer : transfer.list()){ upTransfer.setStatus("CO");
         * OBDal.getInstance().save(upTransfer); }
         * 
         * }
         */

      }
      log.debug("approve:" + transfertrx.getTransferSource());
      log.debug("revoke:" + transfertrx.isRevoke());
      OBDal.getInstance().save(transfertrx);
      transferId = transfertrx.getId();
      if (!StringUtils.isEmpty(transferId)) {
        // insertHistory =
        // BudgetRevisionRework.insertBudgRevHistory(OBDal.getInstance().getConnection(), clientId,
        // orgId, roleId, userId, transferId, comments, appstatus, pendingapproval);
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      log.error("Exception in updateHeaderStatus in Budget Revision: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Insert the records in Manual Encumbrance if it is decrease amount on waiting for approval
   * status
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param efinBudgetRev
   * @return 1 if the process succeed
   */
  public static int insertTransferSubmit(Connection con, String clientId, String orgId,
      String roleId, String userId, String efinBudgetRev) {
    String query = null;
    PreparedStatement ps1 = null;
    ResultSet rs1 = null;
    con = OBDal.getInstance().getConnection();
    try {

      OBContext.setAdminMode(true);
      EfinBudgetTransfertrx efinBudget = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          efinBudgetRev);
      query = " select line.efin_budgetlines_id,ln.description,ln.decrease,ln.c_salesregion_id as dept,ln.efin_budget_transfertrxline_id,line.uniquecode from efin_budget_transfertrxline ln  "
          + " join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
          + " join efin_budget_transfertrx trx on trx.efin_budget_transfertrx_id= ln.efin_budget_transfertrx_id "
          + " where trx.efin_budget_transfertrx_id='" + efinBudget.getId() + "' and trx.c_year_id='"
          + efinBudget.getYear().getId() + "' and  ln.decrease > 0";
      ps1 = con.prepareStatement(query);
      log.debug("uniquecode:" + ps1.toString());
      rs1 = ps1.executeQuery();
      while (rs1.next()) {
        efinbudgetencum efinencum = OBProvider.getInstance().get(efinbudgetencum.class);
        efinencum.setClient(OBContext.getOBContext().getCurrentClient());
        efinencum.setOrganization(OBContext.getOBContext().getCurrentOrganization());
        efinencum.setActive(true);
        efinencum.setUpdatedBy(OBContext.getOBContext().getUser());
        efinencum.setCreationDate(new java.util.Date());
        efinencum.setCreatedBy(OBContext.getOBContext().getUser());
        efinencum.setUpdated(new java.util.Date());
        efinencum.setAmount(new BigDecimal(rs1.getInt("decrease")));
        efinencum.setTransactionDate(efinBudget.getTrxdate());
        efinencum.setAccountingDate(efinBudget.getAccountingDate());
        efinencum.setDescription(rs1.getString("description"));
        efinencum.setEfinBudgetTransfertrxline(OBDal.getInstance()
            .get(EfinBudgetTransfertrxline.class, rs1.getString("efin_budget_transfertrxline_id")));
        efinencum.setBudgetLines(
            OBDal.getInstance().get(EFINBudgetLines.class, rs1.getString("efin_budgetlines_id")));
        efinencum.setAppstatus("APP");
        efinencum.setDoctype("ABR");
        efinencum.setAutoencumrance(true);
        efinencum.setUniqueCode(rs1.getString("uniquecode"));

        // to add department.
        efinencum.setDept(OBDal.getInstance().get(SalesRegion.class, rs1.getString("dept")));
        log.debug("dept :" + rs1.getString("dept"));

        OBDal.getInstance().save(efinencum);
        OBDal.getInstance().flush();
        // all values are udpated in Encumbrance Table
      }
    } catch (Exception e) {
      log.error("Exception in BudgetRevision in insertTransferApproval: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

  /**
   * if Final approval Insert the records in BudgetRevision Transfer and delete the lines from
   * encumbrance
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param efinBudgetRev
   * @return 1 if the process succeed
   */

  public static int insertTransferApproval(Connection con, String clientId, String orgId,
      String roleId, String userId, String efinBudgetRev) {
    String query = "", deleteqry = "";
    PreparedStatement ps1 = null, ps = null;
    ResultSet rs1 = null;
    con = OBDal.getInstance().getConnection();
    try {
      OBContext.setAdminMode(true);
      EfinBudgetTransfertrx efinBudget = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
          efinBudgetRev);
      deleteqry = " delete from efin_budget_encum where efin_budget_encum_id in ( select efin_budget_encum_id from efin_budget_encum en "
          + " join efin_budget_transfertrxline ln on ln.efin_budget_transfertrxline_id=en.efin_budget_transfertrxline_id "
          + " join efin_budget_transfertrx trx on trx.efin_budget_transfertrx_id= ln.efin_budget_transfertrx_id  "
          + " where trx.efin_budget_transfertrx_id='" + efinBudget.getId() + "' and "
          + " trx.c_year_id='" + efinBudget.getYear().getId() + "') ";
      ps = con.prepareStatement(deleteqry);
      ps.executeUpdate();

      query = " select line.efin_budgetlines_id,ln.description,ln.decrease,ln.increase,ln.efin_budget_transfertrxline_id,line.uniquecode from efin_budget_transfertrxline ln  "
          + " join efin_budgetlines line on ln.unique_code = line.efin_budgetlines_id "
          + " join efin_budget_transfertrx trx on trx.efin_budget_transfertrx_id= ln.efin_budget_transfertrx_id "
          + " where trx.efin_budget_transfertrx_id='" + efinBudget.getId() + "' and trx.c_year_id='"
          + efinBudget.getYear().getId() + "' ";
      ps1 = con.prepareStatement(query);
      log.debug("approvalFlow:" + ps1.toString());
      rs1 = ps1.executeQuery();
      while (rs1.next()) {
        EfinBudgetTransfer transfer = OBProvider.getInstance().get(EfinBudgetTransfer.class);
        transfer.setClient(OBContext.getOBContext().getCurrentClient());
        transfer.setOrganization(OBDal.getInstance().get(Organization.class, orgId));
        transfer.setActive(true);
        transfer.setCreatedBy(OBContext.getOBContext().getUser());
        transfer.setCreationDate(new java.util.Date());
        transfer.setCreatedBy(OBContext.getOBContext().getUser());
        transfer.setUpdated(new java.util.Date());
        transfer.setEfinBudgetlines(
            OBDal.getInstance().get(EFINBudgetLines.class, rs1.getString("efin_budgetlines_id")));
        transfer.setAmount(new BigDecimal(rs1.getInt("increase")));
        transfer.setDECAmount(new BigDecimal(rs1.getInt("decrease")));
        transfer.setDescription(rs1.getString("description"));
        transfer.setEfinBudgetTransfertrx(efinBudget);
        transfer.setACTDate(efinBudget.getAccountingDate());
        transfer.setTrxdate(efinBudget.getTrxdate());
        transfer.setEfinBudgetTransfertrxline(OBDal.getInstance()
            .get(EfinBudgetTransfertrxline.class, rs1.getString("efin_budget_transfertrxline_id")));
        transfer.setTransfer(efinBudget.getDocType());
        transfer.setTrxdate(efinBudget.getTrxdate());
        transfer.setStatus("CO");
        OBDal.getInstance().save(transfer);
        // all values are udpated in budget transfer table

        OBDal.getInstance().flush();
      }

    } catch (Exception e) {
      log.error("Exception in insertTransferApproval in BudgetRevision: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }
}
