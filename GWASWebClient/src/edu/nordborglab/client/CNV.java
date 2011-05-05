package edu.nordborglab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTML;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;

public class CNV implements EntryPoint {
	
	//interface Binder extends UiBinder<ViewCNV, CNV> { }
	//private static final Binder binder = GWT.create(Binder.class);
	
	
	public void onModuleLoad() {
		//ViewCNV outer = binder.createAndBindUi(this);		
		ViewCNV outer = new ViewCNV();
		
		//VerticalPanel vPanel = new VerticalPanel();
		//vPanel.add(outer);

		//RootPanel.get("gwt").add(outer);
		
		RootLayoutPanel root = RootLayoutPanel.get();
		//root.add(vPanel);
		root.add(outer);
		
		//RootPanel.get("gwt").add(vPanel);
		//Button b = new Button();
		//b.setText("submit");
		//vPanel.add(b);
		
		//vPanel.add(outer);

	}
	
}
