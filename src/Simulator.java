import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;



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
	
	/**
	 * If this boolean is set to true every KStack can unpark at anytime given
	 * the space is free. The simulator does care for the order unparking
	 * orders came in. As soon the space is free the KStack will unpark. This
	 * can lead to heavy delays for individual unparking events but might
	 * increase mean performance.
	 * If it is set to false the simulator will follow the order in which cars
	 * parked and makes unparking events with lower priority wait.
	 */
	private boolean chaoticUnparking;
	
	
	private int kHeight;
	private int carSize;
	private int parkingRows;
	private boolean debug;
	private int visualOutput;
	private int verboseLevel;
	private int crossroadRoundRobinState;
	
	public Simulator(Spawn spawn, Despawn despawn, Crossroad crossroad, KStack[] kstack, EventItem[] eventList, int kHeight, int carSize, int parkingRows) {
		this.tick = 0;
		this.spawn = spawn;
		this.despawn = despawn;
		this.crossroad = crossroad;
		this.kstack = kstack;
		this.eventList = eventList;
		this.kHeight = kHeight;
		this.carSize = carSize;
		this.parkingRows = parkingRows;
		this.unparkingList = new UnparkEvent();
		this.unparkingList.setHead();
		this.visualOutput = 0;
		this.verboseLevel = 2;
		this.spawnList = null;
		this.crossroadRoundRobinState = 0;
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
	public void runSimulator(int maxTick, boolean debug, int visualOutput, int verboseLevel, boolean chaoticUnparking) {
		
		long time = System.currentTimeMillis();
		
		this.debug = debug;
		this.chaoticUnparking = chaoticUnparking;
		this.verboseLevel = verboseLevel;//Math.min(Math.max(0, verboseLevel),2);
		this.visualOutput = visualOutput;

		this.visualOutput = 0;
//		this.verboseLevel = 0;
		
		while(!eventsFinished() && (tick<maxTick || maxTick == 0)) {
			
//			boolean renderImage = true;
			
			if (tick == 29460) {
				this.visualOutput = 1;
//				this.verboseLevel = 2;
			}
//				
			if (tick == 29600)
				return;
			
			
			System.out.println(this.tick);
			debugOutput("=============================================================",2);
			debugOutput("Tick: "+tick,2);
			debugOutput("=============================================================",2);
			
			this.usedByKStacks = null;
			checkForStreetBlocking();
			
			checkForRoundRobinAtCrossroad(3);
			// TODO ROUND ROBIN @ crossroad
			
//			renderImage = moveCars();
			moveCars();
			debugOutput("Moved Cars",2);
			
			checkForStartsStops();
			
			refreshStreets();
			
			despawnCar();
			
			checkForSpawns();
			
//			if (!renderImage)
//				renderImage = spawnCar();
//			else
				spawnCar();
			
			checkForUnparkingEvents();
			
			boolean carsInLot = false;
			for (int i = 0; i<eventList.length; i++)
				if (eventList[i].getCar().isInParkingLot())
					carsInLot = true;
				
			if (carsInLot && this.visualOutput != 0 && (tick%this.visualOutput)==0) {
				try {
					generateImage(Integer.toString(tick)+"_0");
				} catch (Exception e) {debugOutput(""+e,2);}
			} else {
//				System.out.println("Image omitted");
			}
			
//			if (tick > 29460) {
//				System.out.println("tick: "+tick+", parking: "+kstack[138].lockedForParking+", unparking: "+kstack[138].lockedForUnparking);
//			}
			
			tick++;
			debugOutput("=============================================================",2);
		}
		long duration = System.currentTimeMillis()-time;
		System.out.println("# Gesamtdauer "+((int)(duration/60000))+" min "+(((int)(duration/1000))%60)+" sek.");
	}
	
	
	private boolean eventsFinished() {
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
	private void despawnCar() {
		if (despawn.car != null) {
			debugOutput("==========",2);
			debugOutput("Just despawned car "+despawn.car,2);
			Car tempCar = despawn.car;
			tempCar.eventItem.fulfill(tick);
			Street tempStreet = despawn;
			while(tempStreet != crossroad) {
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
				debugOutput("==========",2);
				debugOutput("checkForSpawns: Put a car on spawn list at "+this.tick+" tick(s).",2);
				
				// adds up the content of all kstacks
				int carsInStacks = 0;
				for (int j=0; j<kstack.length; j++) {
					carsInStacks += kstack[j].watermark;
				}
				// If the parking lot is full the cars event is fulfilled but
				// with a tick count of "right now" minus 1. This ensures that
				// it is obvious which cars never entered the parking lot.
				if (carsInStacks == kstack.length*kHeight) {
					eventList[i].fulfill(tick-1);
					debugOutput("checkForSpawn: a car tried to spawn but parkingLot is full",2);
					debugOutput("checkForSpawn: event: "+eventList[i],2);
					debugOutput("checkForSpawn: entryTime: "+eventList[i].getEntryTime()+", backOrderTime: "+eventList[i].getBackOrderTime()+", exitTime"+eventList[i].getExitTime(),2);
				} else {
					// If there is enough space the spawning of this car will
					// be appended to the list of spawns.
					if (spawnList == null) {
						spawnList = new SpawnEvent(eventList[i]);
					} else {
						spawnList.addNewEvent(spawnList, eventList[i]);
					}
//					int j = 0;
//					while (spawnList[j] != null) {
//						j++;
//					}
//					spawnList[j] = eventList[i];
					debugOutput("==========",2);
				}
			}
		}
	}
	
	
	
	/**
	 * This method spawns the next car in the queue in case the spawn is free.
	 * The kstack the car should go is also determined right before spawning.
	 */
	private boolean spawnCar() {
		if (spawnList != null) {
			debugOutput("==========",2);
			debugOutput("spawnCar: Car is on spawn list.",2);
			
			// check if spawn is free -- possible cases
			// spawn blocked by kstack (unparking)
			// spawn blocked by car (previous spawn)
			boolean spawnBlocked = false, freeAvailableKStack = false;
			if (spawn.blockingKStack != null || spawn.car != null || spawn.carAtLastTick != null || (kHeight*carSize>2?spawn.prev1.car != null:false)) {
				debugOutput("spawnCar: Cannot spawn since spawn is blocked!",2);
				spawnBlocked = true;
			}
			for (int i=0; i<kstack.length; i++) {
				if (!kstack[i].lockedForParking)
					freeAvailableKStack = true;
			}
			
			debugOutput("spawnCar: Spawn was "+(spawnBlocked?"":"not ")+"blocked.",2);
			
			// if the spawn is not blocked the will be spawned
			if (!spawnBlocked && freeAvailableKStack) {
				// index of the stack with the smallest amount of cars
				// the boolean is for debug purposes -- if set to TRUE the
				// simulator tries to stack all into kstack[0]
				int index = findSmallestStack(this.debug);
				debugOutput("spawnCar: Car was put on stack "+index+" ("+this.kstack[index]+").",2);
				
				EventItem tempEventItem = spawnList.eventItem;
				
				// assign the right lane to a car
				tempEventItem.getCar().kstack = kstack[index];
				if (index/(kstack.length/3)==0) {
					tempEventItem.getCar().lane = spawn.next1;
				} else if (index/(kstack.length/3)==1) {
					tempEventItem.getCar().lane = spawn.next2;
				} else {
					tempEventItem.getCar().lane = spawn.next3;
				}
				
				// assign parkingSpot to the car
				tempEventItem.getCar().parkingSpot = kstack[index].watermark;
				debugOutput("spawnCar: assigned parking spot: "+tempEventItem.getCar().parkingSpot,2);
				
				// increment the cars stored in this kstack
				kstack[index].watermark++;
				debugOutput("spawnCar: watermark of "+this.kstack[index]+" now "+this.kstack[index].watermark+".",2);
								
				
				// put the car down at the spawn
				tempEventItem.getCar().spawn();
				
				// setup driving target, which is essential the final parking position
				Street tempStreet1 = tempEventItem.getCar().kstack;
				
				// the kstack is locked for unparking but not for parking 
				tempEventItem.getCar().kstack.lockedForUnparking = true;
				tempEventItem.getCar().kstack.lockedForParking = true;
//				debugOutput("spawnCar: carSize "+carSize);
				debugOutput("spawnCar: watermark of "+tempStreet1+" is "+(tempEventItem.getCar().parkingSpot+1)+" and lockedForUnparking is "+tempEventItem.getCar().kstack.lockedForUnparking,2);
				// fin the right spot where to park exactly
				tempStreet1 = getParkingSpot(tempEventItem.getCar());
				
				debugOutput("spawnCar: Final parking position: "+tempStreet1,2);
				DrivingTarget[] targets = new DrivingTarget[1];
				targets[0] = new DrivingTarget(tempStreet1, 'D', tempEventItem.getCar().kstack, tempEventItem.getCar().kstack, false, unparkingList, null, false);
				tempEventItem.getCar().setDrivingTargets(targets);
				
				// remove recently spawned car from spawnList
				spawnList = spawnList.next;
			} else {
				SpawnEvent tempSpawnEvent = spawnList;
				while (tempSpawnEvent != null) {
					tempSpawnEvent.eventItem.increaseEntryDelay();
					tempSpawnEvent = tempSpawnEvent.next;
				}
			}
			debugOutput("==========",2);
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
			for (int i = 0; i < (car.parkingSpot*carSize); i++) {
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
	private boolean moveCars() {
		boolean renderImage = false;
		for (int i=0; i<eventList.length; i++) {
			if (eventList[i].getCar().isInParkingLot()) {
//				debugOutput("==========",2);
//				debugOutput("Trying to move car "+eventList[i].getCar(),2);
				if (!renderImage)
					renderImage = eventList[i].getCar().drive();
				else
					eventList[i].getCar().drive();
//				debugOutput("==========",2);
			}
		}
		return renderImage;
	}
	
	/**
	 * Queues a new unparking process into the queue and sets new locks for
	 * unparking and parking. 
	 */
	private void checkForUnparkingEvents() {
		// Create a new UnparkEvent for every car that wants to unpark.
		for (int i=0; i<eventList.length; i++) {
			if (eventList[i].getBackOrderTime() + eventList[i].getBackOrderDelay() == tick) {
				if (eventList[i].getCar().kstack.lockedForUnparking || eventList[i].getCar().kstack.lockedForParking || eventList[i].getCar().drivingTarget != null) {
					debugOutput("==========",2);
					eventList[i].increaseBackOrderDelay();
					debugOutput("checkForUnparkingEvents: was delayed because the kstack was locked",2);
					debugOutput("==========",2);
				} else {
					debugOutput("==========",2);
					debugOutput("scheduleUnparking: Creating an unparking Event at "+this.tick+" tick(s).",2);
					debugOutput("scheduleUnparking: Stack: "+eventList[i].getCar().kstack.id+".",2);
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
						debugOutput("checkForUnparkingEvents: found just 1 car!",2);
						newUnparkEvent.carsInTheWay = 0;
						newUnparkEvent.firstInQueue = eventList[i].getCar();
						
						
						// find street right before entering the street
						Street tempStreet2 = eventList[i].getCar().kstack;
						if (carSize > 1) {
							for (int j = 0; j < carSize-1; j++) {
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
						debugOutput("checkForUnparkingEvents: found more than 1 car!",2);
						
						// the new targets for the car which unparks
						DrivingTarget[] tempTarget1 = new DrivingTarget[2];
						tempTarget1[0] = new DrivingTarget(newUnparkEvent.getCarToUnpark().kstack.prev1, 'R', null, null, false, unparkingList, newUnparkEvent, false);
						tempTarget1[1] = new DrivingTarget(despawn, 'D', null, null, false, unparkingList, null, false);
						eventList[i].getCar().drivingTarget = tempTarget1;
						
						// all other cars
						Car tempCar1 = eventList[i].getCar();
						newUnparkEvent.carsInTheWay = 0;
						
l4:						while (tempStreet1.car != null) {
							debugOutput("checkForUnparkingEvents: tempStreet1.car = "+tempStreet1.car,2);
							if (tempStreet1.car != null && tempStreet1.car != tempCar1) {
								newUnparkEvent.carsInTheWay++;
								// set the pointer to the next ..
								tempCar1 = tempStreet1.car;
								// .. and increase the count of cars which are in the same stack behind the car that wants to unpark
								counter++;
								
								newUnparkEvent.firstInQueue = tempCar1;
								
								DrivingTarget tempDrivingTarget[] = new DrivingTarget[4];
								tempStreet1.car.parkingSpot--;
								debugOutput("scheduleUnparking: new targets for car "+tempStreet1.car+": "+unparkingSpot(counter, newUnparkEvent.getKStack())+", "+newUnparkEvent.getCarToUnpark().currentStreet,2);
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
						debugOutput("checkForUnparkingEvents: cars in the way = "+newUnparkEvent.carsInTheWay,2);
						newUnparkEvent.firstInQueue.drivingTarget[0].reduceWatermark = true;
						newUnparkEvent.firstInQueue.drivingTarget[3].unlockKStackForUnparking = newUnparkEvent.getKStack();
						newUnparkEvent.firstInQueue.drivingTarget[3].unlockKStackForParking = newUnparkEvent.getKStack();
						newUnparkEvent.firstInQueue.drivingTarget[3].continousUnblocking = true;
					}
					this.unparkingList.addEvent(newUnparkEvent);
					debugOutput("==========",2);
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
		for (int i = 0; i < spot*carSize; i++) {
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
			debugOutput("==========",2);
			debugOutput("checkForStreetBlocking: checking "+tempUnparkEvent1,2);
			tempUnparkEvent1 = tempUnparkEvent1.next;
			
			// Cars are now at the street.
			if (tempUnparkEvent1.getKStack().car != null && !tempUnparkEvent1.doneBlocking) {
				debugOutput("checkForStreetBlocking: car is at the kstack "+tempUnparkEvent1.getKStack().id+" ("+tempUnparkEvent1+")",2);
				Street tempStreet1 = tempUnparkEvent1.getKStack().prev1;
				
				// Checking if the street is blocked.
				boolean spaceIsFree = true;
				debugOutput("checkForStreetBlocking: checking for space "+((tempUnparkEvent1.carsInTheWay+1)*this.carSize),2);
				for (int i = 0; i < (tempUnparkEvent1.carsInTheWay+1)*this.carSize; i++) {
					if (!this.chaoticUnparking) {
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
				
				
				debugOutput("checkForStreetBlocking: spaceIsFree? "+spaceIsFree,2);
				
				// If not the street can be blocked.
				if (spaceIsFree) {
					blockStreets(tempUnparkEvent1.getKStack(), ((tempUnparkEvent1.carsInTheWay+1)*this.carSize));
					tempUnparkEvent1.doneBlocking = true;
					tempUnparkEvent1.firstInQueue.disabled = false;
				} else {
					tempUnparkEvent1.firstInQueue.disabled = true;
				}
			}
		}
		if (tempUnparkEvent1.next != null)
			debugOutput("==========",2);
	}
		
	
	/**
	 * Blocking a certain amount of tiles beginning at a certain KStack.
	 * @param kstack KStack which is the starting point.
	 * @param length Number of tiles that are needed to be blocked.
	 */
	public void blockStreets(KStack kstack, int length) {
		debugOutput("==========",2);
		debugOutput("blockStreets: kStack: "+kstack.id+", length: "+length,2);
		Street tempStreet1 = kstack.prev1; // Street right before the KStack
		for (int i = 0; i < length; i++) {
			tempStreet1.blockingKStack = kstack; // block every street with the right KStack
			tempStreet1 = tempStreet1.prev1;
		}
		debugOutput("==========",2);
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
			eventList[i].getCar().checkForStartsStops();
	}
	
	
	private void addStreetToUsedByKStacks(Street street) {
		// if the list is empty this fills it with one item
		if (this.usedByKStacks == null) {
			this.usedByKStacks = new Street[1];
			this.usedByKStacks[0] = street;
		} else {
			// first it has to check whether that street is existing in the list
			// this is actually optional since duplicates in the list do not matter
//			boolean isInList = false;
//l6:			for (int i = 0; i < this.usedByKStacks.length; i++) {
//				if (this.usedByKStacks[i]==street) {
//					isInList = true;
//					break l6;
//				}
//			}
			// if item is not in the list it gets added
//			if (!isInList) {			
				Street tempStreet1[] = new Street[this.usedByKStacks.length+1];
				for (int i = 0; i < this.usedByKStacks.length; i++) {
					tempStreet1[i] = this.usedByKStacks[i];
				}
				tempStreet1[tempStreet1.length-1] = street;
				this.usedByKStacks = tempStreet1;
//			}
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
		
//		if (list[crossroadRoundRobinState].car != null) {
//			if (list[(crossroadRoundRobinState+1)%3].car != null)
//				list[(crossroadRoundRobinState+1)%3].car.disabled = true;
//			if (list[(crossroadRoundRobinState+2)%3].car != null)
//				list[(crossroadRoundRobinState+2)%3].car.disabled = true;
//		}
		
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
	 * kstacks are full the method returns a kstack that has the lowest number
	 * of cars even if it is locked. 
	 * @param testing if this is true the method will give always kstack[0]
	 * @return kstack to park in
	 */
	private int findSmallestStack(boolean testing) {
		if (!testing) {
			int indexUnlockedStacks = -1, indexLockedStacks = -1;
			int watermarkUnlockedStacks = kHeight, watermarkLockedStacks = kHeight;
			
			for (int i=0; i<parkingRows*6; i++) {
				// looking for unlocked kstack with lowest watermark
				if (kstack[i].watermark < watermarkUnlockedStacks && !kstack[i].lockedForParking && !kstack[i].disabled) {
					watermarkUnlockedStacks = kstack[i].watermark;
					indexUnlockedStacks = i;
				}
				// simultaneously looking for the kstack with lowest watermark
				// with no regards to whether the kstack is locked or not
				if (kstack[i].watermark < watermarkLockedStacks && !kstack[i].disabled) {
					watermarkLockedStacks = kstack[i].watermark;
					indexLockedStacks = i;
				}
			}
			// in case there is no unlocked kstack with lowest watermark
			if (indexUnlockedStacks == -1) {
				return indexLockedStacks;
			}
			return indexUnlockedStacks;
		}
		// when testing == true this always return kstack[0]
		return 0;
	}
	
	
	@SuppressWarnings("unused")
	private void printMidLane() {
		debugOutput("==========",2);
		debugOutput("Middle Lane:",2);
		Street tempStreet1 = this.spawn;
		while (tempStreet1.prev1 != null) {
			tempStreet1 = tempStreet1.prev1;
		}
		do {
			debugOutput("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack,2);
			tempStreet1 = tempStreet1.next1;
		} while(tempStreet1 != null);
		debugOutput("==========",2);
	}
	
	@SuppressWarnings("unused")
	private void printDespawn() {
		debugOutput("==========",2);
		debugOutput("Exit of the parking Lot:",2);
		Street tempStreet1 = this.crossroad;
		do {
			debugOutput("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack,2);
			tempStreet1 = tempStreet1.next1;
		} while (tempStreet1 != this.despawn);
		debugOutput("Street: "+this.despawn+", Car: "+this.despawn.car,2);
		debugOutput("==========",2);
	}
	
	
	@SuppressWarnings("unused")
	private void printStack(int stack) {
		debugOutput("==========",2);
		debugOutput("Stack "+stack+" (watermark: "+kstack[stack].watermark+"):",2);
		Street tempStreet1 = kstack[stack];
		debugOutput("Street: "+tempStreet1+", Car: "+tempStreet1.car+", lockedForUnparking: "+((KStack)tempStreet1).lockedForUnparking+", lockedForParking: "+((KStack)tempStreet1).lockedForParking+", blocking kstack: "+tempStreet1.blockingKStack,2);
		for (int i=0; i<(kHeight*carSize)-1; i++) {
			tempStreet1 = tempStreet1.next1;
			debugOutput("Street: "+tempStreet1+", Car: "+tempStreet1.car+", blocking kstack: "+tempStreet1.blockingKStack,2);
		}
		debugOutput("==========",2);
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
		debugOutput(stats[0]+","+stats[1]+","+stats[2]+","+stats[3]+","+stats[4]+","+(stats[4]-stats[2])+","+item.getCar().tilesMoved+","+item.getCar().startstop,1);
	}
	
	private void generateImage(String tick) throws Exception{
		int X = 2+this.parkingRows+this.carSize+this.carSize*this.kHeight, Y = (6*this.carSize*this.kHeight)+3;
		int x =0, y = 0, PIX_SIZE = 15;
		BufferedImage bi = new BufferedImage( PIX_SIZE * X, PIX_SIZE * Y, BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D g=(Graphics2D)bi.getGraphics();
		String filename =  "tick_"+tick+ "_img.png";
		
		// paint everything white
		for( int i = 0; i < X; i++ ){
            for( int j =0; j < Y; j++ ){
            	g.setColor(Color.WHITE);
            	g.fillRect(i * PIX_SIZE, j * PIX_SIZE, PIX_SIZE, PIX_SIZE);
            }
		}
		
		// paint unused areas near spawn black
		for (int i = 0; i < this.carSize*this.kHeight; i++) {
			for (int j = 0; j < Y; j++) {
				if (j != Y/2) {
	            	g.setColor(Color.BLACK);
	            	g.fillRect(i * PIX_SIZE, j * PIX_SIZE, PIX_SIZE, PIX_SIZE);
				}
			}
		}
		
		
		
		// paint unused areas near despawn black
		for (int i = this.carSize*this.kHeight+2+this.parkingRows; i < X; i++) {
			for (int j = 0; j < Y; j++) {
				if (j != Y/2) {
	            	g.setColor(Color.BLACK);
	            	g.fillRect(i * PIX_SIZE, j * PIX_SIZE, PIX_SIZE, PIX_SIZE);
				}
			}
		}
		

		
		// paint unused areas near the corners black
		for (int j = 0; j < this.carSize*this.kHeight; j++) {
        	g.setColor(Color.BLACK);
        	g.fillRect((this.carSize*this.kHeight) * PIX_SIZE, j*PIX_SIZE, PIX_SIZE, PIX_SIZE);
        	g.fillRect((this.carSize*this.kHeight+this.parkingRows+1) * PIX_SIZE, j*PIX_SIZE, PIX_SIZE, PIX_SIZE);
        	g.fillRect((this.carSize*this.kHeight) * PIX_SIZE, (Y-j-1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
        	g.fillRect((this.carSize*this.kHeight+this.parkingRows+1) * PIX_SIZE, (Y-j-1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
		}

		
		// paint middle streets gray or red (if blocked)
		Street tempStreet1 = spawn;
		while (tempStreet1.prev1 != null) {
			tempStreet1 = tempStreet1.prev1;
		}
		for (int i = 0; i < parkingRows+2+kHeight*carSize+carSize; i++) {
			if (tempStreet1.car != null) {
				g.setColor(tempStreet1.car.getColor());
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect(i*PIX_SIZE, (Y/2)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			if (tempStreet1.next1 != null) // this should only catch the last case when the pointer already reached despawn
				tempStreet1 = tempStreet1.next1;
		}
		

		// paint left vertical streets gray or red (if blocked)
		tempStreet1 = spawn.next2;
		Street tempStreet2 = spawn.next3;
		for (int i = 1; i < 2*kHeight*carSize+2; i++) {
			if (tempStreet1.car != null) {
				g.setColor(tempStreet1.car.getColor());
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect(carSize*kHeight*PIX_SIZE, (((Y/2))-i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				g.setColor(tempStreet2.car.getColor());
			} else if (tempStreet2.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect(carSize*kHeight*PIX_SIZE, (((Y/2))+i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			tempStreet1 = tempStreet1.next1;
			tempStreet2 = tempStreet2.next1;
		}
		
		// use the position of the streets and keep painting upper and lower horizontal streets
		for (int i = 0; i < parkingRows; i++) {
			if (tempStreet1.car != null) {
				g.setColor(tempStreet1.car.getColor());
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((carSize*kHeight+1+i)*PIX_SIZE, (((Y/2))-2*carSize*kHeight-1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				g.setColor(tempStreet2.car.getColor());
			} else if (tempStreet2.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((carSize*kHeight+1+i)*PIX_SIZE, (((Y/2))+2*carSize*kHeight+1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			tempStreet1 = tempStreet1.next1;
			tempStreet2 = tempStreet2.next1;
		}
		
		// and now the right vertical streets
		for (int i = 2*kHeight*carSize+1; i > 0; i--) {
			if (tempStreet1.car != null) {
				g.setColor(tempStreet1.car.getColor());
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((carSize*kHeight+parkingRows+1)*PIX_SIZE, (((Y/2))-i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				g.setColor(tempStreet2.car.getColor());
			} else if (tempStreet2.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((carSize*kHeight+parkingRows+1)*PIX_SIZE, (((Y/2))+i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			tempStreet1 = tempStreet1.next1;
			tempStreet2 = tempStreet2.next1;
		}

		
		
		
				
		
		// paint cars in the middle kstacks
		for (int i = 0; i < (kstack.length/3); i++) {
			y = (Y/2);
			x = this.carSize*this.kHeight+1;
			tempStreet1 = spawn.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y--;
					}
					break l2;
					
				} else if (kstack[i] == tempStreet1.kstack2) {
					tempStreet1 = tempStreet1.kstack2;
					y++;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y++;
					}
					break l2;
						
				}else {
					tempStreet1 = tempStreet1.next1;
					x++;
				}
			}
		}
		
		// paint cars in the top kstacks
		for (int i = kstack.length/3; i < 2*(kstack.length/3); i++) {
			y = carSize*kHeight;
			x = this.carSize*this.kHeight+1;
			tempStreet1 = spawn.next2;
			while (tempStreet1.kstack1 == null)
				tempStreet1 = tempStreet1.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y--;
					}
					break l2;
					
				} else if (kstack[i] == tempStreet1.kstack2) {
					tempStreet1 = tempStreet1.kstack2;
					y++;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y++;
					}
					break l2;
						
				}else {
					tempStreet1 = tempStreet1.next1;
					x++;
				}
			}
		}
		
		// paint cars in the bottom kstacks
		for (int i = 2*(kstack.length/3); i < kstack.length; i++) {
			y = Y-(carSize*kHeight)-1;
			x = this.carSize*this.kHeight+1;
			tempStreet1 = spawn.next3;
			while (tempStreet1.kstack1 == null)
				tempStreet1 = tempStreet1.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y--;
					}
					break l2;
					
				} else if (kstack[i] == tempStreet1.kstack2) {
					tempStreet1 = tempStreet1.kstack2;
					y++;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
							g.setColor(tempStreet1.car.getColor());
							g.fillRect(x*PIX_SIZE, y*PIX_SIZE, PIX_SIZE, PIX_SIZE);
						}
						tempStreet1 = tempStreet1.next1;
						y++;
					}
					break l2;
						
				}else {
					tempStreet1 = tempStreet1.next1;
					x++;
				}
			}
		}
		// save all the images!!
		g.dispose();
        saveToFile( bi, new File( filename ) );
	}
	
	
	/**
	 * This method saves an image from state of the parking lot to an assigned
	 * destination.
	 * @param img the image itself
	 * @param file file where the image is supposed to go
	 * @throws IOException something can always go wrong
	 */
	private void saveToFile( BufferedImage img, File file ) throws IOException {
		ImageWriter writer = null;
		@SuppressWarnings("rawtypes")
		java.util.Iterator iter = ImageIO.getImageWritersByFormatName("png");
		if( iter.hasNext() ){
		    writer = (ImageWriter)iter.next();
		}
		ImageOutputStream ios = ImageIO.createImageOutputStream( file );
		writer.setOutput(ios);
		ImageWriteParam param = new JPEGImageWriteParam( java.util.Locale.getDefault() );
		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT) ;
		param.setCompressionQuality(0.98f);
		writer.write(null, new IIOImage( img, null, null ), param);
    }
	
	/**
	 * The debug output happens here. Depending on the set verbose level of the
	 * simulator and the priority the messages come in with this method prints
	 * them out or discards them.
	 * @param text text, which should be printed
	 * @param priority has to be less or equal to verbose level to get printed
	 */
	private void debugOutput(String text, int priority) {
		if (priority <= this.verboseLevel) {
			System.out.println(text);
		}
	}
}
