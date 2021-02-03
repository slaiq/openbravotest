OB.ESCM = {};
OB.ESCM.QtyValidation = {};

OB.ESCM.QtyValidation.delqtymismatch = function(item, validator, value, record) {
    if (value < 0) {
        isc.warn(OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
        return false;
    }
    var acceptedQty = record.acceptedQuantity,
        deliveredQty = record.deliveredqty,
        quantity = record.quantity;
    if ((acceptedQty - deliveredQty - quantity) < 0) {
        isc.warn(OB.I18N.getLabel('ESCM_POFinalRec_GrtActQty'));
        return false;
    } else return true;

};
OB.ESCM.QtyValidation.insqtymismatch = function(item, validator, value, record) {
    var jsonInspList = {};
    jsonInspList.List = [];
    jsonInspList.List.length = 0;
    var selectedRecords = item.grid.getSelectedRecords(),
        selectedRecordsLength = selectedRecords.length,
        status = "",
        tempescminitialreceipt = "",
        errorflag = true;

    //chk qty validation not exceeds remaining inspected qty
    var remaInspQty = (record.irqty - record.inspectedQty),
        quantity = record.quantity;
    if (value < 0) {
        isc.warn(OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
        item.grid.setEditValue(item.grid.getRecordIndex(record), 'quantity', 0);
        errorflag = true;
    }
    if ((remaInspQty - quantity) < 0) {
        var msg = OB.I18N.getLabel('ESCM_POIns_GrtAvlInsQty');
        msg = msg.replace('%', remaInspQty);
        isc.warn(msg);
        // item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, msg);
        item.grid.setEditValue(item.grid.getRecordIndex(record), 'quantity', 0);
        errorflag = true;
    }
    if (errorflag) {
        // accept and rejected qty validation
        if (selectedRecordsLength > 0) {
            var data = jsonInspList.List,
                canAdd = true;
            jsonInspList.List.length = 0;
            data.length = 0;
            for (i = 0; i < selectedRecordsLength; i++) {
                editedRecord = isc.addProperties({}, selectedRecords[i], item.grid.getEditedRecord(selectedRecords[i]));
                var initialirqty = editedRecord.irqty,
                    inspectedQty = editedRecord.inspectedQty,
                    escminitialreceipt = editedRecord.escmInitialreceipt;
                var remInspectedQty = initialirqty - inspectedQty;
                var qty = editedRecord.quantity,
                    id = editedRecord.id,
                    status = editedRecord.alertStatus;
                for (var j in data) {
                    if (data[j].id == escminitialreceipt) {
                        if (status == 'A') {
                            jsonInspList.List[len]['acceptid'] = id;
                            jsonInspList.List[len]['acceptQty'] = qty;
                        } else {
                            jsonInspList.List[len]['rejectedid'] = id;
                            jsonInspList.List[len]['rejectedQty'] = qty;
                        }
                        data[j].qty = data[j].qty + qty;
                        canAdd = false;
                        break;
                    } else canAdd = true;
                }
                if (canAdd) {
                    var len = jsonInspList.List.length;
                    jsonInspList.List[len] = {};
                    jsonInspList.List[len]['id'] = escminitialreceipt;
                    if (status == 'A') {
                        jsonInspList.List[len]['acceptid'] = id;
                        jsonInspList.List[len]['acceptQty'] = qty;
                    } else {
                        jsonInspList.List[len]['rejectedid'] = id;
                        jsonInspList.List[len]['rejectedQty'] = qty;
                    }

                    jsonInspList.List[len]['qty'] = qty;
                    jsonInspList.List[len]['remInspectedQty'] = remInspectedQty;
                }
            }
        }
        for (var k in data) {
            if (data[k].remInspectedQty - data[k].qty < 0 || (data[k].qty < 0)) {
                if (data[k].acceptid != null && data[k].rejectedid != null) {
                    if (data[k].acceptid != record.id) {
                        item.grid.setEditValue(item.grid.getRecordIndex(item.grid.data.localData.find('id', data[k].acceptid)), 'quantity', (data[k].remInspectedQty - data[k].rejectedQty));
                        return true;
                    } else if (data[k].rejectedid != record.id) {
                        item.grid.setEditValue(item.grid.getRecordIndex(item.grid.data.localData.find('id', data[k].rejectedid)), 'quantity', (data[k].remInspectedQty - data[k].acceptQty));
                        return true;
                    }
                } else {
                    item.grid.setEditValue(item.grid.getRecordIndex(record), 'quantity', 0);
                }
            }
        }
    }

    return true;

};
OB.ESCM.QtyValidation.retqtymismatch = function(item, validator, value, record) {

    var reserved = record.reservedQuantity,
        quantity = record.newquantity;
    if ((reserved - quantity) < 0) {
        isc.warn(OB.I18N.getLabel('ESCM_PORecGrtRetAvaQty'));
        return false;
    } else return true;

};

OB.ESCM.QtyValidation.siteissuereqqtymismatch = function(item, validator, value, record) {

    if (value < 0) {
        isc.warn(OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
        return false;
    }
    var reserved = record.reservedQuantity,
        quantity = record.reqqty,
        issuedQty = record.issuedQty,
        inprgqty = record.inprgqty;
    if ((reserved - issuedQty - inprgqty) < quantity) {
        isc.warn(OB.I18N.getLabel('ESCM_SMIR_QtyGretthanRecQty'));
        return false;
    }

    var fractional = (quantity - Math.floor(quantity));
    if (fractional < 1 && fractional != 0) {
        isc.warn(OB.I18N.getLabel('ESCM_Fractional(Custody)'));
        return false;
    } else return true;

};

OB.ESCM.QtyValidation.createprqtyval = function(item, validator, value, record) {
    var recordId = record.id,
        pendingqty = record.pendingQty;
    if (value <= 0 || value == null) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
        return false;
    }
    if (recordId != null) {
        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.MaterialIssuePendQtyHandler', {
            recordId: recordId,
        }, {}, function(response, data, request) {

            if (value > data.pendingQty) {
                item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_SMIR_QtyGretthanRecQty'));
                item.grid.setEditValue(item.grid.getRecordIndex(record), 'pendingQty', 0);
            }
        });
    }
    var fractional = (value - Math.floor(value));
    if (fractional < 1 && fractional != 0) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_Fractional(Custody)'));
        return false;
    } else return true;

};
OB.ESCM.QtyValidation.bidQtyVal = function(item, validator, value, record) {
    var recordId = record.id,
        reqqty = record.requestedQty,
        bidqty = record.escmBidmgmtQty,
        qty = record.quantity;
    console.log()
    if ((reqqty - bidqty) < qty) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_SMIR_QtyGretthanRecQty'));
        return false;
    }
    if (value < 0 || value == null) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
        return false;
    } else return true;

};

