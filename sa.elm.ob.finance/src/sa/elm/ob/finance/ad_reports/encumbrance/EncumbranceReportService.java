package sa.elm.ob.finance.ad_reports.encumbrance;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public interface EncumbranceReportService {

  public Workbook createSheet1(XSSFWorkbook workbook, String fileName)
      throws FileNotFoundException, Exception;

  public File createSheet2(XSSFWorkbook workbook, String fileName)
      throws FileNotFoundException, Exception;

}
