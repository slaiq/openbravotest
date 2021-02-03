/**
 * 
 * @author Gokul on 10/06/2020
 */


OB.ESCMPO = {};
OB.ESCMPO.OnChangeFunction = {};
OB.ESCM.POValidation = {};
OB.ESCM.BidValidation = {};
OB.ESCM.PRValidation = {};



OB.ESCMPO.OnChangeFunction.getContractDuration = function(item, view, form, grid) {
	var current_datetime = form.getItem('contractstartdate').getValue();
	var contractEndDate = form.getItem('contractenddate').getValue();
	var contractDuration = form.getItem('contractduration').getValue();
	var onBoardDateh = form.getItem('onboarddateh').getValue();
	var periodType = form.getItem('periodtype').getValue();
	var proposalId = view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID;
	var contractStartDate = current_datetime;
	var formatted_date =  new Date();
	var formatted_date1 =  new Date();
	if (contractStartDate !== null) {
		 formatted_date = ('0' + contractStartDate.getDate()).slice(-2)+ "-" + ('0' + (contractStartDate.getMonth() + 1 )).slice(-2) + "-" + contractStartDate.getFullYear();
	}
	if (contractEndDate !== null) {
		formatted_date1 = ('0' + contractEndDate.getDate()).slice(-2) + "-" +('0' + (contractEndDate.getMonth() + 1 )).slice(-2)  + "-" + contractEndDate.getFullYear();
	}
	//console.log(formatted_date);
	//console.log(formatted_date1);

	if (contractStartDate !== null && contractEndDate !== null) {
		callback = function(response, data, request) {
			if (data !== undefined && data.periodType !== null) {
				form.getItem('periodtype').setValue(data.periodType);
			}
			if (data !== undefined && data.duration !== null) {
				form.getItem('contractduration').setValue(data.duration);
			}
		};
		//call back function to get the getContractDuration
		OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CreatePOSubmitValidations', {
			proposalId: proposalId,
			contractStartDate: formatted_date,
			contractEndDate: formatted_date1,
			contractDuration: contractDuration,
			periodType: periodType,
			action: "getConractDuration"
		}, {}, callback);
	}
};


OB.ESCMPO.OnChangeFunction.getContractEndDatebyContractDuration = function(item, view, form, grid) {
	var contractStartDate = form.getItem('contractstartdate').getValue();
	var contractDuration = form.getItem('contractduration').getValue();
	var periodType = form.getItem('periodtype').getValue();
	var proposalId = view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID;
	var formatted_date = "";
	if (contractStartDate !== null) {
		formatted_date = ('0' + contractStartDate.getDate()).slice(-2)+ "-" + ('0' + (contractStartDate.getMonth() + 1 )).slice(-2) + "-" + contractStartDate.getFullYear();
	}
	
	if (contractStartDate !== null && periodType !== null && contractDuration !== null) {
		callback = function(response, data, request) {
			if (data !== undefined && data.contractEndDate !== null) {
				form.getItem('contractenddate').dateTextField.setValue(data.contractEndDate);
				form.getItem('contractenddate').setValue(data.contractEndDate);
			}
		};
		//call back function to get the getContractEndDate
		OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CreatePOSubmitValidations', {
			proposalId: proposalId,
			contractStartDate: formatted_date,
			contractDuration: contractDuration,
			periodType: periodType,
			action: "getConractEndDateByContractDuration"
		}, {}, callback);
		
	}
}



OB.ESCMPO.OnChangeFunction.getContractDurationByStartDate = function(item, view, form, grid) {
	var current_datetime = form.getItem('contractstartdate').getValue();
	var contractEndDate = form.getItem('contractenddate').getValue();
	var contractDuration = form.getItem('contractduration').getValue();
	var periodType = form.getItem('periodtype').getValue();
	var onBoardDateh = form.getItem('onboarddateh').getValue();
	var proposalId = view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID;
	var contractStartDate = current_datetime;
	var formatted_date = "";
	var formatted_date1 ="";
	if (contractStartDate !== null) {
		form.getItem('onboarddateh').setValue(contractStartDate);
		formatted_date = ('0' + contractStartDate.getDate()).slice(-2)+ "-" + ('0' + (contractStartDate.getMonth() + 1 )).slice(-2) + "-" + contractStartDate.getFullYear();
	}
	if (contractEndDate !== null && contractEndDate!== undefined) {
		formatted_date1 = ('0' + contractEndDate.getDate()).slice(-2) + "-" +('0' + (contractEndDate.getMonth() + 1 )).slice(-2)  + "-" + contractEndDate.getFullYear();
	}
	//console.log(formatted_date);
	//console.log(formatted_date1);

	if (contractStartDate !== null && contractEndDate !== null) {
		callback = function(response, data, request) {
			if (data !== undefined && data.contractEndDate !== null && data.gregDate !== null) {
				form.getItem('contractenddate').setValue(data.contractEndDate);
				form.getItem('onboarddateg').setValue(data.gregDate);
			}
		};
		//call back function to get the getContractDuration
		OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CreatePOSubmitValidations', {
			proposalId: proposalId,
			contractStartDate: formatted_date,
			contractEndDate: formatted_date1,
			contractDuration: contractDuration,
			periodType: periodType,
			action: "getConractDurationByStartDate"
		}, {}, callback);
	}
};





