package sa.elm.ob.utility.ad_actionHandler.deleteMessageActionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Tab;

public class DeleteMessageActionHandlerImpl implements DeleteMessageActionHandlerDAO {
  private static final Logger log4j = Logger.getLogger(DeleteMessageActionHandlerImpl.class);

  @Override
  public String messageForTabLevelZero(Tab tab, String lang, String numberOfRecords) {
    String message = "";
    TreeSet<String> tabNames = new TreeSet<>();
    try {
      OBContext.setAdminMode();
      if (lang.equals("ar_SA")) {
        message = message.concat("<p align=\"right\"> ");
      }
      message = message.concat(OBMessageUtils.messageBD("EUT_DeleteMsg_ParentRecord") + "<br/>");
      message = message.replace("%", numberOfRecords.equals("1") ? "" : numberOfRecords + " ");
      if (lang.equals("ar_SA")) {
        message = message.concat(tab.getWindow().getName() + "  ⸮ <br/>");
      } else {
        message = message.concat(tab.getWindow().getName() + " ? <br/>");
      }
      tabNames = tabNames(tab.getId(), tab.getWindow().getId(), tab.getTabLevel(), lang);
      if (tabNames.size() > 0) {
        message = message.concat(OBMessageUtils.messageBD("EUT_DeleteMsg_ChildRecord") + " <br/>");
        int count = 1;
        for (String names : tabNames) {
          if (lang.equals("ar_SA")) {
            message = message.concat(names + "(" + count + "<br/>");
          } else {
            message = message.concat(count + ")" + names + "<br/>");
          }
          count = count + 1;
        }
        if (lang.equals("ar_SA"))
          message = message.concat("</p>");
      }
    } catch (Exception e) {
      log4j.error("Exception in tabLevelGreaterThanZero  : " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return message;
  }

  @Override
  public String messageForTabLevelGreaterThanZero(Tab tab, String lang, String numberOfRecords) {
    String message = "";
    TreeSet<String> minimumSeqNUmber = new TreeSet<>();
    Long minimumSequenceNUmber = null;
    try {
      OBContext.setAdminMode();
      if (lang.equals("ar_SA")) {
        message = message.concat("<p align=\"right\"> ");
      }
      message = message.concat(OBMessageUtils.messageBD("EUT_DeleteMsg_ParentRecord") + "<br/>");
      message = message.replace("%", numberOfRecords.equals("1") ? "" : numberOfRecords + " ");
      if (lang.equals("ar_SA")) {
        message = message.concat(tab.getADTabTrlList().get(0).getName() + " ⸮ <br/>");
      } else {
        message = message.concat(tab.getName() + " ? <br/>");
      }

      minimumSeqNUmber = tabNames(tab.getId(), tab.getWindow().getId(), tab.getTabLevel(), lang);
      if (minimumSeqNUmber.size() > 0) {
        minimumSequenceNUmber = Long.parseLong(minimumSeqNUmber.first());
      }

      OBQuery<Tab> tabObj = OBDal.getInstance().createQuery(Tab.class,
          " as e where e.window.id= :windowID and e.tabLevel> :tablevel and e.active = 'Y' and e.uIPattern <> 'RO'  "
              + (minimumSequenceNUmber != null ? "  and e.sequenceNumber< :seqNumber" : "") + " ");

      tabObj.setNamedParameter("windowID", tab.getWindow().getId());
      tabObj.setNamedParameter("tablevel", tab.getTabLevel());
      if (minimumSequenceNUmber != null) {
        tabObj.setNamedParameter("seqNumber", minimumSequenceNUmber);
      }

      List<Tab> tabLs = tabObj.list();
      if (tabLs.size() > 0) {
        message = message.concat(OBMessageUtils.messageBD("EUT_DeleteMsg_ChildRecord") + " <br/>");
        int count = 1;
        for (Tab t1 : tabLs) {
          if (lang.equals("ar_SA")) {
            message = message.concat(t1.getADTabTrlList().get(0).getName() + "(" + count + "<br/>");
          } else {
            message = message.concat(count + ")" + t1.getName() + "<br/>");
          }
          count = count + 1;
        }
        if (lang.equals("ar_SA"))
          message = message.concat("</p>");
      }
    } catch (Exception e) {
      log4j.error("Exception in tabLevelGreaterThanZero  : " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return message;
  }

  /**
   * This method is used to return tab list
   * 
   * @param tabId
   * @param windowId
   * @param tabLevel
   * @return
   */
  public TreeSet<String> tabNames(String tabId, String windowId, Long tabLevel, String lang) {
    List<Tab> tabList = new ArrayList<>();
    TreeSet<String> tabNames = new TreeSet<>();
    Long minimumSequenceNUmber = new Long(0);
    try {
      OBContext.setAdminMode();
      OBQuery<Tab> tabObj = OBDal.getInstance().createQuery(Tab.class,
          " as e where e.window.id= :windowID and e.id<> :tabID and e.tabLevel= :tablevel and e.active ='Y' and e.uIPattern <> 'RO' ");
      tabObj.setNamedParameter("windowID", windowId);
      tabObj.setNamedParameter("tabID", tabId);
      tabObj.setNamedParameter("tablevel", tabLevel == 0 ? new Long(1) : tabLevel);
      tabList = tabObj.list();
      if (tabList.size() > 0) {
        if (tabLevel == 0) {
          for (Tab t : tabList) {
            if (lang.equals("ar_SA")) {
              if (tabNames.isEmpty()) {
                tabNames.add(t.getADTabTrlList().get(0).getName());
              } else if (!tabNames.contains(t.getName().trim())) {
                tabNames.add(t.getADTabTrlList().get(0).getName().trim());
              }
            } else {
              if (tabNames.isEmpty()) {
                tabNames.add(t.getName());
              } else if (!tabNames.contains(t.getName().trim())) {
                tabNames.add(t.getName().trim());
              }
            }
          }
          return tabNames;
        } else {
          minimumSequenceNUmber = tabList.stream()
              .reduce((x, y) -> x.getSequenceNumber() < y.getSequenceNumber() ? x : y).get()
              .getSequenceNumber();
          tabNames.add(minimumSequenceNUmber.toString());
        }

      }
    } catch (Exception e) {
      log4j.error("Exception in tabLIst  : " + e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return tabNames;
  }

}
