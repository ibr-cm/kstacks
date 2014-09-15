
public class KStack extends Street{
	public int watermark;
	public int id;
	public boolean lockedForParking;
	public boolean lockedForUnparking;
	public UnparkEvent currentUnparkEvent;
	public boolean disabled;
	public KStack(int id) {
		super();
		this.disabled = false;
		this.watermark = 0;
		this.currentUnparkEvent = null;
		this.id  = id;
	}
}
