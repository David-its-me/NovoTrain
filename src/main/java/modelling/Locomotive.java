package modelling;

import java.util.HashMap;
import java.util.Iterator;

import main.Main;
import main.MySerializable;
import main.StoreHandler;

/**
 * This class represents a Locomotive, which is a special Type of Vahicles.
 * 
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 10.05.2021
 */
public class Locomotive extends Vehicle implements MySerializable {

	private static final long serialVersionUID = -5468893304843805237L;
	private int currentDccSpeedLimit;
	private int currentDccSpeed;
	/**
	 * here for each dcc Value the speed of the locomotive is stored. Note that this
	 * is the speed of the model railway. If you want to obtain the speed the model
	 * would have in real world, you have to multiply the speed with the SCALE
	 */
	private final double[] meterPerSecond = new double[Main.MAX_DCC_VALUES];
	/**
	 * The average velocity is formed using moving averaging. The window width must
	 * be saved for this. If the window Size is -1, there has already been a value
	 * from the neighbourhood aplied.
	 */
	private final int[] windowSize = new int[Main.MAX_DCC_VALUES];
	private final int maxWindowSize = 100;

	private boolean directionDecoder;
	private final int dccAddress;
	private static final HashMap<Integer, Locomotive> locomotives;
	
	static {
		locomotives = new HashMap<Integer, Locomotive>();
		try {
			for(Locomotive locomotive : StoreHandler.getAllLocomotives()) {
				locomotives.put(locomotive.getDccAddress(), locomotive);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Locomotive(String name, double lenghtInMeter, double maxSpeedInRealMeterPerSecond,
			Position initialMiddlePosition, int dccAddress) throws Exception {
		super(name, lenghtInMeter, maxSpeedInRealMeterPerSecond, initialMiddlePosition);

		this.dccAddress = dccAddress;
		locomotives.put(dccAddress, this);
		directionDecoder = Main.controlUnit.getDirection(this);
		this.meterPerSecond[0] = 0;
		//TODO vielleicht macht das auch keinen Sinn
		this.initializeSimpleValues();
		this.initializeWindowSize();
		this.currentDccSpeed = Main.controlUnit.getDccSpeed(this);
		this.currentDccSpeedLimit = currentDccSpeed;

		StoreHandler.addObject(this);
	}

	@Override
	synchronized public void delete() throws Exception {
		if (this.getNextVehicle() != null) {
			TrainScope.decouple(this, this.getNextVehicle());
		}
		if (this.getPreviousVehicle() != null) {
			TrainScope.decouple(this, this.getPreviousVehicle());
		}

		// Im Trainscope darf nur noch dieses Fahrzeug sein.
		assert (getNextVehicle() == null);
		assert (getPreviousVehicle() == null);
		assert (getTrainScope().getFirstVehicle() == this);
		assert (getTrainScope().getLastVehicle() == this);
		getTrainScope().delete();
		this.getMiddlePosition().delete();
		StoreHandler.deleteObject(this);
	}

	
	synchronized private void initializeSimpleValues() {
		this.meterPerSecond[0] = 0;
		for(int i = 1; i< Main.MAX_DCC_VALUES; i++) {
			this.meterPerSecond[i] = (i - 1) /(double) Main.SCALE * 3;
			
		}
	}
	
	synchronized private void initializeWindowSize() {
		this.windowSize[0] = this.maxWindowSize;
		for(int i = 1; i< Main.MAX_DCC_VALUES; i++) {
			windowSize[i] = -1;
		}
	}
	synchronized private void movingAveraging(int dccSpeed, double meterPerSecond) {
		//TODO nicht getestet
		if (windowSize[dccSpeed] <= 0) {
			this.meterPerSecond[dccSpeed] = meterPerSecond;
			windowSize[dccSpeed] = 1;
		} else if (windowSize[dccSpeed] < maxWindowSize) {
			this.meterPerSecond[dccSpeed] = this.meterPerSecond[dccSpeed] * windowSize[dccSpeed] + meterPerSecond;
			windowSize[dccSpeed]++;
			this.meterPerSecond[dccSpeed] = this.meterPerSecond[dccSpeed] / windowSize[dccSpeed];
		} else {
			assert (windowSize[dccSpeed] == maxWindowSize);
			this.meterPerSecond[dccSpeed] = this.meterPerSecond[dccSpeed] * (maxWindowSize - 1.0) + meterPerSecond;
			this.meterPerSecond[dccSpeed] = this.meterPerSecond[dccSpeed] / maxWindowSize;
		}
	}

	synchronized private void updateDccNeighbourhoodWithNoMeasuredValues(int dccSpeed, double meterPerSecond) {
		//TODO nicht getestet
		double differencePerNeighbour = 0;
		if (dccSpeed >= 2) {
			int nextLowerMeasuredValue = dccSpeed - 1;
			while (this.windowSize[nextLowerMeasuredValue] <= 0) {
				nextLowerMeasuredValue--;
			}
			int numberOfLowerNeighbours = dccSpeed - nextLowerMeasuredValue;
			differencePerNeighbour = (meterPerSecond - this.meterPerSecond[nextLowerMeasuredValue])
					/ numberOfLowerNeighbours;
			for (int i = 1; i < numberOfLowerNeighbours; i++) {
				this.meterPerSecond[nextLowerMeasuredValue + i] = this.meterPerSecond[nextLowerMeasuredValue]
						+ differencePerNeighbour * i;
				this.windowSize[nextLowerMeasuredValue + i] = -1;
			}
		}

		if (dccSpeed >= 1 && dccSpeed < Main.MAX_DCC_VALUES - 1) {
			boolean existsUpperMeasuredValue = true;
			int nextUpperMeasuredValue = dccSpeed + 1;
			while (this.windowSize[nextUpperMeasuredValue] <= 0) {
				if (nextUpperMeasuredValue == Main.MAX_DCC_VALUES - 1) {
					existsUpperMeasuredValue = false;
					break;
				}
				nextUpperMeasuredValue++;
			}
			int numberOfUpperNeighbours = nextUpperMeasuredValue - dccSpeed;
			if (existsUpperMeasuredValue) {
				differencePerNeighbour = (this.meterPerSecond[nextUpperMeasuredValue] - meterPerSecond)
						/ numberOfUpperNeighbours;
			} // else use the difference per Neighbour from above
			for (int i = 1; i < numberOfUpperNeighbours; i++) {
				this.meterPerSecond[dccSpeed + i] = this.meterPerSecond[dccSpeed] + differencePerNeighbour * i;
				this.windowSize[dccSpeed + i] = -1;
			}
		}
	}

	synchronized private int getNextUpperMeasuredValue(int dccSpeed) {
		//TODO  nicht getestet
		if (dccSpeed >= Main.MAX_DCC_VALUES) {
			return Integer.MAX_VALUE;
		}
		int nextUpperMeasuredValue = dccSpeed + 1;
		while (this.windowSize[nextUpperMeasuredValue] <= 0) {
			if (nextUpperMeasuredValue == Main.MAX_DCC_VALUES - 1) {
				return Integer.MAX_VALUE;
			}
			nextUpperMeasuredValue++;
		}
		return nextUpperMeasuredValue;
	}

	private void bubbleSortSpeeds() {
		//TODO  nicht getestet
		int currentDccValue = 0;
		int nextDccValue = getNextUpperMeasuredValue(currentDccValue);
		while (nextDccValue < Main.MAX_DCC_VALUES) {
			assert (windowSize[nextDccValue] > 0);
			if (meterPerSecond[currentDccValue] > meterPerSecond[nextDccValue]) {
				// Exchange the values
				double cacheSpeed = meterPerSecond[currentDccValue];
				meterPerSecond[currentDccValue] = meterPerSecond[nextDccValue];
				meterPerSecond[nextDccValue] = cacheSpeed;
				int cacheWindow = windowSize[currentDccValue];
				windowSize[currentDccValue] = windowSize[nextDccValue];
				windowSize[nextDccValue] = cacheWindow;
			}
			currentDccValue = nextDccValue;
			nextDccValue = getNextUpperMeasuredValue(currentDccValue);
		}
	}

	synchronized void addSpeedMeasurement(int dccSpeed, double meterPerSecond) {
		//TODO nicht getestet
		assert (0 <= dccSpeed);
		assert (dccSpeed < Main.MAX_DCC_VALUES);
		assert (windowSize[dccSpeed] >= -1);
		assert (windowSize[dccSpeed] <= maxWindowSize);

		movingAveraging(dccSpeed, meterPerSecond);
		bubbleSortSpeeds();
		bubbleSortSpeeds();
		bubbleSortSpeeds();
		updateDccNeighbourhoodWithNoMeasuredValues(dccSpeed, meterPerSecond);

	}

	synchronized public boolean isDirectionDecoder() {
		return directionDecoder;
	}

	synchronized void setDirectionDecoder(boolean directionDecoder) {
		this.directionDecoder = directionDecoder;
		Main.controlUnit.setDccSpeed(this, (byte) 0);
	}

	synchronized public int getDccAddress() {
		return dccAddress;
	}

	synchronized public static Locomotive getLocomotiveByDccAddress(int dccAddress) {
		return locomotives.get(dccAddress);
	}
	
	/**
	 * Set the real speed of the locomotive in the Model. 
	 * Note that the controlling Thread of the TrainScope permanently 
	 * calls this Method. This means the new speed endures for only a very short time.
	 * If you want to permanently  set a speed Limit, you may consider
	 * the Method setSpeedLimit()
	 * 
	 * @param speed - in m/s in the Model
	 */
	synchronized void setCurrentSpeed(double speed) {
		//TODO sehr inneffizient implementiert
		//TODO falls die aktuelle DCC Geschwindigkeit die selbe ist s
		//ollte sie nicht nochmals gesendet werden um den Netzwerkverkehr zu entlasten
		for(int i = 0; i < Main.MAX_DCC_VALUES; i++) {
			if(meterPerSecond[i] >= speed) {
				//Die Variable currentDccSpeed wird durch die rückmeldung der ControlUnit aktualisiert.
				//Daher braucht man sie in dieser Methode nicht zu setzen.
				Main.controlUnit.setDccSpeed(this, i);
				return;
			}
		}
		Main.controlUnit.setDccSpeed(this, Main.MAX_DCC_VALUES -1);
		
	}
	
	/**
	 * @return the current speed in m/s in the Model
	 */
	synchronized double getCurrentSpeed() {
		return this.meterPerSecond[currentDccSpeed];
	}
	
	/**
	 * 
	 * @return the current speed Limit in m/s in the Model
	 */
	synchronized double getCurrentSpeedLimit() {
		return this.meterPerSecond[currentDccSpeedLimit];
	}

	synchronized public void setDccSpeedLimit(int dccSpeedLimit) {
		assert (dccSpeedLimit < Main.MAX_DCC_VALUES);
		assert (0 <= dccSpeedLimit);
		this.currentDccSpeedLimit = dccSpeedLimit;
	}

	/**
	 * Only the controlUnit should use this Method
	 * 
	 * @param direction
	 */
	synchronized public void updateDirectionDecoder(boolean direction) {
		this.directionDecoder = direction;
	}

	/**
	 * Only the controlUnit should use this Method
	 * 
	 * @param dccValue
	 */
	synchronized public void updateDccSpeed(int dccValue) {
		assert (0 <= dccValue);
		assert (dccValue < Main.MAX_DCC_VALUES);
		this.currentDccSpeed = dccValue;
		this.getTrainScope().setCurrentSpeedInModel(this.meterPerSecond[dccValue]);
	}

}
