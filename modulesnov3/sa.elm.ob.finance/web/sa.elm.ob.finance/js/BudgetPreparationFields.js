/*
 * Contributor; Gopalakrishnan
 ************************************************************************
*/

isc.defineClass('BudgetPreparation_FieldAmt', isc.Label);
isc.defineClass('BudgetPreparation_FieldAct', isc.Label);
isc.defineClass('BudgetPreparation_FieldFa', isc.Label);

isc.BudgetPreparation_FieldAmt.addProperties({
  height: 1,
  width: 100,
  initWidget: function () {
    this.setBackgroundColor('#33BBFF');
  }
});
isc.BudgetPreparation_FieldAct.addProperties({
  height: 1,
  width: 100,
  initWidget: function () {
    this.setBackgroundColor('#DFFF80');
  }
});
isc.BudgetPreparation_FieldFa.addProperties({
  height: 1,
  width: 100,
  initWidget: function () {
    this.setBackgroundColor('#FFB3D9');
  }
});
