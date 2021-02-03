package sa.elm.ob.scm.ad_process.POandContract;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.hibernate.exception.GenericJDBCException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.OBInterceptor;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBErrorBuilder;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.alert.Alert;
import org.openbravo.model.ad.alert.AlertRecipient;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinEncControl;
import sa.elm.ob.finance.ad_process.PurchaseOrder.OrderFundsCheck;
import sa.elm.ob.finance.ad_process.purchaseRequisition.RequisitionfundsCheck;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.scm.ESCMProductContCatg;
import sa.elm.ob.scm.EscmPoMandatoryLookup;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.actionHandler.PurchaseReleaseDAO;
import sa.elm.ob.scm.ad_callouts.dao.BGWorkbenchDAO;
import sa.elm.ob.scm.ad_process.BidManagement.dao.BidManagementDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.scm.util.AlertUtility;
import sa.elm.ob.scm.util.AlertWindow;
import sa.elm.ob.scm.webservice.epm.service.EmpServiceImpl;
import sa.elm.ob.scm.webservice.epm.service.EpmService;
import sa.elm.ob.utility.EutDocappDelegateln;
import sa.elm.ob.utility.EutNextRole;
import sa.elm.ob.utility.EutNextRoleLine;
import sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.dao.DocumentRuleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.DelegatedNextRoleDAOImpl;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRule;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.properties.Resource;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.DocumentTypeE;
import sa.elm.ob.utility.util.Preferences;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * @author gopalakrishnan on 27/07/2017
 */

public class POContractSummaryAction extends DalBaseProcess {
  /**
   * This servlet is responsible for the Doc Actions in Purchase order
   */

  private static final Logger log = LoggerFactory.getLogger(POContractSummaryAction.class);
  private static String errorMsgs = null;
  public static final String PO_Window_ID = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";

