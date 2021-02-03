package sa.elm.ob.scm.webservice.epm.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.common.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import sa.elm.ob.scm.webservice.epm.dto.AddExtractProjectRequestDto;
import sa.elm.ob.scm.webservice.epm.dto.AddProjectRequestDto;
import sa.elm.ob.scm.webservice.epm.dto.AddProjectResponseDto;
import sa.elm.ob.utility.WebserviceTrackerHeader;

@Service
public class EmpServiceImpl implements EpmService {

  private static final Logger log = LoggerFactory.getLogger(EmpServiceImpl.class);

  // @Bean
  // public RestTemplate restTemplate() {
  // return new RestTemplate();
  // }

  // @Autowired
  RestTemplate restTemplate = new RestTemplate();

  /**
   * Add Project EPM Web service
   */
  @Override
  public void addProject(Order order, Integer cityProjectId, BigDecimal valueApprovedExtracts) {
    AddProjectRequestDto requestDto = new AddProjectRequestDto();
    ResponseEntity<AddProjectResponseDto> result = null;
    try {
      requestDto.setProjectPlaceId(cityProjectId);
      requestDto.setName(order.getDescription());
      if (order.getEfinBudgetint() != null && order.getEfinBudgetint().getDescription() != null) {
        requestDto.setYear(order.getEfinBudgetint().getDescription());
      }
      requestDto.setContractSignDate_Georgian(formatDate(order.getEscmSignaturedate()));
      requestDto.setProjectApprovedValue(order.getEscmTotPoUpdatedAmt());
      requestDto.setValueFinalContract(order.getGrandTotalAmount());
      requestDto.setNumberOfBaptism(order.getEscmReferenceNo());
      requestDto.setValueApprovedExtracts(valueApprovedExtracts);
      requestDto.setgRPNumber(order.getDocumentNo());

      String url = Preferences.getPreferenceValue("ESCM_EPM_URL", true,
          OBContext.getOBContext().getCurrentClient(), null, null, null, null);

      log.debug("---------Emp Add Project Calling Webservice------------");

      result = restTemplate.postForEntity(url, requestDto, AddProjectResponseDto.class);
      log.debug("Emp Add Project Webservice response code " + result.getStatusCode());
      if (result != null && result.getStatusCode() == HttpStatus.OK) {
        log.debug("Emp Add Project Webservice :" + result.getBody().isSuccess());
      }
    } catch (Exception e) {
      log.error("Emp Add Project Webservice Excception :", e);
    } finally {
      addProjectDBLog(result, requestDto);
    }

  }

  private void addProjectDBLog(ResponseEntity<AddProjectResponseDto> result,
      AddProjectRequestDto requestDto) {
    try {
      OBContext.setAdminMode(true);
      ObjectMapper objMap = new ObjectMapper();
      String request = requestDto != null ? objMap.writeValueAsString(requestDto) : null;
      String response = result != null ? objMap.writeValueAsString(result) : null;

      WebserviceTrackerHeader header = OBProvider.getInstance().get(WebserviceTrackerHeader.class);
      header.setOrganization(OBContext.getOBContext().getCurrentOrganization());
      header.setClient(OBContext.getOBContext().getCurrentClient());
      header.setRequestnumber(null);
      header.setResponse(response);
      header.setResponseerrormessage(request);// logging request, because no field present.
      header.setWebservicename("AddEpmProject");
      OBDal.getInstance().save(header);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("addProjectDBLog Exception :", e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private String formatDate(Date date) {
    String dateFormat = null;
    if (date != null) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      dateFormat = formatter.format(date);
    }

    return dateFormat;
  }

  @Override
  public void addExtractProject(Order order, Integer cityProjectId) {
    AddExtractProjectRequestDto requestDto = new AddExtractProjectRequestDto();
    requestDto.setExtractValue(null);
    requestDto.setExtractValueDate(null);
    requestDto.setExchangeValue(null);
    requestDto.setExchangeValueDate(null);
    requestDto.setgRPNumber(order.getDocumentNo());

    log.debug("---------Emp Add Project Calling Webservice------------");

    try {
      // stage url

      String url = Preferences.getPreferenceValue("ESCM_EPM_URL", false,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), null);
      // String url =
      // "http://epmdashboard.mot.gov.sa/EPMProjectAPI/api/ProjectAPI/AddExtractsInProject";

      ResponseEntity<String> result = restTemplate.postForEntity(url, requestDto, String.class);
      log.debug("Emp Add Project Webservice response code " + result.getStatusCode());
      if (result.getStatusCode() == HttpStatus.OK) {

      }
    } catch (Exception e) {
      log.error("Emp Add Project Webservice Exception :", e);
    }

  }

}
