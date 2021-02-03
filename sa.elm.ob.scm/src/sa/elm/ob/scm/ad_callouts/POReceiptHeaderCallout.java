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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package sa.elm.ob.scm.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;

import sa.elm.ob.scm.ad_callouts.dao.POReceiptHeaderCalloutDAO;
import sa.elm.ob.utility.util.Constants;

public class POReceiptHeaderCallout extends SimpleCallout {

  /**
   * Callout for PO Receipt Header
   */

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("resource")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    String strMWarehouseId = info.vars.getStringParameter("inpmWarehouseId");
    String inpLastFieldChanged = info.vars.getStringParameter("inpLastFieldChanged");
    String strOrgid = info.vars.getStringParameter("inpadOrgId");
    String strClientId = info.vars.getStringParameter("inpadClientId");
    String strFromBname = info.vars.getStringParameter("inpemEscmBname");
    String strporeference = info.vars.getStringParameter("inpporeference");
    String strFromBtype = info.vars.getStringParameter("inpemEscmBtype");
    String strToBtype = info.vars.getStringParameter("inpemEscmTobeneficiary");
    String strIssueReason = info.vars.getStringParameter("inpemEscmIssuereason");
    String strToBName = info.vars.getStringParameter("inpemEscmTobenefiName");
    String strReceivingtype = info.vars.getStringParameter("inpemEscmReceivingtype");
    String strIsCustodyTransfer = info.vars.getStringParameter("inpemEscmIscustodyTransfer");
    String strMinoutId = info.vars.getStringParameter("inpmInoutId");
    String strOrderId = info.vars.getStringParameter("inpcOrderId");
    String doctypeId = info.vars.getStringParameter("inpcDoctypeId");
    String inpemEscmCtsender = info.vars.getStringParameter("inpemEscmCtsender");
    String inpemEscmCtsenderemp = info.vars.getStringParameter("inpemEscmCtsenderemp");
    String inpemEscmCtreceiver = info.vars.getStringParameter("inpemEscmCtreceiver");
    String inpemEscmCtreceiveremp = info.vars.getStringParameter("inpemEscmCtreceiveremp");

    // boolean updateWarehouse = true;
    // FieldProvider[] td = null;
    String inpTabId = info.vars.getStringParameter("inpTabId");
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement st = null;
    ResultSet rs = null;
    String sql = "";
    String sqlquery = "";
    String query = "";
    User senderManagerId = null, receiverManagerId = null, useObj = null;
    log4j.debug("inpTabId " + inpTabId);
    log4j.debug("strMWarehouseId " + strMWarehouseId);
    log4j.debug("inpLastFieldChanged " + inpLastFieldChanged);
    log4j.debug("strIsCustodyTransfer: " + strIsCustodyTransfer);
    User currentuser = OBContext.getOBContext().getUser();
    /*
     * try {
     * 
     * if (inpLastFieldChanged.equals("inpadOrgId")) { if (inpTabId.equals("296")) { ComboTableData
     * comboTableData = new ComboTableData(info.vars, this, "18", "M_Warehouse_ID", "197",
     * strIsSOTrx.equals("Y") ? "C4053C0CD3DC420A9924F24FC1F860A0" :
     * "301AAE6E09C5402198813447D752EF59", Utility.getReferenceableOrg(info.vars,
     * info.vars.getStringParameter("inpadOrgId")), Utility.getContext(this, info.vars,
     * "#User_Client", info.getWindowId()), 0); Utility.fillSQLParameters(this, info.vars, null,
     * comboTableData, info.getWindowId(), ""); td = comboTableData.select(false); comboTableData =
     * null; } else if ((inpTabId.equals("72A6B3CA5BE848ACA976304375A5B7A6") || inpTabId
     * .equals("922927563BFC48098D17E4DC85DD504C"))) { ComboTableData comboTableData = new
     * ComboTableData(info.vars, this, "18", "M_Warehouse_ID", "197", strIsSOTrx.equals("Y") ?
     * "552CF3354797470F9535869BF731C775" : "552CF3354797470F9535869BF731C775",
     * Utility.getReferenceableOrg(info.vars, info.vars.getStringParameter("inpadOrgId")),
     * Utility.getContext(this, info.vars, "#User_Client", info.getWindowId()), 0);
     * Utility.fillSQLParameters(this, info.vars, null, comboTableData, info.getWindowId(), ""); td
     * = comboTableData.select(false); comboTableData = null; } if (td != null && td.length > 0) {
     * for (int i = 0; i < td.length; i++) { if (td[i].getField("id").equals(strMWarehouseId)) {
     * updateWarehouse = false; break; } } if (updateWarehouse) { info.addResult("inpmWarehouseId",
     * td[0].getField("id")); } } else { info.addResult("inpmWarehouseId", null); } } } catch
     * (Exception ex) { throw new ServletException(ex); }
     */

