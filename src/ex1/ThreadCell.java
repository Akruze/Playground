package ex1;

import java.util.ArrayList;

public class ThreadCell extends Thread {

    //region Private Properties
    private ThreadCell[][] threadArrayRef;               // A reference to the array that contains all of the ThreadObjs.
    private Point threadArraySize;                    // The size of the thread array.
    private Point threadLocation;                     // The position of this ThreadObj in the threadArray.

    private Point minPoint;                           // start index on the real full board
    private Point maxPoint;                           // end index on the real full board

    private int maxGen;                                 // The number of generations that need to be calculated

    private SynchronizedQueue<IndexedCell> syncQueue;   // A queue that holds the incoming cells from the neighboring ThreadObjs in the thread array.

    private Cell[][] cellArray;                         // An array to store all the cells that this ThreadObj works on.
                                                            // This array also has a "ghost-boarder" - this boarder will store the incoming cells from the neighboring ThreadObjs.
                                                            // The queue will dump its content to this boarder.
    private Point cellArraySize;                      // The size of the cell array

    private Point start, end;                         // start and end indexes (x and y) of the relevant part of the cell array (excluding the ghost-boarder)
    //endregion

    //region Constructor
    public ThreadCell(boolean[][] initialField, ThreadCell[][] threadArray, int maxGen,
                     Point threadArraySize, Point threadLocation, Point minPoint, Point maxPoint) {
        //region Init the private fields
        this.threadArrayRef = threadArray;
        this.threadArraySize = new Point(threadArraySize);
        this.threadLocation = new Point(threadLocation);
        this.minPoint = new Point(minPoint);
        this.maxPoint = new Point(maxPoint);
        this.maxGen = maxGen;
        this.syncQueue = new SynchronizedQueue<IndexedCell>();
        this.cellArraySize = new Point(maxPoint.getX()-minPoint.getX()+1+2,maxPoint.getY()-minPoint.getY()+1+2);   // the original size of this part of the board, plus the padding of the ghost-boarder
        this.cellArray = new Cell[this.cellArraySize.getX()][this.cellArraySize.getY()];
        this.start = new Point(1, 1); // (0,0) is part of the ghost board
        this.end = new Point (cellArraySize.getX()-2,cellArraySize.getY()-2);
        //endregion

        //region Copy the relevant part of the initialField to the cellArray (including the ghost-boarder)
        Point boardSize = new Point(initialField.length, initialField[0].length);
        for (int i = 0; i < this.cellArraySize.getX(); i++) {
            for (int j = 0; j < this.cellArraySize.getY(); j++) {
            	if(this.maxPoint.getX()==0){ /*case the thread works on the top row and the ghost board need to be filled with dead units*/
            		if(i==0)
            			this.cellArray[i][j] = new Cell(false,false,0);
            	}
            		
                Point from = Point.Mod(Point.Add(minPoint, Point.Add(boardSize, new Point(i - 1, j - 1))), boardSize);
                this.cellArray[i][j] = new Cell(initialField[from.getX()][from.getY()]);
            }
        }
        //endregion
    }
    //endregion

    //region Public Functions
    /**
     * The Threads run() function
     */
    @Override
    public void run() {
        boolean reachedMaxGen = false;  // Will determine whether all of the relevant cells have reached maxGen

        while (!reachedMaxGen) {    // If the relevant cells haven't reached the maxGen, continue the calculation
            reachedMaxGen = true;

            for (int i = start.getX(); i <= end.getX(); i++) {
                for (int j = start.getY(); j <= end.getY(); j++) {
                    if (cellArray[i][j].GetTopGenerationNum() == maxGen)    // if this cell has reached maxGen, no need to calculate its next generation
                        continue;

                    if (UpdateCellIfPossible(i, j))                         // update the cell if possible.
                        SendCellIfNeeded(i, j);                             // if possible, than send the cell to the neighboring ThreadObjs if needed.

                    if (cellArray[i][j].GetTopGenerationNum() < maxGen)     // if this cell still didn't reach the maxGen
                        reachedMaxGen = false;
                }
            }

            if (!GhostBoarderFinished()) {  // check if the ghost-boarder has finished to be filled (look in GhostBoarderFinished)
                while (!UnpackQueue()) {}   // unpack the queue. if the queue was empty to begin with, than do a busy-wait until there is something in it.
                                                // if there was nothing to unpack, there is noting new to calculate
                                                // also, we know that more stuff needs to be unpacked because GhostBoarderFinished() was false
            }
        }
    }

    /**
     * Add a Cell to the Ghost-Boarder of this neighboring ThreadObj
     * @param cell the cell you send
     * @param fromPoint the cell's location in your cell array
     * @param relativeThreadPoint the relative vector of your thread
     */
    public void AddToQueue(Cell cell, Point fromPoint, Point relativeThreadPoint) {
        syncQueue.enqueue(new IndexedCell(cell, fromPoint, relativeThreadPoint));
    }

    /**
     * Fill the real boolean board with this ThreadObj's cellArray
     * @param outField the full board that needs to be filled
     * @param Gen the generation of the board's cells
     */
    public void FillBoard(boolean[][] outField, int Gen) {
        for (int i = minPoint.getX(), m = start.getX(); i <= maxPoint.getX(); m++, i++) {
            for (int j = minPoint.getY(), n = start.getY(); j <= maxPoint.getY(); n++, j++) {
                outField[i][j] = this.cellArray[m][n].GetGenIfExist(Gen);
            }
        }
    }
    //endregion

