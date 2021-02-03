package sa.elm.ob.finance.ad_reports.encumbrance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

public class EncumbranceReportDAO {

  /**
   * This method is used to get all the encumbrance and its corresponding values
   * 
   * @return List<EncumbranceDTO>
   */
  public static List<EncumbranceDTO> getExcelData() {

    String fundsEncumQry = "";
    EncumbranceDTO encumDTO = null;
    FundsEncumbranceQuery fundsEncumbranceQuery = null;

    List<EncumbranceDTO> encumbranceDTOList = new ArrayList<>();

    try {

      OBContext.setAdminMode();

      String sqlquery = "select  encum.efin_budget_manencum_id as efinbudgetmanencumid, "
          + "case when budgettype.em_efin_budgettype='C' then 'Cost' else 'Fund' end as budgetType, "
          + "encum.documentno as encumbranceNo, "
          + "case when encum.isauto='Y' then 'Auto' else 'Manual' end as encumbranceMethod, "
          + "reflist.name as encumbranceType, "
          + "case when count(inv.c_invoice_id) >0 then true else false end as isInvoiced, "
          + "CostEncum.documentno as costEncumbranceNo , "
          + "case when count(inv.c_invoice_id) >0 and encum.isauto='Y' then encum.amount else  coalesce( sum(lines.used_amount),0)end as amountInvoiced "
          + "from efin_budget_manencum encum "
          + "join efin_budget_manencumlines lines on lines.efin_budget_manencum_id = encum.efin_budget_manencum_id "
          + "join c_campaign budgettype on budgettype.c_campaign_Id = encum.c_campaign_id "
          + "join ad_ref_list reflist on reflist.value = encum.encum_type  and reflist.ad_reference_id ='8B295E69212844C6AF89EFB0554B6143' "
          + "left join c_invoice inv on (inv.em_efin_manualencumbrance_id=  encum.efin_budget_manencum_id or inv.em_efin_funds_encumbrance_id=encum.efin_budget_manencum_id)and inv.em_efin_isreserved='Y'  "
          + "left join efin_budget_manencum CostEncum on encum.cost_encumbrance_id = CostEncum.efin_budget_manencum_id "
          + " where encum.ad_client_id =?  "
          + "group by budgettype.em_efin_budgettype,encum.documentno,encum.isauto,reflist.name,CostEncum.documentno,encum.amount,encum.efin_budget_manencum_id";

      SQLQuery encumbranceQry = OBDal.getInstance().getSession().createSQLQuery(sqlquery);
      encumbranceQry.setParameter(0, OBContext.getOBContext().getCurrentClient().getId());
      encumbranceQry.setResultTransformer(Transformers.aliasToBean(EncumbranceQueryDTO.class));

      @SuppressWarnings("unchecked")
      List<EncumbranceQueryDTO> encumbranceList = encumbranceQry.list();

      for (EncumbranceQueryDTO queryDTO : encumbranceList) {
        encumDTO = new EncumbranceDTO();
        encumDTO.setBudgetType(queryDTO.getBudgettype());
        encumDTO.setEncumbranceNo(queryDTO.getEncumbranceno());
        encumDTO.setEncumbranceType(queryDTO.getEncumbrancetype());
        encumDTO.setEncumbranceMethod(queryDTO.getEncumbrancemethod());
        encumDTO.setIsInvoiced(queryDTO.isinvoiced);
        encumDTO.setCostEncumbranceNo(queryDTO.getCostencumbranceno());
        encumDTO.setAmountInvoiced(queryDTO.getAmountinvoiced());
        encumDTO.setIsValid(true);

        if (encumDTO.getBudgetType().equals("Cost")) {
          fundsEncumQry = "select COALESCE(sum(fundsencumbranceamt),0) AS fundsencumbranceamt, COALESCE(sum(fundsactualamt),0) AS fundsactualamt from (select "
              + "case when fundsencum.isauto ='Y' and inv.posted='N' then sum(fundsencum.amount) "
              + "     when fundsencum.isauto ='Y' and inv.posted='Y' then 0 "
              + "         when fundsencum.isauto ='N' and inv.posted='N' then sum(lines.used_amount) "
              + "         when fundsencum.isauto ='N' and inv.posted='Y' then 0 "
              + "         when fundsencum.isauto ='Y'  then sum(fundsencum.amount)  "
              + "         when fundsencum.isauto ='N'  then sum(lines.used_amount)  "
              + "         else 0 end as fundsencumbranceamt , "
              + "case when fundsencum.isauto ='Y' and inv.posted='N' then 0  "
              + "     when fundsencum.isauto ='Y' and inv.posted='Y' then sum(fundsencum.amount) "
              + "         when fundsencum.isauto ='N' and inv.posted='N' then 0 "
              + "         when fundsencum.isauto ='N' and inv.posted='Y' then sum(lines.used_amount) "
              + "         when fundsencum.isauto ='N'  then 0 "
              + "         when fundsencum.isauto ='Y'  then 0 "
              + "         else sum(lines.used_amount) end as fundsactualamt "
              + "from efin_budget_manencum fundsencum "
              + "join efin_budget_manencumlines lines on lines.efin_budget_manencum_id = fundsencum.efin_budget_manencum_id "
              + "left join c_invoice inv on inv.em_efin_funds_encumbrance_id= fundsencum.efin_budget_manencum_id  where  fundsencum.cost_encumbrance_id= ? and inv.em_efin_isreserved='Y' "
              + "group by inv.c_invoice_id,fundsencum.isauto,inv.posted,fundsencum.amount,lines.used_amount)a ";

          encumbranceQry = OBDal.getInstance().getSession().createSQLQuery(fundsEncumQry);
          encumbranceQry.setParameter(0, queryDTO.getEncumbranceId());
          encumbranceQry
              .setResultTransformer(Transformers.aliasToBean(FundsEncumbranceQuery.class));

          fundsEncumbranceQuery = (FundsEncumbranceQuery) encumbranceQry.uniqueResult();

          if (fundsEncumbranceQuery != null) {
            encumDTO.setFundsActualAmount(fundsEncumbranceQuery.getFundsactualamt());
            encumDTO.setFundsEncumbranceAmount(fundsEncumbranceQuery.getFundsencumbranceamt());
          }

          if (encumDTO.getAmountInvoiced()
              .subtract(encumDTO.getFundsEncumbranceAmount().add(encumDTO.getFundsActualAmount()))
              .compareTo(BigDecimal.ZERO) == 0) {
            encumDTO.setIsValid(true);
          } else {
            encumDTO.setIsValid(false);
          }

        } else {
          encumDTO.setFundsActualAmount(BigDecimal.ZERO);
          encumDTO.setFundsEncumbranceAmount(BigDecimal.ZERO);
        }

        encumbranceDTOList.add(encumDTO);
      }

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return encumbranceDTOList;

  }

  public static List<EncumbranceSummaryDTO> getExcelSummaryData() {

    String fundsEncumQry = "";
    EncumbranceSummaryDTO encumDTO = null;

    List<EncumbranceSummaryDTO> encumbranceDTOList = new ArrayList<>();

    try {

      OBContext.setAdminMode();

      String sqlquery = "SELECT " + "       "
          + "       uniquecode.fundssuniq           AS fundsuniq, "
          + "       uniquecode.costuniq             AS costuniq, "
          + "       uniquecode.uniquecode           AS uniquecode, "
          + "       uniquecode.validcomb            AS validcombid, "
          + "       encumbrance                     AS encumbrance, "
          + "       spent_amt                       AS actual, "
          + "       Sum(notinvoiceamount)           AS notinvoiceamount, "
          + "       Sum(unpaidinvoice)              AS unpaidinvoice, "
          + "       Sum(paidinvoice)                AS paidinvoice, "
          + "       Sum(postedinvoice)              AS postedinvoice, "
          + "       uniquecode.account              AS account, "
          + "       uniquecode.budgettype           AS budgettype, "
          + "       Sum(unpaidinvoiceamountcost)    AS unpaidinvoiceamountcost, "
          + "       Sum(unpaidinvoiceamountdirect) + Sum(paidinvoiceamountsdirect) AS directfundencumbrance, "
          + "       Sum(paidinvoiceamountcost)      AS paidinvoiceamountcost,"
          + "       Sum(postinvoiceamountcost)      as postinvoiceamountcost, CASE "
          + "         WHEN uniquecode.budgettype = 'Fund' AND Sum(notinvoiceamount) + Sum(unpaidinvoice) + Sum(paidinvoice) = encumbrance THEN true "
          + "         WHEN uniquecode.budgettype = 'Cost' AND Sum(notinvoiceamount) + Sum(unpaidinvoice) + Sum(paidinvoice) + Sum(postedinvoice) = encumbrance THEN true "
          + "         ELSE false END AS encumbrancecheck, coalesce(journalamount,0) as journalamount, "
          + "     original_budget, current_budget,coalesce(sum(legacy_spent),0) as legacy_spent FROM    "
          + "      (select  comb.em_efin_fundscombination as fundssuniq,legacy.sum_acc_paid as legacy_spent,  "
          + "           comb.em_efin_costcombination as costuniq,   "
          + "           comb.em_efin_uniquecode as uniquecode, comb.c_validcombination_id as validcomb ,inq.spent_amt, "
          + " inq.encumbrance as encumbrance, "
          + " budinq.current_budget as current_budget, budinq.org_amt as original_budget,"
          + " case when count(inv.c_invoice_id) > 0 and header.docstatus ='CO' "
          + " then COALESCE(lines.remaining_amount,0)+COALESCE(lines.app_amt,0) when header.docstatus ='CO' "
          + " then COALESCE(lines.app_amt,0) +COALESCE(lines.remaining_amount,0) "
          + " else lines.system_updated_amt end as notinvoiceamount, "
          // + " case when count(inv.c_invoice_id) > 0 " + " then 0 when header.docstatus ='CO' then
          // "
          // + " COALESCE(lines.app_amt,0)+COALESCE(lines.remaining_amount,0) "
          // + " else lines.system_updated_amt end as notinvoiceamount,"
          + "           case when count(inv.c_invoice_id) >0 and inv.ispaid='N' then lines.used_amount   "
          + "                        when count(inv.c_invoice_id) >0 and inv.ispaid='Y' then 0   "
          + "                        else 0 end as unpaidinvoice,  "
          + "           case when count(inv.c_invoice_id) >0 and inv.ispaid='Y' and inv.posted='N' then coalesce((lines.used_amount),0)  "
          + "                        else 0 end as paidinvoice,  "
          + "           case when count(inv.c_invoice_id) >0 and inv.posted='Y' then coalesce((lines.used_amount),0)  "
          + "                        else 0 end as postedinvoice,   "
          + "           case when budgettype.em_efin_budgettype='C' then 'Cost' else 'Fund' end as budgetType,   "
          + "                        account.value as account,"
          + "           case when budgettype.em_efin_budgettype='F' and count(inv.c_invoice_id) >0 and header.cost_encumbrance_id is not null and inv.ispaid ='Y' and inv.posted ='N' then lines.used_amount"
          + "                        else 0 end as paidinvoiceamountcost,"
          + "           case when budgettype.em_efin_budgettype='F' and count(inv.c_invoice_id) >0 and header.cost_encumbrance_id is not null and inv.ispaid ='Y' and inv.posted ='Y' then lines.used_amount"
          + "                        else 0 end as postinvoiceamountcost,"
          + "           case when budgettype.em_efin_budgettype='F' and count(inv.c_invoice_id) >0 and header.cost_encumbrance_id is null and inv.ispaid ='Y'  then lines.used_amount"
          + "                        else 0 end as paidinvoiceamountsdirect, "
          + "           case when budgettype.em_efin_budgettype='F' and count(inv.c_invoice_id) >0 and header.cost_encumbrance_id is not null and inv.ispaid ='N'then lines.used_amount"
          + "                        else 0 end as unpaidinvoiceamountcost,"
          + "           case when budgettype.em_efin_budgettype='F' and count(inv.c_invoice_id) >0 and inv.ispaid ='N'and header.cost_encumbrance_id is null then lines.used_amount"
          + "                        else 0 end as unpaidinvoiceamountdirect, "
          + " (select sum(actualtran.amount )  FROM efin_actualtransaction actualtran "
          + "    LEFT JOIN gl_journal gl ON gl.gl_journal_id = actualtran.gl_journal_id "
          + "   where actualtran.c_validcombination_id=comb.c_validcombination_id) as journalamount"
          + "           from efin_budgetinquiry inq   "
          + "join c_validcombination comb on comb.c_validcombination_id =inq.c_validcombination_Id and comb.em_efin_dimensiontype='E'   "
          + "join c_elementvalue account on account.c_elementvalue_id = comb.account_id   "
          + "join efin_budget_manencumlines lines on lines.c_validcombination_id = comb.c_validcombination_id   "
          + "join efin_budget_manencum header on header.efin_budget_manencum_id = lines.efin_budget_manencum_id and header.efin_budgetint_id=inq.efin_budgetint_id  "
          + " left join efin_reservation_dataload legacy on  legacy.natural_account=account.value "
          + " and header.efin_reservation_dataload_id=legacy.efin_reservation_dataload_id "
          + "join c_campaign budgettype on budgettype.c_campaign_Id = header.c_campaign_id   "
          + "left join c_invoice inv on (inv.em_efin_manualencumbrance_id=  header.efin_budget_manencum_id or inv.em_efin_funds_encumbrance_id = header.efin_budget_manencum_id )   "
          + "and inv.em_efin_isreserved='Y' and inv.docstatus!='EFIN_CA'   "
          + " left join efin_budgetinquiry budinq on  "
          + " budinq.c_salesregion_id in (select hq_budgetcontrolunit from efin_budget_ctrl_param) "
          + " and  budinq.ad_client_id=inq.ad_client_id "
          + "  and budinq.c_elementvalue_id=account.c_elementvalue_id "
          + " and budinq.c_campaign_id=budgettype.c_campaign_id   and budinq.efin_budgetint_id='D8D5FA6B54E345B0B0331BC5764BDFAC'  "
          + "where inq.ad_client_id =? and (header.docstatus='CO' or header.isreservedfund ='Y')  and inq.efin_budgetint_id='D8D5FA6B54E345B0B0331BC5764BDFAC'  "
          + "and comb.c_salesregion_id not in (select hq_budgetcontrolunit from efin_budget_ctrl_param) and    "
          + "comb.c_salesregion_id not in (select budgetcontrol_costcenter from efin_budget_ctrl_param)  "
          + "group by comb.c_validcombination_id,inq.encumbrance, header.isauto,header.amount , budinq.current_budget,budinq.org_amt,  "
          + "lines.used_amount,inq.spent_amt,budgettype.em_efin_budgettype,account.value,inv.posted,lines.revamount,legacy.sum_acc_paid,comb.em_efin_costcombination ,comb.em_efin_costcombination,inv.ispaid, lines.efin_budget_manencumlines_id,header.encum_type, header.docstatus,header.cost_encumbrance_id ) uniquecode  "
          + "group by uniquecode.validcomb,uniquecode.uniquecode,uniquecode.account,original_budget,current_budget, uniquecode.budgetType,uniquecode.fundssuniq, uniquecode.costuniq, uniquecode.encumbrance, uniquecode.spent_amt ,uniquecode.journalamount  "
          + "order by uniquecode.account,uniquecode.uniquecode;";

      SQLQuery encumbranceQry = OBDal.getInstance().getSession().createSQLQuery(sqlquery);
      encumbranceQry.setParameter(0, OBContext.getOBContext().getCurrentClient().getId());
      encumbranceQry.setResultTransformer(Transformers.aliasToBean(EncumQuerySummaryDTO.class));
      @SuppressWarnings("unchecked")
      List<EncumQuerySummaryDTO> encumbranceList = encumbranceQry.list();

      for (EncumQuerySummaryDTO queryDTO : encumbranceList) {
        encumDTO = new EncumbranceSummaryDTO();
        encumDTO.setBudgettype(queryDTO.getBudgettype());
        encumDTO.setAccount(queryDTO.getAccount());
        encumDTO.setUniquecode(queryDTO.getUniquecode());
        encumDTO.setEncumbranceamount(queryDTO.getEncumbrance());
        encumDTO.setUnpaidinvoice(queryDTO.getUnpaidinvoice());
        encumDTO.setPaidinvoice(queryDTO.getPaidinvoice());
        encumDTO.setPostedinvoice(queryDTO.getPostedinvoice());
        encumDTO.setActualamount(queryDTO.getActual());
        encumDTO.setNotinvoicedamount(queryDTO.getNotinvoiceamount());
        encumDTO.setJournal_amount(queryDTO.getJournalamount());
        encumDTO.setEncumbrancecheck(queryDTO.getEncumbrancecheck());
        encumDTO.setUnpaidinvoicefromcost(queryDTO.getUnpaidinvoiceamountcost());
        encumDTO.setPaidinvoicefromcost(queryDTO.getPaidinvoiceamountcost());
        encumDTO.setDirectfundsencumbranceamount(queryDTO.getDirectfundencumbrance());
        encumDTO.setCostActualAmount(queryDTO.getPostinvoiceamountcost());
        encumDTO.setFundsinvoicepaid(BigDecimal.ZERO);
        encumDTO.setFundsinvoiceunpaid(BigDecimal.ZERO);
        encumDTO.setFundsActualamount(BigDecimal.ZERO);
        encumDTO.setCurrent_budget(queryDTO.getCurrent_budget());
        encumDTO.setOriginal_budget(queryDTO.getOriginal_budget());
        encumDTO.setLegacy_spent(queryDTO.getLegacy_spent());
        encumDTO.setFundscostencumbrancecheck(true);

        if ("Cost".equals(queryDTO.getBudgettype()) && queryDTO.getFundsuniq() != null) {
          fundsEncumQry = "select COALESCE(sum(fundsactual),0) as fundsactual,"
              + " COALESCE(sum(unpaidinvoice),0) as unpaidinvoice, COALESCE(sum(paidinvoice),0) as paidinvoice "
              + "from( " + "            select  case when count(inv.c_invoice_id) >0  "
              + " and inv.ispaid ='Y' and inv.posted ='Y' then lines.used_amount "
              + "                         else 0 end as fundsactual, "
              + "                case when count(inv.c_invoice_id) >0  and inv.ispaid ='N' and inv.posted ='N' "
              + "then lines.used_amount "
              + "                         else 0 end as unpaidinvoice,    "
              + "                case when count(inv.c_invoice_id) >0  and inv.ispaid ='Y'  and inv.posted ='N' then lines.used_amount "
              + "                     else 0 end as paidinvoice " + "from efin_budgetinquiry inq  "
              + "join c_validcombination comb on comb.c_validcombination_id =inq.c_validcombination_Id  "
              + "join efin_budget_manencumlines lines on lines.c_validcombination_id = comb.c_validcombination_id  "
              + "join efin_budget_manencum header on header.efin_budget_manencum_id = lines.efin_budget_manencum_id and header.cost_encumbrance_id is not null   and header.efin_budgetint_id=inq.efin_budgetint_id "
              + "left join c_invoice inv on (inv.em_efin_funds_encumbrance_id=  header.efin_budget_manencum_id )  "
              + "and inv.em_efin_isreserved='Y'  where lines.c_validcombination_id= ? and inv.docstatus!='EFIN_CA' "
              + " and inq.efin_budgetint_id='D8D5FA6B54E345B0B0331BC5764BDFAC'  "
              + "group by comb.c_validcombination_id,inq.encumbrance, header.isauto,header.amount,lines.used_amount,inq.spent_amt,inv.posted,lines.revamount,header.cost_encumbrance_id,inv.ispaid) fundsencumbrance";
          encumbranceQry = OBDal.getInstance().getSession().createSQLQuery(fundsEncumQry);
          encumbranceQry.setParameter(0, queryDTO.getFundsuniq());
          encumbranceQry.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

          @SuppressWarnings("unchecked")
          Map<String, BigDecimal> encumbranceMap = (HashMap<String, BigDecimal>) encumbranceQry
              .uniqueResult();
          BigDecimal calculatedAmount = BigDecimal.ZERO;
          if (encumbranceMap.size() > 0) {
            encumDTO.setFundsinvoicepaid(encumbranceMap.get("paidinvoice"));
            encumDTO.setFundsinvoiceunpaid(encumbranceMap.get("unpaidinvoice"));
            encumDTO.setFundsActualamount(encumbranceMap.get("fundsactual"));
            calculatedAmount = encumDTO.getPaidinvoice().add(encumDTO.getUnpaidinvoice())
                .add(encumDTO.getPostedinvoice()).subtract(encumDTO.getFundsinvoicepaid()
                    .add(encumDTO.getFundsinvoiceunpaid()).add(encumDTO.getFundsActualamount()));
            encumDTO.setFundscostencumbrancecheck(calculatedAmount.compareTo(BigDecimal.ZERO) == 0);
          } else {
            encumDTO.setFundsinvoicepaid(BigDecimal.ZERO);
            encumDTO.setFundsinvoiceunpaid(BigDecimal.ZERO);
            encumDTO.setFundsActualamount(BigDecimal.ZERO);
          }
        }
        // // To fetch current budget and original budget for selected account
        //
        // AccountingCombination objValidCombination = OBDal.getInstance()
        // .get(AccountingCombination.class, queryDTO.getValidcombid());
        // OBQuery<EfinBudgetInquiry> budgetEnquiryQry = OBDal.getInstance().createQuery(
        // EfinBudgetInquiry.class,
        // "as e where e.account.id = :accountId and e.salesCampaign.id = :campaignId "
        // + " and e.department.id = :departmentId ");
        // budgetEnquiryQry.setNamedParameter("accountId",
        // objValidCombination.getAccount().getId());
        // budgetEnquiryQry.setNamedParameter("campaignId",
        // objValidCombination.getSalesCampaign().getId());
        // budgetEnquiryQry.setNamedParameter("departmentId", "38E2AC97B70641F2AF72CBB634935FC4");
        // if (budgetEnquiryQry != null && budgetEnquiryQry.list().size() > 0) {
        // EfinBudgetInquiry objBudgetEnquiry = budgetEnquiryQry.list().get(0);
        // encumDTO.setCurrent_budget(objBudgetEnquiry.getCurrentBudget());
        // encumDTO.setOriginal_budget(objBudgetEnquiry.getREVAmount());
        // } else {
        // encumDTO.setCurrent_budget(BigDecimal.ZERO);
        // encumDTO.setOriginal_budget(BigDecimal.ZERO);
        // }

        encumbranceDTOList.add(encumDTO);
      }

    } catch (

    Exception e) {
      e.printStackTrace();
    } finally {
      OBContext.restorePreviousMode();
    }
    return encumbranceDTOList;

  }

}
