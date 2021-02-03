/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.FundsRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.jfree.util.Log;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINBudget;
import sa.elm.ob.finance.EFINBudgetLines;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author J.Divya
 */
public class FundsRequestActionDAO {
  private static final Logger LOG = LoggerFactory.getLogger(FundsRequestActionDAO.class);
  public static String errorMsgs = null;

  /**
   * check sum of to account increase and from account decrease amount is equal or not
   * 
   * @param req
   * @param conn
   * @return
   */
  public static Boolean chkIncAndDecAmtEqulorNot(EFINFundsReq req, Connection conn) {
    String strQuery = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    Boolean errorFlag = false;
    String reqLineIds = null;
    try {
      OBContext.setAdminMode();

      EfinBudgetControlParam controlparam = FundsReqMangementDAO
          .getControlParam(req.getClient().getId());
      strQuery = "  select fromaccount,ln.efin_fundsreqline_id as fromreq, decrease,"
          + " ln.req_type,budginq.funds_available, (select  sum(increase)||'-'||array_to_string(array_agg(reqln.efin_fundsreqline_id),',') "
          + "  from efin_fundsreqline reqln   where reqln.toaccount in(select c_validcombination_id"
          + "   from c_validcombination where (account_id||'-'||c_project_id||'-'||c_campaign_id||'-'||c_bpartner_id||'-'||c_activity_id||'-'|| user1_id ||'-'|| user2_id)"
          + "  in ( select account_id||'-'||c_project_id||'-'||c_campaign_id||'-'||c_bpartner_id||'-'||c_activity_id||'-'|| user1_id ||'-'|| user2_id "
          + "  from c_validcombination  where c_validcombination_id=ln.fromaccount) ) "
          + "   and reqln.efin_fundsreq_id=ln.efin_fundsreq_id and reqln.dist_type='DIST' ) as increase,ln.dist_type  from efin_fundsreqline ln "
          + "    left join efin_fundsreq req on req.efin_fundsreq_id= ln.efin_fundsreq_id  "
          + "    left join efin_budgetinquiry budginq on budginq.c_validcombination_id= ln.fromaccount"
          + "       left join efin_budgetint budinit on budinit.efin_budgetint_id= budginq.efin_budgetint_id"
          + "        where ln.efin_fundsreq_id=? and ln.fromaccount is not null   "
          + "         and budinit.efin_budgetint_id =? and budinit.ad_client_id =? order by ln.fromaccount asc";
      ps = conn.prepareStatement(strQuery);
      ps.setString(1, req.getId());
      ps.setString(2, req.getEfinBudgetint().getId());
      ps.setString(3, req.getClient().getId());

      LOG.debug("strQuery:" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (rs.getString("increase") != null) {
          reqLineIds = rs.getString("increase").split("-")[1].toString();
          BigDecimal increase = new BigDecimal(rs.getString("increase").split("-")[0].toString());

          // check sum of to account increase and from account decrease amount is equal or not if
          // its not equal then set status as failure
          if ((rs.getBigDecimal("decrease").compareTo(increase) > 0
              || (rs.getBigDecimal("decrease").compareTo(increase) < 0))
              && rs.getString("dist_type").equals("DIST")) {
            errorFlag = true;
            if (reqLineIds != null) {
              strQuery = " update efin_fundsreqline set status=? ,failure_reason=? where  efin_fundsreqline_id = ? ";
              ps = conn.prepareStatement(strQuery);
              ps.setString(1, "FL");
              ps.setString(2, OBMessageUtils.messageBD("Efin_FundReq_ReqGreThanFromAcct"));
              ps.setString(3, rs.getString("fromreq"));
              LOG.debug(
                  "strQueryupdate:" + OBMessageUtils.messageBD("Efin_FundReq_ReqGreThanFromAcct"));
              ps.executeUpdate();
            }
          }
          // else update the status as success
          else {
            strQuery = " update efin_fundsreqline set status=? ,failure_reason=? where  efin_fundsreqline_id = ? ";
            ps = conn.prepareStatement(strQuery);
            ps.setString(1, "SCS");
            ps.setString(2, null);
            ps.setString(3, rs.getString("fromreq"));
            ps.executeUpdate();
          }
        }
        // if to account not added only decrease the from account then also need to set the status
        // failure
        else {
          if (rs.getString("dist_type").equals("DIST")) {
            errorFlag = true;
            strQuery = " update efin_fundsreqline set status=? ,failure_reason=? where  efin_fundsreqline_id = ? ";
            ps = conn.prepareStatement(strQuery);
            ps.setString(1, "FL");
            ps.setString(2, OBMessageUtils.messageBD("Efin_FundReq_ReqGreThanFromAcct"));
            ps.setString(3, rs.getString("fromreq"));
            LOG.debug("strQueryupdate:" + ps.toString());
            ps.executeUpdate();
          }
        }

        // if decrease amount is greater than funds available then set the status as failure
        if (rs.getString("funds_available") != null && !req.isReserve()) {
          if (rs.getString("decrease") != null && rs.getString("increase") != null) {
            if (rs.getBigDecimal("decrease").compareTo(rs.getBigDecimal("funds_available")) > 0
                && ((!controlparam.isReqmorethanhqfunds()
                    && rs.getString("req_type").equals("DIST"))
                    || (rs.getString("req_type").equals("REL")))) {
              errorFlag = true;
              strQuery = " update efin_fundsreqline set status=? ,failure_reason=? where  efin_fundsreqline_id = ? ";
              ps = conn.prepareStatement(strQuery);
              ps.setString(1, "FL");
              ps.setString(2, OBMessageUtils.messageBD("Efin_FundReq_ReqGreThanFundsAvail"));
              ps.setString(3, rs.getString("fromreq"));
              LOG.debug("strQueryupdate:" + ps.toString());
              ps.executeUpdate();
            }
          }
        }

      }
      return errorFlag;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in chkFrmAcctEquToAcct " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return errorFlag;
  }

  /**
   * insert into Budget inquiry Lines
   * 
   * @param clientId
   * @param fundsReq
   * @param conn
   * @param vars
   * @param reserve
   * @param isresfinal
   * @return count
   */
  @SuppressWarnings("resource")
  public static int insertBudegtInquiryLines(String clientId, EFINFundsReq fundsReq,
      Connection conn, VariablesSecureApp vars, Boolean reserve, Boolean isresfinal) {
    // declare the variables
    String reqLineQry = null, reqFromLineQry = null, insertQry = null, getvalcomExistrs = null,
        chkLineExists = null, updatelinetoExists = null, updatelinefrmExists = null,
        updatedeptlinetoExists = null, updatedeptlinefrmExists = null, frominqId = null;
    Boolean insertflag = false;
    PreparedStatement ps = null, insertps = null;
    ResultSet reqFromRs = null, reqlrs = null, valrs = null, chklnrs = null;
    String temfromacctId = null;
    String fromAcc_Dept = null;
    String toAcc_Dept = null;
    boolean chkFromToAcc = false;
    String parent_id = null;
    try {
      Long Lineno = (long) 0;
      // Year year = FundsReqMangementDAO.getBudgetYear(fundsReq);

      // initialize the update and select qry
      chkLineExists = " select efin_budgetinquiry_id ,c_validcombination_id  from efin_budgetinquiry where efin_budgetint_id= ? and c_validcombination_id= ? ";
      getvalcomExistrs = " select c_validcombination_id from c_validcombination where c_validcombination_id = ? or c_validcombination_id= ? order by case when c_validcombination_id=? then 1 else 2  end asc ";
      updatelinetoExists = " update efin_budgetinquiry set disinc_amt=disinc_amt+ ?  where efin_budgetinquiry_id=? ";
      updatelinefrmExists = " update efin_budgetinquiry set disdec_amt=disdec_amt+? where efin_budgetinquiry_id=? ";

      updatedeptlinetoExists = " update efin_budgetinquiry set depinc_amt=depinc_amt+ ?  where efin_budgetinquiry_id=? ";
      updatedeptlinefrmExists = " update efin_budgetinquiry set depdec_amt=depdec_amt+? where efin_budgetinquiry_id=? ";

      insertQry = " INSERT INTO efin_budgetinquiry(efin_budgetinquiry_id, ad_client_id, ad_org_id, isactive, created,"
          + " createdby, updated, updatedby, uniquecode, "
          + " c_salesregion_id, user1_id, user2_id, c_activity_id, c_elementvalue_id,"
          + " c_campaign_id, current_budget, funds_available,  c_project_id, c_bpartner_id,c_validcombination_id, uniquecodename,depinc_amt,disinc_amt,"
          + " isdistribute,efin_budgetint_id,parent_id)"
          + " select get_uuid(), ? , ad_orgtrx_id, 'Y', now(),?, now(), ?, em_efin_uniquecode,"
          + " c_salesregion_id,user1_id, user2_id, c_activity_id, account_id, c_campaign_id,"
          + " ?,?, c_project_id,c_bpartner_id,c_validcombination_id, em_efin_uniquecodename , ?,?,?,?,? "
          + "           from c_validcombination where  c_validcombination_id = ? ";
      insertps = conn.prepareStatement(insertQry);

      // fetch FROM account detail from the funds request lines
      reqFromLineQry = "select distinct (com.account_id||com.c_project_id||com.c_campaign_id||com.c_bpartner_id||com.c_activity_id\n"
          + "||com.user1_id||com.user2_id) as com "
          + ",ln.fromaccount,ln.efin_fundsreqline_id from efin_fundsreqline ln"
          + "     left join c_validcombination com on com.c_validcombination_id= ln.fromaccount"
          + "      where ln.efin_fundsreq_id= ? and fromaccount is not null  order by fromaccount asc  ";
      ps = conn.prepareStatement(reqFromLineQry);
      ps.setString(1, fundsReq.getId());
      LOG.debug("strQueryupdate:" + ps.toString());
      reqFromRs = ps.executeQuery();
      while (reqFromRs.next()) {
        if (temfromacctId == null || (temfromacctId != null
            && !temfromacctId.equals(reqFromRs.getString("fromaccount")))) {
          if (fundsReq.getEfinBudgetint() != null) {
            temfromacctId = reqFromRs.getString("fromaccount");
            // based on FROM account detail fetching from account and to account details in funds
            // request lines
            reqLineQry = " select distinct ln.efin_fundsreqline_id,ln.increase,ln.decrease,ln.toaccount,ln.fromaccount,ln.req_type ,ln.dist_type, "
                + " case when ln.fromaccount is not null and ln.toaccount is not null then 'Y' else 'N' end as bothaccount "
                + " from  efin_fundsreqline ln  left join  c_validcombination com on com.c_validcombination_id= ln.toaccount or com.c_validcombination_id=ln.fromaccount "
                + "   where ((toaccount  in ( select c_validcombination_id from c_validcombination  "
                + "  where  (com.account_id||com.c_project_id||com.c_campaign_id||com.c_bpartner_id||com.c_activity_id\n"
                + "||com.user1_id||com.user2_id)   = ? ))  " + " or (fromaccount = ?)) "
                + "    and efin_fundsreq_id= ?  order by   ln.fromaccount asc ";
            ps = conn.prepareStatement(reqLineQry);
            ps.setString(1, reqFromRs.getString("com"));
            ps.setString(2, reqFromRs.getString("fromaccount"));
            ps.setString(3, fundsReq.getId());
            LOG.debug("strQueryupdate:" + ps.toString());
            reqlrs = ps.executeQuery();

            while (reqlrs.next()) {

              // if dist type is 'DIST' then both account will get the value 'N'
              if (reqlrs.getString("bothaccount").equals("N")
                  && reqlrs.getString("dist_type").equals("DIST")) {

                // to account & from account already present in Budget enquiry or not
                ps = conn.prepareStatement(chkLineExists);
                ps.setString(1, fundsReq.getEfinBudgetint().getId());
                if (reqlrs.getString("toaccount") != null)
                  ps.setString(2, reqlrs.getString("toaccount"));
                else
                  ps.setString(2, reqlrs.getString("fromaccount"));
                LOG.debug("strQueryupdate:" + ps.toString());
                chklnrs = ps.executeQuery();

                // if exists update the lines
                if (chklnrs.next()) {

                  // if to account present then just update the dist increase/ dept increase(Dept
                  // distribution) - based on funds type, only final approval(isresfinal=true)
                  if (reqlrs.getString("toaccount") != null && isresfinal) {
                    if (fundsReq.getOrgreqFundsType() != null
                        && fundsReq.getOrgreqFundsType().equals("DD"))
                      ps = conn.prepareStatement(updatedeptlinetoExists);
                    else
                      ps = conn.prepareStatement(updatelinetoExists);
                    ps.setBigDecimal(1, reqlrs.getBigDecimal("increase"));
                    ps.setString(2, chklnrs.getString("efin_budgetinquiry_id"));
                    ps.executeUpdate();
                  }
                  // if from account present then just update the dist decrease/ dept decrease(Dept
                  // distribution) - based on funds type, only reserve role approval(reserve=true)
                  else if (reqlrs.getString("fromaccount") != null) {
                    frominqId = chklnrs.getString("efin_budgetinquiry_id");
                    if (reserve) {
                      if (fundsReq.getOrgreqFundsType() != null
                          && fundsReq.getOrgreqFundsType().equals("DD"))
                        ps = conn.prepareStatement(updatedeptlinefrmExists);
                      else
                        ps = conn.prepareStatement(updatelinefrmExists);
                      ps.setBigDecimal(1, reqlrs.getBigDecimal("decrease"));
                      ps.setString(2, frominqId);
                      LOG.debug("strQueryupdate:" + ps.toString());
                      ps.executeUpdate();
                    }
                  }
                } else {
                  // if to account not present in budget enquiry then insert a record in budget
                  // enqiury only final approval (isresfinal=true)
                  if (reqlrs.getString("toaccount") != null
                      && reqlrs.getString("req_type").equals("DIST") && isresfinal) {
                    // Lineno = rs.getLong("line");
                    insertps.setString(1, fundsReq.getClient().getId());
                    insertps.setString(2, vars.getUser());
                    insertps.setString(3, vars.getUser());
                    insertps.setBigDecimal(4, reqlrs.getBigDecimal("increase"));
                    insertps.setBigDecimal(5, reqlrs.getBigDecimal("increase"));
                    if (fundsReq.getOrgreqFundsType() != null
                        && fundsReq.getOrgreqFundsType().equals("DD"))
                      insertps.setBigDecimal(6, reqlrs.getBigDecimal("increase"));
                    else
                      insertps.setBigDecimal(6, BigDecimal.ZERO);
                    if (fundsReq.getOrgreqFundsType() != null
                        && fundsReq.getOrgreqFundsType().equals("DD"))
                      insertps.setBigDecimal(7, BigDecimal.ZERO);
                    else
                      insertps.setBigDecimal(7, reqlrs.getBigDecimal("increase"));
                    insertps.setString(8, "Y");
                    insertps.setString(9, fundsReq.getEfinBudgetint().getId());
                    insertps.setString(10, frominqId);
                    insertps.setString(11, reqlrs.getString("toaccount"));
                    LOG.debug("insertps:" + insertps.toString());
                    insertflag = true;
                    Lineno = Lineno + 10;
                    insertps.addBatch();
                  }
                }
              }

              // if Dist Type is 'MANUAL'
              else {
                // chk line exists
                ps = conn.prepareStatement(getvalcomExistrs);
                ps.setString(1, reqlrs.getString("fromaccount"));
                ps.setString(2, reqlrs.getString("toaccount"));
                ps.setString(3, reqlrs.getString("fromaccount"));
                LOG.debug("strQueryupdate:" + ps.toString());
                valrs = ps.executeQuery();
                // if exists update the lines
                while (valrs.next()) {
                  ps = conn.prepareStatement(chkLineExists);
                  ps.setString(1, fundsReq.getEfinBudgetint().getId());
                  ps.setString(2, valrs.getString("c_validcombination_id"));
                  chklnrs = ps.executeQuery();
                  if (chklnrs.next()) {

                    // if from account present then just update the dist decrease/ dept
                    // decrease(Dept
                    // distribution) - based on funds type, only reserve role approval(reserve=true)

                    if (chklnrs.getString("c_validcombination_id") != null) {
                      if (reqlrs.getString("fromaccount") != null
                          && valrs.getString("c_validcombination_id")
                              .equals(reqlrs.getString("fromaccount"))) {
                        fromAcc_Dept = getDepartment(reqlrs.getString("fromaccount"));
                        toAcc_Dept = getDepartment(reqlrs.getString("toaccount"));
                        if (fromAcc_Dept.equals(toAcc_Dept)) {
                          OBQuery<EfinBudgetControlParam> budgetControlParam = OBDal.getInstance()
                              .createQuery(EfinBudgetControlParam.class,
                                  " as e where e.budgetcontrolCostcenter.id = :department and e.client.id = :client");
                          budgetControlParam.setNamedParameter("department", fromAcc_Dept);
                          budgetControlParam.setNamedParameter("client", vars.getClient());
                          if (budgetControlParam != null && budgetControlParam.list().size() > 0) {
                            ps = conn.prepareStatement(chkLineExists);
                            ps.setString(1, fundsReq.getEfinBudgetint().getId());
                            if (reqlrs.getString("fromaccount") != null)
                              ps.setString(2, reqlrs.getString("fromaccount"));
                            LOG.debug("strQueryupdate:" + ps.toString());
                            chklnrs = ps.executeQuery();
                            if (chklnrs.next()) {
                              EfinBudgetInquiry budgetInq = OBDal.getInstance().get(
                                  EfinBudgetInquiry.class,
                                  chklnrs.getString("efin_budgetinquiry_id"));
                              frominqId = chklnrs.getString("efin_budgetinquiry_id");
                              parent_id = budgetInq.getParent().getId();
                              chkFromToAcc = true;

                            }
                          }

                        } else {
                          frominqId = chklnrs.getString("efin_budgetinquiry_id");
                        }
                        if (reserve) {
                          if (fundsReq.getOrgreqFundsType() != null
                              && fundsReq.getOrgreqFundsType().equals("DD"))
                            ps = conn.prepareStatement(updatedeptlinefrmExists);
                          else
                            ps = conn.prepareStatement(updatelinefrmExists);
                          ps.setBigDecimal(1, reqlrs.getBigDecimal("decrease"));
                          ps.setString(2, frominqId);
                          LOG.debug("strQueryupdate:" + ps.toString());
                          ps.executeUpdate();
                        }
                      }
                      // if to account present then just update the dist increase/ dept
                      // increase(Dept
                      // distribution) - based on funds type, only reserve role
                      // approval(isresfinal=true)
                      else if (reqlrs.getString("toaccount") != null && chklnrs
                          .getString("c_validcombination_id").equals(reqlrs.getString("toaccount"))
                          && isresfinal) {
                        if (fundsReq.getOrgreqFundsType() != null
                            && fundsReq.getOrgreqFundsType().equals("DD"))
                          ps = conn.prepareStatement(updatedeptlinetoExists);
                        else
                          ps = conn.prepareStatement(updatelinetoExists);
                        ps.setBigDecimal(1, reqlrs.getBigDecimal("increase"));
                        ps.setString(2, chklnrs.getString("efin_budgetinquiry_id"));
                        LOG.debug("strQueryupdate:" + ps.toString());
                        ps.executeUpdate();
                      }
                    }
                  } else {
                    // if to account not present in budget enquiry then insert a record in budget
                    // enqiury only final approval (isresfinal=true)
                    if (reqlrs.getString("req_type").equals("DIST") && isresfinal) {
                      insertps.setString(1, fundsReq.getClient().getId());
                      insertps.setString(2, vars.getUser());
                      insertps.setString(3, vars.getUser());
                      // /insertps.setString(4, rs.getString("efin_budget_id"));
                      insertps.setLong(5, Lineno);
                      insertps.setBigDecimal(4, reqlrs.getBigDecimal("increase"));
                      insertps.setBigDecimal(5, reqlrs.getBigDecimal("increase"));
                      if (fundsReq.getOrgreqFundsType() != null
                          && fundsReq.getOrgreqFundsType().equals("DD"))
                        insertps.setBigDecimal(6, reqlrs.getBigDecimal("increase"));
                      else
                        insertps.setBigDecimal(6, BigDecimal.ZERO);
                      if (fundsReq.getOrgreqFundsType() != null
                          && fundsReq.getOrgreqFundsType().equals("DD"))
                        insertps.setBigDecimal(7, BigDecimal.ZERO);
                      else
                        insertps.setBigDecimal(7, reqlrs.getBigDecimal("increase"));
                      insertps.setString(8, "Y");
                      insertps.setString(9, fundsReq.getEfinBudgetint().getId());
                      if (chkFromToAcc)
                        insertps.setString(10, parent_id);
                      else
                        insertps.setString(10, frominqId);
                      insertps.setString(11, reqlrs.getString("toaccount"));
                      LOG.debug("insertps:" + insertps.toString());
                      insertflag = true;
                      insertps.addBatch();
                      Lineno = Lineno + 10;
                    }
                  }
                }
              }
            }
          }
        }
      }
      if (insertflag) {
        insertps.executeBatch();
      }
      return 1;
    } catch (Exception e) {
      LOG.error("Exception in insertBudegtInquiryLines " + e.getMessage());
      OBDal.getInstance().rollbackAndClose();
      return 2; // process failed.
    }
  }

  /**
   * update the funds request while reactivate
   * 
   * @param fundsReq
   */
  public static void updateFundsReq(EFINFundsReq fundsReq) {
    try {
      EFINFundsReq request = fundsReq;
      request.setDocumentStatus("CO");
      request.setAction("RE");
      OBDal.getInstance().save(request);
    } catch (Exception e) {
      LOG.error("Exception in updateFundsReq " + e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * direct distribute -BCU Auto Request ( occured only budget revision / adjustment , if lines are
   * marked with distribution yes)
   * 
   * @param conn
   * @param transactionType
   * @param headerId
   */
  public static void directDistribute(Connection conn, String transactionType, String headerId,
      VariablesSecureApp vars, String clientId, String roleId) {
    try {
      OBContext.setAdminMode();
      EfinBudgetTransfertrx budRev = null;
      BudgetAdjustment budAdj = null;
      EFINBudget budget = null;
      OBQuery<EfinBudgetTransfertrxline> revLineQry = null;
      OBQuery<BudgetAdjustmentLine> adjLineQry = null;
      OBQuery<EFINBudgetLines> budLineQry = null;
      EFINFundsReq fundsreqObj = null;
      // get budget control param
      EfinBudgetControlParam budgContrparam = null;
      if (transactionType.equals("BR")) {
        // get revision object
        budRev = OBDal.getInstance().get(EfinBudgetTransfertrx.class, headerId);
        budgContrparam = FundsReqMangementDAO.getControlParam(budRev.getClient().getId());
        // get Budget Revision Line details with distribute is yes
        revLineQry = OBDal.getInstance()
            .createQuery(EfinBudgetTransfertrxline.class, " as e where e.efinBudgetTransfertrx.id='"
                + headerId
                + "' and e.increase > 0  and e.distribute='Y' and e.distributeLineOrg is not null ");

      } else if (transactionType.equals("BUD")) {
        // get Budget object
        budget = OBDal.getInstance().get(EFINBudget.class, headerId);
        budgContrparam = FundsReqMangementDAO.getControlParam(budget.getClient().getId());
        // get Budget Line details with distribute is yes
        budLineQry = OBDal.getInstance().createQuery(EFINBudgetLines.class,
            " as e where e.efinBudget.id='" + headerId
                + "'  and e.amount > 0  and e.distribute='Y' and e.distributionLinkOrg is not null ");
      } else {
        // get Adjustment object
        budAdj = OBDal.getInstance().get(BudgetAdjustment.class, headerId);
        budgContrparam = FundsReqMangementDAO.getControlParam(budAdj.getClient().getId());
        // get Budget Revision Line details with distribute is yes
        adjLineQry = OBDal.getInstance().createQuery(BudgetAdjustmentLine.class,
            " as e where e.efinBudgetadj.id='" + headerId
                + "' and e.increase > 0  and e.distribute='Y'  and e.dislinkorg is not null ");
      }

      if (revLineQry != null && revLineQry.list().size() > 0) {
        // insert a funds request
        fundsreqObj = insertFundsReq(conn, budgContrparam, budRev, budAdj, null);

        // loop the revision lines
        for (EfinBudgetTransfertrxline ln : revLineQry.list()) {
          insertFundsReqLines(conn, budgContrparam, ln.getAccountingCombination(),
              ln.getDistributeLineOrg(), fundsreqObj, ln.getIncrease(), budRev.getEfinBudgetint());
        }
      } else if (budLineQry != null && budLineQry.list().size() > 0) {
        // insert a funds request
        fundsreqObj = insertFundsReq(conn, budgContrparam, null, null, budget);

        for (EFINBudgetLines ln : budLineQry.list()) {
          insertFundsReqLines(conn, budgContrparam, ln.getAccountingCombination(),
              ln.getDistributionLinkOrg(), fundsreqObj, ln.getAmount(), budget.getEfinBudgetint());
        }

      } else if (adjLineQry != null && adjLineQry.list().size() > 0) {
        // insert a funds request
        fundsreqObj = insertFundsReq(conn, budgContrparam, budRev, budAdj, null);

        for (BudgetAdjustmentLine ln : adjLineQry.list()) {
          insertFundsReqLines(conn, budgContrparam, ln.getAccountingCombination(),
              ln.getDislinkorg(), fundsreqObj, ln.getIncrease(), budAdj.getEfinBudgetint());
        }
      }
      if (fundsreqObj != null) {
        fundsreqObj.setDocumentStatus("CO");
        fundsreqObj.setAction("PD");
        OBDal.getInstance().save(fundsreqObj);
        OBDal.getInstance().flush();
        sendAlertForDirectDist(fundsreqObj, roleId, conn, clientId, vars);
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in directDistribute " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * insert a record in funds request (only for BCU Auto Request)
   * 
   * @param conn
   * @param budgContrparam
   * @param budRev
   * @param budAdj
   * @return
   */

  public static EFINFundsReq insertFundsReq(Connection conn, EfinBudgetControlParam budgContrparam,
      EfinBudgetTransfertrx budRev, BudgetAdjustment budAdj, EFINBudget budget) {
    EFINFundsReq req = null;
    try {
      OBContext.setAdminMode();
      req = OBProvider.getInstance().get(EFINFundsReq.class);
      req.setOrganization(budgContrparam.getOrganization());

      req.setTrxdate(budRev != null ? budRev.getTrxdate()
          : (budAdj != null ? budAdj.getTRXDate() : budget.getTransactionDate()));
      req.setAccountingDate(budRev != null ? budRev.getAccountingDate()
          : (budAdj != null ? budAdj.getAccountingDate() : budget.getTransactionDate()));
      req.setTransactionOrg(budgContrparam.getAgencyHqOrg());
      req.setTransactionPeriod(budRev != null ? budRev.getTransactionperiod()
          : (budAdj != null ? budAdj.getTransactionPeriod() : budget.getTransactionPeriod()));
      req.setSalesCampaign(budRev != null ? budRev.getSalesCampaign()
          : (budAdj != null ? budAdj.getBudgetType() : budget.getSalesCampaign()));
      req.setYear(budRev != null ? budRev.getYear()
          : (budAdj != null ? budAdj.getYear() : budget.getYear()));
      req.setEfinBudgetint(budRev != null ? budRev.getEfinBudgetint()
          : (budAdj != null ? budAdj.getEfinBudgetint() : budget.getEfinBudgetint()));
      req.setTransactionType("BCUAR");
      if (budAdj != null) {
        req.setEfinBudgetadj(budAdj);
      } else if (budget != null) {
        req.setBudget(budget);
      } else
        req.setEfinBudgetTransfertrx(budRev);
      req.setOrgreqFundsType("OD");

      OBDal.getInstance().save(req);
      return req;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in chkFrmAcctEquToAcct " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return req;
  }

  /**
   * insert a record into funds request lines(only for BCU Auto Request)
   * 
   * @param conn
   * @param budgContrparam
   * @param combinObj
   * @param distOrg
   * @param req
   * @param increase
   * @param budgInit
   */
  public static void insertFundsReqLines(Connection conn, EfinBudgetControlParam budgContrparam,
      AccountingCombination combinObj, Organization distOrg, EFINFundsReq req, BigDecimal increase,
      EfinBudgetIntialization budgInit) {
    final List<Object> parameters = new ArrayList<Object>();
    List<AccountingCombination> combList = new ArrayList<AccountingCombination>();
    Boolean toacct = false;
    EfinBudgetInquiry frominq = null;
    try {
      OBContext.setAdminMode();
      // getting accounting combination for to account based on from account with replacement of
      // cost center
      OBQuery<AccountingCombination> acctComQry = OBDal.getInstance().createQuery(
          AccountingCombination.class,
          " as e where e.trxOrganization.id=? and  e.salesRegion.id=?  and  e.account.id=? and  e.project.id=? and  e.salesCampaign.id=?"
              + " and  e.businessPartner.id=? and  e.activity.id=? and  e.stDimension.id=? and  e.ndDimension.id=? and e.client.id = ?   ");
      parameters.add(distOrg);
      parameters.add(budgContrparam.getBudgetcontrolCostcenter().getId());
      parameters.add(combinObj.getAccount().getId());
      parameters.add(combinObj.getProject().getId());
      parameters.add(combinObj.getSalesCampaign().getId());
      parameters.add(combinObj.getBusinessPartner().getId());
      parameters.add(combinObj.getActivity().getId());
      parameters.add(combinObj.getStDimension().getId());
      parameters.add(combinObj.getNdDimension().getId());
      parameters.add(combinObj.getClient().getId());
      acctComQry.setParameters(parameters);
      acctComQry.setMaxResult(1);
      combList = acctComQry.list();
      if (combList.size() > 0) {
        AccountingCombination com = combList.get(0);

        // insert a funds request lines
        EFINFundsReqLine reqline = OBProvider.getInstance().get(EFINFundsReqLine.class);
        reqline.setOrganization(req.getOrganization());
        reqline.setEfinFundsreq(req);
        reqline.setFromaccount(combinObj);
        reqline.setToaccount(com);
        reqline.setIncrease(increase);
        reqline.setDecrease(increase);
        reqline.setDistType("MAN");
        reqline.setDistribute(true);
        reqline.setFromuniquecodename(combinObj.getEfinUniquecodename());
        reqline.setTouniquecodename(com.getEfinUniquecodename());
        reqline.setREQType("DIST");
        reqline.setPercentage(BigDecimal.ZERO);
        OBDal.getInstance().save(reqline);
        // fetching to account combination budget inquiry obj
        OBQuery<EfinBudgetInquiry> inqQry = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
            " as e where e.efinBudgetint.id= '" + budgInit.getId() + "'"
                + "  and   ( e.accountingCombination.id= '" + com.getId()
                + "' or e.accountingCombination.id='" + combinObj.getId() + "')");

        // if exists then update the dis_increase amount in budget inquiry
        Log.debug("size:" + inqQry.list().size());
        if (inqQry.list().size() > 0) {
          for (EfinBudgetInquiry inq : inqQry.list()) {

            if (com.getId().equals(inq.getAccountingCombination().getId())) {
              toacct = true;
              inq.setDisincAmt(inq.getDisincAmt().add(increase));
            } else {
              frominq = inq;
              inq.setDisdecAmt(inq.getDisdecAmt().add(increase));
            }
            OBDal.getInstance().save(inq);
          }
        }
        // else then insert a record in budget inquiry
        if (!toacct) {
          EfinBudgetInquiry inquiry = OBProvider.getInstance().get(EfinBudgetInquiry.class);
          inquiry.setOrganization(distOrg);
          inquiry.setEfinBudgetint(budgInit);
          inquiry.setDistribute(true);
          inquiry.setAccountingCombination(com);
          inquiry.setUniqueCodeName(com.getEfinUniquecodename());
          inquiry.setUniqueCode(com.getEfinUniqueCode());
          inquiry.setDepartment(com.getSalesRegion());
          inquiry.setAccount(com.getAccount());
          inquiry.setSalesCampaign(com.getSalesCampaign());
          inquiry.setProject(com.getProject());
          inquiry.setBusinessPartner(com.getBusinessPartner());
          inquiry.setFunctionalClassfication(com.getActivity());
          inquiry.setFuture1(com.getStDimension());
          inquiry.setNdDimension(com.getNdDimension());
          inquiry.setDisincAmt(increase);
          inquiry.setParent(frominq);
          inquiry.setCurrentBudget(increase);
          inquiry.setFundsAvailable(increase);
          OBDal.getInstance().save(inquiry);
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in insertFundsReqLines " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * reactivate the budget inquiry changes while reactivate the funds request
   * 
   * @param conn
   * @param headerId
   * @param ischkReserveIsDoneorNot
   * @param isreactivate
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static boolean reactivateBudgetInqchanges(Connection conn, String headerId,
      Boolean ischkReserveIsDoneorNot, Boolean isreactivate) {
    EFINFundsReq fundsReq = null;
    Boolean validProcess = true;
    SQLQuery updatefrmQry = null, updatetoQry = null;
    try {

      OBContext.setAdminMode();
      if (headerId != null)
        fundsReq = OBDal.getInstance().get(EFINFundsReq.class, headerId);
      // check common validation before reactivate the funds request
      if (isreactivate) {
        validProcess = CommonValidations.checkValidations(headerId, "BudgetDistribution",
            OBContext.getOBContext().getCurrentClient().getId(), "RE", false);
        LOG.debug("validProcess:" + validProcess);
      }

      // if common validation return true the do further process
      if (validProcess) {
        String updatetoquery = " update efin_budgetinquiry set disinc_amt=disinc_amt- :disincrease  where efin_budgetinquiry_id=:headerId  ";
        String updatefrmquery = " update efin_budgetinquiry set disdec_amt=disdec_amt- :disdecrease  where efin_budgetinquiry_id=:headerId  ";

        String updatedepttoquery = " update efin_budgetinquiry set depinc_amt=depinc_amt- :disincrease  where efin_budgetinquiry_id=:headerId  ";
        String updatedeptfrmquery = " update efin_budgetinquiry set depdec_amt=depdec_amt- :disdecrease  where efin_budgetinquiry_id=:headerId  ";

        String query = "  select  inq.efin_budgetinquiry_id, inq.current_budget as fromcb,inq.funds_available as frmfundavail,"
            + "  case when ln.fromaccount =inq.c_validcombination_id then inq.disdec_amt  else 0 end as inqdisdecrease,"
            + " case when ln.toaccount =inq.c_validcombination_id then      inq.disinc_amt  else 0 end as inqdisincrease,"
            + " case when ln.toaccount =inq.c_validcombination_id  then  ln.increase else 0 end as fundsincrease,"
            + " case when ln.fromaccount =inq.c_validcombination_id  then ln.decrease else 0 end as funddecrease,"
            + " case when ln.fromaccount =inq.c_validcombination_id  then inq.c_validcombination_id end as fromaccount,"
            + " case when ln.toaccount =inq.c_validcombination_id  then inq.c_validcombination_id end as toaccount"
            + " from efin_budgetinquiry inq join efin_fundsreqline ln on ln.fromaccount=inq.c_validcombination_id or ln.toaccount=inq.c_validcombination_id"
            + "  where ln.efin_fundsreq_id  = :headerId  and inq.efin_budgetint_id= :initid  order  by case when  ln.fromaccount =inq.c_validcombination_id  then 1"
            + "  else 2 end asc  ";
        SQLQuery sqlQuery = OBDal.getInstance().getSession().createSQLQuery(query);
        sqlQuery.setParameter("headerId", headerId);
        sqlQuery.setParameter("initid", fundsReq.getEfinBudgetint().getId());
        LOG.debug("sqlQuery:" + sqlQuery.toString());
        List queryList = sqlQuery.list();
        if (sqlQuery != null && queryList.size() > 0) {
          for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
            Object[] row = (Object[]) iterator.next();
            if (row[7] != null && (ischkReserveIsDoneorNot || isreactivate)) {

              // if funds type is dept distribution then revert from account the dept decrease and
              // dist decrease
              // changes
              if (fundsReq.getOrgreqFundsType() != null
                  && fundsReq.getOrgreqFundsType().equals("DD"))
                updatefrmQry = OBDal.getInstance().getSession().createSQLQuery(updatedeptfrmquery);
              else
                updatefrmQry = OBDal.getInstance().getSession().createSQLQuery(updatefrmquery);
              updatefrmQry.setParameter("disdecrease", ((BigDecimal) row[6]));
              updatefrmQry.setParameter("headerId", row[0]);
              LOG.debug("updatefrmQry:" + updatefrmQry.toString());
              updatefrmQry.executeUpdate();
            }
            // if funds type is dept distribution then revert to account the dept increase and dist
            // increase
            // changes
            else if (!ischkReserveIsDoneorNot || isreactivate) {
              if (fundsReq.getOrgreqFundsType() != null
                  && fundsReq.getOrgreqFundsType().equals("DD"))
                updatetoQry = OBDal.getInstance().getSession().createSQLQuery(updatedepttoquery);
              else
                updatetoQry = OBDal.getInstance().getSession().createSQLQuery(updatetoquery);
              updatetoQry.setParameter("disincrease", ((BigDecimal) row[5]));
              updatetoQry.setParameter("headerId", row[0]);
              LOG.debug("updatetoQry:" + updatetoQry.toString());
              updatetoQry.executeUpdate();
            }
          }
          // update funds request header status;
          if (headerId != null) {
            if (!fundsReq.getTransactionType().equals("BCUAR"))
              updateFundsReqRE(fundsReq);
            else
              deleteFundsReq(fundsReq);
          }
        }
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in reactivateBudgetInqchanges " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  /**
   * update the funds request header as draft
   * 
   * @param fundsReq
   */
  public static void updateFundsReqRE(EFINFundsReq fundsReq) {
    try {
      OBContext.setAdminMode();
      fundsReq.setDocumentStatus("DR");
      fundsReq.setAction("CO");
      fundsReq.setReserve(false);
      OBDal.getInstance().save(fundsReq);
    } catch (Exception e) {
      LOG.error("Exception in updateFundsReqRE " + e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * deelete the funds requet only if BCU Auto Requesst
   * 
   * @param fundsReq
   */
  public static void deleteFundsReq(EFINFundsReq fundsReq) {
    List<EFINFundsReqLine> reqline = new ArrayList<EFINFundsReqLine>();

    try {
      OBContext.setAdminMode();
      updateFundsReqRE(fundsReq);
      reqline = fundsReq.getEFINFundsReqLineList();
      if (reqline.size() > 0) {
        for (EFINFundsReqLine reqln : reqline) {
          OBDal.getInstance().remove(reqln);
        }
      }
      OBDal.getInstance().remove(fundsReq);

    } catch (Exception e) {
      LOG.error("Exception in deleteFundsReq " + e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check if any lines in budget revision / budget adjustment distribute check box is checked, if
   * checked then return true else false to do the direct distribute
   * 
   * @param adj
   * @param trx
   * @return
   */
  public static boolean chkdistisdoneornot(BudgetAdjustment adj, EfinBudgetTransfertrx trx,
      EFINBudget bud) {
    Boolean ispresent = false;
    try {
      OBContext.setAdminMode();
      // Budget Adjustment
      if (adj != null) {
        for (BudgetAdjustmentLine ln : adj.getEfinBudgetAdjlineList()) {
          if (ln.getDislinkorg() != null && ln.isDistribute()) {
            ispresent = true;
            break;
          }
        }
        return ispresent;
      }
      // Budget
      else if (bud != null) {
        for (EFINBudgetLines ln : bud.getEFINBudgetLinesList()) {
          if (ln.getDistributionLinkOrg() != null && ln.isDistribute()) {
            ispresent = true;
            break;
          }
        }
        return ispresent;

      }
      // Budget Revision
      else {
        for (EfinBudgetTransfertrxline ln : trx.getEfinBudgetTransfertrxlineList()) {
          if (ln.getDistributeLineOrg() != null && ln.isDistribute()) {
            ispresent = true;
            break;
          }
        }
        return ispresent;
      }
    } catch (Exception e) {
      LOG.error("Exception in chkdistisdoneornot " + e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }

    finally {
      OBContext.restorePreviousMode();
    }

    return false;
  }

  /**
   * get funds request header id while reactive the budget revision and budget adjustment
   * 
   * @param adj
   * @param trx
   * @return
   */
  public static String getFundsReqId(BudgetAdjustment adj, EfinBudgetTransfertrx trx,
      EFINBudget bud) {
    OBQuery<EFINFundsReq> reqobj = null;
    String fundsreqId = null;
    try {
      OBContext.setAdminMode();
      // Budget Adjustement
      if (adj != null) {
        reqobj = OBDal.getInstance().createQuery(EFINFundsReq.class,
            " as e where e.efinBudgetadj.id='" + adj.getId() + "'");
        reqobj.setMaxResult(1);
        if (reqobj.list().size() > 0) {
          fundsreqId = reqobj.list().get(0).getId();
          return fundsreqId;
        }
      }
      // Budget
      else if (bud != null) {
        reqobj = OBDal.getInstance().createQuery(EFINFundsReq.class,
            " as e where e.budget.id='" + bud.getId() + "'");
        reqobj.setMaxResult(1);
        if (reqobj.list().size() > 0) {
          fundsreqId = reqobj.list().get(0).getId();
          return fundsreqId;
        }

      }
      // Budget Revision
      else {
        reqobj = OBDal.getInstance().createQuery(EFINFundsReq.class,
            " as e where e.efinBudgetTransfertrx.id='" + trx.getId() + "'");
        reqobj.setMaxResult(1);
        if (reqobj.list().size() > 0) {
          fundsreqId = reqobj.list().get(0).getId();
          return fundsreqId;
        }
      }
    } catch (Exception e) {
      LOG.error("Exception in getFundsReqId " + e.getMessage());
      OBDal.getInstance().rollbackAndClose();
    }

    finally {
      OBContext.restorePreviousMode();
    }
    return fundsreqId;
  }

  /**
   * update the next role who is next approver and update the header status of funds request based
   * on approval flow and sending the alert
   * 
   * @param con
   * @param clientId
   * @param orgId
   * @param roleId
   * @param userId
   * @param fundsreq
   * @param appstatus
   * @param comments
   * @param currentDate
   * @param vars
   * @param docType
   * @param isDist
   * @param currentRoleId
   * @return
   */
  @SuppressWarnings({ "unused", "unchecked" })
  public static JSONObject updateNextRole(Connection con, String clientId, String orgId,
      String roleId, String userId, EFINFundsReq fundsreq, String appstatus, String comments,
      Date currentDate, VariablesSecureApp vars, String docType, Boolean isDist,
      String currentRoleId, boolean isDummyRoleCdn) {
    String fundsreqId = null, pendingapproval = null, reserveRoleId = "";
    int count = 0;
    Boolean isDirectApproval = false, reserve = false, isresfinal = false, orgUserManager = false,
        orgUserBudgManager = false, removerecFlag = false, issubmit = false;
    String Description = "", orgBudgRoleId = null;
    String alertRuleId = "", alertWindow = AlertWindow.BudgetDistribution, orgBCURoleId = null,
        dummyRole = null;
    JSONObject result = new JSONObject();

    List<UserRoles> orgbudgManagrole = new ArrayList<UserRoles>();
    ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
    JSONObject forwardJsonObj = new JSONObject();
    JSONObject fromUserandRoleJson = new JSONObject();
    try {
      OBContext.setAdminMode(true);

      NextRoleByRuleVO nextApproval = null;
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      BigDecimal requsitionamt = BigDecimal.ZERO;
      HashMap<String, String> role = null;
      String delegatedFromRole = null, delegatedToRole = null, qu_next_role_id = "";
      fundsreqId = fundsreq.getId();
      Organization org = OBDal.getInstance().get(Organization.class, orgId);
      // chk role is direct approver or not
      isDirectApproval = isDirectApproval(fundsreq.getId(), currentRoleId);
      Boolean allowDelegation = false, isDummyRole = false;
      String fromUser = userId;
      String fromRole = roleId;
      String Lang = vars.getLanguage();
      User objUser = OBDal.getInstance().get(User.class, vars.getUser());
      LOG.debug("chkDirectApproval" + isDirectApproval);
      LOG.debug("nxt role>" + fundsreq.getNextRole());

      // find the submitted role org/branch details
      Role submittedRoleObj = null;
      String submittedRoleOrgId = null;
      if (fundsreq.getNextRole() != null) {
        if (fundsreq.getEfinSubmittedRole() != null
            && fundsreq.getEfinSubmittedRole().getEutReg() != null) {
          submittedRoleOrgId = fundsreq.getEfinSubmittedRole().getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      } else if (fundsreq.getNextRole() == null) {
        submittedRoleObj = OBContext.getOBContext().getRole();
        if (submittedRoleObj != null && submittedRoleObj.getEutReg() != null) {
          submittedRoleOrgId = submittedRoleObj.getEutReg().getId();
        } else {
          submittedRoleOrgId = orgId;
        }
      }
      // get the dummy role before insert into next lines for check the backward delegation
      if ((fundsreq.getNextRole() != null)) {
        dummyRole = roleId;
      }
      if (fundsreq.getNextRole() != null) {
        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(fundsreq.getNextRole(),
            userId, roleId, clientId, submittedRoleOrgId, docType, isDummyRoleCdn,
            isDirectApproval);
        if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
          if (fromUserandRoleJson.has("fromUser"))
            fromUser = fromUserandRoleJson.getString("fromUser");
          if (fromUserandRoleJson.has("fromRole"))
            fromRole = fromUserandRoleJson.getString("fromRole");
          if (fromUserandRoleJson.has("isDirectApproval"))
            isDirectApproval = fromUserandRoleJson.getBoolean("isDirectApproval");
        }

      } else {
        fromUser = userId;
        fromRole = roleId;
      }

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      // if frst time requester submit chk who is the next approver for budget distribution
      if ((fundsreq.getNextRole() == null)) {
        reserveRoleId = roleId;
        nextApproval = NextRoleByRule.getBudgetDistNextRole(OBDal.getInstance().getConnection(),
            clientId, submittedRoleOrgId, fromRole, fromUser, docType, 0);
        issubmit = true;
      } else {

        // after second approval chk next approver
        if (isDirectApproval) {
          reserveRoleId = roleId;
          nextApproval = NextRoleByRule.getBudgetDistNextRole(OBDal.getInstance().getConnection(),
              clientId, submittedRoleOrgId, fromRole, fromUser, docType, 0);

          // if next approver there then check delegation of next approval and chk backward
          // delgation of current user
          if (nextApproval != null && nextApproval.hasApproval()) {

            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id='" + objNextRoleLine.getRole().getId() + "'");
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    userRole.list().get(0).getUserContact().getId(), docType, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                // chk backward delegation
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                    delegatedFromRole, delegatedToRole, fromUser, docType, requsitionamt);
                if (isBackwardDelegation)
                  break;
              }
            }
          }

          LOG.debug("isBackwardDelegation" + isBackwardDelegation);
          // if back ward delegation is true then check next approver
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getBudgetDistNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, delegatedFromRole, fromUser, docType, 0);
            reserveRoleId = delegatedFromRole;
          }
        }
        // if no direct approver then chk delegated role
        else {
          // if the next role is dummy role and it is delegated then set the dummy role as reserve
          // role
          if (fundsreq.getNextRole() != null) {
            DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
            allowDelegation = delagationDao.checkDelegation(currentDate, vars.getRole(), docType);
            isDummyRole = fundsreq.getNextRole().getEutNextRoleLineList().get(0)
                .getDummyRole() == null ? false : true;
          }
          if (allowDelegation && isDummyRole) {
            nextApproval = NextRoleByRule.getBudgetDistNextRole(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, fromRole, fromUser, docType, 0);
            reserveRoleId = fromRole;

          } else {

            role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
                clientId, submittedRoleOrgId, fromUser, docType, qu_next_role_id);
            delegatedFromRole = role.get("FromUserRoleId");
            delegatedToRole = role.get("ToUserRoleId");

            if (delegatedFromRole != null && delegatedToRole != null)
              nextApproval = NextRoleByRule.getBudgetDistNextRole(
                  OBDal.getInstance().getConnection(), clientId, submittedRoleOrgId,
                  delegatedFromRole, fromUser, docType, 0);
            reserveRoleId = delegatedFromRole;
          }
        }
      }
      // If no org manager / org budget manager is not associated for particular transaction org
      // then throwing error
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("EFIN_OrgBudMangisNotAssociated")) {
        errorMsgs = OBMessageUtils.messageBD("EFIN_OrgBudMangisNotAssociated");
        errorMsgs = errorMsgs.replace("%", nextApproval.getOrgName());
        result.put("count", "-1");
        result.put("errorMsgs", errorMsgs);
        return result;
      }
      // If no role is define for next approver then throwing error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NoRoleDefineForUser")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getUserName());
        result.put("count", "-2");
        result.put("errorMsgs", errorMsgs);
        return result;
      } else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        result.put("count", "-3");
        result.put("errorMsgs", errorMsgs);
        return result;
      }

      // if next approver then insert alert for next approver and change the status as waiting for
      // approval
      else if (nextApproval != null && nextApproval.hasApproval()) {
        ArrayList<String> includeRecipient = new ArrayList<String>();

        // set isresfinal is not final approver
        isresfinal = false;

        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(fundsreq.getNextRole(), docType);

        forwardDao.getAlertForForwardedUser(fundsreq.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, fundsreq.getDocumentNo(), Lang, vars.getRole(),
            fundsreq.getEUTForward(), docType, alertReceiversMap);
        // update the funds request status
        if (fundsreq.getDocumentStatus().equals("DR")
            || fundsreq.getDocumentStatus().equals("RW")) {
          fundsreq.setRevoke(true);
        } else
          fundsreq.setRevoke(false);
        fundsreq.setUpdated(new java.util.Date());
        fundsreq.setUpdatedBy(OBContext.getOBContext().getUser());
        fundsreq.setDocumentStatus("WFA");
        fundsreq.setNextRole(nextRole);
        fundsreq.setAction("AP");

        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // // delete alert for approval alerts
          // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
          // "as e where e.referenceSearchKey='" + fundsreq.getId() + "' and e.alertStatus='NEW'");
          // if (alertQuery.list().size() > 0) {
          // for (Alert objAlert : alertQuery.list()) {
          // objAlert.setAlertStatus("SOLVED");
          // }
          // }

          // set the description
          Description = sa.elm.ob.finance.properties.Resource
              .getProperty("finance.fundsreq.waiting.for.approval", vars.getLanguage());

          if (pendingapproval == null)
            pendingapproval = nextApproval.getStatus();

          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            AlertUtility.alertInsertionRole(fundsreq.getId(), fundsreq.getDocumentNo(),
                objNextRoleLine.getRole().getId(),
                (objNextRoleLine.getUserContact() == null ? ""
                    : objNextRoleLine.getUserContact().getId()),
                fundsreq.getClient().getId(), Description, "NEW", alertWindow,
                "finance.fundsreq.waiting.for.approval", Constants.GENERIC_TEMPLATE);
            // get user name for delegated user to insert on approval history.
            OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
                EutDocappDelegateln.class,
                " as e left join e.eUTDocappDelegate as hd where hd.role.id ='"
                    + objNextRoleLine.getRole().getId() + "' and hd.fromDate <='" + currentDate
                    + "' and hd.date >='" + currentDate + "' and e.documentType='" + docType + "'");
            if (delegationln != null && delegationln.list().size() > 0) {
              AlertUtility.alertInsertionRole(fundsreq.getId(), fundsreq.getDocumentNo(),
                  delegationln.list().get(0).getRole().getId(),
                  delegationln.list().get(0).getUserContact().getId(), fundsreq.getClient().getId(),
                  Description, "NEW", alertWindow, "finance.fundsreq.waiting.for.approval",
                  Constants.GENERIC_TEMPLATE);
              LOG.debug("del role>" + delegationln.list().get(0).getRole().getId());
              includeRecipient.add(delegationln.list().get(0).getRole().getId());
              if (pendingapproval != null)
                pendingapproval += "/" + delegationln.list().get(0).getUserContact().getName();
              else
                pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                    delegationln.list().get(0).getUserContact().getName());
              includeRecipient.add(delegationln.list().get(0).getRole().getId());
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());

          }
        }
        // existing Recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }

        if (issubmit)
          result.put("count", "2"); // submit msg
        else
          result.put("count", "1"); // waiting for approval approved msg

      }
      // if final approver then set the status as approved
      else {
        ArrayList<String> includeRecipient = new ArrayList<String>();
        ArrayList<String> includeRecipient1 = new ArrayList<String>();
        isresfinal = true;

        fundsreq.setUpdated(new java.util.Date());
        fundsreq.setUpdatedBy(OBContext.getOBContext().getUser());
        fundsreq.setDocumentStatus("CO");
        fundsreq.setNextRole(null);
        fundsreq.setAction("RE");
        if (reserve)
          fundsreq.setReserve(true);

        Role objCreatedRole = null;

        if (fundsreq.getCreatedBy().getADUserRolesList().size() > 0) {
          if (fundsreq.getRole() != null)
            objCreatedRole = fundsreq.getRole();
          else
            objCreatedRole = fundsreq.getCreatedBy().getADUserRolesList().get(0).getRole();
        }
        // // delete alert for approval alerts
        // OBQuery<Alert> alertQuery = OBDal.getInstance().createQuery(Alert.class,
        // "as e where e.referenceSearchKey='" + fundsreq.getId() + "' and e.alertStatus='NEW'");
        // if (alertQuery.list().size() > 0) {
        // for (Alert objAlert : alertQuery.list()) {
        // objAlert.setAlertStatus("SOLVED");
        // }
        // }
        // get alert recipient
        OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
            .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
        // check and insert recipient
        if (receipientQuery.list().size() > 0) {
          for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(fundsreq.getNextRole(), docType);
        forwardDao.getAlertForForwardedUser(fundsreq.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE, fundsreq.getDocumentNo(), Lang, vars.getRole(),
            fundsreq.getEUTForward(), docType, alertReceiversMap);
        if (includeRecipient != null)
          includeRecipient.add(objCreatedRole.getId());

        // alert on ORG Budget Manager
        result = alertonOrgManager(fundsreqId, roleId, con, clientId);
        if (result != null) {
          JSONObject json1 = null;
          JSONArray jsonArray = result.getJSONArray("list");
          LOG.debug("jsonArray:" + jsonArray);
          for (int i = 0; i < jsonArray.length(); i++) {
            json1 = jsonArray.getJSONObject(i);
            includeRecipient.add(json1.getString("roleId"));
          }
        }

        includeRecipient.add(objCreatedRole.getId());

        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }

        // set alert for requester
        Description = sa.elm.ob.finance.properties.Resource.getProperty("finance.fundsreq.approved",
            vars.getLanguage());
        AlertUtility.alertInsertionRole(fundsreq.getId(), fundsreq.getDocumentNo(),
            fundsreq.getRole().getId(), fundsreq.getCreatedBy().getId(),
            fundsreq.getClient().getId(), Description, "NEW", alertWindow,
            "finance.fundsreq.approved", Constants.GENERIC_TEMPLATE);

        // alert on ORG Budget Manager
        if (result != null) {
          JSONObject json1 = null;
          String alertKey = "";
          JSONArray jsonArray = result.getJSONArray("list");
          for (int i = 0; i < jsonArray.length(); i++) {
            json1 = jsonArray.getJSONObject(i);
            LOG.debug("type:" + json1.getString("type").equals("DIST"));

            if (json1.getString("type").equals("REC")) {
              Description = sa.elm.ob.finance.properties.Resource
                  .getProperty("finance.fundsreq.received", vars.getLanguage())
                  .replace("%", fundsreq.getDocumentNo());
              alertKey = "finance.fundsreq.received";
            }

            else {
              Description = sa.elm.ob.finance.properties.Resource
                  .getProperty("finance.fundsreq.released", vars.getLanguage())
                  .replace("%", fundsreq.getDocumentNo());
              alertKey = "finance.fundsreq.released";
            }

            AlertUtility.alertInsertionRole(fundsreq.getId(), fundsreq.getDocumentNo(),
                json1.getString("roleId"), json1.getString("userId"), fundsreq.getClient().getId(),
                Description, "NEW", alertWindow, alertKey, Constants.GENERIC_TEMPLATE);
          }
        }

        result.put("count", "1"); // Final Approval Flow
      }

      // check current role exists in document rule ,if it is not there then delete Delete it
      // why ??? current user only already approved
      String checkQuery = "as a join a.nextRole r join r.eutNextRoleLineList l where l.role.id = '"
          + vars.getRole() + "' and a.documentStatus ='WFA'";

      OBQuery<EFINFundsReq> checkRecipientQry = OBDal.getInstance().createQuery(EFINFundsReq.class,
          checkQuery);

      LOG.debug("listfinal:" + checkRecipientQry.list().size());
      if (checkRecipientQry.list().size() == 0) {

        // chk if budget manager is final approver then dont allow to remove the budget manager role
        // in alert receipients

        // get org budget manager for particular record transaction org
        orgbudgManagrole = UtilityDAO.getOrgMangOrBudgManagerUserRole(clientId, false, true,
            fundsreq.getTransactionOrg().getId());
        if (orgbudgManagrole.size() > 0) {
          for (UserRoles rol : orgbudgManagrole) {
            if (rol.getRole().getId().equals(vars.getRole()))
              removerecFlag = true;
          }
        }

        if (!removerecFlag) {
          OBQuery<AlertRecipient> currentRoleQuery = OBDal.getInstance()
              .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId
                  + "' and e.role.id='" + vars.getRole() + "'");
          if (currentRoleQuery.list().size() > 0) {
            for (AlertRecipient delObject : currentRoleQuery.list()) {
              OBDal.getInstance().remove(delObject);
            }
          }
        }
      }

      OBDal.getInstance().save(fundsreq);
      fundsreqId = fundsreq.getId();
      if (!StringUtils.isEmpty(fundsreqId)) {
        JSONObject historyData = new JSONObject();

        // insert approval history
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", orgId);
        historyData.put("RoleId", currentRoleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", fundsreqId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.Budget_Distribution_History);
        historyData.put("HeaderColumn", ApprovalTables.Budget_Distribution_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Budget_Distribution_DOCACTION_COLUMN);

        Utility.InsertApprovalHistory(historyData);

      }
      OBDal.getInstance().flush();

      // checking role is reserver , if reserver then doing funds allocation
      reserve = UtilityDAO.getReserveFundsRole(docType, fromRole,
          fundsreq.getOrganization().getId(), fundsreq.getId(), BigDecimal.ZERO);
      LOG.debug("reserve:" + reserve);

      // insert into budget inquiry lines
      count = FundsRequestActionDAO.insertBudegtInquiryLines(clientId, fundsreq, con, vars, reserve,
          isresfinal);
      if (reserve)
        fundsreq.setReserve(true);
      LOG.debug("isReserve:" + fundsreq.isReserve());
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), docType);
      // after approved by forwarded user removing the forward and rmi id
      if (fundsreq.getEUTForward() != null) {
        forwardDao.setForwardStatusAsDraft(fundsreq.getEUTForward());
        fundsreq.setEUTForward(null);
      }
      if (fundsreq.getEUTReqmoreinfo() != null) {
        forwardDao.setForwardStatusAsDraft(fundsreq.getEUTReqmoreinfo());
        fundsreq.setEUTReqmoreinfo(null);
        fundsreq.setRequestMoreInformation("N");
      }
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {
      Log.error("Exception in updateNextRole in Funds request Action Dao: ", e);
      OBDal.getInstance().rollbackAndClose();
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /**
   * check current approver is direct approver or not
   * 
   * @param fundsreqId
   * @param roleId
   * @return
   */
  @SuppressWarnings("unused")
  private static boolean isDirectApproval(String fundsreqId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(*) from efin_fundsreq req join eut_next_role rl on "
          + "req.eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and req.efin_fundsreq_id = ? and li.ad_role_id =?";

      if (query != null) {
        ps = con.prepareStatement(query);
        ps.setString(1, fundsreqId);
        ps.setString(2, roleId);

        rs = ps.executeQuery();

        if (rs.next()) {
          if (rs.getInt("count") > 0)
            return true;
          else
            return false;
        } else
          return false;
      } else
        return false;
    } catch (Exception e) {
      LOG.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {

      }
    }
  }

  /**
   * get org manager for transaction org and org manager role also to send the alert .. if from
   * account then alert will send like release the funds , to account means alert will be receive
   * the funds
   * 
   * @param fundsreqId
   * @param roleId
   * @param con
   * @param clientId
   * @return
   */
  @SuppressWarnings({ "unchecked" })
  public static JSONObject alertonOrgManager(String fundsreqId, String roleId, Connection con,
      String clientId) {
    PreparedStatement ps = null;
    ResultSet rs = null;
    String sqlQuery = null;
    JSONObject result = new JSONObject(), json = null;
    String tempfromOrg = null, temptoOrg = null;
    List<UserRoles> role = new ArrayList<UserRoles>();
    JSONArray array = new JSONArray();
    try {

      sqlQuery = "" + " select  req_type,com.c_validcombination_id,"
          + " case when ln.fromaccount =com.c_validcombination_id  then  ln.fromaccount end as fromaccount  ,"
          + " case when ln.toaccount =com.c_validcombination_id  then  ln.toaccount end as toaccount  ,"
          + " case when ln.fromaccount =com.c_validcombination_id  then  com.ad_org_id end as fromorg,"
          + " case when ln.toaccount =com.c_validcombination_id  then  com.ad_org_id end as toorg,"
          + " case when ln.fromaccount =com.c_validcombination_id  then  com.c_salesregion_id end as fromdept,"
          + " case when ln.toaccount =com.c_validcombination_id  then  com.c_salesregion_id end as todept"
          + " ,param.budgetcontrol_costcenter as costcenter,param.hq_budgetcontrolunit as bcu"
          + " from efin_fundsreqline ln"
          + " left join c_validcombination com on com.c_validcombination_id=ln.fromaccount  or com.c_validcombination_id=ln.toaccount "
          + " left join efin_budget_ctrl_param param on param.ad_client_id=ln.ad_client_id  "
          + "  where efin_fundsreq_id  = ?   order  by "
          + "  case when  ln.fromaccount =com.c_validcombination_id  then 1   else 2 end asc ";

      ps = con.prepareStatement(sqlQuery);
      ps.setString(1, fundsreqId);
      LOG.debug("ps:" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {
        if (rs.getString("fromaccount") != null
            && (!rs.getString("fromdept").equals(rs.getString("bcu")))
            && (rs.getString("fromorg") != null
                && (tempfromOrg == null || !tempfromOrg.equals(rs.getString("fromorg"))))) {
          tempfromOrg = rs.getString("fromorg");
          role = UtilityDAO.getOrgMangOrBudgManagerUserRole(clientId, false, true,
              rs.getString("fromorg"));
          LOG.debug("role.size():" + role.size());
          if (role.size() > 0) {
            for (UserRoles rol : role) {
              json = new JSONObject();
              json.put("userId", rol.getUserContact().getId());
              json.put("roleId", rol.getRole().getId());
              json.put("type", "REL");
              array.put(json);
            }
          }
        } else if (rs.getString("toaccount") != null
            && (!rs.getString("todept").equals(rs.getString("bcu")))
            && (rs.getString("toorg") != null
                && (temptoOrg == null || !temptoOrg.equals(rs.getString("toorg"))))) {
          temptoOrg = rs.getString("toorg");

          role = UtilityDAO.getOrgMangOrBudgManagerUserRole(clientId, false, true,
              rs.getString("toorg"));

          if (role.size() > 0) {
            for (UserRoles rol : role) {
              json = new JSONObject();
              json.put("userId", rol.getUserContact().getId());
              LOG.debug("rol:" + rol.getRole().getId());
              json.put("roleId", rol.getRole().getId());
              json.put("type", "REC");
              array.put(json);
            }
          }
        }
      }
      result.put("list", array);
      LOG.debug("result:" + result);
      return result;

    } catch (Exception e) {
      Log.error("Exception in alertonOrgManager: ", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
    }
    return result;
  }

  /**
   * check distribute unqiuecode is present or not in Accounting dimension ( for Auto BCU request)
   * 
   * @param trxline
   * @param adjline
   * @param conn
   * @param headerId
   * @return
   */
  public static Boolean checkDistUniquecodePresntOrNot(EfinBudgetTransfertrx trxline,
      BudgetAdjustment adjline, Connection conn, String headerId, EFINBudget budget) {
    String strQry = null, lineid = null, isdistcol = null, distorgcol = null, lntable = null,
        headerid = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    Boolean errorflag = false;
    try {
      if (trxline != null) {
        lineid = " efin_budget_transfertrxline_id ";
        headerid = " efin_budget_transfertrx_id ";
        distorgcol = " distribute_line_org ";
        isdistcol = " distribute ";
        lntable = " efin_budget_transfertrxline ";
      }
      if (adjline != null) {
        lineid = " efin_budgetadjline_id ";
        headerid = " efin_budgetadj_id ";
        distorgcol = " dislinkorg ";
        isdistcol = " isdistribute ";
        lntable = " efin_budgetadjline ";
      }
      if (budget != null) {
        lineid = " efin_budgetlines_id ";
        headerid = " efin_budget_id ";
        distorgcol = " dislinkorg ";
        isdistcol = " isdistribute ";
        lntable = " efin_budgetlines ";
      }

      strQry = " select  com.c_validcombination_id as comid , lncom.c_validcombination_id as lncomid ,ln."
          + lineid + " as lineid from " + lntable + " ln "
          + " left join c_validcombination lncom on lncom.c_validcombination_id= ln.c_validcombination_id "
          + " join efin_budget_ctrl_param para on para.ad_client_id=ln.ad_client_id "
          + " left join c_validcombination com on com.ad_org_id=ln." + distorgcol
          + " and com.c_salesregion_id= para.budgetcontrol_costcenter and com.account_id= lncom.account_id "
          + " and com.c_project_id= lncom.c_project_id and  com.c_activity_id=lncom.c_activity_id and com.user1_id=lncom.user1_id "
          + " and com.user2_id =lncom.user2_id and com.c_campaign_id= lncom.c_campaign_id and com.c_bpartner_id= lncom.c_bpartner_id  and com.isactive='Y'"
          + " where 1=1 and " + distorgcol + " is not null  and  " + isdistcol + " ='Y'  "
          + "  and ln." + headerid + "= ? ";

      ps = conn.prepareStatement(strQry);
      ps.setString(1, headerId);
      LOG.debug("checkDistUniquecodePresntOrNot:" + ps.toString());
      rs = ps.executeQuery();
      while (rs.next()) {

        // update the line status
        if (trxline != null) {
          if (rs.getString("comid") == null && trxline != null) {
            EfinBudgetTransfertrxline line = OBDal.getInstance()
                .get(EfinBudgetTransfertrxline.class, rs.getString("lineid"));
            line.setStatus(OBMessageUtils.messageBD("EFIN_DistUnqiueCodeNotPresent"));
            OBDal.getInstance().save(line);
            errorflag = true;
          }
          // else {
          // EfinBudgetTransfertrxline line = OBDal.getInstance()
          // .get(EfinBudgetTransfertrxline.class, rs.getString("lineid"));
          // line.setStatus(null);
          // OBDal.getInstance().save(line);
          // }
        } else {
          if (budget == null) {
            if (rs.getString("comid") == null && adjline != null) {
              BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
                  rs.getString("lineid"));
              line.setFailureReason(OBMessageUtils.messageBD("EFIN_DistUnqiueCodeNotPresent"));
              line.setAlertStatus("Failed");
              OBDal.getInstance().save(line);
              errorflag = true;
            }
            // else {
            // BudgetAdjustmentLine line = OBDal.getInstance().get(BudgetAdjustmentLine.class,
            // rs.getString("lineid"));
            // line.setFailureReason(null);
            // line.setAlertStatus(null);
            // OBDal.getInstance().save(line);
            // }
          } else {
            if (rs.getString("comid") == null && budget != null) {
              EFINBudgetLines line = OBDal.getInstance().get(EFINBudgetLines.class,
                  rs.getString("lineid"));
              line.setCheckingStausFailure(
                  (OBMessageUtils.messageBD("EFIN_DistUnqiueCodeNotPresent")));
              line.setCheckingStaus("Failed");
              OBDal.getInstance().save(line);
              errorflag = true;
            } else {
              EFINBudgetLines line = OBDal.getInstance().get(EFINBudgetLines.class,
                  rs.getString("lineid"));
              line.setCheckingStausFailure(null);
              line.setCheckingStaus(null);
              OBDal.getInstance().save(line);
            }
          }
        }
      }
      if (!errorflag)
        return true;
      else
        return false;
    } catch (Exception e) {
      Log.error("Exception in checkDistUniquecodePresntOrNot: ", e);
      OBDal.getInstance().rollbackAndClose();
      return false;
    } finally {
    }
  }

  /**
   * send the alert for Auto BCU Request -Transaction Org budg Manager
   * 
   * @param req
   * @param roleId
   * @param con
   * @param clientId
   * @param vars
   */
  public static void sendAlertForDirectDist(EFINFundsReq req, String roleId, Connection con,
      String clientId, VariablesSecureApp vars) {
    JSONObject result = new JSONObject();
    ArrayList<String> includeRecipient = new ArrayList<String>();
    String alertWindow = AlertWindow.BudgetDistribution, Description = null, alertRuleId = null;
    try {

      // get alert rule id
      OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
          "as e where e.client.id='" + clientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (queryAlertRule.list().size() > 0) {
        AlertRule objRule = queryAlertRule.list().get(0);
        alertRuleId = objRule.getId();
      }

      // get alert recipient
      OBQuery<AlertRecipient> receipientQuery = OBDal.getInstance()
          .createQuery(AlertRecipient.class, "as e where e.alertRule.id='" + alertRuleId + "'");
      // check and insert recipient
      if (receipientQuery.list().size() > 0) {
        for (AlertRecipient objAlertReceipient : receipientQuery.list()) {
          includeRecipient.add(objAlertReceipient.getRole().getId());
          OBDal.getInstance().remove(objAlertReceipient);
        }
      }

      // get the org manager userid and roles
      result = alertonOrgManager(req.getId(), roleId, con, clientId);

      if (result != null) {
        JSONObject json1 = null;
        JSONArray jsonArray = result.getJSONArray("list");
        LOG.debug("jsonArray:" + jsonArray);
        for (int i = 0; i < jsonArray.length(); i++) {
          json1 = jsonArray.getJSONObject(i);
          LOG.debug("json1.getString(\"roleId\"))" + json1.getString("roleId"));
          includeRecipient.add(json1.getString("roleId"));
        }
      }

      // avoid duplicate recipient
      HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
      Iterator<String> iterator = incluedSet.iterator();
      while (iterator.hasNext()) {
        AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
      }

      // alert on ORG Budget Manager
      if (result != null) {
        String alertKey = "";
        JSONObject json1 = null;
        JSONArray jsonArray = result.getJSONArray("list");
        for (int i = 0; i < jsonArray.length(); i++) {
          json1 = jsonArray.getJSONObject(i);
          LOG.debug("type:" + json1.getString("type").equals("DIST"));

          if (json1.getString("type").equals("REC")) {
            Description = sa.elm.ob.finance.properties.Resource
                .getProperty("finance.fundsreq.received", vars.getLanguage())
                .replace("%", req.getDocumentNo());
            alertKey = "finance.fundsreq.received";
          }

          else {
            Description = sa.elm.ob.finance.properties.Resource
                .getProperty("finance.fundsreq.released", vars.getLanguage())
                .replace("%", req.getDocumentNo());
            alertKey = "finance.fundsreq.released";
          }

          AlertUtility.alertInsertionRole(req.getId(), req.getDocumentNo(),
              json1.getString("roleId"), json1.getString("userId"), req.getClient().getId(),
              Description, "NEW", alertWindow, alertKey, Constants.GENERIC_TEMPLATE);
        }
      }

    } catch (Exception e) {
      Log.error("Exception in sendAlertForDirectDist: ", e);
      OBDal.getInstance().rollbackAndClose();
    }
  }

  public static String getDepartment(String validCombination) {
    String department = null;
    try {
      AccountingCombination accCombination = OBDal.getInstance().get(AccountingCombination.class,
          validCombination);
      if (accCombination != null) {
        department = accCombination.getSalesRegion().getId();
      }

    } catch (Exception e) {
      Log.error("Exception in getDepartment: ", e);
      OBDal.getInstance().rollbackAndClose();
    }
    return department;
  }
}