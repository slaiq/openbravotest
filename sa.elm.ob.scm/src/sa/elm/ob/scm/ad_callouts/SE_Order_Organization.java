/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2013-2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package sa.elm.ob.scm.ad_callouts;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.RequestFilter;
import org.openbravo.base.filter.ValueListFilter;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.CashVATUtil;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.calendar.Period;

import sa.elm.ob.scm.EscmPurchaseOrderConfiguration;

public class SE_Order_Organization extends SimpleCallout {
  private static final long serialVersionUID = 1L;
  private static final RequestFilter filterYesNo = new ValueListFilter("Y", "N");

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    final String strinpissotrx = info.getStringParameter("inpissotrx", filterYesNo);
    String strMWarehouseId = info.vars.getStringParameter("inpmWarehouseId");
    boolean updateWarehouse = true;
    FieldProvider[] td = null;

    final String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
    final String strBPartnerId = info.getStringParameter("inpcBpartnerId", IsIDFilter.instance);
    final String strBPartnerLocationId = info.getStringParameter("inpcBpartnerLocationId",
        IsIDFilter.instance);

    VariablesSecureApp vars = info.vars;
    String inpLastFieldChanged = vars.getStringParameter("inpLastFieldChanged");
    String inpadOrgId = vars.getStringParameter("inpadOrgId");
    String inpdateordered = vars.getStringParameter("inpdateordered");
    String inpadClientId = vars.getStringParameter("inpadClientId");
    String inpOrdertype = vars.getStringParameter("inpemEscmOrdertype");

    info.addResult("inpiscashvat",
        CashVATUtil.isCashVAT(strinpissotrx, strOrgId, strBPartnerId, strBPartnerLocationId));

    try {
      ComboTableData comboTableData = new ComboTableData(info.vars, this, "TABLE", "M_Warehouse_ID",
          "197", strinpissotrx.equals("Y") ? "C4053C0CD3DC420A9924F24FC1F860A0" : "",
          Utility.getReferenceableOrg(info.vars, info.vars.getStringParameter("inpadOrgId")),
          Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
      Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), "");
      td = comboTableData.select(false);
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    if (td != null && td.length > 0) {
      for (int i = 0; i < td.length; i++) {
        if (td[i].getField("id").equals(strMWarehouseId)) {
          updateWarehouse = false;
          break;
        }
      }
      if (updateWarehouse) {
        info.addResult("inpmWarehouseId", td[0].getField("id"));
      }
    } else {
      info.addResult("inpmWarehouseId", null);
    }
    if (inpLastFieldChanged.equals("inpadOrgId")) {
      String periodId = null, orgID = null;
      Date gregPODate = sa.elm.ob.utility.util.UtilityDAO.convertToGregorianDate(inpdateordered);
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

      if (inpadOrgId.equals("0")) {
        orgID = sa.elm.ob.utility.util.UtilityDAO.getChildOrgwithCalenderId(inpadClientId,
            inpadOrgId);
      } else {
        orgID = inpadOrgId;
      }
      if (gregPODate != null) {
        periodId = sa.elm.ob.utility.util.UtilityDAO.getPeriod(dateFormat.format(gregPODate),
            orgID);
        OBQuery<Period> periodQuery = OBDal.getInstance().createQuery(Period.class,
            " as e where e.id=:periodID");
        periodQuery.setNamedParameter("periodID", periodId);
        if (periodQuery != null) {
          List<Period> period = periodQuery.list();
          if (period.size() > 0) {
            String yearId = period.get(0).getYear().getId();
            info.addResult("inpemEscmFinanyear", yearId);
          } else {
            info.addResult("inpemEscmFinanyear", null);
          }
        }
      }

      EscmPurchaseOrderConfiguration configuration = PurchaseAgreementCalloutDAO.checkDocTypeConfig(
          OBContext.getOBContext().getCurrentClient().getId(), inpadOrgId, inpOrdertype);

      if (configuration != null) {
        info.addResult("inpemEscmMotcontperson",
            configuration.getMOTContactPerson() != null
                ? configuration.getMOTContactPerson().getName()
                : null);
        info.addResult("inpemEscmMotcontposition", configuration.getMOTContactPosition());
      }
    }
  }
}
