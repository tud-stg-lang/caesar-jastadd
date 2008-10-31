package org.caesarj.util;

public class VerboseProgress implements IProgressListener {
	
	public void startPhase(String phaseID, String displayText) { 
		System.out.println(displayText + ": 0%");
	}
	
	public void advanceProgress(String displayText, 
			 double changeInPhase, double overallChange,
			 double progressInPhase, double overallProgress) {
		System.out.println(displayText + ": " + (float)(progressInPhase * 100) + "%");
	}
	
	public void endPhase(String phaseID) { }		
}
