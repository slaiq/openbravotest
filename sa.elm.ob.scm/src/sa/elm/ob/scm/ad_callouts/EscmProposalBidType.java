package sa.elm.ob.scm.ad_callouts;

import java.util.Date;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.ad_callouts.BudgetAdjustmentCallout;
import sa.elm.ob.finance.ad_callouts.dao.RequisitionHeaderCalloutDAO;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.ad_callouts.dao.ProposalManagementDAO;

public class EscmProposalBidType extends SimpleCallout {
  private static Logger log = Logger.getLogger(EscmProposalBidType.class);
  private static String strProposalWindowId = "CAF2D3EEF3B241018C8F65E8F877B29F";
  /**
   * Callout to update the Bidtype
   */
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strBidID = vars.getStringParameter("inpescmBidmgmtId");
    String strOrgID = vars.getStringParameter("inpadOrgId");
    String effectiveFrom = vars.getStringParameter("inpeffectivefrom");
    String strSupplierID = vars.getStringParameter("inpsupplier");
    String clientid = vars.getStringParameter("inpadClientId");
    String inpadClientId = vars.getStringParameter("inpadClientId");
    // String encummethod = vars.getStringParameter("inpemEfinEncumMethod");
    String encumId = vars.getStringParameter("inpemEfinEncumbranceId");
    String inpistax = vars.getStringParameter("inpistax");
    String inpadRoleId = vars.getRole();
    String strSecondSupplierId = vars.getStringParameter("inpsecondsupplier");
    String jscode = "";
    String selectedIban = vars.getStringParameter("inpissecondsupplier");
    String budgInitialId = null;
    try {

      if (inpLastFieldChanged.equals("inpadOrgId")) {
        Date endDate = new Date();
        // getting budget initial id based on transaction date
        budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(endDate,
            inpadClientId, strProposalWindowId);
        if (budgInitialId != null)
          info.addResult("inpemEfinBudgetinitialId", budgInitialId);
        else
          info.addResult("inpemEfinBudgetinitialId", null);
      }

      if (inpLastFieldChanged.equals("inpescmBidmgmtId")) {
        if (strBidID != null && !strBidID.equals("")) {
          EscmBidMgmt bidMgmt = OBDal.getInstance().get(EscmBidMgmt.class, strBidID);
          info.addResult("inpbidtype", bidMgmt.getBidtype());
          info.addResult("inpapprovedbudget", bidMgmt.getApprovedbudget());
          info.addResult("inpbidname", bidMgmt.getBidname());
          if (!bidMgmt.getBidtype().equals("DR"))
            info.addResult("inpenvelopcounts", "1");
          else
            info.addResult("inpenvelopcounts", "0");
          if (bidMgmt.getContractType() != null) {
            info.addResult("inpcontractType", bidMgmt.getContractType().getId());
            info.addResult("JSEXECUTE",
                "form.view.viewForm.getItem('contractType').setDisabled(true)");
          }
        } else {
          info.addResult("inpbidtype", "");
          info.addResult("inpapprovedbudget", "");
          info.addResult("inpbidname", "");
          info.addResult("inpenvelopcounts", "0");
          info.addResult("inpcontractType", null);
          info.addResult("JSEXECUTE",
              "form.view.viewForm.getItem('contractType').setDisabled(false)");
        }
        // Effective from Date in Proposal management screen will be default to Open Envelop day
        // based
        // on selected bidno,if unselect the bidno then current date
        String Date = ProposalManagementDAO.getBidOpenenvelopdayOrSystemDate(strBidID);
        info.addResult("inpeffectivefrom", Date);
      }
      if (inpLastFieldChanged.equals("inpadOrgId")) {
        Organization org = OBDal.getInstance().get(Organization.class, strOrgID);
        Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
        info.addResult("inpcCurrencyId",
            org.getCurrency() == null ? objCurrency.getId() : org.getCurrency().getId());
      }
      if (inpLastFieldChanged.equals("inpsupplier")) {
        BusinessPartner supplier = OBDal.getInstance().get(BusinessPartner.class, strSupplierID);
        Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
        info.addResult("inpcCurrencyId",
            supplier.getCurrency() == null
                ? (supplier.getOrganization().getCurrency() == null ? objCurrency.getId()
                    : supplier.getOrganization().getCurrency().getId())
                : supplier.getCurrency().getId());
        EscmBidMgmt bidMgmt = OBDal.getInstance().get(EscmBidMgmt.class, strBidID);
        if (bidMgmt != null && bidMgmt.getContractType() != null) {
          info.addResult("inpcontractType", bidMgmt.getContractType().getId());
          info.addResult("JSEXECUTE",
              "form.view.viewForm.getItem('contractType').setDisabled(true)");
        }
      }
      if (inpLastFieldChanged.equals("inpeffectivefrom")) {
        String EffectiveTo = ProposalManagementDAO.getDateforEffectiveTo(effectiveFrom, clientid);
        info.addResult("inpeffectiveto", EffectiveTo);
        /*
         * String[] dateParts = effectiveFrom.split("-"); String hijiridate = dateParts[2] +
         * dateParts[1] + dateParts[0]; Query query = null; String strQuery = "", enddate = "";
         * strQuery =
         * "select hijri_date from (select max(hijri_date) as hijri_date from eut_hijri_dates where hijri_date >= '"
         * + hijiridate + "' and ad_client_id='" + vars.getClient() +
         * "' group by hijri_date order by hijri_date limit 90) dual order by hijri_date desc limit 1"
         * ; query = OBDal.getInstance().getSession().createSQLQuery(strQuery); List<Object>
         * querylist = query.list(); if (query != null && querylist.size() > 0) { Object row =
         * querylist.get(0); enddate = (String) row; enddate = enddate.substring(6, 8) + "-" +
         * enddate.substring(4, 6) + "-" + enddate.substring(0, 4); info.addResult("inpeffectiveto",
         * enddate); } else { info.addResult("inpeffectiveto", effectiveFrom); }
         */
      }

      // while changing encumbrance method remove value from encumbrance and UniqueCode field
      if (inpLastFieldChanged.equals("inpemEfinEncumMethod")) {
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('EM_Efin_Encumbrance_ID').setValue('')");
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('')");
      }
      // set uniquecode while changing encumbrance
      if (inpLastFieldChanged.equals("inpemEfinEncumbranceId")) {
        // getting budget initial id based on transaction date
        if (encumId != null && !encumId.equals("")) {
          EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class, encumId);
          info.addResult("inpemEscmManualEncumNo", encum.getDocumentNo());
          String uniqueCode = RequisitionHeaderCalloutDAO.getUniqueCode(encumId, inpadClientId,
              inpadRoleId);
          if (uniqueCode != null) {
            jscode = "if(form.view.getParentId()!=null) { form.view.parentView.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('"
                + uniqueCode
                + "');} else {form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('"
                + uniqueCode + "'); }";
            jscode += "form.doChangeFICCall('EM_Efin_C_Validcombination_ID');";
            info.addResult("JSEXECUTE", jscode);
          } else
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('')");
        } else {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').setValue('')");
        }
      }
      if (inpLastFieldChanged.equals("inpistax")) {
        if (inpistax.equals("N")) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('Efin_Tax_Method_ID').setValue('')");
        }
      }
      // set the subcontractor field while change the joint venture supplier
      if (inpLastFieldChanged.equals("inpsecondsupplier")) {
        BusinessPartner second_supplier = OBDal.getInstance().get(BusinessPartner.class,
            strSecondSupplierId);
        if (second_supplier != null) {
          info.addResult("inpsubcontractors", second_supplier.getName());
          // set the second branch name
          OBQuery<Location> bpLocation = OBDal.getInstance().createQuery(Location.class,
              "as e where e.businessPartner.id =:supplier");
          bpLocation.setNamedParameter("supplier", strSecondSupplierId);
          if (bpLocation != null && bpLocation.list().size() > 0) {
            jscode = "form.getFieldFromColumnName('Second_Branchname').setValue('"
                + bpLocation.list().get(0).getId() + "');";
            jscode += "form.doChangeFICCall('Second_Branchname');";
            info.addResult("JSEXECUTE", jscode);
          }

        } else {
          info.addResult("inpsubcontractors", null);
          info.addResult("inpissecondsupplier", false);
        }

      }
      // if second supplier iban as false then set the iban as empty
      if (inpLastFieldChanged.equals("inpissecondsupplier")) {
        if (selectedIban.equals("N")) {
          info.addResult("JSEXECUTE", "form.getFieldFromColumnName('Iban').setValue('')");
        }
      }
    } catch (Exception e) {
      log.error("Exception in proposal bidtype callout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
