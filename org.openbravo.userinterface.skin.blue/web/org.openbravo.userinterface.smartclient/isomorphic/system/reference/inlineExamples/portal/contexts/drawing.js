// Sample drawing shown by default
var sampleDrawing = "<DrawOval left=\"91\" top=\"99\" width=\"108\" height=\"53\" ID=\"DrawOval1\">\r\n    <lineWidth>1<\/lineWidth>\r\n    <fillGradient ref=\"oval\"\/>\r\n    <shadow>\r\n        <color>#333333<\/color>\r\n        <blur>2<\/blur>\r\n        <offset x=\"1\" y=\"1\"\/>\r\n    <\/shadow>\r\n    <keepInParentRect>true<\/keepInParentRect>\r\n<\/DrawOval>\r\n\r\n\r\n<DrawRect left=\"133\" top=\"263\" width=\"50\" height=\"50\" ID=\"DrawRect3\">\r\n    <lineWidth>1<\/lineWidth>\r\n    <fillGradient ref=\"diamond\"\/>\r\n    <shadow>\r\n        <color>#333333<\/color>\r\n        <blur>2<\/blur>\r\n        <offset x=\"1\" y=\"1\"\/>\r\n    <\/shadow>\r\n    <rotation>45<\/rotation>\r\n    <keepInParentRect>true<\/keepInParentRect>\r\n    \r\n<\/DrawRect>\r\n\r\n\r\n<DrawRect left=\"387\" top=\"226\" width=\"140\" height=\"52\" ID=\"DrawRect4\">\r\n    <rounding>0.25<\/rounding>\r\n    <lineWidth>1<\/lineWidth>\r\n    <fillGradient ref=\"rect\"\/>\r\n    <shadow>\r\n        <color>#333333<\/color>\r\n        <blur>2<\/blur>\r\n        <offset x=\"1\" y=\"1\"\/>\r\n    <\/shadow>\r\n    <keepInParentRect>true<\/keepInParentRect>\r\n    \r\n<\/DrawRect>\r\n\r\n\r\n<DrawPane ID=\"DrawPane0\" width=\"100%\" height=\"100%\" autoDraw=\"false\">\r\n    <drawItems><DrawItem ref=\"DrawOval1\"\/><DrawItem ref=\"DrawRect3\"\/><DrawItem ref=\"DrawRect4\"\/>\r\n    <\/drawItems>\r\n    <gradients>\r\n        <SimpleGradient id=\"oval\">\r\n            <direction>90<\/direction>\r\n            <startColor>#ffffff<\/startColor>\r\n            <endColor>#99ccff<\/endColor>\r\n<\/SimpleGradient>\r\n        <SimpleGradient id=\"diamond\">\r\n            <direction>90<\/direction>\r\n            <startColor>#d3d3d3<\/startColor>\r\n            <endColor>#666699<\/endColor>\r\n<\/SimpleGradient>\r\n        <SimpleGradient id=\"rect\">\r\n            <direction>90<\/direction>\r\n            <startColor>#f5f5f5<\/startColor>\r\n            <endColor>#a9b3b8<\/endColor>\r\n<\/SimpleGradient>\r\n    <\/gradients>\r\n    \r\n<\/DrawPane>\r\n";

// The following gradients are shared by various DrawItem shapes and are 
// applied to the empty DrawPane as well as the palette tile DrawPanes
var commonGradients = [
    { id: "oval", direction: 90, startColor: "#ffffff", endColor: "#99ccff" },
    { id: "diamond", direction: 90, startColor: "#d3d3d3", endColor: "#666699" },
    { id: "rect", direction: 90, startColor: "#f5f5f5", endColor: "#a9b3b8" }
];

// Empty DrawPane palette node use when clearing edit canvas
var emptyDrawPanePaletteNode = {
    type: "DrawPane",
    defaults: {
        width: "100%",
        height: "100%",
        gradients: commonGradients
    }
};