    if (strIsCustodyTransfer.equals("Y") && inpTabId.equals("CB9A2A4C6DB24FD19D542A78B07ED6C1")) {
      log4j.debug("Exists: " + strIsCustodyTransfer);

      if (inpLastFieldChanged.equals("inpadOrgId")) {
        try {

          if (inpLastFieldChanged.equals("inpadOrgId")) {
            sql = "select c_bpartner.c_bpartner_id as bpartnerid,c_bpartner_location.c_bpartner_location_id as locationid from c_bpartner "
                + "left join c_bpartner_location on c_bpartner_location.c_bpartner_id = c_bpartner.c_bpartner_id "
                + "where c_bpartner.em_escm_defaultpartner='Y' and c_bpartner.ad_client_id='"
                + strClientId + "'";
            st = conn.prepareStatement(sql);
            rs = st.executeQuery();

            if (rs.next()) {
              info.addResult("inpcBpartnerId", rs.getString("bpartnerid").toString());
              info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
              info.addResult("inpcDoctypeId", "FF8080812C2ABFC6012C2B3BDF4A004E");
            }
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();

            sql = "select m_warehouse_id from m_warehouse where isactive='Y' and ad_client_id='"
                + strClientId + "' limit 1";
            st = conn.prepareStatement(sql);
            rs = st.executeQuery();
            if (rs.next()) {
              info.addResult("inpmWarehouseId", rs.getString("m_warehouse_id"));
            }
          }
          Boolean ispreference = false;
          /*
           * Boolean ispreference = Preferences.existsPreference("ESCM_Inventory_Control", true,
           * null, null, currentuser.getId(), null, null);
           */

          String preferenceValue = "";

          try {
            preferenceValue = Preferences.getPreferenceValue("ESCM_CTIsAdmin", true,
                info.vars.getClient(), strMinoutId, info.vars.getUser(), info.vars.getRole(),
                "184");
          } catch (PropertyException e) {
          }
          if (preferenceValue != null && preferenceValue.equals("Y"))
            ispreference = true;

          // check preference
          OBQuery<Preference> procurementPrefernce = OBDal.getInstance().createQuery(
              Preference.class, "as e where e.property='ESCM_CTIsAdmin' and e.searchKey='Y' "
                  + " and e.visibleAtRole.id=:roleID and active='Y'");
          procurementPrefernce.setNamedParameter("roleID", info.vars.getRole());
          if (procurementPrefernce.list().size() == 0) {
            ispreference = false;
          }
          log4j.debug("ispreference :" + ispreference);
          if (!ispreference) {
            info.addResult("inpemEscmBtype", "E");
            if (currentuser.getBusinessPartner() != null) {
              strFromBname = currentuser.getBusinessPartner().getId();
              info.addResult("inpemEscmBname", currentuser.getBusinessPartner() == null ? null
                  : currentuser.getBusinessPartner().getId());
            }
            info.addResult("inpemEscmCtsender", currentuser.getId());
            info.addResult("inpemEscmCtsenderemp", currentuser.getBusinessPartner() == null ? null
                : currentuser.getBusinessPartner().getId());
            senderManagerId = POReceiptHeaderCalloutDAO.getUserManagerId(currentuser);
            if (senderManagerId != null)
              info.addResult("inpemEscmCtsendlinemng", senderManagerId.getId());
            if (senderManagerId != null && senderManagerId.getBusinessPartner() != null)
              info.addResult("inpemEscmCtsendlineemp",
                  senderManagerId.getBusinessPartner().getId());

          }
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          log4j.error("Exception in POReceiptHeaderCallout ", e);
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
          }

        }
      }
      if (inpLastFieldChanged.equals("inpemEscmBname")) {
        if (strFromBtype.equals("E")) {
          BusinessPartner objBpartner = OBDal.getInstance().get(BusinessPartner.class,
              strFromBname);
          // Task No.4812 info.addResult("inpemEscmFromemployee", objBpartner.getName());
          OBQuery<User> userlist = OBDal.getInstance().createQuery(User.class,
              " businessPartner.id=:bpartnerID ");
          userlist.setNamedParameter("bpartnerID", objBpartner.getId());
          if (userlist.list().size() > 0) {
            User user = userlist.list().get(0);
            info.addResult("inpemEscmCtsender", user.getId());
            info.addResult("inpemEscmCtsenderemp", user.getBusinessPartner().getId());

            senderManagerId = POReceiptHeaderCalloutDAO.getUserManagerId(user);
            if (senderManagerId != null)
              info.addResult("inpemEscmCtsendlinemng", senderManagerId.getId());
            else
              info.addResult("inpemEscmCtsendlinemng", user.getId());

            if (senderManagerId != null && senderManagerId.getBusinessPartner() != null)
              info.addResult("inpemEscmCtsendlineemp",
                  senderManagerId.getBusinessPartner().getId());
            else
              info.addResult("inpemEscmCtsendlineemp", user.getBusinessPartner().getId());

          } else {
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('EM_Escm_Ctsender').setValue('')");
            info.addResult("JSEXECUTE",
                "form.getFieldFromColumnName('EM_Escm_Ctsendlinemng').setValue('')");
          }
        } else {
          info.addResult("inpemEscmCtsender", "");
          info.addResult("inpemEscmCtsenderemp", "");
        }

      } else if (inpLastFieldChanged.equals("inpemEscmTobenefiName")) {

        if (strToBtype.equals("E")) {
          BusinessPartner objBpartner = OBDal.getInstance().get(BusinessPartner.class, strToBName);
          // Task No.4812 info.addResult("inpemEscmToemployee", objBpartner.getName());
          if (objBpartner != null) {

            OBQuery<User> userlist = OBDal.getInstance().createQuery(User.class,
                " businessPartner.id=:bpartnerId ");
            userlist.setNamedParameter("bpartnerId", objBpartner.getId());
            if (userlist.list().size() > 0) {
              User user = userlist.list().get(0);
              info.addResult("inpemEscmCtreceiver", user.getId());
              info.addResult("inpemEscmCtreceiveremp", user.getBusinessPartner().getId());

              receiverManagerId = POReceiptHeaderCalloutDAO.getUserManagerId(user);
              if (receiverManagerId != null)
                info.addResult("inpemEscmCtreclinemng", receiverManagerId.getId());
              else
                info.addResult("inpemEscmCtreclinemng", user.getId());

              if (receiverManagerId != null && receiverManagerId.getBusinessPartner() != null)
                info.addResult("inpemEscmCtreclineemp",
                    receiverManagerId.getBusinessPartner().getId());
              else
                info.addResult("inpemEscmCtreclineemp", user.getBusinessPartner().getId());
            } else {
              info.addResult("JSEXECUTE",
                  "form.getFieldFromColumnName('EM_Escm_Ctreceiver').setValue('')");
              info.addResult("JSEXECUTE",
                  "form.getFieldFromColumnName('EM_Escm_Ctreclinemng').setValue('')");
            }
          }
        } else {
          info.addResult("inpemEscmCtreceiver", "");
        }
      }
      // getting the line manager based on sender/receiver

