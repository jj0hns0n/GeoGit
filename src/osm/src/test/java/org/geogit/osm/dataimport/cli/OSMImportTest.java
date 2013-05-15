/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.dataimport.cli;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Node;
import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.dataimport.internal.OSMLogEntry;
import org.geogit.osm.dataimport.internal.ReadOSMLogEntries;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class OSMImportTest extends Assert {

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
    }

    @Test
    public void testImportFromFile() throws Exception {
        String filename = getClass().getResource("nodes.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = cli.getGeogit().command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
    }

    @Test
    public void testImportNodes() throws Exception {
        String filename = getClass().getResource("nodes_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "import", "-f", filterFile.getAbsolutePath());
        Iterator<DiffEntry> unstaged = cli.getGeogit().getRepository().getWorkingTree()
                .getUnstaged(null);
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = cli.getGeogit().command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());
    }

    @Test
    public void testImportWays() throws Exception {
        String filename = getClass().getResource("ways_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "import", "-f", filterFile.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
    }

    @Test
    public void testImportWaysWithoutNodes() throws Exception {
        String filename = getClass().getResource("ways_no_nodes_overpass_filter.txt").getFile();
        File filterFile = new File(filename);
        cli.execute("osm", "import", "-f", filterFile.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertFalse(tree.isPresent());
        // check that there are no commits
    }

    @Test
    public void testImportWithBBox() throws Exception {
        cli.execute("osm", "import", "-b", "50.79", "7.19", "50.8", "7.20");
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("way");
        assertTrue(tree.isPresent());
    }

}
