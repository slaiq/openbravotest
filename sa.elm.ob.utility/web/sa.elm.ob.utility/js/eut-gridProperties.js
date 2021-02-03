isc.OBGridButtonsComponent.addProperties({
  initWidget: function () {
    var me = this,
        formButton;

    this.editButton = isc.OBGridToolStripIcon.create({
      buttonType: 'edit',
      originalPrompt: OB.I18N.getLabel('OBUIAPP_GridEditButtonPrompt'),
      prompt: OB.I18N.getLabel('OBUIAPP_GridEditButtonPrompt'),
      action: function () {
        var actionObject = {
          target: me,
          method: me.doEdit,
          parameters: null
        };
        me.grid.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      },

      setErrorMessage: function (msg) {
        if (msg) {
          this.prompt = msg + '<br><br>' + this.originalPrompt;
        } else {
          this.prompt = this.originalPrompt;
        }
      },

      showable: function () {
        var tabId = me.grid.view.tabId;
        if (tabId === '789F9855EAFA40A0A4F1DC610809ED98' || tabId === 'CD3CA08EC1E342B18E438C840325CBFD' || tabId === 'CF6F6528BAC645A9A84EB009BD511A82' || tabId === '609443D6A3964370AF77D10B6859A716' || tabId === '48CC5A2353A74037B27B54217F50053E' || tabId === 'DC44C87B80344B25985C263737A78774' || tabId === '1AEAD3FB37094003AF607E944C8B35C8' || tabId === '1E3F409791B94E2BA877BB035A6C3949' || tabId === 'A1AF3BFA329A4C1B9E8B410889C4936A' || tabId === '25AF62AB8F89499287C44DDC58D3EB02' || tabId === '66BAA8CBEF624E92A59E796BE425B066' || tabId === 'B0EB700BD9CB47918D6FD77B201336E2' || tabId === '11B13C8244D449CEA40065FE8D2D8F27' || tabId === '62CB8DD2443A49E8909551E2D0E1C3D9' || tabId === '0F4DE234CEF8484CA6EE942C1E2ACAC6' || tabId === '0472D5524A474C01984B0F3EA641B16E') {
          if (me.grid.view.getParentRecord() != null && (me.grid.view.getParentRecord().efinProcessbutton || ((tabId === '1AEAD3FB37094003AF607E944C8B35C8' || tabId === '1E3F409791B94E2BA877BB035A6C3949' || tabId === 'A1AF3BFA329A4C1B9E8B410889C4936A' || tabId === '25AF62AB8F89499287C44DDC58D3EB02' || tabId === '66BAA8CBEF624E92A59E796BE425B066' || tabId === 'CF6F6528BAC645A9A84EB009BD511A82' || tabId === '62CB8DD2443A49E8909551E2D0E1C3D9' || tabId === '0472D5524A474C01984B0F3EA641B16E')))) {
            return false;
          } else {
            return !me.grid.view.readOnly && !me.record._readOnly;
          }
        } else {
          return !me.grid.view.readOnly && !me.record._readOnly;
        }
      },

      show: function () {
        if (!this.showable()) {
          return;
        }
        return this.Super('show', arguments);
      }
    });

    formButton = isc.OBGridToolStripIcon.create({
      buttonType: 'form',
      prompt: OB.I18N.getLabel('OBUIAPP_GridFormButtonPrompt'),
      action: function () {
        var actionObject = {
          target: me,
          method: me.doOpen,
          parameters: null
        };
        me.grid.view.standardWindow.doActionAfterAutoSave(actionObject, true);
      }
    });

    this.buttonSeparator1 = isc.OBGridToolStripSeparator.create({});

    if (me.grid.view.readOnly) {
      this.buttonSeparator1.visibility = 'hidden';
    }

    this.addMembers([formButton, this.buttonSeparator1, this.editButton]);
    this.Super('initWidget', arguments);
  },
  doSave: function () {
    // note change back to editOpen is done in the editComplete event of the
    // grid itself
    var tabId = OB.MainView.TabSet.getSelectedTab().pane.activeView.tabId;
    var me = this;
    if (tabId === "6A3644A0025F4A4991CC3DBD7820087A") {
      var editRow = OB.MainView.TabSet.getSelectedTab().pane.activeView.viewGrid.getEditRow();
      var editRecord = OB.MainView.TabSet.getSelectedTab().pane.activeView.viewGrid.getEditValues(editRow);
      var recordId = this.record.id;
      var isMatch = editRecord.match;
      var matchQty = editRecord.maxQty;
      var penaltyAmt = editRecord.penaltyAmt;
      var totalDeduct = editRecord.totalDeduct;
      var matchAmt = editRecord.matchAmt;
      var advDeduction = editRecord.strAdvanceDeductionAmount;

      var callback = function (ok) {
          if (ok) {

            var calendar = $.calendars.instance("ummalqura");
            var hijriDate = calendar.today();
            var strHijDate = (hijriDate.day() < 10 ? "0" + hijriDate.day() : hijriDate.day() + "") + "-" + (hijriDate.month() < 10 ? "0" + hijriDate.month() : hijriDate.month() + "") + "-" + hijriDate.year();

            var a = OB.RemoteCallManager.call('sa.elm.ob.finance.ad_process.RDVProcess.RDVOnSaveHandler', {
              recordId: recordId,
              inpAction: 'addPenalty',
              matchQty: matchQty,
              actionDate: strHijDate,
              advDeductionAmt: advDeduction
            }, {}, function (response, data, request) {
              if (data.result == "true") {
                totalDeduct = totalDeduct + data.penaltyAmount;
                matchAmt = matchAmt - totalDeduct;
                me.grid.setEditValue(editRow, 'penaltyAmt', data.penaltyAmount);
                me.grid.setEditValue(editRow, 'totalDeduct', totalDeduct);
                me.grid.setEditValue(editRow, 'netmatchAmt', matchAmt);

                isc.say(data.message);
                me.grid.getEditForm().hasChanged = true;
                me.grid.endEditing();

              } else {
                isc.say(data.message);
              }
            });

          } else {
            me.grid.endEditing();
          }
          };

      if (recordId != null && isMatch && penaltyAmt == 0 && matchQty > 0) {
        var a = OB.RemoteCallManager.call('sa.elm.ob.finance.ad_process.RDVProcess.RDVOnSaveHandler', {
          recordId: recordId,
          inpAction: 'checkForPenalty',
          matchQty: matchQty
        }, {}, function (response, data, request) {
          if (data.addPenalty == "true") {
            isc.ask(
            OB.I18N.getLabel('EFIN_AddPenalty'), callback);
          } else if (data.hasOwnProperty('message')) {
            isc.say(data.message);
          } else {
            me.grid.endEditing();
          }
        });
      } else if (penaltyAmt != 0) {
        isc.say(OB.I18N.getLabel('Efin_Remove_Penalty'));
      } else {
        this.grid.endEditing();
      }

    } else {
      this.grid.endEditing();
    }

  }
});