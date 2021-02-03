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
 * All portions are Copyright (C) 2010-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.kernel;

import java.io.File;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.module.Module;
import org.openbravo.service.web.WebServiceUtil;

/**
 * The component representing the component in the
 * 
 * @author mtaal
 * @author iperdomo
 */
public class StaticResourceComponent extends BaseComponent {
  private static final Logger log = Logger.getLogger(StaticResourceComponent.class);

  public static final String GEN_TARGET_LOCATION = "/web/js/gen";

  @Inject
  @Any
  private Instance<ComponentProvider> componentProviders;

  private Boolean isInDevelopment;

  @Override
  public boolean isInDevelopment() {
    if (isInDevelopment == null) {
      isInDevelopment = false;
      for (ComponentProvider provider : componentProviders) {
        final List<ComponentResource> resources = provider.getGlobalComponentResources();
        if (resources == null || resources.size() == 0) {
          continue;
        }
        if (provider.getModule().isInDevelopment()) {
          isInDevelopment = true;
          return isInDevelopment;
        }
      }
    }
    return isInDevelopment;
  }

  /**
   * @return returns this instance
   * @see org.openbravo.client.kernel.BaseComponent#getData()
   */
  public Object getData() {
    return this;
  }

  @Override
  public String getETag() {
    return String.valueOf(System.currentTimeMillis()); // prevent cache
  }

  @Override
  public String generate() {
    final long t1 = System.currentTimeMillis();

    try {
      // note the document.write content must be divided up like this, if the document.write
      // contains a complete string like <script or </script> then the browser will execute
      // them
      // directly and not the document.write, see here:
      // http://www.codehouse.com/javascript/articles/external/

      if (isClassicMode()) {
        // set in the session that we are looking at the new ui
        // note injecting the HttpSession through Weld does not work
        // as it will instantiate one of the subclasses of HttpSession
        // defined in the RequestContext
        final HttpSession session = (HttpSession) getParameters().get(KernelConstants.HTTP_SESSION);
        session.setAttribute("#Hide_BackButton".toUpperCase(), "true");
        OBContext.getOBContext().setNewUI(true);
      }

      StringBuilder result = new StringBuilder();
      final String scriptPath = getContextUrl() + GEN_TARGET_LOCATION.substring(1) + "/"
          + getStaticResourceFileName() + ".js";

      if (isClassicMode()) {
        result.append("document.write(\"<LINK rel='stylesheet' type='text/css' href='"
            + getContextUrl()
            + "org.openbravo.client.kernel/OBCLKER_Kernel/StyleSheetResources?_skinVersion="
            + KernelConstants.SKIN_DEFAULT + "&_mode=" + KernelConstants.MODE_PARAMETER_CLASSIC
            + "'></link>\");\n");
        result
            .append("var isomorphicDir='../web/org.openbravo.userinterface.smartclient/isomorphic/';\n");

        final String scDevModulePackage = "org.openbravo.userinterface.smartclient.dev";
        if (KernelUtils.getInstance().isModulePresent(scDevModulePackage)
            && KernelUtils.getInstance().getModule(scDevModulePackage).isInDevelopment()) {
          result
              .append("document.write('<'+'SCRIPT SRC=' + window.isomorphicDir + 'ISC_Combined.uncompressed.js><'+'/SCRIPT>');");
        }
      }
      result.append("document.write(\"<s\" + \"cript type='text/javascript' src='" + scriptPath
          + "'><\\/s\"+\"cript>\");");
      return result.toString();
    } catch (Exception e) {
      log.error("Error generating component; " + e.getMessage(), e);
    } finally {
      log.debug("StaticResourceComponent generation took: " + (System.currentTimeMillis() - t1)
          + "ms");
    }
    return "";
  }

  public String getId() {
    return KernelConstants.RESOURCE_COMPONENT_ID;
  }

