
public class Car {
	public int exitTime;
	
	public int startstop;
	public int tilesMoved;
	
	public char direction; // D, N, R = Drive, Neutral, Reverse
	
	public int size;
	
	public KStack kstack;
	
	public Spawn spawn; // Street where every car spawns
	public Despawn despawn; // Street where every car despawns
	
	public Street currentStreet;
	
	public DrivingTarget drivingTarget;
	
	public Street streetAtLastTick;
	
	public Street lane;
	
	public EventItem eventItem;
	
	public boolean firstRide; // mark if this is the first trip to the kstack; important if on the way to a kstack which is in action unparking
	public boolean done; // true if successfully left the parking lot
	
	
	
	
	public Car() {
	}
	
	public Car(int size, EventItem eventItem) {
		this.size = size;
		this.kstack = null;
		this.currentStreet = null;
		this.drivingTarget = null;
		this.streetAtLastTick = null;
		this.startstop = 0;
		this.tilesMoved = 0;
		this.direction = 'N';
		this.eventItem = eventItem;
		this.firstRide = true;
	}
	
	public void spawn() {
		
	}
}
