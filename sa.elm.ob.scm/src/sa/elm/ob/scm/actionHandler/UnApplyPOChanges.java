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
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

import sa.elm.ob.scm.actionHandler.dao.POContractSummaryUnApplyDAO;
import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Kousalya on 25/03/2019
 */

public class UnApplyPOChanges extends BaseActionHandler {
  /**
   * This Servlet Class is responsible to UnApply PO Changes
   */
  private static Logger log4j = Logger.getLogger(ApplyPOChangesToLines.class);
  public static final String PO_Window_ID = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
  public static final String PO_HeaderTab_ID = "62248BBBCF644C18A75B92AD8E50238C"; // Header Tab Id
  public static final String PO_LinesTab_ID = "8F35A05BFBB34C34A80E9DEF769613F7"; // Line Tab Id

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {
      if (data != null) {
        final JSONObject jsonData = new JSONObject(data);
        HttpServletRequest request = RequestContext.get().getRequest();
        VariablesSecureApp vars = new VariablesSecureApp(request);
        String tabId = "", linesId = "";
        int valueSet = 1;// 0-Value Added Successfully, 1-No Lines to add, 2-Value Failed to add,
                         // 3-Values not set in header, 5-Validation Error, 6-No line Selected

        if (parameters.containsKey("action")) {
          final String action = (String) parameters.get("action");
          if ("unapply".equals(action)) {
            if (jsonData.has("tabId")) {
              tabId = jsonData.getString("tabId");
            }
            if (jsonData.has("linesId")) {
              linesId = jsonData.getString("linesId");

              if (linesId != null && !linesId.isEmpty()) {
                JSONArray arr = new JSONArray(linesId);
                // Header - Apply to all lines
                if (tabId.equals(PO_HeaderTab_ID)) {
                  String headerId = arr.getString(0);
                  Order ord = Utility.getObject(Order.class, headerId);
                  if (ord.getEscmTotPoChangeType() != null && ord.getEscmTotPoChangeFactor() != null
                      && ord.getEscmTotPoChangeValue() != null
                      && ord.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
                    if (ord.getEscmTotPoUpdatedAmt().compareTo(BigDecimal.ZERO) > 0) {
                      ord.setEscmCalculateTaxlines(false);
                      // Unapply values to all lines and calculate net line amt and update header
                      // net
                      // total amt

                      valueSet = POContractSummaryUnApplyDAO.unApplyChangeValueToAllLines(ord);
                      ord.setEscmTotPoChangeType(null);
                      ord.setEscmTotPoChangeFactor(null);
                      ord.setEscmTotPoChangeValue(BigDecimal.ZERO);
                      OBDal.getInstance().save(ord);
                      if (valueSet != 5) {
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

                    if (valueSet != 5) {
                      valueSet = 0;
                      ord.setEscmCalculateTaxlines(false);
                      try {
                        List<String> parentId = new ArrayList<String>();
                        for (int i = 0; i < arr.length(); i++) {
                          String lineId = arr.getString(i);
                          ordLn = Utility.getObject(OrderLine.class, lineId);
                          if (ordLn.isEscmIssummarylevel()) {
                            parentId.add(ordLn.getId());
                          } else {
                            ordLn.setEscmPoChangeType(null);
                            ordLn.setEscmPoChangeFactor(null);
                            ordLn.setEscmPoChangeValue(BigDecimal.ZERO);
                            ordLn.setEscmLineTotalUpdated(
                                ordLn.getOrderedQuantity().multiply(ordLn.getUnitPrice()));
                            ordLn.setLineNetAmount(
                                ordLn.getOrderedQuantity().multiply(ordLn.getUnitPrice()));
                            OBDal.getInstance().save(ordLn);
                          }
                        }
                        valueSet = POContractSummaryUnApplyDAO
                            .unApplyChangeValuesToChildLines(parentId);
                        OBDal.getInstance().flush();
                        // calculate net line amt and update header net total amt
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

                      } catch (Exception e) {
                        valueSet = 2;
                        log4j.error("Exception while unapplyChangeValueforSelectedLines:" + e);
                      }
                    }
                  } else
                    valueSet = 6;
                }
              }
            }
          } else if ("getStatus".equals(action)) {
            if (jsonData.has("orderId")) {
              if (jsonData.has("tabId")) {
                tabId = jsonData.getString("tabId");
              }
              String orderId = jsonData.getString("orderId");
              if (orderId != null && !orderId.equals("null")) {
                Order ord = Utility.getObject(Order.class, orderId);
                if (ord != null) {
                  int isAllowed = POContractSummaryTotPOChangeDAO.checkApplyChangeAllowed(ord,
                      vars.getRole(), vars.getUser());
                  if (isAllowed == 0) {
                    if (tabId.equals(PO_HeaderTab_ID)) {
                      if (ord.getEscmTotPoChangeFactor() != null
                          && ord.getEscmTotPoChangeType() != null
                          && ord.getEscmTotPoChangeValue() != null
                          && ord.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) != 0) {
                        result.put("Message", "Allowed");
                      } else {
                        result.put("Message", "NotAllowed");
                      }
                    } else if (tabId.equals(PO_LinesTab_ID)) {
                      int lineEnable = 0;
                      if (jsonData.has("linesId")) {
                        linesId = jsonData.getString("linesId");

                        if (linesId != null && !linesId.isEmpty()) {
                          JSONArray arr = new JSONArray(linesId);
                          if (arr.length() != 0) {
                            for (int i = 0; i < arr.length(); i++) {
                              String lineId = arr.getString(i);
                              OrderLine ordLn = Utility.getObject(OrderLine.class, lineId);
                              if (ordLn.getEscmPoChangeFactor() != null
                                  && ordLn.getEscmPoChangeType() != null
                                  && ordLn.getEscmPoChangeValue() != null
                                  && ordLn.getEscmPoChangeValue().compareTo(BigDecimal.ZERO) != 0) {
                                lineEnable = 1;
                                break;
                              } else {
                                lineEnable = 0;
                              }
                            }
                          }
                        }
                      }
                      if (lineEnable == 0) {
                        result.put("Message", "NotAllowed");
                      } else if (lineEnable == 1) {
                        result.put("Message", "Allowed");
                      }
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
    } catch (Exception e) {
      log4j.error("Exception in UnApplyPOChangesToLines :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return result;
  }
}