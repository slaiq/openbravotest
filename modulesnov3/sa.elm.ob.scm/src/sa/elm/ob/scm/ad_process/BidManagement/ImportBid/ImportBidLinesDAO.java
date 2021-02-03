package sa.elm.ob.scm.ad_process.BidManagement.ImportBid;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.scm.ESCMProductCategoryV;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmBidmgmtLineV;
import sa.elm.ob.scm.EscmCsvBidLinesImport;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.utility.util.Utility;

public class ImportBidLinesDAO {

  private static final Logger log4j = Logger.getLogger(ImportBidLinesDAO.class);
  @SuppressWarnings("unused")
  private Connection conn = null;
  DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  Date currentDate = new Date();
  String formattedCurrentDateStr = dateFormat.format(currentDate);

  public ImportBidLinesDAO(Connection con) {
    this.conn = con;
  }

  public JSONObject processUploadedCsvFile(String inpBidId, VariablesSecureApp vars) {
    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;
    EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class, inpBidId);
    try {
      OBContext.setAdminMode(true);

      isSuccess = UpdateBidLines(bid);
      OBDal.getInstance().flush();

      log4j.debug("addOrUpdateBidLines isSuccess" + isSuccess);

      // if (isSuccess == 0) {
      // jsonresult = new JSONObject();
      // jsonresult.put("status", "0");
      // jsonresult.put("statusMessage", OBMessageUtils.messageBD("Escm_Import_Record_Failed"));
      // }
      if (isSuccess == 1) {
        jsonresult = new JSONObject();
        jsonresult.put("status", "1");
        jsonresult.put("statusMessage", OBMessageUtils.messageBD("Escm_Import_Record_Success"));
      }
    } catch (Exception e) {
      jsonresult = new JSONObject();
      try {
        jsonresult.put("status", "0");
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

  public void deleteCSVImportEntries() {
    List<EscmCsvBidLinesImport> csvImportList = null;
    try {
      OBQuery<EscmCsvBidLinesImport> csvImportQry = OBDal.getInstance()
          .createQuery(EscmCsvBidLinesImport.class, "order by creationDate");
      if (csvImportQry != null)
        csvImportList = csvImportQry.list();
      for (EscmCsvBidLinesImport csvimprt : csvImportList) {
        OBDal.getInstance().remove(csvimprt);
      }
    } catch (final Exception e) {
      e.printStackTrace();
      log4j.error("Exception in deleteCSVImportEntries() : ", e);
    }
  }

  public static Escmbidmgmtline getParent(Long lineno, String bidId) {
    Escmbidmgmtline bidLine = null;
    try {
      OBQuery<Escmbidmgmtline> ParentLine = OBDal.getInstance().createQuery(Escmbidmgmtline.class,
          " as e where e.lineNo = :ParentlineNo and e.escmBidmgmt.id = :bidId ");
      ParentLine.setNamedParameter("ParentlineNo", lineno);
      ParentLine.setNamedParameter("bidId", bidId);

      List<Escmbidmgmtline> parentLinesList = ParentLine.list();
      if (parentLinesList != null && parentLinesList.size() > 0) {
        bidLine = parentLinesList.get(0);
      }
    } catch (Exception e) {
      log4j.error("Exception in getParent", e);
    }
    return bidLine;
  }

  public static String getProductId(String itemCode) {
    String productId = null;
    try {

      if (!itemCode.equals("") && itemCode != null) {
        // String[] item = itemCode.split("-", 2);

        OBQuery<org.openbravo.model.common.plm.Product> itemCodeQry = OBDal.getInstance()
            .createQuery(org.openbravo.model.common.plm.Product.class, "trim(searchKey) = :code ");// and
                                                                                                   // trim(name)
                                                                                                   // =
                                                                                                   // :name
        itemCodeQry.setNamedParameter("code", itemCode);// item[0].trim())
        // itemCodeQry.setNamedParameter("name", item[1].trim());
        List<org.openbravo.model.common.plm.Product> itemCodeList = itemCodeQry.list();
        if (itemCodeList.size() > 0) {
          productId = itemCodeList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getProductId method in ImportPOLinesDAO.java", e);
      return productId;
    }
    return productId;
  }

  public static String getProductCategory(String pdtCategory) {
    String ProductCategory = null;
    try {
      if (!pdtCategory.equals("") && pdtCategory != null) {
        OBQuery<ESCMProductCategoryV> pdtCategoryQry = OBDal.getInstance()
            .createQuery(ESCMProductCategoryV.class, "validationCode = :code");
        pdtCategoryQry.setNamedParameter("code", pdtCategory);
        List<ESCMProductCategoryV> pdtCategoryList = pdtCategoryQry.list();
        if (pdtCategoryList.size() > 0) {
          ProductCategory = pdtCategoryList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getProductCategory", e);
    }
    return ProductCategory;
  }

  public static String getUOM(String uomName) {

    String uomId = null;
    try {

      if (!uomName.equals("") && uomName != null) {
        OBQuery<UOM> uomQuery = OBDal.getInstance().createQuery(UOM.class, " name = :name");
        uomQuery.setNamedParameter("name", uomName);
        List<UOM> uomCodeList = uomQuery.list();
        if (uomCodeList.size() > 0) {
          uomId = uomCodeList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getUOM method in ImportPOLinesDAO.java", e);
      return uomId;
    }
    return uomId;
  }

  public static String getUniqueCodeId(String uniqueCode) {
    String uniquecodeId = null;
    try {
      if (!uniqueCode.equals("") && !uniqueCode.isEmpty()) {
        OBQuery<AccountingCombination> uniqueCodeQry = OBDal.getInstance()
            .createQuery(AccountingCombination.class, "efinUniqueCode = '" + uniqueCode + "'");
        List<AccountingCombination> uniqueCodeList = uniqueCodeQry.list();
        if (uniqueCodeList.size() > 0) {
          uniquecodeId = uniqueCodeList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getUniqueCodeId method in ImportPOLinesDAO.java", e);
      return uniquecodeId;
    }
    return uniquecodeId;
  }

  private int UpdateBidLines(EscmBidMgmt bid) throws Exception {
    try {

      OBQuery<EscmCsvBidLinesImport> csvImportQry = OBDal.getInstance().createQuery(
          EscmCsvBidLinesImport.class,
          " as e where e.escmBidmgmt = :bidId order by e.creationDate ");
      csvImportQry.setNamedParameter("bidId", bid);
      List<EscmCsvBidLinesImport> csvImportList = csvImportQry.list();
      HashMap<Long, Long> parentLineMap = new HashMap<>();

      for (EscmCsvBidLinesImport csvImportLne : csvImportList) {

        Escmbidmgmtline parentId = null;
        String productId = null;
        String productCategoryId = null;
        ESCMProductCategoryV category = null;
        String uomId = null;
        String uniqueCodeId = null;
        Product product = null;

        if (csvImportLne.getEscmBidmgmtLine() != null) {
          OBQuery<Escmbidmgmtline> existionOrderLine = OBDal.getInstance().createQuery(
              Escmbidmgmtline.class,
              " as e where e.id = :bidLineId and e.escmBidmgmt.id = :bidId ");
          existionOrderLine.setNamedParameter("bidLineId",
              csvImportLne.getEscmBidmgmtLine().getId());
          existionOrderLine.setNamedParameter("bidId", bid);
          List<Escmbidmgmtline> linesList = existionOrderLine.list();
          if (linesList.size() > 0) {
            Escmbidmgmtline bidLine = linesList.get(0);
            bidLine.setLineNo(csvImportLne.getLineNo());
            if (csvImportLne.getParentlineNo() != null) {
              parentId = getParent(csvImportLne.getParentlineNo(), bid.getId());
              if (parentId != null) {
                EscmBidmgmtLineV parentLine = OBDal.getInstance().get(EscmBidmgmtLineV.class,
                    parentId.getId());
                bidLine.setParentline(parentLine);
              } else {
                bidLine.setParentline(null);
                parentLineMap.put(csvImportLne.getLineNo(), csvImportLne.getParentlineNo());
              }
            } else {
              bidLine.setParentline(null);
            }
            if (csvImportLne.getItem() != null && csvImportLne.getItem() != "") {
              productId = getProductId(csvImportLne.getItem());
              if (productId != null) {
                product = OBDal.getInstance().get(Product.class, productId);
                bidLine.setProduct(product);
              }
            } else {
              bidLine.setProduct(null);
            }

            if (csvImportLne.getItem() != null && csvImportLne.getItem() != "" && product != null) {
              bidLine.setDescription(product.getName());
            } else {
              if (csvImportLne.getDescription() != null) {
                bidLine.setDescription(csvImportLne.getDescription());
              }
            }

            if (csvImportLne.getProductCategory() != null
                && csvImportLne.getProductCategory() != "") {
              if (product != null && product.getProductCategory() != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                    product.getProductCategory().getId());
                bidLine.setProductCategory(category);

              } else {
                productCategoryId = getProductCategory(csvImportLne.getProductCategory());
                if (productCategoryId != null) {
                  category = OBDal.getInstance().get(ESCMProductCategoryV.class, productCategoryId);
                  bidLine.setProductCategory(category);
                }
              }
            } else {
              if (product != null && product.getProductCategory() != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                    product.getProductCategory().getId());
                bidLine.setProductCategory(category);
              }
            }
            if (csvImportLne.getUom() != null) {
              if (product != null && product.getUOM() != null) {
                bidLine.setUOM(product.getUOM());
              } else {
                uomId = getUOM(csvImportLne.getUom());
                if (uomId != null)
                  bidLine.setUOM(OBDal.getInstance().get(UOM.class, uomId));
              }
            }
            bidLine.setMovementQuantity(new BigDecimal(csvImportLne.getQtyOrdered()));
            bidLine.setSummarylevel(csvImportLne.isSummaryLevel());
            if (csvImportLne.getUniquecode() != null) {
              uniqueCodeId = getUniqueCodeId(csvImportLne.getUniquecode());

              // updating unique code name and funds available along with unique code
              if (uniqueCodeId != null) {
                AccountingCombination dimension = OBDal.getInstance()
                    .get(AccountingCombination.class, uniqueCodeId);

                bidLine.setUniquecodename(dimension.getEfinUniquecodename());
              } else {
                bidLine.setUniquecodename(null);
              }

            }
            OBDal.getInstance().save(bidLine);
          }
        } else {
          // insert new Bidline
          Escmbidmgmtline bidline = OBProvider.getInstance().get(Escmbidmgmtline.class);

          EscmBidMgmt objBid = csvImportLne.getEscmBidmgmt();
          bidline.setClient(objBid.getClient());
          bidline.setEscmBidmgmt(objBid);
          bidline.setOrganization(objBid.getOrganization());
          bidline.setCreationDate(new java.util.Date());
          bidline.setCreatedBy(objBid.getCreatedBy());
          bidline.setUpdated(new java.util.Date());
          bidline.setUpdatedBy(objBid.getUpdatedBy());
          bidline.setActive(true);
          bidline.setLineNo(csvImportLne.getLineNo());

          // Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
          //
          // TaxRate tax = null;
          //
          // OBQuery<TaxRate> objTaxQry = OBDal.getInstance().createQuery(TaxRate.class,
          // "as e order by e.creationDate desc");
          // objTaxQry.setMaxResult(1);
          // List<TaxRate> objTaxList = objTaxQry.list();
          // if (objTaxList.size() > 0) {
          // tax = objTaxList.get(0);
          // }
          //
          // if (tax != null) {
          // poline.setTax(tax);
          // }

          if (csvImportLne.getItem() != null && csvImportLne.getItem() != "") {
            productId = getProductId(csvImportLne.getItem());
            if (productId != null) {
              product = OBDal.getInstance().get(Product.class, productId);
              bidline.setProduct(product);
            }
          } else {
            bidline.setProduct(null);
          }

          if (csvImportLne.getItem() != null && csvImportLne.getItem() != "" && product != null) {
            bidline.setDescription(product.getName());
          } else {
            if (csvImportLne.getDescription() != null) {
              bidline.setDescription(csvImportLne.getDescription());
            }
          }

          if (csvImportLne.getUom() != null) {
            if (product != null && product.getUOM() != null) {
              bidline.setUOM(product.getUOM());
            } else {
              uomId = getUOM(csvImportLne.getUom());
              if (uomId != null)
                bidline.setUOM(OBDal.getInstance().get(UOM.class, uomId));
            }
          }

          bidline.setMovementQuantity(new BigDecimal(csvImportLne.getQtyOrdered()));

          bidline.setSummarylevel(csvImportLne.isSummaryLevel());

          if (csvImportLne.getProductCategory() != null
              && csvImportLne.getProductCategory() != "") {
            if (product != null && product.getProductCategory() != null) {
              category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                  product.getProductCategory().getId());
              bidline.setProductCategory(category);

            } else {
              productCategoryId = getProductCategory(csvImportLne.getProductCategory());
              if (productCategoryId != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class, productCategoryId);
                bidline.setProductCategory(category);
              }
            }
          } else {
            if (product != null && product.getProductCategory() != null) {
              category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                  product.getProductCategory().getId());
              bidline.setProductCategory(category);
            }
          }

          if (csvImportLne.getParentlineNo() != null) {
            parentId = getParent(csvImportLne.getParentlineNo(), bid.getId());
            if (parentId != null) {
              EscmBidmgmtLineV parentLineV = OBDal.getInstance().get(EscmBidmgmtLineV.class,
                  parentId.getId());
              bidline.setParentline(parentLineV);
            } else {
              bidline.setParentline(null);
              parentLineMap.put(csvImportLne.getLineNo(), csvImportLne.getParentlineNo());
            }
          } else {
            bidline.setParentline(null);
          }

          if (csvImportLne.getUniquecode() != null) {
            uniqueCodeId = getUniqueCodeId(csvImportLne.getUniquecode());

            // updating unique code name and funds available along with unique code
            if (uniqueCodeId != null) {
              AccountingCombination dimension = OBDal.getInstance().get(AccountingCombination.class,
                  uniqueCodeId);
              bidline.setUniquecodename((dimension.getEfinUniquecodename()));
            } else {
              bidline.setUniquecodename(null);
            }

          }
          OBDal.getInstance().save(bidline);
        }
      }
      OBDal.getInstance().flush();

      // Update parent line id
      for (Long lineNo : parentLineMap.keySet()) {
        OBQuery<Escmbidmgmtline> lineQry = OBDal.getInstance().createQuery(Escmbidmgmtline.class,
            " as e where e.escmBidmgmt.id = :bidId and e.lineNo = :lineNo");
        lineQry.setNamedParameter("bidId", bid.getId());
        lineQry.setNamedParameter("lineNo", lineNo);
        List<Escmbidmgmtline> lineList = lineQry.list();
        if (lineList.size() > 0) {
          Escmbidmgmtline line = lineList.get(0);

          Escmbidmgmtline parentLine = getParent(parentLineMap.get(lineNo), bid.getId());
          if (parentLine != null) {
            EscmBidmgmtLineV parentLineV = OBDal.getInstance().get(EscmBidmgmtLineV.class,
                parentLine.getId());
            line.setParentline(parentLineV);
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Error in ImportPOLines.java : UpdateBidLines() ", e);
      throw new Exception(e.getMessage());
    }
    return 1;
  }

  private String getCellValue(Cell cell) {
    try {
      if (cell == null)
        return "";
      if (Cell.CELL_TYPE_NUMERIC == cell.getCellType()) {
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

  private boolean checkValidBidLines(String bidLineId, String bidId) {
    boolean result = false;
    try {

      Escmbidmgmtline line = OBDal.getInstance().get(Escmbidmgmtline.class, bidLineId);

      if (bidId.equals(line.getEscmBidmgmt().getId())) {
        result = true;
      }
      return result;
    } catch (final Exception e) {
      log4j.error("Exception in checkValidOrderLines() : ", e);
      return result;
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
      log4j.error("Exception in chkisnull method in ImportPOLinesDAO.java", e);
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
      log4j.error("Exception in chkisnegative method in ImportPOLinesDAO.java", e);

    }
    return true;
  }

  public static boolean isValidParentLine(String parentLineNo, String bidId) {
    boolean isValidParentLine = false;
    try {
      if (!parentLineNo.equals("") && parentLineNo != null) {
        OBQuery<Escmbidmgmtline> parentLineQry = OBDal.getInstance()
            .createQuery(Escmbidmgmtline.class, "escmBidmgmt.id = :bidId and lineNo = :lineNo");
        parentLineQry.setNamedParameter("bidId", bidId);
        parentLineQry.setNamedParameter("lineNo", Long.parseLong(parentLineNo));
        List<Escmbidmgmtline> parentLineList = parentLineQry.list();
        if (parentLineList.size() > 0) {
          isValidParentLine = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidParentLine method in ImportBidLinesDAO.java", e);
      return isValidParentLine;
    }
    return isValidParentLine;
  }

  public static boolean isValidItemCode(String itemCode) {
    boolean isValidItemcode = false;
    try {
      if (itemCode.equals("-")) {
        isValidItemcode = true;
        return isValidItemcode;
      }
      if (!itemCode.equals("") && itemCode != null) {
        // String[] item = itemCode.split("-", 2);
        // if (item.length == 2) {
        OBQuery<Product> itemCodeQry = OBDal.getInstance().createQuery(Product.class,
            "trim(searchKey) = :code ");// and trim(name) = :name
        itemCodeQry.setNamedParameter("code", itemCode);// item[0].trim()
        // itemCodeQry.setNamedParameter("name", item[1].trim());

        List<Product> itemCodeList = itemCodeQry.list();
        if (itemCodeList.size() > 0) {
          isValidItemcode = true;
        }
        // }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidItemCode method in ImportPOLinesDAO.java", e);
      return isValidItemcode;
    }
    return isValidItemcode;
  }

  public static boolean isValidPdtCategory(String pdtCategory) {
    boolean isValidPdtCategory = false;
    try {
      if (!pdtCategory.equals("") && pdtCategory != null) {
        OBQuery<ESCMProductCategoryV> pdtCategoryQry = OBDal.getInstance()
            .createQuery(ESCMProductCategoryV.class, "validationCode = :code");
        pdtCategoryQry.setNamedParameter("code", pdtCategory);
        List<ESCMProductCategoryV> pdtCategoryList = pdtCategoryQry.list();
        if (pdtCategoryList.size() > 0) {
          isValidPdtCategory = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidPdtCategory method in ImportPOLinesDAO.java", e);
      return isValidPdtCategory;
    }
    return isValidPdtCategory;
  }

  public static boolean isValidUOM(String uom) {
    boolean isValidUOM = false;
    try {
      if (!uom.equals("") && uom != null) {
        OBQuery<UOM> uomQry = OBDal.getInstance().createQuery(UOM.class, "name = :name");
        uomQry.setNamedParameter("name", uom);
        List<UOM> uomList = uomQry.list();
        if (uomList.size() > 0) {
          isValidUOM = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidUOM method in ImportPOLinesDAO.java", e);
      return isValidUOM;
    }
    return isValidUOM;
  }

  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars, String inpBidId,
      boolean isUpload) {
    JSONObject resultJSON = new JSONObject();
    FileInputStream inputStream = null;
    Sheet sheet = null;
    XSSFWorkbook xssfWorkbook = null;

    try {
      OBContext.setAdminMode(true);

      boolean validFile = true;

      ArrayList<String> lineNoList = new ArrayList<String>();

      StringBuffer resultMessage = new StringBuffer();
      StringBuffer numericErrorDetails = new StringBuffer();
      StringBuffer negativeErrorDetails = new StringBuffer();
      StringBuffer uniqueCodeDetails = new StringBuffer();
      StringBuffer itemCodeDetails = new StringBuffer();
      StringBuffer pdtCategoryDetails = new StringBuffer();
      StringBuffer needByDateDetails = new StringBuffer();
      StringBuffer uomDetails = new StringBuffer();
      StringBuffer parentLineDetails = new StringBuffer();
      StringBuffer qtyDetails = new StringBuffer();
      StringBuffer priceDetails = new StringBuffer();
      StringBuffer nationalPdtDetails = new StringBuffer();
      StringBuffer changetypeDetails = new StringBuffer();
      StringBuffer lineExistsDetails = new StringBuffer();
      StringBuffer descriptionMandatory = new StringBuffer();

      boolean noMandatoryFields = false;
      boolean hasErrorMessage = false;
      // boolean hasValidLine = false;
      boolean isParentInvalid = false;
      boolean lineNumberExists = false;

      EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class, inpBidId);

      // Get Work Book
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);
      boolean isLineNoBlank = false;
      // Get all line numbers and store it in lineNoList
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          if (validFile) {
            sheet = xssfWorkbook.getSheetAt(s);
            if (sheet == null)
              break;
            int startRow = 1;

            Row row = null;
            Cell cell = null;

            for (int i = startRow;; i++) {

              row = sheet.getRow(i);

              if (row == null)
                break;

              if (row.getCell(1) != null && StringUtils.isEmpty(row.getCell(1).toString()))
                break;

              // Line No
              cell = row.getCell(1);
              if (cell != null) {
                if (lineNoList.contains(getCellValue(cell))) {
                  lineNumberExists = true;
                  validFile = false;
                  break;
                } else {
                  lineNoList.add(getCellValue(cell));
                }
              } else {
                validFile = false;
                noMandatoryFields = true;
                break;
              }
            }
          }
        }
      }
      if (xssfWorkbook.getNumberOfSheets() > 0) {
        for (int s = 0; s < xssfWorkbook.getNumberOfSheets(); s++) {
          if (validFile) {
            // Get Each Sheet
            sheet = xssfWorkbook.getSheetAt(s);
            if (sheet == null)
              break;

            boolean isUpdateSheet = false;
            if (sheet.getRow(s) != null) {
              Cell updateCell = sheet.getRow(s).getCell(0);
              if (updateCell != null) {
                String update = getCellValue(updateCell);
                if (update != null && update.equals("escm_bidmgmt_line_id")) {
                  isUpdateSheet = true;
                }
              }
            }

            // Initialize Data to be inserted in temporary table
            BigDecimal orderQty = BigDecimal.ZERO;
            Escmbidmgmtline bidLne = null;

            String description = "";
            String uniqueCode = "";
            String productCategory = "";
            int startRow = 1;

            // Get Each Row
            for (int i = startRow;; i++) {
              boolean hasError = false;
              Row row = null;
              Cell cell = null;
              String bidLineId = null;
              String lineNo = "";
              String parentLineNo = "";
              String cellVal = "";
              String itemCode = "";
              String uom = "";
              String summary = "";
              boolean isSummary = false;
              String strOrderQty = null;

              row = sheet.getRow(i);

              if (row == null)
                break;

              if (StringUtils.isEmpty(row.getCell(1).toString())
                  && StringUtils.isEmpty(row.getCell(2).toString())
                  && StringUtils.isEmpty(row.getCell(3).toString())
                  && StringUtils.isEmpty(row.getCell(4).toString())
                  && StringUtils.isEmpty(row.getCell(5).toString())
                  && StringUtils.isEmpty(row.getCell(6).toString())
                  && StringUtils.isEmpty(row.getCell(7).toString())
                  && StringUtils.isEmpty(row.getCell(8).toString())
                  && StringUtils.isEmpty(row.getCell(9).toString()))
                break;

              // BidLine
              cell = row.getCell(0);
              if (cell != null) {
                bidLineId = Utility.nullToEmpty(getCellValue(cell));
                bidLne = OBDal.getInstance().get(Escmbidmgmtline.class, bidLineId);

                if (bidLne != null) {
                  if (!checkValidBidLines(bidLineId, inpBidId)) {
                    resultMessage.append(OBMessageUtils.messageBD("Escm_DifferentBidLines"));
                    hasErrorMessage = true;
                    break;
                  }
                } else {
                  String lineNumber = getCellValue(row.getCell(1));
                  String message = OBMessageUtils.messageBD("Escm_BidlineNotExists");
                  message = message.replace("%", lineNumber);
                  resultMessage.append(message);
                  hasErrorMessage = true;
                  break;
                }
              } else {
                // validFile = false;
                // break;
                bidLne = null;
              }

              // Line No
              cell = row.getCell(1);
              if (cell != null && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (cellVal != "" && cellVal != null) {
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + cellVal
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(cellVal)) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + cellVal
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    if (!isUpdateSheet) {
                      boolean isLineExists = isValidParentLine(cellVal, bid.getId());
                      if (isLineExists) {
                        lineExistsDetails.append(
                            OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + cellVal);
                        lineExistsDetails.append("<br>");
                        hasError = true;
                      }
                    }
                    lineNo = getCellValue(cell);
                  }
                } else {
                  validFile = false;
                  break;
                }
              } else {
                isLineNoBlank = true;
                hasError = true;
              }

              // parent Line no
              cell = row.getCell(2);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (!isNumeric(cellVal)) {
                  numericErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  numericErrorDetails.append("<br>");
                  hasError = true;
                } else if (!chkisnegative(getCellValue(cell))) {
                  negativeErrorDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  negativeErrorDetails.append("<br>");
                  hasError = true;
                } else if (cellVal != "" && !lineNoList.contains(cellVal)) {
                  parentLineDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  parentLineDetails.append("<br>");
                  hasError = true;
                } else {
                  parentLineNo = getCellValue(cell);
                  if (isUpdateSheet) {
                    if (parentLineNo != null && parentLineNo != ""
                        && !isValidParentLine(parentLineNo, inpBidId)) {
                      isParentInvalid = true;
                      parentLineDetails
                          .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                              + lineNo + " -> " + cellVal);
                      parentLineDetails.append("<br>");
                      hasError = true;
                    }
                  }
                }
              }

              // item Code
              cell = row.getCell(3);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "" && !isValidItemCode(cellVal)) {
                  itemCodeDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                      + lineNo + " ->  " + cellVal);
                  itemCodeDetails.append("<br>");
                  hasError = true;
                } else {
                  itemCode = getCellValue(cell);
                }
              }

              // Description
              cell = row.getCell(4);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  description = getCellValue(cell);
                } else {
                  if (itemCode == null || itemCode.equals("")) {
                    descriptionMandatory
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    descriptionMandatory.append("<br>");
                    hasError = true;
                  }
                }
              } else {
                if (itemCode == null || itemCode.equals("")) {
                  descriptionMandatory
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  descriptionMandatory.append("<br>");
                  hasError = true;
                }
              }

              // product category
              cell = row.getCell(5);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "" && !isValidPdtCategory(cellVal)) {
                  pdtCategoryDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  pdtCategoryDetails.append("<br>");
                  hasError = true;
                } else {
                  productCategory = getCellValue(cell);
                }
              }

              // UOM
              cell = row.getCell(6);
              if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK
                  && getCellValue(cell) != "") {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "" && !isValidUOM(cellVal)) {
                  uomDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                      + lineNo + " ->  " + cellVal);
                  uomDetails.append("<br>");
                  hasError = true;
                } else {
                  uom = getCellValue(cell);
                }
              } else {
                uomDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                    + lineNo + " ->  " + cellVal);
                uomDetails.append("<br>");
                hasError = true;
              }

              // summary
              cell = row.getCell(10);
              if (cell != null) {
                cellVal = getCellValue(cell);
                isSummary = (cell.toString().equals("Y")) ? true : false;
              }

              // Qty Ordered
              cell = row.getCell(7);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  strOrderQty = cellVal;
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(getCellValue(cell))) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    if (cellVal.equals("0") && !isSummary) {
                      qtyDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                          + lineNo + " ->  " + cellVal);
                      qtyDetails.append("<br>");
                      hasError = true;
                    } else {
                      orderQty = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                  }
                } else {
                  qtyDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                      + lineNo + " ->  " + cellVal);
                  qtyDetails.append("<br>");
                  hasError = true;
                }
              } else {
                qtyDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                    + lineNo + " ->  " + cellVal);
                qtyDetails.append("<br>");
                hasError = true;
              }

              if (bidLne != null) {

                // If no error, insert into temporary table
                if (!hasError) {
                  EscmCsvBidLinesImport csvImport = OBProvider.getInstance()
                      .get(EscmCsvBidLinesImport.class);
                  csvImport.setEscmBidmgmtLine(bidLne);
                  csvImport.setLineNo(Long.parseLong(lineNo));
                  if (parentLineNo != "")
                    csvImport.setParentlineNo(Long.parseLong(parentLineNo));
                  csvImport.setQtyOrdered(orderQty.longValue());
                  csvImport.setUniquecode(uniqueCode);
                  csvImport.setItem(itemCode);
                  csvImport.setDescription(description);
                  csvImport.setProductCategory(productCategory);
                  csvImport.setUom(uom);
                  csvImport.setEscmBidmgmt(bid);
                  if (summary.equals("Y")) {
                    csvImport.setSummaryLevel(true);
                  } else {
                    csvImport.setSummaryLevel(false);
                  }
                  csvImport.setEscmBidmgmt(bid);
                  OBDal.getInstance().save(csvImport);
                  OBDal.getInstance().flush();
                  // hasValidLine = true;

                }
              } else {
                if (!hasError) {
                  EscmCsvBidLinesImport csvImport = OBProvider.getInstance()
                      .get(EscmCsvBidLinesImport.class);
                  csvImport.setEscmBidmgmtLine(null);
                  csvImport.setLineNo(Long.parseLong(lineNo));
                  if (parentLineNo != "")
                    csvImport.setParentlineNo(Long.parseLong(parentLineNo));
                  csvImport.setQtyOrdered(orderQty.longValue());
                  csvImport.setUniquecode(uniqueCode);
                  csvImport.setItem(itemCode);
                  csvImport.setDescription(description);
                  csvImport.setProductCategory(productCategory);
                  csvImport.setUom(uom);
                  if (summary.equals("Y")) {
                    csvImport.setSummaryLevel(true);
                  } else {
                    csvImport.setSummaryLevel(false);
                  }
                  csvImport.setEscmBidmgmt(bid);
                  OBDal.getInstance().save(csvImport);
                  OBDal.getInstance().flush();
                }
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
          // Description Null Error
          if (descriptionMandatory.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_Desc_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(descriptionMandatory.toString());
            hasErrorMessage = true;
          }

          if (qtyDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_Qty_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(qtyDetails.toString());
            hasErrorMessage = true;
          }

          // Throws error when mandatory fields are not filled.
          if (noMandatoryFields) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_LineNo_Mandatory"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          }

          // Quantity can not be zero
          if (qtyDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_PurReq_QtyZero"));
            resultMessage.append("<br>");
            resultMessage.append(qtyDetails.toString());
            hasErrorMessage = true;
          }

          // Unit Price can not be zero
          if (priceDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("EFIN_PenaltyRelAmtGrtZero"));
            resultMessage.append("<br>");
            resultMessage.append(priceDetails.toString());
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

          if (uniqueCodeDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidUniqueCode"));
            resultMessage.append("<br>");
            resultMessage.append(uniqueCodeDetails.toString());
            hasErrorMessage = true;
          }

          // Item code Errors
          if (itemCodeDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidItemCode"));
            resultMessage.append("<br>");
            resultMessage.append(itemCodeDetails.toString());
            hasErrorMessage = true;
          }

          // Product Category Errors
          if (pdtCategoryDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidProductCategory"));
            resultMessage.append("<br>");
            resultMessage.append(pdtCategoryDetails.toString());
            hasErrorMessage = true;
          }

          // Need by date Errors
          if (needByDateDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidNeedbyDate"));
            resultMessage.append("<br>");
            resultMessage.append(needByDateDetails.toString());
            hasErrorMessage = true;
          }

          // UOM Errors
          if (uomDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidUOM"));
            resultMessage.append("<br>");
            resultMessage.append(uomDetails.toString());
            hasErrorMessage = true;
          }

          // National Product Errors
          if (nationalPdtDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_National_Product_value"));
            resultMessage.append("<br>");
            resultMessage.append(nationalPdtDetails.toString());
            hasErrorMessage = true;
          }
          // parentLine Error
          if (parentLineDetails.toString().length() > 0) {
            if (isParentInvalid) {
              resultMessage.append(OBMessageUtils.messageBD("Escm_PO_NoParentLineNumber"));
              resultMessage.append("<br>");
              resultMessage.append(parentLineDetails.toString());
              hasErrorMessage = true;
            } else {
              resultMessage.append(OBMessageUtils.messageBD("Escm_InvalidParentLine"));
              resultMessage.append("<br>");
              resultMessage.append(parentLineDetails.toString());
              hasErrorMessage = true;
            }
          }
          if (changetypeDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_POChangeValCantBeApplied"));
            resultMessage.append("<br>");
            resultMessage.append(changetypeDetails.toString());
            hasErrorMessage = true;
          }
          // Line already exists
          if (lineExistsDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Efin_lineexist"));
            resultMessage.append("<br>");
            resultMessage.append(lineExistsDetails.toString());
            hasErrorMessage = true;
          }

        } else {
          if (noMandatoryFields) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_LineNo_Mandatory"));
            resultMessage.append("<br>");
            hasErrorMessage = true;
          } else if (lineNumberExists) {
            // Line number already exists
            resultMessage.append(OBMessageUtils.messageBD("Efin_lineexist"));
            resultMessage.append("<br>");
            hasErrorMessage = true;

          } else {
            // Invalid Files
            resultMessage.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_InvalidfFile"));
            hasErrorMessage = true;
          }
        }
      }
      if (hasErrorMessage) {
        resultJSON.put("status", "0");
        resultJSON.put("statusMessage", resultMessage.toString());
        OBDal.getInstance().rollbackAndClose();
        log4j.debug("Validation Failed");
      } else {
        resultJSON.put("status", "1");
        resultJSON.put("statusMessage", OBMessageUtils.messageBD("Escm_CSV_ValidatedSuccess"));
        log4j.debug("Validation Success");
      }
    } catch (final Exception e) {
      log4j.error("Exception in ImportPOLines() : ", e);
      e.printStackTrace();
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

}
