package org.geogit.osm.dataimport.internal;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.annotation.Nullable;

import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class EntityConverter {

    protected static final String NAMESPACE = "www.openstreetmap.org";

    private static SimpleFeatureType NodeType;

    protected synchronized static SimpleFeatureType nodeType() {
        if (NodeType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,tags:String,location:Point:srid=4326";
            try {
                SimpleFeatureType type = DataUtilities.createType(NAMESPACE,
                        OSMUtils.NODE_TYPE_NAME, typeSpec);
                boolean longitudeFirst = true;
                CoordinateReferenceSystem forceLonLat = CRS.decode("EPSG:4326", longitudeFirst);
                NodeType = DataUtilities.createSubType(type, null, forceLonLat);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return NodeType;
    }

    private static SimpleFeatureType WayType;

    private synchronized static SimpleFeatureType wayType() {
        if (WayType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,tags:String,way:LineString:srid=4326";
            try {
                SimpleFeatureType type = DataUtilities.createType(NAMESPACE,
                        OSMUtils.WAY_TYPE_NAME, typeSpec);
                boolean longitudeFirst = true;
                CoordinateReferenceSystem forceLonLat = CRS.decode("EPSG:4326", longitudeFirst);
                WayType = DataUtilities.createSubType(type, null, forceLonLat);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return WayType;
    }

    public SimpleFeature toFeature(Entity entity, Geometry geom) {

        SimpleFeatureType ft = entity instanceof Node ? nodeType() : wayType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(ft);

        // "visible:Boolean,version:Int,timestamp:long,[location:Point | way:LineString];
        builder.set("visible", Boolean.TRUE); // TODO: Check this!
        builder.set("version", Integer.valueOf(entity.getVersion()));
        builder.set("timestamp", Long.valueOf(entity.getTimestamp().getTime()));
        String tags = buildTagsString(entity.getTags());
        builder.set("tags", tags);
        if (entity instanceof Node) {
            builder.set("location", geom);
        } else if (entity instanceof Way) {
            builder.set("way", geom);
        } else {
            throw new IllegalArgumentException();
        }

        String fid = String.valueOf(entity.getId());
        SimpleFeature simpleFeature = builder.buildFeature(fid);
        return simpleFeature;
    }

    public Entity toEntity(SimpleFeature feature) {
        Entity entity;
        Geometry geom = (Geometry) feature.getDefaultGeometryProperty().getValue();
        if (geom instanceof Point) {// node
            Point pt = (Point) geom;
            long id = Long.parseLong(feature.getID());
            int version = ((Integer) feature.getAttribute("version")).intValue();
            Long milis = (Long) feature.getAttribute("timestamp");
            Date timestamp = new Date(milis);
            OsmUser user;
            Collection<Tag> tags = Lists.newArrayList();
            String tagsString = (String) feature.getAttribute("tags");
            String[] tokens = tagsString.split(";");
            for (String token : tokens) {
                String[] subtokens = token.split(":");
                Tag tag = new Tag(subtokens[0], subtokens[1]);
                tags.add(tag);
            }
            entity = new Node(id, version, timestamp, null, 0l, tags, pt.getY(), pt.getX());
        } else {
            // TODO:
            entity = null;
        }

        return entity;
    }

    /**
     * @param collection
     * @return
     */
    @Nullable
    private static String buildTagsString(Collection<Tag> collection) {
        if (collection.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Iterator<Tag> it = collection.iterator(); it.hasNext();) {
            Tag e = it.next();
            String key = e.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String value = e.getValue();
            sb.append(key).append(':').append(value);
            if (it.hasNext()) {
                sb.append(';');
            }
        }
        return sb.toString();
    }

}