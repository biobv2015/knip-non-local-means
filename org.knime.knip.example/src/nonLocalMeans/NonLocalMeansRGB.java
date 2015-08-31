package nonLocalMeans;

import java.util.ArrayList;
import java.util.Iterator;

import net.imagej.ops.Contingent;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(menu = {@Menu(label = "DeveloperPlugins"), @Menu(label = "Non Local Means Color")}, headless = true, type = Command.class)
public class NonLocalMeansRGB<T extends RealType<T>> implements Command, Contingent {

        @Parameter(type = ItemIO.OUTPUT)
        private Img<DoubleType> outputImage;

        @Parameter(type = ItemIO.INPUT, label = "Image")
        private Img<T> inputImage;

        @Parameter(type = ItemIO.INPUT, label = "sigma", description = "Denoising parameter to be chosen based on the noise distribution.")
        private double sigma = 1;

        @Parameter(type = ItemIO.INPUT)
        private OpService ops;

        private long span;
        private long research_span;

        @Override
        public void run() {
                RandomAccessible<T> extendedImage = Views.extendBorder(inputImage);
                IntervalView<T> extendedImageCropped = Views.interval(extendedImage, inputImage);

                ImgFactory<DoubleType> f3 = new ArrayImgFactory<DoubleType>();

                //set span/researchspan in dependency of sigma
                if (sigma <= 25) {
                        span = 1;
                        research_span = 10;
                } else if (sigma > 25 && sigma <= 55) {
                        span = 2;
                        research_span = 17;
                } else if (sigma > 55) {
                        span = 3;
                        research_span = 17;
                }

                //seperate different color channels from input image
                long[] min = {0, 0, 0};
                long[] max = {inputImage.dimension(0) - 1, inputImage.dimension(0) - 2, 0};
                IntervalView<T> channelRView = Views.interval(extendedImageCropped, min, max);
                min[2] = 1;
                max[2] = 1;
                IntervalView<T> channelGView = Views.interval(extendedImageCropped, min, max);
                min[2] = 2;
                max[2] = 2;
                IntervalView<T> channelBView = Views.interval(extendedImageCropped, min, max);

                long[] dim = {inputImage.dimension(0), inputImage.dimension(1)};

                DoubleType dt = new DoubleType(0);

                Img<DoubleType> channelR = f3.create(dim, dt);
                Img<DoubleType> channelG = f3.create(dim, dt);
                Img<DoubleType> channelB = f3.create(dim, dt);

                Cursor<T> rViewCursor = channelRView.cursor();
                Cursor<T> gViewCursor = channelGView.cursor();
                Cursor<T> bViewCursor = channelBView.cursor();

                Cursor<DoubleType> rImgCursor = channelR.cursor();
                Cursor<DoubleType> gImgCursor = channelG.cursor();
                Cursor<DoubleType> bImgCursor = channelB.cursor();

                while (rViewCursor.hasNext()) {
                        rImgCursor.next().setReal(rViewCursor.next().getRealDouble());
                        gImgCursor.next().setReal(gViewCursor.next().getRealDouble());
                        bImgCursor.next().setReal(bViewCursor.next().getRealDouble());
                }
                RandomAccessible<DoubleType> channelRextended = Views.extendBorder(channelR);
                IntervalView<DoubleType> channelRcropped = Views.interval(channelRextended, channelR);
                RandomAccessible<DoubleType> channelGextended = Views.extendBorder(channelG);
                IntervalView<DoubleType> channelGcropped = Views.interval(channelGextended, channelG);
                RandomAccessible<DoubleType> channelBextended = Views.extendBorder(channelB);
                IntervalView<DoubleType> channelBcropped = Views.interval(channelBextended, channelB);

                //create research neighborhoods 
                Shape shape = new RectangleShape((int) (span + research_span), false);

                IterableInterval<Neighborhood<DoubleType>> rNeighbors = shape.neighborhoodsSafe(channelRcropped);

                IterableInterval<Neighborhood<DoubleType>> gNeighbors = shape.neighborhoodsSafe(channelGcropped);

                IterableInterval<Neighborhood<DoubleType>> bNeighbors = shape.neighborhoodsSafe(channelBcropped);

                RGBBundle<Neighborhood<DoubleType>, ArrayList<Double>> bun = new RGBBundle<Neighborhood<DoubleType>, ArrayList<Double>>(span,
                                research_span);

                Cursor<Neighborhood<DoubleType>> rNCursor = rNeighbors.cursor();
                Cursor<Neighborhood<DoubleType>> gNCursor = gNeighbors.cursor();
                Cursor<Neighborhood<DoubleType>> bNCursor = bNeighbors.cursor();

                ArrayList<ArrayList<Double>> distancesR = new ArrayList<ArrayList<Double>>();
                ArrayList<ArrayList<Double>> distancesG = new ArrayList<ArrayList<Double>>();
                ArrayList<ArrayList<Double>> distancesB = new ArrayList<ArrayList<Double>>();

                while (rNCursor.hasNext()) {
                        ArrayList<Double> currentR = new ArrayList<Double>();
                        ArrayList<Double> currentG = new ArrayList<Double>();
                        ArrayList<Double> currentB = new ArrayList<Double>();
                        distancesR.add(bun.test(rNCursor.next(), currentR));
                        distancesG.add(bun.test(gNCursor.next(), currentG));
                        distancesB.add(bun.test(bNCursor.next(), currentB));

                }

                //calc finished distances
                ArrayList<ArrayList<Double>> allDistances = finalizeDistances(distancesR, distancesG, distancesB);

                //calc weights
                ArrayList<ArrayList<Double>> weights = calcWeights(allDistances);

                Cursor<DoubleType> rC = channelR.cursor();
                Cursor<DoubleType> gC = channelG.cursor();
                Cursor<DoubleType> bC = channelB.cursor();

                Shape shapeResearch = new RectangleShape((int) research_span, false);
                IterableInterval<Neighborhood<DoubleType>> resPatchesR = shapeResearch.neighborhoodsSafe(channelRcropped);
                IterableInterval<Neighborhood<DoubleType>> resPatchesG = shapeResearch.neighborhoodsSafe(channelGcropped);
                IterableInterval<Neighborhood<DoubleType>> resPatchesB = shapeResearch.neighborhoodsSafe(channelBcropped);

                Cursor<Neighborhood<DoubleType>> rNc = resPatchesR.cursor();
                Cursor<Neighborhood<DoubleType>> gNc = resPatchesG.cursor();
                Cursor<Neighborhood<DoubleType>> bNc = resPatchesB.cursor();

                Iterator<ArrayList<Double>> weightListIterator = weights.iterator();

                //apply weights

                while (rC.hasNext()) {
                        rC.next();
                        gC.next();
                        bC.next();

                        Neighborhood<DoubleType> nbhR = rNc.next();
                        Neighborhood<DoubleType> nbhG = gNc.next();
                        Neighborhood<DoubleType> nbhB = bNc.next();

                        ArrayList<Double> nbhWeights = weightListIterator.next();

                        double weightSum = 0;
                        double weightedSummedValueR = 0;
                        double weightedSummedValueG = 0;
                        double weightedSummedValueB = 0;

                        Iterator<Double> currentWeightIterator = nbhWeights.iterator();

                        Cursor<DoubleType> qCursorR = nbhR.cursor();
                        Cursor<DoubleType> qCursorG = nbhG.cursor();
                        Cursor<DoubleType> qCursorB = nbhB.cursor();

                        while (qCursorR.hasNext()) {
                                double currentWeight = currentWeightIterator.next();

                                double qValueR = qCursorR.next().getRealDouble();
                                double qValueG = qCursorG.next().getRealDouble();
                                double qValueB = qCursorB.next().getRealDouble();

                                weightSum += currentWeight;

                                weightedSummedValueR = qValueR * currentWeight;
                                weightedSummedValueG = qValueG * currentWeight;
                                weightedSummedValueB = qValueB * currentWeight;
                        }
                        double weightedValueR = weightedSummedValueR / weightSum;
                        double weightedValueG = weightedSummedValueG / weightSum;
                        double weightedValueB = weightedSummedValueB / weightSum;

                        rC.get().setReal(weightedValueR);
                        gC.get().setReal(weightedValueG);
                        bC.get().setReal(weightedValueB);

                }

                //create finished output image from combined resulting values
                //TODO
        }

