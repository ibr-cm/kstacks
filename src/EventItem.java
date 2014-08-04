
public class EventItem {
	public Car car;
	public int entryTime; // in ticks -- time when car enters the parking lot
	public int backOrderTime; // in ticks -- time when car is ordered off the parking lot
	public int exitTime; // in ticks -- time when car actually exits parking lot
	public boolean fulfilled;
	
	
	public EventItem() {
		this.car = null;
		this.entryTime = 0;
		this.backOrderTime = 0;
		this.fulfilled = true;
	}
	
	public void setupEvent(Car car, int entryTime, int backOrderTime) {
		this.car = car;
		this.entryTime = entryTime;
		this.backOrderTime = backOrderTime;
		this.exitTime = 0;
		this.fulfilled = false;
	}
	
	public void printStats() {
		System.out.println("Event: "+this+"Car: "+this.car+", entryTime: "+this.entryTime+", backOrderTime: "+this.backOrderTime+", exit-time: "+this.exitTime+", fulfilled: "+this.fulfilled);
	}
}
