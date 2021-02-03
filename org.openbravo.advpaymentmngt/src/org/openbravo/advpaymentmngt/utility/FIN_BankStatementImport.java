/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.utility;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.BankFileException;
import org.openbravo.model.financialmgmt.payment.BankFileFormat;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.service.db.CallStoredProcedure;

public abstract class FIN_BankStatementImport {
  private FIN_FinancialAccount financialAccount;
  OBError myError = null;
  String filename = "";
  private static Logger log4j = Logger.getLogger(FIN_BankStatementImport.class);

  public static final String DOCUMENT_BankStatementFile = "BSF";

  ArrayList<String> stringExceptions = null;

  public FIN_BankStatementImport(FIN_FinancialAccount _financialAccount) {
    setFinancialAccount(_financialAccount);
  }

  public FIN_BankStatementImport() {
  }

  /**
   * @return the myError
   */
  public OBError getMyError() {
    return myError;
  }

  /**
   * @param error
   *          the myError to set
   */
  public void setMyError(OBError error) {
    this.myError = error;
  }

  public void init(FIN_FinancialAccount _financialAccount) {
    setFinancialAccount(_financialAccount);
  }

  private void setFinancialAccount(FIN_FinancialAccount _financialAccount) {
    financialAccount = _financialAccount;
  }

  private InputStream getFile(VariablesSecureApp vars) throws IOException {
    FileItem fi = vars.getMultiFile("inpFile");
    if (fi == null)
      throw new IOException("Invalid filename");
    filename = fi.getName();
    InputStream in = fi.getInputStream();
    if (in == null)
      throw new IOException("Corrupted file");
    return in;
  }

  private FIN_BankStatement createFINBankStatement(ConnectionProvider conn, VariablesSecureApp vars)
      throws Exception {
    final FIN_BankStatement newBankStatement = OBProvider.getInstance()
        .get(FIN_BankStatement.class);
    newBankStatement.setAccount(financialAccount);
    DocumentType doc = null;
    try {
      doc = getDocumentType();
    } catch (Exception e) {
      throw new Exception(e);
    }
    String documentNo = getDocumentNo(conn, vars, doc);
    newBankStatement.setDocumentType(doc);
    newBankStatement.setDocumentNo(documentNo);
    newBankStatement.setOrganization(financialAccount.getOrganization());
    String name = documentNo + " - " + filename;
    if (name.length() > 60) {
      name = name.substring(0, 60);
    }
    newBankStatement.setName(name);
    newBankStatement.setImportdate(new Date());
    newBankStatement.setTransactionDate(new Date());
    newBankStatement.setFileName(filename);
    OBDal.getInstance().save(newBankStatement);
    OBDal.getInstance().flush();
    return newBankStatement;
  }

  public OBError importFile(ConnectionProvider conn, VariablesSecureApp vars) {
    InputStream file = null;
    FIN_BankStatement bankStatement;
    List<FIN_BankStatementLine> bankStatementLines = new ArrayList<FIN_BankStatementLine>();
    int previousNumberofLines = 0;
    int numberOfLines = 0;
    BankFileFormat bff = getBankFileFormat();
    stringExceptions = loadExceptions(bff);
    try {
      file = getFile(vars);
    } catch (IOException e) {
      return getOBError(conn, vars, "@WrongFile@", "Error", "Error");
    }

    try {
      bankStatement = createFINBankStatement(conn, vars);
    } catch (Exception ex) {
      return getOBError(conn, vars, "@APRM_DocumentTypeNotFound@", "Error", "Error");
    }

    try {
      bankStatementLines = loadFile(file, bankStatement);
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      return getOBError(conn, vars, e.getMessage(), "Error", "Error");
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      return getOBError(conn, vars, "@APRM_InvalidOrMissingValues@", "Error", "Error");
    }
    if (bankStatementLines == null || bankStatementLines.size() == 0) {
      OBDal.getInstance().rollbackAndClose();
      return getMyError();
    }

    previousNumberofLines = bankStatementLines.size();

    try {
      numberOfLines = saveFINBankStatementLines(bankStatementLines);
      OBDal.getInstance().refresh(bankStatement);
      OBError processResult = FIN_AddPayment.processBankStatement(vars, conn, "P",
          bankStatement.getId());
      setMyError(processResult);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Error importing file.", e);
      return getMyError();
    }
    if (getMyError() != null && !getMyError().getType().toLowerCase().equals("success")
        && !getMyError().getType().toLowerCase().equals("warning")) {
      OBDal.getInstance().rollbackAndClose();
      return getMyError();
    } else if (getMyError() != null && getMyError().getType().toLowerCase().equals("success")) {
      if (numberOfLines < previousNumberofLines) {
        OBError msg = new OBError();
        msg.setType("Success");
        msg.setTitle(Utility.messageBD(conn, "Success", vars.getLanguage()));
        String message = String.format(
            Utility.messageBD(conn, "APRM_ZeroAmountNotInserted", vars.getLanguage()),
            String.valueOf(numberOfLines), String.valueOf(previousNumberofLines - numberOfLines));
        msg.setMessage(message);
        setMyError(msg);
      }
      return getOBError(conn, vars, "@APRM_BankStatementNo@ " + bankStatement.getDocumentNo()
          + "<br/>" + numberOfLines + " " + "@RowsInserted@", "Success", "Success");
    } else if (getMyError() != null && getMyError().getType().toLowerCase().equals("warning")) {
      return getMyError();
    } else {
      return getOBError(conn, vars, "@APRM_BankStatementNo@ " + bankStatement.getDocumentNo()
          + "<br/>" + numberOfLines + " " + "@RowsInserted@", "Success", "Success");
    }
  }

