package sa.elm.ob.scm.ad_callouts;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;

import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * 
 * 
 */

@SuppressWarnings("serial")
public class BankGuaranteeCallout extends SimpleCallout {
  private static final String warningMessage = "Escm_BankGuara_Value";

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    VariablesSecureApp vars = info.vars;

    DateFormat dateyearFormat = new SimpleDateFormat("dd-MM-yyyy");
    DateFormat dateyearForm = new SimpleDateFormat("yyyy-MM-dd");
    String inpbgstartdateh = vars.getStringParameter("inpbgstartdateh");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String proposalattid = null, bidid = null;

    String inpbgamount = vars.getNumericParameter("inpbgamount");
    String inpstartdategre = vars.getStringParameter("inpbgstartdategre");
    String inpexpirydategre = vars.getStringParameter("inpexpirydategre");
    String parsedMessage = null;

    BigDecimal discountamt = BigDecimal.ZERO;
    if (vars.getStringParameter("inpescmBidmgmtId") != null)
      bidid = vars.getStringParameter("inpescmBidmgmtId");

    if (vars.getStringParameter("inpescmProposalAttrId") != null)
      proposalattid = vars.getStringParameter("inpescmProposalAttrId");

    try {
      inpstartdategre = dateyearForm.format(dateyearFormat.parse(inpstartdategre));
      inpexpirydategre = dateyearForm.format(dateyearFormat.parse(inpexpirydategre));

    } catch (ParseException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    try {
      if (inpLastFieldChanged.equals("inpbgstartdateh")) {
        info.addResult("inpbgstartdategre", UtilityDAO.convertToGregorian_tochar(inpbgstartdateh));
      }
      if (inpLastFieldChanged.equals("inpbgstartdategre")) {
        info.addResult("inpbgstartdateh", UtilityDAO.convertToGregorian_tochar(inpstartdategre));
      }
      if (inpLastFieldChanged.equals("inpexpirydategre")) {
        info.addResult("inpexpirydateh", UtilityDAO.convertToGregorian_tochar(inpexpirydategre));
      }
      if (inpLastFieldChanged.equals("inpbgamount")) {
        BigDecimal bgamt = new BigDecimal(inpbgamount);

        discountamt = BGWorkbenchDAO.chkWithbgAmtandTermValue(proposalattid, bidid, null);

        if (bgamt.compareTo(discountamt) > 0) {
          parsedMessage = Utility.messageBD(this, warningMessage,
              OBContext.getOBContext().getLanguage().getId());
          info.addResult("WARNING", parsedMessage);

        }
      }
    } catch (Exception e) {
      log4j.error("Exception in BankGuaranteeCallout  :", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }
}
