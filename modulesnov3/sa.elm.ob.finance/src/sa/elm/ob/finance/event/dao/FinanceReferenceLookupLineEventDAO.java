package sa.elm.ob.finance.event.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;

import sa.elm.ob.finance.EfinLookupLine;

public class FinanceReferenceLookupLineEventDAO {
  private static Logger log4j = Logger.getLogger(FinanceReferenceLookupLineEventDAO.class);

  /**
   * 
   * @param lookUpTypeId
   * @param clientId
   * @return true if already exists
   */
  public boolean checkDefaultAlreadyExist(String lookUpTypeId, String clientId) {
    List<EfinLookupLine> ls = new ArrayList<EfinLookupLine>();
    try {
      OBQuery<EfinLookupLine> count = OBDal.getInstance().createQuery(EfinLookupLine.class,
          "as e where e. lookUp.id=:lookUpTypeId and e. escmDefault ='Y' and e.client.id =:clientId ");
      count.setNamedParameter("lookUpTypeId", lookUpTypeId);
      count.setNamedParameter("clientId", clientId);
      ls = count.list();
      if (ls.size() > 0) {
        return true;
      }

    } catch (OBException e) {
      log4j.error("error while check Default AlreadyExists", e);
      return false;
    }
    return false;
  }

}
