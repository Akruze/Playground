package ex1;

/**
 * A wrapper object for a cell.
 * This wrapper is sent via syncQueues from one thread to another
 */
public class IndexedCell {

    private Cell cell;                      // the cell to be stored
    private Point fromPoint;              // the position (x,y) in the cellArray of the sending thread
    private Point relativeThreadPoint;    // the relative vector between the sending thread and the receiving thread

    public IndexedCell(Cell cell, Point fromPoint, Point relativeThreadPoint) {
        this.cell = new Cell(cell);
        this.fromPoint = new Point(fromPoint);
        this.relativeThreadPoint = new Point(relativeThreadPoint);
    }

    public Cell GetCell() {return cell;}
    public Point GetFromPoint() {return fromPoint;}
    public Point GetRelativeThreadPoint() {return relativeThreadPoint;}
}
