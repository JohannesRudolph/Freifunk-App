package de.inmotion_sst.freifunkfinder.clustering;

import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;

import java.util.Set;

public interface ClusterAlgorithm<T extends ClusterItem> {
    Set<? extends Cluster<T>> getClusters(double var1);
}
