
public class UnparkEvent {
	public boolean first; // indicates if this UnparkEvent is the first in the list - generally false
	public UnparkEvent next;
	public Car carToUnpark;
	public Car firstInQueue;
	public Street firstAim, finalAim;
	public int carsInTheWay;
	public KStack kstack;
	public int carSize;
	
	
	public UnparkEvent() {
		this.first = false;
		this.next = null;
		this.carToUnpark = null;
		this.firstInQueue = null;
		this.finalAim = null;
		this.firstAim = null;
		this.carsInTheWay = 0;
		this.kstack = null;
		this.carSize = 0;
	}
	
	public void setCarSize(int carSize) {
		this.carSize = carSize;
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
	
	//public boolean readyToBlock() {
	//	return true;
	//}
	
	public boolean isReadyToUnblock() {
		Street tempStreet1 = kstack;
		System.out.println("isReadyToUnblock: checking "+tempStreet1);
		System.out.println("isReadyToUnblock: going back "+(carsInTheWay+1)*carSize+" tiles");
		for (int i = 0; i < (carsInTheWay+1)*carSize; i++) {
			tempStreet1 = tempStreet1.prev1;
		}
		System.out.println("isReadyToUnblock: "+tempStreet1+" "+tempStreet1.car);
		System.out.println("isReadyToUnblock: "+this.firstInQueue);
		if (tempStreet1.car == firstInQueue)
			return true;
		return false;
	}
	
	public void unblockTiles() {
		Street tempStreet1 = kstack;
		System.out.println("unblockTiles: "+tempStreet1);
		
		for (int i = 0; i < (carsInTheWay+(firstInQueue==carToUnpark?1:2))*carSize; i++) {
			tempStreet1 = tempStreet1.prev1;
			System.out.println("unblockTiles: "+tempStreet1);
			tempStreet1.blockingKStack = null;
		}
	}
	
	public boolean isDone() {
		if (carToUnpark.currentStreet == kstack.prev1) {
			return true;
		}
		return false;
	}
}
