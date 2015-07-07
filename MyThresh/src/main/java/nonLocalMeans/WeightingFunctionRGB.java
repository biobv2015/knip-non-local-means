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
		double h=0.4;
		if(input.sigma<=25){
			h=0.55*input.sigma;
		}else if(input.sigma>25&&input.sigma<=55){
			h=0.4*input.sigma;
		}else if(input.sigma>55){
			h=0.35*input.sigma;
		}

		double sigma2 = 2 *input.sigma*input.sigma;
		double dist = calcDistance(input);

		
		if (maxdistance < (dist - sigma2)) {
			maxdistance = dist - sigma2;
		}

		ARGBDoubleType result = new ARGBDoubleType();

		result.setA(Math.pow((Math.E), (-(maxdistance / h*h))));

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
			dresult += (ARGBType.blue(pValue) - ARGBType.blue(qValue))*(ARGBType.blue(pValue) - ARGBType.blue(qValue)); 
			dresult += (ARGBType.green(pValue) - ARGBType.green(qValue))*(ARGBType.green(pValue) - ARGBType.green(qValue)); 
			dresult += (ARGBType.red(pValue) - ARGBType.red(qValue))*(ARGBType.red(pValue) - ARGBType.red(qValue)); 

		}

		dresult = dresult / 3*input.pNeighbors.dimension(0)*input.pNeighbors.dimension(0);

		return dresult;

	}

}
