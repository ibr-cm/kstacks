import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;


public class Main {
	
	private static int totalCarsUsed, entryTime[], exitTime[], carSize, parkingRows, kHeight, verboseLevel;
	private static Spawn spawn;
	private static Crossroad crossroad;
	private static Despawn despawn;
	private static KStack kstacks[];
	
	public static void main(String args[]) {
		
		parkingRows = 2;
		carSize = 3;
		kHeight = 3;
		
		verboseLevel = 1;
		
		spawn = new Spawn();
		crossroad = new Crossroad();
		despawn = new Despawn();
		
		// first third belongs to top lane; second third belongs to middle lane
		// last third belongs to bottom lane;
		kstacks = new KStack[parkingRows*6]; 
		
		
		// setting up the entire map
		setupMap();

		
		
		
		// instance of the csv parser
		CSV csv = CSV
			    .separator(',')  // delimiter of fields
			    .quote('"')      // quote character
			    .create();       // new instance is immutable
		
		// evaluate the number of lines inside the file to calculate the number
		// of Cars and EventItem needed for the simulation
		csv.read("test.csv", new CSVReadProc() {
		    public void procRow(int rowIndex, String... values) {
		    	totalCarsUsed = rowIndex+1;
		    }
		});
		
		// create to arrays with the values of every car
		entryTime = new int[totalCarsUsed];
		exitTime = new int[totalCarsUsed];
		
		// fill the arrays with data concerning the entry and exit tick
		csv.read("test.csv", new CSVReadProc() {
		    public void procRow(int rowIndex, String... values) {
		        entryTime[rowIndex] = Integer.valueOf(values[0]);
		        exitTime[rowIndex] = Integer.valueOf(values[1]);
//		        System.out.println(values[1]);
		    }
		});
		
		
		Car[] carList = new Car[totalCarsUsed];
		EventItem[] eventList = new EventItem[totalCarsUsed];
		
		// creating the number of cars and events needed
		for (int i=0; i<totalCarsUsed; i++) {
			eventList[i] = new EventItem();
			carList[i] = new Car(carSize, eventList[i], spawn, despawn, crossroad, verboseLevel);
		}
		
		// fill data into the events
		for (int i = 0; i < totalCarsUsed; i++) {
			eventList[i].setupEvent(carList[i], entryTime[i], exitTime[i]);
		}
		
		// create an instance of the simulator itself
		Simulator simulator = new Simulator(spawn, despawn, crossroad, kstacks, carList, eventList, totalCarsUsed, kHeight, carSize, parkingRows);
		
		
		// run the simulator
		simulator.runSimulator(0, false, true, verboseLevel);
		
		// do some statistics
		evaluateResults();
		
		
		
	}
	
	public static void printLayout(Spawn spawn, Despawn despawn, Crossroad crossroad) {
		Street tempStreet2 = new Street();
		System.out.println("lane before spawn:");

		System.out.println();

		
		
		
		
		
		
		
		
		
		
		
		
		
		// print out the streets before the spawn
		tempStreet2 = spawn;
		System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		while (tempStreet2.prev1 != null) {
			tempStreet2 = tempStreet2.prev1;
			System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		}
		
		System.out.println();
		System.out.println();
		System.out.println();
		
		System.out.println("lane behind crossroads:");

		System.out.println();
		
		tempStreet2 = crossroad;
		System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		while (tempStreet2.next1 != null) {
			tempStreet2 = tempStreet2.next1;
			System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		}
		
		System.out.println();
		System.out.println();
		System.out.println();
		
		System.out.println("middle lane:");

		System.out.println();
		
		// print middle lane
		tempStreet2 = spawn;
		System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		tempStreet2 = ((Spawn)(tempStreet2)).next1;
		System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		while (tempStreet2.next1 != null ) {//&& tempStreet2 != crossroad) {
			tempStreet2 = tempStreet2.next1;
			System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		}
		
		System.out.println();
		System.out.println();
		System.out.println();
		
		System.out.println("top lane:");

		System.out.println();
		
		// print top lane
		tempStreet2 = spawn;
		System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		tempStreet2 = ((Spawn)(tempStreet2)).next2;
		System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		while (tempStreet2.next1 != null && tempStreet2 != crossroad) {
			tempStreet2 = tempStreet2.next1;
			System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		}
		
		System.out.println();
		System.out.println();
		System.out.println();
		
		System.out.println("bottom lane:");

		System.out.println();
		
		// print bottom lane
		tempStreet2 = spawn;
		System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		tempStreet2 = ((Spawn)(tempStreet2)).next3;
		System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		while (tempStreet2.next1 != null && tempStreet2 != crossroad) {
			tempStreet2 = tempStreet2.next1;
			System.out.println(tempStreet2+"  kstack1: "+tempStreet2.kstack1+"  kstack2: "+tempStreet2.kstack2);
		}
	}
	
