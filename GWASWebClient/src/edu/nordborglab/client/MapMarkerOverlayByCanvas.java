/*
 * 2011-5-13
 * 	a map overlay powered by the canvas widget in GWT. It will be drawn on a MapCanvasOverlay canvas,
 * 	which is shared by all markers like this one. MapCanvasOverlay is added to the map before all these markers
 * 	are added.
 */
package edu.nordborglab.client;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;

import com.google.gwt.maps.client.MapPane;
import com.google.gwt.maps.client.MapPaneType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.overlay.Marker;
//import com.google.gwt.maps.client.event.MarkerClickHandler;
//import com.google.gwt.maps.client.event.MarkerMouseOverHandler;
//import com.google.gwt.maps.client.event.MarkerClickHandler.MarkerClickEvent;
//import com.google.gwt.maps.client.event.MarkerMouseOverHandler.MarkerMouseOverEvent;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.impl.HandlerCollection;
import com.google.gwt.maps.client.impl.MapEvent;
import com.google.gwt.maps.client.impl.EventImpl.VoidCallback;
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.canvas.dom.client.FillStrokeStyle;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOverHandler;

import edu.nordborglab.client.event.MapCanvasMarkerClickHandler;
import edu.nordborglab.client.event.MapCanvasMarkerClickHandler.MarkerClickEvent;
import edu.nordborglab.client.event.MapCanvasMarkerMouseOverHandler;
import edu.nordborglab.client.event.MapCanvasMarkerMouseOverHandler.MarkerMouseOverEvent;


public class MapMarkerOverlayByCanvas extends Overlay {

	private final LatLng latLng;
	//private final SimplePanel textPanel;
	private MapCanvasOverlay canvasOverlay;
	private Context2d context;
	
	private MapWidget parentMap;

	private MapPane pane;

	private Point offset;
	private String title;
	private ArrayList<Integer> accession_id_list;
	private ArrayList<Double> sizeValueList;
	private ArrayList<Double> colorValueList;
	private int objIndex;
	
	// Keep track of JSO's registered for each instance of addXXXListener()
	private HandlerCollection<MapCanvasMarkerClickHandler> markerClickHandlers;
	private HandlerCollection<MapCanvasMarkerMouseOverHandler> markerMouseOverHandlers;
	
	
	final FillStrokeStyle strokeStyle = CssColor.make("#333300");
	final CssColor bgColor = CssColor.make("rgba(0, 0, 0, 0)");
	public CssColor randomColor= bgColor;
	
	public Point offsetPoint;
	
	/**
	 * Main constructor
	 * 
	 * @param latLng
	 */
	public MapMarkerOverlayByCanvas(MapCanvasOverlay canvasOverlay, LatLng latLng, String title, 
			ArrayList<Integer> accession_id_list,
			ArrayList<Double> sizeValueList, ArrayList<Double> colorValueList, int objIndex) {
		/* Save our inputs to the object */

		this.canvasOverlay = canvasOverlay;
		this.context = canvasOverlay.context;
		
		this.latLng = latLng;
		this.offsetPoint = Point.newInstance(0, 0);
		
		//this.value = value;
		this.title = title;
		this.accession_id_list = accession_id_list;
		this.sizeValueList = sizeValueList;
		this.colorValueList = colorValueList;
		
		this.objIndex = objIndex;
		
		//this.randomColor = CssColor.make(color);
		/*
		 * // init the canvases
		 
		canvasOverlay.setWidth(width + "px");
		canvasOverlay.setHeight(height + "px");
		canvasOverlay.setCoordinateSpaceWidth(width);
		canvasOverlay.setCoordinateSpaceHeight(height);
		backBuffer.setCoordinateSpaceWidth(width);
		backBuffer.setCoordinateSpaceHeight(height);
		RootPanel.get(holderId).add(canvasOverlay);
		*/
		/*
		textPanel = new SimplePanel();
		textPanel.setStyleName("textOverlayPanel");
		textPanel.add(canvasOverlay);
		*/
		if (canvasOverlay.binOption==1){	//for color only
		
			
		}
		else{
			
		}
	}

	/* context gets added to the map and placed in the initialize method */
	@Override
	protected final void initialize(MapWidget map) {
		/* Save a handle to the parent map widget */
		parentMap = map; // If we need to do redraws we'll need this

		/* Add canvasOverlay to the main map pane */
		//pane = map.getPane(MapPaneType.MARKER_PANE);
		
		//pane.add(canvasOverlay);	// 2011-5-12 maybe i should add this to the parent of pane.
		
		
		drawSelfWithOffset(canvasOverlay.zeroPointRelativeToPane);
		//drawSelf();
		
		
	}

