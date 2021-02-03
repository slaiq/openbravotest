package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.SQLQuery;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmOrderlineV;
import sa.elm.ob.scm.EscmProposalAttribute;
import sa.elm.ob.scm.EscmProposalMgmt;
import sa.elm.ob.scm.EscmProposalmgmtLine;
import sa.elm.ob.scm.Escmbankguaranteedetail;
import sa.elm.ob.scm.EscmproposalDistribution;
import sa.elm.ob.scm.event.dao.BankGuaranteeDetailEventDAO;

public class POcontractAddproposalDAO {
  @SuppressWarnings("unused")
  private Connection conn = null;

  public POcontractAddproposalDAO(Connection conn) {
    this.conn = conn;
  }

  private final static Logger log = LoggerFactory.getLogger(POcontractAddproposalDAO.class);

  // insert the Order lines based on selection
  public static int insertOrderline(Connection conn, EscmProposalMgmt proposal, Order objPo) {
    int count = 0;
    // long lineno = 10;

    List<Escmbankguaranteedetail> bgdetail = new ArrayList<Escmbankguaranteedetail>();
    List<EscmproposalDistribution> proposaldis = new ArrayList<EscmproposalDistribution>();
    EscmProposalAttribute proatt = null;
    List<EscmProposalmgmtLine> chilproline = null;
    String mainparentid = null, parentid = null;

    Date needByDate = new Date();
    Calendar calendar = Calendar.getInstance();

    if (calendar.get(Calendar.DAY_OF_WEEK) == 5) {
      calendar.add(Calendar.DATE, 3);
    } else {
      calendar.add(Calendar.DATE, 1);
    }

    needByDate = calendar.getTime();

    OBQuery<OrderLine> poln = null;
    List<OrderLine> polist = null;
    String poLineParentId = null;
    TaxRate tax = null;
    boolean inclusiveTax = false;
    try {
      OBContext.setAdminMode();

      OBQuery<TaxRate> objTaxQry = OBDal.getInstance().createQuery(TaxRate.class,
          "as e order by e.creationDate desc");
      objTaxQry.setMaxResult(1);
      List<TaxRate> objTaxList = objTaxQry.list();
      if (objTaxList.size() > 0) {
        tax = objTaxList.get(0);
      }
      OBQuery<EscmProposalmgmtLine> pmgmtln = OBDal.getInstance().createQuery(
          EscmProposalmgmtLine.class,
          " as e where e.escmProposalmgmt.id=:proposalID and e.summary='N' "
              + "and (e.status != 'CL' or e.status is null) order by e.lineNo desc");
      pmgmtln.setNamedParameter("proposalID", proposal.getId());
      if (pmgmtln.list().size() > 0) {
        chilproline = pmgmtln.list();

      }

      // is having inclusive tax
      if (proposal.getEfinTaxMethod() != null && proposal.getEfinTaxMethod().isPriceIncludesTax()) {
        inclusiveTax = true;
      }

      // Insert Tree
      List<String> parentlist = new ArrayList<String>();
      for (EscmProposalmgmtLine deliverables : chilproline) {
        // clearing previous tree
        parentlist.clear();
        mainparentid = deliverables.getParentLineNo() != null
            ? deliverables.getParentLineNo().getId()
            : null;
        parentid = mainparentid;

        poln = OBDal.getInstance().createQuery(OrderLine.class,
            " as e where e.escmProposalmgmtLine.id=:proposalLnID and e.salesOrder.id=:orderID");
        poln.setNamedParameter("proposalLnID", parentid);
        poln.setNamedParameter("orderID", objPo.getId());

        poln.setMaxResult(1);
        polist = poln.list();
        if (parentid != null && polist.size() == 0) {
          parentlist.add(parentid);
        } else {
          poLineParentId = polist.size() > 0 ? polist.get(0).getId() : null;
        }
        if (polist.size() == 0) {
          while (parentid != null) {
            EscmProposalmgmtLine parent = OBDal.getInstance().get(EscmProposalmgmtLine.class,
                parentid);
            parentid = parent.getParentLineNo() != null ? parent.getParentLineNo().getId() : null;
            poln = OBDal.getInstance().createQuery(OrderLine.class,
                " as e where e.escmProposalmgmtLine.id=:proposalLnID and e.salesOrder.id=:orderID");
            poln.setNamedParameter("proposalLnID", parentid);
            poln.setNamedParameter("orderID", objPo.getId());
            polist = poln.list();
            if (parentid != null && polist.size() == 0) {
              parentlist.add(parentid);
            }
          }
          log.info("parentlist:" + parentlist.toString());
          ListIterator<String> li = parentlist.listIterator(parentlist.size());
          // Iterate in reverse.
          while (li.hasPrevious()) {
            String poParentId = null;
            EscmProposalmgmtLine line = OBDal.getInstance().get(EscmProposalmgmtLine.class,
                li.previous());
            // get parentPoLineID
            if (line.getParentLineNo() != null) {
              OBQuery<OrderLine> poParentln = OBDal.getInstance().createQuery(OrderLine.class,
                  " as e where e.escmProposalmgmtLine.id=:proposalLnID");
              poParentln.setNamedParameter("proposalLnID", line.getParentLineNo().getId());
              List<OrderLine> poParentList = poParentln.list();
              if (poParentList.size() > 0) {
                poParentId = poParentList.get(0).getId();
              }
            }

            final SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
                "select coalesce(max(line),0)+10   as lineno from c_orderline where c_order_id=:orderId");

            query.setParameter("orderId", objPo.getId());
            // lineno = ((BigDecimal) (Object) query.list().get(0)).longValue();

            OrderLine poline = OBProvider.getInstance().get(OrderLine.class);
            poline.setBusinessPartner(proposal.getSupplier());
            poline.setClient(objPo.getClient());
            poline.setOrganization(objPo.getOrganization());
            poline.setCreationDate(new java.util.Date());
            poline.setCreatedBy(objPo.getCreatedBy());
            poline.setUpdated(new java.util.Date());
            poline.setUpdatedBy(objPo.getUpdatedBy());
            poline.setActive(true);
            poline.setSalesOrder(objPo);
            poline.setLineNo(line.getLineNo());
            poline.setOrderDate(objPo.getOrderDate());
            poline.setWarehouse(objPo.getWarehouse());
            Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
            poline.setCurrency(
                objPo.getCurrency() == null
                    ? (objPo.getOrganization().getCurrency() == null ? objCurrency
                        : objPo.getOrganization().getCurrency())
                    : objPo.getCurrency());
            // poline.setCurrency(objPo.getCurrency());
            if (tax != null) {
              poline.setTax(tax);
            }
            // poline.setTax(OBDal.getInstance().get(TaxRate.class,
            // "97E8BE0B96364939A4515D7FEFFA41BF"));

            if (line.getProduct() != null) {
              poline.setProduct(line.getProduct());
            }
            poline.setEscmProdescription(line.getDescription());
            poline.setUOM(line.getUOM());
            poline.setOrderedQuantity((line.getMovementQuantity()));
            poline.setUnitPrice(line.getLineTotal().divide(line.getMovementQuantity()));
            poline.setEscmPodiscount(line.getDiscount());
            poline.setEscmPodiscountamount(line.getDiscountmount());
            poline.setLineNetAmount(line.getLineTotal());
            poline.setEscmLineTotalUpdated(line.getLineTotal());

            /*
             * poline
             * .setLineNetAmount((line.getMovementQuantity()).multiply(line.getNegotUnitPrice()));
             */
            poline.setEscmProposalmgmt(proposal);
            poline.setEscmIsmanual(false);
            poline.setEscmIssummarylevel(true);
            poline.setEscmProposalmgmtLine(line);
            poline.setEscmProductCategory(line.getProductCategory());
            poline.setEscmNeedbydate(needByDate);
            if (poParentId != null) {
              poline.setEscmParentline(OBDal.getInstance().get(EscmOrderlineV.class, poParentId));
            }
            poline.getSalesOrder().setEscmProposalmgmt(proposal);
            poline.getSalesOrder().setEscmProposalno(proposal.getSupplierProposalNo());
            poline.getSalesOrder().setEscmProposaldate(proposal.getSupplierProposalDate());
            poline.getSalesOrder().setEscmAddproposal(true);
            if (proposal.getSecondsupplier() != null)
              poline.getSalesOrder().setEscmSecondsupplier(proposal.getSecondsupplier());
            if (proposal.getSecondBranchname() != null)
              poline.getSalesOrder().setEscmSecondBranchname(proposal.getSecondBranchname());
            poline.getSalesOrder().setEscmIssecondsupplier(proposal.isSecondsupplier());
            if (proposal.getIBAN() != null)
              poline.getSalesOrder().setEscmSecondIban(proposal.getIBAN());
            if (proposal.getSubcontractors() != null)
              poline.getSalesOrder().setEscmSubcontractors(proposal.getSubcontractors());
            if (proposal.getEscmBidmgmt() != null) {
              poline.getSalesOrder().setEscmBidmgmt(proposal.getEscmBidmgmt());
              poline.getSalesOrder().setEscmProjectname(proposal.getEscmBidmgmt().getBidname());
            } else {
              poline.getSalesOrder().setEscmProjectname(proposal.getBidName());
            }
            if (inclusiveTax) {
              if (proposal.isNeedEvaluation()) {
                poline.setEscmInitialUnitprice(line.getNegotUnitPrice().add(line.getUnittax()));
              } else {
                poline.setEscmInitialUnitprice(line.getGrossUnitPrice());
              }
            }
            OBDal.getInstance().save(poline);
            OBDal.getInstance().flush();
            objPo.getOrderLineList().add(poline);
            poLineParentId = poline.getId();

          }

        }

        // insert po line

        EscmProposalmgmtLine line = deliverables;

        final SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
            "select coalesce(max(line),0)+10   as lineno from c_orderline where c_order_id=:orderId");

        query.setParameter("orderId", objPo.getId());
        // lineno = ((BigDecimal) (Object) query.list().get(0)).longValue();

        OrderLine poline = OBProvider.getInstance().get(OrderLine.class);
        poline.setClient(objPo.getClient());
        poline.setOrganization(objPo.getOrganization());
        poline.setCreationDate(new java.util.Date());
        poline.setCreatedBy(objPo.getCreatedBy());
        poline.setUpdated(new java.util.Date());
        poline.setUpdatedBy(objPo.getUpdatedBy());
        poline.setActive(true);
        poline.setSalesOrder(objPo);
        poline.setLineNo(line.getLineNo());
        poline.setOrderDate(objPo.getOrderDate());
        poline.setWarehouse(objPo.getWarehouse());
        Currency objCurrency = OBDal.getInstance().get(Currency.class, "317");
        poline.setCurrency(
            objPo.getCurrency() == null
                ? (objPo.getOrganization().getCurrency() == null ? objCurrency
                    : objPo.getOrganization().getCurrency())
                : objPo.getCurrency());
        // poline.setCurrency(objPo.getCurrency());
        // poline.setescmpa
        if (tax != null) {
          poline.setTax(tax);
        }
        // poline.setTax(OBDal.getInstance().get(TaxRate.class,
        // "97E8BE0B96364939A4515D7FEFFA41BF"));

        if (line.getProduct() != null) {
          poline.setProduct(line.getProduct());
        }
        poline.setEscmProdescription(line.getDescription());
        poline.setUOM(line.getUOM());

        if (proposal.getProposalstatus().equals("PAWD")) {
          if (line.getAwardedqty().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal awardedQty = line.getAwardedqty();
            poline.setOrderedQuantity(awardedQty);
            BigDecimal lineTotal = awardedQty.multiply(line.getNetUnitprice());
            if (proposal.getEfinTaxMethod() != null) {
              if (line.getTaxAmount() != null) {
                BigDecimal taxAmt = line.getUnittax().multiply(awardedQty);
                BigDecimal totalAmt = lineTotal.subtract(taxAmt);
                poline.setUnitPrice(totalAmt.divide(awardedQty, 2, RoundingMode.HALF_UP));
                if (!proposal.getEfinTaxMethod().isPriceIncludesTax()) {
                  poline.setEscmLineTotalUpdated(awardedQty.multiply(poline.getUnitPrice()));
                } else {
                  BigDecimal taxPercent = new BigDecimal(
                      proposal.getEfinTaxMethod().getTaxpercent());
                  int roundoffConst = 2;
                  BigDecimal UpdatedLineTotal = ((line.getNegotUnitPrice().add(line.getUnittax()))
                      .multiply(awardedQty))
                          .divide(
                              BigDecimal.ONE.add(taxPercent.divide(new BigDecimal(100),
                                  roundoffConst, RoundingMode.HALF_UP)),
                              roundoffConst, RoundingMode.HALF_UP);
                  poline.setEscmLineTotalUpdated(UpdatedLineTotal);

                }
              }

            } else {
              poline.setUnitPrice(lineTotal.divide(line.getAwardedqty(), 2, RoundingMode.HALF_UP));
              poline.setEscmLineTotalUpdated(lineTotal);
            }
          }

        } else {
          poline.setOrderedQuantity((line.getMovementQuantity()));
          if (proposal.getEfinTaxMethod() != null) {
            if (line.getTaxAmount() != null) {
              BigDecimal totalAmt = line.getLineTotal().subtract(line.getTaxAmount());
              poline.setUnitPrice(
                  totalAmt.divide(line.getMovementQuantity(), 2, RoundingMode.HALF_UP));
              if (!proposal.getEfinTaxMethod().isPriceIncludesTax()) {
                poline.setEscmLineTotalUpdated(
                    line.getMovementQuantity().multiply(poline.getUnitPrice()));
              } else {
                BigDecimal taxPercent = new BigDecimal(proposal.getEfinTaxMethod().getTaxpercent());
                int roundoffConst = 2;
                BigDecimal UpdatedLineTotal = ((line.getNegotUnitPrice().add(line.getUnittax()))
                    .multiply(line.getMovementQuantity())).divide(
                        BigDecimal.ONE.add(taxPercent.divide(new BigDecimal(100), roundoffConst,
                            RoundingMode.HALF_UP)),
                        roundoffConst, RoundingMode.HALF_UP);
                poline.setEscmLineTotalUpdated(UpdatedLineTotal);

              }
            }

          } else {
            poline.setUnitPrice(
                line.getLineTotal().divide(line.getMovementQuantity(), 2, RoundingMode.HALF_UP));
            poline.setEscmLineTotalUpdated(line.getLineTotal());

          }
        }

        poline.setEscmPodiscount(line.getDiscount());
        poline.setEscmPodiscountamount(line.getDiscountmount());

        if (proposal.getProposalstatus().equals("PAWD")) {
          poline.setLineNetAmount(line.getAwardedqty().multiply(line.getNetUnitprice()));
        } else {
          poline.setLineNetAmount(line.getLineTotal());
        }

        // poline.setLineNetAmount((line.getMovementQuantity()).multiply(line.getNegotUnitPrice()));
        poline.setEscmProposalmgmt(proposal);
        poline.setEscmIsmanual(false);
        poline.setEscmIssummarylevel(false);
        poline.setEscmProposalmgmtLine(line);
        poline.setEscmProductCategory(line.getProductCategory());
        poline.setEscmNeedbydate(needByDate);
        if (mainparentid != null) {
          poline.setEscmParentline(OBDal.getInstance().get(EscmOrderlineV.class, poLineParentId));
        }
        poline.getSalesOrder().setEscmProposalmgmt(proposal);
        poline.getSalesOrder().setEscmProposalno(proposal.getSupplierProposalNo());
        poline.getSalesOrder().setEscmProposaldate(proposal.getSupplierProposalDate());
        poline.getSalesOrder().setEscmAddproposal(true);
        if (proposal.getSecondsupplier() != null)
          poline.getSalesOrder().setEscmSecondsupplier(proposal.getSecondsupplier());
        if (proposal.getSecondBranchname() != null)
          poline.getSalesOrder().setEscmSecondBranchname(proposal.getSecondBranchname());
        poline.getSalesOrder().setEscmIssecondsupplier(proposal.isSecondsupplier());
        if (proposal.getIBAN() != null)
          poline.getSalesOrder().setEscmSecondIban(proposal.getIBAN());
        if (proposal.getSubcontractors() != null)
          poline.getSalesOrder().setEscmSubcontractors(proposal.getSubcontractors());
        if (proposal.getEscmBidmgmt() != null) {
          poline.getSalesOrder().setEscmBidmgmt(proposal.getEscmBidmgmt());
          poline.getSalesOrder().setEscmProjectname(proposal.getEscmBidmgmt().getBidname());
        } else {
          poline.getSalesOrder().setEscmProjectname(proposal.getBidName());
        }
        poline.setEFINUniqueCode(line.getEFINUniqueCode());
        poline.setEFINUniqueCodeName(line.getEFINUniqueCodeName());
        if (!poline.getSalesOrder().getEscmOrdertype().equals("PUR_AG")) {
          poline.setEfinBudEncumlines(line.getEfinBudgmanencumline());
          poline.getSalesOrder().setEfinBudgetManencum(proposal.getEfinEncumbrance());
        }
        poline.setEscmProposalmgmtLine(line);
        poline.setBusinessPartner(proposal.getSupplier());
        if (line.getTaxAmount() != null) {
          if (proposal.getProposalstatus().equals("PAWD")) {
            poline.setEscmLineTaxamt(line.getUnittax().multiply(line.getAwardedqty()));
          } else {
            poline.setEscmLineTaxamt(line.getTaxAmount());
          }

        }
        if (inclusiveTax) {
          // new fields
          if (proposal.isNeedEvaluation()) {
            poline.setEscmInitialUnitprice(line.getNegotUnitPrice().add(line.getUnittax()));
          } else {
            poline.setEscmInitialUnitprice(line.getGrossUnitPrice());
          }
          poline.setEscmUnittax(line.getUnittax());
          poline.setEscmNetUnitprice(line.getNetUnitprice());
          poline.setEscmRounddiffTax(line.getRounddiffTax());
        }
        OBDal.getInstance().save(poline);
        OBDal.getInstance().flush();
        objPo.getOrderLineList().add(poline);
      }

