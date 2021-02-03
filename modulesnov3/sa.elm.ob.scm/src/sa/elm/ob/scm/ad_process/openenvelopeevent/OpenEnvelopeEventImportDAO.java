package sa.elm.ob.scm.ad_process.openenvelopeevent;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.finance.EfinBank;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.ad_callouts.BudgetAdjustmentCallout;
import sa.elm.ob.finance.ad_callouts.dao.HijiridateDAO;
import sa.elm.ob.scm.ESCMBGDocumentnoV;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.ESCM_Csv_Import_Oee_Bgdet;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.Escm_Csv_Import_Oee_Prop;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.Escmbidsuppliers;
import sa.elm.ob.scm.Escmopenenvcommitee;
import sa.elm.ob.scm.Escmsalesvoucher;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.ad_callouts.dao.OpenEnvelopeDAO;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class OpenEnvelopeEventImportDAO {

  private static final Logger log4j = Logger.getLogger(OpenEnvelopeEventImportDAO.class);
  @SuppressWarnings("unused")
  private static Connection conn = null;
  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
  Date currentDate = new Date();
  String formattedCurrentDateStr = dateFormat.format(currentDate);

  public OpenEnvelopeEventImportDAO(Connection con) {
    this.conn = con;
  }

  public JSONObject processUploadedCsvFile(String inpOpenEnvelopeEventId, VariablesSecureApp vars) {
    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;
    Escmopenenvcommitee openEnvelopeEventId = OBDal.getInstance().get(Escmopenenvcommitee.class,
        inpOpenEnvelopeEventId);

    try {
      OBContext.setAdminMode(true);

      isSuccess = addOrUpdateProposalAttribute(openEnvelopeEventId, vars);
      OBDal.getInstance().flush();

      log4j.debug("addOrUpdatePOReceiptLines isSuccess" + isSuccess);

      if (isSuccess == 0) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "0");
        jsonresult.put("statusMessage", OBMessageUtils.messageBD("Escm_Import_Record_Failed"));
      } else {
        jsonresult = new JSONObject();
        jsonresult.put("status", "1");
        jsonresult.put("statusMessage", OBMessageUtils.messageBD("Escm_Import_Record_Success"));
      }

    } catch (Exception e) {
      jsonresult = new JSONObject();
      try {
        jsonresult.put("status", "1");
        jsonresult.put("statusMessage", e.getMessage());
      } catch (JSONException e1) {
        log4j.error("Exception in processUploadedCsvFile ", e1);
      }
      log4j.error("Exception in processUploadedCsvFile ", e);
    } finally {
      deleteCSVImportEntries();
      OBContext.restorePreviousMode();
    }
    return jsonresult;
  }

  private int addOrUpdateProposalAttribute(Escmopenenvcommitee openEnvelopeEventId,
      VariablesSecureApp vars) {
    try {
      String currencyId = null;
      String bPartnerId = null;
      Long lineNo;
      Boolean isValidSupplier = false, isSupplierExists = false, isProposalAttrExists = false;
      Escmbidsuppliers supplierDet = null;
      String bankId = null;
      EscmProposalMgmt propMgmt = null;
      Date today = new Date();
      String yearId = null;
      String budgInitialId = null;
      String strProposalWindowId = "CAF2D3EEF3B241018C8F65E8F877B29F";
      String proposalManagementId = null;
      EfinBank bankObj = null;
      // Get Old Proposal entries from Temporary table
      OBQuery<Escm_Csv_Import_Oee_Prop> csvImportQryExisting = OBDal.getInstance().createQuery(
          Escm_Csv_Import_Oee_Prop.class,
          " as e where e.openEnvelopID = :OeeId and e.isnewline='N' order by e.creationDate ");
      csvImportQryExisting.setNamedParameter("OeeId", openEnvelopeEventId);
      List<Escm_Csv_Import_Oee_Prop> csvImportExistingList = csvImportQryExisting.list();

      for (Escm_Csv_Import_Oee_Prop csvImportLne : csvImportExistingList) {

        OBQuery<EscmProposalAttribute> existingPropAttr = OBDal.getInstance()
            .createQuery(EscmProposalAttribute.class, " as e where e.id = :propAttrId  ");
        existingPropAttr.setNamedParameter("propAttrId",
            csvImportLne.getEscmProposalAttr().getId());
        List<EscmProposalAttribute> linesList = existingPropAttr.list();
        if (linesList.size() > 0) {
          EscmProposalAttribute propAttr = linesList.get(0);
          propAttr.setGrossPrice(csvImportLne.getGrossPrice());
          if (csvImportLne.getComments() != null) {
            propAttr.setComments(csvImportLne.getComments());
          }
          propAttr.setDiscount(csvImportLne.getDiscount());
          propAttr.setDiscountAmount(csvImportLne.getDiscountAmount());
          propAttr.setNetPrice(csvImportLne.getNetPrice());
          propAttr.setRepresentativeName(csvImportLne.getRepresentativeName());
          propAttr.setComments(csvImportLne.getComments());
          OBDal.getInstance().save(propAttr);
        }
        // Get New Bank Guarantee Entries for Exisiting proposals from temprorary table
        OBQuery<ESCM_Csv_Import_Oee_Bgdet> csvBgDetNew = OBDal.getInstance().createQuery(
            ESCM_Csv_Import_Oee_Bgdet.class,
            " as e where e.openEnvelopID = :OeeId  and e.lineNo=:lineNo and e.isnewline='Y' and e.proposalno=:propNo");
        csvBgDetNew.setNamedParameter("OeeId", openEnvelopeEventId);
        csvBgDetNew.setNamedParameter("lineNo", csvImportLne.getLineNo());
        csvBgDetNew.setNamedParameter("propNo", csvImportLne.getProposalno());
        User user = null;
        ESCMBGDocumentnoV docNo = null;
        ESCMBGWorkbench newBgWorkBench = OBProvider.getInstance().get(ESCMBGWorkbench.class);
        Escmbankguaranteedetail BgWorkBenchLines = OBProvider.getInstance()
            .get(Escmbankguaranteedetail.class);
        List<ESCM_Csv_Import_Oee_Bgdet> csvBgDetNewList = csvBgDetNew.list();
        for (ESCM_Csv_Import_Oee_Bgdet newBgDet : csvBgDetNewList) {
          user = OBDal.getInstance().get(User.class, vars.getUser());
          String propNo = newBgDet.getProposalno();

          if (propNo != null) {
            OBQuery<ESCMBGDocumentnoV> docNoQry = OBDal.getInstance()
                .createQuery(ESCMBGDocumentnoV.class, "as e where e.documentNo=:propNo");
            docNoQry.setNamedParameter("propNo", propNo);
            if (docNoQry.list().size() > 0)
              docNo = docNoQry.list().get(0);

            OBQuery<EscmProposalAttribute> propAttrQry = OBDal.getInstance().createQuery(
                EscmProposalAttribute.class,
                " as e where e.escmProposalmgmt.id in (select e.id from Escm_Proposal_Management e where e.proposalno =:proposalno)");
            propAttrQry.setNamedParameter("proposalno", propNo);
            List<EscmProposalAttribute> propAttrList = propAttrQry.list();
            // if (propAttrList.size() > 0) {

            EscmProposalAttribute propAttribute = propAttrList.get(0);
            newBgWorkBench.setOrganization(propAttribute.getOrganization());
            newBgWorkBench.setClient(propAttribute.getClient());
            newBgWorkBench.setEscmProposalAttr(propAttribute);
            newBgWorkBench.setEscmProposalmgmt(propAttribute.getEscmProposalmgmt());
            String sequence = Utility.getTransactionSequencewithclient("0",
                propAttribute.getClient().getId(), "BGD");
            newBgWorkBench.setInternalNo(Long.parseLong(sequence));
            newBgWorkBench.setDocumentType("P");
            newBgWorkBench.setBidName(propAttribute.getEscmProposalmgmt().getBidName());
            newBgWorkBench.setBidNo(propAttribute.getEscmProposalmgmt().getEscmBidmgmt());
            /*
             * newBgWorkBench.setContactName(OBDal.getInstance().get(
             * org.openbravo.model.common.businesspartner.BusinessPartner.class,
             * user.getBusinessPartner().getId()));
             */
            OBQuery<Preference> bgSpecialist = OBDal.getInstance().createQuery(Preference.class,
                "as e where e.property='ESCM_BGSpecialist_Role' and e.searchKey='Y' "
                    + " and e.client.id=:clientID and active='Y'");
            bgSpecialist.setNamedParameter("clientID", propAttribute.getClient().getId());
            List<Preference> preference = bgSpecialist.list();
            if (preference != null && preference.size() > 0) {
              Preference contactName = preference.get(0);
              User user1 = contactName.getUserContact();
              Role role = contactName.getVisibleAtRole();
              if (user1 != null && user1.getBusinessPartner() != null) {
                newBgWorkBench.setContactName(user.getBusinessPartner());
              } else if (role != null) {
                List<UserRoles> userRoles = role.getADUserRolesList();
                if (userRoles != null && userRoles.size() > 0) {
                  for (UserRoles usr : userRoles) {
                    if (usr.getUserContact() != null) {
                      if (usr.getUserContact().getBusinessPartner() != null)
                        newBgWorkBench.setContactName(usr.getUserContact().getBusinessPartner());
                      break;
                    }
                  }
                }
              }
            }
            newBgWorkBench
                .setDocumentNo(OBDal.getInstance().get(ESCMBGDocumentnoV.class, docNo.getId()));
            newBgWorkBench.setBGArchiveRef(newBgDet.getBgarchieveref());
            newBgWorkBench.setType("IBG");
            OBDal.getInstance().save(newBgWorkBench);
            OBDal.getInstance().flush();

            if (newBgWorkBench != null) {
              BgWorkBenchLines.setEscmBgworkbench(newBgWorkBench);
              BgWorkBenchLines.setEscmProposalAttr(propAttribute);
              BgWorkBenchLines.setEscmProposalmgmt(propAttribute.getEscmProposalmgmt());
              BgWorkBenchLines.setLineNo(newBgDet.getLineNo());
              BgWorkBenchLines.setOrganization(newBgWorkBench.getOrganization());
              BgWorkBenchLines.setClient(newBgWorkBench.getClient());
              BgWorkBenchLines.setBankbgno(newBgDet.getLetterrefno());
              BgWorkBenchLines.setArchiveRef(newBgDet.getBgarchieveref());
              BgWorkBenchLines.setBankguaranteetype("IBG");
              if (newBgDet.getBanknumber() != null)
                bankObj = getBankId(newBgDet.getBanknumber(), newBgDet.getBankName());
              if (bankObj != null)
                BgWorkBenchLines.setBankName(bankObj);
              BgWorkBenchLines.setBankAddress(newBgDet.getBankAddress());
              BgWorkBenchLines.setInternalno(newBgWorkBench.getInternalNo());
              BgWorkBenchLines.setBgamount(newBgDet.getBGAmountSAR());
              BgWorkBenchLines.setForeignBank(newBgDet.isForeignBank());
              if (newBgDet.isForeignBank())
                BgWorkBenchLines.setForeignBankName(newBgDet.getForeignBankName());
              BgWorkBenchLines.setBgstartdateh(
                  UtilityDAO.convertToGregorianDate(newBgDet.getStartDateh().trim()));
              BgWorkBenchLines.setExpirydateh(
                  UtilityDAO.convertToGregorianDate(newBgDet.getExpirtDateh().trim()));
              BgWorkBenchLines.setExtendExpdateh(newBgDet.getExtendExpdateh());
              BgWorkBenchLines.setBgstartdategre(new SimpleDateFormat("dd-MM-yyyy")
                  .format(UtilityDAO.convertToGregorianDate(newBgDet.getStartDateh().trim())));
              BgWorkBenchLines.setExpirydategre(new SimpleDateFormat("dd-MM-yyyy")
                  .format(UtilityDAO.convertToGregorianDate(newBgDet.getExpirtDateh().trim())));
              yearId = HijiridateDAO.getYearId(today, newBgDet.getClient().getId());
              BgWorkBenchLines.setFinancialYear(OBDal.getInstance()
                  .get(org.openbravo.model.financialmgmt.calendar.Year.class, yearId));
              BgWorkBenchLines.setBgworkbench(true);
              BgWorkBenchLines.setEscmGuaGletterBookNo(newBgDet.getEscmGuaGletterBookNo());
              OBDal.getInstance().save(BgWorkBenchLines);
              OBDal.getInstance().flush();
            }

            // }
          }
        }
      }

      // Get newly created proposal entries from Temporary table
      OBQuery<Escm_Csv_Import_Oee_Prop> csvImportQryNewLines = OBDal.getInstance().createQuery(
          Escm_Csv_Import_Oee_Prop.class,
          " as e where e.openEnvelopID = :OeeId and e.isnewline='Y' order by e.creationDate ");

      csvImportQryNewLines.setNamedParameter("OeeId", openEnvelopeEventId);
      List<Escm_Csv_Import_Oee_Prop> csvImportQryNewLinesList = csvImportQryNewLines.list();

      for (Escm_Csv_Import_Oee_Prop csvImportNewLne : csvImportQryNewLinesList) {
        // Proposal
        if (csvImportNewLne.getSupplier() != null) {
          bPartnerId = getBusinessPartnerId(csvImportNewLne.getSupplier(), vars);
          String secondSupplierId = getBusinessPartnerId(csvImportNewLne.getSecondsupplier(), vars);
          // check if supplier is valid
          if (bPartnerId != null) {
            isSupplierExists = checkDuplicate(bPartnerId, openEnvelopeEventId, vars);
          }
          // if supplier exists in Supplier Tab(Limited Bid) /RFP(Tender)
          // if (isValidSupplier) {
          if (!isSupplierExists) {

            OBQuery<Escmbidsuppliers> supQry = OBDal.getInstance().createQuery(
                Escmbidsuppliers.class,
                " as e where suppliernumber.id in (select e.id from BusinessPartner e where "
                    + "     e.efinDocumentno=:documentNo and client.id=:clientId) ");
            supQry.setNamedParameter("documentNo", csvImportNewLne.getSupplier());
            supQry.setNamedParameter("clientId", csvImportNewLne.getClient());
            if (supQry.list().size() > 0) {
              supplierDet = supQry.list().get(0);
            }
            propMgmt = OBProvider.getInstance().get(EscmProposalMgmt.class);
            EscmBidMgmt bidObj = openEnvelopeEventId.getBidNo();

            // for (EscmBidMgmt bidObj : bidList) {

            // EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class, bidObj.getId());
            propMgmt.setClient(openEnvelopeEventId.getClient());
            propMgmt.setOrganization(bidObj.getOrganization());
            propMgmt.setProposalType(bidObj.getBidtype());
            propMgmt.setBuyername(OBDal.getInstance().get(User.class, vars.getUser()));
            propMgmt.setBranchName(supplierDet.getBranchname());
            propMgmt.setProposalstatus("SUB");
            propMgmt.setProposalno(
                UtilityDAO.getTransactionSequence(bidObj.getOrganization().getId(), "PMG"));
            propMgmt.setBidName(bidObj.getBidname());
            propMgmt.setBidType(bidObj.getBidtype());
            if (csvImportNewLne.getCurrency() != null) {
              currencyId = getCurrency(csvImportNewLne.getCurrency());
              if (currencyId != null)
                propMgmt.setCurrency(OBDal.getInstance().get(Currency.class, currencyId));
            }
            yearId = HijiridateDAO.getYearId(today, bidObj.getClient().getId());
            propMgmt.setFinancialYear(OBDal.getInstance()
                .get(org.openbravo.model.financialmgmt.calendar.Year.class, yearId));
            propMgmt.setApprovedBudgetSAR(bidObj.getApprovedbudget());

            budgInitialId = BudgetAdjustmentCallout.getBudgetDefinitionForStartDate(today,
                bidObj.getClient().getId(), strProposalWindowId);
            propMgmt.setEfinBudgetinitial(
                OBDal.getInstance().get(EfinBudgetIntialization.class, budgInitialId));
            propMgmt.setEscmBidmgmt(bidObj);
            propMgmt.setSubmissiondate(currentDate);
            propMgmt.setSubmissiontime(new SimpleDateFormat("HH:MM").format(currentDate.getTime()));
            propMgmt.setSupplier(OBDal.getInstance().get(BusinessPartner.class, bPartnerId));
            if (secondSupplierId != null) {
              propMgmt.setSecondsupplier(
                  OBDal.getInstance().get(BusinessPartner.class, secondSupplierId));
              propMgmt.setSubcontractors(propMgmt.getSecondsupplier().getName());
              OBQuery<Location> bpLocation = OBDal.getInstance().createQuery(Location.class,
                  "as e where e.businessPartner.id =:supplier order by created desc");
              bpLocation.setNamedParameter("supplier", secondSupplierId);
              bpLocation.setMaxResult(1);
              if (bpLocation != null && bpLocation.list().size() > 0) {
                propMgmt.setSecondBranchname(bpLocation.list().get(0));
              }
            }
            propMgmt.setEscmDocaction("RE");
            propMgmt.setRole(OBContext.getOBContext().getRole());
            propMgmt.setContractType(bidObj.getContractType());
            OBDal.getInstance().save(propMgmt);
            OBDal.getInstance().flush();

          }
          if (isSupplierExists) {
            EscmBidMgmt bid = openEnvelopeEventId.getBidNo();
            OBQuery<EscmProposalMgmt> existPropMgmt = OBDal.getInstance().createQuery(
                EscmProposalMgmt.class,
                "as e where escmBidmgmt.id=:bidId and e.supplier.id=:suppId");
            existPropMgmt.setNamedParameter("bidId", bid.getId());
            existPropMgmt.setNamedParameter("suppId", bPartnerId);
            List<EscmProposalMgmt> propList = existPropMgmt.list();
            if (propList.size() > 0) {
              propMgmt = propList.get(0);
            }
            OBQuery<EscmProposalAttribute> propAttr = OBDal.getInstance().createQuery(
                EscmProposalAttribute.class, "as e where escmProposalmgmt.id=:propmgmtid");
            propAttr.setNamedParameter("propmgmtid", propMgmt.getId());
            List<EscmProposalAttribute> propAttributeList = propAttr.list();
            if (propAttributeList.size() > 0) {
              isProposalAttrExists = true;
            }
            // isProposalAttrExists = propAttributeList.isEmpty();
          }
          if (propMgmt.getId() != null && !isProposalAttrExists) {
            EscmProposalAttribute newPropAttr = OBProvider.getInstance()
                .get(EscmProposalAttribute.class);

            newPropAttr.setEscmProposalmgmt(propMgmt);
            newPropAttr.setLineNo(csvImportNewLne.getLineNo());
            newPropAttr.setClient(propMgmt.getClient());
            newPropAttr.setOrganization(propMgmt.getOrganization());
            newPropAttr.setEscmOpenenvcommitee(openEnvelopeEventId);
            newPropAttr.setSupplier(OBDal.getInstance().get(BusinessPartner.class, bPartnerId));
            newPropAttr.setDiscount(csvImportNewLne.getDiscount());
            newPropAttr.setDiscountAmount(csvImportNewLne.getDiscountAmount());
            newPropAttr.setGrossPrice(csvImportNewLne.getGrossPrice());
            newPropAttr.setBranchName(propMgmt.getBranchName());
            newPropAttr.setNetPrice(csvImportNewLne.getNetPrice());
            newPropAttr.setRepresentativeName(csvImportNewLne.getRepresentativeName());
            newPropAttr.setComments(csvImportNewLne.getComments());
            if (csvImportNewLne.getCurrency() != null && csvImportNewLne.getCurrency() != "") {
              currencyId = getCurrencyId(csvImportNewLne.getCurrency());
              if (currencyId != null)
                newPropAttr.setCurrency(OBDal.getInstance().get(Currency.class, currencyId));
            } else {
              newPropAttr.setCurrency(null);
            }
            OBDal.getInstance().save(newPropAttr);
            OBDal.getInstance().flush();

            // Get New Bank Guarantee Entries for newly created proposal from temporary table
            OBQuery<ESCM_Csv_Import_Oee_Bgdet> csvBgDetNew = OBDal.getInstance().createQuery(
                ESCM_Csv_Import_Oee_Bgdet.class,
                " as e where  e.isnewline='Y' and e.lineNo=:lineNo");
            csvBgDetNew.setNamedParameter("lineNo", newPropAttr.getLineNo());
            User user = null;
            ESCMBGDocumentnoV docNo = null;
            ESCMBGWorkbench newBgWorkBench = OBProvider.getInstance().get(ESCMBGWorkbench.class);
            Escmbankguaranteedetail BgWorkBenchLines = OBProvider.getInstance()
                .get(Escmbankguaranteedetail.class);
            List<ESCM_Csv_Import_Oee_Bgdet> csvBgDetNewList = csvBgDetNew.list();
            for (ESCM_Csv_Import_Oee_Bgdet newBgDet : csvBgDetNewList) {
              user = OBDal.getInstance().get(User.class, vars.getUser());
              String propNo = newPropAttr.getEscmProposalmgmt().getProposalno();
              if (propNo != null) {
                OBQuery<ESCMBGDocumentnoV> docNoQry = OBDal.getInstance()
                    .createQuery(ESCMBGDocumentnoV.class, "as e where e.documentNo=:propNo");
                docNoQry.setNamedParameter("propNo", propNo);
                if (docNoQry.list().size() > 0)
                  docNo = docNoQry.list().get(0);

                OBQuery<EscmProposalAttribute> propAttrQry = OBDal.getInstance().createQuery(
                    EscmProposalAttribute.class,
                    " as e where e.escmProposalmgmt.id in (select e.id from Escm_Proposal_Management e where e.proposalno =:proposalno)");
                propAttrQry.setNamedParameter("proposalno", propNo);
                List<EscmProposalAttribute> propAttrList = propAttrQry.list();
                // if (propAttrList.size() > 0) {

                EscmProposalAttribute propAttribute = propAttrList.get(0);
                newBgWorkBench.setOrganization(propAttribute.getOrganization());
                newBgWorkBench.setClient(propAttribute.getClient());
                newBgWorkBench.setEscmProposalAttr(propAttribute);
                newBgWorkBench.setEscmProposalmgmt(propAttribute.getEscmProposalmgmt());
                String sequence = Utility.getTransactionSequencewithclient("0",
                    propAttribute.getClient().getId(), "BGD");
                newBgWorkBench.setInternalNo(Long.parseLong(sequence));
                newBgWorkBench.setDocumentType("P");
                newBgWorkBench.setBidName(propAttribute.getEscmProposalmgmt().getBidName());
                newBgWorkBench.setBidNo(propAttribute.getEscmProposalmgmt().getEscmBidmgmt());
                newBgWorkBench.setContactName(OBDal.getInstance().get(
                    org.openbravo.model.common.businesspartner.BusinessPartner.class,
                    user.getBusinessPartner().getId()));
                newBgWorkBench
                    .setDocumentNo(OBDal.getInstance().get(ESCMBGDocumentnoV.class, docNo.getId()));
                newBgWorkBench.setBGArchiveRef(newBgDet.getBgarchieveref());
                newBgWorkBench.setType("IBG");
                OBDal.getInstance().save(newBgWorkBench);
                OBDal.getInstance().flush();

                if (newBgWorkBench != null) {
                  BgWorkBenchLines.setEscmBgworkbench(newBgWorkBench);
                  BgWorkBenchLines.setEscmProposalAttr(propAttribute);
                  BgWorkBenchLines.setEscmProposalmgmt(propAttribute.getEscmProposalmgmt());
                  BgWorkBenchLines.setLineNo(newBgDet.getLineNo());
                  BgWorkBenchLines.setOrganization(newBgWorkBench.getOrganization());
                  BgWorkBenchLines.setClient(newBgWorkBench.getClient());
                  BgWorkBenchLines.setBankbgno(newBgDet.getLetterrefno());
                  BgWorkBenchLines.setArchiveRef(newBgDet.getBgarchieveref());
                  BgWorkBenchLines.setBankguaranteetype("IBG");

                  if (newBgDet.getBanknumber() != null)
                    bankObj = getBankId(newBgDet.getBanknumber(), newBgDet.getBankName());
                  if (bankObj != null)
                    BgWorkBenchLines.setBankName(bankObj);
                  BgWorkBenchLines.setBankAddress(newBgDet.getBankAddress());
                  BgWorkBenchLines.setInternalno(newBgWorkBench.getInternalNo());
                  BgWorkBenchLines.setBgamount(newBgDet.getBGAmountSAR());
                  BgWorkBenchLines.setForeignBank(newBgDet.isForeignBank());
                  if (newBgDet.isForeignBank())
                    BgWorkBenchLines.setForeignBankName(newBgDet.getForeignBankName());
                  BgWorkBenchLines.setBgstartdateh(
                      UtilityDAO.convertToGregorianDate(newBgDet.getStartDateh().trim()));
                  BgWorkBenchLines.setExpirydateh(
                      UtilityDAO.convertToGregorianDate(newBgDet.getExpirtDateh().trim()));
                  BgWorkBenchLines.setExtendExpdateh(newBgDet.getExtendExpdateh());
                  BgWorkBenchLines.setBgstartdategre(new SimpleDateFormat("dd-MM-yyyy")
                      .format(UtilityDAO.convertToGregorianDate(newBgDet.getStartDateh().trim())));
                  BgWorkBenchLines.setExpirydategre(new SimpleDateFormat("dd-MM-yyyy")
                      .format(UtilityDAO.convertToGregorianDate(newBgDet.getExpirtDateh().trim())));
                  yearId = HijiridateDAO.getYearId(today, newBgWorkBench.getClient().getId());
                  BgWorkBenchLines.setFinancialYear(OBDal.getInstance()
                      .get(org.openbravo.model.financialmgmt.calendar.Year.class, yearId));
                  BgWorkBenchLines.setBgworkbench(true);
                  BgWorkBenchLines.setEscmGuaGletterBookNo(newBgDet.getEscmGuaGletterBookNo());
                  OBDal.getInstance().save(BgWorkBenchLines);
                  OBDal.getInstance().flush();
                }
              }
            }

          }

          // }
        }

      }
      // Get Old Bank Guarantee Entries from temprorary table
      OBQuery<ESCM_Csv_Import_Oee_Bgdet> csvBgDetExisting = OBDal.getInstance().createQuery(
          ESCM_Csv_Import_Oee_Bgdet.class, " as e where  e.escmProposalAttr.id in (select e.id "
              + "from escm_proposal_attr e where e.escmOpenenvcommitee.id=:OeeId ) and isnewline='N'");
      csvBgDetExisting.setNamedParameter("OeeId", openEnvelopeEventId);
      List<ESCM_Csv_Import_Oee_Bgdet> csvBgDetExistingList = csvBgDetExisting.list();

      for (ESCM_Csv_Import_Oee_Bgdet csvImportBgLne : csvBgDetExistingList) {
        OBQuery<Escmbankguaranteedetail> existingBg = OBDal.getInstance().createQuery(
            Escmbankguaranteedetail.class, " as e where e.escmProposalAttr.id = :propId  ");
        existingBg.setNamedParameter("propId", csvImportBgLne.getEscmProposalAttr().getId());
        List<Escmbankguaranteedetail> bgList = existingBg.list();
        if (bgList.size() > 0) {
          Escmbankguaranteedetail bgDetail = bgList.get(0);
          if (csvImportBgLne.getBanknumber() != null)
            bankObj = getBankId(csvImportBgLne.getBanknumber(), csvImportBgLne.getBankName());
          if (bankObj != null)
            bgDetail.setBankName(bankObj);
          bgDetail.setBankAddress(csvImportBgLne.getBankAddress());
          // bgDetail.setBgstartdateh(csvImportBgLne.getStartDateh());
          bgDetail.setBgamount(csvImportBgLne.getBGAmountSAR());
          bgDetail.setEscmGuaGletterBookNo(csvImportBgLne.getLetterrefno());
          bgDetail.setForeignBank(csvImportBgLne.isForeignBank());
          if (csvImportBgLne.isForeignBank())
            bgDetail.setForeignBankName(csvImportBgLne.getForeignBankName());
          // bgDetail.setExpirydateh(csvImportBgLne.getExpirtDateh());
          bgDetail.setNotes(csvImportBgLne.getNotes());
          // bgDetail.setExtendExpdateh(csvImportBgLne.getExtendExpdateh());
          OBDal.getInstance().save(bgDetail);
        }
      }
    } catch (

    Exception e) {
      log4j.error("Error in OpenEnvelopeEventImportDAO.java : addOrUpdateProposalAttribute() ", e);
      return 0;
    }
    return 1;
  }

  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars, String inpOeeId,
      boolean isUpload) {
    JSONObject resultJSON = new JSONObject();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    Sheet proposalSheet = null;
    Sheet bgDetails = null;
    XSSFWorkbook xssfWorkbook = null;
    List<String> proposalSeqNo = new ArrayList<String>();
    List<String> bankGuranteeSeqNo = new ArrayList<String>();
    String openEnvelopeEventNo = null, fileName = null, fileNameTemp = null;
    String chkProposalLastDate = "";

    try {
      OBContext.setAdminMode(true);

      boolean validFile = true;
      ArrayList<String> lineNoList = new ArrayList<String>();

      StringBuffer resultMessage = new StringBuffer();
      StringBuffer numericErrorDetails = new StringBuffer();
      StringBuffer negativeErrorDetails = new StringBuffer();
      StringBuffer startDateDetails = new StringBuffer();
      StringBuffer supplierNotExistsDetails = new StringBuffer();
      StringBuffer secondSupplierNotExistsDetails = new StringBuffer();
      StringBuffer currencyDetails = new StringBuffer();
      StringBuffer expiryDateDetails = new StringBuffer();
      StringBuffer existinPropDetails = new StringBuffer();
      StringBuffer lineNumberExistsDetails = new StringBuffer();
      StringBuffer discountAmtBlank = new StringBuffer();
      StringBuffer grossPriceBlank = new StringBuffer();
      StringBuffer currencyBlank = new StringBuffer();
      StringBuffer bankNameBlank = new StringBuffer();
      StringBuffer bgAmtBlank = new StringBuffer();
      StringBuffer letterRefBlank = new StringBuffer();
      StringBuffer foreignBankBlank = new StringBuffer();
      StringBuffer expiryDateBlank = new StringBuffer();
      StringBuffer supplierBlank = new StringBuffer();
      StringBuffer bankInvalid = new StringBuffer();

      boolean noMandatoryFields = false;
      boolean hasErrorMessage = false;
      boolean hasValidLine = false;
      boolean lineNumberExists = false;
      boolean hasError = false;
      boolean hasBgSplRole = true, bgExpiryDateError = false, isPropLastDateOver = false,
          isCurrencyBlank = false, isGrossPriceBlank = false, isSupplierBlank = false,
          isBankNameEmpty = false, isBgAmountBlank = false, isLetterRefBlank = false,
          isForeignBankBlank = false, isStartDateBlank = false, isExpiryDateBlank = false,
          isLineNoBlank = false;

      Escmopenenvcommitee oee = OBDal.getInstance().get(Escmopenenvcommitee.class, inpOeeId);

      // Get Work Book
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
      DataFormatter formatter = new DataFormatter();
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        if (validFile) {

          fileName = file.getName().split("_")[1]
              .replaceAll("^.*?(([^/\\\\\\.]+))\\.[^\\.]+$", "$1").trim();
          String fileName1 = FilenameUtils.removeExtension(file.getName()).split("_")[0].trim();

          // Task No.:8230 Due to arabic translation, docno might be at 0 or 1 index
          fileNameTemp = file.getName().split("_")[0]
              .replaceAll("^.*?(([^/\\\\\\.]+))\\.[^\\.]+$", "$1").trim();
          String fileName1Temp = FilenameUtils.removeExtension(fileNameTemp).split("\\(")[0].trim();

          if (oee != null)
            openEnvelopeEventNo = oee.getEventno();
          if (fileName1.compareTo(openEnvelopeEventNo) != 0) {
            if (fileName1Temp.compareTo(openEnvelopeEventNo) != 0) {
              validFile = false;
            }
          }
        }
      }

      if (xssfWorkbook.getNumberOfSheets() > 0) {
        // for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
        if (validFile) {
          // Get Each Sheet
          sheet = xssfWorkbook.getSheetAt(0);
          if (sheet == null)
            validFile = false;
          proposalSheet = xssfWorkbook.getSheetAt(0);
          bgDetails = xssfWorkbook.getSheetAt(1);
          if (proposalSheet != null) {
            // Initialize Data to be inserted in temporary table
            BigDecimal grossPrice = BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal discountAmt = BigDecimal.ZERO;
            BigDecimal netUnitPrice = BigDecimal.ZERO;
            EscmProposalAttribute propAttr = null;

            int startRow = 1;
            Row row = null;
            Cell cell = null, cell1 = null;
            String proposalAttrId = null, proposalNo = null, repName = null, comments = null;

            String supplier = null;
            String secondsupplier = null;
            String currency = null;
            String lineNo = "";
            String cellVal = "";

            // Get Each Row
            for (int i = startRow;; i++) {

              row = proposalSheet.getRow(i);

              if (row == null)
                break;

              if (StringUtils.isEmpty(row.getCell(1).toString())
                  && StringUtils.isEmpty(row.getCell(2).toString())
                  && StringUtils.isEmpty(row.getCell(3).toString())
                  && StringUtils.isEmpty(row.getCell(4).toString())
                  && StringUtils.isEmpty(row.getCell(5).toString())
                  && StringUtils.isEmpty(row.getCell(6).toString()))
                break;

              // ProposalAttr Id
              cell = row.getCell(0);
              if (cell != null) {
                proposalAttrId = Utility.nullToEmpty(getCellValue(cell));
                propAttr = OBDal.getInstance().get(EscmProposalAttribute.class, proposalAttrId);
              } else
                proposalAttrId = null;
              // Line No
              cell = row.getCell(1);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (lineNoList.contains(getCellValue(cell))) {
                  lineNumberExists = true;
                  lineNumberExistsDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " -> " + cellVal);
                  lineNumberExistsDetails.append("<br>");
                  // validFile = false;
                } else {
                  lineNoList.add(getCellValue(cell));
                }
              } else {
                isLineNoBlank = true;
                hasError = true;
              }
              // Seq No

              cell = row.getCell(1);
              if (cell != null) {
                lineNo = Utility.nullToEmpty(getCellValue(cell));
              }

              // Proposal No
              cell = row.getCell(2);
              if (cell != null) {
                proposalNo = Utility.nullToEmpty(getCellValue(cell));
              } else
                proposalNo = null;
              if ((proposalAttrId == null || proposalAttrId == "")
                  && (proposalNo == null || proposalAttrId == "")) {
                proposalSeqNo.add(lineNo);
              }

              // Supplier
              cell = row.getCell(3);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "" && !checkIsValidSupplier(cellVal, oee, vars)) {
                  supplierNotExistsDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  supplierNotExistsDetails.append("<br>");
                  hasError = true;
                } else if (cellVal != null && cellVal != ""
                    && checkExisitingProposalStatus(cellVal, oee, vars)) {
                  existinPropDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  existinPropDetails.append("<br>");
                  hasError = true;
                } else {
                  supplier = Utility.nullToEmpty(getCellValue(cell));
                }
              } else {
                isSupplierBlank = true;
                supplierBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo);
                supplierBlank.append("<br>");
                hasError = true;
              }
              // joint venture supplier
              cell = row.getCell(5);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "" && !checkIsValidSecondSupplier(cellVal)) {
                  secondSupplierNotExistsDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  secondSupplierNotExistsDetails.append("<br>");
                  hasError = true;
                } else {
                  secondsupplier = Utility.nullToEmpty(getCellValue(cell));
                }
              }

              // Gross Price
              cell = row.getCell(6);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (!isNumeric(cellVal)) {
                  numericErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " - " + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                          + cellVal);
                  numericErrorDetails.append("<br>");
                  hasError = true;
                } else if (!chkisnegative(getCellValue(cell))) {
                  negativeErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " - " + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                          + cellVal);
                  negativeErrorDetails.append("<br>");
                  hasError = true;
                } else {
                  grossPrice = new BigDecimal(cellVal);
                }
              } else {
                isGrossPriceBlank = true;
                grossPriceBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo);
                grossPriceBlank.append("<br>");
                hasError = true;
              }
              // currency
              cell = row.getCell(7);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != ""
                    && (getCurrency(cellVal) == null || getCurrency(cellVal).equals(null))) {
                  currencyDetails.append(OBMessageUtils.messageBD("ESCM_Invalid_currency") + lineNo
                      + " ->  " + cellVal);
                  currencyDetails.append("<br>");
                  hasError = true;
                } /*
                   * else if (cellVal == "") { noMandatoryFields = true; break; }
                   */ else {
                  currency = Utility.nullToEmpty(getCellValue(cell));
                }
              } else {
                currencyBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo);
                currencyBlank.append("<br>");
                hasError = true;
              }
              // Discount Percentage
              cell = row.getCell(8);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (!isNumeric(cellVal)) {
                  numericErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " - " + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                          + cellVal);
                  numericErrorDetails.append("<br>");
                  hasError = true;
                } else if (!chkisnegative(getCellValue(cell))) {
                  negativeErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " - " + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                          + cellVal);
                  negativeErrorDetails.append("<br>");
                  hasError = true;
                } else {
                  cellVal = formatter.formatCellValue((cell));
                  discount = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                }
              } else {
                discountAmtBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo);
                discountAmtBlank.append("<br>");
                hasError = true;
              }

              // Discount Amt
              cell = row.getCell(9);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                discountAmt = new BigDecimal(cell.getNumericCellValue());
              }
              // Net Unit Price
              cell = row.getCell(10);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                netUnitPrice = new BigDecimal(cell.getNumericCellValue());
              }
              // Representative Name
              cell = row.getCell(11);
              if (cell != null) {
                repName = Utility.nullToEmpty(getCellValue(cell));
              }
              // commemnts
              cell = row.getCell(12);
              if (cell != null) {
                comments = Utility.nullToEmpty(getCellValue(cell));
              }
              chkProposalLastDate = getmaxbidproposallastdayandbidnumber(oee);
              if (chkProposalLastDate != "Success") {
                isPropLastDateOver = true;
                hasError = true;

              }

              // If no error, insert into temporary table
              if (!hasError) {
                Escm_Csv_Import_Oee_Prop csvImport = OBProvider.getInstance()
                    .get(Escm_Csv_Import_Oee_Prop.class);
                csvImport.setOpenEnvelopID(oee);
                csvImport.setGrossPrice(grossPrice);
                csvImport.setDiscount(discount);
                csvImport.setDiscountAmount(discountAmt);
                csvImport.setEscmProposalAttr(propAttr);
                csvImport.setNetPrice(netUnitPrice);
                csvImport.setSupplier(supplier);
                csvImport.setSecondsupplier(secondsupplier);
                csvImport.setCurrency(currency);
                csvImport.setProposalno(proposalNo);
                csvImport.setLineNo(Long.valueOf(lineNo));
                if (proposalAttrId == null || proposalAttrId == "")
                  csvImport.setNewline(true);
                else if (proposalAttrId != null)
                  csvImport.setNewline(false);
                csvImport.setRepresentativeName(repName);
                csvImport.setComments(comments);
                OBDal.getInstance().save(csvImport);
                OBDal.getInstance().flush();
                hasValidLine = true;
              }

            }
          }
          if (bgDetails != null) {
            int startRow = 1;
            Row row = null;
            Cell cell = null;
            String bgInternalNo = null, bankBranch = null, bankNumber = null, bankName = null,
                bankAddress = null, propAttrId = null, proposalNo = null, seqNo = null;
            String letterRefNo = null, bgArchieveRef = null;
            String foreignBank = null, foreignBankName = null, notes = null;
            String cellVal = "";
            String startDate = "", expireDateH = "", extendDateH = "";
            String archiveFolderNo = null;
            BigDecimal bgAmount = BigDecimal.ZERO;
            BigDecimal revisedBgAmt = BigDecimal.ZERO;
            // boolean hasError = false;
            EscmProposalAttribute propAttr = null;

            for (int i = startRow;; i++) {

              row = bgDetails.getRow(i);

              if (row == null)
                break;
              cell = row.getCell(1);
              if (StringUtils.isEmpty(cell.toString()))
                break;

              // BgDetail Id
              cell = row.getCell(0);
              if (cell != null) {
                propAttrId = Utility.nullToEmpty(getCellValue(cell));
                propAttr = OBDal.getInstance().get(EscmProposalAttribute.class, propAttrId);
              } else {
                propAttrId = null;
                propAttr = null;
              }
              // seqNo
              cell = row.getCell(1);
              if (cell != null) {
                seqNo = Utility.nullToEmpty(getCellValue(cell));
              }
              // proposalNo
              cell = row.getCell(2);
              if (cell != null) {
                proposalNo = Utility.nullToEmpty(getCellValue(cell));
              } else
                proposalNo = null;
              if ((propAttrId == null || propAttrId == "")
                  && (proposalNo == null || proposalNo == "")) {
                bankGuranteeSeqNo.add(seqNo);
              }

              // bgInternalNO
              cell = row.getCell(3);
              if (cell != null) {
                bgInternalNo = Utility.nullToEmpty(getCellValue(cell));
              }

              // bankNumber
              cell = row.getCell(4);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (!chkBankIsValid(cellVal)) {
                  bankInvalid
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + seqNo);
                  bankInvalid.append("<br>");
                  hasError = true;
                } else {
                  bankNumber = Utility.nullToEmpty(getCellValue(cell));
                }
              } else {
                isBankNameEmpty = true;
                bankNameBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + seqNo);
                bankNameBlank.append("<br>");
                hasError = true;
              }
              // bankName
              cell = row.getCell(5);
              if (cell != null) {
                bankName = Utility.nullToEmpty(getCellValue(cell));
              }

              // bankBranch
              cell = row.getCell(6);
              if (cell != null) {
                bankBranch = Utility.nullToEmpty(getCellValue(cell));
              }
              // bankAddress
              cell = row.getCell(7);
              if (cell != null) {
                bankAddress = Utility.nullToEmpty(getCellValue(cell));
              }

              // bgAmount
              cell = row.getCell(8);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (!isNumeric(cellVal)) {
                  numericErrorDetails.append(
                      OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + bgInternalNo
                          + " - " + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                          + cellVal);
                  numericErrorDetails.append("<br>");
                  hasError = true;
                } else if (!chkisnegative(getCellValue(cell))) {
                  negativeErrorDetails.append(
                      OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + bgInternalNo
                          + " - " + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                          + cellVal);
                  negativeErrorDetails.append("<br>");
                  hasError = true;
                } else {
                  if (cellVal != null && cellVal != "")
                    bgAmount = new BigDecimal(cellVal);
                  else
                    bgAmount = BigDecimal.ZERO;
                }
              } else {
                isBgAmountBlank = true;
                bgAmtBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + seqNo);
                bgAmtBlank.append("<br>");
              }
              // revisedBgAmt
              cell = row.getCell(9);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (!isNumeric(cellVal)) {
                  numericErrorDetails.append(
                      OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + bgInternalNo
                          + " - " + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                          + cellVal);
                  numericErrorDetails.append("<br>");
                  hasError = true;
                } else if (!chkisnegative(getCellValue(cell))) {
                  negativeErrorDetails.append(
                      OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + bgInternalNo
                          + " - " + OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_ReceivedQty")
                          + cellVal);
                  negativeErrorDetails.append("<br>");
                  hasError = true;
                } else {
                  if (cellVal != null && cellVal != "")
                    revisedBgAmt = new BigDecimal(cellVal);
                  else
                    revisedBgAmt = BigDecimal.ZERO;

                }
              }

              // Letter Ref No
              cell = row.getCell(10);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                letterRefNo = cellVal;
              } else {
                isLetterRefBlank = true;
                letterRefBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + seqNo);
                letterRefBlank.append("<br>");
                hasError = true;
              }
              // Foreign bank
              cell = row.getCell(11);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                foreignBank = Utility.nullToEmpty(getCellValue(cell));
              } else {
                isForeignBankBlank = true;
                foreignBankBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + seqNo);
                foreignBankBlank.append("<br>");
                hasError = true;
              }
              // Foreign bank name
              cell = row.getCell(12);
              if (cell != null) {
                foreignBankName = Utility.nullToEmpty(getCellValue(cell));
              }
              // Hijiri start date
              cell = row.getCell(13);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "" && !isValidDate(cellVal)) {
                  startDateDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                      + seqNo + " -> " + cellVal);
                  startDateDetails.append("<br>");
                  hasError = true;

                } else
                  startDate = cellVal;
              } else {
                isStartDateBlank = true;
                startDateDetails
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + seqNo);
                startDateDetails.append("<br>");
                hasError = true;
              }
              // ExpiryDate Hijiri
              cell = row.getCell(14);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (validBGDatewithOpenEnvel(oee, cellVal)) {
                  expiryDateDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + seqNo);
                  expiryDateDetails.append("<br>");
                  hasError = true;
                } else
                  expireDateH = cellVal;
              } else {
                isExpiryDateBlank = true;
                expiryDateBlank
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + seqNo);
                expiryDateBlank.append("<br>");
                hasError = true;
              }
              // notes
              cell = row.getCell(15);
              if (cell != null) {
                notes = Utility.nullToEmpty(getCellValue(cell));
              }
              // bg Archeive ref

              cell = row.getCell(16);
              if (cell != null) {
                bgArchieveRef = Utility.nullToEmpty(getCellValue(cell));
              }
              // Hijiri Extend Date
              cell = row.getCell(17);
              if (cell != null) {
                extendDateH = getCellValue(cell);
                // expireDateH = UtilityDAO.convertToGregorian_tochar(cellVal);
              }
              // Archive Folder No
              cell = row.getCell(18);
              if (cell != null) {
                archiveFolderNo = getCellValue(cell);
              }
              //
              boolean isSpecialityRolePresent = checkIfBgSpecialityRoleExists(oee);
              if (!isSpecialityRolePresent) {
                hasBgSplRole = false;
                hasError = true;
              }
              if (!startDate.equals("") && !expireDateH.equals("")) {
                boolean isBgExpiryDateLesser = checkBgExpDateGRT(startDate, expireDateH);
                if (isBgExpiryDateLesser) {
                  bgExpiryDateError = true;
                  hasError = true;
                }
              }

              // If no error, insert into temporary table
              if (!hasError) {
                ESCM_Csv_Import_Oee_Bgdet csvImportBg = OBProvider.getInstance()
                    .get(ESCM_Csv_Import_Oee_Bgdet.class);
                csvImportBg.setOpenEnvelopID(oee);
                csvImportBg.setEscmProposalAttr(propAttr);
                csvImportBg.setLineNo(Long.valueOf(seqNo));
                csvImportBg.setProposalno(proposalNo);
                if (bgInternalNo != null && bgInternalNo != "")
                  csvImportBg.setInternalNo(Long.parseLong(bgInternalNo));
                csvImportBg.setBanknumber(bankNumber);
                csvImportBg.setBankName(bankName);
                csvImportBg.setBankAddress(bankAddress);
                csvImportBg.setBGAmountSAR(bgAmount);
                csvImportBg.setRevisedbgamt(revisedBgAmt.longValue());
                csvImportBg.setLetterrefno(letterRefNo.toString());
                csvImportBg.setForeignBank(false);
                csvImportBg.setForeignBankName(foreignBankName);
                if (startDate != null && startDate != "")
                  csvImportBg.setStartDateh(startDate);
                if (extendDateH != null && extendDateH != "")
                  csvImportBg
                      .setExtendExpdateh(new SimpleDateFormat("dd-MM-yyyy").parse(extendDateH));
                if (expireDateH != null && expireDateH != "")
                  csvImportBg.setExpirtDateh(expireDateH);
                csvImportBg.setNotes(notes);
                if (propAttrId == null || propAttrId == "")
                  csvImportBg.setNewline(true);
                else if (propAttrId != null)
                  csvImportBg.setNewline(false);
                csvImportBg.setBgarchieveref(bgArchieveRef);
                csvImportBg.setEscmGuaGletterBookNo(archiveFolderNo);
                OBDal.getInstance().save(csvImportBg);
                OBDal.getInstance().flush();
                hasValidLine = true;
              }
            }
          }
          if (bankGuranteeSeqNo.size() > 0) {

            for (String bgSeq : bankGuranteeSeqNo) {

              if (proposalSeqNo.indexOf(bgSeq) == -1) {

                validFile = false;
              }
            }
          }
        }

        if (validFile) {
          // If Line No is Blank
          if (isLineNoBlank) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_LineNo_Mandatory"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }
          // Numeric Errors
          if (numericErrorDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_NonNumeric"));
            resultMessage.append("<br>");
            resultMessage.append(numericErrorDetails.toString());
            hasErrorMessage = true;
          }

          // Negative Errors
          if (negativeErrorDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_Negative"));
            resultMessage.append("<br>");
            resultMessage.append(negativeErrorDetails.toString());
            hasErrorMessage = true;
          }
          // Supplier error
          if (supplierNotExistsDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_Invalid_Supplier"));
            resultMessage.append("<br>");
            resultMessage.append(supplierNotExistsDetails.toString());
            hasErrorMessage = true;
          }
          // Second Supplier error
          if (secondSupplierNotExistsDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_Invalid_SecondSupplier"));
            resultMessage.append("<br>");
            resultMessage.append(secondSupplierNotExistsDetails.toString());
            hasErrorMessage = true;
          }

          if (existinPropDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_Proposal_status"));
            resultMessage.append("<br>");
            resultMessage.append(existinPropDetails.toString());
            hasErrorMessage = true;
          }
          if (discountAmtBlank.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_discountamt_blank"));
            resultMessage.append("<br>");
            resultMessage.append(discountAmtBlank.toString());
            hasErrorMessage = true;
          }
          if (currencyBlank.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ASSET_CURRENCY_MANDATORY"));
            resultMessage.append("<br>");
            resultMessage.append(currencyBlank.toString());
            hasErrorMessage = true;
          }

          // currency error
          if (currencyDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_Invalid_Currency"));
            resultMessage.append("<br>");
            resultMessage.append(currencyDetails.toString());
            hasErrorMessage = true;
          }
          // Expiry Date error
          if (expiryDateDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_BGExpDatCompWithOpenEnv"));
            resultMessage.append("<br>");
            resultMessage.append(expiryDateDetails.toString());
            hasErrorMessage = true;
          }
          // bank invalid
          if (bankInvalid.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_BankInvalid"));
            resultMessage.append("<br>");
            resultMessage.append(bankInvalid.toString());
            hasErrorMessage = true;
          }

          if (noMandatoryFields) {
            resultMessage.append(OBMessageUtils.messageBD("OBUIAPP_FillMandatoryFields"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }
          if (isGrossPriceBlank) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_Gross_Price_Blank"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }
          if (isSupplierBlank) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_SupplierID_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(supplierBlank.toString());
            hasErrorMessage = true;
          }
          if (isBankNameEmpty) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_Bank_Name_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(bankNameBlank.toString());
            hasErrorMessage = true;
          }
          if (isBgAmountBlank) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_Bg_Amount_Madatory"));
            resultMessage.append("<br>");
            resultMessage.append(bgAmtBlank.toString());
            hasErrorMessage = true;
          }
          if (isLetterRefBlank) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_BGLetter_Ref_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(letterRefBlank.toString());
            hasErrorMessage = true;
          }
          if (isForeignBankBlank) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_Foreign_Bank_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(foreignBankBlank.toString());
            hasErrorMessage = true;
          }
          if (isStartDateBlank) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_Bg_StartDate_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(startDateDetails.toString());
            hasErrorMessage = true;
          }
          if (isExpiryDateBlank) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_Expirydate_mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(expiryDateBlank.toString());
            hasErrorMessage = true;
          }
          if (lineNumberExistsDetails.toString().length() > 0) { // Line number already exists
            resultMessage.append(OBMessageUtils.messageBD("Efin_lineexist"));
            resultMessage.append("<br>");
            resultMessage.append(lineNumberExistsDetails.toString());
            hasErrorMessage = true;
          }
          if (!hasBgSplRole) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_NOBGSpecialistrole"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }
          if (bgExpiryDateError) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_BGStatdatGrtthanExpdate"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }
          if (isPropLastDateOver) {
            resultMessage.append(chkProposalLastDate);
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }
        } else {
          // Invalid Files
          resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_InvalidfFile"));
          hasErrorMessage = true;

        }

        if (hasErrorMessage) {
          OBDal.getInstance().rollbackAndClose();
          resultJSON.put("status", "0");
          resultJSON.put("statusMessage", resultMessage.toString());
          log4j.debug("Validation Failed");
        } else {
          resultJSON.put("status", "1");
          resultJSON.put("statusMessage", OBMessageUtils.messageBD("Escm_CSV_ValidatedSuccess"));
          log4j.debug("Validation Success");
        }
        // }
      }
    } catch (

    final Exception e) {
      log4j.error("Exception in OpenEnvelopeEventImportDAO : ", e);
      return null;
    } finally {
      if (!isUpload) {
        deleteCSVImportEntries();
        log4j.debug("Deleted CSV Import Entry");
      }
      OBContext.restorePreviousMode();
    }
    return resultJSON;
  }

  public void deleteCSVImportEntries() {
    try {
      OBQuery<Escm_Csv_Import_Oee_Prop> csvImportQry = OBDal.getInstance()
          .createQuery(Escm_Csv_Import_Oee_Prop.class, "order by creationDate");
      List<Escm_Csv_Import_Oee_Prop> csvImportList = csvImportQry.list();
      for (Escm_Csv_Import_Oee_Prop csvimprt : csvImportList) {
        OBDal.getInstance().remove(csvimprt);
      }
      OBQuery<ESCM_Csv_Import_Oee_Bgdet> csvBgImportQry = OBDal.getInstance()
          .createQuery(ESCM_Csv_Import_Oee_Bgdet.class, "order by creationDate");
      List<ESCM_Csv_Import_Oee_Bgdet> csvBgImportList = csvBgImportQry.list();
      for (ESCM_Csv_Import_Oee_Bgdet csvbgimprt : csvBgImportList) {
        OBDal.getInstance().remove(csvbgimprt);
      }
    } catch (final Exception e) {
      log4j.error("Exception in deleteCSVImportEntries() : ", e);
    }
  }

  private String getCellValue(Cell cell) {
    try {
      if (cell == null)
        return "";
      if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()
          || Cell.CELL_TYPE_FORMULA == cell.getCellType()) {
        if (DateUtil.isCellDateFormatted(cell)) {
          java.util.Date date = cell.getDateCellValue();
          return Utility.dateTimeFormat.format(date);
        } else {
          return cell.getNumericCellValue() == 0 ? "0"
              : BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString()
                  .toString().trim();
        }
      } else
        return Utility.nullToEmpty(cell.getRichStringCellValue().getString());
    } catch (final Exception e) {
      log4j.error("Exception in getCellValue", e);
      return "";
    }
  }

  public static boolean isNumeric(String strVal) {
    try {
      String str = strVal;
      if (str.equals("") && str.isEmpty()) {
        str = "0";
      }
      @SuppressWarnings("unused")
      double d = Double.parseDouble(str);
    } catch (NumberFormatException nfe) {
      return false;
    }
    return true;
  }

  public static boolean chkisnull(String cell) {
    try {
      if (cell == null || cell.equals("")) {
        return false;
      } else {
        return true;
      }

    } catch (Exception e) {
      log4j.error("Exception in chkisnull()", e);
    }
    return true;
  }

  public static boolean chkisnegative(String cellVal) {
    try {
      String cell = cellVal;
      if (cell.equals("") && cell.isEmpty()) {
        cell = "0";
      }
      if (new BigDecimal(cell).compareTo(BigDecimal.ZERO) < 0) {
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      log4j.error("Exception in chkisnegative()", e);
    }
    return true;
  }

  public static boolean chkBankIsValid(String bankNumber) {
    try {
      OBQuery<EfinBank> bankQry = OBDal.getInstance().createQuery(EfinBank.class,
          " as e where e.searchKey=:bankNumber ");
      bankQry.setNamedParameter("bankNumber", bankNumber);
      if (bankQry.list().size() > 0) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      log4j.error("Exception in chkBankIsValid()", e);
    }
    return true;
  }

  public static EfinBank getBankId(String bankNumber, String bankName) {
    List<EfinBank> bankList = new ArrayList<EfinBank>();
    List<EfinBank> bankNameList = new ArrayList<EfinBank>();
    String obQry = null;
    String orderByQry = null;
    try {
      obQry = " as e where e.searchKey=:bankNumber ";
      orderByQry = " order by created desc ";
      OBQuery<EfinBank> bankQry = OBDal.getInstance().createQuery(EfinBank.class,
          obQry + " " + orderByQry);
      bankQry.setNamedParameter("bankNumber", bankNumber);
      bankList = bankQry.list();
      if (bankList.size() == 1) {
        return bankList.get(0);
      } else if (bankList.size() > 1) {
        if (bankName != null) {
          obQry += " and trim(e.bankname)=trim(:bankName) ";
        }
        OBQuery<EfinBank> bankNameQry = OBDal.getInstance().createQuery(EfinBank.class, obQry);
        bankNameQry.setNamedParameter("bankNumber", bankNumber);
        if (bankName != null) {
          bankNameQry.setNamedParameter("bankName", bankName);
        }
        bankNameQry.setMaxResult(1);
        bankNameList = bankNameQry.list();
        if (bankNameList.size() > 0) {
          return bankNameList.get(0);
        } else {
          return bankList.get(0);
        }
      } else {
        return null;
      }
    } catch (Exception e) {
      log4j.error("Exception in getBankId()", e);
    }
    return null;
  }

  public static String getCurrencyId(String currency) {
    String currencyId = null;
    try {

      if (!currency.equals("") && currency != null) {

        OBQuery<Currency> currencyQry = OBDal.getInstance().createQuery(Currency.class,
            "trim(ISO_Code) = :iso_code ");
        currencyQry.setNamedParameter("iso_code", currency);
        List<Currency> currencyList = currencyQry.list();
        if (currencyList.size() > 0) {
          currencyId = currencyList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getCurrencyId method in ImportPOLinesDAO.java", e);
      return currencyId;
    }
    return currencyId;
  }

  public static boolean isValidDate(String date) {
    SimpleDateFormat df = new SimpleDateFormat("dd-mm-yyyy");
    boolean isValidHijiriDate = false;
    try {
      df.parse(date);

      PreparedStatement st = null;
      ResultSet rs = null;
      String gregDate = null;
      try {
        st = new DalConnectionProvider(false).getConnection()
            .prepareStatement("select eut_convertto_gregorian('" + date + "')");
        rs = st.executeQuery();
        if (rs.next()) {
          gregDate = rs.getString("eut_convertto_gregorian");
        }
      } catch (final Exception e) {
        log4j.error("Exception in isValidDate method in OpenEnvelopeEventImportDAO.java", e);
        return isValidHijiriDate;
      }
      if (gregDate != null) {
        isValidHijiriDate = true;
      }
      return isValidHijiriDate;
    } catch (ParseException e) {
      log4j.error("Exception in isValidDate method in OpenEnvelopeEventImportDAO.java", e);
      return isValidHijiriDate;
    }
  }

  public static String getBusinessPartnerId(String bpDocNo, VariablesSecureApp vars) {

    String bpId = null;
    // String[] bpName = name.split("-", 2);
    List<BusinessPartner> bpList = new ArrayList<BusinessPartner>();
    try {

      if (!bpDocNo.equals("") && bpDocNo != null) {
        OBQuery<BusinessPartner> bpQry = OBDal.getInstance().createQuery(BusinessPartner.class,
            " as e where e.efinDocumentno=:bpDocumentNo ");
        bpQry.setNamedParameter("bpDocumentNo", bpDocNo);
        bpList = bpQry.list();
        if (bpList.size() > 0) {
          bpId = bpList.get(0).getId();
        }
      }
      /*
       * st = new DalConnectionProvider(false).getConnection().prepareStatement(
       * "select c_bpartner_id from c_bpartner where em_efin_documentno=? and ad_client_id=?");
       * st.setString(1, bpDocNo); st.setString(2, vars.getClient()); rs = st.executeQuery(); if
       * (rs.next()) { bpId = rs.getString("c_bpartner_id"); }
       */
      /*
       * OBQuery<org.openbravo.model.common.businesspartner.BusinessPartner> bpQuery = OBDal
       * .getInstance()
       * .createQuery(org.openbravo.model.common.businesspartner.BusinessPartner.class,
       * "as e where efinDocumentno = :docNo"); bpQuery.setNamedParameter("docNo",
       * bpName[0].trim()); System.out.println(bpName[0].trim());
       * List<org.openbravo.model.common.businesspartner.BusinessPartner> bpList = bpQuery.list();
       * if (bpList.size() > 0) { bpId = bpList.get(0).getId(); }
       */

    } catch (Exception e) {
      log4j.error("Exception in getBusinessPartnerId method in OpenEnvelopeEventImportDAO.java", e);
      return bpId;
    }
    return bpId;

  }

  public static boolean checkIsValidSupplier(String bpDocumentNo,
      Escmopenenvcommitee openEnvelopeEventCom, VariablesSecureApp vars) {
    try {
      // String[] supplierName = bpName.split("-");
      // String name = supplierName[1].trim();
      Escmopenenvcommitee openEnvelopeEvent = OBDal.getInstance().get(Escmopenenvcommitee.class,
          openEnvelopeEventCom.getId());
      OBQuery<EscmBidMgmt> BidQry = OBDal.getInstance().createQuery(EscmBidMgmt.class,
          " as e where e.bidno = :bidno ");
      BidQry.setNamedParameter("bidno", openEnvelopeEvent.getBidNo().getBidno());
      List<EscmBidMgmt> bidList = BidQry.list();
      for (EscmBidMgmt bidObj : bidList) {
        if (bidObj.getBidtype().equals("LD")) {
          OBQuery<Escmbidsuppliers> bidSuppliers = OBDal.getInstance().createQuery(
              Escmbidsuppliers.class,
              " as e where  e.escmBidmgmt.id=:bidId and e.suppliernumber.id in (select bp.id from BusinessPartner bp where "
                  + " bp.efinDocumentno=:documentNo and client.id=:clientId) ");
          bidSuppliers.setNamedParameter("bidId", bidObj.getId());
          bidSuppliers.setNamedParameter("documentNo", bpDocumentNo);
          bidSuppliers.setNamedParameter("clientId", vars.getClient());

          List<Escmbidsuppliers> supplierList = bidSuppliers.list();
          if (supplierList.size() > 0) {
            return true;
          } else
            return false;

        } else if (bidObj.getBidtype().equals("TR")) {
          OBQuery<Escmsalesvoucher> bidRfpList = OBDal.getInstance()
              .createQuery(Escmsalesvoucher.class, " as e where  e.escmBidmgmt.id=:bidId ");
          bidRfpList.setNamedParameter("bidId", bidObj.getId());
          List<Escmsalesvoucher> rfpSuppliers = bidRfpList.list();
          if (rfpSuppliers.size() > 0) {
            OBQuery<Escmsalesvoucher> rfpSupplier = OBDal.getInstance().createQuery(
                Escmsalesvoucher.class,
                " as e where  e.escmBidmgmt.id=:bidId and e.supplierNumber.id in (select bp.id from BusinessPartner bp where "
                    + " bp.efinDocumentno=:documentNo  and client.id=:clientId)");
            rfpSupplier.setNamedParameter("bidId", bidObj.getId());
            rfpSupplier.setNamedParameter("documentNo", bpDocumentNo);
            rfpSupplier.setNamedParameter("clientId", vars.getClient());
            List<Escmsalesvoucher> rfpSupplierList = rfpSupplier.list();
            if (rfpSupplierList.size() > 0) {
              return true;
            } else
              return false;
          }
        }
      }
      return true;
    } catch (

    Exception e) {
      log4j.error("Exception in checkIsValidSupplier method in OpenEnvelopeEventImportDAO.java", e);
      return false;
    }
  }

  public static boolean checkIsValidSecondSupplier(String bpDocumentNo) {
    try {
      OBQuery<BusinessPartner> bpQry = OBDal.getInstance().createQuery(BusinessPartner.class,
          " as e where e.efinDocumentno=:bpDocumentNo ");
      bpQry.setNamedParameter("bpDocumentNo", bpDocumentNo);
      if (bpQry.list().size() > 0) {
        return true;
      } else {
        return false;
      }

    } catch (

    Exception e) {
      log4j.error(
          "Exception in checkIsValidSecondSupplier method in OpenEnvelopeEventImportDAO.java", e);
      return false;
    }
  }

  public static boolean validBGDatewithOpenEnvel(Escmopenenvcommitee openEnvelopeEventCom,
      String expiryDateH) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    try {
      OBContext.setAdminMode();
      String maxbidopenday = null, AftersixtyoneDays = null, bidId = null, gExpiryDate = null;
      String days = "60";
      Date AftersixtyoneDay = null;
      Escmopenenvcommitee openEnvelopeEvent = OBDal.getInstance().get(Escmopenenvcommitee.class,
          openEnvelopeEventCom.getId());
      OBQuery<EscmBidMgmt> BidQry = OBDal.getInstance().createQuery(EscmBidMgmt.class,
          " as e where e.bidno = :bidno ");
      BidQry.setNamedParameter("bidno", openEnvelopeEvent.getBidNo().getBidno());
      List<EscmBidMgmt> bidList = BidQry.list();
      if (bidList.size() > 0)
        bidId = bidList.get(0).getId();

      maxbidopenday = OpenEnvelopeDAO.getBidOpenenvelopday(bidId);

      AftersixtyoneDays = BGWorkbenchDAO.getRequestedExpirydate(maxbidopenday, days,
          openEnvelopeEventCom.getClient().getId(), null);
      AftersixtyoneDays = UtilityDAO.eventConvertToGregorian(AftersixtyoneDays.split("-")[2]
          + AftersixtyoneDays.split("-")[1] + AftersixtyoneDays.split("-")[0]);
      AftersixtyoneDay = sdf.parse(AftersixtyoneDays);
      gExpiryDate = UtilityDAO.eventConvertToGregorian(
          expiryDateH.split("-")[2] + expiryDateH.split("-")[1] + expiryDateH.split("-")[0]);
      boolean expiryDateless = sdf.parse(gExpiryDate).compareTo(AftersixtyoneDay) < 0;

      if (expiryDateless) {
        return true;
      } else {
        return false;
      }

    } catch (OBException e) {
      log4j.error("Exception while validBGDatewithOpenEnvel:" + e);
      throw new OBException(e.getMessage());
    } catch (ParseException e) {
      log4j.error("Exception while validBGDatewithOpenEnvel:" + e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public boolean checkDuplicate(String supplierID, Escmopenenvcommitee openEnvelopeEventCom,
      VariablesSecureApp vars) {
    if (openEnvelopeEventCom != null) {
      EscmBidMgmt bid = openEnvelopeEventCom.getBidNo();
      // String bpId = getBusinessPartnerId(bpDocumentNo, vars);
      OBQuery<EscmProposalMgmt> Duplicate = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
          "escmBidmgmt.id=:bidID and supplier.id=:supplierID and proposalstatus<>'CL'");
      Duplicate.setNamedParameter("bidID", bid.getId());
      Duplicate.setNamedParameter("supplierID", supplierID);

      if (Duplicate.list().size() > 0) {
        return true;
      } else {
        return false;
      }
    }
    return false;
  }
  // Check if proposal exists already for the supplier,if status is draft throw error

  public boolean checkExisitingProposalStatus(String bpDocumentNo,
      Escmopenenvcommitee openEnvelopeEventCom, VariablesSecureApp vars) {
    if (openEnvelopeEventCom != null) {
      EscmBidMgmt bid = openEnvelopeEventCom.getBidNo();
      // String bpId = getBusinessPartnerId(bpName, vars);
      OBQuery<EscmProposalMgmt> Duplicate = OBDal.getInstance().createQuery(EscmProposalMgmt.class,
          "escmBidmgmt.id=:bidID and supplier.id in (select e.id from BusinessPartner e where "
              + " e.efinDocumentno=:documentNo ) and proposalstatus<>'CL'");
      Duplicate.setNamedParameter("bidID", bid.getId());
      Duplicate.setNamedParameter("documentNo", bpDocumentNo);
      List<EscmProposalMgmt> propList = Duplicate.list();

      if (propList.size() > 0) {
        String propId = propList.get(0).getId();
        EscmProposalMgmt propManagement = OBDal.getInstance().get(EscmProposalMgmt.class, propId);
        if (propManagement.getProposalstatus().equals("DR")) {
          return true;
        }
      } else {
        return false;
      }
    }
    return false;
  }

  public static String getCurrency(String code) {

    String currencyId = null;
    try {

      if (!code.equals("") && code != null) {
        OBQuery<Currency> curQry = OBDal.getInstance().createQuery(Currency.class,
            " iSOCode = :code");
        curQry.setNamedParameter("code", code);
        List<Currency> currencyList = curQry.list();
        if (currencyList.size() > 0) {
          currencyId = currencyList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getCurrency method in OpenEnvelopeEventImportDAO.java", e);
      return currencyId;
    }
    return currencyId;
  }

  public static String getBankId(String bankName) {
    String bankId = null;
    try {
      if (!bankName.equals("") && bankName != null) {
        String[] bank = bankName.split("-", 2);
        OBQuery<EfinBank> bankQry = OBDal.getInstance().createQuery(EfinBank.class,
            " as e where e.bankname = :bankName");
        bankQry.setNamedParameter("bankName", bank[1].trim());
        List<EfinBank> bankList = bankQry.list();
        if (bankList.size() > 0) {
          bankId = bankList.get(0).getId();
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getBankId method in OpenEnvelopeEventImportDAO.java", e);
      return bankId;
    }
    return bankId;
  }

  public boolean checkIfBgSpecialityRoleExists(Escmopenenvcommitee openEnvelopeEventCom) {

    OBQuery<Preference> bgSpecialist = OBDal.getInstance().createQuery(Preference.class,
        "as e where e.property='ESCM_BGSpecialist_Role' and e.searchKey='Y' "
            + " and e.client.id=:clientID and active='Y'");
    bgSpecialist.setNamedParameter("clientID", openEnvelopeEventCom.getClient().getId());
    List<Preference> preference = bgSpecialist.list();
    if (preference != null && preference.size() > 0) {
      return true;
    } else {
      return false;
    }
  }

  public boolean checkBgExpDateGRT(String hStartDate, String hExpiryDate) {
    Date gStartDate = UtilityDAO.convertToGregorianDate(hStartDate.trim());
    Date gExpiryDate = UtilityDAO.convertToGregorianDate(hExpiryDate.trim());
    if (gStartDate.compareTo(gExpiryDate) > 0) {
      return true;

    } else
      return false;
  }

  public String getmaxbidproposallastdayandbidnumber(Escmopenenvcommitee openEnvelopeEventCom) {
    String sqlquery = null, bidno = "", strproposallastday = "", proposallastdayhijri = "";
    Date proposallastday = null;
    Query query = null;
    String message = "";

    String proposalSubmissionDate = null;
    Date proposalSubDate = null;
    SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    DateFormat yearFormat = Utility.YearFormat;
    EscmBidMgmt bid = openEnvelopeEventCom.getBidNo();

    String preferenceValue = "N";
    try {
      preferenceValue = Preferences.getPreferenceValue("ESCM_AllowPastDate", true,
          openEnvelopeEventCom.getClient().getId(), openEnvelopeEventCom.getOrganization().getId(),
          OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId(),
          Constants.PROPOSAL_MANAGEMENT_W);
      if (preferenceValue == null) {
        preferenceValue = "N";
      }
    } catch (PropertyException e) {
      preferenceValue = "N";
    }

    sqlquery = " select to_date(to_char(maxbd.proposallastday,'yyyy-MM-dd HH24:mi:ss'),'yyyy-MM-dd HH24:mi:ss') , bidm.bidno "
        + " ,eut_convert_to_hijri(to_char(maxbd.proposallastday,'yyyy-MM-dd')), max( maxbd.proposallastdaytime) "
        + " from ( select escm_bidmgmt_id, max(escm_biddates.proposallastday) as lastday from  escm_biddates group by escm_bidmgmt_id ) a "
        + " join escm_biddates maxbd on a.lastday= maxbd.proposallastday and a.escm_bidmgmt_id= maxbd.escm_bidmgmt_id "
        + " join escm_bidmgmt bidm on bidm.escm_bidmgmt_id=maxbd.escm_bidmgmt_id "
        + " where bidm.escm_bidmgmt_id=?  group by maxbd.proposallastday,bidm.bidno ";
    query = OBDal.getInstance().getSession().createSQLQuery(sqlquery);
    query.setParameter(0, bid.getId());
    List<Object> list = query.list();
    if (query.list().size() > 0) {
      for (@SuppressWarnings("rawtypes")
      Iterator iterator = list.iterator(); iterator.hasNext();) {
        Object[] row = (Object[]) iterator.next();
        if (row[0] != null) {
          try {
            strproposallastday = row[0].toString();
            proposallastday = dateformat
                .parse(strproposallastday + " " + row[3].toString() + ":00");
            // if (bid.getCreationDate() != null
            // && proposalmgmt.getSubmissiontime() != null) {
            proposalSubmissionDate = yearFormat.format(currentDate) + " "
                + new SimpleDateFormat("HH:MM").format(currentDate.getTime()) + ":00";
            proposalSubDate = dateformat.parse(proposalSubmissionDate);
            // }

          } catch (ParseException e) {
            // TODO Auto-generated catch block
            log4j.error("exception while getmaxbidproposallastdayandbidnumber() "
                + "in ProposalManagementEvent", e);
          }
        }
        if (row[1] != null) {
          bidno = row[1].toString();
        }
        if (row[2] != null) {
          proposallastdayhijri = row[2].toString() + " " + row[3].toString() + ":00";
        }
      }
    }

    // Skip Past Date validation if SCM_AllowPastDate preference is present
    if (proposallastday != null && proposalSubDate != null
        && proposallastday.compareTo(proposalSubDate) < 0 && preferenceValue.equals("N")) {

      message = OBMessageUtils.messageBD("ESCM_BidProposallastday");
      message = message.replace("$", proposallastdayhijri).replace("%", bidno);

    } else {
      message = "Success";
    }
    return message;
  }

}