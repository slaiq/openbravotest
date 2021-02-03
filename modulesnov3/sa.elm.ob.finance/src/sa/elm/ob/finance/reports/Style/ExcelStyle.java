package sa.elm.ob.finance.reports.Style;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;

/**
 * @author Gopalakrishnan created on 29/12/2016
 * 
 */

public class ExcelStyle {
  public static HSSFCellStyle headerfontColor(HSSFWorkbook workbook, short fontSize,
      short colorindex) {
    HSSFCellStyle style = workbook.createCellStyle();
    HSSFFont font = workbook.createFont();
    font.setColor(colorindex);
    font.setFontName("Calibri");
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    font.setFontHeightInPoints((short) fontSize);
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.ALIGN_CENTER);
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle headerfontColorHeader(HSSFWorkbook workbook, HSSFCellStyle style,
      short fontSize, short colorindex) {

    HSSFFont font = workbook.createFont();
    font.setColor(colorindex);
    font.setFontName("Calibri");
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    font.setFontHeightInPoints((short) fontSize);
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.ALIGN_CENTER);
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle headerfontColorBreakDown(HSSFWorkbook workbook, HSSFCellStyle style,
      short fontSize, short colorindex) {

    HSSFFont font = workbook.createFont();
    font.setColor(colorindex);
    font.setFontName("Calibri");
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    font.setFontHeightInPoints((short) fontSize);
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.ALIGN_CENTER);
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle headerfontColorAccount(HSSFWorkbook workbook, HSSFCellStyle style,
      short fontSize, short colorindex) {

    HSSFFont font = workbook.createFont();
    font.setColor(colorindex);
    font.setFontName("Calibri");
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    font.setFontHeightInPoints((short) fontSize);
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.ALIGN_CENTER);
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle headerfontColorSubAccount(HSSFWorkbook workbook, HSSFCellStyle style,
      short fontSize, short colorindex) {

    HSSFFont font = workbook.createFont();
    font.setColor(colorindex);
    font.setFontName("Calibri");
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    font.setFontHeightInPoints((short) fontSize);
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.ALIGN_CENTER);
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle headerfontColorElement(HSSFWorkbook workbook, short fontSize,
      short colorindex) {

    HSSFCellStyle style = workbook.createCellStyle();
    HSSFFont font = workbook.createFont();
    font.setColor(colorindex);
    font.setFontName("Calibri");
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    font.setFontHeightInPoints((short) fontSize);
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.ALIGN_CENTER);
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle fillColorBackGround(HSSFWorkbook workbook, short fontSize,
      short colorindex, short backColorIndex) {

    HSSFCellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(backColorIndex);
    style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
    HSSFFont font = workbook.createFont();
    font.setColor(colorindex);
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    font.setFontHeightInPoints((short) fontSize);
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.ALIGN_CENTER);
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle fillBotttomColorBackGround(HSSFWorkbook workbook, short fontSize,
      short colorindex, short backColorIndex) {

    HSSFCellStyle style = workbook.createCellStyle();
    DataFormat format = workbook.createDataFormat();
    style.setFillForegroundColor(backColorIndex);
    style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
    HSSFFont font = workbook.createFont();
    font.setColor(colorindex);
    font.setBoldweight(Font.BOLDWEIGHT_BOLD);
    font.setFontHeightInPoints((short) fontSize);
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.ALIGN_CENTER);
    style.setDataFormat(format.getFormat("#,##0.00"));
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle amountCalibriFont(HSSFWorkbook workbook, HSSFCellStyle style) {

    HSSFFont font = workbook.createFont();
    DataFormat format = workbook.createDataFormat();
    style.setDataFormat(format.getFormat("#,##0.00"));
    font.setFontName("Calibri");
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setFont(font);
    return style;
  }

  public static HSSFCellStyle fullborderstyle(HSSFWorkbook workbook) {

    HSSFCellStyle style = workbook.createCellStyle();
    style.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    style.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
    style.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);
    style.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);

    return style;
  }

  public static HSSFCellStyle halfborderstyle(HSSFWorkbook workbook) {

    HSSFCellStyle style = workbook.createCellStyle();
    style.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    style.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);

    return style;
  }

  public static HSSFCellStyle leftborderstyle(HSSFWorkbook workbook) {

    HSSFCellStyle style = workbook.createCellStyle();
    style.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    style.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
    style.setBorderLeft(HSSFCellStyle.BORDER_MEDIUM);

    return style;
  }

  public static HSSFCellStyle rightborderstyle(HSSFWorkbook workbook) {

    HSSFCellStyle style = workbook.createCellStyle();
    style.setBorderBottom(HSSFCellStyle.BORDER_MEDIUM);
    style.setBorderTop(HSSFCellStyle.BORDER_MEDIUM);
    style.setBorderRight(HSSFCellStyle.BORDER_MEDIUM);

    return style;
  }

  public static CellStyle rightAlign(HSSFWorkbook workbook) {

    CellStyle rightAligned = workbook.createCellStyle();
    rightAligned.setAlignment(CellStyle.ALIGN_RIGHT);

    return rightAligned;
  }

  public static CellStyle centerAlign(HSSFWorkbook workbook) {

    CellStyle rightAligned = workbook.createCellStyle();
    rightAligned.setAlignment(CellStyle.ALIGN_CENTER);

    return rightAligned;
  }

  public static HSSFCellStyle fontSize(HSSFWorkbook workbook, short fontSize) {

    HSSFCellStyle style = workbook.createCellStyle();
    HSSFFont font = workbook.createFont();
    font.setFontHeightInPoints((short) fontSize);
    style.setFont(font);

    return style;

  }

  public static HSSFCellStyle verticalAlign(HSSFWorkbook workbook) {

    HSSFCellStyle style = workbook.createCellStyle();
    style.setVerticalAlignment(HSSFCellStyle.VERTICAL_TOP);
    return style;

  }

  public static HSSFCellStyle foreGroundColor(HSSFWorkbook workbook, short colorindex) {

    HSSFCellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(colorindex);
    style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

    return style;
  }

}