      else if (inpLastFieldChanged.equals("inpemEscmCtsender") && strFromBtype.equals("E")) {

        useObj = OBDal.getInstance().get(User.class, inpemEscmCtsender);

        if (useObj != null)
          senderManagerId = POReceiptHeaderCalloutDAO.getUserManagerId(useObj);

        if (senderManagerId != null)
          info.addResult("inpemEscmCtsendlinemng", senderManagerId.getId());
        else
          info.addResult("inpemEscmCtsendlinemng", inpemEscmCtsender);
      }

      else if (inpLastFieldChanged.equals("inpemEscmCtsenderemp") && strFromBtype.equals("E")) {

        useObj = POReceiptHeaderCalloutDAO.getUserId(inpemEscmCtsenderemp);

        if (useObj != null)
          senderManagerId = POReceiptHeaderCalloutDAO.getUserManagerId(useObj);

        if (senderManagerId != null && senderManagerId.getBusinessPartner() != null)
          info.addResult("inpemEscmCtsendlineemp", senderManagerId.getBusinessPartner().getId());
        else
          info.addResult("inpemEscmCtsendlineemp", inpemEscmCtsenderemp);
      }

      else if (inpLastFieldChanged.equals("inpemEscmCtreceiver")
          && (strFromBtype.equals("E") || strToBtype.equals("E"))) {

        useObj = OBDal.getInstance().get(User.class, inpemEscmCtreceiver);

        if (useObj != null)
          receiverManagerId = POReceiptHeaderCalloutDAO.getUserManagerId(useObj);

        if (receiverManagerId != null)
          info.addResult("inpemEscmCtreclinemng", receiverManagerId.getId());
        else
          info.addResult("inpemEscmCtreclinemng", inpemEscmCtreceiver);
      }

