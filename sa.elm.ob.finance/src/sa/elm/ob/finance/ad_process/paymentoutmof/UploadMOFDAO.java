package sa.elm.ob.finance.ad_process.paymentoutmof;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import sa.elm.ob.utility.util.UtilityDAO;

public class UploadMOFDAO {
  Connection con = null;
  private static Logger log4j = Logger.getLogger(UploadMOFDAO.class);
  UploadMOFVO VO = null;
  final static String quoted = "\"(:?[^\"]|\"\")+\"";

  public UploadMOFDAO(Connection con) {
    this.con = con;
  }

  /**
   * this method is used to get table ID
   * 
   * @param tabId
   * @return
   */
  public String getTableId(String tabId) {
    ResultSet rs = null;
    PreparedStatement st = null;
    String tableID = "";

    try {
      st = con.prepareStatement("select ad_table_id from ad_tab  where ad_tab_id  =? ");
      st.setString(1, tabId);
      log4j.debug(st.toString());
      rs = st.executeQuery();
      if (rs.next()) {
        tableID = rs.getString("ad_table_id");
      }
    } catch (Exception e) {
      log4j.error("Exception in getTableId :", e);
      return tableID;
    }
    return tableID;
  }

  /**
   * This method is used to validate file
   * 
   * @param excelFile
   * @param format
   * @param fileType
   * @param userId
   * @param clientId
   * @return
   * @throws IOException
   */
  public ArrayList<UploadMOFVO> validateFile(File excelFile, String format, String fileType,
      String userId, String clientId) throws IOException {
    int j = 1, flag = 0;
    PreparedStatement st = null;
    ResultSet rs = null;
    XSSFWorkbook xssfWorkbook = null;
    HSSFWorkbook hssfWorkbook = null;
    FileInputStream fin = null;

    Date mofcqDate = null, payRecDate = null;
    ArrayList<UploadMOFVO> rsLs = new ArrayList<UploadMOFVO>();
    boolean isXSSFWorkbook = false;
    int noOfSheets = 0;

    try {
      fin = new FileInputStream(excelFile);
      if (fileType.equals("xlsx")) {
        xssfWorkbook = new XSSFWorkbook(fin);
        isXSSFWorkbook = true;
      } else if (fileType.equals("xls"))
        hssfWorkbook = new HSSFWorkbook(fin);
      Sheet sheet = null;
      if (hssfWorkbook != null || xssfWorkbook != null) {
        if (isXSSFWorkbook)
          noOfSheets = xssfWorkbook.getNumberOfSheets();
        else
          noOfSheets = hssfWorkbook.getNumberOfSheets();
        VO = new UploadMOFVO();
        for (int i = 0; i < noOfSheets; i++) {
          j = 1;

          if (isXSSFWorkbook)
            sheet = xssfWorkbook.getSheetAt(i);
          else
            sheet = hssfWorkbook.getSheetAt(i);
          Row row = null;

          if (sheet != null) {
            if (sheet.getLastRowNum() == 0) {
              VO.setResult(2);
              VO.setDocNo("");
              VO.setColName("Empty Sheet");
              // VO.setMessage("Empty sheet cannot be uploaded");
              rsLs.add(VO);
            } else {
              for (; j <= sheet.getLastRowNum(); j++) {
                String docNo = "", mofBankName = "", mofChequeNo = "", mofChequeStatus = "",
                    mofChequeDate = "";
                String payReceivedBy = "", payReceivedDate = "";
                String accTyp = "";

                row = sheet.getRow(j);
                if (row != null) {
                  VO = new UploadMOFVO();
                  VO.setResult(1);
                  int uploadRes = 0;
                  String colName = "";
                  Cell cell = row.getCell(0);
                  if (cell != null && !cell.toString().equals("")) {
                    cell.setCellType(Cell.CELL_TYPE_STRING);
                    if (!cell.getStringCellValue().equals("")) {
                      docNo = cell.getStringCellValue();
                      if (!validateDoc(docNo)) {
                        VO.setResult(0);
                        VO.setDocNo(docNo);
                        colName = ",Document No";
                        // VO.setColName("Document No");
                        // VO.setMessage("Document No not matches");
                      }
                    }
                  } else {
                    VO.setResult(0);
                    VO.setDocNo(docNo);
                    colName = ",Document No";
                    // VO.setColName("Document No");
                    // VO.setMessage("Document No not matches");
                  }

                  /* Check Payment Account */
                  if (!docNo.equals("")) {
                    accTyp = checkFinanceAcc(docNo, clientId);// MOF SAN EXT
                    if (accTyp == null)
                      accTyp = "MOF";
                  }
                  /* MoF Details */
                  if (accTyp.equals("MOF") || accTyp.equals("EXT")) {
                    VO.setAccType("MOF/EXT");
                    if ((row.getCell(5) != null && !row.getCell(5).toString().equals(""))
                        || (row.getCell(6) != null && !row.getCell(6).toString().equals(""))) {
                      VO.setResult(0);
                      VO.setDocNo(docNo);
                      colName += "MOF/EXT Acc";
                    } else {
                      cell = row.getCell(1);
                      if (cell != null && !cell.toString().equals("")) {
                        if (!cell.getStringCellValue().equals(""))
                          mofBankName = cell.getStringCellValue();
                      } else {
                        VO.setResult(0);
                        VO.setDocNo(docNo);
                        colName += ",Bank Name";
                        // VO.setColName("Bank Name");
                        // VO.setMessage("MoF Bank Name cannot be empty");
                      }

                      cell = row.getCell(2);
                      if (cell != null && !cell.toString().equals("")) {
                        cell.setCellType(Cell.CELL_TYPE_STRING);
                        if (!cell.getStringCellValue().equals(""))
                          mofChequeNo = cell.getStringCellValue();
                      } else {
                        VO.setResult(0);
                        VO.setDocNo(docNo);
                        colName += ",Cheque No";
                        // VO.setColName("Cheque No");
                        // VO.setMessage("MoF Cheque No cannot be empty");
                      }
                      cell = row.getCell(3);
                      if (cell != null && !cell.toString().equals("")) {
                        mofChequeStatus = cell.getStringCellValue();
                        if (!mofChequeStatus.trim().equals("Issued")
                            && !mofChequeStatus.trim().equals("Cancelled")) {
                          VO.setResult(0);
                          VO.setDocNo(docNo);
                          colName += ",Cheque Status";
                          // VO.setColName("Cheque Status");
                          // VO.setMessage("Cheque Status should be Issued/Cancelled");
                        }
                      } else {
                        VO.setResult(0);
                        VO.setDocNo(docNo);
                        colName += ",Cheque Status";
                        // VO.setColName("Cheque Status");
                        // VO.setMessage("Cheque Status should be Issued/Cancelled");
                      }

                      cell = row.getCell(4);
                      if (cell != null && !cell.toString().equals("")) {
                        int innerFlag = 0;
                        try {
                          if (!cell.getDateCellValue().toString().equals("")) {
                            mofChequeDate = cell.getDateCellValue().toString();
                            flag = 0;
                          }
                        } catch (final Exception e) {
                          try {
                            mofChequeDate = cell.getStringCellValue();
                            flag = 1;
                          } catch (final Exception e1) {
                            VO.setResult(0);
                            VO.setDocNo(docNo);
                            colName += ",Cheque Date";
                            // VO.setColName("Cheque Date");
                            // VO.setMessage("Invalid Date format");
                          }
                        }

                        if (flag == 0 && mofChequeDate != null && mofChequeDate != "") {

                          try {
                            mofcqDate = parseExcelDate(mofChequeDate, format);
                          } catch (final Exception e) {
                            VO.setResult(0);
                            VO.setDocNo(docNo);
                            colName += ",Cheque Date";
                            // VO.setColName("Cheque Date");
                            // VO.setMessage("Invalid Date format");
                          }
                        }
                        if (flag == 1 && mofChequeDate != null && mofChequeDate != "") {

                          try {
                            mofcqDate = parseDate(mofChequeDate, format, false);
                          } catch (final Exception e) {
                            VO.setResult(0);
                            VO.setDocNo(docNo);
                            colName += ",Cheque Date";
                            // VO.setColName("Cheque Date");
                            // VO.setMessage("Invalid Date format");
                            innerFlag = 1;
                          }

                          if (innerFlag == 0) {
                            if (mofcqDate == null)
                              mofcqDate = parseDate(mofChequeDate, "mm/dd/yyyy", false);
                            if (mofcqDate == null)
                              mofcqDate = parseDate(mofChequeDate, "mm-dd-yyyy", false);
                            if (mofcqDate == null)
                              mofcqDate = parseDate(mofChequeDate, "yyyy/mm/dd", false);
                            if (mofcqDate == null)
                              mofcqDate = parseDate(mofChequeDate, "yyyy-mm-dd", false);
                            if (mofcqDate == null) {
                              VO.setResult(0);
                              VO.setDocNo(docNo);
                              colName += ",Cheque Date";
                              // VO.setColName("Cheque Date");
                              // VO.setMessage("Invalid Date format");
                            }
                          }
                        }
                      } else {
                        VO.setResult(0);
                        VO.setDocNo(docNo);
                        colName += ",Cheque Date";
                        // VO.setColName("Cheque Date");
                        // VO.setMessage("MoF Cheque Date cannot be empty");
                      }

                      if (VO.getResult() == 1) {
                        SimpleDateFormat formatter = new SimpleDateFormat(format);
                        mofChequeDate = formatter.format(mofcqDate);
                        if (UtilityDAO.Checkhijridate(mofChequeDate)) {
                          Date dateDate = formatter.parse(mofChequeDate);
                          SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyyMMdd");
                          String stringDate = dateYearFormat.format(dateDate);
                          st = con.prepareStatement(
                              "select gregorian_date as v_gregorian_date from eut_hijri_dates where hijri_date = ?;");
                          st.setString(1, stringDate);
                          rs = st.executeQuery();
                          if (rs.next()) {
                            st = con.prepareStatement(
                                "update fin_payment set em_efin_mofbankname=?, em_efin_mofchequeno=?, "
                                    + " em_efin_mofchequedate=to_date(?,'yyyy-MM-dd'), em_efin_mofchqstatus=?, updated=now(), updatedby=?  where documentno=? "
                                    + " and status in ('RPAP','RPAE','RPR','PPM','PWNC','RDNC','EFIN_WFA') and ad_client_id=? ");
                            st.setString(1, mofBankName);
                            st.setString(2, mofChequeNo);
                            st.setString(3, rs.getString("v_gregorian_date"));
                            st.setString(4, mofChequeStatus.equals("Issued") ? "I" : "C");
                            st.setString(5, userId);
                            st.setString(6, docNo);
                            st.setString(7, clientId);
                            uploadRes = st.executeUpdate();
                            VO.setUploadMoF(uploadRes);
                            VO.setDocNo(docNo);
                          }
                        } else {
                          VO.setResult(0);
                          VO.setDocNo(docNo);
                          colName += ",Cheque Date";
                          // VO.setColName("Cheque Date");
                          // VO.setMessage("Invalid Date format");
                        }
                      }
                    }
                  } else if (accTyp.equals("SAN")) {
                    VO.setAccType("SAN");
                    if ((row.getCell(1) != null && !row.getCell(1).toString().equals(""))
                        || (row.getCell(2) != null && !row.getCell(2).toString().equals(""))
                        || (row.getCell(3) != null && !row.getCell(3).toString().equals(""))
                        || (row.getCell(4) != null && !row.getCell(4).toString().equals(""))) {
                      VO.setResult(0);
                      VO.setDocNo(docNo);
                      colName += ",SAN Acc";
                    } else {
                      /* Payment Details */
                      flag = 0;
                      cell = row.getCell(5);
                      if (cell != null && !cell.toString().equals("")) {
                        int innerFlag = 0;
                        try {
                          if (!cell.getDateCellValue().toString().equals("")) {
                            payReceivedDate = cell.getDateCellValue().toString();
                            flag = 0;
                          }
                        } catch (final Exception e) {
                          try {
                            payReceivedDate = cell.getStringCellValue();
                            flag = 1;
                          } catch (final Exception e1) {
                            VO.setResult(0);
                            VO.setDocNo(docNo);
                            colName += ",Payment Received Date";
                            // VO.setColName("Payment Received Date");
                            // VO.setMessage("Invalid Date format");
                          }
                        }

                        if (flag == 0 && payReceivedDate != null && payReceivedDate != "") {

                          try {
                            payRecDate = parseExcelDate(payReceivedDate, format);
                          } catch (final Exception e) {
                            VO.setResult(0);
                            VO.setDocNo(docNo);
                            colName += ",Payment Received Date";
                            // VO.setColName("Payment Received Date");
                            // VO.setMessage("Invalid Date format");
                          }
                        }
                        if (flag == 1 && payReceivedDate != null && payReceivedDate != "") {

                          try {
                            payRecDate = parseDate(payReceivedDate, format, false);
                          } catch (final Exception e) {
                            VO.setResult(0);
                            VO.setDocNo(docNo);
                            colName += ",Payment Received Date";
                            // VO.setColName("Payment Received Date");
                            // VO.setMessage("Invalid Date format");
                            innerFlag = 1;
                          }

                          if (innerFlag == 0) {
                            if (payRecDate == null)
                              payRecDate = parseDate(payReceivedDate, "mm/dd/yyyy", false);
                            if (payRecDate == null)
                              payRecDate = parseDate(payReceivedDate, "mm-dd-yyyy", false);
                            if (payRecDate == null)
                              payRecDate = parseDate(payReceivedDate, "yyyy/mm/dd", false);
                            if (payRecDate == null)
                              payRecDate = parseDate(payReceivedDate, "yyyy-mm-dd", false);
                            if (payRecDate == null) {
                              VO.setResult(0);
                              VO.setDocNo(docNo);
                              colName += ",Payment Received Date";
                              // VO.setColName("Payment Received Date");
                              // VO.setMessage("Invalid Date format");
                            }
                          }
                        }
                      } else {
                        VO.setResult(0);
                        VO.setDocNo(docNo);
                        colName += ",Payment Received Date";
                        // VO.setColName("Payment Received Date");
                        // VO.setMessage("Payment Received Date cannot be empty");
                      }

                      cell = row.getCell(6);
                      if (cell != null && !cell.toString().equals("")) {
                        if (!cell.getStringCellValue().equals(""))
                          payReceivedBy = cell.getStringCellValue();
                      } else {
                        VO.setResult(0);
                        VO.setDocNo(docNo);
                        colName += ",Payment Received By";
                        // VO.setColName("Payment Received By");
                        // VO.setMessage("Payment Received By cannot be empty");
                      }

                      if (VO.getResult() == 1) {
                        SimpleDateFormat formatter = new SimpleDateFormat(format);
                        payReceivedDate = formatter.format(payRecDate);
                        if (UtilityDAO.Checkhijridate(payReceivedDate)) {
                          Date dateDate = formatter.parse(payReceivedDate);
                          SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyyMMdd");
                          String stringDate = dateYearFormat.format(dateDate);
                          st = con.prepareStatement(
                              "select gregorian_date as v_gregorian_date from eut_hijri_dates where hijri_date = ?;");
                          st.setString(1, stringDate);
                          rs = st.executeQuery();
                          if (rs.next()) {
                            st = con.prepareStatement(
                                "update fin_payment set em_efin_pay_receive_date=to_date(?,'yyyy-MM-dd'), "
                                    + "  em_efin_pay_receivedby=?, updated=now(), updatedby=?  where documentno=? and status in ('RPAP','RPAE','RPR','PPM','PWNC','RDNC','EFIN_WFA') and ad_client_id=? ");
                            st.setString(1, rs.getString("v_gregorian_date"));
                            st.setString(2, payReceivedBy);
                            st.setString(3, userId);
                            st.setString(4, docNo);
                            st.setString(5, clientId);
                            uploadRes = st.executeUpdate();
                            VO.setUploadMoF(uploadRes);
                            VO.setDocNo(docNo);
                          }
                        } else {
                          VO.setResult(0);
                          VO.setDocNo(docNo);
                          colName += ",Payment Received Date";
                          // VO.setColName("Payment Received Date");
                          // VO.setMessage("Invalid Date format");
                        }
                      }
                    }
                  }

                  if ((!docNo.equals(""))
                      || (row.getCell(1) != null && !row.getCell(1).toString().equals(""))
                      || (row.getCell(2) != null && !row.getCell(2).toString().equals(""))
                      || (row.getCell(3) != null && !row.getCell(3).toString().equals(""))
                      || (row.getCell(4) != null && !row.getCell(4).toString().equals(""))
                      || (row.getCell(5) != null && !row.getCell(5).toString().equals(""))
                      || (row.getCell(6) != null && !row.getCell(6).toString().equals(""))) {
                    colName = colName.replaceFirst(",", "");
                    VO.setColName(colName);
                    rsLs.add(VO);
                  }
                }
              }
              break;
            }
          }
        }
      }
    } catch (final Exception e) {
      log4j.error("Exception in validateFile() : ", e);
      excelFile.delete();
    } finally {
      if (fin != null) {
        try {
          fin.close();
        } catch (final IOException e) {
          log4j.error("Exception in validateFile() : ", e);
        }
      }
    }
    return rsLs;
  }

