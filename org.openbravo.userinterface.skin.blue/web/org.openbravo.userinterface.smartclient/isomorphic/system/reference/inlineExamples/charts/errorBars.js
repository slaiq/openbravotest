var productRevenue = isc.DataSource.get("productRevenue");

isc.FacetChart.create({
    ID: "errorBars",
    title: "Error Bars",
    chartType: "Line",
    metricFacetId: "errorMetrics",
    lowErrorMetric: "lowValue",
    highErrorMetric: "highValue",
    showDataPoints: true,
    stacked: false,
    showLegend: false,

    facets: [{
        id: "Time"
    }, {
        id: "Regions"
    }, {
        id: "errorMetrics",
        inlinedValues: true,
        values: [{ id: "value" }, { id: "lowValue" }, { id: "highValue" }]
    }],

    updateData : function () {
        var criteria = {
            Scenarios: "Actual",
            Products: "sum",
            Regions: "North"
        };
        productRevenue.fetchData(criteria, "errorBars.setData(data)");
    },

    randomErrorPercentage : function () {
        return Math.max(0.05, Math.random() * 0.15);
    },

    // Override setData() to add random error bars to the line chart.
    setData : function (data) {
        if (data != null) {
            for (var i = 0; i < data.length; i++) {
                var record = data[i];
                record.lowValue = record.value * (1 - this.randomErrorPercentage());
                record.highValue = record.value * (1 + this.randomErrorPercentage());
            }
        }
        return this.Super("setData", arguments);
    },

    initWidget : function () {

        // Chart can auto-derive facet values from data.  The productRevenue
        // data source has values that are parents/sums of other values.  For example,
        // the value for "Q4-2004" is a sum of the values for "10/1/2004", "11/1/2004",
        // and "12/1/2004".  The sum values need to be excluded from the time facet
        // to avoid having a bizarre chart comparing sums to parts.
        var timeFacet = this.facets.find("id", "Time"),
            timeFacetValueIds = [
                "12/1/2004", "11/1/2004", "10/1/2004", "9/1/2004", "8/1/2004",
                "7/1/2004", "6/1/2004", "5/1/2004", "4/1/2004", "3/1/2004",
                "2/1/2004", "1/1/2004", "12/1/2003", "11/1/2003", "10/1/2003",
                "9/1/2003", "8/1/2003", "7/1/2003", "6/1/2003", "5/1/2003",
                "4/1/2003", "3/1/2003", "2/1/2003", "1/1/2003", "12/1/2002",
                "11/1/2002", "10/1/2002", "9/1/2002", "8/1/2002", "7/1/2002",
                "6/1/2002", "5/1/2002", "4/1/2002", "3/1/2002", "2/1/2002",
                "1/1/2002"];

        timeFacet.values = [];

        for (var i = 0; i < timeFacetValueIds.length; ++i) {
            var valueId = timeFacetValueIds[i],
                year = valueId.substring(valueId.lastIndexOf("/") + 1),
                month = valueId.substring(0, valueId.indexOf("/")),
                quarter = Math.ceil(parseInt(month) / 3);
            timeFacet.values[i] = {
                id: valueId,
                title: valueId,
                parentId: "Q" + quarter + "-" + year
            };
        }

        return this.Super("initWidget", arguments);
    }
});

isc.VLayout.create({
    width: "100%",
    height: "100%",
    members: [errorBars]
});

errorBars.updateData();
