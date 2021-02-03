var _today = isc.DateUtil.getStartOf(new Date(2012, 6, 5), "W");

var _start = _today.getDate() - _today.getDay();
var _month = _today.getMonth();
var _year = _today.getFullYear();

var events = [

{
    eventId: 1, 
    name: "Add new Timeline view",
    description: "Add a new calendar Timeline component",
    startDate: new Date(_year, _month, _start + 2),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 8), "D"),
    lane: "darcyFeeney"
},
{
    eventId: 2,
    name: "ListGrid field autoSize",
    description: "Complex field-autosizing in ListGrid",
    startDate: new Date(_year, _month, _start),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start), "D"),
    lane: "kaiKong"
},
{
    eventId: 3,
    name: "PDF Import/Export",
    description: "Implement native PDF import/export",
    startDate: new Date(_year, _month, _start + 1),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 6), "D"),
    lane: "garretMonroe"
},
{
    eventId: 4, 
    name: "Calculated Fields",
    description: "Formula and Summary fields for ListGrid",
    startDate: new Date(_year, _month, _start),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 4), "D"),
    lane: "charlesMadigen"
},
{
    eventId: 5,
    name: "ListGrid cell-level selection",
    description: "Implement spreadsheet-like selection in ListGrid",
    startDate: new Date(_year, _month, _start + 7),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 14), "D"),
    lane: "charlesMadigen"
},
{
    eventId: 6,
    name: "Text import",
    description: "Server text-import",
    startDate: new Date(_year, _month, _start + 16),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 20), "D"),
    lane: "charlesMadigen"
},
{
    eventId: 7,
    name: "TabIndex enhancements",
    description: "Enhance formItem tabindex handling",
    startDate: new Date(_year, _month, _start + 9),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 11), "D"),
    lane: "kaiKong"
},
{
    eventId: 8,
    name: "Visual Builder skin",
    description: "Skinning changes",
    startDate: new Date(_year, _month, _start),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 3), "D"),
    lane: "shellyFewel"
},
{
    eventId: 9,
    name: "DataSource Transaction-handling",
    description: "New transaction features",
    startDate: new Date(_year, _month, _start),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 2), "D"),
    lane: "tamaraKane"
},
{
    eventId: 10,
    name: "New Samples",
    description: "Add 20 samples for the following new features: ...",
    startDate: new Date(_year, _month, _start + 4),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 20), "D"),
    lane: "tamaraKane"
},
{
    eventId: 11,
    name: "Localization",
    description: "Extend i18n support",
    startDate: new Date(_year, _month, _start + 9),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 14), "D"),
    lane: "darcyFeeney"
},
{
    eventId: 12,
    name: "New Language Packs",
    description: "Add these 4 new language packs: ...",
    startDate: new Date(_year, _month, _start + 16),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 18), "D"),
    lane: "darcyFeeney"
},
{
    eventId: 13,
    name: "ComponentXML",
    description: "Add the following features and update documentation: ...",
    startDate: new Date(_year, _month, _start + 5),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 11), "D"),
    lane: "shellyFewel"
},
{
    eventId: 14,
    name: "TileGrid",
    description: "Change styling on builtin tiles as follows: ...",
    startDate: new Date(_year, _month, _start + 14),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 19), "D"),
    lane: "shellyFewel"
},
{
    eventId: 15,
    name: "Dev Meeting",
    description: "Weekly dev meeting",
    startDate: new Date(_year, _month, _start + 1),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 1), "D"),
    lane: "charlesMadigen",
    eventWindowStyle: "testStyle",
    canEdit: false
},
{
    eventId: 16,
    name: "Dev Meeting",
    description: "Weekly dev meeting",
    startDate: new Date(_year, _month, _start + 8),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 8), "D"),
    lane: "charlesMadigen",
    eventWindowStyle: "testStyle",
    canEdit: false
},
{
    eventId: 17,
    name: "Dev Meeting",
    description: "Weekly dev meeting",
    startDate: new Date(_year, _month, _start + 15),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 15), "D"),
    lane: "charlesMadigen",
    eventWindowStyle: "testStyle",
    canEdit: false
},
{
    eventId: 18,
    name: "Oracle enhancements",
    description: "Add the following 11g-specific enhancements: ...",
    startDate: new Date(_year, _month, _start + 7),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 9), "D"),
    lane: "garretMonroe"
},
{
    eventId: 19,
    name: "Client export",
    description: "Excel export alterations",
    startDate: new Date(_year, _month, _start + 11),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 14), "D"),
    lane: "garretMonroe"
},
{
    eventId: 20,
    name: "Record Components",
    description: "New ListGrid recordComponent modes: ...",
    startDate: new Date(_year, _month, _start + 16),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 20), "D"),
    lane: "garretMonroe"
},
{
    eventId: 21,
    name: "SQLDataSource",
    description: "Enhancements to customSQL support",
    startDate: new Date(_year, _month, _start + 2),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 4), "D"),
    lane: "kaiKong"
},
{
    eventId: 22,
    name: "includeFrom",
    description: "Update support via includeFrom",
    startDate: new Date(_year, _month, _start + 6),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 8), "D"),
    lane: "kaiKong"
},
{
    eventId: 23,
    name: "FileItem",
    description: "Add milti-file upload support",
    startDate: new Date(_year, _month, _start + 14),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 16), "D"),
    lane: "kaiKong"
},
{
    eventId: 24,
    name: "Doc viewer",
    description: "Enhance documentation viewer with these additional syntax-hilites: ...",
    startDate: new Date(_year, _month, _start + 18),
    endDate: isc.DateUtil.getEndOf(new Date(_year, _month, _start + 19), "D"),
    lane: "kaiKong"
}

];

