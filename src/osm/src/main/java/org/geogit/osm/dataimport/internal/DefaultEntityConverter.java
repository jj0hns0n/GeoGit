package org.geogit.osm.dataimport.internal;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.Nullable;

import org.geotools.data.DataUtilities;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.google.common.base.Throwables;
import com.vividsolutions.jts.geom.Geometry;

public class DefaultEntityConverter extends EntityConverter {

    public DefaultEntityConverter() {
        super(null);
    }

    public Feature convert(Entity entity, Geometry geom) {
        SimpleFeature feature = toFeature(entity, geom);
        return feature;
    }

    /** NODE */
    protected static final String NODE_TYPE_NAME = "node";

    private static SimpleFeatureType NodeType;

    protected synchronized static SimpleFeatureType nodeType() {
        if (NodeType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,tags:String,location:Point:srid=4326";
            try {
                SimpleFeatureType type = DataUtilities.createType(NAMESPACE, NODE_TYPE_NAME,
                        typeSpec);
                boolean longitudeFirst = true;
                CoordinateReferenceSystem forceLonLat = CRS.decode("EPSG:4326", longitudeFirst);
                NodeType = DataUtilities.createSubType(type, null, forceLonLat);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return NodeType;
    }

    private static final String WAY_TYPE_NAME = "way";

    private static SimpleFeatureType WayType;

    private synchronized static SimpleFeatureType wayType() {
        if (WayType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,tags:String,way:LineString:srid=4326";
            try {
                SimpleFeatureType type = DataUtilities.createType(NAMESPACE, WAY_TYPE_NAME,
                        typeSpec);
                boolean longitudeFirst = true;
                CoordinateReferenceSystem forceLonLat = CRS.decode("EPSG:4326", longitudeFirst);
                WayType = DataUtilities.createSubType(type, null, forceLonLat);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return WayType;
    }

    private static SimpleFeature toFeature(Entity feature, Geometry geom) {

        SimpleFeatureType ft = feature instanceof Node ? nodeType() : wayType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(ft);

        // "visible:Boolean,version:Int,timestamp:long,[location:Point | way:LineString];
        builder.set("visible", Boolean.TRUE); // TODO: Check this!
        builder.set("version", Integer.valueOf(feature.getVersion()));
        builder.set("timestamp", Long.valueOf(feature.getTimestamp().getTime()));

        String tags = buildTagsString(feature.getTags());
        builder.set("tags", tags);
        if (feature instanceof Node) {
            builder.set("location", geom);
        } else if (feature instanceof Way) {
            builder.set("way", geom);
        } else {
            throw new IllegalArgumentException();
        }

        String fid = String.valueOf(feature.getId());
        SimpleFeature simpleFeature = builder.buildFeature(fid);
        return simpleFeature;
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