package nonLocalMeans;

import net.imagej.ops.AbstractStrictFunction;
import net.imglib2.Cursor;
import net.imglib2.type.numeric.ARGBDoubleType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

public class WeightingFunctionRGB<I extends NeighborhoodPair<O>, O extends NumericType<O>>
		extends AbstractStrictFunction<I, O> {

	@Override
	public O compute(I input, O output) {
		// TODO set h depending on sigma

		double maxdistance = 0;
		double h = 0.4;

		double sigma2 = 2 * Math.pow(input.sigma, 2);
		double dist = calcDistance(input);

		if (maxdistance < (dist - sigma2)) {
			maxdistance = dist - sigma2;
		}

		ARGBDoubleType result = new ARGBDoubleType();

		result.setA(Math.pow((Math.E), (-(maxdistance / h))));

		return (O) result;
	}

	public double calcDistance(NeighborhoodPair<O> input) {

		double dresult = 0;

		Cursor<O> pCursor = input.pNeighbors.cursor();
		Cursor<O> qCursor = input.qNeighbors.cursor();

		while (pCursor.hasNext()) {
			pCursor.next();
			qCursor.next();
			// TODO
			int pValue = ((ARGBType) pCursor.get()).get();
			int qValue = ((ARGBType) qCursor.get()).get();
			dresult += Math.pow(ARGBType.blue(pValue) - ARGBType.blue(qValue),2); 
			dresult += Math.pow(ARGBType.green(pValue) - ARGBType.green(qValue),2); 
			dresult += Math.pow(ARGBType.red(pValue) - ARGBType.red(qValue),2); 

		}

		dresult = dresult / (3 * Math.pow((input.pNeighbors.dimension(0)), 2));

		return dresult;

	}

}
