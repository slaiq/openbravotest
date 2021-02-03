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
 * All portions are Copyright (C) 2009-2016 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.SQLGrammarException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.EnumerateDomainType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.Process;
import org.openbravo.client.application.RefWindow;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.window.OBViewUtil;
import org.openbravo.client.kernel.BaseKernelServlet;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.OBUserException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.reference.NumberUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Element;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.portal.PortalAccessible;
import org.openbravo.service.json.DefaultJsonDataService;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.service.web.InvalidContentException;
import org.openbravo.service.web.InvalidRequestException;
import org.openbravo.service.web.ResourceNotFoundException;
import org.openbravo.service.web.WebServiceUtil;
import org.openbravo.utils.Replace;

/**
 * A web service which provides a JSON REST service using the {@link DataSourceService}
 * implementation. Retrieves the data source using the {@link DataSourceServiceProvider}.
 * 
 * @author mtaal
 */
public class DataSourceServlet extends BaseKernelServlet {
  private static final Logger log = Logger.getLogger(DataSourceServlet.class);

  private static final long serialVersionUID = 1L;

  private static String servletPathPart = "org.openbravo.service.datasource";

  public static String getServletPathPart() {
    return servletPathPart;
  }

  @Inject
  private DataSourceServiceProvider dataSourceServiceProvider;

  @Override
  public void init(ServletConfig config) {
    if (config.getInitParameter(DataSourceConstants.URL_NAME_PARAM) != null) {
      servletPathPart = config.getInitParameter(DataSourceConstants.URL_NAME_PARAM);
    }

    super.init(config);
  }

