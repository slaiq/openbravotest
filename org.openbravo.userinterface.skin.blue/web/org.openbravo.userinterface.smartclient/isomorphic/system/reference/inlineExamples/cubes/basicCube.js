isc.CubeGrid.create({
    ID: "basicCubeGrid",
    data: productData,
    width:"80%",
    hideEmptyFacetValues:true,
    
    formatCellValue:"isc.NumberUtil.format(value, '\u00A4,0.00')",

    columnFacets:["quarter","month","metric"],
    rowFacets:["region","product"]
});
