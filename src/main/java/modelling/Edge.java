package modelling;

import java.util.HashSet;
import java.util.Iterator;

import main.MySerializable;
import main.StoreHandler;

/**
 * An Edge is an abstraction from which a rail network is built. TrackSegments consist out of 
 * several Edges that models the connections from the startingNode to the endNode.
 * All computations about distances will be performed on Edges, ...not on TrackSegments.
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 26.05.2021
 */
public class Edge implements MySerializable {
	private static final long serialVersionUID = 8814425580564386974L;
	private final double length;
	private final Node firstNode;
	private final Node secondNode;
	private final TrackSegment origin;
	private final double maxSpeed;
	private final HashSet<BlockPoint> blockPoints;
	private final HashSet<TrainScope> trainScopes;
	private double biasPositiveDirection;
	private double biasNegativeDirection;
	
	
	/**
	 * 
	 * An Edge is an abstraction from which a rail network is built. TrackSegments consist out of 
	 * several Edges that models the path through a TrackSegment.
	 * All computations about distances will be performed on Edges, ...not on TrackSegments.
	 * 
	 * @param length - the length of the node
	 * @param firstNode - the node where the Edge starts
	 * @param secondNode - the node where the Edge ends
	 * @param maxSpeed - the maximal Speed that can be driven on this edge in m/s in reality.
	 * @param origin - the TrackSegment that owns this Edge
	 * @throws Exception 
	 */
	Edge(double length, 
			Node firstNode, 
			Node secondNode, 
			double maxSpeed, 
			TrackSegment origin) throws Exception {
		super();
		this.length = length;
		this.firstNode = firstNode;
		this.secondNode = secondNode;
		this.maxSpeed = maxSpeed;
		this.origin = origin;
		this.blockPoints = new HashSet<>();
		this.trainScopes = new HashSet<>();
		
		StoreHandler.addObject(this);
	}
	
	/**
	 * 
	 * @return maxSpeed - the maximal Speed that can be driven on this edge in m/s in reality.
	 */
	double getMaxSpeed() {
		return maxSpeed;
	}

	@Override
	public void delete() throws Exception {
		StoreHandler.deleteObject(this);
	}
	
	/**
	 * 
	 * @return the length of the Edge
	 */
	double getLength() {
		return length;
	}
	
	
	Node getFirstNode() {
		return firstNode;
	}
	Node getSecondNode() {
		return secondNode;
	}
	
	/**
	 * 
	 * @return the TrackSegment that owns this Edge
	 */
	public TrackSegment getOrigin() {
		return origin;
	}
	
	/**
	 * This Method will be called by a BlockPoint during instantiation.
	 * @param blockPoint
	 */
	synchronized void addBlockPoint(BlockPoint blockPoint) {
		assert(blockPoint.getPosition().getEdge() == this);
		blockPoints.add(blockPoint);
	}
	
	/**
	 * This Method will be called by a BlockPoint during distortion.
	 * @param blockPoint
	 */
	synchronized void removeBlockPoint(BlockPoint blockPoint) {
		assert(blockPoint.getPosition().getEdge() == this);
		blockPoints.remove(blockPoint);
	}
	
	HashSet<BlockPoint> getBlockPoints() {
		return blockPoints;
	}

	/**
	 * This method must be called by any train, that enters this edge,
	 * in order to register if a vehicle is on this edge.
	 * @throws InterruptedException
	 */
	synchronized void enter(TrainScope train) {
		trainScopes.add(train);
	}
	/**
	 * This method must be called by any train, that leaves this edge,
	 * in order to register if a Vehicle is on this edge
	 */
	synchronized void leave(TrainScope train) {
		trainScopes.remove(train);
	}
	
	/**
	 * Returns weather there is an train on this edge.
	 * @return
	 */
	synchronized boolean hasTrains() {
		return ! trainScopes.isEmpty();
	}
	
	
	HashSet<TrainScope> getTrainScopes() {
		return trainScopes;
	}

	/**
	 * 
	 * @return the current Edge that is connected to this Edge via the second Node.
	 * The Edges can Change because of switching tracks.
	 */
	Edge getNextEdge() {
		assert(hasNextEdge());
		for(Edge edge : secondNode.getFirstTrackSegment().getAllAscociatiedEdges()) {
			if(edge == this) {
				return secondNode.getSecondTrackSegment().getCurrentTrackEdge();
			}
		}
		return secondNode.getFirstTrackSegment().getCurrentTrackEdge();
	}
	
	/**
	 * 
	 * @return the current Edge that is connected to this Edge via the first Node.
	 * The Edges can Change because of switching tracks.
	 */
	Edge getPreviousEdge() {
		assert(hasPreviousEdge());
		for(Edge edge : firstNode.getFirstTrackSegment().getAllAscociatiedEdges()) {
			if(edge == this) {
				return firstNode.getSecondTrackSegment().getCurrentTrackEdge();
			}
		}
		return firstNode.getFirstTrackSegment().getCurrentTrackEdge();
	}
	
