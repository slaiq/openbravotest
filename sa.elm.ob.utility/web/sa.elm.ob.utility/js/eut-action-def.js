OB.Utilities.Action.set('openDirectTabWithMultiRecord', function(paramObj) {
    var processIndex, tabPosition, isTabOpened = false;
    var urlparams = OB.Utilities.getUrlParameters();
    urlparams.filterClause = paramObj.filterClause;
    if (!paramObj.newTabPosition) {
        tabPosition = OB.Utilities.getTabNumberById(paramObj.tabId); // Search if the tab has been opened before
        if (tabPosition !== -1) {
            paramObj.newTabPosition = tabPosition;
            isTabOpened = true;
        } else {
            processIndex = OB.Utilities.getProcessTabBarPosition(paramObj._processView);
            if (processIndex === -1) {
                // If the process is not found in the main tab bar, add the new window in the last position
                paramObj.newTabPosition = OB.MainView.TabSet.paneContainer.members.length;
            } else {
                // If the process is found in the main tab bar, add the new window in its next position
                paramObj.newTabPosition = processIndex + 1;
            }
        }
    }
    if (!paramObj.isOpening) {
        OB.Utilities.openDirectTab(paramObj.tabId, paramObj.recordId, paramObj.command, paramObj.newTabPosition, paramObj.criteria, null, urlparams);
    }
    if ((paramObj.wait === true || paramObj.wait === 'true') && paramObj.threadId) {
        if (!OB.MainView.TabSet.getTabObject(paramObj.newTabPosition) || OB.MainView.TabSet.getTabObject(paramObj.newTabPosition).pane.isLoadingTab === true || isTabOpened) {
            OB.Utilities.Action.pauseThread(paramObj.threadId);
            paramObj.isOpening = true;
            OB.Utilities.Action.execute('openDirectTab', paramObj, 100); //Call this action again with a 100ms delay
        } else {
            OB.Utilities.Action.resumeThread(paramObj.threadId, 1500); //Call this action again with a 1500ms delay
        }
    }
});



OB.Utilities.Action.set('openAttachmentInProposal', function(paramObj) {

    var noButton = isc.OBFormButton.create({
        title: OB.I18N.getLabel('OBUIAPP_AttachmentAdd'),
        theForm: form,
        click: function() {
            this.topElement.cancelClick();
            createPopup(false);
        }
    });


    var YesButton = isc.OBFormButton.create({
        title: OB.I18N.getLabel('Escm_replace'),
        theForm: form,
        click: function() {
            this.topElement.cancelClick();
            createPopup(true);
        }
    });



    var confirmation = isc.confirm(OB.I18N.getLabel('Escm_removeAttachment_question'), {
        isModal: true,
        showModalMask: true,
        title: OB.I18N.getLabel('Escm_add_attachment'),
        toolbarButtons: [YesButton, noButton, isc.Dialog.CANCEL]
    });


    function createPopup(remove) {
        var attachmentFile = OB.I18N.getLabel('OBUIAPP_AttachmentFile');
        var form = isc.DynamicForm.create({
            autoFocus: true,
            fields: [{
                name: 'inpname',
                title: attachmentFile,
                type: 'upload',
                multiple: true,
                canFocus: false,
                align: 'right'
            }, {
                name: 'Command',
                type: 'hidden',
                value: 'SAVE_NEW_OB3'
            }, {
                name: 'buttonId',
                type: 'hidden',
                value: ''
            }, {
                name: 'inpKey',
                type: 'hidden',
                value: paramObj.inpescmProposalmgmtId
            }, {
                name: 'inpTabId',
                type: 'hidden',
                value: paramObj.tabId
            }, {
                name: 'inpwindowId',
                type: 'hidden',
                value: ''
            }, {
                name: 'inpDescription',
                title: OB.I18N.getLabel('APRM_FATS_DESCRIPTION'),
                type: 'text',
                value: ''
            }, {
                name: 'inpRemove',
                title: 'Remove',
                type: 'hidden',
                value: remove
            }],
            encoding: 'multipart',
            action: './sa.elm.ob.scm.ad_process.ProposalManagement/CreatePoAttachmentUpload',
            target: "background_target",
            numCols: 4,
            align: 'center',
            height: '30px',
            redraw: function() {},
        });

        var submitbutton = isc.OBFormButton.create({
            title: OB.I18N.getLabel('OBUIAPP_AttachmentSubmit'),
            theForm: form,
            click: function() {
                var fileName, form = this.theForm;
                form.submitForm();
                form.popup.hide();
            }
        });

        var popup = isc.OBAttachmentsSubmitPopup.create({
            submitButton: submitbutton,
            addForm: form
        });
        form.popup = popup;
        popup.show();
    }

});



OB.Utilities.Action.set('openAttachmentInPopup', function(paramObj) {

    var dialog = isc.Dialog.create({
        width: 350,
        height: 150,
        canDragReposition: false,
        canDragResize: false,
        showCloseButton: true,
        showMaximizeButton: false,
        showMinimizeButton: false,
        title: OB.I18N.getLabel('escm_attachment_associated'),
        initWidget: function() {
            this.Super('initWidget', arguments);
        },
        message: paramObj.message != undefined ? paramObj.message : OB.I18N.getLabel('escm_attachment_notassociated'),
        autoSize: true,
        autoCenter: true,
        contentLayout: 'horizontal',
        backgroundColor: null,
        border: null,
        styleName: 'OBPopup',
        edgeCenterBackgroundColor: '#FFFFFF',
        bodyColor: 'transparent',
        bodyStyle: 'OBPopupBody',
        headerStyle: 'OBPopupHeader',
        messageStyle: 'OBDialogLabel',
        layoutMargin: 0,
        membersMargin: 0,
        showShadow: false,
        shadowDepth: 5
    });

    dialog.show();


});


OB.Utilities.uploadFinished = function(target, data) {
    var origButton = window[target];
    OB.Utilities.currentUploader = null;
    if (origButton && origButton.callback) {
        origButton.callback(data);

        if (data.attachments !== undefined && data.attachments.length > 0 && window[target].tabId === 'A0F3A7D17A834A93B3BD4D2C40E77AFE' && OB.Properties.Eut_AllowDMSIntegration !== undefined && OB.Properties.Eut_AllowDMSIntegration === 'Y') {
            var recentObj = data.attachments[0],
                age = [];
            for (i in data.attachments) {
                if (data.attachments[i].age < recentObj) {
                    recentObj = data.attachments[i];
                }
            }
            OB.RemoteCallManager.call('sa.elm.ob.utility.ad_process.digitalsignature.AttachmentDigitalSignature', {
                recordId: window[target].recordId,
                attachmentId: recentObj.id,
                tabId: window[target].tabId
            }, {}, function() {
                origButton.parentElement.view.refresh()
            });

        }

    }
};


OB.Utilities.sendToDMSCallback = function(target, data) {
    var origButton = window[target];
    isc.clearPrompt();
    origButton.parentElement.view.refresh()
    if (!data.error) {
        origButton.parentElement.view.messageBar.setMessage(isc.OBMessageBar.TYPE_SUCCESS, null, data.message);
    } else {
        origButton.parentElement.view.messageBar.setMessage(isc.OBMessageBar.TYPE_ERROR, null, data.message);
    }

};