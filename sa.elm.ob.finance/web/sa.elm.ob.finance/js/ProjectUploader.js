(function () {
  var view, grid, recordId, tabId;
  var docs = [];
  var buttonProps = {
    action: function () {
      var callback, i, view = this.view,
          grid = view.viewGrid,
          tabId = view['tabId'],
          windowId = view['windowId'];
      openUploadDialog(tabId, windowId, docs);
    },
    buttonType: 'efin_project_upload_excel',
    prompt: OB.I18N.getLabel('EFIN_UploadExcel'),
    updateState: function () {
      var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid;
    }
  };

  function openUploadDialog(tabId, windowId, docs) {
    var action = "";
    OB.Layout.ClassicOBCompatibility.Popup.open('FIN_Payment', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.finance.ad_process.projectuploader.header/ProjectUploader") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD", '', null, false, false, true);
  }
  
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 160, 'C65F8D7099BE42FEBDD927DECC4D7D85');
}());