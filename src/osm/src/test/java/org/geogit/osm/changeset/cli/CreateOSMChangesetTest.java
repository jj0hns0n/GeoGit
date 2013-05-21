/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.osm.changeset.cli;

import java.io.File;
import java.util.List;

import jline.UnsupportedTerminal;
import jline.console.ConsoleReader;

import org.geogit.api.Node;
import org.geogit.api.Platform;
import org.geogit.api.TestPlatform;
import org.geogit.cli.GeogitCLI;
import org.geogit.osm.base.OSMLogEntry;
import org.geogit.osm.base.ReadOSMLogEntries;
import org.geogit.osm.changeset.internal.CreateOSMChangesetOpTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class CreateOSMChangesetTest extends Assert {

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
    public void testCreateChangesets() throws Exception {
        String filename = CreateOSMChangesetOpTest.class.getResource("nodes.xml").getFile();
        File file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        Optional<Node> tree = cli.getGeogit().getRepository().getRootTreeChild("node");
        assertTrue(tree.isPresent());
        List<OSMLogEntry> entries = cli.getGeogit().command(ReadOSMLogEntries.class).call();
        assertFalse(entries.isEmpty());

        filename = CreateOSMChangesetOpTest.class.getResource("nodes2.xml").getFile();
        file = new File(filename);
        cli.execute("osm", "import", file.getAbsolutePath());
        cli.execute("osm", "create-changeset", "HEAD", "HEAD~1", "-f", "d:\\changesets.xml");

    }

}
