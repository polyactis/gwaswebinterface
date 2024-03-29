/*
 * 2011-9
 * 	a multi-zoom-tier genome browser.
 * 
 */
function segmentKeyForStartSearch (position){
	/* 2010-9-17 generates a function. In case the position is in between of the segment, return position as key to be best matched.
	
	 */
	return function(segment) {
		if (typeof(segment.stop) ==='undefined' || segment.stop!==null){
			return (segment.start<position && position<=segment.stop)? position: segment.start;
			}
		else{
			return segment.start;
		}
		};
	}

var no_of_widgets = 0;
var defaultTopOffset = 20;

function createNewDiv(){
	/*
	 * 2010-9-26 sequentially create a new div id 
	 */
	var divID = "widget" + no_of_widgets;
	while(document.getElementById(divID)){
		no_of_widgets ++;
		divID = "widget" + no_of_widgets;
	}
	var div = document.createElement("div");
	div.setAttribute('id', divID);
	
	return div;
}

function zoomInPanel(){
	this.addYTicks = true;	//2010-10-17
	this.addXTicks = true;	//2010-10-17
	this.addZoomButton = false;
}

zoomInPanel.prototype.init = function (parentDivID, title, fetchURL, originalData, originalDataStart, originalDataStop, xStart, xStop,
		yStart, yStop, width, height, topOffset, imageURL, addZoomButton)
{
	/*
	 * 2010-9-27
	 * 	this init() can't be a constructor because it requires arguments and "new zoomInPanel" won't be valid in prototype inheritance.
	 */
	this.parentDivID = parentDivID;	
	this.parentDiv = document.getElementById(this.parentDivID);
	this.div = createNewDiv();
	this.divID = this.div.id;
	this.parentDiv.appendChild(this.div);
	
	this.title = title;
	this.fetchURL = fetchURL;
	this.dataFetched = null;	//including originalData (=dataFetched.data) and others.
	this.originalData = originalData;
	if (originalDataStart===null && this.originalData!==null){
		this.originalDataStart = pv.min(this.originalData, function(d) {return d.xStart;}); 
	}
	else{
		this.originalDataStart = originalDataStart;
	}
	if (originalDataStop===null && this.originalData!==null){
		this.originalDataStop = pv.max(this.originalData, function(d) {return d.xStop;});
	}
	else{
		this.originalDataStop = originalDataStop;
	}
	this.xStart = xStart;
	this.xStop = xStop;
	if (yStart===null && this.originalData!==null){
		this.yStart = pv.min(this.originalData, function(d) {return d.yStart;});
	}
	else{
		this.yStart = yStart;
	}
	if (yStop===null && this.originalData!==null){
		this.yStop = pv.max(this.originalData, function(d) {return d.yStart;});	/* 09/26/10 d.yStop is not used now.*/
	}
	else{
		this.yStop = yStop;
	}
	this.width = width;
	this.height = height;
	if (topOffset===null){
		this.topOffset = defaultTopOffset;
	}
	else{
		this.topOffset = topOffset;
	}

	this.imageURL = imageURL;
	this.addZoomButton = addZoomButton;
	
	this.parentWidget = null;
	this.children = [];
	this.childID2index = {};
	this.autoUpdateYScaleDomain = 0;	// 2010-9-24 a variable determining whether to update the domain of y-scale in initData()
	
	this.basePanel = new pv.Panel()
		.canvas(this.divID)
		.width(this.width + this.topOffset*2)
		.height(this.height + this.topOffset*2)
		.left(0)
		.right(0)
		.top(0)
		.bottom(0);
	
	this.panel = this.basePanel.add(pv.Panel)
		.width(this.width)
		.height(this.height)
		.left(this.topOffset)
		.right(this.topOffset)
		.top(this.topOffset)
		.bottom(this.topOffset);
	/*
	this.events("all")
		.event("mousedown", pv.Behavior.pan())
		.event("mousewheel", pv.Behavior.zoom(1.5));
	*/
	
	/* focus range for children */
	this.focusRange = {x:200, dx:50};
	
	//alert(this.topOffset);
	this.xScale = pv.Scale.linear(this.xStart, this.xStop).range(0, this.width);
	this.yScale = pv.Scale.linear(this.yStart, this.yStop).range(0, this.height);
	
	
	/* X-axis ticks. */
	var xScale = this.xScale;
	if (this.addXTicks===true)
	{
		this.panel.add(pv.Rule)
			.data(function() {return xScale.ticks();})
			.left(xScale)
			.strokeStyle("#eee")
			.anchor("bottom").add(pv.Label)
			.text(xScale.tickFormat);
	}
	var yScale = this.yScale;
	/* Y-axis ticks. */
	if (this.addYTicks===true)
	{
		this.panel.add(pv.Rule)
			.data(function() {return yScale.ticks();})
			.bottom(yScale)
			.strokeStyle(function(d) {return d ? "#aaa" : "#000"})
			.anchor("left").add(pv.Label)
			.text(this.yScale.tickFormat);
	}
	/* filled by inherited classes.
	this.data = this.initData();
	this.plot = this.panel.add(pv.Panel)
		.overflow("hidden")	//"hidden" is to instruct this panel to hide overflow parts.
		.add(...)
		...
	this.panel.render();
	*/
	
	/* add a title to the panel */
	if (this.title){
		this.panel.add(pv.Label)
			.left(this.width/2)
			.bottom(this.height)
			.textAlign("center")
			.text(this.title);
	}
	
	
	/* report where the X position of the context */
	this.crossHairLocation = {x: -1, y: 0 };
	var crossHairLocation = this.crossHairLocation;
	var xScale = this.xScale;	// "this" points to a different thing in protovis embedded functions.
	this.panel.add(pv.Label)
		.data([crossHairLocation])
		.visible(function(d) {return d.x >= 0;})
		.left(10)
		.bottom(this.height)
		.text(function(d)  {return xScale.invert(d.x).toFixed(0) + ", " + yScale.invert(d.y).toFixed(1)});
	
	/* add a close button, width increase/decrease buttons for this widget */
	var widget = this;
	var closeButtonSize = 15;
	this.closeButtonPanel = this.basePanel.add(pv.Panel)
		.right(0)
		.height(closeButtonSize*3)
		.width(closeButtonSize)
		.top(0);
	
	this.closeButtonPanel.add(pv.Image)
		.url(this.imageURL + "/close_panel.png")
		.right(0)
		.top(0)
		.width(closeButtonSize)
		.height(closeButtonSize)
		.cursor("pointer")
		.title("Close")
		.event("click", function(){
			widget.removeItself();
			});
	
	/* width adjust button */
	this.closeButtonPanel.add(pv.Image)
		.url(this.imageURL + "/slide-right.png")
		.right(0)
		.top(closeButtonSize)
		.width(closeButtonSize)
		.height(closeButtonSize)
		.cursor("pointer")
		.title("Fit the width to the browser")
		.event("click", function(){
			var extraWidth = widget.parentDiv.clientWidth - widget.width - widget.topOffset*2;
			widget.changeWidthBy(extraWidth);
			});
	
	/* width decrease button */
	this.closeButtonPanel.add(pv.Image)
		.url(this.imageURL + "/zoom-out-1.png")
		.top(closeButtonSize*2)
		.right(0)
		.width(closeButtonSize)
		.height(closeButtonSize)
		.cursor("pointer")
		.title("Decrease the width of the plot")
		.event("click", function(){
			widget.changeWidthBy(-40);
			});
	
	var height = 20;
	this.zoomButtonPanel = this.basePanel.add(pv.Panel)
		.width(this.width)
		.height(height)
		.left(this.topOffset)
		.bottom(0);
	this.zoomTrapezoidData = this.generateZoomTrapezoidData(this.topOffset);
	if (this.addZoomButton)
	{
		
		var widget = this;	// this has a different meaning in protovis functions.
		
		this.zoomTrapezoid = this.zoomButtonPanel.add(pv.Area)
			.data(this.zoomTrapezoidData)
			.left(function(d) {return d.x})
			.height(function(d) {return d.y})
			.bottom(0)
			.fillStyle("rgba(255, 128, 128, .4)");
		
		this.zoomButtonPanel.add(pv.Image)
			.url(imageURL + "/zoom-out-1.png")
			.left(this.width/2)
			.width(height)
			.bottom(0)
			.cursor("pointer")
			.title("Zoom out")
			.event("mouseover", function() {this.status = "zoom out"})
			.event("mouseout", function() {this.status = ""})
			.event("click", function(){
				widget.focusRange.x = Math.max(0,widget.focusRange.x - widget.focusRange.dx/2);
				widget.focusRange.dx = widget.focusRange.dx*2;
				widget.updateChildrenToRefocus();
				});
		
		this.zoomButtonPanel.add(pv.Image)
			.url(widget.imageURL + "/zoom-in-1.png")
			.left(widget.width*4/6)
			.width(height)
			.bottom(0)
			.cursor("pointer")
			.title("Zoom in")
			.event("mouseover", function() {this.status = "zoom in"})
			.event("mouseout", function() {this.status = ""})
			.event("click", function(){
				widget.focusRange.x = Math.max(0, widget.focusRange.x + widget.focusRange.dx/4);
				widget.focusRange.dx = widget.focusRange.dx/2;
				widget.updateChildrenToRefocus();
				});
		
		this.zoomButtonPanel.add(pv.Image)
			.url(imageURL + "/slide-left.png")
			.left(this.width*2/6)
			.width(height)
			.bottom(0)
			.cursor("pointer")
			.title("Slide left")
			.event("click", function(){
				widget.focusRange.x = Math.max(0,widget.focusRange.x - widget.focusRange.dx/3);
				widget.focusRange.dx = widget.focusRange.dx;
				widget.updateChildrenToRefocus();
				});
		
		this.zoomButtonPanel.add(pv.Image)
			.url(imageURL + "/slide-right.png")
			.left(this.width*5/6)
			.width(height)
			.bottom(0)
			.cursor("pointer")
			.title("Slide right")
			.event("click", function(){
				widget.focusRange.x = Math.max(0, widget.focusRange.x + widget.focusRange.dx/3);
				widget.focusRange.dx = widget.focusRange.dx;
				widget.updateChildrenToRefocus();
				});
		
	}
	
}

