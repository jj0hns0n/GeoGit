package org.geogit.osm.dataimport.internal;

import org.opengis.feature.Feature;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

import com.vividsolutions.jts.geom.Geometry;

public class EntityConverter {

    private String mapping;

    /** FeatureType namespace */
    protected static final String NAMESPACE = "www.openstreetmap.org";

    public EntityConverter(String mapping) {
        this.mapping = mapping;
        // TODO Auto-generated constructor stub
    }

    public Feature convert(Entity entity, Geometry geom) {
        return null;
    }

}
