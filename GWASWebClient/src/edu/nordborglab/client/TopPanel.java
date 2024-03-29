package edu.nordborglab.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;


public class TopPanel extends Composite {

	private static TopPanelUiBinder uiBinder = GWT
			.create(TopPanelUiBinder.class);

	interface TopPanelUiBinder extends UiBinder<Widget, TopPanel> {
	}
	
	@UiField Anchor actionLink;
	@UiField SpanElement userSpan;
	@UiField SpanElement actionSpan;

	public TopPanel() {
		initWidget(uiBinder.createAndBindUi(this));
	}

}
