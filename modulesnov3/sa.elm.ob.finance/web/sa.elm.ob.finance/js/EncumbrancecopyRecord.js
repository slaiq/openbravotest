(function () {
	
  var buttonProps = {
      action: function(){
    	  
    	  var callback,requestparams, encumbranceId,userId, view = this.view, grid = view.viewGrid;
    	  var editRecordAfterClone=true;
    	  if (editRecordAfterClone !== false) {
    		    editRecordAfterClone = true;
    		  }
    	callback=function(ok){
    	if(ok){
    		requestparams={
    				encumbranceId:view.viewGrid.getSelectedRecord().id,
    				userId:OB.User.id,
    				clientId:OB.User.clientId
    		};
    	var  CopyRecord = function(response, data, request) {    		
    		     var recordIndex = view.viewGrid.getRecordIndex(view.viewGrid.getSelectedRecord()) + 1,
		            recordsData = view.viewGrid.getDataSource().recordsFromObjects(data)[0];
		            view.viewGrid.addToCacheData(recordsData, recordIndex);
		            view.viewGrid.scrollToRow(recordIndex);
		            view.viewGrid.markForRedraw();
		        if (view.viewGrid.getEditRow()) {
		          view.viewGrid.endEditing();
		        }
		        view.viewGrid.doSelectSingleRecord(recordIndex);
		        if (editRecordAfterClone) {
		          view.editRecord(view.viewGrid.getRecord(recordIndex), false);
		        }      
          	};
          OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.CopyEncumbranceHandler', {},
        		   requestparams, CopyRecord);	
    		} 
    		};
        isc.ask(OB.I18N.getLabel('OBUIAPP_WantToCloneRecord'), callback);
      },
      buttonType: 'efin_enccopy_record',
      prompt: OB.I18N.getLabel('efin_enccopy_record'),
      updateState: function(){
    	  var thisEnc = this;
          var view = this.view, form = view.viewForm, grid = view.viewGrid, selectedRecords = grid.getSelectedRecords();
          var tabId = "9CBD55F879EA4DCAA4E944C0B7DC03D4";
          if (selectedRecords != null && selectedRecords != -1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: grid.getSelectedRecord()!=undefined ? grid.getSelectedRecord().id : null,
                tabId: tabId
                
              }, {}, function (response, data, request) {
                if (data.IsDraft == 1) {
                	thisEnc.setDisabled(true);
                } else {
                	thisEnc.setDisabled(false);
                }
              });
            }
          if (view.isShowingForm && form.isNew) {
            this.setDisabled(true);
          } else if (view.isEditingGrid && grid.getEditForm().isNew) {
            this.setDisabled(true);
          } else {
            this.setDisabled(selectedRecords.length === 0);
          }
      }
    };
 

  // register the button for the encumbrance header tab
  // the first parameter is a unique identification so that one button can not be registered multiple times
  //OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 140, '9CBD55F879EA4DCAA4E944C0B7DC03D4');
}());