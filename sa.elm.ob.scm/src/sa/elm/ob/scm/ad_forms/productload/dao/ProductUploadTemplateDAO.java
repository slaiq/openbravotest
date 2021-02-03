package sa.elm.ob.scm.ad_forms.productload.dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
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
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxCategory;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmCsvProductimport;

/**
 * @author Gopalakrishnan Servlet implementation class of Product Load
 */

public class ProductUploadTemplateDAO {

  private static final Logger log4j = Logger.getLogger(ProductUploadTemplateDAO.class);

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
      String documentNo = "987654"; // doc no to identify temp table

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

        OBCriteria<EscmCsvProductimport> csvTable = OBDal.getInstance().createCriteria(
            EscmCsvProductimport.class);
        csvTable.add(Restrictions.eq(EscmCsvProductimport.PROPERTY_DOCUMENTNO, documentNo));
        List<EscmCsvProductimport> csvTableList = csvTable.list();
        for (EscmCsvProductimport delcsvObj : csvTableList) {
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

  @SuppressWarnings("unchecked")
  private int insertIntoProductDetail(String documentNo) {
    int status = 1;
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
      log4j.debug("Inserting of Product Master" + documentNo);
      String selectProduct = " select csv.itemdesc,uom.c_uom_id,ps.escm_deflookups_typeln_id,list.value as ptype, "
          + " org.ad_org_id,pc.m_product_category_id as m_product_category_id, "
          + " csv.purchase,csv.inspection,csv.sale,csv.stock,csv.inspection,csv.escm_active "
          + " from escm_csv_product_import csv   "
          + " left join (select main.name as maincode,sub.name as subcode,sub.m_product_category_id,sub.ad_client_id  "
          + "    from m_product_category sub "
          + "     join m_product_category main on main.m_product_category_id=sub.em_escm_product_category) "
          + "     pc on to_number(pc.maincode)=to_number(csv.maincode) and to_number(pc.subcode)=to_number(csv.subcode) and  pc.ad_client_id=csv.ad_client_id  "
          + " left join ( select name,ltl.escm_deflookups_typeln_id,lt.ad_client_id from escm_deflookups_type lt "
          + " left join escm_deflookups_typeln ltl on ltl.escm_deflookups_type_id=lt.escm_deflookups_type_id "
          + " where lt.reference='PST' and (ltl.value='CUS' or ltl.value='CON')) as ps on ps.name ilike csv.stocktype and ps.ad_client_id=csv.ad_client_id"
          + " left join c_uom uom on uom.name ilike csv.uom "
          + " left join ad_org org on org.name ilike  csv.escm_organization "
          + " left join ad_ref_list list on ad_reference_id  ='270' and list.name ilike csv.producttype "
          + " where csv.documentno=? "
          + " group by escm_csv_product_import_id,uom.c_uom_id,ps.escm_deflookups_typeln_id,list.value,org.ad_org_id,pc.m_product_category_id  ";
      SQLQuery productQuery = OBDal.getInstance().getSession().createSQLQuery(selectProduct);
      productQuery.setParameter(0, documentNo);

      List<Object[]> objectslist = (ArrayList<Object[]>) productQuery.list();
      if (productQuery != null && objectslist.size() > 0) {
        for (int i = 0; i < objectslist.size(); i++) {
          Object[] objects = objectslist.get(i);
          Product productheadObj = null;

          productheadObj = OBProvider.getInstance().get(Product.class);
          productheadObj.setProductCategory(OBDal.getInstance().get(ProductCategory.class,
              objects[5].toString()));
          productheadObj.setTaxCategory(objTaxCategory);
          productheadObj.setName(objects[0].toString());
          productheadObj.setOrganization(OBDal.getInstance().get(Organization.class,
              objects[4].toString()));
          productheadObj.setUOM(OBDal.getInstance().get(UOM.class, objects[1].toString()));
          productheadObj.setProductType(objects[3].toString()); // Product Type
          productheadObj.setEscmStockType(OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
              objects[2].toString()));
          productheadObj.setPurchase(objects[6].toString().equals("Y") ? true : false);
          productheadObj.setEscmNoinspection(objects[7].toString().equals("Y") ? true : false);
          productheadObj.setSale(objects[8].toString().equals("Y") ? true : false);
          productheadObj.setStocked(objects[9].toString().equals("Y") ? true : false);
          productheadObj.setActive(objects[11].toString().equals("Y") ? true : false);
          OBDal.getInstance().save(productheadObj);
          OBDal.getInstance().flush();
          // End of Product
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

      EscmCsvProductimport csvImport = null;

      csvImport = OBProvider.getInstance().get(EscmCsvProductimport.class);
      csvImport.setDocumentNo(documentNo);
      csvImport.setEscmOrganization(fields[0]);
      csvImport.setMaincode(fields[1]);
      csvImport.setMaindesc(fields[2]);
      csvImport.setSubcode(fields[3]);
      csvImport.setSubdesc(fields[4]);
      csvImport.setItemdesc(fields[5]);
      csvImport.setUom(fields[6]);
      csvImport.setStockType(fields[7]);
      csvImport.setProductType(fields[8]);
      csvImport.setPurchase((fields[9]).toLowerCase().equals("yes") ? "Y" : "N");
      csvImport.setStock((fields[10]).toLowerCase().equals("yes") ? "Y" : "N");
      csvImport.setInspection((fields[11]).toLowerCase().equals("yes") ? "Y" : "N");
      csvImport.setEscmActive((fields[12]).toLowerCase().equals("yes") ? "Y" : "N");
      csvImport.setSale((fields[13]).toLowerCase().equals("yes") ? "Y" : "N");
      OBDal.getInstance().save(csvImport);
      // Organization //Main Code//Main desc //Sub Code//Sub Desc
      // Item Description//UOM //Stock Type
      // Stock Type //Purchase //Stock //Inspection //Active//Sale
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

    String unquoted = "(:?[^\"" + separator + "]|\"\")*";
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
        description.append(br);
        if (fields.length < 14) {
          failed += 1;
          description.append("Partial record at line No.<b>" + lineNo
              + "</b> in the data file.<br>");
          br = "<br>";
        } else if (fields.length > 14) {
          failed += 1;
          description.append("Line No.<b>" + lineNo
              + "</b> in the data file has more fields than expected(14).<br>");
          br = "<br>";
        } else {
          br = "";
          if (StringUtils.isEmpty(fields[0])) {
            failed += 1;
            description.append("Organization is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String orgName = fields[0];
            OBCriteria<Organization> orgListCriteria = OBDal.getInstance().createCriteria(
                Organization.class);
            orgListCriteria.add(Restrictions.ilike(Organization.PROPERTY_NAME, orgName));
            orgListCriteria.setMaxResults(1);
            List<Organization> orglist = orgListCriteria.list();
            if (!(orglist.size() > 0)) {// PriceList not found
              failed += 1;
              description.append("Organization <b>" + orgName + "</b> at line No.<b>" + lineNo
                  + "</b> not found in Master Data.<br>");
              br = "<br>";
            }
          }
          if (StringUtils.isEmpty(fields[1])) {
            failed += 1;
            description.append("Main Category Code is empty at line no.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[2])) {
            failed += 1;
            description.append("Main Category Desc is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[3])) {// Recipient
            failed += 1;
            description.append("Sub Category Code is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String productCatName = fields[3];
            OBQuery<ProductCategory> prdCatQry = OBDal.getInstance().createQuery(
                ProductCategory.class,
                "as e where e.escmProductCategory.name='" + fields[1] + "' and e.name='"
                    + productCatName + "'");
            List<ProductCategory> productCatlist = prdCatQry.list();
            if (!(productCatlist.size() > 0)) {// Product Category not found
              failed += 1;
              description.append("Product Sub Category <b>" + productCatName
                  + "</b> of Product Category <b>" + fields[1] + " at line No.<b>" + lineNo
                  + "</b> not  exists in Master Data.<br>");
              br = "<br>";
            }
          }
          if (StringUtils.isEmpty(fields[4])) {
            failed += 1;
            description.append("Sub Category description is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[5])) {
            failed += 1;
            description.append("Item Description is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String productName = fields[5];
            OBCriteria<Product> productListCriteria = OBDal.getInstance().createCriteria(
                Product.class);
            productListCriteria.add(Restrictions.ilike(Product.PROPERTY_NAME, productName));
            productListCriteria.setMaxResults(1);
            productListCriteria.setFilterOnActive(false);
            List<Product> productlist = productListCriteria.list();
            if ((productlist.size() > 0)) {// PriceList not found
              failed += 1;
              description.append("Product <b>" + productName + "</b> at line No.<b>" + lineNo
                  + "</b> already exists in Master Data.<br>");
              br = "<br>";
            }
          }
          if (StringUtils.isEmpty(fields[6])) {
            failed += 1;
            description.append("UOM is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String uomName = fields[6];
            OBCriteria<UOM> UOMListCriteria = OBDal.getInstance().createCriteria(UOM.class);
            UOMListCriteria.add(Restrictions.ilike(UOM.PROPERTY_NAME, uomName));
            UOMListCriteria.setMaxResults(1);
            List<UOM> uomlist = UOMListCriteria.list();
            if (!(uomlist.size() > 0)) {// PriceList not found
              failed += 1;
              description.append("UOM <b>" + uomName + "</b> at line No.<b>" + lineNo
                  + "</b> not found in Master Data.<br>");
              br = "<br>";
            }
          }
          if (StringUtils.isEmpty(fields[7])) {
            failed += 1;
            description.append("Stock Type is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          } else {
            String sType = fields[7];
            OBQuery<ESCMDefLookupsTypeLn> defineLookupList = OBDal.getInstance().createQuery(
                ESCMDefLookupsTypeLn.class,
                "as e where e.escmDeflookupsType.reference='PST' and e.commercialName='" + sType
                    + "'");
            if (!(defineLookupList.list().size() > 0)) {
              failed += 1;
              description.append("Stock Type <b>" + sType + "</b> at line No.<b>" + lineNo
                  + "</b> not found in Look Up reference.<br>");
              br = "<br>";
            }
          }
          if (StringUtils.isEmpty(fields[8])) {
            failed += 1;
            description.append("Product Type is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          } /*
             * else { String pType = fields[8]; OBCriteria<org.openbravo.model.ad.domain.List>
             * refListCri = OBDal.getInstance()
             * .createCriteria(org.openbravo.model.ad.domain.List.class);
             * refListCri.add(Restrictions.ilike(org.openbravo.model.ad.domain.List.PROPERTY_NAME,
             * pType)); refListCri.add(Restrictions.ilike(
             * org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,
             * OBDal.getInstance().get(Reference.class, pType))); refListCri.setMaxResults(1);
             * List<org.openbravo.model.ad.domain.List> reflist = refListCri.list(); if
             * (!(reflist.size() > 0)) {// PriceList not found failed += 1;
             * description.append("Product Type <b>" + pType + "</b> at line No.<b>" + lineNo +
             * "</b> not found in Master Data.<br>"); br = "<br>"; } }
             */
          if (StringUtils.isEmpty(fields[9])) {
            failed += 1;
            description.append("Purchase is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[10])) {
            failed += 1;
            description.append("Stock is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[11])) {
            failed += 1;
            description.append("Inspection is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[12])) {
            failed += 1;
            description.append("Activ is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
          }
          if (StringUtils.isEmpty(fields[13])) {
            failed += 1;
            description.append("Sale is empty at line No.<b>" + lineNo
                + "</b> in the data file.<br>");
            br = "<br>";
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
      log4j.error("Exception in Product Upload CSV Validate", e);
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }

    return jsonresult;

  }

}
