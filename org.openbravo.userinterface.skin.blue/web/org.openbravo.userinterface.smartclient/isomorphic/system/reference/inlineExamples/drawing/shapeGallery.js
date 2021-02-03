var mainPane = isc.DrawPane.create({
    autoDraw: false,
    showEdges: true,
    width: 720,
    height: 475
});
var labelProps = {
    fontFamily: "Arial",
    fontSize: 14    
};


isc.DrawTriangle.create({
    autoDraw: true,
    drawPane: mainPane,
    points: [[100,50],[150,150],[50,150]],
    canDrag:true
});

isc.DrawCurve.create({
    autoDraw: true,
    drawPane: mainPane,
    startPoint: [200, 50],
    endPoint: [300, 150],
    controlPoint1: [250, 0],
    controlPoint2: [250, 200],
    canDrag:true
});

isc.DrawLinePath.create({
    autoDraw: true,
    drawPane: mainPane,
    startPoint: [350, 50],
    endPoint: [450, 150],
    canDrag:true
});

isc.DrawPath.create({
    autoDraw: true,
    drawPane: mainPane,
    points: [[500, 50], [525,50], [550,75], [575,75], 
            [600,75], [600,125], [575,125], [550,125], 
            [525,150], [500,150]],
    canDrag:true
});

isc.DrawOval.create({
    autoDraw: true,
    drawPane: mainPane,
    left: 50,
    top: 300,
    width: 100,
    height: 100,
    canDrag:true
});

isc.DrawRect.create({
    autoDraw: true,
    drawPane: mainPane,
    left: 200,
    top: 300,
    width: 150,
    height: 100,
    canDrag:true
});

isc.DrawLine.create({
    autoDraw:true,
    drawPane: mainPane,        
    startPoint: [400, 300],
    endPoint: [500,400],
    canDrag:true
});


isc.DrawSector.create({
    autoDraw:true,
    drawPane:mainPane,
    centerPoint:[550, 300],
    startAngle: 0,
    endAngle: 90,
    radius:100,
    canDrag:true
});

// labels, first row
isc.DrawLabel.create({        
    autoDraw: true,
    drawPane: mainPane,
    left: 50,
    top: 175,
    contents: "Triangle"
    
}, labelProps);

isc.DrawLabel.create({        
    autoDraw: true,
    drawPane: mainPane,
    left: 200,
    top: 175,
    contents: "Curve"
    
}, labelProps);

isc.DrawLabel.create({        
    autoDraw: true,
    drawPane: mainPane,
    left: 350,
    top: 175,
    contents: "LinePath"
    
}, labelProps);

isc.DrawLabel.create({        
    autoDraw: true,
    drawPane: mainPane,
    left: 500,
    top: 175,
    contents: "Path"
    
}, labelProps);
// labels, second row
isc.DrawLabel.create({        
    autoDraw: true,
    drawPane: mainPane,
    left: 50,
    top: 415,
    contents: "Oval"
    
}, labelProps);

isc.DrawLabel.create({        
    autoDraw: true,
    drawPane: mainPane,
    left: 200,
    top: 415,
    contents: "Rectangle"
    
}, labelProps);

isc.DrawLabel.create({        
    autoDraw: true,
    drawPane: mainPane,
    left: 400,
    top: 415,
    contents: "Line"
    
}, labelProps);

isc.DrawLabel.create({        
    autoDraw: true,
    drawPane: mainPane,
    left: 550,
    top: 415,
    contents: "Sector"
    
}, labelProps);


isc.HStack.create({
    width: "100%",
    members: [mainPane]
});