OB.ESCM.QtyValidation.OnLoad = function(view) {
    receiptsToAdd = [];
    receiptsTotal = 0;
    console.log(view.theForm.getItem('Lines'));
    var form = view.theForm,
        linesGrid = form.getItem('Lines').canvas.viewGrid;
    linesGrid.selectionChanged = OB.ESCM.QtyValidation.selectionChanged;
};

OB.ESCM.QtyValidation.selectionChanged = function(record, state) {
    this.Super('selectionChanged', arguments);
    var lines = this.view.theForm.getItem('Lines').canvas.viewGrid;

    var reqqty = lines.getFieldByColumnName('reqqty');
    var bidqty = lines.getFieldByColumnName('escmBidmgmtQty');
    var remqty = lines.getFieldByColumnName('quantity');
    var reqqtyvalue = lines.getEditedCell(record, reqqty);
    var bidqtyvalue = lines.getEditedCell(record, bidqty);
    console.log(reqqtyvalue);
    console.log(bidqtyvalue);
    console.log(remqty);
    if (remqty != 0) {
        var remqtyvalue = reqqtyvalue - bidqtyvalue;
        console.log(remqtyvalue);
        lines.setEditValue(lines.getRecordIndex(record), 'quantity', remqtyvalue);
    } else {
        lines.setEditValue(lines.getRecordIndex(record), 'quantity', 0);
    }
};

OB.ESCM.QtyValidation.createPOQtyValidation = function(item, validator, value, record) {
    var recordId = record.id,
        varrequestedQty = parseFloat(record.requestedQty),
        varOrderQty = parseFloat(record.escmOrderQty);
    if (value <= 0 || value == null) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
        return false;
    } else {
        var allowQty = varrequestedQty - (parseFloat(value) + varOrderQty);
        if (allowQty < 0) {
            item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_SMIR_QtyGretthanRecQty'));
            return false;
        } else {
            return true;
        }
    }
};

