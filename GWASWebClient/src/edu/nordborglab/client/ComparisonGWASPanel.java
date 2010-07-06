package edu.nordborglab.client;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.DataView;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.events.ReadyHandler;
import com.google.gwt.visualization.client.events.StateChangeHandler;
import com.google.gwt.visualization.client.visualizations.MotionChart;
import com.google.gwt.visualization.client.visualizations.PieChart;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONException;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.ajaxloader.client.ArrayHelper;





public class ComparisonGWASPanel extends Composite{
	
	interface Binder extends UiBinder<Widget, ComparisonGWASPanel>{ };
	private static final Binder binder = GWT.create(Binder.class);
	
	@UiField ListBox call_method_listbox;
	@UiField ListBox analysis_listbox;
	@UiField FlowPanel testFlowPanel; 
	@UiField MotionChart motionChart;
	//@UiField PieChart pie;
	
	
	private String phenotypeMethodID;
	private String phenotypeMethodShortName;
	private String phenotypeMethodDescription;

	private String callInfoURL;
	private String phenotypeHistImageURL;
	private String callPhenotypeQQImageURL;
	private String phenotypeHistogramDataURL;
	private Integer oldXAsis=0;
	private Integer oldYAsis=0;
	
	ComparisonGWASPanel()
	{
		initWidget(binder.createAndBindUi(this));
		Window.setMargin("0px");
        
		Runnable onLoadCallback = new Runnable() {
		      public void run() {
		    	  	MotionChart.Options options = MotionChart.Options.create();
		    	  	options.setHeight(600);
		    	  	options.setWidth(1000);
		    	  	DataTable data = DataTable.create();
		    	  	data.addColumn(ColumnType.STRING,"SNPs");
		    	  	data.addColumn(ColumnType.DATE,"Datum");
		    	  	data.addColumn(ColumnType.NUMBER,"Chromosome");
		    	  	data.addColumn(ColumnType.NUMBER,"Position");
		    	  	data.addColumn(ColumnType.NUMBER,"Phenotype1");
		    	  	data.addColumn(ColumnType.NUMBER,"Phenotype2");
		    	  	data.addColumn(ColumnType.NUMBER,"Phenotype3");
		    	  	data.addColumn(ColumnType.NUMBER,"Phenotype4");
		    	  	data.addColumn(ColumnType.NUMBER,"Phenotype5");
		    	  	data.addColumn(ColumnType.NUMBER,"Phenotype6");
		    	  	data.addColumn(ColumnType.NUMBER,"Phenotype7");
		    	  	data.addColumn(ColumnType.NUMBER,"Phenotype8");
		    	  	data.addRow();
		    	  	data.setValue(0,0, "SNP1");
		    	  	data.setValue(0,1, new Date());
		    	  	data.setValue(0,2, 1);
		    	  	data.setValue(0,3, 200);
		    	  	data.setValue(0,4, 7);
		    	  	data.setValue(0,5, 123);
		    	  	data.setValue(0,6, 100);
		    	  	data.setValue(0,7, 4);
		    	  	data.setValue(0,8, 123);
		    	  	data.setValue(0,9, 40);
		    	  	data.setValue(0,10, 30);
		    	  	data.setValue(0,11, 120);
		    	  	data.addRow();
		    	  	data.setValue(1,0, "SNP2");
		    	  	data.setValue(1,1, new Date());
		    	  	data.setValue(1,2, 1);
		    	  	data.setValue(1,3, 200);
		    	  	data.setValue(1,4, 14);
		    	  	data.setValue(1,5, 412);
		    	  	data.setValue(1,6, 51);
		    	  	data.setValue(1,7, 53);
		    	  	data.setValue(1,8, 53);
		    	  	data.setValue(1,9, 51);
		    		data.setValue(1,10, 142);
		    	  	data.setValue(1,11, 120);
		    	  	motionChart.addReadyHandler(new ReadyHandler()
		    	  	{
						@Override
						public void onReady(ReadyEvent event) {
							motionChart.addStateChangeHandler(createStateHandler(motionChart));
						}
		    	  	}
		    	  	);
		    	  	motionChart.draw(data, options);
		    	  	motionChart.setVisible(true);
		    		
		      }
		    };

		    // Load the visualization api, passing the onLoadCallback to be called
		    // when loading is done.
		    VisualizationUtils.loadVisualizationApi(onLoadCallback, MotionChart.PACKAGE);
		    
		    
		JSONObject callMethodList = new JSONObject(this.getCallMethodList());
		JSONArray callMethodOptions = callMethodList.get("options").isArray();
		Common.fillSelectBox(callMethodOptions,this.call_method_listbox);
		
		JSONArray analysisList = new JSONArray(this.getAnalysisList());
		for (int i = 0;i< analysisList.size();i++)
		{
			JSONArray analysisItem = analysisList.get(i).isArray();
			Double value = analysisItem.get(0).isNumber().doubleValue();
			Integer valueInt = value.intValue();
		    this.analysis_listbox.addItem(analysisItem.get(1).isString().stringValue(),valueInt.toString());
		    
		}
	}
		