  OBError getOBError(ConnectionProvider conn, VariablesSecureApp vars, String strMessage,
      String strMsgType, String strTittle) {
    OBError message = new OBError();
    message.setType(strMsgType);
    message.setTitle(Utility.messageBD(conn, strTittle, vars.getLanguage()));
    message.setMessage(Utility.parseTranslation(conn, vars, vars.getLanguage(), strMessage));
    return message;
  }

  private int saveFINBankStatementLines(List<FIN_BankStatementLine> bankStatementLines) {
    int counter = 0;
    BigDecimal crAmount;
    BigDecimal drAmount;
    for (FIN_BankStatementLine bankStatementLine : bankStatementLines) {
      BusinessPartner businessPartner = null;
      GLItem glItem = null;
      crAmount = bankStatementLine.getCramount();
      drAmount = bankStatementLine.getDramount();
      if (!(crAmount.compareTo(BigDecimal.ZERO) == 0)
          || !(drAmount.compareTo(BigDecimal.ZERO) == 0)) {
        // Try finding Previous matches
        HashMap<String, String> previous = matchPreviousBSL(bankStatementLine.getBpartnername(),
            bankStatementLine.getOrganization(), bankStatementLine.getBankStatement().getAccount());
        if ((previous != null) && (!"".equals(previous.get("BPartnerID")))) {
          businessPartner = OBDal.getInstance().get(BusinessPartner.class,
              previous.get("BPartnerID"));
        }
        if ((previous != null) && (!"".equals(previous.get("GLItemID")))) {
          glItem = OBDal.getInstance().get(GLItem.class, previous.get("GLItemID"));
        }
        // if no previous BSL is found, try match BP Name
        if (businessPartner == null) {
          try {
            businessPartner = matchBusinessPartner(bankStatementLine.getBpartnername(),
                bankStatementLine.getOrganization(), bankStatementLine.getBankStatement()
                    .getAccount());
          } catch (Exception e) {
            businessPartner = null;
          }
        }
        bankStatementLine.setBusinessPartner(businessPartner);
        bankStatementLine.setGLItem(glItem);
        bankStatementLine.setLineNo(new Long((counter + 1) * 10));
        OBDal.getInstance().save(bankStatementLine);
        counter++;
      } else {
        OBDal.getInstance().remove(bankStatementLine);
      }

    }
    OBDal.getInstance().flush();
    return counter;
  }

  private DocumentType getDocumentType() throws Exception {
    final List<Object> parameters = new ArrayList<Object>();
    parameters.add(financialAccount.getClient());
    parameters.add(financialAccount.getOrganization());
    parameters.add(DOCUMENT_BankStatementFile);
    String strDocTypeId = (String) CallStoredProcedure.getInstance().call("AD_GET_DOCTYPE",
        parameters, null);
    if (strDocTypeId == null) {
      throw new Exception("The Document Type is missing for the Bank Statement");
    }
    return new AdvPaymentMngtDao().getObject(DocumentType.class, strDocTypeId);
  }

  private String getDocumentNo(ConnectionProvider conn, VariablesSecureApp vars,
      DocumentType documentType) {
    return Utility.getDocumentNo(conn, vars, "AddPaymentFromInvoice", "FIN_Payment",
        documentType.getId(), documentType.getId(), false, true);

  }

  private BusinessPartner matchBusinessPartner(String partnername, Organization organization,
      FIN_FinancialAccount account) {
    // TODO extend with other matching methods. It will make it easier to later reconcile
    BusinessPartner bp = finBPByName(partnername, organization);
    if (bp == null) {
      bp = matchBusinessPartnerByNameTokens(partnername, organization);
    }
    return bp;
  }