zoomInPanel.prototype.generateZoomTrapezoidData = function(height){
	return [{x:0, y:0},
		{x:this.focusRange.x, y:height},
		{x:this.focusRange.x+ this.focusRange.dx, y:height},
		{x:this.width, y:0}];
}

zoomInPanel.prototype.initData = function()
{
	var startIndex = pv.search.index(this.originalData, this.xStart, segmentKeyForStartSearch(this.xStart)) - 1,
		stopIndex = pv.search.index(this.originalData, this.xStop, function(d) {return d.start}) +1;
	if (stopIndex>this.originalData.length-1)
	{
		stopIndex = this.originalData.length-1;
	}
	if (startIndex>this.originalData.length-1)
	{
		startIndex = 0;
	}
	else if (startIndex>stopIndex)
	{
		startIndex = stopIndex-1;
	}
	var dd = this.originalData.slice(Math.max(0, startIndex), stopIndex );
	this.xScale.domain(this.xStart, this.xStop);
	if (this.autoUpdateYScaleDomain){
		this.yScale.domain([pv.min(dd, function(d) {return d.yStart;}), pv.max(dd, function(d) {return d.yStart;})]);
	}
	this.zoomTrapezoidData = this.generateZoomTrapezoidData(this.topOffset);
	return dd;
}

