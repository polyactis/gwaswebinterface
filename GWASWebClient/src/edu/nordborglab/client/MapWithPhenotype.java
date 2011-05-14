/*
 * 2009-4-1 yh: map widget, part of MapTableTree widget.
 *  1. wiggle the latitude, longitudes of strains with same lat,lon so they occupy different place
 *  2. map and the table in MapTableTree would respond to each other's select event
 *  3. color the accessions according to phenotypes selected or haplotype structure 
 */
package edu.nordborglab.client;


import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.visualization.client.AbstractDataTable;
import com.google.gwt.visualization.client.AbstractVisualization;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.CssColor;
import com.google.gwt.canvas.dom.client.FillStrokeStyle;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.visualization.client.AbstractDrawOptions;
import com.google.gwt.ajaxloader.client.ArrayHelper;
import com.google.gwt.visualization.client.Selectable;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.events.SelectHandler.SelectEvent;
import com.google.gwt.maps.client.InfoWindowContent;
import com.google.gwt.maps.client.MapPane;
import com.google.gwt.maps.client.MapPaneType;
import com.google.gwt.maps.client.MapWidget;
import com.google.gwt.maps.client.MapType;
import com.google.gwt.maps.client.control.LargeMapControl;
import com.google.gwt.maps.client.control.MapTypeControl;
import com.google.gwt.maps.client.geom.LatLng;
import com.google.gwt.maps.client.overlay.Marker;
import com.google.gwt.maps.client.overlay.MarkerOptions;
import com.google.gwt.maps.client.overlay.Icon; 
import com.google.gwt.maps.client.overlay.Overlay;
import com.google.gwt.maps.client.geom.Size;
import com.google.gwt.event.dom.client.ChangeHandler; 
import com.google.gwt.event.dom.client.ChangeEvent;

import com.google.gwt.maps.client.event.MapMoveEndHandler;
import com.google.gwt.maps.client.event.MapMoveHandler;
import com.google.gwt.maps.client.event.MarkerClickHandler;
import com.google.gwt.maps.client.event.MarkerMouseOverHandler;
import com.google.gwt.maps.client.InfoWindow;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import java.util.HashMap;
import edu.nordborglab.module.Pair;

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;


public class MapWithPhenotype extends AbstractVisualization<MapWithPhenotype.CustomVisualizationDrawOptions> implements Selectable{
	/**
	 * Drawing options supported by this visualization.
	 */
	public static class CustomVisualizationDrawOptions extends
	AbstractDrawOptions {
		protected CustomVisualizationDrawOptions() {
		}
	}
	
	
	private AccessionConstants constants;
	private AbstractDataTable dataTable;

	private ListBox phenotypeSelectBox = new ListBox(false);
	//private RadioButton displayOptionRadioButton;
	private ListBox displayOptionSelectBox = new ListBox(false);
	private HorizontalPanel topHPanel = new HorizontalPanel();

	private MapWidget map;
	public MapPane pane;
	
	// 2011-5-12 canvas to draw anything we want
	private MapCanvasOverlay mapCanvas;

	private VerticalPanel dialogVPanel = new VerticalPanel();
	private DisplayJSONObject jsonErrorDialog;

	private Button closeButton = new Button("Close");
	private static final String DIALOG_DEFAULT_TEXT = "Map With Phenotype";
	private static final String DIALOG_WAITING_TEXT = "Waiting...";

	private int latitude_idx = 6;
	private int longitude_idx = 7;
	private int nativename_idx = 2;
	private int ecotypeid_idx = 0;

	private final HashMap<Integer, Overlay> rowIndex2Marker = new HashMap<Integer, Overlay>();
	private int selectedRow=-1;
	private SelectHandler selectHandler;
	
