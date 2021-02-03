package sa.elm.ob.finance.ad_reports.Mumtalaqat;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

import sa.elm.ob.scm.ad_process.printreport.PrintReportVO;

public class MumtalaqatReportAjax extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("static-access")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection con = null;
    MumtalaqatDao dao = null;
    try {
      con = getConnection();
      dao = new MumtalaqatDao(con);

      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      if (action.equals("getLookUpsList")) {
        List<PrintReportVO> lkUpLs = null;
        try {
          String sortColName = request.getParameter("sidx");
          String sortType = request.getParameter("sord");
          String searchFlag = request.getParameter("_search");

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
          lkUpLs = dao.getLookups(vars.getClient(), sortColName, sortType, rows, page, searchFlag);
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
      } else if (action.equals("getbeneficiary")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getPaymentBeneficiaryList(OBContext.getOBContext().getCurrentClient().getId(),
            request.getParameter("searchTerm"), Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getinvoice")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getInvoiceList(OBContext.getOBContext().getCurrentClient().getId(),
            request.getParameter("searchTerm"), Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")), request.getParameter("bpId"));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getyear")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getFinancialYear(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      } else if (action.equals("getregion")) {
        JSONObject jsob = new JSONObject();
        jsob = dao.getRegion(vars.getClient(), request.getParameter("searchTerm"),
            Integer.parseInt(request.getParameter("pageLimit")),
            Integer.parseInt(request.getParameter("page")));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write(jsob.toString());
      }
    } catch (final Exception e) {
      log4j.error("Exception in CustodyCardReportAjax : ", e);
    }
  }
}