OB.ESCMPO.OnChangeFunction.getGregorianDate = function(item, view, form, grid) {
	var current_datetime = form.getItem('contractstartdate').getValue();
	var contractEndDate = form.getItem('contractenddate').getValue();
	var contractDuration = form.getItem('contractduration').getValue();
	var periodType = form.getItem('periodtype').getValue();
	var onBoardDateh = form.getItem('onboarddateh').getValue();
	var formatted_date = "";
	if (onBoardDateh !== null) {
		//var formatted_date = ('0' + contractStartDate.getDate()).slice(-2)+ "-" + (contractStartDate.getMonth() + 1) + "-" + contractStartDate.getFullYear();
		formatted_date = ('0' + onBoardDateh.getDate()).slice(-2)+ "-" + ('0' + (onBoardDateh.getMonth() + 1 )).slice(-2) + "-" + onBoardDateh.getFullYear();
	}
	var proposalId = view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID;
	var contractStartDate = current_datetime;


	if (contractStartDate !== null && onBoardDateh !== null) {
		callback = function(response, data, request) {
			if (data !== undefined && data.gregDate !== null) {
				form.getItem('onboarddateg').setValue(data.gregDate);
			}
		};
		//call back function to get the getContractDuration
		OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CreatePOSubmitValidations', {
			proposalId: proposalId,
			onBoardDateh: formatted_date,
			action: "getGregorianDate"
		}, {}, callback);
	}
};



OB.ESCMPO.OnChangeFunction.getPoAmountCalcPer = function(item, view, form, grid) {
	var advPaymentPer = form.getItem('advpaymentper').getValue();
	var totalAmount = view.parentWindow.view.getContextInfo().inptotalamount;
	 var proposalId =null;
	if(view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID !== undefined && view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID!=null)
	{
      proposalId = view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID;
	}
    if(view.parentWindow.view.getContextInfo().Escm_Proposalevl_Event_ID!==undefined && view.parentWindow.view.getContextInfo().Escm_Proposalevl_Event_ID!=null){
    	proposalId = view.sourceView.getContextInfo().inpescmProposalmgmtId;
    }


	if (advPaymentPer !== null) {
		callback = function(response, data, request) {
			if (data !== undefined && data.amount !== null) {
				form.getItem('advpaymentamt').setValue(data.amount);
			}
		};
		//call back function to get the getContractDuration
		OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CreatePOSubmitValidations', {
			proposalId: proposalId,
			advPaymentPer: advPaymentPer,
			totalAmount: totalAmount,
			action: "getPoAmountCalcPer"
		}, {}, callback);
	}
};


OB.ESCMPO.OnChangeFunction.getPoAmountCalcAmount = function(item, view, form, grid) {
	var advPaymentAmt = form.getItem('advpaymentamt').getValue();
	var totalAmount = view.parentWindow.view.getContextInfo().inptotalamount;
	var proposalId = null;
	if(view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID !== undefined && view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID!=null)
	{
      proposalId = view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID;
	}
    if(view.parentWindow.view.getContextInfo().Escm_Proposalevl_Event_ID!==undefined && view.parentWindow.view.getContextInfo().Escm_Proposalevl_Event_ID!=null){
    	proposalId = view.sourceView.getContextInfo().inpescmProposalmgmtId;
    }


	if (advPaymentAmt !== null) {
		callback = function(response, data, request) {
			if (data !== undefined && data.amount !== null) {
				form.getItem('advpaymentper').setValue(data.amount);
			}
		};
		//call back function to get the getContractDuration
		OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CreatePOSubmitValidations', {
			proposalId: proposalId,
			advPaymentAmt: advPaymentAmt,
			totalAmount: totalAmount,
			action: "getPoAmountCalcAmount"
		}, {}, callback);
	}
};



