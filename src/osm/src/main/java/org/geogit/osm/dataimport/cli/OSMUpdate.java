/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.dataimport.cli;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.geogit.api.GeoGIT;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.SymRef;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.cli.AbstractCommand;
import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.dataimport.internal.OSMImportOp;
import org.geogit.osm.dataimport.internal.OSMLogEntry;
import org.geogit.osm.dataimport.internal.ReadOSMFilterFile;
import org.geogit.osm.dataimport.internal.ReadOSMLogEntries;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Updates previously imported OSM data, connecting again to the Overpass API
 * 
 * It reuses the filter and mapping from the last import that can be reached from the the current
 * branch
 * 
 * The current mechanism is not smart, since it downloads all data, not just the elements
 * modified/added since the last import (which is stored on the OSM log file)
 */
@Parameters(commandNames = "updates", commandDescription = "Updates a geogit repository with OpenStreetMap data")
public class OSMUpdate extends AbstractCommand implements CLICommand {

    private static final String DEFAULT_API_ENDPOINT = "http://overpass-api.de/api/interpreter";

    private static final String OSM_FETCH_BRANCH = null;

    @Parameter(arity = 1, description = "<OSM Overpass api URL. eg: http://api.openstreetmap.org/api>", required = false)
    public List<String> apiUrl = Lists.newArrayList();

    private GeoGIT geogit;

    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        checkState(cli.getGeogit() != null, "Not a geogit repository: " + cli.getPlatform().pwd());

        ConsoleReader console = cli.getConsole();

        geogit = cli.getGeogit();

        final Optional<Ref> currHead = geogit.command(RefParse.class).setName(Ref.HEAD).call();
        Preconditions.checkState(currHead.isPresent(), "Repository has no HEAD, can't update.");
        Preconditions.checkState(currHead.get() instanceof SymRef,
                "Can't update from detached HEAD");

        List<OSMLogEntry> entries = geogit.command(ReadOSMLogEntries.class).call();
        checkArgument(!entries.isEmpty(), "Not in a geogit repository with OSM data");

        Iterator<RevCommit> log = geogit.command(LogOp.class).setFirstParentOnly(false)
                .setTopoOrder(false).call();

        RevCommit lastCommit = null;
        OSMLogEntry lastEntry = null;
        while (log.hasNext()) {
            RevCommit commit = log.next();
            for (OSMLogEntry entry : entries) {
                if (entry.getId().equals(commit.getId())) {
                    lastCommit = commit;
                    lastEntry = entry;
                    break;
                }
            }
        }
        checkNotNull(lastCommit, "The current branch does not contain OSM data");

        geogit.command(BranchCreateOp.class).setSource(lastCommit.getId().toString())
                .setName(OSM_FETCH_BRANCH).setAutoCheckout(true).setForce(true);

        Optional<String> filter = geogit.command(ReadOSMFilterFile.class).setEntry(lastEntry)
                .call();

        Preconditions.checkState(filter.isPresent(), "Filter file not found");

        geogit.command(OSMImportOp.class).setFilter(filter.get()).setDataSource(resolveAPIURL())
                .setProgressListener(cli.getProgressListener()).call();

        geogit.command(CheckoutOp.class).setSource(currHead.get().getName()).call();

        cli.execute("merge", OSM_FETCH_BRANCH);

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
