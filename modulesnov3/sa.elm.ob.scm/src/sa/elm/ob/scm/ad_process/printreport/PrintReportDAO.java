package sa.elm.ob.scm.ad_process.printreport;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMProposalMgmtLetter;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.escmpurordercontletter;

public class PrintReportDAO {
  private static Logger log4j = Logger.getLogger(PrintReportDAO.class);

  private <T extends BaseOBObject> T getTableObject(Class<T> t, String strId) {
    return OBDal.getInstance().get(t, strId);
  }

  private long getSequenceNo(String tableName, String parentCol, String parentId) {
    long seq = 0;
    StringBuffer query = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      query = new StringBuffer();
      query.append("select COALESCE(MAX(LINE),0)+10 AS lineno FROM ");
      query.append(tableName);
      query.append(" where ").append(parentCol).append(" = ? ");
      st = OBDal.getInstance().getConnection().prepareStatement(query.toString());
      st.setString(1, parentId);
      rs = st.executeQuery();
      if (rs.next()) {
        seq = rs.getLong("lineno");
      }
    } catch (Exception e) {
      log4j.error("Exception in getSequenceNo ", e);
    }
    return seq;
  }

  public void updateMIRPrinted(String pintedBy, String mirId) {
    String hql = "UPDATE Escm_Material_Request set printed_date = now(), printedby = :printedby "
        + "WHERE escm_material_request_id = :mirId";
    Query updQuery = OBDal.getInstance().getSession().createQuery(hql);
    updQuery.setParameter("printedby", pintedBy);
    updQuery.setParameter("mirId", mirId);
    updQuery.executeUpdate();
  }

  public String getUserName(String id) {
    OBQuery<User> query = OBDal.getInstance().createQuery(User.class, "id = :id");
    query.setNamedParameter("id", id);
    return query.uniqueResult().getName();
  }

  public MaterialIssueRequest getMIR(String id) {
    OBQuery<MaterialIssueRequest> query = OBDal.getInstance()
        .createQuery(MaterialIssueRequest.class, "id = :id");
    query.setNamedParameter("id", id);
    return query.uniqueResult();
  }

  public String getPrintedDate(String id) {
    OBQuery<MaterialIssueRequest> query = OBDal.getInstance()
        .createQuery(MaterialIssueRequest.class, "id = :id");
    query.setNamedParameter("id", id);
    Date date = query.uniqueResult().getPrintedDate();
    SimpleDateFormat printedDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    if (date != null) {
      return printedDateFormat.format(date);
    } else {
      return null;
    }
  }

  /**
   * 
   * @param clientId
   * @param sortColName
   * @param sortType
   * @param rows
   * @param page
   * @param searchFlag
   * @param type
   * @return lkUpLs
   */
  @SuppressWarnings("rawtypes")
  public List<PrintReportVO> getAwardLetterandReminderLetterCopiesLookups(String clientId,
      String sortColName, String sortType, int rows, int pageNo, String searchFlag, String type) {
    List<PrintReportVO> lkUpLs = null;
    PrintReportVO rtnVO = null;
    SQLQuery lkupQuery = null;
    StringBuffer query = null;
    int count = 0, totalPages = 0, start = 0;
    int page = pageNo;
    try {
      OBContext.setAdminMode(true);
      query = new StringBuffer();

      query.append(
          "select lkupln.value, lkupln.line as seqno, lkupln.description, lkupln.name, lkupln.escm_deflookups_typeln_id as lookuplnid "
              + " from escm_deflookups_type lkup left join escm_deflookups_typeln lkupln on lkup.escm_deflookups_type_id=lkupln.escm_deflookups_type_id "
              + " where lkup.reference='" + type + "' and lkup.ad_client_id='" + clientId + "' ");

      if (sortColName != null)
        query.append(" order by " + sortColName + " " + sortType);

      lkupQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
      log4j.info("getlookups->" + query.toString());
      if (lkupQuery != null) {
        count = lkupQuery.list().size();
        if (count > 0) {
          totalPages = (int) (count) / rows;

          if ((int) (count) % rows > 0)
            totalPages = totalPages + 1;
          start = (page - 1) * rows;

          if (page > totalPages) {
            page = totalPages;
            start = (page - 1) * rows;
          }
        } else {
          totalPages = 0;
          page = 0;
        }
        lkupQuery.setFirstResult(start);
        lkupQuery.setMaxResults(rows);
        lkUpLs = new ArrayList<PrintReportVO>();
        if (lkupQuery.list().size() > 0) {
          for (Iterator iterator = lkupQuery.list().listIterator(); iterator.hasNext();) {
            Object[] objects = (Object[]) iterator.next();
            rtnVO = new PrintReportVO();
            rtnVO.setPage(page);
            rtnVO.setTotalPages(totalPages);
            rtnVO.setCount(count);

            rtnVO.setSeqNo(objects[1] == null ? "" : objects[1].toString());
            rtnVO.setAwardLookUp(objects[3] == null ? "" : objects[3].toString());
            rtnVO.setAwardLookUpLnId(objects[4] == null ? "" : objects[4].toString());
            rtnVO.setValue(objects[0] == null ? "" : objects[0].toString());
            log4j.info("id>" + rtnVO.getAwardLookUpLnId() + "," + rtnVO.getSeqNo());
            lkUpLs.add(rtnVO);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getAwardLetterandReminderLetterCopiesLookups ", e);
    } finally {
      OBContext.restorePreviousMode();
      OBDal.getInstance().getSession().clear();
    }
    return lkUpLs;
  }

  /**
   * 
   * @param ClientId
   * @return ReminderSubjectlist
   */
  public List<ESCMDefLookupsTypeLn> getReminderSubjectlist(String ClientId) {
    OBQuery<ESCMDefLookupsTypeLn> ReminderSubjectlist = null;
    try {
      ReminderSubjectlist = OBDal.getInstance().createQuery(ESCMDefLookupsTypeLn.class,
          "as e where e.client.id='" + ClientId + "' and e.escmDeflookupsType.reference='RLS'");
      ReminderSubjectlist.setFilterOnReadableOrganization(false);
    } catch (Exception e) {
      log4j.debug("Exception getReminderSubjectlist :" + e);
    }
    return ReminderSubjectlist.list();
  }

  /**
   * 
   * @param ProposalMgmtId
   * @return letter tab count
   */
  public static int getlettercount(String ProposalMgmtId) {
    OBQuery<ESCMProposalMgmtLetter> lettercount = null;
    int count = 0;
    try {
      lettercount = OBDal.getInstance().createQuery(ESCMProposalMgmtLetter.class,
          "as e where e.escmProposalmgmt.id='" + ProposalMgmtId + "'");
      count = lettercount.list().size();
    } catch (Exception e) {
      log4j.error("Exception in getlettercount ", e);
    }
    return count;
  }

  /**
   * 
   * @param userId
   * @param clientId
   * @param orgId
   * @param propMgmtId
   * @param reportName
   */
  public void insertLetter(String userId, String clientId, String orgId, String propMgmtId,
      String reportName, String type, String sequence) {
    StringBuffer query = null;
    String ltrId = null, dmsId = "";
    Date dmsDate = null;
    Client client = null;
    Organization organization = null;
    User user = null;
    ESCMProposalMgmtLetter ltrTb = null;
    EscmProposalMgmt propMgmt = null;
    PreparedStatement st = null;
    ResultSet rs = null;

    try {
      OBContext.setAdminMode();
      // if (type.equals("AWDLTR")) {
      query = new StringBuffer();
      query.append(
          "select escm_proposalmgmt_letter_id, dms_id, dms_date from escm_proposalmgmt_letter where escm_proposalmgmt_id=? and lettername ilike '%AwardLetter%' ");
      st = OBDal.getInstance().getConnection().prepareStatement(query.toString());
      st.setString(1, propMgmtId);
      rs = st.executeQuery();
      if (rs.next()) {
        ltrId = rs.getString("escm_proposalmgmt_letter_id");
        dmsId = rs.getString("dms_id");
        dmsDate = rs.getDate("dms_date");
      }
      // }
      if (ltrId != null && type.equals("AWDLTR")) {
        String hql = "UPDATE ESCM_ProposalMgmtLetter set printDateH = now(), letterName = :letterName "
            + "WHERE escm_proposalmgmt_id = :propMgmtId and id= :ltrId";
        Query updQuery = OBDal.getInstance().getSession().createQuery(hql);
        updQuery.setParameter("letterName", reportName);
        updQuery.setParameter("propMgmtId", propMgmtId);
        updQuery.setParameter("ltrId", ltrId);
        updQuery.executeUpdate();
      } else {
        client = getTableObject(Client.class, clientId);
        organization = getTableObject(Organization.class, orgId);
        user = getTableObject(User.class, userId);
        propMgmt = getTableObject(EscmProposalMgmt.class, propMgmtId);

        ltrTb = OBProvider.getInstance().get(ESCMProposalMgmtLetter.class);
        ltrTb.setClient(client);
        ltrTb.setOrganization(organization);
        ltrTb.setCreatedBy(user);
        ltrTb.setUpdatedBy(user);
        ltrTb.setLineNo(
            getSequenceNo("escm_proposalmgmt_letter", "escm_proposalmgmt_id", propMgmtId));
        // set new Spec No
        // sequence = Utility.getTransactionSequence(orgId, "PMGLTR");
        ltrTb.setLetterNo(sequence);
        ltrTb.setLetterName(reportName);
        ltrTb.setPrintDateH(new Date());
        ltrTb.setEscmProposalmgmt(propMgmt);
        if (type.equals("REMLTR")) {
          ltrTb.setDMSID(dmsId);
          ltrTb.setDMSDateH(dmsDate);
        }
        OBDal.getInstance().save(ltrTb);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      log4j.error(" Exception in insertLetter ", e);
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in insertLetter() ", e);
      }
      OBContext.restorePreviousMode();
      OBDal.getInstance().getSession().clear();
    }
  }

  /**
   * 
   * @param userId
   * @param clientId
   * @param orgId
   * @param poId
   * @param reportName
   */
  public void insertPOLetter(String userId, String clientId, String orgId, String poId,
      String reportName, String type, String sequence) {
    Client client = null;
    Organization organization = null;
    User user = null;
    escmpurordercontletter ltrTb = null;
    Order ord = null;
    StringBuffer query = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    String ltrId = null;

    try {
      OBContext.setAdminMode();
      query = new StringBuffer();
      query.append(
          "select escm_purordercont_letter_id from escm_purordercont_letter where c_order_id =? and lettername ilike '%Contract Execution Order%' ");
      st = OBDal.getInstance().getConnection().prepareStatement(query.toString());
      st.setString(1, poId);
      rs = st.executeQuery();
      if (rs.next()) {
        ltrId = rs.getString("escm_purordercont_letter_id");
      }
      // }
      if (ltrId != null && type.equals("CONEXEC")) {
        String hql = "update escm_purordercont_letter set print_date =now() where escm_purordercont_letter_id =:ltrId";
        Query updQuery = OBDal.getInstance().getSession().createQuery(hql);
        updQuery.setParameter("ltrId", ltrId);
        updQuery.executeUpdate();
      } else {
        client = getTableObject(Client.class, clientId);
        organization = getTableObject(Organization.class, orgId);
        user = getTableObject(User.class, userId);
        ord = getTableObject(Order.class, poId);

        ltrTb = OBProvider.getInstance().get(escmpurordercontletter.class);
        ltrTb.setClient(client);
        ltrTb.setOrganization(organization);
        ltrTb.setCreatedBy(user);
        ltrTb.setUpdatedBy(user);
        ltrTb.setLineNo(getSequenceNo("escm_purordercont_letter", "c_order_id", poId));
        ltrTb.setLetterNo(sequence);
        ltrTb.setLetterName(reportName);
        ltrTb.setPrintDateH(new Date());
        ltrTb.setSalesOrder(ord);

        OBDal.getInstance().save(ltrTb);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      log4j.error(" Exception in insertLetter ", e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (st != null) {
          st.close();
        }
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }
  }

  /*
   * public PrintReportVO getMembers(String committeId, String MemberType, String MemberTypeCode) {
   * PrintReportVO presVO = null; SQLQuery presQuery = null; StringBuffer query = null;
   * 
   * try { OBContext.setAdminMode(true); query = new StringBuffer();
   * 
   * query.append("select string_agg(emp_name, ', ') as emp_name, " +
   * "regexp_replace(repeat('------------------,', cast(count(emp_name) as integer)), ',+$', '') as signature, "
   * + "lktypln.name, value " +
   * "from escm_committee_members cmem left join escm_deflookups_typeln lktypln on lktypln.escm_deflookups_typeln_id=cmem.membertype "
   * +
   * "left join escm_deflookups_type lktyp on lktyp.escm_deflookups_type_id=lktypln.escm_deflookups_type_id "
   * + "where escm_committee_id='" + committeId + "' and lktyp.reference='CMT' and (name='" +
   * MemberType + "' or value='" + MemberTypeCode + "') group by lktypln.name, value");
   * 
   * presQuery = OBDal.getInstance().getSession().createSQLQuery(query.toString());
   * log4j.info("getlookups->" + query.toString()); if (presQuery != null) { if
   * (presQuery.list().size() > 0) { presVO = new PrintReportVO(); for (Iterator iterator =
   * presQuery.list().listIterator(); iterator.hasNext();) { Object[] objects = (Object[])
   * iterator.next(); presVO.setMemberName(objects[0] == null ? "" : objects[0].toString());
   * presVO.setMemberSignature(objects[1] == null ? "" : objects[1].toString()); } } } } catch
   * (Exception e) { log4j.error("Exception in getPresident ", e); } finally {
   * OBContext.restorePreviousMode(); OBDal.getInstance().getSession().clear(); } return presVO; }
   */

  public String getBgAmountInWords(String orderId) {
    String bgAmtInWords = "";
    try {
      OBQuery<Escmbankguaranteedetail> bgDetail = OBDal.getInstance().createQuery(
          Escmbankguaranteedetail.class, "as e where salesOrder.id='" + orderId + "' ");

      if (bgDetail != null && bgDetail.list().size() > 0) {
        for (Escmbankguaranteedetail bgamt : bgDetail.list()) {
          log4j.debug("bgamt>" + bgamt.getBgamount());
          bgAmtInWords += "," + sa.elm.ob.utility.ad_reports.NumberToArabic
              .convertToArabic(bgamt.getBgamount(), "SAR");
        }
      }
      bgAmtInWords = bgAmtInWords.replaceFirst(",", "");
    } catch (Exception e) {
      log4j.error("Exception in getBgAmountInWords ", e);
    } finally {
      OBContext.restorePreviousMode();
      OBDal.getInstance().getSession().clear();
    }
    return bgAmtInWords;
  }
}
