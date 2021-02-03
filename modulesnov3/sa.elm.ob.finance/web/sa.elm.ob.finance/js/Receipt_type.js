OB.EFINRec = OB.EFINRec || {};
OB.EFINRec.OnChangeFunctions = OB.EFINRec.OnChangeFunctions || {};

OB.EFINRec.OnChangeFunctions.orderToReceiveLine_receiptType = function (item, view, form, grid) {
  //set empty values.
  form.setItemValue('eFINPrepayment', null);
  form.setItemValue('efinCValidcombination', null);
}




//On change function for Payment sequence window - Header tab - fields name[save payment, apply payment]
OB.EFINRec.OnChangeFunctions.savePayment = function (item, view, form, grid) {
  // set value for apply
  if (form.getItem('savePayment').getValue()) {
    form.setItemValue('apply', false);
  } else {
    form.setItemValue('apply', true);
  }
}

OB.EFINRec.OnChangeFunctions.applyPayment = function (item, view, form, grid) {
  if (form.getItem('apply').getValue()) {
    form.setItemValue('savePayment', false);
  } else {
    form.setItemValue('savePayment', true);
  }
}


OB.EFINRec.OnChangeFunctions.parentFrom = function (item, view, form, grid) {
  if (form.getItem('fINFrom').getValue() !== null) {
    form.setItemValue('fINFrom', Math.round(Math.abs(form.getItem('fINFrom').getValue())));
  } else {
    form.setItemValue('fINFrom', form.getItem('fINTo').getValue());
  }


}

OB.EFINRec.OnChangeFunctions.parentTo = function (item, view, form, grid) {
  if (form.getItem('fINTo').getValue() !== null) {
    form.setItemValue('fINTo', Math.round(Math.abs(form.getItem('fINTo').getValue())));
  } else {
    form.setItemValue('fINTo', form.getItem('fINFrom').getValue());
  }
}



OB.EFINRec.OnChangeFunctions.childFrom = function (item, view, form, grid) {
  if (form.getItem('fINFrom').getValue() !== null) {
    form.setItemValue('fINFrom', Math.round(Math.abs(form.getItem('fINFrom').getValue())));
  } else {
    form.setItemValue('fINFrom', form.getItem('fINTo').getValue());
  }


}

OB.EFINRec.OnChangeFunctions.childTo = function (item, view, form, grid) {
  if (form.getItem('fINTo').getValue() !== null) {
    form.setItemValue('fINTo', Math.round(Math.abs(form.getItem('fINTo').getValue())));
  } else {
    form.setItemValue('fINTo', form.getItem('fINFrom').getValue());
  }
}


OB.EFINRec.OnChangeFunctions.ClientSideValidation = function (view, actionHandlerCall, failureCallback) {
  var form = view.theForm;
  if (form.getItem('From').getValue() > form.getItem('To').getValue()) {
    view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('Efin_seq_fromnotgreater'));
    return failureCallback();
  } else {
    actionHandlerCall();
  }
}