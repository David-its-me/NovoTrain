package modelling;

import java.io.Serializable;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import main.Main;
import main.MySerializable;
import main.StoreHandler;
import modelling.Edge.EdgeIterator;
import modelling.Edge.EdgeIteratorObject;

/**
 * A TrainScope consists out of Vehicles that are coupled together. They all
 * behave as one unit.
 * 
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 26.05.2021
 */
public class TrainScope implements MySerializable {

	private static final long serialVersionUID = -7140613898733525211L;

	/**
	 * The front position of the Brake Curve
	 */
	private BlockPoint brakeCurveFront;
	/**
	 * The front position of the Scope
	 */
	private BlockPoint front;
	/**
	 * The Back of the Scope
	 */
	private BlockPoint back;
	private final double length;
	private final Vehicle firstVehicle;
	private final Vehicle lastVehicle;
	private volatile boolean alive = false;
	private volatile boolean closeSpeedControl = false;

	private final HashSet<Locomotive> locomotives = new HashSet<>();
	private final Set<Edge> enteredEdges;
	private transient Thread speedControlThread;
	private String vehicles;
	/**
	 * The current Speed of the TrainScope in the Model
	 */
	private double currentSpeedInModel;

	private final double brakeAcceleration = 0.7;
	private final double driveAcceleration = 0.5;
	private Status status;

	/**
	 * Initialize a new TrainScope around one Vehicle
	 * 
	 * @param vehicle
	 * @throws Exception
	 */
	TrainScope(Vehicle vehicle) throws Exception {

		this.status = Status.READY;
		this.length = vehicle.getLengthInMeter();
		vehicle.setNextVehicle(null);
		vehicle.setPreviousVehicle(null);
		this.firstVehicle = vehicle;
		this.lastVehicle = vehicle;
		this.enteredEdges = new HashSet<>();

		if (vehicle.toString().contains("Locomotive")) {
			getLocomotives().add((Locomotive) vehicle);
		}

		// Initialize Positional information
		Position middlePosition = vehicle.getMiddlePosition();
		Position frontPosition = new Position(middlePosition.getEdge(), middlePosition.getOffset() + (length / 2));
		Position backPosition = new Position(middlePosition.getEdge(), middlePosition.getOffset() - (length / 2));
		Edge startEdge = middlePosition.getEdge();
		initializeEndPoints(startEdge, true, frontPosition, backPosition);
		this.brakeCurveFront = new BlockPoint(front.getProperties(), new Position(front.getPosition(), true));
		this.organiseTrainScope(front);
		this.currentSpeedInModel = 0;
		printInformation();

		try {
			this.speedControlThread = startSpeedControlThread();
		} catch (Exception e) {
			e.printStackTrace();
		}

		assert (getListOfVehicles().size() == 1);
		assert (getListOfVehicles().get(0) == vehicle);

		// Store the TrainScope
		StoreHandler.addObject(this);
	}

	static TrainScope couple(TrainScope firstScope, TrainScope secondScope) {
		// TODO
		assert (false);
		return new TrainScope(firstScope, secondScope);
	}

	private TrainScope(TrainScope firstScope, TrainScope secondScope) {
		double distanceBackBack = firstScope.back.getPosition().calculateDistanceTo(secondScope.back.getPosition(),
				Main.TOLERANCE_DISTANCE * 3);
		double distanceBackFront = firstScope.back.getPosition().calculateDistanceTo(secondScope.front.getPosition(),
				Main.TOLERANCE_DISTANCE * 3);
		double distanceFrontBack = firstScope.front.getPosition().calculateDistanceTo(secondScope.back.getPosition(),
				Main.TOLERANCE_DISTANCE * 3);
		double distanceFrontFront = firstScope.front.getPosition().calculateDistanceTo(secondScope.front.getPosition(),
				Main.TOLERANCE_DISTANCE * 3);

		double smallesDistance = Math.min(Math.min(distanceFrontFront, distanceFrontBack),
				Math.min(distanceBackBack, distanceBackFront));

		// TODO
		// TODO
		if (smallesDistance == distanceFrontFront) {
			secondScope.invertDirection();

		} else if (smallesDistance == distanceFrontBack) {

		} else if (smallesDistance == distanceBackFront) {

		} else if (smallesDistance == distanceBackBack) {
			secondScope.invertDirection();

		} else if (smallesDistance == Double.MAX_VALUE) {
			throw new Exception("The Trainscopes couldn't be coupeled together, because they are not close enought");
		} else {
			assert (false);
		}
		// TODO
		assert (false);
		firstScope.delete();
		secondScope.delete();
		// return the new TrainScope
		return null;
	}

	synchronized public void invertDirection() throws Exception {
		if (currentSpeedInModel != 0) {
			throw new Exception("You cannot invert the direction while the TrainScope drives at a speed");
		}
		BlockPoint buffer = back;
		back = front;
		front = buffer;
	}

