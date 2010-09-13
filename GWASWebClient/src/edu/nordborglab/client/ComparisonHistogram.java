package edu.nordborglab.client;

import org.mortbay.util.StringUtil;
import org.mortbay.util.ajax.JSON;

import com.google.gwt.ajaxloader.client.ArrayHelper;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
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
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.visualization.client.DataTable;
import com.google.gwt.visualization.client.DataView;
import com.google.gwt.visualization.client.LegendPosition;
import com.google.gwt.visualization.client.Selection;
import com.google.gwt.visualization.client.VisualizationUtils;
import com.google.gwt.visualization.client.AbstractDataTable.ColumnType;
import com.google.gwt.visualization.client.events.SelectHandler;
import com.google.gwt.visualization.client.events.SelectHandler.SelectEvent;
import com.google.gwt.visualization.client.formatters.PatternFormat;
import com.google.gwt.visualization.client.visualizations.ColumnChart;
import com.google.gwt.visualization.client.visualizations.Table;
import com.google.gwt.visualization.client.visualizations.ColumnChart.Options;


public class ComparisonHistogram extends Composite {

	
	
	private static ComparisonHistogramUiBinder uiBinder = GWT
			.create(ComparisonHistogramUiBinder.class);
	
	@UiField ListBox call_method_listbox;
	@UiField ListBox phenotype_listbox;
	@UiField ListBox results_listbox;
	@UiField ColumnChart columnGenesChart;
	@UiField ColumnChart columnSNPsChart;
	@UiField ListBox sel_results_listbox;
	@UiField Button submitButton;
	@UiField Button addButton;
	@UiField Button removeButton;
	@UiField Table tableGenes;
	@UiField Label tableGenesLabel;
	@UiField Table tableSNPs;
	@UiField Label tableSNPsLabel;
	@UiField ListBox chartType;
	@UiField TextBox numberOfGenes;
	@UiField TextBox numberOfSNPs;
	@UiField TabLayoutPanel tabPanel;
	
	private DataTable[] tableGenesDataTable = null;
	private DataTable barChartGenesDataTable = null;
	
	private DataTable[] tableSNPsDataTable = null;
	private DataTable barChartSNPsDataTable = null;
	
	private enum ChartType {GENES,SNPS};
	
	
	interface ComparisonHistogramUiBinder extends
			UiBinder<Widget, ComparisonHistogram> {
	}
	
	public ComparisonHistogram() {
		super();
		initWidget(uiBinder.createAndBindUi(this));

		JSONObject callMethodList = new JSONObject(this.getCallMethodList());
		JSONArray callMethodOptions = callMethodList.get("options").isArray();
		for (int i = 0;i< callMethodOptions.size();i++)
		{
			JSONObject idValueDict= callMethodOptions.get(i).isObject();
			String item = idValueDict.get("id").isString().stringValue();
			Double value = idValueDict.get("value").isNumber().doubleValue();
			Integer valueInt = value.intValue();
		    this.call_method_listbox.addItem(item,valueInt.toString());
		}
		
		chartType.addItem("Both","both");
		chartType.addItem("Genes","genes");
		chartType.addItem("SNPS","snps");
		numberOfGenes.setWidth("4em");
		numberOfSNPs.setWidth("4em");
		// Load the visualization api, passing the onLoadCallback to be called
	    // when loading is done.
	    VisualizationUtils.loadVisualizationApi(new Runnable() {
			
			@Override
			public void run() {
			}
		}, ColumnChart.PACKAGE, Table.PACKAGE);
	    
	    columnGenesChart.addSelectHandler(new BarChartClickedHandler(ChartType.GENES));
	    columnSNPsChart.addSelectHandler(new BarChartClickedHandler(ChartType.SNPS));
	}
	
	public class BarChartClickedHandler extends SelectHandler
	{
		private ChartType type = null;
		
		public BarChartClickedHandler(ChartType type)
		{
			this.type = type;
		}
		@Override
		public void onSelect(SelectEvent event) {
			JsArray<Selection> selectionLs = null;
			ColumnChart chart = null;
			DataTable chartdataTable = null;
			Label label = null;
			Table table = null;
			DataTable [] dataTables = null;
			if (type == ChartType.GENES)
			{
				selectionLs = columnGenesChart.getSelections();
				chartdataTable = barChartGenesDataTable;
				label = tableGenesLabel;
				table = tableGenes;
				dataTables = tableGenesDataTable;
			}
			else
			{
				selectionLs = columnSNPsChart.getSelections();
				chartdataTable = barChartSNPsDataTable;
				label = tableSNPsLabel;
				table = tableSNPs;
				dataTables = tableSNPsDataTable;
			}
			
			for (int i=0; i< selectionLs.length(); i++ )
			{
				Selection s = selectionLs.get(i);
				String count_s = chartdataTable.getValueString(s.getRow(), 0);
				String amount = String.valueOf(chartdataTable.getValueInt(s.getRow(), 1));
				Integer count = Integer.parseInt(count_s);
				Table.Options options = createTableOptions();
				label.setText(amount+ " Genes in "+count_s+" GWAS Results");
				DataView view = DataView.create(dataTables[count-1]);
				if (type == ChartType.SNPS)
					view.setColumns(new int[] {0,1,2});
				table.draw(view,options);
			}
			
		}
	}
	
