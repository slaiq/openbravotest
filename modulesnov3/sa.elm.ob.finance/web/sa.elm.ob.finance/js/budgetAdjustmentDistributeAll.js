/*
 * Contributor; Priyanka Ranjan
 ************************************************************************
 */

(function () {
  var budgetAdjUpdateState = false;
  var buttonProps = {
    action: function () {
      var view = this.view,
          grid = view.viewGrid;
      var callback = function (ok) {
          var requestParams;

          if (ok) {
            requestParams = {
              BudgetAdjustId: view.viewGrid.getSelectedRecord().id
            };
            var EfinAutoCopy = function (response, data, request) {
                if (data.Message == "Success") {
                  grid.view.viewGrid.refreshGrid()
                  grid.view.messageBar.setMessage(
                  isc.OBMessageBar.TYPE_SUCCESS, OB.I18N.getLabel('Efin_Distribute_Success'));
                } else if (data.Message == "NoLines") {
                  grid.view.messageBar.setMessage(
                  isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('Efin_No_Line_Distribution'));
                } else {
                  grid.view.messageBar.setMessage(
                  isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ProcessFailed'));
                }
                };
            OB.RemoteCallManager.call('sa.elm.ob.finance.ad_callouts.BudgetAdjustmentDistributeAllCallout', {
              action: 'setDistributeAll'
            }, requestParams, EfinAutoCopy);
          }
          };
      isc.ask(OB.I18N.getLabel('Efin_WantToDistribute'), callback);
    },
    updateState: function () {
      if (budgetAdjUpdateState == true) {
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
      budgetAdjUpdateState = true;
      setTimeout(function () {
        budgetAdjUpdateState = false;
      }, 300);
      var requestParams;
      if (typeof view.viewGrid.getSelectedRecord() != 'undefined') {
        requestParams = {
          BudgetAdjustId: view.viewGrid.getSelectedRecord().id
        };
      }
      var EfinCallback = function (response, data, request) {
          if (typeof view.viewGrid.getSelectedRecord() != 'undefined') {
            if (typeof view.viewGrid.getSelectedRecord().distributionLinkOrg != 'undefined') {
              if (view.viewGrid.getSelectedRecord().documentStatus != 'DR' /*|| (view.viewGrid.getSelectedRecord().distributionLinkOrg === null)*/ || data.isNoFlag) {
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
      OB.RemoteCallManager.call('sa.elm.ob.finance.ad_callouts.BudgetAdjustmentDistributeAllCallout', {
        action: 'getDistributeFlag'
      }, requestParams, EfinCallback);

    },
    buttonType: 'efin_ba_distribute_all',
    prompt: OB.I18N.getLabel('EFIN_Distribute_All'),
  };

  // register the button for the Budget Adjustment tab
  // the first parameter is a unique identification so that one button can not
  // be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 220, 'A9D394A5BE374ADC815DABBAF3D6D591');
}());