	/**
	 * the start Edge should be a Edge in the trainScope. ONLY USE THIS METHOD WITH
	 * THE INITIAL CONSTRUCTOR
	 * 
	 * @param startEdge
	 * @param the           direction of the Train it has at the start Edge.
	 * @param frontPosition
	 * @param backPosition
	 * @throws Exception
	 */
	synchronized private void initializeEndPoints(Edge startEdge, boolean directionStartEdge, Position frontPosition,
			Position backPosition) throws Exception {

		BlockPointProperties frontPropertie;
		BlockPointProperties backPropertie;
		if (directionStartEdge) {
			frontPropertie = BlockPointProperties.BLOCK_NEGATIVE_DIRECTION;
			backPropertie = BlockPointProperties.BLOCK_POSITIVE_DIRECTION;
		} else {
			frontPropertie = BlockPointProperties.BLOCK_POSITIVE_DIRECTION;
			backPropertie = BlockPointProperties.BLOCK_NEGATIVE_DIRECTION;
		}

		// Iterate forward in order to find the FrontPosition
		Edge currentEdge = startEdge;
		Edge nextEdge;
		while (frontPosition.getEdge() != currentEdge) {
			switch (frontPropertie) {
			case BLOCK_ALL:
				assert (false);
				break;
			case BLOCK_NEGATIVE_DIRECTION:
				nextEdge = currentEdge.getNextEdge();
				if (nextEdge.getNextEdge() == currentEdge) {
					frontPropertie = BlockPointProperties.BLOCK_POSITIVE_DIRECTION;
				}
				currentEdge = nextEdge;
				break;
			case BLOCK_POSITIVE_DIRECTION:
				nextEdge = currentEdge.getPreviousEdge();
				if (nextEdge.getPreviousEdge() == currentEdge) {
					frontPropertie = BlockPointProperties.BLOCK_NEGATIVE_DIRECTION;
				}
				currentEdge = nextEdge;
				break;
			default:
				assert (false);
				break;
			}
		}
		assert (front == null);
		BlockPoint oldFront = front;
		this.front = new BlockPoint(frontPropertie, frontPosition);

		// Iterate Backwards in order to find the BackPosition
		currentEdge = startEdge;
		nextEdge = null;
		while (backPosition.getEdge() != currentEdge) {
			switch (backPropertie) {
			case BLOCK_ALL:
				assert (false);
				break;
			case BLOCK_NEGATIVE_DIRECTION:
				nextEdge = currentEdge.getNextEdge();
				if (nextEdge.getNextEdge() == currentEdge) {
					backPropertie = BlockPointProperties.BLOCK_POSITIVE_DIRECTION;
				}
				currentEdge = nextEdge;
				break;
			case BLOCK_POSITIVE_DIRECTION:
				nextEdge = currentEdge.getPreviousEdge();
				if (nextEdge.getPreviousEdge() == currentEdge) {
					backPropertie = BlockPointProperties.BLOCK_NEGATIVE_DIRECTION;
				}
				currentEdge = nextEdge;
				break;
			default:
				assert (false);
				break;
			}
		}
		assert (back == null);
		BlockPoint oldBack = back;
		this.back = new BlockPoint(backPropertie, backPosition);

		if (oldFront != null) {
			oldFront.delete();
		}
		if (oldBack != null) {
			oldBack.delete();
		}
	}

	synchronized double getCurrentSpeedInModel() {
		return currentSpeedInModel;
	}

	synchronized void setCurrentSpeedInModel(double currentSpeedInModel) {
		this.currentSpeedInModel = currentSpeedInModel;
	}

	/**
	 * Start the SpeedControl Thread of a Vehicle
	 * 
	 * @return Thread
	 * @throws Exception - if there is already a Thread
	 */
	synchronized public Thread startSpeedControlThread() throws Exception {
		if (alive) {
			throw new Exception("The TrainScope with Vehicles " + vehicles + " already runs a SpeedControl Thread");
		}
		// If there is no locomotive in the TrainScope, then the Scope can't drive any
		// speed.
		if (getLocomotives().isEmpty()) {
			closeSpeedControl = true;
			alive = false;
		} else {
			closeSpeedControl = false;
			alive = true;
		}

		// Start SpeedControl Thread
		Runnable speedControl = new SpeedControl(this);
		speedControlThread = new Thread(speedControl);
		speedControlThread.start();
		return speedControlThread;
	}

	public void closeSpeedControlThread() throws InterruptedException {
		synchronized (this) {
			closeSpeedControl = true;
		}
		speedControlThread.join();
		assert (!alive);
	}

	@Override
	synchronized public void delete() throws Exception {
		assert (closeSpeedControl);
		assert (!alive);
		for (Edge edge : this.enteredEdges) {
			edge.leave(this);
		}
		StoreHandler.deleteObject(this);
		front.delete();
		back.delete();
		brakeCurveFront.delete();
		alive = false;
	}

	synchronized BlockPoint getFront() {
		return front;
	}

	synchronized BlockPoint getBack() {
		return back;
	}

