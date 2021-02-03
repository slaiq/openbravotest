package sa.elm.ob.scm.ad_process.printreport;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ESCMDefLookupsType;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ad_reports.contractexecutionorder.ContractExecutionOrder;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

public class POOrderContractSummaryPrint extends GenerateJasperPrint {
  private static Logger log = Logger.getLogger(POOrderContractSummaryPrint.class);

  private static POOrderContractSummaryPrint poOrderContractPrint;

  public static POOrderContractSummaryPrint getInstance() {
    if (poOrderContractPrint == null) {
      poOrderContractPrint = new POOrderContractSummaryPrint();
    }
    return poOrderContractPrint;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {
    PrintReportDAO dao = null;
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();
      dao = new PrintReportDAO();
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));

      if (action.equals("")) {
        log.debug("Enter into");
        log.debug("recordid" + request.getParameter("inpRecordId"));
        Order poheader = OBDal.getInstance().get(Order.class, request.getParameter("inpRecordId"));
        String Docstatus = poheader.getEscmAppstatus().toString();
        String Ordertype = poheader.getEscmOrdertype().toString();
        List<ContractExecutionOrder> groupList = new ArrayList<ContractExecutionOrder>();
        OBCriteria<ESCMDefLookupsType> lookupCriteria = OBDal.getInstance()
            .createCriteria(ESCMDefLookupsType.class);
        lookupCriteria.add(Restrictions.eq(ESCMDefLookupsType.PROPERTY_REFERENCE, "ELS"));
        lookupCriteria.setMaxResults(1);
        ESCMDefLookupsType lookup = (ESCMDefLookupsType) lookupCriteria.uniqueResult();
        if (lookup != null) {
          for (ESCMDefLookupsTypeLn lookupLine : lookup.getESCMDefLookupsTypeLnList()) {
            ContractExecutionOrder co = new ContractExecutionOrder();
            co.setSequenceNo(lookupLine.getSearchKey());
            co.setSubject(lookupLine.getCommercialName());
            co.setId(lookupLine.getId());
            groupList.add(co);
          }
        }
        isJasper = Boolean.FALSE;
        request.setAttribute("pageType", paramObject.getString("pageType"));
        request.setAttribute("inpTabId", paramObject.getString("tabId"));
        request.setAttribute("inpWindowID", paramObject.getString("inpWindowID"));
        request.setAttribute("inpRecordId", paramObject.getString("inpRecordId"));
        request.setAttribute("inpSubject", groupList);
        request.setAttribute("inpReceivingType", paramObject.getString("receiveType"));
        request.setAttribute("inpDocNo", paramObject.getString("documentNo"));
        request.setAttribute("Docstatus", Docstatus);
        request.setAttribute("Ordertype", Ordertype);
        strJspPage = "../web/sa.elm.ob.scm/jsp/printreport/PoOrderandContractsReport.jsp";
      } else {
        isJasper = Boolean.TRUE;
        String reportDir = paramObject.getString("reportDir");
        connection = (Connection) paramObject.get("connection");

        designParameters.put("BASE_DESIGN", paramObject.getString("basedesign"));
        String imageFlag = "N";
        String inpOrderReport = request.getParameter("inpOrderReport");
        log.debug("inpOrderReport" + inpOrderReport);
        String inpContractreport = request.getParameter("inpContractreport");
        log.debug("inpContractreport" + inpContractreport);
        String inpRecordId = request.getParameter("inpRecordId");
        Order ord = OBDal.getInstance().get(Order.class, request.getParameter("inpRecordId"));
        OrganizationInformation objInfo = Utility.getOrgInfo(ord.getOrganization().getId());

        if (objInfo != null) {
          if (objInfo.getYourCompanyDocumentImage() != null) {
            imageFlag = "Y";
          }
        }
        if (inpOrderReport.equals("PCER") || inpContractreport.equals("PCER")) {
          log.info("inpSubject:" + request.getParameter("inpSubject"));
          ESCMDefLookupsTypeLn reflookup = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
              request.getParameter("inpSubject"));
          String Subjectpara2vartext = reflookup.getVartext();
          String para2description = StringUtils.isNotEmpty(reflookup.getDescription())
              ? reflookup.getDescription()
              : reflookup.getCommercialName();
          Order prOrderContract = OBDal.getInstance().get(Order.class,
              request.getParameter("inpRecordId"));
          log.info("recordId" + request.getParameter("inpRecordId"));
          designParameters.put("inpOrgId", prOrderContract.getOrganization().getId());
          designParameters.put("inpPOId", inpRecordId);

          String inpContractLookUps = request.getParameter("inpContractLookUps");
          String inpOutput = request.getParameter("inpOutput");
          // List<Map<String, String>> lkpUpsLs = null;
          ArrayList<PrintReportVO> lkpUpsLs = null;
          PrintReportVO lookUpVO = null;

          if (inpContractLookUps != null && !inpContractLookUps.equals("")) {
            // lkpUpsLs = new ArrayList<Map<String, String>>();
            lkpUpsLs = new ArrayList<PrintReportVO>();
            JSONObject json = new JSONObject(inpContractLookUps);
            JSONArray arr = json.getJSONArray("Lookup");

            for (int i = 0; i < arr.length(); i++) {
              JSONObject jsob = arr.getJSONObject(i);
              lookUpVO = new PrintReportVO();
              lookUpVO.setSeqNo(jsob.getString("SeqNo"));
              lookUpVO.setContractLookUp(jsob.getString("ContractLookup"));
              lkpUpsLs.add(lookUpVO);
            }
          }
          if (para2description.toLowerCase().contains("region")) {
            para2description = para2description.toLowerCase().replace("region",
                (prOrderContract.getEscmCRegion() == null ? " "
                    : prOrderContract.getEscmCRegion().getName()));
          }

          BeanComparator fieldComparator = new BeanComparator("seqNo");
          if (fieldComparator != null) {
            Collections.sort(lkpUpsLs, fieldComparator);
          }
          designParameters.put("inpPhotoCopiesList", lkpUpsLs);
          designParameters.put("inpVarText", Subjectpara2vartext);
          designParameters.put("inpCondText", para2description);
          designParameters.put("inpClientId", prOrderContract.getClient().getId());

          strReportName = reportDir + "contractexecutionorder/contractexecutionorder.jrxml";
          strFileName = "Contract Execution Order";

          if (inpOutput.equals("FNL")) {
            String sequence = Utility.getTransactionSequence(vars.getOrg(), "POLTR");
            if (!sequence.equals("false")) {
              dao.insertPOLetter(vars.getUser(), vars.getClient(), vars.getOrg(), inpRecordId,
                  strFileName, "CONEXEC", sequence);
            }
          }
        } else if (inpOrderReport.equals("DPOR")
            || (inpOrderReport.equals("0") && inpContractreport.equals("0"))) {
          designParameters.put("inpPOId", inpRecordId);
          designParameters.put("inpSubject", request.getParameter("inpSubjectdirect"));
          String inpContractLookUps = request.getParameter("inpContractLookUps");

          // List<Map<String, String>> lkpUpsLs = null;
          ArrayList<PrintReportVO> lkpUpsLs = null;
          PrintReportVO lookUpVO = null;

          if (inpContractLookUps != null && !inpContractLookUps.equals("")) {
            // lkpUpsLs = new ArrayList<Map<String, String>>();
            lkpUpsLs = new ArrayList<PrintReportVO>();
            JSONObject json = new JSONObject(inpContractLookUps);
            JSONArray arr = json.getJSONArray("Lookup");

            for (int i = 0; i < arr.length(); i++) {
              JSONObject jsob = arr.getJSONObject(i);
              lookUpVO = new PrintReportVO();
              lookUpVO.setSeqNo(jsob.getString("SeqNo"));
              lookUpVO.setDirectlookup(jsob.getString("ContractLookup"));
              lkpUpsLs.add(lookUpVO);
            }
          }
          BeanComparator fieldComparator = new BeanComparator("seqNo");
          if (fieldComparator != null) {
            Collections.sort(lkpUpsLs, fieldComparator);
          }

          log.debug("lkpUpsLs" + lkpUpsLs);
          designParameters.put("inpPhotoCopiesList", lkpUpsLs);

          strReportName = reportDir + "DirectPOLetterReport/DirectPOLetterReport.jrxml";
          strFileName = "Direct PO Letter Report";

        } else if (inpContractreport.equals("CONT")) {
          designParameters.put("inpPOId", inpRecordId);
          // Fetch bg amt in words
          String bgAmtInWords = dao.getBgAmountInWords(inpRecordId);
          designParameters.put("bgAmtInWords", bgAmtInWords);
          strReportName = reportDir + "contract/Contract.jrxml";
          strFileName = "ContractReport";
        } else if (inpContractreport.equals("CLFD")) {
          designParameters.put("inpPOId", inpRecordId);
          String inpDptGnrlMgr = request.getParameter("inpDptGnrlMgr");
          designParameters.put("inpDptGnrlMgr", inpDptGnrlMgr);
          strReportName = reportDir + "contractltrtofinancedewan/ContractLtrToFinanceDean.jrxml";
          strFileName = "ContractLetterToFinanceDewan";
        } else if (inpContractreport.equals("TSR")) {
          String inpDptOwner = request.getParameter("inpDptOwner");
          designParameters.put("inpDptOwner", inpDptOwner);
          designParameters.put("inpPOId", inpRecordId);
          strReportName = reportDir + "TechnicalStudyReport/TechnicalStudyReport.jrxml";
          strFileName = "TechnicalStudyReport";
        } else if (inpContractreport.equals("PCCR")) {
          designParameters.put("inpImageFlag", imageFlag);
          designParameters.put("inpOrgId", ord.getOrganization().getId());
          designParameters.put("inpPOId", inpRecordId);
          strReportName = reportDir
              + "PrintComputersContractReport/PrintComputersContractReport.jrxml";
          strFileName = "PrintComputersContractReport";
        } else if (inpContractreport.equals("PER")) {
          designParameters.put("inpPOId", inpRecordId);
          strReportName = reportDir + "printelectricityreport/PrintElectricity.jrxml";
          strFileName = "printelectricity";
        } else if (inpContractreport.equals("CCR")) {
          designParameters.put("OrderId", inpRecordId);
          strReportName = reportDir + "cleaningContract/CleaningContracts.jrxml";
          strFileName = "PrintCleaningContractReport";
        } else if (inpContractreport.equals("PSC")) {
          designParameters.put("inpPOId", inpRecordId);
          strReportName = reportDir + "printsupervisioncontract/PrintSupervisionContract.jrxml";
          strFileName = "PrintSupervisionContract";
        } else if (inpContractreport.equals("DCR")) {
          designParameters.put("inpImageFlag", imageFlag);
          designParameters.put("inpOrgId", ord.getOrganization().getId());
          designParameters.put("inpPOId", inpRecordId);
          strReportName = reportDir + "designcontractreport/DesignContractReport.jrxml";
          strFileName = "DesignContractReport";
        } else if (inpContractreport.equals("MOFAC")) {
          String inpContractLookUps = request.getParameter("inpContractLookUps");
          ArrayList<PrintReportVO> lkpUpsLs = null;
          PrintReportVO lookUpVO = null;

          if (inpContractLookUps != null && !inpContractLookUps.equals("")) {

            lkpUpsLs = new ArrayList<PrintReportVO>();
            JSONObject json = new JSONObject(inpContractLookUps);
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
              lookUpVO.setAwardLookUp(jsob.getString("ContractLookup"));
              lkpUpsLs.add(lookUpVO);
            }
          }
          BeanComparator fieldComparator = new BeanComparator("seqNo");
          if (fieldComparator != null) {
            Collections.sort(lkpUpsLs, fieldComparator);
          }
          designParameters.put("inpPhotoCopiesList", lkpUpsLs);
          designParameters.put("inpPOId", inpRecordId);
          strReportName = reportDir + "mofapproval/MOFApproval.jrxml";
          strFileName = "MOFApprovalReport";
        }
      }

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