  /**
   * This method is used to validate CSV file
   * 
   * @param csvFile
   * @param format
   * @param userId
   * @param clientId
   * @return
   */
  public ArrayList<UploadMOFVO> validateCsvFile(File csvFile, String format, String userId,
      String clientId) {
    BufferedReader inpReader = null;
    FileReader inpFile = null;
    VO = new UploadMOFVO();
    Date mofcqDate = null;
    PreparedStatement st = null;
    ResultSet rs = null;
    ArrayList<UploadMOFVO> rsLs = new ArrayList<UploadMOFVO>();
    try {
      VO.setResult(1);
      VO.setMessage("Success");
      inpFile = new FileReader(csvFile);
      inpReader = new BufferedReader(inpFile);
      String inpLine = "";

      while ((inpLine = inpReader.readLine()) != null) {
        List<String> records = parseCSV(inpLine, ",");
        String fields[] = (String[]) records.toArray(new String[0]);
        for (int i = 0; i < 1; i++) {
          int innerFlag = 0;
          String docNo = "", mofBankName = "", mofChequeNo = "", mofChequeStatus = "",
              mofChequeDate = "";
          fields[i] = fields[i].replace("\"", "");
          log4j.debug("cell val>>" + fields[0] + "," + fields[1] + "," + fields[2] + "," + fields[3]
              + "," + fields[4]);
          if (fields[0] == null || fields[0].equals("")) {
            docNo = fields[0];
          }
          if (fields[1] == null || fields[1].equals("")) {
            mofBankName = fields[1];
          }
          if (fields[2] == null || fields[2].equals("")) {
            mofChequeNo = fields[2];
          }
          if (fields[3] == null || fields[3].equals("")) {
            mofChequeStatus = fields[3];
          }
          if (fields[4] == null || fields[4].equals("")) {
            try {
              mofcqDate = parseDate(fields[4], format, false);
            } catch (final Exception e) {
              VO.setResult(0);
              VO.setDocNo(docNo);
              VO.setMessage("Invalid Date format");
              innerFlag = 1;
            }
          }
          if (innerFlag == 1) {
            if (mofcqDate == null)
              mofcqDate = parseDate(fields[1], "mm/dd/yyyy", false);
            if (mofcqDate == null)
              mofcqDate = parseDate(fields[1], "mm-dd-yyyy", false);
            if (mofcqDate == null)
              mofcqDate = parseDate(fields[1], "yyyy/mm/dd", false);
            if (mofcqDate == null)
              mofcqDate = parseDate(fields[1], "yyyy-mm-dd", false);
            if (mofcqDate == null) {
              VO.setResult(0);
              VO.setDocNo(docNo);
              VO.setMessage("Invalid Date format");
            }
          }

          log4j.debug("cell val>>" + docNo + "," + mofBankName + "," + mofChequeNo + ","
              + mofChequeStatus + "," + mofChequeDate);
          if (VO.getResult() == 1) {
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            mofChequeDate = formatter.format(mofcqDate);
            if (UtilityDAO.Checkhijridate(mofChequeDate)) {
              Date dateDate = formatter.parse(mofChequeDate);
              SimpleDateFormat dateYearFormat = new SimpleDateFormat("yyyyMMdd");
              String stringDate = dateYearFormat.format(dateDate);
              st = con.prepareStatement(
                  "select gregorian_date as v_gregorian_date from eut_hijri_dates where hijri_date = ?;");
              st.setString(1, stringDate);
              rs = st.executeQuery();
              if (rs.next()) {
                st = con.prepareStatement(
                    "update fin_payment set em_efin_mofbankname=?, em_efin_mofchequeno=?, "
                        + " em_efin_mofchequedate=to_date(?,'yyyy-MM-dd'), em_efin_mofchqstatus=?, updated=now(), updatedby=?  where documentno=? "
                        + " and status in ('RPAP','PPM','RPPC','PWNC','EFIN_WFA') ");
                st.setString(1, mofBankName);
                st.setString(2, mofChequeNo);
                st.setString(3, rs.getString("v_gregorian_date"));
                st.setString(4, mofChequeStatus.equals("Issued") ? "I" : "C");
                st.setString(5, userId);
                st.setString(6, docNo);
                st.executeUpdate();
              }
            } else {
              VO.setResult(0);
              VO.setDocNo(docNo);
              VO.setMessage("Invalid Date format");
            }
          }
          rsLs.add(VO);
        }
      }
    } catch (final Exception ee) {
      log4j.error("Validate Csv File", ee);
      csvFile.delete();
    } finally {
      if (inpReader != null) {
        try {
          inpReader.close();
          inpFile.close();
        } catch (final Exception e) {
          log4j.error("Validate Csv File", e);
        }
      }
    }
    return rsLs;
  }

