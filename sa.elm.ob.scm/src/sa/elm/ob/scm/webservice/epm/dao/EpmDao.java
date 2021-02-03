package sa.elm.ob.scm.webservice.epm.dao;

import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMBGWorkbench;

public class EpmDao {

	public static Attachment findAttachment(String path, String fileName, String tableId) {
		Attachment attachment = null;
		OBQuery<Attachment> obQry = OBDal.getInstance().createQuery(Attachment.class,
				"as e where e.table.id=:tableId and e.name = :fileName and e.path=:path");

		obQry.setNamedParameter("path", path);
		obQry.setNamedParameter("fileName", fileName);
		obQry.setNamedParameter("tableId", tableId);
		obQry.setFilterOnReadableClients(false);
		obQry.setFilterOnReadableOrganization(false);

		if (obQry.list().size() > 0) {
			attachment = obQry.list().get(0);
		}
		return attachment;
	}

	private final static Logger log = LoggerFactory.getLogger(EpmDao.class);

	/**
	 * Method to insert the Order lines based on selection
	 * 
	 * @param orderId
	 * @return
	 */
	public static String getBgworkbenchd(String orderId) {
		String bgWorkbenchId = null;
		try {
			OBContext.setAdminMode();
			OBQuery<ESCMBGWorkbench> bg = OBDal.getInstance().createQuery(ESCMBGWorkbench.class,
					" as e where e.documentNo.id=:orderID ");
			bg.setNamedParameter("orderID", orderId);
			bg.setMaxResult(1);
			if (bg.list().size() > 0) {
				bgWorkbenchId = bg.list().get(0).getId();
				return bgWorkbenchId;
			}
			OBDal.getInstance().flush();
		} catch (Exception e) {
			log.error("Exception in getBgworkbenchd in POContractSummaryDAO: ", e);
			OBDal.getInstance().rollbackAndClose();
			return bgWorkbenchId;
		} finally {
			OBContext.restorePreviousMode();
		}
		return bgWorkbenchId;
	}

	public static boolean checkAgreementRelease(Order objOrder) {
		List<Order> orderList = null;
		try {
			OBContext.setAdminMode();
			OBQuery<Order> order = OBDal.getInstance().createQuery(Order.class,
					" as e where e.escmPurchaseagreement.id=:orderId and (e.escmAppstatus='ESCM_IP' or e.escmAppstatus='DR')");
			order.setNamedParameter("orderId", objOrder.getId());
			orderList = order.list();
			if (orderList != null && orderList.size() > 0) {
				return true;
			}
		} catch (OBException e) {
			log.error("Exception while checkAgreementRelease:" + e);
			throw new OBException(e.getMessage());
		} finally {
			OBContext.restorePreviousMode();
		}
		return false;
	}
}