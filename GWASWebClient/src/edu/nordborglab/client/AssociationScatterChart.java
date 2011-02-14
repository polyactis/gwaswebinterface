/**
 * a customized scatterchart used to display association mapping results.

 *   add a select event handler which leads to the opening of a new window showing SNP information
 *   add mouseover/mouseout handler to display popup
 *   sink the MOUSEEVENTS in order to pass X,Y to visualization's mouseover/mouseout event handler
 */
/*package edu.nordborglab.client;

import org.danvk.dygraphs.client.Dygraphs;
import org.danvk.dygraphs.client.events.DataPoint;
import org.danvk.dygraphs.client.events.SelectHandler;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.visualization.client.AbstractDataTable;


public class AssociationScatterChart extends Dygraphs  {
	private String chromosome;
	private String color;
	private int chrLength;
	private int maxChrLength;
	private double maxY;
	private String SNPBaseURL;
	
	private int pointSize =2;
	
	private AbstractDataTable dataTable;
	
	
	AssociationScatterChart(String chromosome,
			String color, int chrLength, int maxChrLength, double maxY, String SNPBaseURL)
	{
		super();
		this.chromosome = chromosome;
		this.color = color;
		this.chrLength = chrLength;
		this.maxChrLength = maxChrLength;
		this.maxY = maxY;
		this.SNPBaseURL = SNPBaseURL;
		
	}

	public void initHandler()
	{
		addSelectHandler(new ScatterSelectionHandler());
	}
	
	public void draw_gwas(AbstractDataTable data){
		this.dataTable = data;
		Dygraphs.Options options = Dygraphs.Options.create();
		options = setOptions(options);
		this.draw(this.dataTable, options);
	}
	public Options setOptions(Dygraphs.Options options){
		options.setStrokeWidth(0.000000001);
		options.setDrawPoints(true);
		options.setPointSize(2);
		options.setIncludeZero(true);
		options.setWidth(1000);
		options.setHeight(200);
		options.setAxisLabelFontSize(12);
		options.setValueRange(0,(int)maxY + 2);
		options.setxAxisLabelWidth(100);
		options.setyAxisLabelWidth(20);
		options.setColors(new String[] {color});
		options.setMinimumDistanceForHighlight(10);
		options.setIncludeYPositionForHightlight(true);
		return options;
	}
	
	public void draw_gwas(AbstractDataTable data, Dygraphs.Options options){
		this.dataTable = data;
		options = setOptions(options);
		this.draw(data, options);
	}
	
	class ScatterSelectionHandler extends SelectHandler {
		
		@Override
		public void onSelect(SelectEvent event) {
			//open a new window pointing to the SNP page
			DataPoint point = event.point;
			int position = (int)point.getXVal();
			double score = point.getYVal();
			final String _SNPURL = URL.encode(SNPBaseURL + "&chromosome="+chromosome+"&position=" + position +"&score="+score);
			Window.open(_SNPURL, "", "");
		}
	}
}
*/