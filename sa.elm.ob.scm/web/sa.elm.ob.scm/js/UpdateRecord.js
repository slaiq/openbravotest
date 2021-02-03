(function () {
  var view;
  var grid;
  var recordId;
  var tabId, windowId;

  var buttonProps = {
    action: function () {
      var callback;

      view = this.view;
      grid = view.viewGrid;

      windowId = view['windowId'];
      tabId = view['tabId'];      
      recordId = view.viewGrid.getSelectedRecord().id;         
      var warehouseId = view.viewGrid.getSelectedRecord().warehouse;
      callback=function(ok){
      	if(ok){
      		var a = OB.RemoteCallManager.call('sa.elm.ob.scm.ad_process.IssueRequest.UpdateRecord', {
          			recordId:view.viewGrid.getSelectedRecord().id,
    				warehouseId:warehouseId,
    				type:'updateqty'
          }, {}, function (response, data, request) {
            	if (data.update == true) {
            		OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewGrid.refreshGrid();
                    grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, OB.I18N.getLabel('ESCM_QtyUpdatedSuccess'));
                } else{
                	grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_QtyNotUpdated'));  
                }            	
          });
      	}      	
      } 
      isc.ask(OB.I18N.getLabel('ESCM_UpdateQty'), callback);
    },
    buttonType: 'escm_update',
    prompt: OB.I18N.getLabel('ESCM_UpdateRecord'),
    updateState: function () {
      var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid,
          selectedRecords = grid.getSelectedRecords(),
      thisdel = this;
      if (view.isShowingForm && form.isNew) {
    	  thisdel.setDisabled(true);
      } else if (view.isEditingGrid && grid.getEditForm().isNew) {
    	  thisdel.setDisabled(true);
      } else {
        //this.setDisabled(selectedRecords.length === 0);    	  
    	  if (selectedRecords.length === 1) {    		  
              var a = OB.RemoteCallManager.call('sa.elm.ob.scm.ad_process.IssueRequest.UpdateRecord', {
            	  roleId: OB.User.roleId,
            	  clientId: OB.User.clientId,            	  
            	  orgId: view.viewGrid.getSelectedRecord().organization,
            	  userId: OB.User.id, 
            	  mirId: view.viewGrid.getSelectedRecord().id,
                  type:'checkaccess'
              }, {}, function (response, data, request) {
                if (data.hasAccess) {
                	thisdel.setDisabled(false);
                } else {
                	thisdel.setDisabled(true);
                }
              });
            }else{
            	thisdel.setDisabled(true);
            }    	  
      }
    }
  };
  // register the Material Issue request Tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 160, ['CE947EDC9B174248883292F17F03BB32']);
}());