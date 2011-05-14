/*
 * 2011-5-13
 * together with MapProtovisDotOverlay,
 * 	a test which tries to use http://vis.stanford.edu/protovis/ex/oakland.html
 * but turns out a disaster as it's very hard to pass GWT's MapWidget to protovis.
 */

package edu.nordborglab.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;

public class MapPVDotJSOverlay extends JavaScriptObject {
	// Overlay types always have protected, zero-arg ctors
	protected MapPVDotJSOverlay() { }

	// Typically, methods on overlay types are JSNI
	public final native LatLng getLatLng() /*-{ return new $wnd.GLatLng(this.lat, this.lon); }-*/;
	public final native int getObjIndex()  /*-{ return this.objIndex;  }-*/;

	// Note, though, that methods aren't required to be JSNI
	protected final native void remove() /*-{ this.remove();  }-*/;
	
	// Note, though, that methods aren't required to be JSNI
	protected final native void redraw(boolean force) /*-{ this.redraw(force);  }-*/;
	
	
	protected final native void initialize(MapWidget map) /*-{
		this.map = map;
		this.canvas = $wnd.document.createElement("div");
		this.canvas.setAttribute("class", "canvas");
		//Call instance method instanceFoo() on x
		//var pane = map.@com.google.gwt.maps.client.MapWidget::getPane(Ljava/lang/String;)(G_MAP_MAP_PANE);
		//pane.parentNode.appendChild(this.canvas);
		//map.getPane(G_MAP_MAP_PANE).parentNode.appendChild(this.canvas);
		}-*/;
}
