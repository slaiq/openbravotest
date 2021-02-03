package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;

/**
 * @author qualian
 *
 */

public class POContractSummaryTotPOChange extends SimpleCallout {
  private static final String infoMessage = "Escm_DiscountChanged_AfterTax";

  /**
   * Callout to update receive Type based on Contract category in PO Header
   */

  private static Logger log = Logger.getLogger(POContractSummaryTotPOChange.class);
  Integer roundoffConst = 2;

  private static final long serialVersionUID = 1L;

  @Override
  @SuppressWarnings("unchecked")
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpemEscmTotPoChangeType = vars.getStringParameter("inpemEscmTotPoChangeType");
    String inpemEscmTotPoChangeValue = vars.getStringParameter("inpemEscmTotPoChangeValue");
    // String inpemEscmTotPoChangeFactor = vars.getStringParameter("inpemEscmTotPoChangeFactor");
    String inpemEscmContactType = vars.getStringParameter("inpemEscmContactType");
    // String inpemEscmIstax = vars.getStringParameter("inpemEscmIstax");
    String inpemEscmLineTaxamt = vars.getNumericParameter("inpemEscmLineTaxamt");
    String strCOrderId = vars.getStringParameter("inpcOrderId");
    String totalTaxAmount = vars.getNumericParameter("inpemEscmTotalTaxamt");
    Query query = null;
    List<Object> refLookUpLnList = new ArrayList<>();
    BigDecimal changeValue = (StringUtils.isNotEmpty(inpemEscmTotPoChangeValue)
        && inpemEscmTotPoChangeValue != "") ? new BigDecimal(inpemEscmTotPoChangeValue)
            : BigDecimal.ZERO;

    String inpContactType = vars.getStringParameter("inpemEscmContactType");
    String parsedMessage = null;

    try {
      OBContext.setAdminMode();
      log.debug("inpLastFieldChanged>" + inpLastFieldChanged);

      if (inpLastFieldChanged.equals("inpemEscmTotPoChangeValue")
          || inpLastFieldChanged.equals("inpemEscmTotPoChangeType")
          || inpLastFieldChanged.equals("inpemEscmTotPoChangeFactor")) {
        if (new BigDecimal(totalTaxAmount).compareTo(BigDecimal.ZERO) > 0) {
          parsedMessage = Utility.messageBD(this, infoMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("INFO", parsedMessage);
        }

      }
      // if change type as amount then round off the value to 2 decimal
      if (inpLastFieldChanged.equals("inpemEscmTotPoChangeValue")) {
        String amountChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
            "01");
        if (amountChangeType.equals(inpemEscmTotPoChangeType)) {
          info.addResult("inpemEscmTotPoChangeValue",
              changeValue.setScale(roundoffConst, RoundingMode.HALF_UP));
        }
      }
      if (inpLastFieldChanged.equals("inpemEscmTotPoChangeType")) {
        String amountChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
            "01");
        if (amountChangeType.equals(inpemEscmTotPoChangeType)) {
          if (changeValue.compareTo(new BigDecimal(0)) > 0) {
            info.addResult("inpemEscmTotPoChangeValue",
                changeValue.setScale(roundoffConst, RoundingMode.HALF_UP));
          }
        }
      }

