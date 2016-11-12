package ex1;

/***
 * Used to indicate a position in a 2-D array
 */
public class Point {
	private int x,y;
	
	public Point (int x,int y){
		this.x=x;
		this.y=y;
	}
	
	public Point (Point p){
		this.x=p.x;
		this.y=p.y;
	}
	
	public int getX(){return x;}
	public int getY(){return y;}
	public void setX(int x, Point p){p.x=x;}
	public void sety(int y, Point p){p.y=y;}
	@Override
	public String toString(){
		return "("+x+","+y+")";
	}
}