zoomInPanel.prototype.addPlot = function()
{
}

zoomInPanel.prototype.render = function()
{
	this.data = this.initData();
	if (this.zoomTrapezoid){
		this.zoomTrapezoid.data(this.zoomTrapezoidData);
	}
	this.plot.data(this.data);	// update plot's data. make sure this.plot is initialized before this is called.
	this.basePanel.render();
}

zoomInPanel.prototype.updateChildrenToRefocus = function()
{
	if (this.zoomTrapezoid){	//update the trapezoid which denotes the zoom in region if it exists
		this.zoomTrapezoidData = this.generateZoomTrapezoidData(this.topOffset);
		this.zoomTrapezoid.data(this.zoomTrapezoidData);
		this.zoomTrapezoid.root.render();
	}
	if (this.children.length>0){
		for (var i=0; i<this.children.length; i++)
		{
			var childPanel = this.children[i];
			if (childPanel!==null){	// it could be null if it has been removed.
				this.updateOnePanelToRefocus(childPanel);
				if (childPanel.children.length>0){	// if this child has children, remove them as well.
					childPanel.updateChildrenToRefocus();
				}
			}
		}
	}
	//updatePanelsWithCanvasStartStop(this.xScale, this.focusRange.x, this.focusRange.x + this.focusRange.dx, 
	//		this.children);
}

