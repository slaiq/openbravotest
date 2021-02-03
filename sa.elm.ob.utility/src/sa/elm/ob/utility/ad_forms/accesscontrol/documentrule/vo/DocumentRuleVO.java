package sa.elm.ob.utility.ad_forms.accesscontrol.documentrule.vo;

import java.math.BigDecimal;

public class DocumentRuleVO {
  private String id;
  private String name;
  private String no;
  private BigDecimal ruleValue;
  private int ruleSequenceNo;
  private int roleSequenceNo;
  private int roleOrderNo;
  private String ruleUsed;
  private String allowReservation;
  private String Reservation_role;
  private String isMultiRule;
  private String requester;
  private Boolean isDummyRole;
  private String iscontractcategory;
  private String contractcategory_role;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNo() {
    return no;
  }

  public void setNo(String no) {
    this.no = no;
  }

  public BigDecimal getRuleValue() {
    return ruleValue;
  }

  public void setRuleValue(BigDecimal ruleValue) {
    this.ruleValue = ruleValue;
  }

  public String getRuleUsed() {
    return ruleUsed;
  }

  public void setRuleUsed(String ruleUsed) {
    this.ruleUsed = ruleUsed;
  }

  public int getRoleSequenceNo() {
    return roleSequenceNo;
  }

  public void setRoleSequenceNo(int roleSequenceNo) {
    this.roleSequenceNo = roleSequenceNo;
  }

  public int getRoleOrderNo() {
    return roleOrderNo;
  }

  public void setRoleOrderNo(int roleOrderNo) {
    this.roleOrderNo = roleOrderNo;
  }

  public int getRuleSequenceNo() {
    return ruleSequenceNo;
  }

  public void setRuleSequenceNo(int ruleSequenceNo) {
    this.ruleSequenceNo = ruleSequenceNo;
  }

  public String getAllowReservation() {
    return allowReservation;
  }

  public void setAllowReservation(String allowReservation) {
    this.allowReservation = allowReservation;
  }

  public String getReservation_role() {
    return Reservation_role;
  }

  public void setReservation_role(String reservation_role) {
    Reservation_role = reservation_role;
  }

  public String getIsMultiRule() {
    return isMultiRule;
  }

  public void setIsMultiRule(String isMultiRule) {
    this.isMultiRule = isMultiRule;
  }

  public String getRequester() {
    return requester;
  }

  public void setRequester(String requester) {
    this.requester = requester;
  }

  public Boolean getIsDummyRole() {
    return isDummyRole;
  }

  public void setIsDummyRole(Boolean isDummyRole) {
    this.isDummyRole = isDummyRole;
  }

  public String getContractcategory_role() {
    return contractcategory_role;
  }

  public void setContractcategory_role(String contractcategory_role) {
    this.contractcategory_role = contractcategory_role;
  }

  public String getIscontractcategory() {
    return iscontractcategory;
  }

  public void setIscontractcategory(String iscontractcategory) {
    this.iscontractcategory = iscontractcategory;
  }
}