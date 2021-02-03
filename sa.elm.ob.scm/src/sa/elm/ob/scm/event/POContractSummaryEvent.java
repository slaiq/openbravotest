package sa.elm.ob.scm.event;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BankAccount;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;

import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.scm.ESCMDefLookupsTypeLn;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmPurOrderActionHistory;
import sa.elm.ob.scm.ad_callouts.dao.POContractSummaryTotPOChangeDAO;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.scm.util.AlertUtilityDAO;
import sa.elm.ob.utility.EutForwardReqMoreInfo;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAO;
import sa.elm.ob.utility.ad_process.Forward.ForwardRequestMoreInfoDAOImpl;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Preferences;
import sa.elm.ob.utility.util.Utility;

public class POContractSummaryEvent extends EntityPersistenceEventObserver {

  /**
   * This class is used to handle the events in c_order table
   */

  private Logger log = Logger.getLogger(this.getClass());
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Order.ENTITY_NAME) };
  ForwardRequestMoreInfoDAO forwardReqMoreInfoDAO = new ForwardRequestMoreInfoDAOImpl();
  final String PO_Window_ID = "2ADDCB0DD2BF4F6DB13B21BBCCC3038C";

  Integer roundoffConst = 2;

  @Override
  protected Entity[] getObservedEntities() {
    // TODO Auto-generated method stub
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try {
      OBContext.setAdminMode();
      Order poheader = (Order) event.getTargetInstance();
      if (poheader.getClient().getCurrency() != null
          && poheader.getClient().getCurrency().getStandardPrecision() != null) {
        roundoffConst = poheader.getClient().getCurrency().getStandardPrecision().intValue();
      }

      final Property paymentterm = entities[0].getProperty(Order.PROPERTY_PAYMENTTERMS);
      final Property oldOrder = entities[0].getProperty(Order.PROPERTY_ESCMOLDORDER);
      final Property encumbrance = entities[0].getProperty(Order.PROPERTY_EFINBUDGETMANENCUM);
      final Property uniqueCode = entities[0].getProperty(Order.PROPERTY_EFINUNIQUECODE);
      final Property encumMethod = entities[0].getProperty(Order.PROPERTY_EFINENCUMBRANCEMETHOD);
      // final Property totPOUpdatedAmt =
      // entities[0].getProperty(Order.PROPERTY_ESCMTOTPOUPDATEDAMT);
      // final Property totAmt = entities[0].getProperty(Order.PROPERTY_GRANDTOTALAMOUNT);
      final Property documentNo = entities[0].getProperty(Order.PROPERTY_DOCUMENTNO);
      final Property totPOChangeValue = entities[0]
          .getProperty(Order.PROPERTY_ESCMTOTPOCHANGEVALUE);
      final Property totPOChangeFact = entities[0]
          .getProperty(Order.PROPERTY_ESCMTOTPOCHANGEFACTOR);
      final Property isPaymentSchedule = entities[0]
          .getProperty(Order.PROPERTY_ESCMISPAYMENTSCHEDULE);
      Date startDate = poheader.getEscmContractstartdate();
      Date endDate = poheader.getEscmContractenddate();
      // DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      // Date now = new Date();
      // Date todaydate = dateFormat.parse(dateFormat.format(now));
      final Property podate = entities[0].getProperty(Order.PROPERTY_ORDERDATE);
      String sequence = "", seqName = null, calendarId = "";
      Organization org = null;
      Long documentNumber = (long) 0;
      boolean isDocNoExists = false;
      String preferenceValue = "N";

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();

      String accountDate = new SimpleDateFormat("dd-MM-yyyy").format(poheader.getAccountingDate());
      org = OBDal.getInstance().get(Organization.class, poheader.getOrganization().getId());

      // Check Budget Controller Preference

      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", true, clientId,
            poheader.getOrganization().getId(), userId, roldId, PO_Window_ID, "N");
        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;

      } catch (PropertyException e) {
        preferenceValue = "N";
        // log.error("Exception in getting budget controller :", e);
      }

      if (!preferenceValue.equals("Y") && poheader.getEutForward() != null) {// check for
                                                                             // temporary
        // preference
        String requester_user_id = poheader.getEutForward().getUserContact().getId();
        String requester_role_id = poheader.getEutForward().getRole().getId();
        preferenceValue = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", roldId, userId, clientId, poheader.getOrganization().getId(),
            PO_Window_ID, requester_user_id, requester_role_id);
      }

      // if Budget Controller Preference not enabled then the user will not able to change the
      // values of
      // Budget related fields
      if (!preferenceValue.equals("Y") && event.getCurrentState(oldOrder) == null) {
        if (event.getCurrentState(encumbrance) != null
            || event.getCurrentState(uniqueCode) != null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }
      // To check budget initial is mandatory.
      if (poheader.getEfinBudgetint() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_Budget_Init_Mandatory"));
      }
      // tax method is mandatory if is tax lien selected as "Yes"
      if (poheader.isEscmIstax() && poheader.getEscmTaxMethod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoTaxMethod"));
      }

      // document sequence for Purchase agreement and Purchase Release
      if (poheader.isEscmIspurchaseagreement() && poheader.getEscmBaseOrder() == null) {
        if (org.getCalendar() != null) {
          calendarId = org.getCalendar().getId();
        } else {
          // get parent organization list
          String[] orgIds = null;
          SQLQuery query = OBDal.getInstance().getSession()
              .createSQLQuery("select eut_parent_org ('" + poheader.getOrganization().getId()
                  + "','" + poheader.getClient().getId() + "')");
          Object list = query.list().get(0);
          orgIds = ((String) list).split(",");
          for (int i = 0; i < orgIds.length; i++) {
            org = OBDal.getInstance().get(Organization.class, orgIds[i].replace("'", ""));
            if (org.getCalendar() != null) {
              calendarId = org.getCalendar().getId();
              break;
            }
          }
        }
        if (poheader.getEscmOrdertype().equals(Constants.PURCHASE_AGREEMENT)) {
          seqName = Constants.PURCHASE_AGREEMENT_DOC_SEQ;
        } else if (poheader.getEscmOrdertype().equals(Constants.PURCHASE_RELEASE)) {
          seqName = Constants.PURCHASE_RELEASE_DOC_SEQ;
        }
        sequence = Utility.getDocumentSequence(accountDate, seqName, calendarId,
            poheader.getOrganization().getId(), true);
        if (sequence.equals("0")) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoDocumentSequence"));
        }
        event.setCurrentState(documentNo, sequence);
      }

      // To set the default payment term in po contract summary
      OBQuery<PaymentTerm> objPayTermQry = OBDal.getInstance().createQuery(PaymentTerm.class,
          "as e order by e.creationDate desc ");
      objPayTermQry.setMaxResult(1);
      if (objPayTermQry != null && objPayTermQry.list().size() > 0) {
        event.setCurrentState(paymentterm, objPayTermQry.list().get(0));
      }
      /*
       * PaymentTerm payment = OBDal.getInstance().get(PaymentTerm.class,
       * "BF85382838A74304B9B0475A1334637C");
       */
      // Contract Enddate should greater than Contract Startdate
      if (startDate != null && endDate != null) {
        if (endDate.compareTo(startDate) == -1) {
          throw new OBException(OBMessageUtils.messageBD("Escm_ContractEnddate"));
        }
      }
      // Rate should not be Zero
      /*
       * if (poheader.getEscmRate().compareTo(new BigDecimal(0)) == 0) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_Rate_Zero")); }
       */
      if (event.getCurrentState(oldOrder) == null) {
        // Past Date is not allowed in PO Date
        /*
         * if (poheader.getOrderDate() != null) { if
         * (dateFormat.parse(dateFormat.format(poheader.getOrderDate())) .compareTo(todaydate) < 0)
         * { throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed")); } }
         */
        // Past Date is not allowed in Contract Start Date
        // if (poheader.getEscmContractstartdate() != null) {
        // if (dateFormat.parse(dateFormat.format(poheader.getEscmContractstartdate()))
        // .compareTo(todaydate) < 0) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed"));
        // }
        // }
        // Past Date is not allowed in Contract End date
        // if (poheader.getEscmContractenddate() != null) {
        // if (dateFormat.parse(dateFormat.format(poheader.getEscmContractenddate()))
        // .compareTo(todaydate) < 0) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed"));
        // }
        // }
        // Past Date is not allowed in Onboard date(H)
        // if (poheader.getEscmOnboarddateh() != null) {
        // if (dateFormat.parse(dateFormat.format(poheader.getEscmOnboarddateh()))
        // .compareTo(todaydate) < 0) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed"));
        // }
        // }
        // Past Date is not allowed in Signature Date (H)
        // if (poheader.getEscmSignaturedate() != null) {
        // if (dateFormat.parse(dateFormat.format(poheader.getEscmSignaturedate()))
        // .compareTo(todaydate) < 0) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed"));
        // }
        // }
      }
      if (poheader.getEscmAcctno() != null) {
        String hql = "update OrderLine set updated =now(), escmAcctno=:acctNo where salesOrder.id =:poId";
        Query updQuery = OBDal.getInstance().getSession().createQuery(hql);
        updQuery.setParameter("acctNo", poheader.getEscmAcctno());
        updQuery.setParameter("poId", poheader.getId());
        updQuery.executeUpdate();
      }

      // adj amt should not po full amount.
      if (poheader.getEscmAdvpaymntPercntge().compareTo(new BigDecimal(100)) >= 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_AdvAmtExceedPOTotal"));
      }
      /* Validate encumbrance cannot be empty if encumbrance method is manual */
      if (poheader.getEFINEncumbranceMethod().equals("M")) {
        if (poheader.getEfinBudgetManencum() == null && poheader.getEscmLegacycontract() == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_EncumNotEmptyForManual"));
        }
      }
      // Advance pyement % should be greater than 0
      // removed Task 6239
      /*
       * if (poheader.getEscmContractstartdate() != null && (poheader.getEscmAdvpaymntPercntge() ==
       * null || poheader.getEscmAdvpaymntPercntge().compareTo(BigDecimal.ZERO) <= 0)) { // throw
       * new OBException(OBMessageUtils.messageBD("ESCM_AdvPay_Percet_morethan_zero")); }
       */

      // Get total Amount Lookuo Id
      // String totalAmtChangeType =
      // POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
      // "01");
      // Get Percentage Lookuo Id
      String percentageChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
          "02");
      // Get Line Parent Percent Lookuo Id
      // String lineParentPercentChangeType = POContractSummaryTotPOChangeDAO
      // .getPOChangeLookUpId("TPOCHGTYP", "03");

      if (poheader.getEscmTotPoChangeType() != null && poheader.getEscmTotPoChangeValue() != null) {
        if (poheader.getEscmTotPoChangeType().getId().equals(percentageChangeType)) {
          if (poheader.getEscmTotPoChangeValue().compareTo(new BigDecimal(100)) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_POChangeValueNotGreater"));
          }
        }
        // BigDecimal poUpdatedAmt =
        // POContractSummaryTotPOChangeDAO.getTopLevelParentAmt(poheader.getId());
        // String incFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGFACT",
        // "02");
        // Sring decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGFACT",
        // "01");
        // if (poheader.getEscmTotPoChangeFactor() != null) {
        // // POChange type -Total Amount Calc
        // if (poheader.getEscmTotPoChangeType().getId().equals(totalAmtChangeType)
        // && poheader.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
        // if (poheader.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
        // poUpdatedAmt = poUpdatedAmt.subtract(poheader.getEscmTotPoChangeValue());
        // } else if (poheader.getEscmTotPoChangeFactor().getId().equals(incFactId)) {
        // poUpdatedAmt = poUpdatedAmt.add(poheader.getEscmTotPoChangeValue());
        // }
        // } // POChange type -Total Percentage Calc
        // else if (poheader.getEscmTotPoChangeType().getId().equals(totalPercentChangeType)
        // && poheader.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
        // if (poheader.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
        // poUpdatedAmt = poUpdatedAmt.subtract(poUpdatedAmt
        // .multiply(poheader.getEscmTotPoChangeValue().divide(new BigDecimal("100"))));
        // } else if (poheader.getEscmTotPoChangeFactor().getId().equals(incFactId)) {
        // poUpdatedAmt = poUpdatedAmt.add(poUpdatedAmt
        // .multiply(poheader.getEscmTotPoChangeValue().divide(new BigDecimal("100"))));
        // }
        // }
        // // set updated amt
        // event.setCurrentState(totAmt, poUpdatedAmt);
        // }
        // POChange type -Line Parent Percentage Calc
        // if (poheader.getEscmTotPoChangeType().getId().equals(lineParentPercentChangeType)
        // && poheader.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
        // if (poheader.getOrderLineList() != null && poheader.getOrderLineList().size() > 0) {
        // POContractSummaryTotPOChangeDAO.updateParentPOchange(poheader.getId(),
        // poheader.getEscmTotPoChangeValue());
        // poUpdatedAmt = POContractSummaryTotPOChangeDAO.getTopLevelParentAmt(poheader.getId());
        // // set updated amt
        // event.setCurrentState(totAmt, poUpdatedAmt);
        // }
        // }
      }

      // if (poheader.getEscmMaxRelease() != null) {
      // if (poheader.getEscmMaxRelease().compareTo(poheader.getGrandTotalAmount()) < 0) {
      // throw new OBException(OBMessageUtils.messageBD("ESCM_Agrtamt_val"));
      // }
      // }

      if (poheader.getEscmTotPoChangeType() == null) {
        event.setCurrentState(totPOChangeValue, BigDecimal.ZERO);
        event.setCurrentState(totPOChangeFact, null);
      }
      // else {
      // // set updated amt
      // event.setCurrentState(totAmt, poheader.getGrandTotalAmount());
      // }

      if (poheader.getEscmTotPoChangeType() != null && poheader.getEscmTotPoChangeValue() != null
          && poheader.getEscmTotPoChangeFactor() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangefacman"));
      } else if (poheader.getEscmTotPoChangeType() != null
          && poheader.getEscmTotPoChangeValue() == null
          && poheader.getEscmTotPoChangeFactor() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangefacman"));
      }
      // PO date for newer version should not be lesser than the older version
      Date po_date = (Date) event.getCurrentState(podate);
      if (oldOrder != null) {
        Order oldPO = (Order) event.getCurrentState(oldOrder);
        if (oldPO != null) {
          Date oldPoDate = oldPO.getOrderDate();
          if (po_date.compareTo(oldPoDate) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PO_NewVersionDate"));
          }
        }
      }
      // purchase release is selected then purchase agreement no is mandatory
      if (poheader.isEscmIspurchaseagreement() && poheader.getEscmOrdertype().equals("PUR_REL")) {
        if (poheader.getEscmPurchaseagreement() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_PurchaseAgr_Mandatory"));
        }
      }
      if (poheader.getEscmAppstatus().equals("DR") && poheader.getEscmOrdertype() != null
          && (poheader.getEscmOrdertype().equals("PUR") || poheader.getEscmOrdertype().equals("CR")
              || poheader.getEscmOrdertype().equals("PUR_AG")
              || poheader.getEscmOrdertype().equals("PUR_REL")))
        poheader.setSalesTransaction(false);

      // Task no. 7652: Document Sequence is getting skipped for PO
      if (poheader.getEscmOrdertype().equals(Constants.PURCHASE_AGREEMENT)
          || poheader.getEscmOrdertype().equals(Constants.PURCHASE_RELEASE)) {
        DocumentType docType = OBDal.getInstance().get(DocumentType.class,
            poheader.getTransactionDocument().getId());
        if (docType.getDocumentSequence() != null) {
          // get document sequence
          Sequence docseq = OBDal.getInstance().get(Sequence.class,
              docType.getDocumentSequence().getId());
          if (docseq != null) {
            documentNumber = docseq.getNextAssignedNumber() - 1;
            isDocNoExists = POContractSummaryTotPOChangeDAO.checkDocNoExists(documentNumber);
            if (!isDocNoExists) {
              docseq.setNextAssignedNumber(documentNumber);
            }
          }
        }
      }
      // if issecondsupplieriban as checked then iban as mandatory
      if (poheader.isEscmIssecondsupplier()) {
        if (poheader.getEscmSecondIban() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_second_Iban_Mandatory"));
        }
      }
      if (poheader.getEscmContactType() != null) {
        // Update Payment Schedule based on the contract category configuration
        ESCMDefLookupsTypeLn reflookuplnObj = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            poheader.getEscmContactType().getId());
        if (reflookuplnObj != null) {
          event.setCurrentState(isPaymentSchedule, reflookuplnObj.isPaymentschedule());
        } else {
          event.setCurrentState(isPaymentSchedule, false);
        }
      }

    } catch (OBException e) {
      log.error("exception while creating POContractSummaryEvent", e);
      throw new OBException(e.getMessage());
      // } catch (ParseException e) {
      // log.debug("exception while creating POContractSummaryEvent" + e);
      // e.printStackTrace();
      // throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while creating POContractSummaryEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Order poheader = (Order) event.getTargetInstance();
      if (poheader.getClient().getCurrency() != null
          && poheader.getClient().getCurrency().getStandardPrecision() != null) {
        roundoffConst = poheader.getClient().getCurrency().getStandardPrecision().intValue();
      }

      final Property paymentterm = entities[0].getProperty(Order.PROPERTY_PAYMENTTERMS);
      final Property podate = entities[0].getProperty(Order.PROPERTY_ORDERDATE);
      // final Property contractstartdate = entities[0]
      // .getProperty(Order.PROPERTY_ESCMCONTRACTSTARTDATE);
      // final Property contractenddate =
      // entities[0].getProperty(Order.PROPERTY_ESCMCONTRACTENDDATE);
      // final Property onboarddateh = entities[0].getProperty(Order.PROPERTY_ESCMONBOARDDATEH);
      // final Property signaturedate = entities[0].getProperty(Order.PROPERTY_ESCMSIGNATUREDATE);
      final Property oldOrder = entities[0].getProperty(Order.PROPERTY_ESCMOLDORDER);
      // final Property totPOUpdatedAmt =
      // entities[0].getProperty(Order.PROPERTY_ESCMTOTPOUPDATEDAMT);
      // final Property totAmt = entities[0].getProperty(Order.PROPERTY_GRANDTOTALAMOUNT);
      final Property totPOChangeValue = entities[0]
          .getProperty(Order.PROPERTY_ESCMTOTPOCHANGEVALUE);
      final Property totPOChangeFact = entities[0]
          .getProperty(Order.PROPERTY_ESCMTOTPOCHANGEFACTOR);
      final Property amountLimit = entities[0].getProperty(Order.PROPERTY_ESCMMAXRELEASE);
      final Property iban = entities[0].getProperty(Order.PROPERTY_ESCMIBAN);

      String userId = OBContext.getOBContext().getUser().getId();
      String roldId = OBContext.getOBContext().getRole().getId();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();

      final Property encumbrance = entities[0].getProperty(Order.PROPERTY_EFINBUDGETMANENCUM);
      final Property uniqueCode = entities[0].getProperty(Order.PROPERTY_EFINUNIQUECODE);
      final Property encumMethod = entities[0].getProperty(Order.PROPERTY_EFINENCUMBRANCEMETHOD);
      final Property forwardId = entities[0].getProperty(Order.PROPERTY_EUTFORWARD);
      final Property requisition_true = entities[0].getProperty(Order.PROPERTY_ESCMADDREQUISITION);
      final Property proposal_line = entities[0].getProperty(Order.PROPERTY_ESCMPROPOSALMGMT);
      final Property isPaymentSchedule = entities[0]
          .getProperty(Order.PROPERTY_ESCMISPAYMENTSCHEDULE);
      String preferenceValue = "N";

      // Check Budget Controller Preference
      try {
        preferenceValue = Preferences.getPreferenceValue("ESCM_BudgetControl", Boolean.TRUE,
            clientId, poheader.getOrganization().getId(), userId, roldId, PO_Window_ID, "N");
        preferenceValue = (preferenceValue == null) ? "N" : preferenceValue;
      } catch (PropertyException e) {
        preferenceValue = "N";
        // log.error("Exception in getting budget controller :", e);
      }
      // find out the forward reference record against the PO
      EutForwardReqMoreInfo objForward = poheader.getEutForward();
      if (objForward == null) {
        objForward = forwardReqMoreInfoDAO.findForwardReferenceAgainstTheRecord(poheader.getId(),
            userId, roldId);
      }
      if (!preferenceValue.equals("Y") && objForward != null) {// check for
        // temporary
        // preference
        String requester_user_id = objForward.getUserContact().getId();
        String requester_role_id = objForward.getRole().getId();
        preferenceValue = forwardReqMoreInfoDAO.checkAndReturnTemporaryPreference(
            "ESCM_BudgetControl", roldId, userId, clientId, poheader.getOrganization().getId(),
            PO_Window_ID, requester_user_id, requester_role_id);

      }

      // if Budget Controller Preference not
      // enabled then the user will not able to change the
      // values of
      // Budget related fields

      // if (!preferenceValue.equals("Y") && ((event.getCurrentState(forwardId) == null
      // && event.getPreviousState(forwardId) == null)
      // || (event.getCurrentState(forwardId) != null && event.getPreviousState(forwardId) != null
      // && event.getCurrentState(forwardId).equals(event.getPreviousState(forwardId)))))

      if (!preferenceValue.equals("Y") && event.getCurrentState(requisition_true).equals("N")
          && event.getCurrentState(proposal_line) == null) {
        if ((event.getCurrentState(encumbrance) != null
            && !event.getCurrentState(encumbrance).equals(event.getPreviousState(encumbrance)))
            || (event.getCurrentState(encumMethod) != null
                && !event.getCurrentState(encumMethod).equals(event.getPreviousState(encumMethod)))
            || (event.getCurrentState(uniqueCode) != null
                && !event.getCurrentState(uniqueCode).equals(event.getPreviousState(uniqueCode)))) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_Not_BudgetController_Action"));
        }
      }

      // check for agreement alone
      if (poheader.getEscmMaxRelease() != null) {
        if (!event.getCurrentState(amountLimit).equals(event.getPreviousState(amountLimit))) {
          if (poheader.getEscmMaxRelease() != null
              && poheader.getEscmOrdertype().equals("PUR_AG")) {
            if (poheader.getEscmMaxRelease()
                .compareTo(poheader.getOrderLineList().stream()
                    .filter(a -> !a.isEscmIssummarylevel()).map(x -> x.getLineNetAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add)) < 0) {
              throw new OBException(OBMessageUtils.messageBD("ESCM_Agrtamt_val"));
            }
          }
        }
      }

      if (poheader.getEscmTaxMethod() != null) {
        final Property isTaxLine = entities[0].getProperty(Order.PROPERTY_ESCMISTAX);
        final Property totalTaxAmountCur = entities[0].getProperty(Order.PROPERTY_ESCMTOTALTAXAMT);
        final Property taxMethod = entities[0].getProperty(Order.PROPERTY_ESCMTAXMETHOD);

        Boolean isTaxLinePre = (Boolean) event.getPreviousState(isTaxLine);
        Boolean isTaxLineCur = (Boolean) event.getCurrentState(isTaxLine);
        BigDecimal totalTaxAmount = (BigDecimal) event.getCurrentState(totalTaxAmountCur);

        BigDecimal lineNetAmt = BigDecimal.ZERO;
        BigDecimal unitPrice = BigDecimal.ZERO;
        BigDecimal grossLineNetAmt = BigDecimal.ZERO;
        if (!isTaxLinePre.equals(isTaxLineCur)) {
          if (isTaxLinePre) {
            if (!isTaxLineCur && totalTaxAmount.compareTo(BigDecimal.ZERO) > 0) {

              if (poheader.getOrderLineList().size() > 0) {
                for (OrderLine orderline : poheader.getOrderLineList()) {
                  if (!orderline.isEscmIssummarylevel()
                      && orderline.getLineNetAmount().compareTo(BigDecimal.ZERO) > 0) {
                    // exclusive
                    if (!poheader.getEscmTaxMethod().isPriceIncludesTax()) {
                      lineNetAmt = orderline.getLineNetAmount()
                          .subtract(orderline.getEscmLineTaxamt());
                      orderline.setLineNetAmount(lineNetAmt);
                      orderline.setEscmLineTaxamt(BigDecimal.ZERO);

                    }
                    // inclusive
                    else {
                      grossLineNetAmt = orderline.getEscmLineTotalUpdated()
                          .add(orderline.getEscmLineTaxamt());
                      // unitPrice = grossLineNetAmt.divide(orderline.getOrderedQuantity(),
                      // roundoffConst, RoundingMode.HALF_UP);
                      // orderline.setEscmLineTotalUpdated(orderline.getOrderedQuantity()
                      // .multiply(unitPrice).setScale(roundoffConst, RoundingMode.HALF_UP));
                      // orderline.setLineGrossAmount(grossLineNetAmt);
                      orderline.setUnitPrice(orderline.getEscmInitialUnitprice());
                      orderline.setEscmInitialUnitprice(BigDecimal.ZERO);
                      orderline.setEscmLineTaxamt(BigDecimal.ZERO);
                      orderline.setEscmRounddiffTax(BigDecimal.ZERO);
                      if (orderline.getEscmPoChangeType() != null)
                        POContractSummaryDAO.updateTaxAndChangeValue(true,
                            orderline.getSalesOrder(), orderline);

                    }
                    OBDal.getInstance().save(orderline);
                    // OBDal.getInstance().flush();

                  }
                }
                // throw new OBException(OBMessageUtils.messageBD("ESCM_TaxRmvSucess"));
              } else {
                throw new OBException(OBMessageUtils.messageBD("ESCM_NoLineToCalTax"));
              }
            }
            if (isTaxLinePre && !isTaxLineCur) {
              // to set null value in Tax method
              event.setCurrentState(taxMethod, null);
            }
          }
        }
      }
      Date startDate = poheader.getEscmContractstartdate();
      Date endDate = poheader.getEscmContractenddate();
      // DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
      // Date now = new Date();
      // Date todaydate = dateFormat.parse(dateFormat.format(now));
      // Contract Enddate should greater than Contract Startdate
      if (startDate != null && endDate != null) {
        if (endDate.compareTo(startDate) == -1) {
          throw new OBException(OBMessageUtils.messageBD("Escm_ContractEnddate"));
        }
      }
      // tax method is mandatory if is tax lien selected as "Yes"
      if (poheader.isEscmIstax() && poheader.getEscmTaxMethod() == null) {
        throw new OBException(OBMessageUtils.messageBD("Efin_NoTaxMethod"));
      }
      // Rate should not be Zero
      /*
       * if (poheader.getEscmRate().compareTo(new BigDecimal(0)) == 0) { throw new
       * OBException(OBMessageUtils.messageBD("Escm_Rate_Zero")); }
       */
      // To set the default payment term in po contract summary
      if (!event.getCurrentState(paymentterm).equals(event.getPreviousState(paymentterm))) {
        OBQuery<PaymentTerm> objPayTermQry = OBDal.getInstance().createQuery(PaymentTerm.class,
            "as e order by e.creationDate desc ");
        objPayTermQry.setMaxResult(1);
        if (objPayTermQry != null && objPayTermQry.list().size() > 0) {
          event.setCurrentState(paymentterm, objPayTermQry.list().get(0));
        }
        /*
         * PaymentTerm payment = OBDal.getInstance().get(PaymentTerm.class,
         * "BF85382838A74304B9B0475A1334637C"); event.setCurrentState(paymentterm, payment);
         */
      }
      // Past Date is not allowed in PO Date
      if (event.getCurrentState(oldOrder) == null) {
        /*
         * if (poheader.getOrderDate() != null &&
         * (!event.getPreviousState(podate).equals(event.getCurrentState(podate)))) { if
         * (dateFormat.parse(dateFormat.format(event.getCurrentState(podate))) .compareTo(todaydate)
         * < 0) { throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed")); } }
         */
        // //Past Date is not allowed in Contract Start Date
        // if (poheader.getEscmContractstartdate() != null) {
        // if ((event.getPreviousState(contractstartdate) != null && !event
        // .getPreviousState(contractstartdate).equals(event.getCurrentState(contractstartdate)))
        // || event.getPreviousState(contractstartdate) == null) {
        // if (dateFormat.parse(dateFormat.format(event.getCurrentState(contractstartdate)))
        // .compareTo(todaydate) < 0) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed"));
        // }
        // }
        // }
        // // Past Date is not allowed in Contract End date
        // if (poheader.getEscmContractenddate() != null) {
        // if ((event.getPreviousState(contractenddate) != null && (!event
        // .getPreviousState(contractenddate).equals(event.getCurrentState(contractenddate))))
        // || event.getPreviousState(contractenddate) == null) {
        // if (dateFormat.parse(dateFormat.format(event.getCurrentState(contractenddate)))
        // .compareTo(todaydate) < 0) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed"));
        // }
        // }
        // }
        // //Past Date is not allowed in Onboard date(H)
        // if (poheader.getEscmOnboarddateh() != null) {
        // if ((event.getPreviousState(onboarddateh) != null && (!event
        // .getPreviousState(onboarddateh).equals(event.getCurrentState(onboarddateh))))
        // || event.getPreviousState(onboarddateh) == null) {
        // if (dateFormat.parse(dateFormat.format(event.getCurrentState(onboarddateh)))
        // .compareTo(todaydate) < 0) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed"));
        // }
        // }
        // }
        // // Past Date is not allowed in Signature Date (H)
        // if (poheader.getEscmSignaturedate() != null && (!event.getPreviousState(signaturedate)
        // .equals(event.getCurrentState(signaturedate)))) {
        // if (dateFormat.parse(dateFormat.format(event.getCurrentState(signaturedate)))
        // .compareTo(todaydate) < 0) {
        // throw new OBException(OBMessageUtils.messageBD("ESCM_PastDate_Not_Allowed"));
        // }
        // }
      }
      if (poheader.getEscmAcctno() != null) {
        String hql = "update OrderLine set updated =now(), escmAcctno=:acctNo where salesOrder.id =:poId";
        Query updQuery = OBDal.getInstance().getSession().createQuery(hql);
        updQuery.setParameter("acctNo", poheader.getEscmAcctno());
        updQuery.setParameter("poId", poheader.getId());
        updQuery.executeUpdate();
      }
      // adj amt should not po full amount.
      if (poheader.getEscmAdvpaymntPercntge() != null
          && poheader.getEscmAdvpaymntPercntge().compareTo(new BigDecimal(100)) >= 0) {
        throw new OBException(OBMessageUtils.messageBD("Escm_AdvAmtExceedPOTotal"));
      }
      /* Validate encumbrance cannot be empty if encumbrance method is manual */
      if (poheader.getEFINEncumbranceMethod().equals("M")) {
        if (poheader.getEfinBudgetManencum() == null && poheader.getEscmLegacycontract() == null) {
          throw new OBException(OBMessageUtils.messageBD("EFIN_EncumNotEmptyForManual"));
        }
      }

      // Advance pyement % should be greater than 0
      // removed for Task 6239
      /*
       * if (poheader.getEscmContractstartdate() != null && (poheader.getEscmAdvpaymntPercntge() ==
       * null || poheader.getEscmAdvpaymntPercntge().compareTo(BigDecimal.ZERO) <= 0)) { // throw
       * new OBException(OBMessageUtils.messageBD("ESCM_AdvPay_Percet_morethan_zero")); }
       */

      // Get total Amount Lookuo Id
      // String totalAmtChangeType =
      // POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
      // "01");
      // Get total Percent Lookuo Id
      String percentageChangeType = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGTYP",
          "02");
      // Get Line Parent Percent Lookuo Id
      // String lineParentPercentChangeType = POContractSummaryTotPOChangeDAO
      // .getPOChangeLookUpId("TPOCHGTYP", "03");

      if (poheader.getEscmTotPoChangeType() != null && poheader.getEscmTotPoChangeValue() != null) {
        if (poheader.getEscmTotPoChangeType().getId().equals(percentageChangeType)) {
          if (poheader.getEscmTotPoChangeValue().compareTo(new BigDecimal(100)) > 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_POChangeValueNotGreater"));
          }
        }
        // if (poheader.getEscmTotPoChangeFactor() != null) {
        // BigDecimal poUpdatedAmt = POContractSummaryTotPOChangeDAO
        // .getTopLevelParentAmt(poheader.getId());
        //
        // String incFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGFACT",
        // "02");
        // String decFactId = POContractSummaryTotPOChangeDAO.getPOChangeLookUpId("TPOCHGFACT",
        // "01");
        // // POChange type -Total Amount Calc
        // if (poheader.getEscmTotPoChangeType().getId().equals(totalAmtChangeType)
        // && poheader.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
        // if (poheader.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
        // poUpdatedAmt = poUpdatedAmt.subtract(poheader.getEscmTotPoChangeValue());
        // } else if (poheader.getEscmTotPoChangeFactor().getId().equals(incFactId)) {
        // poUpdatedAmt = poUpdatedAmt.add(poheader.getEscmTotPoChangeValue());
        // }
        // } // POChange type -Total Percentage Calc
        // else if (poheader.getEscmTotPoChangeType().getId().equals(totalPercentChangeType)
        // && poheader.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
        // if (poheader.getEscmTotPoChangeFactor().getId().equals(decFactId)) {
        // poUpdatedAmt = poUpdatedAmt.subtract(poUpdatedAmt
        // .multiply(poheader.getEscmTotPoChangeValue().divide(new BigDecimal("100"))));
        // } else if (poheader.getEscmTotPoChangeFactor().getId().equals(incFactId)) {
        // poUpdatedAmt = poUpdatedAmt.add(poUpdatedAmt
        // .multiply(poheader.getEscmTotPoChangeValue().divide(new BigDecimal("100"))));
        // }
        // }
        // // set updated amt
        // event.setCurrentState(totAmt, poUpdatedAmt);
        // }
        // POChange type -Line Parent Percentage Calc
        // if (poheader.getEscmTotPoChangeType().getId().equals(lineParentPercentChangeType)
        // && poheader.getEscmTotPoChangeValue().compareTo(BigDecimal.ZERO) > 0) {
        // if (poheader.getOrderLineList() != null && poheader.getOrderLineList().size() > 0) {
        // POContractSummaryTotPOChangeDAO.updateParentPOchange(poheader.getId(),
        // poheader.getEscmTotPoChangeValue());
        // poUpdatedAmt = POContractSummaryTotPOChangeDAO.getTopLevelParentAmt(poheader.getId());
        // // set updated amt
        // event.setCurrentState(totAmt, poUpdatedAmt);
        // }
        // }
      }
      if (poheader.getEscmTotPoChangeType() == null) {
        event.setCurrentState(totPOChangeValue, BigDecimal.ZERO);
        event.setCurrentState(totPOChangeFact, null);
      }
      // else {
      // // set updated amt
      // event.setCurrentState(totAmt, poheader.getGrandTotalAmount());
      // }

      if (poheader.getEscmTotPoChangeType() != null && poheader.getEscmTotPoChangeValue() != null
          && poheader.getEscmTotPoChangeFactor() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangefacman"));
      } else if (poheader.getEscmTotPoChangeType() != null
          && poheader.getEscmTotPoChangeValue() == null
          && poheader.getEscmTotPoChangeFactor() == null) {
        throw new OBException(OBMessageUtils.messageBD("ESCM_POChangefacman"));
      }
      // PO date for newer version should not be lesser than the older version
      Date po_date = (Date) event.getCurrentState(podate);
      if (oldOrder != null) {
        Order oldPO = (Order) event.getCurrentState(oldOrder);
        if (oldPO != null) {
          Date oldPoDate = oldPO.getOrderDate();
          if (po_date.compareTo(oldPoDate) < 0) {
            throw new OBException(OBMessageUtils.messageBD("ESCM_PO_NewVersionDate"));
          }
        }
      }
      // purchase release is selected then purchase agreement no is mandatory
      if (poheader.isEscmIspurchaseagreement() && poheader.getEscmOrdertype().equals("PUR_REL")) {
        if (poheader.getEscmPurchaseagreement() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_PurchaseAgr_Mandatory"));
        }
      }

      // If IBAN is changed in PO and if RDV is created already, change IBAN in RDV also
      if ((event.getPreviousState(iban) == null && event.getCurrentState(iban) != null)
          || (event.getPreviousState(iban) != null && event.getCurrentState(iban) != null
              && !event.getPreviousState(iban).equals(event.getCurrentState(iban)))) {

        if (poheader.getEscmAppstatus().equals("ESCM_AP")) {
          // Get RDV against the Purchase Order
          OBQuery<EfinRDV> rdvQry = OBDal.getInstance().createQuery(EfinRDV.class,
              " as e where ( e.salesOrder.id = :salesOrderID or e.salesOrder.id = :baseOrderID or e.salesOrder.id = :oldOrderId)");
          rdvQry.setNamedParameter("salesOrderID", poheader.getId());
          rdvQry.setNamedParameter("baseOrderID",
              poheader.getEscmBaseOrder() != null ? poheader.getEscmBaseOrder().getId() : null);
          rdvQry.setNamedParameter("oldOrderId",
              poheader.getEscmOldOrder() != null ? poheader.getEscmOldOrder().getId() : null);
          if (rdvQry != null) {
            List<EfinRDV> rdvList = rdvQry.list();
            if (rdvList.size() > 0) {
              for (EfinRDV rdv : rdvList) {
                BankAccount objIban = (BankAccount) event.getCurrentState(iban);
                if (objIban != null) {
                  rdv.setPartnerBankAccount(objIban);
                  rdv.setEfinBank(objIban.getEfinBank());
                  if (objIban.getIBAN() != null) {
                    rdv.setIBAN(objIban.getIBAN());
                  }
                }
              }
            }
          }
        }
      } else if (event.getPreviousState(iban) != null && event.getCurrentState(iban) == null) {
        if (poheader.getEscmAppstatus().equals("ESCM_AP")) {
          OBQuery<EfinRDV> rdvQry = OBDal.getInstance().createQuery(EfinRDV.class,
              " as e where ( e.salesOrder.id = :salesOrderID or e.salesOrder.id = :baseOrderID or e.salesOrder.id = :oldOrderId)");
          rdvQry.setNamedParameter("salesOrderID", poheader.getId());
          rdvQry.setNamedParameter("baseOrderID",
              poheader.getEscmBaseOrder() != null ? poheader.getEscmBaseOrder().getId() : null);
          rdvQry.setNamedParameter("oldOrderId",
              poheader.getEscmOldOrder() != null ? poheader.getEscmOldOrder().getId() : null);
          if (rdvQry != null) {
            List<EfinRDV> rdvList = rdvQry.list();
            if (rdvList.size() > 0) {
              for (EfinRDV rdv : rdvList) {
                BankAccount objIban = (BankAccount) event.getCurrentState(iban);
                if (objIban == null) {
                  rdv.setPartnerBankAccount(null);
                  rdv.setEfinBank(null);
                  rdv.setIBAN(null);
                }
              }
            }
          }
        }
      }
      // if issecondsupplieriban as checked then iban as mandatory
      if (poheader.isEscmIssecondsupplier()) {
        if (poheader.getEscmSecondIban() == null) {
          throw new OBException(OBMessageUtils.messageBD("Escm_second_Iban_Mandatory"));
        }
      }

      if (poheader.getEscmContactType() != null) {
        // Update Payment Schedule based on the contract category configuration
        ESCMDefLookupsTypeLn reflookuplnObj = OBDal.getInstance().get(ESCMDefLookupsTypeLn.class,
            poheader.getEscmContactType().getId());
        if (reflookuplnObj != null) {
          event.setCurrentState(isPaymentSchedule, reflookuplnObj.isPaymentschedule());
        } else {
          event.setCurrentState(isPaymentSchedule, false);
        }
      }

    } catch (OBException e) {
      log.error("exception while updating POContractSummaryEvent", e);
      throw new OBException(e.getMessage());
      // } catch (ParseException e) {
      // log.debug("exception while updating POContractSummaryEvent" + e);
      // e.printStackTrace();
      // throw new OBException(e.getMessage());
    } catch (Exception e) {
      log.error("exception while updating POContractSummaryEvent", e);
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    String AccountDate = "";
    String seqName = null;
    Boolean isYearBased = false;
    String alertWindow = sa.elm.ob.scm.util.AlertWindow.contractUser;
    if (!isValidEvent(event)) {
      return;
    }
    try {
      OBContext.setAdminMode();
      Order order = (Order) event.getTargetInstance();

      if (order.getDocumentStatus().equals("DR") && order.getEscmProposalmgmt() != null) {
        order.getEscmProposalmgmt().setDocumentNo(null);
        OBDal.getInstance().save(order);

        // Updating the PO reference in PEE(Proposal Attribute)
        // Fetching the PEE irrespective of Proposal Version
        OBQuery<EscmProposalAttribute> proposalAttr = OBDal.getInstance().createQuery(
            EscmProposalAttribute.class,
            " as a  join a.escmProposalevlEvent b where b.status='CO' and a.escmProposalmgmt.proposalno= :proposalID ");
        proposalAttr.setNamedParameter("proposalID", order.getEscmProposalmgmt().getProposalno());
        List<EscmProposalAttribute> proposalAttrList = proposalAttr.list();
        if (proposalAttrList.size() > 0) {
          EscmProposalAttribute proposalAttrObj = proposalAttrList.get(0);
          proposalAttrObj.setOrder(null);
          OBDal.getInstance().save(proposalAttrObj);
        }

        OBDal.getInstance().flush();
      }

      if (order.getEscmAppstatus().equals("ESCM_RA")
          && order.getEscmPurorderacthistList().size() > 0 && order.getEscmRevision() > 0) {
        java.util.List<EscmPurOrderActionHistory> delhistorylist = order
            .getEscmPurorderacthistList();
        for (EscmPurOrderActionHistory delhistory : delhistorylist) {
          OBDal.getInstance().remove(delhistory);
        }

        java.util.List<OrderLine> ordLnlist = order.getOrderLineList();
        for (OrderLine ordLn : ordLnlist) {
          ordLn.setEscmOldOrderline(null);
          OBDal.getInstance().flush();
          OBDal.getInstance().save(ordLn);
        }
      }
      if (order.getEscmOldOrder() == null && order.getEscmBaseOrder() == null) {
        // update 'next assigned no' with previous no. in document seq while delete the recent
        // record for reusing the document sequence task no - 7409

        // get document type
        DocumentType docType = OBDal.getInstance().get(DocumentType.class,
            order.getTransactionDocument().getId());
        if (docType.getDocumentSequence() != null) {
          // get document sequence
          Sequence docseq = OBDal.getInstance().get(Sequence.class,
              docType.getDocumentSequence().getId());
          seqName = docseq.getName();
          if (order.getEscmOrdertype().equals(Constants.PURCHASE_AGREEMENT)) {
            seqName = Constants.PURCHASE_AGREEMENT_DOC_SEQ_NAME;
            isYearBased = true;
          } else if (order.getEscmOrdertype().equals(Constants.PURCHASE_RELEASE)) {
            seqName = Constants.PURCHASE_RELEASE_DOC_SEQ_NAME;
            isYearBased = true;
          }
          AccountDate = new SimpleDateFormat("dd-MM-yyyy").format(order.getAccountingDate());

          Utility.setDocumentSequenceAfterDeleteRecord(AccountDate, seqName,
              order.getOrganization().getId(), Long.parseLong(order.getDocumentNo()), null,
              isYearBased);
        }
      }
      if (order.getEscmProposalmgmt() != null) {
        // check if the contract user have alert then need to revert
        OBQuery<AlertRule> queryAlertRule = OBDal.getInstance().createQuery(AlertRule.class,
            "as e where e.client.id='" + order.getClient().getId() + "' and e.eSCMProcessType='"
                + alertWindow + "' order by e.creationDate desc");
        queryAlertRule.setMaxResult(1);
        if (queryAlertRule.list().size() > 0) {
          String alertRuleId = queryAlertRule.list().get(0).getId();
          AlertUtilityDAO.deleteAlertPreference(order.getId(), alertRuleId);
        }
      }
    } catch (Exception e) {
      log.error(" Exception while Deleting POContractSummaryEvent  : ", e);
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
