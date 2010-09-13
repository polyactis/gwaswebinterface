package edu.nordborglab.client.visualizations;

import com.google.gwt.ajaxloader.client.Properties;
import com.google.gwt.ajaxloader.client.Properties.TypeException;
import com.google.gwt.visualization.client.events.Handler;

public abstract class OnMouseMoveHandler extends Handler {

	
	public class OnMouseMoveEvent 
	{
		private int row;
		private int col;
		
		public OnMouseMoveEvent(int row,int col)
		{
			this.row = row;
			this.col = col;
		}
		public int getRow()
		{
			return this.row;
		}
		public int getCol()
		{
			return this.col;
		}
	}
	
	public abstract void onMouseMove(OnMouseMoveEvent event);
	
	@Override
	protected void onEvent(Properties properties)  {
		double row = -1;
		double col = -1;
		try
		{
			row = properties.getNumber("row");
			col = properties.getNumber("col");
		}
		catch (Exception ex)
		{
			
		}
		onMouseMove(new OnMouseMoveEvent((int)row,(int)col));
	}

}
