package sa.elm.ob.finance.ad_process.Sadad;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.db.DalConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EFINSadadbilConfig;
import sa.elm.ob.finance.EFINServiceItemConfiguration;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinLookupLine;
import sa.elm.ob.finance.ad_callouts.BudgetAdjustmentCallout;
import sa.elm.ob.scm.ESCM_Certificates;
import sa.elm.ob.utility.sadad.producer.ObjectFactory;
import sa.elm.ob.utility.sadad.producer.SadadBillResponse;

public class BillCreationInGrpDao {

  private static final Logger log = LoggerFactory.getLogger(BillCreationInGrpDao.class);

  /**
   * This Method is used to get the Client
   * 
   * @param clientId
   * @return Client
   */

  public static Client getClient(String clientId) {
    try {
      Client client = OBDal.getInstance().get(Client.class, clientId);
      if (client != null) {
        return client;
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting client" + e.getMessage());
    }
    return null;
  }

  /**
   * This method is to get User details from username
   * 
   * @param username,
   *          ClientId
   * @return User(ADUSER)
   */

  public static User getUser(String username, String clientId) {

    try {
      final User userQry = OBDal.getInstance().get(User.class, username);
      if (userQry != null) {
        return userQry;
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting user" + e.getMessage());
    }

    return null;

  }

  /**
   * This Method is used to sadad bill configuration setup object
   *
   * @param clientId
   * 
   * @return EFINSadadbilConfig object
   */

  public static EFINSadadbilConfig getSadadBillConfigObj(Client client) {

    try {
      String whereClause = "as e where e.client.id= :client ";
      final OBQuery<EFINSadadbilConfig> saddadQry = OBDal.getInstance()
          .createQuery(EFINSadadbilConfig.class, whereClause);
      saddadQry.setNamedParameter("client", client.getId());
      saddadQry.setFilterOnReadableClients(false);
      saddadQry.setFilterOnReadableOrganization(false);

      List<EFINSadadbilConfig> saddadQryList = saddadQry.list();

      if (saddadQryList != null && saddadQryList.size() > 0) {
        return saddadQryList.get(0);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting sadadbill config" + e.getMessage());
    }

    return null;
  }

  /**
   * This method is to get businesspartner details
   * 
   * @param idno,
   *          clientId
   * @return Bpartner(c_bpartner_id)
   */

  public static org.openbravo.model.common.businesspartner.BusinessPartner getBpartner(String idno,
      String clientId) {

    try {
      String whereClause = "as e where e.certificateNumber= :idno and e.client.id=:client";
      final OBQuery<ESCM_Certificates> bpQry = OBDal.getInstance()
          .createQuery(ESCM_Certificates.class, whereClause);
      bpQry.setNamedParameter("idno", idno);
      bpQry.setNamedParameter("client", clientId);
      bpQry.setFilterOnReadableClients(false);
      bpQry.setFilterOnReadableOrganization(false);
      bpQry.setFilterOnActive(false);

      List<ESCM_Certificates> bpQryList = bpQry.list();
      if (bpQryList != null && bpQryList.size() > 0) {
        return bpQryList.get(0).getBusinessPartner();
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting businessaprtner" + e.getMessage());
    }
    return null;
  }

  /**
   * This method is to get budget defintion object
   * 
   * @param clientId
   * @return EfinBudgetIntialization object
   */

  public static EfinBudgetIntialization getBudgetDefintion(String clientId) {
    try {
      String strBudgetInitializationId = BudgetAdjustmentCallout
          .getBudgetDefinitionForStartDate(new Date(), clientId, "");
      EfinBudgetIntialization budgetInt = OBDal.getInstance().get(EfinBudgetIntialization.class,
          strBudgetInitializationId);
      if (budgetInt != null) {
        return budgetInt;
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting budget defintion" + e.getMessage());
    }
    return null;

  }

  /**
   * This method is to lookup id for input value
   * 
   * @param value,clientId
   * @return EfinLookupLine object
   */

  public static EfinLookupLine getReferenceLookup(String type, String value, String clientId) {
    try {
      String whereClause = "as line join line.lookUp as header where header.reference =:type and line.searchKey=:value and line.client.id=:clientId";
      final OBQuery<EfinLookupLine> lookupQry = OBDal.getInstance()
          .createQuery(EfinLookupLine.class, whereClause);
      lookupQry.setNamedParameter("type", type);
      lookupQry.setNamedParameter("value", value);
      lookupQry.setNamedParameter("clientId", clientId);
      lookupQry.setFilterOnReadableClients(false);
      lookupQry.setFilterOnReadableOrganization(false);
      lookupQry.setFilterOnActive(false);

      List<EfinLookupLine> lookUpList = lookupQry.list();
      if (lookUpList != null && lookUpList.size() > 0) {
        return lookUpList.get(0);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting reference lookup" + e.getMessage());
    }
    return null;

  }

  /**
   * This method is to get main account details
   * 
   * @param value,
   *          clientId
   * @return main account(c_elementvalue)
   */

  public static ElementValue getElementValue(String value, String clientId) {
    try {
      String whereClause = " as e where e.searchKey=:value and e.client.id=:clientId";
      final OBQuery<ElementValue> mainAccQry = OBDal.getInstance().createQuery(ElementValue.class,
          whereClause);

      mainAccQry.setNamedParameter("value", value);
      mainAccQry.setNamedParameter("clientId", clientId);
      mainAccQry.setFilterOnReadableClients(false);
      mainAccQry.setFilterOnReadableOrganization(false);
      mainAccQry.setFilterOnActive(false);

      List<ElementValue> mainAccList = mainAccQry.list();
      if (mainAccList != null && mainAccList.size() > 0) {
        return mainAccList.get(0);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting main account" + e.getMessage());
    }

    return null;

  }

  /**
   * This method is to certificates based on customer idtype
   * 
   * @param value,
   *          clientid
   * @return ESCMDefLookupsTypeLn object
   */

  public static ESCM_Certificates getCustomerReferenceLookup(String value, String clientId) {

    try {
      String whereClause = " as line join line.certificateName as cr where cr.itemvalue=:value and cr.client.id=:clientId";
      final OBQuery<ESCM_Certificates> lookupQry = OBDal.getInstance()
          .createQuery(ESCM_Certificates.class, whereClause);
      lookupQry.setNamedParameter("value", Long.valueOf(value));
      lookupQry.setNamedParameter("clientId", clientId);
      lookupQry.setFilterOnReadableClients(false);
      lookupQry.setFilterOnReadableOrganization(false);
      lookupQry.setFilterOnActive(false);

      List<ESCM_Certificates> lookUpList = lookupQry.list();
      if (lookUpList != null && lookUpList.size() > 0) {
        return lookUpList.get(0);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting customerreference lookup" + e.getMessage());
    }
    return null;

  }

  /**
   * This method is to get currency object
   * 
   * @param clientid
   * @return Currency object
   */

  public static Currency getCurrency(String clientId) {
    try {
      Currency currency = OBDal.getInstance().get(Currency.class, "317");
      if (currency != null) {
        return currency;
      } else {
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting currency" + e.getMessage());
    }
    return null;

  }

  /**
   * This method is used to complete the invoice
   * 
   * @param invoice
   * 
   * @return String (Error msg if any)
   */

  public static String completeInvoice(Invoice invoice) {

    try {
      ConnectionProvider conn = new DalConnectionProvider();
      PreparedStatement ps = null;
      ResultSet rs = null;

      String p_instance_id = SequenceIdData.getUUID();
      String error = "", s = "";
      int count = 0;

      log.debug("p_instance_id:" + p_instance_id);
      String sql = " INSERT INTO ad_pinstance (ad_pinstance_id, ad_process_id, record_id, isactive, ad_user_id, ad_client_id, ad_org_id, created, createdby, updated, updatedby,isprocessing)  "
          + "  VALUES ('" + p_instance_id + "', '111','" + invoice.getId() + "', 'Y','"
          + invoice.getCreatedBy().getId() + "','" + invoice.getClient().getId() + "','"
          + invoice.getOrganization().getId() + "', now(),'" + invoice.getCreatedBy().getId()
          + "', now(),'" + invoice.getCreatedBy().getId() + "','Y')";
      ps = conn.getPreparedStatement(sql);
      log.debug("ps:" + ps.toString());
      count = ps.executeUpdate();
      log.debug("count:" + count);

      String instanceqry = "select ad_pinstance_id from ad_pinstance where ad_pinstance_id=?";
      PreparedStatement pr = conn.getPreparedStatement(instanceqry);
      pr.setString(1, p_instance_id);
      ResultSet set = pr.executeQuery();

      if (set.next()) {
        sql = " select * from  c_invoice_post0(?)";
        ps = conn.getPreparedStatement(sql);
        ps.setString(1, p_instance_id);
        ps.executeQuery();

        log.debug("count12:" + set.getString("ad_pinstance_id"));
        sql = " select result, errormsg from ad_pinstance where ad_pinstance_id='" + p_instance_id
            + "'";
        ps = conn.getPreparedStatement(sql);
        log.debug("ps12:" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          log.debug("result:" + rs.getString("result"));

          if (rs.getString("result").equals("0")) {
            error = rs.getString("errormsg").replace("@ERROR=", "");
            log.debug("error:" + error);
            s = error;
            int start = s.indexOf("@");
            int end = s.lastIndexOf("@");

            if (log.isDebugEnabled()) {
              log.debug("start:" + start);
              log.debug("end:" + end);
            }

            if (end != 0) {
              sql = " select  msgtext from ad_message where value ='" + s.substring(start + 1, end)
                  + "'";
              ps = conn.getPreparedStatement(sql);
              log.debug("ps12:" + ps.toString());
              rs = ps.executeQuery();
              if (rs.next()) {
                if (rs.getString("msgtext") != null)
                  throw new OBException(error);
              }
            }
          } else if (rs.getString("result").equals("1")) {

          }
        }
      }
    } catch (OBException e) {
      return e.getMessage();
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("erroe while completing invoice" + e);
      return "ERROR while completing the invoice";
    }
    return null;
  }

  /**
   * This method is used to create response based on error flag and msg
   * 
   * @param errorflag,
   *          errormsg, billnumber
   * 
   * @return response(SadadBillResponse)
   */

  public static SadadBillResponse createResponse(boolean errorflag, String errormsg,
      int billnumber) {
    ObjectFactory objectFactory = new ObjectFactory();
    SadadBillResponse response = new SadadBillResponse();
    response.setHasError(errorflag);
    response.setErrorMessage(
        objectFactory.createSadadBillResponseErrorMessage(OBMessageUtils.messageBD(errormsg)));
    if (errorflag) {
      response.setNewBillNumber(null);
    } else {
      response.setNewBillNumber(billnumber);
    }
    return response;
  }

  /**
   * This method is used to get service item mapping based on main account, dept auth, dept benefit,
   * service item value, application type
   * 
   * @param value
   * @param invoice
   * @param sadadBill(EFINSadadbilConfig)
   * @return EFINServiceItemConfiguration object
   */
  public static EfinLookupLine getServiceItemConfig(int value, Invoice invoice,
      EFINSadadbilConfig sadadBill) {
    try {
      StringBuilder whereClause = new StringBuilder();
      if (sadadBill.isServiceItemSecurity()) {
        whereClause.append(
            " as e join e.serviceItem service where service.searchKey =:value and e.deptAuthCode=:depauth and e.deptBenefitCode=:deptbenefit and e.applicationType=:app and e.account=:account and e.client=:client");

        final OBQuery<EFINServiceItemConfiguration> lookupQry = OBDal.getInstance()
            .createQuery(EFINServiceItemConfiguration.class, whereClause.toString());
        if (sadadBill.isServiceItemSecurity()) {
          lookupQry.setNamedParameter("value", String.valueOf(value));
          lookupQry.setNamedParameter("client", sadadBill.getClient());
          lookupQry.setNamedParameter("deptbenefit", sadadBill.getDeptbenefitcode());
          lookupQry.setNamedParameter("depauth", sadadBill.getDeptauthcode());
          lookupQry.setNamedParameter("app", invoice.getEfinApplicationtype());
          lookupQry.setNamedParameter("account", invoice.getEfinElementvalue());
        } else {
          lookupQry.setNamedParameter("client", sadadBill.getClient());
          lookupQry.setNamedParameter("value", String.valueOf(value));
        }
        lookupQry.setFilterOnReadableClients(false);
        lookupQry.setFilterOnReadableOrganization(false);
        lookupQry.setFilterOnActive(false);

        List<EFINServiceItemConfiguration> serviceItemList = lookupQry.list();
        if (serviceItemList != null && serviceItemList.size() > 0) {
          return serviceItemList.get(0).getServiceItem();
        } else {
          return null;
        }
      } else {
        return getReferenceLookup("SITEM", String.valueOf(value), sadadBill.getClient().getId());
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("erroe while serviceitem " + e);
    }
    return null;

  }

  /**
   * This method is used to get unique code based on mainaccount,department
   * 
   * @param value
   * @param invoice
   * @param sadadBill(EFINSadadbilConfig)
   * @return EFINServiceItemConfiguration object
   */
  public static AccountingCombination getUniqueCodeObj(Invoice invoice,
      EFINSadadbilConfig sadadBill) {
    try {
      String whereClause = "as e where organization=:org and salesRegion=:dept and account=:account and efinDimensiontype='A' and client=:client";
      final OBQuery<AccountingCombination> uniqueCodeQry = OBDal.getInstance()
          .createQuery(AccountingCombination.class, whereClause.toString());
      uniqueCodeQry.setNamedParameter("org", sadadBill.getOrganization());
      uniqueCodeQry.setNamedParameter("account", invoice.getEfinElementvalue());
      uniqueCodeQry.setNamedParameter("dept", sadadBill.getDepartment());
      uniqueCodeQry.setNamedParameter("client", sadadBill.getClient());
      uniqueCodeQry.setFilterOnReadableClients(false);
      uniqueCodeQry.setFilterOnReadableOrganization(false);
      uniqueCodeQry.setFilterOnActive(false);

      List<AccountingCombination> uniqueCodeQryList = uniqueCodeQry.list();
      if (uniqueCodeQryList != null && uniqueCodeQryList.size() > 0) {
        return uniqueCodeQryList.get(0);
      } else {
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("erroe while getting uniqecode" + e);
      return null;
    }
  }

  /**
   * This method is to get GLitem
   * 
   * @param clientId
   * @return GLItem(c_GLItem_id)
   */

  public static GLItem getGLItem(String clientId) {

    try {
      String whereClause = "as e where e.client.id=:client";
      final OBQuery<GLItem> glQry = OBDal.getInstance().createQuery(GLItem.class, whereClause);
      glQry.setNamedParameter("client", clientId);
      glQry.setFilterOnReadableClients(false);
      glQry.setFilterOnReadableOrganization(false);
      glQry.setFilterOnActive(false);

      List<GLItem> glQryList = glQry.list();
      if (glQryList != null && glQryList.size() > 0) {
        return glQryList.get(0);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting businessaprtner" + e.getMessage());
    }
    return null;
  }

  /**
   * This method is to get Sales pricelist
   * 
   * @param clientId
   * @return Pricelist(m_pricelist_id)
   */

  public static PriceList getPriceList(String clientId) {

    try {
      String whereClause = "as e where e.client.id=:client and e.salesPriceList='Y'";
      final OBQuery<PriceList> priceQry = OBDal.getInstance().createQuery(PriceList.class,
          whereClause);
      priceQry.setNamedParameter("client", clientId);
      priceQry.setFilterOnReadableClients(false);
      priceQry.setFilterOnReadableOrganization(false);
      priceQry.setFilterOnActive(false);

      List<PriceList> priceQryList = priceQry.list();
      if (priceQryList != null && priceQryList.size() > 0) {
        return priceQryList.get(0);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("Error while getting businessaprtner" + e.getMessage());
    }
    return null;
  }

}
