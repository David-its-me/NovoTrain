package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import modelling.BlockPoint;
import modelling.BlockPointProperties;
import modelling.Locomotive;
import modelling.Node;
import modelling.Position;
import modelling.Track;
import modelling.TrainScope;
import sensorsAktuators.ControlUnit;
import sensorsAktuators.Z21;

public class Main {

	static final String modelRailwaysLocation = "ModelRailways.ebs";
	static String anlageID;
	static String anlageName;
	public static volatile int MAX_DCC_VALUES;
	public static int S21_PORT;
	public static byte[] S21_IP4;
	/**
	 * TOLERANCE_DISTANCE in meters in the model
	 */
	public static volatile double TOLERANCE_DISTANCE;
	/**
	 * EPSILON Distance in meters in the model
	 */
	public static volatile double EPSILON = 0.0000001;
	public static volatile int SCALE = 160;
	public static ControlUnit controlUnit;

	public static void main(String[] args) throws Exception {
		
		//Load Models
		String railwayID = ModelRailwaySelector.openModelRailway();
		// Load and initialize Model
		loadModelRailwayFields(railwayID);
		controlUnit = new Z21();
		StoreHandler.open();
		//TODO der folgende Befehl muss hier wieder weg.
		StoreHandler.deleteAllFromDisk();

		try {
			// Start all Computational Threads
			for (TrainScope scope : StoreHandler.getAllTrainScopes()) {
				scope.startSpeedControlThread();
			}
			
			Locomotive br185;
			for(Locomotive locomotive : StoreHandler.getAllLocomotives()) {
				br185 = locomotive;
			}
			testModel();
			
			// Start UI
			//Console.openConsole();
			Thread.sleep(70000);

			// Stop all Computational Threads
			for (TrainScope scope : StoreHandler.getAllTrainScopes()) {
				scope.closeSpeedControlThread();
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Stop all Computational Threads
			controlUnit.close();
			StoreHandler.close();
		}

	}
	
	private static void testModel() throws Exception {
		
		System.out.println();
		System.out.println("Testing Modell");
		System.out.println();
		
		Node n1 = new Node();
		Node n2 = new Node();
		Node n3 = new Node();
		Node n4 = new Node();
		Node n5 = new Node();

		
		Track t1 = new Track(n1, n2, 1,50);
		Track t2 = new Track(n2, n3, 1,50);
		Track t3 = new Track(n3, n4, 1,60);
		Track t4 = new Track(n4, n5, 1,50);
		Track t5 = new Track(n5, n1, 1,50);
		
		BlockPoint block = new BlockPoint(BlockPointProperties.BLOCK_NEGATIVE_DIRECTION, new Position(t2.getCurrentTrackEdge(), 0.5));
		
		Locomotive br185 = new Locomotive("br185", 0.1, 160, new Position(t1.getCurrentTrackEdge(), 0.5), 185);
		
		/*
		br185.getTrainScope().closeSpeedControlThread();
		br185.delete();
		doppelstock.delete();
		balise1.delete();
		
		t1.delete();
		t2.delete();
		t3.delete();
		t4.delete();
		t5.delete();
		s1.delete();
		b1.delete();
		
		n1.delete();
		n2.delete();
		n3.delete();
		n4.delete();
		n5.delete();
		n6.delete();
		n7.delete();
		*/
		
	}

	private static void loadModelRailwayFields(String modelID) throws Exception {
		String[] attributes = getLineByID(modelRailwaysLocation, modelID);
		anlageID = attributes[0];
		assert(anlageID.equals(modelID));
		anlageName = attributes[1];
		MAX_DCC_VALUES = Integer.parseInt(attributes[2]);
		S21_PORT = Integer.parseInt(attributes[3]);
		S21_IP4 = new byte[4];
		String[] ip = attributes[4].split(":");
		for (int i = 0; i < 4; i++) {
			S21_IP4[i] = (byte) Integer.parseInt(ip[i]);
		}
		SCALE = Integer.parseInt(attributes[5]);
		TOLERANCE_DISTANCE = Double.parseDouble(attributes[6]);

	}

	private static String[] getLineByID(String file, String id) throws Exception {
		for (String[] line : getLines(file)) {
			if (line[0].equals(id)) {
				return line;
			}
		}
		throw new Exception("ID " + id + " not found");
	}

	static List<String[]> getLines(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		List<String[]> lines = new LinkedList<>();
		reader.readLine();
		String line = reader.readLine();
		while (line != null) {
			lines.add(line.split(","));
			line = reader.readLine();
		}
		reader.close();
		return lines;
	}
}
