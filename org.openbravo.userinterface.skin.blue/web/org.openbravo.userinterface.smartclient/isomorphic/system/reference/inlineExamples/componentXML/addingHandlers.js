isc.Canvas.create({
    ID: "container"
});

isc.RPCManager.loadScreen("addingHandlers",function (response) {
    saveForm = response.getByLocalId("saveForm");
    response.getByLocalId("saveButton").addProperties({click: function () {
        if (saveForm.getValue("inStock") == false 
                && saveForm.getValue("nextShipment") == null) {
            isc.warn("New stock items which are not already stocked must have a Stock Date");
        }
    }})
    container.addChild(response);
});