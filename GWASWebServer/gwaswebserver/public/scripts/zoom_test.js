function segmentKeyForStartSearch (position){
	/* 2010-9-17 generates a function. In case the position is in between of the segment, return position as key to be best matched.
	
	 */
	return function(segment) {return (segment.start<position && position<=segment.stop)? position: segment.start;}
	}


function zoomTest(divID, fullData){

var deletionData = fullData.deletionData;

if (deletionData.length==0)
{
	/* no drawing if deletionData is empty.*/
	return;
}
var chromosome = deletionData[0].chromosome;
var geneModelData = chr2geneModelDataLsJson[chromosome];

//var year = 1000 * 60 * 60 * 24 * 365;
var data = fullData["overviewData"];
var start = data[0].x;
var end = data[data.length - 1].x;



/* Scales and sizing. */
var figWidth = 1000,
	topZoomInHeight = 700,
	middleZoomInHeight = 80,
	contextHeight = 40,
	contextFocusHeight = 50,
	contextFFHeight = 100,
	verticalMarginHeight = 20,
	contextXScale = pv.Scale.linear(start, end).range(0, figWidth),
	contextYScale = pv.Scale.linear(0, pv.max(data, function(d) {return d.y})).range(0, contextHeight);

var noOfGeneLanes = 6,
	noOfAccessions = 1000;

/* 1st Focus */
var contextFocusRange = {x:200, dx:50},
	contextFocusXScale = pv.Scale.linear().range(0, figWidth),
	contextFocusYScale = pv.Scale.linear(0, pv.max(data, function(d) {return d.y})).range(0, contextFocusHeight);


/* 2nd Focus scales will have domain set on-render. */
var contextFocusFocusRange = {x:200, dx:50},
	contextFFXScale = pv.Scale.linear().range(0, figWidth),
	contextFFYScale = pv.Scale.linear(0, pv.max(data, function(d) {return d.y})).range(0, contextFFHeight),
	topZoomInXScale = pv.Scale.linear().range(0, figWidth),
	topZoomInYScale = pv.Scale.linear().range(0, topZoomInHeight),
	middleZoomInXScale = pv.Scale.linear().range(0, figWidth),
	middleZoomInYScale = pv.Scale.linear().range(0, middleZoomInHeight);

/* Root panel. */
var vis = new pv.Panel()
	.canvas(divID)
    .width(figWidth)
    .height(topZoomInHeight + verticalMarginHeight*5 + middleZoomInHeight + contextFFHeight + contextFocusHeight + contextHeight)
    .bottom(0)
    .left(20)
    .right(20)
    .top(verticalMarginHeight);

this.vis = vis;

/* zoomInPanel consists of 3 panels, contextFF, middleZoomInPanel, topZoomInPanel,*/
var zoomInPanel = vis.add(pv.Panel)
	.top(contextHeight + verticalMarginHeight*2 + contextFocusHeight)
	.height(contextFFHeight + middleZoomInHeight + verticalMarginHeight*2 + topZoomInHeight);

zoomInPanel
	.events("all")
	.event("mousedown", pv.Behavior.pan())
	.event("mousewheel", pv.Behavior.zoom(1.5));

/* middle panel (zoomed in). */
var middleZoomInPanel = zoomInPanel.add(pv.Panel)
    .def("init", function() {
        var d1 = contextFocusXScale.invert(contextFocusFocusRange.x),
            d2 = contextFocusXScale.invert(contextFocusFocusRange.x + contextFocusFocusRange.dx),
            startIndex = pv.search.index(geneModelData, d1, segmentKeyForStartSearch(d1)) - 1,
            stopIndex = pv.search.index(geneModelData, d2, function(d) {return d.start}) +1,
            dd = geneModelData.slice( Math.max(0, startIndex), stopIndex );
        middleZoomInXScale.domain(d1, d2);
        middleZoomInYScale.domain(0, noOfGeneLanes);
        return dd;
      })
    .top(contextFFHeight+ verticalMarginHeight)
    .height(middleZoomInHeight);


/* X-axis ticks. */
middleZoomInPanel.add(pv.Rule)
    .data(function() {return middleZoomInXScale.ticks();})
    .left(middleZoomInXScale)
    .strokeStyle("#eee")
  .anchor("bottom").add(pv.Label)
    .text(middleZoomInXScale.tickFormat);

/* Y-axis ticks. */
middleZoomInPanel.add(pv.Rule)
    .data(function() {return middleZoomInYScale.ticks(7);})
    .bottom(middleZoomInYScale)
    .strokeStyle(function(d) {return d ? "#aaa" : "#000"})
  .anchor("left").add(pv.Label)
    .text(middleZoomInYScale.tickFormat);

/* middleZoomIn area chart. */
middleZoomInPanel.add(pv.Panel)
    .overflow("hidden")
  .add(pv.Bar)
    .data(function() {return middleZoomInPanel.init()})
    .left(function(d) {return middleZoomInXScale(d.start)})
    .bottom(function() {return middleZoomInYScale(this.index%noOfGeneLanes)})
    .height(function() {return middleZoomInYScale(0.8)})
    .fillStyle("lightblue")
    .width(function(d) {return middleZoomInXScale(d.stop)-middleZoomInXScale(d.start)})
    	/* 2010-9-17 above is different from "middleZoomInXScale(d.stop-d.star)" */
    .title(function(d) {return d.locustag + " " + d.start + "-" + d.stop + ". " + d.type_of_gene + ". " + d.description})
  .anchor("left").add(pv.Label)
    .text(function(d) {return d.name})
    .textStyle("black");


/* topZoomIn panel. */
var topZoomInPanel = zoomInPanel.add(pv.Panel)
    .def("init", function() {
        var d1 = contextFocusXScale.invert(contextFocusFocusRange.x),
            d2 = contextFocusXScale.invert(contextFocusFocusRange.x + contextFocusFocusRange.dx),
            startIndex = pv.search.index(deletionData, d1, segmentKeyForStartSearch(d1)) - 1,
            stopIndex = pv.search.index(deletionData, d2, function(d) {return d.start}) +1,
            dd = deletionData.slice(Math.max(0, startIndex), stopIndex);
        topZoomInXScale.domain(d1, d2);
        topZoomInYScale.domain(0, noOfAccessions);
        return dd;
      })
    .top(contextFFHeight + verticalMarginHeight*2 + middleZoomInHeight)
    .height(topZoomInHeight);

/* X-axis ticks. */
topZoomInPanel.add(pv.Rule)
    .data(function() {return topZoomInXScale.ticks()})
    .left(topZoomInXScale)
    .strokeStyle("#eee")
  .anchor("bottom").add(pv.Label)
    .text(topZoomInXScale.tickFormat);
    
/* topZoomIn area chart. */
topZoomInPanel.add(pv.Panel)
    .overflow("hidden")
    .data(function() {return topZoomInPanel.init()})
  .add(pv.Bar)
    .left(function(d) {return topZoomInXScale(d.start)})
    .top(function(d) {return topZoomInYScale(d.row_index)})
    .height(function() {return topZoomInYScale(1)})
    .fillStyle("red")
    .width(function(d) {return topZoomInXScale(d.stop)-topZoomInXScale(d.start)})
    .title(function(d) {return "probability=" + d.probability.toFixed(2)});

/* contextFF panel (zoomed in of contextFocus). */
var contextFF = zoomInPanel.add(pv.Panel)
    .def("init", function() {
        var d1 = contextFocusXScale.invert(contextFocusFocusRange.x),
            d2 = contextFocusXScale.invert(contextFocusFocusRange.x + contextFocusFocusRange.dx),
            originalData = data,
            startIndex = pv.search.index(originalData, d1, function(d) {return d.x}) - 1,
            stopIndex = pv.search.index(originalData, d2, function(d) {return d.x}) +1,
            dd = originalData.slice(Math.max(0, startIndex), stopIndex);
        contextFFXScale.domain(d1, d2);
        return dd;
      })
    .top(0)
    .height(contextFFHeight);


/* X-axis ticks. */
contextFF.add(pv.Rule)
    .data(function() {return contextFFXScale.ticks();})
    .left(contextFFXScale)
    .strokeStyle("#eee")
  .anchor("bottom").add(pv.Label)
    .text(contextFFXScale.tickFormat);

/* Y-axis ticks. */
contextFF.add(pv.Rule)
    .data(function() {return contextFFYScale.ticks();})
    .bottom(contextFFYScale)
    .strokeStyle(function(d) {return d ? "#aaa" : "#000"})
  .anchor("left").add(pv.Label)
    .text(contextFFYScale.tickFormat);

/* ContextFocus dot plot. */
var contextFFDot = contextFF.add(pv.Panel)
	.overflow("hidden")
	.add(pv.Dot)
	.data(function() {return contextFF.init()})
	.left(function(d) {return contextFFXScale(d.x)})
	.bottom(function(d) {return contextFFYScale(d.y)})
	.size(6)
	.title(function(d) {return "x="+d.x + " y=" + d.y.toFixed(2) + " description=" + d.description;});

/* contextFocus panel */
var contextFocus = vis.add(pv.Panel)
    .def("init", function() {
        var d1 = contextXScale.invert(contextFocusRange.x),
            d2 = contextXScale.invert(contextFocusRange.x) + contextXScale.invert(contextFocusRange.dx),
	    originalData = data,
            startIndex = pv.search.index(originalData, d1, function(d) {return d.x}) - 1,
            stopIndex = pv.search.index(originalData, d2, function(d) {return d.x}) +1,
            dd = originalData.slice(Math.max(0, startIndex), stopIndex);
	//	window.alert(startIndex);
	//	window.alert(stopIndex);
        contextFocusXScale.domain(d1, d2);
        return dd;
      })
	.top(contextHeight + verticalMarginHeight)
	.height(contextFocusHeight);

/* X-axis ticks. */
contextFocus.add(pv.Rule)
	.data(function() {return contextFocusXScale.ticks();})
	.left(contextFocusXScale)
	.strokeStyle("#eee")
	.anchor("bottom").add(pv.Label)
	.text(contextFocusXScale.tickFormat);

/* Y-axis ticks. */
contextFocus.add(pv.Rule)
	.data(contextFocusYScale.ticks(4))
	.strokeStyle(function(d) {return d ? "#aaa" : "#000"})
	.bottom(contextFocusYScale)
	.anchor("left").add(pv.Label)
	.text(contextFocusYScale.tickFormat);

/* ContextFocus dot plot. */
var contextFocusDot = contextFocus.add(pv.Panel)
	.overflow("hidden")
	.add(pv.Dot)
	.data(function() {return contextFocus.init()})
	.left(function(d) {return contextFocusXScale(d.x)})
	.bottom(function(d) {return contextFocusYScale(d.y)})
	.size(5);


/* report where the X position of the context */
var contextFocusCrossHairLocation = {x: -1, y: 0 };
contextFocus.add(pv.Label)
    .data([contextFocusCrossHairLocation])
    .visible(function(d) {return d.x >= 0;})
    .left(10)
    .bottom(contextFocusHeight/2)
    .text(function(d)  {return contextFocusXScale.invert(d.x).toFixed(0)});

/* The selectable, draggable contextFocus region for zoomInPanel. */
contextFocus.add(pv.Panel)
    .data([contextFocusFocusRange])
    .cursor("crosshair")
    .events("all")
    .event("mouseout", function() {
        contextFocusCrossHairLocation.x = -1;
        return contextFocus;
      })
     .event("mousemove", function() {
        contextFocusCrossHairLocation.x = vis.mouse().x;
        contextFocusCrossHairLocation.y = vis.mouse().y;
        return contextFocus;
      })
    .event("mousedown", pv.Behavior.select())
    .event("mouseup", function(){contextFF.render(); topZoomInPanel.render(); middleZoomInPanel.render();})	// event could be 'select' but it causes instant update while the mouse is still moving. too much burden.
  .add(pv.Bar)
    .left(function(d) {return d.x})
    .width(function(d) {return d.dx})
    .fillStyle("rgba(255, 128, 128, .4)")
    .title(function(d) {startX = contextFocusXScale.invert(d.x); stopX = contextFocusXScale.invert(d.x+d.dx); 
    		return startX.toFixed(0) + " - " + stopX.toFixed(0)})
    .cursor("move")
    .event("mousedown", pv.Behavior.drag())
    .event("mouseup", function(){contextFF.render(); topZoomInPanel.render(); middleZoomInPanel.render();});	// event could be 'drag' but it causes instant update while the mouse is still moving. too much burden.

/* Context panel (zoomed out). */
var context = vis.add(pv.Panel)
	.top(0)
	.height(contextHeight);
	
/* X-axis ticks. */
context.add(pv.Rule)
    .data(contextXScale.ticks())
    .left(contextXScale)
    .strokeStyle("#eee")
  .anchor("bottom").add(pv.Label)
    .text(contextXScale.tickFormat);

/* Y-axis ticks. */
context.add(pv.Rule)
    .bottom(0);

/* Context area chart. */
var contextArea = context.add(pv.Dot)
    .data(data)
    .left(function(d) {return contextXScale(d.x)})
    .bottom(function(d) {return contextYScale(d.y)})
    .size(1);

/* report where the X position of the context */
var crossHairLocation = {x: -1, y: 0 };
contextArea.add(pv.Label)
    .data([crossHairLocation])
    .visible(function(d) {return d.x >= 0;})
    .left(10)
    .bottom(contextHeight/2)
    .text(function(d)  {return contextXScale.invert(d.x).toFixed(0)});

/* The selectable, draggable context region. */
context.add(pv.Panel)
    .data([contextFocusRange])
    .cursor("crosshair")
    .events("all")
    .event("mouseout", function() {
        crossHairLocation.x = -1;
        return context;
      })
     .event("mousemove", function() {
        crossHairLocation.x = vis.mouse().x;
        crossHairLocation.y = vis.mouse().y;
        return context;
      })
    .event("mousedown", pv.Behavior.select())
    .event("mouseup", contextFocus)	// event could be 'select' but it causes instant update while the mouse is still moving. too much burden.
  .add(pv.Bar)
    .left(function(d) {return d.x})
    .width(function(d) {return d.dx})
    .fillStyle("rgba(255, 128, 128, .4)")
    .title(function(d) {startX = contextXScale.invert(d.x); stopX = contextXScale.invert(d.x+d.dx); 
    		return startX.toFixed(0) + " - " + stopX.toFixed(0)})
    .cursor("move")
    .event("mousedown", pv.Behavior.drag())
    .event("mouseup", contextFocus);// event could be 'drag' but it causes instant update while the mouse is still moving. too much burden.


vis.render();
return vis;
}
