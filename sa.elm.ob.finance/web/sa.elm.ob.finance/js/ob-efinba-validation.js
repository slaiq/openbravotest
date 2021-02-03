OB.EFINBA= {};
OB.EFINBA.QtyValidation = {};
//qty updation in budget adjustment addlines pop up
OB.EFINBA.QtyValidation.OnChangeAmount = function (item, view, form, grid) {
	  var incvalue =grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Adjamount'));
	  var decvalue = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Decadjamount'));
	  if (item.columnName === 'Decadjamount' && decvalue > 0) {
		    grid.setEditValue(item.rowNum, 'adjamount', Number('0'));
		  } else if (item.columnName === 'Adjamount' && incvalue > 0) {
		    grid.setEditValue(item.rowNum, 'decadjamount', Number('0'));
		  }
	  if((item.columnName === 'Decadjamount' && decvalue < 0 )|| (item.columnName === 'Adjamount' && incvalue < 0)){
		  isc.warn(OB.I18N.getLabel('Efin_AppliedAmountNegative'));				  
	  }
          return true;
	 
	};	
	//qty updation in budget Revision addlines pop up
	OB.EFINBA.QtyValidation.OnChangeIncDecAmountRevision = function (item, view, form, grid) {
		
			  var incvalue =grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Incamount'));
			  var decvalue = grid.getEditedCell(item.rowNum, grid.getFieldByColumnName('Decamount'));
			  if (item.columnName === 'Decamount' && decvalue > 0) {
				    grid.setEditValue(item.rowNum, 'incamount', Number('0'));
				  } else if (item.columnName === 'Incamount' && incvalue > 0) {
				    grid.setEditValue(item.rowNum, 'decamount', Number('0'));
				  }
			  if(item.columnName === 'Decamount' && decvalue < 0 || item.columnName === 'Incamount' && incvalue < 0){
				  isc.warn(OB.I18N.getLabel('Efin_AppliedAmountNegative'));				  
			  }
			  
		     return true;
			 
			};