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
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Op.class, name = "non_local_means")
public class NonLocalMeans<T extends RealType<T>> implements Op{

	@Parameter(type = ItemIO.OUTPUT)
	private Img<T> outputImage;
	
	@Parameter
	ImagePlus inputImage;
	
	@Parameter
	double sigma;
	
	@Parameter
	OpService ops;
	
	@Parameter
	long span;
	
	public void run() {
		// TODO Auto-generated method stub
		
		Img<T> in = ImageJFunctions.wrapReal(inputImage);
		
		RandomAccessible<T> border = Views.extendZero(in);
		
		long[] min = new long[in.numDimensions()];
		long[] max = new long[in.numDimensions()];
		
		for (int i = 0; i < max.length; i++) {
			min[i]=0-span; max[i]=in.dimension(i)+span;
		}

		ImgFactory<T> fac = in.factory();
		
		outputImage = fac.create(in, in.firstElement());
		
		RandomAccessibleInterval<T> borderCropped = Views.interval(border, min, max);
		
		Shape shape = new RectangleShape((int) span, true);
		
		RandomAccessibleInterval<T> borderCroppedOffset = Views.offsetInterval(border, borderCropped);
		
		Cursor<T> pCursor = in.cursor();
		Cursor<T> outCursor = outputImage.cursor();
		
		Iterable<Neighborhood<T>> neighbors = shape.neighborhoodsSafe(borderCroppedOffset);
		
		boolean trigger = false;
		
		while(pCursor.hasNext()){
		//for(int i = 0; i<=in.dimension(0);i++){
			//for(int j = 0;j<=in.dimension(1);j++){
				T p = pCursor.next();
				outCursor.next();
				int xPos = pCursor.getIntPosition(0);
				int yPos = pCursor.getIntPosition(1);
				
				int pxmin = (int) (xPos-span>=0?xPos-span:0);
				int pymin = (int) (yPos-span>=0?yPos-span:0);
				
				int pxmax = (int) (xPos+span<=in.dimension(0)-1?xPos+span:in.dimension(0)-1);
				int pymax = (int) (yPos+span<=in.dimension(1)-1?yPos+span:in.dimension(1)-1);
				
				long[] pmax = {pxmax, pymax};
				long[] pmin = {pxmin, pymin};
				
				IntervalView<T> pNeighbors = Views.interval(borderCroppedOffset, pmin, pmax); 
				IterableInterval<Neighborhood<T>> qneighbors = shape.neighborhoodsSafe(pNeighbors);
				
				Cursor<Neighborhood<T>> qCursor = qneighbors.cursor();
				
				//create all neighborhoodpairs for map op
				ArrayList<NeighborhoodPair<T>> nbhPairs = new ArrayList<NeighborhoodPair<T>>();
				
				Img<T> copy = fac.create(pNeighbors, pNeighbors.firstElement());
								
				Cursor<T> pNCursor = pNeighbors.cursor();
				Cursor<T> copyCursor = copy.cursor();
				while (pNCursor.hasNext()) {
					pNCursor.next(); copyCursor.next(); qCursor.next();
					copyCursor.get().set(pNCursor.get());
					
					NeighborhoodPair<T> toAdd = new NeighborhoodPair<T>();
					toAdd.pNeighbors = copy;
					toAdd.qNeighbors = qCursor.get();
					
					nbhPairs.add(toAdd);
				}
								
				Function<NeighborhoodPair<T>, T> weightfunc = new WeightingFunction<NeighborhoodPair<T>, T>(); 
			
				Img<T> weights = (Img<T>) ops.map(copy, nbhPairs, weightfunc);
				
				T nbhsum = (T) new DoubleType();
				nbhsum.setZero();
				nbhsum = (T) ops.sum(nbhsum, weights);
				
				double res_ = 0;
				Cursor<T> resCursor = weights.cursor();
				Cursor<T> qCursor2 = pNeighbors.cursor();
				while(resCursor.hasNext()){
					T qweight = resCursor.next();
					T q = qCursor2.next();
					res_ += q.getRealDouble()*qweight.getRealDouble();
				}
				outCursor.get().setReal((1/nbhsum.getRealDouble())*res_);
						
	//		}
			
		}
		 //iterate over all pixels
		
		//iterate over the neighbors of one pixels
		
		//calculate distance and weight per pixel/neighborhood
		
		//evaluate u_hat and draw resulting pixel -> next pixel
		
	}
	
	public static void main(final String... args) throws Exception {
		final ImageJ ij = new ImageJ();

		//Open an image to work with in imagej
		File file = new File( "C:/Users/fv/Desktop/test/blobs.tif" );
		ImagePlus imp =  new Opener().openImage( file.getAbsolutePath() );

        ij.ui().showUI();
        
		// Run our op
		final Object threshimg = ij.op().run("non_local_means", imp, 1, 3);

		// And display the result!
		ij.ui().show(threshimg);
	}

}
