package sa.elm.ob.utility.properties;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

import sa.elm.ob.utility.util.Utility;

public class Resource {
  private static Resource resource = null;
  private static HashMap<String, Properties> hm = new HashMap<String, Properties>();
  private static Properties defaultProp = null;
  public static final String AP_INVOICE_RULE = "EUT_101";
  public static final String BUDGET_PREPARATION_RULE = "EUT_102";
  public static final String BUDGET_ENTRY_RULE = "EUT_103";
  public static final String BUDGET_REVISION_RULE = "EUT_104";
  public static final String MANUAL_ENCUMBRANCE_RULE = "EUT_105";
  public static final String GLJOURNAL_RULE = "EUT_106";
  public static final String PAYMENT_OUT_RULE = "EUT_107";
  public static final String PURCHASE_ORDER_RULE = "EUT_108";
  public static final String AP_Prepayment_App_RULE = "EUT_109";
  public static final String AP_Prepayment_Inv_RULE = "EUT_110";
  public static final String PURCHASE_REQUISITION = "EUT_111";
  public static final String MATERIAL_ISSUE_REQUEST = "EUT_112";
  public static final String Return_Transaction = "EUT_113";
  public static final String CUSTODY_TRANSFER = "EUT_114";
  public static final String MATERIAL_ISSUE_REQUEST_IT = "EUT_115";
  public static final String Bid_Management = "EUT_116";
  public static final String PROPOSAL_MANAGEMENT = "EUT_117";
  public static final String PURCHASE_REQUISITION_LIMITED = "EUT_118";
  public static final String BUDGET_ADJUSTMENT_RULE = "EUT_119";
  public static final String BCU_BUDGET_DISTRIBUTION = "EUT_120";
  public static final String ORG_BUDGET_DISTRIBUTION = "EUT_121";
  public static final String PROPOSAL_MANAGEMENT_DIRECT = "EUT_122";
  public static final String TECHNICAL_EVALUATION_EVENT = "EUT_123";
  public static final String RDV_Transaction = "EUT_124";
  public static final String PURCHASE_AGREEMENT_RULE = "EUT_125";
  public static final String RDV_BudgHoldDtl = "EUT_126";
  public static final String RDV_LAST_VERSION = "EUT_127";
  public static final String PROPERTY_COMPENSATION = "EUT_131";

  private static Logger log4j = Logger.getLogger(Resource.class);

  public static String getProperty(String propKey, String lang) {
    return getValue(propKey, lang, false);
  }

  public static String getProperty(String propKey, String lang, boolean escape) {
    return getValue(propKey, lang, escape);
  }

  public static String getDynamicProperty(String propKey, String lang, String args[]) {
    String value = getProperty(propKey, lang);
    if (args != null)
      for (int i = 0; i < args.length; i++) {
        value = value.replace("${" + i + "}", args[i]);
      }

    return value;
  }

  private static String getValue(String propKey, String lang, boolean escape) {
    String val = null;
    if (resource == null) {
      resource = new Resource();
      try {
        log4j.debug("inside:" + lang.toLowerCase());
        defaultProp = new Properties();
        InputStream input = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("sa/elm/ob/utility/properties/applicationresources.properties");
        defaultProp.load(new InputStreamReader(input, "UTF-8"));
        hm.put("en_us", defaultProp);
      } catch (Exception e) {
        log4j.error("Exception in Resource", e);
      }
    }
    log4j.debug("language:" + lang.toLowerCase());
    Properties prop = hm.get(lang.toLowerCase());
    if (prop != null) {
      val = prop.getProperty(propKey);
      return val == null ? "" : (escape ? Utility.escapeHTML(val) : val);
    } else {
      try {
        Properties props = new Properties();
        InputStream input = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("sa/elm/ob/utility/properties/applicationresources_"
                + lang.toLowerCase() + ".properties");
        props.load(new InputStreamReader(input, "UTF-8"));
        log4j.debug("language else:" + lang.toLowerCase());
        hm.put(lang.toLowerCase(), props);
        val = props.getProperty(propKey);
        return val == null ? "" : (escape ? Utility.escapeHTML(val) : val);
      } catch (Exception e) {
        log4j.error("Exception in Resource", e);
        val = defaultProp.getProperty(propKey);
        return val == null ? "" : (escape ? Utility.escapeHTML(val) : val);
      }
    }
  }
}