  /**
   * @return all static resources needed by the application and placed in the top of the application
   *         page, in order based on module dependencies and using an unique version string to force
   *         client side reload or caching.
   */
  public String getStaticResourceFileName() {
    final List<Module> modules = KernelUtils.getInstance().getModulesOrderedByDependency();
    final ServletContext context = (ServletContext) getParameters().get(
        KernelConstants.SERVLET_CONTEXT);
    final StringBuffer sb = new StringBuffer();

    final String skinParam;
    if (getParameters().containsKey(KernelConstants.SKIN_PARAMETER)) {
      skinParam = (String) getParameters().get(KernelConstants.SKIN_PARAMETER);
    } else {
      skinParam = KernelConstants.SKIN_DEFAULT;
    }

    int cntDynamicScripts = 0;
    final String appName = getApplicationName();

    for (Module module : modules) {
      for (ComponentProvider provider : componentProviders) {
        final List<ComponentResource> resources = provider.getGlobalComponentResources();
        if (resources == null || resources.size() == 0) {
          continue;
        }

        if (provider.getModule().getId().equals(module.getId())) {
          for (ComponentResource resource : resources) {

            if (!resource.isValidForApp(appName)) {
              continue;
            }

            log.debug("Processing resource: " + resource);
            String resourcePath = resource.getPath();
            if (resource.getType() == ComponentResourceType.Stylesheet) {
              // do these differently...
            } else if (resource.getType() == ComponentResourceType.Static) {
              if (resourcePath.startsWith(KernelConstants.KERNEL_JAVA_PACKAGE)) {
                final String[] pathParts = WebServiceUtil.getInstance().getSegments(
                    resourcePath.substring(KernelConstants.KERNEL_JAVA_PACKAGE.length()));
                final Component component = provider.getComponent(pathParts[1], getParameters());
                sb.append(ComponentGenerator.getInstance().generate(component)).append("\n");
              } else {

                // Skin version handling
                if (resourcePath.contains(KernelConstants.SKIN_PARAMETER)) {
                  resourcePath = resourcePath.replaceAll(KernelConstants.SKIN_PARAMETER, skinParam);
                }

                if (!resourcePath.startsWith("/")) {
                  // Tomcat 8 forces getRealPath to start with a slash
                  resourcePath = "/" + resourcePath;
                }

                try {
                  final File file = new File(context.getRealPath(resourcePath));
                  if (!file.exists() || !file.canRead()) {
                    log.error(file.getAbsolutePath() + " cannot be read");
                    continue;
                  }
                  String resourceContents = FileUtils.readFileToString(file, "UTF-8");
                  sb.append(resourceContents).append("\n");
                } catch (Exception e) {
                  log.error("Error reading file: " + resource, e);
                }
              }
            } else if (resource.getType() == ComponentResourceType.Dynamic) {
              if (resourcePath.startsWith("/") && getContextUrl().length() > 0) {
                resourcePath = getContextUrl() + resourcePath.substring(1);
              } else {
                resourcePath = getContextUrl() + resourcePath;
              }

              sb.append("$LAB.script('" + resourcePath
                  + "').wait(function(){var _exception; try{\n");
              cntDynamicScripts++;
            } else {
              log.error("Resource " + resource + " not supported");
            }
          }
        }
      }
    }

    if (!"".equals(sb.toString())) {
      /*
       * If a module is in development or the application is running the tests, add the isDebug
       * variable to the generated javascript file.
       * 
       * If the isDebug variable is present in the javascript files, the code that calls
       * OB.UTIL.Debug will not be executed
       * 
       * This option is intended to run additional code (checks, etc) that will not be run while in
       * production.
       * 
       * This improves performance at the same time that the developer have a tool to improve
       * stability.
       * 
       * TODO: add an algorithm to remove the OB.UTIL.Debug code and calls from the generated
       * javacript file
       * 
       * TODO: don't load the ob-debug.js file if not in use
       */
      if (isInDevelopment()
          || OBPropertiesProvider.getInstance().getBooleanProperty("test.environment")) {
        // append a global isDebug var and the causes that provoked the application to enter Debug
        // mode
        sb.insert(
            0,
            String
                .format(
                    "var isDebug = true;\nvar debugCauses = {\n  isInDevelopment: %s,\n  isTestEnvironment: %s\n};\n\n",
                    isInDevelopment(),
                    OBPropertiesProvider.getInstance().getBooleanProperty("test.environment")));
      }
      sb.append("if (window.onerror && window.onerror.name === '"
          + KernelConstants.BOOTSTRAP_ERROR_HANDLER_NAME + "') { window.onerror = null; }");
      sb.append("if (typeof OBStartApplication !== 'undefined' && Object.prototype.toString.call(OBStartApplication) === '[object Function]') { OBStartApplication(); }");
    }

    for (int i = 0; i < cntDynamicScripts; i++) {
      // add extra exception handling code otherwise exceptions occuring in
      // the Labs wait function are not visible.
      sb.append("\n} catch (_exception) {");
      sb.append("if (isc) { isc.Log.logError(_exception + ' ' + _exception.message + ' ' + _exception.stack); }");
      sb.append("if (console && console.trace) { console.trace();}");
      sb.append("}\n});");
    }

    // note compress, note that modules are cached in memory
    // when changing development status, system needs to be restarted.
    final String output;
    // in classicmode the isc combined is included, compressing that gives errors
    if (!isInDevelopment() && !isClassicMode()
        && !OBPropertiesProvider.getInstance().getBooleanProperty("test.environment")) {
      output = JSCompressor.getInstance().compress(sb.toString());
    } else {
      output = sb.toString();
    }
    final String md5 = DigestUtils.md5Hex(output);
    final String getTargetLocation = context.getRealPath(GEN_TARGET_LOCATION);
    final File dir = new File(getTargetLocation);
    if (!dir.exists()) {
      dir.mkdir();
    }
    File outFile = new File(getTargetLocation + "/" + md5 + ".js");
    if (!outFile.exists()) {
      try {
        log.debug("Writing file: " + outFile.getAbsolutePath());
        FileUtils.writeStringToFile(outFile, output, "UTF-8");
      } catch (Exception e) {
        log.error("Error writing file: " + e.getMessage(), e);
      }
    }
    return md5;
  }
}