	private StateChangeHandler createStateHandler(final MotionChart motianChart)
	{
		return new StateChangeHandler()
		{
			@Override
			public void onStateChange(StateChangeEvent event) {
				//Window.alert(motionchart.getState());
				Boolean isChanged = false;
				@SuppressWarnings("unused")
				JSONValue value = JSONParser.parse(motionChart.getState());
				String currentXSelection = value.isObject().get("xAxisOption").isString().stringValue();
				Integer currentXAxisindex = 0;
				if (currentXSelection == "_NOTHING")
					currentXAxisindex = oldXAsis;
				else
				{
					currentXAxisindex = Integer.parseInt(currentXSelection);
					if (currentXAxisindex != oldXAsis)
					{
						Window.alert("X-Axe geaendert");
						isChanged = true;
					}
					oldXAsis = currentXAxisindex;
				}
				String currentYSelection = value.isObject().get("yAxisOption").isString().stringValue();
				Integer currentYAxisindex = 0;
				if (currentYSelection == "_NOTHING")
					currentYAxisindex = oldYAsis;
				else
				{
					currentYAxisindex = Integer.parseInt(currentYSelection);
					if (currentYAxisindex != oldYAsis)
					{
						Window.alert("Y-Axe geaendert");
						isChanged = true;
					}
					oldYAsis = currentYAxisindex;
				}
				
				if (isChanged)
				{
					
				}
			}
		};
	}
	
	
	private class MotionChartDataResponseHandler implements RequestCallback {
		public void onError(Request request, Throwable exception) {
			
		}
	
		public void onResponseReceived(Request request, Response response) {
			String responseText = response.getText();
			try {
				/*DataTable callInfoData = Common.asDataTable(responseText);
				DataView motionView = DataView.create(callInfoData);
				//int[] columnIndices = findColIndex(callInfoData); // {2,0,4,3,8,5,6,7};
				motionView.setColumns(columnIndices);
				MotionChart.Options options = MotionChart.Options.create();
				//com.google.gwt.core.client.JavaScriptObject value;
				//options.setOption("state", "{'colorOption':4,'sizeOption':'_UNISIZE'};");
				//String stateStr = new "{'time':'notime','iconType':'BUBBLE','xZoomedDataMin':null,'yZoomedDataMax':null,'xZoomedIn':false,'iconKeySettings':[],'showTrails':true,'xAxisOption':'2','colorOption':'4','yAxisOption':'3','playDuration':15,'xZoomedDataMax':null,'orderedByX':false,'duration':{'multiplier':1,'timeUnit':'none'},'xLambda':1,'orderedByY':false,'sizeOption':'_UNISIZE','yZoomedDataMin':null,'nonSelectedAlpha':0.4,'stateVersion':3,'dimensions':{'iconDimensions':['dim0']},'yLambda':1,'yZoomedIn':false};";
				//2009-4-22 stateStr written same as in javascript won't work because it's encoded into the following crap in client's browser and GWT doesnt' do the encoding for you.
				options.set("state", "%7B%22time%22%3A%22notime%22%2C%22iconType%22%3A%22BUBBLE%22%2C%22xZoomedDataMin%22%3Anull%2C%22yZoomedDataMax%22%3Anull%2C%22xZoomedIn%22%3Afalse%2C%22iconKeySettings%22%3A%5B%5D%2C%22showTrails%22%3Atrue%2C%22xAxisOption%22%3A%222%22%2C%22colorOption%22%3A%224%22%2C%22yAxisOption%22%3A%223%22%2C%22playDuration%22%3A15%2C%22xZoomedDataMax%22%3Anull%2C%22orderedByX%22%3Afalse%2C%22duration%22%3A%7B%22multiplier%22%3A1%2C%22timeUnit%22%3A%22none%22%7D%2C%22xLambda%22%3A1%2C%22orderedByY%22%3Afalse%2C%22sizeOption%22%3A%22_UNISIZE%22%2C%22yZoomedDataMin%22%3Anull%2C%22nonSelectedAlpha%22%3A0.4%2C%22stateVersion%22%3A3%2C%22dimensions%22%3A%7B%22iconDimensions%22%3A%5B%22dim0%22%5D%7D%2C%22yLambda%22%3A1%2C%22yZoomedIn%22%3Afalse%7D%3B");
				options.setHeight(600);
				options.setWidth(1000);
				motionChart.draw(motionView, options);*/
			}
			catch (JSONException e) {
				
			}
		}
	}
	
	
	private void initData(Integer result_id_x, Integer result_id_y)
	{
		String url = URL.encode(phenotypeHistogramDataURL);
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new MotionChartDataResponseHandler());
		} catch (RequestException ex) {
		}
	}
	
	public native JsArray getCallMethodList()/*-{
		return $wnd.call_method_ls_json;
	}-*/;
	
	public native JsArray getAnalysisList()/*-{
		return $wnd.analysis_method_ls_json;
	}-*/;
}
