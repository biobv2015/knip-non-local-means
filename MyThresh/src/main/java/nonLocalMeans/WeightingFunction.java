package nonLocalMeans;

import net.imagej.ops.AbstractStrictFunction;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

public class WeightingFunction<I extends NeighborhoodPair<O>, O extends RealType<O>>
		extends AbstractStrictFunction<I, O> {

	public O compute(I input, O output) {

		double h=0.4;
		if(input.sigma<=30){
			h=0.4*input.sigma;
		}else if(input.sigma>30&&input.sigma<=75){
			h=0.35*input.sigma;
		}else if(input.sigma>75){
			h=0.3*input.sigma;
		}
		
		double maxdistance = 0;

		double sigma2 = 2 * input.sigma*input.sigma;

		double dist = calcDistance(input);

		if (maxdistance < (dist - sigma2)) {
			maxdistance = dist - sigma2;
		}

		DoubleType result = new DoubleType();

		result.setReal(Math.pow(Math.E, (-(maxdistance / (h*h) ) )));

		return (O) result;
	}

	public double calcDistance(NeighborhoodPair<O> input) {

		double dresult = 0;

		Cursor<O> pCursor = input.pNeighbors.cursor();
		Cursor<O> qCursor = input.qNeighbors.cursor();

		while (pCursor.hasNext()) {
			pCursor.next();
			qCursor.next();
			dresult += (pCursor.get().getRealDouble()
					- qCursor.get().getRealDouble())*(pCursor.get().getRealDouble()
					- qCursor.get().getRealDouble());
		}
		double div=1;
		if(input.span!=0){
			div = (2*input.span+1)*(2*input.span+1);
		}else{
			div = input.pNeighbors.dimension(0)*input.pNeighbors.dimension(0);
		}

		dresult = dresult / div;
		return dresult;
	}

}
