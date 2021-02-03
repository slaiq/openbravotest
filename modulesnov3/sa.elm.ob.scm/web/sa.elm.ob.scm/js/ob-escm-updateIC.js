OB.ESCMTransactionRegistry = OB.ESCMTransactionRegistry || {};
OB.ESCMTransactionRegistry.process = {
    updateIC: function(params, view) {
        var i, selection = params.button.contextView.viewGrid.getSelectedRecords(),
            transactions = [],
            callback;
        callback = function(rpcResponse, data, rpcRequest) {
            if (data.code == "-1") {
                view.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, data.message);
            } else view.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, null, data.message);


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

        OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.TransactionRegistry.UpdateICActionHandler', {
            transactions: transactions,
        }, {}, callback);
    }
};