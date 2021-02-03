/*
 * Contributor: Priyanka Ranjan
 ************************************************************************
 */

(function () {
  var ReqUpdateState = false;
  var buttonProps = {
    action: function () {
      var view = this.view,
          grid = view.viewGrid,      
      currentGrid, selectedRecords;
      windowId = view['windowId'];
      var tabId1 = view['tabId'];
      var ids = [];
      if (view.isShowingTree) {
        currentGrid = view.treeGrid;
      } else {
        currentGrid = view.viewGrid;
      }
      //Header - Get headerId
      if (tabId == '62248BBBCF644C18A75B92AD8E50238C') {
        ids[0] = view.viewGrid.getSelectedRecord().id;
      } //Lines - Get Selected Lines 
      else if (tabId == '8F35A05BFBB34C34A80E9DEF769613F7') {
        selectedRecords = currentGrid.getSelectedRecords();
        length = selectedRecords.length;
        for (i = 0; i < length; i++) {
          ids[i] = selectedRecords[i].id;
        }
      }
      // after click on applyUniqueCode icon save the record (autosave)
      this.view.viewForm.autoSave();
      var callback = function (ok) {
          var requestParams;

          if (ok) {
            requestParams = {
              POId: JSON.stringify(ids),
              tabId: tabId1,
              Type:"PO"
            };
            var EscmAutoCopy = function (response, data, request) {
                if (data.Message == "Success") {
                	 if (currentGrid.view.getParentId() === undefined) {
                         currentGrid.view.refreshCurrentRecord()
                         currentGrid.view.refreshChildViews();
                         if (currentGrid.view.childTabSet.tabs[0].pane.viewGrid.view.isShowingTree) {
                           currentGrid.view.childTabSet.tabs[0].pane.viewGrid.view.refresh()
                         }
                       } else {
                         currentGrid.view.refresh();
                         currentGrid.view.refreshParentRecord();
                       }
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, OB.I18N.getLabel('Escm_Distribute_Success'));
                } else if (data.Message == "NoLines") {
                  grid.view.messageBar.setMessage(
                  isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('Escm_No_Line_Distribution_PR'));
                } else if (data.Message == "NoUniqueCode") {
                    grid.view.messageBar.setMessage(
                            isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_No_UniqueCode_Selected'));
                } else if (data.Message == "QtyIsZero") {
                    grid.view.messageBar.setMessage(
                            isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_QtyCantBeZeroForLines'));
                } else {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ProcessFailed'));
                }
                };
            OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CopyUniqueCode', {
              action: 'setDistributeAll'
            }, requestParams, EscmAutoCopy);
          }
          };
      isc.ask(OB.I18N.getLabel('Escm_WantToDistribute'), callback);
    },
    updateState: function () {
      if (ReqUpdateState == true) {
        return false;
      }
      var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid,
      selectedRecords = grid.getSelectedRecords();
      windowId = view['windowId'];
      tabId = view['tabId'];
      var me = this,
          disable = false;
      if (view.isShowingForm && form.isNew) {
        disable = true;
      } else if (view.isEditingGrid && grid.getEditForm().isNew) {
        disable = true;
      } else {
        if (selectedRecords.length === 0) {
          disable = true;
        } else {
          disable = false;
        }

      }
      me.setDisabled(disable);
      ReqUpdateState = true;
      setTimeout(function () {
    	  ReqUpdateState = false;
      }, 300);
      var requestParams;
      var orderId = "";
      var appStatus = "";
      var isEncumbered="";
      var encumberedType = "";
      var uniqueCode = "";
      if (typeof view.viewGrid.getSelectedRecord() != 'undefined' && !view.isShowingTree) {
        if (tabId == '62248BBBCF644C18A75B92AD8E50238C') {
        	appStatus =	view.viewGrid.getSelectedRecord().escmAppstatus;
        	isEncumbered = view.viewGrid.getSelectedRecord().efinEncumbered;
        	 if(view.viewForm.getFieldFromColumnName('EM_Efin_Encum_Method')!=null &&
          			  view.viewForm.getFieldFromColumnName('EM_Efin_Encum_Method')!=""){
        	encumberedType = view.viewForm.getFieldFromColumnName('EM_Efin_Encum_Method').getValue();
        	 }
          orderId = view.viewGrid.getSelectedRecord().id;
          if(view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID')!=null &&
       			  view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID')!=""){
          uniqueCode = view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').getValue();
          }
        } else if (tabId == '8F35A05BFBB34C34A80E9DEF769613F7') {
          if (view.isShowingTree) 
        	  lines = view.treeGrid.getSelectedRecords();
          else 
        	  lines = view.viewGrid.getSelectedRecords();
          if (lines[0]._new === undefined) orderId =lines[0].id;
          if(lines[0]!=null){
        	  if(view.getParentRecord()!=null && view.getParentRecord()!=''){
        	  appStatus =	view.getParentRecord().escmAppstatus;
          	isEncumbered = view.getParentRecord().efinEncumbered;
          	encumberedType = view.getParentRecord().eFINEncumbranceMethod;
        	  }
       	  if(view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID')!=null &&
       			  view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID')!=""){
            uniqueCode = view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').getValue();
       	  }
            if(uniqueCode == null || uniqueCode == ""){
            	 if(view.getParentRecord()!=null && view.getParentRecord()!=''){
            	uniqueCode = view.getParentRecord().eFINUniqueCode;
            	 }
            }
          }
        }
        if (orderId != "") {
          var requestParams = {
            POId: orderId,
            tabId: tabId,
            Type:"PO"
          };
        }
      
      var EscmCallback = function (response, data, request) {
          if (typeof view.viewGrid.getSelectedRecord() != 'undefined') {
        	  if ((view.viewGrid.getSelectedRecord().eFINUniqueCode != null || data.Message === "Success") && data.isBudgetContrl === "Yes" && data.isEncumActive == "Yes") {        		 
        		  if((uniqueCode != null && uniqueCode != '')
        				  && encumberedType != null){  
        		  if (     appStatus   == 'ESCM_AP' || isEncumbered == true ) {   			 
        		  disable = true;
              }else  {          	 
                disable = false;
              }
          	  }else{
          		disable = true;
          	  }
            } else {
              disable = true;
            }
           
          if ((uniqueCode != null && 
        		  data.Message === "Success" && data.ispurchaseAgreement === "Yes" && 
        		  appStatus == "DR"))
        	  	{
        	  disable = false;
        	  	}
          }
         else {
            disable = true;
          }
          me.setDisabled(disable);
      }
     
      OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CopyUniqueCode', {
        action: 'getuniquecode'
      }, requestParams, EscmCallback);
      }
    },
    buttonType: 'escm_po_apply_uniquecode',
    prompt: OB.I18N.getLabel('escm_pr_distribute_all'),
  };

  // register the button for the Purchase Order and Contracts Summary tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 120, ['62248BBBCF644C18A75B92AD8E50238C', '8F35A05BFBB34C34A80E9DEF769613F7']);
}());