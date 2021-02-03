package sa.elm.ob.scm.webservice.epm.controller;

import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import sa.elm.ob.scm.webservice.epm.service.EpmService;

/**
 * 
 * @author Kazim Epm Integration
 */

@RestController
@RequestMapping("openerp/epm")
public class EpmIntController {

  private static final Logger log4j = LoggerFactory.getLogger(EpmIntController.class);

  @Autowired
  EpmService epmService;

  /**
   * for testing webserivce in local env , not in stage, prod
   * 
   * @return
   * @throws Exception
   */
  @RequestMapping(value = "/project", method = RequestMethod.POST)
  public ResponseEntity<String> addProject() throws Exception {

    log4j.info("PKI -> addProject start");

    try {
      OBContext.setAdminMode();

      Order order = new Order();
      order.setDocumentNo("1234");
      epmService.addProject(order, 100, null);

    } catch (Exception exception) {
      log4j.error("PKI -> addProject Exception :", exception);
    } finally {
      OBContext.restorePreviousMode();
    }
    log4j.info("PKI -> addProject end");
    return new ResponseEntity<String>("OK", HttpStatus.OK);

  }
}