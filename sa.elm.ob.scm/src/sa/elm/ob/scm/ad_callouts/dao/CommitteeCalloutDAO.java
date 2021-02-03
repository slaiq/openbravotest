package sa.elm.ob.scm.ad_callouts.dao;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
import org.openbravo.model.common.enterprise.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMCommittee;
import sa.elm.ob.utility.util.Utility;

public class CommitteeCalloutDAO {
  private static final Logger log4j = LoggerFactory.getLogger(CommitteeCalloutDAO.class);

  /**
   * Get departmentID from departmentcode.
   * 
   * @param deptCode
   * @return DeptID
   */
  public static String getDepartmentId(String deptCode) {
    String DeptID = "";

    try {
      OBContext.setAdminMode();
      List<Organization> orgList = new ArrayList<Organization>();
      if (deptCode != null && !deptCode.equals("")) {
        OBQuery<Organization> org = OBDal.getInstance().createQuery(Organization.class,
            " searchKey =:dept and ehcmOrgtyp.id in (select e.id from EHCM_org_type as e )");
        org.setNamedParameter("dept", deptCode);
        orgList = org.list();
        if (orgList.size() > 0) {
          DeptID = orgList.get(0).getId();
        }
      }
      return DeptID;

    } catch (OBException e) {
      log4j.error("Exception while get department id", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get hijri date
   * 
   * @param gregoriandate
   * @param clientId
   * @return String
   */
  public static String getOneDayMinusHijiriDate(String gregoriandate, String clientId) {
    Query query = null;
    String startdate = "";
    StringBuffer hqlQuery = null;
    try {
      OBContext.setAdminMode();
      hqlQuery = new StringBuffer();
      hqlQuery.append(
          " select hdt.hijriDate from EUT_HijiriDates hdt where hdt.hijriDate<:hdate order by hdt.hijriDate desc 1");

      query = OBDal.getInstance().getSession().createQuery(hqlQuery.toString());
      query.setParameter("hdate", gregoriandate);
      query.setMaxResults(1);
      log4j.debug("hqlQuery:" + hqlQuery.toString());
      if (query.list().size() > 0) {
        if (query.iterate().hasNext()) {
          startdate = query.iterate().next().toString();
          log4j.debug("startdate" + startdate);
          startdate = startdate.substring(6, 8) + "-" + startdate.substring(4, 6) + "-"
              + startdate.substring(0, 4);
          log4j.debug("startdate12" + startdate);
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getOneDayMinusHijiriDate", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return startdate;
  }

  /**
   * Get Effective From and To Date
   * 
   * @param commiteeId
   * @return JSONObject
   * 
   */
  public static JSONObject getDates(String commiteeId) {
    JSONObject json = null;
    String effectiveFrom = "";
    String effectiveTo = "";
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    List<Object> paramList = new ArrayList<Object>();
    String query = " as e where e.id=?";
    paramList.add(commiteeId);
    try {
      json = new JSONObject();
      OBContext.setAdminMode();
      OBQuery<ESCMCommittee> commQry = OBDal.getInstance().createQuery(ESCMCommittee.class, query,
          paramList);
      if (commQry.list().size() > 0) {
        ESCMCommittee comm = commQry.list().get(0);
        effectiveFrom = df.format(comm.getStartingDate());
        effectiveTo = df.format(comm.getEndDate());

        effectiveFrom = Utility.convertTohijriDate(effectiveFrom);
        effectiveTo = Utility.convertTohijriDate(effectiveTo);
      }
      json.put("effectiveFrom", effectiveFrom);
      json.put("effectiveTo", effectiveTo);
    } catch (OBException e) {
      log4j.error("Exception while getDates:" + e);
      throw new OBException(e.getMessage());
    } catch (JSONException ex) {
      log4j.error("Exception while getDates:" + ex);
      throw new OBException(ex.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return json;
  }
}
