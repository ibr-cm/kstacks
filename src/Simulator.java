public class Simulator {
	
	/**
	 * Ticks the simulator is using to calculate discrete time intervals.
	 */
	private int tick;
	
	/**
	 * Spawn and Despawn where cars enter and leave the parking lot.
	 */
	private Spawn spawn;
	private Despawn despawn;
	
	/**
	 * The crossroad is a special piece of street near the despawn. It has
	 * three streets leading to it. So it has three references to them.
	 */
	private Crossroad crossroad;
	
	/**
	 * List of KStack which can be used.
	 */
	private KStack[] kstack;
	
	/**
	 * List of all events, which will be used in the process of simulation.
	 * Each of them has a reference to a car and a time when this car enters
	 * and leaves the parking lot. When all events are fulfilled the simulation
	 * is complete.
	 */
	private EventItem[] eventList;
	
	/**
	 * The spawn list a linked list which has all the spawns which are supposed
	 * to happen. This ensures that a certain priority. 
	 */
	private SpawnEvent spawnList;
	
	/**
	 * This is a list that holds all the unparking processes.
	 */
	private UnparkEvent unparkingList;
	
	/**
	 * This is a list of streets are will be used in the next ticks of
	 * simulation. This ensures that the unparking order follows the order in
	 * which unparking oders came in. It is only used when the boolean
	 * chaoticUnparking is set to false.
	 * Every KStack writes all the streets it needs for the current unparking
	 * event to this list. When it detects that a street it needs is already on
	 * the stack it cannot unpark. Also only if chaoticUnparking is set to
	 * false. 
	 */
	private Street[] usedByKStacks;
	
	
	private int visualOutput;
//	private int verboseLevel;
	private int crossroadRoundRobinState;
	
	private int cars100k;
	
	/**
	 * Configuration file where most global variables
	 */
	private Configuration config;
	
	public Simulator(Spawn spawn, Despawn despawn, Crossroad crossroad, KStack[] kstack, EventItem[] eventList, Configuration config) {
		
		// This ensures that all are working with the same config file
		this.config = config;
		
		this.tick = 0;
		this.spawn = spawn;
		this.despawn = despawn;
		this.crossroad = crossroad;
		this.kstack = kstack;
		this.eventList = eventList;
		this.unparkingList = new UnparkEvent();
		this.unparkingList.setHead();
		this.visualOutput = 0; // for documentation please see config file
		this.spawnList = null;
		this.crossroadRoundRobinState = 0;
		
		
//		try{
//			resultWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/results_"+config.resultPostfix+".csv"),true));
//		} catch (Exception e) {System.out.println("Could not create resultWriter.");}
//		
//		try{
//			debugWriter = new BufferedWriter(new FileWriter(new File("./"+config.resultPostfix+"/debug_"+config.resultPostfix+".txt"),true));
//		} catch (Exception e) {System.out.println("Could not create debugWriter.");}
		
		config.output.writeToResultFile("# EntryTime,EntryDelay,BackOrderTime,BackOrderDelay,ExitTime,ExitTime-BackOrderTime,tilesMoved,startstop");
	}
	
	/**
	 * Heart of the simulator. This piece of code calculates the single steps.
	 * @param maxTick Maximum of ticks the simulator is supposed to run. If
	 * set to 0 the simulator runs until all items on the eventList are done.
	 * @param debug If this boolean is set the simulator tries to put all cars
	 * into kstack[0]. This is only useful when debuging.
	 * @param visualOutput In case the user wishes to have visual output of the
	 * state of the parking lot the value can be used to specify every how many
	 * ticks the user wants to have output. The down-side is the performance
	 * reduction when drawing is necessary. 0 means no output, 1 means every
	 * tick, 2 means every other tick, ...
	 * @param verboseLevel This specifies how much text output the simulator
	 * preduces. 0 means no output, 1 means despawn events only, 2 means every-
	 * thing the methods during the simulation process have to say.
	 */
	@SuppressWarnings("unused")
	public void runSimulator() {
		this.cars100k = 0;
		this.visualOutput = config.visualOutput;

		// disable previous settings for outputs
		if (config.debugPeriodStart != -1 && config.debugPeriodStop != -1 && config.debugPeriodStart <= config.debugPeriodStop)
			this.visualOutput = 0;
		
		while(!simulationDone()) {
			
			if (config.debugPeriodStop != -1 && config.debugPeriodStart != -1 && config.debugPeriodStart == tick)
				this.visualOutput = config.debugPeriodVisual;
			
			if (config.debugPeriodStop != 0 && tick == config.debugPeriodStop && config.debugBreakAfter)
				return;
			if ((tick%5000)==0)
				System.out.println(tick);
			config.output.consoleOutput("=============================================================",2,tick);
			config.output.consoleOutput("Tick: "+tick,2,tick);
			config.output.consoleOutput("=============================================================",2,tick);
			
			this.usedByKStacks = null;
			checkForStreetBlocking();
			
			
			checkForRoundRobinAtCrossroad(3);
			moveCars();
			config.output.consoleOutput("Moved Cars",2,tick);
			
			checkForStartsStops();
			
			refreshStreets();
			
			despawnCar();
			
			checkForSpawns();
			
			spawnCar();
			
			checkForUnparkingEvents();
			
			boolean carsInLot = false;
l7:			for (int i = 0; i<eventList.length; i++)
				if (eventList[i].getCar().isInParkingLot()) {
					carsInLot = true;
					break l7;
				}
			
			// generate a picture if all conditions are meet
			if ((carsInLot || (config.debugPeriodStop > -1 && config.debugPeriodStart > -1)) && this.visualOutput != 0 && (tick%this.visualOutput)==0)
				try {config.output.generateImage(kstack, spawn, crossroad, tick);} catch (Exception e) {}
			
			tick++;
			config.output.consoleOutput("=============================================================",2,tick);
		}
//		System.out.println(cars100k);
//		for (int i = 0; i < eventList.length; i++)
//			this.printEventItem(eventList[i]);
	}
	
	
	private boolean simulationDone() {
		for (int i=0; i<eventList.length; i++) {
			if (!eventList[i].isFulfilled())
				return false;
		}
		return true;
	}
	
	/**
	 * Whenever a car enters the despawn it will automatically vanish and the
	 * EventItem will be filled with the correct information about when the car
	 * left the parking lot.
	 * When it is done the stats of the car get printed out.
	 */
	// TODO
	private void despawnCar() {
		if (despawn.car != null) {
			config.output.consoleOutput("==========",2,tick);
			config.output.consoleOutput("Just despawned car "+despawn.car,2,tick);
			Car tempCar = despawn.car;
			tempCar.eventItem.fulfill(tick);
			Street tempStreet = despawn;
			while(tempStreet.car == tempCar) {
				tempStreet.car = null;
				tempStreet = tempStreet.prev1;
			}
			printEventItem(tempCar.eventItem);
		}
		if (config.tripleDespawn && config.despawnLane2.car != null) {
			config.output.consoleOutput("==========",2,tick);
			config.output.consoleOutput("Just despawned car "+config.despawnLane2.car,2,tick);
			Car tempCar = config.despawnLane2.car;
			tempCar.eventItem.fulfill(tick);
			Street tempStreet = config.despawnLane2;
			while(tempStreet.car == tempCar) {
				tempStreet.car = null;
				tempStreet = tempStreet.prev1;
			}
			printEventItem(tempCar.eventItem);
		}
		if (config.tripleDespawn && config.despawnLane3.car != null) {
			config.output.consoleOutput("==========",2,tick);
			config.output.consoleOutput("Just despawned car "+config.despawnLane3.car,2,tick);
			Car tempCar = config.despawnLane3.car;
			tempCar.eventItem.fulfill(tick);
			Street tempStreet = config.despawnLane3;
			while(tempStreet.car == tempCar) {
				tempStreet.car = null;
				tempStreet = tempStreet.prev1;
			}
			printEventItem(tempCar.eventItem);
		}
	}
	
	
	/**
	 * This method checks for new cars, which should spawn now and puts them in
	 * a queue for the spawn. It also checks if too many cars are inside the
	 * parking lot.
	 */
	private void checkForSpawns() {
		for (int i=0; i<eventList.length; i++) {
			
			// check if a car is supposed to spawn
			if (eventList[i].getEntryTime() == tick) {
				config.output.consoleOutput("==========",2,tick);
				config.output.consoleOutput("checkForSpawns: Put a car on spawn list at "+this.tick+" tick(s).",2,tick);
				
				// adds up the content of all kstacks
				int carsInStacks = 0;
				for (int j = 0; j < kstack.length; j++) {
					carsInStacks += kstack[j].watermark;
				}
				if (spawnList == null)
					spawnList = new SpawnEvent(eventList[i]);
				else
					spawnList.addNewEvent(eventList[i]);
				config.output.consoleOutput("==========",2,tick);
			}
		}
	}
	
	
	
	/**
	 * This method spawns the next car in the queue in case the spawn is free.
	 * The kstack the car should go is also determined right before spawning.
	 */
	private boolean spawnCar() {
		if (spawnList != null) {
			config.output.consoleOutput("==========",2,tick);
			config.output.consoleOutput("spawnCar: Car is on spawn list.",2,tick);
			
			// check if spawn is free -- possible cases
			// spawn blocked by kstack (unparking)
			// spawn blocked by car (previous spawn)
			boolean spawnBlocked = false, freeAvailableKStack = false;
			if (spawn.blockingKStack != null || spawn.car != null || spawn.carAtLastTick != null || spawn.next1.car != null || spawn.next2.car != null || spawn.next3.car != null || (config.kHeight*config.carSize>2?spawn.prev1.car != null:false) || findSmallestStack() == -1) {
				config.output.consoleOutput("spawnCar: Cannot spawn since spawn is blocked!",2,tick);
				spawnBlocked = true;
				
			}
			for (int i=0; i<kstack.length; i++) {
				if (!kstack[i].lockedForParking)
					freeAvailableKStack = true;
			}
			
			config.output.consoleOutput("spawnCar: Spawn was "+(spawnBlocked?"":"not ")+"blocked.",2,tick);
				
			
			// if the spawn is not blocked the will be spawned
			if (!spawnBlocked && freeAvailableKStack) {
				// index of the stack with the smallest amount of cars
				// the boolean is for debug purposes -- if set to TRUE the
				// simulator tries to stack all into kstack[0]
				int index = -1;
				if (tick == config.assortByRandom)
					index = findRandomStack();
				else
					index = findSmallestStack();
				config.output.consoleOutput("spawnCar: Car was put on stack "+index+" ("+this.kstack[index]+").",2,tick);
				
				EventItem tempEventItem = spawnList.eventItem;
				
				// assign the right lane to a car
				tempEventItem.getCar().kstack = kstack[index];
				
				// assign parkingSpot to the car
				tempEventItem.getCar().parkingSpot = kstack[index].watermark;
				config.output.consoleOutput("spawnCar: assigned parking spot: "+tempEventItem.getCar().parkingSpot,2,tick);
				
				// increment the cars stored in this kstack
				kstack[index].watermark++;
				config.output.consoleOutput("spawnCar: watermark of "+this.kstack[index]+" now "+this.kstack[index].watermark+".",2,tick);
								
				
				// put the car down at the spawn
				tempEventItem.getCar().spawn();
				
				// setup driving target, which is essential the final parking position
				Street tempStreet1 = tempEventItem.getCar().kstack;
				
				// the kstack is locked for unparking but not for parking 
				tempEventItem.getCar().kstack.lockedForUnparking = true;
				tempEventItem.getCar().kstack.lockedForParking = true;
//				debugOutput("spawnCar: carSize "+carSize);
				config.output.consoleOutput("spawnCar: watermark of "+tempStreet1+" is "+(tempEventItem.getCar().parkingSpot+1)+" and lockedForUnparking is "+tempEventItem.getCar().kstack.lockedForUnparking,2,tick);
				// fin the right spot where to park exactly
				tempStreet1 = getParkingSpot(tempEventItem.getCar());
				
				config.output.consoleOutput("spawnCar: Final parking position: "+tempStreet1,2,tick);
				DrivingTarget[] targets = new DrivingTarget[1];
				targets[0] = new DrivingTarget(tempStreet1, 'D', tempEventItem.getCar().kstack, tempEventItem.getCar().kstack, false, unparkingList, null, false);
				tempEventItem.getCar().setDrivingTargets(targets);
				
				// Assess the minimal distance the car has to drive.
				if (tempEventItem.getCar().kstack.lane == spawn.next2 || tempEventItem.getCar().kstack.lane == spawn.next3)
					tempEventItem.setMinDistance((2*config.kHeight*config.carSize)+1+config.parkingRows+1+(config.tripleDespawn?0:(2*config.kHeight*config.carSize+1))+config.carSize);
				else
					tempEventItem.setMinDistance(config.parkingRows+1+config.carSize);
				
				// Assess the time the car would need to the next despawn
				int minUnparkingTime = 0;
				tempStreet1 = tempEventItem.getCar().kstack.prev1;
				while (tempStreet1 != despawn && (!config.tripleDespawn || (tempStreet1 != config.despawnLane2 && tempStreet1 != config.despawnLane3))) {
					tempStreet1 = tempStreet1.next1;
					minUnparkingTime++;
				}
				tempEventItem.setMinUnparkingTime(minUnparkingTime);
				
				// remove recently spawned car from spawnList
				spawnList = spawnList.next;
			} else {
				// increase all waitings cars spawn delay by 1
				SpawnEvent tempSpawnEvent = spawnList;
				while (tempSpawnEvent != null) {
					tempSpawnEvent.eventItem.increaseEntryDelay();
					tempSpawnEvent = tempSpawnEvent.next;
				}
			}
			config.output.consoleOutput("==========",2,tick);
			return true;
		}
		return false;
	}
	
	/**
	 * This method finds, depending on the parking spot the car is upposed to
	 * get the correct street.
	 * @param car car, which should park
	 * @param kstack kstack the car parks in
	 * @return a street the car has to reach for its final parking position
	 */
	private Street getParkingSpot(Car car) {
		Street tempStreet1 = car.kstack;
		while (tempStreet1.next1 != null) {
			tempStreet1 = tempStreet1.next1;
		}
		if (car.parkingSpot != 0) {
			for (int i = 0; i < (car.parkingSpot*config.carSize); i++) {
				tempStreet1 = tempStreet1.prev1;
			}
		}
		return tempStreet1;
	}
	
	/**
	 * This method checks for every car whether or not it is inside the parking
	 * lot and if it is this method tries to initiate driving. This depends on
	 * the state of the driving targets. If the car has none it will not move.
	 */
	private void moveCars() {
		for (int i=0; i<eventList.length; i++) {
			if (eventList[i].getCar().isInParkingLot() && eventList[i].getCar().drivingTarget != null) {
				config.output.consoleOutput("==========",2,tick);
				config.output.consoleOutput("Trying to move car "+eventList[i].getCar(),2,tick);
				eventList[i].getCar().drive();
				config.output.consoleOutput("==========",2,tick);
			}
		}
	}
	
	/**
	 * Queues a new unparking process into the queue and sets new locks for
	 * unparking and parking. 
	 */
	private void checkForUnparkingEvents() {
		// Create a new UnparkEvent for every car that wants to unpark.
		for (int i=0; i<eventList.length; i++) {
			if (eventList[i].getBackOrderTime() + eventList[i].getBackOrderDelay() == tick && !eventList[i].isFulfilled()) {
				if (eventList[i].getCar().kstack == null) {
					System.out.println("car was backordered though not yet parked");
					System.out.println("car    "+eventList[i].getCar());
					System.out.println("entry  "+eventList[i].getEntryTime());
					System.out.println("entryD "+eventList[i].getEntryDelay());
					System.out.println("exit   "+eventList[i].getBackOrderTime());
					System.out.println("exitD  "+eventList[i].getBackOrderDelay());
					System.out.println("street "+eventList[i].getCar().currentStreet);
					System.out.println("exit   "+eventList[i].getExitTime());
					System.out.println(tick);
					if (this.spawnList != null)
						System.out.println(this.spawnList.length());
					else
						System.out.println("spawnList is null");
					System.exit(0);
				}
				if (eventList[i].getCar().kstack.lockedForUnparking || eventList[i].getCar().kstack.lockedForParking || eventList[i].getCar().drivingTarget != null) {
					config.output.consoleOutput("==========",2,tick);
					eventList[i].increaseBackOrderDelay();
					config.output.consoleOutput("checkForUnparkingEvents: was delayed because the kstack was locked",2,tick);
					config.output.consoleOutput("==========",2,tick);
				} else {
					config.output.consoleOutput("==========",2,tick);
					config.output.consoleOutput("scheduleUnparking: Creating an unparking Event at "+this.tick+" tick(s).",2,tick);
					config.output.consoleOutput("scheduleUnparking: Stack: "+eventList[i].getCar().kstack.id+".",2,tick);
					UnparkEvent newUnparkEvent = new UnparkEvent();
					newUnparkEvent.setCarToUnpark(eventList[i].getCar()); // Car that wants to leave its spot.
					newUnparkEvent.getKStack().lockedForParking = true;
					newUnparkEvent.getKStack().lockedForUnparking = true;
					
					eventList[i].getCar().unparking = true;
					
					int counter = 0;
					
					// decide whether there is one car which has to unpark or more than one car
					Street tempStreet1 = eventList[i].getCar().currentStreet;
					while (tempStreet1.car == eventList[i].getCar() && tempStreet1 != eventList[i].getCar().kstack)
						tempStreet1 = tempStreet1.prev1;
					
					// only one car unparking
					if (tempStreet1.car == null || tempStreet1.car == eventList[i].getCar()) {
						config.output.consoleOutput("checkForUnparkingEvents: found just 1 car!",2,tick);
						newUnparkEvent.carsInTheWay = 0;
						newUnparkEvent.firstInQueue = eventList[i].getCar();
						
						
						// find street right before entering the street
						Street tempStreet2 = eventList[i].getCar().kstack;
						if (config.carSize > 1) {
							for (int j = 0; j < config.carSize-1; j++) {
								tempStreet2 = tempStreet2.next1;
							}
						}
							
						
						DrivingTarget[] tempTarget1 = new DrivingTarget[2];
						// drive to the right position 
						tempTarget1[0] = new DrivingTarget(newUnparkEvent.getCarToUnpark().kstack.prev1, 'R', newUnparkEvent.getKStack(), newUnparkEvent.getKStack(), true, unparkingList, newUnparkEvent, false);
						tempTarget1[0].reduceWatermark = true;
						tempTarget1[1] = new DrivingTarget(despawn, 'D', null, null, false, unparkingList, null, false);
						
						eventList[i].getCar().drivingTarget = tempTarget1;
						
						
					} else { // more than one car
						config.output.consoleOutput("checkForUnparkingEvents: found more than 1 car!",2,tick);
						
						// the new targets for the car which unparks
						DrivingTarget[] tempTarget1 = new DrivingTarget[2];
						tempTarget1[0] = new DrivingTarget(newUnparkEvent.getCarToUnpark().kstack.prev1, 'R', null, null, false, unparkingList, newUnparkEvent, false);
						tempTarget1[1] = new DrivingTarget(despawn, 'D', null, null, false, unparkingList, null, false);
						eventList[i].getCar().drivingTarget = tempTarget1;
						
						// all other cars
						Car tempCar1 = eventList[i].getCar();
						newUnparkEvent.carsInTheWay = 0;
						
l4:						while (tempStreet1.car != null) {
							config.output.consoleOutput("checkForUnparkingEvents: tempStreet1.car = "+tempStreet1.car,2,tick);
							if (tempStreet1.car != null && tempStreet1.car != tempCar1) {
								newUnparkEvent.carsInTheWay++;
								// set the pointer to the next ..
								tempCar1 = tempStreet1.car;
								// .. and increase the count of cars which are in the same stack behind the car that wants to unpark
								counter++;
								
								newUnparkEvent.firstInQueue = tempCar1;
								
								DrivingTarget tempDrivingTarget[] = new DrivingTarget[4];
								tempStreet1.car.parkingSpot--;
								config.output.consoleOutput("scheduleUnparking: new targets for car "+tempStreet1.car+": "+unparkingSpot(counter, newUnparkEvent.getKStack())+", "+newUnparkEvent.getCarToUnpark().currentStreet,2,tick);
								tempDrivingTarget[0] = new DrivingTarget(unparkingSpot(counter, newUnparkEvent.getKStack()), 'R', null, null, false, unparkingList, null, false);
								tempDrivingTarget[1] = new DrivingTarget(unparkingSpot(counter, newUnparkEvent.getKStack()), 'N', null, null, false, unparkingList, null, false);
								tempDrivingTarget[2] = new DrivingTarget(unparkingSpot(counter, newUnparkEvent.getKStack()), 'N', null, null, false, unparkingList, null, false);
								tempDrivingTarget[3] = new DrivingTarget(getParkingSpot(tempStreet1.car), 'D', null, null, false, unparkingList, null, false);
								tempCar1.drivingTarget = tempDrivingTarget;
								
							}
							if (tempStreet1 == tempCar1.kstack || tempStreet1.car == null)
								break l4;
							tempStreet1 = tempStreet1.prev1;
						}
						config.output.consoleOutput("checkForUnparkingEvents: cars in the way = "+newUnparkEvent.carsInTheWay,2,tick);
						newUnparkEvent.firstInQueue.drivingTarget[0].reduceWatermark = true;
						newUnparkEvent.firstInQueue.drivingTarget[3].unlockKStackForUnparking = newUnparkEvent.getKStack();
						newUnparkEvent.firstInQueue.drivingTarget[3].unlockKStackForParking = newUnparkEvent.getKStack();
						newUnparkEvent.firstInQueue.drivingTarget[3].continousUnblocking = true;
					}
					this.unparkingList.addEvent(newUnparkEvent);
					config.output.consoleOutput("==========",2,tick);
				}
			}
		}
	}
					
					
					
					
					
					
		
	/**
	 * This method finds the right spot a car has to reach to let the car,
	 * which wants to unpark, out of the kstack. 
	 * @param spot How far away from the kstack does the car have to park?
	 * @param kstack What kstack is this all about?
	 * @return The street the car has to reach as the currentStreet.
	 */
	private Street unparkingSpot(int spot, Street kstack) {
		Street tempStreet1 = kstack.prev1;
		for (int i = 0; i < spot*config.carSize; i++) {
			tempStreet1 = tempStreet1.prev1;
		}
		return tempStreet1;
	}
	
	
	/**
	 * This method checks if there is an unparkEvent on the list, which has
	 * cars that are ready to enter the street. And if so it checks if there
	 * is enough space to proceed with the unparking event.
	 * If the cars are piled up next to the street but there is something in
	 * the way so unparking is impossible this method disables the car, which
	 * is first in the queue of unparking cars. This way they cannot
	 * accidentally enter the street.
	 */
	private void checkForStreetBlocking() {
		UnparkEvent tempUnparkEvent1 = unparkingList;
		while (tempUnparkEvent1.next != null) {
			config.output.consoleOutput("==========",2,tick);
			config.output.consoleOutput("checkForStreetBlocking: checking "+tempUnparkEvent1,2,tick);
			tempUnparkEvent1 = tempUnparkEvent1.next;
			
			// Cars are now at the street.
			if (tempUnparkEvent1.getKStack().car != null && !tempUnparkEvent1.doneBlocking) {
				config.output.consoleOutput("checkForStreetBlocking: car is at the kstack "+tempUnparkEvent1.getKStack().id+" ("+tempUnparkEvent1+")",2,tick);
				Street tempStreet1 = tempUnparkEvent1.getKStack().prev1;
				
				// Checking if the street is blocked.
				boolean spaceIsFree = true;
				config.output.consoleOutput("checkForStreetBlocking: checking for space "+((tempUnparkEvent1.carsInTheWay+1)*config.carSize),2,tick);
				for (int i = 0; i < (tempUnparkEvent1.carsInTheWay+1)*config.carSize; i++) {
					if (!config.chaoticUnparking) {
						if (isStreetAlreadyUsedByKStack(tempStreet1)) {
							spaceIsFree = false;
						} else {
							addStreetToUsedByKStacks(tempStreet1);
						}
					}
					if (spaceIsFree && (tempStreet1.car != null || tempStreet1.blockingKStack != null)) {
						spaceIsFree = false;
					}
					tempStreet1 = tempStreet1.prev1;
				}
				
				
				config.output.consoleOutput("checkForStreetBlocking: spaceIsFree? "+spaceIsFree,2,tick);
				
				// If not the street can be blocked.
				if (spaceIsFree) {
					blockStreets(tempUnparkEvent1.getKStack(), ((tempUnparkEvent1.carsInTheWay+1)*config.carSize));
					tempUnparkEvent1.doneBlocking = true;
					tempUnparkEvent1.firstInQueue.disabled = false;
				} else {
					tempUnparkEvent1.firstInQueue.disabled = true;
				}
			}
		}
		if (tempUnparkEvent1.next != null)
			config.output.consoleOutput("==========",2,tick);
	}
		
	
	/**
	 * Blocking a certain amount of tiles beginning at a certain KStack.
	 * @param kstack KStack which is the starting point.
	 * @param length Number of tiles that are needed to be blocked.
	 */
	public void blockStreets(KStack kstack, int length) {
		config.output.consoleOutput("==========",2,tick);
		config.output.consoleOutput("blockStreets: kStack: "+kstack.id+", length: "+length,2,tick);
		Street tempStreet1 = kstack.prev1; // Street right before the KStack
		for (int i = 0; i < length; i++) {
			tempStreet1.blockingKStack = kstack; // block every street with the right KStack
			tempStreet1 = tempStreet1.prev1;
		}
		config.output.consoleOutput("==========",2,tick);
	}

	
	
	/**
	 * This method runs a refresh on all streets. Streets store the car
	 * standing on it right now in carAtLastTick. So there is a way to find out
	 * whether or not there was a car standing here during the last tick. So
	 * cars always leave one tile free inbetween each other while driving.
	 * Since the actual tick is also given to the street there is no way this
	 * can accidentally happen twice to a street during one tick.
	 */
	private void refreshStreets() {
		/* middle lane from first piece of street all the way to the despawn */
		Street tempStreet1 = spawn;
		while (tempStreet1.prev1 != null) { // find the first piece of street
			tempStreet1 = tempStreet1.prev1;
		}
		tempStreet1.refresh(this.tick);
		while (tempStreet1.next1 != null) {
			tempStreet1 = tempStreet1.next1; // go to despawn
			tempStreet1.refresh(this.tick);
		}
		tempStreet1 = spawn.next2; // do the same for the second lane
		tempStreet1.refresh(this.tick);
		while (tempStreet1.next1 != crossroad) {
			tempStreet1 = tempStreet1.next1;
			tempStreet1.refresh(this.tick);
		}
		tempStreet1 = spawn.next3; // do the same for the third lane
		tempStreet1.refresh(this.tick);
		while (tempStreet1.next1 != crossroad) {
			tempStreet1 = tempStreet1.next1;
			tempStreet1.refresh(this.tick);
		}
		
		for (int i = 0; i < kstack.length; i++) {
			kstack[i].refresh(this.tick);
			tempStreet1 = kstack[i];
			while (tempStreet1.next1 != null) {
				tempStreet1 = tempStreet1.next1;
				tempStreet1.refresh(this.tick);
			}
		}
	}
	
	/**
	 * This method initiates the check if cars changed their state (moving/not
	 * moving) compared to the last tick. This will be also saved.
	 */
	private void checkForStartsStops() {
		for (int i = 0; i < eventList.length; i++)
			eventList[i].getCar().checkForStartsStops(tick);
	}
	
	
	private void addStreetToUsedByKStacks(Street street) {
		// if the list is empty this fills it with one item
		if (this.usedByKStacks == null) {
			this.usedByKStacks = new Street[1];
			this.usedByKStacks[0] = street;
		} else {		
			Street tempStreet1[] = new Street[this.usedByKStacks.length+1];
			for (int i = 0; i < this.usedByKStacks.length; i++) {
				tempStreet1[i] = this.usedByKStacks[i];
			}
			tempStreet1[tempStreet1.length-1] = street;
			this.usedByKStacks = tempStreet1;
		}
	}
	
	private boolean isStreetAlreadyUsedByKStack(Street street) {
		if (this.usedByKStacks != null) {
			for (int i = 0; i < this.usedByKStacks.length; i++) {
				if (this.usedByKStacks[i] == street)
					return true;
			}
		}
		return false;
	}
	
	
	private void checkForRoundRobinAtCrossroad(int iterations) {
		if (iterations == 0||crossroad.car != null)
			return;
		Street list[] = {crossroad.prev1, crossroad.prev2, crossroad.prev3};
		if (list[crossroadRoundRobinState].car != null && list[crossroadRoundRobinState].car.drivingTarget[0].direction != 'R' && list[crossroadRoundRobinState].car.drivingTarget[0].street == despawn) {
			list[crossroadRoundRobinState].car.disabled = false;
			if (list[(crossroadRoundRobinState+1)%3].car != null && list[(crossroadRoundRobinState+1)%3].car.drivingTarget[0].direction != 'R' && list[(crossroadRoundRobinState+1)%3].car.drivingTarget[0].street == despawn)
				list[(crossroadRoundRobinState+1)%3].car.disabled = true;
			if (list[(crossroadRoundRobinState+2)%3].car != null && list[(crossroadRoundRobinState+2)%3].car.drivingTarget[0].direction != 'R' && list[(crossroadRoundRobinState+2)%3].car.drivingTarget[0].street == despawn)
				list[(crossroadRoundRobinState+2)%3].car.disabled = true;
			crossroadRoundRobinState = (crossroadRoundRobinState+1)%3;
		} else {
			crossroadRoundRobinState = (crossroadRoundRobinState+1)%3;
			checkForRoundRobinAtCrossroad(iterations-1);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * In order to park a car in the kstack with the lowest number of vehicles
	 * this method finds the smallest stack. If there is a kstack which is not
	 * full and unlocked for parking this method will find it. If all unlocked
	 * kstacks are full the method returns -1. Cars will not spawn then.
	 * @param testing if this is true the method will give always kstack[0]
	 * @return kstack to park in
	 */
	private int findSmallestStack() {
		if (config.debugSmallestStack <= -1) {
			int indexStack = -1, watermarkStack = config.kHeight;
			
			for (int i=0; i<config.parkingRows*6; i++) {
				// looking for unlocked kstack with lowest watermark
				if (kstack[i].watermark < watermarkStack && !kstack[i].lockedForParking && !kstack[i].disabled) {
					watermarkStack = kstack[i].watermark;
					indexStack = i;
				}
			}
			return indexStack;
		}
		// when config.debugSmallestStack > -1 this always return
		// kstack[config.debugSmallestStack]
		return Math.min(config.debugSmallestStack,kstack.length-1);
	}
	
	/**
	 * This gives back a random Stack, which is not fully occupied.
	 */
	private int findRandomStack() {
		if (config.debugSmallestStack <= -1) {
			int indexStack = -1;
			
			indexStack = (int)(config.secRandom.nextDouble() * (float)(config.parkingRows*6));
			
			for (int i=0; i<config.parkingRows*6; i++) {
				
				
				// looking for unlocked kstack with lowest watermark
//				if (kstack[i].watermark < watermarkStack && !kstack[i].lockedForParking && !kstack[i].disabled) {
//					watermarkStack = kstack[i].watermark;
//					indexStack = i;
//				}
			}
			return indexStack;
		}
		// when config.debugSmallestStack > -1 this always return
		// kstack[config.debugSmallestStack]
		return Math.min(config.debugSmallestStack,kstack.length-1);
	}
	
	
	
	
	
	/**
	 * This method prints a whole EventItem with all important information.
	 * This usually only happens when a car leaves the parking lot.
	 * @param item EventItem, which will be printed
	 */
	private void printEventItem(EventItem item) {
		// if verboselevel is 2 this "==========" will appear here anyway
//		if (verboseLevel == 1)
//			debugOutput("==========",1);
		int stats[] = item.getEventStats();
//		debugOutput("EventItem "+item+" of car "+item.getCar(),1);
//		debugOutput("Entry Time: "+stats[0],1);
//		debugOutput("Entry Delay: "+stats[1],1);
//		debugOutput("BackOrder Time: "+stats[2],1);
//		if (verboseLevel == -1)
//			System.out.println(stats[4]-stats[2]);
//		else
//			debugOutput("BackOrder Delay: "+stats[3],1);
//		debugOutput("Exit Time: "+stats[4],1);
//		debugOutput("Moved Tiles: "+item.getCar().tilesMoved,1);
//		debugOutput("Starts, Stops: "+item.getCar().startstop, 1);
//		debugOutput("Watermark: "+item.getCar().kstack.watermark,1);
//		debugOutput("==========",1);
//		if (stats[2] != config.excludeFromResults) {
			String result = stats[0]+","+stats[1]+","+stats[2]+","+stats[3]+","+stats[4]+","+(stats[4]-stats[2])+","+item.getCar().tilesMoved+","+item.getCar().startstop;
			config.output.consoleOutput(result,1, tick);
			config.output.writeToResultFile(result);
			config.output.writeToBackOrderTime((stats[4]-stats[2])+" "+item.getMinUnparkingTime());
			config.output.writeToTilesMoved(item.getCar().tilesMoved+" "+item.getMinDistance());
			config.output.writeToStartStop(item.getCar().startstop);
//		}
//		else
//			cars100k++;
	}
	
}
