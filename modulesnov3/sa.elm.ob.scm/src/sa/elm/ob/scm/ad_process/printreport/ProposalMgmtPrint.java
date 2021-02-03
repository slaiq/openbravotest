package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.OrganizationInformation;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

public class ProposalMgmtPrint extends GenerateJasperPrint {
  private static final Logger log4j = Logger.getLogger(PODeliveryPrint.class);

  private static ProposalMgmtPrint proposalMgmtPrint;

  public static ProposalMgmtPrint getInstance() {
    if (proposalMgmtPrint == null) {
      proposalMgmtPrint = new ProposalMgmtPrint();
    }
    return proposalMgmtPrint;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    try {
      OBContext.setAdminMode();

      // proposal mgmt
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      if (action.equals("")) {
        EscmProposalMgmt prpmgCount = OBDal.getInstance().get(EscmProposalMgmt.class,
            paramObject.getString("inpRecordId"));
        log4j.info("stat>>" + prpmgCount.getProposalstatus());
        if (prpmgCount.getProposalstatus().equals("AWD")
            && prpmgCount.getProposalappstatus().equals("APP")) {
          request.setAttribute("pageType", paramObject.getString("pageType"));
          request.setAttribute("inpTabId", paramObject.getString("tabId"));
          request.setAttribute("inpWindowID", paramObject.getString("inpWindowID"));
          request.setAttribute("inpRecordId", paramObject.getString("inpRecordId"));
          // get letter tab record count
          int lettercount = PrintReportDAO.getlettercount(paramObject.getString("inpRecordId"));
          request.setAttribute("Count", lettercount);
          isJasper = Boolean.FALSE;

          strJspPage = "../web/sa.elm.ob.scm/jsp/printreport/ProposalMgmtReport.jsp";

        } else if (prpmgCount.getProposalstatus().equals("SUB")) {
          designParameters.put("Proposalid", paramObject.getString("inpRecordId"));
          strReportName = paramObject.getString("reportDir")
              + "ProposalReceiptConfirmation/ProposalReceiptConfirmReport.jrxml";
          strFileName = "ProposalReceiptConfirmation" + " " + " " + hijriDate;

          isJasper = Boolean.TRUE;
          connection = (Connection) paramObject.get("connection");

          designParameters.put("BASE_DESIGN", paramObject.getString("basedesign"));
        }

      } else {
        PrintReportDAO dao = new PrintReportDAO();
        VariablesSecureApp vars = new VariablesSecureApp(request);
        // VariablesSecureApp vars = null;
        // vars = new VariablesSecureApp(request);
        // final String basedesign = getBaseDesignPath(vars.getLanguage());
        // final String reportDir = basedesign + "/sa/elm/ob/scm/ad_reports/";
        String reportDir = paramObject.getString("reportDir");
        isJasper = Boolean.TRUE;
        connection = (Connection) paramObject.get("connection");

        designParameters.put("BASE_DESIGN", paramObject.getString("basedesign"));
        String imageFlag = "N";
        String inpPropMgmtRpt = request.getParameter("inpPropMgmtRpt");
        String inpRecordId = request.getParameter("inpRecordId");
        request.getSession().setAttribute("inpParamCost2", request.getParameter("inpParamCost2"));
        request.getSession().setAttribute("inpParamCost1", request.getParameter("inpParamCost1"));
        request.getSession().setAttribute("inpParamCost3", request.getParameter("inpParamCost3"));
        request.getSession().setAttribute("inpParamCost4", request.getParameter("inpParamCost4"));
        request.getSession().setAttribute("inpParamCost5", request.getParameter("inpParamCost5"));
        request.getSession().setAttribute("inpParam1", request.getParameter("inpParam1"));
        request.getSession().setAttribute("inpParam2", request.getParameter("inpParam2"));
        request.getSession().setAttribute("inpParam3", request.getParameter("inpParam3"));
        request.getSession().setAttribute("inpParam4", request.getParameter("inpParam4"));
        request.getSession().setAttribute("inpParam5", request.getParameter("inpParam5"));
        request.getSession().setAttribute("inpParam6", request.getParameter("inpParam6"));
        EscmProposalMgmt prpmgCount = OBDal.getInstance().get(EscmProposalMgmt.class,
            request.getParameter("inpRecordId"));
        log4j.debug(request.getParameter("inpRecordId"));

        OrganizationInformation objInfo = prpmgCount.getOrganization()
            .getOrganizationInformationList().get(0);
        // check org have image
        if (objInfo != null) {
          if (objInfo.getYourCompanyDocumentImage() != null) {
            imageFlag = "Y";
          }
        }
        designParameters.put("inpImageFlag", imageFlag);
        designParameters.put("inpOrgId", prpmgCount.getOrganization().getId());
        designParameters.put("inpProposalMgmtId", inpRecordId);
        log4j.info("inpPropMgmtRpt>" + inpPropMgmtRpt);
        if (inpPropMgmtRpt.equals("FRIR")) {
          designParameters.put("inpParam1", request.getParameter("inpParam1"));
          designParameters.put("inpParam2", request.getParameter("inpParam2"));
          designParameters.put("inpParam3", request.getParameter("inpParam3"));
          designParameters.put("inpParam4", request.getParameter("inpParam4"));
          designParameters.put("inpParam5", request.getParameter("inpParam5"));
          designParameters.put("inpParam6", request.getParameter("inpParam6"));

          strReportName = reportDir + "fundreservationinforeq/FundReservationInfoReq.jrxml";
          strFileName = "FundReservationInfoReq" + " " + " " + hijriDate;
        } else if (inpPropMgmtRpt.equals("FRCR")) {
          designParameters.put("inpParamCost1", request.getParameter("inpParamCost1"));
          designParameters.put("inpParamCost2", request.getParameter("inpParamCost2"));
          designParameters.put("inpParamCost3", request.getParameter("inpParamCost3"));
          designParameters.put("inpParamCost4", request.getParameter("inpParamCost4"));
          designParameters.put("inpParamCost5", request.getParameter("inpParamCost5"));

          strReportName = reportDir + "fundreservationcost/FundReservationCost.jrxml";
          strFileName = "FundReservationCostReq" + " " + " " + hijriDate;
        } else if (inpPropMgmtRpt.equals("AWDLTR")) {
          String inpAwardLookUps = request.getParameter("inpAwardLookUps");
          String inpOutput = request.getParameter("inpOutput");
          // List<Map<String, String>> lkpUpsLs = null;
          ArrayList<PrintReportVO> lkpUpsLs = null;
          PrintReportVO lookUpVO = null;

          if (inpAwardLookUps != null && !inpAwardLookUps.equals("")) {
            // lkpUpsLs = new ArrayList<Map<String, String>>();
            lkpUpsLs = new ArrayList<PrintReportVO>();
            JSONObject json = new JSONObject(inpAwardLookUps);
            JSONArray arr = json.getJSONArray("Lookup");

            for (int i = 0; i < arr.length(); i++) {
              JSONObject jsob = arr.getJSONObject(i);
              /*
               * HashMap<String,String> hm=new HashMap<String,String>(); hm.put("SeqNo",
               * jsob.getString("SeqNo")); hm.put("AwardLookUp", jsob.getString("AwardLookUp"));
               * lkpUpsLs.add(hm);
               */
              lookUpVO = new PrintReportVO();
              lookUpVO.setSeqNo(jsob.getString("SeqNo"));
              lookUpVO.setAwardLookUp(jsob.getString("AwardLookUp"));
              lkpUpsLs.add(lookUpVO);
            }
          }

          BeanComparator fieldComparator = new BeanComparator("seqNo");
          if (fieldComparator != null) {
            Collections.sort(lkpUpsLs, fieldComparator);
          }
          designParameters.put("inpPhotoCopiesList", lkpUpsLs);

          strReportName = reportDir + "awardletter/AwardLetter.jrxml";
          strFileName = "AwardLetter" + " " + " " + hijriDate;
          if (inpOutput.equals("FNL")) {
            String sequence = Utility.getTransactionSequence(prpmgCount.getOrganization().getId(),
                "PMGLTR");
            if (!sequence.equals("false")) {
              dao.insertLetter(vars.getUser(), vars.getClient(),
                  prpmgCount.getOrganization().getId(), inpRecordId, strFileName, inpPropMgmtRpt,
                  sequence);
            }
          }

        } else if (inpPropMgmtRpt.equals("REMLTR")) {
          ESCMDefLookupsTypeLn reflookup = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
              request.getParameter("inpParamReminder2"));
          String Subjectpara2value = reflookup.getSearchKey();
          String Subjectpara2name = reflookup.getCommercialName();
          String para2description = reflookup.getDescription();
          designParameters.put("inpParamReminder2value", Subjectpara2value);
          designParameters.put("inpParamReminder2name", Subjectpara2name);
          designParameters.put("inpconditionalbullets", para2description);
          String inpAwardLookUps = request.getParameter("inpAwardLookUps");
          String inpOutput = request.getParameter("inpParamReminder1");
          ArrayList<PrintReportVO> lkpUpsLs = null;
          PrintReportVO lookUpVO = null;

          if (inpAwardLookUps != null && !inpAwardLookUps.equals("")) {
            lkpUpsLs = new ArrayList<PrintReportVO>();
            JSONObject json = new JSONObject(inpAwardLookUps);
            JSONArray arr = json.getJSONArray("Lookup");

            for (int i = 0; i < arr.length(); i++) {
              JSONObject jsob = arr.getJSONObject(i);
              /*
               * HashMap<String, String> hm = new HashMap<String, String>(); hm.put("SeqNo",
               * jsob.getString("SeqNo")); hm.put("AwardLookUp", jsob.getString("AwardLookUp"));
               * lkpUpsLs.add(hm);
               */
              lookUpVO = new PrintReportVO();
              lookUpVO.setSeqNo(jsob.getString("SeqNo"));
              lookUpVO.setAwardLookUp(jsob.getString("AwardLookUp"));
              lkpUpsLs.add(lookUpVO);
            }
          }
          // sorting
          BeanComparator fieldComparator = new BeanComparator("seqNo");
          if (fieldComparator != null) {
            Collections.sort(lkpUpsLs, fieldComparator);
          }

          designParameters.put("inpPhotoCopiesList", lkpUpsLs);
          strReportName = reportDir + "ReminderLetterReport/ReminderLetterReport.jrxml";
          strFileName = "ReminderLetterReport" + " " + " " + hijriDate;
          // insert record in letter tab
          if (inpOutput.equals("FNL")) {
            // Organization org = prpmgCount.getOrganization();
            String sequence = Utility.getTransactionSequence(prpmgCount.getOrganization().getId(),
                "PMGLTR");
            if (!sequence.equals("false")) {
              dao.insertLetter(vars.getUser(), vars.getClient(),
                  prpmgCount.getOrganization().getId(), inpRecordId, strFileName, inpPropMgmtRpt,
                  sequence);
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getReportVariables method in ProposalMgmtPrint", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
