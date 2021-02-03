package sa.elm.ob.scm.event;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.utility.EutLookupAccess;

public class EutLookUpAccessEventDAO {
  private Logger log = Logger.getLogger(this.getClass());

  /**
   * 
   * @param lookUpAccess
   * @return true if look up access already exists
   */
  public Boolean checkListIsUnique(EutLookupAccess lookUpAccess) {
    try {
      OBQuery<EutLookupAccess> lookUpQuery = OBDal.getInstance().createQuery(EutLookupAccess.class,
          "escmDeflookupsType.id=:lookupID and escmDeflookupsTypeln.id=:lookUpLnID and role.id =:roleID");
      lookUpQuery.setNamedParameter("lookupID", lookUpAccess.getEscmDeflookupsType().getId());
      lookUpQuery.setNamedParameter("lookUpLnID", lookUpAccess.getEscmDeflookupsTypeln().getId());
      lookUpQuery.setNamedParameter("roleID", lookUpAccess.getRole().getId());
      if (lookUpQuery.list().size() > 0) {
        return true;
      }

    } catch (OBException e) {
      log.error("exception while creating listaccess", e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating listaccess", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

    return false;

  }

}
