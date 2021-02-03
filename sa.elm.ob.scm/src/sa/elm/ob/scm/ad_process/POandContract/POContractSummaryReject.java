package sa.elm.ob.scm.ad_process.POandContract;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;

import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.scm.ad_process.ProposalManagement.ProposalManagementRejectMethods;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * 
 * @author gopalakrishnan on 01/08/2017
 *
 */
public class POContractSummaryReject implements Process {
  /**
   * This Servlet Class responsible to reject records
   */
  private final OBError obError = new OBError();
  private static Logger log = Logger.getLogger(POContractSummaryReject.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    // TODO Auto-generated method stub
    // Connection connection = null;
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    String Lang = vars.getLanguage();

    final String strOrderId = (String) bundle.getParams().get("C_Order_ID").toString();

    Order objOrder = OBDal.getInstance().get(Order.class, strOrderId);

    final String clientId = (String) bundle.getContext().getClient();
    final String orgId = objOrder.getOrganization().getId();
    final String userId = (String) bundle.getContext().getUser();
    final String roleId = (String) bundle.getContext().getRole();
    String comments = (String) bundle.getParams().get("comments").toString();
    Order headerId = null;
    String appstatus = "", alertWindow = AlertWindow.PurchaseOrderContract;
    ArrayList<String> includeRecipient = new ArrayList<String>();
    boolean fromPR = false, fromProposal = false;
    JSONObject resultEncum = null;
    EfinBudgetManencum encumbrance = null;
    // log.debug("comments " + comments + ", role Id:" + roleId + ", User Id:" + userId);
    // Connection conn = OBDal.getInstance().getConnection();
    boolean errorFlag = true;
    boolean allowUpdate = false, allowReject = false;
    boolean allowDelegation = false;
    String errorMsg = "", alertRuleId = "";
    Date CurrentDate = new Date();
    int count = 0;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    List<EfinBudgetManencumlines> encumLinesList = null;
    boolean checkEncumbranceAmountZero = false;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    EfinBudgetManencum encum = null;
    String windowId = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";
    EutForwardReqMoreInfo forwardObj = objOrder.getEutForward();
    try {
      OBContext.setAdminMode(true);
      Order header = OBDal.getInstance().get(Order.class, strOrderId);
      String documentRule = null;

      if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
        documentRule = Resource.PURCHASE_AGREEMENT_RULE;
      } else {
        documentRule = Resource.PURCHASE_ORDER_RULE;
      }

      Boolean isEncumbered = objOrder.isEfinEncumbered();
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = POContractSummaryDAO.getEncControleList(objOrder);
      // End Task No.5925

      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // reject the record without refreshing the page
      if (objOrder.getEutForward() != null) {
        allowReject = forwardReqMoreInfoDAO.allowApproveReject(objOrder.getEutForward(), userId,
            roleId, documentRule);
      }
      if (objOrder.getEutReqmoreinfo() != null
          || ((objOrder.getEutForward() != null) && (!allowReject))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // Task No.5925
      if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")) {
        encumbrance = header.getEfinBudgetManencum();

        if (objOrder.getEscmBaseOrder() != null && objOrder.isEfinEncumbered()
            && objOrder.getEscmOldOrder().getEfinBudgetManencum() != null) {
          if (header.getEfinBudgetManencum() != null
              && header.getEfinBudgetManencum().getEncumMethod().equals("M")) {
            errorFlag = POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                objOrder.getEscmBaseOrder(), true, true, null);
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              bundle.setResult(result);
              return;
            } else {
              POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                  objOrder.getEscmBaseOrder(), false, true, null);
              errorFlag = true;
            }
          } else {
            JSONObject object = POContractSummaryDAO.getUniquecodeListforPOVerAuto(objOrder,
                objOrder.getEscmBaseOrder(), true, null);
            // funds validation.
            errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                objOrder.getEfinBudgetint(), "PO", false);
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
              bundle.setResult(result);
              return;
            } else {
              POContractSummaryDAO.doRejectPOVersionMofifcationInEncumbrance(objOrder,
                  objOrder.getEscmBaseOrder(), false, null);
              errorFlag = true;
            }
          }
        } else {
          // check from proposal line added case:
          if (objOrder.getEscmProposalmgmt() == null) {
            // check lines added from pr
            OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(OrderLine.class,
                "salesOrder.id=:orderID and efinMRequisitionline.id is not null");
            orderLine.setNamedParameter("orderID", objOrder.getId());

            if (orderLine.list() != null && orderLine.list().size() > 0) {
              fromPR = true;
            }
          } else {
            fromProposal = true;
          }

          // if after budget control, try to reject then check funds for negative impacts.
          if (header.isEfinEncumbered()) {
            OBInterceptor.setPreventUpdateInfoChange(true);

            // get encum line list

            OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance()
                .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id=:encumID");
            encumLines.setNamedParameter("encumID", header.getEfinBudgetManencum().getId());
            if (encumLines.list() != null && encumLines.list().size() > 0) {
              encumLinesList = encumLines.list();
            }
            // validation
            errorFlag = POContractSummaryDAO.checkFundsForReject(header, encumLinesList);
            log.debug("errorFlag:" + errorFlag);
            if (errorFlag) {

              if (!fromPR && !fromProposal) {

                // manual encum
                if (header.getEfinBudgetManencum().getEncumMethod().equals("M")) {
                  // update amount
                  POContractSummaryDAO.updateManualEncumAmountRej(header, encumLinesList, false,
                      "");

                  // Check Encumbrance Amount is Zero Or Negative
                  if (header.getEfinBudgetManencum() != null)
                    encumLinelist = header.getEfinBudgetManencum().getEfinBudgetManencumlinesList();
                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZero = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (checkEncumbranceAmountZero) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_Encumamt_Neg@");
                    bundle.setResult(result);
                    return;
                  }

                  header.setEfinEncumbered(false);
                  header.getEfinBudgetManencum().setBusinessPartner(null);
                  OBDal.getInstance().save(header);

                }
                // auto encumbrance
                else {
                  POContractSummaryDAO.updateAmtInEnquiryRej(header.getId(), encumLinesList, false,
                      "");

                  // Check Encumbrance Amount is Zero Or Negative
                  if (header.getEfinBudgetManencum() != null)
                    encumLinelist = header.getEfinBudgetManencum().getEfinBudgetManencumlinesList();
                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZero = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (checkEncumbranceAmountZero) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_Encumamt_Neg@");
                    bundle.setResult(result);
                    return;
                  }
                  // remove encum
                  encum = header.getEfinBudgetManencum();
                  encum.setDocumentStatus("DR");
                  header.setEfinBudgetManencum(null);
                  header.setEfinEncumbered(false);
                  OBDal.getInstance().save(header);
                  // remove encum reference in lines.
                  List<OrderLine> ordLine = header.getOrderLineList();
                  for (OrderLine ordLineList : ordLine) {
                    ordLineList.setEfinBudEncumlines(null);
                    OBDal.getInstance().save(ordLineList);
                  }
                  OBDal.getInstance().flush();
                  OBDal.getInstance().remove(encum);
                }
              } else if (fromPR) {
                // reactivate the merge and splitencumbrance
                resultEncum = POContractSummaryDAO.checkFullPRQtyUitlizeorNot(header);

                // if full qty only used then remove the encumbrance reference and change the
                // encumencumbrance stage as PR Stage
                if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                    && resultEncum.getBoolean("isAssociatePREncumbrance")
                    && resultEncum.has("isFullQtyUsed")
                    && resultEncum.getBoolean("isFullQtyUsed")) {
                  encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                      resultEncum.getString("encumbrance"));
                  errorFlag = POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(header,
                      encumbrance, true, true);
                  log.debug("errorFlag:" + errorFlag);

                  // Check Encumbrance Amount is Zero Or Negative
                  if (header.getEfinBudgetManencum() != null)
                    encumLinelist = header.getEfinBudgetManencum().getEfinBudgetManencumlinesList();
                  if (encumLinelist.size() > 0)
                    checkEncumbranceAmountZero = UtilityDAO
                        .checkEncumbranceAmountZero(encumLinelist);

                  if (checkEncumbranceAmountZero) {
                    OBDal.getInstance().rollbackAndClose();
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_Encumamt_Neg@");
                    bundle.setResult(result);
                    return;
                  }
                  if (errorFlag) {
                    OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_ProcessFailed(Reason)@");
                    bundle.setResult(result1);
                    return;
                  } else {
                    encumbrance.setEncumStage("PRE");

                    POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(header, encumbrance,
                        false, true);

                    if (header.getEfinBudgetManencum() != null)
                      header.getEfinBudgetManencum().setBusinessPartner(null);
                    header.setEfinBudgetManencum(null);
                    header.setEfinEncumbered(false);
                    OBDal.getInstance().save(header);
                    OBDal.getInstance().save(encumbrance);

                    errorFlag = true;
                  }
                }
                // if pr is skip the encumbrance
                else if (resultEncum.has("isAssociatePREncumbrance")
                    && !resultEncum.getBoolean("isAssociatePREncumbrance")) {

                  errorFlag = POContractSummaryDAO.checkFundsForReject(header, encumLinesList);
                  if (errorFlag) {
                    POContractSummaryDAO.updateAmtInEnquiryRej(header.getId(), encumLinesList,
                        false, "");

                    // Check Encumbrance Amount is Zero Or Negative
                    if (objOrder.getEfinBudgetManencum() != null)
                      encumLinelist = objOrder.getEfinBudgetManencum()
                          .getEfinBudgetManencumlinesList();
                    if (encumLinelist.size() > 0)
                      checkEncumbranceAmountZero = UtilityDAO
                          .checkEncumbranceAmountZero(encumLinelist);

                    if (checkEncumbranceAmountZero) {
                      OBDal.getInstance().rollbackAndClose();
                      OBError result = OBErrorBuilder.buildMessage(null, "error",
                          "@ESCM_Encumamt_Neg@");
                      bundle.setResult(result);
                      return;
                    }
                    // remove encum
                    if (header.getEfinBudgetManencum() != null) {
                      encum = header.getEfinBudgetManencum();
                      encum.setDocumentStatus("DR");
                      header.setEfinBudgetManencum(null);
                      header.setEfinEncumbered(false);
                      OBDal.getInstance().save(header);
                      // remove encum reference in lines.
                      List<OrderLine> ordLine = header.getOrderLineList();
                      for (OrderLine ordLineList : ordLine) {
                        ordLineList.setEfinBudEncumlines(null);
                        OBDal.getInstance().save(ordLineList);
                      }
                      OBDal.getInstance().flush();
                      OBDal.getInstance().remove(encum);
                    }
                  } else {
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@Efin_Encum_Used_Cannot_Rej@");
                    bundle.setResult(result);
                    return;
                  }
                }
                // if full qty not used / manual encumbrance remaining amount and applied amount
                // will not match / one or more encumbrance used in PO
                else {
                  errorFlag = POContractSummaryDAO.chkFundsAvailforReactOldEncumbrance(header,
                      null);
                  if (errorFlag) {
                    OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_ProcessFailed(Reason)@");
                    bundle.setResult(result1);
                    return;
                  } else {
                    if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                      POContractSummaryDAO.reactivateSplitPR(resultEncum, header);
                    }
                    if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                      POContractSummaryDAO.reactivateSplitPR(resultEncum, header);
                    }
                    errorFlag = true;
                  }
                }
              } else if (fromProposal) {
                boolean checkException = false;
                if (header.getEfinBudgetManencum().getEncumType().equals("POE")) {
                  // newly created so delete new and increase in old.
                  errorFlag = POContractSummaryDAO.chkFundsAvailforReactOldEncumbrance(header,
                      null);
                  if (errorFlag) {
                    OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                        "@ESCM_ProcessFailed(Reason)@");
                    bundle.setResult(result1);
                    return;
                  } else {
                    POContractSummaryDAO.reactivateSplitPR(resultEncum, header);
                    errorFlag = true;
                    POContractSummaryDAO.updateOldProposalEncum(header);
                  }
                } else if (header.getEfinBudgetManencum().getEncumStage().equals("POE")) {
                  for (OrderLine objOrderLine : objOrder.getOrderLineList()) {
                    if (!objOrderLine.isEscmIssummarylevel()) {
                      // check diff between proposal and order, make impact in encumbrance
                      BigDecimal propLineAmt = objOrderLine.getEscmProposalmgmtLine()
                          .getEscmProposalmgmt().getProposalstatus().equals("PAWD")
                              ? objOrderLine.getEscmProposalmgmtLine().getAwardedamount()
                              : objOrderLine.getEscmProposalmgmtLine().getLineTotal();
                      BigDecimal diff = objOrderLine.getLineNetAmount().subtract(propLineAmt);
                      if (diff.compareTo(BigDecimal.ZERO) < 0) {
                        // check funds available
                        JSONObject fundsCheckingObject = CommonValidationsDAO.CommonFundsChecking(
                            objOrder.getEfinBudgetint(), objOrderLine.getEFINUniqueCode(),
                            diff.negate());
                        if (fundsCheckingObject.has("errorFlag")) {
                          if ("0".equals(fundsCheckingObject.get("errorFlag"))) {
                            checkException = Boolean.TRUE;
                            String status = fundsCheckingObject.getString("message");
                            objOrderLine.setEfinFailureReason(status);
                            OBDal.getInstance().save(objOrderLine);
                          } else {
                            // delete modification
                            EfinBudgetManencumlines encumbranceline = objOrderLine
                                .getEfinBudEncumlines();
                            ProposalManagementRejectMethods.deleteModification(encumbranceline,
                                diff);
                            encumbranceline
                                .setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                            OBDal.getInstance().save(encumbranceline);
                          }
                        }
                        if (checkException) {
                          OBDal.getInstance().rollbackAndClose();
                          OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                              "@ESCM_ProcessFailed(Reason)@");
                          bundle.setResult(result1);
                          return;
                        }
                      } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
                        // insert modification
                        if (objOrder.getEfinBudgetManencum().getEncumType().equals("A")) {
                          EfinBudgetManencumlines encumbranceline = objOrderLine
                              .getEfinBudEncumlines();
                          ProposalManagementRejectMethods.deleteModification(encumbranceline, diff);
                          encumbranceline.setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                          OBDal.getInstance().save(encumbranceline);
                        } else {

                          EfinBudgetManencumlines encumbranceline = objOrderLine
                              .getEfinBudEncumlines();
                          encumbranceline.setAPPAmt(encumbranceline.getAPPAmt().add(diff.negate()));
                          OBDal.getInstance().save(encumbranceline);

                        }
                      }
                    }
                  }

                  objOrder.getEfinBudgetManencum().setEncumStage("PAE");
                  objOrder.setEfinEncumbered(false);
                  OBDal.getInstance().save(objOrder);
                  errorFlag = true;
                  // POContractSummaryDAO.reactivatePOProposal(objOrder);
                  // errorFlag = true;
                } else {
                  // old encum just reduce value.
                  objOrder.getEfinBudgetManencum().setEncumStage("PAE");
                  objOrder.setEfinEncumbered(false);
                  OBDal.getInstance().save(objOrder);
                  OBDal.getInstance().flush();
                }
              }
            } else {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Efin_Encum_Used_Cannot_Rej@");
              bundle.setResult(result);
              return;
            }
            OBDal.getInstance().flush();
            OBInterceptor.setPreventUpdateInfoChange(false);
          }
        }
      }
      // End Task No.5925

      // get alert rule id - Task No:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if (errorFlag) {
        try {
          String appResource = null;
          if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
            appResource = "scm.pac.rejected";
          } else {
            appResource = "scm.poc.rejected";
          }

          OBQuery<OrderLine> lines = OBDal.getInstance().createQuery(OrderLine.class,
              "salesOrder.id =:orderID");
          lines.setNamedParameter("orderID", strOrderId);
          count = lines.list().size();

          if (count > 0) {
            if (header.getEutNextRole() != null) {
              java.util.List<EutNextRoleLine> li = header.getEutNextRole().getEutNextRoleLineList();
              for (int i = 0; i < li.size(); i++) {
                String role = li.get(i).getRole().getId();
                if (roleId.equals(role)) {
                  allowUpdate = true;
                }
              }
            }
            if (header.getEutNextRole() != null) {
              DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
              allowDelegation = delagationDao.checkDelegation(CurrentDate, roleId,
                  DocumentTypeE.PURCHASE_ORDER.getDocumentTypeCode());
              /*
               * String sql = ""; sql =
               * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
               * + CurrentDate + "' and to_date >='" + CurrentDate +
               * "' and document_type='EUT_108'"; st = conn.prepareStatement(sql); rs =
               * st.executeQuery(); if(rs.next()) { String roleid = rs.getString("ad_role_id");
               * if(roleid.equals(roleId)) { allowDelegation = true; } }
               */
            }
            if (allowUpdate || allowDelegation) {

              // get old nextrole line user and role list
              HashMap<String, String> alertReceiversMap = forwardReqMoreInfoDAO
                  .getNextRoleLineList(objOrder.getEutNextRole(), documentRule);

              header.setUpdated(new java.util.Date());
              header.setUpdatedBy(OBContext.getOBContext().getUser());
              header.setEscmDocaction("CO");
              objOrder.setDocumentAction("CO");
              objOrder.setDocumentStatus("DR");
              header.setEscmAppstatus("ESCM_REJ");
              header.setEutNextRole(null);
              OBDal.getInstance().save(header);
              // if after budget control, try to reject then remove the stage impact.
              /*
               * if (header.isEfinEncumbered() != null && header.isEfinEncumbered() &&
               * objOrder.getEscmProposalmgmt() != null) { header.setEfinEncumbered(false);
               * header.getEfinBudgetManencum().setEncumStage("PAE");
               * OBDal.getInstance().save(header); }
               */
              OBDal.getInstance().flush();
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  documentRule);
              headerId = header;
              if (!StringUtils.isEmpty(headerId.getId())) {
                appstatus = "REJ";

                JSONObject historyData = new JSONObject();

                historyData.put("ClientId", clientId);
                historyData.put("OrgId", orgId);
                historyData.put("RoleId", roleId);
                historyData.put("UserId", userId);
                historyData.put("HeaderId", headerId.getId());
                historyData.put("Comments", comments);
                historyData.put("Status", appstatus);
                historyData.put("NextApprover", "");
                historyData.put("Revision", objOrder.getEscmRevision());
                historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
                historyData.put("HeaderColumn",
                    ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
                historyData.put("ActionColumn",
                    ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);
                // task no 6093
                // count=Utility.InsertApprovalHistory(historyData);
                POContractSummaryDAO.purchaseOrderApprovalHistory(historyData);
              }
              // delete the unused nextroles in eut_next_role table.
              DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                  documentRule);

              // Removing forwardRMI id
              if (header.getEutForward() != null) {
                // Removing the Role Access given to the forwarded user
                // Update statuses draft the forward Record
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutForward());
                // Removing Forward_Rmi id from transaction screens
                forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(header.getId(),
                    Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY);

              }
              if (header.getEutReqmoreinfo() != null) {
                // Remove Forward_Rmi id from transaction screens
                // Update statuses draft the RMI Record
                forwardReqMoreInfoDAO.setForwardStatusAsDraft(header.getEutReqmoreinfo());
                // access remove
                forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(header.getId(),
                    Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY);

              }

              if (count > 0 && !StringUtils.isEmpty(header.getId())) {
                String Description = sa.elm.ob.scm.properties.Resource.getProperty(appResource,
                    Lang) + " " + objUser.getName();

                Role objCreatedRole = null;
                if (header.getCreatedBy().getADUserRolesList().size() > 0
                    && header.getEscmAdRole() != null) {
                  objCreatedRole = header.getEscmAdRole();

                }
                // check and insert alert recipient - TaskNo:7618
                List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);
                if (alrtRecList.size() > 0) {
                  for (AlertRecipient objAlertReceipient : alrtRecList) {
                    includeRecipient.add(objAlertReceipient.getRole().getId());
                    OBDal.getInstance().remove(objAlertReceipient);
                  }
                }
                if (includeRecipient != null)
                  includeRecipient.add(objCreatedRole.getId());
                // avoid duplicate recipient
                HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
                Iterator<String> iterator = incluedSet.iterator();
                while (iterator.hasNext()) {
                  AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
                }

                // solving approval alerts - TaskNo:7618
                AlertUtility.solveAlerts(header.getId());

                forwardReqMoreInfoDAO.getAlertForForwardedUser(objOrder.getId(), alertWindow,
                    alertRuleId, objUser, clientId, Constants.REJECT,
                    objOrder.getDocumentNo() + "-" + header.getEscmRevision()
                        + ((objOrder.getEscmNotes() != null
                            && !objOrder.getEscmNotes().equals("null")
                            && !objOrder.getEscmNotes().equals("")) ? "-" + objOrder.getEscmNotes()
                                : ""),
                    Lang, vars.getRole(), forwardObj, documentRule, alertReceiversMap);
                // set alert for Budget Controller
                if (isEncumbered) {
                  AlertUtility.alertInsertionPreference(header.getId(), header.getDocumentNo() + "-"
                      + header.getEscmRevision()
                      + ((objOrder.getEscmNotes() != null && !objOrder.getEscmNotes().equals("null")
                          && !objOrder.getEscmNotes().equals("")) ? "-" + objOrder.getEscmNotes()
                              : ""),
                      "ESCM_BudgetControl", header.getClient().getId(), Description, "NEW",
                      alertWindow, appResource, Constants.GENERIC_TEMPLATE, windowId,
                      header.getCreatedBy().getId());
                }

                // Check Encumbrance Amount is Zero Or Negative
                if (objOrder.getEfinBudgetManencum() != null)
                  encumLinelist = objOrder.getEfinBudgetManencum().getEfinBudgetManencumlinesList();
                if (encumLinelist.size() > 0)
                  checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

                if (checkEncumbranceAmountZero) {
                  OBDal.getInstance().rollbackAndClose();
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_Encumamt_Neg@");
                  bundle.setResult(result);
                  return;
                }

                // delete the unused nextroles in eut_next_role table.
                DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(),
                    documentRule);
                AlertUtility.alertInsertionRole(header.getId(), header.getDocumentNo() + "-"
                    + header.getEscmRevision()
                    + ((objOrder.getEscmNotes() != null && !objOrder.getEscmNotes().equals("null")
                        && !objOrder.getEscmNotes().equals("")) ? "-" + objOrder.getEscmNotes()
                            : ""),
                    objCreatedRole.getId(), header.getCreatedBy().getId(),
                    header.getClient().getId(), Description, "NEW", alertWindow, appResource,
                    Constants.GENERIC_TEMPLATE);
                OBError result = OBErrorBuilder.buildMessage(null, "success",
                    "@Escm_Order_Rejected@");
                bundle.setResult(result);
                return;
              }
              OBDal.getInstance().flush();
            } else {
              errorFlag = false;
              errorMsg = OBMessageUtils.messageBD("Escm_AlreadyPreocessed_Approved");
              throw new OBException(errorMsg);
            }
          }
        } catch (Exception e) {
          log.error("exception :", e);
          obError.setType("Error");
          obError.setTitle("Error");
          obError.setMessage(errorMsg);
          bundle.setResult(obError);
          OBDal.getInstance().rollbackAndClose();
        }
      } else if (errorFlag == false) {
        obError.setType("Error");
        obError.setTitle("Error");
        obError.setMessage(errorMsg);
      }
      bundle.setResult(obError);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } /*
       * catch (Exception e) { bundle.setResult(obError); log.error("exception :", e);
       * OBDal.getInstance().rollbackAndClose(); }
       */
    catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("exception:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    }

    finally {
      OBContext.restorePreviousMode();
    }
  }
}
