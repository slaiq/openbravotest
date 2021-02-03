isc.defineClass("DemoDrawPane", "DrawPane").addProperties({
    margin: 2,
    width: "100%",
    height: "*",
    border: "1px solid #f0f0f0",
    overflow: "hidden"
});

var drawLinePane = isc.DemoDrawPane.create();
var drawLine = isc.DrawLine.create({
    drawPane: drawLinePane,
    startPoint: [200, 20],
    endPoint: [400, 70],
    keepInParentRect: true
});
var drawLineKnobsForm = isc.DynamicForm.create({
    numCols: 4,
    items: [{
        editorType: "CheckboxItem",
        title: "startPoint",
        changed : function (form, item, value) {
            drawLine[value ? "showKnobs" : "hideKnobs"].call(drawLine, "startPoint");
        }
    }, {
        editorType: "CheckboxItem",
        title: "endPoint",
        changed : function (form, item, value) {
            drawLine[value ? "showKnobs" : "hideKnobs"].call(drawLine, "endPoint");
        }
    }]
});

var drawRectPane = isc.DemoDrawPane.create();
var drawRect = isc.DrawRect.create({
    drawPane: drawRectPane,
    left: 160,
    top: 30,
    width: 50,
    height: 120,
    keepInParentRect: true
});
var drawRectKnobsForm = isc.DynamicForm.create({
    numCols: 4,
    items: [{
        editorType: "CheckboxItem",
        title: "resize",
        changed : function (form, item, value) {
            drawRect[value ? "showKnobs" : "hideKnobs"].call(drawRect, "resize");
        }
    }, {
        editorType: "CheckboxItem",
        title: "move",
        changed : function (form, item, value) {
            drawRect[value ? "showKnobs" : "hideKnobs"].call(drawRect, "move");
        }
    }]
});

var drawOvalPane = isc.DemoDrawPane.create();
var drawOval = isc.DrawOval.create({
    drawPane: drawOvalPane,
    left: 450,
    top: 10,
    width: 70,
    height: 140,
    keepInParentRect: true
});
var drawOvalKnobsForm = isc.DynamicForm.create({
    numCols: 4,
    items: [{
        editorType: "CheckboxItem",
        title: "resize",
        changed : function (form, item, value) {
            drawOval[value ? "showKnobs" : "hideKnobs"].call(drawOval, "resize");
        }
    }, {
        editorType: "CheckboxItem",
        title: "move",
        changed : function (form, item, value) {
            drawOval[value ? "showKnobs" : "hideKnobs"].call(drawOval, "move");
        }
    }]
});

var drawImagePane = isc.DemoDrawPane.create();
var drawImage = isc.DrawImage.create({
    drawPane: drawImagePane,
    left: 250,
    top: 30,
    width: 48,
    height: 48,
    src: "/isomorphic/system/reference/exampleImages/pieces/48/piece_red.png",
    keepInParentRect: true
});
var drawImageKnobsForm = isc.DynamicForm.create({
    numCols: 4,
    items: [{
        editorType: "CheckboxItem",
        title: "resize",
        changed : function (form, item, value) {
            drawImage[value ? "showKnobs" : "hideKnobs"].call(drawImage, "resize");
        }
    }, {
        editorType: "CheckboxItem",
        title: "move",
        changed : function (form, item, value) {
            drawImage[value ? "showKnobs" : "hideKnobs"].call(drawImage, "move");
        }
    }]
});

var drawCurvePane = isc.DemoDrawPane.create();
var drawCurve = isc.DrawCurve.create({
    drawPane: drawCurvePane,
    startPoint: [60, 140],
    endPoint: [200, 10],
    controlPoint1: [20, 20],
    controlPoint2: [300, 120],
    keepInParentRect: true
});
var drawCurveKnobsForm = isc.DynamicForm.create({
    numCols: 8,
    items: [{
        editorType: "CheckboxItem",
        title: "startPoint",
        changed : function (form, item, value) {
            drawCurve[value ? "showKnobs" : "hideKnobs"].call(drawCurve, "startPoint");
        }
    }, {
        editorType: "CheckboxItem",
        title: "endPoint",
        changed : function (form, item, value) {
            drawCurve[value ? "showKnobs" : "hideKnobs"].call(drawCurve, "endPoint");
        }
    }, {
        editorType: "CheckboxItem",
        title: "controlPoint1",
        changed : function (form, item, value) {
            drawCurve[value ? "showKnobs" : "hideKnobs"].call(drawCurve, "controlPoint1");
        }
    }, {
        editorType: "CheckboxItem",
        title: "controlPoint2",
        changed : function (form, item, value) {
            drawCurve[value ? "showKnobs" : "hideKnobs"].call(drawCurve, "controlPoint2");
        }
    }]
});

var drawLabelPane = isc.DemoDrawPane.create();
var drawLabel = isc.DrawLabel.create({
    drawPane: drawLabelPane,
    left: 160,
    top: 30,
    contents: "This is a DrawLabel.",
    fontSize: 30,
    fontWeight: "normal",
    fontStyle: "italic",
    fontFamily: "Times New Roman, serif",
    keepInParentRect: true
});
var drawLabelKnobsForm = isc.DynamicForm.create({
    numCols: 4,
    items: [{
        editorType: "CheckboxItem",
        title: "move",
        changed : function (form, item, value) {
            drawLabel[value ? "showKnobs" : "hideKnobs"].call(drawLabel, "move");
        }
    }]
});


isc.SectionStack.create({
    width: "100%",
    overflow: "visible",
    visibilityMode: "multiple",
    sections: [{
        title: "DrawLine Knobs",
        expanded: true,
        items: [
            isc.VLayout.create({
                width: "100%",
                height: 200,
                members: [drawLineKnobsForm, drawLinePane]
            })
        ]
    }, {
        title: "DrawRect Knobs",
        expanded: true,
        items: [
            isc.VLayout.create({
                width: "100%",
                height: 200,
                members: [drawRectKnobsForm, drawRectPane]
            })
        ]
    }, {
        title: "DrawOval Knobs",
        expanded: true,
        items: [
            isc.VLayout.create({
                width: "100%",
                height: 200,
                members: [drawOvalKnobsForm, drawOvalPane]
            })
        ]
    }, {
        title: "DrawImage Knobs",
        expanded: true,
        items: [
            isc.VLayout.create({
                width: "100%",
                height: 200,
                members: [drawImageKnobsForm, drawImagePane]
            })
        ]
    }, {
        title: "DrawCurve Knobs",
        expanded: true,
        items: [
            isc.VLayout.create({
                width: "100%",
                height: 200,
                members: [drawCurveKnobsForm, drawCurvePane]
            })
        ]
    }, {
        title: "DrawLabel Knobs",
        expanded: true,
        items: [
            isc.VLayout.create({
                width: "100%",
                height: 200,
                members: [drawLabelKnobsForm, drawLabelPane]
            })
        ]
    }]
});
