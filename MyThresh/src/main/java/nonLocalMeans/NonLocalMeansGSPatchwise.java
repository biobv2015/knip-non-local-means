package nonLocalMeans;

import ij.ImagePlus;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

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
import net.imglib2.view.Views;

@Plugin(type = Op.class, name = "non_local_means_gs_pw")
public class NonLocalMeansGSPatchwise<T extends RealType<T>> implements Op{


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
			min[i] = 0 - (researchspan+span);
			max[i] = in.dimension(i) + researchspan+span;
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

		//TODO: check if necessary
		IterableInterval<Neighborhood<T>> neighbors = shape
				.neighborhoodsSafe(borderCroppedOffset);

		Cursor<Neighborhood<T>> neighborhoodCursor = neighbors.cursor();
		
		
		
	}

}
