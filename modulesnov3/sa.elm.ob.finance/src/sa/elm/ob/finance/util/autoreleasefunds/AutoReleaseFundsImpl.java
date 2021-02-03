package sa.elm.ob.finance.util.autoreleasefunds;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.BudgetAdjustment;
import sa.elm.ob.finance.BudgetAdjustmentLine;
import sa.elm.ob.finance.EFINFundsReq;
import sa.elm.ob.finance.EFINFundsReqLine;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetTransfertrx;
import sa.elm.ob.finance.EfinBudgetTransfertrxline;
import sa.elm.ob.finance.util.TableIdConstant;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * This class is to implement auto request funds logic in Budget revision and Budget adjustment
 * 
 * @author sathishkumar
 *
 */

public class AutoReleaseFundsImpl implements AutoReleaseFundsService {
  private static final Logger log = LoggerFactory.getLogger(AutoReleaseFundsImpl.class);

  @Override
  public Boolean checkBCUFundsAvailable(EfinBudgetTransfertrx revision, BudgetAdjustment adjustment,
      HashMap<EfinBudgetTransfertrxline, BigDecimal> revLineMap,
      HashMap<BudgetAdjustmentLine, BigDecimal> adjustmentLineMap) {
    try {
      OBContext.setAdminMode();
      boolean isFundsAvail = true;
      log.debug("Revision:" + revision);
      log.debug("adjustment:" + adjustment);
      isFundsAvail = checkFundsAvail(revision, adjustment, revLineMap, adjustmentLineMap);
      log.debug("isFundsAvail:" + isFundsAvail);
      return isFundsAvail;
    } catch (Exception e) {
      log.debug("Errow while checking bcu funds" + e.getMessage());
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  public Boolean insertReleaseInBudgetDistribution(EfinBudgetTransfertrx revision,
      BudgetAdjustment adjustment, HashMap<EfinBudgetTransfertrxline, BigDecimal> revLineMap,
      HashMap<BudgetAdjustmentLine, BigDecimal> adjustmentLineMap, boolean isDistribution,
      List<EFINFundsReqLine> lineList) {
    try {
      OBContext.setAdminMode();

      if (revision != null) {
        EfinBudgetControlParam budgetControlParam = getBudgetCtrlParamDept(
            revision.getClient().getId());
        if (revLineMap != null && revLineMap.size() > 0) {
          EFINFundsReq fundsReqheader = insertFundsMgmtReqHeader(revision, null,
              budgetControlParam);
          revLineMap.forEach((key, value) -> {
            lineList
                .add(insertFundsMgmtReqLine(fundsReqheader, key, null, budgetControlParam, value));
          });
        }
      } else {
        EfinBudgetControlParam budgetControlParam = getBudgetCtrlParamDept(
            adjustment.getClient().getId());
        if (adjustmentLineMap != null && adjustmentLineMap.size() > 0) {
          EFINFundsReq fundsReqheader = insertFundsMgmtReqHeader(null, adjustment,
              budgetControlParam);
          adjustmentLineMap.forEach((key, value) -> {
            lineList
                .add(insertFundsMgmtReqLine(fundsReqheader, null, key, budgetControlParam, value));
          });
        }
      }
      return Boolean.TRUE;
    } catch (Exception e) {
      return Boolean.TRUE;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * This method is used to get budget control parameter
   * 
   * @param clientId
   * @return EfinBudgetControlParam object
   */
  private EfinBudgetControlParam getBudgetCtrlParamDept(String clientId) {

    try {
      OBContext.setAdminMode();
      List<BaseOBObject> budgetCtrlParamList;
      String strBudgetCtrlParam = " client.id =:clientId";
      HashMap<String, String> budgetCtrlParamMap = new HashMap<String, String>();

      budgetCtrlParamMap.put("clientId", clientId);
      budgetCtrlParamList = UtilityDAO.getQueryList(budgetCtrlParamMap, strBudgetCtrlParam,
          TableIdConstant.BUDGETCTRLPARAMATER_ID);

      for (BaseOBObject budgetCtrlObj : budgetCtrlParamList) {
        EfinBudgetControlParam budgetCtrlParam = (EfinBudgetControlParam) budgetCtrlObj;
        if (budgetCtrlParam != null) {
          return budgetCtrlParam;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("error while getting budget control paramater", e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  /**
   * This method is used to check budget fundsavailable for budget revision
   * 
   * @param revision(EfinBudgetTransfertrx
   *          Object)
   * @return true if fundsavail or else false
   */
  private boolean checkFundsAvail(EfinBudgetTransfertrx revision, BudgetAdjustment adjustment,
      HashMap<EfinBudgetTransfertrxline, BigDecimal> revLineMap,
      HashMap<BudgetAdjustmentLine, BigDecimal> adjustmentLineMap) {

    try {
      OBContext.setAdminMode();
      boolean isFundsAvail = true;

      String budgetIntId = revision != null ? revision.getEfinBudgetint().getId()
          : adjustment.getEfinBudgetint().getId();
      String parentWhereClause = "accountingCombination.id=:accountCombinationId and efinBudgetint.id = :intId";
      String childWhereClause = "parent.id=:parentId and department.id =:deptId  and organization.id =:orgId";
      String budgetCostCenterId = "";
      String clientId = revision != null ? revision.getClient().getId()
          : adjustment.getClient().getId();

      EfinBudgetControlParam budgetControlParam = getBudgetCtrlParamDept(clientId);
      if (budgetControlParam != null) {
        budgetCostCenterId = budgetControlParam.getBudgetcontrolCostcenter().getId();
      }

      HashMap<String, String> parentQryMap = new HashMap<String, String>();
      HashMap<String, String> childQryMap = new HashMap<String, String>();

      if (revision != null) {
        isFundsAvail = getRevisionFA(revision, budgetIntId, parentWhereClause, childWhereClause,
            parentQryMap, childQryMap, budgetCostCenterId, revLineMap);
      } else {
        isFundsAvail = getAdjustmentFA(adjustment, budgetIntId, parentWhereClause, childWhereClause,
            parentQryMap, childQryMap, budgetCostCenterId, adjustmentLineMap);
      }

      return isFundsAvail;
    } catch (Exception e) {
      log.debug("Errow while checking bcu funds in BR" + e.getMessage());
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * <h1>This method is to insert Funds Request Management Header Object(EFINFundsReq)</h1>
   * 
   * @param revision(EfinBudgetTransfertrx)
   * @param distribution(EFINFundsReq)
   */

  private EFINFundsReq insertFundsMgmtReqHeader(EfinBudgetTransfertrx budRevObj,
      BudgetAdjustment budAdjLine, EfinBudgetControlParam budgContrparam) {

    EFINFundsReq req = null;
    try {
      OBContext.setAdminMode();
      EfinBudgetTransfertrx budRev = budRevObj;
      BudgetAdjustment budAdj = budAdjLine;
      JSONObject documentNo = getDocumentNo(budgContrparam.getAgencyHqOrg(),
          budRev != null ? budRev.getAccountingDate() : (budAdj.getAccountingDate()));
      String strDocumentNo = "";

      if (documentNo.getBoolean("hasError")) {
        throw new OBException(documentNo.getString("errorMsg"));
      } else {
        strDocumentNo = documentNo.getString("sequenceNo");
      }

      req = OBProvider.getInstance().get(EFINFundsReq.class);
      req.setOrganization(budgContrparam.getOrganization());

      req.setTrxdate(budRev != null ? budRev.getTrxdate() : (budAdj.getTRXDate()));
      req.setAccountingDate(
          budRev != null ? budRev.getAccountingDate() : (budAdj.getAccountingDate()));
      req.setTransactionOrg(budgContrparam.getAgencyHqOrg());
      req.setTransactionPeriod(
          budRev != null ? budRev.getTransactionperiod() : (budAdj.getTransactionPeriod()));
      req.setSalesCampaign(budRev != null ? budRev.getSalesCampaign() : (budAdj.getBudgetType()));
      req.setYear(budRev != null ? budRev.getYear() : (budAdj.getYear()));
      req.setEfinBudgetint(
          budRev != null ? budRev.getEfinBudgetint() : (budAdj.getEfinBudgetint()));
      req.setTransactionType("BCUAR");
      if (budAdj != null) {
        req.setEfinBudgetadj(budAdj);
      } else
        req.setEfinBudgetTransfertrx(budRev);
      req.setOrgreqFundsType("OD");
      req.setDocumentNo(strDocumentNo);
      req.setAction("CO");
      req.setReserve(true);
      req.setRole(budRev != null ? budRev.getRole() : (budAdj.getRole()));
      req.setDocumentStatus("CO");

      OBDal.getInstance().save(req);
      OBDal.getInstance().flush();

      return req;
    } catch (Exception e) {
      e.printStackTrace();
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while creating funds request managment " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return req;

  }

  /**
   * This method is used to get the document no of funds request management
   * 
   * @param org
   * @param accountDate
   * @return
   */

  @SuppressWarnings("finally")
  private JSONObject getDocumentNo(Organization org, Date date) {
    JSONObject result = new JSONObject();

    try {
      OBContext.setAdminMode();
      String SequenceNo = "0";
      if (org.getCalendar() != null) {
        String accountDate = new SimpleDateFormat("dd-MM-yyyy").format(date);
        SequenceNo = Utility.getDocumentSequence(accountDate, "efin_fundsreq",
            org.getCalendar().getId(), "0", true);
        if (SequenceNo.equals("0")) {
          result.put("hasError", true);
          result.put("errorMsg", OBMessageUtils.messageBD("EFIN_DocSequndefined"));
          result.put("sequenceNo", "0");
        } else {
          result.put("hasError", false);
          result.put("errorMsg", "");
          result.put("sequenceNo", SequenceNo);
        }
      } else {
        result.put("hasError", true);
        result.put("errorMsg", OBMessageUtils.messageBD("EFIN_FRCalendarUndefined"));
        result.put("sequenceNo", SequenceNo);
      }
    } catch (Exception e) {
      result.put("hasError", true);
      result.put("errorMsg", e.getMessage());
      result.put("sequenceNo", "");
    } finally {
      OBContext.restorePreviousMode();
      return result;
    }
  }

  /**
   * This method is used to insert funds request management line
   * 
   * @param fundsReqheader
   * @param revision
   * @param adjustment
   * @return
   */

  private EFINFundsReqLine insertFundsMgmtReqLine(EFINFundsReq fundsReqheader,
      EfinBudgetTransfertrxline revisionLine, BudgetAdjustmentLine adjustmentLine,
      EfinBudgetControlParam budgetControlParam, BigDecimal value) {

    try {
      OBContext.setAdminMode();
      AccountingCombination accountCombination = (revisionLine != null
          ? revisionLine.getAccountingCombination()
          : (adjustmentLine != null ? adjustmentLine.getAccountingCombination() : null));

      EFINFundsReqLine reqline = OBProvider.getInstance().get(EFINFundsReqLine.class);
      reqline.setOrganization(fundsReqheader.getOrganization());
      reqline.setEfinFundsreq(fundsReqheader);
      reqline.setToaccount(revisionLine != null ? revisionLine.getAccountingCombination()
          : adjustmentLine.getAccountingCombination());
      reqline.setFromaccount(getBcuCostCenter(accountCombination, budgetControlParam));
      reqline.setIncrease(value);
      reqline.setDecrease(value);
      reqline.setDistType("MAN");
      reqline.setCurrentBudget(revisionLine != null ? revisionLine.getCurrentBudget()
          : adjustmentLine.getCurrentBudget());
      reqline.setDistribute(true);
      reqline.setFromuniquecodename(accountCombination.getEfinUniquecodename());
      reqline.setTouniquecodename(reqline.getToaccount().getEfinUniquecodename());
      reqline.setREQType("REL");
      reqline.setPercentage(BigDecimal.ZERO);
      reqline.setAlertStatus("SCS");
      OBDal.getInstance().save(reqline);

      OBDal.getInstance().flush();

      updateBudgetinquiry(fundsReqheader, reqline, value);
      return reqline;

    } catch (Exception e) {
      e.printStackTrace();
      log.debug("error while creating funds request mangment line", e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }

    return null;
  }

  /**
   * This method is used to get 999 dept unique code for corresponding 990 unique code
   * 
   * @param combinObj
   * @param budgContrparam
   */
  private AccountingCombination getBcuCostCenter(AccountingCombination combinObj,
      EfinBudgetControlParam budgContrparam) {

    try {
      OBContext.setAdminMode();
      String whereclause = " as e where e.trxOrganization.id=:trxOrganization and  e.salesRegion.id=:salesRegion  and  e.account.id=:account and  e.project.id=:project and  e.salesCampaign.id=:salescampaign"
          + " and  e.businessPartner.id=:bpId and  e.activity.id=:activity and  e.stDimension.id=:stdimension and  e.ndDimension.id=:nddimension and e.client.id = :clientId  ";

      HashMap<String, String> listMap = new HashMap<>();
      listMap.put("trxOrganization", combinObj.getTrxOrganization().getId());
      listMap.put("salesRegion", budgContrparam.getBudgetcontrolCostcenter().getId());
      listMap.put("account", combinObj.getAccount().getId());
      listMap.put("project", combinObj.getProject().getId());
      listMap.put("salescampaign", combinObj.getSalesCampaign().getId());
      listMap.put("bpId", combinObj.getBusinessPartner().getId());
      listMap.put("activity", combinObj.getActivity().getId());
      listMap.put("stdimension", combinObj.getStDimension().getId());
      listMap.put("nddimension", combinObj.getNdDimension().getId());
      listMap.put("clientId", budgContrparam.getClient().getId());

      List<BaseOBObject> uniqueCodeList = UtilityDAO.getQueryList(listMap, whereclause,
          TableIdConstant.VALIDCOMBINATION_ID);

      for (BaseOBObject uniqObj : uniqueCodeList) {
        return (AccountingCombination) uniqObj;
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("error while getting 999 dept", e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return null;
  }

  /**
   * This method is used to get the funds available of each revision line uniquecode
   * 
   * @param revision
   * @param budgetIntId
   * @param parentWhereClause
   * @param parentQryMap
   * @param budgetCostCenterId
   * @return true it has enough FA else false
   */

  private boolean getRevisionFA(EfinBudgetTransfertrx revision, String budgetIntId,
      String parentWhereClause, String childWhereClause, HashMap<String, String> parentQryMap,
      HashMap<String, String> childQryMap, String budgetCostCenterId,
      HashMap<EfinBudgetTransfertrxline, BigDecimal> revLineMap) {

    try {
      OBContext.setAdminMode();
      boolean isFundsAvail = true;
      List<BaseOBObject> parentObjList, childObjList;

      List<EfinBudgetTransfertrxline> revisionLines = revision.getEfinBudgetTransfertrxlineList()
          .stream().filter(r -> (r.getDecrease().compareTo(BigDecimal.ZERO) > 0))
          .collect(Collectors.toList());

      if (revisionLines != null && revisionLines.size() > 0) {
        for (EfinBudgetTransfertrxline line : revisionLines) {
          parentQryMap.put("accountCombinationId", line.getAccountingCombination().getId());
          parentQryMap.put("intId", budgetIntId);

          parentObjList = UtilityDAO.getQueryList(parentQryMap, parentWhereClause,
              TableIdConstant.BUDGETENQUIRY_ID);

          if (parentObjList != null && parentObjList.size() == 0) {
            isFundsAvail = false;
            line.setStatus(OBMessageUtils.messageBD("Efin_noBudgetInquiry"));
            OBDal.getInstance().save(line);
          } else {
            for (BaseOBObject parentObj : parentObjList) {
              EfinBudgetInquiry parentInquiry = (EfinBudgetInquiry) parentObj;
              if (parentInquiry != null) {
                if (line.getDecrease().compareTo(parentInquiry.getBCUFundsAvailable()) > 0) {

                  childQryMap.put("parentId", parentInquiry.getId());
                  childQryMap.put("deptId", budgetCostCenterId);
                  childQryMap.put("orgId", parentInquiry.getOrganization().getId());

                  childObjList = UtilityDAO.getQueryList(childQryMap, childWhereClause,
                      TableIdConstant.BUDGETENQUIRY_ID);

                  if (childObjList != null && childObjList.size() == 0) {
                    isFundsAvail = false;
                    line.setStatus(OBMessageUtils.messageBD("Efin_funds"));
                    OBDal.getInstance().save(line);
                  }

                  for (BaseOBObject childObj : childObjList) {
                    EfinBudgetInquiry childEnquiry = (EfinBudgetInquiry) childObj;
                    if (childEnquiry != null && (line.getDecrease().compareTo(childEnquiry
                        .getFundsAvailable().add((parentInquiry.getBCUFundsAvailable()))) > 0)) {
                      isFundsAvail = false;
                      line.setStatus(OBMessageUtils.messageBD("Efin_funds"));
                      OBDal.getInstance().save(line);
                    } else {
                      revLineMap.put(line,
                          line.getDecrease().subtract(parentInquiry.getBCUFundsAvailable()));
                    }
                  }
                }
              }
            }
          }
        }
      }
      OBDal.getInstance().flush();

      return isFundsAvail;
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("error while getRevisionFA", e.getMessage());
      return Boolean.FALSE;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to get the funds available of each adjustment line uniquecode
   * 
   * @param adjustment
   * @param budgetIntId
   * @param parentWhereClause
   * @param parentQryMap
   * @param budgetCostCenterId
   * @return true it has enough FA else false
   */

  private boolean getAdjustmentFA(BudgetAdjustment adjustment, String budgetIntId,
      String parentWhereClause, String childWhereClause, HashMap<String, String> parentQryMap,
      HashMap<String, String> childQryMap, String budgetCostCenterId,
      HashMap<BudgetAdjustmentLine, BigDecimal> adjustmentLineMap) {

    try {
      OBContext.setAdminMode();
      boolean isFundsAvail = true;

      List<BaseOBObject> parentObjList, childObjList;
      List<BudgetAdjustmentLine> adjustmentLines = adjustment.getEfinBudgetAdjlineList().stream()
          .filter(r -> (r.getDecrease().compareTo(BigDecimal.ZERO) > 0))
          .collect(Collectors.toList());

      if (adjustmentLines != null && adjustmentLines.size() > 0) {
        for (BudgetAdjustmentLine line : adjustmentLines) {
          parentQryMap.put("accountCombinationId", line.getAccountingCombination().getId());
          parentQryMap.put("intId", budgetIntId);

          parentObjList = UtilityDAO.getQueryList(parentQryMap, parentWhereClause,
              TableIdConstant.BUDGETENQUIRY_ID);

          for (BaseOBObject parentObj : parentObjList) {
            EfinBudgetInquiry parentInquiry = (EfinBudgetInquiry) parentObj;
            if (parentInquiry != null
                && (line.getDecrease().compareTo(parentInquiry.getBCUFundsAvailable()) > 0)) {

              childQryMap.put("parentId", parentInquiry.getId());
              childQryMap.put("deptId", budgetCostCenterId);
              childQryMap.put("orgId", parentInquiry.getOrganization().getId());

              childObjList = UtilityDAO.getQueryList(childQryMap, childWhereClause,
                  TableIdConstant.BUDGETENQUIRY_ID);

              if (childObjList != null && childObjList.size() == 0) {
                isFundsAvail = false;
                line.setAlertStatus("Failed");
                line.setFailureReason(OBMessageUtils.messageBD("Efin_funds"));
                OBDal.getInstance().save(line);
              }

              for (BaseOBObject childObj : childObjList) {
                EfinBudgetInquiry childEnquiry = (EfinBudgetInquiry) childObj;
                if (childEnquiry != null && (line.getDecrease().compareTo(childEnquiry
                    .getFundsAvailable().add(parentInquiry.getBCUFundsAvailable())) > 0)) {
                  isFundsAvail = false;
                  line.setAlertStatus("Failed");
                  line.setFailureReason(OBMessageUtils.messageBD("Efin_funds"));
                  OBDal.getInstance().save(line);
                } else {
                  adjustmentLineMap.put(line,
                      line.getDecrease().subtract(parentInquiry.getBCUFundsAvailable()));
                }
              }
            }
          }
        }
      }
      OBDal.getInstance().flush();

      return isFundsAvail;
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("error while getAdjustmentFA", e.getMessage());
      return Boolean.FALSE;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * This method is used to update the budget enquiry while creating and deleting funds request
   * management
   * 
   * @param fundsReqheader
   * @param reqline
   */

  private void updateBudgetinquiry(EFINFundsReq fundsReqheader, EFINFundsReqLine reqline,
      BigDecimal value) {

    try {
      OBContext.setAdminMode();
      String whereClause = " as e where e.efinBudgetint.id= :intId "
          + " and   ( e.accountingCombination.id= :combinationId "
          + "        or e.accountingCombination.id=:toaccountId) ";

      HashMap<String, String> budgetEnquiryMap = new HashMap<String, String>();

      budgetEnquiryMap.put("intId", fundsReqheader.getEfinBudgetint().getId());
      budgetEnquiryMap.put("combinationId", reqline.getFromaccount().getId());
      budgetEnquiryMap.put("toaccountId", reqline.getToaccount().getId());

      List<BaseOBObject> budgetEnquiryList = UtilityDAO.getQueryList(budgetEnquiryMap, whereClause,
          TableIdConstant.BUDGETENQUIRY_ID);

      for (BaseOBObject budgetEnquiryObj : budgetEnquiryList) {
        EfinBudgetInquiry inq = (EfinBudgetInquiry) budgetEnquiryObj;
        if (reqline.getToaccount().getId().equals(inq.getAccountingCombination().getId())) {
          inq.setDisincAmt(inq.getDisincAmt().add(value));
        } else {
          inq.setDisdecAmt(inq.getDisdecAmt().add(value));
        }
        OBDal.getInstance().save(inq);
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
      log.debug("error while updating budget enquiry", e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