    //region Send Calculated Cell To Other Threads
    /**
     * Send the cell at (x,y) to the neighboring cells, only if (x,y) is on the inner boarder (the most outer boarder that is not the ghost boarder)
     * @param x x position
     * @param y y position
     */
    private void SendCellIfNeeded(int x, int y) {
        Cell cell = cellArray[x][y];
        if (!(x==start.getX() || x==end.getX() || y==start.getY() || y==end.getY())) //if not on boarder, than exit
            return;

        ArrayList<Point> relativePointsArray = new ArrayList<Point>();

        //region Add relative vectors to the relativePointsArray based on (x,y)
        if (x == start.getX()) {
            relativePointsArray.add(new Point(-1, 0));
        }
        if (x == end.getX()) {
            relativePointsArray.add(new Point(1, 0));
        }

        if (y == start.getY()) {
            relativePointsArray.add(new Point(0, -1));
        }
        if (y == end.getY()) {
            relativePointsArray.add(new Point(0, 1));
        }

        if (x == start.getX() && y == start.getY()) {
            relativePointsArray.add(new Point(-1, -1));
        }
        if (x == start.getX() && y == end.getY()) {
            relativePointsArray.add(new Point(-1, 1));
        }
        if (x == end.getX() && y == start.getY()) {
            relativePointsArray.add(new Point(1, -1));
        }
        if (x == end.getX() && y == end.getY()) {
            relativePointsArray.add(new Point(1, 1));
        }
        //endregion

        //region Go over the relativePointsArray and send the cell to the relevant ThreadObjs in the threadArray (uses threadArrayRef)
        for (Point p : relativePointsArray) {
            Point dest = Point.Add(Point.Add(p, this.threadLocation), this.threadArraySize);
            dest = Point.Mod(dest, this.threadArraySize);
            threadArrayRef[dest.getX()][dest.getY()].AddToQueue(new Cell(cell), new Point(x, y), p);
        }
        //endregion
    }
    //endregion

    //region Queue Unpacking Functions
    /**
     * Places an indexedCell (a cell sent from a neighboring ThreadObj via the syncQueue) in the cellArray
     * @param ic    the indexed cell
     */
    private void PlaceIndexedCell(IndexedCell ic) {
        int toX = 0;
        int toY = 0;

        if (ic.GetRelativeThreadPoint().getX() == 0)
            toX = ic.GetFromPoint().getX();
        else if (ic.GetRelativeThreadPoint().getX() < 0)
            toX = this.cellArraySize.getX() - 1;

        if (ic.GetRelativeThreadPoint().getY() == 0)
            toY = ic.GetFromPoint().getY();
        else if (ic.GetRelativeThreadPoint().getY() < 0)
            toY = this.cellArraySize.getY() - 1;

        cellArray[toX][toY] = ic.GetCell();
    }

    /**
     * Unpacks the queue - empties the queue, and places all of its content on the Ghost-Boarder
     * @return true if there was something to unpack (if the queue was not empty at the beginning). and false otherwise.
     */
    private boolean UnpackQueue() {
        IndexedCell next = syncQueue.getNext();
        if (next == null)
            return false;
        while (next != null) {
            PlaceIndexedCell(next);
            next = syncQueue.getNext();
        }
        return true;
    }
    //endregion

    //region Update Cell Functions

    /**
     * Calculates the number of live neighbors (if possible)
     * @param x    x position of the cell
     * @param y    y position of the cell
     * @return the number of live neighbors, or null if can't calculate (the neighbors don't have the right generation calculated)
     */
    private Integer NumOfLiveNeighbors(int x, int y) {
        int genToCalcMinusOne = cellArray[x][y].GetTopGenerationNum();
        int count = 0;

        for (int i = x-1; i <= x+1; i++) {
            for (int j = y-1; j <= y+1; j++) {
                if (i==x && j==y)
                    continue;

                Boolean b = cellArray[i][j].GetGenIfExist(genToCalcMinusOne);
                if (b == null)
                    return null;
                if (b)
                    count++;
            }
        }

        return count;
    }

    /**
     * Updates the cell at (x,y) if possible
     * @param x    x position of the cell
     * @param y    y position of the cell
     * @return true if the cell was updated. or false otherwise
     */
    private boolean UpdateCellIfPossible(int x, int y) {
        Integer numNeighbors = NumOfLiveNeighbors(x, y);

        if (numNeighbors == null)
            return false;

        cellArray[x][y].AddGen(numNeighbors==3 || (cellArray[x][y].GetTopState() && numNeighbors==2));
        return true;
    }
    //endregion

    //region Checking Functions
    /**
     * Check if the Ghost-Boarder doesn't need to be updated anymore.
     *
     * When all of the cells on the ghost-boarder have reached the maxGen-1 (at least),
     * we don't need to update it any more - we have enough information to calculate maxGen of all the relevant cells in the cellArray.
     *
     * @return true if there is no need to update the ghost-boarder
     */
    private boolean GhostBoarderFinished() {
        for (int i = 0; i < cellArraySize.getX(); i++) {
            for (int j = 0; j < cellArraySize.getY(); j++) {

                if (!(cellArray[i][j].GetTopGenerationNum() >= maxGen-1))
                    return false;

                if (j==0 && i != 0 && i != cellArraySize.getX()-1) {
                    j = cellArraySize.getY() - 2;
                }
            }
        }
        return true;
    }
    //endregion
}
