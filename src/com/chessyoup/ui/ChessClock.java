package com.chessyoup.ui;

import java.util.Timer;
import java.util.TimerTask;

import com.chessyoup.model.TimeControl;

public class ChessClock extends TimerTask {
			
	private Timer clockTimer;
	
	private TimeControl timeControl;
	
	private ChessClockListener listener;
	
	public interface ChessClockListener{
		
		public void onStart();
		
		public void onStop();
		
		public void onTimesUp();
			
		public void onTick();
	}
	
	public ChessClock(){
		this.clockTimer = new Timer("ChessClock", false);
		this.timeControl = new TimeControl();
	}
	
	public void startClock(long timeControll, long increment){
		this.timeControl.setTimeControl(timeControll, 0, increment);
		this.clockTimer.scheduleAtFixedRate(this, 1000, 1000);
		this.timeControl.startTimer(System.currentTimeMillis());
				
		if( this.listener != null ){
			this.listener.onStart();
		}
	}
	
	public void stopClock(){
		this.clockTimer.cancel();
		this.timeControl.stopTimer(System.currentTimeMillis());
	}
	
	public void pressClock(){
		this.timeControl.moveMade(System.currentTimeMillis(), true);		
	}
	
	public boolean isRunning(){
		return this.timeControl.clockRunning();
	}
	
	int count = 0;
	
	@Override
	public void run() {
		
		if( this.listener != null ){
			
			System.out.println("white :"+ timeControl.getRemainingTime(true, System.currentTimeMillis()));
			System.out.println("black: "+timeControl.getRemainingTime(false, System.currentTimeMillis()));
			
			this.listener.onTick();
		}
		
		count ++;
		
		if( count % 7 == 0){
			System.out.println("Clock presed");
			pressClock();
		}
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
}
