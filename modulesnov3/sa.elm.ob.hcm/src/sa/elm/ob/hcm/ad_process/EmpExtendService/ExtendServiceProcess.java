package sa.elm.ob.hcm.ad_process.EmpExtendService;

import java.text.DateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.hcm.EhcmEmpPerInfo;
import sa.elm.ob.hcm.EhcmExtendService;
import sa.elm.ob.hcm.ad_process.DecisionTypeConstants;
import sa.elm.ob.hcm.ad_process.EmpExtendService.DAO.ExtendServiceHandlerDAO;
import sa.elm.ob.hcm.util.UtilityDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * @author poongodi on 08/02/2018
 */
public class ExtendServiceProcess implements Process {
  private static final Logger log = Logger.getLogger(ExtendServiceProcess.class);
  private final OBError obError = new OBError();

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    log.debug("Issue the EmpExtendService");

    final String extendServiceId = bundle.getParams().get("Ehcm_Extend_Service_ID").toString();
    EhcmExtendService extendServiceProcess = OBDal.getInstance().get(EhcmExtendService.class,
        extendServiceId);
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    int count = 0;
boolean checkOriginalDecisionNoIsInActInEmpInfo = false;
    String ClientId = extendServiceProcess.getClient().getId();
    DateFormat dateYearFormat = Utility.YearFormat;
    DateFormat dateFormat = Utility.dateFormat;

    boolean isissued = false;
    boolean decisionUsed = false;
    try {

      OBContext.setAdminMode(true);

      // To check whether Decision number is already issued
      if (extendServiceProcess.getDecisionType().equals("UP")
          || extendServiceProcess.getDecisionType().equals("CA")) {
        String employeeId = extendServiceProcess.getEmployee().getId();
        checkOriginalDecisionNoIsInActInEmpInfo = UtilityDAO
            .checkOriginalDecisionNoIsInActInEmpInfo(null, extendServiceProcess, null, null, null,
                null, null, null, employeeId);
        if (checkOriginalDecisionNoIsInActInEmpInfo) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("EHCM_Already_Issued"));
          bundle.setResult(obError);
          return;
        }
      }

      EhcmEmpPerInfo person = OBDal.getInstance().get(EhcmEmpPerInfo.class,
          extendServiceProcess.getEmployee().getId());
      // check whether the employee is suspended or not
      if (extendServiceProcess.getEmployee().getEmploymentStatus()
          .equals(DecisionTypeConstants.EMPLOYMENTSTATUS_SUSPENDED)) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(OBMessageUtils.messageBD("EHCM_emplo_suspend"));
        bundle.setResult(obError);
        return;
      }

      if (extendServiceProcess.getOriginalDecisionNo() != null) {
        decisionUsed = ExtendServiceHandlerDAO.chkOriginalDecisionUsed(
            extendServiceProcess.getEmployee().getId(), ClientId,
            extendServiceProcess.getOriginalDecisionNo().getId(), extendServiceId);
        if (decisionUsed) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("Ehcm_OriginalDec_Used"));
          bundle.setResult(obError);
          return;
        }
      }
      // check selected Original Decision No is issued or not
      if (!extendServiceProcess.getDecisionType().equals("CR")) {
        isissued = ExtendServiceHandlerDAO.checkOriginalDecisionNoIssued(extendServiceProcess,
            ClientId);
        if (isissued) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("EHCM_AbsenceOrigianlDecNoUP"));
          bundle.setResult(obError);
          return;
        }
      }
      JSONObject result = new JSONObject();

      result = sa.elm.ob.hcm.util.UtilityDAO.getExtendPeriodRecord(ClientId,
          extendServiceProcess.getOrganization().getId());
      if (result != null) {
        if (result.has("extendAllowed") && result.has("maxExtPeriod")) {
          if (result.getInt("extendAllowed") == 0 && result.getInt("maxExtPeriod") > 0) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("Ehcm_client_Extperiod"));
            bundle.setResult(obError);
            return;
          }

          else if (result.getInt("maxExtPeriod") == 0 && result.getInt("extendAllowed") > 0) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("Ehcm_client_maxextpeiod"));
            bundle.setResult(obError);
            return;
          } else if (result.getInt("maxExtPeriod") == 0 && result.getInt("extendAllowed") == 0) {
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("Ehcm_Extend_Error"));
            bundle.setResult(obError);
            return;
          } else {
            if (extendServiceProcess.getDecisionType().equals("CR")) {
              // check the employee eligible for extend
              String effective_Date = dateFormat.format(extendServiceProcess.getEffectivedate());
              String dob_Date = dateFormat.format(person.getDob());

              if (extendServiceProcess.getClient().getEhcmMaxempage() != null) {
                boolean ageCalculation = ExtendServiceHandlerDAO.chkEmpExtendOrNot(effective_Date,
                    dob_Date, extendServiceProcess.getClient().getEhcmMaxempage().intValue());
                if (ageCalculation) {
                  obError.setType("Error");
                  obError.setTitle("Error");
                  obError.setMessage(OBMessageUtils.messageBD("Ehcm_Extend_EmpAge"));
                  bundle.setResult(obError);
                  return;
                }
              }
            }
            if (extendServiceProcess.getDecisionType().equals("CR")) {
              // check the extendallowed is possible for selected employee
              int empCount = ExtendServiceHandlerDAO
                  .getExtendCountFromEmploymentInfo(extendServiceProcess);

              if (empCount > 0 && result.getInt("extendAllowed") > 0) {
                if (empCount >= result.getInt("extendAllowed")) {
                  obError.setType("Error");
                  obError.setTitle("Error");
                  obError.setMessage(OBMessageUtils.messageBD("Ehcm_Emp_ExtendPeriod"));
                  bundle.setResult(obError);
                  return;
                }
              }
            }
          }
        }
      }
      if (extendServiceProcess.getDecisionType().equals("CR")) {
        // check the employee period exist.
        boolean periodExists = ExtendServiceHandlerDAO.checkPeriodExists(extendServiceProcess);
        if (periodExists) {
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(OBMessageUtils.messageBD("Ehcm_period_exist"));
          bundle.setResult(obError);
          return;
        }
      }
      // check Issued or not
      if (!extendServiceProcess.isSueDecision()) {
        // update status as Issued and set decision date for all cases
        extendServiceProcess.setSueDecision(true);
        extendServiceProcess.setDecisionDate(new Date());
        extendServiceProcess.setDecisionStatus("I");
        OBDal.getInstance().save(extendServiceProcess);
        OBDal.getInstance().flush();

        count = ExtendServiceHandlerDAO.insertLineinEmploymentInfo(extendServiceProcess, vars);

        if (count == 1) {
          obError.setType("Success");
          obError.setTitle("Success");
          obError.setMessage(OBMessageUtils.messageBD("Ehcm_ExtraStep_Process"));
          bundle.setResult(obError);
          OBDal.getInstance().flush();
          OBDal.getInstance().commitAndClose();
        }

      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
