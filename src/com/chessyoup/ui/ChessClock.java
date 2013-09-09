package com.chessyoup.ui;

import java.util.Timer;
import java.util.TimerTask;

import com.chessyoup.model.TimeControl;

public class ChessClock extends TimerTask {
			
	private Timer clockTimer;
	
	private TimeControl timeControl;
	
	private ChessClockListener listener;
	
	public interface ChessClockListener{
		
		public void onClockStart();
		
		public void onClockStop();
		
		public void onSwitchClock();
				
	}
	
	public void startClock(){
		
	}
	
	public void stopClock(){
		
	}
	
	public ChessClock(){
		this.clockTimer = new Timer("ChessClock", false);
	}
	
	@Override
	public void run() {
		
		
	}

	public ChessClockListener getListener() {
		return listener;
	}

	public void setListener(ChessClockListener listener) {
		this.listener = listener;
	}

	public Timer getClockTimer() {
		return clockTimer;
	}

	public void setClockTimer(Timer clockTimer) {
		this.clockTimer = clockTimer;
	}

	public TimeControl getTimeControl() {
		return timeControl;
	}

	public void setTimeControl(TimeControl timeControl) {
		this.timeControl = timeControl;
	}				
}
