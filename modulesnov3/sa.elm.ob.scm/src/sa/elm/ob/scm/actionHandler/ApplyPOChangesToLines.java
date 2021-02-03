package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.db.DalConnectionProvider;

import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ad_callouts.SLOrderAmtData;
import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Kousalya on 06/12/2018
 */

public class ApplyPOChangesToLines extends BaseActionHandler {
  /**
   * This Servlet Class is responsible to Apply
   */
  private static Logger log4j = Logger.getLogger(ApplyPOChangesToLines.class);
  public static final String PO_Window_ID = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
  public static final String PO_HeaderTab_ID = "62248BBBCF644C18A75B92AD8E50238C"; // Header Tab Id
  public static final String PO_LinesTab_ID = "8F35A05BFBB34C34A80E9DEF769613F7"; // Line Tab Id

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    ConnectionProvider connectionProvider = null;
    try {
      connectionProvider = new DalConnectionProvider(true);
      if (data != null) {
        final JSONObject jsonData = new JSONObject(data);
        HttpServletRequest request = RequestContext.get().getRequest();
        VariablesSecureApp vars = new VariablesSecureApp(request);
        String tabId = "", linesId = "";
        int valueSet = 1;// 0-Value Added Successfully, 1-No Lines to add, 2-Value Failed to add,
                         // 3-Values not set in header, 4-New Line Added but already value applied
                         // for
                         // other lines, 5-Validation Error, 6-No line Selected
        boolean taxapplied = false;
        if (parameters.containsKey("action")) {
          final String action = (String) parameters.get("action");
          if ("setChangeValues".equals(action)) {
            if (jsonData.has("tabId")) {
              tabId = jsonData.getString("tabId");
            }
            if (jsonData.has("linesId")) {
              linesId = jsonData.getString("linesId");

              if (linesId != null && !linesId.isEmpty()) {
                JSONArray arr = new JSONArray(linesId);
                String strPricePrecision = "";
                // Header - Apply to all lines
                if (tabId.equals(PO_HeaderTab_ID)) {
                  String headerId = arr.getString(0);
                  Order ord = Utility.getObject(Order.class, headerId);
                  if (ord.getEscmTotPoChangeType() != null && ord.getEscmTotPoChangeFactor() != null
                      && ord.getEscmTotPoChangeValue() != null
                      && ord.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
                    if (ord.getEscmTotPoUpdatedAmt().compareTo(BigDecimal.ZERO) > 0) {
                      ord.setEscmCalculateTaxlines(false);
                      SLOrderAmtData[] currencyPrec = SLOrderAmtData.select(connectionProvider,
                          headerId);
                      if (currencyPrec != null && currencyPrec.length > 0) {
                        strPricePrecision = currencyPrec[0].priceprecision.equals("") ? "0"
                            : currencyPrec[0].priceprecision;
                      }
                      int pricePrecision = Integer.valueOf(strPricePrecision).intValue();
                      // Set values to all lines and calculate net line amt and update header net
                      // total
                      // amt

                      try {
                        // Check change value is greater than gross amount
                        POContractSummaryTotPOChangeDAO.isChangeValueGreater(ord);
                      } catch (Exception e) {
                        throw new OBException(e.getMessage());
                      }
                      try {
                        valueSet = POContractSummaryTotPOChangeDAO.setChangeValueToAllLines(ord,
                            pricePrecision);
                      } catch (Exception e) {
                        throw new OBException(e.getMessage());
                      }
                      if (valueSet != 5) {
                        POContractSummaryTotPOChangeDAO.updatePOLines(headerId); // Update flag po
                                                                                 // change
                                                                                 // value is calc
                                                                                 // for
                                                                                 // all
                                                                                 // lines
                        POContractSummaryTotPOChangeDAO.updateLineTotalAmt(ord.getId());// Update
                                                                                        // line
                                                                                        // net
                                                                                        // amt and
                                                                                        // gross
                                                                                        // line amt
                                                                                        // from
                                                                                        // its
                                                                                        // parent
                                                                                        // id

                        POContractSummaryTotPOChangeDAO.getPOChildLinesAmt(ord.getId());// Get sum
                                                                                        // of
                                                                                        // child
                                                                                        // line
                                                                                        // net
                                                                                        // amt
                        if (ord.getEscmTotalTaxamt().compareTo(BigDecimal.ZERO) > 0) {
                          taxapplied = true;
                        }
                      }
                    } else {
                      valueSet = 5;
                    }
                  } else {
                    valueSet = 3;
                  }
                } // Lines - Apply to selected lines
                else if (tabId.equals(PO_LinesTab_ID)) {
                  if (arr.length() != 0) {
                    String lnId = arr.getString(0);
                    OrderLine ordLn = Utility.getObject(OrderLine.class, lnId);
                    Order ord = Utility.getObject(Order.class, ordLn.getSalesOrder().getId());
                    // if (arr.length() == 1) {
                    // if (ordLn.getEscmLineTotalUpdated().compareTo(BigDecimal.ZERO) == 0)
                    // valueSet = 5;
                    // }
                    if (valueSet != 5) {
                      if (ord.getEscmTotPoChangeType() != null
                          && ord.getEscmTotPoChangeFactor() != null
                          && ord.getEscmTotPoChangeValue() != null
                          && ord.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
                        if (ord.getEscmTotPoUpdatedAmt().compareTo(BigDecimal.ZERO) > 0) {
                          valueSet = 0;
                          List<OrderLine> ordLnLs = POContractSummaryTotPOChangeDAO
                              .getOrderChildLineList(ord.getId());
                          BigDecimal lineChangeValue = BigDecimal.ZERO;
                          ord.setEscmCalculateTaxlines(false);
                          // Get total Amount Lookup Id
                          String totalAmtChangeTypeId = POContractSummaryTotPOChangeDAO
                              .getPOChangeLookUpId("TPOCHGTYP", "01");
                          // Get total Percent Lookup Id
                          String totalPercentChangeTypeId = POContractSummaryTotPOChangeDAO
                              .getPOChangeLookUpId("TPOCHGTYP", "02");
                          // Get Line amount Lookup Id
                          String lineAmtChangeTypeId = POContractSummaryTotPOChangeDAO
                              .getPOChangeLookUpId("POCHGTYP", "01");
                          // Get Line Percent Lookup Id
                          String linePercentChangeTypeId = POContractSummaryTotPOChangeDAO
                              .getPOChangeLookUpId("POCHGTYP", "02");
                          String decFactId = POContractSummaryTotPOChangeDAO
                              .getPOChangeLookUpId("POCHGFACT", "01");
                          String lineChangeTypeId = "";

                          BigDecimal totalRemAmt = POContractSummaryTotPOChangeDAO
                              .totalRemainingAmt(ord);
                          if (ordLnLs != null && ordLnLs.size() > 0) {
                            if (ord.getEscmTotPoChangeType().getId()
                                .equals(totalPercentChangeTypeId)) {
                              lineChangeValue = ord.getEscmTotPoChangeValue();
                              lineChangeTypeId = linePercentChangeTypeId;
                            } else if (ord.getEscmTotPoChangeType().getId()
                                .equals(totalAmtChangeTypeId)) {
                              // get line count and divide with total po change value to get single
                              // line
                              // po
                              // change value
                              // BigDecimal lineCount = new BigDecimal(ordLnLs.size());
                              if (ord.getEscmTotPoChangeFactor() != null
                                  && ord.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
                                if (totalRemAmt.compareTo(BigDecimal.ZERO) > 0)
                                  lineChangeValue = (ord.getEscmTotPoChangeValue()
                                      .divide(totalRemAmt, 20, BigDecimal.ROUND_DOWN));
                              } else {
                                if (ord.getEscmTotPoUpdatedAmt().compareTo(BigDecimal.ZERO) > 0)
                                  lineChangeValue = (ord.getEscmTotPoChangeValue().divide(
                                      ord.getEscmTotPoUpdatedAmt(), 20, BigDecimal.ROUND_DOWN));
                              }
                              lineChangeTypeId = lineAmtChangeTypeId;
                            }
                          }
                          try {
                            log4j.debug("lineChangeValue>" + lineChangeValue);
                            int count = POContractSummaryTotPOChangeDAO
                                .checkAlreadyValuesSet(ord.getId());
                            if (count > 0) {
                              for (int i = 0; i < arr.length(); i++) {
                                String lineId = arr.getString(i);
                                ordLn = Utility.getObject(OrderLine.class, lineId);

                                if (!ordLn.isEscmIsPochgevalcalc()) {
                                  valueSet = 4;
                                  break;
                                }
                              }
                            }
                            if (valueSet != 4) {
                              BigDecimal lnPriceUpdatedAmt = BigDecimal.ZERO;
                              List<String> parentId = new ArrayList<String>();
                              for (int i = 0; i < arr.length(); i++) {
                                BigDecimal changeValue = BigDecimal.ZERO;
                                String lineId = arr.getString(i);
                                ordLn = Utility.getObject(OrderLine.class, lineId);
                                if (ordLn.isEscmIssummarylevel()) {
                                  parentId.add(ordLn.getId());
                                } else {

                                  BigDecimal remAmt = BigDecimal.ZERO;
                                  remAmt = POContractSummaryTotPOChangeDAO.remainingAmtLines(ordLn);

                                  if (ord.getEscmTotPoChangeType().getId()
                                      .equals(totalAmtChangeTypeId)) {

                                    if (ord.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
                                      if (remAmt.compareTo(BigDecimal.ZERO) > 0) {
                                        changeValue = lineChangeValue.multiply(remAmt);
                                      } else {
                                        changeValue = lineChangeValue;
                                      }
                                    } else {
                                      if (ordLn.getEscmLineTotalUpdated()
                                          .compareTo(BigDecimal.ZERO) > 0) {
                                        changeValue = lineChangeValue
                                            .multiply(ordLn.getEscmLineTotalUpdated());
                                      } else {
                                        changeValue = lineChangeValue;
                                      }
                                    }
                                  } else if (ord.getEscmTotPoChangeType().getId()
                                      .equals(totalPercentChangeTypeId)) {
                                    if (ord.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
                                      changeValue = remAmt.multiply(
                                          (lineChangeValue).divide(new BigDecimal("100")));
                                    } else {
                                      changeValue = lineChangeValue;
                                    }
                                  }

                                  log4j.debug("changeValue>" + changeValue);
                                  changeValue = changeValue.setScale(2, BigDecimal.ROUND_HALF_UP);
                                  if (ord.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
                                    if (totalAmtChangeTypeId
                                        .equals(ord.getEscmTotPoChangeType().getId())) {
                                      if (ordLn.getEscmLineTotalUpdated()
                                          .compareTo(changeValue) < 0) {
                                        valueSet = 5;// Validation Error
                                        break;
                                      }
                                    }
                                  }
                                  if (ordLn.getEscmLineTotalUpdated()
                                      .compareTo(BigDecimal.ZERO) > 0) {
                                    ESCMDefLookupsTypeLn lookup = Utility
                                        .getObject(ESCMDefLookupsTypeLn.class, lineChangeTypeId);
                                    ordLn.setEscmPoChangeType(lookup);
                                    ordLn.setEscmPoChangeFactor(ord.getEscmTotPoChangeFactor());

                                    // ordLn.setEscmPoChangeValue(changeValue);
                                    if (ord.getEscmTotPoChangeType().getId()
                                        .equals(totalPercentChangeTypeId)) {
                                      ordLn.setEscmPoChangeValue(lineChangeValue);
                                    } else {
                                      ordLn.setEscmPoChangeValue(changeValue);
                                    }

                                    /*
                                     * Task No. if (ordLn.getEscmLineTotalUpdated()
                                     * .compareTo(BigDecimal.ZERO) > 0) { lnPriceUpdatedAmt =
                                     * POContractSummaryTotPOChangeDAO
                                     * .calculateLineUpdatedAmt(lineChangeTypeId,
                                     * ord.getEscmTotPoChangeFactor().getId(), changeValue,
                                     * ordLn.getEscmLineTotalUpdated());
                                     * ordLn.setLineNetAmount(lnPriceUpdatedAmt); }
                                     */
                                  }
                                  POContractSummaryDAO.updateTaxAndChangeValue(true,
                                      ordLn.getSalesOrder(), ordLn);
                                  /*
                                   * Task No. ordLn.setLineNetAmount(BigDecimal.ZERO); if
                                   * (ordLn.getSalesOrder().getEscmTaxMethod() != null &&
                                   * ordLn.getSalesOrder().isEscmIstax()) { JSONObject taxObject =
                                   * POContractSummaryTotPOChangeDAO
                                   * .calculateTax(ordLn.getOrderedQuantity(), ordLn.getUnitPrice(),
                                   * ordLn.getEscmPoChangeType().getId(), changeValue,
                                   * ordLn.getEscmPoChangeFactor().getId(),
                                   * ordLn.getEscmLineTaxamt(), ordLn.getSalesOrder().getId()); if
                                   * (taxObject.length() > 0) { if (taxObject.has("lineNetAmt")) {
                                   * ordLn.setLineNetAmount( new
                                   * BigDecimal(taxObject.getString("lineNetAmt"))); } if
                                   * (taxObject.has("taxAmount")) { ordLn.setEscmLineTaxamt( new
                                   * BigDecimal(taxObject.getString("taxAmount"))); } if
                                   * (taxObject.has("calGrossPrice")) {
                                   * ordLn.setEscmLineTotalUpdated( new
                                   * BigDecimal(taxObject.getString("calGrossPrice"))); } if
                                   * (taxObject.has("calUnitPrice")) { ordLn.setUnitPrice( new
                                   * BigDecimal(taxObject.getString("calUnitPrice"))); } }
                                   * ordLn.getSalesOrder().setEscmCalculateTaxlines(true); }
                                   */
                                  OBDal.getInstance().save(ordLn);
                                }
                              }
                              try {
                                valueSet = POContractSummaryTotPOChangeDAO.setValuesToChildLines(
                                    parentId, ord.getEscmTotPoChangeType(),
                                    ord.getEscmTotPoChangeFactor(), lineChangeValue,
                                    lineChangeTypeId);
                                OBDal.getInstance().flush();
                              } catch (Exception e) {
                                throw new OBException(e.getMessage());
                              }
                              // calculate net line amt and update header net total amt
                              POContractSummaryTotPOChangeDAO.updatePOLines(ord.getId());// Update
                                                                                         // flag
                                                                                         // po
                                                                                         // changevalue
                                                                                         // is
                                                                                         // calc for
                                                                                         // all
                                                                                         // lines
                              POContractSummaryTotPOChangeDAO.updateLineTotalAmt(ord.getId());// Update
                                                                                              // line
                                                                                              // net
                                                                                              // amt
                                                                                              // and
                                                                                              // gross
                                                                                              // line
                                                                                              // amt
                                                                                              // from
                                                                                              // its
                                                                                              // parent
                                                                                              // id
                              POContractSummaryTotPOChangeDAO.getPOChildLinesAmt(ord.getId());// Get
                                                                                              // sum
                                                                                              // ofchild
                                                                                              // line
                                                                                              // net
                                                                                              // amt
                              if (ord.getEscmTotalTaxamt().compareTo(BigDecimal.ZERO) > 0) {
                                taxapplied = true;
                              }
                            }
                          } catch (OBException e) {
                            OBDal.getInstance().rollbackAndClose();
                            log4j.error("Exception while setChangeValueforSelectedLines:" + e);
                            throw new OBException(e.getMessage());
                          } catch (Exception e) {
                            valueSet = 2;
                            log4j.error("Exception while setChangeValueforSelectedLines:" + e);
                          }
                        } else {
                          valueSet = 5;
                        }
                      } else {
                        valueSet = 3;
                      }
                    }

                  } else
                    valueSet = 6;
                }
              }
            }
          } else if ("getStatus".equals(action)) {
            if (jsonData.has("orderId")) {
              String orderId = jsonData.getString("orderId");
              if (orderId != null && !orderId.equals("null")) {
                Order ord = Utility.getObject(Order.class, orderId);
                if (ord != null) {
                  int isAllowed = POContractSummaryTotPOChangeDAO.checkApplyChangeAllowed(ord,
                      vars.getRole(), vars.getUser());
                  if (isAllowed == 0) {
                    if (ord.getEscmTotPoChangeFactor() != null
                        && ord.getEscmTotPoChangeType() != null
                        && ord.getEscmTotPoChangeValue() != null
                        && ord.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) != 0) {
                      result.put("Message", "Allowed");
                    } else {
                      result.put("Message", "NotAllowed");
                    }
                  } else
                    result.put("Message", "NotAllowed");
                  return result;
                } else {
                  result.put("Message", "NotAllowed");
                  return result;
                }
              } else {
                result.put("Message", "");
                return result;
              }
            }
          }
        }
        log4j.debug("valueSet>" + valueSet);
        if (valueSet == 0) {
          result.put("Message", "Success");
          result.put("taxApplied", taxapplied);
        } else if (valueSet == 1) {
          result.put("Message", "NoLines");
        } else if (valueSet == 2) {
          result.put("Message", "Error");
        } else if (valueSet == 3) {
          result.put("Message", "NoValueSet");
        } else if (valueSet == 4) {
          result.put("Message", "NewLine");
        } else if (valueSet == 5) {
          result.put("Message", "ValidationError");
        } else if (valueSet == 6) {
          result.put("Message", "NoLineSelected");
        } else {
          result.put("Message", "");
        }
      }
      // result.put("Message", valueSet);
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      try {
        result.put("Message", e.getMessage());
      } catch (Exception a) {

      }
    } catch (Exception e) {
      log4j.error("Exception in ApplyPOChangesToLines :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return result;
  }
}