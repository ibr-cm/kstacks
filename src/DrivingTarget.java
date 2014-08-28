
public class DrivingTarget {
	public Street street;
	public char direction;
	public KStack unlockKStack;
	public DrivingTarget() {
		this.street = null;
		this.direction = 'N';
		this.unlockKStack = null;
	}
	public DrivingTarget(Street street, char direction, KStack kstack) {
		this.street = street;
		this.direction = direction;
		this.unlockKStack = kstack;
	}
}