  public void service(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException {

    try {
      if (OBContext.getOBContext() != null && OBContext.getOBContext().isPortalRole()) {
        if (!(getDataSource(request) instanceof PortalAccessible)) {
          log.error("Portal user " + OBContext.getOBContext().getUser() + " with role "
              + OBContext.getOBContext().getRole()
              + " is trying to access to non granted datasource " + request.getRequestURL());
          throw new OBSecurityException();
        }
      }

      SessionInfo.setModuleId(request.getParameter("moduleId"));
      SessionInfo.setCommand(request.getParameter(DataSourceConstants.OPERATION_TYPE_PARAM));
      SessionInfo.setProcessId(request.getParameter("tabId"));
      SessionInfo.setProcessType("W");

      super.service(request, response);
    } catch (final InvalidRequestException e) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(400);
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } catch (final InvalidContentException e) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(409);
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } catch (final ResourceNotFoundException e) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(404);
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } catch (final OBSecurityException e) {
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(401);
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } catch (final Throwable t) {
      t.printStackTrace(System.err);
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().setDoRollback(true);
      }
      response.setStatus(500);
      log.error(t.getMessage(), t);
      writeResult(response, JsonUtils.convertExceptionToJson(t));
    }
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    final Map<String, String> parameters = getParameterMap(request);
    doFetch(request, response, parameters);
  }

  private void doFetch(HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws IOException, ServletException {
    // checks and set parameters, if not valid then go away
    if (!checkSetParameters(request, response, parameters)) {
      return;
    }

    if (log.isDebugEnabled()) {
      getRequestContent(request);
    }
    try {
      if (!hasAccess(request, parameters.get("tabId"))) {
        throw new OBUserException("AccessTableNoView");
      }

      String filterClass = parameters.get(DataSourceConstants.DS_FILTERCLASS_PARAM);
      if (filterClass != null) {
        try {
          DataSourceFilter filter = (DataSourceFilter) Class.forName(filterClass).newInstance();
          filter.doFilter(parameters, request);
        } catch (Exception e) {
          log.error("Error trying to apply datasource filter with class: " + filterClass, e);
        }
      }
      // now do the action
      boolean isExport = "true".equals(parameters.get("exportToFile"));
      if (isExport) {
        String exportAs = parameters.get("exportAs");
        if (StringUtils.isEmpty(exportAs)) {
          exportAs = "csv";
        }
        if ("csv".equals(exportAs)) {
          try {
            OBContext.setAdminMode();
            try {
              Window window = parameters.get(JsonConstants.TAB_PARAMETER) == null
                  || parameters.get(JsonConstants.TAB_PARAMETER).equals("undefined") ? null : OBDal
                  .getInstance().get(Tab.class, parameters.get(JsonConstants.TAB_PARAMETER))
                  .getWindow();
              String encoding = Preferences.getPreferenceValue("OBSERDS_CSVTextEncoding", true,
                  OBContext.getOBContext().getCurrentClient(), OBContext.getOBContext()
                      .getCurrentOrganization(), OBContext.getOBContext().getUser(), OBContext
                      .getOBContext().getRole(), window);
              response.setContentType("text/csv; charset=" + encoding);
            } catch (PropertyNotFoundException e) {
              // There is no preference for encoding, using standard one which works on Excel
              response.setContentType("text/csv; charset=iso-8859-1");
            }
          } finally {
            OBContext.restorePreviousMode();
          }
          response.setHeader("Content-Disposition", "attachment; filename=ExportedData.csv");
          QueryJSONWriterToCSV writer;
          if (getDataSource(request) instanceof DefaultDataSourceService) {
            writer = new QueryJSONWriterToCSV(request, response, parameters, getDataSource(request)
                .getEntity());
            // when exporting a OB grid, the isActive filter should not be set
            parameters.put(JsonConstants.NO_ACTIVE_FILTER, "true");
            ((DefaultDataSourceService) getDataSource(request)).fetch(parameters, writer);
          } else {
            String result = getDataSource(request).fetch(parameters);
            JSONObject jsonResult = new JSONObject(result);
            JSONArray data = jsonResult.getJSONObject("response").getJSONArray("data");
            writer = new QueryJSONWriterToCSV(request, response, parameters, getDataSource(request)
                .getEntity());
            for (int i = 0; i < data.length(); i++) {
              writer.write(data.getJSONObject(i));
            }
          }

          writer.writeCSVFooterNote(parameters);
        } else {
          log.error("Unsupported export format: " + exportAs);
        }
      } else {
        String result = getDataSource(request).fetch(parameters);
        writeResult(response, result);
      }
    } catch (Exception e) {
      handleException(e, response);
    }
  }

  private class QueryJSONWriterToCSV extends DefaultJsonDataService.QueryResultWriter {

    Writer writer;
    String fieldSeparator;
    String decimalSeparator;
    String prefDecimalSeparator;
    List<String> fieldProperties;
    Map<String, String> niceFieldProperties = new HashMap<String, String>();
    boolean propertiesWritten = false;
    Map<String, Map<String, String>> refLists = new HashMap<String, Map<String, String>>();
    List<String> refListCols = new ArrayList<String>();
    List<String> dateCols = new ArrayList<String>();
    List<String> dateTimeCols = new ArrayList<String>();
    List<String> timeCols = new ArrayList<String>();
    List<String> numericCols = new ArrayList<String>();
    Map<String, DecimalFormat> formats = new HashMap<String, DecimalFormat>();
    int clientUTCOffsetMiliseconds;
    TimeZone clientTimeZone;

    public QueryJSONWriterToCSV(HttpServletRequest request, HttpServletResponse response,
        Map<String, String> parameters, Entity entity) {
      try {
        OBContext.setAdminMode();
        response.setHeader("Content-Disposition", "attachment; filename=ExportedData.csv");
        writer = response.getWriter();
        VariablesSecureApp vars = new VariablesSecureApp(request);
        Window window = parameters.get(JsonConstants.TAB_PARAMETER) == null
            || parameters.get(JsonConstants.TAB_PARAMETER).equals("undefined") ? null : OBDal
            .getInstance().get(Tab.class, parameters.get(JsonConstants.TAB_PARAMETER)).getWindow();
        try {
          prefDecimalSeparator = Preferences.getPreferenceValue("OBSERDS_CSVDecimalSeparator",
              true, OBContext.getOBContext().getCurrentClient(), OBContext.getOBContext()
                  .getCurrentOrganization(), OBContext.getOBContext().getUser(), OBContext
                  .getOBContext().getRole(), window);
        } catch (PropertyNotFoundException e) {
          // There is no preference for the decimal separator.
        }
        decimalSeparator = vars.getSessionValue("#DecimalSeparator|generalQtyEdition").substring(0,
            1);
        try {
          fieldSeparator = Preferences.getPreferenceValue("OBSERDS_CSVFieldSeparator", true,
              OBContext.getOBContext().getCurrentClient(), OBContext.getOBContext()
                  .getCurrentOrganization(), OBContext.getOBContext().getUser(), OBContext
                  .getOBContext().getRole(), window);
        } catch (PropertyNotFoundException e) {
          // There is no preference for the field separator. Using the default one.
          fieldSeparator = ",";
        }
        if ((prefDecimalSeparator != null && prefDecimalSeparator.equals(fieldSeparator))
            || (prefDecimalSeparator == null && decimalSeparator.equals(fieldSeparator))) {
          if (!fieldSeparator.equals(";")) {
            fieldSeparator = ";";
          } else {
            fieldSeparator = ",";
          }
          log.warn("Warning: CSV Field separator is identical to the decimal separator. Changing the field separator to "
              + fieldSeparator + " to avoid generating a wrong CSV file");
        }
        if (parameters.get("_UTCOffsetMiliseconds").length() > 0) {
          clientUTCOffsetMiliseconds = Integer.parseInt(parameters.get("_UTCOffsetMiliseconds"));
        } else {
          clientUTCOffsetMiliseconds = 0;
        }

        clientTimeZone = null;
        try {
          String clientTimeZoneId = Preferences.getPreferenceValue("localTimeZoneID", true,
              OBContext.getOBContext().getCurrentClient(), OBContext.getOBContext()
                  .getCurrentOrganization(), OBContext.getOBContext().getUser(), OBContext
                  .getOBContext().getRole(), null);
          List<String> validTimeZoneIDs = Arrays.asList(TimeZone.getAvailableIDs());
          if (validTimeZoneIDs.contains(clientTimeZoneId)) {
            clientTimeZone = TimeZone.getTimeZone(clientTimeZoneId);
          } else {
            log4j
                .error(clientTimeZoneId
                    + " is not a valid time zone identifier. For a list of all accepted identifiers check http://www.java2s.com/Tutorial/Java/0120__Development/GettingallthetimezonesIDs.htm");
          }
        } catch (PropertyException pe) {
          log4j
              .warn("The local Local Timezone ID property is not defined. It can be defined in a preference. For a list of all accepted values check http://www.java2s.com/Tutorial/Java/0120__Development/GettingallthetimezonesIDs.htm");
        }

        fieldProperties = new ArrayList<String>();
        if (parameters.get("viewState") != null
            && !parameters.get("viewState").toString().equals("undefined")) {
          String viewStateO = parameters.get("viewState");
          String viewStateWithoutParenthesis = viewStateO.substring(1, viewStateO.length() - 1);
          JSONObject viewState = new JSONObject(viewStateWithoutParenthesis);
          String fieldA = viewState.getString("field");
          JSONArray fields = new JSONArray(fieldA);
          for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (field.has("visible") && !field.getBoolean("visible")) {
              // The field is not visible. We should not export it
              continue;
            }
            if (field.getString("name").equals("_checkboxField")
                || field.getString("name").equals("_editLink")) {
              continue;
            }
            fieldProperties.add(field.getString("name"));
          }
        }

        // Now we calculate ref lists and nice property names
        final String userLanguageId = OBContext.getOBContext().getLanguage().getId();
        if (entity != null) {
          final Map<String, Property> properties = new HashMap<String, Property>();
          for (Property prop : entity.getProperties()) {
            if (!fieldProperties.contains(prop.getName())) {
              continue;
            }
            properties.put(prop.getName(), prop);
          }
          for (String fieldProperty : fieldProperties) {
            if (fieldProperty.contains(DalUtil.FIELDSEPARATOR)) {
              properties.put(fieldProperty, DalUtil.getPropertyFromPath(entity, fieldProperty));
            }
          }

          for (String propKey : properties.keySet()) {
            final Property prop = properties.get(propKey);
            Column col = OBDal.getInstance().get(Column.class, prop.getColumnId());

            if (prop.isAuditInfo()) {
              Element element = null;
              if ("creationDate".equals(propKey)) {
                element = OBViewUtil.createdElement;
              } else if ("createdBy".equals(propKey)) {
                element = OBViewUtil.createdByElement;
              } else if ("updated".equals(propKey)) {
                element = OBViewUtil.updatedElement;
              } else if ("updatedBy".equals(propKey)) {
                element = OBViewUtil.updatedByElement;
              }
              if (element != null) {
                niceFieldProperties.put(propKey,
                    OBViewUtil.getLabel(element, element.getADElementTrlList()));
              } else {
                niceFieldProperties.put(propKey, col.getName());
              }
            } else if (parameters.get(JsonConstants.TAB_PARAMETER) != null
                && !parameters.get(JsonConstants.TAB_PARAMETER).equals("")) {
              Tab tab = OBDal.getInstance().get(Tab.class,
                  parameters.get(JsonConstants.TAB_PARAMETER));
              for (Field field : tab.getADFieldList()) {
                if (field.getColumn() == null || !field.getColumn().getId().equals(col.getId())) {
                  continue;
                }
                niceFieldProperties.put(propKey, field.getName());
                for (FieldTrl fieldTrl : field.getADFieldTrlList()) {
                  if (fieldTrl.getLanguage().getId().equals(userLanguageId)) {
                    niceFieldProperties.put(propKey, fieldTrl.getName());
                  }
                }
              }
            } else {
              niceFieldProperties.put(propKey, col.getName());
            }
            UIDefinition uiDef = UIDefinitionController.getInstance().getUIDefinition(col.getId());
            if (uiDef instanceof NumberUIDefinition) {
              formats.put(propKey,
                  Utility.getFormat(vars, ((NumberUIDefinition) uiDef).getFormat()));
            }

            // We also store the date properties
            if (prop.isDate()) {
              dateCols.add(propKey);
            } else if (prop.isDatetime()) {
              dateTimeCols.add(propKey);
            } else if (prop.isTime()) {
              timeCols.add(propKey);
            } else if (prop.isPrimitive() && prop.isNumericType()) {
              numericCols.add(propKey);
            }

            if (!(prop.getDomainType() instanceof EnumerateDomainType)) {
              continue;
            }
            String referenceId = col.getReferenceSearchKey().getId();
            Map<String, String> reflists = new HashMap<String, String>();
            final String hql = "select al.searchKey, al.name from ADList al where "
                + " al.reference.id=? and al.active=true";
            final Query qry = OBDal.getInstance().getSession().createQuery(hql);
            qry.setString(0, referenceId);
            for (Object o : qry.list()) {
              final Object[] row = (Object[]) o;
              reflists.put(row[0].toString(), row[1].toString());
            }
            final String hqltrl = "select al.searchKey, trl.name from ADList al, ADListTrl trl where "
                + " al.reference.id=? and trl.listReference=al and trl.language.id=?"
                + " and al.active=true and trl.active=true";
            final Query qrytrl = OBDal.getInstance().getSession().createQuery(hqltrl);
            qrytrl.setString(0, referenceId);
            qrytrl.setString(1, userLanguageId);
            for (Object o : qrytrl.list()) {
              final Object[] row = (Object[]) o;
              reflists.put(row[0].toString(), row[1].toString());
            }
            refListCols.add(propKey);
            refLists.put(propKey, reflists);
          }
        }
        
        /**
         * Adding BOM Characters
         * */
        writer.append("\ufeff");
        if(fieldProperties.size() > 0 ) {
        	for (int i = 0; i < fieldProperties.size(); i++) {
        		if(i>0){
        			writer.append(fieldSeparator);
        		}
        	}
        }
        writer.append("\n");
        
        writeCSVHeaderNote(parameters);
        if (fieldProperties.size() > 0) {
          // If the request came with the view state information, we get the properties from there
          for (int i = 0; i < fieldProperties.size(); i++) {
            if (i > 0) {
              writer.append(fieldSeparator);
            }
            if (niceFieldProperties.get(fieldProperties.get(i)) != null) {
              writer.append("\"").append(niceFieldProperties.get(fieldProperties.get(i)))
                  .append("\"");
            }
          }
          propertiesWritten = true;
        }
      } catch (Exception e) {
        throw new OBException("Error while exporting a CSV file", e);
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    private void writeJSONProperties(JSONObject row) {
      final Iterator<?> itKeysF = row.keys();
      Vector<String> keys = new Vector<String>();
      boolean isFirst = true;
      try {
        while (itKeysF.hasNext()) {
          String key = (String) itKeysF.next();
          if (key.endsWith(JsonConstants.IDENTIFIER)) {
            continue;
          }
          if (fieldProperties.size() > 0 && !fieldProperties.contains(key)) {
            // Field is not visible. We don't show it
            continue;
          }
          if (isFirst) {
            isFirst = false;
          } else {
            writer.append(fieldSeparator);
          }
          keys.add(key);
          writer.append("\"").append(key).append("\"");
        }
        propertiesWritten = true;
      } catch (Exception e) {
        throw new OBException("Error while writing column names when exporting a CSV file", e);
      }
    }

    public void write(JSONObject json) {
      try {
        if (!propertiesWritten) {
          writeJSONProperties(json);
        }
        writer.append("\n");
        final Iterator<?> itKeys;
        if (fieldProperties.size() > 0) {
          itKeys = fieldProperties.iterator();
        } else {
          itKeys = json.keys();
        }

        boolean isFirst = true;
        while (itKeys.hasNext()) {
          String key = (String) itKeys.next();
          if (key.endsWith(JsonConstants.IDENTIFIER)) {
            continue;
          }
          if (fieldProperties.size() > 0 && !fieldProperties.contains(key)) {
            // Field is not visible. We don't show it
            continue;
          }
          if (isFirst) {
            isFirst = false;
          } else {
            writer.append(fieldSeparator);
          }
          if (!json.has(key)) {
            continue;
          }
          Object keyValue = json.has(key + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER) ? json
              .get(key + DalUtil.FIELDSEPARATOR + JsonConstants.IDENTIFIER) : json.get(key);
          if (refListCols.contains(key)) {
            keyValue = refLists.get(key).get(keyValue);
          } else if (keyValue instanceof Number && keyValue != null) {
            DecimalFormat format = formats.get(key);
            if (format == null) {
              // if the CSV decimal separator property is defined, used it over the character
              // defined in Format.xml
              keyValue = keyValue.toString().replace(".",
                  prefDecimalSeparator != null ? prefDecimalSeparator : decimalSeparator);
            } else {
              keyValue = format.format(new BigDecimal(keyValue.toString()));
              if (prefDecimalSeparator != null) {
                keyValue = keyValue
                    .toString()
                    .replace(
                        new Character(format.getDecimalFormatSymbols().getDecimalSeparator())
                            .toString(),
                        prefDecimalSeparator);
              }

            }
          } else if (dateCols.contains(key) && keyValue != null
              && !keyValue.toString().equals("null")) {
            Date date = JsonUtils.createDateFormat().parse(keyValue.toString());
            String pattern = RequestContext.get().getSessionAttribute("#AD_JAVADATEFORMAT")
                .toString();
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
            dateFormat.setLenient(true);
            keyValue = dateFormat.format(date);
          } else if (dateTimeCols.contains(key) && keyValue != null
              && !keyValue.toString().equals("null")) {
            final String repairedString = JsonUtils.convertFromXSDToJavaFormat(keyValue.toString());
            Date localDate = JsonUtils.createDateTimeFormat().parse(repairedString);
            Date clientTimezoneDate = null;
            clientTimezoneDate = convertFromLocalToClientTimezone(localDate);
            String pattern = RequestContext.get().getSessionAttribute("#AD_JAVADATETIMEFORMAT")
                .toString();
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
            dateFormat.setLenient(true);
            keyValue = dateFormat.format(clientTimezoneDate);
          } else if (timeCols.contains(key) && keyValue != null
              && !keyValue.toString().equals("null")) {
            Date UTCdate = JsonUtils.createTimeFormatWithoutGMTOffset().parse(keyValue.toString());
            Date clientTimezoneDate = null;
            clientTimezoneDate = convertFromUTCToClientTimezone(UTCdate);
            SimpleDateFormat timeFormat = JsonUtils.createTimeFormatWithoutGMTOffset();
            timeFormat.setLenient(true);
            keyValue = timeFormat.format(clientTimezoneDate);
          }

          if (keyValue != null && !keyValue.toString().equals("null")) {
            keyValue = Replace.replace(keyValue.toString(), "\"", "\"\"");
          } else {
            keyValue = "";
          }
          if (!numericCols.contains(key)) {
            keyValue = "\"" + keyValue + "\"";
          }
          writer.append(keyValue.toString());
        }
      } catch (Exception e) {
        throw new OBException("Error while exporting CSV information", e);
      }
    }

    private Date convertFromLocalToClientTimezone(Date localDate) {

      Date UTCDate = convertFromLocalToUTCTimezone(localDate);
      Date clientDate = convertFromUTCToClientTimezone(UTCDate);

      return clientDate;
    }

    private Date convertFromUTCToClientTimezone(Date UTCdate) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(UTCdate);
      if (clientTimeZone != null) {
        calendar = Calendar.getInstance(clientTimeZone);
        calendar.setTime(UTCdate);
        int gmtMillisecondOffset = (calendar.get(Calendar.ZONE_OFFSET) + calendar
            .get(Calendar.DST_OFFSET));
        calendar.add(Calendar.MILLISECOND, gmtMillisecondOffset);
      } else {
        calendar = Calendar.getInstance();
        calendar.setTime(UTCdate);
        calendar.add(Calendar.MILLISECOND, clientUTCOffsetMiliseconds);
      }
      return calendar.getTime();
    }

    private Date convertFromLocalToUTCTimezone(Date localDate) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(localDate);

      int gmtMillisecondOffset = (calendar.get(Calendar.ZONE_OFFSET) + calendar
          .get(Calendar.DST_OFFSET));
      calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);

      return calendar.getTime();
    }

    private void writeCSVHeaderNote(Map<String, String> parameters) throws IOException,
        PropertyException {
      final String csvHeaderMsg = getMessage(parameters, "OBSERDS_CSVHeaderMessage");

      if (StringUtils.isNotBlank(csvHeaderMsg)) {
        writer.append("\"").append(csvHeaderMsg).append("\"");
        fillEmptyColumns();
        writer.append("\n");
      }
    }

    private void writeCSVFooterNote(Map<String, String> parameters) throws IOException,
        PropertyException {
      final String csvFooterMsg = getMessage(parameters, "OBSERDS_CSVFooterMessage");

      if (StringUtils.isNotBlank(csvFooterMsg)) {
        writer.append("\n").append("\"").append(csvFooterMsg).append("\"");
        fillEmptyColumns();
      }
    }

    private String getMessage(final Map<String, String> parameters, final String property)
        throws PropertyException {
      OBContext.setAdminMode(true);
      try {
        String csvMessage = null;
        try {
          Window window = parameters.get(JsonConstants.TAB_PARAMETER) == null
              || parameters.get(JsonConstants.TAB_PARAMETER).equals("undefined") ? null : OBDal
              .getInstance().get(Tab.class, parameters.get(JsonConstants.TAB_PARAMETER))
              .getWindow();
          csvMessage = Preferences.getPreferenceValue(property, true, OBContext.getOBContext()
              .getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(), OBContext
              .getOBContext().getUser(), OBContext.getOBContext().getRole(), window);
        } catch (PropertyNotFoundException e) {
          // There is no preference defined
          csvMessage = null;
        }

        if (StringUtils.isNotBlank(csvMessage)) {
          csvMessage = Replace.replace(Replace.replace(
              Replace.replace(OBMessageUtils.messageBD(csvMessage), "\\n", "\n"), "&quot;", "\""),
              "\"", "\"\"");
        }

        return csvMessage;
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    private void fillEmptyColumns() throws IOException {
      for (int i = 1; i < fieldProperties.size(); i++) {
        writer.append(fieldSeparator);
      }
    }
  }

  private void handleException(Exception e, HttpServletResponse response) throws IOException {
    log4j.error(e.getMessage(), e);
    if (e instanceof SQLGrammarException) {
      log.error(((SQLGrammarException) e).getSQL());
    }
    if (!response.isCommitted()) {
      final JSONObject jsonResult = new JSONObject();
      final JSONObject jsonResponse = new JSONObject();
      String result = "";
      try {
        jsonResponse.put(JsonConstants.RESPONSE_STATUS,
            JsonConstants.RPCREQUEST_STATUS_VALIDATION_ERROR);
        jsonResponse.put("error", KernelUtils.getInstance().createErrorJSON(e));
        jsonResult.put(JsonConstants.RESPONSE_RESPONSE, jsonResponse);
        result = jsonResult.toString();
      } catch (JSONException e1) {
        log.error("Error genearating JSON error", e1);
      }
      writeResult(response, result);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    final Map<String, String> parameters = getParameterMap(request);

    setSessionInfo();

    try {
      if (!hasAccess(request, parameters.get("tabId"))) {
        throw new OBUserException("AccessTableNoView");
      }

      if (DataSourceConstants.FETCH_OPERATION.equals(parameters
          .get(DataSourceConstants.OPERATION_TYPE_PARAM))) {
        doFetch(request, response, parameters);
        return;
      }

      // note if clause updates parameter map
      if (checkSetIDDataSourceName(request, response, parameters)) {
        final String result = getDataSource(request).add(parameters, getRequestContent(request));
        writeResult(response, result);
      }
    } catch (Exception e) {
      handleException(e, response);
    }
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final Map<String, String> parameters = getParameterMap(request);
    setSessionInfo();
    try {
      // checks and set parameters, if not valid then go away
      if (!checkSetParameters(request, response, parameters)) {
        return;
      }
      if (!hasAccess(request, parameters.get("tabId"))) {
        throw new OBUserException("AccessTableNoView");
      }

      final String id = parameters.get(JsonConstants.ID);
      if (id == null) {
        throw new InvalidRequestException("No id parameter");
      }

      final String result = getDataSource(request).remove(parameters);
      writeResult(response, result);
    } catch (Exception e) {
      handleException(e, response);
    }
  }

  private String getDataSourceNameFromRequest(HttpServletRequest request) {
    final String url = request.getRequestURI();
    if (url.indexOf(getServletPathPart()) == -1) {
      throw new OBException("Request url " + url + " is not valid");
    }
    final int startIndex = 1 + url.indexOf(getServletPathPart()) + getServletPathPart().length();
    final int endIndex = url.indexOf("/", startIndex + 1);
    final String dsName = (endIndex == -1 ? url.substring(startIndex) : url.substring(startIndex,
        endIndex));

    if (dsName.length() == 0) {
      throw new ResourceNotFoundException("Data source not found using url " + url);
    }
    return dsName;
  }

  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    final Map<String, String> parameters = getParameterMap(request);
    setSessionInfo();
    try {
      if (!hasAccess(request, parameters.get("tabId"))) {
        throw new OBUserException("AccessTableNoView");
      }

      // note if clause updates parameter map
      if (checkSetIDDataSourceName(request, response, parameters)) {
        final String result = getDataSource(request).update(parameters, getRequestContent(request));
        writeResult(response, result);
      }

    } catch (Exception e) {
      handleException(e, response);
    }
  }

  private void setSessionInfo() {
    // FIXME: Because of issue #15331 connection is initialized with temporary audit table before
    // setting session info
    // Reset Session Info in DB manually as it was set in the service but actual information is not
    // available till now.
    SessionInfo.setDBSessionInfo(OBDal.getInstance().getConnection(), OBPropertiesProvider
        .getInstance().getOpenbravoProperties().getProperty("bbdd.rdbms"));
  }

  private boolean checkSetParameters(HttpServletRequest request, HttpServletResponse response,
      Map<String, String> parameters) throws IOException {
    if (!request.getRequestURI().contains("/" + servletPathPart)) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, the path should contain the service name: " + servletPathPart)));
      return false;
    }
    final int nameIndex = request.getRequestURI().indexOf(servletPathPart);
    final String servicePart = request.getRequestURI().substring(nameIndex);
    final String[] pathParts = WebServiceUtil.getInstance().getSegments(servicePart);
    if (pathParts.length == 0 || !pathParts[0].equals(servletPathPart)) {
      writeResult(
          response,
          JsonUtils.convertExceptionToJson(new InvalidRequestException("Invalid url: "
              + request.getRequestURI())));
      return false;
    }
    if (pathParts.length == 1) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, no datasource name: " + request.getRequestURI())));
      return false;
    }
    final String dsName = pathParts[1];
    parameters.put(DataSourceConstants.DS_NAME_PARAM, dsName);
    if (pathParts.length > 2) {
      // search on the exact id
      parameters.put(JsonConstants.ID, pathParts[2]);
      if (!parameters.containsKey(JsonConstants.TEXTMATCH_PARAMETER)) {
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER, JsonConstants.TEXTMATCH_EXACT);
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE, JsonConstants.TEXTMATCH_EXACT);
      }
    }
    return true;
  }

  private Map<String, String> getParameterMap(HttpServletRequest request) {
    final Map<String, String> parameterMap = new HashMap<String, String>();
    for (@SuppressWarnings("rawtypes")
    Enumeration keys = request.getParameterNames(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();

      // do simple conversion of array of values to a string
      // TODO: replace when advancedcriteria are supported
      final String[] values = request.getParameterValues(key);
      if (values.length == 1) {
        parameterMap.put(key, values[0]);
      } else {
        final StringBuilder sb = new StringBuilder();
        for (String value : values) {
          if (sb.length() > 0) {
            sb.append(JsonConstants.IN_PARAMETER_SEPARATOR);
          }
          sb.append(value);
        }
        parameterMap.put(key, sb.toString());
      }
    }
    return parameterMap;
  }

  // NOTE: parameters parameter is updated inside this method
  private boolean checkSetIDDataSourceName(HttpServletRequest request,
      HttpServletResponse response, Map<String, String> parameters) throws IOException {
    if (!request.getRequestURI().contains("/" + servletPathPart)) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, the path should contain the service name: " + servletPathPart)));
      return false;
    }
    final int nameIndex = request.getRequestURI().indexOf(servletPathPart);
    final String servicePart = request.getRequestURI().substring(nameIndex);
    final String[] pathParts = WebServiceUtil.getInstance().getSegments(servicePart);
    if (pathParts.length == 0 || !pathParts[0].equals(servletPathPart)) {
      writeResult(
          response,
          JsonUtils.convertExceptionToJson(new InvalidRequestException("Invalid url: "
              + request.getRequestURI())));
      return false;
    }
    if (pathParts.length == 1) {
      return true;
    }

    final String dsName = pathParts[1];
    parameters.put(DataSourceConstants.DS_NAME_PARAM, dsName);

    if (pathParts.length > 2) {
      // search on the exact id
      parameters.put(JsonConstants.ID, pathParts[2]);
      if (!parameters.containsKey(JsonConstants.TEXTMATCH_PARAMETER)) {
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER, JsonConstants.TEXTMATCH_EXACT);
        parameters.put(JsonConstants.TEXTMATCH_PARAMETER_OVERRIDE, JsonConstants.TEXTMATCH_EXACT);
      }
    }
    return true;
  }

  private DataSourceService getDataSource(HttpServletRequest request) {
    final String dsName = getDataSourceNameFromRequest(request);
    final DataSourceService dataSource = dataSourceServiceProvider.getDataSource(dsName);
    return dataSource;
  }

  private void writeResult(HttpServletResponse response, String result) throws IOException {
    response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
    response.setHeader("Content-Type", JsonConstants.JSON_CONTENT_TYPE);

    final Writer w = response.getWriter();
    w.write(result);
    w.close();
  }

  private String getRequestContent(HttpServletRequest request) throws IOException {
    final BufferedReader reader = request.getReader();
    if (reader == null) {
      return "";
    }
    String line;
    final StringBuilder sb = new StringBuilder();
    while ((line = reader.readLine()) != null) {
      if (sb.length() > 0) {
        sb.append("\n");
      }
      sb.append(line);
    }
    log.debug("REQUEST CONTENT>>>>");
    for (Enumeration<?> enumeration = request.getParameterNames(); enumeration.hasMoreElements();) {
      final Object key = enumeration.nextElement();
      log.debug(key + ": " + request.getParameter((String) key));
    }
    return sb.toString();
  }

  /**
   * Checks access to the current tab, it reuses infrastructure in HttpSecureAppServlet
   */
  private boolean hasAccess(HttpServletRequest req, String tabId) {
    try {
      if (tabId == null || tabId.isEmpty()) {
        return true;
      }
      VariablesSecureApp vars = new VariablesSecureApp(req);
      boolean hasAccess = hasGeneralAccess(vars, "W", tabId);
      if (hasAccess) {
        return true;
      }
      // Here is checked process definition that containing parameters defined as "window",
      // automatically inherit permissions. See issue #29035
      try {
        OBContext.setAdminMode(true);
        Tab tab = OBDal.getInstance().get(Tab.class, tabId);
        if (tab == null) {
          return false;
        }
        // Search window references that linked with the window of the provided tab.
        OBCriteria<RefWindow> obcRefWindow = OBDal.getInstance().createCriteria(RefWindow.class);
        obcRefWindow.add(Restrictions.eq(RefWindow.PROPERTY_WINDOW, tab.getWindow()));
        if (obcRefWindow.list().size() == 0) {
          return false;
        }
        final List<Reference> references = new ArrayList<Reference>();
        for (RefWindow refWindow : obcRefWindow.list()) {
          references.add(refWindow.getReference());
        }

        // Then search parameters that linked with references and get theirs processes.
        OBCriteria<Parameter> obParameters = OBDal.getInstance().createCriteria(Parameter.class);
        obParameters.add(Restrictions.in(Parameter.PROPERTY_REFERENCESEARCHKEY, references));
        if (obParameters.list().size() == 0) {
          return false;
        }
        final List<Process> obuiapProcesses = new ArrayList<Process>();
        for (Parameter parameter : obParameters.list()) {
          obuiapProcesses.add(parameter.getObuiappProcess());
        }

        // Finally select all columns that linked with selected processes and get their fields.
        OBCriteria<Column> columns = OBDal.getInstance().createCriteria(Column.class);
        columns.add(Restrictions.in(Column.PROPERTY_OBUIAPPPROCESS, obuiapProcesses));
        if (columns.list().size() == 0) {
          return false;
        }
        final List<Field> fields = new ArrayList<Field>();
        for (Column col : columns.list()) {
          for (Field field : col.getADFieldList()) {
            fields.add(field);
          }
        }

        Map<String, Object> parameters = new HashMap<String, Object>();
        // Avoid to invoke hasAccess() method with same window.
        final List<String> windowCheck = new ArrayList<String>();
        for (Field f : fields) {
          // Check access with OBUIAPPProcess & windowId for each field.
          String windowId = (String) DalUtil.getId(f.getTab().getWindow());
          if (!windowCheck.contains(windowId)) {
            windowCheck.add(windowId);
            parameters.put("windowId", windowId);
            boolean hasAccessEntity = BaseProcessActionHandler.hasAccess(f.getColumn()
                .getOBUIAPPProcess(), parameters);
            if (hasAccessEntity) {
              return true;
            }
          }
        }
        // here there is no access to current tabId
        return false;
      } catch (final Exception e) {
        log4j.error("Error checking access: ", e);
        return false;
      } finally {
        OBContext.restorePreviousMode();
      }
    } catch (final Exception e) {
      log4j.error("Error checking access: ", e);
      return false;
    }
  }
}
