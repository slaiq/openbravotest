package sa.elm.ob.scm.ad_callouts;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.scm.ad_callouts.dao.CommitteeCalloutDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Divya on 29/05/2017
 * 
 */
public class CommitteeCallout extends SimpleCallout {
  private static Logger log = Logger.getLogger(CommitteeCallout.class);
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    // String strEmpId = vars.getStringParameter("inpehcmEmpPerinfoId");
    String strEmpId = vars.getStringParameter("inpcBpartnerId");
    String strtype = vars.getStringParameter("inptype");
    String inpstartdate = vars.getStringParameter("inpstartdate");
    String strtabId = vars.getStringParameter("inpTabId");
    String commiteeid = vars.getStringParameter("inpescmCommitteeId");
    log.debug("commiteeid:" + commiteeid);
    String deptId = "";
    try {
      log.debug("LastChanged:" + inpLastFieldChanged);
      if (inpLastFieldChanged.equals("inpcBpartnerId")) {
        if (strEmpId != null) {
          BusinessPartner person = Utility.getObject(BusinessPartner.class, strEmpId);
          // EhcmEmpPerInfo person = OBDal.getInstance().get(EhcmEmpPerInfo.class, strEmpId);

          info.addResult("inpempName", person.getName());
          // get deptID using dept code.
          deptId = CommitteeCalloutDAO.getDepartmentId(person.getEhcmDepartmentCode());
          if (deptId.equals(""))
            info.addResult("inpdepname", null);
          else
            info.addResult("inpdepname", deptId);
          info.addResult("inpehcmPositionId", person.getEhcmPosition());
          info.addResult("inpehcmGradeclassId", person.getEhcmGrade());

          JSONObject json = CommitteeCalloutDAO.getDates(commiteeid);

          info.addResult("inpeffectiveFrom", json.get("effectiveFrom").toString());
          info.addResult("inpeffectiveTo", json.get("effectiveTo").toString());
          /*
           * if (person.getGradeClass() != null) info.addResult("inpehcmGradeclassId",
           * person.getGradeClass().getId()); OBQuery<EmploymentInfo> empInfo =
           * OBDal.getInstance().createQuery(EmploymentInfo.class, " ehcmEmpPerinfo.id='" + strEmpId
           * + "' and enabled='Y' order by creationDate desc "); log4j.debug("employeeId:" +
           * strEmpId); log4j.debug("positiontype:" + empInfo.list().size()); if
           * (empInfo.list().size() > 0) { empinfo = empInfo.list().get(0);
           * info.addResult("inpehcmPositionId", empinfo.getPosition().getId()); }
           */
        }
      }
      if (inpLastFieldChanged.equals("inptype") || (inpLastFieldChanged.equals("inpstartdate")
          && strtabId.equals("9D69A2330F9B48678D473A05BD486A40"))) {
        String startyear = inpstartdate.split("-")[2];

        if (strtype.equals("PC")) {
          int endyear = Integer.valueOf(startyear) + Integer.valueOf(1);
          String enddate = endyear + inpstartdate.split("-")[1] + inpstartdate.split("-")[0];
          log4j.debug("enddate:" + enddate);
          enddate = CommitteeCalloutDAO.getOneDayMinusHijiriDate(enddate, vars.getClient());
          info.addResult("inpenddate", enddate);
        } else if (strtype.equals("OEC")) {
          int endyear = Integer.valueOf(startyear) + Integer.valueOf(3);
          String enddate = endyear + inpstartdate.split("-")[1] + inpstartdate.split("-")[0];
          log4j.debug("enddate:" + enddate);
          enddate = CommitteeCalloutDAO.getOneDayMinusHijiriDate(enddate, vars.getClient());
          info.addResult("inpenddate", enddate);
        }
      }

    } catch (Exception e) {
      log.error("Exception in Commiteecallout:", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }
}
