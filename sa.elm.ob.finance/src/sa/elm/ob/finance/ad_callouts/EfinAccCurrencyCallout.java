package sa.elm.ob.finance.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import sa.elm.ob.finance.EfinAccount;
import sa.elm.ob.finance.EfinBank;

public class EfinAccCurrencyCallout extends SimpleCallout {
  private static final long serialVersionUID = -8469446494495266203L;

  @SuppressWarnings("resource")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    VariablesSecureApp vars = info.vars;
    String inpadOrgId = vars.getStringParameter("inpadOrgId");
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    log4j.info("inpadOrgId:" + inpadOrgId);
    String inpemEfinBankId = vars.getStringParameter("inpemEfinBankId");
    String inpemEfinAccountId = vars.getStringParameter("inpemEfinAccountId");

    if (inpLastFieldChanged.equals("inpadOrgId")) {
      Connection conn = OBDal.getInstance().getConnection();
      PreparedStatement st = null;
      ResultSet rs, rs1 = null;
      String currId = null;

      try {
        OBContext.setAdminMode();
        while (inpadOrgId != null && !inpadOrgId.equals("")) {
          log4j.info("inpadOrgId ins:" + inpadOrgId);
          st = conn.prepareStatement(
              "select c_currency_id from c_acctschema  where c_acctschema_id = (select c_acctschema_id from ad_org where ad_org_id = ?)");
          st.setString(1, inpadOrgId);
          rs = st.executeQuery();
          if (rs.next() && rs.getString("c_currency_id") != null
              && !rs.getString("c_currency_id").equals("")) {
            currId = rs.getString("c_currency_id");
            break;
          } else {
            st = conn.prepareStatement("SELECT parent_id FROM ad_treenode WHERE node_id = ? "
                + " AND ad_tree_id = (SELECT ad_tree_id FROM ad_tree "
                + " WHERE ad_client_id = ? AND treetype = 'OO');");
            st.setString(1, inpadOrgId);
            st.setString(2, vars.getClient());
            rs1 = st.executeQuery();
            if (rs1.next()) {
              inpadOrgId = rs1.getString("parent_id");
              continue;
            } else {
              break;
            }
          }
        }
        if (currId != null)
          info.addResult("inpcCurrencyId", currId);
        /*
         * else info.addResult("inpcCurrencyId", "");
         */
      } catch (SQLException e) {
        log4j.debug("Exception while handling EfinAccCurrencyCallout callout", e);
      } finally {
        //close connection 
        try {
          if(st != null) {
            st.close();
          }if(rs1 != null) {
            rs1.close();
          }
        }catch(Exception e) {
          log4j.debug("Exception while close connection", e);
        }
        OBContext.restorePreviousMode();
      }
    }

    if (inpLastFieldChanged.equals("inpemEfinBankId")) {
      if (inpemEfinBankId != null && !inpemEfinBankId.equals("")) {
        EfinBank bank = OBDal.getInstance().get(EfinBank.class, inpemEfinBankId);
        info.addResult("inpemEfinAltbankname", bank.getAltbankname());
        info.addResult("inpemEfinShrtbankname", bank.getShrtbankname());
        info.addResult("inpemEfinBankno", bank.getSearchKey());

      } else {
        info.addResult("inpemEfinAltbankname", null);
        info.addResult("inpemEfinShrtbankname", null);
        info.addResult("inpemEfinBankno", null);

      }
    }

    if (inpLastFieldChanged.equals("inpemEfinAccountId")) {
      if (inpemEfinAccountId != null && !inpemEfinAccountId.equals("")) {
        EfinAccount account = OBDal.getInstance().get(EfinAccount.class, inpemEfinAccountId);
        info.addResult("inpemEfinAcctType", account.getAccountType());
        info.addResult("inpiban", account.getIBAN());
        info.addResult("inpswiftcode", account.getSWIFTCode());
      } else {
        info.addResult("JSEXECUTE",
            "form.getFieldFromColumnName('em_efin_acct_type').setValue('')");
        info.addResult("inpiban", null);
        info.addResult("inpswiftcode", null);
      }

    }

    if (inpLastFieldChanged.equals("inptype")) {
      info.addResult("JSEXECUTE", "form.getFieldFromColumnName('em_efin_bank_id').setValue('')");

    }
  }
}