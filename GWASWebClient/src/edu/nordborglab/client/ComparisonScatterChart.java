package edu.nordborglab.client;

import java.util.Date;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dev.shell.JsValue;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.DataView;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.events.SelectHandler.SelectEvent;
import com.google.gwt.visualization.client.visualizations.ColumnChart;
import com.google.gwt.visualization.client.visualizations.MotionChart;
import com.google.gwt.visualization.client.visualizations.ScatterChart;
import com.google.gwt.visualization.client.visualizations.Table;
import com.google.gwt.visualization.client.Selection;




public class ComparisonScatterChart extends Composite {

	interface Binder extends UiBinder<Widget, ComparisonScatterChart>{ };
	private static final Binder binder = GWT.create(Binder.class);
	private DataTable dataTable = null;
	private DataView viewPlot = null;
	private DataView tablePlot = null;
	private JsArray<GWASData> gwasData = null;
	private JSONNumber x_analysis_method_id;
	

	@UiField ListBox x_call_method_listbox;
	@UiField ListBox x_phenotype_listbox;
	@UiField ListBox x_results_listbox;
	@UiField ScatterChart scatterChart;
	@UiField ListBox y_call_method_listbox;
	@UiField ListBox y_phenotype_listbox;
	@UiField ListBox y_results_listbox;
	@UiField Button submitButton;
	@UiField Table table;
	@UiField ListBox y_results_file;
	@UiField ListBox x_results_file;
	
	ComparisonScatterChart()
	{
		super();
		initWidget(binder.createAndBindUi(this));
		
		JSONObject callMethodList = new JSONObject(this.getCallMethodList());
		JSONArray callMethodOptions = callMethodList.get("options").isArray();
		for (int i = 0;i< callMethodOptions.size();i++)
		{
			JSONObject idValueDict= callMethodOptions.get(i).isObject();
			String item = idValueDict.get("id").isString().stringValue();
			Double value = idValueDict.get("value").isNumber().doubleValue();
			Integer valueInt = value.intValue();
		    this.x_call_method_listbox.addItem(item,valueInt.toString());
		    this.y_call_method_listbox.addItem(item,valueInt.toString());
		    
		}
		JSONObject gwas_info = new JSONObject(this.getTemporaryGWASFiles());
		JSONArray gwas_files = gwas_info.get("files").isArray();
		for (int i = 0;i< gwas_files.size();i++)
		{
			String file= gwas_files.get(i).isString().stringValue();
		    this.x_results_file.addItem(file,file);
		    this.y_results_file.addItem(file,file);
		}
		
		
		// Load the visualization api, passing the onLoadCallback to be called
	    // when loading is done.
	    VisualizationUtils.loadVisualizationApi(new Runnable() {
			
			@Override
			public void run() {
			}
		}, ScatterChart.PACKAGE, Table.PACKAGE,ColumnChart.PACKAGE);
	    
	    scatterChart.addSelectHandler(new SelectHandler(){

			@Override
			public void onSelect(SelectEvent event) {
				JsArray<Selection> selectionLs = scatterChart.getSelections();
				for (int i=0; i< selectionLs.length(); i++ )
				{
					Selection s = selectionLs.get(i);
					table.setSelections(getTableSelection());
					String chr = dataTable.getProperty(s.getRow(), 0, "chr");
					String pos = dataTable.getProperty(s.getRow(),0,"pos");
					double score = dataTable.getValueDouble(s.getRow(),0);
					String SNPBaseURL = "/SNP/?call_method_id="+x_call_method_listbox.getValue(x_call_method_listbox.getSelectedIndex())+"&phenotype_method_id="+x_phenotype_listbox.getValue(x_phenotype_listbox.getSelectedIndex())+"+&analysis_method_id="+x_analysis_method_id.toString();
					final String _SNPURL = URL.encode(SNPBaseURL + "&chromosome="+chr+"&position=" + pos +"&score="+String.valueOf(score));
					Window.open(_SNPURL, "", "");
				}
				
			}
			
		}
		);
	    //JsArray temporary_gwas_result = this.getTemporaryGWASResult();
	    String test = "test";
	}
	
	private final native JsArray<Selection> getScatterSelection()  /*-{
	 	return eval([{'row':415,'column':1}]);
	 	}-*/; 

	private final native JsArray<Selection> getTableSelection()  /*-{
 		return eval([{'row':415}]);
 	}-*/; 
	
	@UiHandler("submitButton")
	void onClickSubmitButton(ClickEvent event)
	{
		loadScatterPlot();
	}
	
	@UiHandler(value={"x_phenotype_listbox","y_phenotype_listbox","y_call_method_listbox","x_call_method_listbox","x_results_listbox","y_results_listbox"})
	void onChangeListBox(ChangeEvent event)
	{
		ListBox source = (ListBox)event.getSource();
		
		if (source == y_phenotype_listbox)
			loadYResultMethodListBox();
		else if (source==x_phenotype_listbox)
			loadXResultMethodListBox();
		else if (source==x_call_method_listbox)
			loadXPhenoTypListBox();
		else if (source==y_call_method_listbox)
			loadYPhenoTypListBox();
		else if (source==y_results_listbox || source==x_results_listbox)
			setSubmitButtonStatus();
	}
	
