package sa.elm.ob.finance.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.service.db.DalConnectionProvider;

//import sa.elm.ob.finance.Efin_Project;

@SuppressWarnings("serial")
public class AlreadyDefaultCallout extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    String inpDefaultpro = info.getStringParameter("inpemEfinIsdefault", null);
    String inpDefaultuser1 = info.getStringParameter("inpemEfinIsdefault", null);
    String inpDefaultuser2 = info.getStringParameter("inpemEfinIsdefault", null);
    String inpDefaultfc = info.getStringParameter("inpemEfinIsdefault", null);
    String inpprojectId = info.getStringParameter("inpcProjectId", null);
    String inpUser1Id = info.getStringParameter("inpuser1Id", null);
    String inpUser2Id = info.getStringParameter("inpuser2Id", null);
    String inpFcId = info.getStringParameter("inpcActivityId", null);

    String ColumnId = info.getStringParameter("inpkeyColumnId", null);

    // if (inpDefault.equals("Y") && ColumnId.equals("C_SalesRegion_ID")) {
    // OBQuery<SalesRegion> obQuery = OBDal.getInstance().createQuery(SalesRegion.class,
    // "default='Y' and id!='" + inpSalesId + "'");
    // if (obQuery.list().size() > 0) {
    //
    // info.addResult("ERROR",
    // String.format(Utility.messageBD(conn, "EM_Efin_Dept_def_avail", language)));
    // info.addResult("inpisdefault", "N");
    //
    // }
    //
    // }
    // if (inpDefault.equals("Y") && inptransactiondep.equals("Y")) {
    // info.addResult("ERROR",
    // String.format(Utility.messageBD(conn, "EM_Efin_TranDep_def_avail", language)));
    // }

    if (inpDefaultpro.equals("Y") && ColumnId.equals("C_Project_ID")) {

      OBQuery<Project> obQuerypro = OBDal.getInstance().createQuery(Project.class,
          "eFINDefault='Y' and id!='" + inpprojectId + "'");
      if (obQuerypro.list().size() > 0) {

        info.addResult("ERROR",
            String.format(Utility.messageBD(conn, "EM_EFIN_Proj_def", language)));
        info.addResult("inpemEfinIsdefault", "N");

      }

    }
    if (inpDefaultuser1.equals("Y") && ColumnId.equals("User1_ID")) {

      OBQuery<UserDimension1> obQueryuser1 = OBDal.getInstance().createQuery(UserDimension1.class,
          "efinIsdefault='Y' and id!='" + inpUser1Id + "'");
      if (obQueryuser1.list().size() > 0) {

        info.addResult("ERROR",
            String.format(Utility.messageBD(conn, "EM_EFIN_User1_def", language)));
        info.addResult("inpemEfinIsdefault", "N");

      }

    }
    if (inpDefaultuser2.equals("Y") && ColumnId.equals("User2_ID")) {

      OBQuery<UserDimension2> obQueryuser2 = OBDal.getInstance().createQuery(UserDimension2.class,
          "efinIsdefault='Y' and id!='" + inpUser2Id + "'");
      if (obQueryuser2.list().size() > 0) {

        info.addResult("ERROR",
            String.format(Utility.messageBD(conn, "EM_EFIN_User2_def", language)));
        info.addResult("inpemEfinIsdefault", "N");

      }

    }
    if (inpDefaultfc.equals("Y") && ColumnId.equals("C_Activity_ID")) {

      OBQuery<ABCActivity> obQueryfc = OBDal.getInstance().createQuery(ABCActivity.class,
          "efinIsdefault='Y' and id!='" + inpFcId + "'");
      if (obQueryfc.list().size() > 0) {

        info.addResult("ERROR", String.format(Utility.messageBD(conn, "EM_EFIN_Fc_def", language)));
        info.addResult("inpemEfinIsdefault", "N");

      }

    }

  }
}
