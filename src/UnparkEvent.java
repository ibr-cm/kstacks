
public class UnparkEvent {
	public boolean first;
	public UnparkEvent next;
	public Car carToUnpark;
	public Car firstInQueue;
	public Street firstAim, finalAim;
	public int carsInTheWay;
	public KStack kstack;
	
	
	public UnparkEvent() {
		this.first = false;
		this.next = null;
		this.carToUnpark = null;
		this.firstInQueue = null;
		this.finalAim = null;
		this.firstAim = null;
		this.carsInTheWay = 0;
		this.kstack = null;
	}
	
	public void setFirst() {
		this.first = true;
	}
	
	public void addEvent(UnparkEvent nextEvent) {
		if (this.next == null) {
			this.next = nextEvent;
		} else {
			this.next.addEvent(nextEvent);
		}
		System.out.println("New UnparkEvent "+nextEvent+" has been added to the queue.");
	}
	
	public void pop(UnparkEvent first) {
		UnparkEvent tempEvent1 = first;
		while (tempEvent1.next != this) {
			tempEvent1 = tempEvent1.next;
		}
		tempEvent1.next = this.next;
	}
	
	public void setCarToUnpark(Car carToUnpark) {
		this.carToUnpark = carToUnpark;
	}
	
	public void setCarFirstInQueue(Car carFirstInQueue) {
		this.firstInQueue = carFirstInQueue;
	}
	
	public void setKStack() {
		this.kstack = this.carToUnpark.kstack;
	}
	
	public boolean readyToBlock() {
		return true;
	}
}
