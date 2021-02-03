OB.EUT = {};
OB.EUT.defaultJSDateFormat = 'dd-MM-yyyy';
OB.EUT.defaultJSDateTimeFormat = 'dd-MM-yyyy HH:mm:ss';
var QUCallback = function(response, data, request) {
	OB.EUT.defaultJSDateFormat = data.defaultJSDateFormat;
	OB.EUT.defaultJSDateTimeFormat = data.defaultJSDateTimeFormat;
};
OB.RemoteCallManager.call('sa.elm.ob.utility.ad_callouts.UtilityCallout', {
	action : 'getJSDateFormat'
}, {}, QUCallback);
