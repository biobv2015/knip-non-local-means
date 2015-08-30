package nlm2;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(menu = {@Menu(label = "DeveloperPlugins"), @Menu(label = "TEST")}, headless = true, type = Command.class)
public class Test<T extends RealType<T>> implements Command {

        @Parameter(type = ItemIO.OUTPUT)
        private Img<T> outputImage;

        @Parameter(type = ItemIO.INPUT, label = "Image")
        private Img<T> inputImage;

        @Override
        public void run() {
                final ImgFactory<FloatType> imgFactory = new CellImgFactory<FloatType>(5);

                // create an 3d-Img with dimensions 20x30x40 (here cellsize is 5x5x5)Ø
                final Img<FloatType> img1 = imgFactory.create(new long[] {20, 10, 1}, new FloatType());

                Cursor<FloatType> c = img1.cursor();
                while (c.hasNext()) {
                        c.next();
                        System.out.println(c.numDimensions());
                }

                outputImage = (Img<T>) img1.copy();
        }
}
