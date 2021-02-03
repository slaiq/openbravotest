/**
 * divya.j on 02-12-2019
 */

OB.EFINPOHOLDPLANREL = {};
OB.EFINPOHOLDPLANREL.ReleaseAmtVal = {};

OB.EFINPOHOLDPLANREL.ReleaseAmtVal.OnChangeAmount = function (item, validator, value, record) {
	  var recordId = record.id,
	      holdAmt = parseFloat(record.holdAmount),
	      releasedAmt = parseFloat(record.releaseAmount),
	      transferAmt=  parseFloat(record.budgTransferamt),
	      remainingAmt = (holdAmt - releasedAmt-transferAmt).toFixed(2),
	      //remainingAmt = (holdAmt - releasedAmt).toFixed(2),
	      enteredAmt = value;
	  errorflag = false;
	  //check value is greater than zero
	  if (value <= 0) {
	    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_PenaltyRelAmtGrtZero'));
	    errorflag=true;
	  }
	  // check value more than remaining amount (hold amount -released amount- transfer amount)
	  if ((parseFloat(remainingAmt) - parseFloat(enteredAmt)) < 0) {
	    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_HoldEntAmtNotGrtThanRemAmt'));
	    errorflag=true;
	  }
	  
	  if (recordId != null) {
		    var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.POHoldPlanReleaseHandler', {
		      recordId: recordId,
		    }, {}, function (response, data, request) {

		      if (value > data.pendingQty) {
		    	  errorflag=true;
		        item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_HoldEntAmtNotGrtThanRemAmt'));
		      }
		    });
		  }
	  
	  if (errorflag) {
		  return false;
	  }
	  else {
	    return true;
	  }
	};
	
	
	//on load in po hold plan release popup
	OB.EFINPOHOLDPLANREL.ReleaseAmtVal.onLoad = function (view) {
	  var form = view.theForm,
	      holdGrid = form.getItem('Release').canvas.viewGrid;
	  holdGrid.selectionChanged = OB.EFINPOHOLDPLANREL.ReleaseAmtVal.selectionChanged;
	};
	//apply default value(hold amount - released amount- transfer amt) in amount field in po hold plan release popup
	OB.EFINPOHOLDPLANREL.ReleaseAmtVal.selectionChanged = function (record, state) {
	  var holdGrid = this.view.theForm.getItem('Release').canvas.viewGrid;
	  this.Super('selectionChanged', arguments);
	  if (arguments[0].obSelected == true) {
	    tempenactIdId = arguments[0].id;
	    var enteredAmt = parseFloat(arguments[0].holdAmt) - parseFloat(arguments[0].releasedAmt);
	    OB.EFINPOHOLDPLANREL.ReleaseAmtVal.readOnlyLogic(this.view.theForm, this.view, arguments[0].efinRdvBudgholdline, holdGrid, enteredAmt, record);
	  }
	};
	// to apply the read only logic for Amount field in po hold plan release popup
	OB.EFINPOHOLDPLANREL.ReleaseAmtVal.readOnlyLogic = function (form, view, rdvBudgetHoldLine, holdGrid, enteredAmt, record) {
	    var amountField = holdGrid.getFieldByColumnName('enteredAmount'),
	    holdAmt = parseFloat(record.holdAmount),
	      releasedAmt = parseFloat(record.releaseAmount),
	      transferAmt=  parseFloat(record.budgTransferamt),
	      remainingAmt = (holdAmt - releasedAmt-transferAmt),
	      //remainingAmt = (holdAmt - releasedAmt),
	        selectedRecords = holdGrid.getSelectedRecords();
	    for (i = 0; i < selectedRecords.length; i++) {
	        holdGrid.setEditValue(holdGrid.getRecordIndex(record), 'enteredAmount',  remainingAmt);
	    }
	  };
	  OB.EFINPOHOLDPLANREL.ReleaseAmtVal.ReleaseRevertChangeRelAmount = function (item, validator, value, record) {
		  var recordId = record.id,
		      releaseAmt = Math.abs(record.rDVHoldAmount),
		      enteredAmt = value;
		  errorflag = false;
		  //check value is greater than zero
		  if (value <= 0) {
		    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_PenaltyRelAmtGrtZero'));
		    errorflag=true;
		  }
		  // check value more than remaining amount (hold amount - transfer amount)
		  if ((parseFloat(releaseAmt) - parseFloat(enteredAmt)) < 0) { 
		    item.grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, OB.I18N.getLabel('EFIN_HoldRelEntAmtNotGrtThanRemRelAmt'));
		    errorflag=true;
		  }
		  if (errorflag) {
			  return false;
		  }
		  else {
		    return true;
		  }
		};
		