OB.ESCM.QtyValidation.OnQuantityLoad = function(view) {
    receiptsToAdd = [];
    receiptsTotal = 0;
    console.log(view.theForm.getItem('Lines'));
    var form = view.theForm,
        linesGrid = form.getItem('Lines').canvas.viewGrid;
    linesGrid.selectionChanged = OB.ESCM.QtyValidation.selectionPOOrderChanged;
};

OB.ESCM.QtyValidation.selectionPOOrderChanged = function(record, state) {
    this.Super('selectionChanged', arguments);
    var lines = this.view.theForm.getItem('Lines').canvas.viewGrid;

    var reqqty = lines.getFieldByColumnName('reqqty');
    var orderQty = lines.getFieldByColumnName('escmOrderQty');
    var remqty = lines.getFieldByColumnName('quantity');
    var reqqtyvalue = lines.getEditedCell(record, reqqty);
    var orderQtyvalue = lines.getEditedCell(record, orderQty);
    if (remqty != 0) {
        var remqtyvalue = reqqtyvalue - orderQtyvalue;
        console.log(remqtyvalue);
        lines.setEditValue(lines.getRecordIndex(record), 'quantity', remqtyvalue);
    } else {
        lines.setEditValue(lines.getRecordIndex(record), 'quantity', 0);
    }
};

OB.ESCM.QtyValidation.EnterRequestedQTY = function(item, validator, value, record) {
    var recordId = record.id,
        varrequestedQty = parseFloat(record.requestedQty),
        varReqQty = parseFloat(record.pending);
    if (value <= 0 || value == null) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
        return false;
    } else {
        var allowQty = varReqQty - value;
        if (allowQty < 0) {
            item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_SMIR_QtyGretthanRecQty'));
            return false;
        } else {
            item.grid.view.messageBar.hide();
            return true;
        }

    }
};

/*
 * validation on add PR lines in Proposal
 */
OB.ESCM.QtyValidation.createPRProposalQtyValidation = function(item, validator, value, record) {
    var recordId = record.id,
        varrequestedQty = parseFloat(record.requestedQty),





        // varOrderQty = parseFloat(record.escmProposalQty);
        varOrderQty = 0;
    if (value <= 0 || value == null) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_PurReq_QtyZero'));
        return false;
    } else {
        var allowQty = varrequestedQty - (parseFloat(value) + varOrderQty);
        if (allowQty < 0) {
            item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_SMIR_QtyGretthanRecQty'));
            return false;
        } else {
            return true;
        }
    }
}; /*po receipt add line amount popup*/
OB.ESCM.QtyValidation.enterRequestedAmt = function(item, validator, value, record) {
    var recordId = record.id,
        varReqAmt = parseFloat(record.pendingamt);
    if (value <= 0 || value == null) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_PurReq_AmtZero'));
        return false;
    } else {
        var allowAmt = varReqAmt - value;
        if (allowAmt < 0) {
            item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_AmtDue_Greater'));
            return false;
        } else {
            item.grid.view.messageBar.hide();
            return true;
        }

    }
};
//Negative values and more than 100 percentage not allowed in percentage archived
OB.ESCM.QtyValidation.enterRequestedPercentage = function(item, validator, value, record) {
    var recordId = record.id
    if (value <= 0 || value == null) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_Negative_Percentage'));
        return false;
    }
    if (value > 100) {
        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('ESCM_Percentage_Greater'));
        return false;
    } else {
        return true;

    }
};

