package nonLocalMeans;

import net.imagej.ops.AbstractFunction;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

public class PatchingFunctionGS<I extends Neighborhood<O>, O extends RealType<O>> extends AbstractFunction<I, O> {

        @Parameter(type = ItemIO.INPUT)
        private OpService ops;

        @Parameter(type = ItemIO.INPUT)
        private double sigma;

        private long span;

        private long research_span;

        @Parameter(type = ItemIO.INPUT)
        private ImgFactory<O> fac;

        public PatchingFunctionGS(OpService ops, double sigma, long span, long researchspan, ImgFactory<O> fac) {
                this.ops = ops;
                this.sigma = sigma;
                this.span = span;
                this.research_span = researchspan;
                this.fac = fac;
        }

        @Override
        public O compute(I input, O output) {
                //Create neighborhoods
                Shape shape = new RectangleShape((int) span, false);

                //therefore create a Img from the input neighborhood
                long[] min;
                long[] max;
                min = new long[2];
                min[0] = span;
                min[1] = span;
                max = new long[2];
                max[0] = 2 * research_span + 1;
                max[1] = 2 * research_span + 1;
                Img<O> researchPatch = fac.create(input, input.firstElement());
                //fill research patch img
                Cursor<O> inputCursor = input.cursor();
                Cursor<O> researchPatchCursor = researchPatch.cursor();
                while (inputCursor.hasNext()) {
                        researchPatchCursor.next().set(inputCursor.next());
                }
                RandomAccessible<O> researchPatchExt = Views.extendBorder(researchPatch);
                RandomAccessibleInterval<O> research = Views.offsetInterval(researchPatchExt, min, max);
                IntervalView<O> researchPatchCropped = Views.offsetInterval(researchPatch, min, max);
                IterableInterval<Neighborhood<O>> patches = shape.neighborhoodsSafe(research);
                //Create weight matrix as copy of the cropped research patch
                long[] dims = {researchPatchCropped.dimension(0), researchPatchCropped.dimension(1)};
                Img<O> weightMatrix = fac.create(researchPatchCropped, input.firstElement());
                //create denoise patch
                long[] minDenoise;
                long[] maxDenoise;
                minDenoise = new long[2];
                maxDenoise = new long[2];
                minDenoise[0] = research_span;
                minDenoise[1] = research_span;
                maxDenoise[0] = researchPatch.dimension(1) - research_span;
                maxDenoise[1] = researchPatch.dimension(0) - research_span;
                IntervalView<O> denoiseView = Views.interval(researchPatch, minDenoise, maxDenoise);
                Img<O> denoisePatch = fac.create(denoiseView, input.firstElement());

                Cursor<O> denoiseCursor = denoisePatch.cursor();
                Cursor<O> croppedCursor = denoiseView.cursor();
                while (denoiseCursor.hasNext()) {
                        denoiseCursor.next().set(croppedCursor.next());
                }
                Cursor<Neighborhood<O>> test = patches.cursor();
                Cursor<O> weightCursor = weightMatrix.cursor();
                while (test.hasNext()) {
                        //TODO

                        Neighborhood<O> patch = test.next();
                        O weight = weightCursor.next();
                        weight.setReal(calcWeight(patch, denoisePatch));
                }
                //Calculate resulting denoised value
                // sum(weights * researchPatch)
                DoubleType normalizeFactor = new DoubleType();
                normalizeFactor.setZero();
                ops.sum(normalizeFactor, weightMatrix);

                double result = 0;
                Cursor<O> inCursor = researchPatchCropped.cursor();
                Cursor<O> outCursor = weightMatrix.cursor();
                while (inCursor.hasNext()) {
                        inCursor.next();
                        outCursor.next();
                        result += outCursor.get().getRealDouble() * inCursor.get().getRealDouble();
                }
                output.setReal(result / normalizeFactor.get());
                return (O) output;
        }

        private double calcWeight(Neighborhood<O> comp, Img<O> patch) {
                double h = 0.4;
                if (sigma <= 30) {
                        h = 0.4 * sigma;
                } else if (sigma > 30 && sigma <= 75) {
                        h = 0.35 * sigma;
                } else if (sigma > 75) {
                        h = 0.3 * sigma;
                }

                double maxdistance = 0;

                double sigma2 = 2 * (sigma * sigma);

                double distance = calcDistance(comp, patch);

                if (maxdistance < (distance - sigma2)) {
                        maxdistance = distance - sigma2;
                }

                return (Math.pow(Math.E, (-(maxdistance / (h * h)))));
        }

        private double calcDistance(IterableInterval<O> in, IterableInterval<O> patch) {

                double distance = 0;
                Cursor<O> qCursor = patch.cursor();
                Cursor<O> pCursor = in.cursor();

                while (pCursor.hasNext()) {
                        pCursor.next();
                        qCursor.next();
                        distance += (pCursor.get().getRealDouble() - qCursor.get().getRealDouble())
                                        * (pCursor.get().getRealDouble() - qCursor.get().getRealDouble());
                }
                double result = distance / (in.dimension(0) * in.dimension(0));
                return result;
        }
}
