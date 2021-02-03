OB.ToolbarUtils.print = function(view, url, directPrint, type) {
	var popupFunction = function(){

		  var selectedRecords = view.viewGrid.getSelectedRecords(),
		      length = selectedRecords.length;

		  if (length === 0) {
		    view.messageBar.setMessage(isc.OBMessageBar.TYPE_WARNING, '', OB.I18N.getLabel('OBUIAPP_PrintNoRecordSelected'));
		    return;
		  }

		  var popupParams = {},
		      allProperties = view.getContextInfo(false, true, false, true),
		      sessionProperties = view.getContextInfo(true, true, false, true),
		      param, i, value, selectedIds = '';

		  popupParams = {
		    Command: 'DEFAULT',
		    inppdfpath: url,
		    inphiddenkey: view.standardProperties.inpKeyName,
		    inpdirectprint: (directPrint ? 'Y' : 'N'),
		    inpButtonType: type
		  };

		  for (param in allProperties) {
		    if (allProperties.hasOwnProperty(param)) {
		      value = allProperties[param];

		      if (typeof value === 'boolean') {
		        value = value ? 'Y' : 'N';
		      }

		      popupParams[param] = value;
		    }
		  }

		  selectedIds = '';
		  for (i = 0; i < length; i++) {
		    selectedIds += (i > 0 ? ',' : '') + selectedRecords[i].id;
		  }

		  popupParams.inphiddenvalue = selectedIds;

		  view.setContextInfo(sessionProperties, function () {
		    OB.Layout.ClassicOBCompatibility.Popup.open('print', 0, 0, OB.Application.contextUrl + 'businessUtility/PrinterReports.html', '', window, false, false, true, popupParams);
		  });

	}
	
  if (view.tabId == '290') {
    var requestParams = {
      recordId: view.viewGrid.getSelectedRecord().id
    };
    var successCallback = function(response, data, request) {
      if (data.status == 0) {
        grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, OB.I18N.getLabel('EFIN_Currency_Conversion').replace("%", data.fromCurrency));
      } else {
    	  popupFunction();
      }
    };
    OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.PurchaseInvoicePrint', {
      recordId: view.viewGrid.getSelectedRecord().id
    }, {}, successCallback);
  } else {
	  popupFunction();
  }
};