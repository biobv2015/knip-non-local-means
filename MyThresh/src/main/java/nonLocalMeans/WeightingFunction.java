package nonLocalMeans;

import net.imagej.ops.AbstractStrictFunction;
import net.imagej.ops.Function;
import net.imglib2.Cursor;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.type.numeric.real.DoubleType;

public class WeightingFunction<I extends NeighborhoodPair<T>, O extends Neighborhood<T>, T> extends AbstractStrictFunction<I, O>{

	public O compute(I input, O output) {
		
		Function<I, O> distf = new DistanceFunction<I, O, T>();
		
		Neighborhood<T> distances = null;
		
		//TODO set h and get distances
		
		double maxdistance = 0;
		double h = 0;
		
		Cursor<T> distCursor = distances.cursor();
		while(distCursor.hasNext()){
			T current = distCursor.next();
			double squaredDist = Math.pow(((DoubleType) current).getRealDouble(),2);
			double sigma2 = Math.pow(input.sigma, 2);
			
			if(maxdistance<(squaredDist-sigma2)){
				maxdistance=squaredDist-sigma2;
			}
		}
		
		DoubleType result = new DoubleType();
		
		result.setReal(Math.pow((Math.E),(-(maxdistance/h))));
		
		return (O) result;
	}

}
