package modelling;

import main.Main;
import main.MySerializable;
import main.StoreHandler;
import modelling.Edge.EdgeIterator;
import modelling.Edge.EdgeIteratorObject;

/**
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 26.04.2021
 */
public class Position implements MySerializable{
	
	
	private static final long serialVersionUID = 2428198363863639357L;
	private Edge edge;
	private double offset;
	
	
	/**
	 * @param edge
	 * @param offset - the position on the Edge counting upwards from the firstNode of the Edge.
	 */
	public Position(Edge edge, double offset) {
		super();
		this.edge = edge;
		this.offset = offset;
		this.calculatePositionOverflow();
		StoreHandler.addObject(this);
	}
	
	/**
	 * Create a new Position with a deep copy of another Position.
	 * Additionally one can decide weather the new instance should be added to the 
	 * storeHandler or not
	 * @param other Position
	 * @param store - decide if the Position should be added to the storeHandler
	 */
	Position(Position other, boolean store) {
		super();
		this.edge = other.edge;
		this.offset = other.offset;
		this.calculatePositionOverflow();
		if(store) {
			StoreHandler.addObject(this);
		}
		
	}
	
	/**
	 * Create a new Position
	 * Additionally one can decide weather the new instance should be added to the 
	 * storeHandler or not
	 * @param edge
	 * @param offset - the position on the Edge counting upwards from the firstNode of the Edge.
	 * @param store - decide if the Position should be added to the storeHandler
	 */
	Position(Edge edge, double offset, boolean store) {
		super();
		this.edge = edge;
		this.offset = offset;
		this.calculatePositionOverflow();
		if(store) {
			StoreHandler.addObject(this);
		}
		
	}
	
	@Override
	public void delete() {
		StoreHandler.deleteObject(this);
	}
	
	private boolean checkPositionValid() {
		if(0 <= offset && offset < edge.getLength()) {
			return true;
		}
		return false;
	}
	
	Edge getEdge() {
		return edge;
	}
	void setEdge(Edge edge) {
		this.edge = edge;
	}
	double getOffset() {
		return offset;
	}
	void setOffset(double offset) {
		this.offset = offset;
		this.calculatePositionOverflow();
	}
	
	/**
	 * Set the Position with a deep copy of another Position.
	 * @param other Position
	 */
	void setPosition(Position other) {
		assert(other.checkPositionValid());
		this.edge = other.edge;
		this.offset = other.offset;
		assert(this.checkPositionValid());
	}
	