zoomInPanel.prototype.updateOnePanelToRefocus = function(panel)
{
	updatePanelsWithCanvasStartStop(this.xScale, this.focusRange.x, this.focusRange.x + this.focusRange.dx, 
			[panel]);
}

zoomInPanel.prototype.addOneFocusChild = function(panel)
{
	panel.id = 'child'+this.children.length;
	this.childID2index[panel.id] = this.children.length;
	this.children = this.children.concat(panel);
	panel.parentWidget = this;
	updatePanelsWithCanvasStartStop(this.xScale, this.focusRange.x, this.focusRange.x + this.focusRange.dx, 
			[panel]);
}

zoomInPanel.prototype.removeOneFocusChild = function(childPanel)
/*
 * 2010-9-24
 * this just dissociates the focus-zoom relationship between "this" and childPanel.
 * childPanel is still alive because its underlying dom element still exists.
 */
{
	var child_index = this.childID2index[childPanel.id];
	//this.childID2index[childPanel.id] = null;
	this.children[child_index] = null;
	//this.children.splice(child_index, 1);
	
	// reset this.childID2index because the index is changed due to splice()
	//this.childID2index = {};
	//for (var i=0; i<this.children.length; i++)
	//{
	//	var childID = this.children[i].id;
	//	this.childID2index[childID] = i;
	//}
}

zoomInPanel.prototype.removeItself = function()
/*
 * 2010-9-24
 * this removes the underlying dom element, which means everything.
 * removeChildren() will be called if this.children is not empty.
 */
{
	if (this.children.length>0){
		this.removeChildren();
	}
	if (this.parentWidget){
		this.parentWidget.removeOneFocusChild(this)
	}
	var el = document.getElementById(this.divID);
	el.parentNode.removeChild(el);
	delete this;	// avoid memory overflow
}

zoomInPanel.prototype.removeChildren = function()
/*
 * 2010-9-24
 * remove all children from this.children and their underlying dom element. 
 */
{
	if (this.children.length>0){
		for (var i=0; i<this.children.length; i++)
		{
			var childPanel = this.children[i];
			if (childPanel!==null){
				childPanel.removeItself();
				delete childPanel;	// avoid memory overflow
			}
			/*
			this.removeOneFocusChild(childPanel);
			var childDivID = childPanel.divID;
			var el = document.getElementById(childDivID);
			if (el && el.parentNode){
				el.parentNode.removeChild(el);
			}
			*/
			
		}
	}
}

zoomInPanel.prototype.changeWidthBy = function(extraWidth)
/*
 * 2010-11-4
 * 	given extraWidth (positive for expansion, negative for shrinking), scale the whole panel.
 * 	200 in pixels is minimum width allowed.
 */
{
	var newWidth = this.width + extraWidth;
	if (newWidth>200){
		this.width += extraWidth;
		this.xScale.range(0, this.width);
		this.basePanel.width(this.width + 2*this.topOffset);
		this.panel.width(this.width);
		this.zoomButtonPanel.width(this.width);
		this.closeButtonPanel.width(this.width);
		this.render();
		if (this.children.length>0){
			for (var i=0; i<this.children.length; i++)
			{
				var child = this.children[i];
				if (child!==null){
					child.changeWidthBy(extraWidth);
				}
			}
		}
	}
}

