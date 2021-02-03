package sa.elm.ob.scm.ad_process.IssueRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.ESCMProductCategoryV;
import sa.elm.ob.scm.MaterialIssueRequest;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.MaterialIssueRequestLine;

/**
 * @author Gopalakrishnan on 13/03/2017
 */

public class IrGenerateTag extends DalBaseProcess {

  /**
   * This servlet class was responsible to insert records in custody details on IssueRequest Window
   * 
   */
  private static final Logger log = LoggerFactory.getLogger(IrGenerateTag.class);
  private final OBError obError = new OBError();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub

    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    Connection conn = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String existingDocNo = "1000000001", query = "";
    int custodyCount = 0, deliveredQty = 0;

    log.debug("entering into Generate Tag");
    try {
      OBContext.setAdminMode();
      String strRequisitionId = (String) bundle.getParams().get("Escm_Material_Request_ID");
      MaterialIssueRequest objRequest = OBDal.getInstance().get(MaterialIssueRequest.class,
          strRequisitionId);

      // get Line List
      // get recent tag number
      OBQuery<MaterialIssueRequestCustody> objCustodyQry = OBDal.getInstance().createQuery(
          MaterialIssueRequestCustody.class,
          "as e where e.organization.id=:orgID order by creationDate desc");
      objCustodyQry.setNamedParameter("orgID", objRequest.getOrganization().getId());
      objCustodyQry.setMaxResult(1);
      if (objCustodyQry.list().size() > 0) {
        MaterialIssueRequestCustody recentObj = objCustodyQry.list().get(0);
        if (recentObj.getDocumentNo() != null && StringUtils.isNotEmpty(recentObj.getDocumentNo()))
          existingDocNo = String.valueOf(Integer.parseInt(recentObj.getDocumentNo()) + 1);
      }

      // make custody for only custody products
      query = " select escm_material_reqln_id from escm_material_reqln ln "
          + " join m_product prd on prd.m_product_id=ln.m_product_id "
          + " join (select escm_deflookups_typeln_id from escm_deflookups_type lt "
          + " join escm_deflookups_typeln ltl on ltl.escm_deflookups_type_id=lt.escm_deflookups_type_id "
          + " where lt.reference='PST' and ltl.value='CUS') cusref on cusref.escm_deflookups_typeln_id=prd.em_escm_stock_type "
          + " where ln.escm_material_request_id='" + strRequisitionId + "'";
      ps = conn.prepareStatement(query);
      rs = ps.executeQuery();
      while (rs.next()) {
        // check already existing custody line count
        MaterialIssueRequestLine objLineList = OBDal.getInstance()
            .get(MaterialIssueRequestLine.class, rs.getString("escm_material_reqln_id"));
        custodyCount = objLineList.getEscmMrequestCustodyList().size();
        deliveredQty = objLineList.getDeliveredQantity().intValue();
        log.debug("existingDocNo:" + existingDocNo);
        log.debug("deliveredQty:" + deliveredQty);
        // no custody line insert the custodies line
        if (custodyCount == 0) {
          for (int i = 1; i <= deliveredQty; i++) {
            // get existing tag no
            MaterialIssueRequestCustody objCustody = OBProvider.getInstance()
                .get(MaterialIssueRequestCustody.class);
            Product objProduct = OBDal.getInstance().get(Product.class,
                objLineList.getProduct().getId());
            ESCMProductCategoryV prdcat = OBDal.getInstance().get(ESCMProductCategoryV.class,
                objProduct.getProductCategory());
            objCustody.setProductCategory(prdcat);
            objCustody.setDocumentNo(existingDocNo);
            objCustody.setQuantity(BigDecimal.ONE);
            objCustody.setDescription(objLineList.getDescription());
            objCustody.setAlertStatus("N");
            objCustody.setOrganization(objLineList.getOrganization());
            objCustody.setEscmMaterialReqln(objLineList);
            objCustody.setProduct(objProduct);
            if (objProduct.getEscmCusattribute() != null)
              objCustody.setAttributeSet(objProduct.getEscmCusattribute());
            objCustody.setBeneficiaryType(objRequest.getBeneficiaryType());
            objCustody.setBeneficiaryIDName(objRequest.getBeneficiaryIDName());
            objCustody.setProcurement(objLineList.getConditions());
            OBDal.getInstance().save(objCustody);
            OBDal.getInstance().flush();
            existingDocNo = String.valueOf(Integer.parseInt(existingDocNo) + 1);

          }
        } else if (deliveredQty < custodyCount) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_delivered_qty(less)@");
          bundle.setResult(result);
          return;
        }
        if ((deliveredQty > custodyCount) && custodyCount != 0) {
          // find diff and insert custody lines
          int diff = deliveredQty - custodyCount;
          for (int i = 1; i <= diff; i++) {
            // get existing tag no
            MaterialIssueRequestCustody objCustody = OBProvider.getInstance()
                .get(MaterialIssueRequestCustody.class);
            Product objProduct = OBDal.getInstance().get(Product.class,
                objLineList.getProduct().getId());
            ESCMProductCategoryV prdcat = OBDal.getInstance().get(ESCMProductCategoryV.class,
                objProduct.getProductCategory());
            objCustody.setProductCategory(prdcat);
            objCustody.setDocumentNo(existingDocNo);
            objCustody.setQuantity(BigDecimal.ONE);
            objCustody.setDescription(objLineList.getDescription());
            objCustody.setAlertStatus("N");
            objCustody.setOrganization(objLineList.getOrganization());
            objCustody.setEscmMaterialReqln(objLineList);
            objCustody.setProduct(objProduct);
            objCustody.setProcurement(objLineList.getConditions());
            OBDal.getInstance().save(objCustody);
            OBDal.getInstance().flush();
            existingDocNo = String.valueOf(Integer.parseInt(existingDocNo) + 1);
          }

        }
      }
      obError.setType("Success");
      obError.setTitle("Success");
      obError.setMessage("Process Completed Successfully");
      bundle.setResult(obError);
      OBDal.getInstance().save(objRequest);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      log.error("Exeception in Generate Tag:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      // close db connection
      try {
        if (ps != null)
          ps.close();
        if (rs != null)
          rs.close();
      } catch (Exception e) {
      }
      OBContext.restorePreviousMode();
    }

  }
}
