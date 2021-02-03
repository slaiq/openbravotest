/**
 * 
 * @author priyanka
 */
(function() {
  var poSelectedRecordId = null,
    poSelectedRecordId2 = null;

  function showPOExportPopup(tabId, recordId) {
    var windowId = '62E42B7D4CF74BF08532F18D5AF084FD';
    OB.Layout.ClassicOBCompatibility.Popup.open('escm_oee_export', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.openenvelopeevent/OpenEnvelopeEventExport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId, '', null, false, false, true);
  }

  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton('escm_oee_export', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'];
      if (tabId === '8095B818800446D795B8ADFEDE104733') {
        recordId = grid.getSelectedRecord().id;
      } else {
        return false;
      }
      showPOExportPopup(tabId, recordId);
    },
    buttonType: 'escm_oee_export',
    prompt: OB.I18N.getLabel('escm_oee_export'),
    updateState: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'],
        me = this,
        selectedPORecord,
        recordId,
        disableButton;

      disableButton = function(disable) {
        me.setDisabled(disable);
      };
      if (!grid || !grid.getSelectedRecord()) {
        disableButton(true);
        return false;
      }
      if (poSelectedRecordId != null && poSelectedRecordId === grid.getSelectedRecord().id) {
        return false;
      }
      poSelectedRecordId = grid.getSelectedRecord().id;
      setTimeout(function() {
        poSelectedRecordId = null;
      }, 5000);

      disableButton(true);

      if (tabId === '8095B818800446D795B8ADFEDE104733') {
        selectedPORecord = grid.getSelectedRecord();
        if (!selectedPORecord) {
          disableButton(true);
        } else if (selectedPORecord) {
          disableButton(false);
        } 
      } else {
        return false;
      }
    }
  }, 120, ['8095B818800446D795B8ADFEDE104733']);
}());