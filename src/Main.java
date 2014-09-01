public class Main {
	public static void main(String args[]) {
		
		int parkingRows = 1;
		int carSize = 3;
		int kHeight = 4;
		int totalCarsUsed = 4;
		
		
		
		Spawn spawn = new Spawn();
		Crossroad crossroad = new Crossroad();
		Despawn despawn = new Despawn();
		
		KStack[] kstacks = new KStack[parkingRows*6]; // first third belongs to top lane; second third belongs to middle lane; last third belongs to bottom lane;
		Car[] carList = new Car[totalCarsUsed];
		EventItem[] eventList = new EventItem[totalCarsUsed];
		
		Street tempStreet1 = new Street(), tempStreet2 = new Street(); // piece of street to work with

		
		
		/*
		 ****************************************************************
		 * SETUP
		 ****************************************************************
		 */
		
		
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
		
		
		
		
		
		
		
		//printLayout(spawn, despawn, crossroad);
		
		//printKStacks(kstacks, carSize, kHeight);
		
		
		for (int i=0; i<totalCarsUsed; i++) {
			eventList[i] = new EventItem();
			carList[i] = new Car(carSize,eventList[i], spawn, despawn, crossroad);
		}
		
		
		eventList[0].setupEvent(carList[0], 1, 20);
		eventList[1].setupEvent(carList[1], 4, 95);
		eventList[2].setupEvent(carList[2], 7, 95);
		eventList[3].setupEvent(carList[3], 11, 95);
//		eventList[4].setupEvent(carList[4], 25, 95);
//		eventList[5].setupEvent(carList[5], 30, 95);
//		eventList[6].setupEvent(carList[6], 35, 95);
		
		
		
		
		Simulator simulator = new Simulator(spawn, despawn, crossroad, kstacks, carList, eventList, totalCarsUsed, kHeight, carSize, parkingRows);
		simulator.runSimulator();
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
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
}
