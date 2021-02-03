OB.EFINPENREL = {};
OB.EFINPENREL.PenaltyAmtVal = {};
//penalty validation - check entered amount more than the penalty amount - released amount
OB.EFINPENREL.PenaltyAmtVal.OnChangeAmount = function (item, validator, value, record) {
  var recordId = record.id,
      penaltyAmt = parseFloat(record.penaltyAmount),
      releasedAmt = parseFloat(record.releasedamt),
      remainingAmt = (penaltyAmt - releasedAmt).toFixed(2),
      enteredAmt = value;
  //check value is greater than zero
  if (value <= 0) {
    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_PenaltyRelAmtGrtZero'));
    return false;
  }
  // check value more than remaining amount (penalty amount -released amount)
  if ((parseFloat(remainingAmt) - parseFloat(enteredAmt)) < 0) {
    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_PenaltyEntAmtNotGrtThanRemAmt'));
    return false;
  } else {
    return true;
  }
};

//on load in penalty release popup
OB.EFINPENREL.PenaltyAmtVal.onLoad = function (view) {
  var form = view.theForm,
      penaltyGrid = form.getItem('Lines').canvas.viewGrid;
  penaltyGrid.selectionChanged = OB.EFINPENREL.PenaltyAmtVal.selectionChanged;
};
//apply default value(penalty amount - released amount) in amount field in penalty release popup
OB.EFINPENREL.PenaltyAmtVal.selectionChanged = function (record, state) {
  var penaltyGrid = this.view.theForm.getItem('Lines').canvas.viewGrid;
  this.Super('selectionChanged', arguments);
  if (arguments[0].obSelected == true) {
    tempenactIdId = arguments[0].id;
    var enteredAmt = parseFloat(arguments[0].penaltyAmount) - parseFloat(arguments[0].releasedamt);
    OB.EFINPENREL.PenaltyAmtVal.readOnlyLogic(this.view.theForm, this.view, arguments[0].efinRdvtxnline, penaltyGrid, enteredAmt, record);
  }
};

// to apply the read only logic for Amount field in penalty release popup
OB.EFINPENREL.PenaltyAmtVal.readOnlyLogic = function (form, view, rdvTxnLineId, penaltyGrid, enteredAmt, record) {
  callbackReadOnlyLogic = function (response, data, request) {
    var amountField = penaltyGrid.getFieldByColumnName('Enteredamt'),
        selectedRecords = penaltyGrid.getSelectedRecords();
    for (i = 0; i < selectedRecords.length; i++) {
      if (data.status != 'DR' && data.status != 'REJ') {
        amountField.disabled = true;
        penaltyGrid.setEditValue(penaltyGrid.getRecordIndex(record), 'enteredamt', Number(0));
      } else {
        amountField.disabled = false;
        penaltyGrid.setEditValue(penaltyGrid.getRecordIndex(record), 'enteredamt', Number(enteredAmt));
      }
    }

  };
  //call back function to get the rdv transaction version status
  OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PenaltyReleaseReadonlyActionHandler', {
    rdvTxnlineId: rdvTxnLineId
  }, {}, callbackReadOnlyLogic);
};