package sa.elm.ob.finance.actionHandler.dao;

import java.math.BigDecimal;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;

/**
 * 
 * @author Divya 03/04/2019
 *
 */

public class AddUniqueCodeInEncumLinesDAO {
  private static final Logger LOG = LoggerFactory.getLogger(AddUniqueCodeInEncumLinesDAO.class);

  /**
   * insert encumbrance lines
   * 
   * @param encum
   * @param Amount
   * @param combination
   * @param lineno
   * @return
   */
  public static int insertEncumbranceLines(EfinBudgetManencum encum, BigDecimal Amount,
      AccountingCombination combination, Long lineno, BigDecimal fAInPopup) {
    EfinBudgetInquiry budInq = null;
    try {
      OBContext.setAdminMode();
      EfinBudgetManencumlines manualline = OBProvider.getInstance()
          .get(EfinBudgetManencumlines.class);
      manualline.setClient(encum.getClient());
      manualline.setOrganization(encum.getOrganization());
      manualline.setUpdatedBy(encum.getCreatedBy());
      manualline.setCreationDate(new java.util.Date());
      manualline.setCreatedBy(encum.getCreatedBy());
      manualline.setUpdated(new java.util.Date());
      manualline.setLineNo(lineno);
      manualline.setFundsAvailable(fAInPopup);
      if (combination != null) {
        manualline.setUniquecode(combination.getEfinUniqueCode());
        manualline.setSalesRegion(combination.getSalesRegion());
        manualline.setAccountElement(combination.getAccount());
        manualline.setSalesCampaign(combination.getSalesCampaign());
        manualline.setProject(combination.getProject());
        manualline.setActivity(combination.getActivity());
        manualline.setStDimension(combination.getStDimension());
        manualline.setNdDimension(combination.getNdDimension());
        manualline.setAccountingCombination(combination);
      }

      manualline.setBudgetLines(null);
      manualline.setManualEncumbrance(encum);
      manualline.setAmount(Amount);
      manualline.setRevamount(Amount);
      manualline.setRemainingAmount(Amount);
      manualline.setOriginalamount(Amount);
      manualline.setAPPAmt(BigDecimal.ZERO);
      manualline.setSystemUpdatedAmt(Amount);
      manualline.setUsedAmount(BigDecimal.ZERO);
      manualline.setManualline(true);
      OBDal.getInstance().save(manualline);
      OBDal.getInstance().flush();

      // Update or Insert in Budget Enquiry for unique code with department funds ='N'

      if (!manualline.getAccountingCombination().isEFINDepartmentFund()) {
        String budgetIntId = manualline.getManualEncumbrance().getBudgetInitialization().getId();

        budInq = sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO
            .getBudgetInquiry(manualline.getAccountingCombination().getId(), budgetIntId);

        if (budInq != null) {
          budInq.setEncumbrance(budInq.getEncumbrance().add(manualline.getAmount()));
          OBDal.getInstance().save(budInq);
        } else {
          EfinBudgetInquiry parentInq = null;
          // Get Parent Id for new budget Inquiry
          parentInq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
              manualline.getAccountingCombination(),
              manualline.getManualEncumbrance().getBudgetInitialization());

          budInq = OBProvider.getInstance().get(EfinBudgetInquiry.class);
          budInq.setEfinBudgetint(manualline.getManualEncumbrance().getBudgetInitialization());
          budInq.setAccountingCombination(manualline.getAccountingCombination());
          budInq.setUniqueCodeName(manualline.getUniqueCodeName());
          budInq.setUniqueCode(manualline.getAccountingCombination().getEfinUniqueCode());
          budInq.setOrganization(manualline.getOrganization());
          budInq.setDepartment(manualline.getSalesRegion());
          budInq.setAccount(manualline.getAccountElement());
          budInq.setSalesCampaign(manualline.getSalesCampaign());
          budInq.setProject(manualline.getProject());
          budInq.setBusinessPartner(manualline.getBusinessPartner());
          budInq.setFunctionalClassfication(manualline.getActivity());
          budInq.setFuture1(manualline.getStDimension());
          budInq.setNdDimension(manualline.getNdDimension());
          budInq.setORGAmt(BigDecimal.ZERO);
          budInq.setREVAmount(BigDecimal.ZERO);
          budInq.setFundsAvailable(BigDecimal.ZERO);
          budInq.setCurrentBudget(BigDecimal.ZERO);
          budInq.setEncumbrance(manualline.getAmount());
          budInq.setSpentAmt(BigDecimal.ZERO);
          budInq.setParent(parentInq);
          budInq.setVirtual(true);
          OBDal.getInstance().save(budInq);
        }
      }

      // -------------------
    }

    catch (

    Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while insertEncumbranceLines in AddUniqueCodeInEncumLinesDAO : ", e,
            e);
      }
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return 1;
  }

}