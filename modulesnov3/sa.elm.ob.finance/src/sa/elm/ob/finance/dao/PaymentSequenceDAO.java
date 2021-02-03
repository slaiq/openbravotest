package sa.elm.ob.finance.dao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sa.elm.ob.finance.Efin_Child_Sequence;
import sa.elm.ob.finance.Efin_Parent_Sequence;
import sa.elm.ob.finance.Efin_Return_Sequence;
import sa.elm.ob.finance.Efin_payment_sequences;

public class PaymentSequenceDAO {
  private static final Logger LOG = LoggerFactory.getLogger(PaymentSequenceDAO.class);

  /**
   * This method is used to get the payment sequence based on [Payment and parent sequence] selected
   * in payment out
   * 
   * @param payment
   * @return sequence
   */
  public static Map<Efin_Child_Sequence, String> getPaymentSequenceWhileSave(FIN_Payment payment) {

    try {
      OBContext.setAdminMode();
      String whereClause = "as child join child.efinParentSequence parent "
          + " join parent.efinPaymentSequences  payment where payment.id = :payment and parent.id = :parent and child.userContact.id =:user order by child.fINFrom ";
      String returnSeqWhereClause = " as e where e.efinChildSequence.id =:childid order by e.returnsequence  asc";

      Map<Efin_Child_Sequence, String> resultMap = new HashMap<Efin_Child_Sequence, String>();
      BigDecimal nextSeq = null;

      OBQuery<Efin_Child_Sequence> childSequenceQry = OBDal.getInstance()
          .createQuery(Efin_Child_Sequence.class, whereClause);

      childSequenceQry.setNamedParameter("payment",
          payment.getEfinPaymentSequences() != null ? payment.getEfinPaymentSequences().getId()
              : null);

      childSequenceQry.setNamedParameter("parent",
          payment.getEfinParentSequences() != null ? payment.getEfinParentSequences().getId()
              : null);
      childSequenceQry.setNamedParameter("user", payment.getCreatedBy().getId());

      List<Efin_Child_Sequence> childSeqList = childSequenceQry.list();
      if ((!childSeqList.isEmpty()) && childSeqList.size() > 0) {
        Efin_Child_Sequence childSeq = null;
        for (Efin_Child_Sequence seq : childSeqList) {
          if (childSeq != null) {
            break;
          }
          if (childSeq == null && seq.getFINTo().compareTo(seq.getNextSequence()) >= 0
              || seq.getEfinReturnSequenceList().size() > 0) {
            childSeq = seq;
          }
        }

        if (childSeq != null) {
          if (childSeq.getEfinReturnSequenceList().size() > 0) {
            OBQuery<Efin_Return_Sequence> returnSequenceQuery = OBDal.getInstance()
                .createQuery(Efin_Return_Sequence.class, returnSeqWhereClause);
            returnSequenceQuery.setNamedParameter("childid", childSeq.getId());

            List<Efin_Return_Sequence> returnSeqList = returnSequenceQuery.list();
            Efin_Return_Sequence returnSeq = returnSeqList.get(0);
            resultMap.put(returnSeq.getEfinChildSequence(),
                returnSeq.getReturnsequence() != null ? returnSeq.getReturnsequence().toString()
                    : null);
            OBDal.getInstance().remove(returnSeq);

          } else {
            nextSeq = childSeq.getNextSequence();
            childSeq.setNextSequence(childSeq.getNextSequence().add(BigDecimal.ONE));
            childSeq.setUsed(true);
            OBDal.getInstance().save(childSeq);
            resultMap.put(childSeq, nextSeq != null ? nextSeq.toString() : null);
          }
        }
      }
      return resultMap;

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.debug("Exception while getting payment sequence", e);
    }
    return null;
  }

  /**
   * This method is used to get the payment sequence based on [Payment and parent sequence] selected
   * in payment out
   * 
   * @param payment
   * @return sequence
   */
  public static String getPaymentSequence(FIN_Payment payment) {

    try {
      OBContext.setAdminMode();
      String whereClause = "as child join child.efinParentSequence parent "
          + " join parent.efinPaymentSequences  payment where payment.id = :payment and parent.id = :parent and child.fINTo  >= child.nextSequence and child.userContact.id =:user order by child.fINFrom ";

      BigDecimal nextSeq = null;

      OBQuery<Efin_Child_Sequence> childSequenceQry = OBDal.getInstance()
          .createQuery(Efin_Child_Sequence.class, whereClause);

      childSequenceQry.setNamedParameter("payment",
          payment.getEfinPaymentSequences() != null ? payment.getEfinPaymentSequences().getId()
              : null);

      childSequenceQry.setNamedParameter("parent",
          payment.getEfinParentSequences() != null ? payment.getEfinParentSequences().getId()
              : null);
      childSequenceQry.setNamedParameter("user", payment.getCreatedBy().getId());

      if ((!childSequenceQry.list().isEmpty()) && childSequenceQry.list().size() > 0) {

        Efin_Child_Sequence childSeq = childSequenceQry.list().get(0);

        if (childSeq.getEfinReturnSequenceList().size() > 0) {
          Efin_Return_Sequence returnSeq = childSeq.getEfinReturnSequenceList().get(0);
          nextSeq = returnSeq.getReturnsequence();
          OBDal.getInstance().remove(returnSeq);
        } else {
          nextSeq = childSeq.getNextSequence();
          childSeq.setNextSequence(childSeq.getNextSequence().add(BigDecimal.ONE));
          childSeq.setUsed(true);
          OBDal.getInstance().save(childSeq);
          payment.setEfinChildSequence(childSeq);
          OBDal.getInstance().save(payment);
        }
      }

      if (nextSeq == null) {
        return null;
      } else {
        return nextSeq.toString();
      }

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.debug("Exception while getting payment sequence", e);
    }
    return null;
  }

  /**
   * This method is used to get payment sequence header based on [Organization, financial account->
   * bankId, accountId , payment method]
   * 
   * @param paymentMethod
   * @param bankId
   * @param accountId
   * @param orgId
   * @return
   */
  public static String getPaymentSequenceHeader(String paymentMethod, String bankId,
      String accountId, String orgId) {

    try {
      OBContext.setAdminMode();

      User user = OBContext.getOBContext().getUser();
      String whereClause = "as payment where payment.paymentMethod.id =:paymentmethod  and payment.efinBank.id =:bank and payment.efinAccount.id =:account and payment.organization.id =:org ";

      OBQuery<Efin_payment_sequences> paymentequenceQry = OBDal.getInstance()
          .createQuery(Efin_payment_sequences.class, whereClause);

      paymentequenceQry.setNamedParameter("paymentmethod", paymentMethod);
      paymentequenceQry.setNamedParameter("bank", bankId);
      paymentequenceQry.setNamedParameter("account", accountId);
      paymentequenceQry.setNamedParameter("org", orgId);

      if ((!paymentequenceQry.list().isEmpty()) && paymentequenceQry.list().size() > 0) {

        Efin_payment_sequences paymentSeq = paymentequenceQry.list().get(0);
        long userCount = 0;
        for (Efin_Parent_Sequence parentSeq : paymentSeq.getEfinParentSequenceList()) {
          userCount = parentSeq.getEfinChildSequenceList().stream()
              .filter(a -> a.getUserContact().getId().equals(user.getId())
                  && (a.getFINTo().compareTo(a.getNextSequence()) >= 0
                      || a.getEfinReturnSequenceList().size() > 0))
              .count();
          if (userCount > 0) {
            return paymentSeq.getId();
          }
        }
      }
      return null;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      LOG.debug("Exception while getting payment sequence", e);
    }
    return null;
  }

}