genePanel.prototype = new zoomInPanel;
genePanel.prototype.constructor = genePanel;
function genePanel(parentDivID, fetchURL, originalData, originalDataStart, originalDataStop, width, height, noOfGeneLanes, imageURL)
{
	
	this.noOfGeneLanes = noOfGeneLanes;
	var xStart = 0;
	var xStop = 1000;
	var yStart = -1;	// extra space below 0 for gene label
	var singleGeneHeight = height/(2*this.noOfGeneLanes);
	var yStop =  yStart + this.noOfGeneLanes + 0.5;
	var topOffset = defaultTopOffset;
	this.addYTicks = false;
	//this.inheritFrom = zoomInPanel;
	//this.prototype = new zoomInPanel(parentDivID, "Gene Model", fetchURL, originalData, originalDataStart, originalDataStop, xStart, 
	//		xStop, yStart, yStop, width, height, topOffset, imageURL);
	
	this.init(parentDivID, "Gene Model", fetchURL, originalData, originalDataStart, originalDataStop, xStart, 
			xStop, yStart, yStop, width, height, topOffset, imageURL);
	
	this.data = this.initData();
	this.plot = this.panel.add(pv.Panel)
		.overflow("hidden").add(pv.Panel); //"hidden" is to instruct this panel to hide overflow parts of the plot.
	var xScale = this.xScale;
	var yScale = this.yScale;
	var noOfGeneLanes = this.noOfGeneLanes;
	
	// 2010-10-18 each gene is a panel
	var geneBlocks = this.plot.data(this.data)
		.left(function(d) {return xScale(d.start)})
		.bottom(function() {return yScale(this.index%noOfGeneLanes)})
		.height(singleGeneHeight)
		.width(function(d) {return xScale(d.stop)- xScale(d.start)});
			/* 2010-9-17 above is different from "middleZoomInXScale(d.stop-d.star)" */
	
	/*
	geneBlocks.add(pv.Bar)
		.left(0)
		.bottom(0)
		.width(function(d) {return xScale(d.stop)- xScale(d.start)})
		.height(singleGeneHeight)
		.fillStyle("lightblue");
	*/
	// 2010-10-18 add a line from gene start to stop
	geneBlocks.add(pv.Rule)
		.left(0)
		.bottom(singleGeneHeight/2)
		.width(function(d) {return xScale(d.stop)- xScale(d.start)})
		.strokeStyle("red");
	
	// 2010-10-18 add an orientation line based on d.strand
	geneBlocks.add(pv.Line)
		.data(function(d) {
			var yMin = singleGeneHeight/2;
			var yMax = singleGeneHeight;
			var width = xScale(d.stop)- xScale(d.start);
			return d.strand==="+1"?  [[width, yMin], [width*4/5, yMax]]: [[0, yMin], [width/5, yMax]];
			//return [[0, singleGeneHeight/2], [width/5, singleGeneHeight]];
			})
		.left(function(d) {return d[0]})
		.bottom(function(d) {return d[1]})
		.strokeStyle("red")
		.lineWidth(1);
	
	// add a label and title
	geneBlocks.anchor("bottom").add(pv.Label)
		.visible(function() {
			var genomeSpan = xScale.domain()[1]-xScale.domain()[0];
			return genomeSpan<90000?1 :0;
			})
		.textBaseline("top")
		.text(function(d) {return d.name})
		.textStyle("black");
	
	// 2010-10-18 add a bar for each block (UTR/exon/intron) in a gene. transparent or not depends on is_translated.
	
	var geneBlockColor = pv.Scale.ordinal(0, 1).range("lightblue", "#1f77b4"); // color depends on is_translated (0 or 1) "#f33", "#33f".
	var geneBlockAlpha = pv.Scale.ordinal(0, 1).range(0, 0.7);	// if is_translated===0, total transparent; otherwise 0.7.
	geneBlocks.add(pv.Bar)
		.visible(function() {
			var genomeSpan = xScale.domain()[1]-xScale.domain()[0];
			return genomeSpan<200000?1 :0;
			})
		.def("xStartPosInFigCoord", function(d) {return xScale(d.start)})
		.data(function(d) {return d.box_ls})
		.left(function(d) {return xScale(d[0])-this.xStartPosInFigCoord()})
		.bottom(0)
		.height(singleGeneHeight)
		.width(function(d) {return xScale(d[1])- xScale(d[0])})
		.fillStyle(function(d) {return geneBlockColor(d[3]).alpha(geneBlockAlpha(d[3]))})
		.strokeStyle(function(d) {
			if (d[2]==='exon' && d[3]===0){
				return pv.colors("red")(0).alpha(0.7);	//UTR has a frame around it.
			}
			else{
				return this.fillStyle().alpha(0);
			}
			});
	
	geneBlocks.add(pv.Bar)
		.def("popupTitle", function(d) {return d.locustag + " " + d.start + "-" + d.stop + ". " + d.type_of_gene + ". " + d.description})
		.fillStyle(function(d) {return geneBlockColor(0).alpha(0.05)})
		.title(function() {return this.popupTitle()});
	
	this.basePanel.render();
}

