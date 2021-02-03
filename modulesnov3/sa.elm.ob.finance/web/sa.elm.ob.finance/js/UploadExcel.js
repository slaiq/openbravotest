(function () {
	var view;
	var grid;
	var recordId;
	var tabId;
	var docs = [];
	var buttonProps = {
		action: function(){
			var callback, i; 
			
			view = this.view;
			grid = view.viewGrid;
			//recordId = view.viewGrid.getSelectedRecord().id;	
			//var selectedRecords = grid.getSelectedRecords();			
			tabId = view['tabId'];	
			var windowId = view['windowId'];	
					              
            /*// collect the docs ids
            for (i = 0; i < selectedRecords.length; i++) {
            	docs.push(selectedRecords[i].id);            	
            }*/
            openUploadDialog(tabId, windowId, docs);              
      },
      buttonType: 'efin_upload_excel',
      prompt: OB.I18N.getLabel('EFIN_UploadExcel'),
      updateState: function(){
          var view = this.view, form = view.viewForm, grid = view.viewGrid;
          //var selectedRecords = grid.getSelectedRecords();
          /*if (view.isShowingForm && form.isNew) {
            this.setDisabled(true);
          } else if (view.isEditingGrid && grid.getEditForm().isNew) {
            this.setDisabled(true);
          } else {
            this.setDisabled(selectedRecords.length === 0);
          }*/
      }
    };
	
	function openUploadDialog(tabId, windowId, docs) {	 
 		var action="";	
 		OB.Layout.ClassicOBCompatibility.Popup.open('FIN_Payment', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.finance.ad_process.paymentoutmof/UploadMOF") + "?Command=" +"DEFAULT"+"&inpRecordId="+recordId+"&inpTabId="+tabId+"&inpWindowID="+windowId+"&pageType=WAD", '', null, false, false, true);		
 	}
  // register the button for the payment out tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 160, 'F7A52FDAAA0346EFA07D53C125B40404');
}());