package modelling;

import main.Main;
import main.MySerializable;
import main.StoreHandler;

/**
 * A Bumper is a specific TrackSegment.
 * 
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 08.06.2021
 */
public class Bumper implements TrackSegment,MySerializable{
	
	
	private static final long serialVersionUID = -2158111075326030074L;
	private final Edge edge;
	private final Edge[] asscosiatedEdges;
	private final BlockPoint block;
	private final Node helperNode;
	
	/**
	 * A Bumper is a specific TrackSegment.
	 * @param id
	 * @param node - the connecting Node to the next TrackSegment
	 * @param length - the length of the track segment 
	 * @param maxSpeed - the maximal Speed that can be driven on this track m/s in reality
	 * @throws Exception 
	 */
	public Bumper(Node node, double length, double maxSpeed) throws Exception{
		helperNode = new Node();
		edge = new Edge(length, node, helperNode, maxSpeed, this);
		asscosiatedEdges = new Edge[1];
		asscosiatedEdges[0] = edge;
		//set BlockPoint
		BlockPoint blockS = null;
		try {
			blockS = new BlockPoint(BlockPointProperties.BLOCK_ALL, new Position(edge,length - Main.EPSILON));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		block = blockS;
		node.bind(this);
		helperNode.bind(this);
		edge.addBlockPoint(block);
		
		StoreHandler.addObject(this);
	}
	
	@Override
	public void delete() throws Exception {
		StoreHandler.deleteObject(this);
		edge.getFirstNode().rebind(this);
		edge.getSecondNode().rebind(this);
		edge.delete();
		block.delete();
		helperNode.delete();
	}

	@Override
	public Edge getCurrentTrackEdge() {
		return edge;
	}

	@Override
	public Edge[] getAllAscociatiedEdges() {
		return asscosiatedEdges;
	}

}
