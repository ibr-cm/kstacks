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
//		Car testCar1 = new Car();
//		spawn.next1.next1.next1.next1.car = testCar1;
//		spawn.next1.next1.next1.next1.next1.car = testCar1;
//		spawn.next1.next1.next1.next1.next1.next1.car = testCar1;
//		spawn.next1.next1.next1.next1.next1.next1.next1.car = testCar1;
//		
//		Car testCar2 = new Car();
//		kstack[0].next1.next1.car = testCar2;
//		kstack[0].next1.next1.next1.car = testCar2;
//		kstack[0].next1.next1.next1.next1.car = testCar2;
//		kstack[0].next1.next1.next1.next1.next1.car = testCar2;
//		
//		Car testCar3 = new Car();
//		kstack[1].car = testCar3;
//		kstack[1].next1.car = testCar3;
//		kstack[1].next1.next1.car = testCar3;
//		kstack[1].next1.next1.next1.car = testCar3;
//		
//		Car testCar4 = new Car();
//		kstack[4].next1.car = testCar4;
//		kstack[4].next1.next1.car = testCar4;
//		kstack[4].next1.next1.next1.car = testCar4;
//		kstack[4].next1.next1.next1.next1.car = testCar4;
//		
//		Car testCar5 = new Car();
//		kstack[6].car = testCar5;
//		kstack[6].next1.car = testCar5;
//		kstack[6].next1.next1.car = testCar5;
//		kstack[6].next1.next1.next1.car = testCar5;
//		
//		Car testCar6 = new Car();
//		kstack[12].car = testCar6;
//		kstack[12].next1.car = testCar6;
//		kstack[12].next1.next1.car = testCar6;
//		kstack[12].next1.next1.next1.car = testCar6;
		
		try {
			generateImage(Integer.toString(tick)+"_pre");
		} catch (Exception e) {System.out.println(e);}

		
		while(!eventsFinished() && tick<60) {
			
			try {
				generateImage(Integer.toString(tick)+"_0");
			} catch (Exception e) {System.out.println(e);}
			
			
			System.out.println("=============================================================");
			System.out.println("Tick: "+tick);
			System.out.println("=============================================================");
			System.out.println();
			
			checkForStreetBlocking();
			
//			try {
//				generateImage(Integer.toString(tick)+"_1");
//			} catch (Exception e) {System.out.println(e);}
			
			
			moveCars();
			System.out.println("Moved Cars");
			System.out.println();
			
//			try {
//				generateImage(Integer.toString(tick)+"_2");
//			} catch (Exception e) {System.out.println(e);}
			
			despawnCar();
			
//			try {
//				generateImage(Integer.toString(tick)+"_3");
//			} catch (Exception e) {System.out.println(e);}
			
			checkForSpawns();
			
//			try {
//				generateImage(Integer.toString(tick)+"_4");
//			} catch (Exception e) {System.out.println(e);}
			
			spawnCar();
			
//			try {
//				generateImage(Integer.toString(tick)+"_5");
//			} catch (Exception e) {System.out.println(e);}
			
			checkForUnparkingEvents();
			
//			printMidLane();
//			printStack(0);
//			printStack(1);
//			printStack(2);
//			printStack(3);
//			printStack(4);
//			printStack(5);
//			printDespawn();
			
			//checkForStreetUnblocking();
			
			System.out.println(kstack[0].lockedForParking);
			System.out.println(kstack[0].lockedForUnparking);
			
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
				// the boolean is for debug purposes -- if set to TRUE the
				// simulator tries to stack all into kstack[0]
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
				System.out.println(eventList[i].car.parkingSpot);
				eventList[i].car.parkingSpot = kstack[index].watermark;
				
				// increment the cars stored in this kstack
				kstack[index].watermark++;
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
			if (spawn.blockingKStack != null || spawn.car != null || (kHeight*carSize>2?spawn.prev1.car != null:false) || spawnList[0].car.kstack.lockedForParking) {
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
				spawnList[0].car.kstack.lockedForUnparking = true;
				System.out.println("spawnCar: carSize "+carSize);
				System.out.println("spawnCar: watermark of "+tempStreet1+" is "+spawnList[0].car.parkingSpot+" and lockedForUnparking is "+spawnList[0].car.kstack.lockedForUnparking);
				// fin the right spot where to park exactly
				tempStreet1 = getParkingSpot(spawnList[0].car, spawnList[0].car.kstack);
				
//				
//				while (tempStreet1.next1 != null) {
//					tempStreet1 = tempStreet1.next1;
//				}
//				if (spawnList[0].car.parkingSpot != 0) {
//					for (int i = 0; i < spawnList[0].car.parkingSpot*carSize; i++) {
//						tempStreet1 = tempStreet1.prev1;
//					}
//				}
				
				System.out.println("spawnCar: Final parking position: "+tempStreet1);
				targets[0] = new DrivingTarget(tempStreet1, 'D', spawnList[0].car.kstack, null, false, unparkingList, null);
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
	
	private Street getParkingSpot(Car car, Street kstack) {
		Street tempStreet1 = kstack;
//		System.out.println("getParkingSpot: kstack = "+kstack);
		while (tempStreet1.next1 != null) {
			tempStreet1 = tempStreet1.next1;
		}
//		System.out.println("getParkingSpot: tempStreet = "+tempStreet1);
//		System.out.println("getParkingSpot: car.parkingSpot = "+car.parkingSpot);
		if (car.parkingSpot != 0) {
			for (int i = 0; i < (car.parkingSpot*carSize); i++) {
				tempStreet1 = tempStreet1.prev1;
			}
		}
//		System.out.println("getParkingSpot: tempStreet = "+tempStreet1);
		return tempStreet1;
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
			if (eventList[i].backOrderTime + eventList[i].backOrderDelay == tick) {
				if (eventList[i].car.kstack.lockedForUnparking) {
					System.out.println("checkForUnparkingEvents: was delayed because the kstack was locked");
					eventList[i].backOrderDelay++;
					return;
				}
				System.out.println("scheduleUnparking: Creating an unparking Event at "+this.tick+" tick(s).");
				UnparkEvent newUnparkEvent = new UnparkEvent();
				newUnparkEvent.setCarSize(carSize);
				newUnparkEvent.setCarToUnpark(eventList[i].car); // Car that wants to leave its spot.
				newUnparkEvent.setKStack();
				newUnparkEvent.kstack.watermark--;
				newUnparkEvent.kstack.lockedForParking = true;
				newUnparkEvent.kstack.lockedForUnparking = true;
				
				
				// The car needs new coordinates where it is supposed to go.
				// So the kStack the car is assigned to is deleted from the cars object and the new coordinates are put in.
				DrivingTarget[] tempTarget1 = new DrivingTarget[2];
				tempTarget1[0] = new DrivingTarget(newUnparkEvent.carToUnpark.kstack.prev1, 'R', null, newUnparkEvent.kstack, true, unparkingList, newUnparkEvent);
				tempTarget1[1] = new DrivingTarget(despawn, 'D', null, null, false, unparkingList, null);
				//System.out.println(tempTarget1[0].street+" "+tempTarget1[0].direction);
				//System.out.println(tempTarget1[1].street+" "+tempTarget1[1].direction);
				eventList[i].car.drivingTarget = tempTarget1;
				eventList[i].car.unparking = true;
//				newUnparkEvent.carToUnpark.kstack = null;
				
				
				// Get a list of cars that need to be unparked. The car closest to the street (in case there is any) will be
				// made known to the UnparkEvent. So this can observe if the cars already proceeded up to the street.
				// Otherwise cars will be moved up to the street first and block the street only if they are entering it right away.
				Street tempStreet1 = eventList[i].car.currentStreet;
				Car tempCar1 = eventList[i].car;
				System.out.println("scheduleUnparking: "+tempCar1+" is going to unpark.");
				int counter = 0;
l3:				while (tempCar1 != null) {
					while (tempStreet1.car == tempCar1) {
						// Move to the next parking space (depends on the car size). Cars with different length could be used.55x3
						tempStreet1 = tempStreet1.prev1;
					}
					if (tempStreet1.car == null) {
						// So now the car closest to the road is found. In case this is the car, which tries to unpark, the UnparkEvent
						// will handle that.
						newUnparkEvent.firstInQueue = tempCar1;
						if (tempCar1 != eventList[i].car) {
							tempCar1.drivingTarget[2].unlockKStackForUnparking = newUnparkEvent.kstack;
							System.out.println("car: "+tempCar1);
						}
						break l3; // set tempCar1 to null to break the while-loop
					} else {
						// set the pointer to the next ..
						tempCar1 = tempStreet1.car;
						// .. and increase the count of cars which are in the same stack behind the car that wants to unpark
						counter++;
						DrivingTarget tempDrivingTarget[] = new DrivingTarget[3];
						tempStreet1.car.parkingSpot--;
						System.out.println("scheduleUnparking: new targets for car "+tempStreet1.car+": "+unparkingHelpSpot(counter, newUnparkEvent.kstack)+", "+newUnparkEvent.carToUnpark.currentStreet);
						tempDrivingTarget[0] = new DrivingTarget(unparkingHelpSpot(counter, newUnparkEvent.kstack), 'R', null, newUnparkEvent.kstack, true, unparkingList, null);
						tempDrivingTarget[1] = new DrivingTarget(unparkingHelpSpot(counter, newUnparkEvent.kstack), 'N', null, null, false, unparkingList, null);
						tempDrivingTarget[2] = new DrivingTarget(getParkingSpot(tempStreet1.car, tempStreet1.car.kstack), 'D', null, null, false, unparkingList, null);
						tempStreet1.car.drivingTarget = tempDrivingTarget;
						// TODO: according to the counter the final position can be calculated the car has to reach to let the
						// car go, which unparks.
					}
					newUnparkEvent.carsInTheWay = counter;
				}
				if (tempCar1 == eventList[i].car) {
					tempCar1.drivingTarget[0].unlockKStackForUnparking = newUnparkEvent.kstack;
					System.out.println("car: "+tempCar1);
				}
				System.out.println("scheduleUnparking: "+counter+" cars are in the way.");
				this.unparkingList.addEvent(newUnparkEvent);
			}
		}
	}
	
	private Street unparkingHelpSpot(int spot, Street kstack) {
//		System.out.println("unparkingHelpSpot: spot = "+spot+", carSize = "+carSize+", spot*carSize = "+(spot*carSize));
		Street tempStreet1 = kstack;
		for (int i = 0; i < ((spot*carSize)+1); i++) {
			tempStreet1 = tempStreet1.prev1;
		}
		return tempStreet1;
	}
	
	
	private void checkForStreetBlocking() {
//		System.out.println("checkForStreetBlocking ausgefuehrt");
		// TODO: Make the first car in the queue block the kstack and unblock it when back at the right parking spot.
		UnparkEvent tempUnparkEvent1 = unparkingList;
		while (tempUnparkEvent1.next != null) {
			System.out.println("checkForStreetBlocking: checking "+tempUnparkEvent1);
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
				System.out.println("checkForStreetBlocking: car is at the street ("+tempUnparkEvent1+")");
				Street tempStreet1 = tempUnparkEvent1.kstack.prev1;
				
				// Checking if the street is blocked.
				boolean spaceIsFree = true;
				System.out.println("checkForStreetBlocking: checking  for space "+((tempUnparkEvent1.carsInTheWay+1)*this.carSize));
				for (int i = 0; i < (tempUnparkEvent1.carsInTheWay+1)*this.carSize; i++) {
					if (tempStreet1.car != null) {
						spaceIsFree = false;
					}
				}
				System.out.println("checkForStreetBlocking: blocked? "+spaceIsFree);
				
				// If not the street can be blocked.
				if (spaceIsFree) { // && tempUnparkEvent1.carsInTheWay == 0) {
					blockStreets(tempUnparkEvent1.kstack, ((tempUnparkEvent1.carsInTheWay+1)*this.carSize));
					
				}
			}
		}
	}
		
	
	// Blocking a certain amount of tiles
	public void blockStreets(KStack kstack, int length) {
		System.out.println("blockStreets: kStack: "+kstack+", length: "+length);
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
		System.out.println("Street: "+tempStreet1+", Car: "+tempStreet1.car+", lockedForUnparking: "+((KStack)tempStreet1).lockedForUnparking+", lockedForParking: "+((KStack)tempStreet1).lockedForParking+", blocking kstack: "+tempStreet1.blockingKStack);
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
		System.out.println("Delayed due to parking: "+item.backOrderDelay);
		System.out.println("==========");
		System.out.println();
	}
	
	private void generateImage(String tick) throws Exception{
		int X = 2+this.parkingRows+this.carSize+this.carSize*this.kHeight, Y = (6*this.carSize*this.kHeight)+3;
//		System.out.println("generateImage: image size: X = "+X+", Y = "+Y);
		int x =0, y = 0, PIX_SIZE = 5;
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
			
			if (tempStreet2.blockingKStack != null)
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
			
			if (tempStreet2.blockingKStack != null)
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
			
			if (tempStreet2.blockingKStack != null)
				g.setColor(Color.RED);
			else
				g.setColor(Color.LIGHT_GRAY);
			g.fillRect((carSize*kHeight+parkingRows+1)*PIX_SIZE, (((Y/2))+i)*PIX_SIZE, PIX_SIZE, PIX_SIZE);
			
			tempStreet1 = tempStreet1.next1;
			tempStreet2 = tempStreet2.next1;
		}

		
		
		
				
		
		// paint cars in the middle kstacks
		for (int i = 0; i < (kstack.length/3); i++) {
//			System.out.println("generateImage: checking kstack up to "+(kstack.length/3)+" now at "+i);
			y = (Y/2);
			x = this.carSize*this.kHeight+1;
//			System.out.println("Searching for kstack["+i+"] "+kstack[i]+", y="+y+", x="+x);
			tempStreet1 = spawn.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
//							System.out.println("generateImage: found car "+tempStreet1.car+" at "+tempStreet1+" in kstack "+i);
//							System.out.println("generateImage: painting car at x="+x+", y="+y);
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
//							System.out.println("generateImage: found car "+tempStreet1.car+" at "+tempStreet1+" in kstack "+i);
//							System.out.println("generateImage: painting car at x="+x+", y="+y);
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
//					System.out.println("generateImage: checking kstack up to "+(kstack.length/3)+" now at "+i);
			y = carSize*kHeight-1;
			x = this.carSize*this.kHeight+1;
//					System.out.println("Searching for kstack["+i+"] "+kstack[i]+", y="+y+", x="+x);
			tempStreet1 = spawn.next2;
			while (tempStreet1.kstack1 == null)
				tempStreet1 = tempStreet1.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
//							System.out.println("generateImage: found car "+tempStreet1.car+" at "+tempStreet1+" in kstack "+i);
//							System.out.println("generateImage: painting car at x="+x+", y="+y);
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
//							System.out.println("generateImage: found car "+tempStreet1.car+" at "+tempStreet1+" in kstack "+i);
//							System.out.println("generateImage: painting car at x="+x+", y="+y);
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
//					System.out.println("generateImage: checking kstack up to "+(kstack.length/3)+" now at "+i);
			y = Y-(carSize*kHeight)-1;
			x = this.carSize*this.kHeight+1;
//					System.out.println("Searching for kstack["+i+"] "+kstack[i]+", y="+y+", x="+x);
			tempStreet1 = spawn.next3;
			while (tempStreet1.kstack1 == null)
				tempStreet1 = tempStreet1.next1;
			
l2:			while (tempStreet1 != crossroad) {
				if (kstack[i] == tempStreet1.kstack1) {
					tempStreet1 = tempStreet1.kstack1;
					y--;
					
					while (tempStreet1 != null) {
						if (tempStreet1.car != null) {
//							System.out.println("generateImage: found car "+tempStreet1.car+" at "+tempStreet1+" in kstack "+i);
//							System.out.println("generateImage: painting car at x="+x+", y="+y);
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
//							System.out.println("generateImage: found car "+tempStreet1.car+" at "+tempStreet1+" in kstack "+i);
//							System.out.println("generateImage: painting car at x="+x+", y="+y);
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
	
	
	
	public void saveToFile( BufferedImage img, File file ) throws IOException {
		ImageWriter writer = null;
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
}