      // Change receive Type based on Contract category
      if (inpLastFieldChanged.equals("inpemEscmContactType")) {

        String sqlString = "select escm_deflookups_typeln_id from escm_deflookups_typeln where escm_deflookups_typeln_id in "
            + " (Select receive_type from escm_deflookups_typeln where escm_deflookups_typeln_id in "
            + " (select escm_deflookups_typeln_id from escm_deflookups_typeln where escm_deflookups_typeln_id =:contractCategory))"
            + " and escm_deflookups_type_id in ( select  escm_deflookups_type_id"
            + " from escm_deflookups_type where reference = 'RT' and isactive='Y') and value = 'AMT' ";
        query = OBDal.getInstance().getSession().createSQLQuery(sqlString);
        query.setParameter("contractCategory", inpemEscmContactType);
        refLookUpLnList = query.list();
        if (refLookUpLnList.size() > 0) {
          info.addResult("inpemEscmReceivetype", "AMT");
        } else {
          info.addResult("inpemEscmReceivetype", "QTY");
        }

        // Update Payment Schedule based on the contract category configuration
        ESCMDefLookupsTypeLn reflookuplnObj = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            inpContactType);
        if (reflookuplnObj != null) {
          info.addResult("inpemEscmIspaymentschedule", reflookuplnObj.isPaymentschedule());
        } else {
          info.addResult("inpemEscmIspaymentschedule", false);
        }
        ESCMDefLookupsTypeLn cntrctCatlookup = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            vars.getStringParameter("inpemEscmContactType"));
        if (cntrctCatlookup == null
            || (cntrctCatlookup != null && !cntrctCatlookup.isMaintenancecontract())) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Maintenance_Cntrct_No').setValue('')");
        }
        if (cntrctCatlookup != null && !cntrctCatlookup.isMaintenancecontract()) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Maintenance_Cntrct_No').setValue('')");
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Maintenance_Project').setValue('')");
        }

      }

      // if (inpLastFieldChanged.equals("inpemEscmIstax")) {
      // if (inpemEscmIstax.equals("N")) {
      // /*
      // * info.addResult("JSEXECUTE",
      // * "form.getFieldFromColumnName('EM_Escm_Tax_Method_ID').setValue('')");
      // */
      // }
      // }

      if (inpLastFieldChanged.equals("inpemEscmLineTaxamt")) {
        if (StringUtils.isEmpty(inpemEscmLineTaxamt)) {
          info.addResult("inpemEscmLineTaxamt", BigDecimal.ZERO);
        }
        Order order = OBDal.getInstance().get(Order.class, strCOrderId);
        order.setEscmCalculateTaxlines(false);
        OBDal.getInstance().save(order);
        OBDal.getInstance().flush();

      }
      // Get Line Parent Percent
      // String lineParentPercentChangeType = POContractSummaryTotPOChangeDAO
      // .getPOChangeLookUpId("TPOCHGTYP", "03");
      // Get tanfeez contact type
      // String tanfeezContactType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("POCONTYP",
      // "01");
      // // When POChnage type is changed check for Line Parent Percentage and set Contact Type and
      // PO
      // // Change factor
      // if (inpLastFieldChanged.equals("inpemEscmTotPoChangeType")) {
      // if (inpemEscmTotPoChangeType.equals(lineParentPercentChangeType)) {
      // info.addResult("inpemEscmContactType", tanfeezContactType);
      // info.addResult("JSEXECUTE",
      // "form.getFieldFromColumnName('EM_Escm_Tot_Po_Change_Factor').setValue('')");
      // } else {
      // if (inpemEscmContactType.equals(tanfeezContactType)) {
      // info.addResult("JSEXECUTE",
      // "form.getFieldFromColumnName('EM_Escm_Contact_Type').setValue('')");
      // }
      // }
      // }
      // // When Contact type is changed check for Tanfeez and set PO Change Type and Contact Type
      // if (inpLastFieldChanged.equals("inpemEscmContactType")) {
      // if (inpemEscmContactType.equals(tanfeezContactType)) {
      // info.addResult("inpemEscmTotPoChangeType", lineParentPercentChangeType);
      // info.addResult("JSEXECUTE",
      // "form.getFieldFromColumnName('EM_Escm_Tot_Po_Change_Factor').setValue('')");
      // } else {
      // if (inpemEscmTotPoChangeType.equals(lineParentPercentChangeType)) {
      // info.addResult("JSEXECUTE",
      // "form.getFieldFromColumnName('EM_Escm_Tot_Po_Change_Type').setValue('')");
      // }
      // }
      // }
      // // When total po change is line parent percentage then make po change factor to empty
      // if (inpemEscmTotPoChangeType.equals(lineParentPercentChangeType)) {
      // info.addResult("JSEXECUTE",
      // "form.getFieldFromColumnName('EM_Escm_Tot_Po_Change_Factor').setValue('')");
      // }
    } catch (Exception e) {
      log.debug("Exception in PurorderContractSummaryCallout:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
