
public class DrivingTarget {
	public Street street;
	public char direction;
	public KStack unlockKStackForParking;
	public KStack unlockKStackForUnparking;
	public boolean releaseStreetBlock;
	public UnparkEvent unparkList;
	public UnparkEvent unparkEvent;
	public DrivingTarget() {
		this.street = null;
		this.direction = 'N';
		this.unlockKStackForUnparking = null;
		this.unlockKStackForParking = null;
		this.releaseStreetBlock = false;
		this.unparkList = null;
		this.unparkEvent = null;
	}
	public DrivingTarget(Street street, char direction, KStack kstackForUnparking, KStack kstackForParking, boolean releaseStreetBlock, UnparkEvent unparkingList, UnparkEvent unparkEvent) {
		this.street = street;
		this.direction = direction;
		this.unlockKStackForUnparking = kstackForUnparking;
		this.unlockKStackForParking = kstackForParking;
		this.releaseStreetBlock = releaseStreetBlock;
		this.unparkList = unparkingList;
		this.unparkEvent = unparkEvent;
	}
}
