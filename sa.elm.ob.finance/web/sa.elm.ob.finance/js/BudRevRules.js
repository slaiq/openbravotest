OB.EFINBudRevRules = OB.EFINBudRevRules || {};
OB.EFINBudRevRules.OnChangeFunctions = OB.EFINBudRevRules.OnChangeFunctions || {};

OB.EFINBudRevRules.OnChangeFunctions.enableBudRevRule_changeOperatorPercentage = function(item, view, form, grid) {
	//set empty values.
	//form.setItemValue('operators', null);
	form.setItemValue('percentage', null);
}