package modelling;

import main.MySerializable;
import main.StoreHandler;

/**
 * A Node connects two TrackSegments
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 18.05.2021
 */
public class Node implements MySerializable{
	

	private static final long serialVersionUID = -778610684749988450L;
	private TrackSegment firstTrackSegment = null;
	private TrackSegment secondTrackSegment = null;
	
	public Node() throws Exception{
		StoreHandler.addObject(this);
	}
	
	@Override
	public void delete() throws Exception{
		assert(firstTrackSegment == null);
		assert(secondTrackSegment == null);
		StoreHandler.deleteObject(this);
	}
	
	TrackSegment getFirstTrackSegment() {
		return firstTrackSegment;
	}
	TrackSegment getSecondTrackSegment() {
		return secondTrackSegment;
	}
	
	boolean isBound() {
		if(firstTrackSegment != null && secondTrackSegment != null) {
			return true;
		}
		return false;
	}
	
	void bind(TrackSegment segment) throws Exception{
		if(firstTrackSegment  == null) {
			firstTrackSegment = segment;
		}else if(secondTrackSegment == null) {
			secondTrackSegment = segment;
		}else {
			throw new Exception(this.toString() + " is already bound");
		}
	}
	
	void rebind(TrackSegment segment){
		if(firstTrackSegment == segment) {
			firstTrackSegment = null;
		}else if (secondTrackSegment == segment){
			secondTrackSegment = null;
		}else {
			assert(false);
		}
	}
}
