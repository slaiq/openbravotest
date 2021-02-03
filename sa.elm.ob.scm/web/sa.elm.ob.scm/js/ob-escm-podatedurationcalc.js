//OB.ESCMPO = {};
//OB.ESCMPO.OnChangeFunction = {};
//OB.ESCM.POValidation = {};
//OB.ESCM.BidValidation = {};
//
//OB.ESCM.PRValidation = {};
//
//
//
//OB.ESCMPO.OnChangeFunction.getOnHoldFromDate = function(item, view, form, grid) {
//    var changeStatus = form.getItem('changestatus').getValue();
//    var orderId = view.parentWindow.view.getContextInfo().C_Order_ID;
//    if (changeStatus == 'ESCM_EOHLD') {
//        callback = function(response, data, request) {
//            if (data != undefined && data.OnHoldFromDate != null) {
//                form.getItem('onholdfromdate').setValue(data.OnHoldFromDate);
//            } else {
//                form.getItem('onholdfromdate').setValue(null);
//            }
//        };
//        //call back function to get the calcFromDate
//        OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POChangeActionDurationCalc', {
//            orderId: orderId,
//            action: "calcFromDate"
//        }, {}, callback);
//    }
//};
//
//OB.ESCMPO.OnChangeFunction.calcDateDuration = function(item, view, form, grid) {
//    var onHoldFromDate = form.getItem('onholdfromdate').getValue();
//    var onHoldToDate = form.getItem('onholdtodate').getValue();
//
//    callback = function(response, data, request) {
//        if (data != undefined && data.Duration != null) {
//            form.getItem('duration').setValue(data.Duration);
//        }
//        if (data != undefined && data.PeriodType != null) {
//            form.getItem('periodtype').setValue(data.PeriodType);
//        }
//    };
//    //call back function to get the datedurationcalc
//    OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POChangeActionDurationCalc', {
//        onHoldFromDate: onHoldFromDate,
//        onHoldToDate: onHoldToDate,
//        action: "calcDuration"
//    }, {}, callback);
//};
//OB.ESCMPO.OnChangeFunction.calcOnHoldToDate = function(item, view, form, grid) {
//    var onHoldFromDate = form.getItem('onholdfromdate').getValue();
//    var periodType = form.getItem('periodtype').getValue();
//    var duration = form.getItem('duration').getValue();
//
//    if (duration < 1) {
//        isc.warn(OB.I18N.getLabel('ESCM_DurationCantBeNegative'));
//        return false;
//    }
//    callback = function(response, data, request) {
//        if (data != undefined && data.OnHoldEndDate != null) {
//            form.getItem('onholdtodate').setValue(data.OnHoldEndDate);
//        }
//    };
//    //call back function to get the datedurationcalc
//    OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POChangeActionDurationCalc', {
//        onHoldFromDate: onHoldFromDate,
//        periodType: periodType,
//        duration: duration,
//        action: "calcOnHoldToDate"
//    }, {}, callback);
//};
//
//// list the status in popup based on approval status
//OB.ESCM.POValidation.OnStatusLoad = function(view) {
//    var approvalStatus = view.parentWindow.view.getContextInfo().inpemEscmAppstatus;
//    var statusMap = view.theForm.getItem('changestatus').valueMap;
//    view.theForm.getItem('changestatus').valueMap = {};
//    if (approvalStatus != undefined && approvalStatus == 'ESCM_AP') {
//        view.theForm.getItem('changestatus').valueMap['ESCM_WD'] = statusMap['ESCM_WD']
//        view.theForm.getItem('changestatus').valueMap['ESCM_OHLD'] = statusMap['ESCM_OHLD']
//
//        // Hide cancel option for Purchase agreement	
//        var orderType = view.parentWindow.view.getContextInfo().inpemEscmOrdertype;
//        if (orderType != undefined && orderType != 'PUR_AG') {
//            view.theForm.getItem('changestatus').valueMap['ESCM_CAN'] = statusMap['ESCM_CAN']
//        }
//    }
//    if (approvalStatus != undefined && approvalStatus == 'ESCM_WD') {
//        view.theForm.getItem('changestatus').valueMap['ESCM_RWD'] = statusMap['ESCM_RWD']
//    }
//    if (approvalStatus != undefined && approvalStatus == 'ESCM_OHLD') {
//        view.theForm.getItem('changestatus').valueMap['ESCM_ROHLD'] = statusMap['ESCM_ROHLD']
//        view.theForm.getItem('changestatus').valueMap['ESCM_EOHLD'] = statusMap['ESCM_EOHLD']
//    }
//
//};
//
////list the action in popup based on bid type
//OB.ESCM.BidValidation.OnActionLoad = function(view) {
//    var bidType = view.parentWindow.view.getContextInfo().inpbidtype;
//    var actionMap = view.theForm.getItem('Action').valueMap;
//    var delActionMap = view.theForm.getItem('RemoveTransaction').valueMap;
//    var removeAllBidPref= OB.Properties.ESCM_RemBidRelTrans;
//    view.theForm.getItem('Action').valueMap = {};
//    view.theForm.getItem('RemoveTransaction').valueMap = {};
//    if (bidType != undefined && bidType == 'DR') {
//        view.theForm.getItem('Action').valueMap['PRO'] = actionMap['PRO']
//        view.theForm.getItem('Action').valueMap['PEECD'] = actionMap['PEECD']
//        if(removeAllBidPref=='Y')
//        view.theForm.getItem('Action').valueMap['DELTRNS'] = actionMap['DELTRNS']
//        
//        view.theForm.getItem('RemoveTransaction').valueMap['UPEE'] = delActionMap['UPEE']
//        view.theForm.getItem('RemoveTransaction').valueMap['UPRO'] = delActionMap['UPRO']
//        view.theForm.getItem('RemoveTransaction').valueMap['UBID'] = delActionMap['UBID']
//    }
//    if (bidType != undefined && bidType!='DR') {
//    	 view.theForm.getItem('Action').valueMap['OEECD'] = actionMap['OEECD']
//    	 view.theForm.getItem('Action').valueMap['PRO'] = actionMap['PRO']
//    	 view.theForm.getItem('Action').valueMap['TEECD'] = actionMap['TEECD']
//         view.theForm.getItem('Action').valueMap['PEECD'] = actionMap['PEECD']
//    	 if(removeAllBidPref=='Y')
//         view.theForm.getItem('Action').valueMap['DELTRNS'] = actionMap['DELTRNS']
//    	 
//    	 
//    	 view.theForm.getItem('RemoveTransaction').valueMap['UPEE'] = delActionMap['UPEE']
//    	 view.theForm.getItem('RemoveTransaction').valueMap['UTEE'] = delActionMap['UTEE']
//    	 view.theForm.getItem('RemoveTransaction').valueMap['UOEE'] = delActionMap['UOEE']
//    	 view.theForm.getItem('RemoveTransaction').valueMap['UPRO'] = delActionMap['UPRO']
//    	 view.theForm.getItem('RemoveTransaction').valueMap['UBID'] = delActionMap['UBID']
//    }
//   
//
//};
//
////List the action in popup based on process type
//OB.ESCM.PRValidation.OnPrActionLoad = function(view) {
//    var processType = view.parentWindow.view.getContextInfo().inpemEscmProcesstype;
//    var actionMap = view.theForm.getItem('praction').valueMap;
//    view.theForm.getItem('praction').valueMap = {};
//    if (processType != undefined && processType == 'DP') {
//        view.theForm.getItem('praction').valueMap['ESCM_BID'] = actionMap['ESCM_BID']
//        view.theForm.getItem('praction').valueMap['ESCM_PR'] = actionMap['ESCM_PR']
//        view.theForm.getItem('praction').valueMap['ESCM_RE'] = actionMap['ESCM_RE']
//    }
//    if (processType != undefined && (processType == 'PB' || processType == 'LB')) {
//    		view.theForm.getItem('praction').valueMap['ESCM_BID'] = actionMap['ESCM_BID']
//        view.theForm.getItem('praction').valueMap['ESCM_RE'] = actionMap['ESCM_RE']
//    }
//};