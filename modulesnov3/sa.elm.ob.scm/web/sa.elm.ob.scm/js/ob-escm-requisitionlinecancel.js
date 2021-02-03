OB.ESCMRequisition = {};
OB.ESCMRequisition.Requisitionlinecancel = {};
var receiptsToAdd = [],
    receiptsTotal;
OB.ESCMRequisition.Requisitionlinecancel.OnLoad = function (view) {
  receiptsToAdd = [];
  receiptsTotal = 0;
  var form = view.theForm,
      linesGrid = form.getItem('m_requisitionline_id').canvas.viewGrid;
  linesGrid.selectionChanged = OB.ESCMRequisition.Requisitionlinecancel.selectionChanged;
};

OB.ESCMRequisition.Requisitionlinecancel.selectionChanged = function (record, state) {
	  this.Super('selectionChanged', arguments);
	  var reqLine = this.view.theForm.getItem('m_requisitionline_id').canvas.viewGrid;
	  var reason = this.view.theForm.getItem('reason').getValue();
	  var cancelReason = reqLine.getEditValue(reqLine.getRecordIndex(record), 'escmCancelReason');
	  if(cancelReason===undefined){
		  reqLine.setEditValue(reqLine.getRecordIndex(record), 'escmCancelReason', '');
	  }else{
	  reqLine.setEditValue(reqLine.getRecordIndex(record), 'escmCancelReason', reason);
	  }
	};
