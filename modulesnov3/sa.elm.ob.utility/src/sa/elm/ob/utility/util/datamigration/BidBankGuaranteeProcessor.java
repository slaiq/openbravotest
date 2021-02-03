package sa.elm.ob.utility.util.datamigration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BidBankGuaranteeProcessor {

	private static final String BANK_MAPPING_SHEET = "Bank Mapping";
	private static final int FINANCIAL_YEAR_INDEX = 0;
	private static final int INTERNAL_NO_INDEX = 1;
	private static final int BID_NO_INDEX = 2;
	private static final int SUPPLIER_NAME_INDEX = 7;
	private static final int SUPPLIER_CRN_INDEX = 8;
	private static final int BANK_NAME_INDEX = 10;
	private static final int AMOUNT_INDEX = 11;
	private static final int LETTER_REFRENCE_NUM_INDEX = 12;
	private static final int GUA_GLITTER_BOOK_NO_INDEX = 13;
	private static final int START_DATE_INDEX = 15;
	private static final int END_DATE_INDEX = 17;
	private static final String DATE_FORMAT = "yyyyMMdd";

	private static final Logger logger = LoggerFactory.getLogger(BidBankGuaranteeProcessor.class);
	private static final Logger resultLogger = LoggerFactory.getLogger(ResultLogger.class);
	
	public static HashMap<String, BidBankGuarantee> loadFile(String inputFilePath, String sheetName) throws Exception {
		HashMap<String, BidBankGuarantee> inputList = null;
		logger.info(String.format("File Name:%s, Sheet Name:%s", inputFilePath, sheetName));
		FileInputStream excelFile = new FileInputStream(new File(inputFilePath));
		Workbook workbook = new XSSFWorkbook(excelFile);
		Sheet datatypeSheet = workbook.getSheet(sheetName);
		Iterator<Row> iterator = datatypeSheet.iterator();
		inputList = new HashMap<String, BidBankGuarantee>();
		//TODO Skip header row !
		iterator.next();
		while (iterator.hasNext()) {
			Row currentRow = iterator.next();
			try {
				addLine(currentRow, inputList);
			} catch (Exception e) {
				String innerExceptionMessage = "No Inner Exception."; 
				if(e.getCause() != null) {
					innerExceptionMessage = e.getCause().getMessage();
				}
				String message = String.format("Error while Proccessing file line :%d, Error: %s, Inner Exception Message =%s", currentRow.getRowNum(),e.getMessage(),innerExceptionMessage);
				resultLogger.error(message);
			}
		}

		return inputList;
	}

	private static void addLine(Row line, HashMap<String, BidBankGuarantee> inputList) throws Exception {
		String bidNo = getBidNo(line);
		logger.info("Start Processing new BG line with BID #:" + bidNo);
		BidBankGuarantee bidBankGuarantee = inputList.get(bidNo);
		if (bidBankGuarantee == null) {
			bidBankGuarantee = createBidBankGuarantee(inputList, line);
		}
		addSupplierGuarantee(bidBankGuarantee, line);
		//TODO if error when create the supplier remove the bid
	}
	
	private static BidBankGuarantee createBidBankGuarantee(HashMap<String, BidBankGuarantee> inputList, Row line) {
		String bidNo = getBidNo(line);
		logger.info("Create BG to BID #:" + bidNo);
		BidBankGuarantee bidBankGuarantee = mapBankGuarantee(line);
		inputList.put(bidNo, bidBankGuarantee);
		return bidBankGuarantee;
	}
	
	private static BidBankGuarantee mapBankGuarantee(Row line) {
		BidBankGuarantee bidBankGuarantee = new BidBankGuarantee();
		bidBankGuarantee.setBidNo(getBidNo(line));
		bidBankGuarantee.setFinancialYear(getFinancialYear(line));
		return bidBankGuarantee;
	}

	private static void addBankGuarantee(SupplierBankGuarantee supplierBankGuarantee, Row line) {
		BankGuarantee bankGuarantee = createBankGuarantee(line);
		supplierBankGuarantee.getBanks().add(bankGuarantee );
	}

	private static BankGuarantee createBankGuarantee(Row line) {
		
		BankGuarantee bankGuarantee = new BankGuarantee();
		
		bankGuarantee.setBankName(getBankName(line));
		bankGuarantee.setInternalNo(getInternalNo(line));
		bankGuarantee.setLetterRefrenceNum(getLetterRefrenceNum(line));
		bankGuarantee.setAmount(getAmount(line));
		bankGuarantee.setStartDate(getStartDate(line));
		bankGuarantee.setEndDate(getEndDate(line));
		bankGuarantee.setGuaGlitterBookNo(getGuaGlitterBookNo(line));
		bankGuarantee.setLinNo(line.getRowNum());
		
		return bankGuarantee;
	}
	
	private static String getGuaGlitterBookNo(Row line) {
		Cell guaGlitterBookNoCell = line.getCell(GUA_GLITTER_BOOK_NO_INDEX);
		if (guaGlitterBookNoCell != null) {
			return guaGlitterBookNoCell.getStringCellValue();
		}
		return null;
	}

	private static String getBidNo(Row line) {
		return line.getCell(BID_NO_INDEX).getStringCellValue();
	}

	private static LocalDate getEndDate(Row line) {
		return parseDate(line.getCell(END_DATE_INDEX).getStringCellValue());
	}

	private static LocalDate getStartDate(Row line) {
		return parseDate(line.getCell(START_DATE_INDEX).getStringCellValue());
	}

	private static BigDecimal getAmount(Row line) {
		return new BigDecimal(line.getCell(AMOUNT_INDEX).getNumericCellValue());
	}

	private static String getLetterRefrenceNum(Row line) {
		return line.getCell(LETTER_REFRENCE_NUM_INDEX).getStringCellValue();
	}

	private static String getInternalNo(Row line) {
		Cell internalNoCell = line.getCell(INTERNAL_NO_INDEX);
		if (internalNoCell != null) {
			line.getCell(INTERNAL_NO_INDEX).setCellType(Cell.CELL_TYPE_STRING);
			return internalNoCell.getStringCellValue();
		}
		return null;
	}

	private static String getBankName(Row line) {
		return line.getCell(BANK_NAME_INDEX).getStringCellValue();
	}

	private static String getSupplierName(Row line) {
		return line.getCell(SUPPLIER_NAME_INDEX).getStringCellValue();
	}

	private static String getSupplierCRN(Row line) {
		Cell supplierCRNCell = line.getCell(SUPPLIER_CRN_INDEX);
		if (supplierCRNCell != null) {
			return supplierCRNCell.getStringCellValue();
		}
		return null;
	}
	
	private static String getFinancialYear(Row line) {
		line.getCell(FINANCIAL_YEAR_INDEX).setCellType(Cell.CELL_TYPE_STRING);
		return line.getCell(FINANCIAL_YEAR_INDEX).getStringCellValue();
	}
	
	private static BusinessPartner getBusinessPartner(String supplierCRN, String supplierName) {
		BusinessPartner businessPartner = null;

		businessPartner = getBusinessPartnerByCRN(supplierCRN);
		if (businessPartner == null) {
			businessPartner = getBusinessPartnerByName(supplierName);
		}

		return businessPartner;
	}

	private static BusinessPartner getBusinessPartnerByName(String supplierName) {
		OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class, " where name = :name ");
		bpQuery.setNamedParameter("name", supplierName);
		bpQuery.setFilterOnActive(false);
		return bpQuery.uniqueResult();
	}

	private static BusinessPartner getBusinessPartnerByCRN(String crnNmber) {
		OBQuery<BusinessPartner> bpQuery = OBDal.getInstance().createQuery(BusinessPartner.class, " where escmCrnumber = :crnNmber or searchKey= :crnNmber");
		bpQuery.setNamedParameter("crnNmber", crnNmber);
		bpQuery.setFilterOnActive(false);
		return bpQuery.uniqueResult();
	}

	private static LocalDate parseDate(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
		LocalDate localDate = LocalDate.parse(date, formatter);
		return localDate;
	}

	
	public static HashMap<String, String> loadBankLookup(String inputFilePath) throws IOException {
		HashMap<String, String> inputList = null;

		FileInputStream excelFile = new FileInputStream(new File(inputFilePath));
		Workbook workbook = new XSSFWorkbook(excelFile);
		Sheet datatypeSheet = workbook.getSheet(BANK_MAPPING_SHEET);
		Iterator<Row> iterator = datatypeSheet.iterator();
		inputList = new HashMap<String, String>();
		iterator.next();
		while (iterator.hasNext()) {
			Row currentRow = iterator.next();
			addBank(currentRow, inputList);
		}

		return inputList;
	}

	private static void addBank(Row row, HashMap<String, String> inputList) {
		String bankName = row.getCell(0).getStringCellValue();
		row.getCell(2).setCellType(Cell.CELL_TYPE_STRING);
		String bankNo = row.getCell(2).getStringCellValue();
		inputList.put(bankName, bankNo);
	}
	
	private static void addSupplierGuarantee(BidBankGuarantee bidBankGuarantee, Row line) throws Exception {
		String supplierCRN = getSupplierCRN(line);
		String supplierName = getSupplierName(line);
		BusinessPartner businessPartner = getBusinessPartner(supplierCRN, supplierName);
		if (businessPartner == null) { 
			String errorMessage = String.format("line No: %d with BID #: %s will be skipped: Can't find business partner with CRN:%s and Name:%s", line.getRowNum(), getBidNo(line),supplierCRN, supplierName);
			throw new Exception(errorMessage);	
		}
		String id = businessPartner.getId();
		SupplierBankGuarantee supplierBankGuarantee = bidBankGuarantee.getSuppliers().get(id);
		if (supplierBankGuarantee == null) {
			supplierBankGuarantee = new SupplierBankGuarantee();
			supplierBankGuarantee.setSupplierId(id);
			supplierBankGuarantee.setSupplierName(supplierName);
			bidBankGuarantee.getSuppliers().put(id, supplierBankGuarantee);
		}
		addBankGuarantee(supplierBankGuarantee, line);
	}
}
