package nonLocalMeans;

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

public class DistanceMatrixFunction<I extends Neighborhood<DoubleType>, O extends Neighborhood<DoubleType>> extends AbstractFunction<I, O> {

        private long span;
        private long research_span;

        public DistanceMatrixFunction(long span, long researchspan) {
                this.span = span;
                this.research_span = researchspan;
        }

        public O compute(I input, O output) {

                Cursor<DoubleType> ccc = input.cursor();
                while (ccc.hasNext()) {
                        ccc.next();
                        //                        if (Double.isNaN(ccc.get().get()))
                        //                                ccc.get().set(0);
                        //                        System.out.println(ccc.get().getRealDouble());
                }

                ImgFactory<DoubleType> matrixFactory = new ArrayImgFactory<DoubleType>();
                Img<DoubleType> inputImg = matrixFactory.create(new long[] {input.dimension(0), input.dimension(1)}, new DoubleType(0));
                //                System.out.println(span + " " + input.dimension(0));
                inputImg = copyFromNbh(input, inputImg);
                IntervalView<DoubleType> croppedImg = Views.interval(inputImg, new long[] {span, span}, new long[] {span + (2 * research_span),
                                span + (2 * research_span)});
                //                System.out.println(croppedImg.dimension(1) + " " + output.dimension(1));

                Img<DoubleType> distanceMatrix = matrixFactory.create(croppedImg, new DoubleType());

                Shape shape = new RectangleShape((int) span, false);
                IterableInterval<Neighborhood<DoubleType>> patches = shape.neighborhoodsSafe(croppedImg);
                IntervalView<DoubleType> centerPatch = Views.interval(inputImg, new long[] {research_span + 1, research_span + 1}, new long[] {
                                research_span + (2 * span + 1), research_span + (2 * span + 1)});
                Cursor<Neighborhood<DoubleType>> patchesCursor = patches.cursor();
                Cursor<DoubleType> distanceCursor = output.cursor();

                while (distanceCursor.hasNext()) {
                        Neighborhood<DoubleType> current = patchesCursor.next();

                        //                        Cursor<DoubleType> cc = current.cursor();
                        //                        while (cc.hasNext()) {
                        //                                System.out.println(cc.next().get());
                        //                        }

                        DoubleType d = distanceCursor.next();
                        d.set(calcDist(current, centerPatch));
                }

                //                Cursor<DoubleType> centerCursor = centerPatch.cursor();
                //                while (centerCursor.hasNext()) {
                //                        System.out.print(centerCursor.next().get() + " ");
                //                }
                //                System.out.println();
                return (O) output;
        }

        public double calcDist(Neighborhood<DoubleType> q, IntervalView<DoubleType> p) {
                Cursor<DoubleType> pCursor = p.cursor();
                Cursor<DoubleType> qCursor = q.cursor();
                double result = 0;
                while (pCursor.hasNext()) {
                        double pValue = pCursor.next().getRealDouble();
                        double qValue = qCursor.next().getRealDouble();
                        //                        System.out.println(pValue + " " + qValue);
                        result += ((pValue - qValue) * (pValue - qValue));
                }
                return result;
        }

        public Img<DoubleType> copyFromNbh(Neighborhood<DoubleType> in, Img<DoubleType> out) {

                Cursor<DoubleType> inCursor = in.cursor();
                Cursor<DoubleType> outCursor = out.cursor();

                if (in.size() != out.size()) {
                        return null;
                }

                while (inCursor.hasNext()) {
                        //                        inCursor.next().get();

                        outCursor.next().set(inCursor.next());
                        //                        System.out.println(inCursor.get().getRealDouble());
                }

                return out;
        }

}
