package sa.elm.ob.scm.ad_process.ProductCategoryImport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.ProductCategory;

import sa.elm.ob.scm.ESCMPrdCatUpload;
import sa.elm.ob.utility.util.Utility;

public class ProductCategoryImportDAO {
  Connection con = null;
  private static Logger log4j = Logger.getLogger(ProductCategoryImportDAO.class);
  private ProductCategoryImportVO VO = null;
  private ProductCategoryImportVO temVO = null;
  private final static String quoted = "\"(:?[^\"]|\"\")+\"";

  public ProductCategoryImportDAO(Connection con) {
    this.con = con;
  }

  @SuppressWarnings({ "rawtypes", "unused" })
  public ArrayList validateCsvFile(File csvFile, String orgId, String userId, String clientId,
      String action) {
    int total = 0, errorFlag = 0;
    BufferedReader inpReader = null;
    FileReader inpFile = null;
    ArrayList<ProductCategoryImportVO> rsLs = new ArrayList<ProductCategoryImportVO>();
    ArrayList<ProductCategoryImportVO> rsLs1 = new ArrayList<ProductCategoryImportVO>();
    Boolean haveInsertPST1 = false;

    try {
      inpFile = new FileReader(csvFile);
      inpReader = new BufferedReader(inpFile);
      String inpLine = "", documentno = "", inpDelimiter = "";
      documentno = "12345";
      StringBuffer description = new StringBuffer();
      String strbreak = "";
      final String firstLineHeader = "Y";

      int lineNo = 0;
      if (StringUtils.equals(firstLineHeader, "Y")) {
        inpLine = inpReader.readLine();
        if (inpLine.contains(","))
          inpDelimiter = ",";
        else if (inpLine.contains(";"))
          inpDelimiter = ";";
      }
      // }
      // int iteration = 0;
      while ((inpLine = inpReader.readLine()) != null) {
        /*
         * if (iteration == 0) { iteration++; continue; }
         */
        lineNo += 1;
        List<String> records = parseCSV(inpLine, inpDelimiter);
        total = records.size();
        String fields[] = (String[]) records.toArray(new String[0]);
        description.append(strbreak);
        for (int i = 0; i < 1; i++) {
          int innerFlag = 0;
          String docNo = documentno, maincatcode = "", maincatname = "", subcatcode = "",
              subcatname = "";
          fields[i] = fields[i].replace("\"", "");
          if (StringUtils.isEmpty(fields[0])) {
            maincatcode = null;
            innerFlag = 1;
          } else
            maincatcode = fields[0];
          if (StringUtils.isEmpty(fields[1])) {
            maincatname = null;
            innerFlag = 1;
          } else
            maincatname = fields[1];
          if (StringUtils.isEmpty(fields[2])) {
            subcatcode = null;
            innerFlag = 1;
          } else
            subcatcode = fields[2];
          if (StringUtils.isEmpty(fields[3])) {
            subcatname = null;
            innerFlag = 1;
          } else
            subcatname = fields[3];
          if (StringUtils.isNotEmpty(fields[1])) {
            OBQuery<ProductCategory> cat = OBDal.getInstance().createQuery(ProductCategory.class,
                " as e where e.searchKey='" + fields[1] + "'   and summaryLevel='Y' and client.id='"
                    + clientId + "'");
            log4j.debug("cat.list().size():" + cat.list().size());
            if (cat.list().size() > 0) {
              innerFlag = 1;
              VO = new ProductCategoryImportVO();
              VO.setMaincatcode(maincatcode);
              VO.setMaincatname(maincatname);
              VO.setSubcatcode(subcatcode);
              VO.setSubcatname(subcatname);
              VO.setDocuemntno(documentno);
              VO.setMessage("Already Product Category Exists at line No");
              VO.setLineno(lineNo);
              rsLs.add(VO);
            }
          }
          OBQuery<ProductCategory> cat = OBDal.getInstance().createQuery(ProductCategory.class,
              " as e where e.searchKey='" + fields[2]
                  + "' and escmProductCategory.id in ( select id from ProductCategory where searchKey='"
                  + fields[1] + "' and name='" + fields[0]
                  + "' and  summaryLevel='Y' and client.id='" + clientId
                  + "' )and summaryLevel='N' and client.id='" + clientId + "'");
          log4j.debug("cat.list().size():" + cat.list().size());
          if (cat.list().size() > 0) {
            VO = new ProductCategoryImportVO();
            VO.setMaincatcode(maincatcode);
            VO.setMaincatname(maincatname);
            VO.setSubcatcode(subcatcode);
            VO.setSubcatname(subcatname);
            VO.setDocuemntno(documentno);
            VO.setMessage("Already Product Category Exists at line No.");
            VO.setLineno(lineNo);
            rsLs.add(VO);
          }
          /*
           * if (StringUtils.isNotEmpty(fields[0])) { log4j.debug("fields[0]:" +
           * fields[0].length()); for (int j = 0, len = fields[0].length(); j < len; i++) { if
           * (Character.isDigit(fields[0].charAt(j))) { charcount++; } } + isallow =
           * fields[0].matches("-?\\d+(\\.\\d+)?"); // it will check only numeric values
           * log4j.debug("isallow:" + isallow); if (charcount > 2 && fields[0].length() > 2) {
           * isallow = false; } log4j.debug("fields[0]).compareTo(BigDecimal.ZERO):" + new
           * BigDecimal(fields[0]).compareTo(BigDecimal.ZERO)); if (new
           * BigDecimal(fields[0]).compareTo(BigDecimal.ZERO) < 0) { isallow = false; }
           * log4j.debug("isallow:" + isallow); if (!isallow) { VO = new ProductCategoryImportVO();
           * VO.setMaincatcode(maincatcode); VO.setMaincatname(maincatname);
           * VO.setSubcatcode(subcatcode); VO.setSubcatname(subcatname);
           * VO.setDocuemntno(documentno);
           * VO.setMessage("Allowed values for code is from 00 to 99."); VO.setLineno(lineNo);
           * rsLs.add(VO); } } if (StringUtils.isNotEmpty(fields[2])) { log4j.debug("fields[2]:" +
           * fields[2]); for (int j = 0, len = fields[2].length(); j < len; i++) { if
           * (Character.isDigit(fields[2].charAt(i))) { charcount++; } } isallow =
           * fields[2].matches("-?\\d+(\\.\\d+)?"); // it will check only numeric values
           * 
           * if (charcount > 2 && fields[2].length() > 2) { isallow = false; } if (new
           * BigDecimal(fields[2]).compareTo(BigDecimal.ZERO) < 0) { isallow = false; } if
           * (!isallow) { VO = new ProductCategoryImportVO(); VO.setMaincatcode(maincatcode);
           * VO.setMaincatname(maincatname); VO.setSubcatcode(subcatcode);
           * VO.setSubcatname(subcatname); VO.setDocuemntno(documentno);
           * VO.setMessage("Allowed values for code is from 00 to 99."); VO.setLineno(lineNo);
           * rsLs.add(VO); } }
           */
          if (maincatcode != null && maincatname != null && subcatname != null
              && subcatcode != null) {
            temVO = new ProductCategoryImportVO();
            temVO.setMaincatcode(maincatcode);
            temVO.setMaincatname(maincatname);
            temVO.setSubcatcode(subcatcode);
            temVO.setSubcatname(subcatname);
            temVO.setDocuemntno(docNo);
            rsLs1.add(temVO);
          }
          if (innerFlag == 1) {
            errorFlag = 1;
            VO = new ProductCategoryImportVO();
            VO.setMaincatcode(maincatcode);
            VO.setMaincatname(maincatname);
            VO.setSubcatcode(subcatcode);
            VO.setSubcatname(subcatname);
            VO.setDocuemntno(documentno);
            VO.setLineno(lineNo);
            rsLs.add(VO);

          }

        }
      }
      if (rsLs1.size() > 0) {
        for (ProductCategoryImportVO vo : rsLs1) {
          ESCMPrdCatUpload prdcatupload = OBProvider.getInstance().get(ESCMPrdCatUpload.class);
          prdcatupload.setClient(OBDal.getInstance().get(Client.class, clientId));
          prdcatupload.setOrganization(OBDal.getInstance().get(Organization.class, orgId));
          prdcatupload.setCreatedBy(OBDal.getInstance().get(User.class, userId));
          prdcatupload.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          prdcatupload.setMaincatcode(vo.getMaincatcode());
          prdcatupload.setMaincatname(vo.getMaincatname());
          prdcatupload.setSubcatcode(vo.getSubcatcode());
          prdcatupload.setSubcatname(vo.getSubcatname());
          prdcatupload.setDocumentNo(documentno);
          OBDal.getInstance().save(prdcatupload);
          haveInsertPST1 = true;
          OBDal.getInstance().flush();
        }
      }
      if (action.equals("Validate") || errorFlag == 1) {
        OBQuery<ESCMPrdCatUpload> prodcatupldel = OBDal.getInstance()
            .createQuery(ESCMPrdCatUpload.class, " documentNo='" + documentno + "'");
        if (prodcatupldel.list().size() > 0) {
          for (ESCMPrdCatUpload up : prodcatupldel.list()) {
            OBDal.getInstance().remove(up);
          }
          OBDal.getInstance().flush();
        }

      }
    } catch (final Exception ee) {
      log4j.error("Validate Csv File", ee);
      csvFile.delete();
    } finally {
      if (inpReader != null) {
        try {
          inpReader.close();
          inpFile.close();
        } catch (final Exception e) {
          log4j.error("Validate Csv File", e);
        }
      }
    }
    return rsLs;
  }

