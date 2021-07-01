package modelling;

import java.io.Serializable;

import main.Main;
import modelling.Edge.EdgeIterator;
import modelling.Edge.EdgeIteratorObject;

/**
 * This class represents a Vehicle, that drives on the track.
 * 
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 10.05.2021
 */
public abstract class Vehicle implements Serializable {

	private static final long serialVersionUID = 3904414231848213575L;
	private final String name;
	private final double lengthInMeter;
	private final double maxSpeedInRealMeterPerSecond;
	private TrainScope trainScope;
	private Vehicle previousVehicle;
	private Vehicle nextVehicle;
	private Position middlePosition;
	private boolean countDirection;

	Vehicle(String name, double lenghtInMeter, double maxSpeedInRealMeterPerSecond, Position initialMiddlePosition)
			throws Exception {
		this.name = name;
		this.lengthInMeter = lenghtInMeter;
		this.maxSpeedInRealMeterPerSecond = maxSpeedInRealMeterPerSecond;
		this.middlePosition = initialMiddlePosition;
		this.setCountDirection(false);
		this.previousVehicle = null;
		this.nextVehicle = null;
		this.trainScope = new TrainScope(this);
	}

	double getLengthInMeter() {
		return lengthInMeter;
	}

	Position getMiddlePosition() {
		return middlePosition;
	}

	public TrainScope getTrainScope() {
		return trainScope;
	}

	void setTrainScope(TrainScope trainScope) {
		synchronized (trainScope) {
			this.trainScope = trainScope;
		}
	}

	Vehicle getPreviousVehicle() {
		return previousVehicle;
	}

	void setPreviousVehicle(Vehicle previousVehicle) {

		this.previousVehicle = previousVehicle;

	}

	Vehicle getNextVehicle() {
		return nextVehicle;
	}

	void setNextVehicle(Vehicle nextVehicle) {
		this.nextVehicle = nextVehicle;
	}

	public String getName() {
		return name;
	}

	public double getMaxSpeedInRealMeterPerSecond() {
		return maxSpeedInRealMeterPerSecond;
	}

	/**
	 * This Method updates the MiddlePosition of a Vehicle and all coupled Members
	 * 
	 * @param newPosition
	 */
	void updateMiddlePosition(Position newPosition) {
		synchronized (trainScope) {

			double differenceIntoCountDirection = this.middlePosition.calculateDistanceTo(newPosition,
					Main.TOLERANCE_DISTANCE * 10, countDirection);
			double differenceAgainstCountDirection = this.middlePosition.calculateDistanceTo(newPosition,
					Main.TOLERANCE_DISTANCE * 10, !countDirection);

			BlockPoint newFront = null;
			Position newFrontPosition = null;
			EdgeIterator iterator = null;
			EdgeIteratorObject current = null;

			if (differenceIntoCountDirection < differenceAgainstCountDirection) {
				// ...then organize TrainScope into the countDirection
				switch (this.trainScope.getFront().getProperties()) {
				case BLOCK_ALL:
					// Diser Fall darf nicht auftreten
					assert (false);
					break;
				case BLOCK_NEGATIVE_DIRECTION:
					newFrontPosition = new Position(this.trainScope.getFront().getPosition().getEdge(),
							this.trainScope.getFront().getPosition().getOffset() + differenceIntoCountDirection);
					iterator = this.trainScope.getFront().getPosition().iterator(true);
					break;
				case BLOCK_POSITIVE_DIRECTION:
					newFrontPosition = new Position(this.trainScope.getFront().getPosition().getEdge(),
							this.trainScope.getFront().getPosition().getOffset() - differenceIntoCountDirection);
					iterator = this.trainScope.getFront().getPosition().iterator(false);
					break;
				}
				
				//Iterate over the Edges in order to get the BlockDirection
				do {
					current = iterator.next();
				}while(current.getEdge() != newFrontPosition.getEdge());
				
				if(current.isDirection()) {
					newFront = new BlockPoint(BlockPointProperties.BLOCK_NEGATIVE_DIRECTION, newFrontPosition);
				}else {
					newFront = new BlockPoint(BlockPointProperties.BLOCK_POSITIVE_DIRECTION, newFrontPosition);
				}
				
			} else if (differenceIntoCountDirection > differenceAgainstCountDirection) {
				// ...then organize TrainScope against the countDirection
				switch (this.trainScope.getFront().getProperties()) {
				case BLOCK_ALL:
					// Diser Fall darf nicht auftreten
					assert (false);
					break;
				case BLOCK_NEGATIVE_DIRECTION:
					newFrontPosition = new Position(this.trainScope.getFront().getPosition().getEdge(),
							this.trainScope.getFront().getPosition().getOffset() - differenceAgainstCountDirection);
					iterator = this.trainScope.getFront().getPosition().iterator(false);
					break;
				case BLOCK_POSITIVE_DIRECTION:
					newFrontPosition = new Position(this.trainScope.getFront().getPosition().getEdge(),
							this.trainScope.getFront().getPosition().getOffset() + differenceAgainstCountDirection);
					iterator = this.trainScope.getFront().getPosition().iterator(true);
					break;
				}
				
				//Iterate over the Edges in order to get the BlockDirection
				do {
					current = iterator.next();
				}while(current.getEdge() != newFrontPosition.getEdge());
				
				if(current.isDirection()) {
					newFront = new BlockPoint(BlockPointProperties.BLOCK_POSITIVE_DIRECTION, newFrontPosition);
				}else {
					newFront = new BlockPoint(BlockPointProperties.BLOCK_NEGATIVE_DIRECTION, newFrontPosition);
				}
			}
			this.trainScope.organiseTrainScope(newFront);
			
			
			//Nachdem der Trainscope neu zusammengesetzt wurde muss die Postition dieses Fahrzeugs dort sein wo sie angegeben wurde.
			assert(this.middlePosition.equals(newPosition));
		}
	}

	public boolean isCountDirection() {
		return countDirection;
	}

	public void setCountDirection(boolean countDirection) {
		this.countDirection = countDirection;
	}

}
