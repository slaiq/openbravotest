/*
 ******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2013 Openbravo S.L.U.
 ******************************************************************************
 */
package sa.elm.ob.finance.reports.MonthlyClosingReport;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Vector;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.accounting.coa.ElementValue;

/**
 * @author Fernando Iriazabal
 * 
 *         This one is the class in charge of the report of accounting
 */
public class AccountTree {
  private static Logger log4j = Logger.getLogger(AccountTree.class);
  private VariablesSecureApp vars;
  private ConnectionProvider conn;
  private AccountTreeData[] accountsFacts;
  private AccountTreeData[] accountsTree;
  private AccountTreeData[] reportElements;
  private String[] reportNodes;
  // Used to inform if the applySign function has reset to zero the qty values
  // or not
  private boolean resetFlag;
  // True when operandsCalculate() calls calculateTree(), and the calculateTree()
  // calls again operandsCalculte()
  private boolean recursiveOperands = false;

  /**
   * Constructor
   * 
   * @param _vars
   *          VariablesSecureApp object with the session methods.
   * @param _conn
   *          ConnectionProvider object with the connection methods.
   * @param _accountsTree
   *          Array of element values. (structure)
   * @param _accountsFacts
   *          Array of accounting facts. (data)
   * @param _reportNode
   *          String with the value of the parent element to evaluate.
   * @throws ServletException
   */
  public AccountTree(VariablesSecureApp _vars, ConnectionProvider _conn,
      AccountTreeData[] _accountsTree, AccountTreeData[] _accountsFacts, String _reportNode)
      throws ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("AccountTree []");
    vars = _vars;
    conn = _conn;
    accountsTree = _accountsTree;
    accountsFacts = _accountsFacts;
    reportNodes = new String[1];
    reportNodes[0] = _reportNode;
    reportElements = updateTreeQuantitiesSign(null, 0, "D");
    // Calculating forms for every elements
    if (reportElements != null && reportElements.length > 0) {
      // forms: Array of accounts with its operands.
      AccountTreeData[] operands = AccountTreeData.selectOperands(conn,
          Utility.getContext(conn, vars, "#User_Org", "AccountTree"),
          Utility.getContext(conn, vars, "#User_Client", "AccountTree"),
          OBDal.getInstance().get(ElementValue.class, reportNodes[0]).getAccountingElement()
              .getId());
      reportElements = calculateTree(operands, reportNodes, new Vector<Object>());
    }
  }

  /**
   * Constructor
   * 
   * @param _vars
   *          VariablesSecureApp object with the session methods.
   * @param _conn
   *          ConnectionProvider object with the connection methods.
   * @param _accountsTree
   *          Array of account's elements (elementValues).
   * @param _accountsFacts
   *          Array of all the fact accts.
   * @param _reportNodes
   *          Array with the value of the parent elements to evaluate (For example, first expenses
   *          then revenues) Objective tree.
   * @throws ServletException
   */
  public AccountTree(VariablesSecureApp _vars, ConnectionProvider _conn,
      AccountTreeData[] _accountsTree, AccountTreeData[] _accountsFacts, String[] _reportNodes)
      throws ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("AccountTree []");
    vars = _vars;
    conn = _conn;
    accountsTree = _accountsTree;
    accountsFacts = _accountsFacts;
    reportNodes = _reportNodes;
    applySignAsPerParent();
    // Loading tree with new amounts, applying signs (Debit or Credit) and
    // setting the account level (1, 2, 3,...)
    reportElements = updateTreeQuantitiesSign(null, 0, "D");

    if (reportElements != null && reportElements.length > 0) {
      // Array of accounts with its operands.
      // Calculating forms for every elements
      AccountTreeData[] operands = AccountTreeData.selectOperands(conn,
          Utility.getContext(conn, vars, "#User_Org", "AccountTree"),
          Utility.getContext(conn, vars, "#User_Client", "AccountTree"),
          OBDal.getInstance().get(ElementValue.class, reportNodes[0]).getAccountingElement()
              .getId());

      Vector<Object> vec = new Vector<Object>();
      AccountTreeData[] r;

      for (int i = 0; i < reportNodes.length; i++) {
        r = calculateTree(operands, reportNodes[i], new Vector<Object>());
        for (int j = 0; j < r.length; j++)
          vec.addElement(r[j]);
      }

      reportElements = new AccountTreeData[vec.size()];
      vec.copyInto(reportElements);
    }
  }

  /**
   * Method to get the processed accounts.
   * 
   * @return Array with the resultant accounts.
   */
  public AccountTreeData[] getAccounts() {
    return reportElements;
  }

  /**
   * Applies the sign to the quantity, according to the showValueCond field
   * 
   * @param qty
   *          BigDecimal value with the quantity to evaluate.
   * @param sign
   *          String with the showValueCond field value.
   * @param isSummary
   *          Boolean that indicates if this is a summary record.
   * @return BigDecimal with the correct sign applied.
   */
  private BigDecimal applyShowValueCond(BigDecimal qty, String sign, boolean isSummary) {
    // resetFlag will store whether the value has been truncated because of
    // showvaluecond or not
    resetFlag = false;
    BigDecimal total = BigDecimal.ZERO;
    if (isSummary && !sign.equalsIgnoreCase("A")) {
      if (sign.equalsIgnoreCase("P")) {
        if (qty.compareTo(total) > 0) {
          total = qty;
        } else {
          total = BigDecimal.ZERO;
          resetFlag = true;
        }
      } else if (sign.equalsIgnoreCase("N")) {
        if (qty.compareTo(total) < 0) {
          total = qty;
        } else {
          total = BigDecimal.ZERO;
          resetFlag = true;
        }
      } else
        total = qty;
    } else
      total = qty;
    return total;
  }

  /**
   * Update the quantity and the operation quantity fields of the element, depending on the
   * isDebitCredit field.
   * 
   * @param reportElement
   *          AccoutnTreeData object with the element information.
   * @param isDebitCredit
   *          String with the parameter to evaluate if is a Debit or Credit element.
   * @return AccountTreeData object with the new element's information.
   */
  private AccountTreeData setDataQty(AccountTreeData reportElement, String isDebitCredit) {
    if (reportElement == null || accountsFacts == null || accountsFacts.length == 0)
      return reportElement;
    for (int i = 0; i < accountsFacts.length; i++) {
      if (accountsFacts[i].id.equals(reportElement.id)) {
        if (isDebitCredit.equals("C")) {
          accountsFacts[i].qty = accountsFacts[i].qtycredit;
          accountsFacts[i].qtyRef = accountsFacts[i].qtycreditRef;
        }
        reportElement.qtyOperation = accountsFacts[i].qty;
        reportElement.qtyOperationRef = accountsFacts[i].qtyRef;
        // added by gopal on adjustment
        reportElement.qtycredit = accountsFacts[i].qtycredit;
        reportElement.qtycreditRef = accountsFacts[i].qtycreditRef;
        BigDecimal bdQtyCredit = new BigDecimal(reportElement.qtycredit);
        BigDecimal bdQtyCreditRef = new BigDecimal(reportElement.qtycreditRef);

        reportElement.qtycredit = (applyShowValueCond(bdQtyCredit, reportElement.showvaluecond,
            reportElement.issummary.equals("Y"))).toPlainString();
        reportElement.qtycreditRef = (applyShowValueCond(bdQtyCreditRef,
            reportElement.showvaluecond, reportElement.issummary.equals("Y"))).toPlainString();
        //
        BigDecimal bdQty = new BigDecimal(reportElement.qtyOperation);
        BigDecimal bdQtyRef = new BigDecimal(reportElement.qtyOperationRef);

        reportElement.qty = (applyShowValueCond(bdQty, reportElement.showvaluecond,
            reportElement.issummary.equals("Y"))).toPlainString();
        reportElement.qtyRef = (applyShowValueCond(bdQtyRef, reportElement.showvaluecond,
            reportElement.issummary.equals("Y"))).toPlainString();
        break;
      }
    }
    return reportElement;
  }

  /**
   * This method updates all the Quantitie's signs of the tree. Is used by the constructor to
   * initialize the element's quantities. Also initializes the level of each account
   * 
   * @param rootElement
   *          String with the index from which to start updating.
   * @param level
   *          Integer with the level of the elements.
   * @param accountSign
   *          String with the is debit or credit value of the trunk.
   * @return Array of AccountTreeData with the updated tree.
   */
  private AccountTreeData[] updateTreeQuantitiesSign(String rootElement, int level,
      String accountSign) {
    if (accountsTree == null || accountsTree.length == 0)
      return accountsTree;
    AccountTreeData[] result = null;
    Vector<Object> vec = new Vector<Object>();
    // if (log4j.isDebugEnabled())
    // log4j.debug("AccountTree.updateTreeQuantitiesSign() - elements: " +
    // elements.length);
    if (rootElement == null)
      rootElement = "0";
    for (int i = 0; i < accountsTree.length; i++) {
      if (accountsTree[i].parentId.equals(rootElement)) {
        // accountSign = accountsTree[i].accountsign;
        AccountTreeData[] dataChilds = updateTreeQuantitiesSign(accountsTree[i].nodeId,
            (level + 1), accountSign);
        accountsTree[i].elementLevel = Integer.toString(level);
        accountsTree[i] = setDataQty(accountsTree[i], accountsTree[i].accountsign);
        vec.addElement(accountsTree[i]);
        if (dataChilds != null && dataChilds.length > 0) {
          for (int j = 0; j < dataChilds.length; j++)
            vec.addElement(dataChilds[j]);
        }
      }
    }
    result = new AccountTreeData[vec.size()];
    vec.copyInto(result);
    return result;
  }

  /**
   * Method to know if an element has form or not.
   * 
   * @param indice
   *          String with the index of the element.
   * @param forms
   *          Array with the existing forms.
   * @return Boolean indicating if has or not form.
   */
  private boolean hasOperand(String indice, AccountTreeData[] forms) {
    if (indice == null) {
      log4j.error("AccountTree.hasForm - Missing index");
      return false;
    }
    for (int i = 0; i < forms.length; i++) {
      if (forms[i].id.equals(indice))
        return true;
    }
    return false;
  }

  /**
   * Method to calculate the values with the operands's conditions.
   * 
   * @param vecAll
   *          Vector with the evaluated tree.
   * @param operands
   *          Array with the operands.
   * @param accountId
   *          String with the index of the element to evaluate.
   * @param vecTotal
   *          Vector with the totals of the operation.
   */
  private void operandsCalculate(Vector<Object> vecAll, AccountTreeData[] operands,
      String accountId, Vector<Object> vecTotal, boolean isExactValue) {
    if (isExactValue) {
      recursiveOperands = true;
    } else {
      recursiveOperands = false;
    }
    if (log4j.isDebugEnabled())
      log4j.debug("AccountTree.formsCalculate");
    if (reportElements == null || reportElements.length == 0)
      return;
    if (accountId == null) {
      log4j.error("AccountTree.formsCalculate - Missing accountId");
      return;
    }
    if (vecTotal == null)
      vecTotal = new Vector<Object>();
    if (vecTotal.size() == 0) {
      vecTotal.addElement("0");
      vecTotal.addElement("0");
    }
    BigDecimal total = new BigDecimal((String) vecTotal.elementAt(0));
    BigDecimal totalRef = new BigDecimal((String) vecTotal.elementAt(1));
    boolean encontrado = false;
    for (int i = 0; i < operands.length; i++) {
      if (operands[i].id.equals(accountId)) {
        encontrado = false;
        // There exists two options to calculate operands: run through
        // the already processed elements of the report (a) or call
        // calculateTree to obtain amount (b)
        /* (a) */
        for (int j = 0; j < vecAll.size(); j++) {
          AccountTreeData actual = (AccountTreeData) vecAll.elementAt(j);
          log4j.debug("AccountTree.formsCalculate - actual.nodeId: " + actual.nodeId
              + " - forms[i].nodeId: " + operands[i].nodeId);
          if (actual.nodeId.equals(operands[i].nodeId)) {
            encontrado = true;

            actual.qty = (applyShowValueCond(new BigDecimal(actual.qtyOperation),
                actual.showvaluecond, actual.issummary.equals("Y"))).toPlainString();
            actual.qtyRef = (applyShowValueCond(new BigDecimal(actual.qtyOperationRef),
                actual.showvaluecond, actual.issummary.equals("Y"))).toPlainString();
            total = total
                .add(new BigDecimal(actual.qty).multiply(new BigDecimal(operands[i].sign)));

            totalRef = totalRef.add(new BigDecimal(actual.qtyRef).multiply(new BigDecimal(
                operands[i].sign)));
            break;
          }
        }
        /* (b) */if (!encontrado) {
          if (log4j.isDebugEnabled())
            log4j.debug("AccountTree.formsCalculate - C_ElementValue_ID: " + operands[i].nodeId
                + " not found");
          Vector<Object> amounts = new Vector<Object>();
          amounts.addElement("0");
          amounts.addElement("0");
          calculateTree(operands, operands[i].nodeId, amounts, true, true);
          BigDecimal parcial = new BigDecimal((String) amounts.elementAt(0));
          BigDecimal parcialRef = new BigDecimal((String) amounts.elementAt(1));
          if (log4j.isDebugEnabled())
            log4j.debug("AccountTree.formsCalculate - parcial: " + parcial.toPlainString());
          parcial = parcial.multiply(new BigDecimal(operands[i].sign));
          parcialRef = parcialRef.multiply(new BigDecimal(operands[i].sign));
          if (log4j.isDebugEnabled())
            log4j.debug("AccountTree.formsCalculate - C_ElementValue_ID: " + operands[i].nodeId
                + " found with value: " + parcial + " account sign: " + operands[i].sign);
          total = total.add(parcial);
          totalRef = totalRef.add(parcialRef);
        }
      }
    }
    vecTotal.set(0, total.toPlainString());
    vecTotal.set(1, totalRef.toPlainString());
  }

  /**
   * Main method, which is called by the constructor to evaluate the tree.
   * 
   * @param forms
   *          Array with the forms.
   * @param indice
   *          Array with the start indexes.
   * @param vecTotal
   *          Vector with the accumulated totals.
   * @return Array with the new calculated tree.
   */
  private AccountTreeData[] calculateTree(AccountTreeData[] forms, String[] indice,
      Vector<Object> vecTotal) {
    return calculateTree(forms, indice, vecTotal, true, false);
  }

  /**
   * Main method, which is called by the constructor to evaluate the tree.
   * 
   * @param operands
   *          Array with the forms.
   * @param reportNode
   *          String with the index of the start element.
   * @param vecTotal
   *          Vector with the accumulated totals.
   * @return Array with the new calculated tree.
   */
  private AccountTreeData[] calculateTree(AccountTreeData[] operands, String reportNode,
      Vector<Object> vecTotal) {
    return calculateTree(operands, reportNode, vecTotal, true, false);
  }

  private boolean nodeIn(String node, String[] listOfNodes) {
    for (int i = 0; i < listOfNodes.length; i++)
      if (node.equals(listOfNodes[i]))
        return true;
    return false;
  }

  /**
   * Main method, which is called by the constructor to evaluate the tree.
   * 
   * @param operands
   *          Array with the forms.
   * @param reportNode
   *          String with the index of the start element.
   * @param vecTotal
   *          Vector with the accumulated totals.
   * @param applysign
   *          Boolean to know if the sign must be applied or not.
   * @param isExactValue
   *          Boolean auxiliar to use only for the calls from the forms calculating.
   * @return Array with the new calculated tree.
   */
  private AccountTreeData[] calculateTree(AccountTreeData[] operands, String reportNode,
      Vector<Object> vecTotal, boolean applysign, boolean isExactValue) {
    String[] i = new String[1];
    i[0] = reportNode;

    return calculateTree(operands, i, vecTotal, applysign, isExactValue);
  }

  /**
   * Main method, which is called by the constructor to evaluate the tree.
   * 
   * @param operands
   *          Array with the forms.
   * @param reportNode
   *          Array with the start indexes.
   * @param totalAmounts
   *          Vector with the accumulated totals.
   * @param applysign
   *          Boolean to know if the sign must be applied or not.
   * @param isExactValue
   *          Boolean auxiliar to use only for the calls from the forms calculating.
   * @return Array with the new calculated tree.
   */
  private AccountTreeData[] calculateTree(AccountTreeData[] operands, String[] reportNode,
      Vector<Object> totalAmounts, boolean applysign, boolean isExactValue) {
    if (reportElements == null || reportElements.length == 0)
      return reportElements;
    if (reportNode == null) {
      reportNode = new String[1];
      reportNode[0] = "0";
    }
    AccountTreeData[] result = null;
    Vector<Object> report = new Vector<Object>();
    if (log4j.isDebugEnabled())
      log4j.debug("AccountTree.calculateTree() - accounts: " + reportElements.length);
    if (totalAmounts == null)
      totalAmounts = new Vector<Object>();
    if (totalAmounts.size() == 0) {
      totalAmounts.addElement("0");
      totalAmounts.addElement("0");
      totalAmounts.addElement("0");
      totalAmounts.addElement("0");
    }
    BigDecimal total = new BigDecimal((String) totalAmounts.elementAt(0));
    BigDecimal totalRef = new BigDecimal((String) totalAmounts.elementAt(1));
    // added by
    // gopal for adjustment
    BigDecimal totalCredit = new BigDecimal((String) totalAmounts.elementAt(2));
    BigDecimal totalCreditRef = new BigDecimal((String) totalAmounts.elementAt(3));
    //

    for (int i = 0; i < reportElements.length; i++) {
      if ((isExactValue && nodeIn(reportElements[i].nodeId, reportNode))
          || (!isExactValue && nodeIn(reportElements[i].parentId, reportNode))) { // modified by
        // Eduardo Argal.
        // For
        // operands calculation
        AccountTreeData[] reportElementChilds = null;
        if (reportElements[i].calculated.equals("N")) // this would
        // work if it
        // only passed
        // here once,
        // but it's
        // passing more
        // times...
        // why????
        {
          Vector<Object> amounts = new Vector<Object>();
          amounts.addElement("0");
          amounts.addElement("0");
          amounts.addElement("0");
          amounts.addElement("0");
          @SuppressWarnings("unchecked")
          Vector<Object> reportAux = (Vector<Object>) report.clone();
          reportElementChilds = calculateTree(operands, reportElements[i].nodeId, amounts);
          if (reportElementChilds != null && reportElementChilds.length > 0) {
            for (int h = 0; h < reportElementChilds.length; h++) {
              reportAux.addElement(reportElementChilds[h]);
            }
          }
          if (!hasOperand(reportElements[i].nodeId, operands)) {
            BigDecimal parcial = new BigDecimal((String) amounts.elementAt(0));
            BigDecimal parcialRef = new BigDecimal((String) amounts.elementAt(1));
            // added by
            // gopal for adjustment
            BigDecimal parcialCredit = new BigDecimal((String) amounts.elementAt(2));
            BigDecimal parcialCreditRef = new BigDecimal((String) amounts.elementAt(3));
            //
            reportElements[i].qtyOperation = (new BigDecimal(reportElements[i].qtyOperation)
                .add(parcial)).toPlainString();
            reportElements[i].qtyOperationRef = (new BigDecimal(reportElements[i].qtyOperationRef)
                .add(parcialRef)).toPlainString();
            // added by
            // gopal for adjustment
            reportElements[i].qtycredit = (new BigDecimal(reportElements[i].qtycredit)
                .add(parcialCredit)).toPlainString();
            reportElements[i].qtycreditRef = (new BigDecimal(reportElements[i].qtycreditRef)
                .add(parcialCreditRef)).toPlainString();
            // log4j.debug("calculateTree - NothasForm - parcial:" + parcial
            // + " - resultantAccounts[i].qtyOperation:" + reportElements[i].qtyOperation
            // + " - resultantAccounts[i].nodeId:" + reportElements[i].nodeId);
          } else {
            amounts.set(0, "0");
            amounts.set(1, "0");
            amounts.set(2, "0");
            amounts.set(3, "0");
            operandsCalculate(reportAux, operands, reportElements[i].nodeId, amounts, isExactValue);
            BigDecimal parcial = new BigDecimal((String) amounts.elementAt(0));
            BigDecimal parcialRef = new BigDecimal((String) amounts.elementAt(1));
            // added by
            // gopal for adjustment
            BigDecimal parcialCredit = new BigDecimal((String) amounts.elementAt(2));
            BigDecimal parcialCreditRef = new BigDecimal((String) amounts.elementAt(3));
            //
            reportElements[i].qtyOperation = (new BigDecimal(reportElements[i].qtyOperation)
                .add(parcial)).toPlainString();
            reportElements[i].qtyOperationRef = (new BigDecimal(reportElements[i].qtyOperationRef)
                .add(parcialRef)).toPlainString();
            // added by
            // gopal for adjustment
            reportElements[i].qtycredit = (new BigDecimal(reportElements[i].qtycredit)
                .add(parcialCredit)).toPlainString();
            reportElements[i].qtycreditRef = (new BigDecimal(reportElements[i].qtycreditRef)
                .add(parcialCreditRef)).toPlainString();
            log4j.debug("calculateTree - HasForm - parcial:" + parcial
                + " - resultantAccounts[i].qtyOperation:" + reportElements[i].qtyOperation
                + " - resultantAccounts[i].nodeId:" + reportElements[i].nodeId);
          }
          // SVC show value condition
          String SVC = "";
          if (isExactValue && !recursiveOperands) {
            SVC = "A";
          } else {
            SVC = reportElements[i].showvaluecond;
          }
          reportElements[i].qty = (applyShowValueCond(
              new BigDecimal(reportElements[i].qtyOperation), SVC,
              reportElements[i].issummary.equals("Y"))).toPlainString();
          reportElements[i].qtyRef = (applyShowValueCond(new BigDecimal(
              reportElements[i].qtyOperationRef), SVC, reportElements[i].issummary.equals("Y")))
              .toPlainString();
          // addedby
          // gopal for adjustment
          reportElements[i].qtycredit = (applyShowValueCond(new BigDecimal(
              reportElements[i].qtycredit), SVC, reportElements[i].issummary.equals("Y")))
              .toPlainString();
          reportElements[i].qtycreditRef = (applyShowValueCond(new BigDecimal(
              reportElements[i].qtycreditRef), SVC, reportElements[i].issummary.equals("Y")))
              .toPlainString();

          if (resetFlag) {
            reportElements[i].svcreset = "Y";
            reportElements[i].svcresetref = "Y";
          }
          reportElements[i].calculated = "Y";
        }
        if (applysign) {
          total = total.add(new BigDecimal(reportElements[i].qty));
          totalRef = totalRef.add(new BigDecimal(reportElements[i].qtyRef));
          // addedby
          // gopal for adjustment
          totalCredit = totalCredit.add(new BigDecimal(reportElements[i].qtycredit));
          totalCreditRef = totalCreditRef.add(new BigDecimal(reportElements[i].qtycreditRef));
        } else {
          total = total.add(new BigDecimal(reportElements[i].qtyOperation));
          totalRef = totalRef.add(new BigDecimal(reportElements[i].qtyOperationRef));
          // addedby
          // gopal for adjustment
          totalCredit = totalCredit.add(new BigDecimal(reportElements[i].qtycredit));
          totalCreditRef = totalCreditRef.add(new BigDecimal(reportElements[i].qtycreditRef));
        }
        // If the element is not active and it has balance != 0 it must be shown otherwise, it must
        // not.
        ElementValue repElementAccount = OBDal.getInstance().get(ElementValue.class,
            reportElements[i].id);
        BigDecimal qtyOperation = new BigDecimal(reportElements[i].qtyOperation);
        if (repElementAccount.isActive() || (total.compareTo(BigDecimal.ZERO) != 0)
            || (qtyOperation.compareTo(BigDecimal.ZERO) != 0)) {
          report.addElement(reportElements[i]);
          if (reportElementChilds != null && reportElementChilds.length > 0) {
            for (int j = 0; j < reportElementChilds.length; j++)
              report.addElement(reportElementChilds[j]);
          }
        }
      }
    }
    totalAmounts.set(0, total.toPlainString());
    totalAmounts.set(1, totalRef.toPlainString());
    totalAmounts.set(2, totalCredit.toPlainString());
    totalAmounts.set(3, totalCreditRef.toPlainString());
    result = new AccountTreeData[report.size()];
    report.copyInto(result);
    return result;
  }

  /**
   * Method to make the level filter of the tree, to eliminate the levels that shouldn't be shown in
   * the report.
   * 
   * @param indice
   *          Array of indexes to evaluate.
   * @param found
   *          Boolean to know if the index has been found
   * @param strLevel
   *          String with the level.
   * @return New Array with the filter applied.
   */
  private AccountTreeData[] levelFilter(String[] indice, boolean found, String strLevel) {
    if (reportElements == null || reportElements.length == 0 || strLevel == null
        || strLevel.equals(""))
      return reportElements;
    AccountTreeData[] result = null;
    Vector<Object> vec = new Vector<Object>();
    if (log4j.isDebugEnabled())
      log4j.debug("AccountTree.levelFilter() - accounts: " + reportElements.length);

    // if (indice == null) indice="0";
    if (indice == null) {
      indice = new String[1];
      indice[0] = "0";
    }
    for (int i = 0; i < reportElements.length; i++) {
      // if (resultantAccounts[i].parentId.equals(indice) && (!found ||
      // resultantAccounts[i].elementlevel.equalsIgnoreCase(strLevel))) {
      if (nodeIn(reportElements[i].parentId, indice)
          && (!found || reportElements[i].elementlevel.equalsIgnoreCase(strLevel))) {
        AccountTreeData[] dataChilds = levelFilter(reportElements[i].nodeId,
            (found || reportElements[i].elementlevel.equals(strLevel)), strLevel);
        if (isAccountLevelLower(strLevel, reportElements[i]))
          vec.addElement(reportElements[i]);
        if (dataChilds != null && dataChilds.length > 0)
          for (int j = 0; j < dataChilds.length; j++)
            if (isAccountLevelLower(strLevel, dataChilds[j]))
              vec.addElement(dataChilds[j]);
      }
    }
    result = new AccountTreeData[vec.size()];
    vec.copyInto(result);
    vec.clear();
    return result;
  }

  /**
   * Method to make the level filter of the tree, to eliminate the levels that shouldn't be shown in
   * the report.
   * 
   * @param indice
   *          String with the index to evaluate.
   * @param found
   *          Boolean to know if the index has been found
   * @param strLevel
   *          String with the level.
   * @return New Array with the filter applied.
   */
  private AccountTreeData[] levelFilter(String indice, boolean found, String strLevel) {
    String[] i = new String[1];
    i[0] = indice;
    if (log4j.isDebugEnabled())
      log4j.debug("AccountTree.levelFilter1");
    return levelFilter(i, found, strLevel);
  }

  /**
   * Method to filter the complete tree to show only the desired levels.
   * 
   * @param indice
   *          Array of start indexes.
   * @param notEmptyLines
   *          Boolean to indicate if the empty lines must been removed.
   * @param strLevel
   *          String with the level.
   * @param isLevel
   *          Boolean not used.
   * @return New Array with the filtered tree.
   */
  public AccountTreeData[] filterStructure(String[] indice, boolean notEmptyLines, String strLevel,
      boolean isLevel) {
    if (log4j.isDebugEnabled())
      log4j.debug("AccountTree.filterStructure() - accounts: " + reportElements.length);
    if (reportElements == null || reportElements.length == 0)
      return reportElements;
    AccountTreeData[] result = null;
    Vector<Object> vec = new Vector<Object>();

    AccountTreeData[] r = levelFilter(indice, false, strLevel);

    for (int i = 0; i < r.length; i++) {
      if (r[i].showelement.equals("Y")) {
        r[i].qty = (applyShowValueCond(new BigDecimal(r[i].qty), r[i].showvaluecond, true))
            .toPlainString();
        r[i].qtyRef = (applyShowValueCond(new BigDecimal(r[i].qtyRef), r[i].showvaluecond, true))
            .toPlainString();
        if ((!notEmptyLines || (new BigDecimal(r[i].qty).compareTo(BigDecimal.ZERO) != 0 || new BigDecimal(
            r[i].qtyRef).compareTo(BigDecimal.ZERO) != 0)) || "Y".equals(r[i].isalwaysshown)) {
          if ("Y".equals(r[i].isalwaysshown)) {
            r[i].qty = null;
            r[i].qtyRef = null;
          }
          vec.addElement(r[i]);
        }
      }
    }
    result = new AccountTreeData[vec.size()];
    vec.copyInto(result);
    return result;
  }

  /**
   * Not used
   * 
   * @param notEmptyLines
   * @param strLevel
   * @param isLevel
   */
  public void filter(boolean notEmptyLines, String strLevel, boolean isLevel) {
    if (log4j.isDebugEnabled())
      log4j.debug("filter");
    if (reportElements == null)
      log4j.warn("No resultant Acct");
    reportElements = filterStructure(reportNodes, notEmptyLines, strLevel, isLevel);
  }

  /**
   * Resets amounts of subaccounts which parents have been reset because of show value condition
   */
  public void filterSVC() {
    if (log4j.isDebugEnabled())
      log4j.debug("AccountTree.filterShowValueCond() - accounts: " + reportElements.length);
    if (reportElements == null || reportElements.length == 0)
      return;

    int[] levels = new int[2];
    levels[0] = Integer.MAX_VALUE; // Value of the min level flaged as
    // SVCReset
    levels[1] = Integer.MAX_VALUE; // Value of the min level flaged as
    // SVCResetRef

    for (int i = 0; i < reportElements.length; i++) {
      int level = Integer.parseInt(reportElements[i].elementLevel);
      if (reportElements[i].svcreset.equals("Y")) {
        levels[0] = Math.min(level, levels[0]);
      }
      if (reportElements[i].svcresetref.equals("Y")) {
        levels[1] = Math.min(level, levels[1]);
      }
      if (level > levels[0]) {
        reportElements[i].qty = "0.0";
      }
      if (level == levels[0] && reportElements[i].svcreset.equals("N")) {
        levels[0] = Integer.MAX_VALUE;
      }
      if (level > levels[1]) {
        reportElements[i].qtyRef = "0.0";
      }
      if (level == levels[1] && reportElements[i].svcresetref.equals("N")) {
        levels[1] = Integer.MAX_VALUE;
      }
    }
  }

  private boolean isAccountLevelLower(String reportAccountLevel, AccountTreeData accountToBeAdded) {
    if (reportAccountLevel.equalsIgnoreCase("D"))
      if (accountToBeAdded.elementlevel.equalsIgnoreCase("S"))
        return false;

    return true;
  }

  private void applySignAsPerParent() {
    if (accountsTree == null || accountsTree.length == 0) {
      return;
    }
    String parentId = accountsTree[0].id;
    String accountSign = accountsTree[0].accountsign;
    HashMap<String, String> parentSigns = new HashMap<String, String>();
    parentSigns.put(parentId, accountSign);
    for (AccountTreeData node : accountsTree) {
      parentId = node.id;
      accountSign = node.accountsign;
      parentSigns.put(parentId, accountSign);
      if (parentSigns.get(node.parentId) != null
          && !node.accountsign.equals(parentSigns.get(node.parentId))) {
        node.accountsign = parentSigns.get(node.parentId);
      }
    }
    return;
  }

}
