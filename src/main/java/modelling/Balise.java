package modelling;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import main.Main;
import main.MySerializable;
import main.StoreHandler;
import modelling.Edge.EdgeIterator;
import modelling.Edge.EdgeIteratorObject;

public class Balise implements MySerializable {

	private static final long serialVersionUID = 5217412304697230641L;
	private static final HashMap<Integer, Balise> allBalises;
	private final Position position;
	private final int addressNumber;

	static {
		allBalises = new HashMap<>();
		try {
			for (Balise balise : StoreHandler.getAllBalises()) {
				allBalises.put(balise.getAddressNumber(), balise);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Balise(Position position, int addressNumber) throws Exception {
		this.position = position;
		this.addressNumber = addressNumber;
		allBalises.put(addressNumber, this);
		StoreHandler.addObject(this);
	}

	@Override
	public void delete() throws Exception {
		allBalises.remove(addressNumber);
		StoreHandler.deleteObject(this);
		position.delete();
	}

	private Position getPosition() {
		return position;
	}

	private int getAddressNumber() {
		return addressNumber;
	}

	private static Balise getBaliseByAddress(int addressNumber) {
		return allBalises.get(addressNumber);
	}

	private static double calculateMaxSearchDistance(Position position) {
		double maxDistance = Main.TOLERANCE_DISTANCE;
		if (position.getEdge().getLength() - position.getOffset() < position.getOffset()) {

			maxDistance = Math.max(Main.TOLERANCE_DISTANCE,
					Math.min(Main.TOLERANCE_DISTANCE * 2, position.getOffset()));

		} else {
			maxDistance = Math.max(Main.TOLERANCE_DISTANCE,
					Math.min(Main.TOLERANCE_DISTANCE * 2, position.getEdge().getLength() - position.getOffset()));
		}
		return maxDistance;
	}

	private static Set<Edge> getEdgesInRegion(Position position, double maxDistance) {
		Set<Edge> edges = new HashSet<>();
		edges.add(position.getEdge());
		
		// Iterate Forward
		EdgeIterator iterator = position.getEdge().iterator(true);
		EdgeIteratorObject current = iterator.next();
		while (current.getAccumulatedLength() < maxDistance && iterator.hasNext()) {
			edges.add(current.getEdge());
			current = iterator.next();
		}

		// iterate Backwards
		iterator = position.getEdge().iterator(false);
		current = iterator.next();
		while (current.getAccumulatedLength() < maxDistance && iterator.hasNext()) {
			edges.add(current.getEdge());
			current = iterator.next();
		}
		return edges;
	}
	
	private static Set<Vehicle> getVehiclesOnEdges(Set<Edge> edges){
		Set<Vehicle> vehicles = new HashSet<>();
		for(Edge edge : edges) {
			for(TrainScope scope : edge.getTrainScopes()) {
				vehicles.addAll(scope.getListOfVehicles());
			}
		}
		return vehicles;
	}

	private static Vehicle getNearestVehicle(Position position) {
		double maxDistance = calculateMaxSearchDistance(position);
		Set<Edge> edgesToBeDiscovered = getEdgesInRegion(position, maxDistance);
		Set<Vehicle> vehicles = getVehiclesOnEdges(edgesToBeDiscovered);
		
		Vehicle nearestVehicle = null;
		double nearestDistance = Double.MAX_VALUE;
		double distance;
		for(Vehicle vehicle : vehicles) {
			distance = position.calculateDistanceTo(vehicle.getMiddlePosition(), maxDistance);
			if(distance < nearestDistance) {
				nearestDistance = distance;
				nearestVehicle = vehicle;
			}
		}
		return nearestVehicle;
	}

	/**
	 * Call this Method if a Balise is triggered.
	 * @param addressNumber
	 */
	public static void triggerBalise(int addressNumber) {
		Balise balise = getBaliseByAddress(addressNumber);
		Position position = balise.getPosition();
		Vehicle nearest = getNearestVehicle(position);
		if(nearest == null) {
			System.out.println("WARNUNG: Balise " + addressNumber + " was triggered but didn't found a Vehicle in the near Region");
			//TODO eventuell den Toleranzabstand erhöhen.
		}else {
			nearest.updateMiddlePosition(balise.position);
			System.out.println("Balise trigger of the following Tainscope:");
			nearest.getTrainScope().printInformation();
		}
	}
}
