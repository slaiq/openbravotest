
package sa.elm.ob.utility.gsb.redf;

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
     * @param dateOfBirth
     * @return
     *     returns sa.elm.ob.utility.gsb.redf.GetLoanDetailsResponseStructure
     * @throws Interface1GetLoanDetailsCommonErrorElementFaultMessage
     */
    @WebMethod(operationName = "GetLoanDetails", action = "http://tempuri.org/ILoanInquiryService/GetLoanDetails")
    @WebResult(name = "GetLoanDetailsResult", targetNamespace = "http://tempuri.org/")
    @RequestWrapper(localName = "GetLoanDetails", targetNamespace = "http://tempuri.org/", className = "sa.elm.ob.utility.gsb.redf.GetLoanDetails")
    @ResponseWrapper(localName = "GetLoanDetailsResponse", targetNamespace = "http://tempuri.org/", className = "sa.elm.ob.utility.gsb.redf.GetLoanDetailsResponse")
    public GetLoanDetailsResponseStructure getLoanDetails(
        @WebParam(name = "NationalId", targetNamespace = "http://tempuri.org/")
        String nationalId,
        @WebParam(name = "DateOfBirth", targetNamespace = "http://tempuri.org/")
        String dateOfBirth)
        throws Interface1GetLoanDetailsCommonErrorElementFaultMessage
    ;

}