//	/*get the percentage when change the amount receive*/
OB.ESCM.QtyValidation.percentageCalculation = function(item, view, form, grid) {
    var amountReceive = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Amt'));
    var amountReceiveOrig = grid[item.rowNum + '_Amt_orig'];
    if (!amountReceiveOrig || parseFloat(amountReceive) != parseFloat(amountReceiveOrig)) {
        var priceActual = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Pendingamt'));
        var calcPercentage = ((amountReceive / priceActual) * 100);
        grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Percentagearchived'), calcPercentage);
        grid[item.rowNum + '_Percentagearchived_orig'] = OB.Utilities.Number.JSToOBMasked(calcPercentage, item.typeInstance.maskNumeric, item.typeInstance.decSeparator, item.typeInstance.groupSeparator, OB.Format.defaultGroupingSize);
        grid[item.rowNum + '_Amt_orig'] = amountReceive;
    }
}; /*get the amountReceive when change the percentage*/
OB.ESCM.QtyValidation.calAmountReceive = function(item, view, form, grid) {
    var percentageArchieved = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Percentagearchived'));
    var percentageArchievedOrig = grid[item.rowNum + '_Percentagearchived_orig'];
    if (!percentageArchievedOrig || parseFloat(percentageArchieved) != parseFloat(percentageArchievedOrig)) {
        var priceActual = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Pendingamt'));
        var calcAmt = ((percentageArchieved * priceActual) / 100);
        grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Amt'), calcAmt);
        grid[item.rowNum + '_Amt_orig'] = OB.Utilities.Number.JSToOBMasked(calcAmt, item.typeInstance.maskNumeric, item.typeInstance.decSeparator, item.typeInstance.groupSeparator, OB.Format.defaultGroupingSize);
        grid[item.rowNum + '_Percentagearchived_orig'] = percentageArchieved;
    }
};


//calculateExeStartDategregorian date
OB.ESCM.QtyValidation.calExeStartDategre = function(item, view, form, grid) {
    var recordId = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Exestartdateh'));
    var endDateH = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Exeenddateh'));
    if (recordId != null) {
        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POReceiptAddLinesAmount', {
            action: 'setexeStartDateGre',
            recordId: recordId,
            endDateH: endDateH
        }, {}, function(response, data, request) {
            grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Exestartdategre'), data.exeStartDateGre);
            grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Contractexedays'), data.executedDays);
            return true;
        });
    }
};

//calculateExeEndDategregorian date
OB.ESCM.QtyValidation.calExeEndDategre = function(item, view, form, grid) {

    var calExEndDateH = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Exeenddateh'));
    var Exestartdateh = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Exestartdateh'));
    var needbyDate = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Needbydate'));
    if (calExEndDateH != null) {
        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POReceiptAddLinesAmount', {
            action: 'setexeEndDateGre',
            calExEndDateH: calExEndDateH,
            Exestartdateh: Exestartdateh,
            needbyDate: needbyDate
        }, {}, function(response, data, request) {
            grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Exeenddategre'), data.exeEndDateGre);
            grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Contractexedays'), data.executedDays);
            grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Contractdelaydays'), data.Contractdelaydays);
            return true;
        });
    }
};
//calculate calExeStartDateH
OB.ESCM.QtyValidation.calExeStartDateH = function(item, view, form, grid) {
    var Exestartdategre = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Exestartdategre'));
    var Exeenddateh = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Exeenddateh'));
    console.log("Enter");
    var calendar = $.calendars.instance("ummalqura");
    if (Exestartdategre != null) {
        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POReceiptAddLinesAmount', {
            action: 'setexeStartDateH',
            Exestartdategre: Exestartdategre,
            Exeenddateh: Exeenddateh
        }, {}, function(response, data, request) {
            console.log("Data" + data.convertexeStartDateH);


            grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Exestartdateh'), data.convertexeStartDateH);
            //		    	 pickerDataChanged
            //		    	 grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Contractexedays'), data.executedDays);
            //		    	 grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Contractdelaydays'), data.Contractdelaydays);
            return true;
        });
    }
    return true;
};

//calculate calExeEndDateH
OB.ESCM.QtyValidation.calExeEndDateH = function(item, view, form, grid) {
    var Exeenddategre = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Exeenddategre'));

    if (Exeenddategre != null) {
        var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POReceiptAddLinesAmount', {
            action: 'setexeEndDateH',
            Exeenddategre: Exeenddategre
        }, {}, function(response, data, request) {
            grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Exeenddateh'), data.convertexeEndDateH);
            //		    	 grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Contractexedays'), data.executedDays);
            //		    	 grid.setEditValue(item.rowNum, grid.getFieldByColumnName('Contractdelaydays'), data.Contractdelaydays);
            return true;
        });
    }
};


OB.ESCM.QtyValidation.OnPRProposalQuantityLoad = function(view) {
    receiptsToAdd = [];
    receiptsTotal = 0;
    console.log(view.theForm.getItem('Lines'));
    var form = view.theForm,
        linesGrid = form.getItem('Lines').canvas.viewGrid;
    linesGrid.selectionChanged = OB.ESCM.QtyValidation.selectionPRProposalChanged;
};

