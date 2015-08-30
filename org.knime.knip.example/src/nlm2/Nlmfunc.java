package nlm2;

import net.imagej.ops.AbstractFunction;
import net.imglib2.algorithm.region.localneighborhood.Neighborhood;
import net.imglib2.type.numeric.RealType;

public class Nlmfunc<I extends Neighborhood<O>, O extends RealType<O>> extends AbstractFunction<I, O> {

        @Override
        public O compute(I input, O output) {

                output.setReal(100);
                return output;
        }

}
