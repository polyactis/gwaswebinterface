/*
 * 2010-09 js testing protovis scatterplot
 */
var data = pv.range(1000).map(function(x) {
    return {x: x, y: Math.random(), z: Math.pow(10, 3 * Math.random())};
  });
  

    /* Sizing and scales. */
var w = 400,
    h = 400,
    x = pv.Scale.linear(0, 999).range(0, w),
    y = pv.Scale.linear(0, 1).range(0, h),
    c = pv.Scale.log(1, 100).range("orange", "brown");

/* The root panel. */
var vis = new pv.Panel()
    .canvas("fig")
    .width(w)
    .height(h)
    .bottom(20)
    .left(20)
    .right(10)
    .top(5);

/* Y-axis and ticks. */
vis.add(pv.Rule)
    .data(y.ticks())
    .bottom(y)
    .strokeStyle(function(d) {return d ? "#eee" : "#0a0"})
  .anchor("left").add(pv.Label)
    .visible(function(d) {return d > 0 && d < 1})
    .text(y.tickFormat);

/* X-axis and ticks. */
vis.add(pv.Rule)
    .data(x.ticks())
    .left(x)
    .strokeStyle(function(d) {return d ? "#eee" : "#ae0"})
  .anchor("bottom").add(pv.Label)
    .visible(function(d) {return d > 0 && d < 1000})
    .text(x.tickFormat);

var c = pv.Scale.linear(1, 1000).range("#1f77b4", "#ff7f0e");


/* The dot plot! */
var dotPlot = vis.add(pv.Panel)
    .data(data)
  .add(pv.Dot)
    .def("i", -1)
    .left(function(d) {return x(d.x)})
    .bottom(function(d) {return y(d.y)})
    .strokeStyle(function(d) {return c(d.z)})
    .fillStyle(function(d) {return this.strokeStyle().alpha(1.)})
    .fillStyle(function() {return this.i() == this.index ? "red" : "blue"})
    .size(function(d) {return Math.log(d.z)})
    .event("mouseover", function() {return this.i(this.index)})
    .event("mouseout", function() {return this.i(-1)})
    .title(function(d) {return "d.z: " + d.z.toFixed(2)});

var i = -1;

var dot = dotPlot.add(pv.Dot)
    .visible(function() {return i > 0 && i<999})
    .data(function() {return [data[i]]})
    .fillStyle(function() {return dotPlot.strokeStyle()})
    .strokeStyle("#000")
    .size(20)
    .lineWidth(1);

dotPlot.add(pv.Label)
    .visible(function() {return i > 0 && i<999})
    .left(80)
    .bottom(40)
    .anchor("right").add(pv.Label)
    .text(function(d) {return "x= " + data[i].x.toFixed(2)})
    .strokeStyle("#000")
    .size(20)
    .fillStyle(function() {return dotPlot.strokeStyle()});


dotPlot.event("mouseout", function() {
        i = -3;
        return vis;
      })
    .event("mousemove", function() {
        var mx = x.invert(vis.mouse().x);
        i = pv.search(data.map(function(d) {return d.x}), mx);
        i = i < 0 ? (- 2) : i;
        return vis;
      });
      
vis.render();