import java.security.SecureRandom;
import java.util.Random;

import cern.jet.random.Binomial;
import cern.jet.random.Exponential;
import cern.jet.random.NegativeBinomial;
import cern.jet.random.Poisson;
import cern.jet.random.engine.RandomEngine;
import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVReadProc;


public class Main {
	
	private static int totalCarsUsed, carSize, parkingRows, kHeight;//, verboseLevel;
	private static Spawn spawn;
	private static Crossroad crossroad;
	private static Despawn despawn;
	private static KStack kstacks[];
//	public static Random mathRandom;
//	private static SecureRandom secRandom;
	private static Configuration config;
	private static Exponential exponential;
	
	public static int[][] csvData;
	public static int size;
	
	
	
	
//	private static SecureRandom secRandom2 = new SecureRandom();
	
	
	public static void main(String args[]) {
		
		long timeTotal = System.currentTimeMillis();
		
		config = new Configuration();
		
		config.output.writeDownSettings();
		
		parkingRows = config.parkingRows;
		carSize = config.carSize;
		kHeight = config.kHeight;
		
//		verboseLevel = 0;
		
		spawn = new Spawn();
		crossroad = new Crossroad();
		despawn = new Despawn();
		
//		mathRandom = new Random(config.randomSeed);
//		config.secRandom = new SecureRandom();
		config.secRandom.setSeed(config.randomSeed);
		
		// first third belongs to top lane; second third belongs to middle lane
		// last third belongs to bottom lane;
		kstacks = new KStack[parkingRows*6];
		
		
		
		// setting up the entire map
		long timeMeasure = System.currentTimeMillis();
		setupMap();
		timeMeasure = System.currentTimeMillis() - timeMeasure;
		config.output.writeToDebugFile("####################\r\n# Setting up map "+((int)(timeMeasure/60000))+" min "+(((int)(timeMeasure/1000))%60)+" sek.\r\n####################");

		
		
		
		EventItem[] eventList = new EventItem[2]; // will be initialized with correct data later
		Car[] carList = new Car[2]; // will be initialized with correct data later

		
		
		
		
		
		
		
		
		timeMeasure = System.currentTimeMillis();
		
		totalCarsUsed = kHeight*parkingRows*6;
		// creating the number of cars and events needed
		carList = new Car[totalCarsUsed];
		eventList = new EventItem[totalCarsUsed];
		for (int i=0; i<totalCarsUsed; i++) {
			eventList[i] = new EventItem();
			carList[i] = new Car(carSize, eventList[i], spawn, despawn, crossroad, config);
		}
		
		int waitTime = (int)(2000*((float)(1440)/(float)(totalCarsUsed)))*2;
		
		switch(config.simulatorCase) {
		case 0:
			/** BEST CASE **/
			for (int i = 0; i < totalCarsUsed; i++) {
				eventList[i].setupEvent(carList[i], 0, 3000+waitTime+(50*1440)-50*i);
			}
			break;
			
		case 1:
			/** WORST CASE **/
			for (int i = 0; i < totalCarsUsed; i++) {
				eventList[i].setupEvent(carList[i], 0, waitTime+5000*(i/(totalCarsUsed/kHeight)));
			}
			break;
			
		case 2:
			
			/** RANDOM CASE **/
			double lambdaSpawn = 3.5;
			double[][] lutDep = {{0.96,3}, {0.95,3}, {0.93,4}, {0.9,6}, {0.87,6}, {0.98,6}, {0.89,6}, {0.9,5}, {0.94,4}, {0.98,4}};
			int[][] matching = new int[0][2];
			
			while(!isEnoughCars(matching)) {
				if((matching.length%1000)==0)
					System.out.println(matching.length);
				int arrivalTime = nextArrivalTime(); 
//				System.out.println("arrivalTime: "+arrivalTime);
				matching = addANewCar(matching, arrivalTime, nextParkDuration((int)(arrivalTime/4000)));
			}
			
			for (int i = 0; i < matching.length; i++) { 
				config.output.writeDemoFile("random_demo_data.csv",matching[i][0]+","+matching[i][1]+"\r\n");
			}
			
			
			
			System.out.println(matching.length);
			
			System.exit(0);
			
		case 3:
			/** ROUND ROBIN TEST CASE **/
			for (int i = 0; i < totalCarsUsed; i++) {
				eventList[i].setupEvent(carList[i], 0, 2500);
			}
			break;
			
		case 4:
			/** REALISTIC TEST CASE **/
			csvData = new int[3][1];
			size = 0;
			CSV csv = CSV
				    .separator(',')  // delimiter of fields
				    .quote('"')      // quote character
				    .create();       // new instance is immutable
			
			// Read the file and count the lines
			csv.read(config.inputFileName, new CSVReadProc() {
			    public void procRow(int rowIndex, String... values) {
			    	size = rowIndex+1;
			    }
			});

			// create new array to transcribe csv to array
			csvData = new int[size][3];
			
			// transcribe data to array
			csv.read(config.inputFileName, new CSVReadProc() {
			    public void procRow(int rowIndex, String... values) {
			    	csvData[rowIndex][0] = Integer.valueOf(values[0]); // tick
			    	csvData[rowIndex][1] = Integer.valueOf(values[1]); // entries
			    	csvData[rowIndex][2] = Integer.valueOf(values[2]); // exits
			    }
			});
			
			// count incoming cars
			int eventsSize = 0;
			for (int i=0; i<size; i++) {
				eventsSize += csvData[i][1];
			}

			
			// set correct number of cars used
			totalCarsUsed = eventsSize;
			
			
//			int highestValue = 0, currentValue = 0;
//			for (int i = 0; i < csvData[0].length; i++) {
//				currentValue += csvData[1][i];
//				highestValue = Math.max(highestValue, currentValue);
//				currentValue -= csvData[2][i];
//			}
//			System.out.println("current: "+currentValue+", max value: "+highestValue);
//			System.exit(0);
			
			
			// create new list of events, which will be formed to eventItems
			int events[][] = new int[eventsSize][2];
			for (int i = 0; i < eventsSize; i++) {
				events[i][0] = 0;
				events[i][1] = 0;
			}
			
			// set counter that iterates throught the events-array
			int eventsPointer = 0;
			
			// new ListEntry for the list of possible entries
			ListEntry list = new ListEntry();
			
			for (int i = size-1; i >= 0; i--) {
				// if there is an exit assign at this time
				if (csvData[i][2] != 0) {
					// create new list
					list = new ListEntry();
					
					
					// look for an entry that is at least 15mins earlier
					for (int j = i; j >= 0; j--) {
						// did it enter +15min erlier?
						if ((csvData[j][0] <= (csvData[i][0]-1000)) && (csvData[j][1] > 0)) { //(csvData[j][0] >= (csvData[i][0]-12000)) && 
							
							// create as much list entries as needed
//							for (int k = 0; k < csvData[j][1]; k++)
								list.append(list, j);
						}
					}
					
//					System.out.println("i: "+i);
//					list.print(list);
//					System.exit(0);
					
					// are there any entries, that could work
					if (list.size() != 0) {
						// find a random one
						int enterIndex = list.getListEntry((int)(config.secRandom.nextFloat()*(float)(list.size()))).index;
						// reduce the exits by 1
						csvData[i][2]--;
						// reduce the entries by 1
						csvData[enterIndex][1]--;
						// make correct entry in the events list
						events[eventsPointer][0] = csvData[enterIndex][0]+(int)(config.secRandom.nextFloat()*66.0);
						events[eventsPointer][1] = csvData[i][0]+(int)(config.secRandom.nextFloat()*66.0);
						// print into the mapping file
						config.output.writeToMappingFile(events[eventsPointer][0]+","+events[eventsPointer][1]);
						eventsPointer++;
						i++;
					}
				}
			}
			
			int tenK=0;
			// map every car that did not exit the parking lot by now to exit 100000
			for (int i = 0; i < size; i++) {
				// if there is an entry
				if (csvData[i][1] != 0) {
//					System.out.println(i);
					tenK++;
					// map it to exit = 100k
					events[eventsPointer][0] = csvData[i][0]+(int)(config.secRandom.nextFloat()*66.0);
					events[eventsPointer][1] = 100000;
					config.output.writeToMappingFile(events[eventsPointer][0]+","+events[eventsPointer][1]);
					// increase pointer
					eventsPointer++;
					// reduce array cell in csvData
					csvData[i][1]--;
					// work on same cell again
					i--;
				}
			}
			System.out.println("cars with backOrderTime = 10k: "+tenK);
//			System.exit(0);
			
			// check how many cars there will actually be
			totalCarsUsed = 0;
			for (int i=0; i<events.length; i++) {
				// no car will exit at 0, so this is a pretty good indicator
				if (events[i][1] != 0)
					totalCarsUsed++;
			}
			
			System.out.println("totalCarsUsed: "+totalCarsUsed);
			
			// print out if verboseLevel permits
			if (config.verboseLevel > 0) {
				System.out.println("# EntryTime,ExitTime");
				for (int i = 0; i < totalCarsUsed; i++) {
					System.out.println(events[0][i]+","+events[1][i]);
				}
			}
			
			// creation of the correct number of items 
			carList = new Car[totalCarsUsed];
			eventList = new EventItem[totalCarsUsed];
			
			
			// creating the number of cars and events needed
			for (int i=0; i<totalCarsUsed; i++) {
				eventList[i] = new EventItem();
				carList[i] = new Car(carSize, eventList[i], spawn, despawn, crossroad, config);
				eventList[i].setupEvent(carList[i], events[i][0], events[i][1]);
//				System.out.println(events[0][i]+","+events[1][i]);
			}
			
			int[] testArray = new int[100001];
			for (int i = 0; i < testArray.length; i++)
				testArray[i] = 0;
			
			int highestValue2 = 0;
			for (int i = 0; i < eventList.length; i++) {
				System.out.println(eventList[i].getEntryTime()+","+eventList[i].getBackOrderTime());
				for (int j = eventList[i].getEntryTime(); j < eventList[i].getBackOrderTime(); j++)
					testArray[j]++;
			}
			for (int i = 0; i < testArray.length; i++) {
				highestValue2 = Math.max(highestValue2, testArray[i]);
			}
			System.out.println("max value: "+highestValue2);
			
			System.exit(0);
			break;
			
		case 5:
			
			size = 0;
			
			String fileName = "random_data.csv";
			
			CSV csv2 = CSV
		    .separator(',')  // delimiter of fields
		    .quote('"')      // quote character
		    .create();       // new instance is immutable
			
			csv2.read(fileName, new CSVReadProc() {
			    public void procRow(int rowIndex, String... values) {
			    	size = rowIndex+1;
			    }
			});
			
			carList = new Car[size];
			eventList = new EventItem[size];

			csvData = new int[2][size];
			csv2.read(fileName, new CSVReadProc() {
			    public void procRow(int rowIndex, String... values) {
			    	csvData[0][rowIndex] = Integer.valueOf(values[0]);
			    	csvData[1][rowIndex] = Integer.valueOf(values[1]);
			    }
			});
			
			for (int i = 0; i < size; i++) {
				eventList[i] = new EventItem();
				carList[i] = new Car(carSize, eventList[i], spawn, despawn, crossroad, config);
				eventList[i].setupEvent(carList[i], csvData[0][i], csvData[1][i]);
//				System.out.println(csvData[0][i]+", "+csvData[1][i]);
			}
			break;
			
		default:
			System.out.println("Please review the settings of the simulation in the config file!");
			return;
		}
		
		timeMeasure = System.currentTimeMillis() - timeMeasure;
		config.output.writeToDebugFile("# Setting up eventItems "+((int)(timeMeasure/60000))+" min "+(((int)(timeMeasure/1000))%60)+" sek.\r\n####################\r\n# Starting simulator!\r\n####################");
		
		
		
		// create an instance of the simulator itself
		Simulator simulator = new Simulator(spawn, despawn, crossroad, kstacks, eventList, kHeight, carSize, parkingRows, config);
		
		
		
		// run the simulator
		timeMeasure = System.currentTimeMillis();
		simulator.runSimulator();
		timeMeasure = System.currentTimeMillis() - timeMeasure;
		config.output.writeToDebugFile("####################\r\n# Simulation time "+((int)(timeMeasure/60000))+" min "+(((int)(timeMeasure/1000))%60)+" sek.\r\n####################");
		
		timeTotal = System.currentTimeMillis() - timeTotal;
		config.output.writeToDebugFile("# Total used time "+((int)(timeTotal/60000))+" min "+(((int)(timeTotal/1000))%60)+" sek.\r\n####################");
		
		
		
	}
	
