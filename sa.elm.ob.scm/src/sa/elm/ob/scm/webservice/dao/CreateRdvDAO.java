package sa.elm.ob.scm.webservice.dao;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BankAccount;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.scm.webservice.exception.CreateReceiptException;
import sa.elm.ob.utility.util.UtilityDAO;

/**
 * This class is used to create RDV
 * 
 * @author Kiruthika.K
 */
public class CreateRdvDAO {
  private static final Logger log4j = Logger.getLogger(CreateRdvDAO.class);

  /*
   * Check whether RDV is already created for the PO. If RDV is not created, create new RDV header
   * 
   * @param ShipmentInOut
   * 
   * @return EfinRDV
   */
  public static EfinRDV createRDVForPOReceipt(ShipmentInOut receipt) throws Exception {

    EfinRDV rdv = null;
    String orderId = null;
    try {
      if (receipt != null) {
        orderId = receipt.getSalesOrder().getId();
        rdv = getRdvIdForPO(orderId);

        if (rdv == null) {
          rdv = createRDVHeader(orderId);
          return rdv;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in createRDVForPOReceipt() in CreateRdvDAO file : ", e);
      throw new Exception(e.getMessage());
    }
    return rdv;
  }

  /*
   * Check whether Txn Version is in Draft status. If Txn Version is Draft, throw error. If
   * completed, create new Txn Version.
   * 
   * @param ShipmentInOut
   * 
   * @param EfinRDV
   * 
   * @return EfinRDVTransaction
   */
  public static EfinRDVTransaction createRDVTxn(ShipmentInOut inout, EfinRDV rdv)
      throws CreateReceiptException, Exception {

    EfinRDVTransaction rdvTxn = null;
    try {
      if (rdv != null && inout != null) {

        // because currently we are allowing multiple draft versions.
        /*
         * if (checkTVisDraft(rdv)) { throw new
         * CreateReceiptException(OBMessageUtils.messageBD("ESCM_RDVTXN_Draft"), true); } else {
         * rdvTxn = createTxnVersion(inout, rdv); }
         */
        rdvTxn = createTxnVersion(inout, rdv);
      }
    } catch (CreateReceiptException e) {
      throw new CreateReceiptException(e.getMessage());
    } catch (Exception e) {
      throw new Exception(e.getMessage());
    }
    return rdvTxn;
  }

  /*
   * Get RDV id for the current PO
   * 
   * @param orderId
   * 
   * @return EfinRDV
   */
  public static EfinRDV getRdvIdForPO(String orderId) {

    EfinRDV rdv = null;
    try {
      if (orderId != null) {
        Order order = OBDal.getInstance().get(Order.class, orderId);
        if (order != null) {

          OBQuery<EfinRDV> rdvQry = OBDal.getInstance().createQuery(EfinRDV.class,
              " salesOrder.documentNo = '" + order.getDocumentNo() + "'");

          rdvQry.setFilterOnReadableClients(false);
          rdvQry.setFilterOnReadableOrganization(false);

          List<EfinRDV> rdvList = rdvQry.list();
          if (rdvList.size() > 0) {
            rdv = rdvList.get(0);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getRdvIdForPO() in CreateRdvDAO file : ", e);
    }
    return rdv;
  }

  /*
   * Creates RDV header
   * 
   * @param orderId
   * 
   * @return EfinRDV
   */
  public static EfinRDV createRDVHeader(String orderId) throws Exception {

    Order order = null;
    EfinRDV rdv = null;
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    EfinBudgetIntialization budgInitial = null;
    String hijiriDate = UtilityDAO.convertTohijriDate(dateFormat.format(date));
    try {
      if (orderId != null) {
        order = OBDal.getInstance().get(Order.class, orderId);
        rdv = OBProvider.getInstance().get(EfinRDV.class);
        rdv.setOrganization(order.getOrganization());
        rdv.setTXNType("PO");
        rdv.setTXNDate(date);
        rdv.setWebservice(true);
        rdv.setTxndategre(sdf.format(date));
        rdv.setSalesOrder(order);
        rdv.setBusinessPartner(order.getBusinessPartner());
        rdv.setManualEncumbrance(
            order.getEfinBudgetManencum() == null ? null : order.getEfinBudgetManencum());
        rdv.setEXPDate(order.getEscmContractenddate());
        rdv.setStatus(order.getEscmAppstatus());
        rdv.setType(order.getEscmOrdertype());
        rdv.setDate(order.getOrderDate());
        rdv.setDategreg((order.getOrderDate().toString()).substring(8, 10) + "-"
            + (order.getOrderDate().toString()).substring(5, 7) + "-"
            + (order.getOrderDate().toString()).substring(0, 4));
        rdv.setCurrency(order.getEscmCurrency());
        rdv.setContractDuration(order.getEscmContractduration());
        rdv.setPeriodType(order.getEscmPeriodtype());
        rdv.setContractAmt(order.getGrandTotalAmount());
        rdv.setADVDeductPercent(order.getEscmAdvpaymntPercntge());
        // getting budget initial id based on transaction date
        budgInitial = UtilityDAO.getBudgetInitial(hijiriDate, order.getClient().getId());
        if (budgInitial != null) {
          rdv.setBudgetInitialization(budgInitial);
        } else {
          rdv.setBudgetInitialization(null);

        }
        if (order.getEscmTaxMethod() != null && order.isEscmIstax()) {
          rdv.setEfinTaxMethod(order.getEscmTaxMethod());
        }

        if (order.getEscmIban() != null) {
          // Bank account may not be there in Business Partner.
          rdv.setPartnerBankAccount(order.getEscmIban());
          if (order.getEscmIban().getEfinBank() != null) {
            rdv.setEfinBank(order.getEscmIban().getEfinBank());
          }
          rdv.setIBAN(order.getEscmIban().getIBAN());
        } else {

          List<BankAccount> bankAccountList = order.getBusinessPartner()
              .getBusinessPartnerBankAccountList();
          if (bankAccountList.size() == 0) {
            throw new CreateReceiptException(
                OBMessageUtils.messageBD("Escm_configureBpbankaccount"), true);
          }
          BankAccount bpBank = bankAccountList.get(0);
          rdv.setPartnerBankAccount(bpBank);
          if (bpBank.getEfinBank() != null) {
            rdv.setEfinBank(bpBank.getEfinBank());
          }
          rdv.setIBAN(bpBank.getIBAN());
        }

        rdv.setAwardDate(date);
        rdv.setADVDeductMethod("PE");

        OBDal.getInstance().save(rdv);
        OBDal.getInstance().flush();

      }
    } catch (CreateReceiptException e) {
      log4j.error("Exception in createRDVHeader() in CreateRdvDAO file : ", e);
      throw new CreateReceiptException(e.getMessage());
    } catch (Exception e) {
      log4j.error("Exception in createRDVHeader() in CreateRdvDAO file : ", e);
      throw new Exception(e.getMessage());
    }
    return rdv;
  }

  /*
   * Checks whether Txn Version contains Draft record.
   * 
   * @param orderId
   * 
   * @return EfinRDV
   */
  public static boolean checkTVisDraft(EfinRDV rdv) {

    boolean isTVisDraft = false;

    try {
      OBQuery<EfinRDVTransaction> rdvTxnQry = OBDal.getInstance()
          .createQuery(EfinRDVTransaction.class, "efinRdv.id = '" + rdv.getId() + "'");
      rdvTxnQry.setFilterOnReadableClients(false);
      rdvTxnQry.setFilterOnReadableOrganization(false);

      List<EfinRDVTransaction> rdvTxnList = rdvTxnQry.list();
      if (rdvTxnList.size() > 0) {
        for (EfinRDVTransaction txnVersion : rdvTxnList) {
          if ("DR".equals(txnVersion.getTxnverStatus())) {
            isTVisDraft = true;
            return isTVisDraft;
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in checkTVisDraft() in CreateRdvDAO file : ", e);
    }
    return isTVisDraft;
  }

  /*
   * Creates Txn Version
   * 
   * @param orderId
   * 
   * @return EfinRDV
   */
  public static EfinRDVTransaction createTxnVersion(ShipmentInOut inout, EfinRDV rdv)
      throws Exception {

    EfinRDVTransaction rdvTxn = null;
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
    String sql = "";
    Query qry = null;
    BigDecimal versionNo = BigDecimal.ZERO;

    try {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      date = cal.getTime();

      if (inout != null && rdv != null) {
        rdvTxn = OBProvider.getInstance().get(EfinRDVTransaction.class);
        rdvTxn.setTxnverDate(date);
        rdvTxn.setTxnverDateGreg(sdf.format(date));
        rdvTxn.setEfinRdv(rdv);

        rdvTxn.setCertificateDate(date);
        rdvTxn.setAdvancetransaction(false);
        rdvTxn.setTxnverStatus("DR");
        rdvTxn.setWebservice(true);

        rdvTxn.setClient(rdv.getClient());
        rdvTxn.setOrganization(rdv.getOrganization());
        rdvTxn.setActive(true);
        rdvTxn.setUpdatedBy(rdv.getCreatedBy());
        rdvTxn.setCreationDate(date);
        rdvTxn.setCreatedBy(rdv.getCreatedBy());
        rdvTxn.setUpdated(date);

        // Get maximum transaction version number
        sql = "select coalesce(max(TXN_Version),0)+1 from Efin_RDVTxn where Efin_RDV_ID=:rdvId ";
        qry = OBDal.getInstance().getSession().createSQLQuery(sql);
        qry.setParameter("rdvId", rdv.getId());
        qry.setMaxResults(1);
        if (qry != null && qry.list().size() > 0) {
          @SuppressWarnings("unchecked")
          List<Object> object = qry.list();
          versionNo = (BigDecimal) object.get(0);
          rdvTxn.setTXNVersion(versionNo.longValue());
        }

        OBDal.getInstance().save(rdvTxn);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      log4j.error("Exception in createTxnVersion() in CreateRdvDAO file : ", e);
      throw new Exception(e.getMessage());
    }
    return rdvTxn;
  }

  /*
   * Check whether RDV for PO with PO Receipt is already created and Transaction Version has draft
   * records
   * 
   * @param orderId
   * 
   */
  public static void rdvInitialCheck(String orderId) throws CreateReceiptException, Exception {
    try {
      EfinRDV rdvForPO = getRdvIdForPO(orderId);
      if (rdvForPO != null) {
        if (rdvForPO.getGoodsShipment() != null) {
          throw new CreateReceiptException(OBMessageUtils.messageBD("Escm_POReceiptAlreadyCreated"),
              true);
        }
        // because currently we are allowing multiple draft versions.
        /*
         * if (checkTVisDraft(rdvForPO)) { throw new
         * CreateReceiptException(OBMessageUtils.messageBD("ESCM_RDVTXN_Draft"), true); }
         */
      }
    } catch (CreateReceiptException e) {
      throw new CreateReceiptException(e.getMessage());
    } catch (Exception e) {
      log4j.debug("Error while checking rdvInitialCheck" + e.getMessage());
      throw new Exception(e.getMessage());
    }
  }

  /*
   * Check whether PO is already approved and it is a latest version
   * 
   * @param orderId
   * 
   */
  public static void checkApprovedPOandVersion(String orderId)
      throws CreateReceiptException, Exception {

    String query = null;
    Long maxRevision = (long) 0;
    Order order = OBDal.getInstance().get(Order.class, orderId);

    try {
      if (order != null) {
        if (!"ESCM_AP".equals(order.getEscmAppstatus())) {
          throw new CreateReceiptException(OBMessageUtils.messageBD("Escm_POnotApproved"), true);
        }

        query = "select max(o.escmRevision) from Order o where o.documentNo = :documentNo and o.escmAppstatus = :appStatus";
        Query qry = OBDal.getInstance().getSession().createQuery(query);
        qry.setParameter("documentNo", order.getDocumentNo());
        qry.setParameter("appStatus", "ESCM_AP");
        qry.setMaxResults(1);

        if (qry != null) {
          @SuppressWarnings("rawtypes")
          List revList = qry.list();
          if (revList.size() > 0) {
            Object row = (Object) revList.get(0);
            maxRevision = (Long) row;
          }
        }

        if (maxRevision != order.getEscmRevision()) {
          throw new CreateReceiptException(OBMessageUtils.messageBD("Escm_PoLatestVersion"), true);
        }
      }
    } catch (CreateReceiptException e) {
      throw new CreateReceiptException(e.getMessage());
    } catch (Exception e) {
      log4j.error("Exception in checkApprovedPOandVersion() in CreateRdvDAO file : ", e);
      throw new Exception(e.getMessage());
    }
  }
}
