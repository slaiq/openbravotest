package sa.elm.ob.utility.ad_actionHandler.deleteMessageActionHandler;

import org.openbravo.model.ad.ui.Tab;

public interface DeleteMessageActionHandlerDAO {
  /**
   * This method is used to get message for tab level zero
   * 
   * @param tab
   * @param lang
   * @param numberOfRecords
   * @return
   */
  public String messageForTabLevelZero(Tab tab, String lang, String numberOfRecords);

  /**
   * This method is used to get message for tab level greater than zero
   * 
   * @param tab
   * @param lang
   * @param numberOfRecords
   * @return
   */
  public String messageForTabLevelGreaterThanZero(Tab tab, String lang, String numberOfRecords);
}