	double getBrakeAcceleration() {
		return brakeAcceleration;
	}

	double getDriveAcceleration() {
		return driveAcceleration;
	}

	synchronized public void printInformation() {
		Iterator<Vehicle> iterator = this.iterator();
		vehicles = iterator.next().getName();
		while (iterator.hasNext()) {
			vehicles += " # " + iterator.next().getName();
		}

		System.out.println("TrainScope Message of " + vehicles + ":");
		if (alive) {
			System.out.println("      SpeedControl Thread is running");
			System.out.println("      Speed: " + currentSpeedInModel + "m/s in the model");
			System.out.println("             " + currentSpeedInModel * Main.SCALE + "m/s --- "
					+ ((int) (currentSpeedInModel * Main.SCALE * 3.6)) + "km/h");
		}
		System.out.println("      BrakeCurveFront: Object: " + brakeCurveFront.toString());
		System.out.println("                       Position(" + brakeCurveFront.getPosition().getEdge().getOrigin()
				+ "," + brakeCurveFront.getPosition().getOffset() + ")");
		System.out.println("                       Propertie: " + brakeCurveFront.getProperties());

		System.out.println("      Front Position:  Object: " + front.toString());
		System.out.println("                       Position(" + front.getPosition().getEdge().getOrigin() + ","
				+ front.getPosition().getOffset() + ") ");
		System.out.println("                       Propertie: " + front.getProperties());

		System.out.println("      Back Position:   Object: " + back.toString());
		System.out.println("                       Position(" + back.getPosition().getEdge().getOrigin() + ","
				+ back.getPosition().getOffset() + ") ");
		System.out.println("                       Propertie: " + back.getProperties());
		System.out.println();

	}

	synchronized List<Vehicle> getListOfVehicles() {
		List<Vehicle> vehicles = new LinkedList<>();
		Iterator<Vehicle> iterator = this.iterator();
		while (iterator.hasNext()) {
			vehicles.add(iterator.next());
		}
		return vehicles;
	}

	/**
	 * decouple a TrainScope between the firstVehicle and the secondVehicle
	 * 
	 * @param firstVehicle
	 * @param secondVehicle
	 */
	synchronized static void decouple(Vehicle firstVehicle, Vehicle secondVehicle) {
		// TODO
		assert (false);
	}

	synchronized Vehicle getFirstVehicle() {
		return firstVehicle;
	}

	synchronized Vehicle getLastVehicle() {
		return lastVehicle;
	}

	public HashSet<Locomotive> getLocomotives() {
		return locomotives;
	}

	/**
	 * This Method checks weather all Vehicles in the TrainScope have the
	 * rightPosition respectively to each other
	 * 
	 * @return true - if everything seems to be right.
	 */
	synchronized boolean checkValid() {
		if (firstVehicle == null || lastVehicle == null) {
			return false;
		}
		Position helperPosition;
		VehicleIterator iterator = this.iterator();
		Vehicle currentVehicle = null;
		double offset = 0;

		while (iterator.hasNext()) {
			currentVehicle = iterator.next();
			offset += currentVehicle.getLengthInMeter() * 0.5;
			helperPosition = new Position(front.getPosition(), false);
			switch (front.getProperties()) {
			case BLOCK_ALL:
				return false;
			case BLOCK_NEGATIVE_DIRECTION:
				helperPosition.setOffset(helperPosition.getOffset() - offset);
				break;
			case BLOCK_POSITIVE_DIRECTION:
				helperPosition.setOffset(helperPosition.getOffset() + offset);
				break;
			}

			if (!currentVehicle.getMiddlePosition().equals(helperPosition)) {
				return false;
			}
			offset += currentVehicle.getLengthInMeter() * 0.5;

		}

		if (currentVehicle != lastVehicle) {
			return false;
		}

		helperPosition = new Position(front.getPosition(), false);
		switch (front.getProperties()) {
		case BLOCK_ALL:
			return false;
		case BLOCK_NEGATIVE_DIRECTION:
			helperPosition.setOffset(helperPosition.getOffset() - offset);
			break;
		case BLOCK_POSITIVE_DIRECTION:
			helperPosition.setOffset(helperPosition.getOffset() + offset);
			break;
		}
		if (!back.getPosition().equals(helperPosition)) {
			return false;
		}

		return true;
	}

