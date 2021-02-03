package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.Escmopenenvcommitee;

public class TechnicalEvaluationEventCallout extends SimpleCallout {

  /**
   * callout for Technical evaluation event.Its called when date hijir or bid management or
   * estimated price change
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;

    String inpdateHijri = vars.getStringParameter("inpdateHijri");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String strBidID = vars.getStringParameter("inpescmBidmgmtId");
    String inpestimatedPrice = vars.getNumericParameter("inpestimatedPrice");
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    List<EscmProposalAttribute> proAttList = new ArrayList<EscmProposalAttribute>();
    PreparedStatement st = null;
    ResultSet rs = null;
    List<Escmopenenvcommitee> openlist = new ArrayList<Escmopenenvcommitee>();
    BigDecimal difference = BigDecimal.ZERO;
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

          // get committee details based on bid id
          OBQuery<Escmopenenvcommitee> openenvelop = OBDal.getInstance()
              .createQuery(Escmopenenvcommitee.class, " as e where e.bidNo.id='" + strBidID + "'");
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
      // if estimated price change then need to calculate difference in proposal lines.
      if (inpLastFieldChanged.equals("inpestimatedPrice")) {

        // fetch the proposal attribute record by using bid in open envelop
        OBQuery<EscmProposalAttribute> proposalatt = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class,
            " as e where e.escmOpenenvcommitee.id is not null "
                + " and e.escmOpenenvcommitee.id=( select ope.id from escm_openenvcommitee ope where ope.bidNo='"
                + strBidID + "' and ope.alertStatus='CO') ");
        proAttList = proposalatt.list();
        if (proAttList.size() > 0) {
          for (EscmProposalAttribute proatt : proAttList) {
            // set the difference
            if (inpestimatedPrice != null && inpestimatedPrice != "null" && inpestimatedPrice != ""
                && proatt.getTechNegotiatedPrice() != null
                && (new BigDecimal(inpestimatedPrice).compareTo(BigDecimal.ZERO) > 0)) {
              difference = (new BigDecimal(inpestimatedPrice)
                  .subtract(proatt.getTechNegotiatedPrice())
                  .divide(new BigDecimal(inpestimatedPrice), 4, RoundingMode.FLOOR))
                      .multiply(new BigDecimal(100));
              proatt.setTechVariation(difference);
              OBDal.getInstance().save(proatt);
              OBDal.getInstance().flush();
            } else if (inpestimatedPrice == null || inpestimatedPrice == "null"
                || inpestimatedPrice == ""
                || (new BigDecimal(inpestimatedPrice).compareTo(BigDecimal.ZERO) == 0)) {
              proatt.setTechVariation(BigDecimal.ZERO);
              OBDal.getInstance().save(proatt);
              OBDal.getInstance().flush();
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in Technical EvaluationEventCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      try {
        if (rs != null)
          rs.close();
        if (st != null)
          st.close();
      } catch (Exception e) {
        log4j.error("Exception while closing the statement in Technical EvaluationEventCallout ",
            e);
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
