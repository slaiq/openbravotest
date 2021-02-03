package sa.elm.ob.scm.ad_forms.CustodyUpload.dao;

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
import org.openbravo.model.common.plm.Attribute;
import org.openbravo.model.common.plm.AttributeInstance;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.AttributeValue;
import org.openbravo.model.common.plm.Product;

import sa.elm.ob.scm.BeneficiaryView;
import sa.elm.ob.scm.CsvCustodyImport;
import sa.elm.ob.scm.ESCMProductCategoryV;
import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;

/**
 * @author Gopalakrishnan on 27/04/2017
 */

public class CustodyUploadTemplateDAO {
  /**
   * Servlet implementation class of CustodyUploadTemplate
   */

  private static final Logger log4j = Logger.getLogger(CustodyUploadTemplateDAO.class);

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
      JSONObject jsonHeaderresult = new JSONObject();

      if (StringUtils.equals(firstLineHeader, "Y")) {
        inpLine = inpReader.readLine();
        if (inpLine.contains(","))
          inpDelimiter = ",";
        else if (inpLine.contains(";"))
          inpDelimiter = ";";
        List<String> resultHead = parseCSV(inpLine, inpDelimiter);
        String fieldsHead[] = (String[]) resultHead.toArray(new String[0]);
        for (int i = 0; i < fieldsHead.length; i++) {
          fieldsHead[i] = fieldsHead[i].replace("\"", "");
        }
        jsonHeaderresult.put("brand", fieldsHead[6]);
        jsonHeaderresult.put("year", fieldsHead[5]);
        jsonHeaderresult.put("cylinder", fieldsHead[8]);
        jsonHeaderresult.put("oiltype", fieldsHead[9]);
        jsonHeaderresult.put("color", fieldsHead[10]);
        jsonHeaderresult.put("plate", fieldsHead[2]);
        jsonHeaderresult.put("chasis", fieldsHead[3]);
        jsonHeaderresult.put("cusDesc", fieldsHead[7]);
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
        isSuccess = insertIntoProductDetail(documentNo, jsonHeaderresult);
        // if(isSuccess ==1){
        // all records whether inserted successfully or not, delete the records from temp table

        OBCriteria<CsvCustodyImport> csvTable = OBDal.getInstance()
            .createCriteria(CsvCustodyImport.class);
        csvTable.add(Restrictions.eq(CsvCustodyImport.PROPERTY_DOCUMENTNO, documentNo));
        List<CsvCustodyImport> csvTableList = csvTable.list();
        for (CsvCustodyImport delcsvObj : csvTableList) {
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

  @SuppressWarnings("unchecked")
  private int insertIntoProductDetail(String documentNo, JSONObject jsonHeaderresult) {
    int status = 1;
    try {

      // select excempt tax
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
      // TaxCategory objTaxCategory = null;
      // OBQuery<TaxCategory> objQuery = OBDal.getInstance().createQuery(TaxCategory.class,
      // "as e where e.client.id='" + objClient.getId() + "' ");
      // objQuery.setMaxResult(1);
      // if (objQuery.list().size() > 0) {
      // objTaxCategory = objQuery.list().get(0);
      // }
      // START of Product Master
      log4j.debug("Inserting of Product Master" + documentNo);
      String selectProduct = " select csv.tagno,bp.c_bpartner_id,csv.cusdesc,prd.m_product_id ,"
          + "csv.modelyear,csv.brand,csv.cylinder,csv.oiltype,csv.color,csv.platenumber,csv.chasisnumber "
          + " from escm_csv_custody_import csv "
          + " left join c_bpartner bp on bp.value ilike csv.employeeno and isemployee='Y' and bp.ad_client_id='"
          + objClient.getId() + "' "
          + " left join m_product prd on prd.name ilike itemdesc and  prd.ad_client_id='"
          + objClient.getId() + "'" + " where documentno=? and csv.ad_client_id='"
          + objClient.getId() + "'";
      SQLQuery productQuery = OBDal.getInstance().getSession().createSQLQuery(selectProduct);
      productQuery.setParameter(0, documentNo);

      List<Object[]> objectslist = (ArrayList<Object[]>) productQuery.list();
      if (productQuery != null && objectslist.size() > 0) {
        for (int i = 0; i < objectslist.size(); i++) {
          Object[] objects = objectslist.get(i);
          // get product
          Product objProduct = OBDal.getInstance().get(Product.class, objects[3].toString());
          // create attribute instance
          AttributeSetInstance objAttributeSet = OBProvider.getInstance()
              .get(AttributeSetInstance.class);
          objAttributeSet.setDescription(objects[10].toString().concat("_" + objects[9].toString())
              .concat("_" + objects[4].toString()).concat("_" + objects[5].toString())
              .concat("_" + objects[6].toString()).concat("_" + objects[7].toString())
              .concat("_" + objects[8].toString()).concat("_" + objects[2].toString()));
          objAttributeSet.setAttributeSet(objProduct.getEscmCusattribute());
          OBDal.getInstance().save(objAttributeSet);
          OBDal.getInstance().flush();
          // create instance
          if (objProduct.getEscmCusattribute() != null) {
            String selectAttribute = " select attrset.m_attributeset_id,attr.m_attribute_id,attrvalue.m_attributevalue_id,"
                + " attrvalue.value from m_attributeset  attrset "
                + " join m_attributeuse uattr on uattr.m_attributeset_id=attrset.m_attributeset_id "
                + " join m_attribute attr on attr.m_attribute_id= uattr.m_attribute_id "
                + "   join m_attributevalue attrvalue on attrvalue.m_attribute_id=attr.m_attribute_id "
                + "   where attrvalue.name= ? and lower(attr.name)=?"
                + "   and attrset.m_attributeset_id=? limit 1";
            // Model year

            /*
             * SQLQuery attributeQuery = OBDal.getInstance().getSession()
             * .createSQLQuery(selectAttribute); attributeQuery.setParameter(0,
             * objects[4].toString()); attributeQuery.setParameter(1, "Model Year".toLowerCase());
             * attributeQuery.setParameter(2, objProduct.getEscmCusattribute().getId());
             * 
             * List<Object[]> objectsYearlist = (ArrayList<Object[]>) attributeQuery.list(); if
             * ((objectsYearlist.size() > 0)) { Object[] objectsYear = objectsYearlist.get(0);
             * AttributeInstance objInstance =
             * OBProvider.getInstance().get(AttributeInstance.class);
             * objInstance.setAttribute(OBDal.getInstance().get(Attribute.class,
             * objectsYear[1].toString()));
             * objInstance.setAttributeValue(OBDal.getInstance().get(AttributeValue.class,
             * objectsYear[2].toString())); objInstance.setAttributeSetValue(objAttributeSet);
             * OBDal.getInstance().save(objInstance); }
             */

            // brand

            SQLQuery attributeBrandQuery = OBDal.getInstance().getSession()
                .createSQLQuery(selectAttribute);
            attributeBrandQuery.setParameter(0, objects[5].toString());
            attributeBrandQuery.setParameter(1,
                jsonHeaderresult.get("brand").toString().toLowerCase());
            attributeBrandQuery.setParameter(2, objProduct.getEscmCusattribute().getId());

            List<Object[]> objectsBrandlist = (ArrayList<Object[]>) attributeBrandQuery.list();
            if ((objectsBrandlist.size() > 0)) {
              Object[] objectsYear = objectsBrandlist.get(0);
              AttributeInstance objInstance = OBProvider.getInstance().get(AttributeInstance.class);
              objInstance.setAttribute(
                  OBDal.getInstance().get(Attribute.class, objectsYear[1].toString()));
              objInstance.setAttributeValue(
                  OBDal.getInstance().get(AttributeValue.class, objectsYear[2].toString()));
              objInstance.setAttributeSetValue(objAttributeSet);
              OBDal.getInstance().save(objInstance);
            }

            // Cylinder

            SQLQuery attributeCycQuery = OBDal.getInstance().getSession()
                .createSQLQuery(selectAttribute);
            attributeCycQuery.setParameter(0, objects[6].toString());
            attributeCycQuery.setParameter(1,
                jsonHeaderresult.get("cylinder").toString().toLowerCase());
            attributeCycQuery.setParameter(2, objProduct.getEscmCusattribute().getId());

            List<Object[]> objectsCyclist = (ArrayList<Object[]>) attributeCycQuery.list();
            if ((objectsCyclist.size() > 0)) {
              Object[] objectsYear = objectsCyclist.get(0);
              AttributeInstance objInstance = OBProvider.getInstance().get(AttributeInstance.class);
              objInstance.setAttribute(
                  OBDal.getInstance().get(Attribute.class, objectsYear[1].toString()));
              objInstance.setAttributeValue(
                  OBDal.getInstance().get(AttributeValue.class, objectsYear[2].toString()));
              objInstance.setAttributeSetValue(objAttributeSet);
              OBDal.getInstance().save(objInstance);
            }

            // Oil Type
            SQLQuery attributeOilQuery = OBDal.getInstance().getSession()
                .createSQLQuery(selectAttribute);
            attributeOilQuery.setParameter(0, objects[7].toString());
            attributeOilQuery.setParameter(1,
                jsonHeaderresult.get("oiltype").toString().toLowerCase());
            attributeOilQuery.setParameter(2, objProduct.getEscmCusattribute().getId());

            List<Object[]> objectsOillist = (ArrayList<Object[]>) attributeOilQuery.list();
            if ((objectsOillist.size() > 0)) {
              Object[] objectsYear = objectsOillist.get(0);
              AttributeInstance objInstance = OBProvider.getInstance().get(AttributeInstance.class);
              objInstance.setAttribute(
                  OBDal.getInstance().get(Attribute.class, objectsYear[1].toString()));
              objInstance.setAttributeValue(
                  OBDal.getInstance().get(AttributeValue.class, objectsYear[2].toString()));
              objInstance.setAttributeSetValue(objAttributeSet);
              OBDal.getInstance().save(objInstance);
            }

            // Color

            SQLQuery attributeColorQuery = OBDal.getInstance().getSession()
                .createSQLQuery(selectAttribute);
            attributeColorQuery.setParameter(0, objects[8].toString());
            attributeColorQuery.setParameter(1,
                jsonHeaderresult.get("color").toString().toLowerCase());
            attributeColorQuery.setParameter(2, objProduct.getEscmCusattribute().getId());

            List<Object[]> objectsColorlist = (ArrayList<Object[]>) attributeColorQuery.list();
            if ((objectsColorlist.size() > 0)) {
              Object[] objectsYear = objectsColorlist.get(0);
              AttributeInstance objInstance = OBProvider.getInstance().get(AttributeInstance.class);
              objInstance.setAttribute(
                  OBDal.getInstance().get(Attribute.class, objectsYear[1].toString()));
              objInstance.setAttributeValue(
                  OBDal.getInstance().get(AttributeValue.class, objectsYear[2].toString()));
              objInstance.setAttributeSetValue(objAttributeSet);
              OBDal.getInstance().save(objInstance);
            }
            // attribute Chassis Number
            String selectAttributeText = " select attrset.m_attributeset_id,attr.m_attribute_id "
                + " from m_attributeset  attrset  "
                + "     join m_attributeuse uattr on uattr.m_attributeset_id=attrset.m_attributeset_id "
                + "    join m_attribute attr on attr.m_attribute_id= uattr.m_attribute_id "
                + "    where lower(attr.name)=?        and attrset.m_attributeset_id=? limit 1 ";
            SQLQuery attributeChassisQuery = OBDal.getInstance().getSession()
                .createSQLQuery(selectAttributeText);

            attributeChassisQuery.setParameter(0,
                jsonHeaderresult.get("chasis").toString().toLowerCase());
            attributeChassisQuery.setParameter(1, objProduct.getEscmCusattribute().getId());
            List<Object[]> objectsChasislist = (ArrayList<Object[]>) attributeChassisQuery.list();
            if ((objectsChasislist.size() > 0)) {
              Object[] objectsYear = objectsChasislist.get(0);
              AttributeInstance objInstance = OBProvider.getInstance().get(AttributeInstance.class);
              objInstance.setAttribute(
                  OBDal.getInstance().get(Attribute.class, objectsYear[1].toString()));
              objInstance.setAttributeSetValue(objAttributeSet);
              objInstance.setSearchKey(objects[10].toString());
              OBDal.getInstance().save(objInstance);
            }
            // plate number
            SQLQuery attributePlateQuery = OBDal.getInstance().getSession()
                .createSQLQuery(selectAttributeText);

            attributePlateQuery.setParameter(0,
                jsonHeaderresult.get("plate").toString().toLowerCase());
            attributePlateQuery.setParameter(1, objProduct.getEscmCusattribute().getId());
            List<Object[]> objectsPlatelist = (ArrayList<Object[]>) attributePlateQuery.list();
            if ((objectsPlatelist.size() > 0)) {
              Object[] objectsYear = objectsPlatelist.get(0);
              AttributeInstance objInstance = OBProvider.getInstance().get(AttributeInstance.class);
              objInstance.setAttribute(
                  OBDal.getInstance().get(Attribute.class, objectsYear[1].toString()));
              objInstance.setAttributeSetValue(objAttributeSet);
              objInstance.setSearchKey(objects[9].toString());
              OBDal.getInstance().save(objInstance);
            }
            // model year
            SQLQuery attributeYearQuery = OBDal.getInstance().getSession()
                .createSQLQuery(selectAttributeText);

            attributeYearQuery.setParameter(0,
                jsonHeaderresult.get("year").toString().toLowerCase());
            attributeYearQuery.setParameter(1, objProduct.getEscmCusattribute().getId());
            List<Object[]> objectsAttrYearlist = (ArrayList<Object[]>) attributeYearQuery.list();
            if ((objectsAttrYearlist.size() > 0)) {
              Object[] objectsAttributeYear = objectsAttrYearlist.get(0);
              AttributeInstance objInstance = OBProvider.getInstance().get(AttributeInstance.class);
              objInstance.setAttribute(
                  OBDal.getInstance().get(Attribute.class, objectsAttributeYear[1].toString()));
              objInstance.setAttributeSetValue(objAttributeSet);
              objInstance.setSearchKey(objects[4].toString());
              OBDal.getInstance().save(objInstance);
            }
            // Custody Desc
            SQLQuery attributeDescQuery = OBDal.getInstance().getSession()
                .createSQLQuery(selectAttributeText);

            attributeDescQuery.setParameter(0,
                jsonHeaderresult.get("cusDesc").toString().toLowerCase());
            attributeDescQuery.setParameter(1, objProduct.getEscmCusattribute().getId());
            List<Object[]> objectsAttrDesclist = (ArrayList<Object[]>) attributeDescQuery.list();
            if ((objectsAttrDesclist.size() > 0)) {
              Object[] objectsAttributeDesc = objectsAttrDesclist.get(0);
              AttributeInstance objInstance = OBProvider.getInstance().get(AttributeInstance.class);
              objInstance.setAttribute(
                  OBDal.getInstance().get(Attribute.class, objectsAttributeDesc[1].toString()));
              objInstance.setAttributeSetValue(objAttributeSet);
              objInstance.setSearchKey(objects[2].toString());
              OBDal.getInstance().save(objInstance);
            }
          }
          MaterialIssueRequestCustody objCustody = OBProvider.getInstance()
              .get(MaterialIssueRequestCustody.class);
          objCustody.setBeneficiaryType("E");
          objCustody.setBeneficiaryIDName(
              OBDal.getInstance().get(BeneficiaryView.class, objects[1].toString()));
          objCustody.setProduct(objProduct);
          objCustody.setDescription(objProduct.getName());
          objCustody.setQuantity(BigDecimal.ONE);
          ESCMProductCategoryV prdcat = OBDal.getInstance().get(ESCMProductCategoryV.class,
              objProduct.getProductCategory().getId());
          objCustody.setProductCategory(prdcat);
          objCustody.setAttributeSet(objProduct.getEscmCusattribute());
          objCustody.setAttributeSetValue(objAttributeSet);
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
              .setBname(OBDal.getInstance().get(BeneficiaryView.class, objects[1].toString()));
          objTransaction.setLine2(Long.valueOf(10));
          objTransaction.setLineNo(Long.valueOf(10));
          objTransaction.setTransactiontype("IE");
          objTransaction.setTransactionreason("Initial Data Loaded");
          objTransaction.setTransactionDate(new Date());
          objTransaction.setProcessed(true);
          OBDal.getInstance().save(objTransaction);
          OBDal.getInstance().flush();
          existingDocNo = String.valueOf(Integer.parseInt(existingDocNo) + 1);
          // End of Custody
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in insertIntoProductDetail()", e);
      status = 0;
      // TODO: handle exception
    }
    return status;

  }

  private int importDataInDatabase(String[] fields, String documentNo) {
    int isSuccess = 1;

    try {

      CsvCustodyImport csvImport = null;

      csvImport = OBProvider.getInstance().get(CsvCustodyImport.class);
      csvImport.setDocumentNo(documentNo);
      csvImport.setEmployeeno(fields[0]);
      csvImport.setEmployeename(fields[1]);
      csvImport.setPlatenumber(fields[2]);
      csvImport.setChasisnumber(fields[3]);
      csvImport.setItemdesc(fields[4]);
      csvImport.setModelyear(fields[5]);
      csvImport.setBrand(fields[6]);
      csvImport.setCusdesc(fields[7]);
      csvImport.setCylinder(fields[8]);
      csvImport.setOiltype(fields[9]);
      csvImport.setColor(fields[10]);
      // csvImport.setTagno(fields[11]);
      OBDal.getInstance().save(csvImport);
      // Employee Number //employee name //Plate Number
      // Chassis Number
      // Item description //Model Year//Brand //Custody description//Cylinder
      // Oil type//Color
    } catch (Exception e) {
      isSuccess = 0;
      log4j.error("Exception in importDataInDatabase()", e);
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

    String unquoted = "(:?[^\"" + separator + "]|\"\")*";
    String ending = "(:?" + separator + "|$)";
    return Pattern.compile('(' + unquoted + ')' + ending);
  }

  @SuppressWarnings({ "unchecked", "unused" })
  public JSONObject processValidateCsvFile(String filePath) {
    JSONObject jsonresult = new JSONObject();
    try {
      OBContext.setAdminMode(true);
      int lineNo = 1;
      int failed = 0;
      String strModelYear = "", strBrand = "", strCylinder = "", strOilType = "", strColor = "";

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
        List<String> resultHead = parseCSV(inpLine, inpDelimiter);
        String fieldsHead[] = (String[]) resultHead.toArray(new String[0]);
        for (int i = 0; i < fieldsHead.length; i++) {
          fieldsHead[i] = fieldsHead[i].replace("\"", "");
        }
        strModelYear = fieldsHead[5];
        strBrand = fieldsHead[6];
        strCylinder = fieldsHead[8];
        strOilType = fieldsHead[9];
        strColor = fieldsHead[10];
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
        log4j.debug("fields.length:" + fields.length);
        description.append(br);
        if (fields.length < 11) {
          failed += 1;
          description
              .append("Partial record at line No.<b>" + lineNo + "</b> in the data file.<br>");
          br = "<br>";
        } else if (fields.length > 11) {
          failed += 1;
          description.append("Line No.<b>" + lineNo
              + "</b> in the data file has more fields than expected(11).<br>");
          br = "<br>";
        } else {
          br = "";
          if (StringUtils.isEmpty(fields[0])) {
            failed += 1;
            description.append(
                "Employee Number is empty at line No.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[1])) {
            failed += 1;
            description.append(
                "Employee Name is empty at line no.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String employee = fields[0];
            OBCriteria<BusinessPartner> empListCriteria = OBDal.getInstance()
                .createCriteria(BusinessPartner.class);
            empListCriteria.add(Restrictions.ilike(BusinessPartner.PROPERTY_SEARCHKEY, employee));
            empListCriteria.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
            empListCriteria.setMaxResults(1);
            List<BusinessPartner> empList = empListCriteria.list();
            if (!(empList.size() > 0)) {// EmpList not found
              failed += 1;
              description.append("Employee <b>" + fields[1] + "</b> at line No.<b>" + lineNo
                  + "</b> not found in Master Data.<br>");
              br = "<br>";
            }
          }
          /*
           * if (StringUtils.isEmpty(fields[2])) { failed += 1;
           * description.append("Plate Number is empty at line No.<b>" + lineNo +
           * "</b> in the data file.<br>"); br = "<br>"; } if (StringUtils.isEmpty(fields[3])) {//
           * Recipient failed += 1; description.append("Chassis Number is empty at line No.<b>" +
           * lineNo + "</b> in the data file.<br>"); br = "<br>"; }
           */
          if (StringUtils.isEmpty(fields[4])) {
            failed += 1;
            description.append(
                "Item description is empty at line No.<b>" + lineNo + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String productName = fields[4];
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
            } else {
              Product objProduct = productlist.get(0); // product has attribute set then validate
                                                       // attribute columns
              if (objProduct.getEscmCusattribute() != null) {
                String selectAttribute = " select attrset.m_attributeset_id from m_attributeset  attrset "
                    + " join m_attributeuse uattr on uattr.m_attributeset_id=attrset.m_attributeset_id "
                    + " join m_attribute attr on attr.m_attribute_id= uattr.m_attribute_id "
                    + "   join m_attributevalue attrvalue on attrvalue.m_attribute_id=attr.m_attribute_id "
                    + "   where attrvalue.name= ? and lower(attr.name)=?"
                    + "   and attrset.m_attributeset_id=? ";
                // Model year

                /*
                 * if (StringUtils.isNotEmpty(fields[5])) { SQLQuery attributeQuery =
                 * OBDal.getInstance().getSession() .createSQLQuery(selectAttribute);
                 * attributeQuery.setParameter(0, fields[5]); attributeQuery.setParameter(1,
                 * strModelYear.toLowerCase()); attributeQuery.setParameter(2,
                 * objProduct.getEscmCusattribute().getId());
                 * 
                 * List<Object[]> objectslist = (ArrayList<Object[]>) attributeQuery.list(); if
                 * (!(objectslist.size() > 0)) { failed += 1;
                 * description.append("Attibute Value <b>" + fields[5] + "</b> at line No.<b>" +
                 * lineNo + "</b> does not exits in Master Data for Attribute <b>" + strModelYear +
                 * ".<br>"); br = "<br>"; } }
                 */

                if (StringUtils.isNotEmpty(fields[6])) {
                  SQLQuery attributeQuery = OBDal.getInstance().getSession()
                      .createSQLQuery(selectAttribute);
                  attributeQuery.setParameter(0, fields[6]);
                  attributeQuery.setParameter(1, strBrand.toLowerCase());
                  attributeQuery.setParameter(2, objProduct.getEscmCusattribute().getId());

                  List<Object[]> objectslist = (ArrayList<Object[]>) attributeQuery.list();
                  if (!(objectslist.size() > 0)) {
                    failed += 1;
                    description.append("Attibute Value <b>" + fields[6] + "</b> at line No.<b>"
                        + lineNo + "</b> does not exits in Master Data for Attribute <b>" + strBrand
                        + ".<br>");
                    br = "<br>";
                  }

                }
                if (StringUtils.isNotEmpty(fields[8])) {
                  SQLQuery attributeQuery = OBDal.getInstance().getSession()
                      .createSQLQuery(selectAttribute);
                  attributeQuery.setParameter(0, fields[8]);
                  attributeQuery.setParameter(1, strCylinder.toLowerCase());
                  attributeQuery.setParameter(2, objProduct.getEscmCusattribute().getId());

                  List<Object[]> objectslist = (ArrayList<Object[]>) attributeQuery.list();
                  if (!(objectslist.size() > 0)) {
                    failed += 1;
                    description.append("Attibute Value <b>" + fields[8] + "</b> at line No.<b>"
                        + lineNo + "</b> does not exits in Master Data for Attribute <b>"
                        + strCylinder + ".<br>");
                    br = "<br>";
                  }

                }
                if (StringUtils.isNotEmpty(fields[9])) {
                  SQLQuery attributeQuery = OBDal.getInstance().getSession()
                      .createSQLQuery(selectAttribute);
                  attributeQuery.setParameter(0, fields[9]);
                  attributeQuery.setParameter(1, strOilType.toLowerCase());
                  attributeQuery.setParameter(2, objProduct.getEscmCusattribute().getId());

                  List<Object[]> objectslist = (ArrayList<Object[]>) attributeQuery.list();
                  if (!(objectslist.size() > 0)) {
                    failed += 1;
                    description.append("Attibute Value <b>" + fields[9] + "</b> at line No.<b>"
                        + lineNo + "</b> does not exits in Master Data for Attribute <b>"
                        + strOilType + ".<br>");
                    br = "<br>";
                  }

                }
                // Color
                if (StringUtils.isNotEmpty(fields[10])) {
                  SQLQuery attributeQuery = OBDal.getInstance().getSession()
                      .createSQLQuery(selectAttribute);
                  attributeQuery.setParameter(0, fields[10]);
                  attributeQuery.setParameter(1, strColor.toLowerCase());
                  attributeQuery.setParameter(2, objProduct.getEscmCusattribute().getId());

                  List<Object[]> objectslist = (ArrayList<Object[]>) attributeQuery.list();
                  if (!(objectslist.size() > 0)) {
                    failed += 1;
                    description.append("Attibute Value <b>" + fields[10] + "</b> at line No.<b>"
                        + lineNo + "</b> does not exits in Master Data for Attribute <b>" + strColor
                        + ".<br>");
                    br = "<br>";
                  }
                }
              } else {
                failed += 1;
                description.append("Product <b>" + productName + "</b> at line No.<b>" + lineNo
                    + "</b> does not have attirbute set instance in Master Data.<br>");
                br = "<br>";
              }
            }
          }

          if (StringUtils.isEmpty(fields[7])) {
            failed += 1;
            description.append("Custody description is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          }
          /*
           * if (StringUtils.isEmpty(fields[11])) { failed += 1;
           * description.append("Product Tag is empty at line No.<b>" + lineNo +
           * "</b> in the data file.<br>"); br = "<br>"; }
           */
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
      log4j.error("Exception in Product Upload CSV Validate", e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonresult;

  }

}
