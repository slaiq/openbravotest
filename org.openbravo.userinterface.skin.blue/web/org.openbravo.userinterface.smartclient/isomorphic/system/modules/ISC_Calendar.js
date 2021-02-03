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

if (window.isc && window.isc.module_Core && !window.isc.module_Calendar) {
  isc.module_Calendar = 1;
  isc._moduleStart = isc._Calendar_start = (isc.timestamp ? isc.timestamp() : new Date().getTime());
  if (isc._moduleEnd && (!isc.Log || (isc.Log && isc.Log.logIsDebugEnabled('loadTime')))) {
    isc._pTM = {
      message: 'Calendar load/parse time: ' + (isc._moduleStart - isc._moduleEnd) + 'ms',
      category: 'loadTime'
    };
    if (isc.Log && isc.Log.logDebug) isc.Log.logDebug(isc._pTM.message, 'loadTime');
    else if (isc._preLog) isc._preLog[isc._preLog.length] = isc._pTM;
    else isc._preLog = [isc._pTM]
  }
  isc.definingFramework = true;
  if (window.isc && isc.version != "v10.0d_2014-02-13/LGPL Deployment") {
    isc.logWarn("SmartClient module version mismatch detected: This application is loading the core module from SmartClient version '" + isc.version + "' and additional modules from 'v10.0d_2014-02-13/LGPL Deployment'. Mixing resources from different SmartClient packages is not supported and may lead to unpredictable behavior. If you are deploying resources from a single package you may need to clear your browser cache, or restart your browser." + (isc.Browser.isSGWT ? " SmartGWT developers may also need to clear the gwt-unitCache and run a GWT Compile." : ""))
  }
  isc.ClassFactory.defineClass("CalendarView", "ListGrid");
  isc.A = isc.CalendarView.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.hiliteRowOnFocus = false;
  isc.A.useEventCanvasPool = true;
  isc.A.eventCanvasPoolingMode = "data";
  isc.A.hoverDelay = 0;
  isc.A.eventDragTargetDefaults = {
    _constructor: "Canvas",
    border: "1px dashed red",
    width: 1,
    height: 1,
    snapToGrid: false,
    autoDraw: false,
    moveWithMouse: false,
    dragAppearance: "target",
    dragTarget: this,
    visibility: "hidden",
    keepInParentRect: true,
    hoverMoveWithMouse: true,
    showHover: true,
    hoverDelay: 0,
    hoverProps: {
      overflow: "visible",
      hoverMoveWithMouse: this.hoverMoveWIthMouse
    },
    getHoverHTML: function () {
      var _1 = this.eventCanvas,
          _2 = _1.event,
          _3 = _1.$1245;
      var _4 = "<div style='" + (_2.styleName || "testStyle1") + "'>" + _3.$131e.toShortDatetime() + "</div><div style='" + (_2.styleName || "testStyle2") + "'>" + _3.$131f.toShortDatetime() + "</div>";
      return _4
    },
    setView: function (_1) {
      this.view = _1
    },
    getEventPadding: function () {
      var _1 = this.eventCanvas.calendar;
      return _1.useDragPadding ? _1.getLanePadding(this.view) : 0
    },
    fillOverlapSlots: true,
    positionToEventCanvas: function (_1) {
      var _2 = this.eventCanvas,
          _3 = _2.calendar,
          _4 = this.view,
          _5 = _4.getEventLeft(_2.event) + this.getEventPadding(),
          _6 = _2.getTop(),
          _7 = _2.getVisibleWidth(),
          _8 = _2.getVisibleHeight(),
          _9 = _2.$1245;
      if (this.fillOverlapSlots) {
        if (_4.isTimelineView()) {
          var _10 = _4.getEventRow(_6);
          _6 = _4.getRowTop(_10);
          if (!_9.$1297) {
            _8 = _4.getLaneHeight(_10)
          } else {
            _6 += _9.$1298.top;
            _8 = _9.$1298.height
          }
        } else {
          var _11 = _4.body.getEventColumn(_5);
          _5 = _4.body.getColumnLeft(_11);
          if (_9.useLanes) {
            if (!_9.$1297) {
              _7 = _4.getLaneWidth(_11)
            } else {
              _5 += _9.$1298.left;
              _7 = _9.$1298.width
            }
          } else {
            _7 = _4.body.getColumnWidth(_11)
          }
        }
      }
      if (this.$1244) {
        if (_4.isTimelineView()) {
          _6 = _4.body.getRowTop(_2.$1245.$8l)
        } else {
          _5 = _4.body.getColumnLeft(_2.$1245.$644)
        }
      }
      if (_5 < 0) _5 = 0;
      this.moveTo(_5, _6);
      this.resizeTo(_7, _8);
      if (_1) {
        if (!this.isDrawn()) this.draw();
        this.show();
        this.bringToFront()
      }
      if (_3.showDragHovers) isc.Hover.show(this.getHoverHTML(), this.hoverProps)
    },
    moveToEvent: function () {},
    dragRepositionStart: function () {
      var _1 = this.eventCanvas,
          _2 = _1.event,
          _3 = _1.calendar,
          _4 = this.view,
          _5 = _4.body;
      if (!_3.canDragEvent(_2)) return false;
      this.$1246 = true;
      var _6 = _5.getEventRow(),
          _7 = _5.getRowTop(_6),
          _8 = _5.getRowHeight(_6),
          _9 = _4.getEventLeft(_2) + 1,
          _10 = _5.getEventColumn(_9),
          _11 = _5.getColumnLeft(_10),
          _12 = _5.getColumnWidth(_10),
          _13 = _5.getOffsetX() - _1.getLeft(),
          _14 = _5.getOffsetY() - _1.getTop();
      var _15 = _4.isTimelineView();
      var _16 = _1.$1245 = {};
      _16.$126l = !_15;
      _16.$8l = _6;
      _16.$644 = _10;
      _16.$1249 = _8;
      _16.$1248 = _12;
      _16.$117y = _15 ? _4.$732(_2) : _16.$1248;
      _16.$126o = _15 ? _16.$1249 : _1.getVisibleHeight();
      _16.$936 = _6;
      _16.$937 = _10;
      _16.$116y = _13;
      _16.$1247 = _14;
      _16.$126p = Math.round(_16.$126o / _16.$1249);
      _16.$126q = _4.data.getLength() - _16.$126p;
      _16.$126r = _4.getRowTop(_16.$126q);
      _16.$126s = _15 ? _5.getScrollWidth() - _16.$117y : _5.getColumnLeft(_5.fields.length - 1);
      _16.$126t = _15 ? _5.getEventColumn(_16.$126s) : _5.fields.length - 1;
      _16.$131e = _3.getEventStartDate(_2);
      _16.$131f = _3.getEventEndDate(_2);
      if (_4.hasLanes()) {
        var _17 = _4.getLane(_2[_3.laneNameField]),
            _18 = !_17 || !_17.sublanes ? null : _17.sublanes.find(_3.laneNameField, _2[_3.sublaneNameField]);
        _16.$1299 = true;
        _16.$130a = _17;
        _16.$130b = _17;
        _16.$1297 = _3.useSublanes && _17 && _17.sublanes && _17.sublanes.length > 0;
        _16.$130c = _18;
        _16.$1298 = _18;
        _16.$130d = !_3.canEditEventLane(_2, _4);
        _16.$130e = !_3.canEditEventSublane(_2, _4)
      }
      this.positionToEventCanvas(true);
      return isc.EH.STOP_BUBBLING
    },
    dragRepositionMove: function () {
      var _1 = this.eventCanvas,
          _2 = _1.$1245,
          _3 = _1.event,
          _4 = _1.calendar,
          _5 = this.view,
          _6 = _5.isTimelineView(),
          _7 = _5.body,
          _8 = this.getEventPadding(),
          _9 = -1,
          _10 = -1,
          _11 = -1,
          _12 = -1;
      if (_2.$1299) {
        var _13 = _5.getLaneFromPoint(),
            _14 = _2.$1297 ? _5.getSublaneFromPoint() : null;
        if (!_13 || _5.isGroupNode(_13)) {
          _13 = _2.$130b;
          _14 = _2.$1298
        } else {
          if (_2.$130d) {
            _13 = _2.$130a;
            if (_2.$1297 && (_2.$130e || !_13.sublanes.contains(_14))) {
              _14 = _2.$130c
            }
          } else {
            if (_2.$1297) {
              if (_2.$130e) {
                var _15 = _13.sublanes ? _13.sublanes.find(_4.laneNameField, _2.$130c.name) : null;
                if (_15) {
                  _14 = _15
                } else {
                  _13 = _2.$130b;
                  _14 = _2.$1298
                }
              } else {
                if (_13 != _2.$130b) {
                  if (!_13.sublanes) {
                    _13 = _2.$130b;
                    _14 = _2.$1298
                  }
                }
              }
            }
          }
        }
        if (_6) {
          var _16 = _5.getRecordIndex(_13);
          _9 = _5.getRowTop(_16);
          if (_14) _9 += _14.top;
          _12 = (_14 ? _14.height : _13.height);
          _2.$936 = _16
        } else {
          var _16 = _5.getLaneIndex(_13[_4.laneNameField]);
          _10 = _5.body.getColumnLeft(_16);
          _11 = _5.getLaneWidth(_13[_4.laneNameField]);
          if (_14) {
            _10 += _14.left;
            _11 = _14.width
          }
          _2.$937 = _16
        }
      }
      var _17 = _7.getEventRow(),
          _18 = Math.min(_2.$126q, (_17 < 0 ? 0 : _17)),
          _19 = _7.getRowTop(_18),
          _20 = _7.getOffsetY(),
          _21 = (Math.floor((_20 - _19) / _4.eventSnapGap) * _4.eventSnapGap),
          _22 = _6 ? _19 : Math.min(_2.$126r, _19 + _21),
          _23 = this.getVisibleHeight(),
          _24 = _23;
      var _25 = Math.min(_2.$126t, _7.getEventColumn()),
          _26 = _7.getColumnLeft(_25),
          _27 = (_7.getOffsetX() - _2.$116y),
          _28 = Math.max(0, _27 - ((_27 - _26) % _4.eventSnapGap) + 1),
          _29 = _5.getDateFromPoint(_28, _22, null, true),
          _30 = Math.min(_2.$126s, (_6 ? _4.getDateLeftOffset(_29, _5) : _26)),
          _31 = _30 + (_6 ? (_2.$117y) : _1.getVisibleWidth());
      var _32 = _7.getEventColumn(_31 - 1);
      if (_32 < 0) {
        this.moveTo(_2.$117z, _22);
        return isc.EH.STOP_BUBBLING
      }
      if (!_6) {
        if (_18 != _2.$936) {
          if (_18 < 0) {
            _18 = 0;
            _22 = 0
          } else {
            var _33 = _19 + _2.$126o;
            var _34 = _7.getEventRow(_19 + _2.$126o - _2.$1249);
            if (_34 < 0) {
              _18 = _2.$936;
              _22 = _7.getRowTop(_18)
            } else {
              _2.$936 = _18
            }
          }
        }
      }
      var _35 = _5.isTimelineView() ? (_9 >= 0 && _12 >= 0) : (_2.$1299 ? (_10 >= 0 && _11 >= 0) : false)
      if (!_35) {
        _2.$936 = _18
      }
      if (_25 != _2.$937) {
        if (_5.isDayView() || _5.isWeekView()) {
          if (_5.isDayView() && _4.showDayLanes && !_4.canEditEventLane(_3, _5)) {
            _25 = _2.$937;
            _30 = _2.$117z
          } else {
            if (_25 == -1) _2.$937 = 0;
            else if (_25 == -2) _2.$937 = _2.$937;
            else _2.$937 = _25;
            _30 = _7.getColumnLeft(_2.$937)
          }
        } else {
          _2.$937 = Math.max(1, _25)
        }
      }
      var _36 = Math.max(0, (_9 >= 0 ? _9 : _22)),
          _33 = Math.min(_5.body.getScrollHeight(), _36 + _2.$126o),
          _37 = _5.getDateFromPoint(_30 + 1, _36),
          _38 = _5.getDateFromPoint(_31, _33);
      if (_5.isDayView() || _5.isWeekView()) {
        if (_37.getDate() != _38.getDate()) {
          _38 = isc.DateUtil.getEndOf(_37, "d")
        }
      }
      var _39 = true;
      var _40 = _38.duplicate();
      _40.setTime(_38.getTime() - 1);
      if (_4.shouldDisableDate(_37, _5) || _4.shouldDisableDate(_40, _5)) {
        _39 = false
      }
      if (_39) {
        var _41 = _4.createEventObject(_3, _37, _38, _13 && _13[_4.laneNameField], _14 && _14[_4.laneNameField]);
        _39 = _4.eventRepositionMove(_3, _41, this)
      }
      if (_39) {
        if (_35) {
          if (_6) {
            _36 = _9;
            _2.$125b = _12;
            this.resizeTo(_2.$117y, _12)
          } else {
            _30 = _10;
            _2.$131b = _11;
            this.resizeTo(_11, null)
          }
          _2.$1298 = _14;
          _2.$130b = _13
        } else {
          if (_36 + _24 > _5.body.getScrollHeight() - 1) {
            _24 = _5.body.getScrollHeight() - 1 - _36
          }
          _2.$125b = _24;
          this.resizeTo(null, _24)
        }
        _2.$125a = _36;
        _2.$117z = _30;
        _2.$131e = _37;
        _2.$131f = _38
      }
      this.moveTo(_2.$117z, _2.$125a);
      if (_4.showDragHovers) isc.Hover.show(this.getHoverHTML(), this.hoverProps);
      return isc.EH.STOP_BUBBLING
    },
    dragRepositionStop: function () {
      var _1 = this.eventCanvas,
          _2 = _1.$1245,
          _3 = _1.calendar,
          _4 = this.view,
          _5 = _4.body,
          _6 = _1.event;
      if (_3.showDragHovers) isc.Hover.hide();
      this.hide();
      var _7 = _3.canEditEventLane(_6, _4),
          _8 = _3.canEditEventSublane(_6, _4),
          _9, _10;
      if (_4.isTimelineView()) {
        if (_7 || _8) {
          if (_7) _9 = _2.$130b[_3.laneNameField];
          if (_8 && _3.useSublanes && _2.$1298) {
            _10 = _2.$1298[_3.laneNameField]
          }
        }
      } else if (_4.isDayView() && _3.showDayLanes) {
        if (_7 || _8) {
          if (_7) _9 = _2.$130b[_3.laneNameField];
          if (_8 && _3.useSublanes && _2.$1298) {
            _10 = _2.$1298[_3.laneNameField]
          }
        } else return false
      }
      var _11 = [_2.$131e.duplicate(), _2.$131f.duplicate()];
      var _12 = _11[0].getTime() - _3.getEventStartDate(_6).getTime(),
          _13 = Math.floor(_12 / (1000 * 60)),
          _14 = {};
      if (_4.isTimelineView()) {
        if (_6[_3.leadingDateField] && _6[_3.trailingDateField]) {
          _11.add(_6[_3.leadingDateField].duplicate());
          _11[2].setMinutes(_11[2].getMinutes() + _13);
          _11.add(_6[_3.trailingDateField].duplicate());
          _11[3].setMinutes(_11[3].getMinutes() + _13);
          _14[_3.leadingDateField] = _11[2];
          _14[_3.trailingDateField] = _11[3]
        }
      }
      if (_9 == null) _9 = _6[_3.laneNameField];
      if (_3.adjustEventTimes) {
        var _15 = _3.adjustEventTimes(_6, _1, _11[0], _11[1], _9);
        if (_15) {
          _11[0] = _15[0].duplicate();
          _11[1] = _15[1].duplicate()
        }
      }
      if (_3.allowEventOverlap == false) {
        var _16 = _3.checkForOverlap(_4, _1, _6, _11[0], _11[1], _9);
        if (_16 == true) {
          if (_3.timelineEventOverlap) {
            _3.timelineEventOverlap(false, _6, _1, _11[0], _11[1], _9)
          }
          return false
        } else if (isc.isAn.Array(_16)) {
          _11[0] = _16[0].duplicate();
          _11[1] = _16[1].duplicate();
          if (_3.timelineEventOverlap) {
            _3.timelineEventOverlap(true, _6, _1, _11[0], _11[1], _9)
          }
        }
      }
      if (_11[0] != _2.$131e) _2.$131e = _11[0];
      if (_11[1] != _2.$131f) _2.$131f = _11[1];
      var _17 = _3.createEventObject(_6, _2.$131e, _2.$131f, _2.$130b && _2.$130b[_3.laneNameField], _2.$1298 && _2.$1298[_3.laneNameField]);
      var _18 = _3.eventRepositionStop(_6, _17, _14, this);
      this.$1246 = false;
      if (_18 != false) {
        if (_4.isTimelineView()) {
          if (_3.timelineEventMoved(_6, _2.$131e, _2.$131f, _9) == false) return false
        } else {
          if (_3.eventMoved(_2.$131e, _6, _9) == false) return false
        }
        _3.updateCalendarEvent(_6, _17)
      }
      delete _1.$1245;
      return isc.EH.STOP_BUBBLING
    },
    dragResizeStart: function () {
      var _1 = this.eventCanvas,
          _2 = _1.event,
          _3 = _1.calendar,
          _4 = this.view,
          _5 = _4.body;
      if (!_3.canResizeEvent(_1.event)) return false;
      this.$1244 = true;
      var _6 = _5.getEventRow(),
          _7 = _5.getRowTop(_6),
          _8 = _5.getRowHeight(_6),
          _9 = _5.getEventColumn(),
          _10 = _5.getColumnLeft(_9),
          _11 = _5.getColumnWidth(_9),
          _12 = _5.getOffsetX() - _1.getLeft(),
          _13 = _5.getOffsetY() - _1.getTop(),
          _14 = _1.getVisibleWidth(),
          _15 = _4.hasLanes(),
          _16 = _4.isTimelineView(),
          _17 = _16 && (_12 < _14 / 2),
          _18 = _15 ? _4.getLaneFromPoint() : null,
          _19 = _18 && _3.useSublanes ? _3.getSublaneFromPoint() : null;
      var _20 = {
        $1299: _4.hasLanes(),
        $1297: _3.useSublanes,
        $117z: _16 ? _4.getDateLeftOffset(_3.getEventStartDate(_2)) : _10 + (_15 && _19 ? _19.left : 0),
        $1171: _1.getLeft() + _14,
        $125a: _16 ? _7 + (_19 ? _19.top : 0) : _1.getTop(),
        $125b: (_16 ? (_19 ? _19.height : _18.height) : _1.getVisibleHeight()),
        $131b: _16 ? _1.getVisibleWidth() : (_19 ? _19.width : (_18 && _4.getLaneWidth ? _4.getLaneWidth(_2[_3.laneNameField]) : _11)),
        $1172: _17,
        $131q: _16 && !_17,
        $131g: !_16,
        $131e: _3.getEventStartDate(_1.event),
        $131f: _3.getEventEndDate(_1.event),
        $130b: _18,
        $1298: _19
      };
      if (_20.$125a == -1) {
        _20.$125a = 0;
        _20.$125b -= _5.getScrollTop()
      }
      _1.$1245 = _20;
      this.positionToEventCanvas(true);
      return isc.EH.STOP_BUBBLING
    },
    dragResizeMove: function () {
      var _1 = this.eventCanvas,
          _2 = _1.$1245,
          _3 = _1.event,
          _4 = _1.calendar,
          _5 = this.view,
          _6 = _2.$125a,
          _7 = _2.$117z,
          _8 = _2.$125b,
          _9 = _2.$131b,
          _10 = _2.$131e,
          _11 = _2.$131f,
          _12 = isc.DateUtil;
      var _13 = _5.getDateFromPoint();
      if (_2.$131g) {
        _11 = _4.addSnapGapsToDate(_13, _5, 1);
        if (_11.getDate() != _10.getDate()) {
          _11 = isc.DateUtil.getEndOf(_10, "d")
        }
        var _14 = _5.getDateTopOffset(_11);
        _8 = _14 - _6
      } else if (_2.$1172) {
        if (!_13) _13 = _5.startDate.duplicate();
        _10 = _13;
        var _15 = _7 + _9;
        if (_3[_4.durationField] != null) {
          var _16 = _11.getTime() - _10.getTime(),
              _17 = _3[_4.durationUnitField],
              _18 = _12.getTimeUnitMilliseconds(_17);
          if (_16 % _18 != 0) {
            var _19 = Math.round(_12.convertPeriodUnit(_16, "ms", _17)),
                _10 = _12.dateAdd(_11.duplicate(), _17, _19 * -1)
          }
        }
        _7 = _5.getDateLeftOffset(_10);
        _9 = (_15 - _7)
      } else {
        if (!_13) _13 = _5.endDate.duplicate();
        else _13 = _4.addSnapGapsToDate(_13.duplicate(), _5, 1);
        _11 = _13.duplicate();
        var _20 = _4.getVisibleEndDate(_5);
        if (_11.getTime() > _20.getTime()) {
          _11.setTime(_20.getTime())
        }
        if (_3[_4.durationField] != null) {
          var _16 = _11.getTime() - _10.getTime(),
              _17 = _3[_4.durationUnitField],
              _18 = _12.getTimeUnitMilliseconds(_17);
          if (_16 % _18 != 0) {
            var _19 = Math.round(_12.convertPeriodUnit(_16, "ms", _17)),
                _11 = _12.dateAdd(_10.duplicate(), _17, _19)
          }
        }
        _9 = _5.$732({
          startDate: _10,
          endDate: _11
        })
      }
      var _21 = true;
      if (_11.getTime() <= _10.getTime()) {
        _21 = false
      } else {
        var _22 = _11.duplicate();
        _22.setTime(_11.getTime() - 1);
        if (((_2.$1172 || _2.$137d) && _4.shouldDisableDate(_10, _5)) || ((_2.$131q || _2.$131g) && _4.shouldDisableDate(_22, _5))) {
          _21 = false
        }
      }
      if (_21) {
        var _23 = _4.createEventObject(_3, _10, _11)
        var _21 = _4.eventResizeMove(_3, _23, _5);
        if (_21 != false) {
          _2.$131e = _10;
          _2.$131f = _11;
          _2.$125a = _6;
          _2.$117z = _7;
          _2.$131b = _9;
          _2.$125b = _8
        }
      }
      this.moveTo(_2.$117z, _2.$125a);
      this.resizeTo(_2.$131b, _2.$125b);
      if (_4.showDragHovers) isc.Hover.show(this.getHoverHTML(), this.hoverProps);
      return isc.EH.STOP_BUBBLING
    },
    dragResizeStop: function () {
      var _1 = this.eventCanvas,
          _2 = _1.$1245,
          _3 = _1.calendar,
          _4 = this.view,
          _5 = _1.event,
          _6 = _2.$131e,
          _7 = _2.$131f;
      if (_3.showDragHovers) isc.Hover.hide();
      this.hide();
      var _8 = _3.createEventObject(_5, _6);
      if (_5[_3.durationField] != null) {
        var _9 = _7.getTime() - _6.getTime();
        var _10 = Math.round(isc.DateUtil.convertPeriodUnit(_9, "ms", _5[_3.durationUnitField]));
        _8[_3.durationField] = _10;
        _7 = _2.$131f = _3.getEventEndDate(_8)
      }
      _8[_3.endDateField] = _7;
      var _11 = _3.eventResizeStop(_5, _8, null, this);
      if (_11 != false) {
        if (_4.isTimelineView()) {
          if (_3.timelineEventResized(_5, _6, _7) == false) return false
        } else {
          if (_3.eventResized(_7, _5) == false) return false
        }
        _3.updateCalendarEvent(_5, _8)
      }
      this.$1244 = false;
      return isc.EH.STOP_BUBBLING
    }
  };
  isc.B.push(isc.A.initWidget = function isc_CalendarView_initWidget() {
    var _1 = this.calendar,
        _2 = _1.showCellHovers;
    if (_2) {
      this.canHover = _2;
      this.showHover = _2
    }
    this.Super("initWidget", arguments)
  }, isc.A.isSelectedView = function isc_CalendarView_isSelectedView() {
    return this.calendar.getCurrentViewName() == this.viewName
  }, isc.A.isTimelineView = function isc_CalendarView_isTimelineView() {
    return this.viewName == "timeline"
  }, isc.A.isDayView = function isc_CalendarView_isDayView() {
    return this.viewName == "day"
  }, isc.A.isWeekView = function isc_CalendarView_isWeekView() {
    return this.viewName == "week"
  }, isc.A.isMonthView = function isc_CalendarView_isMonthView() {
    return this.viewName == "month"
  }, isc.A.getLaneIndex = function isc_CalendarView_getLaneIndex(_1) {
    return null
  }, isc.A.getLane = function isc_CalendarView_getLane(_1) {
    return null
  }, isc.A.getLaneFromPoint = function isc_CalendarView_getLaneFromPoint(_1, _2) {
    return null
  }, isc.A.getSublane = function isc_CalendarView_getSublane(_1, _2) {
    if (!this.hasSublanes()) return null;
    var _3 = this.getLane(_1),
        _4 = _3 && _3.sublanes ? isc.isAn.Object(_2) ? _2 : _3.sublanes.find(this.calendar.laneNameField, _2) : null;
    return _4
  }, isc.A.getSublaneFromPoint = function isc_CalendarView_getSublaneFromPoint(_1, _2) {
    return null
  }, isc.A.hasLanes = function isc_CalendarView_hasLanes() {
    return this.isTimelineView() || (this.isDayView() && this.calendar.showDayLanes)
  }, isc.A.hasSublanes = function isc_CalendarView_hasSublanes() {
    return this.calendar.useSublanes && this.hasLanes()
  }, isc.A.getEventCanvasStyle = function isc_CalendarView_getEventCanvasStyle(_1) {
    if (this.hasLanes()) {
      var _2 = this.calendar,
          _3 = _2.laneNameField,
          _4 = _2.sublaneNameField,
          _5 = _2.eventStyleNameField,
          _6 = this.getLane(_1[_3]),
          _7 = _6 ? this.getSublane(_6[_3], _1[_4]) : null;
      return (_7 && _7.eventStyleName) || (_6 && _6.eventStyleName) || this.eventStyleName
    }
    return this.eventStyleName
  }, isc.A.getDateFromPoint = function isc_CalendarView_getDateFromPoint() {
    return this.getCellDate()
  }, isc.A.mouseMove = function isc_CalendarView_mouseMove() {
    var _1 = this.getDateFromPoint();
    if (_1 != this.$131h) {
      this.$131h = _1;
      this.calendar.$131i(this, _1)
    }
    if (this.$53r) {
      if (this.isTimelineView()) this.cellOver()
    }
  }, isc.A.getHoverHTML = function isc_CalendarView_getHoverHTML() {
    var _1 = this.calendar.getCellHoverHTML(this);
    return _1
  }, isc.A.getPrintHTML = function isc_CalendarView_getPrintHTML(_1, _2) {
    _1 = isc.addProperties({}, _1);
    this.body.printChildrenAbsolutelyPositioned = true;
    var _3 = this.calendar,
        _4 = this.isTimelineView(),
        _5 = this.isWeekView(),
        _6 = this.isDayView(),
        _7 = this.isMonthView();
    if (_7) return;
    var _8 = this.getFields(),
        _9 = this.getData(),
        _10 = isc.StringBuffer.create(),
        _11 = 0,
        _12 = null,
        _13 = this;
    if (_4) {
      _12 = _8.map(function (_29) {
        return _13.getFieldWidth(_29)
      });
      if (this.frozenFields) _11 += this.frozenBody.$26a.sum()
    } else {
      _11 = this.body.$26a.sum();
      if (this.frozenBody) _11 += this.frozenBody.$26a.sum()
    }
    var _14 = "<TR",
        _15 = "</TR>",
        _16 = ">",
        _17 = " HEIGHT=",
        _18 = " VALIGN=";
    var _19 = 40;
    _10.append("<TABLE WIDTH=", _11, " style='position: absolute; top:", _19, ";'>");
    if (this.showHeader) {
      _10.append(this.getPrintHeaders(0, this.fields.length))
    }
    _19 += this.getHeaderHeight();
    _10.append("<TABLE role='presentation' border='' class:'", this.baseStyle, "' ", "style='borderSpacing:0; position: absolute; top:", _19, "; z-index: -1' cellpadding='0' cellspacing='0'>");
    for (var i = 0; i < _9.length; i++) {
      _10.append(_14, _17, this.getRowHeight(i), _16);
      for (var j = 0; j < _8.length; j++) {
        var _22 = this.getCellValue(_9[i], i, j);
        _10.append("<TD padding=0 class='", this.getCellStyle(_9[i], i, j), "' ", "width='", this.getFieldWidth(j) + (j == 0 ? 2 : 4), "px' ", "style='margin: 0px; padding: 0px; ", "border-width: 0px 1px 1px 0px; ", "border-bottom: 1px solid #ABABAB; border-right: 1px solid #ABABAB; ", "border-top: none; border-left: none;'>");
        _10.append(this.getCellValue(_9[i], i, j) || "&nbsp;");
        _10.append("</TD>")
      }
      _10.append(_15)
    }
    var _23 = [];
    if (_3.isTimeline()) {
      _23 = this.getVisibleEvents();
      for (var i = 0; i < _23.length; i++) {
        var _24 = _23.get(i),
            _25 = _3.getEventCanvasID(this, _24),
            _26 = window[_25],
            _27 = isc.addProperties({}, _1, {
            i: i
          });
        if (_26) {
          _10.append(_26.getPrintHTML(_1, _2))
        }
      }
    } else {
      _23 = this.body.children;
      for (var i = 0; i < _23.length; i++) {
        if (!_23[i].isEventCanvas) continue;
        _10.append(_23[i].getPrintHTML(_1, _2))
      }
    }
    _10.append("</TR>");
    _10.append("</TABLE>");
    _10.append("</TABLE>");
    var _28 = _10.toString();
    return _28
  }, isc.A.getPrintHeaders = function isc_CalendarView_getPrintHeaders(_1, _2) {
    var _3 = (this.isRTL() ? isc.Canvas.LEFT : isc.Canvas.RIGHT),
        _4 = this.printHeaderStyle || this.headerBaseStyle,
        _5;
    if (this.headerSpans) {
      var _6 = [],
          _7 = 1;
      for (var i = _1; i < _2; i++) {
        var _9 = this.getField(i);
        _6[i] = [_9];
        var _10 = this.spanMap[_9.name];
        while (_10 != null) {
          _6[i].add(_10);
          _10 = _10.parentSpan
        }
        _7 = Math.max(_6[i].length, _7)
      }
      _5 = [];
      for (var i = _7 - 1; i >= 0; i--) {
        _5[_5.length] = "<TR>";
        var _11 = null,
            _12 = null;
        for (var _13 = _1; _13 < _2; _13++) {
          var _14 = 1,
              _15 = 1;
          var _16 = (i == 0);
          var _17 = _6[_13][i];
          if (_17 == "spanned") {
            continue
          }
          var _18, _19 = _13,
              _20 = [];
          _15 = 0;
          while (_19 < _2) {
            var _21 = null,
                _22 = false;
            for (var _23 = 0;
            (i - _23) >= 0; _23++) {
              _21 = _6[_19][i - _23];
              if (_21 != null) {
                if (_17 == null) {
                  _17 = _21;
                  _18 = _23;
                  if (i - _23 == 0) {
                    _16 = true
                  }
                }
                if (_17 == _21) {
                  _20[_15] = _23;
                  _18 = Math.min(_23, _18)
                } else {
                  _22 = true
                }
                break
              }
            }
            if (_22) {
              break
            }
            _19++;
            _15++
          }
          if (_18 != null) {
            _14 = _18 + 1
          }
          for (var _24 = 0; _24 < _20.length; _24++) {
            var _25 = _6[_24 + _13],
                _23 = _20[_24];
            for (var _26 = 0; _26 <= _23; _26++) {
              if (_24 == 0 && _26 == 0) {
                _25[i - _26] = _17
              } else if (_26 <= _18) {
                _25[i - _26] = "spanned"
              } else {
                _25[i - _26] = null
              }
            }
          }
          if (_17 == null) {
            this.logWarn("Error in getPrintHeaders() - unable to generate print header HTML from this component's specified headerSpans")
          }
          var _27 = "center",
              _28;
          if (_16) {
            _27 = _17.align || _3;
            _28 = this.getHeaderButtonTitle(_17.masterIndex)
          } else {
            _28 = _17.title
          }
          var _29 = _5.length;
          _5[_5.length] = "<TD class='";
          _5[_5.length] = _4;
          _5[_5.length] = "' align='";
          _5[_5.length] = "center";
          _5[_5.length] = "' rowSpan='";
          _5[_5.length] = _14;
          _5[_5.length] = "' colSpan='";
          _5[_5.length] = _15;
          _5[_5.length] = "' width=";
          _5[_5.length] = this.getFieldWidth(_17);
          _5[_5.length] = ">";
          _5[_5.length] = _28;
          _5[_5.length] = "</TD>"
        }
        _5[_5.length] = "</TR>"
      }
    } else {
      _5 = ["<TR>"];
      var _30 = ["<TD CLASS=", _4, " ALIGN="].join("");
      if (this.frozenBody) {
        for (var _31 = 0; _31 < this.frozenFields.length; _31++) {
          var _9 = this.frozenBody.fields[_31];
          if (!_9) continue;
          var _27 = _9.align || _3;
          var _32 = this.getFieldWidth(_31);
          _5.addList([_30, _27, " width=" + _32 + ">", this.getHeaderButtonTitle(_9.masterIndex), "</TD>"])
        }
      }
      for (var _31 = _1; _31 < _2; _31++) {
        var _9 = this.body.fields[_31];
        if (!_9) continue;
        var _27 = _9.align || _3;
        var _32 = this.getFieldWidth(_31);
        _5.addList([_30, _27, " width=" + _32 + ">", this.getHeaderButtonTitle(_9.masterIndex), "</TD>"])
      }
      _5[_5.length] = "</TR>"
    }
    return _5.join(isc.emptyString)
  }, isc.A.scrolled = function isc_CalendarView_scrolled() {
    if (this.renderEventsOnDemand && this.refreshVisibleEvents) {
      var _1 = this,
          _2 = this.data;
      if (this.$57s) isc.Timer.clear(this.$57s);
      this.$57s = isc.Timer.setTimeout(function () {
        _1.refreshVisibleEvents()
      })
    }
  }, isc.A.resized = function isc_CalendarView_resized() {
    this.Super('resized', arguments);
    if (this.renderEventsOnDemand && this.isDrawn() && this.calendar.hasData()) {
      this.refreshVisibleEvents()
    }
  }, isc.A.forceDataSort = function isc_CalendarView_forceDataSort(_1, _2) {
    var _3 = this.calendar,
        _4 = [];
    if (this.isTimelineView() || (this.isDayView() && _3.showDayLanes)) {
      _4.add({
        property: _3.laneNameField,
        direction: "ascending"
      })
    }
    if (this.isTimelineView() && _3.overlapSortSpecifiers) {
      _4.addList(_3.overlapSortSpecifiers)
    } else {
      _4.add({
        property: _3.startDateField,
        direction: "ascending"
      })
    }
    if (_2 || !_1) {
      if (!_1) _1 = _3.data;
      _3.$53e = true
    }
    _1.setSort(_4)
  }, isc.A.findEventsInRange = function isc_CalendarView_findEventsInRange(_1, _2, _3, _4) {
    var _5 = this.calendar,
        _6 = {},
        _7 = _3 != null && (this.isTimelineView() || (this.isDayView() && _5.showDayLanes));
    _6[_5.startDateField] = _1;
    _6[_5.endDateField] = _2;
    if (_7) _6[_5.laneNameField] = _3;
    var _8 = this.findOverlappingEvents(_6, _6, false, _7, _4, true);
    return _8
  }, isc.A.findOverlappingEvents = function isc_CalendarView_findOverlappingEvents(_1, _2, _3, _4, _5, _6) {
    var _7 = this.calendar,
        _8 = _5 != null;
    var _9 = _8 ? _5 : _7.data;
    if (!_8) this.forceDataSort(_9, _6);
    var _10 = [],
        _11 = _9.getLength(),
        _12 = _7.getEventStartDate(_2),
        _13 = _7.getEventEndDate(_2),
        _14 = isc.DateUtil.getStartOf(_13, "d"),
        _15 = isc.DateUtil.getEndOf(_12, "d");
    var _16 = {};
    var _17 = _4 ? _1[_7.laneNameField] : null,
        _18 = 0;
    if (_17) _18 = _9.findIndex(_7.laneNameField, _17);
    if (_18 < 0) return _10;
    for (var i = _18; i < _11; i++) {
      var _20 = _9.get(i);
      if (!_20) {
        isc.logWarn('findOverlappingEvents: potentially invalid index: ' + i);
        break
      }
      if (_4 && _20[_7.laneNameField] != _17) break;
      if (!_3 && _7.eventsAreSame(_20, _1)) {
        continue
      }
      if (this.isTimelineView()) {
        if (_20[_7.leadingDateField] && _20[_7.trailingDateField]) {
          _16[_7.leadingDateField] = _2[_7.leadingDateField];
          _16[_7.trailingDateField] = _2[_7.trailingDateField];
          if (_16[_7.trailingDateField].getTime() > this.endDate.getTime()) {
            _16[_7.trailingDateField].setTime(this.endDate.getTime() - 1)
          }
        } else {
          _16[_7.startDateField] = _12;
          _16[_7.endDateField] = _13;
          if (_16[_7.endDateField].getTime() > this.endDate.getTime()) {
            _16[_7.endDateField].setTime(this.endDate.getTime() - 1)
          }
        }
      } else {
        if (_7.getEventStartDate(_20).getTime() > _15.getTime()) continue;
        if (_7.getEventEndDate(_20).getTime() < _14.getTime()) continue;
        _16[_7.startDateField] = _12;
        _16[_7.endDateField] = _13;
        if (_16[_7.endDateField].getTime() > _13.getTime()) {
          _16[_7.endDateField].setTime(_13.getTime())
        }
      }
      _16[_7.laneNameField] = _20[_7.laneNameField];
      if (this.eventsOverlap(_16, _20, _4)) {
        _10.add(_20)
      }
    }
    return _10
  }, isc.A.eventsOverlap = function isc_CalendarView_eventsOverlap(_1, _2, _3) {
    var a = _1,
        b = _2,
        _6 = this.calendar,
        _7 = _6.startDateField,
        _8 = _6.endDateField;
    if (_3 && a[_6.laneNameField] != b[_6.laneNameField]) return false;
    if (this.isTimelineView()) {
      if (a[_6.leadingDateField] && b[_6.leadingDateField]) _7 = _6.leadingDateField;
      if (a[_6.trailingDateField] && b[_6.trailingDateField]) _8 = _6.trailingDateField
    }
    var _9 = a[_7],
        _10 = a[_8] || _6.getEventEndDate(a),
        _11 = b[_7],
        _12 = b[_8] || _6.getEventEndDate(b);
    if (_6.equalDatesOverlap && _6.allowEventOverlap) {
      if ((_9 < _11 && _10 >= _11 && _10 <= _12) || (_9 <= _12 && _10 > _12) || (_9 <= _11 && _10 >= _12) || (_9 >= _11 && _10 <= _12)) {
        return true
      } else {
        return false
      }
    } else {
      if (_11 < _10 && _12 > _9) return true;
      return false
    }
  });
  isc.evalBoundary;
  isc.B.push(isc.A.updateEventRange = function isc_CalendarView_updateEventRange(_1, _2) {
    if (!isc.isAn.Object(_2)) _2 = this.overlapRanges.ranges[_2];
    var _3 = _2.events;
    _3.remove(_1);
    this.updateOverlapRanges(_3)
  }, isc.A.updateOverlapRanges = function isc_CalendarView_updateOverlapRanges(_1) {
    var _2 = this.calendar,
        _3 = _1 || _2.data,
        _4 = this.overlapRanges || [],
        _5 = _3.getLength(),
        _6 = this.isTimelineView() || (this.isDayView() && _2.showDayLanes),
        _7 = !this.isTimelineView(),
        _8 = [],
        _9 = this.startDate,
        _10 = this.endDate;
    if (isc.isA.ResultSet(_3)) {
      _3 = _3.allRows
    }
    _3.setProperty("$122k", false);
    _3.setProperty("$646", null);
    _3.setProperty("$126u", null);
    var _11 = _6 && _2.lanes ? isc.getKeys(_2.getLaneMap()) : [];
    for (var i = 0; i < _5; i++) {
      var _13 = _3.get(i);
      if (_13.$122k) continue;
      if (_6 && !_11.contains(_13[_2.laneNameField])) {
        continue
      }
      _13.$122k = true;
      _13.$646 = {};
      var _14 = false,
          _15 = {};
      _15[_2.startDateField] = _2.getEventStartDate(_13)
      _15[_2.endDateField] = _2.getEventEndDate(_13);
      if (_6) _15[_2.laneNameField] = _15.lane = _13[_2.laneNameField];
      _15.events = [];
      var _16 = this.findOverlappingEvents(_13, _13, true, _6, _3);
      if (_16 && _16.length > 0) {
        _15.totalSlots = _16.length;
        var _17 = _15.totalSlots;
        var _18 = 1;
        for (var j = 0; j < _16.length; j++) {
          var _20 = _16[j],
              _21 = _2.getEventStartDate(_20),
              _22 = _2.getEventStartDate(_20);
          if (_21 < _15[_2.startDateField]) _15[_2.startDateField] = _21;
          if (_22 > _15[_2.endDateField]) _15[_2.endDateField] = _22;
          var _23 = _20 != _13 ? this.findOverlappingEvents(_20, _20, true, _6, _3) : [];
          if (_23 && _23.length > 0) {
            var _24 = [];
            _23.map(function (_28) {
              if (_28.$646) _24.add(_28.$646.totalSlots)
            });
            if (_24.max() != _17) {
              _17 = Math.min(_24.max(), _17);
              _18++
            }
          }
          var _25 = _18;
          if (!_20.$646) {
            _20.$122k = true;
            _20.$646 = {
              slotNum: _25,
              totalSlots: _18
            }
          } else {
            _20.$646.totalSlots = Math.max(_18, _20.$646.totalSlots);
            _20.$122l = true
          }
        }
        _15.totalSlots = _18;
        _16.map(function (_28) {
          if (_28.$122l) delete _28.$122l;
          else _28.$646.totalSlots = _18
        });
        _13.$646.totalSlots = _15.totalSlots;
        _15.events = _16;
        _14 = true;
        for (var k = 0; k < _4.length; k++) {
          if (_15[_2.laneNameField] != _4[k][_2.laneNameField]) continue;
          var _27 = this.eventsOverlap(_15, _4[k], true);
          if (_27) {
            if (_15.totalSlots > _4[k].totalSlots) {
              _13.$646.totalSlots = _15.totalSlots;
              _13.$646.slotCount = _15.totalSlots - _13.$646.slotNum
            }
            this.mergeOverlapRanges(_15, _4[k]);
            if (!_8.contains(_4[k])) _8.add(_4[k]);
            _14 = false
          }
          if (!_14) break
        }
      }
      if (_14) {
        _4.add(_15);
        if (!_8.contains(_15)) _8.add(_15)
      }
    }
    for (i = 0; i < _4.length; i++) {
      var _15 = _4[i];
      _15.events.setProperty("overlapRangeId", _4.length + i);
      if (!this.isTimelineView()) _15.colNum = this.getColFromDate(_15[_2.startDateField])
    }
    this.overlapRanges = _4;
    return _8
  }, isc.A.getTouchedOverlapRanges = function isc_CalendarView_getTouchedOverlapRanges(_1, _2, _3) {
    if (!this.overlapRanges) this.overlapRanges = [];
    var _4 = true,
        _5 = this.calendar,
        _6 = this.overlapRanges,
        r = {},
        _8 = [];
    r[_5.startDateField] = _1;
    r[_5.endDateField] = _2;
    r[_5.laneNameField] = _3;
    for (var k = 0; k < _6.length; k++) {
      var _10 = _6[k];
      if (_3 != null && _10[_5.laneNameField] != _3) continue;
      var _11 = this.eventsOverlap(r, _10, true);
      if (_11) {
        _8.add(_10)
      }
    }
    return _8
  }, isc.A.mergeOverlapRanges = function isc_CalendarView_mergeOverlapRanges(_1, _2) {
    if (!isc.isAn.Array(_1)) _1 = [_1];
    var _3 = this.calendar,
        _4 = _3.startDateField,
        _5 = _3.endDateField,
        b = _2;
    for (var i = 0; i < _1.length; i++) {
      var a = _1[i];
      if (a[_4] < b[_4]) b[_4] = a[_4];
      if (a[_5] > b[_5]) b[_5] = a[_5];
      if (a.totalSlots > b.totalSlots) b.totalSlots = a.totalSlots;
      b.events.addList(a.events);
      b.events = b.events.getUniqueItems()
    }
  }, isc.A.getEventLaneIndex = function isc_CalendarView_getEventLaneIndex(_1) {
    return this.getLaneIndex(_1[this.calendar.laneNameField])
  }, isc.A.getEventLane = function isc_CalendarView_getEventLane(_1) {
    return this.getLane(_1[this.calendar.laneNameField])
  }, isc.A.hasOverlapRanges = function isc_CalendarView_hasOverlapRanges() {
    return this.overlapRanges != null && this.overlapRanges.length > 0
  }, isc.A.getLaneOverlapRanges = function isc_CalendarView_getLaneOverlapRanges(_1) {
    if (!this.hasOverlapRanges()) return;
    var _2 = this.calendar,
        _3 = [];
    this.overlapRanges.map(function (_4) {
      if (_4[_2.laneNameField] == _1) _3.add(_4)
    });
    return _3
  }, isc.A.getDayOverlapRanges = function isc_CalendarView_getDayOverlapRanges(_1) {
    if (!this.hasOverlapRanges()) return;
    var _2 = this.getColFromDate(_1);
    if (_2 >= 0) return this.getColOverlapRanges(_2)
  }, isc.A.getColOverlapRanges = function isc_CalendarView_getColOverlapRanges(_1) {
    if (!this.hasOverlapRanges()) return;
    var _2 = this.overlapRanges.findAll("colNum", _1);
    return _2
  }, isc.A.removeOverlapRanges = function isc_CalendarView_removeOverlapRanges(_1) {
    if (!this.hasOverlapRanges() || !_1) return;
    _1.map(function (_2) {
      _2.events.setProperty("overlapRangeId", null)
    });
    this.overlapRanges.removeList(_1)
  }, isc.A.getEventOverlapRange = function isc_CalendarView_getEventOverlapRange(_1) {
    if (!this.hasOverlapRanges()) return;
    return this.overlapRanges[_1.overlapRangeId]
  }, isc.A.getDateOverlapRange = function isc_CalendarView_getDateOverlapRange(_1, _2) {
    if (!this.hasOverlapRanges()) return;
    var _3 = this.calendar,
        _4 = _1.getTime();
    var _5 = this.overlapRanges.map(function (_6) {
      if (_4 >= _6[_3.startDateField].getTime() && _4 <= _6[_3.endDateField].getTime() && (!_2 || _2 == _6[_3.laneNameField])) {
        return _6
      }
    });
    if (_5) _5.removeEmpty();
    return _5 && _5.length && _5[0] ? _5[0] : null
  }, isc.A.retagLaneEvents = function isc_CalendarView_retagLaneEvents(_1) {
    var _2 = this.isTimelineView();
    if (!(_2 || (this.isDayView() && this.calendar.showDayLanes))) return;
    var _3 = this.getLane(_1);
    if (_2) {
      this.retagRowEvents(_3, true)
    } else {
      this.retagColumnEvents(_3, true)
    }
  }, isc.A.retagDayEvents = function isc_CalendarView_retagDayEvents(_1) {
    if (this.isTimelineView()) return;
    var _2 = this.getColFromDate(_1);
    this.retagColumnEvents(_2, false)
  }, isc.A.retagColumnEvents = function isc_CalendarView_retagColumnEvents(_1, _2) {
    if (this.isTimelineView()) return;
    var _3;
    if (isc.isA.Number(_1)) {
      _3 = this.body.getField(_1)
    } else {
      _3 = _1;
      _1 = this.body.getFieldNum(_3)
    }
    this.removeOverlapRanges(this.getColOverlapRanges(_1));
    var _4 = this.getDateFromCol(_1);
    if (!_4) return;
    var _5 = _4,
        _6 = isc.DateUtil.getEndOf(_4, "d");
    var _7 = this.findEventsInRange(_5, _6, (_2 ? _3.name : null));
    this.renderEvents(_7, _2)
  }, isc.A.retagRowEvents = function isc_CalendarView_retagRowEvents(_1) {
    if (!this.isTimelineView()) return;
    var _2 = this.calendar,
        _3;
    if (isc.isA.Number(_1)) {
      _3 = this.getRecord(_1)
    } else {
      _3 = _1;
      _1 = this.isGrouped ? this.getGroupedRecordIndex() : this.getRecordIndex(_3)
    }
    var _4 = _3[_2.laneNameField];
    this.removeOverlapRanges(this.getLaneOverlapRanges(_4));
    var _5 = this.startDate,
        _6 = this.endDate;
    var _7 = this.findEventsInRange(_5, _6, _4);
    this.renderEvents(_7, true)
  }, isc.A.retagOverlapRange = function isc_CalendarView_retagOverlapRange(_1, _2, _3) {
    var _4 = this.calendar,
        _5 = this.getTouchedOverlapRanges(_1, _2, _3),
        _6 = _5 ? _5[0] : null,
        _7 = _1.duplicate(),
        _8 = _2.duplicate();
    if (_6) {
      _5.removeAt(0);
      this.mergeOverlapRanges(_5, _6);
      _7 = _6[_4.startDateField];
      _8 = _6[_4.endDateField];
      this.removeOverlapRanges(_5);
      this.removeOverlapRanges([_6]);
      var _9 = this.findEventsInRange(_7, _8, _3, _6.events);
      this.renderEvents(_9, (_3 != null))
    } else {
      var _9 = this.findEventsInRange(_7, _8, _3, _4.data);
      this.renderEvents(_9, (_3 != null))
    }
  }, isc.A.sortForRender = function isc_CalendarView_sortForRender(_1) {
    var _2 = this.calendar,
        _3 = [];
    if (this.isTimelineView() || (this.isDayView() && _2.showDayLanes)) {
      _3.add({
        property: _2.laneNameField,
        direction: "ascending"
      })
    }
    if (this.isTimelineView() && _2.overlapSortSpecifiers) {
      _3.addList(_2.overlapSortSpecifiers)
    } else {
      _3.addList([{
        property: "$126u",
        direction: "ascending"
      }, {
        property: _2.startDateField,
        direction: "ascending"
      }])
    }
    _1.setSort(_3)
  }, isc.A.renderEvents = function isc_CalendarView_renderEvents(_1, _2) {
    this.tagDataForOverlap(_1, _2);
    this.sortForRender(_1);
    var _3 = this.calendar,
        _4 = this.isTimelineView(),
        _5 = _2 ? (_4 ? this.body.getVisibleRows() : this.body.getVisibleColumns()) : [],
        _6 = this;
    for (var i = 0; i < _1.length; i++) {
      var _8 = _1.get(i),
          _9 = _8.$646,
          _10 = _2 ? _6.getLaneIndex(_8[_3.laneNameField]) : null;
      if (!_2 || (_10 >= _5[0] && _10 <= _5[1])) {
        var _11 = this.getCurrentEventCanvas(_8);
        if (_11) {
          _11.event = _8;
          _6.sizeEventCanvas(_11, _6)
        }
      }
    }
  }, isc.A.sizeEventCanvas = function isc_CalendarView_sizeEventCanvas(_1, _2) {
    if (Array.isLoading(_1.event)) return;
    var _3 = this.calendar,
        _4 = _1.event,
        _5 = this.isTimelineView(),
        _6 = this.isWeekView(),
        _7 = this.hasLanes(),
        _8 = _3.getEventStartDate(_4),
        _9 = _3.getEventEndDate(_4);
    if (_2) _1.hide();
    var _10, _11, _12, _13, _14 = _7 ? this.getLaneIndex(_4[_3.laneNameField]) : null,
        _15 = _7 ? this.getLane(_4[_3.laneNameField]) : null;
    if (_5) {
      if (!_15) return;
      _13 = this.getLaneHeight(_15.name);
      _12 = this.$732(_4);
      var _16 = _3.eventSnapGap;
      if (_3.isDurationEvent(_4) && _3.getEventDuration(_4) == 0) {
        _16 = _3.zeroLengthEventSize + (_3.getLanePadding(this) * 2)
      }
      _12 = Math.max(_12, _16);
      _11 = this.getEventLeft(_4);
      _10 = this.getRowTop(_14);
      var _17 = _3.getLanePadding(this);
      if (_17 > 0) {
        _10 += _17;
        _11 += _17;
        _12 -= (_17 * 2);
        _13 -= (_17 * 2)
      }
      if (_3.eventsOverlapGridLines) {
        _11 -= 1;
        _12 += 1;
        _10 -= 1;
        _13 += 1
      }
      if (this.eventDragGap > 0) {
        _12 = Math.max(this.eventDragGap, _12 - this.eventDragGap)
      }
    } else {
      var _18;
      if (this.isDayView()) {
        if (_3.showDayLanes) _18 = _14;
        else _18 = 0
      } else {
        _18 = this.getColFromDate(_8)
      }
      _11 = this.body.getColumnLeft(_18);
      _12 = this.body.getColumnWidth(_18);
      var _19 = this.body.getRowHeight(1),
          _20 = _9.getHours() == 0 && _9.getDate() != _8.getDate() ? 24 : _9.getHours(),
          _21 = false,
          _22 = _3.getMinutesPerRow(this),
          _23 = _3.getRowsPerHour(this);
      if (_9.getDate() > _8.getDate()) {
        _21 = true;
        _20 = 24
      }
      _10 = _8.getHours() * (_19 * _23);
      _13 = (_20 - _8.getHours()) * (_19 * _23);
      _13 -= 1;
      if (_3.weekEventBorderOverlap && _6) _12 += 1;
      var _24 = _8.getMinutes();
      if (_24 > 0) {
        var _25 = _3.getMinutePixels(_24, _19, this);
        _13 -= _25;
        _10 += _25
      }
      if (_9.getMinutes() > 0 && !_21) {
        _13 += _3.getMinutePixels(_9.getMinutes(), _19, this)
      }
      if (_3.weekEventBorderOverlap && _6) _12 += 1;
      if (_3.eventsOverlapGridLines) {
        _11 -= 1;
        _12 += 1;
        _10 -= 1;
        _13 += 1
      }
    }
    if (_3.useSublanes && _15 && _15.sublanes) {
      this.sizeEventCanvasToSublane(_1, _15, _11, _10, _12, _13)
    } else {
      this.adjustDimensionsForOverlap(_1, _11, _10, _12, _13)
    }
    if (_1.setDescriptionText) {
      if (_3.showEventDescriptions != false) {
        _1.setDescriptionText(_4[_3.descriptionField])
      } else {
        _1.setDescriptionText(_4[_3.nameField])
      }
    } else {
      _1.markForRedraw()
    }
    if (_5 && _4 != null) {
      if (_4[_3.leadingDateField] && _4[_3.trailingDateField]) {
        if (_1.$645) this.addLeadingAndTrailingLines(_1);
        else this.delayCall("addLeadingAndTrailingLines", [_1])
      }
    }
  }, isc.A.adjustDimensionsForOverlap = function isc_CalendarView_adjustDimensionsForOverlap(_1, _2, _3, _4, _5) {
    var _6 = this.calendar,
        _7 = _1.event.$646,
        _8 = this.isTimelineView(),
        _9 = _6.getLanePadding(this);
    if (_7 && _7.totalSlots > 0) {
      var _10 = _8 ? Math.floor(_5 / _7.totalSlots) : Math.floor(_4 / _7.totalSlots);
      if (_8) {
        _5 = _10;
        if (_7.slotCount) _5 *= _7.slotCount;
        if (_7.totalSlots > 1) {
          _5 -= Math.floor(_9 / (_7.totalSlots))
        }
        _3 = _3 + Math.floor((_10 * (_7.slotNum - 1)));
        if (_7.slotNum > 1) _3 += (_9 * (_7.slotNum - 1))
      } else {
        _4 = _10;
        if (_7.slotCount) Math.floor(_4 *= _7.slotCount);
        if (_7.totalSlots > 1) {}
        _2 = _2 + Math.floor((_10 * (_7.slotNum - 1)));
        if (_6.eventOverlap && _7.$64m != false) {
          if (_7.slotNum > 1) {
            _2 -= Math.floor(_10 * (_6.eventOverlapPercent / 100));
            _4 += Math.floor(_10 * (_6.eventOverlapPercent / 100))
          }
        }
        var _11 = !_7 ? true : (_7.slotNum == _7.totalSlots || (_7.slotNum + _7.slotCount) - 1 == _7.totalSlots);
        if (_11) {
          _4 -= _6.eventDragGap
        }
      }
      if (_6.eventsOverlapGridLines) {
        if (_8) {
          if (_7.totalSlots > 1) _5 += 1
        } else {
          _5 += 1;
          if (_7.slotNum > 0 && !_6.eventOverlap) {
            _4 += 1
          }
        }
      }
    }
    _1.renderEvent(_3, _2, _4, _5)
  }, isc.A.sizeEventCanvasToSublane = function isc_CalendarView_sizeEventCanvasToSublane(_1, _2, _3, _4, _5, _6) {
    var _7 = this.calendar,
        _8 = _1.event,
        _9 = _2.sublanes,
        _10 = _9.findIndex("name", _8[this.calendar.sublaneNameField]),
        _11 = this.isTimelineView(),
        _12 = _9.length,
        _13 = _7.getLanePadding(this),
        _14 = 0;
    if (_10 < 0) return;
    for (var i = 0; i <= _10; i++) {
      if (i == _10) {
        if (_11) {
          _4 += _14;
          _6 = _9[i].height - _13
        } else {
          _3 += _14;
          _5 = _9[i].width - _13;
          if (_3 + _5 + 1 < this.body.getScrollWidth()) _5 += 1;
          if (_4 + _6 + 1 < this.body.getScrollHeight()) _6 += 1
        }
        break
      }
      if (_11) _14 += _9[i].height;
      else _14 += _9[i].width
    }
    if (_10 > 0 && _13 > 0) {
      if (_11) _6 -= Math.floor(_13 / _9.length);
      else _5 -= Math.floor(_13 / _9.length)
    }
    _1.renderEvent(_4, _3, _5, _6)
  }, isc.A.tagDataForOverlap = function isc_CalendarView_tagDataForOverlap(_1, _2) {
    if (_1.getLength() == 0) return;
    var _3 = this.calendar,
        _4 = [],
        _5 = 0,
        _6 = 0,
        _7 = 0,
        _8 = this.isTimelineView();
    if (_3.eventAutoArrange == false) return;
    this.forceDataSort(_1);
    var _9 = _1.get(0),
        _10 = _9[_3.laneNameField];
    var _11 = [];
    _1.setProperty("$646", null);
    _1.setProperty("$126u", null);
    var _12 = this.isTimelineView() || (this.isDayView() && _3.showDayLanes);
    var _13 = this.updateOverlapRanges(_1);
    var _14 = [];
    if (_8 || (this.isDayView() && _3.showDayLanes)) {
      _14.add({
        property: _3.laneNameField,
        direction: "ascending"
      })
    }
    if (_8 && _3.overlapSortSpecifiers) {
      _14.addList(_3.overlapSortSpecifiers)
    } else {
      _14.add({
        property: _3.startDateField,
        direction: "ascending"
      });
      _14.add({
        property: "eventLength",
        direction: "descending"
      })
    }
    for (var j = 0; j < _13.length; j++) {
      var _16 = _13[j];
      var _17 = _16.events;
      _17.setSort(_14);
      var _18 = [];
      var _19 = 1;
      for (var i = 0; i < _17.getLength(); i++) {
        var _21 = _17.get(i);
        _2 = _21[_3.laneNameField];
        _21.$646 = {};
        var _22 = null;
        var _23 = 1;
        if (_18.length > 0) {
          var _24 = [],
              _25 = false,
              _26 = 1,
              _27 = 1,
              _28 = 1;
          for (var k = 0; k < _18.length; k++) {
            var _30 = _18[k],
                r = isc.addProperties({}, _21),
                _32 = _30.$646;
            if (this.eventsOverlap(r, _30, _12)) {
              if (_3.eventOverlap) {
                if (!_3.eventOverlapIdenticalStartTimes) {
                  var _33 = (r[_3.startDateField].getTime() == _30[_3.startDateField].getTime());
                  if (_33) {
                    _22 = _32.slotNum
                  }
                } else {
                  _21.$646.$64m = true
                }
              }
              if (!_25) {
                _25 = true;
                if (_32.slotNum >= _23) {
                  if (_32.slotNum == _23 + 1) {
                    _21.$646.endSlotNum = _32.slotNum;
                    _21.$646.slotCount = _32.slotNum - _23;
                    break
                  } else if (_32.slotNum == _23) {
                    _23++;
                    _32.endSlotNum = _23;
                    _32.slotCount = _23 - _32.slotNum
                  }
                  continue
                }
              }
              if (_32.slotNum == _21.$646.endSlotNum) {
                _21.$646.slotCount = _21.$646.endSlotNum - _23;
                break
              }
              if (_32.slotNum == _23) {
                var _34 = _32.slotCount;
                if (_34 == null) _34 = 1;
                _23 = _32.slotNum + 1
              } else if (_32.slotNum > _23) {
                _21.$646.slotCount = _32.slotNum - _23;
                break
              }
              if (_32.slotCount == null) {
                _24.add(_30)
              }
            }
          }
          if (_24.length) {
            for (var k = 0; k < _24.length; k++) {
              var _35 = _24[k];
              _35.$646.slotCount = _23 - _35.$646.slotNum
            }
          }
        }
        _21.$646.slotNum = _21.$126u = _23;
        if (_22 != null && _22 < _23) _21.$646.$64m = false;
        if (_23 > _19) {
          _19 = _23
        }
        _18.add(_21)
      }
      _17.map(function (_36) {
        if (!_36.$646.slotCount) {
          _36.$646.slotCount = (_19 - _36.$646.slotNum) + 1
        }
        _36.$646.totalSlots = _19
      })
    }
    return _11
  }, isc.A.getVisibleDateRange = function isc_CalendarView_getVisibleDateRange() {
    var _1 = this.calendar;
    if (!this.renderEventsOnDemand) {
      if (this.isTimelineView()) {
        return [this.startDate.duplicate(), this.endDate.duplicate()]
      } else if (this.isWeekView()) {
        return [_1.chosenWeekStart, _1.chosenWeekEnd]
      } else if (this.isDayView()) {
        return [_1.chosenDateStart, _1.chosenDateEnd]
      } else if (this.isMonthView()) {
        return [isc.DateUtil.getStartOf(_1.chosenDate, "M"), isc.DateUtil.getEndOf(_1.chosenDate, "M")]
      }
    }
    var _2 = this.body.getScrollLeft(),
        _3 = _2 + this.body.getVisibleWidth(),
        _4 = this.body.getEventColumn(_2 + 1),
        _5 = this.body.getEventColumn(_3),
        _6 = this.body.getScrollTop(),
        _7 = _6 + this.body.getVisibleHeight(),
        _8 = this.body.getEventRow(_6 + 1),
        _9 = this.body.getEventRow(_7);
    if (_9 < 0 || isNaN(_9)) _9 = this.data.getLength() - 1;
    if (_5 < 0 || isNaN(_5)) {
      if (this.isTimelineView()) {
        _5 = this.$116z
      } else {
        _5 = this.body.fields.length - 1
      }
    }
    var _10 = this.getCellDate(_8, _4),
        _11 = this.getCellDate(_9, _5);
    return [_10, _11]
  }, isc.A.getVisibleRowRange = function isc_CalendarView_getVisibleRowRange() {
    if (!this.renderEventsOnDemand) {
      return [0, this.data.getLength()]
    }
    return this.getVisibleRows()
  }, isc.A.getVisibleColumnRange = function isc_CalendarView_getVisibleColumnRange() {
    if (!this.renderEventsOnDemand) {
      return [0, this.fields.getLength()]
    }
    return this.body.getVisibleColumns()
  }, isc.A.refreshVisibleEvents = function isc_CalendarView_refreshVisibleEvents(_1) {
    if (!this.body || !this.body.isDrawn()) return;
    if (!this.$128q) {
      this.refreshEvents();
      return
    }
    _1 = _1 || this.getVisibleEvents();
    this.sortForRender(_1);
    var _2 = [];
    var _3 = _1.getLength();
    var _4 = this.useEventCanvasPool ? this.$128q.duplicate() : [],
        _2 = [];
    this.logDebug('refreshing visible events', 'calendar');
    for (var i = 0; i < _3; i++) {
      var _6 = _1.get(i),
          _7 = this.$128q.contains(_6);
      if (_7) {
        _4.remove(_6);
        if (this.isGrouped || this.useEventCanvasPool) {
          var _8 = this.getCurrentEventCanvas(_6);
          this.sizeEventCanvas(_8, true)
        }
        continue
      }
      _2.add(_6)
    }
    if (this.isGrouped || (this.useEventCanvasPool && this.eventCanvasPoolingMode == "viewport")) {
      for (var i = 0; i < _4.length; i++) {
        var _8 = this.getCurrentEventCanvas(_4[i]);
        if (_8) this.clearEventCanvas(_8)
      }
    }
    if (_2.length > 0) {
      var _9 = _2.length;
      for (var i = 0; i < _9; i++) {
        var _6 = _2[i];
        if (!this.$128q.contains(_6)) this.$128q.add(_6);
        this.addEvent(_6, false)
      }
    }
    var _10 = this.calendar;
    if (_10.eventsRendered && isc.isA.Function(_10.eventsRendered)) _10.eventsRendered()
  }, isc.A.getVisibleEvents = function isc_CalendarView_getVisibleEvents() {
    if (!this.renderEventsOnDemand) return _1.data;
    var _1 = this.calendar,
        _2 = this.isTimelineView(),
        _3 = _1.showDayLanes && this.isDayView(),
        _4 = this.getVisibleDateRange(),
        _5 = (_2 || _3),
        _6 = _5 ? (_2 ? this.getVisibleRowRange() : this.getVisibleColumnRange()) : null;
    var _7 = _1.data,
        _8 = _4[0].getTime(),
        _9 = _4[1].getTime(),
        _10 = _7.getLength(),
        _11 = [],
        _12 = this.isWeekView(),
        _13 = this.isGrouped ? this.data.getOpenList() : null;
    for (var i = 0; i < _10; i++) {
      var _15 = _7.get(i);
      if (!_15) {
        isc.logWarn('getVisibleEvents: potentially invalid index: ' + i);
        break
      }
      if (isc.isA.String(_15)) return [];
      if (_1.shouldShowEvent(_15, this) == false) continue;
      var _16 = _1.getEventLeadingDate(_15) || _1.getEventStartDate(_15),
          _17 = _1.getEventTrailingDate(_15) || _1.getEventEndDate(_15),
          _18 = _17.getTime();
      if (_18 <= _8) continue;
      if (_16.getTime() >= _9) continue;
      if (_12) {
        if (_17.getHours() < _4[0].getHours()) continue;
        if (_16.getHours() > _4[1].getHours()) continue
      }
      var _19 = {};
      if (_5) {
        if (this.isGrouped) {
          var _20 = _13.findIndex(_1.laneNameField, _15[_1.laneNameField]);
          if (_20 < 0) continue
        } else {
          var _21 = this.getEventLaneIndex(_15);
          if (_21 == null || _21 < _6[0] || _21 > _6[1]) continue
        }
        _19[_1.laneNameField] = _15[_1.laneNameField]
      }
      if (_2) {
        if (_15[_1.leadingDateField] && _15[_1.trailingDateField]) {
          _19[_1.leadingDateField] = _4[0];
          _19[_1.trailingDateField] = _4[1]
        } else {
          _19[_1.startDateField] = _4[0];
          _19[_1.endDateField] = _4[1]
        }
      } else {
        _19[_1.startDateField] = _4[0];
        _19[_1.endDateField] = _4[1]
      }
      if (this.eventsOverlap(_19, _15, _5)) {
        _11.add(_15)
      }
    }
    return _11
  }, isc.A.clearEventCanvas = function isc_CalendarView_clearEventCanvas(_1, _2) {
    if (_1) {
      if (!isc.isAn.Array(_1)) _1 = [_1];
      var _3 = _1.length;
      while (--_3 >= 0) {
        var _4 = _1[_3];
        if (_4.hide) _4.hide();
        if (this.$129c) this.$129c.remove(_4);
        if (this.$128q) this.$128q.remove(_4.event);
        if (this.useEventCanvasPool && !_2) {
          this.poolEventCanvas(_4)
        } else {
          _4.destroy();
          _4 = null
        }
      }
    }
  }, isc.A.clearEvents = function isc_CalendarView_clearEvents(_1, _2) {
    var _3 = this.$128h;
    if (!this.body || !this.body.children || !_3) return;
    if (!_1) _1 = 0;
    if (_2 == null) _2 = !this.useEventCanvasPool;
    var _4 = this.$129c,
        _5 = _4.length;
    while (--_5 >= 0) {
      if (_4[_5]) {
        if (_4[_5].$128k) {
          this.clearEventCanvas(_4[_5], _2)
        }
      }
    }
    _4.removeEmpty()
  }, isc.A.areSame = function isc_CalendarView_areSame(_1, _2) {
    var _3 = this.calendar;
    if (_3.dataSource) {
      var _4 = _3.getEventPKs(),
          _5 = true;
      for (var i = 0, _7 = _4.length; i < _7; i++) {
        if (_1[_4[i]] != _2[_4[i]]) {
          _5 = false;
          break
        }
      }
      return _5
    } else {
      return (_1 === _2)
    }
  }, isc.A.getEventCanvasConstructor = function isc_CalendarView_getEventCanvasConstructor(_1) {
    return this.eventCanvasConstructor
  }, isc.A.getCurrentEventCanvas = function isc_CalendarView_getCurrentEventCanvas(_1) {
    var _2 = this.calendar.getEventCanvasID(this, _1);
    var _3 = window[_2];
    return _3
  }, isc.A.poolEventCanvas = function isc_CalendarView_poolEventCanvas(_1) {
    if (!this.$128h) this.$128h = [];
    if (this.body) {
      if (_1.event) {
        this.calendar.setEventCanvasID(this, _1.event, null);
        _1.event = null
      }
      _1.$128k = true;
      if (this.$129c) this.$129c.remove(_1);
      if (!this.$128h.contains(_1)) this.$128h.add(_1);
      return true
    } else return false
  }, isc.A.getPooledEventCanvas = function isc_CalendarView_getPooledEventCanvas(_1) {
    if (!this.$128h) this.$128h = [];
    if (!this.body) return;
    var _2 = this.$128h,
        _3 = this.calendar,
        _4;
    if (_2.length > 0) {
      var _5 = _2.findIndex("$128k", true);
      if (_5 < 0) return null;
      _4 = _2[_5];
      _4.$128k = false;
      _3.setEventCanvasID(this, _1, _4.ID);
      _2.remove(_4)
    }
    return _4
  }, isc.A.addEvent = function isc_CalendarView_addEvent(_1, _2) {
    if (!this.$129c) this.$129c = [];
    if (!this.$128h) this.$128h = [];
    this.clearSelection();
    var _3 = this.calendar,
        _4 = _3.$129r(_1, this),
        _5 = false;
    if (_4.isDrawn()) _4.hide();
    _4.calendarView = this;
    if (!this.$129c.contains(_4)) this.$129c.add(_4);
    if (this.body) this.body.addChild(_4);
    _4.$53i = this.isWeekView();
    if (this.isDayView() && _3.showDayLanes) {
      var _6 = _1[_3.laneNameField],
          _7 = this.lanes.find("name", _6);
      if (!_7) _5 = true
    }
    var _8 = _3.canEditEvent(_1);
    _4.setDragProperties(_8, _8, this.eventDragTarget);
    if (!_5 && this.body && this.body.isDrawn()) {
      if (_2) {
        this.retagOverlapRange(_3.getEventStartDate(_1), _3.getEventEndDate(_1), _1[_3.laneNameField])
      } else {
        this.sizeEventCanvas(_4)
      }
    }
  }, isc.A.removeEvent = function isc_CalendarView_removeEvent(_1) {
    var _2 = this.getCurrentEventCanvas(_1);
    if (_2) {
      this.clearEventCanvas(_2, !this.useEventCanvasPool);
      return true
    } else {
      return false
    }
  }, isc.A.clearZones = function isc_CalendarView_clearZones() {
    var _1 = this.$131c || [];
    for (var i = 0; i < _1.length; i++) {
      if (_1[i]) {
        this.body.removeChild(_1[i]);
        if (_1[i].destroy()) _1[i].destroy();
        _1[i] = null
      }
    }
    this.$131c = []
  }, isc.A.drawZones = function isc_CalendarView_drawZones() {
    if (this.$131c) this.clearZones();
    if (!this.calendar.showZones) return;
    var _1 = this.calendar,
        _2 = _1.zones || [],
        _3 = this.$131c = [];
    if (this.isGrouped) {
      this.logInfo("Zones are not currently supported in grouped Calendar views.");
      return
    }
    if (!_2 || _2.length <= 0) return;
    var _4 = [],
        _5 = this.getVisibleDateRange(),
        _6 = _5[0].getTime(),
        _7 = _5[1].getTime();
    _2.map(function (_9) {
      if (_9[_1.startDateField].getTime() < _7 && _9[_1.endDateField].getTime() > _6) {
        _4.add(_9)
      }
    });
    for (var i = 0; i < _4.length; i++) {
      var _9 = _4[i],
          _10 = _1.getZoneCanvas(_9, this),
          _11 = this.getDateLeftOffset(_9[_1.startDateField]),
          _12 = this.getDateLeftOffset(_9[_1.endDateField]);
      this.body.addChild(_10)
      _10.renderEvent(0, _11, _12 - _11, this.body.getScrollHeight());
      _3.add(_10)
    }
  }, isc.A.clearIndicators = function isc_CalendarView_clearIndicators() {
    var _1 = this.$131d || [];
    for (var i = 0; i < _1.length; i++) {
      if (_1[i]) {
        this.body.removeChild(_1[i]);
        if (_1[i].destroy()) _1[i].destroy();
        _1[i] = null
      }
    }
    this.$131d = []
  }, isc.A.drawIndicators = function isc_CalendarView_drawIndicators() {
    if (this.$131d) this.clearIndicators();
    if (!this.calendar.showIndicators) return;
    var _1 = this.calendar,
        _2 = _1.indicators || [],
        _3 = this.$131d = [];
    if (this.isGrouped) {
      this.logInfo("Indicators are not currently supported in grouped Calendar views.");
      return
    }
    if (!_2 || _2.length <= 0) return;
    var _4 = [],
        _5 = this.getVisibleDateRange(),
        _6 = _5[0].getTime(),
        _7 = _5[1].getTime();
    for (var i = 0; i < _2.length; i++) {
      var _9 = _2[i];
      delete _9.endDate;
      _9.duration = 0;
      _9.durationUnit = "minute";
      var _10 = _1.getEventStartDate(_9).getTime();
      if (_10 >= _6 && _10 < _7) {
        _4.add(_9)
      }
    };
    for (var i = 0; i < _4.length; i++) {
      var _9 = _4[i],
          _11 = _1.getIndicatorCanvas(_9, this),
          _12 = this.getDateLeftOffset(_9[_1.startDateField]);
      this.body.addChild(_11)
      _11.renderEvent(0, _12, 2, this.body.getScrollHeight());
      _3.add(_11)
    }
  }, isc.A.refreshEvents = function isc_CalendarView_refreshEvents() {
    if (this.$132n) return;
    this.$129s = true;
    if (!this.$129c) this.$129c = [];
    if (!this.$128q) this.$128q = [];
    var _1 = this.calendar;
    if (!this.body || !_1.hasData()) return;
    this.$132n = true;
    this.clearZones();
    this.clearIndicators();
    var _2 = this.$129c;
    if (_2.length > 0) {
      _2.setProperty("$128k", true);
      this.clearEvents(0, !this.useEventCanvasPool)
    }
    this.$128q = [];
    this.$129c = [];
    var _3 = _1.getVisibleStartDate(this),
        _4 = _3.getTime(),
        _5 = _1.getVisibleEndDate(this),
        _6 = _5.getTime();
    this.overlapRanges = [];
    var _7 = _1.data.getLength();
    var _8 = _1.data.getRange(0, _7);
    var _9 = [];
    var _10 = this.viewName + "Props";
    while (--_7 >= 0) {
      var _11 = _8.get(_7);
      if (!isc.isA.String(_11)) {
        if (_1.shouldShowEvent(_11, this) == false) continue;
        var _12 = _1.getEventLeadingDate(_11) || _1.getEventStartDate(_11),
            _13 = _12.getTime(),
            _14 = _1.getEventTrailingDate(_11) || _1.getEventEndDate(_11),
            _15 = _14.getTime();
        if ((_13 >= _4 && _13 < _6) || (_15 > _4 && _15 <= _6)) {
          _11.eventLength = (_14 - _12);
          if (_11[_1.durationField] != null) {
            _11.isDuration = true;
            _11.isZeroDuration = _11[_1.durationField] == 0
          }
          _9.add(_11)
        }
      }
    };
    this.tagDataForOverlap(_9);
    if (this.hasLanes() && _1.lanes) {
      var _16 = _1.lanes.length,
          _17 = [],
          _18 = false;
      for (var i = 0; i < _16; i++) {
        var _20 = _1.lanes[i];
        if (this.isGroupNode(_20)) continue;
        if (_1.shouldShowLane(_20)) {
          _17.add(_20);
          _18 = true
        }
      }
      if (_18 && (!this.lanes || this.lanes.length != _17.length)) {
        this.setLanes(_17, true);
        this.redraw()
      }
    }
    this.drawZones();
    this.drawIndicators();
    this.refreshVisibleEvents();
    if (this.$123r) {
      this.body.scrollTo(null, this.$123r);
      delete this.$123r
    }
    delete this.$123s;
    delete this.$132n
  });
  isc.evalBoundary;
  isc.B.push(isc.A.$116v = function isc_CalendarView__refreshData() {
    var _1 = this.calendar;
    if (_1.dataSource && isc.ResultSet && isc.isA.ResultSet(_1.data)) {
      _1.fetchData(_1.getNewCriteria(this))
    } else {
      _1.dataChanged()
    }
  });
  isc.B._maxIndex = isc.C + 69;
  isc.ClassFactory.defineClass("DaySchedule", "CalendarView");
  isc.DaySchedule.changeDefaults("bodyProperties", {
    snapToCells: false,
    suppressVSnapOffset: true,
    suppressHSnapOffset: true,
    childrenSnapToGrid: false
  });
  isc.A = isc.DaySchedule.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.autoDraw = false;
  isc.A.canSort = false;
  isc.A.canResizeFields = false;
  isc.A.canReorderFields = false;
  isc.A.showHeader = false;
  isc.A.showHeaderContextMenu = false;
  isc.A.showAllRecords = true;
  isc.A.fixedRecordHeights = true;
  isc.A.labelColumnWidth = 60;
  isc.A.labelColumnAlign = "right";
  isc.A.showLabelColumn = true;
  isc.A.labelColumnPosition = "left";
  isc.A.labelColumnBaseStyle = "labelColumn";
  isc.A.showRollOver = true;
  isc.A.useCellRollOvers = true;
  isc.A.canAutoFitFields = false;
  isc.A.canSelectCells = true;
  isc.B.push(isc.A.initWidget = function isc_DaySchedule_initWidget() {
    this.fields = [];
    var _1 = this.calendar;
    if (_1.showDayLanes && this.isDayView() && _1.alternateLaneStyles) {
      this.alternateFieldStyles = true;
      this.alternateFieldFrequency = _1.alternateFieldFrequency
    }
    if (_1.labelColumnWidth && _1.labelColumnWidth != this.labelColumnWidth) {
      this.labelColumnWidth = _1.labelColumnWidth
    }
    this.renderEventsOnDemand = _1.renderEventsOnDemand;
    this.eventDragGap = _1.eventDragGap;
    this.fields = [];
    this.Super("initWidget");
    if (isc.isAn.Array(_1.data)) {
      this.$128r = true;
      this.$53e = true
    }
    this.rebuildFields();
    this.addAutoChild("eventDragTarget");
    this.body.addChild(this.eventDragTarget);
    this.dragTarget = this.eventDragTarget
  }, isc.A.getFirstDateColumn = function isc_DaySchedule_getFirstDateColumn() {
    return this.frozenBody ? this.frozenFields.length : 0
  }, isc.A.getCellValue = function isc_DaySchedule_getCellValue(_1, _2, _3) {
    var _4 = this.getFirstDateColumn();
    if (_3 >= _4) return null;
    return this.Super("getCellValue", arguments)
  }, isc.A.reorderFields = function isc_DaySchedule_reorderFields(_1, _2, _3) {
    this.Super("reorderFields", arguments);
    this.refreshEvents()
  }, isc.A.rebuildFields = function isc_DaySchedule_rebuildFields() {
    var _1 = this.calendar,
        _2 = [],
        _3 = {
        width: this.labelColumnWidth,
        name: "label",
        title: " ",
        cellAlign: "right",
        calendar: _1,
        formatCellValue: function (_27, _28, _29, _30, _31) {
          var _4 = _31.creator.getRowsPerHour(_31);
          if (_29 % _4 == 0) {
            var _5 = (_29 / _4);
            var _6 = isc.Time.parseInput(_5);
            return isc.Time.toTime(_6, _31.creator.timeFormatter, true)
          } else {
            return ""
          }
        }
        };
    if (this.showLabelColumn && this.labelColumnPosition == "left") {
      _2.add(_3)
    }
    if (this.hasLanes()) {
      var _7 = this.lanes = this.lanes || _1.lanes.duplicate() || [];
      _2[0].frozen = true;
      var d = _1.chosenDate.duplicate(),
          _9 = isc.DaySchedule.$126v(_1, this, d),
          _10 = isc.Date.createLogicalDate(d.getFullYear(), d.getMonth(), d.getDate()),
          _11 = {
          date: _10,
          align: "center",
          canReorder: _1.canReorderLanes
          };
      for (var i = 0; i < _7.length; i++) {
        var _13 = _7[i],
            _14 = _13[_1.laneNameField] || _13.name,
            p = isc.addProperties({}, _11, {
            name: _14
          });
        p[_1.laneNameField] = _14;
        if (_13.sublanes) {
          var _16 = this.getLaneWidth(_13),
              _17 = _13.sublanes.length,
              _18 = Math.floor(_16 / _17),
              _19 = 0;
          for (var j = 0; j < _17; j++) {
            var _21 = _13.sublanes[j];
            _21[_1.laneNameField] = _21.name;
            _21.left = _19;
            if (_21.width == null) _21.width = _18;
            _19 += _21.width
          }
          _13.width = _13.sublanes.getProperty("width").sum()
        }
        _2.add(isc.addProperties(p, _13))
      }
      _9.setProperty(_14, "");
      this.setShowHeader(true);
      if (_1.canReorderLanes) this.canReorderFields = _1.canReorderLanes;
      if (_1.minLaneWidth != null) this.minFieldWidth = _1.minLaneWidth;
      this.data = _9
    } else {
      var _22 = _1.chosenDate;
      _2[0].frozen = true;
      _2.add({
        name: "day1",
        align: "center",
        date: _1.chosenDate
      });
      if (this.isWeekView()) {
        var _23 = 8;
        for (var i = 2; i < _23; i++) {
          _2.add({
            name: "day" + i,
            align: "center"
          })
        }
        this.setShowHeader(true);
        if (!_1.showWeekends) {
          var _24 = this.showLabelColumn && this.labelColumnPosition == "left" ? 1 : 0;
          var _25 = Date.getWeekendDays();
          for (var i = _24; i < _2.length; i++) {
            var _26 = ((i - _24) + _1.firstDayOfWeek) % 7;
            if (_25.contains(_26)) {
              _2[i].showIf = "return false;"
            }
          }
        }
        _22 = this.chosenWeekStart
      } else {
        this.setShowHeader(false)
      }
      this.data = isc.DaySchedule.$126v(_1, this, this.scaffoldingStartDate)
    }
    if (this.showLabelColumn && this.labelColumnPosition == "right") {
      _2.add(_3)
    }
    this.setFields(_2)
  }, isc.A.getDateFromPoint = function isc_DaySchedule_getDateFromPoint(_1, _2, _3, _4) {
    var _5 = this.calendar;
    if (_4) {}
    if (_1 == null && _2 == null) {
      _2 = this.body.getOffsetY();
      _1 = this.body.getOffsetX()
    }
    var _6 = this.body.getEventRow(_2),
        _7 = this.body.getRowHeight(_6),
        _8 = this.body.getRowTop(_6),
        _9 = this.body.getEventColumn(_1),
        _10 = (_9 < 0);
    if (_9 == -1) _9 = 0;
    else if (_9 == -2) _9 = this.body.fields.length - 1;
    var _11 = this.getCellDate(_6, _9),
        _12 = _5.getMinutesPerRow(this),
        _13 = _5.getRowsPerHour(this),
        _14 = _2 - _8,
        _15 = _14 - (_14 % _5.eventSnapGap),
        _16 = _12 / (_7 / _5.eventSnapGap),
        _17 = _15 / _5.eventSnapGap,
        _18 = _16 * _17;
    _11.setMinutes(_11.getMinutes() + _18);
    return _11
  }, isc.A.getCellDate = function isc_DaySchedule_getCellDate(_1, _2) {
    if (!(this.body && this.body.fields) || !this.$129f) return null;
    if (_1 < 0) _1 = this.data.getLength() - 1;
    var _3 = this.isDayView() ? "day1" : this.body.fields[_2][this.fieldIdProperty];
    return this.$129f[_1][_3].duplicate()
  }, isc.A.getEventLeft = function isc_DaySchedule_getEventLeft(_1) {
    var _2 = this.getColFromDate(this.calendar.getEventStartDate(_1));
    return this.body.getColumnLeft(_2)
  }, isc.A.getEventRight = function isc_DaySchedule_getEventRight(_1) {
    var _2 = this.getColFromDate(this.calendar.getEventEndDate(_1));
    return this.body.getColumnLeft(_2) + this.body.getColumnWidth(_2)
  }, isc.A.getDateLeftOffset = function isc_DaySchedule_getDateLeftOffset(_1) {
    for (var i = 0; i < this.fields.length; i++) {
      var f = this.fields[i];
      if (f.$66a != null && f.$659 != null && f.$658 != null) {
        var _4 = Date.createLogicalDate(f.$66a, f.$659, f.$658);
        if (Date.compareLogicalDates(_1, _4) == 0) {
          return this.getColumnLeft(this.getFieldNum(f))
        }
      }
    }
    return 0
  }, isc.A.getDateTopOffset = function isc_DaySchedule_getDateTopOffset(_1) {
    if (!_1) return null;
    var _2 = _1.getTime(),
        _3 = this.getColFromDate(_1),
        _4 = this.data.length;
    for (var i = 0; i <= _4; i++) {
      var _6 = this.getCellDate(i, _3),
          _7 = _6.getTime();
      if (_7 >= _2) {
        var _8 = i - (i == 0 ? 0 : 1),
            _9 = this.getRowTop(_8),
            _10 = this.getRowHeight(_8);
        if (_10 / this.calendar.eventSnapGap != 1) {
          var _11 = Math.floor(_7 / 1000 / 60),
              _12 = this.getRowHeight(i) / this.calendar.eventSnapGap,
              _13 = Math.floor((_11 / _12) * this.calendar.eventSnapGap);
          _9 += _13
        } else {
          _9 += _10
        }
        return _9
      }
    }
    return this.body.getScrollHeight() - 1
  }, isc.A.setLanes = function isc_DaySchedule_setLanes(_1) {
    this.lanes = _1.duplicate();
    this.rebuildFields();
    this.refreshEvents()
  }, isc.A.getLane = function isc_DaySchedule_getLane(_1) {
    var _2 = isc.isA.Number(_1) ? _1 : -1;
    if (_2 == -1) {
      if (isc.isAn.Object(_1)) _2 = this.body.fields.indexOf(_1);
      else if (isc.isA.String(_1)) _2 = this.getLaneIndex(_1)
    }
    if (_2 >= 0) return this.body.fields[_2]
  }, isc.A.getLaneIndex = function isc_DaySchedule_getLaneIndex(_1) {
    if (!this.isDayView() || !this.creator.showDayLanes) return;
    var _2 = this.body.fields,
        _3 = -1;
    if (isc.isAn.Object(_1)) _3 = _2.indexOf(_1)
    else if (isc.isA.String(_1)) {
      _3 = _2.findIndex("name", _1);
      if (_3 < 0) _3 = _2.findIndex(this.creator.laneNameField, _1)
    }
    return _3
  }, isc.A.getLaneWidth = function isc_DaySchedule_getLaneWidth(_1) {
    var _2 = null;
    if (isc.isA.String(_1)) _1 = this.getLane(_1);
    if (_1) {
      if (_1.width) _2 = _1.width;
      else {
        var _3 = this.calendar.laneNameField,
            _4 = this.body.fields.findIndex(_3, _1[_3]);
        _2 = _4 >= 0 ? this.body.getColumnWidth(_4) : null
      }
    }
    return _2
  }, isc.A.getLaneFromPoint = function isc_DaySchedule_getLaneFromPoint(_1, _2) {
    if (!this.hasLanes()) return null;
    if (_1 == null) _1 = this.body.getOffsetX();
    var _3 = this.body.getEventColumn(_1),
        _4 = this.body.fields[_3];
    return !this.isGroupNode(_4) ? _4 : null
  }, isc.A.getSublaneFromPoint = function isc_DaySchedule_getSublaneFromPoint(_1, _2) {
    if (!this.hasSublanes()) return null;
    if (_1 == null) _1 = this.body.getOffsetX();
    var _3 = this.body.getEventColumn(_1),
        _4 = this.body.fields[_3],
        _5 = _4 ? _4.sublanes : null;
    if (!_5) return null;
    var _6 = this.body.getColumnLeft(_3),
        _7 = _1 - _6,
        _8 = this.getLaneWidth(_4),
        _9 = _5.length,
        _10 = 0;
    for (var i = 0; i < _9; i++) {
      if (_10 + _5[i].width > _7) {
        return _5[i]
      }
      _10 += _5[i].width
    }
    return null
  }, isc.A.draw = function isc_DaySchedule_draw(_1, _2, _3, _4) {
    this.invokeSuper(isc.DaySchedule, "draw", _1, _2, _3, _4);
    this.logDebug('draw', 'calendar');
    this.body.addChild(this.eventDragTarget);
    this.eventDragTarget.setView(this);
    if (this.$128r) {
      delete this.$128r;
      this.refreshEvents()
    }
    this.setSnapGap();
    if (this.creator.scrollToWorkday) this.scrollToWorkdayStart()
  }, isc.A.setSnapGap = function isc_DaySchedule_setSnapGap() {
    var _1 = this.creator.eventSnapGap;
    this.body.snapVGap = Math.round((_1 / this.creator.getMinutesPerRow(this)) * this.body.getRowSize(0));
    this.body.snapHGap = null
  }, isc.A.scrollToWorkdayStart = function isc_DaySchedule_scrollToWorkdayStart() {
    var _1 = this.calendar;
    if (_1.scrollToWorkday && !this.hasLanes()) {
      var _2 = this.calcRowHeight();
      if (_2 != _1.rowHeight) {
        _1.setRowHeight(_2)
      }
    }
    var _3 = this.getWorkdayRange(),
        _4 = _3.start;
    var _5 = _1.getMinutesPerRow(this),
        _6 = _1.getRowsPerHour(this),
        _7 = _4.getHours() * _6,
        _8 = _4.getMinutes(),
        _9 = _8 % _5,
        _10 = Math.floor((_8 - _9) / _5);
    _7 += _10;
    if (_9 > 0) _7++;
    var _11 = _1.rowHeight * _7;
    this.body.delayCall("scrollTo", [0, _11])
  }, isc.A.getWorkdayRange = function isc_DaySchedule_getWorkdayRange() {
    var _1 = this.body.fields,
        _2 = {
        start: isc.Time.parseInput("23:59"),
        end: isc.Time.parseInput("00:01")
        },
        _3 = this.calendar,
        _4 = _3.chosenDate,
        _5;
    if (this.isWeekView()) {
      for (var i = 0; i < _1.length; i++) {
        _4 = this.getDateFromCol(i);
        if (isc.isA.Date(_4)) {
          _5 = isc.Time.parseInput(_3.getWorkdayStart(_4));
          if (isc.Date.compareDates(_2.start, _5) < 0) {
            _2.start = _5
          }
          _5 = isc.Time.parseInput(_3.getWorkdayEnd(_4));
          if (isc.Date.compareDates(_2.end, _5) > 0) {
            _2.end = _5
          }
        }
      }
    } else if (_3.showDayLanes) {
      for (var i = 0; i < _1.length; i++) {
        var _7 = _1[i],
            _8 = _7[_3.laneNameField];
        if (isc.isA.Date(_4)) {
          _5 = isc.Time.parseInput(_3.getWorkdayStart(_4, _8));
          if (isc.Date.compareDates(_2.start, _5) < 0) {
            _2.start = _5
          }
          _5 = isc.Time.parseInput(_3.getWorkdayEnd(_4, _8));
          if (isc.Date.compareDates(_2.end, _5) > 0) {
            _2.end = _5
          }
        }
      }
    } else {
      _2.start = isc.Time.parseInput(_3.getWorkdayStart(_3.chosenDate));
      _2.end = isc.Time.parseInput(_3.getWorkdayEnd(_3.chosenDate))
    }
    return _2
  }, isc.A.calcRowHeight = function isc_DaySchedule_calcRowHeight() {
    var _1 = this.getWorkdayRange(),
        _2 = _1.end.getHours() - _1.start.getHours(),
        _3 = this.calendar.rowHeight;
    if (_2 <= 0) return _3;
    var _4 = Math.floor(this.body.getViewportHeight() / (_2 * this.calendar.getRowsPerHour()));
    return _4 < _3 ? _3 : _4
  }, isc.A.getRowHeight = function isc_DaySchedule_getRowHeight(_1, _2) {
    return this.calendar.rowHeight
  }, isc.A.getDayFromCol = function isc_DaySchedule_getDayFromCol(_1) {
    if (_1 < 0) return null;
    var _2 = this.body.fields.get(_1).$654;
    return _2
  }, isc.A.getDateFromCol = function isc_DaySchedule_getDateFromCol(_1) {
    if (_1 < 0) return null;
    var _2 = this.getCellDate(0, _1);
    return _2
  }, isc.A.getColFromDate = function isc_DaySchedule_getColFromDate(_1) {
    for (var i = 0; i < this.body.fields.length; i++) {
      var _3 = this.body.fields.get(i);
      if (!_3.date) continue;
      if (isc.Date.compareLogicalDates(_1, _3.date) == 0) return i
    }
    return null
  }, isc.A.isLabelCol = function isc_DaySchedule_isLabelCol(_1) {
    return this.completeFields[_1] && this.completeFields[_1].date == null
  }, isc.A.cellDisabled = function isc_DaySchedule_cellDisabled(_1, _2) {
    var _3 = this.getFieldBody(_2);
    if (!_3 || _3 == this.frozenBody) return false;
    var _4 = this.getLocalFieldNum(_2),
        _5 = this.getCellDate(_1, _4);
    return this.calendar.shouldDisableDate(_5, this)
  }, isc.A.refreshStyle = function isc_DaySchedule_refreshStyle() {
    if (!this.body) return;
    if (this.isWeekView() || this.calendar.showDayLanes) {
      this.markForRedraw();
      return
    }
    for (var i = 0; i < this.data.length; i++) {
      this.body.refreshCellStyle(i, 1)
    }
  }, isc.A.headerClick = function isc_DaySchedule_headerClick(_1, _2) {
    var _3 = this.calendar;
    if (this.isLabelCol(_1)) return true;
    if (_3.showDayLanes && !this.isWeekView()) return true;
    var _4 = this.getField(_1);
    _3.dateChooser.dateClick(_4.$66a, _4.$659, _4.$658);
    _3.selectTab(0);
    return true
  }, isc.A.getCellAlign = function isc_DaySchedule_getCellAlign(_1, _2, _3) {
    return this.labelColumnAlign
  }, isc.A.cellMouseDown = function isc_DaySchedule_cellMouseDown(_1, _2, _3) {
    if (this.isLabelCol(_3) || this.cellDisabled(_2, _3)) return true;
    var _4 = this.getCellDate(this.body.getEventRow(), this.body.getEventColumn());
    if (this.creator.backgroundMouseDown && this.creator.backgroundMouseDown(_4) == false) return;
    if (!this.creator.canCreateEvents) return true;
    this.clearSelection();
    this.$53q = {};
    this.$53q.colNum = _3;
    this.$53q.startRowNum = _2;
    this.$53q.endRowNum = _2;
    this.$53r = true;
    this.refreshCellStyle(_2, _3)
  }, isc.A.cellOver = function isc_DaySchedule_cellOver(_1, _2, _3) {
    if (this.$53r && this.$53q) {
      var _4;
      if (this.$53q.startRowNum < this.$53q.endRowNum) {
        if (_2 > this.$53q.endRowNum) {
          _4 = _2
        } else {
          _4 = this.$53q.endRowNum
        }
        this.$53q.endRowNum = _2
      } else {
        if (_2 < this.$53q.endRowNum) {
          _4 = _2
        } else {
          _4 = this.$53q.endRowNum
        }
        this.$53q.endRowNum = _2
      }
      var _5 = 6,
          _6 = this.$53q.colNum;
      for (var i = _4 - _5; i < _4 + _5; i++) {
        if (i >= 0 && i <= 47) this.refreshCellStyle(i, _6)
      }
    }
  }, isc.A.cellMouseUp = function isc_DaySchedule_cellMouseUp(_1, _2, _3) {
    if (!this.$53q) return true;
    this.$53r = false;
    var _4, _5, _6;
    if (this.$53q.startRowNum > this.$53q.endRowNum) {
      _4 = this.$53q.endRowNum;
      _5 = this.$53q.startRowNum
    } else {
      _5 = this.$53q.endRowNum;
      _4 = this.$53q.startRowNum
    }
    _6 = _5 - _4 + 1;
    var _7 = this.calendar,
        _8 = _7.getCellDate(_4, _3, this),
        _9 = _7.getCellDate(_4 + _6, _3, this);
    if (_6 == 1 && _7.backgroundClick) {
      if (_7.backgroundClick(_8, _9) == false) {
        this.clearSelection();
        return
      }
    }
    if (_7.backgroundMouseUp) {
      if (_7.backgroundMouseUp(_8, _9) == false) {
        this.clearSelection();
        return
      }
    }
    var _10, _11;
    if (_7.showDayLanes && _7.dayViewSelected()) {
      _10 = this.getLaneFromPoint();
      _11 = _10 ? this.getSublaneFromPoint() : null
    }
    var _12 = _7.createEventObject(null, _8, _9, _10 && _10[_7.laneNameField], _11 && _11[_7.laneNameField]);
    _7.showEventDialog(_12, true)
  }, isc.A.getCellStyle = function isc_DaySchedule_getCellStyle(_1, _2, _3) {
    var _4 = this.calendar,
        _5 = this.getBaseStyle(_1, _2, _3);
    if (this.isLabelCol(_3)) return _5;
    if (this.cellDisabled(_2, _3)) return _5 + "Disabled";
    if (this.$53q && this.$53q.colNum == _3) {
      var _6 = this.$53q.startRowNum,
          _7 = this.$53q.endRowNum;
      if (_2 >= _6 && _2 <= _7 || _2 >= _7 && _2 <= _6) {
        if (_5 == _4.workdayBaseStyle) return _5 + "Selected";
        return _4.selectedCellStyle
      }
    }
    if (!this.isWeekView() && this.alternateRecordStyles && _2 % 2 != 0) {
      if (_5 == _4.workdayBaseStyle) return _5;
      return _5 + "Dark"
    }
    if (_4.dayViewSelected() && _4.showDayLanes && this.alternateFieldStyles && _3 % 2 != 0) {
      if (_5 == _4.workdayBaseStyle) return _5;
      return _5 + "Dark"
    }
    return _5
  }, isc.A.getBaseStyle = function isc_DaySchedule_getBaseStyle(_1, _2, _3) {
    var _4 = this.calendar,
        _5 = _4.getCellDate(_2, _3, this),
        _6 = _5 ? _4.getDateStyle(_5, _2, _3, this) : null,
        _7 = this.isWeekView();
    if (_6) {
      return _6
    }
    if (this.isLabelCol(_3)) return this.labelColumnBaseStyle;
    if (!_4.showWorkday) return this.baseStyle;
    var _8 = this.getFieldBody(_3),
        _9 = _3;
    if (_8 == this.body) _9 = this.getLocalFieldNum(_3);
    var _10 = _7 ? this.getDayFromCol(_9) : _4.chosenDate.getDay();
    var _11 = _7 ? this.getDateFromCol(_9) : _4.chosenDate.duplicate(),
        _12 = _11.duplicate(),
        _13 = _5 ? _5.duplicate() : null,
        _14 = _4.showDayLanes ? this.body.getField(_9)[_4.laneNameField] : null;
    if (_13) {
      var _15 = isc.Time.parseInput(_4.getWorkdayStart(_13, _14)),
          _16 = isc.Time.parseInput(_4.getWorkdayEnd(_13, _14));
      _11.setHours(_15.getHours(), _15.getMinutes(), 0, 0);
      _12.setHours(_16.getHours(), _16.getMinutes(), 0, 0);
      var _17 = _4.dateIsWorkday(_13, _14);
      _13 = _13.getTime();
      if (_17 && _11.getTime() <= _13 && _13 < _12.getTime()) {
        return _4.workdayBaseStyle
      } else {
        return this.baseStyle
      }
    } else {
      return this.baseStyle
    }
  }, isc.A.clearSelection = function isc_DaySchedule_clearSelection() {
    if (this.$53q) {
      var _1, _2, _3 = this.$53q.colNum;
      if (this.$53q.startRowNum < this.$53q.endRowNum) {
        _1 = this.$53q.startRowNum;
        _2 = this.$53q.endRowNum
      } else {
        _1 = this.$53q.endRowNum;
        _2 = this.$53q.startRowNum
      }
      this.$53q = null;
      for (var i = _1; i < _2 + 1; i++) {
        this.refreshCellStyle(i, _3)
      }
    }
  }, isc.A.destroyEvents = function isc_DaySchedule_destroyEvents() {
    if (!this.body || !this.body.children) return;
    var _1 = this.body.children.length;
    while (--_1 >= 0) {
      var _2 = this.body.children[_1];
      if (_2) {
        this.body.removeChild(_2);
        _2.destroy();
        _2 = null
      }
    }
    this.$128q = null;
    this.$129c = null;
    this.$128h = null
  }, isc.A.destroy = function isc_DaySchedule_destroy() {
    this.calendar = null;
    this.destroyEvents(true);
    if (this.clearZones) this.clearZones();
    if (this.clearIndicators) this.clearIndicators();
    this.Super("destroy", arguments)
  }, isc.A.updateEventWindow = function isc_DaySchedule_updateEventWindow(_1) {
    if (!this.body || !this.body.children) return;
    var _2 = this.body.children,
        _3 = this.calendar;
    for (var i = 0; i < _2.length; i++) {
      if (_2[i] && _2[i].isEventCanvas && this.areSame(_2[i].event, _1)) {
        _2[i].event = _1;
        this.sizeEventCanvas(_2[i]);
        if (_2[i].setDescriptionText) _2[i].setDescriptionText(_1[_3.descriptionField]);
        return true
      }
    }
    return false
  });
  isc.B._maxIndex = isc.C + 40;
  isc.ClassFactory.defineClass("WeekSchedule", "DaySchedule");
  isc.ClassFactory.defineClass("MonthSchedule", "CalendarView");
  isc.ClassFactory.defineClass("MonthScheduleBody", "GridBody");
  isc.MonthSchedule.changeDefaults("headerButtonProperties", {
    showRollOver: false,
    showDown: false,
    cursor: "default"
  });
  isc.MonthSchedule.changeDefaults("bodyProperties", {
    redrawOnResize: true
  });
  isc.A = isc.MonthSchedule.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.autoDraw = false;
  isc.A.leaveScrollbarGap = false;
  isc.A.showAllRecords = true;
  isc.A.fixedRecordHeights = true;
  isc.A.showHeader = true;
  isc.A.showHeaderContextMenu = false;
  isc.A.canSort = false;
  isc.A.canResizeFields = false;
  isc.A.canReorderFields = false;
  isc.A.canAutoFitFields = false;
  isc.A.canHover = true;
  isc.A.showHover = true;
  isc.A.hoverWrap = false;
  isc.A.showRollOver = true;
  isc.A.useCellRollOvers = true;
  isc.A.canSelectCells = true;
  isc.A.dayHeaderHeight = 20;
  isc.A.alternateRecordStyles = false;
  isc.A.cellHeight = 1;
  isc.A.enforceVClipping = true;
  isc.B.push(isc.A.initWidget = function isc_MonthSchedule_initWidget() {
    var _1 = this.calendar;
    if (_1.data) this.data = this.getDayArray();
    this.fields = [{
      name: "day1",
      align: "center"
    }, {
      name: "day2",
      align: "center"
    }, {
      name: "day3",
      align: "center"
    }, {
      name: "day4",
      align: "center"
    }, {
      name: "day5",
      align: "center"
    }, {
      name: "day6",
      align: "center"
    }, {
      name: "day7",
      align: "center"
    }];
    this.firstDayOfWeek = _1.firstDayOfWeek;
    var _2 = Date.getShortDayNames();
    var _3 = Date.getWeekendDays();
    for (var i = 0; i < 7; i++) {
      var _5 = (i + this.firstDayOfWeek) % 7;
      this.fields[i].title = _2[_5];
      this.fields[i].$654 = _5;
      this.fields[i].$66b = i + 1;
      if (!_1.showWeekends && _3.contains(_5)) {
        this.fields[i].showIf = "return false;"
      }
    }
    this.minimumDayHeight = _1.minimumDayHeight;
    this.Super("initWidget")
  }, isc.A.getCalendar = function isc_MonthSchedule_getCalendar() {
    return this.calendar
  }, isc.A.getCellCSSText = function isc_MonthSchedule_getCellCSSText(_1, _2, _3) {
    var _4 = this.creator.$116j(this, _1, _2, _3);
    if (_4) return _4;
    return this.Super("getCellCSSText", arguments)
  }, isc.A.getDayArray = function isc_MonthSchedule_getDayArray() {
    var _1 = [],
        _2, _3, _4 = new Date(this.creator.year, this.creator.month, 1),
        _5 = this.calendar;
    while (_4.getDay() != _5.firstDayOfWeek) {
      this.incrementDate(_4, -1)
    }
    if (!_5.showWeekends) {
      var _6 = Date.getWeekendDays();
      var _7 = _4.duplicate();
      var _8 = true;
      for (var i = 0; i <= 7 - _6.length; i++) {
        if (_7.getMonth() == _5.month) {
          _8 = false;
          break
        }
        this.incrementDate(_7, 1)
      }
      if (_8) this.incrementDate(_4, 7)
    }
    _3 = new Date(_5.year, _5.month, _4.getDate() + 40);
    _2 = _5.$53g(_4, _3, this);
    _2.sortByProperty("name", true, function (_10, _11, _12) {
      return _10[_12.startDateField].getTime()
    }, _5);
    this.$53t = 0;
    for (var i = 0; i < 6; i++) {
      if (_5.showDayHeaders) _1.add(this.getHeaderRowObject(_4));
      _1.add(this.getEventRowObject(_4, _2));
      this.incrementDate(_4, 7);
      if (_4.getMonth() != _5.month) break
    }
    return _1
  }, isc.A.getHeaderRowObject = function isc_MonthSchedule_getHeaderRowObject(_1) {
    var _2 = {};
    var _3 = _1.duplicate();
    for (var i = 0; i < 7; i++) {
      _2["day" + (i + 1)] = _3.getDate();
      _2["date" + (i + 1)] = _3.duplicate();
      this.incrementDate(_3, 1)
    }
    return _2
  }, isc.A.getCellDate = function isc_MonthSchedule_getCellDate(_1, _2) {
    if (_1 == null && _2 == null) {
      _1 = this.getEventRow();
      _2 = this.getEventColumn()
    }
    if (_1 < 0 || _2 < 0) return null;
    var _3 = this.body.fields.get(_2).$66b,
        _4 = this.getRecord(_1),
        _5 = _4["date" + _3];
    return _5
  }, isc.A.incrementDate = function isc_MonthSchedule_incrementDate(_1, _2) {
    var _3 = _1.getDate();
    _1.setDate(_3 + _2);
    if (_1.getDate() == (_3 + _2) - 1) {
      _1.setHours(_1.getHours() + 1);
      _1.setDate(_3 + _2)
    }
    return _1
  }, isc.A.getEventRowObject = function isc_MonthSchedule_getEventRowObject(_1, _2) {
    var _3 = {};
    var _4 = _1.duplicate();
    for (var i = 0; i < 7; i++) {
      var _6 = [];
      while (this.$53t < _2.length) {
        var _7 = _2[this.$53t];
        if (_7[this.creator.startDateField].getMonth() != _4.getMonth() || _7[this.creator.startDateField].getDate() != _4.getDate()) {
          break
        } else {
          _6.add(_7);
          this.$53t += 1
        }
      }
      _3["day" + (i + 1)] = _4.getDate();
      _3["date" + (i + 1)] = _4.duplicate();
      _3["event" + (i + 1)] = _6;
      this.incrementDate(_4, 1)
    }
    return _3
  }, isc.A.getEvents = function isc_MonthSchedule_getEvents(_1, _2) {
    var _3 = this.getFieldBody(_2);
    if (!_3 || _3 == this.frozenBody) return false;
    var _4 = this.getLocalFieldNum(_2);
    var _5 = this.getDayFromCol(_4);
    var _6 = this.fields.get(_4).$66b;
    var _7 = this.data[_1]["event" + _6];
    return _7
  }, isc.A.getEventCell = function isc_MonthSchedule_getEventCell(_1) {
    var _2 = this.data;
    for (var _3 = 0; _3 < this.fields.length; _3++) {
      var _4 = this.fields[_3].$66b,
          _5 = "event" + _4;
      for (var _6 = 0; _6 < _2.length; _6++) {
        var _7 = _2.get(_6)[_5];
        if (_7 != null && _7.contains(_1)) {
          return [_6, _3]
        }
      }
    }
  }, isc.A.getDayFromCol = function isc_MonthSchedule_getDayFromCol(_1) {
    var _2 = this.body.fields.get(_1).$654;
    return _2
  }, isc.A.cellDisabled = function isc_MonthSchedule_cellDisabled(_1, _2) {
    var _3 = this.getFieldBody(_2);
    if (!_3 || _3 == this.frozenBody) return false;
    var _4 = this.getLocalFieldNum(_2),
        _5 = this.getCellDate(_1, _4);
    return this.calendar.shouldDisableDate(_5, this)
  }, isc.A.refreshEvents = function isc_MonthSchedule_refreshEvents() {
    var _1 = this.calendar;
    if (!_1.hasData()) return;
    this.logDebug('refreshEvents: month', 'calendar');
    this.setData(this.getDayArray());
    if (_1.eventsRendered && isc.isA.Function(_1.eventsRendered)) _1.eventsRendered()
  }, isc.A.rowIsHeader = function isc_MonthSchedule_rowIsHeader(_1) {
    var _2 = this.calendar;
    if (!_2.showDayHeaders || (_2.showDayHeaders && _1 % 2 == 1)) return false;
    else return true
  }, isc.A.formatCellValue = function isc_MonthSchedule_formatCellValue(_1, _2, _3, _4) {
    var _5 = this.calendar,
        _6 = this.fields.get(_4).$66b,
        _7 = _2["event" + _6],
        _8 = _2["date" + _6],
        _9 = _8.getMonth() != _5.chosenDate.getMonth();
    if (this.rowIsHeader(_3)) {
      if (!_5.showOtherDays && _9) {
        return ""
      } else {
        return _5.getDayHeaderHTML(_8, _7, _5, _3, _4)
      }
    } else {
      if (!_5.showOtherDays && _9) {
        return ""
      } else {
        return _5.getDayBodyHTML(_8, _7, _5, _3, _4)
      }
    }
  }, isc.A.getRowHeight = function isc_MonthSchedule_getRowHeight(_1, _2) {
    var _3 = this.calendar,
        _4 = _3.showDayHeaders;
    if (this.rowIsHeader(_2)) {
      return this.dayHeaderHeight
    } else {
      var _5 = _3.getMinutesPerRow(this),
          _6 = _3.getRowsPerHour(this),
          _7 = _4 ? this.data.length / _6 : this.data.length,
          _8 = _4 ? this.body.getViewportHeight() - (this.dayHeaderHeight * _7) : this.body.getViewportHeight(),
          _9 = _4 ? this.minimumDayHeight - this.dayHeaderHeight : null;
      if (_8 / _7 <= _9) {
        return _9
      } else {
        var _10 = _8 % _7,
            _11 = 0,
            _12 = _4 ? (_2 - 1) / _6 : _2;
        if (_12 < _10) _11 = 1;
        return (Math.floor(_8 / _7) + _11)
      }
    }
  }, isc.A.getCellAlign = function isc_MonthSchedule_getCellAlign(_1, _2, _3) {
    if (this.rowIsHeader(_2)) return "right";
    else return "left"
  }, isc.A.getCellVAlign = function isc_MonthSchedule_getCellVAlign(_1, _2, _3) {
    if (!this.rowIsHeader(_2)) return "top";
    else return "center"
  }, isc.A.cellHoverHTML = function isc_MonthSchedule_cellHoverHTML(_1, _2, _3) {
    var _4 = this.fields.get(_3).$66b;
    var _5 = _1["date" + _4];
    var _6 = _1["event" + _4];
    if (!this.rowIsHeader(_2) && _6 != null) {
      var _7 = this.calendar;
      return _7.getMonthViewHoverHTML(_5, _6)
    }
  }, isc.A.getBaseStyle = function isc_MonthSchedule_getBaseStyle(_1, _2, _3) {
    var _4 = this.calendar,
        _5 = this.fields.get(_3).$66b;
    var _6;
    if (this.rowIsHeader(_2)) {
      if ((_2 == 0 && _1["day" + _5] > 7) || (_2 == this.data.length - 2 && _1["day" + _5] < 7)) {
        if (!_4.showOtherDays) return _4.otherDayBlankStyle;
        else _6 = _4.otherDayHeaderBaseStyle
      } else _6 = _4.dayHeaderBaseStyle
    } else {
      var _7 = this.cellDisabled(_2, _3),
          _8 = _4.showDayHeaders ? 1 : 0,
          _9 = this.data.length - 1;
      if ((_2 == _8 && this.data[_8]["day" + _5] > 7) || (_2 == _9 && this.data[_9]["day" + _5] < 7)) {
        if (!_4.showOtherDays) return _4.otherDayBlankStyle;
        else _6 = _7 ? _4.otherDayBodyBaseStyle + "Disabled" : _4.otherDayBodyBaseStyle
      } else _6 = _7 ? _4.dayBodyBaseStyle + "Disabled" : _4.dayBodyBaseStyle
    }
    return _6
  }, isc.A.cellClick = function isc_MonthSchedule_cellClick(_1, _2, _3) {
    var _4 = this.calendar,
        _5, _6, _7 = this.fields.get(_3).$66b,
        _8 = _1["date" + _7],
        _9 = _1["event" + _7],
        _10 = _4.chosenDate.getMonth() != _8.getMonth(),
        _11 = false;
    if (this.rowIsHeader(_2)) {
      if (!(!this.creator.showOtherDays && _10)) {
        _11 = _4.dayHeaderClick(_8, _9, _4, _2, _3)
      }
      if (_11) {
        if (_2 == 0 && _1["day" + _7] > 7) {
          if (_4.month == 0) {
            _5 = _4.year - 1;
            _6 = 11
          } else {
            _5 = _4.year;
            _6 = _4.month - 1
          }
        } else if (_2 == this.data.length - 2 && _1["day" + _7] < 7) {
          if (_4.month == 11) {
            _5 = _4.year + 1;
            _6 = 0
          } else {
            _5 = _4.year;
            _6 = _4.month + 1
          }
        } else {
          _5 = _4.year;
          _6 = _4.month
        }
        _4.dateChooser.dateClick(_5, _6, _1["day" + _7]);
        _4.selectTab(0)
      }
    } else {
      if (!this.cellDisabled(_2, _3) && !(!_4.showOtherDays && _10)) {
        _11 = _4.dayBodyClick(_8, _9, _4, _2, _3);
        if (_11 && _4.canCreateEvents) {
          var _12 = _4.getCellDate(_2, _3, this),
              _13 = _4.getCellDate(_2, _3 + 1, this);
          var _14 = _4.createEventObject(null, _12, _13);
          _4.showEventDialog(_14, true)
        }
      }
    }
  });
  isc.B._maxIndex = isc.C + 21;
  isc.ClassFactory.defineClass("TimelineView", "CalendarView");
  isc.TimelineView.changeDefaults("bodyProperties", {
    snapToCells: false,
    suppressVSnapOffset: true,
    suppressHSnapOffset: true,
    childrenSnapToGrid: false
  });
  isc.A = isc.TimelineView.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.canSort = false;
  isc.A.canResizeFields = false;
  isc.A.canAutoFitFields = false;
  isc.A.canReorderFields = false;
  isc.A.showHeaderContextMenu = false;
  isc.A.showAllRecords = true;
  isc.A.alternateRecordStyles = false;
  isc.A.showRollOver = true;
  isc.A.useCellRollOvers = true;
  isc.A.canSelectCells = true;
  isc.A.laneNameField = "lane";
  isc.A.columnWidth = 60;
  isc.A.laneHeight = 60;
  isc.A.labelColumnWidth = 75;
  isc.A.labelColumnBaseStyle = "labelColumn";
  isc.A.eventPageSize = 30;
  isc.A.trailIconSize = 16;
  isc.A.leadIconSize = 16;
  isc.A.scrollToToday = false;
  isc.A.lineImage = "[SKINIMG]Stretchbar/hsplit_over_stretch.gif";
  isc.A.trailingEndPointImage = "[SKINIMG]actions/prev.png";
  isc.A.leadingEndPointImage = "[SKINIMG]actions/next.png";
  isc.A.headerSpanHeight = 24;
  isc.A.headerProperties = {
    inherentWidth: false
  };
  isc.A.dragSelectCanvasDefaults = {
    _constructor: "Canvas",
    styleName: "calendarCellSelected",
    opacity: 60,
    width: 1,
    height: 1,
    disabled: true,
    autoDraw: false,
    resizeNow: function (_1) {
      var _2 = this.creator,
          _3 = _2.calendar,
          p = isc.addProperties({}, this.props, _1);
      if (p.top == null) {
        p.top = _2.getRowTop(_2.getLaneIndex(p.lane));
        if (p.sublane) p.top += p.sublane.top
      }
      if (p.height == null) {
        p.height = p.sublane ? p.sublane.height : _2.getLaneHeight(p.lane[_3.laneNameField])
      }
      var _5 = _2.getDateLeftOffset(p.startDate),
          _6 = _2.getDateLeftOffset(p.endDate) - _5;
      this.props = p;
      this.moveTo(_5, p.top);
      this.resizeTo(_6, p.height);
      if (!this.isDrawn()) this.draw();
      if (!this.isVisible()) {
        this.show()
      }
    }
  };
  isc.A.groupRowHeight = 30;
  isc.B.push(isc.A.initWidget = function isc_TimelineView_initWidget() {
    this.fields = [];
    var c = this.calendar;
    if (c.alternateLaneStyles) {
      this.alternateRecordStyles = c.alternateLaneStyles
    }
    if (c.canGroupLanes != null) {
      this.canGroupBy = c.canGroupLanes;
      this.groupByField = c.laneGroupByField;
      if (c.laneGroupStartOpen != null) this.groupStartOpen = c.laneGroupStartOpen
    }
    if (c.canReorderLanes) {
      this.canReorderRecords = c.canReorderLanes
    }
    this.firstDayOfWeek = this.creator.firstDayOfWeek;
    if (c.laneNameField) this.laneNameField = c.laneNameField;
    if (c.renderEventsOnDemand) this.renderEventsOnDemand = c.renderEventsOnDemand;
    if (c.startDate) this.startDate = c.startDate.duplicate();
    if (c.endDate) this.endDate = c.endDate.duplicate();
    if (c.labelColumnWidth && c.labelColumnWidth != this.labelColumnWidth) {
      this.labelColumnWidth = c.labelColumnWidth
    }
    if (c.eventDragGap != null) this.eventDragGap = c.eventDragGap;
    if (c.headerLevels) this.headerLevels = isc.shallowClone(c.headerLevels);
    this.$963 = this.headerHeight;
    this.cellHeight = this.laneHeight;
    var _2 = c.timelineGranularity,
        _3 = isc.DateUtil.getTimeUnitKey(_2);
    if (!this.startDate) {
      this.startDate = c.startDate = isc.DateUtil.getAbsoluteDate("-0" + _3, c.chosenDate)
    }
    if (!this.endDate) {
      this.endDate = c.endDate = isc.DateUtil.getAbsoluteDate("+" + c.defaultTimelineColumnSpan + _3, this.startDate)
    } else if (isc.Date.compareDates(this.startDate, this.endDate) == -1) {
      var s = this.startDate;
      this.startDate = c.startDate = this.endDate;
      this.endDate = c.endDate = s;
      this.logWarn("Timeline startDate is later than endDate - switching the values.")
    }
    this.Super("initWidget");
    this.rebuild(true);
    this.addAutoChild("eventDragTarget")
  }, isc.A.getDragSelectCanvas = function isc_TimelineView_getDragSelectCanvas(_1) {
    if (!this.body) return null;
    if (!this.dragSelectCanvas) {
      this.addAutoChild("dragSelectCanvas", {
        eventProxy: this.body
      });
      this.body.addChild(this.dragSelectCanvas)
    }
    return this.dragSelectCanvas
  }, isc.A.cellMouseDown = function isc_TimelineView_cellMouseDown(_1, _2, _3) {
    if (this.isLabelCol(_3)) {
      return true
    }
    var _4 = this.$131h,
        _5 = this.calendar;
    if (_5.shouldDisableDate(_4, this)) {
      return true
    }
    var _6 = this.body.getOffsetX(),
        _7 = this.getDateLeftOffset(_4);
    if (_5.backgroundMouseDown && _5.backgroundMouseDown(_4) == false) return;
    if (!_5.canCreateEvents) return true;
    this.clearSelection();
    var _8 = this.getDragSelectCanvas(),
        _9 = _5.addSnapGapsToDate(_4, this, 1),
        _10 = this.getLaneFromPoint(),
        _11 = this.getSublaneFromPoint();
    var p = {
      lane: _10,
      sublane: _11,
      startDate: _4,
      endDate: _9,
      top: null,
      height: null
    };
    _8.resizeNow(p);
    this.$53r = true;
    return false
  }, isc.A.cellOver = function isc_TimelineView_cellOver(_1, _2, _3) {
    _3 -= 1;
    if (this.$53r) {
      var _4 = this.getDragSelectCanvas(),
          _5 = _4.props,
          _6 = this.getDateFromPoint(),
          _7 = this.calendar.addSnapGapsToDate(_6, this, 1);
      _5.endDate = _7;
      _4.resizeNow(_5)
    }
  }, isc.A.cellMouseUp = function isc_TimelineView_cellMouseUp(_1, _2, _3) {
    if (!this.$53r) return true;
    this.$53r = false;
    var _4 = this.calendar,
        _5 = this.getDragSelectCanvas(),
        _6 = _5.props;
    var _7 = _6.startDate,
        _8 = _6.endDate;
    if (_4.backgroundClick) {
      if (_4.backgroundClick(_6.startDate, _6.endDate) == false) {
        this.clearSelection();
        return
      }
    }
    if (_4.backgroundMouseUp) {
      if (_4.backgroundMouseUp(_6.startDate, _6.endDate) == false) {
        this.clearSelection();
        return
      }
    }
    if (_4.shouldDisableDate(_8, this)) {
      this.clearSelection();
      return true
    }
    var _9 = _6.lane,
        _10 = _6.sublane;
    var _11 = _4.createEventObject(null, _7, _8, _9 && _9[_4.laneNameField], _10 && _10[_4.laneNameField]);
    _4.showEventDialog(_11, true)
  }, isc.A.clearSelection = function isc_TimelineView_clearSelection() {
    var _1 = this.getDragSelectCanvas();
    if (_1) _1.hide()
  }, isc.A.getCellDate = function isc_TimelineView_getCellDate(_1, _2) {
    if (!this.body) return null;
    var _3 = this.body.getField(_2);
    if (!_3 || !_3.date) return null;
    return _3.date
  }, isc.A.getCellEndDate = function isc_TimelineView_getCellEndDate(_1, _2) {
    if (!this.body) return null;
    var _3 = this.body.getField(_2);
    if (!_3 || !_3.endDate) return null;
    return _3.endDate
  }, isc.A.recordDrop = function isc_TimelineView_recordDrop(_1, _2, _3, _4) {
    this.Super("recordDrop", arguments);
    this.$116v();
    this.markForRedraw()
  }, isc.A.getFirstDateColumn = function isc_TimelineView_getFirstDateColumn() {
    return this.frozenBody ? this.frozenFields.length : 0
  }, isc.A.getCellValue = function isc_TimelineView_getCellValue(_1, _2, _3) {
    var _4 = this.getFirstDateColumn();
    if (_3 >= _4) return null;
    return this.Super("getCellValue", arguments)
  }, isc.A.rebuild = function isc_TimelineView_rebuild(_1) {
    if (this.$129c && this.$129c.length > 0) {
      this.$129c.setProperty("$128k", true);
      this.clearEvents()
    }
    var _2 = this.calcFields();
    if (this.isDrawn()) {
      this.setFields(_2)
    } else this.fields = _2;
    var _3 = this.lanes || this.creator.lanes || [];
    this.setLanes(_3.duplicate(), true);
    this.$730();
    if (_1) {
      this.$116v()
    } else {
      this.refreshEvents()
    }
  }, isc.A.setLanes = function isc_TimelineView_setLanes(_1, _2) {
    var _3 = this.calendar,
        _4 = _3.laneNameField;
    this.lanes = _1.duplicate();
    var _5 = this;
    _1.map(function (_12) {
      if (!_12[_4]) _12[_4] = _12.name;
      if (_12.sublanes) {
        var _6 = _5.getLaneHeight(_12),
            _7 = _12.sublanes.length,
            _8 = Math.floor(_6 / _7),
            _9 = 0;
        for (var j = 0; j < _7; j++) {
          var _11 = _12.sublanes[j];
          _11[_4] = _11.name;
          _11.top = _9;
          if (_11.height == null) _11.height = _8;
          _9 += _11.height
        }
        _12.height = _12.sublanes.getProperty("height").sum()
      } else {
        _12.height = _5.getLaneHeight(_12)
      }
    });
    this.setData(_1);
    if (!_2) this.$116v()
  }, isc.A.getLaneIndex = function isc_TimelineView_getLaneIndex(_1) {
    var _2 = isc.isAn.Object(_1) ? _1 : this.data.find("name", _1);
    if (!_2) _2 = this.data.find(this.creator.laneNameField, _1);
    var _3 = this.getRecordIndex(_2);
    return _3
  }, isc.A.getLane = function isc_TimelineView_getLane(_1) {
    var _2 = this.getLaneIndex(_1);
    if (_2 >= 0) return this.getRecord(_2)
  }, isc.A.getLaneFromPoint = function isc_TimelineView_getLaneFromPoint(_1, _2) {
    if (_2 == null) _2 = this.body.getOffsetY();
    var _3 = this.getEventRow(_2),
        _4 = this.getRecord(_3);
    return !this.isGroupNode(_4) ? _4 : null
  }, isc.A.getSublaneFromPoint = function isc_TimelineView_getSublaneFromPoint(_1, _2) {
    if (_2 == null) _2 = this.body.getOffsetY();
    var _3 = this.getEventRow(_2),
        _4 = this.getRecord(_3),
        _5 = _4 ? _4.sublanes : null;
    if (!_5) return null;
    var _6 = this.getRowTop(_3),
        _7 = _2 - _6,
        _8 = this.getLaneHeight(_4),
        _9 = _5.length,
        _10 = 0;
    for (var i = 0; i < _9; i++) {
      if (_10 + _5[i].height >= _7) {
        return _5[i]
      }
      _10 += _5[i].height
    }
    return null
  }, isc.A.$730 = function isc_TimelineView__scrubDateRange() {
    var _1 = this.creator.timelineGranularity;
    if (_1 == "month") {
      this.startDate.setDate(1)
    } else if (_1 == "week") {
      this.startDate = isc.DateUtil.getStartOf(this.startDate, "w", true)
    } else if (_1 == "day") {
      this.startDate.setHours(0);
      this.startDate.setMinutes(0);
      this.startDate.setSeconds(0);
      this.startDate.setMilliseconds(0)
    } else if (_1 == "hour") {
      this.startDate.setMinutes(0);
      this.startDate.setSeconds(0);
      this.startDate.setMilliseconds(0)
    } else if (_1 == "minute") {
      this.startDate.setSeconds(0);
      this.startDate.setMilliseconds(0)
    }
  }, isc.A.scrollTimelineTo = function isc_TimelineView_scrollTimelineTo(_1) {
    this.bodies[1].scrollTo(_1)
  }, isc.A.setLaneHeight = function isc_TimelineView_setLaneHeight(_1) {
    this.laneHeight = _1;
    this.setCellHeight(_1);
    this.refreshEvents()
  }, isc.A.getRowHeight = function isc_TimelineView_getRowHeight(_1, _2) {
    var _3 = null
    if (_1) {
      if (this.isGroupNode(_1)) _3 = this.groupRowHeight;
      else _3 = _1.height
    }
    return _3 || this.Super("getRowHeight", arguments)
  }, isc.A.setInnerColumnWidth = function isc_TimelineView_setInnerColumnWidth(_1) {
    this.columnWidth = _1;
    this.setFields(this.calcFields());
    this.refreshEvents()
  }, isc.A.setTimelineRange = function isc_TimelineView_setTimelineRange(_1, _2, _3, _4, _5, _6, _7) {
    var _8 = this.calendar,
        _9 = _4 || this.$116z || _8.defaultTimelineColumnSpan,
        _10 = false;
    _1 = _1 || this.startDate;
    this.startDate = _1.duplicate();
    _8.startDate = _1.duplicate();
    if (_2) {
      this.endDate = _2.duplicate()
    } else {
      var _11 = (_3 || _8.timelineGranularity).toLowerCase(),
          _12 = isc.DateUtil.getTimeUnitKey(_11),
          _13 = _6 && _6.length ? _6[_6.length - 1] : null;
      this.endDate = isc.DateUtil.getAbsoluteDate("+" + _9 + _12, this.startDate)
    }
    _8.endDate = this.endDate.duplicate();
    if (_3) _8.timelineGranularity = _3;
    if (_5) _8.timelineUnitsPerColumn = _5;
    if (_6) {
      _8.headerLevels = _6;
      _10 = true
    }
    if (_8.fetchMode && _8.fetchMode != "all") _10 = true;
    _8.dateChooser.setData(this.startDate);
    if (!_7) _8.setChosenDate(this.startDate, true);
    this.rebuild(_10)
  }, isc.A.addUnits = function isc_TimelineView_addUnits(_1, _2, _3) {
    _3 = _3 || this.calendar.timelineGranularity;
    if (_3 == "century") {
      _1.setFullYear(_1.getFullYear() + (_2 * 100))
    } else if (_3 == "decade") {
      _1.setFullYear(_1.getFullYear() + (_2 * 10))
    } else if (_3 == "year") {
      _1.setFullYear(_1.getFullYear() + _2)
    } else if (_3 == "quarter") {
      _1.setMonth(_1.getMonth() + (_2 * 3))
    } else if (_3 == "month") {
      _1.setMonth(_1.getMonth() + _2)
    } else if (_3 == "week") {
      _1.setDate(_1.getDate() + (_2 * 7))
    } else if (_3 == "day") {
      _1.setDate(_1.getDate() + _2)
    } else if (_3 == "hour") {
      _1.setHours(_1.getHours() + _2)
    } else if (_3 == "minute") {
      _1.setMinutes(_1.getMinutes() + _2)
    } else if (_3 == "second") {
      _1.setSeconds(_1.getSeconds() + _2)
    } else if (_3 == "millisecond") {
      _1.setMilliseconds(_1.getMilliseconds() + _2)
    }
    return _1
  }, isc.A.getColFromDate = function isc_TimelineView_getColFromDate(_1) {
    var _2 = this.frozenBody ? this.body.fields : this.getFields(),
        _3 = _1.getTime();
    for (var i = 0; i < _2.length; i++) {
      var _5 = _2[i];
      if (_5.date && _5.date.getTime() > _3) {
        return i - 1
      }
    }
    return null
  }, isc.A.calcFields = function isc_TimelineView_calcFields() {
    var _1 = [],
        _2 = this.creator;
    if (_2.laneFields) {
      var _3 = _2.laneFields;
      _3.setProperty("frozen", true);
      _3.setProperty("isLaneField", true);
      for (var i = 0; i < _3.length; i++) {
        if (_3[i].width == null) _3[i].width = this.labelColumnWidth;
        _1.add(_3[i])
      }
    } else {
      var _5 = {
        width: this.labelColumnWidth,
        name: "title",
        title: " ",
        showTitle: false,
        frozen: true,
        isLaneField: true
      };
      _1.add(_5)
    }
    if (!_2.headerLevels && !this.headerLevels) {
      _2.headerLevels = [{
        unit: _2.timelineGranularity
      }]
    }
    if (_2.headerLevels) {
      this.headerLevels = isc.shallowClone(_2.headerLevels)
    }
    if (this.headerLevels) {
      this.fieldHeaderLevel = this.headerLevels[this.headerLevels.length - 1];
      this.headerLevels.remove(this.fieldHeaderLevel);
      _2.timelineGranularity = this.fieldHeaderLevel.unit
    }
    this.adjustTimelineForHeaders();
    var _6 = this.startDate.duplicate(),
        _7 = this.endDate.duplicate(),
        _8 = _2.timelineUnitsPerColumn,
        _9 = 0,
        _10 = this.fieldHeaderLevel,
        _11 = _10 && _10.titles ? _10.titles : [];
    if (_10.headerWidth) this.columnWidth = _10.headerWidth;
    var _12 = _7.getTime();
    while (_6.getTime() <= _12) {
      var _13 = _6.duplicate(),
          _14 = _2.shouldShowDate(_6, this),
          _15 = null;
      if (_14) {
        var _16 = this.getInnerFieldTitle(_10, _9, _6);
        _15 = isc.addProperties({}, {
          name: "f" + _9,
          title: _16,
          width: _10.headerWidth || this.columnWidth,
          date: _13.duplicate(),
          canGroup: false,
          canSort: false
        }, this.getFieldProperties(_13))
      }
      _6 = this.addUnits(_6, _8);
      if (_14) {
        _15.endDate = _6.duplicate();
        _1.add(_15);
        _9++
      }
    }
    this.buildHeaderSpans(_1, this.headerLevels, this.startDate, this.endDate);
    this.$116z = _9 - 1;
    return _1
  }, isc.A.redraw = function isc_TimelineView_redraw() {
    this.Super("redraw", arguments);
    if (!this.animateFolders && this.$129z) {
      delete this.$129z;
      this.refreshVisibleEvents()
    }
  }, isc.A.toggleFolder = function isc_TimelineView_toggleFolder(_1) {
    this.Super("toggleFolder", arguments);
    if (!this.animateFolders) {
      this.$129z = true;
      this.markForRedraw()
    }
  }, isc.A.rowAnimationComplete = function isc_TimelineView_rowAnimationComplete(_1, _2) {
    this.Super("rowAnimationComplete", arguments);
    if (!this.$64v) this.refreshVisibleEvents()
  }, isc.A.adjustTimelineForHeaders = function isc_TimelineView_adjustTimelineForHeaders() {
    var _1 = this.calendar,
        _2 = this.fieldHeaderLevel ? this.fieldHeaderLevel.unit : _1.timelineGranularity,
        _3 = _1.startDate,
        _4 = _1.endDate;
    var _5 = isc.DateUtil.getTimeUnitKey(_2);
    _1.startDate = this.startDate = isc.DateUtil.getStartOf(_3, _5);
    _1.endDate = this.endDate = isc.DateUtil.getEndOf(_4, _5)
  }, isc.A.buildHeaderSpans = function isc_TimelineView_buildHeaderSpans(_1, _2, _3, _4) {
    var _5 = _3.duplicate(),
        c = this.creator,
        _7 = [],
        _8 = [];
    if (_2 && _2.length > 0) {
      _8 = this.getHeaderSpans(_3, _4, _2, 0, _1);
      this.headerHeight = this.$963 + (_2.length * this.headerSpanHeight)
    }
    if (_8 && _8.length > 0) this.headerSpans = _8
  }, isc.A.getHeaderSpans = function isc_TimelineView_getHeaderSpans(_1, _2, _3, _4, _5) {
    var _6 = _1.duplicate(),
        c = this.creator,
        _8 = _3[_4],
        _9 = _8.unit,
        _10 = _4 > 0 ? _3[_4 - 1].unit : _9,
        _11 = c.timelineUnitsPerColumn,
        _12 = _8.titles || [],
        _13 = [],
        _14 = 0;
    if (_4 > 0) {
      if (isc.DateUtil.compareTimeUnits(_9, _10) > 0) {
        isc.logWarn("The order of the specified HeaderLevels is incorrect - '" + _9 + "' is of a larger granularity than '" + _10 + "'")
      }
    }
    var _15 = isc.DateUtil;
    while (_6 <= _2) {
      _15.dateAdd(_6, "mn", 1, 1);
      var _16 = this.addUnits(_6.duplicate(), _11, _9);
      var _17 = {
        unit: _9,
        startDate: _6,
        endDate: _16
      };
      this.setSpanDates(_17, _6);
      _16 = _17.endDate;
      var _18 = this.getHeaderLevelTitle(_8, _14, _6, _16);
      _17.title = _18;
      _17.fields = [];
      for (var i = 0; i < _5.length; i++) {
        var _20 = _5[i];
        if (_20.isLaneField || _20.date < _17.startDate) continue;
        if (_20.date >= _17.endDate) break;
        _17.fields.add(_20.name)
      }
      if (_4 < _3.length - 1) {
        _17.spans = this.getHeaderSpans(_17.startDate, _17.endDate, _3, _4 + 1, _5);
        if (_17.spans && _17.spans.length > 0) _17.fields = null;
        if (_8.titles && _8.titles.length != _17.spans.length) {
          isc.logWarn("The titles array provided for the " + _8.unit + " levelHeader has a length mismatch: expected " + _17.spans.length + " but " + _8.titles.length + " are present.  Some titles  may be auto-generated according to TimeUnit.")
        }
      }
      _13.add(isc.clone(_17));
      _6 = _16.duplicate();
      _14++
    }
    return _13
  }, isc.A.getHeaderLevelTitle = function isc_TimelineView_getHeaderLevelTitle(_1, _2, _3, _4) {
    var _5 = _1.unit,
        _6 = _1.titles ? _1.titles[_2] : null;
    if (!_6) {
      if (_5 == "century" || _5 == "decade") {
        _6 = _3.getFullYear() + " - " + _4.getFullYear()
      } else if (_5 == "year") {
        _6 = _3.getFullYear()
      } else if (_5 == "quarter") {
        _6 = _3.getShortMonthName() + " - " + _4.getShortMonthName()
      } else if (_5 == "month") {
        _6 = _3.getShortMonthName()
      } else if (_5 == "week") {
        _6 = this.creator.weekPrefix + " " + _4.getWeek(this.firstDayOfWeek)
      } else if (_5 == "day") {
        _6 = _3.getShortDayName()
      } else {
        if (_5 == "hour") _6 = _3.getHours();
        if (_5 == "minute") _6 = _3.getMinutes();
        if (_5 == "second") _6 = _3.getSeconds();
        if (_5 == "millisecond") _6 = _3.getMilliseconds();
        if (_5 == "hour") _6 = _3.getHours()
      }
      if (isc.isA.Function(_1.titleFormatter)) {
        _6 = _1.titleFormatter(_1, _3, _4, _6, this.creator)
      }
    }
    return _6
  }, isc.A.setSpanDates = function isc_TimelineView_setSpanDates(_1, _2) {
    var _3 = isc.DateUtil.getTimeUnitKey(_1.unit);
    _1.startDate = isc.DateUtil.getStartOf(_2, _3, null, this.firstDayOfWeek);
    _1.endDate = isc.DateUtil.getEndOf(_1.startDate, _3, null, this.firstDayOfWeek)
  }, isc.A.getFieldProperties = function isc_TimelineView_getFieldProperties(_1) {
    return null
  }, isc.A.getInnerFieldTitle = function isc_TimelineView_getInnerFieldTitle(_1, _2, _3, _4) {
    var _5 = _1.unit,
        _6 = _1.titles ? _1.titles[_2] : null;
    if (!_6) {
      if (_5 == "year") {
        _6 = _3.getFullYear()
      } else if (_5 == "month") {
        _6 = _3.getShortMonthName()
      } else if (_5 == "week") {
        _6 = this.creator.weekPrefix + _3.getWeek(this.firstDayOfWeek)
      } else if (_5 == "day") {
        _6 = (_3.getMonth() + 1) + "/" + _3.getDate()
      } else {
        var _7 = _3.getMinutes().toString();
        if (_7.length == 1) _7 = "0" + _7;
        _6 = _3.getHours() + ":" + _7
      }
      if (isc.isA.Function(_1.titleFormatter)) {
        _6 = _1.titleFormatter(_1, _3, _4, _6, this.creator)
      }
    }
    return _6
  }, isc.A.draw = function isc_TimelineView_draw(_1, _2, _3, _4) {
    this.invokeSuper(isc.TimelineView, "draw", _1, _2, _3, _4);
    var _5 = this.creator.eventSnapGap;
    if (_5) {
      this.body.snapHGap = Math.round((_5 / 60) * this.columnWidth)
    } else {
      this.body.snapHGap = this.columnWidth
    }
    this.body.snapVGap = this.laneHeight;
    if (this.scrollToToday != false) {
      var _6 = new Date();
      _6.setDate(_6.getDate() - this.scrollToToday);
      var _7 = this.creator.getDayDiff(this.startDate, _6);
      var _8 = _7 * this.columnWidth;
      this.bodies[1].scrollTo(_8, 0)
    }
    this.logDebug('draw', 'calendar');
    this.body.addChild(this.eventDragTarget);
    this.eventDragTarget.setView(this);
    this.refreshEvents()
  }, isc.A.getCellCSSText = function isc_TimelineView_getCellCSSText(_1, _2, _3) {
    var _4 = this.creator.$116j(this, _1, _2, _3);
    if (_4) return _4;
    return this.Super("getCellCSSText", arguments)
  }, isc.A.formatDateForDisplay = function isc_TimelineView_formatDateForDisplay(_1) {
    return _1.getShortMonthName() + " " + _1.getDate() + ", " + _1.getFullYear()
  }, isc.A.getLabelColCount = function isc_TimelineView_getLabelColCount() {
    if (this.creator.laneFields) {
      return this.creator.laneFields.length
    } else {
      return 1
    }
  }, isc.A.isLabelCol = function isc_TimelineView_isLabelCol(_1) {
    return this.getField(_1).frozen == true
  }, isc.A.showField = function isc_TimelineView_showField() {
    this.Super("showField", arguments);
    this.refreshEvents()
  }, isc.A.hideField = function isc_TimelineView_hideField() {
    this.Super("hideField", arguments);
    this.refreshEvents()
  }, isc.A.getCellStyle = function isc_TimelineView_getCellStyle(_1, _2, _3) {
    var _4 = this.getBaseStyle(_1, _2, _3);
    if (_3 == null) return _4;
    if (this.isLabelCol(_3)) return _4;
    if (this.alternateRecordStyles && _2 % 2 != 0) return _4 + "Dark";
    if (_3 > 0) {
      var _5 = _3 - (this.frozenBody ? this.frozenFields.length : 0);
      var _6 = this.getCellDate(_2, _5);
      if (_6 && this.calendar.shouldDisableDate(_6, this)) {
        return _4 + "Disabled"
      }
    }
    return _4
  }, isc.A.getBaseStyle = function isc_TimelineView_getBaseStyle(_1, _2, _3) {
    var _4 = this.calendar;
    if (this.isLabelCol(_3)) return this.labelColumnBaseStyle;
    else {
      var _5 = _4.getCellDate(_2, _3, this),
          _6 = _5 ? _4.getDateStyle(_5, _2, _3, this) : null;
      return _6 || this.baseStyle
    }
  }, isc.A.slideRange = function isc_TimelineView_slideRange(_1) {
    var c = this.creator,
        _3 = c.timelineGranularity.toLowerCase(),
        _4 = isc.DateUtil.getTimeUnitKey(_3),
        _5 = c.timelineUnitsPerColumn,
        _6 = this.startDate.duplicate(),
        _7 = this.endDate.duplicate(),
        _8 = _1 ? 1 : -1,
        _9 = c.columnsPerPage || (this.getFields().length - this.getLabelColCount());
    _6 = isc.DateUtil.dateAdd(_6, _4, _9, _8, false);
    _6 = isc.DateUtil.getStartOf(_6, _4, false);
    _7 = isc.DateUtil.dateAdd(_7, _4, _9, _8, false);
    _7 = isc.DateUtil.getEndOf(_7, _4, false);
    this.setTimelineRange(_6, _7, _3, null, _5, null, false)
  }, isc.A.nextOrPrev = function isc_TimelineView_nextOrPrev(_1) {
    this.slideRange(_1)
  }, isc.A.compareDates = function isc_TimelineView_compareDates(_1, _2, _3) {
    if (_1.getFullYear() < _2.getFullYear()) {
      return 1
    } else if (_1.getFullYear() > _2.getFullYear()) {
      return -1
    }
    if (_1.getMonth() < _2.getMonth()) {
      return 1
    } else if (_1.getMonth() > _2.getMonth()) {
      return -1
    }
    if (_1.getDate() < _2.getDate()) {
      return 1
    } else if (_1.getDate() > _2.getDate()) {
      return -1
    }
    return 0
  }, isc.A.getDateFromPoint = function isc_TimelineView_getDateFromPoint(_1, _2, _3, _4) {
    var _5 = this.calendar;
    if (_1 == null && _2 == null) {
      _1 = this.body.getOffsetX();
      _2 = this.body.getOffsetY()
    }
    if (_1 < 0 || _2 < 0) return null;
    var _6 = this.body.getEventColumn(_1);
    if (_6 == -2) _6 = this.body.fields.length - 1;
    if (_6 == -1) return null;
    if (_4 == null) _4 = true;
    if (_4) {
      var r = _1 % this.creator.eventSnapGap;
      if (r) _1 -= r
    }
    var _8 = this.body.fields[_6].date,
        _9 = this.body.getColumnLeft(_6),
        _10 = _1 - _9,
        _11 = Math.floor(_10 / _5.eventSnapGap);
    if (_11) _8 = _5.addSnapGapsToDate(_8.duplicate(), this, _11);
    return _8
  }, isc.A.$731 = function isc_TimelineView__getMinsInACell() {
    var _1 = this.creator.timelineUnitsPerColumn;
    var _2 = this.creator.timelineGranularity;
    var _3 = 24 * 60;
    var _4;
    var _5 = 0;
    if (_2 == "month") {
      _4 = _1 * (_3 * 30)
    } else if (_2 == "week") {
      _4 = _1 * (_3 * 7)
    } else if (_2 == "day") {
      _4 = _1 * _3
    } else if (_2 == "hour") {
      _4 = _1 * 60
    } else if (_2 == "minute") {
      _4 = _1
    }
    return _4
  }, isc.A.$732 = function isc_TimelineView__getEventBreadth(_1) {
    var _2 = this.calendar,
        _3 = _2.getEventStartDate(_1),
        _4 = _2.getEventEndDate(_1),
        _5 = _2.getVisibleStartDate(this).getTime(),
        _6 = _2.getVisibleEndDate(this).getTime();
    if (_3.getTime() < _5) _3.setTime(_5);
    if (_4.getTime() >= _6) {
      _4.setTime(_6 - 1)
    }
    var _7 = this.getDateLeftOffset(_3),
        _8 = this.getDateRightOffset(_4),
        _9 = _8 - _7;
    return _9
  }, isc.A.getDateRightOffset = function isc_TimelineView_getDateRightOffset(_1) {
    return this.getDateLeftOffset(_1, true)
  }, isc.A.getDateLeftOffset = function isc_TimelineView_getDateLeftOffset(_1, _2) {
    if (!_1) return 0;
    var _3 = _1.duplicate(),
        _4 = this.calendar.getVisibleStartDate(this),
        _5 = this.calendar.getVisibleEndDate(this);
    if (_3.getTime() <= _4.getTime()) {
      _3.setTime(_4.getTime() + 1)
    }
    if (_3.getTime() >= _5.getTime()) {
      _3.setTime(_5.getTime() - 1)
    }
    var _6 = this.calendar,
        _7 = this.body.fields,
        _8 = _7.getLength(),
        _9 = _3.getTime(),
        _10 = Math.floor(_9 / 60000);
    for (var i = 0; i < _8; i++) {
      var _12 = _7[i];
      if (!this.fieldIsVisible(_12)) continue;
      var _13 = _12.date.getTime(),
          _14 = _12.endDate.getTime(),
          _15 = Math.floor(_12.date.getTime() / 60000),
          _16 = Math.floor(_12.endDate.getTime() / 60000);
      if (_10 < _16) {
        if (_10 >= _15) {
          var _17 = this.body.getColumnLeft(i),
              _18 = (_9 - _13),
              _19 = _10 - _15,
              _20 = _6.getSnapGapMinutes(this),
              _21 = !_20 ? 1 : _2 ? Math.round(_19 / _20) : Math.floor(_19 / _20),
              _22 = _17 + (_10 == _15 ? 0 : (_21 * _6.eventSnapGap));
          return _22
        } else {
          return this.body.getColumnLeft(i)
        }
      }
    }
    return -1
  }, isc.A.getEventLeft = function isc_TimelineView_getEventLeft(_1) {
    return this.getDateLeftOffset(this.calendar.getEventStartDate(_1))
  }, isc.A.getEventRight = function isc_TimelineView_getEventRight(_1) {
    return this.getDateRightOffset(this.calendar.getEventEndDate(_1))
  }, isc.A.getLaneHeight = function isc_TimelineView_getLaneHeight(_1) {
    _1 = this.getLane(_1);
    if (isc.isA.Number(_1)) _1 = this.getRecord(_1);
    else if (isc.isA.String(_1)) _1 = this.getLane(_1);
    return _1 && _1.height || this.cellHeight
  }, isc.A.getSublaneHeight = function isc_TimelineView_getSublaneHeight(_1, _2) {
    if (!isc.isAn.Object(_1)) {
      if (!_2 || !_2.sublanes) return null;
      if (isc.isA.Number(_1)) _1 = _2.sublanes[_1];
      else if (isc.isA.String(_1)) {
        _1 = _2.sublanes.find(this.calendar.laneNameField, _1)
      }
    }
    return _1 ? _1.height : null
  }, isc.A.addLeadingAndTrailingLines = function isc_TimelineView_addLeadingAndTrailingLines(_1) {
    var _2, _3, _4, _5;
    if (_1.$645) {
      _2 = _1.$645[0];
      _3 = _1.$645[1];
      _4 = _1.$645[2];
      _5 = _1.$645[3]
    } else {
      _2 = this.$65r();
      _3 = this.$65s(_1, "lead");
      _4 = this.$65r();
      _5 = this.$65s(_1, "trail")
    }
    var _6 = this.$65t(_3, _2);
    var _7 = this.$65t(_5, _4);
    if (!_1.$645) {
      this.body.addChild(_2);
      this.body.addChild(_3);
      this.body.addChild(_4);
      this.body.addChild(_5);
      _1.$645 = [_2, _3, _4, _5]
    }
  }, isc.A.$65t = function isc_TimelineView__positionIcon(_1, _2) {
    var _3 = this.calendar,
        _4 = _1.eventCanvas,
        _5 = _4.event,
        _6 = _1.type,
        _7 = this.columnWidth,
        _8 = _4.getVisibleHeight(),
        _9 = _4.getTop(),
        _10 = _4.getLeft();
    var _11, _12, _13 = true;
    if (_6 == "trail") {
      if (this.compareDates(_5[_3.trailingDateField], this.endDate) < 0) {
        _11 = _3.getDayDiff(this.endDate, _5[_3.startDateField]);
        if (_11 < 1) _11 = 1;
        _12 = _11 * _7;
        _13 = false
      } else {
        _11 = _3.getDayDiff(_5[_3.trailingDateField], _5[_3.startDateField]);
        _12 = (_11 * _7) - (Math.round(_7 / 2))
      }
    } else {
      if (this.compareDates(this.startDate, _5[_3.leadingDateField]) < 0) {
        _11 = _3.getDayDiff(this.startDate, _3.getEventStartDate(_5));
        if (_11 < 1) _11 = 1;
        _12 = _11 * _7;
        _13 = false
      } else {
        _11 = _3.getDayDiff(_5[_3.leadingDateField], _3.getEventStartDate(_5));
        _12 = (_11 * _7) - (Math.round(_7 / 2))
      }
    }
    var _14 = (_6 == "trail" ? _10 + _7 : _10 - _12);
    _2.moveTo(_14, _9 + (Math.round(_8 / _3.getRowsPerHour(this))));
    _2.setWidth(_12);
    var _15 = 0;
    if (_5.$646 && _5.$646.slotNum > 0) {
      _15 = (_5.$646.slotNum - 1) * _8
    }
    var _16 = (_6 == "trail" ? this.trailIconSize : this.leadIconSize);
    var _17;
    if (_13 == false) _17 = -50;
    else if (_6 == "trail") _17 = _10 + _7 + _12 - Math.round(_16 / 2);
    else _17 = _10 - _12 - Math.round(_16 / 2);
    _1.moveTo(_17, _9 + Math.round(_8 / 2) - Math.round(_16 / 2));
    _1.$647 = Math.round(_8 / 2) - Math.round(_16 / 2) + _15;
    _1.$648 = Math.round(_7 / 2) - Math.round(_16 / 2);
    _1.$65f = _3.getDayDiff(_3.getEventStartDate(_5), this.startDate);
    return _13
  }, isc.A.$65s = function isc_TimelineView__makeIcon(_1, _2) {
    var _3 = (_2 == "trail" ? this.trailIconSize : this.leadIconSize);
    var _4 = isc.Img.create({
      eventCanvas: _1,
      type: _2,
      autoDraw: false,
      _redrawWithParent: false,
      src: (_2 == "trail" ? this.trailingEndPointImage : this.leadingEndPointImage),
      width: _3,
      height: _3,
      canDragReposition: (this.creator.canEditEvents == true),
      dragRepositionStart: function () {
        this.$1245.$8l = this.parentElement.getEventRow();
        this.$1245.$644 = this.parentElement.getEventColumn();
        this.parentElement.VSnapOrigin = this.$647;
        this.parentElement.HSnapOrigin = this.$648
      },
      dragRepositionStop: function () {
        var _5 = this.$65f,
            _6 = this.$1245.$644,
            _7 = this.parentElement.getEventColumn(),
            _8 = _7 - _6,
            _9 = this.eventCanvas.event,
            _10 = this.eventCanvas.calendar,
            _11 = this.type == "trail" ? _7 - _5 : _5 - _7;
        if (_11 < 1) return false;
        var _12 = {};
        var _13 = this.type == "trail" ? _10.trailingDateField : _10.leadingDateField;
        var _14 = _9[_13].duplicate();
        _14.setDate(_14.getDate() + _8);
        _12[_13] = _14;
        _10.updateEvent(_9, _10.getEventStartDate(_9), _10.getEventEndDate(_9), _9[_10.nameField], _9[_10.descriptionField], _12, true);
        return true
      }
    });
    return _4
  }, isc.A.$65r = function isc_TimelineView__makeLine() {
    var _1 = isc.Canvas.create({
      autoDraw: false,
      _redrawWithParent: false,
      height: 2,
      overflow: "hidden",
      styleName: "eventLine"
    });
    return _1
  }, isc.A.updateEventWindow = function isc_TimelineView_updateEventWindow(_1) {
    if (!this.body || !this.body.children) return;
    var _2 = this.calendar,
        _3 = _1[_2.laneNameField];
    var _4 = this.tagDataForOverlap(_2.data.getRange(0, _2.data.getLength()), _3);
    if (this.renderEventsOnDemand) {
      this.refreshVisibleEvents()
    } else {
      for (var i = 0; i < _4.length; i++) {
        var _6 = _4.get(i),
            _7 = this.getCurrentEventCanvas(this, _6);
        _7.event = _6;
        this.sizeEventCanvas(_7)
      }
    }
  }, isc.A.getEventCanvasConstructor = function isc_TimelineView_getEventCanvasConstructor(_1) {
    if (this.eventCanvasConstructor) return this.eventCanvasConstructor;
    if (this.calendar.eventCanvasConstructor == "EventWindow") return "TimelineWindow";
    return null
  });
  isc.B._maxIndex = isc.C + 63;
  isc.A = isc.DaySchedule;
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.B.push(isc.A.$126v = function isc_c_DaySchedule__getEventScaffolding(_1, _2, _3) {
    var _4 = _1.getMinutesPerRow(_2),
        _5 = (60 / _4) * 24,
        _6 = [],
        _7 = {
        label: "",
        day1: "",
        day2: "",
        day3: "",
        day4: "",
        day5: "",
        day6: "",
        day7: ""
        },
        _8 = _3 || new Date(),
        _9 = new Date(_8.getFullYear(), _8.getMonth(), _8.getDate(), 0, 0, 0, 0),
        _10 = [],
        _11 = _2.isDayView();
    if (_11) isc.DaySchedule.$129g(_1, _2, _9.duplicate());
    for (var i = 0; i < _5; i++) {
      var _13 = _9.duplicate();
      _6.add(isc.addProperties({}, _7, {
        time: _13
      }));
      _9 = isc.DateUtil.dateAdd(_9, "mn", _4, 1)
    }
    return _6
  }, isc.A.$129g = function isc_c_DaySchedule__getCellDates(_1, _2, _3) {
    var _4 = _1.getMinutesPerRow(_2),
        _5 = _3 || new Date(),
        _6 = new Date(_5.getFullYear(), _5.getMonth(), _5.getDate(), 0, 0, 0, 0),
        _7 = (60 / _4) * 24,
        _8 = _2.isDayView() ? 1 : 7,
        _9 = [];
    for (var i = 0; i <= _7; i++) {
      for (var j = 0; j < _8; j++) {
        if (!_9[i]) _9[i] = {};
        var _12 = isc.DateUtil.dateAdd(_6.duplicate(), "d", j * 1, 1);
        _9[i]["day" + (j + 1)] = _12
      }
      _6 = isc.DateUtil.dateAdd(_6, "mn", _4, 1)
    }
    _2.$129f = _9;
    return _9
  });
  isc.B._maxIndex = isc.C + 2;
  isc.ClassFactory.defineClass("Calendar", "Canvas", "DataBoundComponent");
  isc.A = isc.Calendar.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.defaultWidth = "100%";
  isc.A.defaultHeight = "100%";
  isc.A.year = new Date().getFullYear();
  isc.A.month = new Date().getMonth();
  isc.A.baseStyle = "calendar";
  isc.A.dayHeaderBaseStyle = "calMonthDayHeader";
  isc.A.dayBodyBaseStyle = "calMonthDayBody";
  isc.A.otherDayHeaderBaseStyle = "calMonthOtherDayHeader";
  isc.A.otherDayBodyBaseStyle = "calMonthOtherDayBody";
  isc.A.otherDayBlankStyle = "calMonthOtherDayBlank";
  isc.A.minimumDayHeight = 80;
  isc.A.selectedCellStyle = "calendarCellSelected";
  isc.A.eventStyleName = "eventWindow";
  isc.A.calMonthEventLinkStyle = "calMonthEventLink";
  isc.A.workdayBaseStyle = "calendarWorkday";
  isc.A.workdayStart = "9:00am";
  isc.A.workdayEnd = "5:00pm";
  isc.A.showWorkday = false;
  isc.A.workdays = [1, 2, 3, 4, 5];
  isc.A.scrollToWorkday = false;
  isc.A.minutesPerRow = 30;
  isc.A.nameField = "name";
  isc.A.descriptionField = "description";
  isc.A.startDateField = "startDate";
  isc.A.endDateField = "endDate";
  isc.A.durationField = "duration";
  isc.A.durationUnitField = "durationUnit";
  isc.A.laneNameField = "lane";
  isc.A.sublaneNameField = "sublane";
  isc.A.leadingDateField = "leadingDate";
  isc.A.trailingDateField = "trailingDate";
  isc.A.labelColumnWidth = 60;
  isc.A.eventWindowStyleField = "eventWindowStyle";
  isc.A.eventStyleNameField = "styleName";
  isc.A.canEditField = "canEdit";
  isc.A.canEditLaneField = "canEditLane";
  isc.A.canEditSublaneField = "canEditSublane";
  isc.A.canRemoveField = "canRemove";
  isc.A.canDragEventField = "canDrag";
  isc.A.canResizeEventField = "canResize";
  isc.A.durationUnitOptions = ["minute", "hour", "day", "week"];
  isc.A.laneEventPadding = 0;
  isc.A.eventDragGap = 10;
  isc.A.weekEventBorderOverlap = false;
  isc.A.eventSnapGap = 20;
  isc.A.showQuickEventDialog = true;
  isc.A.canCreateEvents = true;
  isc.A.canEditEvents = true;
  isc.A.canRemoveEvents = true;
  isc.A.canDragEvents = true;
  isc.A.canResizeEvents = true;
  isc.A.showDateChooser = false;
  isc.A.disableWeekends = true;
  isc.A.showWeekends = true;
  isc.A.showDayHeaders = true;
  isc.A.showOtherDays = true;
  isc.A.eventAutoArrange = true;
  isc.A.eventOverlap = true;
  isc.A.eventOverlapPercent = 10;
  isc.A.alternateLaneFrequency = 1;
  isc.A.showTimelineView = false;
  isc.A.renderEventsOnDemand = true;
  isc.A.timelineGranularity = "day";
  isc.A.timelineUnitsPerColumn = 1;
  isc.A.canResizeTimelineEvents = false;
  isc.A.defaultTimelineColumnSpan = 20;
  isc.A.weekPrefix = "Week";
  isc.A.laneGroupStartOpen = "first";
  isc.A.showEventDescriptions = true;
  isc.A.showEventHeaders = true;
  isc.A.eventHeaderWrap = true;
  isc.A.eventHeaderHeight = 12;
  isc.A.allowEventOverlap = true;
  isc.A.sizeEventsToGrid = true;
  isc.A.dayViewTitle = "Day";
  isc.A.weekViewTitle = "Week";
  isc.A.monthViewTitle = "Month";
  isc.A.timelineViewTitle = "Timeline";
  isc.A.eventNameFieldTitle = "Event Name";
  isc.A.eventStartDateFieldTitle = "From";
  isc.A.eventEndDateFieldTitle = "To";
  isc.A.eventDescriptionFieldTitle = "Description";
  isc.A.eventLaneFieldTitle = "Lane";
  isc.A.eventSublaneFieldTitle = "Sublane";
  isc.A.eventDurationFieldTitle = "Duration";
  isc.A.eventDurationUnitFieldTitle = "&nbsp";
  isc.A.saveButtonTitle = "Save Event";
  isc.A.detailsButtonTitle = "Edit Details";
  isc.A.cancelButtonTitle = "Cancel";
  isc.A.previousButtonHoverText = "Previous";
  isc.A.nextButtonHoverText = "Next";
  isc.A.addEventButtonHoverText = "Add an event";
  isc.A.datePickerHoverText = "Choose a date";
  isc.A.invalidDateMessage = "From must be before To";
  isc.A.dayViewConstructor = "DaySchedule";
  isc.A.weekViewConstructor = "WeekSchedule";
  isc.A.monthViewConstructor = "MonthSchedule";
  isc.A.timelineViewConstructor = "TimelineView";
  isc.A.mainViewDefaults = {
    _constructor: isc.TabSet,
    defaultWidth: "80%",
    defaultHeight: "100%",
    tabBarAlign: "right",
    selectedTab: 1
  };
  isc.A.dateChooserConstructor = "DateChooser";
  isc.A.eventDialogConstructor = "Window";
  isc.A.eventDialogDefaults = {
    showHeaderIcon: false,
    showMinimizeButton: false,
    showMaximumButton: false,
    canDragReposition: true,
    overflow: "visible",
    bodyProperties: {
      overflow: "visible"
    },
    keepInParentRect: true,
    maxWidth: 400,
    height: 100
  };
  isc.A.eventEditorLayoutConstructor = "Window";
  isc.A.eventEditorLayoutDefaults = {
    showHeaderIcon: false,
    showShadow: false,
    showMinimizeButton: false,
    showMaximumButton: false,
    canDragReposition: false
  };
  isc.A.eventEditorConstructor = "DynamicForm";
  isc.A.eventEditorDefaults = {
    padding: 4,
    numCols: 5,
    colWidths: [80, 60, 90, "*", "*"],
    showInlineErrors: false,
    width: 460,
    titleWidth: 80,
    wrapItemTitles: false
  };
  isc.A.addEventButtonConstructor = "ImgButton";
  isc.A.addEventButtonDefaults = {
    title: "",
    src: "[SKINIMG]actions/add.png",
    showRollOver: false,
    showDown: false,
    showFocused: false,
    width: 16,
    height: 16
  };
  isc.A.datePickerButtonConstructor = "ImgButton";
  isc.A.datePickerButtonDefaults = {
    title: "",
    src: "[SKIN]/controls/date_control.gif",
    width: 16,
    height: 16,
    showRollOver: false,
    showFocused: false
  };
  isc.A.showControlsBar = true;
  isc.A.controlsBarConstructor = "HLayout";
  isc.A.controlsBarDefaults = {
    defaultLayoutAlign: "center",
    height: 25,
    membersMargin: 5
  };
  isc.A.previousButtonConstructor = "ImgButton";
  isc.A.previousButtonDefaults = {
    title: "",
    src: "[SKINIMG]actions/back.png",
    showFocused: false,
    width: 16,
    height: 16,
    click: function () {
      this.creator.previous()
    },
    showRollOver: false,
    showDown: false
  };
  isc.A.nextButtonConstructor = "ImgButton";
  isc.A.nextButtonDefaults = {
    title: "",
    src: "[SKINIMG]actions/forward.png",
    showFocused: false,
    width: 16,
    height: 16,
    click: function () {
      this.creator.next()
    },
    showRollOver: false,
    showDown: false
  };
  isc.A.dateLabelConstructor = "Label";
  isc.A.dateLabelDefaults = {
    wrap: false,
    width: 5,
    contents: "-"
  };
  isc.A.DAY = "day";
  isc.A.WEEK = "week";
  isc.A.MONTH = "month";
  isc.A.TIMELINE = "timeline";
  isc.A.rowHeight = isc.ListGrid.getInstanceProperty("cellHeight");
  isc.A.zeroLengthEventSize = 2;
  isc.A.$1290 = "mn";
  isc.A.dateFormatter = null;
  isc.A.timeFormatter = "toShortPaddedTime";
  isc.A.showDragHovers = false;
  isc.A.showCellHovers = false;
  isc.A.eventCanvasConstructor = "EventCanvas";
  isc.A.eventCanvasContextMenuConstructor = "Menu";
  isc.A.eventCanvasContextMenuStyle = "eventWindowContextMenu";
  isc.A.eventCanvasContextMenuDefaults = {};
  isc.A.useEventCanvasRolloverControls = true;
  isc.A.eventCanvasButtonLayoutConstructor = "HLayout";
  isc.A.eventCanvasButtonLayoutDefaults = {
    width: 1,
    height: 1,
    overflow: "visible",
    snapTo: "TR",
    membersMargin: 1,
    layoutTopMargin: 1,
    layoutRightMargin: 3,
    mouseOver: function () {
      return isc.EH.STOP_BUBBLING
    }
  };
  isc.A.eventCanvasCloseButtonConstructor = "ImgButton";
  isc.A.eventCanvasCloseButtonDefaults = {
    width: 11,
    height: 10,
    showDown: false,
    showRollOver: true,
    layoutAlign: "center",
    src: "[SKIN]/headerIcons/close.png",
    styleName: "eventCanvasCloseButton",
    click: function () {
      var _1 = this.eventCanvas;
      if (this.creator.eventRemoveClick(_1.event, _1.calendarView.viewName) != false) {
        this.creator.removeEvent(_1.event)
      }
      return false
    }
  };
  isc.A.eventCanvasContextButtonConstructor = "ImgButton";
  isc.A.eventCanvasContextButtonDefaults = {
    width: 11,
    height: 10,
    showDown: false,
    showRollOver: true,
    layoutAlign: "left",
    src: "[SKIN]/headerIcons/arrow_down.png",
    click: function () {
      this.creator.showEventCanvasContextMenu(this.eventCanvas);
      return false
    }
  };
  isc.A.eventCanvasVResizerConstructor = "Img";
  isc.A.eventCanvasVResizerDefaults = {
    width: 12,
    height: 6,
    overflow: "hidden",
    src: "[SKIN]/Window/v_resizer.png",
    canDragResize: true
  };
  isc.A.eventCanvasHResizerConstructor = "Img";
  isc.A.eventCanvasHResizerDefaults = {
    width: 6,
    height: 10,
    overflow: "hidden",
    src: "[SKIN]/Window/h_resizer.png",
    canDragResize: true
  };
  isc.A.zoneStyleName = "zoneCanvas";
  isc.A.zoneCanvasConstructor = "ZoneCanvas";
  isc.A.indicatorStyleName = "indicatorCanvas";
  isc.A.indicatorCanvasConstructor = "IndicatorCanvas";
  isc.A.eventsOverlapGridLines = true;
  isc.A.$81i = /^\d{4}.\d\d?.\d\d?$/;
  isc.A.$81j = /^\d\d?.\d\d.\d{4}?$/;
  isc.A.newEventEditorWindowTitle = "New Event";
  isc.B.push(isc.A.getMinutesPerRow = function isc_Calendar_getMinutesPerRow(_1) {
    _1 = _1 || this.getSelectedView();
    return this.minutesPerRow
  }, isc.A.getMinutesPerCol = function isc_Calendar_getMinutesPerCol(_1) {
    return isc.DateUtil.convertPeriodUnit(1, this.timelineGranularity, "mn")
  }, isc.A.getSnapGapMinutes = function isc_Calendar_getSnapGapMinutes(_1, _2, _3) {
    _1 = _1 || this.getSelectedView();
    if (_2 == null) _2 = 0;
    if (_3 == null) _3 = 0;
    var _4 = _1.isTimelineView(),
        _5 = _4 ? _1.body.getColumnWidth(_3) : _1.getRowHeight(_2),
        _6 = _4 ? this.getMinutesPerCol(_1) : this.getMinutesPerRow(_1),
        _7 = Math.floor(_6 / (_5 / this.eventSnapGap));
    return _7
  }, isc.A.addSnapGapsToDate = function isc_Calendar_addSnapGapsToDate(_1, _2, _3) {
    if (!_1) return null;
    _2 = _2 || this.getSelectedView();
    if (!_3) _3 = 1;
    var _4 = this.getSnapGapMinutes(_2);
    var _5 = _1.duplicate();
    _5.setMinutes(_5.getMinutes() + (_4 * _3));
    return _5
  }, isc.A.getRowsPerHour = function isc_Calendar_getRowsPerHour(_1) {
    return Math.floor(60 / this.getMinutesPerRow())
  }, isc.A.getRowFromDate = function isc_Calendar_getRowFromDate(_1, _2) {
    var _3 = this.getMinutesPerRow(_1),
        _4 = this.getRowsPerHour(_1),
        _5 = Math.floor(_2.getMinutes() / _3),
        _6 = (_2.getMinutes() % _3 == 0 ? 0 : 1),
        _7 = (_2.getHours() * _4) + _5 + _6;
    return _7
  }, isc.A.getMinutePixels = function isc_Calendar_getMinutePixels(_1, _2, _3) {
    _3 = _3 || this.getSelectedView();
    if (_3.isTimelineView()) {
      var _4 = _2 != null ? _2 : _3.columnWidth;
      return Math.round((_4 / 60) * _1)
    } else if (_3.isDayView() || _3.isWeekView()) {
      var _5 = (_2 != null ? _2 : _3.getRowHeight(0)) * this.getRowsPerHour(_3);
      return Math.round((_5 / 60) * _1)
    }
  }, isc.A.scrollToTime = function isc_Calendar_scrollToTime(_1, _2) {
    _2 = _2 || this.getSelectedView();
    _1 = isc.Time.parseInput(_1);
    if (isc.isA.Date(_1)) {
      var _3 = this.getRowFromDate(_2, _1);
      var _4 = _2.getRowHeight(null, 0) * _3;
      _2.body.scrollTo(0, _4);
      _2.redraw()
    }
  }, isc.A.moveToEvent = function isc_Calendar_moveToEvent(_1, _2) {
    _2 = _2 || this.getSelectedView();
    this.setChosenDate(this.getEventStartDate(_1))
  }, isc.A.getDurationUnitMap = function isc_Calendar_getDurationUnitMap() {
    var _1 = this.durationUnitOptions,
        _2 = isc.DateUtil,
        _3 = {};
    for (var i = 0; i < _1.length; i++) {
      _3[_2.getTimeUnitKey(_1[i])] = _2.getTimeUnitTitle(_1[i]) + "s"
    }
    return _3
  }, isc.A.setHeaderLevels = function isc_Calendar_setHeaderLevels(_1) {
    this.headerLevels = _1;
    if (this.timelineView) this.timelineView.rebuild(true)
  }, isc.A.dateIsWeekend = function isc_Calendar_dateIsWeekend(_1) {
    return Date.getWeekendDays().contains(_1.getDay())
  }, isc.A.shouldDisableDate = function isc_Calendar_shouldDisableDate(_1, _2) {
    _2 = _2 || this.getSelectedView();
    if (this.disableWeekends && this.dateIsWeekend(_1)) {
      return true
    }
    return false
  }, isc.A.shouldShowDate = function isc_Calendar_shouldShowDate(_1, _2) {
    _2 = _2 || this.getSelectedView();
    if (_2.isTimelineView()) {
      if (!this.showWeekends && this.dateIsWeekend(_1)) return false
    }
    return true
  }, isc.A.shouldShowLane = function isc_Calendar_shouldShowLane(_1, _2) {
    _2 = _2 || this.getSelectedView();
    if (this.hideUnusedLanes && this.getLaneEvents(_1).length == 0) {
      return false
    }
    return true
  }, isc.A.shouldShowEvent = function isc_Calendar_shouldShowEvent(_1, _2) {
    return true
  }, isc.A.getWorkdayStart = function isc_Calendar_getWorkdayStart(_1, _2) {
    return this.workdayStart
  }, isc.A.getWorkdayEnd = function isc_Calendar_getWorkdayEnd(_1, _2) {
    return this.workdayEnd
  }, isc.A.getVisibleStartDate = function isc_Calendar_getVisibleStartDate(_1) {
    _1 = _1 || this.getSelectedView();
    if (!_1 || isc.isAn.emptyString(_1)) return null;
    return !_1.body ? _1.startDate : _1.getCellDate(0, 0)
  }, isc.A.getVisibleEndDate = function isc_Calendar_getVisibleEndDate(_1) {
    _1 = _1 || this.getSelectedView();
    if (!_1 || isc.isAn.emptyString(_1)) return null;
    if (!_1.body) return _1.endDate;
    var _2 = _1.getData().length - 1,
        _3 = _1.body.fields.length - 1;
    if (_1.getCellEndDate) return _1.getCellEndDate(_2, _3);
    return _1.getCellDate(_2, _3)
  }, isc.A.getPeriodStartDate = function isc_Calendar_getPeriodStartDate(_1) {
    _1 = _1 || this.getSelectedView();
    if (_1.isDayView()) {
      return this.chosenDateStart.duplicate()
    } else if (_1.isWeekView()) {
      return this.chosenWeekStart.duplicate()
    } else if (_1.isMonthView()) {
      return isc.DateUtil.getStartOf(this.chosenDate, isc.DateUtil.getTimeUnitKey("month"))
    } else if (_1.isTimelineView()) {
      return this.getVisibleStartDate(_1)
    }
  }, isc.A.getPeriodEndDate = function isc_Calendar_getPeriodEndDate(_1) {
    _1 = _1 || this.getSelectedView();
    if (_1.isDayView()) {
      return this.chosenDateEnd.duplicate()
    } else if (_1.isWeekView()) {
      return this.chosenWeekEnd.duplicate()
    } else if (_1.isMonthView()) {
      return isc.DateUtil.getEndOf(this.chosenDate, isc.DateUtil.getTimeUnitKey("month"))
    } else if (_1.isTimelineView()) {
      return this.getVisibleEndDate(_1)
    }
  }, isc.A.groupLanesBy = function isc_Calendar_groupLanesBy(_1) {
    if (this.timelineView) {
      this.timelineView.groupBy(_1)
    }
  }, isc.A.setLanes = function isc_Calendar_setLanes(_1) {
    if (!_1) {
      return
    }
    this.lanes = _1;
    if (this.timelineView) {
      this.timelineView.setLanes(this.lanes)
    }
    if (this.showDayLanes && this.dayView) {
      this.dayView.setLanes(this.lanes)
    }
  }, isc.A.addLane = function isc_Calendar_addLane(_1, _2) {
    var _3;
    if (this.timelineViewSelected()) {
      _3 = this.timelineView
    } else if (this.dayViewSelected() && this.showDayLanes) {
      _3 = this.dayView
    }
    if (!_3) {
      return
    }
    if (!this.lanes) this.lanes = [];
    if (_2 == null) _2 = this.lanes.length;
    this.lanes.add(_1, _2);
    _3.setLanes(this.lanes)
  }, isc.A.removeLane = function isc_Calendar_removeLane(_1) {
    var _2;
    if (this.timelineViewSelected()) _2 = this.timelineView;
    else if (this.dayViewSelected() && this.showDayLanes) _2 = this.dayView;
    if (!_2 || !this.lanes) return;
    if (isc.isA.String(_1)) _1 = this.lanes.find("name", _1);
    if (_1) {
      this.lanes.remove(_1);
      _2.setLanes(this.lanes)
    }
  }, isc.A.setShowDayLanes = function isc_Calendar_setShowDayLanes(_1) {
    if (this.showDayLanes == _1) return;
    this.showDayLanes = _1;
    if (this.dayView) {
      this.dayView.$123r = this.dayView.body.getScrollTop();
      this.dayView.rebuildFields();
      if (this.dayViewSelected()) {
        this.dayView.refreshEvents()
      } else {
        this.dayView.$123s = true
      }
    }
  }, isc.A.initWidget = function isc_Calendar_initWidget() {
    if (!this.chosenDate) this.chosenDate = new Date();
    this.year = this.chosenDate.getFullYear();
    this.month = this.chosenDate.getMonth();
    if (this.firstDayOfWeek == null) this.firstDayOfWeek = Number(isc.DateChooser.getInstanceProperty("firstDayOfWeek"));
    if (this.laneGroupByField && !isc.isAn.Array(this.laneGroupByField)) {
      this.laneGroupByField = [this.laneGroupByField]
    }
    if (this.timelineSnapGap != null) {
      this.snapGap = this.timelineSnapGap;
      delete this.timelineSnapGap
    }
    if (this.timelineStartDate != null) {
      this.startDate = this.timelineStartDate.duplicate();
      delete this.timelineStartDate
    }
    if (this.timelineEndDate != null) {
      this.endDate = this.timelineEndDate.duplicate();
      delete this.timelineEndDate
    }
    if (this.timelineLabelFields != null) {
      this.laneFields = this.timelineLabelFields;
      this.timelineLabelFields = null
    }
    if (this.eventTypeData != null) {
      this.lanes = isc.clone(this.eventTypeData);
      this.eventTypeData = null
    }
    if (this.eventTypeField != null) {
      this.laneNameField = this.eventTypeField;
      delete this.eventTypeField
    }
    if (this.showDescription != null) {
      this.showEventDescriptions = this.showDescription;
      delete this.showDescription
    }
    if (this.canEditEventType != null) {
      this.canEditLane = this.canEditEventType;
      delete this.canEditEventType
    }
    if (this.canDeleteEvents != null) {
      this.canRemoveEvents = this.canDeleteEvents;
      delete this.canDeleteEvents
    }
    if (this.eventWindowDefaults != null) {
      this.eventCanvasDefaults = isc.addProperties({}, this.eventWindowDefaults, this.eventCanvasDefaults);
      delete this.eventWindowDefaults
    }
    if (this.eventWindowProperties != null) {
      this.eventCanvasProperties = isc.addProperties({}, this.eventWindowProperties, this.eventCanvasProperties);
      delete this.eventWindowProperties
    }
    if (this.overlapSortSpecifiers && !isc.isAn.Array(this.overlapSortSpecifiers)) {
      this.overlapSortSpecifiers = [this.overlapSortSpecifiers]
    }
    if (!this.data) this.data = this.getDefaultData();
    this.previousButtonDefaults.prompt = this.previousButtonHoverText;
    this.nextButtonDefaults.prompt = this.nextButtonHoverText;
    this.datePickerButtonDefaults.prompt = this.datePickerHoverText;
    this.addEventButtonDefaults.prompt = this.addEventButtonHoverText;
    this.$129t(this.chosenDate);
    this.createChildren();
    this.$53a();
    this.setData(null);
    if (!this.initialCriteria && this.autoFetchData) {
      this.initialCriteria = this.getNewCriteria(null)
    }
    this.invokeSuper(isc.Calendar, "initWidget");
    this.createEditors()
  }, isc.A.autoDetectFieldNames = function isc_Calendar_autoDetectFieldNames() {
    this.dataSource = isc.DS.getDataSource(this.dataSource);
    var _1 = this.dataSource,
        _2 = isc.getValues(_1.getFields()),
        _3 = 1024000,
        _4 = null,
        _5;
    if (this.fieldIsMissing(this.nameField, _1)) {
      this.nameField = _1.getTitleField()
    }
    if (this.fieldIsMissing(this.descriptionField, _1)) {
      _2.sortByProperties(["length"], [false]);
      _4 = {
        length: 0
      };
      for (var i = 0; i < _2.length; i++) {
        _5 = _2.get(i);
        if (!_5.type || _5.type == "text" || _5.type == "string") {
          if (_5.length > 255 && _5.length < _3) {
            this.descriptionField = _5.name;
            break
          } else if (_5.length && _5.length < _3 && _5.length > _4.length) {
            _4 = _5
          } else if (!_5.length) {
            if (!_4) _4 = _5
          }
        }
      }
      if (_4 != null && this.fieldIsMissing(this.descriptionField, _1)) this.descriptionField = _4.name
    }
    if (this.fieldIsMissing(this.startDateField, _1)) {
      _4 = null;
      for (var i = 0; i < _2.length; i++) {
        _5 = _2.get(i);
        if ((_5.type == "date" || _5.type == "datetime")) {
          if (_5.name.toLowerCase().indexOf("start") >= 0 || _5.name.toLowerCase().indexOf("begin") >= 0) {
            this.startDateField = _5.name;
            break
          } else _4 = _5
        }
      }
      if (_4 != null && this.fieldIsMissing(this.startDateField, _1)) this.startDateField = _4.name
    }
    if (this.fieldIsMissing(this.endDateField, _1)) {
      _4 = null;
      for (var i = 0; i < _2.length; i++) {
        _5 = _2.get(i);
        if ((_5.type == "date" || _5.type == "datetime")) {
          if (_5.name.toLowerCase().indexOf("end") >= 0 || _5.name.toLowerCase().indexOf("stop") >= 0) {
            this.endDateField = _5.name;
            break
          } else if (_5.name != this.startDateField) _4 = _5
        }
      }
      if (_4 != null && this.fieldIsMissing(this.endDateField, _1)) this.endDateField = _4.name
    }
  }, isc.A.fieldIsMissing = function isc_Calendar_fieldIsMissing(_1, _2) {
    return (!_1 || _1 == "" || (_2 && !_2.getField(_1)))
  }, isc.A.getDefaultData = function isc_Calendar_getDefaultData() {
    return []
  }, isc.A.setData = function isc_Calendar_setData(_1) {
    if (this.data == _1) return;
    if (this.data) {
      this.ignore(this.data, "dataChanged");
      if (this.data.$31k && isc.isA.Function(this.data.destroy)) this.data.destroy()
    }
    if (_1) this.data = _1;
    if (!this.data) return;
    this.observe(this.data, "dataChanged", "observer.dataChanged()");
    if (this.hasData()) {
      this.dataChanged()
    }
  }, isc.A.getData = function isc_Calendar_getData() {
    return this.data
  }, isc.A.hasData = function isc_Calendar_hasData() {
    if (!this.data || (isc.ResultSet && isc.isA.ResultSet(this.data) && !this.data.lengthIsKnown())) {
      return false
    } else {
      return true
    }
  }, isc.A.dataChanged = function isc_Calendar_dataChanged() {
    if (this.destroying || this.destroyed) return;
    if (this.$53e) {
      this.logDebug('dataChanged, ignoring', 'calendar');
      this.$53e = false
    } else {
      this.logDebug('dataChanged, refreshing', 'calendar');
      this.refreshSelectedView()
    }
  }, isc.A.destroy = function isc_Calendar_destroy() {
    if (this.data) this.ignore(this.data, "dataChanged");
    if (this.controlsBar) this.controlsBar.destroy();
    if (this.controlsBarContainer) this.controlsBarContainer.destroy();
    if (this.dateChooser) this.dateChooser.destroy();
    if (this.eventCanvasButtonLayout) this.eventCanvasButtonLayout.destroy();
    if (this.mainLayout) this.mainLayout.destroy();
    this.Super("destroy", arguments)
  }, isc.A.refreshSelectedView = function isc_Calendar_refreshSelectedView() {
    if (this.dayViewSelected()) {
      this.dayView.refreshEvents();
      if (this.monthView) this.monthView.$123s = true
    } else if (this.weekViewSelected()) {
      this.weekView.refreshEvents();
      if (this.monthView) this.monthView.$123s = true
    } else if (this.monthViewSelected()) {
      this.monthView.refreshEvents()
    } else if (this.timelineViewSelected()) {
      this.timelineView.refreshEvents()
    }
  }, isc.A.getSelectedView = function isc_Calendar_getSelectedView() {
    if (this.dayViewSelected()) {
      return this.dayView
    } else if (this.weekViewSelected()) {
      return this.weekView
    } else if (this.monthViewSelected()) {
      return this.monthView
    } else if (this.timelineViewSelected()) {
      return this.timelineView
    }
  }, isc.A.getView = function isc_Calendar_getView(_1) {
    if (!_1) return this.getSelectedView();
    if (_1 == "day") return this.dayView;
    if (_1 == "week") return this.weekView;
    if (_1 == "month") return this.monthView;
    if (_1 == "timeline") return this.timelineView
  }, isc.A.setRowHeight = function isc_Calendar_setRowHeight(_1) {
    if (this.eventSnapGap == this.rowHeight) {
      this.eventSnapGap = _1
    }
    this.rowHeight = _1;
    if (this.dayView) {
      this.dayView.setCellHeight(this.rowHeight);
      this.dayView.refreshEvents();
      if (this.scrollToWorkday) this.dayView.scrollToWorkdayStart()
    }
    if (this.weekView) {
      this.weekView.setCellHeight(this.rowHeight);
      this.weekView.refreshEvents();
      if (this.scrollToWorkday) this.dayView.scrollToWorkdayStart()
    }
  }, isc.A.getCurrentViewName = function isc_Calendar_getCurrentViewName() {
    var _1 = this.getSelectedView();
    return _1 != null ? _1.viewName : null
  }, isc.A.setCurrentViewName = function isc_Calendar_setCurrentViewName(_1) {
    var _2 = this.mainView.tabs.findIndex("viewName", _1);
    if (_2 != null) this.selectTab(_2);
    return _1
  }, isc.A.getEventPKs = function isc_Calendar_getEventPKs(_1) {
    if (!this.$129u) {
      _1 = _1 || this.getDataSource();
      if (_1) {
        this.$129u = _1.getPrimaryKeyFieldNames()
      }
    }
    return this.$129u || []
  }, isc.A.getEventCanvasID = function isc_Calendar_getEventCanvasID(_1, _2) {
    if (!_2 || !_1 || !_1.$128i) return null;
    var _3 = this.getEventPKs();
    if (_3.length > 0) {
      var _4 = "event_";
      for (var i = 0; i < _3.length; i++) {
        _4 += _2[_3[i]];
        if (i == _3.length) break
      }
      return _1.$128i[_4]
    } else {
      return _2.$128i ? _2.$128i[_1.viewName] : null
    }
  }, isc.A.setEventCanvasID = function isc_Calendar_setEventCanvasID(_1, _2, _3) {
    if (!_1.$128i) _1.$128i = {};
    var _4 = this.getEventPKs().duplicate();
    if (_4.length > 0) {
      var _5 = "event_";
      for (var i = 0; i < _4.length; i++) {
        _5 += _2[_4[i]];
        if (i == _4.length) break
      }
      _1.$128i[_5] = _3
    } else {
      if (!_2.$128i) _2.$128i = {};
      _2.$128i[_1.viewName] = _3
    }
  }, isc.A.clearViewSelection = function isc_Calendar_clearViewSelection(_1) {
    if (_1) {
      if (_1.clearSelection) _1.clearSelection()
    } else {
      if (this.dayView) this.dayView.clearSelection();
      if (this.weekView) this.weekView.clearSelection();
      if (this.timelineView) this.timelineView.clearSelection()
    }
  }, isc.A.getDayDiff = function isc_Calendar_getDayDiff(_1, _2, _3) {
    return Math.abs(isc.Date.$1094(_1, _2, _3, false))
  }, isc.A.getEventStartCol = function isc_Calendar_getEventStartCol(_1, _2, _3) {
    var _4 = _3 || (_2 ? _2.calendarView : this.getSelectedView()),
        _5 = _2 || _4.getCurrentEventCanvas(_1),
        _6 = _4.getEventColumn(_5.getLeft() + 1);
    return _6
  }, isc.A.getEventEndCol = function isc_Calendar_getEventEndCol(_1, _2, _3) {
    var _4 = _4 || (_2 ? _2.calendarView : this.getSelectedView()),
        _5 = _2 || _4.getCurrentEventCanvas(_1),
        _6 = _4.getEventColumn(_5.getLeft() + _5.getVisibleWidth() + 1);
    return _6
  }, isc.A.getEventLeft = function isc_Calendar_getEventLeft(_1, _2) {
    _2 = _2 || this.getSelectedView();
    if (_2.getEventLeft) return _2.getEventLeft(_1);
    var _3 = _2.body.getColumnWidth(0),
        _4 = 0;
    if (_2.isWeekView()) {
      var _5 = this.getDayDiff(this.getEventStartDate(_1), this.chosenWeekStart, (this.showWeekends == false));
      _4 = (_5 * _3)
    } else if (this.showDayLanes) {
      var _6 = _2.completeFields.findIndex("name", _1[this.laneNameField]);
      if (_6) {
        _4 = _2.getColumnLeft(_6)
      }
    } else {
      var _6 = _2.getColFromDate(this.getEventStartDate(_1));
      if (_6) {
        _4 = _2.getColumnLeft(_6)
      }
    }
    if (this.logIsDebugEnabled("calendar")) {
      this.logDebug('calendar.getEventLeft() = ' + _4 + ' for:' + isc.Log.echoFull(_1), 'calendar')
    }
    return _4
  }, isc.A.getEventHeaderHTML = function isc_Calendar_getEventHeaderHTML(_1, _2) {
    var _3 = _2.isTimelineView() ? null : isc.Time.toTime(this.getEventStartDate(_1), this.timeFormatter, true),
        _4 = (_3 ? _3 + " " : "") + _1[this.nameField];
    return _4
  }, isc.A.getEventBodyHTML = function isc_Calendar_getEventBodyHTML(_1, _2) {
    return _1[this.descriptionField]
  }, isc.A.getEventLeadingDate = function isc_Calendar_getEventLeadingDate(_1, _2) {
    if (!_1) return null;
    var _3 = _1[this.leadingDateField];
    return _3 ? _3.duplicate() : null
  }, isc.A.getEventTrailingDate = function isc_Calendar_getEventTrailingDate(_1, _2) {
    if (!_1) return null;
    var _3 = _1[this.trailingDateField];
    return _3 ? _3.duplicate() : null
  }, isc.A.getEventStartDate = function isc_Calendar_getEventStartDate(_1, _2) {
    if (!_1 || !_1[this.startDateField]) return null;
    return _1[this.startDateField].duplicate()
  }, isc.A.getEventEndDate = function isc_Calendar_getEventEndDate(_1, _2) {
    if (!_1) return null;
    var _3 = this.getEventDuration(_1),
        _4 = _1[this.endDateField];
    if (_3 != null) {
      var _5 = this.getEventDurationUnit(_1) || "mn"
      _4 = this.getEventStartDate(_1);
      if (_5) _4 = isc.DateUtil.dateAdd(_4, _5, _3)
    }
    return _4 ? _4.duplicate() : null
  }, isc.A.isDurationEvent = function isc_Calendar_isDurationEvent(_1) {
    return (!_1[this.endDateField] && _1[this.durationField] != null)
  }, isc.A.getEventDuration = function isc_Calendar_getEventDuration(_1, _2) {
    return _1[this.durationField]
  }, isc.A.getEventDurationUnit = function isc_Calendar_getEventDurationUnit(_1, _2) {
    return _1[this.durationUnitField] || this.$1290
  }, isc.A.setShowWeekends = function isc_Calendar_setShowWeekends(_1) {
    this.showWeekends = _1;
    if (isc.isA.TabSet(this.mainView)) {
      var _2 = this.mainView.getSelectedTabNumber();
      this.mainView.removeTabs(this.mainView.tabs);
      if (this.dayView) this.dayView.destroy();
      if (this.weekView) this.weekView.destroy();
      if (this.monthView) this.monthView.destroy();
      var _3 = this.$653();
      this.mainView.addTabs(_3);
      this.mainView.selectTab(_2)
    } else {
      var _4 = this.children[0].members[1];
      if (!_4) return;
      var _5 = _4.members[1];
      var _6 = this.$653()[0].pane;
      _4.removeMember(_5);
      _5.destroy();
      _4.addMember(_6)
    }
    this.$53a();
    this.setDateLabel()
  }, isc.A.canEditEvent = function isc_Calendar_canEditEvent(_1) {
    if (!_1) return false;
    else if (_1[this.canEditField] != null) return _1[this.canEditField];
    else return this.canEditEvents
  }, isc.A.canDragEvent = function isc_Calendar_canDragEvent(_1) {
    if (!_1 || !this.canEditEvent(_1)) return false;
    if (_1[this.canDragEventField] != null) return _1[this.canDragEventField];
    else return this.canDragEvents
  }, isc.A.canResizeEvent = function isc_Calendar_canResizeEvent(_1) {
    if (!_1 || !this.canEditEvent(_1) || !this.canDragEvent(_1)) return false;
    else if (_1[this.canResizeEventField] != null) return _1[this.canResizeEventField];
    else return this.canResizeEvents
  }, isc.A.canRemoveEvent = function isc_Calendar_canRemoveEvent(_1) {
    if (!_1) return false;
    else if (_1[this.canRemoveField] != null) return _1[this.canRemoveField];
    else return this.canRemoveEvents && this.canEditEvent(_1)
  }, isc.A.getDateEditingStyle = function isc_Calendar_getDateEditingStyle() {
    if (!this.timelineView) {
      return "time"
    }
    var _1 = this.dateEditingStyle;
    if (!_1) {
      if (this.dataSource) _1 = this.getDataSource().getField(this.startDateField).type;
      if (!_1) {
        switch (this.timelineGranularity) {
        case "hour":
          _1 = "datetime";
          break;
        case "millisecond":
        case "second":
        case "minute":
          _1 = "time";
          break;
        default:
          _1 = "date";
          break
        }
      }
    }
    return _1
  }, isc.A.addLaneEvent = function isc_Calendar_addLaneEvent(_1, _2, _3, _4, _5, _6) {
    _6 = _6 || {};
    var _7 = this.createEventObject(null, _2, _3, _1, _6[this.sublaneNameField], _4, _5);
    this.addCalendarEvent(_7, _6)
  }, isc.A.createEventObject = function isc_Calendar_createEventObject(_1, _2, _3, _4, _5, _6, _7) {
    var _8 = isc.addProperties({}, _1);
    if (_2) _8[this.startDateField] = _2;
    if (_3) _8[this.endDateField] = _3;
    if (_4) _8[this.laneNameField] = _4;
    if (_5) _8[this.sublaneNameField] = _5;
    if (_6) _8[this.nameField] = _6;
    if (_7) _8[this.descriptionField] = _7;
    delete _8.eventLength;
    delete _8.__ref;
    return _8
  }, isc.A.addEvent = function isc_Calendar_addEvent(_1, _2, _3, _4, _5, _6, _7) {
    if (_7 == null) _7 = true;
    if (!isc.isAn.Object(_5)) _5 = {};
    var _8;
    if (isc.isA.Date(_1)) {
      _8 = this.createEventObject(null, _1, _2, _6 || _5[this.laneNameField], _5[this.sublaneNameField], _3, _4);
      isc.addProperties(_8, _5)
    } else if (isc.isAn.Object(_1)) {
      _8 = _1
    } else {
      isc.logWarn('addEvent error: startDate parameter must be either a Date or an event record (Object)');
      return
    }
    var _9 = this;
    if (_7) this.$53e = true;
    if (this.dataSource) {
      isc.DataSource.get(this.dataSource).addData(_8, function (_10, _11, _12) {
        _9.processSaveResponse(_10, _11, _12)
      }, {
        componentId: this.ID,
        willHandleError: true
      });
      return
    } else {
      this.$53e = true;
      this.data.add(_8);
      this.processSaveResponse({
        status: 0
      }, [_8], {
        operationType: "add"
      })
    }
  }, isc.A.addCalendarEvent = function isc_Calendar_addCalendarEvent(_1, _2, _3) {
    if (!_1) return;
    if (_3 == null) _3 = true;
    var _4 = this.getEventStartDate(_1);
    if (!isc.isA.Date(_4)) {
      isc.logWarn('addCalendarEvent: passed event has no start date');
      return
    }
    isc.addProperties(_1, _2);
    if (_3) this.$53e = true;
    if (this.dataSource) {
      var _5 = this;
      isc.DataSource.get(this.dataSource).addData(_1, function (_6, _7, _8) {
        _5.processSaveResponse(_6, _7, _8)
      }, {
        componentId: this.ID,
        willHandleError: true
      });
      return
    } else {
      this.$53e = true;
      this.data.add(_1);
      this.processSaveResponse({
        status: 0
      }, [_1], {
        operationType: "add"
      })
    }
  }, isc.A.removeEvent = function isc_Calendar_removeEvent(_1, _2) {
    if (_2 == null) _2 = true;
    var _3 = this.getEventStartDate(_1),
        _4 = this.getEventEndDate(_1);
    var _5 = this;
    var _6 = function () {
        if (_5.$53b(_3, _4)) {
          _5.dayView.removeEvent(_1)
        }
        if (_5.$53c(_3, _4)) {
          _5.weekView.removeEvent(_1)
        }
        if (_5.$53d(_3, _4)) {
          _5.monthView.refreshEvents()
        }
        if (_5.$131r(_3, _4)) {
          _5.timelineView.removeEvent(_1)
        }
        if (_5.eventAutoArrange) {
          if (_5.dayView) {
            if (_5.dayView.isSelectedView()) _5.dayView.refreshEvents();
            else _5.dayView.$123s = true
          }
          if (_5.weekView) {
            if (_5.weekView.isSelectedView()) _5.weekView.refreshEvents();
            else _5.weekView.$123s = true
          }
        }
        if (_5.eventRemoved) _5.eventRemoved(_1)
        };
    if (_2) this.$53e = true;
    if (this.dataSource) {
      isc.DataSource.get(this.dataSource).removeData(_1, _6, {
        componentId: this.ID,
        oldValues: _1
      });
      return
    } else {
      this.data.remove(_1);
      _6()
    }
  }, isc.A.updateEvent = function isc_Calendar_updateEvent(_1, _2, _3, _4, _5, _6, _7, _8, _9) {
    if (_7 == null) _7 = true;
    if (!isc.isAn.Object(_6)) _6 = {};
    var _10 = this.createEventObject(_1, _2, _3, _8 || _6[this.laneNameField], _9 || _6[this.sublaneNameField], _4, _5);
    this.updateCalendarEvent(_1, _10, _6, _7)
  }, isc.A.updateCalendarEvent = function isc_Calendar_updateCalendarEvent(_1, _2, _3, _4) {
    if (_4) this.$53e = true;
    _3 = _3 || {};
    if (this.dataSource) {
      var _5 = isc.DataSource.get(this.dataSource);
      var _6 = isc.addProperties({}, _2, _3);
      var _7 = this;
      _5.updateData(_6, function (_9, _10, _11) {
        _7.processSaveResponse(_9, _10, _11, _1)
      }, {
        oldValues: _1,
        componentId: this.ID,
        willHandleError: true
      });
      return
    } else {
      var _8 = isc.addProperties({}, _1);
      isc.addProperties(_1, _2, _3);
      this.processSaveResponse({
        status: 0
      }, [_1], {
        operationType: "update"
      }, _8)
    }
  }, isc.A.processSaveResponse = function isc_Calendar_processSaveResponse(_1, _2, _3, _4) {
    var _5 = isc.isAn.Array(_2) ? _2[0] : _2;
    if (!_5 || isc.isA.String(_5)) _5 = _4;
    var _6 = _3 ? _3.operationType : null,
        _7 = _6 == "update",
        _8 = _6 == "add",
        _9 = this.$1115,
        _10 = this.$1116,
        _11 = _7 && _4 ? this.getEventStartDate(_4) : null,
        _12 = _7 && _4 ? this.getEventEndDate(_4) : null,
        _13 = _7 && _4 ? _4[this.laneNameField] : null;
    delete this.$1115;
    delete this.$1116;
    if (_1 && _1.status < 0) {
      var _14 = _1 ? _1.errors : null;
      if (_9) {
        if (_14) this.eventDialog.items[0].setErrors(_14, true);
        this.displayEventDialog()
      } else if (_10) {
        this.eventEditorLayout.show();
        if (_14) this.eventEditor.setErrors(_14, true)
      }
      if (!_14) isc.RPCManager.$a0(_1, _3);
      return
    }
    var _15 = this.getEventStartDate(_5),
        _16 = this.getEventEndDate(_5),
        _17 = _5[this.laneNameField];
    _5.eventLength = (_16.getTime() - _15.getTime());
    if (_5[this.durationField] != null) {
      _5.isDuration = true;
      _5.isZeroDuration = _5[this.durationField] == 0
    }
    if (this.$53b(_15, _16) || (_7 && this.$53b(_11, _12))) {
      if (!this.dayViewSelected()) this.dayView.$123s = true;
      else {
        if (_7) {
          var _18 = this.dayView;
          if (this.showDayLanes) {
            _18.retagLaneEvents(_13);
            if (_17 != _13) _18.retagLaneEvents(_17)
          } else {
            _18.retagColumnEvents(0)
          }
        } else if (_8) {
          this.dayView.refreshEvents()
        }
      }
    }
    if (this.$53c(_15, _16)) {
      if (!this.weekViewSelected()) this.weekView.$123s = true;
      else {
        var _18 = this.weekView;
        if (_7) {
          _18.retagDayEvents(_11);
          if (isc.Date.compareLogicalDates(_11, _15) != 0) {
            _18.retagDayEvents(_15)
          }
        } else if (_8) {
          _18.addEvent(_5, true);
          _18.retagDayEvents(_15)
        }
      }
    }
    if (this.$53d(_15, _16)) {
      if (!this.monthViewSelected()) this.monthView.$123s = true;
      else this.monthView.refreshEvents()
    }
    if (this.$131r(_15, _16)) {
      if (!this.timelineViewSelected()) this.timelineView.$123s = true;
      else {
        var _18 = this.timelineView;
        if (_13 && _13 != _17) _18.retagLaneEvents(_13);
        _18.retagLaneEvents(_17);
        this.timelineView.refreshVisibleEvents()
      }
    }
    if (_7 && this.eventChanged) this.eventChanged(_5);
    if (_8 && this.eventAdded) this.eventAdded(_5)
  }, isc.A.eventsAreSame = function isc_Calendar_eventsAreSame(_1, _2) {
    if (this.dataSource) {
      var _3 = isc.DataSource.get(this.dataSource),
          _4 = this.getEventPKs(),
          _5 = true;
      for (var i = 0; i < _4.length; i++) {
        var _7 = _4[i];
        if (_1[_7] != _2[_7]) {
          _5 = false;
          break
        }
      }
      return _5
    } else {
      return (_1 === _2)
    }
  }, isc.A.getEventHoverHTML = function isc_Calendar_getEventHoverHTML(_1, _2, _3) {
    var _4 = this,
        _5 = null;
    var _6 = _4.getEventStartDate(_1),
        _7 = _6.toShortDate(this.dateFormatter, false),
        _8 = isc.Time.toTime(_6, this.timeFormatter, true),
        _9 = this.getEventEndDate(_1),
        _10 = _9.toShortDate(this.dateFormatter, false),
        _11 = isc.Time.toTime(_9, this.timeFormatter, true),
        _12 = _1[_4.nameField],
        _13 = _1[_4.descriptionField],
        _14 = isc.StringBuffer.create();
    if (_3.isTimelineView()) {
      if (_6.getDate() != _9.getDate()) {
        _14.append(_7, "&nbsp;", _8, "&nbsp;-&nbsp;", _10, "&nbsp;", _11)
      } else {
        _14.append(_7, "&nbsp;", _8, "&nbsp;-&nbsp;", _11)
      }
    } else {
      _14.append(_7, "&nbsp;", _8, "&nbsp;-&nbsp;", _11)
    }
    _14.append((_12 || _13 ? "</br></br>" : ""), (_12 ? _12 + "</br></br>" : ""), (_13 ? _13 : ""));
    var _5 = _14.release();
    return _5
  });
  isc.evalBoundary;
  isc.B.push(isc.A.getZoneHoverHTML = function isc_Calendar_getZoneHoverHTML(_1, _2, _3) {
    return null
  }, isc.A.getIndicatorHoverHTML = function isc_Calendar_getIndicatorHoverHTML(_1, _2, _3) {
    return this.getEventHoverHTML(_1, _2, _3)
  }, isc.A.getCellHoverHTML = function isc_Calendar_getCellHoverHTML(_1, _2, _3, _4) {
    if (!this.showCellHovers) return null;
    var _4 = _1.getDateFromPoint(),
        _5;
    if (_4) {
      var _6 = this.dateCellHoverStyle || this.hoverStyle;
      _5 = "<div style='" + _6 + "'>" + _4.toShortDateTime() + "</div>"
    }
    return _5
  }, isc.A.$131i = function isc_Calendar__mouseDateChanged(_1, _2) {
    if (this.showCellHovers) {
      if (isc.Hover.lastHoverTarget != _1) _1.startHover();
      else _1.updateHover()
    }
    if (this.mouseDateChanged) this.mouseDateChanged(_1, _2)
  }, isc.A.$53b = function isc_Calendar__shouldRefreshDay(_1, _2) {
    if (!this.dayView || !this.dayView.body) return false;
    var _3 = _1.getTime() < this.chosenDateEnd.getTime(),
        _4 = _2.getTime() > this.chosenDateStart.getTime();
    return (_3 && _4)
  }, isc.A.$53c = function isc_Calendar__shouldRefreshWeek(_1, _2) {
    if (!this.weekView || !this.weekView.body) return false;
    var _3 = _1.getTime() < this.chosenWeekEnd.getTime(),
        _4 = _2.getTime() > this.chosenWeekStart.getTime();
    return (_3 && _4)
  }, isc.A.$53d = function isc_Calendar__shouldRefreshMonth(_1, _2) {
    if (!this.monthView || !this.monthView.body) return false;
    var _3 = new Date(this.year, this.month, -7, 0, 0, 0).getTime(),
        _4 = new Date(this.year, this.month, 37, 23, 59, 59).getTime();
    return (_1.getTime() < _4 && _2.getTime() > _3)
  }, isc.A.$131r = function isc_Calendar__shouldRefreshTimeline(_1, _2) {
    if (!this.timelineView || !this.timelineView.body) return false;
    var _3 = _1.getTime() < this.timelineView.endDate.getTime(),
        _4 = _2.getTime() > this.timelineView.startDate.getTime();
    return (_3 && _4)
  }, isc.A.getEventCanvasConstructor = function isc_Calendar_getEventCanvasConstructor(_1, _2) {
    _2 = _2 || this.getSelectedView();
    return _2.getEventCanvasConstructor(_1) || this.eventCanvasConstructor
  }, isc.A.getEventCanvasStyle = function isc_Calendar_getEventCanvasStyle(_1, _2) {
    _2 = _2 || this.getSelectedView();
    var _3 = this.$131j(_1) || _2.getEventCanvasStyle(_1) || this.eventWindowStyle || this.eventStyleName;
    return _3
  }, isc.A.showEventCanvasContextMenu = function isc_Calendar_showEventCanvasContextMenu(_1) {
    if (!_1.shouldShowContextButton()) return false;
    var _2 = this.getEventCanvasMenuItems(_1);
    if (!this.eventCanvasContextMenu) this.addAutoChild("eventCanvasContextMenu");
    this.eventCanvasContextMenu.setData(_2);
    _1.contextMenu = this.eventCanvasContextMenu;
    _1.showContextMenu()
  }, isc.A.getEventCanvasMenuItems = function isc_Calendar_getEventCanvasMenuItems(_1, _2) {
    _2 = _2 || this.getSelectedView();
    return
  }, isc.A.hideEventCanvasRolloverControls = function isc_Calendar_hideEventCanvasRolloverControls(_1) {
    if (!_1.$129e) return;
    for (var i = 0; i < _1.$129e.length; i++) {
      _1.removeChild(_1.$129e[i])
    }
    _1.$129e = []
  }, isc.A.showEventCanvasRolloverControls = function isc_Calendar_showEventCanvasRolloverControls(_1) {
    if (_1.showRolloverControls == false) return false;
    var _2 = _1.calendarView,
        _3 = _1.shouldShowCloseButton(),
        _4 = _1.shouldShowContextButton(),
        _5 = [],
        _6;
    if (_3 || _4) {
      var _7;
      if (this.useEventCanvasRolloverControls) {
        if (!this.eventCanvasButtonLayout) this.addAutoChild("eventCanvasButtonLayout")
        _7 = this.eventCanvasButtonLayout;
        _7.members.removeAll()
      } else {
        _7 = this.createAutoChild("eventCanvasButtonLayout")
      }
      if (_4) {
        var _8 = this.getEventCanvasMenuItems(_1);
        if (_8) {
          _6 = this.getEventCanvasContextButton();
          if (_6) {
            _6.eventCanvas = _1;
            _7.addMember(_6);
            _6.show()
          }
        }
      } else if (this.useEventCanvasRolloverControls) {
        if (this.eventCanvasContextButton) this.eventCanvasContextButton.hide()
      }
      if (_3) {
        _6 = this.getEventCanvasCloseButton();
        if (_6) {
          _6.eventCanvas = _1;
          _7.addMember(_6);
          _6.show()
        }
      } else if (this.useEventCanvasRolloverControls) {
        if (this.eventCanvasCloseButton) this.eventCanvasCloseButton.hide()
      }
      if (_7.members.length > 0) {
        _7.eventCanvas = _1;
        _5.add(_7)
      } else {}
    }
    if (this.canResizeEvent(_1.event)) {
      var _9 = _1.resizeFrom || [],
          _10 = this.getEventStartDate(_1.event),
          _11 = this.getEventEndDate(_1.event);
      for (var i = 0; i < _9.length; i++) {
        var _13 = _9[i];
        if ((["T", "L"].contains(_13) && !this.shouldDisableDate(_10)) || (["B", "R"].contains(_13) && !this.shouldDisableDate(_11))) {
          _6 = this.getEventCanvasResizer(_9[i]);
          if (_6) {
            _6.eventCanvas = _1;
            _6.dragTarget = _1.dragTarget;
            _5.add(_6)
          }
        }
      }
    }
    _1.$129e = [];
    for (var i = 0; i < _5.length; i++) {
      _1.addChild(_5[i]);
      _1.$129e.add(_5[i])
    }
    return true
  }, isc.A.getEventCanvasCloseButton = function isc_Calendar_getEventCanvasCloseButton() {
    if (this.useEventCanvasRolloverControls) {
      if (!this.eventCanvasCloseButton) {
        this.eventCanvasCloseButton = this.addAutoChild("eventCanvasCloseButton")
      }
      return this.eventCanvasCloseButton
    } else {
      return this.createAutoChild("eventCanvasCloseButton")
    }
  }, isc.A.getEventCanvasContextButton = function isc_Calendar_getEventCanvasContextButton(_1) {
    if (this.useEventCanvasRolloverControls) {
      if (!this.eventCanvasContextButton) {
        this.eventCanvasContextButton = this.addAutoChild("eventCanvasContextButton")
      }
      return this.eventCanvasContextButton
    } else {
      return this.createAutoChild("eventCanvasContextButton")
    }
  }, isc.A.getEventCanvasResizer = function isc_Calendar_getEventCanvasResizer(_1) {
    var _2 = "eventCanvasResizer" + _1,
        _3 = this.useEventCanvasRolloverControls ? this[_2] : null;
    if (!_3) {
      var _4 = "eventCanvas" + (["T", "B"].contains(_1) ? "V" : "H") + "Resizer",
          _5 = {
          snapTo: _1,
          getEventEdge: function () {
            return this.snapTo
          }
          };
      _3 = this.createAutoChild(_4, _5);
      this[_2] = _3
    }
    return _3
  }, isc.A.setShowZones = function isc_Calendar_setShowZones(_1) {
    this.showZones = _1;
    var _2 = this.timelineView;
    if (_2 && _2.isSelectedView()) _2.refreshEvents();
    else if (_2) _2.$123s = true
  }, isc.A.setZones = function isc_Calendar_setZones(_1) {
    if (!_1) {
      return
    }
    this.zones = _1;
    if (this.timelineView) {
      this.timelineView.drawZones()
    }
  }, isc.A.addZone = function isc_Calendar_addZone(_1) {
    if (!_1) return;
    this.zones = this.zones || [];
    this.zones.add(_1);
    this.setZones(this.zones)
  }, isc.A.removeZone = function isc_Calendar_removeZone(_1) {
    if (!_1 || !this.zones) return;
    if (isc.isA.String(_1)) _1 = this.zones.find(this.nameField, _1);
    if (_1) {
      this.zones.remove(_1);
      this.setZones(this.zones)
    }
  }, isc.A.getZoneCanvas = function isc_Calendar_getZoneCanvas(_1, _2) {
    var _3 = {
      calendar: this,
      calendarView: _2,
      event: _1,
      isZoneCanvas: true,
      styleName: this.getZoneCanvasStyle(_1, _2)
    };
    var _4 = this.createAutoChild("zoneCanvas", _3, this.zoneCanvasConstructor);
    return _4
  }, isc.A.$131j = function isc_Calendar__getEventStyleName(_1) {
    return _1[this.eventWindowStyleField] || _1[this.eventStyleNameField]
  }, isc.A.getZoneCanvasStyle = function isc_Calendar_getZoneCanvasStyle(_1, _2) {
    _2 = _2 || this.getSelectedView();
    var _3 = this.$131j(_1) || (_2 && _2.zoneStyleName) || this.zoneStyleName;
    return _3
  }, isc.A.setShowIndicators = function isc_Calendar_setShowIndicators(_1) {
    this.showIndicators = _1;
    var _2 = this.timelineView;
    if (_2 && _2.isSelectedView()) _2.refreshEvents();
    else if (_2) _2.$123s = true
  }, isc.A.getIndicatorCanvas = function isc_Calendar_getIndicatorCanvas(_1, _2) {
    _2 = _2 || this.getSelectedView();
    var _3 = {
      calendar: this,
      calendarView: _2,
      event: _1,
      isIndicatorCanvas: true,
      styleName: this.getIndicatorCanvasStyle(_1, _2)
    };
    var _4 = this.createAutoChild("indicatorCanvas", _3, this.indicatorCanvasConstructor);
    return _4
  }, isc.A.getIndicatorCanvasStyle = function isc_Calendar_getIndicatorCanvasStyle(_1, _2) {
    _2 = _2 || this.getSelectedView();
    return this.$131j(_1) || (_2 && _2.indicatorStyleName) || this.indicatorStyleName
  }, isc.A.setIndicators = function isc_Calendar_setIndicators(_1) {
    if (!_1) {
      return
    }
    this.indicators = _1;
    if (this.timelineView) {
      this.timelineView.drawIndicators()
    }
  }, isc.A.addIndicator = function isc_Calendar_addIndicator(_1) {
    if (!_1) return;
    this.indicators = this.indicators || [];
    this.indicators.add(_1);
    this.setIndicators(this.indicators)
  }, isc.A.removeIndicator = function isc_Calendar_removeIndicator(_1) {
    if (!_1 || !this.indicators) return;
    if (isc.isA.String(_1)) _1 = this.indicators.find(this.nameField, _1);
    if (_1) {
      this.indicators.remove(_1);
      this.setIndicators(this.indicators)
    }
  }, isc.A.$129r = function isc_Calendar__getEventCanvas(_1, _2) {
    var _3 = this.canDragEvents,
        _4 = this.canEditEvent(_1),
        _5 = this.canRemoveEvent(_1),
        _6 = this.getEventCanvasStyle(_1, _2),
        _7 = false,
        _8 = false,
        _9;
    var _10 = {
      isEventCanvas: true,
      autoDraw: false,
      _redrawWithParent: false,
      calendar: this,
      calendarView: _2,
      vertical: !_2.isTimelineView(),
      canDragReposition: _3 && _4,
      canDragResize: _4,
      showCloseButton: _5,
      dragTarget: _2.eventDragTarget
    };
    _9 = _2.getCurrentEventCanvas(_1);
    _8 = (_9 != null);
    if (_8) {
      _2.$128h.remove(_9)
    } else if (_2.useEventCanvasPool) {
      _9 = _2.getPooledEventCanvas(_1);
      if (_9) {
        _7 = true;
        _9.VSnapOrigin = 0
      }
    }
    if (_9) {
      _9.event = _1;
      if (!_8) _9.setProperties(_10);
      if (_9.setEvent) _9.setEvent(_1, _6);
      else {
        _9.event = _1;
        _9.setEventStyle(_6)
      }
    } else {
      _10 = isc.addProperties(_10, {
        event: _1,
        baseStyle: _6,
        styleName: _6
      });
      var _11 = this.getEventCanvasConstructor(_1, _2);
      _9 = this.createAutoChild("eventCanvas", _10, _11)
    }
    if (_2.$129c && !_2.$129c.contains(_9)) _2.$129c.add(_9);
    if (_2.$128q && !_2.$128q.contains(_1)) _2.$128q.add(_1);
    _9.$128k = false;
    this.setEventCanvasID(_2, _1, _9.ID);
    return _9
  }, isc.A.$53g = function isc_Calendar__getEventsInRange(_1, _2, _3, _4) {
    var _5 = [],
        _6 = Date.getWeekendDays(),
        _7 = this.data.getLength(),
        _8 = [],
        _9 = _1.getTime(),
        _10 = _2.getTime();
    _3 = _3 || this.getSelectedView();
    if (_4) {
      var _11 = _3.body.getVisibleColumns();
      if (_11[0] >= 0 && _11[1] >= 0) {
        for (var i = _11[0]; i <= _11[1]; i++) {
          _8.add(_3.body.fields[i][this.laneNameField])
        }
      }
    }
    for (var i = 0; i < _7; i++) {
      var _13 = this.data.get(i),
          _14 = this.getEventStartDate(_13);
      if (_4 && !_8.contains(_13[this.laneNameField])) continue;
      if (!_13 || !_14) return [];
      if (_14.getTime() >= _1.getTime() && _14.getTime() <= _2.getTime() && (this.showWeekends || !_6.contains(_14.getDay()))) {
        if (_3 && _3.isWeekView()) _5.add(_13);
        else if (!this.showDayLanes || _8.contains(_13[this.laneNameField])) _5.add(_13)
      }
    }
    return _5
  }, isc.A.getDayEnd = function isc_Calendar_getDayEnd(_1) {
    return new Date(_1.getFullYear(), _1.getMonth(), _1.getDate(), 23, 59, 59)
  }, isc.A.isTimeline = function isc_Calendar_isTimeline() {
    var _1 = this.getCurrentViewName() == "timeline";
    return _1
  }, isc.A.$129t = function isc_Calendar__storeChosenDateRange(_1) {
    this.chosenDateStart = isc.DateUtil.getStartOf(_1, "d");
    this.chosenDateEnd = isc.DateUtil.getEndOf(_1, "d");
    var _2 = this.chosenWeekStart = new Date(this.year, this.month, this.chosenDate.getDate() - this.chosenDate.getDay() + this.firstDayOfWeek, 0, 0);
    if (Date.compareDates(this.chosenDate, _2) == 1) {
      this.chosenWeekStart.setDate(this.chosenWeekStart.getDate() - 7)
    }
    this.chosenWeekEnd = new Date(_2.getFullYear(), _2.getMonth(), _2.getDate() + 6, 23, 59);
    if (Date.compareDates(this.chosenDate, this.chosenWeekEnd) == -1) {
      this.chosenWeekStart.setDate(this.chosenWeekStart.getDate() + 7);
      this.chosenWeekEnd.setDate(this.chosenWeekEnd.getDate() + 7)
    }
  }, isc.A.setChosenDate = function isc_Calendar_setChosenDate(_1, _2) {
    var _3 = this.getSelectedView();
    this.year = _1.getFullYear();
    this.month = _1.getMonth();
    this.$53h = this.chosenDate.duplicate();
    this.chosenDate = _1;
    this.$129t(_1.duplicate());
    if (this.dayView) {
      var _4 = {
        date: isc.Date.createLogicalDate(_1.getFullYear(), _1.getMonth(), _1.getDate()),
        $654: _1.getDay(),
        $658: _1.getDate(),
        $659: _1.getMonth(),
        $66a: _1.getFullYear()
      },
          _5;
      for (var i = 0; i < this.dayView.body.fields.length; i++) {
        _5 = this.dayView.body.getField(i);
        if (_5) isc.addProperties(_5, _4)
      }
      isc.DaySchedule.$129g(this, this.dayView, this.chosenDate)
    }
    if (this.$53h.getFullYear() != this.year || this.$53h.getMonth() != this.month) {
      if (this.monthView) {
        if (this.monthViewSelected()) this.monthView.refreshEvents();
        else this.monthView.$123s = true
      }
    }
    var _7 = new Date(this.$53h.getFullYear(), this.$53h.getMonth(), this.$53h.getDate() - this.$53h.getDay());
    var _8 = new Date(_7.getFullYear(), _7.getMonth(), _7.getDate() + 6);
    var _9 = this.chosenDate.getTime();
    if (_9 < _7.getTime() || _9 > _8.getTime()) {
      if (this.weekView) {
        this.$53a();
        if (this.weekViewSelected()) this.weekView.refreshEvents();
        else this.weekView.$123s = true
      }
    }
    if (_9 != this.$53h.getTime()) {
      if (this.dayView) {
        if (this.dayViewSelected()) this.dayView.refreshEvents();
        else this.dayView.$123s = true
      }
    }
    if (this.timelineView && !_2) {
      this.timelineView.setTimelineRange(this.chosenDate, null, null, null, null, null, true)
    } else {
      if (this.scrollToWorkday && _3.scrollToWorkdayStart) {
        _3.scrollToWorkdayStart()
      } else {
        _3.redraw()
      }
    }
    this.setDateLabel();
    this.dateChanged()
  }, isc.A.dateIsWorkday = function isc_Calendar_dateIsWorkday(_1, _2) {
    if (!_1 || !this.workdays) return false;
    return this.workdays.contains(_1.getDay())
  }, isc.A.adjustCriteria = function isc_Calendar_adjustCriteria(_1) {
    return _1
  }, isc.A.getNewCriteria = function isc_Calendar_getNewCriteria(_1) {
    _1 = _1 || this.getSelectedView();
    if (!_1) return {};
    var _2 = null,
        _3 = null,
        _4 = {},
        _5 = this.fetchMode || "all";
    if (_5 == "auto") {
      var _6 = this.getLargestScrollableRange();
      _2 = _6[0];
      _3 = _6[1]
    } else if (_5 != "all") {
      var _7 = this.getView(_5);
      _2 = this.getVisibleStartDate(_7);
      _3 = this.getVisibleEndDate(_7)
    }
    if (_2 && _3) {
      _4 = {
        _constructor: "AdvancedCriteria",
        operator: "and",
        criteria: [{
          fieldName: this.startDateField,
          operator: "lessThan",
          value: _3
        }, {
          fieldName: this.endDateField,
          operator: "greaterThan",
          value: _2
        }]
      }
    }
    _4 = this.adjustCriteria(_4);
    return _4
  }, isc.A.$53a = function isc_Calendar__setWeekTitles() {
    if (!this.weekView) return;
    var _1 = this.chosenWeekStart.duplicate();
    var _2 = Date.getShortDayNames();
    var _3 = Date.getWeekendDays();
    isc.DaySchedule.$129g(this, this.weekView, this.chosenWeekStart);
    for (var i = 1; i < 8; i++) {
      if (this.weekView.getFieldNum("day" + i) >= 0) {
        var _5 = _1.toShortDate(this.dateFormatter, false);
        if (_5.match(this.$81i) != null) _5 = _5.substring(5);
        else if (_5.match(this.$81j)) _5 = _5.substring(0, _5.length - 5);
        var _6 = _2[_1.getDay()] + " " + _5;
        var p = {
          title: _6,
          align: "right",
          $654: _1.getDay(),
          $658: _1.getDate(),
          $659: _1.getMonth(),
          $66a: _1.getFullYear()
        };
        p.date = isc.Date.createLogicalDate(p.$66a, p.$659, p.$658), this.weekView.setFieldProperties("day" + i, p);
        if (this.weekView.header) this.weekView.header.markForRedraw()
      }
      _1.setDate(_1.getDate() + 1)
    }
    this.weekView.startDate = this.chosenWeekStart;
    this.weekView.endDate = this.chosenWeekEnd
  }, isc.A.next = function isc_Calendar_next() {
    var _1;
    if (this.dayViewSelected()) {
      _1 = new Date(this.year, this.month, this.chosenDate.getDate() + 1);
      if (!this.showWeekends) {
        var _2 = Date.getWeekendDays();
        for (var i = 0; i < _2.length; i++) {
          if (_2.contains(_1.getDay())) _1.setDate(_1.getDate() + 1)
        }
      }
    } else if (this.weekViewSelected()) {
      _1 = new Date(this.year, this.month, this.chosenDate.getDate() + 7)
    } else if (this.monthViewSelected()) {
      _1 = new Date(this.year, this.month + 1, 1)
    } else if (this.timelineViewSelected()) {
      _1 = this.chosenDate.duplicate();
      this.timelineView.nextOrPrev(true);
      return
    }
    this.dateChooser.setData(_1);
    this.setChosenDate(_1)
  }, isc.A.previous = function isc_Calendar_previous() {
    var _1;
    if (this.dayViewSelected()) {
      _1 = new Date(this.year, this.month, this.chosenDate.getDate() - 1);
      if (!this.showWeekends) {
        var _2 = Date.getWeekendDays();
        for (var i = 0; i < _2.length; i++) {
          if (_2.contains(_1.getDay())) _1.setDate(_1.getDate() - 1)
        }
      }
    } else if (this.weekViewSelected()) {
      _1 = new Date(this.year, this.month, this.chosenDate.getDate() - 7)
    } else if (this.monthViewSelected()) {
      _1 = new Date(this.year, this.month - 1, 1)
    } else if (this.timelineViewSelected()) {
      this.timelineView.nextOrPrev(false);
      return
    }
    this.dateChooser.setData(_1);
    this.setChosenDate(_1)
  }, isc.A.dataArrived = function isc_Calendar_dataArrived() {
    return true
  }, isc.A.draw = function isc_Calendar_draw(_1, _2, _3, _4) {
    this.invokeSuper(isc.Calendar, "draw", _1, _2, _3, _4);
    if (isc.ResultSet && isc.isA.ResultSet(this.data) && this.dataSource) {
      this.observe(this.data, "dataArrived", "observer.dataArrived(arguments[0], arguments[1])")
    }
    if (this.mainView.isA("TabSet")) {
      if (this.showControlsBar != false) {
        this.mainView.addChild(this.controlsBar);
        this.controlsBar.moveAbove(this.mainView.tabBar)
      }
    }
  }, isc.A.$653 = function isc_Calendar__getTabs() {
    var _1 = [],
        _2 = {
        calendar: this,
        baseStyle: this.baseStyle
        },
        _3 = this.lanes ? this.lanes.duplicate() : null;
    if (this.showDayView != false) {
      this.dayView = this.createAutoChild("dayView", isc.addProperties({
        viewName: "day",
        startDate: this.chosenDateStart,
        endDate: this.chosenDateEnd
      }, _2, {
        cellHeight: this.rowHeight
      }));
      _1.add({
        title: this.dayViewTitle,
        pane: this.dayView,
        viewName: "day"
      })
    }
    if (this.showWeekView != false) {
      this.weekView = this.createAutoChild("weekView", isc.addProperties({
        viewName: "week"
      }, _2, {
        cellHeight: this.rowHeight
      }));
      _1.add({
        title: this.weekViewTitle,
        pane: this.weekView,
        viewName: "week"
      })
    }
    if (this.showMonthView != false) {
      this.monthView = this.createAutoChild("monthView", isc.addProperties({
        viewName: "month"
      }, _2, {
        bodyConstructor: "MonthScheduleBody"
      }));
      _1.add({
        title: this.monthViewTitle,
        pane: this.monthView,
        viewName: "month"
      })
    }
    if (this.showTimelineView != false) {
      this.timelineView = this.createAutoChild("timelineView", isc.addProperties({
        viewName: "timeline"
      }, _2));
      _1.add({
        title: this.timelineViewTitle,
        pane: this.timelineView,
        viewName: "timeline"
      })
    }
    return _1
  }, isc.A.$655 = function isc_Calendar__createTabSet(_1) {
    if (_1.length > 1) {
      this.mainView = this.createAutoChild("mainView", {
        tabs: _1,
        tabSelected: function (_3, _4, _5, _6) {
          this.creator.$567 = _4.viewName;
          this.creator.setDateLabel();
          if (this.creator.getSelectedView().$123s) {
            this.creator.refreshSelectedView()
          }
          this.creator.currentViewChanged(_4.viewName)
        }
      });
      if (this.currentViewName) {
        var _2 = _1.find("viewName", this.currentViewName);
        if (_2) this.mainView.selectTab(_2)
      }
    } else {
      this.mainView = _1[0].pane
    }
  }, isc.A.getLaneMap = function isc_Calendar_getLaneMap() {
    if (!this.isTimeline() && !this.showDayLanes) return {};
    var _1 = this.showDayLanes ? this.lanes : this.timelineView.data,
        _2 = {};
    for (var i = 0; i < _1.length; i++) {
      var _4 = _1[i].name || _1[i][this.laneNameField],
          _5 = _1[i].title || _4;
      _2[_4] = _5
    }
    return _2
  }, isc.A.getSublaneMap = function isc_Calendar_getSublaneMap(_1, _2) {
    _2 = _2 || this.getSelectedView();
    var _3 = {};
    if (isc.isA.String(_1)) _1 = _2.getLane(_1);
    if (_1 && _1.sublanes) {
      for (var i = 0; i < _1.sublanes.length; i++) {
        var _5 = _1.sublanes[i],
            _6 = _5.name || _5[this.laneNameField],
            _7 = _5.title || _6;
        _3[_6] = _7
      }
    }
    return _3
  }, isc.A.getLanePadding = function isc_Calendar_getLanePadding(_1) {
    _1 = _1 || this.getSelectedView();
    if (_1.hasLanes()) return this.laneEventPadding;
    return 0
  }, isc.A.getLaneEvents = function isc_Calendar_getLaneEvents(_1, _2) {
    var _3 = isc.isAn.Object(_1) ? _1.name : _1;
    if (!_3 || !isc.isA.String(_3)) return [];
    _2 = _2 || this.getSelectedView();
    var _4 = this.data.findAll(this.laneNameField, _3) || [];
    return _4
  }, isc.A.getSublaneEvents = function isc_Calendar_getSublaneEvents(_1, _2, _3) {
    var _4 = isc.isAn.Object(_1) ? _1.name : _1,
        _5 = isc.isAn.Object(_2) ? _2.name : _2;
    if ((!_4 || !isc.isA.String(_4)) || (!_5 || !isc.isA.String(_5))) {
      return []
    }
    _3 = _3 || this.getSelectedView();
    var _6 = this.getLaneEvents(_4, _3),
        _7 = _6.findAll(this.sublaneNameField, _5);
    return _7
  }, isc.A.createChildren = function isc_Calendar_createChildren() {
    var _1 = this.$653();
    this.$655(_1);
    var _2 = 20;
    if (this.showControlsBar != false) {
      this.dateLabel = this.createAutoChild("dateLabel");
      this.addEventButton = this.createAutoChild("addEventButton", {
        click: function () {
          var _3 = this.creator;
          var _4 = _3.getSelectedView();
          _3.eventDialog.event = null;
          _3.eventDialog.isNewEvent = true;
          _3.eventDialog.items[0].createFields();
          var _5 = new Date(),
              _6 = null,
              _7 = _3.chosenDate.duplicate();
          if (_4.isDayView()) {
            _5 = _7
          } else if (_4.isWeekView()) {
            if (_3.chosenWeekStart.getTime() > _5.getTime()) {
              _5 = _3.chosenWeekStart.duplicate()
            }
            if (!this.showWeekends) {
              var _8 = Date.getWeekendDays();
              for (var i = 0; i < _8.length; i++) {
                if (_8.contains(_5.getDay())) _5.setDate(_5.getDate() + 1)
              }
            }
            _5.setMinutes(0);
            if (_5.getHours() > 22) {
              _5.setDate(_5.getDate() + 1);
              _5.setHours(0)
            } else _5.setHours(_5.getHours() + 1)
          } else if (_4.isMonthView()) {
            _7.setDate(1);
            if (_7.getTime() > _5.getTime()) _5 = _7
          } else if (_3.isTimeline()) {
            var _10 = _3.timelineView,
                _11 = _10.getVisibleDateRange();
            _5 = _11[0];
            _6 = _5.duplicate();
            _6 = _10.addUnits(_6, 1, _3.timelineGranularity)
          }
          _3.eventDialog.setDate(_5, _6);
          _3.eventDialog.setPageLeft(_3.getPageLeft());
          _3.eventDialog.setPageTop(this.getPageTop() + this.getVisibleHeight());
          _3.displayEventDialog()
        }
      });
      this.datePickerButton = this.createAutoChild("datePickerButton", {
        click: function () {
          var _3 = this.creator;
          if (this.$54x) {
            this.$54x.setData(_3.chosenDate);
            this.$54x.draw()
          } else {
            this.$54x = isc[_3.dateChooserConstructor].create({
              calendar: this.creator,
              autoDraw: false,
              showCancelButton: true,
              autoClose: true,
              disableWeekends: this.creator.disableWeekends,
              firstDayOfWeek: this.creator.firstDayOfWeek,
              showWeekends: this.creator.showWeekends,
              dateClick: function (_16, _17, _18) {
                var _12 = new Date(_16, _17, _18);
                this.setData(_12);
                this.calendar.dateChooser.dateClick(_16, _17, _18);
                this.close()
              }
            });
            this.$54x.setData(_3.chosenDate);
            _3.addChild(this.$54x);
            this.$54x.placeNextTo(this, "bottom", true)
          }
        }
      });
      this.previousButton = this.createAutoChild("previousButton", {});
      this.nextButton = this.createAutoChild("nextButton", {})
    }
    var _13 = [];
    if (this.showPreviousButton != false) _13.add(this.previousButton);
    if (this.showDateLabel != false) _13.add(this.dateLabel);
    if (this.showDatePickerButton != false) _13.add(this.datePickerButton);
    if (this.canCreateEvents && this.showAddEventButton != false) _13.add(this.addEventButton);
    if (this.showNextButton != false) _13.add(this.nextButton);
    if (this.showControlsBar != false) {
      this.controlsBar = this.createAutoChild("controlsBar", {
        members: _13
      })
    }
    var _3 = this;
    this.dateChooser = this.createAutoChild("dateChooser", {
      disableWeekends: this.disableWeekends,
      showWeekends: this.showWeekends,
      chosenDate: this.chosenDate,
      month: this.month,
      year: this.year,
      dateClick: function (_16, _17, _18) {
        var _12 = new Date(_16, _17, _18);
        this.setData(_12);
        this.creator.setChosenDate(_12)
      },
      showPrevYear: function () {
        this.year--;
        this.dateClick(this.year, this.month, this.chosenDate.getDate())
      },
      showNextYear: function () {
        this.year++;
        this.dateClick(this.year, this.month, this.chosenDate.getDate())
      },
      showPrevMonth: function () {
        if (--this.month == -1) {
          this.month = 11;
          this.year--
        }
        this.dateClick(this.year, this.month, 1)
      },
      showNextMonth: function () {
        if (++this.month == 12) {
          this.month = 0;
          this.year++
        }
        this.dateClick(this.year, this.month, 1)
      }
    });
    if (!this.children) this.children = [];
    var _14 = [];
    var _15 = [];
    _15.add(this.dateChooser);
    if (this.showDateChooser) {
      _14.add(isc.VLayout.create({
        autoDraw: false,
        width: "20%",
        membersMargin: 10,
        layoutTopMargin: 10,
        members: _15
      }))
    }
    if (this.mainView.isA("TabSet")) {
      _14.add(this.mainView)
    } else {
      if (this.showControlsBar != false) {
        this.controlsBarContainer = this.createAutoChild("controlsBarContainer", {
          autoDraw: false,
          height: this.controlsBar.getVisibleHeight(),
          width: "100%"
        }, isc.HLayout);
        this.controlsBarContainer.addMember(isc.LayoutSpacer.create({
          autoDraw: false,
          width: "*"
        }));
        this.controlsBarContainer.addMember(this.controlsBar);
        this.controlsBarContainer.addMember(isc.LayoutSpacer.create({
          autoDraw: false,
          width: "*"
        }));
        this.mainLayout = this.createAutoChild("mainLayout", {
          autoDraw: false,
          members: [this.controlsBarContainer, this.mainView]
        }, isc.VLayout);
        _14.add(this.mainLayout)
      } else {
        _14.add(this.mainView)
      }
    }
    this.children.add(isc.HLayout.create({
      autoDraw: false,
      width: "100%",
      height: "100%",
      members: _14
    }));
    this.setDateLabel()
  }, isc.A.createEditors = function isc_Calendar_createEditors() {
    var _1 = this;
    this.eventDialog = this.createAutoChild("eventDialog", {
      items: [isc.DynamicForm.create({
        autoDraw: false,
        padding: 4,
        calendar: this,
        saveOnEnter: true,
        useAllDataSourceFields: true,
        numCols: 2,
        colWidths: [80, "*"],
        $642: [_1.nameField, _1.laneNameField, _1.sublaneNameField],
        getCustomValues: function () {
          if (!this.calendar.eventDialogFields) return;
          var _2 = this.$642;
          var _3 = this.calendar.eventDialogFields;
          var _4 = {};
          for (var i = 0; i < _3.length; i++) {
            var _6 = _3[i];
            if (_6.name && !_2.contains(_6.name)) {
              _4[_6.name] = this.getValue(_6.name)
            }
          }
          return _4
        },
        setCustomValues: function (_51) {
          if (!this.calendar.eventDialogFields) return;
          var _2 = this.$642;
          var _3 = this.calendar.eventDialogFields;
          for (var i = 0; i < _3.length; i++) {
            var _6 = _3[i];
            if (_6.name && !_2.contains(_6.name)) {
              this.setValue(_6.name, _51[_6.name])
            }
          }
        },
        createFields: function (_51) {
          var _1 = this.calendar,
              _7 = _1.eventDialog.isNewEvent,
              _8 = !_7 ? "staticText" : "text",
              _9 = !_7 ? "staticText" : "select",
              _10 = !_7 ? "staticText" : "select",
              _11 = _1.isTimeline() || (_1.showDayLanes && _1.dayViewSelected()),
              _12 = _11 && _1.useSublanes;
          var _13 = [{
            name: _1.nameField,
            title: _1.eventNameFieldTitle,
            type: _8,
            width: 250
          }, {
            name: _1.laneNameField,
            title: _1.eventLaneFieldTitle,
            type: _9,
            width: 150,
            valueMap: _1.getLaneMap(),
            showIf: _11 ? "true" : "false",
            changed: function (_41, _52, _53) {
              var _14 = _1.lanes.find("name", _53);
              if (_53 && _14) {
                var _15 = _41.getItem(_1.sublaneNameField);
                if (_15) _15.setValueMap(_1.getSublaneMap(_14))
              }
            }
          }, {
            name: _1.sublaneNameField,
            title: _1.eventSublaneFieldTitle,
            type: _10,
            width: 150,
            valueMap: [],
            showIf: _12 ? "true" : "false"
          }, {
            name: "save",
            title: _1.saveButtonTitle,
            editorType: "SubmitItem",
            endRow: false
          }, {
            name: "details",
            title: _1.detailsButtonTitle,
            type: "button",
            startRow: false,
            click: function (_41, _52) {
              var _1 = _41.calendar,
                  _16 = _1.eventDialog.isNewEvent,
                  _17 = _1.eventDialog.event,
                  _18 = _41.getValue(_1.nameField),
                  _19 = _41.getValue(_1.laneNameField),
                  _20 = _41.getValue(_1.sublaneNameField);
              if (_16) {
                _17[_1.nameField] = _18;
                if (_19) _17[_1.laneNameField] = _19;
                if (_20) _17[_1.sublaneNameField] = _19
              }
              _41.calendar.showEventEditor(_17, _16)
            }
          }];
          if (!_7) _13.removeAt(3);
          var _21 = isc.DataSource.create({
            addGlobalId: false,
            fields: _13
          });
          this.setDataSource(_21);
          this.setFields(isc.shallowClone(this.calendar.eventDialogFields))
        },
        submit: function () {
          var _1 = this.calendar,
              _7 = _1.eventDialog.isNewEvent,
              _22 = _7 ? _1.eventDialog.event : null,
              _23 = _1.eventDialog.currentStart,
              _24 = _1.eventDialog.currentEnd,
              _14 = null,
              _25 = null;
          if (!this.validate()) return;
          if (_1.isTimeline() || (_1.dayViewSelected() && _1.showDayLanes)) {
            _14 = this.getItem(_1.laneNameField).getValue();
            _25 = this.getItem(_1.sublaneNameField).getValue()
          }
          var _26 = isc.addProperties({}, this.getCustomValues());
          _1.$1115 = true;
          var _27 = _1.createEventObject(_22, _23, _24, _14, _25, this.getValue(_1.nameField));
          if (!_7) {
            _1.updateCalendarEvent(_22, _27, _26)
          } else {
            _1.addCalendarEvent(_27, _26)
          }
          _1.hideEventDialog()
        }
      })],
      setDate: function (_51, _50) {
        var _1 = this.creator;
        if (!_50) {
          if (_51.getHours() == 23 && _51.getMinutes() == (60 - _1.getMinutesPerRow())) {
            _50 = new Date(_51.getFullYear(), _51.getMonth(), _51.getDate() + 1)
          } else {
            _50 = new Date(_51.getFullYear(), _51.getMonth(), _51.getDate(), _51.getHours() + 1, _51.getMinutes())
          }
        }
        this.setTitle(_1.$53k(_51, _50));
        this.currentStart = _51;
        this.currentEnd = _50;
        this.items[0].getItem(_1.nameField).setValue("")
      },
      setLane: function (_14) {
        var _1 = this.creator;
        if (isc.isA.Number(_14)) _14 = _1.lanes[_14].name;
        this.items[0].getItem(_1.laneNameField).setValue(_14)
      },
      setEvent: function (_17) {
        this.event = _17;
        var _28 = this.items[0],
            _1 = this.creator,
            _29 = _1.getSelectedView();
        if (_1.eventDialogFields) {
          _28.clearErrors(true);
          _28.setCustomValues(_17)
        }
        this.setDate(_1.getEventStartDate(_17), _1.getEventEndDate(_17));
        if (_1.useSublanes && _17[_1.laneNameField]) {
          var _14 = _29.getLane(_17[_1.laneNameField]);
          if (_14) {
            var _15 = _28.getItem(_1.sublaneNameField);
            _15.setValueMap(_1.getSublaneMap(_14))
          }
        }
        _28.setValues(_17)
      },
      closeClick: function () {
        this.Super('closeClick');
        this.creator.clearViewSelection()
      },
      show: function () {
        if (this.creator.showQuickEventDialog) {
          if (!this.isDrawn()) this.draw();
          this.Super('show');
          this.items[0].getItem(this.creator.nameField).focusInItem()
        } else {
          this.creator.showEventEditor(this.event)
        }
      },
      hide: function () {
        this.Super('hide');
        this.moveTo(0, 0)
      }
    });
    this.eventEditor = this.createAutoChild("eventEditor", {
      useAllDataSourceFields: true,
      titleWidth: 80,
      initWidget: function () {
        this.invokeSuper(isc.DynamicForm, "initWidget", arguments);
        this.timeFormat = this.creator.timeFormat;
        var _13 = [],
            _1 = this.creator,
            _30 = _1.getDateEditingStyle(),
            _31 = [{
            name: "endType",
            type: "text",
            showTitle: false,
            width: "*",
            editorType: "SelectItem",
            textAlign: "right",
            valueMap: [_1.eventDurationFieldTitle, _1.eventEndDateFieldTitle],
            endRow: false,
            changed: function (_41, _51, _52) {
              _30 = _1.getDateEditingStyle();
              if (_52 == _1.eventDurationFieldTitle) {
                _41.getItem(_1.durationField).show();
                _41.getItem(_1.durationUnitField).show();
                if (_30 == "time") {
                  _41.getItem("endHours").hide();
                  _41.getItem("endMinutes").hide();
                  _41.getItem("endAMPM").hide()
                } else {
                  _41.getItem(_1.endDateField).hide()
                }
              } else {
                _41.getItem(_1.durationField).hide();
                _41.getItem(_1.durationUnitField).hide();
                if (_30 == "time") {
                  _41.getItem("endHours").show();
                  _41.getItem("endMinutes").show();
                  _41.getItem("endAMPM").show()
                } else {
                  _41.getItem(_1.endDateField).show()
                }
              }
            }
          }, {
            name: _1.durationField,
            type: "integer",
            editorType: "SpinnerItem",
            title: _1.eventDurationFieldTitle,
            endRow: false,
            showTitle: false,
            width: "*",
            colSpan: 1,
            defaultValue: 1
          }, {
            name: _1.durationUnitField,
            type: "text",
            showTitle: false,
            endRow: true,
            title: _1.eventDurationUnitFieldTitle,
            width: "*",
            colSpan: 1,
            valueMap: _1.getDurationUnitMap(),
            defaultValue: "minute"
          }];
        this.$642.addList([_1.nameField, _1.descriptionField, _1.startDateField, "endType", _1.durationField, _1.durationUnitField, _1.endDateField]);
        if (_1.timelineView || (_1.dayViewSelected() && _1.showDayLanes)) {
          var _32 = _1.getLaneMap(),
              _33 = {
              name: _1.laneNameField,
              title: _1.eventLaneFieldTitle,
              type: "select",
              valueMap: _32,
              endRow: true,
              width: "*",
              colSpan: 3,
              changed: function (_41, _51, _52) {
                var _14 = _1.lanes.find("name", _52);
                if (_52 && _14) {
                  var _15 = _41.getItem(_1.sublaneNameField);
                  if (_15) _15.setValueMap(_1.getSublaneMap(_14))
                }
              }
              };
          _13.add(_33);
          if (_1.useSublanes) {
            var _34 = {},
                _35 = {
                name: _1.sublaneNameField,
                title: _1.eventSublaneFieldTitle,
                type: "select",
                valueMap: _34,
                endRow: true,
                width: "*",
                colSpan: 3
                };
            _13.add(_35)
          }
        }
        var _36 = _1.allowDurationEvents;
        if (_30 == "date" || _30 == "datetime") {
          _13.add({
            name: _1.startDateField,
            title: _1.eventStartDateFieldTitle,
            type: _30,
            colSpan: "*",
            endRow: true
          });
          if (_36) _13.addList(_31);
          _13.addList([{
            name: _1.endDateField,
            title: _1.eventEndDateFieldTitle,
            showTitle: !_36,
            type: _30,
            colSpan: "*",
            endRow: true
          }, {
            name: "invalidDate",
            type: "blurb",
            width: "*",
            colSpan: "*",
            visible: false,
            defaultValue: _1.invalidDateMessage,
            cellStyle: this.errorStyle || "formCellError",
            endRow: true
          }])
        } else if (_30 == "time") {
          this.numCols = 4;
          this.setColWidths([this.titleWidth, 60, 60, "*"]);
          _13.addList([{
            name: "startHours",
            title: _1.eventStartDateFieldTitle,
            type: "integer",
            width: 60,
            editorType: "select",
            valueMap: this.getTimeValues("hours")
          }, {
            name: "startMinutes",
            showTitle: false,
            type: "integer",
            width: 60,
            editorType: "select",
            valueMap: this.getTimeValues("minutes")
          }, {
            name: "startAMPM",
            showTitle: false,
            type: "select",
            width: 60,
            valueMap: this.getTimeValues(),
            endRow: true
          }, {
            name: "invalidDate",
            type: "blurb",
            colSpan: 4,
            visible: false,
            defaultValue: _1.invalidDateMessage,
            cellStyle: this.errorStyle || "formCellError",
            endRow: true
          }]);
          if (_36) _13.addList(_31);
          _13.addList([{
            name: "endHours",
            type: "integer",
            width: 60,
            title: _1.eventEndDateFieldTitle,
            showTitle: !_36,
            editorType: "select",
            valueMap: this.getTimeValues("hours")
          }, {
            name: "endMinutes",
            showTitle: false,
            type: "integer",
            width: 60,
            editorType: "select",
            valueMap: this.getTimeValues("minutes")
          }, {
            name: "endAMPM",
            showTitle: false,
            type: "select",
            width: 60,
            valueMap: this.getTimeValues(),
            endRow: true
          }])
        }
        _13.addList([{
          name: _1.nameField,
          title: _1.eventNameFieldTitle,
          type: "text",
          colSpan: "*",
          width: "*"
        }, {
          name: _1.descriptionField,
          title: _1.eventDescriptionFieldTitle,
          type: "textArea",
          colSpan: "*",
          width: "*",
          height: 50
        }]);
        var _37 = isc.DataSource.create({
          addGlobalId: false,
          fields: _13
        });
        this.setDataSource(_37);
        var _38 = isc.shallowClone(_1.eventEditorFields);
        this.setFields(_38)
      },
      getTimeValues: function (_51, _52) {
        if (!_52) _52 = 0;
        var _39 = {};
        if (_51 == "hours") {
          for (var i = _52; i < 12; i++) {
            _39[(i + 1) + ""] = (i + 1)
          }
        } else if (_51 == "minutes") {
          for (var i = 0; i < 60; i++) {
            var _40 = i < 10 ? "0" + i : "" + i;
            _39[i + ""] = _40
          }
        } else {
          _39["am"] = "am";
          _39["pm"] = "pm"
        }
        return _39
      },
      $642: ["startHours", "startMinutes", "startAMPM", "endHours", "endMinutes", "endAMPM"],
      getCustomValues: function () {
        if (!this.creator.eventEditorFields) return;
        var _1 = this.creator,
            _2 = this.$642;
        var _3 = this.creator.eventEditorFields;
        var _4 = {};
        for (var i = 0; i < _3.length; i++) {
          var _6 = _3[i];
          if (_6.name && !_2.contains(_6.name)) {
            _4[_6.name] = this.getValue(_6.name)
          }
        }
        return _4
      },
      setCustomValues: function (_51) {
        if (!this.creator.eventEditorFields) return;
        var _2 = this.$642;
        var _3 = this.creator.eventEditorFields;
        for (var i = 0; i < _3.length; i++) {
          var _6 = _3[i];
          if (_6.name && !_2.contains(_6.name)) {
            this.setValue(_6.name, _51[_6.name])
          }
        }
      }
    });
    this.eventEditorLayout = this.createAutoChild("eventEditorLayout", {
      items: [this.eventEditor, isc.HLayout.create({
        membersMargin: 10,
        layoutMargin: 10,
        autoDraw: false,
        members: [isc.IButton.create({
          autoDraw: false,
          title: this.saveButtonTitle,
          calendar: this,
          click: function () {
            this.calendar.addEventOrUpdateEventFields()
          }
        }), isc.IButton.create({
          autoDraw: false,
          title: this.cancelButtonTitle,
          calendar: this,
          click: function () {
            this.calendar.eventEditorLayout.hide()
          }
        })]
      })],
      setDate: function (_51, _50, _52, _14, _25) {
        if (!_52) _52 = "";
        if (!_50) {
          _50 = new Date(_51.getFullYear(), _51.getMonth(), _51.getDate(), _51.getHours() + 1, _51.getMinutes())
        }
        var _1 = this.creator;
        this.setTitle(_1.$53k(_51, _50));
        this.currentStart = _51;
        this.currentEnd = _50;
        var _30 = _1.getDateEditingStyle(),
            _41 = this.items[0];
        if (_30 == "date" || _30 == "datetime") {
          _41.getItem(_1.startDateField).setValue(_51.duplicate());
          _41.getItem(_1.endDateField).setValue(_50.duplicate())
        } else if (_30 == "time") {
          _41.getItem("startHours").setValue(this.getHours(_51.getHours()));
          _41.getItem("endHours").setValue(this.getHours(_50.getHours()));
          _41.getItem("startMinutes").setValue(_51.getMinutes());
          _41.getItem("endMinutes").setValue(_50.getMinutes());
          if (!_1.twentyFourHourTime) {
            _41.getItem("startAMPM").setValue(this.getAMPM(_51.getHours()));
            _41.getItem("endAMPM").setValue(this.getAMPM(_50.getHours()))
          }
        }
      },
      getHours: function (_51) {
        if (this.creator.twentyFourHourTime) return _51;
        else return this.creator.$53n(_51)
      },
      getAMPM: function (_51) {
        if (_51 < 12) return "am";
        else return "pm"
      },
      setEvent: function (_17) {
        var _41 = this.items[0],
            _1 = this.creator,
            _29 = this.view,
            _42 = _41.getItem(_1.laneNameField),
            _43 = _41.getItem(_1.sublaneNameField),
            _36 = _1.allowDurationEvents,
            _44 = _41.getItem("endType"),
            _45 = _41.getItem(_1.durationField),
            _46 = _41.getItem(_1.durationUnitField);
        this.event = _17;
        if (_1.eventEditorFields) {
          _41.clearErrors(true);
          _41.setCustomValues(_17)
        }
        if (_42) {
          _42.setValueMap(_1.getLaneMap());
          _42.setValue(_17[_1.laneNameField]);
          _42.setDisabled(!_1.canEditEventLane(_17));
          var _47 = _29.isTimelineView() || (_29.isDayView() && _1.showDayLanes);
          if (_47) _42.show();
          else _42.hide()
        }
        if (_43) {
          _43.setValueMap(_1.getSublaneMap(_17[_1.laneNameField]));
          _43.setValue(_17[_1.sublaneNameField]);
          _43.setDisabled(!_1.canEditEventSublane(_17));
          var _47 = _1.useSublanes && (_29.isTimelineView() || (_29.isDayView() && _1.showDayLanes));
          if (_47) _43.show();
          else _43.hide()
        }
        if (_36) {
          var _48 = _17[_1.durationField],
              _49 = _17[_1.durationUnitField] || "minute";
          if (_48 != null) {
            _44.setValue(_1.eventDurationFieldTitle);
            _45.setValue(_48);
            _45.show();
            _46.setValue(_49);
            _46.show();
            if (_1.getDateEditingStyle() == "time") {
              _41.getField("endHours").hide();
              _41.getField("endMinutes").hide();
              _41.getField("endAMPM").hide()
            } else {
              _41.getField(_1.endDateField).hide()
            }
          } else {
            _44.setValue(_1.eventEndDateFieldTitle);
            _45.hide();
            _46.hide();
            var _50 = _17[_1.endDateField];
            if (_1.getDateEditingStyle() == "time") {
              _41.getField("endHours").show();
              _41.getField("endHours").setValue(_50.getHours());
              _41.getField("endMinutes").show();
              _41.getField("endMinutes").setValue(_50.getMinutes());
              _41.getField("endAMPM").show()
            } else {
              _41.getField(_1.endDateField).show();
              _41.getField(_1.endDateField).setValue(_50)
            }
          }
        }
        this.setDate(_1.getEventStartDate(_17), _1.getEventEndDate(_17));
        _41.setValue(_1.nameField, _17[_1.nameField]);
        _41.setValue(_1.descriptionField, _17[_1.descriptionField]);
        this.originalStart = isc.clone(this.currentStart);
        this.originalEnd = isc.clone(this.currentEnd)
      },
      hide: function () {
        this.Super('hide');
        this.creator.clearViewSelection();
        this.creator.eventEditor.hideItem("invalidDate")
      },
      sizeMe: function () {
        this.setWidth(this.creator.mainView.getVisibleWidth());
        this.setHeight(this.creator.mainView.getVisibleHeight());
        this.setLeft(this.creator.mainView.getLeft())
      }
    });
    this.eventEditorLayout.hide()
  });
  isc.evalBoundary;
  isc.B.push(isc.A.hideEventDialog = function isc_Calendar_hideEventDialog() {
    this.eventDialog.hide()
  }, isc.A.displayEventDialog = function isc_Calendar_displayEventDialog() {
    this.eventDialog.show()
  }, isc.A.addEventOrUpdateEventFields = function isc_Calendar_addEventOrUpdateEventFields() {
    var _1 = this,
        _2 = _1.eventEditorLayout.isNewEvent,
        _3 = _1.eventEditorLayout.event,
        _4 = _1.eventEditor,
        _5 = _1.getDateEditingStyle(),
        _6 = _4.getValues(),
        _7 = _1.isTimeline() || (_1.dayViewSelected() && _1.showDayLanes) && _1.canEditLane,
        _8 = _7 ? _6[_1.laneNameField] : null,
        _9 = _7 && _1.useSublanes ? _6[_1.sublaneNameField] : null,
        _10 = _6["endType"] == this.eventDurationFieldTitle,
        _11 = _10 ? _6[this.durationField] || 1 : null,
        _12 = _10 ? _6[this.durationUnitField] || (_5 == "time" ? "minute" : "hour") : null;
    var _13 = isc.addProperties({}, _3, {
      eventLength: null
    });
    _13[this.nameField] = _6[this.nameField];
    _13[this.descriptionField] = _6[this.descriptionField];
    if (_8) _13[this.laneNameField] = _8;
    if (_9) _13[this.sublaneNameField] = _9;
    if (_5 == "date" || _5 == "datetime") {
      var _14 = _6[this.startDateField],
          _15 = !_10 ? _6[this.endDateField] : null;
      if (!_10 && _15 < _14) {
        _4.showItem("invalidDate");
        return false
      }
      if (!_4.validate()) return false;
      _13[_1.startDateField] = _14;
      _13.isDuration = _10;
      if (_10) {
        _13[_1.durationField] = _11;
        _13[_1.durationUnitField] = _12;
        delete _13[_1.endDateField]
      } else {
        _13[_1.endDateField] = _15;
        delete _13[_1.durationField];
        delete _13[_1.durationUnitField]
      }
      _1.eventEditorLayout.currentStart = _14;
      _1.eventEditorLayout.currentEnd = _1.getEventEndDate(_13);
      _1.eventEditorLayout.hide();
      _1.$1116 = true
    } else if (_5 == "time") {
      var _16 = _6["startAMPM"],
          _17 = _1.twentyFourHourTime ? _1.$534(_6["startHours"], _16) : _6["startHours"],
          _18 = _6["startMinutes"];
      var _19 = _1.eventEditorLayout.currentStart.duplicate();
      _19.setHours(_17);
      _19.setMinutes(_18);
      var _20 = _19.getTime(),
          _21 = isc.DateUtil.getEndOf(_19.duplicate(), "d");
      _13[_1.startDateField] = _19;
      if (_10) {
        var _22 = _21.getTime(),
            _23 = isc.DateUtil.convertPeriodUnit(_11, _12, "ms"),
            _24 = Math.min(_20 + _23, _22);
        if (_24 != _20 + _23) {
          _11 = isc.DateUtil.convertPeriodUnit(_24 - _20, "ms", _12);
          _11 = Math.round(_11)
        }
        _13[this.durationField] = _11;
        _13[this.durationUnitField] = _12
      } else {
        var _25 = _6["endHours"],
            _26 = _6["endMinutes"],
            _27;
        if (!_1.twentyFourHourTime) {
          _27 = _6["endAMPM"];
          _25 = _1.$534(_25, _27);
          if (_25 == 0) _25 = 24
        }
        if (!(_17 < _25 || (_17 == _25 && _18 < _26))) {
          _4.showItem("invalidDate");
          return false
        }
        if (!_4.validate()) return false;
        var _28 = _19.duplicate();
        _28.setHours(_25);
        _28.setMinutes(_26);
        if (_28.getTime() > _21.getTime()) {
          _28 = _21.duplicate()
        }
        _13[_1.endDateField] = _28;
        _1.$1116 = true
      }
    }
    var _29 = isc.addProperties({}, _4.getCustomValues());
    _1.eventEditorLayout.hide();
    if (!_2) {
      _1.updateCalendarEvent(_3, _13, _29)
    } else {
      _1.addCalendarEvent(_13, _29, false)
    }
    return true
  }, isc.A.setDateLabel = function isc_Calendar_setDateLabel() {
    if (!this.dateLabel) return;
    var _1 = "",
        _2 = this.chosenDate,
        _3 = null,
        _4 = this.getCurrentViewName();
    if (_4 == "day") {} else if (_4 == "week") {
      var _5 = this.$656();
      _2 = _5[0];
      _3 = _5[1]
    } else if (_4 == "month") {
      _2 = isc.DateUtil.getStartOf(_2, "M");
      _3 = isc.DateUtil.getEndOf(_2, "M")
    } else if (_4 == "timeline") {
      var _6 = this.timelineView;
      _2 = _6.startDate;
      _3 = _6.endDate
    }
    _1 = this.getDateLabelText(_4, _2, _3);
    this.dateLabel.setContents(_1)
  }, isc.A.getDateLabelText = function isc_Calendar_getDateLabelText(_1, _2, _3) {
    var _4 = "";
    if (_1 == "day") {
      _4 = "<b>" + Date.getFormattedDateRangeString(_2) + "</b>"
    } else if (_1 == "week") {
      _4 = "<b>" + Date.getFormattedDateRangeString(_2, _3) + "</b>"
    } else if (_1 == "month") {
      _4 = "<b>" + _2.getShortMonthName() + " " + _2.getFullYear() + "</b>"
    } else if (_1 == "timeline") {
      var _5 = this.timelineView;
      _4 = "<b>" + _5.formatDateForDisplay(_2) + "</b> through <b>" + _5.formatDateForDisplay(_3) + "</b>"
    }
    return _4
  }, isc.A.$656 = function isc_Calendar__getWeekRange() {
    var _1 = this.chosenWeekStart.duplicate();
    var _2 = this.chosenWeekEnd.duplicate();
    if (!this.showWeekends) {
      var _3 = Date.getWeekendDays();
      var _4 = 7 - _3.length;
      while (_3.contains(_1.getDay())) {
        _1.setDate(_1.getDate() + 1)
      }
      var _5 = 0,
          _6 = _1.duplicate();
      for (var i = 0; i < _4; i++) {
        if (_3.contains(_6.getDay())) _5++;
        _6.setDate(_6.getDate() + 1)
      }
      _2 = _1.duplicate();
      _2.setDate(_2.getDate() + (_4 - 1) + _5)
    }
    return [_1, _2]
  }, isc.A.dayViewSelected = function isc_Calendar_dayViewSelected() {
    if (this.mainView && !this.mainView.isA("TabSet")) return this.mainView.viewName == "day";
    else return this.$567 == "day"
  }, isc.A.weekViewSelected = function isc_Calendar_weekViewSelected() {
    if (this.mainView && !this.mainView.isA("TabSet")) return this.mainView.viewName == "week";
    else return this.$567 == "week"
  }, isc.A.monthViewSelected = function isc_Calendar_monthViewSelected() {
    if (this.mainView && !this.mainView.isA("TabSet")) return this.mainView.viewName == "month";
    else return this.$567 == "month"
  }, isc.A.timelineViewSelected = function isc_Calendar_timelineViewSelected() {
    if (this.mainView && !this.mainView.isA("TabSet")) return this.mainView.viewName == "timeline";
    else return this.$567 == "timeline"
  }, isc.A.showEventDialog = function isc_Calendar_showEventDialog(_1, _2) {
    if (_2 == null) _2 = (_1 == null);
    this.$53l(_1, _2)
  }, isc.A.showNewEventDialog = function isc_Calendar_showNewEventDialog(_1) {
    _1 = _1 || {};
    this.showEventDialog(_1, true)
  }, isc.A.$53l = function isc_Calendar__showEventDialog(_1, _2) {
    _1 = _1 || {};
    var _3 = this.getEventStartDate(_1) || new Date(),
        _4 = this.getEventEndDate(_1),
        _5 = this.getSelectedView(),
        _6 = _5.isMonthView() ? null : _5.getCurrentEventCanvas(_1),
        _7, _8, _9;
    if (!_6) {
      if (this.eventEditorLayout) {
        this.eventEditorLayout.event = _1;
        this.eventEditorLayout.isNewEvent = _2
      }
      this.eventDialog.eventWindow = null;
      this.eventDialog.event = _1;
      this.eventDialog.isNewEvent = _2;
      this.eventDialog.items[0].createFields();
      var _10 = _3,
          _11 = _4;
      _1[this.startDateField] = _10;
      if (_5.isMonthView()) {
        var _12 = new Date();
        _12 = _12.getHours();
        if (_12 > 22) _12 -= 1;
        _10.setHours(_12);
        _1[this.startDateField] = _10
      } else if (_5.isTimelineView()) {
        var _13 = this.timelineView;
        _7 = _13.getEventLaneIndex(_1);
        _8 = _13.body.getEventColumn(_13.getDateLeftOffset(_10));
        _11 = _4 || this.getDateFromPoint(_13.getDateLeftOffset(_10) + _13.getColumnWidth(_8));
        this.eventDialog.setLane(_1[this.laneNameField])
      } else {
        if (_5.isMonthView()) {
          _7 = _5.getEventRow();
          _8 = _5.getEventColumn();
          _11 = _4 || this.getCellDate(_7, _8, _5)
        } else {
          _7 = _3.getHours() * this.getRowsPerHour(_5);
          _7 += Math.floor(_3.getMinutes() / this.getMinutesPerRow());
          if (this.showDayLanes && _5.isDayView()) {
            _8 = _5.getEventLaneIndex(_1)
          } else {
            _8 = _5.getColFromDate(_3)
          }
          _11 = _4 || this.getCellDate(_7, _8, _5)
        }
      }
      _1[this.endDateField] = _11;
      this.eventDialog.setEvent(_1)
    } else {
      if (_5.isTimelineView()) {
        _7 = _5.getEventLaneIndex(_1);
        _8 = _5.body.getEventColumn(_5.getDateLeftOffset(_3))
      } else if (_5.isDayView() || _5.isWeekView()) {
        _7 = _3.getHours() * this.getRowsPerHour(_5);
        _7 += Math.floor(_3.getMinutes() / this.getMinutesPerRow());
        _8 = _5.getColFromDate(_3)
      }
      this.eventDialog.eventWindow = _6;
      this.eventDialog.isNewEvent = false;
      this.eventDialog.items[0].createFields();
      this.eventDialog.setEvent(_6.event);
      _9 = [_6.getPageLeft(), _6.getPageTop()]
    }
    this.eventDialog.moveTo(0, -10000);
    this.displayEventDialog();
    if (!_9) _9 = _5.body.getCellPageRect(_7, _8);
    this.eventDialog.placeNear(_9[0], _9[1]);
    isc.Timer.setTimeout(this.ID + ".eventDialog.bringToFront()")
  }, isc.A.showEventEditor = function isc_Calendar_showEventEditor(_1, _2) {
    if (_2 == null) _2 = (_1 == null);
    this.$53j(_1, _2)
  }, isc.A.showNewEventEditor = function isc_Calendar_showNewEventEditor(_1) {
    this.showEventEditor(_1, true)
  }, isc.A.$53j = function isc_Calendar__showEventEditor(_1, _2) {
    if (!this.eventEditorLayout.isDrawn()) this.eventEditorLayout.draw();
    this.eventEditorLayout.setWidth(this.mainView.getVisibleWidth());
    this.eventEditorLayout.setHeight(this.mainView.getVisibleHeight());
    this.eventEditorLayout.setPageLeft(this.mainView.getPageLeft());
    this.eventEditorLayout.setPageTop(this.getPageTop());
    this.eventEditorLayout.isNewEvent = _2;
    this.eventEditorLayout.view = this.getSelectedView();
    if (_1) {
      this.eventEditorLayout.setEvent(_1)
    } else {
      this.eventEditor.clearValues();
      this.eventEditorLayout.setTitle(this.newEventEditorWindowTitle);
      if (this.eventDialog && this.eventDialog.isVisible()) {
        if (this.eventEditorFields) {
          this.eventEditorLayout.items[0].setCustomValues(this.eventDialog.items[0].getCustomValues())
        }
        var _3 = this.eventDialog.items[0].getValue(this.nameField);
        var _4 = this.eventDialog.items[0].getItem(this.laneNameField);
        var _5 = _4 ? _4.getValue() : null;
        var _6 = new Date();
        this.eventEditorLayout.setDate(_6, this.eventDialog.currentEnd, _3, _5)
      }
    }
    this.hideEventDialog();
    this.eventEditorLayout.show()
  }, isc.A.$53k = function isc_Calendar__getEventDialogTitle(_1, _2) {
    var _3 = Date.getShortDayNames(),
        _4 = Date.getShortMonthNames(),
        _5 = isc.Time.toTime(_1, this.timeFormatter, true),
        _6 = isc.Time.toTime(_2, this.timeFormatter, true),
        _7;
    if (this.isTimeline()) {
      var _8 = (isc.Date.compareLogicalDates(_1, _2) != 0);
      if (_8) {
        _7 = _3[_1.getDay()] + ", " + _4[_1.getMonth()] + " " + _1.getDate() + ", " + _5 + " - " + _3[_2.getDay()] + ", " + _4[_2.getMonth()] + " " + _2.getDate() + ", " + _6;
        return _7
      }
    }
    var _9 = _5 + " - " + _6;
    return _3[_1.getDay()] + ", " + _4[_1.getMonth()] + " " + _1.getDate() + ", " + _9
  }, isc.A.$53n = function isc_Calendar__to12HrNotation(_1) {
    if (_1 == 0) return 12;
    else if (_1 < 13) return _1;
    else return _1 - 12
  }, isc.A.$534 = function isc_Calendar__to24HourNotation(_1, _2) {
    _1 = parseInt(_1);
    if (_2.toLowerCase() == "am" && _1 == 12) {
      return 0
    } else if (_2.toLowerCase() == "pm" && _1 < 12) {
      return _1 + 12
    } else {
      return _1
    }
  }, isc.A.$116j = function isc_Calendar__getCellCSSText(_1, _2, _3, _4) {
    var _5 = this.getCellDate(_3, _4, _1);
    if (!_5) return null;
    var _6 = this.getDateCSSText(_5, _3, _4, _1);
    if (_6) return _6;
    if (this.todayBackgroundColor) {
      var _7 = isc.Date.compareLogicalDates(_5, new Date());
      if ((_7 !== false && _7 == 0)) {
        return "background-color:" + this.todayBackgroundColor + ";"
      }
    }
    return null
  }, isc.A.getDateCSSText = function isc_Calendar_getDateCSSText(_1, _2, _3, _4) {
    return null
  }, isc.A.getDateStyle = function isc_Calendar_getDateStyle(_1, _2, _3, _4) {
    return null
  }, isc.A.getCellDate = function isc_Calendar_getCellDate(_1, _2, _3) {
    _3 = _3 || this.getSelectedView();
    var _4;
    if (_1 == null && _2 == null) {
      _1 = _3.getEventRow();
      _2 = _3.getEventCol()
    }
    var _5 = _3.frozenFields ? _3.frozenFields.length : 0;
    if (_3.isDayView() || _3.isWeekView() || _3.isTimelineView()) {
      var _6 = _2 - _5;
      _4 = _6 >= 0 ? _3.getCellDate(_1, _6) : null
    } else if (_3.isMonthView()) {
      if (_2 >= _3.getFields().length) _2 = _3.getFields().length - 1;
      var _7 = _3.data.get(_1);
      var _8 = _3.getField(_2).$66b;
      if (_7 && _7["date" + _8] != null) {
        _4 = _7["date" + _8].duplicate();
        _4.setHours(0);
        _4.setMinutes(0);
        _4.setSeconds(0)
      }
    } else {
      return
    }
    return _4
  }, isc.A.getDateFromPoint = function isc_Calendar_getDateFromPoint(_1, _2, _3, _4) {
    _4 = _4 || this.getSelectedView();
    if (_3 == null) _3 = true;
    if (_4.getDateFromPoint) return _4.getDateFromPoint(_1, _2, null, _3);
    if (_1 == null && _2 == null) {
      _1 = _4.body.getOffsetX();
      _2 = _4.body.getOffsetY()
    }
    var _5 = _4.body.getEventColumn(_1),
        _6 = _4.body.getEventRow(_2),
        _7;
    if (_4.isMonthView()) {
      _7 = this.getCellDate(_6, _5, _4)
    } else {
      return
    }
    return _7
  }, isc.A.getLane = function isc_Calendar_getLane(_1, _2) {
    if (!_1) return null;
    _2 = _2 || this.getSelectedView();
    if (_2.getLane) return _2.getLane(_1);
    return null
  }, isc.A.getEventLane = function isc_Calendar_getEventLane(_1, _2) {
    if (!_1) return null;
    return this.getLane(_1[this.laneNameField], _2)
  }, isc.A.getSublane = function isc_Calendar_getSublane(_1, _2, _3) {
    if (!_1) return null;
    _3 = _3 || this.getSelectedView();
    if (_3.getSublane) return _3.getSublane(_1, _2);
    return null
  }, isc.A.getEventSublane = function isc_Calendar_getEventSublane(_1, _2) {
    if (!_1) return null;
    return this.getSublane(_1[this.laneNameField], _1[this.sublaneNameField], _2)
  }, isc.A.getLaneFromPoint = function isc_Calendar_getLaneFromPoint(_1, _2, _3) {
    _3 = _3 || this.getSelectedView();
    if (!_3.hasLanes()) return null;
    if (_3.getLaneFromPoint) return _3.getLaneFromPoint(_1, _2);
    return null
  }, isc.A.getSublaneFromPoint = function isc_Calendar_getSublaneFromPoint(_1, _2, _3) {
    _3 = _3 || this.getSelectedView();
    if (_3.getSublaneFromPoint) return _3.getSublaneFromPoint(_1, _2);
    return null
  }, isc.A.getDateLeftOffset = function isc_Calendar_getDateLeftOffset(_1, _2) {
    if (_2 && _2.getDateLeftOffset) return _2.getDateLeftOffset(_1)
  }, isc.A.monthViewEventClick = function isc_Calendar_monthViewEventClick(_1, _2, _3) {
    var _4 = this.monthView.getEvents(_1, _2);
    var _5 = _4[_3];
    if (this.eventClick(_5, "month")) this.showEventEditor(_5)
  }, isc.A.currentViewChanged = function isc_Calendar_currentViewChanged(_1) {}, isc.A.getDayBodyHTML = function isc_Calendar_getDayBodyHTML(_1, _2, _3, _4, _5) {
    var _6 = _1.getDay();
    var _7 = _2,
        _8 = 15,
        _9 = this.monthView.data ? this.monthView.data[1] : null,
        _10 = this.monthView.getRowHeight(_9, 1);
    var _11 = "";
    for (var i = 0; i < _7.length; i++) {
      var _13 = isc.Time.toTime(this.getEventStartDate(_7[i]), this.timeFormatter, true) + " ";
      if (this.canEditEvent(_7[i])) {
        var _14 = "<a href='javascript:" + this.ID + ".monthViewEventClick(" + _4 + "," + _5 + "," + i + ");' class='" + this.calMonthEventLinkStyle + "'>";
        _11 += _14 + _13 + _7[i][this.nameField] + "</a><br/>"
      } else {
        _11 += _13 + _7[i][this.nameField] + "<br/>"
      }
      if ((i + 3) * _8 > _10) break
    }
    if (i < _7.length - 1) {
      _11 += "+ " + (_7.length - 1 - i) + " more..."
    }
    return _11
  }, isc.A.getMonthViewHoverHTML = function isc_Calendar_getMonthViewHoverHTML(_1, _2) {
    if (_2 != null) {
      var _3 = "";
      var _4 = this.creator || this;
      for (var i = 0; i < _2.length; i++) {
        var _6 = isc.Time.toTime(_4.getEventStartDate(_2[i]), _4.timeFormatter, true);
        _3 += _6 + " " + _2[i][_4.nameField] + "<br/>"
      }
      return _3
    }
  }, isc.A.getDayHeaderHTML = function isc_Calendar_getDayHeaderHTML(_1, _2, _3, _4, _5) {
    return _1.getDate()
  }, isc.A.dayBodyClick = function isc_Calendar_dayBodyClick(_1, _2, _3, _4, _5) {
    return true
  }, isc.A.dayHeaderClick = function isc_Calendar_dayHeaderClick(_1, _2, _3, _4, _5) {
    return true
  }, isc.A.eventClick = function isc_Calendar_eventClick(_1, _2) {
    return true
  }, isc.A.$129v = function isc_Calendar__eventCanvasClick(_1) {
    var _2 = _1.event,
        _3 = _1.calendarView,
        _4 = _3.isWeekView(),
        _5 = this.eventClick(_2, _3.viewName);
    if (_5) {
      if (!this.canEditEvent(_2)) return;
      this.clearViewSelection();
      if (!_3.isTimelineView()) {
        var _6 = this.getEventStartDate(_2);
        var _7 = (_3.frozenFields ? _3.frozenFields.length : 0);
        var _8 = _4 ? _6.getDay() - this.firstDayOfWeek + _7 : _7;
        if (_4 && this.showWeekends == false) _8--;
        var _9 = _6.getHours() * this.getRowsPerHour()
      }
      this.showEventDialog(_2)
    }
  }, isc.A.eventRemoveClick = function isc_Calendar_eventRemoveClick(_1, _2) {
    return true
  }, isc.A.eventMoved = function isc_Calendar_eventMoved(_1, _2, _3) {
    return true
  }, isc.A.eventResized = function isc_Calendar_eventResized(_1, _2) {
    return true
  }, isc.A.timelineEventMoved = function isc_Calendar_timelineEventMoved(_1, _2, _3, _4) {
    return true
  }, isc.A.timelineEventResized = function isc_Calendar_timelineEventResized(_1, _2, _3) {
    return true
  }, isc.A.getValidSnapDate = function isc_Calendar_getValidSnapDate(_1, _2) {
    if (this.isTimeline()) {} else {
      var _3 = this.eventSnapGap;
      var _4 = ((_1.getHours() * 60) + _1.getMinutes()) % _3;
      var _5 = (_2.getHours() * 60) + _2.getMinutes();
      var _6 = Math.round((_5 - _4) / _3);
      var _7 = (_6 * _3) + _4;
      var _8 = Math.floor(_7 / 60),
          _9 = _7 % 60;
      _2.setHours(_8);
      _2.setMinutes(_9)
    }
    return _2
  }, isc.A.selectTab = function isc_Calendar_selectTab(_1) {
    if (this.mainView && this.mainView.isA("TabSet") && this.mainView.tabs.getLength() > _1) {
      this.mainView.selectTab(_1);
      this.refreshSelectedView();
      return true
    } else {
      return false
    }
  }, isc.A.parentResized = function isc_Calendar_parentResized() {
    this.Super('parentResized', arguments);
    if (this.eventEditorLayout.isVisible()) this.eventEditorLayout.sizeMe()
  }, isc.A.dateChanged = function isc_Calendar_dateChanged() {
    return true
  }, isc.A.getActiveDay = function isc_Calendar_getActiveDay() {
    var _1 = this.getActiveTime();
    if (_1) return _1.getDay()
  }, isc.A.getActiveTime = function isc_Calendar_getActiveTime() {
    var _1 = this.ns.EH,
        _2 = this.getSelectedView();
    var _3 = _2.getEventRow();
    var _4 = _2.getEventColumn();
    return this.getCellDate(_3, _4, _2)
  }, isc.A.setTimelineRange = function isc_Calendar_setTimelineRange(_1, _2, _3, _4, _5, _6, _7) {
    if (this.timelineView) this.timelineView.setTimelineRange(_1, _2, _3, _4, _5, _6);
    if (_7) this.fireCallback(_7)
  }, isc.A.setResolution = function isc_Calendar_setResolution(_1, _2, _3, _4, _5) {
    if (this.timelineView) {
      _4 = _4 || 1;
      this.timelineView.setTimelineRange(this.startDate, null, _2, _3, _4, _1)
    }
    if (_5) this.fireCallback(_5)
  }, isc.A.getEventLength = function isc_Calendar_getEventLength(_1, _2) {
    var _3 = _1.eventLength,
        _4 = isc.DateUtil;
    if (_3 == null) {
      _3 = _4.getPeriodLength(this.getEventStartDate(_1), this.getEventEndDate(_1));
      _1.eventLength = _3
    }
    if (_2) {
      return _4.convertPeriodUnit(_1.eventLength, "ms", _2)
    }
    return _1.eventLength
  }, isc.A.canEditEventLane = function isc_Calendar_canEditEventLane(_1, _2) {
    var _3 = _1[this.canEditLaneField] != null ? _1[this.canEditLaneField] : this.canEditLane != false;
    return _3
  }, isc.A.canEditEventSublane = function isc_Calendar_canEditEventSublane(_1, _2) {
    if (!this.useSublanes) return false;
    var _3 = _1[this.canEditSublaneField];
    if (_3 == null) _3 = (this.canEditSublane != false);
    return _3
  }, isc.A.eventRepositionMove = function isc_Calendar_eventRepositionMove(_1, _2, _3) {
    return true
  }, isc.A.eventRepositionStop = function isc_Calendar_eventRepositionStop(_1, _2, _3, _4) {
    return true
  }, isc.A.eventResizeMove = function isc_Calendar_eventResizeMove(_1, _2, _3) {
    return true
  }, isc.A.eventResizeStop = function isc_Calendar_eventResizeStop(_1, _2, _3, _4) {
    return true
  }, isc.A.checkForOverlap = function isc_Calendar_checkForOverlap(_1, _2, _3, _4, _5, _6) {
    var _7 = {},
        _8 = this.startDateField,
        _9 = this.endDateField;
    _7[_8] = _4.duplicate();
    _7[_9] = _5.duplicate();
    _7[this.laneNameField] = _6;
    var _10 = this.data;
    if (_6) {
      _10 = this.getLaneEvents(_6, _1)
    }
    var _11 = _1.findOverlappingEvents(_3, _7, null, (_6 != null), _10);
    if (_11.length == 0) {
      return false
    } else if (_11.length > 1) {
      return true
    } else {
      var _12 = _11[0];
      if ((this.equalDatesOverlap == false ? _5 > _12[_8] : _5 >= _12[_8]) && _4 < _12[_8]) {
        _5 = _12[_8].duplicate();
        _4 = _5.duplicate();
        _4.setMinutes(_4.getMinutes() - this.getEventLength(_3, "minute"));
        return [_4, _5]
      } else if ((this.equalDatesOverlap == false ? _4 < _12[_9] : _4 <= _12[_9]) && _5 > _12[_9]) {
        _4 = _12[_9].duplicate();
        _5 = _4.duplicate();
        _5.setMinutes(_5.getMinutes() + this.getEventLength(_3, "minute"));
        return [_4, _5]
      } else {
        return true
      }
    }
  });
  isc.B._maxIndex = isc.C + 189;
  isc.ClassFactory.defineClass("EventWindow", "Window");
  isc.EventWindow.changeDefaults("resizerDefaults", {
    overflow: "hidden",
    height: 6,
    snapTo: "B",
    canDragResize: true
  });
  isc.EventWindow.changeDefaults("headerDefaults", {
    layoutMargin: 0,
    layoutLeftMargin: 3,
    layoutRightMargin: 3
  });
  isc.A = isc.EventWindow.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.autoDraw = false;
  isc.A.minHeight = 5;
  isc.A.minWidth = 5;
  isc.A.showHover = true;
  isc.A.canHover = true;
  isc.A.hoverWidth = 200;
  isc.A.canDragResize = true;
  isc.A.canDragReposition = true;
  isc.A.resizeFrom = ["B"];
  isc.A.showShadow = false;
  isc.A.showEdges = false;
  isc.A.showHeaderBackground = false;
  isc.A.useBackMask = false;
  isc.A.keepInParentRect = true;
  isc.A.headerProperties = {
    padding: 0,
    margin: 0,
    height: 14
  };
  isc.A.closeButtonProperties = {
    height: 10,
    width: 10
  };
  isc.A.bodyColor = null;
  isc.A.showHeaderIcon = false;
  isc.A.showMinimizeButton = false;
  isc.A.showMaximimumButton = false;
  isc.A.showFooter = true;
  isc.A.baseStyle = "eventWindow";
  isc.A.dragAppearance = "none";
  isc.A.$125c = {
    overflow: "hidden",
    defaultLayoutAlign: "center",
    height: 7
  };
  isc.B.push(isc.A.initWidget = function isc_EventWindow_initWidget() {
    this.descriptionText = this.event[this.calendar.descriptionField];
    this.showHeader = this.calendar.showEventDescriptions;
    this.showBody = this.calendar.showEventDescriptions;
    this.footerProperties = isc.addProperties({
      dragTarget: this.eventDragTarget
    }, this.footerProperties, this.$125c);
    if (this.bodyConstructor == null) this.bodyConstructor = isc.HTMLFlow;
    if (this.calendar.showEventDescriptions != false) {
      this.bodyProperties = isc.addProperties({}, this.bodyProperties, {
        contents: this.descriptionText,
        valign: "top",
        overflow: "hidden"
      })
    }
    if (this.calendar.showEventBody == false) {
      this.showBody = false;
      this.showTitle = false
    }
    this.Super("initWidget", arguments);
    if (this.calendar.showEventDescriptions == false) {
      var _1 = isc.Label.create({
        autoDraw: true,
        border: "0px",
        padding: 3,
        height: 1,
        width: 1,
        backgroundColor: this.event.backgroundColor,
        textColor: this.event.textColor,
        setContents: function (_2) {
          this.$116k = _2;
          this.Super("setContents", arguments)
        },
        canHover: true,
        showHover: true,
        eventCanvas: this,
        getHoverHTML: function () {
          return this.eventCanvas.getHoverHTML()
        },
        redrawWithParent: true
      });
      _1.addMember = function (_2) {
        this.addChild(_2)
      };
      _1.addChild(this.resizer);
      this.addChild(_1);
      this.header = _1;
      this.$128l = _1;
      this.$77n = true
    }
    this.setEventStyle(this.baseStyle)
  }, isc.A.getEvent = function isc_EventWindow_getEvent() {
    return this.event
  }, isc.A.getCalendar = function isc_EventWindow_getCalendar() {
    return this.calendar
  }, isc.A.getCalendarView = function isc_EventWindow_getCalendarView() {
    return this.calendarView
  }, isc.A.setDragProperties = function isc_EventWindow_setDragProperties(_1, _2, _3) {
    this.canDragResize = _2 == null ? true : _2;
    if (_1 == null) _1 = true;
    this.dragTarget = _3;
    this.setCanDragReposition(_1, _3);
    if (this.canDragResize) {
      if (!this.resizer) this.makeFooter();
      else if (!this.resizer.isVisible()) this.resizer.show()
    } else {
      if (this.resizer && this.resizer.isVisible()) this.resizer.hide()
    }
  }, isc.A.setEventStyle = function isc_EventWindow_setEventStyle(_1, _2, _3) {
    _2 = _2 || this.headerStyle || _1 + "Header";
    _3 = _3 || this.bodyStyle || _1 + "Body";
    this.baseStyle = _1;
    this.styleName = _1;
    this.bodyStyle = _3;
    this.headerStyle = _2;
    this.setStyleName(_1);
    if (this.header) this.header.setStyleName(this.headerStyle);
    if (this.headerLabel) {
      this.headerLabel.setStyleName(this.headerStyle)
    } else {
      this.headerLabelProperties = isc.addProperties({}, this.headerLabelProperties, {
        styleName: this.headerStyle
      })
    }
    if (this.body) this.body.setStyleName(this.bodyStyle);
    if (this.$128l) this.$128l.setStyleName(this.bodyStyle)
  }, isc.A.mouseUp = function isc_EventWindow_mouseUp() {
    return isc.EH.STOP_BUBBLING
  }, isc.A.makeFooter = function isc_EventWindow_makeFooter() {
    if (!this.showFooter || this.canDragResize == false) return;
    var _1 = {
      dragTarget: this.dragTarget,
      styleName: this.baseStyle + "Resizer"
    };
    if (this.$77n) _1.snapTo = "B";
    this.resizer = this.createAutoChild("resizer", _1);
    if (this.$77n) {
      this.header.addChild(this.resizer)
    } else {
      this.addChild(this.resizer)
    }
    if (this.resizer) this.resizer.bringToFront()
  }, isc.A.setDescriptionText = function isc_EventWindow_setDescriptionText(_1) {
    if (this.calendar.getDescriptionText) {
      _1 = this.calendar.getDescriptionText(this.event)
    }
    if (_1) {
      if (this.body) {
        this.descriptionText = _1;
        this.body.setContents(_1)
      } else {
        this.descriptionText = _1;
        if (this.$734) {
          this.$734.setWidth("100%");
          this.$734.setContents(_1)
        } else if (this.calendar.showEventDescriptions == false) {
          this.$128l.setContents(_1);
          this.$128l.redraw()
        }
      }
    }
  }, isc.A.click = function isc_EventWindow_click() {
    if (this.$53u) return;
    if (this.$67e) {
      this.$67e = null;
      return
    }
    var _1 = this.calendar;
    var _2 = _1.eventClick(this.event, this.$53i ? "week" : "day");
    if (_2) {
      if (!_1.canEditEvent(this.event)) return;
      _1.clearViewSelection();
      var _3 = (this.$53i && _1.weekView.isLabelCol(0) ? 1 : 0);
      var _4 = this.$53i ? _1.getEventStartDate(this.event).getDay() - _1.firstDayOfWeek + _3 : _3;
      if (this.$53i && _1.showWeekends == false) _4--;
      _1.showEventDialog(this.event)
    }
  }, isc.A.mouseDown = function isc_EventWindow_mouseDown() {
    if (this.dragTarget) this.dragTarget.eventCanvas = this;
    this.calendar.eventDialog.hide();
    return isc.EH.STOP_BUBBLING
  }, isc.A.renderEvent = function isc_EventWindow_renderEvent(_1, _2, _3, _4) {
    var _5 = this.calendar,
        _6 = this.event;
    if (isc.isA.Number(_3) && isc.isA.Number(_4)) {
      this.resizeTo(Math.round(_3), Math.round(_4))
    }
    if (isc.isA.Number(_1) && isc.isA.Number(_2)) {
      this.moveTo(Math.round(_2), Math.round(_1))
    }
    var _7 = _5.getEventHeaderHTML(_6, this.calendarView),
        _8 = _7,
        _9 = "";
    if (_6.headerBackgroundColor) _9 += "backgroundColor: " + _6.headerBackgroundColor + ";";
    if (_6.headerTextColor) _9 += "backgroundColor: " + _6.headerTextColor + ";";
    if (_9 != "") _8 = "<span style='" + _9 + "'>" + _8 + "<span>";
    this.setTitle(_8);
    this.updateColors(_7);
    if (this.$77n) {
      this.header.resizeTo(Math.round(_3), Math.round(_4));
      this.header.setContents(_8)
    }
    if (!this.isDrawn()) this.draw();
    this.show();
    this.bringToFront()
  }, isc.A.updateColors = function isc_EventWindow_updateColors(_1) {
    var _2 = this.calendar,
        _3 = this.event,
        _4 = this.header,
        _5 = _4 ? _4.getMember ? _4.getMember(0) : _4 : null,
        _6 = _5,
        _7 = _1 || _2.getEventHeaderHTML(_3, this.calendarView);
    if (!_3) return;
    if (_5 && _5.children && _5.children[0]) {
      var _8 = _5.children[0].members;
      if (_8 && _8.length > 0) _6 = _8[0]
    }
    if (_3.backgroundColor) {
      this.setBackgroundColor(_3.backgroundColor);
      if (this.body) this.body.setBackgroundColor(_3.backgroundColor)
    } else {
      this.backgroundColor = null;
      if (this.isDrawn() && this.getStyleHandle()) {
        this.getStyleHandle().backgroundColor = null
      }
      if (this.body) {
        this.body.backgroundColor = null;
        if (this.body.isDrawn() && this.body.getStyleHandle()) {
          this.body.getStyleHandle().backgroundColor = null
        }
      }
      if (_6) {
        _6.backgroundColor = null;
        if (_6.isDrawn() && _6.getStyleHandle()) {
          _6.getStyleHandle().backgroundColor = null
        }
      }
    }
    if (_3.textColor) {
      this.setTextColor(_3.textColor);
      if (this.body) {
        var _9 = "color:" + _3.textColor + ";"
        this.body.setTextColor(_3.textColor);
        this.body.setContents("<span style='" + _9 + "'>" + _3[_2.descriptionField] || "</span>")
      }
    } else {
      if (this.textColor) {
        this.setTextColor(null);
        if (this.isDrawn() && this.getStyleHandle()) {
          this.getStyleHandle().color = null
        }
        if (this.body) {
          this.body.setTextColor(null);
          this.body.setContents(_3[_2.descriptionField])
        }
        if (_6) {
          _6.setTextColor(null);
          _6.setContents(_7)
        }
        if (this.$128l) {
          this.$128l.setTextColor(null);
          this.$128l.setContents(_7)
        }
      }
    }
    if (this.header) {
      var _10, _11;
      if (_2.showEventDescriptions == false) {
        _10 = _3.backgroundColor;
        _11 = _3.textColor
      } else {
        _10 = _3.headerBackgroundColor;
        _11 = _3.headerTextColor
      }
      if (_10) {
        this.header.setBackgroundColor(_10);
        if (_6) _6.setBackgroundColor(_10)
      } else {
        this.header.backgroundColor = null;
        if (this.isDrawn() && this.header.getStyleHandle()) {
          this.header.getStyleHandle().backgroundColor = null
        }
        if (_6) {
          _6.backgroundColor = null;
          if (_6.getStyleHandle()) {
            _6.getStyleHandle().backgroundColor = null
          }
        }
      }
      if (_11) {
        this.header.setTextColor(_11);
        var _9 = "color:" + _11 + ";",
            _12 = _2.showEventDescriptions == false ? this.header.$116k : _7,
            _13 = "<span style='" + _9 + "'>" + _12 + "</span>";
        if (!_6) {
          if (this.header.setContents) this.header.setContents(_13)
        } else {
          _6.setTextColor(_11);
          _6.setContents(_13)
        }
      } else {
        if (this.header.textColor) {
          this.header.setTextColor(null);
          if (this.isDrawn() && this.header.getStyleHandle()) {
            this.header.getStyleHandle().color = null
          }
          if (_6) {
            _6.setTextColor(null);
            if (_6.isDrawn() && _6.getStyleHandle()) {
              _6.getStyleHandle().color = null
            }
          }
        }
      }
      this.markForRedraw()
    }
  }, isc.A.getPrintHTML = function isc_EventWindow_getPrintHTML(_1, _2) {
    var _3 = isc.StringBuffer.create(),
        _4 = this.calendar,
        _5 = _4.isTimeline(),
        _6 = this.parentElement,
        _7 = _6.grid,
        _8 = 40 + _7.getHeaderHeight(),
        _9 = this.getTop(),
        _10 = _6.getPageTop(),
        _11 = (_9) + _8 + 1,
        _12 = _6.$26a,
        _13 = _7.getLeft() + _6.getLeft() + (_7.getEventLeft ? _7.getEventLeft(this.event) : _4.getEventLeft(this.event, _7)),
        _14 = this.getVisibleWidth(),
        _15 = this.getVisibleHeight() - 2,
        i = (_1 && _1.i ? _1.i : 1);
    var _17 = _4.getEventStartCol(this.event, this, this.calendarView),
        _18 = _4.getEventEndCol(this.event, this, this.calendarView);
    if (_5) {
      _13 += (14 + ((_17 - 1) * 2));
      _14 += _18 - _17
    } else {
      _13 += _7.$53i ? 6 : 8
    }
    var _19 = _5 ? this.baseStyle : this.body.styleName;
    _3.append("<div class='", _19, "' ", "style='border: 1px solid grey; vertical-align: ", (_4.showEventDescriptions ? "top" : "middle"), "; ", (_5 ? "overflow:hidden; " : ""), "position: absolute; ", "left:", _13, "; top:", _11, "; width: ", _14, "; height: ", _15, "; ", "z-index:", i + 2, ";'>");
    if (_4.showEventDescriptions) {
      _3.append(this.title, "<br>", this.event[_4.descriptionField])
    } else {
      _3.append(this.title)
    }
    _3.append("</div>");
    var _20 = _3.toString();
    return _20
  }, isc.A.getHoverHTML = function isc_EventWindow_getHoverHTML() {
    return this.calendar.getEventHoverHTML(this.event, this, this.calendarView)
  }, isc.A.closeClick = function isc_EventWindow_closeClick() {
    var _1 = this.calendar;
    if (_1.eventRemoveClick(this.event) == false) {
      this.$67e = true;
      return
    }
    this.Super("closeClick", arguments);
    this.calendar.removeEvent(this.event, true);
    this.$53u = true
  }, isc.A.parentResized = function isc_EventWindow_parentResized() {
    this.Super('parentResized', arguments);
    if (this.event) this.calendarView.sizeEventCanvas(this)
  }, isc.A.getEventLength = function isc_EventWindow_getEventLength() {
    return this.event.eventLength
  }, isc.A.show = function isc_EventWindow_show() {
    this.Super("show", arguments)
  }, isc.A.resized = function isc_EventWindow_resized() {
    if (this.$77n) {
      this.header.resizeTo(this.getVisibleWidth(), this.getVisibleHeight())
    }
  });
  isc.B._maxIndex = isc.C + 20;
  isc.ClassFactory.defineClass("TimelineWindow", "EventWindow");
  isc.A = isc.TimelineWindow.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.showFooter = false;
  isc.A.resizeFrom = ["L", "R"];
  isc.A.dragAppearance = "none";
  isc.B.push(isc.A.initWidget = function isc_TimelineWindow_initWidget() {
    if (this.calendar.showEventWindowHeader == false) {
      this.showBody = false;
      this.showTitle = false
    }
    this.Super("initWidget", arguments)
  }, isc.A.draw = function isc_TimelineWindow_draw(_1, _2, _3, _4) {
    this.invokeSuper(isc.TimelineWindow, "draw", _1, _2, _3, _4);
    if (this.calendar.showEventWindowHeader == false) {
      var _5 = isc.Canvas.create({
        autoDraw: false,
        width: "100%",
        height: 0,
        top: 0,
        contents: (this.descriptionText ? this.descriptionText : " "),
        backgroundColor: this.event.backgroundColor,
        textColor: this.event.textColor
      });
      if (this.body) this.body.addMember(_5);
      else this.addMember(_5);
      _5.setHeight("100%");
      this.$734 = _5
    }
  }, isc.A.click = function isc_TimelineWindow_click() {
    var _1 = this.calendar,
        _2 = _1.timelineView,
        _3 = _1.eventClick(this.event, "timeline");
    if (_3) {
      if (!_1.canEditEvent(this.event)) return;
      _1.showEventDialog(this.event)
    } else return isc.EH.STOP_BUBBLING
  }, isc.A.destroyLines = function isc_TimelineWindow_destroyLines() {
    if (this.$645) {
      if (this.$645[0]) this.$645[0].destroy();
      if (this.$645[1]) this.$645[1].destroy();
      if (this.$645[2]) this.$645[2].destroy();
      if (this.$645[3]) this.$645[3].destroy()
    }
  }, isc.A.hideLines = function isc_TimelineWindow_hideLines() {
    if (this.$645) {
      if (this.$645[0]) this.$645[0].hide();
      if (this.$645[1]) this.$645[1].hide();
      if (this.$645[2]) this.$645[2].hide();
      if (this.$645[3]) this.$645[3].hide()
    }
  }, isc.A.showLines = function isc_TimelineWindow_showLines() {
    if (this.$645) {
      if (this.$645[0]) this.$645[0].show();
      if (this.$645[1]) this.$645[1].show();
      if (this.$645[2]) this.$645[2].show();
      if (this.$645[3]) this.$645[3].show()
    }
  }, isc.A.hide = function isc_TimelineWindow_hide() {
    this.invokeSuper(isc.TimelineWindow, "hide");
    this.hideLines()
  }, isc.A.show = function isc_TimelineWindow_show() {
    this.invokeSuper(isc.TimelineWindow, "show");
    this.showLines()
  }, isc.A.parentResized = function isc_TimelineWindow_parentResized() {
    this.invokeSuper(isc.EventWindow, "parentResized")
  });
  isc.B._maxIndex = isc.C + 9;
  isc.Calendar.registerStringMethods({
    getDayBodyHTML: "date,events,calendar,rowNum,colNum",
    getDayHeaderHTML: "date,events,calendar,rowNum,colNum",
    dayBodyClick: "date,events,calendar,rowNum,colNum",
    dayHeaderClick: "date,events,calendar,rowNum,colNum",
    eventClick: "event,viewName",
    eventChanged: "event",
    eventMoved: "newDate,event",
    eventResized: "newDate,event",
    backgroundClick: "startDate,endDate",
    backgroundMouseDown: "startDate",
    backgroundMouseUp: "startDate,endDate"
  });
  isc.defineClass("EventCanvas", "VLayout");
  isc.A = isc.EventCanvas.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.autoDraw = false;
  isc.A.overflow = "hidden";
  isc.A.minHeight = 1;
  isc.A.minWidth = 1;
  isc.A.showHover = true;
  isc.A.canHover = true;
  isc.A.hoverWidth = 200;
  isc.A.snapToGrid = false;
  isc.A.keepInParentRect = true;
  isc.A.dragAppearance = "none";
  isc.A.canDragResize = true;
  isc.A.canDragReposition = true;
  isc.A.vertical = true;
  isc.A.headerPosition = "header";
  isc.A.bodyHeight = "auto";
  isc.A.divTemplate = ["<div class='", , "' style='", , ";'>", , "</div>"];
  isc.A.showRolloverControls = true;
  isc.B.push(isc.A.getShowHeader = function isc_EventCanvas_getShowHeader() {
    if (this.showHeader != null) return this.showHeader;
    return this.calendar.showEventHeaders
  }, isc.A.getShowBody = function isc_EventCanvas_getShowBody() {
    if (this.showBody != null) return this.showBody;
    return this.calendar.showEventDescriptions
  }, isc.A.initWidget = function isc_EventCanvas_initWidget() {
    if (this.vertical) this.resizeFrom = ["B"];
    else this.resizeFrom = ["L", "R"];
    this.Super("initWidget", arguments);
    if (this.event) this.setEvent(this.event, this.styleName);
    if (!this.calendar.useEventCanvasRolloverControls) {
      this.calendar.showEventCanvasRolloverControls(this)
    }
  }, isc.A.setEvent = function isc_EventCanvas_setEvent(_1, _2, _3, _4) {
    this.event = _1;
    var _5 = this.calendar,
        _6 = _5.canEditEvent(_1),
        _7 = _5.canDragEvent(_1),
        _8 = _5.canResizeEvent(_1),
        _9 = _5.canRemoveEvent(_1);
    this.showCloseButton = _9;
    this.canDragReposition = _7;
    this.canDragResize = _8;
    _2 = _2 || _5.getEventCanvasStyle(_1, this.calendarView);
    this.setEventStyle(_2, _3, _4)
  }, isc.A.setDragProperties = function isc_EventCanvas_setDragProperties(_1, _2, _3) {
    this.canDragReposition = _1 == null ? true : _1;
    this.canDragResize = _2 == null ? true : _2;
    this.dragTarget = _3
  }, isc.A.setEventStyle = function isc_EventCanvas_setEventStyle(_1, _2, _3) {
    _2 = _2 || this.headerStyle || (_1 + "Header");
    _3 = _3 || this.bodyStyle || (_1 + "Body");
    this.baseStyle = _1;
    this.styleName = _1;
    this.$129w = _3;
    this.$7m = _2;
    this.setStyleName(_1)
  }, isc.A.getStartDate = function isc_EventCanvas_getStartDate() {
    return this.calendar.getEventStartDate(this.event)
  }, isc.A.getEndDate = function isc_EventCanvas_getEndDate() {
    return this.calendar.getEventEndDate(this.event)
  }, isc.A.getDuration = function isc_EventCanvas_getDuration() {
    return this.event[this.calendar.durationField]
  }, isc.A.getEventLength = function isc_EventCanvas_getEventLength(_1) {
    if (this.event.eventLength) return this.event.eventLength;
    return this.calendar.getEventLength(this.event, _1 || "minute")
  }, isc.A.getHeaderWrap = function isc_EventCanvas_getHeaderWrap() {
    if (this.headerWrap != null) return this.headerWrap;
    return this.calendar.eventHeaderWrap
  }, isc.A.getHeaderHeight = function isc_EventCanvas_getHeaderHeight() {
    var _1 = !this.getShowBody() ? "100%" : (this.getHeaderWrap() ? "auto" : this.headerHeight != null ? this.headerHeight : this.calendar.eventHeaderHeight + "px");
    return _1
  }, isc.A.getHeaderStyle = function isc_EventCanvas_getHeaderStyle() {
    return this.$7m
  }, isc.A.getHeaderHTML = function isc_EventCanvas_getHeaderHTML() {
    if (!this.event) {
      return "No event"
    }
    return this.calendar.getEventHeaderHTML(this.event, this.calendarView)
  }, isc.A.getHeaderCSSText = function isc_EventCanvas_getHeaderCSSText() {
    var _1 = this.event,
        _2 = this.getHeaderHeight(),
        _3 = isc.StringBuffer.create();
    _3.append("overflow:hidden;vertical-align:middle;width:auto;height:", _2, (this.getHeaderWrap() ? "" : ";text-wrap:none"), (this.getShowBody() ? "" : ";text-align:center"), (this.headerPosition != "footer" ? "" : ";vertical-align:bottom"));
    if (_1.headerTextColor) _3.append(";color:", _1.headerTextColor);
    if (_1.headerBackgroundColor) {
      _3.append(";background-color:", _1.headerBackgroundColor)
    }
    var _4 = _3.release();
    return _4
  }, isc.A.getBodyStyle = function isc_EventCanvas_getBodyStyle() {
    return this.$129w
  }, isc.A.getBodyHTML = function isc_EventCanvas_getBodyHTML() {
    if (!this.event) {
      return ""
    }
    return this.calendar.getEventBodyHTML(this.event, this.calendarView)
  }, isc.A.getBodyCSSText = function isc_EventCanvas_getBodyCSSText() {
    var _1 = this.event,
        _2 = isc.StringBuffer.create();
    _2.append("width:auto;height:", this.bodyHeight);
    if (_1.textColor) _2.append(";color:", _1.textColor);
    if (_1.backgroundColor) {
      _2.append(";background-color:", _1.backgroundColor)
    }
    var _3 = _2.release();
    return _3
  }, isc.A.getInnerHTML = function isc_EventCanvas_getInnerHTML() {
    var _1 = "",
        _2 = "",
        _3 = "";
    if (this.event) {
      if (this.getShowHeader()) {
        var _4 = this.divTemplate.duplicate();
        _4[1] = this.getHeaderStyle();
        _4[3] = this.getHeaderCSSText();
        _4[5] = this.getHeaderHTML();
        _2 = _4.join("")
      }
      if (this.getShowBody()) {
        var _5 = this.divTemplate.duplicate();
        _5[1] = this.getBodyStyle();
        _5[3] = this.getBodyCSSText();
        _5[5] = this.getBodyHTML();
        _3 += _5.join("")
      }
      if (this.headerPosition == "header") _1 = _2 + _3;
      else if (this.headerPosition == "body") _1 = _2;
      else if (this.headerPosition == "footer") _1 = _2;
      if (!this.getShowHeader() && !this.getShowBody()) {
        _1 = this.getHeaderHTML()
      }
    }
    return _1
  }, isc.A.getHoverHTML = function isc_EventCanvas_getHoverHTML() {
    return this.calendar.getEventHoverHTML(this.event, this, this.calendarView)
  }, isc.A.shouldShowCloseButton = function isc_EventCanvas_shouldShowCloseButton() {
    return this.showCloseButton != false
  }, isc.A.shouldShowContextButton = function isc_EventCanvas_shouldShowContextButton() {
    return this.showContextButton != false
  }, isc.A.getRolloverControls = function isc_EventCanvas_getRolloverControls() {
    return null
  }, isc.A.renderEvent = function isc_EventCanvas_renderEvent(_1, _2, _3, _4, _5) {
    if (isc.isA.Number(_3) && isc.isA.Number(_4)) {
      this.resizeTo(Math.round(_3), Math.round(_4))
    }
    if (isc.isA.Number(_1) && isc.isA.Number(_2)) {
      this.moveTo(Math.round(_2), Math.round(_1))
    }
    this.checkStyle();
    if (!this.parentElement.isDrawn()) return;
    if (!this.isDrawn()) this.draw();
    this.show();
    if (_5) this.sendToBack();
    else this.bringToFront()
  }, isc.A.checkStyle = function isc_EventCanvas_checkStyle() {
    var _1 = this.calendar.getEventCanvasStyle(this.event, this.calendarView);
    if (_1 != this.styleName) this.setEventStyle(_1)
  }, isc.A.click = function isc_EventCanvas_click() {
    this.calendar.$129v(this)
  }, isc.A.mouseUp = function isc_EventCanvas_mouseUp() {
    return isc.EH.STOP_BUBBLING
  }, isc.A.mouseDown = function isc_EventCanvas_mouseDown() {
    if (this.dragTarget) this.dragTarget.eventCanvas = this;
    this.calendar.eventDialog.hide();
    return isc.EH.STOP_BUBBLING
  }, isc.A.mouseOver = function isc_EventCanvas_mouseOver() {
    if (!this.showRolloverControls || !this.calendar.useEventCanvasRolloverControls) return;
    if (this.$129e && this.$129e.length > 0) {
      var _1 = isc.EH.lastEvent.target;
      if (_1 == this || _1.eventCanvas == this) return
    }
    this.calendar.showEventCanvasRolloverControls(this)
  }, isc.A.mouseOut = function isc_EventCanvas_mouseOut() {
    if (!this.showRolloverControls || !this.calendar.useEventCanvasRolloverControls) return;
    var _1 = isc.EH.lastEvent.target;
    if (_1 && (_1.eventCanvas == this || _1 == isc.Hover.hoverCanvas)) return;
    this.calendar.hideEventCanvasRolloverControls(this)
  }, isc.A.destroy = function isc_EventCanvas_destroy() {
    if (!this.calendar.useEventCanvasRolloverControls && this.$129e) {
      for (var i = this.$129e.length - 1; i >= 0; i--) {
        var _2 = this.$129e[i];
        this.$129e.removeAt(i);
        this.removeChild(_2);
        _2.destroy();
        _2 = null
      }
    }
  });
  isc.B._maxIndex = isc.C + 32;
  isc.defineClass("ZoneCanvas", "EventCanvas");
  isc.A = isc.ZoneCanvas.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.headerPosition = "footer";
  isc.A.showHeader = false;
  isc.A.showBody = false;
  isc.A.canEdit = false;
  isc.A.canDrag = false;
  isc.A.canDragReposition = false;
  isc.A.canDragResize = false;
  isc.A.canRemove = false;
  isc.A.showRolloverControls = false;
  isc.B.push(isc.A.initWidget = function isc_ZoneCanvas_initWidget() {
    this.showCloseButton = false;
    this.canDragReposition = false;
    this.canDragResize = false;
    this.Super("initWidget", arguments)
  }, isc.A.getInnerHTML = function isc_ZoneCanvas_getInnerHTML() {
    var _1 = isc.StringBuffer.create();
    _1.append("<div class='", this.getHeaderStyle(), "' style='position:absolute;bottom:0;width:100%;'>", this.event.name, "</div>");
    var _2 = _1.release();
    return _2
  }, isc.A.setEvent = function isc_ZoneCanvas_setEvent(_1, _2, _3, _4) {
    this.event = _1;
    this.showCloseButton = false;
    this.canDragReposition = false;
    this.canDragResize = false;
    var _5 = this.calendar;
    _2 = _2 || _5.getZoneCanvasStyle(_1, this.calendarView);
    this.setEventStyle(_2, _3, _4)
  }, isc.A.click = function isc_ZoneCanvas_click() {
    if (this.calendar.zoneClick) this.calendar.zoneClick(this.event, this.calendarView.viewName)
  }, isc.A.getHoverHTML = function isc_ZoneCanvas_getHoverHTML() {
    return this.calendar.getZoneHoverHTML(this.event, this, this.calendarView)
  }, isc.A.checkStyle = function isc_ZoneCanvas_checkStyle() {});
  isc.B._maxIndex = isc.C + 6;
  isc.defineClass("IndicatorCanvas", "EventCanvas");
  isc.A = isc.IndicatorCanvas.getPrototype();
  isc.B = isc._allFuncs;
  isc.C = isc.B._maxIndex;
  isc.D = isc._funcClasses;
  isc.D[isc.C] = isc.A.Class;
  isc.A.headerPosition = "none";
  isc.A.showHeader = false;
  isc.A.showBody = false;
  isc.A.canEdit = false;
  isc.A.canDrag = false;
  isc.A.canDragReposition = false;
  isc.A.canDragResize = false;
  isc.A.canRemove = false;
  isc.A.showRolloverControls = false;
  isc.B.push(isc.A.initWidget = function isc_IndicatorCanvas_initWidget() {
    this.showCloseButton = false;
    this.canDragReposition = false;
    this.canDragResize = false;
    this.Super("initWidget", arguments)
  }, isc.A.getInnerHTML = function isc_IndicatorCanvas_getInnerHTML() {
    var _1 = isc.StringBuffer.create();
    _1.append("<div class='", this.getHeaderStyle(), "' style='position:absolute;bottom:0;width:100%;'>", this.event.name, "</div>");
    var _2 = _1.release();
    return _2
  }, isc.A.setEvent = function isc_IndicatorCanvas_setEvent(_1, _2, _3, _4) {
    this.event = _1;
    this.showCloseButton = false;
    this.canDragReposition = false;
    this.canDragResize = false;
    var _5 = this.calendar;
    _2 = _2 || _5.getIndicatorCanvasStyle(_1, this.calendarView);
    this.setEventStyle(_2, _3, _4)
  }, isc.A.click = function isc_IndicatorCanvas_click() {
    if (this.calendar.indicatorClick) this.calendar.indicatorClick(this.event, this.calendarView.viewName)
  }, isc.A.getHoverHTML = function isc_IndicatorCanvas_getHoverHTML() {
    return this.calendar.getIndicatorHoverHTML(this.event, this, this.calendarView)
  }, isc.A.checkStyle = function isc_IndicatorCanvas_checkStyle() {});
  isc.B._maxIndex = isc.C + 6;
  isc.AutoTest.customizeCalendar();
  isc.ClassFactory.defineClass("Timeline", "Calendar");
  isc.A = isc.Timeline.getPrototype();
  isc.A.showTimelineView = true;
  isc.A.showDayView = false;
  isc.A.showWeekView = false;
  isc.A.showMonthView = false;
  isc.A.showControlBar = false;
  isc.A.labelColumnWidth = 75;
  isc.A.sizeEventsToGrid = false;
  isc.A.eventDragGap = 0;
  isc._nonDebugModules = (isc._nonDebugModules != null ? isc._nonDebugModules : []);
  isc._nonDebugModules.push('Calendar');
  isc.checkForDebugAndNonDebugModules();
  isc._moduleEnd = isc._Calendar_end = (isc.timestamp ? isc.timestamp() : new Date().getTime());
  if (isc.Log && isc.Log.logIsInfoEnabled('loadTime')) isc.Log.logInfo('Calendar module init time: ' + (isc._moduleEnd - isc._moduleStart) + 'ms', 'loadTime');
  delete isc.definingFramework;
  if (isc.Page) isc.Page.handleEvent(null, "moduleLoaded", {
    moduleName: 'Calendar',
    loadTime: (isc._moduleEnd - isc._moduleStart)
  });
} else {
  if (window.isc && isc.Log && isc.Log.logWarn) isc.Log.logWarn("Duplicate load of module 'Calendar'.");
}
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