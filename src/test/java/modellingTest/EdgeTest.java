package modellingTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import main.StoreHandler;
import modelling.Bumper;
import modelling.Node;
import modelling.Switch;
import modelling.Track;
import modelling.Edge.EdgeIteratorObject;

class EdgeTest {
	
	@Test
	void testIterator() throws Exception {
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
		
		Iterator<EdgeIteratorObject> iterator = t4.getCurrentTrackEdge().iterator(true);
		assertEquals(iterator.next().getEdge().getOrigin(), t4);
		assertEquals(iterator.next().getEdge().getOrigin(), t3);
		assertEquals(iterator.next().getEdge().getOrigin(), t1);
		assertEquals(iterator.next().getEdge().getOrigin(), t2);
		assertEquals(iterator.next().getEdge().getOrigin(), s1);
		assertEquals(iterator.next().getEdge().getOrigin(), t5);
		assertEquals(iterator.next().getEdge().getOrigin(), b1);
		
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
