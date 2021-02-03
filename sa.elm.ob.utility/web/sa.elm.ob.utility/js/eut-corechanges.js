/*
 *************************************************************************
 * This file is used to modify the core js files so that logic implemented
 * in this file override the core changes in openbravo
 * 
 * Author:  Qualian Technologies
 ************************************************************************
 */


isc.OBSelectorItem.addProperties({
  // we are setting this.setValueFromRecord(null); if we deleting the character using keyboard
  // because this will set value to selector so that callout will trigger
  changed: function (form, item, newValue) {
    var identifier;
    // only do the identifier actions when clearing
    // in all other cases pickValue is called
    if (!newValue) {
      if (this.getElementValue() === '' && this.pickList && this.pickList.getSelectedRecord() && this.pickList.getSelectedRecord().id) {
        this.setValueFromRecord(null);
        return;
      }
      this.setValueFromRecord(null);
    }
    if (OB.Utilities.isUUID(newValue)) {
      identifier = this.mapValueToDisplay(newValue);
    } else {
      identifier = newValue;
    }

    // check if the whole item identifier has been entered
    // see issue https://issues.openbravo.com/view.php?id=22821
    if (OB.Utilities.isUUID(this.mapDisplayToValue(identifier)) && this._notUpdatingManually !== true) {
      this.fullIdentifierEntered = true;
    } else {
      delete this.fullIdentifierEntered;
    }

    //Setting the element value again to align the cursor position correctly.
    //Before setting the value check if the identifier is part of the value map or the full identifier is entered.
    //If it fails set newValue as value.
    if ((this.valueMap && this.valueMap[newValue] === identifier && identifier.trim() !== '') || this.fullIdentifierEntered) {
      this.setElementValue(identifier);
    } else {
      this.setElementValue(newValue);
    }
  }
});