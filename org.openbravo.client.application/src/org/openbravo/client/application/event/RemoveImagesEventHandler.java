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
 * All portions are Copyright (C) 2014-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.event;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.plm.Product;

public class RemoveImagesEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = getImageEntities();
  private static final String DUMMY_IMAGE_ID = "2FA7212E426F11E5A151FEFF819CDC9F";
  private Image dummyImage = null;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    // Iterate image properties of the entity
    for (String property : getImageProperties(event.getTargetInstance().getEntity())) {

      Property imageProperty = event.getTargetInstance().getEntity().getProperty(property);

      // Remove image if it exists
      if (event.getCurrentState(imageProperty) != null) {

        Image bob = (Image) event.getCurrentState(imageProperty);
        if (imageProperty.isMandatory()) {
          // If the property is mandatory replace the current image with a dummy one to prevent
          // breaking the not null constraint
          // See issue https://issues.openbravo.com/view.php?id=30571
          event.setCurrentState(imageProperty, getDummyImage());
        }
        if (bob != null) {
          String selectedProduct = event.getId();
          if (!checkImageUtilization(selectedProduct, bob)) {
            OBContext.setAdminMode(true);
            try {
              OBDal.getInstance().remove(bob);
            } finally {
              OBContext.restorePreviousMode();
            }
          }
        }
      }
    }
  }

  /**
   * Returns a dummy image (AD_Image instance) that will be named DUMMY_IMAGE_NAME and will not have
   * binary data
   * 
   * @return a dummy image
   */
  private Image getDummyImage() {
    // If not chached yet, obtain it, otherwise just return the cached one
    if (dummyImage == null) {
      OBCriteria<Image> dummyImageCriteria = OBDal.getInstance().createCriteria(Image.class);
      dummyImageCriteria.add(Restrictions.eq(Image.PROPERTY_ID, DUMMY_IMAGE_ID));
      dummyImage = (Image) dummyImageCriteria.uniqueResult();
      // If it is not already created, do it
      if (dummyImage == null) {
        dummyImage = createDummyImage();
      }
    }
    return dummyImage;
  }

  /**
   * Creates a dummy image, that will be called DUMMY_IMAGE_NAME and will not have binary data
   * 
   * @return the dummy image
   */
  private Image createDummyImage() {
    Image dummy = OBProvider.getInstance().get(Image.class);
    dummy.setId(DUMMY_IMAGE_ID);
    dummy.setName("DummyImageForDeletedRows");
    dummy.setNewOBObject(true);
    OBDal.getInstance().save(dummy);
    return dummy;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    // Iterate image properties of the entity
    for (String property : getImageProperties(event.getTargetInstance().getEntity())) {

      Property imageProperty = event.getTargetInstance().getEntity().getProperty(property);

      // If the old image is different than the new one remove the old image if exists
      if (event.getPreviousState(imageProperty) != null
          && event.getCurrentState(imageProperty) != event.getPreviousState(imageProperty)) {

        Image bob = (Image) event.getPreviousState(imageProperty);
        if (bob != null) {
          String selectedProduct = event.getId();
          if (!checkImageUtilization(selectedProduct, bob)) {
            OBContext.setAdminMode(true);
            try {
              OBDal.getInstance().remove(bob);
            } finally {
              OBContext.restorePreviousMode();
            }
          }
        }
      }
    }
  }

  private static Entity[] getImageEntities() {
    ArrayList<Entity> entityArray = new ArrayList<Entity>();

    // Create the observed entities from ModelProvider
    for (Entity entity : ModelProvider.getInstance().getEntityWithImage().keySet()) {
      entityArray.add(entity);
    }
    return entityArray.toArray(new Entity[entityArray.size()]);
  }

  // Check if this image is used by another product
  private static boolean checkImageUtilization(String productId, Image bob) {
    final OBCriteria<Product> obCriteria = OBDal.getInstance().createCriteria(Product.class);
    obCriteria.add(Restrictions.eq(Product.PROPERTY_IMAGE, bob));
    obCriteria.add(Restrictions.ne(Product.PROPERTY_ID, productId));
    obCriteria.setFilterOnActive(false);
    obCriteria.setFilterOnReadableClients(false);
    obCriteria.setFilterOnReadableOrganization(false);
    obCriteria.setMaxResults(1);
    Product product = (Product) obCriteria.uniqueResult();

    if (product != null) {
      return true;
    }
    return false;
  }

  private static List<String> getImageProperties(Entity entity) {
    // Get EntitiesWithImages from ModelProvider
    return ModelProvider.getInstance().getEntityWithImage().get(entity);
  }
}