	//
	private double latitude_step = 0.001;	// step size around central point
	private double longitude_step = 0.002;
	// 8 angles to circle around one point
	private Double[][] angleCoefficients = {{1.0,0.0}, {0.7071,0.7071}, {0.0,1.0}, {-0.7071,0.7071}, {-1.0,0.0}, {-0.7071,-0.7071}, {0.0,-1.0}, {0.7071,-0.7071} };
	// dictionary records how many times one point has occurred
	//private HashMap<LatLng, Integer> latlngPnt2counter = new HashMap<LatLng, Integer>();	LatLng's hashcode() is different on same GPS coordinates.
	private HashMap<Pair<Double, Double>, Integer> latlngPnt2counter = new HashMap<Pair<Double, Double>, Integer>();
	
	// map width and height
	private int width;
	private int height;
	LatLng mapCenter = LatLng.newInstance(30.93992433102344, 121.5966796875);	//it's shanghai
	
	
	public MapWithPhenotype(AccessionConstants constants, DisplayJSONObject jsonErrorDialog) {
		this.constants = constants;
		this.jsonErrorDialog = jsonErrorDialog;
		// Set the dialog box's caption.
		//setText(DIALOG_DEFAULT_TEXT);

		
		// 2009-5-3 commented out, wait for later improvement
		//fillPhenotypeSelectBox(phenotypeSelectBox);
		phenotypeSelectBox.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				refreshMap(map, phenotypeSelectBox.getSelectedIndex(), displayOptionSelectBox.getSelectedIndex());
				//multiBox.ensureDebugId("cwListBox-multiBox");
			}
		});

		String[] displayOptions = {constants.MapWithPhenotypeDisplayOption1(), constants.MapWithPhenotypeDisplayOption2(), 
				constants.MapWithPhenotypeDisplayOption3()};
		for (int i = 0; i < displayOptions.length; i++) {
			displayOptionSelectBox.addItem(displayOptions[i]);
		}
		displayOptionSelectBox.addChangeHandler(new ChangeHandler() {
			public void onChange(ChangeEvent event) {
				refreshMap(map, phenotypeSelectBox.getSelectedIndex(), displayOptionSelectBox.getSelectedIndex());
				resetMapSize();
				//multiBox.ensureDebugId("cwListBox-multiBox");
			}
		});
		topHPanel.add(phenotypeSelectBox);
		topHPanel.add(displayOptionSelectBox);
		
		
		map = new MapWidget(mapCenter, 3);
		mapCanvas = new MapCanvasOverlay(mapCenter);
		map.addOverlay(mapCanvas);
		map.addMapMoveHandler(new MapMoveHandler(){
			@Override
			public void onMove(MapMoveEvent event) {
				/*
				 * 2011-5-13 every time map moves, reset the mapCanvas
				 */
				mapCanvas.resetWidgetPositionAfterMapMove();
			}
			
		});
		/*
		map.addMapMoveEndHandler(new MapMoveEndHandler(){
			@Override
			public void onMoveEnd(MapMoveEndEvent event) {
				//mapCanvas.drawMarkerList();
			}
			
			
		});
		*/
		resetMapSize();
		
		
		// Add some controls for the zoom level
		map.addControl(new LargeMapControl());
		map.addControl(new MapTypeControl());
		map.addMapType(MapType.getPhysicalMap());	//2009-4-9 add the terrain map
		map.setCurrentMapType(MapType.getPhysicalMap());	//2009-4-9 set the terrain map as default
		map.setScrollWheelZoomEnabled(true);
		map.setVisible(true);
		
		dialogVPanel.add(map);
		dialogVPanel.add(topHPanel);
		
		initWidget(dialogVPanel);
		Window.addResizeHandler(new ResizeHandler(){
			@Override
			public void onResize(ResizeEvent event) {
				resetMapSize();
			}
		});
		
		//context.drawImage(canvas.getCanvasElement(), 0.0, 0.0);
		/*
		 * 2011-5-13 drawing in the constructor won't show anything.
		context.setFillStyle(redrawColor);
		context.fillRect(0, 0, 200, 200);
		context.fill();
		 */
		
		/*
		 * 2011-5-13 test using a timer to add random rectangles
		final Timer timer = new Timer() {
			@Override
			public void run() {
				drawSomethingNew();
			}
		};
		timer.scheduleRepeating(1500);
		*/
	}

	
	public void findLatLongCol(AbstractDataTable dataTable)
	{
		int no_of_cols = dataTable.getNumberOfColumns();

		for (int i =0; i<no_of_cols; i++)
		{
			String col_id = dataTable.getColumnId(i);
			if (col_id=="latitude")
				latitude_idx = i;
			else if (col_id=="longitude")
				longitude_idx = i;
			else if (col_id=="nativename")
				nativename_idx = i;
			else if (col_id=="tg_ecotypeid")
				ecotypeid_idx = i;
		}
	}
	
	@Override
	public void draw(AbstractDataTable dataTable, CustomVisualizationDrawOptions options)
	{
		/*
		 * 2009-4-9 required for AbstractVisualization
		 */
		addMarkers(dataTable);
	}
	
	public void addMarkers(AbstractDataTable dataTable)
	{
		this.dataTable = dataTable;
		findLatLongCol(dataTable);
		JSONObject ecotype_id2phenotype_value = null;
		double min_value = 0;
		double max_value = 0;
		String phenotype_method_id = "-1";
		int displayOption = 0;
		addMarkers(dataTable, ecotype_id2phenotype_value, min_value, max_value, phenotype_method_id, displayOption);
	}

	public void addMarkers(AbstractDataTable dataTable, JSONObject ecotype_id2phenotype_value, double min_value, double max_value, String phenotype_method_id, int displayOption)
	{
		/*
		 * 2011-4-30
		 * 	remove the shadow of each marker.
		 * 
		 * 2010
		 * 1. create 3 data tables
		 * 2. for loop to add markers (with url for the icon) 
		 * 3. add click callback on each marker
		 */
		latlngPnt2counter.clear();	//clear up
		rowIndex2Marker.clear();
		map.clearOverlays();
		mapCanvas.clearMarkerList();
		map.addOverlay(mapCanvas);
		
		double latitude;
		double longitude;
		for (int i=0; i<dataTable.getNumberOfRows(); i++)
		{
			latitude = (double)dataTable.getValueDouble(i, latitude_idx);
			longitude = (double)dataTable.getValueDouble(i, longitude_idx);
			//latitude = Random.nextInt(70);	//(double)dataTable.getValueDouble(i, latitude_idx);
			//longitude = Random.nextInt(120); //(double)dataTable.getValueDouble(i, longitude_idx);
			LatLng point = LatLng.newInstance(latitude, longitude);
			
			final String markerLabel = dataTable.getValueString(i, nativename_idx)+" ID: " + dataTable.getValueInt(i, ecotypeid_idx);
			final MarkerOptions markerOption = MarkerOptions.newInstance();
			markerOption.setTitle(markerLabel);	// title shows up as tooltip
			/*
			//2011-4-30 squash the shadow
			final Icon icon = Icon.getDefaultIcon();
			icon.setShadowSize(Size.newInstance(0, 0));
			markerOption.setIcon(icon);
			final Marker marker = new Marker(newNonOverlappingPnt(point), markerOption);
			*/
			//final MapOverlayByCanvas marker = new MapOverlayByCanvas(newNonOverlappingPnt(point), 50);
			//final MapCustomShapeOverlay.RectangleOverlay marker = new MapCustomShapeOverlay.RectangleOverlay(newNonOverlappingPnt(point), 
			//		0.5, markerLabel, i, 5, "rgba(255, 0, 0, 0.5)");
			//final MapProtovisDotOverlay marker = new MapProtovisDotOverlay(newNonOverlappingPnt(point), 0.5, markerLabel, i, 0.0, 1.0);
			//final MapMarkerTextOverlay marker = new MapMarkerTextOverlay(newNonOverlappingPnt(point), "ABC");
			final MapMarkerOverlayByCanvas marker = new MapMarkerOverlayByCanvas(mapCanvas, newNonOverlappingPnt(point), markerLabel, i, 10, "rgba(255, 0, 0, 0.5)");
			if (ecotype_id2phenotype_value== null)
			{
				
			}
			final int rowIndex = i;
			rowIndex2Marker.put(rowIndex, marker);
			map.addOverlay(marker);
			mapCanvas.addOneMarker(marker);
			
			/*
			marker.addMarkerClickHandler(new MarkerClickHandler() {
				public void onClick(MarkerClickEvent event) {
					InfoWindow info = map.getInfoWindow();
					info.open(marker.getLatLng(), new InfoWindowContent(markerLabel));
					selectedRow = rowIndex;
					//Selection.triggerSelection(MapWithPhenotype.this, getSelections());	//2009-4-9  doesn't work
					if (selectHandler!=null)	//2009-4-9 check if selectHandler is initialized.
					{
						SelectEvent e = new SelectEvent();
						selectHandler.onSelect(e);
					}
					//MapWithPhenotype.this.fireSelectionEvent();	//2009-4-9 fireSelectionEvent() below doesn't work 
				}
			});
			
			
			
			marker.addMarkerMouseOverHandler(new MarkerMouseOverHandler(){
				public void onMouseOver(MarkerMouseOverEvent event) {
					InfoWindow info = map.getInfoWindow();
					info.open(marker.getLatLng(), new InfoWindowContent(markerLabel));
				}
			});
			*/
			
		}

	}

	private class PhenotypeChangeJSONResponseHandler implements RequestCallback {
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetSearchButtonCaption();
		}

		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				JSONValue jsonValue = JSONParser.parseStrict(responseText);
				JSONObject jsonObject = jsonValue.isObject();
				double min_value = jsonObject.get("min_value").isNumber().doubleValue();
				double max_value = jsonObject.get("max_value").isNumber().doubleValue();
				JSONObject ecotype_id2phenotype_value = jsonObject.get("ecotype_id2phenotype_value").isObject();
				String phenotype_method_id = phenotypeSelectBox.getValue(phenotypeSelectBox.getSelectedIndex());
				addMarkers(dataTable, ecotype_id2phenotype_value, min_value, max_value, phenotype_method_id, displayOptionSelectBox.getSelectedIndex());
				
				//displayJSONObject(jsonValue);

			} catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetSearchButtonCaption();
		}
	}

	public void refreshMap(MapWidget map, int phenotype_method_index, int displayOption)
	{
		/*
		 * 1. fetch data (phenotype-value + icon urls)
		 * 3. addMarkers
		 */
		//setText(DIALOG_WAITING_TEXT);
		if (this.dataTable!=null)	// dataTable is not null. already initialized.
		{
			String phenotype_method_id = phenotypeSelectBox.getValue(phenotype_method_index);
			String url = URL.encode(constants.GetPhenotypeValueURL() + "/" + phenotype_method_id);
			RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
			try {
				requestBuilder.sendRequest(null, new PhenotypeChangeJSONResponseHandler());
			} catch (RequestException ex) {
				jsonErrorDialog.displaySendError(ex.toString());
				resetSearchButtonCaption();
			}
		}
	}

	private class FillPhenotypeJSONResponseHandler implements RequestCallback {
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetSearchButtonCaption();
		}

		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				JSONArray phenotypeMethodArray = JSONParser.parseStrict(responseText).isArray();
				for (int i = 0; i < phenotypeMethodArray.size(); i++) {
					JSONArray phenotypeTuple = phenotypeMethodArray.get(i).isArray();
					String phenotype_id = phenotypeTuple.get(0).isString().stringValue();
					String phenotype_name = phenotypeTuple.get(1).isString().stringValue();
					phenotypeSelectBox.addItem(phenotype_name, phenotype_id);

				}

			} catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetSearchButtonCaption();
		}
	}

	public void fillPhenotypeSelectBox(ListBox phenotypeSelectBox)
	{
		//setText(DIALOG_WAITING_TEXT);
		String url = URL.encode(constants.MapWithPhenotypeGetPhenotypeMethodLsURL());
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new FillPhenotypeJSONResponseHandler());
		} catch (RequestException ex) {
			jsonErrorDialog.displaySendError(ex.toString());
			resetSearchButtonCaption();
		}
	}
	private void resetSearchButtonCaption() {
		//setText(DIALOG_DEFAULT_TEXT);
	}
	public void setSelections(JsArray<Selection> s)
	{
		for (int i = 0; i < s.length(); ++i) {
			Integer rowIndex=null;
			if (s.get(i).isCell()) {
				rowIndex = s.get(i).getRow();
			} else if (s.get(i).isRow()) {
				rowIndex = s.get(i).getRow();
			}
			if (rowIndex!=null)
			{
				Overlay marker = rowIndex2Marker.get(rowIndex);
				//map.setCenter(marker.getLatLng());
				InfoWindow info = map.getInfoWindow();
				//info.open(marker, new InfoWindowContent(marker.getTitle()));
			}
		}
	}
	public final void addSelectHandler(SelectHandler handler)
	{
		this.selectHandler = handler;	//2009-4-9 since fireSelectionEvent() doesn't work, stuff below
		//SelectEvent event = new SelectEvent();
		//handler.onSelect(event);
		//Selection.addSelectHandler(this, handler);	//2009-4-9 doesn't work
	}
	
	public JsArray<Selection> getSelections()
	{
		//JsArray<Selection> sel = new JsArray<Selection>();
		return ArrayHelper.toJsArray(Selection.createRowSelection(selectedRow));
	}
	
	private LatLng newNonOverlappingPnt(LatLng point)
	{
		Pair<Double, Double> key = Pair.from(point.getLatitude(), point.getLongitude());
		if (!latlngPnt2counter.containsKey(key))
		{
			
			latlngPnt2counter.put(key, 0);
		}
		latlngPnt2counter.put(key, latlngPnt2counter.get(key)+1);
		if (latlngPnt2counter.get(key)==1)	// 1st point, no wiggling
			return point;
		
		int no_of_rounds = (latlngPnt2counter.get(key)-2)/angleCoefficients.length + 1;	//1st point = 1 (-1/8=0, not -1), 2nd-9th = 1, etc
		int angleCoefficientsIdx = (latlngPnt2counter.get(key)-2)%angleCoefficients.length;
		/*
		// 2009-4-9 debug purpose
		String errorStr = " latlngPnt2counter.get(key)= "+latlngPnt2counter.get(key);
		errorStr += "\n angleCoefficients.length= "+angleCoefficients.length;
		errorStr += "\n no_of_rounds= "+no_of_rounds;
		errorStr += "\n angleCoefficientsIdx= "+angleCoefficientsIdx;
		jsonErrorDialog.displayParseError(errorStr);
		*/
		double newLat = point.getLatitude() + no_of_rounds*angleCoefficients[angleCoefficientsIdx][0]*latitude_step;
		double newLon = point.getLongitude() + no_of_rounds*angleCoefficients[angleCoefficientsIdx][1]*longitude_step;
		LatLng newPoint = LatLng.newInstance(newLat, newLon);
		
		// add the new point to the dictionary as well
		key = Pair.from(newPoint.getLatitude(), newPoint.getLongitude());
		if (!latlngPnt2counter.containsKey(key))
		{
			latlngPnt2counter.put(key, 0);
		}
		latlngPnt2counter.put(key, latlngPnt2counter.get(key)+1);
		
		return newPoint;
	}
	
	public void resetMapSize()
	{
		width = Window.getClientWidth()-50;
		height = Window.getClientHeight() - 250;
		map.setSize(width+"px", height+"px");
		mapCanvas.reSizeWidget();
		mapCanvas.resetWidgetPosition();
		/*
		mapCanvas.canvas.setWidth(width+"px");
		mapCanvas.canvas.setHeight(height + "px");
		mapCanvas.canvas.setCoordinateSpaceWidth(width);
		mapCanvas.canvas.setCoordinateSpaceHeight(height);
		*/
		//map.setSize(mapWidth, mapHeight);
	}
}
