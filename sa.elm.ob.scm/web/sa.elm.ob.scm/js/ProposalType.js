OB.ESCMPOProposalType = OB.ESCMPOProposalType || {};
OB.ESCMPOProposalType.OnChangeFunctions = OB.ESCMPOProposalType.OnChangeFunctions || {};
OB.ESCM.OEE = {};

OB.ESCMPOProposalType.OnChangeFunctions.proposalType_changeDepartmentName = function(item, view, form, grid) {
    //set empty values.
    form.setItemValue('departmentName', null);
    //Task #7867 
    //After changing the proposal type column 
    //resetting the bid,supplier,supplier branch to empty
    form.getFieldFromColumnName('Escm_Bidmgmt_ID').setValue('');
    form.getFieldFromColumnName('Supplier').setValue('');
    form.getFieldFromColumnName('Branchname').setValue('');

    if (item.getValue() == 'LD' || item.getValue() == 'TR') {
        form.setItemValue('needEvaluation', true);
    }
}


//On change function for role window- Header tab - fields name[include secure pr price, exclude secure pr price]
OB.ESCMPOProposalType.OnChangeFunctions.includeprice = function(item, view, form, grid) {
    if (form.getItem('escmIsprocurecommitee').getValue()) {
        form.setItemValue('escmIsExcludesecuredprice', false);
    }
}

OB.ESCMPOProposalType.OnChangeFunctions.excludePrice = function(item, view, form, grid) {
    if (form.getItem('escmIsExcludesecuredprice').getValue()) {
        form.setItemValue('escmIsprocurecommitee', false);
    }
}


OB.ESCM.OEE.ClientSideValidation = function(view, actionHandlerCall, failureCallback) {
    var contextInfo = view.sourceView.getContextInfo();
    var me = view;
    var allProperties = this.getUnderLyingRecordContext(false, true, false, true);
    allProperties._params = contextInfo;

    // allow to add external parameters
    isc.addProperties(allProperties._params, this.externalParams);

    var callback = function(response, data, request) {
            if (data != undefined && data.message != undefined) {
                var message = data.message.text;
                var noButton = isc.OBFormButton.create({
                    title: OB.I18N.getLabel('Escm_no'),
                    click: function() {
                        this.topElement.cancelClick();
                        view.closeClick();
                    }
                });

                var YesButton = isc.OBFormButton.create({
                    title: OB.I18N.getLabel('OBUISC_Dialog.YES_BUTTON_TITLE'),
                    message: data.message,
                    click: function() {
                        this.topElement.cancelClick();
                        me.setHeight(483);
                        me.showProcessing(true);
                        OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.OEESubmitActionHandler', {
                            oeeId: contextInfo.Escm_Openenvcommitee_ID,
                            notes: me.theForm.getItem('Comments').getEnteredValue(),
                            action: "getBGDetails",
                            validation: this.message
                        }, {}, callback);

                    }
                });

                var confirmation = isc.confirm(message, {
                    isModal: true,
                    showModalMask: true,
                    autoSize: false,
                    overflow: "visible",
                    width: 500,
                    height: 300,
                    title: OB.I18N.getLabel('ESCM_OEE_proceed'),
                    toolbarButtons: [YesButton, noButton]
                });

            } else {
                view.setHeight(483);
                actionHandlerCall();
            }
        };

    if (contextInfo.inpstatus === 'DR') {
        OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.OEESubmitActionHandler', {
            oeeId: contextInfo.Escm_Openenvcommitee_ID,
            action: "getBGDetails"
        }, {}, callback);
    } else {
        view.setHeight(483);
        actionHandlerCall();
    }
};


OB.ESCM.OEE.onLoad = function(view) {
    var contextInfo = view.sourceView.getContextInfo();
    // set height to bring button above
    view.setHeight(200);
    view.parentElement.parentElement.setWidth(798);
    view.parentElement.parentElement.setHeight(515);
    view.theForm.getItems()[0].setWidth(200);
    view.theForm.getItem('Comments').setWidth(350);
    view.theForm.getItem('Comments').setHeight(75);
    if (contextInfo.inpstatus === 'DR') {
        view.parentElement.parentElement.setTitle(OB.I18N.getLabel('ESCM_OEEsubmit'));
    } else {
        view.parentElement.parentElement.setTitle(OB.I18N.getLabel('ESCM_OEEreactivate'));
    }

    // Remove unwanted button 
    var buttons = view.theForm.parentElement.parentElement.members[2].members[0].members;
    var btn_submit, btn_submit_spacer, btn_reactivate, btn_reactivate_spacer, btn_cancel, btn_cancel_spacer;

    for (var i = 0; i < buttons.length; i++) {
        var btnComponent = view.theForm.parentElement.parentElement.members[2].members[0].members[i];
        if (contextInfo.inpstatus === 'DR') {
            if (btnComponent._buttonValue == 'CO') {
                btn_submit = view.theForm.parentElement.parentElement.members[2].members[0].members[i];
                btn_submit_spacer = view.theForm.parentElement.parentElement.members[2].members[0].members[i + 1];
            }

            if (btnComponent.title === 'Cancel' || (btnComponent.baseStyle === 'OBFormButton' && btnComponent._buttonValue === undefined)) {
                btn_cancel = view.theForm.parentElement.parentElement.members[2].members[0].members[i];
                btn_cancel_spacer = view.theForm.parentElement.parentElement.members[2].members[0].members[i];
            }
        } else {
            if (btnComponent._buttonValue == 'RE') {
                btn_reactivate = view.theForm.parentElement.parentElement.members[2].members[0].members[i];
                btn_reactivate_spacer = view.theForm.parentElement.parentElement.members[2].members[0].members[i + 1];
            }
        }

        if (btnComponent.title === 'Cancel' || (btnComponent.baseStyle === 'OBFormButton' && btnComponent._buttonValue === undefined)) {
            btn_cancel = view.theForm.parentElement.parentElement.members[2].members[0].members[i];
            btn_cancel_spacer = view.theForm.parentElement.parentElement.members[2].members[0].members[i];
        }
    }

    view.theForm.parentElement.parentElement.members[2].members[0].removeMember(view.theForm.parentElement.parentElement.members[2].members[0].members);
    if (contextInfo.inpstatus === 'DR') {
        view.theForm.parentElement.parentElement.members[2].members[0].addMember(btn_submit);
        view.theForm.parentElement.parentElement.members[2].members[0].addMember(btn_submit_spacer);
        view.theForm.parentElement.parentElement.members[2].members[0].addMember(btn_cancel);
        view.theForm.parentElement.parentElement.members[2].members[0].addMember(btn_cancel_spacer);
    } else {
        view.theForm.parentElement.parentElement.members[2].members[0].addMember(btn_reactivate);
        view.theForm.parentElement.parentElement.members[2].members[0].addMember(btn_reactivate_spacer);
        view.theForm.parentElement.parentElement.members[2].members[0].addMember(btn_cancel);
        view.theForm.parentElement.parentElement.members[2].members[0].addMember(btn_cancel_spacer);
    }
};