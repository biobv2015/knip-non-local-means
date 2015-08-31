package nonLocalMeans;

import java.util.ArrayList;

import net.imagej.ops.AbstractFunction;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class RGBBundle<I extends Neighborhood<DoubleType>, O extends ArrayList<Double>> extends AbstractFunction<I, O> {

        private long span, research_span;

        public RGBBundle(long span, long research_span) {
                this.span = span;
                this.research_span = research_span;
        }

        public O test(I in, O out) {
                Cursor<DoubleType> inc = in.cursor();

                //make img
                ImgFactory<DoubleType> fac = new ArrayImgFactory<DoubleType>();
                Img<DoubleType> inputImg = fac.create(in, new DoubleType());
                Cursor<DoubleType> inCursor = in.cursor();
                Cursor<DoubleType> imgCursor = inputImg.cursor();
                while (inCursor.hasNext()) {
                        imgCursor.next().set(new DoubleType(inCursor.next().getRealDouble()));
                }
                //crop span off img
                IntervalView<DoubleType> researchWindow = Views.offsetInterval(inputImg, new long[] {span, span}, new long[] {
                                1 + (2 * research_span), 1 + (2 * research_span)});
                IntervalView<DoubleType> centerPatch = Views.interval(inputImg, new long[] {research_span, research_span}, new long[] {
                                research_span + (2 * span), research_span + (2 * span)});

                Shape shape = new RectangleShape((int) span, false);
                IterableInterval<Neighborhood<DoubleType>> patches = shape.neighborhoodsSafe(researchWindow);

                Cursor<Neighborhood<DoubleType>> patchCursor = patches.cursor();

                while (patchCursor.hasNext()) {
                        double distance = patchDistance(patchCursor.next(), centerPatch);
                        out.add(new Double(distance));
                }

                return out;
        }

        private double patchDistance(Neighborhood<DoubleType> patch, IntervalView<DoubleType> center) {
                Cursor<DoubleType> centerCursor = center.cursor();
                Cursor<DoubleType> patchCursor = patch.cursor();
                double result = 0;
                while (centerCursor.hasNext()) {
                        double c = centerCursor.next().getRealDouble();
                        double p = patchCursor.next().getRealDouble();
                        result += ((c - p) * (c - p));
                }

                return result;
        }

        @Override
        public O compute(I arg0, O arg1) {
                return test(arg0, arg1);
        }

}
