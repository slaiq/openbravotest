package sa.elm.ob.finance.iban;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class SuppliersApplication extends DalBaseProcess {

  private String FILE_NAME = getPoolPropertiesConfig().getProperty("attach.path")
      + "/suppliers-pro-updated.xlsx";
  public static ArrayList<Supplier> suppliers = new ArrayList<>();
  private Properties poolPropertiesConfig;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    try {
      OBError obError = new OBError();
      FileInputStream excelFile = new FileInputStream(new File(FILE_NAME));
      Workbook workbook = new XSSFWorkbook(excelFile);
      Sheet datatypeSheet = workbook.getSheetAt(0);
      Iterator<Row> iterator = datatypeSheet.iterator();
      Connection conn = OBDal.getInstance().getConnection();

      while (iterator.hasNext()) {

        Row currentRow = iterator.next();
        Iterator<Cell> cellIterator = currentRow.iterator();
        Supplier supplier = new Supplier(cellIterator.next().getStringCellValue(),
            (int) cellIterator.next().getNumericCellValue(),
            cellIterator.next().getStringCellValue(),
            (int) cellIterator.next().getNumericCellValue());

        String bankQuery = "";

        if (supplier.getBankId() < 10)
          bankQuery = "select efin_bank_id from efin_bank where value = '0" + supplier.getBankId()
              + "';";
        else if (supplier.getBankId() == 65)
          bankQuery = "select efin_bank_id from efin_bank where value = '56';";
        else
          bankQuery = "select efin_bank_id from efin_bank where value = '" + supplier.getBankId()
              + "';";

        ResultSet bankRs = conn.prepareStatement(bankQuery).executeQuery();

        while (bankRs.next()) {
          supplier.setBankKey(bankRs.getString("efin_bank_id"));
        }

        String query = "select ec.c_bpartner_id from escm_certificates ec where ec.certificatenumber = '"
            + supplier.getCrNumber() + "';";

        ResultSet rs = conn.prepareStatement(query).executeQuery();

        while (rs.next()) {
          supplier.setId(rs.getString("c_bpartner_id"));
        }
        suppliers.add(supplier);
      }

      String insertQuery = "";
      for (Supplier s : suppliers) {
        insertQuery = "insert into c_bp_bankaccount (c_bp_bankaccount_id, ad_client_id, ad_org_id, c_bpartner_id, iban, em_efin_bank_id,  bankformat, c_country_id, createdby, updatedby) values (get_uuid(), 'FFEFCBB01E1F412886CB69CDBDD81774', 'D67E1FAA6B9445758EE62BAB1A211C3A', '"
            + s.getId() + "','" + s.getIban() + "','" + s.getBankKey()
            + "', 'IBAN','296', '100', '100');";
        conn.prepareStatement(insertQuery).executeUpdate();
      }
      obError.setType("Success");
      obError.setTitle("Success");
      obError.setMessage(OBMessageUtils.messageBD("ProcessOK"));
      bundle.setResult(obError);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public Properties getPoolPropertiesConfig() {
    if (null == poolPropertiesConfig)
      return OBPropertiesProvider.getInstance().getOpenbravoProperties();
    else
      return poolPropertiesConfig;
  }

}