// Define a class for all of the tiles in the TilePalette defined below.  Each
// tile has a DrawPane that is used to render a single DrawItem.
isc.defineClass("SimpleDrawItemTile", "SimpleTile").addProperties({
    initWidget : function () {
        this._drawPane = isc.DrawPane.create({
            autoDraw: false,
            width: "100%",
            height: "100%",
            gradients: commonGradients
        });
        this.children = [this._drawPane];

        this.Super("initWidget", arguments);

        this._record = this.getRecord();
    },

    getInnerHTML : function () {
        return isc.Canvas.getInstanceProperty("getInnerHTML").call(this, arguments);
    },

    _drawRecord : function (record) {
        var tilePalette = this.creator,
            drawItem = tilePalette.makeEditNode(record).liveObject;

        if (!isc.isAn.Instance(drawItem)) {
            drawItem = isc[drawItem._constructor].create(isc.addProperties({}, drawItem, {
                autoDraw: false
            }));
        }

        this._drawPane.addDrawItem(drawItem);
    },

    draw : function () {
        var ret = this.Super("draw", arguments);
        this._drawRecord(this.getRecord());
        return ret;
    },

    redraw : function () {
        var drawPane = this._drawPane,
            record = this.getRecord();

        if (record !== this._record) {
            drawPane.erase();

            this._drawRecord(record);
            this._record = record;
        }

        return this.Super("redraw", arguments);
    }
});


