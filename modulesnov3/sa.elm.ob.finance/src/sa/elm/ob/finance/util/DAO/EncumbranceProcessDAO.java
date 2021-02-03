/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.util.DAO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.sales.SalesRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.ad_callouts.dao.FundsReqMangementDAO;

public class EncumbranceProcessDAO {

  private static final Logger LOG = LoggerFactory.getLogger(EncumbranceProcessDAO.class);

  public static Boolean insertEncumbranceLine(Connection conn, EfinBudgetTransfertrx budRev,
      BudgetAdjustment budAdj, BigDecimal decreaseAmount, String validCombinationId,
      boolean isPOHold) {
    Boolean errorflag = false;
    Boolean UpdateFlag = false;
    EfinBudgetManencumlines lines = null;
    EfinBudgetManencum manual = null;
    long lineno = 10;
    PreparedStatement ps = null;
    ResultSet rs = null;
    Boolean updateAdjFlag = false;
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetManencum> chkLineExists = OBDal.getInstance().createQuery(
          EfinBudgetManencum.class,
          "as e where e.sourceref = '" + (budRev != null ? budRev.getId() : budAdj.getId()) + "'");

      if (chkLineExists.list() != null && chkLineExists.list().size() > 0) {
        manual = chkLineExists.list().get(0);
        ps = conn.prepareStatement(
            "select max(line) as line from efin_budget_manencumlines where efin_budget_manencum_id = '"
                + manual.getId() + "'");
        rs = ps.executeQuery();

        while (rs.next()) {
          lineno = lineno + rs.getLong("line");
        }
        lines = OBProvider.getInstance().get(EfinBudgetManencumlines.class);
        lines.setOrganization(manual.getOrganization());
        lines.setLineNo(lineno);
        lines.setAccountingCombination(
            OBDal.getInstance().get(AccountingCombination.class, validCombinationId));
        lines.setManualEncumbrance(manual);
        AccountingCombination validCombination = OBDal.getInstance()
            .get(AccountingCombination.class, validCombinationId);
        lines.setSalesRegion(validCombination.getSalesRegion());
        lines.setAccountElement(validCombination.getAccount());
        lines.setSalesCampaign(validCombination.getSalesCampaign());
        lines.setBusinessPartner(validCombination.getBusinessPartner());
        lines.setProject(validCombination.getProject());
        lines.setStDimension(validCombination.getStDimension());
        lines.setNdDimension(validCombination.getNdDimension());
        lines.setActivity(validCombination.getActivity());
        lines.setAmount(decreaseAmount);
        lines.setRevamount(decreaseAmount);
        lines.setRemainingAmount(decreaseAmount);
        OBDal.getInstance().save(lines);
        OBDal.getInstance().flush();
        updateBudgetInquiry(lines, manual, decreaseAmount, UpdateFlag, updateAdjFlag);
        if (isPOHold) {
          updateBudgetInquiryfor990(lines.getAccountingCombination(), lines.getClient().getId(),
              lines.getManualEncumbrance(), decreaseAmount);
        }
      } else {
        EfinBudgetManencumlines line = insertEncumbranceHeader(conn, budRev, budAdj,
            validCombinationId, decreaseAmount);
        updateBudgetInquiry(line, line.getManualEncumbrance(), decreaseAmount, UpdateFlag,
            updateAdjFlag);
        if (isPOHold) {
          updateBudgetInquiryfor990(line.getAccountingCombination(), line.getClient().getId(),
              line.getManualEncumbrance(), decreaseAmount);
        }
        if (budRev != null) {
          EfinBudgetTransfertrx transfer = OBDal.getInstance().get(EfinBudgetTransfertrx.class,
              budRev.getId());
          transfer.setManualEncumbrance(OBDal.getInstance().get(EfinBudgetManencum.class,
              line.getManualEncumbrance().getId()));

        } else {
          BudgetAdjustment budAdjustment = OBDal.getInstance().get(BudgetAdjustment.class,
              budAdj.getId());
          budAdjustment.setManualEncumbrance(OBDal.getInstance().get(EfinBudgetManencum.class,
              line.getManualEncumbrance().getId()));

        }
        errorflag = true;

      }

    } catch (OBException e) {
      LOG.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in insertEncumbranceLine " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return errorflag;

  }