	private void fillListBox(String responseText,ListBox box)
	{
		box.clear();
		JSONValue jsonValue = JSONParser.parse(responseText);
		JSONObject jsonObject = jsonValue.isObject();
		JSONArray phenotypeMethodLs = jsonObject.get("options").isArray();
		Common.fillSelectBox(phenotypeMethodLs,box);
	}
	
	private void loadXPhenoTypListBox() 
	{
		String url = URL.encode("/DisplayResults/getPhenotypeMethodLsJson?call_method_id="+x_call_method_listbox.getValue(x_call_method_listbox.getSelectedIndex()));
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new RequestCallback()
			{
				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode())
					{
						fillListBox(response.getText(),x_phenotype_listbox);
					}
				}
				@Override
				public void onError(Request request, Throwable exception) {
					
				}
			}
			
			);
		} catch (RequestException ex) {
			
		}
	}
	
	private void loadYPhenoTypListBox() 
	{
		String url = URL.encode("/DisplayResults/getPhenotypeMethodLsJson?call_method_id="+y_call_method_listbox.getValue(y_call_method_listbox.getSelectedIndex()));
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new RequestCallback()
			{
				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode())
					{
						fillListBox(response.getText(),y_phenotype_listbox);
					}
				}
				@Override
				public void onError(Request request, Throwable exception) {
					
				}
			}
			
			);
		} catch (RequestException ex) {
			
		}
	}
	
	private void loadXResultMethodListBox()
	{
		if (x_call_method_listbox.getSelectedIndex() !=-1 && x_phenotype_listbox.getSelectedIndex() !=-1 )
		{
			String call_method_id = x_call_method_listbox.getValue(x_call_method_listbox.getSelectedIndex());
			String phenotype_method_id = x_phenotype_listbox.getValue(x_phenotype_listbox.getSelectedIndex());
			String url = URL.encode("/Comparison/getResultMethodByArgs?call_method_id="+call_method_id+ "&phenotype_method_id=" + phenotype_method_id);
			RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
			try {
				requestBuilder.sendRequest(null, new RequestCallback()
				{
					public void onResponseReceived(Request request, Response response) {
						if (200 == response.getStatusCode())
						{
							fillListBox(response.getText(),x_results_listbox);
						}
					}
					@Override
					public void onError(Request request, Throwable exception) {
						
					}
				}
				
				);
			} catch (RequestException ex) {
			}
		}
	}
	
	private void loadYResultMethodListBox()
	{
		if (x_call_method_listbox.getSelectedIndex() !=-1 && x_phenotype_listbox.getSelectedIndex() !=-1 )
		{
			String call_method_id = y_call_method_listbox.getValue(y_call_method_listbox.getSelectedIndex());
			String phenotype_method_id = y_phenotype_listbox.getValue(y_phenotype_listbox.getSelectedIndex());
			String url = URL.encode("/Comparison/getResultMethodByArgs?call_method_id="+call_method_id+ "&phenotype_method_id=" + phenotype_method_id);
			RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
			try {
				requestBuilder.sendRequest(null, new RequestCallback()
				{
					public void onResponseReceived(Request request, Response response) {
						if (200 == response.getStatusCode())
						{
							fillListBox(response.getText(),y_results_listbox);
						}
					}
					@Override
					public void onError(Request request, Throwable exception) {
					}
				}
				
				);
			} catch (RequestException ex) {
			}
		}
	}
	
	
	private void setSubmitButtonStatus()
	{
		if (x_results_listbox.getSelectedIndex() > 0 && y_results_listbox.getSelectedIndex() > 0 )
		{
			submitButton.setEnabled(true);
		}
		else
			submitButton.setEnabled(false);
	}
	
	private void fillScatterBox(String responseText)
	{
		try
		{
			dataTable = DataTable.create();
			dataTable.addColumn(ColumnType.NUMBER,"chr");
			dataTable.addColumn(ColumnType.NUMBER,"pos");
			dataTable.addColumn(ColumnType.NUMBER,"x_value_t");
			dataTable.addColumn(ColumnType.NUMBER,"y_value_t");
			dataTable.addColumn(ColumnType.NUMBER,"x_value");
			dataTable.addColumn(ColumnType.NUMBER,"y_value");
			JSONValue json_parsed = JSONParser.parse(responseText);
			JSONObject json_obj = json_parsed.isObject();
			JSONNumber overlapString  = json_obj.get("overlap_count").isNumber();
			x_analysis_method_id = json_obj.get("x_analysis_method_id").isNumber();
			 
			JSONArray snps_data = json_obj.get("chr_pos_scores").isArray();
			/*gwasData = getGWASData(snps_data.toString());
			dataTable.addRows(gwasData.length());*/
			int size = snps_data.size();
			dataTable.addRows(size);
			for (int i =0;i< size;i++)
			{
				JSONArray gwas = snps_data.get(i).isArray();
				int chr = (int)gwas.get(0).isNumber().doubleValue();
				int pos = (int)gwas.get(1).isNumber().doubleValue();
				double x_value = gwas.get(2).isNumber().doubleValue();
				double y_value = gwas.get(3).isNumber().doubleValue();
				dataTable.setValue(i, 0, chr);
				dataTable.setValue(i,1,pos);
				dataTable.setValue(i,2,x_value);
				dataTable.setValue(i,3,y_value);
				dataTable.setValue(i,4,x_value);
				dataTable.setValue(i,5,y_value);
				dataTable.setFormattedValue(i, 2, "Chr: " +  String.valueOf(chr) + " / Pos:" + String.valueOf(pos));
				dataTable.setFormattedValue(i, 3, "Score: "+ String.valueOf(x_value) + "/" + String.valueOf(y_value));
				dataTable.setProperty(i, 0, "chr", String.valueOf(chr));
				dataTable.setProperty(i, 0, "pos", String.valueOf(pos));
			}
			viewPlot  = DataView.create(dataTable);
			tablePlot = DataView.create(dataTable);
			int[] columns_plot = new int[] {2,3};
			int[] columns_table = new int[] {0,1,4,5};
			viewPlot.setColumns(columns_plot);
			tablePlot.setColumns(columns_table);
			ScatterChart.Options scatterChartOptions = this.createScatterChartOptions();
			scatterChartOptions.setTitle("Comparision Scatter Chart. Overlap: " + size);
			scatterChart.draw(viewPlot, scatterChartOptions);
			Table.Options tableOptions = this.createTableOptions();
			table.draw(tablePlot,tableOptions);
		}
		catch (Exception e)
		{
			
		}
	}
	
	private ScatterChart.Options createScatterChartOptions() {
		ScatterChart.Options options = ScatterChart.Options.create();
		options.setWidth(600);
		options.setHeight(600);
		options.set("titleX", "-log Pvalue for " + x_results_listbox.getItemText(x_results_listbox.getSelectedIndex()));
		options.set("titleY", "-log Pvalue for " + y_results_listbox.getItemText(y_results_listbox.getSelectedIndex()));
		
		options.setLegend(LegendPosition.NONE);
		return options;
	 }
	 
	private Table.Options createTableOptions() {
		Table.Options options = Table.Options.create();
		options.setPage(Table.Options.Policy.ENABLE);
		options.setPageSize(25);
		return options;
	 }
	 
	
	private void loadScatterPlot()
	{
		if ( (x_results_listbox.getSelectedIndex() !=-1 && y_results_listbox.getSelectedIndex() !=-1) ||
			 (x_results_file.getSelectedIndex() > 0 && y_results_file.getSelectedIndex() > 0))
			
		{
			String x_result_file = null;
			String y_result_file = null;
			if (x_results_file.getSelectedIndex() > 0)
				x_result_file = x_results_file.getValue(x_results_file.getSelectedIndex());
			if (y_results_file.getSelectedIndex() > 0)
				y_result_file = x_results_file.getValue(y_results_file.getSelectedIndex());

			String x_results_method_id = x_results_listbox.getValue(x_results_listbox.getSelectedIndex());
			String y_results_method_id = y_results_listbox.getValue(y_results_listbox.getSelectedIndex());
			String url = URL.encode("/Comparison/getGWASForComparision?x_results_method_id="+x_results_method_id+ "&y_results_method_id=" + y_results_method_id) + (x_result_file != null ? "&x_results_file="+x_result_file : "") + (y_result_file != null ? "&y_results_file="+y_result_file : "");
			
			submitButton.setEnabled(false);
			submitButton.setText("LOADING");
			RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
			
			try {
				requestBuilder.sendRequest(null, new RequestCallback()
				{
					public void onResponseReceived(Request request, Response response) {
						if (200 == response.getStatusCode())
						{
							submitButton.setEnabled(true);
							submitButton.setText("Submit");
							fillScatterBox(response.getText());
						}
					}
					@Override
					public void onError(Request request, Throwable exception) {
						submitButton.setEnabled(true);
						submitButton.setText("Submit");
					}
				}
				
				);
			} catch (RequestException ex) {
			}
		}
	}
	
	
	public native JsArray<JsArray> getTemporaryGWASFiles()/*-{
		return $wnd.gwas_files;
	}-*/;

		
	public native JsArray getCallMethodList()/*-{
		return $wnd.call_method_ls_json;
	}-*/;

	public native JsArray getAnalysisList()/*-{
		return $wnd.analysis_method_ls_json;
	}-*/;
	
	static class GWASData extends JavaScriptObject
	{
			protected GWASData() {}
			
			public final native double getXpValue() /*-{return this.x_value;}-*/;
			public final native double getYpValue() /*-{return this.y_value;}-*/;
			public final native int getChromosome() /*-{return this.chr;}-*/;
			public final native int getPosition() /*-{return this.pos;}-*/;
			public final String getTest()
			{
				return "test";
			}
	}
	
	private final native JsArray<GWASData> getGWASData(String json) /*-{
	 	return eval(json);
	 	}-*/; 
}
