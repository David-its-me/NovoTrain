package modelling;

import java.io.Serializable;

/**
 * A TrackSegment is a piece of Track which an be implemented by a Switch, Bumper, ...
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 18.05.2021
 */
public interface TrackSegment extends Serializable{
	
	/**
	 * 
	 * @return an Edge dependent in the current state of the TrackSegment. 
	 * For example a two-way Switch can have two states. 
	 */
	Edge getCurrentTrackEdge();
	
	/**
	 * 
	 * @return all associated Edges a TrackSegment can have.
	 */
	Edge[] getAllAscociatiedEdges();

}
