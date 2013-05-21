/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.xmlexport.cli;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.geogit.api.Bounded;
import org.geogit.api.GeoGIT;
import org.geogit.api.NodeRef;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.plumbing.LsTreeOp;
import org.geogit.api.plumbing.LsTreeOp.Strategy;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.base.EntityConverter;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Exports features from a feature type into a shapefile.
 * 
 * @see ExportOp
 */
@Parameters(commandNames = "export", commandDescription = "Export to OSM XML")
public class OSMExport extends AbstractCommand implements CLICommand {

    @Parameter(description = "<file>", arity = 1)
    public List<String> args;

    @Parameter(names = { "--overwrite", "-o" }, description = "Overwrite output file")
    public boolean overwrite;

    @Parameter(names = { "--bbox", "-b" }, description = "The bounding box to use as filter (S W N E).", arity = 4)
    private List<String> bbox;

    private GeoGIT geogit;

    /**
     * Executes the export command using the provided options.
     * 
     * @param cli
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }

        if (args.size() != 1) {
            printUsage();
            return;
        }

        checkArgument(bbox == null || bbox.size() == 4, "The specified bounding box is not correct");

        String osmfile = args.get(0);
        File file = new File(osmfile);
        checkArgument(!file.exists() || overwrite,
                "The selected file already exists. Use -o to overwrite");

        geogit = cli.getGeogit();

        Iterator<EntityContainer> nodes = getFeatures("node");
        Iterator<EntityContainer> ways = getFeatures("way");
        Iterator<EntityContainer> iterator = Iterators.concat(nodes, ways);
        XmlWriter writer = new XmlWriter(file, CompressionMethod.None);
        while (iterator.hasNext()) {
            EntityContainer entity = iterator.next();
            writer.process(entity);
        }
        writer.complete();

    }

    private Iterator<EntityContainer> getFeatures(String ref) {
        LsTreeOp op = geogit.command(LsTreeOp.class).setStrategy(Strategy.DEPTHFIRST_ONLY_FEATURES)
                .setReference(ref);
        if (bbox != null) {
            final Envelope env;
            try {
                env = new Envelope(Double.parseDouble(bbox.get(0)),
                        Double.parseDouble(bbox.get(2)), Double.parseDouble(bbox.get(1)),
                        Double.parseDouble(bbox.get(3)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong bbox definition");
            }
            Predicate<Bounded> filter = new Predicate<Bounded>() {
                @Override
                public boolean apply(final Bounded bounded) {
                    boolean intersects = bounded.intersects(env);
                    return intersects;
                }
            };
            op.setBoundsFilter(filter);
        }
        Iterator<NodeRef> iterator = op.call();
        final EntityConverter converter = new EntityConverter();
        Function<NodeRef, EntityContainer> function = new Function<NodeRef, EntityContainer>() {

            @Override
            @Nullable
            public EntityContainer apply(@Nullable NodeRef ref) {
                RevFeature revFeature = geogit.command(RevObjectParse.class)
                        .setObjectId(ref.objectId()).call(RevFeature.class).get();
                RevFeatureType revFeatureType = geogit.command(RevObjectParse.class)
                        .setObjectId(ref.getMetadataId()).call(RevFeatureType.class).get();
                SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(
                        (SimpleFeatureType) revFeatureType.type());
                ImmutableList<PropertyDescriptor> descriptors = revFeatureType.sortedDescriptors();
                ImmutableList<Optional<Object>> values = revFeature.getValues();
                for (int i = 0; i < descriptors.size(); i++) {
                    PropertyDescriptor descriptor = descriptors.get(i);
                    Optional<Object> value = values.get(i);
                    featureBuilder.set(descriptor.getName(), value.orNull());
                }
                SimpleFeature feature = featureBuilder.buildFeature(ref.name());
                Entity entity = converter.toEntity(feature);
                EntityContainer container;
                if (entity instanceof Node) {
                    container = new NodeContainer((Node) entity);
                } else {
                    container = new WayContainer((Way) entity);
                }

                return container;

            }

        };
        return Iterators.transform(iterator, function);
    }
}
