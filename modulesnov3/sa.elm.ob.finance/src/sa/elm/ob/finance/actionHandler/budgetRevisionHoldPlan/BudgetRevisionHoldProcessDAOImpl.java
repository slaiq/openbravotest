package sa.elm.ob.finance.actionHandler.budgetRevisionHoldPlan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.marketing.Campaign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINRdvBudgHoldLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.EfinRdvBudgTransfer;
import sa.elm.ob.finance.ad_callouts.BudgetAdjustmentCallout;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;
import sa.elm.ob.finance.ad_callouts.dao.HijiridateDAO;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author poongodi on 28/11/2019
 *
 */
public class BudgetRevisionHoldProcessDAOImpl implements BudgetRevisionHoldProcessDao {
  private static final Logger LOG = LoggerFactory.getLogger(BudgetRevisionHoldProcessDAOImpl.class);
  public static final String Transaction_Type = "REV";

  @Override
  public EfinBudgetTransfertrx insertBudgetRevisionHeader(JSONArray selectedlines,
      String revDocType) {
    String fundsBudgetType = null;
    boolean budgetLine = true;
    EfinBudgetTransfertrx header = null;
    // TODO Auto-generated method stub
    try {
      OBContext.setAdminMode();
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
      String gregorianmonth = "";
      SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyy-MM-dd");
      String date = dateYearFormat.format(new java.util.Date());
      Date todayDate = null;
      String yearId = null;

      try {
        todayDate = dateYearFormat.parse(date);
      } catch (ParseException e1) {
      }

      header = OBProvider.getInstance().get(EfinBudgetTransfertrx.class);
      header.setClient(OBContext.getOBContext().getCurrentClient());
      header.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
      header.setActive(true);
      header.setCreatedBy(OBContext.getOBContext().getUser());
      header.setCreationDate(new java.util.Date());
      header.setUpdated(new java.util.Date());
      header.setUpdatedBy(OBContext.getOBContext().getUser());
      header.setAccountingDate(todayDate);
      header.setTrxdate(todayDate);
      header.setDocType(revDocType);// Transaction_Type
      header
          .setRole(OBDal.getInstance().get(Role.class, OBContext.getOBContext().getRole().getId()));
      OBQuery<Campaign> budgetType = OBDal.getInstance().createQuery(Campaign.class,
          " as e where e.efinBudgettype ='F' ");
      if (budgetType.list() != null && budgetType.list().size() > 0) {
        fundsBudgetType = budgetType.list().get(0).getId();

      }
      header.setSalesCampaign(OBDal.getInstance().get(Campaign.class, fundsBudgetType));
      yearId = getYearId(header.getAccountingDate());
      header.setYear(OBDal.getInstance().get(Year.class, yearId));

      String budgetReferenceId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(
          header.getAccountingDate(), OBContext.getOBContext().getCurrentClient().getId(), "");
      header.setEfinBudgetint(
          OBDal.getInstance().get(EfinBudgetIntialization.class, budgetReferenceId));
      header.setRdvhold(true);
      gregorianmonth = HijiridateDAO.getGregorianPeriod(
          UtilityDAO.convertTohijriDate(dateFormat.format(header.getTrxdate())));
      header.setTransactionperiod(gregorianmonth);
      OBDal.getInstance().save(header);
      OBDal.getInstance().flush();
      budgetLine = insertBudgetrevisionLines(selectedlines, header);

    } catch (OBException e) {
      LOG.error("Exception while insertBudgetRevisionHeader:" + e);
      throw new OBException(e.getMessage());
    } catch (SQLException e) {
      // TODO Auto-generated catch block
    } finally {
      OBContext.restorePreviousMode();
    }
    return header;

  }

  /**
   * 
   * @param accountingdate
   * @return
   */
  public String getYearId(Date accountingdate) {
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String yearId = null;
    try {
      ps = conn.prepareStatement(
          "select yr.c_year_id as year from c_period  pr  join c_year yr on yr.c_year_id= pr.c_year_id "
              + "     where '" + accountingdate + "'"
              + "  between startdate and enddate  and pr.ad_client_id='"
              + OBContext.getOBContext().getCurrentClient().getId() + "' and pr.ad_org_id = '0' ");
      rs = ps.executeQuery();
      if (rs.next()) {
        yearId = rs.getString("year");
      }

    } catch (SQLException e) {
      // TODO Auto-generated catch block
    }
    return yearId;
  }

