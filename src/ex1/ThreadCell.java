package ex1;

import java.util.ArrayList;

public class ThreadCell extends Thread {

	// Reference to an array that contains all threads
	private ThreadCell[][] threadArrayRef;
	private Point threadArraySize;
	//Position of the current thread in the array
	private Point threadLocation;

	//Start index on the original board
	private Point minPoint;
	//End index
	private Point maxPoint;

	private int maxGen;

	// A queue that holds the cells from the neighboring threads from the thread array.
	private SynchronizedQueue<Cell> syncQueue;

	/* Holds the cells of the current thread
	 * Includes the cells of its neighbors
	 */
	private Cell[][] cellArray;                        

	private Point cellArraySize;

	//Used to indicate the cells that belong solely to the thread
	private Point start, end;


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
        this.syncQueue = new SynchronizedQueue<Cell>();
        this.cellArraySize = new Point(maxPoint.getX()-minPoint.getX()+1+2,maxPoint.getY()-minPoint.getY()+1+2);   // the original size of this part of the board, plus the padding of the ghost-boarder
        this.cellArray = new Cell[this.cellArraySize.getX()][this.cellArraySize.getY()];
        this.start = new Point(1, 1); // (0,0) is part of the ghost board
        this.end = new Point (cellArraySize.getX()-2,cellArraySize.getY()-2);
        //endregion

        //region Copy the relevant part of the initialField to the cellArray (including the ghost-boarder)
        Point boardSize = new Point(initialField.length, initialField[0].length);
        for (int i = 0; i < this.cellArraySize.getX(); i++) {
            for (int j = 0; j < this.cellArraySize.getY(); j++) {
            	if(this.minPoint.getX()==0){ /*case the thread works on the top row and the ghost board needs to be filled with dead units*/
            		if(i==0)
            			this.cellArray[i][j] = new Cell(false,false,0);
            		continue;
            	}
            	if(this.maxPoint.getX()==initialField.length){ /*case the thread works on the bottom row and the ghost board needs to be filled with dead units*/
            		if(i==this.cellArraySize.getX()-1)
            			this.cellArray[i][j] = new Cell(false,false,0);
            		continue;
            	}
            	if(this.minPoint.getY()==0){ /*case the thread works on the most left column and the ghost board needs to be filled with dead units*/
            		if(j==0)
            			this.cellArray[i][j] = new Cell(false,false,0);
            		continue;
            	}
            	if(this.maxPoint.getY()==initialField[0].length){ /*case the thread works on the most right column and the ghost board needs to be filled with dead units*/
            		if(j==this.cellArraySize.getY()-1)
            			this.cellArray[i][j] = new Cell(false,false,0);
            		continue;
            	}
            	this.cellArray[i][j] = new Cell(initialField[maxPoint.getX()+i][maxPoint.getY()+j],false,0); //assume that the unit was dead before gen 0
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
                    if (cellArray[i][j].getCurrGen() != maxGen){
                    	if (UpdateCellIfPossible(i, j))                 // update the cell if possible and needed.
                    		SendCellIfNeeded(i, j);
                    	if (cellArray[i][j].getCurrGen() < maxGen)     // if this cell still didn't reach the maxGen
                            reachedMaxGen = false;
                    }
                }
            } /************************************************finished until here except for functions*/

            if (!GhostBoarderFinished()) {  // check if the ghost-boarder part has finished to be filled
                while (!UnpackQueue()) {}   // unpack the queue. if the queue was empty to begin with, than do a busy-wait until there is something in it.
                                                // if there was nothing to unpack, there is noting new to calculate
                                                // also, we know that more stuff needs to be unpacked because GhostBoarderFinished() was false
            }
        }
    }

    /**
     * Add a Cell to the Ghost-Boarder of this neighboring ThreadCell (Updated from Ron)
     * @param cell the cell you send - the cell itself already contain it's own point the real array
     */
    public void AddToQueue(Cell cell) {
        syncQueue.enqueue(new Cell(cell));
    }

    /**
     * Fill the real boolean board with this ThreadCell's cellArray Updated from Ron)
     * @param outField the full board that needs to be filled
     * @param Gen the generation of the board's cells
     */
    public void FillBoard(boolean[][] outField, int Gen) {
        for (int i = minPoint.getX(), m = start.getX(); i <= maxPoint.getX(); m++, i++) {
            for (int j = minPoint.getY(), n = start.getY(); j <= maxPoint.getY(); n++, j++) {
                outField[i][j] = this.cellArray[m][n].genState(Gen);
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
     * Places an Cell from the queue in the cellArray (Updated from Ron)
     * @param cell    the cell to be placed
     */
    private void PlaceIndexedCell(Cell cell) {
        int toX = 0;
        int toY = 0;

        if (cell.getPoint().getX()<this.minPoint.getX()) /*case the cell is in the most left column*/
            toX = 0;
        else if (cell.getPoint().getX()>this.maxPoint.getX()) /*case the cell is in the most right column*/
            toX = this.cellArraySize.getX() - 1;
        else toX = this.maxPoint.getX()-this.minPoint.getX(); /*case the cell is in between*/

        if (cell.getPoint().getY()<this.minPoint.getY()) /*case the cell is in the first row*/
            toY = 0;
        else if (cell.getPoint().getY()>this.maxPoint.getY()) /*case the cell is in the bottom row*/
            toY = this.cellArraySize.getY() - 1;
        else toY = this.maxPoint.getY()-this.minPoint.getY(); /*case the cell is in between*/


        cellArray[toX][toY] = new Cell(cell);
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