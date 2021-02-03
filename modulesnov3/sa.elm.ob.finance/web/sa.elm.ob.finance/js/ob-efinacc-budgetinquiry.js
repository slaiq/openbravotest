OB.EFINACC = {};
OB.EFINACC.OnChangeFunction = {};

	
	OB.EFINACC.OnChangeFunction.onLoad = function (grid) { 
		  var allRecords = (grid.data.allRows) ? grid.data.allRows.length : 0;
		  for (i = 0; i < allRecords; i++) {
			 var record = grid[i];
		var acctdate = grid.getFieldByColumnName('acctDate');
		var acctdat=grid.getEditedCell(i, acctdate);
		var calendar = $.calendars.instance("ummalqura");
		var cal=calendar.newDate().fromJSDate(acctdat);
		var formattedDate = calendar.formatDate('dd-mm-yyyy', cal);
			  grid.setEditValue((i), acctdate, formattedDate+"");	
			  var  editedRecord = isc.addProperties({}, record);
			var ids =OB.EFIN.OnChangeFunction.ids(record.id);
		       record.id = ids;
			  grid.selectedIds.push(record.id);
			  grid.data.savedData.push(editedRecord);
		  }
		 
		};
		
			
			OB.EFIN.OnChangeFunction.onLoadModication = function (view){
				var form = view.theForm,
				revisionDate = form.getItem('Efin_Revison_Date').getValue(),
				calendar = $.calendars.instance("ummalqura"),
				currentHijriDate = calendar.newDate().fromJSDate(new Date()),
				formattedDate = calendar.formatDate('dd-mm-yyyy', currentHijriDate);
				form.getItem('Efin_Revison_Date').setValue(formattedDate+"");
			};
