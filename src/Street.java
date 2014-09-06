
public class Street {
	public Street prev1, next1;
	public KStack kstack1, kstack2;
	public KStack blockingKStack;
	public Car car;
	public Car carAtLastTick;
	public int lastRefresh;
	public int watermark;
	
	
	public Street() {
		this.prev1 = null;
		this.next1 = null;
		this.kstack1 = null;
		this.kstack2 = null;
		this.car = null;
		this.carAtLastTick = null;
		this.lastRefresh = 0;
		this.watermark = 0;
	}
	
	// this ensures, that streets remember whether or not a car
	// was on this street last tick.
	public void refresh(int tick) {
		if (this.lastRefresh != tick) {
			this.carAtLastTick = this.car;
			this.lastRefresh = tick;
		}
	}
}
