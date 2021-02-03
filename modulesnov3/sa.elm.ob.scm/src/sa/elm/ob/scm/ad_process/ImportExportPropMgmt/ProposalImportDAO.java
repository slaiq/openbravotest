package sa.elm.ob.scm.ad_process.ImportExportPropMgmt;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;

import sa.elm.ob.scm.ESCMProductCategoryV;
import sa.elm.ob.scm.ESCMProposalMgmtLineV;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.ProposalLinesImport;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;
import sa.elm.ob.scm.event.dao.ProposalManagementLineEventDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Kiruthika on 01/06/2020
 *
 */

public class ProposalImportDAO {

  private static final Logger log4j = Logger.getLogger(ProposalImportDAO.class);
  @SuppressWarnings("unused")
  private Connection conn = null;
  DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
  Date currentDate = new Date();
  String formattedCurrentDateStr = dateFormat.format(currentDate);

  public ProposalImportDAO(Connection con) {
    this.conn = con;
  }

  public JSONObject processUploadedCsvFile(String inpProposalId, VariablesSecureApp vars) {
    JSONObject jsonresult = new JSONObject();
    int isSuccess = 0;
    EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, inpProposalId);
    try {
      OBContext.setAdminMode(true);

      isSuccess = UpdateProposalLines(proposal);

      log4j.debug("processUploadedCsvFile isSuccess" + isSuccess);

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

  private int UpdateProposalLines(EscmProposalMgmt proposal) throws Exception {
    try {

      OBQuery<ProposalLinesImport> csvImportQry = OBDal.getInstance().createQuery(
          ProposalLinesImport.class,
          " as e where e.escmProposalmgmt.id = :proposalId order by e.creationDate ");
      csvImportQry.setNamedParameter("proposalId", proposal.getId());
      List<ProposalLinesImport> csvImportList = csvImportQry.list();
      HashMap<Long, Long> parentLineMap = new HashMap<>();

      for (ProposalLinesImport csvImportLne : csvImportList) {

        EscmProposalmgmtLine parentId = null;
        String productId = null;
        String productCategoryId = null;
        ESCMProductCategoryV category = null;
        String uomId = null;
        String uniqueCodeId = null;
        Product product = null;
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
        BigDecimal netPrice = BigDecimal.ZERO;
        BigDecimal negUnitPrice = BigDecimal.ZERO;
        BigDecimal netUnitPrice = BigDecimal.ZERO;
        BigDecimal unitPriceAfChng = BigDecimal.ZERO;
        BigDecimal lineTotal = BigDecimal.ZERO;
        BigDecimal discAmt = BigDecimal.ZERO;
        BigDecimal discPer = BigDecimal.ZERO;

        if (csvImportLne.getEscmProposalmgmtLine() != null) {
          EscmProposalmgmtLine proposalLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
              csvImportLne.getEscmProposalmgmtLine().getId());
          if (proposalLine != null) {
            proposalLine.setLineNo(csvImportLne.getLineNo());
            if (csvImportLne.getParentlineNo() != null) {
              parentId = getParent(csvImportLne.getParentlineNo(), proposal.getId());
              if (parentId != null) {
                ESCMProposalMgmtLineV parentLineV = OBDal.getInstance()
                    .get(ESCMProposalMgmtLineV.class, parentId.getId());
                proposalLine.setParentLineNo(parentLineV);
              } else {
                proposalLine.setParentLineNo(null);
                parentLineMap.put(csvImportLne.getLineNo(), csvImportLne.getParentlineNo());
              }
            } else {
              proposalLine.setParentLineNo(null);
            }
            if (csvImportLne.getItemCode() != null && csvImportLne.getItemCode() != "") {
              productId = getProductId(csvImportLne.getItemCode());
              if (productId != null) {
                product = OBDal.getInstance().get(Product.class, productId);
                proposalLine.setProduct(product);
              }
            } else {
              proposalLine.setProduct(null);
            }
            if (csvImportLne.getItemCode() != null && csvImportLne.getItemCode() != ""
                && product != null) {
              proposalLine.setDescription(product.getName());
            } else {
              if (csvImportLne.getDescription() != null) {
                proposalLine.setDescription(csvImportLne.getDescription());
              }
            }

            if (csvImportLne.getProductCategory() != null
                && csvImportLne.getProductCategory() != "") {
              if (product != null && product.getProductCategory() != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                    product.getProductCategory().getId());
                proposalLine.setProductCategory(category);

              } else {
                productCategoryId = getProductCategory(csvImportLne.getProductCategory());
                if (productCategoryId != null) {
                  category = OBDal.getInstance().get(ESCMProductCategoryV.class, productCategoryId);
                  proposalLine.setProductCategory(category);
                }
              }
            } else {
              if (product != null && product.getProductCategory() != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                    product.getProductCategory().getId());
                proposalLine.setProductCategory(category);
              }
            }
            if (csvImportLne.getUom() != null) {
              if (product != null && product.getUOM() != null) {
                proposalLine.setUOM(product.getUOM());
              } else {
                uomId = getUOM(csvImportLne.getUom());
                if (uomId != null)
                  proposalLine.setUOM(OBDal.getInstance().get(UOM.class, uomId));
              }
            }
            if (csvImportLne.getEscmProposalQty() != null) {
              proposalLine.setMovementQuantity(csvImportLne.getEscmProposalQty());
              qty = csvImportLne.getEscmProposalQty();
            }
            if (csvImportLne.getGrossUnitprice() != null
                && csvImportLne.getNEGUnitprice() != null) {

              unitPrice = csvImportLne.getGrossUnitprice();
              netPrice = unitPrice;// Net unit price

              if (proposal.getProposalstatus().equals("SUB")
                  || proposal.getProposalstatus().equals("OPE")) {
                negUnitPrice = unitPrice;
              } else {
                if (csvImportLne.getNEGUnitprice().compareTo(BigDecimal.ZERO) == 0) {
                  negUnitPrice = unitPrice;
                } else {
                  negUnitPrice = csvImportLne.getNEGUnitprice();
                }
              }

              netUnitPrice = negUnitPrice;// Net unit price (final)
              unitPriceAfChng = negUnitPrice;// Unit price after change

              lineTotal = negUnitPrice.multiply(qty);

              if (unitPrice.compareTo(negUnitPrice) != 0) {

                discAmt = (unitPrice.multiply(qty)).subtract((negUnitPrice).multiply(qty));
                discPer = (discAmt.divide(unitPrice.multiply(qty), 2, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal(100));

              } else {
                if (csvImportLne.getDiscount() != null
                    && csvImportLne.getDiscount().compareTo(BigDecimal.ZERO) != 0) {
                  discPer = csvImportLne.getDiscount();
                } else if (proposal.getDiscountForTheDeal() != null
                    && proposal.getDiscountForTheDeal().compareTo(BigDecimal.ZERO) > 0) {
                  discPer = proposal.getDiscountForTheDeal();
                } else {
                  discPer = BigDecimal.ZERO;
                }
                if (discPer.compareTo(BigDecimal.ZERO) != 0) {
                  discAmt = (discPer.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP))
                      .multiply(lineTotal);
                  lineTotal = lineTotal.subtract(discAmt);
                  negUnitPrice = negUnitPrice
                      .subtract(discAmt.divide(qty, 2, RoundingMode.HALF_UP));
                  netUnitPrice = negUnitPrice;
                  unitPriceAfChng = negUnitPrice;
                }
              }

              proposalLine.setDiscount(discPer);
              proposalLine.setDiscountmount(discAmt);
              proposalLine.setTaxAmount(BigDecimal.ZERO);
              proposalLine.setUnittax(BigDecimal.ZERO);
              proposalLine.setGrossUnitPrice(unitPrice);
              proposalLine.setNegotUnitPrice(negUnitPrice);
              proposalLine.setUnitpricedis(unitPriceAfChng);
              proposalLine.setNetprice(netPrice);
              proposalLine.setNetUnitprice(netUnitPrice);
              proposalLine.setLineTotal(lineTotal);
            }

            proposalLine.setNotprovided(csvImportLne.isNotProvided());
            proposalLine.setSummary(csvImportLne.isSummaryLevel());

            if (csvImportLne.getComments() != null) {
              proposalLine.setComments(csvImportLne.getComments());
            }
            if (csvImportLne.getNationalProduct() != null) {
              proposalLine.setNationalproduct(csvImportLne.getNationalProduct());
            }
            if (csvImportLne.getUniquecode() != null) {
              uniqueCodeId = getUniqueCodeId(csvImportLne.getUniquecode());

              // updating unique code name and funds available along with unique code
              if (uniqueCodeId != null) {
                AccountingCombination dimension = OBDal.getInstance()
                    .get(AccountingCombination.class, uniqueCodeId);
                BigDecimal fundsAvailable = RequisitionDao.getAutoEncumFundsAvailable(uniqueCodeId,
                    proposalLine.getEscmProposalmgmt().getEfinBudgetinitial().getId());
                proposalLine.setEFINUniqueCode(
                    OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
                proposalLine.setEFINUniqueCodeName(dimension.getEfinUniquecodename());
                proposalLine.setEFINFundsAvailable(fundsAvailable);
              } else {
                proposalLine.setEFINUniqueCode(null);
                proposalLine.setEFINUniqueCodeName(null);
                proposalLine.setEFINFundsAvailable(BigDecimal.ZERO);
              }

            }
            // updating the callout field
            proposalLine.setBaselineQuantity(qty);
            proposalLine.setTechLineQty(qty);
            if (!csvImportLne.isNotProvided())
              proposalLine.setTechUnitPrice(negUnitPrice);
            else
              proposalLine.setTechUnitPrice(BigDecimal.ZERO);
            proposalLine.setTechLineTotal(lineTotal);
            if (!proposal.getProposalstatus().equals("DR"))
              proposalLine.setTEENetUnitprice(netUnitPrice);
            OBDal.getInstance().save(proposalLine);
          }
        } else {
          // insert new proposal line
          EscmProposalMgmt objProposal = csvImportLne.getEscmProposalmgmt();

          EscmProposalmgmtLine objProposalLine = OBProvider.getInstance()
              .get(EscmProposalmgmtLine.class);
          objProposalLine.setClient(objProposal.getClient());
          objProposalLine.setOrganization(objProposal.getOrganization());
          objProposalLine.setCreationDate(new java.util.Date());
          objProposalLine.setCreatedBy(objProposal.getCreatedBy());
          objProposalLine.setUpdated(new java.util.Date());
          objProposalLine.setUpdatedBy(objProposal.getUpdatedBy());
          objProposalLine.setActive(true);
          objProposalLine.setEscmProposalmgmt(objProposal);

          objProposalLine.setLineNo(csvImportLne.getLineNo());
          objProposalLine.setManual(true);

          if (csvImportLne.getParentlineNo() != null) {
            parentId = getParent(csvImportLne.getParentlineNo(), proposal.getId());
            if (parentId != null) {
              ESCMProposalMgmtLineV parentLineV = OBDal.getInstance()
                  .get(ESCMProposalMgmtLineV.class, parentId.getId());
              objProposalLine.setParentLineNo(parentLineV);
            } else {
              objProposalLine.setParentLineNo(null);
              parentLineMap.put(csvImportLne.getLineNo(), csvImportLne.getParentlineNo());
            }
          } else {
            objProposalLine.setParentLineNo(null);
          }

          if (csvImportLne.getItemCode() != null && csvImportLne.getItemCode() != "") {
            productId = getProductId(csvImportLne.getItemCode());
            if (productId != null) {
              product = OBDal.getInstance().get(Product.class, productId);
              objProposalLine.setProduct(product);
            }
          } else {
            objProposalLine.setProduct(null);
          }

          if (csvImportLne.getItemCode() != null && csvImportLne.getItemCode() != ""
              && product != null) {
            objProposalLine.setDescription(product.getName());
          } else {
            if (csvImportLne.getDescription() != null) {
              objProposalLine.setDescription(csvImportLne.getDescription());
            }
          }

          if (csvImportLne.getProductCategory() != null
              && csvImportLne.getProductCategory() != "") {
            if (product != null && product.getProductCategory() != null) {
              category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                  product.getProductCategory().getId());
              objProposalLine.setProductCategory(category);

            } else {
              productCategoryId = getProductCategory(csvImportLne.getProductCategory());
              if (productCategoryId != null) {
                category = OBDal.getInstance().get(ESCMProductCategoryV.class, productCategoryId);
                objProposalLine.setProductCategory(category);
              }
            }
          } else {
            if (product != null && product.getProductCategory() != null) {
              category = OBDal.getInstance().get(ESCMProductCategoryV.class,
                  product.getProductCategory().getId());
              objProposalLine.setProductCategory(category);
            }
          }

          if (csvImportLne.getUom() != null) {
            if (product != null && product.getUOM() != null) {
              objProposalLine.setUOM(product.getUOM());
            } else {
              uomId = getUOM(csvImportLne.getUom());
              if (uomId != null)
                objProposalLine.setUOM(OBDal.getInstance().get(UOM.class, uomId));
            }
          }

          if (csvImportLne.getEscmProposalQty() != null) {
            objProposalLine.setMovementQuantity(csvImportLne.getEscmProposalQty());
            qty = csvImportLne.getEscmProposalQty();
          }

          if (csvImportLne.getGrossUnitprice() != null && csvImportLne.getNEGUnitprice() != null) {

            unitPrice = csvImportLne.getGrossUnitprice();
            netPrice = unitPrice;// Net unit price

            if (proposal.getProposalstatus().equals("SUB")
                || proposal.getProposalstatus().equals("OPE")) {
              negUnitPrice = unitPrice;
            } else {
              if (csvImportLne.getNEGUnitprice().compareTo(BigDecimal.ZERO) == 0) {
                negUnitPrice = unitPrice;
              } else {
                negUnitPrice = csvImportLne.getNEGUnitprice();
              }
            }

            netUnitPrice = negUnitPrice;// Net unit price (final)
            unitPriceAfChng = negUnitPrice;// Unit price after change

            lineTotal = negUnitPrice.multiply(qty);

            if (unitPrice.compareTo(negUnitPrice) != 0) {

              discAmt = (unitPrice.multiply(qty)).subtract((negUnitPrice).multiply(qty));
              discPer = (discAmt.divide(unitPrice.multiply(qty), 2, RoundingMode.HALF_UP))
                  .multiply(new BigDecimal(100));

            } else {
              if (csvImportLne.getDiscount() != null
                  && csvImportLne.getDiscount().compareTo(BigDecimal.ZERO) != 0) {
                discPer = csvImportLne.getDiscount();
              } else if (proposal.getDiscountForTheDeal() != null
                  && proposal.getDiscountForTheDeal().compareTo(BigDecimal.ZERO) > 0) {
                discPer = proposal.getDiscountForTheDeal();
              } else {
                discPer = BigDecimal.ZERO;
              }
              if (discPer.compareTo(BigDecimal.ZERO) != 0) {
                discAmt = (discPer.divide(new BigDecimal(100), 2, RoundingMode.HALF_UP))
                    .multiply(lineTotal);
                lineTotal = lineTotal.subtract(discAmt);
                negUnitPrice = negUnitPrice.subtract(discAmt.divide(qty, 2, RoundingMode.HALF_UP));
                netUnitPrice = negUnitPrice;
                unitPriceAfChng = negUnitPrice;
              }
            }

            objProposalLine.setDiscount(discPer);
            objProposalLine.setDiscountmount(discAmt);
            objProposalLine.setTaxAmount(BigDecimal.ZERO);
            objProposalLine.setUnittax(BigDecimal.ZERO);
            objProposalLine.setGrossUnitPrice(unitPrice);
            objProposalLine.setNegotUnitPrice(negUnitPrice);
            objProposalLine.setUnitpricedis(unitPriceAfChng);
            objProposalLine.setNetprice(netPrice);
            objProposalLine.setNetUnitprice(netUnitPrice);
            objProposalLine.setLineTotal(lineTotal);
          }

          objProposalLine.setNotprovided(csvImportLne.isNotProvided());

          if (csvImportLne.getNationalProduct() != null) {
            objProposalLine.setNationalproduct(csvImportLne.getNationalProduct());
          }

          objProposalLine.setSummary(csvImportLne.isSummaryLevel());

          if (csvImportLne.getUniquecode() != null) {
            uniqueCodeId = getUniqueCodeId(csvImportLne.getUniquecode());

            // updating unique code name and funds available along with unique code
            if (uniqueCodeId != null) {
              AccountingCombination dimension = OBDal.getInstance().get(AccountingCombination.class,
                  uniqueCodeId);
              BigDecimal fundsAvailable = RequisitionDao.getAutoEncumFundsAvailable(uniqueCodeId,
                  objProposalLine.getEscmProposalmgmt().getEfinBudgetinitial().getId());
              objProposalLine.setEFINUniqueCode(
                  OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
              objProposalLine.setEFINUniqueCodeName(dimension.getEfinUniquecodename());
              objProposalLine.setEFINFundsAvailable(fundsAvailable);
            } else {
              objProposalLine.setEFINUniqueCode(null);
              objProposalLine.setEFINUniqueCodeName(null);
              objProposalLine.setEFINFundsAvailable(BigDecimal.ZERO);
            }
          }

          if (csvImportLne.getComments() != null) {
            objProposalLine.setComments(csvImportLne.getComments());
          }
          // updating the callout field
          objProposalLine.setBaselineQuantity(qty);
          objProposalLine.setTechLineQty(qty);
          if (!csvImportLne.isNotProvided())
            objProposalLine.setTechUnitPrice(negUnitPrice);
          else
            objProposalLine.setTechUnitPrice(BigDecimal.ZERO);
          objProposalLine.setTechLineTotal(lineTotal);
          if (!proposal.getProposalType().equals("DR"))
            objProposalLine.setTEENetUnitprice(netUnitPrice);
          OBDal.getInstance().save(objProposalLine);
        }
      }
      OBDal.getInstance().flush();

      // Update parent line id
      for (Long lineNo : parentLineMap.keySet()) {
        OBQuery<EscmProposalmgmtLine> lineQry = OBDal.getInstance().createQuery(
            EscmProposalmgmtLine.class,
            " as e where e.lineNo = :lineNo and e.escmProposalmgmt.id = :proposalId ");
        lineQry.setNamedParameter("lineNo", lineNo);
        lineQry.setNamedParameter("proposalId", proposal.getId());
        List<EscmProposalmgmtLine> lineList = lineQry.list();
        if (lineList.size() > 0) {
          EscmProposalmgmtLine line = lineList.get(0);

          EscmProposalmgmtLine parentLine = getParent(parentLineMap.get(lineNo), proposal.getId());
          if (parentLine != null) {
            ESCMProposalMgmtLineV parentLineV = OBDal.getInstance().get(ESCMProposalMgmtLineV.class,
                parentLine.getId());
            line.setParentLineNo(parentLineV);
            OBDal.getInstance().flush();
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Error in ProposalImportDAO.java ", e);
      throw new Exception(e.getMessage());
    }
    return 1;
  }

  public JSONObject processValidateCsvFile(File file, VariablesSecureApp vars, String inpProposalId,
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
      StringBuffer intialUnitPriceChangeBeOEEDetails = new StringBuffer();
      StringBuffer proposalLineNotExistsDetails = new StringBuffer();
      StringBuffer uniqueCodeDetails = new StringBuffer();
      StringBuffer itemCodeDetails = new StringBuffer();
      StringBuffer pdtCategoryDetails = new StringBuffer();
      StringBuffer needByDateDetails = new StringBuffer();
      StringBuffer uomDetails = new StringBuffer();
      StringBuffer parentLineDetails = new StringBuffer();
      StringBuffer qtyDetails = new StringBuffer();
      StringBuffer priceDetails = new StringBuffer();
      StringBuffer nationalPdtDetails = new StringBuffer();
      StringBuffer lineExistsDetails = new StringBuffer();
      StringBuffer discountPerDetails = new StringBuffer();
      StringBuffer descriptionMandatory = new StringBuffer();
      StringBuffer intialUnitPriceMandatory = new StringBuffer();
      StringBuffer NegUnitPriceMandatory = new StringBuffer();

      boolean noMandatoryFields = false;
      boolean hasErrorMessage = false;
      boolean isParentInvalid = false;
      boolean lineNumberExists = false;
      boolean isLineNoBlank = false;
      EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, inpProposalId);

      if (!proposal.getProposalstatus().equals("DR") && !proposal.getProposalstatus().equals("SUB")
          && !proposal.getProposalstatus().equals("OPE")) {
        resultJSON.put("status", "0");
        resultJSON.put("statusMessage", OBMessageUtils.messageBD("Escm_CantImportExcel"));
        log4j.debug("Validation Failed");
        return resultJSON;
      }

      // Get Work Book
      inputStream = new FileInputStream(file);
      xssfWorkbook = new XSSFWorkbook(inputStream);

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
                if (update != null && update.equals("escm_proposalmgmt_line_id")) {
                  isUpdateSheet = true;
                }
              }
            }

