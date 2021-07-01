package modelling;

import main.MySerializable;
import main.StoreHandler;

/**
 * This class represents a BlockPoint. A BlockPoint can be used in order to stop
 * trains driving over a specific position. This can be used for several things:
 * Signals, end of a TrainScope, Bumper...
 * 
 * @author David Lieb
 * @author david.lieb.00@gmail.com
 * @version 18.05.2021
 */
public class BlockPoint implements MySerializable {

	private static final long serialVersionUID = -8684899766541231456L;
	private final BlockPointProperties properties;
	private final Position position;

	/**
	 * @param properties -
	 *                   BLOCK_POSITIVE_DIRECTION,BLOCK_NEGATIVE_DIRECTION,BLOCK_ALL
	 * @param position   - The Position at witch the BlockPoint should block a track
	 *                   segment
	 * @throws Exception
	 */
	public BlockPoint(BlockPointProperties properties, Position position) {
		super();
		this.properties = properties;
		this.position = position;
		position.getEdge().addBlockPoint(this);
		StoreHandler.addObject(this);
		StoreHandler.addObject(position);

	}

	@Override
	public void delete() {
		this.position.getEdge().removeBlockPoint(this);
		StoreHandler.deleteObject(this);
		position.delete();
	}

	/**
	 * A BlockPoint can have these Properties:
	 * BLOCK_POSITIVE_DIRECTION,BLOCK_NEGATIVE_DIRECTION,BLOCK_ALL
	 * 
	 * @return the properties of this BlockPoint
	 */
	BlockPointProperties getProperties() {
		return properties;
	}

	/**
	 * @return the position where the BlockPoint Blocks the TrackSegment
	 */
	Position getPosition() {
		return position;
	}

}
