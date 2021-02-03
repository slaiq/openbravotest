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
 * Contributor(s):  Prakash.
 ************************************************************************
 */

isc.OBTextItem.addProperties({
    itemHoverHTML: function(item, form) {
    	uniqueCodeFields = ['9E23AF8BF43C4923941DD656B24D3299', '2A026B140B034735B9214F01301C0EAE', '3C2E81B82C644C2294777C8B9CF507A3', 'BCF6EAEE66DF4EA281798A1F4D277B29', '6A4BB44EFDEC4DB180154A6BAEF2458F', 'AC0DF50A7AD4440EB7B9F250F17B0B54', '341EE83154784B8CB46A3E2950CAF977', '2E347337FA124DF29972DE44BA90CC55', '4AB4810316D54B1CB9B5BF10957FF9BC'];

        if (this.isDisabled()) {
            if (uniqueCodeFields.indexOf(item.id) > -1) {
                return form.getFieldFromInpColumnName('inpuniquecodename') ? form.getFieldFromInpColumnName('inpuniquecodename')._value : this.getValue();
            } else {
                return this.getValue();
            }
        } else if (this.mask) {
            return this.mask;
        }
    }
});