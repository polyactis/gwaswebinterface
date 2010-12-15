package edu.nordborglab.client.visualizations;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.AbstractDrawOptions;
import com.google.gwt.visualization.client.Selectable;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.events.Handler;
import com.google.gwt.visualization.client.events.ReadyHandler;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.events.StateChangeHandler;
import com.google.gwt.visualization.client.visualizations.Visualization;

public class BioHeatMap extends Visualization<BioHeatMap.Options>  implements
    Selectable{

	public static class Options extends AbstractDrawOptions {
	    
		public static class Colors extends JavaScriptObject
		{
			public static Colors create() {
			      return JavaScriptObject.createObject().cast();
			}
			
			protected Colors() {
		    }
			
			public final void setColor(Integer r, Integer g, Integer b, Integer a)
			{
				this.setRGBRed(r);
				this.setRGBBlue(b);
				this.setRGBGreen(g);
				this.setAlpha(a);
			}
			
			public final native void setRGBRed(Integer r) /*-{
		    	this.r = r;
		    }-*/;
			
			public final native void setRGBGreen(Integer g) /*-{
	    		this.g = g;
	    	}-*/;
			
			public final native void setRGBBlue(Integer b) /*-{
    			this.b = b;
    		}-*/;
			
			public final native void setAlpha(Integer a) /*-{
    			this.a = a;
    		}-*/;
		}
		
		public static Options create() {
	      return JavaScriptObject.createObject().cast();
	    }

	    protected Options() {
	    }
	    
	    public final void setStartColor(Options.Colors startColor) {
    		this.set("startColor",startColor);
    	}
	   
	    public final void setEndColor(Options.Colors endColor) {
	    	this.set("endColor",endColor);
		}
	    
	    public final void setEmptyDataColor(Options.Colors emptyDataColor) {
			this.set("emptyDataColor",emptyDataColor);
		}
	    
	    public final void setNumberOfColors(Integer numberOfColors) {
	    	this.set("numberOfColors",(double)numberOfColors);
	    }
	    
	    public final void setPassThroughBlack(Boolean passThroughBlack) {
	    	this.set("passThroughBlack", passThroughBlack);
	    }
	    
	    public final void setUseRowLabels(Boolean useRowLabels) {
			this.set("useRowLabels",useRowLabels);
		}
	    
	    public final void setCellWidth(Integer cellWidth) {
    		this.set("cellWidth",(double)cellWidth);
    	}
	    
	    public final void setCellHeight(Integer cellHeight) {
			this.set("cellHeight",(double)cellHeight);
		}
	    
	    public final void setMapWidth(Integer mapWidth) {
			this.set("mapWidth",(double)mapWidth);
		}
		    
		public final void setMapHeight(Integer mapHeight) {
			this.set("mapHeight",(double)mapHeight);
		}
		    
		public final void setFontSize(Integer fontSize) {
			this.set("fontSize",(double)fontSize);
		}
		    
		public final void setVerticalPadding(Integer verticalPadding) {
			this.set("verticalPadding",(double)verticalPadding);
		}
		    
		public final void setHorizontalPadding(Integer horizontalPadding) {
			this.set("horizontalPadding",(double)horizontalPadding);
		}
	    
	    public final void setDrawBorder(Boolean drawBorder) {
			this.set("drawBorder",drawBorder);
		}
	    public final void setCellBorder(Boolean cellBorder) {
			this.set("cellBorder",cellBorder);
		}
	}
	
	public static final String PACKAGE = "bioheatmap";
	
	public BioHeatMap() {
		super();
	}
	
	public BioHeatMap(AbstractDataTable data,Options options) {
		super(data,options);
	}
	
	@Override
	protected native JavaScriptObject createJso(Element parent) /*-{
		return new $wnd.org.systemsbiology.visualization.BioHeatMap(parent);
	}-*/;

	@Override
	public void addSelectHandler(SelectHandler handler) {
		Selection.addSelectHandler(this, handler);
	}
	
	public void addOnMouseMoveHandler(OnMouseMoveHandler handler)
	{
		Handler.addHandler(this, "mousemove", handler);
	}
	
	
	@Override
	public JsArray<Selection> getSelections() {
		return Selection.getSelections(this);
	}

	@Override
	public void setSelections(JsArray<Selection> sel) {
		Selection.setSelections(this, sel);
		
	}
}


