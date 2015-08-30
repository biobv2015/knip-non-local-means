package nlm2;

import net.imagej.ops.Op;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(menu = {@Menu(label = "DeveloperPlugins"), @Menu(label = "NLM")}, headless = true, type = Command.class)
public class NlmGS<T extends RealType<T>> implements Command, Op {

        @Parameter(type = ItemIO.OUTPUT)
        private Img<T> outputImage;

        @Parameter(type = ItemIO.INPUT, label = "Image")
        private Img<T> inputImage;

        @Parameter(type = ItemIO.INPUT, label = "sigma", description = "Denoising parameter to be chosen based on the noise distribution.")
        private double sigma = 1;

        @Parameter(type = ItemIO.INPUT)
        private OpService ops;

        private long span = 20;
        private long research_span;

        @Override
        public void run() {
                long starttime = System.currentTimeMillis();

                ImgFactory<T> fac = inputImage.factory();
                Img<T> processedImage = fac.create(inputImage, inputImage.firstElement());
                processedImage = inputImage.copy();

                long[] min = {0, 0, 0};
                long[] max = {processedImage.dimension(0) - 1, processedImage.dimension(1) - 1, (long) 2};

                IntervalView<T> small = Views.interval(processedImage, min, max);

                Cursor<T> testCursor = small.localizingCursor();

                long[] dim = {processedImage.dimension(0), processedImage.dimension(1)};
                outputImage = fac.create(small, inputImage.firstElement());
                Cursor<T> out = outputImage.localizingCursor();
                testCursor.reset();
                while (out.hasNext()) {
                        out.next().set(testCursor.next());
                        System.out.println(out.getIntPosition(0));
                        out.localize(new long[] {out.getIntPosition(0) + 10, out.getIntPosition(1), out.getIntPosition(2)});
                        System.out.println(out.getIntPosition(0) + " t");

                }

                long stoptime = System.currentTimeMillis();
                System.out.println(stoptime - starttime + " ms");
        }
}
