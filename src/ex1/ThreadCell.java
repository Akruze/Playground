package ex1;
public class ThreadCell extends Thread {

	// Reference to an array that contains all threads
	private ThreadCell[][] threadArrayRef;
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
        new Point(threadArraySize);
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
            	
            	/* if the thread works on the right most column
            	 * the neighboring cells need to be filled with dead units*/
            	if(this.maxPoint.getY()==initialBoard[0].length-1){ 
            		if(j==this.cellArraySize.getY()-1){
            			this.cellArray[i][j] = new Cell(false,false,0);
            			continue;
            		}
            	}
            	
            	boolean generation = initialBoard[minPoint.getX()+i-1][minPoint.getY()+j-1];
            	this.cellArray[i][j] = new Cell(generation,generation,0);
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
                    	if (updateCellIfPossible(i, j)){
                    		sendCellIfNeeded(cellArray[i][j]);
                    	}
                    		
                    	// if this cell still didn't reach the maxGen
                    	if (reachedMaxGen && cellArray[i][j].getCurrGen() < maxGen){
                    		reachedMaxGen = false;
                    	}
                    }
                }
            }

            // check if the neighbors' parts have finished
            if (!ghostBoardFinished()) {
            	/* unpack the queue. if the queue was empty to begin with, than do a busy-wait until there is something in it.
                * if there was nothing to unpack, there is noting new to calculate
                * also, we know that more stuff needs to be unpacked because GhostBoarderFinished() was false
                */
            	while (!unpackQueue()) {}
            }
        }
    }

    /**
     * Add a Cell to the Ghost-Boarder of this ThreadCell.
     * The cell itself already contain it's own point in the real array.
     * Is called by a thread to update a neighbor that a cell has changed.
     * @param cell the cell you send
     */
    public void addToQueue(Cell cell) {
        syncQueue.enqueue(new Cell(cell));
    }

    /**
     * Fill the original board with this Thread's cellArray
     * @param originalBoard the full board that needs to be filled
     * @param gen the generation of the board's cells
     */
    public void fillBoard(boolean[][] originalBoard, int gen) {
        for (int i = minPoint.getX(), m = start.getX(); i <= maxPoint.getX(); m++, i++) {
            for (int j = minPoint.getY(), n = start.getY(); j <= maxPoint.getY(); n++, j++) {
                originalBoard[i][j] = this.cellArray[m][n].genState(gen);
            }
        }
    }

    
    
    /*
     * Updates the cell at (x,y) if possible
     * Returns true if the cell was updated.
     */
    private boolean updateCellIfPossible(int x, int y) {
        Integer numNeighbors = numOfLiveNeighbors(x, y);

        if (numNeighbors == null)
            return false;
        
        cellArray[x][y].Advance(numNeighbors==3 || (cellArray[x][y].currState() && numNeighbors==2));
        return true;
    }
  
    
    /*
     * Calculates the number of live neighbors (if possible)
     * in a given (x,y) position
     * Returns the number of live neighbors, or null if can't calculate
     * (the neighbors don't have the right generation calculated yet)
     */
    private Integer numOfLiveNeighbors(int x, int y) {
        int genToCalcMinusOne = cellArray[x][y].getCurrGen();
        int livingNeighbors = 0;

        for (int i = x-1; i <= x+1; i++) {
            for (int j = y-1; j <= y+1; j++) {
            	if(cellArray[i][j].getPoint() == null) continue;
                if (i==x && j==y) continue;
                
                Boolean b = cellArray[i][j].genState(genToCalcMinusOne);
                
				if (b == null) return null;               
	            if (b) livingNeighbors++;               
            }
        }
        return livingNeighbors;
    }
    
    
    /*
     * Send the cell to the neighboring threads,
     * only if the cell is located on the edge
     */
    private void sendCellIfNeeded(Cell c) {
        if(!onBorder(c)) return;
        
        if(c.getPoint().getY()== maxPoint.getY()){ // send down
        	if(c.getPoint().getY() < initSize.getY()-1){
        		threadArrayRef[threadLocation.getX()][threadLocation.getY()+1].addToQueue(new Cell(c));	
        	}
        }
        
        if(c.getPoint().getY()==minPoint.getY()){//send up
        	if(c.getPoint().getY()>0){
        		threadArrayRef[threadLocation.getX()][threadLocation.getY()-1].addToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getX()==minPoint.getX()){ //send left
        	if(c.getPoint().getX()>0){
        		threadArrayRef[threadLocation.getX()-1][threadLocation.getY()].addToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getX()==maxPoint.getX()){  //send right
        	if(c.getPoint().getX() < initSize.getX()-1){
        		threadArrayRef[threadLocation.getX()+1][threadLocation.getY()].addToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getX()==maxPoint.getX()&&c.getPoint().getY()==maxPoint.getY()){ //send bottom right
        	if((c.getPoint().getY()<initSize.getY()-1)&&(c.getPoint().getX() < initSize.getX()-1)){
        		threadArrayRef[threadLocation.getX()+1][threadLocation.getY()+1].addToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getX()==minPoint.getX()&& c.getPoint().getY()==maxPoint.getY()){ // send bottom left
        	if(c.getPoint().getX() > 0 && c.getPoint().getY() < initSize.getY() - 1){
        		threadArrayRef[threadLocation.getX()-1][threadLocation.getY()+1].addToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getX() == maxPoint.getX() && c.getPoint().getY() == minPoint.getY()){ //send upper right 
        	if((c.getPoint().getY() > 0)&&(c.getPoint().getX() < initSize.getX() - 1)){
        		threadArrayRef[threadLocation.getX()+1][threadLocation.getY()-1].addToQueue(new Cell(c));
        	}
        }
        
        if(c.getPoint().getX()==minPoint.getX()&&c.getPoint().getY()==minPoint.getY()){ // send upper left
        	if(c.getPoint().getX() > 0 && c.getPoint().getY() > 0 ){
        		threadArrayRef[threadLocation.getX()-1][threadLocation.getY()-1].addToQueue(new Cell(c));
        	}
        }
    }

    
    /* Checks whether a cell is on the edge of the thread's border,
     * meaning that the cell is closest to the ghost-border but not on it.
     * Also checks whether there are neighbors to send i.e if the cell is (0,0)
     * it doesn't have neighbors to its left and above it. 
     */
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

    
    /*
     * Used as a busy-wait for the threads while its neighbors finish
     * updating their cells.
     * Unpacks the queue - empties the queue, and places all of its content
     * on the ghost-board
     * Returns true if there was something to unpack
     * (if the queue was not empty at the beginning) and false otherwise.
     */
    private boolean unpackQueue() {
    	if(syncQueue.isEmpty()) return true;
        Cell next = syncQueue.getNext();
        if (next == null)
            return false;
        while (next != null) {
            placeCell(next);
            next = syncQueue.getNext();
        }
        return true;
    }

    
    //Places a Cell from the queue in the cellArray
    private void placeCell(Cell c) {
        int toX = 0;
        int toY = 0;

        //case the cell is in the most left column
        if (c.getPoint().getX()<this.minPoint.getX()) 
            toX = 0;
        
        //case the cell is in the most right column
        else if (c.getPoint().getX()>this.maxPoint.getX()) 
            toX = this.cellArraySize.getX() - 1;
        
        //case the cell is in between
        else toX = c.getPoint().getX()-this.minPoint.getX()+1;

        
        
        //case the cell is in the first row
        if (c.getPoint().getY()<this.minPoint.getY())
            toY = 0;
        
        //case the cell is in the bottom row
        else if (c.getPoint().getY()>this.maxPoint.getY()) 
            toY = this.cellArraySize.getY() - 1;
        
        //case the cell is in between
        else toY = c.getPoint().getY()-this.minPoint.getY()+1;

        cellArray[toX][toY] = new Cell(c);
    }


    /*
     * Check if the ghost-board doesn't need to be updated anymore. 
     * When all of the cells on the ghost-board have reached the max generation
     * minus 1 (at least), we don't need to update it any more
     * we have enough information to calculate max generation of all
     * of the relevant cells in the cellArray.
     * Returns true if there is no need to update the ghost-board
     */
    private boolean ghostBoardFinished() {
        for (int i = 0; i < cellArraySize.getX(); ++i) {
            for (int j = 0; j < cellArraySize.getY(); j++) {

                if (!(cellArray[i][j].getCurrGen() >= maxGen-1))
                    return false;

                if (j==0 && i != 0 && i != cellArraySize.getX()-1)
					j = cellArraySize.getY() - 2;
            }
        }
        return true;
    }
}