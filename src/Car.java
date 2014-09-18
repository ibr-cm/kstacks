import java.awt.Color;


public class Car {
	public int exitTime;
	
	public int verboseLevel;
	
	public boolean unparking;
	
	private boolean isInParkingLot;
	
	public int size;
	
	public KStack kstack;
	public int parkingSpot;
	
	public Spawn spawn; // Street where every car spawns
	public Despawn despawn; // Street where every car despawns
	public Crossroad crossroad;
	
	public Street currentStreet;
	public Street lastCurrentStreet;
	
	public DrivingTarget[] drivingTarget;
	
	/**
	 * This variables are needed to check if the car is moving a tile and
	 * count its starts and stops.
	 */
	public Street streetAtLastTick;
	public int startstop;
	public boolean wasMoving;
	public int tilesMoved;
	
	public EventItem eventItem;
	
//	public boolean firstRide; // mark if this is the first trip to the kstack; important if on the way to a kstack which is in action unparking
	
	public boolean disabled;
	
	private Color color;
	
	/**
	 * This debug option marks all cars red, which are going to this KStack.
	 * Disable by setting it to -1 or to a number higher than the number of
	 * stacks available.
	 */
	private int debugKStackID = -1;
	
	private Configuration config;
	
	
	public Car() {
		this.color = Color.getHSBColor(0.5f, 1.0f, 1.0f);
		this.verboseLevel = config.verboseLevel;
		this.wasMoving = false;
	}
	
	public Car(int size, EventItem eventItem, Spawn spawn, Despawn despawn, Crossroad crossroad) {
		this.color = Color.getHSBColor(0.5f, 1.0f, 1.0f);
		this.size = size;
		this.kstack = null;
		this.currentStreet = null;
		this.drivingTarget = null;
		this.streetAtLastTick = null;
		this.wasMoving = false;
		this.startstop = 0;
		this.tilesMoved = 0;
		this.eventItem = eventItem;
//		this.firstRide = true;
		this.spawn = spawn;
		this.despawn = despawn;
		this.crossroad = crossroad;
		this.parkingSpot = 0;
		this.isInParkingLot = false;
		this.disabled = false;
		this.verboseLevel = config.verboseLevel;
	}
	
	public void spawn() {
		float hue = (this.kstack.id%2)*0.5f+this.kstack.id*0.05f;
		hue -= (int)hue;
		hue = hue*0.8f+0.1f;
		if (this.kstack.id == this.debugKStackID)
			hue = 0.0f;  // Makes the car red in case of debugging this stack.
		this.color = Color.getHSBColor(hue, 1.0f, (float)(0.5*Math.random()+0.5));
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
		
		debugOutput("spawn: Car "+this+" spawn at "+this.spawn+" and was assigned to kStack "+this.kstack+" (parkingSpot "+this.parkingSpot+").",2);
	}
	
	public void setDrivingTargets(DrivingTarget[] drivingTarget) {
		this.drivingTarget = drivingTarget;
	}
	
	public void drive() {
		if (this.drivingTarget == null || disabled)
			return;
		
		
		// if there are at least one more driving target the car tries to drive there
		
		if (this.drivingTarget != null && this.drivingTarget[0] != null) {
			debugOutput("drive: driving car "+this,2);
			debugOutput("drive: direction "+this.drivingTarget[0].direction+" to "+this.drivingTarget[0].street,2);
			
			
			
			// Drive forward:
			if (this.drivingTarget[0].direction == 'D') {
				// Car stands at the spawn
				if (this.currentStreet == spawn) {
					if (isNextTileFree(this.kstack.lane)) {
						this.kstack.lane.car = this;
						this.currentStreet = this.kstack.lane;
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
				if (this.drivingTarget[0].unparkEvent != null)
					this.drivingTarget[0].unparkEvent.pop(this.drivingTarget[0].unparkList);
				
				
				// if this car is supposed to reduce the watermark when reaching this position
				if (this.drivingTarget[0].reduceWatermark)
					this.kstack.watermark--;
				
				
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
//				this.firstRide = false;
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
		if (tempStreet1.car == null && tempStreet1.carAtLastTick == null && (this.unparking || tempStreet1.blockingKStack == null || tempStreet1.blockingKStack == this.kstack)) {
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
	
	/**
	 * This method makes outputs if applicable.
	 * @param text Message to the command line.
	 * @param priority This number has to be less or equal to the verboseLevel
	 * or the text will not be displayed.
	 */
	private void debugOutput(String text, int priority) {
		if (priority <= this.verboseLevel) {
			System.out.println(text);
		}
	}
	
	/**
	 * This method counts the starts and stops the car performed during the
	 * duration of time it spent inside the parking lot. Starts and Stops are
	 * not counted seperately since it always starts with a Start and both
	 * events occur interleaving.
	 */
	public void checkForStartsStops() {
		if (this.currentStreet != null && this.isInParkingLot && ((this.currentStreet == this.lastCurrentStreet && this.wasMoving) || (this.currentStreet != this.lastCurrentStreet && !this.wasMoving))) {
			this.startstop++;
			this.wasMoving = !this.wasMoving;
			this.lastCurrentStreet = this.currentStreet;
		}
	}
	
	/**
	 * If the car left the parking lot this boolean is set to false to reduce
	 * the load for the simulator and suppress unneeded or dangerous routines
	 * (e.g. trying to move the car) from perfoming.
	 */
	public void leaveParkingLot() {
		this.isInParkingLot = false;
	}
	
	/**
	 * Checks if the car is still in the parking lot.
	 * @return boolean whether or not the car is inside the parking lot
	 */
	public boolean isInParkingLot() {
		return this.isInParkingLot;
	}
	
	public Color getColor() {
		if (drivingTarget != null && this.kstack.id != this.debugKStackID)
			return Color.getHSBColor(0.1f, 1.0f, 1.0f);
		return this.color;
	}
}
