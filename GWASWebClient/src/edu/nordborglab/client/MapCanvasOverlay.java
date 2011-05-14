/*
 * 2011-5-13 a whole-map canvas, used as a basis to draw individual markerList (another overlay).
 * The markers (MapMarkerOverlayByCanvas) are in the coordinate system of the map pane but are
 * drawn on this shared canvas, which covers the whole visible part of the pane.
 */
package edu.nordborglab.client;

import java.util.ArrayList;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.maps.client.MapPane;
import com.google.gwt.maps.client.MapPaneType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.overlay.Overlay;

public class MapCanvasOverlay  extends Overlay {
	private final LatLng latLng;
	public Canvas canvas;
	public Context2d context;
	
	private MapWidget parentMap;

	private MapPane pane;
	
	public int width;
	public int height;
	
	//public MapMarkerOverlayByCanvas[] markerList;
	public ArrayList<MapMarkerOverlayByCanvas> markerList;
	
	
	/*
	 * 2011-5-13
	 * an important variable recording the coordinates of (0,0) of this canvas in the coordinate system of the Map pane.
	 * most of the time, it should still be (0,0). However, when user starts to move the map by dragging the mouse, the two differ.
	 * The (0,0) of the map pane is stuck with its original GPS coordinate,
	 * 	while the canvas has to be moved to cover the visible part of the whole pane and thus the difference.
	 */
	public Point zeroPointRelativeToPane;
	
	public MapCanvasOverlay(LatLng latLng) {
		/* Save our inputs to the object */
		this.latLng = latLng;
		
		canvas = Canvas.createIfSupported();
		context = canvas.getContext2d();
		markerList = new ArrayList<MapMarkerOverlayByCanvas>(0);
		
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
		return new MapCanvasOverlay(latLng);
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
		//reSizeCanvas(width, height);
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


}
