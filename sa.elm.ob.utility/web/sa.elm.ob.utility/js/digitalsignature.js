OB.Digitalsignature = OB.Digitalsignature || {};
OB.Digitalsignature.process = {
    Digitalsignature: function(params, view) {
        var i, recordId = params.button.contextView.viewGrid.getSelectedRecord().id,
            tableId = view.view.standardProperties.inpTableId,
            callback;

        var msg = OB.I18N.getLabel('EUT_digitalsignature');
        var popupCallback = function(ok) {
                if (ok) {
                    isc.showPrompt(OB.I18N.getLabel('OBUIAPP_Loading') + isc.Canvas.imgHTML({
                        src: OB.Styles.LoadingPrompt.loadingImage.src
                    }));
                    callback = function(rpcResponse, data, rpcRequest) {
                        console.log(data);
                        isc.clearPrompt();
                        if (!data.message.isError) {
                            try {
                                console.log(data.message);

                                var xaxis, yaxis;

                                if (data.message.documentType === 'EUT_101' && data.message.level === 'L3') {
                                    xaxis = '400';
                                    yaxis = '35';
                                } else if (data.message.documentType === 'EUT_101' && data.message.level === 'L4') {
                                    xaxis = '280';
                                    yaxis = '35';
                                } else if (data.message.documentType === 'EUT_101' && data.message.level === 'L5') {
                                    xaxis = '130';
                                    yaxis = '35';
                                } else {
                                    xaxis = "20";
                                    yaxis = "40";
                                }

                                if (data.message.documentType === 'EUT_101') {

                                    var parameters = {
                                        "operation": "1",
                                        "filein": {
                                            "web": {
                                                "url": OB.Properties.EUT_Middleware_GetDocument,
                                                "header": {
                                                    "Authorization": "Basic cGtpLWdycDo5em9WWEY="
                                                },
                                                "ProfileURI": data.message.ProfileURI.split(','),
                                                "ProcessId": data.message.GrpRequestID.split(','),
                                                "body": {
                                                    "UserId": data.message.UserID
                                                }
                                            }
                                        },
                                        "fileout": {
                                            "web": {
                                                "url": OB.Properties.EUT_Middleware_PostDocument,
                                                "header": {
                                                    "Authorization": "Basic cGtpLWdycDo5em9WWEY="
                                                },
                                                "DocumentName": data.message.DocumentName.split(','),
                                                "body": {
                                                    "Description": "sampleDesc"
                                                }
                                            }
                                        },
                                        "apperance": {                                            
                                            "width": "200",
                                            "height": "45",
                                            "marginx": xaxis,
                                            "marginy": yaxis
                                        }
                                    };
                                } else {
                                    var parameters = {
                                        "operation": "1",
                                        "filein": {
                                            "web": {
                                                "url": OB.Properties.EUT_Middleware_GetDocument,
                                                "header": {
                                                    "Authorization": "Basic cGtpLWdycDo5em9WWEY="
                                                },
                                                "ProfileURI": data.message.ProfileURI.split(','),
                                                "ProcessId": data.message.GrpRequestID.split(','),
                                                "body": {
                                                    "UserId": data.message.UserID
                                                }
                                            }
                                        },
                                        "fileout": {
                                            "web": {
                                                "url": OB.Properties.EUT_Middleware_PostDocument,
                                                "header": {
                                                    "Authorization": "Basic cGtpLWdycDo5em9WWEY="
                                                },
                                                "DocumentName": data.message.DocumentName.split(','),
                                                "body": {
                                                    "Description": "sampleDesc"
                                                }
                                            }
                                        },
                                        "apperance": {
                                            "width": "0",
                                            "height": "0",
                                            "marginx": xaxis,
                                            "marginy": yaxis,
                                            "align": data.message.position
                                        }
                                    };
                                }
                                console.log(parameters);

                                if (data.message.documentType === 'EUT_101' && (data.message.level === 'L1' || data.message.level === 'L2')) {

                                } else {
                                    var tssParam = encodeURI(JSON.stringify(parameters));
                                    console.log(tssParam);
                                    CallPlugin(tssParam);
                                }
                            } catch (e) {
                                console.error(e);
                            }
                            view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, null, data.message.Message);
                        } else {
                            view.activeView.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, data.message.Message);
                        }

                    };

                    OB.RemoteCallManager.call('sa.elm.ob.utility.ad_process.digitalsignature.DigitalSignature', {
                        recordId: recordId
                    }, {}, callback);
                };
            }
        isc.ask(msg, popupCallback);
    }
};