	public static void printKStacks(KStack[] kstacks, int carSize, int kHeight) {
		for (int i=0; i<kstacks.length; i++) {
			System.out.println(i);
			Street tempStreet1 = kstacks[i];
			System.out.println(tempStreet1.prev1);
			System.out.println(tempStreet1);
			for (int j=0; j<carSize*kHeight-1; j++) {
				tempStreet1 = tempStreet1.next1;
				System.out.println(tempStreet1);
			}
			System.out.println();
			System.out.println();
		}
	}
	
	
	private static void setupMap() {
		Street tempStreet1 = new Street(), tempStreet2 = new Street(); // piece of street to work with
		// pieces of street before spawn to drive back from kstacks close to spawn if necessary;	
		if (carSize*kHeight>1) {
			tempStreet2 = spawn;
			for (int i=0; i<carSize*kHeight; i++) {
				tempStreet1 = new Street();
				tempStreet1.next1 = tempStreet2;
				tempStreet2.prev1 = tempStreet1;
				tempStreet2 = tempStreet1;
			}
		}
		
		// pieces of street between crossroads and despawn so a car fits in there
		if (carSize > 1) {
			tempStreet1 = crossroad;
			for (int i=0; i<carSize-1; i++) {
				tempStreet2 = new Street();
				tempStreet2.prev1 = tempStreet1;
				tempStreet1.next1 = tempStreet2;
				tempStreet1 = tempStreet2;
			}
			tempStreet2.next1 = despawn;
			despawn.prev1 = tempStreet2; 
		} else {
			despawn.prev1 = crossroad;
			crossroad.next1 = despawn;
		}
		
		// create middle lane
		tempStreet1 = new Street();
		tempStreet2 = new Street();
		tempStreet1.prev1 = spawn;
		spawn.next1 = tempStreet1;
		if (parkingRows>1) {
			for (int i=0; i<parkingRows-1; i++) {
				tempStreet2 = new Street();
				tempStreet2.prev1 = tempStreet1;
				tempStreet1.next1 = tempStreet2;
				tempStreet1 = tempStreet2;
			}
		} else {
			tempStreet2 = tempStreet1;
		}
		crossroad.prev1 = tempStreet2;
		tempStreet2.next1 = crossroad;
				
		// create the top lane
		tempStreet1 = new Street();
		tempStreet2 = new Street();
		tempStreet1.prev1 = spawn;
		spawn.next2 = tempStreet1;
		for (int i=0; i<2*kHeight*carSize+parkingRows+2*kHeight*carSize+1;i++) {
			tempStreet2 = new Street();
			tempStreet2.prev1 = tempStreet1;
			tempStreet1.next1 = tempStreet2;
			tempStreet1 = tempStreet2;
		}
		crossroad.prev2 = tempStreet2;
		tempStreet2.next1 = crossroad;
		
		// create bottom lane
		tempStreet1 = new Street();
		tempStreet1.prev1 = spawn;
		spawn.next3 = tempStreet1;
		for (int i=0; i<2*kHeight*carSize+parkingRows+2*kHeight*carSize+1;i++) {
			tempStreet2 = new Street();
			tempStreet2.prev1 = tempStreet1;
			tempStreet1.next1 = tempStreet2;
			tempStreet1 = tempStreet2;
		}
		crossroad.prev3 = tempStreet2;
		tempStreet2.next1 = crossroad;
		
		// create kstacks
		for (int i=0; i<parkingRows*6; i++) {
			kstacks[i] = new KStack();
		}
		
		// append kstacks to correct streets
		// first 1/3 kstack is connected to top lane
		// second 1/3 is connected to middle lane
		// last 1/3 is connected to bottom lane
		
		// mid lane
		tempStreet1 = spawn.next1;
		for (int i=0; i<parkingRows; i++) {
			tempStreet1.kstack1 = kstacks[2*i  ];
			tempStreet1.kstack2 = kstacks[2*i+1];
			tempStreet1.kstack1.prev1 = tempStreet1;
			tempStreet1.kstack2.prev1 = tempStreet1;
			tempStreet1 = tempStreet1.next1;
		}
		
		// top lane
		tempStreet1 = spawn.next2;
		for (int i=0; i<2*kHeight*carSize+1; i++) {
			tempStreet1 = tempStreet1.next1;
		}
		for (int i=0; i<parkingRows; i++) {
			tempStreet1.kstack1 = kstacks[2*i   +2*parkingRows];
			tempStreet1.kstack2 = kstacks[2*i+1 +2*parkingRows];
			tempStreet1.kstack1.prev1 = tempStreet1;
			tempStreet1.kstack2.prev1 = tempStreet1;
			tempStreet1 = tempStreet1.next1;
		}
		
		// bottom lane
		tempStreet1 = spawn.next3;
		for (int i=0; i<2*kHeight*carSize+1; i++) {
			tempStreet1 = tempStreet1.next1;
		}
		for (int i=0; i<parkingRows; i++) {
			tempStreet1.kstack1 = kstacks[2*i   +4*parkingRows];
			tempStreet1.kstack2 = kstacks[2*i+1 +4*parkingRows];
			tempStreet1.kstack1.prev1 = tempStreet1;
			tempStreet1.kstack2.prev1 = tempStreet1;
			tempStreet1 = tempStreet1.next1;
		}
		
		// create parkingspots behind the kstacks
		for (int i=0; i<parkingRows*6; i++) {
			tempStreet1 = kstacks[i];
			for (int j=0; j<kHeight*carSize-1; j++) {
				tempStreet2 = new Street();
				tempStreet1.next1 = tempStreet2;
				tempStreet2.prev1 = tempStreet1;
				tempStreet1 = tempStreet2;
			}
		}
	}
	
	private static void evaluateResults() {
		; // TODO
	}
}
