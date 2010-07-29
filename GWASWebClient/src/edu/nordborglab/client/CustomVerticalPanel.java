/*
 * 2010-3-11
 * 	a class holding custom vertical panel used a lot in the GWAS web interface.
 * 	By 'custom', this vertical panel includes a helpButton, the click upon which would induce popup of a helpDialog. 
 * 
 */
package edu.nordborglab.client;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;

public class CustomVerticalPanel extends VerticalPanel{
	private AccessionConstants constants;
	private DisplayJSONObject jsonErrorDialog;
	
	
	private HelpDialog helpDialog;
	private String helpID;
	
	private FlexTable layout;
	private Button helpButton;
	
	CustomVerticalPanel(AccessionConstants constants, DisplayJSONObject jsonErrorDialog, String helpID)
	{
		this.constants = constants;
		this.jsonErrorDialog = jsonErrorDialog;
		this.helpID = helpID;
		
		
		helpDialog = new HelpDialog(constants, jsonErrorDialog, helpID);
		helpDialog.hide();
		
		helpButton = new Button("help");
		helpButton.addClickHandler(new ClickHandler(){
			public void onClick(ClickEvent event)
			{
				helpDialog.center();
				helpDialog.show();
			}
		});
		
		layout = new FlexTable();
		layout.setWidget(0, 0, helpButton);
		this.add(layout);
		//this.setCellHorizontalAlignment(layout, HasHorizontalAlignment.ALIGN_LEFT);
	}
}
