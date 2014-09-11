
public class SpawnEvent {
	public EventItem eventItem; // pointer to the event
	public SpawnEvent next; // pointer to the next spawn in the list
	
	/**
	 * Generic Contructor
	 */
	public SpawnEvent() {
		this.eventItem = null;
		this.next = null;
	}
	
	/**
	 * Constructor which creates a new SpawnEvent.
	 * @param eventItem The eventItem that should be linked to this SpawnEvent. 
	 */
	public SpawnEvent(EventItem eventItem) {
		this.eventItem = eventItem;
		this.next = null;
	}
	
	/**
	 * Puts a new SpawnEvent at the end of the list of SpawnEvents.
	 * @param head First event on the list. Used to find the last element.
	 * @param eventItem EventItem that should go to the end of the list.
	 */
	public void addNewEvent(SpawnEvent head, EventItem eventItem) {
		SpawnEvent tempSpawnEvent = head;
		while (tempSpawnEvent.next != null)
			tempSpawnEvent = tempSpawnEvent.next;
		tempSpawnEvent.next = new SpawnEvent(eventItem);
	}
}
