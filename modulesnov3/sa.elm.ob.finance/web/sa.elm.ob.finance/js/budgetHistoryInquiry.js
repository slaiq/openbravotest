OB.EFIN.OnHistoryChangeFunction = {};

OB.EFIN.OnHistoryChangeFunction.transactionTypeOnHistoryChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria; //newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}
OB.EFIN.OnHistoryChangeFunction.BudgetTypeOnHistoryChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}
OB.EFIN.OnHistoryChangeFunction.YearOnHistoryChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}
OB.EFIN.OnHistoryChangeFunction.FromAcctOnHistoryChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}
OB.EFIN.OnHistoryChangeFunction.ToAcctOnHistoryChangeFunction = function (item, view, form, grid) {
  var inquiryLinesGrid = form.getItem('line').canvas.viewGrid,
      newCriteria = {};
  selectedRecords = inquiryLinesGrid.getSelectedRecords();

  var revisiontype = item.getValue();
  if (item.getValue() === item.oldSelectedValue) {
    // only fetch new data if the selected value has changed.
    return;
  }
  item.oldSelectedValue = item.getValue();
  newCriteria = inquiryLinesGrid.addSelectedIDsToCriteria(inquiryLinesGrid.getCriteria(), true);
  newCriteria.criteria = newCriteria.criteria || [];
  // add dummy criterion to force fetch
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  inquiryLinesGrid.invalidateCache();

  form.redraw();
}

OB.EFIN.OnHistoryChangeFunction.onRefresh = function (view) {
  var grid = view.theForm.getItem('line').canvas.viewGrid,
      newCriteria = {};
  newCriteria.criteria = [];
  newCriteria.criteria.push(isc.OBRestDataSource.getDummyCriterion());
  grid.invalidateCache();
  view.theForm.redraw();
};
