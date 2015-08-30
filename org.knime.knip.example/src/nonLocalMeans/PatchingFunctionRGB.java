package nonLocalMeans;

import net.imagej.ops.AbstractFunction;
import net.imagej.ops.OpService;
import net.imglib2.img.ImgFactory;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;

public class PatchingFunctionRGB<I, O> extends AbstractFunction<I, O> {

        @Parameter(type = ItemIO.INPUT)
        private OpService ops;

        @Parameter(type = ItemIO.INPUT)
        private double sigma;

        private long span;

        private long research_span;

        @Parameter(type = ItemIO.INPUT)
        private ImgFactory<O> fac;

        public PatchingFunctionRGB(OpService ops, double sigma, long span, long researchspan, ImgFactory<O> fac) {
                this.ops = ops;
                this.sigma = sigma;
                this.span = span;
                this.research_span = researchspan;
                this.fac = fac;
        }

        @Override
        public O compute(I arg0, O arg1) {
                System.out.println(1);
                return null;
        }
}
