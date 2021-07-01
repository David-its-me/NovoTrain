package modellingTest;

import org.junit.jupiter.api.Test;

import main.StoreHandler;
import modelling.Bumper;
import modelling.Node;
import modelling.Position;
import modelling.Switch;
import modelling.Track;

class PositionTest {

	@Test
	void  test() throws Exception {
		if(!StoreHandler.isOpen()) {
			StoreHandler.open();
		}
		
		Node n1 = new Node();
		Node n2 = new Node();
		Node n3 = new Node();
		Node n4 = new Node();
		Node n5 = new Node();
		Node n6 = new Node();
		Node n7 = new Node();

		Track t1 = new Track(n1, n2, 1,50);
		Track t2 = new Track(n2, n3, 1,50);
		Track t3 = new Track(n1, n7, 1,50);
		Track t4 = new Track(n4, n7, 0.8,50);
		Track t5 = new Track(n6, n5, 0.8,50);
		Switch s1 = new Switch(n3, n5, n4, 0.1, 0.1,10,50);
		Bumper b1 = new Bumper(n6, 0.1,10);
		
		Position p1 = new Position(t4.getCurrentTrackEdge(), 0.9);
		Position p2 = new Position(t4.getCurrentTrackEdge(), 0.1);
		
		System.out.println(p1.calculateDistanceTo(p2, 5));
		
		p1.delete();
		p2.delete();
		
		t1.delete();
		t2.delete();
		t3.delete();
		t4.delete();
		t5.delete();
		s1.delete();
		b1.delete();
		
		n1.delete();
		n2.delete();
		n3.delete();
		n4.delete();
		n5.delete();
		n6.delete();
		n7.delete();
		
		StoreHandler.close();
	}

}
