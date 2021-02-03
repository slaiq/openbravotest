package sa.elm.ob.scm.webservice.dao;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinPenalty;
import sa.elm.ob.finance.EfinPenaltyAction;
import sa.elm.ob.finance.EfinPenaltyHeader;
import sa.elm.ob.finance.EfinPenaltyTypes;
import sa.elm.ob.finance.EfinRDV;
import sa.elm.ob.finance.EfinRDVTransaction;
import sa.elm.ob.finance.EfinRDVTxnline;
import sa.elm.ob.scm.webservice.dto.PoReceiptHeaderDTO;
import sa.elm.ob.scm.webservice.dto.PoReceiptLineDTO;
import sa.elm.ob.scm.webservice.dto.RDVPenaltyDTO;
import sa.elm.ob.utility.util.Utility;

/**
 * This class is used for Penalty Process
 * 
 * @author DivyaPrakash JS
 */

public class PenaltyProcessDAO {
  private static final Logger log4j = LoggerFactory.getLogger(PenaltyProcessDAO.class);
  public static final String accountNumber = "1431";

  /**
   * This method is used to perform penalty process
   * 
   * @param orderDTO
   * @param rdv
   * @throws Exception
   */
  public static void penaltyProcess(PoReceiptHeaderDTO orderDTO, EfinRDV rdv,
      EfinRDVTransaction rdvTxn) throws Exception {
    Boolean isPenaltyTypeValid = false;
    try {
      OBContext.setAdminMode();
      Order objOrder = OBDal.getInstance().get(Order.class, orderDTO.getOrderId());
      HashSet<String> penaltyType = getPenaltyType(objOrder);
      isPenaltyTypeValid = isPenaltyTypeValid(orderDTO, penaltyType);
      if (isPenaltyTypeValid) {
        createPenaltyAction(orderDTO, objOrder, rdv, rdvTxn);
      }
    } catch (Exception e) {
      log4j.error("Exception in penaltyProcess" + e);
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to return penaltyType
   * 
   * @param objOrder
   * @return lookupLineSet
   * @throws Exception
   */
  public static HashSet<String> getPenaltyType(Order objOrder) throws Exception {
    HashSet<String> lookupLineSet = new HashSet<String>();
    try {
      OBContext.setAdminMode();
      String sqlqry = "select deductiontype from efin_penalty_types  where deductiontype is not null and ad_client_id=?";
      Query qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      qry.setParameter(0, objOrder.getClient().getId());
      log4j.debug("qry:" + qry);
      @SuppressWarnings({ "rawtypes" })
      List getPenaltytyType = qry.list();
      if (getPenaltytyType != null && getPenaltytyType.size() > 0) {
        for (@SuppressWarnings("rawtypes")
        Iterator iterator = getPenaltytyType.iterator(); iterator.hasNext();) {
          String row = (String) iterator.next();
          if (StringUtils.isNotBlank(row)) {
            lookupLineSet.add(row);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getPenaltyType" + e.getMessage());
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return lookupLineSet;
  }

  /**
   * This method is used to check whether given penalty type and amount is valid
   * 
   * @param order
   * @return Boolean
   * @throws Exception
   */
  public static Boolean isPenaltyTypeValid(PoReceiptHeaderDTO order, HashSet<String> penaltyType)
      throws Exception {
    try {
      OBContext.setAdminMode();
      if (order.getLineDTO().size() > 0) {
        for (PoReceiptLineDTO lineDTO : order.getLineDTO()) {
          OrderLine line = OBDal.getInstance().get(OrderLine.class, lineDTO.getPoLineId());
          if (!line.isEscmIssummarylevel()) {
            if (lineDTO.getPenaltyDTO() != null && lineDTO.getPenaltyDTO().size() > 0) {
              for (RDVPenaltyDTO penaltyDTO : lineDTO.getPenaltyDTO()) {
                // to check penalty id is valid
                if ((penaltyDTO.getPenaltyId() != null
                    && !penaltyType.contains(penaltyDTO.getPenaltyId()))) {
                  String message = OBMessageUtils.messageBD("ESCM_PenaltyInvalid");
                  message = message.replace("%", penaltyDTO.getPenaltyId());
                  throw new Exception(message);

                } else if (penaltyDTO.getPenaltyId() != null
                    && penaltyType.contains(penaltyDTO.getPenaltyId())) {
                  // to check penalty amount is not less than or equal to zero
                  if (penaltyDTO.getPenaltyAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    String message = OBMessageUtils.messageBD("ESCM_PenaltyAmtInvalid");
                    message = message.replace("%", penaltyDTO.getPenaltyId());
                    throw new Exception(message);
                  } else {
                    // for deduction type "ECA" or "IGI" Business partner is mandatory
                    OBQuery<EfinPenaltyTypes> type = OBDal.getInstance().createQuery(
                        EfinPenaltyTypes.class,
                        " as e join e.deductiontype as d where d.code =:penaltyType");
                    type.setNamedParameter("penaltyType", penaltyDTO.getPenaltyId());
                    type.setFilterOnReadableClients(false);
                    type.setFilterOnReadableOrganization(false);
                    type.setMaxResult(1);
                    List<EfinPenaltyTypes> penaltyTypeList = type.list();
                    if (penaltyTypeList.get(0).getDeductiontype().getPenaltyLogic() != null) {
                      if (penaltyTypeList.get(0).getDeductiontype().getPenaltyLogic().equals("ECA")
                          || penaltyTypeList.get(0).getDeductiontype().getPenaltyLogic()
                              .equals("IGI")) {
                        if (!StringUtils.isNotEmpty(penaltyDTO.getBpartnerId())) {
                          String message = OBMessageUtils.messageBD("ESCM_BP_Mandatory");
                          message = message.replace("%", penaltyDTO.getPenaltyId());
                          throw new Exception(message);
                        }
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
      log4j.error("Exception in isPenaltyTypeValid" + e);
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  /**
   * This method is used to createdpenaltyAction
   * 
   * @param orderDTO
   * @param objOrder
   * @param rdv
   * @param rdvTxn
   * @throws Exception
   */
  public static void createPenaltyAction(PoReceiptHeaderDTO orderDTO, Order objOrder, EfinRDV rdv,
      EfinRDVTransaction rdvTxn) throws Exception {
    try {
      OBContext.setAdminMode();
      long n = 10;
      for (PoReceiptLineDTO lineDTO : orderDTO.getLineDTO()) {
        if (lineDTO.getPenaltyDTO() != null && lineDTO.getPenaltyDTO().size() > 0) {
          OrderLine line = OBDal.getInstance().get(OrderLine.class, lineDTO.getPoLineId());
          if (!line.isEscmIssummarylevel()) {
            n = 10;
            // to get RDV txn line obj
            OBQuery<EfinRDVTxnline> txnLine = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
                "as e where e.salesOrderLine.id=:orderId and e.efinRdvtxn.id=:rdvTxnID");
            txnLine.setNamedParameter("orderId", lineDTO.getPoLineId());
            txnLine.setNamedParameter("rdvTxnID", rdvTxn.getId());
            txnLine.setFilterOnActive(true);
            txnLine.setFilterOnReadableClients(false);
            txnLine.setFilterOnReadableOrganization(false);
            List<EfinRDVTxnline> txnLineList = txnLine.list();

            // to get rdvTrxLIne
            EfinRDVTxnline rdvTxnline = OBDal.getInstance().get(EfinRDVTxnline.class,
                txnLineList.get(0).getId());
            // to check penaltyamout is greater than penalty amount
            BigDecimal penaltyAmoutValid = lineDTO.getPenaltyDTO().stream()
                .map(a -> a.getPenaltyAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

            if (penaltyAmoutValid.compareTo(rdvTxnline.getMatchAmt()) > 0) {
              String message = OBMessageUtils.messageBD("ESCM_InvalidPenaltyAmt");
              message = message.replace("%", lineDTO.getPoLineId());
              throw new Exception(message);
            }
            if (lineDTO.getPenaltyDTO().size() > 0) {
              for (RDVPenaltyDTO penaltyDTO : lineDTO.getPenaltyDTO()) {

                // to get orderLineObj
                OrderLine orderLineObj = OBDal.getInstance().get(OrderLine.class,
                    lineDTO.getPoLineId());

                OBQuery<EfinPenaltyTypes> type = OBDal.getInstance().createQuery(
                    EfinPenaltyTypes.class,
                    " as e join e.deductiontype as d where d.code =:penaltyType");
                type.setNamedParameter("penaltyType", penaltyDTO.getPenaltyId());
                type.setFilterOnReadableClients(false);
                type.setFilterOnReadableOrganization(false);
                type.setMaxResult(1);
                List<EfinPenaltyTypes> penaltyTypeList = type.list();

                // to get penaltyType

                EfinPenaltyTypes penaltytype = OBDal.getInstance().get(EfinPenaltyTypes.class,
                    penaltyTypeList.get(0).getId());

                // insert penalty Action
                EfinPenaltyAction action = OBProvider.getInstance().get(EfinPenaltyAction.class);
                action.setClient(objOrder.getClient());
                action.setOrganization(objOrder.getOrganization());
                action.setSequenceNumber(n);
                action.setTRXAppNo(rdvTxnline.getTrxappNo());
                action.setAction("AD");
                if (penaltyDTO.getActionDate() != null && penaltyDTO.getActionDate() != "") {
                  action.setActionDate(convertGregorian(penaltyDTO.getActionDate()));
                }
                action.setAmount(rdvTxnline.getMatchAmt());
                action.setEfinPenaltyTypes(penaltytype);
                action.setPenaltyPercentage(null);
                action.setPenaltyAmount(penaltyDTO.getPenaltyAmount());
                action.setActionReason(penaltyDTO.getActionReason());
                action.setActionJustification(penaltyDTO.getActionJustification());
                if (penaltyDTO.getBpartnerId() != null && penaltyDTO.getBpartnerId() != "")
                  action.setBusinessPartner(
                      OBDal.getInstance().get(BusinessPartner.class, penaltyDTO.getBpartnerId()));
                action.setName(objOrder.getBusinessPartner().getName());
                action.setEfinRdvtxnline(rdvTxnline);
                action.setFreezePenalty(false);
                action.setInvoice(null);
                action.setAmarsarfAmount(null);

                if (StringUtils.isNotEmpty(penaltyDTO.getUniqueCode())) {
                  action.setPenaltyAccountType("E");
                  action.setPenaltyUniquecode(orderLineObj.getEFINUniqueCode());
                } else {
                  List<AccountingCombination> accCombinationList = null;
                  if (StringUtils.isNotEmpty(accountNumber)) {
                    OBQuery<AccountingCombination> accCombination = OBDal.getInstance().createQuery(
                        AccountingCombination.class,
                        " as v join v.account as e where v.efinDimensiontype = 'A' and e.searchKey =:accountNumber");
                    accCombination.setNamedParameter("accountNumber", accountNumber);
                    accCombination.setFilterOnReadableClients(false);
                    accCombination.setFilterOnReadableOrganization(false);
                    accCombination.setMaxResult(1);
                    accCombinationList = accCombination.list();
                    if (accCombinationList.size() > 0) {
                      action.setPenaltyAccountType("A");
                      action.setPenaltyUniquecode(accCombinationList.get(0));
                    } else {
                      throw new Exception(OBMessageUtils.messageBD("ESCM_NoDefaultUniqPenalty")
                          .replace("%", accountNumber));
                    }
                  }
                }
                OBDal.getInstance().save(action);
                OBDal.getInstance().flush();
                insertPenaltyHeader(action, rdvTxnline, penaltyDTO.getPenaltyAmount());
                n++;
              }
            }
          }

        }
      }
    } catch (Exception e) {
      log4j.error("Exception in createPenaltyAction" + e);
      OBDal.getInstance().rollbackAndClose();
      throw new Exception(e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * This method is used to insert penalty header
   * 
   * @param penaltyaction
   * @param rdvTxnline
   * @param penaltyAmount
   */
  public static void insertPenaltyHeader(EfinPenaltyAction penaltyaction, EfinRDVTxnline rdvTxnline,
      BigDecimal penaltyAmount) {
    EfinRDVTransaction previousrdvtrx = null;
    EfinRDVTxnline previousrdxtrxln = null;
    BigDecimal prevpenhdAmt = BigDecimal.ZERO;
    JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode();

      // get json object of penalty amount on each penalty types based on rdv header and penalty
      // type
      result = getPenaltyAmt(penaltyaction.getEfinRdvtxnline(), null, penaltyaction);

      // get previous rdv transaction based on current rdv transaction line , created desc , not
      // inculding current version id , limit 1
      OBQuery<EfinRDVTransaction> rdvtrx = OBDal.getInstance().createQuery(EfinRDVTransaction.class,
          " as e where e.id <>'" + rdvTxnline.getEfinRdvtxn().getId() + "'" + "  and e.efinRdv.id='"
              + rdvTxnline.getEfinRdvtxn().getEfinRdv().getId() + "' " + "and e.tXNVersion < '"
              + rdvTxnline.getEfinRdvtxn().getTXNVersion() + "' order by created desc   ");
      rdvtrx.setFilterOnReadableClients(false);
      rdvtrx.setFilterOnReadableOrganization(false);
      rdvtrx.setMaxResult(1);
      if (rdvtrx.list().size() > 0) {
        previousrdvtrx = rdvtrx.list().get(0);
      }

      // get previous rdv transaction line based on previous trx version and current rdv line
      // product id
      if (previousrdvtrx != null
          && (rdvTxnline.getProduct() != null || rdvTxnline.getItemDesc() != null)) {
        OBQuery<EfinRDVTxnline> prerdvtrxln = OBDal.getInstance().createQuery(EfinRDVTxnline.class,
            " as e where e.efinRdvtxn.id='" + previousrdvtrx.getId() + "' and e.trxlnNo='"
                + rdvTxnline.getTrxlnNo() + "'");
        prerdvtrxln.setFilterOnReadableClients(false);
        prerdvtrxln.setFilterOnReadableOrganization(false);
        prerdvtrxln.setMaxResult(1);
        if (prerdvtrxln.list().size() > 0) {
          previousrdxtrxln = prerdvtrxln.list().get(0);
        }
      }
      // based on previous rdv transaction and rdv transaction line , get the penalty header prvious
      // penalty amount values
      if (previousrdvtrx != null && previousrdxtrxln != null) {
        OBQuery<EfinPenaltyHeader> prevpenhd = OBDal.getInstance().createQuery(
            EfinPenaltyHeader.class, " as e where e.efinRdvtxnline.id='" + previousrdxtrxln.getId()
                + "' and e.efinRdvtxn.id='" + previousrdvtrx.getId() + "' ");
        prevpenhd.setFilterOnReadableClients(false);
        prevpenhd.setFilterOnReadableOrganization(false);
        prevpenhd.setMaxResult(1);
        if (prevpenhd.list().size() > 0) {
          prevpenhdAmt = prevpenhd.list().get(0).getUpdatedPenaltyAmt();
        } else
          prevpenhdAmt = BigDecimal.ZERO;
      }

      // check if penalty header exists or not based on current rdv transaction line and rdv
      // transaction id
      OBQuery<EfinPenaltyHeader> penhdexisting = OBDal.getInstance().createQuery(
          EfinPenaltyHeader.class, " as e where e.efinRdvtxnline.id='" + rdvTxnline.getId()
              + "' and e.efinRdvtxn.id='" + rdvTxnline.getEfinRdvtxn().getId() + "'");
      penhdexisting.setFilterOnReadableClients(false);
      penhdexisting.setFilterOnReadableOrganization(false);
      penhdexisting.setMaxResult(1);
      // if exists penalty header update penalty amount and update penalty amount
      if (penhdexisting.list().size() > 0) {
        // if difference of old penalty amount and new penalty amunt not zero or action changed, or
        // penalty type changed
        if (penaltyAmount.compareTo(BigDecimal.ZERO) != 0) {

          EfinPenaltyHeader penltyhd = penhdexisting.list().get(0);
          // get penalty header amount
          log4j.debug("penaltyamt:" + penaltyAmount);
          penltyhd.setPenaltyAmount(penaltyAmount);
          penltyhd.setUpdatedPenaltyAmt(prevpenhdAmt.add(penaltyAmount));
          OBDal.getInstance().save(penltyhd);

          penaltyaction.setEfinPenaltyHeader(penltyhd);
          OBDal.getInstance().save(penaltyaction);

          updatePenalty(result, penaltyaction);
          OBDal.getInstance().flush();

        }
      } else {

        EfinPenaltyHeader penltyhd = OBProvider.getInstance().get(EfinPenaltyHeader.class);
        penltyhd.setClient(penaltyaction.getClient());
        penltyhd.setOrganization(penaltyaction.getOrganization());
        penltyhd.setLineNo(penaltyaction.getEfinRdvtxnline().getTrxlnNo());
        penltyhd.setEfinRdvtxn(rdvTxnline.getEfinRdvtxn());
        penltyhd.setEfinRdvtxnline(rdvTxnline);
        penltyhd.setExistingPenalty(BigDecimal.ZERO);
        penltyhd.setPenaltyAmount(penaltyAmount);
        penltyhd
            .setUpdatedPenaltyAmt(penltyhd.getExistingPenalty().add(penltyhd.getPenaltyAmount()));
        OBDal.getInstance().save(penltyhd);
        penaltyaction.setEfinPenaltyHeader(penltyhd);
        OBDal.getInstance().save(penaltyaction);
        if (result != null) {
          updatePenalty(result, penaltyaction);
        }
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      log4j.error("Exception in insertPenaltyHeader" + e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * This method is used to convert date to gregorian
   * 
   * @param penaltyaction
   * @param hijridate
   * @return greDate
   */
  public static Date convertGregorian(String hijridate) {
    log4j.debug("hi:" + hijridate);
    String gregDate = Utility.convertToGregorian(hijridate);
    log4j.debug("gregDate:" + gregDate);
    Date greDate = null;
    try {
      DateFormat df1 = new SimpleDateFormat("yyyy-MM-dd");
      greDate = df1.parse(gregDate);
      log4j.debug("greDate:" + greDate);
    } catch (Exception e) {
      log4j.error("Exception convertGregorian", e);

    }
    return greDate;

  }

  /**
   * This method is used to get Penalty Amount
   * 
   * @param trxline
   * @param actiontype
   * @return result
   */

  @SuppressWarnings("rawtypes")
  public static JSONObject getPenaltyAmt(EfinRDVTxnline trxline, String actiontype,
      EfinPenaltyAction penaltyaction) {
    JSONObject result = new JSONObject(), json = null;
    JSONArray array = new JSONArray();
    String sqlqry = null;
    Query qry = null;
    BigDecimal reduceAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      result.put("totalamount", "0");
      sqlqry = "    select  act.efin_penalty_types_id,  "
          + "               SUM(CASE WHEN act.action='AD'  THEN penalty_amount ELSE 0 END) + "
          + "               SUM(CASE WHEN act.action='RM'  THEN penalty_amount ELSE 0 END) as total "
          + "   from efin_penalty_action act   "
          + "             join efin_rdvtxnline ln on ln.efin_rdvtxnline_id= act.efin_rdvtxnline_id  "
          + "            join efin_rdvtxn trx on trx.efin_rdvtxn_id=ln.efin_rdvtxn_id  "
          + "            join efin_rdv rdv on rdv.efin_rdv_id= trx.efin_rdv_id    where  rdv.efin_rdv_id= ?  ";
      // if(actiontype!=null && actiontype.equals("del"))
      // sqlqry += " and act.efin_penalty_action_id<> ?";
      sqlqry += "              group by act.efin_penalty_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      log4j.debug("qry1" + qry);
      qry.setParameter(0, trxline.getEfinRdvtxn().getEfinRdv().getId());
      // if(actiontype!=null && actiontype.equals("del"))
      // qry.setParameter(1, penaltyaction.getId());
      List getPenaltytyAmt = qry.list();
      if (getPenaltytyAmt != null && getPenaltytyAmt.size() > 0) {
        for (Iterator iterator = getPenaltytyAmt.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          json = new JSONObject();
          if (row[0] != null) {
            json.put("penaltytype", row[0].toString());
            if (actiontype != null && actiontype.equals("del")) {
              if (penaltyaction.getEfinPenaltyTypes() != null
                  && penaltyaction.getEfinPenaltyTypes().getId().equals(row[0].toString())) {
                reduceAmt = new BigDecimal(row[1].toString())
                    .subtract(penaltyaction.getPenaltyAmount());
                json.put("amount", reduceAmt.toString());
              } else {
                json.put("amount", row[1].toString());
              }
            } else {
              json.put("amount", row[1].toString());
            }
            array.put(json);
          }
        }
        result.put("penaltylist", array);
        result.put("totalamount", new BigDecimal(result.getString("totalamount"))
            .add(new BigDecimal(json.getString("amount"))));
      }
      log4j.debug("result;" + result);
    } catch (Exception e) {
      log4j.error("Exception in getPenaltyAmt", e);
      OBDal.getInstance().rollbackAndClose();

    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /**
   * This method is used to update penalty
   * 
   * @param result
   * @param penaltyaction
   */
  public static void updatePenalty(JSONObject result, EfinPenaltyAction penaltyaction) {
    JSONObject json = null;
    try {
      OBContext.setAdminMode();
      if (result != null) {
        JSONArray array = result.getJSONArray("penaltylist");
        for (int i = 0; i < array.length(); i++) {
          json = array.getJSONObject(i);
          BigDecimal penApplied = new BigDecimal(json.getString("amount"));

          EfinPenaltyTypes penaltytype = OBDal.getInstance().get(EfinPenaltyTypes.class,
              json.getString("penaltytype"));
          OBQuery<EfinPenalty> uppenaltyQry = OBDal.getInstance().createQuery(EfinPenalty.class,
              " as e where e.efinRdv.id in ( select e.efinRdv.id from Efin_RDVTxn e where e.id='"
                  + penaltyaction.getEfinRdvtxnline().getEfinRdvtxn().getId() + "') "
                  + " and e.penaltyType='" + penaltytype.getDeductiontype().getCode() + "'");
          uppenaltyQry.setFilterOnReadableClients(false);
          uppenaltyQry.setFilterOnReadableOrganization(false);
          uppenaltyQry.setMaxResult(1);
          log4j.debug("penalty:" + uppenaltyQry.getWhereAndOrderBy());
          if (uppenaltyQry.list().size() > 0) {
            EfinPenalty upoldpenalty = uppenaltyQry.list().get(0);
            upoldpenalty.setPenaltyApplied(new BigDecimal(json.getString("amount")));

            if (!(upoldpenalty.getOpeningPenAmount().compareTo(BigDecimal.ZERO) == 0)) {
              upoldpenalty
                  .setPenaltyRemaining(upoldpenalty.getOpeningPenAmount().subtract(penApplied));
            }
            OBDal.getInstance().save(upoldpenalty);
            if (penApplied.compareTo(BigDecimal.ZERO) == 0) {
              deletePenalty(upoldpenalty.getId(), penaltytype, penaltyaction);
            }
          } else {
            insertPenalty(penaltyaction, penaltytype, penApplied);
          }
        }
      }

    } catch (final Exception e) {
      log4j.error("Exception in updatePenalty", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Method to delete penalty if the penalty type is not used in other lines
   * 
   * @param oldPenaltyId
   * @param penaltytype
   * @param penaltyaction
   */
  public static void deletePenalty(String oldPenaltyId, EfinPenaltyTypes penaltytype,
      EfinPenaltyAction penaltyaction) {
    Boolean canRemove = Boolean.TRUE;
    try {
      OBContext.setAdminMode();
      EfinRDV rdv = penaltyaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv();
      OBQuery<EfinPenaltyAction> penaltyAct = OBDal.getInstance().createQuery(
          EfinPenaltyAction.class,
          " as e where e.efinRdvtxnline.efinRdv.id=:rdvId and e.efinPenaltyTypes.id=:deductType "
              + "and e.efinRdvtxnline.id<>:currentlineId");
      penaltyAct.setNamedParameter("rdvId", rdv.getId());
      penaltyAct.setNamedParameter("deductType", penaltytype.getId());
      penaltyAct.setNamedParameter("currentlineId", penaltyaction.getEfinRdvtxnline().getId());
      penaltyAct.setFilterOnReadableClients(false);
      penaltyAct.setFilterOnReadableOrganization(false);

      if (penaltyAct.list().size() > 0) {
        canRemove = Boolean.FALSE;
      }
      if (canRemove) {
        EfinPenalty penalty = OBDal.getInstance().get(EfinPenalty.class, oldPenaltyId);
        OBDal.getInstance().remove(penalty);
        OBDal.getInstance().flush();
      }
    } catch (final Exception e) {
      log4j.error("Exception in deletePenalty", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Method to insert penalty if penalty does not exists based on RDV header
   * 
   * @param penaltyaction
   * @param penaltytype
   * @param penApplied
   */
  public static void insertPenalty(EfinPenaltyAction penaltyaction, EfinPenaltyTypes penaltytype,
      BigDecimal penApplied) {
    try {
      OBContext.setAdminMode();
      BigDecimal threshold = BigDecimal.ZERO;
      BigDecimal openPenAmt = BigDecimal.ZERO;
      BigDecimal percent = new BigDecimal("0.01");

      EfinRDV rdv = penaltyaction.getEfinRdvtxnline().getEfinRdvtxn().getEfinRdv();
      if (penaltytype.getThreshold() != null) {
        threshold = penaltytype.getThreshold().multiply(percent);
      }
      openPenAmt = threshold.multiply(rdv.getContractAmt());

      // insert penalty
      EfinPenalty penalty = OBProvider.getInstance().get(EfinPenalty.class);
      penalty.setClient(penaltyaction.getClient());
      penalty.setOrganization(penaltyaction.getOrganization());
      penalty.setEfinRdv(rdv);
      penalty.setPenaltyType(penaltytype.getDeductiontype());
      penalty.setAlertStatus(rdv.getPenaltyStatus());
      penalty.setPenaltyApplied(penApplied);
      if (!(openPenAmt.compareTo(BigDecimal.ZERO) == 0)) {
        penalty.setPenaltyRemaining(openPenAmt.subtract(penApplied));
      }
      penalty.setPenaltyPercentage(null);
      penalty.setOpeningPenAmount(openPenAmt);
      OBDal.getInstance().save(penalty);
      OBDal.getInstance().flush();

    } catch (final Exception e) {
      log4j.error("Exception in insertPenalty", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to get penalty amount header
   * 
   * @param trxline
   * @param calPenaltyheaderAmt
   */
  @SuppressWarnings("rawtypes")
  public static BigDecimal getPenaltyheaderAmt(EfinRDVTxnline trxline) {
    String sqlqry = null;
    Query qry = null;
    BigDecimal calPenaltyheaderAmt = BigDecimal.ZERO;
    try {
      OBContext.setAdminMode();
      sqlqry = "  select (coalesce(sum(act.penalty_amount),0)) as totpenamt , act.efin_rdvtxnline_id from efin_penalty_action act"
          + "     where act.efin_rdvtxnline_id=  ?  "
          + "      group by act.efin_rdvtxnline_id,act.efin_penalty_types_id ";
      qry = OBDal.getInstance().getSession().createSQLQuery(sqlqry);
      qry.setParameter(0, trxline.getId());
      log4j.debug("qry:" + qry);
      List getPenaltytyAmt = qry.list();
      if (getPenaltytyAmt != null && getPenaltytyAmt.size() > 0) {
        for (Iterator iterator = getPenaltytyAmt.iterator(); iterator.hasNext();) {
          Object[] row = (Object[]) iterator.next();
          calPenaltyheaderAmt = calPenaltyheaderAmt.add(new BigDecimal(row[0].toString()));
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in getPenaltyheaderAmt", e);

    } finally {
      OBContext.restorePreviousMode();
    }
    return calPenaltyheaderAmt;
  }

  /**
   * This method is used to perform penalty process
   * 
   * @param orderDTO
   * @param rdv
   * @throws Exception
   */
  public static void bulkPenaltyProcess(PoReceiptHeaderDTO orderDTO, EfinRDV rdv,
      EfinRDVTransaction rdvTxn) throws Exception {
    Boolean isBulkPenaltyTypeValid = false;
    try {
      OBContext.setAdminMode();
      Order objOrder = OBDal.getInstance().get(Order.class, orderDTO.getOrderId());
      HashSet<String> penaltyType = getPenaltyType(objOrder);
      isBulkPenaltyTypeValid = isBulkPenaltyTypeValid(orderDTO, penaltyType);
      if (isBulkPenaltyTypeValid) {
        createPenaltyActionforBulkPenalty(orderDTO, objOrder, rdv, rdvTxn);
      }
    } catch (Exception e) {
      log4j.error("Exception in penaltyProcess" + e);
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * This method is used to check whether given penalty type and amount is valid
   * 
   * @param order
   * @return Boolean
   * @throws Exception
   */
  public static Boolean isBulkPenaltyTypeValid(PoReceiptHeaderDTO order,
      HashSet<String> penaltyType) throws Exception {
    try {
      OBContext.setAdminMode();
      if (order.getBulkPenaltyDTO() != null && order.getBulkPenaltyDTO().size() > 0) {
        for (RDVPenaltyDTO bulkpenaltyDTO : order.getBulkPenaltyDTO()) {

          // to check penalty id is valid
          if ((bulkpenaltyDTO.getPenaltyId() != null
              && !penaltyType.contains(bulkpenaltyDTO.getPenaltyId()))) {
            String message = OBMessageUtils.messageBD("ESCM_PenaltyInvalid");
            message = message.replace("%", bulkpenaltyDTO.getPenaltyId());
            throw new Exception(message);

          } else if (bulkpenaltyDTO.getPenaltyId() != null
              && penaltyType.contains(bulkpenaltyDTO.getPenaltyId())) {
            // to check penalty amount is not less than or equal to zero
            if (bulkpenaltyDTO.getPenaltyAmount().compareTo(BigDecimal.ZERO) <= 0) {
              String message = OBMessageUtils.messageBD("ESCM_PenaltyAmtInvalid");
              message = message.replace("%", bulkpenaltyDTO.getPenaltyId());
              throw new Exception(message);
            } else {
              // for deduction type "ECA" or "IGI" Business partner is mandatory
              OBQuery<EfinPenaltyTypes> type = OBDal.getInstance().createQuery(
                  EfinPenaltyTypes.class,
                  " as e join e.deductiontype as d where d.code =:penaltyType");
              type.setNamedParameter("penaltyType", bulkpenaltyDTO.getPenaltyId());
              type.setFilterOnReadableClients(false);
              type.setFilterOnReadableOrganization(false);
              type.setMaxResult(1);
              List<EfinPenaltyTypes> penaltyTypeList = type.list();

              if (penaltyTypeList.get(0).getDeductiontype().getPenaltyLogic() != null) {
                if (penaltyTypeList.get(0).getDeductiontype().getPenaltyLogic().equals("ECA")
                    || penaltyTypeList.get(0).getDeductiontype().getPenaltyLogic().equals("IGI")) {
                  if (!StringUtils.isNotEmpty(bulkpenaltyDTO.getBpartnerId())) {
                    String message = OBMessageUtils.messageBD("ESCM_BP_Mandatory");
                    message = message.replace("%", bulkpenaltyDTO.getPenaltyId());
                    throw new Exception(message);
                  }
                }
              }

            }
          }

        }
      }

    } catch (Exception e) {
      log4j.error("Exception in isBulkPenaltyTypeValid" + e);
      throw new Exception(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return true;
  }

  /**
   * This method is used to createdpenaltyAction
   * 
   * @param orderDTO
   * @param objOrder
   * @param rdv
   * @param rdvTxn
   * @throws Exception
   */
  public static void createPenaltyActionforBulkPenalty(PoReceiptHeaderDTO orderDTO, Order objOrder,
      EfinRDV rdv, EfinRDVTransaction rdvTxn) throws Exception {
    try {
      OBContext.setAdminMode();
      long n = 10;
      if (orderDTO.getBulkPenaltyDTO() != null && orderDTO.getBulkPenaltyDTO().size() > 0) {
        for (RDVPenaltyDTO bulkPenaltyDTO : orderDTO.getBulkPenaltyDTO()) {
          if (rdvTxn.getEfinRDVTxnlineList().size() > 0) {
            for (EfinRDVTxnline rdvTxnline : rdvTxn.getEfinRDVTxnlineList()) {
              if (!rdvTxnline.isSummaryLevel()) {
                if (rdvTxnline.getNetmatchAmt().compareTo(bulkPenaltyDTO.getPenaltyAmount()) < 0) {
                  String message = OBMessageUtils.messageBD("ESCM_InvalidBulkPenaltyAmt");
                  message = message.replace("%", bulkPenaltyDTO.getPenaltyId());
                  throw new Exception(message);
                }

                // to get orderLineObj
                OrderLine orderLineObj = OBDal.getInstance().get(OrderLine.class,
                    rdvTxnline.getSalesOrderLine().getId());

                OBQuery<EfinPenaltyTypes> type = OBDal.getInstance().createQuery(
                    EfinPenaltyTypes.class,
                    " as e join e.deductiontype as d where d.code =:penaltyType");
                type.setNamedParameter("penaltyType", bulkPenaltyDTO.getPenaltyId());
                type.setFilterOnReadableClients(false);
                type.setFilterOnReadableOrganization(false);
                type.setMaxResult(1);
                List<EfinPenaltyTypes> penaltyTypeList = type.list();

                // to get penaltyType

                EfinPenaltyTypes penaltytype = OBDal.getInstance().get(EfinPenaltyTypes.class,
                    penaltyTypeList.get(0).getId());

                // insert penalty Action
                EfinPenaltyAction action = OBProvider.getInstance().get(EfinPenaltyAction.class);
                action.setClient(objOrder.getClient());
                action.setOrganization(objOrder.getOrganization());
                action.setSequenceNumber(n);
                action.setTRXAppNo(rdvTxnline.getTrxappNo());
                action.setAction("AD");
                if (bulkPenaltyDTO.getActionDate() != null
                    && bulkPenaltyDTO.getActionDate() != "") {
                  action.setActionDate(convertGregorian(bulkPenaltyDTO.getActionDate()));
                }
                action.setAmount(rdvTxnline.getMatchAmt());
                action.setEfinPenaltyTypes(penaltytype);
                action.setPenaltyPercentage(null);
                action.setPenaltyAmount(bulkPenaltyDTO.getPenaltyAmount());
                action.setActionReason(bulkPenaltyDTO.getActionReason());
                action.setActionJustification(bulkPenaltyDTO.getActionJustification());
                if (bulkPenaltyDTO.getBpartnerId() != null && bulkPenaltyDTO.getBpartnerId() != "")
                  action.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class,
                      bulkPenaltyDTO.getBpartnerId()));
                action.setName(objOrder.getBusinessPartner().getName());
                action.setEfinRdvtxnline(rdvTxnline);
                action.setFreezePenalty(false);
                action.setInvoice(null);
                action.setAmarsarfAmount(null);
                action.setPenaltyAccountType("E");
                action.setPenaltyUniquecode(orderLineObj.getEFINUniqueCode());
                OBDal.getInstance().save(action);
                OBDal.getInstance().flush();
                insertPenaltyHeader(action, rdvTxnline, bulkPenaltyDTO.getPenaltyAmount());

              }

            }

          }
          n++;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in createPenaltyAction" + e);
      OBDal.getInstance().rollbackAndClose();
      throw new Exception(e.getMessage());

    } finally {
      OBContext.restorePreviousMode();
    }

  }
}
