isc.DrawPane.create({
    showEdges: true,
    autoDraw:  true,
    ID:        "mainPane",
    width:     800,
    height:    340,
    top:       110,
    overflow:  "hidden"
});

isc.DynamicForm.create({
    ID:     "dataForm",
    width:  300,
    fields: [
        { 
            editorType:   "spinner", 
            name:         "lineWidth", 
            title:        "Line Width", 
            defaultValue: 5,
            min:          1, 
            max:          10, 
            step:         1, 
            width:        150
        }, 
        {
            type:         "select", 
            name:         "lineStyle", 
            title:        "Line Style", 
            width:        150,
            defaultValue: "solid",
            valueMap: {
                "solid":     "Solid",
                "dot":       "Dot",
                "dash":      "Dash",
                "shortdot":  "Short dot",
                "shortdash": "Short dash",
                "longdash":  "Long dash"
            }
        }, 
        {
            name:         "lineCapStyle", 
            title:        "Line Cap Style", 
            type:         "select", 
            width:        150,
            defaultValue: "round",
            valueMap: {
                "round":  "Round",
                "square": "Square",
                "butt":   "Butt"
            }
        },
        {
            name:         "arrowHeadStyle", 
            title:        "Arrow Head Style", 
            type:         "select", 
            width:        150,
            defaultValue: "open",
            valueMap: {
                "open":  "Open",
                "block": "Block"
            }
        }
    ],
    itemChanged : drawLines
});

function drawLines(){
    mainPane.destroyItems();

    var lineWidth = dataForm.values["lineWidth"];
    var lineStyle = dataForm.values["lineStyle"];
    var lineCapStyle  = dataForm.values["lineCapStyle"];
    var arrowHeadStyle = dataForm.values["arrowHeadStyle"];

    isc.DrawLine.create({
        autoDraw:    true,
        drawPane:    mainPane,
        lineWidth:   lineWidth,
        linePattern: lineStyle, 
        lineCap:     lineCapStyle,
        startArrow: arrowHeadStyle,
        endArrow: arrowHeadStyle,
        startPoint:  [20, 30],
        endPoint:    [180, 190]
    });

    isc.DrawLinePath.create({
        autoDraw:    true,
        drawPane:    mainPane,
        lineWidth:   lineWidth,
        linePattern: lineStyle,
        lineCap:     lineCapStyle,
        startArrow: arrowHeadStyle,
        endArrow: arrowHeadStyle,
        startPoint:  [190, 40],
        endPoint:    [340, 190]
    });

    isc.DrawPath.create({
        autoDraw: true,
        drawPane: mainPane,
            lineWidth:   lineWidth,
            linePattern: lineStyle,
            lineCap:     lineCapStyle,
            startArrow: arrowHeadStyle,
            endArrow: arrowHeadStyle,
        points: [[400, 25], [425,50], [450,75], [475,75], 
                [500,75], [500,125], [475,125], [450,125], 
                [425,150], [500,175]]
    });

    isc.DrawCurve.create({
        autoDraw: true,
        drawPane: mainPane,
        lineWidth:   lineWidth,
        linePattern: lineStyle,
        startArrow: arrowHeadStyle,
        endArrow: arrowHeadStyle,
        startPoint: [600, 50],
        endPoint: [700, 200],
        controlPoint1: [680, -10],
        controlPoint2: [620, 260]
    });
}

drawLines();
