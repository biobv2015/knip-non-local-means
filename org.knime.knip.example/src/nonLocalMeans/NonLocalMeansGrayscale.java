package nonLocalMeans;

import net.imagej.ops.Contingent;
import net.imagej.ops.OpService;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.algorithm.region.localneighborhood.RectangleShape;
import net.imglib2.algorithm.region.localneighborhood.Shape;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(menu = {@Menu(label = "DeveloperPlugins"), @Menu(label = "Non Local Means")}, headless = true, type = Command.class)
public class NonLocalMeansGrayscale<T extends RealType<T>> implements Command, Contingent {

        @Parameter(type = ItemIO.OUTPUT)
        private Img<T> outputImage;

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

                //Calculate the span and research-span to be used based on sigma
                if (sigma <= 15) {
                        span = 1;
                        research_span = 10;
                } else if (sigma > 15 && sigma <= 30) {
                        span = 2;
                        research_span = 10;
                } else if (sigma > 30 && sigma <= 45) {
                        span = 3;
                        research_span = 17;
                } else if (sigma > 45 && sigma <= 75) {
                        span = 4;
                        research_span = 17;
                } else if (sigma > 75) {
                        span = 5;
                        research_span = 17;
                }
                //Expand image with border
                RandomAccessible<T> imageWithBorder = Views.extendBorder(inputImage);
                RandomAccessibleInterval<T> imageWithBorderCropped = Views.interval(imageWithBorder, inputImage);

                //Create output image
                ImgFactory<T> fac = inputImage.factory();
                Img<T> processedImage = fac.create(inputImage, inputImage.firstElement());

                //Create neighborhoods for patching function
                Shape shape = new RectangleShape((int) (research_span + span), false);
                IterableInterval<Neighborhood<T>> researchWindows = shape.neighborhoodsSafe(imageWithBorderCropped);

                //Create patching function which calculates the nlm for one pixel.
                //Preload with value for spans and factory
                PatchingFunctionGS<Neighborhood<T>, T> patchingFunc = new PatchingFunctionGS<Neighborhood<T>, T>(ops, sigma, span, research_span, fac);
                //Call patching function on the complete image
                outputImage = (Img<T>) ops.map(processedImage, researchWindows, patchingFunc);
        }

        @Override
        public boolean conforms() {
                if (inputImage.numDimensions() > 2) {
                        return false;
                } else {
                        return true;
                }
        }
}
