package nonLocalMeans;

import ij.ImagePlus;
import ij.io.Opener;

import java.io.File;
import java.util.ArrayList;

import net.imagej.ImageJ;
import net.imagej.ops.Function;
import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Op.class, name = "non_local_mean")
public class NonLocalMeans<T extends NumericType<T>> implements Op{

	@Parameter(type = ItemIO.OUTPUT)
	private Img<T> outputImage;
	
	@Parameter
	private ImagePlus inputImage;
	
	@Parameter
	private double sigma;
	
	@Parameter
	private OpService ops;
	
	@Parameter
	private long span;
	
	public void run() {
		
		if(inputImage.getBitDepth()==24){
			outputImage = (Img<T>) ops.run("non_local_means_rgb", inputImage, sigma, span);	
		}else{
			outputImage = (Img<T>) ops.run("non_local_means_gs", inputImage, sigma, span);
		}
		
	}
	
	public static void main(final String... args) throws Exception {
		long startTime = System.currentTimeMillis();
		final ImageJ ij = new ImageJ();

		//Open an image to work with in imagej
		File file = new File( "C:/Users/fv/Desktop/test/test.tif" );
		ImagePlus imp =  new Opener().openImage( file.getAbsolutePath() );
		
        //ij.ui().showUI();
		Img in;
		if(imp.getBitDepth()==24){
			in = ImageJFunctions.wrapRGBA(imp);
		}else{
			in = ImageJFunctions.wrapReal(imp);
		}
        
		// Run our op
		final Object threshimg = ij.op().run("nlm", in, 15, 5);
		long stopTime = System.currentTimeMillis();
		System.out.println((stopTime-startTime) + " ms overall");
		// And display the result!
		//ij.ui().show(threshimg);
	}

}
