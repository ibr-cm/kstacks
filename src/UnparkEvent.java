
public class UnparkEvent {
	/**
	 * indicates if this UnparkEvent is the first in the list - usually false -- obsolete
	 */
	public boolean first; 
	
	/**
	 * Pointer to the next UnparkEvent since they are stored in a linked list.
	 */
	public UnparkEvent next;
	
	/**
	 * Pointer to the car, which is supposed to leave its parking spot and
	 * leave the parking lot.
	 */
	private Car carToUnpark;
	
	/**
	 * Pointer to the car, which is closest to the road.
	 */
	public Car firstInQueue;
	
	/**
	 * Number of cars, which are standing between the unparking car
	 * and the street.
	 */
	public int carsInTheWay;
	
	
	/**
	 * This kind of shows the progress of the UnparkEvent. After the street is
	 * blocked this will be set to true.
	 * This assures that the method, which blocks parts of the street, only
	 * blocks the streets for this event once.
	 */
	public boolean doneBlocking;
	
	/**
	 * Generic contructor
	 * All vital variables become set.
	 */
	public UnparkEvent() {
		this.first = false;
		this.next = null;
		this.carToUnpark = null;
		this.firstInQueue = null;
		this.carsInTheWay = 0;
		this.doneBlocking = false;
	}
	
	/**
	 * TODO get rid of this
	 */
	public void setHead() {
		this.first = true;
	}
	
	/**
	 * Add an UnparkEvent to the list of UnparkEvents
	 * @param newWvent new event to append to the list
	 */
	public void addEvent(UnparkEvent newEvent) {
		if (this.next == null)
			this.next = newEvent;
		else
			this.next.addEvent(newEvent);
	}
	
	/**
	 * Remove an UnparkEvent from the list.
	 * @param first reference to the head of the list
	 */
	public void pop(UnparkEvent first) {
		UnparkEvent tempEvent1 = first;
		while (tempEvent1.next != this) {
			tempEvent1 = tempEvent1.next;
		}
		tempEvent1.next = this.next;
	}
	
	/**
	 * Set the car which is supposed to unpark.
	 * @param carToUnpark the car which is supposed to leave its spot
	 */
	public void setCarToUnpark(Car carToUnpark) {
		if (carToUnpark == null)
			this.carToUnpark = carToUnpark;
	}
	
	/**
	 * Set the first car in the queue which will enter the street first.
	 * This car can then be enabled or disabled when the street is blocked.
	 * @param carFirstInQueue
	 */
	public void setCarFirstInQueue(Car carFirstInQueue) {
		this.firstInQueue = carFirstInQueue;
	}
	

	/* GET METHODS */
	public Car getCarToUnpark() {return this.carToUnpark;}
	public KStack getKStack() {return this.carToUnpark.kstack;}
	
}
