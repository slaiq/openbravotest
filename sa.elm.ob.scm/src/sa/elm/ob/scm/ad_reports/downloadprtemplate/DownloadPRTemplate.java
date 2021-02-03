package sa.elm.ob.scm.ad_reports.downloadprtemplate;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.session.OBPropertiesProvider;

public class DownloadPRTemplate extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String action = request.getParameter("act") == null ? "" : request.getParameter("act");
    try {
      if ("DownloadTemplate".equals(action)) {
        // String realPath = globalParameters.strFTPDirectory;
        // realPath = realPath.substring(0, realPath.lastIndexOf("/"));
        String realPath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
            .getProperty("source.path");
        realPath = realPath + "/modules/sa.elm.ob.scm/web/sa.elm.ob.scm/jsp/downloadprtemplate";
        File templateFile = new File(realPath, "PR_Template.xlsx");
        if (templateFile.exists()) {
          response.setHeader("Content-Type",
              getServletContext().getMimeType(templateFile.getName()));
          response.setHeader("Content-Disposition",
              "inline; filename=\"" + templateFile.getName() + "\"");
          BufferedInputStream input = null;
          BufferedOutputStream output = null;
          try {
            input = new BufferedInputStream(new FileInputStream(templateFile));
            output = new BufferedOutputStream(response.getOutputStream());

            byte[] buffer = new byte[response.getBufferSize()];
            for (int length = 0; (length = input.read(buffer)) > 0;) {
              output.write(buffer, 0, length);
            }
          } finally {
            if (output != null)
              try {
                output.flush();
                output.close();
              } catch (IOException ignore) {
              }
            if (input != null)
              try {
                input.close();
              } catch (IOException ignore) {
              }
          }
          return;
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in DownloadPRTemplate :", e);
    }
  }

}