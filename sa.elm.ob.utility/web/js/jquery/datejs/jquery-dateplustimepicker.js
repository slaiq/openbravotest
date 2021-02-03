/*
* jQuery dateplustimepicker
* By: Fernando San Julián
* Version 0.1
* Last Modified: 
* 
* Copyright 2010 Fernando San Julián
* Dual licensed under the MIT and GPL licenses.
* http://www.opensource.org/licenses/mit-license.php
* http://www.gnu.org/licenses/gpl.html
* 
*
*/

(function($, undefined) {

	//****************** TIME OBJECT ******************//
	function Time(hours, minutes, seconds) {
		switch(typeof hours) {
			case 'number':
				if (minutes===undefined)
					this.setFromSeconds(hours);
				else
					this.set(hours, minutes, seconds);
				break;
			case 'string':
				this.setFromString(hours, minutes);
				break;
			case 'object':
				if (hours.getDate!==undefined)
					this.setFromDate(hours);
				else
					this.setFromTime(hours);
				break;
			default:
				if (hours===undefined) break;
				throw "illegal parameters";
				break;
		};
	};

	/* default values */
	Time.prototype.hours = 0;
	Time.prototype.minutes = 0;
	Time.prototype.seconds = 0;

    /* getters and setters */
	Time.prototype.getHours = function() {
		return this.hours;
	};

	Time.prototype.setHours = function(hours) {
		this.hours = hours;
	};

	Time.prototype.getMinutes = function() {
		return this.minutes;
	};

	Time.prototype.setMinutes = function(minutes) {
		this.minutes = minutes;
	};

	Time.prototype.getSeconds = function() {
		return this.seconds;
	};

	Time.prototype.setSeconds = function(seconds) {
		this.seconds = seconds;
	};

	/* Get the time in seconds */
	Time.prototype.getTime = function() {
		return this.hours*3600 + this.minutes*60 + this.seconds;
	}

	/* Checks if time is greater than the specified time object */
	Time.prototype.GT = function(time) {
		return (this.hours>time.hours
			|| (this.hours==time.hours && this.minutes>time.minutes)
			|| (this.hours==time.hours && this.minutes==time.minutes && this.seconds>time.seconds)) ? true : false;
	};

	/* Checks is time is less than the specified time object */
	Time.prototype.LT = function(time) {
		return (this.hours<time.hours
			|| (this.hours==time.hours && this.minutes<time.minutes)
			|| (this.hours==time.hours && this.minutes==time.minutes && this.seconds<time.seconds)) ? true : false;
	};

	/* Check if time equals the specified time object */
	Time.prototype.equals = function(time) {
		return (this.hours==time.hours && this.minutes==time.minutes && this.seconds==time.seconds) ? true : false;
	};

	/* Check if a format is valid time format

	   The format can be combinations of the following:
	
		h  - hour (no leading zero)
		hh - hour (two digit)
		m  - minutes (no leading zero)
		mm - minutes (two digit)
		s  - seconds (no leading zero)
		ss - seconds (two digit)
		t  - a for AM, p for PM
		tt - am for AM, pm for PM
		T  - A for AM, P for PM
		TT - AM for AM, PM for PM

		Allowed separator characters ':' or '.' Optional t|tt|T|TT separator ' '
	*/
	Time._isValidFormat = function(format) {
		var match = (format.match(/(^((TT|tt|T|t)(\s)?)?h{1,2}((:|\.)m{1,2}((:|\.)s{1,2})?)?$)|(^h{1,2}((:|\.)m{1,2}((:|\.)s{1,2})?)?((\s)?(TT|tt|T|t))?$)|(^m{1,2}((:|\.)s{1,2})?$)|(^s{1,2}$)/g) !== null);
		var matchSep1 = (format.match(/:/) === null);
		var matchSep2 = (format.match(/\./) === null);
		var matchSep = (matchSep1 && matchSep2)
			|| (matchSep1!=matchSep2);
		return (match&&matchSep);
	};

	/* Checks if a string is a valid time in the specified format */
	Time._isValidTime = function(time, format) {

		var is12HourFormat = Time._is12HourFormat(format);

		// format to num regexp
		var numTimeFormat = format
    		.replace(/hh/g, is12HourFormat ? '((0[0-9])|(1[0-2]))' : '(([0-1][0-9])|(2[0-4]))')
    		.replace(/h/g, is12HourFormat ? '(([0-9])|(1[0-2]))' : '(([0-9])|(1[0-9])|(2[0-4]))')
    		.replace(/mm/g, '([0-5][0-9])')
    		.replace(/m/g, '(([1-9])|([1-5][0-9]))')
    		.replace(/ss/g, '([0-5][0-9])')
    		.replace(/s/g, '(([1-9])|([1-5][0-9]))')
    		.replace(/TT/g, '(AM|PM)')
    		.replace(/tt/g, '(am|pm)')
    		.replace(/T/g, '(A|P)')
    		.replace(/t/g, '(a|p)');

		var numTimeFormat = '^' + numTimeFormat + '$';

		return time===undefined ? false : time.match(numTimeFormat) !== null;
	};

	/* Checks if a format is 12 hours or 24 hours */
	Time._is12HourFormat = function(format) {
		return format.match(/t{1,2}/ig) !== null;
	}

	/* Format a time object into a string value.
	   If format is undefined a default 'hh:mm tt' format will be used.
	   @param  format    string - the desired format of the time
	   @return  string - the date in the specified format */
	Time.prototype.formatTime = function(format) {
		var timeFormat = '';
		if (format===undefined) {
			timeFormat = 'hh:mm tt';
		}
		else {
			if (Time._isValidFormat(format)) {
				timeFormat = format;
			}
			else {
				throw "illegal format";
			}
		}
		var formattedTime = '';
		var ampm = (this.hours>=12) ? 'PM' : 'AM';
		var hours = Time._is12HourFormat(timeFormat) ? (this.hours>12 ? this.hours%12 : (this.hours==0 ? 12 : this.hours)) : this.hours;

		formattedTime = timeFormat.toString()
        	.replace(/hh/g, ((hours < 10) ? '0' : '') + hours)
        	.replace(/h/g, hours)
        	.replace(/mm/g, ((this.minutes < 10) ? '0' : '') + this.minutes)
        	.replace(/m/g, this.minutes)
        	.replace(/ss/g, ((this.seconds < 10) ? '0' : '') + this.seconds)
        	.replace(/s/g, this.seconds)
        	.replace(/TT/g, ampm.toUpperCase())
        	.replace(/tt/g, ampm.toLowerCase())
        	.replace(/T/g, ampm.charAt(0).toUpperCase())
        	.replace(/t/g, ampm.charAt(0).toLowerCase());

		return formattedTime;
	};

	/* Set time from hours, minutes and seconds */
	Time.prototype.set = function(hours, minutes, seconds) {
		var hours = (hours===undefined || (typeof hours != 'number')) ? 0 : Math.round(hours);
		var minutes = (minutes===undefined || (typeof minutes != 'number')) ? 0 : Math.round(minutes);
		var seconds = (seconds===undefined || (typeof minutes != 'number')) ? 0 : Math.round(seconds);
		this.setFromSeconds(hours*3600 + minutes*60 + seconds);
	};

	/* Set time from a Date object */
	Time.prototype.setFromDate = function(date) {
		if (date.getHours()===undefined || date.getMinutes()===undefined || date.getSeconds()===undefined) {
			throw "illegal parameters";
		}
		this.hours = date.getHours();
		this.minutes = date.getMinutes();
		this.seconds = date.getSeconds();
	};

	/* Set time from a time object */
	Time.prototype.setFromTime = function(time) {
		var hours = (time.hours===undefined || (typeof time.hours != 'number')) ? 0 :  Math.round(time.hours);
		var minutes = (time.minutes===undefined || (typeof time.minutes != 'number')) ? 0 :  Math.round(time.minutes);
		var seconds = (time.seconds===undefined || (typeof time.seconds != 'number')) ? 0 :  Math.round(time.seconds);
		this.setFromSeconds(hours*3600 + minutes*60 + seconds);
	};

	/* Set time to maximum */
	Time.prototype.setMaxHour = function() {
		this.hours = 23;
		this.minutes = 59;
		this.seconds = 59;
	}

	/* Set time to minimun */
	Time.prototype.setMinHour = function() {
		this.hours = 0;
		this.minutes = 0;
		this.seconds = 0;
	}

	/* Set time from seconds */
	Time.prototype.setFromSeconds = function(seconds) {
		if (seconds>=24*3600) {
			this.setMaxHour();
		}
		else if (seconds<0) {
			this.setMinHour();
		}
		else {
			this.hours = Math.floor(seconds/3600);
			this.minutes = Math.floor((seconds%3600)/60);
			this.seconds = (seconds%3600)%60;
		}
	}

	/* Set time from a String in the specified time format */
	Time.prototype.setFromString = function(time, format) {
		var timeFormat = '';
		var timeString = time;
		if (format===undefined) {
			timeFormat = 'hh:mm tt';
		}
		else {
			if (Time._isValidFormat(format)) {
				timeFormat = format;
			}
			else {
				throw "illegal format";
			}
		}

		var is12HourFormat = Time._is12HourFormat(timeFormat);

		if (!Time._isValidTime(timeString, timeFormat)) {
			throw "time does not match format";
		}

		// find hours, minutes, seconds
		var ampm = is12HourFormat ? (timeString.match(/a/ig) ? 'AM' : 'PM') : '';
		var timeString = timeString
			.replace(/(am|a|pm|p)/ig, '')
			.replace(/\s/g, '');
		var separator = timeString.match(/:/g)!==null ? ':' : (timeString.match(/\./g)!==null ? '.' : ' ');
		var hours = 0;
		var minutes = 0;
		var seconds = 0;
		var tokens = timeString.split(separator);
		var numToken = 0;

		if (timeFormat.search(/h/g)!=-1) {
			hours = parseInt(tokens[numToken],10);
			hours = (ampm=='PM' && hours!=12) ? hours+12 : ((ampm=='AM' && hours==12) ? 0 : hours);
			numToken++;
		}

		if (timeFormat.search(/m/g)!=-1) {
			minutes = parseInt(tokens[numToken],10);
			numToken++;
		}

		if (timeFormat.search(/s/g)!=-1) {
			seconds = parseInt(tokens[numToken],10);
		}

		this.hours = hours;
		this.minutes = minutes;
		this.seconds = seconds;
	};
	//**************** END TIME OBJECT ****************//


    function Dateplustimepicker() {
        this.regional = []; // Available regional settings, indexed by language code
        this.regional[''] = { // Default regional settings
            currentText: 'Now', // override datepicker currentText when not user defined
            hourText: 'Hours',
            minuteText: 'Minutes',
            secondText: 'Seconds',
            timeFormat: 'hh:mm tt',
            timeOnlyTitle: 'Choose Time',
            timeText: 'Time'
        };
        this._defaults = {
            // Global defaults for all the datetime picker instances
			altTimeField: '',
			altTimeFormat: '',
			defaultTime: {
				hours: 0,
				minutes: 0,
				seconds: 0
			},
            hourGrid: 0,
			maxTime: {
				hours: 23,
				minutes: 59,
				seconds: 59
			},
			minTime: {
				hours: 0,
				minutes: 0,
				seconds: 0
			},
            minuteGrid: 0,
			onDateTimeChange: null, // Define a callback function when the date or time is changed
			onDateTimeChangeStop: null, // Define a callback function when the date or time is changed (only after slide stops)
			onTimeChange: null, // Define a callback function when the time is changed
			onTimeChangeStop: null, // Define a callback function when the time is changed (only after slide stops)
            secondGrid: 0,
            showTime: true,
            step: {
				hours: 0,
				minutes: 0,
				seconds: 1
			},
            timeOnly: false
        };
        $.extend(this._defaults, this.regional['']);
    };

    $.extend(Dateplustimepicker.prototype, {

		/* Class name added to elements to indicate already configured with a time picker. */
		markerClassName: 'hasDateplustimepicker',

		/* Attach the time picker to a date picker.
		   @param  target    element - the target input field or division or span
		   @param  settings  object - the new settings to use for this datetime picker instance (anonymous) */
		_attachDateplustimepicker: function(target, settings) {
			// check for settings on the control itself - in namespace 'time:'
			var inlineSettings = null;
			for (var attrName in this._defaults) {
				var attrValue = target.getAttribute('time:' + attrName);
				if (attrValue) {
					inlineSettings = inlineSettings || {};
					try {
						inlineSettings[attrName] = eval(attrValue);
					} catch (err) {
						inlineSettings[attrName] = attrValue;
					}
				}
			}
			var inst = $.datepicker._getInst(target);
			var nodeName = target.nodeName.toLowerCase();
			var inline = (nodeName == 'div' || nodeName == 'span');

			inst.timepicker = this._newInst();
			inst.timepicker.settings = $.extend({}, settings || {}, inlineSettings || {});

			if (nodeName == 'input') {
				this._connectDateplustimepicker(target, inst);
			} else if (inline) {
				this._inlineDateplustimepicker(target, inst);
			}
		},

		/* Create a new instance object.
		   @return the time picker instance */
        _newInst: function() {
	
			var timepicker = {
				time: new Time(),
				lastVal: '',
				lastAltFieldVal: ''
			};

			return timepicker;
		},
		
		/* Attach the time picker to a date picker.
		   @param  target    element - the target input field or division or span
		   @param  inst      object - date picker instance */
		_connectDateplustimepicker: function(target, inst) {

			var $input = $(target);

			if ($input.hasClass(this.markerClassName))
				return;
			$input.addClass(this.markerClassName);
			this._autoSize(inst);
			
			this._attachments($input, inst);
			
			$.dateplustimepicker._setDateTimeFromField(inst, false);
			
			if ($input.val()!='') {
				$.dateplustimepicker.updateDateTime(inst);
			}

		},

		/* Make attachments based on settings.
		   @param  input    element - the target input field or division or span
		   @param  inst     object - date picker instance */
		_attachments: function(input, inst) {

			var $input = input;
			
			var $altTimeField = $($.dateplustimepicker._get(inst.timepicker, "altTimeField"));
			var timeOnly = $.dateplustimepicker._get(inst.timepicker, 'timeOnly');

			if (timeOnly) {
				$input.unbind('keydown')
					.unbind('keypress').keypress(this._doKeyPressTime)
					.unbind('keyup').keyup(this._doKeyUpTime);
			}
			else if ($altTimeField.length>0) {
				$input.unbind('keydown').keydown($.datepicker._doKeyDown)
					.unbind('keypress').keypress($.datepicker._doKeyPress)
					.unbind('keyup').keyup($.datepicker._doKeyUp);
				$altTimeField.unbind('keydown').keydown($.datepicker._doKeyDown)
					.unbind('keypress').keypress(this._doKeyPressTime)
					.unbind('keyup').keyup(this._doKeyUpTime);
			}
			else {
				$input.unbind('keydown').keydown($.datepicker._doKeyDown)
					.unbind('keypress').keypress(this._doKeyPress)
					.unbind('keyup').keyup($.datepicker._doKeyUp).keyup(this._doKeyUp);
			}
			
			// trigger timepicker when focus on altTimeField if exists
            if ($altTimeField.length>0) {
				$.data($altTimeField[0], 'datepicker', inst); // add reference to inst
				$altTimeField.addClass($.datepicker.markerClassName); // mark as part of datepicker
				// altTimeField focus handler - show datepicker and get focus
				var _onFocus = function() {
					$.datepicker._showDatepicker($input[0]); // show datepicker - it steals focus
					$altTimeField.unbind('focus'); // unbind focus event to avoid triggering it again when restored
					setTimeout(function() { // wait a little while datepicker steals focus
						$altTimeField.focus(); // restore focus
					}, 200);
				};
				$altTimeField.focus(_onFocus);

				// altTimeField blur handler 
				$altTimeField.blur(function(){
					$altTimeField.unbind('focus').focus(_onFocus); // restore focus handler
				});
            }

		},

		/* Apply the maximum length for the datetime format.
		   @param  inst    object - date picker instance */
		_autoSize: function(inst) {
			if ($.datepicker._get(inst, 'autoSize') && !inst.inline) {
				var time = new Time(24,59,59);
				var timeFormat = $.dateplustimepicker._get(inst.timepicker, 'timeFormat');
				var maxTimeLength = time.formatTime(timeFormat).length; // Calculate time string max length
				
				var $altTimeField = $($.dateplustimepicker._get(inst.timepicker, 'altTimeField'));
				if ($altTimeField.length>0) {
					$altTimeField.attr('size', maxTimeLength);
				}
				else {
					var inputLength = inst.input.attr('size');
					inst.input.attr('size', inputLength + 1 + maxTimeLength);
				}
			}
		},

		/* Attach time picker to an inline date picker.
		   @param  target    element - the target input field or division or span
		   @param  inst      object - date picker instance */
		_inlineDateplustimepicker: function(target, inst) {
			var divSpan = $(target);
			if (divSpan.hasClass(this.markerClassName))
				return;
			var defTime = new Time($.dateplustimepicker._get(inst.timepicker, 'defaultTime'));
			inst.timepicker.time = defTime;
			inst.timepicker.time = $.dateplustimepicker._constraintTime(inst);
			$.datepicker._updateDatepicker(inst);
			$.dateplustimepicker.updateDateTime(inst);
		},
		
		/* Get a setting value, defaulting if necessary. */
		_get: function(timepicker, name) {
			return timepicker.settings[name] !== undefined ?
				timepicker.settings[name] : this._defaults[name];
		},

		/* Update or retrieve the settings for a time picker attached to an input field or division.
		   @param  target  element - the target input field or division or span
		   @param  name    object - the new settings to update or
		                   string - the name of the setting to change or retrieve,
		                   when retrieving also 'all' for all instance settings or
		                   'defaults' for all global defaults
		   @param  value   any - the new value for the setting
		                   (omit if above is an object or to retrieve a value) */
		_optionDateTimePicker: function(target, name, value) {
			var inst = $.datepicker._getInst(target);
			if (arguments.length == 2 && typeof name == 'string') {
				return (name == 'defaults' ? $.extend({}, $.datepicker._defaults, $.dateplustimepicker._defaults) :
					(inst ? (name == 'all' ? $.extend({}, inst.settings) :
					($.dateplustimepicker._get(inst, name) || $.datepicker._get(inst, name)) ) : null));
			}
			var settings = name || {};
			if (typeof name == 'string') {
				settings = {};
				settings[name] = value;
			}
			if (settings.altTimeField!==undefined) {
				var oldAltTimeField = $($.dateplustimepicker._get(inst.timepicker, 'altTimeField'));
				oldAltTimeField.val('');
				oldAltTimeField.unbind('keydown').unbind('keypress').unbind('keyup').unbind('focus').unbind('blur');
			}
			if (inst) {
				if ($.datepicker._curInst == inst) {
					$.datepicker._hideDatepicker();
				}
				var date = $.dateplustimepicker._getDateTimeDateTimePicker(target, true);
				extendRemove(inst.timepicker.settings, settings);
				extendRemove(inst.settings, settings);
				var nodeName = target.nodeName.toLowerCase();
				var inline = (nodeName == 'div' || nodeName == 'span');
				if (!inline) {
					$.datepicker._attachments($(target), inst);
					$.dateplustimepicker._attachments($(target), inst);
					$.datepicker._autoSize(inst);
					$.dateplustimepicker._autoSize(inst);
				}
				$.dateplustimepicker._setDateTimeDateTimePicker(target, date);
				$.dateplustimepicker._setDateTimeFromField(inst);
				$.datepicker._updateDatepicker(inst);
				
			}
		},

        /* inject html for time picker into ui date picker
		   @param  inst   object - datepicker instance */
        injectTimePicker: function(inst)
        {

            var $dp = inst.dpDiv;

            // Prevent displaying twice
            if ($(".ui-dateplustimepicker-div", $dp).length === 0) {

                var html = $.dateplustimepicker._generateHtml(inst);
                $tp = $(html);

				// inject timepicker
				var showButtonPanel = $.datepicker._get(inst, 'showButtonPanel');
				if (showButtonPanel) {
					$dp.find('.ui-datepicker-buttonpane').before($tp);
				}
				else {
					$dp.append($tp);
				}

				$.dateplustimepicker._addSliders(inst);
				$.dateplustimepicker._addGrids(inst);

                // if only time
				var timeOnly = $.dateplustimepicker._get(inst.timepicker, "timeOnly");
                if (timeOnly===true) {
					var timeOnlyTitle = $.dateplustimepicker._get(inst.timepicker, "timeOnlyTitle");
                    $tp.prepend(
                    '<div class="ui-widget-header ui-helper-clearfix ui-corner-all">' +
                    '<div class="ui-datepicker-title">' + timeOnlyTitle + '</div>' +
                    '</div>');
                    $dp.find('.ui-datepicker-header, .ui-datepicker-calendar').hide();
                }

            }

        },

		/* Generate the HTML for the current state of the time picker. */
		_generateHtml: function(inst) {

			var noDisplay = ' style="display:none;"';

			var showTime = $.dateplustimepicker._get(inst.timepicker, "showTime");
			var timeText = $.dateplustimepicker._get(inst.timepicker, "timeText");
			var format = $.dateplustimepicker._get(inst.timepicker, "timeFormat");
			var time = inst.timepicker.time.formatTime(format);

			var showHour = format.match(/h/) !== null;
			var hourText = $.dateplustimepicker._get(inst.timepicker, "hourText");
			var hourGrid = $.dateplustimepicker._get(inst.timepicker, "hourGrid");
			
			var showMinute = format.match(/m/) !== null;
			var minuteText = $.dateplustimepicker._get(inst.timepicker, "minuteText");
			var minuteGrid = $.dateplustimepicker._get(inst.timepicker, "minuteGrid");
			
			var showSecond = format.match(/s/) !== null;
			var secondText = $.dateplustimepicker._get(inst.timepicker, "secondText");
			var secondGrid = $.dateplustimepicker._get(inst.timepicker, "secondGrid");
			
			// begin timepicker layer
            var html = '<div class="ui-dateplustimepicker-div">';

			// time display
			html += '<div class="ui-dateplustimepicker-time"' + (showTime ? '': noDisplay) + '>';
            html += '<div class="ui-dateplustimepicker-time-label ui-dateplustimepicker-label">' + timeText + '</div>';
            html += '<div class="ui-dateplustimepicker-time-value ui-dateplustimepicker-value">' + time + '</div>';
			html += '</div>';

			// hour slider and grid
			html += '<div class="ui-dateplustimepicker-hour"' + (showHour ? '': noDisplay) + '>';
            html += '<div class="ui-dateplustimepicker-hour-label ui-dateplustimepicker-label">' + hourText + '</div>';
            html += '<div class="ui-dateplustimepicker-hour-value ui-dateplustimepicker-value">';
            html += '<div class="ui-dateplustimepicker-hour-slider"></div>';
			html += '<div class = "ui-dateplustimepicker-hour-grid ui-dateplustimepicker-grid"' + (hourGrid>0 ? '' : noDisplay) + '></div>';
            html += '</div>';
			html += '</div>'

			// minute slider and grid
			html += '<div class="ui-dateplustimepicker-minute"' + (showMinute ? '': noDisplay) + '>';
            html += '<div class="ui-dateplustimepicker-minute-label ui-dateplustimepicker-label">' + minuteText + '</div>';
            html += '<div class="ui-dateplustimepicker-minute-value ui-dateplustimepicker-value">';
            html += '<div class="ui-dateplustimepicker-minute-slider"></div>';
			html += '<div class = "ui-dateplustimepicker-minute-grid ui-dateplustimepicker-grid"' + (minuteGrid>0 ? '' : noDisplay) + '></div>';
            html += '</div>';
			html += '</div>'

			// second slider and grid
			html += '<div class="ui-dateplustimepicker-second"' + (showSecond ? '': noDisplay) + '>';
            html += '<div class="ui-dateplustimepicker-second-label ui-dateplustimepicker-label">' + secondText + '</div>';
            html += '<div class="ui-dateplustimepicker-second-value ui-dateplustimepicker-value">';
            html += '<div class="ui-dateplustimepicker-second-slider"></div>';
			html += '<div class = "ui-dateplustimepicker-second-grid ui-dateplustimepicker-grid"' + (secondGrid>0 ? '' : noDisplay) + '></div>';
            html += '</div>';
			html += '</div>'

			// end timepicker layer
            html += '</div>';

			return html;

		},
		
		/* add slider to the time picker */
		_addSliders: function(inst) {
			
			var $dp = inst.dpDiv;
			
			var format = $.dateplustimepicker._get(inst.timepicker, "timeFormat");
			var showHour = format.match(/h/) !== null;
			var showMinute = format.match(/m/) !== null;
			var showSecond = format.match(/s/) !== null;
			
			// hour slider
			if (showHour) {
	            $('.ui-dateplustimepicker-hour-slider', $dp).slider({
	                orientation: "horizontal",
	                value: inst.timepicker.time.hours,
	                min: 0,
	                max: 23,
	                step: 1,
					start: function(event, ui)
					{
						timeSlidersTarget = "hour";
						timeSlidersStartValue = ui.value;
					},
	                slide: function(event, ui)
	                {
	                    $.dateplustimepicker._onHourChange(inst, ui.value);
	                },
					stop: function(event, ui) {
						$.dateplustimepicker._updateSliders(inst, event);
					}
	            });
			}

			// minute slider
			if (showMinute) {
	            $('.ui-dateplustimepicker-minute-slider', $dp).slider({
	                orientation: "horizontal",
	                value: inst.timepicker.time.minutes,
	                min: 0,
	                max: 59,
	                step: 1,
					start: function(event, ui)
					{
						timeSlidersTarget = "minute";
						timeSlidersStartValue = ui.value;
					},
	                slide: function(event, ui)
	                {
	                  	$.dateplustimepicker._onMinuteChange(inst, ui.value);
	                },
					stop: function(event, ui) {
						$.dateplustimepicker._updateSliders(inst);
					}
	            });
			}

			// second slider
			if (showSecond) {
	            $('.ui-dateplustimepicker-second-slider', $dp).slider({
	                orientation: "horizontal",
	                value: inst.timepicker.time.seconds,
	                min: 0,
	                max: 59,
	                step: 1,
					start: function(event, ui)
					{
						timeSlidersTarget = "second";
						timeSlidersStartValue = ui.value;
					},
	                slide: function(event, ui)
	                {
	                    $.dateplustimepicker._onSecondChange(inst, ui.value);
	                },
					stop: function(event, ui) {
						$.dateplustimepicker._updateSliders(inst);
					}
	            });
			}

		},

		/* add grids to the time picker */
		_addGrids: function(inst) {
			
			var $dp = inst.dpDiv;
			
			var format = $.dateplustimepicker._get(inst.timepicker, "timeFormat");
			var hourGrid = $.dateplustimepicker._get(inst.timepicker, "hourGrid");
			var minuteGrid = $.dateplustimepicker._get(inst.timepicker, "minuteGrid");
			var secondGrid = $.dateplustimepicker._get(inst.timepicker, "secondGrid");

			var showHour = format.match(/h/) !== null;
			var showMinute = format.match(/m/) !== null;
			var showSecond = format.match(/s/) !== null;

			var minTime = new Time($.dateplustimepicker._get(inst.timepicker, 'minTime'));
			var maxTime = new Time($.dateplustimepicker._get(inst.timepicker, 'maxTime'));
			var hourMin = minTime.hours;
			var hourMax = maxTime.hours;

			var ampm = Time._is12HourFormat($.dateplustimepicker._get(inst.timepicker, "timeFormat"));
			
			var time = new Time();

			// hour grid
			if (showHour && hourGrid>0) {

				var tdWidth = 100 / (23/hourGrid);

				var html = '<table><tr>';

	            for (var h = 0; h < 24; h += hourGrid)
	            {
	                // hourGridSize++;
					time.hours = h;
					var hour = ampm ? time.formatTime('hht') : time.formatTime('hh');
	                html += '<td width="' + tdWidth + '%"><span' + ((h<hourMin || h>hourMax) ? ' style="display:none;">' : '>') + hour + '</span></td>';
	            }

	            html += '</tr></table>';
	
				$('.ui-dateplustimepicker-hour .ui-dateplustimepicker-hour-grid', $dp).append(html);

                $(".ui-dateplustimepicker-hour span", $dp).each(function(index) {
                   	$(this).click(function() {
                        var h = $(this).html();
						var time = ampm ? new Time(h, 'hht') : new Time(h, 'hh');  
						var value = time.hours;
                        $.dateplustimepicker._onHourChange(inst, value);
						$.dateplustimepicker._updateSliders(inst);
                    });
                });

			}

			// minute grid
			if (showMinute && minuteGrid>0) {

				var tdWidth = 100/ (59/minuteGrid);

				var html = '<table><tr>';

	            for (var m = 0; m < 60; m += minuteGrid)
	            {
	                html += '<td width="' + tdWidth + '%"><span>' + ((m < 10) ? '0': '') + m + '</span></td>';
	            }

	            html += '</tr></table>';

				$('.ui-dateplustimepicker-minute .ui-dateplustimepicker-minute-grid', $dp).append(html);

                $(".ui-dateplustimepicker-minute span", $dp).each(function(index) {
                    $(this).click(function() {
						var value = parseInt($(this).html(), 10);
                        $.dateplustimepicker._onMinuteChange(inst, value);
 						$.dateplustimepicker._updateSliders(inst, 'minute');
                   });
                });
			}


			// second grid
			if (showSecond && secondGrid>0) {

				var tdWidth = 100/ (59/secondGrid);

				var html = '<table><tr>';

	            for (var s = 0; s < 60; s += secondGrid)
	            {
	                html += '<td width="' + tdWidth + '%"><span>' + ((s < 10) ? '0': '') + s + '</span></td>';
	            }

	            html += '</tr></table>';

				$('.ui-dateplustimepicker-second .ui-dateplustimepicker-second-grid', $dp).append(html);

                $(".ui-dateplustimepicker-second span", $dp).each(function(index) {
                    $(this).click(function() {
						var value = parseInt($(this).html(), 10);
                        $.dateplustimepicker._onSecondChange(inst, value);
 						$.dateplustimepicker._updateSliders(inst, 'second');
                   });
                });

			}

		},

        /* Update time display panel
		   @param  inst   Object - datepicker instance */
		_updateTimeDisplay: function(inst) {
			var format = $.dateplustimepicker._get(inst.timepicker, "timeFormat");
			var time = inst.timepicker.time.formatTime(format);
			var timeDisplay = $('.ui-dateplustimepicker-time-value', inst.dpDiv);
			timeDisplay.text(time);
		},

        /* Update sliders with time value
		   @param  inst   Object - datepicker instance */
		_updateSliders: function(inst, event) {
			var hourSlider = $('.ui-dateplustimepicker-hour-slider', inst.dpDiv);
			var minuteSlider = $('.ui-dateplustimepicker-minute-slider', inst.dpDiv);
			var secondSlider = $('.ui-dateplustimepicker-second-slider', inst.dpDiv);
			hourSlider.slider('option', 'value', inst.timepicker.time.hours);
			minuteSlider.slider('option', 'value', inst.timepicker.time.minutes);
			secondSlider.slider('option', 'value', inst.timepicker.time.seconds);

			if ((timeSlidersTarget == "hour" && timeSlidersStartValue != inst.timepicker.time.hours)
			 || (timeSlidersTarget == "minute" && timeSlidersStartValue != inst.timepicker.time.minutes)
			 || (timeSlidersTarget == "second" && timeSlidersStartValue != inst.timepicker.time.seconds))
			{
				this._notifyTimeChangeStop(inst, true);
				this._notifyDateTimeChangeStop(inst, true);
			}

		},

        /* Constraint the time taking into account min, max and step
		   @param  inst   Object - datepicker instance */
		_constraintTime: function(inst, slider) {
			
			var format = $.dateplustimepicker._get(inst.timepicker, 'timeFormat');
			var hasHours = format.match(/h/) !== null;
			var hasMinutes = format.match(/m/) !== null;
			var hasSeconds = format.match(/s/) !== null;

			var step = new Time($.dateplustimepicker._get(inst.timepicker, 'step'));
			step.hours = hasHours ? step.hours : 0;
			step.minutes = hasMinutes ? step.minutes : 0;
			step.seconds = hasSeconds ? step.seconds : (step.hours != 0 || step.minutes != 0) ? 0 : 1;
			var stepInSeconds = step.getTime();
			
			var minTime = new Time($.dateplustimepicker._get(inst.timepicker, 'minTime'));
			minTime.hours = hasHours ? minTime.hours : 0;
			minTime.minutes = hasMinutes ? minTime.minutes : 0;
			minTime.seconds = hasSeconds ? minTime.seconds : 0;
			var minTimeInSeconds = minTime.getTime(); 

			var maxTime = new Time($.dateplustimepicker._get(inst.timepicker, 'maxTime'));
			maxTime.hours = hasHours ? maxTime.hours : 0;
			maxTime.minutes = hasMinutes ? maxTime.minutes : 0;
			maxTime.seconds = hasSeconds ? maxTime.seconds : 0;
			var maxTimeInSeconds = maxTime.getTime();
			maxTimeInSeconds = maxTimeInSeconds - (maxTimeInSeconds-minTimeInSeconds)%stepInSeconds;

			var timeInSeconds = inst.timepicker.time.getTime();
			var steps = Math.round((timeInSeconds-minTimeInSeconds)/stepInSeconds); // nearest step

			steps = steps<0 ? 0 : steps; // min time restriction
			timeInSeconds = minTimeInSeconds + steps*stepInSeconds; // step restriction
			timeInSeconds = timeInSeconds>maxTimeInSeconds ? maxTimeInSeconds : timeInSeconds; // max time restriction
			
			var time = new Time(timeInSeconds);

			// correction
			//	- moving minute slider can't make hours greater
			//	- moving second slider can't make minutes greater
			if ((slider=='minute' && time.hours>inst.timepicker.time.hours)
				|| (slider=='second' && time.minutes>inst.timepicker.time.minutes)) {
					steps -= 1;
					timeInSeconds = minTimeInSeconds + steps*stepInSeconds;
					time.setFromSeconds(timeInSeconds);
			}

			return time;

		},

        /* Update time when hour slider slides
		   @param  inst   Object - datepicker instance
		   @param  value  float - hour slider current value */
		_onHourChange: function(inst, value) {
			
			var previous_value = inst.timepicker.time.hours;
 
			inst.timepicker.time.hours = value;
			inst.timepicker.time = $.dateplustimepicker._constraintTime(inst);
			
			if (previous_value != inst.timepicker.time.hours) {
				this._notifyTimeChange(inst, true);
				this._notifyDateTimeChange(inst, true);
			}

			var $minuteSlider = $('.ui-dateplustimepicker-minute-slider', inst.dpDiv);
			var $secondSlider = $('.ui-dateplustimepicker-second-slider', inst.dpDiv);
			$minuteSlider.slider("option", "value", inst.timepicker.time.minutes);
			$secondSlider.slider("option", "value", inst.timepicker.time.seconds);

			// update time display and inputs
			$.dateplustimepicker._updateTimeDisplay(inst);
			$.dateplustimepicker.updateDateTime(inst);

		},
		
        /* Update time when minute slider slides
		   @param  inst   Object - datepicker instance
		   @param  value  float - minute slider current value */
		_onMinuteChange: function(inst, value) {
			
			var previous_value = inst.timepicker.time.minutes;
 
			inst.timepicker.time.minutes = value;
			inst.timepicker.time = $.dateplustimepicker._constraintTime(inst, 'minute');

			if (previous_value != inst.timepicker.time.minutes) {
				this._notifyTimeChange(inst, true);
				this._notifyDateTimeChange(inst, true);
			}

			var $secondSlider = $('.ui-dateplustimepicker-second-slider', inst.dpDiv);
			$secondSlider.slider("option", "value", inst.timepicker.time.seconds);

			// update time display and inputs
			$.dateplustimepicker._updateTimeDisplay(inst);
			$.dateplustimepicker.updateDateTime(inst);

		},

        /* Update time when second slider slides
		   @param  inst   Object - datepicker instance
		   @param  value  float - second slider current value */
		_onSecondChange: function(inst, value) {
			
			var previous_value = inst.timepicker.time.seconds;
 
			inst.timepicker.time.seconds = value;
			inst.timepicker.time = $.dateplustimepicker._constraintTime(inst, 'second');

			if (previous_value != inst.timepicker.time.seconds) {
				this._notifyTimeChange(inst, true);
				this._notifyDateTimeChange(inst, true);
			}

			// update time display and inputs
			$.dateplustimepicker._updateTimeDisplay(inst);
			$.dateplustimepicker.updateDateTime(inst);

		},

		/* Notify change of time. */
		_notifyTimeChange: function(inst, fromPicker) {
			var onTimeChange = this._get(inst, 'onTimeChange');
			if (onTimeChange)
				onTimeChange.apply((inst.input ? inst.input[0] : null),
					[inst.timepicker.time, inst, fromPicker]);
		},

		/* Notify change of date and/or time. */
		_notifyDateTimeChange: function(inst, fromPicker) {
			var onDateTimeChange = this._get(inst, 'onDateTimeChange');
			var date = this._getDateTime(inst);
			if (onDateTimeChange)
				onDateTimeChange.apply((inst.input ? inst.input[0] : null),
					[date, inst, fromPicker]);
		},

		/* Notify change of time. After the slide stops */
		_notifyTimeChangeStop: function(inst, fromPicker) {
			var onTimeChangeStop = this._get(inst, 'onTimeChangeStop');
			if (onTimeChangeStop)
				onTimeChangeStop.apply((inst.input ? inst.input[0] : null),
					[inst.timepicker.time, inst, fromPicker]);
		},

		/* Notify change of date and/or time. After the slide stops */
		_notifyDateTimeChangeStop: function(inst, fromPicker) {
			var onDateTimeChangeStop = this._get(inst, 'onDateTimeChangeStop');
			var date = this._getDateTime(inst);
			if (onDateTimeChangeStop)
				onDateTimeChangeStop.apply((inst.input ? inst.input[0] : null),
					[date, inst, fromPicker]);
		},

		/* Update inputs with current date and time.
		   @param  inst   object - date picker instance */
		updateDateTime: function(inst) {
			
			var date = $.datepicker._formatDate(inst);

			var format = $.dateplustimepicker._get(inst.timepicker, "timeFormat");
			var time = inst.timepicker.time.formatTime(format);

			var timeOnly = $.dateplustimepicker._get(inst.timepicker, 'timeOnly');
			var alwaysSetTime = $.dateplustimepicker._get(inst.timepicker, 'alwaysSetTime');

			var $input = inst.input;
			var $altTimeField = $($.dateplustimepicker._get(inst.timepicker, 'altTimeField'));
			
			var inputVal = date;
			
            if (timeOnly===true) {
				inputVal = time;
				inst.timepicker.lastVal = inputVal;
                if ($altTimeField.length>0)
                {
                    $altTimeField.val(time);
					inst.timepicker.lastAltFieldVal = time;
				}
            }
            else if (timeOnly!==true) { // || timeAvailable)) {

                if ($altTimeField.length>0)
                {
                    $altTimeField.val(time);
					inst.timepicker.lastAltFieldVal = time;
                }
                else {
                    inputVal += ' ' + time;
					inst.timepicker.lastVal = inputVal;
                }
            }

            if (!inst.inline && $input) {
                $input.val(inputVal);
                $input.trigger("change");
            }

			$.dateplustimepicker._updateAlternate(inst);

		},

		/* Override the default settings for all instances of the time picker.
		   @param  settings  object - the new settings to use as defaults (anonymous object)
		   @return the manager object */
        setDefaults: function(settings) {
            extendRemove(this._defaults, settings || {});
            return this;
        },

        /* Get the date for the first entry in a jQuery selection.
			   @param  target     element - the target input field or division or span
			   @param  noDefault  boolean - true if no default date is to be used
			   @return Date - the current date and time */
        _getDateTimeDateTimePicker: function(target, noDefault) {
            var inst = $.datepicker._getInst(target);
            if (inst && !inst.inline)
            	$.dateplustimepicker._setDateTimeFromField(inst, noDefault);
            return (inst ? $.dateplustimepicker._getDateTime(inst) : null);
        },

        /* Retrieve the datetime directly. */
        _getDateTime: function(inst) {

			var date = (!inst.currentYear || (inst.input && inst.input.val() == '') ? null :
				new Date(inst.currentYear, inst.currentMonth, inst.currentDay,
                inst.timepicker.time.hours, inst.timepicker.time.minutes, inst.timepicker.time.seconds));
			return date;

        },

        /* Set the dates for a jQuery selection.
			   @param  target   element - the target input field or division or span
			   @param  date     Date - the new date */
        _setDateTimeDateTimePicker: function(target, date, noChange) {
	
			if(date===null) { return; }

            var inst = $.datepicker._getInst(target);

            if (inst) {

				if (typeof date == 'object' && date!==null) {
					$.datepicker._setDate(inst, new Date(date.getTime()), noChange);
				}
				else {
					$.datepicker._setDate(inst, date, noChange);
				}

                $.dateplustimepicker._setTimeDateTime(inst, date);

	            // update the input field
	            $.dateplustimepicker.updateDateTime(inst);

	            $.datepicker._updateDatepicker(inst);
            }
        },

        /* Set the time directly from a date or datetime string. */
        _setTimeDateTime: function(inst, date, noChange) {

            // set time from Date object
            if (typeof date == 'object' && date!==null) {
                $.dateplustimepicker._setTimeFromDate(inst, date);
            } else if (typeof date == 'string') {
                $.dateplustimepicker._setTimeFromDateTimeString(inst, date);
            }

			// apply min and max constraints
			inst.timepicker.time = $.dateplustimepicker._constraintTime(inst);

			this._notifyTimeChange(inst, false);
			this._notifyTimeChangeStop(inst, false);
			this._notifyDateTimeChange(inst, false);
			this._notifyDateTimeChangeStop(inst, false);
        },

        /* Set the time directly from a datetime string. */
		_setTimeFromDateTimeString: function(inst, datetimeString, noDefault) {
			
			var timeFormat = $.dateplustimepicker._get(inst.timepicker, 'timeFormat');

			// date and time has an space in between
			var tokens = datetimeString.split(' ');
			// remove first token (date) and join the rest of tokens (time - 1 or 2 tokens)
			var timeString = tokens.splice(1).join(' ');
			
			$.dateplustimepicker._setTimeFromString(inst, timeString, noDefault);

		},

        /* Set the time directly from a time string. */
		_setTimeFromString: function(inst, timeString, noDefault) {
			
			var timeFormat = $.dateplustimepicker._get(inst.timepicker, 'timeFormat');
			
			try {
				inst.timepicker.time.setFromString(timeString, timeFormat);
			}
			catch(err) {
				if (noDefault===undefined || noDefault==false) {
					var defTime = new Time($.dateplustimepicker._get(inst.timepicker, 'defaultTime'));
					inst.timepicker.time = defTime;
				}
			}

		},

        /* Set the time directly from a date object. */
		_setTimeFromDate: function(inst, date) {

	        if (inst.timepicker) {
	            inst.timepicker.time.hours = date.getHours();
	            inst.timepicker.time.minutes = date.getMinutes();
	            inst.timepicker.time.seconds = date.getSeconds();
			}

		},
		
        /* Set the time directly from a time object. */
		_setTimeFromObject: function(inst, time) {
			
	        if (inst.timepicker) {
	            inst.timepicker.time.hours = time.hours;
	            inst.timepicker.time.minutes = time.minutes;
	            inst.timepicker.time.seconds = time.seconds;
			}

		},

        /* Get the time for the first entry in a jQuery selection.
			   @param  target     element - the target input field or division or span
			   @param  noDefault  boolean - true if no default date is to be used
			   @return Time - the current time */
        _getTimeDateTimePicker: function(target, noDefault) {
            var inst = $.datepicker._getInst(target);
            if (inst && !inst.inline)
            	$.dateplustimepicker._setDateTimeFromField(inst, noDefault);
            return (inst ? $.dateplustimepicker._getTime(inst) : null);
        },

		/* Retrieve the time directly. */
		_getTime: function(inst) {
			var time = (!inst.timepicker.time || (inst.input && inst.input.val() == '') ? null :
				inst.timepicker.time);
			return time;
		},
	
        /* Set the time for a jQuery selection.
			   @param  target   element - the target input field or division or span
			   @param  date     Date - the new date */
        _setTimeDateTimePicker: function(target, time, noChange) {

			if (time===null) { return; }

            var inst = $.datepicker._getInst(target);

            if (inst) {

               $.dateplustimepicker._setTime(inst, time);

	            // update the input field
	            $.dateplustimepicker.updateDateTime(inst);

	            $.datepicker._updateDatepicker(inst);
            }
        },

		/* Set the time directly. */
		_setTime: function(inst, time) {
			
			if (typeof time == 'object') {
				$.dateplustimepicker._setTimeFromObject(time);
			}
			else if (typeof time == 'string'){
				$.dateplustimepicker._setTimeFromString(inst, time, false);
			}

			// apply min and max constraints
			inst.timepicker.time = $.dateplustimepicker._constraintTime(inst);

			this._notifyTimeChange(inst, false);
			this._notifyTimeChangeStop(inst, false);
			this._notifyDateTimeChange(inst, false);
			this._notifyDateTimeChangeStop(inst, false);
		},

		/* Parse existing datetime and initialise datetime picker. */
		_setDateTimeFromField: function(inst, noDefault) {

			var timeOnly = $.dateplustimepicker._get(inst.timepicker, "timeOnly");
			if (timeOnly) {
				var timeString = inst.input.val();
				$.dateplustimepicker._setTimeFromString(inst, timeString, noDefault);
			}
			else {
				$.datepicker._setDateFromField(inst, noDefault);
			
				var $altTimeField = $($.dateplustimepicker._get(inst.timepicker, "altTimeField"));
				if ($altTimeField.length>0) {
					var timeString = $altTimeField.val();
					$.dateplustimepicker._setTimeFromString(inst, timeString, noDefault);
				}
				else {
					$.dateplustimepicker._setTimeFromDateTimeString(inst, inst.input.val(), noDefault);
				}
			}
			
			// apply min and max constraints
			inst.timepicker.time = $.dateplustimepicker._constraintTime(inst);
			
		},

		/* Extract all possible characters from the time format. */
		_possibleChars: function (format) {

			// numbers mandatory, no matter the format
			var chars = '0123456789';

			var otherChars = format.replace(/[hms]/g, '')
    			.replace(/TT/g, 'APM')
    			.replace(/tt/g, 'apm')
    			.replace(/T/g, 'AP')
    			.replace(/t/g, 'ap');

			chars += otherChars;
			
			return chars;
			
		},

		/* Filter entered characters - based on date and time formats. */
		_doKeyPress: function(event) {

	        var inst = $.datepicker._getInst(event.target);
			var constrainInput = $.datepicker._get(inst, 'constrainInput');

            if (constrainInput) {
				var dateFormat = $.datepicker._get(inst, 'dateFormat');
				var timeFormat = $.dateplustimepicker._get(inst, 'timeFormat')
                var dateChars = $.datepicker._possibleChars(dateFormat);
				var timeChars = $.dateplustimepicker._possibleChars(timeFormat);
				var chars = dateChars + timeChars;
				 
                var chr = String.fromCharCode(event.charCode === undefined ? event.keyCode: event.charCode);

                return event.ctrlKey || (chr < ' ' || !chars || chars.indexOf(chr) > -1);
            }

	    },

		/* Filter entered characters - based on time format. */
		_doKeyPressTime: function(event) {
			
	        var inst = $.data(event.target, 'datepicker') || $.datepicker._getInst(event.target);
			var constrainInput = $.datepicker._get(inst, 'constrainInput');

            if (constrainInput) {
				var timeFormat = $.dateplustimepicker._get(inst, 'timeFormat');
                var chars = $.dateplustimepicker._possibleChars(timeFormat);

                var chr = String.fromCharCode(event.charCode === undefined ? event.keyCode: event.charCode);

                return event.ctrlKey || (chr < ' ' || !chars || chars.indexOf(chr) > -1);
            }

	    },

		/* Synchronise manual entry and field/alternate field. */
		_doKeyUpTime: function(event) {

			var inst = $.data(event.target, 'datepicker') || $.datepicker._getInst(event.target);
			var val = $(event.target).val();
			var lastVal = ($(event.target)==inst.input) ? inst.timepicker.lastVal : inst.timepicker.lastAltFieldVal;
			var lastTime = inst.timepicker.time;

			if (val != lastVal) {
				$.dateplustimepicker._setTimeFromString(inst, val, true); // try to set time
				inst.timepicker.time = $.dateplustimepicker._constraintTime(inst); // apply min and max constraints
	            $.datepicker._updateAlternate(inst); // update the alternate input field
	            $.datepicker._updateDatepicker(inst); // update picker
				if (!lastTime.equals(inst.timepicker.time)) {
					var format = $.dateplustimepicker._get(inst.timepicker, 'timeFormat');
					$(event.target).val(inst.timepicker.time.formatTime(format));
				}
				$(event.target).focus(); // prevent focus stolen if edit on altTimeField
			}
			if (val == "") {
				$.dateplustimepicker._setDateTimeFromField(inst);
				$.datepicker._updateDatepicker(inst);
			}
			return true;
		},

		/* Synchronise manual entry and field/alternate field. */
		_doKeyUp: function(event) {

			var inst = $.datepicker._getInst(event.target);
			var val = $(event.target).val();
			var lastVal = inst.timepicker.lastVal;

			if (val != lastVal) {
				$.dateplustimepicker._setTimeFromDateTimeString(inst, val, true); // try to set time
				inst.timepicker.time = $.dateplustimepicker._constraintTime(inst); // apply min and max constraints
	            $.datepicker._updateAlternate(inst); // update the alternate input field
	            $.datepicker._updateDatepicker(inst); // update picker
			}
			if (val == "") {
				$.dateplustimepicker._setDateTimeFromField(inst);
				$.datepicker._updateDatepicker(inst);
			}
			return true;
		},

		_datepickerUpdateDatePicker: $.datepicker._updateDatepicker,
		/* Generate the datetime picker content. */
		_updateDateTimePicker: function(inst) {
			$.dateplustimepicker._datepickerUpdateDatePicker.apply($.datepicker, [inst]);
			if (inst.timepicker) {
				$.dateplustimepicker.injectTimePicker(inst);
			}
		},

		_datepickerUpdateAlternate: $.datepicker._updateAlternate,
		/* Update any alternate field to synchronise with the main field.
		   @param  inst   Object - datepicker instance */
		_updateAlternate: function(inst) {
			
			if (!inst.timepicker) {
				$.dateplustimepicker._datepickerUpdateAlternate.apply($.datepicker, [inst]);
				return;
			}
			
			var altField = $.datepicker._get(inst, 'altField');
			if (altField) { // update alternate field too
				var altFormat = $.datepicker._get(inst, 'altFormat') || $.datepicker._get(inst, 'dateFormat');
				var date = $.datepicker._getDate(inst);
				var dateStr = $.datepicker.formatDate(altFormat, date, $.datepicker._getFormatConfig(inst));

				var altTimeFormat = $.dateplustimepicker._get(inst.timepicker, 'altTimeFormat') || $.dateplustimepicker._get(inst.timepicker, 'timeFormat');
				var timeStr = inst.timepicker.time.formatTime(altTimeFormat);
				
				var timeOnly = $.dateplustimepicker._get(inst.timepicker, 'timeOnly');
				
				datetimeStr = timeOnly ? timeStr : dateStr + ' ' + timeStr;

				$(altField).each(function() { $(this).val(datetimeStr); });
			}
		},

		_datepickerGotoToday: $.datepicker._gotoToday,
		/* Action for current link. */
		_gotoToday: function(id) {

	        var target = $(id);
	        var inst = $.datepicker._getInst(target[0]);

	        if (inst.timepicker) {
				$.dateplustimepicker._setDateTimeDateTimePicker(target[0], new Date());
			}
			else {
				$.dateplustimepicker._datepickerGotoToday.apply($.datepicker, [id]);
			}

	    },

		_datepickerSetDate: $.datepicker._setDate,
		/* Set the date directly. */
		_setDate: function(inst, date, noChange) {
			$.dateplustimepicker._datepickerSetDate.apply($.datepicker, [inst, date, noChange]);
			if (inst.timepicker) {
				setTimeout(function() {
						$.dateplustimepicker.updateDateTime(inst);
					}
				,100);
			}
		},

	    _datepickerSelectDate: $.datepicker._selectDate,
		/* Update the input field with the selected date and current time. */
 		_selectDate: function(id, dateStr) {

	        var target = $(id);
	        var inst = $.datepicker._getInst(target[0]);

	        if (inst.timepicker) {
				var inline = inst.inline;
	            inst.inline = true;
	            $.dateplustimepicker._datepickerSelectDate.apply($.datepicker, [id, dateStr]);
	            inst.inline = inline;
				$.dateplustimepicker.updateDateTime(inst);
	        }
	        else {
	            $.dateplustimepicker._datepickerSelectDate.apply($.datepicker, [id, dateStr]);
	        }

			$.dateplustimepicker._notifyDateTimeChange(inst, true);
			$.dateplustimepicker._notifyDateTimeChangeStop(inst, true);
	    }

    });

    /* extend timepicker to datepicker */
    jQuery.fn.dateplustimepicker = function(options) {

		var otherArgs = Array.prototype.slice.call(arguments, 1);
        if (typeof(options) == 'string') {

			if (options == 'getDateTime' || options == 'getTime' || 
				(options == 'option' && arguments.length == 2 && typeof arguments[1] == 'string'))
				return $.dateplustimepicker['_' + options + 'DateTimePicker'].apply($.dateplustimepicker, [this[0]].concat(otherArgs));

            if (options == 'setDateTime' || options == 'setTime' || options == 'option')
 				return this.each(function() {
					$.dateplustimepicker['_' + options + 'DateTimePicker'].apply($.dateplustimepicker, [this].concat(otherArgs));
				});

            if (options == 'dialog')
            	return this.datepicker(options, arguments[1], arguments[2], arguments[3], arguments[4]);

            return this.datepicker(options);
        }


		var settings = $.extend({}, {
				currentText: $.dateplustimepicker._defaults.currentText
			},
			options
		);

		this.datepicker(settings);
		this.each(function() {
			$.dateplustimepicker._attachDateplustimepicker(this, options);
		});
		return this;

    };

    /* shorthand just to use timepicker.. */
    jQuery.fn.timepicker = function(opts) {
        if (typeof opts == 'object')
        opts = $.extend(opts, {
            timeOnly: true
        });

        return $(this).dateplustimepicker(opts, arguments[1], arguments[2], arguments[3], arguments[4]);
    };

	/* jQuery extend now ignores nulls! */
    function extendRemove(target, props) {
        $.extend(target, props);
        for (var name in props)
        	if (props[name] == null || props[name] == undefined)
        		target[name] = props[name];
        return target;
    };

    $.dateplustimepicker = new Dateplustimepicker(); // singleton instance

	// date picker hacks
	$.datepicker._updateDatepicker = $.dateplustimepicker._updateDateTimePicker;
	$.datepicker._gotoToday = $.dateplustimepicker._gotoToday;
	$.datepicker._selectDate = $.dateplustimepicker._selectDate;
	$.datepicker._setDate = $.dateplustimepicker._setDate;
	$.datepicker._updateAlternate = $.dateplustimepicker._updateAlternate;

})(jQuery);