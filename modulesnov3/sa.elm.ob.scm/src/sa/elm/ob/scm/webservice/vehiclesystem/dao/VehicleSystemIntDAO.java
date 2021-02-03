package sa.elm.ob.scm.webservice.vehiclesystem.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.plm.Attribute;
import org.openbravo.model.common.plm.AttributeInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.BeneficiaryView;
import sa.elm.ob.scm.Escm_custody_transaction;
import sa.elm.ob.scm.MaterialIssueRequestCustody;
import sa.elm.ob.scm.webservice.vehiclesystem.constant.VehicleSystemConstants;
import sa.elm.ob.scm.webservice.vehiclesystem.dto.CustodyTagDetailsIntResponse;
import sa.elm.ob.scm.webservice.vehiclesystem.dto.ProductDetailsIntRequest;
import sa.elm.ob.scm.webservice.vehiclesystem.util.StringValidationUtil;

/**
 * This class is used to create and retrive custody details
 * 
 * @author kazim,gopal
 */
public class VehicleSystemIntDAO {

  private static final Logger log4j = LoggerFactory.getLogger(VehicleSystemIntDAO.class);

  public static boolean updateCustody(ProductDetailsIntRequest request) throws Exception {
    boolean dataInsert = false;

    try {
      // Find Existing Custody for Tag Based on Employee
      MaterialIssueRequestCustody objCustody = StringValidationUtil
          .findCustodyDetails(request.getTagNo(), request.getCurrentBeneficiaryIDName());

      // Find last Custody Transaction for tag and update movement date
      Boolean isUpdated = findAndupdateReceentCustodyTransaction(objCustody.getId());

      // Create New CustodyTrasnaction and Change Custody to new beneficiary
      if (isUpdated) {
        dataInsert = insertAndUpdateCustodyTransaction(request, objCustody);
      }
    } catch (Exception e) {
      dataInsert = false;
      log4j.error("updateCustody " + e);
      throw new Exception(e.getMessage(), e);
    }

    return dataInsert;
  }

  private static boolean insertAndUpdateCustodyTransaction(ProductDetailsIntRequest request,
      MaterialIssueRequestCustody objCustody) {

    // TODO Auto-generated method stub
    PreparedStatement st = null;
    ResultSet rs = null;
    Connection conn = OBDal.getInstance().getConnection();
    Boolean isUpdated = false;
    Long custline = (long) 10;
    BeneficiaryView newBenificiary = null, oldBeneficiary = null;
    try {
      newBenificiary = OBDal.getInstance().get(BeneficiaryView.class,
          StringValidationUtil.getEmployeeDetails(request.getNewBeneficiaryIDName()));
      oldBeneficiary = objCustody.getBeneficiaryIDName();
      st = conn.prepareStatement(
          " select coalesce(max(line2),0)+10 as lineno from escm_custody_transaction where escm_mrequest_custody_id  = ? ");
      st.setString(1, objCustody.getId());
      rs = st.executeQuery();
      if (rs.next()) {
        custline = rs.getLong("lineno");

      }
      // Update Custody to new beneficiary
      objCustody.setAlertStatus("IU");
      objCustody.setBeneficiaryType("E");
      objCustody.setBeneficiaryIDName(newBenificiary);
      OBDal.getInstance().save(objCustody);
      // Insert New Transaction Details
      Escm_custody_transaction custtransaction = OBProvider.getInstance()
          .get(Escm_custody_transaction.class);

      custtransaction.setClient(objCustody.getClient());
      custtransaction.setOrganization(objCustody.getOrganization());
      custtransaction.setCreationDate(new java.util.Date());
      custtransaction.setCreatedBy(objCustody.getCreatedBy());
      custtransaction.setUpdated(new java.util.Date());
      custtransaction.setUpdatedBy(objCustody.getUpdatedBy());
      custtransaction.setActive(true);
      custtransaction.setBname(oldBeneficiary);
      custtransaction.setBtype(objCustody.getBeneficiaryType());
      // custtransaction.setGoodsShipmentLine(inoutline);
      custtransaction.setLine2(custline);
      custtransaction.setLineNo((long) 10);
      // custtransaction.setDocumentNo(inout.getDocumentNo());
      custtransaction.setEscmMrequestCustody(objCustody);
      custtransaction.setTransactiontype("TR");
      custtransaction.setTransactionDate(new Date());
      custtransaction.setComments(VehicleSystemConstants.webServiceDescription);
      custtransaction.setTransactionreason(VehicleSystemConstants.webServiceDescription);
      custtransaction.setProcessed(true);
      OBDal.getInstance().save(custtransaction);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(objCustody);
      OBDal.getInstance().refresh(custtransaction);
      isUpdated = true;
    } catch (Exception e) {
      isUpdated = false;
      log4j.error("Error while creating Custody Transaction", e);
      OBDal.getInstance().rollbackAndClose();
    }
    return isUpdated;
  }

