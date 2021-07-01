package modelling;


import main.MySerializable;
import main.StoreHandler;

/**
 * 
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 26.05.2021
 */
public class Track implements TrackSegment, MySerializable{
	
	
	private static final long serialVersionUID = 2600162178536721956L;
	private final Edge edge;
	private final Edge[] asscociatedEdges;
	
	/**
	 * A Track
	 * @param firstNode
	 * @param secondNode
	 * @param length - the length of the track segment
	 * @param maxSpeed - the maximal Speed that can be driven on this track m/s in reality
	 * @throws Exception
	 */
	public Track(Node firstNode, Node secondNode, double length, double maxSpeed) throws Exception {
		edge = new Edge(length, firstNode, secondNode, maxSpeed, this);
		asscociatedEdges = new Edge[1];
		asscociatedEdges[0] = edge;
		
		firstNode.bind(this);
		secondNode.bind(this);
		
		StoreHandler.addObject(this);
	}

	@Override
	public void delete() throws Exception {
		StoreHandler.deleteObject(this);
		edge.getFirstNode().rebind(this);
		edge.getSecondNode().rebind(this);
		edge.delete();
	}
	
	@Override
	public Edge getCurrentTrackEdge() {
		return edge;
	}

	@Override
	public Edge[] getAllAscociatiedEdges() {
		return asscociatedEdges;
	}

}
