/*
 * 2011-5-12
 * a map circle overlay implemented through div element and css color setting. very slow and cumbersome.
 */
package edu.nordborglab.client;

import com.google.gwt.maps.client.MapPane;
import com.google.gwt.maps.client.MapPaneType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.geom.LatLngBounds;
import com.google.gwt.maps.client.geom.Point;
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;

/**
 * This demo shows how to create a custom overlay in the form of a Rectangle and
 * add it to the map.
 */
public class MapCustomShapeOverlay extends Composite {
	public static class Rectangle extends AbsolutePanel {
		public Rectangle() {
			super(DOM.createDiv());
			DOM.setStyleAttribute(getElement(), "borderStyle", "solid");
		}

		public void setBorderColor(String color) {
			DOM.setStyleAttribute(getElement(), "borderColor", color);
		}

		public void setBorderWidth(String width) {
			DOM.setStyleAttribute(getElement(), "borderWidth", width);
		}
		
		public void setColor(String color) {
			DOM.setStyleAttribute(getElement(), "color", color);
		}
		public void setBackgroundCo/*
		 * 2011-5-13
		 * 	a map overlay powered by the canvas widget in GWT. It will be drawn on a MapCanvasOverlay canvas,
		 * 	which is shared by all markers like this one. MapCanvasOverlay is added to the map before all these markers
		 * 	are added.
		 */lor(String color) {
			DOM.setStyleAttribute(getElement(), "backgroundColor", color);
		}
		public void setBorderRadius(String width) {
			DOM.setStyleAttribute(getElement(), "borderRadius", width);
			DOM.setStyleAttribute(getElement(), "WebkitBorderRadius", width);
			DOM.setStyleAttribute(getElement(), "MozBorderRadius", width);
		}
	}

	public static class RectangleOverlay extends Overlay {

		private final LatLng latLng;

		private final Rectangle rectangle;
		
		private double value;
		private String title;
		private int objIndex;
		private double radius;
		private String color;
		private MapWidget map;

		private MapPane pane;
		
		
		public RectangleOverlay(LatLng latLng, double value, String title, int objIndex, double radius, String color) {
			this.latLng = latLng;
			this.value = value;
			this.title = title;
			this.objIndex = objIndex;
			this.radius = radius;
			this.color = color;
			
			rectangle = new Rectangle();
			rectangle.setBorderRadius(radius + "px");
			//rectangle.setColor(color);
			rectangle.setBorderColor("rgba(255, 0, 0, 0)");
			rectangle.setBackgroundColor(color);
			//rectangle.setBorderColor("#888888");
		}

		@Override
		protected Overlay copy() {
			return new RectangleOverlay(latLng, value,  title, objIndex, radius, color);
		}

		@Override
		protected void initialize(MapWidget map) {
			this.map = map;
			pane = map.getPane(MapPaneType.MAP_PANE);
			pane.add(rectangle);
		}

		@Override
		protected void redraw(boolean force) {
			// Only set the rectangle's size if the map's size has changed
			if (!force) {
				return;
			}

			Point pointInPixel = map.convertLatLngToDivPixel(latLng);
			pane.setWidgetPosition(rectangle, pointInPixel.getX()-(int)radius, pointInPixel.getY()-(int)radius);
			int width = (int)radius*2;
			int height = (int)radius*2;
			rectangle.setSize(width + "px", height + "px");
		}

		@Override
		protected void remove() {
			rectangle.removeFromParent();
		}
	}

}
