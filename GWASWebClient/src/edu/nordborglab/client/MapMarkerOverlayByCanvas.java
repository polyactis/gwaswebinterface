/*
 * 2011-5-13
 * 	a map overlay powered by the canvas widget in GWT. It will be drawn on a MapCanvasOverlay canvas,
 * 	which is shared by all markers like this one. MapCanvasOverlay is added to the map before all these markers
 * 	are added.
 */
package edu.nordborglab.client;

import com.google.gwt.maps.client.MapPane;
import com.google.gwt.maps.client.MapPaneType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.canvas.dom.client.FillStrokeStyle;

public class MapMarkerOverlayByCanvas extends Overlay {

	private final LatLng latLng;
	//private final SimplePanel textPanel;
	private MapCanvasOverlay canvas;
	private Context2d context;
	
	private MapWidget parentMap;

	private MapPane pane;

	private Point offset;
	private int radius;
	private double value;
	private String title;
	private int objIndex;
	public String color;
	
	final FillStrokeStyle strokeStyle = CssColor.make("#333300");
	final CssColor redrawColor = CssColor.make("rgba(255,0, 0, 0.6)");
	public final CssColor randomColor;
	
	public Point offsetPoint;
	
	/**
	 * Main constructor
	 * 
	 * @param latLng
	 */
	public MapMarkerOverlayByCanvas(MapCanvasOverlay canvas, LatLng latLng, String title, int objIndex, int radius, String color) {
		/* Save our inputs to the object */
		this.latLng = latLng;
		this.offsetPoint = Point.newInstance(0, 0);
		
		this.canvas = canvas;
		this.context = canvas.context;
		
		//this.value = value;
		this.title = title;
		this.objIndex = objIndex;
		this.radius = radius;
		this.color = color;
		
		this.randomColor = CssColor.make(color);

		/*
		 * // init the canvases
		 
		canvas.setWidth(width + "px");
		canvas.setHeight(height + "px");
		canvas.setCoordinateSpaceWidth(width);
		canvas.setCoordinateSpaceHeight(height);
		backBuffer.setCoordinateSpaceWidth(width);
		backBuffer.setCoordinateSpaceHeight(height);
		RootPanel.get(holderId).add(canvas);
		*/
		/*
		textPanel = new SimplePanel();
		textPanel.setStyleName("textOverlayPanel");
		textPanel.add(canvas);
		*/
		
		/* context gets added to the map and placed in the initialize method */
	}

	@Override
	protected final void initialize(MapWidget map) {
		/* Save a handle to the parent map widget */
		parentMap = map; // If we need to do redraws we'll need this

		/* Add canvas to the main map pane */
		//pane = map.getPane(MapPaneType.MARKER_PANE);
		
		//pane.add(canvas);	// 2011-5-12 maybe i should add this to the parent of pane.
		drawSelfWithOffset(canvas.zeroPointRelativeToPane);
		//drawSelf();
		
		
	}

	@Override
	protected final Overlay copy() {
		return new MapMarkerOverlayByCanvas(canvas, latLng, title, objIndex, radius, color);
	}
	
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
		drawMarkerOnContext(context, locationPoint.getX()-offsetPoint.getX(), locationPoint.getY()-offsetPoint.getY(), radius, randomColor);
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

}