isc.TilePalette.create({
    ID: "tilePalette",
    width: 300,
    tileWidth: 80,
    tileHeight: 80,
    canDragTilesOut: true,

    tileConstructor: "SimpleDrawItemTile",
    fields: [{
        name: "type"
    }, {
        name: "title",
        title: "Component"
    }],

    initWidget : function () {
        // We are supplying the component data inline for this example.
        // However, the TilePalette is a subclass of TileGrid, so you could
        // also use a dataSource.
        this.data = this.getData(this.tileWidth, this.tileHeight, 3);

        // Set default properties on the DrawItems offered in the palette.
        var defaultDrawItemProperties = {
            keepInParentRect: true,
            lineWidth: 1,
            shadow: { color: '#333333', blur: 2, offset: [1,1] }
        };
        for (var i = 0, len = this.data.length; i < len; ++i) {
            var defaults = this.data[i].defaults;
            if (defaults == null) {
                defaults = this.data[i].defaults = {};
            }
            isc.addDefaults(defaults, defaultDrawItemProperties);
        }

        this.Super("initWidget", arguments);
    },

    // Creates PaletteNodes for each of nine different types of DrawItems.  The
    // defaults of the nodes are set so that the shapes will fit in the grid
    // tiles.
    getData : function (tileWidth, tileHeight, topPadding, leftPadding, rightPadding, bottomPadding) {
        if (tileHeight == null) tileHeight = tileWidth;

        if (topPadding == null) topPadding = 2;
        if (leftPadding == null) leftPadding = topPadding;
        if (rightPadding == null) rightPadding = leftPadding;
        if (bottomPadding == null) bottomPadding = topPadding;

        tileWidth -= (leftPadding + rightPadding);
        tileHeight -= (topPadding + bottomPadding);

        var xc = leftPadding + (tileWidth / 2),
            yc = topPadding + (tileHeight / 2),
            width = tileWidth - leftPadding - rightPadding,
            height = tileHeight - topPadding - bottomPadding,
            center = [Math.round(xc), Math.round(yc)],

            // variables for the DrawRect:
            smallAngle = Math.PI / 5,
            rectPoints = this.getPolygonPoints(
                width, height, xc, yc,
                [smallAngle, Math.PI - smallAngle, Math.PI + smallAngle, -smallAngle]),
            rectTop = rectPoints[1][1],
            rectLeft = rectPoints[1][0],
            rectWidth = rectPoints[3][0] - rectLeft,
            rectHeight = rectPoints[3][1] - rectTop;

            // variable for the DrawSector:
            radius = Math.min(width, height);

        return [{
            title: "Line",
            type: "DrawLine",
            defaults: {
                startPoint: [Math.round(xc - width / 2), Math.round(yc - height / 2)],
                endPoint: [Math.round(xc + width / 2), Math.round(yc + height / 2)]
            }
        }, {
            title: "Line w/arrow",
            type: "DrawLine",
            defaults: {
                startPoint: [Math.round(xc - width / 2), Math.round(yc - height / 2)],
                endPoint: [Math.round(xc + width / 2), Math.round(yc + height / 2)],
                lineWidth: 1,
                endArrow: "block"
            }
        }, {
            title: "Line w/two arrows",
            type: "DrawLine",
            defaults: {
                startPoint: [Math.round(xc - width / 2), Math.round(yc - height / 2)],
                endPoint: [Math.round(xc + width / 2), Math.round(yc + height / 2)],
                startArrow: "block",
                endArrow: "block"
            }
        }, {
            title: "Curve",
            type: "DrawCurve",
            defaults: isc.addProperties(
                {}, this.scaleAndCenterBezier(width, height-20, xc, yc, [200, 50], [300, 150], [250, 0], [250, 200]))
        }, {
            title: "Curve w/arrow",
            type: "DrawCurve",
            defaults: isc.addProperties(
                { endArrow: "block" }, this.scaleAndCenterBezier(width, height-20, xc, yc, [200, 50], [300, 150], [250, 0], [250, 200]))
        }, {
            title: "Curve w/two arrows",
            type: "DrawCurve",
            defaults: isc.addProperties(
                { startArrow: "block",
                    endArrow: "block"
                }, this.scaleAndCenterBezier(width, height-20, xc, yc, [200, 50], [300, 150], [250, 0], [250, 200]))
        }, {
            title: "Line Path",
            type: "DrawLinePath",
            defaults: {
                startPoint: [Math.round(xc - width / 2), Math.round(yc - (height - 10) / 2)],
                endPoint: [Math.round(xc + width / 2), Math.round(yc + (height - 20) / 2)],
                endArrow: null
            }
        }, {
            title: "Line Path w/arrow",
            type: "DrawLinePath",
            defaults: {
                startPoint: [Math.round(xc - width / 2), Math.round(yc - (height - 10) / 2)],
                endPoint: [Math.round(xc + width / 2), Math.round(yc + (height - 20) / 2)],
                endArrow: "block"
            }
        }, {
            title: "Line Path w/two arrows",
            type: "DrawLinePath",
            defaults: {
                startPoint: [Math.round(xc - width / 2), Math.round(yc - (height - 10) / 2)],
                endPoint: [Math.round(xc + width / 2), Math.round(yc + (height - 20) / 2)],
                startArrow: "block",
                endArrow: "block"
            }
        }, {
            title: "Rectangle",
            type: "DrawRect",
            defaults: {
                top: rectTop,
                left: rectLeft,
                width: rectWidth,
                height: rectHeight,
                fillGradient: "rect"
            }
        }, {
            title: "Rounded Rectangle",
            type: "DrawRect",
            defaults: {
                top: rectTop,
                left: rectLeft,
                width: rectWidth,
                height: rectHeight,
                rounding: 0.25,
                fillGradient: "rect"
            }
        }, {
            title: "Oval",
            type: "DrawOval",
            defaults: {
                top: rectTop,
                left: rectLeft,
                width: rectWidth,
                height: rectHeight,
                fillGradient: "oval"
            }
        }, {
            title: "Triangle",
            type: "DrawTriangle",
            defaults: {
                points: this.getRegularPolygonPoints(3, width, height, xc, yc, Math.PI / 2)
            }
        }, {
            title: "Diamond",
            type: "DrawRect",
            defaults: {
                top: 15,
                left: 15,
                width: 50,
                height: 50,
                rotation: 45,
                fillGradient: "diamond"
            }
        }, {
            title: "Label",
            type: "DrawLabel",
            defaults: {
                contents: "Text",
                alignment: "center",
                left: xc/2,
                top: yc/2
            }
        }];
    },

    // Define a cubic polynomial expression used in calculations for Bezier
    // curves.
    bezier : function (x, y, z, w, t) {
        return (((x - 3 * (y - z) - w) * t + 3 * (y - z - z + w)) * t + 3 * (z - w)) * t + w;
    },

    // Computes the minimum and maximum value of the polynomial defined in
    // bezier(), for 0 <= t <= 1.
    bezierExtrema : function (x, y, z, w) {
        var a = (x - 3 * y + 3 * z - w),
            b = (2 * y - 4 * z + 2 * w),
            c = (-w + z),
            discriminant = (b * b - 4 * a * c),
            list = [0, 1];

        if (a == 0) {
            if (b != 0) {
                var root = -c / b;
                root = Math.max(0, Math.min(1, root));
                if (!list.contains(root)) {
                    list.push(root);
                }
            }
        } else if (discriminant >= 0) {
            var sqrtDiscriminant = Math.sqrt(discriminant),
                root1 = (-b + sqrtDiscriminant) / (2 * a),
                root2 = (-b - sqrtDiscriminant) / (2 * a);

            root1 = Math.max(0, Math.min(1, root1));
            root2 = Math.max(0, Math.min(1, root2));
            if (!list.contains(root1)) {
                list.push(root1);
            }
            if (!list.contains(root2)) {
                list.push(root2);
            }
        }

        var min = this.bezier(x, y, z, w, list[0]),
            max = min;

        for (var i = 1; i < list.length; ++i) {
            var value = this.bezier(x, y, z, w, list[i]);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return { min: min, max: max };
    },

    // Computes the bounding box (the array [min. x, min. y, max. x, max. y])
    // of a Bezier curve with endpoints p0 and p3 and control points p1 and p2.
    getBezierBoundingBox : function (p0, p1, p2, p3) {
        var xExtrema = this.bezierExtrema(p0[0], p1[0], p2[0], p3[0]),
            yExtrema = this.bezierExtrema(p0[1], p1[1], p2[1], p3[1]);

        return [xExtrema.min, yExtrema.min, xExtrema.max, yExtrema.max];
    },

    // Like scaleAndCenter() (defined above) but works with Bezier curves rather
    // than arrays of points.
    scaleAndCenterBezier : function (width, height, xc, yc, startPoint, endPoint, controlPoint1, controlPoint2) {
        var box = this.getBezierBoundingBox(startPoint, controlPoint1, controlPoint2, endPoint),
            maxX = box[2],
            minX = box[0],
            maxY = box[3],
            minY = box[1],
            scaleX = width / (maxX - minX),
            scaleY = height / (maxY - minY),
            x0 = (minX + maxX) / 2,
            y0 = (minY + maxY) / 2;
        return {
            startPoint: [
                Math.round(xc + scaleX * (startPoint[0] - x0)),
                Math.round(yc + scaleY * (startPoint[1] - y0))],
            endPoint: [
                Math.round(xc + scaleX * (endPoint[0] - x0)),
                Math.round(yc + scaleY * (endPoint[1] - y0))],
            controlPoint1: [
                Math.round(xc + scaleX * (controlPoint1[0] - x0)),
                Math.round(yc + scaleY * (controlPoint1[1] - y0))],
            controlPoint2: [
                Math.round(xc + scaleX * (controlPoint2[0] - x0)),
                Math.round(yc + scaleY * (controlPoint2[1] - y0))]
        };
    },

    // Calls getPolygonPoints() with angles spread even over the full 360 degrees.
    getRegularPolygonPoints : function (n, width, height, xc, yc, startAngle) {
        var theta = 2 * Math.PI / n,
            angles = new Array(n);
        for (var i = 0; i < n; ++i) {
            angles[i] = startAngle + i * theta;
        }
        return this.getPolygonPoints(width, height, xc, yc, angles);
    },

    // Computes an array of Points for a polygon that has an equal distance
    // from its center to any of its vertices to fit in the given width and
    // height.
    getPolygonPoints : function (width, height, xc, yc, angles) {
        var n = angles.length,
            maxSin = -1, minSin = 1, maxCos = -1, minCos = 1,
            points = new Array(n);

        for (var i = 0; i < n; ++i) {
            var angle = angles[i],
                sin = Math.sin(angle),
                cos = Math.cos(angle);

            points[i] = [cos, sin];

            maxSin = Math.max(maxSin, sin);
            minSin = Math.min(minSin, sin);
            maxCos = Math.max(maxCos, cos);
            minCos = Math.min(minCos, cos);
        }

        var t = Math.min(
                width / (maxCos - minCos),
                height / (maxSin - minSin)),
            xCenter = xc - width / 2 + (width - t * (maxCos - minCos)) / 2 - t * minCos,
            yCenter = yc - height / 2 + (height - t * (maxSin - minSin)) / 2 + t * maxSin;

        for (var i = 0; i < n; ++i) {
            var point = points[i];
            point[0] = Math.round(xCenter + t * point[0]);
            point[1] = Math.round(yCenter - t * point[1]);
        }

        return points;
    }
});

isc.DynamicForm.create({
    ID: "drawItemProperties",
    width: "100%",
    numCols: 8,
    colWidths: [100,100,100,50,50,50,50,50],
    titleOrientation: "top",
    fields: [
        { name: "lineColor", title: "Line color",
            type: "color", supportsTransparency: true,
            pickerColorSelected : function (color, opacity) {
                this.Super("pickerColorSelected", arguments);
                this.form.setDrawItemProperty("lineColor", color, "setLineColor");
                this.form.setDrawItemProperty("lineOpacity", opacity, "setLineOpacity");
            }
        },
        { name: "fillColor", title: "Fill color",
            type: "color", supportsTransparency: true,
            pickerColorSelected : function (color, opacity) {
            this.Super("pickerColorSelected", arguments);
                this.form.setDrawItemProperty("fillGradient", null, "setFillGradient");
                this.form.setDrawItemProperty("fillColor", color, "setFillColor");
                this.form.setDrawItemProperty("fillOpacity", (opacity != null ? opacity/100 : 1.0), "setFillOpacity");
            }
        },
        { name: "arrows", title: "Arrows",
            type: "select",
            valueMap: [ "None", "Start", "End", "Both" ],
            changed : function (form, item, value) {
                this.form.setDrawItemProperty("startArrow", (value == "Start" || value == "Both" ? "block" : null), "setStartArrow");
                this.form.setDrawItemProperty("endArrow", (value == "End" || value == "Both" ? "block" : null), "setEndArrow");
            }
        },
        { editorType: "SpacerItem", showTitle: false },
        { name: "sendToBack", title: "Send to back", vAlign: "bottom",
            type: "button", startRow: false, endRow: false,
            click: function () {
                this.form.callDrawItemMethod("sendToBack");
            }
        },
        { name: "bringToFront", title: "Bring to front", vAlign: "bottom",
            type: "button", startRow: false, endRow: false,
            click: function () {
                this.form.callDrawItemMethod("bringToFront");
            }
        },
        { editorType: "SpacerItem", showTitle: false },
        { name: "removeItem", title: "Remove", vAlign: "bottom",
            type: "button", startRow: false, endRow: false,
            click: function () {
                this.form.removeItem();
            }
        },
    ],
    initWidget : function () {
        this.Super("initWidget", arguments);

        // Set initial field values/state 
        this.selectedComponentsUpdated();
    },
    removeItem : function () {
        var selection = this.getSelectedItems();
        if (!selection) return;
        for (var i = 0; i < selection.length; i++) {
            var item = selection[i];

            // Remove node from editContext and destroy it
            this.editContext.removeNode(item.editNode);
            item.destroy();
        }
    },
    callDrawItemMethod : function (methodName) {
        var selection = this.getSelectedItems();
        if (!selection) return;
        for (var i = 0; i < selection.length; i++) {
            var item = selection[i];

            if (item[methodName]) item[methodName]();
        }
    },
    setDrawItemProperty : function (property, value, methodName) {
        var selection = this.getSelectedItems();
        if (!selection) return;
        for (var i = 0; i < selection.length; i++) {
            var item = selection[i];

            if (item[methodName]) {
                var properties = {};
                properties[property] = value;
                this.editContext.setNodeProperties(item.editNode, properties);
            }
        }
    },
    getSelectedItems : function () {
        return (this.editContext ? this.editContext.getSelectedComponents() : []);
    },
    selectedComponentsUpdated : function () {
        var selection = this.getSelectedItems();
        if (selection.length == 0 || selection.length > 1) {
            // No selection or multiple selection
            this.getField("lineColor").disable();
            this.getField("fillColor").disable();
            this.getField("arrows").disable();
            var disabled = (selection.length == 0);
            this.getField("sendToBack").setDisabled(disabled);
            this.getField("bringToFront").setDisabled(disabled);
            this.getField("removeItem").setDisabled(disabled);

            this.clearValue("lineColor");
            this.clearValue("fillColor");
            this.clearValue("arrows");
        } else {
            this.getField("sendToBack").enable();
            this.getField("bringToFront").enable();
            this.getField("removeItem").enable();

            // TODO Enable only property controls that are applicable to selection
            this.getField("lineColor").enable();
            this.getField("fillColor").enable();
            this.getField("arrows").enable();

            var item = selection[0],
                arrows = (item.startArrow && item.endArrow ? "Both" : (item.startArrow ? "Start" : (item.endArrow ? "End" : "None")))
            ;
            this.setValue("lineColor", item.lineColor);
            this.setValue("fillColor", item.fillColor);
            this.setValue("arrows", arrows);
        }
    }
});

// Define class implementing the EditContext to instantiate as-is
isc.ClassFactory.defineClass("HiddenEditContext", "EditContext");


// The editCanvas is the root component in which the items can be placed.
isc.Canvas.create({
    ID: "editCanvas",
    border: "1px solid black",
    width: "100%",
    height: "100%"
});

var editContext = isc.HiddenEditContext.create({
    height: "25%",
    defaultPalette: tilePalette,

    rootComponent: {
        type: "Canvas",
        liveObject: editCanvas
    },

    nodeAdded : function (newNode, parentNode, rootNode) {
        // Enable editMode on new node if parent editMode is enabled
        if (parentNode && this.isNodeEditingOn(parentNode)) {
            // Install selection and selection handler on the DrawPane
            if (newNode.type == "DrawPane") {
                if (!newNode.editProxyProperties) newNode.editProxyProperties = {};
                newNode.editProxyProperties.enableComponentSelection = true;
            }
            this.enableEditing(newNode);
        }
        // Save first node added to the tree. That node is where
        // double-clicked paletteNodes should be placed by default
        // (i.e. defaultParent).
        if (!this.defaultParent) this.defaultParent = newNode;
    },

    //  Return the defaultParent saved in nodeAdded() above if available
    getDefaultParent : function (newNode, returnNullIfNoSuitableParent) {
        return this.defaultParent || this.getRootEditNode();
    },
    
    destroyAll : function () {
        this.defaultParent = null;
        this.Super("destroyAll", arguments);
    },
    selectedComponentsUpdated : function () {
        drawItemProperties.selectedComponentsUpdated();
    }
});

// Set the defaultEditContext on palette which is used when double-clicking on components.
tilePalette.setDefaultEditContext(editContext);

// Place editCanvas into editMode to allow paletteNode drops
var editCanvasEditNode = editContext.getRootEditNode();
editCanvas.setEditMode(true, editContext, editCanvasEditNode);

// The above use of an edit canvas and edit context can be replaced by
// an EditPane which combines these separate parts into a single component.

drawItemProperties.editContext = editContext;


// Place sample drawing into editContext
editContext.addPaletteNodesFromXML(sampleDrawing);
////Place base component (DrawPane) into editContext.
//editContext.addFromPaletteNode(emptyDrawPanePaletteNode);


// Layout for the example. The layouts are nested because this
// is used as a basis for other examples, in which some
// user interface elements are added.
isc.VLayout.create({
    ID: "vlayout",
    width: "100%",
    height: "100%",
    membersMargin: 10,
    members: [
        isc.HLayout.create({
            ID: "hlayout",
            membersMargin: 20,
            width: "100%",
            height: "100%",
            members: [
                tilePalette,
                isc.VLayout.create({
                    width: "100%",
                    membersMargin: 5,
                    members: [
                        editCanvas,
                        drawItemProperties
                    ]
                })
            ]
        })
    ]
});


isc.Button.create({
    ID: "showComponentXMLButton",
    title: "Show Component XML",
    autoFit: true,

    click : function () {
        var paletteNodes = editContext.serializeAllEditNodes();

        var syntaxHiliter = isc.XMLSyntaxHiliter.create(),
            formattedText = syntaxHiliter.hilite(paletteNodes),
            window = isc.Window.create({
                width: Math.round(vlayout.width / 2),
                defaultHeight: Math.round(vlayout.height * 2/3),
                title: "Component XML",
                autoCenter: true,
                showMinimizeButton: false,
                canDragResize: true,
                isModal: true,
                keepInParentRect: true,
                items: [
                    isc.Canvas.create({ contents: formattedText})
                ]
            })
        ;

        window.show();
    }
});

isc.Button.create({
    ID: "reloadSampleButton",
    title: "Reload Sample Drawing",
    autoFit: true,

    click : function () {
        // Destroy all the nodes
        editContext.destroyAll();

        // Recreate sample drawing
        editContext.addPaletteNodesFromXML(sampleDrawing);
    }
});

isc.Button.create({
    ID: "clearButton",
    title: "Clear Drawing",
    autoFit: true,

    click : function () {
        // Destroy all the nodes
        editContext.destroyAll();

        // Create default DrawPane
        editContext.addFromPaletteNode(emptyDrawPanePaletteNode);
    }
});


// This button will destroy the EditDrawPane and then recreate it from saved state.
isc.Button.create({
    ID: "destroyAndRecreateButton",
    title: "Destroy and Recreate",
    autoFit: true,

    click : function () {
        // We save the editPane node data in a variable
        var paletteNodes = editContext.serializeAllEditNodes();

        // Animate the disappearance of the editCanvas, since otherwise
        // everything happens at once.
        editCanvas.animateFade(0, function () {
            // Once the animation is finished, destroy all the nodes
            editContext.destroyAll();

            // Then add them back from the serialized form
            editContext.addPaletteNodesFromXML(paletteNodes);

            // And make us visible again
            editCanvas.setOpacity(100);
        }, 2000, "smoothEnd");
    }
});

// Create button bar
isc.HLayout.create({
    ID: "actionBar",
    membersMargin: 10,
    width: "100%",
    height: 30,
    members: [
        isc.LayoutSpacer.create({ width: "*" }),
        showComponentXMLButton,
        isc.LayoutSpacer.create({ width: 20 }),
        reloadSampleButton,
        clearButton,
        isc.LayoutSpacer.create({ width: 20 }),
        destroyAndRecreateButton
    ]
});

// This inserts the action buttons into the overall layout for the example.
vlayout.addMember(actionBar, 0);
