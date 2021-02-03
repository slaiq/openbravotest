/*
 * All Rights Reserved By Qualian Technologies Pvt Ltd.
 */
package sa.elm.ob.finance.ad_process.purchaseRequisition;

import java.math.BigDecimal;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.scm.EscmProposalmgmtLine;

/**
 * @author Gowtham.V
 */

public class RequisitionfundsCheck implements Process {
  /**
   * This process to check funds available status for each line. through funds check button.
   */
  private static final Logger LOG = LoggerFactory.getLogger(RequisitionfundsCheck.class);

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    String strRequisitionId = (String) bundle.getParams().get("M_Requisition_ID");
    Requisition objRequisition = OBDal.getInstance().get(Requisition.class, strRequisitionId);
    List<EfinBudgetManencumlines> encumLineList = null;
    String status = "", message = "";
    boolean error = false;
    JSONArray arraylist = new JSONArray(), linearraylist = null;
    JSONObject object = new JSONObject(), json = null, json1 = null;
    try {
      OBContext.setAdminMode(true);
      OBQuery<RequisitionLine> ln = OBDal.getInstance().createQuery(RequisitionLine.class,
          " requisition.id='" + strRequisitionId + "' order by efinCValidcombination.id  ");
      if (ln.list().size() > 0) {
        for (RequisitionLine reqLineobj : ln.list()) {
          if (reqLineobj.getEfinCValidcombination() != null) {
            if (json != null && json.has("Uniquecode") && json.getString("Uniquecode")
                .equals(reqLineobj.getEfinCValidcombination().getId())) {
              json.put("Amount",
                  new BigDecimal(json.getString("Amount")).add(reqLineobj.getLineNetAmount()));
              json1 = new JSONObject();
              json1.put("lineId", reqLineobj.getId());
              linearraylist.put(json1);
              json.put("lineList", linearraylist);
            } else {
              if (json != null)
                json.put("lineList", linearraylist);
              linearraylist = new JSONArray();
              json = new JSONObject();
              json.put("Uniquecode", reqLineobj.getEfinCValidcombination().getId());
              json.put("Amount", reqLineobj.getLineNetAmount());
              json.put("isSummary", reqLineobj.isEscmIssummary());
              json1 = new JSONObject();
              json1.put("lineId", reqLineobj.getId());
              linearraylist.put(json1);
              arraylist.put(json);
            }
          }
          reqLineobj.setEscmCancelReason(null);
          OBDal.getInstance().save(reqLineobj);
        }
        if (linearraylist != null) {
          json.put("lineList", linearraylist);
        } else {
          // Message
          status = "success";
          message = "@ProcessOK@";
          final OBError result = OBErrorBuilder.buildMessage(null, status, message);
          bundle.setResult(result);
          return;
        }
      } else {
        // Message
        status = "error";
        message = "@Efin_Fundscheck_Fail@";
        final OBError result = OBErrorBuilder.buildMessage(null, status, message);
        bundle.setResult(result);
        return;
      }
      object.put("uniquecodeList", arraylist);

      if (objRequisition.getEfinBudgetManencum() != null) {
        EfinBudgetManencum manualEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
            objRequisition.getEfinBudgetManencum().getId());
        encumLineList = manualEncum.getEfinBudgetManencumlinesList();
        error = manualEncumbranceValidation(encumLineList, object, "PR", true);
      } else {
        error = autoEncumbranceValidation(object, objRequisition.getEfinBudgetint(), "PR", true);
      }
      if (error) {
        // warning
        status = "warning";
        message = "@Efin_FundsChk_Error@";
      } else {
        // Message
        status = "success";
        message = "@ProcessOK@";
      }
      final OBError result = OBErrorBuilder.buildMessage(null, status, message);
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Budget add lines process " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Validation for Auto encumbrance in purchase requisition.
   * 
   * @param result
   * @param budInit
   * @param fundsCheck
   * @return
   */
  public static boolean autoEncumbranceValidation(JSONObject result,
      EfinBudgetIntialization budInit, String type, boolean fundsCheck) {
    JSONObject json = null, json1 = null;
    RequisitionLine line = null;
    OrderLine oLine = null;
    EscmProposalmgmtLine pmLine = null;
    String department = "";
    boolean errorflag = false;

    try {
      OBContext.setAdminMode(true);
      if (result != null && result.has("uniquecodeList")) {
        JSONArray array = result.getJSONArray("uniquecodeList");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          if (!json.getBoolean("isSummary") && json.getString("Uniquecode") != null) {
            AccountingCombination validCombination = OBDal.getInstance()
                .get(AccountingCombination.class, json.getString("Uniquecode"));
            // if isdepartment fund yes, then check dept level distribution acct.
            if (validCombination.isEFINDepartmentFund()) {
              OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance().createQuery(
                  EfinBudgetInquiry.class,
                  "efinBudgetint.id= :BudgetintID and accountingCombination.id= :accountingCombinationID");
              budInq.setNamedParameter("BudgetintID", budInit.getId());
              budInq.setNamedParameter("accountingCombinationID", validCombination.getId());
              List<EfinBudgetInquiry> budInqList = budInq.list();
              JSONArray linearray = json.getJSONArray("lineList");
              for (int j = 0; j < linearray.length(); j++) {
                json1 = linearray.getJSONObject(j);
                if (type.equals("PR")) {
                  line = OBDal.getInstance().get(RequisitionLine.class, json1.getString("lineId"));
                } else if (type.equals("PM")) {
                  pmLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
                      json1.getString("lineId"));
                } else {
                  oLine = OBDal.getInstance().get(OrderLine.class, json1.getString("lineId"));
                }
                if (budInqList != null && budInqList.size() > 0) {
                  if (new BigDecimal(json.getString("Amount"))
                      .compareTo(budInqList.get(0).getFundsAvailable()) > 0) {
                    if (type.equals("PR")) {
                      if (fundsCheck) {
                        line.setEscmCancelReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                      } else {
                        line.setEscmCancelReason(OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                      }
                    } else if (type.equals("PM")) {
                      if (fundsCheck) {
                        pmLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                      } else {
                        pmLine
                            .setEfinFailureReason(OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                      }
                    } else {
                      if (fundsCheck) {
                        oLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                      } else {
                        oLine
                            .setEfinFailureReason(OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                      }
                    }
                    errorflag = true;
                  } else {
                    if (type.equals("PR")) {
                      if (fundsCheck) {
                        line.setEscmCancelReason(OBMessageUtils.messageBD("EFIN_Process Status-S"));
                      } else {
                        line.setEscmCancelReason("");
                      }
                    } else if (type.equals("PM")) {
                      if (fundsCheck) {
                        pmLine.setEfinFailureReason(
                            OBMessageUtils.messageBD("EFIN_Process Status-S"));
                      } else {
                        pmLine.setEfinFailureReason("");
                      }
                    } else {
                      if (fundsCheck) {
                        oLine.setEfinFailureReason(
                            OBMessageUtils.messageBD("EFIN_Process Status-S"));
                      } else {
                        oLine.setEfinFailureReason("");
                      }
                    }
                  }
                } else {
                  if (type.equals("PR")) {
                    if (fundsCheck) {
                      line.setEscmCancelReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                    } else {
                      line.setEscmCancelReason(OBMessageUtils.messageBD("Efin_NoDist_Dept"));
                    }
                  } else if (type.equals("PM")) {
                    if (fundsCheck) {
                      pmLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                    } else {
                      pmLine.setEfinFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Dept"));
                    }
                  } else {
                    if (fundsCheck) {
                      oLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                    } else {
                      oLine.setEfinFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Dept"));
                    }
                  }
                  errorflag = true;
                }
                if (type.equals("PR")) {
                  OBDal.getInstance().save(line);
                } else if (type.equals("PM")) {
                  OBDal.getInstance().save(pmLine);
                } else {
                  OBDal.getInstance().save(oLine);
                }
              }
            }
            // if isdepartment fund No, then check Org level distribution acct.
            else {
              OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
                  .createQuery(EfinBudgetControlParam.class, "");
              if (bcp.list() != null && bcp.list().size() > 0) {
                department = bcp.list().get(0).getBudgetcontrolCostcenter().getId();
                // getorg level uniquecode
                OBQuery<AccountingCombination> accountCombination = OBDal.getInstance()
                    .createQuery(AccountingCombination.class, "account.id= '"
                        + validCombination.getAccount().getId() + "'" + " and businessPartner.id='"
                        + validCombination.getBusinessPartner().getId() + "' "
                        + "and salesRegion.id='" + department + "' and project.id = '"
                        + validCombination.getProject().getId() + "' " + "and salesCampaign.id='"
                        + validCombination.getSalesCampaign().getId() + "' " + "and activity.id='"
                        + validCombination.getActivity().getId() + "' and stDimension.id='"
                        + validCombination.getStDimension().getId() + "' "
                        + "and ndDimension.id = '" + validCombination.getNdDimension().getId()
                        + "' " + "and organization.id = '"
                        + validCombination.getOrganization().getId() + "'");
                List<AccountingCombination> accountCombinationList = accountCombination.list();
                if (accountCombinationList != null && accountCombinationList.size() > 0) {
                  AccountingCombination combination = accountCombinationList.get(0);
                  OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance().createQuery(
                      EfinBudgetInquiry.class,
                      "efinBudgetint.id= :BudgetintID and accountingCombination.id= :accountingCombinationID");
                  budInq.setNamedParameter("BudgetintID", budInit.getId());
                  budInq.setNamedParameter("accountingCombinationID", combination.getId());
                  List<EfinBudgetInquiry> budInqList = budInq.list();
                  JSONArray linearray = json.getJSONArray("lineList");
                  for (int j = 0; j < linearray.length(); j++) {
                    json1 = linearray.getJSONObject(j);
                    if (type.equals("PR")) {
                      line = OBDal.getInstance().get(RequisitionLine.class,
                          json1.getString("lineId"));
                    } else if (type.equals("PM")) {
                      pmLine = OBDal.getInstance().get(EscmProposalmgmtLine.class,
                          json1.getString("lineId"));
                    } else {
                      oLine = OBDal.getInstance().get(OrderLine.class, json1.getString("lineId"));
                    }

                    if (budInqList != null && budInqList.size() > 0) {
                      if (new BigDecimal(json.getString("Amount"))
                          .compareTo(budInqList.get(0).getFundsAvailable()) > 0) {
                        if (type.equals("PR")) {
                          if (fundsCheck) {
                            line.setEscmCancelReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                          } else {
                            line.setEscmCancelReason(
                                OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                          }
                        } else if (type.equals("PM")) {
                          if (fundsCheck) {
                            pmLine
                                .setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                          } else {
                            pmLine.setEfinFailureReason(
                                OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                          }
                        } else {
                          if (fundsCheck) {
                            oLine
                                .setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                          } else {
                            oLine.setEfinFailureReason(
                                OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                          }
                        }
                        errorflag = true;
                      } else {
                        if (type.equals("PR")) {
                          if (fundsCheck) {
                            line.setEscmCancelReason(
                                OBMessageUtils.messageBD("EFIN_Process Status-S"));
                          } else {
                            line.setEscmCancelReason("");
                          }
                        } else if (type.equals("PM")) {
                          if (fundsCheck) {
                            pmLine.setEfinFailureReason(
                                OBMessageUtils.messageBD("EFIN_Process Status-S"));
                          } else {
                            pmLine.setEfinFailureReason("");
                          }
                        } else {
                          if (fundsCheck) {
                            oLine.setEfinFailureReason(
                                OBMessageUtils.messageBD("EFIN_Process Status-S"));
                          } else {
                            oLine.setEfinFailureReason("");
                          }
                        }
                      }
                    } else {
                      if (type.equals("PR")) {
                        if (fundsCheck) {
                          line.setEscmCancelReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                        } else {
                          line.setEscmCancelReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                        }
                      } else if (type.equals("PM")) {
                        if (fundsCheck) {
                          pmLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                        } else {
                          pmLine.setEfinFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                        }
                      } else {
                        if (fundsCheck) {
                          oLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                        } else {
                          oLine.setEfinFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                        }
                      }
                      errorflag = true;
                    }
                    if (type.equals("PR")) {
                      OBDal.getInstance().save(line);
                    } else if (type.equals("PM")) {
                      OBDal.getInstance().save(pmLine);
                    } else {
                      OBDal.getInstance().save(oLine);
                    }
                  }
                } else {
                  if (type.equals("PR")) {
                    if (fundsCheck) {
                      line.setEscmCancelReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                    } else {
                      line.setEscmCancelReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                    }
                  } else if (type.equals("PM")) {
                    if (fundsCheck) {
                      pmLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                    } else {
                      pmLine.setEfinFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                    }
                  } else {
                    if (fundsCheck) {
                      oLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                    } else {
                      oLine.setEfinFailureReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                    }
                  }
                  errorflag = true;
                }
                if (type.equals("PR")) {
                  OBDal.getInstance().save(line);
                } else if (type.equals("PM")) {
                  OBDal.getInstance().save(pmLine);
                } else {
                  OBDal.getInstance().save(oLine);
                }
              }
            }
          }
        }
      }
      return errorflag;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Funds check process in requisition " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return true;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Validation for manual encumbrance in purchase requisition.
   * 
   * @param encumLineList
   * @param result
   * @param fundsCheck
   * @return
   */
  public static boolean manualEncumbranceValidation(List<EfinBudgetManencumlines> encumLineList,
      JSONObject result, String type, boolean fundsCheck) {
    JSONObject json = null, json1 = null;
    RequisitionLine line = null;
    OrderLine oLine = null;

    boolean errorflag = false;
    try {
      OBContext.setAdminMode(true);
      if (result != null) {
        JSONArray array = result.getJSONArray("uniquecodeList");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          if (!json.getBoolean("isSummary") && json.getString("Uniquecode") != null) {
            JSONArray linearray = json.getJSONArray("lineList");
            for (int j = 0; j < linearray.length(); j++) {
              json1 = linearray.getJSONObject(j);
              if (type.equals("PR")) {
                line = OBDal.getInstance().get(RequisitionLine.class, json1.getString("lineId"));
              } else {
                oLine = OBDal.getInstance().get(OrderLine.class, json1.getString("lineId"));
              }
              for (EfinBudgetManencumlines encumline : encumLineList) {
                if (json.getString("Uniquecode")
                    .equals(encumline.getAccountingCombination().getId())) {
                  BigDecimal remAmt = encumline.getRevamount().subtract(encumline.getAPPAmt())
                      .subtract(encumline.getUsedAmount());
                  if (new BigDecimal(json.getString("Amount")).compareTo(remAmt) > 0) {
                    if (type.equals("PR")) {
                      if (fundsCheck) {
                        line.setEscmCancelReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                      } else {
                        line.setEscmCancelReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
                      }
                    } else {
                      if (fundsCheck) {
                        oLine.setEfinFailureReason(OBMessageUtils.messageBD("EFIN_Fin_Failure"));
                      } else {
                        oLine.setEfinFailureReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
                      }
                    }
                    errorflag = true;
                  } else {
                    if (type.equals("PR")) {
                      if (fundsCheck) {
                        line.setEscmCancelReason(OBMessageUtils.messageBD("EFIN_Process Status-S"));
                      } else {
                        line.setEscmCancelReason("");
                      }
                    } else {
                      if (fundsCheck) {
                        oLine.setEfinFailureReason(
                            OBMessageUtils.messageBD("EFIN_Process Status-S"));
                      } else {
                        oLine.setEfinFailureReason("");
                      }
                    }
                  }
                }
                if (type.equals("PR")) {
                  OBDal.getInstance().save(line);
                } else {
                  OBDal.getInstance().save(oLine);
                }
              }
            }
          }
        }
      }
      return errorflag;
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception in Funds check process in requisition " + e, e);
      }
      OBDal.getInstance().rollbackAndClose();
      return true;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
