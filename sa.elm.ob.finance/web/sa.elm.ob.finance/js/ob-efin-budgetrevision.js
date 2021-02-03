OB.EFIN = {};
OB.EFIN.OnChangeFunction = {};

OB.EFIN.OnChangeFunction.transactionTypeOnChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria; //newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}
OB.EFIN.OnChangeFunction.BudgetTypeOnChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}
OB.EFIN.OnChangeFunction.YearOnChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}
OB.EFIN.OnChangeFunction.FromAcctOnChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}
OB.EFIN.OnChangeFunction.ToAcctOnChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}

OB.EFIN.OnChangeFunction.onRefresh = function (view) {
  var grid = view.theForm.getItem('line').canvas.viewGrid,
      newCriteria = {};
  newCriteria.criteria = [];
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  grid.invalidateCache();
  view.theForm.redraw();
};


OB.EFIN.OnChangeFunction.onLoad = function (grid) {
  var allRecords = (grid.data.allRows) ? grid.data.allRows.length : 0;
  for (i = 0; i < allRecords; i++) {
    var record = grid[i];
    var acctdate = grid.getFieldByColumnName('acctDate');
    var acctdat = grid.getEditedCell(i, acctdate);
    var calendar = $.calendars.instance("ummalqura");
    var cal = calendar.newDate().fromJSDate(acctdat);
    var formattedDate = calendar.formatDate('dd-mm-yyyy', cal);
    grid.setEditValue((i), acctdate, formattedDate + "");
    var editedRecord = isc.addProperties({}, record);
    var ids = OB.EFIN.OnChangeFunction.ids(record.id);
    record.id = ids;
    grid.selectedIds.push(record.id);
    grid.data.savedData.push(editedRecord);
  }

};


OB.EFIN.OnChangeFunction.ids = function (val) {
  var valArray = val.replaceAll(' ', '').split(',').sort(),
      retVal, length;

  valArray = valArray.filter(function (elem, pos, self) {
    return self.indexOf(elem) === pos;
  });

  retVal = valArray.toString().replaceAll(',', ', ');
  return retVal;
};


OB.EFIN.OnChangeFunction.onLoadModication = function (view) {
  var form = view.theForm,
      revisionDate = form.getItem('Efin_Revison_Date').getValue(),
      calendar = $.calendars.instance("ummalqura"),
      currentHijriDate = calendar.newDate().fromJSDate(new Date()),
      formattedDate = calendar.formatDate('dd-mm-yyyy', currentHijriDate);
  form.getItem('Efin_Revison_Date').setValue(formattedDate + "");
};

OB.EFIN.OnChangeFunction.changeInc = function (item, view, form, grid) {

  if (item.getValue() > 0) {
    grid.setEditValue(item.rowNum, 'rEVDecrease', Number('0'));
  }

  if (item.getValue() < 0) {
    isc.warn(OB.I18N.getLabel('Efin_amountlesszero'));
    grid.setEditValue(item.rowNum, 'refamount', Number('0'));
  }

};


OB.EFIN.OnChangeFunction.changeDec = function (item, view, form, grid) {

  if (item.getValue() > 0) {
    grid.setEditValue(item.rowNum, 'refamount', Number('0'));
  }

  if (item.getValue() < 0) {
    isc.warn(OB.I18N.getLabel('Efin_amountlesszero'));
    grid.setEditValue(item.rowNum, 'rEVDecrease', Number('0'));
  }

};

OB.EFIN.OnChangeFunction.changePOUniquecode = function (item, view, form, grid) {
  form.getItem('efinBudgetManencum').setValue('');
  if (form.getItem('eFINUniqueCode')) {
    form.getItem('eFINUniqueCode').setValue('');
  }
};

OB.EFIN.OnChangeFunction.changePRUniquecode = function (item, view, form, grid) {
  form.getItem('efinBudgetManencum').setValue('');
//  if(form.getItem('escmManualEncumNo')){
//  form.getItem('escmManualEncumNo').setValue('');
//  }
  if (form.getItem('eFINUniqueCode')) {
    form.getItem('eFINUniqueCode').setValue('');
  }
};
OB.EFIN.OnChangeFunction.changeProposalUniquecode = function (item, view, form, grid) {
  if (form.getItem('efinEncumbrance')) {
    form.getItem('efinEncumbrance').setValue('');
  }
  if (form.getItem('eFINUniqueCode')) {
    form.getItem('eFINUniqueCode').setValue('');
  }
};
OB.EFIN.OnChangeFunction.changeProposalAttrEncumMethod = function (item, view, form, grid) {
	  if (form.getItem('eFINManualEncumbrance')) {
	    form.getItem('eFINManualEncumbrance').setValue('');
	  }
	  if (form.getItem('eFINUniqueCode')) {
	    form.getItem('eFINUniqueCode').setValue('');
	  }
	};
OB.EFIN.OnChangeFunction.skipEncumbrance = function (item, view, form, grid) {
  if (form.getItem('efinSkipencumbrance') && form.getItem('efinSkipencumbrance').getValue()) {
  //  form.getItem('escmManualEncumNo').setValue('');
    form.getItem('eFINUniqueCode').setValue('');
    form.getItem('efinBudgetManencum').setValue('');
    
    form.getFieldFromColumnName('EM_Efin_Encum_Method').hide();
    form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').hide();
    form.getFieldFromColumnName('EM_Efin_Budget_Manencum_ID').hide();
    form.getItem('efinEncumMethod').setValue('A');

  } else {
    form.getFieldFromColumnName('EM_Efin_Encum_Method').show();
    form.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').show();
    form.getFieldFromColumnName('EM_Efin_Budget_Manencum_ID').show();
  }
};

OB.EFIN.OnChangeFunction.changeProposalEncumbrance = function (item, view, form, grid) {
//  if (form.getItem('eFINUniqueCode')) {
//    form.getItem('eFINUniqueCode').setValue('');
//  }
};



OB.EFIN.OnChangeFunction.invoiceamtchange = function (item, view, form, grid) {
	form.getItem('lineNetAmount').setValue(item.getValue());
};

OB.EFIN.OnChangeFunction.getFundsAvailable = function (item, view, form, grid) {
	var tabId="",recordId="";
	tabId=form.paramWindow.parentWindow.activeView.tabId;
	if(tabId=="A2E25351FBFF41CB949EDF35DE875B73")
		recordId=form.paramWindow.parentWindow.activeView.getParentId();
	else
		recordId=form.paramWindow.parentWindow.activeView.viewGrid.getSelectedRecord().id;
	
	  var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.GetFundsAvailable', {
	    	action: 'getfundsAvailable',
	    	combinationId: form.getItem("C_Validcombination_ID").getValue(),
	    	targetRecordId:recordId
	    }, {}, function (response, data, request) {
	    	form.getItem('Funds_Available').setValue(data.funds_available);
	    	 return true;
	    });
	  

	};

	
	OB.EFIN.OnChangeFunction.compareAmtAndFA = function (item, view, form, grid) {
		var funsavailable=parseFloat(form.getItem("Funds_Available").getValue());
		var amount =parseFloat(form.getItem("Amount").getValue());
		if(amount<=0){
			view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_PenaltyRelAmtGrtZero'));
		    return false;
		}
		if(funsavailable<amount){
			view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('Efin_Encum_Amt_Error'));
		    return false;
		}
		};
		
		
		