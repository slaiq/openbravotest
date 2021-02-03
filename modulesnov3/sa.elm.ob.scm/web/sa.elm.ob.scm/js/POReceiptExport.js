/**
 * 
 * @author Divya on 22-07-2020
 */
(function() {
  var propSelectedRecordId = null,
    propSelectedRecordId2 = null;

  function showProposalExportPopup(tabId, recordId) {
    var windowId = '184';
    OB.Layout.ClassicOBCompatibility.Popup.open('escm_po_Receipt_export', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.exportporeceipt.header/ExportPOReceipt") + "?Command=" + "DEFAULT" + "&M_InOut_ID=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId, '', null, false, false, true);
  }

  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton('escm_po_Receipt_export', isc.OBToolbarIconButton, {
    action: function() {
      var view = this.view,
        grid = view.viewGrid,
        tabId = this.view['tabId'];
      if (tabId === '296') {
        recordId = grid.getSelectedRecord().id;
      } else {
        return false;
      }
      showProposalExportPopup(tabId, recordId);
    },
    buttonType: 'escm_po_Receipt_export',
    prompt: OB.I18N.getLabel('ESCM_POReceiptExport'),
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

      if (tabId === '296') {
        selectedPropRecord = grid.getSelectedRecord();
        if (!selectedPropRecord) {
          disableButton(true);
        } 
     // MM receipt and other than site receiving,Project receiving,receive should not show import icon, 
        //for site receiving,Project receiving,receive all  status should show
        else if(((selectedPropRecord.escmDoctype!="PO        ") 
            	|| ((selectedPropRecord.escmDoctype=="PO        ") & ( 
            			selectedPropRecord.escmReceivingtype!="SR" & selectedPropRecord.escmReceivingtype!="IR" 
        		& selectedPropRecord.escmReceivingtype!="PROJ" )))){
        	 disableButton(true);
        }
        else {
              disableButton(false);            
        }
      } else {
        return false;
      }
    }
  }, 120, ['296']);
}());