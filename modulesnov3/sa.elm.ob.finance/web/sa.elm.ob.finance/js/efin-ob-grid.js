/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use. this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  Prakash and Gopal.
 ************************************************************************
 */

OB.EFIN = OB.EFIN || {};
OB.EFIN.UniqueCode = OB.EFIN.UniqueCode || {};

//Add (Window Id : tabId : criteria(IOnly if needs to be filtered by generating unqiuecode)) to list, to display unique code filter
OB.EFIN.UniqueCode.Windows = {
  //Budget
  '0D8568D5973442B6ABA7EEF7D044CF78': {
    'tabId': 'BC56B7B735274931B0A78E1F41608470'
  },
  //Account Dimension
  '3CFA76455DC54E6EB6BAEFDE43CDF199': {
    'tabId': 'C6211EBC186642348ECC5BDB8E753BD8'
  },
  //Budget Adjustment
  '609AC728E0234C57A2A4EDF13DF0BF1F': {
    'tabId': 'E0FBE98B570248128541D4CF5E8011D2',
    //'criteria': 'accountingCombination'
  },
  //Budget Revision
  '05C3944B54FE4C5DA0E735D1144DCB94': {
    'tabId': 'E68453B4E62548C6B5E79FEDE3C36586',
    'criteria': 'accountingCombination'
  },
  //Funds Request Management
  '4824ABD4AE6E49F68F2AAFE976EFFEC2': {
    'tabId': '50A8467D2F614400A757CC1465A383DD',
    'criteria1': 'fromaccount',
    'criteria2': 'toaccount'
  },
  //Budget Enquiry -->lines
  'A4B43D3C9E9E47D39AD74BE7248236D7': {
    'tabId': '4B84B7F7B6DC488F9E0B4FB754199846'
  }
};

//Method that filters ListGrid based on UniqueCodeFilter
OB.Utilities.Action.set('filterGridByUniqueCode', function (paramObj) {
  var windowId = OB.MainView.TabSet.getSelectedTab().windowId;
  var selectedTabId = OB.MainView.TabSet.getSelectedTab().pane.activeView.tabId
  var window = OB.EFIN.UniqueCode.Windows[windowId];

  if (window) {
    var unqiueCodeTabId = window.tabId;

    if (unqiueCodeTabId != selectedTabId) {
      OB.MainView.TabSet.selectChildTab();
    }

    var pushCriteria = function (oldCriteria, criteriaName, operation, value) {
        oldCriteria.criteria.push({
          fieldName: criteriaName,
          operator: operation,
          value: value,
          _constructor: "AdvancedCriteria"
        });
        };

    var removeCriteria = function (oldCriteria, criteriaName) {
        var len = oldCriteria.criteria.length;
        for (var i = 0; i < len; i++) {
          if (oldCriteria.criteria[i].fieldName === criteriaName) {
            oldCriteria.criteria.splice(i, 1);
            break;
          }
        }
        };

    var oldCri = OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewGrid.getCriteria();
    if (window.criteria) {
      //Set Filter Criteria
      removeCriteria(oldCri, window.criteria + '$_identifier');
      pushCriteria(oldCri, window.criteria + '$_identifier', 'iContains', paramObj.uniqueCode);
    } else if (window.criteria1 && window.criteria2) {
      //Set Filter Criteria for from and to accounts
      if (paramObj.isFilterToAccount && paramObj.isFilterToAccount === true) {
        removeCriteria(oldCri, window.criteria2 + '$_identifier');
        pushCriteria(oldCri, window.criteria2 + '$_identifier', 'iContains', paramObj.uniqueCode);
      } else {
        removeCriteria(oldCri, window.criteria1 + '$_identifier');
        pushCriteria(oldCri, window.criteria1 + '$_identifier', 'iContains', paramObj.uniqueCode);
      }
    } else {
      //Set Filter Criteria for 9 dimensions
      var accountCriteriaName = (window.tabId === 'C6211EBC186642348ECC5BDB8E753BD8' || window.tabId === '4B84B7F7B6DC488F9E0B4FB754199846' || window.tabId === 'E0FBE98B570248128541D4CF5E8011D2' ) ? 'account$_identifier' : 'accountElement$_identifier';
      var deptCriteriaName = (window.tabId === '4B84B7F7B6DC488F9E0B4FB754199846' || window.tabId === 'E0FBE98B570248128541D4CF5E8011D2') ? 'department$_identifier' : 'salesRegion$_identifier';
      var funClassiCriteriaName = (window.tabId === '4B84B7F7B6DC488F9E0B4FB754199846' || window.tabId === 'E0FBE98B570248128541D4CF5E8011D2') ? 'functionalClassfication$_identifier' : 'activity$_identifier';
     // var futureoneCriteriaName = (window.tabId === '4B84B7F7B6DC488F9E0B4FB754199846' || window.tabId === 'E0FBE98B570248128541D4CF5E8011D2' ) ? 'future1$_identifier' : 'stDimension$_identifier';
      var orgCriteriaName = (window.tabId === 'E0FBE98B570248128541D4CF5E8011D2' ) ? 'orgid$_identifier' : 'organization$_identifier';
      var projectCriteriaName = (window.tabId === 'E0FBE98B570248128541D4CF5E8011D2' ) ? 'subAccount$_identifier' : 'project$_identifier';
      
      removeCriteria(oldCri, orgCriteriaName);
      removeCriteria(oldCri, deptCriteriaName);
      removeCriteria(oldCri, accountCriteriaName);
      removeCriteria(oldCri, projectCriteriaName);
      removeCriteria(oldCri, 'salesCampaign$_identifier');
      removeCriteria(oldCri, 'businessPartner$_identifier');
      removeCriteria(oldCri, funClassiCriteriaName);
    //  removeCriteria(oldCri, futureoneCriteriaName);
    //  removeCriteria(oldCri, 'ndDimension$_identifier');
      
      if (paramObj.org) {
        pushCriteria(oldCri, orgCriteriaName, 'iContains', paramObj.org);
      }
      if (paramObj.dept) {
        pushCriteria(oldCri, deptCriteriaName, 'iContains', paramObj.dept);
      }
      if (paramObj.acct) {
        pushCriteria(oldCri, accountCriteriaName, 'iContains', paramObj.acct);
      }
      if (paramObj.proj) {
        pushCriteria(oldCri, projectCriteriaName, 'iContains', paramObj.proj);
      }
      if (paramObj.budTyp) {
        pushCriteria(oldCri, 'salesCampaign$_identifier', 'iContains', paramObj.budTyp);
      }
      if (paramObj.entity) {
        pushCriteria(oldCri, 'businessPartner$_identifier', 'iContains', paramObj.entity);
      }
      if (paramObj.activity) {
        pushCriteria(oldCri, funClassiCriteriaName, 'iContains', paramObj.activity);
      }
     // if (paramObj.future1) {
    //    pushCriteria(oldCri, futureoneCriteriaName, 'iContains', paramObj.future1);
     // }
     // if (paramObj.future2) {
    //    pushCriteria(oldCri, 'ndDimension$_identifier', 'iContains', paramObj.future2);
    //  }
    }
    OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewGrid.clearFilter(false, true);

    OB.MainView.TabSet.getSelectedTab().pane.activeView.dataSource.view.viewGrid.setCriteria(oldCri);
  }
});