      else if (inpLastFieldChanged.equals("inpemEscmCtreceiveremp")
          && (strFromBtype.equals("E") || strToBtype.equals("E"))) {

        useObj = POReceiptHeaderCalloutDAO.getUserId(inpemEscmCtreceiveremp);

        if (useObj != null)
          receiverManagerId = POReceiptHeaderCalloutDAO.getUserManagerId(useObj);

        if (receiverManagerId != null && receiverManagerId.getBusinessPartner() != null)
          info.addResult("inpemEscmCtreclineemp", receiverManagerId.getBusinessPartner().getId());
        else
          info.addResult("inpemEscmCtreclineemp", inpemEscmCtreceiveremp);
      }

    }

    if (inpLastFieldChanged.equals("inpadOrgId")
        || inpLastFieldChanged.equals("inpemEscmReceivingtype")
            && !inpTabId.equals("CB9A2A4C6DB24FD19D542A78B07ED6C1")) {
      if (strReceivingtype.equals("IR")) {
        try {
          sql = "select EM_Escm_Warehousereceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IR' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmWarehousereceiver", rs.getString("EM_Escm_Warehousereceiver"));

          } else {
            info.addResult("inpemEscmWarehousereceiver", null);
          }

          query = "select em_escm_warehousekeeper from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IR' order by updated desc limit 1";
          st = conn.prepareStatement(query);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmWarehousekeeper", rs.getString("em_escm_warehousekeeper"));

          } else {
            info.addResult("inpemEscmWarehousekeeper", null);
          }

          sqlquery = "select em_escm_inventorymgr from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IR' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInventorymgr", rs.getString("em_escm_inventorymgr"));

          } else {
            info.addResult("inpemEscmInventorymgr", null);
          }

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          log4j.error("Exception in POReceiptHeaderCallout ", e);
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
          }
        }
      } else if (strReceivingtype.equals("DEL")) {
        try {
          sql = "select em_escm_delwhreceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'DEL' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmDelwhreceiver", rs.getString("em_escm_delwhreceiver"));

          } else {
            info.addResult("inpemEscmDelwhreceiver", null);
          }
          st.close();
          rs.close();

          query = "select em_escm_delwhkeeper from m_inout where ad_org_id = ?  and em_escm_receivingtype = 'DEL' order by updated desc limit 1";
          st = conn.prepareStatement(query);
          st.setString(1, strOrgid);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpemEscmDelwhkeeper", rs.getString("em_escm_delwhkeeper"));

          } else {
            info.addResult("inpemEscmDelwhkeeper", null);
          }
          sqlquery = "select em_escm_delinvmgr from m_inout where ad_org_id = ?  and em_escm_receivingtype = 'DEL' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpemEscmDelinvmgr", rs.getString("em_escm_delinvmgr"));

          } else {
            info.addResult("inpemEscmDelinvmgr", null);
          }

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          log4j.error("Exception in POReceiptHeaderCallout ", e);
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
          }
        }
      } else if (strReceivingtype.equals("INS")) {
        try {
          sql = "select em_escm_inswhkeeper from m_inout where ad_org_id = ?  and em_escm_receivingtype = 'INS' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInswhkeeper", rs.getString("em_escm_inswhkeeper"));

          } else {
            info.addResult("inpemEscmInswhkeeper", null);
          }

          query = "select em_escm_inswhreceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INS'  order by updated desc limit 1";
          st = conn.prepareStatement(query);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInswhreceiver", rs.getString("em_escm_inswhreceiver"));

          } else {
            info.addResult("inpemEscmInswhreceiver", null);
          }
          sqlquery = "select em_escm_insinvmgr from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INS' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpemEscmInsinvmgr", rs.getString("em_escm_insinvmgr"));

          } else {
            info.addResult("inpemEscmInsinvmgr", null);
          }

          sqlquery = "select em_escm_inventoryctrl from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INS' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();
          if (rs.next()) {
            info.addResult("inpemEscmInventoryctrl", rs.getString("em_escm_inventoryctrl"));
          } else {
            info.addResult("inpemEscmInventoryctrl", null);
          }
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          log4j.error("Exception in POReceiptHeaderCallout ", e);
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
          }
        }
      } else if (strReceivingtype.equals("INR")) {
        try {
          sql = "select c_bpartner.c_bpartner_id as bpartnerid,c_bpartner_location.c_bpartner_location_id as locationid from c_bpartner "
              + "left join c_bpartner_location on c_bpartner_location.c_bpartner_id = c_bpartner.c_bpartner_id "
              + "where c_bpartner.em_escm_defaultpartner='Y' and c_bpartner.ad_client_id='"
              + strClientId + "' limit 1";
          st = conn.prepareStatement(sql);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpcBpartnerId", rs.getString("bpartnerid"));
            info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
            info.addResult("inpcDoctypeId", "FF8080812C2ABFC6012C2B3BDF4A004E");
          }
          sql = "select EM_Escm_Warehousereceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INR' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmWarehousereceiver", rs.getString("EM_Escm_Warehousereceiver"));

          } else {
            info.addResult("inpemEscmWarehousereceiver", null);
          }
          sqlquery = "select em_escm_inventorymgr from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INR' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInventorymgr", rs.getString("em_escm_inventorymgr"));

          } else {
            info.addResult("inpemEscmInventorymgr", null);
          }
          sqlquery = "select EM_Escm_Appauthority from m_inout where ad_org_id = ? and em_escm_receivingtype = 'INR' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmAppauthority", rs.getString("EM_Escm_Appauthority"));

          } else {
            info.addResult("inpemEscmAppauthority", null);
          }

          // By default, returner id name should be logged in user name

          if (currentuser.getBusinessPartner() != null) {
            info.addResult("inpemEscmBname", currentuser.getBusinessPartner().getId());
            info.addResult("inpemEscmOwnername", currentuser.getBusinessPartner().getName());
          } else {
            info.addResult("inpemEscmBname", null);
          }

          // For field - Return Dept Mgr- Defaulted to preparer line manager employee name as per
          // HR
          // data.

          sqlquery = "  select hrbp.name as name from ad_user  user1 "
              + " join c_bpartner bp on user1.c_bpartner_id = bp.c_bpartner_id "
              + " join c_bpartner hrbp on bp.em_ehcm_manager = hrbp.em_efin_documentno "
              + " where user1.ad_user_id = ? and hrbp.ad_client_id  = ?";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, currentuser.getId());
          st.setString(2, OBContext.getOBContext().getCurrentClient().getId());

          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmReturndeptmgr", rs.getString("name"));
          } else {
            info.addResult("inpemEscmReturndeptmgr", null);
          }

          // if it is return transaction, default returner type should be Employee

          if (inpTabId.equals("72A6B3CA5BE848ACA976304375A5B7A6")) {
            info.addResult("inpemEscmBtype", "E");
          }

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          log4j.error("Exception in POReceiptHeaderCallout ", e);
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
          }
        }
      } else if (strReceivingtype.equals("IRT")) {
        try {
          sql = "select c_bpartner.c_bpartner_id as bpartnerid,c_bpartner_location.c_bpartner_location_id as locationid from c_bpartner "
              + "left join c_bpartner_location on c_bpartner_location.c_bpartner_id = c_bpartner.c_bpartner_id "
              + "where c_bpartner.em_escm_defaultpartner='Y' and c_bpartner.ad_client_id='"
              + strClientId + "' limit 1";
          st = conn.prepareStatement(sql);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpcBpartnerId", rs.getString("bpartnerid"));
            info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
            info.addResult("inpcDoctypeId", "FF8080812C2ABFC6012C2B3BDF4A004E");
          }
          sql = "select EM_Escm_Warehousereceiver from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IRT' order by updated desc limit 1";
          st = conn.prepareStatement(sql);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmWarehousereceiver", rs.getString("EM_Escm_Warehousereceiver"));

          } else {
            info.addResult("inpemEscmWarehousereceiver", null);
          }
          sqlquery = "select em_escm_inventorymgr from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IRT' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmInventorymgr", rs.getString("em_escm_inventorymgr"));

          } else {
            info.addResult("inpemEscmInventorymgr", null);
          }
          sqlquery = "select EM_Escm_Appauthority from m_inout where ad_org_id = ? and em_escm_receivingtype = 'IRT' order by updated desc limit 1";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strOrgid);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmAppauthority", rs.getString("EM_Escm_Appauthority"));

          } else {
            info.addResult("inpemEscmAppauthority", null);
          }
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          log4j.error("Exception in POReceiptHeaderCallout ", e);
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
          }
        }
      } else if (strReceivingtype.equals("LD")) {
        try {
          sql = "select c_bpartner.c_bpartner_id as bpartnerid,c_bpartner_location.c_bpartner_location_id as locationid from c_bpartner "
              + "left join c_bpartner_location on c_bpartner_location.c_bpartner_id = c_bpartner.c_bpartner_id "
              + "where c_bpartner.em_escm_defaultpartner='Y' and c_bpartner.ad_client_id='"
              + strClientId + "' limit 1";
          st = conn.prepareStatement(sql);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpcBpartnerId", rs.getString("bpartnerid"));
            info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
            info.addResult("inpcDoctypeId", "FF8080812C2ABFC6012C2B3BDF4A004E");
          }

        } catch (SQLException e) {
          // TODO Auto-generated catch block
          log4j.error("Exception in POReceiptHeaderCallout ", e);
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
          }
        }
      }
    }
    if (inpLastFieldChanged.equals("inpemEscmReceivingtype") && inpTabId.equals("296")) {
      if (strReceivingtype.equals(Constants.RECEIVING)) {
        info.addResult("inpemEscmTranstypevalue", Constants.RECEIVING);
      } else if (strReceivingtype.equals(Constants.SITE_RECEIVING)) {
        info.addResult("inpemEscmTranstypevalue", Constants.SITE_RECEIVING);
      } else if (strReceivingtype.equals(Constants.INSPECT)) {
        info.addResult("inpemEscmTranstypevalue", Constants.INSPECT);
      } else if (strReceivingtype.equals(Constants.DELIVERY)) {
        info.addResult("inpemEscmTranstypevalue", Constants.DELIVERY);
      } else if (strReceivingtype.equals(Constants.RETURN)) {
        info.addResult("inpemEscmTranstypevalue", Constants.RETURN);
      } else if (strReceivingtype.equals(Constants.PROJECT_RECEIVING)) {
        info.addResult("inpemEscmTranstypevalue", Constants.PROJECT_RECEIVING);
      }

      if (strReceivingtype.equals("PROJ")) {
        info.addResult("inpemEscmSitereceivedby", currentuser.getName());
      } else {
        info.addResult("inpemEscmSitereceivedby", "");
      }
    }

    if (inpLastFieldChanged.equals("inpporeference") && inpTabId.equals("296")) {
      try {
        sql = " select c_bpartner_id as bpartnerid,c_bpartner_location_id as locationid,c_doctype_id, eut_convert_to_hijri(to_char(dateacct,'yyyy-MM-dd'))  as dateacct ,em_escm_receivetype as receiveType from m_inout "
            + " where em_escm_receivingtype in ('SR','IR','INS','DEL','RET','PROJ') and poreference= ? and ad_client_id= ? and ad_org_id= ?"
            + " order by created desc limit 1";
        st = conn.prepareStatement(sql);
        st.setString(1, strporeference);
        st.setString(2, strClientId);
        st.setString(3, strOrgid);
        rs = st.executeQuery();

        if (rs.next()) {
          info.addResult("inpcBpartnerId", rs.getString("bpartnerid"));
          info.addResult("inpcBpartnerLocationId", rs.getString("locationid"));
          info.addResult("inpcDoctypeId", rs.getString("c_doctype_id"));
          info.addResult("inpdateacct", rs.getString("dateacct"));
          info.addResult("inpemEscmReceivetype", rs.getString("receiveType"));
        }
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        log4j.error("Exception in POReceiptHeaderCallout ", e);
      } finally {
        try {
          if (rs != null)
            rs.close();
          if (st != null)
            st.close();
        } catch (Exception e) {
          log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
        }
      }

    }
    if (inpLastFieldChanged.equals("inpemEscmIssuereason")
        && (inpTabId.equals("922927563BFC48098D17E4DC85DD504C")
            || inpTabId.equals("72A6B3CA5BE848ACA976304375A5B7A6")
            || inpTabId.equals("CB9A2A4C6DB24FD19D542A78B07ED6C1"))) {
      if (strIssueReason.equals("MA") || strIssueReason.equals("SA")
          || strIssueReason.equals("OB")) {
        info.addResult("inpemEscmBtype", "");
        info.addResult("inpemEscmBname", "");
      }
    }
    if (inpLastFieldChanged.equals("inpcOrderId")) {
      Order ord = OBDal.getInstance().get(Order.class, strOrderId);
      if (ord != null) {
        info.addResult("inpporeference", ord.getDocumentNo());
        info.addResult("inpcBpartnerId", ord.getBusinessPartner().getId());
        // System.out.println(ord.getEscmSubcontractors());
        info.addResult("inpemEscmSubcontractors", ord.getEscmSubcontractors());
      }
    }
    // change doctype field to po if po receipt is checked
    if (inpLastFieldChanged.equals("inpcDoctypeId")) {
      DocumentType doctype = OBDal.getInstance().get(DocumentType.class, doctypeId);
      if (doctype != null && doctype.isEscmIsporeceipt()) {
        info.addResult("inpemEscmDoctype", "PO");
      } else {
        info.addResult("inpemEscmDoctype", "MR");
      }
    }
    if (inpLastFieldChanged.equals("inpemEscmBname")
        && inpTabId.equals("72A6B3CA5BE848ACA976304375A5B7A6")) {

      if (strReceivingtype.equals("INR")) {

        try {
          sqlquery = "  select name from c_bpartner where c_bpartner_id= ? ";
          st = conn.prepareStatement(sqlquery);
          st.setString(1, strFromBname);
          rs = st.executeQuery();

          if (rs.next()) {
            info.addResult("inpemEscmOwnername", rs.getString("name"));

          } else {
            info.addResult("inpemEscmOwnername", "");
          }

        } catch (SQLException e) {
          if (log4j.isDebugEnabled()) {
            log4j.debug("error while executing query :" + sqlquery + e);
          }
        } finally {
          try {
            if (rs != null)
              rs.close();
            if (st != null)
              st.close();
          } catch (Exception e) {
            log4j.error("Exception while closing the statement in POReceiptHeaderCallout ", e);
          }
        }

      }
    }
    if (inpLastFieldChanged.equals("inpcDoctypeId") || inpLastFieldChanged.equals("inpcOrderId")) {
      DocumentType doctype = OBDal.getInstance().get(DocumentType.class, doctypeId);
      if (doctype != null && doctype.isEscmIsporeceipt()) {
        Order po = OBDal.getInstance().get(Order.class, strOrderId);
        if (po != null && po.getEscmReceivetype().equals("QTY")) {
          info.addResult("inpemEscmReceivetype", "QTY");
        } else if (po != null && po.getEscmReceivetype().equals("AMT")) {
          info.addResult("inpemEscmReceivetype", "AMT");
        } else {
          info.addResult("inpemEscmReceivetype", "QTY");
        }
      } else {
        info.addResult("inpemEscmReceivetype", "QTY");
      }
    }

  }
}