        public ArrayList<ArrayList<Double>> finalizeDistances(ArrayList<ArrayList<Double>> inDist0, ArrayList<ArrayList<Double>> inDist1,
                        ArrayList<ArrayList<Double>> inDist2) {
                Iterator<ArrayList<Double>> outCursor = inDist0.iterator();
                Iterator<ArrayList<Double>> in1Cursor = inDist1.iterator();
                Iterator<ArrayList<Double>> in2Cursor = inDist2.iterator();

                ArrayList<ArrayList<Double>> resultList = new ArrayList<ArrayList<Double>>();

                while (outCursor.hasNext()) {

                        ArrayList<Double> outList = new ArrayList<Double>();

                        ArrayList<Double> outNeighborhood = outCursor.next();
                        ArrayList<Double> in1Neighborhood = in1Cursor.next();
                        ArrayList<Double> in2Neighborhood = in2Cursor.next();

                        Iterator<Double> outNbhCursor = outNeighborhood.iterator();
                        Iterator<Double> in1NbhCursor = in1Neighborhood.iterator();
                        Iterator<Double> in2NbhCursor = in2Neighborhood.iterator();

                        while (outNbhCursor.hasNext()) {
                                double value1 = outNbhCursor.next();
                                double value2 = in1NbhCursor.next();
                                double value3 = in2NbhCursor.next();

                                double result = (value3 + value2 + value1) / (3 * (2 * research_span + 1));
                                outList.add(new Double(result));
                        }
                        resultList.add(outList);

                }

                return resultList;
        }

        public ArrayList<ArrayList<Double>> calcWeights(ArrayList<ArrayList<Double>> distances) {
                double h = 0.4;
                if (sigma <= 25) {
                        h = 0.55 * sigma;
                } else if (sigma > 25 && sigma <= 55) {
                        h = 0.40 * sigma;
                } else if (sigma > 55) {
                        h = 0.5 * sigma;
                }

                ArrayList<ArrayList<Double>> weights = new ArrayList<ArrayList<Double>>();
                Iterator<ArrayList<Double>> researchDistances = distances.iterator();
                while (researchDistances.hasNext()) {
                        ArrayList<Double> currentList = researchDistances.next();
                        ArrayList<Double> currentWeightList = new ArrayList<Double>();

                        Iterator<Double> dist = currentList.iterator();
                        while (dist.hasNext()) {
                                double d = dist.next();
                                double maxdistance = 0;

                                double sigma2 = 2 * (sigma * sigma);

                                if (maxdistance < (-sigma2)) {
                                        maxdistance = d - sigma2;
                                }

                                double weight = (Math.pow(Math.E, (-(maxdistance / (h * h)))));
                                currentWeightList.add(new Double(weight));
                        }
                        weights.add(currentWeightList);
                }

                return weights;
        }

        @Override
        public boolean conforms() {
                if (inputImage.numDimensions() > 2) {
                        return true;
                } else {
                        return false;
                }
        }

}
