//OB.EFIN = {};
OB.EFIN.BatchPosting = {};
var receiptsToAdd = [],
    receiptsTotal;
OB.EFIN.BatchPosting.OnLoad = function (view) {
  receiptsToAdd = [];
  receiptsTotal = 0;
  var form = view.theForm,
      receiptGrid = form.getItem('EM_Efin_Batch_Post').canvas.viewGrid;
  receiptGrid.selectionChanged = OB.EFIN.BatchPosting.selectionChanged;
};

OB.EFIN.BatchPosting.selectionChanged = function (record, state) {
  this.Super('selectionChanged', arguments);
  if ((receiptsToAdd.indexOf(arguments[0].id) === -1 && arguments[0].obSelected == true) || (receiptsToAdd.indexOf(arguments[0].id) != -1 && arguments[0].obSelected == false)) {
    if (arguments[0].obSelected == true) {
      receiptsToAdd.push(arguments[0].id);
      receiptsTotal = receiptsTotal + arguments[0].amount;
    } else {
      receiptsToAdd.removeAt(receiptsToAdd.indexOf(arguments[0].id));
      receiptsTotal = receiptsTotal - arguments[0].amount;
    }
    this.view.theForm.getItem('total').setValue(Number(receiptsTotal.toString()));
  }
};


OB.EFIN.BatchPosting.ClientSideValidation = function (view, actionHandlerCall, failureCallback) {
  actionHandlerCall();
  var grid = view.theForm.getItem('EM_Efin_Batch_Post').canvas.viewGrid,
      newCriteria = {};
  newCriteria.criteria = [];
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  grid.invalidateCache();
  view.theForm.getItem('receipt_batch_name').setValue("");
  view.theForm.redraw();
};