package sa.elm.ob.scm.ad_process.openenvelopeevent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.TabTrl;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.ui.WindowTrl;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;

import sa.elm.ob.finance.ad_process.ExportBudget.util.XSSFExcelStyles;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.ad_process.POandContract.POExportDAOImpl;
import sa.elm.ob.scm.properties.Resource;
import sa.elm.ob.utility.util.Constants;

/**
 * 
 * @author Priyanka on 21/05/2020
 *
 */

public class OpenEnvelopeEventExport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  // Proposal
  public static final String tabId = "106D3D0F9C6648A9AB3F50FA69AC6BCA";
  public static final String seqNo = "1B9655ECD7D0418380C2A5915CEACBFC";
  public static final String proposalNo = "23A74316871145CD88EE719183682483";
  public static final String supplierWithSubcontracts = "910DC34F94AD4A44AF480C7BCF2ECD24";
  public static final String grossPrice = "B94CF75E81FC4937997E049926CDFB0E";
  public static final String currency = "743E0D9803F644DFBEDF41C0EF857A91";
  public static final String discountPercentage = "BED2A3378F7E487DBDE15C77FC40BA56";
  public static final String discountAmount = "37EBDBC65B32474F961E96C16CA7B401";
  public static final String netPrice = "8E828AE6159A486DAD9AC2BB3E909549";
  public static final String representativeName = "90F7FDEAF6F541E7BEB757A9020730CB";
  public static final String comments = "49E434DA95414C0E9F3B934BA69A91E5";

  // Bank Gurantee Tab details
  public static final String bgTabId = "BC7489A521854DA1B92D40ED7C7A7098";
  public static final String bgInternalNo = "2886ED89178C4CC9B2681DAC21160597";
  public static final String bankName = "9D71FCE2A05C4AF5A6A8B294D34468E2";
  public static final String bankBranch = "65176FD0E769428E88DAE0A80BC6F5A1";
  public static final String bankAddress = "7177BBEEA2E24ECEAC1F9A7AF300206D";
  public static final String bgAmount = "B044497434FC421FA21511701BC4897B";
  public static final String revisedBgAmount = "F88C42E3AD324B2D959383F3E0724466";
  public static final String letterReferenceNo = "B105863769034BA6AC4B4868D0A9B988";
  public static final String foreignBank = "F0E99477E4F74455BB67862678B4D526";
  public static final String foreignBankName = "AA3787F22E8E4C05B8C7EA1E05E7DBD2";
  public static final String startDate = "0092FA1634C842D1B4A532A55A8652DF";
  public static final String expiryDate = "AB939D3AA5994619A0CB85414FF0B323";
  public static final String notes = "C7577B7E39E2482EBEE33BD7C648B5CB";
  public static final String bgArchieveRef = "0EC300C6C20F4696A8724A1DBE1CAEF0";
  public static final String extendExpiryDate = "C15B369F716F4E39A38EE8A464A05B32";
  public static final String archiveFolderNo = "D3BF0AB8CB51466EB6A7C7B1EB8B57A7";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    try {
      // Excel File
      OBContext.setAdminMode();
      @SuppressWarnings("unused")
      File excelFile = null;
      String openEnvelopeId = request.getParameter("inpRecordId");
      VariablesSecureApp vars = new VariablesSecureApp(request);
      Escmopenenvcommitee openEnvelopeEvent = OBDal.getInstance().get(Escmopenenvcommitee.class,
          openEnvelopeId);

      OBContext.setAdminMode();
      Window windowobj = OBDal.getInstance().get(Window.class, Constants.OPEN_ENVELOPE_EVENT_W);
      String windowName = "";
      if (windowobj != null) {
        List<WindowTrl> transList = windowobj.getADWindowTrlList();
        if (transList.size() > 0) {
          windowName = "_" + transList.get(0).getName();
        }
      }
      // create excel file
      // Task No: 8230 structured in reverse direction to get the required
      // format(ArabiceWindowName_Docno)
      String file = openEnvelopeEvent.getEventno() + windowName;
      file = URLEncoder.encode(file, "UTF-8").replaceAll("\\+", " ");

      String filedir = globalParameters.strFTPDirectory + "/";
      excelFile = createExcel(filedir + file + "-.xlsx", openEnvelopeId, vars);

      response.setContentType("text/html; charset=UTF-8");
      response.setCharacterEncoding("UTF-8");
      response.addHeader("Content-Disposition", "inline; filename=" + file + "-.html");
      printPagePopUpDownload(response.getOutputStream(),
          URLEncoder.encode(file, "UTF-8") + "-.xlsx");

    } catch (IOException e) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      log4j.error("Exception in Download : ", e);
    } catch (Exception e) {
      log4j.error("Exception in Download : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public File createExcel(String filePath, String openEnvelopeId, VariablesSecureApp vars) {
    File file = null;
    PreparedStatement st = null;
    PreparedStatement st1 = null;
    ResultSet rs = null;
    ResultSet rs1 = null;
    try {
      OBContext.setAdminMode();
      Connection conn = null;
      conn = getConnection();
      int x = 1;
      int y = 1;
      POExportDAOImpl dao = new POExportDAOImpl();
      String lang = vars.getLanguage();

      st = conn.prepareStatement(
          "select escm_proposal_attr_id,line,proposalno,bp.em_efin_documentno as supplierID,bp.name as supplierName,secbp.em_efin_documentno as jointventure_supplier,gross_price,iso_code,discount,discountamt,\n"
              + "proatt.netprice,representative_name,comments " + "from escm_proposal_attr proatt "
              + "join escm_openenvcommitee openenv on openenv.escm_openenvcommitee_id=proatt.escm_openenvcommitee_id "
              + "join escm_proposalmgmt prop on prop.escm_proposalmgmt_id=proatt.escm_proposalmgmt_id "
              + "join c_currency cur on cur.c_currency_id=prop.c_currency_id "
              + "join c_bpartner bp on bp.c_bpartner_id=suppliername "
              + "left join c_bpartner secbp on secbp.c_bpartner_id=prop.Secondsupplier "
              + "where proatt.escm_openenvcommitee_id='" + openEnvelopeId + "' order by line");

      rs = st.executeQuery();
      st1 = conn.prepareStatement("select bgdet.escm_proposal_attr_id,line,"
          + "proposalno,bgdet.internalno,bank.value as banknumber ,bank.bankname as bankname,bank_branch,bank_address,bgdet.bgamount,"
          + "bgdet.bgamount-coalesce(bgrev.bgreducedamt,0) as revisedbgamt,bgdet.bankbgno as letterrefno,foreign_bank,foreign_bank_name,eut_convert_to_hijri(to_char(bgstartdateh,'YYYY-MM-DD')) as startdateh,eut_convert_to_hijri(to_char(expirydateh,'YYYY-MM-DD')) as expirydateh,"
          + "bgdet.notes,bgdet.BG_Archive_Ref,extend_expdateh,Escm_Gua_Gletter_Book_No "
          + "from escm_bankguarantee_detail bgdet "
          + "join escm_proposalmgmt prop on prop.escm_proposalmgmt_id=bgdet.escm_proposalmgmt_id "
          + "join efin_bank bank on bank.efin_bank_id=bgdet.bankname "
          + "join escm_bgworkbench bgwrkbench on bgwrkbench.escm_bgworkbench_id=bgdet.escm_bgworkbench_id "
          + "left join (select coalesce(bg.bgamount,0)-coalesce(sum(rev.reduced_amt),0) as bgreducedamt,bg.escm_bankguarantee_detail_id  from escm_bg_amtrevision rev ,escm_bankguarantee_detail bg "
          + " where rev.escm_bankguarantee_detail_id= bg.escm_bankguarantee_detail_id "
          + "and  rev.escm_bankguarantee_detail_id =bg.escm_bankguarantee_detail_id  group by bg.bgamount,bg.escm_bankguarantee_detail_id) bgrev on bgrev.escm_bankguarantee_detail_id=bgdet.escm_bankguarantee_detail_id "
          + "where bgdet.escm_proposal_attr_id in (select escm_proposal_attr_id from escm_proposal_attr "
          + "where escm_openenvcommitee_id='" + openEnvelopeId + "') order by line");
      rs1 = st1.executeQuery();

      file = new File(filePath);
      XSSFWorkbook workbook = new XSSFWorkbook();
      OBContext.setAdminMode();
      Tab tabobj = OBDal.getInstance().get(Tab.class, Constants.OEE_PROPOSAL_ATTRIBUTES_TAB);
      String tabName = "";
      if (tabobj != null) {
        List<TabTrl> transList = tabobj.getADTabTrlList();
        if (transList.size() > 0) {
          tabName = transList.get(0).getName();
        }
      }
      XSSFSheet sheet = workbook.createSheet(tabName);

      if (!vars.getLanguage().equals("en_US")) {
        sheet.setRightToLeft(true);
      }

      tabobj = OBDal.getInstance().get(Tab.class, Constants.OEE_BANK_GUARANTEE_TAB);
      tabName = "";
      if (tabobj != null) {
        List<TabTrl> transList = tabobj.getADTabTrlList();
        if (transList.size() > 0) {
          tabName = transList.get(0).getName();
        }
      }
      XSSFSheet sheet1 = workbook.createSheet(tabName);

      if (!vars.getLanguage().equals("en_US")) {
        sheet.setRightToLeft(true);
      }

      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      XSSFCellStyle unlockedCellStyle = workbook.createCellStyle();
      unlockedCellStyle.setLocked(true);

      XSSFExcelStyles excelStyles = new XSSFExcelStyles();
      Map<String, XSSFCellStyle> styles = excelStyles.createStyles(workbook);
      sheet.enableLocking();
      sheet1.enableLocking();
      CTSheetProtection sheetProtection = sheet.getCTWorksheet().getSheetProtection();

      /*
       * sheetProtection.setSelectLockedCells(true); sheetProtection.setSelectUnlockedCells(false);
       * sheetProtection.setFormatCells(false); sheetProtection.setFormatColumns(false);
       * sheetProtection.setFormatRows(true); sheetProtection.setInsertColumns(false);
       * sheetProtection.setInsertRows(false); sheetProtection.setInsertHyperlinks(false);
       * sheetProtection.setDeleteColumns(false); sheetProtection.setDeleteRows(false);
       * sheetProtection.setSort(false); sheetProtection.setAutoFilter(false);
       * sheetProtection.setPivotTables(false); sheetProtection.setObjects(false);
       * sheetProtection.setScenarios(false);
       */

      // Proposal Field List
      List<Field> fieldList = null;
      HashMap<String, String> fieldMap = new HashMap<>();

      OBQuery<Field> fieldObj = OBDal.getInstance().createQuery(Field.class,
          "as e where e.tab.id=:tabId");
      fieldObj.setNamedParameter("tabId", tabId);
      fieldList = fieldObj.list();
      for (Field itr : fieldList) {
        fieldMap.put(itr.getId(), itr.getName());
      }
      // BG Field list
      List<Field> bgFieldList = null;
      HashMap<String, String> bgFieldMap = new HashMap<>();

      OBQuery<Field> bgFieldObj = OBDal.getInstance().createQuery(Field.class,
          "as e where e.tab.id=:bgTabId");
      bgFieldObj.setNamedParameter("bgTabId", bgTabId);
      bgFieldList = bgFieldObj.list();
      for (Field itr : bgFieldList) {
        bgFieldMap.put(itr.getId(), itr.getName());
      }
      if (vars.getLanguage().equals("en_US")) {
        // Create Header for Proposal
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i <= 13; i++) {
          XSSFCell cell = headerRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString("escm_proposal_attr_id"));
            break;
          case 1:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(seqNo) ? fieldMap.get(seqNo) : "")
                    + "(*)");
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(proposalNo) ? fieldMap.get(proposalNo) : ""));

            break;
          case 3:
            cell.setCellValue(
                new XSSFRichTextString(Resource.getProperty("scm.supplier.ID", lang)) + "(*)");
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 4:
            cell.setCellValue(
                new XSSFRichTextString(Resource.getProperty("scm.supplier.Name", lang)));
            cell.setCellStyle(styles.get("CellLock"));
            break;

          case 5:
            cell.setCellValue(
                new XSSFRichTextString(Resource.getProperty("scm.joint.venture.supplier", lang)));
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 6:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(grossPrice) ? fieldMap.get(grossPrice) : "") + "(*)");

            break;
          case 7:
            cell.setCellValue(
                new XSSFRichTextString(fieldMap.containsKey(currency) ? fieldMap.get(currency) : "")
                    + "(*)");
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 8:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(discountPercentage) ? fieldMap.get(discountPercentage) : "")
                + "(*)");
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(discountAmount) ? fieldMap.get(discountAmount) : ""));
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 10:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(netPrice) ? fieldMap.get(netPrice) : ""));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(representativeName) ? fieldMap.get(representativeName) : ""));
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(
                fieldMap.containsKey(comments) ? fieldMap.get(comments) : ""));
            break;
          }
        }
        // Create Header for Bank Guarantee Details
        XSSFRow header = sheet1.createRow(0);

        for (int i = 0; i <= 18; i++) {
          XSSFCell cell1 = header.createCell(i);
          cell1.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell1.setCellValue(new XSSFRichTextString("escm_proposal_attr_id"));
            break;
          case 1:
            cell1.setCellValue(new XSSFRichTextString("Seq No.") + "(*)");
            break;
          case 2:
            cell1.setCellValue(new XSSFRichTextString("Proposal No"));
            break;

          case 3:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(bgInternalNo) ? bgFieldMap.get(bgInternalNo) : ""));
            break;
          case 4:
            cell1.setCellValue(
                new XSSFRichTextString(Resource.getProperty("scm.bank.number", lang)) + "(*)");
            break;
          case 5:
            cell1.setCellValue(new XSSFRichTextString(Resource.getProperty("scm.bank.name", lang)));
            break;
          case 6:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(bankBranch) ? bgFieldMap.get(bankBranch) : ""));
            break;
          case 7:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(bankAddress) ? bgFieldMap.get(bankAddress) : ""));
            break;
          case 8:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(bgAmount) ? bgFieldMap.get(bgAmount) : "") + "(*)");
            break;
          case 9:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(revisedBgAmount) ? bgFieldMap.get(revisedBgAmount) : ""));
            break;
          case 10:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(letterReferenceNo) ? bgFieldMap.get(letterReferenceNo) : "")
                + "(*)");
            break;
          case 11:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(foreignBank) ? bgFieldMap.get(foreignBank) : "") + "(*)");
            break;
          case 12:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(foreignBankName) ? bgFieldMap.get(foreignBankName) : ""));
            break;
          case 13:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(startDate) ? bgFieldMap.get(startDate) : "") + "(*)");
            break;
          case 14:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(expiryDate) ? bgFieldMap.get(expiryDate) : "") + "(*)");
            break;
          case 15:
            cell1.setCellValue(
                new XSSFRichTextString(bgFieldMap.containsKey(notes) ? bgFieldMap.get(notes) : ""));
            break;
          case 16:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(bgArchieveRef) ? bgFieldMap.get(bgArchieveRef) : ""));
            break;
          case 17:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(extendExpiryDate) ? bgFieldMap.get(extendExpiryDate) : ""));
            break;
          case 18:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldMap.containsKey(archiveFolderNo) ? bgFieldMap.get(archiveFolderNo) : ""));
            break;

          }
        }

      }

      else {
        List<FieldTrl> fieldTrlList = null;
        HashMap<String, String> fieldTrlMap = new HashMap<>();
        OBQuery<FieldTrl> fieldTrlObj = OBDal.getInstance().createQuery(FieldTrl.class,
            "as e join e.field field join field.tab tab where field.tab.id=:tabId");
        fieldTrlObj.setNamedParameter("tabId", tabId);
        fieldTrlList = fieldTrlObj.list();
        for (FieldTrl itr : fieldTrlList) {
          fieldTrlMap.put(itr.getField().getId(),
              StringUtils.isEmpty(itr.getName()) ? itr.getField().getName() : itr.getName());
        }

        // BG Field list
        List<FieldTrl> bgFieldTrlList = null;
        HashMap<String, String> bgFieldTrlMap = new HashMap<>();
        OBQuery<FieldTrl> bgFieldTrlObj = OBDal.getInstance().createQuery(FieldTrl.class,
            "as e join e.field field join field.tab tab where field.tab.id=:bgTabId");
        bgFieldTrlObj.setNamedParameter("bgTabId", bgTabId);
        bgFieldTrlList = bgFieldTrlObj.list();
        for (FieldTrl itr : bgFieldTrlList) {
          bgFieldTrlMap.put(itr.getField().getId(),
              StringUtils.isEmpty(itr.getName()) ? itr.getField().getName() : itr.getName());
        }

        // Create Header
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i <= 13; i++) {
          XSSFCell cell = headerRow.createCell(i);
          cell.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString("escm_proposal_attr_id"));
            break;
          case 1:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(seqNo) ? fieldTrlMap.get(seqNo)
                    : fieldMap.get(seqNo) + "(*)"));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(proposalNo) ? fieldTrlMap.get(proposalNo)
                    : fieldMap.get(proposalNo)));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 3:
            cell.setCellValue(
                new XSSFRichTextString(Resource.getProperty("scm.supplier.ID", lang)) + "(*)");
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 4:
            cell.setCellValue(
                new XSSFRichTextString(Resource.getProperty("scm.supplier.Name", lang)));
            cell.setCellStyle(styles.get("CellLock"));
            break;

          case 5:
            cell.setCellValue(
                new XSSFRichTextString(Resource.getProperty("scm.joint.venture.supplier", lang)));
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 6:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(grossPrice) ? fieldTrlMap.get(grossPrice)
                    : fieldMap.get(grossPrice))
                + "(*)");
            break;
          case 7:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(currency) ? fieldTrlMap.get(currency)
                    : fieldMap.get(currency)) + "(*)");
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 8:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(discountPercentage) ? fieldTrlMap.get(discountPercentage)
                    : fieldMap.get(discountPercentage))
                + "(*)");
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(discountAmount) ? fieldTrlMap.get(discountAmount)
                    : fieldMap.get(discountAmount)));
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 10:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(netPrice) ? fieldTrlMap.get(netPrice)
                    : fieldMap.get(netPrice)));
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(
                fieldTrlMap.containsKey(representativeName) ? fieldTrlMap.get(representativeName)
                    : fieldMap.get(representativeName)));
            break;
          case 12:
            cell.setCellValue(
                new XSSFRichTextString(fieldTrlMap.containsKey(comments) ? fieldTrlMap.get(comments)
                    : fieldMap.get(comments)));
            break;
          }
        }

        // Create Header for Bank Guarantee Details
        XSSFRow header = sheet1.createRow(0);

        for (int i = 0; i <= 18; i++) {
          XSSFCell cell1 = header.createCell(i);
          cell1.setCellStyle(styles.get("Header"));
          switch (i) {
          case 0:
            cell1.setCellValue(new XSSFRichTextString("escm_proposal_attr_id"));
            break;
          case 1:
            cell1.setCellValue(new XSSFRichTextString("Seq No.") + "(*)");
            break;
          case 2:
            cell1.setCellValue(new XSSFRichTextString("Proposal No"));
            break;

          case 3:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(bgInternalNo) ? bgFieldTrlMap.get(bgInternalNo)
                    : bgFieldMap.get(bgInternalNo)));
            break;
          case 4:
            cell1.setCellValue(
                new XSSFRichTextString(Resource.getProperty("scm.bank.number", lang)) + "(*)");
            break;
          case 5:
            cell1.setCellValue(new XSSFRichTextString(Resource.getProperty("scm.bank.name", lang)));
            break;
          case 6:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(bankBranch) ? bgFieldTrlMap.get(bankBranch)
                    : bgFieldMap.get(bankBranch)));
            break;
          case 7:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(bankAddress) ? bgFieldTrlMap.get(bankAddress)
                    : bgFieldMap.get(bankAddress)));
            break;
          case 8:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(bgAmount) ? bgFieldTrlMap.get(bgAmount)
                    : bgFieldMap.get(bgAmount))
                + "(*)");
            break;
          case 9:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(revisedBgAmount) ? bgFieldTrlMap.get(revisedBgAmount)
                    : bgFieldMap.get(revisedBgAmount)));
            break;
          case 10:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(letterReferenceNo) ? bgFieldTrlMap.get(letterReferenceNo)
                    : bgFieldMap.get(letterReferenceNo))
                + "(*)");
            break;
          case 11:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(foreignBank) ? bgFieldTrlMap.get(foreignBank)
                    : bgFieldMap.get(foreignBank))
                + "(*)");
            break;
          case 12:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(foreignBankName) ? bgFieldTrlMap.get(foreignBankName)
                    : bgFieldMap.get(foreignBankName)));
            break;
          case 13:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(startDate) ? bgFieldTrlMap.get(startDate)
                    : bgFieldMap.get(startDate))
                + "(*)");
            break;
          case 14:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(expiryDate) ? bgFieldTrlMap.get(expiryDate)
                    : bgFieldMap.get(expiryDate))
                + "(*)");
            break;
          case 15:
            cell1.setCellValue(
                new XSSFRichTextString(bgFieldTrlMap.containsKey(notes) ? bgFieldTrlMap.get(notes)
                    : bgFieldMap.get(notes)));
            break;
          case 16:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(bgArchieveRef) ? bgFieldTrlMap.get(bgArchieveRef)
                    : bgFieldMap.get(bgArchieveRef)));
            break;
          case 17:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(extendExpiryDate) ? bgFieldTrlMap.get(extendExpiryDate)
                    : bgFieldMap.get(extendExpiryDate)));
            break;
          case 18:
            cell1.setCellValue(new XSSFRichTextString(
                bgFieldTrlMap.containsKey(archiveFolderNo) ? bgFieldTrlMap.get(archiveFolderNo)
                    : bgFieldMap.get(archiveFolderNo)));
            break;

          }
        }

      }

      while (rs.next()) {

        XSSFRow row = sheet.createRow(x);
        HashMap<Integer, String> cellMap = dao
            .getOeeProposalCellStyle(rs.getString("escm_proposal_attr_id"));

        for (int i = 0; i <= 12; i++) {
          CellStyle editableStyle = workbook.createCellStyle();
          editableStyle.setLocked(false);
          sheet.setDefaultColumnStyle(i, editableStyle);

          XSSFCell cell = row.createCell(i);
          cell.setCellStyle(styles.get(cellMap.get(i)));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString(rs.getString("escm_proposal_attr_id")));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(rs.getString("line")));
            break;

          case 2:
            cell.setCellValue(new XSSFRichTextString(rs.getString("proposalno")));

            break;
          case 3:
            cell.setCellValue(new XSSFRichTextString(rs.getString("supplierID")));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString(rs.getString("supplierName")));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 5:
            cell.setCellValue(new XSSFRichTextString(rs.getString("jointventure_supplier")));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 6:
            cell.setCellValue(new XSSFRichTextString(rs.getString("gross_price")));
            cell.setCellStyle(styles.get("CellUnlock"));
            break;
          case 7:
            cell.setCellValue(new XSSFRichTextString(rs.getString("iso_code")));
            break;
          case 8:
            cell.setCellValue(new XSSFRichTextString(rs.getString("discount")));
            cell.setCellStyle(styles.get("CellUnlock"));
            break;
          case 9:
            String Formula = "G" + (x + 1) + "-((" + "G" + (x + 1) + "*(100-" + "I" + (x + 1)
                + "))/100)";
            cell.setCellFormula(Formula);
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 10:
            String netPriceFormula = "G" + (x + 1) + "-J" + (x + 1);
            cell.setCellFormula(netPriceFormula);
            cell.setCellStyle(styles.get("CellLock"));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(rs.getString("representative_name")));
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(rs.getString("comments")));
            break;
          }
        }

        x++;
      }
      while (rs1.next()) {
        XSSFRow row = sheet1.createRow(y);
        // HashMap<Integer, String> cellMap = dao
        // .getOeeProposalCellStyle(rs1.getString("escm_proposal_attr_id"));

        for (int i = 0; i <= 18; i++) {
          CellStyle editableStyle = workbook.createCellStyle();
          editableStyle.setLocked(false);
          sheet1.setDefaultColumnStyle(i, editableStyle);

          XSSFCell cell = row.createCell(i);
          // cell.setCellStyle(styles.get(cellMap.get(i)));
          switch (i) {
          case 0:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("escm_proposal_attr_id")));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("line")));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("proposalno")));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 3:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("internalno")));
            cell.setCellStyle(styles.get("TextLock"));

            break;
          case 4:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("banknumber")));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 5:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("bankname")));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 6:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("bank_branch")));
            cell.setCellStyle(styles.get("TextLock"));
            break;
          case 7:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("bank_address")));
            break;
          case 8:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("bgamount")));
            cell.setCellStyle(styles.get("CellUnlock"));
            break;
          case 9:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("revisedbgamt")));
            cell.setCellStyle(styles.get("CellUnlock"));
            break;
          case 10:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("letterrefno")));
            cell.setCellStyle(styles.get("CellUnlock"));
            break;
          case 11:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("foreign_bank")));
            break;
          case 12:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("foreign_bank_name")));
            break;
          case 13:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("startdateh")));
            break;
          case 14:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("expirydateh")));
            break;
          case 15:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("notes")));
            break;
          case 16:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("BG_Archive_Ref")));
            break;
          case 17:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("extend_expdateh")));
            break;
          case 18:
            cell.setCellValue(new XSSFRichTextString(rs1.getString("Escm_Gua_Gletter_Book_No")));
            break;
          }
        }
        y++;
      }
      sheet.setColumnWidth(0, 50 * 100);
      sheet.setColumnWidth(1, 40 * 100);
      sheet.setColumnWidth(2, 40 * 100);
      sheet.setColumnWidth(3, 50 * 100);
      sheet.setColumnWidth(4, 50 * 100);
      sheet.setColumnWidth(5, 50 * 100);
      sheet.setColumnWidth(6, 50 * 100);
      sheet.setColumnWidth(7, 50 * 100);
      sheet.setColumnWidth(8, 75 * 100);
      sheet.setColumnWidth(9, 50 * 100);
      sheet.setColumnWidth(10, 30 * 100);
      sheet.setColumnWidth(11, 50 * 100);
      sheet.setColumnWidth(12, 50 * 100);
      sheet.setColumnWidth(13, 50 * 100);
      sheet.setColumnWidth(14, 50 * 100);
      sheet.setColumnWidth(15, 100 * 100);

      sheet.setColumnHidden(0, true);
      sheet1.setColumnHidden(0, true);
      sheet1.setColumnWidth(1, 60 * 100);
      sheet1.setColumnWidth(2, 40 * 100);
      sheet1.setColumnWidth(3, 50 * 100);
      sheet1.setColumnWidth(4, 50 * 100);
      sheet1.setColumnWidth(5, 50 * 100);
      sheet1.setColumnWidth(6, 50 * 100);
      sheet1.setColumnWidth(7, 50 * 100);
      sheet1.setColumnWidth(8, 75 * 100);
      sheet1.setColumnWidth(9, 50 * 100);
      sheet1.setColumnWidth(10, 30 * 100);
      sheet1.setColumnWidth(11, 50 * 100);
      sheet1.setColumnWidth(12, 50 * 100);
      sheet1.setColumnWidth(13, 50 * 100);
      sheet1.setColumnWidth(14, 50 * 100);
      sheet1.setColumnWidth(15, 100 * 100);
      sheet1.setColumnWidth(16, 50 * 100);
      sheet1.setColumnWidth(17, 50 * 100);
      sheet1.setColumnWidth(18, 50 * 100);
      sheet1.setColumnWidth(19, 50 * 100);

      // Create 100 Empty Rows
      for (int i = x; i < 100 + x; i++) {

        XSSFRow row = sheet.createRow(i);
        CellStyle editableStyle = workbook.createCellStyle();
        editableStyle.setLocked(false);
        sheet.setDefaultColumnStyle(i, editableStyle);
        for (int j = 0; j <= 12; j++) {
          XSSFCell cell = row.createCell(j);
          evaluator.evaluateFormulaCell(cell);
          if (j == 0)
            cell.setCellValue("Print");
          if (j == 1 || j == 2 || j == 4 || j == 6 || j == 7 || j == 8) {
            cell.setCellStyle(styles.get("CellUnlock"));
            cell.setCellStyle(styles.get("TextUnlock"));
          } else if (j == 3 || j == 5)
            cell.setCellStyle(styles.get("TextUnlockFormat"));
          if (j == 9) {
            String Formula = "IF(AND(ISNUMBER(G" + (i + 1) + "),ISNUMBER(I" + (i + 1) + "))," + "G"
                + (i + 1) + "-((" + "G" + (i + 1) + "*(100-" + "I" + (i + 1) + "))/100)," + "\"\""
                + ")";
            cell.setCellFormula(Formula);
            cell.setCellStyle(styles.get("Celllock"));
            cell.setCellValue("");
          }
          if (j == 10) {
            String netPriceFormula = "IF(AND(ISNUMBER(G" + (i + 1) + "),ISNUMBER(I" + (i + 1)
                + "))," + "G" + (i + 1) + "-J" + (i + 1) + "," + "\"\"" + ")";

            cell.setCellFormula(netPriceFormula);
            cell.setCellStyle(styles.get("Celllock"));

          }
          if (j == 11 || j == 12)
            cell.setCellStyle(styles.get("TextUnlockFormat"));
          cell.setCellValue("");

          cell.setCellValue(new XSSFRichTextString(""));
        }
      }
      for (int k = y; k < 100 + y; k++) {

        XSSFRow row = sheet1.createRow(k);
        CellStyle editableStyle = workbook.createCellStyle();
        editableStyle.setLocked(false);
        sheet.setDefaultColumnStyle(k, editableStyle);
        for (int j = 0; j < 19; j++) {
          XSSFCell cell = row.createCell(j);

          if (j == 3 || j == 9)
            cell.setCellStyle(styles.get("Celllock"));
          else if (j == 4 || j == 5 || j == 6 || j == 7 || j == 10 || j == 12 || j == 15 || j == 16
              || j == 18)
            cell.setCellStyle(styles.get("TextUnlockFormat"));
          else
            cell.setCellStyle(styles.get("TextUnlock"));

        }
      }

      evaluator.evaluateAll();
      sheet.setDisplayFormulas(false);

      FileOutputStream fileOut = new FileOutputStream(filePath);
      workbook.write(fileOut);
      fileOut.close();

    } catch (

    final Exception e) {
      log4j.error("Exception in createExcel() Method : ", e);
      return null;
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in createExcel() Method ", e);
      }
      OBContext.restorePreviousMode();
    }
    return file;
  }

}