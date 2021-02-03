/**
 * 
 * @author Gokul on 21/05/2020
 */
(function() {
  var bidSelectedRecordId = null,
    bidSelectedRecordId2 = null;

  function showBidExportPopup(tabId, recordId) {
    var windowId = 'E509200618424FD099BAB1D4B34F96B8';
    OB.Layout.ClassicOBCompatibility.Popup.open('escm_bid_export', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.BidManagement/BidExportProcess") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId, '', null, false, false, true);
  }

  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton('escm_bid_export', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'];
      if (tabId === '31960EC365D746A180594FFB7B403ABB') {
        recordId = grid.getSelectedRecord().id;
      } else {
        return false;
      }
      showBidExportPopup(tabId, recordId);
    },
    buttonType: 'escm_bid_export',
    prompt: OB.I18N.getLabel('escm_bid_export'),
    updateState: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'],
        me = this,
        selectedBidRecord,
        recordId,
        disableButton;

      disableButton = function(disable) {
        me.setDisabled(disable);
      };
      if (!grid || !grid.getSelectedRecord()) {
        disableButton(true);
        return false;
      }
      if (bidSelectedRecordId != null && bidSelectedRecordId === grid.getSelectedRecord().id) {
        return false;
      }
      bidSelectedRecordId = grid.getSelectedRecord().id;
      setTimeout(function() {
        bidSelectedRecordId = null;
      }, 5000);

      disableButton(true);

      if (tabId === '31960EC365D746A180594FFB7B403ABB') {
        selectedBidRecord = grid.getSelectedRecord();
        if (!selectedBidRecord) {
          disableButton(true);
        } 
        else{
        	disableButton(false);
        }
       /* else if (selectedBidRecord.Bidappstatus == "ESCM_AP" || selectedBidRecord.Bidappstatus == "ESCM_IP" || selectedBidRecord.Bidappstatus != null) {
          disableButton(true);
        } else {
          console.error("Export Header HideExportAndImportButtonInBid");
          OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.HideExportAndImportButtonInBid', {
            action: 'getOrderLineCount'
          }, {
            recordId: selectedBidRecord.id
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
  }, 120, ['31960EC365D746A180594FFB7B403ABB']);

  OB.ToolbarRegistry.registerButton('escm_poline_export', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'];
      if (tabId === 'D54F30C8AD574A2A84999F327EF0E3A4') {
        recordId = view.getParentRecord().id;
      } else {
        return false;
      }
      showBidExportPopup(tabId, recordId);
    },
    buttonType: 'escm_bid_export',
    prompt: OB.I18N.getLabel('escm_bid_export'),
    updateState: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'],
        me = this,
        selectedBidRecord,
        recordId,
        disableButton;

      disableButton = function(disable) {
        me.setDisabled(disable);
      };
      if (!grid || !grid.getSelectedRecord()) {
        disableButton(true);
        return false;
      }
      if (bidSelectedRecordId2 != null && bidSelectedRecordId2 === grid.getSelectedRecord().id) {
        return false;
      }
      bidSelectedRecordId2 = grid.getSelectedRecord().id;
      setTimeout(function() {
        bidSelectedRecordId2 = null;
      }, 5000);

      disableButton(true);

      if (tabId === 'D54F30C8AD574A2A84999F327EF0E3A4') {
        selectedBidRecord = view.getParentRecord();
        if (!selectedBidRecord) {
          disableButton(true);
        } 
       /* else if (selectedBidRecord.Bidappstatus == "ESCM_AP" || selectedBidRecord.Bidappstatus == "ESCM_IP" || selectedBidRecord.Bidappstatus != null) {
          disableButton(true);
        } else if (grid.getTotalRows() === 0) {
          disableButton(true);
        }*/
        else {
          disableButton(false);
        }
      } else {
        return false;
      }
    }
  }, 130, ['D54F30C8AD574A2A84999F327EF0E3A4']);
}());