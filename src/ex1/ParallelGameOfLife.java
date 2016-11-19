package ex1;


public class ParallelGameOfLife implements GameOfLife {

	public boolean[][][] invoke(boolean[][] initalField, int hSplit, int vSplit,
			int generations) {

		//Init array of threads
		Point threadArraySize = new Point(vSplit, hSplit);
		ThreadCell[][] threadArray = new ThreadCell[threadArraySize.getX()][threadArraySize.getY()];

		Point boardSize = new Point(initalField.length, initalField[0].length);
		
		int horizotalCellPerThread = initalField.length/hSplit;
		int verticalCellPerThread = initalField[0].length/vSplit;
		
		for (int i = 0; i < threadArraySize.getX(); i++) {
			for (int j = 0; j < threadArraySize.getY(); j++) {
				Point start = new Point(i*verticalCellPerThread,j*horizotalCellPerThread);
				Point end = new Point ((i+1)*verticalCellPerThread-1,(j+1)*horizotalCellPerThread-1);
				if (i == threadArraySize.getX()-1)
					end.setX(boardSize.getX() - 1);
				if (j == threadArraySize.getY()-1)
					end.sety(boardSize.getY() - 1);

				threadArray[i][j] = new ThreadCell(initalField, threadArray, generations,
						threadArraySize, new Point(i, j), start, end);
			}
		}

		//Start the Threads
		for (int i = 0; i < threadArraySize.getX(); i++) {
			for (int j = 0; j < threadArraySize.getY(); j++) {
				threadArray[i][j].start();
			}
		}

		//Join the Threads
		for (int i = 0; i < threadArraySize.getX(); i++) {
			for (int j = 0; j < threadArraySize.getY(); j++) {
				try {
					threadArray[i][j].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}


		//Build Result Board
		boolean[][][] outBoard = new boolean[2][boardSize.getX()][boardSize.getY()];

		for (int i = 0; i < threadArraySize.getX(); i++) {
			for (int j = 0; j < threadArraySize.getY(); j++) {
				threadArray[i][j].fillBoard(outBoard[0], generations-1);
				threadArray[i][j].fillBoard(outBoard[1], generations);
			}
		}

		return outBoard;
	}

}

