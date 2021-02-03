function getUmmalQura(id) {
	$('#' + id).calendarsPicker({
		calendar : $.calendars.instance('ummalqura'),
		dateFormat : 'dd-mm-yyyy'
	});
}
function renderInputHijriDate() {
	$("input[datatype='hijridate']").get().forEach(function(item, index) {
		getUmmalQura(item.id);
		document.getElementById(item.id).focusLogic = function() {
			return true;
		}
	});
}