  @SuppressWarnings("rawtypes")
  public int uploadCSVFile(File csvFile, String orgId, String userId, String clientId) {
    int count = 0;
    BufferedReader inpReader = null;
    FileReader inpFile = null;
    Boolean haveInsertPST1 = false;
    try {
      inpFile = new FileReader(csvFile);
      inpReader = new BufferedReader(inpFile);
      String documentno = "";
      documentno = "12345";
      // main category
      SQLQuery qry = OBDal.getInstance().getSession().createSQLQuery(
          "select  distinct maincatcode,maincatname  from escm_prdcat_upload where documentno='12345'");
      if (qry != null && qry.list().size() > 0) {
        for (Iterator iterator = qry.list().iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          ProductCategory category = OBProvider.getInstance().get(ProductCategory.class);
          category.setClient(OBDal.getInstance().get(Client.class, clientId));
          category.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
          category.setCreatedBy(OBDal.getInstance().get(User.class, userId));
          category.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          category.setName(Utility.nullToZero(row[0]).toString());
          category.setSearchKey(Utility.nullToZero(row[1]).toString());
          category.setSummaryLevel(true);
          category.setPlannedMargin(new BigDecimal(0));
          OBDal.getInstance().save(category);
          OBDal.getInstance().flush();
          haveInsertPST1 = true;
        }
      }
      // sub category
      SQLQuery qry1 = OBDal.getInstance().getSession().createSQLQuery(
          "select  maincatcode, maincatname,subcatcode,subcatname from escm_prdcat_upload where documentno='12345'");
      if (qry1 != null && qry1.list().size() > 0) {
        for (Iterator iterator = qry1.list().iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          OBQuery<ProductCategory> cat = OBDal.getInstance().createQuery(ProductCategory.class,
              " as e where e.searchKey='" + Utility.nullToZero(row[1]).toString()
                  + "'  and  e.name='" + Utility.nullToZero(row[0]).toString()
                  + "'  and e.summaryLevel='Y'");
          ProductCategory category = OBProvider.getInstance().get(ProductCategory.class);
          category.setClient(OBDal.getInstance().get(Client.class, clientId));
          category.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
          category.setCreatedBy(OBDal.getInstance().get(User.class, userId));
          category.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          category.setName(Utility.nullToZero(row[2]).toString());
          category.setSearchKey(Utility.nullToZero(row[3]).toString());
          category.setSummaryLevel(false);
          if (cat.list().size() > 0) {
            ProductCategory mastercat = cat.list().get(0);
            category.setEscmProductCategory(mastercat);
          }
          category.setPlannedMargin(new BigDecimal(0));
          OBDal.getInstance().save(category);
          haveInsertPST1 = true;
        }
      }

      if (haveInsertPST1) {
        OBQuery<ESCMPrdCatUpload> prodcatupldel = OBDal.getInstance()
            .createQuery(ESCMPrdCatUpload.class, " documentNo='" + documentno + "'");
        if (prodcatupldel.list().size() > 0) {
          for (ESCMPrdCatUpload up : prodcatupldel.list()) {
            OBDal.getInstance().remove(up);
          }
          OBDal.getInstance().flush();
        }
        count = 1;
      }

    } catch (final Exception ee) {
      log4j.error("Upload Csv File", ee);
      csvFile.delete();
    } finally {
      if (inpReader != null) {
        try {
          inpReader.close();
          inpFile.close();
        } catch (final Exception e) {
          log4j.error("Upload Csv File", e);
        }
      }
    }
    return count;
  }

  private List<String> parseCSV(String csv, String delim) {
    final Pattern NEXT_COLUMN = nextColumnRegex(delim);
    final List<String> strings = new ArrayList<String>();
    final Matcher matcher = NEXT_COLUMN.matcher(csv);
    while (!matcher.hitEnd() && matcher.find()) {
      String match = matcher.group(1);
      if (match.matches(quoted))
        match = match.substring(1, match.length() - 1);
      match = match.replaceAll("\"\"", "\"");
      strings.add(match);
    }
    return strings;
  }

  private Pattern nextColumnRegex(String comma) {
    String unquoted = "(:?[^\"" + comma + "]|\"\")*";
    String ending = "(:?" + comma + "|$)";
    return Pattern.compile('(' + quoted + '|' + unquoted + ')' + ending);
  }
}