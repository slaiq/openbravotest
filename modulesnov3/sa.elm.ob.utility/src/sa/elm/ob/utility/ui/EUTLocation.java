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
 * All portions are Copyright (C) 2001-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.utility.ui;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.geography.City;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

public class EUTLocation extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static String tabId = "";

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    try {
      OBContext.setAdminMode();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String windowId = null, cityId = null, regionId = null, countryId = null, adOrgId = null;

      if (vars.commandIn("DEFAULT")) {
        tabId = vars.getStringParameter("TabID");
        vars.getRequestGlobalVariable("inpIDValue", "Location.inpcLocationId");
        windowId = vars.getRequestGlobalVariable("WindowID", "Location.inpwindowId");
        adOrgId = vars.getGlobalVariable("inpadOrgId", windowId + "|AD_Org_ID", "");
        if (!"".equals(adOrgId)) {
          vars.setSessionValue("Location.inpadOrgId", adOrgId);
        }
        printPageFS(response, vars);
      } else if (vars.commandIn("KEY")) {
        String strcLocationId = vars.getStringParameter("inpIDValue");
        // String strcLocationId = vars.getStringParameter("inpNameValue");
        if (log4j.isDebugEnabled())
          log4j.debug("1 Location: " + strcLocationId);
        EUTLocationSearchData[] data = EUTLocationSearchData.select(this, vars.getLanguage(),
            strcLocationId);
        if (data != null && data.length == 1) {
          printPageKey(response, vars, data[0]);
        } else {
          vars.setSessionValue("Location.inpcLocationId", strcLocationId);
          vars.getRequestGlobalVariable("inpwindowId", "Location.inpwindowId");
          printPageFS(response, vars);
        }
      } else if (vars.commandIn("FRAME1")) {
        String strcLocationId = vars.getSessionValue("Location.inpcLocationId");
        vars.removeSessionValue("Location.inpcLocationId");
        String strWindow = vars.getSessionValue("Location.inpwindowId");
        vars.removeSessionValue("Location.inpwindowId");
        String stradOrgId = vars.getSessionValue("Location.inpadOrgId");
        vars.removeSessionValue("Location.inpadOrgId");
        // if window id is 122 then it is country and region window , so we have populate region and
        // city from parent tab
        if (strWindow.equals("122")) {
          regionId = vars.getGlobalVariable("inpcRegionId", strWindow + "|C_REGION_ID", "");
          cityId = vars.getGlobalVariable("inpcCityId", strWindow + "|C_CITY_ID", "");
          countryId = vars.getGlobalVariable("inpcCountryId", strWindow + "|C_Country_ID", "");
          printCountrynRegion(response, vars, strcLocationId, strWindow, stradOrgId, countryId,
              regionId, cityId);
        } else {
          printPageSheet(response, vars, strcLocationId, strWindow, stradOrgId);
        }
      } else if (vars.commandIn("SAVE_NEW")) {
        EUTLocationSearchData data = getEditVariables(vars);
        String strSequence = SequenceIdData.getUUID();
        data.cLocationId = strSequence;
        data.insert(this);
        data.name = EUTLocationSearchData.locationAddress(this, vars.getLanguage(),
            data.cLocationId);
        printPageKey(response, vars, data);
      } else if (vars.commandIn("SAVE_EDIT")) {
        EUTLocationSearchData data = getEditVariables(vars);
        data.update(this);
        data.name = EUTLocationSearchData.locationAddress(this, vars.getLanguage(),
            data.cLocationId);
        printPageKey(response, vars, data);
      } else if (vars.commandIn("OBTENER_ARRAY")) {
        // onchange function country field
        // Populate city and region for corresponding country field change
        String strcCountryId = vars.getStringParameter("inpcCountryId");
        String strWindow = vars.getSessionValue("Location.inpwindowId");
        getCitiesAndRegions(response, vars, strcCountryId, strWindow);
      } else if (vars.commandIn("CITY_ARRAY")) {
        // onchange event of region field
        // Populate city based on region selection
        final String strcRegionID = vars.getStringParameter("inpcRegionId");
        String strWindow = vars.getSessionValue("Location.inpwindowId");
        getCityBasedOnRegion(response, vars, strcRegionID, strWindow);
      } else if (vars.commandIn("REGION_ARRAY")) {
        // onchange event of city field
        // populate region and make it readonly
        String strcCityID = vars.getStringParameter("inpCCityId");
        String strWindow = vars.getSessionValue("Location.inpwindowId");
        getRegionFromCity(response, vars, strcCityID, strWindow);
      } else
        pageError(response);
    } catch (Exception e) {
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private EUTLocationSearchData getEditVariables(VariablesSecureApp vars) {
    EUTLocationSearchData data = new EUTLocationSearchData();
    data.cLocationId = vars.getStringParameter("inpCLocationId");
    data.adClientId = vars.getClient();
    data.adOrgId = vars.getStringParameter("inpadOrgId");
    if ("".equals(data.adOrgId)) {
      data.adOrgId = vars.getOrg();
    }
    data.createdby = vars.getUser();
    data.updatedby = vars.getUser();
    data.cCountryId = vars.getStringParameter("inpcCountryId");
    data.cRegionId = vars.getStringParameter("inpcRegionId");
    data.cCityId = vars.getStringParameter("inpCCityId");
    data.address1 = vars.getStringParameter("inpAddress1");
    data.address2 = vars.getStringParameter("inpAddress2");
    data.postal = vars.getStringParameter("inpPostal");

    // pass city name into database
    City city = OBDal.getInstance().get(City.class, data.cCityId);
    if (city != null) {
      if (city.getName() != null) {
        data.city = city.getName();
      }
    }

    return data;
  }

  private void printPageKey(HttpServletResponse response, VariablesSecureApp vars,
      EUTLocationSearchData data) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: Location seeker Frame Set");
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/info/SearchUniqueKeyResponse")
        .createXmlDocument();

    xmlDocument.setParameter("script", generateResult(data));
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private String generateResult(EUTLocationSearchData data) throws IOException, ServletException {
    StringBuffer html = new StringBuffer();

    html.append("\nfunction validateSelector() {\n");
    html.append("var key = \"" + data.cLocationId + "\";\n");
    html.append("var text = \""
        + Replace.replace(Replace.replace(data.name, "\\", "\\\\"), "\"", "\\\\\\\"") + "\";\n");
    html.append("var theOpener = parent.opener || getFrame('LayoutMDI');\n");
    html.append("theOpener.closeSearch(\"SAVE\", key, text);\n");
    html.append("}\n");
    return html.toString();
  }

  private void printPageFS(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: FS Locations seeker");

    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("sa/elm/ob/utility/ui/Location_FS")
        .createXmlDocument();

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strcLocationId, String strWindow, String stradOrgId)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: F1 Locations seeker");
    XmlDocument xmlDocument;

    EUTLocationSearchData[] data;

    if (strcLocationId.equals("")) {
      xmlDocument = xmlEngine.readXmlTemplate("sa/elm/ob/utility/ui/Location_F1")
          .createXmlDocument();
      xmlDocument.setParameter("Command", "NEW");

      // Set default country as saudiarabia
      String strDefaultCountry = EUTLocationSearchData.selectDefaultCountry(this, vars.getOrg(),
          Utility.getContext(this, vars, "#User_Client", strWindow));
      if (strDefaultCountry.equals("")) {
        strDefaultCountry = "296";
      }
      data = EUTLocationSearchData.set(strDefaultCountry);
    } else {
      xmlDocument = xmlEngine.readXmlTemplate("sa/elm/ob/utility/ui/Location_F1")
          .createXmlDocument();
      xmlDocument.setParameter("Command", "EDIT");
      if (log4j.isDebugEnabled())
        log4j.debug("2 Location: " + strcLocationId);
      data = EUTLocationSearchData.select(this, vars.getLanguage(), strcLocationId);
    }
    xmlDocument.setParameter("inpadOrgId", stradOrgId);
    xmlDocument.setParameter("inpwindowId", "var strWindow =\"" + strWindow + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setData("structure1", data);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Country_ID", "",
          "", Utility.getReferenceableOrg(vars, vars.getOrg()),
          Utility.getContext(this, vars, "#User_Client", strWindow), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, strWindow, data[0].cCountryId);
      xmlDocument.setData("reportCountry", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Region_ID", "",
          "C_Region of Country", Utility.getReferenceableOrg(vars, vars.getOrg()),
          Utility.getContext(this, vars, "#User_Client", strWindow), 0);
      Utility.fillSQLParameters(this, vars, data[0], comboTableData, strWindow, data[0].cRegionId);
      xmlDocument.setData("reportRegion", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_City_ID", "",
          "ESCM_CityBasedOnCountry", Utility.getReferenceableOrg(vars, vars.getOrg()),
          Utility.getContext(this, vars, "#User_Client", strWindow), 0);
      Utility.fillSQLParameters(this, vars, data[0], comboTableData, strWindow, data[0].cCityId);
      xmlDocument.setData("reportCity", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void getCityBasedOnRegion(HttpServletResponse response, VariablesSecureApp vars,
      String strcCountryId, String strWindow) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: F2 Locations seeker");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("sa/elm/ob/utility/ui/Location_F3")
        .createXmlDocument();
    if (strcCountryId.equals("")) {
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_City_ID", "",
            "ESCM_CityBasedOnCountry", Utility.getReferenceableOrg(vars, vars.getOrg()),
            Utility.getContext(this, vars, "#User_Client", strWindow), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, strWindow, "");
        xmlDocument.setParameter("array",
            Utility.arrayEntradaSimple("cities", comboTableData.select(false)));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
    } else {
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_City_ID", "",
            "Escm_CityBasedOnRegion",
            Utility.getContext(this, vars, "#AccessibleOrgTree", strWindow),
            Utility.getContext(this, vars, "#User_Client", strWindow), 0);
        Utility.fillSQLParameters(this, vars, null, comboTableData, strWindow, "");
        xmlDocument.setParameter("array",
            Utility.arrayEntradaSimple("cities", comboTableData.select(false)));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
    }
    xmlDocument.setParameter("inpwindowId", "var strWindow =\"" + strWindow + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void getRegionFromCity(HttpServletResponse response, VariablesSecureApp vars,
      String strcCountryId, String strWindow) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: F2 Locations seeker");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("sa/elm/ob/utility/ui/Location_F4")
        .createXmlDocument();
    try {
      City city = OBDal.getInstance().get(City.class, strcCountryId);
      if (city != null) {
        if (city.getRegion() != null) {
          xmlDocument.setParameter("array", "var regions= new Array( new Array(\""
              + city.getRegion().getId() + "\",\"" + city.getRegion().getName() + "\"))");
        } else {
          xmlDocument.setParameter("array", "var regions = null");
        }
      } else {
        try {
          ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Region_ID",
              "", "C_Region of Country",
              Utility.getContext(this, vars, "#AccessibleOrgTree", strWindow),
              Utility.getContext(this, vars, "#User_Client", strWindow), 0);
          Utility.fillSQLParameters(this, vars, null, comboTableData, strWindow, "");
          xmlDocument.setParameter("array",
              Utility.arrayEntradaSimple("regions", comboTableData.select(false)));
          comboTableData = null;
        } catch (Exception ex) {
          throw new ServletException(ex);
        }

      }
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("inpwindowId", "var strWindow =\"" + strWindow + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void getCitiesAndRegions(HttpServletResponse response, VariablesSecureApp vars,
      String strcCountryId, String strWindow) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: F2 Locations seeker");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("sa/elm/ob/utility/ui/Location_F2")
        .createXmlDocument();
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Region_ID", "",
          "C_Region of Country", Utility.getContext(this, vars, "#AccessibleOrgTree", strWindow),
          Utility.getContext(this, vars, "#User_Client", strWindow), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, strWindow, "");
      xmlDocument.setParameter("array",
          Utility.arrayEntradaSimple("regions", comboTableData.select(false)));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_City_ID", "",
          "ESCM_CityBasedOnCountry",
          Utility.getContext(this, vars, "#AccessibleOrgTree", strWindow),
          Utility.getContext(this, vars, "#User_Client", strWindow), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, strWindow, "");
      xmlDocument.setParameter("cities",
          Utility.arrayEntradaSimple("cities", comboTableData.select(false)));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("inpwindowId", "var strWindow =\"" + strWindow + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printCountrynRegion(HttpServletResponse response, VariablesSecureApp vars,
      String strcLocationId, String strWindow, String stradOrgId, String countryId, String regionId,
      String cityId) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: F1 Locations seeker");
    XmlDocument xmlDocument;

    EUTLocationSearchData[] data;
    if (strcLocationId.equals("")) {
      xmlDocument = xmlEngine.readXmlTemplate("sa/elm/ob/utility/ui/Location_F1")
          .createXmlDocument();
      xmlDocument.setParameter("Command", "NEW");
      data = EUTLocationSearchData.setCityandRegion(countryId, cityId, regionId);

      if (!cityId.isEmpty() && tabId.equals("3E7A2DB9FD36425091C04A13B59DDDBE")) {
        City city = OBDal.getInstance().get(City.class, cityId);
        if (city != null) {
          if (city.getRegion() != null) {
            data[0].cRegionId = city.getRegion().getId();
          } else {
            data[0].cRegionId = "";
          }
        }
      } else {
        data[0].cCityId = "";
      }

    } else {
      xmlDocument = xmlEngine.readXmlTemplate("sa/elm/ob/utility/ui/Location_F1")
          .createXmlDocument();
      xmlDocument.setParameter("Command", "EDIT");
      if (log4j.isDebugEnabled())
        log4j.debug("2 Location: " + strcLocationId);
      data = EUTLocationSearchData.select(this, vars.getLanguage(), strcLocationId);
    }
    xmlDocument.setData("structure1", data);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Country_ID", "",
          "", Utility.getReferenceableOrg(vars, vars.getOrg()),
          Utility.getContext(this, vars, "#User_Client", strWindow), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, strWindow, data[0].cCountryId);
      xmlDocument.setData("reportCountry", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Region_ID", "",
          "", Utility.getReferenceableOrg(vars, vars.getOrg()),
          Utility.getContext(this, vars, "#User_Client", strWindow), 0);
      Utility.fillSQLParameters(this, vars, data[0], comboTableData, strWindow, data[0].cRegionId);
      xmlDocument.setData("reportRegion", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    if (tabId.equals("3E7A2DB9FD36425091C04A13B59DDDBE")) {
      // if the parent tab is city, then get city from the parent tab
      xmlDocument.setParameter("disableCity", "var disableCity =\"" + true + "\";");
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_City_ID", "",
            "", Utility.getReferenceableOrg(vars, vars.getOrg()),
            Utility.getContext(this, vars, "#User_Client", strWindow), 0);
        Utility.fillSQLParameters(this, vars, data[0], comboTableData, strWindow, data[0].cCityId);
        xmlDocument.setData("reportCity", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
    } else {
      // if the parent tab is region , get city from the based on region
      try {
        ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_City_ID", "",
            "Escm_CityBasedOnRegion", Utility.getReferenceableOrg(vars, vars.getOrg()),
            Utility.getContext(this, vars, "#User_Client", strWindow), 0);
        Utility.fillSQLParameters(this, vars, data[0], comboTableData, strWindow,
            data[0].cRegionId);
        xmlDocument.setData("reportCity", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

    }
    xmlDocument.setParameter("inpwindowId", "var strWindow =\"" + strWindow + "\";");
    xmlDocument.setParameter("inpadOrgId", stradOrgId);
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("theme", vars.getTheme());

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  public String getServletInfo() {
    return "Servlet that presents the Locations seeker";
  } // end of
    // getServletInfo()
    // method
}
