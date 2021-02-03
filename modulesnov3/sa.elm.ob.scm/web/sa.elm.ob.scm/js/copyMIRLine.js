(function () {
  var buttonProps = {
      action: function(){
    	  var callback,requestparams, mirLineId,userId, view = this.view, grid = view.viewGrid;
    	  var editRecordAfterClone=true;
    	  if (editRecordAfterClone !== false) {
    		    editRecordAfterClone = true;
    		  }
    	callback=function(ok){
    	if(ok){
    		requestparams={
    				mirLineId:view.viewGrid.getSelectedRecord().id,
    				userId:OB.User.id,
    				clientId:OB.User.clientId
    		};
    	var  CopyMIRRecord = function(response, data, request) {

    		        var recordIndex = view.viewGrid.getRecordIndex(view.viewGrid.getSelectedRecord()) + 1,
		            recordsData = view.viewGrid.getDataSource().recordsFromObjects(data)[0];
		            view.viewGrid.addToCacheData(recordsData, recordIndex);
		            view.viewGrid.scrollToRow(recordIndex);
		            view.viewGrid.markForRedraw();
		        if (view.viewGrid.getEditRow()) {
		          view.viewGrid.endEditing();
		        }
		        view.viewGrid.doSelectSingleRecord(recordIndex);
	            view.viewGrid.refreshGrid();
          	};
          OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CopyMIRLineHandler', {},
        		   requestparams, CopyMIRRecord);	
    		} 
    		};
        isc.ask(OB.I18N.getLabel('OBUIAPP_WantToCloneRecord'), callback);
      },
      buttonType: 'escm_copy_MIR_Line',
      prompt: OB.I18N.getLabel('ESCM_Copy_MIR_Line'),
      updateState: function(){
    	  var thisWin = this;
          var view = this.view, form = view.viewForm, grid = view.viewGrid, selectedRecords = grid.getSelectedRecords();
          var tabId = "4AB913F4E6064ED1833ED08A8B7FA2D5";
         if (selectedRecords != null && selectedRecords != -1 && selectedRecords.length > 0) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: selectedRecords[0].id,
                tabId: tabId
              }, {}, function (response, data, request) {
                if (data.IsDraft == 1) {
                	thisWin.setDisabled(true);
                } else {
                	thisWin.setDisabled(false);
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
  
  // register the button for the sales order tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 100, '4AB913F4E6064ED1833ED08A8B7FA2D5');
}());