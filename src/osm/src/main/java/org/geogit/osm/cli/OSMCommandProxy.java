/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.osm.changeset.cli.CreateOSMChangeset;
import org.geogit.osm.history.cli.OSMHistoryImport;
import org.geogit.osm.update.cli.OSMUpdate;
import org.geogit.osm.xmlexport.cli.OSMExport;
import org.geogit.osm.xmlimport.cli.OSMImport;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for osm specific commands.
 * 
 * @see OSMHistoryImport
 */
@Parameters(commandNames = "osm", commandDescription = "GeoGit/OpenStreetMap integration utilities")
public class OSMCommandProxy implements CLICommandExtension {

    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit osm");
        commander.addCommand("import-history", new OSMHistoryImport());
        commander.addCommand("import", new OSMImport());
        commander.addCommand("export", new OSMExport());
        commander.addCommand("update", new OSMUpdate());
        commander.addCommand("create-changeset", new CreateOSMChangeset());
        return commander;
    }
}