  private HashMap<String, String> matchPreviousBSL(String partnername, Organization organization,
      FIN_FinancialAccount account) {
    HashMap<String, String> result = new HashMap<String, String>();
    result.put("BPartnerID", "");
    result.put("GLItemID", "");
    if (partnername == null || "".equals(partnername.trim())) {
      return null;
    }
    final StringBuilder whereClause = new StringBuilder();
    List<Object> parameters = new ArrayList<Object>();
    OBContext.setAdminMode();
    try {
      whereClause.append(" as bsl ");
      whereClause
          .append(" where translate(replace(bsl."
              + FIN_BankStatementLine.PROPERTY_BPARTNERNAME
              + ",' ', ''),'0123456789', '          ') = translate( replace(?,' ',''),'0123456789', '          ')");
      parameters.add(partnername.replaceAll("\\r\\n|\\r|\\n", " "));
      whereClause.append(" and (bsl." + FIN_BankStatementLine.PROPERTY_BUSINESSPARTNER
          + " is not null or bsl." + FIN_BankStatementLine.PROPERTY_GLITEM + " is not null)");
      whereClause.append(" and bsl." + FIN_BankStatementLine.PROPERTY_BANKSTATEMENT + ".");
      whereClause.append(FIN_BankStatement.PROPERTY_ACCOUNT + ".id = ?");
      parameters.add(account.getId());
      whereClause.append(" and bsl." + FIN_BankStatementLine.PROPERTY_ORGANIZATION + ".id in (");
      whereClause.append(Utility.getInStrSet(new OrganizationStructureProvider()
          .getNaturalTree(organization.getId())) + ") ");
      whereClause.append(" and bsl.bankStatement.processed = 'Y'");
      whereClause.append(" order by bsl." + FIN_BankStatementLine.PROPERTY_CREATIONDATE + " desc");
      final OBQuery<FIN_BankStatementLine> bsl = OBDal.getInstance().createQuery(
          FIN_BankStatementLine.class, whereClause.toString(), parameters);
      bsl.setFilterOnReadableOrganization(false);
      // Just look in 10 matches
      bsl.setMaxResult(10);
      for (FIN_BankStatementLine line : bsl.list()) {
        if (line.getGLItem() != null && "".equals(result.get("GLItemID"))) {
          result.put("GLItemID", line.getGLItem().getId());
        }
        if (line.getBusinessPartner() != null && "".equals(result.get("BPartnerID"))) {
          result.put("BPartnerID", line.getBusinessPartner().getId());
        }
        if (!"".equals(result.get("BPartnerID")) && !"".equals(result.get("GLItemID"))) {
          return result;
        }
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private BusinessPartner finBPByName(String partnername, Organization organization) {
    if (partnername == null || "".equals(partnername.trim())) {
      return null;
    }
    final StringBuilder whereClause = new StringBuilder();
    List<Object> parameters = new ArrayList<Object>();

    OBContext.setAdminMode();
    try {
      whereClause.append(" as bp ");
      whereClause.append(" where bp." + BusinessPartner.PROPERTY_NAME + " = ?");
      parameters.add(partnername);
      whereClause.append(" and bp." + BusinessPartner.PROPERTY_ORGANIZATION + ".id in (");
      whereClause.append(Utility.getInStrSet(new OrganizationStructureProvider()
          .getNaturalTree(organization.getId())) + ") ");
      final OBQuery<BusinessPartner> bp = OBDal.getInstance().createQuery(BusinessPartner.class,
          whereClause.toString(), parameters);
      bp.setFilterOnReadableOrganization(false);
      bp.setMaxResult(1);
      List<BusinessPartner> matchedBP = bp.list();
      if (matchedBP.size() == 0)
        return null;
      else
        return matchedBP.get(0);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public abstract List<FIN_BankStatementLine> loadFile(InputStream in,
      FIN_BankStatement targetBankStatement);

  private BusinessPartner matchBusinessPartnerByNameTokens(String partnername,
      Organization organization) {
    if (partnername == null || "".equals(partnername.trim())) {
      return null;
    }
    String parsedPartnername = partnername.toLowerCase();
    // Remove exceptions
    for (String eliminate : stringExceptions) {
      parsedPartnername = parsedPartnername.replaceAll(eliminate.toLowerCase(), "");
    }
    StringTokenizer st = new StringTokenizer(parsedPartnername);
    List<String> list = new ArrayList<String>();
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      if (token.length() > 3) {
        list.add(token);
      }
    }
    if (list.isEmpty()) {
      return null;
    }
    final StringBuilder whereClause = new StringBuilder();
    OBContext.setAdminMode();
    ScrollableResults businessPartnersScroll = null;
    try {
      whereClause.append("select b.id as id, b.name as name from ");
      whereClause.append(" BusinessPartner b ");
      whereClause.append(" where (");
      for (String token : list) {
        whereClause.append(" lower(b." + BusinessPartner.PROPERTY_NAME + ") like lower('%" + token
            + "%') or ");
      }
      whereClause.delete(whereClause.length() - 3, whereClause.length()).append(")");
      whereClause.append(" and b." + BusinessPartner.PROPERTY_ORGANIZATION + ".id in (");
      whereClause.append(Utility.getInStrSet(new OrganizationStructureProvider()
          .getNaturalTree(organization.getId())) + ") ");
      final Query bl = OBDal.getInstance().getSession().createQuery(whereClause.toString());
      businessPartnersScroll = bl.scroll(ScrollMode.SCROLL_SENSITIVE);

      if (!businessPartnersScroll.next()) {
        return null;
      }

      else {
        final Object[] resultObject = (Object[]) businessPartnersScroll.get(0);
        if (!businessPartnersScroll.next()) {
          String strParnterId = "";
          if (resultObject.getClass().isArray()) {
            final Object[] values = resultObject;
            strParnterId = (String) values[0];
          }
          BusinessPartner bp = OBDal.getInstance().get(BusinessPartner.class, strParnterId);
          return bp;
        }

        else {
          String closestId = closest(businessPartnersScroll, partnername);
          BusinessPartner closest = OBDal.getInstance().get(BusinessPartner.class, closestId);
          return closest;
        }
      }

    } finally {
      businessPartnersScroll.close();
      OBContext.restorePreviousMode();
    }
  }

  private String closest(ScrollableResults businessPartners, String partnername) {
    String targetBusinessPartnerId = "";
    try {
      businessPartners.beforeFirst();
      businessPartners.next();
      Object[] resultObject = (Object[]) businessPartners.get(0);

      String targetBusinessPartnerName = "";
      if (resultObject.getClass().isArray()) {
        final Object[] values = resultObject;
        targetBusinessPartnerId = (String) values[0];
        targetBusinessPartnerName = (String) values[1];
      }

      int distance = StringUtils.getLevenshteinDistance(partnername, targetBusinessPartnerName);
      String parsedPartnername = partnername.toLowerCase();
      // Remove exceptions
      for (String eliminate : stringExceptions) {
        parsedPartnername = parsedPartnername.replaceAll(eliminate.toLowerCase(), "");
      }

      // Remove Numeric characters
      char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
      for (char character : digits) {
        parsedPartnername = parsedPartnername.replace(character, ' ');
        parsedPartnername = parsedPartnername.trim();
      }

      businessPartners.beforeFirst();
      int i = 0;
      while (businessPartners.next()) {
        i++;
        String bpId = "";
        String bpName = "";
        resultObject = (Object[]) businessPartners.get(0);
        if (resultObject.getClass().isArray()) {
          final Object[] values = resultObject;
          bpId = (String) values[0];
          bpName = (String) values[1];
        }
        // Calculates distance between two strings meaning number of changes required for a string
        // to
        // convert in another string
        int bpDistance = StringUtils
            .getLevenshteinDistance(parsedPartnername, bpName.toLowerCase());
        if (bpDistance < distance) {
          distance = bpDistance;
          targetBusinessPartnerId = bpId;
        }
        if (i % 100 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
      return targetBusinessPartnerId;
    } catch (Exception e) {
      log4j.error(e.getStackTrace());
    } finally {
      return targetBusinessPartnerId;
    }

  }

  private BankFileFormat getBankFileFormat() {
    List<BankFileFormat> bankFileFormat = new ArrayList<BankFileFormat>();
    OBContext.setAdminMode();
    final String JAVACLASSNAME = this.getClass().getName();
    try {
      OBCriteria<BankFileFormat> obc = OBDal.getInstance().createCriteria(BankFileFormat.class);
      obc.add(Restrictions.eq(BankFileFormat.PROPERTY_JAVACLASSNAME, JAVACLASSNAME));
      obc.setMaxResults(1);
      bankFileFormat = obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
    return bankFileFormat.size() > 0 ? bankFileFormat.get(0) : null;
  }

  ArrayList<String> loadExceptions(BankFileFormat bankFileFormat) {
    ArrayList<String> exceptions = new ArrayList<String>();
    List<BankFileException> bankFileExceptions = new ArrayList<BankFileException>();
    OBContext.setAdminMode();
    try {
      OBCriteria<BankFileException> obc = OBDal.getInstance().createCriteria(
          BankFileException.class);
      obc.createAlias(BankFileException.PROPERTY_BANKFILEFORMAT, "BFF");
      obc.add(Restrictions.eq("BFF." + BankFileFormat.PROPERTY_JAVACLASSNAME,
          bankFileFormat.getJavaClassName()));
      obc.add(Restrictions.or(
          Restrictions.eq(BankFileException.PROPERTY_FINANCIALACCOUNT, financialAccount),
          Restrictions.isNull(BankFileException.PROPERTY_FINANCIALACCOUNT)));
      bankFileExceptions = obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
    for (BankFileException ex : bankFileExceptions) {
      exceptions.add(ex.getTextToExclude());
    }
    return exceptions;
  }
}
