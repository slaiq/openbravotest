/*
 * Contributor: Kousalya
 ************************************************************************
 */

(function () {
  var ReqUpdateState = false;
  var tabId, windowId;
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
      //console.log(JSON.stringify(ids));
      var callback = function (ok) {
          var requestParams;
          if (ok) {
            requestParams = {
              linesId: JSON.stringify(ids),
              tabId: tabId1
            };
            var EscmApplyChange = function (response, data, request) {
                if (data.Message == "Success") {
                	if (tabId == '8F35A05BFBB34C34A80E9DEF769613F7') 
                		currentGrid.deselectAllRecords();
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
                  if(data.taxApplied == false)
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, OB.I18N.getLabel('ESCM_POChangesAppliedSuccessfully'));
                  else if(data.taxApplied == true)
                	  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_INFO, OB.I18N.getLabel('Escm_DiscountChanged_AfterTax'));
                } else if (data.Message == "NoLines") {
                  grid.view.messageBar.setMessage(
                  isc.OBMessageBar.TYPE_WARNING, OB.I18N.getLabel('ESCM_PONoLinesToApplyChanges'));
                } else if (data.Message == "NoValueSet") {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_PONoValueSet'));
                } else if (data.Message == "NewLine") {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_PONewLineAdded'));
                } else if (data.Message == "ValidationError") {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_POChangeValCantBeApplied'));
                } else if (data.Message == "NoLineSelected") {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('ESCM_PONoLineSelected'));
                } else {
                  grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, data.Message);
                }
                };
            OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.ApplyPOChangesToLines', requestParams, {
              action: 'setChangeValues'
            }, EscmApplyChange);
          }
          };
      isc.ask(OB.I18N.getLabel('ESCM_SureToApplyChanges'), callback);
    },
    updateState: function () {
      var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid,
          selectedRecords = grid.getSelectedRecords();
      windowId = view['windowId'];
      tabId = view['tabId'];
      var status = "",
          lines = "";
      var me = this,
          disable = false;
      if (view.isShowingForm && form.isNew) {
        me.setDisabled(true);
      } else if (view.isEditingGrid && grid.getEditForm().isNew) {
        me.setDisabled(true);
      } else {
        me.setDisabled(selectedRecords.length === 0);
      }
      var orderId = "";
      if (typeof view.viewGrid.getSelectedRecord() != 'undefined' && !view.isShowingTree) {
        if (tabId == '62248BBBCF644C18A75B92AD8E50238C') {
          orderId = view.viewGrid.getSelectedRecord().id;
        } else if (tabId == '8F35A05BFBB34C34A80E9DEF769613F7') {
          if (view.isShowingTree) lines = view.treeGrid.getSelectedRecords();
          else lines = view.viewGrid.getSelectedRecords();
          if (lines[0]._new === undefined) orderId = lines[0].salesOrder;
        }
        if (orderId != "") {
          var requestParams = {
            orderId: orderId
          };
        }

        var EscmCallback = function (response, data, request) {
            if (data != undefined) {
              if (data.Message == 'NotAllowed') {
                disable = true;
              } else if (data.Message == 'Allowed') {
                disable = false;
              } else {
                disable = true;
              }
            }
            me.setDisabled(disable);
            };
        OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.ApplyPOChangesToLines', requestParams, {
          action: 'getStatus'
        }, EscmCallback);

        //me.setDisabled(disable);
      }

      if (view.isShowingTree && view.treeGrid.getSelectedRecord() != undefined) {
        if (tabId == '8F35A05BFBB34C34A80E9DEF769613F7') {
          lines = view.treeGrid.getSelectedRecords();
          if (lines.length > 0) {
            orderId = lines[0].salesOrder;
          }
        }
        if (orderId != "") {
          var requestParams = {
            orderId: orderId
          };
        }
        var EscmCallback = function (response, data, request) {
            if (data != undefined) {
              if (data.Message == 'NotAllowed') {
                disable = true;
              } else if (data.Message == 'Allowed') {
                disable = false;
              } else {
                disable = true;
              }
            }
            me.setDisabled(disable);
            };
        OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.ApplyPOChangesToLines', requestParams, {
          action: 'getStatus'
        }, EscmCallback);
      }
    },
    buttonType: 'escm_apply',
    prompt: OB.I18N.getLabel('ESCM_ApplyChanges')
  };

  // register the button for the PO and Contract summary- Header, Lines
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 120, ['62248BBBCF644C18A75B92AD8E50238C', '8F35A05BFBB34C34A80E9DEF769613F7']);


}());