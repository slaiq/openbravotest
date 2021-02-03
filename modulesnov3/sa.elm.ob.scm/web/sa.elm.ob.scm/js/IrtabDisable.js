var lastRecordId = "",
    Status = "",
    type = "",
    count, tabId, flag = 0,
    printButtonLastRecordId;
isc.OBToolbar.addClassProperties({
  BUTTON_PROPERTIES: {
    'newDoc': {
      updateState: function () {
        var view = this.view,
            tabId = view['tabId'];
        form = view.viewForm, grid = view.viewGrid;
        // Initial receipt tab
        if (tabId === '2A8F52E5BF1846B2BFBDAAFEF6F89135') {
          var recordId = view.parentRecordId,
              thisRefir = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefir.setDisabled(true);
              } else {
                thisRefir.setDisabled(false);
              }
            });
          }
        }
        // issue return transaction
        else if (tabId === '5B16AE5DFDEF47BB9518CDD325F31DFF') {
          var recordId = view.parentRecordId,
              thisRefissreturn = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.receivingType === "IRT") {
                thisRefissreturn.setDisabled(true);
              } else {
                thisRefissreturn.setDisabled(false);
              }
            });
          }
        }
        // return transaction
        else if (tabId === '0C0819F5D78A401A916BDD8ADB30E4EF') {
          var recordId = view.parentRecordId,
              thisRefreturn = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.receivingType === "INR") {
                thisRefreturn.setDisabled(true);
              } else {
                thisRefreturn.setDisabled(false);
              }
            });
          }
        }
        // custody return transaction
        else if (tabId === 'DD6AB8A564D5482795B0976F6A68FBC5') {
          var recordId = view.parentRecordId,
              thisRefcus = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.receivingType === "INR") {
                thisRefcus.setDisabled(true);
              } else {
                thisRefcus.setDisabled(false);
              }
            });

          }
        }
        // custody isue return transaction
        else if (tabId === 'D4E9D5A2F73E4A15AEA52FD9A5A57902') {
          var recordId = view.parentRecordId,
              thisRefcusiss = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.receivingType === "IRT") {
                thisRefcusiss.setDisabled(true);
              } else {
                thisRefcusiss.setDisabled(false);
              }
            });
          }
        }
        // Custody Transfer window line
        else if (tabId === '09E1F10975074FD9B4E43AF27AA54DE9') {
          this.setDisabled(true);
        }
        // Custody Transfer window Custody Transaction
        else if (tabId === '950C9D8B1D9944B3840495CD2BE80407') {
          this.setDisabled(true);
        }
        // Bid management source ref
        else if(tabId === 'FC8BC787053F4759A9C2129C324834FE'){
        	this.setDisabled(true);
        }
        // Material Issue Request
        else if (tabId === '4AB913F4E6064ED1833ED08A8B7FA2D5') {
          tabId = 'CE947EDC9B174248883292F17F03BB32';
          var recordId = view.parentRecordId,
              thisRefmi = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefmi.setDisabled(true);
              } else {
                thisRefmi.setDisabled(false);
              }
            });
          }
        }
        // //Material Issue Request --custody Transaction
        else if (tabId === 'CB52E60359E7477E82FA36BDBFF0008C') {
          this.setDisabled(true);
        }
        // inventory counting line
        else if (tabId === '9A4225DDEFFD40C8BFA386059CA93DEC') {
          var recordId = view.parentRecordId,
              thisRefic = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefic.setDisabled(true);
              } else {
                thisRefic.setDisabled(false);
              }
            });
          }
        }
        // commitee
        else if (tabId === '23F1315422F341588CA43363CF21915E') {
          var recordId = view.parentRecordId,
              thisRefcomm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefcomm.setDisabled(true);
              } else {
                thisRefcomm.setDisabled(false);
              }
            });
          }
        }
        // media
        else if (tabId === 'D04AD680E4DB4A48BB887B031A7E06A2') {
          var recordId = view.parentRecordId,
              thisRefmed = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefmed.setDisabled(true);
              } else {
                thisRefmed.setDisabled(false);
              }
            });
          }
        }
        // bid
        else if (tabId === '1EF34C4055DC47BDAC85371CE8386B54') {
          var recordId = view.parentRecordId,
              thisRefbid = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefbid.setDisabled(true);
              } else {
                thisRefbid.setDisabled(false);
              }
            });
          }
        }
        // Bid management line
        else if (tabId === 'D54F30C8AD574A2A84999F327EF0E3A4') {
          var recordId = view.parentRecordId,
              thisbidline = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidline.setDisabled(true);
              } else {
                thisbidline.setDisabled(false);
              }
            });
          }
        }
        // bid managemnt source
        else if (tabId === 'FC8BC787053F4759A9C2129C324834FE') {
          var recordId = view.parentRecordId,
              thisbidsrc = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidsrc.setDisabled(true);
              } else {
                thisbidsrc.setDisabled(false);
              }
            });
          }
        }
        // bid dates
        else if (tabId === '754D4F75D3F54A3EBBC69496D27B9C3B') {
          var recordId = view.parentRecordId,
              thisbiddates = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbiddates.setDisabled(true);
              } else {
                thisbiddates.setDisabled(false);
              }
            });
          }
        }
        // bid supplier
        else if (tabId === '0D0A5AFFF5EA480DAB978052AD2198D3') {
          var recordId = view.parentRecordId,
              thisbidsup = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidsup.setDisabled(true);
              } else {
                thisbidsup.setDisabled(false);
              }
            });
          }
        }
        // bid term and cdn
        else if (tabId === '9165D36805BC4B6E8B7CDE4420D09B4B') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // Bank guarantee tabs in proposal managemnt.
        else if (tabId === '614665E1FB764B38A0EAA6153B110824') {
          var recordId = view.parentRecordId,
              thisBGtab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBGtab.setDisabled(true);
              } else {
                thisBGtab.setDisabled(false);
              }
            });
          }
        }
        // Lines tabs in proposal managemnt.
        else if (tabId === '88E026FD2D0446048C80E9D4749AB608') {
          var recordId = view.parentRecordId,
              thisLinetab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisLinetab.setDisabled(true);
              } else {
                thisLinetab.setDisabled(false);
              }
            });
          }
        }
        // committee recommendation tabs in proposal evaluation
        // event.
        else if (tabId === 'B95E00033F514207B2915772C2D6D282') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // committee comments tabs in proposal evaluation event.
        else if (tabId === '6E0596A123994C82BC8F80C0D2554578') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // committee recommendation tabs in Technical evaluation
        // event.
        else if (tabId === 'F8DBF5C0C51E4212A331FBA07BCDAC53') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }

        // committee comments tabs in Technical evaluation
        // event.
        else if (tabId === '4937EA14A9E44775B176F79052F13BFF') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // purchase order new button disable if proposal are
        // added
        else if (tabId === '8F35A05BFBB34C34A80E9DEF769613F7') {
          var recordId = view.parentRecordId,
              thispoproposal = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thispoproposal.setDisabled(true);
              } else {
                thispoproposal.setDisabled(false);
              }
            });
          }
        }
        // Proposal Evaluation Proposal, Lines, Bank
        // Gurantee,Regulation Document
        else if (tabId === 'FB93C95370E049739F7460E8C60B8B9E' || tabId === '53A3B7C2D094483CBC66DEE4D9715A6E' || tabId === '072C2E3474D74982B0E2319CD3CF1961' || tabId === '7D61A1CB7D4A4B14843C728A9CF5E1E9') {
          thisproevalnew = this;
          thisproevalnew.setDisabled(true);
        }
        // Technical Evaluation Proposal,
        else if (tabId === '2F500D79A67F4CF5927467A48680B829') {
          thisproevalnew = this;
          thisproevalnew.setDisabled(true);
        }
        // Bank Guarantee WorkBench- relase and confiscation
        else if (tabId === '008692D1D80444E78AAB4FDFFFA41476') {
          var recordId = view.parentRecordId,
              thisbgreleasenew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              action: "new"
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbgreleasenew.setDisabled(true);
              } else {
                thisbgreleasenew.setDisabled(false);
              }
            });
          }
        }
        // Bg Extension,Amt Revision
        else if (tabId === 'E579C036C1C2401FA439F0F858FA8DE3' || tabId === '4E2C60BDF7894C32BF27E6CAC7684625') {
          var recordId = view.parentRecordId,
              thisbgextamtnew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbgextamtnew.setDisabled(true);
              } else {
                thisbgextamtnew.setDisabled(false);
              }
            });
          }
        }
        // Bank guarantee tabs in Purchase order.
        else if (tabId === '07AF133F4E2E45AAA53D7FEA71656DD4') {
          var recordId = view.parentRecordId,
              thisBGtab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBGtab.setDisabled(true);
              } else {
                thisBGtab.setDisabled(false);
              }
            });
          }
        }
        // Bank guarantee tabs in Open Envelop.
        else if (tabId === 'BC7489A521854DA1B92D40ED7C7A7098') {
          var recordId = view.parentRecordId,
              thisBGtab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBGtab.setDisabled(true);
              } else {
                thisBGtab.setDisabled(false);
              }
            });
          }
        }

        // shipment attribute
        else if (tabId === 'EFD9C9C596D24068ABEB15062EE2EDBC') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }
        // sourceref in po
