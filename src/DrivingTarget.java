
public class DrivingTarget {
	public Street street;
	public char direction;
	public DrivingTarget next;
	public DrivingTarget() {
		this.street = null;
		this.direction = 'N';
		this.next = null;
	}
}