OB.ESCM.POValidation.OnPopupLoad = function(view) {
	var proposalType =null;
	var proposalAttrId = null;
	var totalAmount = view.parentWindow.view.getContextInfo().inptotalamount;
	if(view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID !== undefined && view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID!=null)
		{
	var proposalId = view.parentWindow.view.getContextInfo().Escm_Proposalmgmt_ID;
		}
	if(view.parentWindow.view.getContextInfo().Escm_Proposalevl_Event_ID!==undefined && view.parentWindow.view.getContextInfo().Escm_Proposalevl_Event_ID!=null){
		 proposalAttrId = view.sourceView.getContextInfo().inpescmProposalAttrId;
	}
	var orgId = view.parentWindow.view.getContextInfo().inpadOrgId;
	 proposalType = view.parentWindow.view.getContextInfo().inpproposaltype;
    
	if ((totalAmount !== null && proposalId!=null && orgId!=null) || proposalAttrId!=null) {
		callback = function(response, data, request) {
			if (data !== undefined && data.poType !== null) {
				if(data.poType==="PUR"){
					//document.getElementsByName("contractstartdate").style.display='block';
					//item.theForm.items[1].hide
					view.theForm.hideItem('contractstartdate')
					view.theForm.hideItem('contractenddate')
					view.theForm.hideItem('onboarddateh')
					view.theForm.hideItem('onboarddateg')
					view.theForm.hideItem('contractduration')
					view.theForm.hideItem('periodtype')
					view.theForm.hideItem('advpaymentper')
					view.theForm.hideItem('advpaymentamt')
					//form.setVisible(x, false);
//					var contractEndDate = form.getItem('contractenddate').getValue();
//					var contractDuration = form.getItem('contractduration').getValue();
//					var periodType = form.getItem('periodtype').getValue();
//					var onBoardDateh = form.getItem('onboarddateh').getValue();
				}
				// Hide bank guarantee workbench for direct proposal
				if(data.proposalType == 'DR') {
					view.theForm.hideItem('ESCM_BGWorkbench')
				}
			}
		};
		//call back function to get the getContractDuration
		OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.CreatePOSubmitValidations', {
			proposalId: proposalId,
			orgId: orgId,
			totalAmount: totalAmount,
			proposalAttrId:proposalAttrId,
			action: "getPoType"
		}, {}, callback);
};
	
	

};





OB.ESCMPO.OnChangeFunction.getOnHoldFromDate = function(item, view, form, grid) {
    var changeStatus = form.getItem('changestatus').getValue();
    var orderId = view.parentWindow.view.getContextInfo().C_Order_ID;
    if (changeStatus == 'ESCM_EOHLD') {
        callback = function(response, data, request) {
            if (data != undefined && data.OnHoldFromDate != null) {
                form.getItem('onholdfromdate').setValue(data.OnHoldFromDate);
            } else {
                form.getItem('onholdfromdate').setValue(null);
            }
        };
        //call back function to get the calcFromDate
        OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POChangeActionDurationCalc', {
            orderId: orderId,
            action: "calcFromDate"
        }, {}, callback);
    }
};

OB.ESCMPO.OnChangeFunction.calcDateDuration = function(item, view, form, grid) {
    var onHoldFromDate = form.getItem('onholdfromdate').getValue();
    var onHoldToDate = form.getItem('onholdtodate').getValue();

    callback = function(response, data, request) {
        if (data != undefined && data.Duration != null) {
            form.getItem('duration').setValue(data.Duration);
        }
        if (data != undefined && data.PeriodType != null) {
            form.getItem('periodtype').setValue(data.PeriodType);
        }
    };
    //call back function to get the datedurationcalc
    OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POChangeActionDurationCalc', {
        onHoldFromDate: onHoldFromDate,
        onHoldToDate: onHoldToDate,
        action: "calcDuration"
    }, {}, callback);
};
OB.ESCMPO.OnChangeFunction.calcOnHoldToDate = function(item, view, form, grid) {
    var onHoldFromDate = form.getItem('onholdfromdate').getValue();
    var periodType = form.getItem('periodtype').getValue();
    var duration = form.getItem('duration').getValue();

    if (duration < 1) {
        isc.warn(OB.I18N.getLabel('ESCM_DurationCantBeNegative'));
        return false;
    }
    callback = function(response, data, request) {
        if (data != undefined && data.OnHoldEndDate != null) {
            form.getItem('onholdtodate').setValue(data.OnHoldEndDate);
            form.getItem('onholdtodate').dateTextField.setValue(data.OnHoldEndDate);
        }
    };
    //call back function to get the datedurationcalc
    OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.POChangeActionDurationCalc', {
        onHoldFromDate: onHoldFromDate,
        periodType: periodType,
        duration: duration,
        action: "calcOnHoldToDate"
    }, {}, callback);
};