  /**
   * This method is used to parse
   * 
   * @param csv
   * @param delim
   * @return
   */
  public List<String> parseCSV(String csv, String delim) {
    final Pattern NEXT_COLUMN = nextColumnRegex(delim);
    final List<String> strings = new ArrayList<String>();
    final Matcher matcher = NEXT_COLUMN.matcher(csv);
    while (!matcher.hitEnd() && matcher.find()) {
      String match = matcher.group(1);
      if (match.matches(quoted))
        match = match.substring(1, match.length() - 1);
      match = match.replaceAll("\"\"", "\"");
      strings.add(match);
    }
    return strings;
  }

  /**
   * This method is used to regex
   * 
   * @param comma
   * @return
   */
  private Pattern nextColumnRegex(String comma) {
    String unquoted = "(:?[^\"" + comma + "]|\"\")*";
    String ending = "(:?" + comma + "|$)";
    return Pattern.compile('(' + quoted + '|' + unquoted + ')' + ending);
  }

  /**
   * This method is used to parse excel date
   * 
   * @param inpDate
   * @param format
   * @return
   */
  public Date parseExcelDate(String inpDate, String format) {
    String OLD_FORMAT = "EEE MMM d HH:mm:ss z yyyy";
    String format1 = "d MMM EEE HH:mm:ss z yyyy";
    String format2 = "d EEE MMM HH:mm:ss z yyyy";
    String format3 = "MMM EEE d HH:mm:ss z yyyy";
    Date outDate = null;
    SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
    try {
      outDate = sdf.parse(inpDate);
    } catch (final ParseException e) {
      try {
        sdf = new SimpleDateFormat(format1);
        outDate = sdf.parse(inpDate);
      } catch (final ParseException e1) {
        try {
          sdf = new SimpleDateFormat(format2);
          outDate = sdf.parse(inpDate);
        } catch (final ParseException e2) {
          try {
            sdf = new SimpleDateFormat(format3);
            outDate = sdf.parse(inpDate);
          } catch (final Exception e3) {
            throw new RuntimeException(e3.getMessage());
          }
        }
      }
    }
    SimpleDateFormat formatter = new SimpleDateFormat(format);
    try {
      formatter.format(outDate);
    } catch (final Exception e3) {
      throw new RuntimeException(e3.getMessage());
    }
    return outDate;
  }

