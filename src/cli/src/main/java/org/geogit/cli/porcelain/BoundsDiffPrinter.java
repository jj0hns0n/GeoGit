/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the BSD New License, available at the root
 * application directory.
 */
package org.geogit.cli.porcelain;

import java.io.IOException;

import jline.console.ConsoleReader;

import org.fusesource.jansi.Ansi;
import org.geogit.api.GeoGIT;
import org.geogit.cli.AnsiDecorator;

import com.vividsolutions.jts.geom.Envelope;

public class BoundsDiffPrinter {
	 public void print(GeoGIT geogit, ConsoleReader console, Envelope envelope) throws IOException {

        Ansi ansi = AnsiDecorator.newAnsi(console.getTerminal().isAnsiSupported());
        
        if(envelope.isNull()){
        	ansi.a("No differences found.");	
        }else{
        	 ansi.a(envelope.getMinX() + ", "+  envelope.getMaxX() + ", " + envelope.getMinY() + ", " + envelope.getMaxY());
        }
       
	    console.println(ansi.toString());
	 }


}