// list the status in popup based on approval status
OB.ESCM.POValidation.OnStatusLoad = function(view) {
    var approvalStatus = view.parentWindow.view.getContextInfo().inpemEscmAppstatus;
    var statusMap = view.theForm.getItem('changestatus').valueMap;
    view.theForm.getItem('changestatus').valueMap = {};
    if (approvalStatus != undefined && approvalStatus == 'ESCM_AP') {
        view.theForm.getItem('changestatus').valueMap['ESCM_WD'] = statusMap['ESCM_WD']
        view.theForm.getItem('changestatus').valueMap['ESCM_OHLD'] = statusMap['ESCM_OHLD']

        // Hide cancel option for Purchase agreement	
        var orderType = view.parentWindow.view.getContextInfo().inpemEscmOrdertype;
        if (orderType != undefined && orderType != 'PUR_AG') {
            view.theForm.getItem('changestatus').valueMap['ESCM_CAN'] = statusMap['ESCM_CAN']
        }
    }
    if (approvalStatus != undefined && approvalStatus == 'ESCM_WD') {
        view.theForm.getItem('changestatus').valueMap['ESCM_RWD'] = statusMap['ESCM_RWD']
    }
    if (approvalStatus != undefined && approvalStatus == 'ESCM_OHLD') {
        view.theForm.getItem('changestatus').valueMap['ESCM_ROHLD'] = statusMap['ESCM_ROHLD']
        view.theForm.getItem('changestatus').valueMap['ESCM_EOHLD'] = statusMap['ESCM_EOHLD']
    }

};

//list the action in popup based on bid type
OB.ESCM.BidValidation.OnActionLoad = function(view) {
    var bidType = view.parentWindow.view.getContextInfo().inpbidtype;
    var actionMap = view.theForm.getItem('Action').valueMap;
    var delActionMap = view.theForm.getItem('RemoveTransaction').valueMap;
    var removeAllBidPref= OB.Properties.ESCM_RemBidRelTrans;
    view.theForm.getItem('Action').valueMap = {};
    view.theForm.getItem('RemoveTransaction').valueMap = {};
    if (bidType != undefined && bidType == 'DR') {
        view.theForm.getItem('Action').valueMap['PRO'] = actionMap['PRO']
        view.theForm.getItem('Action').valueMap['PEECD'] = actionMap['PEECD']
        if(removeAllBidPref=='Y')
        view.theForm.getItem('Action').valueMap['DELTRNS'] = actionMap['DELTRNS']
        
        view.theForm.getItem('RemoveTransaction').valueMap['UPEE'] = delActionMap['UPEE']
        view.theForm.getItem('RemoveTransaction').valueMap['UPRO'] = delActionMap['UPRO']
        view.theForm.getItem('RemoveTransaction').valueMap['UBID'] = delActionMap['UBID']
    }
    if (bidType != undefined && bidType!='DR') {
    	 view.theForm.getItem('Action').valueMap['OEECD'] = actionMap['OEECD']
    	 view.theForm.getItem('Action').valueMap['PRO'] = actionMap['PRO']
    	 view.theForm.getItem('Action').valueMap['TEECD'] = actionMap['TEECD']
         view.theForm.getItem('Action').valueMap['PEECD'] = actionMap['PEECD']
    	 if(removeAllBidPref=='Y')
         view.theForm.getItem('Action').valueMap['DELTRNS'] = actionMap['DELTRNS']
    	 
    	 
    	 view.theForm.getItem('RemoveTransaction').valueMap['UPEE'] = delActionMap['UPEE']
    	 view.theForm.getItem('RemoveTransaction').valueMap['UTEE'] = delActionMap['UTEE']
    	 view.theForm.getItem('RemoveTransaction').valueMap['UOEE'] = delActionMap['UOEE']
    	 view.theForm.getItem('RemoveTransaction').valueMap['UPRO'] = delActionMap['UPRO']
    	 view.theForm.getItem('RemoveTransaction').valueMap['UBID'] = delActionMap['UBID']
    }
   

};

//List the action in popup based on process type
OB.ESCM.PRValidation.OnPrActionLoad = function(view) {
    var processType = view.parentWindow.view.getContextInfo().inpemEscmProcesstype;
    var actionMap = view.theForm.getItem('praction').valueMap;
    view.theForm.getItem('praction').valueMap = {};
    if (processType != undefined && processType == 'DP') {
        view.theForm.getItem('praction').valueMap['ESCM_BID'] = actionMap['ESCM_BID']
        view.theForm.getItem('praction').valueMap['ESCM_PR'] = actionMap['ESCM_PR']
        view.theForm.getItem('praction').valueMap['ESCM_RE'] = actionMap['ESCM_RE']
    }
    if (processType != undefined && (processType == 'PB' || processType == 'LB')) {
    		view.theForm.getItem('praction').valueMap['ESCM_BID'] = actionMap['ESCM_BID']
        view.theForm.getItem('praction').valueMap['ESCM_RE'] = actionMap['ESCM_RE']
    }
};
