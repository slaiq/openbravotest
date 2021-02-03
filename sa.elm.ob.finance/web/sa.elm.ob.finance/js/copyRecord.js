(function () {
  var buttonProps = {
      action: function(){
    	  var callback,requestparams, journalId,userId, view = this.view, grid = view.viewGrid;
    	  var editRecordAfterClone=true;
    	  if (editRecordAfterClone !== false) {
    		    editRecordAfterClone = true;
    		  }
    	callback=function(ok){
    	if(ok){
    		requestparams={
    				journalId:view.viewGrid.getSelectedRecord().id,
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
          OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.CopySimpleGLJournalHandler', {},
        		   requestparams, CopyRecord);	
    		} 
    		};
        isc.ask(OB.I18N.getLabel('OBUIAPP_WantToCloneRecord'), callback);
      },
      buttonType: 'efin_copy_record',
      prompt: OB.I18N.getLabel('EFIN_Copy_Record'),
      updateState: function(){
          var view = this.view, form = view.viewForm, grid = view.viewGrid, selectedRecords = grid.getSelectedRecords();
          if (view.isShowingForm && form.isNew) {
            this.setDisabled(true);
          } else if (view.isEditingGrid && grid.getEditForm().isNew) {
            this.setDisabled(true);
          } else {
            this.setDisabled(selectedRecords.length === 0);
          }
      }
    };
  
  // register the button for the simple g/l journal header tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 100, '5A6F0ED7230C462BA4010653BA3F816A');
}());