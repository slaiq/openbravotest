package sa.elm.ob.hcm.ad_process.EmployeeTransfer;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.hcm.EHCMEmpTransfer;
import sa.elm.ob.hcm.EhcmPosition;
import sa.elm.ob.hcm.EmploymentInfo;
import sa.elm.ob.hcm.ad_process.DecisionTypeConstants;
import sa.elm.ob.hcm.ad_process.EmployeePromotion.EmployeePromotionHandlerDAO;
import sa.elm.ob.hcm.ad_process.assignedOrReleasePosition.AssingedOrReleaseEmpInPositionDAO;
import sa.elm.ob.hcm.ad_process.assignedOrReleasePosition.AssingedOrReleaseEmpInPositionDAOImpl;
import sa.elm.ob.hcm.util.Utility;
import sa.elm.ob.hcm.util.UtilityDAO;

public class EmpTransferIssueDecision implements Process {
  private static final Logger log = Logger.getLogger(EmpTransferIssueDecision.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Issue the position");

    final String transferId = (String) bundle.getParams().get("Ehcm_Emp_Transfer_ID").toString();
    EHCMEmpTransfer transfer = OBDal.getInstance().get(EHCMEmpTransfer.class, transferId);
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    EmploymentInfo info = null;
    String decisionType = transfer.getDecisionType();
    log.debug("transferId:" + transferId);
    String lang = vars.getLanguage();
    Boolean chkPositionAvailableOrNot = false;
    Boolean update = false;
    String clientId = transfer.getClient().getId();
    Boolean isoverlapping = false;
    Boolean checkOriginalDecisionNoIsInActInEmpInfo = false;
    String employeeId = transfer.getEhcmEmpPerinfo().getId();
    AssingedOrReleaseEmpInPositionDAO assingedOrReleaseEmpInPositionDAO = new AssingedOrReleaseEmpInPositionDAOImpl();
    String recentInfoDepartmentId = "";
    try {
      OBContext.setAdminMode(true);
      log.debug("isSueDecision:" + transfer.isSueDecision());
      log.debug("getDecisionType:" + transfer.getDecisionType());
      // To check whether Decision number is already issued
      if (transfer.getDecisionType().equals("UP") || transfer.getDecisionType().equals("CA")) {
        employeeId = transfer.getEhcmEmpPerinfo().getId();
        checkOriginalDecisionNoIsInActInEmpInfo = UtilityDAO
            .checkOriginalDecisionNoIsInActInEmpInfo(null, null, transfer, null, null, null, null,
                null, employeeId);
        if (checkOriginalDecisionNoIsInActInEmpInfo) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("EHCM_Already_Issued"));
          bundle.setResult(obError);
          return;
        }
      }
      // check whether the employee is suspended or not
      if (transfer.getEhcmEmpPerinfo().getEmploymentStatus()
          .equals(DecisionTypeConstants.EMPLOYMENTSTATUS_SUSPENDED)) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("EHCM_emplo_suspend"));
        bundle.setResult(obError);
        return;
      }
      // If the inside deparment has changed
      if (transfer.getTransferType().equals("ID")) {
        recentInfoDepartmentId = EmployeePromotionHandlerDAO
            .getRecentEmpInfo(transfer.getEhcmEmpPerinfo().getId());
        if (!recentInfoDepartmentId.equals(transfer.getNewDepartmentCode().getId())) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("Ehcm_EmpDet_Cdn"));
          bundle.setResult(obError);
          return;

        }

      }
      // If the outside deparment has changed
      if (transfer.getTransferType().equals("OD")) {
        recentInfoDepartmentId = EmployeePromotionHandlerDAO
            .getRecentEmpInfo(transfer.getEhcmEmpPerinfo().getId());
        if (recentInfoDepartmentId.equals(transfer.getNewDepartmentCode().getId())) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("Ehcm_EmpDet_Cdn"));
          bundle.setResult(obError);
          return;

        }

      }

      // check start date is overlapping with recent transaction of employee
      if (transfer.getDecisionType().equals(DecisionTypeConstants.DECISION_TYPE_CREATE)) {
        isoverlapping = UtilityDAO.chkOverlapDecisionStartdate(employeeId, transfer.getStartDate(),
            clientId);
        if (isoverlapping) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("EHCM_Date_Overlapping"));
          bundle.setResult(obError);
          return;
        }

      }

      // checking position is available or not
      if (!transfer.getDecisionType().equals("CA")) {
        EhcmPosition position = OBDal.getInstance().get(EhcmPosition.class,
            transfer.getNEWEhcmPosition().getId());
        if (!position.getId().equals(transfer.getPosition().getId())) {
          chkPositionAvailableOrNot = assingedOrReleaseEmpInPositionDAO.chkPositionAvailableOrNot(
              transfer.getEhcmEmpPerinfo(), position, transfer.getStartDate(),
              transfer.getEndDate(), transfer.getDecisionType(), false);
          if (chkPositionAvailableOrNot) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("EHCM_PosNotAvailable"));
            bundle.setResult(obError);
            return;
          }
        }
      } else {/*
               * EhcmPosition currentPos = assingedOrReleaseEmpInPositionDAO.getRecentPosition(
               * transfer.getEhcmEmpPerinfo(), null, transfer.getOriginalDecisionsNo(), null);
               */

        EmploymentInfo recentEmployeInfo = assingedOrReleaseEmpInPositionDAO
            .getRecentEmploymentInfo(transfer.getEhcmEmpPerinfo(), null,
                transfer.getOriginalDecisionsNo(), null);

        if (recentEmployeInfo != null && recentEmployeInfo.getPosition() != null
            && !recentEmployeInfo.getPosition().getId().equals(transfer.getPosition().getId())) {
          chkPositionAvailableOrNot = assingedOrReleaseEmpInPositionDAO.chkPositionAvailableOrNot(
              transfer.getEhcmEmpPerinfo(), recentEmployeInfo.getPosition(),
              transfer.getStartDate(), null, transfer.getDecisionType(), false);
          if (chkPositionAvailableOrNot) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("EHCM_PosNotAvailable"));
            bundle.setResult(obError);
            return;
          }
        }
      }

      if (transfer.getDecisionType().equals("CA")) {

        // dont allow to cancel the transfer if the employee has promoted
        OBQuery<EmploymentInfo> chkemphavepromt = OBDal.getInstance().createQuery(
            EmploymentInfo.class,
            " as e where e.ehcmEmpPerinfo.id=:empId and e.enabled='Y' and e.ehcmEmpPromotion is not null order by e.creationDate desc ");
        chkemphavepromt.setNamedParameter("empId", employeeId);
        log.debug("chkemphavepromt:" + chkemphavepromt.list().size());
        if (chkemphavepromt.list().size() > 0) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("EHCM_EmpTranCan_Error"));
          bundle.setResult(obError);
          return;
        }
      }

      // check Issued or not
      if (!transfer.isSueDecision()) {
        // update status as Issued and set decision date for all cases
        transfer.setSueDecision(true);
        transfer.setDecisionDate(new Date());
        transfer.setDecisionStatus("I");
        transfer.getEhcmEmpPerinfo().setEmploymentStatus("AC");
        OBDal.getInstance().save(transfer);
        OBDal.getInstance().flush();

        if (transfer.getDecisionType().equals("CR") && !transfer.isJoinworkreq()) {

          info = Utility.getActiveEmployInfo(employeeId);
          EmpTransferIssueDecisionDAO.insertEmploymentInfo(transfer, info, vars, decisionType, lang,
              null, null);

        } else if (transfer.getDecisionType().equals("UP")
            && transfer.getOriginalDecisionsNo() != null
            && !transfer.getOriginalDecisionsNo().isJoinworkreq()) {
          info = Utility.getActiveEmployInfo(employeeId);
          EmpTransferIssueDecisionDAO.insertEmploymentInfo(transfer, info, vars, decisionType, lang,
              null, null);
        } else if (transfer.getDecisionType().equals("CA")
            && transfer.getOriginalDecisionsNo() != null
            && !transfer.getOriginalDecisionsNo().isJoinworkreq()) {

          // update the acive flag='Y' and enddate is null for recently update record
          OBQuery<EmploymentInfo> originalemp = OBDal.getInstance().createQuery(
              EmploymentInfo.class,
              " ehcmEmpPerinfo.id=:empId and enabled='N' order by creationDate desc ");
          originalemp.setNamedParameter("empId", employeeId);

          originalemp.setMaxResult(1);
          if (originalemp.list().size() > 0) {
            info = originalemp.list().get(0);
          }
          EmpTransferIssueDecisionDAO.updateOldEmpTransferActCancel(transfer, info, vars);
        }
      }
      obError.setType("Success");
      obError.setTitle("Success");
      obError.setMessage(OBMessageUtils.messageBD("Ehcm_Submit_Process"));
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    }

    catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), t.getMessage());
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
