package ex1;


public class ParallelGameOfLife implements GameOfLife {

	public boolean[][][] invoke(boolean[][] initalField, int hSplit, int vSplit,
			int generations) {

		//region Init threadArray
		Point threadArraySize = new Point(vSplit, hSplit);//TODO: check order of v and h
		ThreadCell[][] threadArray = new ThreadCell[threadArraySize.getX()][threadArraySize.getY()];

		Point boardSize = new Point(initalField.length, initalField[0].length);
		Point cellsPerThread = Point.Div(boardSize, threadArraySize);

		for (int i = 0; i < threadArraySize.getX(); i++) {
			for (int j = 0; j < threadArraySize.getY(); j++) {
				Point start = Point.Mul(new Point(i, j), cellsPerThread);
				Point end = Point.Add(Point.Add(start, cellsPerThread), -1);
				if (i == threadArraySize.getX()-1)
					end = new Point(boardSize.getX() - 1, end.getY());
				if (j == threadArraySize.getY()-1)
					end = new Point(end.getX(), boardSize.getY() - 1);

				threadArray[i][j] = new ThreadCell(initalField, threadArray, generations,
						threadArraySize, new Point(i, j), start, end);
			}
		}
		//endregion

		//region Start the Threads
		for (int i = 0; i < threadArraySize.getX(); i++) {
			for (int j = 0; j < threadArraySize.getY(); j++) {
				threadArray[i][j].start();
			}
		}
		//endregion

		//region Join the Threads
		for (int i = 0; i < threadArraySize.getX(); i++) {
			for (int j = 0; j < threadArraySize.getY(); j++) {
				try {
					threadArray[i][j].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		//endregion

		//region Build Result Board
		boolean[][][] outBoard = new boolean[2][boardSize.getX()][boardSize.getY()];

		for (int i = 0; i < threadArraySize.getX(); i++) {
			for (int j = 0; j < threadArraySize.getY(); j++) {
				threadArray[i][j].FillBoard(outBoard[0], generations-1);
				threadArray[i][j].FillBoard(outBoard[1], generations);
			}
		}
		//endregion

		return outBoard;
	}

}

