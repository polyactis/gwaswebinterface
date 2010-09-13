package edu.nordborglab.client.visualizations.dygraphs;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.AbstractDrawOptions;
import com.google.gwt.visualization.client.visualizations.Visualization;
import edu.nordborglab.client.visualizations.dygraphs.events.SelectHandler;


public class Dygraphs extends Visualization<Dygraphs.Options>  {
	
	
	public static class Options extends AbstractDrawOptions {
	    
		
		public static Options create() {
	      return JavaScriptObject.createObject().cast();
	    }
		
		public final void setStrokeWidth(double width) {
			this.set("strokeWidth",width);
		}
		
		public final void setDrawPoints(boolean drawPoints) {
			this.set("drawPoints",drawPoints);
		}
		
		public final void setIncludeZero(boolean includeZero) {
			this.set("includeZero",includeZero);
		}
		
		public final void setAxisLabelFontSize(int size)
		{
			this.set("axisLabelFontSize",(double)size);
		}
		
		public final void setxAxisLabelWidth(int size)
		{
			this.set("xAxisLabelWidth",(double)size);
		}
		public final void setyAxisLabelWidth(int size)
		{
			this.set("yAxisLabelWidth",(double)size);
		}
		
		public final void setColorValue(double color)
		{
			this.set("colorValue",color);
		}
		
		public final void setColorSaturation(double color)
		{
			this.set("colorSaturation",color);
		}
		
		public final void setColors(String[] colors)
		{
			JsArrayString array = JsArrayString.createArray().cast();
			for (int i =0;i< colors.length;i++)
				array.push(colors[i]);
			this.set("colors",array);
		}
		
		public final void setPointSize(int pointSize) {
			this.set("pointSize",(double)pointSize);
		}
		
		public final void setGridLineColor(String gridLineColor)
		{
			this.set("gridLineColor",gridLineColor);
		}
		
		public final void setWidth(int width)
		{
			this.set("width",(double)width);
		}
		public final void setHeight(int height)
		{
			this.set("height",(double)height);
		}
		
		public final void setIncludeYPositionForHightlight(boolean includeYPositionForHightlight)
		{
			this.set("includeYPositionForHightlight",includeYPositionForHightlight);
		}
		
		public final void setMinimumDistanceForHighlight(int minimumDistanceForHighlight)
		{
			this.set("minimumDistanceForHighlight", (double)minimumDistanceForHighlight);
		}
		
		public final void setValueRange(int min,int max)
		{
			JsArrayInteger array = JsArrayInteger.createArray().cast();
			array.set(0, min);
			array.set(1,max);
			this.set("valueRange", array);
		}

	    protected Options() {}
	    
	}
	
	public static final String PACKAGE = "dygraphs";
	
	public Dygraphs() {
		super();
	}
	
	public Dygraphs(AbstractDataTable data,Options options) {
		super(data,options);
	}
	
	@Override
	protected native JavaScriptObject createJso(Element parent) /*-{
		return new $wnd.Dygraph.GVizChart(parent);
	}-*/;

	
	
/*	@Override
	public JsArray<Selection> getSelections() {
		return Selection.getSelections(this);
	}

	@Override
	public void setSelections(JsArray<Selection> sel) {
		Selection.setSelections(this, sel);
		
	}
*/
	
	public void addSelectHandler(SelectHandler handler) {
		SelectHandler.addHandler(this, "clickCallback", handler);
	}
}


