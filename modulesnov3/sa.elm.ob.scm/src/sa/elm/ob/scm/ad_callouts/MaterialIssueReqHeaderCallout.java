package sa.elm.ob.scm.ad_callouts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Expression;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.utility.util.Utility;

/*
 * automatically fill the signature group fields based on last record and organization.
 */
@SuppressWarnings("deprecation")
public class MaterialIssueReqHeaderCallout extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("resource")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    Connection conn = OBDal.getInstance().getConnection();
    String inpLastFieldChanged = info.vars.getStringParameter("inpLastFieldChanged");
    String strOrgid = info.vars.getStringParameter("inpadOrgId");
    String strBeneficiary = info.vars.getStringParameter("inpbeneficiaryName");
    String strBeneficiaryType = info.vars.getStringParameter("inpbeneficiaryType");
    String inpmWarehouseId = info.vars.getStringParameter("inpmWarehouseId");
    String inpmirId = info.vars.getStringParameter("inpescmMaterialRequestId");
    String strLineManager = "", receiverName = "", warehouseKeeper = "";
    PreparedStatement ps = null;
    ResultSet rs = null, rs1 = null;
    log4j.debug("inpLastFieldChanged>" + inpLastFieldChanged);
    if (inpLastFieldChanged.equals("inpadOrgId")) {
      try {
        OBCriteria<MaterialIssueRequest> mreq = OBDal.getInstance().createCriteria(
            MaterialIssueRequest.class);
        mreq.add(Expression.eq("organization.id", strOrgid));
        mreq.addOrderBy("updated", false);
        mreq.setMaxResults(1);

        strLineManager = Utility.getUserLineManager(OBContext.getOBContext().getUser());
        /* Mantis Issue -5954 */
        ps = conn
            .prepareStatement("select coalesce((select name from ad_user where ad_user_id=? ), '') AS defaultvalue from ad_preference pref "
                + " where property ='ESCM_WarehouseKeeper' and value='Y'  "
                + " and ((ad_user_id =?) or (visibleat_role_id=? and ad_user_id is null))");
        ps.setString(1, info.vars.getUser());
        // ps.setString(2, info.vars.getOrg());
        ps.setString(2, info.vars.getUser());
        ps.setString(3, info.vars.getRole());
        log4j.info("qry>" + ps.toString());
        rs = ps.executeQuery();
        if (rs.next()) {
          warehouseKeeper = rs.getString("defaultvalue");
          info.addResult("inpwarehouseKeeper", rs.getString("defaultvalue"));
        } else {
          ps = conn
              .prepareStatement("select lns.ad_role_id, usr.name as warehousekeeper from eut_documentrule_lines lns "
                  + " left join eut_documentrule_header hdr on hdr.eut_documentrule_header_id=lns.eut_documentrule_header_id "
                  + " left join ad_preference pref on pref.visibleat_role_id=lns.ad_role_id "
                  + " join ad_user_roles usrrol on usrrol.ad_role_id=pref.visibleat_role_id "
                  + " join ad_user usr on (usr.ad_user_id=usrrol.ad_user_id or usr.ad_user_id=pref.ad_user_id) "
                  + " where document_type ='EUT_112' and pref.property ='ESCM_WarehouseKeeper' and pref.value='Y' and usrrol.ad_org_id=? limit 1");
          ps.setString(1, info.vars.getOrg());
          log4j.info("qry>" + ps.toString());
          rs = ps.executeQuery();
          if (rs.next()) {
            warehouseKeeper = rs.getString("warehousekeeper");
            info.addResult("inpwarehouseKeeper", rs.getString("warehousekeeper"));
          }
        }

        /*
         * http://182.18.161.127/mantis/view.php?id=5204
         */
        info.addResult("inprequestManager", strLineManager);
        if (mreq.list() != null && mreq.list().size() > 0) {
          MaterialIssueRequest mrequest = mreq.list().get(0);
          info.addResult("inpinventoryMgr", mrequest.getInventoryMgr());
          if (warehouseKeeper == null || warehouseKeeper.equals(""))
            info.addResult("inpwarehouseKeeper", mrequest.getWarehouseKeeper());
          info.addResult("inpfinalApprover", mrequest.getFinalApprover());
        } else {
          info.addResult("inpinventoryMgr", null);
          if (warehouseKeeper == null || warehouseKeeper.equals(""))
            info.addResult("inpwarehouseKeeper", null);
          info.addResult("inpfinalApprover", null);
        }

      } catch (Exception e) {
        log4j.debug("callout in material issue request header:" + e);
        e.printStackTrace();
      }
    }
    if (inpLastFieldChanged.equals("inpmWarehouseId") && StringUtils.isNotBlank(inpmirId)) {
      try {
        MaterialIssueRequest mir = Utility.getObject(MaterialIssueRequest.class, inpmirId);
        if (mir.getEscmMaterialReqlnList().size() > 0) {
          ps = conn
              .prepareStatement("select m_product_id from escm_material_reqln where escm_material_request_id=? ");
          ps.setString(1, inpmirId);
          rs = ps.executeQuery();
          while (rs.next()) {
            ps = conn
                .prepareStatement("select sum(qtyonhand) as qtyonhand from m_storage_detail strdt "
                    + " left join m_locator loc on loc.m_locator_id=strdt.m_locator_id "
                    + " where m_product_id = ? and loc.m_warehouse_id = ? "
                    + " group by m_warehouse_id, m_product_id ");
            ps.setString(1, rs.getString("m_product_id"));
            ps.setString(2, inpmWarehouseId);
            rs1 = ps.executeQuery();
            if (rs1.next()) {
              ps = conn
                  .prepareStatement("update escm_material_reqln set onhand_qty=?, updated=now() where m_product_id=? ");
              ps.setBigDecimal(1, rs1.getBigDecimal("qtyonhand"));
              ps.setString(2, rs.getString("m_product_id"));
              ps.executeUpdate();
            } else {
              ps = conn
                  .prepareStatement("update escm_material_reqln set onhand_qty=0, updated=now() where m_product_id=? ");
              ps.setString(1, rs.getString("m_product_id"));
              ps.executeUpdate();
            }
          }
          info.addResult("JSEXECUTE", " OB.MainView.TabSet.selectChildTab()");
          info.addResult("JSEXECUTE",
              "OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewGrid.refreshGrid()");
        }
      } catch (SQLException e) {
        // TODO Auto-generated catch block
        log4j.debug("callout in material issue request header:" + e);
      }
    }
    /*
     * http://182.18.161.127/mantis/view.php?id=5204
     */
    if (inpLastFieldChanged.equals("inpbeneficiaryName")) {
      if ("E".equals(strBeneficiaryType)) {
        strLineManager = Utility.getEmployeeLineManager(strBeneficiary);
        info.addResult("inprequestManager", strLineManager);
        receiverName = Utility.getObject(BusinessPartner.class, strBeneficiary).getName();
        info.addResult("inpreceiver", receiverName);
      } else {
        info.addResult("inpreceiver", null);
      }
    } else {
      strLineManager = Utility.getUserLineManager(OBContext.getOBContext().getUser());
      info.addResult("inprequestManager", strLineManager);
    }
  }
}
