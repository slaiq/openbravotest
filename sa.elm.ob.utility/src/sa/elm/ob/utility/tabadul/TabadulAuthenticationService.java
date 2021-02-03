package sa.elm.ob.utility.tabadul;

public interface TabadulAuthenticationService {
  /**
   * Authenticate the user
   * 
   * @param userEmail
   * @param password
   * @return
   */
  String authenticate(String userEmail, String password);

  /**
   * Authenticate user using configured username and password
   * 
   * @return
   */
  String authenticate();

  /**
   * Get the session token for auditor user
   * 
   * @return
   */
  String authenticateAuditUser();

  /**
   * Check if current Session Token is valid or not
   * 
   * @return
   */
  Boolean isTokenValid(String sessionToken);
}
