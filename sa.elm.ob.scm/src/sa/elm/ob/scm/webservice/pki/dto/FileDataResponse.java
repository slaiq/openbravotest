package sa.elm.ob.scm.webservice.pki.dto;

public class FileDataResponse {

  private String licenseBytes;
  private String signatureBytes;

  public String getLicenseBytes() {
    return licenseBytes;
  }

  public void setLicenseBytes(String licenseBytes) {
    this.licenseBytes = licenseBytes;
  }

  public String getSignatureBytes() {
    return signatureBytes;
  }

  public void setSignatureBytes(String signatureBytes) {
    this.signatureBytes = signatureBytes;
  }

}
