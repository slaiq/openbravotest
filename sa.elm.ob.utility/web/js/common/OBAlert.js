var isOBDialogOpen = false;
function OBAlert(message, title, callback) {
	if (existOBDialog())
		return;
	if (typeof (message) != "string")
		message = "";
	if (typeof (title) != undefined && typeof (title) == "function") {
		callback = title;
		title = "";
	}
	addMessage(message, title, "A", "O", "W", function(r) {
		isOBDialogOpen = false;
		if (callback && r != -1) {
			callback(r);
		}
	});
}
function OBInfo(message, title, callback) {
	if (existOBDialog())
		return;
	if (typeof (message) != "string")
		message = "";
	if (typeof (title) != undefined && typeof (title) == "function") {
		callback = title;
		title = "";
	}
	addMessage(message, title, "I", "O", "I", function(r) {
		isOBDialogOpen = false;
		if (callback && r != -1) {
			callback(r);
		}
	});
}
function OBConfirm(message, title, callback) {
	if (existOBDialog())
		return;
	if (typeof (message) != "string")
		message = "";
	if (typeof (title) != undefined && typeof (title) == "function") {
		callback = title;
		title = "";
	}
	addMessage(message, title, "C", "OC", "C", function(r) {
		isOBDialogOpen = false;
		if (callback && r != -1) {
			callback(r);
		}
	});
}
function OBAsk(message, title, callback) {
	if (existOBDialog())
		return;
	if (typeof (message) != "string")
		message = "";
	if (typeof (title) != undefined && typeof (title) == "function") {
		callback = title;
		title = "";
	}
	addMessage(message, title, "C", "YN", "A", function(r) {
		isOBDialogOpen = false;
		if (callback && r != -1) {
			callback(r);
		}
	});
}
function existOBDialog() {
	if (document.getElementById("Dialog_Overlay") && document.getElementById("Dialog_Overlay").style.display == "" && document.getElementById("Dialog_Container")
			&& document.getElementById("Dialog_Container").style.display == "")
		return true;
	else
		return false;
}
// type - Alert(A), Information(I), Confirm(C)
// button - O(Ok), OC(OK, Cancel), YN(Yes, No)
// image - Warning(W), Information(I), Confirm(C), Ask(A)
function addMessage(message, title, type, button, image, callback) {
	if (typeof Grids != 'undefined')
		Grids.Focused = null;
	isOBDialogOpen = true;
	// Overlay Div
	var overlayDiv = document.createElement('div');
	overlayDiv.style.position = "fixed";
	overlayDiv.style.zIndex = "5000000";
	overlayDiv.style.width = "100%";
	overlayDiv.style.height = "100%";
	overlayDiv.style.top = "0px";
	overlayDiv.style.left = "0px";
	overlayDiv.style.backgroundColor = "#000000";
	overlayDiv.style.opacity = 0.5;
	overlayDiv.style.filter = 'alpha(opacity=50)';
	overlayDiv.id = "Dialog_Overlay";
	// Container Div
	var containerDiv = document.createElement('div');
	containerDiv.style.position = "absolute";
	containerDiv.style.zIndex = "5000001";
	containerDiv.style.display = "none";
	containerDiv.style.width = "450px";
	containerDiv.style.height = "150px";
	containerDiv.style.padding = "0";
	containerDiv.style.margin = "0";
	containerDiv.style.backgroundColor = "#FFFFFF";
	containerDiv.id = "Dialog_Container";
	if (document.body.firstChild) {
		document.body.insertBefore(overlayDiv, document.body.firstChild);
		document.body.insertBefore(containerDiv, document.body.firstChild);
	}
	else {
		document.body.appendChild(overlayDiv);
		document.body.appendChild(containerDiv);
	}
	// Positioning Center
	var windowWidth, windowHeight;
	if (self.innerHeight) {
		windowWidth = self.innerWidth;
		windowHeight = self.innerHeight;
	}
	else if (document.body) {
		windowWidth = document.body.clientWidth;
		windowHeight = document.body.clientHeight;
	}
	var top = (windowHeight / 2) - 125;
	var left = (windowWidth / 2) - 220;
	var right = (windowWidth / 2) - 220;
	if (top < 0)
		top = 0;
	if (left < 0)
		left = 0;
	containerDiv.style.top = top + "px";
	containerDiv.style.left = left + "px";
	containerDiv.style.right = right + "px";
	// Set InnerHTML
	var headerText = title;
	if (title == "" || title == undefined) {
		if (type == "A")
			headerText = "Alert";
		else if (type == "I")
			headerText = "Information";
		else if (type == "C")
			headerText = "Confirm";
	}
	// Set Header Info
	var pad = '0';
	if (navigator.appName.toLowerCase().indexOf("microsoft") != -1)
		pad = '4';
	var html = '<table id="Dialog_HeaderMovable" cellspacing="0" cellpadding="2" style="-moz-border-image: none; width: 100%; border: 2px solid #72AB10; border-top: 0px;">'
			+ '<tbody><tr style="background: transparent url(\'../web/images/dialog/DialogHeaderBG.png\') repeat-x left top;  width: 100%; height: 27px;">'
			+ '<td align="left" valign="middle" style="width: 20px;"><div style="margin-left: 4px; margin-top: 2px;"><img alt="Openbravo" src="../web/images/dialog/DialogHeaderIcon.png"></div></td>'
			+ '<td align="center" valign="middle" style="cursor: move; padding: ' + pad
			+ 'px 0;"><span id="Dialog_HeaderText" style="color: rgb(255, 255, 255); font-family: Arial,sans-serif; font-size: 15px; font-weight: bold;">' + headerText + '</span></td>'
			+ '<td align="right" valign="middle" style="width: 20px;"><div style="margin-right: 4px; margin-top: 2px;">'
			+ '<img id="Dialog_Close" alt="Close" src="../web/images/dialog/DialogHeaderClose.png" style="cursor: pointer;" onclick="" ></div></td>' + '</tr></tbody></table>';
	// Set Message Info
	html += '<table id="Dialog_Body" cellspacing="0" cellpadding="2" style="width: 100%; height: 123px; border: 2px solid #72AB10; border-top: 0px;">'
			+ '<tr><td style="vertical-align: top;"><div style="margin-left: 20px; margin-top: 20px;">';
	if (image == "W")
		html += '<img alt="B" src="../web/images/dialog/warning.png"></img>';
	if (image == "I")
		html += '<img alt="B" src="../web/images/dialog/information.png"></img>';
	if (image == "C")
		html += '<img alt="B" src="../web/images/dialog/confirm.png"></img>';
	if (image == "A")
		html += '<img alt="B" src="../web/images/dialog/ask.png"></img>';
	html += '</div></td><td><p style="max-height: 70px; overflow: auto; margin: 10px 10px 0px 10px; white-space: normal !important;float: none;font-family: sans-serif,\'lucida sans\' !important; font-weight:400;  font-size: 13px !important;">'
			+ message + '</p></td>' + '</tr><tr><td colspan="2" align="center" valign="middle"><table><tr>';
	if (button == "O")
		html += '<td><input type="button" id="Dialog_OK" style="float: left; border: 2px solid #FA962F;color: #000000;cursor: pointer;font-family: \'lucida sans\',sans-serif; font-size: 12px;padding: 4px 20px; background: transparent url(\'../web/images/dialog/DialogButton.png\') repeat-x left top;" onmouseover="this.style.backgroundImage=\'url(../web/images/dialog/DialogButtonHover.png)\';"onmouseout="this.style.backgroundImage=\'url(../web/images/dialog/DialogButton.png)\';" value="OK"></input></td>';
	else if (button == "OC") {
		html += '<td><input type="button" id="Dialog_OK"     style="float: left; border: 2px solid #FA962F;color: #000000;cursor: pointer;font-family: \'lucida sans\',sans-serif; font-size: 12px;padding: 4px 20px; background: transparent url(\'../web/images/dialog/DialogButton.png\') repeat-x left top;" onmouseover="this.style.backgroundImage=\'url(../web/images/dialog/DialogButtonHover.png)\';"onmouseout="this.style.backgroundImage=\'url(../web/images/dialog/DialogButton.png)\';" value="OK"></input></td>';
		html += '<td><input type="button" id="Dialog_CANCEl" style="float: left; border: 2px solid #EFEFEF;color: #000000;cursor: pointer;font-family: \'lucida sans\',sans-serif; font-size: 12px;padding: 4px 20px; background: transparent url(\'../web/images/dialog/DialogButton.png\') repeat-x left top;" onmouseover="this.style.backgroundImage=\'url(../web/images/dialog/DialogButtonHover.png)\';"onmouseout="this.style.backgroundImage=\'url(../web/images/dialog/DialogButton.png)\';" value="Cancel"></input></td>';
	}
	else if (button == "YN") {
		html += '<td><input type="button" id="Dialog_OK"     style="float: left; border: 2px solid #FA962F;color: #000000;cursor: pointer;font-family: \'lucida sans\',sans-serif; font-size: 12px;padding: 4px 20px; background: transparent url(\'../web/images/dialog/DialogButton.png\') repeat-x left top;" onmouseover="this.style.backgroundImage=\'url(../web/images/dialog/DialogButtonHover.png)\';"onmouseout="this.style.backgroundImage=\'url(../web/images/dialog/DialogButton.png)\';" value="Yes"></input></td>';
		html += '<td><input type="button" id="Dialog_CANCEl" style="float: left; border: 2px solid #EFEFEF;color: #000000;cursor: pointer;font-family: \'lucida sans\',sans-serif; font-size: 12px;padding: 4px 20px; background: transparent url(\'../web/images/dialog/DialogButton.png\') repeat-x left top;" onmouseover="this.style.backgroundImage=\'url(../web/images/dialog/DialogButtonHover.png)\';"onmouseout="this.style.backgroundImage=\'url(../web/images/dialog/DialogButton.png)\';" value="No"></input></td>';
	}
	html += '</tr></table></td></tr></table>';
	containerDiv.innerHTML = html;
	// Set Events
	var okButton = document.getElementById("Dialog_OK");
	var cancelButton = document.getElementById("Dialog_CANCEl");
	overlayDiv.onclick = function(e) {
		if (cancelButton != undefined && cancelButton.style.borderColor == "rgb(250, 150, 47)") {
			cancelButton.style.border = "2px solid #FA962F";
			okButton.style.border = "2px solid #EFEFEF";
			cancelButton.focus();
		}
		else {
			okButton.style.border = "2px solid #FA962F";
			okButton.focus();
		}
		containerDiv.style.transform = "scale3d(1.1, 1.1, 1.1)";
		setTimeout(function() {
			document.getElementById("Dialog_Container").style.transform = "none";
		}, 100);
	}
	containerDiv.style.transition = "all 0.05s ease-in-out";
	containerDiv.onclick = function(e) {
		if (cancelButton != undefined && cancelButton.style.borderColor == "rgb(250, 150, 47)") {
			cancelButton.style.border = "2px solid #FA962F";
			okButton.style.border = "2px solid #EFEFEF";
			cancelButton.focus();
		}
		else {
			okButton.style.border = "2px solid #FA962F";
			okButton.focus();
		}
	}
	document.getElementById("Dialog_Close").onclick = function(e) {
		containerDiv.parentNode.removeChild(containerDiv);
		overlayDiv.style.display = "none";
		callback(-1);
	}
	okButton.onkeydown = function(e) {
		if (typeof Grids != 'undefined')
			Grids.Focused = null;
		e = e || window.event;
		var keyCode = e.keyCode || e.which;
		if (keyCode == 13 || keyCode == 27) {
			containerDiv.parentNode.removeChild(containerDiv);
			overlayDiv.style.display = "none";
			if (callback && keyCode == 13)
				callback(true);
			else
				callback(false);
		}
		if (keyCode == 9 || keyCode == 37 || keyCode == 39) {
			if (button == "O") {
				okButton.style.border = "2px solid #FA962F";
				setTimeout(function() {
					okButton.focus()
				}, 5);
			}
			else {
				okButton.style.border = "2px solid #EFEFEF";
				cancelButton.style.border = "2px solid #FA962F";
				setTimeout(function() {
					cancelButton.focus()
				}, 5);
			}
		}
	}
	okButton.onclick = function(e) {
		containerDiv.parentNode.removeChild(containerDiv);
		overlayDiv.style.display = "none";
		if (callback)
			callback(true);
	}
	if (button != "O") {
		cancelButton.onkeydown = function(e) {
			if (typeof Grids != 'undefined')
				Grids.Focused = null;
			e = e || window.event;
			var keyCode = e.keyCode || e.which;
			if (keyCode == 13 || keyCode == 27) {
				containerDiv.parentNode.removeChild(containerDiv);
				overlayDiv.style.display = "none";
				if (callback)
					callback(false);
			}
			if (keyCode == 9 || keyCode == 37 || keyCode == 39) {
				cancelButton.style.border = "2px solid #EFEFEF";
				okButton.style.border = "2px solid #FA962F";
				setTimeout(function() {
					okButton.focus()
				}, 5);
			}
		}
		cancelButton.onclick = function(e) {
			containerDiv.parentNode.removeChild(containerDiv);
			overlayDiv.style.display = "none";
			if (callback)
				callback(false);
		}
	}
	// Set Drag
	var Drag = {
		obj : null,
		init : function(o, oRoot, minX, maxX, minY, maxY, bSwapHorzRef, bSwapVertRef, fXMapper, fYMapper) {
			o.onmousedown = Drag.start;
			o.hmode = bSwapHorzRef ? false : true;
			o.vmode = bSwapVertRef ? false : true;
			o.root = oRoot && oRoot != null ? oRoot : o;
			if (o.hmode && isNaN(parseInt(o.root.style.left)))
				o.root.style.left = "0px";
			if (o.vmode && isNaN(parseInt(o.root.style.top)))
				o.root.style.top = "0px";
			if (!o.hmode && isNaN(parseInt(o.root.style.right)))
				o.root.style.right = "0px";
			if (!o.vmode && isNaN(parseInt(o.root.style.bottom)))
				o.root.style.bottom = "0px";
			o.minX = typeof minX != 'undefined' ? minX : null;
			o.minY = typeof minY != 'undefined' ? minY : null;
			o.maxX = typeof maxX != 'undefined' ? maxX : null;
			o.maxY = typeof maxY != 'undefined' ? maxY : null;
			o.xMapper = fXMapper ? fXMapper : null;
			o.yMapper = fYMapper ? fYMapper : null;
			o.root.onDragStart = new Function();
			o.root.onDragEnd = new Function();
			o.root.onDrag = new Function();
		},
		start : function(e) {
			var o = Drag.obj = this;
			e = Drag.fixE(e);
			var y = parseInt(o.vmode ? o.root.style.top : o.root.style.bottom);
			var x = parseInt(o.hmode ? o.root.style.left : o.root.style.right);
			o.root.onDragStart(x, y);
			o.lastMouseX = e.clientX;
			o.lastMouseY = e.clientY;
			if (o.hmode) {
				if (o.minX != null)
					o.minMouseX = e.clientX - x + o.minX;
				if (o.maxX != null)
					o.maxMouseX = o.minMouseX + o.maxX - o.minX;
			}
			else {
				if (o.minX != null)
					o.maxMouseX = -o.minX + e.clientX + x;
				if (o.maxX != null)
					o.minMouseX = -o.maxX + e.clientX + x;
			}
			if (o.vmode) {
				if (o.minY != null)
					o.minMouseY = e.clientY - y + o.minY;
				if (o.maxY != null)
					o.maxMouseY = o.minMouseY + o.maxY - o.minY;
			}
			else {
				if (o.minY != null)
					o.maxMouseY = -o.minY + e.clientY + y;
				if (o.maxY != null)
					o.minMouseY = -o.maxY + e.clientY + y;
			}
			document.onmousemove = Drag.drag;
			document.onmouseup = Drag.end;
			return false;
		},
		drag : function(e) {
			e = Drag.fixE(e);
			var o = Drag.obj;
			var ey = e.clientY;
			var ex = e.clientX;
			var y = parseInt(o.vmode ? o.root.style.top : o.root.style.bottom);
			var x = parseInt(o.hmode ? o.root.style.left : o.root.style.right);
			var nx, ny;
			if (o.minX != null)
				ex = o.hmode ? Math.max(ex, o.minMouseX) : Math.min(ex, o.maxMouseX);
			if (o.maxX != null)
				ex = o.hmode ? Math.min(ex, o.maxMouseX) : Math.max(ex, o.minMouseX);
			if (o.minY != null)
				ey = o.vmode ? Math.max(ey, o.minMouseY) : Math.min(ey, o.maxMouseY);
			if (o.maxY != null)
				ey = o.vmode ? Math.min(ey, o.maxMouseY) : Math.max(ey, o.minMouseY);
			nx = x + ((ex - o.lastMouseX) * (o.hmode ? 1 : -1));
			ny = y + ((ey - o.lastMouseY) * (o.vmode ? 1 : -1));
			if (ny <= 0) {
				ny = 0;
			}
			if (nx <= 0)
				nx = 0;
			var w = 0, h = 0;
			if (self.innerHeight) {
				w = self.innerWidth;
				h = self.innerHeight;
			}
			else if (document.documentElement && document.documentElement.clientHeight) {
				w = document.documentElement.clientWidth;
				h = document.documentElement.clientHeight;
			}
			else if (document.body) {
				w = document.body.clientWidth;
				h = document.body.clientHeight;
			}
			;
			if (h < (ny + 140))
				ny = h - 143;
			if (w < (nx + 450))
				nx = w - 450;
			if (o.xMapper)
				nx = o.xMapper(y)
			else if (o.yMapper)
				ny = o.yMapper(x)
			Drag.obj.root.style[o.hmode ? "left" : "right"] = nx + "px";
			Drag.obj.root.style[o.vmode ? "top" : "bottom"] = ny + "px";
			Drag.obj.lastMouseX = ex;
			Drag.obj.lastMouseY = ey;
			Drag.obj.root.onDrag(nx, ny);
			return false;
		},
		end : function() {
			document.onmousemove = null;
			document.onmouseup = null;
			Drag.obj.root.onDragEnd(parseInt(Drag.obj.root.style[Drag.obj.hmode ? "left" : "right"]), parseInt(Drag.obj.root.style[Drag.obj.vmode ? "top" : "bottom"]));
			Drag.obj = null;
			if (cancelButton != undefined && cancelButton.style.borderColor == "rgb(250, 150, 47)") {
				cancelButton.style.border = "2px solid #FA962F";
				okButton.style.border = "2px solid #EFEFEF";
				cancelButton.focus();
			}
			else {
				okButton.style.border = "2px solid #FA962F";
				okButton.focus();
			}
		},
		fixE : function(e) {
			if (typeof e == 'undefined')
				e = window.event;
			if (typeof e.layerX == 'undefined')
				e.layerX = e.offsetX;
			if (typeof e.layerY == 'undefined')
				e.layerY = e.offsetY;
			return e;
		}
	};
	Drag.init(document.getElementById("Dialog_HeaderMovable"), containerDiv);
	// Set Display
	containerDiv.style.display = "";
	document.getElementById("Dialog_OK").focus();
}