	@Override
	protected final Overlay copy() {
		return new MapMarkerOverlayByCanvas(canvasOverlay, latLng, title, accession_id_list,
				sizeValueList, colorValueList, objIndex);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.google.gwt.maps.client.overlay.Overlay#redraw(boolean)
	 * 2011-5-16 do nothing because mapCanvas is charge of drawing.
	 */
	@Override
	protected final void redraw(boolean force) {
		if(!force)
		{
			return;
		}
		//drawSelf();
	
	}
	
	public void drawSelf(){
		drawSelfWithOffset(this.offsetPoint);
	}

	/*
	 * 2011-5-13
	 * 	offsetPoint is necessary because MapMarkerOverlayByCanvas is in the coordinate system (parentMap.convertLatLngToDivPixel)
	 * 	of the pane of the map. but it's drawn on the context of MapCanvasOverlay. Most of the time the two are same.
	 * 	But when the map is in a "moving"/"mouse-dragging" motion, (0,0) of MapCanvasOverlay becomes
	 * 	fixed to the map according to GPS of the original anchor and MapCanvasOverlay needs to be moved to the top left corner
	 *  of map pane. The two start to differ as the top left corner of the map is no longer (0,0) in the map pane.
	 * During moving, all markers of MapCanvasOverlay will move along with the canvas, which causes a major dislocation.
	 * 	This offsetPoint is used to set them back to where they should belong on the map.
	 * 
	 */
	public void drawSelfWithOffset(Point offsetPoint){
		Point locationPoint = parentMap.convertLatLngToDivPixel(this.latLng);
		int radius = canvasOverlay.value2Size.getRadiusFromSizeValueList(sizeValueList);
		if (radius<canvasOverlay.minSize){
			radius = canvasOverlay.minSize;
		}
		int centerX = locationPoint.getX()-offsetPoint.getX();
		int centerY = locationPoint.getY()-offsetPoint.getY();
		int circleCenterY = centerY - radius - 2*canvasOverlay.minSize;
		if (canvasOverlay.plotOption==1){	//one pie for each individual.
			double startAngle = 0;
			double deltaAngle = Math.PI*2/colorValueList.size();
			for (int i=0; i<colorValueList.size(); i++){
				Double value = colorValueList.get(i);
				String color = canvasOverlay.value2Color.value2RGBcolor(value);
				drawOnePie(context, centerX, circleCenterY, startAngle, startAngle+deltaAngle, radius, 
						CssColor.make(color));
				startAngle = startAngle + deltaAngle;
			}
		}
		else{	//one 360 degree pie with color corresponding to the median value of all non-NA. 
			//get mean or median
			Double value = Common.getAvgOfArrayListWithinMinMax(colorValueList, canvasOverlay.minValueForColor, 
					canvasOverlay.maxValueForColor);
			String color = canvasOverlay.value2Color.value2RGBcolor(value);
			drawOnePie(context, centerX, circleCenterY, 0, Math.PI*2, radius, CssColor.make(color));
			
		}
		
		//draw the outline of the whole pie
		final FillStrokeStyle strokeStyle = CssColor.make("#333300");
		context.setStrokeStyle(strokeStyle);
		context.beginPath();
		context.arc(centerX, circleCenterY, radius, 0, Math.PI*2,  false);
		// the last boolean argument (anticlockwise) would change the painting direction.
		// the angle direction is always clockwise.
		//X-axis positive side is always angle 0.
		context.closePath();
		context.stroke();
		
		
		drawPedestal(context,  centerX, centerY, canvasOverlay.minSize*2, randomColor);
		//drawMarkerOnContext(context, centerX, centerY, radius, randomColor);
	}
	@Override
	protected final void remove() {
		//canvas.removeFromParent();
	}

	public LatLng getLatLng() {
		return latLng;
	}

	public String getText() {
		return title;
	}

	public void setText(String title) {
		this.title = title;
	}

	public Point getOffset() {
		return offset;
	}

	public void setOffset(Point offset) {
		this.offset = offset;
	}
	

	public static void drawOnePie(Context2d context, int centerX, int centerY, double startAngle, double endAngle,
			double headRadius, CssColor color) {
		int circleCenterX = centerX;
		int circleCenterY = centerY;
		
		context.setFillStyle(color);
		context.beginPath();
		context.arc(circleCenterX, circleCenterY, headRadius, startAngle, endAngle,  false);
		context.lineTo(circleCenterX, circleCenterY);
		// the last boolean argument (anticlockwise) would change the painting direction.
		// the angle direction is always clockwise.
		//X-axis positive side is always angle 0.
		context.closePath();
		context.fill();
		
		
	}
	
	
	public static void drawPedestal(Context2d context, int centerX, int centerY, int height, CssColor color) {
		int topLeftX = centerX - (int)(height/8.0);
		int topLeftY = centerY - height;
		int topRightX = centerX + (int)(height/8.0);
		int topRightY = topLeftY;
		
		// 2011-5-13 draw a google-map-marker-like shape
		final FillStrokeStyle strokeStyle = CssColor.make("#333300");
		context.setStrokeStyle(strokeStyle);
		context.setFillStyle(color);
		context.beginPath();
		context.moveTo(topRightX, topRightY);
		context.lineTo(topLeftX, topRightY);
		context.bezierCurveTo(centerX, topLeftY, centerX, centerY- (int)(height/2.0), centerX, centerY);
		context.bezierCurveTo(centerX, centerY-(int)(height/2.0), centerX, topLeftY, topRightX, topRightY);
		context.closePath();
		context.stroke();
		context.fill();
	}
	
	
	/*
	 * 2011-5-13
	 * 	draw a marker similar to google's marker with a big head and a needle connecting the head to the GPS location.
	 */
	public static void drawMarkerOnContext(Context2d context, int centerX, int centerY, double headRadius, CssColor color) {
		/*
		// Get random coordinates and sizing
		int canvasWidth = map.getSize().getWidth();
		int canvasHeight = map.getSize().getHeight();
		int rndX = Random.nextInt(canvasWidth);
		int rndY = Random.nextInt(canvasHeight);
		int rndWidth = Random.nextInt((int)(canvasWidth/10.0));
		int rndHeight = Random.nextInt(canvasHeight);

		// Get a random color and alpha transparency
		int rndRedColor = Random.nextInt(255);
		int rndGreenColor = Random.nextInt(255);
		int rndBlueColor = Random.nextInt(255);
		double rndAlpha = Random.nextDouble();
		CssColor randomColor = CssColor.make("rgba(" + rndRedColor + ", "
				+ rndGreenColor + "," + rndBlueColor + ", " + rndAlpha + ")");

		//context.setFillStyle(randomColor);
		//context.fillRect(rndX, rndY, rndWidth, rndHeight);
		//context.fill();
		*/
		
		
		int circleCenterX = centerX;
		int circleCenterY = centerY- (int)(3*headRadius);
		int offset = (int)(headRadius*Math.sqrt(0.5));
		int circleLeftPointX = circleCenterX - offset;
		int circleLeftPointY = circleCenterY + offset;
		int circleRightPointX = circleCenterX + offset;
		int circleRightPointY = circleLeftPointY;
		
		// 2011-5-13 draw a google-map-marker-like shape
		final FillStrokeStyle strokeStyle = CssColor.make("#333300");
		context.setStrokeStyle(strokeStyle);
		context.setFillStyle(color);
		context.beginPath();
		context.arc(circleCenterX, circleCenterY, headRadius, Math.PI*0.25, Math.PI * 0.75,  true);
		// the last boolean argument (anticlockwise) would change the painting direction.
		// the angle direction is always clockwise.
		//X-axis positive side is always angle 0.
		context.bezierCurveTo(centerX, circleLeftPointY, centerX, centerY-headRadius, centerX, centerY);
		context.bezierCurveTo(centerX, centerY-headRadius, centerX, circleLeftPointY, circleRightPointX, circleRightPointY);
		context.closePath();
		context.stroke();
		context.fill();
		//context.restore();
		
	}
	
	/**
	 * Lazy init the HandlerCollection.
	 */
	private void maybeInitMarkerMouseOverEvent() {
		if (markerMouseOverHandlers == null) {
			markerMouseOverHandlers = new HandlerCollection<MapCanvasMarkerMouseOverHandler>(jsoPeer, MapEvent.MOUSEOVER);
		}
	}
	
	/**
	 * This event is fired when the mouse enters the area of the marker icon.
	 * 
	 * @param handler
	 *            the handler to call when this event fires.
	 */
	public void addMarkerMouseOverHandler(final MapCanvasMarkerMouseOverHandler handler) {
		maybeInitMarkerMouseOverEvent();
		markerMouseOverHandlers.addHandler(handler, new VoidCallback() {
			@Override
			public void callback() {
				MarkerMouseOverEvent e = new MarkerMouseOverEvent(MapMarkerOverlayByCanvas.this);
				handler.onMouseOver(e);
			}
		});
	}
	

	/**
	 * Lazy init the HandlerCollection.
	 */
	private void maybeInitMarkerClickHandlers() {
		if (markerClickHandlers == null) {
			markerClickHandlers = new HandlerCollection<MapCanvasMarkerClickHandler>(jsoPeer, MapEvent.CLICK);
		}
	}
	
	/**
	 * This event is fired when the marker icon was clicked. Notice that this
	 * event will also fire for the map, with the marker passed as an argument
	 * to the event handler.
	 * 
	 * @param handler
	 *            the handler to call when this event fires.
	 */
	public void addMarkerClickHandler(final MapCanvasMarkerClickHandler handler) {
		maybeInitMarkerClickHandlers();

		markerClickHandlers.addHandler(handler, new VoidCallback() {
			@Override
			public void callback() {
				MarkerClickEvent e = new MarkerClickEvent(MapMarkerOverlayByCanvas.this);
				handler.onClick(e);
			}
		});
	}
	

}
