package sa.elm.ob.finance.ad_process.addpenalty;

import java.math.BigDecimal;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINPenaltyType_V;
import sa.elm.ob.finance.EfinPenaltyTypes;
import sa.elm.ob.utility.EUTDeflookupsTypeLn;

/**
 * @author Kousalya on 06-04-2019
 */

public class AddPenaltyDAO {
  private static final Logger LOG = LoggerFactory.getLogger(AddPenaltyDAO.class);

  /**
   * get penalty type from look ups and add in penalty maintenance
   * 
   * @param clientId
   * @return list of penalty types
   */
  public static List<EUTDeflookupsTypeLn> getPenaltyLookup(String clientId) {
    StringBuffer whereClause = new StringBuffer();
    List<EUTDeflookupsTypeLn> penaltyLkpList = null;
    try {
      whereClause.append(
          " as typln left join typln.eUTDeflookupsType typ where typ.searchKey='PENALTY_TYPE' and typ.client.id= :clientId ");

      OBQuery<EUTDeflookupsTypeLn> lookup = OBDal.getInstance()
          .createQuery(EUTDeflookupsTypeLn.class, whereClause.toString());
      lookup.setNamedParameter("clientId", clientId);
      lookup.setFilterOnActive(true);
      penaltyLkpList = lookup.list();
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in getPenaltyLookup", e.getMessage());
      return null;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in getPenaltyLookup", e.getMessage());
      return null;
    }
    return penaltyLkpList;
  }

  public static boolean checkPenaltyExist(String penaltyCode, String clientId) {
    boolean penaltyExist = false;
    try {
      OBQuery<EfinPenaltyTypes> penaltyTyp = OBDal.getInstance().createQuery(EfinPenaltyTypes.class,
          "  as e where e.deductiontype.code=:penaltycode and e.client.id=:clientId ");
      penaltyTyp.setNamedParameter("penaltycode", penaltyCode);
      penaltyTyp.setNamedParameter("clientId", clientId);
      if (penaltyTyp.list().size() > 0) {
        penaltyExist = true;
      } else {
        penaltyExist = false;
      }
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in checkPenaltyExist", e.getMessage());
      return penaltyExist;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in checkPenaltyExist", e.getMessage());
      return penaltyExist;
    }
    return penaltyExist;
  }

  public static int insertPenaltyTypeMaintenance(String clientId, String orgId,
      EUTDeflookupsTypeLn penalty) {
    int insert = 0;
    try {
      EfinPenaltyTypes penalTyp = OBProvider.getInstance().get(EfinPenaltyTypes.class);
      penalTyp.setClient(OBDal.getInstance().get(Client.class, clientId));
      penalTyp.setOrganization(OBDal.getInstance().get(Organization.class, orgId));

      penalTyp.setDeductiontype(OBDal.getInstance().get(EFINPenaltyType_V.class, penalty.getId()));
      if (penalty.getPenaltyLogic().equals("ECA")) {
        penalTyp.setEnable(true);
        penalTyp.setEdituniqcode(false);
      } else if (penalty.getPenaltyLogic().equals("IGI")) {
        penalTyp.setEnable(true);
        penalTyp.setThreshold(new BigDecimal("1"));
        penalTyp.setPenaltyOverride(true);
        penalTyp.setEdituniqcode(true);
      } else if (penalty.getPenaltyLogic().equals("DP")) {
        penalTyp.setEnable(true);
        penalTyp.setThreshold(new BigDecimal("10"));
        penalTyp.setPenaltyOverride(true);
        penalTyp.setEdituniqcode(true);
      } else if (penalty.getPenaltyLogic().equals("ETC")) {
        penalTyp.setEnable(true);
        penalTyp.setEdituniqcode(true);
      } else {
        penalTyp.setEdituniqcode(true);
      }
      OBDal.getInstance().save(penalTyp);
      OBDal.getInstance().flush();
      insert = 1;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in insertPenaltyTypeMaintenance", e.getMessage());
      return 0;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.error("Exception in insertPenaltyTypeMaintenance", e.getMessage());
      return 0;
    }
    return insert;
  }
}