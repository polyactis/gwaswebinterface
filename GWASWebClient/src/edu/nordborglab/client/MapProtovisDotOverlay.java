/*
 * 2011-5-13
 *  * together with MapPVDotJSOverlay,
 * 	a test which tries to use http://vis.stanford.edu/protovis/ex/oakland.html
 * but turns out a disaster as it's very hard to pass GWT's MapWidget to protovis.
 */
package edu.nordborglab.client;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.maps.client.MapPane;
import com.google.gwt.maps.client.MapPaneType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;

public class MapProtovisDotOverlay extends Overlay {

	private final LatLng latLng;
	/*
	private final PVPanel pvPanel;
	
	private final PVPanel vis;
	*/
	private final MapPVDotJSOverlay marker;
	
	private MapWidget parentMap;

	private MapPane pane;
	
	private double radius;
	
	private double value;
	private String title;
	private int objIndex;
	private double minValue;
	private double maxValue;
	
	private Point offset;
	
	//final PVLogScale dotColorScale;
	//final PVLogScale dotSizeScale;
	/**
	 * Main constructor
	 * 
	 * @param latLng
	 */
	public MapProtovisDotOverlay(LatLng latLng, double value, String title, int objIndex, double minValue, double maxValue) {
		/* Save our inputs to the object */
		this.latLng = latLng;
		this.value = value;
		this.title = title;
		this.objIndex = objIndex;
		this.minValue = minValue;
		this.maxValue = maxValue;
		
		this.offset = Point.newInstance(0, 0);
		
		/*
		pvPanel = PVPanel.create(DOM.createDiv());

		dotColorScale = PV.Scale.log(minValue, maxValue).range("lightblue", "#1f77b4");	//PV.Color.category20();
		dotSizeScale = PV.Scale.log(minValue, maxValue).range(50, 500);
		radius = dotSizeScale.fd(value);
		vis = pvPanel.width(radius*2).height(radius*2);
		
		*/
		marker = getNewMarker(latLng.getLatitude(), latLng.getLongitude(), value, title, objIndex, minValue, maxValue);
		
		/* Panel gets added to the map and placed in the initialize method */
	}
	
	
	public native MapPVDotJSOverlay getNewMarker(double lat, double lon, double value, String title, 
			int objIndex, double minValue, double maxValue)
		/*-{ return new $wnd.mapPVDot(lat, lon, value, title, objIndex, minValue, maxValue);  }-*/;
	
	@Override
	protected final void initialize(MapWidget map) {
		
		marker.initialize(map);
	}

	@Override
	protected final Overlay copy() {
		return new MapProtovisDotOverlay(latLng, value, title, objIndex, minValue, maxValue);
	}

	@Override
	protected final void redraw(boolean force) {
		marker.redraw(force);
	}

	@Override
	protected final void remove() {
		marker.remove();
	}

	public LatLng getLatLng() {
		return latLng;
	}

	public Point getOffset() {
		return offset;
	}

	public void setOffset(Point offset) {
		this.offset = offset;
	}

}
