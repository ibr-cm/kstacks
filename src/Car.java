
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
		this.kstack.locked = true;
		
		System.out.println("spawn: Car "+this+" spawn at "+this.spawn+" and was assigned to kStack "+this.kstack+" (parkingSpot "+this.parkingSpot+") through lane "+lane+".");
	}
	
	public void setDrivingTargets(DrivingTarget[] drivingTarget) {
		this.drivingTarget = drivingTarget;
	}
	
	public void drive() {
		if (this.currentStreet == spawn && this.firstRide) {
			if (lane.car == null && lane.blockingKStack == null) {
				lane.car = this;
				this.currentStreet = lane;
				if (size == 1)
					this.currentStreet.prev1.car = null;
				// TODO remove pieces of this car behind itself if size > 1
			}
		} else if (this.currentStreet == spawn && !this.firstRide) {
			// TODO
		} else {
			if (drivingTarget != null && drivingTarget[0] != null) {
				if (drivingTarget[0].direction == 'D') { // drive forward
					if (this.currentStreet.kstack1 == this.kstack && this.currentStreet.kstack1.car == null) {
						this.currentStreet.kstack1.car = this;
						this.currentStreet = this.currentStreet.kstack1;
						this.currentStreet.prev1.car = null;
					} else if (this.currentStreet.kstack2 == this.kstack && this.currentStreet.kstack2.car == null) {
						this.currentStreet.kstack2.car = this;
						this.currentStreet = this.currentStreet.kstack2;
						this.currentStreet.prev1.car = null;
					} else if (this.currentStreet.next1.car == null) {
						this.currentStreet.next1.car = this;
						this.currentStreet = this.currentStreet.next1;
						this.currentStreet.prev1.car = null;
					}
				} else if (drivingTarget[0].direction == 'R') { // drive backwards
					; //TODO
				}
			}
		}
		if (this.drivingTarget != null && this.currentStreet == drivingTarget[0].street) {
			System.out.println("drive: reduced drivingTargets");
			System.out.println();
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
		}
	}
}