/* else if (tabId === '832ED077041D47F49BB8AA9EB70F14EC') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }*/
        // source ref in Proposal
        else if (tabId === '8876DC52E0214C1C8A442F88784A9ACD') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }

        // payment term in po
        else if (tabId === '02F79A626AEE4BB4B8B12D345FFB164C') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }
        // payment schedule in po
        else if (tabId === '283293291F49463A905E37366C799426') {
          var recordId = view.parentRecordId,
          thispopaymentschedule = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
            	  thispopaymentschedule.setDisabled(true);
              } else {
            	  thispopaymentschedule.setDisabled(false);
              }
            });
          }
        }
        // PO amendement
        else if (tabId === '1CEC4F8FFBCC41AD86E0A830880CBFF3') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }
        // Extension and relese tab in Insurance Certificate.
        else if (tabId === '742DBD83811C4B1FAD5570D5160B30FF' || tabId === 'A76CE84017684DDD94583680C5AF7912') {
          var recordId = view.parentRecordId,
              thisicreleasenew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisicreleasenew.setDisabled(true);
              } else {
                thisicreleasenew.setDisabled(false);
              }
            });
          }
        } else if (tabId === '6732339A97874A85BF73542C2B5AFF88') {
          var recordId = view.parentRecordId,
              thisbgnew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              console.log(data.IsDraft);
              if (data.IsDraft == 1) {
                thisbgnew.setDisabled(true);
              } else {
                thisbgnew.setDisabled(false);
              }
            });
          }
        }
        // Employee Evalution - Employees Tab
        else if (tabId === 'F206150C87E14866A68F08389DE85549') {
          var recordId = view.parentRecordId,
              empevalemployees = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.hcm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              console.log(data.IsDraft);
              if (data.IsDraft == 1) {
                empevalemployees.setDisabled(true);
              } else {
                empevalemployees.setDisabled(false);
              }
            });
          }
        } // security rules associated tabs
        else if (tabId === '789F9855EAFA40A0A4F1DC610809ED98' || tabId === 'CD3CA08EC1E342B18E438C840325CBFD' || tabId === 'CF6F6528BAC645A9A84EB009BD511A82' || tabId === '609443D6A3964370AF77D10B6859A716' || tabId === '48CC5A2353A74037B27B54217F50053E' || tabId === 'DC44C87B80344B25985C263737A78774' || tabId === '1AEAD3FB37094003AF607E944C8B35C8' || tabId === '1E3F409791B94E2BA877BB035A6C3949' || tabId === 'A1AF3BFA329A4C1B9E8B410889C4936A' || tabId === '25AF62AB8F89499287C44DDC58D3EB02' || tabId === '66BAA8CBEF624E92A59E796BE425B066' || tabId === 'B0EB700BD9CB47918D6FD77B201336E2' || tabId === '11B13C8244D449CEA40065FE8D2D8F27' || tabId === '62CB8DD2443A49E8909551E2D0E1C3D9' || tabId === '0F4DE234CEF8484CA6EE942C1E2ACAC6' || tabId === '0472D5524A474C01984B0F3EA641B16E') {
          var recordId = view.parentRecordId,
              thisDepTab = this;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                if (view.viewGrid.contextMenu) {
                  view.viewGrid.contextMenu.destroy();
                  view.viewGrid.contextMenu = null;
                  view.viewGrid.showCellContextMenus = false;
                }
                thisDepTab.setDisabled(true);
              } else {
                if (view.viewGrid.contextMenu == null) {
                  var grid = view.viewGrid;
                  var menuItems = [{
                    title: OB.I18N.getLabel('OBUIAPP_CreateRecordInGrid'),
                    click: function () {
                      grid.deselectAllRecords();
                      grid.startEditingNew();
                    }
                  }, {
                    title: OB.I18N.getLabel('OBUIAPP_CreateRecordInForm'),
                    click: function () {
                      grid.deselectAllRecords();
                      grid.view.newDocument();
                    }
                  }];


                  view.viewGrid.contextMenu = view.viewGrid.getMenuConstructor().create({
                    items: menuItems
                  });
                  view.viewGrid.contextMenu.show = function () {
                    var me = this;
                    var grid = view.viewGrid;
                    // If not in the header tab, and no parent is selected, do not show the context menu
                    // See issue https://issues.openbravo.com/view.php?id=21787
                    if (!grid.view.hasValidState()) {
                      return;
                    }
                    if (grid.isGrouped) {
                      return;
                    }
                    if (!grid.view.isActiveView()) {
                      // The view where the context menu is being opened must be active
                      // See issue https://issues.openbravo.com/view.php?id=20872
                      grid.view.setAsActiveView(true);
                      setTimeout(function () {
                        me.Super('show', arguments);
                      }, 10);
                    } else {
                      me.Super('show', arguments);
                    }
                  }
                }
                view.viewGrid.showCellContextMenus = true;
                thisDepTab.setDisabled(false);
              }
            });
          }
        } 
       // BG Release
        else if (tabId === 'C1779EE84BE44C30B4385F367742CE7F') {
          var recordId = view.parentRecordId,
              thisDepTab = this;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              action: "new"
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                if (view.viewGrid.contextMenu) {
                  view.viewGrid.contextMenu.destroy();
                  view.viewGrid.contextMenu = null;
                  view.viewGrid.showCellContextMenus = false;
                }
                thisDepTab.setDisabled(true);
              } else {
                if (view.viewGrid.contextMenu == null) {
                  var grid = view.viewGrid;
                  var menuItems = [{
                    title: OB.I18N.getLabel('OBUIAPP_CreateRecordInGrid'),
                    click: function () {
                      grid.deselectAllRecords();
                      grid.startEditingNew();
                    }
                  }, {
                    title: OB.I18N.getLabel('OBUIAPP_CreateRecordInForm'),
                    click: function () {
                      grid.deselectAllRecords();
                      grid.view.newDocument();
                    }
                  }];


                  view.viewGrid.contextMenu = view.viewGrid.getMenuConstructor().create({
                    items: menuItems
                  });
                  view.viewGrid.contextMenu.show = function () {
                    var me = this;
                    var grid = view.viewGrid;
                    // If not in the header tab, and no parent is selected, do not show the context menu
                    // See issue https://issues.openbravo.com/view.php?id=21787
                    if (!grid.view.hasValidState()) {
                      return;
                    }
                    if (grid.isGrouped) {
                      return;
                    }
                    if (!grid.view.isActiveView()) {
                      // The view where the context menu is being opened must be active
                      // See issue https://issues.openbravo.com/view.php?id=20872
                      grid.view.setAsActiveView(true);
                      setTimeout(function () {
                        me.Super('show', arguments);
                      }, 10);
                    } else {
                      me.Super('show', arguments);
                    }
                  }
                }
                view.viewGrid.showCellContextMenus = true;
                thisDepTab.setDisabled(false);
              }
            });
          }
        }
     // REVISION
       /* else if (tabId === 'E68453B4E62548C6B5E79FEDE3C36586') {
          var recordId = view.parentRecordId,
              thisDepTab = this;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              action: "new"
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                if (view.viewGrid.contextMenu) {
                  view.viewGrid.contextMenu.destroy();
                  view.viewGrid.contextMenu = null;
                  view.viewGrid.showCellContextMenus = false;
                }
                thisDepTab.setDisabled(true);
              } else {
                if (view.viewGrid.contextMenu == null) {
                  var grid = view.viewGrid;
                  var menuItems = [{
                    title: OB.I18N.getLabel('OBUIAPP_CreateRecordInGrid'),
                    click: function () {
                      grid.deselectAllRecords();
                      grid.startEditingNew();
                    }
                  }, {
                    title: OB.I18N.getLabel('OBUIAPP_CreateRecordInForm'),
                    click: function () {
                      grid.deselectAllRecords();
                      grid.view.newDocument();
                    }
                  }];


                  view.viewGrid.contextMenu = view.viewGrid.getMenuConstructor().create({
                    items: menuItems
                  });
                  view.viewGrid.contextMenu.show = function () {
                    var me = this;
                    var grid = view.viewGrid;
                    // If not in the header tab, and no parent is selected, do not show the context menu
                    // See issue https://issues.openbravo.com/view.php?id=21787
                    if (!grid.view.hasValidState()) {
                      return;
                    }
                    if (grid.isGrouped) {
                      return;
                    }
                    if (!grid.view.isActiveView()) {
                      // The view where the context menu is being opened must be active
                      // See issue https://issues.openbravo.com/view.php?id=20872
                      grid.view.setAsActiveView(true);
                      setTimeout(function () {
                        me.Super('show', arguments);
                      }, 10);
                    } else {
                      me.Super('show', arguments);
                    }
                  }
                }
                view.viewGrid.showCellContextMenus = true;
                thisDepTab.setDisabled(false);
              }
            });
          }
        }*/
        else {
          if (view.isShowingForm) {
            this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          } else {
            this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          }
        }
      }
    },
    'newRow': {
      updateState: function () {
        var view = this.view,
            tabId = view['tabId'];
        form = view.viewForm, grid = view.viewGrid;

        // Initial receipt tab
        if (tabId === '2A8F52E5BF1846B2BFBDAAFEF6F89135') {
          var recordId = view.parentRecordId,
              thisRefirrow = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefirrow.setDisabled(true);
              } else {
                thisRefirrow.setDisabled(false);
              }
            });
          }
        }
        // issue return transaction
        else if (tabId === '5B16AE5DFDEF47BB9518CDD325F31DFF') {
          var recordId = view.parentRecordId,
              thisRefissreturnrow = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.receivingType === "IRT") {
                thisRefissreturnrow.setDisabled(true);
              } else {
                thisRefissreturnrow.setDisabled(false);
              }
            });
          }
        }
        // Bid management source ref
        else if(tabId === 'FC8BC787053F4759A9C2129C324834FE'){
        	this.setDisabled(true);
        }
        // return transaction
        else if (tabId === '0C0819F5D78A401A916BDD8ADB30E4EF') {
          var recordId = view.parentRecordId,
              thisRefrow = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.receivingType === "INR") {
                thisRefrow.setDisabled(true);
              } else {
                thisRefrow.setDisabled(false);
              }
            });
          }
        }
        // custody return transaction
        else if (tabId === 'DD6AB8A564D5482795B0976F6A68FBC5') {
          var recordId = view.parentRecordId,
              thisRefcusrow = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.receivingType === "INR") {
                thisRefcusrow.setDisabled(true);
              } else {
                thisRefcusrow.setDisabled(false);
              }
            });

          }
        }
        // custody isue return transaction
        else if (tabId === 'D4E9D5A2F73E4A15AEA52FD9A5A57902') {
          var recordId = view.parentRecordId,
              thisRefcusissrow = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.receivingType === "IRT") {
                thisRefcusissrow.setDisabled(true);
              } else {
                thisRefcusissrow.setDisabled(false);
              }
            });
          }
        }
        // Custody Transfer window line
        else if (tabId === '09E1F10975074FD9B4E43AF27AA54DE9') {
          this.setDisabled(true);
        }
        // Custody Transfer window Custody Transaction
        else if (tabId === '950C9D8B1D9944B3840495CD2BE80407') {
          this.setDisabled(true);
        }
        // Material Issue Request
        else if (tabId === '4AB913F4E6064ED1833ED08A8B7FA2D5') {
          tabId = 'CE947EDC9B174248883292F17F03BB32';
          var recordId = view.parentRecordId,
              thisRefmirow = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefmirow.setDisabled(true);
              } else {
                thisRefmirow.setDisabled(false);
              }
            });
          }
        }
        // //Material Issue Request --custody Transaction
        else if (tabId === 'CB52E60359E7477E82FA36BDBFF0008C') {
          this.setDisabled(true);
        }
        // inventory counting line
        else if (tabId === '9A4225DDEFFD40C8BFA386059CA93DEC') {
          var recordId = view.parentRecordId,
              thisReficrow = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisReficrow.setDisabled(true);
              } else {
                thisReficrow.setDisabled(false);
              }
            });
          }
        }
        // committee
        else if (tabId === '23F1315422F341588CA43363CF21915E') {
          var recordId = view.parentRecordId,
              thisRefcomm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefcomm.setDisabled(true);
              } else {
                thisRefcomm.setDisabled(false);
              }
            });
          }
        }
        // Bid Managment line
        else if (tabId === 'D54F30C8AD574A2A84999F327EF0E3A4') {
          var recordId = view.parentRecordId,
              thisbidmgmtline = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidmgmtline.setDisabled(true);
              } else {
                thisbidmgmtline.setDisabled(false);
              }
            });
          }
        }
        // bid managemnt source
        else if (tabId === 'FC8BC787053F4759A9C2129C324834FE') {
          var recordId = view.parentRecordId,
              thisbidmgmtsrc = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidmgmtsrc.setDisabled(true);
              } else {
                thisbidmgmtsrc.setDisabled(false);
              }
            });
          }
        }
        // media
        else if (tabId === 'D04AD680E4DB4A48BB887B031A7E06A2') {
          var recordId = view.parentRecordId,
              thisRefmednew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefmednew.setDisabled(true);
              } else {
                thisRefmednew.setDisabled(false);
              }
            });
          }
        }
        // bid
        else if (tabId === '1EF34C4055DC47BDAC85371CE8386B54') {
          var recordId = view.parentRecordId,
              thisRefbidnew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefbidnew.setDisabled(true);
              } else {
                thisRefbidnew.setDisabled(false);
              }
            });
          }
        }
        // bid dates
        else if (tabId === '754D4F75D3F54A3EBBC69496D27B9C3B') {
          var recordId = view.parentRecordId,
              thisbiddates = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbiddates.setDisabled(true);
              } else {
                thisbiddates.setDisabled(false);
              }
            });
          }
        }
        // bid supplier
        else if (tabId === '0D0A5AFFF5EA480DAB978052AD2198D3') {
          var recordId = view.parentRecordId,
              thisbidsupp = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidsupp.setDisabled(true);
              } else {
                thisbidsupp.setDisabled(false);
              }
            });
          }
        }

        // bid term and cdn
        else if (tabId === '9165D36805BC4B6E8B7CDE4420D09B4B') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // Bank guarantee tabs in proposal managemnt.
        else if (tabId === '614665E1FB764B38A0EAA6153B110824') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // Lines tabs in proposal managemnt.
        else if (tabId === '88E026FD2D0446048C80E9D4749AB608') {
          var recordId = view.parentRecordId,
              thisLinerowtab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisLinerowtab.setDisabled(true);
              } else {
                thisLinerowtab.setDisabled(false);
              }
            });
          }
        }
        // committee recommendation tabs in proposal evaluation
        // event.
        else if (tabId === 'B95E00033F514207B2915772C2D6D282') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // committee comments tabs in proposal evaluation event.
        else if (tabId === '6E0596A123994C82BC8F80C0D2554578') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // committee recommendation tabs in Technical evaluation
        // event.
        else if (tabId === 'F8DBF5C0C51E4212A331FBA07BCDAC53') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }
        // committee comments tabs in Technical evaluation
        // event.
        else if (tabId === '4937EA14A9E44775B176F79052F13BFF') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                thisbidterm.setDisabled(false);
              }
            });
          }
        }

        //purchase order new button disable if proposal are added 
        else if (tabId === '8F35A05BFBB34C34A80E9DEF769613F7') {
          var recordId = view.parentRecordId,
              thispoproposalrow = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thispoproposalrow.setDisabled(true);
              } else {
                thispoproposalrow.setDisabled(false);
              }
            });
          }
        } else if (tabId === 'FB93C95370E049739F7460E8C60B8B9E' || tabId === '53A3B7C2D094483CBC66DEE4D9715A6E' || tabId === '072C2E3474D74982B0E2319CD3CF1961' || tabId === '7D61A1CB7D4A4B14843C728A9CF5E1E9') {
          thisproevalnewrow = this;
          thisproevalnewrow.setDisabled(true);
        }

        //Technical Evaluation Proposal,
        else if (tabId === '2F500D79A67F4CF5927467A48680B829') {
          thisproevalnew = this;
          thisproevalnew.setDisabled(true);
        }

        //Bank Guarantee WorkBench- relase and confiscation
        else if (tabId === '008692D1D80444E78AAB4FDFFFA41476') {
          var recordId = view.parentRecordId,
              thisbgreleasenew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              action: "new"
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbgreleasenew.setDisabled(true);
              } else {
                thisbgreleasenew.setDisabled(false);
              }
            });
          }
        }
        //bg extension , Amt Revision
        else if (tabId === 'E579C036C1C2401FA439F0F858FA8DE3' || tabId === '4E2C60BDF7894C32BF27E6CAC7684625') {
          var recordId = view.parentRecordId,
              thisbgextamtnew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbgextamtnew.setDisabled(true);
              } else {
                thisbgextamtnew.setDisabled(false);
              }
            });
          }
        }
        //Bank guarantee tabs in Purchase order. 
        else if (tabId === '07AF133F4E2E45AAA53D7FEA71656DD4') {
          var recordId = view.parentRecordId,
              thisBGtab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBGtab.setDisabled(true);
              } else {
                thisBGtab.setDisabled(false);
              }
            });
          }
        }
        //Bank guarantee tabs in Open Envelop. 
        else if (tabId === 'BC7489A521854DA1B92D40ED7C7A7098') {
          var recordId = view.parentRecordId,
              thisBGtab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBGtab.setDisabled(true);
              } else {
                thisBGtab.setDisabled(false);
              }
            });
          }
        }

        //shipment attribute
        else if (tabId === 'EFD9C9C596D24068ABEB15062EE2EDBC') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }
        //sourceref in po
