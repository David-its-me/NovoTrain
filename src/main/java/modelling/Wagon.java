package modelling;

import main.MySerializable;
import main.StoreHandler;

public class Wagon extends Vehicle implements MySerializable{

	
	private static final long serialVersionUID = -1341908304557457636L;

	public Wagon(String name, double lenghtInMeter, double maxSpeed, Position initialMiddlePosition) throws Exception {
		super(name, lenghtInMeter, maxSpeed, initialMiddlePosition);
		StoreHandler.addObject(this);
	}
	
	@Override
	public void delete() throws Exception {
		if(this.getNextVehicle() != null) {
			TrainScope.decouple(this,this.getNextVehicle());
		}
		if(this.getPreviousVehicle() != null) {
			TrainScope.decouple(this, this.getPreviousVehicle());
		}
		//Im Trainscope darf nur noch dieses Fahrzeug sein.
		assert(getNextVehicle() == null);
		assert(getPreviousVehicle() == null);
		assert(getTrainScope().getFirstVehicle() == this);
		assert(getTrainScope().getLastVehicle() == this);
		getTrainScope().delete();
		this.getMiddlePosition().delete();
		StoreHandler.deleteObject(this);
	}

}
