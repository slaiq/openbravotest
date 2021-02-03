package sa.elm.ob.utility.util;

import java.math.BigDecimal;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * @auther Qualian
 */
public class UtilityFn {
	private static final Logger log4j = Logger.getLogger(UtilityFn.class);

	/**
	 * Check AD message with Code
	 * 
	 * @param conn
	 * @param value
	 * @param lang
	 * @return String
	 */
	public static String parseADMessage(String errorCode, String language) {
		String parsedMessage = "";
		String errorMessage = errorCode;
		if(errorMessage == null || "".equals(errorMessage))
			return "";
		int index = errorMessage.indexOf("@ERROR=");
		if(index != -1)
			errorMessage = errorMessage.substring(7);
		while ((index = errorMessage.indexOf("@")) != -1) {
			String tmpMessage = errorMessage.substring(index + 1);
			String tmpWord = errorMessage.substring(0, index);
			parsedMessage += tmpWord;
			index = tmpMessage.indexOf("@");
			if(index != -1) {
				tmpWord = tmpMessage.substring(0, index);
				tmpWord = UtilityDAO.getADMessage(tmpWord, language);
			}
			parsedMessage += tmpWord;
			errorMessage = tmpMessage.substring(index + 1);
		}
		parsedMessage += errorMessage;
		return parsedMessage;
	}

	/**
	 * Escape String for HTML
	 * 
	 * @param value
	 * @return escaped String
	 */
	public static String escapeHTML(String value) {
		try {
			if(value == null || "".equals(value.toString()))
				return "";
			return StringEscapeUtils.escapeHtml(value.trim()).replace("'", "&#039;");
		}
		catch (final Exception e) {
			log4j.error("Exception in escapeHTML : ", e);
			return value;
		}
	}

	/**
	 * UnEscape String for HTML grid
	 * 
	 * @param value
	 * @return escaped String
	 */
	public static String unescapeHTML(String value) {
		try {
			if(value == null || "".equals(value))
				return "";
			return StringEscapeUtils.unescapeHtml(value);
		}
		catch (final Exception e) {
			log4j.error("Exception in unescapeHTML : ", e);
			return "";
		}
	}

	/**
	 * Encode String for Treegrid
	 * 
	 * @param value
	 * @return encoded String
	 */
	public static String escapeTreeGridHTML(String value) {
		try {
			if(value == null || "".equals(value))
				return "";
			return convertStringtoASCII(value);
		}
		catch (final Exception e) {
			log4j.error("Exception in escapeTreeGridHTML : ", e);
			return "";
		}
	}

	/**
	 * Escape Quote
	 * 
	 * @param value
	 * @return encoded String
	 */
	public static String escapeQuote(Object value) {
		try {
			if(value == null || value.toString().equals(""))
				return "";
			else if(value instanceof String) {
				return value.toString().replace("'", "&#039;").replace("\"", "&quot;");
			}
			else if(value instanceof JSONObject) {
				return value.toString().replace("'", "&#039;").replace("\"", "&quot;");
			}
			else
				return value.toString();
		}
		catch (final Exception e) {
			log4j.error("Exception in escapeQuote : ", e);
			return "";
		}
	}

	/**
	 * Convert String into ASCII Values
	 * 
	 * @param value
	 * @return ASCII String
	 */
	public static String convertStringtoASCII(String value) {
		String val = "", bstr = "";
		char c = 'c';
		try {
			if(value == null || "".equals(value.toString()))
				return "";
			val = value.toString();
			for (int i = 0; i < val.length(); i++) {
				if(val.codePointAt(i) > 127)
					bstr += "&#" + val.codePointAt(i) + ";";
				else {
					c = val.charAt(i);
					if(c == '&')
						bstr += "&amp;";
					else if(c == '<')
						bstr += "&lt;";
					else if(c == '>')
						bstr += "&gt;";
					else if(c == '"')
						bstr += "&quot;";
					else if(c == '\'')
						bstr += "&#039;";
					else
						bstr += c;
				}
			}
		}
		catch (final Exception e) {
			log4j.error("Exception in convertStringtoASCII() Method : ", e);
			return val;
		}
		return bstr;
	}

	/**
	 * Encode String for Treegrid
	 * 
	 * Change Should be made in OBToolTip.js also
	 * 
	 * @param value
	 * @return encoded String
	 */
	public static String createTooltipEle(Object value, int len) {
		try {
			if(value == null || value.toString().equals(""))
				return "";
			else if(value instanceof String) {
				String val = value.toString();
				if(val.length() <= len)
					return "<span>" + value + "</span>";
				else {
					String html = "<span style='color: inherit; font-size: inherit;'>";
					html += val.substring(0, len);
					html += "<span class='OBToolTip'>...</span>";
					html += "<span class='OBToolTipTxt' style='display: none;'>" + val + "</span>";
					html += "</span>";
					return html;
				}
			}
			else
				return value.toString();
		}
		catch (final Exception e) {
			log4j.error("Exception in createTooltipEle : ", e);
			return "";
		}
	}

	/**
	 * Convert Number Format
	 * 
	 * @param vars
	 * @param type
	 * @param obj
	 * @return formatted string
	 */
	public static String getNumberFormat(VariablesSecureApp vars, String type, Object obj) {
		String s = "";
		try {
			if(obj == null)
				return "0";
			Object number = 0;
			if(obj instanceof String)
				number = new BigDecimal(obj.toString());
			else
				number = obj;
			s = type.equals(Utility.numberFormat_QuantityEdition) ? "qtyEdition" : type.equals(Utility.numberFormat_QuantityRelation) ? "qtyRelation" : type.equals(Utility.numberFormat_PriceEdition) ? "priceEdition" : type.equals(Utility.numberFormat_PriceRelation) ? "priceRelation" : type
					.equals(Utility.numberFormat_IntegerRelation) ? "integerRelation" : "integerEdition";
			s = org.openbravo.erpCommon.utility.Utility.getFormat(vars, s).format(number);
		}
		catch (final Exception e) {
			log4j.error("Exception in getNumberFormat() Method : ", e);
			return "0";
		}
		return s;
	}

	/**
	 * Throw EventHandler Exception
	 * 
	 * @param value
	 * @return throw OBException
	 */
	public static String throwEventHandlerException(String value) {
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		ConnectionProvider conn = new DalConnectionProvider(false);
		throw new OBException(org.openbravo.erpCommon.utility.Utility.messageBD(conn, value, language));
	}

	/**
	 * Escape String for Query
	 * 
	 * @param value
	 * @return escaped String
	 */
	public static String escapeQrySearchStr(Object value) {
		try {
			if(value == null || value.toString().equals(""))
				return "";
			else
				return value.toString().replace("'", "''").trim();
		}
		catch (final Exception e) {
			log4j.error("Exception in escapeQrySearchStr : ", e);
			return "";
		}
	}

	/**
	 * Null to Empty
	 * 
	 * @param value
	 * @return escaped String
	 */
	public static String nullToEmpty(Object value) {
		try {
			if(value == null || value.toString().equals(""))
				return "";
			else
				return value.toString().trim();
		}
		catch (final Exception e) {
			log4j.error("Exception in escapeQrySearchStr : ", e);
			return "";
		}
	}
	
	/**
	 * Encode String for Treegrid
	 * 
	 * @param value
	 * @return encoded String
	 */
	public static String nullToZero(Object value) {
		try {
			return value == null ? "0" : value.toString().trim();
		}
		catch (final Exception e) {
			log4j.error("Exception in nullToZero : ", e);
			return "";
		}
	}
}