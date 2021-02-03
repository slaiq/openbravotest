package sa.elm.ob.scm.ad_callouts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmTechnicalevlEvent;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.utility.util.UtilityDAO;

public class ProposalEvaluationEventCallout extends SimpleCallout {

  /**
   * callout for Proposal evaluation event.Its called when date hijir or bid management
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;

    String inpdateHijri = vars.getStringParameter("inpdateHijri");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strBidID = vars.getStringParameter("inpescmBidmgmtId");
    String inpApprovalDateH = vars.getStringParameter("inpescmApprovalDateHijiri");
    String inpApprovalDateG = vars.getStringParameter("inpescmApprovalDateGreg");

    DateFormat dateyearFormat = new SimpleDateFormat("dd-MM-yyyy");
    DateFormat dateyearForm = new SimpleDateFormat("yyyy-MM-dd");

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    EscmTechnicalevlEvent techeventobj = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    List<Escmopenenvcommitee> openlist = new ArrayList<Escmopenenvcommitee>();

    try {
      log4j.debug("inpLastFieldChanged " + inpLastFieldChanged);

      // get corresponding Gregorian date if hijiri date changed.
      if (inpLastFieldChanged.equals("inpdateHijri")) {
        st = OBDal.getInstance().getConnection()
            .prepareStatement("select to_char(eut_convertto_gregorian('" + inpdateHijri
                + "')) as eut_convertto_gregorian ");
        rs = st.executeQuery();

        if (rs.next()) {
          info.addResult("inpdateGregorian", rs.getString("eut_convertto_gregorian"));

        }
        rs.close();
      }
      // if bid is change then bring the bid details
      if (inpLastFieldChanged.equals("inpescmBidmgmtId")) {
        if (strBidID != null && !strBidID.equals("")) {
          EscmBidMgmt bidMgmt = OBDal.getInstance().get(EscmBidMgmt.class, strBidID);
          info.addResult("inpapprovedbudget", bidMgmt.getApprovedbudget());
          info.addResult("inpbidname", bidMgmt.getBidname());
          // Set award full qty by default as true
          if (bidMgmt.isPartialaward()) {
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('Ispartialaward').setValue(true)");
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('Isawardfullqty').setValue(true)");
          }

          OBQuery<EscmTechnicalevlEvent> techEvlevent = OBDal.getInstance().createQuery(
              EscmTechnicalevlEvent.class, " as e where e.bidNo.id=:bidId and e.status='CO' ");
          techEvlevent.setNamedParameter("bidId", bidMgmt.getId());
          techEvlevent.setMaxResult(1);
          if (techEvlevent.list().size() > 0) {
            techeventobj = techEvlevent.list().get(0);
          }
          if (techeventobj != null) {
            info.addResult("inpescmTechnicalevlEventId", techeventobj.getId());
            info.addResult("inptecDateHijri",
                convertTohijriDate(dateFormat.format(techeventobj.getDateH())));
          } else {
            info.addResult("inpescmTechnicalevlEventId", null);
            info.addResult("inptecDateHijri", null);
          }

          // get committee details based on bid id
          OBQuery<Escmopenenvcommitee> openenvelop = OBDal.getInstance()
              .createQuery(Escmopenenvcommitee.class, " as e where e.bidNo.id=:bidId ");
          openenvelop.setNamedParameter("bidId", strBidID);
          openenvelop.setMaxResult(1);
          openlist = openenvelop.list();
          if (openlist.size() > 0) {
            info.addResult("inpescmOpenenvcommiteeId", openlist.get(0).getId());
            if (openlist.get(0).getTodaydate() != null)
              info.addResult("inpenvelopeDate",
                  convertTohijriDate(dateFormat.format(openlist.get(0).getTodaydate())));
            else
              info.addResult("inpenvelopeDate", "");

            if (openlist.get(0).getProposalcount() != null)
              info.addResult("inpproposalCounts", openlist.get(0).getProposalcount());
            else
              info.addResult("inpproposalCounts", 0);

          } else {
            info.addResult("inpescmOpenenvcommiteeId", null);
            info.addResult("inpproposalCounts", 0);
            info.addResult("inpenvelopeDate", "");
          }
        } else {
          info.addResult("inpapprovedbudget", "");
          info.addResult("inpbidname", "");
          info.addResult("inpescmOpenenvcommiteeId", null);
          info.addResult("inpproposalCounts", 0);
          info.addResult("inpenvelopeDate", "");
        }
      }
      if (inpLastFieldChanged.equals("inpescmApprovalDateHijiri")) {
        if (StringUtils.isNotEmpty(inpApprovalDateH)) {
          String appDate = UtilityDAO.convertToGregTimeStamp(inpApprovalDateH);
          System.out.println("appDate:" + appDate);
          info.addResult("inpescmApprovalDateGreg",
              UtilityDAO.convertToGregTimeStamp(inpApprovalDateH));
        } else
          info.addResult("inpescmApprovalDateGreg", null);
      }
      if (inpLastFieldChanged.equals("inpescmApprovalDateGreg")) {
        if (StringUtils.isNotEmpty(inpApprovalDateG)) {
          String approvalDateGreg = dateyearForm.format(dateyearFormat.parse(inpApprovalDateG));
          info.addResult("inpescmApprovalDateHijiri",
              UtilityDAO.convertToHijriDate(approvalDateGreg));
        } else {
          info.addResult("inpemEscmApprovalDateHijiri", null);
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in ProposalEvaluationEventCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      // close connection
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in ProposalEvaluationEventCallout ", e);
      }
    }

  }

  /**
   * Get hijiri date
   * 
   * @param gregDate
   *          format('yyyy-MM-dd')
   * @return hijiri date
   */
  public String convertTohijriDate(String gregDate) {
    String hijriDate = "";
    try {
      SQLQuery gradeQuery = OBDal.getInstance().getSession()
          .createSQLQuery("select eut_convert_to_hijri(to_char(to_timestamp('" + gregDate
              + "','YYYY-MM-DD HH24:MI:SS'),'YYYY-MM-DD  HH24:MI:SS'))");
      if (gradeQuery.list().size() > 0) {
        Object row = (Object) gradeQuery.list().get(0);
        hijriDate = (String) row;
      }
    } catch (final Exception e) {
      log4j.error("Exception in convertTohijriDate() Method : ", e);
      return "0";
    }
    return hijriDate;
  }
}