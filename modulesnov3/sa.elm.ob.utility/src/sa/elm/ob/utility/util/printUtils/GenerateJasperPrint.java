package sa.elm.ob.utility.util.printUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

public abstract class GenerateJasperPrint {

  protected String strReportName;
  public HashMap<String, Object> designParameters = new HashMap<String, Object>();
  protected Connection connection;
  protected ByteArrayOutputStream baos = new ByteArrayOutputStream();
  protected ZipOutputStream zos = new ZipOutputStream(baos);
  Logger log = Logger.getLogger(GenerateJasperPrint.class);
  protected String reportGeneratedDate = new SimpleDateFormat("yyyy-MM-dd")
      .format(new java.util.Date());
  protected String hijriDate = sa.elm.ob.utility.util.Utility
      .convertTohijriDate(reportGeneratedDate);
  protected String strFileName;
  protected Boolean isJasper = Boolean.FALSE, isZip = Boolean.FALSE;
  protected String strJspPage = "";

  public abstract void getReportVariables(HttpServletRequest request, JSONObject paramObject);

  public String getFileName() {
    return strFileName;
  }

  public Boolean isZip() {
    return isZip;
  }

  public Boolean isJasper() {
    return isJasper;
  }

  public ByteArrayOutputStream getBaos() {
    return baos;
  }

  public String getJspPage() {
    return strJspPage;
  }

  public HashMap<String, Object> getDesignParameters() {
    return designParameters;
  }

  public JasperPrint getJasperPrint() {
    InputStream fin;
    JasperPrint jasperPrint = null;
    try {
      fin = new FileInputStream(strReportName);
      JasperDesign jasperDesign = JRXmlLoader.load(fin);
      JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
      jasperPrint = JasperFillManager.fillReport(jasperReport, designParameters, connection);
    } catch (FileNotFoundException e) {
      log.error("FileNotFoundException while generating Print :", e);
    } catch (JRException e) {
      log.error("JRException while generating Print :", e);
    }
    return jasperPrint;
  }

}