  @SuppressWarnings("unchecked")
  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    // TODO Auto-generated method stub
    HttpServletRequest request = RequestContext.get().getRequest();
    VariablesSecureApp vars = new VariablesSecureApp(request);
    String Lang = vars.getLanguage();
    Connection conn = OBDal.getInstance().getConnection();
    String appstatus = "";
    boolean errorFlag = false;
    boolean allowUpdate = false, allowApprove = false;
    boolean allowDelegation = false, mixedencumbrance = false;
    boolean isPeriodOpen = true;
    boolean checkEncumbranceAmountZero = false;
    BigDecimal releaseAmt = BigDecimal.ZERO;
    String region_org_id = "";
    PreparedStatement ps = null;
    List<EfinBudgetManencumlines> encumLinelist = new ArrayList<EfinBudgetManencumlines>();
    try {
      OBContext.setAdminMode();
      final String strOrderId = (String) bundle.getParams().get("C_Order_ID");
      Order objOrder = OBDal.getInstance().get(Order.class, strOrderId);
      Entity orderEntity = objOrder.getEntity();
      String DocStatus = objOrder.getEscmAppstatus();
      String DocAction = objOrder.getEscmDocaction();
      NextRoleByRuleVO nextApproval = null;
      final String clientId = bundle.getContext().getClient();
      final String orgId = objOrder.getOrganization().getId();
      final String userId = bundle.getContext().getUser();
      final String roleId = bundle.getContext().getRole();
      BigDecimal qtyCount1 = BigDecimal.ZERO;
      BigDecimal qtyCount2 = BigDecimal.ZERO;
      JSONObject resultfundsavail = null;
      Date currentDate = new Date();
      List<EfinBudgetManencumlines> encumLineList = null;
      int count = 0;
      String sql = "", encumId = "", preferenceValue = "";
      int promgmtlncount = 0;
      JSONObject resultEncum = null;
      Boolean chkRoleIsInDocRul;
      String comments = (String) bundle.getParams().get("notes");
      ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
      String documentRule = null;
      Boolean releaseAmtGreater = Boolean.FALSE;
      String encAmount, encRemAmount = null;
      BigDecimal amount, remAmount = BigDecimal.ZERO;
      Boolean isAmtlesser = false;
      Date check_period_date = new Date();
      String created_user_id = objOrder.getCreatedBy().getId();
      Organization object_reg_org = UtilityDAO.getUserRegion(created_user_id, clientId);
      String fieldName = "";
      Date orderDate = objOrder.getOrderDate();

      // Check Mandatory fields based on setup in reference lookup configuration
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_RA")) {

        List<String> mandatoryColumns = new ArrayList<String>();
        List<EscmPoMandatoryLookup> mandatoryLookups = objOrder.getEscmContactType() != null
            ? objOrder.getEscmContactType().getEscmPomandatoryLookupList()
            : null;

        if (mandatoryLookups != null) {
          mandatoryColumns = mandatoryLookups.stream().map(a -> a.getField().getColumn().getId())
              .collect(Collectors.toList());

          for (Property prop : orderEntity.getProperties()) {
            if (mandatoryColumns.contains(prop.getColumnId())) {
              if (objOrder.get(prop.getName()) == null) {

                if (OBContext.getOBContext().getLanguage().getLanguage().equals("ar_SA")) {
                  String fieldId = mandatoryLookups.stream()
                      .filter(a -> a.getField().getColumn().getId().equals(prop.getColumnId()))
                      .map(a -> a.getField().getId()).collect(Collectors.joining(","));
                  OBQuery<FieldTrl> transaltion = OBDal.getInstance().createQuery(FieldTrl.class,
                      "as e where e.field.id =:fieldId");
                  transaltion.setNamedParameter("fieldId", fieldId);
                  if (transaltion != null && transaltion.list().size() > 0) {
                    fieldName = transaltion.list().get(0).getName();
                  }
                } else {
                  fieldName = mandatoryLookups.stream()
                      .filter(a -> a.getField().getColumn().getId().equals(prop.getColumnId()))
                      .map(a -> a.getField().getName()).collect(Collectors.joining(","));
                }

                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error", "");
                result.setMessage(
                    OBMessageUtils.messageBD("Escm_Po_mandatoryfields").replace("%", fieldName)
                        .replace("$", objOrder.getEscmContactType().getCommercialName()));
                bundle.setResult(result);
                return;
              }

            }

          }

        }

        if (objOrder.getEscmProposalmgmt() != null) {
          EscmProposalMgmt proposal = OBDal.getInstance().get(EscmProposalMgmt.class,
              objOrder.getEscmProposalmgmt().getId());
          proposal.setDocumentNo(objOrder);

          OBQuery<EscmProposalAttribute> attrQry = OBDal.getInstance().createQuery(
              EscmProposalAttribute.class,
              " as e where e.escmProposalmgmt.id='" + objOrder.getEscmProposalmgmt().getId() + "'");
          if (attrQry != null && attrQry.list().size() > 0) {
            EscmProposalAttribute attrObj = OBDal.getInstance().get(EscmProposalAttribute.class,
                attrQry.list().get(0).getId());
            attrObj.setOrder(objOrder);
          }

        }

        if (objOrder.isEscmIspaymentschedule()) {

          // Updating the Payment Schedule -> Invoiced Amount if there exist parent ID
          String sqlQuery = " update escm_payment_schedule set invoiced_amt = subquery.invoiced_amt, "
              + " old_contract = subquery.old_contract "
              + " from (select a.escm_payment_schedule_id,b.invoiced_amt,b.old_contract from escm_payment_schedule a "
              + " join escm_payment_schedule b on a.parent_id=b.escm_payment_schedule_id "
              + " where a.parent_id is not null and a.c_order_id= ?) as subquery "
              + " where escm_payment_schedule.escm_payment_schedule_id=subquery.escm_payment_schedule_id "
              + " and escm_payment_schedule.invoiced_amt != subquery.invoiced_amt ";
          ps = conn.prepareStatement(sqlQuery);
          ps.setString(1, objOrder.getId());
          ps.executeUpdate();
          ps.close();

          // -------- Payment Schedule Validation Starts Here ---------

          // Payment Schedule -> sum of Amount should be equal to Net Total Amount
          List<ESCMPaymentSchedule> pslist = objOrder.getEscmPaymentScheduleList();
          BigDecimal amt = BigDecimal.ZERO;
          for (ESCMPaymentSchedule paymentSchedule : pslist) {
            if (paymentSchedule.getAmount() != null) {
              // Payment Schedule amount should be greater than or equal to invoiced amount
              if (paymentSchedule.getAmount().compareTo(paymentSchedule.getInvoicedAmt()) < 0) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_PaymentScheduleAmt_lt_invAmt@");
                bundle.setResult(result);
                return;
              }
              amt = amt.add(paymentSchedule.getAmount());
            }
            // Payment Schedule need by date is mandatory & should be greater than PO date
            if (paymentSchedule.getNeedbydate() != null) {
              if (paymentSchedule.getNeedbydate().compareTo(orderDate) <= 0) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_PaySchedule_Needbydate_Mandatory@");
                bundle.setResult(result);
                return;
              }
            } else {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_PaySchedule_Needbydate_Mandatory@");
              bundle.setResult(result);
              return;
            }
          }
          if (objOrder.getGrandTotalAmount().compareTo(amt) != 0) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_PO_paymentamt_eq_poamt@");
            bundle.setResult(result);
            return;
          }

          // All Payment Schedule except old contract, should have Unique code or shouldn't have
          // Unique code
          pslist = objOrder.getEscmPaymentScheduleList().stream()
              .filter(a -> a.isOLDContract().equals(false)).collect(Collectors.toList());
          int initial = 0;
          Boolean empty = false, emptyTemp = false;
          for (ESCMPaymentSchedule paymentSchedule : pslist) {
            if (initial == 0)
              empty = (paymentSchedule.getValidcombination() != null) ? false : true;
            else {
              emptyTemp = (paymentSchedule.getValidcombination() != null) ? false : true;
              if (!empty.equals(emptyTemp)) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_PaymentSchedule_UniqueCode@");
                bundle.setResult(result);
                return;
              }
            }
            initial++;
          }

          // Payment Schedule final pay should be equal to retainage value
          amt = BigDecimal.ZERO;
          pslist = objOrder.getEscmPaymentScheduleList().stream()
              .filter(a -> a.getPAYNature().equals("FP")).collect(Collectors.toList());
          BigDecimal retainageAmt = objOrder.getEscmRetainageAmt() != null
              ? objOrder.getEscmRetainageAmt()
              : BigDecimal.ZERO;
          if (pslist.size() > 0) {
            if (pslist.size() == 1) {
              ESCMPaymentSchedule paymentSchedule = pslist.get(0);
              amt = paymentSchedule.getAmount() != null ? paymentSchedule.getAmount()
                  : BigDecimal.ZERO;
              if (amt.compareTo(retainageAmt) != 0 && objOrder.isEscmIsadvancepayment()) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_PaymentSchedule_FinalPay_eq_Retainage@");
                bundle.setResult(result);
                return;
              }
            } else {
              for (ESCMPaymentSchedule paymentSchedule : pslist) {
                if (paymentSchedule.getAmount() != null)
                  amt = amt.add(paymentSchedule.getAmount());
              }
              if (amt.compareTo(retainageAmt) != 0 && objOrder.isEscmIsadvancepayment()) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_PaymentSchedule_FinalPay_eq_Retainage@");
                bundle.setResult(result);
                return;
              }
            }
          } else {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_PaymentSchedule_FinalPay_notExist@");
            bundle.setResult(result);
            return;
          }

          // If parent id doesnot exist then payment schedule zero is not allowed
          pslist = objOrder.getEscmPaymentScheduleList().stream().filter(a -> a.getParent() == null)
              .collect(Collectors.toList());
          for (ESCMPaymentSchedule paymentSchedule : pslist) {
            amt = paymentSchedule.getAmount() != null ? paymentSchedule.getAmount()
                : BigDecimal.ZERO;
            if (amt.compareTo(BigDecimal.ZERO) == 0) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_PaymentSchedule_Amt_Zero@");
              bundle.setResult(result);
              return;
            }
          }

          pslist = objOrder.getEscmPaymentScheduleList();
          Map<String, BigDecimal> psUCAmtWCMap = new HashMap<String, BigDecimal>();
          Map<String, BigDecimal> psUCAmtWOCMap = new HashMap<String, BigDecimal>();
          Map<String, BigDecimal> psUCNetLineAmtCMap = new HashMap<String, BigDecimal>();

          // If Unique Code exist then summing up the Payment Schedule-> Amount based on
          // (Contract & without Contract) with Unique Code
          for (ESCMPaymentSchedule paymentSchedule : pslist) {
            if (paymentSchedule.getValidcombination() != null) {
              if (paymentSchedule.isOLDContract()) {
                if (psUCAmtWCMap.get(paymentSchedule.getValidcombination().getId()) == null)
                  psUCAmtWCMap.put(paymentSchedule.getValidcombination().getId(),
                      paymentSchedule.getAmount());
                else
                  psUCAmtWCMap.put(paymentSchedule.getValidcombination().getId(),
                      psUCAmtWCMap.get(paymentSchedule.getValidcombination().getId())
                          .add(paymentSchedule.getAmount()));

              } else {
                if (psUCAmtWOCMap.get(paymentSchedule.getValidcombination().getId()) == null)
                  psUCAmtWOCMap.put(paymentSchedule.getValidcombination().getId(),
                      paymentSchedule.getAmount());
                else
                  psUCAmtWOCMap.put(paymentSchedule.getValidcombination().getId(),
                      psUCAmtWOCMap.get(paymentSchedule.getValidcombination().getId())
                          .add(paymentSchedule.getAmount()));

              }
            }
          }

          // If Unique Code exist then summing up the OrderLine-> Net Line Amount based on the
          // Unique Code
          for (OrderLine orderLineObj : objOrder.getOrderLineList()) {
            if (orderLineObj.getEFINUniqueCode() != null) {
              if (psUCNetLineAmtCMap.get(orderLineObj.getEFINUniqueCode().getId()) == null)
                psUCNetLineAmtCMap.put(orderLineObj.getEFINUniqueCode().getId(),
                    orderLineObj.getLineNetAmount());
              else
                psUCNetLineAmtCMap.put(orderLineObj.getEFINUniqueCode().getId(),
                    psUCNetLineAmtCMap.get(orderLineObj.getEFINUniqueCode().getId())
                        .add(orderLineObj.getLineNetAmount()));
            }
          }

          for (String uniqueCodeId : psUCNetLineAmtCMap.keySet()) {
            if (psUCAmtWCMap.get(uniqueCodeId) != null) {
              // If unique code & old contract exist then amount should be less or equal to
              // uniquecode
              // level(order line- net total amount)
              if (psUCAmtWCMap.get(uniqueCodeId)
                  .compareTo(psUCNetLineAmtCMap.get(uniqueCodeId)) > 0) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_PaySch_UniqueCodeLvl_WC_AmtVal@");
                bundle.setResult(result);
                return;
              }
            }
            if (psUCAmtWOCMap.get(uniqueCodeId) != null) {
              // If unique code & no old contract then amount should be equal to uniquecode
              // level(order line- net total amount)
              if (psUCAmtWOCMap.get(uniqueCodeId)
                  .compareTo(psUCNetLineAmtCMap.get(uniqueCodeId)) != 0) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_PaySch_UniqueCodeLvl_WOC_AmtVal@");
                bundle.setResult(result);
                return;
              }
            }
          }
          // -------- Payment Schedule Validation Ends Here ---------
        }
      }

      // Attachment is mandatory if preference added
      String preferenceAttachValue = UtilityDAO.getAttachmentPref(
          Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W, objOrder.getClient().getId());

      if (preferenceAttachValue != null && preferenceAttachValue.equals("Y")
          || objOrder.isEscmIssecondsupplier()) {
        // attachment is mandatory
        OBQuery<Attachment> file = OBDal.getInstance().createQuery(Attachment.class,
            " as e where e.record=:Record");
        file.setNamedParameter("Record", strOrderId);
        if (file != null && file.list().size() == 0) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@EUT_Attachment_Mandatory@");
          bundle.setResult(result);
          return;
        }
      }
      // Throw error if PO tax method is inactive
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("ESCM_RA")) {

        if (objOrder.getEscmProposalmgmt() != null) {
          if (!objOrder.isEscmIstax() || objOrder.getEscmTaxMethod() == null) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_POTaxMand_ProposalRef@");
            bundle.setResult(result);
            return;
          }
        }

        if (objOrder.isEscmIstax()) {
          if (objOrder.getEscmTaxMethod() != null) {

            boolean isTaxMethodActive = objOrder.getEscmTaxMethod().isActive();
            if (!isTaxMethodActive) {
              OBDal.getInstance().rollbackAndClose();
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PoTaxInactive@");
              bundle.setResult(result);
              return;
            }
          }
        }
      }

      // Throw error if Proposal has tax and PO doesn't have tax
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("ESCM_RA")) {
        if (objOrder.getEscmProposalmgmt() != null) {
          if (objOrder.getEscmProposalmgmt().isTaxLine()
              && objOrder.getEscmProposalmgmt().getEfinTaxMethod() != null
              && !objOrder.isEscmIstax() && objOrder.getEscmTaxMethod() == null) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_ProposalHasTax@");
            bundle.setResult(result);
            return;
          }
        }
      }

      if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
        documentRule = Resource.PURCHASE_AGREEMENT_RULE;
      } else {
        documentRule = Resource.PURCHASE_ORDER_RULE;
      }

      if (objOrder.getEfinBudgetint().getStatus().equals("CL")) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_Budget_Definition_Closed@");
        bundle.setResult(result);
        return;
      }
      // Budget Definition is Pre closed validation
      if (objOrder.getEfinBudgetint().isPreclose()) {

        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Efin_PreClose_Year_Validation@");
        bundle.setResult(result);
        return;

      }
      // Task No.5925
      List<EfinEncControl> enccontrollist = new ArrayList<EfinEncControl>();
      enccontrollist = POContractSummaryDAO.getEncControleList(objOrder);
      // End Task No.5925

      if (objOrder.isEscmIstax() && objOrder.getEscmTaxMethod() != null
          && objOrder.getGrandTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_OrderAmount_Not_Zero@");
        bundle.setResult(result);
        return;
      }

      if (objOrder.getEscmBaseOrder() != null) {
        boolean isRDVCreated = POContractSummaryDAO.checkRDVIsCreated(objOrder);
        if (isRDVCreated) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_NotAllowChangeUniqueCode@");
          bundle.setResult(result);
          return;
        }
      }
      if (objOrder.isEscmIstax() && objOrder.getEscmTaxMethod() != null) {
        if (objOrder.getOrderLineList().size() > 0) {
          for (OrderLine ordline : objOrder.getOrderLineList()) {
            if (!ordline.isEscmIssummarylevel()) {
              if (ordline.getEscmLineTaxamt().compareTo(BigDecimal.ZERO) == 0
                  && ordline.getUnitPrice().compareTo(BigDecimal.ZERO) > 0) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_NeedToCalTax@");
                bundle.setResult(result);
                return;
              }
            }
          }
        }
      }
      if (objOrder.getEscmContactType() == null) {
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@ESCM_ContractCatgCantBeEmpty@");
        bundle.setResult(result);
        return;
      }
      // TO check isTax is false and tax amt greater than zero v should allow for next approval
      if (!objOrder.isEscmIstax() && objOrder.getEscmTotalTaxamt().compareTo(BigDecimal.ZERO) > 0) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_TaxExists@");
        bundle.setResult(result);
        return;
      }
      // order amount should not be zero
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("ESCM_RA")
          || DocStatus.equals("ESCM_IP")) {
        if (objOrder.getGrandTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_OrderAmount_Not_Zero@");
          bundle.setResult(result);
          return;
        }
        if (!DocStatus.equals("ESCM_IP")) {
          if (objOrder.getEscmTotPoChangeType() != null
              && objOrder.getEscmTotPoChangeValue() != null
              && objOrder.getEscmTotPoChangeFactor() != null
              && objOrder.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
            boolean isPOFactApplied = POContractSummaryDAO.checkPOFactorApplied(objOrder);
            if (!isPOFactApplied) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_POChangeFactNotApplied@");
              bundle.setResult(result);
              return;
            }
          }
          mixedencumbrance = POContractSummaryDAO.checkmixedPREncumbrance(objOrder);
          if (mixedencumbrance) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Efin_BidPRMixedEncumbrance@");
            bundle.setResult(result);
            return;
          }
        }
      }
      // Check the contract attributes is exist if the order type is not PO
      if (!"PUR".equalsIgnoreCase(objOrder.getEscmOrdertype())) {
        if (!objOrder.isEscmIspurchaseagreement() && !isContractAttributesExist(objOrder)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_Order_Contract_Att_Not_Exist@");
          bundle.setResult(result);
          return;
        }
      }
      if ("PUR_REL".equalsIgnoreCase(objOrder.getEscmOrdertype())) {
        Order order = OBDal.getInstance().get(Order.class,
            objOrder.getEscmPurchaseagreement().getId());
        if (order != null) {
          if (order.getEscmAppstatus().equals("ESCM_WD")
              || order.getEscmAppstatus().equals("ESCM_OHLD")) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_CannotSubmit_PurchaseRL@");
            bundle.setResult(result);
            return;
          }
        }
      }

      if (objOrder.getEscmContactType() == null && !objOrder.getEscmOrdertype().equals("PUR_AG")
          && !objOrder.getEscmOrdertype().equals("PUR_REL")) {
        for (OrderLine line : objOrder.getOrderLineList()) {
          if (line.getEscmProposalmgmt() == null && line.getEfinMRequisitionline() == null) {
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_POContCatgIsMand@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // Check whether the product belongs to that contract category or not
      if (objOrder.getEscmContactType() != null) {
        String contCatgId = objOrder.getEscmContactType().getId();
        String lineNo = "";
        boolean hasError = false;
        for (OrderLine line : objOrder.getOrderLineList()) {
          if (line.getProduct() != null) {
            if (line.getProduct().getESCMPRODCONTCATGList() != null
                && line.getProduct().getESCMPRODCONTCATGList().size() != 0) {
              boolean contCatgMatch = true;
              for (ESCMProductContCatg pContCatg : line.getProduct().getESCMPRODCONTCATGList()) {
                if (pContCatg.getContractCategory().getId().equals(contCatgId)) {
                  contCatgMatch = true;
                  break;
                } else {
                  if (contCatgId.equals(Utility.getConCatTypeOther())) {
                    contCatgMatch = true;
                    break;
                  } else {
                    contCatgMatch = false;
                  }
                }
              }
              if (!contCatgMatch) {
                hasError = true;
                if (!lineNo.equals("")) {
                  lineNo = lineNo.concat(", ");
                }
                lineNo = lineNo.concat(line.getLineNo().toString());
              }
            }
          }
        }
        if (hasError) {
          String errorMsg = OBMessageUtils.messageBD("ESCM_ItemMismatchContCatgLineNo");
          errorMsg = errorMsg.replace("%", lineNo);
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsg);
          bundle.setResult(result);
          return;
        }
      }
      // check all the lines belongs to same department in requisition.
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) {
        boolean deptSame = POContractSummaryDAO.checkSameDept(objOrder.getId());
        if (deptSame) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_BidMultiDep@");
          bundle.setResult(result);
          return;
        }
      }

      // check if line amount in new version is greater than released amount

      if (objOrder.getEscmOldOrder() != null) {
        Order order = OBDal.getInstance().get(Order.class, objOrder.getEscmOldOrder().getId());
        if (objOrder.getEscmReceivetype().equals("AMT")
            && order.getEscmReleaseamount().compareTo(objOrder.getSummedLineAmount()) > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AmtLessThanReleaseAmt@");
          bundle.setResult(result);
          return;
        } else if (objOrder.getEscmReceivetype().equals("QTY")) {
          if (POContractSummaryDAO.totalLineQty(objOrder)) {
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_QtyLessThanReleaseQty@");
            bundle.setResult(result);
            return;
          }
        }
      }
      if (objOrder.getEscmRevision() == 0) {
        check_period_date = objOrder.getOrderDate();
      }
      // Check transaction period is opened or not
      isPeriodOpen = Utility.checkOpenPeriod(check_period_date,
          orgId.equals("0") ? vars.getOrg() : orgId, objOrder.getClient().getId());
      if (!isPeriodOpen) {
        errorFlag = true;
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@PeriodNotAvailable@");
        bundle.setResult(result);
        return;
      }

      // check document type match
      if (!objOrder.isEscmIspurchaseagreement()
          && !POContractSummaryDAO.checkDocTypeConfig(objOrder.getClient().getId(),
              objOrder.getOrganization().getId(), objOrder.getGrandTotalAmount(),
              objOrder.getEscmOrdertype())) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_ConfigDocTypeNotMatch@");
        bundle.setResult(result);
        return;
      }
      // check amt utilized
      if (objOrder.getEscmBaseOrder() != null && !objOrder.isEscmIspurchaseagreement()) {
        if (POContractSummaryDAO.checkTotalAmtUtilized(objOrder.getDocumentNo(), "Y",
            objOrder.getEscmOrdertype(), objOrder.getClient().getId(),
            objOrder.getGrandTotalAmount(), objOrder)) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_POAmtUtilized@");
          bundle.setResult(result);
          return;
        }
      }

      // need to check amount limit for agreement new version.
      if (objOrder.getEscmBaseOrder() != null && objOrder.isEscmIspurchaseagreement()
          && objOrder.getEscmOrdertype().equals("PUR_AG")) {
        if (objOrder.getEscmMaxRelease() != null
            && objOrder.getEscmMaxRelease().compareTo(BigDecimal.ZERO) > 0
            && objOrder.getGrandTotalAmount().compareTo(objOrder.getEscmMaxRelease()) > 0) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_POAmtUtilized@");
          bundle.setResult(result);
          return;
        }
      }

      // Advance Payment mandatory if type is contract
      if (objOrder.getEscmOrdertype().equals("CR")) {
        if (objOrder.getEscmAdvpaymntPercntge() == null || objOrder.getEscmAdvpaymntAmt() == null
            || objOrder.getEscmRetainPercn() == null || objOrder.getEscmTotretainAmt() == null) {
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_AdvAmt_Mandtory@");
          bundle.setResult(result);
          return;
        }
      }

      if (objOrder.getEscmMotcontperson() == null || objOrder.getEscmMotcontposition() == null) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_POMoTFieldCantEmpty@");
        bundle.setResult(result);
        return;
      }
      // check quantity is matchecd for refresh version record.
      if (objOrder.getEscmOldOrder() != null) {
        // check old po and new po qty is same else dont allow to approve.
        if (objOrder.getEscmOldOrder() != null) {

          /* check ordered qty has less qty than received qty */
          if (objOrder.getEscmReceivetype().equals("QTY")) {
            if (POContractSummaryDAO.checkOlderVersionQty(objOrder.getId(),
                objOrder.getEscmOldOrder().getId())) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_PONewQtyCantBeLess@");
              bundle.setResult(result);
              return;
            }
            // check valid_change =1 then PO new qty and amount are less than the earlier receipt
            // qty and amount
            // valid_change=2 then qty is stil same as received qty
            // but the unit price changed should not allowed to avoid issue in encumbrance
            int valid_change = POContractSummaryDAO.checkOlderVersionQtyAndUnitPrice(
                objOrder.getId(), objOrder.getEscmOldOrder().getId());
            if (valid_change > 0) {
              if (valid_change == 1) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_PONewQtyAmountCantBeLess@");
                bundle.setResult(result);
                return;
              }
              if (valid_change == 2) {
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@ESCM_POUnitPriceChangeNotAllowed@");
                bundle.setResult(result);
                return;
              }

            }
          }
          // Checks whether new order amount is lesser than old order received amount
          if (objOrder.getEscmReceivetype().equals("AMT")) {
            isAmtlesser = POContractSummaryDAO.checkOlderVersionAmt(objOrder.getId(),
                objOrder.getEscmOldOrder().getId());
            if (isAmtlesser) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@ESCM_PONewAmtCantBeLess@");
              bundle.setResult(result);
              return;
            }
          }

          qtyCount1 = POContractSummaryDAO.getPendingqtycount(objOrder.getId());
          qtyCount2 = POContractSummaryDAO.getPendingqtycount(objOrder.getEscmOldOrder().getId());
          if (qtyCount1.compareTo(qtyCount2) != 0) {
            if (DocStatus.equals("ESCM_RA") || DocStatus.equals("ESCM_REJ")) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Escm_PO_RefreshQty_Diff_Sub@");
              bundle.setResult(result);
              return;
            } else {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Escm_PO_RefreshQty_Diff@");
              bundle.setResult(result);
              return;
            }

          }
        }
      }
      // If the record is Forwarded or given RMI then throw error when any other user tries to
      // approve the record without refreshing the page
      if (objOrder.getEutForward() != null) {
        allowApprove = forwardReqMoreInfoDAO.allowApproveReject(objOrder.getEutForward(), userId,
            roleId, documentRule);
      }
      if (objOrder.getEutReqmoreinfo() != null
          || ((objOrder.getEutForward() != null) && (!allowApprove))) {
        OBDal.getInstance().rollbackAndClose();
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@Escm_AlreadyPreocessed_Approved@");
        bundle.setResult(result);
        return;
      }
      // check role is present in document rule or not
      if (DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) {
        region_org_id = object_reg_org != null ? object_reg_org.getId() : orgId;
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRul(OBDal.getInstance().getConnection(),
            clientId, region_org_id, userId, roleId, documentRule, objOrder.getGrandTotalAmount());
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
        // // check role is present in document rule or not based on amount
        chkRoleIsInDocRul = UtilityDAO.chkRoleIsInDocRulBasedonAmount(
            OBDal.getInstance().getConnection(), clientId, region_org_id, userId, roleId,
            documentRule, objOrder.getGrandTotalAmount());
        if (!chkRoleIsInDocRul) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RoleIsNotIncInDocRule@");
          bundle.setResult(result);
          return;
        }
        // check PO lines equal to Proposal Lines
        if (objOrder.getEscmProposalmgmt() != null && objOrder.getEscmOldOrder() == null) {
          // restrict to count cancelled proposal management line - task no.- 6660 -note 17867
          for (EscmProposalmgmtLine promgmtln : objOrder.getEscmProposalmgmt()
              .getEscmProposalmgmtLineList()) {
            if ((promgmtln.getStatus() == null) || (!promgmtln.getStatus().equals("CL"))) {
              promgmtlncount = promgmtlncount + 1;
            }
          }
          if ((objOrder.getOrderLineList().size() - promgmtlncount) != 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@Escm_propsal_orderlines(not match)@");
            bundle.setResult(result);
            return;
          }
        }

        // chk qty is zero
        for (OrderLine line : objOrder.getOrderLineList()) {
          if (line.getOrderedQuantity().compareTo(BigDecimal.ZERO) == 0) {
            errorFlag = true;
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_IR_Quantity@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // check proposal is not cancelled.
      if (objOrder.getEscmProposalmgmt() != null
          && !objOrder.getEscmProposalmgmt().getProposalstatus().equals("AWD")
          && !objOrder.getEscmProposalmgmt().getProposalstatus().equals("PAWD")) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Proposal_cancelled@");
        bundle.setResult(result);
        return;
      }

      // chk order is 100 % BG
      if (!objOrder.getEscmOrdertype().equals("PUR_AG")) {
        if (objOrder != null && objOrder.getEscmProposalmgmt() != null
            && objOrder.getEscmProposalmgmt().getEscmBidmgmt() != null
            && !objOrder.getEscmProposalmgmt().getEscmBidmgmt().getBidtype().equals("DR")
            && (objOrder.getEscmOldOrder() == null)) {
          String bgworkbenchId = POContractSummaryDAO.getBgworkbenchd(objOrder.getId());
          if (bgworkbenchId != null) {
            errorFlag = BGWorkbenchDAO.chkTotBGAmtCalAmt(bgworkbenchId, true);
          } else
            errorFlag = true;
          if (errorFlag) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_BGHundPerValidOrd@");
            bundle.setResult(result);
            return;
          }
        }
      }
      if (objOrder != null && objOrder.isEscmPocopy()) {
        if (objOrder.getEscmContactType() != null) {
          ESCMDefLookupsTypeLn lookupLn = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
              objOrder.getEscmContactType().getId());
          if (lookupLn.isBankguaranteemandatory()) {
            String fbgworkbenchId = POContractSummaryDAO.getFinalBgworkbenchd(objOrder.getId());
            if (fbgworkbenchId == null) {
              OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Mandatory_Bg@");
              bundle.setResult(result);
              return;
            }
          }
        }
      }
      System.out.println("orderprop:" + objOrder.getEscmProposalmgmt());
      // chk qty match only for requisition
      if (objOrder.getEscmProposalmgmt() == null) {
        if (objOrder.getOrderLineList().size() > 0) {
          List<String> lineNos = new ArrayList<String>();
          String lineno = null;
          OBQuery<OrderLine> reqLines = OBDal.getInstance().createQuery(OrderLine.class,
              " as e where e.salesOrder.id=:orderId and e.efinMRequisitionline is not null");
          reqLines.setNamedParameter("orderId", objOrder.getId());

          List<OrderLine> reqLinesList = reqLines.list();
          for (OrderLine line : reqLinesList) {
            sql = " select coalesce(sum(quantity),0) from escm_ordersource_ref where  c_orderline_id ='"
                + line.getId() + "' ";
            SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
            if (query != null) {
              List<Object> rows = query.list();
              if (rows.size() > 0) {
                if (((BigDecimal) rows.get(0)) != null
                    && ((BigDecimal) rows.get(0)).compareTo(line.getOrderedQuantity()) != 0) {
                  errorFlag = true;

                  lineNos.add(line.getLineNo().toString());
                  lineno = StringUtils.join(lineNos, ",");
                }
              }
            }
          }

          if (errorFlag) {
            OBDal.getInstance().rollbackAndClose();
            String message = OBMessageUtils.messageBD("ESCM_BidMgmLine_GrtSrcQty");
            message = message.replace("%", lineno);
            OBError result = OBErrorBuilder.buildMessage(null, "error", message);
            bundle.setResult(result);
            return;
          }
        }
      }

      /*
       * // chk shipment Qty if (objOrder.getOrderLineList().size() > 0) { String lineno = null;
       * List<String> lineNos = new ArrayList<String>(); for (OrderLine line :
       * objOrder.getOrderLineList()) { sql =
       * " select coalesce(sum(movementqty),0) from escm_ordershipment where  c_orderline_id ='" +
       * line.getId() + "' "; SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
       * if (query != null) { List<Object> rows = query.list(); if (rows.size() > 0) { if
       * (((BigDecimal) rows.get(0)) != null && ((BigDecimal)
       * rows.get(0)).compareTo(line.getOrderedQuantity()) != 0) { errorFlag = true;
       * lineNos.add(line.getLineNo().toString()); lineno = StringUtils.join(lineNos, ","); } } } }
       * 
       * if (errorFlag) { OBDal.getInstance().rollbackAndClose(); String message =
       * OBMessageUtils.messageBD("Escm_PO_ShipmentQty_Validation"); message = message.replace("%",
       * lineno); OBError result = OBErrorBuilder.buildMessage(null, "error", message);
       * bundle.setResult(result); return; } }
       */
      // check current role associated with document rule for approval flow
      OBContext.setAdminMode();
      if (!DocStatus.equals("DR") && !DocStatus.equals("ESCM_REJ")
          && !DocStatus.equals("ESCM_RA")) {
        if (objOrder.getEutNextRole() != null) {
          java.util.List<EutNextRoleLine> li = objOrder.getEutNextRole().getEutNextRoleLineList();
          for (int i = 0; i < li.size(); i++) {
            String role = li.get(i).getRole().getId();
            if (roleId.equals(role)) {
              allowUpdate = true;
            }
          }
        }
        if (objOrder.getEutNextRole() != null) {
          DelegatedNextRoleDAO delagationDao = new DelegatedNextRoleDAOImpl();
          allowDelegation = delagationDao.checkDelegation(currentDate, roleId,
              DocumentTypeE.PURCHASE_ORDER.getDocumentTypeCode());

          /*
           * sql = ""; Connection con = OBDal.getInstance().getConnection(); PreparedStatement st =
           * null; ResultSet rs1 = null; sql =
           * "select dll.ad_role_id from eut_docapp_delegate dl join eut_docapp_delegateln dll on  dl.eut_docapp_delegate_id = dll.eut_docapp_delegate_id where from_date <= '"
           * + currentDate + "' and to_date >='" + currentDate + "' and document_type='EUT_108'"; st
           * = con.prepareStatement(sql); rs1 = st.executeQuery(); while (rs1.next()) { String
           * roleid = rs1.getString("ad_role_id"); if (roleid.equals(roleId)) { allowDelegation =
           * true; break; } }
           */
        }
        if (!allowUpdate && !allowDelegation) {
          errorFlag = true;
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@Escm_AlreadyPreocessed_Approved@");
          bundle.setResult(result);
          return;
        }
      }

      // if ((!vars.getRole().equals(objOrder.getEscmAdRole().getId())) && (DocStatus.equals("DR"))
      // && objOrder.getEscmLegacycontract() == null) {
      // errorFlag = true;
      // OBDal.getInstance().rollbackAndClose();
      // OBError result = OBErrorBuilder.buildMessage(null, "error",
      // "@Escm_AlreadyPreocessed_Approved@");
      // bundle.setResult(result);
      // return;
      // }

      // Task No.5925
      // check all line uniquecode belongs to same encumbrance if manual encumbrance for Budget
      // Controller
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", Boolean.TRUE,
            vars.getClient(), objOrder.getOrganization().getId(), vars.getUser(), vars.getRole(),
            PO_Window_ID, "N");
        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;

      } catch (PropertyException e) {
        preferenceValue = "N";
        // log.error("Exception in getting budget controller :", e);
      }

      if (!preferenceValue.equals("Y") && objOrder.getEutForward() != null) {// check for
        // temporary
        // preference
        String requester_user_id = objOrder.getEutForward().getUserContact().getId();
        String requester_role_id = objOrder.getEutForward().getRole().getId();
        preferenceValue = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", vars.getRole(), vars.getUser(), vars.getClient(),
            objOrder.getOrganization().getId(), PO_Window_ID, requester_user_id, requester_role_id);
      }
      // // Check preference value is not null

      if (preferenceValue == null) {
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Preference_value@");
        bundle.setResult(result);
        return;
      }

      // check preference is given by forward then restrict to give access while submit
      if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ")) && preferenceValue.equals("Y")) {
        List<Preference> prefs = forwardReqMoreInfoDAO.getPreferences("ESCM_BudgetControl", true,
            vars.getClient(), objOrder.getOrganization().getId(), vars.getUser(), vars.getRole(),
            Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY_W, false, true, true);
        for (Preference preference : prefs) {
          if (preference.getEutForwardReqmoreinfo() != null) {
            preferenceValue = "N";
          }
        }
      }

      if (preferenceValue != null && preferenceValue.equals("Y")) {
        if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")) {
          if (!objOrder.isEfinEncumbered()) {
            if (objOrder.getEfinBudgetManencum() != null) {
              errorFlag = POContractSummaryDAO.checkAllUniquecodesameEncum(objOrder);
            }
            if (errorFlag) {
              OBError result = OBErrorBuilder.buildMessage(null, "error",
                  "@Efin_Unicode_Same_Encum@");
              bundle.setResult(result);
              return;
            }
          }
        }
        // Copy UniqueCode... task no. 6007
        // Copy UniqueCode validations only for budget controller role
        if (StringUtils.isNotEmpty(objOrder.getId())) {
          POContractSummaryDAO.copyUniqueCodeValidation(objOrder);
        }

      }

      // End Task No.5925
      // check mandatory field for budget manager

      boolean fromPR = false, isAutoValid = false, isManualValid = false, isStageMove = false;

      // Task No.5925
      if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")
          && !objOrder.isEfinEncumbered()) {
        if (preferenceValue != null && preferenceValue.equals("Y")) {

          // all unniqecode should be mandatory.
          OBQuery<OrderLine> lines = OBDal.getInstance().createQuery(OrderLine.class,
              "salesOrder.id =:orderId and escmIssummarylevel = 'N'");
          lines.setNamedParameter("orderId", objOrder.getId());
          if (lines.list() != null && lines.list().size() > 0) {
            for (OrderLine OLines : lines.list()) {
              if (OLines.getEFINUniqueCode() == null) {
                errorFlag = true;
                OBDal.getInstance().rollbackAndClose();
                OBError result = OBErrorBuilder.buildMessage(null, "error",
                    "@Efin_Req_Uniquecode_Mandatory@");// ESCM_RoleIsNotIncInDocRule
                bundle.setResult(result);
                return;
              }
            }
          }

          // Task No.7523: Issue in Proposal with manual encumbrance to PO
          if (objOrder.getEscmProposalmgmt() != null) {

            EscmProposalMgmt proposalMgmt = OBDal.getInstance().get(EscmProposalMgmt.class,
                objOrder.getEscmProposalmgmt().getId());

            if (proposalMgmt.getEfinEncumbrance() != null
                && proposalMgmt.getEfinEncumbrance().getEncumMethod().equals("M")) {

              BigDecimal encumTotalAmt = proposalMgmt.getEfinEncumbrance().getAmount();
              BigDecimal proposalMgmtAmt = proposalMgmt.getProposalstatus().equals("PAWD")
                  ? proposalMgmt.getAwardamount()
                  : proposalMgmt.getTotalamount();
              BigDecimal orderAmt = objOrder.getGrandTotalAmount();

              if (proposalMgmtAmt.compareTo(encumTotalAmt) == 0) {
                if (orderAmt.compareTo(proposalMgmtAmt) > 0) {// increase
                  // throw error
                  errorFlag = true;
                  OBError result = OBErrorBuilder.buildMessage(null, "error",
                      "@Escm_ManEncumProposal@");
                  bundle.setResult(result);
                  return;
                } else if (orderAmt.compareTo(proposalMgmtAmt) < 0) {// decrease
                  // update
                }
              }
            }
          }

          // check the funds available
          OrderFundsCheck fundscheck = new OrderFundsCheck();
          fundscheck.fundsCheck(objOrder);

          // Task No.6078 create new version manual validation
          if (objOrder.getEscmBaseOrder() != null && objOrder.getEscmOldOrder() != null
              && objOrder.getEscmOldOrder().getEfinBudgetManencum() != null) {
            if (objOrder.getEfinBudgetManencum() != null
                && objOrder.getEfinBudgetManencum().getEncumMethod().equals("M")) {
              errorFlag = POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                  objOrder.getEscmBaseOrder(), true, false, null);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              } else {
                isManualValid = true;
              }
            } else {
              JSONObject object = POContractSummaryDAO.getUniquecodeListforPOVerAuto(objOrder,
                  objOrder.getEscmBaseOrder(), false, null);
              // funds validation.
              errorFlag = RequisitionfundsCheck.autoEncumbranceValidation(object,
                  objOrder.getEfinBudgetint(), "PO", false);
              if (errorFlag) {
                OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_Chk_Line_Info@");
                bundle.setResult(result);
                return;
              } else {
                isAutoValid = true;
              }
            }
          } else {

            // from proposal line added case:
            if (objOrder.getEscmProposalmgmt() == null) {
              // check lines added from pr
              OBQuery<OrderLine> orderLine = OBDal.getInstance().createQuery(OrderLine.class,
                  "salesOrder.id=:orderId and efinMRequisitionline.id is not null");
              orderLine.setNamedParameter("orderId", objOrder.getId());
              if (orderLine.list() != null && orderLine.list().size() > 0) {
                fromPR = true;
              }
              // from pr line added case:
              if (fromPR) {
                resultEncum = POContractSummaryDAO.checkFullPRQtyUitlizeorNot(objOrder);
                log.debug("resultEncums:" + resultEncum);
                if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                    && resultEncum.getBoolean("isAssociatePREncumbrance")
                    && resultEncum.has("isFullQtyUsed")
                    && !resultEncum.getBoolean("isFullQtyUsed")) {
                  JSONObject object = POContractSummaryDAO.getUniquecodeObject(objOrder);
                  JSONObject errorresult = POContractSummaryDAO
                      .checkAutoEncumValidationForPR(object, objOrder);
                  if (errorresult.has("errorflag")) {
                    if (errorresult.getString("errorflag").equals("0")) {
                      errorFlag = true;
                      OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                          errorresult.getString("errormsg"));
                      bundle.setResult(result1);
                      return;
                    }
                  }
                  EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                      resultEncum.getString("encumbrance"));

                  resultfundsavail = POContractSummaryDAO.getUniqueCodeListforFundschk(objOrder,
                      encumbrance);
                  errorFlag = resultfundsavail.getBoolean("errorflag");

                } else if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                    && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                  JSONObject object = POContractSummaryDAO.getUniquecodeObject(objOrder);
                  JSONObject errorresult = POContractSummaryDAO
                      .checkAutoEncumValidationForPR(object, objOrder);
                  if (errorresult.has("errorflag")) {
                    if (errorresult.getString("errorflag").equals("0")) {
                      errorFlag = true;
                      OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                          errorresult.getString("errormsg"));
                      bundle.setResult(result1);
                      return;
                    }
                  }
                } else {
                  EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
                      resultEncum.getString("encumbrance"));
                  errorFlag = POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(objOrder,
                      encumbrance, true, false);
                }

                if (errorFlag) {
                  OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                      "@ESCM_ProcessFailed(Reason)@");
                  bundle.setResult(result1);
                  return;
                }
              }
              // auto or manual encum case:
              else {
                // form uniqucode jsonobject for validation.
                // new validation with common method.
                JSONObject object = POContractSummaryDAO.getUniquecodeObject(objOrder);
                // manual case:
                if (objOrder.getEfinBudgetManencum() != null) {
                  EfinBudgetManencum manualEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
                      objOrder.getEfinBudgetManencum().getId());
                  encumLineList = manualEncum.getEfinBudgetManencumlinesList();
                  errorFlag = RequisitionfundsCheck.manualEncumbranceValidation(encumLineList,
                      object, "PO", false);
                  if (errorFlag) {
                    OBError result = OBErrorBuilder.buildMessage(null, "error",
                        "@Efin_Chk_Line_Info@");
                    bundle.setResult(result);
                    return;
                  } else {
                    isManualValid = true;
                  }
                }
                // auto case:
                else {
                  JSONObject errorresult = POContractSummaryDAO
                      .checkAutoEncumValidationForPR(object, objOrder);
                  log.debug("errorresults:" + errorresult);
                  if (errorresult.has("errorflag")) {
                    if (errorresult.getString("errorflag").equals("0")) {
                      errorFlag = true;
                      OBError result1 = OBErrorBuilder.buildMessage(null, "error",
                          errorresult.getString("errormsg"));
                      bundle.setResult(result1);
                      return;
                    } else if (errorresult.has("errorflag")
                        && errorresult.getString("errorflag").equals("1")) {
                      isAutoValid = true;
                    }
                  }
                }
              }
            } else {
              // check the proposal encum fully utilised.
              // else create new encum
              EfinBudgetManencum encum = objOrder.getEscmProposalmgmt().getEfinEncumbrance();
              Integer ReleasedMatch = 0;
              log.debug("getAppliedAmount:" + encum.getAppliedAmount());

              if (objOrder.getEscmOrdertype().equals("PUR_REL")) {
                // BigDecimal fundsAvailable = BigDecimal.ZERO;
                // JSONObject fundsCheckingObject = null;
                // Task no: 7554
                // if (!objOrder.getEscmProposalmgmt().isTaxLine() && objOrder.isEscmIstax()) {
                // for (OrderLine line : objOrder.getOrderLineList()) {
                // if (!line.isEscmIssummarylevel()
                // && !line.getEscmProposalmgmtLine().isSummary()) {
                // if ((line.getLineNetAmount().subtract(line.getEscmLineTaxamt()))
                // .compareTo(line.getEscmProposalmgmtLine().getLineTotal()) != 0) {
                // BigDecimal diffAmount = line.getEscmLineTaxamt();
                // try {
                // fundsCheckingObject = CommonValidationsDAO.CommonFundsChecking(
                // objOrder.getEfinBudgetint(), line.getEFINUniqueCode(), diffAmount);
                // fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
                // if ("0".equals(fundsCheckingObject.getString("errorFlag"))) {
                // OBError result = OBErrorBuilder.buildMessage(null, "error",
                // fundsCheckingObject.getString("message"));
                // bundle.setResult(result);
                // return;
                // }
                // } catch (Exception e) {
                // fundsAvailable = BigDecimal.ZERO;
                // log.error("Exception in CommonFundsChecking " + e, e);
                // }
                // }
                // }
                // }
                // }

                if (objOrder.getOrderLineList().size() == objOrder.getEscmProposalmgmt()
                    .getEscmProposalmgmtLineList().size()) {
                  // Check tax field is checked for Proposal Management associated with PO
                  if (!objOrder.getEscmProposalmgmt().isTaxLine() && objOrder.isEscmIstax()) {
                    for (OrderLine line : objOrder.getOrderLineList()) {
                      if ((line.getLineNetAmount().subtract(line.getEscmLineTaxamt()))
                          .compareTo(line.getEscmProposalmgmtLine().getLineTotal()) == 0) {
                        ReleasedMatch++;
                      } else {
                        break;
                      }
                    }
                  } else {
                    for (OrderLine line : objOrder.getOrderLineList()) {
                      if ((line.getLineNetAmount())
                          .compareTo(line.getEscmProposalmgmtLine().getLineTotal()) == 0) {
                        ReleasedMatch++;
                      } else {
                        break;
                      }
                    }
                  }
                }

                // Check whether all quantity/amount is released during purchase release
                // then update stage
                if (ReleasedMatch == objOrder.getOrderLineList().size()) {
                  // Task no: 7554
                  // if (!objOrder.getEscmProposalmgmt().isTaxLine() && objOrder.isEscmIstax()) {
                  // objOrder.getEscmProposalmgmt().getEfinEncumbrance().setEncumStage("POE");
                  // objOrder.setEFINEncumbranceMethod(
                  // objOrder.getEscmProposalmgmt().getEFINEncumbranceMethod());
                  // objOrder
                  // .setEfinBudgetManencum(objOrder.getEscmProposalmgmt().getEfinEncumbrance());
                  // objOrder.setEfinEncumbered(true);
                  // OBDal.getInstance().save(objOrder);
                  // OBDal.getInstance().flush();
                  // POContractSummaryDAO.doPOChangeMofifcationInEncumbrance(objOrder);
                  // } else {
                  objOrder.getEscmProposalmgmt().getEfinEncumbrance().setEncumStage("POE");
                  objOrder.setEFINEncumbranceMethod(
                      objOrder.getEscmProposalmgmt().getEFINEncumbranceMethod());
                  objOrder
                      .setEfinBudgetManencum(objOrder.getEscmProposalmgmt().getEfinEncumbrance());
                  objOrder.setEfinEncumbered(true);
                  OBDal.getInstance().save(objOrder);
                  OBDal.getInstance().flush();
                  // }
                } else {

                  POContractSummaryDAO.splitPRforPA(encum.getId(), null, objOrder);
                }

              } else {
                // chk the funds available for proposal to po
                if (objOrder.getOrderLineList().size() > 0) {
                  HashMap<String, BigDecimal> encumDiffRem = new HashMap<>();
                  org.codehaus.jettison.json.JSONArray jsonArray = null;
                  JSONObject json = POContractSummaryDAO.getEncumAmount(objOrder);

                  if (json != null && json.length() > 0) {
                    jsonArray = json.getJSONArray("data");
                  }
                  if (jsonArray != null) {
                    if (jsonArray.length() > 0) {
                      for (int i = 0; i < jsonArray.length(); i++) {
                        json = jsonArray.getJSONObject(i);
                        encAmount = json.getString("encAmount");
                        amount = new BigDecimal(encAmount);
                        encRemAmount = json.getString("encumLineRem");
                        remAmount = new BigDecimal(encRemAmount);
                        if (objOrder.getEfinBudgetManencum().getEncumMethod().equals("A")) {
                          EfinBudgetIntialization budgetInt = OBDal.getInstance().get(
                              EfinBudgetIntialization.class,
                              json.getString("budgetInt").toString());
                          AccountingCombination accDimension = OBDal.getInstance().get(
                              AccountingCombination.class,
                              json.getString("accDimension").toString());
                          JSONObject resultFunds = CommonValidationsDAO
                              .CommonFundsChecking(budgetInt, accDimension, amount);
                          if ("0".equals(resultFunds.getString("errorFlag"))) {
                            OBError result = OBErrorBuilder.buildMessage(null, "error",
                                resultFunds.getString("message"));
                            bundle.setResult(result);
                            return;
                          }
                        } else {
                          if (amount.compareTo(BigDecimal.ZERO) > 0) {
                            if (encumDiffRem.containsKey(json.getString("encumLine"))) {
                              BigDecimal rem = encumDiffRem.get(json.getString("encumLine"));
                              if (rem.compareTo(amount) >= 0) {
                                encumDiffRem.put(json.getString("encumLine"), rem.subtract(amount));
                              } else {
                                String erMsg = OBMessageUtils
                                    .messageBD("EFIN_PurInv_ManEncumRemAmt");
                                EfinBudgetManencumlines encLine = OBDal.getInstance().get(
                                    EfinBudgetManencumlines.class, json.getString("encumLine"));
                                erMsg = erMsg.replace("%",
                                    encLine.getAccountingCombination().getEfinUniqueCode());
                                OBError result = OBErrorBuilder.buildMessage(null, "error", erMsg);
                                bundle.setResult(result);
                                return;
                              }
                            } else {
                              if (remAmount.compareTo(amount) >= 0) {
                                encumDiffRem.put(json.getString("encumLine"),
                                    remAmount.subtract(amount));
                              } else {
                                String erMsg = OBMessageUtils
                                    .messageBD("EFIN_PurInv_ManEncumRemAmt");
                                EfinBudgetManencumlines encLine = OBDal.getInstance().get(
                                    EfinBudgetManencumlines.class, json.getString("encumLine"));
                                erMsg = erMsg.replace("%",
                                    encLine.getAccountingCombination().getEfinUniqueCode());
                                OBError result = OBErrorBuilder.buildMessage(null, "error", erMsg);
                                bundle.setResult(result);
                                return;
                              }
                            }
                          }
                        }
                      }

                    }
                  }
                }
                if (objOrder.getEscmProposalmgmt().getProposalstatus().equals("AWD")) {
                  if (encum.getRevamount().compareTo(objOrder.getGrandTotalAmount()) == 0) {
                    isStageMove = true;
                  } else {
                    encumId = encum.getId();
                  }
                } else {
                  // Partial Award
                  // If same manual encumbrance is used in other proposals, do split
                  boolean isEncumUsed = POContractSummaryDAO
                      .encumUsedInOtherProposals(objOrder.getEscmProposalmgmt());
                  if (encum.getRevamount().compareTo(objOrder.getGrandTotalAmount()) == 0
                      && !isEncumUsed) {
                    isStageMove = true;
                  } else {
                    encumId = encum.getId();
                  }
                }
              }
            }
          }
        }
      }
      // End Task No.5925

      // check min release amount in contract.
      if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmOrdertype().equals("PUR_REL")) {
        if ((objOrder.getEscmPurchaseagreement().getEscmMinRelease() != null
            && objOrder.getEscmPurchaseagreement().getEscmMinRelease()
                .compareTo(BigDecimal.ZERO) > 0
            && objOrder.getGrandTotalAmount()
                .compareTo(objOrder.getEscmPurchaseagreement().getEscmMinRelease()) < 0)) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_Min/Max_Release@");
          bundle.setResult(result);
          return;
        }
      }

      if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmPurchaseagreement() == null) {
        objOrder.setEscmMaxRelease(objOrder.getGrandTotalAmount());
      }
      // check grand total is greater than agreement limit.
      if (objOrder.getEscmRevision() == 0) {
        if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmPurchaseagreement() == null) {
          if (objOrder.getEscmMaxRelease() != null
              && objOrder.getGrandTotalAmount().compareTo(objOrder.getEscmMaxRelease()) > 0) {
            errorFlag = true;
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Agrtamt_val@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // check grand total is greater than agreement limit of previous version.
      if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
        if (objOrder.getEscmRevision() > 0 && objOrder.isEscmIspurchaseagreement()) {
          if (objOrder.getGrandTotalAmount()
              .compareTo(objOrder.getEscmOldOrder().getEscmMaxRelease()) > 0) {
            errorFlag = true;
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_Agrmtlmt_gt_totamt@");
            bundle.setResult(result);
            return;
          }
        }
      }

      // Check whether parent and children has different suppliers
      Boolean isSupplierDifferent = Boolean.FALSE;
      String parentId = null;
      if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmOrdertype().equals("PUR_AG")) {
        // Clear failure reason
        for (OrderLine line : objOrder.getOrderLineList()) {
          if (line.getEfinFailureReason() != null) {
            line.setEfinFailureReason(null);
          }
        }
        for (OrderLine line : objOrder.getOrderLineList()) {
          if (!line.isEscmIssummarylevel()) {
            String childSupplierId = line.getBusinessPartner().getId();
            if (line.getEscmParentline() != null) {
              parentId = line.getEscmParentline().getId();
            }
            while (parentId != null) {
              OrderLine parent = OBDal.getInstance().get(OrderLine.class, parentId);
              String parentSupplierId = parent.getBusinessPartner().getId();
              if (!childSupplierId.equals(parentSupplierId)) {
                line.setEfinFailureReason(OBMessageUtils.messageBD("ESCM_DiffSuppliers_PurAgrmt"));
                parent
                    .setEfinFailureReason(OBMessageUtils.messageBD("ESCM_DiffSuppliers_PurAgrmt"));
                isSupplierDifferent = Boolean.TRUE;
              }
              if (parent.getEscmParentline() != null) {
                parentId = parent.getEscmParentline().getId();
              } else {
                parentId = null;
              }
            }
          }
        }
      }
      if (isSupplierDifferent) {
        errorFlag = true;
        OBError result = OBErrorBuilder.buildMessage(null, "error",
            "@ESCM_DiffSuppliers_PurAgrmt@");
        bundle.setResult(result);
        return;
      }

      PurchaseReleaseDAO dao = new PurchaseReleaseDAO();
      Boolean isQtyAmtGreater = Boolean.FALSE;
      Order order = objOrder;
      if (objOrder.getEscmOldOrder() != null) {
        order = objOrder.getEscmOldOrder();
      }

      // check release qty/amount validation
      if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmOrdertype().equals("PUR_REL")) {
        // Clear failure reason
        for (OrderLine line : objOrder.getOrderLineList()) {
          line.setEfinFailureReason(null);
        }
        for (OrderLine line : objOrder.getOrderLineList()) {
          if (!line.isEscmIssummarylevel() && line.getEscmAgreementLine() != null) {

            if (objOrder.getEscmReceivetype().equals("QTY")) {

              // Check whether remaining quantity is greater than release quantity
              BigDecimal inProgressReleaseQty = dao
                  .getInProgressReleaseQty(line.getEscmAgreementLine().getId(), line.getId());
              BigDecimal remainingQty = BigDecimal.ZERO;
              if (!POContractSummaryDAO.isNewVersionCreatedAgainstPA(order)) {
                remainingQty = line.getEscmAgreementLine().getOrderedQuantity()
                    .subtract(line.getEscmAgreementLine().getEscmReleaseqty())
                    .subtract(inProgressReleaseQty);
              } else {
                remainingQty = line.getEscmAgreementLine().getOrderedQuantity()
                    .subtract(line.getEscmAgreementLine().getEscmReleaseqty())
                    .subtract(inProgressReleaseQty)
                    .add(line.getEscmOldOrderline().getOrderedQuantity());
              }
              if (line.getOrderedQuantity().compareTo(remainingQty) > 0) {
                isQtyAmtGreater = Boolean.TRUE;
                String message = String.format(OBMessageUtils.messageBD("ESCM_ReleaseGrtrRemQty"),
                    line.getOrderedQuantity(), remainingQty);
                line.setEfinFailureReason(message);
              }
            }
            if (objOrder.getEscmReceivetype().equals("AMT")) {
              boolean agreementHasTax = false;
              if (objOrder.getEscmPurchaseagreement().isEscmIstax()
                  && objOrder.getEscmPurchaseagreement().getEscmTaxMethod() != null) {
                agreementHasTax = true;
              }

              releaseAmt = line.getLineNetAmount();
              if (!agreementHasTax && objOrder.isEscmIstax()
                  && objOrder.getEscmTaxMethod() != null) {
                if (!objOrder.getEscmTaxMethod().isPriceIncludesTax()) {
                  releaseAmt = line.getLineNetAmount().subtract(line.getEscmLineTaxamt());
                }
              }

              // Check whether remaining amount is greater than release amount
              BigDecimal inProgressReleaseAmt = dao.getInProgressReleaseAmt(
                  line.getEscmAgreementLine().getId(), line.getId(), agreementHasTax);
              BigDecimal remainingAmt = BigDecimal.ZERO;
              if (!POContractSummaryDAO.isNewVersionCreatedAgainstPA(order)) {
                remainingAmt = line.getEscmAgreementLine().getLineNetAmount()
                    .subtract(line.getEscmAgreementLine().getEscmReleaseamt())
                    .subtract(inProgressReleaseAmt);
              } else {
                remainingAmt = line.getEscmAgreementLine().getLineNetAmount()
                    .subtract(line.getEscmAgreementLine().getEscmReleaseamt())
                    .subtract(inProgressReleaseAmt)
                    .add(line.getEscmOldOrderline().getLineNetAmount());
              }

              if (releaseAmt.compareTo(remainingAmt) > 0) {
                isQtyAmtGreater = Boolean.TRUE;
                String message = String.format(OBMessageUtils.messageBD("ESCM_ReleaseGrtrRemAmt"),
                    line.getLineNetAmount(), remainingAmt);
                line.setEfinFailureReason(message);
              }
            }
          }
        }
      }
      if (isQtyAmtGreater && objOrder.getEscmReceivetype().equals("QTY")) {
        errorFlag = true;
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_GrtrReleaseQty@");
        bundle.setResult(result);
        return;
      }
      if (isQtyAmtGreater && objOrder.getEscmReceivetype().equals("AMT")) {
        errorFlag = true;
        OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_GrtrReleaseAmt@");
        bundle.setResult(result);
        return;
      }

      // Check whether the release lineNetamount is greater than the agreement line remaining amount
      if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmOrdertype().equals("PUR_REL")) {
        releaseAmtGreater = dao.checkAgreementRemainingAmt(objOrder);
        if (releaseAmtGreater) {
          errorFlag = true;
          OBError result = OBErrorBuilder.buildMessage(null, "error",
              "@ESCM_RelAmtGrtThan_AgrAmt@");
          bundle.setResult(result);
          return;
        }

        // Release amount should not exceed the proposal encumbrance amount for release created from
        // proposal
        if (objOrder.getEscmProposalmgmt() != null) {
          releaseAmtGreater = dao.checkAmtGrtThnEncAmount(objOrder);
          if (releaseAmtGreater) {
            errorFlag = true;
            OBError result = OBErrorBuilder.buildMessage(null, "error",
                "@ESCM_RelAmtGrtThan_EncAmt@");
            bundle.setResult(result);
            return;
          }
        }
      }
      if (!errorFlag) {
        if ((DocStatus.equals("DR") || DocStatus.equals("ESCM_REJ") || DocStatus.equals("ESCM_RA"))
            && DocAction.equals("CO")) {
          appstatus = "SUB";
        } else if (DocStatus.equals("ESCM_IP") && DocAction.equals("AP")) {
          appstatus = "AP";
        }
        // Branch Roll out changes #Task no: 8047
        // Check Approval flow for submit user region/organization instead of record organization

        region_org_id = object_reg_org != null ? object_reg_org.getId() : orgId;
        count = updateHeaderStatus(conn, clientId, region_org_id, roleId, userId, objOrder,
            appstatus, comments, currentDate, vars, nextApproval, Lang, bundle, preferenceValue,
            enccontrollist);
        if (count == 3) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_No_LineManager@");
          bundle.setResult(result);
          return;
        } else if (count == -2) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", errorMsgs);
          bundle.setResult(result);
          return;
        } else if (count == 2 || count == 1) {
          OBInterceptor.setPreventUpdateInfoChange(true);

          // Task No.6078 new version encumbrance update
          // added this condition "objOrder.getEscmBaseOrder().getEfinBudgetManencum()!=null " for
          // case that in legacy
          // data the encumbrance may not be presented in intial version
          if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")
              && objOrder.getEscmOldOrder() != null
              && objOrder.getEscmOldOrder().getEfinBudgetManencum() != null
              && !objOrder.isEfinEncumbered() && preferenceValue != null
              && preferenceValue.equals("Y")) {
            // New version encumbrance update
            // it will insert modification in existing encumbrance when amount is differ in new
            // version
            // why ????
            if (objOrder.getEfinBudgetManencum() != null
                && objOrder.getEfinBudgetManencum().getEncumMethod().equals("A")) {
              POContractSummaryDAO.doMofifcationInEncumbrance(objOrder, objOrder.getEscmOldOrder());
            } else {
              POContractSummaryDAO.chkNewVersionManualEncumbranceValidation(objOrder,
                  objOrder.getEscmOldOrder(), false, false, null);
            }
          }
          // Task No.5925
          else if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")
              && !objOrder.isEfinEncumbered() && preferenceValue != null
              && preferenceValue.equals("Y")
              && (objOrder.getEscmBaseOrder() == null || (objOrder.getEscmBaseOrder() != null
                  && objOrder.getEscmBaseOrder().getEfinBudgetManencum() == null))) {

            // added this to get encumlist from header encumbrance in case for legacy data
            if (objOrder.getEscmBaseOrder() != null
                && objOrder.getEscmBaseOrder().getEfinBudgetManencum() == null) {
              if (objOrder.getEfinBudgetManencum() != null)
                encumLineList = objOrder.getEfinBudgetManencum().getEfinBudgetManencumlinesList();
            }
            if (objOrder.getEscmProposalmgmt() != null
                && !objOrder.getEscmOrdertype().equals("PUR_REL")) {
              // move stage of encum
              if (isStageMove) {
                if (objOrder.getEfinBudgetManencum() != null) {
                  objOrder.getEfinBudgetManencum().setEncumStage("POE");
                  objOrder.setEfinEncumbered(true);
                  if (objOrder.getEfinBudgetManencum().getEncumMethod().equals("A")) {
                    if (objOrder.getEscmProjectname() != null) {
                      objOrder.getEfinBudgetManencum()
                          .setDescription(objOrder.getEscmProjectname());
                    } else {
                      objOrder.getEfinBudgetManencum().setDescription(objOrder.getDocumentNo());
                    }

                  }
                }

                if (objOrder.getEfinBudgetManencum() != null
                    && objOrder.getEfinBudgetManencum().getEncumMethod().equals("A")) {
                  if (objOrder.getOrderLineList().size() > 0) {
                    POContractSummaryDAO.doPOChangeMofifcationInEncumbrance(objOrder);

                    // if (objOrder.getEscmOrdertype().equals("PUR_REL")) {
                    // EfinBudgetManencum newEncumbrance = null;
                    // newEncumbrance = POContractSummaryDAO.insertEncumbranceOrder(objOrder,
                    // objOrder.getEfinBudgetManencum());
                    // objOrder.setEfinBudgetManencum(newEncumbrance);
                    // }
                  }
                } else {
                  for (OrderLine oLine : objOrder.getOrderLineList()) {
                    if (!oLine.isEscmIssummarylevel()) {
                      BigDecimal propLineAmt = oLine.getEscmProposalmgmtLine().getEscmProposalmgmt()
                          .getProposalstatus().equals("PAWD")
                              ? oLine.getEscmProposalmgmtLine().getAwardedamount()
                              : oLine.getEscmProposalmgmtLine().getLineTotal();
                      BigDecimal diffAmt = oLine.getLineNetAmount().subtract(propLineAmt);
                      if (diffAmt.compareTo(BigDecimal.ZERO) > 0) {
                        EfinBudgetManencumlines encumLine = oLine.getEfinBudEncumlines();
                        encumLine.setAPPAmt(encumLine.getAPPAmt().add(diffAmt));
                        OBDal.getInstance().save(encumLine);
                      }
                    }
                  }
                }

                OBDal.getInstance().save(objOrder);
                OBDal.getInstance().flush();
                OBInterceptor.setPreventUpdateInfoChange(false);

              } else {
                // create new encum
                POContractSummaryDAO.splitPRforPA(encumId, null, objOrder);
              }
            } else if (fromPR) {
              // check encum validation.
              // impacts
              // encumbrance split & merge concept added form Pr line
              if (!errorFlag && resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                  && resultEncum.getBoolean("isAssociatePREncumbrance")) {
                if (resultEncum.has("isFullQtyUsed") && resultEncum.getBoolean("isFullQtyUsed")) {

                  // if full qty used in Pr check any manual line is presented or not
                  objOrder.setEfinBudgetManencum(OBDal.getInstance().get(EfinBudgetManencum.class,
                      resultEncum.getString("encumbrance")));
                  objOrder.getEfinBudgetManencum().setEncumStage("POE");
                  objOrder.setEfinEncumbered(true);
                  // set supplier
                  if (objOrder.getEfinBudgetManencum().getBusinessPartner() == null) {
                    objOrder.getEfinBudgetManencum()
                        .setBusinessPartner(objOrder.getBusinessPartner());
                  }
                  if (objOrder.getEfinBudgetManencum().getEncumMethod().equals("A")) {
                    if (objOrder.getEscmNotes() != null) {
                      objOrder.getEfinBudgetManencum().setDescription(objOrder.getEscmNotes());
                    } else {
                      objOrder.getEfinBudgetManencum().setDescription(objOrder.getDocumentNo());
                    }

                  }
                  OBDal.getInstance().save(objOrder);
                  POContractSummaryDAO.chkAndUpdateforProposalPRFullQty(objOrder,
                      objOrder.getEfinBudgetManencum(), false, false);
                  // update enucm line
                  for (OrderLine line : objOrder.getOrderLineList()) {
                    if (line.getEFINUniqueCode() != null
                        && objOrder.getEfinBudgetManencum() != null) {
                      OBQuery<EfinBudgetManencumlines> manline = OBDal.getInstance().createQuery(
                          EfinBudgetManencumlines.class,
                          " as e  where e.manualEncumbrance.id=:encumId and e.accountingCombination.id=:accId ");
                      manline.setNamedParameter("encumId",
                          objOrder.getEfinBudgetManencum().getId());
                      manline.setNamedParameter("accId", line.getEFINUniqueCode().getId());
                      manline.setMaxResult(1);
                      if (manline.list().size() > 0) {
                        line.setEfinBudEncumlines(manline.list().get(0));
                        OBDal.getInstance().save(line);
                      }
                    }
                  }
                } else {
                  if (resultEncum.has("type") && resultEncum.getString("type").equals("SPLIT")) {
                    BidManagementDAO.splitPR(resultEncum, null, objOrder, null, null);
                  }
                  if (resultEncum.has("type") && resultEncum.getString("type").equals("MERGE")) {
                    BidManagementDAO.splitPR(resultEncum, null, objOrder, null, null);
                  }
                }
              }

              // if pr is skip the encumbrance
              else if (resultEncum != null && resultEncum.has("isAssociatePREncumbrance")
                  && !resultEncum.getBoolean("isAssociatePREncumbrance")) {
                // insert auto encumbrance.
                POContractSummaryDAO.insertAutoEncumbrance(objOrder);
              }
            } else if (isManualValid) {
              // amount update
              POContractSummaryDAO.updateManualEncumAmount(objOrder, encumLineList);
            } else if (isAutoValid) {
              // insert auto encumbrance.
              POContractSummaryDAO.insertAutoEncumbrance(objOrder);
              // after that should imapct in inquiry Trigger changes
              // POContractSummaryDAO.updateAmtInEnquiry(objOrder.getId());

            }

          }

          if (count == 1
              && objOrder.getEscmContactType().getId().equals("7A690E46B6E043C7A8B34B2D92E17D87")) {

            // -- EPM Web service starts

            log.debug("EPM webservice starts");

            // get Default city as Riyadh
            // City Riyadh = OBDal.getInstance().get(City.class,
            // "37AB29E2A42046EE98E95A5F27394DD7");
            // 180 is city riyadhs project id in epm.
            long projectId = (objOrder.getEscmCCity() != null
                ? (objOrder.getEscmCCity().getEscmEpmprojectid() != null
                    ? objOrder.getEscmCCity().getEscmEpmprojectid()
                    : 180)
                : 180);

            EpmService service = new EmpServiceImpl();
            log.debug("ProjectId" + projectId);
            service.addProject(order, projectId == 0 ? null : (int) projectId,
                order.getEfinInvoiceAmt().subtract(order.getEfinLegacypaidAmt()));

            log.debug("EPM webservice ends");

          }

          // Check Encumbrance Amount is Negative
          if (objOrder.getEfinBudgetManencum() != null)
            encumLinelist = objOrder.getEfinBudgetManencum().getEfinBudgetManencumlinesList();
          if (encumLinelist.size() > 0)
            checkEncumbranceAmountZero = UtilityDAO.checkEncumbranceAmountZero(encumLinelist);

          if (checkEncumbranceAmountZero) {
            OBDal.getInstance().rollbackAndClose();
            OBError result = OBErrorBuilder.buildMessage(null, "error", "@ESCM_Encumamt_Neg@");
            bundle.setResult(result);
            return;
          }

          // End Task No.5925
          OBError result = OBErrorBuilder.buildMessage(null, "success",
              "@Escm_Ir_complete_success@");
          bundle.setResult(result);
          return;
        } else if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")
            && count == 4) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Efin_No_BudgetControl@");
          bundle.setResult(result);
          return;
        } else if (objOrder.getEscmOrdertype().equals("PUR_AG") && count == 5) {
          OBDal.getInstance().rollbackAndClose();
          OBError result = OBErrorBuilder.buildMessage(null, "error", "@Escm_PurAgHasPurRel@");
          bundle.setResult(result);
          return;
        }
      }
    } catch (GenericJDBCException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception in POContractSummaryAction :", e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      String errorMessage = OBMessageUtils.translateError(t.getMessage()).getMessage();
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD(errorMessage));
      bundle.setResult(error);
      return;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log.error(" Exception while insertAutoEncumbrance: ", e);
      e.printStackTrace();
      OBError result = OBErrorBuilder.buildMessage(null, "error", e.getMessage());
      bundle.setResult(result);
      return;
    } catch (Exception e) {
      log.error("Exeception in PurchaseOrderSubmit Process:", e);
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private boolean isContractAttributesExist(Order objOrder) {
    BigDecimal escmAdvpaymntAmt = objOrder.getEscmAdvpaymntAmt();

    BigDecimal escmAdvpaymntPercntge = objOrder.getEscmAdvpaymntPercntge();

    Long escmContractduration = objOrder.getEscmContractduration();
    Date escmContractenddate = objOrder.getEscmContractenddate();
    Date escmContractstartdate = objOrder.getEscmContractstartdate();
    Date escmOnboarddateh = objOrder.getEscmOnboarddateh();

    String escmPeriodtype = objOrder.getEscmPeriodtype();
    BigDecimal zero = new BigDecimal(0);
    if (escmAdvpaymntAmt == null || zero.compareTo(escmAdvpaymntAmt) > 0) {
      log.error("escmAdvpaymntAmt is not valid, Value :" + escmAdvpaymntAmt);
      return false;
    }
    if (escmAdvpaymntPercntge == null || escmAdvpaymntPercntge.compareTo(BigDecimal.ZERO) < 0) {
      log.error("escmAdvpaymntPercntge is not valid, Value :" + escmAdvpaymntPercntge);
      return false;
    }
    if (escmContractduration == null || escmContractduration.compareTo(0L) <= 0) {
      log.error("escmContractduration is not valid, Value :" + escmContractduration);
      return false;
    }
    if (escmContractenddate == null) {
      log.error("escmContractenddate is not valid, Value :" + escmContractenddate);
      return false;
    }
    if (escmContractstartdate == null) {
      log.error("escmContractstartdate is not valid, Value :" + escmContractstartdate);
      return false;
    }
    if (escmOnboarddateh == null) {
      log.error("escmOnboarddateh is not valid, Value :" + escmOnboarddateh);
      return false;
    }
    if (StringUtils.isEmpty(escmPeriodtype)) {
      log.error("escmPeriodtype is not valid, Value :" + escmPeriodtype);
      return false;
    }
    return true;
  }

  private int updateHeaderStatus(Connection con, String clientId, String orgId, String roleId,
      String userId, Order objOrder, String appstatus, String comments, Date currentDate,
      VariablesSecureApp vars, NextRoleByRuleVO paramnextApproval, String Lang,
      ProcessBundle bundle, String preferenceValue, List<EfinEncControl> enccontrollist) {
    String strOrderId = null, pendingapproval = null;
    int count = 0;
    Boolean isDirectApproval = false;
    String alertRuleId = "", alertWindow = AlertWindow.PurchaseOrderContract;
    User objUser = OBDal.getInstance().get(User.class, vars.getUser());
    User objCreater = objOrder.getCreatedBy();
    // check po record is legacy then
    // update order created by as submitting user
    // and created role as submit role.
    // if (objOrder.getEscmLegacycontract() != null && objOrder.getEscmAppstatus().equals("DR")) {
    // objCreater = objUser;
    // objOrder.setEscmAdRole(OBDal.getInstance().get(Role.class, vars.getRole()));
    // objOrder.setCreatedBy(objCreater);
    //
    // } else {
    // objCreater = objOrder.getCreatedBy();
    // }

    NextRoleByRuleVO nextApproval = paramnextApproval;
    ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
    boolean isDummyRole = false;
    try {
      OBContext.setAdminMode();

      // NextRoleByRuleVO nextApproval = NextRoleByRule.getNextRole(con, clientId, orgId,
      // roleId,userId, Resource.PURCHASE_REQUISITION, 0.00);
      EutNextRole nextRole = null;
      boolean isBackwardDelegation = false;
      HashMap<String, String> role = null;
      String qu_next_role_id = "";
      String delegatedFromRole = null;
      String delegatedToRole = null;
      JSONObject fromUserandRoleJson = new JSONObject();
      String fromUser = userId;
      String fromRole = roleId;
      ForwardRequestMoreInfoDAO forwardDao = new ForwardRequestMoreInfoDAOImpl();
      isDirectApproval = isDirectApproval(objOrder.getId(), roleId);
      String documentRule = null;
      BigDecimal releaseAmt = BigDecimal.ZERO;

      if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
        documentRule = Resource.PURCHASE_AGREEMENT_RULE;
      } else {
        documentRule = Resource.PURCHASE_ORDER_RULE;
      }
      // get alert rule id - TaskNo:7618
      alertRuleId = AlertUtility.getAlertRule(clientId, alertWindow);

      if ((objOrder.getEutNextRole() != null)) {

        fromUserandRoleJson = forwardDao.getFromuserAndFromRoleWhileApprove(
            objOrder.getEutNextRole(), userId, roleId, clientId, orgId, documentRule, isDummyRole,
            isDirectApproval);
        if (fromUserandRoleJson != null && fromUserandRoleJson.length() > 0) {
          if (fromUserandRoleJson.has("fromUser"))
            fromUser = fromUserandRoleJson.getString("fromUser");
          if (fromUserandRoleJson.has("fromRole"))
            fromRole = fromUserandRoleJson.getString("fromRole");
          if (fromUserandRoleJson.has("isDirectApproval"))
            isDirectApproval = fromUserandRoleJson.getBoolean("isDirectApproval");
        }
      } else {
        fromUser = userId;
        fromRole = roleId;
      }
      if ((objOrder.getEutNextRole() == null)) {
        nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
            OBDal.getInstance().getConnection(), clientId, orgId, fromRole, fromUser, documentRule,
            objOrder.getGrandTotalAmount(), fromUser, false, objOrder.getEscmAppstatus());
      } else {
        if (isDirectApproval) {

          nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
              orgId, fromRole, fromUser, documentRule, objOrder.getGrandTotalAmount());

          /*
           * nextApproval = NextRoleByRule.getLineManagerBasedNextRole(OBDal.getInstance()
           * .getConnection(), clientId, orgId, roleId, userId, documentRule,
           * objOrder.getGrandTotalAmount(), objOrder.getCreatedBy().getId(), false);
           */

          if (nextApproval != null && nextApproval.hasApproval()) {
            nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
            if (nextRole.getEutNextRoleLineList().size() > 0) {
              for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
                OBQuery<UserRoles> userRole = OBDal.getInstance().createQuery(UserRoles.class,
                    "role.id=:roleID ");
                userRole.setNamedParameter("roleID", objNextRoleLine.getRole().getId());
                role = NextRoleByRule.getbackwardDelegatedFromAndToRoles(
                    OBDal.getInstance().getConnection(), clientId, orgId,
                    userRole.list().get(0).getUserContact().getId(), documentRule, "");
                delegatedFromRole = role.get("FromUserRoleId");
                delegatedToRole = role.get("ToUserRoleId");
                isBackwardDelegation = NextRoleByRule.isBackwardDelegation(
                    OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole,
                    delegatedToRole, fromUser, documentRule, objOrder.getGrandTotalAmount());
                if (isBackwardDelegation)
                  break;
              }
            }
          }
          if (isBackwardDelegation) {
            nextApproval = NextRoleByRule.getNextRole(OBDal.getInstance().getConnection(), clientId,
                orgId, delegatedFromRole, fromUser, documentRule, objOrder.getGrandTotalAmount());

            /*
             * nextApproval = NextRoleByRule.getLineManagerBasedNextRole(
             * OBDal.getInstance().getConnection(), clientId, orgId, delegatedFromRole, userId,
             * documentRule, objOrder.getGrandTotalAmount(), objOrder.getCreatedBy().getId(),
             * false);
             */
          }
        } else {
          role = NextRoleByRule.getDelegatedFromAndToRoles(OBDal.getInstance().getConnection(),
              clientId, orgId, fromUser, documentRule, qu_next_role_id);

          delegatedFromRole = role.get("FromUserRoleId");
          delegatedToRole = role.get("ToUserRoleId");

          if (delegatedFromRole != null && delegatedToRole != null)
            nextApproval = NextRoleByRule.getDelegatedNextRole(OBDal.getInstance().getConnection(),
                clientId, orgId, delegatedFromRole, delegatedToRole, fromUser, documentRule,
                objOrder.getGrandTotalAmount());
        }
      }
      if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().equals("NoManagerAssociatedWithRole")) {
        count = 3;
      }
      // if Role doesnt has any user associated then this condition will execute and return error
      else if (nextApproval != null && nextApproval.getErrorMsg() != null
          && nextApproval.getErrorMsg().contains("EUT_NOUser_ForRoles")) {
        errorMsgs = OBMessageUtils.messageBD(nextApproval.getErrorMsg());
        errorMsgs = errorMsgs.replace("@", nextApproval.getRoleName());
        count = -2;
      } else if (nextApproval != null && nextApproval.hasApproval()) {

        ArrayList<String> includeRecipient = new ArrayList<String>();
        String appResource = null;
        if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
          appResource = "scm.pac.wfa";
        } else {
          appResource = "scm.poc.wfa";
        }

        nextRole = OBDal.getInstance().get(EutNextRole.class, nextApproval.getNextRoleId());
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objOrder.getEutNextRole(), documentRule);

        objOrder.setUpdated(new java.util.Date());
        objOrder.setUpdatedBy(OBContext.getOBContext().getUser());
        objOrder.setEscmDocaction("AP");
        objOrder.setEscmAppstatus("ESCM_IP");
        objOrder.setEutNextRole(nextRole);
        // get alert recipient - TaskNo:7618
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);

        // set alerts for next roles
        if (nextRole.getEutNextRoleLineList().size() > 0) {
          // solve approval alerts - TaskNo:7618
          AlertUtility.solveAlerts(objOrder.getId());

          forwardDao.getAlertForForwardedUser(objOrder.getId(), alertWindow, alertRuleId, objUser,
              clientId, Constants.APPROVE,
              objOrder.getDocumentNo() + "-" + objOrder.getEscmRevision()
                  + ((objOrder.getEscmNotes() != null && !objOrder.getEscmNotes().equals("null")
                      && !objOrder.getEscmNotes().equals("")) ? "-" + objOrder.getEscmNotes() : ""),
              Lang, vars.getRole(), objOrder.getEutForward(), documentRule, alertReceiversMap);

          String Description = sa.elm.ob.scm.properties.Resource.getProperty(appResource, Lang)
              + " " + objCreater.getName();

          for (EutNextRoleLine objNextRoleLine : nextRole.getEutNextRoleLineList()) {
            List<Alert> docAlert = POContractSummaryDAO
                .CheckAlert(objNextRoleLine.getRole().getId(), objOrder.getId(), clientId);

            if (docAlert != null && docAlert.size() > 0) {
              Alert objAlert = docAlert.get(0);
              objAlert.setUserContact(null);
            } else {
              if (objOrder.getEscmNotes() != null && !objOrder.getEscmNotes().equals("null")
                  && !objOrder.getEscmNotes().equals("")) {
                AlertUtility.alertInsertionRole(objOrder.getId(),
                    objOrder.getDocumentNo()
                        + "-" + objOrder.getEscmRevision() + "-" + objOrder.getEscmNotes(),
                    objNextRoleLine.getRole().getId(),
                    (objNextRoleLine.getUserContact() == null ? ""
                        : objNextRoleLine.getUserContact().getId()),
                    objOrder.getClient().getId(), Description, "NEW", alertWindow, appResource,
                    Constants.GENERIC_TEMPLATE);
              } else {
                AlertUtility.alertInsertionRole(objOrder.getId(),
                    objOrder.getDocumentNo() + "-" + objOrder.getEscmRevision(),
                    objNextRoleLine.getRole().getId(),
                    (objNextRoleLine.getUserContact() == null ? ""
                        : objNextRoleLine.getUserContact().getId()),
                    objOrder.getClient().getId(), Description, "NEW", alertWindow, appResource,
                    Constants.GENERIC_TEMPLATE);
              }
            }
            // get user name for delegated user to insert on approval history.

            /* Task #7742 */
            List<EutDocappDelegateln> delegationlnList = UtilityDAO
                .getDelegation(objNextRoleLine.getRole().getId(), currentDate, "EUT_108");
            if (delegationlnList.size() > 0) {
              for (EutDocappDelegateln obDocAppDelegation : delegationlnList) {
                List<Alert> delAlert = POContractSummaryDAO
                    .CheckAlert(obDocAppDelegation.getRole().getId(), objOrder.getId(), clientId);

                if (delAlert != null && delAlert.size() > 0) {
                  /*
                   * Alert objAlert = delAlert.get(0); objAlert.setUserContact(null);
                   */
                } else {
                  if (objOrder.getEscmNotes() != null && !objOrder.getEscmNotes().equals("null")
                      && !objOrder.getEscmNotes().equals("")) {
                    AlertUtility.alertInsertionRole(objOrder.getId(),
                        objOrder.getDocumentNo() + "-" + objOrder.getEscmRevision() + "-"
                            + objOrder.getEscmNotes(),
                        obDocAppDelegation.getRole().getId(),
                        obDocAppDelegation.getUserContact().getId(), objOrder.getClient().getId(),
                        Description, "NEW", alertWindow, appResource, Constants.GENERIC_TEMPLATE);
                  } else {
                    AlertUtility.alertInsertionRole(objOrder.getId(),
                        objOrder.getDocumentNo() + "-" + objOrder.getEscmRevision(),
                        obDocAppDelegation.getRole().getId(),
                        obDocAppDelegation.getUserContact().getId(), objOrder.getClient().getId(),
                        Description, "NEW", alertWindow, appResource, Constants.GENERIC_TEMPLATE);
                  }
                  log.debug("del role>" + obDocAppDelegation.getRole().getId());
                }

                includeRecipient.add(obDocAppDelegation.getRole().getId());
              }
              // IF next document rule have only one role and the same delegated to one user
              // then display role name with user
              if (nextRole.getEutNextRoleLineList().size() == 1 && delegationlnList.size() == 1
                  && Utility.getAssignedUserForRoles(
                      nextRole.getEutNextRoleLineList().get(0).getRole().getId()).size() == 1) {
                if (pendingapproval != null)
                  pendingapproval += "/" + objNextRoleLine.getRole().getName() + "("
                      + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + "/"
                      + delegationlnList.get(0).getRole().getName() + " - "
                      + delegationlnList.get(0).getUserContact().getName();
                else
                  pendingapproval = String.format(Constants.sWAITINGFOR_S_APPROVAL,
                      objNextRoleLine.getRole().getName() + " ("
                          + OBMessageUtils.messageBD("ESCM_Delegated_From") + ")" + " / "
                          + delegationlnList.get(0).getRole().getName() + "-"
                          + delegationlnList.get(0).getUserContact().getName());
              }
            }
            // add next role recipient
            includeRecipient.add(objNextRoleLine.getRole().getId());
          }
        }
        // existing Recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        }
        objOrder.setEscmDocaction("AP");
        if (pendingapproval == null)
          pendingapproval = nextApproval.getStatus();

        count = 2;
      } else {
        // in final approval should check budget controller already processed. else error.
        // Check Purchase Agreement has Purchase Release in status Draft or wfa
        if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
          if (POContractSummaryDAO.checkAgreementRelease(objOrder)) {
            count = 5;
            return count;
          }
        }
        // get old nextrole line user and role list
        HashMap<String, String> alertReceiversMap = forwardDao
            .getNextRoleLineList(objOrder.getEutNextRole(), documentRule);

        if (enccontrollist.size() > 0 && !objOrder.getEscmOrdertype().equals("PUR_AG")
            && (preferenceValue == null || !preferenceValue.equals("Y"))) {
          if (!objOrder.isEfinEncumbered()) {
            count = 4;
            return count;
          }
        }
        ArrayList<String> includeRecipient = new ArrayList<String>();
        objOrder.setUpdated(new java.util.Date());
        objOrder.setUpdatedBy(OBContext.getOBContext().getUser());
        Role objCreatedRole = null;
        if (objOrder.getCreatedBy().getADUserRolesList().size() > 0
            && objOrder.getEscmAdRole() != null) {
          objCreatedRole = objOrder.getEscmAdRole();
        }
        // solve approval alerts - TaskNo:7618
        AlertUtility.solveAlerts(objOrder.getId());

        // get alert recipient
        List<AlertRecipient> alrtRecList = AlertUtility.getAlertReceipient(alertRuleId);
        // check and insert recipient
        if (alrtRecList.size() > 0) {
          for (AlertRecipient objAlertReceipient : alrtRecList) {
            includeRecipient.add(objAlertReceipient.getRole().getId());
            OBDal.getInstance().remove(objAlertReceipient);
          }
        }
        forwardDao.getAlertForForwardedUser(objOrder.getId(), alertWindow, alertRuleId, objUser,
            clientId, Constants.APPROVE,
            objOrder.getDocumentNo() + "-" + objOrder.getEscmRevision()
                + ((objOrder.getEscmNotes() != null && !objOrder.getEscmNotes().equals("null")
                    && !objOrder.getEscmNotes().equals("")) ? "-" + objOrder.getEscmNotes() : ""),
            Lang, vars.getRole(), objOrder.getEutForward(), documentRule, alertReceiversMap);
        if (objCreatedRole != null)
          includeRecipient.add(objCreatedRole.getId());
        // avoid duplicate recipient
        HashSet<String> incluedSet = new HashSet<String>(includeRecipient);
        Iterator<String> iterator = incluedSet.iterator();
        while (iterator.hasNext()) {
          AlertUtility.insertAlertRecipient(iterator.next(), null, clientId, alertWindow);
        } // set alert for requester
        String appResource = null;
        if (objOrder.getEscmOrdertype().equals("PUR_AG")) {
          appResource = "scm.pac.approved";
        } else {
          appResource = "scm.poc.approved";
        }
        String Description = sa.elm.ob.scm.properties.Resource.getProperty(appResource, Lang) + " "
            + objUser.getName();
        if (objOrder.getEscmNotes() != null && !objOrder.getEscmNotes().equals("null")
            && !objOrder.getEscmNotes().equals("")) {
          AlertUtility.alertInsertionRole(objOrder.getId(),
              objOrder.getDocumentNo() + "-" + objOrder.getEscmRevision() + "-"
                  + objOrder.getEscmNotes(),
              objOrder.getEscmAdRole().getId(), objOrder.getCreatedBy().getId(),
              objOrder.getClient().getId(), Description, "NEW", alertWindow, "appResource",
              Constants.GENERIC_TEMPLATE);
        } else {
          AlertUtility.alertInsertionRole(objOrder.getId(),
              objOrder.getDocumentNo() + "-" + objOrder.getEscmRevision(),
              objOrder.getEscmAdRole().getId(), objOrder.getCreatedBy().getId(),
              objOrder.getClient().getId(), Description, "NEW", alertWindow, "appResource",
              Constants.GENERIC_TEMPLATE);
        }
        objOrder.setEscmDocaction("PD");
        objOrder.setDocumentAction("CL");
        objOrder.setDocumentStatus("CO");
        objOrder.setEscmAppstatus("ESCM_AP");
        objOrder.setProcessed(true);
        // update order contract remaining amount Task No.7470
        if (objOrder.getEfinLegacypaidAmt().compareTo(BigDecimal.ZERO) > 0
            && objOrder.getEfinInvoiceAmt() == BigDecimal.ZERO) {
          objOrder.setEfinRemainingAmt(
              objOrder.getGrandTotalAmount().subtract(objOrder.getEfinLegacypaidAmt()));
          objOrder.setEfinPaidAmt(objOrder.getEfinLegacypaidAmt());
          objOrder.setEfinInvoiceAmt(objOrder.getEfinLegacypaidAmt());
        } else {
          // objOrder.setEfinRemainingAmt(objOrder.getGrandTotalAmount());
          objOrder.setEfinRemainingAmt(
              objOrder.getGrandTotalAmount().subtract(objOrder.getEfinInvoiceAmt()));
        }
        Order order = objOrder;
        if (objOrder.getEscmOldOrder() != null) {
          order = objOrder.getEscmOldOrder();
        }
        // update released qty/amt updation in agreement
        if (objOrder.isEscmIspurchaseagreement() && objOrder.getEscmOrdertype().equals("PUR_REL")) {
          for (OrderLine line : objOrder.getOrderLineList()) {
            if (!line.isEscmIssummarylevel() && line.getEscmAgreementLine() != null) {
              OrderLine agreementLine = line.getEscmAgreementLine();
              if (objOrder.getEscmReceivetype().equals("QTY")) {
                if (!POContractSummaryDAO.isNewVersionCreatedAgainstPA(order)) {
                  agreementLine.setEscmReleaseqty(
                      agreementLine.getEscmReleaseqty().add(line.getOrderedQuantity()));
                } else {
                  agreementLine.setEscmReleaseqty(
                      agreementLine.getEscmReleaseqty().add(line.getOrderedQuantity())
                          .subtract(line.getEscmOldOrderline().getOrderedQuantity()));
                }
              }
              if (objOrder.getEscmReceivetype().equals("AMT")) {
                // boolean agreementHasTax = false;
                // if (objOrder.getEscmPurchaseagreement().isEscmIstax()
                // && objOrder.getEscmPurchaseagreement().getEscmTaxMethod() != null) {
                // agreementHasTax = true;
                // }

                releaseAmt = line.getLineNetAmount();
                // task no: 7554
                // if (!agreementHasTax && objOrder.isEscmIstax()
                // && objOrder.getEscmTaxMethod() != null) {
                // if (!objOrder.getEscmTaxMethod().isPriceIncludesTax()) {
                // releaseAmt = line.getLineNetAmount().subtract(line.getEscmLineTaxamt());
                // }
                // }
                if (!POContractSummaryDAO.isNewVersionCreatedAgainstPA(order)) {
                  agreementLine
                      .setEscmReleaseamt(agreementLine.getEscmReleaseamt().add(releaseAmt));
                } else {
                  agreementLine.setEscmReleaseamt(agreementLine.getEscmReleaseamt().add(releaseAmt)
                      .subtract(line.getEscmOldOrderline().getLineNetAmount()));
                }
              }
              OBDal.getInstance().save(agreementLine);
            }
          }
          if (!POContractSummaryDAO.isNewVersionCreatedAgainstPA(order)) {
            objOrder.getEscmPurchaseagreement()
                .setEscmReleasecount(objOrder.getEscmPurchaseagreement().getEscmReleasecount() + 1);
          }
        }

        if (objOrder.getEscmOldOrder() != null) {
          objOrder.getEscmOldOrder().setActive(false);
          OBDal.getInstance().save(objOrder.getEscmOldOrder());
        }
        objOrder.setEutNextRole(null);
        count = 1;

      }
      OBDal.getInstance().save(objOrder);

      strOrderId = objOrder.getId();
      if (!StringUtils.isEmpty(strOrderId)) {
        JSONObject historyData = new JSONObject();
        historyData.put("ClientId", clientId);
        historyData.put("OrgId", objOrder.getOrganization().getId());
        historyData.put("RoleId", roleId);
        historyData.put("UserId", userId);
        historyData.put("HeaderId", strOrderId);
        historyData.put("Comments", comments);
        historyData.put("Status", appstatus);
        historyData.put("Revision", objOrder.getEscmRevision());
        historyData.put("NextApprover", pendingapproval);
        historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
        historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
        historyData.put("ActionColumn", ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);

        // task no 6093
        // Utility.InsertApprovalHistory(historyData);
        POContractSummaryDAO.purchaseOrderApprovalHistory(historyData);

      }
      // Removing forwardRMI id
      if (objOrder.getEutForward() != null) {
        // Removing the Role Access given to the forwarded user
        // Update statuses draft the forward Record
        forwardReqMoreInfoDAO.setForwardStatusAsDraft(objOrder.getEutForward());
        // Removing Forward_Rmi id from transaction screens
        forwardReqMoreInfoDAO.revokeRemoveForwardRmiFromWindows(objOrder.getId(),
            Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY);

      }
      if (objOrder.getEutReqmoreinfo() != null) {
        // Update statuses draft the RMI Record
        forwardReqMoreInfoDAO.setForwardStatusAsDraft(objOrder.getEutReqmoreinfo());
        // access remove
        // Remove Forward_Rmi id from transaction screens
        forwardReqMoreInfoDAO.revokeRemoveRmiFromWindows(objOrder.getId(),
            Constants.PURCHASE_ORDER_AND_CONTRACT_SUMMARY);

      }

      OBDal.getInstance().flush();
      // delete the unused nextroles in eut_next_role table.
      DocumentRuleDAO.deleteUnusedNextRoles(OBDal.getInstance().getConnection(), documentRule);

    } /*
       * catch (Exception e) { log.error("Exception in updateHeaderStatus in Purchase Order: ", e);
       * OBDal.getInstance().rollbackAndClose(); }
       */
    catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.debug("Exception in updateHeaderStatus in Purchase Order:" + e);
      e.printStackTrace();
      // Throwable t = DbUtility.getUnderlyingSQLException(e);
      final OBError error = OBMessageUtils.translateError(bundle.getConnection(), vars,
          vars.getLanguage(), OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      bundle.setResult(error);
    } finally {
      /* OBContext.restorePreviousMode(); */
    }
    return count;
  }

  private boolean isDirectApproval(String RequestId, String roleId) {

    Connection con = OBDal.getInstance().getConnection();
    PreparedStatement ps = null;
    ResultSet rs = null;
    String query = null;
    try {
      query = "select count(ord.c_order_id) from c_order ord join eut_next_role rl on "
          + "ord.em_eut_next_role_id = rl.eut_next_role_id "
          + "join eut_next_role_line li on li.eut_next_role_id = rl.eut_next_role_id "
          + "and ord.c_order_id = ? and li.ad_role_id =?";

      ps = con.prepareStatement(query);
      ps.setString(1, RequestId);
      ps.setString(2, roleId);

      rs = ps.executeQuery();

      if (rs.next()) {
        if (rs.getInt("count") > 0)
          return true;
        else
          return false;
      } else
        return false;

    } catch (Exception e) {
      log.error("Exception in isDirectApproval " + e.getMessage());
      return false;
    } finally {
      // close db connection
      try {
        if (rs != null)
          rs.close();
        if (ps != null)
          ps.close();
      } catch (Exception e) {
      }
    }
  }
 

}
