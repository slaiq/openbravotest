package sa.elm.ob.scm.ad_process.AddAllContracts;

import java.util.List;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.Organization;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.utility.EutLookupAccess;
import sa.elm.ob.utility.util.Constants;

public class AddAllContractsDAO {

  /**
   * Add all contracts in Role - Lookup Access which are not already added
   * 
   * @param roleId
   * @param userId
   * @return
   */
  public static boolean addAllContracts(String roleId, String userId, String orgId,
      String clientId) {

    try {
      OBContext.setAdminMode();

      OBQuery<ESCMDefLookupsTypeLn> contractListQry = OBDal.getInstance().createQuery(
          ESCMDefLookupsTypeLn.class,
          " as e where e.escmDeflookupsType.id in (select hd.id from ESCM_DefLookups_Type hd "
              + " where hd.reference = 'POCONCATG') and e.id not in (select lk.escmDeflookupsTypeln.id from Eut_Lookup_Access lk "
              + " where lk.role.id = :roleId)");
      contractListQry.setNamedParameter("roleId", roleId);

      List<ESCMDefLookupsTypeLn> contractList = contractListQry.list();
      if (contractList.size() > 0) {
        for (ESCMDefLookupsTypeLn contract : contractList) {
          // Insert contract categories in Lookup Access
          EutLookupAccess lookupAccess = OBProvider.getInstance().get(EutLookupAccess.class);

          User user = OBDal.getInstance().get(User.class, userId);
          Role role = OBDal.getInstance().get(Role.class, roleId);
          Organization org = OBDal.getInstance().get(Organization.class, orgId);
          Client client = OBDal.getInstance().get(Client.class, clientId);
          Window window = OBDal.getInstance().get(Window.class,
              Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W);

          lookupAccess.setClient(client);
          lookupAccess.setOrganization(org);
          lookupAccess.setCreationDate(new java.util.Date());
          lookupAccess.setCreatedBy(user);
          lookupAccess.setUpdated(new java.util.Date());
          lookupAccess.setUpdatedBy(user);
          lookupAccess.setRole(role);
          lookupAccess.setEscmDeflookupsTypeln(contract);
          lookupAccess.setEscmDeflookupsType(contract.getEscmDeflookupsType());
          // lookupAccess.setWindow(window);
          OBDal.getInstance().save(lookupAccess);
          OBDal.getInstance().flush();
        }
      }

      return true;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
