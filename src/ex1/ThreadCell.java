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
	
	//Original border size
	private Point initSize;

	// A queue that holds the cells from the neighboring threads from the thread array.
	private SynchronizedQueue<Cell> syncQueue;

	/* Holds the cells of the current thread
	 * Includes the cells of its neighbors
	 */
	private Cell[][] cellArray;                        

	private Point cellArraySize;

	//Used to indicate the cells that belong solely to the thread
	private Point start, end;


    public ThreadCell(boolean[][] initialBoard, ThreadCell[][] threadArray, int maxGen,
                     Point threadArraySize, Point threadLocation, Point minPoint, Point maxPoint) {
        //Init the private fields
        this.threadArrayRef = threadArray;
        this.threadArraySize = new Point(threadArraySize);
        this.threadLocation = new Point(threadLocation);
        this.minPoint = new Point(minPoint);
        this.maxPoint = new Point(maxPoint);
        this.maxGen = maxGen;
        this.syncQueue = new SynchronizedQueue<Cell>();
        this.cellArraySize = new Point(maxPoint.getX() - minPoint.getX() + 3,maxPoint.getY() - minPoint.getY() + 3);   // the original size of this part of the board, plus the padding of the ghost-boarder
        this.cellArray = new Cell[this.cellArraySize.getX()][this.cellArraySize.getY()];
        this.start = new Point(1, 1); //(0,0) is part of the neighbor's board
        this.end = new Point (cellArraySize.getX()-2,cellArraySize.getY()-2);
        this.initSize = new Point (initialBoard.length, initialBoard[0].length);
      //Copy the relevant part to the cellArray (including neighbors)
        for (int i = 0; i < this.cellArraySize.getX(); i++) {
            for (int j = 0; j < this.cellArraySize.getY(); j++) {
            	
            	/* if the thread works on the top row 
            	 * the neighbors are dead units*/
            	if(this.minPoint.getX()==0){ 
            		if(i==0){
            			this.cellArray[i][j] = new Cell(false,false,0);
            			continue;
            		}
            	}
            	
            	/* if the thread works on the bottom row
            	 * the neighboring cells need to be filled with dead units*/
            	if(this.maxPoint.getX()==initialBoard.length-1){ 
            		if(i==this.cellArraySize.getX()-1){
            			this.cellArray[i][j] = new Cell(false,false,0);
            			continue;
            		}
            	}
            	
            	/* if the thread works on the left most column
            	 * the neighboring cells need to be filled with dead units*/
            	if(this.minPoint.getY()==0){
            		if(j==0){
            			this.cellArray[i][j] = new Cell(false,false,0);
            			continue;
            		}
            	}
            	
//            	if(this.getName().equals("Thread-1")){//TODO
//            		System.out.println("SDGSG");
//            	}
            	/* if the thread works on the right most column
            	 * the neighboring cells need to be filled with dead units*/
            	if(this.maxPoint.getY()==initialBoard[0].length-1){ 
            		if(j==this.cellArraySize.getY()-1){
            			this.cellArray[i][j] = new Cell(false,false,0);
            			continue;
            		}
            	}
            	//assume that the unit was dead before gen 0
            	System.out.println(this.getName() +": " + i + ", " +j +" -- "+" "+minPoint+" xLen: "+initialBoard.length+" yLen: "+initialBoard[0].length);
            	this.cellArray[i][j] = new Cell(initialBoard[minPoint.getX()+i-1][minPoint.getY()+j-1],false,0);
            	this.cellArray[i][j].setPoint(new Point(minPoint.getX()+i-1, minPoint.getY()+j-1));
            }
        }
    }

    
    @Override
    public void run() {
    	//Flag to determine whether we've reached max gen
        boolean reachedMaxGen = false;
        while (!reachedMaxGen) {
            reachedMaxGen = true;

            for (int i = start.getX(); i <= end.getX(); i++) {
                for (int j = start.getY(); j <= end.getY(); j++) {
                    if (cellArray[i][j].getCurrGen() != maxGen){
                    	// update the cell if possible and needed.
                    	if (UpdateCellIfPossible(i, j))
                    		SendCellIfNeeded(cellArray[i][j]);
                    	// if this cell still didn't reach the maxGen
                    	if (reachedMaxGen && cellArray[i][j].getCurrGen() < maxGen)
                            reachedMaxGen = false;
                    }
                }
            }

            // check if the neighbors' parts have finished
            if (!GhostBoarderFinished()) {
            	/* unpack the queue. if the queue was empty to begin with, than do a busy-wait until there is something in it.
                * if there was nothing to unpack, there is noting new to calculate
                * also, we know that more stuff needs to be unpacked because GhostBoarderFinished() was false
                */
            	while (!UnpackQueue()) {}
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

    /**
     * Send the cell at (x,y) to the neighboring cells,
     * only if (x,y) is on the inner boarder
     * (the most outer boarder that is not the ghost boarder)
     * @param x x position
     * @param y y position
     */
    private void SendCellIfNeeded(Cell c) {
        if(!onBorder(c)) return;
    	
        if(c.getPoint().getX()== maxPoint.getX()){ // send down
        	if(c.getPoint().getX() < initSize.getX()){
        		threadArrayRef[threadLocation.getX()][threadLocation.getY()+1].AddToQueue(new Cell(c));	
        	}
        }
        
        if(c.getPoint().getX()==minPoint.getX()){//send up
        	if(c.getPoint().getX()>0){
        		threadArrayRef[threadLocation.getX()][threadLocation.getY()-1].AddToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getY()==maxPoint.getY()){ //send left
        	if(c.getPoint().getY()<initSize.getY()){
        		threadArrayRef[threadLocation.getX()-1][threadLocation.getY()].AddToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getY()==minPoint.getY()){  //send right
        	if(c.getPoint().getY()>0){
        		threadArrayRef[threadLocation.getX()+1][threadLocation.getY()].AddToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint()==maxPoint){ //send bottom right
        	if((c.getPoint().getY()<initSize.getY())&&(c.getPoint().getX() < initSize.getX())){
        		threadArrayRef[threadLocation.getX()+1][threadLocation.getY()+1].AddToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getX()==maxPoint.getX()&& c.getPoint().getY()==minPoint.getY()){ // send bottom left
        	if(c.getPoint().getX() < initSize.getX()&&c.getPoint().getY()>0){
        		threadArrayRef[threadLocation.getX()-1][threadLocation.getY()+1].AddToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint()==maxPoint){ //send upper right 
        	if((c.getPoint().getY() > 0)&&(c.getPoint().getX() < initSize.getX())){
        		threadArrayRef[threadLocation.getX()+1][threadLocation.getY()-1].AddToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getX()==maxPoint.getX()&& c.getPoint().getY()==minPoint.getY()){ // send upper left
        	if(c.getPoint().getX() > 0 && c.getPoint().getY() > 0 ){
        		threadArrayRef[threadLocation.getX()-1][threadLocation.getY()-1].AddToQueue(new Cell(c));
        	}
        }
    }
        
    private boolean onBorder(Cell c) {
		int cellPointX = c.getPoint().getX();
		int cellPointY = c.getPoint().getY();
		boolean betweenX = cellPointX > minPoint.getX() && cellPointX < maxPoint.getX();
		boolean betweenY = cellPointY > minPoint.getY() && cellPointY < maxPoint.getY();
		if (betweenX && betweenY)
			return false;
		if (betweenX && (cellPointY == 0 || cellPointY == initSize.getY() - 1))
			return false;
		if (betweenY && (cellPointX == 0 || cellPointX == initSize.getX() - 1))
			return false;
		return true;
	}



    /**
     * Places an Cell from the queue in the cellArray (Updated from Ron)
     * @param c    the cell to be placed
     */
    private void placeCell(Cell c) {
        int toX = 0;
        int toY = 0;

        if (c.getPoint().getX()<this.minPoint.getX()) /*case the cell is in the most left column*/
            toX = 0;
        else if (c.getPoint().getX()>this.maxPoint.getX()) /*case the cell is in the most right column*/
            toX = this.cellArraySize.getX() - 1;
        else toX = this.maxPoint.getX()-this.minPoint.getX(); /*case the cell is in between*/

        if (c.getPoint().getY()<this.minPoint.getY()) /*case the cell is in the first row*/
            toY = 0;
        else if (c.getPoint().getY()>this.maxPoint.getY()) /*case the cell is in the bottom row*/
            toY = this.cellArraySize.getY() - 1;
        else toY = this.maxPoint.getY()-this.minPoint.getY(); /*case the cell is in between*/


        cellArray[toX][toY] = new Cell(c);
    }

    /**
     * Unpacks the queue - empties the queue, and places all of its content on the Ghost-Boarder (Updated From Ron)
     * @return true if there was something to unpack (if the queue was not empty at the beginning). and false otherwise.
     */
    private boolean UnpackQueue() {
        Cell next = syncQueue.getNext();
        if (next == null)
            return false;
        while (next != null) {
            placeCell(next);
            next = syncQueue.getNext();
        }
        return true;
    }


    /**
     * Calculates the number of live neighbors (if possible) (Updated From Ron)
     * @param x    x position of the cell
     * @param y    y position of the cell
     * @return the number of live neighbors, or null if can't calculate (the neighbors don't have the right generation calculated)
     */
    private Integer NumOfLiveNeighbors(int x, int y) {
        int genToCalcMinusOne = cellArray[x][y].getCurrGen();
        int count = 0;

        for (int i = x-1; i <= x+1; i++) {
            for (int j = y-1; j <= y+1; j++) {
                if (i==x && j==y)
                    continue;

                Boolean b = cellArray[i][j].genState(genToCalcMinusOne);
                if (b == null)
                    return null;
                if (b)
                    count++;
            }
        }

        return count;
    }

    /**
     * Updates the cell at (x,y) if possible (Updated From Ron)
     * @param x    x position of the cell
     * @param y    y position of the cell
     * @return true if the cell was updated. or false otherwise
     */
    private boolean UpdateCellIfPossible(int x, int y) {
        Integer numNeighbors = NumOfLiveNeighbors(x, y);

        if (numNeighbors == null)
            return false;

        cellArray[x][y].Advance(numNeighbors==3 || (cellArray[x][y].currState() && numNeighbors==2));
        return true;
    }

    /**
     * Check if the Ghost-Boarder doesn't need to be updated anymore. (Updated From Ron) 
     *
     * When all of the cells on the ghost-boarder have reached the maxGen-1 (at least),
     * we don't need to update it any more - we have enough information to calculate maxGen of all the relevant cells in the cellArray.
     *
     * @return true if there is no need to update the ghost-boarder
     */
    private boolean GhostBoarderFinished() {
        for (int i = 0; i < cellArraySize.getX(); i++) {
            for (int j = 0; j < cellArraySize.getY(); j++) {

                if (!(cellArray[i][j].getCurrGen() >= maxGen-1))
                    return false;

                if (j==0 && i != 0 && i != cellArraySize.getX()-1) {
                    j = cellArraySize.getY() - 2;
                }
            }
        }
        return true;
    }
}