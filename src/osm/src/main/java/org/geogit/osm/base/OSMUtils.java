package org.geogit.osm.base;

import org.geotools.data.DataUtilities;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Throwables;

public class OSMUtils {

    public static final String NODE_TYPE_NAME = "node";

    public static final String WAY_TYPE_NAME = "way";

    public static final String NAMESPACE = "www.openstreetmap.org";

    public static SimpleFeatureType NodeType;

    public synchronized static SimpleFeatureType nodeType() {
        if (NodeType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,tags:String,"
                    + "changeset:java.lang.Long,user:String,location:Point:srid=4326";
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

    public static SimpleFeatureType WayType;

    public synchronized static SimpleFeatureType wayType() {
        if (WayType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,tags:String,"
                    + "changeset:java.lang.Long,user:String,nodes:String,way:LineString:srid=4326";
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

    public static SimpleFeatureType ClosedWayType;

    public synchronized static SimpleFeatureType closedWayType() {
        if (ClosedWayType == null) {
            String typeSpec = "visible:Boolean,version:Integer,timestamp:java.lang.Long,tags:String,"
                    + "changeset:java.lang.Long,user:String,nodes:String,way:Polygon:srid=4326";
            try {
                SimpleFeatureType type = DataUtilities.createType(NAMESPACE,
                        OSMUtils.WAY_TYPE_NAME, typeSpec);
                boolean longitudeFirst = true;
                CoordinateReferenceSystem forceLonLat = CRS.decode("EPSG:4326", longitudeFirst);
                ClosedWayType = DataUtilities.createSubType(type, null, forceLonLat);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return ClosedWayType;
    }

}
