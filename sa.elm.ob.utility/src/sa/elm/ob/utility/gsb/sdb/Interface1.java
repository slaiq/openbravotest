
package sa.elm.ob.utility.gsb.sdb;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.2.9-b130926.1035
 * Generated source version: 2.2
 * 
 */
@WebService(name = "Interface1", targetNamespace = "http://schemas.nevatech.com/services/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface Interface1 {


    /**
     * 
     * @param nationalId
     * @return
     *     returns sa.elm.ob.utility.gsb.sdb.GetLoanInfoResponseStructure
     * @throws Interface1GetLoanInfoCommonErrorElementFaultMessage
     */
    @WebMethod(operationName = "GetLoanInfo", action = "http://tempuri.org/ILoanInformationService/GetLoanInfo")
    @WebResult(name = "GetLoanInfoResult", targetNamespace = "http://tempuri.org/")
    @RequestWrapper(localName = "GetLoanInfo", targetNamespace = "http://tempuri.org/", className = "sa.elm.ob.utility.gsb.sdb.GetLoanInfo")
    @ResponseWrapper(localName = "GetLoanInfoResponse", targetNamespace = "http://tempuri.org/", className = "sa.elm.ob.utility.gsb.sdb.GetLoanInfoResponse")
    public GetLoanInfoResponseStructure getLoanInfo(
        @WebParam(name = "NationalId", targetNamespace = "http://tempuri.org/")
        String nationalId)
        throws Interface1GetLoanInfoCommonErrorElementFaultMessage
    ;

}