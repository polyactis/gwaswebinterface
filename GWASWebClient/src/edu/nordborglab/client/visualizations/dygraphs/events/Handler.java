package edu.nordborglab.client.visualizations.dygraphs.events;
import com.google.gwt.ajaxloader.client.Properties;
import com.google.gwt.ajaxloader.client.Properties.TypeException;
import com.google.gwt.core.client.GWT;
import com.google.gwt.visualization.client.visualizations.Visualization;

public abstract class Handler {
	
	 
	public static native void addHandler(Visualization<?> viz,String eventName,Handler handler) /*-{
		var jso = viz.@edu.nordborglab.client.visualizations.dygraphs.Dygraphs::getJso()();
		if (eventName == 'clickCallback')
		{
			var callback = function(event,x,pts) {
				var props = @com.google.gwt.ajaxloader.client.Properties::create()();
				props.event = event;
				props.x_Value =x;
				props.points = pts;
				@edu.nordborglab.client.visualizations.dygraphs.events.Handler::onCallback(Ledu/nordborglab/client/visualizations/dygraphs/events/Handler;Lcom/google/gwt/ajaxloader/client/Properties;)(handler, props);
			}
		}
		jso.date_graph.user_attrs_['clickCallback'] = callback;
	}-*/;
	
	
	  @SuppressWarnings("unused")
	  private static void onCallback(final Handler handler,
	      final Properties properties) {
	    try {
	      handler.onEvent(properties);
	    } catch (Throwable x) {
	      GWT.getUncaughtExceptionHandler().onUncaughtException(x);
	    }
	  }
	 
	 /**
	   * This method should be overridden by event-specific Handler subclasses. The
	   * subclass should extract the event properties (if any), create a GWT Event
	   * bean object, and pass it to the event-specific callback.
	   * 
	   * @param properties The JavaScriptObject containing data about the event.
	   * @throws TypeException If some property of the event has an unexpected type.
	   */
	  protected abstract void onEvent(Properties properties) throws TypeException;
	 

}
