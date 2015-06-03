package net.myThresh.MyThresh;

import ij.ImagePlus;
import ij.io.Opener;

import java.io.File;

import net.imagej.ImageJ;
import net.imagej.ops.AbstractStrictFunction;
import net.imagej.ops.Function;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imagej.ops.map.Map;
import net.imglib2.Cursor;
import net.imglib2.EuclideanSpace;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhood;
import net.imglib2.algorithm.neighborhood.RectangleNeighborhoodSkipCenter;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Op.class, name = "my_local_threshold")
public class MyLocalThresholdOp<T extends RealType<T>> implements Op {

	@Parameter(type = ItemIO.OUTPUT)
	private Img<BitType> res;
	
	@Parameter
	ImagePlus inputImage;
	
	@Parameter
	int span;
	
	@Parameter
	OpService ops;
	
	public void run() {
		
		Img<T> in = ImageJFunctions.wrapReal(inputImage);
		
		ImgFactory<BitType> fac = new ArrayImgFactory<BitType>();	

		RandomAccessible<T> border = Views.extendMirrorSingle(in);
		
		long[] min = new long[in.numDimensions()];
		long[] max = new long[in.numDimensions()];
		
		for (int i = 0; i < max.length; i++) {
			min[i]=0; max[i]=in.dimension(i);
		}

		RandomAccessibleInterval<T> borderCropped = Views.interval(border, min, max);

		res = fac.create(borderCropped, new BitType());
		
		Shape shape = new RectangleShape(span, true);
				
		RandomAccessibleInterval<T> borderCroppedOffset = Views.offsetInterval(border, borderCropped);
		
		Iterable<Neighborhood<T>> neighbors = shape.neighborhoodsSafe(borderCroppedOffset);
		
		Function<Iterable<T>, BitType> func = new My_Mean<Iterable<T>, BitType>();
		
		res = (Img<BitType>) ops.run(Map.class, res, neighbors, func);
		
	}
	
	
	private class My_Mean<I extends Iterable<T>,O extends BitType> extends AbstractStrictFunction<I, O> {

		public O compute(I input, O output) {
			//calc mean 
			T mean = (T) new DoubleType();
			mean = (T) ops.mean(mean, input);
			
			//Not sure if the real center is found 
			RectangleNeighborhoodSkipCenter<T> currentNeighbors = (RectangleNeighborhoodSkipCenter) input;
			
			long centerPosition = (currentNeighbors.size()-1)/2+1;
			Cursor<T> centerCursor = currentNeighbors.cursor();
			centerCursor.jumpFwd(centerPosition);
			T center = centerCursor.get();
			
			if(mean.getRealDouble()<center.getRealDouble()){
				output.set(true);
			}else{
				output.set(false);
			}
			// return result
			return output;
		}
		
	}

}
