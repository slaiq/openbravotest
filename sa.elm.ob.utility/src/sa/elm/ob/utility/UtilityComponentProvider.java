package sa.elm.ob.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;

@ApplicationScoped
@ComponentProvider.Qualifier(UtilityComponentProvider.PROVIDER)
public class UtilityComponentProvider extends BaseComponentProvider {

  public static final String PROVIDER = "EUT_Provider";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    return null;
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    globalResources.add(createStaticResource(
        "web/org.openbravo.client.application/js/main/eut-standard-view-datasource.js", false));
    // globalResources.add(createStaticResource("web/org.openbravo.client.application/js/grid/eut-view-grid.js",
    // false));
    // globalResources.add(createStaticResource("web/sa.elm.ob.utility/js/form/eut-formitem-minidaterange.js",
    // false));
    globalResources.add(createStaticResource("web/sa.elm.ob.utility/js/Utility.js", false));
    globalResources.add(createStaticResource("web/js/common/DateConverter.js", false));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.utility/js/form/eut-formitem-hijri-datechooser.js", true));
    globalResources.add(
        createStaticResource("web/sa.elm.ob.utility/js/form/eut-formitem-hijri-date.js", true));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.utility/js/eut-gridProperties.js", true));
    globalResources.add(createStaticResource(
        "web/org.openbravo.userinterface.smartclient/isomorphic/eut-DateGrid.js", true));
    globalResources.add(createStaticResource("web/sa.elm.ob.utility/js/DateUtils.js", true));
    globalResources.add(createStaticResource("web/js/jquery-1.7.2.min.js", true));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.plugin.js", true));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.js", true));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.plus.js", true));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.js",
        true));
    globalResources.add(createStaticResource(
        "web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.ummalqura.js",
        true));
    globalResources
        .add(createStaticResource("web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_PARAMETER + "/sa.elm.ob.utility/eut-form-styles.js", false));
    // globalResources.add(createStyleSheetResource("web/sa.elm.ob.utility/js/jquery.calendars.package-2.0.2/jquery.calendars.picker.css",
    // true));
    globalResources
        .add(createStyleSheetResource(
            "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
                + KernelConstants.SKIN_PARAMETER + "/sa.elm.ob.utility/eut-form-styles.css",
            false));
    globalResources.add(createStaticResource("web/sa.elm.ob.utility/js/Connection.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.utility/js/eut-corechanges.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.utility/js/eut_ob-view-form.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.utility/js/eut_ob-formitem-number.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.utility/js/eut_ob-formitem-search.js", false));
    globalResources.add(createStyleSheetResource(
        "web/org.openbravo.userinterface.smartclient/openbravo/skins/"
            + KernelConstants.SKIN_PARAMETER + "/sa.elm.ob.utility/eut_request_more_info.css",
        false));
    globalResources.add(
        createStaticResource("web/sa.elm.ob.utility/js/eut-ob-parameter-window-view.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.utility/js/eut-standard-view.js", false));
    globalResources.add(
        createStaticResource("web/sa.elm.ob.utility/js/eut-ob-view-form-attachments.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.utility/js/TSS_Script.min.js", false));
    globalResources.add(createStaticResource("web/sa.elm.ob.utility/js/eut-action-def.js", false));
    globalResources
        .add(createStaticResource("web/sa.elm.ob.utility/js/digitalsignature.js", false));
    return globalResources;

  }
}