/* else if (tabId === '832ED077041D47F49BB8AA9EB70F14EC') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }*/
        //source reference in Proposal
        else if (tabId === '8876DC52E0214C1C8A442F88784A9ACD') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }
        //payment term  in po
        else if (tabId === '02F79A626AEE4BB4B8B12D345FFB164C') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }
        //PO amendement 
        else if (tabId === '1CEC4F8FFBCC41AD86E0A830880CBFF3') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }
        //Extension and relese tab in Insurance Certificate.
        else if (tabId === '742DBD83811C4B1FAD5570D5160B30FF' || tabId === 'A76CE84017684DDD94583680C5AF7912') {
          var recordId = view.parentRecordId,
              thisicreleasenewdoc = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisicreleasenewdoc.setDisabled(true);
              } else {
                thisicreleasenewdoc.setDisabled(false);
              }
            });
          }
        } else if (tabId === '6732339A97874A85BF73542C2B5AFF88') {
          var recordId = view.parentRecordId,
              thisbgnewgird = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbgnewgird.setDisabled(true);
              } else {
                thisbgnewgird.setDisabled(false);
              }
            });
          }
        } // Employee Evalution - Employees Tab
        else if (tabId === 'F206150C87E14866A68F08389DE85549') {
          var recordId = view.parentRecordId,
              empevalemployees = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.hcm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              console.log(data.IsDraft);
              if (data.IsDraft == 1) {
                empevalemployees.setDisabled(true);
              } else {
                empevalemployees.setDisabled(false);
              }
            });
          }
        } // security rules associated tabs
        else if (tabId === '789F9855EAFA40A0A4F1DC610809ED98' || tabId === 'CD3CA08EC1E342B18E438C840325CBFD' || tabId === 'CF6F6528BAC645A9A84EB009BD511A82' || tabId === '609443D6A3964370AF77D10B6859A716' || tabId === '48CC5A2353A74037B27B54217F50053E' || tabId === 'DC44C87B80344B25985C263737A78774' || tabId === '1AEAD3FB37094003AF607E944C8B35C8' || tabId === '1E3F409791B94E2BA877BB035A6C3949' || tabId === 'A1AF3BFA329A4C1B9E8B410889C4936A' || tabId === '25AF62AB8F89499287C44DDC58D3EB02' || tabId === '66BAA8CBEF624E92A59E796BE425B066' || tabId === 'B0EB700BD9CB47918D6FD77B201336E2' || tabId === '11B13C8244D449CEA40065FE8D2D8F27' || tabId === '62CB8DD2443A49E8909551E2D0E1C3D9' || tabId === '0F4DE234CEF8484CA6EE942C1E2ACAC6' || tabId === '0472D5524A474C01984B0F3EA641B16E') {
          var recordId = view.parentRecordId,
              thisDepTab = this;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                if (view.viewGrid.contextMenu) {
                  view.viewGrid.contextMenu.destroy();
                  view.viewGrid.contextMenu = null;
                  view.viewGrid.showCellContextMenus = false;
                }
                thisDepTab.setDisabled(true);
              } else {
                view.viewGrid.showCellContextMenus = true;
                thisDepTab.setDisabled(false);
              }
            });
          }
        } 
     // BG Release
        else if (tabId === 'C1779EE84BE44C30B4385F367742CE7F') {
          var recordId = view.parentRecordId,
              thisDepTab = this;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              action: "new"
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                if (view.viewGrid.contextMenu) {
                  view.viewGrid.contextMenu.destroy();
                  view.viewGrid.contextMenu = null;
                  view.viewGrid.showCellContextMenus = false;
                }
                thisDepTab.setDisabled(true);
              } else {
                view.viewGrid.showCellContextMenus = true;
                thisDepTab.setDisabled(false);
              }
            });
          }
        }
       /* //revision Line
        else if (tabId === 'E68453B4E62548C6B5E79FEDE3C36586') {
            var recordId = view.parentRecordId,
                thisRevisionLine = this;
            if (recordId != null && recordId != 1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId,
                action: "new"
              }, {}, function (response, data, request) {
                if (data.IsDraft === 1) {
                  if (view.viewGrid.contextMenu) {
                    view.viewGrid.contextMenu.destroy();
                    view.viewGrid.contextMenu = null;
                    view.viewGrid.showCellContextMenus = false;
                  }
                  thisRevisionLine.setDisabled(true);
                } else {
                  view.viewGrid.showCellContextMenus = true;
                  thisRevisionLine.setDisabled(false);
                }
              });
            }
          }*/
        else {
          if (view.isShowingForm) {
            this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          } else {
            this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          }
        }
      }
    },
    'eliminate': {
      updateState: function () {
        var view = this.view,
            tabId = view['tabId'];
        form = view.viewForm, grid = view.viewGrid;
        //Initial receipt tab
        if (tabId === '2A8F52E5BF1846B2BFBDAAFEF6F89135') {
          var recordId = view.parentRecordId,
              thisReficdel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'delete'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisReficdel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisReficdel);
              }
            });
          }
        }
        //purchase requisition line
        else if (tabId === '800251') {
          var recordId = view.parentRecordId,
              thisRefprdel = this;
          if (recordId != null) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.PurReqDeleteButtonDisableHandler', {
              recordId: recordId
            }, {}, function (response, data, request) {
              if (data.reqCount == 0) {
                thisRefprdel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRefprdel);
              }
            });
          }
        }
        // payment schedule in po
        else if (tabId === '283293291F49463A905E37366C799426') {
          var recordId = view.parentRecordId,
          thispopaymentschedule = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'delete'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
            	  thispopaymentschedule.setDisabled(true);
              } else {
            	  isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thispopaymentschedule);
              }
            });
          }
        }
        
        
        // Bid management source ref
        else if(tabId === 'FC8BC787053F4759A9C2129C324834FE'){
        	this.setDisabled(true);
        }
        //inventory counting line
        else if (tabId === '9A4225DDEFFD40C8BFA386059CA93DEC') {
          var recordId = view.parentRecordId,
              thisReficdel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                thisReficdel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisReficdel);
              }
            });
          }
        }
        //Return Transaction(72A6B3CA5BE848ACA976304375A5B7A6) and Issue Return Transaction(922927563BFC48098D17E4DC85DD504C)
        else if (tabId === '72A6B3CA5BE848ACA976304375A5B7A6' || tabId === '922927563BFC48098D17E4DC85DD504C') {
          if (grid.getSelectedRecord() != null) {
            var recordId = grid.getSelectedRecord().id,
                thisRTTranDel = this;
            if (recordId != null && recordId != -1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId
              }, {}, function (response, data, request) {
                if (data.IsDraft === 1) {
                  thisRTTranDel.setDisabled(true);
                } else {
                	isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRTTranDel);
                }
              });
            }
          }

        }
        //Return Transaction line (0C0819F5D78A401A916BDD8ADB30E4EF), Issue Return Transaction line(5B16AE5DFDEF47BB9518CDD325F31DFF)
        else if (tabId === '0C0819F5D78A401A916BDD8ADB30E4EF' || tabId === '5B16AE5DFDEF47BB9518CDD325F31DFF') {
          var recordId = view.parentRecordId,
              thisRTlineDel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                thisRTlineDel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRTlineDel);
              }
            });
          }
        }
        //Custody transaction under Return Transaction (DD6AB8A564D5482795B0976F6A68FBC5) and Custody transaction under Issue Return Transaction (D4E9D5A2F73E4A15AEA52FD9A5A57902)
        else if (tabId === 'DD6AB8A564D5482795B0976F6A68FBC5' || tabId === 'D4E9D5A2F73E4A15AEA52FD9A5A57902') {
          var recordId = view.parentRecordId,
              thisRTcusline = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                thisRTcusline.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRTcusline);
              }
            });
          }
        }
        //committee
        else if (tabId === '23F1315422F341588CA43363CF21915E') {
          var recordId = view.parentRecordId,
              thisRefcomm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefcomm.setDisabled(true);
              } else {
            	  isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRefcomm);
              }
            });
          }
        }
        //bid managemnt line
        else if (tabId === 'D54F30C8AD574A2A84999F327EF0E3A4') {
          var recordId = view.parentRecordId,
              thisBidmgmtline = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBidmgmtline.setDisabled(true);
              } else {
            	  isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisBidmgmtline);
              }
            });
          }
        }
        //bid managemnt source
        else if (tabId === 'FC8BC787053F4759A9C2129C324834FE') {
          var recordId = view.parentRecordId,
              thisBidmgmtsrc = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBidmgmtsrc.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisBidmgmtsrc);
              }
            });
          }
        }

        //bid managemnt 
        else if (tabId === '31960EC365D746A180594FFB7B403ABB') {
          if (grid.getSelectedRecord() != null) {
            var recordId = grid.getSelectedRecord().id,
                thisBidmgmtdel = this;
            if (recordId != null && recordId != -1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId
              }, {}, function (response, data, request) {
                if (data.IsDraft === 1) {
                  thisBidmgmtdel.setDisabled(true);
                } else {
                  isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisBidmgmtdel);
                }
              });
            }
          }
        }
        //bid dates 
        else if (tabId === '754D4F75D3F54A3EBBC69496D27B9C3B') {
          var recordId = view.parentRecordId,
              thisRefmeddel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'delete'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefmeddel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRefmeddel);
              }
            });
          }
        }
        // media
        else if (tabId === 'D04AD680E4DB4A48BB887B031A7E06A2') {
          var recordId = view.parentRecordId,
              thisRefbiddel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefbiddel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRefbiddel);
              }
            });
          }
        }
        // bid
        else if (tabId === '1EF34C4055DC47BDAC85371CE8386B54') {
          var recordId = view.parentRecordId,
              thisRefbiddel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisRefbiddel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRefbiddel);
              }
            });
          }
        }
        //bid dates 
        else if (tabId === '754D4F75D3F54A3EBBC69496D27B9C3B') {
          var recordId = view.parentRecordId,
              thisbiddates = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'delete'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbiddates.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbiddates);
              }
            });
          }
        }
        //bid supplier 
        else if (tabId === '0D0A5AFFF5EA480DAB978052AD2198D3') {
          var recordId = view.parentRecordId,
              thisbidsupplier = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidsupplier.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbidsupplier);
              }
            });
          }
        }

        //bid term and cdn 
        else if (tabId === '9165D36805BC4B6E8B7CDE4420D09B4B') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbidterm);
              }
            });
          }
        } else if (tabId === '6F86F1F0E85C4A8F8DF36B5654BA3E3C') {
          if (grid.getSelectedRecord() != null) {
            var recordId = grid.getSelectedRecord().id,
                thisbidterm = this;
            if (recordId != null && recordId != -1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId
              }, {}, function (response, data, request) {
                if (data.IsDraft == 1) {
                  thisbidterm.setDisabled(true);
                } else {
                  isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbidterm);
                }
              });
            }
          }
        }
        //Bank guarantee tabs in proposal managemnt. 
        else if (tabId === '614665E1FB764B38A0EAA6153B110824') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbidterm);
              }
            });
          }
        }
        //committee recommendation tabs in proposal evaluation event. 
        else if (tabId === 'B95E00033F514207B2915772C2D6D282') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbidterm);
              }
            });
          }
        }
        //committee comments tabs in proposal evaluation event. 
        else if (tabId === '6E0596A123994C82BC8F80C0D2554578') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbidterm);
              }
            });
          }
        }

        //committee recommendation tabs in TECHNICAL evaluation event. 
        else if (tabId === 'F8DBF5C0C51E4212A331FBA07BCDAC53') {
          var recordId = view.parentRecordId,
              thisbidterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbidterm.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbidterm);
              }
            });
          }
        }
        //committee comments tabs in TECHNICAL evaluation event. 
        else if (tabId === '4937EA14A9E44775B176F79052F13BFF') {
          var recordId = view.parentRecordId,
              thistechterm = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              console.log(data.IsDraft);
              if (data.IsDraft == 1) {
                thistechterm.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thistechterm);
              }
            });
          }
        }
        //proposal management tab. if bid is associated then we should not allow to delete. 
        else if (tabId === '88E026FD2D0446048C80E9D4749AB608') {
          var recordId = view.parentRecordId,
              thispropmgmtdel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'delete'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thispropmgmtdel.setDisabled(true);
              } else {
            	  isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thispropmgmtdel);
              }
            });
          }
        }
        //Proposal Evaluation Proposal, Lines, Bank Gurantee,Regulation Document
        else if (tabId === 'FB93C95370E049739F7460E8C60B8B9E' || tabId === '072C2E3474D74982B0E2319CD3CF1961' || tabId === '7D61A1CB7D4A4B14843C728A9CF5E1E9') {
          thisproevaldel = this;
          thisproevaldel.setDisabled(true);
        }
        //Technical Evaluation Proposal,
        else if (tabId === '2F500D79A67F4CF5927467A48680B829') {
          thisproevaldel = this;
          thisproevaldel.setDisabled(true);
        } else if (tabId === '53A3B7C2D094483CBC66DEE4D9715A6E') {
          var recordId = view.parentRecordId,
              thispropattdel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thispropattdel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thispropattdel);
              }
            });
          }
        }
        //Bank Guarantee WorkBench- relase and confiscation
        else if (tabId === '008692D1D80444E78AAB4FDFFFA41476') {
          var recordId = view.parentRecordId,
              thisbgreleasenew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbgreleasenew.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbgreleasenew);
              }
            });
          }
        }
        //bg Extension ,Amt Revision
        else if (tabId === 'E579C036C1C2401FA439F0F858FA8DE3' || tabId === '4E2C60BDF7894C32BF27E6CAC7684625') {
          var recordId = view.parentRecordId,
              thisbgextamtnew = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbgextamtnew.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbgextamtnew);
              }
            });
          }
        }

        //Bank guarantee tabs in Purchase order. 
        else if (tabId === '07AF133F4E2E45AAA53D7FEA71656DD4') {
          var recordId = view.parentRecordId,
              thisBGtab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBGtab.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisBGtab);
              }
            });
          }
        }
        //Bank guarantee tabs in Open Envelop. 
        else if (tabId === 'BC7489A521854DA1B92D40ED7C7A7098') {
          var recordId = view.parentRecordId,
              thisBGtab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisBGtab.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisBGtab);
              }
            });
          }
        }
        //poheader
        else if (tabId === '62248BBBCF644C18A75B92AD8E50238C') {
          if (grid.getSelectedRecord() != null) {
            var recordId = grid.getSelectedRecord().id,
                thispurchaseorder = this;
            if (recordId != null && recordId != -1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId,
                button: 'delete'
              }, {}, function (response, data, request) {
                if (data.IsDraft === 1) {
                  thispurchaseorder.setDisabled(true);
                } else {
                  isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thispurchaseorder);
                }
              });
            }
          }
        }

        //poline
        else if (tabId === '8F35A05BFBB34C34A80E9DEF769613F7') {
          var recordId = view.parentRecordId,
              thispoline = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'delete'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thispoline.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thispoline);
              }
            });
          }
        }
        
        //shipment attribute
        else if (tabId === 'EFD9C9C596D24068ABEB15062EE2EDBC') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisposhipment);
              }
            });
          }
        }
        //sourceref in po
