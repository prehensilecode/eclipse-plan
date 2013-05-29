/*
 * EclipseToEGS.java
 *
 * Converts an exported Eclipse plan into input files
 * for BEAMnrc and DOSXYZnrc.
 *
 * Huge chunks stolen from Joe Killoran.
 */

/*  This file is part of EclipsePlan.
 *
 *  Copyright (C) 2008  Dana-Farber/Brigham & Women's Cancer Center
 *
 *  EclipsePlan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  EclipsePlan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  
 *  NOTE: This program is not to be used for ANY clinical purpose, or
 *        in any situation which will affect patient care. This program
 *        is to be used only for research purposes.
 *
 *  Author: David Chin <dwchin@lroc.harvard.edu>  
 */



/* $Id: EclipseToEGS.java 257 2008-06-25 20:16:51Z dwchin $ */

package edu.harvard.lroc.eclipseplan;

import java.io.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.args4j.*;


/** Converts an exported Eclipse patient plan to EGS input.
 *
 * @author David Chin
 * @version $Revision: 257 $
 */
public class EclipseToEGS {
    public EclipseToEGS() {
    }
    
    /**
     * 
     * @param patientID
     */
    public EclipseToEGS(String patientID) {
        this.patient = new Patient(patientID);
    }
    
    /**
     * 
     * @param patient
     */
    public EclipseToEGS(Patient patient) {
        this.patient = patient;
    }

    /**
     * Patient id
     */
    @Option(name="-patientid", usage="Set patient ID")
    private String patientid;
    
    /**
     * 
     * @return Patient ID
     */
    public String getPatientid() {
        return patientid;
    }
    
    /** Patient data: plan, structures, and CT images. */
    private Patient patient;
    
    /**
     * 
     * @return patient object
     */
    public Patient getPatient() {
        return patient;
    }
    
    private EgsPhant egsphant;
    
    /** Structure to use for cropping phantom */
    @Option(name="-cropstructure", usage="Set cropping structure")
    private String cropstructure;
    
    /**
     * 
     * @return Name of structure used for cropping down phantom.
     */
    public String getCropstructure() {
        return cropstructure;
    }
    
    /** debug flag */
    private boolean debug_p = true;
    
    /** execute program */
    public void run() {
        if (this.patientid == null) {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter patient ID: ");
            try {
                this.patientid = console.readLine();
            } catch (IOException e) {
                System.err.println("Error: " + e);
            }
        }
        
        System.out.println("Reading patient " + this.patientid);
        
        this.patient = new Patient(this.patientid);
        
        this.patient.printStructureList();
        
        this.patient.promptForStructureAndResize();
        
        this.egsphant = new EgsPhant(this.patient);
        try {
            this.egsphant.writeFile();
        } catch (IOException ex) {
            Logger.getLogger(EclipseToEGS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(EclipseToEGS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Simple test program. 
     * @param args
     */
    public static void main(String[] args) {
        EclipseToEGS converter = new EclipseToEGS();
        CmdLineParser parser = new CmdLineParser(converter);
        
        try {
            parser.parseArgument(args);
            converter.run();
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }
        
    }
}
