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
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

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

		res = fac.create(in, new BitType());
		
		Shape shape = new RectangleShape(span, true);
		
		Iterable<Neighborhood<T>> neighbors = shape.neighborhoodsSafe(in);
		
		Function<Iterable<T>, BitType> func = new My_Mean<Iterable<T>, BitType>();
		
		res = (Img<BitType>) ops.run(Map.class, res, neighbors, func);
		
	}
	
	
	private class My_Mean<I extends Iterable<T>,O extends BitType> extends AbstractStrictFunction<I, O> {

		public O compute(I input, O output) {
			//calc mean 
			T mean = (T) new DoubleType();
			mean = (T) ops.mean(mean, input);
			
			// loop implementation of mean for testing purposes
//			Cursor<T> cur = (Cursor<T>) input.iterator();
//			double sum = 0;
//			int count = 0;
//			while (cur.hasNext()) {
//				T t = (T) cur.next();
//				count++;
//				sum += t.getRealDouble();
//			}
//			double value = sum/count;
//			mean.setReal(value);
			
			//compare to center (atm fixed threshold, needs rework of neighborhood)
			if(mean.getRealDouble()>100){
				output.set(true);
			}else{
				output.set(false);
			}
			// return result
			return output;
		}
		
	}
	public static void main(final String... args) throws Exception {
		final ImageJ ij = new ImageJ();

		//Open an image to work with in imagej
		File file = new File( "C:/Users/fv/Desktop/test/blobs.tif" );
		ImagePlus imp =  new Opener().openImage( file.getAbsolutePath() );

        ij.ui().showUI();
        
		// Run our op
		final Object threshimg = ij.op().run("my_local_threshold", imp, 2);

		// And display the result!
		ij.ui().show(threshimg);
	}

}
