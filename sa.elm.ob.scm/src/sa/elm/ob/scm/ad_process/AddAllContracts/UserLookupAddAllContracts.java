package sa.elm.ob.scm.ad_process.AddAllContracts;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.utility.EutUserLookupAccess;

/**
 * @author Priyanka on 14/10/2020
 */

public class UserLookupAddAllContracts implements Process {

  /**
   * This class is used to add contracts in User - Lookup Access which are not added already
   */
  private static Logger log = Logger.getLogger(UserLookupAddAllContracts.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    try {
      OBContext.setAdminMode();

      // Variable declaration
      final String userId = bundle.getParams().get("AD_User_ID").toString();
      final String roleId = bundle.getContext().getRole();
      final String orgId = bundle.getContext().getOrganization();
      final String clientId = bundle.getContext().getClient();

      OBQuery<ESCMDefLookupsTypeLn> contractListQry = OBDal.getInstance().createQuery(
          ESCMDefLookupsTypeLn.class,
          " as e where e.escmDeflookupsType.id in (select hd.id from ESCM_DefLookups_Type hd "
              + " where hd.reference = 'POCONCATG') and e.id not in (select lk.lookupValues.id from Eut_User_Lookup_Access lk "
              + " where lk.userContact.id = :userId)");
      contractListQry.setNamedParameter("userId", userId);

      List<ESCMDefLookupsTypeLn> contractList = contractListQry.list();
      if (contractList.size() > 0) {
        for (ESCMDefLookupsTypeLn contract : contractList) {
          // Insert contract categories in Lookup Access
          EutUserLookupAccess lookupAccess = OBProvider.getInstance()
              .get(EutUserLookupAccess.class);

          User user = OBDal.getInstance().get(User.class, userId);
          Role role = OBDal.getInstance().get(Role.class, roleId);
          Organization org = OBDal.getInstance().get(Organization.class, orgId);
          Client client = OBDal.getInstance().get(Client.class, clientId);

          lookupAccess.setClient(client);
          lookupAccess.setOrganization(org);
          lookupAccess.setCreationDate(new java.util.Date());
          lookupAccess.setCreatedBy(user);
          lookupAccess.setUpdated(new java.util.Date());
          lookupAccess.setUpdatedBy(user);
          lookupAccess.setUserContact(user);
          lookupAccess.setLookup(contract.getEscmDeflookupsType());
          lookupAccess.setLookupValues(contract);
          OBDal.getInstance().save(lookupAccess);
          OBDal.getInstance().flush();
        }
        String message = OBMessageUtils.messageBD("Escm_AddAllContractsSuccess");
        OBError result = OBErrorBuilder.buildMessage(null, "success", message);
        bundle.setResult(result);
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exeception in UserLookupAddAllContracts:", e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}