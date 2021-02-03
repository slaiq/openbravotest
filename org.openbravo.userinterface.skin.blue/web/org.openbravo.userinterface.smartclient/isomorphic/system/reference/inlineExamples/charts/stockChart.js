var stockChart = isc.FacetChart.create({
    ID: "stockChart",
    autoDraw: false,
    showTitle: false,
    showDataAxisLabel: false,
    canZoom: true,

    chartType: "Area",
    facets: [{ id: "date", title: "Day" }],
    valueProperty: "close",
    labelCollapseMode: "time",
    minLabelGap: 4,
    rotateLabels: "never",

    _loadingMessage: "Loading data ...",

    _errorMessage: "This test uses sample data provided by Yahoo&trade; Finance, " +
        "but Yahoo data is not currently available.  Refresh the sample to " +
        "try again.  You may need to wait a while for the Yahoo service to " +
        "become available again.",

    _showMessage : function (message, alignCenter) {
        var label = this._label;
        if (label == null) {
            label = this._label = isc.Label.create({
                autoDraw: false,
                contents: "",
                valign: "center",
                wrap: true,
                showEdges: false
            });
            stockChart.addChild(label);
        }

        label.setContents(message);

        var alignment = (alignCenter ? "center" : "left");
        label.setAlign(alignment);

        var widthRatio = 0.6, heightRatio = 0.6;
        var width = this.getWidth(), height = this.getHeight();
        label.setTop(Math.round((1 - heightRatio) / 2 * height));
        label.setLeft(Math.round((1 - widthRatio) / 2 * width));
        label.setHeight(Math.round(heightRatio * height));
        label.setWidth(Math.round(widthRatio * width));
        label.show();
    },

    _hideMessage : function () {
        if (this._label != null) {
            this._label.hide();
        }
    },

    _updateData : function (symbol, name) {
        // Clear the chart and display a loading message.
        this.destroyItems();
        this._showMessage(this._loadingMessage, true);

        var _this = this,
            stockDs = isc.DataSource.get("stockData");

        stockDs.fetchData({ symbol: symbol }, function (dsResponse, data) {
            if (isc.isAn.Array(data) && data.length > 0) {
                _this._hideMessage();
                _this.setProperty("title", name);
                _this.setData(data.sortByProperty("date", Date.compareDates));
            } else {
                _this._showMessage(_this._errorMessage, false);
                _this.setData([]);
            }
        });
    }
});

var symbolForm = isc.DynamicForm.create({
    items: [{
        name: "symbol",
        title: "Stock Symbol",
        editorType: "comboBox",
        autoFetchData: true,
        optionDataSource: "nasdaqSymbols",
        pickListWidth: 450,
        pickListFields: [{ name: "symbol"}, { name: "name" }],
        displayField: "name",
        valueField: "symbol",
        sortField: "symbol",
        changeOnKeypress: false,
        changed : "stockChart._updateData(value, item.getDisplayValue(value))",
        defaultToFirstOption: true,
        clearValue : function () {
            this.Super("clearValue", arguments);
            stockChart._updateData(this.getValue(), this.getDisplayValue());
        }
    }]
});

isc.VLayout.create({
    width: "100%",
    height: "100%",
    margin: 5,
    membersMargin: 20,
    members: [symbolForm, stockChart]
});
