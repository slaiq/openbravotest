package sa.elm.ob.scm.ad_callouts.dao;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCM_Certificates;
import sa.elm.ob.scm.EscmBidMgmt;

/**
 * 
 * @author Priyanka Ranjan 25-07-2017
 * 
 */
// SalesVoucher callout file
public class SalesVoucherDAO {
  private static final Logger log = LoggerFactory.getLogger(SalesVoucherDAO.class);

  /**
   * 
   * @param inpbidsupplier
   * @return CommerialRegisteryNo
   */
  public static String getCommercialRegisteryNo(String inpbidsupplier) {
    log.debug("getCommercialRegisteryNo; Partner Id :  " + inpbidsupplier);

    final String query = "  as e where e.businessPartner.id = ? and e.certificateName.searchKey =? ";
    List<ESCM_Certificates> certificatesList = null;
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(inpbidsupplier);
    parametersList.add("CRN");

    try {
      OBContext.setAdminMode();

      OBQuery<ESCM_Certificates> escmCertificatesList = OBDal.getInstance()
          .createQuery(ESCM_Certificates.class, query, parametersList);
      certificatesList = escmCertificatesList.list();

      if (certificatesList.size() > 0) {
        ESCM_Certificates escm_Certificate = certificatesList.get(0);
        return escm_Certificate.getCertificateNumber();
      }
    } catch (OBException e) {
      log.error("Exception while getCommercialRegisteryNo:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  /**
   * Method to get Supplier Name
   * 
   * @param inpbidsupplier
   * @return SupplierName
   */
  public static String getSupplierName(String inpbidsupplier) {
    final String query = "  as e where e.id = ? ";
    List<BusinessPartner> suppList = null;
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(inpbidsupplier);

    try {
      OBContext.setAdminMode();
      OBQuery<BusinessPartner> escmSuppList = OBDal.getInstance().createQuery(BusinessPartner.class,
          query, parametersList);
      suppList = escmSuppList.list();

      if (suppList.size() > 0) {
        BusinessPartner suppName = suppList.get(0);
        return suppName.getName();
      }
    } catch (OBException e) {
      log.error("Exception while getSupplierName:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  /**
   * Method to get BidName and RFPPrice
   * 
   * @param inpbidId
   * @return result
   */
  public static JSONObject getBidNameAndRFPPrice(String inpbidId) {
    JSONObject result = new JSONObject();

    try {
      OBContext.setAdminMode();
      OBQuery<EscmBidMgmt> queryBidDts = OBDal.getInstance().createQuery(EscmBidMgmt.class,
          "as e where e.id=:bidID ");
      queryBidDts.setNamedParameter("bidID", inpbidId);

      if (queryBidDts.list().size() > 0) {
        EscmBidMgmt objBid = queryBidDts.list().get(0);
        result.put("bidname", objBid.getBidname());
        if (objBid.getRfpprice() != null)
          result.put("rfpprice", objBid.getRfpprice().toString());
        /*
         * else result.put("rfpprice", "0");
         */
      }
    } catch (OBException e) {
      log.error("OBException while getBidNameAndRFPPrice:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } catch (JSONException e) {
      log.error("JSONException while getBidNameAndRFPPrice:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /**
   * Method to get Location, Phone and Fax
   * 
   * @param inpbranch
   * @return result
   */
  public static JSONObject getLocationPhoneandFax(String inpbranch) {
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();
      if (inpbranch != null && !inpbranch.equals("")) {
        OBQuery<Location> queryBPLoc = OBDal.getInstance().createQuery(Location.class,
            "as e where e.id=:branchID ");
        queryBPLoc.setNamedParameter("branchID", inpbranch);

        if (queryBPLoc.list().size() > 0) {
          Location objLoc = queryBPLoc.list().get(0);
          result.put("c_location_id", objLoc.getLocationAddress().getId());
          result.put("phone", objLoc.getPhone());
          result.put("fax", objLoc.getFax());
        }
      }
    } catch (OBException e) {
      log.error("Exception while getLocationPhoneandFax:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } catch (JSONException e) {
      log.error("Exception while getLocationPhoneandFax:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /**
   * Method to get branch Name
   * 
   * @param inpbidsupplier
   * @return BranchName
   */
  public static String getBranchName(String inpbidsupplier) {
    String BranchName = "";
    try {
      OBContext.setAdminMode();
      if (inpbidsupplier != null && !inpbidsupplier.equals("")) {
        OBQuery<Location> queryBPLoc = OBDal.getInstance().createQuery(Location.class,
            "as e where e.businessPartner.id=:bpartnerID order by created asc ");
        queryBPLoc.setNamedParameter("bpartnerID", inpbidsupplier);
        if (queryBPLoc.list().size() > 0) {
          Location objLoc = queryBPLoc.list().get(0);
          BranchName = objLoc.getId();
        }
      }
    } catch (OBException e) {
      log.error("Exception while getBranchName:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return BranchName;
  }

  /**
   * Get gregorian date to set rfp document date in gregorian
   * 
   * @param inpDocumentDate
   * @return inpDocumentDateGreg
   */
  @SuppressWarnings("unchecked")
  public static String getGregorianDate(String inpDocumentDate) {
    String inpDocumentDateGreg = "", strQuery = "";
    Query query = null;
    try {
      OBContext.setAdminMode();
      strQuery = "select to_char(eut_convertto_gregorian(?)) as eut_convertto_gregorian ";

      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter(0, inpDocumentDate);
      List<Object> queryList = query.list();
      if (queryList.size() > 0) {
        Object row = queryList.get(0);
        log.info("inpDocumentDateGreg>" + (String) row);
        inpDocumentDateGreg = (String) row;
      }
    } catch (OBException e) {
      log.error("Exception while getGregorianDate:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return inpDocumentDateGreg;
  }
}
