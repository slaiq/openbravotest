package sa.elm.ob.scm.ad_process.RequisitionLineUploader.dao;

import java.io.File;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 * 
 * @author Gopalakrishnan on 06/11/2017
 *
 */
public interface RequisitionLineUploaderDAO {

  /**
   * 
   * @param file
   * @param vars
   * @param inpRequistionId
   * @return success once file processed
   */
  public JSONObject processUploadedCsvFile(File file, VariablesSecureApp vars,
      String inpRequistionId) throws Exception;

  /**
   * 
   * @param file
   * @param vars
   * @param inpBudgetId
   * @return success valid xlsx file
   */
  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars, String inpBudgetId)
      throws Exception;

  /**
   * 
   * @param cell
   * @return value of cell based on type
   */
  public String getCellValue(Cell cell) throws Exception;

  /**
   * Parsing .xlsx file using regular Expression
   */

  public List<String> parseExcel(Row row) throws Exception;

  /**
   * 
   * @param fields
   * @param inpRequistionId
   * @return success when data inserted in active table
   */
  public int importDataInDatabase(String[] fields, String inpRequistionId) throws Exception;

  /**
   * 
   * @param inpRequistionId
   * @return success once requisition line inserted
   */
  public int insertIntoRequisitionLine(String inpRequistionId) throws Exception;

  /**
   * 
   * @param inpRequistionId
   * @return return success once updated lines to frame tree
   */
  public int updateRequisitionLine(String inpRequistionId) throws Exception;

  public boolean isRowEmpty(Row row) throws Exception;
}