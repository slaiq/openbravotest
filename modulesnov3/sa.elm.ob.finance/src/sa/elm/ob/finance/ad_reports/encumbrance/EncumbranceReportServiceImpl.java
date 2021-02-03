package sa.elm.ob.finance.ad_reports.encumbrance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class EncumbranceReportServiceImpl implements EncumbranceReportService {

  @Override
  public Workbook createSheet1(XSSFWorkbook workbook, String fileName) throws Exception {

    try {

      XSSFSheet sheet = workbook.createSheet("EncumbranceReport");

      // Header Cell Style
      XSSFFont boldFont = (XSSFFont) workbook.createFont();
      boldFont.setBold(true);

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(boldFont);

      // Create Header
      int noOfColumns = 9;
      XSSFRow headerRow = sheet.createRow(0);
      for (int i = 0; i <= noOfColumns; i++) {
        XSSFCell cell = headerRow.createCell(i);
        cell.setCellStyle(headerStyle);
        switch (i) {
        case 0:
          cell.setCellValue(new XSSFRichTextString("Budget Type"));
          break;
        case 1:
          cell.setCellValue(new XSSFRichTextString("Encumbrance No"));
          break;
        case 2:
          cell.setCellValue(new XSSFRichTextString("Encumbrance Method"));
          break;
        case 3:
          cell.setCellValue(new XSSFRichTextString("Encumbrance Type"));
          break;
        case 4:
          cell.setCellValue(new XSSFRichTextString("Is Invoiced"));
          break;
        case 5:
          cell.setCellValue(new XSSFRichTextString("Cost Encumbrance No"));
          break;
        case 6:
          cell.setCellValue(new XSSFRichTextString("Amount Invoiced"));
          break;
        case 7:
          cell.setCellValue(new XSSFRichTextString("Funds Encumbrance Amount"));
          break;
        case 8:
          cell.setCellValue(new XSSFRichTextString("Funds Actual Amount"));
          break;
        case 9:
          cell.setCellValue(new XSSFRichTextString("Is Valid"));
          break;

        }
      }
      int i = 1;
      List<EncumbranceDTO> rowDataList = EncumbranceReportDAO.getExcelData();

      for (EncumbranceDTO data : rowDataList) {
        XSSFRow dataRow = sheet.createRow(i);
        for (int j = 0; j <= noOfColumns; j++) {
          XSSFCell cell = dataRow.createCell(j);
          switch (j) {
          case 0:
            cell.setCellValue(new XSSFRichTextString(data.getBudgetType()));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(data.getEncumbranceNo()));
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(data.getEncumbranceMethod()));
            break;
          case 3:
            cell.setCellValue(new XSSFRichTextString(data.getEncumbranceType()));
            break;
          case 4:
            cell.setCellValue(data.getIsInvoiced());
            break;
          case 5:
            cell.setCellValue(new XSSFRichTextString(data.getCostEncumbranceNo()));
            break;
          case 6:
            cell.setCellValue(data.getAmountInvoiced().toPlainString());
            break;
          case 7:
            cell.setCellValue(data.getFundsEncumbranceAmount().toPlainString());
            break;
          case 8:
            cell.setCellValue(data.getFundsActualAmount().toEngineeringString());
            break;
          case 9:
            cell.setCellValue(data.getIsValid());
            break;
          }
        }
        i++;
      }

      sheet.setAutoFilter(new CellRangeAddress(0, i, 0, 9));

    } catch (Exception e) {
      e.printStackTrace();
    }

    return workbook;

  }

  @Override
  public File createSheet2(XSSFWorkbook workbook, String fileName)
      throws FileNotFoundException, Exception {

    // Create Workbook and Sheet
    File file = null;

    try {

      XSSFSheet sheet = workbook.createSheet("EncumbranceReportSummary");
      file = new File(fileName);

      // Header Cell Style
      XSSFFont boldFont = (XSSFFont) workbook.createFont();
      boldFont.setBold(true);

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(boldFont);

      // Create Header
      int noOfColumns = 21;
      XSSFRow headerRow = sheet.createRow(0);
      for (int i = 0; i <= noOfColumns; i++) {
        XSSFCell cell = headerRow.createCell(i);
        cell.setCellStyle(headerStyle);
        switch (i) {
        case 0:
          cell.setCellValue(new XSSFRichTextString("Budget Type"));
          break;
        case 1:
          cell.setCellValue(new XSSFRichTextString("Account"));
          break;
        case 2:
          cell.setCellValue(new XSSFRichTextString("Uniquecode"));
          break;
        case 3:
          cell.setCellValue(new XSSFRichTextString("Original Budget Value"));
          break;
        case 4:
          cell.setCellValue(new XSSFRichTextString("Current Budget Value"));
          break;
        case 5:
          cell.setCellValue(new XSSFRichTextString("Encumbrance Amount"));
          break;
        case 6:
          cell.setCellValue(new XSSFRichTextString("Actual Amount"));
          break;
        case 7:
          cell.setCellValue(new XSSFRichTextString("Uninvoiced Amount"));
          break;
        case 8:
          cell.setCellValue(new XSSFRichTextString("Unpaid Invoice Amount"));
          break;
        case 9:
          cell.setCellValue(new XSSFRichTextString("Paid Invoice Amount"));
          break;
        case 10:
          cell.setCellValue(new XSSFRichTextString("Posted Invoice Amount"));
          break;
        case 11:
          cell.setCellValue(new XSSFRichTextString("Journal Amount"));
          break;
        case 12:
          cell.setCellValue(new XSSFRichTextString("Cost - Unpaid Invoice Amount"));
          break;
        case 13:
          cell.setCellValue(new XSSFRichTextString("Cost - Paid Invoice Amount"));
          break;
        case 14:
          cell.setCellValue(new XSSFRichTextString("Cost - Actual Amount"));
          break;
        case 15:
          cell.setCellValue(new XSSFRichTextString("Direct Encumbrance Amount"));
          break;
        case 16:
          cell.setCellValue(new XSSFRichTextString("Funds - Unpaid Invoice Amount"));
          break;
        case 17:
          cell.setCellValue(new XSSFRichTextString("Funds - Paid Invoice Amount"));
          break;
        case 18:
          cell.setCellValue(new XSSFRichTextString("Funds - Actual Amount"));
          break;
        case 19:
          cell.setCellValue(new XSSFRichTextString("Encumbrance Check"));
          break;
        case 20:
          cell.setCellValue(new XSSFRichTextString("Funds and Cost Encumbrance Check"));
          break;
        case 21:
          cell.setCellValue(new XSSFRichTextString("Legacy Spent Value"));
          break;

        }

      }
      int i = 1;
      List<EncumbranceSummaryDTO> rowDataList = EncumbranceReportDAO.getExcelSummaryData();

      for (EncumbranceSummaryDTO data : rowDataList) {
        XSSFRow dataRow = sheet.createRow(i);
        for (int j = 0; j <= noOfColumns; j++) {
          XSSFCell cell = dataRow.createCell(j);
          switch (j) {
          case 0:
            cell.setCellValue(new XSSFRichTextString(data.getBudgettype()));
            break;
          case 1:
            cell.setCellValue(new XSSFRichTextString(data.getAccount()));
            break;
          case 2:
            cell.setCellValue(new XSSFRichTextString(data.getUniquecode()));
            break;
          case 3:
            cell.setCellValue(data.getOriginal_budget().doubleValue());
            break;
          case 4:
            cell.setCellValue(data.getCurrent_budget().doubleValue());
            break;
          case 5:
            cell.setCellValue(data.getEncumbranceamount().doubleValue());
            break;
          case 6:
            cell.setCellValue(data.getActualamount().doubleValue());
            break;
          case 7:
            cell.setCellValue(data.getNotinvoicedamount().doubleValue());
            break;
          case 8:
            cell.setCellValue(data.getUnpaidinvoice().doubleValue());
            break;
          case 9:
            cell.setCellValue(data.getPaidinvoice().doubleValue());
            break;
          case 10:
            cell.setCellValue(data.getPostedinvoice().doubleValue());
            break;
          case 11:
            cell.setCellValue(data.getJournal_amount().doubleValue());
            break;
          case 12:
            cell.setCellValue(data.getUnpaidinvoicefromcost().doubleValue());
            break;
          case 13:
            cell.setCellValue(data.getPaidinvoicefromcost().doubleValue());
            break;
          case 14:
            cell.setCellValue(data.getCostActualAmount().doubleValue());
            break;
          case 15:
            cell.setCellValue(data.getDirectfundsencumbranceamount().doubleValue());
            break;
          case 16:
            cell.setCellValue(data.getFundsinvoiceunpaid().doubleValue());
            break;
          case 17:
            cell.setCellValue(data.getFundsinvoicepaid().doubleValue());
            break;
          case 18:
            cell.setCellValue(data.getFundsActualamount().doubleValue());
            break;
          case 19:
            cell.setCellValue(data.getEncumbrancecheck());
            break;
          case 20:
            cell.setCellValue(data.getFundscostencumbrancecheck());
            break;
          case 21:
            cell.setCellValue(data.getLegacy_spent().doubleValue());
            break;

          }
        }
        i++;
      }

      sheet.setAutoFilter(new CellRangeAddress(0, i, 0, 21));
      // Write workbook in file
      FileOutputStream fileOut = new FileOutputStream(fileName);
      workbook.write(fileOut);
      fileOut.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

    return file;

  }

}
