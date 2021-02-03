package sa.elm.ob.scm.ad_process.Requisition;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetControlParam;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.utility.util.Utility;

/**
 * @author Gowtham.V
 */
public class RequisitionDao {
  private static final Logger log = LoggerFactory.getLogger(RequisitionDao.class);

  // remove
  /**
   * check validation for manual encumbrance in requision budget control role.
   * 
   * @param objRequisition
   * @param encumLines
   * @return
   */
  public static boolean checkManualEncumValidation(Requisition objRequisition,
      List<EfinBudgetManencumlines> encumLines) {
    boolean errorFlag = false, combinationMatch = false;
    List<RequisitionLine> objRequisitionLineList = null;
    try {
      OBContext.setAdminMode();
      // checking with requisition line
      objRequisitionLineList = objRequisition.getProcurementRequisitionLineList();
      for (RequisitionLine reqLine : objRequisitionLineList) {
        if (!reqLine.isEscmIssummary()) {
          for (EfinBudgetManencumlines encline : encumLines) {
            if (reqLine.getEfinCValidcombination().equals(encline.getAccountingCombination())) {
              combinationMatch = true;
              BigDecimal remAmt = encline.getRevamount().subtract(encline.getAPPAmt());
              if (reqLine.getLineNetAmount().compareTo(remAmt) > 0) {
                reqLine.setEscmCancelReason(OBMessageUtils.messageBD("Efin_ReqAmt_More"));
                errorFlag = true;
              } else {
                reqLine.setEscmCancelReason("");
              }
              OBDal.getInstance().save(reqLine);
            }
          }

          if (!combinationMatch) {
            reqLine.setEscmCancelReason(OBMessageUtils.messageBD("Efin_ChkEncm_Uniquecode"));
            errorFlag = true;
          } else if (combinationMatch && !errorFlag) {
            reqLine.setEscmCancelReason("");
          }
        }
        OBDal.getInstance().save(reqLine);
        combinationMatch = false;
      }
      return errorFlag;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in checkManualEncumValidation in requisition : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * update applied amt in encumbrance
   * 
   * @param objRequisition
   * @param encumLines
   */
  public static void updateManualEncumAmount(Requisition objRequisition,
      List<EfinBudgetManencumlines> encumLines) {
    List<RequisitionLine> objRequisitionLineList = null;
    try {
      OBContext.setAdminMode();
      objRequisitionLineList = objRequisition.getProcurementRequisitionLineList();

      // checking with requisition line
      for (RequisitionLine reqLineup : objRequisitionLineList) {
        if (!reqLineup.isEscmIssummary()) {
          for (EfinBudgetManencumlines enclineup : encumLines) {
            if (reqLineup.getEfinCValidcombination().equals(enclineup.getAccountingCombination())) {
              log.debug("enclineup:" + enclineup.getAPPAmt());
              enclineup.setAPPAmt(enclineup.getAPPAmt().add(reqLineup.getLineNetAmount()));
              reqLineup.setEfinBudEncumlines(enclineup);
              // enclineup.setSysremamt(enclineup.getSysremamt().subtract(reqLineup.getLineNetAmount()));
              log.debug("enclineup:" + enclineup.getAPPAmt());
              OBDal.getInstance().save(reqLineup);
              OBDal.getInstance().save(enclineup);
            }
          }
        }
      }
      objRequisition.setEfinEncumbered(true);
      OBDal.getInstance().save(objRequisition);
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in updateManualEncumAmount in requisition : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * update Applied amount in encumbrance for requisition reject or cancel.
   * 
   * @param objRequisition
   * @param encumLines
   * @param isCancel
   * @param LineId
   */
  public static void updateManualEncumAmountRej(Requisition objRequisition,
      List<EfinBudgetManencumlines> encumLines, boolean isCancel, String LineId) {
    List<RequisitionLine> objRequisitionLineList = null;
    try {
      OBContext.setAdminMode();
      objRequisitionLineList = objRequisition.getProcurementRequisitionLineList();
      // checking with requisition line
      for (RequisitionLine reqLineup : objRequisitionLineList) {
        if (!isCancel || (isCancel && reqLineup.getId().equals(LineId))) {
          if (!reqLineup.isEscmIssummary()) {
            for (EfinBudgetManencumlines enclineup : encumLines) {
              if (reqLineup.getEfinCValidcombination()
                  .equals(enclineup.getAccountingCombination())) {
                enclineup.setAPPAmt(enclineup.getAPPAmt().subtract(reqLineup.getLineNetAmount()));
                if (isCancel) {
                  /*
                   * Trigger changes enclineup.setRevamount(
                   * enclineup.getRevamount().subtract(reqLineup.getLineNetAmount()));
                   * enclineup.setRemainingAmount(
                   * enclineup.getRemainingAmount().subtract(reqLineup.getLineNetAmount()));
                   */
                } else {
                  reqLineup.setEfinBudEncumlines(null);
                }
                // enclineup.setSysremamt(enclineup.getSysremamt().subtract(reqLineup.getLineNetAmount()));
                OBDal.getInstance().save(reqLineup);
                OBDal.getInstance().save(enclineup);

                // inser encumbrance modification if cancel in PR.
                /*
                 * if (isCancel) { insertEncumbranceModification(enclineup, reqLineup); // update
                 * amt in inquiry
                 * 
                 * Trigger changes EfinEncumbarnceRevision.updateBudgetInquiry(enclineup,
                 * enclineup.getManualEncumbrance(), reqLineup.getLineNetAmount().negate());
                 * 
                 * }
                 */
              }
            }
          }
        }
      }
    } catch (

    final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in updateManualEncumAmountrej in requisition : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * create auto encumbrance and associate in requisition
   * 
   * @param objRequisition
   */
  public static void insertAutoEncumbrance(Requisition objRequisition) {
    /* Date currentDate = new Date(); */
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      OBContext.setAdminMode();
      List<RequisitionLine> reqLineList = null;
      OBQuery<RequisitionLine> reqLineQry = OBDal.getInstance().createQuery(RequisitionLine.class,
          "requisition.id=:reqID and escmIssummary='N'");
      reqLineQry.setNamedParameter("reqID", objRequisition.getId());
      if (reqLineQry.list() != null && reqLineQry.list().size() > 0) {
        reqLineList = reqLineQry.list();
      }
      AccountingCombination com = reqLineList.get(0).getEfinCValidcombination();
      EfinBudgetManencum encum = OBProvider.getInstance().get(EfinBudgetManencum.class);
      encum.setSalesCampaign(reqLineList.get(0).getEfinCValidcombination().getSalesCampaign());
      encum.setEncumType("PRE");
      encum.setSalesRegion(reqLineList.get(0).getEfinCValidcombination().getSalesRegion());
      encum.setEncumMethod("A");
      encum.setEncumStage("PRE");
      encum.setOrganization(objRequisition.getOrganization());
      encum.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setBudgetInitialization(objRequisition.getEfinBudgetint());
      encum.setAction("PD");
      encum.setDescription(objRequisition.getDescription());
      OBDal.getInstance().save(encum);
      OBDal.getInstance().flush();
      for (RequisitionLine reqLine : reqLineList) {
        OBQuery<EfinBudgetManencumlines> encumlineexists = OBDal.getInstance().createQuery(
            EfinBudgetManencumlines.class,
            "as e where e.manualEncumbrance.id=:encumID and e.accountingCombination.id =:acctID ");
        encumlineexists.setNamedParameter("encumID", encum.getId());
        encumlineexists.setNamedParameter("acctID", reqLine.getEfinCValidcombination().getId());
        if (encumlineexists.list() != null && encumlineexists.list().size() > 0) {
          EfinBudgetManencumlines encumLines = encumlineexists.list().get(0);
          encumLines.setAmount(encumLines.getAmount().add(reqLine.getLineNetAmount()));
          encumLines.setRemainingAmount(BigDecimal.ZERO);
          encumLines.setAPPAmt(encumLines.getAPPAmt().add(reqLine.getLineNetAmount()));
          encumLines.setRevamount(encumLines.getRevamount().add(reqLine.getLineNetAmount()));
          OBDal.getInstance().save(encumLines);
          OBDal.getInstance().flush();
          reqLine.setEfinBudEncumlines(encumLines);
          OBDal.getInstance().save(reqLine);
        } else {
          EfinBudgetManencumlines encumLines = OBProvider.getInstance()
              .get(EfinBudgetManencumlines.class);
          JSONObject fundsCheckingObject = null;
          BigDecimal fundsAvailable = BigDecimal.ZERO;
          if (com != null) {
            EfinBudgetIntialization budgetIntialization = Utility
                .getObject(EfinBudgetIntialization.class, encum.getBudgetInitialization().getId());

            try {
              if ("E".equals(com.getEfinDimensiontype())) {
                fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization, com);
                fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
              }
            } catch (Exception e) {
              fundsAvailable = BigDecimal.ZERO;
            }
          }
          encumLines.setManualEncumbrance(encum);
          encumLines.setLineNo(reqLine.getLineNo().longValue());
          encumLines.setAmount(reqLine.getLineNetAmount());
          encumLines.setUsedAmount(BigDecimal.ZERO);
          encumLines.setRemainingAmount(BigDecimal.ZERO);
          encumLines.setFundsAvailable(fundsAvailable);
          encumLines.setAPPAmt(reqLine.getLineNetAmount());
          encumLines.setRevamount(reqLine.getLineNetAmount());
          encumLines.setOrganization(objRequisition.getOrganization());
          encumLines.setSalesRegion(reqLine.getEfinCValidcombination().getSalesRegion());
          encumLines.setAccountElement(reqLine.getEfinCValidcombination().getAccount());
          encumLines.setSalesCampaign(reqLine.getEfinCValidcombination().getSalesCampaign());
          encumLines.setProject(reqLine.getEfinCValidcombination().getProject());
          encumLines.setActivity(reqLine.getEfinCValidcombination().getActivity());
          encumLines.setStDimension(reqLine.getEfinCValidcombination().getStDimension());
          encumLines.setNdDimension(reqLine.getEfinCValidcombination().getNdDimension());
          encumLines.setBusinessPartner(reqLine.getEfinCValidcombination().getBusinessPartner());
          encumLines.setAccountingCombination(reqLine.getEfinCValidcombination());
          encumLines.setUniqueCodeName(reqLine.getEfinCValidcombination().getEfinUniquecodename());
          OBDal.getInstance().save(encumLines);
          OBDal.getInstance().flush();
          reqLine.setEfinBudEncumlines(encumLines);
          OBDal.getInstance().save(reqLine);
        }
      }
      encum.setDocumentStatus("CO");
      OBDal.getInstance().save(encum);
      objRequisition.setEfinBudgetManencum(encum);
      objRequisition.setEfinEncumbered(true);
      OBDal.getInstance().save(objRequisition);
    } catch (OBException e) {
      log.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in updateManualEncumAmount in requisition : ", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * get funds avilable of uniquecode for auto encum in requisition callout.
   * 
   * @param uniqueCode
   * @return
   */
  public static BigDecimal getAutoEncumFundsAvailable(String uniqueCode, String budgetInit) {
    String department = "";
    BigDecimal fundsAvailable = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();

      AccountingCombination validCombination = OBDal.getInstance().get(AccountingCombination.class,
          uniqueCode);
      // if isdepartment fund yes, then check dept level distribution acct.
      if (validCombination.isEFINDepartmentFund()) {
        OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
            "efinBudgetint.id=:budgetInitID and accountingCombination.id=:acctID ");
        budInq.setNamedParameter("budgetInitID", budgetInit);
        budInq.setNamedParameter("acctID", uniqueCode);
        if (budInq.list() != null && budInq.list().size() > 0) {
          fundsAvailable = budInq.list().get(0).getFundsAvailable();
        }
      }
      // if isdepartment fund No, then check Org level distribution acct.
      else {
        OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
            .createQuery(EfinBudgetControlParam.class, "");
        if (bcp.list() != null && bcp.list().size() > 0) {
          department = bcp.list().get(0).getBudgetcontrolCostcenter().getId();
          // getorg level uniquecode
          OBQuery<AccountingCombination> accountCombination = OBDal.getInstance().createQuery(
              AccountingCombination.class,
              " account.id=:acctID and businessPartner.id=:bpartnerID and "
                  + " salesRegion.id=:dept and project.id =:projID and salesCampaign.id=:salesCampID "
                  + " and activity.id=:activityID and stDimension.id=:stDimID and ndDimension.id =:ndDimID"
                  + " and organization.id =:orgID");

          accountCombination.setNamedParameter("acctID", validCombination.getAccount().getId());
          accountCombination.setNamedParameter("bpartnerID",
              validCombination.getBusinessPartner().getId());
          accountCombination.setNamedParameter("dept", department);
          accountCombination.setNamedParameter("projID", validCombination.getProject().getId());
          accountCombination.setNamedParameter("salesCampID",
              validCombination.getSalesCampaign().getId());
          accountCombination.setNamedParameter("activityID",
              validCombination.getActivity().getId());
          accountCombination.setNamedParameter("stDimID",
              validCombination.getStDimension().getId());
          accountCombination.setNamedParameter("ndDimID",
              validCombination.getNdDimension().getId());
          accountCombination.setNamedParameter("orgID", validCombination.getOrganization().getId());

          if (accountCombination.list() != null && accountCombination.list().size() > 0) {
            AccountingCombination combination = accountCombination.list().get(0);
            OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance().createQuery(
                EfinBudgetInquiry.class,
                "efinBudgetint.id=:budgetInitID and accountingCombination.id=:acctID ");
            budInq.setNamedParameter("budgetInitID", budgetInit);
            budInq.setNamedParameter("acctID", combination.getId());
            if (budInq.list() != null && budInq.list().size() > 0) {
              fundsAvailable = budInq.list().get(0).getFundsAvailable();
            }
          }
        }
      }
      return fundsAvailable;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception while getting funds availble in requisition : ", e);
      return fundsAvailable;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // remove
  /**
   * Check funds available for requisition uniqucode
   * 
   * @param headerId
   * @return
   */
  public static int checkFundsAvailableInEnquiry(String headerId) {
    int count = 0;
    boolean acctMatch = false;
    String department = "";
    List<EfinBudgetInquiry> budInqList = null;
    try {
      OBContext.setAdminMode();
      Requisition requisition = OBDal.getInstance().get(Requisition.class, headerId);
      // iterate each line in requisition
      for (RequisitionLine reqLine : requisition.getProcurementRequisitionLineList()) {
        acctMatch = false;
        if (!reqLine.isEscmIssummary()) {
          OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance().createQuery(
              EfinBudgetInquiry.class,
              "efinBudgetint.id='" + requisition.getEfinBudgetint().getId() + "'");
          // if isdepartment fund yes, then check dept level distribution acct.
          if (reqLine.getEfinCValidcombination().isEFINDepartmentFund()) {
            if (budInq.list() != null && budInq.list().size() > 0) {
              budInqList = budInq.list();
              for (EfinBudgetInquiry Enquiry : budInqList) {
                if (reqLine.getEfinCValidcombination() == Enquiry.getAccountingCombination()) {
                  if (reqLine.getLineNetAmount().compareTo(Enquiry.getFundsAvailable()) > 0) {
                    // funds not available
                    reqLine.setEscmCancelReason(OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                    OBDal.getInstance().save(reqLine);
                    count = 1;
                  } else {
                    reqLine.setEscmCancelReason("");
                  }
                  OBDal.getInstance().save(reqLine);
                  acctMatch = true;
                }
              }
              if (!acctMatch) {
                count = 1;
                reqLine.setEscmCancelReason(OBMessageUtils.messageBD("Efin_NoDist_Dept"));
                OBDal.getInstance().save(reqLine);
              } else if (acctMatch && count == 1) {
                reqLine.setEscmCancelReason("");
              }
              OBDal.getInstance().save(reqLine);
            }
          }
          // if isdepartment fund No, then check Org level distribution acct.
          else {
            OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
                .createQuery(EfinBudgetControlParam.class, "");
            if (bcp.list() != null && bcp.list().size() > 0) {
              department = bcp.list().get(0).getBudgetcontrolCostcenter().getId();
              // getorg level uniquecode
              OBQuery<AccountingCombination> accountCombination = OBDal.getInstance().createQuery(
                  AccountingCombination.class,
                  "account.id= '" + reqLine.getEfinCValidcombination().getAccount().getId() + "'"
                      + " and businessPartner.id='"
                      + reqLine.getEfinCValidcombination().getBusinessPartner().getId() + "' "
                      + "and salesRegion.id='" + department + "' and project.id = '"
                      + reqLine.getEfinCValidcombination().getProject().getId() + "' "
                      + "and salesCampaign.id='"
                      + reqLine.getEfinCValidcombination().getSalesCampaign().getId() + "' "
                      + "and activity.id='"
                      + reqLine.getEfinCValidcombination().getActivity().getId()
                      + "' and stDimension.id='"
                      + reqLine.getEfinCValidcombination().getStDimension().getId() + "' "
                      + "and ndDimension.id = '"
                      + reqLine.getEfinCValidcombination().getNdDimension().getId() + "' "
                      + "and organization.id = '"
                      + reqLine.getEfinCValidcombination().getOrganization().getId() + "'");

              if (accountCombination.list() != null && accountCombination.list().size() > 0) {
                AccountingCombination combination = accountCombination.list().get(0);

                if (budInq.list() != null && budInq.list().size() > 0) {
                  budInqList = budInq.list();
                  for (EfinBudgetInquiry Enquiry : budInqList) {
                    if (combination == Enquiry.getAccountingCombination()) {
                      if (reqLine.getLineNetAmount().compareTo(Enquiry.getFundsAvailable()) > 0) {
                        // funds not available
                        reqLine
                            .setEscmCancelReason(OBMessageUtils.messageBD("Efin_Encum_Amt_Error"));
                        OBDal.getInstance().save(reqLine);
                        count = 1;
                      }
                      acctMatch = true;
                    }
                  }
                  if (!acctMatch) {
                    reqLine.setEscmCancelReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                    OBDal.getInstance().save(reqLine);
                    count = 1;
                  }
                }
              } else {
                reqLine.setEscmCancelReason(OBMessageUtils.messageBD("Efin_NoDist_Org"));
                OBDal.getInstance().save(reqLine);
                count = 1;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsAvailableInEnquiry " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }

  /**
   * Update encum amount in enquiry for auto encum in requisition.
   * 
   * @param headerId
   */
  public static void updateAmtInEnquiry(String headerId) {
    String department = "";
    EfinBudgetInquiry allDept = null;
    List<EfinBudgetInquiry> budInqList = null;
    try {
      OBContext.setAdminMode();
      Requisition requisition = OBDal.getInstance().get(Requisition.class, headerId);
      // iterate each line in requisition
      for (RequisitionLine reqLine : requisition.getProcurementRequisitionLineList()) {
        if (!reqLine.isEscmIssummary()) {
          OBQuery<EfinBudgetInquiry> budInq = OBDal.getInstance()
              .createQuery(EfinBudgetInquiry.class, "efinBudgetint.id=:budInitID");
          budInq.setNamedParameter("budInitID", requisition.getEfinBudgetint().getId());
          if (reqLine.getEfinCValidcombination().isEFINDepartmentFund()) {
            if (budInq.list() != null && budInq.list().size() > 0) {
              budInqList = budInq.list();
              for (EfinBudgetInquiry Enquiry : budInqList) {
                if (reqLine.getEfinCValidcombination() == Enquiry.getAccountingCombination()) {
                  Enquiry.setEncumbrance(Enquiry.getEncumbrance().add(reqLine.getLineNetAmount()));
                  OBDal.getInstance().save(Enquiry);
                  if (Enquiry.getParent() != null) {
                    Enquiry.getParent().setEncumbrance(
                        Enquiry.getParent().getEncumbrance().add(reqLine.getLineNetAmount()));
                    allDept = Enquiry.getParent();
                    OBDal.getInstance().save(Enquiry);
                  }
                  if (allDept.getParent() != null) {
                    allDept.getParent().setEncumbrance(
                        allDept.getParent().getEncumbrance().add(reqLine.getLineNetAmount()));
                    OBDal.getInstance().save(allDept);
                  }
                }
              }
            }
          } else {
            OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
                .createQuery(EfinBudgetControlParam.class, "");
            if (bcp.list() != null && bcp.list().size() > 0) {
              department = bcp.list().get(0).getBudgetcontrolCostcenter().getId();

              OBQuery<AccountingCombination> accountCombination = OBDal.getInstance().createQuery(
                  AccountingCombination.class,
                  "account.id=:acctID and businessPartner.id=:bpartnerID and "
                      + " salesRegion.id=:dept and project.id =:projID and "
                      + " salesCampaign.id=:salesCampID and activity.id=:activityID "
                      + " and stDimension.id=:stDimID and ndDimension.id =:ndDimID"
                      + " and organization.id =:orgID");

              accountCombination.setNamedParameter("acctID",
                  reqLine.getEfinCValidcombination().getAccount().getId());
              accountCombination.setNamedParameter("bpartnerID",
                  reqLine.getEfinCValidcombination().getBusinessPartner().getId());
              accountCombination.setNamedParameter("dept", department);
              accountCombination.setNamedParameter("projID",
                  reqLine.getEfinCValidcombination().getProject().getId());
              accountCombination.setNamedParameter("salesCampID",
                  reqLine.getEfinCValidcombination().getSalesCampaign().getId());
              accountCombination.setNamedParameter("activityID",
                  reqLine.getEfinCValidcombination().getActivity().getId());
              accountCombination.setNamedParameter("stDimID",
                  reqLine.getEfinCValidcombination().getStDimension().getId());
              accountCombination.setNamedParameter("ndDimID",
                  reqLine.getEfinCValidcombination().getNdDimension().getId());
              accountCombination.setNamedParameter("orgID",
                  reqLine.getEfinCValidcombination().getOrganization().getId());

              if (accountCombination.list() != null && accountCombination.list().size() > 0) {
                AccountingCombination combination = accountCombination.list().get(0);

                if (budInq.list() != null && budInq.list().size() > 0) {
                  budInqList = budInq.list();
                  for (EfinBudgetInquiry Enquiry : budInqList) {
                    if (combination == Enquiry.getAccountingCombination()) {
                      Enquiry
                          .setEncumbrance(Enquiry.getEncumbrance().add(reqLine.getLineNetAmount()));
                      OBDal.getInstance().save(Enquiry);
                      if (Enquiry.getParent() != null) {
                        Enquiry.getParent().setEncumbrance(
                            Enquiry.getParent().getEncumbrance().add(reqLine.getLineNetAmount()));
                        OBDal.getInstance().save(Enquiry);
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsAvailableInEnquiry " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Update encum amount in enquiry for auto encum in requisition reject or requision cancel.
   * 
   * @param headerId
   * @param iscancel
   * @param lineId
   *          --req line id
   */
  public static void updateAmtInEnquiryRej(String headerId,
      List<EfinBudgetManencumlines> encumLines, boolean isCancel, String lineId) {
    try {
      OBContext.setAdminMode();
      Requisition requisition = OBDal.getInstance().get(Requisition.class, headerId);
      // iterate each line in requisition
      for (RequisitionLine reqLine : requisition.getProcurementRequisitionLineList()) {
        if (!isCancel || (isCancel && reqLine.getId().equals(lineId))) {

          if (!reqLine.isEscmIssummary()) {
            for (EfinBudgetManencumlines enclineup : encumLines) {
              if (reqLine.getEfinCValidcombination().equals(enclineup.getAccountingCombination())) {
                /*
                 * Trigger changes OBQuery<EfinBudgetInquiry> budInq =
                 * OBDal.getInstance().createQuery( EfinBudgetInquiry.class, "efinBudgetint.id='" +
                 * requisition.getEfinBudgetint().getId() + "'"); if
                 * (reqLine.getEfinCValidcombination().isEFINDepartmentFund()) { if (budInq.list()
                 * != null && budInq.list().size() > 0) { budInqList = budInq.list(); for
                 * (EfinBudgetInquiry Enquiry : budInqList) { if (reqLine.getEfinCValidcombination()
                 * == Enquiry .getAccountingCombination()) { Enquiry.setEncumbrance(
                 * Enquiry.getEncumbrance().subtract(reqLine.getLineNetAmount()));
                 * OBDal.getInstance().save(Enquiry); if (Enquiry.getParent() != null) {
                 * Enquiry.getParent().setEncumbrance(Enquiry.getParent().getEncumbrance()
                 * .subtract(reqLine.getLineNetAmount())); allDept = Enquiry.getParent();
                 * OBDal.getInstance().save(Enquiry); } if (allDept.getParent() != null) {
                 * allDept.getParent().setEncumbrance(allDept.getParent().getEncumbrance()
                 * .subtract(reqLine.getLineNetAmount())); OBDal.getInstance().save(allDept); } } }
                 * } } else { OBQuery<EfinBudgetControlParam> bcp = OBDal.getInstance()
                 * .createQuery(EfinBudgetControlParam.class, ""); if (bcp.list() != null &&
                 * bcp.list().size() > 0) { department =
                 * bcp.list().get(0).getBudgetcontrolCostcenter().getId();
                 * 
                 * OBQuery<AccountingCombination> accountCombination = OBDal.getInstance()
                 * .createQuery(AccountingCombination.class, "account.id= '" +
                 * reqLine.getEfinCValidcombination().getAccount().getId() + "'" +
                 * " and businessPartner.id='" +
                 * reqLine.getEfinCValidcombination().getBusinessPartner().getId() + "' " +
                 * "and salesRegion.id='" + department + "' and project.id = '" +
                 * reqLine.getEfinCValidcombination().getProject().getId() + "' " +
                 * "and salesCampaign.id='" +
                 * reqLine.getEfinCValidcombination().getSalesCampaign().getId() + "' " +
                 * "and activity.id='" + reqLine.getEfinCValidcombination().getActivity().getId() +
                 * "' and stDimension.id='" +
                 * reqLine.getEfinCValidcombination().getStDimension().getId() + "' " +
                 * "and ndDimension.id = '" +
                 * reqLine.getEfinCValidcombination().getNdDimension().getId() + "' " +
                 * "and organization.id = '" +
                 * reqLine.getEfinCValidcombination().getOrganization().getId() + "'");
                 * 
                 * if (accountCombination.list() != null && accountCombination.list().size() > 0) {
                 * AccountingCombination combination = accountCombination.list().get(0);
                 * 
                 * if (budInq.list() != null && budInq.list().size() > 0) { budInqList =
                 * budInq.list(); for (EfinBudgetInquiry Enquiry : budInqList) { if (combination ==
                 * Enquiry.getAccountingCombination()) { Enquiry.setEncumbrance(
                 * Enquiry.getEncumbrance().subtract(reqLine.getLineNetAmount()));
                 * OBDal.getInstance().save(Enquiry); if (Enquiry.getParent() != null) {
                 * Enquiry.getParent().setEncumbrance(Enquiry.getParent()
                 * .getEncumbrance().subtract(reqLine.getLineNetAmount()));
                 * OBDal.getInstance().save(Enquiry); } } } } } } }
                 */
                if (isCancel) {
                  // update encum amt

                  // insert modification encumbrance if pr cancel.
                  insertEncumbranceModification(enclineup, reqLine);

                  enclineup.setAPPAmt(enclineup.getAPPAmt().subtract(reqLine.getLineNetAmount()));
                  /*
                   * Trigger changes enclineup
                   * .setRevamount(enclineup.getRevamount().subtract(reqLine.getLineNetAmount()));
                   * enclineup.setRemainingAmount(
                   * enclineup.getRemainingAmount().subtract(reqLine.getLineNetAmount()));
                   */
                  OBDal.getInstance().save(enclineup);
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsAvailableInEnquiry Rej " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Check there is current uniqucode is also present in cost type.
   * 
   * @param objRequisition
   * @return
   */
  public static boolean checkFundsNoCostValidation(Requisition objRequisition) {
    boolean error = false;
    try {
      OBContext.setAdminMode();
      Requisition requisition = OBDal.getInstance().get(Requisition.class, objRequisition.getId());
      for (RequisitionLine line : requisition.getProcurementRequisitionLineList()) {
        if (!line.isEscmIssummary()) {
          OBQuery<AccountingCombination> uniqucode = OBDal.getInstance().createQuery(
              AccountingCombination.class,
              "account.id=:acctID and salesCampaign.efinBudgettype='C' and account.efinFundsonly='N' ");
          uniqucode.setNamedParameter("acctID",
              line.getEfinCValidcombination().getAccount().getId());
          if (uniqucode.list() != null && uniqucode.list().size() > 0) {
            error = true;
            line.setEscmCancelReason(OBMessageUtils.messageBD("Efin_FundsNoCost_Req"));
            OBDal.getInstance().save(line);
          } else {
            line.setEscmCancelReason("");
            OBDal.getInstance().save(line);
          }
        }
      }
      return error;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsNoCostValidation in requisiton " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return error;
  }

  /**
   * While reject after reserverd role should check its encum used or not.
   * 
   * @param objRequisition
   * @return
   */
  public static boolean checkFundsForReject(Requisition objRequisition,
      List<EfinBudgetManencumlines> encumLines) {
    boolean error = true;
    try {
      OBContext.setAdminMode();
      // checking with requisition line
      for (RequisitionLine reqLineup : objRequisition.getProcurementRequisitionLineList()) {
        if (!reqLineup.isEscmIssummary()) {
          for (EfinBudgetManencumlines enclineup : encumLines) {
            if (reqLineup.getEfinCValidcombination().equals(enclineup.getAccountingCombination())) {
              if (enclineup.getUsedAmount().compareTo(BigDecimal.ZERO) > 0) {
                error = false;
              }
            }
          }
        }
      }
      return error;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Exception in  checkFundsForReject in requisiton " + e, e);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return error;
  }

  /**
   * check validation for manual encumbrance all the uniquceode belongs to same encum.
   * 
   * @param objRequisition
   * @param encumLines
   * @return
   */
  public static boolean checkAllUniquecodesameEncum(Requisition objRequisition) {
    boolean errorFlag = false;
    try {
      OBContext.setAdminMode();
      // checking with requisition line
      OBQuery<RequisitionLine> rline = OBDal.getInstance().createQuery(RequisitionLine.class,
          " efinCValidcombination.id not in(select e.accountingCombination.id from Efin_Budget_Manencumlines "
              + "as e where e.manualEncumbrance.id =:encumID) and requisition.id =:reqID ");
      rline.setNamedParameter("encumID", objRequisition.getEfinBudgetManencum().getId());
      rline.setNamedParameter("reqID", objRequisition.getId());
      if (rline.list() != null && rline.list().size() > 0) {
        errorFlag = true;
      }
      return errorFlag;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in checkAllUniquecodesameEncum in requisition : ", e);
      return false;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Insert encum modification while cancel PR.
   * 
   * @param encumbranceline
   * @param decamount
   */
  public static void insertEncumbranceModification(EfinBudgetManencumlines encumbranceline,
      RequisitionLine reqLine) {
    try {
      EfinBudManencumRev manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
      // insert into Manual Encumbrance Revision Table
      manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
      manEncumRev.setOrganization(
          OBDal.getInstance().get(Organization.class, encumbranceline.getOrganization().getId()));
      manEncumRev.setActive(true);
      manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
      manEncumRev.setCreationDate(new java.util.Date());
      manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
      manEncumRev.setUpdated(new java.util.Date());
      manEncumRev.setUniqueCode(encumbranceline.getUniquecode());
      manEncumRev.setManualEncumbranceLines(encumbranceline);
      manEncumRev.setRevdate(new java.util.Date());
      manEncumRev.setStatus("APP");
      manEncumRev.setAuto(true);
      manEncumRev.setRevamount(reqLine.getLineNetAmount().negate());
      manEncumRev.setEncumbranceType("PRE");
      manEncumRev.setRequisitionLine(reqLine);
      manEncumRev.setAccountingCombination(encumbranceline.getAccountingCombination());
      manEncumRev.setSystem(true);
      OBDal.getInstance().save(manEncumRev);
    } catch (Exception e) {
      log.error("Exception in insertEncumbranceModification " + e.getMessage());
    }
  }

  /**
   * if budget controller approved,then try to revoke we have to revert the encumbrance effects.
   * 
   * @param headerCheck
   * @return
   */
  public static void revertencumimpact(Requisition headerCheck) {
    try {
      // get encum line list
      List<EfinBudgetManencumlines> encumLinesList = null;
      OBQuery<EfinBudgetManencumlines> encumLines = OBDal.getInstance()
          .createQuery(EfinBudgetManencumlines.class, " manualEncumbrance.id=:encumID");
      encumLines.setNamedParameter("encumID", headerCheck.getEfinBudgetManencum().getId());
      if (encumLines.list() != null && encumLines.list().size() > 0) {
        encumLinesList = encumLines.list();
      }

      if (headerCheck.getEfinBudgetManencum().getEncumMethod().equals("M")) {
        // update amount
        RequisitionDao.updateManualEncumAmountRej(headerCheck, encumLinesList, false, "");
        headerCheck.setEfinEncumbered(false);
        OBDal.getInstance().save(headerCheck);
      }
      // auto encumbrance
      else {
        RequisitionDao.updateAmtInEnquiryRej(headerCheck.getId(), encumLinesList, false, "");
        // remove encum
        EfinBudgetManencum encum = OBDal.getInstance().get(EfinBudgetManencum.class,
            headerCheck.getEfinBudgetManencum().getId());
        encum.setDocumentStatus("DR");
        headerCheck.setEfinBudgetManencum(null);
        headerCheck.setEscmManualEncumNo("");
        headerCheck.setEfinEncumbered(false);
        OBDal.getInstance().save(headerCheck);
        // remove encum reference in lines.
        List<RequisitionLine> reqLine = headerCheck.getProcurementRequisitionLineList();
        for (RequisitionLine reqLineList : reqLine) {
          reqLineList.setEfinBudEncumlines(null);
          OBDal.getInstance().save(reqLineList);
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().remove(encum);
      }

    } catch (Exception e) {
      log.error("Exception in requisition revoke  " + e.getMessage());
    }

  }
}
