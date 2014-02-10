/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */

package org.geogit.api.plumbing;

import java.util.Iterator;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.plumbing.diff.DiffEntry;
import org.geogit.api.plumbing.diff.DiffObjectCount;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Computes the bounds of the difference between the two trees instead of the actual diffs.
 * 
 */

public class DiffBounds extends AbstractGeoGitOp<DiffObjectCount> {

    @Override
    public DiffObjectCount call() {
        // TODO Auto-generated method stub
        return null;
    }

    public DiffBounds() {

    }

    /**
     * 
     * @param entries - A list containing each of the DiffEntries
     * @return Envelope - representing the final bounds
     */
    public Envelope computeDiffBounds(Iterator<DiffEntry> entries) {

        Envelope boundsEnvelope = new Envelope();
        boundsEnvelope.setToNull();

        Envelope oldEnvelope = new Envelope();
        Envelope newEnvelope = new Envelope();

        // create a list of envelopes using the entries list
        while (entries.hasNext()) {
            DiffEntry entry = entries.next();

            if (entry.getOldObject() != null) {
                entry.getOldObject().expand(oldEnvelope);
            }

            if (entry.getNewObject() != null) {
                entry.getNewObject().expand(newEnvelope);
            }

            if (!oldEnvelope.equals(newEnvelope)) {
            	if (entry.getOldObject() != null) entry.getOldObject().expand(boundsEnvelope);
            	if (entry.getNewObject() != null) entry.getNewObject().expand(boundsEnvelope);
            }
        }

        return boundsEnvelope;

    }
}
