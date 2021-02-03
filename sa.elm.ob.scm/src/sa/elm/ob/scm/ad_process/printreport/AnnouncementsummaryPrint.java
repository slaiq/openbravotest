package sa.elm.ob.scm.ad_process.printreport;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import sa.elm.ob.scm.ESCMAnnouSummaryMedia;
import sa.elm.ob.scm.IdentifySupplier;
import sa.elm.ob.utility.util.printUtils.GenerateJasperPrint;

public class AnnouncementsummaryPrint extends GenerateJasperPrint {
  Logger log = Logger.getLogger(AnnouncementsummaryPrint.class);

  private static AnnouncementsummaryPrint announcementSummaryPrint;

  public static AnnouncementsummaryPrint getInstance() {
    if (announcementSummaryPrint == null) {
      announcementSummaryPrint = new AnnouncementsummaryPrint();
    }
    return announcementSummaryPrint;
  }

  @Override
  public void getReportVariables(HttpServletRequest request, JSONObject paramObject) {

    try {
      OBContext.setAdminMode();
      baos = new ByteArrayOutputStream();
      zos = new ZipOutputStream(baos);
      String strLocalReportName;
      HashMap<String, Object> localDesignParameters = new HashMap<String, Object>();
      String reportDir = "";
      reportDir = paramObject.getString("reportDir");
      isZip = Boolean.TRUE;
      connection = (Connection) paramObject.get("connection");
      InputStream fin = null;
      String medianame = "";
      OBQuery<ESCMAnnouSummaryMedia> announmedia = OBDal.getInstance().createQuery(
          ESCMAnnouSummaryMedia.class,
          "as e where escmAnnoucements.id='" + request.getParameter("inpRecordId") + "' ");

      if (announmedia != null && announmedia.list().size() > 0) {
        int count = 0;
        for (@SuppressWarnings("rawtypes")
        Iterator iterator = announmedia.list().listIterator(); iterator.hasNext();) {
          ESCMAnnouSummaryMedia media = (ESCMAnnouSummaryMedia) iterator.next();
          if (media.getMediaName() != null) {
            count = count + 1;
            medianame = media.getMediaName().getId();
            // log4j.info("medianame>" + medianame);
            localDesignParameters.put("BASE_DESIGN", paramObject.getString("basedesign"));
            localDesignParameters.put("MediaName", medianame);
            localDesignParameters.put("AnnouncementID", media.getEscmAnnoucements().getId());
            Boolean isummalalqura = IdentifySupplier.identifyBPartner(media.getClient().getId(),
                media.getMediaName().getId());
            if (isummalalqura) {
              strLocalReportName = reportDir
                  + "BidNewspaperAnnouncementReportUmmAlQura/BidNewspaperAnnouncementReportUmmAlQura.jrxml";
              /*
               * strFileName = "BidNewspaperAnnouncementReportUmmAlQura" + " " + " " + hijriDate;
               */
            } else {
              strLocalReportName = reportDir
                  + "BidNewspaperAnnouncementReportForOthers/BidNewspaperAnnouncementReportOthers.jrxml";
              /*
               * strFileName = "BidNewspaperAnnouncementReportForOthers" + " " + " " + hijriDate;
               */
            }

            fin = new FileInputStream(strLocalReportName);
            JasperDesign jasperDesign = JRXmlLoader.load(fin);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
                localDesignParameters, connection);

            final byte[] bytes = JasperExportManager.exportReportToPdf(jasperPrint);
            final ZipEntry ze = new ZipEntry("BidNewspaperAnnouncementReport" + count + ".pdf"); // periodicScale13032015.pdf
            ze.setSize(bytes.length);
            ze.setTime(System.currentTimeMillis());
            zos.putNextEntry(ze);
            zos.write(bytes);
            zos.closeEntry();
            media.setPrintedon(new java.util.Date());
          }
        }

      }
      zos.flush();
      baos.flush();
      zos.close();
      baos.close();

    } catch (Exception e) {
      log.error("Exception in getReportVariables(): ", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /*
   * private String getBaseDesignPath(String language) { // TODO Auto-generated method stub return
   * null; }
   */
}