OB.ESCM.QtyValidation.selectionPRProposalChanged = function(record, state) {
    this.Super('selectionChanged', arguments);
    var lines = this.view.theForm.getItem('Lines').canvas.viewGrid;

    var reqqty = lines.getFieldByColumnName('reqqty');
    //var orderQty = lines.getFieldByColumnName('escmProposalQty');
    var remqty = lines.getFieldByColumnName('quantity');
    var reqqtyvalue = lines.getEditedCell(record, reqqty);
    // var orderQtyvalue = lines.getEditedCell(record, orderQty);
    var orderQtyvalue = 0;
    if (remqty != 0) {
        var remqtyvalue = reqqtyvalue - orderQtyvalue;
        console.log(remqtyvalue);
        lines.setEditValue(lines.getRecordIndex(record), 'quantity', remqtyvalue);
    } else {
        lines.setEditValue(lines.getRecordIndex(record), 'quantity', 0);
    }
};

// If user select a record with summarylevel ='Y' then deselect it
OB.ESCM.QtyValidation.OnRecordSelection = function(view) {
    var form = view.theForm,
        linesGrid = form.getItem('Lines').canvas.viewGrid;
    linesGrid.selectionChanged = OB.ESCM.QtyValidation.selectionPOReceiptChanged;
};


OB.ESCM.QtyValidation.selectionPOReceiptChanged = function(record, state) {
    if (record.summaryLevel) {
        record.obSelected = false;
        this.Super('selectionChanged', arguments);
    } else {
        this.Super('selectionChanged', arguments);
    }
};


//If user select a record with summarylevel ='Y' then deselect it- for return po
OB.ESCM.QtyValidation.OnRecordSelectionForReturn = function(view) {
    var form = view.theForm,
        linesGrid = form.getItem('Return').canvas.viewGrid;
    linesGrid.selectionChanged = OB.ESCM.QtyValidation.selectionPOReceiptReturnChanged;
};


OB.ESCM.QtyValidation.selectionPOReceiptReturnChanged = function(record, state) {
    if (record.summaryLevel) {
        record.obSelected = false;
        this.Super('selectionChanged', arguments);
    } else {
        this.Super('selectionChanged', arguments);
    }
};
//If user select a record with summarylevel ='Y' then deselect it for PO receipt amount add lines for project receiving
OB.ESCM.QtyValidation.OnRecordSelectionPoAmtReceipt = function(view) {
    var form = view.theForm,
        linesGrid = form.getItem('Receive').canvas.viewGrid;
    linesGrid.selectionChanged = OB.ESCM.QtyValidation.selectionPOAmtReceiptChanged;
};


OB.ESCM.QtyValidation.selectionPOAmtReceiptChanged = function(record, state) {
    if (record.summary) {
        record.obSelected = false;
        this.Super('selectionChanged', arguments);
    } else {
        this.Super('selectionChanged', arguments);
    }
};
//If user select a record with summarylevel ='Y' then deselect it for PO receipt add lines for initial Receipt
OB.ESCM.QtyValidation.OnRecordSelectionPoInitialReceipt = function(view) {
    var form = view.theForm,
        linesGrid = form.getItem('Receive').canvas.viewGrid;
    linesGrid.selectionChanged = OB.ESCM.QtyValidation.selectionPOInitialReceiptChanged;
};


OB.ESCM.QtyValidation.selectionPOInitialReceiptChanged = function(record, state) {
    if (record.summary) {
        record.obSelected = false;
        this.Super('selectionChanged', arguments);
    } else {
        this.Super('selectionChanged', arguments);
    }
};

//While Selecting Parent, Child should also get selected
OB.ESCM.QtyValidation.OnSelectionForPRQty = function(grid, record, state) {
    if (state) {
        record.releaseQty = record.remainingQty;
        record.obSelected = true;
        grid.pneSelectionUpdated(record, true);
    } else {
        record.releaseQty = 0;
        record.obSelected = false;
        grid.pneSelectionUpdated(record, false);
    }

    var allRecords = (grid.data.allRows) ? grid.data.allRows.length : ((grid.data.localData) ? grid.data.localData.length : 0);
    var allRecordslength = (grid.data.allRows) ? grid.data.allRows.length : 0;
    var list = [];

    for (i = 0; i < allRecords; i++) {
        var currentRecordId = "";
        if (allRecordslength > 0) {
            currentRecordId = grid.data.allRows[i];
        } else {
            currentRecordId = grid.data.localData[i];
        }
        if (currentRecordId && currentRecordId.parentid == record.id) {
            if (state) {
                currentRecordId.obSelected = true;
                grid.pneSelectionUpdated(currentRecordId, true);
                currentRecordId.releaseQty = currentRecordId.remainingQty;
            } else {
                currentRecordId.obSelected = false;
                grid.pneSelectionUpdated(currentRecordId, false);
                currentRecordId.releaseQty = 0;
            }
            list.push(currentRecordId.id);
        }
    }
    OB.ESCM.QtyValidation.OnListIterate(list, grid, record, state);
};


