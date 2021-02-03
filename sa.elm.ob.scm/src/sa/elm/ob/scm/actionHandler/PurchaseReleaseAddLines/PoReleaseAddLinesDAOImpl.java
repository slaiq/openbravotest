package sa.elm.ob.scm.actionHandler.PurchaseReleaseAddLines;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.SQLQuery;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmOrderlineV;
import sa.elm.ob.scm.actionHandler.PurchaseReleaseDAO;
import sa.elm.ob.utility.util.Constants;

public class PoReleaseAddLinesDAOImpl implements PoReleaseAddLinesDAO {
  private static final Logger log = LoggerFactory.getLogger(PoReleaseAddLinesDAOImpl.class);

  public int insertAgreementLines(String agreementLineId, BigDecimal releaseQty, Order releaseHdr,
      JSONObject selectedRow, Integer roundoffConst) {
    try {

      BigDecimal taxAmount = BigDecimal.ZERO;
      BigDecimal unitPrice = BigDecimal.ZERO;
      BigDecimal grossPrice = BigDecimal.ZERO;
      BigDecimal lineNet = BigDecimal.ZERO;
      OrderLine selectedAgreementLine = OBDal.getInstance().get(OrderLine.class, agreementLineId);
      BigDecimal selectedLineReleasedQty = releaseQty;
      long line = 0;
      PurchaseReleaseDAO purchaseReleaseDao = new PurchaseReleaseDAO();

      // Reset Tax Amount
      JSONObject taxObject = calculateTaxAmount(releaseHdr, selectedAgreementLine, roundoffConst,
          selectedLineReleasedQty);
      taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
      unitPrice = new BigDecimal(taxObject.get("UnitPrice").toString());
      grossPrice = new BigDecimal(taxObject.get("GrossPrice").toString());
      lineNet = new BigDecimal(taxObject.get("LineNet").toString());

      OBQuery<OrderLine> releaseLn = OBDal.getInstance().createQuery(OrderLine.class,
          "escmAgreementLine.id =:agreementLineID and salesOrder.id=:releaseHdrId ");
      releaseLn.setNamedParameter("agreementLineID", agreementLineId);
      releaseLn.setNamedParameter("releaseHdrId", releaseHdr.getId());
      List<OrderLine> ordLineList = new ArrayList<OrderLine>();
      ordLineList = releaseLn.list();

      // delete if the line is already inserted
      if (ordLineList.size() > 0) {
        OrderLine prevLine = ordLineList.get(0);
        line = prevLine.getLineNo();
        OBDal.getInstance().remove(prevLine);
        OBDal.getInstance().flush();
      }

      if (line == 0) {
        line = getLineNumber(releaseHdr);
      }

      // Create Purchase Release Lines
      if (!selectedAgreementLine.isEscmIssummarylevel()
          && selectedAgreementLine.getEscmParentline() == null) {
        OrderLine releaseLine = (OrderLine) DalUtil.copy(selectedAgreementLine, false);
        releaseLine.setEscmAgreementLine(selectedAgreementLine);
        releaseLine.setSalesOrder(releaseHdr);
        releaseLine.setEscmParentline(null);
        releaseLine.setCreationDate(new Date());
        releaseLine.setUpdated(new Date());
        releaseLine.setEscmPoChangeType(null);
        releaseLine.setEscmPoChangeFactor(null);
        releaseLine.setEscmPoChangeValue(BigDecimal.ZERO);
        releaseLine.setOrderedQuantity(selectedLineReleasedQty);
        releaseLine.setUnitPrice(unitPrice);
        releaseLine.setEscmLineTaxamt(taxAmount);
        releaseLine.setEscmLineTotalUpdated(grossPrice);
        releaseLine.setLineNetAmount(lineNet);
        releaseLine.setLineGrossAmount(lineNet);
        releaseLine.setLineNo(line);
        releaseLine.setEscmOldOrderline(null);

        // update the tax flag in header
        if (taxAmount.compareTo(BigDecimal.ZERO) != 0) {
          releaseLine.getSalesOrder().setEscmCalculateTaxlines(true);
        }
        OBDal.getInstance().save(releaseLine);
        OBDal.getInstance().flush();

      } else {
        ArrayList<String> parentList = new ArrayList<String>();
        parentList.add(selectedAgreementLine.getId());
        purchaseReleaseDao.getParentLines(selectedAgreementLine, parentList, releaseHdr, line,
            selectedRow, selectedAgreementLine.getSalesOrder());
      }

    } catch (Exception e) {
      log.error("Exception in insertAgreementLines: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }
    return 1;
  }

  public int deleteAgreementLine(String agreementLineId, Order releaseHdr) {
    try {
      OrderLine ordLine = null;

      List<OrderLine> ordLineList = new ArrayList<OrderLine>();
      OBQuery<OrderLine> releaseLine = OBDal.getInstance().createQuery(OrderLine.class,
          "escmAgreementLine.id =:agreementLineID and salesOrder.id=:releaseHdrId ");
      releaseLine.setNamedParameter("agreementLineID", agreementLineId);
      releaseLine.setNamedParameter("releaseHdrId", releaseHdr.getId());

      ordLineList = releaseLine.list();

      if (ordLineList.size() > 0) {
        ordLine = ordLineList.get(0);
        OBDal.getInstance().remove(ordLine);
        OBDal.getInstance().flush();
      }

    } catch (Exception e) {
      log.error("Exception in deleteAgreementLine: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }
    return 1;
  }

  public JSONObject calculateTaxAmount(Order releaseHdr, OrderLine selectedAgreementLine,
      int roundoffConst, BigDecimal selectedLineReleasedQty) {
    JSONObject taxObject = new JSONObject();
    BigDecimal releaseQty = selectedLineReleasedQty;

    BigDecimal grossPrice = BigDecimal.ZERO;
    BigDecimal unitPrice = BigDecimal.ZERO;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal taxpercent = BigDecimal.ZERO;
    BigDecimal lineNet = BigDecimal.ZERO;
    BigDecimal PERCENT = new BigDecimal("0.01");

    try {
      if (releaseHdr.isEscmIstax() && releaseHdr.getEscmTaxMethod() != null) {
        taxpercent = new BigDecimal(releaseHdr.getEscmTaxMethod().getTaxpercent());
        unitPrice = selectedAgreementLine.getLineNetAmount()
            .subtract(selectedAgreementLine.getEscmLineTaxamt())
            .divide(selectedAgreementLine.getOrderedQuantity(), roundoffConst,
                BigDecimal.ROUND_HALF_UP);
        grossPrice = selectedLineReleasedQty.multiply(unitPrice).setScale(roundoffConst,
            BigDecimal.ROUND_HALF_UP);
        taxAmount = grossPrice.multiply(taxpercent.multiply(PERCENT));
        lineNet = grossPrice.add(taxAmount).setScale(roundoffConst, BigDecimal.ROUND_HALF_UP);
      } else {
        unitPrice = selectedAgreementLine.getLineNetAmount()
            .divide(selectedAgreementLine.getOrderedQuantity());
        grossPrice = selectedLineReleasedQty.multiply(unitPrice).setScale(roundoffConst,
            BigDecimal.ROUND_HALF_UP);
        lineNet = grossPrice;
      }
      if (selectedAgreementLine.isEscmIssummarylevel()) {
        releaseQty = BigDecimal.ONE;
      }

      taxObject.put("GrossPrice", grossPrice);
      taxObject.put("UnitPrice", unitPrice);
      taxObject.put("TaxAmount", taxAmount);
      taxObject.put("LineNet", lineNet);
      taxObject.put("ReleaseQty", releaseQty);

    } catch (Exception e) {
      try {
        taxObject.put("GrossPrice", BigDecimal.ZERO);
        taxObject.put("UnitPrice", BigDecimal.ZERO);
        taxObject.put("TaxAmount", BigDecimal.ZERO);
        taxObject.put("LineNet", BigDecimal.ZERO);
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      log.error("Exception while calculateTaxAmount: " + e);
    }
    return taxObject;
  }

  public JSONObject calculateTaxAmtBased(Order releaseHdr, OrderLine selectedAgreementLine,
      BigDecimal selectedLineReleasedAmt) {
    JSONObject taxObject = new JSONObject();

    BigDecimal grossPrice = BigDecimal.ZERO;
    BigDecimal unitPrice = BigDecimal.ZERO;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal lineNet = BigDecimal.ZERO;
    BigDecimal weightage = BigDecimal.ZERO;
    BigDecimal PERCENT = new BigDecimal("0.01");

    try {
      // tax calculation
      if (releaseHdr.isEscmIstax() && releaseHdr.getEscmTaxMethod() != null) {
        weightage = ((selectedLineReleasedAmt.divide(selectedAgreementLine.getLineNetAmount(), 15,
            BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100)));
        taxAmount = (selectedAgreementLine.getEscmLineTaxamt()
            .multiply(weightage.multiply(PERCENT)));
        grossPrice = selectedLineReleasedAmt.subtract(taxAmount);
        unitPrice = grossPrice;
      } else {
        unitPrice = selectedLineReleasedAmt;
        grossPrice = selectedLineReleasedAmt;
      }
      lineNet = selectedLineReleasedAmt;

      taxObject.put("GrossPrice", grossPrice);
      taxObject.put("UnitPrice", unitPrice);
      taxObject.put("TaxAmount", taxAmount);
      taxObject.put("LineNet", lineNet);

    } catch (Exception e) {
      try {
        taxObject.put("GrossPrice", BigDecimal.ZERO);
        taxObject.put("UnitPrice", BigDecimal.ZERO);
        taxObject.put("TaxAmount", BigDecimal.ZERO);
        taxObject.put("LineNet", BigDecimal.ZERO);
      } catch (JSONException e1) {
        e1.printStackTrace();
      }
      log.error("Exception while calculateTaxAmtBased: " + e);
    }
    return taxObject;
  }

  public long getLineNumber(Order releaseHdr) {
    long line = 0;

    try {
      final SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(
          "select coalesce(max(line),0)+10 as lineno from c_orderline where c_order_id=:id");
      query.setParameter("id", releaseHdr.getId());
      line = ((BigDecimal) (Object) query.list().get(0)).longValue();

    } catch (Exception e) {
      log.error("Exception in getLineNumber: " + e);
    }
    return line;
  }

  @Override
  public int insertAgreementLinesAmt(String agreementLineId, BigDecimal releaseAmt,
      Order releaseHdr, JSONObject selectedRow) {
    try {

      OrderLine selectedAgreementLine = OBDal.getInstance().get(OrderLine.class, agreementLineId);
      BigDecimal selectedLineReleasedAmt = releaseAmt;
      BigDecimal unitPrice = BigDecimal.ZERO;
      BigDecimal grossPrice = BigDecimal.ZERO;
      BigDecimal weightage = BigDecimal.ZERO;
      BigDecimal taxAmount = BigDecimal.ZERO;
      BigDecimal PERCENT = new BigDecimal("0.01");
      long line = 0;
      PurchaseReleaseDAO purchaseReleaseDao = new PurchaseReleaseDAO();

      // tax calculation
      if (releaseHdr.isEscmIstax() && releaseHdr.getEscmTaxMethod() != null) {
        weightage = ((selectedLineReleasedAmt.divide(selectedAgreementLine.getLineNetAmount(), 15,
            BigDecimal.ROUND_HALF_UP)).multiply(new BigDecimal(100)));
        taxAmount = (selectedAgreementLine.getEscmLineTaxamt()
            .multiply(weightage.multiply(PERCENT)));
        grossPrice = selectedLineReleasedAmt.subtract(taxAmount);
        unitPrice = grossPrice;
      } else {
        unitPrice = selectedLineReleasedAmt;
        grossPrice = selectedLineReleasedAmt;
      }

      OBQuery<OrderLine> releaseLn = OBDal.getInstance().createQuery(OrderLine.class,
          "escmAgreementLine.id =:agreementLineID and salesOrder.id=:releaseHdrId ");
      releaseLn.setNamedParameter("agreementLineID", agreementLineId);
      releaseLn.setNamedParameter("releaseHdrId", releaseHdr.getId());
      List<OrderLine> ordLineList = new ArrayList<OrderLine>();
      ordLineList = releaseLn.list();

      // delete if the line is already inserted
      if (releaseLn.list().size() > 0) {
        OrderLine prevLine = ordLineList.get(0);
        line = prevLine.getLineNo();
        OBDal.getInstance().remove(prevLine);
        OBDal.getInstance().flush();
      }
      if (line == 0) {
        line = getLineNumber(releaseHdr);
      }
      if (!selectedAgreementLine.isEscmIssummarylevel()
          && selectedAgreementLine.getEscmParentline() == null) {
        // Create Purchase Release Lines
        OrderLine releaseLine = (OrderLine) DalUtil.copy(selectedAgreementLine, false);
        releaseLine.setEscmAgreementLine(selectedAgreementLine); // Agreement Line FK
        releaseLine.setLineNo(line);
        releaseLine.setSalesOrder(releaseHdr);
        releaseLine.setEscmParentline(null);
        releaseLine.setCreationDate(new Date());
        releaseLine.setUpdated(new Date());
        releaseLine.setEscmPoChangeType(null);
        releaseLine.setEscmPoChangeFactor(null);
        releaseLine.setEscmPoChangeValue(BigDecimal.ZERO);
        releaseLine.setOrderedQuantity(BigDecimal.ONE); // Amt Based
        releaseLine.setUnitPrice(unitPrice); // Unit Price
        releaseLine.setEscmLineTaxamt(taxAmount); // Tax
        releaseLine.setEscmLineTotalUpdated(grossPrice);
        releaseLine.setLineNetAmount(selectedLineReleasedAmt);
        releaseLine.setLineGrossAmount(selectedLineReleasedAmt);
        releaseLine.setEscmOldOrderline(null);
        // update the tax flag
        if (taxAmount.compareTo(BigDecimal.ZERO) != 0) {
          releaseLine.getSalesOrder().setEscmCalculateTaxlines(true);
        }

        OBDal.getInstance().save(releaseLine);
        OBDal.getInstance().flush();
      } else {
        ArrayList<String> parentList = new ArrayList<String>();
        parentList.add(selectedAgreementLine.getId());
        purchaseReleaseDao.getParentLines(selectedAgreementLine, parentList, releaseHdr, line,
            selectedRow, selectedAgreementLine.getSalesOrder());
      }

    } catch (Exception e) {
      log.error("Exception in insertAgreementLinesAmt: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }

    return 1;
  }

  @Override
  public int deleteAgreementLineAmt(String agreementLineId, Order releaseHdr) {
    try {
      OrderLine ordLine = null;

      List<OrderLine> ordLineList = new ArrayList<OrderLine>();
      OBQuery<OrderLine> releaseLine = OBDal.getInstance().createQuery(OrderLine.class,
          "escmAgreementLine.id =:agreementLineID and salesOrder.id=:releaseHdrId ");
      releaseLine.setNamedParameter("agreementLineID", agreementLineId);
      releaseLine.setNamedParameter("releaseHdrId", releaseHdr.getId());

      ordLineList = releaseLine.list();

      if (ordLineList.size() > 0) {
        ordLine = ordLineList.get(0);
        OBDal.getInstance().remove(ordLine);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      log.error("Exception in deleteAgreementLine: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }
    return 1;
  }

  @Override
  public int insertChildLines(String agreementLineId, Order releaseHdr, JSONObject taxObject,
      boolean isSelectedLine, List<String> selectedLinesList) {
    try {
      OrderLine selectedParentLine = OBDal.getInstance().get(OrderLine.class, agreementLineId);
      OrderLine parentLn = insertParentAndChild(selectedParentLine, releaseHdr, taxObject,
          isSelectedLine);
      EscmOrderlineV parentLineV = null;
      long line = 0;
      Boolean isselectedchild = Boolean.FALSE;

      if (selectedParentLine.isEscmIssummarylevel()) {
        // getting the child Lines of Selected Parent
        List<OrderLine> ordLineList = new ArrayList<OrderLine>();
        OBQuery<OrderLine> childLine = OBDal.getInstance().createQuery(OrderLine.class,
            "escmParentline.id =:parentLineID and businessPartner.id =:supplierID and salesOrder.id=:agreementHdrId");
        childLine.setNamedParameter("parentLineID", agreementLineId);
        childLine.setNamedParameter("agreementHdrId", selectedParentLine.getSalesOrder());
        childLine.setNamedParameter("supplierID", releaseHdr.getBusinessPartner().getId());
        ordLineList = childLine.list();

        if (ordLineList.size() > 0) {
          for (OrderLine childRcd : ordLineList) {

            if (childRcd.isEscmIssummarylevel()) {
              // If the child is a sub parent then insert the childLines of this sub parent
              insertChildLines(childRcd.getId(), releaseHdr, null, false, null);
            } else {
              line = 0;
              OBQuery<OrderLine> releaseLn = OBDal.getInstance().createQuery(OrderLine.class,
                  "escmAgreementLine.id =:agreementLineID and salesOrder.id=:releaseHdrId ");
              releaseLn.setNamedParameter("agreementLineID", childRcd.getId());
              releaseLn.setNamedParameter("releaseHdrId", releaseHdr.getId());
              List<OrderLine> ordLnList = new ArrayList<OrderLine>();
              ordLnList = releaseLn.list();
              if (ordLnList.size() == 0) {
                line = getLineNumber(releaseHdr);
                // inserting the child Line
                OrderLine releaseLine = (OrderLine) DalUtil.copy(childRcd, false);
                releaseLine.setEscmAgreementLine(childRcd); // Agreement Line FK
                releaseLine.setLineNo(line);
                releaseLine.setSalesOrder(releaseHdr);
                releaseLine.setCreationDate(new Date());
                releaseLine.setUpdated(new Date());
                releaseLine.setEscmPoChangeType(null);
                releaseLine.setEscmPoChangeFactor(null);
                releaseLine.setEscmPoChangeValue(BigDecimal.ZERO);
                releaseLine.setOrderedQuantity(childRcd.getOrderedQuantity());
                releaseLine.setUnitPrice(childRcd.getUnitPrice());
                releaseLine.setEscmLineTaxamt(childRcd.getEscmLineTaxamt());
                releaseLine.setEscmLineTotalUpdated(childRcd.getEscmLineTotalUpdated());
                releaseLine.setLineNetAmount(childRcd.getLineNetAmount());
                releaseLine.setLineGrossAmount(childRcd.getLineGrossAmount());
                releaseLine.setEscmIsmanual(false);
                releaseLine.setEscmOldOrderline(null);

                if (parentLn == null) {
                  releaseLine.setEscmParentline(null);
                } else {
                  parentLineV = OBDal.getInstance().get(EscmOrderlineV.class, parentLn.getId());
                  releaseLine.setEscmParentline(parentLineV);
                }

                OBDal.getInstance().save(releaseLine);
                OBDal.getInstance().flush();
              } else {
                // check whether the child is in selected Lines
                if (selectedLinesList != null) {
                  isselectedchild = selectedLinesList.contains(childRcd.getId());
                }
                // updating quantity and amount
                if (!isselectedchild) {
                  OrderLine prevLine = ordLnList.get(0);
                  prevLine.setOrderedQuantity(childRcd.getOrderedQuantity());
                  prevLine.setUnitPrice(childRcd.getUnitPrice());
                  prevLine.setEscmLineTaxamt(childRcd.getEscmLineTaxamt());
                  prevLine.setEscmLineTotalUpdated(childRcd.getEscmLineTotalUpdated());
                  prevLine.setLineNetAmount(childRcd.getLineNetAmount());
                  prevLine.setLineGrossAmount(childRcd.getLineGrossAmount());
                  OBDal.getInstance().save(prevLine);
                  OBDal.getInstance().flush();
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log.error("Exception in insertChildLines: ", e);
      OBDal.getInstance().rollbackAndClose();
      return 0;
    }
    return 1;
  }

  public OrderLine insertParentAndChild(OrderLine agreementLine, Order releaseHdr,
      JSONObject taxObject, boolean isSelectedLine) {
    OrderLine releaseLine = null;
    try {
      long line = 0;
      EscmOrderlineV parentLineV = null;
      BigDecimal taxAmount = BigDecimal.ZERO;
      BigDecimal unitPrice = BigDecimal.ZERO;
      BigDecimal grossPrice = BigDecimal.ZERO;
      BigDecimal lineNet = BigDecimal.ZERO;
      BigDecimal orderedQty = BigDecimal.ZERO;

      // Tax Calculation
      if (isSelectedLine) {
        if (releaseHdr.getEscmReceivetype().equals(Constants.QTY_BASED)) {
          orderedQty = new BigDecimal(taxObject.get("ReleaseQty").toString());
        } else if (releaseHdr.getEscmReceivetype().equals(Constants.AMOUNT_BASED)) {
          orderedQty = BigDecimal.ONE;
        }
        unitPrice = new BigDecimal(taxObject.get("UnitPrice").toString());
        taxAmount = new BigDecimal(taxObject.get("TaxAmount").toString());
        grossPrice = new BigDecimal(taxObject.get("GrossPrice").toString());
        lineNet = new BigDecimal(taxObject.get("LineNet").toString());

      } else {
        taxAmount = agreementLine.getEscmLineTaxamt();
        unitPrice = agreementLine.getUnitPrice();
        grossPrice = agreementLine.getLineGrossAmount();
        lineNet = agreementLine.getLineNetAmount();
        orderedQty = agreementLine.getOrderedQuantity();
      }

      // check if the line is already exists
      OBQuery<OrderLine> releaseLn = OBDal.getInstance().createQuery(OrderLine.class,
          "escmAgreementLine.id =:agreementLineID and salesOrder.id=:releaseHdrId ");
      releaseLn.setNamedParameter("agreementLineID", agreementLine.getId());
      releaseLn.setNamedParameter("releaseHdrId", releaseHdr.getId());
      List<OrderLine> ordLnList = new ArrayList<OrderLine>();
      ordLnList = releaseLn.list();

      if (ordLnList.size() > 0) {
        OrderLine prevLine = ordLnList.get(0);
        if (!prevLine.isEscmIssummarylevel() && isSelectedLine) {
          // updating quantity and amount
          prevLine.setOrderedQuantity(orderedQty);
          prevLine.setUnitPrice(unitPrice);
          prevLine.setEscmLineTaxamt(taxAmount);
          prevLine.setEscmLineTotalUpdated(grossPrice);
          prevLine.setLineNetAmount(lineNet);
          prevLine.setLineGrossAmount(grossPrice);
          OBDal.getInstance().save(prevLine);
          OBDal.getInstance().flush();
        }
        releaseLine = prevLine;
      } else {

        // inserting superParent record if not present
        if (agreementLine.getEscmParentline() != null) {
          parentLineV = getPoReleaseParentLine(agreementLine, releaseHdr);
          if (parentLineV == null) {
            OrderLine superParent = OBDal.getInstance().get(OrderLine.class,
                agreementLine.getEscmParentline().getId());
            OrderLine newParent = insertParentAndChild(superParent, releaseHdr, taxObject, false);
            parentLineV = OBDal.getInstance().get(EscmOrderlineV.class, newParent.getId());
          }
        }

        // inserting the parent or child line
        line = getLineNumber(releaseHdr);
        releaseLine = (OrderLine) DalUtil.copy(agreementLine, false);
        releaseLine.setEscmAgreementLine(agreementLine); // Agreement Line FK
        releaseLine.setLineNo(line);
        releaseLine.setSalesOrder(releaseHdr);
        releaseLine.setCreationDate(new Date());
        releaseLine.setUpdated(new Date());
        releaseLine.setEscmPoChangeType(null);
        releaseLine.setEscmPoChangeFactor(null);
        releaseLine.setEscmPoChangeValue(BigDecimal.ZERO);

        releaseLine.setOrderedQuantity(orderedQty);
        releaseLine.setUnitPrice(unitPrice);
        releaseLine.setEscmLineTaxamt(taxAmount);
        releaseLine.setEscmLineTotalUpdated(grossPrice);
        releaseLine.setLineNetAmount(lineNet);
        releaseLine.setLineGrossAmount(grossPrice);

        releaseLine.setEscmParentline(parentLineV);
        releaseLine.setEscmIsmanual(false);
        releaseLine.setEscmOldOrderline(null);
        OBDal.getInstance().save(releaseLine);
        OBDal.getInstance().flush();
      }
    } catch (Exception e) {
      log.error("Exception in insertParentAndChild: ", e);
    }
    return releaseLine;
  }

  public EscmOrderlineV getPoReleaseParentLine(OrderLine agreementLine, Order releaseHdr) {
    EscmOrderlineV parentLineV = null;

    try {
      OBQuery<OrderLine> parentLn = OBDal.getInstance().createQuery(OrderLine.class,
          "escmAgreementLine.id =:agreementLineID and salesOrder.id=:releaseHdrId ");
      parentLn.setNamedParameter("agreementLineID", agreementLine.getEscmParentline().getId());
      parentLn.setNamedParameter("releaseHdrId", releaseHdr.getId());
      List<OrderLine> ordLineList = new ArrayList<OrderLine>();
      ordLineList = parentLn.list();

      if (ordLineList.size() > 0) {
        OrderLine prntLine = ordLineList.get(0);
        parentLineV = OBDal.getInstance().get(EscmOrderlineV.class, prntLine.getId());
      }
    } catch (Exception e) {
      log.error("Exception in getPoReleaseParentLine: ", e);
    }
    return parentLineV;
  }

}
