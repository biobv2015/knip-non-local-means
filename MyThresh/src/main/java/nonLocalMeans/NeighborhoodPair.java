package nonLocalMeans;

import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.img.Img;

public class NeighborhoodPair<T> {

	Img<T> pNeighbors;
	T p;
	Neighborhood<T> qNeighbors;
	T q;
	//workaround
	double sigma;
	long span=0;
}
