package modelling;

import main.MySerializable;
import main.StoreHandler;

/**
 * A Switch TrackSegment
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 26.05.2021
 */
public class Switch implements TrackSegment,MySerializable{
	
	private static final long serialVersionUID = 4407187894697135038L;
	private final Edge leftEdge;
	private final Edge rightEdge;
	private final Edge[] asscociatedEdges;
	private SwitchState state;
	private BlockPoint blockLeft;
	private BlockPoint blockRight;
	
	/**
	 * A switch Track Segment
	 * @param firstNode
	 * @param leftNode - the secondNode of the left branch
	 * @param rightNode - the secondNode of the right branch
	 * @param lengthLeft - the length of the left branch
	 * @param lengthRight - the length of the right branch
	 * @param maxSpeedLeft - the maximal Speed that can be driven on the left branch m/s in reality
	 * @param maxSpeedRight - the maximal Speed that can be driven on the right branch m/s in reality
	 * @throws Exception 
	 */
	public Switch(Node firstNode, Node leftNode, Node rightNode, double lengthLeft, double lengthRight,double maxSpeedLeft, double maxSpeedRight) throws Exception {
		leftEdge = new Edge(lengthLeft, firstNode, leftNode, maxSpeedLeft, this);
		rightEdge = new Edge(lengthRight, firstNode, rightNode, maxSpeedRight, this);
		asscociatedEdges = new Edge[2];
		asscociatedEdges[0] = leftEdge;
		asscociatedEdges[1] = rightEdge;
		
		try {
			blockLeft = new BlockPoint(BlockPointProperties.BLOCK_NEGATIVE_DIRECTION, new Position(leftEdge, lengthLeft));
			blockRight = new BlockPoint(BlockPointProperties.BLOCK_NEGATIVE_DIRECTION, new Position(rightEdge, lengthRight));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		state = SwitchState.LEFT;
		rightEdge.addBlockPoint(blockRight);
		leftEdge.removeBlockPoint(blockLeft);
		
		//TODO Z21 API must know that the switch is left.
		
		firstNode.bind(this);
		leftNode.bind(this);
		rightNode.bind(this);
		StoreHandler.addObject(this);
	}
	
	@Override
	public void delete() throws Exception {
		leftEdge.getFirstNode().rebind(this);
		leftEdge.getSecondNode().rebind(this);
		rightEdge.getSecondNode().rebind(this);
		
		StoreHandler.deleteObject(this);
		
		leftEdge.delete();
		rightEdge.delete();
		blockLeft.delete();
		blockRight.delete();
	}
	
	@Override
	synchronized public Edge getCurrentTrackEdge() {
		switch (state) {
		case LEFT: 
			return leftEdge;
		case RIGHT:
			return rightEdge;
		}
		return rightEdge;
	}
	
	/**
	 * This Method checks weather the state of this Switch can be changed.
	 * @return
	 */
	synchronized boolean switchPossible(){
		switch(state) {
		case LEFT:
			return leftEdge.hasTrains();
		case RIGHT:
			return rightEdge.hasTrains();
		}
		return false;
	}
	
	/**
	 * This Method switches the state o
	 * @throws Exception
	 */
	synchronized public void switchTrack() throws Exception {
		switch (state) {
		case LEFT:
			switchRight();
			break;
		case RIGHT:
			switchLeft();
			break;
		}
	}
	
	/**
	 * This Method switches this Switch to the left branch
	 * @throws Exception
	 */
	synchronized void switchLeft() throws Exception {
		if(!switchPossible()) {
			throw new Exception("Currently it is not possible to switch");
		}
		rightEdge.addBlockPoint(blockRight);
		//TODO Z21 API must know that the switch is left.
		//Wait until Z21 switched
		state = SwitchState.LEFT;
		leftEdge.removeBlockPoint(blockLeft);
	}
	
	/**
	 * This Method switches this Switch to the right branch
	 * @throws Exception
	 */
	synchronized void switchRight() throws Exception {
		if(!switchPossible()) {
			throw new Exception("Currently it is not possible to switch");
		}
		leftEdge.addBlockPoint(blockLeft);
		//TODO Z21 API must know that the switch is left.
		//Wait until Z21 switched
		state = SwitchState.RIGHT;
		rightEdge.removeBlockPoint(blockRight);
	}

	@Override
	public Edge[] getAllAscociatiedEdges() {
		return asscociatedEdges;
	}
}
