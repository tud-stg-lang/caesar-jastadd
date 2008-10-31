package org.caesarj.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to track a progress of some computation process.
 * 
 * Intended to be sub-classed and extended with specific notifications.
 * 
 * @author Vaidas
 *
 */
public class ProgressTracker {
	
	protected final double PRECISION = 0.01;
	
	protected class Phase {
		public final String id;
		public final String displayText;
		public final double share;
		public final double progressAtStart;
		
		public Phase(String id, String displayText, double share) {
			this.id = id; 
			this.displayText = displayText;
			this.share = share;
			this.progressAtStart = progress;
		}
	}
	
	protected double progressScale;
	protected double progress;
	protected List<Phase> phaseStack = new ArrayList<Phase>();
	protected List<IProgressListener> progressListeners = new ArrayList<IProgressListener>();
	
	/**
	 * Default constructor 
	 */
	public ProgressTracker() {
		progressScale = 1.0;
		progress = 0.0;
	}
	
	/**
	 * Get overall (top level) progress
	 *  
	 * @return progress [0.0, 1.0] 
	 */
	public double overallProgress() {
		return progress;
	}
	
	/**
	 * Get local progress of the current (deepest) phase
	 * 
	 * @return progress [0.0, 1.0]
	 */
	public double phaseProgress() {
		double start = (phaseStack.size() > 0) ? 
				phaseStack.get(phaseStack.size()-1).progressAtStart : 0.0;
		return (progress - start) / progressScale;
	}
	
	/**
	 * Add a new progress listener
	 * 
	 * @param listener - listener to add
	 */
	public void addProgressListener(IProgressListener listener) {
		progressListeners.add(listener);
	}
	
	/**
	 * Remove a progress listener
	 * 
	 * @param listener - listener to remove
	 */
	public void removeProgressListener(IProgressListener listener) {
		progressListeners.remove(listener);
	}
	
	/**
	 * Start a new progress phase. If a previous phase was not ended
	 * a child phase is started.
	 * 
	 * @param phaseID - an arbitrary identifier of the phase 
	 * @param displayText - information text about the phase to be displayed
	 * @param share - the share of the phase (0.0 < share < 1.0) 
	 *                in the progress of the parent phase (or total progress)
	 */	
	public void startPhase(String phaseID, String displayText, double share) {
		progressScale = progressScale * share;
		phaseStack.add(new Phase(phaseID, displayText, share));
		
		for (IProgressListener listener : progressListeners) {
			listener.startPhase(phaseID, displayText);
		}
	}
	
	/**
	 * Advance progress counter.
	 * 
	 * @param displayText - information about completed work to be displayed
	 * @param share - share of the current phase (or total progress) to advance
	 */
	public void advanceProgress(String displayText, double share) {
		progress += share * progressScale;
		if (progress > 1.0 + PRECISION) {
			System.out.println("ProgressControl warning: progress exceeded 100%");
		}
		
		for (IProgressListener listener : progressListeners) {
			listener.advanceProgress(displayText, share, share * progressScale,
					phaseProgress(), overallProgress());
		}
	}
	
	/**
	 * End the current phase or one of the parent phases, depending on phaseID.
	 * 
	 * @param phaseID - the identifier of the ended phase 
	 */	
	public void endPhase(String phaseID) {
		int depth = phaseStack.size() - 1;
		while (depth >= 0 && phaseStack.get(depth).id != phaseID) {
			Phase phase = phaseStack.get(depth);
			progressScale = progressScale / phase.share;			
			phaseStack.remove(depth);
			depth--;
		}
		
		if (depth < 0) {
			System.out.println("ProgressControl warning: inconsistent phaseID reported to progress monitor");
		}
		else {
			Phase phase = phaseStack.get(depth);
			if (phaseProgress() < 1.0 - PRECISION) {
				advanceProgress(phase.displayText, 1.0 - phaseProgress());
			}
			else if (phaseProgress() > 1.0 + PRECISION) {
				System.out.println("ProgressControl warning: a phase exceeded its progress share");
			}
			progressScale = progressScale / phase.share;
			phaseStack.remove(depth);			
		}		
		
		for (IProgressListener listener : progressListeners) {
			listener.endPhase(phaseID);
		}
	}	
}
