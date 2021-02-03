package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.EfinYearSequence;
import sa.elm.ob.scm.EscmOrderlineV;
import sa.elm.ob.scm.EscmProposalmgmtLine;

public class PurchaseReleaseDAO {

  private final static Logger log = LoggerFactory.getLogger(PurchaseReleaseDAO.class);

  public void getParentLines(OrderLine orderLine, ArrayList<String> parentList, Order releaseHeader,
      long line, JSONObject selectedRow, Order agreement) {
    try {
      OBQuery<OrderLine> agreementLineQry = OBDal.getInstance().createQuery(OrderLine.class,
          "as e where e.salesOrder.id = '" + agreement.getId() + "' and e.id ='" + orderLine.getId()
              + "'");

      if (orderLine.getEscmParentline() != null) {
        String ParentId = orderLine.getEscmParentline().getId();
        OrderLine parentOrdLine = OBDal.getInstance().get(OrderLine.class, ParentId);

        OBQuery<OrderLine> chkLineExists = OBDal.getInstance().createQuery(OrderLine.class,
            "as e where e.salesOrder.id = '" + releaseHeader.getId()
                + "' and e.escmAgreementLine.id ='" + ParentId + "'");

        chkLineExists.setMaxResult(1);

        if (chkLineExists.list().size() > 0) {
          insertParentLines(parentList, releaseHeader, line, selectedRow,
              chkLineExists.list().get(0), agreementLineQry.list().get(0), false);
        } else {
          parentList.add(ParentId);
          getParentLines(parentOrdLine, parentList, releaseHeader, line, selectedRow, agreement);
        }

      } else {
        insertParentLines(parentList, releaseHeader, line, selectedRow, null,
            agreementLineQry.list().get(0), false);
      }
    } catch (Exception e) {
      log.error("Exception in getParentLines: ", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }

  /**
   * If document sequence is not present, returns 0. Else returns sequence number
   * 
   * @param AccountDate
   * @param sequenceName
   * @param CalendarId
   * @param OrgId
   * @return document sequence for Sequence Name
   */
  public String checkDocumentSequence(String AccountDate, String sequenceName, String CalendarId,
      String OrgId) {
    String yearquery = "", ParentQury = "";
    String[] orgIds = null;
    String sequence = "0";
    String sequenceId = "";
    String yearId = "";
    String gsQuery = "";
    try {
      OBContext.setAdminMode();
      yearquery = "     select yr.c_year_id from c_period pr"
          + " join c_year yr on pr.c_year_id=yr.c_year_id" + " where to_date('" + AccountDate
          + "','dd-MM-yyyy') between cast(pr.startdate as date) and cast(pr.enddate as date)"
          + " and c_calendar_id='" + CalendarId + "'";

      // get document sequence
      Query resultset = OBDal.getInstance().getSession().createSQLQuery(yearquery);
      Object yearID = resultset.list().get(0);
      yearId = (String) yearID;
      // get GeneralSequence number from year
      gsQuery = "select yrseq.efin_year_sequence_id from ad_sequence seq "
          + "join efin_year_sequence yrseq on seq.ad_sequence_id=yrseq.ad_sequence_id "
          + " where yrseq.c_year_id= :year_id and seq.ad_org_id= :org_id and lower(seq.name)=lower('documentno_"
          + sequenceName + "') and seq.isactive = 'Y'";
      Query genSequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
      genSequencelist.setParameter("year_id", yearId);
      genSequencelist.setParameter("org_id", OrgId);
      if (genSequencelist.list().size() == 0) {
        ParentQury = " select eut_parent_org('" + OrgId + "','"
            + OBContext.getOBContext().getCurrentClient().getId() + "')";
        Query parentresult = OBDal.getInstance().getSession().createSQLQuery(ParentQury);
        Object parentOrg = parentresult.list().get(0);
        orgIds = ((String) parentOrg).split(",");
        for (int i = 0; i < orgIds.length; i++) {
          Query Sequencelist = OBDal.getInstance().getSession().createSQLQuery(gsQuery);
          Sequencelist.setParameter("year_id", yearId);
          Sequencelist.setParameter("org_id", orgIds[i].replace("'", ""));
          if (Sequencelist.list().size() > 0) {
            Object sequenceID = Sequencelist.list().get(0);
            sequenceId = (String) sequenceID;
            break;
          }
        }
      } else {
        Object sequenceID = genSequencelist.list().get(0);
        sequenceId = (String) sequenceID;
      }
      EfinYearSequence yearSequence = OBDal.getInstance().get(EfinYearSequence.class, sequenceId);
      if (yearSequence != null) {
        sequence = yearSequence.getNextAssignedNumber() == null ? ""
            : yearSequence.getNextAssignedNumber().toString();

      } else {
        return "0";
      }

    } catch (Exception e) {
      // TODO: handle exception
      return "0";
    } finally {
      OBContext.restorePreviousMode();
    }
    return sequence;

  }

  /**
   * Returns Release inprogress quantity
   * 
   * @param agreementLineId
   * 
   * @return inProgressQty
   */
  public BigDecimal getInProgressReleaseQty(String agreementLineId, String releaseLineId) {
    try {
      BigDecimal inProgressQty = BigDecimal.ZERO;
      BigDecimal oldOrderQty = BigDecimal.ZERO;
      BigDecimal currentRlseOrderQty = BigDecimal.ZERO;
      String hqlQry = "escmAgreementLine.id = :agreementLineId and salesOrder.escmAppstatus = :status  ";
      if (releaseLineId != null) {
        hqlQry += "and id != :releaseLineId";
      }
      OBQuery<OrderLine> agrmtLineQry = OBDal.getInstance().createQuery(OrderLine.class, hqlQry);
      agrmtLineQry.setNamedParameter("agreementLineId", agreementLineId);
      agrmtLineQry.setNamedParameter("status", "ESCM_IP");
      if (releaseLineId != null) {
        agrmtLineQry.setNamedParameter("releaseLineId", releaseLineId);
      }
      List<OrderLine> agrmtLineList = agrmtLineQry.list();

      if (agrmtLineList.size() > 0) {
        for (OrderLine agrmtLine : agrmtLineList) {
          currentRlseOrderQty = BigDecimal.ZERO;
          if (agrmtLine.getEscmOldOrderline() != null) {
            if (agrmtLine.getSalesOrder().getEscmAppstatus().equals("ESCM_AP")) {
              oldOrderQty = agrmtLine.getEscmOldOrderline().getOrderedQuantity();
              if (agrmtLine.getOrderedQuantity().compareTo(oldOrderQty) > 0) {
                currentRlseOrderQty = agrmtLine.getOrderedQuantity().subtract(oldOrderQty);
              }
            }

          } else {
            currentRlseOrderQty = agrmtLine.getOrderedQuantity();
          }
          inProgressQty = inProgressQty.add(currentRlseOrderQty);
        }
      }
      return inProgressQty;
    } catch (Exception e) {
      log.error("Exception in getInProgressReleaseQty: ", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  /**
   * Returns Release inprogress amount
   * 
   * @param agreementLineId
   * 
   * @return inProgressAmt
   */
  public BigDecimal getInProgressReleaseAmt(String agreementLineId, String releaseLineId,
      boolean agreementHasTax) {
    try {
      BigDecimal inProgressAmt = BigDecimal.ZERO;
      String hqlQry = "escmAgreementLine.id = :agreementLineId and salesOrder.escmAppstatus = :status ";
      if (releaseLineId != null) {
        hqlQry += "and id != :releaseLineId";
      }
      OBQuery<OrderLine> agrmtLineQry = OBDal.getInstance().createQuery(OrderLine.class, hqlQry);
      agrmtLineQry.setNamedParameter("agreementLineId", agreementLineId);
      agrmtLineQry.setNamedParameter("status", "ESCM_IP");
      if (releaseLineId != null) {
        agrmtLineQry.setNamedParameter("releaseLineId", releaseLineId);
      }
      List<OrderLine> agrmtLineList = agrmtLineQry.list();
      if (agrmtLineList.size() > 0) {
        for (OrderLine agrmtLine : agrmtLineList) {
          if (!agreementHasTax && agrmtLine.getSalesOrder().isEscmIstax()
              && agrmtLine.getSalesOrder().getEscmTaxMethod() != null) {
            if (!agrmtLine.getSalesOrder().getEscmTaxMethod().isPriceIncludesTax()) {
              inProgressAmt = inProgressAmt
                  .add(agrmtLine.getLineNetAmount().subtract(agrmtLine.getEscmLineTaxamt()));
            } else {
              inProgressAmt = inProgressAmt.add(agrmtLine.getLineNetAmount());
            }
          } else {
            inProgressAmt = inProgressAmt.add(agrmtLine.getLineNetAmount());
          }
        }
      }
      return inProgressAmt;
    } catch (Exception e) {
      log.error("Exception in getInProgressReleaseAmt: ", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
  }

  /**
   * Method to check whether the total release amount is greater than agreement remaining amount
   * 
   * @param release
   * @return
   */
  public Boolean checkAgreementRemainingAmt(Order release) {
    Boolean releaseAmtGreater = Boolean.FALSE;
    List<OrderLine> releaseLnList = null;
    BigDecimal releaseAmt = BigDecimal.ZERO;
    try {
      for (OrderLine relObjLine : release.getOrderLineList()) {
        releaseAmt = BigDecimal.ZERO;

        // getting both approved and in progress releaseLines of the selected agreementLine
        OBQuery<OrderLine> relLineQuery = OBDal.getInstance().createQuery(OrderLine.class,
            "as e where e.escmAgreementLine.id =:agrmtlineId and"
                + " e.salesOrder in (select o from Order o where o.documentNo<>:currentdoc"
                + " and (o.escmAppstatus='ESCM_AP' or o.escmAppstatus='ESCM_IP') "
                + "and o.escmRevision = (select max(a.escmRevision) from Order a where a.documentNo = o.documentNo)) ");

        relLineQuery.setNamedParameter("agrmtlineId", relObjLine.getEscmAgreementLine().getId());
        relLineQuery.setNamedParameter("currentdoc", release.getDocumentNo());
        releaseLnList = relLineQuery.list();

        if (releaseLnList.size() > 0) {
          for (OrderLine prevRelease : releaseLnList) {
            releaseAmt = releaseAmt.add(prevRelease.getLineNetAmount());
          }
        }
        // adding current releaseLineAmount
        releaseAmt = releaseAmt.add(relObjLine.getLineNetAmount());
        if (releaseAmt.compareTo(relObjLine.getEscmAgreementLine().getLineNetAmount()) > 0) {
          releaseAmtGreater = Boolean.TRUE;
          break;
        }
      }
    } catch (Exception e) {
      log.error("Exception in checkAgreementRemainingAmt: ", e);
    }
    return releaseAmtGreater;

  }

  /**
   * Method to check whether the release lineNetAmount is greater than the Encumbrance Amount
   * [PORelease created from Proposal]
   * 
   * @param release
   * @return
   */
  public Boolean checkAmtGrtThnEncAmount(Order release) {
    Boolean releaseAmtGreater = Boolean.FALSE;
    BigDecimal releaseAmt = BigDecimal.ZERO;
    BigDecimal encumbranceAmt = BigDecimal.ZERO;
    try {
      for (OrderLine relObjLine : release.getOrderLineList()) {
        EscmProposalmgmtLine proposalLine = relObjLine.getEscmProposalmgmtLine();
        encumbranceAmt = proposalLine.getEfinBudgmanencumline().getAPPAmt();
        releaseAmt = relObjLine.getLineNetAmount();

        if (releaseAmt.compareTo(encumbranceAmt) > 0) {
          releaseAmtGreater = Boolean.TRUE;
        }
      }
    } catch (Exception e) {
      log.error("Exception in checkAmtGrtThnEncAmount: ", e);
    }
    return releaseAmtGreater;
  }

  public static void insertParentLines(ArrayList<String> parentList, Order objorder, long line,
      JSONObject selectedRow, OrderLine orderline, OrderLine agreementLine, boolean updateqtyflag) {

    try {
      Long lineNo = line;
      OrderLine originalLine = orderline;
      OrderLine oldsalesOrderLine = null;
      OrderLine salesOrderLine = null;
      EscmOrderlineV parentLine = null;

      BigDecimal UnitPrice = BigDecimal.ZERO;
      BigDecimal grossPrice = BigDecimal.ZERO;
      BigDecimal taxpercent = BigDecimal.ZERO;
      BigDecimal PERCENT = new BigDecimal("0.01");
      BigDecimal taxAmount = BigDecimal.ZERO;
      BigDecimal lineNet = BigDecimal.ZERO;
      BigDecimal selectedLineReleasedAmt = BigDecimal.ZERO;
      BigDecimal weightage = BigDecimal.ZERO;

      final UIDefinitionController.FormatDefinition formatDef = UIDefinitionController.getInstance()
          .getFormatDefinition("euro", "Relation");
      DecimalFormat decimal = new DecimalFormat(formatDef.getFormat());
      Integer roundoffConst = decimal.getMaximumFractionDigits();

      for (int i = parentList.size() - 1; i >= 0; i--) {
        OrderLine parentOrdLine = OBDal.getInstance().get(OrderLine.class, parentList.get(i));
        salesOrderLine = (OrderLine) DalUtil.copy(parentOrdLine, false);
        salesOrderLine.setCreationDate(new java.util.Date());
        salesOrderLine.setCreatedBy(objorder.getCreatedBy());
        salesOrderLine.setUpdated(new java.util.Date());
        salesOrderLine.setUpdatedBy(objorder.getUpdatedBy());
        salesOrderLine.setSalesOrder(objorder);
        salesOrderLine.setLineNo(lineNo);
        salesOrderLine.setEscmAgreementLine(parentOrdLine);
        salesOrderLine.setEscmPoChangeType(null);
        salesOrderLine.setEscmPoChangeFactor(null);
        salesOrderLine.setEscmPoChangeValue(BigDecimal.ZERO);

        if (oldsalesOrderLine == null && orderline == null) {
          salesOrderLine.setEscmParentline(null);
        } else {
          if (originalLine == null) {
            parentLine = OBDal.getInstance().get(EscmOrderlineV.class, oldsalesOrderLine.getId());
            salesOrderLine.setEscmParentline(parentLine);
          } else {
            parentLine = OBDal.getInstance().get(EscmOrderlineV.class, originalLine.getId());
            salesOrderLine.setEscmParentline(parentLine);
            originalLine = null;
          }
        }
        if (objorder.getEscmReceivetype().equals("QTY")) {

          if (selectedRow.getString("id").equals(parentOrdLine.getId())) {

            // QTY Based Tax Calculation
            if (parentOrdLine.getSalesOrder().isEscmIstax()
                && parentOrdLine.getSalesOrder().getEscmTaxMethod() != null) {
              taxpercent = new BigDecimal(
                  parentOrdLine.getSalesOrder().getEscmTaxMethod().getTaxpercent());

              UnitPrice = parentOrdLine.getLineNetAmount()
                  .subtract(parentOrdLine.getEscmLineTaxamt()).divide(
                      parentOrdLine.getOrderedQuantity(), roundoffConst, BigDecimal.ROUND_HALF_UP);
              grossPrice = new BigDecimal(selectedRow.getString("releaseQty")).multiply(UnitPrice)
                  .setScale(roundoffConst, BigDecimal.ROUND_HALF_UP);
              taxAmount = grossPrice.multiply(taxpercent.multiply(PERCENT));
              lineNet = grossPrice.add(taxAmount).setScale(roundoffConst, BigDecimal.ROUND_HALF_UP);
            } else {
              UnitPrice = parentOrdLine.getLineNetAmount()
                  .divide(parentOrdLine.getOrderedQuantity());
              grossPrice = new BigDecimal(selectedRow.getString("releaseQty")).multiply(UnitPrice)
                  .setScale(roundoffConst, BigDecimal.ROUND_HALF_UP);
              lineNet = grossPrice;
            }

            salesOrderLine.setOrderedQuantity(new BigDecimal(selectedRow.getString("releaseQty")));
            salesOrderLine.setUnitPrice(UnitPrice);
            salesOrderLine.setEscmLineTotalUpdated(grossPrice);
            salesOrderLine.setEscmLineTaxamt(taxAmount);
            salesOrderLine.setLineNetAmount(lineNet);
            salesOrderLine.setLineGrossAmount(lineNet);

          } else {
            salesOrderLine.setOrderedQuantity(parentOrdLine.getOrderedQuantity());
          }
        }

        else {
          if (selectedRow.getString("id").equals(parentOrdLine.getId())) {

            selectedLineReleasedAmt = new BigDecimal(selectedRow.getString("releaseamt"));
            // AMT Based Tax Calculation
            if (parentOrdLine.getSalesOrder().isEscmIstax()
                && parentOrdLine.getSalesOrder().getEscmTaxMethod() != null) {
              taxpercent = new BigDecimal(
                  parentOrdLine.getSalesOrder().getEscmTaxMethod().getTaxpercent());
              weightage = ((selectedLineReleasedAmt.divide(parentOrdLine.getLineNetAmount(), 15,
                  BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100)));
              taxAmount = (parentOrdLine.getEscmLineTaxamt().multiply(weightage.multiply(PERCENT)));
              grossPrice = selectedLineReleasedAmt.subtract(taxAmount);
              UnitPrice = grossPrice;
            } else {
              UnitPrice = selectedLineReleasedAmt;
              grossPrice = selectedLineReleasedAmt;
            }

            salesOrderLine.setOrderedQuantity(BigDecimal.ONE);
            salesOrderLine.setUnitPrice(UnitPrice);
            salesOrderLine.setEscmLineTotalUpdated(grossPrice);
            salesOrderLine.setLineNetAmount(selectedLineReleasedAmt);
            salesOrderLine.setLineGrossAmount(selectedLineReleasedAmt);
            salesOrderLine.setEscmLineTaxamt(taxAmount);
          } else {
            salesOrderLine.setOrderedQuantity(BigDecimal.ONE);
            salesOrderLine.setUnitPrice(BigDecimal.ZERO);
            salesOrderLine.setEscmLineTotalUpdated(BigDecimal.ZERO);
            salesOrderLine.setLineNetAmount(BigDecimal.ZERO);
            salesOrderLine.setLineGrossAmount(BigDecimal.ZERO);
          }
        }
        if (taxAmount.compareTo(BigDecimal.ZERO) != 0) {
          salesOrderLine.getSalesOrder().setEscmCalculateTaxlines(true);
        }
        salesOrderLine.setEscmIsmanual(false);
        OBDal.getInstance().save(salesOrderLine);
        OBDal.getInstance().flush();
        oldsalesOrderLine = salesOrderLine;
        lineNo = lineNo + 10;
      }

    } catch (Exception e) {
      log.error("Exception in insertParentLines: ", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }

  }

}