	/**
	 * Organize the Vehicle Middle Positions according to the Front Point.
	 * 
	 * @param newFront
	 * @return length of the TrainScope.
	 */
	synchronized private double organizeVehicles(BlockPoint newFront) {
		Position helperPosition;
		VehicleIterator vehicleIterator = this.iterator();
		Vehicle currentVehicle = null;
		double offset = 0;
		EdgeIterator edgeIterator = null;
		EdgeIteratorObject currentEdge;
		switch (newFront.getProperties()) {
		case BLOCK_ALL:
			// Dieser Fall darf nicht auftreten.
			assert (false);
			break;
		case BLOCK_NEGATIVE_DIRECTION:
			edgeIterator = front.getPosition().iterator(false);
			break;
		case BLOCK_POSITIVE_DIRECTION:
			edgeIterator = front.getPosition().iterator(true);
			break;
		}
		currentEdge = edgeIterator.next();

		while (vehicleIterator.hasNext()) {
			currentVehicle = vehicleIterator.next();
			offset += currentVehicle.getLengthInMeter() * 0.5;
			helperPosition = new Position(newFront.getPosition(), false);
			switch (newFront.getProperties()) {
			case BLOCK_ALL:
				// Dieser Fall darf nicht auftreten.
				assert (false);
				break;
			case BLOCK_NEGATIVE_DIRECTION:
				helperPosition.setOffset(helperPosition.getOffset() - offset);
				break;
			case BLOCK_POSITIVE_DIRECTION:
				helperPosition.setOffset(helperPosition.getOffset() + offset);
				break;
			}

			currentVehicle.getMiddlePosition().setPosition(helperPosition);

			// Set countDirection of Vehicle by using the EdgeIterator
			int i = 0;
			while (currentEdge.getEdge() != currentVehicle.getMiddlePosition().getEdge()) {
				currentEdge = edgeIterator.next();
				i++;
				// Es ist sehr unwarscheinlich, dass sich ein Fahrzeug sich über 10 Edges
				// erstreckt.
				assert (i < 10);
			}
			if (currentEdge.isDirection()) {
				currentVehicle.setCountDirection(false);
			} else {
				currentVehicle.setCountDirection(true);
			}

			offset += currentVehicle.getLengthInMeter() * 0.5;

		}
		assert (currentVehicle == lastVehicle);
		return offset;
	}

	/**
	 * Organize the BackPosition of the TrainScope
	 * 
	 * @param newFront
	 * @param scopeLength
	 */
	synchronized private void organizeBack(BlockPoint newFront, double scopeLength) {
		// Calculate Position of Back
		Position newBackPosition = new Position(newFront.getPosition(), false);
		EdgeIterator edgeIterator = null;
		EdgeIteratorObject current = null;
		switch (newFront.getProperties()) {
		case BLOCK_ALL:
			// Dieser Fall darf nicht auftreten
			assert (false);
			break;
		case BLOCK_NEGATIVE_DIRECTION:
			newBackPosition.setOffset(newBackPosition.getOffset() - scopeLength);
			edgeIterator = newFront.getPosition().iterator(false);
			current = edgeIterator.next();
			assert (!current.isDirection());
			break;
		case BLOCK_POSITIVE_DIRECTION:
			newBackPosition.setOffset(newBackPosition.getOffset() + scopeLength);
			edgeIterator = newFront.getPosition().iterator(true);
			current = edgeIterator.next();
			assert (current.isDirection());
			break;
		}

		// Calculate Blocking direction of Back
		while (current.getEdge() != newBackPosition.getEdge()) {
			current = edgeIterator.next();
		}
		BlockPoint oldBack = back;
		if (current.isDirection()) {
			back = new BlockPoint(BlockPointProperties.BLOCK_NEGATIVE_DIRECTION, newBackPosition);
		} else {
			back = new BlockPoint(BlockPointProperties.BLOCK_POSITIVE_DIRECTION, newBackPosition);
		}

		if (newFront != front) {
			BlockPoint oldFront = front;
			front = newFront;
			oldFront.delete();
		}
		oldBack.delete();

	}

	private double calculateIntendedDistanceFrontToBrakeCurveFront() {
		return accelerationDistance(this.currentSpeedInModel, 0, this.getBrakeAcceleration()) + Main.TOLERANCE_DISTANCE;
	}

	/**
	 * Organize the BrakeCurve Front of the TrainScope
	 * 
	 * @param newFront
	 */
	synchronized private void organizeBrakeCurveFront(BlockPoint newFront) {
		// Distance to front is the BrakeDistance + ToleranceDistance
		double distanceToFront = calculateIntendedDistanceFrontToBrakeCurveFront();
		if (this.currentSpeedInModel == 0) {
			assert distanceToFront == Main.TOLERANCE_DISTANCE;
		}

		Position newBrakeCurveFrontPosition = new Position(newFront.getPosition(), true);
		EdgeIterator iterator = null;
		switch (newFront.getProperties()) {
		case BLOCK_ALL:
			assert false;
			break;
		case BLOCK_NEGATIVE_DIRECTION:
			iterator = newBrakeCurveFrontPosition.iterator(true);
			newBrakeCurveFrontPosition.setOffset(newBrakeCurveFrontPosition.getOffset() + distanceToFront);
			break;
		case BLOCK_POSITIVE_DIRECTION:
			iterator = newBrakeCurveFrontPosition.iterator(false);
			newBrakeCurveFrontPosition.setOffset(newBrakeCurveFrontPosition.getOffset() - distanceToFront);
			break;
		}
		EdgeIteratorObject current = iterator.next();
		while (current.getEdge() != newBrakeCurveFrontPosition.getEdge()) {
			current = iterator.next();
		}
		BlockPoint oldBrakeCurveFront = brakeCurveFront;
		if (current.isDirection()) {
			brakeCurveFront = new BlockPoint(BlockPointProperties.BLOCK_NEGATIVE_DIRECTION, newBrakeCurveFrontPosition);
		} else {
			brakeCurveFront = new BlockPoint(BlockPointProperties.BLOCK_POSITIVE_DIRECTION, newBrakeCurveFrontPosition);
		}
		oldBrakeCurveFront.delete();
	}