	/**
	 * 
	 * @return true if this Edge has a next Edge.
	 */
	boolean hasNextEdge() {
		if(secondNode.isBound()) {
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @return true if this Edge has previous Edge
	 */
	boolean hasPreviousEdge() {
		if(firstNode.isBound()) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * Get a new EdgeIterator and state weather the
	 * Iterator should start in positive or negative Direction.
	 * The cool thing about the Iterator is, that it always iterates forward no 
	 * matter of the direction of the Edges. 
	 * 
	 * If a next Edge could not be found, hasNext() == false. This case can occur if the
	 * current Edge belongs to a bumper or the nextEdge is from a Switch that is
	 * currently switched to another Edge than the currentEdge.
	 * 
	 * Note that the state of TrackSegments (e.g. Switch) can influence the behavior of the EdgeIterator.
	 * 
	 * @param startDirection - the direction in which the Iterator should start.
	 */
	public EdgeIterator iterator(boolean startDirection) {
		return new EdgeIterator(this, startDirection);
	}
	

	
	
	/**
	 * An EdgeIteratorObject is the return type of the EdgeIterator.
	 * @author David Lieb
	 * @author david.lieb.00@gmail.com
	 * @version 20.05.2021
	 */
	public class EdgeIteratorObject {
		private final Edge edge;
		private final boolean direction;
		private final double length;
		
		private EdgeIteratorObject(Edge edge, boolean direction, double length) {
			this.edge = edge;
			this.direction = direction;
			this.length = length;
		}

		
		/**
		 * get the direction of the current Edge 
		 * @return direction
		 */
		public boolean isDirection() {
			return direction;
		}

		/**
		 * get the current Edge the Iterator has
		 * @return currentEdge
		 */
		public Edge getEdge() {
			return edge;
		}
		
		/**
		 * Get the length added up on all previous edges
		 * @return lenght
		 */
		public double getAccumulatedLength() {
			return this.length;
		}
		
	}
	
	public class EdgeIterator implements Iterator<EdgeIteratorObject>{
		
		private boolean currentDirection;
		private Edge currentEdge;
		private double totalLength;
		
		/**
		 * Create a new Edge Iterator and state weather the
		 * Iterator should start in positive or negative Direction.
		 * The cool thing about the Iterator is, that it always iterates forward no 
		 * matter of the direction of the Edges. 
		 * 
		 * If a next Edge could not be found, hasNext() == false. This case can occur if the
		 * current Edge belongs to a bumper or the nextEdge is from a Switch that is
		 * currently switched to another Edge than the currentEdge.
		 * 
		 * Note that the state of TrackSegments (e.g. Switch) can influence the behavior of the EdgeIterator.
		 * 
		 * @param startEdge - the Edge on which to be started.
		 * @param startDirection - the direction in which the Iterator should start.
		 */
		private EdgeIterator (final Edge startEdge, final boolean startDirection) {
			this.currentDirection = startDirection;
			currentEdge = startEdge;
			totalLength = startEdge.getLength();
			
		}
		
		/**
		 * Create a new Edge Iterator and state weather the
		 * Iterator should start in positive or negative Direction.
		 * The cool thing about the Iterator is, that it always iterates forward no 
		 * matter of the direction of the Edges. 
		 * 
		 * If a next Edge could not be found, hasNext() == false. This case can occur if the
		 * current Edge belongs to a bumper or the nextEdge is from a Switch that is
		 * currently switched to another Edge than the currentEdge.
		 * 
		 * Note that the state of TrackSegments (e.g. Switch) can influence the behavior of the EdgeIterator.
		 * 
		 * @param startPoint - the Position form which the Iterator should start.
		 * @param startDirection - the direction in which the Iterator should start.
		 */
		EdgeIterator (final Position startPoint, final boolean startDirection) {
			this.currentDirection = startDirection;
			currentEdge = startPoint.getEdge();
			if(startDirection) {
				totalLength = startPoint.getEdge().getLength() - startPoint.getOffset();
			}else {
				totalLength = startPoint.getOffset();
			}
			
		}
		
		
		@Override
		public boolean hasNext() {
			if(currentEdge == null) {
				return false;
			}
			return true;
		}
		
		private boolean setNewDirection(final Edge currentEdge, final Edge nextEdge) {
			boolean newDirection = false;
			if(nextEdge.hasNextEdge()) {
				if(nextEdge.getNextEdge() == currentEdge) {
					newDirection = false;
				}
			}if(nextEdge.hasPreviousEdge()) {
				if(nextEdge.getPreviousEdge() == currentEdge) {
					newDirection = true;
				}
			}
			return newDirection;
		}
		
		private boolean checkIfNextEdgeIsReallyTheNextEdge(final Edge currentEdge, final Edge nextEdge) {
			if(nextEdge == null) {
				return false;
			}
			if (nextEdge.hasNextEdge()) {
				if (nextEdge.getNextEdge() == currentEdge) {
					return true;
				}
			}
			if(nextEdge.hasPreviousEdge()) {
				if(nextEdge.getPreviousEdge() == currentEdge) {
					return true;
				}
			}
			return false;
		}

		@Override
		public EdgeIteratorObject next() {
			if(currentEdge == null) {
				return null;
			}
			Edge returnEdge = currentEdge;
			boolean returnDirection = currentDirection;
			double returnLength = totalLength;
			
			Edge nextEdge = null;
			if(currentDirection) {
				if(currentEdge.hasNextEdge()) {
					nextEdge = currentEdge.getNextEdge();
				}
			}else {
				if(currentEdge.hasPreviousEdge()) {
					nextEdge = currentEdge.getPreviousEdge();
				}
			}
			
			if(checkIfNextEdgeIsReallyTheNextEdge(currentEdge, nextEdge)) {
				currentDirection = setNewDirection(currentEdge, nextEdge);
				currentEdge = nextEdge;
				totalLength += nextEdge.getLength();
			}else {
				//In this case there will be no next Edge
				currentEdge = null;
			}
			return new EdgeIteratorObject(returnEdge, returnDirection, returnLength);
		}
		
	}
	
	
}
