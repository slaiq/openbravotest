/*
 *************************************************************************
 * All Rights Reserved.
 * Contributor(s):  Qualian
 ************************************************************************
 */
package sa.elm.ob.finance.event.dao;

import java.util.List;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlJournalLineEventDAO {
  private static final Logger LOG = LoggerFactory.getLogger(GlJournalLineEventDAO.class);

  public static List<AccountingCombination> getUniqueCodeList(Invoice invoice) {
    List<AccountingCombination> uniquecodeList = null;

    try {
      OBQuery<AccountingCombination> accCombination = OBDal.getInstance().createQuery(
          AccountingCombination.class,
          " as e where e.id in (select li.accountingCombination.id from Efin_Budget_Manencumlines li where li.manualEncumbrance.id in (select inv.efinManualencumbrance.id from Invoice inv where inv.id =:invoiceId)) ");
      accCombination.setNamedParameter("invoiceId", invoice.getId());
      if (accCombination != null && accCombination.list().size() > 0) {
        uniquecodeList = accCombination.list();
      }

    } catch (Exception e) {
      LOG.error("Exception in getUniqueCodeList ", e);
    }
    return uniquecodeList;
  }

  public static List<AccountingCombination> getUniqueCodeListUsingInv(Invoice invoice) {
    List<AccountingCombination> uniquecodeList = null;

    try {
      OBQuery<AccountingCombination> accCombination = OBDal.getInstance().createQuery(
          AccountingCombination.class,
          " as e where e.id in (select li.efinCValidcombination.id from InvoiceLine li where li.invoice.id = :invoiceId)");
      accCombination.setNamedParameter("invoiceId", invoice.getId());
      if (accCombination != null && accCombination.list().size() > 0) {
        uniquecodeList = accCombination.list();
      }

    } catch (Exception e) {
      LOG.error("Exception in getUniqueCodeList ", e);
    }
    return uniquecodeList;
  }
}
