
public class Car {
	public int exitTime;
	
	public int startstop;
	public boolean wasMoving;
	public int tilesMoved;
	
	public int verboseLevel;
	
	// public char direction; // D, N, R = Drive, Neutral, Reverse
	
	public boolean unparking;
	
	public boolean isInParkingLot;
	
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
	
	public int[] color;
	
	public boolean disabled;
	
	
	
	
	public Car() {
		this.color = new int[3];
		this.color[0] = 0; // R
		this.color[1] = (int)(Math.random()*256); // G
		this.color[2] = (int)(Math.random()*256); // B
		this.verboseLevel = 0;
	}
	
	public Car(int size, EventItem eventItem, Spawn spawn, Despawn despawn, Crossroad crossroad, int verboseLevel) {
		color = new int[3];
		color[0] = 0;
		color[1] = (int)(Math.random()*256);
		color[2] = (int)(Math.random()*256);
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
		this.isInParkingLot = false;
		this.disabled = false;
		this.verboseLevel = verboseLevel;
	}
	
	public void spawn() {
		this.currentStreet = spawn;
		spawn.car = this;
		Street tempStreet1 = spawn;
		if (this.size > 1) {
			for (int i = 0; i < this.size-1; i++) {
				tempStreet1 = tempStreet1.prev1;
				tempStreet1.car = this;
			}
		}
		
		this.isInParkingLot = true;
		this.kstack.lockedForUnparking = true; // locks kstack against unparking
		
		debugOutput("spawn: Car "+this+" spawn at "+this.spawn+" and was assigned to kStack "+this.kstack+" (parkingSpot "+this.parkingSpot+") through lane "+lane+".",2);
	}
	
	public void setDrivingTargets(DrivingTarget[] drivingTarget) {
		this.drivingTarget = drivingTarget;
	}
	
	public void drive() {
		if (disabled || this.drivingTarget == null)
			return;
		
		
		// if there are at least one more driving target the car tries to drive there
		
		if (this.drivingTarget != null && this.drivingTarget[0] != null) {
			debugOutput("drive: driving car "+this,2);
			debugOutput("drive: direction "+this.drivingTarget[0].direction+" to "+this.drivingTarget[0].street,2);
			
			
			
			// Drive forward:
			if (this.drivingTarget[0].direction == 'D') {
				// Car stands at the spawn
				if (this.currentStreet == spawn) {
					if (isNextTileFree(this.lane)) {
						this.lane.car = this;
						this.currentStreet = this.lane;
						clearTileBehindCar();
						if (this.drivingTarget[0].continousUnblocking)
							getPrevTile().blockingKStack = null;
						tilesMoved++;
					}
 				}
				
				// kstack1 is the one the car is supposed to enter
				else if (this.currentStreet.kstack1 != null && this.currentStreet.kstack1 == this.kstack && this.currentStreet.kstack1.car == null && this.currentStreet.kstack1.carAtLastTick == null && !this.unparking) {
					this.currentStreet.kstack1.car = this;
					this.currentStreet = this.currentStreet.kstack1;
					clearTileBehindCar();
					if (this.drivingTarget[0].continousUnblocking)
						getPrevTile().blockingKStack = null;
					tilesMoved++;
				}
				
				// kstack2 is the one the car is supposed to enter
				else if (this.currentStreet.kstack2 != null && this.currentStreet.kstack2 == this.kstack && this.currentStreet.kstack2.car == null && this.currentStreet.kstack2.carAtLastTick == null && !this.unparking) {
					this.currentStreet.kstack2.car = this;
					this.currentStreet = this.currentStreet.kstack2;
					clearTileBehindCar();
					if (this.drivingTarget[0].continousUnblocking)
						getPrevTile().blockingKStack = null;
					tilesMoved++;
				}
				
				// neither kstack is the correct one and the car keeps moving forward
				else if (isNextTileFree(this.currentStreet.next1)) {
					this.currentStreet.next1.car = this;
					this.currentStreet = this.currentStreet.next1;
					clearTileBehindCar();
					if (this.drivingTarget[0].continousUnblocking)
						getPrevTile().blockingKStack = null;
					tilesMoved++;
				}
			}
			
			
			
			
			// Drive backward:
			else if (this.drivingTarget[0].direction == 'R') {
				debugOutput("drive: driving backwards",2);
				if (isPrevTileFree()) {
					debugOutput("drive: tile behind me was free",2);
					getPrevTile().car = this;
					this.currentStreet.car = null;
					this.currentStreet = this.currentStreet.prev1;
					tilesMoved++;
				}
				else
					debugOutput("drive: tile behind me was not free",2);
			}
			
			// Drive nowhere but stall one tick:
			else if (this.drivingTarget[0].direction == 'N') {
				; // do nothing -- this means the car idles at its current position for one round
			}
			
			
			
			// if the car reached its driving target
			if (this.drivingTarget[0] != null && this.currentStreet == drivingTarget[0].street) {
				
				debugOutput("drive: drivingTarget of car "+this+" reached",2);
				// if the driving target wants to unlock a stack e.g. after the last car is back in the stack
				// after unparking a car
				if (drivingTarget[0].unlockKStackForParking != null) {
					drivingTarget[0].unlockKStackForParking.lockedForParking = false;
				}
				
				// if a kstack is supposed to be unlocked for unparking - this is the place to be! :)
				if (drivingTarget[0].unlockKStackForUnparking != null) {
					drivingTarget[0].unlockKStackForUnparking.lockedForUnparking = false;
				}
				
				
				// if the car is supposed to unlock the whole street from a lock of a certain kstack
				if (this.drivingTarget[0].releaseStreetBlock) {
					
//					debugOutput("drive: removing lock on streets: car = "+this+", position: "+this.currentStreet);
					
					Street tempStreet1 = this.currentStreet, tempStreet2 = this.kstack.prev1;
					while (tempStreet1.prev1.car == this) {
						tempStreet1 = tempStreet1.prev1;
					}
					while (tempStreet2 != tempStreet1.prev1) {
						if (tempStreet2.blockingKStack == this.kstack)
							tempStreet2.blockingKStack = null;
						tempStreet2 = tempStreet2.prev1;
					}
				}
				
				// if the car is supposed to delete a UnparkEvent from the list of events
				if (this.drivingTarget[0].unparkEvent != null) {
					this.drivingTarget[0].unparkEvent.pop(this.drivingTarget[0].unparkList);
				}
				
				
				
				
				// now reduce the list of driving target by 1
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
				debugOutput("drive: drivingTarget now: "+drivingTarget,2);
			}
		}
	}
	
	
	
