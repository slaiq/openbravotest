package sa.elm.ob.finance.hooks;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openbravo.advpaymentmngt.ProcessInvoiceHook;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinManualEncumInvoice;

/**
 * 
 * @author sathish kumar.p
 * 
 *         This class is to implement the hook for Process Invoice. This is POST-execution hook
 * 
 *
 */

public class PostProcessInvoiceHook implements ProcessInvoiceHook {

  private final String ERROR = "Error";

  @Override
  public OBError preProcess(Invoice invoice, String strDocAction) {

    return null;
  }

  @Override
  public OBError postProcess(Invoice invoice, String strDocAction) {

    Invoice prepaymentInv = null;
    EfinBudgetManencum encum = null;
    BigDecimal invLinenetAmt = new BigDecimal("0");
    List<EfinBudgetManencumlines> encumbranceLineList = new ArrayList<EfinBudgetManencumlines>();
    List<EfinManualEncumInvoice> invoiceRefList = new ArrayList<EfinManualEncumInvoice>();

    BigDecimal expenseLineNetAmt = new BigDecimal("0");

    List<InvoiceLine> invoiceLine = null;
    String errorMsg = "";

    // If it is AR invoice, the pre payment invoice is present in line. Then reduce the line net
    // amount from enucmbrance. This is only for sales invoice (Arinvoice)

    if ("CO".equals(strDocAction)) {
      if (invoice.isSalesTransaction()) {
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
          if (line.getEfinReceiptType() != null && line.getEFINPrepayment() != null) {
            prepaymentInv = OBDal.getInstance().get(Invoice.class,
                line.getEFINPrepayment().getId());
            if (prepaymentInv != null) {
              if (prepaymentInv.getEfinManualencumbrance() != null) {
                encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                    prepaymentInv.getEfinManualencumbrance().getId());
                invLinenetAmt = line.getLineNetAmount();

                invoiceLine = prepaymentInv.getInvoiceLineList().stream()
                    .filter(a -> a.getEfinCValidcombination().getId()
                        .equals(line.getEfinCValidcombination().getId()))
                    .collect(Collectors.toList());

                if (invoiceLine != null && invoiceLine.size() > 0) {
                  InvoiceLine originalLine = invoiceLine.get(0);
                  encumbranceLineList = encum.getEfinBudgetManencumlinesList().stream()
                      .filter(a -> (a.getAccountingCombination().getId())
                          .equals(originalLine.getEfinExpenseAccount().getId()))
                      .collect(Collectors.toList());

                  if (encumbranceLineList != null && encumbranceLineList.size() == 1) {
                    for (EfinBudgetManencumlines encumLine : encumbranceLineList) {
                      // if applied amount in encumbrance line is less than the amount entered in
                      // order to receive
                      expenseLineNetAmt = invoice.getInvoiceLineList().stream()
                          .filter(a -> a.getEFINPrepayment() != null
                              && a.getEFINPrepayment().getId()
                                  .equals(line.getEFINPrepayment().getId())
                              && a.getEfinExpenseAccount() != null
                              && a.getEfinExpenseAccount().getId()
                                  .equals(originalLine.getEfinExpenseAccount().getId()))
                          .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                      if (expenseLineNetAmt.compareTo(new BigDecimal("0")) == 0) {
                        expenseLineNetAmt = invLinenetAmt;
                      }

                      if (encumLine.getAPPAmt().compareTo(expenseLineNetAmt) < 0) {
                        OBError error = new OBError();
                        error.setType(ERROR);
                        error.setTitle(OBMessageUtils.messageBD("OBUIAPP_Error"));
                        errorMsg = OBMessageUtils.messageBD("Efin_amountGreater_Encumamt")
                            .replace("@", invLinenetAmt.toPlainString())
                            .replace("!", originalLine.getEfinExpenseAccount().getEfinUniqueCode())
                            .replace("#", encumLine.getAPPAmt().toPlainString())
                            .replace("%", encumLine.getManualEncumbrance().getDocumentNo())
                            .replace("$", encumLine.getAccountingCombination().getEfinUniqueCode());
                        error.setMessage(errorMsg);
                        return error;

                      } else {
                        encumLine.setAPPAmt(encumLine.getAPPAmt().subtract(invLinenetAmt));
                        // encumLine.setENCDecrease(encumLine.getENCDecrease().add(invLinenetAmt));
                        OBDal.getInstance().save(encumLine);

                        // create modification entry in encumbrance while completing the arinvoice

                        EfinBudManencumRev modification = OBProvider.getInstance()
                            .get(EfinBudManencumRev.class);
                        modification.setUniqueCode(encumLine.getUniquecode());
                        modification.setAccountingCombination(encumLine.getAccountingCombination());
                        modification.setRevamount(invLinenetAmt.negate());
                        modification
                            .setDescription(OBMessageUtils.messageBD("Efin_encumbrance_desc")
                                .replace("@", invoice.getDocumentNo()));
                        modification.setManualEncumbranceLines(encumLine);
                        modification.setSystem(true);
                        OBDal.getInstance().save(modification);

                        invoiceRefList = encumLine.getEfinManualEncumInvoiceList().stream().filter(
                            a -> a.getInvoice().getId().equals(line.getEFINPrepayment().getId()))
                            .collect(Collectors.toList());

                        // decrease amount in invoice tab under the enumbrance lines
                        if (invoiceRefList != null) {
                          EfinManualEncumInvoice invoiceRef = invoiceRefList.get(0);
                          invoiceRef
                              .setInvamount(invoiceRef.getInvamount().subtract(invLinenetAmt));
                          OBDal.getInstance().save(invoiceRef);
                        }
                      }

                    }
                  }
                  OBDal.getInstance().flush();
                }
              }
            }
          }
        }
      }
    }

    // if action is reactivate then amount entered in line have to updated back to remaining amount
    // in prepayment invoice

    if ("RE".equals(strDocAction)) {
      if (invoice.isSalesTransaction()) {
        for (InvoiceLine line : invoice.getInvoiceLineList()) {
          if (line.getEfinReceiptType() != null && line.getEFINPrepayment() != null) {
            prepaymentInv = OBDal.getInstance().get(Invoice.class,
                line.getEFINPrepayment().getId());
            if (prepaymentInv != null) {
              prepaymentInv.setEfinPreRemainingamount(
                  prepaymentInv.getEfinPreRemainingamount().add(line.getLineNetAmount()));
              prepaymentInv.setEfinPreUsedamount(
                  prepaymentInv.getEfinPreUsedamount().subtract(line.getLineNetAmount()));
              OBDal.getInstance().save(prepaymentInv);

              if (prepaymentInv.getEfinManualencumbrance() != null) {
                encum = OBDal.getInstance().get(EfinBudgetManencum.class,
                    prepaymentInv.getEfinManualencumbrance().getId());
                invLinenetAmt = line.getLineNetAmount();

                invoiceLine = prepaymentInv.getInvoiceLineList().stream()
                    .filter(a -> a.getEfinCValidcombination().getId()
                        .equals(line.getEfinCValidcombination().getId()))
                    .collect(Collectors.toList());

                if (invoiceLine != null && invoiceLine.size() > 0) {
                  InvoiceLine originalLine = invoiceLine.get(0);
                  encumbranceLineList = encum.getEfinBudgetManencumlinesList().stream()
                      .filter(a -> (a.getAccountingCombination().getId())
                          .equals(originalLine.getEfinExpenseAccount().getId()))
                      .collect(Collectors.toList());

                  if (encumbranceLineList != null && encumbranceLineList.size() == 1) {
                    for (EfinBudgetManencumlines encumLine : encumbranceLineList) {

                      encumLine.setAPPAmt(encumLine.getAPPAmt().add(invLinenetAmt));
                      // encumLine.setENCDecrease(encumLine.getENCDecrease().subtract(invLinenetAmt));
                      OBDal.getInstance().save(encumLine);

                      final OBQuery<EfinBudManencumRev> modification = OBDal.getInstance()
                          .createQuery(EfinBudManencumRev.class,
                              "description='" + OBMessageUtils.messageBD("Efin_encumbrance_desc")
                                  .replace("@", invoice.getDocumentNo()) + "'");

                      if (modification.list().size() > 0) {
                        for (EfinBudManencumRev dim : modification.list()) {
                          OBDal.getInstance().remove(dim);
                        }

                      }

                      invoiceRefList = encumLine.getEfinManualEncumInvoiceList().stream()
                          .filter(
                              a -> a.getInvoice().getId().equals(line.getEFINPrepayment().getId()))
                          .collect(Collectors.toList());

                      if (invoiceRefList != null) {
                        EfinManualEncumInvoice invoiceRef = invoiceRefList.get(0);
                        invoiceRef.setInvamount(invoiceRef.getInvamount().add(invLinenetAmt));
                        OBDal.getInstance().save(invoiceRef);

                      }
                    }
                  }
                  OBDal.getInstance().flush();
                }
              }
            }
          }
        }
        OBDal.getInstance().flush();
      }
    }

    return null;
  }

}
