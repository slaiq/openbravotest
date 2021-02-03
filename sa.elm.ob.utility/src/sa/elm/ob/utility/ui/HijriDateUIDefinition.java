package sa.elm.ob.utility.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.window.FormInitializationComponent;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.reference.UIDefinition;

public class HijriDateUIDefinition extends UIDefinition {

  private static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
  private static final String UIPATTERN = "yyyy-MM-dd";

  private SimpleDateFormat format = null;
  private String lastUsedPattern = null;
  private SimpleDateFormat dateFormat = null;
  private SimpleDateFormat uiDateFormat = null;

  public SimpleDateFormat getFormat() {
    if (format == null) {
      format = new SimpleDateFormat(PATTERN);
      format.setLenient(true);
    }
    return format;
  }

  private SimpleDateFormat getUIFormat() {
    if (uiDateFormat == null) {
      uiDateFormat = new SimpleDateFormat(UIPATTERN);
      uiDateFormat.setLenient(true);
    }
    return uiDateFormat;
  }

  protected SimpleDateFormat getClassicFormat() {
    String pattern = RequestContext.get().getSessionAttribute("#AD_JAVADATEFORMAT").toString();
    if (dateFormat == null || !pattern.equals(lastUsedPattern)) {
      dateFormat = new SimpleDateFormat(pattern);
      lastUsedPattern = pattern;
      dateFormat.setLenient(true);
    }
    return dateFormat;
  }

  @Override
  public synchronized String convertToClassicString(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof String) {
      return (String) value;
    }
    return getClassicFormat().format(value);
  }

  @Override
  public synchronized Object createFromClassicString(String value) {
    try {
      String unrealDate = value;
      try {

        String[] valuearr = value.split(" ");
        if (valuearr.length > 0) {
          String[] dateseparatorarr = valuearr[0].split("-");
          if (dateseparatorarr[0].length() == 4) {
            unrealDate = valuearr[0];
          } else {
            unrealDate = dateseparatorarr[2] + "-" + dateseparatorarr[1] + "-"
                + dateseparatorarr[0];
          }

        } else {
          String[] dateseparatorarr = value.split("-");
          if (dateseparatorarr[0].length() == 4) {
            unrealDate = valuearr[0];
          } else {
            unrealDate = dateseparatorarr[2] + "-" + dateseparatorarr[1] + "-"
                + dateseparatorarr[0];
          }
        }

      } catch (Exception e) {

      }

      Map<String, String> unRealDateMap = FormInitializationComponent.getDateMap();
      if (value == null || value.trim().length() == 0) {
        return null;
      }

      List<String> dateList = getdateList();

      if (dateList.contains(unrealDate)) {
        return value.split(" ").length > 0 ? value.split(" ")[0] : value;
        // return value;
      }
      if (value.contains("T")) {
        return value;
      }
      final Date date = getClassicFormat().parse(value);
      return getUIFormat().format(date);
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  @Override
  public String getParentType() {
    return "date";
  }

  @Override
  public String getFormEditorType() {
    return "OBHijriDateItem";
  }

  @Override
  public String getFilterEditorType() {
    return "OBMiniDateRangeItem";
  }

  @Override
  public String getTypeProperties() {
    final StringBuilder sb = new StringBuilder();
    sb.append("editFormatter: function(value, field, component, record) {"
        + "return OB.Utilities.Date.JSToOB(value, " + getClientFormatObject() + ");" + "},"
        + "parseInput: function(value, field, component, record) {"
        + "return OB.Utilities.Date.OBToJS(value, " + getClientFormatObject() + ");" + "},");
    sb.append("shortDisplayFormatter: function(value, field, component, record) {"
        + "return OB.Utilities.Date.JSToOB(value, " + getClientFormatObject() + ");" + "},"
        + "normalDisplayFormatter: function(value, field, component, record) {"
        + "return OB.Utilities.Date.JSToOB(value, " + getClientFormatObject() + ");" + "},"
        + "createClassicString: function(value) {" + "return OB.Utilities.Date.JSToOB(value, "
        + getClientFormatObject() + ");" + "},");
    sb.append("getGroupingModes: isc.SimpleType.getType('date').getGroupingModes,");
    sb.append("getGroupValue: isc.SimpleType.getType('date').getGroupValue,");
    sb.append("getGroupTitle: isc.SimpleType.getType('date').getGroupTitle,");

    return sb.toString();
  }

  protected String getClientFormatObject() {
    return "OB.Format.date";
  }

  public List<String> getdateList() {
    List<String> dateList = new ArrayList<String>();
    dateList.add("1430-02-29");
    dateList.add("1430-02-30");
    dateList.add("1431-02-29");
    dateList.add("1431-02-30");
    dateList.add("1432-02-30");
    dateList.add("1433-02-29");
    dateList.add("1434-02-29");
    dateList.add("1434-02-30");
    dateList.add("1435-02-29");
    dateList.add("1436-02-30");
    dateList.add("1437-02-29");
    dateList.add("1438-02-29");
    dateList.add("1439-02-29");
    dateList.add("1440-02-30");
    dateList.add("1441-02-29");
    dateList.add("1442-02-29");
    dateList.add("1442-02-30");
    dateList.add("1443-02-29");
    dateList.add("1444-02-30");
    dateList.add("1445-02-29");
    dateList.add("1445-02-30");
    dateList.add("1446-02-29");
    dateList.add("1446-02-30");
    dateList.add("1447-02-29");
    dateList.add("1448-02-30");
    return dateList;
  }
}