            int startRow = 1;

            // Get Each Row
            for (int i = startRow;; i++) {
              boolean hasError = false;
              Row row = null;
              Cell cell = null;
              String proposalLineId = null;

              // Initialize Data to be inserted in temporary table
              String cellVal = "";
              String lineNo = "";
              String parentLineNo = "";
              String itemCode = "";
              String description = "";
              String productCategory = "";
              String uom = "";
              BigDecimal proposalQty = BigDecimal.ZERO;
              BigDecimal initialUnitPrice = BigDecimal.ZERO;
              BigDecimal negUnitPrice = BigDecimal.ZERO;
              BigDecimal netUnitPrice = BigDecimal.ZERO;
              BigDecimal discountPer = BigDecimal.ZERO;
              BigDecimal discountAmt = BigDecimal.ZERO;
              BigDecimal lineAmount = BigDecimal.ZERO;
              boolean isNotProvided = false;
              String nationalPro = "";
              boolean isSummary = false;
              String uniqueCode = "";
              String comments = "";

              EscmProposalmgmtLine proposalLne = null;

              row = sheet.getRow(i);

              if (row == null)
                break;

              // OrderLine
              cell = row.getCell(0);
              if (cell != null) {
                proposalLineId = Utility.nullToEmpty(getCellValue(cell));
                proposalLne = OBDal.getInstance().get(EscmProposalmgmtLine.class, proposalLineId);

                if (proposalLne != null) {
                  if (!checkValidProposalLines(proposalLineId, inpProposalId)) {
                    resultMessage.append(OBMessageUtils.messageBD("Escm_DifferentProposalLines"));
                    hasErrorMessage = true;
                    break;
                  }
                } else {
                  String lineNumber = getCellValue(row.getCell(1));
                  String message = OBMessageUtils.messageBD("Escm_ProposalLineNotExists");
                  message = message.replace("%", lineNumber);
                  resultMessage.append(message);
                  hasErrorMessage = true;
                  break;
                }
              } else {
                proposalLne = null;
              }

              if (StringUtils.isEmpty(row.getCell(1).toString())
                  && StringUtils.isEmpty(row.getCell(2).toString())
                  && StringUtils.isEmpty(row.getCell(3).toString())
                  && StringUtils.isEmpty(row.getCell(4).toString())
                  && StringUtils.isEmpty(row.getCell(5).toString())
                  && StringUtils.isEmpty(row.getCell(6).toString())
                  && StringUtils.isEmpty(row.getCell(7).toString())
                  && StringUtils.isEmpty(row.getCell(8).toString())
                  && StringUtils.isEmpty(row.getCell(9).toString())
                  && StringUtils.isEmpty(row.getCell(10).toString())
                  && StringUtils.isEmpty(row.getCell(11).toString())
                  && StringUtils.isEmpty(row.getCell(12).toString())
                  && StringUtils.isEmpty(row.getCell(13).toString())
                  && StringUtils.isEmpty(row.getCell(14).toString())
                  && StringUtils.isEmpty(row.getCell(15).toString())
                  && StringUtils.isEmpty(row.getCell(16).toString())
                  && StringUtils.isEmpty(row.getCell(17).toString())
                  && StringUtils.isEmpty(row.getCell(18).toString()))
                break;
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
                      boolean isLineExists = isValidParentLine(cellVal, proposal.getId());
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
                } else if (!chkisnegative(cellVal)) {
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
                cellVal = cell.toString();// getCellValue(cell);
                if (cellVal != null && cellVal != "" && !isValidPdtCategory(cellVal)) {
                  pdtCategoryDetails
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  pdtCategoryDetails.append("<br>");
                  hasError = true;
                } else {
                  productCategory = cellVal;
                }
              }