	synchronized private void calculatePositionOverflow(){
		if (!checkPositionValid()) {
			if(offset < 0) {
				if(edge.hasPreviousEdge()) {
					Edge newEdge = edge.getPreviousEdge();
					
					if(newEdge.getFirstNode() == edge.getFirstNode()) {
						offset = - offset;
						edge = newEdge;
						this.calculatePositionOverflow();
						
					}else if (newEdge.getSecondNode() == edge.getFirstNode()) {
						offset = newEdge.getLength() + offset - Main.EPSILON;
						edge = newEdge;
						this.calculatePositionOverflow();
						
					}else {
						//Dieser Fall kann auftreten z.B bei einer Weiche. Diese Edge zeigt auf eine
						//nächstes Segment, aber das Segment zeigt nicht zurück.
						offset = 0;
					}
				}else {//has NO previous Edge
					//Dieser Fall tritt auf, wenn ein Node keinen nächstes Element besitzt.
					offset = 0;
				}
				
				
			}else if (offset >= edge.getLength()) {
				if(edge.hasNextEdge()) {
					Edge newEdge = edge.getNextEdge();
					
					if (newEdge.getFirstNode() == edge.getSecondNode()) {
						offset = offset - edge.getLength();
						edge = newEdge;
						this.calculatePositionOverflow();
						
					}else if (newEdge.getSecondNode() == edge.getSecondNode()) {
						offset = newEdge.getLength() - offset + edge.getLength() - Main.EPSILON;
						edge = newEdge;
						this.calculatePositionOverflow();
						
					}else {
						//Dieser Fall kann auftreten z.B bei einer Weiche. Diese Edge zeigt auf eine
						//nächstes Segment, aber das Segment zeigt nicht zurück.
						offset = edge.getLength() - Main.EPSILON;
					}
				}else {//has NO next Edge
					//Dieser Fall tritt auf, wenn ein Node keinen nächstes Element besitzt.
					offset = edge.getLength() - Main.EPSILON;
				}				
			}else {
				assert(false);
			}
		}
		assert(checkPositionValid());
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
	 * @param startEdge - the Edge on which to be started.
	 * @param startDirection - the direction in which the Iterator should start.
	 */
	EdgeIterator iterator(boolean direction) {
		return this.getEdge().new EdgeIterator(this, direction);
	}
	
	/**
	 * This Method calculates the shortest Distance to the otherPosition, by going from this Position 
	 * into the specified direction. 
	 * 
	 * Additionally you have to specify a border in which the otherPosition should be found. You should
	 * consider the border as low as possible, so that the Algorithm does not run without a time limit.
	 * If the otherPostion was not found this Method will return Double.MAX_VALUE.
	 * @param otherPosition
	 * @param border
	 * @param direction - the direction to start.
	 * @return the distance if it is within the border. Else it returns Double.MAX_VALUE;
	 */
	double calculateDistanceTo(Position otherPosition, double border, boolean direction) {
		assert(this.checkPositionValid());
		assert(otherPosition.checkPositionValid());
		
		
		boolean otherIsBehindThisButOnSameEdge = false;
		if(this.getEdge() == otherPosition.getEdge()) {
			if(direction && this.getOffset() <= otherPosition.getOffset()) {
				return Math.abs(this.getOffset() - otherPosition.getOffset());
			}else if(!direction && otherPosition.getOffset() <= this.getOffset()) {
				return Math.abs(this.getOffset() - otherPosition.getOffset());
			}else {
				otherIsBehindThisButOnSameEdge = true;
			}
		}
		
		EdgeIterator iterator = this.iterator(direction);
		EdgeIteratorObject current = iterator.next();
		assert(current.getEdge() == this.getEdge());
		double returnValue = Double.MAX_VALUE;
		
		while(current.getAccumulatedLength() <= border + otherPosition.getEdge().getLength() + Main.EPSILON) {
			
			if(current.getEdge() == otherPosition.getEdge() && !otherIsBehindThisButOnSameEdge) {
				double length = current.getAccumulatedLength();
			
				assert(length >= 0);
				if(current.isDirection()) {
					length += -otherPosition.getEdge().getLength() + otherPosition.getOffset();
				}else {
					length -= otherPosition.getOffset();
				}
				assert(length >= 0);
				returnValue = length;
				break;
			}
			
			if(iterator.hasNext()) {
				current = iterator.next();
			}else {
				returnValue = Double.MAX_VALUE;
				break;
			}
			otherIsBehindThisButOnSameEdge = false;
		}
		if(returnValue > border + Main.EPSILON) {
			returnValue = Double.MAX_VALUE;
		}
		return returnValue;
	}
	
	/**
	 * This Method calculates the shortest Distance to the otherPosition.
	 * Additionally you have to specify a border in which the otherPosition should be found. You should
	 * consider the border as low as possible, so that the Algorithm does not run without a time limit.
	 * If the otherPostion was not found this Method will return Double.MAX_VALUE.
	 * @param otherPosition
	 * @param border
	 * @return the distance if it is within the border. Else it returns Double.MAX_VALUE;
	 */
	public double calculateDistanceTo(Position otherPosition, double border) {
		assert(this.checkPositionValid());
		assert(otherPosition.checkPositionValid());
		
		if(this.getEdge() == otherPosition.getEdge()) {
			return Math.abs(this.getOffset() - otherPosition.getOffset());
		}
		
		double distance = Math.min(
				calculateDistanceTo(otherPosition, border, false),
				calculateDistanceTo(otherPosition, border, true));
		
		
		return distance;
	}
	
	/**
	 * Check weather to Positions are at the same point
	 * @param other
	 * @return true - if the Positions are equals
	 */
	public boolean equals(Position other) {
		assert(this.checkPositionValid());
		assert(other.checkPositionValid());
		if(this.edge == other.edge) {
			double difference = Math.abs(this.offset - other.offset);
			if(difference <= Main.EPSILON) {
				return true;
			}
		}
		return false;
	}
}
