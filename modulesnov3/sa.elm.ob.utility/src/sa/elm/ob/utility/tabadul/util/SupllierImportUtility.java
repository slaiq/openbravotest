package sa.elm.ob.utility.tabadul.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.actionHandler.SupplierController;

/**
 * Temporary utility class for Supplier Import from excel sheet
 * 
 * @author mrahim
 *
 */
public class SupllierImportUtility extends DalBaseProcess {

  private static final Logger log = LoggerFactory.getLogger(SupllierImportUtility.class);
  private SupplierController supplierController;
  private Properties poolPropertiesConfig;
  private static String SUPPLIER_IMPORT_FILE_PATH = "supplier.import.file.path";
  private static String SUPPLIER_IMPORT_FILE_RESULT_PATH = "supplier.import.file.result";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    OBError obError = new OBError();
    supplierController = new SupplierController();
    BufferedWriter bw = null;
    FileWriter fw = null;
    // Call the service to update purchases from tabadul
    try {
      // Read from excel file the records one by one
      final String attachmentAbsolutePath = getPoolPropertiesConfig()
          .getProperty(SUPPLIER_IMPORT_FILE_PATH);
      final String attachmentresultFilePath = getPoolPropertiesConfig()
          .getProperty(SUPPLIER_IMPORT_FILE_RESULT_PATH);

      fw = new FileWriter(attachmentresultFilePath);
      bw = new BufferedWriter(fw);

      FileInputStream excelFile = new FileInputStream(new File(attachmentAbsolutePath));

      Workbook workbook = new XSSFWorkbook(excelFile);
      Sheet datatypeSheet = workbook.getSheetAt(0);
      Iterator<Row> iterator = datatypeSheet.iterator();
      int rowNum = 0;
      String crn = null;
      while (iterator.hasNext()) {

        try {
          Row currentRow = iterator.next();
          if (rowNum == 0) {
            rowNum++;
            continue;
          }
          Cell cell = currentRow.getCell(1);
          crn = cell.getStringCellValue();

          supplierController.importSupplierByCRN(crn);
          log.info("Supplier Import Successful");
        } catch (Exception e) {
          bw.write(crn);
          bw.append(",");
          bw.append(e.getMessage());
          bw.append("\n");

          log.error("Exception : CRN : " + crn + " Exception : ", e.getMessage());
          // Put the failed records in other file
        }

      }
      obError.setType("Success");
      obError.setTitle("Success");
      obError.setMessage(OBMessageUtils.messageBD("ProcessOK"));

    } catch (Exception e) {
      log.error("Exception in BidPurchasesUpdateScheduler ", e.getMessage());
      obError.setType("Error");
      obError.setTitle("Error");
      obError.setMessage(OBMessageUtils.messageBD("EUT_TABADUL.ERROR.INTERNAL_ERROR"));
    } finally {
      try {

        if (bw != null)
          bw.close();

        if (fw != null)
          fw.close();

      } catch (IOException ex) {

      }

    }

    bundle.setResult(obError);
  }

  public Properties getPoolPropertiesConfig() {
    if (null == poolPropertiesConfig)
      return OBPropertiesProvider.getInstance().getOpenbravoProperties();
    else
      return poolPropertiesConfig;
  }

}
