/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s): ___________
 ************************************************************************
 */

// = Manage Views =
// This file contains implementations for the 3 main actions pertaining to views: storing, deleting and 
// applying views to a window.
//
// == View state json structure ==
// The view state is stored for a complete window, i.e. for each tab of a window. The viewstate consists
// of 3 parts:
// - grid state: which columns are visible etc.
// - form state: the layout and first focus of the form
// - window state: the layout of the parent/child tabs
// 
// The view state is stored as json on the server in a structure like this:
// { 
//  viewDefinition: [
//      name: 'name',
//      isDefault: true,
//      window: {...},
//      100: {
//          form: {...},
//          grid: {...},
//          },
//      110: {
//          form: {...},
//          grid: {...},
//          }
//        ]
// }
// where 100 and 110 are tab ids.
//
// == View state stored in the UIPersonalization table ==
// On the server the view state is stored on the server in the UIPersonalization table.
// Records stored as viewstate have as type: Window and have ad_window_id set.
// 
// In the same UIPersonalization table the layout of forms is persisted as it was
// delivered in MP2. See the ob-personalize-form.js file for more information.
// Form layout records in the UIPersonalization table have type: Form and have ad_tab_id set.
//
// == Server side implementation ==
// The server side implementation of the view state saving is implemented in the 
// PersonalizationActionHandler and PersonalizationHandler classes. The first is
// called from the client, the second class (which is used by the first) contains 
// the actual logic to store and retrieve personalization settings from the database. 
//
// == Reading the state from the server ==
// When reading the viewstate from the server for a window the MP2 form layout and the newer viewstate
// are combined into one json record. 
// 
// There is one main difference between the form layout state and the
// complete view state. For the form layout state only the record on the most detailed level is read. 
// For the viewstate all records are read (as the user can choose which viewstate to apply). 
// 
// In addition the json structure read from the server side will contain information to fill the 
// comboboxes of the popups to store a view. These combos allow a user to store personalization
// records on different levels. 
// 
// The server side component which is called is the WindowSettingsActionHandler. This component is
// called from the ob-standard-window.js file in the readWindowSettings method. This action handler is 
// only called once for each specific OBStandardWindow class. The returned json object is stored in the
// Smartclient class instance (standardWindow.getClass()). 
// By storing this information on class level, only one call to the server is needed when the specific
// ADWindow is opened for the first time (during that user session). Subsequence window openings will
// read the personalization (and other settings) from the class.
// 
// The personalization json object returned from the server will consist of 3 parts:
// - forms: the form layouts stored for each tab (see the ob-personalization* files for more information)
// - views: the views, an array of viewdefinitions, one covering a complete window. Each record
//      in the array will have the following information:
//      - the window layout state
//      - view: consists of 3 parts: 
//        - name: the name to be shown in the user interface
//        - for each tab a record containing the grid and form state
//        - the window layout
//      - canEdit information (is the user authorized to change/delete the view)
//      - level information: clientId, orgId, userId, roleId, is read from the visibleAt* and user
//         property of the UIPersonalization record
//      - personalization id: the id of the personalization record
//  - formData: contains the level information at which the user can edit the views. For each level
//      (clients, orgs, roles) a 'valuemap' is contained in this object.
// ** {{{OB.Personalization.applyViewDefinition}}} **
// Apply a selected view definition to a window
OB.Personalization.applyViewDefinition = function (persId, viewDefinition, standardWindow) {
  var i, view, viewTabDefinition, showTreeGrid, selectedView, length = standardWindow.views.length,
      windowDefinition = viewDefinition.window;

  // delete the current form personalization 
  // as these will be overwritten by the new settings
  standardWindow.removeAllFormPersonalizations();

  standardWindow.selectedPersonalizationId = persId;

  // the viewdefinition contains both the global info (form, canDelete, personalizationid)  
  // set the view state for each tab
  for (i = 0; i < length; i++) {
    view = standardWindow.views[i];
    if (!selectedView && windowDefinition && windowDefinition.activeTabId && standardWindow.views[i].tabId === windowDefinition.activeTabId) {
      selectedView = view;
    }
    viewTabDefinition = viewDefinition[view.tabId];
    if (viewTabDefinition) {
      if (viewTabDefinition.viewMode === 'tree' && view.treeGrid) {
        showTreeGrid = true;
      } else {
        showTreeGrid = false;
      }
      if (view.childTabSet && viewTabDefinition.selectedTab) {
        view.childTabSet.selectTab(viewTabDefinition.selectedTab);
      }
      if (view.isRootView || view.isRenderedChildView) {
        OB.Personalization.applyViewDefinitionToView(view, viewTabDefinition, showTreeGrid);
      } else {
        // If the view has not been rendered yet, store the 'initialTabDefinition' to be able
        // to set the proper view definition as soon as the view be loaded.
        view.initialTabDefinition = viewTabDefinition;
      }
    }
  }

  if (selectedView) {
    if (windowDefinition.parentTabSetState && selectedView.parentTabSet) {
      selectedView.parentTabSet.setState(windowDefinition.parentTabSetState);
      selectedView.parentTabSet.setHeight(windowDefinition.parentTabSetHeight);
      // in this case the visibility of the top part of the parent view has to be set
      // as it can be hidden previously
      // https://issues.openbravo.com/view.php?id=18951
      if (selectedView.parentView && !selectedView.parentView.members[0].isVisible() && isc.OBStandardView.STATE_BOTTOM_MAX !== windowDefinition.parentTabSetState) {
        selectedView.parentView.members[0].show();
      }
    } else if (windowDefinition.childTabSetState && selectedView.childTabSet) {
      // in this case the visibility of the top part of the view has to be set
      // as it can be hidden previously
      // https://issues.openbravo.com/view.php?id=18951
      if (!selectedView.members[0].isVisible() && isc.OBStandardView.STATE_BOTTOM_MAX !== windowDefinition.childTabSetState) {
        selectedView.members[0].show();
      }
      selectedView.childTabSet.setState(windowDefinition.childTabSetState);
      selectedView.childTabSet.setHeight(windowDefinition.childTabSetHeight);
    }

    // force an active view
    if (standardWindow.activeView === selectedView) {
      standardWindow.activeView = null;
    }
    selectedView.setAsActiveView(true);
  }

};

