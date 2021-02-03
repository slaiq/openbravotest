var focusedWindowElement = null;
var focusedWindowElement_tmp = null;
var focusedWindowElement_tmp2 = null;
var drawnWindowElement = null;
var windowTableParentElement = null;
var focusedWindowTable = 0;
var frameLocked = false;
var currentWindowElementType = null;
var previousWindowElementType = null;
var selectedArea = "window";
var isGridFocused = null;
var isGenericTreeFocused = null;
var isClickOnGrid = null;
var isClickOnGenericTree = null;
var defaultActionElement = null;
var defaultActionType = null;
var isTabPressed = null;
var isOBTabBehavior = true;
var isFirstTime = true;
var isReadOnlyWindow = false;
var selectInputTextOnTab = true;
var isGoingDown = null;
var isGoingUp = null;
var selectedCombo = null;
var isSelectedComboOpened = null;
var hasCloseWindowSearch = null;
var propagateEnter = true;
var isContextMenuOpened = false;
windowKeyboardCaptureEvents();
function windowKeyboardCaptureEvents() {
	if (typeof captureOnMouseDown == "undefined" || captureOnMouseDown != false) {
		document.onmousedown = mouseDownLogic;
		if (document.layers) {
			window.captureEvents(Event.ONMOUSEDOWN);
			window.onmousedown = mouseDownLogic;
		}
	}
	if (typeof captureOnClick == "undefined" || captureOnClick != false) {
		document.onclick = mouseClickLogic;
		if (document.layers) {
			window.captureEvents(Event.ONCLICK);
			window.onclick = mouseClickLogic;
		}
	}
}
function activateDefaultAction() {
	if (defaultActionElement != null && defaultActionElement != "null"
			&& defaultActionElement != "") {
		if (isSelectedComboOpened != true) {
			defaultActionType = "button";
			drawWindowElementDefaultAction(defaultActionElement);
		} else {
			comboDefaultAction();
		}
	}
}
function disableDefaultAction() {
	if (defaultActionElement != null && defaultActionElement != "null"
			&& defaultActionElement != "") {
		defaultActionType = null;
		eraseWindowElementDefaultAction(defaultActionElement);
	}
}
function comboDefaultAction() {
	if (defaultActionElement != null && defaultActionElement != "null"
			&& defaultActionElement != "") {
		eraseWindowElementDefaultAction(defaultActionElement);
		defaultActionType = "combo";
	}
}
function defaultActionLogic(c) {
	disableDefaultAction();
	defaultActionElement = document
			.getElementById(windowTables[focusedWindowTable].defaultActionButtonId);
	try {
		if ((c.tagName == "INPUT" && c.getAttribute("type") != "file")
				|| c.tagName == "SELECT") {
			activateDefaultAction();
			for ( var a = 0; a < keyArray.length; a++) {
				if (keyArray[a] != null && keyArray[a]) {
					if (keyArray[a].key == "ENTER") {
						if (keyArray[a].auxKey == null
								|| keyArray[a].auxKey == ""
								|| keyArray[a].auxKey == "null") {
							if (keyArray[a].field == c.getAttribute("name")) {
								disableDefaultAction();
								break;
							}
						}
					}
				}
			}
		} else {
			disableDefaultAction();
		}
	} catch (b) {
	}
}
function enableDefaultAction() {
	defaultActionElement = null;
	activateDefaultAction();
}
function activeElementFocus() {
	if (focusedWindowElement && selectedArea == "window") {
		putWindowElementFocus(focusedWindowElement);
	}
}
function setSelectedArea(a) {
	selectedArea = a;
}
function swichSelectedArea() {
	if (selectedArea == "window" && tabsTables[0].tabTableId) {
		previousWindowElementType = currentWindowElementType;
		currentWindowElementType = "tab";
		focusedWindowElement_tmp2 = focusedWindowElement;
		selectedArea = "tabs";
		removeWindowElementFocus(focusedWindowElement);
		setActiveTab();
	} else {
		if (selectedArea == "tabs" && !isReadOnlyWindow) {
			selectedArea = "window";
			removeTabFocus(focusedTab);
			currentWindowElementType = previousWindowElementType;
			setWindowElementFocus(focusedWindowElement_tmp2);
		} else {
			return false;
		}
	}
}
function windowTableId(a, b) {
	this.tableId = a;
	this.defaultActionButtonId = b;
}
function setWindowTableParentElement() {
	setMDIEnvironment();
	windowTableParentElement = windowTables[focusedWindowTable].tableId;
}
var isOnMouseDown = null;
function mouseDownLogic(a, d) {
	if (d == null) {
		d = (!document.all) ? a.target : event.srcElement;
	}
	if (d.tagName == "OPTION") {
		while (d.tagName != "SELECT") {
			d = d.parentNode;
		}
	}
	if (d.tagName == "TD") {
		try {
			var b = getObjParent(getObjParent(getObjParent(getObjParent(d))));
			if (b.tagName == "BUTTON") {
				d = b;
			}
		} catch (c) {
		}
	}
	if (d.tagName == "IMG") {
		try {
			var b = getObjParent(getObjParent(getObjParent(getObjParent(getObjParent(d)))));
			if (b.tagName == "BUTTON") {
				d = b;
			}
		} catch (c) {
		}
	}
	if (checkGenericTree(d)) {
		return true;
	}
	cursorFocus(d, "onmousedown");
	if (d.tagName == "SELECT") {
		comboKeyBehaviour(d, "onmousedown");
	}
	if (hasCloseWindowSearch) {
		closeWindowSearch();
		hasCloseWindowSearch = false;
	}
}
function mouseClickLogic(a, d) {
	if (d == null) {
		d = (!document.all) ? a.target : event.srcElement;
	}
	if (d.tagName == "OPTION") {
		while (d.tagName != "SELECT") {
			d = d.parentNode;
		}
	}
	if (d.tagName == "TD") {
		try {
			var b = getObjParent(getObjParent(getObjParent(getObjParent(d))));
			if (b.tagName == "BUTTON") {
				d = b;
			}
		} catch (c) {
		}
	}
	if (d.tagName == "IMG") {
		try {
			var b = getObjParent(getObjParent(getObjParent(getObjParent(getObjParent(d)))));
			if (b.tagName == "BUTTON") {
				d = b;
			}
		} catch (c) {
		}
	}
	if (checkGenericTree(d)) {
		return true;
	}
	cursorFocus(d, "onclick");
	if (d.tagName == "SELECT") {
		comboKeyBehaviour(d, "onclick");
	}
}
function cursorFocus(b, a) {
	isContextMenuOpened = false;
	if (b == null || b == "null" || b == "") {
		return false;
	}/*
	if (navigator.userAgent.toUpperCase().indexOf("MSIE") != -1
			&& b.getAttribute("type") == "checkbox"
			&& (b.getAttribute("readonly") == "true" || b.readOnly)) {
		return false;
	}*/
	if (a == "onmousedown") {
		if (b == drawnWindowElement) {
			return true;
		}
		if (!isClickOnGrid == true) {
			blurGrid();
		}
		isClickOnGrid = false;
	}
	if (isInsideWindowTable(b) && couldHaveFocus(b) && a == "onmousedown") {
		removeTabFocus(focusedTab);
		frameLocked = false;
		selectedArea = "window";
		focusedWindowElement = b;
		setWindowElementFocus(focusedWindowElement, "obj", a);
	} else {
		if (a == "onclick") {
			if (selectedArea == "window") {
				if (b != focusedWindowElement) {
					eraseWindowElementFocus(focusedWindowElement);
				}
			} else {
				if (selectedArea == "tabs") {
					setTabFocus(focusedTab);
				}
			}
		}
	}
	return true;
}
function checkGenericTree(a) {
	if (a == null || a == "null" || a == "") {
		return false;
	}
	if (!isClickOnGenericTree == true) {
		blurGenericTree();
	}
	isClickOnGenericTree = false;
	if (isGenericTreeFocused) {
		while (a.tagName != "BODY") {
			if (a.getAttribute("id") != null) {
				if (a.getAttribute("id").indexOf("genericTree") != -1) {
					return true;
				}
			}
			a = a.parentNode;
		}
	}
	return false;
}
function comboKeyBehaviour(b, a) {
	if (b == null || b == "null" || b == "") {
		return false;
	}
	if (a == "onmousedown") {
		isOnMouseDown = true;
		if (focusedWindowElement == null) {
			focusedWindowElement = "";
		}
		if (b.tagName == "SELECT" && isSelectedComboOpened == true
				&& selectedCombo == b) {
			selectedCombo = b;
			isSelectedComboOpened = false;
			activateDefaultAction();
		} else {
			if (b.tagName == "SELECT") {
				selectedCombo = b;
				isSelectedComboOpened = true;
				comboDefaultAction();
			} else {
				if (focusedWindowElement.tagName == "SELECT") {
					selectedCombo = b;
					isSelectedComboOpened = false;
					activateDefaultAction();
				}
			}
		}
		return true;
	} else {
		if (a == "onclick") {
			if (b.tagName == "SELECT" && isOnMouseDown != true) {
				selectedCombo = b;
				isSelectedComboOpened = false;
				activateDefaultAction();
			}
			isOnMouseDown = false;
			return true;
		}
	}
	return false;
}
function isInsideWindowTable(c) {
	try {
		for (;;) {
			if (c == null) {
				return false;
			}
			c = c.parentNode;
			for ( var a = 0; a < windowTables.length; a++) {
				if (c == document.getElementById(windowTables[a].tableId)) {
					focusedWindowTable = a;
					setWindowTableParentElement();
					return true;
				}
			}
		}
	} catch (b) {
		return false;
	}
}
function setWindowElementFocus(c, a, b) {
	if (a == null || a == "null" || a == "") {
		a = "obj";
	} else {
		if (a == "id") {
			c = document.getElementById(c);
		}
	}
	if (a == "id" && !canHaveFocus(c)) {
		setFirstWindowElementFocus();
		return false;
	}
	if (c == "firstElement") {
		setFirstWindowElementFocus();
	} else {
		if (c == "lastElement") {
			setLastWindowElementFocus();
		} else {
			removeWindowElementFocus(focusedWindowElement_tmp);
			focusedWindowElement = c;
			focusedWindowElement_tmp = focusedWindowElement;
			if (!frameLocked) {
				putWindowElementFocus(focusedWindowElement, b);
			}
		}
	}
}
function drawWindowElementDefaultAction(b) {
	try {
		if (b.tagName == "A") {
			if (b.className.indexOf("ButtonLink_default") == -1
					&& b.className.indexOf("ButtonLink") != -1
					&& b.className.indexOf("ButtonLink_disabled") == -1) {
				b.className = "ButtonLink_default";
			}
		} else {
			if (b.tagName == "BUTTON") {
				if (b.className.indexOf("ButtonLink_default") == -1
						&& b.className.indexOf("ButtonLink") != -1
						&& b.className.indexOf("ButtonLink_disabled") == -1) {
					b.className = "ButtonLink_default";
				}
			}
		}
	} catch (a) {
	}
}
function eraseWindowElementDefaultAction(b) {
	try {
		if (b.tagName == "A") {
			b.className = b.className.replace("ButtonLink_default",
					"ButtonLink");
		} else {
			if (b.tagName == "BUTTON") {
				b.className = b.className.replace("ButtonLink_default",
						"ButtonLink");
			}
		}
	} catch (a) {
	}
}
function drawWindowElementFocus(c) {
	drawnWindowElement = c;
	try {
		if (parent.frames["frameMenu"]) {
			parent.frames["frameMenu"].onBlurMenu();
		}
	} catch (b) {
	}
	try {
		if (c.tagName == "A") {
			if (c.className.indexOf(" Popup_Client_Help_LabelLink_focus") == -1
					&& c.className.indexOf("Popup_Client_Help_LabelLink") != -1) {
				c.className = c.className
						+ " Popup_Client_Help_LabelLink_focus";
			} else {
				if (c.className.indexOf("DataGrid_Popup_text_pagerange_focus") == -1
						&& c.className.indexOf("DataGrid_Popup_text_pagerange") != -1) {
					c.className = c.className
							+ " DataGrid_Popup_text_pagerange_focus";
				} else {
					if (c.className
							.indexOf("Popup_Client_Help_Icon_LabelLink_focus") == -1
							&& c.className
									.indexOf("Popup_Client_Help_Icon_LabelLink") != -1) {
						c.className = "Popup_Client_Help_Icon_LabelLink_focus";
					} else {
						if (c.className
								.indexOf(" Popup_Client_UserOps_LabelLink_Selected_focus") == -1
								&& c.className
										.indexOf("Popup_Client_UserOps_LabelLink_Selected") != -1) {
							c.className = c.className
									+ " Popup_Client_UserOps_LabelLink_Selected_focus";
						} else {
							if (c.className
									.indexOf(" Popup_Client_UserOps_LabelLink_focus") == -1
									&& c.className
											.indexOf("Popup_Client_UserOps_LabelLink") != -1) {
								c.className = c.className
										+ " Popup_Client_UserOps_LabelLink_focus";
							} else {
								if (c.className
										.indexOf(" LabelLink_noicon_focus") == -1
										&& c.className
												.indexOf("LabelLink_noicon") != -1) {
									c.className = c.className
											+ " LabelLink_noicon_focus";
								} else {
									if (c.className.indexOf("LabelLink_focus") == -1
											&& c.className.indexOf("LabelLink") != -1) {
										c.className = c.className
												+ " LabelLink_focus";
									} else {
										if (c.className
												.indexOf("FieldButtonLink_focus") == -1
												&& c.className
														.indexOf("FieldButtonLink") != -1) {
											c.className = "FieldButtonLink_focus";
										} else {
											if (c.className
													.indexOf("ButtonLink_focus") == -1
													&& c.className
															.indexOf("ButtonLink") != -1
													&& c.className
															.indexOf("ButtonLink_disabled") == -1) {
												c.className = "ButtonLink_focus";
											} else {
												if (c.className
														.indexOf("List_Button_TopLink_focus") == -1
														&& c.className
																.indexOf("List_Button_TopLink") != -1) {
													c.className = "List_Button_TopLink_focus";
												} else {
													if (c.className
															.indexOf("List_Button_MiddleLink_focus") == -1
															&& c.className
																	.indexOf("List_Button_MiddleLink") != -1) {
														c.className = "List_Button_MiddleLink_focus";
													} else {
														if (c.className
																.indexOf("List_Button_BottomLink_focus") == -1
																&& c.className
																		.indexOf("List_Button_BottomLink") != -1) {
															c.className = "List_Button_BottomLink_focus";
														} else {
															if (c.className
																	.indexOf("Dimension_LeftRight_Button_TopLink_focus") == -1
																	&& c.className
																			.indexOf("Dimension_LeftRight_Button_TopLink") != -1) {
																c.className = "Dimension_LeftRight_Button_TopLink_focus";
															} else {
																if (c.className
																		.indexOf("Dimension_LeftRight_Button_BottomLink_focus") == -1
																		&& c.className
																				.indexOf("Dimension_LeftRight_Button_BottomLink") != -1) {
																	c.className = "Dimension_LeftRight_Button_BottomLink_focus";
																} else {
																	if (c.className
																			.indexOf("Dimension_UpDown_Button_BottomLink_focus") == -1
																			&& c.className
																					.indexOf("Dimension_UpDown_Button_BottomLink") != -1) {
																		c.className = "Dimension_UpDown_Button_BottomLink_focus";
																	} else {
																		if (c.className
																				.indexOf("Dimension_UpDown_Button_TopLink_focus") == -1
																				&& c.className
																						.indexOf("Dimension_UpDown_Button_TopLink") != -1) {
																			c.className = "Dimension_UpDown_Button_TopLink_focus";
																		} else {
																			if (c.className
																					.indexOf("Popup_Workflow_Button_focus") == -1
																					&& c.className
																							.indexOf("Popup_Workflow_Button") != -1) {
																				c.className = "Popup_Workflow_Button_focus";
																			} else {
																				if (c.className
																						.indexOf("Popup_Workflow_text_focus") == -1
																						&& c.className
																								.indexOf("Popup_Workflow_text") != -1) {
																					c.className = "Popup_Workflow_text_focus";
																				} else {
																					if (c.className
																							.indexOf("MessageBox_TextLink_focus") == -1
																							&& c.className
																									.indexOf("MessageBox_TextLink") != -1) {
																						c.className = "MessageBox_TextLink_focus";
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			isFirstTime = false;
		} else {
			if (c.tagName == "BUTTON") {
				if (c.className.indexOf("ButtonLink_focus") == -1
						&& c.className.indexOf("ButtonLink") != -1
						&& c.className.indexOf("ButtonLink_disabled") == -1) {
					c.className = "ButtonLink_focus";
				}
				isFirstTime = false;
			} else {
				if (c.tagName == "SELECT") {
					if (navigator.appName.toUpperCase().indexOf("MICROSOFT") == -1) {
						if (c.className.indexOf(" Combo_focus") == -1) {
							c.className = c.className.replace("Login_Combo",
									"Login_Combo_focus");
							c.className = c.className + " Combo_focus";
						}
					}
					isFirstTime = false;
				} else {
					if (c.tagName == "INPUT") {
						if ((c.className.indexOf(" TextBox_focus") == -1)
								&& (c.className.indexOf("dojoValidateEmpty") != -1
										|| c.className
												.indexOf("dojoValidateValid") != -1
										|| c.className
												.indexOf("dojoValidateInvalid") != -1 || c.className
										.indexOf("dojoValidateRange") != -1)) {
							c.className = c.className.replace(
									"dojoValidateEmpty",
									"dojoValidateEmpty_focus");
							c.className = c.className.replace(
									"dojoValidateValid",
									"dojoValidateValid_focus");
							c.className = c.className.replace(
									"dojoValidateInvalid",
									"dojoValidateInvalid_focus");
							c.className = c.className.replace(
									"dojoValidateRange",
									"dojoValidateRange_focus");
							c.className = c.className.replace("required",
									"required_focus");
							c.className = c.className.replace("readonly",
									"readonly_focus");
							c.className = c.className.replace("Login_TextBox",
									"Login_TextBox_focus");
							c.className = c.className + " TextBox_focus";
						} else {/*
							if (c.getAttribute("type") == "checkbox") {
								c.className = "Checkbox_Focused";
								var a = c;
								try {
									for (;;) {
										if (a.getAttribute("class") == "Checkbox_container_NOT_Focused") {
											a.className = "Checkbox_container_Focused";
											break;
										} else {
											a = a.parentNode;
										}
									}
								} catch (b) {
								}
								isFirstTime = false;
							} else {
								if (c.getAttribute("type") == "radio") {
									c.className = "Radio_Focused";
									var a = c;
									try {
										for (;;) {
											if (a.getAttribute("class") == "Radio_container_NOT_Focused") {
												a.className = "Radio_container_Focused";
												break;
											} else {
												a = a.parentNode;
											}
										}
									} catch (b) {
									}
								}
							}
						*/}
						isFirstTime = false;
					} else {
						if (c.tagName == "TEXTAREA") {
							if ((c.className.indexOf(" TextBox_focus") == -1)
									&& (c.className
											.indexOf("dojoValidateEmpty") != -1
											|| c.className
													.indexOf("dojoValidateValid") != -1
											|| c.className
													.indexOf("dojoValidateInvalid") != -1 || c.className
											.indexOf("dojoValidateRange") != -1)) {
								c.className = c.className.replace(
										"dojoValidateValid",
										"dojoValidateValid_focus");
								c.className = c.className.replace("required",
										"required_focus");
								c.className = c.className + " TextBox_focus";
							}
							isFirstTime = false;
						} else {
							if (currentWindowElementType == "grid") {
								isFirstTime = false;
							} else {
								if (currentWindowElementType == "genericTree") {
									isFirstTime = false;
								} else {
									if (currentWindowElementType == "custom") {
										isFirstTime = false;
									} else {
									}
								}
							}
						}
					}
				}
			}
		}
	} catch (b) {
	}
}
function putWindowElementFocus(c, a) {
	isContextMenuOpened = false;
	previousWindowElementType = currentWindowElementType;
	drawWindowElementFocus(c);
	defaultActionLogic(c);
	try {
		if (currentWindowElementType == "grid") {
			c.focus();
			focusGrid();
		} else {
			if (currentWindowElementType == "genericTree") {
				c.focus();
				focusGenericTree();
			} else {
				if (currentWindowElementType == "custom") {
					if (c.focusLogic) {
						c.focusLogic("focus");
					}
				} else {
					if (c.tagName.toLowerCase() == "input"
							&& c.type.toLowerCase() == "text"
							&& a == "onmousedown"
							&& navigator.userAgent.toUpperCase()
									.indexOf("MSIE") == -1) {
					} else {
						c.focus();
					}
				}
			}
		}
		if (selectInputTextOnTab && a != "onclick" && a != "onmousedown") {
			c.select();
		}
	} catch (b) {
	}
}
function eraseWindowElementFocus(c) {
	drawnWindowElement = null;
	try {
		if (c.tagName == "A") {
			c.className = c.className.replace(
					" Popup_Client_UserOps_LabelLink_focus", "");
			c.className = c.className.replace(
					" Popup_Client_UserOps_LabelLink_Selected_focus", "");
			c.className = c.className.replace(
					" Popup_Client_Help_LabelLink_focus", "");
			c.className = c.className.replace(
					" DataGrid_Popup_text_pagerange_focus", "");
			c.className = c.className.replace(" LabelLink_focus", "");
			c.className = c.className.replace(" LabelLink_noicon_focus", "");
			c.className = c.className.replace("FieldButtonLink_focus",
					"FieldButtonLink");
			c.className = c.className.replace("ButtonLink_focus", "ButtonLink");
			c.className = c.className.replace("List_Button_TopLink_focus",
					"List_Button_TopLink");
			c.className = c.className.replace("List_Button_MiddleLink_focus",
					"List_Button_MiddleLink");
			c.className = c.className.replace("List_Button_BottomLink_focus",
					"List_Button_BottomLink");
			c.className = c.className.replace(
					"Dimension_LeftRight_Button_TopLink_focus",
					"Dimension_LeftRight_Button_TopLink");
			c.className = c.className.replace(
					"Dimension_LeftRight_Button_BottomLink_focus",
					"Dimension_LeftRight_Button_BottomLink");
			c.className = c.className.replace(
					"Dimension_UpDown_Button_BottomLink_focus",
					"Dimension_UpDown_Button_BottomLink");
			c.className = c.className.replace(
					"Dimension_UpDown_Button_TopLink_focus",
					"Dimension_UpDown_Button_TopLink");
			c.className = c.className.replace("Popup_Workflow_Button_focus",
					"Popup_Workflow_Button");
			c.className = c.className.replace("Popup_Workflow_text_focus",
					"Popup_Workflow_text");
			c.className = c.className.replace(
					"Popup_Client_Help_Icon_LabelLink_focus",
					"Popup_Client_Help_Icon_LabelLink");
			c.className = c.className.replace("MessageBox_TextLink_focus",
					"MessageBox_TextLink");
		} else {
			if (c.tagName == "BUTTON") {
				c.className = c.className.replace("ButtonLink_focus",
						"ButtonLink");
			} else {
				if (c.tagName == "SELECT") {
					c.className = c.className.replace(" Combo_focus", "");
					c.className = c.className.replace("Login_Combo_focus",
							"Login_Combo");
				} else {
					if (c.tagName == "INPUT") {
						c.className = c.className.replace(" TextBox_focus", "");
						c.className = c.className.replace(
								"dojoValidateEmpty_focus", "dojoValidateEmpty");
						c.className = c.className.replace(
								"dojoValidateValid_focus", "dojoValidateValid");
						c.className = c.className.replace(
								"dojoValidateInvalid_focus",
								"dojoValidateInvalid");
						c.className = c.className.replace(
								"dojoValidateRange_focus", "dojoValidateRange");
						c.className = c.className.replace("required_focus",
								"required");
						c.className = c.className.replace("readonly_focus",
								"readonly");
						c.className = c.className.replace(
								"Login_TextBox_focus", "Login_TextBox");
						/*
						if (c.getAttribute("type") == "checkbox") {
							c.className = "Checkbox_NOT_Focused";
							var a = c;
							try {
								for (;;) {
									if (a.getAttribute("class") == "Checkbox_container_Focused") {
										a.className = "Checkbox_container_NOT_Focused";
										break;
									} else {
										a = a.parentNode;
									}
								}
							} catch (b) {
							}
						} else {
							if (c.getAttribute("type") == "radio") {
								c.className = "Radio_NOT_Focused";
								var a = c;
								try {
									for (;;) {
										if (a.getAttribute("class") == "Radio_container_Focused") {
											a.className = "Radio_container_NOT_Focused";
											break;
										} else {
											a = a.parentNode;
										}
									}
								} catch (b) {
								}
							}
						}*/
					} else {
						if (c.tagName == "TEXTAREA") {
							c.className = c.className.replace(" TextBox_focus",
									"");
							c.className = c.className.replace(
									"dojoValidateValid_focus",
									"dojoValidateValid");
							c.className = c.className.replace("required_focus",
									"required");
						} else {
							if (previousWindowElementType == "grid") {
								blurGrid();
							} else {
								if (previousWindowElementType == "genericTree") {
									blurGenericTree();
								} else {
									if (previousWindowElementType == "custom") {
										if (c.focusLogic) {
											c.focusLogic("blur");
										}
									} else {
									}
								}
							}
						}
					}
				}
			}
		}
	} catch (b) {
	}
}
function removeWindowElementFocus(b) {
	isContextMenuOpened = false;
	eraseWindowElementFocus(b);
	try {
		if (previousWindowElementType == "grid") {
			blurGrid();
		} else {
			if (previousWindowElementType == "genericTree") {
				blurGenericTree();
			} else {
			}
		}
	} catch (a) {
	}
}
function mustBeJumped(a) {
	if (a.focusLogic) {
		return a.focusLogic("mustBeJumped");
	}
	if (a.style.display == "none") {
		return true;
	}
	if (a.getAttribute("id") == "genericTree") {
		return true;
	}
	return false;
}
function mustBeIgnored(a) {
	if (a.focusLogic) {
		return a.focusLogic("mustBeIgnored");
	}
	if (a.style.display == "none") {
		return true;
	}
	if (a.getAttribute("type") == "hidden") {
		return true;
	}
	if (a.getAttribute("readonly") == "true"
			&& a.getAttribute("tabindex") != "1") {
		return true;
	}
	if (a.readOnly && a.getAttribute("tabindex") != "1") {
		return true;
	}
	if (a.getAttribute("disabled") == "true") {
		return true;
	}
	if (a.disabled) {
		return true;
	}
	if (a.className.indexOf("LabelLink") != -1
			&& a.className.indexOf("_LabelLink") == -1
			&& a.className.indexOf("LabelLink_") == -1) {
		return true;
	}
	if (a.className.indexOf("FieldButtonLink") != -1) {
		return true;
	}
	if (a.className.indexOf("ButtonLink_disabled") != -1) {
		return true;
	}
	return false;
}
function canHaveFocus(a) {
	if (mustBeIgnored(a)) {
		return false;
	}
	if (couldHaveFocus(a)) {
		return true;
	}
	return false;
}
function couldHaveFocus(b) {
	try {
		if (b.tagName == "INPUT"
				&& b.getAttribute("id") == "grid_table_dummy_input") {
			currentWindowElementType = "grid";
			return true;
		}
	} catch (a) {
	}
	try {
		if (b.tagName == "INPUT"
				&& b.getAttribute("id") == "genericTree_dummy_input") {
			currentWindowElementType = "genericTree";
			return true;
		}
	} catch (a) {
	}
	try {
		if (b.focusLogic) {
			currentWindowElementType = "custom";
			return b.focusLogic("couldHaveFocus");
		}
	} catch (a) {
	}
	if (b.tagName == "INPUT") {
		currentWindowElementType = "input";
		return true;
	}
	if (b.tagName == "A") {
		currentWindowElementType = "a";
		return true;
	}
	if (b.tagName == "BUTTON"
			&& b.className.indexOf("ButtonLink_disabled") == -1) {
		currentWindowElementType = "button";
		return true;
	}
	if (b.tagName == "SELECT") {
		currentWindowElementType = "select";
		return true;
	}
	if (b.tagName == "TEXTAREA") {
		currentWindowElementType = "textarea";
		return true;
	}
	return false;
}
function getNextWindowElement() {
	if (isReadOnlyWindow) {
		try {
			setTimeout("setSelectedArea('window'); swichSelectedArea();", 50);
			return true;
		} catch (c) {
		}
	}
	var d = null;
	var b = null;
	var a = focusedWindowElement;
	if (a == null) {
		a = getFirstWindowElement();
		return a;
	} else {
		for (;;) {
			b = a;
			try {
				a = a.firstChild;
				for (;;) {
					for (;;) {
						if (a.nodeType != "1") {
							a = a.nextSibling;
						} else {
							break;
						}
					}
					if (!mustBeJumped(a)) {
						break;
					} else {
						a = a.nextSibling;
					}
				}
				d = true;
			} catch (c) {
				d = false;
				a = b;
			}
			if (d) {
				if (canHaveFocus(a)) {
					return a;
				}
			} else {
				for (;;) {
					b = a;
					try {
						a = a.nextSibling;
						for (;;) {
							for (;;) {
								if (a.nodeType != "1") {
									a = a.nextSibling;
								} else {
									break;
								}
							}
							if (!mustBeJumped(a)) {
								break;
							} else {
								a = a.nextSibling;
							}
						}
						d = true;
					} catch (c) {
						d = false;
						a = b;
					}
					if (d) {
						if (canHaveFocus(a)) {
							return a;
						}
						break;
					} else {
						a = a.parentNode;
						if (a == document
								.getElementById(windowTableParentElement)
								|| a == document.getElementsByTagName("BODY")[0]) {
							goToNextWindowTable();
							if (!isFirstTime) {
								return getCurrentWindowTableFirstElement();
							} else {
								try {
									isReadOnlyWindow = true;
									setTimeout(
											"setSelectedArea('window'); swichSelectedArea();",
											50);
								} catch (c) {
								}
							}
						}
					}
				}
			}
		}
	}
}
function getPreviousWindowElement() {
	var d = null;
	var a = null;
	var b = focusedWindowElement;
	if (b == null) {
		b = getLastWindowElement();
		return b;
	} else {
		for (;;) {
			a = b;
			try {
				b = b.lastChild;
				for (;;) {
					for (;;) {
						if (b.nodeType != "1") {
							b = b.previousSibling;
						} else {
							break;
						}
					}
					if (!mustBeJumped(b)) {
						break;
					} else {
						b = b.previousSibling;
					}
				}
				d = true;
			} catch (c) {
				d = false;
				b = a;
			}
			if (d) {
				if (canHaveFocus(b)) {
					return b;
				}
			} else {
				for (;;) {
					a = b;
					try {
						b = b.previousSibling;
						for (;;) {
							for (;;) {
								if (b.nodeType != "1") {
									b = b.previousSibling;
								} else {
									break;
								}
							}
							if (!mustBeJumped(b)) {
								break;
							} else {
								b = b.previousSibling;
							}
						}
						d = true;
					} catch (c) {
						d = false;
						b = a;
					}
					if (d) {
						if (canHaveFocus(b)) {
							return b;
						}
						break;
					} else {
						b = b.parentNode;
						if (b == document
								.getElementById(windowTableParentElement)) {
							goToPreviousWindowTable();
							return getCurrentWindowTableLastElement();
						}
					}
				}
			}
		}
	}
}
function goToNextWindowTable() {
	for (;;) {
		if (focusedWindowTable < windowTables.length - 1) {
			focusedWindowTable = focusedWindowTable + 1;
		} else {
			focusedWindowTable = 0;
		}
		if (document.getElementById(windowTables[focusedWindowTable].tableId).style.display != "none") {
			break;
		}
	}
	setWindowTableParentElement();
}
function goToPreviousWindowTable() {
	for (;;) {
		if (focusedWindowTable > 0) {
			focusedWindowTable = focusedWindowTable - 1;
		} else {
			focusedWindowTable = windowTables.length - 1;
		}
		if (document.getElementById(windowTables[focusedWindowTable].tableId).style.display != "none") {
			break;
		}
	}
	setWindowTableParentElement();
}
function getCurrentWindowTableFirstElement() {
	focusedWindowElement = document
			.getElementById(windowTables[focusedWindowTable].tableId);
	var a = getNextWindowElement();
	return a;
}
function getCurrentWindowTableLastElement() {
	focusedWindowElement = document
			.getElementById(windowTables[focusedWindowTable].tableId);
	var a = getPreviousWindowElement();
	return a;
}
function getFirstWindowElement() {
	focusedWindowElement = document.getElementById(windowTables[0].tableId);
	focusedWindowTable = 0;
	try {
		var b = getNextWindowElement();
		return b;
	} catch (a) {
		b = null;
		focusedWindowElement = document.getElementsByTagName("BODY")[0];
	}
}
function getLastWindowElement() {
	focusedWindowElement = document
			.getElementById(windowTables[windowTables.length - 1].tableId);
	focusedWindowTable = windowTables.length - 1;
	var a = getPreviousWindowElement();
	return a;
}
function setOBTabBehavior(a) {
	if (a == true) {
		isOBTabBehavior = true;
	} else {
		if (a == false) {
			isOBTabBehavior = false;
		}
	}
	return true;
}
function windowTabKey(a) {
	if (isOBTabBehavior) {
		if (a == true) {
			if (isTabBlocked == false) {
				isTabPressed = true;
				isSelectedComboOpened = false;
				if (selectedArea == "window") {
					var b = getNextWindowElement();
					setWindowElementFocus(b);
				} else {
					if (selectedArea == "tabs") {
						var b = getNextTab();
						setTabFocus(b);
					}
				}
			}
		} else {
			isTabPressed = false;
		}
		return false;
	}
	return true;
}
function windowShiftTabKey(a) {
	if (isOBTabBehavior) {
		if (a == true) {
			if (isTabBlocked == false) {
				isTabPressed = true;
				isSelectedComboOpened = false;
				if (selectedArea == "window") {
					var b = getPreviousWindowElement();
					setWindowElementFocus(b);
				} else {
					if (selectedArea == "tabs") {
						var b = getPreviousTab();
						setTabFocus(b);
					}
				}
			}
		} else {
			isTabPressed = false;
		}
		return false;
	}
	return true;
}
function windowEnterKey() {
	if (isGridFocused) {
		propagateEnter = false;
		onRowDblClick();
	} else {
		if (isGenericTreeFocused) {
			propagateEnter = false;
			gt_showNodeDescription(gt_focusedNode);
		} else {
			if (defaultActionType == "button") {
				propagateEnter = false;
				executeWindowButton(defaultActionElement.getAttribute("id"));
			} else {
				if (defaultActionType == "combo") {
					propagateEnter = true;
					isSelectedComboOpened = false;
					activateDefaultAction();
				} else {
					propagateEnter = true;
					return true;
				}
			}
		}
	}
}
function windowCtrlShiftEnterKey() {
	executeAssociatedLink();
}
function windowCtrlEnterKey() {
	if (isGenericTreeFocused) {
		gt_goToNodeLink(gt_focusedNode);
	} else {
		executeAssociatedFieldButton();
	}
}
function setFirstWindowElementFocus() {
	var a = getFirstWindowElement();
	setWindowElementFocus(a);
}
function setLastWindowElementFocus() {
	var a = getLastWindowElement();
	setWindowElementFocus(a);
}
function setCurrentWindowTableFirstElementFocus() {
	var a = getCurrentWindowTableFirstElement();
	setWindowElementFocus(a);
}
function setCurrentWindowTableLastElementFocus() {
	var a = getCurrentWindowTableLastElement();
	setWindowElementFocus(a);
}
function getAssociatedLink() {
	var a = null;
	var c = true;
	a = focusedWindowElement;
	if (a.className.indexOf("TextBox_btn") != -1) {
		try {
			for (;;) {
				a = a.parentNode;
				if (a.tagName == "TABLE") {
					break;
				} else {
					if (a.tagName == "BODY") {
						c = false;
						break;
					}
				}
			}
			for (;;) {
				a = a.parentNode;
				if (a.tagName == "TABLE") {
					break;
				} else {
					if (a.tagName == "BODY") {
						c = false;
						break;
					}
				}
			}
			c = true;
		} catch (b) {
			c = false;
		}
	}
	if (c == true) {
		try {
			for (;;) {
				a = a.parentNode;
				if (a.tagName == "TD") {
					c = true;
					break;
				} else {
					if (a.tagName == "BODY") {
						c = false;
						break;
					}
				}
			}
		} catch (b) {
			c = false;
		}
	}
	if (c == true) {
		try {
			a = a.previousSibling;
			for (;;) {
				if (a.nodeType != "1") {
					a = a.previousSibling;
				} else {
					break;
				}
			}
			a = a.firstChild;
			for (;;) {
				if (a.nodeType != "1") {
					a = a.nextSibling;
				} else {
					break;
				}
			}
			a = a.firstChild;
			for (;;) {
				if (a.nodeType != "1") {
					a = a.nextSibling;
				} else {
					break;
				}
			}
			c = true;
		} catch (b) {
			c = false;
		}
	}
	if (c == true && a.tagName == "A") {
		return a;
	} else {
		return false;
	}
}
function executeAssociatedLink() {
	var a = null;
	a = getAssociatedLink();
	if (a != null && a != false) {
		a.onclick();
		return true;
	} else {
		return false;
	}
}
function getAssociatedFieldButton(a, c) {
	var b = null;
	var f = true;
	if (typeof a == "undefined" || a == null) {
		b = focusedWindowElement;
	} else {
		b = a;
	}
	if (c == "window") {
		try {
			for (;;) {
				b = b.parentNode;
				if (b.tagName == "TD"
						&& b.className.indexOf("TextBox_ContentCell") != -1) {
					break;
				} else {
					if (b.tagName == "BODY") {
						f = false;
						break;
					}
				}
			}
			f = true;
		} catch (d) {
			f = false;
		}
	}
	if (f == true) {
		try {
			b = b.nextSibling;
			for (;;) {
				if (b.nodeType != "1") {
					b = b.nextSibling;
				} else {
					break;
				}
			}
			b = b.firstChild;
			for (;;) {
				if (b.nodeType != "1") {
					b = b.nextSibling;
				} else {
					break;
				}
			}
			f = true;
		} catch (d) {
			f = false;
		}
	}
	if (f == true && b.tagName == "A") {
		return b;
	} else {
		return false;
	}
}
function executeAssociatedFieldButton() {
	var a = null;
	a = getAssociatedFieldButton(null, "window");
	if (a != null && a != false) {
		a.onclick();
		return true;
	}
}
var focusedTab = null;
var focusedTab_tmp = null;
var tabTableParentElement = null;
var focusedTabTable = 0;
function tabTableId(a) {
	this.tabTableId = a;
}
function isTabActive(a) {
	if (a.className.indexOf("Tabcurrent") != -1) {
		return true;
	} else {
		return false;
	}
}
function drawTabFocus(c) {
	var a = null;
	try {
		if (c.tagName == "A") {
			if (c.className.indexOf("dojoTabLink_focus") == -1
					&& c.className.indexOf("dojoTabLink") != -1) {
				a = c;
				c = c.parentNode;
				c = c.parentNode;
				c = c.parentNode;
				if (c.className.indexOf("dojoTabparentfirst") != -1) {
					c.className = "dojoTabparentfirst_focus";
				} else {
					if (c.className.indexOf("dojoTabparent") != -1) {
						c.className = "dojoTabparent_focus";
					} else {
						if (c.className.indexOf("dojoTabcurrentfirst") != -1) {
							c.className = "dojoTabcurrentfirst_focus";
						} else {
							if (c.className.indexOf("dojoTabcurrent") != -1) {
								c.className = "dojoTabcurrent_focus";
							} else {
								c = a;
								c.className = "dojoTabLink_focus";
							}
						}
					}
				}
			}
		} else {
		}
	} catch (b) {
	}
}
function putTabFocus(a) {
	drawTabFocus(a);
	defaultActionLogic(a);
	a.focus();
}
function eraseTabFocus(c) {
	var a = null;
	try {
		if (c.tagName == "A") {
			a = c;
			c = c.parentNode;
			c = c.parentNode;
			c = c.parentNode;
			if (c.className.indexOf("dojoTabparentfirst_focus") != -1) {
				c.className = "dojoTabparentfirst";
			} else {
				if (c.className.indexOf("dojoTabparent_focus") != -1) {
					c.className = "dojoTabparent";
				} else {
					if (c.className.indexOf("dojoTabcurrentfirst_focus") != -1) {
						c.className = "dojoTabcurrentfirst";
					} else {
						if (c.className.indexOf("dojoTabcurrent_focus") != -1) {
							c.className = "dojoTabcurrent";
						} else {
							c = a;
							if (c.className.indexOf("dojoTabLink_focus") != -1) {
								c.className = "dojoTabLink";
							}
						}
					}
				}
			}
		}
	} catch (b) {
	}
}
function removeTabFocus(a) {
	eraseTabFocus(a);
}
function getFirstTab() {
	focusedTab = document.getElementById(tabsTables[0].tabTableId);
	focusedTabTable = 0;
	var a = getNextTab();
	return a;
}
function getLastTab() {
	focusedTab = document
			.getElementById(tabsTables[tabsTables.length - 1].tabTableId);
	focusedTabTable = tabsTables.length - 1;
	var a = getPreviousTab();
	return a;
}
function getNextTab() {
	var d = null;
	var b = null;
	var a = focusedTab;
	if (a == null) {
		a = getActiveTab();
		return a;
	} else {
		for (;;) {
			b = a;
			try {
				a = a.firstChild;
				for (;;) {
					for (;;) {
						if (a.nodeType != "1") {
							a = a.nextSibling;
						} else {
							break;
						}
					}
					if (!mustBeJumped(a)) {
						break;
					} else {
						a = a.nextSibling;
					}
				}
				d = true;
			} catch (c) {
				d = false;
				a = b;
			}
			if (d) {
				if (canHaveFocus(a)) {
					return a;
				}
			} else {
				for (;;) {
					b = a;
					try {
						a = a.nextSibling;
						for (;;) {
							for (;;) {
								if (a.nodeType != "1") {
									a = a.nextSibling;
								} else {
									break;
								}
							}
							if (!mustBeJumped(a)) {
								break;
							} else {
								a = a.nextSibling;
							}
						}
						d = true;
					} catch (c) {
						d = false;
						a = b;
					}
					if (d) {
						if (canHaveFocus(a)) {
							return a;
						}
						break;
					} else {
						a = a.parentNode;
						if (a == document.getElementById(tabTableParentElement)
								|| a == document.getElementsByTagName("BODY")[0]) {
							goToNextTabs();
							return getFirstTab();
						}
					}
				}
			}
		}
	}
}
function getPreviousTab() {
	var d = null;
	var a = null;
	var b = focusedTab;
	if (b == null) {
		b = getActiveTab();
		return b;
	} else {
		for (;;) {
			a = b;
			try {
				b = b.lastChild;
				for (;;) {
					for (;;) {
						if (b.nodeType != "1") {
							b = b.previousSibling;
						} else {
							break;
						}
					}
					if (!mustBeJumped(b)) {
						break;
					} else {
						b = b.previousSibling;
					}
				}
				d = true;
			} catch (c) {
				d = false;
				b = a;
			}
			if (d) {
				if (canHaveFocus(b)) {
					return b;
				}
			} else {
				for (;;) {
					a = b;
					try {
						b = b.previousSibling;
						for (;;) {
							for (;;) {
								if (b.nodeType != "1") {
									b = b.previousSibling;
								} else {
									break;
								}
							}
							if (!mustBeJumped(b)) {
								break;
							} else {
								b = b.previousSibling;
							}
						}
						d = true;
					} catch (c) {
						d = false;
						b = a;
					}
					if (d) {
						if (canHaveFocus(b)) {
							return b;
						}
						break;
					} else {
						b = b.parentNode;
						if (b == document.getElementById(tabTableParentElement)) {
							goToPreviousTabs();
							return getLastTab();
						}
					}
				}
			}
		}
	}
}
function setTabFocus(b, a) {
	if (a == null || a == "null" || a == "" || a == "obj") {
		if (b == "firstElement") {
			setFirstTabFocus();
		} else {
			if (b == "lastElement") {
				setLastTabFocus();
			} else {
				focusedTab = b;
				removeTabFocus(focusedTab_tmp);
				focusedTab_tmp = focusedTab;
				if (!frameLocked) {
					putTabFocus(focusedTab);
				}
			}
		}
	} else {
		if (a == "id") {
			b = document.getElementById(b);
			focusedTab = b;
			removeTabFocus(focusedTab_tmp);
			focusedTab_tmp = focusedTab;
			if (!frameLocked) {
				putWindowElementFocus(b);
			}
			putTabFocus(focusedTab);
		}
	}
}
function setFirstTabFocus() {
	var a = getFirstTab();
	setTabFocus(a);
}
function setLastTabFocus() {
	var a = getLastTab();
	setTabFocus(a);
}
function goToNextTabs() {
	if (focusedTabTable < tabsTables.length - 1) {
		focusedTabTable = focusedTabTable + 1;
	} else {
		focusedTabTable = 0;
	}
	setTabTableParentElement();
}
function goToPreviousTabs() {
	if (focusedTabTable > 0) {
		focusedTabTable = focusedTabTable - 1;
	} else {
		focusedTabTable = tabsTables.length - 1;
	}
	setTabTableParentElement();
}
function setActiveTab() {
	var a = getActiveTab();
	setTabFocus(a);
}
function setTabTableParentElement() {
	tabTableParentElement = tabsTables[focusedTabTable].tabTableId;
}
function getActiveTab() {
	var b = getActiveTabContainer();
	var a = focusedTab;
	focusedTab = b;
	b = getNextTab();
	focusedTab = a;
	return b;
}
function getActiveTabContainer() {
	var d = null;
	var b = null;
	var a = document.getElementById(tabsTables[0].tabTableId);
	if (a == null) {
		return false;
	} else {
		for (;;) {
			b = a;
			try {
				a = a.firstChild;
				for (;;) {
					for (;;) {
						if (a.nodeType != "1") {
							a = a.nextSibling;
						} else {
							break;
						}
					}
					if (!mustBeJumped(a)) {
						break;
					} else {
						a = a.nextSibling;
					}
				}
				d = true;
			} catch (c) {
				d = false;
				a = b;
			}
			if (d) {
				if (isTabActive(a)) {
					return a;
				}
			} else {
				for (;;) {
					b = a;
					try {
						a = a.nextSibling;
						for (;;) {
							for (;;) {
								if (a.nodeType != "1") {
									a = a.nextSibling;
								} else {
									break;
								}
							}
							if (!mustBeJumped(a)) {
								break;
							} else {
								a = a.nextSibling;
							}
						}
						d = true;
					} catch (c) {
						d = false;
						a = b;
					}
					if (d) {
						if (isTabActive(a)) {
							return a;
						}
						break;
					} else {
						a = a.parentNode;
						if (a == document
								.getElementById(windowTableParentElement)
								|| a == document.getElementsByTagName("BODY")[0]) {
							goToNextTabs();
							return false;
						}
					}
				}
			}
		}
	}
}
function focusGrid() {
	try {
		dijit.byId("grid").focusGrid();
	} catch (a) {
	}
}
function blurGrid() {
	try {
		dijit.byId("grid").blurGrid();
	} catch (a) {
	}
}
function focusGenericTree() {
	try {
		gt_focusTreeContainer("genericTree", "id");
	} catch (a) {
	}
}
function blurGenericTree() {
	try {
		if (typeof gt_focusedNode != "undefined") {
			gt_blurTreeContainer("genericTree", "id");
		}
	} catch (a) {
	}
}
function windowUpKey() {
	if (isGridFocused) {
		dijit.byId("grid").goToPreviousRow();
	} else {
		if (isGenericTreeFocused) {
			gt_goToPreviousNode();
		}
	}
}
function windowDownKey() {
	if (isGridFocused) {
		dijit.byId("grid").goToNextRow();
	} else {
		if (isGenericTreeFocused) {
			gt_goToNextNode();
		}
	}
}
function windowLeftKey() {
	if (isGridFocused) {
	} else {
		if (isGenericTreeFocused) {
			if (gt_isClosedNode(gt_focusedNode)) {
				gt_goToParentNode();
			} else {
				gt_closeNode(gt_focusedNode);
			}
		}
	}
}
function windowRightKey() {
	if (isGridFocused) {
	} else {
		if (isGenericTreeFocused) {
			if (gt_isClosedNode(gt_focusedNode)) {
				gt_openNode(gt_focusedNode);
			} else {
				gt_goToNextNode();
			}
		}
	}
}
function windowSpaceKey() {
	if (isGridFocused) {
	} else {
		if (isGenericTreeFocused) {
			gt_checkToggleNode(gt_focusedNode);
		}
	}
}
function windowHomeKey() {
	if (isGridFocused) {
		dijit.byId("grid").goToFirstRow();
	}
}
function windowEndKey() {
	if (isGridFocused) {
		dijit.byId("grid").goToLastRow();
	}
}
function windowAvpageKey() {
	if (isGridFocused) {
		dijit.byId("grid").goToNextPage();
	}
}
function windowRepageKey() {
	if (isGridFocused) {
		dijit.byId("grid").goToPreviousPage();
	}
}