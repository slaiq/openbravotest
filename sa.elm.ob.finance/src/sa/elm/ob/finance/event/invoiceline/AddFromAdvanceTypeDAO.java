package sa.elm.ob.finance.event.invoiceline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.gl.GLItem;

import sa.elm.ob.finance.efinDistributionLines;

/**
 * 
 * @author Gopinagh. R
 */

public class AddFromAdvanceTypeDAO {
  private static Logger log4j = Logger.getLogger(AddFromAdvanceType.class);

  /**
   * This method is used to check is multi entity
   * 
   * @param businessPartner
   * @return
   */
  public static Boolean isMultiEntity(BusinessPartner businessPartner) {
    Boolean isMultiEntity = Boolean.FALSE;

    try {
      if (businessPartner.isEfinMulprepaymentEntity()) {
        isMultiEntity = Boolean.TRUE;
      }
    } catch (Exception e) {
      log4j.error("Exception while isMultiEntity: " + e);
    }
    return isMultiEntity;
  }

  /**
   * This method is used to get adjustment accounts
   * 
   * @param invoice
   * @return
   */
  public static List<efinDistributionLines> getAdjustmentAccounts(Invoice invoice) {
    List<efinDistributionLines> adjustmentAccounts = new ArrayList<efinDistributionLines>();
    try {
      StringBuilder whereClause = new StringBuilder();

      whereClause.append(" where efinDistribution.id = :inpDistribution  ");
      whereClause.append(" and businessPartner.id = :inpSupplier");
      // whereClause.append(" and organization.id = :inpOrg ");

      if ("M".equals(invoice.getEfinEncumtype())) {

        whereClause.append(" and ( expenseUniquecode is null or  expenseUniquecode.id in ( ");
        whereClause.append(
            " select accountingCombination.id from Efin_Budget_Manencumlines where manualEncumbrance.id  = :inpEncumbranceId )) ");

      }

      OBQuery<efinDistributionLines> distributionLinesQuery = OBDal.getInstance()
          .createQuery(efinDistributionLines.class, whereClause.toString());

      distributionLinesQuery.setNamedParameter("inpDistribution",
          invoice.getEfinDistribution() != null ? invoice.getEfinDistribution().getId() : "");
      distributionLinesQuery.setNamedParameter("inpSupplier", invoice.getBusinessPartner().getId());
      // distributionLinesQuery.setNamedParameter("inpOrg", invoice.getOrganization().getId());

      if ("M".equals(invoice.getEfinEncumtype()))
        distributionLinesQuery.setNamedParameter("inpEncumbranceId",
            invoice.getEfinManualencumbrance().getId());

      if (distributionLinesQuery != null) {
        adjustmentAccounts = distributionLinesQuery.list();
      }

    } catch (Exception e) {
      log4j.error("Exception while isMultiEntity: " + e);
    }
    return adjustmentAccounts;
  }

  /**
   * This method is used to insert invoice lines
   * 
   * @param adjustmentAccounts
   * @param invoice
   * @param invoiceLines
   */
  public static void insertInvoiceLines(List<efinDistributionLines> adjustmentAccounts,
      Invoice invoice, List<Object> invoiceLines) {
    try {
      InvoiceLine line = null;
      Integer lineNo = 10;
      GLItem account = getAccount();

      for (efinDistributionLines adjustmentLine : adjustmentAccounts) {
        line = OBProvider.getInstance().get(InvoiceLine.class);

        line.setClient(invoice.getClient());
        line.setOrganization(invoice.getOrganization());
        line.setCreatedBy(OBContext.getOBContext().getUser());
        line.setUpdatedBy(OBContext.getOBContext().getUser());
        line.setLineNo(Long.parseLong(String.valueOf(lineNo)));
        line.setInvoice(invoice);
        line.setAccount(account);

        line.setProject(adjustmentLine.getProject());
        line.setStDimension(adjustmentLine.getStDimension());
        line.setNdDimension(adjustmentLine.getNdDimension());
        line.setEfinCCampaign(adjustmentLine.getSalesCampaign());
        line.setEfinCSalesregion(adjustmentLine.getSalesRegion());
        line.setEfinCActivity(adjustmentLine.getActivity());
        line.setEFINUniqueCode(adjustmentLine.getAccountingCombination().getEfinUniqueCode());
        line.setEfinCElementvalue(adjustmentLine.getAccountElement());
        line.setEfinDistributionLines(adjustmentLine);
        line.setEfinCValidcombination(adjustmentLine.getAccountingCombination());
        line.setEfinExpenseAccount(adjustmentLine.getExpenseUniquecode());
        line.setEfinCBpartner(adjustmentLine.getBusinessPartner());
        line.setLineNetAmount(BigDecimal.ONE);
        line.setEFINFundsAvailable(BigDecimal.ZERO);

        invoiceLines.add(line);

        lineNo = lineNo + 10;

      }
      // OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error("Exception while isMultiEntity: " + e);
    }
  }

  /**
   * This method is to create invoice line automatically on When Selecting Pre payment invoice as
   * document type
   * 
   * @param adjustmentAccounts
   * @param invoice
   * @param invoiceLines
   */
  public static void insertInvoiceLinesOnUpdate(List<efinDistributionLines> adjustmentAccounts,
      Invoice invoice) {
    try {
      InvoiceLine line = null;
      Integer lineNo = 10;
      GLItem account = getAccount();

      for (efinDistributionLines adjustmentLine : adjustmentAccounts) {
        line = OBProvider.getInstance().get(InvoiceLine.class);

        line.setClient(invoice.getClient());
        line.setOrganization(invoice.getOrganization());
        line.setCreatedBy(OBContext.getOBContext().getUser());
        line.setUpdatedBy(OBContext.getOBContext().getUser());
        line.setLineNo(Long.parseLong(String.valueOf(lineNo)));
        line.setInvoice(invoice);
        line.setAccount(account);

        line.setProject(adjustmentLine.getProject());
        line.setStDimension(adjustmentLine.getStDimension());
        line.setNdDimension(adjustmentLine.getNdDimension());
        line.setEfinCCampaign(adjustmentLine.getSalesCampaign());
        line.setEfinCSalesregion(adjustmentLine.getSalesRegion());
        line.setEfinCActivity(adjustmentLine.getActivity());
        line.setEFINUniqueCode(adjustmentLine.getAccountingCombination().getEfinUniqueCode());
        line.setEfinCElementvalue(adjustmentLine.getAccountElement());
        line.setEfinDistributionLines(adjustmentLine);
        line.setEfinCValidcombination(adjustmentLine.getAccountingCombination());
        line.setEfinExpenseAccount(adjustmentLine.getExpenseUniquecode());
        line.setEfinCBpartner(adjustmentLine.getBusinessPartner());
        line.setLineNetAmount(BigDecimal.ONE);
        line.setEFINFundsAvailable(BigDecimal.ZERO);

        lineNo = lineNo + 10;
        OBDal.getInstance().save(line);

      }
      // OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error("Exception while isMultiEntity: " + e);
    }
  }

  /**
   * This method is used to get account
   * 
   * @return
   */
  public static GLItem getAccount() {
    GLItem account = null;
    List<GLItem> accounts = null;
    try {
      OBQuery<GLItem> accountQuery = OBDal.getInstance().createQuery(GLItem.class,
          " order by creationDate desc ");

      if (accountQuery != null) {
        accounts = accountQuery.list();

        if (accounts.size() > 0)
          account = accounts.get(0);
      }
    } catch (Exception e) {
      log4j.error("Exception while getAccount: " + e);
    }

    return account;
  }
}
