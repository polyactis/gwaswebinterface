/*
 * 2011-5-13 a whole-map canvas, used as a basis to draw individual markerList (another overlay).
 * The markers (MapMarkerOverlayByCanvas) are in the coordinate system of the map pane but are
 * drawn on this shared canvas, which covers the whole visible part of the pane.
 */
package edu.nordborglab.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.canvas.dom.client.FillStrokeStyle;
import com.google.gwt.maps.client.MapPane;
import com.google.gwt.maps.client.MapPaneType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.overlay.Overlay;

public class MapCanvasOverlay  extends Overlay {
	private final LatLng latLng;
	public double minValueForSize;
	public double maxValueForSize;
	public double minValueForColor;
	public double maxValueForColor;
	public int binOption;
	public int plotOption;
	public Double[] valueList;
	private boolean value2SizeLogScale;
	private boolean value2ColorLogScale;
	
	public Canvas canvas;
	public Context2d context;
	
	private MapWidget parentMap;

	private MapPane pane;
	
	public int width;
	public int height;
	
	//public MapMarkerOverlayByCanvas[] markerList;
	public ArrayList<MapMarkerOverlayByCanvas> markerList;
	
	public HashMap<Double, String> value2Label = new HashMap<Double, String>();	// for catergorical values, need the map for their labels
	
	public Value2Size value2Size;
	public Value2Color value2Color;
	
	public int minSize = 5;	// in pixels
	public int maxSize = 15;	// in pixels
	private int numberOfBins = 5;	//if binning is used instead, 
	
	public static class Value2Size{
		public Double minValue;
		public Double maxValue;
		public int minSize;
		public int maxSize;
		private int binOption;
		private boolean logScale;
		private int numberOfBins;	//if binning is used instead, this controls how many bins to be generated.
		
		private double deltaToAvoidLogNegative = 0.0;	//a delta to shift the values so that Math.log10() won't be undefined.
		private double binUnitSize;	// how large each bin is.
		private double deltaTolerated = 0.0001;
		// a map for binOption=1 (individual value represents one bin, one category)
		private HashMap<Double, Integer> singleValue2Size = new HashMap<Double, Integer>();
		
		public Value2Size(double minValue, double maxValue, int minSize, int maxSize, int binOption,
				boolean logScale){
			this.minValue = minValue;
			this.maxValue = maxValue;
			
			this.minSize = minSize;
			this.maxSize = maxSize;
			
			this.binOption = binOption;
			this.logScale = logScale;
			
			if(this.logScale){
				if (this.minValue<1){
					this.deltaToAvoidLogNegative = 2-this.minValue;	// using 2 instead of 1 is to avoid the log(minValue)=0,
								// which might cause further problem if maxValue = minValue
				}
				else{
					this.deltaToAvoidLogNegative = 0.0;
				}
				this.minValue = Math.log10(this.deltaToAvoidLogNegative + this.minValue);
				this.maxValue = Math.log10(this.deltaToAvoidLogNegative + this.maxValue);
			}
			
			this.deltaTolerated = (this.maxValue-this.minValue)/100.0;
			
			if (binOption==1){	//numerical value range
				initSingleValue2SizeMap();
			}
			else{
				initBinValue2SizeMap();
			}
			
		}
		
		/*
		 * 2011-5-14
		 * 	sort the value list
		 * 	then from small to large , put them into singleValue2Size()
		 */
		public void initSingleValue2SizeMap(){
			
		}
		
		public void initBinValue2SizeMap(){
			
			//singleValue2Size;
		}
		
