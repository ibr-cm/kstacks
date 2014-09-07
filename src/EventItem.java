
public class EventItem {
	private Car car;
	private int entryTime; // in ticks -- time when car enters the parking lot
	public int entryDelay; // in ticks -- time the car had to wait until entering the lot
	public int backOrderTime; // in ticks -- time when car is ordered off the parking lot
	private int backOrderDelay; // in ticks --  if a stack is locked this will be increased
	public int exitTime; // in ticks -- time when car public KStack kstack;actually exits parking lot
	public boolean fulfilled;
	
	
	public EventItem() {
		this.car = null;
		this.entryTime = 0;
		this.backOrderTime = 0;
		this.backOrderDelay = 0;
		this.fulfilled = true;
	}
	
	public void setupEvent(Car car, int entryTime, int backOrderTime) {
		this.car = car;
		this.entryTime = entryTime;
		this.backOrderTime = backOrderTime;
		this.backOrderDelay = 0;
		this.exitTime = 0;
		this.fulfilled = false;
	}

	public void increaseBackOrderDelay() {
		this.backOrderDelay++;
	}
	public int getBackOrderDelay() {
		return this.backOrderDelay;
	}
	public Car getCar() {
		return this.car;
	}
	
	public int getEntryTime() {
		return this.entryTime;
	}
}
