package sa.elm.ob.finance.ad_callouts;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SQLQuery;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchemaElement;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;

/**
 * 
 * 
 * @author sathish kumar.p
 *
 */

public class ValidCombinationCallout extends SimpleCallout {

  /**
   * This callout is used to reset all the values when we change the dimension type
   */

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String inpaccountId = info.vars.getStringParameter("inpaccountId");
    String inpemEfinDimensiontype = info.vars.getStringParameter("inpemEfinDimensiontype");
    String inpLastFieldChanged = info.vars.getStringParameter("inpLastFieldChanged");
    String inpcAcctschemaId = info.vars.getStringParameter("inpcAcctschemaId");
    String inpcProjectId = info.vars.getStringParameter("inpcProjectId");
    String ClientId = info.vars.getStringParameter("inpadClientId");
    String functionalkey = null, functionalName = null, entityKey = null, entityName = null,
        entityId = null, campaignId = null, campaignKey = null, campaignName = null,
        functionalId = null;

    if (inpLastFieldChanged.equals("inpemEfinDimensiontype")) {
      info.addSelect("inpaccountId");
      info.addSelectResult("", "", true);
      info.endSelect();
      info.addSelect("inpcBpartnerId");
      info.addSelectResult("", "", true);
      info.endSelect();
      info.addSelect("inpcCampaignId");
      info.addSelectResult("", "", true);
      info.endSelect();

      if (inpemEfinDimensiontype.equals("E")) {
        OBQuery<AcctSchemaElement> activity = OBDal.getInstance()
            .createQuery(AcctSchemaElement.class, "client.id='" + ClientId
                + "' and type = 'AY' and accountingSchema.id = '" + inpcAcctschemaId + "' ");

        if (activity.list() != null && activity.list().size() > 0) {
          functionalId = activity.list().get(0).getActivity().getId();
          functionalkey = activity.list().get(0).getActivity().getSearchKey();
          functionalName = activity.list().get(0).getActivity().getName();
        }
        info.addSelect("inpcActivityId");
        info.addSelectResult(functionalId, functionalkey + " - " + functionalName, false);
        info.endSelect();

        OBQuery<AcctSchemaElement> bpartner = OBDal.getInstance()
            .createQuery(AcctSchemaElement.class, "client.id='" + ClientId
                + "' and type = 'BP' and accountingSchema.id = '" + inpcAcctschemaId + "' ");
        if (bpartner.list() != null && bpartner.list().size() > 0) {
          entityId = bpartner.list().get(0).getBusinessPartner().getId();
          entityKey = bpartner.list().get(0).getBusinessPartner().getEfinDocumentno();
          entityName = bpartner.list().get(0).getBusinessPartner().getName();
        }
        info.addSelect("inpcBpartnerId");
        info.addSelectResult(entityId, entityKey + " - " + entityName, false);
        info.endSelect();
      }

      if (inpemEfinDimensiontype.equals("A")) {
        OBQuery<ABCActivity> activity = OBDal.getInstance().createQuery(ABCActivity.class,
            "client.id='" + ClientId + "' and active = true and efinIsdefault = true ");
        if (activity.list() != null && activity.list().size() > 0) {
          for (int i = 0; i < activity.list().size(); i++) {
            ABCActivity objects = activity.list().get(i);
            info.addSelect("inpcActivityId");
            info.addSelectResult(objects.getId(),
                objects.getSearchKey() + " - " + objects.getName(), false);
            info.endSelect();
          }
        }
        OBQuery<Campaign> campaign = OBDal.getInstance().createQuery(Campaign.class,
            "client.id='" + ClientId + "' and active = true and efinBudgettype = 'F' ");
        if (campaign.list() != null && campaign.list().size() > 0) {
          campaignId = campaign.list().get(0).getId();
          campaignKey = campaign.list().get(0).getSearchKey();
          campaignName = campaign.list().get(0).getName();
          info.addSelect("inpcCampaignId");
          info.addSelectResult(campaignId, campaignKey + " - " + campaignName, false);
          info.endSelect();
        }
      }

    }

    if (inpLastFieldChanged.equals("inpaccountId")) {
      if (!StringUtils.isEmpty(inpaccountId)) {
        ElementValue account = OBDal.getInstance().get(ElementValue.class, inpaccountId);
        if (account != null) {
          info.addResult("inpemEfinAccounttype", account.getAccountType());
        }
        if (inpemEfinDimensiontype.equals("E")) {
          String strquery = " select c_campaign.c_campaign_id,c_campaign.value,c_campaign.name from c_campaign left join EFIN_BudgetType_Acct on \n"
              + " c_campaign.c_campaign_id = EFIN_BudgetType_Acct.c_campaign_id \n"
              + "  join ad_treenode on ad_treenode.parent_id = EFIN_BudgetType_Acct.c_elementvalue_id\n"
              + " and ad_treenode.node_id = '" + inpaccountId + "' \n"
              + " and c_campaign.ad_client_id = \n" + " '" + ClientId + "' ";

          SQLQuery campaignlist = OBDal.getInstance().getSession().createSQLQuery(strquery);
          @SuppressWarnings("unchecked")
          List<Object[]> campaignlist1 = (ArrayList<Object[]>) campaignlist.list();

          if (campaignlist1 != null && campaignlist1.size() > 0) {
            for (int i = 0; i < campaignlist1.size(); i++) {
              Object[] objects = campaignlist1.get(i);
              if (objects != null) {
                info.addSelect("inpcCampaignId");
                info.addSelectResult(objects[0].toString(),
                    objects[1].toString() + " - " + objects[2].toString(), false);
                info.endSelect();
              }

            }

          }
        }

      }
      if (!StringUtils.isEmpty(inpcProjectId)) {
        info.addResult("JSEXECUTE", "form.getFieldFromColumnName('C_Project_ID').setValue('')");
      }
    }
  }
}
