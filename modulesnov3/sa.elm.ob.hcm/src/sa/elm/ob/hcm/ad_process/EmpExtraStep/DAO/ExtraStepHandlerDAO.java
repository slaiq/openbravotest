package sa.elm.ob.hcm.ad_process.EmpExtraStep.DAO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.hcm.EhcmEmpPerInfo;
import sa.elm.ob.hcm.EhcmEmployeeExtraStep;
import sa.elm.ob.hcm.EmploymentInfo;
import sa.elm.ob.hcm.ehcmpayscaleline;
import sa.elm.ob.hcm.util.UtilityDAO;

/**
 * 
 * @author Poongodi 06/02/2018
 *
 */

public class ExtraStepHandlerDAO {
  private static final Logger LOG = LoggerFactory.getLogger(ExtraStepHandlerDAO.class);

  /**
   * 
   * @param extraStepProcess
   * @param vars
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static int insertLineinEmploymentInfo(EhcmEmployeeExtraStep extraStepProcess,
      VariablesSecureApp vars) {
    EmploymentInfo info = null;
    List<EmploymentInfo> empInfoList = new ArrayList<EmploymentInfo>();
    int count = 0;
    boolean isExtraStep = true;
    int oneDay = 1 * 24 * 3600 * 1000;
    try {
      OBContext.setAdminMode();

      // get employment Information by passing the corresponding employee id.
      OBQuery<EmploymentInfo> empInfo = OBDal.getInstance().createQuery(EmploymentInfo.class,
          " as e where ehcmEmpPerinfo.id=:employeeId and e.enabled='Y' and e.ehcmEmpExtrastep.id is not null order by e.creationDate desc");
      empInfo.setNamedParameter("employeeId", extraStepProcess.getEhcmEmpPerinfo().getId());
      empInfo.setMaxResult(1);
      empInfoList = empInfo.list();
      if (empInfoList.size() > 0) {
        info = empInfoList.get(0);
      }
      // on create case
      if (extraStepProcess.getDecisionType().equals("CR")) {
        empInfo = OBDal.getInstance().createQuery(EmploymentInfo.class,
            " as e where ehcmEmpPerinfo.id=:employeeId and e.enabled='Y' and (e.ehcmEmpExtrastep.id is null) order by e.creationDate desc");
        empInfo.setNamedParameter("employeeId", extraStepProcess.getEhcmEmpPerinfo().getId());
        empInfo.setMaxResult(1);
        empInfoList = empInfo.list();
        if (empInfoList.size() > 0) {
          info = empInfoList.get(0);
        }
      }

      if (extraStepProcess.getDecisionType().equals("CR")
          || extraStepProcess.getDecisionType().equals("UP")) {
        EmploymentInfo employInfo = null;
        if (extraStepProcess.getDecisionType().equals("CR")) {
          employInfo = OBProvider.getInstance().get(EmploymentInfo.class);
        } else {
          employInfo = info;
        }

        if (extraStepProcess.getDecisionType().equals("UP"))
          employInfo.setChangereason("ES");
        else
          employInfo.setChangereason("ES");
        UtilityDAO.insertActEmplymntInfoDetailsInIssueDecision(extraStepProcess.getEhcmEmpPerinfo(),
            employInfo, isExtraStep, false);

        ehcmpayscaleline line = OBDal.getInstance().get(ehcmpayscaleline.class,
            extraStepProcess.getNewgradepoint().getId());

        employInfo.setEhcmPayscale(line.getEhcmPayscale());
        employInfo.setEhcmPayscaleline(line);

        employInfo.setStartDate(extraStepProcess.getStartDate());
        employInfo.setEhcmEmpExtrastep(extraStepProcess);
        employInfo.setDecisionNo(extraStepProcess.getDecisionNo());
        employInfo.setDecisionDate(extraStepProcess.getDecisionDate());

        // Update the enddate for old hiring record.
        if (extraStepProcess.getDecisionType().equals("CR")) {
          Date dateBefore = null;
          OBQuery<EmploymentInfo> empInfoold = OBDal.getInstance().createQuery(EmploymentInfo.class,
              " as e where ehcmEmpPerinfo.id=:employeeId  and e.enabled='Y' order by e.creationDate desc");
          empInfoold.setNamedParameter("employeeId", extraStepProcess.getEhcmEmpPerinfo().getId());
          empInfoold.setMaxResult(1);
          EmploymentInfo empinfo = empInfoold.list().get(0);
          empinfo.setUpdated(new java.util.Date());
          empinfo.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));

          // if (empinfo.getEndDate() == null) {
          Date startdate = empinfo.getStartDate();
          dateBefore = new Date(extraStepProcess.getStartDate().getTime() - oneDay);

          if (startdate.compareTo(extraStepProcess.getStartDate()) == 0)
            empinfo.setEndDate(empinfo.getStartDate());
          else
            empinfo.setEndDate(dateBefore);

          // }
          empinfo.setAlertStatus("INACT");
          empinfo.setEnabled(false);

          OBDal.getInstance().save(empinfo);
          OBDal.getInstance().flush();

        }
        OBDal.getInstance().save(employInfo);
        OBDal.getInstance().flush();

        // Update Case
        if (extraStepProcess.getDecisionType().equals("UP")) {
          // update the endate and active flag for old hiring record.
          OBQuery<EmploymentInfo> empInfoold = OBDal.getInstance().createQuery(EmploymentInfo.class,
              " ehcmEmpPerinfo.id=:employeeId  and id not in ('" + employInfo.getId()
                  + "')  order by creationDate desc ");
          empInfoold.setNamedParameter("employeeId", extraStepProcess.getEhcmEmpPerinfo().getId());
          empInfoold.setMaxResult(1);
          empInfoList = empInfoold.list();
          if (empInfoList.size() > 0) {
            EmploymentInfo empinfo = empInfoList.get(0);
            empinfo.setUpdated(new java.util.Date());
            empinfo.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
            Date dateBefore = null;
            dateBefore = new Date(extraStepProcess.getStartDate().getTime() - oneDay);
            empinfo.setEndDate(dateBefore);
            /*
             * Date dateBefore = new Date( extraStepProcess.getStartDate().getTime() - 1 * 24 * 3600
             * * 1000);
             * 
             * if (startdate.compareTo(extraStepProcess.getStartDate()) == 0)
             * empinfo.setEndDate(empinfo.getStartDate()); else empinfo.setEndDate(dateBefore);
             * 
             * }
             */
            // update old extrastep as inactive
            EhcmEmployeeExtraStep oldExtraStep = extraStepProcess.getOriginalDecisionNo();
            oldExtraStep.setEnabled(false);
            OBDal.getInstance().save(oldExtraStep);
            OBDal.getInstance().flush();
          }

          EhcmEmpPerInfo person = OBDal.getInstance().get(EhcmEmpPerInfo.class,
              extraStepProcess.getEhcmEmpPerinfo().getId());
          if (extraStepProcess.getDecisionType().equals("CO")) {
            person.setEmploymentStatus("AC");
          } else {
            person.setEmploymentStatus("ES");
          }
          OBDal.getInstance().save(person);
          OBDal.getInstance().flush();

        }
      }
      // cancel case
      else if (extraStepProcess.getDecisionType().equals("CA")) {
        // update the acive flag='Y' and enddate is null for recently update record
        OBQuery<EmploymentInfo> originalemp = OBDal.getInstance().createQuery(EmploymentInfo.class,
            " ehcmEmpPerinfo.id=:employeeId  and (ehcmEmpExtrastep.id not in ('"
                + extraStepProcess.getOriginalDecisionNo().getId()
                + "') or ehcmEmpExtrastep.id is null) order by creationDate desc ");
        originalemp.setNamedParameter("employeeId", extraStepProcess.getEhcmEmpPerinfo().getId());
        originalemp.setMaxResult(1);
        LOG.debug(originalemp.getWhereAndOrderBy());
        empInfoList = originalemp.list();
        if (empInfoList.size() > 0) {
          EmploymentInfo empinfo = empInfoList.get(0);
          empinfo.setUpdated(new java.util.Date());
          empinfo.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
          // empinfo.setEndDate(null);

          if (empinfo.getChangereason().equals("H") || empinfo.getEhcmEmpPromotion() != null
              || empinfo.getEhcmEmpSuspension() != null
              || empinfo.getChangereason().equals("JWRSEC")) {
            empinfo.setEndDate(null);
          } else if (empinfo.getEhcmEmpTransfer() != null) {
            if (empinfo.getEhcmEmpTransfer().getEndDate() != null) {
              empinfo.setEndDate(empinfo.getEhcmEmpTransfer().getEndDate());
            } else {
              empinfo.setEndDate(null);
            }
          } else if (empinfo.getEhcmEmpTransferSelf() != null) {
            if (empinfo.getEhcmEmpTransferSelf().getEndDate() != null) {
              empinfo.setEndDate(empinfo.getEhcmEmpTransferSelf().getEndDate());
            } else {
              empinfo.setEndDate(null);
            }
          } else if (empinfo.getEhcmEmpSuspension() != null) {
            if (empinfo.getEhcmEmpSuspension().getEndDate() != null) {
              empinfo.setEndDate(empinfo.getEhcmEmpSuspension().getEndDate());
            } else {
              empinfo.setEndDate(null);
            }
          } else {
            empinfo.setEndDate(null);
          }
          empinfo.setEnabled(true);
          empinfo.setAlertStatus("ACT");
          OBDal.getInstance().save(empinfo);
          OBDal.getInstance().flush();

          // remove the recent record
          OBQuery<EmploymentInfo> employInfo1 = OBDal.getInstance().createQuery(
              EmploymentInfo.class,
              " ehcmEmpPerinfo.id=:employeeId  and enabled='Y'  and ehcmEmpExtrastep.id ='"
                  + extraStepProcess.getOriginalDecisionNo().getId()
                  + "' order by creationDate desc");
          employInfo1.setNamedParameter("employeeId", extraStepProcess.getEhcmEmpPerinfo().getId());
          employInfo1.setMaxResult(1);
          empInfoList = employInfo1.list();
          if (empInfoList.size() > 0) {
            EmploymentInfo empInfor = empInfoList.get(0);
            OBDal.getInstance().remove(empInfor);
            OBDal.getInstance().flush();
          }
        }
        EhcmEmpPerInfo person = OBDal.getInstance().get(EhcmEmpPerInfo.class,
            extraStepProcess.getEhcmEmpPerinfo().getId());

        if (extraStepProcess.getOriginalDecisionNo().getDecisionType().equals("EX")) {
          person.setEmploymentStatus("SE");
        } else {
          person.setEmploymentStatus("AC");
        }
        OBDal.getInstance().save(person);
        OBDal.getInstance().flush();

        // update old extrastep as inactive
        EhcmEmployeeExtraStep oldExtraStep = extraStepProcess.getOriginalDecisionNo();
        oldExtraStep.setEnabled(false);
        OBDal.getInstance().save(oldExtraStep);
        OBDal.getInstance().flush();

        extraStepProcess.setEnabled(false);
        OBDal.getInstance().save(extraStepProcess);
        OBDal.getInstance().flush();
      }

      count = 1;

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception while inserting lines in employment tab  : ", e, e);
      }
      OBDal.getInstance().rollbackAndClose();

    } finally {
      OBContext.restorePreviousMode();
    }
    return count;

  }

  public static String getRecentEmpInfo(String employeeId) {
    List<EmploymentInfo> infoList = new ArrayList<EmploymentInfo>();
    String payscaleLineId = "";
    try {
      OBQuery<EmploymentInfo> employInfo = OBDal.getInstance().createQuery(EmploymentInfo.class,
          " ehcmEmpPerinfo.id=:employeeId   and enabled='Y'  order by creationDate desc ");
      employInfo.setNamedParameter("employeeId", employeeId);
      employInfo.setMaxResult(1);
      infoList = employInfo.list();
      if (infoList.size() > 0) {
        EmploymentInfo empinfo = infoList.get(0);
        if (empinfo.getEhcmPayscaleline() != null) {
          payscaleLineId = empinfo.getEhcmPayscaleline().getId();
        }

      }

    } catch (Exception e) {
      LOG.error("Exception in getRecentEmpInfo ", e);
    }
    return payscaleLineId;

  }
}