haplotypePanel.prototype = new zoomInPanel;
haplotypePanel.prototype.constructor = haplotypePanel;
function haplotypePanel(parentDivID, title, fetchURL, rowInfoData, originalData, originalDataStart, originalDataStop, 
		xStart, xStop, width, height, imageURL)
{
	this.rowInfoData = rowInfoData;
	var yStart = 0;
	if (typeof(rowInfoData)==='undefined' || rowInfoData===null){
		var yStop = 10;
		this.heightPerUnitInYScale = 0.8;
	}
	else{
		var yStop = this.rowInfoData.rowInfoLs.length;
		this.heightPerUnitInYScale = (yStop-yStart)*0.8/this.rowInfoData.rowInfoLs.length;
	}
	/*
	this.inheritFrom = zoomInPanel;
	this.inheritFrom(parentDivID, title, fetchURL, originalData, originalDataStart, originalDataStop, 
			xStart, xStop, yStart, yStop, width, height, null, imageURL);
	*/
	// this.rowInfoData.row_id2yValue;
	this.init(parentDivID, title, fetchURL, originalData, originalDataStart, originalDataStop, 
			xStart, xStop, yStart, yStop, width, height, null, imageURL);
	
	this.data = this.initData();
	this.plot = this.panel.add(pv.Panel)
		.overflow("hidden")
		.add(pv.Bar);
	
	var widget = this;
	this.plot.data(widget.data)
		.left(function(d) {return widget.xScale(d.start)})
		.top(function(d) {var yValue = widget.rowInfoData.row_id2yValue[d.row_id]; return widget.yScale(yValue)})
		.height(function() {return widget.yScale(widget.heightPerUnitInYScale)})
		.fillStyle("red")
		.width(function(d) {return widget.xScale(d.stop)- widget.xScale(d.start)})
		.title(function(d) {
			var row_index = widget.rowInfoData.row_id2yValue[d.row_id]; 
			var rowInfo = widget.rowInfoData.rowInfoLs[row_index];
			return d.description + " " + rowInfo.description;
			}
		);
	this.panel.events("all")
		.event("mousedown", pv.Behavior.pan())
		.event("mousewheel", pv.Behavior.zoom(1.5));
	
	this.basePanel.render();
}

haplotypePanel.prototype.initData = function()
/*
 * 2010-9-26
 * extra handling due to the rowInfoData.
 * 	update yStop based on that and yScale as well.
 * 	reset the yScale
 */
{
	var dd = zoomInPanel.prototype.initData.call(this);
	if (this.dataFetched!==null && typeof(this.dataFetched.rowInfoData)!=='undefined' && this.dataFetched.rowInfoData!==null){
		this.rowInfoData = this.dataFetched.rowInfoData;
		this.yStop = this.rowInfoData.rowInfoLs.length;
		this.heightPerUnitInYScale = (this.yStop-this.yStart)*0.8/this.rowInfoData.rowInfoLs.length;
		this.yScale.domain([0, this.yStop]);
	}
	return dd;
}

function updatePanelWithDataStartStop(widget, start, stop)
{
	if (widget.originalDataStart<=start && widget.originalDataStop>=stop)
	{
		widget.xStart = start;
		widget.xStop = stop;
		widget.render();
	}
	else{
		// do request to fetch data. if successful, render the widget again.
		var url = widget.fetchURL + "&start="+start + "&stop=" + stop;
		
		var callback = {
			success: function(o) {
					// YAHOO.util.Dom.get(replace).innerHTML = o.responseText;
					dataFetched = YAHOO.lang.JSON.parse(o.responseText);
					if (dataFetched && dataFetched.data && dataFetched.data.length>0){
						widget.dataFetched = dataFetched;
						widget.originalData = dataFetched.data;
						widget.originalDataStart = Math.min(widget.originalData[0].start, start);
						widget.originalDataStop = Math.max(widget.originalData[widget.originalData.length -1].stop, stop);
						widget.xStart = start;
						widget.xStop = stop;
						widget.render();
					}
			},
			failure: function(o) {
				alert("fetching from "+ url +" failed.");
			}
		}
		var transaction = YAHOO.util.Connect.asyncRequest('GET', url, callback, null);
	
	}

}

