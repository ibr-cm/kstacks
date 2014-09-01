
public class Car {
	public int exitTime;
	
	public int startstop;
	public int tilesMoved;
	
	// public char direction; // D, N, R = Drive, Neutral, Reverse
	
	public boolean unparking;
	
	public boolean inParkingLot;
	
	public int size;
	
	public KStack kstack;
	public int parkingSpot;
	
	public Spawn spawn; // Street where every car spawns
	public Despawn despawn; // Street where every car despawns
	public Crossroad crossroad;
	
	public Street currentStreet;
	
	public DrivingTarget[] drivingTarget;
	
	public Street streetAtLastTick;
	
	public Street lane;
	
	public EventItem eventItem;
	
	public boolean firstRide; // mark if this is the first trip to the kstack; important if on the way to a kstack which is in action unparking
	public boolean done; // true if successfully left the parking lot
	
	
	
	
	public Car() {
	}
	
	public Car(int size, EventItem eventItem, Spawn spawn, Despawn despawn, Crossroad crossroad) {
		this.size = size;
		this.kstack = null;
		this.currentStreet = null;
		this.drivingTarget = null;
		this.streetAtLastTick = null;
		this.startstop = 0;
		this.tilesMoved = 0;
		this.eventItem = eventItem;
		this.firstRide = true;
		this.spawn = spawn;
		this.despawn = despawn;
		this.crossroad = crossroad;
		this.parkingSpot = 0;
		this.inParkingLot = false;
	}
	
	public void spawn() {
		this.currentStreet = spawn;
		spawn.car = this;
		this.inParkingLot = true;
		this.kstack.locked = true; // locks kstack against parking and unparking
		
		System.out.println("spawn: Car "+this+" spawn at "+this.spawn+" and was assigned to kStack "+this.kstack+" (parkingSpot "+this.parkingSpot+") through lane "+lane+".");
	}
	
	public void setDrivingTargets(DrivingTarget[] drivingTarget) {
		this.drivingTarget = drivingTarget;
	}
	
	public void drive() {
		// if there are at least one more driving target the car tries to drive there
		
		if (this.drivingTarget != null && this.drivingTarget[0] != null) {
			System.out.println("drive: driving car "+this);
			System.out.println("drive: direction "+this.drivingTarget[0].direction+" to "+this.drivingTarget[0].street);
			
			
			
			// Drive forward:
			if (this.drivingTarget[0].direction == 'D') {
				// Car stands at the spawn
				if (this.currentStreet == spawn) {
					if (isNextTileFree(this.lane)) {
						this.lane.car = this;
						this.currentStreet = this.lane;
						clearTileBehindCar();
					}
 				}
				
				// kstack1 is the one the car is supposed to enter
				else if (this.currentStreet.kstack1 != null && this.currentStreet.kstack1 == this.kstack && this.currentStreet.kstack1.car == null) {
					this.currentStreet.kstack1.car = this;
					this.currentStreet = this.currentStreet.kstack1;
					clearTileBehindCar();
				}
				
				// kstack2 is the one the car is supposed to enter
				else if (this.currentStreet.kstack1 != null && this.currentStreet.kstack2 == this.kstack && this.currentStreet.kstack2.car == null) {
					this.currentStreet.kstack2.car = this;
					this.currentStreet = this.currentStreet.kstack2;
					clearTileBehindCar();
				}
				
				// neither kstack is the correct one and the car keeps moving forward
				else if (this.currentStreet.next1.car == null && isNextTileFree(currentStreet.next1)) {
					this.currentStreet.next1.car = this;
					this.currentStreet = this.currentStreet.next1;
					clearTileBehindCar();
				}
			}
			
			
			
			
			// Drive backward:
			else if (this.drivingTarget[0].direction == 'R') {
				System.out.println("drive: driving backwards");
				if (isPrevTileFree()) {
					System.out.println("drive: tile behind me was free");
					getPrevTile().car = this;
					this.currentStreet.car = null;
					this.currentStreet = this.currentStreet.prev1;
				}
				else
					System.out.println("drive: tile behind me was not free");
			}
			
			// Drive nowhere but stall one tick:
			else if (this.drivingTarget[0].direction == 'N') {
				; // do nothing
			}
			
			
			
			// if the car reached its driving target
			if (this.drivingTarget[0] != null && this.currentStreet == drivingTarget[0].street) {
				
				System.out.println("drive: drivingTarget of car "+this+" reached");
				// if the driving target wants to unlock a stack e.g. after the last car is back in the stack
				// after unparking a car
				if (drivingTarget[0].unlockKStack != null) {
					//System.out.println(drivingTarget[0].unlockKStack+" "+drivingTarget[0].unlockKStack.locked);
					//System.out.println("drive: "+drivingTarget[0].unlockKStack+" unlocked again");
					drivingTarget[0].unlockKStack.locked = false;
					drivingTarget[0].unlockKStack = null;
				}
				
				
				
				// if the car is supposed to unlock the whole street from a lock of a certain kstack
				if (this.drivingTarget[0].releaseStreetBlock) {
					
					System.out.println("removing lock on streets: car = "+this+", position: "+this.currentStreet);
					
					Street tempStreet1 = this.currentStreet;
					Street tempStreet2 = this.kstack.prev1;
					while (tempStreet1.prev1.car == this) {
						tempStreet1 = tempStreet1.prev1;
					}
					while (tempStreet2 != tempStreet1.prev1) {
						tempStreet2.blockingKStack = null;
						tempStreet2 = tempStreet2.prev1;
					}
				}
				
				
				
				
				// now reduce the list of driving target by 1
				// TODO implement a linked list to simplify this kind of list item reducing
				if (drivingTarget.length == 1) {
					this.drivingTarget = null;
				} else {
					DrivingTarget[] tempDrivingTarget = new DrivingTarget[this.drivingTarget.length-1];
					for (int i=0; i<drivingTarget.length-1; i++) {
						tempDrivingTarget[i] = drivingTarget[i+1];
					}
					drivingTarget = tempDrivingTarget;
				}
				this.firstRide = false;
				
				
				
				
				System.out.println("drive: drivingTarget now: "+drivingTarget);
			}

		}
	}
	
	
	
	private void clearTileBehindCar() {
		Street tempStreet1 = this.currentStreet;
		for (int i = 0; i < this.size; i++) {
			tempStreet1 = tempStreet1.prev1;
		}
		tempStreet1.car = null;
	}
	
	
	
	private boolean isNextTileFree(Street street) {
		if ((street.blockingKStack == this.kstack || street.blockingKStack == null) && street.car == null)
			return true;
		return false;
	}
	
	
	
	private boolean isPrevTileFree() {
		Street tempStreet1 = getPrevTile();
		if (tempStreet1.car == null && (this.unparking || tempStreet1.blockingKStack == null || tempStreet1.blockingKStack == this.kstack))
			return true;
		return false;
	}
	
	
	
	private Street getPrevTile() {
		Street tempStreet1 = this.currentStreet;
		System.out.println(size);
		for (int i=0; i < size; i++) {
			tempStreet1 = tempStreet1.prev1;
		}
		System.out.println("getPrevTile: prevTile: "+tempStreet1);
		return tempStreet1;
	}
}