OB.ESCM.QtyValidation.OnListIterate = function(list, grid, record, state) {
    var listIteration = [];
    var allRecords = (grid.data.allRows) ? grid.data.allRows.length : ((grid.data.localData) ? grid.data.localData.length : 0);
    var allRecordslength = (grid.data.allRows) ? grid.data.allRows.length : 0;
    if (list.length > 0) {
        for (i = 0; i < allRecords; i++) {
            var currentRecordId = "";
            if (allRecordslength > 0) {
                currentRecordId = grid.data.allRows[i];
            } else {
                currentRecordId = grid.data.localData[i];
            }
            if (currentRecordId && list.indexOf(currentRecordId.parentid) > -1) {
                if (state) {
                    currentRecordId.obSelected = true;
                    grid.pneSelectionUpdated(currentRecordId, true);
                    currentRecordId.releaseQty = currentRecordId.remainingQty;
                } else {
                    currentRecordId.obSelected = false;
                    grid.pneSelectionUpdated(currentRecordId, false);
                    currentRecordId.releaseQty = 0;
                }
                listIteration.push(currentRecordId.id);
            }
        }
        OB.ESCM.QtyValidation.OnListIterate(listIteration, grid, record, state);
    }
};


//While Selecting Parent, Child should also get selected - Purchase Release Amount based
OB.ESCM.QtyValidation.OnSelectionForPRAmt = function(grid, record, state) {
    if (state) {
        record.releaseamt = record.remainingamt;
        record.obSelected = true;
        grid.pneSelectionUpdated(record, true);
    } else {
        record.releaseamt = 0;
        record.obSelected = false;
        grid.pneSelectionUpdated(record, false);
    }

    var allRecords = (grid.data.allRows) ? grid.data.allRows.length : ((grid.data.localData) ? grid.data.localData.length : 0);
    var allRecordslength = (grid.data.allRows) ? grid.data.allRows.length : 0;
    var list = [];

    for (i = 0; i < allRecords; i++) {
        var currentRecordId = "";
        if (allRecordslength > 0) {
            currentRecordId = grid.data.allRows[i];
        } else {
            currentRecordId = grid.data.localData[i];
        }
        if (currentRecordId && currentRecordId.parentid == record.id) {
            if (state) {
                currentRecordId.obSelected = true;
                grid.pneSelectionUpdated(currentRecordId, true);
                currentRecordId.releaseamt = currentRecordId.remainingamt;
            } else {
                currentRecordId.obSelected = false;
                grid.pneSelectionUpdated(currentRecordId, false);
                currentRecordId.releaseamt = 0;
            }
            list.push(currentRecordId.id);
        }
    }
    OB.ESCM.QtyValidation.OnListIterateAmt(list, grid, record, state);
};


OB.ESCM.QtyValidation.OnListIterateAmt = function(list, grid, record, state) {
    var listIteration = [];
    var allRecords = (grid.data.allRows) ? grid.data.allRows.length : ((grid.data.localData) ? grid.data.localData.length : 0);
    var allRecordslength = (grid.data.allRows) ? grid.data.allRows.length : 0;
    if (list.length > 0) {
        for (i = 0; i < allRecords; i++) {
            var currentRecordId = "";
            if (allRecordslength > 0) {
                currentRecordId = grid.data.allRows[i];
            } else {
                currentRecordId = grid.data.localData[i];
            }
            if (currentRecordId && list.indexOf(currentRecordId.parentid) > -1) {
                if (state) {
                    currentRecordId.obSelected = true;
                    grid.pneSelectionUpdated(currentRecordId, true);
                    currentRecordId.releaseamt = currentRecordId.remainingamt;
                } else {
                    currentRecordId.obSelected = false;
                    grid.pneSelectionUpdated(currentRecordId, false);
                    currentRecordId.releaseamt = 0;
                }
                listIteration.push(currentRecordId.id);
            }
        }
        OB.ESCM.QtyValidation.OnListIterateAmt(listIteration, grid, record, state);
    }
};

