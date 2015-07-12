package nonLocalMeans;

import java.util.ArrayList;

import ij.ImagePlus;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Contingent;
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

@Plugin(type = Op.class, name = "non_local_means_gs_pw")
public class NonLocalMeansGSPatchwise<T extends RealType<T>> implements Op, Contingent{


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

	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Img<T> in = ImageJFunctions.wrapReal(inputImage);
		
		RandomAccessible<T> border = Views.extendZero(in);

		int researchspan = span>2?17:10;
		
		long[] min = new long[in.numDimensions()];
		long[] max = new long[in.numDimensions()];

		for (int i = 0; i < max.length; i++) {
			min[i] = 0 ;
			max[i] = in.dimension(i);
		}

		ImgFactory<T> fac = in.factory();

		outputImage = fac.create(in, in.firstElement());

		RandomAccessibleInterval<T> borderCropped = Views.interval(border, min,
				max);
		

		Shape shape = new RectangleShape((int) span, true);

		RandomAccessibleInterval<T> borderCroppedOffset = Views.offsetInterval(
				border, borderCropped);

		Cursor<T> pCursor = in.cursor();
		Cursor<T> outCursor = outputImage.cursor();
		
		while (pCursor.hasNext()) {
			//calculate research window
			pCursor.next();
			int xPos = pCursor.getIntPosition(0);
			int yPos = pCursor.getIntPosition(1);

			int pxmin = (int) (xPos-researchspan);
			int pymin = (int) (yPos-researchspan);
			int pxmax = (int) (xPos+researchspan);
			int pymax = (int) (yPos+researchspan);
			
			long[] pmax = { pxmax, pymax };
			long[] pmin = { pxmin, pymin };

			IntervalView<T> pNeighbors = Views.interval(borderCroppedOffset,
					pmin, pmax);
		
			IterableInterval<Neighborhood<T>> qneighbors = shape
					.neighborhoodsSafe(pNeighbors);
			
			//comparison window around p
			long[] pmax2 = { xPos+span, yPos+span };
			long[] pmin2 = { xPos-span, yPos-span };
			IntervalView<T> compareP = Views.interval(borderCroppedOffset, pmin2, pmax2);
			
			Cursor<Neighborhood<T>> qCursor = qneighbors.cursor();

			// create all neighborhoodpairs for map op
			ArrayList<NeighborhoodPair<T>> nbhPairs = new ArrayList<NeighborhoodPair<T>>();

			Img<T> copy = fac.create(compareP, compareP.firstElement());
			
			Cursor<T> pNCursor = compareP.cursor();
			Cursor<T> copyCursor = copy.cursor();
			
			while (pNCursor.hasNext()) {
				pNCursor.next();
				
				copyCursor.next();
				qCursor.next();
				copyCursor.get().set(pNCursor.get());

				NeighborhoodPair<T> toAdd = new NeighborhoodPair<T>();
				toAdd.pNeighbors = copy;				
				toAdd.qNeighbors = qCursor.get();
				toAdd.sigma=sigma;
//				toAdd.span=researchspan;
				nbhPairs.add(toAdd);
			}
			

			Function<NeighborhoodPair<T>, T> weightfunc = new WeightingFunction<NeighborhoodPair<T>, T>();

			Img<T> weights = (Img<T>) ops.map(copy, nbhPairs, weightfunc);
			

			T nbhsum = (T) new DoubleType();
			nbhsum.setZero();
		
			nbhsum = (T) ops.sum(nbhsum, weights);
			
			double res_ = 0;
			Cursor<T> resCursor = weights.cursor();
			Cursor<T> qCursor2 = compareP.cursor();
			while (resCursor.hasNext()) {
				T qweight = resCursor.next();
				T q = qCursor2.next();
				res_ += q.getRealDouble() * qweight.getRealDouble();
			}
			
			Cursor<T> compCursor = compareP.cursor();
			while(compCursor.hasNext()){
				compCursor.next().setReal((1 / nbhsum.getRealDouble()) * res_);
			}
		}
		
		
		Cursor<T> c = (Views.interval(borderCroppedOffset, in)).cursor();
		
		while(outCursor.hasNext()){
			
			outCursor.next();
			int xPos = outCursor.getIntPosition(0);
			int yPos = outCursor.getIntPosition(1);
			
			long[] pmax2 = { xPos+span, yPos+span };
			long[] pmin2 = { xPos-span, yPos-span };
			IntervalView<T> compareP = Views.interval(borderCroppedOffset, pmin2, pmax2);
			
			DoubleType pSum= new DoubleType();
			pSum.setZero();
			ops.sum(pSum, compareP);
			outCursor.get().setReal(pSum.getRealDouble()/((2*span+1)*(2*span+1)));
			
//			outCursor.next().set(c.next());
		}
		ImageJFunctions.show(outputImage);
	}


	@Override
	public boolean conforms() {
		// TODO Auto-generated method stub
		return false;
	}

}