//Add (UniqueCode Field Id : UniqueCodeName Field Name) to list, to show tool tip for unique code
OB.EFIN.UniqueCode.Fields = {
  //Account Dimension
  'CBDA4055FC604A5FB39E9B211470DB20': 'efinUniquecodename',
  //Budget
  '9C9E13254CFE48208E342EF9CC34B81E': 'uniqueCodeName',
  // Budget
  '2F79AE5ED1B845EF83219338244AE5FD': 'uniqueCodeName',
  //Budget Adjustment
  'D19CC0D800D64BEE881B2836477335BC': 'uniqueCodeName',
  // Funds Request Management
  '8E7978CFDD1E44359E79269DEC8540DB': 'fromuniquecodename',
  // Funds Request Management
  '03D5BC5E5558406B90EA5ED90152A07C': 'touniquecodename',
  //Budget Revision
  '4C45E146110C40E3808E8F6923F40484': 'uniqueCodeName',
  //Budget Revision
  '9454EF67B2624A02A132F753078D1B7F': 'uniqueCodeName',
  //Budget Enquiry -- >Lines
  'D2FB6A38AC7B4CB2A7A3EB51BE1A84A2': 'uniqueCodeName',
  //Encumbrance  -->Lines
  'F7AE15D9C5C64E1A954DBA114F9A02F9': 'uniqueCodeName',
  //Budget Enquiry --> Encumbrance
  '33E6F91CC8FB441C8CBEC3688214C11C': 'uniqueCodeName',
  //purchase requisition --> line
  '1A3E355B67AC46F4AD25E5F393CF466F': 'efinUniquecodename',
  //po -- > line
  '3C2234A759FA4A02B55E49B3C31AC932': 'eFINUniqueCodeName',
  //Purchase Invoice
  //'866FDB0C9BB540A78E116B330E0A23CC': 'efinUniquename',
  // Simple G/L Journal
  '0367E46826EA4BE296C19B9D79276A32': 'efinUniquecodevalue',
  '4039FDFA71AF4A788438A13B2E487806': 'efinUniquecodevalue',
  // Order to receive
  '1505DAD23044479797C4A42C446C6B9E': 'eFINUniqueCodeName'
};

