/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the LGPL 2.1 license, available at the root
 * application directory.
 */

package org.geogit.geotools.porcelain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Platform;
import org.geogit.cli.GeogitCLI;
import org.geotools.data.ogr.OGRDataStoreFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.exceptions.base.MockitoException;

/**
 *
 */
public class FGDBImportTest extends Assert {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GeogitCLI cli;

    private static OGRDataStoreFactory factory;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        factory = FGDBTestHelper.createTestFactory();
    }

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        setUpGeogit(cli);
    }

    @After
    public void tearDown() throws Exception {
        cli.close();
    }

    @Test
    public void testImport() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = true;
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testNoTableNotAll() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = false;
        importCommand.table = "";
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testAllAndTable() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = true;
        importCommand.table = "table1";
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testNoRepository() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = true;
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testImportTable() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = false;
        importCommand.table = "table1";
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testImportHelp() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.help = true;
        importCommand.run(cli);
    }

    @Test
    public void testImportException() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        GeogitCLI mockCli = spy(new GeogitCLI(consoleReader));

        setUpGeogit(mockCli);

        when(mockCli.getConsole()).thenThrow(new MockitoException("Exception"));
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = true;
        importCommand.dataStoreFactory = factory;
        exception.expect(MockitoException.class);
        importCommand.run(mockCli);
    }

    @Test
    public void testImportNonExistentTable() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = false;
        importCommand.table = "nonexistent";
        importCommand.dataStoreFactory = factory;
        importCommand.run(cli);
    }

    @Test
    public void testImportNonExistentFileGeoDatabase() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://nonexistent.gdb");
        importCommand.all = false;
        importCommand.table = "table1";
        importCommand.run(cli);
    }

    @Test
    public void testEmptyTable() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = true;
        importCommand.dataStoreFactory = FGDBTestHelper.createEmptyTestFactory();
        importCommand.run(cli);
    }

    @Test
    public void testImportGetNamesException() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = true;
        importCommand.dataStoreFactory = FGDBTestHelper.createFactoryWithGetNamesException();
        importCommand.run(cli);
    }

    @Test
    public void testImportFeatureSourceException() throws Exception {
        FGDBImport importCommand = new FGDBImport();
        importCommand.path = new ArrayList<String>();
        importCommand.path.add("file://test.gdb");
        importCommand.all = true;
        importCommand.dataStoreFactory = FGDBTestHelper
                .createFactoryWithGetFeatureSourceException();
        importCommand.run(cli);
    }

    private void setUpGeogit(GeogitCLI cli) throws Exception {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir/.geogit");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);
        when(platform.getUserHome()).thenReturn(userhome);

        cli.setPlatform(platform);
    }
}
