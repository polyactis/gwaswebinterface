package edu.nordborglab.client.visualizations.dygraphs.events;

import com.google.gwt.core.client.JavaScriptObject;

public class DataPoint extends JavaScriptObject {
	
	
	protected DataPoint() {}
	
	public final native double getCanvasX() /*-{ return this.canvasX;}-*/;
	
	public final native double  getCanvasY() /*-{ return this.canvasY;}-*/;
	
	public final native double getXVal() /*-{ return this.xval;}-*/;
	
	public final native double getYVal() /*-{ return this.yval;	}-*/;
	
	public final native  String getName() /*-{ return this.name; }-*/;
	
	public final native  int getX() /*-{ return this.x; }-*/;
	
	public final native  int getY() /*-{ return this.y; }-*/;

}