	/**
	 * Enter and Leave Edges that are used by the TrainScope
	 */
	synchronized private void organizeEnteredEdges() {
		EdgeIterator iterator = null;
		switch (brakeCurveFront.getProperties()) {
		case BLOCK_ALL:
			// Dieser Fall darf nicht auftreten
			assert (false);
			break;
		case BLOCK_NEGATIVE_DIRECTION:
			iterator = brakeCurveFront.getPosition().iterator(false);
			break;
		case BLOCK_POSITIVE_DIRECTION:
			iterator = brakeCurveFront.getPosition().iterator(true);
			break;
		}

		EdgeIteratorObject current = null;
		Set<Edge> oldEnteredEdges = new HashSet<>();
		oldEnteredEdges.addAll(enteredEdges);
		this.enteredEdges.clear();

		do {
			current = iterator.next();
			this.enteredEdges.add(current.getEdge());
		} while (current.getEdge() != back.getPosition().getEdge());

		// Enter all Edges that are used by this TrainScope
		for (Edge edge : enteredEdges) {
			edge.enter(this);
		}

		// Leave Edges that are no longer in the set of enteredEdges
		oldEnteredEdges.removeAll(enteredEdges);
		for (Edge edge : oldEnteredEdges) {
			edge.leave(this);
		}
	}

	/**
	 * Update all positional information in the the TrainScope according to the
	 * front.
	 */
	synchronized void organiseTrainScope(BlockPoint newFront) {

		// organize all Middle Positions of the Vehicles
		double scopeLength = organizeVehicles(newFront);
		// organize the Back BlockPoint with the information about the length of the
		// scope
		organizeBack(newFront, scopeLength);
		// set the brakeCurve BlockPoint
		organizeBrakeCurveFront(newFront);
		// Notify Edges that are covered by this TrainScope
		organizeEnteredEdges();
		assert (checkValid());
	}

	/**
	 * The Vehicle Iterator iterates above all Vehicles in the TrainScope. The
	 * Iterator starts with the first Vehicle and ends with the last Vehicle.
	 * 
	 * @return VehicleIterator
	 */
	private VehicleIterator iterator() {
		return new VehicleIterator();
	}

	/**
	 * The Vehicle Iterator iterates above all Vehicles in the TrainScope. The
	 * Iterator starts with the first Vehicle and ends with the last Vehicle.
	 * 
	 * @author David Lieb
	 * @author david.lieb.00@gmail.com
	 * @version 26.05.2021
	 */
	private class VehicleIterator implements Iterator<Vehicle> {

		Vehicle currentVehicle;

		private VehicleIterator() {
			currentVehicle = getFirstVehicle();
		}

		@Override
		public boolean hasNext() {
			if (currentVehicle != null) {
				return true;
			}
			return false;
		}

		@Override
		public Vehicle next() {
			Vehicle returnVehicle = currentVehicle;
			currentVehicle = currentVehicle.getPreviousVehicle();
			return returnVehicle;
		}

	}

	/**
	 * This method calculates the remaining speed that a locomotive may still have
	 * depending on the remaining braking distance and the allowed remaining speed
	 * at the end of the braking distance.
	 * 
	 * @param remainingBrakeDistance - the remaining distance to the target brake
	 *                               point, in meters of the Model
	 * @param bypassSpeed            - the remaining speed at the target brake
	 *                               point, in m/s in the model at the end of the
	 *                               brake-curve
	 * @param brakeAcceleration      - the brake acceleration
	 * @return remainingSpeed in m/s in the model
	 */
	private static double brakeCurve(double remainingBrakeDistance, double bypassSpeed,
			final double brakeAcceleration) {

		if (remainingBrakeDistance <= 0) {
			return bypassSpeed;
		}

		// Convert in Reality
		remainingBrakeDistance = remainingBrakeDistance * Main.SCALE;
		bypassSpeed = bypassSpeed * Main.SCALE;

		double remainingSpeed;
		remainingSpeed = Math.sqrt(2 * remainingBrakeDistance * brakeAcceleration + Math.pow(bypassSpeed, 2));

		// Re-Convert into Model
		remainingSpeed = remainingSpeed / (double) Main.SCALE;
		return remainingSpeed;
	}

