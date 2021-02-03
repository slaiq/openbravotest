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
 * All portions are Copyright (C) 2011-2015 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.window;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.SecurityChecker;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.TabAttachments;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.utils.FileUtility;

import sa.elm.ob.utility.util.AttachmentProcessDao;
import sa.elm.ob.utility.util.Constants;

public class AttachmentsAH extends BaseActionHandler {

  private static final Logger log = Logger.getLogger(AttachmentsAH.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode();
    try {
      if (parameters.get("Command").equals("DELETE")) {
        String tabId = parameters.get("tabId").toString();
        String recordIds = parameters.get("recordIds").toString();
        String attachmentId = (String) parameters.get("attachId");
        Tab tab = OBDal.getInstance().get(Tab.class, tabId);

        String tableId = (String) DalUtil.getId(tab.getTable());
        String userId = OBContext.getOBContext().getUser().getId();
        String roleId = OBContext.getOBContext().getRole().getId();
        JSONObject obj = new JSONObject();
        boolean deleteFile = true;
        boolean deleteAccess = false;
        String preferenceValue = "N";
        String window = null;
        String clientId = null;
        String appStatus = null;
        JSONObject recordJson = AttachmentProcessDao.getRecordDetails(tabId, recordIds);
        if (recordJson != null) {
          if (recordJson.has("window"))
            window = recordJson.getString("window");
          if (recordJson.has("clientId"))
            clientId = recordJson.getString("clientId");
          if (recordJson.has("appStatus"))
            appStatus = recordJson.getString("appStatus");
        }

        if ((tabId.equals(Constants.PROPOSAL_MANAGEMENT_TAB)
            || tabId.equals(Constants.PURCHASE_ORDER_TAB)
            || tabId.equals(Constants.PURCHASE_REQUISITION_TAB)) && window != null
            && clientId != null) {
          List<Attachment> fileList = new ArrayList<Attachment>();
          try {
            List<Preference> prefs = AttachmentProcessDao.getPreferences("EUT_Attachment_Process",
                true, clientId, null, null, null, window, false, true, true);
            for (Preference preference : prefs) {
              if (preference.getSearchKey() != null && preference.getSearchKey().equals("Y")) {
                preferenceValue = "Y";
              }
            }

          } catch (PropertyException e) {
            preferenceValue = "N";
          } catch (Exception e) {
            // TODO Auto-generated catch block
          }
          if (preferenceValue != null && preferenceValue.equals("Y")) {
            OBQuery<Attachment> file = OBDal.getInstance().createQuery(Attachment.class,
                " as e where e.record=:recordId");
            file.setNamedParameter("recordId", recordIds);
            fileList = file.list();
            if (appStatus != null && fileList.size() > 0) {
              if ((appStatus.equals("INP") || appStatus.equals("ESCM_IP"))
                  && attachmentId != null) {
                deleteFile = AttachmentProcessDao.chkPresentRoleIsWaitingApp(tabId, recordIds,
                    roleId, attachmentId, userId);

              } else if ((appStatus.equals("INP") || appStatus.equals("ESCM_IP"))
                  && attachmentId == null) {
                deleteFile = AttachmentProcessDao.chkPresentRoleIsWaitingAppForRemoveAll(tabId,
                    recordIds, roleId, userId, fileList);

              } else if ((appStatus.equals("APP") || appStatus.equals("ESCM_AP"))) {
                deleteFile = false;
              } else if ((appStatus.equals("REA") || appStatus.equals("ESCM_RA")
                  || appStatus.equals("REJ") || appStatus.equals("ESCM_REJ"))
                  && attachmentId == null) {
                deleteFile = AttachmentProcessDao
                    .chkAttachmentFromOldVersionUsingRemoveAll(fileList);
              } else if ((appStatus.equals("REA") || appStatus.equals("ESCM_RA")
                  || appStatus.equals("REJ") || appStatus.equals("ESCM_REJ"))
                  && attachmentId != null) {
                deleteFile = AttachmentProcessDao.chkAttachmentFromOldVersion(attachmentId);

              }
            }

          }
        }
        if (deleteFile) {
          // Checks if the user has readable access to the record where the file is attached
          Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
          if (entity != null) {
            Object object = OBDal.getInstance().get(entity.getMappingClass(), recordIds);
            if (object instanceof OrganizationEnabled) {
              SecurityChecker.getInstance().checkReadableAccess((OrganizationEnabled) object);
            }
          }

          OBCriteria<Attachment> attachmentFiles = OBDao.getFilteredCriteria(Attachment.class,
              Restrictions.eq("table.id", tableId),
              Restrictions.in("record", recordIds.split(",")));
          // do not filter by the attachment's organization
          // if the user has access to the record where the file its attached, it has access to all
          // its attachments
          attachmentFiles.setFilterOnReadableOrganization(false);
          if (attachmentId != null) {
            attachmentFiles.add(Restrictions.eq(Attachment.PROPERTY_ID, attachmentId));
          }
          for (Attachment attachment : attachmentFiles.list()) {
            deleteFile(attachment);
          }
        }
        obj = getAttachmentJSONObject(tab, recordIds);
        obj.put("buttonId", parameters.get("buttonId"));
        obj.put("deleteFile", deleteFile);
        return obj;

      } else {
        return new JSONObject();
      }
    } catch (JSONException e) {
      throw new OBException("Error while removing file", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void deleteFile(Attachment attachment) {
    String attachmentFolder = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("attach.path");
    String fileDir = TabAttachments.getAttachmentDirectory(attachment.getTable().getId(),
        attachment.getRecord(), attachment.getName());
    String fileDirPath = attachmentFolder + "/" + fileDir;
    FileUtility f = new FileUtility();
    final File file = new File(fileDirPath, attachment.getName());
    if (file.exists()) {
      try {
        f = new FileUtility(fileDirPath, attachment.getName(), false);
        f.deleteFile();
      } catch (Exception e) {
        throw new OBException("//Error while removing file", e);
      }

    } else {
      log.warn("No file was removed as file could not be found");
    }

    OBDal.getInstance().remove(attachment);
    OBDal.getInstance().flush();

  }

  public static JSONObject getAttachmentJSONObject(Tab tab, String recordIds) {
    String tableId = (String) DalUtil.getId(tab.getTable());
    OBCriteria<Attachment> attachmentFiles = OBDao.getFilteredCriteria(Attachment.class,
        Restrictions.eq("table.id", tableId), Restrictions.in("record", recordIds.split(",")));
    attachmentFiles.addOrderBy("creationDate", false);
    List<JSONObject> attachments = new ArrayList<JSONObject>();
    // do not filter by the attachment's organization
    // if the user has access to the record where the file its attached, it has access to all its
    // attachments
    attachmentFiles.setFilterOnReadableOrganization(false);
    for (Attachment attachment : attachmentFiles.list()) {
      JSONObject attachmentobj = new JSONObject();
      try {
        attachmentobj.put("id", attachment.getId());
        attachmentobj.put("name", attachment.getName());
        attachmentobj.put("age", (new Date().getTime() - attachment.getUpdated().getTime()));
        attachmentobj.put("updatedby", attachment.getUpdatedBy().getName());
        attachmentobj.put("description", attachment.getText());
        attachmentobj.put("attachmentPath", attachment.getEutDmsAttachpath());
      } catch (Exception e) {
        throw new OBException("Error while reading attachments:", e);
      }
      attachments.add(attachmentobj);
    }
    JSONObject jsonobj = new JSONObject();
    try {
      jsonobj.put("attachments", new JSONArray(attachments));
    } catch (JSONException e) {
      throw new OBException(e);
    }
    return jsonobj;

  }
}
