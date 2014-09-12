
public class ListEntry {
	public int index;
	public ListEntry next;
	public ListEntry() {
		index = -1;
		next = null;
	}
	
	public ListEntry(int index, ListEntry next) {
		this.index = index;
		this.next = next;
	}
	
	public void append(ListEntry list, int index) {
		if (list.index == -1) {
			list.index = index;
		} else {
			if (list.next == null)
				list.next = new ListEntry(index, null);
			else
				append(list.next, index);
		}
	}
	
	public int size() {
		if (this.next == null)
			return 0;
		return 1+this.next.size();
	}
	
	public void print(ListEntry list) {
		System.out.print(list.index+"-");
		if (list.next != null)
			print(list.next);
	}
	
	public ListEntry getListEntry(int position) {
		if (position != 0) {
			return this.next.getListEntry(position-1);
		}
		return this;
	}
}
