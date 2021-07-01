package main;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

class ModelRailwaySelector {

	/**
	 * This Method opens the Console of the Program. 
	 * Note that this Method will block the current thread.
	 * 
	 * @return the ModelID of the selected Model
	 * @throws IOException 
	 */
	static String open() throws IOException {
		//TODO
		return null;
	}
		
	
	private static void newModelRailway() {
		//TODO
	}
	
	static String openModelRailway() throws IOException {
		System.out.println("Please select one of the ModelRailways");
		List<String[]> lines = Main.getLines(Main.modelRailwaysLocation);
		Set<Integer> ids = new HashSet<>();
		for(String[] current : lines) {
			System.out.println("[" + current[0].split("#")[1] + "] - " + current[1]);
			ids.add(Integer.parseInt(current[0].split("#")[1]));
		}
		System.out.println();
		
		Scanner scanner = new Scanner(System.in);
		
		int modelID;
		
		programm: while(true) {
			String in = "";
			try {
				in = scanner.nextLine();
				modelID = Integer.parseInt(in);
				if(ids.contains(modelID)) {
					break programm;
				}
				System.out.println("Sorry '" + in + "' can't be found");
			}catch (Exception e) {
				System.out.println("Sorry '" + in + "' is not a valid input");
			}
		}
		
		scanner.close();
		
		return "ModelRailway#" + modelID;	
	
	}
	
}
