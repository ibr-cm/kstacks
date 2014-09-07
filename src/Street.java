
public class Street {
	/**
	 * Pointer to the next street and the street before this.
	 */
	public Street prev1, next1;
	
	/**
	 * Pointer to the adjacent kStacks. If there are no kStacks this is null.
	 */
	public KStack kstack1, kstack2;
	
	/**
	 * In case this street is blocked by some kStack this leads to the one.
	 */
	public KStack blockingKStack;
	
	/**
	 * Pointer to the car which is standing on this piece of street right now
	 * and the pointer to the car which stood here at the last tick.
	 */
	public Car car;
	public Car carAtLastTick;
	
	/**
	 * Latest tick at which this street was refreshed. This assures that a this
	 * street can only be refreshed once per tick.
	 */
	public int lastRefresh;
	
	/**
	 * Describes how many cars are piled into this kStack.  
	 */
	public int watermark;
	
	/**
	 * generic constructor
	 */
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
	
	/**
	 * This method tells the street what tick it is and makes it copy the car
	 * (which is standing on this tile of street right now) into the variable
	 * carAtLastTick. So the street knows if it was occupied during the last
	 * tick. Calling this method more than once has no effect.
	 * Important is that this method has to be called AFTER all cars moved.  
	 * @param tick current time in ticks
	 */
	public void refresh(int tick) {
		if (this.lastRefresh != tick) {
			this.carAtLastTick = this.car;
			this.lastRefresh = tick;
		}
	}
}
