/* 
 * 
 * 2011-5-13 copied from http://timlwhite.typepad.com/blog/2008/08/creating-custom-google-maps-overlays-with-gwt-widgets.html
 * 
 * I need to add something to the redraw() for it to work after map is moved or zoomed etc.
 * Original version puts nothing in the redraw()
 * 
 * Custom Map Overlay Code - Copyright 2008 Cyface Design, Released Under the Apache 2.2 License
 * 
 * */

package edu.nordborglab.client;


import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.maps.client.MapPane;
import com.google.gwt.maps.client.MapPaneType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.event.MarkerMouseOverHandler;
import com.google.gwt.maps.client.event.MarkerClickHandler.MarkerClickEvent;
import com.google.gwt.maps.client.event.MarkerMouseOverHandler.MarkerMouseOverEvent;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.impl.HandlerCollection;
import com.google.gwt.maps.client.impl.MapEvent;
import com.google.gwt.maps.client.impl.EventImpl.VoidCallback;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;

public class MapMarkerTextOverlay extends Overlay{

	private final LatLng latLng;

	private final SimplePanel textPanel;

	private MapWidget parentMap;

	private MapPane pane;

	private String text;

	private Point offset;

	// Keep track of JSO's registered for each instance of addXXXListener()
	private HandlerCollection<MarkerClickHandler> markerClickHandlers;
	private HandlerCollection<MarkerMouseOverHandler> markerMouseOverHandlers;
	
	
	/**
	 * Main constructor
	 * 
	 * @param latLng
	 */
	public MapMarkerTextOverlay(LatLng latLng, String text) {
		/* Save our inputs to the object */
		this.latLng = latLng;
		this.text = text;
		this.offset = Point.newInstance(0, 0);

		/* Create a widget for the text */
		HTML textWidget = new HTML(text);

		/* Create the panel to hold the text */
		textPanel = new SimplePanel();
		textPanel.setStyleName("textOverlayPanel");
		textPanel.add(textWidget);

		/* Panel gets added to the map and placed in the initialize method */
	}

	@Override
	protected final void initialize(MapWidget map) {
		/* Save a handle to the parent map widget */
		parentMap = map; // If we need to do redraws we'll need this

		/* Add our textPanel to the main map pane */
		pane = map.getPane(MapPaneType.MARKER_PANE);
		pane.add(textPanel);

		/* Place the textPanel on the pane in the correct spot */
		Point locationPoint = parentMap.convertLatLngToDivPixel(getLatLng());
		Point offsetPoint = Point.newInstance(locationPoint.getX() - getOffset().getX(),
						locationPoint.getY() - getOffset().getY());
		pane.setWidgetPosition(textPanel, offsetPoint.getX(), offsetPoint.getY());
	}

	@Override
	protected final Overlay copy() {
		return new MapMarkerTextOverlay(getLatLng(), getText());
	}

	@Override
	protected final void redraw(boolean force) {
		if (!force)
		{
			return;
		}
		
		Point locationPoint = parentMap.convertLatLngToDivPixel(getLatLng());
		Point offsetPoint = Point.newInstance(locationPoint.getX() - getOffset().getX(),
						locationPoint.getY() - getOffset().getY());
		pane.setWidgetPosition(textPanel, offsetPoint.getX(), offsetPoint.getY());
	}

	@Override
	protected final void remove() {
		textPanel.removeFromParent();
	}

	public LatLng getLatLng() {
		return latLng;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Point getOffset() {
		return offset;
	}

	public void setOffset(Point offset) {
		this.offset = offset;
	}
	
	


}
