package org.geogit.geotools.porcelain;

import java.util.Arrays;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.porcelain.CommitOp;
import org.geogit.cli.GeogitCLI;
import org.geogit.test.integration.RepositoryTestCase;
import org.geotools.data.ogr.OGRDataStoreFactory;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FGDBExportTest extends RepositoryTestCase {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private GeogitCLI cli;

    private static OGRDataStoreFactory factory;

    @BeforeClass
    public static void oneTimeSetup() throws Exception {
        factory = FGDBTestHelper.createTestFactory();
    }

    @Override
    public void setUpInternal() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);

        cli.setGeogit(geogit);

        // Add points
        insertAndAdd(points1);
        insertAndAdd(points2);
        insertAndAdd(points3);

        geogit.command(CommitOp.class).call();

        // Add lines
        insertAndAdd(lines1);
        insertAndAdd(lines2);
        insertAndAdd(lines3);

        geogit.command(CommitOp.class).call();
    }

    @Override
    public void tearDownInternal() throws Exception {
        cli.close();
    }

    @Test
    public void testExport() throws Exception {
        FGDBExport exportCommand = new FGDBExport();
        String geodbFolder = "TestPoints.gdb";
        exportCommand.args = Arrays.asList("Points", "Points", geodbFolder);
        exportCommand.dataStoreFactory = factory;
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithNullFeatureType() throws Exception {
        FGDBExport exportCommand = new FGDBExport();
        String geodbFolder = "TestPoints.gdb";
        exportCommand.args = Arrays.asList("Points", geodbFolder, "Points");
        exportCommand.dataStoreFactory = FGDBTestHelper.createNullTestFactory();
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithInvalidFeatureType() throws Exception {
        FGDBExport exportCommand = new FGDBExport();
        String geodbFolder = "TestPoints.gdb";
        exportCommand.args = Arrays.asList("invalidType", geodbFolder, "Points");
        exportCommand.dataStoreFactory = factory;
        exception.expect(IllegalArgumentException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithFeatureNameInsteadOfType() throws Exception {
        FGDBExport exportCommand = new FGDBExport();
        String geodbFolder = "TestPoints.gdb";
        exportCommand.args = Arrays.asList("Points/Points.1", geodbFolder, "Points");
        exportCommand.dataStoreFactory = factory;
        exception.expect(IllegalArgumentException.class);
        exportCommand.run(cli);
    }

    @Test
    public void testExportWithNoArgs() throws Exception {
        FGDBExport exportCommand = new FGDBExport();
        exportCommand.args = Arrays.asList();
        exportCommand.dataStoreFactory = FGDBTestHelper.createNullTestFactory();
        exportCommand.run(cli);
    }

    @Test
    public void testExportToFileThatAlreadyExistsWithOverwrite() throws Exception {
        FGDBExport exportCommand = new FGDBExport();
        String geodbFolder = "TestPoints.gdb";
        exportCommand.args = Arrays.asList("Points", "Points", geodbFolder);
        exportCommand.dataStoreFactory = factory;
        exportCommand.overwrite = true;
        exportCommand.run(cli);

    }
}
