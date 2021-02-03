package sa.elm.ob.finance.ad_process.RDVProcess;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyConflictException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.finance.EfinPenaltyTypes;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.finance.EfinRdvTxnLineRef;
import sa.elm.ob.finance.Efin_RDv_Types;
import sa.elm.ob.finance.ad_process.RDVProcess.DAO.PenaltyActionDAO;
import sa.elm.ob.utility.util.Constants;
import sa.elm.ob.utility.util.Utility;

/**
 * 
 * @author Gopinagh.R
 *
 */
public class AddDefaultPenaltyDAOImpl implements AddDefaultPenaltyDAO {

  private static final Logger log4j = Logger.getLogger(AddDefaultPenaltyDAOImpl.class);
  private static final String DIRECT_PO = "POD";
  public static final String DELAYED_PENALTY = "DP";
  private static final String PENALTY_ACTION = "AD";
  private static final String PENALTY_ACCOUNT_TYPE_E = "E";
  private static final String PENALTY_ACCOUNT_TYPE_A = "A";
  private static final BigDecimal PERCENT = new BigDecimal("100");
  private static final String DEFAULT_PENALTY_CODE_PREFERENCE = "RDV_Penalty_Code";

  @Override
  public Boolean isPenaltyApplicable(String strRDVTrxLineID) {
    Boolean canApplyPenalty = Boolean.TRUE;
    Date receivedOn = null;
    Date needByDate = null;

    try {
      EfinRDVTxnline rdvLine = Utility.getObject(EfinRDVTxnline.class, strRDVTrxLineID);

      if (rdvLine != null) {
        EfinRDV rdvHeader = rdvLine.getEfinRdv();
        OrderLine orderLine = rdvLine.getSalesOrderLine();
        ShipmentInOut poReceipt = rdvHeader.getGoodsShipment();

        if (rdvHeader != null) {
          if (DIRECT_PO.equals(rdvHeader.getTXNType()) || rdvLine.isAdvance()) {
            canApplyPenalty = Boolean.FALSE;
          } else {

            canApplyPenalty = isPenaltyEnabled(strRDVTrxLineID);

            if (canApplyPenalty) {

              needByDate = orderLine.getEscmNeedbydate();

              if (poReceipt != null)
                receivedOn = poReceipt.getMovementDate();
              else {
                receivedOn = getMaximumReceivedDate(strRDVTrxLineID);
              }

              if (needByDate.after(receivedOn) || needByDate.equals(receivedOn)) {
                canApplyPenalty = Boolean.FALSE;
              }
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Excdeption while isPenaltyApplicable(): " + e);
    }
    return canApplyPenalty;
  }

  @Override
  public Boolean isPenaltyEnabled(String strRDVTrxLineID) {
    Boolean isPenaltyEnabled = Boolean.FALSE;
    String strWhereClause = "";
    try {
      EfinRDVTxnline rdvLine = Utility.getObject(EfinRDVTxnline.class, strRDVTrxLineID);

      if (rdvLine != null) {
        EfinRDV rdvHeader = rdvLine.getEfinRdv();
        Efin_RDv_Types maintenanceType = null;

        strWhereClause = " where transactionType =:transactionType and client.id=:clientId";
        List<Efin_RDv_Types> rdvMaintenanceTypes = new ArrayList<Efin_RDv_Types>();

        OBQuery<Efin_RDv_Types> rdvMaintenanceQuery = OBDal.getInstance()
            .createQuery(Efin_RDv_Types.class, strWhereClause);
        rdvMaintenanceQuery.setNamedParameter("transactionType", rdvHeader.getTXNType());
        rdvMaintenanceQuery.setNamedParameter("clientId", rdvHeader.getClient().getId());

        if (rdvMaintenanceQuery != null) {
          rdvMaintenanceTypes = rdvMaintenanceQuery.list();

          if (rdvMaintenanceTypes.size() > 0) {
            maintenanceType = rdvMaintenanceTypes.get(0);

            if (maintenanceType != null) {
              isPenaltyEnabled = maintenanceType.isEnablePenalty();
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Excdeption while isPenaltyEnabled(): " + e);
    }

    return isPenaltyEnabled;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Date getMaximumReceivedDate(String strRDVTrxLineID) {
    Date receivedDate = null;

    try {
      EfinRDVTxnline rdvLine = Utility.getObject(EfinRDVTxnline.class, strRDVTrxLineID);
      List<Object> receipts = new ArrayList<Object>();

      if (rdvLine != null) {
        OrderLine orderLine = rdvLine.getSalesOrderLine();

        StringBuffer queryBuffer = new StringBuffer();

        queryBuffer.append(" select max(movementdate ) from efin_rdvtxnline r ");
        queryBuffer.append(" join c_orderline o on o.c_orderline_id = r.c_orderline_id ");
        queryBuffer.append(" join escm_initialreceipt ir on  o.c_orderline_id = ir.c_orderline_id");
        queryBuffer.append(" join m_inout io on io.m_inout_id = ir.m_inout_id ");
        queryBuffer.append(" where r.c_orderline_id  =:orderLineId and  ");
        queryBuffer.append(" io.em_escm_receivingtype  in ('SR', 'DEL','PROJ')");

        SQLQuery receiptQuery = OBDal.getInstance().getSession()
            .createSQLQuery(queryBuffer.toString());
        receiptQuery.setParameter("orderLineId", orderLine.getId());

        if (receiptQuery != null) {
          receipts = receiptQuery.list();

          if (receipts.size() > 0) {
            receivedDate = (Date) receipts.get(0);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Excdeption while getMaximumReceivedDate(): " + e);
    }

    return receivedDate;
  }

  @Override
  public EfinPenaltyTypes getDelayedPenaltyType(String strClientID) {
    EfinPenaltyTypes penaltyType = null;
    List<EfinPenaltyTypes> penaltyTypes = new ArrayList<EfinPenaltyTypes>();

    try {
      String strWhereClause = " where deductiontype.code = :deductionType and client.id =:clientID and enable='Y'";

      OBQuery<EfinPenaltyTypes> penaltyTypeQuery = OBDal.getInstance()
          .createQuery(EfinPenaltyTypes.class, strWhereClause);
      penaltyTypeQuery.setNamedParameter("deductionType", DELAYED_PENALTY);
      penaltyTypeQuery.setNamedParameter("clientID", strClientID);

      if (penaltyTypeQuery != null) {
        penaltyTypes = penaltyTypeQuery.list();

        if (penaltyTypes.size() > 0) {
          penaltyType = penaltyTypes.get(0);
        }
      }
    } catch (Exception e) {
      log4j.error("Excdeption while getDelayedPenaltyType(): " + e);
    }

    return penaltyType;
  }

  @Override
  public Boolean addPenalty(String strRDVTrxLineID, EfinPenaltyTypes penaltyTypes,
      BigDecimal matchQty, String actionDate, String strAdvanceDeductionAmount, String strMatchAmt)
      throws RDVException {
    Boolean penaltyAdded = Boolean.TRUE;
    String receiveType = null;
    try {
      Connection connection = OBDal.getInstance().getConnection();
      PenaltyActionDAO dao = new PenaltyActionDAO(connection);
      EfinRDVTxnline rdvLine = Utility.getObject(EfinRDVTxnline.class, strRDVTrxLineID);

      BigDecimal unitPrice = BigDecimal.ZERO, matchAmount = BigDecimal.ZERO,
          penaltyPercent = BigDecimal.ONE, penaltyAmount = BigDecimal.ZERO,
          netMatchAmount = BigDecimal.ZERO, advanceDeduction = BigDecimal.ZERO;

      Long sequenceNo = 10L;
      String strSequenceNo = "", strAmount = "", strPenaltyPercent = "", strPenaltyAmount = "",
          strPenaltyAcctType = "";

      if (rdvLine != null) {
        // checking order is amt based or qty based Task No.7286
        if (rdvLine.getEfinRdv() != null && rdvLine.getEfinRdv().getSalesOrder() != null
            && rdvLine.getEfinRdv().getSalesOrder().getEscmReceivetype() != null
            && rdvLine.getEfinRdv().getSalesOrder().getEscmReceivetype().equals("AMT")) {
          receiveType = Constants.AMOUNT_BASED;
        } else {
          receiveType = Constants.QTY_BASED;
        }
        BusinessPartner partner = rdvLine.getSalesOrderLine().getSalesOrder().getBusinessPartner();
        String defaultUniqueCode = "";

        JSONObject combinationObject = getDefaultUniqueCode(strRDVTrxLineID,
            rdvLine.getClient().getId());

        if (combinationObject.has("HasError") && !combinationObject.getBoolean("HasError")) {

          defaultUniqueCode = combinationObject.has("PenaltyCodeId")
              ? combinationObject.getString("PenaltyCodeId")
              : "";
          strPenaltyAcctType = combinationObject.has("Type") ? combinationObject.getString("Type")
              : PENALTY_ACCOUNT_TYPE_E;

          unitPrice = rdvLine.getUnitCost();
          if (receiveType.equals(Constants.QTY_BASED))
            matchAmount = unitPrice.multiply(matchQty);
          else
            matchAmount = new BigDecimal(strMatchAmt);
          sequenceNo = getSequenceNumber(strRDVTrxLineID);
          penaltyPercent = penaltyTypes.getThreshold();
          penaltyAmount = matchAmount.multiply(penaltyPercent).divide(PERCENT,
              RoundingMode.HALF_UP);

          advanceDeduction = new BigDecimal(strAdvanceDeductionAmount);
          netMatchAmount = matchAmount.subtract(advanceDeduction.add(penaltyAmount));

          strSequenceNo = String.valueOf(sequenceNo);
          strAmount = String.valueOf(matchAmount);
          strPenaltyPercent = String.valueOf(penaltyPercent);
          strPenaltyAmount = String.valueOf(penaltyAmount);

          if (netMatchAmount.compareTo(BigDecimal.ZERO) < 0) {
            penaltyAdded = Boolean.FALSE;
            throw new RDVException(OBMessageUtils.messageBD("Efin_NegativeNetAmount"));
          }

          if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0 && penaltyAdded) {

            dao.getPenaltyAction(strSequenceNo, rdvLine.getTrxappNo(), rdvLine.getClient().getId(),
                PENALTY_ACTION, actionDate, strAmount, penaltyTypes.getId(), strPenaltyPercent,
                strPenaltyAmount, "", "", partner.getId(), partner.getName(), "N", null, "0.00",
                strRDVTrxLineID, strPenaltyAcctType, defaultUniqueCode, true);
          }
        } else {
          penaltyAdded = Boolean.FALSE;
        }
      }

    } catch (RDVException e) {
      penaltyAdded = Boolean.FALSE;
      throw new RDVException(e.getMessage());
    } catch (Exception e) {
      penaltyAdded = Boolean.FALSE;
      log4j.error("Excdeption while addPenalty(): " + e);

    }
    return penaltyAdded;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Long getSequenceNumber(String strRDVTrxLineID) {
    Long sequenceNumber = 10L;

    try {
      String hqlQuery = "select max(sequenceNumber) from efin_penalty_action where efinRdvtxnline.id = :strRDVTrxLineID ";

      Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
      query.setParameter("strRDVTrxLineID", strRDVTrxLineID);
      List<Object> sequenceNumberList = new ArrayList<Object>();

      if (query != null) {
        sequenceNumberList = query.list();
        if (sequenceNumberList.size() > 0) {
          sequenceNumber = sequenceNumberList.get(0) == null ? 10L
              : Long.parseLong(sequenceNumberList.get(0).toString()) + 10;
        }
      }
    } catch (Exception e) {
      log4j.error("Excdeption while getSequenceNumber(): " + e);
    }
    return sequenceNumber;
  }

  @Override
  public JSONObject defaultValidations(String strRDVTrxLineID, String strMatchQty,
      String strMatchAmt) {
    JSONObject validationObject = new JSONObject();
    String receiveType = null;
    BigDecimal available_Amt = BigDecimal.ZERO;

    BigDecimal matchAmt = BigDecimal.ZERO;
    try {
      EfinRDVTxnline rdvLine = Utility.getObject(EfinRDVTxnline.class, strRDVTrxLineID);
      // checking order is amt based or qty based Task No.7286
      if (rdvLine.getEfinRdv() != null && rdvLine.getEfinRdv().getSalesOrder() != null
          && rdvLine.getEfinRdv().getSalesOrder().getEscmReceivetype() != null
          && rdvLine.getEfinRdv().getSalesOrder().getEscmReceivetype().equals("AMT")) {
        receiveType = Constants.AMOUNT_BASED;
      } else {
        receiveType = Constants.QTY_BASED;
      }
      validationObject.put("addPenalty", "true");

      if (rdvLine != null) {
        if (receiveType.equals(Constants.QTY_BASED)) {
          BigDecimal matchQty = new BigDecimal(strMatchQty);

          // new condition based on poreceipt reference.
          BigDecimal available_Qty = BigDecimal.ZERO;
          for (EfinRdvTxnLineRef ref : rdvLine.getEfinRdvTxnLineRefList()) {
            available_Qty = available_Qty.add(ref.getEscmInitialreceipt().getDeliveredQty()
                .subtract(ref.getEscmInitialreceipt().getMatchQty()));
          }
          available_Qty = available_Qty.add(rdvLine.getMatchQty());

          if (matchQty.compareTo(available_Qty) > 0) {
            validationObject.put("addPenalty", "false");
            validationObject.put("message", OBMessageUtils.messageBD("Efin_RdvLine_QtyExceeds"));
            return validationObject;
          }
        } else {
          matchAmt = new BigDecimal(strMatchAmt);

          // new condition based on poreceipt reference.
          for (EfinRdvTxnLineRef ref : rdvLine.getEfinRdvTxnLineRefList()) {
            available_Amt = available_Amt.add(ref.getEscmInitialreceipt().getDeliveredAmt()
                .subtract(ref.getEscmInitialreceipt().getMatchAmt()));
          }
          available_Amt = available_Amt.add(rdvLine.getMatchAmt());

          if (matchAmt.compareTo(available_Amt) > 0) {
            validationObject.put("addPenalty", "false");
            validationObject.put("message", OBMessageUtils.messageBD("Efin_RdvLine_AmtExceeds"));
            return validationObject;
          }

        }
      }
    } catch (Exception e) {
      log4j.error("Excdeption while defaultValidations(): " + e);
      try {
        validationObject.put("result", "false");
        validationObject.put("message", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {
      }
    }
    return validationObject;
  }

  @Override
  public JSONObject getDefaultUniqueCode(String strRDVTrxLineID, String strClientId) {
    JSONObject combinationObject = null;
    String defaultUniqueCode = "";
    AccountingCombination defaultAccount = null;
    AccountingCombination lineAccount = null;
    EfinRDVTxnline rdvLine = null;
    String strPenaltyAcctType = "";
    try {
      rdvLine = Utility.getObject(EfinRDVTxnline.class, strRDVTrxLineID);
      lineAccount = rdvLine == null ? null : rdvLine.getAccountingCombination();

      try {
        defaultUniqueCode = Preferences.getPreferenceValue(DEFAULT_PENALTY_CODE_PREFERENCE,
            Boolean.FALSE, strClientId, null, null, null, null);

        defaultUniqueCode = StringUtils.isNotEmpty(defaultUniqueCode) ? defaultUniqueCode.trim()
            : defaultUniqueCode;

        if (StringUtils.isNotEmpty(defaultUniqueCode) && defaultUniqueCode.length() == 32) {
          defaultAccount = Utility.getObject(AccountingCombination.class, defaultUniqueCode);
          if (defaultAccount != null) {

            strPenaltyAcctType = PENALTY_ACCOUNT_TYPE_A;
          } else {
            defaultAccount = lineAccount;
            defaultUniqueCode = lineAccount.getId();
            strPenaltyAcctType = PENALTY_ACCOUNT_TYPE_E;
          }
        } else {
          defaultAccount = lineAccount;
          defaultUniqueCode = lineAccount.getId();
          strPenaltyAcctType = PENALTY_ACCOUNT_TYPE_E;
        }
      } catch (PropertyNotFoundException e) {
        defaultAccount = lineAccount;
        defaultUniqueCode = lineAccount == null ? "" : lineAccount.getId();
        strPenaltyAcctType = PENALTY_ACCOUNT_TYPE_E;
      } catch (PropertyConflictException e) {
        defaultAccount = lineAccount;
        defaultUniqueCode = lineAccount.getId();
        strPenaltyAcctType = PENALTY_ACCOUNT_TYPE_E;
      }

      combinationObject = new JSONObject();

      combinationObject.put("PenaltyCodeId", defaultUniqueCode);
      combinationObject.put("PenaltyCode",
          defaultAccount == null ? "" : defaultAccount.getEfinUniqueCode());
      combinationObject.put("PenaltyCodeName",
          defaultAccount == null ? "" : defaultAccount.getEfinUniquecodename());
      combinationObject.put("Type", strPenaltyAcctType);
      combinationObject.put("HasError", Boolean.FALSE);
    } catch (Exception e) {

      try {
        combinationObject = new JSONObject();
        combinationObject.put("HasError", Boolean.TRUE);
        combinationObject.put("Error", e.getMessage());

      } catch (JSONException e1) {
        // do nothing
      }

      log4j.error("Exception while getDefaultUniqueCode: " + e);
    }

    return combinationObject;
  }

  @Override
  public Boolean isTotalDeductionGreaterThanMatchAmount(String strRDVTrxLineID,
      String strPenaltyType) {

    Boolean isNetMatchNegative = Boolean.FALSE;
    try {

    } catch (Exception e) {
      log4j.error("Exception while isTotalDeductionGreaterThanMatchAmount(): " + e);
    }
    return isNetMatchNegative;
  }

  @Override
  public JSONObject getSelectedRecordsInformation(JSONArray strSelectedRecordsId) {
    JSONObject rdvtxnlnObject = new JSONObject();
    String txnType = null;
    String txnVersionStatus = null;
    BigDecimal netMatchAmt = BigDecimal.ZERO;
    BigDecimal matchAmt = BigDecimal.ZERO;
    BigDecimal holdAmt = BigDecimal.ZERO;
    BigDecimal advAmt = BigDecimal.ZERO;
    try {
      for (int i = 0; i < strSelectedRecordsId.length(); i++) {
        EfinRDVTxnline rdvTxnLine = OBDal.getInstance().get(EfinRDVTxnline.class,
            strSelectedRecordsId.get(i).toString());
        if (i == 0) {
          txnType = rdvTxnLine.getEfinRdvtxn().getEfinRdv().getTXNType();
          txnVersionStatus = rdvTxnLine.getEfinRdvtxn().getTxnverStatus();
        }
        netMatchAmt = netMatchAmt.add(rdvTxnLine.getNetmatchAmt());
        matchAmt = matchAmt.add(rdvTxnLine.getMatchAmt());
        holdAmt = holdAmt.add(rdvTxnLine.getHoldamt());
        advAmt = advAmt.add(rdvTxnLine.getADVDeduct());

      }

      rdvtxnlnObject.put("matchAmt", matchAmt);
      rdvtxnlnObject.put("netMatchAmt", netMatchAmt);
      rdvtxnlnObject.put("totalHoldAmt", holdAmt);
      rdvtxnlnObject.put("totalAdvAmt", advAmt);
      rdvtxnlnObject.put("txnType", txnType);
      rdvtxnlnObject.put("txnVersionStatus", txnVersionStatus);

      return rdvtxnlnObject;

    } catch (Exception e) {
      log4j.error("Excdeption while getSelectedRecordsInformation(): " + e);
      try {
        rdvtxnlnObject.put("result", "false");
        rdvtxnlnObject.put("message", OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
      } catch (JSONException e1) {
      }
    }
    return rdvtxnlnObject;
  }
}
