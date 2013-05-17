package org.geogit.osm.dataimport.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.geogit.repository.WorkingTree;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.util.ProgressListener;

import com.google.common.collect.HashMultimap;

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

    public void flushAll() {
        for (String key : map.keySet()) {
            flush(key);
        }
        count = 0;
    }

}