  public static EfinBudgetManencumlines insertEncumbranceHeader(Connection conn,
      EfinBudgetTransfertrx budRev, BudgetAdjustment adjustment, String validCombinationId,
      BigDecimal decreaseAmount) {
    EfinBudgetManencum manual = null;
    EfinBudgetManencumlines lines = null;
    long lineno = 10;
    PreparedStatement ps = null;
    ResultSet rs = null;
    SalesRegion salesRegion = null;
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetControlParam> controlParam = OBDal.getInstance().createQuery(
          EfinBudgetControlParam.class,
          "as e where e.client.id = '"
              + (budRev != null ? budRev.getClient().getId() : adjustment.getClient().getId())
              + "'");
      controlParam.setMaxResult(1);
      if (controlParam != null && controlParam.list().size() > 0) {
        salesRegion = controlParam.list().get(0).getBudgetcontrolunit();
      }

      manual = OBProvider.getInstance().get(EfinBudgetManencum.class);
      manual.setOrganization(
          budRev != null ? budRev.getOrganization() : adjustment.getOrganization());
      manual.setTransactionDate(budRev != null ? budRev.getTrxdate() : adjustment.getTRXDate());
      manual.setAccountingDate(
          budRev != null ? budRev.getAccountingDate() : adjustment.getAccountingDate());
      manual.setEncumType("TE");
      manual.setEncumMethod("A");
      manual.setSalesCampaign(
          budRev != null ? budRev.getSalesCampaign() : adjustment.getBudgetType());
      manual.setBudgetInitialization(
          budRev != null ? budRev.getEfinBudgetint() : adjustment.getEfinBudgetint());

      manual.setSalesRegion(salesRegion);
      manual.setSourceref(budRev != null ? budRev.getId() : adjustment.getId());
      manual.setDocumentStatus("CO");
      manual.setAction("PD");
      manual.setDescription(budRev != null ? budRev.getDocumentNo() : adjustment.getDocno());
      OBDal.getInstance().save(manual);
      ps = conn.prepareStatement(
          "select coalesce(max(line),0)+10   as lineno from Efin_Budget_Manencumlines where efin_budget_manencum_id='"
              + manual + "'");
      rs = ps.executeQuery();
      if (rs.next()) {
        lineno = rs.getLong("lineno");
      }
      AccountingCombination validCombination = OBDal.getInstance().get(AccountingCombination.class,
          validCombinationId);

      lines = OBProvider.getInstance().get(EfinBudgetManencumlines.class);
      lines.setOrganization(manual.getOrganization());
      lines.setLineNo(lineno);
      lines.setAccountingCombination(
          OBDal.getInstance().get(AccountingCombination.class, validCombinationId));
      lines.setManualEncumbrance(manual);
      lines.setSalesRegion(validCombination.getSalesRegion());
      lines.setAccountElement(validCombination.getAccount());
      lines.setSalesCampaign(validCombination.getSalesCampaign());
      lines.setBusinessPartner(validCombination.getBusinessPartner());
      lines.setProject(validCombination.getProject());
      lines.setStDimension(validCombination.getStDimension());
      lines.setNdDimension(validCombination.getNdDimension());
      lines.setActivity(validCombination.getActivity());
      lines.setAmount(decreaseAmount);
      lines.setRevamount(decreaseAmount);
      lines.setRemainingAmount(decreaseAmount);
      OBDal.getInstance().save(lines);
      OBDal.getInstance().flush();

    } catch (OBException e) {
      LOG.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in insertEncumbranceHeader " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return lines;
  }

