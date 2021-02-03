package sa.elm.ob.finance.ad_reports.Mumtalaqat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.sales.SalesRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinLookupLine;
import sa.elm.ob.scm.ad_process.printreport.PrintReportVO;
import sa.elm.ob.scm.ad_reports.custodybarcodelabel.CustodyBarcodeLabel;
import sa.elm.ob.utility.ad_reports.NumberToWords;
import sa.elm.ob.utility.util.Utility;

public class MumtalaqatReport extends HttpSecureAppServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private String jspPage = "../web/sa.elm.ob.finance/jsp/MumtalaqatReport/MumtalaqatReport.jsp";
  private static final Logger log = LoggerFactory.getLogger(CustodyBarcodeLabel.class);

  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doPost(request, response);
  }

  @SuppressWarnings("unchecked")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      String action = (request.getParameter("action") == null ? ""
          : request.getParameter("action"));
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String strReportName = "";
      if (action.equals("")) {

        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        request.getRequestDispatcher(jspPage).include(request, response);
      } else if (action.equals("DownloadReport")) {
        String inpBeneficiaryId = request.getParameter("inpBeneficiaryId_1");
        String inpYearId = request.getParameter("inpYearId_1");
        String inpRegionId = request.getParameter("inpRegionId_1");
        String inpContractLookUps = request.getParameter("inpContractLookUps");
        String inpInvoiceId = request.getParameter("inpInvoiceId_1");
        String inpOutput = request.getParameter("inpOutput_1");

        String regionName = "";
        String inwardNumber = "";
        String financialYear = "";
        String mumtalakatDept = "";
        String mumtalakatDeptName = "";
        String inwardDate = "";
        Set<String> mumtalakatDeptValues = new HashSet<String>();
        Set<String> mumtalakatDeptRealValues = new HashSet<String>();

        List<String> mumtalakatDeptNames = new ArrayList<>();

        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("inpBeneficiaryId", inpBeneficiaryId);
        parameters.put("inpYearId", inpYearId);
        parameters.put("inpRegionId", inpRegionId);
        parameters.put("inpInvoiceId", inpInvoiceId);

        // Financial year
        Year year = OBDal.getInstance().get(Year.class, inpYearId);
        if (year != null) {
          financialYear = NumberToWords.ConvertAmountToArabicAmount(year.getFiscalYear());
        }

        // Region
        EfinLookupLine lookupLine = OBDal.getInstance().get(EfinLookupLine.class, inpRegionId);
        if (lookupLine != null) {
          regionName = lookupLine.getName();
        }

        // Purchase invoice
        Invoice invoice = OBDal.getInstance().get(Invoice.class, inpInvoiceId);
        if (invoice != null) {
          inwardNumber = NumberToWords.ConvertAmountToArabicAmount(invoice.getEfinInwardno());
          inwardDate = NumberToWords.ConvertAmountToArabicAmount(
              Utility.convertGregToHijriTabadulPattern(invoice.getEfinInwarddate()));

          mumtalakatDeptRealValues.add(invoice.getEfinMumtalakatdesc());
          if (!StringUtils.isEmpty(invoice.getEfinMumtalakatdesc())) {
            mumtalakatDeptValues
                .add(NumberToWords.ConvertAmountToArabicAmount(invoice.getEfinMumtalakatdesc()));
          } else {
            mumtalakatDeptValues.add(NumberToWords.ConvertAmountToArabicAmount(""));
          }
        }

        // Finding the mumtalkat number from invoice
        if (invoice == null) {
          OBQuery<Invoice> invoiceQry = OBDal.getInstance().createQuery(Invoice.class,
              "as e join e.invoiceLineList linelist where linelist.businessPartner.id =:bpId");

          invoiceQry.setNamedParameter("bpId", inpBeneficiaryId);

          if (invoiceQry.list().size() > 0) {
            for (Invoice invoices : invoiceQry.list()) {
              mumtalakatDeptRealValues.add(invoices.getEfinMumtalakatdesc());
              if (!StringUtils.isEmpty(invoices.getEfinMumtalakatdesc())) {
                mumtalakatDeptValues.add(
                    NumberToWords.ConvertAmountToArabicAmount(invoices.getEfinMumtalakatdesc()));
              } else {
                mumtalakatDeptValues.add(NumberToWords.ConvertAmountToArabicAmount(""));
              }
            }
          }
        }

        // Finding the mumtalakat department Names

        OBQuery<SalesRegion> costcenterQry = OBDal.getInstance().createQuery(SalesRegion.class,
            "as e where e.searchKey in (:values)");

        costcenterQry.setNamedParameter("values", mumtalakatDeptRealValues);

        if (costcenterQry.list().size() > 0) {
          for (SalesRegion regions : costcenterQry.list()) {
            mumtalakatDeptNames.add(regions.getName());
          }
        }

        mumtalakatDept = String.join(",", mumtalakatDeptValues);
        mumtalakatDeptName = String.join(",", mumtalakatDeptNames);

        ArrayList<PrintReportVO> lkpUpsLs = null;
        PrintReportVO lookUpVO = null;
        List<Character> chars = new ArrayList<>();

        if (inwardNumber != null) {
          // Create an empty List of character

          // For each character in the String
          // add it to the List
          for (char ch : inwardNumber.toCharArray()) {

            chars.add(ch);
          }
        }

        if (inpContractLookUps != null && !inpContractLookUps.equals("")) {
          lkpUpsLs = new ArrayList<PrintReportVO>();
          JSONObject json = new JSONObject(inpContractLookUps);
          JSONArray arr = json.getJSONArray("Lookup");
          String lookupValue = "";

          for (int i = 0; i < arr.length(); i++) {
            lookupValue = "";
            JSONObject jsob = arr.getJSONObject(i);
            if (jsob.getString("SeqNo").equals("10")) {
              lookUpVO = new PrintReportVO();
              lookUpVO.setSeqNo(jsob.getString("SeqNo"));
              lookUpVO.setContractLookUp(jsob.getString("ContractLookup") + " " + regionName);
              lkpUpsLs.add(lookUpVO);
            } /*
               * else if (jsob.getString("SeqNo").equals("30")) { lookUpVO = new PrintReportVO();
               * lookUpVO.setSeqNo(jsob.getString("SeqNo")); lookUpVO.setContractLookUp(
               * financialYear + " " + jsob.getString("ContractLookup") + " " + mumtalakatDept);
               * lkpUpsLs.add(lookUpVO); }
               */ else if (jsob.getString("SeqNo").equals("20")) {
              lookUpVO = new PrintReportVO();
              lookUpVO.setSeqNo(jsob.getString("SeqNo"));
              lookUpVO.setContractLookUp(jsob.getString("ContractLookup") + "  "
                  + printListLtr(inwardDate + " , " + inwardNumber));
              lkpUpsLs.add(lookUpVO);
            } /*
               * else if (jsob.getString("SeqNo").equals("40")) { lookUpVO = new PrintReportVO();
               * lookUpVO.setSeqNo(jsob.getString("SeqNo"));
               * lookUpVO.setContractLookUp(jsob.getString("ContractLookup"));
               * lkpUpsLs.add(lookUpVO); }
               */ else if (jsob.getString("SeqNo").equals("30")) { // from 50 to 30 changed.
              lookUpVO = new PrintReportVO();
              String mumtalakatNo = "";
              String invoiceNo = "";
              if (!StringUtils.isEmpty(invoice.getEfinMumtalakatdesc())) {
                mumtalakatNo = NumberToWords
                    .ConvertAmountToArabicAmount(invoice.getEfinMumtalakatdesc());
              }
              if (!StringUtils.isEmpty(invoice.getDocumentNo())) {
                invoiceNo = NumberToWords.ConvertAmountToArabicAmount(invoice.getDocumentNo());
              }
              lookUpVO.setSeqNo(jsob.getString("SeqNo"));
              lookupValue = jsob.getString("ContractLookup");
              String lookupText = jsob.getString("ContractLookup").replace("#", invoiceNo)
                  .replace("$", mumtalakatNo);
              int first = lookupText.indexOf(":");
              String newstr = lookupText.substring(0, first + 1);
              String newstr2 = lookupText.substring(first + 1, lookupText.length())
                  .replaceFirst(":", " " + financialYear + " : ");
              String newstr3 = newstr + newstr2;
              lookUpVO.setContractLookUp(newstr3);
              lkpUpsLs.add(lookUpVO);
            } else {
              lookUpVO = new PrintReportVO();
              lookUpVO.setSeqNo(jsob.getString("SeqNo"));
              lookUpVO.setContractLookUp(jsob.getString("ContractLookup"));
              lkpUpsLs.add(lookUpVO);
            }
          }
        }

        BeanComparator fieldComparator = new BeanComparator("seqNo");
        if (fieldComparator != null) {
          Collections.sort(lkpUpsLs, fieldComparator);
        }
        parameters.put("inpPhotoCopiesList", lkpUpsLs);

        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/Mumtalaqat/mumtalaqatreport.jrxml";
        String strOutput = inpOutput;

        renderJR(vars, response, strReportName, strOutput, parameters, null, null);
      }
    } catch (Exception e) {
      log.error("error MumtalaqatReport" + e);
      // TODO: handle exception
    } finally {

    }
  }

  public String printListLtr(String words) {

    if (StringUtils.isNotEmpty(words)) {
      List<Character> chars = new ArrayList<>();
      for (char ch : words.toCharArray()) {

        chars.add(ch);
      }

      if (chars.size() == 0)
        return "";
      StringBuilder b = new StringBuilder("\u200e");
      for (Character c : chars) {
        b.append('\u200e').append(c);
      }
      return b.append('\u200e').toString();
    }

    return words;
  }
}