package sensorsAktuators;

import modelling.Locomotive;
import modelling.Switch;

public interface ControlUnit {
	
	
	/**
	 * With this command the track voltage is switched off
	 */
	public void setTrackPowerOff();
	
	/**
	 * With this command the track voltage is switched on
	 */
	public void setTrackPowerOn();
	
	/**
	 * With this command the emergency stop is activated, i.e. the locomotives are stopped but the 
	 * track voltage remains switched on.
	 */
	public void stop();
	
	/**
	 * This Method sets the Speed of a locomotive
	 * @param locomotive
	 * @param dccSpeed
	 */
	public void setDccSpeed(Locomotive locomotive, int dccSpeed);
	
	public void setLightsOn(Locomotive locomotive);
	
	public void setLightsOff(Locomotive locomotive);
	
	/**
	 * With the following command a single function of a locomotive decoder can be switched.
	 * @param locomotive
	 * @param function
	 */
	public void setLocoFunction(Locomotive locomotive, byte functionIndex, FunctionSwitchingType switchType);
	
	public boolean isLightOn(Locomotive locomotive);
	
	/**
	 * @param locomotive
	 * @return the current dcc drive level.
	 */
	public int getDccSpeed(Locomotive locomotive);
	
	/**
	 * 
	 * @param locomotive
	 * @return the current direction of the Decoder
	 */
	public boolean getDirection(Locomotive locomotive);
	
	public boolean getSwitchState(Switch switch_);
	
	public void setSwitch(Switch switch_, boolean SwitchState);
	
	public void close();
	
	
	
	
	
	
	
	
	
}
