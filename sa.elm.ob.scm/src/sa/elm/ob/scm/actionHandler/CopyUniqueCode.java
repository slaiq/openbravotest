package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;

import sa.elm.ob.scm.ESCMProposalEvlEvent;
import sa.elm.ob.scm.EscmBidMgmt;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmbidmgmtline;
import sa.elm.ob.scm.actionHandler.dao.CopyUniqueCodeDao;
import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;
import sa.elm.ob.scm.ad_process.Requisition.RequisitionDao;

/**
 * @author Poongodi on 06/01/2018
 */

public class CopyUniqueCode extends BaseActionHandler {
  /**
   * This Servlet Class is responsible to Apply All Line UniqueCode in purchase requisition,Bid
   * Management,Proposal Management,Purchase Order and Contracts Summary
   */
  private static Logger log4j = Logger.getLogger(CopyUniqueCode.class);
  public static final String PO_Window_ID = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
  public static final String Proposal_Window_ID = "CAF2D3EEF3B241018C8F65E8F877B29F";
  public static final String ProposalEvlEvent_Tab_ID = "9B284558C7E149B0AC245D610F8BC2F6";

  public static final String PR_WINDOW_ID = "800092";

  protected JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    try {

      OBContext.setAdminMode();

      final JSONObject jsonData = new JSONObject(data);
      HttpServletRequest request = RequestContext.get().getRequest();
      VariablesSecureApp vars = new VariablesSecureApp(request);
      String recordId = "", budgetController = "", uniqueCodeId = null, uniquecodeName = "",
          tabId = "";
      Boolean isDistributed = false;
      Boolean isEncumActive = false;

      // Purchase Requisition
      if (parameters != null && parameters.containsKey("Type")
          && parameters.get("Type").equals("PR")) {
        if (parameters.containsKey("PurchaseReqId")) {
          recordId = (String) parameters.get("PurchaseReqId");
        }
        if (jsonData.has("action")) {
          final String action = jsonData.getString("action");
          if ("setDistributeAll".equals(action) && !recordId.isEmpty()) {

            Requisition req = OBDal.getInstance().get(Requisition.class, recordId);
            if (req.getEFINUniqueCode() != null) {
              uniqueCodeId = req.getEFINUniqueCode().getId();
              if (uniqueCodeId != null) {
                AccountingCombination uniquecode = OBDal.getInstance()
                    .get(AccountingCombination.class, uniqueCodeId);
                uniquecodeName = uniquecode.getEfinUniquecodename();

              }

              for (RequisitionLine objLines : req.getProcurementRequisitionLineList()) {
                if (!objLines.isEscmIssummary()) {
                  objLines.setEfinCValidcombination(
                      OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
                  objLines.setEfinUniquecodename(uniquecodeName);
                }
                isDistributed = true;
              }
            } else {
              result.put("Message", "NoUniqueCode");
              return result;
            }
            if (isDistributed) {
              result.put("Message", "Success");
            } else {
              result.put("Message", "NoLines");
            }
          }

          if ("getDistributeFlag".equals(action) && !recordId.isEmpty()) {

            Requisition req = OBDal.getInstance().get(Requisition.class, recordId);

            // check role is BudgetController or not
            try {
              budgetController = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
                  vars.getClient(), req.getOrganization().getId(), vars.getUser(), vars.getRole(),
                  PR_WINDOW_ID);
            } catch (PropertyException e) {
              budgetController = "N";
            } catch (Exception e) {
              budgetController = "N";

            }

            if (budgetController != null && budgetController.equals("Y")) {
              result.put("isBudgetContrl", "Y");
            } else {
              result.put("isBudgetContrl", "N");

            }
          }

        }
      }

      // Bid Management
      else if (parameters != null && parameters.containsKey("Type")
          && parameters.get("Type").equals("BID")) {
        if (parameters.containsKey("BidmgmtId")) {
          recordId = (String) parameters.get("BidmgmtId");
        }
        if (jsonData.has("action")) {
          String action = jsonData.getString("action");

          if ("setDistributeAll".equals(action) && !recordId.isEmpty()) {

            EscmBidMgmt bid = OBDal.getInstance().get(EscmBidMgmt.class, recordId);
            if (bid.getEFINUniqueCode() != null) {
              uniqueCodeId = bid.getEFINUniqueCode().getId();
              if (uniqueCodeId != null) {
                AccountingCombination uniquecode = OBDal.getInstance()
                    .get(AccountingCombination.class, uniqueCodeId);
                uniquecodeName = uniquecode.getEfinUniquecodename();

              }

              for (Escmbidmgmtline objLines : bid.getEscmBidmgmtLineList()) {
                if (!objLines.isSummarylevel()) {
                  objLines.setAccountingCombination(
                      OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
                  objLines.setUniquecodename(uniquecodeName);
                }
                isDistributed = true;
              }
            }
            if (isDistributed) {
              result.put("Message", "Success");
            } else {
              result.put("Message", "NoLines");
            }
          }
        }
      }

      // Proposal Management
      else if (parameters != null && parameters.containsKey("Type")
          && parameters.get("Type").equals("PRO")) {
        if (parameters.containsKey("ProposalId")) {
          recordId = (String) parameters.get("ProposalId");
        }
        if (jsonData.has("action")) {
          String action = jsonData.getString("action");

          if ("setDistributeAll".equals(action) && !recordId.isEmpty()) {

            EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class, recordId);
            if (proposal.getEFINUniqueCode() != null) {
              uniqueCodeId = proposal.getEFINUniqueCode().getId();
              if (uniqueCodeId != null) {
                AccountingCombination uniquecode = OBDal.getInstance()
                    .get(AccountingCombination.class, uniqueCodeId);
                uniquecodeName = uniquecode.getEfinUniquecodename();

              }

              for (EscmProposalmgmtLine objLines : proposal.getEscmProposalmgmtLineList()) {
                if (!objLines.isSummary()) {
                  objLines.setEFINUniqueCode(
                      OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
                  objLines.setEFINUniqueCodeName(uniquecodeName);
                }
                if (proposal.getEfinBudgetinitial() != null) {
                  objLines.setEFINFundsAvailable(RequisitionDao.getAutoEncumFundsAvailable(
                      uniqueCodeId, proposal.getEfinBudgetinitial().getId()));
                }
                isDistributed = true;
              }
            } else {
              result.put("Message", "NoUniqueCode");
              return result;
            }
            if (isDistributed) {
              result.put("Message", "Success");
            } else {
              result.put("Message", "NoLines");
            }
          }
          if ("getuniquecode".equals(action) && !recordId.isEmpty()) {

            EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
                recordId);
            if (proposalmgmt != null && proposalmgmt.getEFINUniqueCode() != null) {
              result.put("Message", "Success");
            }
            // check role is BudgetController or not
            try {
              budgetController = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
                  vars.getClient(),
                  proposalmgmt == null ? vars.getOrg() : proposalmgmt.getOrganization().getId(),
                  vars.getUser(), vars.getRole(), Proposal_Window_ID);
            } catch (PropertyException e) {
              budgetController = "N";
            } catch (Exception e) {
              log4j.error("Exception in isBudgetControllerRole :", e);
              OBDal.getInstance().rollbackAndClose();
              throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
            }

            if (budgetController != null && budgetController.equals("Y")) {
              result.put("isBudgetContrl", "Yes");
            }
          }
        }
      }