function updatePanelsWithCanvasStartStop(scale, canvasXStart, canvasXStop, panels)
{
	var start = scale.invert(canvasXStart);
	var stop = scale.invert(canvasXStop);
	for (var i =0; i< panels.length; i++){
		updatePanelWithDataStartStop(panels[i], start, stop);
	}
}

contextPanel.prototype = new zoomInPanel;
contextPanel.prototype.constructor = contextPanel;

function contextPanel(parentDivID, title, fetchURL, originalData, originalDataStart, originalDataStop, xStart, xStop, 
		yStart, yStop, width, height, topOffset, imageURL, addZoomButton)
{
	this.init(parentDivID, title, fetchURL, originalData, originalDataStart, originalDataStop, xStart, xStop,
			yStart, yStop, width, height, topOffset, imageURL, addZoomButton);
}

contextPanel.prototype.addPlot = function(dotSize, noTooltip)
{
	if (!dotSize){
		dotSize = 5
	}
	this.data = this.initData();
	var xScale = this.xScale;
	var yScale = this.yScale;
	this.plot = this.panel.add(pv.Panel)
		.overflow("hidden")
		.add(pv.Dot);
	this.plot.data(this.data)
		.left(function(d) {return xScale(d.start)})
		.bottom(function(d) {return yScale(d.yStart)})
		.size(dotSize);
	if (!noTooltip){
		this.plot.title(function(d) {return "chr=" + d.chromosome + " x="+d.start + " y=" + d.yStart.toFixed(2) + 
			" description=" + d.description;});
	}
	this.basePanel.render();
}



var reportBarSpan = function(scale, x, dx){
	var startX = scale.invert(x);
	var stopX = scale.invert(x+dx); 
	return startX.toFixed(0) + " - " + stopX.toFixed(0);
}

function addFocusBar(widget)
{

	/* The selectable, draggable context region in context. */
	var panel = widget.panel;
	widget.panel.add(pv.Panel)
		.data([widget.focusRange])
		.cursor("crosshair")
		.events("all")
		.event("mouseout", function(d) {
			widget.crossHairLocation.x = -1;
			return panel;
		})
		.event("mousemove", function(d) {
			widget.crossHairLocation.x = panel.mouse().x;
			widget.crossHairLocation.y = panel.mouse().y;
			return panel;
		})
		.event("mousedown", pv.Behavior.select())
		.event("mouseup", function(d) {
			widget.updateChildrenToRefocus();
		})
			// event could be 'select' but it causes instant update while the mouse is still moving. too much burden.
		.add(pv.Bar)
			.left(function(d) {return d.x})
			.width(function(d) {return d.dx})
			.fillStyle("rgba(255, 128, 128, .4)")
		.title(function(d) {return reportBarSpan(widget.xScale, d.x, d.dx);})
		.cursor("move")
		.event("mousedown", pv.Behavior.drag())
		.event("mouseup", function(d) {
			widget.updateChildrenToRefocus();
		});	// event could be 'drag' but it causes instant update while the mouse is still moving. too much burden.
	
	
}

function addCrossHair(widget){
	var panel = widget.panel;
	widget.panel.add(pv.Panel)
		.data([widget.crossHairLocation])
		.events("all")
		.event("mouseout", function(d) {
			widget.crossHairLocation.x = -1;
			return panel;
		})
		.event("mousemove", function(d) {
			widget.crossHairLocation.x = panel.mouse().x;
			widget.crossHairLocation.y = panel.mouse().y;
			return panel;
		});
}

var overviewPanels = [];

function addWidgetAsFocusChildToLastContext(widget)
{
	var index = overviewPanels.length-1;
	while (index>=0){
		var overview = overviewPanels[index];
		if (overview.contextF){
			var contextFDivElem = document.getElementById(overview.contextF.divID);
			//make sure the div element still exists (could be deleted by user)
			if (contextFDivElem!==null){
				overview.contextF.addOneFocusChild(widget);
				break;
			}
		}
		index--;
	}
	overviewPanels = overviewPanels.slice(0, index+1);	//toss the overviews whose contextF doesn't exist anymore
}

