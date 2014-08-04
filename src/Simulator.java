import java.awt.Event;


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
		while(!eventsFinished() && tick<1024) {
			
			
			
			
			
			moveCars();
			
			
			despawnCar();
			
			
			checkForSpawns();
			
			
			spawnCar();
			
			
			
			
			
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
				
				// the spawning of this car will be appended to the list of spawns
				int j = 0;
				while (spawnList[j] != null) {
					j++;
				}
				spawnList[j] = eventList[i];
				
				// index of the stack with the smallest amount of cars
				int index = findSmallestStack();

				// assign kstack to a car
				eventList[i].car.kstack = kstack[index];
				
				// assign the right lane to the car
				if (index/3 == 0) {
					eventList[i].car.lane = spawn.next1;
				} else if (index/3 == 1) {
					eventList[i].car.lane = spawn.next2;
				} else {
					eventList[i].car.lane = spawn.next3;
				}
				
				// increment the cars stored in this kstack
				kstack[findSmallestStack()].watermark++;
			}
		}
	}
	
	
	// spawns the next car in the queue (in case there is one)
	private void spawnCar() {
		if (spawnList[0] != null) {
			boolean spawnBlocked = false; // check if spawn is free
			if (spawn.car != null || (kHeight*carSize>2?spawn.prev1.car != null:false)) {
				spawnBlocked = true;
			}
			
			
			if (!spawnBlocked) {
				// put the car down at the spawn
				spawnList[0].car.spawn();
				
				// shift the whole list one item down
				for (int i=0; i<totalCarsUsed-1; i++) {
					spawnList[i] = spawnList[i+1];
				}
				spawnList[totalCarsUsed-1] = null;
			}
		}
	}
	
	private void moveCars() {
		;
	}
	
	private int findSmallestStack() {
		int index = 0;
		int watermark = kHeight;
		
		for (int i=0; i<parkingRows; i++) {
			if (kstack[i].watermark < watermark) {
				watermark = kstack[i].watermark;
				index = i;
			}
		}
		return index;
	}
	
	private void log(String data) {
		
	}
}
