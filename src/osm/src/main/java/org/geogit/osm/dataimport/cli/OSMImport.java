/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.dataimport.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.dataimport.internal.OSMImportOp;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;

/**
 * Imports data from OSM using the Overpass API
 */
@Parameters(commandNames = "import", commandDescription = "Import OpenStreetMap data")
public class OSMImport extends AbstractCommand implements CLICommand {

    private static final String DEFAULT_API_ENDPOINT = "http://overpass-api.de/api/interpreter";

    private static final String FR_API_ENDPOINT = "http://api.openstreetmap.fr/oapi/interpreter/";

    private static final String RU_API_ENDPOINT = "http://overpass.osm.rambler.ru/";

    @Parameter(names = { "--filter", "-f" }, description = "The filter file to use.")
    private String filterFile;

    @Parameter(names = { "--mapping", "-m" }, description = "The mapping file to use.")
    private String mappingFile;

    @Parameter(arity = 1, description = "<OSM Overpass api URL. eg: http://api.openstreetmap.org/api>", required = false)
    public List<String> apiUrl = Lists.newArrayList();

    @Parameter(names = { "--bbox", "-b" }, description = "The bounding box to use as filter (S W N E).", arity = 4)
    private List<String> bbox;

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());
        File importFile = null;
        if (!apiUrl.isEmpty()) {
            importFile = new File(apiUrl.get(0));
            if (!importFile.exists()) {
                if (apiUrl.get(0).startsWith("http")) {
                    checkArgument(filterFile != null ^ bbox != null,
                            "You must specify a filter file or a bounding box when downloading using the OSM API");
                } else {
                    throw new IllegalArgumentException("The specified OSM data file does not exist");
                }
            }
        }

        checkArgument(mappingFile == null || new File(mappingFile).exists(),
                "The specified mapping file does not exist");
        checkArgument(filterFile == null || new File(filterFile).exists(),
                "The specified filter file does not exist");
        checkArgument(bbox == null || bbox.size() == 4, "The specified bounding box is not correct");

        final String osmAPIUrl = resolveAPIURL();

        OSMImportOp op = cli.getGeogit().command(OSMImportOp.class).setDataSource(osmAPIUrl);

        String filter = null;
        if (filterFile != null) {
            filter = readFile(new File(filterFile));
        } else if (bbox != null) {
            filter = "way(" + bbox.get(0) + "," + bbox.get(1) + "," + bbox.get(2) + ","
                    + bbox.get(3) + ");\n(._;>;);\nout meta;";
        }

        String mapping = null;
        if (mappingFile != null) {
            mapping = readFile(new File(mappingFile));
        }

        op.setFilter(filter).setMapping(mapping).setProgressListener(cli.getProgressListener())
                .call();

    }

    private String readFile(File file) throws IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }
        reader.close();
        return fileData.toString();
    }

    private String resolveAPIURL() {
        String osmAPIUrl;
        if (apiUrl.isEmpty()) {
            osmAPIUrl = DEFAULT_API_ENDPOINT;
        } else {
            osmAPIUrl = apiUrl.get(0);
        }
        return osmAPIUrl;
    }
}