	private static int[][] addANewCar(int[][] matching, int arrivalTime, int parkingDuration) {
		int [][] returnArray = new int[matching.length+1][2];
		if (matching.length > 0) {
			for (int i = 0; i < matching.length; i++) {
				returnArray[i][0] = matching[i][0];
				returnArray[i][1] = matching[i][1];
			}
		}
		
		returnArray[matching.length][0] = arrivalTime;
		returnArray[matching.length][1] = arrivalTime+parkingDuration;
		return returnArray;
	}
	
	private static boolean isEnoughCars(int[][] matching) {
		if (matching.length < config.noOfParkingSpaces)
			return false;
		// check how large the array has to be
		int sizeOfTestArray = 0;
		for (int i = 0; i < matching.length; i++) {
			if (sizeOfTestArray < matching[i][1])
				sizeOfTestArray = matching[i][1];
		}
		
		// create new array with correct size
		int[] testArray = new int[sizeOfTestArray];
		for (int i = 0; i < matching.length; i++) {
			for (int j = matching[i][0]; j < matching[i][1]; j++) {
				testArray[j]++;
				// return true if there are enough cars in the parking lot
				if (testArray[j] == config.noOfParkingSpaces)
					return true;
			}
		}
		
		return false;
	}
	
	private static int nextArrivalTime() {
		Poisson pois = new Poisson(3.5, new RandomEngine() {
			
			@Override
			public int nextInt() {
//				if (config.secureRandom)
					return config.secRandom.nextInt();
//				return (int)(((Math.random()*2)-1)*Integer.MAX_VALUE);
			}
		});
		return (Math.min(pois.nextInt(),9)*4000+(int)((config.secRandom.nextDouble()*4000)+0.5));
	}
	
	
	private static int nextParkDuration(int time) {
		// if the minimum parking duration is set to 1 hour you need no random
		if (config.minParkDuration >= 60)
			return 4000;
		time = Math.min(time, 9);
		time = Math.max(time, 0);
		double[][] lutDep = {{0.96,3}, {0.95,3}, {0.93,4}, {0.9,6}, {0.87,6}, {0.98,6}, {0.89,6}, {0.9,5}, {0.94,4}, {0.98,4}};
		Binomial bin = new Binomial((int)(lutDep[time][1]), lutDep[time][0], new RandomEngine() {
			public int nextInt() {
//				if (config.secureRandom)
					return config.secRandom.nextInt();
//				return (int)(((Math.random()*2)-1)*Integer.MAX_VALUE);
			}
		});
		
		double rand = 0;
//		if (config.secureRandom)
			rand = config.secRandom.nextDouble();
//		else
//			rand = Math.random();
//		System.out.println("rand: "+rand);
		int nextRandom = ((int)(lutDep[time][1])-bin.nextInt());
//		System.out.println("next random "+nextRandom);
		int parkDuration = nextRandom*4000+(int)((rand*4000)+0.5);
//		System.out.println("parkDuration: "+parkDuration);
//		System.out.println("ParkDuration: "+((config.minParkDuration*4000)/60));
//		System.exit(0);
		if (parkDuration < ((config.minParkDuration*4000)/60))
			return nextParkDuration(time);
		
		return parkDuration;
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
		
		
		// three temporary streets are necessary to assign the kstacks
		tempStreet1 = spawn.next1;
		tempStreet2 = spawn.next2;
		Street tempStreet3 = spawn.next3;
		
		switch(config.parkingLotLayout) {
		
		case 0:
			/**
			 * append kstacks to correct streets
			 * first 1/3 kstack is connected to top lane
			 * second 1/3 is connected to middle lane
			 * last 1/3 is connected to bottom lane
			 * 
			 * the first stack is also closest to the entry
			 */ 			
			for (int i=0; i<2*kHeight*carSize+1; i++) {
				tempStreet2 = tempStreet2.next1;
				tempStreet3 = tempStreet3.next1;
			}
			for (int i=0; i<config.parkingRows; i++) {
				tempStreet1.kstack1 = kstacks[2*i  ];
				tempStreet1.kstack2 = kstacks[2*i+1];
				kstacks[2*i  ].lane = spawn.next1;
				kstacks[2*i+1].lane = spawn.next1;
				tempStreet1.kstack1.prev1 = tempStreet1;
				tempStreet1.kstack2.prev1 = tempStreet1;
				tempStreet1 = tempStreet1.next1;
				
				tempStreet2.kstack1 = kstacks[2*i   +2*parkingRows];
				tempStreet2.kstack2 = kstacks[2*i+1 +2*parkingRows];
				kstacks[2*i   +2*parkingRows].lane = spawn.next2;
				kstacks[2*i+1 +2*parkingRows].lane = spawn.next2;
				tempStreet2.kstack1.prev1 = tempStreet2;
				tempStreet2.kstack2.prev1 = tempStreet2;
				tempStreet2 = tempStreet2.next1;
				
				tempStreet3.kstack1 = kstacks[2*i   +4*parkingRows];
				tempStreet3.kstack2 = kstacks[2*i+1 +4*parkingRows];
				kstacks[2*i   +4*parkingRows].lane = spawn.next3;
				kstacks[2*i+1 +4*parkingRows].lane = spawn.next3;
				tempStreet3.kstack1.prev1 = tempStreet3;
				tempStreet3.kstack2.prev1 = tempStreet3;
				tempStreet3 = tempStreet3.next1;
			}
			break;
			
			
			
			
		case 1:
			/**
			 * append kstacks to correct streets
			 * first 1/3 kstack is connected to top lane
			 * second 1/3 is connected to middle lane
			 * last 1/3 is connected to bottom lane
			 * 
			 * the first stack is also closest to the exit
			 */
			for (int i=0; i<2*kHeight*carSize+1; i++) {
				tempStreet2 = tempStreet2.next1;
				tempStreet3 = tempStreet3.next1;
			}
			for (int i=parkingRows-1; i>=0; i--) {
				tempStreet1.kstack1 = kstacks[2*i  ];
				tempStreet1.kstack2 = kstacks[2*i+1];
				kstacks[2*i  ].lane = spawn.next1;
				kstacks[2*i+1].lane = spawn.next1;
				tempStreet1.kstack1.prev1 = tempStreet1;
				tempStreet1.kstack2.prev1 = tempStreet1;
				tempStreet1 = tempStreet1.next1;
				
				tempStreet2.kstack1 = kstacks[2*i   +2*parkingRows];
				tempStreet2.kstack2 = kstacks[2*i+1 +2*parkingRows];
				kstacks[2*i   +2*parkingRows].lane = spawn.next2;
				kstacks[2*i+1 +2*parkingRows].lane = spawn.next2;
				tempStreet2.kstack1.prev1 = tempStreet2;
				tempStreet2.kstack2.prev1 = tempStreet2;
				tempStreet2 = tempStreet2.next1;
				
				tempStreet3.kstack1 = kstacks[2*i   +4*parkingRows];
				tempStreet3.kstack2 = kstacks[2*i+1 +4*parkingRows];
				kstacks[2*i   +4*parkingRows].lane = spawn.next3;
				kstacks[2*i+1 +4*parkingRows].lane = spawn.next3;
				tempStreet3.kstack1.prev1 = tempStreet3;
				tempStreet3.kstack2.prev1 = tempStreet3;
				tempStreet3 = tempStreet3.next1;
			}

			break;
			
			
			
			
			
			
		case 2:
			tempStreet1 = crossroad.prev1;
			tempStreet2 = crossroad.prev2;
			tempStreet3 = crossroad.prev3;
			int counter = 0, step = 0;
			while(counter < config.parkingRows*6) {
				if (tempStreet1 != spawn) {
					tempStreet1.kstack1 = kstacks[counter];
					kstacks[counter].prev1 = tempStreet1;
					kstacks[counter].lane = spawn.next1;
					counter++;
					tempStreet1.kstack2 = kstacks[counter];
					kstacks[counter].prev1 = tempStreet1;
					kstacks[counter].lane = spawn.next1;
					counter++;
				}
				
				if (step >= (2*config.kHeight*config.carSize)+1) {
					tempStreet2.kstack1 = kstacks[counter];
					kstacks[counter].prev1 = tempStreet2;
					kstacks[counter].lane = spawn.next2;
					counter++;
					tempStreet2.kstack2 = kstacks[counter];
					kstacks[counter].prev1 = tempStreet2;
					kstacks[counter].lane = spawn.next2;
					counter++;
					tempStreet3.kstack1 = kstacks[counter];
					kstacks[counter].prev1 = tempStreet3;
					kstacks[counter].lane = spawn.next3;
					counter++;
					tempStreet3.kstack2 = kstacks[counter];
					kstacks[counter].prev1 = tempStreet3;
					kstacks[counter].lane = spawn.next3;
					counter++;
				}
				
				if (tempStreet1 != spawn)
					tempStreet1 = tempStreet1.prev1;
				tempStreet2 = tempStreet2.prev1;
				tempStreet3 = tempStreet3.prev1;
				
				step++;
			}
			break;
			
			
			
			
			
		default:
			System.out.println("Please review the used Configuration. The setting for the layout of the parking does not seem correct.");
			System.exit(1);
			break;
		
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
	
	/**
	 * Random Number Generator
	 * @return random float with [0,1)
	 */
//	private static float getRandomNumber() {
//		if (!config.secureRandom)
//			return (float)(mathRandom.nextFloat());
//		return (float)(Math.abs(mathRandom.nextInt()))/(float)(Integer.MAX_VALUE);
//	}
}
