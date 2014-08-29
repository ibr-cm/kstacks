

public class Simulator {
	
	private int tick;
	private Spawn spawn;
	private Despawn despawn;
	private Crossroad crossroad;
	private KStack[] kstack;
	private EventItem[] eventList;
	private EventItem[] spawnList; // just in case you want to spawn cars fast behind each other
	private UnparkEvent unparkingList;
	private Car[] carList;
	private int totalCarsUsed;
	private int kHeight;
	private int carSize;
	private int parkingRows;
	
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
		this.unparkingList = new UnparkEvent();
		this.unparkingList.setFirst();
	}
	
	public void runSimulator() {
		while(!eventsFinished() && tick<25) {
			
//			if (tick == 8) {
//				System.out.println("Car Test");
//				Car testCar = new Car();
//				spawn.next1.car = testCar;
//			}
			
			
			System.out.println("=============================================================");
			System.out.println("Tick: "+tick);
			System.out.println("=============================================================");
			System.out.println();
			
			checkForStreetBlocking();
			
			checkForStreetUnblocking();
			
			moveCars();
			System.out.println("Moved Cars");
			System.out.println();
			
			despawnCar();
			
			checkForSpawns();
			
			spawnCar();
			
			checkForUnparkingEvents();
			
			printMidLane();
			printStack(0);
			printStack(1);
			printStack(2);
			printStack(3);
			printStack(4);
			printStack(5);
			printDespawn();
			
			tick++;
			System.out.println("=============================================================");
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
			System.out.println("Just despawned car "+despawn.car);
			Car tempCar = despawn.car;
			EventItem tempEvent = tempCar.eventItem;
			
			tempEvent.fulfilled = true;
			tempEvent.exitTime = tick;
			Street tempStreet = despawn;
			while(tempStreet != crossroad) {
				tempStreet.car = null;
				tempStreet = tempStreet.prev1;
			}
			tempCar.done = true;
			printEventItem(tempCar.eventItem);
			// TODO: Delete the corresponding UnparkEvent;
		}
	}
	
	
	// checks for new cars which should spawn now and puts them in a queue
	private void checkForSpawns() {
		for (int i=0; i<totalCarsUsed; i++) {
			
			// check if a car is supposed to spawn
			if (eventList[i].entryTime == tick) {
				
				System.out.println("checkForSpawns: Put a car on spawn list at "+this.tick+" tick(s).");
				
				// if all kstacks are full the spawn gets cancelled, the event is considered fulfilled
				// cars which did not fit into the parkingLot can be recognized because their exit time stamp
				// is earlier than the entry time stamp.
				int carsInStacks = 0;
				for (int j=0; j<kstack.length; j++) {
					carsInStacks += kstack[j].watermark;
				}
				if (carsInStacks == kstack.length*kHeight) {
					eventList[i].fulfilled = true;
					eventList[i].exitTime = tick-1;
					System.out.println("checkForSpawn: a car tried to spawn but parkingLot is full");
					System.out.println("checkForSpawn: event: "+eventList[i]);
					System.out.println("checkForSpawn: entryTime: "+eventList[i].entryTime+", backOrderTime: "+eventList[i].backOrderTime+", exitTime"+eventList[i].exitTime);
					System.out.println();
					return;
				}
				
				// the spawning of this car will be appended to the list of spawns
				int j = 0;
				while (spawnList[j] != null) {
					j++;
				}
				spawnList[j] = eventList[i];
				
				// index of the stack with the smallest amount of cars
				int index = findSmallestStack(true);
				
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
				spawnList[0].car.kstack.locked = true;
				System.out.println("spawnCar: carSize "+carSize);
				System.out.println("spawnCar: watermark of "+tempStreet1+" is "+spawnList[0].car.kstack.watermark+" and locked is "+spawnList[0].car.kstack.locked);
				if (carSize > 1 || spawnList[0].car.kstack.watermark < kHeight) {
					for (int i=0; i<kHeight*carSize-1-carSize*(spawnList[0].car.kstack.watermark-1); i++) {
						tempStreet1 = tempStreet1.next1;
					}
				}
				System.out.println("spawnCar: Final parking position: "+tempStreet1);
				targets[0] = new DrivingTarget(tempStreet1, 'D', spawnList[0].car.kstack);
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
	
	private void checkForUnparkingEvents() {
		// Create a new UnparkEvent for every car that wants to unpark.
		for (int i=0; i<totalCarsUsed; i++) {
			if (eventList[i].backOrderTime == tick) {
				System.out.println("scheduleUnparking: Creating an unparking Event at "+this.tick+" tick(s).");
				UnparkEvent newUnparkEvent = new UnparkEvent();
				newUnparkEvent.setCarSize(carSize);
				newUnparkEvent.setCarToUnpark(eventList[i].car); // Car that wants to leave its spot.
				newUnparkEvent.setKStack();
				
				// The car needs new coordinates where it is supposed to go.
				// So the kStack the car is assigned to is deleted from the cars object and the new coordinates are put in.
				DrivingTarget[] tempTarget1 = new DrivingTarget[2];
				tempTarget1[0] = new DrivingTarget(newUnparkEvent.carToUnpark.kstack.prev1, 'R', null);
				tempTarget1[1] = new DrivingTarget(despawn, 'D', null);
				//System.out.println(tempTarget1[0].street+" "+tempTarget1[0].direction);
				//System.out.println(tempTarget1[1].street+" "+tempTarget1[1].direction);
				eventList[i].car.drivingTarget = tempTarget1;
				eventList[i].car.unparking = true;
				newUnparkEvent.carToUnpark.kstack = null;
				
				
				// Get a list of cars that need to be unparked. The car closest to the street (in case there is any) will be
				// made known to the UnparkEvent. So this can observe if the cars already proceeded up to the street.
				// Otherwise cars will be moved up to the street first and block the street only if they are entering it right away.
				Street tempStreet1 = eventList[i].car.currentStreet;
				Car tempCar1 = eventList[i].car;
				System.out.println("scheduleUnparkting: "+tempCar1+" is going to unpark.");
				int counter = 0;
				while (tempCar1 != null) {
					while (tempStreet1.car == tempCar1) {
						// Move to the next parking space (depends on the car size). Cars with different length could be used.55x3
						tempStreet1 = tempStreet1.prev1;
					}
					if (tempStreet1.car == null) {
						// So now the car closest to the road is found. In case this is the car, which tries to unpark, the UnparkEvent
						// will handle that.
						newUnparkEvent.firstInQueue = tempCar1;
						tempCar1 = null; // set tempCar1 to null to break the while-loop
					} else {
						// set the pointer to the next ..
						tempCar1 = tempStreet1.car;
						// .. and increase the count of cars which are in the same stack behind the car that wants to unpark
						counter++;
						// TODO: according to the counter the final position can be calculated the car has to reach to let the
						// car go, which unparks.
					}
					newUnparkEvent.carsInTheWay = counter;
				}
				System.out.println("scheduleUnparking: "+counter+" cars are in the way.");
				this.unparkingList.addEvent(newUnparkEvent);
			}
		}
	}
	
	
	private void checkForStreetBlocking() {
		// TODO: Make the first car in the queue block the kstack and unblock it when back at the right parking spot.
		UnparkEvent tempUnparkEvent1 = unparkingList;
		while (tempUnparkEvent1.next != null) {
			tempUnparkEvent1 = tempUnparkEvent1.next;
			// If the unparking Cars are piled right up to the street in the next step
			// the street needs to be blocked, so the cars can unpark.
			// Important is here, that the unparking orders need to be done they came in.
			// They can be done in parallel but the one with the highest priority (closest)
			// should not be blocked by other unparking events.
			// In case there is still a car in the area, which needs to be blocked off, the
			// unparking needs to be stalled but there is still no other unparking event allowed
			// when it would block space an unparking event of higher priority needs.
			
			// Cars are now at the street.
			if (tempUnparkEvent1.kstack.car != null) {
				Street tempStreet1 = tempUnparkEvent1.kstack.prev1;
				
				// Checking if the street is blocked.
				boolean spaceIsFree = true;
				for (int i = 0; i < (tempUnparkEvent1.carsInTheWay+1)*this.carSize-1; i++) {
					if (tempStreet1.car != null) {
						spaceIsFree = false;
					}
				}
				
				// If not the street can be blocked.
				if (spaceIsFree) { // && tempUnparkEvent1.carsInTheWay == 0) {
					blockStreets(tempUnparkEvent1.kstack, this.carSize);
					
				}
			}
		}
	}
		
	
	// Blocking a certain amount of tiles
	public void blockStreets(KStack kstack, int length) {
		Street tempStreet1 = kstack.prev1;
		for (int i = 0; i < length; i++) {
			tempStreet1.blockingKStack = kstack;
			tempStreet1 = tempStreet1.prev1;
		}
	}
	
	private void checkForStreetUnblocking() {
		UnparkEvent tempUnparkEvent1 = unparkingList;
		while (tempUnparkEvent1.next != null) {
			tempUnparkEvent1 = tempUnparkEvent1.next;
			if(tempUnparkEvent1.isReadyToUnblock())
				System.out.println("CheckForStreetUnblocking: unblock situation detected");
				tempUnparkEvent1.unblockTiles();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private int findSmallestStack(boolean testing) {
		if (!testing) {
			int index = -1, indexFallback = -1;
			int watermark = kHeight, watermarkFallback = kHeight;
			
			for (int i=0; i<parkingRows*6; i++) {
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
		return 0;
	}
	
	
	private void printMidLane() {
		System.out.println("Middle Lane:");
		Street tempStreet1 = this.spawn;
		while (tempStreet1.prev1 != null) {
			tempStreet1 = tempStreet1.prev1;
		}
		do {
			System.out.println("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack);
			tempStreet1 = tempStreet1.next1;
		} while(tempStreet1 != null);
		System.out.println();
	}
	
	private void printDespawn() {
		System.out.println("Exit of the parking Lot:");
		Street tempStreet1 = this.crossroad;
		do {
			System.out.println("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack);
			tempStreet1 = tempStreet1.next1;
		} while (tempStreet1 != this.despawn);
		System.out.println("Street: "+this.despawn+", Car: "+this.despawn.car);
	}
	
	
	private void printStack(int stack) {
		System.out.println("Stack "+stack+" (watermark: "+kstack[stack].watermark+"):");
		Street tempStreet1 = kstack[stack];
		System.out.println("Street: "+tempStreet1+", Car: "+tempStreet1.car+", locked: "+((KStack)tempStreet1).locked+", blocking kstack: "+tempStreet1.blockingKStack);
		for (int i=0; i<(kHeight*carSize)-1; i++) {
			tempStreet1 = tempStreet1.next1;
			System.out.println("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack);
		}
		System.out.println();
	}
	
	private void printEventItem(EventItem item) {
		System.out.println("==========");
		System.out.println("EventItem "+item+" of car "+item.car);
		System.out.println("Entry Time: "+item.entryTime);
		System.out.println("BackOrder Time: "+item.backOrderTime);
		System.out.println("Exit Time: "+item.exitTime);
		System.out.println("==========");
		System.out.println();
	}
}