function initializeContextsByFetchingData(parentDivID, title, fetchURL, getGeneModelDataJsonURL, width, height, imageURL){
	var url = fetchURL;
	var callback = {
		success: function(o) {
				// YAHOO.util.Dom.get(replace).innerHTML = o.responseText;
				dataFetched = YAHOO.lang.JSON.parse(o.responseText);
				if (dataFetched){
					var overview = new initializeContexts(parentDivID, title, getGeneModelDataJsonURL, dataFetched.overviewData,
							dataFetched.data, width, height, imageURL);
					overviewPanels = overviewPanels.concat(overview);	//overviewPanels is a global variable.
				}
		},
		failure: function(o) {
			alert("fetching from "+ url +" failed.");
		}
	}
	var transaction = YAHOO.util.Connect.asyncRequest('GET', url, callback, null);
}

function initializeContexts(parentDivID, title, getGeneModelDataJsonURL, overviewData, fullData, width, height, imageURL){

	var start = overviewData[0].start;
	var end = overviewData[overviewData.length - 1].start;
	
	/* Scales and sizing. */
	this.width = width;
	var height = height,
		contextFHeight = height+10,
		contextFFHeight = height*2,
		//yStart = pv.min(overviewData, function(d) {return d.yStart}),
		//yStop = pv.max(overviewData, function(d) {return d.yStart}),
		verticalMarginHeight = defaultTopOffset;
	
	var addZoomButton = 1;
	var context = new contextPanel(parentDivID, title, "", overviewData, start, end, start, end, null, null, 
		this.width, height, verticalMarginHeight, imageURL, addZoomButton);
	addFocusBar(context);
	context.addPlot(1, 1);	// First 1 is dot size, 2nd 1 is noTooltip.
	this.context = context;
	
	var contextF = new contextPanel(parentDivID, title, "", fullData, start, end, 0, 1, null, null, 
			this.width, contextFHeight, verticalMarginHeight, imageURL, addZoomButton);
	addFocusBar(contextF);
	contextF.addPlot();
	this.contextF = contextF;
	this.context.addOneFocusChild(contextF);
	
	//this.contextF.addPlot();
	
	var contextFF = new contextPanel(parentDivID, title, "", fullData, start, end, 0, 1, null, null, 
			this.width, contextFFHeight, verticalMarginHeight, imageURL);
	addCrossHair(contextFF);
	contextFF.addPlot();
	this.contextFF = contextFF;
	this.contextF.addOneFocusChild(contextFF);
	//this.vis.add(contextFF);
	
	var noOfGeneLanes = 6;
	var geneModelWidget = new genePanel(parentDivID, getGeneModelDataJsonURL,
		[], 0, 0, width, height*1.5, noOfGeneLanes, imageURL);
	this.geneModelWidget = geneModelWidget;
	this.contextF.addOneFocusChild(geneModelWidget);

}

tickPanel.prototype = new zoomInPanel;
tickPanel.prototype.constructor = tickPanel;
function tickPanel(parentDivID, title, fetchURL, originalData, originalDataStart, originalDataStop, width, height, imageURL)
{
	/*
	 * 2010-10-17 panel which plots data, such as probe positions, as ticks. 
	 */
	var xStart = 0;
	var xStop = 1000;
	var yStart = 0;
	var yStop = yStart + 2;
	var topOffset = defaultTopOffset;
	//this.addXTicks = false;
	this.addYTicks = false;
	this.init(parentDivID, title, fetchURL, originalData, originalDataStart, originalDataStop, xStart, 
			xStop, yStart, yStop, width, height, topOffset, imageURL);
	
	this.data = this.initData();
	this.plot = this.panel.add(pv.Panel)
		.overflow("hidden").add(pv.Rule)
			.bottom(0)
			.add(pv.Rule); //"hidden" is to instruct this panel to hide overflow parts of the plot.
	var xScale = this.xScale;
	var yScale = this.yScale;
	
	this.plot
		.data(this.data)
		.left(function(d) {return xScale(d.start)})
		.bottom(0)
		.height(function() {return yScale(yStop/2)});
	this.basePanel.render();
}