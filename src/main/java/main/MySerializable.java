package main;

import java.io.Serializable;

/**
 * All Objects that have to be stored should implement this Interface.
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 14.05.2021
 */
public interface MySerializable extends Serializable{
	
	/**
	 * This Method must be called if an Serializable Object is not used any more.
	 * If the Programmer don't call this method, the Object will Remain on disk forever.
	 */
	default public void delete() throws Exception {
		StoreHandler.deleteObject(this);
	}

}
