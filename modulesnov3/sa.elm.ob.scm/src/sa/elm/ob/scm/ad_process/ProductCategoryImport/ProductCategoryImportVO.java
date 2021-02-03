package sa.elm.ob.scm.ad_process.ProductCategoryImport;

public class ProductCategoryImportVO {
  private String message;
  private int result;
  private String maincatcode;
  private String maincatname;
  private String subcatname;
  private String subcatcode;
  private String docuemntno;
  private boolean Prodcatexists;
  private int lineno;

  public int getLineno() {
    return lineno;
  }

  public void setLineno(int lineno) {
    this.lineno = lineno;
  }

  public boolean isProdcatexists() {
    return Prodcatexists;
  }

  public void setProdcatexists(boolean prodcatexists) {
    Prodcatexists = prodcatexists;
  }

  public String getDocuemntno() {
    return docuemntno;
  }

  public void setDocuemntno(String docuemntno) {
    this.docuemntno = docuemntno;
  }

  public String getMaincatcode() {
    return maincatcode;
  }

  public void setMaincatcode(String maincatcode) {
    this.maincatcode = maincatcode;
  }

  public String getMaincatname() {
    return maincatname;
  }

  public void setMaincatname(String maincatname) {
    this.maincatname = maincatname;
  }

  public String getSubcatname() {
    return subcatname;
  }

  public void setSubcatname(String subcatname) {
    this.subcatname = subcatname;
  }

  public String getSubcatcode() {
    return subcatcode;
  }

  public void setSubcatcode(String subcatcode) {
    this.subcatcode = subcatcode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public int getResult() {
    return result;
  }

  public void setResult(int result) {
    this.result = result;
  }
}