// ** {{{OB.Personalization.applyViewDefinitionToView}}} **
// Apply a passed view definition to a particular view
OB.Personalization.applyViewDefinitionToView = function (view, viewTabDefinition, showTreeGrid) {
  if (!view || !viewTabDefinition) {
    return;
  }
  if (view.childTabSet && viewTabDefinition.selectedTab) {
    view.childTabSet.selectTab(viewTabDefinition.selectedTab);
  }

  // never show the form as this gives unpredictable results
  // if there is no record selected etc.
  if (view.isShowingForm) {
    view.switchFormGridVisibility();
  } else if (view.isShowingTree && !showTreeGrid) {
    OB.ToolbarUtils.hideTreeGrid(view);
  }

  if (viewTabDefinition.grid) {
    // check if the view to be applied adds new fields into the grid
    // in that case, we invalidate the cache to bring the data of the new fields when refreshing
    if (view.viewGrid.viewHasFieldsNotInGrid(viewTabDefinition.grid)) {
      view.viewGrid.invalidateCache();
    }
    //clear grouping, will be applied later
    view.viewGrid.clearGroupBy();
    view.viewGrid.setViewState(viewTabDefinition.grid);
    if (!view.viewGrid.lazyFiltering && !view.viewGrid.targetRecordId) {
      // do not refresh contents if:
      //  -lazy: requires user action to refresh
      //  -direct navigation: it is done centrally after applying personalizations
      view.viewGrid.refreshContents();
    }
  }
  if (showTreeGrid) {
    // Execute 'OB.ToolbarUtils.showTreeGrid' even if the tree grid is already shown because this function
    // also copies some configuration (criteria, ...) of the grid to the tree grid. This is needed because
    // the grid state has changed just before in the line 'view.viewGrid.setViewState(viewTabDefinition.grid)'
    OB.ToolbarUtils.showTreeGrid(view);
  }
  if (viewTabDefinition.form && view.viewForm.getDataSource()) {
    OB.Personalization.personalizeForm(viewTabDefinition, view.viewForm);
  }
};

// ** {{{OB.Personalization.getViewDefinition}}} **
// Retrieve the view state from the window.
// The levelInformation contains the level at which to store the view. After the save the internal
// view state is stored in the standardWindow.getClass().personalization object.
OB.Personalization.getViewDefinition = function (standardWindow, name, isDefault) {
  var view, persDataByTab, personalizationData = {},
      i, formFields, formData, length = standardWindow.views.length;

  // retrieve the viewstate from the server
  for (i = 0; i < length; i++) {
    persDataByTab = {};
    view = standardWindow.views[i];

    if (view.isRootView || view.isRenderedChildView) {
      // get the form personalization information
      if (!view.viewForm.getDataSource()) {
        // If the datasource is not yet set, view.viewForm.fields it is not set yet.
        // Get them directly form the view definition.
        formFields = view.formFields;
      } else {
        formFields = null;
      }

      formData = OB.Personalization.getPersonalizationDataFromForm(view.viewForm, formFields);
      persDataByTab.form = formData.form;

      if (view.isShowingTree) {
        // Copy the criteria from the tree grid to the view grid
        view.treeGrid.copyCriteriaToViewGrid();
        persDataByTab.viewMode = 'tree';
      }
      // and the grid state
      persDataByTab.grid = view.viewGrid.getViewState(false, true);

      if (view.childTabSet && view.childTabSet.getSelectedTabNumber() >= 0) {
        persDataByTab.selectedTab = view.childTabSet.getSelectedTabNumber();
      }
    } else if (view.initialTabDefinition) {
      // If there it is a non-rendered child view and there is an initial tab definition it means that
      // the view has not been visited yet so it is not possible that exist any modificiation in it
      persDataByTab = isc.shallowClone(view.initialTabDefinition);
    }
    // and store it in the overall structure
    personalizationData[view.tabId] = persDataByTab;
  }
  personalizationData.name = name;
  if (isDefault) {
    personalizationData.isDefault = true;
  }
  if (standardWindow.activeView) {
    personalizationData.window = {
      activeTabId: standardWindow.activeView ? standardWindow.activeView.tabId : null
    };

    if (standardWindow.activeView.parentTabSet) {
      personalizationData.window.parentTabSetState = standardWindow.activeView.parentTabSet.getState();
      personalizationData.window.parentTabSetHeight = standardWindow.activeView.parentTabSet.getHeight();
    } else if (standardWindow.activeView.childTabSet) {
      personalizationData.window.childTabSetState = standardWindow.activeView.childTabSet.getState();
      personalizationData.window.childTabSetHeight = standardWindow.activeView.childTabSet.getHeight();
    }
  }
  return personalizationData;
};

