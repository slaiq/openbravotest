/**
 * 
 * @author Kiruthika on 28-05-2020
 */
(function() {
  var propSelectedRecordId = null,
    propSelectedRecordId2 = null;

  function showProposalExportPopup(tabId, recordId) {
    var windowId = 'CAF2D3EEF3B241018C8F65E8F877B29F';
    OB.Layout.ClassicOBCompatibility.Popup.open('escm_proposal_export', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.ImportExportPropMgmt/ProposalExportProcess") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId, '', null, false, false, true);
  }

  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton('escm_proposal_export', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'];
      if (tabId === 'D6115C9AF1DD4C4C9811D2A69E42878B') {
        recordId = grid.getSelectedRecord().id;
      } else {
        return false;
      }
      showProposalExportPopup(tabId, recordId);
    },
    buttonType: 'escm_proposal_export',
    prompt: OB.I18N.getLabel('escm_proposal_export'),
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

      if (tabId === 'D6115C9AF1DD4C4C9811D2A69E42878B') {
        selectedPropRecord = grid.getSelectedRecord();
        if (!selectedPropRecord) {
          disableButton(true);
        } 
        else{
        	disableButton(false);
        }
/*        else if (selectedPropRecord.proposalstatus != "DR" && selectedPropRecord.proposalstatus != "SUB" && selectedPropRecord.proposalstatus != "OPE") {
          disableButton(true);
        } 
        else {
          console.error("Export Header HideExportImportButtonProposal");
          OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.HideExportImportButtonProposal', {
            action: 'getProposalLineCount'
          }, {
            recordId: selectedPropRecord.id
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
  }, 120, ['D6115C9AF1DD4C4C9811D2A69E42878B']);

  OB.ToolbarRegistry.registerButton('escm_propline_export', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'];
      if (tabId === '88E026FD2D0446048C80E9D4749AB608') {
        recordId = view.getParentRecord().id;
      } else {
        return false;
      }
      showProposalExportPopup(tabId, recordId);
    },
    buttonType: 'escm_proposal_export',
    prompt: OB.I18N.getLabel('escm_proposal_export'),
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
      if (propSelectedRecordId2 != null && propSelectedRecordId2 === grid.getSelectedRecord().id) {
        return false;
      }
      propSelectedRecordId2 = grid.getSelectedRecord().id;
      setTimeout(function() {
    	  	propSelectedRecordId2 = null;
      }, 5000);

      disableButton(true);

      if (tabId === '88E026FD2D0446048C80E9D4749AB608') {
    	  	selectedPropRecord = view.getParentRecord();
        if (!selectedPropRecord) {
          disableButton(true);
        } 
/*        else if (selectedPropRecord.proposalstatus != "DR" && selectedPropRecord.proposalstatus != "SUB" && selectedPropRecord.proposalstatus != "OPE") {
          disableButton(true);
        } else if (grid.getTotalRows() === 0) {
          disableButton(true);
        } */
        else {
          disableButton(false);
        }
      } else {
        return false;
      }
    }
  }, 130, ['88E026FD2D0446048C80E9D4749AB608']);
}());