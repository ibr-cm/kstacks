
public class KStack extends Street{
	public int watermark;
	public boolean lockedForParking;
	public boolean lockedForUnparking;
	public UnparkEvent currentUnparkEvent;
	public KStack() {
		super();
		this.watermark = 0;
		this.currentUnparkEvent = null;
	}
}
