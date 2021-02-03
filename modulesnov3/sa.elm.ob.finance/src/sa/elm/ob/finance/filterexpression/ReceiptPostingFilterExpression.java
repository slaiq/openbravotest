package sa.elm.ob.finance.filterexpression;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;

/**
 * 
 * @author Gopinagh.R
 * 
 */

public class ReceiptPostingFilterExpression implements FilterExpression {

	@Override
	public String getExpression(Map<String, String> requestMap) {
		String strContext = requestMap.get("context");
		String strCurrentParam = "";
		Logger log4j = Logger.getLogger(ReceiptPostingFilterExpression.class);

		try {
			JSONObject context = new JSONObject(strContext);

			//JSONObject context = new JSONObject(requestMap.get("context"));
			strCurrentParam = requestMap.get("currentParam");
			String strDateQuery = "select eut_convert_to_hijri(to_char(now(),'YYYY-MM-DD')) as DefaultValue";
			if(strCurrentParam.equals("Posting_Date")) {
				try {
					SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(strDateQuery);

					if(query != null && query.list().size() > 0) {
						return (String) query.list().get(0);
					}
				}
				catch (Exception e) {
					log4j.debug("error while getting Date default value:" + e.getMessage());
					e.printStackTrace();
				}
			}

			if(strCurrentParam.equalsIgnoreCase("FIN_Financial_Account_ID")) {
				if(context.has("inpfinFinancialAccountId") && context.get("inpfinFinancialAccountId") != JSONObject.NULL && StringUtils.isNotEmpty(context.getString("inpfinFinancialAccountId"))) {
					return context.getString("inpfinFinancialAccountId");
				}
				else if(context.has("Fin_Financial_Account_ID") && context.get("Fin_Financial_Account_ID") != JSONObject.NULL && StringUtils.isNotEmpty(context.getString("Fin_Financial_Account_ID"))) {
					return context.getString("Fin_Financial_Account_ID");
				}
				else if(context.has("inpemEfinFinFinacctId") && context.get("inpemEfinFinFinacctId") != JSONObject.NULL && StringUtils.isNotEmpty(context.getString("inpemEfinFinFinacctId"))) {
					return context.getString("inpemEfinFinFinacctId");
				}
			}
			
			if(strCurrentParam.equals("efin_cusname")){
				if(context.has("inpemEfinCusname") && context.get("inpemEfinCusname") != JSONObject.NULL && StringUtils.isNotEmpty(context.getString("inpemEfinCusname"))) {
					return context.getString("inpemEfinCusname");
				}
			}
			
			if(strCurrentParam.equals("efin_number")){
				if(context.has("inpemEfinCusno") && context.get("inpemEfinCusno") != JSONObject.NULL && StringUtils.isNotEmpty(context.getString("inpemEfinCusno"))) {
					return context.getString("inpemEfinCusno");
				}
			}
			
			if(strCurrentParam.equals("efin_location")){
				if(context.has("inpemEfinCuslocation") && context.get("inpemEfinCuslocation") != JSONObject.NULL && StringUtils.isNotEmpty(context.getString("inpemEfinCuslocation"))) {
					return context.getString("inpemEfinCuslocation");
				}
			}
		}
		catch (Exception e) {
			log4j.debug("Error getting the default value of Organization" + strCurrentParam + " " + e.getMessage());
			e.printStackTrace();
			return null;
		}
		finally {

		}
		return null;
	}
}