		public int value2Size(Double value){
			/*
			 * if (this.binOption==1){
			 
				return this.singleValue2Size.get(value);
			}
			else{
			*/
			if (this.logScale){
				value = this.deltaToAvoidLogNegative + value;
				value = Math.log10(value);
			}
			
			if (Math.abs(value-this.maxValue)<=this.deltaTolerated){
				return this.maxSize;
			}
			else if (Math.abs(value-this.minValue)<=this.deltaTolerated){
				return this.minSize;
			}
			
			double denominator;
			if (this.maxValue>this.minValue){
				denominator = this.maxValue - this.minValue;
			}
			else{
				denominator = Math.max(1, value-this.minValue);
			}
			double size = ((value-this.minValue)/denominator)*(this.maxSize-this.minSize) + this.minSize;
			/*
			if (size<minSize){
				size = minSize;
			}
			else if (size>maxSize){
				size = maxSize;
			}
			*/
			return (int)size;
			//}
		}
		
		/*
		 * 2011-5-22
		 * 	because sometimes logScale is true, sometimes not.
		 * This function returns value if no logScale,  
		 */
		public Double getOriginalValue(Double value){
			if (logScale){
				return Math.pow(10, value)-deltaToAvoidLogNegative;
			}
			else{
				return value;
			}
		}
		
		/*
		 * 2011-5-22
		 * 	Given a size of integer type, get its corresponding value (transformed if logScale=true).
		 * 	call getOriginalValue() to get the un-transformed value.
		 */
		public Double size2Value(int size){
			double denominator;
			if (maxValue>minValue){
				denominator = maxValue - minValue;
			}
			else{
				denominator = 1;
			}
			Double value = (((double)size-minSize)/(double)(maxSize-minSize))*denominator + minValue;
			
			
			return value;
		}
		
		/*
		 * 2011-5-16
		 * 	take average of all non-NA (<minValue is NA) values and get size
		 * The input valueList is before transformation.
		 */
		public int getRadiusFromSizeValueList(ArrayList<Double> valueList){
			Double avgValue = Common.getAvgOfArrayListWithinMinMax(valueList, 
					getOriginalValue(this.minValue)-this.deltaTolerated, 
					getOriginalValue(this.maxValue)+this.deltaTolerated);
			return value2Size(avgValue);
		}
	}
	
	public static class Value2Color extends Value2Size{
		
		
		public static int max_hue_value = 225;	//hue near 255 is very similar to hue=1.
		private static int min_hue_value = 1;
		public Value2Color(double minValue, double maxValue, int binOption,
				boolean logScale){
			super(minValue, maxValue, min_hue_value, max_hue_value, binOption, logScale);
			
		}
		
		
		/*
		 * 2011-5-14
		 * 	call some javascript in the pub folder to do the conversion 
		 */
		public static native String hslValue2RGBColor(int hslValue) /*-{
			rgb_array = $wnd.hslToRgb(hslValue/255.0, 1.0, 0.5);
			return "rgba("+ Math.round(rgb_array[0]) +", " + rgb_array[1] + "," +rgb_array[2] + ", 0.7)";
		}-*/;
		
		public String value2RGBcolor(Double value){
			
			if (value<this.minValue || value>this.maxValue){
				 
				return "rgba(255,255,255,0)";
			}
			else{
				int hslValue = this.value2Size(value);
				//if (hslValue<this.minSize-0.001 || hslValue>this.maxSize+0.001){	//2011-5-22 missing data
				//}
				//else{
				return hslValue2RGBColor(hslValue);
			}
			//in (R,G,B) mode, the bigger R/G/B is, the darker the color is
			//R_value = int(Y/math.pow(2,8))
			//_value = int(Y- R_value*math.pow(2,8))
		}
		
		
	}
	
	
	/*
	 * 2011-5-13
	 * an important variable recording the coordinates of (0,0) of this canvas in the coordinate system of the Map pane.
	 * most of the time, it should still be (0,0). However, when user starts to move the map by dragging the mouse, the two differ.
	 * The (0,0) of the map pane is stuck with its original GPS coordinate,
	 * 	while the canvas has to be moved to cover the visible part of the whole pane and thus the difference.
	 */
	public Point zeroPointRelativeToPane;
	
