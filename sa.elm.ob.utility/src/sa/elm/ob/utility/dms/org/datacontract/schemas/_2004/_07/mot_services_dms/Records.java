
package sa.elm.ob.utility.dms.org.datacontract.schemas._2004._07.mot_services_dms;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Records complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Records">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="DBName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="DBTable" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TableName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TableDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="TotalCount" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="PageCount" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="CurrentPage" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="NumberOfPages" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Record" type="{http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI}ArrayOfRecord" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Records", propOrder = {
    "dbName",
    "dbTable",
    "tableName",
    "tableDescription",
    "totalCount",
    "pageCount",
    "currentPage",
    "numberOfPages",
    "record"
})
public class Records {

    @XmlElementRef(name = "DBName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> dbName;
    @XmlElementRef(name = "DBTable", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> dbTable;
    @XmlElementRef(name = "TableName", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> tableName;
    @XmlElementRef(name = "TableDescription", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> tableDescription;
    @XmlElementRef(name = "TotalCount", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> totalCount;
    @XmlElementRef(name = "PageCount", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> pageCount;
    @XmlElementRef(name = "CurrentPage", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> currentPage;
    @XmlElementRef(name = "NumberOfPages", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<String> numberOfPages;
    @XmlElementRef(name = "Record", namespace = "http://schemas.datacontract.org/2004/07/MOT.Services.DMS.BI", type = JAXBElement.class, required = false)
    protected JAXBElement<ArrayOfRecord> record;

    /**
     * Gets the value of the dbName property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getDBName() {
        return dbName;
    }

    /**
     * Sets the value of the dbName property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setDBName(JAXBElement<String> value) {
        this.dbName = value;
    }

    /**
     * Gets the value of the dbTable property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getDBTable() {
        return dbTable;
    }

    /**
     * Sets the value of the dbTable property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setDBTable(JAXBElement<String> value) {
        this.dbTable = value;
    }

    /**
     * Gets the value of the tableName property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTableName() {
        return tableName;
    }

    /**
     * Sets the value of the tableName property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTableName(JAXBElement<String> value) {
        this.tableName = value;
    }

    /**
     * Gets the value of the tableDescription property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTableDescription() {
        return tableDescription;
    }

    /**
     * Sets the value of the tableDescription property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTableDescription(JAXBElement<String> value) {
        this.tableDescription = value;
    }

    /**
     * Gets the value of the totalCount property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getTotalCount() {
        return totalCount;
    }

    /**
     * Sets the value of the totalCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setTotalCount(JAXBElement<String> value) {
        this.totalCount = value;
    }

    /**
     * Gets the value of the pageCount property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getPageCount() {
        return pageCount;
    }

    /**
     * Sets the value of the pageCount property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setPageCount(JAXBElement<String> value) {
        this.pageCount = value;
    }

    /**
     * Gets the value of the currentPage property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getCurrentPage() {
        return currentPage;
    }

    /**
     * Sets the value of the currentPage property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setCurrentPage(JAXBElement<String> value) {
        this.currentPage = value;
    }

    /**
     * Gets the value of the numberOfPages property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public JAXBElement<String> getNumberOfPages() {
        return numberOfPages;
    }

    /**
     * Sets the value of the numberOfPages property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link String }{@code >}
     *     
     */
    public void setNumberOfPages(JAXBElement<String> value) {
        this.numberOfPages = value;
    }

    /**
     * Gets the value of the record property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfRecord }{@code >}
     *     
     */
    public JAXBElement<ArrayOfRecord> getRecord() {
        return record;
    }

    /**
     * Sets the value of the record property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link ArrayOfRecord }{@code >}
     *     
     */
    public void setRecord(JAXBElement<ArrayOfRecord> value) {
        this.record = value;
    }

}
