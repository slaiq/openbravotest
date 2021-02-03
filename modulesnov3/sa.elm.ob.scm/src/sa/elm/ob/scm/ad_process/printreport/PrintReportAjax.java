package sa.elm.ob.scm.ad_process.printreport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.util.Utility;

public class PrintReportAjax extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  List<PrintReportVO> requestNoList = new ArrayList<PrintReportVO>();

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    PrintReportDAO dao = null;
    JSONArray jsonArray = null;
    JSONObject jsonResponse = null;
    try {
      dao = new PrintReportDAO();
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      if (action.equals("getLookUpsList")) {
        List<PrintReportVO> lkUpLs = null;
        try {
          String sortColName = request.getParameter("sidx");
          String sortType = request.getParameter("sord");
          String searchFlag = request.getParameter("_search");
          String Type = request.getParameter("type");

          int rows, page;
          try {
            page = Integer.parseInt(request.getParameter("page"));
          } catch (Exception nullexp) {
            page = 1;
          }
          try {
            rows = Integer.parseInt(request.getParameter("rows"));
          } catch (Exception nullexp) {
            rows = 20;
          }
          if (Type.equals("AWDLTR")) {
            lkUpLs = dao.getAwardLetterandReminderLetterCopiesLookups(vars.getClient(), sortColName,
                sortType, rows, page, searchFlag, "ALC");
          } else if (Type.equals("REMLTR")) {
            lkUpLs = dao.getAwardLetterandReminderLetterCopiesLookups(vars.getClient(), sortColName,
                sortType, rows, page, searchFlag, "RLP");
          } else if (Type.equals("ELP")) {
            lkUpLs = dao.getAwardLetterandReminderLetterCopiesLookups(vars.getClient(), sortColName,
                sortType, rows, page, searchFlag, "ELP");
          } else if (Type.equals("DLC")) {
            lkUpLs = dao.getAwardLetterandReminderLetterCopiesLookups(vars.getClient(), sortColName,
                sortType, rows, page, searchFlag, "DLC");
          } else if (Type.equals("MOFAC")) {
            lkUpLs = dao.getAwardLetterandReminderLetterCopiesLookups(vars.getClient(), sortColName,
                sortType, rows, page, searchFlag, "MOFAC");
          }
          int pages = 0, totalpage = 0, count = 0;
          response.setContentType("text/xml");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          StringBuffer xmlData = new StringBuffer("<?xml version='1.0' encoding='utf-8'?><rows>");

          if (lkUpLs != null) {
            if (lkUpLs.size() > 0) {
              for (PrintReportVO lkUpVO : lkUpLs) {
                pages = lkUpVO.getPage();
                totalpage = lkUpVO.getTotalPages();
                count = lkUpVO.getCount();

                xmlData.append("<row id = '" + lkUpVO.getAwardLookUpLnId() + "' >");
                xmlData.append("<cell><![CDATA[" + lkUpVO.getSeqNo() + "]]></cell>");
                xmlData.append("<cell><![CDATA[" + lkUpVO.getAwardLookUp() + "]]></cell>");
                xmlData.append("</row>");

              }
              xmlData.append("<page>" + pages + "</page>");
              xmlData.append("<total>" + totalpage + "</total>");
              xmlData.append("<records>" + count + "</records>");
            } else {
              xmlData.append("<page>" + pages + "</page>");
              xmlData.append("<total>" + totalpage + "</total>");
              xmlData.append("<records>" + count + "</records>");
            }
            xmlData.append("</rows>");
            response.getWriter().write(xmlData.toString());
          }
        } catch (Exception e) {
          log4j.error("Exception in PrintReportAjax", e);
        }
      }
      // get ReminderSubject list from reference lookup
      else if (action.equals("getReminderSubjectlist")) {
        List<ESCMDefLookupsTypeLn> ReminderSubjectlist = null;
        ReminderSubjectlist = dao.getReminderSubjectlist(vars.getClient());
        jsonArray = new JSONArray();
        if (ReminderSubjectlist != null && ReminderSubjectlist.size() > 0) {
          for (ESCMDefLookupsTypeLn remindersub : ReminderSubjectlist) {
            jsonResponse = new JSONObject();
            String valueandname = remindersub.getSearchKey()
                .concat("-".concat(remindersub.getCommercialName()));
            jsonResponse.put("referencelookuplineid", remindersub.getId());
            jsonResponse.put("reflookuplnvaluename", valueandname);
            jsonArray.put(jsonResponse);
          }
        }
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());
      } else if (action.equals("checkSequence")) {
        try {
          EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
              request.getParameter("inpRecordId"));
          String sequence = Utility.getTransactionSequence(proposal.getOrganization().getId(),
              "PMGLTR");
          jsonResponse = new JSONObject();
          jsonResponse.put("Sequence", sequence);
          response.setContentType("text/plain");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonResponse.toString());
        } catch (Exception e) {
          log4j.error("Exception in checkSequence", e);
        }
      } else if (action.equals("checkSequencepo")) {
        try {
          String sequence = Utility.getTransactionSequence(vars.getOrg(), "POLTR");
          jsonResponse = new JSONObject();
          jsonResponse.put("Sequence", sequence);
          response.setContentType("text/plain");
          response.setCharacterEncoding("UTF-8");
          response.setHeader("Cache-Control", "no-cache");
          response.getWriter().write(jsonResponse.toString());
        } catch (Exception e) {
          log4j.error("Exception in checkSequence", e);
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in PrintReportAjax : ", e);
    }
  }
}
