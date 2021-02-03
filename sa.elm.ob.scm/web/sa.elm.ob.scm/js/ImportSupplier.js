(function () {
  var buttonProps = {
      action: function(){
        var wi = OB.Layout.ClassicOBCompatibility.Popup.open('', 600, 600, OB.Utilities.applicationUrl("sa.elm.ob.scm.ad_process.importsupplier/ImportSupplier") , '', null, false, false, true);
      },
      buttonType: 'escm_import_supplier',
      prompt: OB.I18N.getLabel('escm_import_supplier'),
    };
  
  // register the button
  // the first parameter is a unique identification so that one button can not be registered multiple times.
  OB.ToolbarRegistry.registerButton(buttonProps.buttonType, isc.OBToolbarIconButton, buttonProps, 1000, '220');
}());