	/**
	 * This Method calculates the distance used to accelerate to a given speed, with
	 * startSpeed as already given.
	 * 
	 * @param speed        - the speed to be accelerated in m/s in the model
	 * @param startSpeed   - the speed already given in m/s in the model. Or you may
	 *                     call it bypass Speed.
	 * @param acceleration - the acceleration
	 * @return distance - the distance that is used to reach the speed, in meters in
	 *         the Model.
	 */
	private static double accelerationDistance(double speed, double startSpeed, double acceleration) {

		if (speed <= startSpeed) {
			return 0;
		}

		// Convert in Reality
		speed = speed * Main.SCALE;
		startSpeed = startSpeed * Main.SCALE;

		double accelerationDistance = 0.5 * ((Math.pow(speed, 2) - Math.pow(startSpeed, 2)) / acceleration);

		// Re-Convert into Model
		accelerationDistance = accelerationDistance / (double) Main.SCALE;
		return accelerationDistance;
	}

	synchronized private double getMaxSpeedOfVehicles() {
		double maxSpeed = Double.MAX_VALUE;
		for (Vehicle vehicle : getListOfVehicles()) {
			maxSpeed = Math.min(maxSpeed, vehicle.getMaxSpeedInRealMeterPerSecond() / (double) Main.SCALE);
		}
		assert (maxSpeed != Double.MAX_VALUE);
		return maxSpeed;
	}

	synchronized private double getMaxSpeedBecauseOfCurrentSpeedLimitOfLocomoitves() {
		double maxSpeed = Double.MAX_VALUE;
		for (Locomotive locomotive : this.locomotives) {
			maxSpeed = Math.min(maxSpeed, locomotive.getCurrentSpeedLimit());
		}
		assert (maxSpeed != Double.MAX_VALUE);
		return maxSpeed;
	}

	synchronized private double getMaxSpeedBecauseOfSpeedLimitOnEdges() {
		double maxSpeed = Double.MAX_VALUE;
		// MaxSpeed on Edges
		EdgeIterator iterator = null;
		switch (back.getProperties()) {
		case BLOCK_ALL:
			assert false;
			break;
		case BLOCK_NEGATIVE_DIRECTION:
			iterator = back.getPosition().iterator(false);
			break;
		case BLOCK_POSITIVE_DIRECTION:
			iterator = back.getPosition().iterator(true);
			break;

		}
		EdgeIteratorObject current;
		do {
			current = iterator.next();
			maxSpeed = Math.min(maxSpeed, current.getEdge().getMaxSpeed() / (double) Main.SCALE);
		} while (current.getEdge() != front.getPosition().getEdge());
		assert (current.getEdge() == front.getPosition().getEdge());

		if (front.getPosition().getEdge() == brakeCurveFront.getPosition().getEdge()) {
			assert (current.getEdge() == brakeCurveFront.getPosition().getEdge());
			return maxSpeed;
		}

		// MaxSpeed on Edges in the BrakeCurve
		double border = accelerationDistance(currentSpeedInModel, 0, getBrakeAcceleration());
		border += Main.EPSILON;
		do {
			current = iterator.next();
			Position helperPosition;
			if (current.isDirection()) {
				helperPosition = new Position(current.getEdge(), 0, false);
			} else {
				helperPosition = new Position(current.getEdge(), current.getEdge().getLength() - Main.EPSILON, false);
			}

			double speed = brakeCurve(helperPosition.calculateDistanceTo(front.getPosition(), border),
					helperPosition.getEdge().getMaxSpeed() / (double) Main.SCALE, getBrakeAcceleration());
			maxSpeed = Math.min(maxSpeed, speed);

		} while (current.getEdge() != brakeCurveFront.getPosition().getEdge());
		assert (current.getEdge() == brakeCurveFront.getPosition().getEdge());

		return maxSpeed;
	}

	/**
	 * This Method is a helper Method to calculate the remaining Distance to another
	 * Position which is BEFORE the Front
	 * 
	 * @param position
	 * @return
	 */
	synchronized private double getRemainingDistanceFromFrontTo(Position position) {
		double border = calculateIntendedDistanceFrontToBrakeCurveFront();
		border += Main.TOLERANCE_DISTANCE;
		border += Main.TOLERANCE_DISTANCE;

		switch (front.getProperties()) {
		case BLOCK_ALL:
			assert false;
			break;
		case BLOCK_NEGATIVE_DIRECTION:
			return front.getPosition().calculateDistanceTo(position, border, true);
		case BLOCK_POSITIVE_DIRECTION:
			return front.getPosition().calculateDistanceTo(position, border, false);
		}
		assert (false);
		return 0;

	}

	/**
	 * Helper Method()
	 * 
	 * @param block
	 * @return
	 */
	synchronized private double getMaxSpeedInOrderToBrakeUntil(Position position) {
		double remainingBrakeDistance = getRemainingDistanceFromFrontTo(position) - Main.TOLERANCE_DISTANCE;

		double maxSpeed = brakeCurve(remainingBrakeDistance, 0, getBrakeAcceleration());

		return maxSpeed;
	}