// ** {{{OB.Personalization.storeViewDefinition}}} **
// Retrieve the view state from the window and stores it in the server using the specified name and id (if set).
// The levelInformation contains the level at which to store the view. After the save the internal
// view state is stored in the standardWindow.getClass().personalization object.
OB.Personalization.storeViewDefinition = function (standardWindow, levelInformation, persId, name, isDefault) {
  var params, personalizationData = OB.Personalization.getViewDefinition(standardWindow, name, isDefault);

  // if there is a personalization id then use that
  // this ensures that a specific record will be updated
  // on the server.
  // the target means that only the view property of the total
  // user interface personalization is stored.
  // also persist the level information
  if (persId) {
    params = {
      action: 'store',
      target: 'viewDefinition',
      clientId: levelInformation.clientId,
      orgId: levelInformation.orgId,
      roleId: levelInformation.roleId,
      userId: levelInformation.userId,
      personalizationId: persId,
      applyLevelInformation: true
    };

  } else {
    // this case is used if there is no personalization record
    // use the level information to store the view state
    params = {
      action: 'store',
      target: 'viewDefinition',
      clientId: levelInformation.clientId,
      orgId: levelInformation.orgId,
      roleId: levelInformation.roleId,
      userId: levelInformation.userId,
      windowId: standardWindow.windowId,
      applyLevelInformation: true
    };
  }

  // and store on the server
  OB.RemoteCallManager.call('org.openbravo.client.application.personalization.PersonalizationActionHandler', personalizationData, params, function (resp, data, req) {
    var i = 0,
        fnd = false,
        length, newView, personalization = standardWindow.getClass().personalization,
        views = personalization && personalization.views ? personalization.views : [];

    standardWindow.selectedPersonalizationId = data.personalizationId;

    // create a new structure, the same way as it is 
    // returned from the server
    newView = isc.addProperties({
      canEdit: true,
      personalizationId: data.personalizationId,
      viewDefinition: personalizationData
    }, levelInformation);

    // when returning update the in-memory entry,
    // first check if there is an existing record, if so 
    // update it
    if (views) {
      length = views.length;
      for (i = 0; i < length; i++) {
        if (views[i].personalizationId === data.personalizationId) {
          views[i] = newView;
          fnd = true;
          break;
        }
      }
    }

    // not found create a new one, take into account
    // that the initial structure maybe empty
    if (!fnd) {
      if (!standardWindow.getClass().personalization) {
        standardWindow.getClass().personalization = {};
      }
      if (!standardWindow.getClass().personalization.views) {
        standardWindow.getClass().personalization.views = [];
        views = standardWindow.getClass().personalization.views;
      }
      views.push(newView);
      if (newView.viewDefinition && newView.viewDefinition.isDefault) {
        OB.PropertyStore.set('OBUIAPP_DefaultSavedView', data.personalizationId, standardWindow.windowId);
      }

      // sort the viewdefinitions
      views.sort(function (v1, v2) {
        var t1 = v1.viewDefinition.name,
            t2 = v2.viewDefinition.name;
        if (t1 < t2) {
          return -1;
        } else if (t1 === t2) {
          return 0;
        }
        return 1;
      });

    }
  });
};

//** {{{OB.Personalization.deleteViewDefinition}}} **
// Delete the view definition from the server, also remove it from the 
// in-memory structure.
OB.Personalization.deleteViewDefinition = function (standardWindow, personalizationId) {
  OB.RemoteCallManager.call('org.openbravo.client.application.personalization.PersonalizationActionHandler', {}, {
    personalizationId: personalizationId,
    action: 'delete'
  }, function (resp, data, req) {
    var personalization = standardWindow.getClass().personalization,
        length, i, views = personalization && personalization.views ? personalization.views : [];

    if (views) {
      length = views.length;
      // remove the entry from the global list
      for (i = 0; i < length; i++) {
        if (views[i].personalizationId === personalizationId) {
          views.splice(i, 1);
          break;
        }
      }
      if (OB.PropertyStore.get('OBUIAPP_DefaultSavedView', standardWindow.windowId) === personalizationId) {
        // If the 'Default View' has been deleted, the local property pointing to it is still
        // in the browser until the following logout/login, so it should be deleted
        delete OB.Properties['OBUIAPP_DefaultSavedView' + '_' + standardWindow.windowId];
      }
    }
  });
};