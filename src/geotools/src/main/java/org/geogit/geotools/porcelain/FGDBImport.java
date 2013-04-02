/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import java.util.List;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geogit.geotools.plumbing.GeoToolsOpException;
import org.geogit.geotools.plumbing.ImportOp;
import org.geotools.data.DataStore;
import org.opengis.util.ProgressListener;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Preconditions;

/**
 * Imports one or more tables from a ESRI file geodatabase.
 * 
 * FGDB CLI proxy for {@link ImportOp}
 * 
 * @see ImportOp
 */
@Parameters(commandNames = "import", commandDescription = "Import ESRI file geodatabase")
public class FGDBImport extends AbstractFGDBCommand implements CLICommand {

    /**
     * If this is set, only this table will be imported.
     */
    @Parameter(names = { "--table", "-t" }, description = "Table to import.")
    public String table = "";

    /**
     * If this is set, all tables will be imported.
     */
    @Parameter(names = "--all", description = "Import all tables.")
    public boolean all = false;

    /**
     * Path to the directory of the file geodatabase
     */
    @Parameter(description = "<fgdb_path>", arity = 1)
    List<String> path;

    /**
     * Executes the import command using the provided options.
     * 
     * @param cli
     * @see org.geogit.cli.AbstractFGDBCommand#runInternal(org.geogit.cli.GeogitCLI)
     */
    @Override
    protected void runInternal(GeogitCLI cli) throws Exception {
        if (cli.getGeogit() == null) {
            cli.getConsole().println("Not a geogit repository: " + cli.getPlatform().pwd());
            return;
        }
        Preconditions.checkArgument(!path.isEmpty(), "No path to file geodatabase was provided");

        DataStore dataStore = getDataStore(path.get(0));

        try {
            cli.getConsole().println("Importing from file geodatabase " + path.get(0));

            ProgressListener progressListener = cli.getProgressListener();
            cli.getGeogit().command(ImportOp.class).setAll(all).setTable(table)
                    .setDataStore(dataStore).setProgressListener(progressListener).call();

            cli.getConsole().println("Import successful.");

        } catch (GeoToolsOpException e) {
            switch (e.statusCode) {
            case TABLE_NOT_DEFINED:
                cli.getConsole().println(
                        "No tables specified for import. Specify --all or --table <table>.");
                break;
            case ALL_AND_TABLE_DEFINED:
                cli.getConsole().println("Specify --all or --table <table>, both cannot be set.");
                break;
            case NO_FEATURES_FOUND:
                cli.getConsole().println("No features were found in the database.");
                break;
            case TABLE_NOT_FOUND:
                cli.getConsole().println("Could not find the specified table.");
                break;
            case UNABLE_TO_GET_NAMES:
                cli.getConsole().println("Unable to get feature types from the database.");
                break;
            case UNABLE_TO_GET_FEATURES:
                cli.getConsole().println("Unable to get features from the database.");
                break;
            case UNABLE_TO_INSERT:
                cli.getConsole().println("Unable to insert features into the working tree.");
                break;
            default:
                cli.getConsole().println("Import failed with exception: " + e.statusCode.name());
            }
        } finally {
            dataStore.dispose();
            cli.getConsole().flush();
        }
    }
}