//Enabled hover property for cells in treegrid 
isc.OBTreeViewGrid.addProperties({
  canHover: true
});

//Add cell over property to display tooltip for cells in tree grid
isc.OBTreeViewGrid.addProperties({
  cellHoverHTML: function (record, rowNum, colNum) {
    var ret, field = this.getField(colNum),
        fieldName = this.getFieldName(colNum),
        cellErrors, prefix = '',
        func = this.getGridSummaryFunction(field),
        isGroupOrSummary = record && (record[this.groupSummaryRecordProperty] || record[this.gridSummaryRecordProperty]);

    if (!record) {
      return;
    }

    if (func && (isGroupOrSummary)) {
      if (func === 'sum') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionSum');
      }
      if (func === 'min') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionMin');
      }
      if (func === 'max') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionMax');
      }
      if (func === 'count') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionCount');
      }
      if (func === 'avg') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionAvg');
      }
      if (prefix) {
        prefix = prefix + ' ';
      }
    }

    if (this.isCheckboxField(field)) {
      return OB.I18N.getLabel('OBUIAPP_GridSelectColumnPrompt');
    }

    if (this.cellHasErrors(rowNum, colNum)) {
      cellErrors = this.getCellErrors(rowNum, colNum);
      // note cellErrors can be a string or array
      // accidentally both have the length property
      if (cellErrors && cellErrors.length > 0) {
        return OB.Utilities.getPromptString(cellErrors);
      }
    }
    if (record && record[isc.OBViewGrid.ERROR_MESSAGE_PROP]) {
      return record[isc.OBViewGrid.ERROR_MESSAGE_PROP];
    }

    this.inCellHoverHTML = true;
    ret = this.Super('cellHoverHTML', arguments);

    if (colNum === 0) {
      //First cell in tree returns html, so fetch tooltip value manually
      ret = record[fieldName];
    }

    //Unique Code Tool Tip Text
    if (OB.EFIN.UniqueCode.Fields[field.id]) {
      ret = record[OB.EFIN.UniqueCode.Fields[field.id]] ? record[OB.EFIN.UniqueCode.Fields[field.id]] : ret;
    }

    var prefix = '';
    delete this.inCellHoverHTML;
    return prefix + (ret ? ret : '');
  }
});


//Overrided cell over property to display unique code name in tooltip for Listgrid
isc.OBGrid.addProperties({
  cellHoverHTML: function (record, rowNum, colNum) {

    var ret, field = this.getField(colNum),
        cellErrors, msg = '',
        prefix = '',
        i, func = this.getGridSummaryFunction(field),
        isGroupOrSummary = record && (record[this.groupSummaryRecordProperty] || record[this.gridSummaryRecordProperty]);

    if (!record) {
      return;
    }

    if (func && (isGroupOrSummary)) {
      if (func === 'sum') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionSum');
      }
      if (func === 'min') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionMin');
      }
      if (func === 'max') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionMax');
      }
      if (func === 'count') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionCount');
      }
      if (func === 'avg') {
        prefix = OB.I18N.getLabel('OBUIAPP_SummaryFunctionAvg');
      }
      if (prefix) {
        prefix = prefix + ' ';
      }
    }

    if (this.isCheckboxField(field)) {
      return OB.I18N.getLabel('OBUIAPP_GridSelectColumnPrompt');
    }

    if (this.cellHasErrors(rowNum, colNum)) {
      cellErrors = this.getCellErrors(rowNum, colNum);
      // note cellErrors can be a string or array
      // accidentally both have the length property
      if (cellErrors && cellErrors.length > 0) {
        return OB.Utilities.getPromptString(cellErrors);
      }
    }
    if (record && record[isc.OBViewGrid.ERROR_MESSAGE_PROP]) {
      return record[isc.OBViewGrid.ERROR_MESSAGE_PROP];
    }

    this.inCellHoverHTML = true;
    ret = this.Super('cellHoverHTML', arguments);

    //Unique Code Tool Tip Text
    if (OB.EFIN.UniqueCode.Fields[field.id]) {
      ret = record[OB.EFIN.UniqueCode.Fields[field.id]] ? record[OB.EFIN.UniqueCode.Fields[field.id]] : ret;
    }

    delete this.inCellHoverHTML;
    return prefix + (ret ? ret : '');
  }
});