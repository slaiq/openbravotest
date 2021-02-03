package sa.elm.ob.finance.ad_process.PurchaseInvoice;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.openbravo.base.ConfigParameters;
import org.hibernate.exception.GenericJDBCException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.dal.core.DalContextListener;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.accounting.coa.AccountingCombination;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.service.db.DbUtility;
import org.openbravo.utils.Replace;

import sa.elm.ob.finance.EfinBudManencumRev;
import sa.elm.ob.finance.EfinBudgetInquiry;
import sa.elm.ob.finance.EfinBudgetIntialization;
import sa.elm.ob.finance.EfinBudgetManencum;
import sa.elm.ob.finance.EfinBudgetManencumlines;
import sa.elm.ob.finance.EfinBudgetManencumv;
import sa.elm.ob.finance.EfinInvoicePaymentSch;
import sa.elm.ob.finance.EfinManualEncumInvoice;
import sa.elm.ob.finance.EfinRdvHoldAction;
import sa.elm.ob.finance.ad_process.dao.ManualEncumbaranceSubmitDAO;
import sa.elm.ob.finance.util.CommonValidations;
import sa.elm.ob.finance.util.FinanceUtils;
import sa.elm.ob.finance.util.DAO.CommonValidationsDAO;
import sa.elm.ob.utility.DMSIntegrationLog;
import sa.elm.ob.scm.ESCMPaymentSchedule;
import sa.elm.ob.scm.ad_process.POandContract.dao.POContractSummaryDAO;
import sa.elm.ob.utility.ad_forms.nextrolebyrule.NextRoleByRuleVO;
import sa.elm.ob.utility.dms.consumer.GRPDmsImplementation;
import sa.elm.ob.utility.dms.consumer.GRPDmsInterface;
import sa.elm.ob.utility.dms.consumer.dto.CreateAttachmentResponseGRP;
import sa.elm.ob.utility.dms.util.DMSConstants;
import sa.elm.ob.utility.dms.util.DMSXmlAttributes;
import sa.elm.ob.utility.dms.util.GetServiceAccount;
import sa.elm.ob.utility.util.ApprovalTables;
import sa.elm.ob.utility.util.Utility;
import sa.elm.ob.utility.util.UtilityDAO;

public class PurchaseInvoiceSubmitUtils {
  private static final Logger log4j = Logger.getLogger(PurchaseInvoiceSubmitUtils.class);
  @SuppressWarnings("unused")
  private Connection connection = null;
  private static Invoice invoice = null;
  private static final String API_DOCUMENT = "API";
  private static final String PPI_DOCUMENT = "PPI";
  private static final String PPA_DOCUMENT = "PPA";
  private static final String RDV_DOCUMENT = "RDV";
  public static final String PO_DOCUMENT = "POM";
  public static final String STATUS_APPROVED = "APP";
  public static final String MANUAL = "M";

  public PurchaseInvoiceSubmitUtils(Connection connection) {
    this.connection = connection;
  }

  public PurchaseInvoiceSubmitUtils(Invoice invoice) {
    PurchaseInvoiceSubmitUtils.invoice = invoice;
  }

  /**
   * This method returns the type of the invoice that is being processed.
   * 
   * @param invoice
   *          - Invoice object of the record being processed {@link Invoice}
   * @return {@link String} - type of Invoice. API -> API invoice, PPI -> Prepayment Invoice, PPA ->
   *         Prepayment Application.
   */
  public static String getInvoiceType(Invoice invoice) {
    String strInvoiceType = "";
    try {
      OBContext.setAdminMode();
      if (invoice != null) {
        if (invoice.getTransactionDocument().isEfinIsprepayinv())
          strInvoiceType = "PPI";
        else if (invoice.getTransactionDocument().isEfinIsprepayinvapp())
          strInvoiceType = "PPA";
        else if (invoice.getTransactionDocument().isEfinIsrdvinv())
          strInvoiceType = "RDV";
        else if (invoice.getTransactionDocument().isEfinIspomatch())
          strInvoiceType = "POM";
        else
          strInvoiceType = "API";
      }
    } catch (Exception e) {
      log4j.error("Exception while getInvoiceType: " + e);
    }
    return strInvoiceType;
  }

  /**
   * This method is used to check cost encumbrance
   * 
   * @param invoice
   * @return
   */
  public static Boolean isCostEncumbrance(Invoice invoice) {
    Boolean isCostEncumbrance = Boolean.FALSE;
    String strBudgetType = "";

    try {
      if (invoice.getEfinManualencumbrance() != null) {
        strBudgetType = invoice.getEfinManualencumbrance().getSalesCampaign().getEfinBudgettype();
      } else {
        strBudgetType = invoice.getEfinBudgetType();
      }
      if ("C".equals(strBudgetType))
        isCostEncumbrance = Boolean.TRUE;
    } catch (Exception e) {
      log4j.error("Exception while isCostEncumbrance: " + e);
    }
    return isCostEncumbrance;
  }

