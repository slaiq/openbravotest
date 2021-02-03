package sa.elm.ob.finance.ad_forms.journalapproval.vo;

import java.math.BigDecimal;

public class GLJournalApprovalVO {
	private String orderId;
	private String documentNo;
	private String status;
	private String dateOrdered;
	private String bPartnerId;
	private String bPartnerName;
	private String deliveryDate;
	private String requester;
	private String requesterDate;
	private String netListPrice;
	private String priority;
	private String referenceNo;
	private String description;
	private String lineno;
	private String lineDebit;
	private String lineCredit;
	private String lineAccount;
	private String lineDescription;
	private String lineQty;
	private String lineUom;
	private String orderLineId;
	private String approvalId;
	private String userId;
	private String roleId;
	private String approverName;
	private String approverRole;
	private String comments;
	private String approvedDate;
	private String warehouse;
	private String curSymbol;
	private String paymentDate;
	private String paymentID;
	private String finAcctId;
	private String finAcctName;
	private String paymentAmount;
	private String paymentDetailId;
	private String orderNo;
	private String invoiceNo;
	private String dueDate;
	private String invoiceAmount;
	private String expectedAmount;
	private String paidAmount;
	private String usedCredit;
	private String quNextRoleId;
	private String fromUserRole;
	private String toUserRole;
	private String glItem;
	private String submittedBy = "";
	private String note = "";
	private String orgName = "";
	private String orgId = "";
	private String ledger = "";
	private String documentDate;
	private String accountDate;
	private String period;
	private String periodName;
	private String Opening;
	private String sealNo;
	private String chequeNo;
	private String billNo;
	private String debitAmount;
	private String creditAmount;
	private String paymentMethodName = "";
	private String acctschemaId = "";
	private String acctschemaName = "";
	private String fundId = "";
	private String fundName = "";
	private String accountId = "";
	private String defaultDep = "";
	private String uniquecode = "";
	private String replaceact = "";
	private String amtCr = "";
	private String amtDr = "";
	private String initialAmt = "";
	private String finalBal = "";
	private String account = "";
	private String accountName = "";
	private String initialDr = "";

	public BigDecimal getInitDr() {
		return initDr;
	}

	public void setInitDr(BigDecimal initDr) {
		this.initDr = initDr;
	}

	public BigDecimal getInitCr() {
		return initCr;
	}

	public void setInitCr(BigDecimal initCr) {
		this.initCr = initCr;
	}

	public BigDecimal getInitNet() {
		return initNet;
	}

	public void setInitNet(BigDecimal initNet) {
		this.initNet = initNet;
	}

	public void setFinalDr(BigDecimal finalDr) {
		FinalDr = finalDr;
	}

	public void setFinalCr(BigDecimal finalCr) {
		FinalCr = finalCr;
	}

	public void setFinalNet(BigDecimal finalNet) {
		FinalNet = finalNet;
	}

	private BigDecimal initDr = new BigDecimal(0);
	private BigDecimal initCr = new BigDecimal(0);
	private BigDecimal initNet = new BigDecimal(0);
	private BigDecimal FinalDr = new BigDecimal(0);
	private BigDecimal FinalCr = new BigDecimal(0);
	private BigDecimal FinalNet = new BigDecimal(0);

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String name = "";

	public String getFinalDr() {
		return finalDr;
	}

	public void setFinalDr(String finalDr) {
		this.finalDr = finalDr;
	}

	public String getFinalCr() {
		return finalCr;
	}

	public void setFinalCr(String finalCr) {
		this.finalCr = finalCr;
	}

	public String getFinalNet() {
		return finalNet;
	}

	public void setFinalNet(String finalNet) {
		this.finalNet = finalNet;
	}

	private String initialCr = "";
	private String finalDr = "";
	private String finalCr = "";
	private String finalNet = "";

	public String getInitialCr() {
		return initialCr;
	}

	public void setInitialCr(String initialCr) {
		this.initialCr = initialCr;
	}

	public String getInitialDr() {
		return initialDr;
	}