	public MapCanvasOverlay(LatLng latLng, double minValueForSize, double maxValueForSize, double minValueForColor,
			double maxValueForColor, int binOption, int plotOption, 
			boolean value2SizeLogScale, boolean value2ColorLogScale) {
		/* Save our inputs to the object */
		this.latLng = latLng;
		this.minValueForSize = minValueForSize;
		this.maxValueForSize = maxValueForSize;
		this.minValueForColor = minValueForColor;
		this.maxValueForColor = maxValueForColor;
		this.binOption = binOption;
		this.plotOption = plotOption;
		this.value2SizeLogScale = value2SizeLogScale;
		this.value2ColorLogScale = value2ColorLogScale;
		
		this.value2Size = new Value2Size(minValueForSize, maxValueForSize, minSize, maxSize, binOption,
				value2SizeLogScale);
		this.value2Color = new Value2Color(minValueForColor, maxValueForColor, binOption, value2ColorLogScale); 
		
		canvas = Canvas.createIfSupported();
		context = canvas.getContext2d();
		markerList = new ArrayList<MapMarkerOverlayByCanvas>(0);
		
	}
	
	/*
	 * 2011-5-14
	 * 	function to reset a couple of values
	 */
	public void resetOptions(int binOption, int plotOption, 
			boolean value2SizeLogScale, boolean value2ColorLogScale){
		this.binOption = binOption;
		this.plotOption = plotOption;
		this.value2SizeLogScale = value2SizeLogScale;
		this.value2ColorLogScale = value2ColorLogScale;
		
		this.value2Size = new Value2Size(minValueForSize, maxValueForSize, minSize, maxSize, binOption, value2SizeLogScale);
		this.value2Color = new Value2Color(minValueForColor, maxValueForColor, binOption, value2ColorLogScale); 
		
	}
	
	/*
	 * 2011-5-14
	 * 	function to reset a couple of values
	 */
	public void resetOptions(double minValueForSize, double maxValueForSize, double minValueForColor,
			double maxValueForColor, int binOption, int plotOption, 
			boolean value2SizeLogScale, boolean value2ColorLogScale){
		this.minValueForSize = minValueForSize;
		this.maxValueForSize = maxValueForSize;
		this.minValueForColor = minValueForColor;
		this.maxValueForColor = maxValueForColor;
		this.binOption = binOption;
		this.plotOption = plotOption;
		this.value2SizeLogScale = value2SizeLogScale;
		this.value2ColorLogScale = value2ColorLogScale;
		
		this.value2Size = new Value2Size(minValueForSize, maxValueForSize, minSize, maxSize, binOption, value2SizeLogScale);
		this.value2Color = new Value2Color(minValueForColor, maxValueForColor, binOption, value2ColorLogScale); 
		
	}
	@Override
	protected final void initialize(MapWidget map) {
		/* Save a handle to the parent map widget */
		parentMap = map; // If we need to do redraws we'll need this

		/* Add canvas to the main map pane */
		pane = parentMap.getPane(MapPaneType.MARKER_PANE);
		pane.add(canvas);	// 2011-5-12 maybe i should add this to the parent of pane.
		reSizeWidget();
		resetWidgetPosition();
		/*
		pane = parentMap.getPane(MapPaneType.MARKER_PANE);
		Point locationPoint = parentMap.convertLatLngToDivPixel(parentMap.getCenter());
		zeroPointRelativeToPane = Point.newInstance(locationPoint.getX()-width/2, locationPoint.getY()-height/2);
		pane.setWidgetPosition(canvas, zeroPointRelativeToPane.getX(), zeroPointRelativeToPane.getY());
		*/
	}

	@Override
	protected final Overlay copy() {
		return new MapCanvasOverlay(latLng, minValueForSize, maxValueForSize, minValueForColor, maxValueForColor,
				binOption, plotOption,
				value2SizeLogScale, value2ColorLogScale);
	}