	private void clearTileBehindCar() {
		Street tempStreet1 = getLastTileOfCar(this.currentStreet);
		tempStreet1.car = null;
	}
	
	private Street getLastTileOfCar(Street street) {
		if (street == crossroad) {
			if (crossroad.prev1.car == this) 
				return getLastTileOfCar(crossroad.prev1);
			else if (crossroad.prev2.car == this) 
				return getLastTileOfCar(crossroad.prev2);
			else if (crossroad.prev3.car == this) 
				return getLastTileOfCar(crossroad.prev3);
		} else if (street.prev1.car == this)
			return getLastTileOfCar(street.prev1);
		return street;
//		if (street == crossroad) {
//			if (crossroad.prev1.car == this) {
//				return getLastTileOfCar(crossroad.prev1);
//			} else if (crossroad.prev2.car == this) {
//				return getLastTileOfCar(crossroad.prev2);
//			} else {
//				return getLastTileOfCar(crossroad.prev3);
//			}
//		} else {
//			if (street.prev1.car == this)
//				return getLastTileOfCar(street.prev1);
//		}
//		return street;
	}
	
	
	
	private boolean isNextTileFree(Street street) {
		if ((street.blockingKStack == this.kstack || street.blockingKStack == null) && street.car == null && street.carAtLastTick == null)
			return true;
		debugOutput(this.currentStreet+" "+crossroad.prev2+" false!",2);
		return false;
	}
	
	
	
	private boolean isPrevTileFree() {
		Street tempStreet1 = getPrevTile();
		debugOutput("isPrevTileFree: car: "+this+" currentStreet: "+getPrevTile(),2);
		if (tempStreet1.next1 == this.kstack && tempStreet1.prev1.blockingKStack != this.kstack) {
			debugOutput("isPrevTileFree: false because street before kstack is not blocked",2);
			return false;
		}
		if (tempStreet1.car == null && (this.unparking || tempStreet1.blockingKStack == null || tempStreet1.blockingKStack == this.kstack)) {
			debugOutput("isPrevTileFree: true",2);
			return true;
		}
		// if the street behind the car is a kstack the street before the kstack has to be blocked by said kstack
		debugOutput("isPrevTileFree: false!",2);
		return false;
	}
	
	
	
	private Street getPrevTile() {
		Street tempStreet1 = this.currentStreet;
		for (int i=0; i < size; i++) {
			tempStreet1 = tempStreet1.prev1;
		}
		return tempStreet1;
	}
	
	private void debugOutput(String text, int priority) {
		if (priority <= this.verboseLevel) {
			System.out.println(text);
		}
	}
}
