package sa.elm.ob.utility.util.datamigration;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBank;
import sa.elm.ob.scm.ESCMBGWorkbench;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.Escmbankguaranteedetail;

public class ImportBankGuarantee extends DalBaseProcess {

  private static final String DOCUMENT_TYPE = "P";
  private static final String BG_TYPE = "FBG";
  private static final String DEFAULT_ORG_NAME = "المركز الرئيسي";
  private static final String DEFAULT_CONTACT_KEY = "4292029";
  private HashMap<String, String> bankMapping = null;
  private static final String DATA_SHEET_NAME = "Data Sheet";
  private static final String BG_RATE = "1";
  private static final String DATE_FORMAT = "yyyyMMdd";
  private static final Logger log = LoggerFactory.getLogger(ImportBankGuarantee.class);
  private static final Logger resultLogger = LoggerFactory.getLogger(ResultLogger.class);

  private Organization organization = null;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    log.info("Import Intial BG Process Started");
    OBError obError = new OBError();
    String fileName = (String) bundle.getParams().get("filepath");
    String name[] = fileName.split(",");
    String operation = name[1];
    log.info("File Path:" + fileName);
    FileWriter fileWriter = null;

    try {
      fileWriter = new FileWriter(name[0]);

      boolean errorOccurred = false;
      organization = getDefaultOrganization(DEFAULT_ORG_NAME);

      if (operation.trim().equals("bid")) {
        processBidBgs(fileWriter);
      } else {
        processInternalNumberBgs(fileWriter);
      }
      // Here write the code to get the BG Workbench and Lines data
      // HashMap<String, BidBankGuarantee> fileMap =
      // BidBankGuaranteeProcessor.loadFile(fileName, sheetName );
      // bankMapping = BidBankGuaranteeProcessor.loadBankLookup(fileName);
      // if (fileMap != null && fileMap.size() > 0) {
      // errorOccurred = processFileMap(fileMap);
      // }
      if (!errorOccurred) {
        obError.setType("Success");
        obError.setTitle("Success");
      } else {
        obError.setType("Error");
        obError.setTitle("Error");
        obError
            .setMessage("some issues occurred during processing the file, please check log file.");
      }
    } catch (Exception e) {

      log.error("Error While Importing  Initial BG", e);
      obError.setType("Error");
      obError.setTitle("Error");
      obError.setMessage(e.getLocalizedMessage());
    } finally {
      try {

        fileWriter.flush();

        fileWriter.close();
      } catch (IOException e) {
        System.out.println("Error while flushing/closing fileWriter !!!");

      }

    }
    bundle.setResult(obError);
  }

  private void processInternalNumberBgs(FileWriter fileWriter) throws IOException {
    List<BidBankGuarantee> bidBgHeadersList = getInternalNumberBankGuarantees();
    // Validate if the given bid number and supplier number already exists
    for (BidBankGuarantee bidBankGuarantee : bidBgHeadersList) {
      try {

        // now also check that whether the supplier number exists in the DB
        BusinessPartner businessPartner = getBusinessPartnerByCRN(
            bidBankGuarantee.getCommercialNumber());
        if (null == businessPartner) {
          fileWriter.append("No Business Partner Found : " + bidBankGuarantee.getInternalNumber()
              + "," + bidBankGuarantee.getCommercialNumber() + "\n");
          log.info(" No Business Partner for CRN : " + bidBankGuarantee.getCommercialNumber());
          continue;
        }

        // Bid and Supplier Number found
        // Query to get the Lines
        List<BankGuarantee> bankGuarantees = getInternalNoBankGuaranteeLines(bidBankGuarantee);
        if (bankGuarantees.size() > 1) {
          fileWriter
              .append("More than 1 Line Rexcord Found : " + bidBankGuarantee.getInternalNumber()
                  + "," + bidBankGuarantee.getCommercialNumber() + "\n");

        } else {
          boolean error = processFileMap(bidBankGuarantee, bankGuarantees, fileWriter);
          if (!error) {
            fileWriter.append("Success : " + bidBankGuarantee.getInternalNumber() + ","
                + bidBankGuarantee.getCommercialNumber() + "\n");

          }

        }

      } catch (Exception exp) {
        fileWriter.append("exception  : " + bidBankGuarantee.getInternalNumber() + ","
            + bidBankGuarantee.getCommercialNumber() + "  Error :" + exp.getMessage() + "\n");
      }
    }
  }

  private void processBidBgs(FileWriter fileWriter) throws IOException {
    List<BidBankGuarantee> bidBgHeadersList = getBidBankGuarantees();
    // Validate if the given bid number and supplier number already exists
    for (BidBankGuarantee bidBankGuarantee : bidBgHeadersList) {
      try {
        String wbId = getBGRecord(bidBankGuarantee.getBidNo(),
            bidBankGuarantee.getCommercialNumber(), bidBankGuarantee.getBgType());
        if (null == wbId) {
          // now also check that whether the supplier number exists in the DB
          String businessPartner = getBPartner(bidBankGuarantee.getCommercialNumber());
          if (null == businessPartner) {
            fileWriter.append("No Business Partner Found : " + bidBankGuarantee.getBidNo() + ","
                + bidBankGuarantee.getCommercialNumber() + "\n");
            log.info(" No Business Partner for CRN : " + bidBankGuarantee.getCommercialNumber());
            continue;
          } else {
            EscmBidMgmt escmBidMgmt = getBid(bidBankGuarantee.getBidNo());
            if (null == escmBidMgmt) {
              log.info(" No Bid No found : " + bidBankGuarantee.getBidNo());
              fileWriter.append("No Bid Found: " + bidBankGuarantee.getBidNo() + ","
                  + bidBankGuarantee.getCommercialNumber() + "\n");
              continue;
            } else {
              // Bid and Supplier Number found
              // Query to get the Lines
              List<BankGuarantee> bankGuarantees = getBidBankGuaranteeLines(bidBankGuarantee);
              processFileMap(bidBankGuarantee, bankGuarantees, fileWriter);
              fileWriter.append("Success Inserted : " + bidBankGuarantee.getBidNo() + ","
                  + bidBankGuarantee.getCommercialNumber() + "\n");
            }
          }
        } else {
          fileWriter.append("Bank Guarnatee already exists : " + bidBankGuarantee.getBidNo() + ","
              + bidBankGuarantee.getCommercialNumber() + "\n");
          log.info("Bid Guarantee workbench already exists ",
              bidBankGuarantee.getBidNo() + "," + bidBankGuarantee.getCommercialNumber());
          // Get the Escm Work bench Record
          ESCMBGWorkbench escmbgWorkbench = getEscmWbById(wbId);
          List<Escmbankguaranteedetail> escmbankguaranteedetails = new ArrayList<Escmbankguaranteedetail>(
              escmbgWorkbench.getEscmBankguaranteeDetailList());
          List<BankGuarantee> bankGuarantees = getBidBankGuaranteeLines(bidBankGuarantee);
          Long lineNo = 10L;
          int records = 0;
          Double amount = escmbgWorkbench.getBGAmountSAR().doubleValue();
          boolean isRecord = false;
          for (BankGuarantee bankGuarantee : bankGuarantees) {
            boolean found = false;
            for (Escmbankguaranteedetail escmbankguaranteedetail : escmbankguaranteedetails) {
              if (escmbankguaranteedetail.getBankbgno().trim()
                  .equals(bankGuarantee.getLetterRefrenceNum().trim())) {
                found = true;
                break;
              }
            }

            if (!found) {
              cretaeDetailLines(bidBankGuarantee, escmbgWorkbench, bankGuarantee, lineNo,
                  fileWriter);
              amount += bankGuarantee.getAmount().doubleValue();
              lineNo += 10;
              isRecord = true;
              records++;

            }

          }
          escmbgWorkbench.setBGAmountSAR(new BigDecimal(amount));
          if (isRecord) {

            OBDal.getInstance().save(escmbgWorkbench);
            OBDal.getInstance().commitAndClose();
            fileWriter.append(" Escm BG Workbench Updated for " + bidBankGuarantee.getBidNo()
                + " Inserted :" + records + "\n");
          } else {
            fileWriter.append(
                " No Escm Workbench Record Updated for " + bidBankGuarantee.getBidNo() + "\n");
          }

        }
      } catch (Exception exp) {
        fileWriter.append("exception  : " + bidBankGuarantee.getBidNo() + ","
            + bidBankGuarantee.getCommercialNumber() + "  Error :" + exp.getMessage() + "\n");
      }
    }
  }

  protected String getSheetName() {
    return DATA_SHEET_NAME;
  }

  private Organization getDefaultOrganization(String defaultOrgName) {
    OBQuery<Organization> bpQuery = OBDal.getInstance().createQuery(Organization.class,
        " where name = :name ");
    bpQuery.setNamedParameter("name", defaultOrgName);
    bpQuery.setFilterOnActive(false);
    return bpQuery.uniqueResult();
  }

  private boolean processFileMap(BidBankGuarantee header, List<BankGuarantee> lines,
      FileWriter fileWriter) throws Exception {
    boolean errorOccurred = false;
    log.info("BG Found Count:" + lines.size());
    // List<BidBankGuarantee> fileLines = new
    // ArrayList<BidBankGuarantee>(fileMap.values());

    try {
      log.info("Start processing Bid:" + header.getBidNo());
      importBankGuarantee(header, lines, fileWriter);
      log.info("End processing Bid:" + header.getBidNo());
    } catch (Exception e) {

      fileWriter.append(" Error : " + header.getInternalNumber() + ","
          + header.getCommercialNumber() + "," + e.getMessage() + "\n");
      errorOccurred = true;
      log.error("Error while processing BID with BID #:" + header.getBidNo(), e);
      resultLogger.error(
          String.format("Error while processing Bid Bank G recored with BID #:" + header.getBidNo())
              + " Error:" + e.getMessage());
    }

    return errorOccurred;
  }

  private void importBankGuarantee(BidBankGuarantee bidBankGuarantee,
      List<BankGuarantee> bankGuaranteeList, FileWriter fileWriter) throws Exception {

    String message = String.format("Start processing Supplier:%s for Bid:%s",
        bidBankGuarantee.getCommercialNumber(), bidBankGuarantee.getBidNo());
    log.info(message);
    ESCMBGWorkbench bgWorkbench = OBProvider.getInstance().get(ESCMBGWorkbench.class);
    Year financialYear = getFinancialYear(bidBankGuarantee.getFinancialYear());
    if (financialYear == null) {
      String errorMessage = String.format("Can't find financial Year for :%s",
          bidBankGuarantee.getFinancialYear());
      throw new Exception(errorMessage);
    }
    bgWorkbench.setOrganization(organization);
    bgWorkbench.setFinancialYear(financialYear);
    bgWorkbench.setDocumentType(getDocumentType());
    // bgWorkbench.setInitialBG(getRate());
    bgWorkbench.setNotes(bidBankGuarantee.getSupplierName());
    addBidData(bidBankGuarantee, bgWorkbench);
    addBusinessPartnerData(bgWorkbench, bidBankGuarantee.getCommercialNumber());
    bgWorkbench.setType(getBgType(bidBankGuarantee.getBgType()));
    bgWorkbench.setContactName(getDefaultContact());
    Double amount = 0.0;
    Long lineNo = 10L;
    for (BankGuarantee bankGuarantee : bankGuaranteeList) {

      amount += bankGuarantee.getAmount().doubleValue();
      cretaeDetailLines(bidBankGuarantee, bgWorkbench, bankGuarantee, lineNo, fileWriter);
      lineNo += 10;

    }
    bgWorkbench.setBGAmountSAR(new BigDecimal(amount, MathContext.DECIMAL64));
    if (bgWorkbench.getEscmBankguaranteeDetailList() != null
        && bgWorkbench.getEscmBankguaranteeDetailList().size() > 0) {
      try {
        OBDal.getInstance().save(bgWorkbench);
        OBDal.getInstance().commitAndClose();
        resultLogger.info(String.format("Bank G created successfully for BID:%S, Supplier:%s",
            bidBankGuarantee.getBidNo(), bidBankGuarantee.getCommercialNumber()));
      } catch (Exception e) {
        OBDal.getInstance().rollbackAndClose();
        throw e;
      }

    } else {
      log.info(String.format(
          "No Record will be created for Supplier:%s for Bid:%s, there is no valid details line",
          bidBankGuarantee.getCommercialNumber(), bidBankGuarantee.getBidNo()));
      resultLogger.error(String.format(
          "No Record will be created for Supplier:%s for Bid:%s, there is no valid details line",
          bidBankGuarantee.getCommercialNumber(), bidBankGuarantee.getBidNo()));
    }

    message = String.format("End processing Supplier:%s for Bid:%s",
        bidBankGuarantee.getCommercialNumber(), bidBankGuarantee.getBidNo());
    log.info(message);

  }

  private String getBgType(String byTypeName) {
    if (byTypeName.trim().equals("Prev Maint Guarantee")) {
      return "PMG";
    } else if (byTypeName.trim().equals("Final BG")) {
      return "FBG";
    } else if (byTypeName.trim().equals("Down Payment Guarantee")) {
      return "DPG";
    } else if (byTypeName.trim().equals("Initial BG")) {
      return "IBG";
    }

    return "";
  }

  protected String getRate() {
    return BG_RATE;
  }

  protected String getDocumentType() {
    return DOCUMENT_TYPE;
  }

  protected String getType() {
    return BG_TYPE;
  }

  private void cretaeDetailLines(BidBankGuarantee supplierBankGuarantee,
      ESCMBGWorkbench bgWorkbench, BankGuarantee bankGuarantee, Long lineNumber,
      FileWriter fileWriter) throws Exception {

    try {
      String message = String.format("Start processing Bank:%s for Supplier:%s",
          bankGuarantee.getBankName(), supplierBankGuarantee.getCommercialNumber());
      log.info(message);
      Escmbankguaranteedetail bgLine = OBProvider.getInstance().get(Escmbankguaranteedetail.class);
      bgLine.setEscmBgworkbench(bgWorkbench);
      bgLine.setOrganization(organization);
      bgLine.setInternalno(Long.parseLong(bankGuarantee.getInternalNo()));
      bgLine.setEscmGuaGletterBookNo(bankGuarantee.getGuaGlitterBookNo());
      EfinBank efinBank = getBankByName(bankGuarantee.getBankCode());
      if (efinBank == null) {
        String errorMessage = String.format(
            "Can't find Bank with name:%s, no details line will be created for row #:%d",
            bankGuarantee.getBankName(), bankGuarantee.getLinNo());
        throw new Exception(errorMessage);
      }
      bgLine.setBankName(efinBank);
      bgLine.setBgamount(bankGuarantee.getAmount());
      bgLine.setBankbgno(bankGuarantee.getLetterRefrenceNum());
      bgLine.setBgstartdategre(bankGuarantee.getStartDate().toString());
      bgLine.setExpirydategre(bankGuarantee.getEndDate().toString());
      bgLine.setBgworkbench(true);
      Date startDate = Date
          .from(bankGuarantee.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
      bgLine.setBgstartdateh(startDate);
      Date expiryDate = Date
          .from(bankGuarantee.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
      bgLine.setExpirydateh(expiryDate);
      if (bgLine.getBgstartdateh().compareTo(bgLine.getExpirydateh()) > 0) {
        fileWriter.append(
            "Expiry Date Less then Start Date : " + supplierBankGuarantee.getInternalNumber() + ","
                + supplierBankGuarantee.getCommercialNumber() + ","
                + supplierBankGuarantee.getBidNo() + "\n");
        return;
      }

      bgLine.setLineNo(lineNumber);

      bgWorkbench.getEscmBankguaranteeDetailList().add(bgLine);

      message = String.format("End processing Bank:%s for Supplier:%s", bankGuarantee.getBankName(),
          supplierBankGuarantee.getCommercialNumber());
      log.info(message);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      resultLogger.error("Error while processing Bank Gurantee, Error:" + e.getMessage());
    }

  }

  private void addBusinessPartnerData(ESCMBGWorkbench bankGuarantee, String supplierId)
      throws Exception {
    BusinessPartner businessPartner = getBusinessPartnerByCRN(supplierId);
    bankGuarantee.setVendorName(businessPartner);
    // TODO check which address to use
    bankGuarantee.setPartnerAddress(businessPartner.getBusinessPartnerLocationList().get(0));
    bankGuarantee.setCurrency(businessPartner.getCurrency());

  }

  private BusinessPartner getDefaultContact() throws Exception {
    BusinessPartner contact = getBusinessPartnerByCRN(DEFAULT_CONTACT_KEY);
    if (contact == null) {
      String errorMessage = String.format("Can't find contact with CRN =%s, record will be skipped",
          DEFAULT_CONTACT_KEY);
      throw new Exception(errorMessage);
    }
    return contact;
  }

  private void addBidData(BidBankGuarantee bidBankGuarantee, ESCMBGWorkbench bankGuarantee) {
    EscmBidMgmt bid = getBid(bidBankGuarantee.getBidNo());
    if (bid != null) {
      bankGuarantee.setBidNo(bid);
      bankGuarantee.setBidName(bid.getBidname());
    } else {
      String errorMessage = String.format(
          "Can't find BID with Number =%s, record will be inserted without BID",
          bidBankGuarantee.getBidNo());
      log.info(errorMessage);
    }
  }

  private EfinBank getBankByName(String bankNo) throws Exception {

    EfinBank bank = getBankByNo(bankNo);

    if (bank == null) {
      String errorMessage = String
          .format("Can't find bank in openbravo with Id:%s, record will be skipped.", bankNo);
      throw new Exception(errorMessage);
    }
    return bank;
  }

  private EfinBank getBankByNo(String bankNo) {
    OBQuery<EfinBank> bpQuery = OBDal.getInstance().createQuery(EfinBank.class,
        " where searchKey = :searchKey ");
    bpQuery.setNamedParameter("searchKey", bankNo);
    bpQuery.setFilterOnActive(false);
    return bpQuery.uniqueResult();
  }

  private ESCMBGWorkbench getEscmWbById(String id) {
    OBQuery<ESCMBGWorkbench> bpQuery = OBDal.getInstance().createQuery(ESCMBGWorkbench.class,
        " where id = :id ");
    bpQuery.setNamedParameter("id", id);
    bpQuery.setFilterOnActive(false);
    return bpQuery.uniqueResult();
  }

  @SuppressWarnings("unused")
  private String mapBankNamme(String bankName) {
    return bankMapping.get(bankName);
  }

  private BusinessPartner getBusinessPartnerByCRN(String crnNmber) {
    OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class,
        " where escmCrnumber = :crnNmber or searchKey= :crnNmber");
    bpQuery.setNamedParameter("crnNmber", crnNmber);
    bpQuery.setNamedParameter("crnNmber", crnNmber);
    bpQuery.setFilterOnActive(false);
    return bpQuery.uniqueResult();
  }

  private EscmBidMgmt getBid(String bidNo) {
    OBQuery<EscmBidMgmt> bpQuery = OBDal.getInstance().createQuery(EscmBidMgmt.class,
        " where bidno = :bidno ");
    bpQuery.setNamedParameter("bidno", bidNo);
    bpQuery.setFilterOnActive(false);
    return bpQuery.uniqueResult();
  }

  private Year getFinancialYear(String financialYear) {
    OBQuery<Year> bpQuery = OBDal.getInstance().createQuery(Year.class,
        " where fiscalYear = :description ");
    bpQuery.setNamedParameter("description", financialYear);
    bpQuery.setFilterOnActive(false);
    return bpQuery.uniqueResult();
  }

  private List<BidBankGuarantee> getBidBankGuarantees() {

    String getTenderQuery = " SELECT financial_year , document_type , commercial_no , supplier_name , m_bg_type FROM escm_bg_bid_header ";
    List<BidBankGuarantee> bidBankGuaranteesList = new ArrayList<BidBankGuarantee>();
    log.info("GET BG Query ---> Query : " + getTenderQuery);
    PreparedStatement ps = null;
    ResultSet rs = null;
    BidBankGuarantee bidBankGuarantee = null;
    try {
      ps = getDbConnection().prepareStatement(getTenderQuery);

      rs = ps.executeQuery();

      while (rs.next()) {
        bidBankGuarantee = new BidBankGuarantee();
        bidBankGuarantee.setFinancialYear(rs.getString(1));
        bidBankGuarantee.setBidNo(rs.getString(2));
        bidBankGuarantee.setCommercialNumber(rs.getString(3));
        bidBankGuarantee.setSupplierName(rs.getString(4));
        bidBankGuarantee.setBgType(rs.getString(5));
        bidBankGuaranteesList.add(bidBankGuarantee);
      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block

    }

    return bidBankGuaranteesList;

  }

  private List<BidBankGuarantee> getInternalNumberBankGuarantees() {

    String getTenderQuery = " SELECT internal_no,financial_year , commercial_no , supplier_name , m_bg_type FROM escm_bg_legacy_no_header "
        + " ";
    List<BidBankGuarantee> bidBankGuaranteesList = new ArrayList<BidBankGuarantee>();
    log.info("GET BG Query ---> Query : " + getTenderQuery);
    PreparedStatement ps = null;
    ResultSet rs = null;
    BidBankGuarantee bidBankGuarantee = null;
    try {
      ps = getDbConnection().prepareStatement(getTenderQuery);

      rs = ps.executeQuery();

      while (rs.next()) {
        bidBankGuarantee = new BidBankGuarantee();
        bidBankGuarantee.setInternalNumber(rs.getString(1));
        bidBankGuarantee.setFinancialYear(rs.getString(2));
        bidBankGuarantee.setCommercialNumber(rs.getString(3));
        bidBankGuarantee.setSupplierName(rs.getString(4));
        bidBankGuarantee.setBgType(rs.getString(5));
        bidBankGuaranteesList.add(bidBankGuarantee);
      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block

    }

    return bidBankGuaranteesList;

  }

  private List<BankGuarantee> getInternalNoBankGuaranteeLines(BidBankGuarantee bidBankGuarantee) {

    String getTenderQuery = " SELECT ob_bank_name , bg_internal_no , LETTER_REFRENCE_NUM , BG_TOTAL_AMOUNT "
        + " , GUA_GEO_SDATE ,EXTENTION_DATE, GUA_GLETTER_BOOK_NO , ob_bank_code  FROM escm_bg_legacy_no_line WHERE bg_internal_no = ? ";
    List<BankGuarantee> bidBankGuaranteesList = new ArrayList<BankGuarantee>();
    log.info("GET BG Query ---> Query : " + getTenderQuery);
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = getDbConnection().prepareStatement(getTenderQuery);
      ps.setString(1, bidBankGuarantee.getInternalNumber());

      int i = 1;
      rs = ps.executeQuery();
      BankGuarantee bankGuarantee = null;
      while (rs.next()) {
        bankGuarantee = new BankGuarantee();

        bankGuarantee.setBankName(rs.getString(1));
        bankGuarantee.setInternalNo(rs.getString(2));
        bankGuarantee.setLetterRefrenceNum(rs.getString(3));
        bankGuarantee.setAmount(rs.getBigDecimal(4));
        bankGuarantee.setStartDate(parseDate(rs.getString(5)));
        bankGuarantee.setEndDate(parseDate(rs.getString(6)));
        bankGuarantee.setGuaGlitterBookNo(rs.getString(7));
        bankGuarantee.setBankCode(rs.getString(8));
        bankGuarantee.setLinNo(i++);
        bidBankGuaranteesList.add(bankGuarantee);

      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block

    }

    return bidBankGuaranteesList;

  }

  private List<BankGuarantee> getBidBankGuaranteeLines(BidBankGuarantee bidBankGuarantee) {

    String getTenderQuery = " SELECT ob_bank_name , bg_internal_no , LETTER_REFRENCE_NUM , BG_TOTAL_AMOUNT "
        + " , GUA_GEO_SDATE ,EXTENTION_DATE, GUA_GLETTER_BOOK_NO , ob_bank_code  FROM escm_bg_bid_line WHERE bid_no = ? "
        + " and commercial_no = ?";
    List<BankGuarantee> bidBankGuaranteesList = new ArrayList<BankGuarantee>();
    log.info("GET BG Query ---> Query : " + getTenderQuery);
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = getDbConnection().prepareStatement(getTenderQuery);
      ps.setString(1, bidBankGuarantee.getBidNo());
      ps.setString(2, bidBankGuarantee.getCommercialNumber());
      int i = 1;
      rs = ps.executeQuery();
      BankGuarantee bankGuarantee = null;
      while (rs.next()) {
        bankGuarantee = new BankGuarantee();

        bankGuarantee.setBankName(rs.getString(1));
        bankGuarantee.setInternalNo(rs.getString(2));
        bankGuarantee.setLetterRefrenceNum(rs.getString(3));
        bankGuarantee.setAmount(rs.getBigDecimal(4));
        bankGuarantee.setStartDate(parseDate(rs.getString(5)));
        bankGuarantee.setEndDate(parseDate(rs.getString(6)));
        bankGuarantee.setGuaGlitterBookNo(rs.getString(7));
        bankGuarantee.setBankCode(rs.getString(8));
        bankGuarantee.setLinNo(i++);
        bidBankGuaranteesList.add(bankGuarantee);

      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block

    }

    return bidBankGuaranteesList;

  }

  public String getBGRecord(String bidNo, String commercialNumber, String bgType) {
    log.debug("Bid No: " + bidNo);
    String escmWbId = null;
    String getTenderQuery = " select w.escm_bgworkbench_id from escm_bgworkbench w , c_bpartner c ,  escm_bidmgmt b"
        + " where w.c_bpartner_id = c.c_bpartner_id "
        + " and w.escm_bidmgmt_id = b.escm_bidmgmt_id "
        + " and b.bidno = ? and c.value = ? and w.bankguaranteetype=?";
    log.info("GET BG Query ---> Query : " + getTenderQuery);
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = getDbConnection().prepareStatement(getTenderQuery);
      ps.setString(1, bidNo);
      ps.setString(2, commercialNumber);
      ps.setString(3, getBgType(bgType));

      rs = ps.executeQuery();
      if (rs.next()) {
        escmWbId = (String) rs.getString(1);
      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block

    }

    return escmWbId;

  }

  public String getBPartner(String crn) {

    String escmWbId = null;
    String getTenderQuery = " select c_bpartner_id from c_bpartner Where value = ?";

    log.info("GET BG Query ---> Query : " + getTenderQuery);
    PreparedStatement ps = null;
    ResultSet rs = null;

    try {
      ps = getDbConnection().prepareStatement(getTenderQuery);
      ps.setString(1, crn);

      rs = ps.executeQuery();
      if (rs.next()) {
        escmWbId = (String) rs.getString(1);
      }
    } catch (SQLException e) {
      // TODO Auto-generated catch block

    }

    return escmWbId;

  }

  @SuppressWarnings("unused")
  private BusinessPartner getBusinessPartner(String supplierCRN, String supplierName) {
    BusinessPartner businessPartner = null;

    businessPartner = getBusinessPartnerByCRN(supplierCRN);
    if (businessPartner == null) {
      businessPartner = getBusinessPartnerByName(supplierName);
    }

    return businessPartner;
  }

  private BusinessPartner getBusinessPartnerByName(String supplierName) {
    OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class,
        " where name = :name ");
    bpQuery.setNamedParameter("name", supplierName);
    bpQuery.setFilterOnActive(false);
    return bpQuery.uniqueResult();
  }

  private Connection getDbConnection() {
    return OBDal.getInstance().getConnection();
  }

  private static LocalDate parseDate(String date) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
    LocalDate localDate = LocalDate.parse(date, formatter);
    return localDate;
  }
}