	protected final void resetWidgetPosition()
	{
		pane = parentMap.getPane(MapPaneType.MARKER_PANE);
		Point locationPoint = parentMap.convertLatLngToDivPixel(parentMap.getCenter());
		zeroPointRelativeToPane = Point.newInstance(locationPoint.getX()-width/2, locationPoint.getY()-height/2);
		pane.setWidgetPosition(canvas, zeroPointRelativeToPane.getX(), zeroPointRelativeToPane.getY());
		/*
		if (locationPoint!=oldCenterInPixel){
			pane.setWidgetPosition(canvas, locationPoint.getX()-width/2, locationPoint.getY()-height/2);
			Point offsetPoint = Point.newInstance(locationPoint.getX() - oldCenterInPixel.getX(),
					locationPoint.getY() - oldCenterInPixel.getY());
			drawMarkerListWithOffsetPoint(offsetPoint);
		}
		*/
		//pane.setWidgetPosition(canvas, 0, 0);
	}
	
	/*
	 * 2011-5-13
	 * a special function same as resetWidgetPosition() except redrawing all markers with an offset in the end.
	 * used in the MapMoveHandler. although calling redraw(true) would achieve the same outcome.
	 */
	protected final void resetWidgetPositionAfterMapMove()
	{
		pane = parentMap.getPane(MapPaneType.MARKER_PANE);
		Point locationPoint = parentMap.convertLatLngToDivPixel(parentMap.getCenter());
		zeroPointRelativeToPane = Point.newInstance(locationPoint.getX()-width/2, locationPoint.getY()-height/2);
		pane.setWidgetPosition(canvas, zeroPointRelativeToPane.getX(), zeroPointRelativeToPane.getY());
		//2011-5-13 here is the key. redraw all the markers on the canvas with an offset between canvas and pane coordinates.
		// the marker's coordinates are always relative to the pane, but they are drawn on the canvas.
		drawMarkerListWithOffsetPoint(zeroPointRelativeToPane);
	}
	
	protected final void reSizeWidget()
	{
		width = parentMap.getSize().getWidth();
		height = parentMap.getSize().getHeight();
		reSizeCanvas(width, height);
		//pane = parentMap.getPane(MapPaneType.MARKER_PANE);
		//pane.setWidgetPosition(canvas, 0, 0);
		/* Place the canvas on the pane in the correct spot */
		//Point locationPoint = parentMap.convertLatLngToDivPixel(latLng);
		//pane.setWidgetPosition(canvas, locationPoint.getX(), locationPoint.getY());
	}
	/*
	 * 2011-5-13 resize the canvas given width and height
	 */
	public void reSizeCanvas(int width, int height){
		canvas.setWidth(width+"px");
		canvas.setHeight(height + "px");
		canvas.setCoordinateSpaceWidth(width);
		canvas.setCoordinateSpaceHeight(height);
	}
	@Override
	protected final void redraw(boolean force) {
		if(!force)
		{
			return;
		}
		resetWidgetPosition();
		reSizeWidget();
		drawMarkerList();
	}
	
	public void  drawMarkerList() {
		/*
		 * 2011-5-13 zeroPointRelativeToPane is an important offset.
		 * usually it's zero, which means the canvas's coordinate is same as map pane's.
		 * However, the two starts to differ when the user is moving the map.
		 * without it, when moving the map around, the pane's zero point is no longer the left right.
		 */
		drawMarkerListWithOffsetPoint(zeroPointRelativeToPane);
	}
	
	public void  drawMarkerListWithOffsetPoint(Point offsetPoint) {
		// clear the canvas first.
		CssColor transparentColor = CssColor.make("rgba(255,0,0,0.2)");
		//context.setFillStyle(transparentColor);
		context.clearRect(0, 0, width, height);
		//context.
		// then redraw every marker
		for (int i = markerList.size() - 1; i >= 0; i--) {
			MapMarkerOverlayByCanvas marker = markerList.get(i);
			marker.drawSelfWithOffset(offsetPoint);
		}
		drawLegend();
	}

	@Override
	protected final void remove() {
		canvas.removeFromParent();
	}

	public LatLng getLatLng() {
		return latLng;
	}
	