  /**
   * This method is used to get budget type
   * 
   * @param bType
   * @return
   */
  public static Campaign getBudgetType(String bType) {
    Campaign campaign = null;
    try {
      List<Campaign> campaigns = new ArrayList<>();
      OBQuery<Campaign> campaignQuery = OBDal.getInstance().createQuery(Campaign.class,
          " where efinBudgettype = :budgetType ");
      campaignQuery.setNamedParameter("budgetType", bType);
      if (campaignQuery != null) {
        campaigns = campaignQuery.list();
        if (campaigns.size() > 0) {
          campaign = campaigns.get(0);
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getFundsBudgetType: " + e);
    }
    return campaign;
  }

  /**
   * This method is used to get funds budget type
   * 
   * @return
   */
  public static Campaign getFundsBudgetType() {
    Campaign campaign = null;
    try {
      List<Campaign> campaigns = new ArrayList<>();
      OBQuery<Campaign> campaignQuery = OBDal.getInstance().createQuery(Campaign.class,
          " where efinBudgettype = 'F' ");

      if (campaignQuery != null) {
        campaigns = campaignQuery.list();
        if (campaigns.size() > 0) {
          campaign = campaigns.get(0);
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getFundsBudgetType: " + e);
    }
    return campaign;
  }

  /**
   * This method is used to get new funds encumbrance
   * 
   * @param fundsBudgetType
   * @param costEncumbranceV
   * @param invoice
   * @param conversionrate
   * @return
   */
  @SuppressWarnings("unused")
  public static EfinBudgetManencum getNewFundsEnumbrance(Campaign fundsBudgetType,
      EfinBudgetManencumv costEncumbranceV, Invoice invoice, BigDecimal conversionrate) {
    EfinBudgetManencum fundsEncumbrance = null;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      String strDocumentNo = "";
      EfinBudgetManencum costEncumbrance = null;
      BigDecimal convertedAmount = BigDecimal.ZERO;

      convertedAmount = FinanceUtils.getConvertedAmount(invoice.getGrandTotalAmount(),
          conversionrate);

      if (costEncumbranceV != null)
        costEncumbrance = Utility.getObject(EfinBudgetManencum.class,
            costEncumbranceV.getManualEncumbrance());

      fundsEncumbrance = OBProvider.getInstance().get(EfinBudgetManencum.class);
      strDocumentNo = Utility.getSequenceNo(OBDal.getInstance().getConnection(),
          invoice.getClient().getId(), "DocumentNo_efin_budget_manencum", Boolean.TRUE);

      fundsEncumbrance.setClient(invoice.getClient());
      fundsEncumbrance.setOrganization(invoice.getOrganization());
      fundsEncumbrance.setCreatedBy(OBContext.getOBContext().getUser());
      fundsEncumbrance.setUpdatedBy(OBContext.getOBContext().getUser());
      fundsEncumbrance.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      fundsEncumbrance.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      fundsEncumbrance.setAmount(BigDecimal.ZERO);
      fundsEncumbrance.setDocumentNo(strDocumentNo);
      fundsEncumbrance.setSalesCampaign(fundsBudgetType);
      fundsEncumbrance.setEncumType(invoice.getEfinEncumbranceType());
      fundsEncumbrance.setEncumStage("AEE");
      fundsEncumbrance.setEncumMethod("A");
      fundsEncumbrance.setDocumentStatus("CO");
      fundsEncumbrance.setAction("PD");
      fundsEncumbrance.setBudgetInitialization(invoice.getEfinBudgetint());
      fundsEncumbrance.setUsedamount(BigDecimal.ZERO);
      fundsEncumbrance.setRemainingamt(BigDecimal.ZERO);
      fundsEncumbrance.setSalesRegion(invoice.getEfinCSalesregion());
      fundsEncumbrance.setBusinessPartner(invoice.getBusinessPartner());
      fundsEncumbrance.setDescription(invoice.getDocumentNo());

      if (costEncumbranceV != null) {
        fundsEncumbrance.setCostEncumbrance(costEncumbrance);
        invoice.setEfinFundsEncumbrance(fundsEncumbrance);
      }

      OBDal.getInstance().save(fundsEncumbrance);
      OBDal.getInstance().save(invoice);

      OBDal.getInstance().flush();

    } catch (OBException e) {
      log4j.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log4j.error("Exception while getNewFundsEnumbrance: " + e);
    }

    return fundsEncumbrance;
  }

  /**
   * Thsi method is used to get encumbrance line amount
   * 
   * @param invoice
   * @return
   */
  public static Map<String, BigDecimal> getEncumbranceLineAmounts(Invoice invoice) {
    Map<String, BigDecimal> encumbranceMap = new HashMap<String, BigDecimal>();

    try {
      String strEncumbranceLineId = "";
      BigDecimal encumbranceAmount = BigDecimal.ZERO;

      if (invoice.getInvoiceLineList().size() > 0) {

        for (InvoiceLine line : invoice.getInvoiceLineList()) {
          encumbranceAmount = BigDecimal.ZERO;

          if (line.getEfinBudgmanuencumln() != null) {
            strEncumbranceLineId = line.getEfinBudgmanuencumln().getId();

            if (encumbranceMap.containsKey(strEncumbranceLineId)) {
              encumbranceAmount = encumbranceMap.get(strEncumbranceLineId);
            }
            encumbranceAmount = encumbranceAmount.add(line.getLineNetAmount());
            encumbranceMap.put(strEncumbranceLineId, encumbranceAmount);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while getEncumbranceLineAmounts: " + e);
    }
    return encumbranceMap;
  }

  /**
   * This method is used to update budget enquire
   * 
   * @param accountingCombination
   * @param grandTotalAmount
   * @param efinBudgetint
   * @param posted
   * @param reverseEntry
   * @param updateActualsOnly
   */
  public static void updateBudgetEnquiry(AccountingCombination accountingCombination,
      BigDecimal grandTotalAmount, EfinBudgetIntialization efinBudgetint, Boolean posted,
      Boolean reverseEntry, Boolean updateActualsOnly) {
    try {

      EfinBudgetInquiry budgetInquiry = null;
      budgetInquiry = getBudgetInquiry(accountingCombination, efinBudgetint);

      if (budgetInquiry != null) {
        if (posted) {
          if (reverseEntry) {
            if (grandTotalAmount.compareTo(BigDecimal.ZERO) < 0 && updateActualsOnly) {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount.negate()));
            } else {
              if (invoice != null && ("A".equals(invoice.getEfinEncumtype())
                  || invoice.getEfinFundsEncumbrance() != null)) {
                budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().subtract(grandTotalAmount));
                invoice = null;
              } else {
                budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().subtract(grandTotalAmount));
                budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().add(grandTotalAmount));
              }
            }
          } else {
            if (grandTotalAmount.compareTo(BigDecimal.ZERO) < 0 && updateActualsOnly) {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount));
            } else {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount));
              budgetInquiry
                  .setEncumbrance(budgetInquiry.getEncumbrance().subtract(grandTotalAmount));
            }
          }
        } else {
          if (reverseEntry) {
            budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().subtract(grandTotalAmount));
          } else {
            budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().add(grandTotalAmount));
          }
        }
      }

      OBDal.getInstance().save(budgetInquiry);

    } catch (Exception e) {
      log4j.error("Exception while insertEncumbranceLines: " + e);
    }
  }

  /**
   * This method is used to update BCU budget Enquiry
   * 
   * @param accountingCombination
   * @param grandTotalAmount
   * @param efinBudgetint
   * @param posted
   * @param reverseEntry
   * @param updateActualsOnly
   */
  public static void updateBCUBudgetEnquiry(AccountingCombination accountingCombination,
      BigDecimal grandTotalAmount, EfinBudgetIntialization efinBudgetint, Boolean posted,
      Boolean reverseEntry, Boolean updateActualsOnly) {
    try {

      EfinBudgetInquiry budgetInquiry = null;
      budgetInquiry = getBudgetInquiry(accountingCombination, efinBudgetint);
      budgetInquiry = budgetInquiry.getParent();

      if (budgetInquiry != null) {
        if (posted) {
          if (reverseEntry) {
            if (grandTotalAmount.compareTo(BigDecimal.ZERO) < 0 && updateActualsOnly) {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount.negate()));
            } else {
              if (invoice != null && "A".equals(invoice.getEfinEncumtype())) {
                budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().subtract(grandTotalAmount));
                invoice = null;
              } else {
                budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().subtract(grandTotalAmount));
                budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().add(grandTotalAmount));
              }
            }
          } else {
            if (grandTotalAmount.compareTo(BigDecimal.ZERO) < 0 && updateActualsOnly) {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount));
            } else {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount));
              budgetInquiry
                  .setEncumbrance(budgetInquiry.getEncumbrance().subtract(grandTotalAmount));
            }
          }
        } else {
          if (reverseEntry) {
            budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().subtract(grandTotalAmount));
          } else {
            budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().add(grandTotalAmount));
          }
        }
      }

      OBDal.getInstance().save(budgetInquiry);

    } catch (Exception e) {
      log4j.error("Exception while insertEncumbranceLines: " + e);
    }
  }

  /**
   * This method is used to update department budget enquiry
   * 
   * @param accountingCombination
   * @param grandTotalAmount
   * @param efinBudgetint
   * @param posted
   * @param reverseEntry
   * @param updateActualsOnly
   */
  public static void updateDeptNBudgetEnquiry(AccountingCombination accountingCombination,
      BigDecimal grandTotalAmount, EfinBudgetIntialization efinBudgetint, Boolean posted,
      Boolean reverseEntry, Boolean updateActualsOnly) {
    try {

      EfinBudgetInquiry budgetInquiry = ManualEncumbaranceSubmitDAO
          .getBudgetInquiry(accountingCombination.getId(), efinBudgetint.getId());

      if (budgetInquiry != null && !accountingCombination.isEFINDepartmentFund()) {
        if (posted) {
          if (reverseEntry) {
            if (grandTotalAmount.compareTo(BigDecimal.ZERO) < 0 && updateActualsOnly) {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount.negate()));
            } else {
              if (invoice != null && ("A".equals(invoice.getEfinEncumtype())
                  || invoice.getEfinFundsEncumbrance() != null)) {
                budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().subtract(grandTotalAmount));
              } else {
                budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().subtract(grandTotalAmount));
                budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().add(grandTotalAmount));
              }
            }
          } else {
            if (grandTotalAmount.compareTo(BigDecimal.ZERO) < 0 && updateActualsOnly) {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount));
            } else {
              budgetInquiry.setSpentAmt(budgetInquiry.getSpentAmt().add(grandTotalAmount));
              budgetInquiry
                  .setEncumbrance(budgetInquiry.getEncumbrance().subtract(grandTotalAmount));
            }
          }
        } else {
          if (reverseEntry) {
            budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().subtract(grandTotalAmount));
          } else {
            budgetInquiry.setEncumbrance(budgetInquiry.getEncumbrance().add(grandTotalAmount));
          }
        }
      }

      OBDal.getInstance().save(budgetInquiry);

    } catch (Exception e) {
      log4j.error("Exception while insertEncumbranceLines: " + e);
    }
  }

  /**
   * This method is used to update department budget Enquiry
   * 
   * @param accountingCombination
   * @param grandTotalAmount
   * @param efinBudgetint
   * @param posted
   * @param reverseEntry
   */
  public static void updateDeptNBudgetEnquiry(AccountingCombination accountingCombination,
      BigDecimal grandTotalAmount, EfinBudgetIntialization efinBudgetint, Boolean posted,
      Boolean reverseEntry) {
    updateDeptNBudgetEnquiry(accountingCombination, grandTotalAmount, efinBudgetint, posted,
        reverseEntry, Boolean.FALSE);
  }

  /**
   * This method is used to update Budget enquiry
   * 
   * @param accountingCombination
   * @param grandTotalAmount
   * @param efinBudgetint
   * @param posted
   * @param reverseEntry
   */
  public static void updateBudgetEnquiry(AccountingCombination accountingCombination,
      BigDecimal grandTotalAmount, EfinBudgetIntialization efinBudgetint, Boolean posted,
      Boolean reverseEntry) {
    updateBudgetEnquiry(accountingCombination, grandTotalAmount, efinBudgetint, posted,
        reverseEntry, Boolean.FALSE);
  }

  /**
   * This method is used to update BCU budget Enquiry
   * 
   * @param accountingCombination
   * @param grandTotalAmount
   * @param efinBudgetint
   * @param posted
   * @param reverseEntry
   */
  public static void updateBCUBudgetEnquiry(AccountingCombination accountingCombination,
      BigDecimal grandTotalAmount, EfinBudgetIntialization efinBudgetint, Boolean posted,
      Boolean reverseEntry) {
    updateBCUBudgetEnquiry(accountingCombination, grandTotalAmount, efinBudgetint, posted,
        reverseEntry, Boolean.FALSE);
  }

  /**
   * This method is used to get funds unqiue code
   * 
   * @param strEncumbranceLineId
   * @param fundsEncumbrance
   * @return
   */
  public static AccountingCombination getFundsUniquecode(String strEncumbranceLineId,
      EfinBudgetManencum fundsEncumbrance) {
    AccountingCombination uniqueCode = null, costUniqueCode = null;

    try {
      String strFBudgetTypeValue = "";
      String strCostUniqueCode = "", fundsUniqueCode = "";
      String[] costDimensions = null;

      strFBudgetTypeValue = fundsEncumbrance.getSalesCampaign().getSearchKey();
      EfinBudgetManencumlines manencumlines = Utility.getObject(EfinBudgetManencumlines.class,
          strEncumbranceLineId);
      EfinBudgetInquiry fundsInquiry = null;

      costUniqueCode = manencumlines.getAccountingCombination();
      strCostUniqueCode = costUniqueCode.getEfinUniqueCode();
      costDimensions = strCostUniqueCode.split("-");
      costDimensions[4] = strFBudgetTypeValue;
      fundsUniqueCode = String.join("-", costDimensions);

      OBQuery<EfinBudgetInquiry> fundsQuery = OBDal.getInstance().createQuery(
          EfinBudgetInquiry.class,
          " where uniqueCode =:fundsCode  and efinBudgetint.id =:budgetInit ");
      fundsQuery.setNamedParameter("fundsCode", fundsUniqueCode);
      fundsQuery.setNamedParameter("budgetInit",
          fundsEncumbrance.getBudgetInitialization().getId());

      if (fundsQuery != null && fundsQuery.list().size() > 0) {
        fundsInquiry = fundsQuery.list().get(0);
        uniqueCode = fundsInquiry.getAccountingCombination();
      }

    } catch (Exception e) {
      log4j.error("Exception while insertEncumbranceLines: " + e);
    }
    return uniqueCode;
  }

  /**
   * This method is used to insert encumbrance invoice reference
   * 
   * @param invoice
   * @param manencum
   * @param isCost
   * @param strInvoiceType
   * @param conversionrate
   */
  public static void insertEncumbranceInvoiceReference(Invoice invoice, EfinBudgetManencum manencum,
      Boolean isCost, String strInvoiceType, BigDecimal conversionrate) {
    try {
      String strCombinationId = "";
      Map<String, BigDecimal> combinations = new HashMap<String, BigDecimal>();
      AccountingCombination uniqueCode = null;

      for (EfinBudgetManencumlines encumbranceLines : manencum.getEfinBudgetManencumlinesList()) {
        StringBuilder whereClause = new StringBuilder();
        combinations = new HashMap<String, BigDecimal>();
        if ("API".equals(strInvoiceType) || "RDV".equals(strInvoiceType)
            || "POM".equals(strInvoiceType))
          whereClause.append(" where efinCValidcombination.id = :combinationId ");
        else if ("PPI".equals(strInvoiceType))
          whereClause.append(" where efinExpenseAccount.id = :combinationId ");

        whereClause.append("   and invoice.id =:invoiceId ");

        OBQuery<InvoiceLine> linesQuery = OBDal.getInstance().createQuery(InvoiceLine.class,
            whereClause.toString());

        if (isCost)
          strCombinationId = encumbranceLines.getAccountingCombination().getEfinCostcombination()
              .getId();
        else
          strCombinationId = encumbranceLines.getAccountingCombination().getId();

        linesQuery.setNamedParameter("combinationId", strCombinationId);
        linesQuery.setNamedParameter("invoiceId", invoice.getId());

        List<InvoiceLine> linesList = new ArrayList<InvoiceLine>();

        if (linesQuery != null) {
          linesList = linesQuery.list();
          if (linesList.size() > 0) {
            if (("PPI".equals(strInvoiceType) || "RDV".equals(strInvoiceType)
                || "POM".equals(strInvoiceType)) && linesList.size() > 1) {
              BigDecimal totalLineNetAmt = linesList.stream().map(a -> a.getLineNetAmount())
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
              InsertInvoicesInEncumbrancePPI(invoice, encumbranceLines, totalLineNetAmt,
                  conversionrate);

            } else {
              for (InvoiceLine invoiceLine : linesList) {
                uniqueCode = invoiceLine.getEfinCValidcombination();

                if (uniqueCode != null) {
                  if (combinations.containsKey(uniqueCode.getId())) {
                    combinations.put(uniqueCode.getId(),
                        combinations.get(uniqueCode.getId()).add(invoiceLine.getLineNetAmount()));
                  } else {
                    combinations.put(uniqueCode.getId(), invoiceLine.getLineNetAmount());
                  }
                }
                /*
                 * InsertInvoicesInEncumbrance(invoiceLine, encumbranceLines, strInvoiceType,
                 * conversionrate, invoiceLine.getInvoice());
                 */
              }
              for (Map.Entry<String, BigDecimal> entry : combinations.entrySet()) {
                InsertInvoicesInEncumbrance(null, encumbranceLines, strInvoiceType, conversionrate,
                    invoice, entry.getValue());
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while insertInvoiceReference: " + e);
    }

  }

  /**
   * This method is used to insert invoice in encumbrance
   * 
   * @param invoiceLine
   * @param encumbranceLines
   * @param strInvoiceType
   * @param conversionrate
   * @param invoice
   * @param totalAmount
   */
  public static void InsertInvoicesInEncumbrance(InvoiceLine invoiceLine,
      EfinBudgetManencumlines encumbranceLines, String strInvoiceType, BigDecimal conversionrate,
      Invoice invoice, BigDecimal totalAmount) {
    try {
      EfinManualEncumInvoice efinManEncInv = OBProvider.getInstance()
          .get(EfinManualEncumInvoice.class);

      efinManEncInv.setClient(OBContext.getOBContext().getCurrentClient());
      efinManEncInv.setOrganization(invoice.getOrganization());
      efinManEncInv.setActive(true);
      efinManEncInv.setUpdatedBy(OBContext.getOBContext().getUser());
      efinManEncInv.setCreationDate(new java.util.Date());
      efinManEncInv.setCreatedBy(OBContext.getOBContext().getUser());
      efinManEncInv.setUpdated(new java.util.Date());
      efinManEncInv.setInvamount(FinanceUtils.getConvertedAmount(totalAmount, conversionrate));
      efinManEncInv.setInvoiceDate(invoice.getInvoiceDate());
      efinManEncInv.setAccountingDate(invoice.getAccountingDate());
      efinManEncInv.setDescription(invoice.getDescription());
      efinManEncInv.setInvoiceLine(invoiceLine);
      efinManEncInv.setInvoice(invoice);
      efinManEncInv.setManualEncumbranceLines(encumbranceLines);
      efinManEncInv.setDocumentNo(invoice.getDocumentNo());

      OBDal.getInstance().save(efinManEncInv);

    } catch (Exception e) {
      log4j.error("Exception while InsertInvoicesInEncumbrance: " + e);
    }
  }

  /**
   * This method is used to insert invoice in encumbrance
   * 
   * @param invoice
   * @param encumbranceLines
   * @param conversionrate
   * @param totalLineNetAmt
   */
  public static void InsertInvoicesInEncumbrancePPI(Invoice invoice,
      EfinBudgetManencumlines encumbranceLines, BigDecimal conversionrate,
      BigDecimal totalLineNetAmt) {
    try {
      EfinManualEncumInvoice efinManEncInv = OBProvider.getInstance()
          .get(EfinManualEncumInvoice.class);

      efinManEncInv.setClient(OBContext.getOBContext().getCurrentClient());
      efinManEncInv.setOrganization(invoice.getOrganization());
      efinManEncInv.setActive(true);
      efinManEncInv.setUpdatedBy(OBContext.getOBContext().getUser());
      efinManEncInv.setCreationDate(new java.util.Date());
      efinManEncInv.setCreatedBy(OBContext.getOBContext().getUser());
      efinManEncInv.setUpdated(new java.util.Date());
      efinManEncInv.setInvamount(FinanceUtils.getConvertedAmount(totalLineNetAmt, conversionrate));
      efinManEncInv.setInvoiceDate(invoice.getInvoiceDate());
      efinManEncInv.setAccountingDate(invoice.getAccountingDate());
      efinManEncInv.setDescription(null);
      efinManEncInv.setInvoiceLine(null);
      efinManEncInv.setInvoice(invoice);
      efinManEncInv.setManualEncumbranceLines(encumbranceLines);
      efinManEncInv.setDocumentNo(invoice.getDocumentNo());

      OBDal.getInstance().save(efinManEncInv);

    } catch (Exception e) {
      log4j.error("Exception while InsertInvoicesInEncumbrance: " + e);
    }
  }

  /**
   * This method is used to insert auto encumbrance
   * 
   * @param invoice
   * @param fundsEncumbrance
   * @param isCost
   * @param strInvoiceType
   * @param conversionrate
   */
  public static void insertAutoEncumbranceLines(Invoice invoice,
      EfinBudgetManencum fundsEncumbrance, Boolean isCost, String strInvoiceType,
      BigDecimal conversionrate) {
    try {
      AccountingCombination uniqueCode = null;
      EfinBudgetManencumlines encumbranceLines = null;
      int lineNo = 10;
      BigDecimal encumbranceAmount = BigDecimal.ZERO, convertedAmount = BigDecimal.ZERO,
          lineNetAmount = BigDecimal.ZERO;
      Map<String, BigDecimal> combinations = new HashMap<String, BigDecimal>();
      for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {

        if ("API".equals(strInvoiceType) || "RDV".equals(strInvoiceType)
            || "POM".equals(strInvoiceType)) {

          if (isCost)
            uniqueCode = invoiceLine.getEfinCValidcombination().getEfinFundscombination();
          else
            uniqueCode = invoiceLine.getEfinCValidcombination();

        } else if ("PPI".equals(strInvoiceType)) {
          uniqueCode = invoiceLine.getEfinExpenseAccount();
        }

        convertedAmount = FinanceUtils.getConvertedAmount(invoiceLine.getLineNetAmount(),
            conversionrate);
        if (uniqueCode != null) {
          if (combinations.containsKey(uniqueCode.getId())) {
            combinations.put(uniqueCode.getId(),
                combinations.get(uniqueCode.getId()).add(convertedAmount));
          } else {
            combinations.put(uniqueCode.getId(), convertedAmount);
          }
        }
      }

      for (Map.Entry<String, BigDecimal> entry : combinations.entrySet()) {
        uniqueCode = Utility.getObject(AccountingCombination.class, entry.getKey());
        lineNetAmount = entry.getValue();
        EfinBudgetInquiry inquiryQuery = getBudgetInquiry(uniqueCode, invoice.getEfinBudgetint());
        JSONObject fundsCheckingObject = null;
        BigDecimal fundsAvailable = BigDecimal.ZERO;
        if (uniqueCode != null) {
          EfinBudgetIntialization budgetIntialization = Utility.getObject(
              EfinBudgetIntialization.class, fundsEncumbrance.getBudgetInitialization().getId());

          try {
            if ("E".equals(uniqueCode.getEfinDimensiontype())) {
              fundsCheckingObject = CommonValidations.getFundsAvailable(budgetIntialization,
                  uniqueCode);
              fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
            }
          } catch (Exception e) {
            fundsAvailable = BigDecimal.ZERO;
          }
        }
        if (inquiryQuery != null && lineNetAmount.compareTo(BigDecimal.ZERO) > 0) {

          encumbranceLines = OBProvider.getInstance().get(EfinBudgetManencumlines.class);

          encumbranceLines.setClient(invoice.getClient());
          encumbranceLines.setOrganization(invoice.getOrganization());
          encumbranceLines.setCreatedBy(OBContext.getOBContext().getUser());
          encumbranceLines.setUpdatedBy(OBContext.getOBContext().getUser());
          encumbranceLines.setAmount(lineNetAmount);
          encumbranceLines.setFundsAvailable(fundsAvailable);
          encumbranceLines.setRevamount(lineNetAmount);
          encumbranceLines.setSalesRegion(uniqueCode.getSalesRegion());
          encumbranceLines.setSalesCampaign(uniqueCode.getSalesCampaign());
          encumbranceLines.setAccountElement(uniqueCode.getAccount());
          encumbranceLines.setProject(uniqueCode.getProject());
          encumbranceLines.setActivity(uniqueCode.getActivity());
          encumbranceLines.setStDimension(uniqueCode.getStDimension());
          encumbranceLines.setNdDimension(uniqueCode.getNdDimension());
          encumbranceLines.setLineNo(Long.parseLong(String.valueOf(lineNo)));
          encumbranceLines.setManualEncumbrance(fundsEncumbrance);
          if (fundsEncumbrance.getCostEncumbrance() != null
              && !fundsEncumbrance.getCostEncumbrance().getEncumType().equals("AEE")
              && invoice.isEfinIsrdv()) {
            encumbranceLines.setAPPAmt(lineNetAmount);
          } else {
            encumbranceLines.setRemainingAmount(lineNetAmount);
          }

          encumbranceLines.setBusinessPartner(uniqueCode.getBusinessPartner());

          if (uniqueCode != null) {
            encumbranceLines.setAccountingCombination(uniqueCode);
            encumbranceLines.setUniquecode(uniqueCode.getEfinUniqueCode());
            encumbranceLines.setUniqueCodeName(uniqueCode.getEfinUniquecodename());
          }

          lineNo = lineNo + 10;

          OBDal.getInstance().save(encumbranceLines);

          fundsEncumbrance.getEfinBudgetManencumlinesList().add(encumbranceLines);
          encumbranceAmount = encumbranceAmount.add(lineNetAmount);
          fundsEncumbrance.setAmount(encumbranceAmount);
          OBDal.getInstance().save(fundsEncumbrance);

          if ("API".equals(strInvoiceType) || "RDV".equals(strInvoiceType)
              || "POM".equals(strInvoiceType))
            updateBudgetEnquiry(encumbranceLines.getAccountingCombination(), lineNetAmount,
                invoice.getEfinBudgetint(), Boolean.FALSE, Boolean.FALSE);
        }
        OBDal.getInstance().flush();
      }

      insertEncumbranceInvoiceReference(invoice, fundsEncumbrance, isCost, strInvoiceType,
          conversionrate);

    } catch (OBException e) {
      log4j.error(" Exception while insertAutoEncumbrance: " + e);
      throw new OBException(e.getMessage());
    } catch (Exception e) {
      log4j.error("Exception while insertAutoEncumbranceLines: " + e);
    }
  }

  /**
   * This method is used to get budget inquiry
   * 
   * @param combination
   * @param intialization
   * @return
   */
  public static EfinBudgetInquiry getBudgetInquiry(AccountingCombination combination,
      EfinBudgetIntialization intialization) {
    EfinBudgetInquiry inquiry = null;
    List<AccountingCombination> combinations = new ArrayList<AccountingCombination>();

    try {
      List<EfinBudgetInquiry> inquiryLines = new ArrayList<EfinBudgetInquiry>();

      if (combination.getEfinDimensiontype().equals("E")) {
        // if Department funds='N', then get parent account
        if (!combination.isEFINDepartmentFund()) {
          combinations = CommonValidationsDAO.getParentAccountCom(combination,
              combination.getClient().getId());
          if (combinations != null && combinations.size() > 0) {
            combination = combinations.get(0);
          }
        }
        if (combination != null) {
          OBQuery<EfinBudgetInquiry> fundsQuery = OBDal.getInstance().createQuery(
              EfinBudgetInquiry.class,
              " where uniqueCode =:fundsCode  and efinBudgetint.id =:budgetInit ");
          fundsQuery.setNamedParameter("fundsCode", combination.getEfinUniqueCode());
          fundsQuery.setNamedParameter("budgetInit", intialization.getId());

          if (fundsQuery != null) {
            inquiryLines = fundsQuery.list();
            if (inquiryLines.size() > 0) {
              inquiry = inquiryLines.get(0);
            } else {
              return null;
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception while getBudgetInquiry: " + e);
    }
    return inquiry;
  }

  /**
   * This method is to check is cross budget
   * 
   * @param invoice
   * @return
   */
  public static Boolean isCrossBudgetType(Invoice invoice) {
    Boolean isCrossBudgetType = Boolean.FALSE;

    try {
      String sql = "select distinct(c.c_campaign_id ) from c_invoice  i "
          + "join c_invoiceline   il on i.c_invoice_id = il.c_invoice_id "
          + "join c_validcombination  c on c.c_validcombination_id = il.em_efin_c_validcombination_id  "
          + "where i.c_invoice_id ='" + invoice.getId()
          + "' and coalesce(c.em_efin_accounttype,'A') = 'E'";

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
      if (query != null && query.list().size() > 1) {
        isCrossBudgetType = Boolean.TRUE;
      }
    } catch (Exception e) {
      log4j.error("Exception while isCrossBudgetType: " + e);
    }

    return isCrossBudgetType;
  }

  /**
   * This method is used to check pre reservation funds checking
   * 
   * @param invoice
   * @param conversionrate
   * @return
   */
  public static Boolean preReservationFundsChecking(Invoice invoice, BigDecimal conversionrate) {
    Boolean isValid = Boolean.TRUE;
    AccountingCombination fundsCombination = null, costCombination = null;
    BigDecimal invoiceAmount = BigDecimal.ZERO, convertedAmount = BigDecimal.ZERO;

    try {
      String strInvoiceType = PurchaseInvoiceSubmitUtils.getInvoiceType(invoice);

      List<InvoiceLine> invoiceLineList = invoice.getInvoiceLineList();
      for (InvoiceLine lines : invoiceLineList) {
        if ("E".equals(lines.getEfinCValidcombination().getEfinDimensiontype())) {
          if (invoice.getEfinBudgetType().equals("C")) {
            costCombination = lines.getEfinCValidcombination();
          } else {
            fundsCombination = lines.getEfinCValidcombination();
          }

          if (costCombination != null) {
            fundsCombination = costCombination.getEfinFundscombination();
          }

          invoiceAmount = invoice.getGrandTotalAmount();

          convertedAmount = invoiceLineList.stream()
              .filter(a -> a.getEfinCValidcombination() == lines.getEfinCValidcombination())
              .map(a -> a.getLineNetAmount().multiply(conversionrate))
              .reduce(BigDecimal.ZERO, BigDecimal::add);

          if (API_DOCUMENT.equals(strInvoiceType)) {
            // if it is cost, then check encumbrance remaining amount for cost uniquecode and
            // fundsavail for funds uniquecode
            if (invoice.getEfinEncumtype().equals("M")) {
              if (invoice.getEfinBudgetType().equals("C")) {
                isValid = checkEncumbranceLineRemainingAmount(invoice, invoiceAmount,
                    conversionrate, strInvoiceType);
                if (isValid) {
                  isValid = isFundsAvailable(invoice, fundsCombination, convertedAmount);
                }
              } else {
                isValid = isFundsAvailable(invoice, fundsCombination, convertedAmount);
              }
            } else {
              // if it is auto, then check funds avail of funds encumbrance
              isValid = isFundsAvailable(invoice, fundsCombination, convertedAmount);
            }

          } else if (PPI_DOCUMENT.equals(strInvoiceType) || PPA_DOCUMENT.equals(strInvoiceType)) {
            if (invoice.getEfinEncumtype().equals("M")) {
              isValid = checkEncumbranceLineRemainingAmount(invoice, invoiceAmount, conversionrate,
                  strInvoiceType);

            } else {
              isValid = isFundsAvailable(invoice, fundsCombination, convertedAmount);
            }
          } else {
            if (!invoice.getEfinManualencumbrance().getEncumbranceType().equals("AEE")) {
              if (invoice.getEfinBudgetType().equals("C")) {
                isValid = isFundsAvailable(invoice, fundsCombination, convertedAmount);
              }
            } else {
              if (invoice.getEfinBudgetType().equals("C")) {
                isValid = isFundsAvailable(invoice, costCombination, convertedAmount);
                if (isValid) {
                  isValid = isFundsAvailable(invoice, fundsCombination, convertedAmount);
                }
              } else {
                isValid = isFundsAvailable(invoice, fundsCombination, convertedAmount);
              }
            }
          }
          if (!isValid) {
            return isValid;
          }
        }

      }
    } catch (Exception e) {
      log4j.error("Exception while getFundsUniquecode: " + e);
    }

    return isValid;
  }

  /**
   * check funds available in uniquecode.
   * 
   * @param invoice
   * @param acctCombination
   * @param convertedAmount
   * @return
   */
  public static Boolean isFundsAvailable(Invoice invoice, AccountingCombination acctCombination,
      BigDecimal convertedAmount) {
    Boolean isValid = Boolean.TRUE;
    try {
      if (acctCombination != null && "E".equals(acctCombination.getEfinDimensiontype())) {
        JSONObject fundsCheckingObject = CommonValidationsDAO
            .CommonFundsChecking(invoice.getEfinBudgetint(), acctCombination, convertedAmount);

        if (fundsCheckingObject.has("errorFlag")
            && "0".equals(fundsCheckingObject.getString("errorFlag"))) {
          isValid = Boolean.FALSE;
        }
      }
    } catch (Exception e) {
      log4j.error("Exception while checkFundsavailable: " + e);
    }
    return isValid;
  }

  /**
   * This method is used to get reserved invoice
   * 
   * @param strInvoiceId
   * @return
   */
  public static List<EfinManualEncumInvoice> getReservedInvoices(String strInvoiceId) {
    List<EfinManualEncumInvoice> reservedInvoices = new ArrayList<EfinManualEncumInvoice>();
    ;
    try {

      OBQuery<EfinManualEncumInvoice> encuminvoiceQuery = OBDal.getInstance()
          .createQuery(EfinManualEncumInvoice.class, "invoice.id= :invoiceId");
      encuminvoiceQuery.setNamedParameter("invoiceId", strInvoiceId);

      if (encuminvoiceQuery != null) {
        reservedInvoices = encuminvoiceQuery.list();
      }

    } catch (Exception e) {
      log4j.error("Exception in getReservedInvoices :", e);
    }
    return reservedInvoices;
  }

  /**
   * This method is used to update advance encumbrance
   * 
   * @param invoice
   * @param grandTotal
   */
  public static void updateAdvanceEncumbrance(Invoice invoice, BigDecimal grandTotal) {
    try {
      EfinBudgetManencum headermanencum = OBDal.getInstance().get(EfinBudgetManencum.class,
          invoice.getEfinManualencumbrance().getId());

      headermanencum.setUpdated(new java.util.Date());
      headermanencum.setUpdatedBy(OBContext.getOBContext().getUser());
      headermanencum.setAppliedAmount(headermanencum.getAppliedAmount().add(grandTotal));
      headermanencum.setRemainingamt(headermanencum.getRemainingamt().subtract(grandTotal));

      OBDal.getInstance().save(headermanencum);

      PurchaseInvoiceSubmitUtils.insertEncumbranceInvoiceReference(invoice, headermanencum,
          Boolean.FALSE, "PPI", BigDecimal.ONE);

    } catch (Exception e) {
      log4j.error("Exception in getReservedInvoices :", e);
    }
  }

  /**
   * checking invoiceline amount with the remaining amount from manual encumbrance line
   * 
   * @param invoice
   * @param grandTotal
   */

  public static int checkRemainingAmount(Invoice invoice, BigDecimal grandTotal) {
    try {
      List<InvoiceLine> invLineList = invoice.getInvoiceLineList();

      for (InvoiceLine invLine : invLineList) {
        BigDecimal totalLineAmt = invLineList.stream()
            .filter(a -> a.getEfinExpenseAccount() == invLine.getEfinExpenseAccount())
            .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        EfinBudgetManencum manualEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
            invoice.getEfinManualencumbrance().getId());

        BigDecimal encumbrancelLineAmt = manualEncum.getEfinBudgetManencumlinesList().stream()
            .filter(a -> a.getAccountingCombination() == invLine.getEfinExpenseAccount())
            .map(a -> a.getRemainingAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (encumbrancelLineAmt.compareTo(totalLineAmt) < 0) {
          return -1;
        }

      }

    } catch (Exception e) {
      log4j.error("Exception in checkRemainingAmount :", e);
    }
    return 0;
  }

  /**
   * checking invoiceline amount with the remaining amount from manual encumbrance line remaining
   * amount
   * 
   * @param invoice
   * @param grandTotal
   * @param conversionrate
   */

  public static Boolean checkEncumbranceLineRemainingAmount(Invoice invoice, BigDecimal grandTotal,
      BigDecimal conversionrate, String invoiceType) {
    try {
      List<InvoiceLine> invLineList = invoice.getInvoiceLineList();

      if (API_DOCUMENT.equals(invoiceType) || RDV_DOCUMENT.equals(invoiceType)) {

        for (InvoiceLine invLine : invLineList) {

          BigDecimal totalLineAmt = invLineList.stream()
              .filter(a -> a.getEfinCValidcombination() == invLine.getEfinCValidcombination()
                  && "E".equals(a.getEfinCValidcombination().getEfinDimensiontype()))
              .map(a -> a.getLineNetAmount().multiply(conversionrate))
              .reduce(BigDecimal.ZERO, BigDecimal::add);

          EfinBudgetManencum manualEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
              invoice.getEfinManualencumbrance().getId());

          BigDecimal encumbrancelLineAmt = manualEncum.getEfinBudgetManencumlinesList().stream()
              .filter(a -> a.getAccountingCombination() == invLine.getEfinCValidcombination())
              .map(a -> a.getRemainingAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          if (encumbrancelLineAmt.compareTo(totalLineAmt) < 0) {
            return false;
          }

        }
      } else {
        for (InvoiceLine invLine : invLineList) {
          BigDecimal totalLineAmt = invLineList.stream()
              .filter(a -> a.getEfinExpenseAccount() == invLine.getEfinExpenseAccount())
              .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          EfinBudgetManencum manualEncum = OBDal.getInstance().get(EfinBudgetManencum.class,
              invoice.getEfinManualencumbrance().getId());

          BigDecimal encumbrancelLineAmt = manualEncum.getEfinBudgetManencumlinesList().stream()
              .filter(a -> a.getAccountingCombination() == invLine.getEfinExpenseAccount())
              .map(a -> a.getRemainingAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          if (encumbrancelLineAmt.compareTo(totalLineAmt) < 0) {
            return false;
          }

        }

      }

    } catch (Exception e) {
      log4j.error("Exception in checkRemainingAmount :", e);
    }
    return true;
  }

  /**
   * This method is used to check is valid pre payment invoice
   * 
   * @param invoice
   * @return
   */
  public static JSONObject isValidPrepaymentInvoice(Invoice invoice) {
    JSONObject errorObject = new JSONObject();

    try {
      String lineNo = "";

      errorObject.put("valid", Boolean.TRUE);
      errorObject.put("message", "Y");

      for (InvoiceLine line : invoice.getInvoiceLineList()) {
        if (line.getEfinExpenseAccount() == null) {
          lineNo = lineNo + ", " + line.getLineNo().intValue();
        }
      }

      if (lineNo.length() > 0) {
        errorObject.put("valid", Boolean.FALSE);
        errorObject.put("message", OBMessageUtils.messageBD("EFIN_PPI_NoExpenseAccount")
            .replaceAll("xx", lineNo.replaceFirst(",", "")));
      }
    } catch (Exception e) {
      log4j.error("Exception in getReservedInvoices :", e);
      try {
        errorObject.put("valid", Boolean.FALSE);
        errorObject.put("message", e.getMessage());
      } catch (JSONException e1) {
      }
    }
    return errorObject;
  }

  /**
   * This method is used to update applied amount
   * 
   * @param invoice
   * @param conversionrate
   * @param isRevoke
   */
  public static void updateAppliedAmountToUsedAmount(Invoice invoice, BigDecimal conversionrate,
      Boolean isRevoke) {
    try {
      StringBuilder queryBuilder = new StringBuilder();
      BigDecimal covertedAmount = BigDecimal.ZERO;
      String validCombination = "";
      HashMap<String, BigDecimal> lineMap = new HashMap<String, BigDecimal>();

      queryBuilder.append(
          " select sum(invl.linenetamt) as amount, encumlines.efin_budget_manencumlines_id  ");
      queryBuilder.append(" from c_invoice inv ");
      queryBuilder.append(" join c_invoiceline invl on invl.c_invoice_id = inv.c_invoice_id ");
      queryBuilder
          .append(" join efin_applied_prepayment ppi on ppi.c_invoice_id = inv.c_invoice_id ");
      queryBuilder.append(" join c_invoice app on app.c_invoice_id = efin_applied_invoice ");
      queryBuilder.append(" join c_invoiceline appl on appl.c_invoice_id = app.c_invoice_id  ");
      queryBuilder.append(
          " join efin_budget_manencumlines encumlines on encumlines.efin_budget_manencum_id = app.em_efin_manualencumbrance_id ");
      queryBuilder.append(" and appl.em_efin_expense_account = encumlines.c_validcombination_id ");
      queryBuilder
          .append(" where invl.em_efin_c_validcombination_id = appl.em_efin_expense_account ");
      queryBuilder.append(" and  ppi.c_invoice_id='").append(invoice.getId()).append("'");
      queryBuilder.append(" group by encumlines.efin_budget_manencumlines_id ");

      if (invoice.getEfinAppliedPrepaymentList().size() > 0) {
        /*
         * SQLQuery query =
         * OBDal.getInstance().getSession().createSQLQuery(queryBuilder.toString());
         * 
         * if (query != null) { encumlines = query.list(); if (encumlines.size() > 0) { for (Object
         * lines : encumlines) { Object[] details = (Object[]) lines; EfinBudgetManencumlines
         * encumbranceLine = Utility .getObject(EfinBudgetManencumlines.class,
         * details[1].toString()); BigDecimal lineNetAmount = new BigDecimal(details[0].toString());
         * lineNetAmount = FinanceUtils.getConvertedAmount(lineNetAmount, conversionrate); if
         * (encumbranceLine != null) {
         * encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().subtract(lineNetAmount));
         * encumbranceLine.setUsedAmount(encumbranceLine.getUsedAmount().add(lineNetAmount));
         * 
         * OBDal.getInstance().save(encumbranceLine); } } OBDal.getInstance().flush(); } }
         */

        if (invoice.getInvoiceLineList().size() > 0) {
          EfinBudgetManencumlines encumbranceLine = null;
          for (InvoiceLine line : invoice.getInvoiceLineList()) {
            covertedAmount = FinanceUtils.getConvertedAmount(line.getLineNetAmount(),
                conversionrate);
            validCombination = line.getEfinCValidcombination().getId();

            if (lineMap.containsKey(validCombination)) {
              lineMap.put(validCombination, lineMap.get(validCombination).add(covertedAmount));
            } else {
              lineMap.put(validCombination, covertedAmount);
            }
          }

          if (!lineMap.isEmpty()) {
            for (Map.Entry<String, BigDecimal> entry : lineMap.entrySet()) {
              validCombination = entry.getKey();
              covertedAmount = lineMap.get(validCombination);

              encumbranceLine = getEncumbranceLine(invoice.getEfinManualencumbrance().getId(),
                  validCombination);

              if (!isRevoke) {
                encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().subtract(covertedAmount));
                encumbranceLine.setUsedAmount(encumbranceLine.getUsedAmount().add(covertedAmount));
              } else {
                encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(covertedAmount));
                encumbranceLine
                    .setUsedAmount(encumbranceLine.getUsedAmount().subtract(covertedAmount));
              }

              OBDal.getInstance().save(encumbranceLine);
            }

            OBDal.getInstance().flush();
          }

        }
      }
    } catch (Exception e) {
      log4j.error("Exception in updateAppliedAmountToUsedAmount :", e);
    }
  }

  /**
   * This method is used to get encumbrance line
   * 
   * @param manualEncumbranceId
   * @param validCombination
   * @return
   */
  public static EfinBudgetManencumlines getEncumbranceLine(String manualEncumbranceId,
      String validCombination) {
    EfinBudgetManencumlines line = null;
    List<EfinBudgetManencumlines> lines = new ArrayList<EfinBudgetManencumlines>();
    try {
      OBQuery<EfinBudgetManencumlines> query = OBDal.getInstance().createQuery(
          EfinBudgetManencumlines.class,
          " where manualEncumbrance.id = :manualEncumbrance and accountingCombination.id = :validCombination ");

      query.setNamedParameter("manualEncumbrance", manualEncumbranceId);
      query.setNamedParameter("validCombination", validCombination);

      if (query != null) {
        lines = query.list();
        if (lines.size() > 0) {
          line = lines.get(0);
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in updateAppliedAmountToUsedAmount :", e);
    }
    return line;
  }

  /**
   * This method is used to update pre payment used amount
   * 
   * @param strInvoiceId
   * @param conversionrate
   * @param isRevoke
   */
  @SuppressWarnings({ "unchecked" })
  public static void updatePrepaymentUsedAmount(String strInvoiceId, BigDecimal conversionrate,
      Boolean isRevoke) {
    try {
      String sql = "";
      List<Object> prepayments = new ArrayList<Object>();
      sql = " select apppay.applied_amount,appinv.em_efin_pre_usedamount as usedamount, "
          + " appinv.em_efin_pre_remainingamount as remainamt,appinv.c_invoice_id "
          + " from efin_applied_prepayment apppay join c_invoice appinv on apppay.efin_applied_invoice=appinv.c_invoice_id "
          + " where  apppay.c_invoice_id='" + strInvoiceId + "'";

      SQLQuery ps = OBDal.getInstance().getSession().createSQLQuery(sql);
      if (ps != null) {
        prepayments = ps.list();

        if (prepayments.size() > 0) {
          for (Object prepayment : prepayments) {
            Object[] details = (Object[]) prepayment;

            Invoice apprepayInvoice = OBDal.getInstance().get(Invoice.class, details[3].toString());

            BigDecimal appliedAmt = new BigDecimal(details[0].toString());
            BigDecimal EfinPreRemainAmt = apprepayInvoice.getEfinPreRemainingamount();

            if (!isRevoke) {
              apprepayInvoice
                  .setEfinPreUsedamount(apprepayInvoice.getEfinPreUsedamount().add(appliedAmt));
              apprepayInvoice.setEfinPreRemainingamount(EfinPreRemainAmt.subtract(appliedAmt));
            } else {
              apprepayInvoice.setEfinPreUsedamount(
                  apprepayInvoice.getEfinPreUsedamount().subtract(appliedAmt));
              apprepayInvoice.setEfinPreRemainingamount(EfinPreRemainAmt.add(appliedAmt));
            }

            OBDal.getInstance().save(apprepayInvoice);

          }
          OBDal.getInstance().flush();
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in updatePrepaymentUsedAmount :", e);
    }
  }

  /**
   * This method is used to check auto invoice is valid
   * 
   * @param invoice
   * @param conversionrate
   * @return
   */
  public static Boolean validateAutoInvoice(Invoice invoice, BigDecimal conversionrate) {
    Boolean isValid = Boolean.FALSE;
    BigDecimal covertedAmount = BigDecimal.ZERO, fundsAvailable = BigDecimal.ZERO;
    try {
      String validCombination = "", parentAcctId = "";
      Map<String, BigDecimal> lineMap = new HashMap<String, BigDecimal>();
      List<AccountingCombination> parentAcctlist = new ArrayList<AccountingCombination>();
      OBQuery<EfinBudgetInquiry> budInq = null;
      Map<String, BigDecimal> parentFundsAvail = new HashMap<String, BigDecimal>();

      for (InvoiceLine line : invoice.getInvoiceLineList()) {
        if ("E".equals(line.getEfinCValidcombination().getEfinDimensiontype())) {
          covertedAmount = FinanceUtils.getConvertedAmount(line.getLineNetAmount(), conversionrate);
          validCombination = line.getEfinCValidcombination().getId();

          if (lineMap.containsKey(validCombination)) {
            lineMap.put(validCombination, lineMap.get(validCombination).add(covertedAmount));
          } else {
            lineMap.put(validCombination, covertedAmount);
          }
        }
      }
      // Check whether 2 unique codes with same account having Department Fund 'No' is submitting
      if (!lineMap.isEmpty()) {
        for (Map.Entry<String, BigDecimal> entry : lineMap.entrySet()) {
          AccountingCombination combination = Utility.getObject(AccountingCombination.class,
              entry.getKey());

          if (!combination.isEFINDepartmentFund()) {
            parentAcctlist = CommonValidationsDAO.getParentAccountCom(combination,
                combination.getClient().getId());

            if (parentAcctlist != null && parentAcctlist.size() > 0) {

              AccountingCombination parentAcct = parentAcctlist.get(0);
              parentAcctId = parentAcct.getId();

              budInq = OBDal.getInstance().createQuery(EfinBudgetInquiry.class,
                  "efinBudgetint.id='" + invoice.getEfinBudgetint().getId()
                      + "'  and accountingCombination.id='" + parentAcct.getId() + "'");
              if (budInq.list() != null && budInq.list().size() > 0) {
                EfinBudgetInquiry Enquiry = budInq.list().get(0);
                if (parentAcctId.equals(Enquiry.getAccountingCombination().getId())) {
                  if (parentFundsAvail.containsKey(parentAcctId)) {
                    parentFundsAvail.put(parentAcctId,
                        parentFundsAvail.get(parentAcctId).add(entry.getValue()));
                  } else {
                    parentFundsAvail.put(parentAcctId, entry.getValue());
                  }
                }

              }
            }
          }
        }
      }
      if (!parentFundsAvail.isEmpty()) {
        for (Map.Entry<String, BigDecimal> entry : parentFundsAvail.entrySet()) {
          AccountingCombination combination = Utility.getObject(AccountingCombination.class,
              entry.getKey());

          JSONObject fundsCheckingObject = CommonValidations
              .getFundsAvailable(invoice.getEfinBudgetint(), combination);

          if (fundsCheckingObject != null) {
            fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
            if (fundsAvailable.compareTo(entry.getValue()) < 0) {
              isValid = Boolean.FALSE;
              return isValid;
            }
          }
        }
      }

      if (!lineMap.isEmpty()) {
        for (Map.Entry<String, BigDecimal> entry : lineMap.entrySet()) {
          AccountingCombination combination = Utility.getObject(AccountingCombination.class,
              entry.getKey());

          JSONObject fundsCheckingObject = CommonValidations
              .getFundsAvailable(invoice.getEfinBudgetint(), combination);

          if (fundsCheckingObject != null) {
            fundsAvailable = new BigDecimal(fundsCheckingObject.get("FA").toString());
            if (fundsAvailable.compareTo(entry.getValue()) >= 0) {
              isValid = Boolean.TRUE;
            } else {
              isValid = Boolean.FALSE;
              return isValid;
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in updatePrepaymentUsedAmount :", e);
    }

    return isValid;
  }

  /**
   * This method is used to validate PPA
   * 
   * @param invoice
   * @param conversionrate
   * @return
   */
  public static String isValidPPA(Invoice invoice, BigDecimal conversionrate) {
    String violatingCodes = "";

    try {
      BigDecimal covertedAmount = BigDecimal.ZERO, appliedAmount = BigDecimal.ZERO,
          lineAmount = BigDecimal.ZERO;
      String validCombination = "";

      Map<String, BigDecimal> lineMap = new HashMap<String, BigDecimal>();
      EfinBudgetManencum encumbrance = Utility.getObject(EfinBudgetManencum.class,
          invoice.getEfinManualencumbrance().getId());

      Map<String, BigDecimal> encumbranceLines = getEncumbranceLinesAppliedAmount(encumbrance);

      for (InvoiceLine line : invoice.getInvoiceLineList()) {
        covertedAmount = FinanceUtils.getConvertedAmount(line.getLineNetAmount(), conversionrate);
        validCombination = line.getEfinCValidcombination().getId();

        if (lineMap.containsKey(validCombination)) {
          lineMap.put(validCombination, lineMap.get(validCombination).add(covertedAmount));
        } else {
          lineMap.put(validCombination, covertedAmount);
        }
      }

      for (Map.Entry<String, BigDecimal> entry : lineMap.entrySet()) {
        validCombination = entry.getKey();
        if (encumbranceLines.containsKey(validCombination)) {

          lineAmount = lineMap.get(validCombination);
          appliedAmount = encumbranceLines.get(validCombination);

          if (appliedAmount.compareTo(lineAmount) < 0) {
            AccountingCombination uniqueCode = Utility.getObject(AccountingCombination.class,
                validCombination);
            violatingCodes = violatingCodes + "," + uniqueCode.getEfinUniqueCode();
          }
        } else {
          AccountingCombination uniqueCode = Utility.getObject(AccountingCombination.class,
              validCombination);
          violatingCodes = violatingCodes + "," + uniqueCode.getEfinUniqueCode();
        }
      }

      if (violatingCodes.length() > 0)
        violatingCodes = violatingCodes.replaceFirst(",", "");

    } catch (Exception e) {
      log4j.error("Exception in isValidPPA :", e);
    }
    return violatingCodes;
  }

  /**
   * This method is used to get encumbrance line applied amount
   * 
   * @param encumbrance
   * @return
   */
  private static Map<String, BigDecimal> getEncumbranceLinesAppliedAmount(
      EfinBudgetManencum encumbrance) {
    Map<String, BigDecimal> encumbranceLines = new HashMap<String, BigDecimal>();

    try {
      String validCombination = "";

      for (EfinBudgetManencumlines line : encumbrance.getEfinBudgetManencumlinesList()) {
        validCombination = line.getAccountingCombination().getId();
        encumbranceLines.put(validCombination, line.getAPPAmt());
      }

    } catch (Exception e) {
      log4j.error("Exception in getEncumbranceLinesAppliedAmount :", e);
    }

    return encumbranceLines;
  }

  /**
   * This method is used to get expense lines
   * 
   * @param invoice
   * @param conversionrate
   * @param strInvoiceType
   * @param isCostEncumbrance
   * @return
   */
  public static HashMap<String, BigDecimal> getExpenseLines(Invoice invoice,
      BigDecimal conversionrate, String strInvoiceType, Boolean isCostEncumbrance) {
    HashMap<String, BigDecimal> combinations = new HashMap<String, BigDecimal>();
    try {
      BigDecimal convertedAmount = BigDecimal.ZERO;
      AccountingCombination uniqueCode = null;
      for (InvoiceLine invoiceLine : invoice.getInvoiceLineList()) {

        if ("API".equals(strInvoiceType) || "RDV".equals(strInvoiceType)
            || "POM".equals(strInvoiceType)) {

          if (isCostEncumbrance)
            uniqueCode = invoiceLine.getEfinCValidcombination().getEfinFundscombination();
          else
            uniqueCode = invoiceLine.getEfinCValidcombination();

        } else if ("PPI".equals(strInvoiceType)) {
          uniqueCode = invoiceLine.getEfinExpenseAccount();
        }

        convertedAmount = FinanceUtils.getConvertedAmount(invoiceLine.getLineNetAmount(),
            conversionrate);
        if (uniqueCode != null && "E".equals(uniqueCode.getEFINAccountType())) {
          if (combinations.containsKey(uniqueCode.getId())) {
            combinations.put(uniqueCode.getId(),
                combinations.get(uniqueCode.getId()).add(convertedAmount));
          } else {
            combinations.put(uniqueCode.getId(), convertedAmount);
          }
        }
      }
    } catch (Exception e) {
      log4j.error("Exception in getInvoicelines :", e);
    }
    return combinations;
  }

  /**
   * This methos is used to check adjustment only invoice
   * 
   * @param pInovice
   * @return
   */
  public static Boolean isAdjustmentOnlyInvoice(Invoice pInovice) {
    Boolean isAdjustment = Boolean.TRUE;
    try {
      AccountingCombination lineCombination = null;
      if (pInovice != null && pInovice.getInvoiceLineList().size() > 0) {

        for (InvoiceLine line : pInovice.getInvoiceLineList()) {
          lineCombination = line.getEfinCValidcombination();
          if (lineCombination != null && "E".equals(lineCombination.getEfinDimensiontype())) {
            isAdjustment = Boolean.FALSE;
            return isAdjustment;
          }
        }
      }

    } catch (Exception e) {
      log4j.error("Exception in isAdjustmentOnlyInvoice :", e);
    }
    return isAdjustment;
  }

  /**
   * This method is used to split PO encumbrance
   * 
   * @param pInovice
   * @return
   */

  @SuppressWarnings({ "rawtypes", "unused" })
  public static String splitPoencum(Invoice pInovice) {
    Boolean success = Boolean.TRUE;
    String query = null;
    String acctcode = "", encumid = "";
    BigDecimal amt = BigDecimal.ZERO;
    Query sqlQuery1 = null;
    List queryList1 = null;
    List<InvoiceLine> invLnList = null;
    DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    long lineno = 0;
    try {
      OBContext.setAdminMode();

      // get smaple uniqcode to get dept and budgettype for invoice.
      OBQuery<InvoiceLine> invline = OBDal.getInstance().createQuery(InvoiceLine.class,
          "efinIspom='Y' and invoice.id=:inv");
      invline.setNamedParameter("inv", pInovice.getId());
      invLnList = invline.list();
      AccountingCombination uniqueCode = invLnList.get(0).getEfinCValidcombination();

      // create new encum
      EfinBudgetManencum encum = OBProvider.getInstance().get(EfinBudgetManencum.class);
      encum.setSalesCampaign(uniqueCode.getSalesCampaign());
      encum.setEncumType("AEE");
      encum.setSalesRegion(pInovice.getEfinCSalesregion());
      encum.setEncumMethod("A");
      encum.setEncumStage("AEE");
      encum.setOrganization(pInovice.getOrganization());
      encum.setAccountingDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setTransactionDate(dateFormat.parse(dateFormat.format(new Date())));
      encum.setBudgetInitialization(pInovice.getEfinBudgetint());
      encum.setAction("PD");
      encum.setBusinessPartner(pInovice.getBusinessPartner());
      encum.setDescription(pInovice.getEfinManualencumbrance() != null
          ? pInovice.getEfinManualencumbrance().getDocumentNo()
          : "-");

      OBDal.getInstance().save(encum);

      query = "select invln.em_efin_c_validcombination_id,sum(linenetamt) from c_invoiceline invln where invln.c_invoice_id=? and em_efin_ispom='Y' group by em_efin_c_validcombination_id ";
      sqlQuery1 = OBDal.getInstance().getSession().createSQLQuery(query);
      sqlQuery1.setParameter(0, pInovice.getId());
      queryList1 = sqlQuery1.list();

      for (Iterator iterator = queryList1.iterator(); iterator.hasNext();) {
        Object[] row = (Object[]) iterator.next();
        acctcode = row[0].toString();
        amt = new BigDecimal(row[1].toString());
        AccountingCombination acct = OBDal.getInstance().get(AccountingCombination.class, acctcode);
        // reduce in oldencum and insert new encum lines.
        EfinBudManencumRev manEncumRev = null;

        EfinBudgetManencum POEncumbrance = Utility.getObject(EfinBudgetManencum.class,
            pInovice.getEfinManualencumbrance().getManualEncumbrance());
        // reduce in old encum
        manEncumRev = insertModifiactionPOM(acct, amt, POEncumbrance);
        lineno = lineno + 10;
        // insert new lines in new encum
        EfinBudgetManencumlines encumLines = OBProvider.getInstance()
            .get(EfinBudgetManencumlines.class);
        encumLines.setManualEncumbrance(encum);
        encumLines.setLineNo(lineno);
        encumLines.setAmount(amt);
        encumLines.setUsedAmount(BigDecimal.ZERO);
        encumLines.setRemainingAmount(amt);// ------
        encumLines.setAPPAmt(BigDecimal.ZERO);// ----------
        log4j.debug("setRemainingAmount:" + amt);
        encumLines.setRevamount(amt);
        encumLines.setOrganization(pInovice.getOrganization());
        encumLines.setSalesRegion(acct.getSalesRegion());
        encumLines.setAccountElement(acct.getAccount());
        encumLines.setSalesCampaign(acct.getSalesCampaign());
        encumLines.setProject(acct.getProject());
        encumLines.setActivity(acct.getActivity());
        encumLines.setStDimension(acct.getStDimension());
        encumLines.setNdDimension(acct.getNdDimension());
        encumLines.setBusinessPartner(acct.getBusinessPartner());
        encumLines.setAccountingCombination(acct);
        encumLines.setUniqueCodeName(acct.getEfinUniquecodename());
        encum.getEfinBudgetManencumlinesList().add(encumLines);
        OBDal.getInstance().save(encum);
        OBDal.getInstance().save(encumLines);
        // invLine.setEfinBudgmanuencumln(encumLines);
        // OBDal.getInstance().save(invLine);

        manEncumRev.setSRCManencumline(encumLines);
        OBDal.getInstance().save(manEncumRev);

        // update encumline ref in invoiceline
        OBQuery<InvoiceLine> invlinQry = OBDal.getInstance().createQuery(InvoiceLine.class,
            " as e where e.invoice.id='" + pInovice.getId() + "' and e.efinCValidcombination.id='"
                + acct.getId() + "'");
        if (invlinQry.list().size() > 0) {
          for (InvoiceLine line : invlinQry.list()) {
            line.setEfinBudgmanuencumln(encumLines);
            OBDal.getInstance().save(line);
          }
        }
      }
      encum.setDocumentStatus("CO");
      OBDal.getInstance().save(encum);

      // change invoice encum fields based on newly created encum.
      encumid = encum.getId();
      EfinBudgetManencumv manenc = OBDal.getInstance().get(EfinBudgetManencumv.class, encumid);
      pInovice.setEfinManualencumbrance(manenc);
      pInovice.setEfinEncumtype(manenc.getEncumbranceMethod());
      pInovice.setEfinEncumbranceType(manenc.getEncumbranceType());
      OBDal.getInstance().save(pInovice);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      log4j.error("Exception in split encum of POM :", e);
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(OBMessageUtils.messageBD("HB_INTERNAL_ERROR"));
    }
    return encumid;
  }

  /**
   * To insert modification for old encum for POM
   * 
   * @param acctCode
   * @param amt
   * @param po
   *          encum
   * @return
   */
  public static EfinBudManencumRev insertModifiactionPOM(AccountingCombination acctCode,
      BigDecimal amt, EfinBudgetManencum encum) {
    EfinBudManencumRev manEncumRev = null;
    try {
      OBContext.setAdminMode();
      OBQuery<EfinBudgetManencumlines> oldencumlineexist = OBDal.getInstance()
          .createQuery(EfinBudgetManencumlines.class, "as e where e.manualEncumbrance.id='"
              + encum.getId() + "' and e.accountingCombination.id ='" + acctCode.getId() + "'");
      if (oldencumlineexist.list() != null && oldencumlineexist.list().size() > 0) {
        EfinBudgetManencumlines encumLines = oldencumlineexist.list().get(0);

        OBDal.getInstance().refresh(encumLines);

        encumLines.setAPPAmt(encumLines.getAPPAmt().subtract(amt));
        OBDal.getInstance().save(encumLines);
        OBDal.getInstance().flush();

        manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
        manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
        manEncumRev.setOrganization(
            OBDal.getInstance().get(Organization.class, encumLines.getOrganization().getId()));
        manEncumRev.setActive(true);
        manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
        manEncumRev.setCreationDate(new java.util.Date());
        manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
        manEncumRev.setUpdated(new java.util.Date());
        manEncumRev.setUniqueCode(encumLines.getUniquecode());
        manEncumRev.setManualEncumbranceLines(encumLines);
        manEncumRev.setRevdate(new java.util.Date());
        manEncumRev.setStatus("APP");
        manEncumRev.setAuto(true);
        manEncumRev.setSystem(true);
        manEncumRev.setRevamount(amt.negate());
        log4j.debug("rev:" + manEncumRev.getRevamount());
        manEncumRev.setAccountingCombination(encumLines.getAccountingCombination());
        // manEncumRev.setSRCManencumline(encumLines);
        manEncumRev.setEncumbranceType("AEE");
        OBDal.getInstance().save(manEncumRev);
        OBDal.getInstance().flush();
        return manEncumRev;
      }

    } catch (Exception e) {
      log4j.error(" Exception while insert modification for POM old encum: " + e);
      throw new OBException(e.getMessage());
    } finally {

    }
    return manEncumRev;
  }

  /**
   * This method is used to insert invoice in encumbrance
   * 
   * @param invoiceLine
   * @param encumbranceLine
   * @param strInvoiceType
   * @param conversionrate
   */
  public static void InsertInvoicesInEncumbrance(InvoiceLine invoiceLine,
      EfinBudgetManencumlines encumbranceLine, String strInvoiceType, BigDecimal conversionrate) {
    InsertInvoicesInEncumbrance(invoiceLine, encumbranceLine, strInvoiceType, conversionrate,
        invoiceLine.getInvoice(), invoiceLine.getLineNetAmount());
  }

  /**
   * This method is used to used to check remaining amount for tax encumbrance
   * 
   * @param rdvInvoice
   * @param taxLinesMap
   * @param isRdvInvoice
   * @return
   */
  public static Boolean checkRemainingAmountForTaxEncumbrance(Invoice rdvInvoice,
      Map<String, BigDecimal> taxLinesMap, Boolean isRdvInvoice, Boolean isUpdation) {
    Boolean hasRemainingAmount = Boolean.TRUE;
    Integer precision = 2;
    try {
      EfinBudgetManencum poEncumbrance = null;
      String strUniquecodeId = "";
      BigDecimal remainingAmount = BigDecimal.ZERO, taxAmount = BigDecimal.ZERO;
      Currency currency = rdvInvoice.getClient().getCurrency();
      Boolean isStage = false;
      if (currency != null) {
        precision = currency.getPricePrecision().intValue();
      }

      if (isRdvInvoice)
        poEncumbrance = getPoEncumbranceFromInvoice(rdvInvoice);
      else {
        Order order = Utility.getObject(Order.class, rdvInvoice.getEfinCOrder().getId());
        if (order != null) {
          poEncumbrance = order.getEfinBudgetManencum();
        }
      }

      if (isRdvInvoice && (rdvInvoice.getEfinManualencumbrance().getDocumentNo()
          .equals(poEncumbrance.getDocumentNo()))) {
        isStage = true;
      }
      // This block is for RDV Invoice
      if (poEncumbrance != null && isRdvInvoice) {

        // if (poEncumbrance.getAppliedAmount().compareTo(invoiceTaxAmount) < 0) {
        // hasRemainingAmount = Boolean.FALSE;
        // } else {
        for (EfinBudgetManencumlines encumbranceLine : poEncumbrance
            .getEfinBudgetManencumlinesList()) {
          strUniquecodeId = encumbranceLine.getAccountingCombination().getId();

          if (taxLinesMap.containsKey(strUniquecodeId)) {
            // check with remaining amt also in case of manual
            if (poEncumbrance.getEncumMethod().equals("M")) {

              if (!isStage) {
                // remainingAmount = encumbranceLine.getAPPAmt()
                // .add(encumbranceLine.getRemainingAmount());
                remainingAmount = encumbranceLine.getAPPAmt();
              } else {
                remainingAmount = encumbranceLine.getRemainingAmount();
              }
            }

            else
              remainingAmount = encumbranceLine.getAPPAmt();

            taxAmount = taxLinesMap.get(strUniquecodeId).setScale(precision, RoundingMode.HALF_UP);

            // Added Auto encumbrance also in this condition because as per Majed mail for auto
            // encumbrance we should not take amount from budget enquiry without budget manger
            // knowledge
            if (poEncumbrance.getEncumMethod().equals("M")
                || poEncumbrance.getEncumMethod().equals("A")) {
              if (taxAmount.compareTo(remainingAmount) > 0) {
                hasRemainingAmount = Boolean.FALSE;
              } else {
                // if applied amt not enough to consume the tax amt then while validation itself
                // check app+remaining amt is enough , if enough then how much we need from
                // remaining add that amt in app amt
                if (taxAmount.compareTo(encumbranceLine.getAPPAmt()) > 0 && !isStage
                    && isUpdation) {
                  BigDecimal deductedAmt = taxAmount.subtract(encumbranceLine.getAPPAmt());
                  if (encumbranceLine.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                      && deductedAmt.compareTo(BigDecimal.ZERO) > 0) {
                    encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(deductedAmt));
                    encumbranceLine.setRemainingAmount(
                        encumbranceLine.getRemainingAmount().subtract(deductedAmt));
                    OBDal.getInstance().save(encumbranceLine);
                    OBDal.getInstance().flush();
                  }
                }
                if (isStage && isUpdation) {
                  BigDecimal deductedAmt = taxAmount;
                  if (encumbranceLine.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                      && deductedAmt.compareTo(BigDecimal.ZERO) > 0) {
                    encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(deductedAmt));
                    encumbranceLine.setRemainingAmount(
                        encumbranceLine.getRemainingAmount().subtract(deductedAmt));
                    OBDal.getInstance().save(encumbranceLine);
                    OBDal.getInstance().flush();
                  }
                }
              }
            } else {
              if (taxAmount.compareTo(remainingAmount) > 0) {
                EfinBudgetInquiry budgetinq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                    encumbranceLine.getAccountingCombination(), rdvInvoice.getEfinBudgetint());

                if (budgetinq != null) {
                  if (budgetinq.getFundsAvailable().compareTo(taxAmount) < 0) {
                    hasRemainingAmount = Boolean.FALSE;
                  }
                } else {
                  hasRemainingAmount = Boolean.FALSE;
                }
              }
            }
            if (!hasRemainingAmount) {
              return hasRemainingAmount;
            }
          }
        }
      }

      // this is for PO Match valiation
      if (poEncumbrance != null && !isRdvInvoice) {

        for (EfinBudgetManencumlines encumbranceLine : poEncumbrance
            .getEfinBudgetManencumlinesList()) {
          strUniquecodeId = encumbranceLine.getAccountingCombination().getId();

          BigDecimal sumLineNetAmt = rdvInvoice.getInvoiceLineList().stream()
              .filter(a -> a.getEfinCValidcombination().getId()
                  .equals(encumbranceLine.getAccountingCombination().getId()))
              .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          if (taxLinesMap.containsKey(strUniquecodeId)) {

            // remainingAmount =
            // encumbranceLine.getAPPAmt().add(encumbranceLine.getRemainingAmount());
            remainingAmount = encumbranceLine.getAPPAmt();

            // Added Auto encumbrance also in this condition because as per Majed mail for auto
            // encumbrance we should not take amount from budget enquiry without budget manger
            // knowledge
            if (poEncumbrance.getEncumMethod().equals("M")
                || poEncumbrance.getEncumMethod().equals("A")) {
              if (sumLineNetAmt.compareTo(remainingAmount) > 0) {
                hasRemainingAmount = Boolean.FALSE;
              } else {
                // if applied amt not enough to consume the invoice amt then while validation itself
                // check app+remaining amt is enough , if enough then how much we need from
                // remaining add that amt in app amt
                if ((sumLineNetAmt.compareTo(encumbranceLine.getAPPAmt()) > 0) && isUpdation) {
                  BigDecimal deductedAmt = sumLineNetAmt.subtract(encumbranceLine.getAPPAmt());
                  if (encumbranceLine.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                      && deductedAmt.compareTo(BigDecimal.ZERO) > 0) {
                    encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(deductedAmt));
                    encumbranceLine.setRemainingAmount(
                        encumbranceLine.getRemainingAmount().subtract(deductedAmt));
                    OBDal.getInstance().save(encumbranceLine);
                    OBDal.getInstance().flush();
                  }
                }
              }
            } else {
              if (sumLineNetAmt.compareTo(remainingAmount) > 0) {
                EfinBudgetInquiry budgetinq = PurchaseInvoiceSubmitUtils.getBudgetInquiry(
                    encumbranceLine.getAccountingCombination(), rdvInvoice.getEfinBudgetint());
                // sumLineNetAmt.subtract(remainingAmount) amt only need to check in Budget inquiry
                // for po match, because some
                // amt(remaining amt-PoAppAmt) is already encumbred in po
                if (budgetinq != null) {
                  if (budgetinq.getFundsAvailable()
                      .compareTo((sumLineNetAmt.subtract(remainingAmount))) < 0) {
                    hasRemainingAmount = Boolean.FALSE;
                  }
                } else {
                  hasRemainingAmount = Boolean.FALSE;
                }

              }

            }
            if (!hasRemainingAmount) {
              return hasRemainingAmount;
            }
          }
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while checkRemainingAmountForTaxEncumbrance(): " + e);
    }

    return hasRemainingAmount;
  }

  /**
   * check amt is enough to create po match invoice .
   * 
   * @param rdvInvoice
   * @param isUpdation
   * @return
   */
  public static Boolean checkRemainingAmountForPOMEncumbrance(Invoice rdvInvoice,
      Boolean isUpdation) {
    Boolean hasRemainingAmount = Boolean.TRUE;
    try {
      EfinBudgetManencum poEncumbrance = null;
      BigDecimal remainingAmount = BigDecimal.ZERO;

      Order order = Utility.getObject(Order.class, rdvInvoice.getEfinCOrder().getId());
      if (order != null) {
        poEncumbrance = order.getEfinBudgetManencum();
      }

      // this is for PO Match valiation
      if (poEncumbrance != null) {

        for (EfinBudgetManencumlines encumbranceLine : poEncumbrance
            .getEfinBudgetManencumlinesList()) {

          BigDecimal sumLineNetAmt = rdvInvoice.getInvoiceLineList().stream()
              .filter(a -> a.getEfinCValidcombination().getId()
                  .equals(encumbranceLine.getAccountingCombination().getId()))
              .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

          // remainingAmount =
          // encumbranceLine.getAPPAmt().add(encumbranceLine.getRemainingAmount());
          remainingAmount = encumbranceLine.getAPPAmt();

          if (sumLineNetAmt.compareTo(remainingAmount) > 0) {
            hasRemainingAmount = Boolean.FALSE;
          } else {
            if ((sumLineNetAmt.compareTo(encumbranceLine.getAPPAmt()) > 0) && isUpdation) {
              BigDecimal deductedAmt = sumLineNetAmt.subtract(encumbranceLine.getAPPAmt());
              if (encumbranceLine.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                  && deductedAmt.compareTo(BigDecimal.ZERO) > 0) {
                encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(deductedAmt));
                encumbranceLine
                    .setRemainingAmount(encumbranceLine.getRemainingAmount().subtract(deductedAmt));
                OBDal.getInstance().save(encumbranceLine);
                OBDal.getInstance().flush();
              }
            }
          }

          if (!hasRemainingAmount) {
            return hasRemainingAmount;
          }
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while checkRemainingAmountForTaxEncumbrance(): " + e);
    }

    return hasRemainingAmount;
  }

  /**
   * This method is used to get PO encumbrance frm invoice
   * 
   * @param invoice
   * @return
   */
  public static EfinBudgetManencum getPoEncumbranceFromInvoice(Invoice invoice) {
    EfinBudgetManencum poEncumbrance = null;

    try {
      poEncumbrance = invoice.getEfinRdvtxn().getEfinRdv().getSalesOrder().getEfinBudgetManencum();
    } catch (Exception e) {
      log4j.error(" Exception while getPoEncumbranceFromInvoice(): " + e);
    }
    return poEncumbrance;
  }

  /**
   * This method is used to add encumbrance modification
   * 
   * @param taxLinesMap
   * @param poEncumbrance
   * @param rdvInvoice
   * @param isRework
   */
  public static void addEncumbranceModification(Map<String, BigDecimal> taxLinesMap,
      EfinBudgetManencum poEncumbrance, Invoice rdvInvoice, Boolean isRework) {

    try {
      String strUniqueCodeId = "";
      BigDecimal taxAmount = BigDecimal.ZERO;
      Integer precision = 2;
      Currency currency = rdvInvoice.getClient().getCurrency();
      if (currency != null) {
        precision = currency.getPricePrecision().intValue();
      }

      for (Entry<String, BigDecimal> taxEntries : taxLinesMap.entrySet()) {
        strUniqueCodeId = taxEntries.getKey();
        taxAmount = taxEntries.getValue().setScale(precision, RoundingMode.HALF_UP);

        if (isRework) {
          if ((!rdvInvoice.getEfinManualencumbrance().getDocumentNo()
              .equals(poEncumbrance.getDocumentNo()))
              || ((rdvInvoice.getEfinManualencumbrance().getDocumentNo()
                  .equals(poEncumbrance.getDocumentNo()))
                  && rdvInvoice.getEfinManualencumbrance().getEncumbranceMethod().equals("A"))) {
            modifyInvoiceEncumbrance(strUniqueCodeId, taxAmount.negate(),
                rdvInvoice.getEfinManualencumbrance());
          }
          if (!rdvInvoice.getEfinManualencumbrance().getDocumentNo()
              .equals(poEncumbrance.getDocumentNo())) {
            modifyPOEncumbrance(strUniqueCodeId, taxAmount, poEncumbrance);
          }
        } else {
          if (!rdvInvoice.getEfinManualencumbrance().getDocumentNo()
              .equals(poEncumbrance.getDocumentNo())) {
            modifyPOEncumbrance(strUniqueCodeId, taxAmount.negate(), poEncumbrance);
          }
          if ((!rdvInvoice.getEfinManualencumbrance().getDocumentNo()
              .equals(poEncumbrance.getDocumentNo()))
              || ((rdvInvoice.getEfinManualencumbrance().getDocumentNo()
                  .equals(poEncumbrance.getDocumentNo()))
                  && rdvInvoice.getEfinManualencumbrance().getEncumbranceMethod().equals("A"))) {
            modifyInvoiceEncumbrance(strUniqueCodeId, taxAmount,
                rdvInvoice.getEfinManualencumbrance());
          }
        }

      }
    } catch (Exception e) {
      log4j.error(" Exception while addEncumbranceModification(): " + e);
    }
  }

  /**
   * Reduce the amount in po encumbrance where it took amount from budget enquiry while applied
   * external tax
   * 
   * @param poEncumbrance
   * @param rdvInvoice
   */
  public static void removePoExtraAmount(EfinBudgetManencum poEncumbrance, Invoice rdvInvoice,
      Boolean isDelete, AccountingCombination acct) {
    String sql = "";
    List<Object> uniqueCodeList = new ArrayList<Object>();
    try {
      sql = "select coalesce(sum(encumln.app_amt+encumln.used_amount),0),c_validcombination_id from efin_budget_manencumlines encumln "
          + " join efin_budget_manencum encum on encum.efin_budget_manencum_id = encumln.efin_budget_manencum_id "
          + " join c_invoice inv on inv.em_efin_manualencumbrance_id = encum.efin_budget_manencum_id "
          + " where inv.c_order_id in (select c_order_id from c_order where documentno =? and docstatus='CO') "
          + " and em_efin_invoicetype_txt='RDV' and inv.docstatus not in ('EFIN_CA')  ";

      if (acct != null) {
        sql += " and c_validcombination_id= ? ";
      }
      sql += "   group by c_validcombination_id  ";

      SQLQuery ps = OBDal.getInstance().getSession().createSQLQuery(sql);
      ps.setParameter(0, rdvInvoice.getSalesOrder().getDocumentNo());
      if (acct != null) {
        ps.setParameter(1, acct.getId());
      }
      if (ps != null) {
        uniqueCodeList = ps.list();
        if (uniqueCodeList.size() > 0) {
          for (Object uniqueCode : uniqueCodeList) {
            Object[] details = (Object[]) uniqueCode;
            OBQuery<EfinBudgetManencumlines> poencumLine = OBDal.getInstance().createQuery(
                EfinBudgetManencumlines.class,
                "manualEncumbrance.id=:poEncum and accountingCombination.id=:uniquCode");
            poencumLine.setNamedParameter("poEncum", poEncumbrance.getId());
            poencumLine.setNamedParameter("uniquCode", details[1].toString());
            if (poencumLine.list().size() > 0) {
              List<EfinBudgetManencumlines> poEncumLine = poencumLine.list();
              BigDecimal poUpdatedAMt = poEncumLine.get(0).getSystemUpdatedAmt();
              BigDecimal appliedAmt = new BigDecimal(details[0].toString());
              BigDecimal taxAmt = BigDecimal.ZERO;
              if (appliedAmt.compareTo(poUpdatedAMt) > 0) {

                if (!isDelete) {
                  taxAmt = rdvInvoice.getInvoiceLineList().stream()
                      .filter(
                          a -> a.getEfinCValidcombination().getId().equals(details[1].toString())
                              && a.isEFINIsTaxLine())
                      .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                  if (appliedAmt.subtract(poUpdatedAMt).compareTo(taxAmt) >= 0) {
                    PurchaseInvoiceSubmitUtils.modifyPOEncumbrance(details[1].toString(),
                        taxAmt.negate(), poEncumbrance);
                  }
                } else {
                  taxAmt = rdvInvoice.getInvoiceLineList().stream()
                      .filter(
                          a -> a.getEfinCValidcombination().getId().equals(details[1].toString()))
                      .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                  if (appliedAmt.subtract(poUpdatedAMt).compareTo(taxAmt) <= 0) {
                    PurchaseInvoiceSubmitUtils.modifyPOEncumbrance(details[1].toString(),
                        appliedAmt.subtract(poUpdatedAMt).negate(), poEncumbrance);
                  } else {
                    PurchaseInvoiceSubmitUtils.modifyPOEncumbrance(details[1].toString(),
                        taxAmt.negate(), poEncumbrance);
                  }
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error(" Exception while removePoExtraAmount(): " + e);
    }
  }

  /**
   * Reduce the amount in po encumbrance where it took amount from budget enquiry while applied
   * external tax
   * 
   * @param poEncumbrance
   * @param rdvInvoice
   */
  public static void removePoMatchExtraBudgetAmount(EfinBudgetManencum poEncumbrance,
      Invoice poMatchInvoice, AccountingCombination acct) {
    String sql = "";
    List<Object> uniqueCodeList = new ArrayList<Object>();
    try {
      sql = "select coalesce(sum(encumln.app_amt+encumln.used_amount+encumln.remaining_amount),0),c_validcombination_id from efin_budget_manencumlines encumln "
          + " join efin_budget_manencum encum on encum.efin_budget_manencum_id = encumln.efin_budget_manencum_id "
          + " join c_invoice inv on inv.em_efin_manualencumbrance_id = encum.efin_budget_manencum_id "
          + " where inv.em_efin_c_order_id in (select c_order_id from c_order where documentno =? and docstatus='CO') "
          + " and em_efin_invoicetype_txt='POM' and ( inv.docstatus not in ('EFIN_CA','DR')  or inv.c_invoice_id =? )  ";

      if (acct != null) {
        sql += " and c_validcombination_id= ? ";
      }
      sql += "   group by c_validcombination_id  ";
      SQLQuery ps = OBDal.getInstance().getSession().createSQLQuery(sql);
      ps.setParameter(0, poMatchInvoice.getEfinCOrder().getDocumentNo());
      ps.setParameter(1, poMatchInvoice.getId());
      if (acct != null) {
        ps.setParameter(2, acct.getId());
      }
      if (ps != null) {
        uniqueCodeList = ps.list();
        if (uniqueCodeList.size() > 0) {
          for (Object uniqueCode : uniqueCodeList) {
            Object[] details = (Object[]) uniqueCode;
            OBQuery<EfinBudgetManencumlines> poencumLine = OBDal.getInstance().createQuery(
                EfinBudgetManencumlines.class,
                "manualEncumbrance.id=:poEncum and accountingCombination.id=:uniquCode");
            poencumLine.setNamedParameter("poEncum", poEncumbrance.getId());
            poencumLine.setNamedParameter("uniquCode", details[1].toString());
            if (poencumLine.list().size() > 0) {
              List<EfinBudgetManencumlines> poEncumLine = poencumLine.list();
              BigDecimal poUpdatedAMt = poEncumLine.get(0).getSystemUpdatedAmt();
              BigDecimal appliedAmt = new BigDecimal(details[0].toString());
              BigDecimal lineNetAmt = BigDecimal.ZERO;
              BigDecimal decBudgAmt = BigDecimal.ZERO;
              if (appliedAmt.compareTo(poUpdatedAMt) > 0) {

                lineNetAmt = poMatchInvoice.getInvoiceLineList().stream()
                    .filter(a -> a.getEfinCValidcombination().getId().equals(details[1].toString()))
                    .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
                if (appliedAmt.subtract(poUpdatedAMt).compareTo(lineNetAmt) <= 0) {
                  decBudgAmt = appliedAmt.subtract(poUpdatedAMt);

                }
                if (appliedAmt.subtract(poUpdatedAMt).compareTo(lineNetAmt) > 0) {
                  decBudgAmt = lineNetAmt;
                }
                if (decBudgAmt.compareTo(BigDecimal.ZERO) > 0) {
                  PurchaseInvoiceSubmitUtils.modifyPOEncumbrance(details[1].toString(),
                      decBudgAmt.negate(), poEncumbrance);
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      log4j.error(" Exception while removePoMatchExtraBudgetAmount(): " + e);
    }
  }

  /**
   * This method is used to modify PO encumbrance
   * 
   * @param strUniqueCodeId
   * @param taxAmount
   * @param poEncumbrance
   */
  public static void modifyPOEncumbrance(String strUniqueCodeId, BigDecimal taxAmount,
      EfinBudgetManencum poEncumbrance) {
    try {
      EfinBudgetManencumlines encumbranceLine = getEncumbranceLine(poEncumbrance.getId(),
          strUniqueCodeId);
      if (encumbranceLine != null) {

        encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(taxAmount));
        OBDal.getInstance().save(encumbranceLine);

        insertModification(encumbranceLine, taxAmount);
      }
    } catch (Exception e) {
      log4j.error(" Exception while modifyPOEncumbrance(): " + e);
    }
  }

  /**
   * This method is used to modify PO encumbrance In Rdv Exclusive tax case.
   * 
   * @param strUniqueCodeId
   * @param taxAmount
   * @param poEncumbrance
   */
  public static void modifyPOEncumbranceRdvExclusiveTax(String strUniqueCodeId,
      BigDecimal taxAmount, EfinBudgetManencum poEncumbrance) {
    try {
      EfinBudgetManencumlines encumbranceLine = getEncumbranceLine(poEncumbrance.getId(),
          strUniqueCodeId);

      if (encumbranceLine != null) {
        encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(taxAmount));
        OBDal.getInstance().save(encumbranceLine);

        insertModification(encumbranceLine, taxAmount);

      }
    } catch (Exception e) {
      log4j.error(" Exception while modifyPOEncumbrance(): " + e);
    }
  }

  /**
   * This method is used to modify invoice encumbrance
   * 
   * @param strUniqueCodeId
   * @param taxAmount
   * @param efinManualencumbrance
   */
  private static void modifyInvoiceEncumbrance(String strUniqueCodeId, BigDecimal taxAmount,
      EfinBudgetManencumv efinManualencumbrance) {
    try {

      EfinBudgetManencumlines encumbranceLine = getEncumbranceLine(efinManualencumbrance.getId(),
          strUniqueCodeId);

      if (encumbranceLine != null) {
        encumbranceLine.setAPPAmt(encumbranceLine.getAPPAmt().add(taxAmount));
        OBDal.getInstance().save(encumbranceLine);

        insertModification(encumbranceLine, taxAmount);
      }
    } catch (Exception e) {
      log4j.error(" Exception while modifyInvoiceEncumbrance(): " + e);
    }
  }

  /**
   * THis method is used to insert modification
   * 
   * @param encumbranceLine
   * @param taxAmount
   */
  public static void insertModification(EfinBudgetManencumlines encumbranceLine,
      BigDecimal taxAmount) {
    try {
      EfinBudManencumRev manEncumRev = null;

      manEncumRev = OBProvider.getInstance().get(EfinBudManencumRev.class);
      manEncumRev.setClient(OBContext.getOBContext().getCurrentClient());
      manEncumRev.setOrganization(encumbranceLine.getOrganization());
      manEncumRev.setActive(true);
      manEncumRev.setUpdatedBy(OBContext.getOBContext().getUser());
      manEncumRev.setCreationDate(new java.util.Date());
      manEncumRev.setCreatedBy(OBContext.getOBContext().getUser());
      manEncumRev.setUpdated(new java.util.Date());
      manEncumRev.setUniqueCode(encumbranceLine.getUniquecode());
      manEncumRev.setManualEncumbranceLines(encumbranceLine);
      manEncumRev.setRevdate(new java.util.Date());
      manEncumRev.setStatus(STATUS_APPROVED);
      manEncumRev.setAuto(Boolean.TRUE);
      manEncumRev.setSystem(true);
      manEncumRev.setRevamount(taxAmount);
      manEncumRev.setAccountingCombination(encumbranceLine.getAccountingCombination());
      // manEncumRev.setSRCManencumline(encumLines);
      manEncumRev.setEncumbranceType("AEE");

      OBDal.getInstance().save(manEncumRev);

    } catch (Exception e) {
      log4j.error(" Exception while insertModification(): " + e);
    }
  }

  /**
   * This method is used to add encumbrance modification
   * 
   * @param taxLinesMap
   * @param poEncumbrance
   * @param rdvInvoice
   */
  public static void addEncumbranceModification(Map<String, BigDecimal> taxLinesMap,
      EfinBudgetManencum poEncumbrance, Invoice rdvInvoice) {
    addEncumbranceModification(taxLinesMap, poEncumbrance, rdvInvoice, Boolean.FALSE);
  }

  /**
   * This method is used to get invoice line
   * 
   * @param poInvoice
   * @return
   */
  public static Map<String, BigDecimal> getInvoiceLines(Invoice poInvoice) {
    Map<String, BigDecimal> combinations = new HashMap<String, BigDecimal>();
    AccountingCombination uniqueCode = null;
    try {
      for (InvoiceLine invoiceLine : poInvoice.getInvoiceLineList()) {
        uniqueCode = invoiceLine.getEfinCValidcombination();

        if (uniqueCode != null) {
          if (combinations.containsKey(uniqueCode.getId())) {
            combinations.put(uniqueCode.getId(),
                combinations.get(uniqueCode.getId()).add(invoiceLine.getLineNetAmount()));
          } else {
            combinations.put(uniqueCode.getId(), invoiceLine.getLineNetAmount());
          }
        }
      }
    } catch (Exception e) {
      log4j.error(" Exception while getInvoiceLines(): " + e);
    }
    return combinations;
  }

  /**
   * This method is used to add applied from remaining amount
   * 
   * @param poInvoice
   * @param taxLinesMap
   */
  public static void addAppliedFromRemainingAmount(Invoice poInvoice,
      Map<String, BigDecimal> taxLinesMap) {
    try {

      EfinBudgetManencum poEncumbrance = Utility.getObject(EfinBudgetManencum.class,
          poInvoice.getEfinManualencumbrance().getManualEncumbrance());

      if (poEncumbrance != null) {
        for (EfinBudgetManencumlines line : poEncumbrance.getEfinBudgetManencumlinesList()) {
          OBDal.getInstance().save(line);
        }
        OBDal.getInstance().flush();

      }
    } catch (Exception e) {
      log4j.error(" Exception while addAppliedFromRemainingAmount(): " + e);
    }
  }

  /**
   * This method is used to check whether grouping of line based on uniquecode(tax lines + other
   * lines) and check whether the sum of this amount is lesser than or equal to the applied amount
   * in encumbrance line
   * 
   * If sum of line net amount is greater than applied amount then we have to put
   * modification(increase) in Po encumbrance from budget enquiry
   * 
   * This validation is only for auto encumbrance and only in case of PO Match
   * 
   * @param Invoice
   * 
   * @return void
   */
  public static void insertModificationForAutoInPoMatch(Invoice rdvInvoice) {

    try {
      BigDecimal remainingAmount = BigDecimal.ZERO;
      EfinBudgetManencum poEncumbrance = Utility.getObject(EfinBudgetManencum.class,
          rdvInvoice.getEfinManualencumbrance().getManualEncumbrance());

      for (EfinBudgetManencumlines encumbranceLine : poEncumbrance
          .getEfinBudgetManencumlinesList()) {

        BigDecimal sumLineNetAmt = rdvInvoice.getInvoiceLineList().stream()
            .filter(a -> a.getEfinCValidcombination().getId()
                .equals(encumbranceLine.getAccountingCombination().getId()))
            .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        remainingAmount = encumbranceLine.getAPPAmt();

        if (sumLineNetAmt.compareTo(remainingAmount) > 0) {
          modifyPOEncumbrance(encumbranceLine.getAccountingCombination().getId(),
              sumLineNetAmt.subtract(remainingAmount), poEncumbrance);
          OBDal.getInstance().flush();
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while insertModificationForAutoInPoMatch(): " + e);
    }

  }

  /**
   * This methos id used to add extra tax amount in invoice encumbrance(Auto) in RDV Invoice case.
   * 
   * @param rdvInvoice
   */
  public static void insertModificationForAutoInRdvInvoiceTax(Invoice rdvInvoice) {

    try {
      BigDecimal remainingAmount = BigDecimal.ZERO;

      EfinBudgetManencum poEncumbrance = PurchaseInvoiceSubmitUtils
          .getPoEncumbranceFromInvoice(rdvInvoice);

      for (EfinBudgetManencumlines encumbranceLine : poEncumbrance
          .getEfinBudgetManencumlinesList()) {

        BigDecimal sumLineNetAmt = rdvInvoice.getInvoiceLineList().stream()
            .filter(a -> a.isEFINIsTaxLine() && a.getEfinCValidcombination().getId()
                .equals(encumbranceLine.getAccountingCombination().getId()))
            .map(a -> a.getLineNetAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        remainingAmount = encumbranceLine.getAPPAmt();

        if (sumLineNetAmt.compareTo(remainingAmount) > 0) {
          modifyPOEncumbranceRdvExclusiveTax(encumbranceLine.getAccountingCombination().getId(),
              sumLineNetAmt.subtract(remainingAmount), poEncumbrance);
          OBDal.getInstance().flush();
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while insertModificationForAutoInRdvInvoiceTax(): " + e);
    }

  }

  /**
   * THis method is used to get latest order
   * 
   * @param order
   * @return
   */
  public static Order getLatestOrder(Order order) {
    Order latestOrder = order;
    List<Order> orderlist = null;
    try {

      if (order != null) {
        OBQuery<Order> getLatestQry = OBDal.getInstance().createQuery(Order.class,
            " as e where (e.escmBaseOrder.id=:OrderId  or e.id=:OrderId)  order by escmRevision desc");

        if (order.getEscmBaseOrder() != null) {
          getLatestQry.setNamedParameter("OrderId", order.getEscmBaseOrder().getId());
        } else {
          getLatestQry.setNamedParameter("OrderId", order.getId());
        }
        getLatestQry.setMaxResult(1);
        orderlist = getLatestQry.list();
        if (orderlist.size() > 0) {
          latestOrder = orderlist.get(0);
          return latestOrder;
        } else {
          return latestOrder;
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while getLatestOrder(): " + e);
    }
    return latestOrder;

  }

  /**
   * THis method is used to get latest order
   * 
   * @param order
   * @return
   */
  public static Order getLatestOrderComplete(Order order) {
    Order latestOrder = order;
    List<Order> orderlist = null;
    try {

      if (order != null) {
        OBQuery<Order> getLatestQry = OBDal.getInstance().createQuery(Order.class,
            " as e where (e.escmBaseOrder.id=:OrderId  or e.id=:OrderId) and e.escmAppstatus='ESCM_AP' order by escmRevision desc");

        if (order.getEscmBaseOrder() != null) {
          getLatestQry.setNamedParameter("OrderId", order.getEscmBaseOrder().getId());
        } else {
          getLatestQry.setNamedParameter("OrderId", order.getId());
        }
        getLatestQry.setMaxResult(1);
        orderlist = getLatestQry.list();
        if (orderlist.size() > 0) {
          latestOrder = orderlist.get(0);
          return latestOrder;
        } else {
          return latestOrder;
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while getLatestOrder(): " + e);
    }
    return latestOrder;

  }

  /**
   * This method is used to get later version orderline list based on the order and orderline line
   * no.
   * 
   * @param order
   * @param ordLine
   * @return orderlinelist
   */
  public static List<OrderLine> getGreaterRevisionOrdLineList(Order order, OrderLine ordLine) {
    List<OrderLine> orderlinelist = null;
    try {
      if (order != null) {
        long lineNo = ordLine.getLineNo();
        OBQuery<OrderLine> getLatestQry = OBDal.getInstance().createQuery(OrderLine.class, " "
            + " as e where e.salesOrder.id in (select ord.id from Order as ord where (ord.escmBaseOrder.id=:OrderId  or ord.id=:OrderId) "
            + " and ord.escmRevision >=:ordrevision) and e.lineNo=:lineNo");
        getLatestQry.setNamedParameter("OrderId", order.getId());
        getLatestQry.setNamedParameter("ordrevision", order.getEscmRevision());
        getLatestQry.setNamedParameter("lineNo", lineNo);
        getLatestQry.setFilterOnActive(false);
        orderlinelist = getLatestQry.list();

      }
    } catch (Exception e) {
      log4j.error(" Exception while getGreaterRevisionOrdLineList(): " + e);
    }
    return orderlinelist;

  }

  public static List<Order> getGreaterRevisionOrdList(Order order) {
    List<Order> orderlist = null;
    try {

      if (order != null) {

        OBQuery<Order> getLatestQry = OBDal.getInstance().createQuery(Order.class,
            " as e where (e.escmBaseOrder.id=:OrderId  or e.id=:OrderId) and e.escmRevision >=:ordrevision order by escmRevision asc");

        if (order.getEscmBaseOrder() != null) {
          getLatestQry.setNamedParameter("OrderId", order.getEscmBaseOrder().getId());
        } else {
          getLatestQry.setNamedParameter("OrderId", order.getId());
        }
        getLatestQry.setNamedParameter("ordrevision", order.getEscmRevision());
        getLatestQry.setFilterOnActive(false);
        orderlist = getLatestQry.list();

      }

    } catch (Exception e) {
      log4j.error(" Exception while getGreaterRevisionOrdList(): " + e);
    }
    return orderlist;

  }

  public static boolean getOrderHaveTax(Invoice invoiceObj, String strInvoiceType) {
    Boolean isOrderHaveTax = false;
    Order order = null;
    try {

      if (invoiceObj != null) {
        if (RDV_DOCUMENT.equals(strInvoiceType)) {
          order = getLatestOrderComplete(invoiceObj.getSalesOrder());
          if (order != null) {
            isOrderHaveTax = order.isEscmIstax();
          }
        } else {
          isOrderHaveTax = false;
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while getOrderHaveTax(): " + e);
    }
    return isOrderHaveTax;

  }

  /**
   * Check whether Funds budget is defined and Funds available is greater than the line net amount
   * for the lines
   * 
   * @param fromRole
   * @param delegatedFromRole
   * @param hasDelegation
   * @param doctype
   * @param invoice
   * @param DocType
   * @param strInvoiceType
   * @param conversionrate
   *
   */
  public static void preValidation(String fromRole, String delegatedFromRole, boolean hasDelegation,
      String doctype, Invoice inv, String strInvoiceType, BigDecimal conversionrate,
      NextRoleByRuleVO nextApproval) {

    boolean reserve = false, hasNoFundsEncumbrance = false;
    Boolean isValidCombination = Boolean.TRUE;
    EfinBudgetManencumv encumId = null;
    boolean isTemEncumCreated = false;

    Campaign budgetType = PurchaseInvoiceSubmitUtils.getBudgetType(inv.getEfinBudgetType());

    if (inv.getEfinManualencumbrance() != null) {
      encumId = inv.getEfinManualencumbrance();
    }

    reserve = UtilityDAO.getReserveFundsRole(doctype, fromRole, inv.getOrganization().getId(),
        inv.getId(), inv.getGrandTotalAmount());

    if (hasDelegation && !reserve) {
      reserve = UtilityDAO.getReserveFundsRole(doctype, delegatedFromRole,
          inv.getOrganization().getId(), inv.getId(), inv.getGrandTotalAmount());
    }

    if (reserve && !inv.isEfinIsreserved()) {

      if (PO_DOCUMENT.equals(strInvoiceType)) {

        // validation need to rewrite, if cost then check for funds alone, if funds no need
        if (inv.getEfinBudgetType().equals("C")) {
          isValidCombination = PurchaseInvoiceSubmitUtils.preReservationFundsChecking(inv,
              conversionrate);
          if (!isValidCombination) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Reservation_Nofunds"));
          }
        }
      }

      if (((inv.getEfinEncumtype().equals("A") && API_DOCUMENT.equals(strInvoiceType))
          || RDV_DOCUMENT.equals(strInvoiceType)) && encumId == null) {

        if (budgetType.getEfinBudgettype().equals("C") || (RDV_DOCUMENT.equals(strInvoiceType))) {
          isValidCombination = PurchaseInvoiceSubmitUtils.preReservationFundsChecking(inv,
              conversionrate);

          if (!isValidCombination) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Reservation_Nofunds"));
          }
        }

      }

      if ((inv.getEfinEncumtype().equals("M")
          && (PPA_DOCUMENT.equals(strInvoiceType) | API_DOCUMENT.equals(strInvoiceType)))
          || (RDV_DOCUMENT.equals(strInvoiceType) && encumId != null)) {

        if (RDV_DOCUMENT.equals(strInvoiceType)) {
          isTemEncumCreated = chkTemEncumbranceCreatedForRdvInvoice(inv);
        }

        if (API_DOCUMENT.equals(strInvoiceType)
            || (RDV_DOCUMENT.equals(strInvoiceType) && !isTemEncumCreated)) {

          isValidCombination = PurchaseInvoiceSubmitUtils.preReservationFundsChecking(inv,
              conversionrate);
          if (!isValidCombination) {
            throw new OBException(OBMessageUtils.messageBD("Efin_Reservation_Nofunds"));
          }
        }
      }
    }

    // Check funds encumbrance is created at final level approval
    if (!reserve && nextApproval != null && nextApproval.getNextRoleId() == null
        && nextApproval.getErrorMsg() == null) {

      if (inv.getEfinBudgetType() != null && inv.getEfinBudgetType().equals("F")
          && inv.getEfinManualencumbrance() == null) {
        hasNoFundsEncumbrance = true;
      } else if (inv.getEfinBudgetType() != null && inv.getEfinBudgetType().equals("C")
          && inv.getEfinFundsEncumbrance() == null) {
        hasNoFundsEncumbrance = true;
      }

      List<InvoiceLine> line = inv.getInvoiceLineList().stream()
          .filter(a -> a.getEfinCValidcombination().getEfinDimensiontype().equals("E"))
          .collect(Collectors.toList());

      if (line.size() > 0) {
        if (hasNoFundsEncumbrance) {
          throw new OBException(OBMessageUtils.messageBD("Efin_NoFundsEncumbrance"));
        }
      }
    }
  }

  /**
   * Check encumbrance is having remaining amount and unused unique codes
   * 
   * @param orderId
   * @return isSplitEncumbrance
   * 
   */
  public static boolean checkSplitEncumbrance(String orderId) {
    boolean isSplitEncumbrance = false;
    ArrayList<String> encumUniqueCodeList = new ArrayList<String>();

    Order order = OBDal.getInstance().get(Order.class, orderId);

    if (order.getEfinBudgetManencum() != null) {

      EfinBudgetManencum encumbrance = order.getEfinBudgetManencum();

      List<EfinBudgetManencumlines> encumLinesList = encumbrance.getEfinBudgetManencumlinesList();

      for (EfinBudgetManencumlines encumLines : encumLinesList) {
        encumUniqueCodeList.add(encumLines.getAccountingCombination().getId());
      }

      for (OrderLine orderLines : order.getOrderLineList()) {
        if (!orderLines.isEscmIssummarylevel()) {
          if (encumUniqueCodeList.stream()
              .filter(a -> a.equals(orderLines.getEFINUniqueCode().getId()))
              .collect(Collectors.toList()).size() == 0) {
            // if (!encumUniqueCode.contains(orderLines.getEFINUniqueCode().getId())) {
            isSplitEncumbrance = true;
            return isSplitEncumbrance;
          }
        }
      }

      for (EfinBudgetManencumlines encumLines : encumLinesList) {
        if (encumLines.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
          isSplitEncumbrance = true;
          return isSplitEncumbrance;
        }
      }
    }
    return isSplitEncumbrance;
  }

  /**
   * Check whether invoice encumbrance is different from order encumbrance
   * 
   * @param invoiceEncumId
   * @param orderId
   * @return isEncumbranceDifferent
   */
  public static boolean isEncumbranceDifferent(String invoiceEncumId, String orderId) {

    boolean isEncumbranceDifferent = false;

    Order order = OBDal.getInstance().get(Order.class, orderId);

    if (order.getEfinBudgetManencum() != null) {
      if (!invoiceEncumId.equals(order.getEfinBudgetManencum().getId())) {
        isEncumbranceDifferent = true;
      }
    }
    return isEncumbranceDifferent;
  }

  public static boolean chkTemEncumbranceCreatedForRdvInvoice(Invoice invoiceObj) {
    List<EfinRdvHoldAction> holdRelease = new ArrayList<EfinRdvHoldAction>();
    boolean isTemEncumCreated = false;
    boolean isAmtMatch = true;
    try {
      if (invoiceObj.getEfinRdvtxn() != null) {
        invoiceObj.getEfinRdvtxn().getEfinRDVTxnlineList().stream().filter(y -> !y.isSummaryLevel())
            .collect(Collectors.toList()).forEach(ln -> {

              holdRelease.addAll(ln.getEfinRdvHoldActionList().stream()
                  .filter(x -> x.getEfinBudgetTransfertrxline() != null)
                  .collect(Collectors.toList()));

            });

        if (holdRelease.size() > 0) {
          HashMap<AccountingCombination, BigDecimal> accountValueMap = new HashMap<AccountingCombination, BigDecimal>();
          for (EfinRdvHoldAction release : holdRelease) {
            AccountingCombination account = release.getEfinRdvtxnline().getAccountingCombination();
            BigDecimal relamount = release.getRDVHoldAmount().negate();

            if (accountValueMap.containsKey(account)) {
              BigDecimal amount = accountValueMap.get(account).add(relamount);
              accountValueMap.put(account, amount);
            } else {
              accountValueMap.put(account, relamount);
            }
          }

          for (EfinRdvHoldAction release : holdRelease) {
            EfinBudgetManencum encum = release.getEfinBudgetTransfertrxline()
                .getEfinBudgetTransfertrx().getManualEncumbrance();
            List<EfinBudgetManencumlines> encumInvalidLine = encum.getEfinBudgetManencumlinesList()
                .stream()
                .filter(x -> accountValueMap.containsKey(x.getAccountingCombination())
                    && x.getRevamount()
                        .compareTo(accountValueMap.get(x.getAccountingCombination())) < 0)
                .collect(Collectors.toList());
            if (encumInvalidLine.size() > 0) {
              isAmtMatch = false;
              break;
            }
          }
          if (isAmtMatch) {
            isTemEncumCreated = true;
          }
        }
      }

    } catch (Exception e) {
      log4j.error(" Exception while chkTemEncumbranceCreatedForRdvInvoice(): " + e);
    }
    return isTemEncumCreated;
  }

  public static String getBaseDesignPath() {
    ServletContext servletContext = DalContextListener.getServletContext();
    ConfigParameters configParameters = ConfigParameters.retrieveFrom(servletContext);

    String base = configParameters.strBaseDesignPath;
    String design = configParameters.strDefaultDesignPath;

    if (!base.startsWith("/")) {
      base = "/" + base;
    }
    return servletContext.getRealPath(base + "/" + design);
  }

  /**
   * This method is used to create report while submitting the invoice
   * 
   * @param vars
   * @param header
   * @param conn
   * @return CreateAttachmentResponseGRP
   */

  public static CreateAttachmentResponseGRP createReportAndSendToDMS(VariablesSecureApp vars,
      Invoice header, ConnectionProvider conn, DMSIntegrationLog log, DMSXmlAttributes attributes,
      String documentno) {
    GRPDmsInterface dmsGRP = new GRPDmsImplementation();
    CreateAttachmentResponseGRP response = null;
    DMSIntegrationLog dmsLog = log;
    try {
      String profileURI = GetServiceAccount.getProperty(DMSConstants.DMS_PROFILE_URI);
      String nodeID = GetServiceAccount.getProperty(DMSConstants.DMS_NODE_ID);
      Boolean isPrepaymentApp = header.getDocumentType().isEfinIsprepayinvapp() != null
          && header.getDocumentType().isEfinIsprepayinvapp() ? true : false;

      HashMap<String, Object> designParameters = new HashMap<String, Object>();
      HashMap<Object, Object> exportParameters = new HashMap<Object, Object>();
      String strOutput = "pdf", responseData = "";
      String strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/InvoiceReport/APInvoiceReport.jrxml";

      if (isPrepaymentApp) {
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/InvoiceReport/GLAdjustment.jrxml";
      } else {
        strReportName = "@basedesign@/sa/elm/ob/finance/ad_reports/InvoiceReport/APInvoiceReport.jrxml";
      }

      final ExportType expType = ExportType.getExportType(strOutput.toUpperCase());
      String strBaseDesign = PurchaseInvoiceSubmitUtils.getBaseDesignPath();
      strReportName = Replace.replace(strReportName, "@basedesign@", strBaseDesign);
      String directory = OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("attach.path");
      String filePathWithName = directory + "/" + "Invoice" + "_" + (header.getDocumentNo()) + "."
          + strOutput;

      designParameters.put("DOCUMENT_ID", header.getId());
      designParameters.put("BASE_DESIGN", strBaseDesign);

      File outputFile = new File(filePathWithName);

      ReportingUtils.exportJR(strReportName, expType, designParameters, outputFile, false, conn,
          null, exportParameters);

      try (InputStream inp = new FileInputStream(outputFile)) {
        // convert to base64 encode
        byte[] bytes = IOUtils.toByteArray(inp);
        responseData = Base64.getEncoder().encodeToString(bytes);

        // delete file in Local Grp location
        outputFile.delete();

        // send to DMS
        response = dmsGRP.sendReportToDMS(profileURI, responseData, nodeID, documentno,
            header.getDocumentNo(), attributes);

      } catch (Exception e) {
        log4j.error("Exception in Generating report: ", e);
        e.printStackTrace();
        OBDal.getInstance().rollbackAndClose();
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();

    }
    return response;
  }

  /**
   * 
   * @param sequenceNo
   * @return it will compare the documentno already exists
   */
  public static Boolean checkInvoiceDocumentNo(String sequenceNo) {
    Boolean isExists = Boolean.FALSE;
    try {
      OBQuery<Invoice> getInvoiceQry = OBDal.getInstance().createQuery(Invoice.class,
          "as e where e.documentNo=:documentNo");
      getInvoiceQry.setNamedParameter("documentNo", sequenceNo);
      if (getInvoiceQry != null && getInvoiceQry.list().size() > 0) {
        isExists = Boolean.TRUE;
      } else {
        isExists = Boolean.FALSE;
      }
    } catch (Exception e) {
      log4j.error("Exception while checking the document number exists or not in the invoice");
    }
    return isExists;
  }

  /**
   * This method is used to update modification with extra amount included in invoice after tax
   * changes
   * 
   * @param sourceEncumbraceLineId
   * 
   */
  public static void updateModificationInPoencumForExtraTax(
      EfinBudgetManencumlines sourceEncumbraceLineId, BigDecimal amount) {
    try {
      OBContext.setAdminMode();

      OBQuery<EfinBudManencumRev> modificationQry = OBDal.getInstance().createQuery(
          EfinBudManencumRev.class, " as e where e.sRCManencumline.id =:sourceEncumbraceLineId");
      modificationQry.setNamedParameter("sourceEncumbraceLineId", sourceEncumbraceLineId.getId());

      List<EfinBudManencumRev> modificationList = modificationQry.list();
      if (modificationList.size() > 0) {
        EfinBudManencumRev modification = modificationList.get(0);
        modification.setRevamount(modification.getRevamount().subtract(amount));
        OBDal.getInstance().save(modification);
        sourceEncumbraceLineId.setAmount(sourceEncumbraceLineId.getAmount().add(amount));
        sourceEncumbraceLineId.setAPPAmt(sourceEncumbraceLineId.getAPPAmt().add(amount));
        sourceEncumbraceLineId.setRevamount(sourceEncumbraceLineId.getRevamount().add(amount));
        OBDal.getInstance().save(sourceEncumbraceLineId);
      }
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error("Exception while updating modification in po" + e.getMessage());
    }
  }

  /**
   * This Method is used to form encumbrance line id and unique code id map
   * 
   * @param map
   *          with id and unique code
   */
  public static HashMap<String, String> getSourceEncumbranceLineIdMap(
      HashMap<String, BigDecimal> taxlines, Invoice invoice1) {
    HashMap<String, String> encumbranceLineMap = new HashMap<>();
    try {
      OBContext.setAdminMode();
      EfinBudgetManencum encumbrance = OBDal.getInstance().get(EfinBudgetManencum.class,
          invoice1.getEfinManualencumbrance().getId());

      for (EfinBudgetManencumlines lines : encumbrance.getEfinBudgetManencumlinesList()) {
        if (!encumbranceLineMap.containsKey(lines.getAccountingCombination().getId())) {
          encumbranceLineMap.put(lines.getAccountingCombination().getId(), lines.getId());
        }
      }

    } catch (Exception e) {
      log4j.error("Exception while gettinf source ebcumrbance  in po" + e.getMessage());
    }
    return encumbranceLineMap;
  }

  /**
   * 
   * @param vars
   * @param Invoice
   *          Object
   * @return based on the business need it will return the validation objects
   */
  @SuppressWarnings({ "unchecked" })
  public static JSONObject checkPaymentScheduleValidation(VariablesSecureApp vars,
      Invoice objinvoice) {
    JSONObject jsob = null;
    List<Object> invoice_list = new ArrayList<Object>(),
        invoice_amount_valid_list = new ArrayList<Object>(),
        invoice_uniquecode_list = new ArrayList<Object>(), needByDateList = new ArrayList<Object>();
    String sql = "";

    try {
      // case 1:
      // all payment schedule selected in invoice, invoice net total should not more than sum of
      // remaining amount
      sql = "select sum(posch.amount-posch.invoiced_amt) as total_remaining,invsch.c_invoice_id "
          + " from EFIN_INVOICE_PAYMENT_SCH invsch "
          + " join escm_payment_schedule posch on invsch.escm_payment_schedule_id=posch.escm_payment_schedule_id "
          + " where invsch.c_invoice_id =:invoiceid" + " group by invsch.c_invoice_id";

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
      query.setParameter("invoiceid", objinvoice.getId());
      if (query != null)
        invoice_list = query.list();

      if (invoice_list.size() > 0) {
        for (Object invoiceObj : invoice_list) {
          Object[] invoiceObjArray = (Object[]) invoiceObj;
          BigDecimal remaining_amount = new BigDecimal(invoiceObjArray[0].toString());
          if (objinvoice.getGrandTotalAmount().compareTo(remaining_amount) > 0) {
            jsob = new JSONObject();
            jsob.put("case", 1);
            jsob.put("message", OBMessageUtils.messageBD("Efin_InvoiceAmt_Exceeds_PaySch"));
          }
        }
      }
      // case 2
      // Invoice Line amount against unique code should not be more than the payment schedule amount
      // against the unique code
      if (jsob == null) {

        sql = "select invmainsch.c_validcombination_id ,comb.em_efin_uniquecode"
            + "          from (select sum(posch.amount-posch.invoiced_amt) as total_remaining,invsch.c_invoice_id, "
            + "        invsch.c_validcombination_id  "
            + "        from EFIN_INVOICE_PAYMENT_SCH invsch "
            + "        join escm_payment_schedule posch on invsch.escm_payment_schedule_id=posch.escm_payment_schedule_id "
            + " where invsch.c_invoice_id =:invmainschinvoice"
            + "        group by invsch.c_invoice_id,invsch.c_validcombination_id ) invmainsch "
            + "        join ( select "
            + "  invline.linenetamt as line_amt,invsch.c_invoice_id,invline.em_efin_c_validcombination_id "
            + "   from (select distinct c_validcombination_id ,c_invoice_id from EFIN_INVOICE_PAYMENT_SCH ) invsch "
            + "    join (select sum(invline.linenetamt) as linenetamt,em_efin_c_validcombination_id,c_invoice_id "
            + "   from c_invoiceline invline " + "    where c_invoice_id=:invlineInvoiceId "
            + "   group by em_efin_c_validcombination_id,c_invoice_id) invline "
            + "   on invline.c_invoice_id=invsch.c_invoice_id   "
            + "  and invline.em_efin_c_validcombination_id=invsch.c_validcombination_id "
            + "   where invsch.c_invoice_id =:invPayInvoiceId "
            + "        ) invline on invline.em_efin_c_validcombination_id=invmainsch.c_validcombination_id "
            + " join c_validcombination comb on comb.c_validcombination_id=invmainsch.c_validcombination_id "
            + "        where invline.line_amt > invmainsch.total_remaining";
        SQLQuery query_amount_vaidation = OBDal.getInstance().getSession().createSQLQuery(sql);
        query_amount_vaidation.setParameter("invmainschinvoice", objinvoice.getId());
        query_amount_vaidation.setParameter("invlineInvoiceId", objinvoice.getId());
        query_amount_vaidation.setParameter("invPayInvoiceId", objinvoice.getId());

        StringBuilder uniqueCodeList = new StringBuilder();
        String uniqueCodes = "";
        if (query_amount_vaidation != null)
          invoice_amount_valid_list = query_amount_vaidation.list();
        if (invoice_amount_valid_list.size() > 0) {
          for (Object invoiceValidObj : invoice_amount_valid_list) {
            Object[] invoiceValidObjArray = (Object[]) invoiceValidObj;
            uniqueCodeList.append(invoiceValidObjArray[1].toString());
            uniqueCodeList.append(",");
          }
        }
        if (uniqueCodeList.length() > 0) {
          jsob = new JSONObject();
          uniqueCodes = uniqueCodeList.substring(0, uniqueCodeList.length() - 1);
          jsob.put("case", 2);
          jsob.put("message", OBMessageUtils.messageBD("Efin_LineAmount_ExceedsPaymentSch")
              .replace("@", uniqueCodes));
        }

      }
      if (jsob == null) {
        sql = " select invline.em_efin_c_validcombination_id,comb.em_efin_uniquecode from c_invoiceline invline "
            + " join c_validcombination comb on comb.c_validcombination_id=invline.em_efin_c_validcombination_id"
            + " where invline.em_efin_c_validcombination_id not in (select c_validcombination_id from EFIN_INVOICE_PAYMENT_SCH "
            + " where c_invoice_id =:schinvoiceid)"
            + " and invline.em_efin_c_validcombination_id in (select c_validcombination_id from escm_payment_schedule "
            + " where c_order_id=:orderId )"
            + " and invline.c_invoice_id =:lineinvoiceid and invline.em_efin_c_validcombination_id is not null";
        SQLQuery query_uniquecode_vaidation = OBDal.getInstance().getSession().createSQLQuery(sql);
        query_uniquecode_vaidation.setParameter("schinvoiceid", objinvoice.getId());
        query_uniquecode_vaidation.setParameter("orderId", objinvoice.getEfinCOrder().getId());
        query_uniquecode_vaidation.setParameter("lineinvoiceid", objinvoice.getId());

        StringBuilder uniqueCodeList = new StringBuilder();
        String uniqueCodes = "";
        if (query_uniquecode_vaidation != null)
          invoice_uniquecode_list = query_uniquecode_vaidation.list();
        if (invoice_uniquecode_list.size() > 0) {
          for (Object invoiceValidObj : invoice_uniquecode_list) {
            Object[] invoiceValidObjArray = (Object[]) invoiceValidObj;
            uniqueCodeList.append(invoiceValidObjArray[1].toString());
            uniqueCodeList.append(",");
          }
        }
        if (uniqueCodeList.length() > 0) {
          jsob = new JSONObject();
          uniqueCodes = uniqueCodeList.substring(0, uniqueCodeList.length() - 1);
          jsob.put("case", 3);
          jsob.put("message", OBMessageUtils.messageBD("Efin_PaySch_UniqueCodeNotInInvLine")
              .replace("@", uniqueCodes));
        }
      }
      if (jsob == null) {
        sql = " select posch.escm_payment_schedule_id " + " from EFIN_INVOICE_PAYMENT_SCH invsch  "
            + " join c_invoice inv on inv.c_invoice_id=invsch.c_invoice_id "
            + " join escm_payment_schedule posch on invsch.escm_payment_schedule_id=posch.escm_payment_schedule_id "
            + " where invsch.c_invoice_id=:invoiceid  "
            + "  and to_date(to_char(inv.dateinvoiced,'DD-MM-YYYY')) < to_date(to_char(posch.needbydate ,'DD-MM-YYYY'))";
        SQLQuery needByDateQuery = OBDal.getInstance().getSession().createSQLQuery(sql);
        needByDateQuery.setParameter("invoiceid", objinvoice.getId());
        if (needByDateQuery != null)
          needByDateList = needByDateQuery.list();

        if (needByDateList.size() > 0) {
          jsob = new JSONObject();
          jsob.put("case", 4);
          jsob.put("message", OBMessageUtils.messageBD("Efin_InvoicdDate_Ls_Agnst_PaySchDate"));

        }
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      try {
        jsob.put("message", OBMessageUtils.messageBD("Efin_Error_onPaymentSchValidation"));

      } catch (JSONException e1) {
        log4j.error("Exception while checkPaymentScheduleValidation: " + e);
      }
      log4j.error("Exception while checkPaymentScheduleValidation: " + e);
    }

    return jsob;
  }

  /**
   * 
   * @param invoice2
   * @param vars
   * @return once all the values in payment scheduled updated will return true else false
   */
  @SuppressWarnings("unchecked")
  public static Boolean updatePaymentScheduleDetails(Invoice objInvoice, VariablesSecureApp vars) {
    Boolean isUpdated = true;
    List<Object> invoice_list = new ArrayList<Object>(), paySch_list = new ArrayList<Object>();
    String sql = "", payScheduleQry = "";

    try {

      sql = " select em_efin_c_validcombination_id,sum(linenetamt) as amount from c_invoiceline "
          + " where c_invoice_id=:invlineinvoiceid " + " group by em_efin_c_validcombination_id "
          + " order by em_efin_c_validcombination_id asc ";

      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(sql);
      query.setParameter("invlineinvoiceid", objInvoice.getId());
      if (query != null)
        invoice_list = query.list();

      if (invoice_list.size() > 0) {
        for (Object invoiceObj : invoice_list) {
          Object[] invoiceObjArray = (Object[]) invoiceObj;
          String invoiceLineUniqueCode = invoiceObjArray[0] != null ? invoiceObjArray[0].toString()
              : "";
          // find out the unique code
          // and update the amount in payment schedule
          if (invoiceObjArray[1] == null) {
            // execute the distribution logic
            Boolean isAmountDistributed = distributeThePaymentScheduleAmount(objInvoice,
                invoiceLineUniqueCode, invoiceObjArray[1].toString());
            if (!isAmountDistributed) {
              isUpdated = false;
            }
          } else {
            BigDecimal remaining_amount = BigDecimal.ZERO;
            String uniqueCode = "";
            Boolean skipUpdate = false;
            BigDecimal lineSumAmount = new BigDecimal(invoiceObjArray[1].toString());
            payScheduleQry = " select (posch.amount-posch.invoiced_amt) as total_remaining ,"
                + "invsch.c_validcombination_id ,invsch.efin_invoice_payment_sch_id"
                + " from EFIN_INVOICE_PAYMENT_SCH invsch "
                + " join escm_payment_schedule posch on invsch.escm_payment_schedule_id=posch.escm_payment_schedule_id  "
                + " where invsch.c_invoice_id =:invmainschinvoice and invsch.c_validcombination_id=:validcombination_id "
                + " order by invsch.c_validcombination_id asc ";
            SQLQuery payschSqlQry = OBDal.getInstance().getSession().createSQLQuery(payScheduleQry);
            payschSqlQry.setParameter("invmainschinvoice", objInvoice.getId());
            payschSqlQry.setParameter("validcombination_id", invoiceLineUniqueCode);

            if (payschSqlQry != null)
              paySch_list = payschSqlQry.list();

            if (paySch_list.size() > 0) {
              for (Object paySchObj : paySch_list) {
                Object[] paySchObjArray = (Object[]) paySchObj;
                if (!skipUpdate) {
                  if (remaining_amount.compareTo(BigDecimal.ZERO) == 0
                      && !uniqueCode.equals(invoiceLineUniqueCode)) {
                    if (lineSumAmount
                        .compareTo(new BigDecimal(paySchObjArray[0].toString())) >= 0) {
                      EfinInvoicePaymentSch invPaymenstSchedule = OBDal.getInstance()
                          .get(EfinInvoicePaymentSch.class, paySchObjArray[2].toString());
                      invPaymenstSchedule
                          .setUniquecodeamount(new BigDecimal(paySchObjArray[0].toString()));
                      invPaymenstSchedule
                          .setInvoiceAmount(new BigDecimal(paySchObjArray[0].toString()));
                      ESCMPaymentSchedule poPaymentSchedule = invPaymenstSchedule
                          .getEscmPaymentSchedule();
                      poPaymentSchedule.setInvoicedAmt(poPaymentSchedule.getInvoicedAmt()
                          .add(new BigDecimal(paySchObjArray[0].toString())));
                      OBDal.getInstance().save(poPaymentSchedule);

                      remaining_amount = lineSumAmount
                          .subtract(new BigDecimal(paySchObjArray[0].toString()));
                      uniqueCode = paySchObjArray[0].toString();
                    } else {
                      EfinInvoicePaymentSch invPaymenstSchedule = OBDal.getInstance()
                          .get(EfinInvoicePaymentSch.class, paySchObjArray[2].toString());
                      invPaymenstSchedule.setUniquecodeamount(lineSumAmount);
                      invPaymenstSchedule.setInvoiceAmount(lineSumAmount);
                      ESCMPaymentSchedule poPaymentSchedule = invPaymenstSchedule
                          .getEscmPaymentSchedule();
                      poPaymentSchedule
                          .setInvoicedAmt(poPaymentSchedule.getInvoicedAmt().add(lineSumAmount));
                      OBDal.getInstance().save(poPaymentSchedule);
                      skipUpdate = true;
                    }

                  } else {
                    if (remaining_amount
                        .compareTo(new BigDecimal(paySchObjArray[0].toString())) >= 0) {
                      EfinInvoicePaymentSch invPaymenstSchedule = OBDal.getInstance()
                          .get(EfinInvoicePaymentSch.class, paySchObjArray[2].toString());
                      invPaymenstSchedule
                          .setUniquecodeamount(new BigDecimal(paySchObjArray[0].toString()));
                      invPaymenstSchedule
                          .setInvoiceAmount(new BigDecimal(paySchObjArray[0].toString()));
                      ESCMPaymentSchedule poPaymentSchedule = invPaymenstSchedule
                          .getEscmPaymentSchedule();
                      poPaymentSchedule.setInvoicedAmt(poPaymentSchedule.getInvoicedAmt()
                          .add(new BigDecimal(paySchObjArray[0].toString())));
                      OBDal.getInstance().save(poPaymentSchedule);

                      remaining_amount = remaining_amount
                          .subtract(new BigDecimal(paySchObjArray[0].toString()));
                      uniqueCode = paySchObjArray[0].toString();
                    } else {
                      EfinInvoicePaymentSch invPaymenstSchedule = OBDal.getInstance()
                          .get(EfinInvoicePaymentSch.class, paySchObjArray[2].toString());
                      invPaymenstSchedule.setUniquecodeamount(remaining_amount);
                      invPaymenstSchedule.setInvoiceAmount(remaining_amount);
                      ESCMPaymentSchedule poPaymentSchedule = invPaymenstSchedule
                          .getEscmPaymentSchedule();
                      poPaymentSchedule
                          .setInvoicedAmt(poPaymentSchedule.getInvoicedAmt().add(remaining_amount));
                      OBDal.getInstance().save(poPaymentSchedule);
                      skipUpdate = true;

                    }
                  }
                }
              }
            }
          }

        }
      }
    } catch (GenericJDBCException e) {
      isUpdated = false;
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Exeception in updatePaymentScheduleDetails:", e);
      Throwable t = DbUtility.getUnderlyingSQLException(e);
      throw new GenericJDBCException(t.getMessage(), null);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Exeception in updatePaymentScheduleDetails:", e);
      isUpdated = false;
    }
    return isUpdated;
  }

  /**
   * 
   * @param objInvoice
   * @param invoiceLineUniqueCode
   * @param invoiceLineId
   * @return if the paymentschedule amount distributed then will return else false
   */
  @SuppressWarnings("unchecked")
  private static Boolean distributeThePaymentScheduleAmount(Invoice objInvoice,
      String invoiceLineUniqueCode, String invoiceLineId) {
    Boolean isDistributed = Boolean.TRUE;
    List<Object> invoice_list = new ArrayList<Object>(), payschedule_list = new ArrayList<Object>();
    String total_sch_amt_qry = "", paysch_qry = "";
    BigDecimal total_scheduled_amt = BigDecimal.ZERO;
    BigDecimal invoiceLineAmount = BigDecimal.ZERO;
    try {
      // find the total scheduled amount
      total_sch_amt_qry = "select sum(amount) as total_scheduled_amt  from Efin_Invoice_Payment_Sch"
          + " where c_invoice_id =:invoiceId";
      SQLQuery query = OBDal.getInstance().getSession().createSQLQuery(total_sch_amt_qry);
      query.setParameter("invoiceId", objInvoice.getId());
      if (query != null)
        invoice_list = query.list();
      if (invoice_list.size() > 0) {
        for (Object invoiceObj : invoice_list) {
          Object[] invoiceObjArray = (Object[]) invoiceObj;
          total_scheduled_amt = new BigDecimal(invoiceObjArray[0].toString());
        }
      }
      // get the invoice line net amount
      InvoiceLine objInvoiceLine = OBDal.getInstance().get(InvoiceLine.class, invoiceLineId);
      invoiceLineAmount = objInvoiceLine.getLineNetAmount();

      // distribute to the invoice payment schedule

      paysch_qry = "select amount,Efin_Invoice_Payment_Sch_id from Efin_Invoice_Payment_Sch "
          + "      where c_invoice_id =:invoiceId";

      SQLQuery paysch_sql_qry = OBDal.getInstance().getSession().createSQLQuery(paysch_qry);
      paysch_sql_qry.setParameter("invoiceId", objInvoice.getId());
      if (paysch_sql_qry != null)
        payschedule_list = paysch_sql_qry.list();
      if (payschedule_list.size() > 0) {
        for (Object invoiceObj : payschedule_list) {
          Object[] invoiceObjArray = (Object[]) invoiceObj;
          BigDecimal amount = new BigDecimal(invoiceObjArray[0].toString());
          BigDecimal weightage_percent = amount.divide(total_scheduled_amt);
          BigDecimal distributed_amount = weightage_percent.multiply(invoiceLineAmount);
          EfinInvoicePaymentSch objPaymentSch = OBDal.getInstance().get(EfinInvoicePaymentSch.class,
              invoiceObjArray[1].toString());
          objPaymentSch.setDistributionamount(distributed_amount);
          objPaymentSch.setInvoiceAmount(objPaymentSch.getInvoiceAmount().add(distributed_amount));
          OBDal.getInstance().save(objPaymentSch);
          ESCMPaymentSchedule poPaymentSchedule = objPaymentSch.getEscmPaymentSchedule();
          poPaymentSchedule
              .setInvoicedAmt(poPaymentSchedule.getInvoicedAmt().add(distributed_amount));
          OBDal.getInstance().save(poPaymentSchedule);
        }
      }
      isDistributed = Boolean.TRUE;
    } catch (Exception e) {
      isDistributed = Boolean.FALSE;
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Exeception in distributeThePaymentScheduleAmount:", e);
    }
    return isDistributed;
  }

  /**
   * 
   * @param objInvoice
   * @param vars
   * @return if the payment schedule is final pay then it will close the PO and return true else
   *         false
   */
  @SuppressWarnings("unchecked")
  public static Boolean checkFinalPaymentAndUpdateOrder(Invoice objInvoice,
      VariablesSecureApp vars) {
    String paysch_qry = "";
    List<Object> payschedule_list = new ArrayList<Object>();
    Boolean isFinalPayAndUpdated = Boolean.FALSE;

    try {
      OBContext.setAdminMode();
      paysch_qry = "select posch.c_order_id ,posch.escm_payment_schedule_id "
          + "from Efin_Invoice_Payment_Sch invsch "
          + "join escm_payment_schedule posch on posch.escm_payment_schedule_id=invsch.escm_payment_schedule_id "
          + "where posch.pay_nature='FP' " + "and invsch.c_invoice_id =:invoiceid limit 1";

      SQLQuery paysch_sql_qry = OBDal.getInstance().getSession().createSQLQuery(paysch_qry);
      paysch_sql_qry.setParameter("invoiceid", objInvoice.getId());
      if (paysch_sql_qry != null)
        payschedule_list = paysch_sql_qry.list();
      if (payschedule_list.size() > 0) {
        for (Object invoicePayObj : payschedule_list) {
          Object[] invoicePayObjArray = (Object[]) invoicePayObj;
          String orderId = invoicePayObjArray[0].toString();
          Order objOrder = OBDal.getInstance().get(Order.class, orderId);
          objOrder.setEscmAppstatus("ESCM_CL");
          OBDal.getInstance().save(objOrder);
          if (!StringUtils.isEmpty(orderId)) {
            JSONObject historyData = new JSONObject();
            historyData.put("ClientId", vars.getClient());
            historyData.put("OrgId", vars.getOrg());
            historyData.put("RoleId", vars.getRole());
            historyData.put("UserId", vars.getUser());
            historyData.put("HeaderId", orderId);
            historyData.put("Comments", OBMessageUtils.messageBD("EFIN_PO_CLOSED_FROM_INVOICE"));
            historyData.put("Status", "CD");
            historyData.put("Revision", objOrder.getEscmRevision());
            historyData.put("NextApprover", "");
            historyData.put("HistoryTable", ApprovalTables.Purchase_Order_History);
            historyData.put("HeaderColumn", ApprovalTables.Purchase_Order_History_HEADER_COLUMN);
            historyData.put("ActionColumn", ApprovalTables.Purchase_Order_History_DOCACTION_COLUMN);

            POContractSummaryDAO.purchaseOrderApprovalHistory(historyData);

          }
        }
      }
      isFinalPayAndUpdated = Boolean.TRUE;
    } catch (Exception e) {
      isFinalPayAndUpdated = Boolean.FALSE;
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Exeception in distributeThePaymentScheduleAmount:", e);
    } finally {
      OBContext.restorePreviousMode();
    }
    return isFinalPayAndUpdated;
  }
}
