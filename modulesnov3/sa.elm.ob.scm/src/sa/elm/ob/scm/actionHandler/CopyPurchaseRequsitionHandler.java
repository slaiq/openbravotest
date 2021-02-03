package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.DataToJsonConverter;

import sa.elm.ob.scm.EscmRequisitionlineV;
import sa.elm.ob.utility.util.UtilityDAO;

public class CopyPurchaseRequsitionHandler extends BaseActionHandler {
  private static Logger log = Logger.getLogger(CopyPurchaseRequsitionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    // TODO Auto-generated method stub
    final DataToJsonConverter jsonConverter = new DataToJsonConverter();
    JSONObject json = new JSONObject();

    try {
      OBContext.setAdminMode();
      // JSONObject jsondata = new JSONObject(content);
      String requisitionId = (String) parameters.get("requisitionId");// jsondata.getString("requisitionId");
      String userId = (String) parameters.get("userId");// jsondata.getString("userId");
      String clientId = (String) parameters.get("clientId");// jsondata.getString("clientId");
      List<Organization> orglist = new ArrayList<Organization>();

      // chk user have department
      User user = OBDal.getInstance().get(User.class, userId);
      if (user.getBusinessPartner() == null || (user.getBusinessPartner() != null
          && user.getBusinessPartner().getEhcmDepartmentCode() == null)) {
        JSONObject json1 = new JSONObject();
        json1.put("message", "ESCM_PurReq_UsrNotDept");
        return json1;
      }
      // Boolean ispreference = false;
      /*
       * Boolean ispreference = Preferences.existsPreference("ESCM_ProcurementDirector", true, null,
       * null, user.getId(), null, null);
       */
      Requisition origrequisition = OBDal.getInstance().get(Requisition.class, requisitionId);

      /*
       * String preferenceValue = ""; try { preferenceValue =
       * Preferences.getPreferenceValue("ESCM_ProcurementDirector", true, clientId,
       * origrequisition.getOrganization().getId(), vars.getUser(), vars.getRole(), "800092"); }
       * catch (PropertyException e) { } if (preferenceValue != null && preferenceValue.equals("Y"))
       * ispreference = true;
       */
      Requisition copyrequisition = (Requisition) DalUtil.copy(origrequisition, false);
      copyrequisition.setDocumentStatus("DR");
      copyrequisition.setEscmDocStatus("DR");
      copyrequisition.setDocumentAction("CO");
      copyrequisition.setEscmDocaction("CO");
      copyrequisition.setEscmSpecNo(null);
      String seqno = UtilityDAO.getSequenceNo(OBDal.getInstance().getConnection(), clientId,
          "DocumentNo_M_Requisition", true);
      copyrequisition.setDocumentNo(seqno);
      copyrequisition.setCreationDate(new java.util.Date());
      copyrequisition.setCreatedBy(user);
      copyrequisition.setUpdatedBy(user);
      copyrequisition.setUserContact(user);
      copyrequisition.setUpdated(new java.util.Date());
      copyrequisition.setEscmSalesregion(user.getEscmSalesregion());
      copyrequisition.setEfinBudgetManencum(null);
      copyrequisition.setEfinEncumMethod("A");
      copyrequisition.setEFINUniqueCode(null);
      copyrequisition.setEfinEncumbered(false);
      copyrequisition.setEfinSkipencumbrance(false);
      // copyrequisition.setEscmProcesstype(origrequisition.getEscmProcesstype());
      if (copyrequisition.getUserContact().getBusinessPartner() != null && copyrequisition
          .getUserContact().getBusinessPartner().getEhcmDepartmentCode() != null) {
        OBQuery<Organization> org = OBDal.getInstance().createQuery(Organization.class,
            " as e where e.searchKey ='"
                + copyrequisition.getUserContact().getBusinessPartner().getEhcmDepartmentCode()
                + "'" + " and e.client.id='" + copyrequisition.getClient().getId() + "'");
        org.setMaxResult(1);
        orglist = org.list();
        if (orglist.size() > 0) {
          copyrequisition.setEscmDepartment(orglist.get(0));
        }
      }
      copyrequisition.setEutNextRole(null);
      /*
       * if (!ispreference) copyrequisition.setEscmProcesstype(null);
       */
      OBDal.getInstance().save(copyrequisition);

      log.debug("getProcurementRequisitionLineList:"
          + origrequisition.getProcurementRequisitionLineList().size());
      log.debug("copyrequisition:" + copyrequisition.getProcurementRequisitionLineList().size());
      Map<BigDecimal, BigDecimal> parentLnNo = new HashMap<BigDecimal, BigDecimal>();
      if (origrequisition.getProcurementRequisitionLineList().size() > 0) {
        for (RequisitionLine line : origrequisition.getProcurementRequisitionLineList()) {
          RequisitionLine copyLines = (RequisitionLine) DalUtil.copy(line);
          copyLines.setRequisition(copyrequisition);
          copyLines.setCreationDate(new java.util.Date());
          copyLines.setCreatedBy(OBDal.getInstance().get(User.class, userId));
          copyLines.setUpdatedBy(OBDal.getInstance().get(User.class, userId));
          copyLines.setUpdated(new java.util.Date());
          copyLines.setNeedByDate(new java.util.Date());
          copyLines.setEscmParentlineno(null);
          copyLines.setEfinCValidcombination(null);
          copyLines.setEfinUniquecodename(null);
          copyLines.setEfinBudEncumlines(null);
          if (line.getEscmParentlineno() != null) {
            RequisitionLine parentReqLnNo = OBDal.getInstance().get(RequisitionLine.class,
                line.getEscmParentlineno().getId());
            parentLnNo.put(new BigDecimal(line.getLineNo()),
                new BigDecimal(parentReqLnNo.getLineNo()));
          }
          OBDal.getInstance().save(copyLines);
        }
      }

      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(copyrequisition);
      int mapSize = parentLnNo.size();
      if (parentLnNo != null && mapSize > 0) {
        OBQuery<RequisitionLine> newLines = OBDal.getInstance().createQuery(RequisitionLine.class,
            " as e where requisition.id='" + copyrequisition.getId() + "'");

        if (newLines != null && newLines.list().size() > 0) {
          for (int i = 0; i < newLines.list().size(); i++) {
            RequisitionLine lns = newLines.list().get(i);
            Long lineNo = lns.getLineNo();
            BigDecimal parentLineno = parentLnNo.get(new BigDecimal(lineNo));
            if (parentLineno != null) {
              OBQuery<RequisitionLine> parentId = OBDal.getInstance()
                  .createQuery(RequisitionLine.class, " as e where e.lineNo ='" + parentLineno
                      + "' and requisition.id='" + copyrequisition.getId() + "'");
              if (parentId != null && parentId.list().size() > 0) {
                RequisitionLine parentLn = parentId.list().get(0);
                EscmRequisitionlineV objParentLine = OBDal.getInstance()
                    .get(EscmRequisitionlineV.class, parentLn.getId());
                if (objParentLine != null) {
                  lns.setEscmParentlineno(objParentLine);
                  OBDal.getInstance().save(lns);
                }
              }
            }

          }
        }
      }
      json = jsonConverter.toJsonObject(copyrequisition, DataResolvingMode.FULL);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      // json.put("message", "Success");
      return json;

    } catch (Exception e) {
      log.error("Exception in CopyPurchaseRequsitionHandler :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
