package ex1;

/***
 * Used to indicate the state of a cell on the original board.
 * Query is possible for the current state and one generation back
 */
public class Cell {
	private boolean currAlive,prevAlive;
	private int currGen;
	private Point location; //Location on original board
	
	public Cell (boolean currAlive,boolean prevAlive,int currGen){
		this.currAlive=currAlive;
		this.prevAlive = prevAlive;
		this.currGen = currGen;
		setPoint(null);
	}
	
	public Cell (Cell c){
		this.currAlive=c.currAlive;
		this.prevAlive = c.prevAlive;
		this.currGen = c.currGen;
	}
	
	public int getCurrGen(){
		return currGen;
	}
	
	public int getPrevGen(){
		return currGen-1;
	}
	
	public boolean currState(){
		return currAlive;
	}
	
	public boolean prevState(){
		return prevAlive;
	}
	
	/***
	 * Get the state of the cell (dead/alive) given a generation number.
	 * @param gen: the generation number (current or previous only)
	 * @return the state in the current gen or previous gen.
	 * @return if the gen is invalid returns null
	 */
	@SuppressWarnings("null")
	public boolean genState(int gen){
		if (gen == currGen)
			return currAlive;
		//In case we want to get its current state on gen number 0 - return null
		return gen == currGen - 1 ? prevAlive : (Boolean) null;
	}
	
	/***
	 * Advance to the next gen, changing the state
	 * @param state: state of the cell in the new gen
	 */
	public void Advance(boolean state){
		this.currAlive=state;
		this.prevAlive = currAlive;
		++this.currGen;
	}

	public Point getPoint() {
		return location;
	}

	public void setPoint(Point location) {
		this.location = location;
	}
	
	
}
