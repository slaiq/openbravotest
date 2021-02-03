package sa.elm.ob.scm.actionHandler;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.scm.EscmInitialReceipt;
import sa.elm.ob.scm.EscmInitialreceiptView;

public class POReceiptAddLinesDAO {

  private final Logger log = LoggerFactory.getLogger(POcontractAddproposalDAO.class);

  /**
   * Method to insert InitialReceipt
   * 
   * @param inoutId
   * @param selectedlines
   * @return success 0 error 1
   */
  public int insertInitialReceipt(String inoutId, JSONArray selectedlines) {

    // long lineno = 10;
    @SuppressWarnings("unused")
    org.openbravo.model.common.plm.Product prod = null;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    String mainparentid = null, parentid = null, poLineParentId = null;
    OBQuery<EscmInitialReceipt> porecln = null;
    List<EscmInitialReceipt> poreclist = null;
    int countOfLineWithZeroQty = 0, countwithZeroAmt = 0;
    // Date currentDate = new Date();
    String receiveType = null;
    try {
      OBContext.setAdminMode();
      ShipmentInOut objInout = OBDal.getInstance().get(ShipmentInOut.class, inoutId);
      receiveType = objInout.getEscmReceivetype();
      // get recent lineno
      // OBQuery<EscmInitialReceipt> linesQry = OBDal.getInstance().createQuery(
      // EscmInitialReceipt.class,
      // "as e where e.goodsShipment.id='" + inoutId + "' order by e.lineNo desc");
      //
      // linesQry.setMaxResult(1);
      // if (linesQry.list().size() > 0) {
      // EscmInitialReceipt objExistLine = linesQry.list().get(0);
      // lineno = objExistLine.getLineNo() + 10;
      // }
      for (int i = 0; i < selectedlines.length(); i++) {

        JSONObject selectedRow = selectedlines.getJSONObject(i);
        if (selectedRow.getString("summary").equals("false")) {
          // check requested qty should not be less than zero

          if (selectedRow.has("requestedQty")
              && new BigDecimal(selectedRow.getString("requestedQty"))
                  .compareTo(BigDecimal.ZERO) <= 0
              && selectedRow.getString("summary").equals("false")) {
            // OBDal.getInstance().rollbackAndClose();
            // return 1;
            countOfLineWithZeroQty = countOfLineWithZeroQty + 1;
          } else if (objInout.getEscmReceivingtype().equals("PROJ") && receiveType.equals("AMT")
              && new BigDecimal(selectedRow.getString("amount")).compareTo(BigDecimal.ZERO) <= 0
              && selectedRow.getString("summary").equals("false")) {
            countwithZeroAmt = countwithZeroAmt + 1;
          } else {
            if (selectedRow.getString("product") != null) {
              prod = OBDal.getInstance().get(org.openbravo.model.common.plm.Product.class,
                  selectedRow.getString("product"));
            }

            // check line already exists if exists update the quantity else insert
            OBQuery<EscmInitialReceipt> existingLines = OBDal.getInstance()
                .createQuery(EscmInitialReceipt.class, "as e where e.goodsShipment.id=:inoutID "
                    + " and e.salesOrderLine.id =:orderLnID order by e.lineNo desc");
            existingLines.setNamedParameter("inoutID", inoutId);
            existingLines.setNamedParameter("orderLnID", selectedRow.getString("salesOrderLine"));
            existingLines.setMaxResult(1);
            if (existingLines.list() != null && existingLines.list().size() > 0) {
              EscmInitialReceipt initialReceipt = existingLines.list().get(0);
              OrderLine objOrderLine = initialReceipt.getSalesOrderLine();
              if (selectedRow.has("requestedQty")) {
                initialReceipt.setQuantity(new BigDecimal(selectedRow.getString("requestedQty")));
              }
              if (objInout.getEscmReceivingtype().equals("PROJ") && receiveType.equals("AMT")) {
                initialReceipt.setReceivedAmount(new BigDecimal(selectedRow.getString("amount")));
                initialReceipt.setTOTLineAmt(new BigDecimal(selectedRow.getString("amount")));
                initialReceipt.setPercentageAchieved(
                    new BigDecimal(selectedRow.getString("percentagearchived")));
              } else if (receiveType != null && receiveType.equals("QTY")) {
                if (selectedRow.has("unitPrice") && selectedRow.has("requestedQty")) {
                  initialReceipt.setTOTLineAmt(new BigDecimal(selectedRow.getString("unitPrice"))
                      .multiply(new BigDecimal(selectedRow.getString("requestedQty"))));
                }
              }

              if (objInout.getEscmReceivingtype().equals("PROJ") && receiveType.equals("AMT")) {
                Date exeStartDateH = dateFormat.parse(selectedRow.getString("exestartdategre"));
                Date exeEndDateH = dateFormat.parse(selectedRow.getString("exeenddategre"));
                initialReceipt.setEXEStartDateG(selectedRow.getString("exestartdategre"));
                initialReceipt.setEXEEndDateG(selectedRow.getString("exeenddategre"));
                initialReceipt.setEXEStartDateH(exeStartDateH);
                initialReceipt.setEXEEndDateH(exeEndDateH);
                initialReceipt
                    .setContractDelayDays(new Long(selectedRow.getString("contractdelaydays")));
                initialReceipt
                    .setContractExeDays(new Long(selectedRow.getString("contractexedays")));
              } else if (objInout.getEscmReceivingtype().equals("PROJ")
                  && receiveType.equals("QTY")) {
                String strExeStartDateH = null;
                String strExeEndDateH = null;
                Date exeStartDateH = null;
                Date exeEndDateH = null;
                if (selectedRow.has("exestartdateh")
                    && selectedRow.getString("exestartdateh") != null) {
                  strExeStartDateH = selectedRow.getString("exestartdateh");
                }
                if (selectedRow.has("exeenddateh")
                    && selectedRow.getString("exeenddateh") != null) {
                  strExeEndDateH = selectedRow.getString("exeenddateh");
                }
                if (strExeStartDateH != null && strExeEndDateH != null) {
                  exeStartDateH = dateFormat.parse(selectedRow.getString("exestartdategre"));
                  exeEndDateH = dateFormat.parse(selectedRow.getString("exeenddategre"));

                  initialReceipt.setEXEStartDateG(selectedRow.getString("exestartdategre"));
                  initialReceipt.setEXEEndDateG(selectedRow.getString("exeenddategre"));
                  initialReceipt.setEXEStartDateH(exeStartDateH);
                  initialReceipt.setEXEEndDateH(exeEndDateH);
                  initialReceipt
                      .setContractDelayDays(new Long(selectedRow.getString("contractdelaydays")));
                  initialReceipt
                      .setContractExeDays(new Long(selectedRow.getString("contractexedays")));
                }
              }
              initialReceipt.setOrderedQuantity(objOrderLine.getOrderedQuantity());
              initialReceipt.setOrderedamt(objOrderLine.getLineNetAmount());
              initialReceipt.setNegotiatedUnitprice(objOrderLine.getUnitPrice());
              initialReceipt.setChangeFactor(objOrderLine.getEscmPoChangeFactor());
              initialReceipt.setChangeType(objOrderLine.getEscmPoChangeType());
              initialReceipt.setChangeValue(objOrderLine.getEscmPoChangeValue());
              initialReceipt.setUnitpriceAfterchag(objOrderLine.getEscmUnitpriceAfterchag());
              initialReceipt.setTaxAmount(objOrderLine.getEscmLineTaxamt());
              initialReceipt.setUnitTax(objOrderLine.getEscmUnittax());

              initialReceipt.setRemainingQuantity(objOrderLine.getOrderedQuantity()
                  .subtract((objOrderLine.getEscmQtyporec() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmQtyporec())
                          .subtract(objOrderLine.getEscmQtyirr() == null ? BigDecimal.ZERO
                              : objOrderLine.getEscmQtyirr())
                          .subtract(objOrderLine.getEscmQtyrejected() == null ? BigDecimal.ZERO
                              : objOrderLine.getEscmQtyrejected())
                          .subtract(objOrderLine.getEscmQtyreturned() == null ? BigDecimal.ZERO
                              : objOrderLine.getEscmQtyreturned()))
                  .subtract(objOrderLine.getEscmQtycanceled() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmQtycanceled())
                  .subtract(objOrderLine.getEscmLegacyQtyDelivered() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmLegacyQtyDelivered()));

              initialReceipt.setRemainingAmt(objOrderLine.getLineNetAmount()
                  .subtract((objOrderLine.getEscmAmtporec() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmAmtporec())
                          .subtract(objOrderLine.getEscmAmtreturned() == null ? BigDecimal.ZERO
                              : objOrderLine.getEscmAmtreturned()))
                  .subtract(objOrderLine.getEscmAmtcanceled() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmAmtcanceled())
                  .subtract(objOrderLine.getEscmLegacyAmtDelivered() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmLegacyAmtDelivered()));

              initialReceipt.setRounddiffTax(objOrderLine.getEscmRounddiffTax());
              initialReceipt.setRounddiffInvoice(objOrderLine.getEscmRounddiffInvoice());

              OBDal.getInstance().save(initialReceipt);
            } else {

              // Insert Tree
              List<String> parentlist = new ArrayList<String>();
              // clearing previous tree
              parentlist.clear();

              OrderLine objOrderLine = OBDal.getInstance().get(OrderLine.class,
                  selectedRow.getString("salesOrderLine"));

              mainparentid = objOrderLine.getEscmParentline() != null
                  ? objOrderLine.getEscmParentline().getId()
                  : null;
              parentid = mainparentid;

              porecln = OBDal.getInstance().createQuery(EscmInitialReceipt.class,
                  " as e where e.salesOrderLine.id=:parentID and goodsShipment.id=:inoutID");
              porecln.setNamedParameter("parentID", parentid);
              porecln.setNamedParameter("inoutID", inoutId);

              porecln.setMaxResult(1);
              poreclist = porecln.list();
              if (parentid != null && poreclist.size() == 0) {
                parentlist.add(parentid);
              } else {
                poLineParentId = poreclist.size() > 0 ? poreclist.get(0).getId() : null;
              }
              if (poreclist.size() == 0) {
                while (parentid != null) {
                  OrderLine parent = OBDal.getInstance().get(OrderLine.class, parentid);
                  parentid = parent.getEscmParentline() != null ? parent.getEscmParentline().getId()
                      : null;
                  porecln = OBDal.getInstance().createQuery(EscmInitialReceipt.class,
                      " as e where e.salesOrderLine.id=:parentID and e.goodsShipment.id=:inoutID ");
                  porecln.setNamedParameter("parentID", parentid);
                  porecln.setNamedParameter("inoutID", inoutId);
                  poreclist = porecln.list();
                  if (parentid != null && poreclist.size() == 0) {
                    parentlist.add(parentid);
                  }
                }

                ListIterator<String> li = parentlist.listIterator(parentlist.size());
                // Iterate in reverse.
                while (li.hasPrevious()) {
                  String poParentId = null;
                  OrderLine line = OBDal.getInstance().get(OrderLine.class, li.previous());
                  // get parentPoLineID
                  if (line.getEscmParentline() != null) {
                    OBQuery<EscmInitialReceipt> poParentln = OBDal.getInstance().createQuery(
                        EscmInitialReceipt.class,
                        " as e where e.salesOrderLine.id=:parentLnID and e.goodsShipment.id=:inoutID");
                    poParentln.setNamedParameter("parentLnID", line.getEscmParentline().getId());
                    poParentln.setNamedParameter("inoutID", inoutId);
                    List<EscmInitialReceipt> poParentList = poParentln.list();
                    if (poParentList.size() > 0) {
                      poParentId = poParentList.get(0).getId();
                    }
                  }

                  EscmInitialReceipt newObject = OBProvider.getInstance()
                      .get(EscmInitialReceipt.class);
                  newObject.setGoodsShipment(objInout);
                  newObject.setOrganization(objInout.getOrganization());
                  newObject.setClient(objInout.getClient());
                  newObject.setAlertStatus("A");
                  // newObject.setUnitprice(new BigDecimal(selectedRow.getString("unitPrice")));

                  if (objInout.getEscmReceivingtype().equals("PROJ") && receiveType.equals("AMT")) {
                    newObject.setQuantity(new BigDecimal(1));
                  } else {
                    newObject.setQuantity(new BigDecimal(selectedRow.getString("requestedQty")));
                  }

                  newObject.setManual(false);
                  if (line.getProduct() != null) {
                    newObject.setProduct(line.getProduct());
                    newObject.setImage(line.getProduct().getImage());
                  } else {
                    newObject.setProduct(null);
                    newObject.setImage(null);
                  }
                  newObject.setSummaryLevel(true);
                  if (poParentId != null) {
                    newObject.setParentLine(
                        OBDal.getInstance().get(EscmInitialreceiptView.class, poParentId));
                  }
                  newObject.setSourceRef(null);
                  newObject.setDescription(line.getEscmProdescription());
                  newObject.setLineNo(line.getLineNo());
                  newObject.setNotes("");
                  newObject
                      .setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
                  newObject.setSalesOrderLine(line);
                  newObject
                      .setDeliverydate(dateFormat.parse(dateFormat.format(new java.util.Date())));
                  newObject.setOrderedQuantity(new BigDecimal(1));
                  newObject.setUnitprice(BigDecimal.ZERO);
                  newObject.setQuantity(BigDecimal.ZERO);
                  OBDal.getInstance().save(newObject);
                  OBDal.getInstance().flush();
                  poLineParentId = newObject.getId();
                  // lineno = lineno + 10;
                }
              }

              EscmInitialReceipt newObject = OBProvider.getInstance().get(EscmInitialReceipt.class);
              newObject.setGoodsShipment(objInout);
              newObject.setOrganization(objInout.getOrganization());
              newObject.setClient(objInout.getClient());
              newObject.setAlertStatus("A");
              newObject.setUnitprice(new BigDecimal(selectedRow.getString("unitPrice")));
              if (objInout.getEscmReceivingtype().equals("PROJ") && receiveType.equals("AMT")) {
                // String exegregDate = dateFormat.format(currentDate);
                Date exeStartDateH = dateFormat.parse(selectedRow.getString("exestartdategre"));
                Date exeEndDateH = dateFormat.parse(selectedRow.getString("exeenddategre"));
                newObject.setQuantity(new BigDecimal(1));
                newObject.setTOTLineAmt(new BigDecimal(selectedRow.getString("amount")));
                newObject.setReceivedAmount(new BigDecimal(selectedRow.getString("amount")));
                newObject.setPercentageAchieved(
                    new BigDecimal(selectedRow.getString("percentagearchived")));
                newObject.setEXEStartDateG(selectedRow.getString("exestartdategre"));
                newObject.setEXEEndDateG(selectedRow.getString("exeenddategre"));
                newObject.setEXEStartDateH(exeStartDateH);
                newObject.setEXEEndDateH(exeEndDateH);
                newObject
                    .setContractDelayDays(new Long(selectedRow.getString("contractdelaydays")));
                newObject.setContractExeDays(new Long(selectedRow.getString("contractexedays")));
              } else {
                newObject.setQuantity(new BigDecimal(selectedRow.getString("requestedQty")));
              }
              if (objInout.getEscmReceivingtype().equals("PROJ") && receiveType.equals("QTY")) {
                String strExeStartDateH = null;
                String strExeEndDateH = null;
                Date exeStartDateH = null;
                Date exeEndDateH = null;
                if (selectedRow.has("exestartdateh")
                    && selectedRow.getString("exestartdateh") != null) {
                  strExeStartDateH = selectedRow.getString("exestartdateh");
                }
                if (selectedRow.has("exeenddateh")
                    && selectedRow.getString("exeenddateh") != null) {
                  strExeEndDateH = selectedRow.getString("exeenddateh");
                }
                if (strExeStartDateH != null && strExeEndDateH != null) {
                  exeStartDateH = dateFormat.parse(selectedRow.getString("exestartdategre"));
                  exeEndDateH = dateFormat.parse(selectedRow.getString("exeenddategre"));

                  newObject.setEXEStartDateG(selectedRow.getString("exestartdategre"));
                  newObject.setEXEEndDateG(selectedRow.getString("exeenddategre"));
                  newObject.setEXEStartDateH(exeStartDateH);
                  newObject.setEXEEndDateH(exeEndDateH);
                  newObject
                      .setContractDelayDays(new Long(selectedRow.getString("contractdelaydays")));
                  newObject.setContractExeDays(new Long(selectedRow.getString("contractexedays")));
                }
              }

              if (selectedRow.has("unitPrice") && selectedRow.has("requestedQty")) {
                newObject.setTOTLineAmt(new BigDecimal(selectedRow.getString("unitPrice"))
                    .multiply(new BigDecimal(selectedRow.getString("requestedQty"))));
              }
              newObject.setManual(false);
              if (objOrderLine.getProduct() != null) {
                newObject.setProduct(objOrderLine.getProduct());
                newObject.setImage(objOrderLine.getProduct().getImage());
              } else {
                newObject.setProduct(null);
                newObject.setImage(null);
              }
              newObject.setSummaryLevel(false);
              if (poLineParentId != null) {
                newObject.setParentLine(
                    OBDal.getInstance().get(EscmInitialreceiptView.class, poLineParentId));
              }
              newObject.setSourceRef(null);
              newObject.setDescription(selectedRow.getString("escmProdescription") == null ? ""
                  : selectedRow.getString("escmProdescription"));
              newObject.setLineNo(objOrderLine.getLineNo());
              newObject.setNotes("");
              newObject.setUOM(OBDal.getInstance().get(UOM.class, selectedRow.getString("uOM")));
              newObject.setSalesOrderLine(objOrderLine);
              newObject.setDeliverydate(dateFormat.parse(dateFormat.format(new java.util.Date())));
              // Task No.
              newObject.setOrderedQuantity(objOrderLine.getOrderedQuantity());
              newObject.setOrderedamt(objOrderLine.getLineNetAmount());
              newObject.setNegotiatedUnitprice(objOrderLine.getUnitPrice());
              newObject.setChangeFactor(objOrderLine.getEscmPoChangeFactor());
              newObject.setChangeType(objOrderLine.getEscmPoChangeType());
              newObject.setChangeValue(objOrderLine.getEscmPoChangeValue());
              newObject.setUnitpriceAfterchag(objOrderLine.getEscmUnitpriceAfterchag());
              newObject.setTaxAmount(objOrderLine.getEscmLineTaxamt());
              newObject.setUnitTax(objOrderLine.getEscmUnittax());

              newObject.setRemainingQuantity(objOrderLine.getOrderedQuantity()
                  .subtract((objOrderLine.getEscmQtyporec() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmQtyporec())
                          .subtract(objOrderLine.getEscmQtyirr() == null ? BigDecimal.ZERO
                              : objOrderLine.getEscmQtyirr())
                          .subtract(objOrderLine.getEscmQtyrejected() == null ? BigDecimal.ZERO
                              : objOrderLine.getEscmQtyrejected())
                          .subtract(objOrderLine.getEscmQtyreturned() == null ? BigDecimal.ZERO
                              : objOrderLine.getEscmQtyreturned()))
                  .subtract(objOrderLine.getEscmQtycanceled() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmQtycanceled())
                  .subtract(objOrderLine.getEscmLegacyQtyDelivered() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmLegacyQtyDelivered()));

              newObject.setRemainingAmt(objOrderLine.getLineNetAmount()
                  .subtract((objOrderLine.getEscmAmtporec() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmAmtporec())
                          .subtract(objOrderLine.getEscmAmtreturned() == null ? BigDecimal.ZERO
                              : objOrderLine.getEscmAmtreturned()))
                  .subtract(objOrderLine.getEscmAmtcanceled() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmAmtcanceled())
                  .subtract(objOrderLine.getEscmLegacyAmtDelivered() == null ? BigDecimal.ZERO
                      : objOrderLine.getEscmLegacyAmtDelivered()));

              newObject.setRounddiffTax(objOrderLine.getEscmRounddiffTax());
              newObject.setRounddiffInvoice(objOrderLine.getEscmRounddiffInvoice());
              OBDal.getInstance().save(newObject);
              OBDal.getInstance().flush();
              // lineno = lineno + 10;
            }
          }
        }
      }
      // if all selected line with qty 0 then throw error
      if (countOfLineWithZeroQty == selectedlines.length()) {
        return 1;
      } else if (countwithZeroAmt == selectedlines.length()) {
        return 2;
      } else {
        OBDal.getInstance().flush();
        return 0;
      }
    } catch (Exception e) {
      log.error("Exception in POReceiptAddLinesDAO :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

}
