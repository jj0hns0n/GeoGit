/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.xmlexport.cli;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Node;
import org.geogit.api.Platform;
import org.geogit.api.RevCommit;
import org.geogit.api.TestPlatform;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.LogOp;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.base.OSMLogEntry;
import org.geogit.osm.base.ReadOSMLogEntries;
import org.geogit.osm.xmlimport.cli.OSMImport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class OSMExportTest extends Assert {

    private GeogitCLI cli;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        ConsoleReader consoleReader = new ConsoleReader(System.in, System.out,
                new UnsupportedTerminal());
        cli = new GeogitCLI(consoleReader);
        File workingDirectory = tempFolder.getRoot();
        Platform platform = new TestPlatform(workingDirectory);
        cli.setPlatform(platform);
        cli.execute("init");
        cli.execute("config", "user.name", "Gabriel Roldan");
        cli.execute("config", "user.email", "groldan@opengeo.org");
        assertTrue(new File(workingDirectory, ".geogit").exists());
        String filename = OSMImport.class.getResource("ways.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = cli.getGeogit().command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
    }

    @Test
    public void testExportAndThenReimport() throws Exception {
        File file = new File(tempFolder.getRoot(), "export.xml");
        cli.execute("osm", "export", file.getAbsolutePath());
        cli.getGeogit().getRepository().getWorkingTree().delete("node");
        cli.getGeogit().getRepository().getWorkingTree().delete("way");
        cli.getGeogit().command(AddOp.class).call();
        cli.getGeogit().command(CommitOp.class).setMessage("Deleted OSM data").call();
        cli.execute("osm", "import", file.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
        Iterator<RevCommit> log = cli.getGeogit().command(LogOp.class).call();
        assertTrue(log.hasNext());
        log.next();
        assertTrue(log.hasNext());
        log.next();
        assertTrue(log.hasNext());

    }
}
