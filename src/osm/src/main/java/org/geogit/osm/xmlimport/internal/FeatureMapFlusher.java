package org.geogit.osm.xmlimport.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geogit.repository.WorkingTree;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.util.ProgressListener;

import com.google.common.collect.HashMultimap;

/**
 * A buffer that wraps a multimap to store features, that flushes when a certain limit is reached.
 * Flushing means in this case inserting the features in the working tree.
 * 
 * The main purpose of this is to reduce the number of insert operations, while still supporting a
 * large amount of feature without causing an OOM error.
 * 
 */
public class FeatureMapFlusher {

    private static final int LIMIT = 10000;

    private HashMultimap<String, SimpleFeature> map;

    private WorkingTree workTree;

    private int count;

    public FeatureMapFlusher(WorkingTree workTree) {
        this.workTree = workTree;
        map = HashMultimap.create();
        count = 0;
    }

    public void put(String path, SimpleFeature feature) {
        map.put(path, feature);
        count++;
        if (count > LIMIT) {
            flushAll();
        }

    }

    private void flush(String path) {
        Set<SimpleFeature> features = map.get(path);
        if (!features.isEmpty()) {
            Iterator<? extends Feature> iterator = features.iterator();
            ProgressListener listener = new NullProgressListener();
            List<org.geogit.api.Node> insertedTarget = null;
            Integer collectionSize = Integer.valueOf(features.size());
            workTree.insert(path, iterator, listener, insertedTarget, collectionSize);
        }
    }

    /**
     * Inserts all features currenty stored in this object into the working tree.
     */
    public void flushAll() {
        for (String key : map.keySet()) {
            flush(key);
        }
        count = 0;
    }

}
