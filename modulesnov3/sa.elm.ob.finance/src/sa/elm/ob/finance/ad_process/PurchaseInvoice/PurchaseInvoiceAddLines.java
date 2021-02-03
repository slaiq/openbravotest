package sa.elm.ob.finance.ad_process.PurchaseInvoice;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DbUtility;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.Efin_UserManager;
import sa.elm.ob.finance.efinDistributionLines;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;
import sa.elm.ob.utility.util.Utility;

public class PurchaseInvoiceAddLines implements Process {

  private static final Logger log = Logger.getLogger(PurchaseInvoiceAddLines.class);
  private final OBError obError = new OBError();
  private static final String PPI_DOCUMENT = "PPI";
  private static final String PPA_DOCUMENT = "PPA";

  @SuppressWarnings("resource")
  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    Connection con = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);

    try {
      ConnectionProvider provider = bundle.getConnection();
      con = provider.getConnection();
    } catch (NoConnectionAvailableException e) {
      log.error("No Database Connection Available.Exception:" + e);
      throw new RuntimeException(e);
    }

    String invoiceId = (String) bundle.getParams().get("C_Invoice_ID");

    final String clientId = (String) bundle.getContext().getClient();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    // String comments = (String) bundle.getParams().get("comments").toString();

    log.debug(" role Id:" + roleId + ", User Id:" + userId);

    boolean errorFlag = true;
    Boolean Distributionset = false, isgovernance = false, ishavingUserManagerData = false;
    Boolean prepayment = false;

    int count = 0;

    log.debug("invoiceId:" + invoiceId);
    try {
      if (errorFlag) {
        try {
          Long lineno = 0L;
          PreparedStatement ps = null, ps1 = null;
          ResultSet rs = null, rs1 = null;

          OBContext.setAdminMode();

          Invoice invoice = Utility.getObject(Invoice.class, invoiceId);
          Organization org = Utility.getObject(Organization.class,
              invoice.getOrganization().getId());

          final boolean multiDept = org.isEfinAllowmultideptinap();
          final String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

          ps = con.prepareStatement(
              " select coalesce(max(line),0)+10  as lineno from c_invoiceline where c_invoice_id='"
                  + invoice.getId() + "' ");
          rs = ps.executeQuery();

          if (rs.next()) {
            lineno = rs.getLong("lineno");
          }

          // get user manager data to check governance cost center.
          OBQuery<Efin_UserManager> userManager = OBDal.getInstance()
              .createQuery(Efin_UserManager.class, "userContact.id=:user and documentType='API'");
          userManager.setNamedParameter("user", userId);
          List<Efin_UserManager> uManager = userManager.list();
          if (uManager != null && uManager.size() > 0) {
            SalesRegion dept = OBDal.getInstance().get(SalesRegion.class,
                uManager.get(0).getDepartment().getId());
            isgovernance = dept.isEfinGovcostcentre();
            ishavingUserManagerData = true;
          }

          if (PPI_DOCUMENT.equalsIgnoreCase(strInvoiceType)
              && invoice.getEfinManualencumbrance() != null
              && invoice.getEfinDistribution() != null) {
            prepayment = true;

            if (multiDept) {
              ps = con.prepareStatement(
                  "select * from efin_distribution_lines dln left join efin_nonexpense_lines ln on ln.uniquecode= dln.uniquecode  where  dln.efin_distribution_id ='"
                      + invoice.getEfinDistribution().getId() + "' and  dln.ad_org_id='"
                      + invoice.getOrganization().getId() + "' and dln.ad_client_id='"
                      + invoice.getClient().getId() + "' and ln.isactive='Y'");

              log.debug("ps:" + ps.toString());
            } else {
              ps = con.prepareStatement(
                  "select * from efin_distribution_lines dln left join efin_nonexpense_lines ln on ln.uniquecode= dln.uniquecode  where  dln.efin_distribution_id ='"
                      + invoice.getEfinDistribution().getId() + "' and dln.c_salesregion_id='"
                      + invoice.getEfinCSalesregion().getId() + "' and  dln.ad_org_id='"
                      + invoice.getOrganization().getId() + "' and dln.ad_client_id='"
                      + invoice.getClient().getId() + "' and ln.isactive='Y'");

              log.debug("ps:" + ps.toString());
            }

            rs = ps.executeQuery();

            while (rs.next()) {
              SalesRegion salesRegion = Utility.getObject(SalesRegion.class,
                  rs.getString("c_salesregion_id"));

              if ((ishavingUserManagerData && !isgovernance
                  && salesRegion == uManager.get(0).getDepartment()) || isgovernance) {

                Distributionset = true;
                ps1 = con.prepareStatement(
                    "select count(c_invoiceline_id) as count from c_invoiceline where em_efin_distribution_lines_id ='"
                        + rs.getString("efin_distribution_lines_id") + "' and c_invoice_id='"
                        + invoiceId + "'");
                rs1 = ps1.executeQuery();
                log.debug("ps:" + ps1.toString());

                if (rs1.next()) {
                  count = rs1.getInt("count");
                }
                if (count == 0) {

                  InvoiceLine invln = OBProvider.getInstance().get(InvoiceLine.class);
                  invln.setClient(OBDal.getInstance().get(Client.class, clientId));
                  invln.setOrganization(
                      Utility.getObject(Organization.class, rs.getString("ad_org_id")));
                  invln.setActive(true);
                  invln.setCreatedBy(OBDal.getInstance().get(User.class, userId));
                  invln.setCreationDate(new java.util.Date());
                  invln.setCreatedBy(OBDal.getInstance().get(User.class, userId));
                  invln.setUpdated(new java.util.Date());
                  invln.setInvoice(invoice);
                  invln.setLineNo(lineno);
                  invln.setFinancialInvoiceLine(true);
                  invln.setAccount(
                      Utility.getObject(GLItem.class, "AB9E92070FE34CD488A89C77336035C2"));
                  invln.setInvoicedQuantity(new BigDecimal(1));
                  invln.setListPrice(new BigDecimal(1));
                  invln.setUnitPrice(new BigDecimal(1));
                  invln.setLineNetAmount(new BigDecimal(1));
                  // invln.setEfinBudgmanuencumln(OBDal.getInstance().get(EfinBudgetManencumlines.class,
                  // distline.getId()));
                  invln.setEfinCElementvalue(
                      Utility.getObject(ElementValue.class, rs.getString("c_elementvalue_id")));
                  invln.setEfinCCampaign(
                      Utility.getObject(Campaign.class, rs.getString("c_campaign_id")));
                  invln.setEfinCSalesregion(
                      Utility.getObject(SalesRegion.class, rs.getString("c_salesregion_id")));
                  invln.setProject(Utility.getObject(Project.class, rs.getString("c_project_id")));
                  invln.setEfinCActivity(
                      Utility.getObject(ABCActivity.class, rs.getString("c_activity_id")));
                  invln.setStDimension(
                      Utility.getObject(UserDimension1.class, rs.getString("user1_id")));
                  invln.setNdDimension(
                      Utility.getObject(UserDimension2.class, rs.getString("user2_id")));
                  invln.setEFINUniqueCode(rs.getString("uniquecode"));
                  invln.setEfinDistributionLines(Utility.getObject(efinDistributionLines.class,
                      rs.getString("efin_distribution_lines_id")));
                  invln.setEFINFundsAvailable(new BigDecimal(0));

                  OBDal.getInstance().save(invln);
                  log.debug("invln:" + invln.getId());

                  lineno += 10;
                }
              }
            }
          }
          // added all the line from Distribution if distribution is selected
          /*
           * if(invoice.getTransactionDocument().isEfinIsprepayinv() &&
           * invoice.getEfinManualencumbrance() != null && invoice.getEfinDistribution() != null) {
           * 
           * OBQuery<efinDistributionLines> disline =
           * OBDal.getInstance().createQuery(efinDistributionLines.class, " efinDistribution.id='" +
           * invoice.getEfinDistribution().getId() + "'"); if(disline.list().size() > 0) { for
           * (efinDistributionLines distline : disline.list()) { ps=con.
           * prepareStatement("select count(c_invoiceline_id) as count from c_invoiceline where em_efin_distribution_lines_id ='"
           * +distline.getId()+"' and c_invoice_id='"+invoiceId+"'"); rs=ps.executeQuery();
           * if(rs.next()){ count=rs.getInt("count"); } if(count==0){ InvoiceLine invln =
           * OBProvider.getInstance().get(InvoiceLine.class);
           * invln.setClient(OBDal.getInstance().get(Client.class, clientId));
           * invln.setOrganization(OBDal.getInstance().get(Organization.class,
           * distline.getOrganization().getId())); invln.setActive(true);
           * invln.setCreatedBy(OBDal.getInstance().get(User.class, userId));
           * invln.setCreationDate(new java.util.Date());
           * invln.setCreatedBy(OBDal.getInstance().get(User.class, userId)); invln.setUpdated(new
           * java.util.Date()); invln.setInvoice(invoice); invln.setLineNo(lineno);
           * invln.setFinancialInvoiceLine(true);
           * invln.setAccount(OBDal.getInstance().get(GLItem.class,
           * "AB9E92070FE34CD488A89C77336035C2")); invln.setInvoicedQuantity(new BigDecimal(1));
           * invln.setListPrice(new BigDecimal(1)); invln.setUnitPrice(new BigDecimal(1));
           * invln.setLineNetAmount(new BigDecimal(1));
           * invln.setEfinBudgmanuencumln(OBDal.getInstance().get(EfinBudgetManencumlines.class,
           * distline.getId())); invln.setEfinCElementvalue(distline.getAccountElement());
           * invln.setEfinCCampaign(distline.getSalesCampaign());
           * invln.setEfinCSalesregion(distline.getSalesRegion());
           * invln.setProject(distline.getProject());
           * invln.setEfinCActivity(distline.getActivity());
           * invln.setStDimension(distline.getStDimension());
           * invln.setNdDimension(distline.getNdDimension());
           * invln.setEFINUniqueCode(distline.getUniqueCode());
           * invln.setEfinDistributionLines(OBDal.getInstance().get(efinDistributionLines.class,
           * distline.getId())); invln.setEFINFundsAvailable(new BigDecimal(0));
           * OBDal.getInstance().save(invln); lineno += 10; } } }
           * 
           * }
           */

          // added all the line from manual encumbrance if distribution not select
          else if (!PPI_DOCUMENT.equals(strInvoiceType)
              && invoice.getEfinManualencumbrance() != null
              && invoice.getEfinDistribution() == null) {
            OBQuery<EfinBudgetManencumlines> lines = null;
            if (PPA_DOCUMENT.equals(strInvoiceType)) {
              lines = OBDal.getInstance().createQuery(EfinBudgetManencumlines.class,
                  " manualEncumbrance.id='" + invoice.getEfinManualencumbrance().getId()
                      + "' and  aPPAmt >0");
            } else {
              lines = OBDal.getInstance().createQuery(EfinBudgetManencumlines.class,
                  " manualEncumbrance.id='" + invoice.getEfinManualencumbrance().getId()
                      + "' and remainingAmount >0");
            }

            log.debug("list:" + lines.list().size());
            if (lines.list().size() > 0) {
              for (EfinBudgetManencumlines ln : lines.list()) {
                if ((ishavingUserManagerData && !isgovernance
                    && ln.getSalesRegion() == uManager.get(0).getDepartment()) || isgovernance) {

                  ps = con.prepareStatement(
                      "select count(c_invoiceline_id) as count from c_invoiceline where em_efin_budgmanuencumln_id ='"
                          + ln.getId() + "' and c_invoice_id='" + invoiceId + "'");

                  rs = ps.executeQuery();
                  if (rs.next()) {
                    count = rs.getInt("count");
                  }
                  if (count == 0) {
                    InvoiceLine invln = OBProvider.getInstance().get(InvoiceLine.class);
                    JSONObject fundsCheckingObject = null;
                    BigDecimal fundsAvailable = BigDecimal.ZERO;
                    invln.setClient(Utility.getObject(Client.class, clientId));
                    invln.setOrganization(
                        Utility.getObject(Organization.class, ln.getOrganization().getId()));
                    invln.setActive(true);
                    invln.setCreatedBy(Utility.getObject(User.class, userId));
                    invln.setCreationDate(new java.util.Date());
                    invln.setCreatedBy(Utility.getObject(User.class, userId));
                    invln.setUpdated(new java.util.Date());
                    invln.setInvoice(invoice);
                    invln.setLineNo(lineno);
                    invln.setFinancialInvoiceLine(true);
                    invln.setAccount(
                        Utility.getObject(GLItem.class, "AB9E92070FE34CD488A89C77336035C2"));
                    invln.setInvoicedQuantity(new BigDecimal(1));
                    if (PPA_DOCUMENT.equals(strInvoiceType)) {
                      invln.setUnitPrice(ln.getAPPAmt());
                      invln.setLineNetAmount(ln.getAPPAmt());
                    } else {
                      invln.setUnitPrice(ln.getRemainingAmount());
                      invln.setLineNetAmount(ln.getRemainingAmount());
                    }
                    invln.setEfinBudgmanuencumln(
                        Utility.getObject(EfinBudgetManencumlines.class, ln.getId()));
                    invln.setEfinCElementvalue(ln.getAccountElement());
                    invln.setEfinCCampaign(ln.getSalesCampaign());
                    invln.setEfinCSalesregion(ln.getSalesRegion());
                    invln.setProject(ln.getProject());
                    invln.setEfinCActivity(ln.getActivity());
                    invln.setStDimension(ln.getStDimension());
                    invln.setNdDimension(ln.getNdDimension());
                    // invln.setEfinBudgetlines(ln.getBudgetLines());
                    invln.setEfinCValidcombination(ln.getAccountingCombination());
                    invln.setEfinCBpartner(ln.getBusinessPartner());
                    invln.setEFINUniqueCode(ln.getAccountingCombination().getEfinUniqueCode());

                    if (invoice.getEfinEncumtype().equals("M"))
                      // fund available from budget enquiry
                      invln.setEFINFundsAvailable(RequisitionDao.getAutoEncumFundsAvailable(
                          ln.getAccountingCombination().getId(),
                          invoice.getEfinBudgetint().getId()));
                    // invln.setEFINFundsAvailable(ln.getRemainingAmount());
                    else if (!PPA_DOCUMENT.equals(strInvoiceType)
                        && invoice.getEfinEncumtype().equals("A"))
                      invln.setEFINFundsAvailable(ln.getBudgetLines().getFundsAvailable());
                    else if (PPA_DOCUMENT.equals(strInvoiceType)
                        && invoice.getEfinEncumtype().equals("A")) {
                      invln.setEFINFundsAvailable(ln.getAPPAmt());
                    }

                    OBDal.getInstance().save(invln);

                    if (invln.getInvoice().getEfinBudgetType().equals("C")) {
                      AccountingCombination accCombination = OBDal.getInstance().get(
                          AccountingCombination.class, invln.getEfinCValidcombination().getId());
                      EfinBudgetIntialization budgetIntialization = Utility.getObject(
                          EfinBudgetIntialization.class,
                          invln.getInvoice().getEfinBudgetint().getId());
                      fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
                          accCombination.getEfinFundscombination());
                      fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
                      invln.setEfinFbFundsAvailable(fundsAvailable);
                    } else {
                      invln.setEfinFbFundsAvailable(null);
                    }
                    lineno += 10;
                  }
                  // OBDal.getInstance().flush();
                  // OBDal.getInstance().commitAndClose();
                }
              }
            }
          }
          // if we select encum type as "Manual" and does not select any Manual Encumbrance then
          // Throw error.
          else if (invoice.getEfinEncumtype().equals("M")
              && invoice.getEfinManualencumbrance() == null
              && PPI_DOCUMENT.equals(strInvoiceType)) {

            errorFlag = false;
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("Efin_PurInv_ManEncMan"));
            bundle.setResult(obError);
            return;
          }

          else if (invoice.getEfinEncumtype().equals("M")
              && invoice.getEfinManualencumbrance() == null) {

            errorFlag = false;
            obError.setType("Error");
            obError.setTitle("Error");
            obError.setMessage(OBMessageUtils.messageBD("Efin_PurInv_SelectManEncum"));
            bundle.setResult(obError);
            return;
          }
          log.debug("erorflag:" + errorFlag);

          if (errorFlag) {
            if (!Distributionset && prepayment) {
              obError.setType("Info");
              obError.setTitle("Info");
              obError.setMessage(OBMessageUtils.messageBD("Efin_No_Distribution"));
              bundle.setResult(obError);
            } else {
              obError.setType("Success");
              obError.setTitle("Success");
              obError.setMessage(OBMessageUtils.messageBD("EFIN_Success"));
            }

          }

        } catch (Exception e) {
          Throwable t = DbUtility.getUnderlyingSQLException(e);
          final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
              vars.getLanguage(), t.getMessage());
          bundle.setResult(error);
          OBDal.getInstance().rollbackAndClose();
        }
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      bundle.setResult(obError);
      log.error("exception :", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
