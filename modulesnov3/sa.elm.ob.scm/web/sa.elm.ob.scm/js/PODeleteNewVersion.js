/*
 * Contributor: Gokul 03/01/2019
 ************************************************************************
 */

OB.PODeleteNewVersion = OB.PODeleteNewVersion || {};
OB.PODeleteNewVersion.process = {
  poDeleteNewVersion: function (params, view) {
    var i, recordId = params.button.contextView.viewGrid.getSelectedRecord().id,
        callback;

    var msg = OB.I18N.getLabel('ESCM_deleteversionPO');

    var popupCallback = function (ok) {

        if (ok) {
          callback = function (rpcResponse, data, rpcRequest) {
            console.log(data);
            if (data.message.severity == "Success") {
              view.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, null, data.message.text);
            } else {
              view.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, data.message.text);
            }
            //refresh the grid after executing the process
            params.button.contextView.viewGrid.refreshGrid();
          };

          OB.RemoteCallManager.call('sa.elm.ob.scm.ad_process.POandContract.PoDeleteNewVersion', {
            recordId: recordId,
          }, {}, callback);
        };
        }
        
    isc.ask(msg, popupCallback);
  }
};