//While Selecting Parent, Child should also get selected - Purchase Release AddLines - Quantity based
OB.ESCM.QtyValidation.OnSelectionForPRAddLnQty = function(grid, record, state) {
    if (state) {
        record.releaseQty = record.remainingQuantity;
        record.obSelected = true;
        grid.pneSelectionUpdated(record, true);
    } else {
        record.releaseQty = 0;
        record.obSelected = false;
        grid.pneSelectionUpdated(record, false);
    }
    var allRecords = (grid.data.allRows) ? grid.data.allRows.length : ((grid.data.localData) ? grid.data.localData.length : 0);
    var allRecordslength = (grid.data.allRows) ? grid.data.allRows.length : 0;
    var list = [];

    for (i = 0; i < allRecords; i++) {
        var currentRecordId = "";
        if (allRecordslength > 0) {
            currentRecordId = grid.data.allRows[i];
        } else {
            currentRecordId = grid.data.localData[i];
        }

        if (currentRecordId && currentRecordId.parentid == record.id) {
            if (state) {
                currentRecordId.obSelected = true;
                grid.pneSelectionUpdated(currentRecordId, true);
                currentRecordId.releaseQty = currentRecordId.remainingQuantity;
            } else {
                currentRecordId.obSelected = false;
                grid.pneSelectionUpdated(currentRecordId, false);
                currentRecordId.releaseQty = 0;
            }
            list.push(currentRecordId.id);
        }
    }
    OB.ESCM.QtyValidation.OnListIterateAddLnQty(list, grid, record, state);
};


OB.ESCM.QtyValidation.OnListIterateAddLnQty = function(list, grid, record, state) {
    var listIteration = [];
    var allRecords = (grid.data.allRows) ? grid.data.allRows.length : ((grid.data.localData) ? grid.data.localData.length : 0);
    var allRecordslength = (grid.data.allRows) ? grid.data.allRows.length : 0;
    if (list.length > 0) {
        for (i = 0; i < allRecords; i++) {
            var currentRecordId = "";
            if (allRecordslength > 0) {
                currentRecordId = grid.data.allRows[i];
            } else {
                currentRecordId = grid.data.localData[i];
            }
            if (currentRecordId && list.indexOf(currentRecordId.parentid) > -1) {
                if (state) {
                    currentRecordId.obSelected = true;
                    grid.pneSelectionUpdated(currentRecordId, true);
                    currentRecordId.releaseQty = currentRecordId.remainingQuantity;
                } else {
                    currentRecordId.obSelected = false;
                    grid.pneSelectionUpdated(currentRecordId, false);
                    currentRecordId.releaseQty = 0;
                }
                listIteration.push(currentRecordId.id);
            }
        }
        OB.ESCM.QtyValidation.OnListIterateAddLnQty(listIteration, grid, record, state);
    }
};



//set remaining quantity to release quantity in Purchase Release 
OB.ESCM.QtyValidation.updateReleaseQuantity = function(grid, record, state) {
    if (state) {
        record.releaseQty = record.remainingQuantity;
    }
};

OB.ESCM.QtyValidation.updateReleaseQty = function(grid, record, state) {
    if (state) {
        record.releaseQty = record.remainingQty;
    }
};

//set remaining amount to release amount in Purchase Release 
OB.ESCM.QtyValidation.updateReleaseAmount = function(grid, record, state) {
    if (state) {
        record.releaseamt = record.remainingamt;
    }
};



OB.ESCM.QtyValidation.validateReleaseQty = function(item, validator, value, record) {
    if (record.description !== record.olddescription) {
        isc.warn(OB.I18N.getLabel('ESCM_ParentNtEditable'));
        item.setValue(record.olddescription);
        return false;

    }

};

OB.ESCM.QtyValidation.onchangeDescription = function(item, view, form, grid) {
    if (item.getValue() != item.record.olddescription) {
        item.setValue(item.record.olddescription);
    }

};

// Awarding qty cannot be negative validation
OB.ESCM.QtyValidation.awardqtychange = function(item, validator, value, record) {
    if (value < 0) {
        return false;
    } else {
        return true;
    }
};