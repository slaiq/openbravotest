package sa.elm.ob.scm.ad_process.Committee.dao;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMCommitteeMembers;

/**
 * 
 * @author qualian-Kousalya
 * 
 */
// process file
public class CommitteeDAO {
  private static final Logger log = LoggerFactory.getLogger(CommitteeDAO.class);

  /**
   * Get President
   * 
   * @param committeeId
   * @return boolean
   */
  public static Boolean getPresident(String committeeId) {
    Boolean isPresident = true;
    List<ESCMCommitteeMembers> commiteeMemList = null;
    List<Object> parametersList = new ArrayList<Object>();
    String query = " as e where e.escmCommittee.id=? and e.memberType in ( select ln.id from ESCM_DefLookups_TypeLn ln "
        + "  where ln.escmDeflookupsType.reference='CMT' and ln.searchKey='P')";
    parametersList.add(committeeId);
    try {
      OBContext.setAdminMode();
      OBQuery<ESCMCommitteeMembers> prescount = OBDal.getInstance().createQuery(
          ESCMCommitteeMembers.class, query, parametersList);
      commiteeMemList = prescount.list();

      if ((commiteeMemList.size() > 1) || (commiteeMemList.size() < 1)) {
        isPresident = false;
      }
    } catch (OBException e) {
      log.error("Exception while getPresident:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isPresident;
  }

  /**
   * Get Members
   * 
   * @param committeeId
   * @return boolean
   */
  public static Boolean getMembers(String committeeId) {
    Boolean isMembers = true;
    List<ESCMCommitteeMembers> commiteeMemList = null;
    List<Object> parametersList = new ArrayList<Object>();
    String query = " as e where e.escmCommittee.id=? and e.memberType in ( select ln.id from ESCM_DefLookups_TypeLn ln "
        + "  where ln.escmDeflookupsType.reference='CMT'  and ln.searchKey='M')";
    parametersList.add(committeeId);
    try {
      OBContext.setAdminMode();
      OBQuery<ESCMCommitteeMembers> membcount = OBDal.getInstance().createQuery(
          ESCMCommitteeMembers.class, query, parametersList);
      commiteeMemList = membcount.list();
      if (commiteeMemList.size() < 3) {
        isMembers = false;
      }
    } catch (OBException e) {
      log.error("Exception while getMembers:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isMembers;
  }

  /**
   * Get President Repl.
   * 
   * @param committeeId
   * @return boolean
   */
  public static Boolean getPresidentRepl(String committeeId) {
    Boolean isPresRepl = true;
    List<ESCMCommitteeMembers> commiteeMemList = null;
    List<Object> parametersList = new ArrayList<Object>();
    String query = " as e where e.escmCommittee.id=? and e.memberType in ( select ln.id from ESCM_DefLookups_TypeLn ln "
        + "  where ln.escmDeflookupsType.reference='CMT' and ln.searchKey='PR')";
    parametersList.add(committeeId);
    try {
      OBContext.setAdminMode();

      OBQuery<ESCMCommitteeMembers> presReplCount = OBDal.getInstance().createQuery(
          ESCMCommitteeMembers.class, query, parametersList);
      commiteeMemList = presReplCount.list();
      if (commiteeMemList.size() > 1) {
        isPresRepl = false;
      }
    } catch (OBException e) {
      log.error("Exception while getPresidentRepl:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isPresRepl;
  }

  /**
   * Get Financial Ctrl
   * 
   * @param committeeId
   * @return boolean
   */
  public static Boolean getFinanceCtrl(String committeeId) {
    Boolean isFinanceCtrl = true;
    List<Object> parametersList = new ArrayList<Object>();
    String query = " as e where e.escmCommittee.id=? and e.memberType in ( select ln.id from ESCM_DefLookups_TypeLn ln  "
        + " where ln.escmDeflookupsType.reference='CMT'  and ln.searchKey='FC')";
    parametersList.add(committeeId);
    try {
      OBContext.setAdminMode();
      OBQuery<ESCMCommitteeMembers> FinancialCtrl = OBDal.getInstance().createQuery(
          ESCMCommitteeMembers.class, query, parametersList);
      if ((FinancialCtrl.list().size() > 1) || (FinancialCtrl.list().size() < 1)) {
        isFinanceCtrl = false;
      }
    } catch (OBException e) {
      log.error("Exception while getFinanceCtrl:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isFinanceCtrl;
  }

  /**
   * Get Committee Members
   * 
   * @param committeeId
   * @return boolean
   */
  public static Boolean getCommitteMemCount(String committeeId) {
    Boolean isLine = true;
    List<Object> parametersList = new ArrayList<Object>();
    String query = " as e where e.escmCommittee.id=? ";
    parametersList.add(committeeId);
    try {
      OBContext.setAdminMode();
      OBQuery<ESCMCommitteeMembers> linecount = OBDal.getInstance().createQuery(
          ESCMCommitteeMembers.class, query, parametersList);

      if ((linecount.list().size() < 1)) {
        isLine = false;
      }
    } catch (OBException e) {
      log.error("Exception while getFinanceCtrl:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return isLine;
  }
}