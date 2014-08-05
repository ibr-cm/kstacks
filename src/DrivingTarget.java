
public class DrivingTarget {
	public Street street;
	public char direction;
	public DrivingTarget() {
		this.street = null;
		this.direction = 'N';
	}
	public DrivingTarget(Street street, char direction) {
		this.street = street;
		this.direction = direction;
	}
}
