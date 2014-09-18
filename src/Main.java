import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;


public class Main {
	
	private static int totalCarsUsed, carSize, parkingRows, kHeight;//, verboseLevel;
	private static Spawn spawn;
	private static Crossroad crossroad;
	private static Despawn despawn;
	private static KStack kstacks[];
	public static Random mathRandom;
	private static SecureRandom secRandom;
	public static String resultName;
//	private static boolean CSVinsteadofLoop; // see config file
	private static int simulatorCase; // see config file
	private static boolean secureRandom; // see config file
	private static int randomSeed; // see config file
	
	public static int[][] csvData;
	public static int size;
	
	private static BufferedWriter writer = null;
	
	public static void main(String args[]) {
		
		Configuration config = new Configuration();
		simulatorCase = config.simulatorCase;
//		CSVinsteadofLoop = config.CSVinsteadofLoop;
		secureRandom = config.secureRandom;
		randomSeed = config.randomSeed;
		
		parkingRows = config.parkingRows;
		carSize = config.carSize;
		kHeight = config.kHeight;
		
//		verboseLevel = 0;
		
		spawn = new Spawn();
		crossroad = new Crossroad();
		despawn = new Despawn();
		
		mathRandom = new Random(randomSeed);
		secRandom.setSeed(randomSeed);
		
		// first third belongs to top lane; second third belongs to middle lane
		// last third belongs to bottom lane;
		kstacks = new KStack[parkingRows*6];
		
		
		
		// setting up the entire map
		setupMap();

		EventItem[] eventList = new EventItem[2]; // will be initialized with correct data later
		Car[] carList = new Car[2]; // will be initialized with correct data later

		
		
		if (simulatorCase == 4 || simulatorCase == 5)
			resultName = "csv-input_";
		else
			resultName = ("testCase_"+simulatorCase+"_");
		resultName += new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		
		new File("./"+resultName).mkdir();
		
		writeDownSettings();
		
		
		
		
		
		totalCarsUsed = kHeight*parkingRows*6;
		// creating the number of cars and events needed
		carList = new Car[totalCarsUsed];
		eventList = new EventItem[totalCarsUsed];
		for (int i=0; i<totalCarsUsed; i++) {
			eventList[i] = new EventItem();
			carList[i] = new Car(carSize, eventList[i], spawn, despawn, crossroad);
		}
		
		int waitTime = (int)(2000*((float)(1440)/(float)(totalCarsUsed)))*2;
		
		switch(simulatorCase) {
		case 0:
			/** BEST CASE **/
			for (int i = 0; i < totalCarsUsed; i++) {
				eventList[i].setupEvent(carList[i], 0, waitTime+100*i);
			}
			break;
			
		case 1:
			/** WORST CASE **/
			for (int i = 0; i < totalCarsUsed; i++) {
				eventList[i].setupEvent(carList[i], 0, waitTime+1000*(i%(totalCarsUsed/kHeight)));
			}
			break;
			
		case 2:
			/** RANDOM CASE **/
			System.out.println("Random Case not yet implemented.");
			return;
			
		case 3:
			/** ROUND ROBIN TEST CASE **/
			for (int i = 0; i < totalCarsUsed; i++) {
				eventList[i].setupEvent(carList[i], 0, 2500);
			}
			break;
			
		case 4:
			try{
				File results = new File("./"+resultName+"/mapping_"+resultName+".csv");
				writer = new BufferedWriter(new FileWriter(results));
				writer.write("# EntryTime,BackOrderTime\r\n");
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			csvData = new int[3][1];
			size = 0;
			CSV csv = CSV
				    .separator(',')  // delimiter of fields
				    .quote('"')      // quote character
				    .create();       // new instance is immutable
			
			csv.read("adjusted.csv", new CSVReadProc() {
			    public void procRow(int rowIndex, String... values) {
			    	size = rowIndex+1;
			    }
			});

			csvData = new int[3][size];
			csv.read(config.inputFileName, new CSVReadProc() {
			    public void procRow(int rowIndex, String... values) {
			    	csvData[0][rowIndex] = Integer.valueOf(values[0]);
			    	csvData[1][rowIndex] = Integer.valueOf(values[1]);
			    	csvData[2][rowIndex] = Integer.valueOf(values[2]);
			    }
			});
			int eventsSize = 0;
			for (int i=0; i<size; i++) {
				eventsSize += csvData[1][i];
			}
			
			totalCarsUsed = eventsSize;
			
			int events[][] = new int[2][eventsSize];
			for (int i = 0; i < eventsSize; i++) {
				events[0][i] = 0;
				events[1][i] = 0;
			}
			int eventsPos = 0;
			ListEntry list = new ListEntry();
			
			for (int i = size-1; i >= 0; i--) {
				if (csvData[1][i] != 0) {
					list = new ListEntry();
					
					
					
					for (int j = i; j < size; j++) {
						if (((csvData[0][i]+1000) <= csvData[0][j]) && (csvData[2][j] > 0)) {
							for (int k = 0; k < csvData[2][j]; k++)
								list.append(list, j);
						}
					}
					if (list.size() != 0) {
						float random = getRandomNumber(secureRandom);
						int exitIndex = list.getListEntry((int)(random*(float)(list.size()))).index;
						csvData[1][i]--;
						csvData[2][exitIndex]--;
						events[0][eventsPos] = csvData[0][i];
						events[1][eventsPos] = csvData[0][exitIndex];
						try{writer.write(events[0][eventsPos]+","+events[1][eventsPos]+"\r\n");}catch(Exception e){}
						eventsPos++;
						i++;
					}
				}
			}
			
			
			totalCarsUsed = 0;
			for (int i=0; i<eventsSize; i++) {
				if (events[1][i] != 0)
					totalCarsUsed++;
			}
			
			
			System.out.println("# EntryTime,ExitTime");
			for (int i = 0; i < totalCarsUsed; i++) {
				System.out.println(events[0][i]+","+events[1][i]);
			}
			
			
			carList = new Car[totalCarsUsed];
			eventList = new EventItem[totalCarsUsed];
			
			
			// creating the number of cars and events needed
			for (int i=0; i<totalCarsUsed; i++) {
				eventList[i] = new EventItem();
				carList[i] = new Car(carSize, eventList[i], spawn, despawn, crossroad);
				eventList[i].setupEvent(carList[i], events[0][i], events[1][i]);
			}
			break;
			
		default:
			System.out.println("Please review the settings of the simulation!");
			return;
		}
		
		
		
		
		// create an instance of the simulator itself
		Simulator simulator = new Simulator(spawn, despawn, crossroad, kstacks, eventList, kHeight, carSize, parkingRows, resultName);
		
		
		
//		int imageEveryXTicks = 5;
		
		// run the simulator
		simulator.runSimulator();
		
		// do some statistics
//		evaluateResults();
		
		
		
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
	
	
	private static void writeDownSettings() {
		try{
			File results = new File("./"+resultName+"/settings"+resultName+".txt");
			writer = new BufferedWriter(new FileWriter(results));
			writer.write("# Settings\r\n");
			writer.write("Date: "+new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+"\r\n");
			if (simulatorCase == 3 || simulatorCase == 4) {
				writer.write("Running data from a CSV file.\r\n");
			} else {
				String caseName[] = {"best case -unpark always just 1 at a time",
						"worst case - unpark all cars with the same rank at once, starting with the highest",
						"random case using poisson distribution with certain probabilities for the parking duration",
						"round robin test case"};
				writer.write("Running test data from case "+simulatorCase+" ("+caseName[simulatorCase]+").\r\n");
				
			}
			if (simulatorCase == 2 || simulatorCase == 3 || simulatorCase == 4) {
				writer.write("Mapping from incoming to outgoing cars where randomized with "+(secureRandom?"secureRandom":"Math.random")+"\r\n");
				writer.write("Seed: "+randomSeed+"\r\n");
			}
			writer.newLine();
			// Layout information
			writer.write("A total of "+(kHeight*6*parkingRows)+" parking spots are available.\r\n");
			writer.write("kHeight = "+kHeight+"\r\n");
			writer.write("parkingRows = "+parkingRows+"\r\n");
			writer.write("car length = "+carSize+"\r\n");
		} catch (Exception e) {
			e.printStackTrace();
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
			kstacks[i] = new KStack(i);
		}
		
		
		
		/**
		 * append kstacks to correct streets
		 * first 1/3 kstack is connected to top lane
		 * second 1/3 is connected to middle lane
		 * last 1/3 is connected to bottom lane
		 * 
		 * the first stack is also closest to the exit
		 */ 
		
		// mid lane
		tempStreet1 = spawn.next1;
		for (int i=parkingRows-1; i>=0; i--) {
			tempStreet1.kstack1 = kstacks[2*i  ];
			tempStreet1.kstack2 = kstacks[2*i+1];
			kstacks[2*i  ].lane = spawn.next1;
			kstacks[2*i+1].lane = spawn.next1;
			tempStreet1.kstack1.prev1 = tempStreet1;
			tempStreet1.kstack2.prev1 = tempStreet1;
			tempStreet1 = tempStreet1.next1;
		}
		
		// top lane
		tempStreet1 = spawn.next2;
		for (int i=0; i<2*kHeight*carSize+1; i++) {
			tempStreet1 = tempStreet1.next1;
		}
		for (int i=parkingRows-1; i>=0; i--) {
			tempStreet1.kstack1 = kstacks[2*i   +2*parkingRows];
			tempStreet1.kstack2 = kstacks[2*i+1 +2*parkingRows];
			kstacks[2*i   +2*parkingRows].lane = spawn.next2;
			kstacks[2*i+1 +2*parkingRows].lane = spawn.next2;
			tempStreet1.kstack1.prev1 = tempStreet1;
			tempStreet1.kstack2.prev1 = tempStreet1;
			tempStreet1 = tempStreet1.next1;
		}
		
		// bottom lane
		tempStreet1 = spawn.next3;
		for (int i=0; i<2*kHeight*carSize+1; i++) {
			tempStreet1 = tempStreet1.next1;
		}
		for (int i=parkingRows-1; i>=0; i--) {
			tempStreet1.kstack1 = kstacks[2*i   +4*parkingRows];
			tempStreet1.kstack2 = kstacks[2*i+1 +4*parkingRows];
			kstacks[2*i   +4*parkingRows].lane = spawn.next3;
			kstacks[2*i+1 +4*parkingRows].lane = spawn.next3;
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
	
	
	private static float getRandomNumber(boolean secureRandom) {
		if (!secureRandom)
			return (float)(mathRandom.nextFloat());
		mathRandom = new SecureRandom();
		int test = Math.abs(mathRandom.nextInt());
		float test2 = (float)(test)/(float)(Integer.MAX_VALUE);
		return test2;
	}
}