      // Proposal Attributes
      else if (parameters != null && parameters.containsKey("Type")
          && parameters.get("Type").equals("PROP_ATTR")) {
        if (parameters.containsKey("ProposalAttrId")) {
          recordId = (String) parameters.get("ProposalAttrId");
        }
        if (jsonData.has("action")) {
          String action = jsonData.getString("action");

          if ("setDistributeAll".equals(action) && !recordId.isEmpty()) {

            EscmProposalAttribute propAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
                recordId);
            EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
                propAttr.getEscmProposalmgmt().getId());

            if (propAttr.getEFINUniqueCode() != null) {
              uniqueCodeId = propAttr.getEFINUniqueCode().getId();
              if (uniqueCodeId != null) {
                AccountingCombination uniquecode = OBDal.getInstance()
                    .get(AccountingCombination.class, uniqueCodeId);
                uniquecodeName = uniquecode.getEfinUniquecodename();

              }

              for (EscmProposalmgmtLine objLines : proposal.getEscmProposalmgmtLineList()) {
                if (!objLines.isSummary()) {
                  objLines.setEFINUniqueCode(
                      OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
                  objLines.setEFINUniqueCodeName(uniquecodeName);
                }
                if (propAttr.getEfinBudgetinitial() != null) {
                  objLines.setEFINFundsAvailable(RequisitionDao.getAutoEncumFundsAvailable(
                      uniqueCodeId, proposal.getEfinBudgetinitial().getId()));
                }
                isDistributed = true;
              }
            } else {
              result.put("Message", "NoUniqueCode");
              return result;
            }
            if (isDistributed) {
              result.put("Message", "Success");
            } else {
              result.put("Message", "NoLines");
            }
          }
          if ("getuniquecode".equals(action) && !recordId.isEmpty()) {

            EscmProposalAttribute propAttr = OBDal.getInstance().get(EscmProposalAttribute.class,
                recordId);

            // EscmProposalMgmt proposalmgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
            // propAttr.getEscmProposalmgmt().getId());

            if (propAttr != null && propAttr.getEFINUniqueCode() != null) {
              result.put("Message", "Success");
            }
            // check role is BudgetController or not
            try {
              budgetController = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
                  vars.getClient(),
                  propAttr == null ? vars.getOrg() : propAttr.getOrganization().getId(),
                  vars.getUser(), vars.getRole(), ProposalEvlEvent_Tab_ID);
            } catch (PropertyException e) {
              budgetController = "N";
            } catch (Exception e) {
              log4j.error("Exception in isBudgetControllerRole :", e);
              OBDal.getInstance().rollbackAndClose();
              throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
            }

            if (budgetController != null && budgetController.equals("Y")) {
              result.put("isBudgetContrl", "Yes");
            }

            ESCMProposalEvlEvent propEvl = propAttr.getEscmProposalevlEvent();

            if (propEvl != null && propEvl.isPartialaward() && propEvl.isAwardfullqty()) {
              result.put("isPartialAwardFullQty", "Y");
            } else {
              result.put("isPartialAwardFullQty", "N");
            }
          }
        }
      }

      // Purchase Order and Contracts Summary
      else if (parameters != null && parameters.containsKey("Type")
          && parameters.get("Type").equals("PO")) {
        boolean isChild = false;
        if (parameters.containsKey("POId")) {
          recordId = (String) parameters.get("POId");
        }

        if (parameters.containsKey("tabId")) {
          tabId = (String) parameters.get("tabId");
        }
        if (jsonData.has("action")) {
          String action = jsonData.getString("action");

          if ("setDistributeAll".equals(action) && !recordId.isEmpty()) {
            JSONArray arr = new JSONArray(recordId);
            AccountingCombination uniquecode = null;
            if (!tabId.isEmpty()) {
              if (tabId.equals("62248BBBCF644C18A75B92AD8E50238C")) {
                Order pocontsum = OBDal.getInstance().get(Order.class, arr.getString(0));
                if (pocontsum.getEFINUniqueCode() != null) {
                  uniqueCodeId = pocontsum.getEFINUniqueCode().getId();
                  if (uniqueCodeId != null) {
                    uniquecode = OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId);
                    uniquecodeName = uniquecode.getEfinUniquecodename();

                  }

                  for (OrderLine objLines : pocontsum.getOrderLineList()) {
                    if (!objLines.isEscmIssummarylevel()) {
                      if (objLines.getOrderedQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        if (objLines.getEscmOldOrderline() == null) {
                          result.put("Message", "QtyIsZero");
                          return result;
                        }
                      }
                    }
                    if (!objLines.isEscmIssummarylevel()) {
                      objLines.setEFINUniqueCode(
                          OBDal.getInstance().get(AccountingCombination.class, uniqueCodeId));
                      objLines.setEFINUniqueCodeName(uniquecodeName);
                    }
                    if (pocontsum.getEfinBudgetint() != null) {
                      objLines.setEFINFundsAvailable(RequisitionDao.getAutoEncumFundsAvailable(
                          uniqueCodeId, pocontsum.getEfinBudgetint().getId()));
                    }
                    isDistributed = true;
                  }
                }

                else {
                  result.put("Message", "NoUniqueCode");
                  return result;
                }
                if (isDistributed) {
                  result.put("Message", "Success");
                } else {
                  result.put("Message", "NoLines");
                }
              }
              if (tabId.equals("8F35A05BFBB34C34A80E9DEF769613F7")) {
                if (arr.length() > 0) {
                  for (int i = 0; i < arr.length(); i++) {
                    OrderLine pocontLine = OBDal.getInstance().get(OrderLine.class,
                        arr.getString(i));
                    // if (pocontLine.getEFINUniqueCode() != null) {
                    // uniqueCodeId = pocontLine.getEFINUniqueCode().getId();
                    // } else
                    //
                    if (!pocontLine.isEscmIssummarylevel()) {
                      if (pocontLine.getOrderedQuantity().compareTo(BigDecimal.ZERO) == 0) {
                        if (pocontLine.getEscmOldOrderline() == null) {
                          result.put("Message", "QtyIsZero");
                          return result;
                        }
                      }
                    }
                    if (pocontLine.getSalesOrder().getEFINUniqueCode() != null) {
                      uniqueCodeId = pocontLine.getSalesOrder().getEFINUniqueCode().getId();
                    }
                    if (uniqueCodeId != null) {
                      uniquecode = OBDal.getInstance().get(AccountingCombination.class,
                          uniqueCodeId);
                      uniquecodeName = uniquecode.getEfinUniquecodename();

                      if (pocontLine.isEscmIssummarylevel()) {
                        isChild = POContractSummaryTotPOChangeDAO
                            .getChildForSelectedParent(pocontLine.getId(), uniquecode);

                        isDistributed = isChild;
                      } else {
                        pocontLine.setEFINUniqueCode(uniquecode);
                        pocontLine.setEFINUniqueCodeName(uniquecodeName);
                        isDistributed = true;
                      }

                    }

                    else {
                      result.put("Message", "NoUniqueCode");
                      return result;
                    }
                  }
                  if (isDistributed) {
                    result.put("Message", "Success");
                  } else {
                    result.put("Message", "NoLines");
                  }
                }
              }

            }
          }

          if ("getuniquecode".equals(action) && !recordId.isEmpty()) {
            Order pocontsum = null;
            OrderLine pocontLine = null;
            Organization organisation = null;
            if (!tabId.isEmpty()) {
              if (tabId.equals("62248BBBCF644C18A75B92AD8E50238C")) {
                pocontsum = OBDal.getInstance().get(Order.class, recordId);
                if (pocontsum != null) {
                  organisation = pocontsum.getOrganization();
                  if (pocontsum.getEFINUniqueCode() != null)
                    result.put("Message", "Success");
                }
              } else if (tabId.equals("8F35A05BFBB34C34A80E9DEF769613F7")) {
                pocontLine = OBDal.getInstance().get(OrderLine.class, recordId);
                if (pocontLine != null) {
                  organisation = pocontLine.getSalesOrder().getOrganization();
                  if (pocontLine.getEFINUniqueCode() != null)
                    result.put("Message", "Success");
                  if (pocontLine.getSalesOrder().getEFINUniqueCode() != null)
                    result.put("Message", "Success");
                }
              }
              // check role is BudgetController or not
              try {
                if (pocontsum != null || pocontLine != null) {
                  budgetController = Preferences.getPreferenceValue("ESCM_BudgetControl", true,
                      vars.getClient(), organisation.getId(), vars.getUser(), vars.getRole(),
                      PO_Window_ID);
                }
              } catch (PropertyException e) {
                budgetController = "N";
              } catch (Exception e) {
                log4j.error("Exception in isBudgetControllerRole :", e);
                OBDal.getInstance().rollbackAndClose();
                throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
              }

              if (budgetController != null && budgetController.equals("Y")) {
                result.put("isBudgetContrl", "Yes");
              }
              // is maintain encum is enabled for PO.
              isEncumActive = CopyUniqueCodeDao.isPoEncumEnabled();
              if (isEncumActive) {
                result.put("isEncumActive", "Yes");
              }
              // purchase agreement is checked or not
              if ((pocontsum != null && pocontsum.getEscmOrdertype().equals("PUR_AG")
                  && pocontsum.isEscmIspurchaseagreement())
                  || (pocontLine != null
                      && pocontLine.getSalesOrder().getEscmOrdertype().equals("PUR_AG")
                      && pocontLine.getSalesOrder().isEscmIspurchaseagreement())) {
                result.put("ispurchaseAgreement", "Yes");
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in CopyUniqueCode :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }
}