	@UiHandler(value={"phenotype_listbox","call_method_listbox"})
	void onChangeListBox(ChangeEvent event)
	{
		ListBox source = (ListBox)event.getSource();
		
		if (source==call_method_listbox)
			loadPhenoTypListBox();
		else if (source == phenotype_listbox)
			loadResultMethodListBox();
	}
	
	@UiHandler(value={"addButton"})
	void onAddResult(ClickEvent event)
	{
		for (int i=0;i<results_listbox.getItemCount();i++ )
		{
			if (results_listbox.isItemSelected(i))
			{
				sel_results_listbox.addItem(results_listbox.getItemText(i),results_listbox.getValue(i));
			}
		}
		if (sel_results_listbox.getItemCount() >= 2)
			submitButton.setEnabled(true);
	}
	
	@UiHandler(value={"removeButton"})
	void onRemoveButton(ClickEvent event)
	{
		for (int i=0;i<sel_results_listbox.getItemCount();i++ )
		{
			if (sel_results_listbox.isItemSelected(i))
			{
				sel_results_listbox.removeItem(i);
			}
		}
		if (sel_results_listbox.getItemCount() < 2)
			submitButton.setEnabled(false);
	}
	
	@UiHandler("submitButton")
	void onClickSubmit(ClickEvent event)
	{
		loadCharts();
	}
	

	
	private void loadCharts()
	{
		if (sel_results_listbox.getItemCount() >=2 )
		{
			String selchartType = chartType.getValue(chartType.getSelectedIndex());
			
			int sel_count = sel_results_listbox.getItemCount();
			StringBuffer ids = new StringBuffer();
			for (int i=0;i<sel_count;i++)
			{
				if (ids.length() == 0)
					ids.append(sel_results_listbox.getValue(i));
				else
					ids.append(",").append(sel_results_listbox.getValue(i));
			}
			
			String url = null;
			if (selchartType.equals("snps"))
				url = URL.encode("/Comparison/getSNPOccurencesData?result_method_ids="+ids.toString()+"&count="+numberOfSNPs.getText());
			else if (selchartType.equals("genes"))
				url = URL.encode("/Comparison/getGeneOccurencesData?result_method_ids="+ids.toString()+"&count="+numberOfGenes.getText());
			else
				url = URL.encode("/Comparison/getResultOccurencesData?result_method_ids="+ids.toString()+"&genes_count="+numberOfGenes.getText()+"&snps_count="+numberOfSNPs.getText());
			
			 
			submitButton.setEnabled(false);
			submitButton.setText("LOADING");
			RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
			try
			{
				requestBuilder.sendRequest(null, new LoadChartCallback());
			}
			catch (Exception e)
			{
				
			}
		}
	}
	
	private class LoadChartCallback implements RequestCallback
	{
		public void onResponseReceived(Request request, Response response) {
			if (200 == response.getStatusCode())
			{
				submitButton.setEnabled(true);
				submitButton.setText("Submit");
				fillPlot(response.getText());
			}
		}
		@Override
		public void onError(Request request, Throwable exception) {
			submitButton.setEnabled(true);
			submitButton.setText("Submit");
		}
		
		
		
		
	
	}
	
	private void fillPlot(String responseText)
	{
		try
		{
			JSONValue retval = JSONParser.parse(responseText);
			JSONObject retval_obj = retval.isObject();
			
			JSONObject snps_data = null;
			JSONObject genes_data = null;
			
			if (retval_obj.containsKey("snps_data"))
				snps_data = retval_obj.get("snps_data").isObject();
			if (retval_obj.containsKey("genes_data"))
				genes_data = retval_obj.get("genes_data").isObject();
			
			if (genes_data != null)
				fillGenesPlot(genes_data);
			
			if (snps_data != null)
				fillSNPsPlot(snps_data);
			
			
			if (chartType.getValue(chartType.getSelectedIndex()).equals("snps"))
				tabPanel.selectTab(1);
		}
		catch (Exception e)
		{
			
		}
	}
	
