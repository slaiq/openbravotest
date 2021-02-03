/**
 * 
 * @author Rashika.V.S on 22-04-2019
 */
(function() {
  var poSelectedRecordId = null,
    poSelectedRecordId2 = null;

  function openUploadDialog(tabId, recordId) {
    var windowId = '2ADDCB0DD2BF4F6DB13B21BBCCC3038C';
    OB.Layout.ClassicOBCompatibility.Popup.open('escm_po_import', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.ImportPurchaseOrder/ImportPOLines") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD", '', null, false, false, true);
  }

  // register the button for purchase order tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton('escm_po_import', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = view['tabId'],
        recordId;
      if (tabId === '62248BBBCF644C18A75B92AD8E50238C') {
        recordId = grid.getSelectedRecord().id;
      } else {
        return false;
      }
      openUploadDialog(tabId, recordId);
    },
    buttonType: 'escm_po_import',
    prompt: OB.I18N.getLabel('ESCM_ImportPO'),
    updateState: function() {
      var view = this.view,
        grid = view.viewGrid,
        me = this,
        selectedPORecord,
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

      if (tabId === '62248BBBCF644C18A75B92AD8E50238C') {
        selectedPORecord = grid.getSelectedRecord();
        if (!selectedPORecord) {
          disableButton(true);
        } 
        else if (selectedPORecord.escmAppstatus == "ESCM_AP" || selectedPORecord.escmAppstatus == "ESCM_IP" || selectedPORecord.escmAppstatus == "ESCM_CA" || selectedPORecord.escmProposalmgmt != null) {
          disableButton(true);
        } 
        else{
        	disableButton(false);
        }
       /* else {
          OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.HideExportAndImportButtonInPO', {
            action: 'getOrderLineCount'
          }, {
            recordId: selectedPORecord.id
          }, function(response, data, request) {
            if (data.result === 0) {
              disableButton(true);
            } else {
              disableButton(false);
            }
          });
        }*/
        
      } else {
        return false;
      }
    }
  }, 120, ['62248BBBCF644C18A75B92AD8E50238C']);

  OB.ToolbarRegistry.registerButton('escm_poline_import', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = view['tabId'],
        recordId;
      if (tabId === '8F35A05BFBB34C34A80E9DEF769613F7') {
        recordId = view.getParentRecord().id;
      } else {
        return false;
      }
      openUploadDialog(tabId, recordId);
    },
    buttonType: 'escm_po_import',
    prompt: OB.I18N.getLabel('ESCM_ImportPO'),
    updateState: function() {
      var view = this.view,
        grid = view.viewGrid,
        me = this,
        selectedPORecord,
        disableButton;
      disableButton = function(disable) {
        me.setDisabled(disable);
      };
      if (!grid || !grid.getSelectedRecord()) {
        disableButton(true);
        return false;
      }
      if (poSelectedRecordId2 != null && poSelectedRecordId2 === grid.getSelectedRecord().id) {
        return false;
      }
      poSelectedRecordId2 = grid.getSelectedRecord().id;
      setTimeout(function() {
        poSelectedRecordId2 = null;
      }, 5000);

      disableButton(true);

      if (tabId === '8F35A05BFBB34C34A80E9DEF769613F7') {
        selectedPORecord = view.getParentRecord();
        if (!selectedPORecord) {
          disableButton(true);
        }
        else if (selectedPORecord.escmAppstatus == "ESCM_AP" || selectedPORecord.escmAppstatus == "ESCM_IP" || selectedPORecord.escmAppstatus == "ESCM_CA" || selectedPORecord.escmProposalmgmt != null) {
          disableButton(true);
        }
        /*else if (grid.getTotalRows() === 0) {
          disableButton(true);
        } */
        else {
          disableButton(false);
        }
      } else {
        return false;
      }
    }
  }, 130, ['8F35A05BFBB34C34A80E9DEF769613F7']);

}());