/*else if (tabId === '832ED077041D47F49BB8AA9EB70F14EC') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                thisposhipment.setDisabled(false);
              }
            });
          }
        }*/
        //sourceref in po
        else if (tabId === '8876DC52E0214C1C8A442F88784A9ACD') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisposhipment);
              }
            });
          }
        }
        //payment term  in po
        else if (tabId === '02F79A626AEE4BB4B8B12D345FFB164C') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisposhipment);
              }
            });
          }
        }
        //PO amendement 
        else if (tabId === '1CEC4F8FFBCC41AD86E0A830880CBFF3') {
          var recordId = view.parentRecordId,
              thisposhipment = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'delete'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisposhipment.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisposhipment);
              }
            });
          }
        }
        //Extension and relese tab in Insurance Certificate.
        else if (tabId === '742DBD83811C4B1FAD5570D5160B30FF' || tabId === 'A76CE84017684DDD94583680C5AF7912') {
          var recordId = view.parentRecordId,
              thisicreleasedel = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisicreleasedel.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisicreleasedel);
              }
            });
          }
        } else if (tabId === '6732339A97874A85BF73542C2B5AFF88') {
          var recordId = view.parentRecordId,
              thisbgelim = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisbgelim.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisbgelim);
              }
            });
          }
        }

        //Distribution tab under Proposalmgmt
        else if (tabId === '47FDFAA3516F41C5AC4F862B0E763503') {
          tabId = 'D6115C9AF1DD4C4C9811D2A69E42878B';
          var recordId = view.parentRecordId,
              thisdistributiontab = this;
          if (recordId != null && recordId != -1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'delete'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisdistributiontab.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisdistributiontab);
              }
            });
          }
        }
        //Distribution tab under PO
        else if (tabId === '2E7B7612C1EB407DA09A28CC6E70ED5A') {
          tabId = '62248BBBCF644C18A75B92AD8E50238C';
          var recordId = view.parentRecordId,
              thisdistab = this;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId,
              button: 'distribution'
            }, {}, function (response, data, request) {
              if (data.IsDraft == 1) {
                thisdistab.setDisabled(true);
              } else {
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisdistab);
              }
            });
          }
        } else if (tabId === '57AA8C32AA2E4A36819DAA3AFEF2DC1C') {
          if (grid.getSelectedRecord()) {
            var recordId = grid.getSelectedRecord().id,
                thisDepTab = this;
            if (recordId != null && recordId != 1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId
              }, {}, function (response, data, request) {
                if (data.IsDraft === 1) {
                  thisDepTab.setDisabled(true);
                } else {
                  isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisDepTab);
                }
              });
            }
          }
        }
        //encumbrance header
        else if (tabId === '9CBD55F879EA4DCAA4E944C0B7DC03D4') {
        	   if (grid.getSelectedRecord()) {
            var recordId = grid.getSelectedRecord().id,
                thisencheader = this;
            if (recordId != null && recordId != -1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId,
                button: 'delete'
              }, {}, function (response, data, request) {
                if (data.IsDraft == 1) {
                	thisencheader.setDisabled(true);
                } else {
                	isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisencheader);
                }
              });
            }
        	   }
          }
        //Encumbrance Line
        else if (tabId === 'A2E25351FBFF41CB949EDF35DE875B73') {
            var recordId = view.parentRecordId,
                thisencline = this;
            if (recordId != null && recordId != -1) {
              var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId,
                button: 'delete'
              }, {}, function (response, data, request) {
                if (data.IsDraft == 1) {
                	thisencline.setDisabled(true);
                } else {
                	isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisencline);
                }
              });
            }
          }
        else if (tabId === '033E788CA45649ED8917A88E1BD48515' ){
        	 if (grid.getSelectedRecord()) {
                 var recordId = grid.getSelectedRecord().id,
                 thisRdvAdvLine=this;
                 if (recordId != null && recordId != 1) {
                	 var lineno= grid.getSelectedRecord().trxlnNo;
                	 var isadvance=grid.getSelectedRecord().isadvance;
                	 if(isadvance){
                	 if(lineno==0 ){
                		 thisRdvAdvLine.setDisabled(true);
                     } else {
                    	 isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisRdvAdvLine);
                	 }
                 }
                 }
                 
        	 }
        }
        // security rules associated tabs
        else if (tabId === '789F9855EAFA40A0A4F1DC610809ED98' || tabId === 'CD3CA08EC1E342B18E438C840325CBFD' || tabId === 'CF6F6528BAC645A9A84EB009BD511A82' || tabId === '609443D6A3964370AF77D10B6859A716' || tabId === '48CC5A2353A74037B27B54217F50053E' || tabId === 'DC44C87B80344B25985C263737A78774' || tabId === '1AEAD3FB37094003AF607E944C8B35C8' || tabId === '1E3F409791B94E2BA877BB035A6C3949' || tabId === 'A1AF3BFA329A4C1B9E8B410889C4936A' || tabId === '25AF62AB8F89499287C44DDC58D3EB02' || tabId === '66BAA8CBEF624E92A59E796BE425B066' || tabId === 'B0EB700BD9CB47918D6FD77B201336E2' || tabId === '11B13C8244D449CEA40065FE8D2D8F27' || tabId === '62CB8DD2443A49E8909551E2D0E1C3D9' || tabId === '0F4DE234CEF8484CA6EE942C1E2ACAC6' || tabId === '0472D5524A474C01984B0F3EA641B16E') {
          var recordId = view.parentRecordId,
              thisDepTab = this;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                if (view.viewGrid.contextMenu) {
                  view.viewGrid.contextMenu.destroy();
                  view.viewGrid.contextMenu = null;
                  view.viewGrid.showCellContextMenus = false;
                }
                thisDepTab.setDisabled(true);
              } else {
                view.viewGrid.showCellContextMenus = true;
                isc.OBToolbar.BUTTON_PROPERTIES.eliminate.defaultUpdateFunction(thisDepTab);
              }
            });
          }
        } 
     // BG Release
        else if (tabId === 'C1779EE84BE44C30B4385F367742CE7F') {
          var recordId = view.parentRecordId,
              thisDepTab = this;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.IsDraft === 1) {
                if (view.viewGrid.contextMenu) {
                  view.viewGrid.contextMenu.destroy();
                  view.viewGrid.contextMenu = null;
                  view.viewGrid.showCellContextMenus = false;
                }
                thisDepTab.setDisabled(true);
              } else {
                view.viewGrid.showCellContextMenus = true;
                thisDepTab.setDisabled(false);
              }
            });
          }
        }
        else {
          if (view.isShowingForm) {
            this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || form.isNew);
          } else {
            if (grid.getSelectedRecords().length > 0) {
              this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState());
            } else {
              this.setDisabled(true);
            }
          }
        }
      },
      defaultUpdateFunction: function (eliminate) {
        var view = eliminate.view,
            form = view.viewForm,
            currentGrid, length, selectedRecords, i;
        if (view.isShowingTree) {
          currentGrid = view.treeGrid;
        } else {
          currentGrid = view.viewGrid;
        }
        selectedRecords = currentGrid.getSelectedRecords();
        length = selectedRecords.length;
        if (!eliminate.view.isDeleteableTable) {
          eliminate.setDisabled(true);
          return;
        }
        for (i = 0; i < length; i++) {
          if (!currentGrid.isWritable(selectedRecords[i])) {
            eliminate.setDisabled(true);
            return;
          }
          if (selectedRecords[i]._new) {
            eliminate.setDisabled(true);
            return;
          }
        }
        if (view.isShowingForm) {
          eliminate.setDisabled(form.isSaving || form.readOnly || view.singleRecord || !view.hasValidState() || form.isNew || (view.standardWindow.allowDelete === 'N'));
        } else {
          eliminate.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || !currentGrid.getSelectedRecords() || currentGrid.getSelectedRecords().length === 0 || (view.standardWindow.allowDelete === 'N'));
        }
      }
    },
    'print': {
      updateState: function () {
        var view = this.view,
            tabId = view['tabId'],
            form = view.viewForm,
            grid = view.viewGrid,
            thisRefPrint = this;
        // NOTE: tabId === 'F7A52FDAAA0346EFA07D53C125B40404' is Payment Out window (Core window) hided print button because of DEMO on Client Side
        //PO Receipt - Header Tab 
        if (tabId === '296' || tabId === 'F7A52FDAAA0346EFA07D53C125B40404') {
          this.hide();
        } else if (tabId === '800249') { // Purchase Requisition tab
          if (grid.getSelectedRecords().length === 0) {
            this.setDisabled(true);
          } else {
            this.setDisabled(false);
          }
        } else if (tabId === '290') {
          if (grid.getSelectedRecords().length === 0) {
            this.setDisabled(true);
          } else {
            var recordId = grid.getSelectedRecord().id;
            if (recordId != null && recordId != -1) {
              printButtonLastRecordId = recordId;
              var a = OB.RemoteCallManager.call('sa.elm.ob.finance.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId,
                button: 'print'
              }, {}, function (response, data, request) {
                if (data.IsDraft === 1) {
                  thisRefPrint.setDisabled(true);
                } else {
                  thisRefPrint.setDisabled(false);
                }
              });
            }
          }
        } else {
          if (view.isShowingForm) {
            this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          } else {
            this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          }
        }
      }
    },
    'escm_print_pdf': {
      updateState: function () {

        var view = this.view,
            tabId = view['tabId'];
        form = view.viewForm, grid = view.viewGrid;
        //PO Receipt tab=296, Material Issue Req=CE947EDC9B174248883292F17F03BB32, Return Tran=72A6B3CA5BE848ACA976304375A5B7A6,Custody Transfer=CB9A2A4C6DB24FD19D542A78B07ED6C1, RFP Sales Voucher=6F86F1F0E85C4A8F8DF36B5654BA3E3C
        if (tabId === '296' || tabId === 'CE947EDC9B174248883292F17F03BB32' || tabId === '72A6B3CA5BE848ACA976304375A5B7A6' || tabId === '922927563BFC48098D17E4DC85DD504C' || tabId === '31960EC365D746A180594FFB7B403ABB' || tabId === 'D04AD680E4DB4A48BB887B031A7E06A2' || tabId === 'BA8A044E0AC54DB8A51210458C4FADD9' || tabId === '6F86F1F0E85C4A8F8DF36B5654BA3E3C' || tabId === 'CB9A2A4C6DB24FD19D542A78B07ED6C1' || tabId === '8095B818800446D795B8ADFEDE104733' || tabId === '61D6CF3612134CAF942B811EC74B1F0B' || tabId === 'D6115C9AF1DD4C4C9811D2A69E42878B' || tabId === '62248BBBCF644C18A75B92AD8E50238C' || tabId === '6732339A97874A85BF73542C2B5AFF88') {
          if (grid.getSelectedRecords().length === 0) {
            this.setDisabled(true);
          }
          // added tabId === '6F86F1F0E85C4A8F8DF36B5654BA3E3C' for task #4994
          if (grid.getSelectedRecords().length === 1 && (printButtonLastRecordId !== grid.getSelectedRecord().id || tabId === '6F86F1F0E85C4A8F8DF36B5654BA3E3C' || tabId=== 'CB9A2A4C6DB24FD19D542A78B07ED6C1' || tabId=== '61D6CF3612134CAF942B811EC74B1F0B' || tabId==='296')) {
            var recordId = grid.getSelectedRecord().id,
                thisRefPrint = this;
            if (tabId == "D04AD680E4DB4A48BB887B031A7E06A2" || tabId == "6732339A97874A85BF73542C2B5AFF88") {
              recordId = view.parentRecordId;
            }
            if (recordId != null && recordId != -1) {
              printButtonLastRecordId = recordId;
              var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId,
                button: 'print'
              }, {}, function (response, data, request) {
                if (data.IsDraft === 0) {
                  thisRefPrint.setDisabled(true);
                } else {
                  thisRefPrint.setDisabled(false);
                }
              });
            }
          }
        } else {
          if (view.isShowingForm) {
            this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          } else {
            this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          }
        }
      }
    },
    'ehcm_print_pdf': {
      updateState: function () {
        var view = this.view,
            tabId = view['tabId'],
            windowId = view['windowId'];
        form = view.viewForm, grid = view.viewGrid;
        if (tabId === '076B159D222E4EEB85C70B3FEE6B22F6') {
          if (grid.getSelectedRecords().length === 0) {
            this.setDisabled(true);
          }
          if (grid.getSelectedRecords().length === 1 && (printButtonLastRecordId !== grid.getSelectedRecord().id || tabId === '076B159D222E4EEB85C70B3FEE6B22F6')) {
            var recordId = grid.getSelectedRecord().id,
                thisRefPrint = this;

            if (recordId != null && recordId != -1) {
              printButtonLastRecordId = recordId;
              var a = OB.RemoteCallManager.call('sa.elm.ob.hcm.actionHandler.irtabs.IrTabDisableProcess', {
                recordId: recordId,
                tabId: tabId,
                button: 'print'
              }, {}, function (response, data, request) {
                console.log("state>" + data.IsDraft)
                if (data.IsDraft === 0) {
                  thisRefPrint.setDisabled(true);
                } else {
                  thisRefPrint.setDisabled(false);
                }
              });
            }
          }
        } else if (tabId === '9FBF2EEF58D443EA9A403FB8D7A6DB5C') {
          this.setDisabled(true);
          if (grid.getSelectionLength() > 0 && grid.getSelectedRecord().certificateStatus == 'PR') {
            this.setDisabled(false);
          }
        } else if (tabId === 'E1FA7F1000E74C41AE4683D596C1FD7A') {
          this.setDisabled(true);
          if (grid.getSelectionLength() > 0 && grid.getSelectedRecord().decisionStatus == 'I') {
            this.setDisabled(false);
          }
        } else if (tabId === '8AB1066731C54CE295D4F1F61BD5D732') {
          this.setDisabled(true);
          if (grid.getSelectionLength() > 0 && grid.getSelectedRecord().decisionStatus == 'I' && grid.getSelectedRecord().decisionType !== 'CA') {
            var recordId = grid.getSelectedRecord().id;
            thisRefPrint = this;
            var a = OB.RemoteCallManager.call('sa.elm.ob.hcm.actionHandler.irtabs.PrintIconDisableProcess', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.EnablePrint == "true") {
                thisRefPrint.setDisabled(false);
              }
            });
          }
        } else {
          if (view.isShowingForm) {
            this.setDisabled(form.isSaving || view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          } else {
            this.setDisabled(view.readOnly || view.singleRecord || !view.hasValidState() || view.editOrDeleteOnly);
          }
        }
      }
    },
    'save': {
      action: function () {
        var view = this.view,
            tabId = view['tabId'],
            thisTab = this;
        // only for Bid Dates
        if (tabId === "754D4F75D3F54A3EBBC69496D27B9C3B") {
          var callback = function (ok) {
              if (ok) {

                thisTab.focus();
                view.savingWithShortcut = true;
                view.saveRow();
                delete view.savingWithShortcut;
              } else {
                return false;
              }

              };

          var recordId = view.parentRecordId;
          if (recordId != null && recordId != 1) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.BidDatesSaveButtonHandler', {
              recordId: recordId,
              tabId: tabId
            }, {}, function (response, data, request) {
              if (data.showAction == "true") {
                isc.ask(
                OB.I18N.getLabel('ESCM_ExtendDate_Confirm'), callback);
              } else {
                thisTab.focus();
                view.savingWithShortcut = true;
                view.saveRow();
                delete view.savingWithShortcut;
              }
            });
          }

        }
        // only for pee window before save validate the proposal have BG
        else if (tabId === "61D6CF3612134CAF942B811EC74B1F0B") {
               if(form.view.getContextInfo().inpescmBidmgmtId!=undefined &&form.view.getContextInfo().inpescmBidmgmtId!=null )
                    	 {
                    	 var recordId = form.view.getContextInfo().inpescmBidmgmtId;
                    	 var a = OB.RemoteCallManager.call('sa.elm.ob.scm.actionHandler.irtabs.PEEOnSaveHandler', {
                     recordId: recordId,
                     tabId: tabId
                   }, {}, function (response, data, request) {
                     if (data.showAction == "true") {
                    	    grid.view.messageBar.setMessage(isc.OBMessageBar.TYPE_INFO, data.showproposal);
                    	     thisTab.focus();
                          view.savingWithShortcut = true;
                          view.saveRow();
                          delete view.savingWithShortcut;
                     } else {
                       thisTab.focus();
                       view.savingWithShortcut = true;
                       view.saveRow();
                       delete view.savingWithShortcut;
                     }
                   });
                  }
               else {
                   thisTab.focus();
                   view.savingWithShortcut = true;
                   view.saveRow();
                   delete view.savingWithShortcut;
                 }
         } 
        
        
        else if (tabId === "6A3644A0025F4A4991CC3DBD7820087A") {
          var isGridEdit = OB.MainView.TabSet.getSelectedTab().pane.activeView.isEditingGrid;

          var recordId = view.viewGrid.getSelectedRecord().id;
          var isMatch;
          var matchQty;
          var penaltyAmt;
          var totalDeduct;
          var matchAmt;
          var advDeductionAmt;
          var me = this;

          if (isGridEdit) {
            var editRow = OB.MainView.TabSet.getSelectedTab().pane.activeView.viewGrid.getEditRow();
            var editRecord = OB.MainView.TabSet.getSelectedTab().pane.activeView.viewGrid.getEditValues(editRow);

            isMatch = editRecord.match;
            matchQty = editRecord.matchQty;
            penaltyAmt = editRecord.penaltyAmt;
            totalDeduct = editRecord.totalDeduct;
            matchAmt = editRecord.matchAmt;
            advDeductionAmt = editRecord.aDVDeduct;
          } else {

            isMatch = view.viewForm.getItem('match').getValue();
            matchQty = view.viewForm.getItem('matchQty').getValue();
            penaltyAmt = view.viewForm.getItem('penaltyAmt').getValue();
            totalDeduct = view.viewForm.getItem('totalDeduct').getValue();
            matchAmt = view.viewForm.getItem('matchAmt').getValue();
            advDeductionAmt = view.viewForm.getItem('aDVDeduct').getValue();
          }

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
                  advDeductionAmt: advDeductionAmt,
                  matchAmt: matchAmt
                }, {}, function (response, data, request) {
                  if (data.result == "true") {
                    totalDeduct = totalDeduct + data.penaltyAmount;
                    matchAmt = matchAmt - totalDeduct;

                    view.viewForm.setItemValue('penaltyAmt', data.penaltyAmount);
                    view.viewForm.setItemValue('totalDeduct', totalDeduct);
                    view.viewForm.setItemValue('netmatchAmt', matchAmt);

                    if (isGridEdit) {
                      var penalty = view.viewGrid.getEditItem('penaltyamt');
                      var netMatchAmtField = view.viewGrid.getEditItem('netmatchAmt');
                      var totalDeductField = view.viewGrid.getEditItem('totalDeduct');

                      penalty.grid.setEditValue(editRow, 'penaltyAmt', data.penaltyAmount);
                      netMatchAmtField.grid.setEditValue(editRow, 'netmatchAmt', matchAmt);
                      totalDeductField.grid.setEditValue(editRow, 'totalDeduct', totalDeduct);
                    }
                    isc.say(data.message);

                    thisTab.focus();
                    view.savingWithShortcut = true;
                    view.saveRow();
                    delete view.savingWithShortcut;
                  } else {
                    isc.say(data.message);
                  }
                });

              } else {
                thisTab.focus();
                view.savingWithShortcut = true;
                view.saveRow();
                delete view.savingWithShortcut
              }
              };

          if (recordId != null && isMatch && matchQty > 0 && penaltyAmt == 0) {
            var a = OB.RemoteCallManager.call('sa.elm.ob.finance.ad_process.RDVProcess.RDVOnSaveHandler', {
              recordId: recordId,
              inpAction: 'checkForPenalty',
              matchQty: matchQty,
              matchAmt: matchAmt
            }, {}, function (response, data, request) {
              if (data.addPenalty == "true") {
                isc.ask(
                OB.I18N.getLabel('EFIN_AddPenalty'), callback);
              } else if (data.hasOwnProperty('message')) {
                isc.say(data.message);
              } else {
                thisTab.focus();
                view.savingWithShortcut = true;
                view.saveRow();
                delete view.savingWithShortcut;
              }
            });
          } else {
            thisTab.focus();
            view.savingWithShortcut = true;
            view.saveRow();
            delete view.savingWithShortcut;
          }

        } else if (tabId === '62248BBBCF644C18A75B92AD8E50238C' || tabId === 'D6115C9AF1DD4C4C9811D2A69E42878B') {
          thisTab.focus();
          view.savingWithShortcut = true;
          view.saveRow();
          view.refreshChildViews();
          delete view.savingWithShortcut;
        } else {
          thisTab.focus();
          view.savingWithShortcut = true;
          view.saveRow();
          delete view.savingWithShortcut;
        }
      }

    }

  }
});