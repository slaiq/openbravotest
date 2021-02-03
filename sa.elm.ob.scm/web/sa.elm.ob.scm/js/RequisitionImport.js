/**
 * 
 * @author Divya on 22-07-2020
 */
(function() {	
  var propSelectedRecordId = null,
    propSelectedRecordId2 = null;

  function openUploadDialog(tabId, recordId) {
    var windowId = '800092';
    OB.Layout.ClassicOBCompatibility.Popup.open('escm_requisition_import', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.RequisitionLineUploader.header/RequisitionLineUploader") + "?Command=" + "DEFAULT" + "&M_Requisition_ID=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD", '', null, false, false, true);
  }

  // register the button for proposal tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton('escm_requisition_import', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = view['tabId'],
        recordId;
      if (view.tabId === '800249') {
        recordId = grid.getSelectedRecord().id;
      } else {
        return false;
      }
      openUploadDialog(tabId, recordId);
    },
    buttonType: 'escm_requisition_import',
    prompt: OB.I18N.getLabel('ESCM_ImportPR'),
    updateState: function() {
      var view = this.view,
        grid = view.viewGrid,
        me = this,
        selectedPropRecord,
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

      if (view.tabId === '800249') {
        selectedPropRecord = grid.getSelectedRecord();
        if (!selectedPropRecord) {
          disableButton(true);
        }
        else if (selectedPropRecord.escmDocStatus != "DR") {
          disableButton(true);
        }
        else{
        	 disableButton(false);
        }
      } else {
        return false;
      }
    }
  }, 130, ['800249']);

}());