  public boolean insertBudgetrevisionLines(JSONArray selectedlines, EfinBudgetTransfertrx header) {
    String budgetInquId = null;
    EfinBudgetTransfertrxline line = null;
    boolean fundsAccount = true;
    EfinBudgetInquiry budgetinquiryline = null;
    EfinBudgetInquiry budgetinqu_cost = null;
    try {
      OBContext.setAdminMode();
      for (int i = 0; i < selectedlines.length(); i++) {
        JSONObject selectedRow = selectedlines.getJSONObject(i);

        line = OBProvider.getInstance().get(EfinBudgetTransfertrxline.class);
        line.setClient(OBDal.getInstance().get(org.openbravo.model.ad.system.Client.class,
            header.getClient().getId()));
        line.setOrganization(
            OBDal.getInstance().get(Organization.class, header.getOrganization().getId()));
        line.setActive(true);
        line.setCreatedBy(OBDal.getInstance().get(User.class, header.getCreatedBy().getId()));
        line.setCreationDate(new java.util.Date());
        line.setUpdatedBy(OBDal.getInstance().get(User.class, header.getUpdatedBy().getId()));
        line.setUpdated(new java.util.Date());
        AccountingCombination accCombination = OBDal.getInstance().get(AccountingCombination.class,
            selectedRow.getString("accountingCombination"));
        accCombination = getParentFunds990acct(accCombination, header.getEfinBudgetint());

        line.setAccountingCombination(accCombination);
        // already present
        OBQuery<EfinBudgetTransfertrxline> revisionLine = OBDal.getInstance().createQuery(
            EfinBudgetTransfertrxline.class,
            " as e where e.efinBudgetTransfertrx.id = :headerId and e.accountingCombination.id =:validCombinationId");
        revisionLine.setNamedParameter("headerId", header.getId());
        revisionLine.setNamedParameter("validCombinationId", accCombination);
        if (revisionLine != null && revisionLine.list().size() > 0) {
          line = revisionLine.list().get(0);
          line.setIncrease(line.getIncrease().add(new BigDecimal(selectedRow.getString("amount"))));
          OBDal.getInstance().save(line);
        } else {
          line.setIncrease(new BigDecimal(selectedRow.getString("amount")));
          line.setRdvhold(true);
          line.setEfinBudgetTransfertrx(header);
          OBQuery<EfinBudgetInquiry> budgetinquiry = OBDal.getInstance().createQuery(
              EfinBudgetInquiry.class,
              "accountingCombination.id=:validCombination and efinBudgetint.id = :budgetInt and salesCampaign.id = :salesCampaign ");
          budgetinquiry.setNamedParameter("validCombination", accCombination);
          budgetinquiry.setNamedParameter("budgetInt", header.getEfinBudgetint());
          budgetinquiry.setNamedParameter("salesCampaign", header.getSalesCampaign());

          if (budgetinquiry.list() != null && budgetinquiry.list().size() > 0) {
            budgetInquId = budgetinquiry.list().get(0).getId();
          }
          if (budgetInquId != null)
            budgetinquiryline = OBDal.getInstance().get(EfinBudgetInquiry.class, budgetInquId);

          if (budgetinquiryline != null) {
            line.setCurrentBudget(budgetinquiryline.getCurrentBudget());
            line.setFundsAvailable(budgetinquiryline.getFundsAvailable());
            line.setUniqueCodeName(budgetinquiryline.getUniqueCodeName());

          } else {
            line.setCurrentBudget(BigDecimal.ZERO);
            line.setFundsAvailable(BigDecimal.ZERO);
            line.setUniqueCodeName(accCombination.getEfinUniquecodename());
          }
          // getCost990acct
          AccountingCombination accCombination_cost = OBDal.getInstance()
              .get(AccountingCombination.class, selectedRow.getString("accountingCombination"));
          budgetinqu_cost = getParentCost990acct(accCombination_cost, header.getEfinBudgetint());

          if (budgetinqu_cost != null) {
            line.setCostcurrentbudget(budgetinqu_cost.getFundsAvailable());
          } else {
            line.setCostcurrentbudget(BigDecimal.ZERO);
          }
          // distribute org
          line.setDistribute(true);
          line.setDistributeLineOrg(accCombination_cost.getTrxOrganization());
          OBDal.getInstance().save(line);
          OBDal.getInstance().flush();
        }
        EFINRdvBudgHoldLine rdvLine = OBDal.getInstance().get(EFINRdvBudgHoldLine.class,
            selectedRow.getString("efinRdvBudgholdline"));

        insertbudTransfer(rdvLine, line, new BigDecimal(selectedRow.getString("amount")));

      }

    } catch (

    OBException e) {
      LOG.error("Exception while insertBudgetrevisionLines:" + e);
      throw new OBException(e.getMessage());
    } catch (JSONException e) {
      // TODO Auto-generated catch block
    } finally {
      OBContext.restorePreviousMode();
    }
    return fundsAccount;
  }

