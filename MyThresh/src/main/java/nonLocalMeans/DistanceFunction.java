package nonLocalMeans;

import net.imagej.ops.AbstractStrictFunction;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.type.numeric.real.DoubleType;

public class DistanceFunction <I extends NeighborhoodPair<T>, O extends Neighborhood<T>, T> extends AbstractStrictFunction<I, O> {

	public O compute(I input, O output) {
		
		DoubleType result = null; 
		
		output = (O) result;
				
		return output;
	}

}
