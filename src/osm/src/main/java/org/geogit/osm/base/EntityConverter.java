package org.geogit.osm.base;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class EntityConverter {

    public SimpleFeature toFeature(Entity entity, Geometry geom) {

        SimpleFeatureType ft = entity instanceof Node ? OSMUtils.nodeType()
                : geom instanceof Polygon ? OSMUtils.closedWayType() : OSMUtils.wayType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(ft);

        builder.set("visible", Boolean.TRUE); // TODO: Check this!
        builder.set("version", Integer.valueOf(entity.getVersion()));
        builder.set("timestamp", Long.valueOf(entity.getTimestamp().getTime()));
        builder.set("changeset", Long.valueOf(entity.getChangesetId()));
        String tags = buildTagsString(entity.getTags());
        builder.set("tags", tags);
        String user = entity.getUser().getName() + ":" + Integer.toString(entity.getUser().getId());
        builder.set("user", user);
        if (entity instanceof Node) {
            builder.set("location", geom);
        } else if (entity instanceof Way) {
            builder.set("way", geom);
            String nodes = buildNodesString(((Way) entity).getWayNodes());
            builder.set("nodes", nodes);
        } else {
            throw new IllegalArgumentException();
        }

        String fid = String.valueOf(entity.getId());
        SimpleFeature simpleFeature = builder.buildFeature(fid);
        return simpleFeature;
    }

    private String buildNodesString(List<WayNode> wayNodes) {
        StringBuilder sb = new StringBuilder();
        for (Iterator<WayNode> it = wayNodes.iterator(); it.hasNext();) {
            WayNode node = it.next();
            sb.append(Long.toString(node.getNodeId()));
            if (it.hasNext()) {
                sb.append(";");
            }
        }
        return sb.toString();

    }

    public Entity toEntity(SimpleFeature feature) {
        // TODO: This doesn't handle mapped features and assumes default feature type!
        Entity entity;
        SimpleFeatureType type = feature.getFeatureType();
        Preconditions.checkArgument(
                type.equals(OSMUtils.wayType()) || type.equals(OSMUtils.nodeType()),
                "Non-OSM feature. Cannot convert to entity:\n" + feature.toString());

        long id = Long.parseLong(feature.getID());
        int version = ((Integer) feature.getAttribute("version")).intValue();
        Long milis = (Long) feature.getAttribute("timestamp");
        Date timestamp = new Date(milis);
        String user = (String) feature.getAttribute("user");
        String[] userTokens = user.split(":");
        OsmUser osmuser;
        try {
            osmuser = new OsmUser(Integer.parseInt(userTokens[1]), userTokens[0]);
        } catch (Exception e) {
            osmuser = OsmUser.NONE;
        }
        Collection<Tag> tags = Lists.newArrayList();
        String tagsString = (String) feature.getAttribute("tags");
        if (tagsString != null) {
            String[] tokens = tagsString.split(";");
            for (String token : tokens) {
                String[] subtokens = token.split(":");
                Tag tag = new Tag(subtokens[0], subtokens[1]);
                tags.add(tag);
            }
        }

        CommonEntityData entityData = new CommonEntityData(id, version, timestamp, osmuser, 0l,
                tags);
        if (type.equals(OSMUtils.nodeType())) {
            Point pt = (Point) feature.getDefaultGeometryProperty().getValue();
            entity = new Node(entityData, pt.getY(), pt.getX());

        } else {
            List<WayNode> nodes = Lists.newArrayList();
            String nodesString = (String) feature.getAttribute("nodes");
            for (String s : nodesString.split(";")) {
                nodes.add(new WayNode(Long.parseLong(s)));
            }
            entity = new Way(entityData, nodes);
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