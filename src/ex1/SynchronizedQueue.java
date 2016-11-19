package ex1;
import java.util.ArrayList;

/**
 * A producer-consumer queue
 * The size of the queue is not limited
 * @param <T> the data type
 */
public class SynchronizedQueue<T> {

    private ArrayList<T> cells;

    public SynchronizedQueue() {cells = new ArrayList<T>();}

    /**
     * Pops the first element in the queue (returns it, and removes it)
     * @return the first element. or null if the queue is empty.
     */
    public synchronized T getNext() {
		return cells.isEmpty() ? null : cells.remove(0);
	}

    /**
     * Adds an element to the end of the queue
     * @param e    the element to add
     */
    public synchronized void enqueue(T e) {cells.add(cells.size() , e);
	}
    
    public synchronized boolean isEmpty(){return cells.isEmpty();}
}