	public void setInitialDr(String initialDr) {
		this.initialDr = initialDr;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getDefaultDep() {
		return defaultDep;
	}

	public void setDefaultDep(String defaultDep) {
		this.defaultDep = defaultDep;
	}

	public String getUniquecode() {
		return uniquecode;
	}

	public void setUniquecode(String uniquecode) {
		this.uniquecode = uniquecode;
	}

	public String getReplaceact() {
		return replaceact;
	}

	public void setReplaceact(String replaceact) {
		this.replaceact = replaceact;
	}

	public String getAmtCr() {
		return amtCr;
	}

	public void setAmtCr(String amtCr) {
		this.amtCr = amtCr;
	}

	public String getAmtDr() {
		return amtDr;
	}

	public void setAmtDr(String amtDr) {
		this.amtDr = amtDr;
	}

	public String getInitialAmt() {
		return initialAmt;
	}

	public void setInitialAmt(String initialAmt) {
		this.initialAmt = initialAmt;
	}

	public String getFinalBal() {
		return finalBal;
	}

	public void setFinalBal(String finalBal) {
		this.finalBal = finalBal;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getStartdate() {
		return startdate;
	}

	public void setStartdate(String startdate) {
		this.startdate = startdate;
	}

	private String startdate = "";

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	private String id = "";

	public String getFundId() {
		return fundId;
	}

	public void setFundId(String fundId) {
		this.fundId = fundId;
	}

	public String getFundName() {
		return fundName;
	}

	public void setFundName(String fundName) {
		this.fundName = fundName;
	}

	public String getAcctschemaId() {
		return acctschemaId;
	}

	public void setAcctschemaId(String acctschemaId) {
		this.acctschemaId = acctschemaId;
	}

	public String getAcctschemaName() {
		return acctschemaName;
	}

	public void setAcctschemaName(String acctschemaName) {
		this.acctschemaName = acctschemaName;
	}

	public String getPeriodName() {
		return periodName;
	}

	public void setPeriodName(String periodName) {
		this.periodName = periodName;
	}

	public String getLineno() {
		return lineno;
	}

	public String getLineQty() {
		return lineQty;
	}

	public void setLineQty(String lineQty) {
		this.lineQty = lineQty;
	}

	public String getLineUom() {
		return lineUom;
	}

	public void setLineUom(String lineUom) {
		this.lineUom = lineUom;
	}

	public String getLineDebit() {
		return lineDebit;
	}

	public void setLineDebit(String lineDebit) {
		this.lineDebit = lineDebit;
	}

	public String getLineCredit() {
		return lineCredit;
	}

	public void setLineCredit(String lineCredit) {
		this.lineCredit = lineCredit;
	}

	public String getLineDescription() {
		return lineDescription;
	}

	public void setLineDescription(String lineDescription) {
		this.lineDescription = lineDescription;
	}

	public void setLineno(String lineno) {
		this.lineno = lineno;
	}

	public String getLineAccount() {
		return lineAccount;
	}

	public void setLineAccount(String lineAccount) {
		this.lineAccount = lineAccount;
	}

	public String getLedger() {
		return ledger;
	}

	public void setLedger(String ledger) {
		this.ledger = ledger;
	}

	public String getDocumentDate() {
		return documentDate;
	}

	public void setDocumentDate(String documentDate) {
		this.documentDate = documentDate;
	}

	public String getAccountDate() {
		return accountDate;
	}

	public void setAccountDate(String accountDate) {
		this.accountDate = accountDate;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getOpening() {
		return Opening;
	}

	public void setOpening(String opening) {
		Opening = opening;
	}

	public String getSealNo() {
		return sealNo;
	}

	public void setSealNo(String sealNo) {
		this.sealNo = sealNo;
	}

	public String getChequeNo() {
		return chequeNo;
	}

	public void setChequeNo(String chequeNo) {
		this.chequeNo = chequeNo;
	}

	public String getBillNo() {
		return billNo;
	}

	public void setBillNo(String billNo) {
		this.billNo = billNo;
	}

	public String getDebitAmount() {
		return debitAmount;
	}

	public void setDebitAmount(String debitAmount) {
		this.debitAmount = debitAmount;
	}

	public String getCreditAmount() {
		return creditAmount;
	}

	public void setCreditAmount(String creditAmount) {
		this.creditAmount = creditAmount;
	}

	public String getPaymentMethodName() {
		return paymentMethodName;
	}

	public void setPaymentMethodName(String paymentMethodName) {
		this.paymentMethodName = paymentMethodName;
	}

	public String getGlItem() {
		return glItem;
	}

	public void setGlItem(String glItem) {
		this.glItem = glItem;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getApproverName() {
		return approverName;
	}

	public void setApproverName(String approverName) {
		this.approverName = approverName;
	}

	public String getApproverRole() {
		return approverRole;
	}

	public void setApproverRole(String approverRole) {
		this.approverRole = approverRole;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getNetListPrice() {
		return netListPrice;
	}

	public void setNetListPrice(String netListPrice) {
		this.netListPrice = netListPrice;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
	}

	public String getReferenceNo() {
		return referenceNo;
	}

	public void setReferenceNo(String referenceNo) {
		this.referenceNo = referenceNo;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getDocumentNo() {
		return documentNo;
	}

	public void setDocumentNo(String documentNo) {
		this.documentNo = documentNo;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDateOrdered() {
		return dateOrdered;
	}

	public void setDateOrdered(String dateOrdered) {
		this.dateOrdered = dateOrdered;
	}

	public String getbPartnerId() {
		return bPartnerId;
	}

	public void setbPartnerId(String bPartnerId) {
		this.bPartnerId = bPartnerId;
	}

	public String getbPartnerName() {
		return bPartnerName;
	}

	public void setbPartnerName(String bPartnerName) {
		this.bPartnerName = bPartnerName;
	}

	public String getDeliveryDate() {
		return deliveryDate;
	}

	public void setDeliveryDate(String deliveryDate) {
		this.deliveryDate = deliveryDate;
	}

	public String getRequester() {
		return requester;
	}

	public void setRequester(String requester) {
		this.requester = requester;
	}

	public String getRequesterDate() {
		return requesterDate;
	}

	public void setRequesterDate(String requesterDate) {
		this.requesterDate = requesterDate;
	}

	public String getOrderLineId() {
		return orderLineId;
	}

	public void setOrderLineId(String orderLineId) {
		this.orderLineId = orderLineId;
	}

	public String getApprovalId() {
		return approvalId;
	}

	public void setApprovalId(String approvalId) {
		this.approvalId = approvalId;
	}

	public String getApprovedDate() {
		return approvedDate;
	}

	public void setApprovedDate(String approvedDate) {
		this.approvedDate = approvedDate;
	}

	public String getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(String warehouse) {
		this.warehouse = warehouse;
	}

	public String getCurSymbol() {
		return curSymbol;
	}

	public void setCurSymbol(String curSymbol) {
		this.curSymbol = curSymbol;
	}

	public String getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(String paymentDate) {
		this.paymentDate = paymentDate;
	}

	public String getPaymentID() {
		return paymentID;
	}

	public void setPaymentID(String paymentID) {
		this.paymentID = paymentID;
	}

	public String getFinAcctId() {
		return finAcctId;
	}

	public void setFinAcctId(String finAcctId) {
		this.finAcctId = finAcctId;
	}

	public String getFinAcctName() {
		return finAcctName;
	}

	public void setFinAcctName(String finAcctName) {
		this.finAcctName = finAcctName;
	}

	public String getPaymentAmount() {
		return paymentAmount;
	}

	public void setPaymentAmount(String paymentAmount) {
		this.paymentAmount = paymentAmount;
	}

	public String getPaymentDetailId() {
		return paymentDetailId;
	}

	public void setPaymentDetailId(String paymentDetailId) {
		this.paymentDetailId = paymentDetailId;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public String getInvoiceNo() {
		return invoiceNo;
	}

	public void setInvoiceNo(String invoiceNo) {
		this.invoiceNo = invoiceNo;
	}

	public String getDueDate() {
		return dueDate;
	}

	public void setDueDate(String dueDate) {
		this.dueDate = dueDate;
	}

	public String getInvoiceAmount() {
		return invoiceAmount;
	}

	public void setInvoiceAmount(String invoiceAmount) {
		this.invoiceAmount = invoiceAmount;
	}

	public String getExpectedAmount() {
		return expectedAmount;
	}

	public void setExpectedAmount(String expectedAmount) {
		this.expectedAmount = expectedAmount;
	}

	public String getPaidAmount() {
		return paidAmount;
	}

	public void setPaidAmount(String paidAmount) {
		this.paidAmount = paidAmount;
	}

	public String getUsedCredit() {
		return usedCredit;
	}

	public void setUsedCredit(String usedCredit) {
		this.usedCredit = usedCredit;
	}

	public String getQuNextRoleId() {
		return quNextRoleId;
	}

	public void setQuNextRoleId(String quNextRoleId) {
		this.quNextRoleId = quNextRoleId;
	}

	public String getFromUserRole() {
		return fromUserRole;
	}

	public void setFromUserRole(String fromUserRole) {
		this.fromUserRole = fromUserRole;
	}

	public String getToUserRole() {
		return toUserRole;
	}

	public void setToUserRole(String toUserRole) {
		this.toUserRole = toUserRole;
	}

	public String getSubmittedBy() {
		return submittedBy;
	}

	public void setSubmittedBy(String submittedBy) {
		this.submittedBy = submittedBy;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}
}