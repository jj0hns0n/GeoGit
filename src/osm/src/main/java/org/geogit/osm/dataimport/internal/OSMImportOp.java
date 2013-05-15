/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.osm.dataimport.internal;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.management.relation.Relation;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevFeature;
import org.geogit.api.RevTree;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveTreeish;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.repository.WorkingTree;
import org.geotools.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.util.ProgressListener;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class OSMImportOp extends AbstractGeoGitOp<Optional<OSMLogEntry>> {

    private String mapping;

    private String filter;

    private String urlOrFilepath;

    public OSMImportOp setFilter(String filter) {
        this.filter = filter;
        return this;
    }

    public OSMImportOp setMapping(String mapping) {
        this.mapping = mapping;
        return this;
    }

    public OSMImportOp setDataSource(String urlOrFilepath) {
        this.urlOrFilepath = urlOrFilepath;
        return this;
    }

    @Override
    public Optional<OSMLogEntry> call() {

        checkNotNull(urlOrFilepath);

        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(true)
                .setNameFormat("osm-data-download-thread-%d").build();
        final ExecutorService executor = Executors.newFixedThreadPool(3, threadFactory);

        Preconditions.checkState(
                getIndex().countStaged(null) + getWorkTree().countUnstaged(null) == 0,
                "Working tree and index are not clean");

        File file;
        if (urlOrFilepath.startsWith("http")) {
            getProgressListener().setDescription("Downloading data...");
            checkNotNull(filter);
            OSMDownloader downloader = new OSMDownloader(urlOrFilepath, executor,
                    getProgressListener());

            Future<File> data = downloader.download(filter);

            try {
                file = data.get();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            } catch (ExecutionException e) {
                throw Throwables.propagate(e);
            }
        } else {
            file = new File(urlOrFilepath);
            Preconditions.checkArgument(file.exists(), "File does not exist: " + urlOrFilepath);
        }

        getProgressListener().setDescription("Importing into GeoGit repo...");

        EntityConverter converter;
        if (mapping == null) {
            converter = new DefaultEntityConverter();
        } else {
            converter = new EntityConverter(mapping);
        }

        OSMLogEntry entry = parseDataFileAndInsert(file, converter);

        if (entry != null) {
            command(WriteOSMFilterFile.class).setEntry(entry).setFilterCode(filter);
        }

        return Optional.fromNullable(entry);

    }

    private OSMLogEntry parseDataFileAndInsert(File file, final EntityConverter converter) {

        boolean pbf = false;
        CompressionMethod compression = CompressionMethod.None;

        if (file.getName().endsWith(".pbf")) {
            pbf = true;
        } else if (file.getName().endsWith(".gz")) {
            compression = CompressionMethod.GZip;
        } else if (file.getName().endsWith(".bz2")) {
            compression = CompressionMethod.BZip2;
        }

        RunnableSource reader;

        if (pbf) {
            try {
                reader = new crosby.binary.osmosis.OsmosisReader(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                // should not reach this, because we have already check existence
                throw new IllegalArgumentException("File does not exist: " + urlOrFilepath);
            }
        } else {
            reader = new XmlReader(file, false, compression);
        }

        ConverterSink sink = new ConverterSink(converter);
        reader.setSink(sink);

        Thread readerThread = new Thread(reader);
        readerThread.start();

        while (readerThread.isAlive()) {
            try {
                readerThread.join();
            } catch (InterruptedException e) {
                return null;
            }
        }

        WorkingTree workTree = this.getWorkTree();

        Multimap<String, SimpleFeature> map = sink.getFeatures();
        for (String parentPath : map.keySet()) {
            Collection<SimpleFeature> features = map.get(parentPath);
            if (features.isEmpty()) {
                continue;
            }

            Iterator<? extends Feature> iterator = features.iterator();
            ProgressListener listener = new NullProgressListener();
            List<org.geogit.api.Node> insertedTarget = null;
            Integer collectionSize = Integer.valueOf(features.size());
            workTree.insert(parentPath, iterator, listener, insertedTarget, collectionSize);
        }

        if (!map.isEmpty()) {
            command(AddOp.class).call();
            RevCommit commit = command(CommitOp.class).setMessage(
                    "Update OSM to changeset " + Long.toString(sink.getLatestChangeset())).call();
            OSMLogEntry entry = new OSMLogEntry(commit.getId(), sink.getLatestChangeset(),
                    sink.getLatestTimestamp());
            command(AddOSMLogEntry.class).setEntry(entry).call();
        }

        return null;

    }

    class ConverterSink implements Sink {

        private EntityConverter converter;

        private long latestChangeset;

        private long latestTimestamp;

        private Multimap<String, SimpleFeature> insertsByParent;

        Map<Long, Coordinate> pointCache;

        public ConverterSink(EntityConverter converter) {
            super();
            this.converter = converter;
            this.latestChangeset = 0;
            this.latestTimestamp = 0;
            this.insertsByParent = HashMultimap.create();
            pointCache = new LinkedHashMap<Long, Coordinate>() {
                /** serialVersionUID */
                private static final long serialVersionUID = 1277795218777240552L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Coordinate> eldest) {
                    return size() == 10000;
                }
            };
        }

        public void process(EntityContainer entityContainer) {
            Entity entity = entityContainer.getEntity();
            latestChangeset = Math.max(latestChangeset, entity.getChangesetId());
            latestTimestamp = Math.max(latestTimestamp, entity.getTimestamp().getTime());
            Geometry geom = parseGeometry(entity);
            Feature feature = converter.convert(entity, geom);
            if (feature != null) { // TODO: revisit this
                String path = feature.getType().getName().getLocalPart();
                insertsByParent.put(path, (SimpleFeature) feature);
            }
        }

        public Multimap<String, SimpleFeature> getFeatures() {
            return insertsByParent;
        }

        public long getLatestTimestamp() {
            return latestTimestamp;
        }

        public long getLatestChangeset() {
            return latestChangeset;
        }

        public void release() {
        }

        public void complete() {
        }

        public void initialize(Map<String, Object> map) {
        }

        protected static final String NODE_TYPE_NAME = "node";

        private final GeometryFactory GEOMF = new GeometryFactory();

        protected Geometry parseGeometry(Entity entity) {

            if (entity instanceof Relation) {
                return null;
            }

            if (entity instanceof Node) {
                Node node = ((Node) entity);
                Coordinate coord = new Coordinate(node.getLongitude(), node.getLatitude());
                Point pt = GEOMF.createPoint(coord);
                pointCache.put(Long.valueOf(node.getId()), coord);
                return pt;
            }

            final Way way = (Way) entity;
            final List<WayNode> nodes = way.getWayNodes();

            List<Coordinate> coordinates = Lists.newArrayList();
            FindTreeChild findTreeChild = command(FindTreeChild.class);
            findTreeChild.setIndex(true);
            ObjectId rootTreeId = command(ResolveTreeish.class).setTreeish(Ref.HEAD).call().get();
            if (!rootTreeId.isNull()) {
                RevTree headTree = command(RevObjectParse.class).setObjectId(rootTreeId)
                        .call(RevTree.class).get();
                findTreeChild.setParent(headTree);
            }
            for (WayNode node : nodes) {
                long nodeId = node.getNodeId();
                Coordinate coord = pointCache.get(nodeId);
                if (coord == null) {
                    String fid = String.valueOf(nodeId);
                    String path = NodeRef.appendChild(NODE_TYPE_NAME, fid);
                    Optional<org.geogit.api.Node> ref = getIndex().findStaged(path);
                    if (!ref.isPresent()) {
                        Optional<NodeRef> nodeRef = findTreeChild.setChildPath(path).call();
                        if (nodeRef.isPresent()) {
                            ref = Optional.of(nodeRef.get().getNode());
                        } else {
                            ref = Optional.absent();
                        }
                    }
                    if (ref.isPresent()) {
                        org.geogit.api.Node nodeRef = ref.get();

                        RevFeature revFeature = getIndex().getDatabase().getFeature(
                                nodeRef.getObjectId());
                        String id = NodeRef.nodeFromPath(nodeRef.getName());
                        Point pt = null;
                        ImmutableList<Optional<Object>> values = revFeature.getValues();
                        for (Optional<Object> opt : values) {
                            if (opt.isPresent()) {
                                Object value = opt.get();
                                if (value instanceof Point) {
                                    pt = (Point) value;
                                }
                            }
                        }

                        if (pt != null) {
                            coord = pt.getCoordinate();
                            pointCache.put(Long.valueOf(nodeId), coord);
                        }
                    }
                }
                if (coord != null) {
                    coordinates.add(coord);
                }
            }
            if (coordinates.size() < 2) {
                return null;
            }
            if (way.isClosed()) {
                Collection<Tag> tags = way.getTags();
                for (Tag tag : tags) {
                    if (tag.getKey().equals("area") && tag.getValue().equals("true")) {
                        return GEOMF.createPolygon(coordinates.toArray(new Coordinate[coordinates
                                .size()]));
                    }
                }
            }
            return GEOMF.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
        }

    };

}