              // UOM
              cell = row.getCell(6);
              if (cell != null) {
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
              cell = row.getCell(16);
              if (cell != null) {
                cellVal = getCellValue(cell);
                isSummary = (cell.toString().equals("Y")) ? true : false;
              }

              // Qty Ordered
              cell = row.getCell(7);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(cellVal)) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    proposalQty = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
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

              // Initial Unit Price
              cell = row.getCell(8);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(cellVal)) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    initialUnitPrice = new BigDecimal(cellVal).setScale(2,
                        BigDecimal.ROUND_HALF_UP);

                    if (initialUnitPrice.compareTo(BigDecimal.ZERO) > 0) {
                      if (proposal.getEscmBidmgmt() != null
                          && !proposal.getEscmBidmgmt().getBidtype().equals("DR")) {
                        if (!ProposalManagementLineEventDAO
                            .chkopenvelopcomornot(proposal.getEscmBidmgmt().getId())) {
                          intialUnitPriceChangeBeOEEDetails
                              .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                                  + lineNo + " ->  " + cellVal);
                          intialUnitPriceChangeBeOEEDetails.append("<br>");
                        }
                      }
                    }

                  }

                } else {
                  intialUnitPriceMandatory
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  intialUnitPriceMandatory.append("<br>");
                  hasError = true;
                }
              } else {
                intialUnitPriceMandatory
                    .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                        + " ->  " + cellVal);
                intialUnitPriceMandatory.append("<br>");
                hasError = true;
              }

              // Negotiated Unit Price
              cell = row.getCell(9);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(cellVal)) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    negUnitPrice = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                  }

                } else {
                  NegUnitPriceMandatory
                      .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                          + " ->  " + cellVal);
                  NegUnitPriceMandatory.append("<br>");
                  hasError = true;
                }
              }
              // Net Unit Price
              cell = row.getCell(10);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(cellVal)) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    netUnitPrice = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                  }

                }
              }

              // Discount Percentage
              cell = row.getCell(11);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(cellVal)) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    discountPer = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                    BigDecimal hundred = new BigDecimal(100);
                    if (discountPer.compareTo(hundred) > 0) {
                      discountPerDetails
                          .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                              + lineNo + " ->  " + cellVal);
                      discountPerDetails.append("<br>");
                      hasError = true;
                    }
                  }
                }
              }

              // Discount Amt
              cell = row.getCell(12);
              if (cell != null) {
                cellVal = getCellValue(cell);
                if (cellVal != null && cellVal != "") {
                  if (!isNumeric(cellVal)) {
                    numericErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    numericErrorDetails.append("<br>");
                    hasError = true;
                  } else if (!chkisnegative(cellVal)) {
                    negativeErrorDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    negativeErrorDetails.append("<br>");
                    hasError = true;
                  } else {
                    discountAmt = new BigDecimal(cellVal).setScale(2, BigDecimal.ROUND_HALF_UP);
                  }
                }
              }

              // Not provided **
              cell = row.getCell(14);
              if (cell != null) {
                cellVal = getCellValue(cell);
                isNotProvided = (cell.toString().equals("Y")) ? true : false;
                if (isNotProvided && proposal.getEscmBidmgmt() != null) {
                  initialUnitPrice = BigDecimal.ZERO;
                  negUnitPrice = BigDecimal.ZERO;
                  netUnitPrice = BigDecimal.ZERO;
                }
              }

              // nationalProduct
              cell = row.getCell(15);
              if (cell != null) {
                cellVal = getCellValue(cell);
                nationalPro = cellVal;
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
                  if (cellVal != null && cellVal != ""
                      && (Long.parseLong(cellVal) > 100 || Long.parseLong(cellVal) == 0)) {
                    nationalPdtDetails
                        .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo") + lineNo
                            + " ->  " + cellVal);
                    nationalPdtDetails.append("<br>");
                    hasError = true;
                  } else {
                    nationalPro = cellVal;
                  }
                }
              }

              // Unique code
              cell = row.getCell(17);
              cellVal = getCellValue(cell);

              if (cellVal != "" && !isValidUniqueCode(cellVal)) {
                uniqueCodeDetails.append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_LineNo")
                    + lineNo + " ->  " + cellVal);
                uniqueCodeDetails.append("<br>");
                hasError = true;
              } else {
                uniqueCode = cellVal;
              }

              // Comments
              cell = row.getCell(18);
              if (cell != null) {
                cellVal = getCellValue(cell);
                comments = cellVal;
              }

              if (proposalLne != null) {

                // If no error, insert into temporary table
                if (!hasError) {
                  ProposalLinesImport csvImport = OBProvider.getInstance()
                      .get(ProposalLinesImport.class);
                  csvImport.setEscmProposalmgmtLine(proposalLne);
                  csvImport.setEscmProposalmgmt(proposal);

                  csvImport.setLineNo(Long.parseLong(lineNo));
                  if (parentLineNo != "")
                    csvImport.setParentlineNo(Long.parseLong(parentLineNo));

                  csvImport.setItemCode(itemCode);
                  csvImport.setDescription(description);
                  csvImport.setProductCategory(productCategory);
                  csvImport.setUom(uom);

                  csvImport.setEscmProposalQty(proposalQty);
                  csvImport.setGrossUnitprice(initialUnitPrice);
                  csvImport.setNEGUnitprice(negUnitPrice);
                  csvImport.setNetUnitprice(netUnitPrice);
                  csvImport.setDiscount(discountPer);
                  csvImport.setDiscountAmount(discountAmt);
                  csvImport.setLinetotal(lineAmount);
                  csvImport.setNotProvided(isNotProvided);

                  if (nationalPro != "")
                    csvImport.setNationalProduct(Long.parseLong(nationalPro));

                  csvImport.setSummaryLevel(isSummary);
                  csvImport.setUniquecode(uniqueCode);
                  csvImport.setComments(comments);

                  OBDal.getInstance().save(csvImport);
                  OBDal.getInstance().flush();

                }
              } else {
                if (!hasError) {
                  ProposalLinesImport csvImport = OBProvider.getInstance()
                      .get(ProposalLinesImport.class);
                  // csvImport.setEscmProposalmgmtLine(proposalLne);
                  csvImport.setEscmProposalmgmt(proposal);

                  csvImport.setLineNo(Long.parseLong(lineNo));
                  if (parentLineNo != "")
                    csvImport.setParentlineNo(Long.parseLong(parentLineNo));

                  csvImport.setItemCode(itemCode);
                  csvImport.setDescription(description);
                  csvImport.setProductCategory(productCategory);
                  csvImport.setUom(uom);

                  csvImport.setEscmProposalQty(proposalQty);
                  csvImport.setGrossUnitprice(initialUnitPrice);
                  csvImport.setNEGUnitprice(negUnitPrice);
                  csvImport.setNetUnitprice(netUnitPrice);
                  csvImport.setDiscount(discountPer);
                  csvImport.setDiscountAmount(discountAmt);
                  csvImport.setLinetotal(lineAmount);
                  csvImport.setNotProvided(isNotProvided);

                  if (nationalPro != "")
                    csvImport.setNationalProduct(Long.parseLong(nationalPro));

                  csvImport.setSummaryLevel(isSummary);
                  csvImport.setUniquecode(uniqueCode);
                  csvImport.setComments(comments);

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
          // Initial Unit Price Null Error
          if (intialUnitPriceMandatory.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_intunitprice_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(intialUnitPriceMandatory.toString());
            hasErrorMessage = true;
          }

          // Initial Unit Price Change before OEE completion Error
          if (intialUnitPriceChangeBeOEEDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("ESCM_CantUpGrsPriceProsal"));
            resultMessage.append("<br>");
            resultMessage.append(intialUnitPriceChangeBeOEEDetails.toString());
            hasErrorMessage = true;
          }

          // Negotiated Unit Price Null Error
          if (NegUnitPriceMandatory.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_NegUnitPrice_Mandatory"));
            resultMessage.append("<br>");
            resultMessage.append(NegUnitPriceMandatory.toString());
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

          // Order Line Errors
          if (proposalLineNotExistsDetails.toString().length() > 0) {
            resultMessage
                .append(OBMessageUtils.messageBD("Escm_Import_IORecpt_Err_InvalidOrderLine"));
            resultMessage.append("<br>");
            resultMessage.append(proposalLineNotExistsDetails.toString());
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
          // Line already exists
          if (lineExistsDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Efin_lineexist"));
            resultMessage.append("<br>");
            resultMessage.append(lineExistsDetails.toString());
            hasErrorMessage = true;
          }

          if (discountPerDetails.toString().length() > 0) {
            resultMessage.append(OBMessageUtils.messageBD("Escm_DiscountPerGreater"));
            resultMessage.append("<br>");
            resultMessage.append(discountPerDetails.toString());
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
      log4j.error("Exception in ProposalImportDAO : ", e);
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
      OBQuery<ProposalLinesImport> csvImportQry = OBDal.getInstance()
          .createQuery(ProposalLinesImport.class, "order by creationDate");
      List<ProposalLinesImport> csvImportList = csvImportQry.list();
      for (ProposalLinesImport csvimprt : csvImportList) {
        OBDal.getInstance().remove(csvimprt);
      }
    } catch (final Exception e) {
      log4j.error("Exception in deleteCSVImportEntries() : ", e);
    }
  }

  private boolean checkValidProposalLines(String proposalLineId, String proposalId) {
    boolean result = false;
    try {

      EscmProposalmgmtLine line = OBDal.getInstance().get(EscmProposalmgmtLine.class,
          proposalLineId);

      if (proposalId.equals(line.getEscmProposalmgmt().getId())) {
        result = true;
      }
      return result;
    } catch (final Exception e) {
      log4j.error("Exception in checkValidProposalLines() : ", e);
      return result;
    }
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
      log4j.error("Exception in chkisnull method in ProposalImportDAO.java", e);
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
      log4j.error("Exception in chkisnegative method in ProposalImportDAO.java", e);

    }
    return true;
  }

  public static boolean isValidUniqueCode(String uniqueCode) {
    boolean isValidUniquecode = false;
    try {
      if (!uniqueCode.equals("") && !uniqueCode.isEmpty()) {
        OBQuery<AccountingCombination> uniqueCodeQry = OBDal.getInstance()
            .createQuery(AccountingCombination.class, "efinUniqueCode = :uniqueCode");
        uniqueCodeQry.setNamedParameter("uniqueCode", uniqueCode);
        List<AccountingCombination> uniqueCodeList = uniqueCodeQry.list();
        if (uniqueCodeList.size() > 0) {
          isValidUniquecode = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidUniqueCode method in ProposalImportDAO.java", e);
      return isValidUniquecode;
    }
    return isValidUniquecode;
  }

  public static EscmProposalmgmtLine getParent(Long lineNo, String proposalId) {
    EscmProposalmgmtLine proposalLine = null;
    try {
      OBQuery<EscmProposalmgmtLine> parentLine = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class,
          " as e where e.lineNo = :lineNo and e.escmProposalmgmt.id = :proposalId ");
      parentLine.setNamedParameter("lineNo", lineNo);
      parentLine.setNamedParameter("proposalId", proposalId);

      List<EscmProposalmgmtLine> parentLinesList = parentLine.list();
      if (parentLinesList != null && parentLinesList.size() > 0) {
        proposalLine = parentLinesList.get(0);
      }
    } catch (Exception e) {
      log4j.error("Exception in getParent", e);
    }
    return proposalLine;
  }

  public static String getProductId(String itemCode) {
    String productId = null;
    try {

      if (!itemCode.equals("") && itemCode != null) {
        // String[] item = itemCode.split("-", 2);

        OBQuery<Product> itemCodeQry = OBDal.getInstance().createQuery(Product.class,
            "trim(searchKey) = :code ");// and trim(name) = :name
        itemCodeQry.setNamedParameter("code", itemCode);// item[0].trim()
        // itemCodeQry.setNamedParameter("name", item[1].trim());
        List<org.openbravo.model.common.plm.Product> itemCodeList = itemCodeQry.list();
        if (itemCodeList.size() > 0) {
          productId = itemCodeList.get(0).getId();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getProductId method in ProposalImportDAO.java", e);
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
      log4j.error("Exception in getUOM method in ProposalImportDAO.java", e);
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
      log4j.error("Exception in getUniqueCodeId method in ProposalImportDAO.java", e);
      return uniquecodeId;
    }
    return uniquecodeId;
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
      log4j.error("Exception in isValidItemCode method in ProposalImportDAO.java", e);
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
      log4j.error("Exception in isValidPdtCategory method in ProposalImportDAO.java", e);
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
      log4j.error("Exception in isValidUOM method in ProposalImportDAO.java", e);
      return isValidUOM;
    }
    return isValidUOM;
  }

  public static boolean isValidParentLine(String parentLineNo, String proposalId) {
    boolean isValidParentLine = false;
    try {
      if (!parentLineNo.equals("") && parentLineNo != null) {
        OBQuery<EscmProposalmgmtLine> parentLineQry = OBDal.getInstance().createQuery(
            EscmProposalmgmtLine.class,
            "as e where e.lineNo = :lineNo and e.escmProposalmgmt.id = :proposalId ");
        parentLineQry.setNamedParameter("lineNo", Long.parseLong(parentLineNo));
        parentLineQry.setNamedParameter("proposalId", proposalId);
        List<EscmProposalmgmtLine> parentLineList = parentLineQry.list();
        if (parentLineList.size() > 0) {
          isValidParentLine = true;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in isValidParentLine method in ProposalImportDAO.java", e);
      return isValidParentLine;
    }
    return isValidParentLine;
  }

  public boolean changeType(OrderLine orderLne, BigDecimal lineNetAmt) {
    boolean isGreaterThanValue = false;
    try {
      if (orderLne != null) {
        if (orderLne.getEscmPoChangeFactor() != null && orderLne.getEscmPoChangeValue() != null) {
          if (orderLne.getEscmPoChangeValue().compareTo(lineNetAmt) > 0) {
            isGreaterThanValue = true;
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in changeType method in ProposalImportDAO.java", e);
      return isGreaterThanValue;
    }
    return isGreaterThanValue;
  }
}