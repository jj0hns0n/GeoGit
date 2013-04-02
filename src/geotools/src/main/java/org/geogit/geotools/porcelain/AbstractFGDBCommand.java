/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */
package org.geogit.geotools.porcelain;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.Map;

import org.geogit.cli.CLICommand;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.DataStore;
import org.geotools.data.ogr.OGRDataStoreFactory;
import org.geotools.data.ogr.bridj.BridjOGRDataStoreFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Maps;

/**
 * A template for fgdb commands; provides out of the box support for the --help argument so far.
 * 
 * @see CLICommand
 */
public abstract class AbstractFGDBCommand implements CLICommand {

    /**
     * Flag for displaying help for the command.
     */
    @Parameter(names = "--help", help = true, hidden = true)
    public boolean help;

    /**
     * Factory for constructing the data store.
     * 
     * @see OGRDataStoreFactory
     */
    public OGRDataStoreFactory dataStoreFactory = new BridjOGRDataStoreFactory();

    /**
     * Executes the command.
     * 
     * @param cli
     * @throws Exception
     * @see org.geogit.cli.CLICommand#run(org.geogit.cli.GeogitCLI)
     */
    @Override
    public void run(GeogitCLI cli) throws Exception {
        if (help) {
            printUsage();
            return;
        }

        runInternal(cli);
    }

    protected void printUsage() {
        JCommander jc = new JCommander(this);
        String commandName = this.getClass().getAnnotation(Parameters.class).commandNames()[0];
        jc.setProgramName("geogit shp " + commandName);
        jc.usage();
    }

    /**
     * Subclasses shall implement to do the real work, will not be called if the command was invoked
     * with {@code --help}
     */
    protected abstract void runInternal(GeogitCLI cli) throws Exception;

    /**
     * Constructs a new fgdb data store using the specified directory.
     * 
     * @param directory the directory with the fgdb
     * @return the constructed data store
     * @throws Exception
     * @see DataStore
     */
    protected DataStore getDataStore(String fgdb) throws Exception {
        Map<String, Serializable> params = Maps.newHashMap();
        params.put(OGRDataStoreFactory.OGR_DRIVER_NAME.key, "FileGDB");
        params.put(OGRDataStoreFactory.OGR_NAME.key, fgdb);
        params.put(OGRDataStoreFactory.NAMESPACEP.key, "http://www.opengis.net/gml");

        if (!dataStoreFactory.canProcess(params)) {
            throw new FileNotFoundException();
        }

        DataStore dataStore = dataStoreFactory.createDataStore(params);

        if (dataStore == null) {
            throw new FileNotFoundException();
        }

        return dataStore;
    }
}