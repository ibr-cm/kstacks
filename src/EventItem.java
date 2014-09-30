
public class EventItem {
	/**
	 * The car this whole EventItem is about.
	 */
	private Car car;
	
	/**
	 * Time at which the car should spawn in the parking Lot and the number of
	 * ticks the actual spawning was delayed due to a blocked spawn.
	 */
	private int entryTime, entryDelay;
	private int backOrderTime; // in ticks -- time when car is ordered off the parking lot
	private int backOrderDelay; // in ticks --  if a stack is locked this will be increased
	private int exitTime; // in ticks -- time when car public KStack kstack;actually exits parking lot
	private boolean fulfilled;
	
	/**
	 * If this is set true the car will not be put into the smallest stack, but
	 * rather put into a random stack, which still has enough room left.
	 */
	public boolean randomStack;
	
	
	public EventItem() {
		this.car = null;
		this.entryTime = 0;
		this.backOrderTime = 0;
		this.backOrderDelay = 0;
		this.fulfilled = true;
		this.randomStack = false;
	}
	
	public void setupEvent(Car car, int entryTime, int backOrderTime) {
		this.car = car;
		this.entryTime = entryTime;
		this.backOrderTime = backOrderTime;
		this.backOrderDelay = 0;
		this.exitTime = 0;
		this.fulfilled = false;
		this.randomStack = false;
	}
	
	
	
	public void fulfill(int tick) {
		this.fulfilled = true;
		this.exitTime = tick;
		this.car.leaveParkingLot();
	}

	public void increaseBackOrderDelay() {
		this.backOrderDelay++;
	}
	
	public void increaseEntryDelay() {
		this.entryDelay++;
	}
	
	
	/*
	 * Methods to get values
	 */
	public boolean isFulfilled() {return this.fulfilled;}
	public int getBackOrderTime() {return this.backOrderTime;}
	public int getBackOrderDelay() {return this.backOrderDelay;}
	public Car getCar() {return this.car;}
	public int getEntryTime() {return this.entryTime;}
	public int getEntryDelay() {return this.entryDelay;}
	public int getExitTime() {return this.exitTime;}
	public int[] getEventStats() {
		int[] stats = {entryTime, entryDelay, backOrderTime, backOrderDelay, exitTime};
		return stats;
	}
}
