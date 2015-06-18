package nonLocalMeans;

import net.imagej.ops.AbstractStrictFunction;
import net.imagej.ops.Function;
import net.imglib2.Cursor;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;

public class WeightingFunction<I extends NeighborhoodPair<O>, O extends RealType<O>> extends AbstractStrictFunction<I, O>{

	public O compute(I input, O output) {
		
	
		//TODO set h depending on sigma 
		
		double maxdistance = 0;
		double h = 0.4;
		
		double sigma2 = 2*Math.pow(input.sigma, 2);
		double dist = calcDistance(input);
			
		if(maxdistance<(dist-sigma2)){
			maxdistance=dist-sigma2;
		}
		
		
		DoubleType result = new DoubleType();
		
		result.setReal(Math.pow((Math.E),(-(maxdistance/h))));
		
		return (O) result;
	}
	
	public double calcDistance(NeighborhoodPair<O> input){
		
		double dresult = 0;
		
		Cursor<O> pCursor = input.pNeighbors.cursor();
		Cursor<O> qCursor = input.qNeighbors.cursor();
		
		while(pCursor.hasNext()){
			pCursor.next(); qCursor.next();
			dresult += Math.pow(pCursor.get().getRealDouble()-qCursor.get().getRealDouble(),2);
		}
		
		dresult = dresult/Math.pow((input.pNeighbors.dimension(0)),2);
		
		return dresult;
		
	}

}
