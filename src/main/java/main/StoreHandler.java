package main;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

import modelling.Balise;
import modelling.Locomotive;
import modelling.TrainScope;

/**
 * This class aims to permanently store the informations of the current layout.
 * 
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 10.05.2021
 */
public class StoreHandler implements Runnable {

	private static final Set<Serializable> storeableItemsForMethodResponses = new HashSet<>();

	private static final Set<Serializable> storeableItemsBatch = new HashSet<>();
	private static final Set<Serializable> addListBatch = new HashSet<>();
	private static final Set<Serializable> deleteListBatch = new HashSet<>();
	private static boolean open = false;

	synchronized public static void open() {
		if (!open) {

			open = true;
			loadAllObjects();
			handleAddList();
			new Thread(new StoreHandler()).start();
		}

	}

	private static void writeNewEmptyList(String filename) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream("serializedObjects/" + filename + ".ser"));
			// At the Beginning store the number of Objects being stored
			oos.writeInt(0);
			oos.flush();
			oos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This Method permanently deletes an Item from the disk.
	 * 
	 * @param id
	 * @throws Exception
	 */
	synchronized public static void deleteObject(Serializable object) {
		if (!open) {
			open();
		}
		deleteListBatch.add(object);
		addListBatch.remove(object);
		storeableItemsForMethodResponses.remove(object);

	}

	/**
	 * This Method Adds an Item to be stored on the Disk
	 * 
	 * @param item
	 * @throws Exception
	 */
	synchronized public static void addObject(Serializable object) {
		if (!open) {
			open();
		}
		addListBatch.add(object);
		deleteListBatch.remove(object);
		storeableItemsForMethodResponses.add(object);
	}

	/**
	 * This Method should be called in order to stop the StoreHandler
	 */
	public synchronized static void close() {
		open = false;
	}

	synchronized public static boolean isOpen() {
		return open;
	}

	synchronized private static void handleDeleteList() {
		storeableItemsBatch.removeAll(deleteListBatch);
		deleteListBatch.clear();
	}

	synchronized private static void handleAddList() {
		storeableItemsBatch.addAll(addListBatch);
		addListBatch.clear();
	}

	private static void storeAllObjects() {
		if (storeableItemsBatch.isEmpty()) {
			writeNewEmptyList(Main.anlageID);
		} else {
			try {
				ObjectOutputStream oos = new ObjectOutputStream(
						new FileOutputStream("serializedObjects/" + Main.anlageID + ".ser"));
				// At the Beginning store the number of Objects being stored
				oos.writeInt(storeableItemsBatch.size());
				for (Serializable item : storeableItemsBatch) {
					storeProcedure: while(true) {
						try {
							oos.writeObject(item);
							break storeProcedure;
						}catch (ConcurrentModificationException e){
							try {
								Thread.sleep(20);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}	
					}
				}
				oos.flush();
				oos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	synchronized private static void loadAllObjects() {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new FileInputStream("serializedObjects/" + Main.anlageID + ".ser"));
			int numberOfObjects = ois.readInt();
			for (int i = 0; i < numberOfObjects; i++) {
				Serializable object = (Serializable) ois.readObject();
				addListBatch.add(object);
				storeableItemsForMethodResponses.add(object);
			}
			ois.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("The StoreHandler has opened this Objects:");
		for (Serializable item : addListBatch) {
			System.out.println(item.toString());
		}
		System.out.println();
	}

	synchronized static void deleteAllFromDisk() {
		if (!open) {
			open();
		}
		deleteListBatch.addAll(storeableItemsBatch);
		addListBatch.clear();
		storeableItemsForMethodResponses.clear();
	}

	synchronized static HashSet<TrainScope> getAllTrainScopes() {
		if (!open) {
			open();
		}
		HashSet<TrainScope> scopes = new HashSet<TrainScope>();
		for (Serializable object : storeableItemsForMethodResponses) {
			if (!object.toString().contains("[") && object.toString().contains("TrainScope")) {
				scopes.add((TrainScope) object);
			}
		}
		return scopes;
	}

	synchronized static public HashSet<Balise> getAllBalises() {
		if (!open) {
			open();
		}
		HashSet<Balise> balises = new HashSet<Balise>();
		for (Serializable object : storeableItemsForMethodResponses) {
			if (!object.toString().contains("[") && object.toString().contains("Balise")) {
				balises.add((Balise) object);
			}
		}
		return balises;
	}

	synchronized static public HashSet<Locomotive> getAllLocomotives() {
		if (!open) {
			open();
		}
		HashSet<Locomotive> locomotives = new HashSet<>();
		for (Serializable object : storeableItemsForMethodResponses) {
			if (!object.toString().contains("[") && object.toString().contains("Locomotive")) {
				locomotives.add((Locomotive) object);
			}
		}
		return locomotives;
	}

	@Override
	public void run() {
		System.out.println("Start StoreHandler Thread");
		System.out.println();
		Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
		do {
			handleAddList();
			handleDeleteList();
			storeAllObjects();
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (isOpen());

		// At the end store all Objects again.
		handleAddList();
		handleDeleteList();
		storeAllObjects();
		System.out.println("StoreHandler Thread terminated");
	}

}
