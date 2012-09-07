/*
 * 2011-6-3
 * 	modeled after MarkerClickHandler from gwt maps
 */
package edu.nordborglab.client.event;

import java.util.EventObject;

import edu.nordborglab.client.MapMarkerOverlayByCanvas;

/**
 * Provides an interface to implement in order to receive MapEvent.CLICK events
 * from the {@link Marker}.
 */
public interface MapCanvasMarkerClickHandler {

	/**
	 * Encapsulates the arguments for the MapEvent.CLICK event on a
	 * {@link Marker}.
	 */

	@SuppressWarnings("serial")
	class MarkerClickEvent extends EventObject {

		public MarkerClickEvent(MapMarkerOverlayByCanvas source) {
			super(source);
		}

		/**
		 * Returns the instance of the map that generated this event.
		 * 
		 * @return the instance of the map that generated this event.
		 */
		public MapMarkerOverlayByCanvas getSender() {
			return (MapMarkerOverlayByCanvas) getSource();
		}
	}

	/**
	 * Method to be invoked when a MapEvent.CLICK event fires on a
	 * {@link Marker}.
	 * 
	 * @param event
	 *            contains the properties of the event.
	 */
	void onClick(MarkerClickEvent event);
}
