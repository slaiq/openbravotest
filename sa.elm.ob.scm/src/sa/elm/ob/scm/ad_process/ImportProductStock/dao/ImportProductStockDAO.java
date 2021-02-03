package sa.elm.ob.scm.ad_process.ImportProductStock.dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;

import sa.elm.ob.utility.util.Utility;

/**
 * @author Divya
 * 
 */

public class ImportProductStockDAO {
  /**
   * Servlet implementation class Import Product Stock in Phsical Inventory
   */
  private static Logger log4j = Logger.getLogger(ImportProductStockDAO.class);

  // Function to insert the records in transaction screens.

  public JSONObject processUploadedCsvFile(String filePath, String inventoryId,
      VariablesSecureApp vars) {

    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;

    try {
      OBContext.setAdminMode(true);
      FileReader inpFile = new FileReader(filePath);
      BufferedReader inpReader = new BufferedReader(inpFile);
      String inpLine = "", inpDelimiter = "";
      final String firstLineHeader = "Y";

      if (StringUtils.equals(firstLineHeader, "Y")) {
        inpLine = inpReader.readLine();
        if (inpLine.contains(","))
          inpDelimiter = ",";
        else if (inpLine.contains(";"))
          inpDelimiter = ";";
      }
      Long line = (long) 0;
      InventoryCount header = OBDal.getInstance().get(InventoryCount.class, inventoryId);
      while ((inpLine = inpReader.readLine()) != null) {
        line += 1;
        List<String> result = parseCSV(inpLine, inpDelimiter);

        if (log4j.isDebugEnabled())
          log4j.debug(result);

        if (result == null)
          continue;

        String fields[] = (String[]) result.toArray(new String[0]);

        for (int i = 0; i < fields.length; i++) {
          fields[i] = fields[i].replace("\"", "");
        }
        // Inserting Data into inventoryline table
        isSuccess = insertIntoProductDetail(fields, header, vars, line);
        log4j.debug("isSuccess" + isSuccess);
        if (isSuccess == 0) {
          break;
        }
        result.clear();
      }
      OBDal.getInstance().flush();
      inpReader.close();
      inpFile.close();

      if (isSuccess == 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", "Record Insert Failed");
      } else {
        jsonresult = new JSONObject();
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", "Records Inserted Succesfully");
      }
    } catch (Exception e) {
      isSuccess = 0;
      e.printStackTrace();
      jsonresult = new JSONObject();
      try {
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", "");
        jsonresult.put("statusMessage", e.getMessage());
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      log4j.error("Exception in Product Data Import", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonresult;
  }

  @SuppressWarnings({ "unchecked", "unused" })
  private int insertIntoProductDetail(String[] fields, InventoryCount header,
      VariablesSecureApp vars, Long line) {
    int status = 1;
    String defaultBinId = "";
    Locator locator = null;
    int issuccess = 0;
    try {
      // select excempt tax
      Client objClient = OBContext.getOBContext().getCurrentClient();
      TaxCategory objTaxCategory = null;
      OBQuery<TaxCategory> objQuery = OBDal.getInstance().createQuery(TaxCategory.class,
          "as e where e.client.id='" + objClient.getId() + "' ");
      objQuery.setMaxResult(1);
      if (objQuery.list().size() > 0) {
        objTaxCategory = objQuery.list().get(0);
      }
      // START of Product Master

      log4j.debug("Inserting of Product Master" + fields[0]);
      String selectProduct = " select prd.m_product_id,uom.c_uom_id from m_product  prd left join c_uom uom on uom.c_uom_id= prd.c_uom_id where prd.name = '"
          + fields[0] + "' ";
      SQLQuery productQuery = OBDal.getInstance().getSession().createSQLQuery(selectProduct);
      // productQuery.setParameter(0, fields[0]);
      log4j.debug("objectslist.size() >" + productQuery.toString());
      List<Object[]> objectslist = (ArrayList<Object[]>) productQuery.list();
      log4j.debug("objectslist.size() >" + objectslist.size());

      if (productQuery != null && objectslist.size() > 0) {
        for (int i = 0; i < objectslist.size(); i++) {
          Object[] objects = objectslist.get(i);
          InventoryCountLine Invline = null;
          defaultBinId = Utility.GetDefaultBin(header.getWarehouse().getId());
          if (defaultBinId.equals("")) {
            if (header.getWarehouse().getLocatorList().size() > 0) {
              locator = header.getWarehouse().getLocatorList().get(0);
            } else {
              return 0;
            }
          } else
            locator = OBDal.getInstance().get(Locator.class, defaultBinId);
          log4j.debug("locator" + locator);
          Invline = OBProvider.getInstance().get(InventoryCountLine.class);
          Invline.setOrganization(header.getOrganization());
          Invline.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          Invline.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          Invline.setPhysInventory(header);
          Invline.setLineNo(line);
          Invline.setProduct(OBDal.getInstance().get(Product.class, objects[0].toString()));
          Invline.setEscmItemdesc(fields[0]);
          Invline.setUOM(OBDal.getInstance().get(UOM.class, objects[1].toString()));
          Invline.setStorageBin(locator);
          Invline.setQuantityCount(new BigDecimal(fields[1]));
          // Invline.setQuantityCount(BigDecimal.ZERO);
          OBDal.getInstance().save(Invline);
          OBDal.getInstance().flush();
          log4j.debug("Invline" + Invline.getId());
          // End of Product
          return 1;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      status = 0;
      // TODO: handle exception
    }
    return status;

  }

  // Parsing .csv file using regular Expression

  private List<String> parseCSV(String csv, String delim) {

    final Pattern NEXT_COLUMN = nextColumnRegex(delim);
    final List<String> strings = new ArrayList<String>();
    final Matcher matcher = NEXT_COLUMN.matcher(csv);

    while (!matcher.hitEnd() && matcher.find()) {
      String match = matcher.group(1);
      strings.add(match);
    }

    return strings;
  }

  private Pattern nextColumnRegex(String separator) {

    String unquoted = "(:?[^\"" + separator + "]|\"\")*";
    String ending = "(:?" + separator + "|$)";
    return Pattern.compile('(' + unquoted + ')' + ending);
  }

  @SuppressWarnings("unused")
  public JSONObject processValidateCsvFile(String filePath, String inventoryId) {
    JSONObject jsonresult = new JSONObject();
    try {
      OBContext.setAdminMode(true);
      int lineNo = 1, duplicateline = 1;
      int failed = 0;
      InventoryCount header = OBDal.getInstance().get(InventoryCount.class, inventoryId);

      FileReader inpFile = new FileReader(filePath);
      BufferedReader inpReader = new BufferedReader(inpFile);
      String inpLine = "", inpDelimiter = "";
      final String firstLineHeader = "Y";
      String defaultBinId = "";

      Locator locator = null;
      StringBuffer description = new StringBuffer();
      HashSet<String> lines = new HashSet<String>();
      HashSet<String> lines1 = new HashSet<String>();
      if (StringUtils.equals(firstLineHeader, "Y")) {
        inpLine = inpReader.readLine();
        if (inpLine.contains(","))
          inpDelimiter = ",";
        else if (inpLine.contains(";"))
          inpDelimiter = ";";
      }
      String br = "";
      while ((inpLine = inpReader.readLine()) != null) {
        lineNo += 1;
        List<String> result = parseCSV(inpLine, inpDelimiter);

        if (log4j.isDebugEnabled())
          log4j.debug(result);

        if (result == null)
          continue;
        String fields[] = (String[]) result.toArray(new String[0]);

        for (int i = 0; i < fields.length; i++) {
          fields[i] = fields[i].replace("\"", "");
        }
        log4j.debug("fields[0]:" + fields[0]);
        log4j.debug("fields[1]:" + fields[1]);
        // if(failed>1)
        description.append(br);
        if (fields.length < 2) {
          failed += 1;
          description
              .append("Partial record at line No.<b>" + lineNo + "</b> in the data file.<br>");
          br = "<br>";
        } else if (fields.length > 2) {
          failed += 1;
          description.append("Line No.<b>" + lineNo
              + "</b> in the data file has more fields than expected(2).<br>");
          br = "<br>";
        } else {
          br = "";
          if (StringUtils.isEmpty(fields[0])) {
            failed += 1;
            description.append(
                "Item Description is empty at line No.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String prdName = fields[0];
            OBCriteria<Product> prdListCriteria = OBDal.getInstance().createCriteria(Product.class);
            prdListCriteria.add(Restrictions.ilike(Product.PROPERTY_NAME, prdName));
            prdListCriteria.setMaxResults(1);
            List<Product> prdlist = prdListCriteria.list();
            if (!(prdlist.size() > 0)) {// Product not found
              failed += 1;
              description.append("Product <b>" + prdName + "</b> at line No.<b>" + lineNo
                  + "</b> not found in Master Data.<br>");
              br = "<br>";
            }
          }
          if (StringUtils.isEmpty(fields[1])) {
            failed += 1;
            description.append(
                "Stock Quantity  is empty at line no.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          }

          // chk duplicate
          String Product = fields[0];
          if (lines.add(Product)) {

          } else {
            lines1.add(Product);
          }
        }
        result.clear();
      }
      for (String s : lines1) {
        description.append(br);
        failed += 1;
        description.append("Product: <b>" + s + " is duplicate </b> in the data file.<br>");
        br = "<br>";
      }

      if (failed > 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("recordsFailed", Integer.toString(failed));
        jsonresult.put("statusMessage", description);
      } else {
        jsonresult.put("status", "1");
        jsonresult.put("recordsFailed", Integer.toString(failed));
        jsonresult.put("statusMessage", "CSV Validated Succesfully");
      }
      inpReader.close();
      inpFile.close();

    } catch (Exception e) {
      log4j.error("Exception in Import Product Stock CSV Validate", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonresult;

  }
}
