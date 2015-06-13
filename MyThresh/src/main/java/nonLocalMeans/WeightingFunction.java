package nonLocalMeans;

import net.imagej.ops.AbstractStrictFunction;
import net.imagej.ops.Function;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.type.numeric.real.DoubleType;

public class WeightingFunction<I extends NeighborhoodPair<T>, O extends Neighborhood<T>, T> extends AbstractStrictFunction<I, O>{

	public O compute(I input, O output) {
		
		Function<I, O> distf = new DistanceFunction<I, O, T>();
		
		DoubleType result = new DoubleType();
		
		return (O) result;
	}

}
