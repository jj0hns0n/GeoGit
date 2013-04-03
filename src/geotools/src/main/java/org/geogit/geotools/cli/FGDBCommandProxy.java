/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.cli;

import org.geogit.cli.CLICommandExtension;
import org.geogit.geotools.porcelain.FGDBExport;
import org.geogit.geotools.porcelain.FGDBImport;
import org.geogit.geotools.porcelain.ShpImport;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

/**
 * {@link CLICommandExtension} that provides a {@link JCommander} for file geodatabase specific
 * commands.
 * <p>
 * Usage:
 * <ul>
 * <li> {@code geogit fgdb <command> <args>...}
 * </ul>
 * 
 * @see ShpImport
 */
@Parameters(commandNames = "fgdb", commandDescription = "GeoGit/ESRI file geodatabase integration utilities")
public class FGDBCommandProxy implements CLICommandExtension {

    /**
     * @return the JCommander parser for this extension
     * @see JCommander
     */
    @Override
    public JCommander getCommandParser() {
        JCommander commander = new JCommander();
        commander.setProgramName("geogit fgdb");
        commander.addCommand("import", new FGDBImport());
        commander.addCommand("export", new FGDBExport());
        return commander;
    }
}
