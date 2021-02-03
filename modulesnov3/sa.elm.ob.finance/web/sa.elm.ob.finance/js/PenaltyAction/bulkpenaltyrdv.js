OB.EFINRdvApplyPanalty = OB.EFINRdvApplyPanalty || {};
OB.EFINRdvApplyPanalty.process = {
    applyPanalty: function(params, view) {
        var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
            transactions = [],
            callback;
        callback = function(rpcResponse, data, rpcRequest) {
        	 if (data.severity == "success") {
               view.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, data.message.text);
          } else view.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, null, data.message.text);


            var i, selectedRecords = params.button.contextView.viewGrid.getSelectedRecords();
            for (i = 0; i < selectedRecords.length; i++) {
                params.button.contextView.viewGrid.selectRecord(selectedRecords[i], false);
            }
            // refresh the whole grid after executing the process
            params.button.contextView.viewGrid.refreshGrid();
        };

        for (i = 0; i < selection.length; i++) {
            transactions.push(selection[i].id);
        };

        wi = OB.Layout.ClassicOBCompatibility.Popup.open('RdvApplyPenalty', 900, 650, OB.Utilities.applicationUrl("sa.elm.ob.finance.ad_process.RDVProcess/RdvApplyPenalty") + "?Command=" + "DEFAULT" + "&inpselectedRecordsId=" + transactions + '', null, false, false, true);
       
    }
};