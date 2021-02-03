package sa.elm.ob.scm.ad_process.ProposalManagement;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.EscmProposalsourceRef;
import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;
import sa.elm.ob.scm.Escmbidconfiguration;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class ProposalManagementProcessDAOImpl implements ProposalManagementProcessDAO {
  private static final Logger log = Logger.getLogger(ProposalManagementProcessDAOImpl.class);

  /**
   * Validate if PO created when cancel
   * 
   * @param proposalId
   * 
   * @return list
   */
  @Override
  public List<Order> checkPOCreated(String proposalId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Order> pmOrder = OBDal.getInstance().createQuery(Order.class,
          " as e where e.escmProposalmgmt.id=:proposalID and e.escmAppstatus !='ESCM_CA' ");
      pmOrder.setNamedParameter("proposalID", proposalId);
      return pmOrder.list();
    } catch (OBException e) {
      log.error("Exception while checkPOCreated:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get proposal attribute
   * 
   * @param proposalId
   * 
   * @return list
   */
  @Override
  public Object getProposalAttr(String proposalId) {
    try {
      OBContext.setAdminMode();
      // OBQuery<EscmProposalAttribute> proposalattribute = OBDal.getInstance().createQuery(
      // EscmProposalAttribute.class, " as e where e.escmProposalmgmt.id=:proposalId");
      // proposalattribute.setNamedParameter("proposalId", proposalId);

      String propAttrQry = " select escm_proposal_attr_id from escm_proposal_attr attr "
          + "left join escm_proposalmgmt pm on pm.escm_proposalmgmt_id=attr.escm_proposalmgmt_id "
          + "or attr.escm_proposalmgmt_id=pm.escm_baseproposal_id "
          + "where pm.escm_proposalmgmt_id=? ";
      Query propAttr = OBDal.getInstance().getSession().createSQLQuery(propAttrQry);
      propAttr.setString(0, proposalId);
      if (propAttr.list().size() > 0)
        return propAttr.list().get(0);
      else
        return null;
    } catch (OBException e) {
      log.error("Exception while getProposalAttr:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Based on configuration minvalue , getting purchase order type is purchase order /contract
   * 
   * @param orgId
   * @param totalAmt
   * @return list
   */
  @Override
  public List<EscmPurchaseOrderConfiguration> getPOTypeBasedOnValue(String orgId,
      BigDecimal totalAmt) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmPurchaseOrderConfiguration> configQry = OBDal.getInstance().createQuery(
          EscmPurchaseOrderConfiguration.class,
          " as e where e.minValue <=:totalAmt and e.organization.id=:orgID order by e.minValue desc ");
      configQry.setNamedParameter("totalAmt", totalAmt);
      configQry.setNamedParameter("orgID", orgId);
      configQry.setMaxResult(1);
      return configQry.list();
    } catch (OBException e) {
      log.error("Exception while getPOTypeBasedOnValue:" + e);
      throw new OBException(e.getMessage());
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  /**
   * get period start date
   * 
   * @param clientId
   * @return String
   */
  @Override
  public String getPeriodStartDate(String clientId) {
    String startingDate = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    try {
      OBContext.setAdminMode();
      ps = OBDal.getInstance().getConnection().prepareStatement(
          "select to_char(startdate,'dd-MM-yyyy') as startingDate from c_period where to_date('"
              + dateFormat.format(new Date()) + "','yyyy-MM-dd')"
              + " between to_date(to_char(startdate,'dd-MM-yyyy'),'dd-MM-yyyy') and to_date(to_char(enddate,'dd-MM-yyyy'),'dd-MM-yyyy') "
              + "  and ad_client_id =?");
      ps.setString(1, clientId);
      rs = ps.executeQuery();
      if (rs.next()) {
        startingDate = rs.getString("startingDate") == null ? "" : rs.getString("startingDate");
      }
    } catch (OBException e) {
      log.error("Exception while getPeriodStartDate:" + e);
      throw new OBException(e.getMessage());
    } catch (SQLException e) {
      log.error("Exception while getPeriodStartDate:" + e);
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (SQLException e) {
        log.error("Exception while closing connection:" + e);
      }
      OBContext.restorePreviousMode();
    }
    return startingDate;
  }

  /**
   * get budget reference from period date
   * 
   * @param clientId
   * @param startingDate
   * @return String
   */
  @Override
  public String getBudgetFromPeriod(String clientId, String startingDate) {
    String budgetReferenceId = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      ps = OBDal.getInstance().getConnection()
          .prepareStatement("select br.efin_budgetint_id from efin_budgetint br "
              + " join c_period fp on fp.c_period_id =br.fromperiod "
              + " join c_period tp on tp.c_period_id =br.toperiod "
              + " where to_date(?,'dd-MM-yyyy') between fp.startdate and tp.enddate "
              + " and br.status ='OP' and br.ad_client_id =? limit 1");
      ps.setString(1, startingDate);
      ps.setString(2, clientId);
      rs = ps.executeQuery();
      if (rs.next()) {
        budgetReferenceId = rs.getString("efin_budgetint_id");
      }
    } catch (OBException e) {
      log.error("Exception while getBudgetFromPeriod:" + e);
      throw new OBException(e.getMessage());
    } catch (SQLException e) {
      log.error("Exception while getBudgetFromPeriod:" + e);
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (SQLException e) {
        log.error("Exception while closing connection:" + e);
      }
      OBContext.restorePreviousMode();
    }
    return budgetReferenceId;
  }

  /**
   * fetch Financial Year
   * 
   * @param clientId
   * @return String
   */
  @Override
  public String getFinancialYear(String clientId) {
    String yearId = "";
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      OBContext.setAdminMode();
      String finacialyear = " select c_year_id from c_year where  c_year_id in (select y.c_year_id from c_period p"
          + "           join c_year y on y.c_year_id = p.c_year_id where now()::date"
          + "  between cast(p.startdate as date) and cast(p.enddate as date) and p.ad_client_id= ?) limit 1 ";
      ps = OBDal.getInstance().getConnection().prepareStatement(finacialyear);
      ps.setString(1, clientId);
      rs = ps.executeQuery();
      if (rs.next()) {
        yearId = rs.getString("c_year_id");
      }
    } catch (OBException e) {
      log.error("Exception while getFinancialYear:" + e);
      throw new OBException(e.getMessage());
    } catch (SQLException e) {
      log.error("Exception while getFinancialYear:" + e);
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (SQLException e) {
        log.error("Exception while closing connection:" + e);
      }
      OBContext.restorePreviousMode();
    }
    return yearId;
  }

  /**
   * fetching warehouse
   * 
   * @param clientID
   * 
   * @return list
   */
  @Override
  public List<Warehouse> getWarehouse(String clientID) {
    try {
      OBContext.setAdminMode();
      OBQuery<Warehouse> warehouseQry = OBDal.getInstance().createQuery(Warehouse.class,
          " as e where e.client.id=:clientID");
      warehouseQry.setNamedParameter("clientID", clientID);
      warehouseQry.setMaxResult(1);
      return warehouseQry.list();
    } catch (OBException e) {
      log.error("Exception while getWarehouse:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * fetching location
   * 
   * @param bpartnerID
   * 
   * @return list
   */
  @Override
  public List<Location> getLocation(String bpartnerID) {
    try {
      OBContext.setAdminMode();
      OBQuery<Location> bplocQry = OBDal.getInstance().createQuery(Location.class,
          " as e where e.businessPartner.id=:bpartnerID and e.active='Y' and e.shipToAddress='Y' ");
      bplocQry.setNamedParameter("bpartnerID", bpartnerID);
      bplocQry.setMaxResult(1);
      return bplocQry.list();
    } catch (OBException e) {
      log.error("Exception while getLocation:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * fetching price list
   * 
   * @param clientID
   * 
   * @return list
   */
  @Override
  public List<PriceList> getPriceList(String clientID) {
    try {
      OBContext.setAdminMode();
      OBQuery<PriceList> pricelistQry = OBDal.getInstance().createQuery(PriceList.class,
          " as e where e.client.id=:clientID and e.active='Y' and e.salesPriceList='N' ");
      pricelistQry.setNamedParameter("clientID", clientID);
      pricelistQry.setMaxResult(1);
      return pricelistQry.list();
    } catch (OBException e) {
      log.error("Exception while getPriceList:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * fetching payment term
   * 
   * @param clientID
   * 
   * @return list
   */
  @Override
  public List<PaymentTerm> getPaymentTerm(String clientID) {
    try {
      OBContext.setAdminMode();
      OBQuery<PaymentTerm> paymentTermQry = OBDal.getInstance().createQuery(PaymentTerm.class,
          " as e where e.client.id=:clientID and e.active='Y' ");
      paymentTermQry.setNamedParameter("clientID", clientID);
      paymentTermQry.setMaxResult(1);
      return paymentTermQry.list();
    } catch (OBException e) {
      log.error("Exception while getPaymentTerm:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get transaction document
   * 
   * @param orgId
   * @param clientID
   * @return Object
   */
  @Override
  public Object getTransactionDoc(String orgId, String clientId) {
    Object transdoctype = null;
    try {
      OBContext.setAdminMode();
      String transactiondoc = " select c_doctype_id from c_doctype where  DocBaseType IN ('SOO', 'POO') "
          + "AND (AD_ISORGINCLUDED(?,C_DocType.AD_Org_ID, ?)   <> '-1' OR COALESCE(?,'-1')='-1') and "
          + " ad_client_id= ? AND C_DocType.IsSOTrx='N' and C_DocType.IsReturn='N'   AND (docsubtypeso is null OR docsubtypeso not like 'OB') ";
      Query transactiondoctype = OBDal.getInstance().getSession().createSQLQuery(transactiondoc);
      transactiondoctype.setString(0, orgId);
      transactiondoctype.setString(1, clientId);
      transactiondoctype.setString(2, orgId);
      transactiondoctype.setString(3, clientId);
      transdoctype = transactiondoctype.list().get(0);
    } catch (OBException e) {
      log.error("Exception while getTransactionDoc:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return transdoctype;
  }

  /**
   * get bid configuration
   * 
   * @param clientID
   * @param orgId
   * @return list
   */
  @Override
  public List<Escmbidconfiguration> getBidConfiguration(String clientId, String orgId) {
    try {
      OBContext.setAdminMode();
      OBQuery<Escmbidconfiguration> bidTyp = OBDal.getInstance().createQuery(
          Escmbidconfiguration.class,
          "as e where e.client.id=:clientID and e.bidType='DRP' and e.organization.id=:orgID");
      bidTyp.setNamedParameter("clientID", clientId);
      bidTyp.setNamedParameter("orgID", orgId);
      return bidTyp.list();
    } catch (OBException e) {
      log.error("Exception while getBidConfiguration:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get proposal source reference
   * 
   * @param propLnId
   * 
   * @return list
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<Object> getProposalLnTotalQty(String propLnId) {
    String sql = "";
    try {
      OBContext.setAdminMode();
      sql = " select coalesce(sum(quantity),0) from escm_proposalsource_ref where  escm_proposalmgmt_line_id =?";
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
      query.setParameter(0, propLnId);
      return query.list();
    } catch (OBException e) {
      log.error("Exception while getProposalSourceRef:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get proposal source reference
   * 
   * @param proposalLnID
   * 
   * @return list
   */
  public List<EscmProposalsourceRef> getProposalSourceRef(String proposalLnID) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalsourceRef> proposalsrcref = OBDal.getInstance().createQuery(
          EscmProposalsourceRef.class,
          "escmProposalmgmtLine.id=:proposalLnID and requisitionLine.id is not null");
      proposalsrcref.setNamedParameter("proposalLnID", proposalLnID);
      return proposalsrcref.list();
    } catch (OBException e) {
      log.error("Exception while getProposalSourceRef:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get open envelop committe
   * 
   * @param bidID
   * 
   * @return list
   */
  @Override
  public List<Escmopenenvcommitee> getOpenEnvCommitte(String bidID) {
    try {
      OBContext.setAdminMode();
      OBQuery<Escmopenenvcommitee> openenvelop = OBDal.getInstance().createQuery(
          Escmopenenvcommitee.class, " as e where e.bidNo.id=:bidID and e.alertStatus='CO' ");
      openenvelop.setNamedParameter("bidID", bidID);
      openenvelop.setMaxResult(1);
      return openenvelop.list();
    } catch (OBException e) {
      log.error("Exception while getOpenEnvCommitte:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get proposal lines
   * 
   * @param proposalID
   * 
   * @return list
   */
  @Override
  public List<EscmProposalmgmtLine> getProposalLines(String proposalID) {
    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalmgmtLine> lines = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class, "escmProposalmgmt.id =:proposalID and summary = 'N'");
      lines.setNamedParameter("proposalID", proposalID);
      return lines.list();
    } catch (OBException e) {
      log.error("Exception while getProposalLines:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get User Role
   * 
   * @param roleID
   * 
   * @return list
   */
  @Override
  public List<UserRoles> getUserRole(String roleID) {
    try {
      OBContext.setAdminMode();
      OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
          "role.id=:roleID");
      userRole.setNamedParameter("roleID", roleID);
      return userRole.list();
    } catch (OBException e) {
      log.error("Exception while getUserRole:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get delegation
   * 
   * @param roleID
   * @param currentDate
   * @param documentType
   * @return list
   */
  @Override
  public List<EutDocappDelegateln> getDelegation(String roleID, Date currentDate,
      String documentType) {
    try {
      OBContext.setAdminMode();
      OBQuery<EutDocappDelegateln> delegationln = OBDal.getInstance().createQuery(
          EutDocappDelegateln.class,
          " as e left join e.eUTDocappDelegate as hd where hd.role.id =:roleID"
              + " and hd.fromDate <=:currentdate and hd.date >=:currentdate and e.documentType=:docType");
      delegationln.setNamedParameter("roleID", roleID);
      delegationln.setNamedParameter("currentdate", currentDate);
      delegationln.setNamedParameter("docType", documentType);
      return delegationln.list();
    } catch (OBException e) {
      log.error("Exception while getDelegation:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get encumbrance lines
   * 
   * @param encumID
   * @param acctID
   * 
   * @return list
   */
  @Override
  public List<EfinBudgetManencumlines> getEncumLines(String encumID, String acctID) {
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " as e  where e.manualEncumbrance.id=:encumID and  e.accountingCombination.id=:acctID");
      manline.setNamedParameter("encumID", encumID);
      manline.setNamedParameter("acctID", acctID);
      manline.setMaxResult(1);
      return manline.list();
    } catch (OBException e) {
      log.error("Exception while getEncumLines:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public String getmaxbidproposallastdayandbidnumber(EscmProposalMgmt proposalmgmt) {
    String sqlquery = null, bidno = "", strproposallastday = "", proposallastdayhijri = "";
    Date proposallastday = null;
    Query query = null;
    String message = "";

    String proposalSubmissionDate = null;
    Date proposalSubDate = null;
    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    DateFormat yearFormat = Utility.YearFormat;

    String preferenceValue = "N";
    try {
      preferenceValue = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
          proposalmgmt.getClient().getId(), proposalmgmt.getOrganization().getId(),
          OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
          Constants.PROPOSAL_MANAGEMENT_W);
      if (preferenceValue == null) {
        preferenceValue = "N";
      }
    } catch (PropertyException e) {
      preferenceValue = "N";
    }

    sqlquery = " select to_date(to_char(maxbd.proposallastday,'yyyy-MM-dd HH24:mi:ss'),'yyyy-MM-dd HH24:mi:ss') , bidm.bidno "
        + " ,eut_convert_to_hijri(to_char(maxbd.proposallastday,'yyyy-MM-dd')), max( maxbd.proposallastdaytime) "
        + " from ( select escm_bidmgmt_id, max(escm_biddates.proposallastday) as lastday from  escm_biddates group by escm_bidmgmt_id ) a "
        + " join escm_biddates maxbd on a.lastday= maxbd.proposallastday and a.escm_bidmgmt_id= maxbd.escm_bidmgmt_id "
        + " join escm_bidmgmt bidm on bidm.escm_bidmgmt_id=maxbd.escm_bidmgmt_id "
        + " where bidm.escm_bidmgmt_id=?  group by maxbd.proposallastday,bidm.bidno ";
    query = OBDal.getInstance().getSession().createSQLQuery(sqlquery);
    query.setParameter(0, proposalmgmt.getEscmBidmgmt().getId());
    List<Object> list = query.list();
    if (query.list().size() > 0) {
      for (@SuppressWarnings("rawtypes")
      Iterator iterator = list.iterator(); iterator.hasNext();) {
        Object[] row = (Object[]) iterator.next();
        if (row[0] != null) {
          try {
            strproposallastday = row[0].toString();
            proposallastday = dateformat
                .parse(strproposallastday + " " + row[3].toString() + ":00");
            if (proposalmgmt.getSubmissiondate() != null
                && proposalmgmt.getSubmissiontime() != null) {
              proposalSubmissionDate = yearFormat.format(proposalmgmt.getSubmissiondate()) + " "
                  + proposalmgmt.getSubmissiontime() + ":00";
              proposalSubDate = dateformat.parse(proposalSubmissionDate);
            }

          } catch (ParseException e) {
            // TODO Auto-generated catch block
            log.error("exception while getmaxbidproposallastdayandbidnumber() "
                + "in ProposalManagementEvent", e);
          }
        }
        if (row[1] != null) {
          bidno = row[1].toString();
        }
        if (row[2] != null) {
          proposallastdayhijri = row[2].toString() + " " + row[3].toString() + ":00";
        }
      }
    }

    // Skip Past Date validation if SCM_AllowPastDate preference is present
    if (proposallastday != null && proposalSubDate != null
        && proposallastday.compareTo(proposalSubDate) < 0 && preferenceValue.equals("N")) {

      message = OBMessageUtils.messageBD("ESCM_BidProposallastday");
      message = message.replace("$", proposallastdayhijri).replace("%", bidno);

    } else {
      message = "Success";
    }
    return message;
  }

  /**
   * if proposal is associate with manual encumbrance or auto encumbrance then check used amount is
   * greater or zero
   * 
   * @param EscmProposalmgmtLine
   * @return if greater the used amount then return false
   */
  @Override
  public boolean chkEncumbranceLineCancelValid(EscmProposalmgmtLine proposalMgmtLine) {
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetManencumlines> encline = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class, " as e where e.id=:encumLineId and e.usedAmount > 0 ");
      encline.setNamedParameter("encumLineId", proposalMgmtLine.getId());
      if (encline.list().size() > 0) {
        return true;
      } else
        return false;
    } catch (final Exception e) {
      log.error("Exception in chkEncumbranceLineCancelValid : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check associated PR Full Qty used or partialy used or combine more than one Encumbrance
   * 
   * @param EscmProposalmgmtLine
   * @return Jsonobject of Encumbrance List, (Type-Split or Merge),PR is associated or Not
   */
  @Override
  public JSONObject checkFullPRLineQtyUitlizeorNot(EscmProposalmgmtLine line) {
    JSONObject result = new JSONObject();
    List<Requisition> requisitionLs = new ArrayList<Requisition>();
    List<EfinBudgetManencum> reqEncumLs = new ArrayList<EfinBudgetManencum>();
    boolean isAssociatePREncumbrance = false;
    int srcrefReqLineCount = 0, reqLineCount = 0, encReqCount = 0, reqCount = 0;
    BigDecimal srcrefLineQty = BigDecimal.ZERO, reqLineQty = BigDecimal.ZERO,
        reqlineAmt = BigDecimal.ZERO;
    Boolean isLineCountSame = true, isLineQtySame = true, ismanualLine = false,
        isEncReqCountSame = true, isEncumAppAmtZero = true;
    try {
      if (!line.isSummary() && !line.isManual()) {
        if (line.getEscmProposalsourceRefList().size() > 0) {
          for (EscmProposalsourceRef srcrefObj : line.getEscmProposalsourceRefList()) {
            // chk source ref having purchase requisition
            // is PRE(Purchase Encumbrance Type)
            if (srcrefObj.getRequisition() != null
                && srcrefObj.getRequisition().getEfinBudgetManencum() != null && srcrefObj
                    .getRequisition().getEfinBudgetManencum().getEncumType().equals("PRE")) {
              // if PR is associated then set the flag as true
              if (srcrefObj.getRequisition().isEfinSkipencumbrance()) {
                break;
              } else {
                isAssociatePREncumbrance = true;
              }
              if (isAssociatePREncumbrance) {
                // forming encumbrance and req List
                if (reqEncumLs != null
                    && !reqEncumLs.contains(srcrefObj.getRequisition().getEfinBudgetManencum())) {
                  reqEncumLs.add(srcrefObj.getRequisition().getEfinBudgetManencum());
                  requisitionLs.add(srcrefObj.getRequisition());
                } else {
                  reqEncumLs.add(srcrefObj.getRequisition().getEfinBudgetManencum());
                  requisitionLs.add(srcrefObj.getRequisition());
                }
              }
            }
          }
        }
      } else if (!line.isSummary() && line.isManual()) {
        ismanualLine = true;
      }

      // avoid the encumbrance/requisition duplicate
      HashSet<EfinBudgetManencum> reqEncumSet = new HashSet<EfinBudgetManencum>(reqEncumLs);
      if (reqEncumSet != null && reqEncumSet.size() == 1 && isAssociatePREncumbrance) {
        HashSet<Requisition> requisitionSet = new HashSet<Requisition>(requisitionLs);
        Iterator<Requisition> iterator = requisitionSet.iterator();
        String encumId = reqEncumSet.iterator().next().getId();

        // itereate the Requisition List
        while (iterator.hasNext()) {
          Requisition reqObj = iterator.next();
          // get the Requisition Line count
          reqLineCount = reqObj.getProcurementRequisitionLineList().size();
          // OBQuery<EscmProposalsourceRef> srcref = OBDal.getInstance().createQuery(
          // EscmProposalsourceRef.class,
          // " as e where e.escmProposalmgmtLine.id =:lineId and e.requisition.id=:reqID");
          // srcref.setNamedParameter("lineId", line.getId());
          // srcref.setNamedParameter("reqID", reqObj.getId());

          OBQuery<EscmProposalsourceRef> srcref = OBDal.getInstance()
              .createQuery(EscmProposalsourceRef.class, " as e where e.escmProposalmgmtLine.id in"
                  + " ( select b.id from Escm_Proposalmgmt_Line b where b.escmProposalmgmt.id=:proposalID) "
                  + " and e.requisition.id=:reqID");
          srcref.setNamedParameter("proposalID", line.getEscmProposalmgmt().getId());
          srcref.setNamedParameter("reqID", reqObj.getId());

          log.debug("srcref:" + srcref.getWhereAndOrderBy());

          // get the source ref Requisition Line count in Bid
          srcrefReqLineCount = srcref.list().size();

          // if count is not same set the flag of "isLineCountSame" is False
          if (srcrefReqLineCount != reqLineCount) {
            isLineCountSame = false;
          }
        }
        // if count is same then check full qty used in each Requisition Line
        if (isLineCountSame) {
          Iterator<Requisition> iteratorreq = requisitionSet.iterator();
          while (iteratorreq.hasNext()) {
            Requisition reqObj = iteratorreq.next();
            for (RequisitionLine lines : reqObj.getProcurementRequisitionLineList()) {
              if (!lines.isEscmIssummary()) {
                // get the each requisition line qty
                srcrefLineQty = lines.getQuantity();

                // get the source ref requisition line qty
                // OBQuery<EscmProposalsourceRef> srcref = OBDal.getInstance().createQuery(
                // EscmProposalsourceRef.class, " as e where e.escmProposalmgmtLine.id =:lineId "
                // + " and e.requisition.id=:reqID and e.requisitionLine.id=:reqLnID ");
                // srcref.setNamedParameter("lineId", line.getId());
                // srcref.setNamedParameter("reqID", reqObj.getId());
                // srcref.setNamedParameter("reqLnID", lines.getId());
                // srcref.setMaxResult(1);

                OBQuery<EscmProposalsourceRef> srcref = OBDal.getInstance().createQuery(
                    EscmProposalsourceRef.class,
                    " as e where e.escmProposalmgmtLine.id in "
                        + " ( select b.id from Escm_Proposalmgmt_Line b where b.escmProposalmgmt.id=:proposalID ) "
                        + " and e.requisition.id=:reqID and e.requisitionLine.id=:reqLnID ");
                srcref.setNamedParameter("proposalID", line.getEscmProposalmgmt().getId());
                srcref.setNamedParameter("reqID", reqObj.getId());
                srcref.setNamedParameter("reqLnID", line.getId());
                srcref.setMaxResult(1);

                log.debug("srcrefs:" + srcref);
                if (srcref.list().size() > 0) {
                  reqLineQty = srcref.list().get(0).getReservedQuantity();
                  reqlineAmt = reqlineAmt.add(reqLineQty.multiply(lines.getUnitPrice()));
                }
                // if req line qty and src ref line qty is not same then set the flag of
                // isLineQtySame
                // is "false"
                if (reqLineQty.compareTo(srcrefLineQty) != 0) {
                  isLineQtySame = false;
                }
              }
            }
          }
        } else {
          isLineQtySame = false;
        }

        if (encumId != null) {
          EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
              encumId);
          BigDecimal remainigAmt = encumbrance.getEfinBudgetManencumlinesList().stream()
              .filter(a -> a.getAPPAmt().compareTo(BigDecimal.ZERO) > 0)
              .map(a -> a.getRemainingAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
          if (encumbrance.getRevamount().compareTo(encumbrance.getAppliedAmount()) != 0) {
            if (reqlineAmt.compareTo((encumbrance.getAppliedAmount().add(remainigAmt))) == 0) {
              isEncumAppAmtZero = true;
            } else {
              isEncumAppAmtZero = false;
            }
          }
          if (isEncumAppAmtZero) {
            OBQuery<Requisition> reqcount = OBDal.getInstance().createQuery(Requisition.class,
                " as e where e.efinBudgetManencum.id=:encumID and e.escmDocStatus  not in ('ESCM_CA') ");
            reqcount.setNamedParameter("encumID", encumId);
            if (reqcount.list().size() > 0) {
              encReqCount = reqcount.list().size();
            }
            reqCount = requisitionSet.size();
            if (reqCount != encReqCount) {
              isEncReqCountSame = false;
            }
          }
        }

        // if line qty same set isFullQtyUsed as "true" && isLineUniqCodeSame
        if (isLineQtySame && isEncumAppAmtZero && isEncReqCountSame && !ismanualLine) {
          result.put("encumbrance", encumId);
          result.put("isFullQtyUsed", true);
          result.put("isLineCountSame", true);
          result.put("encumbrance", encumId);
        }
        // if line qty not same set isFullQtyUsed as "False" and encumbrance list is more than one
        // set the type as "MERGE" or else "SPLIT"
        else {
          result.put("isFullQtyUsed", false);
          if (isLineCountSame) {
            result.put("isLineCountSame", true);
          } else
            result.put("isLineCountSame", false);
          if (reqEncumSet != null && reqEncumSet.size() == 1) {
            result.put("encumbrance", encumId);
            result.put("type", "SPLIT");
          }
        }
      } else if (reqEncumSet != null && reqEncumSet.size() > 1) {
        String encumId = reqEncumSet.iterator().next().getId();
        result.put("isFullQtyUsed", false);
        result.put("type", "MERGE");
        result.put("encumbrance", encumId);
      }
      result.put("isAssociatePREncumbrance", isAssociatePREncumbrance);
    } catch (Exception e) {
      log.error("Exception in checkFullPRLineQtyUitlizeorNot " + e.getMessage());
      return result;
    }
    return result;
  }

  /**
   * check and update proposal pr full qty
   * 
   * @param proposalmgmtline
   * @param encumbrance
   * @param isChkFundsAppliedAmt
   * @param isreject
   * @return
   */
  @Override
  @SuppressWarnings("rawtypes")
  public Boolean chkAndUpdateforProposalPRFullQty(EscmProposalmgmtLine proposalmgmtline,
      EfinBudgetManencum encumbrance, Boolean isChkFundsAppliedAmt, Boolean isreject) {
    JSONObject commonvalresult = null;
    Query query = null;
    BigDecimal proposalAmt = BigDecimal.ZERO;
    BigDecimal reqAmount = BigDecimal.ZERO, diff = BigDecimal.ZERO, appliedAmt = BigDecimal.ZERO,
        usedAmt = BigDecimal.ZERO, revAmt = BigDecimal.ZERO, unappAmt = BigDecimal.ZERO;
    List<EscmProposalmgmtLine> linelist = new ArrayList<EscmProposalmgmtLine>();
    Boolean errorFlag = false, errorunUsedFlag = false;
    List<EfinBudgetManencumlines> enclinelist = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencumlines line = null;
    String encumType = null;
    if (encumbrance != null)
      encumType = encumbrance.getEncumMethod();
    try {

      if (isChkFundsAppliedAmt) {
        proposalmgmtline.setEfinFailureReason(null);
        OBDal.getInstance().save(proposalmgmtline);
      }

      String prosallineQry = " select ln.line_total, ln.em_efin_c_validcombination_id  from escm_proposalmgmt_line ln  "
          + " where ln.escm_proposalmgmt_line_id= ?  and ln.issummarylevel  ='N' and (ln.status != 'CL' or ln.status is null)  ";
      query = OBDal.getInstance().getSession().createSQLQuery(prosallineQry);
      query.setParameter(0, proposalmgmtline.getId());
      log.debug("strQuery:" + query.toString());
      List queryList = query.list();
      if (queryList != null && queryList.size() > 0) {
        // for (Iterator iterator = queryList.iterator(); iterator.hasNext();) {
        Object[] row = (Object[]) queryList.get(0);
        AccountingCombination com = OBDal.getInstance().get(AccountingCombination.class,
            row[1].toString());

        if (row[0] != null)
          proposalAmt = new BigDecimal(row[0].toString());

        String reqlnQry = " select case when coalesce(sum(reqln.qty),0) > 0   "
            + " then sum(round((coalesce(reqln.priceactual,0)*coalesce(ref.quantity,0)),2)) else 0 end  as amount ,"
            + " reqln.em_efin_c_validcombination_id  from escm_proposalsource_ref ref "
            + " join escm_proposalmgmt_line ln on ln.escm_proposalmgmt_line_id= ref.escm_proposalmgmt_line_id  "
            + " and ln.issummarylevel  ='N' and (ln.status != 'CL' or ln.status is null)  "
            + " join m_requisitionline reqln on reqln.m_requisitionline_id= ref.m_requisitionline_id "
            + " and reqln.em_escm_issummary='N' where ln.escm_proposalmgmt_line_id= ? "
            + " and reqln.em_efin_c_validcombination_id=  ? group by reqln.em_efin_c_validcombination_id ";
        query = OBDal.getInstance().getSession().createSQLQuery(reqlnQry);
        query.setParameter(0, proposalmgmtline.getId());
        query.setParameter(1, row[1].toString());
        log.debug("strQuery:" + query.toString());
        List reqqueryList = query.list();
        if (reqqueryList != null && reqqueryList.size() > 0) {
          for (Iterator reqiterator = reqqueryList.iterator(); reqiterator.hasNext();) {
            Object[] reqrow = (Object[]) reqiterator.next();
            if (reqrow[0] != null) {
              reqAmount = new BigDecimal(reqrow[0].toString());
            }
            diff = proposalAmt.subtract(reqAmount);

            OBQuery<EfinBudgetManencumlines> enclineQry = OBDal.getInstance().createQuery(
                EfinBudgetManencumlines.class,
                " as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id=:uniqcodeID");
            enclineQry.setNamedParameter("encumID", encumbrance.getId());
            enclineQry.setNamedParameter("uniqcodeID", row[1].toString());
            enclineQry.setMaxResult(1);
            enclinelist = enclineQry.list();
            if (enclinelist.size() > 0) {
              enclinelist = enclineQry.list();
              line = enclinelist.get(0);
              appliedAmt = line.getAPPAmt();
              usedAmt = line.getUsedAmount();
              revAmt = line.getRevamount();
              unappAmt = (revAmt.subtract(appliedAmt)).subtract(usedAmt);
            }
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
              continue;
            }
            // increase
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
              // Auto- check funds available ( increase) - approve
              if (encumType.equals("A")) {
                if (isChkFundsAppliedAmt && !isreject) {
                  commonvalresult = CommonValidationsDAO
                      .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, diff);

                  if (commonvalresult.getString("errorFlag").equals("0")) {
                    errorFlag = true;
                    if (linelist.size() > 0) {
                      for (EscmProposalmgmtLine prlline : linelist) {
                        prlline.setEfinFailureReason(commonvalresult.getString("message"));
                        OBDal.getInstance().save(prlline);
                      }
                    }
                  }
                }
              } else if (encumType.equals("M")) {
                // Manual- check applied amount ( increase) - approve
                if (isChkFundsAppliedAmt && !isreject) {
                  if (diff.compareTo(unappAmt) > 0) {
                    errorFlag = true;
                    if (linelist.size() > 0) {
                      for (EscmProposalmgmtLine lines : linelist) {
                        lines.setEfinFailureReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
                        OBDal.getInstance().save(lines);
                      }
                    }
                  }
                }
              }
              // update applied amt,rev amount in case of approve for both manual and auto (
              // reject case increase is decrease and decrease is increase)
              if (!isChkFundsAppliedAmt) {
                if (isreject)
                  diff = diff.negate();
                line.setAPPAmt(line.getAPPAmt().add(diff));
              }
            }
            // decrease
            else if (diff.compareTo(BigDecimal.ZERO) < 0) {
              // update applied amt and revamount in case of approve both manual and auto ( reject
              // case increase is decrease and decrease is increase)
              if (!isChkFundsAppliedAmt) {
                if (isreject)
                  diff = diff.negate();
                line.setAPPAmt(line.getAPPAmt().add(diff));
                if (encumType.equals("A")) {
                }
              }
              // Auto - check while reject funds available when the time of decrease
              if (isChkFundsAppliedAmt && isreject) {
                if (encumType.equals("A")) {
                  commonvalresult = CommonValidationsDAO.CommonFundsChecking(
                      encumbrance.getBudgetInitialization(), com, diff.negate());

                  if (commonvalresult.getString("errorFlag").equals("0")) {
                    errorFlag = true;
                    if (linelist.size() > 0) {
                      for (EscmProposalmgmtLine prlline : linelist) {
                        prlline.setEfinFailureReason(commonvalresult.getString("message"));
                        OBDal.getInstance().save(prlline);
                      }
                    }
                  }
                } else {
                  // Manual - check while reject applied amount for encumbrance when the time of
                  // decrease
                  if (diff.negate().compareTo(unappAmt) > 0) {
                    errorFlag = true;
                    if (linelist.size() > 0) {
                      for (EscmProposalmgmtLine lines : linelist) {
                        lines.setEfinFailureReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
                        OBDal.getInstance().save(lines);
                      }
                    }
                  }
                }
              }
            }
            // auto means update budget inquiry and reflect the modification
            if (encumType.equals("A") && !isChkFundsAppliedAmt) {
              // while approve the auto insert the modification for both increase and decrease
              if (!isreject)
                // insert encumbrance modification
                BidManagementDAO.insertEncumbranceModification(line, diff, null, "PRO", null, null);
              // while reject delete the modification
              if (isreject) {
                diff = diff.negate();
                ProposalManagementRejectMethods.deleteModification(line, diff);
              }
              // if reject take increase as decrease and decrease as increase to update the budget
              // enquiry
              if (isreject)
                diff = diff.negate();
              // update budget inquiry Trigger changes
            }
          }
        } else {
          if (isChkFundsAppliedAmt && !isreject) {
            if (encumType.equals("A")) {
              commonvalresult = CommonValidationsDAO
                  .CommonFundsChecking(encumbrance.getBudgetInitialization(), com, proposalAmt);

              if (commonvalresult.getString("errorFlag").equals("0")) {
                errorFlag = true;
                if (linelist.size() > 0) {
                  for (EscmProposalmgmtLine prlline : linelist) {
                    prlline.setEfinFailureReason(commonvalresult.getString("message"));
                    OBDal.getInstance().save(prlline);
                  }
                }
              }
            } else {
              if (!isreject) {
                for (EscmProposalmgmtLine prlline : linelist) {
                  errorFlag = true;
                  prlline
                      .setEfinFailureReason(OBMessageUtils.messageBD("EFIN_PropNewUniqNotAllow"));
                  OBDal.getInstance().save(prlline);
                }
              } else {
              }
            }
          } else {
            if (encumType.equals("A") && !isChkFundsAppliedAmt) {
              if (!isreject) {
                ProposalManagementActionMethod.insertEncumbranceLines(encumbrance, proposalAmt, com,
                    null, proposalmgmtline);
              } else {
                // delete the encumbrance lines
                ProposalManagementRejectMethods.deleteEncumLines(encumbrance, com, null,
                    proposalmgmtline);
              }
            }
          }
        }
      }
      if (isChkFundsAppliedAmt) {
        if (encumType.equals("A")) {
          if (isreject) {
            errorunUsedFlag = ProposalManagementActionMethod.unusedEncumbranceUniquecodeUpdation(
                null, encumbrance, true, true, proposalmgmtline);
          }
        } else {
          if (isreject) {
            errorunUsedFlag = ProposalManagementActionMethod.unusedEncumbranceUniquecodeUpdation(
                null, encumbrance, true, true, proposalmgmtline);
          }
        }
      } else {
        if (encumType.equals("A")) {
          if (!isreject) {
            ProposalManagementActionMethod.unusedEncumbranceUniquecodeUpdation(null, encumbrance,
                false, false, proposalmgmtline);
          } else {
            ProposalManagementActionMethod.unusedEncumbranceUniquecodeUpdation(null, encumbrance,
                false, true, proposalmgmtline);
          }
        }
      }
      if (errorunUsedFlag || errorFlag) {
        return true;
      }
      return errorFlag;
    } catch (Exception e) {
      log.error("Exception in getUniqueCodeListforFundschk " + e.getMessage());
    }
    return errorFlag;
  }

  /**
   * update auto encumbrance value in budget enquiry ( with out bid in proposal)
   * 
   * @param EscmProposalmgmtLine
   */
  @Override
  public void updateAutoEncumbrancechanges(EscmProposalmgmtLine proposalmgmtline,
      boolean isCancel) {
    try {
      OBContext.setAdminMode();
      // checking with propsal line
      if (!proposalmgmtline.isSummary()) {
        EfinBudgetManencumlines encline = proposalmgmtline.getEfinBudgmanencumline();
        if (isCancel) {
          encline.setAPPAmt(encline.getAPPAmt().subtract(proposalmgmtline.getLineTotal()));
          BidManagementDAO.insertEncumbranceModification(encline,
              proposalmgmtline.getLineTotal().negate(), null, "PRO", null, null);
        } else {
          encline.getManualEncumbrance().setDocumentStatus("DR");
          OBDal.getInstance().remove(encline);
        }
      }
    } catch (final Exception e) {
      log.error("Exception in updateManualEncumAppAmt after Reject : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This Method is used to revert the encumbrance stage and set to PR original amt
   * 
   * @param result
   * @param proposal
   * @param proposalmgmtline
   */
  @Override
  public void revertEncumbranceStage(JSONObject result, EscmProposalMgmt proposal,
      EscmProposalmgmtLine proposalmgmtline) {
    EfinBudgetManencum oldEncumbrance = null;
    JSONObject json = null, jsonencum = null, result1 = null;
    JSONObject prResult = null;
    EfinBudManencumRev manEncumRev = null;

    try {
      // create Encumbrance
      if (result.getString("encumbrance") != null) {
        oldEncumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
            result.getString("encumbrance"));

        // get PR detail based on associated Bid source ref .
        prResult = ProposalManagementActionMethod.getPRDetailsBasedOnProposalQty1(proposal,
            proposalmgmtline);
        log.debug("prResult:" + prResult);
        if (prResult != null && prResult.getJSONObject("prListarray") != null) {
          result1 = prResult.getJSONObject("prListarray");
          JSONArray array = result1.getJSONArray("list");
          for (int i = 0; i < array.length(); i++) {
            json = array.getJSONObject(i);
            JSONArray encumarray = json.getJSONArray("encList");
            for (int j = 0; j < encumarray.length(); j++) {
              BigDecimal Amount = BigDecimal.ZERO;
              BigDecimal propAmount = BigDecimal.ZERO;

              jsonencum = encumarray.getJSONObject(j);
              if (jsonencum.has("encumId") && jsonencum.getString("encumId") != null
                  && jsonencum.getString("validcomId") != null) {
                // get old encumbrance line and set original PR amt
                OBQuery<EfinBudgetManencumlines> lines = OBDal.getInstance().createQuery(
                    EfinBudgetManencumlines.class,
                    " as e where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:accId");
                lines.setNamedParameter("encumId", jsonencum.getString("encumId"));
                lines.setNamedParameter("accId", jsonencum.getString("validcomId"));
                lines.setMaxResult(1);
                if (lines.list().size() > 0) {
                  // decrease the rev amount and remaining amount
                  Amount = new BigDecimal(jsonencum.getString("encamount"));
                  log.debug("amount1:" + Amount);
                  EfinBudgetManencumlines encumline = lines.list().get(0);
                  encumline.setAPPAmt(Amount);
                  OBDal.getInstance().save(encumline);
                }
                if (!jsonencum.getString("validcomId")
                    .equals(jsonencum.getString("bidvalidcomId"))) {
                  // get new encumbrance line and revert amt
                  OBQuery<EfinBudgetManencumlines> newline = OBDal.getInstance().createQuery(
                      EfinBudgetManencumlines.class,
                      " as e where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:accId");
                  newline.setNamedParameter("encumId", jsonencum.getString("encumId"));
                  newline.setNamedParameter("accId", jsonencum.getString("bidvalidcomId"));
                  newline.setMaxResult(1);
                  if (newline.list().size() > 0) {
                    propAmount = new BigDecimal(jsonencum.getString("totalamount"));
                    log.debug("amount1:" + Amount);
                    EfinBudgetManencumlines encumline = newline.list().get(0);
                    encumline.setAPPAmt(encumline.getAPPAmt().subtract(propAmount));
                    OBDal.getInstance().save(encumline);

                    // insert the Encumbrance revision entry(-ve value)
                    manEncumRev = BidManagementDAO.insertEncumbranceModification(encumline,
                        propAmount.negate(), null, "PRO", null, null);
                  }
                }
              }
            }
          }
        }
        oldEncumbrance.setEncumStage("PRE");
      }
    } catch (OBException e) {
      log.error(" Exception while revertEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("Exception in revertEncumbrance " + e.getMessage(), e);
    }
  }

  /**
   * get base revision no of proposal
   * 
   * @param proposalId
   * 
   * @return revno in Long
   */
  @Override
  public Long checkBaseProposal(String proposalId) {
    Long revNo = 0L;
    try {
      OBQuery<EscmProposalMgmt> proposal = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
          "as e where e.escmBaseproposal.id=:proposalId order by created desc");
      proposal.setNamedParameter("proposalId", proposalId);
      proposal.setFilterOnActive(false);
      proposal.setMaxResult(1);
      if (proposal.list().size() > 0) {
        EscmProposalMgmt propRev = proposal.list().get(0);
        revNo = propRev.getVersionNo();
      }
    } catch (Exception e) {
      log.error("Exception while getRevisionNo", e);
      OBDal.getInstance().rollbackAndClose();
      return 0L;
    }
    return revNo;
  }

  /**
   * get latest revision of proposal
   * 
   * @param baseproposalId
   * 
   * @return revno in Long
   */
  @Override
  public Long getRevisionNo(String basePropId) {
    Long revNo = 0L;
    StringBuffer query = null;
    Query revQuery = null;
    try {
      query = new StringBuffer();
      query.append("select max(prop.versionNo) as rev from Escm_Proposal_Management prop "
          + "where prop.escmBaseproposal.id=:basePropId and prop.active in ('N', 'Y')"
          + "group by prop.escmBaseproposal.id ");
      revQuery = OBDal.getInstance().getSession().createQuery(query.toString());
      revQuery.setParameter("basePropId", basePropId);

      log.debug(" Query : " + query.toString());
      if (revQuery != null) {
        if (revQuery.list().size() > 0) {
          if (revQuery.iterate().hasNext()) {
            String rev = revQuery.iterate().next().toString();
            revNo = Long.parseLong(rev);
          }
        }
      }

    } catch (Exception e) {
      log.error("Exception while getRevisionNo", e);
      OBDal.getInstance().rollbackAndClose();
      return 0L;
    }
    return revNo;
  }

  /**
   * check and validate duplicate version
   * 
   * @param propId
   * @return true, if has duplicate
   */
  @Override
  public boolean checkDuplicateVersion(String propId) {
    String query = " as e where e.escmOldproposal.id=? and (e.proposalstatus<>'CL' and e.proposalstatus<>'WD' and e.proposalstatus<>'DIS')  ";
    List<EscmProposalMgmt> propLs = null;
    List<Object> parametersList = new ArrayList<Object>();
    parametersList.add(propId);

    try {
      OBContext.setAdminMode();
      OBQuery<EscmProposalMgmt> prop = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
          query, parametersList);
      propLs = prop.list();

      if (propLs.size() > 0) {
        return true;
      }
    } catch (OBException e) {
      log.error("Exception while checkDuplicateVersion:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return false;
  }

  /**
   * Checks the mandatory fields in PO
   * 
   * @param proposalId
   * @return mandatory list
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<String> checkMandatoryfields(String proposalId) {
    Query query = null;
    List<String> filedlist = null;
    try {
      String prosallineQry = " select po.ad_field_id from escm_pomandatory_lookup po "
          + " join escm_deflookups_typeln defln ON defln.escm_deflookups_typeln_id = po.escm_deflookups_typeln_id"
          + " join escm_proposalmgmt prop ON prop.contract_type = defln.escm_deflookups_typeln_id "
          + " where prop.escm_proposalmgmt_id= ?";
      query = OBDal.getInstance().getSession().createSQLQuery(prosallineQry);
      query.setParameter(0, proposalId);
      log.debug("strQuery:" + query.toString());
      filedlist = query.list();
      return filedlist;
    } catch (OBException e) {
      log.error("Exception while checkMandatoryfields:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * check the PO is contract or not
   * 
   * @param totalAmount
   * @param orgId
   * @param clientId
   * @return true if it is contract
   */
  @Override
  public boolean checkOrderIsContract(BigDecimal totalAmount, String orgId, String clientId) {
    BigInteger count = BigInteger.ZERO;
    Query query;
    String orgsId = "0";
    String orderType = "";
    BigInteger confCount = BigInteger.ZERO;
    try {
      String orderQry = "select count(escm_poorder_config_id) from escm_poorder_config where ad_org_id = ? and ad_client_id = ?";
      query = OBDal.getInstance().getSession().createSQLQuery(orderQry);
      query.setParameter(0, orgId);
      query.setParameter(1, clientId);

      if (query.list().size() > 0) {
        count = (BigInteger) query.list().get(0);
      }

      if (count.equals(BigInteger.ZERO)) {
        String orgQry = "select agency_hq_org from efin_budget_ctrl_param where ad_client_id =?  limit 1";
        query = OBDal.getInstance().getSession().createSQLQuery(orgQry);
        query.setParameter(0, clientId);
        if (query.list().size() > 0) {
          orgsId = (String) query.list().get(0);
        }
        String confQry = "select count(escm_poorder_config_id) from escm_poorder_config where ad_org_id = ? and ad_client_id = ?";
        query = OBDal.getInstance().getSession().createSQLQuery(confQry);
        query.setParameter(0, orgsId);
        query.setParameter(1, clientId);
        if (query.list().size() > 0) {
          confCount = (BigInteger) query.list().get(0);
        }

        if (confCount.equals(BigInteger.ZERO)) {
          orgsId = "0";
        }

      }
      String poconfigqry = "select coalesce(ordertype,'PUR') "
          + " from escm_poorder_config where ? >= minvalue and ad_org_id = ? and ad_client_id = ? order by minvalue desc limit 1 ";
      query = OBDal.getInstance().getSession().createSQLQuery(poconfigqry);
      query.setParameter(0, totalAmount);
      query.setParameter(1, orgsId);
      query.setParameter(2, clientId);
      if (query.list().size() > 0) {
        orderType = (String) query.list().get(0);
      }

      if (orderType.equals("CR")) {
        return true;
      } else {
        return false;
      }

    } catch (OBException e) {
      log.error("Exception while checkMandatoryfields:" + e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    } finally {
      // OBContext.restorePreviousMode();
    }
  }

  /**
   * Check minimum proposal is approved
   * 
   * @param proposal
   * @return
   */
  public boolean isMinProposalApproved(EscmProposalMgmt proposal) {

    boolean isMinProposalApproved = false;
    try {
      OBContext.setAdminMode();
      if (proposal.getEscmBidmgmt() != null) {
        List<String> documentnoList = proposal.getEscmBidmgmt().getEscmProposalManagementList()
            .stream().map(a -> a.getProposalno()).collect(Collectors.toList());
        // sort it to get minimum proposal number
        Collections.sort(documentnoList);
        OBQuery<EscmProposalMgmt> proposalQuery = OBDal.getInstance()
            .createQuery(EscmProposalMgmt.class, "as e where e.proposalno =:proposalno");
        proposalQuery.setNamedParameter("proposalno", documentnoList.get(0));
        List<EscmProposalMgmt> minProposalList = proposalQuery.list();
        if (minProposalList.size() > 0) {
          EscmProposalMgmt minProposal = minProposalList.get(0);
          if (minProposal.getProposalappstatus().equals("APP")) {
            isMinProposalApproved = true;
          }
        }
      }
    } catch (OBException e) {
      log.error("Exception while checkMandatoryfields:" + e);
      e.printStackTrace();
      throw new OBException(e.getMessage());
    }
    return isMinProposalApproved;
  }
}
