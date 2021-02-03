package sa.elm.ob.scm.ad_callouts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.UtilityDAO;

public class PurchaseAgreementCallout extends SimpleCallout {

  /**
   * Callout to update Document Number for Purchase Order and Contract Summary Header
   */

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpIspurchaseagreement = vars.getStringParameter("inpemEscmIspurchaseagreement");
    String inpcDoctypetargetId = vars.getStringParameter("inpcDoctypetargetId");
    String inpemEscmPurchaseagreement = vars.getStringParameter("inpemEscmPurchaseagreement");
    String inpOrdertype = vars.getStringParameter("inpemEscmOrdertype");
    String inpadOrgId = vars.getStringParameter("inpadOrgId");
    String inpApprovalDateH = vars.getStringParameter("inpemEscmApprovalDateHijiri");
    String inpMofDateH = vars.getStringParameter("inpemEscmMofDateHijiri");
    String inpReceiveMofDateH = vars.getStringParameter("inpemEscmReceiveMofHijiri");
    String inpApprovalDateG = vars.getStringParameter("inpemEscmApprovalDateGreg");
    String inpMofDateG = vars.getStringParameter("inpemEscmMofDateGreg");
    String inpReceiveMofDateG = vars.getStringParameter("inpemEscmReceiveMofGreg");
    DocumentType docType = null;
    try {
      OBContext.setAdminMode();
      PreparedStatement st = null;
      ResultSet rs = null;

      DateFormat dateyearFormat = new SimpleDateFormat("dd-MM-yyyy");
      DateFormat dateyearForm = new SimpleDateFormat("yyyy-MM-dd");

      if (inpLastFieldChanged.equals("inpemEscmIspurchaseagreement")) {

        docType = OBDal.getInstance().get(DocumentType.class, inpcDoctypetargetId);
        Long seqno = docType.getDocumentSequence().getNextAssignedNumber();

        if (inpIspurchaseagreement.equals("Y")) {
          info.addResult("inpdocumentno", "<>");
        } else {
          info.addResult("inpdocumentno", seqno);
        }

      }
      if (inpLastFieldChanged.equals("inpemEscmPurchaseagreement")) {
        if (StringUtils.isNotEmpty(inpemEscmPurchaseagreement)) {
          Order objOrder = OBDal.getInstance().get(Order.class, inpemEscmPurchaseagreement);
          info.addResult("inpemEscmContactType",
              objOrder.getEscmContactType() == null ? null : objOrder.getEscmContactType().getId());
          info.addResult("inpemEscmReceivetype", objOrder.getEscmReceivetype());
          info.addResult("inpemEscmIstax", objOrder.isEscmIstax());
          info.addResult("inpemEscmTaxMethodId",
              objOrder.getEscmTaxMethod() == null ? null : objOrder.getEscmTaxMethod().getId());
          if (objOrder.getEscmProposalmgmt() != null) {
            info.addResult("inpemEscmProposalmgmtId", objOrder.getEscmProposalmgmt().getId());
          } else {
            info.addResult("inpemEscmProposalmgmtId", null);
          }
        } else {
          info.addResult("inpemEscmContactType", null);
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Receivetype').setValue('QTY')");
          info.addResult("inpemEscmIstax", false);
          info.addResult("inpemEscmTaxMethodId", null);
        }
        String businessPartner = PurchaseAgreementCalloutDAO
            .getBusinessPartner(inpemEscmPurchaseagreement);
        if (businessPartner != null) {
          info.addResult("inpcBpartnerId", businessPartner);
        } else {
          info.addResult("inpcBpartnerId", null);
        }

      }

      if (inpLastFieldChanged.equals("inpemEscmOrdertype")) {
        if (!(inpOrdertype.equals(Constants.PURCHASE_RELEASE))) {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Purchaseagreement').setValue('')");
        }

        EscmPurchaseOrderConfiguration configuration = PurchaseAgreementCalloutDAO
            .checkDocTypeConfig(OBContext.getOBContext().getCurrentClient().getId(), inpadOrgId,
                inpOrdertype);

        if (configuration != null) {
          info.addResult("inpemEscmMotcontperson",
              configuration.getMOTContactPerson() != null
                  ? configuration.getMOTContactPerson().getName()
                  : null);
          info.addResult("inpemEscmMotcontposition", configuration.getMOTContactPosition());
        }
      }
      if (inpLastFieldChanged.equals("inpemEscmMaintenanceProject")) {
        ESCMDefLookupsTypeLn lookup = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            vars.getStringParameter("inpemEscmMaintenanceProject"));
        if (lookup != null) {
          if (lookup.getSearchKey() != null) {
            String Cno = lookup.getSearchKey();
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('EM_Escm_Maintenance_Cntrct_No').setValue(" + Cno
                    + ")");
          }
        } else {
          info.addResult("JSEXECUTE",
              "form.getFieldFromColumnName('EM_Escm_Maintenance_Cntrct_No').setValue('')");
        }
      }
      if (inpLastFieldChanged.equals("inpemEscmMofDateHijiri")) {
        if (StringUtils.isNotEmpty(inpMofDateH)) {
          info.addResult("inpemEscmMofDateGreg", UtilityDAO.convertToGregTimeStamp(inpMofDateH));
        } else {
          info.addResult("inpemEscmMofDateGreg", null);
        }
      }
      if (inpLastFieldChanged.equals("inpemEscmMofDateGreg")) {
        if (StringUtils.isNotEmpty(inpMofDateG))
          info.addResult("inpemEscmMofDateHijiri", UtilityDAO
              .convertToHijriDate(dateyearForm.format(dateyearFormat.parse(inpMofDateG))));
        else
          info.addResult("inpemEscmMofDateHijiri", null);
      }
      if (inpLastFieldChanged.equals("inpemEscmReceiveMofHijiri")) {
        if (StringUtils.isNotEmpty(inpReceiveMofDateH))
          info.addResult("inpemEscmReceiveMofGreg",
              UtilityDAO.convertToGregorian_tochar(inpReceiveMofDateH));
        else
          info.addResult("inpemEscmReceiveMofGreg", null);
      }
      if (inpLastFieldChanged.equals("inpemEscmReceiveMofGreg")) {
        if (StringUtils.isNotEmpty(inpReceiveMofDateG))
          info.addResult("inpemEscmReceiveMofHijiri", UtilityDAO
              .convertToHijriDate(dateyearForm.format(dateyearFormat.parse(inpReceiveMofDateG))));
        else
          info.addResult("inpemEscmReceiveMofHijiri", null);
      }

    } catch (Exception e) {
      log4j.error("Exception in  PurchaseAgreementCallout :", e);
      info.addResult("ERROR", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
