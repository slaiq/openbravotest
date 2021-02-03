OB.EFINHOLDREL = {};
OB.EFINHOLDREL.HoldAmountVal = {};

//hold validation - check entered amount more than the hold amount - released amount
OB.EFINHOLDREL.HoldAmountVal.OnChangeAmount = function (item, validator, value, record) {
  var recordId = record.id,
      holdAmt = parseFloat(record.rDVHoldAmount),
      releasedAmt = parseFloat(record.releasedAmount),
      remainingAmt = (holdAmt - releasedAmt),
      enteredAmt = record.enteredamt;

  //check value is greater than zero
  if (enteredAmt <= 0) {
    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_PenaltyRelAmtGrtZero'));
    return false;
  }
  
  // check value more than remaining amount (penalty amount -released amount)
  if ((parseFloat(remainingAmt) - parseFloat(enteredAmt)) < 0) {
    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_HoldEntAmtNotGrtThanRemAmt'));
    return false;
  } else {
    return true;
  }
};

//on load in hold release pop-up
OB.EFINHOLDREL.HoldAmountVal.onLoad = function (view) {
var form = view.theForm,
    holdGrid = form.getItem('Lines').canvas.viewGrid;
holdGrid.selectionChanged = OB.EFINHOLDREL.HoldAmountVal.selectionChanged;
};

//apply default value(hold amount - released amount) in amount field in hold release pop-up
OB.EFINHOLDREL.HoldAmountVal.selectionChanged = function (record, state) {
var holdGrid = this.view.theForm.getItem('Lines').canvas.viewGrid;
this.Super('selectionChanged', arguments);
if (arguments[0].obSelected == true) {
  tempenactIdId = arguments[0].id;
  var enteredAmt = parseFloat(arguments[0].rDVHoldAmount) - parseFloat(arguments[0].releasedAmount);
  OB.EFINHOLDREL.HoldAmountVal.readOnlyLogic(this.view.theForm, this.view, arguments[0].efinRdvtxnline, holdGrid, enteredAmt, record);
}
};

//to apply the read only logic for Amount field in hold release pop-up
OB.EFINHOLDREL.HoldAmountVal.readOnlyLogic = function (form, view, rdvTxnLineId, holdGrid, enteredAmt, record) {
callbackReadOnlyLogic = function (response, data, request) {
var amountField = holdGrid.getFieldByColumnName('Enteredamt'),
    selectedRecords = holdGrid.getSelectedRecords();
for (i = 0; i < selectedRecords.length; i++) {
  if (data.status != 'DR' && data.status != 'REJ') {
    amountField.disabled = true;
    holdGrid.setEditValue(holdGrid.getRecordIndex(record), 'enteredamt', Number(0));
  } else {
    amountField.disabled = false;
    holdGrid.setEditValue(holdGrid.getRecordIndex(record), 'enteredamt', Number(enteredAmt));
  }
}    
};


//call back function to get the RDV transaction version status
  OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PenaltyReleaseReadonlyActionHandler', {
    rdvTxnlineId: rdvTxnLineId
  }, {}, callbackReadOnlyLogic);
};