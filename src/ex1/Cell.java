package ex1;

public class Cell {
	private boolean currAlive,prevAlive;
	private int currGen;
	
	public Cell (boolean currAlive,boolean prevAlive,int currGen){
		this.currAlive=currAlive;
		this.prevAlive = prevAlive;
		this.currGen = currGen;
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
	
	@SuppressWarnings("null")
	public boolean genState(int gen){
		if (gen == currGen)
			return currAlive;
		return gen == currGen - 1 ? prevAlive : (Boolean) null;
	}
	
	public void Advance(boolean state){
		this.currAlive=state;
		this.prevAlive = currAlive;
		++this.currGen;
	}
	
	
}