  private static Boolean findAndupdateReceentCustodyTransaction(String custodyId) {
    // TODO Auto-generated method stub
    PreparedStatement st = null;
    ResultSet rs = null;
    Connection conn = OBDal.getInstance().getConnection();
    Boolean isUpdate = false;
    try {
      String query = " select escm_custody_transaction_id from escm_custody_transaction where "
          + " escm_mrequest_custody_id = ? " + " and isprocessed='Y' order by created desc limit 1";
      st = conn.prepareStatement(query);
      st.setString(1, custodyId);
      rs = st.executeQuery();
      if (rs.next()) {
        Escm_custody_transaction updCustodytran = OBDal.getInstance()
            .get(Escm_custody_transaction.class, rs.getString("escm_custody_transaction_id"));
        updCustodytran.setReturnDate(new Date());
        OBDal.getInstance().save(updCustodytran);
        isUpdate = true;
      }
    } catch (Exception e) {
      log4j.error("findAndUpdateCustody", e);
      OBDal.getInstance().rollbackAndClose();
    }

    return isUpdate;
  }

  public static List<CustodyTagDetailsIntResponse> detailsByEmployeeCode(String employeeId)
      throws Exception {

    List<CustodyTagDetailsIntResponse> list = new ArrayList<CustodyTagDetailsIntResponse>();

    try {
      OBContext.setAdminMode();
      OBQuery<MaterialIssueRequestCustody> custodyDetailsQry = OBDal.getInstance().createQuery(
          MaterialIssueRequestCustody.class, "as e where e.beneficiaryIDName.id =:employeeID");
      custodyDetailsQry.setNamedParameter("employeeID", employeeId);
      custodyDetailsQry.setFilterOnReadableClients(false);
      custodyDetailsQry.setFilterOnReadableOrganization(false);
      if (custodyDetailsQry.list().size() > 0) {
        for (MaterialIssueRequestCustody custodyDetail : custodyDetailsQry.list()) {
          list.add(mappingCustodyDetails(custodyDetail));
        }
      }
    } catch (Exception e) {
      log4j.error("detailsByProductNumber -> Exception", e);
      throw new Exception(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return list;
  }

  /**
   * Mapping the Custody details into custody DTO
   * 
   * @param custodyDetail
   * @return
   */
  private static CustodyTagDetailsIntResponse mappingCustodyDetails(
      MaterialIssueRequestCustody custodyDetail) {
    // TODO Auto-generated method stub
    CustodyTagDetailsIntResponse custodyTagDTO = new CustodyTagDetailsIntResponse();
    custodyTagDTO.setOrganization(
        custodyDetail.getOrganization() == null ? "0" : custodyDetail.getOrganization().getName());
    custodyTagDTO.setTagNo(custodyDetail.getDocumentNo());
    custodyTagDTO.setCurrentBeneficiaryType("Employee");
    custodyTagDTO.setCurrentBeneficiaryIDName(custodyDetail.getBeneficiaryIDName().getSearchKey());
    custodyTagDTO.setItemCode(new BigDecimal(custodyDetail.getProduct().getSearchKey()));
    custodyTagDTO.setItemName(custodyDetail.getProduct().getName());
    custodyTagDTO.setItemDescription(custodyDetail.getProduct().getDescription());
    custodyTagDTO.setAttributeSet(
        custodyDetail.getAttributeSet() == null ? "" : custodyDetail.getAttributeSet().getName());
    custodyTagDTO
        .setStatus(VehicleSystemConstants.Status.getTextStatus(custodyDetail.getAlertStatus()));
    custodyTagDTO.setMainCategory(
        custodyDetail.getProduct().getProductCategory().getEscmProductCategory() == null ? ""
            : custodyDetail.getProduct().getProductCategory().getEscmProductCategory().getName());
    custodyTagDTO.setSubCategory(custodyDetail.getProduct().getProductCategory() == null ? ""
        : custodyDetail.getProduct().getProductCategory().getName());
    if (custodyDetail.getAttributeSetValue() != null) {
      String custodyId = custodyDetail.getAttributeSetValue().getId();
      custodyTagDTO.setSerialNumber(
          findAttributeValue(custodyId, findAttributeId(VehicleSystemConstants.SerialNumber))
              .toString());
      custodyTagDTO.setPlateNumber(
          findAttributeValue(custodyId, findAttributeId(VehicleSystemConstants.PlateNumber))
              .toString());

      custodyTagDTO.setBodyNumber(
          findAttributeValue(custodyId, findAttributeId(VehicleSystemConstants.BodyNumber))
              .toString());

      custodyTagDTO.setFactoryYear(
          findAttributeValue(custodyId, findAttributeId(VehicleSystemConstants.FactoryYear))
              .toString());

      custodyTagDTO.setTradeMark(
          findAttributeValue(custodyId, findAttributeId(VehicleSystemConstants.TradeMark))
              .toString());

      custodyTagDTO.setCylinderNumber(
          findAttributeValue(custodyId, findAttributeId(VehicleSystemConstants.CylinderNumber)));

      custodyTagDTO.setFuelType(
          findAttributeValue(custodyId, findAttributeId(VehicleSystemConstants.FuelType))
              .toString());
      custodyTagDTO.setColor(
          findAttributeValue(custodyId, findAttributeId(VehicleSystemConstants.Color)).toString());
      custodyTagDTO.setCustodyDescription(custodyDetail.getAttributeSetValue().getDescription());
    }

    return custodyTagDTO;
  }

  private static String findAttributeValue(String attributeSetInstanceId, String attributeId) {
    // TODO Auto-generated method stub
    String attributeValue = "";
    OBQuery<AttributeInstance> attInsQry = OBDal.getInstance().createQuery(AttributeInstance.class,
        "as e where e.attribute.id =:attributeId and e.attributeSetValue.id =:attributeSetInsId");
    attInsQry.setNamedParameter("attributeId", attributeId);
    attInsQry.setNamedParameter("attributeSetInsId", attributeSetInstanceId);
    attInsQry.setFilterOnReadableClients(false);
    attInsQry.setFilterOnReadableOrganization(false);
    if (attInsQry.list().size() > 0) {
      attributeValue = attInsQry.list().get(0).getSearchKey();
      if (StringUtils.isEmpty(attributeValue))
        attributeValue = attInsQry.list().get(0) == null ? ""
            : attInsQry.list().get(0).getAttributeValue().getName();
    }
    return attributeValue == null ? "" : attributeValue;
  }

  private static String findAttributeId(String attributeName) {
    // TODO Auto-generated method stub
    String attributeId = "";
    OBQuery<Attribute> attrQry = OBDal.getInstance().createQuery(Attribute.class,
        " as e where lower(replace(e.description,' ','')) =:attributeName");
    attrQry.setNamedParameter("attributeName", attributeName.toLowerCase().replace(" ", ""));
    attrQry.setFilterOnReadableClients(false);
    attrQry.setFilterOnReadableOrganization(false);
    if (attrQry.list().size() > 0) {
      attributeId = attrQry.list().get(0).getId();
    }
    return attributeId == null ? "" : attributeId;
  }

  public static CustodyTagDetailsIntResponse detailsByEmployeeAndTagCode(String employeeId,
      String tagCode) throws Exception {

    CustodyTagDetailsIntResponse CustodyTagDetailsIntResponseDTO = new CustodyTagDetailsIntResponse();

    try {

      OBContext.setAdminMode();
      OBQuery<MaterialIssueRequestCustody> custodyDetailsQry = OBDal.getInstance().createQuery(
          MaterialIssueRequestCustody.class,
          "as e where e.beneficiaryIDName.id =:employeeID and e.documentNo =:tagNo");
      custodyDetailsQry.setNamedParameter("employeeID", employeeId);
      custodyDetailsQry.setNamedParameter("tagNo", tagCode);
      custodyDetailsQry.setFilterOnReadableClients(false);
      custodyDetailsQry.setFilterOnReadableOrganization(false);

      if (custodyDetailsQry.list().size() > 0) {
        CustodyTagDetailsIntResponseDTO = mappingCustodyDetails(custodyDetailsQry.list().get(0));
      }
    } catch (Exception e) {
      log4j.error("detailsByProductNumber -> Exception", e);
      throw new Exception(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return CustodyTagDetailsIntResponseDTO;
  }
}