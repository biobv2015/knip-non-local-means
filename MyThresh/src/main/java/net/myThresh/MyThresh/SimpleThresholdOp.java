package net.myThresh.MyThresh;


import ij.ImagePlus;
import ij.io.Opener;

import java.io.File;

import net.imagej.ImageJ;
import net.imagej.ops.Op;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Op.class, name = "simple_fixed_threshold")
public class SimpleThresholdOp<T extends RealType<T>> implements Op {

	@Parameter(type = ItemIO.OUTPUT)
	private Img<T> res;
	
	@Parameter
	ImagePlus inputImage;
	
	public void run() {
		// convert input image
		Img<T> in = ImageJFunctions.wrapReal(inputImage);
		
		Cursor<T> cursor = in.cursor();
		
		while (cursor.hasNext()) {
			cursor.fwd();
			if(cursor.get().getRealDouble()>100){
				cursor.get().setReal(255);
			}else{
				cursor.get().setReal(0);
			}
			
		}
		res = in;
		
	}
}
