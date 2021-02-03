/*
 * Isomorphic SmartClient
 * Version v10.0d_2014-02-13 (2014-02-13)
 * Copyright(c) 1998 and beyond Isomorphic Software, Inc. All rights reserved.
 * "SmartClient" is a trademark of Isomorphic Software, Inc.
 *
 * licensing@smartclient.com
 *
 * http://smartclient.com/license
 */

// This file creates a mini-calendar that is used to pick a date, for example, you might have a
// button next to a form date field that brings this file up.
//>	@class	DateGrid
//
// A ListGrid subclass that manages calendar views.
//
// @treeLocation Client Reference/Forms
// @visibility external
//<
if (isc.ListGrid == null) {
	isc.Log.logInfo("Source for DateGrid included in this module, but required " + "superclass (ListGrid) is not loaded. This can occur if the Forms module is "
			+ "loaded without the Grids module. DateGrid class will not be defined within " + "this page.", "moduleDependencies");
}
else {

	// create a customized ListGrid to show the days in a month
	isc.ClassFactory.defineClass("HijriDateGrid", "ListGrid");

	isc.HijriDateGrid.addProperties({
		width : 20,
		height : 10,
		cellHeight : 20,
		autoFitData : "vertical",
		minFieldWidth : 50,
		autoFitMaxRows : 5,
		useCellRollOvers : true,
		canSelectCells : true,
		leaveScrollbarGap : false,
		canResizeFields : false,
		headerButtonProperties : {
			padding : 0
		},
		headerHeight : 35,
		canSort : false,
		canEdit : false,

		showSortArrow : isc.ListGrid.NONE,
		showFiscalYear : false,
		showFiscalWeek : false,
		showCalendarWeek : false,

		loadingDataMessage : "",
		alternateRecordStyles : false,

		showHeaderMenuButton : false,
		showHeaderContextMenu : false,

		cellPadding : 0,

		// we need to locate rows by cell-value, not PK or whatever else
		locateRowsBy : "targetCellValue",

		fiscalYearFieldTitle : "Year",
		weekFieldTitle : "Wk",

		canReorderFields : false,

		bodyProperties : {
			canSelectOnRightMouse : false,
			overflow : "visible"
		},

		headerProperties : {
			overflow : "visible"
		},

		currentRecords : [],

		initWidget : function() {
			this.shortDayNames = getHijriDayTitles();
			this.shortDayTitles = getHijriDayTitles();
			this.shortMonthNames = isc.Date.getShortMonthNames();

			this.Super("initWidget", arguments);

			this.refreshUI();
		},

		getTitleField : function() {
			return null;
		},

		getCellAlign : function(record, rowNum, colNum) {
			return "center";
		},

		formatCellValue : function(value, record, rowNum, colNm) {
			if (value && value.getDate) {
				if (record[colNm].date)
					return record[colNm].date;
				else
					return value.getDate();
			}
			return "" + value;
		},
		getHijCellDate : function(record, rowNum, colNum) {
			var calendar = $.calendars.instance("ummalqura");
			var date;
			switch (colNum) {

			case 0:
				date = record.الأحد;
				break;
			case 1:
				date = record.السبت;
				break;
			case 2:
				date = record.الجمعة;
				break;
			case 3:
				date = record.الخميس;
				break;
			case 4:
				date = record.الأربعاء;
				break;
			case 5:
				date = record.الثلاثاء;
				break;
			case 6:
				date = record.الإثنين;
				break;
			default:

				date = new Date();
				var hijDate = calendar.newDate();
				date.setFullYear(hijDate.year());
				date.setMonth(hijDate.month());
				date.setDate(hijDate.day());
				break;
			}
			var cellDate = record[colNum];
			if (record[colNum].date)
				return calendar.newDate(cellDate.getFullYear(), 2, cellDate.date);
			else
				return calendar.newDate(cellDate.getFullYear(), parseInt(cellDate.getMonth()) + 1, cellDate.getDate());
		},

		getCellStyle : function(record, rowNum, colNum) {
			var calendar = $.calendars.instance("ummalqura");
			var field = this.getField(colNum);
			var selected = record.fiscalWeek == this.selectedWeek;
			if (field.name == "fiscalYear") {
				return !selected ? this.baseFiscalYearStyle : this.selectedWeekStyle;
			}
			else if (field.name == "fiscalWeek" || field.name == "calendarWeek") {
				return !selected ? this.baseWeekStyle : this.selectedWeekStyle;
			}
			// var date = this.getCellDate(record, rowNum, colNum);
			var date = this.getHijCellDate(record, rowNum, colNum);
			var currentMonth = date.month();

			var isDisabled = this.dateIsDisabled(date),isOtherMonth = currentMonth != this.workingMonth, style = this.Super("getCellStyle", arguments);
			
			if (field.isDateField) {
						if ((isOtherMonth)) {

							style = field.isWeekend ? this.disabledWeekendStyle : this.disabledWeekdayStyle;
							
							var isOver = (this.getEventRow() == rowNum && this.getEventColumn() == colNum), isSelected = this.cellSelection ? this.cellSelection.isSelected(rowNum, colNum) : false, overIndex = style
									.indexOf("Over"), selectedIndex = style.indexOf("Selected");

							if (overIndex >= 0)
								style = style.substring(0, overIndex);
							if (selectedIndex >= 0)
								style = style.substring(0, selectedIndex);

							if (isSelected)
								style += "Selected";
							if (isOver)
								style += "Over";
						}
					}
			return style;
		},

		cellMouseDown : function(record, rowNum, colNum) {
			var date = this.getCellDate(record, rowNum, colNum);
			if (!date)
				return true;
			if (this.dateIsDisabled(date))
				return false;
			return true;
		},

		cellClick : function(record, rowNum, colNum) {
			var date = this.getHijCellDate(record, rowNum, colNum);
			if (!date)
				return true;

			if (this.dateIsDisabled(date)) {
				return true;
			}

			var sType = this.getDateType(date);

			if (sType == "Object") {
				this.dateClick(date.year(), date.month(), date.day());
			}
			else
				this.dateClick(date.getFullYear(), date.getMonth(), date.getDate());
		},
		dateClick : function(year, month, date) {
		},

		cellSelectionChanged : function(cellList) {
			var sel = this.getCellSelection();
			for (var i = 0; i < cellList.length; i++) {
				var cell = cellList[i];
				if (sel.cellIsSelected(cell[0], cell[1])) {
					if (this.selectedWeek != this.getRecord(cell[0]).fiscalWeek) {
						this.setSelectedWeek(this.getRecord(cell[0]).fiscalWeek);
					}
					return;
				}
			}
			return;
		},

		setSelectedWeek : function(weekNum) {
			this.selectedWeek = weekNum;
			this.markForRedraw();
			this.selectedWeekChanged(this.selectedWeek);
		},
		selectedWeekChanged : function(weekNum) {
		},

		getWorkingMonth : function() {
			return this.workingMonth;
		},
		getSelectedDate : function() {
			return null;
		},

		disableMarkedDates : function() {
			this.disabledDateStrings = [];
			if (this.disabledDates && this.disabledDates.length > 0) {
				for (var i = 0; i < this.disabledDates.length; i++) {
					this.disabledDateStrings[i] = this.disabledDates[i].toShortDate();
				}
			}
		},

		dateIsDisabled : function(date) {
			if (!date)
				return;
			var sType = this.getDateType(date);
			if (sType == "Object") {
				if (this.disableWeekends && this.dateIsWeekend(date))
					return true;
				var disabled = this.disabledDateStrings.contains(this.getShortHijriDate(date));

				return disabled;
			}
			else {
				if (this.disableWeekends && this.dateIsWeekend(date))
					return true;
				var disabled = this.disabledDateStrings.contains(date.toShortDate());

				return disabled;
			}
		},
		getShortHijriDate : function(date) {
			var year = date.year();
			var day = date.day();
			var month = date.month();

			var shortDate = day + "-" + month + "-" + year;
			return shortDate;
		},
		getCellDate : function(record, rowNum, colNum) {
			if (colNum < this.dateColumnOffset || !this.getField(colNum))
				return;
			var calendar = $.calendars.instance("ummalqura");
			var sType = this.getDateType(record.rowStartDate);
			if (sType == "Object") {
				// var rDate = this.currentRecords[rowNum];
				var rDate = record.rowStartDate;
				var jsDate = calendar.toJSDate(rDate.year(), rDate.month(), rDate.day());
				jsDate.setDate(jsDate.getDate() + (colNum - this.dateColumnOffset));
				var date = calendar.newDate().fromJSDate(jsDate);
				return date;
			}
			else {

				var rDate = record.rowStartDate, date = Date.createLogicalDate(rDate.getFullYear(), rDate.getMonth(), rDate.getDate() + (colNum - this.dateColumnOffset));
				return date;
			}
		},

		selectDateCell : function(date) {
			var selection = this.getCellSelection(), cell = this.getDateCell(date);

			if (!cell)
				return;

			if (cell.colNum != null)
				selection.selectSingleCell(cell.rowNum, cell.colNum);
			this.setSelectedWeek(cell.record.fiscalWeek);
		},

		getDateCell : function(date) {
			// returns an object with rowNum, colNum and record
			var selection = this.getCellSelection(), data = this.data;

			if (date && data && data.length > 0) {
				var dayCount = this.showWeekends == false ? 5 : 7;
				for (var i = 0; i < data.length; i++) {
					var record = data[i];
					if (record) {
						for (var j = 0; j < dayCount; j++) {
							var dateDay = date.getDay();
							if (Date.compareLogicalDates(record[this.shortDayNames[date.getDay()]], date) == 0) {
								var fieldName = this.shortDayNames[date.getDay()], field = this.getField(fieldName), fieldNum = field ? this.getFieldNum(field.name) : null;
								if (field) {
									return {
										rowNum : i,
										colNum : fieldNum,
										record : record
									};
								}
								break;
							}
						}
					}
				}
			}
		},

		shouldDisableDate : function(date) {
			var result = this.dateIsDisabled(date);
			return result;
		},

		getRowHeight : function() {
			if (this._storedRowHeight == null) {
				var bodyHeight = this.body.getVisibleHeight();
				this._storedRowHeight = Math.floor(Math.max(20, Math.floor(bodyHeight / this.data.length)));
			}
			return this._storedRowHeight;
		},

		setStartDate : function(startDate) {
			this.firstDayOfWeek = 6;
			var calendar = $.calendars.instance("ummalqura");
			//console.log("Start Date: " + startDate);

			if (startDate.year() >= 1250 && startDate.year() <= 1500) {
				var hijStartDate = calendar.newDate(startDate.year(), startDate.month(), 1);
				year = hijStartDate.year();
				month = hijStartDate.month();
				date = hijStartDate.day();
				day = hijStartDate.dayOfWeek();

				// console.log("Year: "+year+", month:"+month+" ,Date: "+date+"
				// ,day:"+day);
				weekDate = hijStartDate.newDate();
				delta = 0;

				if (day > this.firstDayOfWeek) {
					delta = (day - this.firstDayOfWeek) * -1;
				}
				else if (day < this.firstDayOfWeek) {
					delta = (this.firstDayOfWeek - day) - 7;
				}

				weekStart = this.getPreviousMonthDate(weekDate, weekDate.dayOfWeek());

				// this.logWarn("in setStartDate - original is " +
				// startDate.toShortDate() + "\n\n" +
				// "year, month, date, monthStart, monthDay, delta *** final
				// date \n" +
				// year+", "+month+", "+date+",
				// "+monthStart.toShortDate()+", "+day+", "+delta+" - *** "
				// + weekStart.toShortDate()
				// );
				this.workingMonth = month;
				this.startDate = weekStart;
				this.buildCalendarData();
				this.markForRedraw();
			}
		},
		getPreviousMonthDate : function(currentDate, days) {
			var calendar = $.calendars.instance("ummalqura");
			var jsDate = currentDate.toJSDate();
			var weekDay = jsDate.getDay();
			if (weekDay == 6)
				return currentDate;
			else {
				jsDate.setDate(jsDate.getDate() - (weekDay + 1));
				var newDate = calendar.newDate().fromJSDate(jsDate);
				return newDate;
			}
		},

		refreshUI : function(startDate) { /*console.log("Refresh UI");*/
			var calendar = $.calendars.instance("ummalqura");

			this.setFields(this.getFieldList());

			startDate = startDate || this.startDate;

			if (startDate) {
				var sType = this.getDateType(startDate);

				if (sType == "Date") {
					var isValid = calendar.isValid(startDate.getFullYear(), startDate.getMonth(), startDate.getDate());
					this.startDate = calendar.newDate().fromJSDate(new Date());
					if (!isValid) {
						startDate = calendar.newDate().fromJSDate(new Date());
					}
					else {
						startDate = calendar.newDate(startDate.getFullYear(), startDate.getMonth(), startDate.getDate());
					}
				}
				this.setStartDate(startDate);
			}
		},

		getFieldList : function() {
			var fields = [];
			this.firstDayOfWeek = 6;
			this.dateColumnOffset = 0;
			if (this.showFiscalYear) {
				fields.add({
					name : "fiscalYear",
					type : "number",
					title : this.fiscalYearFieldTitle,
					width : 30,
					align : "center",
					cellAlign : "center",
					showRollOver : false,
					showDown : false,
					baseStyle : this.baseFiscalYearStyle,
					headerBaseStyle : this.fiscalYearHeaderStyle || this.baseFiscalYearStyle
				});
				this.dateColumnOffset++;
			}
			if (this.showFiscalWeek) {
				fields.add({
					name : "fiscalWeek",
					type : "number",
					title : this.weekFieldTitle,
					width : 25,
					align : "center",
					showRollOver : false,
					showDown : false,
					baseStyle : this.baseWeekStyle,
					headerBaseStyle : this.weekHeaderStyle || this.baseWeekStyle
				});
				this.dateColumnOffset++;
			}
			if (this.showCalendarWeek) {
				fields.add({
					name : "calendarWeek",
					type : "number",
					title : this.weekFieldTitle,
					width : 25,
					align : "center",
					showRollOver : false,
					showDown : false,
					baseStyle : this.baseWeekStyle,
					headerBaseStyle : this.weekHeaderStyle || this.baseWeekStyle
				});
				this.dateColumnOffset++;
			}

			var weekendDays = getUmmAlQuaraWeekendDays();

			for (var i = 0; i < this.shortDayNames.length; i++) {
				var dayNumber = i + this.firstDayOfWeek;
				if (dayNumber > 6)
					dayNumber -= 7;
				// don't add fields for weekends if showWeekends is
				// false
				if (!this.showWeekends && weekendDays.contains(dayNumber))
					continue;
				var field = {
					name : this.shortDayNames[dayNumber],
					title : this.shortDayTitles[dayNumber],
					type : "text",
					align : "center",
					width : "*",
					padding : 0,
					isDateField : true,
					dateOffset : i,
					showRollOver : false,
					showDown : false
				};
				if (weekendDays.contains(dayNumber)) {
					field.isWeekend = true;
					field.baseStyle = this.baseWeekendStyle;
					field.headerBaseStyle = this.weekendHeaderStyle;
				}
				else {
					field.baseStyle = this.baseWeekdayStyle;
					field.headerBaseStyle = this.headerBaseStyle;
				}
				fields.add(field);
			}

			this.disableMarkedDates();

			return fields;
		},

		_weekendDays : null,
		dateIsWeekend : function(date) {
			if (!date)
				return false;
			if (this._weekendDays == null)
				this._weekendDays = getUmmAlQuaraWeekendDays();
			return this._weekendDays.contains(date.getDay())
		},

		buildCalendarData : function(startDate) {
			this.currentRecords = [];
			if (startDate)
				this.startDate = startDate;
			startDate = this.startDate;
			var sType = this.getDateType(startDate);
			if (sType == "Object") {
				var records = [], date = startDate, startMonth = startDate.month(), yearWrap = (startMonth == 12 || this.workingMonth == 12);

				var calendar = $.calendars.instance("ummalqura");

				sDate2 = this.getNextMonthStart(this.workingMonth, startDate);
				var jsDate2 = sDate2.toJSDate(), jsDate = date.toJSDate();

				var delta = (jsDate2.getTime() - jsDate.getTime()) / 1000 / 60 / 60 / 24, weeks = delta / 7;

				var counter = Math.floor(weeks) + (delta % 7 > 0 ? 1 : 0);

				/*
				 * get the start date for every week in the month
				 * */
				for (var i = 0; i <= counter; i++) {
					var thisDate = this.getIncrementedDate(date, (i), 7);
					if (i == counter && thisDate.month() != this.workingMonth) {
						break;
					}
					else {
						var thisWeekRecord = this.getWeekRecord(thisDate, i);
						records.add(thisWeekRecord); // //
														// thisWeekRecord.rowStartDate.toJSDate()
						this.currentRecords.add(calendar.toJSDate(thisWeekRecord.rowStartDate.year(), thisWeekRecord.rowStartDate.month(), thisWeekRecord.rowStartDate.day()));
					}
				}
				this.setData(records);

				// this.selectDateCell(this.getSelectedDate());
			}
		},
		getNextMonthStart : function(workingmonth, startDate) {
			var calendar = $.calendars.instance("ummalqura");
			var month = startDate.month();
			var year = startDate.year();

			var daysInCurrentMonth = calendar.daysInMonth(year, month);
			var jsDate = startDate.toJSDate();
			var weekDay = jsDate.getDay();
			jsDate.setDate(jsDate.getDate() + daysInCurrentMonth);

			var nextMonthStart = calendar.newDate().fromJSDate(jsDate);

			return nextMonthStart;

		},
		getIncrementedDate : function(date, counter, incBy) {
			var incHijDate = date;
			if (counter > 0)
				incHijDate = date.add(parseInt(incBy), 'd');

			return incHijDate;
		},
		getDateType : function(startDate) {
			var type = Object.prototype.toString.call(startDate);
			var sType = type.slice(8, type.length - 1); // [object date]
			return sType;
		},
		getFiscalCalendar : function() {
			return this.fiscalCalendar || Date.getFiscalCalendar();
		},

		// set this to false to allow the DateGrid to NOT always show
		// fiscal week 1 - instead, it
		// may show either the highest partial week or 1, depending on
		// where the fiscalStartDate is
		alwaysShowFirstFiscalWeek : true,

		getWeekRecord : function(date, week) {

			var sType = this.getDateType(date);
			this.firstDayOfWeek = 6;
			if (sType == "Object") {
				var calendar = $.calendars.instance("ummalqura");

				var jsDate = date.toJSDate();
				var endGreDate = new Date(jsDate.getTime() + (6 * 86400000));
				var endDate = calendar.fromJSDate(endGreDate);

				var record = {
					rowStartDate : date,
					rowEndDate : endDate,
					fiscalYear : date.year(),
					// fiscalYear for the end date
					fiscalYearEnd : endDate.year(),

					// fiscal week (for the start date)
					fiscalWeek : date.weekOfYear(),
					// fiscal week end (for the end date)
					fiscalWeekEnd : endDate.weekOfYear(),

					// calendar week (for the first day of week)
					calendarWeek : date.weekOfYear()
				}

				var weekendDays = getUmmAlQuaraWeekendDays();
				/*
				 * Calculates Dates for a particular week.
				 * */
				for (var i = 0; i < 7; i++) {
					var jsDate = date.toJSDate();
					jsDate.setDate(jsDate.getDate() + i);

					var hijriDate = calendar.fromJSDate(jsDate);

					thisDate = Date.createLogicalDate(hijriDate.year(), hijriDate.month() - 1, hijriDate.day(), 0);
					if (hijriDate.month() == 2 && hijriDate.day() == 29) {
						record["newDate"] = Date.createLogicalDate(1432, 2, 29, 0);
						record["day"] = i;
						record["week"] = week;
						thisDate.day=i;
						thisDate.date=29;
					}else if ((hijriDate.month() == 2 && hijriDate.day() == 30)){
						record["newDate"] = Date.createLogicalDate(1445, 5, 30, 0);
						record["day"] = i;
						record["week"] = week;
						thisDate.date=30;
					}

					var dayName = this.shortDayNames[hijriDate.dayOfWeek()];
					var formattedDate = calendar.formatDate('dd-mm-yyyy', hijriDate);
					record[dayName] = thisDate;
					record[i] = thisDate;
				}

				return record;
			}
		}
	});

}