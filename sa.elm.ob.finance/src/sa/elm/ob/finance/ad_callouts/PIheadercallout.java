package sa.elm.ob.finance.ad_callouts;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.CallStoredProcedure;

import sa.elm.ob.finance.EFIN_TaxMethod;
import sa.elm.ob.finance.EfinBudgetManencum;

public class PIheadercallout extends SimpleCallout {

  /**
  
   */
  private static final long serialVersionUID = 1L;
  final private static Logger log = Logger.getLogger(PIheadercallout.class);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    try {
      VariablesSecureApp vars = info.vars;

      String inpenumtype = vars.getStringParameter("inpemEfinEncumtype");
      String inpManualEncumbranceId = vars.getStringParameter("inpemEfinManualencumbranceId");
      String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
      String inpInvText = vars.getStringParameter("inpemEfinInvoicetypeTxt");
      String inpOrder = vars.getStringParameter("inpemEfinCOrderId");
      String inpdateacct = vars.getStringParameter("inpdateacct");
      String jscode = "";

      log.debug("inpenumtype:" + inpenumtype);
      log.debug("inpLastFieldChanged:" + inpLastFieldChanged);
      log.debug("inpemEfinManualencumbranceId:" + inpManualEncumbranceId);
      log.debug("inpcDoctypeId:" + inpManualEncumbranceId);

      if (inpLastFieldChanged.equals("inpemEfinEncumtype")
          || inpLastFieldChanged.equals("inpemEfinEncumType")) {
        if (!inpenumtype.equals("M") && inpManualEncumbranceId != null
            && inpManualEncumbranceId != "") {
          log.debug("inpemEfinManualencumbranceId:"
              + vars.getStringParameter("inpemEfinManualencumbranceId"));
          info.addResult("inpemEfinManualencumbranceId", null);
        }
        String strEncumbranceType = vars.getStringParameter("inpemEfinEncumType");
        if (inpenumtype.equals("A") || "AAE".equals(strEncumbranceType)) {
          info.addResult("inpemEfinBudgetType", "F");
        } else {
          info.addResult("inpemEfinBudgetType", "");
        }
      }

      if (inpLastFieldChanged.equals("inpemEfinCOrderId") && inpInvText.equals("POM")) {
        if (inpOrder != null && inpOrder != "") {
          Order order = OBDal.getInstance().get(Order.class, inpOrder);
          EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
              order.getEfinBudgetManencum().getId());
          info.addResult("inpemEfinBudgetType", encum.getSalesCampaign().getEfinBudgettype());
          info.addResult("inpemEfinEncumtype", encum.getEncumMethod());
          info.addResult("inpemEfinEncumType", encum.getEncumType());
          info.addResult("inpemEfinManualencumbranceId", encum.getId());
          info.addResult("inpemEfinManualencumbranceId", encum.getId());
          if (order.getEscmSecondsupplier() != null && order.getEscmSecondIban() != null) {
            jscode = "form.getFieldFromColumnName('EM_Efin_Iban').setValue('"
                + order.getEscmSecondIban().getId() + "');";
            jscode += "form.doChangeFICCall('EM_Efin_Iban');";
            info.addResult("JSEXECUTE", jscode);
          } else {
            if (order.getEscmIban() != null) {
              jscode = "form.getFieldFromColumnName('EM_Efin_Iban').setValue('"
                  + order.getEscmIban().getId() + "');";
              jscode += "form.doChangeFICCall('EM_Efin_Iban');";
              info.addResult("JSEXECUTE", jscode);
            } else {
              info.addResult("JSEXECUTE",
                  "form.getFieldFromColumnName('EM_Efin_Iban').setValue('');");
            }
          }

          // Set tax based on order date
          final List<Object> parameters = new ArrayList<Object>();
          parameters.add(order.getId());
          parameters.add(null);
          parameters.add(inpdateacct);

          String taxMethodId = (String) CallStoredProcedure.getInstance()
              .call("Efin_gettaxbasedonorder", parameters, null);

          EFIN_TaxMethod taxMethod = order.getEscmTaxMethod();

          if (StringUtils.isNotBlank(taxMethodId)) {
            EFIN_TaxMethod tax = OBDal.getInstance().get(EFIN_TaxMethod.class, taxMethodId);
            if (tax != null) {
              taxMethod = tax;
            }
          }

          if (taxMethod != null) {
            info.addResult("inpemEfinIstax", order.isEscmIstax());
            jscode = "form.getFieldFromColumnName('EM_Efin_Tax_Method_ID').setValue('"
                + taxMethod.getId() + "');";
            info.addResult("JSEXECUTE", jscode);

            info.addResult("inpemEfinTaxMethodId", taxMethod.getId());
            if (taxMethod.isActive() && taxMethod.getValidToDate() == null) {
              info.addResult("JSEXECUTE",
                  "form.getFieldFromColumnName('EM_Efin_Tax_Method_ID').disable()");
              info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EM_Efin_Istax').disable()");
            }
            if (taxMethod.isActive() && taxMethod.getValidToDate() != null)
              info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EM_Efin_Istax').enable()");
          } else {
            info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EM_Efin_Istax').enable()");
          }
          // Set attachment reference in Invoice only if order has reference no.
          if (order.getEscmReferenceNo() != null)
            info.addResult("inpemEfinAttachementref", order.getEscmReferenceNo());
          else
            info.addResult("inpemEfinAttachementref", null);
        } else {
          info.addResult("inpemEfinAttachementref", null);
          info.addResult("inpemEfinIstax", "N");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_Tax_Method_ID').setValue('')");
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('EM_Efin_Istax').enable()");
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in PIheadercallout: " + e);
    }
  }
}
