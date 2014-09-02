
public class DrivingTarget {
	public Street street;
	public char direction;
	public KStack unlockKStack;
	public boolean releaseStreetBlock;
	public UnparkEvent unparkList;
	public UnparkEvent unparkEvent;
	public DrivingTarget() {
		this.street = null;
		this.direction = 'N';
		this.unlockKStack = null;
		this.releaseStreetBlock = false;
		this.unparkList = null;
		this.unparkEvent = null;
	}
	public DrivingTarget(Street street, char direction, KStack kstack, boolean releaseStreetBlock, UnparkEvent unparkingList, UnparkEvent unparkEvent) {
		this.street = street;
		this.direction = direction;
		this.unlockKStack = kstack;
		this.releaseStreetBlock = releaseStreetBlock;
		this.unparkList = unparkingList;
		this.unparkEvent = unparkEvent;
	}
}