      /*
       * for (EscmProposalmgmtLine line : proposal.getEscmProposalmgmtLineList()) {
       * 
       * }
       */

      // whenever adding the proposal under order update the proposal id and proposal attid for
      // related bg
      if (objPo.getEscmProposalmgmt() != null) {
        proatt = BankGuaranteeDetailEventDAO
            .getProposalAttribute(objPo.getEscmProposalmgmt().getId());
        OBQuery<Escmbankguaranteedetail> bg = OBDal.getInstance()
            .createQuery(Escmbankguaranteedetail.class, " as e where e.salesOrder.id=:orderID");
        bg.setNamedParameter("orderID", objPo.getId());
        bgdetail = bg.list();
        if (bgdetail.size() > 0) {
          for (Escmbankguaranteedetail bgobj : bgdetail) {
            bgobj.setEscmProposalmgmt(objPo.getEscmProposalmgmt());
            if (proatt != null)
              bgobj.setEscmProposalAttr(proatt);
            OBDal.getInstance().save(bgobj);
          }
        }
      }
      if (objPo.getEscmProposalmgmt() != null) {
        log.debug("Enter");
        OBQuery<EscmproposalDistribution> pgmt = OBDal.getInstance().createQuery(
            EscmproposalDistribution.class, " as e where e.escmProposalmgmt.id=:proposalID");
        pgmt.setNamedParameter("proposalID", objPo.getEscmProposalmgmt().getId());
        log.debug("pgmt" + pgmt.getWhereAndOrderBy());
        proposaldis = pgmt.list();
        if (proposaldis.size() > 0) {
          for (EscmproposalDistribution proposalobj : proposaldis) {
            proposalobj.setDocumentNo(objPo);
            OBDal.getInstance().save(proposalobj);
          }
        }
      }
      count = 1;
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log.error("Exception in insertOrderline in purchaseorder: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    } finally {
      OBContext.restorePreviousMode();
    }
    return count;
  }
}
