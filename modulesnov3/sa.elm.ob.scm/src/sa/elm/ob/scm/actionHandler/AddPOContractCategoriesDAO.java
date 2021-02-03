package sa.elm.ob.scm.actionHandler;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMProductContCatg;

/**
 * 
 * @author DivyaPrakash 11/03/2019
 *
 */

public class AddPOContractCategoriesDAO {
  private final Logger log = LoggerFactory.getLogger(AddPOContractCategoriesDAO.class);

  /**
   * Method to insert contract categories
   * 
   * @param inpProducttId
   * @param selectedlines
   * @return success 1 error 0
   */
  public int insertContractCategories(String inpProducttId, JSONArray selectedlines) {
    Product product = OBDal.getInstance().get(Product.class, inpProducttId);

    try {
      OBContext.setAdminMode();
      for (int i = 0; i < selectedlines.length(); i++) {
        ESCMProductContCatg cntrctCtgry = OBProvider.getInstance().get(ESCMProductContCatg.class);
        JSONObject selectedRow = selectedlines.getJSONObject(i);
        cntrctCtgry.setContractCategory(
            OBDal.getInstance().get(ESCMDefLookupsTypeLn.class, selectedRow.getString("id")));
        cntrctCtgry.setReceiveType(OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            selectedRow.getString("receiveType")));
        cntrctCtgry.setActive(true);
        cntrctCtgry.setProduct(product);
        cntrctCtgry.setOrganization(product.getOrganization());
        cntrctCtgry.setClient(OBContext.getOBContext().getCurrentClient());
        cntrctCtgry.setCreatedBy(OBContext.getOBContext().getUser());
        cntrctCtgry.setUpdatedBy(OBContext.getOBContext().getUser());
        OBDal.getInstance().save(cntrctCtgry);
        OBDal.getInstance().flush();
      }

      return 1;
    } catch (Exception e) {
      log.error("Exception in AddPOContractCategoriesDAO :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
