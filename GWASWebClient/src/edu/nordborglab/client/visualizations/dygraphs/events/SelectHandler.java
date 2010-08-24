package edu.nordborglab.client.visualizations.dygraphs.events;

import com.google.gwt.ajaxloader.client.Properties;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public abstract class SelectHandler extends Handler {
	
	
	
	/**
	   * The select event is fired when the user selects data displayed in the
	   * visualization. The SelectEvent class is a placeholder.
	   */
	  public static class SelectEvent {
		  private JavaScriptObject event;
		  private double x_Value;
		  private JsArray<DataPoint> points;
		  
		  public SelectEvent(JavaScriptObject event,double x_Value,JsArray<DataPoint> points)
		  {
			  this.event = event;
			  this.x_Value = x_Value;
			  this.points = points;
		  }
		  
		  public JavaScriptObject getEvent() {
			  return this.event; 
		  }
		  
		  public double getXValue()
		  {
			  return this.x_Value;
		  }
		  public JsArray<DataPoint> getPoints() {
			  return this.points;
		  }
		  
	  }

	  public abstract void onSelect(SelectEvent event);

	  @Override
	  protected void onEvent(Properties properties) throws Properties.TypeException {
		  JavaScriptObject event = properties.getObject("event");
		  double x_Value = properties.getNumber("x_Value");
		  JsArray<DataPoint> points = properties.getObject("points").cast();
		  onSelect(new SelectEvent(event,x_Value,points));
	  }

}
