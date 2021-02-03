/*
 * Contributor; Poongodi
 ************************************************************************
 */

(function () {
  var ReqUpdateState = false;
  var buttonProps = {
    action: function () {
      var view = this.view,
          grid = view.viewGrid;
      // after click on applyUniqueCode icon save the record (autosave)
      this.view.viewForm.autoSave();
      
      var callback = function (ok) {
          var requestParams;

          if (ok) {
            requestParams = {
              PurchaseReqId: view.viewGrid.getSelectedRecord().id,
              Type: "PR"

            };
            var EscmAutoCopy = function (response, data, request) {
                if (data.Message == "Success") {
                  grid.view.refreshChildViews();
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, OB.I18N.getLabel('Escm_Distribute_Success'));
                } else if (data.Message == "NoLines") {
                  grid.view.messageBar.setMessage(
                  isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('Escm_No_Line_Distribution_PR'));
                } else if (data.Message == "NoUniqueCode") {
                    grid.view.messageBar.setMessage(
                    isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_No_UniqueCode_Selected'));
                }else {
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
      ReqUpdateState = true;
      setTimeout(function () {
        ReqUpdateState = false;
      }, 300);
      var requestParams;
      if (typeof view.viewGrid.getSelectedRecord() != 'undefined') {
        requestParams = {
          PurchaseReqId: view.viewGrid.getSelectedRecord().id,
          Type: "PR"
        };
      }
      var EscmCallback = function (response, data, request) {
          if (typeof view.viewGrid.getSelectedRecord() != 'undefined') { 
            if (view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID')!=undefined && (view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').getValue() != null && view.viewForm.getFieldFromColumnName('EM_Efin_C_Validcombination_ID').getValue() != '')
            		&& view.viewForm.getFieldFromColumnName('EM_Efin_Encum_Method').getValue() != null) {
              if (data.isBudgetContrl === 'N') {
                disable = true;
              } else {
                disable = false;
              }

            } else {
              disable = true;
            }
          } else {
            disable = true;
          }
          me.setDisabled(disable);
          };

      OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CopyUniqueCode', {
        action: 'getDistributeFlag'
      }, requestParams, EscmCallback);

    },
    buttonType: 'escm_pr_distribute_all',
    prompt: OB.I18N.getLabel('escm_pr_distribute_all'),
  };

  // register the button for the purchase requisition tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 120, '800249');
}());