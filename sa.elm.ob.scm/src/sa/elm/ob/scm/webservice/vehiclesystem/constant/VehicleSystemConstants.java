package sa.elm.ob.scm.webservice.vehiclesystem.constant;

/**
 * 
 * @author Gopal
 *
 */
public class VehicleSystemConstants {

  public static enum Status {
    IU("In Use"), RET("Returned"), SA("Sale"), LD("Lost and Damaged"), RW("Reward");

    private String value;

    Status(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static String getTextStatus(String name) {

      for (Status data : Status.values()) {
        if (data == Status.valueOf(name)) {
          return data.getValue();
        }
      }
      return "";
    }

  }

  public final static String SUCCESS = "SUCCESS";
  public final static String FAILED = "FAILED";
  public final static String SerialNumber = "serialnumber";
  public final static String PlateNumber = "platenumber";
  public final static String BodyNumber = "bodynumber";
  public final static String FactoryYear = "factoryyear";
  public final static String TradeMark = "trademark";
  public final static String CylinderNumber = "cylindernumber";
  public final static String FuelType = "fueltype";
  public final static String Color = "color";
  public final static String webServiceDescription = "Added by Web Service";
}