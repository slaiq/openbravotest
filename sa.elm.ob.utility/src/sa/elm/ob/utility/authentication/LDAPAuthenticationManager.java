package sa.elm.ob.utility.authentication;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.authentication.basic.DefaultAuthenticationManager;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

public class LDAPAuthenticationManager extends AuthenticationManager {

  private static final String DEFAULT_ADMIN_USER = "Openbravo";

  // values to be loaded from configuration file
  private static String adminUserName;
  private static String defaultRole;
  private static String defaultClient;

  private static String distinguishedName;
  private static String objectclass;
  private static String intialBase;
  private static int serverPort;
  private static String serverIP;
  private static String intialBindUser;
  private static String intialBindPassword;
  private static String nameAttribute;
  private static String ldapCertificatePath;
  private static List<String> adminList;
  

  private static final Logger log4j = Logger.getLogger(LDAPAuthenticationManager.class);

  private HttpServlet httpServlet;

  @Override
  protected String doAuthenticate(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException, ServletException, IOException {

    log4j.info("LDAPAuthenticationManager.doAuthenticate Started");

    final VariablesSecureApp vars = new VariablesSecureApp(request, false);

    String sUserId = (String) request.getSession().getAttribute("#Authenticated_user");

    if (!StringUtils.isEmpty(sUserId)) {
      log4j.info("User already Authenticated, return user id.");
      return sUserId;
    }

    String userName = vars.getStringParameter("user");
    String password = vars.getStringParameter("password");

    if (userName == null || StringUtils.isEmpty(userName)) {
      log4j.info(
          "User not authenticated and no user name is provided (no login iformation avilable), return null");
      return null;
    }

    if (isAdmin(userName)) {
      DefaultAuthenticationManager defaultAuthenticationManager = new DefaultAuthenticationManager(
          this.httpServlet);
      log4j.info("System admin try to login, default Authentication Manager will be used");
      defaultAuthenticationManager.init(this.httpServlet);
      return defaultAuthenticationManager.authenticate(request, response);
    }

    // Authenticate the user with LDAP and query basic information.
    AuthenticatedUser authenticatedUser = authenticateUser(userName, password);

    if (authenticatedUser == null) {
      log4j.info("Failed to Authenticate the user with LDAP.");
      OBError errorMsg = new OBError();
      errorMsg.setType("Error");
      errorMsg.setTitle("IDENTIFICATION_FAILURE_TITLE");
      errorMsg.setMessage("IDENTIFICATION_FAILURE_MSG");
      throw new AuthenticationException("IDENTIFICATION_FAILURE_TITLE", errorMsg);
    }

    // Query database for the user
    User user = getUser(userName);

    // User not exist
    if (user == null || user.getId() == null || StringUtils.isEmpty(user.getId())) {
      log4j.info("User not exist in the system.");
      OBError errorMsg = new OBError();
      errorMsg.setType("Error");
      errorMsg.setTitle("IDENTIFICATION_FAILURE_TITLE");
      errorMsg.setMessage("IDENTIFICATION_FAILURE_MSG");
      throw new AuthenticationException("IDENTIFICATION_FAILURE_TITLE", errorMsg);
    }

    if (!user.isActive()) {
      log4j.info("User not Active.");
      OBError errorMsg = new OBError();
      errorMsg.setType("Error");
      errorMsg.setTitle("IDENTIFICATION_FAILURE_TITLE");
      errorMsg.setMessage("IDENTIFICATION_FAILURE_MSG");
      throw new AuthenticationException("IDENTIFICATION_FAILURE_TITLE", errorMsg);
    }

    String userId = null;

    if (user != null) {
      userId = user.getId();
      final String sessionId = createDBSession(request, userName, userId);
      request.getSession(true).setAttribute("#Authenticated_user", userId);
      vars.setSessionValue("#AD_User_ID", userId);
      vars.setSessionValue("#AD_SESSION_ID", sessionId);
      vars.setSessionValue("#LogginIn", "Y");
    }

    return userId;
  }

  private boolean isAdmin(String userName) {
    if (adminList != null) {
      return adminList.contains(userName);
    } else {
      return false;
    }
  }

  private AuthenticatedUser authenticateUser(String userName, String password) {
	log4j.info("<--LDAP AUTHENTICATION START--->");
    AuthenticatedUser authenticatedUser = null;
    log4j.info("<--LDAP CERTIFICATE PATH--->" + ldapCertificatePath);
    // Set the trust store path in System property
    System.setProperty("javax.net.ssl.trustStore", ldapCertificatePath);
    LdapNetworkConnection connection = new LdapNetworkConnection(serverIP, serverPort,true);
    log4j.info("<--Connection Successful--->" + ldapCertificatePath);
    try {
      connection.bind(intialBindUser, intialBindPassword);

      log4j.info("Connected to LDAP successfully with intial binding credintials");

      String filter = String.format("(&(objectclass=%s)(CN=%s))", objectclass, userName);
      EntryCursor cursor = connection.search(intialBase, filter, SearchScope.SUBTREE, "*");
      if (cursor.next()) {
        log4j.info("Entry Found with specified filter.");
        Entry entry = cursor.get();
        log4j.info("Entry Details:" + entry);
        Attribute distinguishedNameAttribute = entry.get(distinguishedName);
        if (distinguishedNameAttribute != null) {
          log4j.info("distinguished Name:" + distinguishedNameAttribute);
          connection.bind(distinguishedNameAttribute.getString(), password);
          log4j.info("User [" + userName + "] binded successfully.\n");
          authenticatedUser = loadUserDetails(userName, entry);
        } else {
          // distinguished attribute not found
        }
      } else {
        // no entry found can't authonticate the user
      }
    } catch (LdapInvalidAttributeValueException e) {
      log4j.error("Can't load Entity attribute");
      e.printStackTrace();
    } catch (LdapException e) {
      log4j.error("Can't Bind the user.");
      e.printStackTrace();
    } catch (CursorException e) {
      log4j.error("Error when try to access search cursor.");
      e.printStackTrace();
    } finally {
      try {
        connection.close();
      } catch (IOException e) {
        log4j.error("Error when trying to close the connection.");
      }
    }

    // authenticatedUser = new AuthenticatedUser();
    // authenticatedUser.setUserName(userName);
    // authenticatedUser.setName("Sameir Mahrous");
    // return null;

    return authenticatedUser;
  }

  private AuthenticatedUser loadUserDetails(String userName, Entry entry)
      throws LdapInvalidAttributeValueException {
    AuthenticatedUser authenticatedUser = new AuthenticatedUser();
    authenticatedUser.setUserName(userName);
    authenticatedUser.setName(entry.get(nameAttribute).getString());
    return authenticatedUser;
  }

  @SuppressWarnings("unused")
  private User createDefaultUser(AuthenticatedUser authenticatedUser) {

    User newUser = OBProvider.getInstance().get(User.class);
    newUser.setName(authenticatedUser.getName());
    Client client = getClientByName(defaultClient);

    newUser.setClient(client);
    newUser.setActive(Boolean.TRUE);
    newUser.setUsername(authenticatedUser.getUserName());
    Role role = getRoleByName(defaultRole);
    newUser.setDefaultRole(role);
    UserRoles userRoles = new UserRoles();
    userRoles.setClient(client);
    userRoles.setOrganization(OBDal.getInstance().get(Organization.class, "0"));
    userRoles.setUserContact(newUser);
    userRoles.setRole(role);
    newUser.getADUserRolesList().add(userRoles);
    newUser.setCreatedBy(getUser("System"));
    newUser.setCreationDate(new Date());

    try {
      OBDal.getInstance().save(newUser);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      log4j.error("Error while creating a new user.");
      return null;
    }

    return newUser;
  }

  private Client getClientByName(String clientName) {
    Client client = null;
    Query query = SessionHandler.getInstance()
        .createQuery("from org.openbravo.model.ad.system.Client where name = ?");
    query.setParameter(0, clientName);
    client = (Client) query.uniqueResult();
    return client;
  }

  private Role getRoleByName(String roleName) {
    Role role = null;
    Query query = SessionHandler.getInstance()
        .createQuery("from org.openbravo.model.ad.access.Role where name = ?");
    query.setParameter(0, roleName);
    role = (Role) query.uniqueResult();
    return role;
  }

  /*
   * private Organization getOrganizationByName(String organizationName) {
   * 
   * Organization organization = null; Query query =
   * SessionHandler.getInstance().createQuery("from Organization where name = ?");
   * query.setParameter(0, organizationName); organization = (Organization) query.uniqueResult();
   * 
   * return organization; }
   */

  private User getUser(String userName) {
    User user = null;
    Query query = SessionHandler.getInstance()
        .createQuery("from org.openbravo.model.ad.access.User where username = ?");
    query.setParameter(0, userName);
    user = (User) query.uniqueResult();
    return user;
  }

  @Override
  protected void doLogout(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

  }

  /*
   * private void setTargetInfoInVariables(HttpServletRequest request, VariablesHistory variables) {
   * // redirects to the menu or the menu with the target String strTarget =
   * request.getRequestURL().toString(); String qString = request.getQueryString(); String
   * strDireccionLocal = HttpBaseUtils.getLocalAddress(request);
   * 
   * if (!strTarget.endsWith("/security/Menu.html")) { variables.setSessionValue("targetmenu",
   * strTarget); }
   * 
   * // Storing target string to redirect after a successful login
   * variables.setSessionValue("target", strDireccionLocal + "/security/Menu.html" + (qString !=
   * null && !qString.equals("") ? "?" + qString : "")); if (qString != null && !qString.equals(""))
   * { variables.setSessionValue("targetQueryString", qString); } }
   */

  public void init(HttpServlet httpServlet) throws AuthenticationException {
    super.init(httpServlet);
    this.httpServlet = httpServlet;

    loadConfiguration();
  }

  private void loadConfiguration() {

    Properties openbravoProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    adminUserName = openbravoProperties.getProperty("authentication.adminUserName",
        DEFAULT_ADMIN_USER);
    loadAdminList(adminUserName);
    defaultRole = openbravoProperties.getProperty("authentication.defaultRole");
    defaultClient = openbravoProperties.getProperty("authentication.defaultClient");

    distinguishedName = openbravoProperties.getProperty("authentication.ldap.distinguishedName");
    objectclass = openbravoProperties.getProperty("authentication.ldap.objectclass");
    nameAttribute = openbravoProperties.getProperty("authentication.ldap.nameAttribute");
    intialBase = openbravoProperties.getProperty("authentication.ldap.intialBase");
    serverPort = Integer
        .parseInt(openbravoProperties.getProperty("authentication.ldap.serverPort", "636"));
    serverIP = openbravoProperties.getProperty("authentication.ldap.serverIP");
    intialBindUser = openbravoProperties.getProperty("authentication.ldap.intialBindUser");
    intialBindPassword = openbravoProperties.getProperty("authentication.ldap.intialBindPassword");
    ldapCertificatePath = openbravoProperties.getProperty("authentication.ldap.certificate.path");

  }

  private void loadAdminList(String admins) {
    if (admins != null) {
      adminList = Arrays.asList(admins.split("\\s*,\\s*"));
    }
  }

public static String getLdapCertificatePath() {
	return ldapCertificatePath;
}

public static void setLdapCertificatePath(String ldapCertificatePath) {
	LDAPAuthenticationManager.ldapCertificatePath = ldapCertificatePath;
}

}