	/**
	 * Helper Method for getMaxSpeedBecauseOfBlockPoints()
	 * 
	 * @param block
	 * @return maxSpeed - calculated with brakeCurve
	 */
	synchronized private double getMaxSpeedBecauseOfTheSpecificBlockPoint(BlockPoint block,
			EdgeIteratorObject current) {

		assert block.getPosition().getEdge() == current.getEdge();
		if (block == brakeCurveFront) {

			/*
			 * Wenn BrakeCurve Front näher an der Front ist, als eigentlich vorgesehen, dann
			 * entwickelt sich diser BlockPoint auch zum "Hindernis". Dieser Fall tritt auf,
			 * wenn der BrakeCurveFront aus verschiedenen Gründen nicht so weit nach vorne
			 * "geschoben" werden kann wie eigentlich erwartet. Z.B. bei einer Switch, nicht
			 * verbundene Nodes, Bumper, etc.
			 */
			double intendedDistance = calculateIntendedDistanceFrontToBrakeCurveFront();
			if (intendedDistance > getRemainingDistanceFromFrontTo(brakeCurveFront.getPosition()) + Main.EPSILON) {
				return getMaxSpeedInOrderToBrakeUntil(brakeCurveFront.getPosition());
			} else {
				return Double.MAX_VALUE;
			}
		}

		// BlockPoints werden erst dann zum "Hinderniss", wenn sie in die Edge in die
		// richtige Richtung sperren.
		switch (block.getProperties()) {
		case BLOCK_ALL:
			return getMaxSpeedInOrderToBrakeUntil(block.getPosition());
		case BLOCK_NEGATIVE_DIRECTION:
			if (!current.isDirection()) {
				return getMaxSpeedInOrderToBrakeUntil(block.getPosition());
			}
			break;
		case BLOCK_POSITIVE_DIRECTION:
			if (current.isDirection()) {
				return getMaxSpeedInOrderToBrakeUntil(block.getPosition());
			}
			break;
		}
		return Double.MAX_VALUE;

	}

	synchronized private boolean brakeCurveFrontIsBehindFront() {
		assert brakeCurveFront.getPosition().getEdge() == front.getPosition().getEdge();
		switch (front.getProperties()) {
		case BLOCK_ALL:
			assert false;
			return false;
		case BLOCK_NEGATIVE_DIRECTION:
			if (brakeCurveFront.getPosition().getOffset() <= front.getPosition().getOffset()) {
				return true;
			}
			return false;
		case BLOCK_POSITIVE_DIRECTION:
			if (brakeCurveFront.getPosition().getOffset() >= front.getPosition().getOffset()) {
				return true;
			}
			return false;
		}
		return false;
	}

	synchronized private double getMaxSpeedBecauseOfBlockPoints() {
		double maxSpeed = Double.MAX_VALUE;
		Iterator<EdgeIteratorObject> iterator = null;
		EdgeIteratorObject current = null;
		switch (front.getProperties()) {
		case BLOCK_ALL:
			assert false;
			break;
		case BLOCK_NEGATIVE_DIRECTION:
			iterator = front.getPosition().iterator(true);
			break;
		case BLOCK_POSITIVE_DIRECTION:
			iterator = front.getPosition().iterator(false);
			break;
		}

		boolean brakeCurveFrontReached = false;
		assert (iterator.hasNext());
		current = iterator.next();
		for (BlockPoint block : current.getEdge().getBlockPoints()) {

			if (block == brakeCurveFront && brakeCurveFrontIsBehindFront()) {
				/*
				 * Sonderfall: bei einer Zyklischen Strecke kommt es vor, dass ab einer
				 * bestimmten Geschwindigkeit die BrakeCurveFront hinter Back stehen kann. Das
				 * tritt auf, sobald der Bremsweg länger als die Zyklische Strecke wird. In
				 * diesem Fall sind beide auf der gleichen Edge, was dazu führt, dass die while
				 * Schleife frühzeitig abgebrochen würde, wenn man diesen Fall nicht beachtet.
				 */
			} else if (block == back) {
				// Do Nothing
			} else {
				maxSpeed = Math.min(maxSpeed, getMaxSpeedBecauseOfTheSpecificBlockPoint(block, current));
				if (block == brakeCurveFront) {
					brakeCurveFrontReached = true;
				}
			}
		}

		/*
		 * Sobald der vorderste Punkt der Bremskurve erreicht wird, ist kein Hinderniss
		 * mehr im Weg, das den Zug zum Bremsen veranlassen würde und Sobald ein
		 * BlockPoint im Weg steht, wird maxSpeed wohl nicht mehr Double.MAX_VALUE
		 * besitzen
		 */
		while (!brakeCurveFrontReached && maxSpeed == Double.MAX_VALUE) {
			assert (iterator.hasNext());
			current = iterator.next();
			for (BlockPoint block : current.getEdge().getBlockPoints()) {
				maxSpeed = Math.min(maxSpeed, getMaxSpeedBecauseOfTheSpecificBlockPoint(block, current));
				if (block == brakeCurveFront) {
					brakeCurveFrontReached = true;
				}
			}
		}

		return maxSpeed;
	}

