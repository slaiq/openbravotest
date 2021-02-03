package sa.elm.ob.scm.ad_callouts.dao;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBankBranch;
import sa.elm.ob.scm.ESCMBGAmtRevision;
import sa.elm.ob.scm.ESCMBGConfiscation;
import sa.elm.ob.scm.ESCMBGExtension;
import sa.elm.ob.scm.ESCMBGRelease;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmBidTermCondition;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author qualian
 * 
 */
// BGWorkbenchDAO callout file
public class BGWorkbenchDAO {
  private static final Logger log = LoggerFactory.getLogger(BGWorkbenchDAO.class);

  /**
   * get Requested Expiry date based on Extend days with previous expiry dte/Extend expiry date
   * 
   * @param requestedDate
   * @param extdays
   * @param clientId
   * @return requestedExpiry Date
   */
  // get the requested expiry date based on ext period days
  @SuppressWarnings("unchecked")
  public static String getRequestedExpirydate(String requestedDate, String extdays, String clientId,
      String maxBGExtensionDate) {
    Query query = null;
    String[] dateParts = null;
    String strQuery = "", enddate = null, hijiridate = null;
    List<Object> querylist = null;
    try {
      OBContext.setAdminMode();
      if (maxBGExtensionDate == null) {
        dateParts = requestedDate.split("-");
        hijiridate = dateParts[2] + dateParts[1] + dateParts[0];
      } else {
        dateParts = maxBGExtensionDate.split("-");
        hijiridate = dateParts[2] + dateParts[1] + dateParts[0];
      }

      strQuery = "select hijri_date from (select max(hijri_date) as hijri_date from eut_hijri_dates where hijri_date > ? "
          + " group by hijri_date order by hijri_date limit ?) dual order by hijri_date desc limit 1";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      if (query != null) {
        query.setParameter(0, hijiridate);
        // query.setParameter(1, clientId);
        query.setInteger(1, Integer.parseInt(extdays));
        log.debug("getRequestedExpirydate:" + query.toString());
        querylist = query.list();
      }

      if (query != null && querylist.size() > 0) {
        Object row = querylist.get(0);
        enddate = (String) row;
        enddate = enddate.substring(6, 8) + "-" + enddate.substring(4, 6) + "-"
            + enddate.substring(0, 4);
        return enddate;
      } else
        return enddate;

    } catch (OBException e) {
      log.error("Exception while getRequestedExpirydate:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Calculate extend days based on Requested expiry date with expiry date / previous extend expiry
   * date
   * 
   * @param requestedDate
   * @param requestedExtDate
   * @param clientId
   * @return extend period days
   */
  // calculate the ext days
  @SuppressWarnings("unchecked")
  public static BigInteger getExtendPeriodDays(String requestedDate, String requestedExtDate,
      String clientId, String maxBGExtensionDate) {
    Query query = null;
    String strQuery = "";
    String[] reqDate = null;
    BigInteger perioddays = BigInteger.ZERO;
    List<Object> querylist = null;
    try {
      OBContext.setAdminMode();
      if (maxBGExtensionDate == null)
        reqDate = requestedDate.split("-");
      else
        reqDate = maxBGExtensionDate.split("-");
      String[] reqExtDate = requestedExtDate.split("-");
      String reqhijiri = reqDate[2] + reqDate[1] + reqDate[0];
      String reqExthiji = reqExtDate[2] + reqExtDate[1] + reqExtDate[0];
      strQuery = " select count(distinct hijri_date) as total from eut_hijri_dates  where hijri_date > ? and hijri_date <= ? ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      if (query != null) {
        query.setParameter(0, reqhijiri);
        query.setParameter(1, reqExthiji);
        log.debug("getExtendPeriodDays:" + query.toString());
        querylist = query.list();
      }
      if (query != null && querylist.size() > 0) {
        Object row = querylist.get(0);
        perioddays = (BigInteger) row;
        log.debug("perioddays:" + perioddays);
        return perioddays;
      } else
        return perioddays;
    } catch (OBException e) {
      log.error("Exception while getExtendPeriodDays:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * chk bg Amount with bid term and condition value
   * 
   * @param proposalattid
   * @param bidid
   * @return true if bg amount less than bid term and condition attribute value else false
   */
  public static BigDecimal chkWithbgAmtandTermValue(String proposalattid, String bidid,
      String ProposalId) {
    BigDecimal totalamt = BigDecimal.ZERO, discountamt = BigDecimal.ZERO;
    String attrvalue = "";
    EscmProposalMgmt pmmgmt = null;
    try {
      OBContext.setAdminMode();
      EscmProposalAttribute attribute = OBDal.getInstance().get(EscmProposalAttribute.class,
          proposalattid);
      if (attribute != null && attribute.getEscmProposalmgmt() != null) {
        OBQuery<EscmProposalMgmt> proposalmgmt = OBDal.getInstance().createQuery(
            EscmProposalMgmt.class, " id='" + attribute.getEscmProposalmgmt().getId() + "' ");
        if (proposalmgmt.list().size() > 0) {
          pmmgmt = proposalmgmt.list().get(0);
          totalamt = pmmgmt.getTotalamount();
        }
      }

      if (ProposalId != null && !ProposalId.equals("") && !ProposalId.equals("null")) {
        pmmgmt = OBDal.getInstance().get(EscmProposalMgmt.class, ProposalId);
        totalamt = pmmgmt.getTotalamount();

      }
      if (bidid != null) {
        OBQuery<EscmBidTermCondition> termcdn = OBDal.getInstance()
            .createQuery(EscmBidTermCondition.class, " escmBidmgmt.id='" + bidid + "' ");
        if (termcdn.list().size() > 0) {
          EscmBidTermCondition term = termcdn.list().get(0);
          attrvalue = term.getAttrvalue();
        }
      }

      if (totalamt != null && attrvalue != null) {
        discountamt = totalamt.subtract(totalamt.multiply(
            new BigDecimal(100).subtract(new BigDecimal(attrvalue)).divide(new BigDecimal(100))));
      }
      return discountamt;

    } catch (OBException e) {
      log.error("Exception while chkWithbgAmtandTermValue:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * 
   * @param strbidId
   * @param bgType
   * @return if bg type is Initial bg then return corresponding bid terms and conditions attribute
   *         value else bring the value from reference look up PRoce_Param value
   */
  // get bid term and conditions value
  public static String getbidTermsValue(String strbidId, String bgType, String clientId) {
    String attvalue = null;
    try {
      OBContext.setAdminMode();
      if (bgType.equals("IBG")) {
        EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class, strbidId);

        OBQuery<EscmBidTermCondition> bidtermsandcond = OBDal.getInstance().createQuery(
            EscmBidTermCondition.class,
            " as e where e.escmBidmgmt.id=:bidID"
                + " and e.client.id=:clientID and e.attributename.id in "
                + " ( select  e.id from ESCM_DefLookups_TypeLn e where e.escmDeflookupsType.id "
                + " in ( select hd.id from ESCM_DefLookups_Type hd where hd.reference='BTC') "
                + "  and e.searchKey='ING' ) ");
        bidtermsandcond.setNamedParameter("bidID", strbidId);
        bidtermsandcond.setNamedParameter("clientID", clientId);
        bidtermsandcond.setMaxResult(1);
        if (bidtermsandcond.list().size() > 0) {
          attvalue = bidtermsandcond.list().get(0).getAttrvalue();
          return attvalue;
        } else {
          if (bid != null && bid.getBidtype().equals("DR")) {
            attvalue = "1";
          }
        }
      } else if (bgType.equals("FBG")) {
        OBQuery<ESCMDefLookupsTypeLn> lookupln = OBDal.getInstance().createQuery(
            ESCMDefLookupsTypeLn.class,
            " as e where e.client.id=:clientID and  e.escmDeflookupsType.id in "
                + " ( select  hd.id from ESCM_DefLookups_Type hd where hd.reference='PRC_PARAM') ");
        lookupln.setNamedParameter("clientID", clientId);
        lookupln.setMaxResult(1);
        if (lookupln.list().size() > 0) {
          attvalue = lookupln.list().get(0).getCommercialName();
          return attvalue;
        }
      }

    } catch (OBException e) {
      log.error("Exception while updateExpireIn:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return attvalue;
  }

  /**
   * update the total amount
   * 
   * @param documentno
   * @param type
   * @return
   */
  public static JSONObject getDocDetails(String documentno, String type) {
    BigDecimal lineTotal = BigDecimal.ZERO;
    JSONObject result = new JSONObject();
    String SupplierId = null, bidId = null, currencyId = null, bidName = "";
    List<EscmProposalAttribute> proposalattlist = new ArrayList<EscmProposalAttribute>();
    try {
      OBContext.setAdminMode();
      if (type.equals("P")) {
        EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, documentno);
        if (proposal.getEscmBidmgmt() != null
            && !proposal.getEscmBidmgmt().getBidtype().equals("DR")) {
          OBQuery<EscmProposalAttribute> prosalattObj = OBDal.getInstance().createQuery(
              EscmProposalAttribute.class,
              " as e where e.escmOpenenvcommitee.id in  "
                  + "( select op.id from escm_openenvcommitee op where op.bidNo.id=:bidID ) "
                  + " and e.escmProposalmgmt.id=:proposalID");
          prosalattObj.setNamedParameter("bidID", proposal.getEscmBidmgmt().getId());
          prosalattObj.setNamedParameter("proposalID", proposal.getId());

          proposalattlist = prosalattObj.list();
          log.debug("proposalattlist:" + proposalattlist.size());
          if (proposalattlist.size() > 0) {
            if (proposalattlist.get(0).getNetPrice() != null)
              lineTotal = proposalattlist.get(0).getNetPrice();
            bidId = proposal.getEscmBidmgmt().getId();
            if (StringUtils.isNotEmpty(bidId)) {
              bidName = proposal.getEscmBidmgmt().getBidname();
            }

          }
        } else {
          if (proposal.getTotalamount() != null)
            lineTotal = proposal.getTotalamount();
          if (proposal.getEscmBidmgmt() != null)
            bidId = proposal.getEscmBidmgmt().getId();
          if (StringUtils.isNotEmpty(bidId)) {
            bidName = proposal.getEscmBidmgmt().getBidname();
          }
        }

        SupplierId = proposal.getSupplier().getId();
        if (proposal.getClient().getCurrency() != null)
          currencyId = proposal.getClient().getCurrency().getId();
      } else {
        Order order = OBDal.getInstance().get(Order.class, documentno);
        // if (order.getEscmProposalmgmt() != null
        // && order.getEscmProposalmgmt().getTotalamount() != null)
        // lineTotal = order.getEscmProposalmgmt().getTotalamount();
        lineTotal = order.getGrandTotalAmount();
        SupplierId = order.getBusinessPartner().getId();
        if (order.getEscmBidmgmt() != null) {
          bidId = order.getEscmBidmgmt().getId();
          bidName = order.getEscmBidmgmt().getBidname();
        }
        if (order.getClient().getCurrency() != null)
          currencyId = order.getClient().getCurrency().getId();
      }
      if (currencyId == null) {
        Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
        currencyId = objCurrency.getId();
      }
      result.put("linetotal", lineTotal);
      result.put("SupplierId", SupplierId);
      result.put("bidId", bidId);
      result.put("currencyId", currencyId);
      result.put("bidName", bidName);
      return result;
    } catch (OBException e) {
      log.error("Exception while get the Document Details:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } catch (JSONException je) {
      log.error("Exception while get the Document Details:" + je);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get the count of bg release or bg confisication based on bank guarantee details
   * 
   * @param bankGuaranteedetailId
   * @param Type
   *          ( Bg Release or BG Confication)
   * @return
   */
  public static int getCountofBgRelorBgConfi(String bankGuaranteedetailId, String Type) {
    int count = 0;
    try {
      OBContext.setAdminMode();
      if (Type.equals("REL")) {
        OBQuery<ESCMBGRelease> bgrellist = OBDal.getInstance().createQuery(ESCMBGRelease.class,
            " as e where e.escmBankguaranteeDetail.id=:bankGuaranteeID ");
        bgrellist.setNamedParameter("bankGuaranteeID", bankGuaranteedetailId);
        count = bgrellist.list().size();
      } else if (Type.equals("CON")) {
        OBQuery<ESCMBGConfiscation> confisList = OBDal.getInstance().createQuery(
            ESCMBGConfiscation.class, " as e where e.escmBankguaranteeDetail.id=:bankGuaranteeID ");
        confisList.setNamedParameter("bankGuaranteeID", bankGuaranteedetailId);
        count = confisList.list().size();
      }

      return count;
    } catch (OBException e) {
      log.error("Exception while getCountofBgRelorBgConfi:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get Bg Specialist Roel for logged user
   * 
   * @param userId
   * @param roleId
   * @param clientId
   * @param orgId
   * @return bg Specialist user bpartnerId
   */
  @SuppressWarnings("unchecked")
  public static String getBgSpeciallistRole(String userId, String roleId, String clientId,
      String orgId) {
    Query query = null;
    String strQuery = null, bpartnerId = null;
    List<Object> querylist = null;
    try {
      OBContext.setAdminMode();

      strQuery = "   select  (case when visibleat_role_id is not null  then ( select  C_BPartner_ID from ad_user  "
          + " where ad_user_id = ( select ad_user_id from ad_user_roles   where ad_role_id =pd.visibleat_role_id "
          + "  order by created asc limit 1 ) ) when ad_user_id is not null then ( select  usr.C_BPartner_ID from ad_user usr "
          + " where usr.ad_user_id= pd.ad_user_id )  end ) as DefaultValue    from ad_preference  pd where  pd.ispropertylist='Y' "
          + "  and pd.property='ESCM_BGSpecialist_Role'  ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      /*
       * query.setParameter(0, userId); query.setParameter(1, clientId); query.setParameter(2,
       * orgId); query.setParameter(3, roleId); query.setParameter(4, userId);
       */
      if (query != null) {
        log.debug("getBgSpeciallistRole:" + query.toString());
        querylist = query.list();
      }
      if (query != null && querylist.size() > 0) {
        Object row = querylist.get(0);
        bpartnerId = (String) row;
        return bpartnerId;
      } else
        return bpartnerId;
    } catch (OBException e) {
      log.error("Exception while getBgSpeciallistRole:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get Business partner for logged user
   * 
   * @param userId
   * @return user bpartnerId
   */
  @SuppressWarnings("unchecked")
  public static String getDefaultContactName(String userId) {

    String strQuery = null, bpartnerId = null;
    try {
      OBContext.setAdminMode();
      strQuery = "select bp.id from ADUser usr join usr.businessPartner as bp where usr.id = :userId";
      Query bpQry = OBDal.getInstance().getSession().createQuery(strQuery);
      bpQry.setParameter("userId", userId);
      if (bpQry != null) {
        List<Object> bpList = bpQry.list();
        if (bpList.size() > 0) {
          bpartnerId = (String) bpList.get(0);
        }
      }
    } catch (OBException e) {
      log.error("Exception while getDefaultContactName:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return bpartnerId;
  }

  /**
   * get Recent Revisied Bg Amount
   * 
   * @param bankguaranteeId
   * @return Recent Revisied Bg Amount
   */
  public static BigDecimal getRevisedAmt(String bankguaranteeId, String inpescmBgAmtrevisionId) {
    BigDecimal RecentRevAmt = BigDecimal.ZERO;
    List<ESCMBGAmtRevision> RecentRevAmtList = new ArrayList<ESCMBGAmtRevision>();
    try {
      OBContext.setAdminMode();

      OBQuery<ESCMBGAmtRevision> amtRevision = OBDal.getInstance().createQuery(
          ESCMBGAmtRevision.class,
          " as e where e.escmBankguaranteeDetail.id=:bgID  and e.id <>:amtRevID "
              + " order by creationDate desc ");
      amtRevision.setNamedParameter("bgID", bankguaranteeId);
      amtRevision.setNamedParameter("amtRevID", inpescmBgAmtrevisionId);

      amtRevision.setMaxResult(1);
      RecentRevAmtList = amtRevision.list();
      if (RecentRevAmtList.size() > 0) {
        RecentRevAmt = RecentRevAmtList.get(0).getNETBgamt();
        log.debug("RecentRevAmt:" + RecentRevAmt);
        return RecentRevAmt;
      } else
        return RecentRevAmt;

    } catch (OBException e) {
      log.error("Exception while getRevisedAmt:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get the next line no for bank guarantee based on Proposal/Order
   * 
   * @param documentId
   * @return
   */
  @SuppressWarnings("unchecked")
  public static BigDecimal getLineno(String documentId) {
    BigDecimal line = BigDecimal.ZERO;
    Query query = null;
    String strQuery = null;
    List<Object> querylist = null;
    try {
      OBContext.setAdminMode();
      strQuery = "  select COALESCE(MAX(LINE),0)+10 AS lineno FROM ESCM_BANKGUARANTEE_DETAIL WHERE document_no = ? ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      if (query != null) {
        query.setParameter(0, documentId);
        querylist = query.list();
      }

      if (query != null && querylist.size() > 0) {
        Object row = querylist.get(0);
        line = (BigDecimal) row;
        return line;
      }
      return line;

    } catch (OBException e) {
      log.error("Exception while getProposalAttribute:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get maiximum bg extension date
   * 
   * @param documentId
   * @param extensionId
   * @return
   */
  public static String getmaximumbgextensiondate(String bankguaranteeId, String extensionId) {
    String maxBGExtDate = null;
    List<ESCMBGExtension> bgExtensionList = new ArrayList<ESCMBGExtension>();
    try {
      OBContext.setAdminMode();

      OBQuery<ESCMBGExtension> bgExtension = OBDal.getInstance().createQuery(ESCMBGExtension.class,
          " as e where e.escmBankguaranteeDetail.id=:bgID and e.id <>:extensionID order by creationDate desc ");
      bgExtension.setNamedParameter("bgID", bankguaranteeId);
      bgExtension.setNamedParameter("extensionID", extensionId);
      bgExtension.setMaxResult(1);
      bgExtensionList = bgExtension.list();
      if (bgExtensionList.size() > 0) {
        maxBGExtDate = UtilityDAO
            .eventConvertTohijriDate(bgExtensionList.get(0).getReqexpiryDate().toString());

        log.debug("maxBGExtDate:" + maxBGExtDate);
        return maxBGExtDate;
      } else
        return maxBGExtDate;

    } catch (OBException e) {
      log.error("Exception while getmaximumbgextensiondate:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * chk already it revisied 100 % or not
   * 
   * @param bankguaranteeId
   * @return
   */
  @SuppressWarnings("unchecked")
  public static Boolean chkhundreadperrevornot(String bankguaranteeId) {
    BigDecimal revamount = BigDecimal.ZERO;
    List<Object> querylist = null;
    Query query = null;
    String strQuery = null;
    try {
      OBContext.setAdminMode();

      strQuery = "     select coalesce(bg.bgamount,0)-coalesce(sum(rev.reduced_amt),0)   from escm_bg_amtrevision rev ,escm_bankguarantee_detail bg"
          + " where  rev.letter_ref_date is not null and rev.escm_bankguarantee_detail_id= bg.escm_bankguarantee_detail_id  and rev.bank_letter_ref is not null "
          + " and  rev.escm_bankguarantee_detail_id = ?   group by bg.bgamount  ";
      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      if (query != null) {
        query.setParameter(0, bankguaranteeId);
        querylist = query.list();
      }

      if (query != null && querylist.size() > 0) {
        Object row = querylist.get(0);
        revamount = (BigDecimal) row;
        if (revamount.compareTo(BigDecimal.ZERO) == 0)
          return true;
      } else
        return false;

    } catch (OBException e) {
      log.error("Exception while chkhundreadperrevornot:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  /**
   * 
   * @param bgworkbenchId
   * @param chkstatusCo
   * @return
   */
  @SuppressWarnings("unchecked")
  public static Boolean chkTotBGAmtCalAmt(String bgworkbenchId, boolean chkstatusCo) {
    Query query = null;
    String strQuery = null;

    BigDecimal totalbgamount = BigDecimal.ZERO, totalcalamount = BigDecimal.ZERO;
    List<Object> querylist = null;
    try {
      OBContext.setAdminMode();
      strQuery = "  select sum(det.bgamount) ,case when cast(bg.initialbg as numeric) > 0 then "
          + " coalesce(round((bg.document_amount * ((cast(bg.initialbg as numeric))/100)),2),0)  "
          + " else 0 end  as calamt from escm_bankguarantee_detail det left join "
          + " escm_bgworkbench bg on bg.escm_bgworkbench_id=det.escm_bgworkbench_id "
          + "  where  det.escm_bgworkbench_id=:bgworkbenchID ";
      if (chkstatusCo)
        strQuery += "  and  (bg.bghdstatus ='CO') ";
      else
        strQuery += "  and  (bg.bghdstatus ='DR') ";
      strQuery += " group by bg.document_amount,bg.initialbg ";

      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter("bgworkbenchID", bgworkbenchId);
      if (query != null) {
        log.debug("query:" + query);
        // query.setParameter(0, proposalId);
        querylist = query.list();
      }

      if (query != null && querylist.size() > 0) {
        Object[] row = (Object[]) querylist.get(0);
        totalbgamount = (BigDecimal) row[0];
        totalcalamount = (BigDecimal) row[1];
        if ((totalcalamount.compareTo(totalbgamount) > 0)) {
          return true;
        } else
          return false;
      } else
        return true;

    } catch (OBException e) {
      log.error("Exception while checkBankPercentage:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * To get address from branch.
   * 
   * @param branchId
   * @return
   */
  public static String getBranchAddress(String branchId) {
    String address = "", address2 = "", street = "", country = "", city = "";
    try {
      OBContext.setAdminMode();
      EfinBankBranch branch = OBDal.getInstance().get(EfinBankBranch.class, branchId);
      if (branch.getAddressLine2() != null) {
        address2 = "-".concat(branch.getAddressLine2());
      }
      if (branch.getStreet() != null) {
        street = "-".concat(branch.getStreet());
      }
      if (branch.getCountry() != null) {
        country = "-".concat(branch.getCountry().getName());
      }
      if (branch.getCity() != null) {
        city = "-".concat(branch.getCity().getName());
      }
      address = branch.getAddressLine1().concat(address2).concat(street).concat(city)
          .concat(country);

      log.debug("branchaddress:" + address);
      return address;

    } catch (OBException e) {
      log.error("Exception while getting address in bank branch :" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Method to get business Partner ID
   * 
   * @param doocumentno
   * @param documentType
   * @return
   */
  public static String getbpartnerId(String doocumentno, String documentType) {
    try {
      OBContext.setAdminMode();
      if (documentType.equals("P")) {
        EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, doocumentno);
        return proposal.getBranchName().getId();
      } else if (documentType.equals("POC")) {
        Order ord = OBDal.getInstance().get(Order.class, doocumentno);
        return ord.getPartnerAddress().getId();
      }
      return null;
    } catch (OBException e) {
      log.error("Exception while getbpartnerId:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get days between expiry date and start date
   * 
   * @param startDate
   * @param Endate
   * @return true it is equal to or greater than 90 days, else return false
   */
  public static boolean getNoofDays(String startDate, String endDate) {
    try {

      if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

        java.util.Date startdate = dateFormat.parse(startDate);
        java.util.Date enddate = dateFormat.parse(endDate);

        if (startdate != null && enddate != null) {
          long diff = enddate.getTime() - startdate.getTime();
          long noOfdays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);

          if (noOfdays < 89) {
            return false;
          }
        }
      } else {
        return true;
      }

    } catch (OBException e) {
      log.error("Exception while getNoofDays:" + e);
      return true;
    } catch (ParseException e) {
      log.error("Exception while getNoofDays:" + e);
      return true;
    }
    return true;
  }

  /**
   * 
   * @param bgworkbenchId
   * @param chkstatusCo
   * @return
   */
  @SuppressWarnings("unchecked")
  public static Boolean chkTotBGAmtCalAmtinOEE(String bgworkbenchId) {
    Query query = null;
    String strQuery = null;

    BigDecimal totalbgamount = BigDecimal.ZERO, totalcalamount = BigDecimal.ZERO;
    List<Object> querylist = null;
    try {
      OBContext.setAdminMode();
      strQuery = "  select sum(det.bgamount) ,case when cast(bg.initialbg as numeric) > 0 then "
          + " coalesce(round((bg.document_amount * ((cast(bg.initialbg as numeric))/100)),2),0)  "
          + " else 0 end  as calamt from escm_bankguarantee_detail det left join "
          + " escm_bgworkbench bg on bg.escm_bgworkbench_id=det.escm_bgworkbench_id "
          + "  where  det.escm_bgworkbench_id=:bgworkbenchID ";

      strQuery += " group by bg.document_amount,bg.initialbg ";

      query = OBDal.getInstance().getSession().createSQLQuery(strQuery);
      query.setParameter("bgworkbenchID", bgworkbenchId);
      if (query != null) {
        log.debug("query:" + query);
        // query.setParameter(0, proposalId);
        querylist = query.list();
      }

      if (query != null && querylist.size() > 0) {
        Object[] row = (Object[]) querylist.get(0);
        totalbgamount = (BigDecimal) row[0];
        totalcalamount = (BigDecimal) row[1];
        if ((totalcalamount.compareTo(totalbgamount) > 0)) {
          return true;
        } else
          return false;
      } else
        return true;

    } catch (OBException e) {
      log.error("Exception while checkBankPercentage:" + e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}