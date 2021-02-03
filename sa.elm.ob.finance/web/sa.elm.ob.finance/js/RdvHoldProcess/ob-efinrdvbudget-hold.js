OB.EFINHOLDACTION = {};
OB.EFINHOLDACTION.BUDGREV = {};

OB.EFINHOLDACTION.BUDGREV.EnterAmount = function (item, validator, value, record) {
	 var recordId = record.id
     varReqAmt = parseFloat(record.remainamt);
    if (value <= 0 || value == null) {
    		item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_PenaltyRelAmtGrtZero'));
    		return false;
    }
     if(recordId != null){
    	  var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.POHoldPlanReleaseHandler', {
		      recordId: record.efinRdvBudgholdline,
		    }, {}, function (response, data, request) {

		      if (value > data.pendingQty) {
		        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('Efin_BudgetHold_Amount'));
		        return false;
		      }
		    });
    }
    	var allowAmt = varReqAmt - value;
    	if (allowAmt < 0) {
    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('Efin_BudgetHold_Amount'));
    return false;
    	} else {
    item.grid.view.messageBar.hide();
    return true;
  }


};


//on load in po hold plan release popup
OB.EFINHOLDACTION.BUDGREV.onLoad = function (view) {
  var form = view.theForm,
      holdGrid = form.getItem('Revision').canvas.viewGrid;
  holdGrid.selectionChanged = OB.EFINHOLDACTION.BUDGREV.selectionChanged;
};
//apply default value(hold amount - released amount- transfer amt) in amount field in po hold plan release popup
OB.EFINHOLDACTION.BUDGREV.selectionChanged = function (record, state) {
  var holdGrid = this.view.theForm.getItem('Revision').canvas.viewGrid;
  this.Super('selectionChanged', arguments);
  if (arguments[0].obSelected == true) {
    tempenactIdId = arguments[0].id;
    var remainingAmt = parseFloat(arguments[0].remainamt).toFixed(2);
    OB.EFINHOLDACTION.BUDGREV.readOnlyLogic(this.view.theForm, this.view, arguments[0].efinRdvBudgholdline, holdGrid, remainingAmt, record);
  }
};
// to apply the read only logic for Amount field in po hold plan release popup
OB.EFINHOLDACTION.BUDGREV.readOnlyLogic = function (form, view, rdvBudgetHoldLine, holdGrid, remainingAmt, record) {
    var  selectedRecords = holdGrid.getSelectedRecords();
    for (i = 0; i < selectedRecords.length; i++) {
        holdGrid.setEditValue(holdGrid.getRecordIndex(record), 'amount',  remainingAmt);
    }

  };