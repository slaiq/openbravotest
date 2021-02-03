package sa.elm.ob.finance.util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;

public class FinanceUtils {

	private static final Logger log4j = Logger.getLogger(FinanceUtils.class);
	@SuppressWarnings("unused")
	private Connection connection = null;

	public FinanceUtils(Connection connection) {
		this.connection = connection;
	}

	@SuppressWarnings("unchecked")
	public static Currency getCurrency(String orgId, Invoice invoice) {
		Organization organization = null;
		Currency currency = null;

		try {

			organization = OBDal.getInstance().get(Organization.class, orgId);

			if (organization.getGeneralLedger() != null) {
				currency = organization.getGeneralLedger().getCurrency();
				return currency;

			} else {

				// get parent organization list
				String[] orgIds = null;
				SQLQuery query = OBDal.getInstance().getSession().createSQLQuery("select eut_parent_org ('"
						+ invoice.getOrganization().getId() + "','" + invoice.getClient().getId() + "')");
				List<String> list = query.list();

				orgIds = list.get(0).split(",");

				for (int i = 0; i < orgIds.length; i++) {
					organization = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));

					if (organization.getGeneralLedger() != null) {
						currency = organization.getGeneralLedger().getCurrency();
						if (currency.getId() != null)
							return currency;
					}
				}
			}
		} catch (Exception e) {
			log4j.error("Exception in insertBudgetApprover: ", e);
			OBDal.getInstance().rollbackAndClose();
			return currency;
		}
		return currency;
	}

	public static BigDecimal getConversionRate(Connection conn, String orgId, Invoice invoice, Currency currency) {
		String sql = null, sql1 = null;
		PreparedStatement ps = null, ps1 = null;
		ResultSet rs = null, rs1 = null;
		BigDecimal conversionrate = BigDecimal.ONE;

		try {
			OBContext.setAdminMode(true);

			// get conversion rate
			sql = " SELECT rate as rate  FROM C_Conversion_Rate_Document  WHERE C_Invoice_ID ='" + invoice.getId()
					+ "'   AND C_Currency_ID ='" + invoice.getCurrency().getId() + "'  AND C_Currency_Id_To ='"
					+ currency.getId() + "'";
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();

			if (rs.next()) {

				conversionrate = rs.getBigDecimal("rate");
				return conversionrate;
			
			} else {
			
				sql1 = " SELECT multiplyrate as rate  FROM C_Conversion_Rate   WHERE C_Currency_ID ='"
						+ invoice.getCurrency().getId() + "'    AND C_Currency_ID_To ='" + currency.getId()
						+ "'    AND ConversionRateType = 'S'    AND ('" + invoice.getInvoiceDate()
						+ "') BETWEEN ValidFrom AND ValidTo    " + "     AND AD_Client_ID IN ('0', '"
						+ invoice.getClient().getId() + "')      AND AD_Org_ID IN ('0', '"
						+ invoice.getOrganization().getId()
						+ "')    AND IsActive = 'Y'    ORDER BY AD_Client_ID DESC,  AD_Org_ID DESC,ValidFrom DESC";
				ps1 = conn.prepareStatement(sql1);
				rs1 = ps1.executeQuery();

				if (rs1.next()) {
				
					conversionrate = rs1.getBigDecimal("rate");
					return conversionrate;
				}
			}
		} catch (Exception e) {
			log4j.error("Exception in insertBudgetApprover: ", e);
			OBDal.getInstance().rollbackAndClose();
			return conversionrate;
		} finally {
			//OBContext.restorePreviousMode();
		}
		return conversionrate;
	}

	public static BigDecimal getConvertedAmount(BigDecimal amount, BigDecimal conversionrate) {
		BigDecimal convertedAmount = BigDecimal.ZERO;
		
		try {
			convertedAmount = amount.multiply(conversionrate);
			
		} catch (Exception e) {
			log4j.error("Exception in getConvertedAmount: ", e);
			return convertedAmount;
		}
		
		return convertedAmount;
	}
}