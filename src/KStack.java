
public class KStack extends Street{
	public int watermark;
	public int id;
	public boolean lockedForParking;
	public boolean lockedForUnparking;
	public UnparkEvent currentUnparkEvent;
	public KStack(int id) {
		super();
		this.watermark = 0;
		this.currentUnparkEvent = null;
		this.id  = id;
	}
}
