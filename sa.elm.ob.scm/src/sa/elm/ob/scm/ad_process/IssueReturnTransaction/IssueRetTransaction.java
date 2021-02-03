package sa.elm.ob.scm.ad_process.IssueReturnTransaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Divya on 15/03/2017
 */

public class IssueRetTransaction extends DalBaseProcess {

  /**
   * This servlet class was responsible for Issue Request Submission Process with Approval
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(IssueRetTransaction.class);
  private final OBError obError = new OBError();

  @SuppressWarnings("resource")
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    boolean errorFlag = false;

    log.debug("entering into IssueRetTransaction Submit");
    try {
      OBContext.setAdminMode();
      final String receiptId = (String) bundle.getParams().get("M_InOut_ID").toString();
      ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class, receiptId);
      String DocStatus = inout.getEscmDocstatus();
      PreparedStatement st = null;
      int count = 0;
      MaterialTransaction trans = null;
      String query = null;
      List<Locator> locList = new ArrayList<Locator>();

      // check lines to submit
      if (inout.getMaterialMgmtShipmentInOutLineList().size() == 0) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTranAddLines@");
        bundle.setResult(result);
        return;
      }
      for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
        if (line.getEscmCustodyTransactionList().size() == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RetTran_LineQtyZero@");
          bundle.setResult(result);
          return;
        }
      }
      log.debug("Check getEscmReceivingtype:" + inout.getEscmReceivingtype());
      if (inout.getEscmReceivingtype().equals("INR")
          || inout.getEscmReceivingtype().equals("IRT")) {
        if (inout.getEscmReceivingtype().equals("INR")) {
          for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
            for (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) {
              log.debug(
                  "Check getEscmMrequestCustody:" + tran.getEscmMrequestCustody().getAlertStatus());
              if (tran.getEscmMrequestCustody().getAlertStatus().equals("RET")) {
                errorFlag = true;
                break;
              }
            }
          }
        }
        if (inout.getEscmReceivingtype().equals("IRT")) {
          for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
            for (Escm_custody_transaction tran : line.getEscmCustodyTransactionList()) {
              log.debug("Check getEscmMrequestCustody irt:"
                  + tran.getEscmMrequestCustody().getAlertStatus());
              if (tran.getEscmMrequestCustody().getAlertStatus().equals("IU")) {
                errorFlag = true;
                break;
              }
            }
          }
        }
        if (errorFlag) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_IssRet_TagAlrProcessed@");
          bundle.setResult(result);
          return;
        }
      }

      if (inout.getEscmReceivingtype().equals("LD")) {
        for (ShipmentInOutLine line : inout.getMaterialMgmtShipmentInOutLineList()) {
          if (line.getMovementQuantity().compareTo(BigDecimal.ZERO) == 0
              || line.getMovementQuantity().compareTo(BigDecimal.ZERO) < 0) {
            errorFlag = true;
            break;
          }
        }
        if (errorFlag) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_IR_Quantity@");
          bundle.setResult(result);
          return;
        }
      }

      // throw the error message while 2nd user try to approve while 1st user already reworked that
      // record with same role
      if ((!vars.getUser().equals(inout.getCreatedBy().getId())) && (DocStatus.equals("CO"))) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      if (inout.getEscmReceivingtype().equals("IRT")) {
        // check product stock exceeds and update failure reason
        query = "select sd.qtyonhand as avstock,movementqty,ln.m_product_id as product,ln.m_inoutline_id as lnid from m_inoutline ln "
            + " join m_product prd on prd.m_product_id=ln.m_product_id "
            + " join (select sum(qtyonhand) as qtyonhand,m_product_id from m_storage_detail where ad_org_id=? "
            + " and m_locator_id in (select m_locator_id from m_locator where m_warehouse_id = ? )"
            + " group by m_product_id ) sd on sd.m_product_id=prd.m_product_id "
            + " where m_inout_id= ? and (sd.qtyonhand-ln.movementqty) < 0 ";
        log.debug("Check query:" + query);
        ps = conn.prepareStatement(query);
        ps.setString(1, inout.getOrganization().getId());
        ps.setString(2, inout.getWarehouse().getId());
        ps.setString(3, receiptId);
        rs = ps.executeQuery();
        while (rs.next()) {
          errorFlag = true;
          ShipmentInOutLine line = OBDal.getInstance().get(ShipmentInOutLine.class,
              rs.getString("lnid"));
          line.setEscmFailurereason("Available quantity is " + rs.getString("avstock"));
          OBDal.getInstance().save(line);
          OBDal.getInstance().flush();
        }
        if (errorFlag) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_RequestQty_Exceeds@");
          bundle.setResult(result);
          return;
        }

        // check product stock exceeds and update failure reason for success
        query = "select sd.qtyonhand as avstock,movementqty,ln.m_product_id as product,ln.m_inoutline_id as lnid from m_inoutline ln "
            + " join m_product prd on prd.m_product_id=ln.m_product_id "
            + " join (select sum(qtyonhand) as qtyonhand,m_product_id from m_storage_detail where ad_org_id= ? "
            + "' and m_locator_id in (select m_locator_id from m_locator where m_warehouse_id = ? )"
            + " group by m_product_id ) sd on sd.m_product_id=prd.m_product_id "
            + " where m_inout_id= ? and (sd.qtyonhand-ln.movementqty) < 0 ";
        log.debug("Check query:" + query);
        ps = conn.prepareStatement(query);
        ps.setString(1, inout.getOrganization().getId());
        ps.setString(2, inout.getWarehouse().getId());
        ps.setString(3, receiptId);
        rs = ps.executeQuery();
        while (rs.next()) {
          ShipmentInOutLine line = OBDal.getInstance().get(ShipmentInOutLine.class,
              rs.getString("lnid"));
          line.setEscmFailurereason("");
          OBDal.getInstance().save(line);
          OBDal.getInstance().flush();
        }
      }
      if (!errorFlag) {
        if (DocStatus.equals("DR")) {

          for (ShipmentInOutLine inoutline : inout.getMaterialMgmtShipmentInOutLineList()) {
            if (inout.getEscmReceivingtype().equals("INR")
                || inout.getEscmReceivingtype().equals("IRT")) {
              trans = OBProvider.getInstance().get(MaterialTransaction.class);
              trans.setOrganization(inoutline.getOrganization());
              trans.setClient(inoutline.getClient());
              trans.setCreatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              trans.setUpdatedBy(OBDal.getInstance().get(User.class, vars.getUser()));
              trans.setCreationDate(new java.util.Date());
              trans.setUpdated(new java.util.Date());
              if (inout.getEscmReceivingtype().equals("INR"))
                trans.setMovementType("V+");
              else if (inout.getEscmReceivingtype().equals("IRT"))
                trans.setMovementType("V-");

              OBQuery<Locator> locator = OBDal.getInstance().createQuery(Locator.class,
                  " as e where e.warehouse.id=:warehouseId and e.default='Y' ");
              locator.setNamedParameter("warehouseId", inout.getWarehouse().getId());
              locator.setMaxResult(1);
              locList = locator.list();
              if (locList.size() > 0) {
                trans.setStorageBin(locList.get(0));
                // log4j.debug("getStorageBin:" + trans.getStorageBin());
              } else {
                errorFlag = true;
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_Locator(Empty)@");
                bundle.setResult(result);
                return;
              }

              trans.setProduct(inoutline.getProduct());
              trans.setMovementDate(inout.getMovementDate());
              if (inout.getEscmReceivingtype().equals("INR")) {
                trans.setMovementQuantity(inoutline.getMovementQuantity());
                trans.setEscmTransactiontype("INR");
              } else if (inout.getEscmReceivingtype().equals("IRT")) {

                int chkqty = Utility.ChkStoragedetOnhandQtyNeg(inoutline.getProduct().getId(),
                    locList.get(0).getId(), inoutline.getMovementQuantity(),
                    inoutline.getClient().getId());
                log.debug("chkqty:" + chkqty);
                if (chkqty == -1) {
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_StorageDetail_QtyonHand@");
                  bundle.setResult(result);
                  OBDal.getInstance().rollbackAndClose();
                  return;
                }

                trans.setMovementQuantity(inoutline.getMovementQuantity().negate());
                trans.setEscmTransactiontype("IRT");
              }
              trans.setGoodsShipmentLine(inoutline);
              trans.setUOM(OBDal.getInstance().get(UOM.class, inoutline.getUOM().getId()));

              OBDal.getInstance().save(trans);
            }
            // update custody trnasaction and custody detail based on inoutline

            for (Escm_custody_transaction objCustodytran : inoutline
                .getEscmCustodyTransactionList()) {
              // update custody detail status
              MaterialIssueRequestCustody objCustody = objCustodytran.getEscmMrequestCustody();
              if (inout.getEscmReceivingtype().equals("INR"))
                objCustody.setAlertStatus("RET");
              else if (inout.getEscmReceivingtype().equals("LD"))
                objCustody.setAlertStatus("LD");
              else if (inout.getEscmReceivingtype().equals("IRT")) {
                if (inout.getEscmIssuereason() != null && inout.getEscmIssuereason().equals("IS"))
                  objCustody.setAlertStatus("IU");
                else
                  objCustody.setAlertStatus(inout.getEscmIssuereason());
              }
              if (inout.getEscmIssuereason() != null && inout.getEscmIssuereason().equals("MA")) {
                objCustody.setBeneficiaryType("MA");
                objCustody.setBeneficiaryIDName(null);
              } else if (inout.getEscmIssuereason() != null
                  && (inout.getEscmIssuereason().equals("SA")
                      || inout.getEscmIssuereason().equals("OB"))) {
                objCustody.setBeneficiaryType(null);
                objCustody.setBeneficiaryIDName(null);
              } else {
                objCustody.setBeneficiaryType(inout.getEscmBtype());
                objCustody.setBeneficiaryIDName(inout.getEscmBname());
              }
              OBDal.getInstance().save(objCustody);
              // update custody transaction status
              if (inout.getEscmReceivingtype().equals("INR"))
                objCustodytran.setTransactiontype("RE");
              else if (inout.getEscmReceivingtype().equals("IRT")) {
                if (inout.getEscmIssuereason() != null && inout.getEscmIssuereason().equals("IS"))
                  objCustodytran.setTransactiontype("IRT");
                else
                  objCustodytran.setTransactiontype(inout.getEscmIssuereason());
              } else if (inout.getEscmReceivingtype().equals("LD"))
                objCustodytran.setTransactiontype("LD");
              objCustodytran.setTransactionDate(inout.getMovementDate());

              query = " select escm_custody_transaction_id from escm_custody_transaction where "
                  + "  escm_custody_transaction_id not in ('" + objCustodytran.getId()
                  + "' )    and escm_mrequest_custody_id ='" + objCustody.getId()
                  + "' and isprocessed = 'Y' order by created desc limit 1";
              st = conn.prepareStatement(query);
              log.debug("st query:" + st);
              rs = st.executeQuery();
              if (rs.next()) {
                Escm_custody_transaction updCustodytran = OBDal.getInstance().get(
                    Escm_custody_transaction.class, rs.getString("escm_custody_transaction_id"));
                updCustodytran.setReturnDate(inout.getMovementDate());
                OBDal.getInstance().save(updCustodytran);
              }
              objCustodytran.setProcessed(true);
              OBDal.getInstance().save(objCustodytran);
              if (st != null)
                st.close();
            }
          }

          inout.setUpdated(new java.util.Date());
          inout.setUpdatedBy(OBContext.getOBContext().getUser());
          // if (inout.getEscmReceivingtype().equals("INR"))
          // inout.setEscmDocstatus("ESCM_RET");
          // else if (inout.getEscmReceivingtype().equals("IRT"))
          inout.setEscmDocstatus("CO");
          inout.setDocumentStatus("CO");
          inout.setDocumentAction("--");
          OBDal.getInstance().save(inout);
          count = 1;

          OBDal.getInstance().flush();
        }

        if (count > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else {
          errorFlag = false;
        }
      }
      if (errorFlag) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage("Process Failed");
      }

      bundle.setResult(obError);
      OBDal.getInstance().save(inout);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exeception in Issue return transaction Submit:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {

      }
      OBContext.restorePreviousMode();
    }

  }
}