	/**
	 * Calculates the maxPossible Speed of the TrainScope because of several
	 * properties.
	 * 
	 * @return maxSpeed in m/s in the model
	 */
	synchronized private double calculateMaxPossibleSpeed(long passedTime) {

		double maxSpeed = this.currentSpeedInModel;
		// MaxSpeed after positive acceleration
		maxSpeed = maxSpeed * Main.SCALE;
		maxSpeed += getDriveAcceleration() * ((double) passedTime / 1000.0);
		maxSpeed = maxSpeed / (double) Main.SCALE;

		// MaxSpeed because of maxSpeed of a Vehicle
		maxSpeed = Math.min(maxSpeed, getMaxSpeedOfVehicles());
		// MaxSpeed because of DCC Limit of Locomotive
		maxSpeed = Math.min(maxSpeed, getMaxSpeedBecauseOfCurrentSpeedLimitOfLocomoitves());
		// MaxSpeed because speedLimit on Edges
		maxSpeed = Math.min(maxSpeed, getMaxSpeedBecauseOfSpeedLimitOnEdges());
		// MexSpeed because of BlockPoints
		maxSpeed = Math.min(maxSpeed, getMaxSpeedBecauseOfBlockPoints());

		assert (maxSpeed < 100);
		return maxSpeed;
	}

	/**
	 * Update the Positional and Directional information of the Front after
	 * traveling for a specific time.
	 * 
	 * @param passedTime - the time traveled.
	 * @return the new front.
	 */
	synchronized private BlockPoint updateFront(long passedTime) {
		double distanceTraveled = ((double) passedTime / 1000.0) * this.currentSpeedInModel;

		// Set Front
		Position newFrontPosition = new Position(front.getPosition(), true);
		EdgeIterator iterator = null;
		switch (front.getProperties()) {
		case BLOCK_ALL:
			assert (false);
			break;
		case BLOCK_NEGATIVE_DIRECTION:
			iterator = newFrontPosition.iterator(true);
			newFrontPosition.setOffset(newFrontPosition.getOffset() + distanceTraveled);
			break;
		case BLOCK_POSITIVE_DIRECTION:
			iterator = newFrontPosition.iterator(false);
			newFrontPosition.setOffset(newFrontPosition.getOffset() - distanceTraveled);
			break;
		default:
			break;
		}
		BlockPoint oldFront = front;
		EdgeIteratorObject current = iterator.next();
		while (current.getEdge() != newFrontPosition.getEdge()) {
			current = iterator.next();
		}
		if (current.isDirection()) {
			front = new BlockPoint(BlockPointProperties.BLOCK_NEGATIVE_DIRECTION, newFrontPosition);
		} else {
			front = new BlockPoint(BlockPointProperties.BLOCK_POSITIVE_DIRECTION, newFrontPosition);
		}
		oldFront.delete();
		return front;
	}

	synchronized private void updatePosition(long passedTime) {
		// Calculate new Front
		BlockPoint newFront = updateFront(passedTime);
		// organize TrainScope
		organiseTrainScope(newFront);
	}

	/**
	 * Each TrainScope has a speed control Thread that controls the behavior.
	 * 
	 * @author David Lieb
	 * @author david.lieb.00@gmail.com
	 * @version 14.05.2021
	 */
	private class SpeedControl implements Runnable, Serializable {

		private static final long serialVersionUID = 4466088373838226371L;
		private final TrainScope scope;

		private SpeedControl(TrainScope scope) {
			this.scope = scope;
		}

		private void printStartMessage() {
			System.out.println("Start SpeedControl Thread of " + vehicles);
			System.out.println();
		}

		private void printEndMessage() {
			System.out.println("SpeedControl Thread of " + vehicles + " terminated");
			System.out.println();
		}

		@Override
		public void run() {
			speedControlThread = Thread.currentThread();
			printStartMessage();

			int sleepIntervall = 100;
			long lastTime = System.currentTimeMillis();
			long currentTime;

			while (!scope.getLocomotives().isEmpty() && !closeSpeedControl) {
				try {
					// Time Pause
					Thread.sleep(sleepIntervall);
					currentTime = System.currentTimeMillis();
					long passedTime = currentTime - lastTime;
					lastTime = currentTime;

					synchronized (this.scope) {
						// UpdatePosition
						updatePosition(passedTime);

						// Set new Speed for the new Environment/Position
						currentSpeedInModel = calculateMaxPossibleSpeed(passedTime);
						for (Locomotive locomotive : locomotives) {
							locomotive.setCurrentSpeed(currentSpeedInModel);
						}
						// Durch das setzten der Geschwindigkeit entsteht ein anderer Bremsweg
						organizeBrakeCurveFront(front);
						// Alle fünf Sekunden
						
					}
					if (currentTime % 5000 < passedTime) {
						printInformation();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			alive = false;
			printEndMessage();
		}
	}
}
