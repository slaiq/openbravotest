(function () {
  var view;
  var grid;
  var recordId, receiveType = '',
      documentNo, whType = '',
      status = '',
      propAppStat = '',
      bidNo = '',
      bidType = '';
  var tabId, windowId;

  var buttonProps = {
    action: function () {
      var callback, i;

      view = this.view;
      grid = view.viewGrid;

      windowId = view['windowId'];
      tabId = view['tabId'];
      recordId = view.viewGrid.getSelectedRecord().id;
      if (windowId == '184') {
        receiveType = view.viewGrid.getSelectedRecord().escmReceivingtype;
      } else if (windowId == 'D8BA0A87790B4B67A86A8DF714525736') {
        whType = view.viewGrid.getSelectedRecord().warehouse$escmWarehouseType;
      }
      documentNo = view.viewGrid.getSelectedRecord().documentNo;
      if (tabId == '1511F2A65DCD4CD49290C1964D5ED741') {
        documentNo = view.viewGrid.getSelectedRecord().escmSpecno;
      }
      if (windowId == 'D6F05B3A695E4D6BB357E1B6686E3D4D' || windowId == 'E397822E8DAB4FCDACC84F5C27455F8C' || windowId == '26209E1C023B4879BF58993F9BF9AAC9' || windowId == 'D8BA0A87790B4B67A86A8DF714525736') {
        documentNo = view.viewGrid.getSelectedRecord().documentNo;
      }
      if (windowId == '62E42B7D4CF74BF08532F18D5AF084FD') { // Open Envelope Committee
        status = view.viewGrid.getSelectedRecord().alertStatus;
      }
      if (windowId == 'CAF2D3EEF3B241018C8F65E8F877B29F') { // Proposal Management
        status = view.viewGrid.getSelectedRecord().proposalstatus;
        propAppStat = view.viewGrid.getSelectedRecord().proposalappstatus;
      }
      if (windowId == '9B284558C7E149B0AC245D610F8BC2F6') { //Proposal Eval Event
        bidNo = view.viewGrid.getSelectedRecord().bidNo;
        if (bidNo != null) {
          bidType = view.viewGrid.getSelectedRecord().bidNo$bidtype;
        }
      }
      openReportDownloadDialog(tabId, windowId);
    },
    buttonType: 'escm_print_pdf',
    prompt: OB.I18N.getLabel('ESCM_PrintReport'),
    updateState: function () {
      var view = this.view,
          form = view.viewForm,
          grid = view.viewGrid,
          selectedRecords = grid.getSelectedRecords();
      if (view.isShowingForm && form.isNew) {
        this.setDisabled(true);
      } else if (view.isEditingGrid && grid.getEditForm().isNew) {
        this.setDisabled(true);
      } else {
        this.setDisabled(selectedRecords.length === 0);
      }
    }
  };

  function openReportDownloadDialog(tabId, windowId) {
    var action = "",
        wi;
    if (windowId == '184') //PO Receipt
    wi = OB.Layout.ClassicOBCompatibility.Popup.open('MaterialMgmtShipmentInOut', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&receiveType=" + receiveType + "&documentNo=" + documentNo, '', null, false, false, true);
    else if (windowId == 'D8BA0A87790B4B67A86A8DF714525736') //Material Issue Request
    wi = OB.Layout.ClassicOBCompatibility.Popup.open('Escm_Material_Request', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&warehouseType=" + whType + "&documentNo=" + documentNo, '', null, false, false, true);
    else if (windowId == 'E397822E8DAB4FCDACC84F5C27455F8C' || windowId == '26209E1C023B4879BF58993F9BF9AAC9' || windowId == 'D6F05B3A695E4D6BB357E1B6686E3D4D') //Return Transaction Window , return items and Custody Transfer
    wi = OB.Layout.ClassicOBCompatibility.Popup.open('MaterialMgmtShipmentInOut', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=" + documentNo, '', null, false, false, true);
    else if (windowId == 'E509200618424FD099BAB1D4B34F96B8') { // bid management
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('Bidmanagement', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=", '', null, false, false, true);
    } else if (windowId == '8FC04D21ED7540F2B6A4ADCE9BDD58A6') //inventory counting
    wi = OB.Layout.ClassicOBCompatibility.Popup.open('MaterialMgmtInventoryCount', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=" + documentNo, '', null, false, false, true);
    else if (windowId === '72AF0A6A09494113ABFA815B367EF930') // bid announcement summary
    {
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('escm_annoucements', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=", '', null, false, false, true);
    } else if (windowId == 'CAF2D3EEF3B241018C8F65E8F877B29F') { // proposal managemnt
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('escm_proposalmgmt', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=", '', null, false, false, true);
    } else if (windowId == 'F7521058B095442698011735E9A5AC80') { // RFP Sales Voucher
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('escm_salesvoucher', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=", '', null, false, false, true);
    } else if (windowId == '62E42B7D4CF74BF08532F18D5AF084FD') { // Open Envelope Committee
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('escm_openenvcommitee', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=", '', null, false, false, true);
    } else if (windowId === '9B284558C7E149B0AC245D610F8BC2F6') {
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('proposalevaluation', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=", '', null, false, false, true);
    } else if (tabId === '6732339A97874A85BF73542C2B5AFF88') { //Bank Guarantee Workbech  
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('BankGuaranteeworkbench', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD", '', null, false, false, true);
    } else if (windowId === '2ADDCB0DD2BF4F6DB13B21BBCCC3038C') { //purchase order and contract 
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('PoOrderandContract', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD", '', null, false, false, true);
    } else if (windowId === '13B58C9F5DA14EEC9CAD9FAF9234457D') { //Insurance Certificate Workbench 
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('EscmInsuranceCertificate', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD", '', null, false, false, true);
    } else if (windowId === '006832D5A20E45289F191D08949D252B') {
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('TechnicalEvaluationEvent', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=", '', null, false, false, true);
    } else if (tabId === 'FDBA56F9D57A4F988F4CC6F3577428B9' || tabId === 'A0F3A7D17A834A93B3BD4D2C40E77AFE') {
      wi = OB.Layout.ClassicOBCompatibility.Popup.open('RDVSummary', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.printreport/PrintReport") + "?Command=" + "DEFAULT" + "&inpRecordId=" + recordId + "&inpTabId=" + tabId + "&inpWindowID=" + windowId + "&pageType=WAD&documentNo=", '', null, false, false, true);
    }
    if (receiveType === 'IR' || receiveType === 'INS' || whType === 'RTW' || windowId == 'E397822E8DAB4FCDACC84F5C27455F8C' || windowId == 'D6F05B3A695E4D6BB357E1B6686E3D4D' || windowId == '26209E1C023B4879BF58993F9BF9AAC9' || windowId == '8FC04D21ED7540F2B6A4ADCE9BDD58A6' || windowId === '72AF0A6A09494113ABFA815B367EF930' || windowId == 'F7521058B095442698011735E9A5AC80' || (windowId == '62E42B7D4CF74BF08532F18D5AF084FD' && status == 'DR') || (windowId == 'CAF2D3EEF3B241018C8F65E8F877B29F' && status != 'AWD' && propAppStat != 'APP') || (windowId == '9B284558C7E149B0AC245D610F8BC2F6' && (bidType == 'TR' || bidType == 'LD')) || (tabId === 'FDBA56F9D57A4F988F4CC6F3577428B9' || tabId === 'A0F3A7D17A834A93B3BD4D2C40E77AFE')) wi.close();
  }
  // register the print button for the PO Receipt tab, Material Issue Request and Return transaction Tab
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 160, ['296', 'CE947EDC9B174248883292F17F03BB32', '72A6B3CA5BE848ACA976304375A5B7A6', '922927563BFC48098D17E4DC85DD504C', 'CB9A2A4C6DB24FD19D542A78B07ED6C1', '1511F2A65DCD4CD49290C1964D5ED741', '31960EC365D746A180594FFB7B403ABB', 'BA8A044E0AC54DB8A51210458C4FADD9', 'D6115C9AF1DD4C4C9811D2A69E42878B', '6F86F1F0E85C4A8F8DF36B5654BA3E3C', '8095B818800446D795B8ADFEDE104733', '61D6CF3612134CAF942B811EC74B1F0B', '6732339A97874A85BF73542C2B5AFF88', '62248BBBCF644C18A75B92AD8E50238C', '15D8FAE224994F089560D0D8F51110CB', '7185D00B421A4F62B403E085F00176D6', 'A0F3A7D17A834A93B3BD4D2C40E77AFE', 'FDBA56F9D57A4F988F4CC6F3577428B9']);
}());