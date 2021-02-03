/*
 * Contributor; Gopalakrishnan
 ************************************************************************
 */

(function () {
  var buttonProps = {
    action: function () {
      var view = this.view,
          grid = view.viewGrid;
      var callback = function (ok) {
          var requestParams;

          if (ok) {
            requestParams = {
              budgetId: view.viewGrid.getSelectedRecord().id
            };
            var EfinAutoCopy = function (response, data, request) {
                if (data.Message == "LineExists") {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('Efin_LinesExists'));
                }
                if (data.Message == "PreviousPeriodNotExists") {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('Efin_PeriodsNotExists'));
                }
                if (data.Message == "PreviousBudgetNotExists") {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('Efin_BudgetNotExists'));
                }
                if (data.Message == "Success") {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, OB.I18N.getLabel('Efin_AutoCopied_Success'));
                }
                };
            OB.RemoteCallManager.call('sa.elm.ob.finance.ad_callouts.AutoCopyCallout', {
              action: 'setAutoCopy'
            }, requestParams, EfinAutoCopy);
          }
          };
      isc.ask(OB.I18N.getLabel('Efin_WantToAutoCopy'), callback);
    },
    updateState: function () {
      var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid,
          selectedRecords = grid.getSelectedRecords();
      var me = this;
      if (view.isShowingForm && form.isNew) {
        this.setDisabled(true);
      } else if (view.isEditingGrid && grid.getEditForm().isNew) {
        this.setDisabled(true);
      } else {
        this.setDisabled(selectedRecords.length === 0);
      }
      var requestParams;

      if (typeof view.viewGrid.getSelectedRecord() != 'undefined') {
        requestParams = {
          budgetId: view.viewGrid.getSelectedRecord().id
        };
      }

      var EfinCallback = function (response, data, request) {
          if (data.isCarryForward) {
            me.setDisabled(true);
          } else if (!data.isCarryForward && (data.status == 'Open' || data.status == 'Rework')) {
            me.setDisabled(false);
          } else {
            me.setDisabled(true);
          }
          };
      OB.RemoteCallManager.call('sa.elm.ob.finance.ad_callouts.AutoCopyCallout', {
        action: 'getCarryForward'
      }, requestParams, EfinCallback);
    },
    buttonType: 'efin_auto_copy',
    prompt: OB.I18N.getLabel('EFIN_AutoCopy'),
  };

  // register the button for the sales order tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 140, 'D1F0FD0F4B3D4CA7AA9BFB81BB819C62');
}());
