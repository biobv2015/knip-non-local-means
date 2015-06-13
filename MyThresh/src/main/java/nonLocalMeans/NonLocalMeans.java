package nonLocalMeans;

import ij.ImagePlus;

import java.util.ArrayList;

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
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
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
			min[i]=0; max[i]=in.dimension(i);
		}

		RandomAccessibleInterval<T> borderCropped = Views.interval(border, min, max);
		
		Shape shape = new RectangleShape((int) span, true);
		
		RandomAccessibleInterval<T> borderCroppedOffset = Views.offsetInterval(border, borderCropped);
		
		Cursor<T> pCursor = in.cursor();
		
		Iterable<Neighborhood<T>> neighbors = shape.neighborhoodsSafe(borderCroppedOffset);
		
		for(int i = 0; i<=in.dimension(0);i++){
			for(int j = 0;j<=in.dimension(1);j++){
				T p = pCursor.next();
				int xPos = pCursor.getIntPosition(0);
				int yPos = pCursor.getIntPosition(1);
				
				int pxmin = (int) (xPos-span>=0?xPos-span:0);
				int pymin = (int) (yPos-span>=0?yPos-span:0);
				
				int pxmax = (int) (xPos+span<=in.dimension(0)?xPos+span:in.dimension(0));
				int pymax = (int) (yPos+span<=in.dimension(1)?yPos+span:in.dimension(1));
				
				long[] pmax = {pxmax, pymax};
				long[] pmin = {pxmin, pymin};
				
				IntervalView<T> pNeighbors = Views.interval(borderCroppedOffset, pmin, pmax); 
				IterableInterval<Neighborhood<T>> qneighbors = shape.neighborhoodsSafe(pNeighbors);
				
				Cursor<Neighborhood<T>> qCursor = qneighbors.cursor();
				
				//create all neighborhoodpairs for map op
				ArrayList<NeighborhoodPair<T>> nbhPairs = new ArrayList<NeighborhoodPair<T>>();
				
				Neighborhood<T> copy = (Neighborhood<T>) ops.createimg(pNeighbors);
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
				
				Function<NeighborhoodPair<T>, Neighborhood<T>> weightfunc = new WeightingFunction<NeighborhoodPair<T>, Neighborhood<T>, T>(); 
			
				
				
			}
			
		}
		 //iterate over all pixels
		
		//iterate over the neighbors of one pixels
		
		//calculate distance and weight per pixel/neighborhood
		
		//evaluate u_hat and draw resulting pixel -> next pixel
		
	}
	
	//main method for testing

}
