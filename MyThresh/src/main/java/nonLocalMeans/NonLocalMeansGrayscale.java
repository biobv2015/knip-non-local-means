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
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Op.class, name = "non_local_means_gs")
public class NonLocalMeansGrayscale<T extends RealType<T>> implements Op {

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
				toAdd.sigma=sigma;
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
			while (resCursor.hasNext()) {
				T qweight = resCursor.next();
				T q = qCursor2.next();
				res_ += q.getRealDouble() * qweight.getRealDouble();
			}
			outCursor.get().setReal((1 / nbhsum.getRealDouble()) * res_);

		}
		ImageJFunctions.show(outputImage);

	}

}
