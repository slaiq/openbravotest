package sa.elm.ob.scm.ad_forms.NonCustodyUpload.dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxCategory;

import sa.elm.ob.scm.BeneficiaryView;
import sa.elm.ob.scm.CsvNoncustodyImport;
import sa.elm.ob.scm.ESCMProductCategoryV;
import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;

/**
 * @author Gopalakrishnan on 02/05/2017
 */

public class NonCustodyUploadTemplateDAO {
  /**
   * Servlet implementation class of NonCustodyUploadTemplate
   */

  private static final Logger log4j = Logger.getLogger(NonCustodyUploadTemplateDAO.class);

  // Function to insert the records in transaction screens.

  public JSONObject processUploadedCsvFile(String filePath) {

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
      /*
       * String documentNo = getSequenceNo(OBContext.getOBContext().getCurrentClient().getId(),
       * "Document_Sequence_CSVImport", true);
       */
      String documentNo = "123456"; // doc no to identify temp table

      while ((inpLine = inpReader.readLine()) != null) {

        List<String> result = parseCSV(inpLine, inpDelimiter);

        if (log4j.isDebugEnabled())
          log4j.debug(result);

        if (result == null)
          continue;

        String fields[] = (String[]) result.toArray(new String[0]);

        for (int i = 0; i < fields.length; i++) {
          fields[i] = fields[i].replace("\"", "");
        }
        // Inserting Data into temp table
        isSuccess = importDataInDatabase(fields, documentNo);
        result.clear();
      }
      OBDal.getInstance().flush();
      inpReader.close();
      inpFile.close();

      if (isSuccess == 1) {
        // All columns in xls are inserted in temp table (AbsCsvProductImport), now actual records
        // are
        // created
        // Inserting product detail.
        isSuccess = insertIntoProductDetail(documentNo);
        // if(isSuccess ==1){
        // all records whether inserted successfully or not, delete the records from temp table

        OBCriteria<CsvNoncustodyImport> csvTable = OBDal.getInstance()
            .createCriteria(CsvNoncustodyImport.class);
        csvTable.add(Restrictions.eq(CsvNoncustodyImport.PROPERTY_DOCUMENTNO, documentNo));
        List<CsvNoncustodyImport> csvTableList = csvTable.list();
        for (CsvNoncustodyImport delcsvObj : csvTableList) {
          OBDal.getInstance().remove(delcsvObj);
        }
        // }

      }
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
      OBContext.setAdminMode(false);
    }
    return jsonresult;
  }

  @SuppressWarnings({ "unchecked", "unused" })
  private int insertIntoProductDetail(String documentNo) {
    int status = 1;
    try {
      // select Tag No
      Client objClient = OBContext.getOBContext().getCurrentClient();
      Organization objOrg = OBContext.getOBContext().getCurrentOrganization();
      String existingDocNo = "1000000001";
      // get recent tag number
      OBQuery<MaterialIssueRequestCustody> objCustodyQry = OBDal.getInstance().createQuery(
          MaterialIssueRequestCustody.class,
          "as e where e.organization.id='" + objOrg.getId() + "' order by creationDate desc");
      objCustodyQry.setMaxResult(1);
      if (objCustodyQry.list().size() > 0) {
        MaterialIssueRequestCustody recentObj = objCustodyQry.list().get(0);
        if (recentObj.getDocumentNo() != null && StringUtils.isNotEmpty(recentObj.getDocumentNo()))
          existingDocNo = String.valueOf(Integer.parseInt(recentObj.getDocumentNo()) + 1);
      }
      TaxCategory objTaxCategory = null;
      OBQuery<TaxCategory> objQuery = OBDal.getInstance().createQuery(TaxCategory.class,
          "as e where e.client.id='" + objClient.getId() + "' ");
      objQuery.setMaxResult(1);
      if (objQuery.list().size() > 0) {
        objTaxCategory = objQuery.list().get(0);
      }
      // START of Product Master
      // log4j.debug("Inserting of Product Master" + documentNo);
      String selectProduct = " select bp.c_bpartner_id,prd.m_product_id ," + " csv.qty as qty "
          + " from escm_csv_noncustody_import csv "
          + " left join c_bpartner bp on bp.value ilike csv.employeeno "
          + " left join m_product prd on prd.name ilike itemdesc " + " where documentno=? ";

      SQLQuery productQuery = OBDal.getInstance().getSession().createSQLQuery(selectProduct);
      productQuery.setParameter(0, documentNo);

      List<Object[]> objectslist = (ArrayList<Object[]>) productQuery.list();
      if (productQuery != null && objectslist.size() > 0) {
        for (int i = 0; i < objectslist.size(); i++) {
          log4j.debug("Line No:" + i + 1);
          Object[] objects = objectslist.get(i);
          // get product
          Product objProduct = OBDal.getInstance().get(Product.class, objects[1].toString());
          for (int j = 0; j < Integer.valueOf(objects[2].toString()); j++) {
            MaterialIssueRequestCustody objCustody = OBProvider.getInstance()
                .get(MaterialIssueRequestCustody.class);
            objCustody.setBeneficiaryType("E");
            objCustody.setBeneficiaryIDName(
                OBDal.getInstance().get(BeneficiaryView.class, objects[0].toString()));
            objCustody.setProduct(objProduct);
            objCustody.setDescription(objProduct.getName());
            objCustody.setQuantity(BigDecimal.ONE);
            ESCMProductCategoryV prdcat = OBDal.getInstance().get(ESCMProductCategoryV.class,
                objProduct.getProductCategory().getId());
            objCustody.setProductCategory(prdcat);
            objCustody.setAttributeSet(objProduct.getEscmCusattribute());
            objCustody.setAlertStatus("IU");
            objCustody.setProcurement("N");
            objCustody.setDocumentNo(existingDocNo);
            OBDal.getInstance().save(objCustody);
            OBDal.getInstance().flush();
            // insert Transaction
            Escm_custody_transaction objTransaction = OBProvider.getInstance()
                .get(Escm_custody_transaction.class);
            objTransaction.setEscmMrequestCustody(objCustody);
            objTransaction.setBtype("E");
            objTransaction
                .setBname(OBDal.getInstance().get(BeneficiaryView.class, objects[0].toString()));
            objTransaction.setLine2(Long.valueOf(10));
            objTransaction.setLineNo(Long.valueOf(10));
            objTransaction.setTransactiontype("IE");
            objTransaction.setTransactionreason("Initial Data Loaded");
            objTransaction.setTransactionDate(new Date());
            objTransaction.setProcessed(true);
            OBDal.getInstance().save(objTransaction);
            OBDal.getInstance().flush();
            existingDocNo = String.valueOf(Integer.parseInt(existingDocNo) + 1);
          }

          // End of Custody
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      status = 0;
      // TODO: handle exception
    }
    return status;

  }

  private int importDataInDatabase(String[] fields, String documentNo) {
    int isSuccess = 1;

    try {

      CsvNoncustodyImport csvImport = null;

      csvImport = OBProvider.getInstance().get(CsvNoncustodyImport.class);
      csvImport.setDocumentNo(documentNo);
      csvImport.setEmployeeno(fields[2]);
      csvImport.setEmployeename(fields[3]);
      csvImport.setItemdesc(fields[0]);
      csvImport.setQuantity(Long.valueOf(fields[1].toString()));
      OBDal.getInstance().save(csvImport);
      // Employee Number //employee name
      // Item description /
    } catch (Exception e) {
      isSuccess = 0;
      e.printStackTrace();
    }
    return isSuccess;

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

    // String unquoted = "(:?[^\"" + separator + "]|\"\")*";
    String unquoted = "(?:(?<=\")([^\"]*)(?=\"))|(?<=" + separator + "|^)([^" + separator + "]*)";
    String ending = "(:?" + separator + "|$)";
    return Pattern.compile('(' + unquoted + ')' + ending);
  }

  public JSONObject processValidateCsvFile(String filePath) {
    JSONObject jsonresult = new JSONObject();
    try {
      OBContext.setAdminMode(true);
      int lineNo = 1;
      int failed = 0;

      FileReader inpFile = new FileReader(filePath);
      BufferedReader inpReader = new BufferedReader(inpFile);
      String inpLine = "", inpDelimiter = "";
      final String firstLineHeader = "Y";
      StringBuffer description = new StringBuffer();
      // read header fields
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
        // if(failed>1)
        // log4j.debug("fields.length:" + fields.length);
        description.append(br);
        if (fields.length < 4) {
          failed += 1;
          description
              .append("Partial record at line No.<b>" + lineNo + "</b> in the data file.<br>");
          br = "<br>";
        } else if (fields.length > 4) {
          failed += 1;
          description.append("Line No.<b>" + lineNo
              + "</b> in the data file has more fields than expected(4).<br>");
          br = "<br>";
        } else {
          br = "";
          if (StringUtils.isEmpty(fields[2])) {
            failed += 1;
            description.append(
                "Employee Number is empty at line No.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[3])) {
            failed += 1;
            description.append(
                "Employee Name is empty at line no.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String employee = fields[2];
            OBCriteria<BusinessPartner> empListCriteria = OBDal.getInstance()
                .createCriteria(BusinessPartner.class);
            empListCriteria.add(Restrictions.ilike(BusinessPartner.PROPERTY_SEARCHKEY, employee));
            empListCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
            empListCriteria.setMaxResults(1);
            List<BusinessPartner> empList = empListCriteria.list();
            if (!(empList.size() > 0)) {// EmpList not found
              failed += 1;
              description.append("Employee <b>" + fields[3] + "</b> at line No.<b>" + lineNo
                  + "</b> not found in Master Data.<br>");
              br = "<br>";
            }
          }
          if (StringUtils.isEmpty(fields[1])) {
            failed += 1;
            description
                .append("Quantity is empty at line No.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[0])) {
            failed += 1;
            description.append(
                "Item description is empty at line No.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String productName = fields[0];
            OBCriteria<Product> productListCriteria = OBDal.getInstance()
                .createCriteria(Product.class);
            productListCriteria.add(Restrictions.ilike(Product.PROPERTY_NAME, productName));
            productListCriteria.setMaxResults(1);
            productListCriteria.setFilterOnActive(false);
            List<Product> productlist = productListCriteria.list();
            if (!(productlist.size() > 0)) {// Product not found
              failed += 1;
              description.append("Product <b>" + productName + "</b> at line No.<b>" + lineNo
                  + "</b> not exists in Master Data.<br>");
              br = "<br>";
            }
          }

        }
        result.clear();
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
      log4j.error("Exception in Non Custody Upload CSV Validate", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonresult;

  }

}
