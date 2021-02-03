var lastRecordId = "", Status = "",count, tabId, flag = 0;
isc.OBToolbar.addClassProperties({
	BUTTON_PROPERTIES : {
		'newDoc' : {
			updateState : function() {
				var view = this.view, tabId = view['tabId'];
				form = view.viewForm, grid = view.viewGrid;
				if (view.isShowingForm) {
			        this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
			      } else {
			        this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
			      }
				if(tabId==='FF808181309036230130905C67210034'){
    				var recordId = view.parentRecordId;
  				if(recordId != null){
   				var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PaymentOutDiableProcess', {
   					recordId : recordId
   				}, {}, callbackprint);
   				if (count > 0) {
   					this.setDisabled(true);
   				}
   				}
    			}
				// Disable new button
				//tab basic discounts in purchase invoice
				if(tabId==='800211'){
    				var recordId = view.parentRecordId;
  				if(recordId != null){
   				var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoiceNewButtonDisableProcess', {
   					recordId : recordId
   				}, {}, callbackprintinvoice);
   				if (count > 0) {
   					this.setDisabled(true);
   				}
   				}
    			}
				//tab reversed invoice in purchase invoice
				if(tabId==='3ED38B380CD849B38F0AC1B52F992C34'){
    				var recordId = view.parentRecordId;
  				if(recordId != null){
   				var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoiceNewButtonDisableProcess', {
   					recordId : recordId
   				}, {}, callbackprintinvoice);
   				if (count > 0) {
   					this.setDisabled(true);
   				}
   				}
    			}
				//tab exchange rates in purchase invoice
				if(tabId==='FF808181308EA4230130901AB2C60090'){
    				var recordId = view.parentRecordId;
  				if(recordId != null){
   				var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoiceNewButtonDisableProcess', {
   					recordId : recordId
   				}, {}, callbackprintinvoice);
   				if (count > 0) {
   					this.setDisabled(true);
   				}
   				}
    			}
				//tab Applied Prepayment in purchase invoice
				if(tabId==='D6998CA8A80C444596119468D59635EB'){
    				var recordId = view.parentRecordId;
  				if(recordId != null){
   				var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoiceNewButtonDisableProcess', {
   					recordId : recordId
   				}, {}, callbackprintinvoice);
   				if (count > 0) {
   					this.setDisabled(true);
   				}
   				}
    			}
				
		}
	},
    'newRow' : {
    	updateState : function() {
			var view = this.view, tabId = view['tabId'];
			form = view.viewForm, grid = view.viewGrid;
			if (view.isShowingForm) {
		        this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
		      } else {
		        this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
		      }
		// Disable Insert Row button
		//tab basic discounts in purchase invoice
		if(tabId==='800211'){
			var recordId = view.parentRecordId;
			if(recordId != null){
			var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoiceNewButtonDisableProcess', {
				recordId : recordId
			}, {}, callbackprintinvoice);
			if (count > 0) {
				this.setDisabled(true);
			}
			}
		}
		//tab reversed invoice in purchase invoice
		if(tabId==='3ED38B380CD849B38F0AC1B52F992C34'){
			var recordId = view.parentRecordId;
			if(recordId != null){
			var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoiceNewButtonDisableProcess', {
				recordId : recordId
			}, {}, callbackprintinvoice);
			if (count > 0) {
				this.setDisabled(true);
			}
			}
		}
		//tab exchange rates in purchase invoice
		if(tabId==='FF808181308EA4230130901AB2C60090'){
			var recordId = view.parentRecordId;
			if(recordId != null){
			var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoiceNewButtonDisableProcess', {
				recordId : recordId
			}, {}, callbackprintinvoice);
			if (count > 0) {
				this.setDisabled(true);
			}
			}
		}
		//tab Applied Prepayment in purchase invoice
		if(tabId==='D6998CA8A80C444596119468D59635EB'){
			var recordId = view.parentRecordId;
			if(recordId != null){
			var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoiceNewButtonDisableProcess', {
				recordId : recordId
			}, {}, callbackprintinvoice);
			if (count > 0) {
				this.setDisabled(true);
			}
			}
		}
    }
	}
	}
});


var callbackprint = function(response, data, request) {
  	count = data.payCount;
   	return count;
    };
    
var callbackprintinvoice = function(response, data, request) {
    count = data.InvoiceCount;
    return count;
    };

