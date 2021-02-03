<#--
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
 * All portions are Copyright (C) 2011-2013 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
*/
-->

{
<@compress single_line=true>
  <#if data.whereClause != "">
  whereClause: '${data.whereClause?js_string}',
  </#if>
  <#if data.whereClauseSQL != "">
  sqlWhereClause: '${data.whereClauseSQL?js_string}',
  </#if>  
  <#if data.orderByClause != "">
  orderByClause: '${data.orderByClause?js_string}',
  </#if>
  <#if data.orderByClauseSQL != "">
  sqlOrderByClause: '${data.orderByClauseSQL?js_string}',
  </#if>
  <#if data.sortField != "">
  sortField: '${data.sortField?js_string}',
  </#if>
  <#if data.filterClause != "">
  filterClause: '${data.filterClause?js_string}',
  </#if>
  <#if data.filterClauseSQL != "">
  sqlFilterClause: '${data.filterClauseSQL?js_string}',
  </#if>
  <#if data.filterName != "">
  filterName: '${data.filterName?js_string}',
  </#if>
  <#if data.lazyFiltering>
  lazyFiltering: ${data.lazyFiltering?string},
  </#if>  
  <#if data.alwaysFilterFksByIdentifier>
  alwaysFilterFksByIdentifier: ${data.alwaysFilterFksByIdentifier?string},
  </#if>   
  dummy: true
</@compress>
}