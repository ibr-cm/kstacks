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
	private boolean debug;
	private boolean visualOutput;
	private int verboseLevel;
	
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
		this.visualOutput = false;
		this.verboseLevel = 2;
	}
	
	public void runSimulator(int maxTick, boolean debug, boolean visualOutput, int verboseLevel) {
		
		long time = System.currentTimeMillis();
		
		this.debug = debug;
		
		this.verboseLevel = Math.min(Math.max(0, verboseLevel),2);
//		System.out.println(verboseLevel);
		this.visualOutput = visualOutput;

		
		while(!eventsFinished() && (tick<maxTick || maxTick == 0)) {
			
			if (visualOutput) {
				try {
					generateImage(Integer.toString(tick)+"_0");
				} catch (Exception e) {debugOutput(""+e,2);}
			}
			
			
			debugOutput("=============================================================",2);
			debugOutput("Tick: "+tick,2);
			debugOutput("=============================================================",2);
			
			checkForStreetBlocking();			
			
			moveCars();
			debugOutput("Moved Cars",2);
			
			checkForStartsStops();
			
			refreshStreets();
			
			despawnCar();
			
			checkForSpawns();
			
			spawnCar();
			
			checkForUnparkingEvents();
			
//			debugOutput("kstack 0 parking: "+kstack[0].lockedForParking+", unparking: "+kstack[0].lockedForUnparking,2);
//			debugOutput("kstack 1 parking: "+kstack[1].lockedForParking+", unparking: "+kstack[1].lockedForUnparking,2);
//			debugOutput("kstack 2 parking: "+kstack[2].lockedForParking+", unparking: "+kstack[2].lockedForUnparking,2);
//			debugOutput("kstack 3 parking: "+kstack[3].lockedForParking+", unparking: "+kstack[3].lockedForUnparking,2);
			
			tick++;
			debugOutput("=============================================================",2);
		}
		
		System.out.println(System.currentTimeMillis()-time);
	}
	
	
	private boolean eventsFinished() {
		for (int i=0; i<totalCarsUsed; i++) {
			if (!eventList[i].fulfilled)
				return false;
		}
		return true;
	}
	
	// 
	private void despawnCar() {
		if (despawn.car != null) {
			debugOutput("==========",2);
			debugOutput("Just despawned car "+despawn.car,2);
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
			tempCar.isInParkingLot = false;
			printEventItem(tempCar.eventItem);
		}
	}
	
	
	// checks for new cars which should spawn now and puts them in a queue
	private void checkForSpawns() {
		for (int i=0; i<totalCarsUsed; i++) {
			
			// check if a car is supposed to spawn
			if (eventList[i].entryTime == tick) {
				debugOutput("==========",2);
				debugOutput("checkForSpawns: Put a car on spawn list at "+this.tick+" tick(s).",2);
				
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
					debugOutput("checkForSpawn: a car tried to spawn but parkingLot is full",2);
					debugOutput("checkForSpawn: event: "+eventList[i],2);
					debugOutput("checkForSpawn: entryTime: "+eventList[i].entryTime+", backOrderTime: "+eventList[i].backOrderTime+", exitTime"+eventList[i].exitTime,2);
					return;
				}
				
				// the spawning of this car will be appended to the list of spawns
				int j = 0;
				while (spawnList[j] != null) {
					j++;
				}
				spawnList[j] = eventList[i];
				debugOutput("==========",2);
			}
		}
	}
	
	
	// spawns the next car in the queue (in case there is one)
	private void spawnCar() {
		if (spawnList[0] != null) {
			debugOutput("==========",2);
			debugOutput("spawnCar: Car is on spawn list.",2);
			
			// check if spawn is free -- possible cases
			// spawn blocked by kstack (unparking)
			// spawn blocked by car (previous spawn)
			// kstack of spawning car is not accessible (because it is unparking)
			boolean spawnBlocked = false;
			if (spawn.blockingKStack != null || spawn.car != null || spawn.carAtLastTick != null || (kHeight*carSize>2?spawn.prev1.car != null:false)) {
				debugOutput("spawnCar: Cannot spawn since spawn is blocked!",2);
				spawnBlocked = true;
			}
			
			debugOutput("spawnCar: Spawn was "+(spawnBlocked?"":"not ")+"blocked.",2);
			
			if (!spawnBlocked) {
				
				// index of the stack with the smallest amount of cars
				// the boolean is for debug purposes -- if set to TRUE the
				// simulator tries to stack all into kstack[0]
				int index = findSmallestStack(this.debug);
				debugOutput("spawnCar: Car was put on stack "+index+" ("+this.kstack[index]+").",2);
				
				// assign kstack to a car
				spawnList[0].car.kstack = kstack[index];
				if (index/(kstack.length/3)==0) {
					spawnList[0].car.lane = spawn.next1;
				} else if (index/(kstack.length/3)==1) {
					spawnList[0].car.lane = spawn.next2;
				} else {
					spawnList[0].car.lane = spawn.next3;
				}
				
				// assign parkingSpot to the car
				debugOutput("spawnCar: assigned parking spot: "+spawnList[0].car.parkingSpot,2);
				spawnList[0].car.parkingSpot = kstack[index].watermark;
				
				// increment the cars stored in this kstack
				kstack[index].watermark++;
				debugOutput("spawnCar: watermark of "+this.kstack[index]+" now "+this.kstack[index].watermark+".",2);
								
				
				// put the car down at the spawn
				spawnList[0].car.spawn();
				
				// setup driving target, which is essential the final parking position
				Street tempStreet1 = spawnList[0].car.kstack;
				
				// the kstack is locked for unparking but not for parking 
				spawnList[0].car.kstack.lockedForUnparking = true;
//				debugOutput("spawnCar: carSize "+carSize);
				debugOutput("spawnCar: watermark of "+tempStreet1+" is "+spawnList[0].car.parkingSpot+" and lockedForUnparking is "+spawnList[0].car.kstack.lockedForUnparking,2);
				// fin the right spot where to park exactly
				tempStreet1 = getParkingSpot(spawnList[0].car, spawnList[0].car.kstack);
				
				debugOutput("spawnCar: Final parking position: "+tempStreet1,2);
				DrivingTarget[] targets = new DrivingTarget[1];
				targets[0] = new DrivingTarget(tempStreet1, 'D', spawnList[0].car.kstack, null, false, unparkingList, null, false);
				spawnList[0].car.setDrivingTargets(targets);
				
				// shift the whole list one item down
				for (int i=0; i<totalCarsUsed-1; i++) {
					spawnList[i] = spawnList[i+1];
				}
				spawnList[totalCarsUsed-1] = null;
				
				if (spawnList[0] == null) {
					debugOutput("spawnCar: Spawn list is now empty.",2);
				} else {
					debugOutput("spawnCar: New latest item on spawn list is "+spawnList[0],2);
				}
				
			}
			debugOutput("==========",2);
		}
	}
	
	private Street getParkingSpot(Car car, Street kstack) {
		Street tempStreet1 = kstack;
//		debugOutput("==========",2);
//		debugOutput("getParkingSpot: kstack = "+kstack);
		while (tempStreet1.next1 != null) {
			tempStreet1 = tempStreet1.next1;
		}
//		debugOutput("getParkingSpot: tempStreet = "+tempStreet1);
//		debugOutput("getParkingSpot: car.parkingSpot = "+car.parkingSpot);
		if (car.parkingSpot != 0) {
			for (int i = 0; i < (car.parkingSpot*carSize); i++) {
				tempStreet1 = tempStreet1.prev1;
			}
		}
//		debugOutput("getParkingSpot: tempStreet = "+tempStreet1);
//		debugOutput("==========",2);
		return tempStreet1;
	}
	
	private void moveCars() {
		for (int i=0; i<carList.length; i++) {
			if (carList[i].isInParkingLot) {
				debugOutput("==========",2);
				debugOutput("Trying to move car "+carList[i],2);
				carList[i].drive();
				debugOutput("==========",2);
			}
		}
	}
	
	private void checkForUnparkingEvents() {
		// Create a new UnparkEvent for every car that wants to unpark.
		for (int i=0; i<totalCarsUsed; i++) {
			if (eventList[i].backOrderTime + eventList[i].backOrderDelay == tick) {
				if (eventList[i].car.kstack.lockedForUnparking) {
					debugOutput("==========",2);
					eventList[i].backOrderDelay++;
					debugOutput("checkForUnparkingEvents: was delayed because the kstack was locked",2);
					debugOutput("==========",2);
				} else {
					debugOutput("==========",2);
					debugOutput("scheduleUnparking: Creating an unparking Event at "+this.tick+" tick(s).",2);
					UnparkEvent newUnparkEvent = new UnparkEvent();
					newUnparkEvent.setCarSize(carSize);
					newUnparkEvent.setCarToUnpark(eventList[i].car); // Car that wants to leave its spot.
					newUnparkEvent.setKStack();
					newUnparkEvent.kstack.watermark--;
					newUnparkEvent.kstack.lockedForParking = true;
					newUnparkEvent.kstack.lockedForUnparking = true;
					
					eventList[i].car.unparking = true;
					
					int counter = 0;
					
					// decide whether there is one car which has to unpark or more than one car
					Street tempStreet1 = eventList[i].car.currentStreet;
					while (tempStreet1.car == eventList[i].car)
						tempStreet1 = tempStreet1.prev1;
					
					// only one car unparking
					if (tempStreet1.car == null) {
						debugOutput("checkForUnparkingEvents: found just 1 car!",2);
						newUnparkEvent.carsInTheWay = 0;
						newUnparkEvent.firstInQueue = eventList[i].car;
						
						
						// find street right before entering the street
						Street tempStreet2 = eventList[i].car.kstack;
						if (carSize > 1) {
							for (int j = 0; j < carSize-1; j++) {
								tempStreet2 = tempStreet2.next1;
							}
						}
							
						
						DrivingTarget[] tempTarget1 = new DrivingTarget[2];
						// drive to the right position 
						tempTarget1[0] = new DrivingTarget(newUnparkEvent.carToUnpark.kstack.prev1, 'R', newUnparkEvent.kstack, newUnparkEvent.kstack, true, unparkingList, newUnparkEvent, false);
						tempTarget1[1] = new DrivingTarget(despawn, 'D', null, null, false, unparkingList, null, false);
						
						eventList[i].car.drivingTarget = tempTarget1;
						
						
					} else { // more than one car
						debugOutput("checkForUnparkingEvents: found more than 1 car!",2);
						
						// the new targets for the car which unparks
						DrivingTarget[] tempTarget1 = new DrivingTarget[2];
						tempTarget1[0] = new DrivingTarget(newUnparkEvent.carToUnpark.kstack.prev1, 'R', null, newUnparkEvent.carToUnpark.kstack, false, unparkingList, newUnparkEvent, false);
						tempTarget1[1] = new DrivingTarget(despawn, 'D', null, null, false, unparkingList, null, false);
						eventList[i].car.drivingTarget = tempTarget1;
						
						// all other cars
						Car tempCar1 = eventList[i].car;
						newUnparkEvent.carsInTheWay = 0;
						
						while (tempStreet1.car != null) {
							debugOutput("checkForUnparkingEvents: tempStreet1.car = "+tempStreet1.car,2);
							if (tempStreet1.car != null && tempStreet1.car != tempCar1) {
								newUnparkEvent.carsInTheWay++;
								// set the pointer to the next ..
								tempCar1 = tempStreet1.car;
								// .. and increase the count of cars which are in the same stack behind the car that wants to unpark
								counter++;
								
								newUnparkEvent.firstInQueue = tempCar1;
								
								DrivingTarget tempDrivingTarget[] = new DrivingTarget[3];
								tempStreet1.car.parkingSpot--;
								debugOutput("scheduleUnparking: new targets for car "+tempStreet1.car+": "+unparkingSpot(counter, newUnparkEvent.kstack)+", "+newUnparkEvent.carToUnpark.currentStreet,2);
								tempDrivingTarget[0] = new DrivingTarget(unparkingSpot(counter, newUnparkEvent.kstack), 'R', null, newUnparkEvent.kstack, false, unparkingList, null, false);
								tempDrivingTarget[1] = new DrivingTarget(unparkingSpot(counter, newUnparkEvent.kstack), 'N', null, null, false, unparkingList, null, false);
								tempDrivingTarget[2] = new DrivingTarget(getParkingSpot(tempStreet1.car, tempStreet1.car.kstack), 'D', null, null, false, unparkingList, null, false);
								tempCar1.drivingTarget = tempDrivingTarget;
								
							}
							tempStreet1 = tempStreet1.prev1;
						}
						newUnparkEvent.firstInQueue.drivingTarget[2].unlockKStackForUnparking = newUnparkEvent.kstack;
						newUnparkEvent.firstInQueue.drivingTarget[2].continousUnblocking = true;
					}
					this.unparkingList.addEvent(newUnparkEvent);
					debugOutput("==========",2);
				}
			}
		}
	}
					
					
					
					
					
					
		
	
	private Street unparkingSpot(int spot, Street kstack) {
//		debugOutput("unparkingHelpSpot: spot = "+spot+", carSize = "+carSize+", spot*carSize = "+(spot*carSize));
		Street tempStreet1 = kstack.prev1;
		for (int i = 0; i < spot*carSize; i++) {
			tempStreet1 = tempStreet1.prev1;
		}
		return tempStreet1;
	}
	
	
	private void checkForStreetBlocking() {
//		debugOutput("checkForStreetBlocking ausgefuehrt");
		UnparkEvent tempUnparkEvent1 = unparkingList;
		while (tempUnparkEvent1.next != null) {
			debugOutput("==========",2);
			debugOutput("checkForStreetBlocking: checking "+tempUnparkEvent1,2);
			tempUnparkEvent1 = tempUnparkEvent1.next;
			
			// Cars are now at the street.
			if (tempUnparkEvent1.kstack.car != null && !tempUnparkEvent1.doneBlocking) {
				debugOutput("checkForStreetBlocking: car is at the street ("+tempUnparkEvent1+")",2);
				Street tempStreet1 = tempUnparkEvent1.kstack.prev1;
				
				// Checking if the street is blocked.
				boolean spaceIsFree = true;
				debugOutput("checkForStreetBlocking: checking  for space "+((tempUnparkEvent1.carsInTheWay+1)*this.carSize),2);
				for (int i = 0; i < (tempUnparkEvent1.carsInTheWay+1)*this.carSize; i++) {
					if (tempStreet1.car != null || tempStreet1.blockingKStack != null) {
						spaceIsFree = false;
					}
					tempStreet1 = tempStreet1.prev1;
				}
				debugOutput("checkForStreetBlocking: spaceIsFree? "+spaceIsFree,2);
				
				// If not the street can be blocked.
				if (spaceIsFree) { // && tempUnparkEvent1.carsInTheWay == 0) {
					blockStreets(tempUnparkEvent1.kstack, ((tempUnparkEvent1.carsInTheWay+1)*this.carSize));
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
		
	
	// Blocking a certain amount of tiles
	public void blockStreets(KStack kstack, int length) {
		debugOutput("==========",2);
		debugOutput("blockStreets: kStack: "+kstack+", length: "+length,2);
		Street tempStreet1 = kstack.prev1;
		for (int i = 0; i < length; i++) {
			tempStreet1.blockingKStack = kstack;
			tempStreet1 = tempStreet1.prev1;
		}
		debugOutput("==========",2);
	}

	
	
	
	private void refreshStreets() {
		Street tempStreet1 = spawn;
		while (tempStreet1.prev1 != null) {
			tempStreet1 = tempStreet1.prev1;
		}
		tempStreet1.refresh(this.tick);
		while (tempStreet1.next1 != null) {
			tempStreet1 = tempStreet1.next1;
			tempStreet1.refresh(this.tick);
		}
		tempStreet1 = spawn.next2;
		tempStreet1.refresh(this.tick);
		while (tempStreet1.next1 != null) {
			tempStreet1 = tempStreet1.next1;
			tempStreet1.refresh(this.tick);
		}
		tempStreet1 = spawn.next3;
		tempStreet1.refresh(this.tick);
		while (tempStreet1.next1 != null) {
			tempStreet1 = tempStreet1.next1;
			tempStreet1.refresh(this.tick);
		}
	}
	
	
	private void checkForStartsStops() {
		for (int i = 0; i < carList.length; i++)
			carList[i].checkForStartsStops(this.tick);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private int findSmallestStack(boolean testing) {
		if (!testing) {
			int index = -1, indexFallback = -1;
			int watermark = kHeight, watermarkFallback = kHeight;
			
			for (int i=0; i<parkingRows*6; i++) {
				// looking for unlocked kstack with lowest watermark
				if (kstack[i].watermark < watermark && !kstack[i].lockedForParking) {
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
	
	private void printEventItem(EventItem item) {
		if (verboseLevel == 1)
			debugOutput("==========",1);
		debugOutput("EventItem "+item+" of car "+item.car,1);
		debugOutput("Entry Time: "+item.entryTime,1);
		debugOutput("BackOrder Time: "+item.backOrderTime,1);
		debugOutput("Exit Time: "+item.exitTime,1);
		debugOutput("Delayed due to parking: "+item.backOrderDelay,1);
		debugOutput("Moved Tiles: "+item.car.tilesMoved,1);
		debugOutput("Starts, Stops: "+item.car.startstop, 1);
		debugOutput("==========",1);
	}
	
	private void generateImage(String tick) throws Exception{
		int X = 2+this.parkingRows+this.carSize+this.carSize*this.kHeight, Y = (6*this.carSize*this.kHeight)+3;
		int x =0, y = 0, PIX_SIZE = 15;
		BufferedImage bi = new BufferedImage( PIX_SIZE * X, PIX_SIZE * Y, BufferedImage.TYPE_3BYTE_BGR );
		Graphics2D g=(Graphics2D)bi.getGraphics();
		String filename =  "tick_"+tick+ "_img.jpg";
		
		// paint everything white
		for( int i = 0; i < X; i++ ){
            for( int j =0; j < Y; j++ ){
            	x = i * PIX_SIZE;
                y = j * PIX_SIZE;
            	g.setColor(Color.WHITE);
            	g.fillRect(x, y, PIX_SIZE, PIX_SIZE);
            }
		}
		
		// paint unused areas near spawn black
		for (int i = 0; i < this.carSize*this.kHeight; i++) {
			for (int j = 0; j < Y; j++) {
				if (j != Y/2) {
					x = i * PIX_SIZE;
	                y = j * PIX_SIZE;
	            	g.setColor(Color.BLACK);
	            	g.fillRect(x, y, PIX_SIZE, PIX_SIZE);
				}
			}
		}
		
		
		
		// paint unused areas near despawn black
		for (int i = this.carSize*this.kHeight+2+this.parkingRows; i < X; i++) {
			for (int j = 0; j < Y; j++) {
				if (j != Y/2) {
					x = i * PIX_SIZE;
	                y = j * PIX_SIZE;
	            	g.setColor(Color.BLACK);
	            	g.fillRect(x, y, PIX_SIZE, PIX_SIZE);
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
				Color tempColor = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
				g.setColor(tempColor);
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
				Color tempColor = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
				g.setColor(tempColor);
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect(carSize*kHeight*PIX_SIZE, (((Y/2))-i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				Color tempColor = new Color(0, tempStreet2.car.color[1], tempStreet2.car.color[2]);
				g.setColor(tempColor);
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
				Color tempColor = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
				g.setColor(tempColor);
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((carSize*kHeight+1+i)*PIX_SIZE, (((Y/2))-2*carSize*kHeight-1)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				Color tempColor = new Color(0, tempStreet2.car.color[1], tempStreet2.car.color[2]);
				g.setColor(tempColor);
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
				Color tempColor = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
				g.setColor(tempColor);
			} else if (tempStreet1.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((carSize*kHeight+parkingRows+1)*PIX_SIZE, (((Y/2))-i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			if (tempStreet2.car != null) {
				Color tempColor = new Color(0, tempStreet2.car.color[1], tempStreet2.car.color[2]);
				g.setColor(tempColor);
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
							Color tempColor1 = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
							g.setColor(tempColor1);
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
							Color tempColor1 = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
							g.setColor(tempColor1);
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
							Color tempColor1 = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
							g.setColor(tempColor1);
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
							Color tempColor1 = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
							g.setColor(tempColor1);
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
							Color tempColor1 = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
							g.setColor(tempColor1);
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
							Color tempColor1 = new Color(0, tempStreet1.car.color[1], tempStreet1.car.color[2]);
							g.setColor(tempColor1);
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
		
		
		g.dispose();
        saveToFile( bi, new File( filename ) );
	}
	
	
	
	private void saveToFile( BufferedImage img, File file ) throws IOException {
		ImageWriter writer = null;
		@SuppressWarnings("rawtypes")
		java.util.Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
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
	
	
	private void debugOutput(String text, int priority) {
		if (priority <= this.verboseLevel) {
			System.out.println(text);
		}
	}
}
