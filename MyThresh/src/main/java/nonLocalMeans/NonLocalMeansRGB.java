package nonLocalMeans;

import java.util.ArrayList;

import ij.ImagePlus;
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
import net.imglib2.type.numeric.ARGBDoubleType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Op.class, name = "non_local_means_rgb")
public class NonLocalMeansRGB<T extends NumericType<T>> implements Op {

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

		Img<T> in = (Img<T>) ImageJFunctions.wrapRGBA(inputImage);

		RandomAccessible<T> border = Views.extendZero(in);

		long[] min = new long[in.numDimensions()];
		long[] max = new long[in.numDimensions()];

		for (int i = 0; i < max.length; i++) {
			min[i] = 0 - span;
			max[i] = in.dimension(i) + span;
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

		Iterable<Neighborhood<T>> neighbors = shape
				.neighborhoodsSafe(borderCroppedOffset);

		boolean trigger = false;

		while (pCursor.hasNext()) {
			T p = pCursor.next();
			outCursor.next();
			int xPos = pCursor.getIntPosition(0);
			int yPos = pCursor.getIntPosition(1);

			int pxmin = (int) (xPos - span >= 0 ? xPos - span : 0);
			int pymin = (int) (yPos - span >= 0 ? yPos - span : 0);

			int pxmax = (int) (xPos + span <= in.dimension(0) - 1 ? xPos + span
					: in.dimension(0) - 1);
			int pymax = (int) (yPos + span <= in.dimension(1) - 1 ? yPos + span
					: in.dimension(1) - 1);

			long[] pmax = { pxmax, pymax };
			long[] pmin = { pxmin, pymin };

			IntervalView<T> pNeighbors = Views.interval(borderCroppedOffset,
					pmin, pmax);
			IterableInterval<Neighborhood<T>> qneighbors = shape
					.neighborhoodsSafe(pNeighbors);

			Cursor<Neighborhood<T>> qCursor = qneighbors.cursor();

			// create all neighborhoodpairs for map op
			ArrayList<NeighborhoodPair<T>> nbhPairs = new ArrayList<NeighborhoodPair<T>>();

			
			Img<T> copy = fac.create(pNeighbors, pNeighbors.firstElement());

			Cursor<T> pNCursor = pNeighbors.cursor();
			Cursor<T> copyCursor = copy.cursor();
			while (pNCursor.hasNext()) {
				pNCursor.next();
				copyCursor.next();
				qCursor.next();
				copyCursor.get().set(pNCursor.get());

				NeighborhoodPair<T> toAdd = new NeighborhoodPair<T>();
				toAdd.pNeighbors = copy;
				toAdd.qNeighbors = qCursor.get();

				nbhPairs.add(toAdd);
			}

			Function<NeighborhoodPair<T>, T> weightfunc = new WeightingFunctionRGB<NeighborhoodPair<T>, T>();

			Img<T> weights = (Img<T>) ops.map(copy, nbhPairs, weightfunc);

			DoubleType nbhsum = new DoubleType();
			nbhsum.set(calcsum(weights));
			

			double res_r = 0;
			double res_g = 0;
			double res_b = 0;
			Cursor<T> resCursor = weights.cursor();
			Cursor<T> qCursor2 = pNeighbors.cursor();
			while (resCursor.hasNext()) {
				T qweight = resCursor.next();
				double weight = ((ARGBType)qweight).get();
				T q = qCursor2.next();
				int qValue = ((ARGBType) q).get();
				res_r += ARGBType.red(qValue) * weight;
				res_g += ARGBType.green(qValue) * weight;
				res_b += ARGBType.blue(qValue) * weight;
			}
			res_r = res_r/ nbhsum.getRealDouble();
			res_g = res_g/ nbhsum.getRealDouble();
			res_b = res_b/ nbhsum.getRealDouble();
			outCursor.get().set((T) new ARGBType(ARGBType.rgba(res_r, res_g, res_b, 0)));

		}
		ImageJFunctions.show(outputImage);

	}
	private double calcsum(Img<T> weights){
		Cursor<T> c = weights.cursor();
		double result = 0;
		while(c.hasNext()){
			T cur = c.next();
			result += ((ARGBType) cur).get();  
		}
		return result; 
	}

}