  public static int insertModificationEntry(Connection conn, EfinBudgetTransfertrx budRev,
      BudgetAdjustment adjustment, String validcombination) {
    EfinBudgetManencumlines Lines = null;
    Boolean updateRevFlag = false;
    Boolean updateAdjFlag = false;
    try {
      OBContext.setAdminMode();
      if (budRev != null) {
        updateRevFlag = true;
      } else {
        updateAdjFlag = true;
      }
      OBQuery<EfinBudgetManencumlines> chkLineExists = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          "as e where e.manualEncumbrance.sourceref = '"
              + (budRev != null ? budRev.getId() : adjustment.getId())
              + "' and accountingCombination.id = '" + validcombination + "' ");

      if (chkLineExists.list() != null && chkLineExists.list().size() > 0) {
        Lines = chkLineExists.list().get(0);
        EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
        manEncumRev.setOrganization(Lines.getOrganization());
        manEncumRev.setAccountingCombination(
            OBDal.getInstance().get(AccountingCombination.class, validcombination));
        manEncumRev.setRevamount(Lines.getRevamount().negate());
        manEncumRev.setManualEncumbranceLines(Lines);
        manEncumRev.setStatus("APP");
        manEncumRev.setSystem(true);
        manEncumRev.setAuto(true);
        OBDal.getInstance().save(manEncumRev);
        /*
         * Lines.setRevamount(Lines.getRevamount().add(manEncumRev.getRevamount()));
         * Lines.setRemainingAmount(Lines.getRemainingAmount().add(manEncumRev.getRevamount()));
         * OBDal.getInstance().save(Lines); OBDal.getInstance().flush();
         */
        updateBudgetInquiry(Lines, Lines.getManualEncumbrance(), manEncumRev.getRevamount(),
            updateRevFlag, updateAdjFlag);

      }

    } catch (

    Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in insertModificationEntry " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

  public static void updateBudgetInquiry(EfinBudgetManencumlines encumLine,
      EfinBudgetManencum manEncumbarance, BigDecimal revamount, Boolean UpdateFlag,
      Boolean updateAdjFlag) {

    OBQuery<EfinBudgetInquiry> budInq = null;
    try {

      budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
          "efinBudgetint.id='" + manEncumbarance.getBudgetInitialization().getId()
              + "' and accountingCombination.account.id='"
              + encumLine.getAccountingCombination().getAccount().getId() + "'");

      if (budInq.list() != null && budInq.list().size() > 0) {
        for (EfinBudgetInquiry Enquiry : budInq.list()) {
          if (encumLine.getAccountingCombination() == Enquiry.getAccountingCombination()) {
            Enquiry.setEncumbrance(Enquiry.getEncumbrance().add(revamount));
            if (UpdateFlag) {
              Enquiry.setRevdecAmt(Enquiry.getRevdecAmt().add(revamount.abs()));
            } else if (updateAdjFlag) {
              Enquiry.setObdecAmt(Enquiry.getObdecAmt().add(revamount.abs()));
            }
            OBDal.getInstance().save(Enquiry);

          }
        }
      }

    } catch (

    Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      LOG.error("Exception in updateBudgetInquiry " + e.getMessage());
    }
  }

  public static void updateBudgetInquiryfor990(AccountingCombination com, String clientId,
      EfinBudgetManencum manEncumbarance, BigDecimal amount) {
    OBQuery<EfinBudgetInquiry> budInq = null;
    String department = null;
    String organization = null;
    AccountingCombination comb = null;
    List<AccountingCombination> acctComb = new ArrayList<AccountingCombination>();
    List<EfinBudgetInquiry> inqList = new ArrayList<EfinBudgetInquiry>();
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
      acctComb = accountCommQry.list();
      if (acctComb.size() > 0) {
        comb = acctComb.get(0);
        if (comb != null) {
          budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
              "efinBudgetint.id=:budgInitId and accountingCombination.id=:acctCombId");
          budInq.setNamedParameter("budgInitId", manEncumbarance.getBudgetInitialization().getId());
          budInq.setNamedParameter("acctCombId", comb.getId());
          inqList = budInq.list();
          if (inqList.size() > 0) {
            EfinBudgetInquiry budgetInqObj = inqList.get(0);
            budgetInqObj.setEncumbrance(budgetInqObj.getEncumbrance().add(amount));
            OBDal.getInstance().save(budgetInqObj);
          }
        }
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      e.printStackTrace();
      LOG.error("Exception in updateBudgetInquiryfor990 " + e.getMessage());
    }
  }
}