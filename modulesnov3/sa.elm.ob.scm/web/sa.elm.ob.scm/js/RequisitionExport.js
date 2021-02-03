/**
 * 
 * @author Divya on 22-07-2020
 */
(function() {
  var propSelectedRecordId = null,
    propSelectedRecordId2 = null;

  function showProposalExportPopup(tabId, recordId) {
    var windowId = '800092';
    OB.Layout.ClassicOBCompatibility.Popup.open('escm_requisition_export', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.exportpr.header/ExportPR") + "?Command=" + "DEFAULT" + "&M_Requisition_ID=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId, '', null, false, false, true);
  }

  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton('escm_requisition_export', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'];
      if (tabId === '800249') {
        recordId = grid.getSelectedRecord().id;
      } else {
        return false;
      }
      showProposalExportPopup(tabId, recordId);
    },
    buttonType: 'escm_requisition_export',
    prompt: OB.I18N.getLabel('ESCM_ExportPR'),
    updateState: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'],
        me = this,
        selectedPropRecord,
        recordId,
        disableButton;

      disableButton = function(disable) {
        me.setDisabled(disable);
      };
      if (!grid || !grid.getSelectedRecord()) {
        disableButton(true);
        return false;
      }
      if (propSelectedRecordId != null && propSelectedRecordId === grid.getSelectedRecord().id) {
        return false;
      }
      propSelectedRecordId = grid.getSelectedRecord().id;
      setTimeout(function() {
        propSelectedRecordId = null;
      }, 5000);

      disableButton(true);

      if (tabId === '800249') {
        selectedPropRecord = grid.getSelectedRecord();
        if (!selectedPropRecord) {
          disableButton(true);
        }  else {
              disableButton(false);            
        }
      } else {
        return false;
      }
    }
  }, 120, ['800249']);
}());