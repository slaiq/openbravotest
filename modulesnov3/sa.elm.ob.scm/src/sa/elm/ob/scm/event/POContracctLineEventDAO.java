package sa.elm.ob.scm.event;

import java.math.BigDecimal;

import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.procurement.RequisitionLine;

/**
 * 
 * @author Gokul 13/05/2019
 *
 */
public class POContracctLineEventDAO {

  /**
   * Update the parent PO Qty as 0
   * 
   * @param line
   */
  public static void updatePoQtyForParent(RequisitionLine line) {
    Boolean isPoQty = true;
    if (line.getEscmParentlineno() != null) {
      String parentId = line.getEscmParentlineno().getId();

      RequisitionLine parentLine = OBDal.getInstance().get(RequisitionLine.class, parentId);
      // line.getEscmParentlineno()
      OBQuery<RequisitionLine> reqlines = OBDal.getInstance().createQuery(RequisitionLine.class,
          "as e where e.escmParentlineno.id=:parentLine");
      reqlines.setNamedParameter("parentLine", parentId);

      if (reqlines != null && reqlines.list().size() > 0) {
        for (RequisitionLine reqlineobj : reqlines.list()) {
          if (reqlineobj.getEscmPoQty().compareTo(BigDecimal.ZERO) > 0) {
            isPoQty = false;
          }
        }
      }

      if (isPoQty) {
        parentLine.setEscmPoQty(BigDecimal.ZERO);
      }
      if (parentLine.getEscmParentlineno() != null) {
        updatePoQtyForParent(parentLine);
      }
    }
  }
}
