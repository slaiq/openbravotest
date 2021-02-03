/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.finance.ad_process.GLJournal;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.SQLQuery;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.xmlEngine.XmlDocument;

import sa.elm.ob.utility.util.UtilityDAO;

public class CopyFromGLJournal extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    log4j.info("Save: GLJournal:" + vars);
    if (vars.commandIn("DEFAULT")) {
      String strWindow = vars.getRequiredStringParameter("inpwindowId");
      String strTab = vars.getRequiredStringParameter("inpTabId");
      String strWindowId = vars.getStringParameter("inpwindowId");
      String strKeyColumnId = vars.getStringParameter("inpkeyColumnId");
      String strKey = vars.getGlobalVariable("inpglJournalbatchId",
          strWindowId + "|" + strKeyColumnId);
      String strDescription = vars.getStringParameter("inpDescription", "");
      String strDocumentNo = vars.getStringParameter("inpDocumentNo", "");
      printPage(response, vars, strDescription, strDocumentNo, strWindow, strTab, strKey);
    } else if (vars.commandIn("FIND")) {
      String strWindow = vars.getRequiredStringParameter("inpwindowId");
      String strTab = vars.getRequiredStringParameter("inpTabId");
      String strKey = vars.getRequiredStringParameter("inpglJournalbatchId");
      String strDescription = vars.getStringParameter("inpDescription");
      String strDocumentNo = vars.getStringParameter("inpDocumentNo");
      printPage(response, vars, strDescription, strDocumentNo, strWindow, strTab, strKey);
    } else if (vars.commandIn("SAVE")) {
      String strWindow = vars.getStringParameter("inpwindowId");
      String strTab = vars.getStringParameter("inpTabId");

      String strWindowPath = Utility.getTabURL(strTab, "R", true);
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;

      String strKey = vars.getRequiredStringParameter("inpglJournalbatchId");
      String strGLJournalBatch = vars.getStringParameter("inpClave");

      OBError myError = processButton(vars, strKey, strGLJournalBatch, strWindow);
      vars.setMessage(strTab, myError);

      printPageClosePopUp(response, vars, strWindowPath);
    } else
      pageErrorPopUp(response);
  }

  /**
   * This method is used to process button
   * 
   * @param vars
   * @param strKey
   * @param strGLJournalBatch
   * @param windowId
   * @return
   */
  private OBError processButton(VariablesSecureApp vars, String strKey, String strGLJournalBatch,
      String windowId) {

    if (log4j.isDebugEnabled())
      log4j.debug("Save: GLJournal");
    if (strGLJournalBatch.equals(""))
      return new OBError();
    ;
    Connection conn = null;

    OBError myError = null;
    try {
      conn = this.getTransactionConnection();
      String CalendarId = "";
      CopyFromGLJournalData[] data = CopyFromGLJournalData.select(this, strKey, strGLJournalBatch);
      for (int i = 0; data != null && i < data.length; i++) {
        String strSequence = SequenceIdData.getUUID();
        // String strDocumentNo = Utility.getDocumentNo(this, vars, windowId, "GL_Journal", "",
        // data[i].cDoctypeId, false, true);
        Organization org = null;
        org = OBDal.getInstance().get(Organization.class, data[i].adOrgId);

        if (org.getCalendar() != null) {
          CalendarId = org.getCalendar().getId();

        } else {

          // get parent organization list
          String[] orgIds = null;
          SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
              "select eut_parent_org ('" + data[i].adOrgId + "','" + vars.getClient() + "')");
          @SuppressWarnings("unchecked")
          List<String> list = query.list();

          orgIds = list.get(0).split(",");

          for (int k = 0; k < orgIds.length; k++) {
            org = OBDal.getInstance().get(Organization.class, orgIds[k].replace("'", ""));

            if (org.getCalendar() != null) {
              CalendarId = org.getCalendar().getId();

              break;
            }
          }

        }
        String strDocumentNo = UtilityDAO.getGeneralSequence(data[i].dateacct, "GS", CalendarId,
            data[i].adOrgId, true);
        try {
          if (CopyFromGLJournalData.insertGLJournal(conn, this, strSequence, vars.getClient(),
              data[i].adOrgId, vars.getUser(), data[i].cAcctschemaId, data[i].cDoctypeId, "DR",
              "CO", data[i].isapproved, data[i].isprinted, data[i].description, data[i].postingtype,
              data[i].glCategoryId, data[i].datedoc, data[i].dateacct, data[i].cPeriodId,
              data[i].cCurrencyId, data[i].currencyratetype, data[i].currencyrate, strKey,
              data[i].controlamt, strDocumentNo, "N", "N", "N", data[i].user1Id, data[i].user2Id,
              data[i].cCampaignId, data[i].cProjectId, data[i].aAssetId, data[i].cCostcenterId,
              data[i].cBpartnerId, data[i].mProductId) == 0)
            log4j.warn("Save: GLJournal record " + i + " not inserted. Sequence = " + strSequence);
        } catch (ServletException ex) {
          myError = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          releaseRollbackConnection(conn);
          return myError;
        }

        CopyFromGLJournalData[] dataLines = CopyFromGLJournalData.selectLines(this,
            data[i].glJournalId);
        for (int j = 0; dataLines != null && j < dataLines.length; j++) {
          String strLineSequence = SequenceIdData.getUUID();
          try {
            String fundsavailable = CopyFromGLJournalData.getFundsavailable(this,
                dataLines[j].budgetlineid);
            if (CopyFromGLJournalData.insertGLJournalLine(conn, this, strLineSequence,
                vars.getClient(), dataLines[j].adOrgId, vars.getUser(), strSequence,
                dataLines[j].line, dataLines[j].isgenerated, dataLines[j].description,
                dataLines[j].amtsourcedr, dataLines[j].amtsourcecr, dataLines[j].cCurrencyId,
                dataLines[j].currencyratetype, dataLines[j].currencyrate, dataLines[j].amtacctdr,
                dataLines[j].amtacctcr, dataLines[j].cUomId, dataLines[j].qty,
                dataLines[j].cValidcombinationId, dataLines[j].user1Id, dataLines[j].user2Id,
                dataLines[j].cCampaignId, dataLines[j].cProjectId, dataLines[j].cActivityId,
                dataLines[j].cSalesregionId, dataLines[j].mProductId, dataLines[j].cBpartnerId,
                dataLines[j].aAssetId, dataLines[j].cCostcenterId, dataLines[j].openItems,
                dataLines[j].finFinancialAccountId, dataLines[j].finPaymentmethodId,
                dataLines[j].cGlitemId,
                (dataLines[j].paymentdate.isEmpty()) ? dataLines[j].paymentdate : data[i].dateacct,
                null, dataLines[j].uniquecode, fundsavailable) == 0)
              log4j.warn("Save: GLJournalLine record " + j + " not inserted. Sequence = "
                  + strLineSequence);
          } catch (ServletException ex) {
            myError = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myError;
          }
        }
      }

      releaseCommitConnection(conn);
      myError = new OBError();
      myError.setType("Success");
      myError.setTitle("");
      myError.setMessage(Utility.messageBD(this, "Success", vars.getLanguage()));
    } catch (Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      log4j.warn("Rollback in transaction");
      myError = Utility.translateError(this, vars, vars.getLanguage(), "@CODE=ProcessRunError");
    }
    return myError;
  }

  /**
   * This method is used to print page
   * 
   * @param response
   * @param vars
   * @param strDescription
   * @param strDocumentNo
   * @param strWindow
   * @param strTab
   * @param strKey
   * @throws IOException
   * @throws ServletException
   */
  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strDescription, String strDocumentNo, String strWindow, String strTab, String strKey)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: Button process copy GLJournalBatch details");
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("sa/elm/ob/finance/ad_process/GLJournal/CopyFromGLJournal")
        .createXmlDocument();
    CopyFromGLJournalData[] data = CopyFromGLJournalData.selectFrom(this, strDescription,
        strDocumentNo, vars.getClient(),
        Utility.getContext(this, vars, "#User_Org", "CopyFromGLJournal"), strKey);
    xmlDocument.setData("structure1", data);
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("window", strWindow);
    xmlDocument.setParameter("tab", strTab);
    xmlDocument.setParameter("key", strKey);
    {
      OBError myMessage = vars.getMessage(strTab);
      vars.removeMessage(strTab);
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "Servlet Project set Type";
  } // end of getServletInfo() method
}
