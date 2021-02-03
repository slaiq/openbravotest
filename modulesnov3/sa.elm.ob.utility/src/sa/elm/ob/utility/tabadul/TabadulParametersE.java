package sa.elm.ob.utility.tabadul;
/**
 * 
 * @author mrahim
 *
 */
public enum TabadulParametersE {

  
  FAQ_DELIVERY_DATE_HIJRI ("faq_delivery_date_hijri"),
  TENDER_ID ("tid"),
  FAQ_DELIVERY_DATE ("faq_delivery_date"),
  OFFER_DELIVERY_DATE ("offer_delivery_date"),
  OFFER_DELIVERY_DATE_HIJRI ("offer_delivery_date_hijri"),
  OFFER_DELIVERY_TIME_HOUR ("offer_delivery_time_hour"),
  OFFER_DELIVERY_TIME_MINUTE ("offer_delivery_time_minute"),
  OPEN_ENV_DATE_HIJRI ("open_env_date_hijri"),
  OPEN_ENV_DATE ("open_env_date"),
  OPEN_ENV_TIME_HOUR ("open_env_time_hour"),
  OPEN_ENV_TIME_MINUTE ("open_env_time_minute"),
  EXTEND_APPROVAL_ID ("aid");
	
  private String parameterName;

  private TabadulParametersE(String parameterValue) {
    this.parameterName = parameterValue;
  }

public String getParameterName() {
	return parameterName;
}

public void setParameterName(String parameterName) {
	this.parameterName = parameterName;
}
 
}
