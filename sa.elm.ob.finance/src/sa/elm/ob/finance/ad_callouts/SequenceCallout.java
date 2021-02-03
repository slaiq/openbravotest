package sa.elm.ob.finance.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;

public class SequenceCallout extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = -5878400975652496282L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpcYearId = vars.getStringParameter("inpcYearId");
    String inpcPeriodId = vars.getStringParameter("inpcPeriodId");
    String inpNonPaySeq = vars.getStringParameter("inpemEfinIsacctnonpaymentseq");
    String inpGenSeq = vars.getStringParameter("inpemEfinIsgeneralseq");
    String inpPaySeq = vars.getStringParameter("inpemEfinIsacctpaymentseq");
    String inpAPPrePayInv = vars.getStringParameter("inpemEfinIsprepayinv");
    String inpAPPrePayInvApp = vars.getStringParameter("inpemEfinIsprepayinvapp");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    log4j.debug("inpNonPaySeq:" + inpNonPaySeq);
    log4j.debug("inpGenSeq:" + inpGenSeq);
    log4j.debug("inpPaySeq:" + inpPaySeq);
    if (inpLastFieldChanged.equals("inpcYearId")) {
      Year yr = OBDal.getInstance().get(Year.class, inpcYearId);
      info.addResult("inpprefix",
          yr.getFiscalYear().substring(yr.getFiscalYear().length() - 2) + "000000");
      info.addResult("inpcurrentnext",
          yr.getFiscalYear().substring(yr.getFiscalYear().length() - 2) + "0000001");
    }
    if (inpLastFieldChanged.equals("inpcPeriodId")) {
      Period period = OBDal.getInstance().get(Period.class, inpcPeriodId);
      if (new BigDecimal(period.getPeriodNo()).compareTo(new BigDecimal(10)) > 0) {
        info.addResult("inpprefix", period.getYear().getFiscalYear().substring(
            period.getYear().getFiscalYear().length() - 2) + period.getPeriodNo() + "0000");
        info.addResult("inpcurrentnext", period.getYear().getFiscalYear().substring(
            period.getYear().getFiscalYear().length() - 2) + period.getPeriodNo() + "00001");
      } else {
        info.addResult("inpprefix",
            period.getYear().getFiscalYear()
                .substring(period.getYear().getFiscalYear().length() - 2) + "0"
                + period.getPeriodNo() + "0000");
        info.addResult("inpcurrentnext",
            period.getYear().getFiscalYear()
                .substring(period.getYear().getFiscalYear().length() - 2) + "0"
                + period.getPeriodNo() + "00001");
      }
    }

    if (inpLastFieldChanged.equals("inpemEfinIsacctnonpaymentseq")) {
      if (inpNonPaySeq.equals("Y"))
        info.addResult("inpemEfinIsgeneralseq", "N");
      info.addResult("inpemEfinIsacctpaymentseq", "N");
    }
    if (inpLastFieldChanged.equals("inpemEfinIsgeneralseq")) {
      if (inpGenSeq.equals("Y"))
        info.addResult("inpemEfinIsacctnonpaymentseq", "N");
      info.addResult("inpemEfinIsacctpaymentseq", "N");
    }
    if (inpLastFieldChanged.equals("inpemEfinIsacctpaymentseq")) {
      if (inpPaySeq.equals("Y"))
        info.addResult("inpemEfinIsgeneralseq", "N");
      info.addResult("inpemEfinIsacctnonpaymentseq", "N");
    }

    /* Validate to check any one of the checkbox */
    if (inpLastFieldChanged.equals("inpemEfinIsprepayinv")) {
      if (inpAPPrePayInv.equals("Y"))
        info.addResult("inpemEfinIsprepayinvapp", "N");
    }
    if (inpLastFieldChanged.equals("inpemEfinIsprepayinvapp")) {
      if (inpAPPrePayInvApp.equals("Y"))
        info.addResult("inpemEfinIsprepayinv", "N");
    }

  }
}
