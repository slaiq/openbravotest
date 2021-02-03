/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

OB.RM = OB.RM || {};

/**
 * Check that entered return quantity is less than original inout qty.
 */
OB.RM.RMOrderQtyValidate = function (item, validator, value, record) {
  if (!isc.isA.Number(value)) {
    return false;
  }
  // Check if record has related shipment to skip check.
  if (record.goodsShipmentLine === null || record.goodsShipmentLine === '') {
    return value !== null && value > 0;
  }
  var movementQty = record.movementQuantity !== null ? new BigDecimal(String(record.movementQuantity)) : BigDecimal.prototype.ZERO,
      returnedQty = record.returnQtyOtherRM !== null ? new BigDecimal(String(record.returnQtyOtherRM)) : BigDecimal.prototype.ZERO,
      newReturnedQty = new BigDecimal(String(value));
  if ((value !== null) && (newReturnedQty.compareTo(movementQty.subtract(returnedQty))) <= 0 && (value > 0)) {
    return true;
  } else {
    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_RM_OutOfRange', [movementQty.subtract(returnedQty).toString()]));
    return false;
  }
};

/**
 * Set quantity, storage bin and condition of the goods.
 */
OB.RM.RMOrderSelectionChange = function (grid, record, state) {
  var contextInfo = null;
  if (state) {
    contextInfo = grid.view.parentWindow.activeView.getContextInfo(false, true, true, true);
    if (!contextInfo.inpdateordered) {
      contextInfo = grid.view.parentWindow.activeView.parentView.getContextInfo(false, true, true, true);
    }
    if (!record.returnReason) {
      record.returnReason = contextInfo.inpcReturnReasonId;
    }
    OB.RemoteCallManager.call('org.openbravo.common.actionhandler.RFCServiceReturnableActionHandler', {
      rfcOrderDate: contextInfo.inpdateordered,
      goodsShipmentId: record.id,
      productId: record.product
    }, {}, function (response, data, request) {
      if (data.message) {
        if (data.message.severity === isc.OBMessageBar.TYPE_ERROR) {
          grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, data.message.title, data.message.text);
          grid.deselectRecord(record);
        } else {
          grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_WARNING, data.message.title, data.message.text);
        }
      }
    });
  }
};
/**
 * Check that entered received quantity is less than pending qty.
 */
OB.RM.RMReceiptQtyValidate = function (item, validator, value, record) {
  if ((value !== null) && (value <= record.pending) && (value > 0)) {
    return true;
  } else {
    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_RM_ReceivingMoreThanPending', [record.pending]));
    return false;
  }
};

/**
 * Set quantity, storage bin and condition of the goods.
 */
OB.RM.RMReceiptSelectionChange = function (grid, record, state) {
  var contextInfo = null;
  if (state) {
    record.receiving = record.pending;
    contextInfo = grid.view.parentWindow.activeView.getContextInfo(false, true, true, true);
    record.storageBin = contextInfo.ReturnLocator;
    if (!record.conditionGoods) {
      record.conditionGoods = contextInfo.inpmConditionGoodsId;
    }
  }
};

/**
 * Check that entered shipped quantity is less than pending qty.
 */
OB.RM.RMShipmentQtyValidate = function (item, validator, value, record) {
  var orderLine = record.orderLine,
      pendingQty = record.pending,
      selectedRecords = item.grid.getSelectedRecords(),
      selectedRecordsLength = selectedRecords.length,
      editedRecord = null,
      storageBin = record.storageBin,
      i;
  //Cheking available stock
  if (storageBin === null) {
    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_RM_NotAvailableStock', [record.rMOrderNo]));
    return false;
  }
  // check value is positive and below available qty and pending qty
  if (value === null || value < 0 || value > record.pending || value > record.availableQty) {
    if (record.pending < record.availableQty) {
      item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_RM_MoreThanPending', [record.pending]));
    } else {
      item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_RM_MoreThanAvailable', [record.availableQty]));
    }
    return false;
  }
  // check shipped total quantity for the order line is below pending qty.
  for (i = 0; i < selectedRecordsLength; i++) {
    editedRecord = isc.addProperties({}, selectedRecords[i], item.grid.getEditedRecord(selectedRecords[i]));
    if (editedRecord.orderLine === orderLine) {
      pendingQty -= editedRecord.movementQuantity;
      if (pendingQty < 0) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_RM_TooMuchShipped', [record.pending]));
        return false;
      }
    }
  }

  return true;
};

/**
 * Set quantity
 */
OB.RM.RMShipmentSelectionChange = function (grid, record, state) {
  var contextInfo = null,
      orderLine = record.orderLine,
      shippedQty = BigDecimal.prototype.ZERO,
      selectedRecords = grid.getSelectedRecords(),
      pending = new BigDecimal(String(record.pending)),
      availableQty = new BigDecimal(String(record.availableQty)),
      storageBin = record.storageBin,
      editedRecord = null,
      isstocked = record.stocked,
      i;
  if (state) {
    // Checking available stock
    if (storageBin === null && isstocked) {
      grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('OBUIAPP_RM_NotAvailableStock', [record.rMOrderNo]));
      return false;
    }
    // calculate already shipped qty on grid
    for (i = 0; i < selectedRecords.length; i++) {
      editedRecord = isc.addProperties({}, selectedRecords[i], grid.getEditedRecord(selectedRecords[i]));
      if (editedRecord.orderLine === orderLine && selectedRecords[i].id !== record.id) {
        shippedQty = shippedQty.add(new BigDecimal(String(editedRecord.movementQuantity)));
      }
    }
    pending = pending.subtract(shippedQty);
    if (pending.compareTo(availableQty) < 0) {
      record.movementQuantity = pending.toString();
    } else {
      record.movementQuantity = availableQty.toString();
    }
  }
};