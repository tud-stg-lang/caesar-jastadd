package org.caesarj.util;


public interface IProgressListener {
	
	/**
	 * Notify about phase start
	 * 
	 * @param phaseID - an arbitrary identifier of the phase 
	 * @param displayText - information text about the phase to be displayed
	 */	
	public void startPhase(String phaseID, String displayText);
	
	/**
	 * Notify about progress counter advancement.
	 * 
	 * @param displayText - information about completed work to be displayed
	 * @param changeInPhase - change of the current phase progress [0.0, 1.0] 
	 * @param overallChange - change of the overall progress [0.0, 1.0]
	 * @param progressInPhase - progress in the current phase [0.0, 1.0]
	 * @param overallProgress - progress in the overall process [0.0, 1.0]
	 */
	
	 public void advanceProgress(String displayText, 
			 double changeInPhase, double overallChange,
			 double progressInPhase, double overallProgress); 
	
	/**
	 * Notify about phase end
	 * 
	 * @param phaseID - the identifier of the ended phase 
	 */	
	public void endPhase(String phaseID); 

}