  public void insertbudTransfer(EFINRdvBudgHoldLine holdLine, EfinBudgetTransfertrxline line,
      BigDecimal amount) {
    try {
      OBContext.setAdminMode();
      EfinRdvBudgTransfer transfer = OBProvider.getInstance().get(EfinRdvBudgTransfer.class);
      transfer.setClient(OBDal.getInstance().get(org.openbravo.model.ad.system.Client.class,
          holdLine.getClient().getId()));
      transfer.setOrganization(
          OBDal.getInstance().get(Organization.class, holdLine.getOrganization().getId()));
      transfer.setActive(true);
      transfer.setCreatedBy(OBDal.getInstance().get(User.class, holdLine.getCreatedBy().getId()));
      transfer.setCreationDate(new java.util.Date());
      transfer.setUpdatedBy(OBDal.getInstance().get(User.class, holdLine.getUpdatedBy().getId()));
      transfer.setUpdated(new java.util.Date());
      transfer.setEfinBudgetTransfertrxline(line);
      transfer.setEfinRdvBudgholdline(holdLine);
      transfer.setReleased(false);
      transfer.setAmount(amount);
      OBDal.getInstance().save(transfer);
      OBDal.getInstance().flush();

    } catch (OBException e) {
      LOG.error("Exception while insertbudTransfer:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * 
   * @param accCombination
   * @param budgetInt
   * @return
   */
  public AccountingCombination getParentFunds990acct(AccountingCombination accCombination,
      EfinBudgetIntialization budgetInt) {
    AccountingCombination fundsUniqueCodeAcc = null;
    List<AccountingCombination> funds_999Account = null;
    String Funds_Uniquecode = null;
    AccountingCombination combination = null;
    List<AccountingCombination> acctcomlist = new ArrayList<AccountingCombination>();
    List<AccountingCombination> acctcomlist_990 = new ArrayList<AccountingCombination>();
    try {
      OBContext.setAdminMode();
      if (accCombination != null
          && accCombination.getSalesCampaign().getEfinBudgettype().equals("C")) {
        fundsUniqueCodeAcc = accCombination.getEfinFundscombination();
        Funds_Uniquecode = accCombination.getEfinFundscombination().getEfinUniqueCode();
      } else {
        fundsUniqueCodeAcc = accCombination;
      }
      acctcomlist = CommonValidationsDAO.getParentAccountCom(fundsUniqueCodeAcc,
          OBContext.getOBContext().getCurrentClient().getId());
      if (acctcomlist != null && acctcomlist.size() > 0) {
        combination = acctcomlist.get(0);
      }

      acctcomlist_990 = getParent990AccountCom(combination,
          OBContext.getOBContext().getCurrentClient().getId());
      if (acctcomlist_990 != null && acctcomlist_990.size() > 0) {
        combination = acctcomlist_990.get(0);
      }

    } catch (OBException e) {
      LOG.error("Exception while getParentFunds990acct:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return combination;

  }

  /**
   * 
   * @param accCombination
   * @param budgetInt
   * @return
   */
  public EfinBudgetInquiry getParentCost990acct(AccountingCombination accCombination,
      EfinBudgetIntialization budgetInt) {
    EfinBudgetInquiry parent_999acc = null;
    EfinBudgetInquiry parent_cost990acc = null;
    try {
      OBContext.setAdminMode();
      if (accCombination != null) {
        OBQuery<EfinBudgetInquiry> budgetinq = OBDal.getInstance().createQuery(
            EfinBudgetInquiry.class,
            "as e where e.accountingCombination.id =:validCombinationId and e.efinBudgetint.id = :budgetIntId ");
        budgetinq.setNamedParameter("validCombinationId", accCombination.getId());
        budgetinq.setNamedParameter("budgetIntId", budgetInt.getId());
        if (budgetinq != null && budgetinq.list().size() > 0) {
          parent_999acc = budgetinq.list().get(0).getParent();
        }
      }
      if (parent_999acc != null) {
        parent_cost990acc = parent_999acc.getParent();
      }

    } catch (OBException e) {
      LOG.error("Exception while getParentCost990acct:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return parent_cost990acc;

  }

  public static List<AccountingCombination> getParent990AccountCom(AccountingCombination com,
      String clientId) {
    List<AccountingCombination> acctlist = new ArrayList<AccountingCombination>();
    String department = null;
    String organization = null;
    try {
      EfinBudgetControlParam budgContrparam = FundsReqMangementDAO.getControlParam(clientId);
      department = budgContrparam.getBudgetcontrolunit().getId();
      organization = budgContrparam.getAgencyHqOrg().getId();
      OBQuery<AccountingCombination> accountCommQry = OBDal.getInstance().createQuery(
          AccountingCombination.class,
          "account.id= '" + com.getAccount().getId() + "'" + " and businessPartner.id='"
              + com.getBusinessPartner().getId() + "' " + "and salesRegion.id='" + department
              + "' and project.id = '" + com.getProject().getId() + "' " + "and salesCampaign.id='"
              + com.getSalesCampaign().getId() + "' " + "and activity.id='"
              + com.getActivity().getId() + "' and stDimension.id='" + com.getStDimension().getId()
              + "' " + " and ndDimension.id = '" + com.getNdDimension().getId() + "' "
              + " and organization.id = '" + organization + "'");

      if (accountCommQry.list().size() > 0)
        return accountCommQry.list();

    } catch (Exception e) {
      LOG.error("Exception in getParent990AccountCom " + e.getMessage());
    }
    return acctlist;

  }
}
