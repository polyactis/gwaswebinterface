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
import com.google.gwt.user.client.ui.Label;
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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;


import edu.nordborglab.client.event.MapCanvasMarkerClickHandler;
import edu.nordborglab.client.event.MapCanvasMarkerMouseOverHandler;
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
	public static class ClusterData{
		private int clusterID;
		public LatLng latLng;
		public String title;
		public ArrayList<Integer> accession_id_list;
		public ArrayList<Integer> rowIndexArrayList;
		public ArrayList<Double> sizeValueList;
		public ArrayList<Double> colorValueList;
		public ClusterData(){
			accession_id_list = new ArrayList<Integer>(0);
			rowIndexArrayList = new ArrayList<Integer>(0);
			sizeValueList = new ArrayList<Double>(0);
			colorValueList = new ArrayList<Double>(0);
		}
		public void setCluserID(int clusterID){
			this.clusterID = clusterID;
		}
		public void getClusterID(int clusterID){
			this.clusterID = clusterID;
		}
	}
	
	private AccessionConstants constants;
	private AbstractDataTable dataTable;
	private MapTableTree.PassingData passingData;
	
	private HorizontalPanel topHPanel = new HorizontalPanel();
	
	private ListBox sizeByWhatSelectBox = new ListBox(false);
	private ListBox colorByWhatSelectBox = new ListBox(false);
	private ListBox binOptionListBox = new ListBox(false);	//every single value is a different category or bin them into 5 bins.
	private ListBox plotOptionListBox = new ListBox(false);	//piechart with pie for every category or circle with one color for median of all values 
	private ListBox displayOptionSelectBox = new ListBox(false);	
	private Button refreshButton = new Button("refresh");
	
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
	private int accession_id_idx = 0;

	private final HashMap<Integer, Overlay> rowIndex2Marker = new HashMap<Integer, Overlay>();
	private final HashMap<Pair<Double, Double>, ClusterData> latlng2clusterData = new HashMap<Pair<Double, Double>, ClusterData>();
	private final HashMap<Integer, ClusterData> clusterID2Data = new HashMap<Integer, ClusterData>();
	private final HashMap<Integer, Integer> rowIndex2ClusterID = new HashMap<Integer, Integer>();
	public HashMap<Integer, ClusterData> clusterIDMap;	// not used as i don't know how to use it in iterator
	private HashMap<Integer, Double> accession_id2valueForColor = new HashMap<Integer, Double>();
	private HashMap<Integer, Double> accession_id2valueForSize = new HashMap<Integer, Double>();
	
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
	
	
	public MapWithPhenotype(AccessionConstants constants, DisplayJSONObject jsonErrorDialog, MapTableTree.PassingData passingData) {
		this.constants = constants;
		this.jsonErrorDialog = jsonErrorDialog;
		this.passingData = passingData;
		
		// Set the dialog box's caption.
		//setText(DIALOG_DEFAULT_TEXT);
		
		
		fillByWhatSelectBox(sizeByWhatSelectBox);
		sizeByWhatSelectBox.addChangeHandler(new mapOptionListBoxChangeHandler());
		
		fillByWhatSelectBox(colorByWhatSelectBox);
		colorByWhatSelectBox.addChangeHandler(new mapOptionListBoxChangeHandler());
		
		String[] binOptions = {constants.MapWithPhenotypeBinOption1(), constants.MapWithPhenotypeBinOption2()};
		Common.fillListBox(binOptions, binOptionListBox);
		binOptionListBox.addChangeHandler(new mapOptionListBoxChangeHandler());
		
		String[] plotOptions = {constants.MapWithPhenotypePlotOption1(), constants.MapWithPhenotypePlotOption2()};
		Common.fillListBox(plotOptions, plotOptionListBox);
		plotOptionListBox.addChangeHandler(new mapOptionListBoxChangeHandler());
		
		String[] displayOptions = {constants.MapWithPhenotypeDisplayOption1(), constants.MapWithPhenotypeDisplayOption2(), 
				constants.MapWithPhenotypeDisplayOption3()};
		Common.fillListBox(displayOptions, displayOptionSelectBox);
		displayOptionSelectBox.addChangeHandler(new mapOptionListBoxChangeHandler());
		
		refreshButton.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				// TODO Auto-generated method stub
				refreshMap();
				resetMapSize();
			}
			
		});
		
		topHPanel.add(new Label("Size by:"));
		topHPanel.add(sizeByWhatSelectBox);
		topHPanel.add(new Label("Color by:"));
		topHPanel.add(colorByWhatSelectBox);
		//topHPanel.add(new Label("Bin values of same-site individuals:"));
		//topHPanel.add(binOptionListBox);
		topHPanel.add(new Label("How to summarize the bins:"));
		topHPanel.add(plotOptionListBox);
		
		topHPanel.add(displayOptionSelectBox);
		topHPanel.add(refreshButton);
		
		map = new MapWidget(mapCenter, 3);
		
		double minValue = 0.0;
		double maxValue = 1.0;
		int binOption = 1;
		int plotOption = 1;
		boolean value2SizeLogScale = true;
		boolean value2ColorLogScale = false;
		mapCanvas = new MapCanvasOverlay(mapCenter, minValue, maxValue, minValue, maxValue, binOption, plotOption,
				value2SizeLogScale, value2ColorLogScale);
		map.addOverlay(mapCanvas);
		map.addMapMoveHandler(new MapMoveHandler(){
			@Override
			public void onMove(MapMoveEvent event) {
				/*
				 * 2011-5-13 every time map moves, reset the mapCanvas to the top left corner.
				 */
				mapCanvas.resetWidgetPositionAfterMapMove();
			}
			
		});
		
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

	
	public class mapOptionListBoxChangeHandler implements ChangeHandler{
		public void onChange(ChangeEvent event) {
			refreshMap();
			resetMapSize();
		}
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
			else if (col_id=="accession_id")
				accession_id_idx = i;
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
		addMarkers(dataTable, mapCanvas, 0, 0);	//2nd-last 0 (sizeAttributeID) means every cluster is drawn of same size.
		//last 0 (colorAttributeID) is for coloring.
	}
	
	public void addMarkers(AbstractDataTable dataTable, MapCanvasOverlay mapCanvas, int sizeAttributeID, 
			int colorAttributeID)
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
		int displayOption_idx = displayOptionSelectBox.getSelectedIndex();
		clusterDataTable(dataTable, accession_id2valueForSize, accession_id2valueForColor,
				mapCanvas, sizeAttributeID, colorAttributeID, displayOption_idx);
		// TODO adjust the sizeValueList and colorValueList for each cluster if attribute_id is 0 (same) or -1 (size)
		// TODO also adjust the minValue, maxValue
		
		map.clearOverlays();
		mapCanvas.clearMarkerList();
		
		int binOption_idx = binOptionListBox.getSelectedIndex();
		int plotOption_idx = plotOptionListBox.getSelectedIndex();
		boolean value2SizeLogScale = true;
		boolean value2ColorLogScale = false;
		
		mapCanvas.resetOptions(binOption_idx+1, plotOption_idx+1, value2SizeLogScale, value2ColorLogScale);
		map.addOverlay(mapCanvas);
		
		
		//double latitude;
		//double longitude;
		
		//Iterator it = clusterID2Data.entrySet().iterator();
		Iterator it = latlng2clusterData.keySet().iterator();
		while (it.hasNext()) {
			ClusterData clusterData = latlng2clusterData.get(it.next());
			
			//clusterIDMap.Entry pairs = (clusterIDMap.Entry) it.next();
			//System.out.println(pairs.getKey() + " = " + pairs.getValue());
		
			//final String markerLabel = dataTable.getValueString(i, nativename_idx)+" ID: " + dataTable.getValueInt(i, accession_id_idx);
			final String markerLabel = "cluster ID: " + clusterData.clusterID + ". " + clusterData.accession_id_list.size() + " accessions.";
			//final MarkerOptions markerOption = MarkerOptions.newInstance();
			//markerOption.setTitle(markerLabel);	// title shows up as tooltip
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
			final MapMarkerOverlayByCanvas marker = new MapMarkerOverlayByCanvas(mapCanvas, 
					newNonOverlappingPnt(clusterData.latLng), markerLabel, clusterData.accession_id_list,
					clusterData.sizeValueList, clusterData.colorValueList, clusterData.clusterID);
			
			for (int i=0; i<clusterData.rowIndexArrayList.size(); i++){
				int rowIndex = clusterData.rowIndexArrayList.get(i);
				rowIndex2Marker.put(rowIndex, marker);
			}
			map.addOverlay(marker);
			mapCanvas.addOneMarker(marker);
			
			marker.addMarkerClickHandler(new MapCanvasMarkerClickHandler() {
				public void onClick(MarkerClickEvent event) {
					InfoWindow info = map.getInfoWindow();
					info.open(marker.getLatLng(), new InfoWindowContent(markerLabel));
					//selectedRow = rowIndex;
					//Selection.triggerSelection(MapWithPhenotype.this, getSelections());	//2009-4-9  doesn't work
					if (selectHandler!=null)	//2009-4-9 check if selectHandler is initialized.
					{
						SelectEvent e = new SelectEvent();
						selectHandler.onSelect(e);
					}
					//MapWithPhenotype.this.fireSelectionEvent();	//2009-4-9 fireSelectionEvent() below doesn't work 
				}
			});
			
			
			
			marker.addMarkerMouseOverHandler(new MapCanvasMarkerMouseOverHandler(){
				public void onMouseOver(MarkerMouseOverEvent event) {
					InfoWindow info = map.getInfoWindow();
					info.open(marker.getLatLng(), new InfoWindowContent(markerLabel));
				}
			});
			
			
		}

	}

	private class ColorByWhatChangeJSONResponseHandler implements RequestCallback {
		private double minValueForSize;
		private double maxValueForSize;
		private int sizeAttributeID;
		private int colorAttributeID;
		public ColorByWhatChangeJSONResponseHandler(double minValueForSize, double maxValueForSize,
				int sizeAttributeID, int colorAttributeID)
		{
			this.minValueForSize = minValueForSize;
			this.maxValueForSize = maxValueForSize;
			this.sizeAttributeID = sizeAttributeID;
			this.colorAttributeID = colorAttributeID;
		}
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetSearchButtonCaption();
		}
		
		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				JSONValue jsonValue = JSONParser.parseStrict(responseText);
				JSONObject jsonObject = jsonValue.isObject();
				double minValueForColor = jsonObject.get("min_value").isNumber().doubleValue();
				double maxValueForColor = jsonObject.get("max_value").isNumber().doubleValue();
				JSONObject accession_id2attribute_value = jsonObject.get("accession_id2attribute_value").isObject();
				convertJSONDictIntoHashMap(accession_id2attribute_value, accession_id2valueForColor);
				mapCanvas.minValueForSize = this.minValueForSize;
				mapCanvas.maxValueForSize = this.maxValueForSize;
				mapCanvas.minValueForColor = minValueForColor;
				mapCanvas.maxValueForColor = maxValueForColor;
				addMarkers(dataTable, mapCanvas, sizeAttributeID, colorAttributeID);
				
				//displayJSONObject(jsonValue);
			} catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetSearchButtonCaption();
		}
	}
	
	/*
	 * 2011-5-15
	 * 	do another ColorByWhatChangeJSONResponseHandler() if request returns good.
	 */
	private class SizeByWhatChangeJSONResponseHandler implements RequestCallback {
		private int sizeAttributeID;
		public SizeByWhatChangeJSONResponseHandler(int sizeAttributeID){
			this.sizeAttributeID = sizeAttributeID;
		}
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetSearchButtonCaption();
		}

		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				JSONValue jsonValue = JSONParser.parseStrict(responseText);
				JSONObject jsonObject = jsonValue.isObject();
				double minValueForSize = jsonObject.get("min_value").isNumber().doubleValue();
				double maxValueForSize = jsonObject.get("max_value").isNumber().doubleValue();
				
				int colorByWhat_idx = colorByWhatSelectBox.getSelectedIndex();
				String colorByWhat = colorByWhatSelectBox.getValue(colorByWhat_idx);	// colorByWhat is id from server.
				JSONObject accession_id2attribute_value = jsonObject.get("accession_id2attribute_value").isObject();
				convertJSONDictIntoHashMap(accession_id2attribute_value, accession_id2valueForSize);

				String colorURL = URL.encode(passingData.accessionAttributeDataURL + "&attribute_id=" + colorByWhat);
				RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, colorURL);
				try {
					requestBuilder.sendRequest(null, new ColorByWhatChangeJSONResponseHandler(minValueForSize, 
							maxValueForSize, sizeAttributeID, Integer.parseInt(colorByWhat) ));
				} catch (RequestException ex) {
					jsonErrorDialog.displaySendError(ex.toString());
					resetSearchButtonCaption();
				}

			} catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetSearchButtonCaption();
			
		}
	}
	
	/*
	 * 1. two consecutive URL requests
	 * 		1. URL request for size
	 * 		2. if size request turns good, do the color URL request
	 */
	public void refreshMap()
	{
		//setText(DIALOG_WAITING_TEXT);
		if (this.dataTable!=null)	// dataTable is not null. already initialized.
		{
			int sizeByWhat_idx = sizeByWhatSelectBox.getSelectedIndex();			
			String sizeByWhat = sizeByWhatSelectBox.getValue(sizeByWhat_idx);	// sizeByWhat is id from server.
			String sizeURL = URL.encode(this.passingData.accessionAttributeDataURL + "&attribute_id=" + sizeByWhat);
			
			RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, sizeURL);
			try {
				requestBuilder.sendRequest(null, new SizeByWhatChangeJSONResponseHandler(Integer.parseInt(sizeByWhat)));
			} catch (RequestException ex) {
				jsonErrorDialog.displaySendError(ex.toString());
				resetSearchButtonCaption();
			}
		}
	}
	
	/*
	 * 2011-5-11
	 * 	function to group accessions into clusters. current clustering is just based on (lat, lng) pair.
	 */
	public void clusterDataTable(AbstractDataTable dataTable, HashMap<Integer, Double> accession_id2valueForSize,
			HashMap<Integer, Double> accession_id2valueForColor, MapCanvasOverlay mapCanvas, 
			int sizeAttributeID, int colorAttributeID, int displayOption_idx){
		double minValueForSize = mapCanvas.minValueForSize;
		double maxValueForSize = mapCanvas.maxValueForSize;
		
		double minValueForColor = mapCanvas.minValueForColor;
		double maxValueForColor = mapCanvas.maxValueForColor; 
		
		double latitude;
		double longitude;
		int accession_id;
		// 2011-5-20 clear all cluster-related data structures first. 
		latlng2clusterData.clear();
		clusterID2Data.clear();
		rowIndex2ClusterID.clear();
		for (int i=0; i<dataTable.getNumberOfRows(); i++)
		{
			
			latitude = (double)dataTable.getValueDouble(i, latitude_idx);
			longitude = (double)dataTable.getValueDouble(i, longitude_idx);
			accession_id = (int)dataTable.getValueInt(i, accession_id_idx);
			/*
			latitude = Random.nextInt(70);	//(double)dataTable.getValueDouble(i, latitude_idx);
			longitude = Random.nextInt(120); //(double)dataTable.getValueDouble(i, longitude_idx);
			accession_id = Random.nextInt(400);	//(int)dataTable.getValueInt(i, accession_id_idx);
			*/
			Double valueForSize;
			if (accession_id2valueForSize.containsKey(accession_id)){
				valueForSize = accession_id2valueForSize.get(accession_id);
			}
			else{
				valueForSize = minValueForSize-1;	// this will be translated as missing in MapCanvasOverlay
			}
			Double valueForColor;
			if (accession_id2valueForColor.containsKey(accession_id)){
				valueForColor = accession_id2valueForColor.get(accession_id);
			}
			else{
				valueForColor = minValueForColor -1;	// this will be translated as missing in MapCanvasOverlay
			}
			
			if (displayOption_idx==1){	//skip accessions with any of the two values being NA
				if (valueForColor<minValueForColor || valueForSize<minValueForSize){
					continue;
				}
			}
			else if (displayOption_idx==2){	//skip accessions with both non-NA values
				if (valueForColor>=minValueForColor && valueForSize>=minValueForSize){
					continue;
				}
			}
			
			LatLng point = LatLng.newInstance(latitude, longitude);
			
			Pair<Double, Double> key = Pair.from(point.getLatitude(), point.getLongitude());
			if (!latlng2clusterData.containsKey(key))
			{
				// create a new cluster
				int clusterID = clusterID2Data.size()+1;
				ClusterData clusterData = new ClusterData();
				clusterData.setCluserID(clusterID);
				clusterData.latLng = point;
				clusterID2Data.put(clusterID, clusterData);
				latlng2clusterData.put(key, clusterData);
			}
			
			ClusterData clusterData = latlng2clusterData.get(key);
			clusterData.accession_id_list.add(accession_id);
			clusterData.rowIndexArrayList.add(i);
			
			clusterData.sizeValueList.add(valueForSize);
			
			clusterData.colorValueList.add(valueForColor);
			
			rowIndex2ClusterID.put(i, clusterData.clusterID); 
			
		}
		// TODO sort clusterData.sizeValueList and clusterData.colorValueList
		Iterator it = latlng2clusterData.keySet().iterator();
		while (it.hasNext()){
			ClusterData clusterData = latlng2clusterData.get(it.next());
			Collections.sort(clusterData.colorValueList);	//sort this array list
			Collections.sort(clusterData.sizeValueList);	//sort it
		}
		
		/* 2011-5-16
		// need to recalculate min/maxValue when attribute_id is 0 (same) or -1 (size), which is cluster-based,
		// rather than individual-based.
		 * 
		 * From server, Accession attribute value is 0 for accession_attribute_id=0, 1 for accession_attribute_id=-1.
		 * 	now adjust the colorValueList and sizeValueList to be of singleValue list.
		 * 
		 */
		if (sizeAttributeID==0 || sizeAttributeID==-1){
			double minValueForSize_backup=0;
			double maxValueForSize_backup=0;
			
			int counter = 0;
			
			Iterator it1 = latlng2clusterData.keySet().iterator();
			while (it1.hasNext()){
				counter++;
				ClusterData clusterData = latlng2clusterData.get(it1.next());
				double value;
				if (sizeAttributeID==0){
					value = 0;
				}
				else{
					value = clusterData.accession_id_list.size();
				}
				//double value = Common.getSumOfArrayListWithinMinMax(clusterData.sizeValueList, 0., 1.);
				
				if (counter==1){
					minValueForSize_backup = value;
					maxValueForSize_backup = value;
				}
				else{
					if (value<minValueForSize_backup){
						minValueForSize_backup = value;
					}
					if (value>maxValueForSize_backup){
						maxValueForSize_backup = value;
					}
				}
				clusterData.sizeValueList.clear();
				clusterData.sizeValueList.add(value);
			}
			mapCanvas.minValueForSize = minValueForSize_backup;
			if (maxValueForSize_backup==minValueForSize_backup){
				maxValueForSize_backup = minValueForSize_backup + 1;
			}
			mapCanvas.maxValueForSize = maxValueForSize_backup;
			
		}
		if (colorAttributeID==0 || colorAttributeID==-1){
			
			double minValueForColor_backup=0;
			double maxValueForColor_backup=0;
			
			int counter = 0;
			
			Iterator it1 = latlng2clusterData.keySet().iterator();
			while (it1.hasNext()){
				counter++;
				ClusterData clusterData = latlng2clusterData.get(it1.next());
				double value;
				if (colorAttributeID==0){
					value = 0;
				}
				else{
					value = clusterData.accession_id_list.size();
				}
				//double value = Common.getSumOfArrayListWithinMinMax(clusterData.colorValueList, 0., 1.);
				
				if (counter==1){
					minValueForColor_backup = value;
					maxValueForColor_backup = value;
				}
				else{
					if (value<minValueForColor_backup){
						minValueForColor_backup = value;
					}
					if (value>maxValueForColor_backup){
						maxValueForColor_backup = value;
					}
				}
				clusterData.colorValueList.clear();
				clusterData.colorValueList.add(value);
			}
			
			mapCanvas.minValueForColor = minValueForColor_backup;
			if (maxValueForColor_backup==minValueForColor_backup){
				maxValueForColor_backup = minValueForColor_backup + 1;
			}
			mapCanvas.maxValueForColor = maxValueForColor_backup;
			
		}
		
	}


	public class FillByWhatJSONResponseHandler implements RequestCallback {
		private ListBox listBox;
		public FillByWhatJSONResponseHandler(ListBox listBox){
			this.listBox = listBox;
		}
		public void onError(Request request, Throwable exception) {
			jsonErrorDialog.displayRequestError(exception.toString());
			resetSearchButtonCaption();
		}
		
		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				JSONArray accessionAttributeNameArray = JSONParser.parseStrict(responseText).isArray();
				for (int i = 0; i < accessionAttributeNameArray.size(); i++) {
					JSONArray attributeNameTuple = accessionAttributeNameArray.get(i).isArray();
					String attribute_id = attributeNameTuple.get(0).isNumber().toString();
					String attribute_name = attributeNameTuple.get(1).isString().stringValue();
					listBox.addItem(attribute_name, attribute_id);
				}
			} catch (JSONException e) {
				jsonErrorDialog.displayParseError(responseText);
			}
			resetSearchButtonCaption();
		}
	}

	public void fillByWhatSelectBox(ListBox listBox)
	{
		//setText(DIALOG_WAITING_TEXT);
		String url = URL.encode(passingData.accessionAttributeNameURL);
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new FillByWhatJSONResponseHandler(listBox));
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
	
	/*
	 * 2009
	 * 	a function to wiggle the GPS coordinates if there is already a marker drawn on the map.
	 */
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
	
	/*
	 * 2011-5-14 resize the map according to the window size
	 */
	public void resetMapSize()
	{
		width = Math.max(200, Window.getClientWidth()-50);
		height = Math.max(200, Window.getClientHeight() - 250);
		map.setSize(width+"px", height+"px");
		mapCanvas.reSizeWidget();
		mapCanvas.resetWidgetPosition();
	}
	
	/*
	 * 2011-5-16
	 * 	used in converting accession_id2attribute_value (a json object from server) to java HashMap
	 */
	public static void convertJSONDictIntoHashMap(JSONObject jsonObject, HashMap<Integer, Double> hashMap){
		//clear the hashMap
		hashMap.clear();
		Iterator it = jsonObject.keySet().iterator();
		while (it.hasNext()) {
			String jsonKey = it.next().toString().toString();
			Integer hashMapKey = Integer.parseInt(jsonKey);
			double attributeValue = jsonObject.get(jsonKey).isNumber().doubleValue();
			if (!hashMap.containsKey(hashMapKey)){
				hashMap.put(hashMapKey, attributeValue);
			}
		}
	}
}