	public void addOneMarker(MapMarkerOverlayByCanvas marker){
		this.markerList.add(marker);
	}
	
	public void clearMarkerList(){
		context.clearRect(0, 0, width, height);
		this.markerList.clear();
	}
	
	
	/*
	 * 2011-5-23
	 * 	draw a half circle and a text, part of the legend for the circle size
	 */
	public void drawHalfCircleLegend(FillStrokeStyle strokeStyle, int sizeLegendStartX, int sizeLegendStartY, 
			int textWidthInPixel, Double valueForTheRadius, Value2Size value2Size){
		context.setStrokeStyle(strokeStyle);
		context.beginPath();
		int radius = value2Size.value2Size(valueForTheRadius);
		context.arc(sizeLegendStartX+radius, sizeLegendStartY, radius, 0, Math.PI,  false);
		int arcLeftPointX = sizeLegendStartX;
		int arcRightPointX  = sizeLegendStartX + 2*radius;
		context.moveTo(arcLeftPointX, sizeLegendStartY);
		//context.lineTo(arcRightPointX, sizeLegendStartY);
		// the last boolean argument (anticlockwise) would change the painting direction.
		// the angle direction is always clockwise.
		//X-axis positive side is always angle 0.
		context.closePath();
		context.stroke();
		
		context.setFillStyle(strokeStyle);
		Integer valueForTheRadiusInt = valueForTheRadius.intValue();
		String valueForTheRadiusStr = valueForTheRadiusInt.toString();
		context.fillText(valueForTheRadiusStr, arcRightPointX, sizeLegendStartY, textWidthInPixel);
	}
	
	/*
	 * 2011-5-14 draw the legend for the color and size
	 */
	public void drawLegend(){
		int colorLegendWidth = 100;	//in pixels
		int colorLegendHeight = 40;
		int no_of_bins = 100;
		double deltaInValue = (value2Color.maxValue-value2Color.minValue)/no_of_bins;
		int deltaInPixel = (int)(colorLegendWidth/(double)no_of_bins);
		int textWidthInPixel = (int)(colorLegendWidth/10.0);
		
		final FillStrokeStyle strokeStyle = CssColor.make("rgba(0,0,0,0.7)");
		
		int sizeLegendStartX = 20;
		int sizeLegendStartY = height - colorLegendHeight -10;
		
		drawHalfCircleLegend(strokeStyle, sizeLegendStartX, sizeLegendStartY, 
				textWidthInPixel, value2Size.getOriginalValue(value2Size.minValue), value2Size);
		drawHalfCircleLegend(strokeStyle, sizeLegendStartX, sizeLegendStartY, 
				textWidthInPixel, value2Size.getOriginalValue(value2Size.maxValue), value2Size);
		Double halfSize = (value2Size.minSize+value2Size.maxSize)/2.0;
		Double valueForHalfSize = value2Size.size2Value(halfSize.intValue());
		drawHalfCircleLegend(strokeStyle, sizeLegendStartX, sizeLegendStartY, 
				textWidthInPixel, value2Size.getOriginalValue(valueForHalfSize), value2Size);
		//draw the legend for the color
		
		int colorLegendStartX = sizeLegendStartX + value2Size.maxSize + 80;
		int colorLegendStartY = sizeLegendStartY;
		for (int i=0; i<no_of_bins; i++){
			Double value = value2Color.minValue + i*deltaInValue;
			
			context.setFillStyle(CssColor.make(value2Color.value2RGBcolor(value)));
			context.fillRect(colorLegendStartX, colorLegendStartY, deltaInPixel, colorLegendHeight);
			//context.fill();
			if (i==0 || i==no_of_bins-1 || i==no_of_bins/2){
				context.setFillStyle(strokeStyle);
				context.fillText(value.toString(), colorLegendStartX, colorLegendStartY, textWidthInPixel);
			}
			
			colorLegendStartX = colorLegendStartX + deltaInPixel;
		}
		
		
		
		
	}


}
