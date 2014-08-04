
public class Street {
	public Street prev1, next1;
	public KStack kstack1, kstack2;
	public Street blockingKStack;
	public Car car;
	public int watermark;
	
	public Street() {
		this.prev1 = null;
		this.next1 = null;
		this.kstack1 = null;
		this.kstack2 = null;
		this.car = null;
		this.watermark = 0;
	}
	
	public void blockSpace(int i, Street kstack) {
		this.blockingKStack = kstack;
		if (i>1) {
			this.prev1.blockSpace(i-1, kstack);
		}
	}
	
	public void unblockSpace(int i) {
		this.blockingKStack = null;
		if (i>1) {
			this.prev1.unblockSpace(i-1);
		}
	}
}