	private void fillSNPsPlot(JSONObject data)
	{
		JSONArray retval_table_data = data.get("table_data").isArray();
		String barChartData = data.get("bar_chart_data").toString();
		PatternFormat format = PatternFormat.create("<a href='http://gbrowse.arabidopsis.org/cgi-bin/gbrowse/arabidopsis/?name=Chr{0}:{1}..{2}' target=_blank>TAIR8</a>");
		barChartSNPsDataTable = Common.doubleJsonify2DataTable(barChartData);
		tableSNPsDataTable = new DataTable[retval_table_data.size()];
		for (int i = 0;i<retval_table_data.size();i++)
		{
			String tableData = retval_table_data.get(i).toString();
			tableSNPsDataTable[i] = Common.doubleJsonify2DataTable(tableData);
			format.format(tableSNPsDataTable[i],ArrayHelper.toJsArrayInteger(new int[] {0,2,3}),2);
		}
		ColumnChart.Options columnChartOptions = this.createColumnChartOptions(ChartType.SNPS);
		columnSNPsChart.draw(barChartSNPsDataTable,columnChartOptions);
		tableSNPsLabel.setText("");
		tableSNPs.draw(DataTable.create());
	}
	
	private void fillGenesPlot(JSONObject data)
	{
		JSONArray retval_table_data = data.get("table_data").isArray();
		String barChartData = data.get("bar_chart_data").toString();
		PatternFormat format = PatternFormat.create("<a href='http://www.arabidopsis.org/servlets/Search?type=general&name={0}&action=detail&method=4&sub_type=gene' target=_blank>{0}</a>");
		barChartGenesDataTable = Common.doubleJsonify2DataTable(barChartData);
		tableGenesDataTable = new DataTable[retval_table_data.size()];
		for (int i = 0;i<retval_table_data.size();i++)
		{
			String tableData = retval_table_data.get(i).toString();
			tableGenesDataTable[i] = Common.doubleJsonify2DataTable(tableData);
			format.format(tableGenesDataTable[i],ArrayHelper.toJsArrayInteger(new int[] {3}),3);
		}
		ColumnChart.Options columnChartOptions = this.createColumnChartOptions(ChartType.GENES);
		columnGenesChart.draw(barChartGenesDataTable,columnChartOptions);
		tableGenesLabel.setText("");
		tableGenes.draw(DataTable.create());
	}
	
	private ColumnChart.Options createColumnChartOptions(ChartType type) {
		ColumnChart.Options options = Options.create();
		options.setWidth(600);
		options.setHeight(600);
		//options.set("hAxis", "{title:'Number of GWAS results'}");
		options.setTitleX("Number of GWAS results");
		if (type == ChartType.GENES)
		{
			options.setTitleY("Numer of Genes");
			options.setTitle("Occurences of Genes");
		}
		else{
			options.setTitleY("Numer of SNPs");
			options.setTitle("Occurences of SNPs");
		}
				//options.set("vAxis.title", "Numer of Genes");
		return options;
	 }
	
	private Table.Options createTableOptions() {
		Table.Options options = Table.Options.create();
		options.setPage(Table.Options.Policy.ENABLE);
		options.setPageSize(25);
		options.setAllowHtml(true);
		return options;
	 }
	 
	
	
	
	private void loadPhenoTypListBox() 
	{
		String url = URL.encode("/DisplayResults/getPhenotypeMethodLsJson?call_method_id="+call_method_listbox.getValue(call_method_listbox.getSelectedIndex()));
		RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
		try {
			requestBuilder.sendRequest(null, new RequestCallback()
			{
				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode())
					{
						fillListBox(response.getText(),phenotype_listbox);
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
	
	private void loadResultMethodListBox()
	{
		if (call_method_listbox.getSelectedIndex() !=-1 && phenotype_listbox.getSelectedIndex() !=-1 )
		{
			String call_method_id = call_method_listbox.getValue(call_method_listbox.getSelectedIndex());
			String phenotype_method_id = phenotype_listbox.getValue(phenotype_listbox.getSelectedIndex());
			String url = URL.encode("/Comparison/getResultMethodByArgs?call_method_id="+call_method_id+ "&phenotype_method_id=" + phenotype_method_id  + "&first_entry=1");
			RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
			try {
				requestBuilder.sendRequest(null, new RequestCallback()
				{
					public void onResponseReceived(Request request, Response response) {
						if (200 == response.getStatusCode())
						{
							fillListBox(response.getText(),results_listbox);
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
	
	private void fillListBox(String responseText,ListBox box)
	{
		box.clear();
		JSONValue jsonValue = JSONParser.parse(responseText);
		JSONObject jsonObject = jsonValue.isObject();
		JSONArray phenotypeMethodLs = jsonObject.get("options").isArray();
		Common.fillSelectBox(phenotypeMethodLs,box);
	}
	
	
	public native JsArray<JavaScriptObject> getCallMethodList()/*-{
		return $wnd.call_method_ls_json;
	}-*/;

}
