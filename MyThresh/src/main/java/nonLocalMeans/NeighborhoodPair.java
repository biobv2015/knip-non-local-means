package nonLocalMeans;

import net.imglib2.algorithm.neighborhood.Neighborhood;

public class NeighborhoodPair<T> {

	Neighborhood<T> pNeighbors;
	T p;
	Neighborhood<T> qNeighbors;
	T q;
	//workaround
	double sigma;
}
