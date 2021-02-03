package sa.elm.ob.finance.ad_process.RDVProcess.DAO;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.sales.SalesRegion;

import sa.elm.ob.finance.EFIN_TaxMethod;
import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.ad_callouts.BudgetAdjustmentCallout;
import sa.elm.ob.finance.ad_process.PurchaseInvoice.PurchaseInvoiceSubmitUtils;
import sa.elm.ob.finance.ad_process.RDVProcess.vo.RDVManagerVO;
import sa.elm.ob.finance.event.invoiceline.AddFromAdvanceTypeDAO;
import sa.elm.ob.finance.util.AlertUtility;
import sa.elm.ob.finance.util.AlertWindow;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.scm.EscmCOrderV;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

public class GenerateAmarsarafDAO {
  VariablesSecureApp vars = null;
  private static final String RDV_DOCUMENT = "RDV";
  private static Logger log4j = Logger.getLogger(GenerateAmarsarafDAO.class);

  /**
   * 
   * @param uniqueCode
   * @param version
   * @param rdv
   * @param orgId
   * @param txnType
   * @return
   */
  @SuppressWarnings("rawtypes")
  public static Invoice insertInvoice(AccountingCombination uniqueCode, EfinRDVTransaction version,
      EfinRDV rdv, String orgId, String txnType, FIN_PaymentMethod payMethod, PaymentTerm payTerm,
      PriceList prcList, DocumentType transDoc) {

    try {
      OBContext.setAdminMode();

      String query = null;
      Query sqlQuery1 = null;
      List queryList1 = null;
      String acctcode = "";
      BigDecimal amt = BigDecimal.ZERO, total = BigDecimal.ZERO;
      BusinessPartner bpartner = null;
      Invoice invoice = OBProvider.getInstance().get(Invoice.class);
      invoice.setOrganization(version.getOrganization());
      invoice.setTransactionDocument(transDoc);
      BigDecimal fundsAvailable = BigDecimal.ZERO;
      BigDecimal GrandtotalAmt = BigDecimal.ZERO;
      JSONObject fundsCheckingObject = null;
      List<InvoiceLine> invoiceLineList = new ArrayList<InvoiceLine>();
      // find Budget Definition Based On Date

      String budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(new Date(),
          version.getClient().getId(), "");

      DocumentType doctype = OBDal.getInstance().get(DocumentType.class, "0");
      invoice.setDocumentType(doctype);
      OBQuery<SalesRegion> dept = OBDal.getInstance().createQuery(SalesRegion.class,
          "organization.id= :orgID");
      dept.setNamedParameter("orgID", orgId);
      dept.setMaxResult(1);
      invoice.setEfinCSalesregion(dept.list().get(0));
      // invoice.setEfinCSalesregion();
      invoice.setBusinessPartner(rdv.getBusinessPartner());
      invoice.setPartnerAddress(rdv.getBusinessPartner().getBusinessPartnerLocationList().get(0));
      invoice.setEfinIban(rdv.getPartnerBankAccount());
      invoice.setInvoiceDate(new Date());
      invoice.setAccountingDate(new Date());
      invoice.setEfinEncumtype("A");
      invoice.setEfinEncumbranceType("AEE");
      invoice
          .setEfinBudgetint(OBDal.getInstance().get(EfinBudgetIntialization.class, budgInitialId));
      invoice.setEfinRdvtxn(version);
      invoice.setPaymentMethod(payMethod);
      invoice.setPaymentTerms(payTerm);
      Currency currency = OBDal.getInstance().get(Currency.class, "317");
      invoice.setCurrency(currency);
      invoice.setEfinBudgetType(uniqueCode.getSalesCampaign().getEfinBudgettype());
      invoice.setEfinCSalesregion(uniqueCode.getSalesRegion());
      invoice.setEfinIsrdv(true);
      invoice.setEfinAdRole(OBContext.getOBContext().getRole());
      invoice.setSalesTransaction(false);
      invoice.setPriceList(prcList);
      invoice.setEfinInvoicetypeTxt(RDV_DOCUMENT);
      if (!rdv.getTXNType().equals("POD")) {
        invoice.setSalesOrder(rdv.getSalesOrder());
        if (rdv.getSalesOrder() != null) {
          EscmCOrderV ord = OBDal.getInstance().get(EscmCOrderV.class, rdv.getSalesOrder().getId());
          invoice.setEfinCOrder(ord);
          if (rdv.getSalesOrder().getEscmSecondsupplier() != null
              && rdv.getSalesOrder().getEscmSecondIban() != null) {
            invoice.setBusinessPartner(rdv.getSalesOrder().getEscmSecondsupplier());
            invoice.setPartnerAddress(rdv.getSalesOrder().getEscmSecondsupplier()
                .getBusinessPartnerLocationList().get(0));
            invoice.setEfinIban(rdv.getSalesOrder().getEscmSecondIban());

          } else {
            if (rdv.getSalesOrder().getEscmIban() != null) {
              invoice.setEfinIban(rdv.getSalesOrder().getEscmIban());
            }
          }

          // if (version.getEfinRDVTxnlineList().get(0).getSalesOrderLine() != null && version
          // .getEfinRDVTxnlineList().get(0).getSalesOrderLine().getSalesOrder().isEscmIstax()) {
          Order latestOrder = PurchaseInvoiceSubmitUtils
              .getLatestOrderComplete(rdv.getSalesOrder());
          if (latestOrder != null && latestOrder.isEscmIstax()) {
            invoice.setEfinIstax(true);
            invoice.setEfinTaxMethod(latestOrder.getEscmTaxMethod());
            invoice.setEfinIstaxpo(true);
          }
        }
        invoice.setEfinSupcontractexpdate(rdv.getEXPDate());
      }
      // Set Attachment Reference in Invoice based on the PO Number incase of Mustakals PO and PO
      // Receipt
      if ((rdv.getTXNType().equals("PO") || rdv.getTXNType().equals("POS"))
          && (rdv.getSalesOrder().getEscmReferenceNo() != null))
        invoice.setEfinAttachementref(rdv.getSalesOrder().getEscmReferenceNo());

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      char combiantion = 'N';

      BigDecimal poTotalAmt = BigDecimal.ZERO, advPercentage = BigDecimal.ZERO;
      BigDecimal newTaxPercentage = BigDecimal.ZERO;
      Order latestOrder = null;
      if (rdv.getSalesOrder() != null) {
        // take latest orderId
        latestOrder = PurchaseInvoiceSubmitUtils.getLatestOrderComplete(rdv.getSalesOrder());
      }

      EFIN_TaxMethod newTaxMethod = calculateNewTax(version, amt);
      if (newTaxMethod != null) {
        newTaxPercentage = new BigDecimal(newTaxMethod.getTaxpercent());
      }

      if (latestOrder != null) {
        poTotalAmt = latestOrder.getGrandTotalAmount();

        advPercentage = (version.getNetmatchAmt().divide(poTotalAmt, 15, RoundingMode.HALF_UP));
        if (newTaxMethod != null) {
          advPercentage = (version.getNewtaxNetmatchAmt().divide(poTotalAmt, 15,
              RoundingMode.HALF_UP));
        }
      }

      // chk version line is mixed with advance and lines also
      query = " select distinct isadvance from efin_rdvtxnline  where efin_rdvtxn_id  = :rdvTxnID";
      sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery1.setParameter("rdvTxnID", version.getId());
      queryList1 = sqlQuery1.list();
      if (sqlQuery1 != null && queryList1.size() > 0) {
        combiantion = (char) queryList1.get(0);
      }

      if (sqlQuery1 != null && queryList1.size() > 1) {
        // this case will not happen as per current flow, because first advance then actaul
        // transaction need to happen
        // otherwise without advance only actual transction will happen,

        // if version mixed with advance and lines
        query = "select c_validcombination_id as uniquecode, sum(match_amt)-sum(holdamt) as per "
            + " from efin_rdvtxnline ln " + " where ln.efin_rdvtxn_id  = '" + version.getId()
            + "' and isadvance='N' and match_amt <> 0 group by c_validcombination_id "
            + " union all "
            + " select  b.uniquecode,sum(b.per) as per from( select a.uniquecode,sum(a.per) as per from ( "
            + " select c_validcombination_id as uniquecode, sum(adv_deduct)*-1 as per "
            + " from efin_rdvtxnline ln " + " where ln.efin_rdvtxn_id  = '" + version.getId()
            + "' and isadvance='N' and adv_deduct <> 0 group by c_validcombination_id " + " union "
            + " select ln.em_efin_c_validcombination_id as uniquecode,sum(linenetamt)*(cast(em_escm_advpaymnt_percntge as numeric)/100) as per from c_orderline ln "
            + " join c_order hd on hd.c_order_id = ln.c_order_id  where ln.c_order_id  = '"
            + rdv.getSalesOrder().getId() + "' "
            + " and em_escm_issummarylevel ='N' group by ln.em_efin_c_validcombination_id,em_escm_advpaymnt_percntge) a group by a.uniquecode ) b where b.per > 0 group by b.uniquecode "
            + " union all "
            + " select a.uniquecode,  sum(a.per) as per from ( select penalty_uniquecode as uniquecode, sum(penalty_amount)*-1 as per "
            + " from efin_rdvtxnline ln join efin_penalty_action pn on pn.efin_rdvtxnline_id = ln.efin_rdvtxnline_id "
            + " where ln.efin_rdvtxn_id  = '" + version.getId()
            + "' and isadvance='N' and penalty_amt <> 0 group by penalty_uniquecode ) a where a.per <> 0 group by a.uniquecode ";
        log4j.debug("RDV generate invoice mixed with advance and lines:" + query);
        sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query);
        queryList1 = sqlQuery1.list();
      } else if (sqlQuery1 != null && queryList1.size() == 1) {
        if (combiantion == 'Y') {
          // if advance alone
          if (rdv.getSalesOrder().getEscmBaseOrder() != null) {
            query = "select ln.em_efin_c_validcombination_id,"
                + " round(case when efin_tax_method_id is not  null then "
                + " sum(linenetamt)* ?  /(1+(";
            if (newTaxMethod != null)
              query += query += "cast(" + newTaxPercentage + "  as numeric)";
            else
              query += "tax.taxpercent";
            query += " /100))  else sum(linenetamt)*  ?  end,2)   as per,"
                + " cast ('N' as text) as istax from c_orderline ln "
                + " join c_order hd on hd.c_order_id = ln.c_order_id  "
                + " left join efin_tax_method tax on tax.efin_tax_method_id = hd.em_escm_tax_method_id "
                + " where ln.c_order_id  = '" + latestOrder.getId()
                + "' and em_escm_issummarylevel ='N' group by ln.em_efin_c_validcombination_id,em_escm_advpaymnt_percntge, tax.efin_tax_method_id"
                + " union all " + " select ln.em_efin_c_validcombination_id,"
                + " round(case when efin_tax_method_id is not  null then "
                + " sum(linenetamt) * ?  " + " - sum(linenetamt)* ? /(1+(";
            if (newTaxMethod != null)
              query += query += "cast(" + newTaxPercentage + "  as numeric)";
            else
              query += "tax.taxpercent";
            query += " /100))  else 0 end ,2)  as per,"
                + " cast ('Y' as text) as istax from c_orderline ln "
                + " join c_order hd on hd.c_order_id = ln.c_order_id  "
                + " left join efin_tax_method tax on tax.efin_tax_method_id = hd.em_escm_tax_method_id "
                + " where ln.c_order_id  = '" + latestOrder.getId()
                + "' and em_escm_issummarylevel ='N' group by ln.em_efin_c_validcombination_id,em_escm_advpaymnt_percntge,tax.efin_tax_method_id";

          } else {
            query = "select ln.em_efin_c_validcombination_id,"
                + " round(case when efin_tax_method_id is not  null then "
                + " sum(linenetamt) * ?  /(1+(";
            if (newTaxMethod != null)
              query += "cast(" + newTaxPercentage + "  as numeric)";
            else
              query += "tax.taxpercent";
            query += "/100)) " + " else sum(linenetamt) * ? end,2)   as per,"
                + " cast ('N' as text) as istax from c_orderline ln "
                + " join c_order hd on hd.c_order_id = ln.c_order_id  "
                + " left join efin_tax_method tax on tax.efin_tax_method_id = hd.em_escm_tax_method_id "
                + " where ln.c_order_id  = '" + latestOrder.getId()
                + "' and em_escm_issummarylevel ='N' group by ln.em_efin_c_validcombination_id,em_escm_advpaymnt_percntge,tax.efin_tax_method_id "
                + " union all " + " select ln.em_efin_c_validcombination_id,"
                + " round(case when efin_tax_method_id is not  null then " + " sum(linenetamt)* ? "
                + " - sum(linenetamt)* ? /(1+(";
            if (newTaxMethod != null)
              query += "cast(" + newTaxPercentage + "  as numeric)";
            else
              query += "tax.taxpercent";
            query += " /100))  else 0 end ,2)  as per,"
                + " cast ('Y' as text) as istax from c_orderline ln "
                + " join c_order hd on hd.c_order_id = ln.c_order_id  "
                + " left join efin_tax_method tax on tax.efin_tax_method_id = hd.em_escm_tax_method_id "
                + " where ln.c_order_id  = '" + latestOrder.getId()
                + "' and em_escm_issummarylevel ='N' group by ln.em_efin_c_validcombination_id,em_escm_advpaymnt_percntge,tax.efin_tax_method_id";
          }

          log4j.debug("RDV generate invoice advance alone:" + query);
          sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query);
          if (rdv.getSalesOrder().getEscmBaseOrder() != null) {
            sqlQuery1.setParameter(0, advPercentage);
            sqlQuery1.setParameter(1, advPercentage);
            sqlQuery1.setParameter(2, advPercentage);
            sqlQuery1.setParameter(3, advPercentage);
          } else {
            sqlQuery1.setParameter(0, advPercentage);
            sqlQuery1.setParameter(1, advPercentage);
            sqlQuery1.setParameter(2, advPercentage);
            sqlQuery1.setParameter(3, advPercentage);
          }
          queryList1 = sqlQuery1.list();

        } else {
          // if lines alone
          query = "select b.uniquecode, b.per,b.istax,b.bpartner from ((select h.uniquecode as uniquecode,sum(sum-per) as per, cast ('N' as text) as istax,'' as bpartner   from "
              + "   (select c_validcombination_id as uniquecode, sum(match_amt),0 as per  "
              + "    from efin_rdvtxnline ln  where ln.efin_rdvtxn_id  =  '" + version.getId() + "'"
              + "    and isadvance='N' and match_amt <> 0 group by c_validcombination_id "
              + "     union all "
              + "     select c_validcombination_id as uniquecode, 0 as sum,sum(holdamt) as per  "
              + "        from efin_rdvtxnline ln  where ln.efin_rdvtxn_id  = '" + version.getId()
              + "'" + "      and isadvance='N' and holdamt <> 0 group by c_validcombination_id"
              + " union all "
              + "    select c_validcombination_id as uniquecode, 0 as sum, sum(line_taxamt)   as per     "
              + "   from efin_rdvtxnline ln  where ln.efin_rdvtxn_id  = '" + version.getId()
              + "'  and isadvance='N' and line_taxamt <> 0 group by c_validcombination_id "
              + " union all "
              + " select txnln.c_validcombination_id as uniquecode ,0 as sum,sum(penalty_amount) as per from efin_penalty_action act "
              + " join efin_rdvtxnline txnln on txnln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
              + " join efin_penalty_types ptype on ptype.efin_penalty_types_id = act.efin_penalty_types_id "
              + " join EUT_Deflookups_TypeLn defln on defln.value = ptype.deductiontype and defln.Penalty_Logic = 'ECA' "
              + " where txnln.efin_rdvtxn_id = '" + version.getId()
              + "' group by txnln.c_validcombination_id " + " ) h "
              + "     group by h.uniquecode ) " + " union all "
              + " select c_validcombination_id as uniquecode, sum(adv_deduct)*-1 as per , cast ('N' as text) as istax,'' as bpartner   "
              + " from efin_rdvtxnline ln " + " where ln.efin_rdvtxn_id  = '" + version.getId()
              + "' and isadvance='N' and adv_deduct <> 0 group by c_validcombination_id "
              + " union all"
              + " select a.uniquecode,  sum(a.per) as per, cast ('N' as text) as istax,'' as bpartner  from ( select penalty_uniquecode as uniquecode, sum(penalty_amount)*-1 as per "
              + " from efin_rdvtxnline ln join efin_penalty_action pn on pn.efin_rdvtxnline_id = ln.efin_rdvtxnline_id "
              + " join efin_penalty_types ptype on ptype.efin_penalty_types_id = pn.efin_penalty_types_id "
              + " join EUT_Deflookups_TypeLn defln on defln.value = ptype.deductiontype and defln.Penalty_Logic <> 'ECA' "
              + " where ln.efin_rdvtxn_id  = '" + version.getId()
              + "' and isadvance='N' and penalty_amt <> 0 group by penalty_uniquecode) a where a.per <> 0 group by a.uniquecode"
              + "  union all "
              + " select tax.uniquecode,  sum(tax.per) as per, cast ('Y' as text) as istax,'' as bpartner  from ( select c_validcombination_id as uniquecode,";
          if (newTaxMethod != null)
            query += "sum(newtax_taxamt) ";
          else
            query += "sum(line_taxamt) ";
          query += "   as per   from efin_rdvtxnline ln   where ln.efin_rdvtxn_id  =  '"
              + version.getId()
              + "' and isadvance='N' and line_taxamt <> 0 group by c_validcombination_id) tax where tax.per <> 0 group by tax.uniquecode "
              + " union all "
              + " select epa.uniquecode,sum(epa.per) as per, cast ('N' as text) as istax,epa.c_bpartner_id as bpartner from ( "
              + " select act.penalty_uniquecode as uniquecode,case when   ord.em_escm_istax='Y' and tax.taxpercent is not null and tax.taxpercent>0  "
              + "    then round(((sum(penalty_amount))/(1+tax.taxpercent/100)),2) else "
              + "   sum(penalty_amount) end as per ,act.c_bpartner_id from efin_penalty_action act "
              + " join efin_rdvtxnline txnln on txnln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
              + "    left join c_orderline ordln on ordln.c_orderline_id= txnln.c_orderline_id "
              + "    left join c_order ord on ord.c_order_id= ordln.c_order_id "
              + "    left join efin_tax_method tax on tax.efin_tax_method_id= ord.em_escm_tax_method_id "
              + " join efin_penalty_types ptype on ptype.efin_penalty_types_id = act.efin_penalty_types_id "
              + " join EUT_Deflookups_TypeLn defln on defln.value = ptype.deductiontype and defln.Penalty_Logic = 'ECA' "
              + " where txnln.efin_rdvtxn_id = '" + version.getId() + "' "
              + " group by act.penalty_uniquecode,act.c_bpartner_id,tax.taxpercent, ord.em_escm_istax) epa where epa.per <> 0 group by epa.uniquecode,epa.c_bpartner_id "
              + " union all  "
              + " select  epatax.uniquecode,  sum(epatax.per) as per,  cast ('Y' as text) as istax,  epatax.c_bpartner_id as bpartner "
              + "  from " + "  (select  act.penalty_uniquecode as uniquecode, "
              + " case when   ord.em_escm_istax='Y' and tax.taxpercent is not null and tax.taxpercent>0  ";
          // + " then round(sum(penalty_amount)-((sum(penalty_amount))/(1+tax.taxpercent/100)),2)";
          if (newTaxMethod != null)
            query += "then round(((sum(penalty_amount))/(1+tax.taxpercent/100)) * (cast("
                + newTaxPercentage + "as numeric )/cast (100 as numeric)) ,2)";
          else
            query += " then round(sum(penalty_amount)-(((sum(penalty_amount))/(1+tax.taxpercent/100)) * cast(1 as numeric)),2) ";
          query += "   else " + " 0 end as per, act.c_bpartner_id "
              + "  from  efin_penalty_action act "
              + "    join efin_rdvtxnline txnln on txnln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
              + "    join c_orderline ordln on ordln.c_orderline_id= txnln.c_orderline_id "
              + "     join c_order ord on ord.c_order_id= ordln.c_order_id "
              + "     join efin_tax_method tax on tax.efin_tax_method_id= ord.em_escm_tax_method_id "
              + "  join efin_penalty_types ptype on ptype.efin_penalty_types_id = act.efin_penalty_types_id  "
              + "  join EUT_Deflookups_TypeLn defln on defln.value = ptype.deductiontype  "
              + "   and defln.Penalty_Logic = 'ECA'  " + " where txnln.efin_rdvtxn_id = '"
              + version.getId() + "'"
              + "  group by act.penalty_uniquecode,  act.c_bpartner_id ,tax.taxpercent, ord.em_escm_istax) epatax "

              + "  where epatax.per <> 0  " + "   group by  epatax.uniquecode,epatax.c_bpartner_id "

              + ") b where b.per <>0 order by b.per asc ";
          log4j.debug("RDV generate invoice  line alone:" + query);
          sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query);
          queryList1 = sqlQuery1.list();
        }
      }
      int line = 0;
      if (sqlQuery1 != null && queryList1.size() > 0) {
        for (Iterator iterator = queryList1.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          amt = new BigDecimal(row[1].toString());
          acctcode = row[0].toString();
          if (amt.compareTo(BigDecimal.ZERO) != 0) {
            GrandtotalAmt = GrandtotalAmt.add(amt);
            if (row.length > 3 && row[3] != null) {
              bpartner = OBDal.getInstance().get(BusinessPartner.class, row[3]);
            }
            AccountingCombination poAcct = OBDal.getInstance().get(AccountingCombination.class,
                acctcode);
            // amt = amt.setScale(2, RoundingMode.HALF_UP);
            // calculate new tax
            InvoiceLine invLine = OBProvider.getInstance().get(InvoiceLine.class);
            invLine.setInvoice(invoice);
            invLine.setOrganization(invoice.getOrganization());
            line = line + 10;
            invLine.setLineNo((long) line);
            invLine.setUnitPrice(amt);
            invLine.setEfinCValidcombination(poAcct);
            invLine.setEfinCSalesregion(poAcct.getSalesRegion());
            invLine.setEfinCElementvalue(poAcct.getAccount());
            invLine.setEfinCCampaign(poAcct.getSalesCampaign());
            invLine.setProject(poAcct.getProject());
            invLine.setEfinCActivity(poAcct.getActivity());
            invLine.setEfinCBpartner(poAcct.getBusinessPartner());
            invLine.setStDimension(poAcct.getStDimension());
            invLine.setNdDimension(poAcct.getNdDimension());
            invLine.setLineNetAmount(amt);
            invLine.setEfinAmtinvoiced(amt);
            invLine.setBusinessPartner(bpartner);
            JSONObject funds = CommonValidations.getFundsAvailable(invoice.getEfinBudgetint(),
                poAcct);
            if (poAcct != null && rdv.getBudgetInitialization() != null) {
              invLine.setEFINFundsAvailable(new BigDecimal(funds.getString("FA")));
            } else {
              invLine.setEFINFundsAvailable(new BigDecimal(funds.getString("FA")));
            }
            invLine.setAccount(AddFromAdvanceTypeDAO.getAccount());
            if (row[2] != null && row[2].equals("Y")) {
              invLine.setEFINIsTaxLine(true);
              if (newTaxMethod != null) {
                invLine.setBusinessPartner(newTaxMethod.getBusinessPartner());
              } else {
                invLine.setBusinessPartner(invoice.getEfinTaxMethod().getBusinessPartner());
              }
              invLine.setEfinSecondaryBeneficiary(
                  bpartner != null ? bpartner : invoice.getBusinessPartner());
              if (invoice.getEfinTaxAmount() == null
                  || invoice.getEfinTaxAmount().compareTo(BigDecimal.ZERO) == 0) {
                invoice.setEfinTaxAmount(amt);
              } else if (invoice.getEfinTaxAmount() != null
                  && invoice.getEfinTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setEfinTaxAmount(invoice.getEfinTaxAmount().add(amt));
              }
            }
            OBDal.getInstance().save(invLine);
            invoice.getInvoiceLineList().add(invLine);
            OBDal.getInstance().save(invoice);

            // Get the funds budget funds available for the cost account
            if (invLine.getInvoice().getEfinBudgetType().equals("C")) {
              AccountingCombination accCombination = OBDal.getInstance()
                  .get(AccountingCombination.class, invLine.getEfinCValidcombination().getId());
              EfinBudgetIntialization budgetIntialization = Utility.getObject(
                  EfinBudgetIntialization.class, invLine.getInvoice().getEfinBudgetint().getId());
              if (accCombination.getEfinFundscombination() != null) {
                fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
                    accCombination.getEfinFundscombination());
                if (fundsCheckingObject != null && fundsCheckingObject.length() > 0
                    && fundsCheckingObject.has("FA")) {
                  fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
                }
                invLine.setEfinFbFundsAvailable(fundsAvailable);
              } else {
                invLine.setEfinFbFundsAvailable(fundsAvailable);
              }
            } else {
              invLine.setEfinFbFundsAvailable(null);
            }

            OBDal.getInstance().flush();
            total = total.add(amt);
          }
        }
      }

      BigDecimal externalAmt = BigDecimal.ZERO, externalAmtWithoutTax = BigDecimal.ZERO;
      String sql = " select coalesce(sum(penalty_amount),0) from efin_penalty_action act where act.efin_rdvtxnline_id in ( select line.efin_rdvtxnline_id from efin_rdvtxnline line where line.efin_rdvtxn_id "
          + "  =? ) and act.efin_penalty_types_id in ( select penalty.efin_penalty_types_id from efin_penalty_types penalty where penalty.deductiontype "
          + " in ( select value from eut_deflookups_typeln where penalty_logic='ECA' and  penalty_logic is not null) ) ";
      Query qry = OBDal.getInstance().getSession().createSQLQuery(sql);
      qry.setParameter(0, version.getId());
      if (qry != null && qry.list().size() > 0) {
        externalAmt = (BigDecimal) qry.list().get(0);
      }
      BigDecimal diffAmtRDVAndInv = BigDecimal.ZERO;
      if (newTaxMethod != null) {
        if (latestOrder.getEscmTaxMethod() != null) {
          externalAmtWithoutTax = externalAmt.divide(
              BigDecimal.ONE.add(new BigDecimal(latestOrder.getEscmTaxMethod().getTaxpercent())
                  .divide(new BigDecimal(100))),
              15, RoundingMode.HALF_UP);

          externalAmt = externalAmtWithoutTax.add((externalAmtWithoutTax)
              .multiply(new BigDecimal(newTaxMethod.getTaxpercent()).divide(new BigDecimal(100))));
        }
        diffAmtRDVAndInv = (version.getNewtaxNetmatchAmt().add(externalAmt))
            .subtract(GrandtotalAmt);
      } else {
        diffAmtRDVAndInv = (version.getNetmatchAmt().add(externalAmt)).subtract(GrandtotalAmt);
      }
      if (diffAmtRDVAndInv.compareTo(BigDecimal.ZERO) != 0) {

        InvoiceLine lineObj = Collections.max(invoice.getInvoiceLineList(),
            Comparator.comparing(s -> s.getLineNetAmount()));
        if ((diffAmtRDVAndInv.abs()).compareTo(lineObj.getLineNetAmount().abs()) < 0) {
          lineObj.setLineNetAmount(lineObj.getLineNetAmount().add(diffAmtRDVAndInv));
          lineObj.setGrossUnitPrice(lineObj.getLineNetAmount().add(diffAmtRDVAndInv));
          lineObj.setEfinAmtinvoiced(lineObj.getEfinAmtinvoiced().add(diffAmtRDVAndInv));
          OBDal.getInstance().save(lineObj);
        } else {
          OBDal.getInstance().rollbackAndClose();
          throw new OBException(OBMessageUtils.messageBD("EFIN_RdvGentAmsIssue"));
        }
      }

      if (newTaxMethod != null) {
        invoice.setEfinTaxMethod(newTaxMethod);
        OBDal.getInstance().save(invoice);
      }

      return invoice;
    } catch (

    Exception e) {
      log4j.error(" Exception while inserting invoice fro rdv: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static Invoice insertInvoiceLineLevel(AccountingCombination uniqueCode,
      EfinRDVTxnline trxLine, EfinRDV rdv, String orgId, String txnType,
      FIN_PaymentMethod payMethod, PaymentTerm payTerm, PriceList prcList, DocumentType transDoc) {

    try {
      OBContext.setAdminMode();

      // find Budget Definition Based On Date

      String budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(new Date(),
          rdv.getClient().getId(), "");

      String query = null;
      Query sqlQuery1 = null;
      List queryList1 = null;
      String acctcode = "";
      BigDecimal amt = BigDecimal.ZERO, total = BigDecimal.ZERO;
      BigDecimal GrandtotalAmt = BigDecimal.ZERO;
      BusinessPartner bpartner = null;
      Invoice invoice = OBProvider.getInstance().get(Invoice.class);
      invoice.setOrganization(trxLine.getOrganization());
      invoice.setTransactionDocument(transDoc);
      BigDecimal fundsAvailable = BigDecimal.ZERO;
      JSONObject fundsCheckingObject = null;
      DocumentType doctype = OBDal.getInstance().get(DocumentType.class, "0");
      invoice.setDocumentType(doctype);
      OBQuery<SalesRegion> dept = OBDal.getInstance().createQuery(SalesRegion.class,
          "organization.id= :orgID");
      dept.setNamedParameter("orgID", orgId);
      dept.setMaxResult(1);
      invoice.setEfinCSalesregion(dept.list().get(0));
      // invoice.setEfinCSalesregion();
      invoice.setBusinessPartner(rdv.getBusinessPartner());
      invoice.setPartnerAddress(rdv.getBusinessPartner().getBusinessPartnerLocationList().get(0));
      invoice.setEfinIban(rdv.getPartnerBankAccount());
      invoice.setInvoiceDate(new Date());
      invoice.setAccountingDate(new Date());
      invoice.setEfinEncumtype("A");
      invoice.setEfinEncumbranceType("AEE");
      invoice
          .setEfinBudgetint(OBDal.getInstance().get(EfinBudgetIntialization.class, budgInitialId));
      invoice.setEfinRdvtxn(trxLine.getEfinRdvtxn());
      invoice.setEfinRdvtxnline(trxLine);
      invoice.setPaymentMethod(payMethod);
      invoice.setPaymentTerms(payTerm);
      Currency currency = OBDal.getInstance().get(Currency.class, "317");
      invoice.setCurrency(currency);
      invoice.setEfinBudgetType(uniqueCode.getSalesCampaign().getEfinBudgettype());
      invoice.setEfinCSalesregion(uniqueCode.getSalesRegion());
      invoice.setEfinIsrdv(true);
      invoice.setEfinAdRole(OBContext.getOBContext().getRole());
      invoice.setSalesTransaction(false);
      invoice.setPriceList(prcList);
      invoice.setEfinInvoicetypeTxt(RDV_DOCUMENT);
      if (!rdv.getTXNType().equals("POD")) {
        invoice.setSalesOrder(rdv.getSalesOrder());
        if (rdv.getSalesOrder() != null) {
          EscmCOrderV ord = OBDal.getInstance().get(EscmCOrderV.class, rdv.getSalesOrder().getId());
          invoice.setEfinCOrder(ord);
          // if (version.getEfinRDVTxnlineList().get(0).getSalesOrderLine() != null && version
          // .getEfinRDVTxnlineList().get(0).getSalesOrderLine().getSalesOrder().isEscmIstax()) {
          Order latestOrder = PurchaseInvoiceSubmitUtils
              .getLatestOrderComplete(rdv.getSalesOrder());
          if (latestOrder != null && latestOrder.isEscmIstax()) {
            invoice.setEfinIstax(true);
            invoice.setEfinTaxMethod(latestOrder.getEscmTaxMethod());
            invoice.setEfinIstaxpo(true);
          }
        }
        invoice.setEfinSupcontractexpdate(rdv.getEXPDate());
      }
      // Set Attachment Reference in Invoice based on the PO Number incase of Mustakals PO and PO
      // Receipt
      if ((rdv.getTXNType().equals("PO") || rdv.getTXNType().equals("POS"))
          && (rdv.getSalesOrder().getEscmReferenceNo() != null))
        invoice.setEfinAttachementref(rdv.getSalesOrder().getEscmReferenceNo());

      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();

      BigDecimal poTotalAmt = BigDecimal.ZERO, newTaxPercentage = BigDecimal.ZERO;

      // take latest orderId
      Order latestOrder = PurchaseInvoiceSubmitUtils.getLatestOrderComplete(rdv.getSalesOrder());

      if (latestOrder != null) {
        poTotalAmt = latestOrder.getGrandTotalAmount();
      }

      EFIN_TaxMethod newTaxMethod = calculateNewTax(trxLine.getEfinRdvtxn(), amt);
      if (newTaxMethod != null) {
        newTaxPercentage = new BigDecimal(newTaxMethod.getTaxpercent());
      }

      BigDecimal advPercentage = (trxLine.getNetmatchAmt().divide(poTotalAmt, 15,
          RoundingMode.HALF_UP));
      if (newTaxMethod != null) {
        advPercentage = (trxLine.getNewtaxNetmatchAmt().divide(poTotalAmt, 15,
            RoundingMode.HALF_UP));
      }

      // if advance alone
      query = "select ln.em_efin_c_validcombination_id,"
          + " round(case when efin_tax_method_id is not  null then " + " sum(linenetamt)* ?/(1+(";

      if (newTaxMethod != null)
        query += query += "cast(" + newTaxPercentage + "  as numeric)";
      else
        query += "tax.taxpercent";
      query += " /100))  else sum(linenetamt)* ? end ,2)  as per,"
          + " cast ('N' as text) as istax from c_orderline ln "
          + " join c_order hd on hd.c_order_id = ln.c_order_id  "
          + " left join efin_tax_method tax on tax.efin_tax_method_id = hd.em_escm_tax_method_id "
          + " where ln.c_order_id  = ?  "
          + "  and em_escm_issummarylevel ='N' group by ln.em_efin_c_validcombination_id,em_escm_advpaymnt_percntge, tax.efin_tax_method_id"
          + " union all " + " select ln.em_efin_c_validcombination_id,"
          + " round (case when efin_tax_method_id is not  null then " + " sum(linenetamt)*  ?  "
          + " - sum(linenetamt)* ? /(1+(";

      if (newTaxMethod != null)
        query += query += "cast(" + newTaxPercentage + "  as numeric)";
      else
        query += "tax.taxpercent";
      query += " /100)) else 0 end ,2)  as per,"
          + " cast ('Y' as text) as istax from c_orderline ln "
          + " join c_order hd on hd.c_order_id = ln.c_order_id  "
          + " left join efin_tax_method tax on tax.efin_tax_method_id = hd.em_escm_tax_method_id "
          + " where ln.c_order_id  = ? "
          + " and em_escm_issummarylevel ='N' group by ln.em_efin_c_validcombination_id,em_escm_advpaymnt_percntge,tax.efin_tax_method_id";

      log4j.debug("RDV generate invoice advance alone:" + query);
      sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery1.setParameter(0, advPercentage);
      sqlQuery1.setParameter(1, advPercentage);
      sqlQuery1.setParameter(2, latestOrder.getId());
      // if (rdv.getSalesOrder().getEscmBaseOrder() != null) {
      // sqlQuery1.setParameter(2, rdv.getSalesOrder().getEscmBaseOrder().getId());
      // } else {
      // sqlQuery1.setParameter(2, rdv.getSalesOrder().getId());
      // }
      sqlQuery1.setParameter(3, advPercentage);
      sqlQuery1.setParameter(4, advPercentage);
      sqlQuery1.setParameter(5, latestOrder.getId());
      // if (rdv.getSalesOrder().getEscmBaseOrder() != null) {
      // sqlQuery1.setParameter(5, rdv.getSalesOrder().getEscmBaseOrder().getId());
      // } else {
      // sqlQuery1.setParameter(5, rdv.getSalesOrder().getId());
      // }

      queryList1 = sqlQuery1.list();
      int line = 0;
      if (sqlQuery1 != null && queryList1.size() > 0) {
        for (Iterator iterator = queryList1.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          amt = new BigDecimal(row[1].toString());
          acctcode = row[0].toString();
          if (amt.compareTo(BigDecimal.ZERO) != 0) {
            GrandtotalAmt = GrandtotalAmt.add(amt);
            if (row.length > 3 && row[3] != null) {
              bpartner = OBDal.getInstance().get(BusinessPartner.class, row[3]);
            }
            AccountingCombination poAcct = OBDal.getInstance().get(AccountingCombination.class,
                acctcode);
            // amt = amt.setScale(2, RoundingMode.HALF_UP);
            InvoiceLine invLine = OBProvider.getInstance().get(InvoiceLine.class);
            invLine.setInvoice(invoice);
            invLine.setOrganization(invoice.getOrganization());
            line = line + 10;
            invLine.setLineNo((long) line);
            invLine.setUnitPrice(amt);
            invLine.setEfinCValidcombination(poAcct);
            invLine.setEfinCSalesregion(poAcct.getSalesRegion());
            invLine.setEfinCElementvalue(poAcct.getAccount());
            invLine.setEfinCCampaign(poAcct.getSalesCampaign());
            invLine.setProject(poAcct.getProject());
            invLine.setEfinCActivity(poAcct.getActivity());
            invLine.setEfinCBpartner(poAcct.getBusinessPartner());
            invLine.setStDimension(poAcct.getStDimension());
            invLine.setNdDimension(poAcct.getNdDimension());
            invLine.setLineNetAmount(amt);
            invLine.setEfinAmtinvoiced(amt);
            invLine.setBusinessPartner(bpartner);
            JSONObject funds = CommonValidations.getFundsAvailable(rdv.getBudgetInitialization(),
                poAcct);
            if (poAcct != null && rdv.getBudgetInitialization() != null) {
              invLine.setEFINFundsAvailable(new BigDecimal(funds.getString("FA")));
            } else {
              invLine.setEFINFundsAvailable(new BigDecimal(funds.getString("FA")));
            }
            invLine.setAccount(AddFromAdvanceTypeDAO.getAccount());
            if (row[2] != null && row[2].equals("Y")) {
              invLine.setEFINIsTaxLine(true);
              if (newTaxMethod != null) {
                invLine.setBusinessPartner(newTaxMethod.getBusinessPartner());
              } else {
                invLine.setBusinessPartner(invoice.getEfinTaxMethod().getBusinessPartner());
              }
              invLine.setEfinSecondaryBeneficiary(invoice.getBusinessPartner());
              if (invoice.getEfinTaxAmount() == null
                  || invoice.getEfinTaxAmount().compareTo(BigDecimal.ZERO) == 0) {
                invoice.setEfinTaxAmount(amt);
              } else if (invoice.getEfinTaxAmount() != null
                  && invoice.getEfinTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setEfinTaxAmount(invoice.getEfinTaxAmount().add(amt));
              }
            }
            OBDal.getInstance().save(invLine);
            invoice.getInvoiceLineList().add(invLine);
            OBDal.getInstance().save(invoice);

            // Get the funds budget funds available for the cost account
            if (invLine.getInvoice().getEfinBudgetType().equals("C")) {
              AccountingCombination accCombination = OBDal.getInstance()
                  .get(AccountingCombination.class, invLine.getEfinCValidcombination().getId());
              EfinBudgetIntialization budgetIntialization = Utility.getObject(
                  EfinBudgetIntialization.class, invLine.getInvoice().getEfinBudgetint().getId());
              if (accCombination.getEfinFundscombination() != null) {
                fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
                    accCombination.getEfinFundscombination());
                if (fundsCheckingObject != null && fundsCheckingObject.length() > 0
                    && fundsCheckingObject.has("FA")) {
                  fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
                }
                invLine.setEfinFbFundsAvailable(fundsAvailable);
              } else {
                invLine.setEfinFbFundsAvailable(fundsAvailable);
              }
            } else {
              invLine.setEfinFbFundsAvailable(null);
            }

            OBDal.getInstance().flush();
            total = total.add(amt);
          }
        }
      }

      BigDecimal diffAmtRDVAndInv = trxLine.getNetmatchAmt().subtract(GrandtotalAmt);

      if (diffAmtRDVAndInv.compareTo(BigDecimal.ZERO) != 0) {

        InvoiceLine lineObj = Collections.max(invoice.getInvoiceLineList(),
            Comparator.comparing(s -> s.getLineNetAmount()));
        lineObj.setLineNetAmount(lineObj.getLineNetAmount().add(diffAmtRDVAndInv));
        lineObj.setGrossUnitPrice(lineObj.getLineNetAmount().add(diffAmtRDVAndInv));
        lineObj.setEfinAmtinvoiced(lineObj.getEfinAmtinvoiced().add(diffAmtRDVAndInv));
        OBDal.getInstance().save(lineObj);
      }
      if (newTaxMethod != null) {
        invoice.setEfinTaxMethod(newTaxMethod);
        OBDal.getInstance().save(invoice);
      }

      return invoice;
    } catch (Exception e) {
      log4j.error(" Exception while inserting invoice fro rdv: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Create new encumbrance for splitted and reduce amount from old encum.
   * 
   * @param invoice
   * @param uniqueCode
   * @param version
   * @param rdv
   * @param orgId
   * @return
   */
  public static int insertEncum(Invoice invoice, AccountingCombination uniqueCode,
      EfinRDVTransaction version, EfinRDV rdv, String orgId) {
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    JSONObject uniqueCodeListJson = new JSONObject(), json = null, lineListJson = null;
    InvoiceLine invline = null;
    BigDecimal amount = BigDecimal.ZERO;
    Long lineNo = (long) 10;
    try {
      OBContext.setAdminMode();

      EfinBudgetManencum encum = OBProvider.getInstance().get(EfinBudgetManencum.class);
      encum.setSalesCampaign(uniqueCode.getSalesCampaign());
      encum.setEncumType("POE");
      encum.setSalesRegion(invoice.getEfinCSalesregion());
      encum.setEncumMethod("A");
      encum.setEncumStage("MUS");
      encum.setOrganization(invoice.getOrganization());
      encum.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setBudgetInitialization(invoice.getEfinBudgetint());
      encum.setAction("PD");
      encum.setBusinessPartner(invoice.getBusinessPartner());
      encum.setDescription(rdv.getSalesOrder().getEfinBudgetManencum() != null
          ? rdv.getSalesOrder().getEfinBudgetManencum().getDocumentNo()
          : "-");

      OBDal.getInstance().save(encum);
      OBDal.getInstance().flush();

      /**
       * Task no - 7364 fetching uniquecode list from the invoiceline- group the uniquecode value
       **/
      uniqueCodeListJson = getLineUniquecodeGrp(invoice);

      /** Task no - 7364 iterate the uniquecode json list **/
      if (uniqueCodeListJson != null && uniqueCodeListJson.length() > 0) {
        JSONArray uniquearray = uniqueCodeListJson.getJSONArray("uniquecodeList");
        for (int i = 0; i < uniquearray.length(); i++) {
          json = uniquearray.getJSONObject(i);
          EfinBudManencumRev manEncumRev = null;

          if (!json.getString("DimensionType").equals("A")) {
            amount = new BigDecimal(json.getString("Amount"));
            AccountingCombination acctcom = OBDal.getInstance().get(AccountingCombination.class,
                json.getString("Uniquecode"));

            /**
             * insert modification-- change the new modification function because of Task no - 7364
             **/
            manEncumRev = insertRevModifiaction(acctcom, amount, rdv);

            /** insert new encum lines under the new encumbrance created for invoice **/
            EfinBudgetManencumlines encumLines = OBProvider.getInstance()
                .get(EfinBudgetManencumlines.class);
            encumLines.setManualEncumbrance(encum);
            encumLines.setLineNo(lineNo);
            encumLines.setAmount(amount);
            encumLines.setUsedAmount(BigDecimal.ZERO);
            encumLines.setRemainingAmount(BigDecimal.ZERO);// ------
            encumLines.setAPPAmt(amount);// ----------
            log4j.debug("app:" + amount);
            encumLines.setRevamount(amount);
            encumLines.setOrganization(invoice.getOrganization());
            encumLines.setSalesRegion(acctcom.getSalesRegion());
            encumLines.setAccountElement(acctcom.getAccount());
            encumLines.setSalesCampaign(acctcom.getSalesCampaign());
            encumLines.setProject(acctcom.getProject());
            encumLines.setActivity(acctcom.getActivity());
            encumLines.setStDimension(acctcom.getStDimension());
            encumLines.setNdDimension(acctcom.getNdDimension());
            encumLines.setBusinessPartner(acctcom.getBusinessPartner());
            encumLines.setAccountingCombination(acctcom);
            encumLines.setUniqueCodeName(acctcom.getEfinUniquecodename());
            encum.getEfinBudgetManencumlinesList().add(encumLines);
            OBDal.getInstance().save(encum);
            OBDal.getInstance().save(encumLines);

            lineNo += 10;
            /** update the created new manual encumbrance line id in the invoiceline **/
            JSONArray InvLineArray = json.getJSONArray("lineList");
            for (int j = 0; j < InvLineArray.length(); j++) {
              lineListJson = InvLineArray.getJSONObject(j);
              invline = OBDal.getInstance().get(InvoiceLine.class,
                  lineListJson.getString("InvlineId"));
              invline.setEfinBudgmanuencumln(encumLines);
              OBDal.getInstance().save(invline);
            }
            /**
             * update the source manual encumbrance(new encum line) line in modification entry of
             * old encumbrance
             **/
            manEncumRev.setSRCManencumline(encumLines);
            OBDal.getInstance().save(manEncumRev);
            OBDal.getInstance().flush();

          }
        }
      }
      encum.setDocumentStatus("CO");
      OBDal.getInstance().save(encum);
      String encumid = encum.getId();
      EfinBudgetManencumv manenc = OBDal.getInstance().get(EfinBudgetManencumv.class, encumid);
      invoice.setEfinManualencumbrance(manenc);
      OBDal.getInstance().save(invoice);

      // negative entry in old encum
      /*
       * for (EfinBudgetManencumlines newLine : encum.getEfinBudgetManencumlinesList()) { for
       * (EfinBudgetManencumlines oldLine : oldencum.getEfinBudgetManencumlinesList()) { if
       * (oldLine.getAccountingCombination() == newLine.getAccountingCombination()) {
       * EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
       * 
       * oldLine.setAPPAmt(oldLine.getAPPAmt().subtract(newLine.getAPPAmt()));
       * OBDal.getInstance().save(oldLine);
       * 
       * // insert into Manual Encumbrance Revision Table
       * manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
       * manEncumRev.setOrganization( OBDal.getInstance().get(Organization.class,
       * oldLine.getOrganization().getId())); manEncumRev.setActive(true);
       * manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
       * manEncumRev.setCreationDate(new java.util.Date());
       * manEncumRev.setCreatedBy(OBContext.getOBContext().getUser()); manEncumRev.setUpdated(new
       * java.util.Date()); manEncumRev.setUniqueCode(oldLine.getUniquecode());
       * manEncumRev.setManualEncumbranceLines(oldLine); manEncumRev.setRevdate(new
       * java.util.Date()); manEncumRev.setStatus("APP"); manEncumRev.setAuto(true);
       * manEncumRev.setRevamount(newLine.getAPPAmt().negate()); log4j.debug("rev:" +
       * manEncumRev.getRevamount());
       * manEncumRev.setAccountingCombination(oldLine.getAccountingCombination());
       * manEncumRev.setSRCManencumline(newLine); manEncumRev.setEncumbranceType("AAE");
       * OBDal.getInstance().save(manEncumRev); OBDal.getInstance().flush(); } } }
       */

    } catch (OBException e) {
      log4j.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log4j.error(" Exception while inserting invoice for rdv: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return 0;

  }

  /**
   * Task no - 7364 get uniquecode json - group by of uniquecode and sum of the line net amount
   * 
   * @param invoice
   * @return
   */
  public static JSONObject getLineUniquecodeGrp(Invoice invoice) {
    JSONObject uniquecodeList = new JSONObject(), json = null, existJson = null,
        lineListJson = null;
    JSONArray lineListArray = null;
    JSONArray uniqueCodeListArray = new JSONArray();
    Boolean sameUniqueCode = false;
    try {
      OBContext.setAdminMode();

      /** iterate the invoice line list based on created new amarsarf **/
      for (InvoiceLine line : invoice.getInvoiceLineList()) {
        sameUniqueCode = false;
        /** only expense line iterate **/
        if ("E".equals(line.getEfinCValidcombination().getEfinDimensiontype())) {

          /**
           * check unqiuecode array present and already same uniquecode is added in the json list or
           * not
           **/
          if (uniqueCodeListArray != null && uniqueCodeListArray.length() > 0) {
            for (int i = 0; i < uniqueCodeListArray.length(); i++) {

              existJson = uniqueCodeListArray.getJSONObject(i);
              /** if already uniquecode present in json add the amount on existing json **/
              if (existJson.getString("Uniquecode")
                  .equals(line.getEfinCValidcombination().getId())) {
                existJson.put("Amount",
                    new BigDecimal(existJson.getString("Amount")).add(line.getLineNetAmount())
                        .setScale(invoice.getCurrency().getStandardPrecision().intValue(),
                            RoundingMode.HALF_UP));
                lineListJson = new JSONObject();
                lineListJson.put("InvlineId", line.getId());
                lineListArray = existJson.getJSONArray("lineList");
                lineListArray.put(lineListJson);
                // existJson.put("lineList", lineListArray);
                sameUniqueCode = true;
                break;
              } else
                continue;
            }
          }
          /** if unqiuecode not present add new json object of uniquecode in json array **/
          if (!sameUniqueCode) {
            lineListJson = new JSONObject();
            lineListJson.put("Amount", line.getLineNetAmount().setScale(
                invoice.getCurrency().getStandardPrecision().intValue(), RoundingMode.HALF_UP));
            lineListJson.put("Uniquecode", line.getEfinCValidcombination().getId());
            lineListJson.put("DimensionType",
                line.getEfinCValidcombination().getEfinDimensiontype());
            lineListArray = new JSONArray();
            json = new JSONObject();
            json.put("InvlineId", line.getId());
            lineListArray.put(json);

            lineListJson.put("lineList", lineListArray);

            uniqueCodeListArray.put(lineListJson);
          }
        }
      }
      uniquecodeList.put("uniquecodeList", uniqueCodeListArray);
    } catch (Exception e) {
      log4j.error(" Exception while getLineUniquecodeGrp for rdv: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return uniquecodeList;
  }

  /**
   * Task no - 7364 insert the modification in old encumbrance while doing generate amarsarf
   * 
   * @param combination
   * @param amount
   * @param rdv
   * @return
   */
  public static EfinBudManencumRev insertRevModifiaction(AccountingCombination combination,
      BigDecimal amount, EfinRDV rdv) {
    EfinBudManencumRev manEncumRev = null;
    try {
      OBContext.setAdminMode();
      OBInterceptor.setPreventUpdateInfoChange(true);

      OBQuery<EfinBudgetManencumlines> oldencumlineexist = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          "as e where e.manualEncumbrance.id='"
              + rdv.getSalesOrder().getEfinBudgetManencum().getId()
              + "' and e.accountingCombination.id ='" + combination.getId() + "'");
      if (oldencumlineexist.list() != null && oldencumlineexist.list().size() > 0) {
        EfinBudgetManencumlines encumLines = oldencumlineexist.list().get(0);

        // if auto encum in PO then insert extra amount in po encum from budget enquiry
        if (rdv.getSalesOrder().getEfinBudgetManencum().getEncumMethod().equals("A")) {
          if (amount.compareTo(encumLines.getAPPAmt()) > 0) {
            PurchaseInvoiceSubmitUtils.modifyPOEncumbranceRdvExclusiveTax(combination.getId(),
                amount.subtract(encumLines.getAPPAmt()),
                rdv.getSalesOrder().getEfinBudgetManencum());
          }
        }

        encumLines.setAPPAmt(encumLines.getAPPAmt().subtract(amount));
        OBDal.getInstance().save(encumLines);
        OBDal.getInstance().flush();
        OBInterceptor.setPreventUpdateInfoChange(false);

        manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
        manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
        manEncumRev.setOrganization(
            OBDal.getInstance().get(Organization.class, encumLines.getOrganization().getId()));
        manEncumRev.setActive(true);
        manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
        manEncumRev.setCreationDate(new java.util.Date());
        manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
        manEncumRev.setUpdated(new java.util.Date());
        manEncumRev.setUniqueCode(encumLines.getUniquecode());
        manEncumRev.setManualEncumbranceLines(encumLines);
        manEncumRev.setRevdate(new java.util.Date());
        manEncumRev.setStatus("APP");
        manEncumRev.setAuto(true);
        manEncumRev.setSystem(true);
        manEncumRev.setRevamount(amount.negate());
        log4j.debug("rev:" + manEncumRev.getRevamount());
        manEncumRev.setAccountingCombination(encumLines.getAccountingCombination());
        // manEncumRev.setSRCManencumline(encumLines);
        manEncumRev.setEncumbranceType("AEE");
        OBDal.getInstance().save(manEncumRev);
        OBDal.getInstance().flush();
        return manEncumRev;
      }

    } catch (Exception e) {
      log4j.error(" Exception while insert modification for rdv old encum: " + e);
      throw new OBException(e.getMessage());
    } finally {

    }
    return manEncumRev;
  }

  /**
   * 
   * @param invoice
   * @param rdv
   * @return
   */
  public static int encumStageMove(Invoice invoice, EfinRDV rdv) {

    try {
      OBContext.setAdminMode();
      String encumid = rdv.getSalesOrder().getEfinBudgetManencum().getId();
      EfinBudgetManencumv manenc = OBDal.getInstance().get(EfinBudgetManencumv.class, encumid);
      invoice.setEfinManualencumbrance(manenc);
      invoice.setEfinEncumtype(rdv.getSalesOrder().getEfinBudgetManencum() != null
          ? rdv.getSalesOrder().getEfinBudgetManencum().getEncumMethod()
          : rdv.getSalesOrder().getEFINEncumbranceMethod());
      OBDal.getInstance().save(invoice);
      EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class, encumid);
      encum.setEncumStage("MUS");
      OBDal.getInstance().save(encum);

      // insert inv ref entry
      for (InvoiceLine ln : invoice.getInvoiceLineList()) {
        for (EfinBudgetManencumlines encLine : encum.getEfinBudgetManencumlinesList()) {
          if (ln.getEfinCValidcombination() == encLine.getAccountingCombination()) {
            ln.setEfinBudgmanuencumln(encLine);
            OBDal.getInstance().save(ln);
          }
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while encumStageMove for rdv: " + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return 0;
  }

  /**
   * 
   * @param orgId
   * @param ClientId
   * @param vars
   * @param invoice
   * @param version
   * @return
   */
  // This function is used to insert alert for rdv manager after create invoice successfully
  public static int insertAlertforRDVManager(String orgId, String ClientId, VariablesSecureApp vars,
      Invoice invoice, EfinRDVTransaction version) {
    String alertWindow = AlertWindow.RDVManager;
    String Description = sa.elm.ob.finance.properties.Resource
        .getProperty("finance.rdvmanager.success", vars.getLanguage()) + " "
        + version.getTXNVersion() + " - " + version.getEfinRdv().getDocumentNo();
    String userId = "";
    String roleId = "";
    String alertRuleId = "";
    ArrayList<RDVManagerVO> includereceipient = new ArrayList<RDVManagerVO>();
    RDVManagerVO vo = null;
    boolean errorFlag = false;
    try {
      OBContext.setAdminMode();

      // getting alert receipient
      OBQuery<AlertRule> alertrule = OBDal.getInstance().createQuery(AlertRule.class,
          " as e where e.client.id='" + ClientId + "' and e.efinProcesstype='" + alertWindow + "'");
      if (alertrule.list().size() > 0) {
        alertRuleId = alertrule.list().get(0).getId();
      }
      OBQuery<AlertRecipient> alertrec = OBDal.getInstance().createQuery(AlertRecipient.class,
          " as e where e.alertRule.id='" + alertRuleId + "'");
      if (alertrec.list().size() > 0) {
        for (AlertRecipient rec : alertrec.list()) {

          /*
           * if (rec.getUserContact() != null) { OBCriteria<UserRoles> userRolesCriteria =
           * OBDal.getInstance() .createCriteria(UserRoles.class);
           * userRolesCriteria.add(Restrictions.eq(AlertRecipient.PROPERTY_ROLE, rec.getRole()));
           * userRolesCriteria.add( Restrictions.eq(AlertRecipient.PROPERTY_USERCONTACT,
           * rec.getUserContact()));
           * 
           * if (userRolesCriteria.list() != null && userRolesCriteria.list().size() > 0) { vo = new
           * RDVManagerVO(rec.getRole().getId(), rec.getUserContact().getId()); } else { vo = new
           * RDVManagerVO(rec.getRole().getId(), "0"); }
           * 
           * } else { vo = new RDVManagerVO(rec.getRole().getId(), "0"); }
           * includereceipient.add(vo);
           */
          OBDal.getInstance().remove(rec);
        }
      }
      // getting the rdv manager in role window based on org access
      String alertQuery = "select ad_role.ad_role_id from ad_role left join AD_Role_OrgAccess on \n"
          + "ad_role.ad_role_id = AD_Role_OrgAccess.ad_role_id\n"
          + "where ad_role.em_efin_rdvmanager = 'Y' and \n" + "AD_Role_OrgAccess.ad_org_id = '"
          + orgId + "' \n" + "and ad_role.ad_client_id = '" + ClientId + "'";
      SQLQuery Query = OBDal.getInstance().getSession().createSQLQuery(alertQuery);
      @SuppressWarnings("rawtypes")
      List RdvList = Query.list();
      if (RdvList != null && RdvList.size() > 0) {
        for (int i = 0; i < RdvList.size(); i++) {
          Object objRdvList = RdvList.get(i);
          roleId = objRdvList.toString();
          OBQuery<UserRoles> userQuery = OBDal.getInstance().createQuery(UserRoles.class,
              "role.id='" + objRdvList.toString() + "'");
          List<UserRoles> userList = userQuery.list();
          for (UserRoles usrRole : userList) {
            userId = usrRole.getUserContact().getId();
            errorFlag = true;
            vo = new RDVManagerVO(roleId, userId);
            includereceipient.add(vo);
          }

        }

      }
      // avoid duplicate recipient
      Set<RDVManagerVO> s = new HashSet<RDVManagerVO>();
      s.addAll(includereceipient);
      includereceipient = new ArrayList<RDVManagerVO>();
      includereceipient.addAll(s);

      // insert alert receipients
      for (RDVManagerVO vo1 : includereceipient) {

        if (vo1.getUserId().equals("0")) {
          AlertUtility.insertAlertRecipient(vo1.getRoleId(), null, ClientId, alertWindow);
        }

        else {
          AlertUtility.insertAlertRecipient(vo1.getRoleId(), vo1.getUserId(), ClientId,
              alertWindow);
        }
      }

      if (errorFlag) {
        // Make solve the previous alert.
        OBQuery<Alert> resolveQuery = OBDal.getInstance().createQuery(Alert.class,
            "as e where e.alertRule.id='" + alertRuleId
                + "' and e.alertStatus='NEW' and e.client.id = '" + ClientId + "'");
        if (resolveQuery.list().size() > 0) {
          for (Alert objAlert : resolveQuery.list()) {
            objAlert.setAlertStatus("SOLVED");
          }

        }
        // insert alert in alert window
        AlertUtility.alertInsertionRole(invoice.getId(), invoice.getDocumentNo(), "", "",
            invoice.getClient().getId(), Description, "NEW", alertWindow,
            "finance.rdvmanager.success", Constants.GENERIC_TEMPLATE);
      }
    }

    catch (Exception e) {
      log4j.error(" Exception while rdvmanageralert for rdv: " + e);
      throw new OBException(e.getMessage());
    } finally {
    }
    return 0;
  }

  /**
   * To insert modification for old encum
   * 
   * @param invLine
   * @param rdv
   * @return
   */
  public static EfinBudManencumRev insertModifiaction(InvoiceLine invLine, EfinRDV rdv) {
    EfinBudManencumRev manEncumRev = null;
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetManencumlines> oldencumlineexist = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          "as e where e.manualEncumbrance.id='"
              + rdv.getSalesOrder().getEfinBudgetManencum().getId()
              + "' and e.accountingCombination.id ='" + invLine.getEfinCValidcombination().getId()
              + "'");
      if (oldencumlineexist.list() != null && oldencumlineexist.list().size() > 0) {
        EfinBudgetManencumlines encumLines = oldencumlineexist.list().get(0);

        encumLines.setAPPAmt(encumLines.getAPPAmt().subtract(invLine.getLineNetAmount()));
        OBDal.getInstance().save(encumLines);
        OBDal.getInstance().flush();

        manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
        manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
        manEncumRev.setOrganization(
            OBDal.getInstance().get(Organization.class, encumLines.getOrganization().getId()));
        manEncumRev.setActive(true);
        manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
        manEncumRev.setCreationDate(new java.util.Date());
        manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
        manEncumRev.setUpdated(new java.util.Date());
        manEncumRev.setUniqueCode(encumLines.getUniquecode());
        manEncumRev.setManualEncumbranceLines(encumLines);
        manEncumRev.setRevdate(new java.util.Date());
        manEncumRev.setStatus("APP");
        manEncumRev.setAuto(true);
        manEncumRev.setRevamount(invLine.getLineNetAmount().negate());
        log4j.debug("rev:" + manEncumRev.getRevamount());
        manEncumRev.setAccountingCombination(encumLines.getAccountingCombination());
        // manEncumRev.setSRCManencumline(encumLines);
        manEncumRev.setEncumbranceType("AEE");
        manEncumRev.setSystem(true);
        OBDal.getInstance().save(manEncumRev);
        OBDal.getInstance().flush();
        return manEncumRev;
      }

    } catch (Exception e) {
      log4j.error(" Exception while insert modification for rdv old encum: " + e);
      throw new OBException(e.getMessage());
    } finally {

    }
    return manEncumRev;
  }

  /**
   * To update modification for old encum
   * 
   * @param invLine
   * @param rdv
   * @param newencumLines
   * @return
   */
  public static EfinBudManencumRev updateModification(InvoiceLine invLine, EfinRDV rdv,
      EfinBudgetManencumlines newencumLines) {
    EfinBudManencumRev encummodification = null;
    try {
      OBContext.setAdminMode();
      // update modification
      OBQuery<EfinBudManencumRev> oldencumrevlineexist = OBDal.getInstance().createQuery(
          EfinBudManencumRev.class,
          "as e where e.sRCManencumline.id='" + newencumLines.getId() + "'");
      if (oldencumrevlineexist.list() != null && oldencumrevlineexist.list().size() > 0) {
        encummodification = oldencumrevlineexist.list().get(0);
        EfinBudgetManencumlines oldencumLines = encummodification.getManualEncumbranceLines();

        oldencumLines.setAPPAmt(oldencumLines.getAPPAmt().subtract(invLine.getLineNetAmount()));
        OBDal.getInstance().save(newencumLines);

        encummodification.setRevamount(
            encummodification.getRevamount().add(invLine.getLineNetAmount().negate()));
        OBDal.getInstance().save(encummodification);
        return encummodification;
      }

    } catch (Exception e) {
      log4j.error(" Exception while Update modification for rdv old encum: " + e);
      throw new OBException(e.getMessage());
    } finally {
    }
    return encummodification;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static Boolean checkEncumbranceAppliedAmount(EfinRDVTransaction version) {
    Boolean hasAppliedAmount = Boolean.TRUE;
    try {
      String query = null;
      Query sqlQuery1 = null;
      List<Object> queryList1 = null;
      String acctcode = "";
      BigDecimal amt = BigDecimal.ZERO;
      Order order = version.getEfinRdv().getSalesOrder();
      HashMap<String, BigDecimal> uniquecodeMap = new HashMap<>();

      EFIN_TaxMethod newTaxMethod = calculateNewTax(version, amt);
      BigDecimal newTaxPercentage = BigDecimal.ZERO;
      if (newTaxMethod != null) {
        newTaxPercentage = new BigDecimal(newTaxMethod.getTaxpercent());
      }

      if (order != null && order.getEfinBudgetManencum() != null) {
        EfinBudgetManencum encumbrance = order.getEfinBudgetManencum();

        // if lines alone
        query = "select b.uniquecode, b.per,b.istax,b.bpartner from ((select h.uniquecode as uniquecode,sum(sum-per) as per, cast ('N' as text) as istax,'' as bpartner   from "
            + "   (select c_validcombination_id as uniquecode, sum(match_amt),0 as per  "
            + "    from efin_rdvtxnline ln  where ln.efin_rdvtxn_id  =  '" + version.getId() + "'"
            + "    and isadvance='N' and match_amt <> 0 group by c_validcombination_id "
            + "     union all "
            + "     select c_validcombination_id as uniquecode, 0 as sum,sum(holdamt) as per  "
            + "        from efin_rdvtxnline ln  where ln.efin_rdvtxn_id  = '" + version.getId()
            + "'" + "      and isadvance='N' and holdamt <> 0 group by c_validcombination_id"
            + " union all "
            + "    select c_validcombination_id as uniquecode, 0 as sum, sum(line_taxamt)   as per     "
            + "   from efin_rdvtxnline ln  where ln.efin_rdvtxn_id  = '" + version.getId()
            + "'  and isadvance='N' and line_taxamt <> 0 group by c_validcombination_id "
            + " union all "
            + " select txnln.c_validcombination_id as uniquecode ,0 as sum,sum(penalty_amount) as per from efin_penalty_action act "
            + " join efin_rdvtxnline txnln on txnln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
            + " join efin_penalty_types ptype on ptype.efin_penalty_types_id = act.efin_penalty_types_id "
            + " join EUT_Deflookups_TypeLn defln on defln.value = ptype.deductiontype and defln.Penalty_Logic = 'ECA' "
            + " where txnln.efin_rdvtxn_id = '" + version.getId()
            + "' group by txnln.c_validcombination_id " + " ) h " + "     group by h.uniquecode ) "
            + " union all "
            + " select c_validcombination_id as uniquecode, sum(adv_deduct)*-1 as per , cast ('N' as text) as istax,'' as bpartner   "
            + " from efin_rdvtxnline ln " + " where ln.efin_rdvtxn_id  = '" + version.getId()
            + "' and isadvance='N' and adv_deduct <> 0 group by c_validcombination_id "
            + " union all"
            + " select a.uniquecode,  sum(a.per) as per, cast ('N' as text) as istax,'' as bpartner  from ( select penalty_uniquecode as uniquecode, sum(penalty_amount)*-1 as per "
            + " from efin_rdvtxnline ln join efin_penalty_action pn on pn.efin_rdvtxnline_id = ln.efin_rdvtxnline_id "
            + " join efin_penalty_types ptype on ptype.efin_penalty_types_id = pn.efin_penalty_types_id "
            + " join EUT_Deflookups_TypeLn defln on defln.value = ptype.deductiontype and defln.Penalty_Logic <> 'ECA' "
            + " where ln.efin_rdvtxn_id  = '" + version.getId()
            + "' and isadvance='N' and penalty_amt <> 0 group by penalty_uniquecode) a where a.per <> 0 group by a.uniquecode"
            + "  union all "
            + " select tax.uniquecode,  sum(tax.per) as per, cast ('Y' as text) as istax,'' as bpartner  from ( select c_validcombination_id as uniquecode,";
        if (newTaxMethod != null)
          query += "sum(newtax_taxamt) ";
        else
          query += "sum(line_taxamt) ";
        query += "   as per   from efin_rdvtxnline ln   where ln.efin_rdvtxn_id  =  '"
            + version.getId()
            + "' and isadvance='N' and line_taxamt <> 0 group by c_validcombination_id) tax where tax.per <> 0 group by tax.uniquecode "
            + " union all "
            + " select epa.uniquecode,sum(epa.per) as per, cast ('N' as text) as istax,epa.c_bpartner_id as bpartner from ( "
            + " select act.penalty_uniquecode as uniquecode,case when   ord.em_escm_istax='Y' and tax.taxpercent is not null and tax.taxpercent>0  "
            + "    then round(((sum(penalty_amount))/(1+tax.taxpercent/100)),2) else "
            + "   sum(penalty_amount) end as per ,act.c_bpartner_id from efin_penalty_action act "
            + " join efin_rdvtxnline txnln on txnln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
            + "    left join c_orderline ordln on ordln.c_orderline_id= txnln.c_orderline_id "
            + "    left join c_order ord on ord.c_order_id= ordln.c_order_id "
            + "    left join efin_tax_method tax on tax.efin_tax_method_id= ord.em_escm_tax_method_id "
            + " join efin_penalty_types ptype on ptype.efin_penalty_types_id = act.efin_penalty_types_id "
            + " join EUT_Deflookups_TypeLn defln on defln.value = ptype.deductiontype and defln.Penalty_Logic = 'ECA' "
            + " where txnln.efin_rdvtxn_id = '" + version.getId() + "' "
            + " group by act.penalty_uniquecode,act.c_bpartner_id,tax.taxpercent, ord.em_escm_istax) epa where epa.per <> 0 group by epa.uniquecode,epa.c_bpartner_id "
            + " union all  "
            + " select  epatax.uniquecode,  sum(epatax.per) as per,  cast ('Y' as text) as istax,  epatax.c_bpartner_id as bpartner "
            + "  from " + "  (select  act.penalty_uniquecode as uniquecode, "
            + " case when   ord.em_escm_istax='Y' and tax.taxpercent is not null and tax.taxpercent>0  ";
        // + " then round(sum(penalty_amount)-((sum(penalty_amount))/(1+tax.taxpercent/100)),2)";
        if (newTaxMethod != null)
          query += "then round(((sum(penalty_amount))/(1+tax.taxpercent/100)) * (cast("
              + newTaxPercentage + "as numeric )/cast (100 as numeric)) ,2)";
        else
          query += " then round(sum(penalty_amount)-(((sum(penalty_amount))/(1+tax.taxpercent/100)) * cast(1 as numeric)),2) ";
        query += "   else " + " 0 end as per, act.c_bpartner_id "
            + "  from  efin_penalty_action act "
            + "    join efin_rdvtxnline txnln on txnln.efin_rdvtxnline_id = act.efin_rdvtxnline_id "
            + "    join c_orderline ordln on ordln.c_orderline_id= txnln.c_orderline_id "
            + "     join c_order ord on ord.c_order_id= ordln.c_order_id "
            + "     join efin_tax_method tax on tax.efin_tax_method_id= ord.em_escm_tax_method_id "
            + "  join efin_penalty_types ptype on ptype.efin_penalty_types_id = act.efin_penalty_types_id  "
            + "  join EUT_Deflookups_TypeLn defln on defln.value = ptype.deductiontype  "
            + "   and defln.Penalty_Logic = 'ECA'  " + " where txnln.efin_rdvtxn_id = '"
            + version.getId() + "'"
            + "  group by act.penalty_uniquecode,  act.c_bpartner_id ,tax.taxpercent, ord.em_escm_istax) epatax "

            + "  where epatax.per <> 0  " + "   group by  epatax.uniquecode,epatax.c_bpartner_id "

            + ") b where b.per <>0 order by b.per asc ";
        log4j.debug("RDV generate invoice  line alone:" + query);
        sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query);
        queryList1 = sqlQuery1.list();

        if (sqlQuery1 != null && queryList1.size() > 0) {
          for (Iterator iterator = queryList1.iterator(); iterator.hasNext();) {
            Object[] row = (Object[]) iterator.next();
            acctcode = row[0].toString();
            amt = new BigDecimal(row[1].toString());
            if (uniquecodeMap.containsKey(acctcode)) {
              uniquecodeMap.put(acctcode, uniquecodeMap.get(acctcode).add(amt));
            } else {
              uniquecodeMap.put(acctcode, amt);
            }
          }
        }

        if (!uniquecodeMap.isEmpty()) {
          Iterator uniquecodeItr = uniquecodeMap.entrySet().iterator();
          while (uniquecodeItr.hasNext()) {
            Map.Entry mapElement = (Map.Entry) uniquecodeItr.next();
            acctcode = mapElement.getKey().toString();
            amt = (BigDecimal) mapElement.getValue();
            AccountingCombination acctCombination = OBDal.getInstance()
                .get(AccountingCombination.class, acctcode);
            if (acctCombination.getEfinDimensiontype().equals("E")) {
              OBQuery<EfinBudgetManencumlines> encumLineQry = OBDal.getInstance().createQuery(
                  EfinBudgetManencumlines.class,
                  "manualEncumbrance.id=:headerId and accountingCombination.id=:acctId");
              encumLineQry.setNamedParameter("headerId", encumbrance.getId());
              encumLineQry.setNamedParameter("acctId", acctcode);
              List<EfinBudgetManencumlines> encumLineList = encumLineQry.list();
              EfinBudgetManencumlines encumLine = encumLineList.get(0);
              // check with remaining amt also in case of manual
              if ((encumLine.getAPPAmt()).compareTo(amt) < 0) {// (encumLine.getAPPAmt().add(encumLine.getRemainingAmount()))
                hasAppliedAmount = Boolean.FALSE;
                return hasAppliedAmount;
              } else {
                // if applied amt not enough to generate invoice then while validation itself
                // check app+remaining amt is enough , if enough then how much we need from
                // remaining add that amt in app amt
                if (amt.compareTo(encumLine.getAPPAmt()) > 0) {
                  BigDecimal deductedAmt = amt.subtract(encumLine.getAPPAmt());
                  if (encumLine.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                      && deductedAmt.compareTo(BigDecimal.ZERO) > 0) {
                    encumLine.setAPPAmt(encumLine.getAPPAmt().add(deductedAmt));
                    OBDal.getInstance().save(encumLine);
                    OBDal.getInstance().flush();
                  }
                }
              }

            }
          }

        }

      }
    } catch (

    Exception e) {
      log4j.error("Exception while checkEncumbranceAppliedAmount: " + e);
    }

    return hasAppliedAmount;
  }

  public static Boolean checkExternalPenaltyExists(EfinRDVTransaction version) {
    Boolean isExternalPenalty = false;
    BigInteger count = BigInteger.ZERO;
    try {
      String sql = " select count(efin_penalty_action_id) from efin_penalty_action act where act.efin_rdvtxnline_id in ( select line.efin_rdvtxnline_id from efin_rdvtxnline line where line.efin_rdvtxn_id "
          + "  =? ) and act.efin_penalty_types_id in ( select penalty.efin_penalty_types_id from efin_penalty_types penalty where penalty.deductiontype "
          + " in ( select value from eut_deflookups_typeln where penalty_logic='ECA' and  penalty_logic is not null) ) ";
      Query qry = OBDal.getInstance().getSession().createSQLQuery(sql);
      qry.setParameter(0, version.getId());
      if (qry != null && qry.list().size() > 0) {
        count = (BigInteger) qry.list().get(0);
      }
      if (count.compareTo(BigInteger.ZERO) > 0) {
        isExternalPenalty = true;
      } else {
        isExternalPenalty = false;
      }

    } catch (Exception e) {
      log4j.error("Exception while checkExternalPenaltyExists: " + e);
    }

    return isExternalPenalty;
  }

  /**
   * Check the status of last RDV version
   * 
   * @param txnVersion
   * @param efinRdv
   * @return true if no transaction is opened else through false
   */
  public static Boolean checkStatusOfPreviousVersion(EfinRDVTransaction trxnversion,
      EfinRDV efinRdv) {
    Boolean isAllowed_to_create = true;
    try {

      OBQuery<EfinRDVTransaction> transactionQry = OBDal.getInstance()
          .createQuery(EfinRDVTransaction.class, " as e where e.id not in ('" + trxnversion.getId()
              + "') and e.efinRdv.id = :rdvId and e.txnverStatus not in ('INV','PD') and e.tXNVersion < :trxnversion");
      transactionQry.setNamedParameter("rdvId", efinRdv.getId());
      transactionQry.setNamedParameter("trxnversion", trxnversion.getTXNVersion());
      if (transactionQry.list() != null && transactionQry.list().size() > 0) {
        isAllowed_to_create = false;
      }
    } catch (Exception e) {
      isAllowed_to_create = false;
      log4j.error("Exception while checkExternalPenaltyExists: " + e);

    }
    return isAllowed_to_create;
  }

  public static EFIN_TaxMethod calculateNewTax(EfinRDVTransaction trxnversion, BigDecimal amt) {
    EFIN_TaxMethod orderTax = null;
    List<EFIN_TaxMethod> taxMethodList = new ArrayList<EFIN_TaxMethod>();
    EFIN_TaxMethod newTaxMethod = null;
    try {
      Date taxEffectiveFrom = new SimpleDateFormat("yyyy-MM-dd").parse("2020-07-01");
      Date orderStartDate = new SimpleDateFormat("yyyy-MM-dd").parse("2020-05-11");
      Date orderEndDate = new SimpleDateFormat("yyyy-MM-dd").parse("2020-06-30");

      // fetching version date is after jun30
      // if (trxnversion.getTxnverDate().compareTo(taxEffectiveFrom) >= 1) {
      if (trxnversion.getNewtaxTaxamt().compareTo(BigDecimal.ZERO) > 0) {
        Order latestOrder = PurchaseInvoiceSubmitUtils
            .getLatestOrderComplete(trxnversion.getEfinRdv().getSalesOrder());
        orderTax = latestOrder.getEscmTaxMethod();
        if (orderTax != null) {
          // if (latestOrder.getOrderDate().compareTo(orderStartDate) >= 0
          // && latestOrder.getOrderDate().compareTo(orderEndDate) <= 0) {
          if (orderTax.getValidToDate() != null) {
            // && orderTax.getValidToDate().compareTo(trxnversion.getTxnverDate()) < 1) {
            OBQuery<EFIN_TaxMethod> taxMethodQry = OBDal.getInstance()
                .createQuery(EFIN_TaxMethod.class, " as e"
                    + " where e.validToDate is null and e.priceIncludesTax=:priceIncluded order by e.creationDate desc");
            taxMethodQry.setNamedParameter("priceIncluded", orderTax.isPriceIncludesTax());
            taxMethodQry.setMaxResult(1);
            taxMethodList = taxMethodQry.list();
            if (taxMethodList.size() > 0) {
              newTaxMethod = taxMethodList.get(0);
            }
            // }
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while calculateNewTax: " + e);
    }
    return newTaxMethod;
  }
}
