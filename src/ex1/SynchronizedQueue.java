package ex1;
import java.util.LinkedList;

/**
 * A producer-consumer queue
 * The size of the queue is not limited
 *
 * @param <T> the data type
 */
public class SynchronizedQueue<T> {

    private LinkedList<T> Data;

    public SynchronizedQueue() {
        Data = new LinkedList<T>();
    }

    /**
     * Pops the first element in the queue (returns it, and removes it)
     * @return the first element. or null if the queue is empty.
     */
    public synchronized T getNext() {
        if (Data.size() == 0) {
            return null;
        }

        return Data.removeFirst();
    }

    /**
     * Adds an element to the end of the queue
     * @param t    the element to add
     */
    public synchronized void enqueue(T t) {
        Data.addLast(t);
    }
}