  /**
   * This method is used to parse date
   * 
   * @param inpDate
   * @param format
   * @param lenient
   * @return
   */
  public Date parseDate(String inpDate, String format, boolean lenient) {
    Date date = null;
    String reFormat = Pattern.compile("d+|M+").matcher(Matcher.quoteReplacement(format))
        .replaceAll("\\\\d{1,2}");
    reFormat = Pattern.compile("y+").matcher(reFormat).replaceAll("\\\\d{4}");
    if (Pattern.compile(reFormat).matcher(inpDate).matches()) {
      // date string matches format structure,
      // - now test it can be converted to a valid date
      SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
      sdf.applyPattern(format);
      sdf.setLenient(lenient);
      try {
        date = sdf.parse(inpDate);
      } catch (final ParseException e) {
        throw new RuntimeException(e.getMessage());
      }
    }
    return date;
  }

  /**
   * This method is used to validate DOC
   * 
   * @param docNo
   * @return
   */
  public boolean validateDoc(String docNo) {
    PreparedStatement st = null;
    ResultSet rs = null;
    try {
      st = con.prepareStatement("select documentno from fin_payment where documentno =?");
      st.setString(1, docNo.trim());
      rs = st.executeQuery();
      if (rs.next()) {
        return true;
      }
    } catch (final SQLException e) {
      log4j.error("validateDoc", e);
      throw new RuntimeException(e.getMessage());
    } catch (final Exception e) {
      log4j.error("validateDoc", e);
      throw new RuntimeException(e.getMessage());
    } finally {
      try {
        if (st != null)
          st.close();
      } catch (final SQLException e) {
        log4j.error("validateDoc", e);
        throw new RuntimeException(e.getMessage());
      }
    }
    return false;
  }

  /**
   * This method is used to check finance Account
   * 
   * @param docNo
   * @param clientId
   * @return
   */
  public String checkFinanceAcc(String docNo, String clientId) {
    PreparedStatement st = null;
    ResultSet rs = null;
    String accTyp = "";
    try {
      st = con.prepareStatement(
          "select em_efin_acct_type from fin_financial_account where fin_financial_account_id = "
              + "	(select fin_financial_account_id from fin_payment  where documentno = ? and ad_client_id=?)");
      st.setString(1, docNo);
      st.setString(2, clientId);
      rs = st.executeQuery();
      if (rs.next()) {
        accTyp = rs.getString("em_efin_acct_type");// MOF SAN EXT
      }
    } catch (final SQLException e) {
      log4j.error("checkFinanceAcc", e);
      throw new RuntimeException(e.getMessage());
    } catch (final Exception e) {
      log4j.error("checkFinanceAcc", e);
      throw new RuntimeException(e.getMessage());
    } finally {
      try {
        if (st != null)
          st.close();
      } catch (final SQLException e) {
        log4j.error("checkFinanceAcc", e);
        throw new RuntimeException(e.getMessage());
      }
    }
    return accTyp;
  }
}