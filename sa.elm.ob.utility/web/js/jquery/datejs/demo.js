$(document).ready(function() {
	 
/*
 * Start
 */
 

	$('#inpReporting').dateplustimepicker({
		dateFormat: 'dd-mm-yy',
		timeFormat: 'hh:mm:ss',
		altField: '#testAlt',
		altTimeFormat: 'h:m:s',		
		minTime: 0,
		maxTime: { hours: 24 },	
		show: 'fold',
		showButtonPanel: true	
		
	});
	
	/*
	 * Ends here
	 */
	
	 

});
