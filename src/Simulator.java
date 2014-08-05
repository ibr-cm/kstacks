

public class Simulator {
	
	private int tick;
	private Spawn spawn;
	private Despawn despawn;
	private Crossroad crossroad;
	private KStack[] kstack;
	private EventItem[] eventList;
	private EventItem[] spawnList; // just in case you want to spawn cars fast behind each other
	private Car[] carList;
	private int totalCarsUsed;
	private int kHeight;
	private int carSize;
	private int parkingRows;
	private int carsInParkingLot;
	
	public Simulator(Spawn spawn, Despawn despawn, Crossroad crossroad, KStack[] kstack, Car[] carList, EventItem[] eventList, int totalCarsUsed, int kHeight, int carSize, int parkingRows) {
		this.tick = 0;
		this.spawn = spawn;
		this.despawn = despawn;
		this.crossroad = crossroad;
		this.kstack = kstack;
		this.eventList = eventList;
		this.carList = carList;
		this.totalCarsUsed = totalCarsUsed;
		this.kHeight = kHeight;
		this.carSize = carSize;
		this.parkingRows = parkingRows;
		this.spawnList = new EventItem[totalCarsUsed];
		for (int i=0; i<totalCarsUsed; i++) {
			this.spawnList[i] = null;
		}
	}
	
	public void runSimulator() {
		while(!eventsFinished() && tick<25) {
			
			
			
			
			
			moveCars();
			
			
			//despawnCar();
			
			
			checkForSpawns();
			
			spawnCar();
			
			printMidLane();
			
			tick++;
		}
	}
	
	
	private boolean eventsFinished() {
		for (int i=0; i<totalCarsUsed; i++) {
			if (!eventList[i].fulfilled)
				return false;
		}
		return true;
	}
	
	private void despawnCar() {
		if (despawn.car != null) {
			Car tempCar = despawn.car;
			EventItem tempEvent = despawn.car.eventItem;
			
			tempEvent.fulfilled = true;
			tempEvent.exitTime = tick;
			Street tempStreet = despawn;
			while(tempStreet != crossroad) {
				tempStreet.car = null;
				tempStreet = tempStreet.prev1;
			}
			tempCar.done = true;
			
			// now one car less in the parking lot
			this.carsInParkingLot--;
		}
	}
	
	
	// checks for new cars which should spawn now and puts them in a queue
	private void checkForSpawns() {
		for (int i=0; i<totalCarsUsed; i++) {
			
			// check if a car is supposed to spawn
			if (eventList[i].entryTime == tick) {
				
				System.out.println("checkForSpawns: Put a car on spawn list at "+this.tick+" tick(s).");
				
				// the spawning of this car will be appended to the list of spawns
				int j = 0;
				while (spawnList[j] != null) {
					j++;
				}
				spawnList[j] = eventList[i];
				
				// index of the stack with the smallest amount of cars
				int index = findSmallestStack();
				
				System.out.println("checkForSpawns: Car was put on stack "+index+" ("+this.kstack[index]+").");
				//System.out.println("Lane "+());

				// assign kstack to a car
				eventList[i].car.kstack = kstack[index];
				if (index/(kstack.length/3)==0) {
					eventList[i].car.lane = spawn.next1;
				} else if (index/(kstack.length/3)==1) {
					eventList[i].car.lane = spawn.next2;
				} else {
					eventList[i].car.lane = spawn.next3;
				}
				
				// assign parkingSpot to the car
				eventList[i].car.parkingSpot = kstack[index].watermark;
				
				// increment the cars stored in this kstack
				kstack[index].watermark += 1;
				System.out.println("checkForSpawns: watermark of "+this.kstack[index]+" now "+this.kstack[index].watermark+".");
				
			}
		}
	}
	
	
	// spawns the next car in the queue (in case there is one)
	private void spawnCar() {
		if (spawnList[0] != null) {
			
			System.out.println("spawnCar: Car is on spawn list.");
			
			// check if spawn is free -- possible cases
			// spawn blocked by kstack (unparking)
			// spawn blocked by car (previous spawn)
			// kstack of spawning car is not accessible (because it is unparking)
			boolean spawnBlocked = false;
			if (spawn.blockingKStack != null || spawn.car != null || (kHeight*carSize>2?spawn.prev1.car != null:false) || spawnList[0].car.kstack.locked) {
				spawnBlocked = true;
			}
			
			System.out.println("spawnCar: Spawn was "+(spawnBlocked?"":"not ")+"blocked.");
			
			if (!spawnBlocked) {
				// put the car down at the spawn
				spawnList[0].car.spawn();
				
				// setup driving target, which is essential the final parking position
				DrivingTarget[] targets = new DrivingTarget[1];
				Street tempStreet1 = new Street();
				tempStreet1 = spawnList[0].car.kstack;
				if (carSize > 1 || spawnList[0].car.kstack.watermark < kHeight-1) {
					System.out.println("spawnCar: carSize "+carSize);
					System.out.println("spawnCar: watermark of "+tempStreet1+" is "+spawnList[0].car.kstack.watermark);
					for (int i=0; i<kHeight*carSize-1-carSize*(spawnList[0].car.kstack.watermark-1); i++) {
						tempStreet1 = tempStreet1.next1;
					}
				}
				System.out.println("spawnCar: Final parking position: "+tempStreet1);
				targets[0] = new DrivingTarget(tempStreet1, 'D');
				spawnList[0].car.setDrivingTargets(targets);
				
				// shift the whole list one item down
				for (int i=0; i<totalCarsUsed-1; i++) {
					spawnList[i] = spawnList[i+1];
				}
				spawnList[totalCarsUsed-1] = null;
				
				if (spawnList[0] == null) {
					System.out.println("spawnCar: Spawn list is now empty.");
				} else {
					System.out.println("spawnCar: New latest item on spawn list is "+spawnList[0]);
				}
				
			}
			System.out.println();
		}
	}
	
	private void moveCars() {
		for (int i=0; i<carList.length; i++) {
			if (carList[i].inParkingLot)
				carList[i].drive();
		}
	}
	
	private int findSmallestStack() {
		int index = -1, indexFallback = -1;
		int watermark = kHeight, watermarkFallback = kHeight;
		
		for (int i=0; i<parkingRows; i++) {
			// looking for unlocked kstack with lowest watermark
			if (kstack[i].watermark < watermark && !kstack[i].locked) {
				watermark = kstack[i].watermark;
				index = i;
			}
			// in case there is no unlocked kstack with an parking spot
			// this method gives back a locked stack with lowest watermark
			if (kstack[i].watermark < watermarkFallback) {
				watermarkFallback = kstack[i].watermark;
				indexFallback = i;
			}
		}
		if (index == -1) {
			return indexFallback;
		}
		return index;
	}
	
	private void log(String data) {
		
	}
	
	private void printMidLane() {
		Street tempStreet1 = this.spawn;
		for (int i=0; i<parkingRows+2; i++) {
			System.out.println("Street: "+tempStreet1+", Car: "+tempStreet1.car);
			tempStreet1 = tempStreet1.next1;
		}
		System.out.println();
	}
}
