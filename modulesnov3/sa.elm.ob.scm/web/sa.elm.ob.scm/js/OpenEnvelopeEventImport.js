/**
 * 
 * @author priyanka
 */
(function() {
  var oeeSelectedRecordId = null;

  function showPOExportPopup(tabId, recordId) {
    var windowId = '62E42B7D4CF74BF08532F18D5AF084FD';
    OB.Layout.ClassicOBCompatibility.Popup.open('escm_oee_import', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.openenvelopeevent/OpenEnvelopeEventImport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId, '', null, false, false, true);
  }

  // the first parameter is a unique identification so that one button can not be registered multiple times.
  
  OB.ToolbarRegistry.registerButton('escm_oee_import', isc.OBToolbarIconButton, {
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
	    buttonType: 'escm_oee_import',
	    prompt: OB.I18N.getLabel('escm_oee_import'),
	    updateState: function() {
	      var view = this.view,
	        grid = view.viewGrid,
	        tabId = this.view['tabId'],
	        me = this,
	        selectedRecord,
	        recordId,
	        disableButton;

	      disableButton = function(disable) {
	        me.setDisabled(disable);
	      };
	      if (!grid || !grid.getSelectedRecord()) {
	        disableButton(true);
	        return false;
	      }
	      if (oeeSelectedRecordId != null && oeeSelectedRecordId === grid.getSelectedRecord().id) {
	        return false;
	      }
	      oeeSelectedRecordId = grid.getSelectedRecord().id;
	      setTimeout(function() {
	    	  oeeSelectedRecordId = null;
	      }, 5000);

	     disableButton(true);

	      if (tabId === '8095B818800446D795B8ADFEDE104733') {
	        selectedRecord = grid.getSelectedRecord();
	        if (!selectedRecord) {
	          disableButton(true);
	        } 
	        else if(selectedRecord.alertStatus!="DR"){
	        	disableButton(true);
	        }
	        else if (selectedRecord) {
	          disableButton(false);
	        } 
	      } else {
	        return false;
	      }
	    }
	  }, 130, ['8095B818800446D795B8ADFEDE104733']);
}());