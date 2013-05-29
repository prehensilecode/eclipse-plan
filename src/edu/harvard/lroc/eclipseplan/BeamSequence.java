/*
 * BeamSequence.java
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

/*
 * $Id: BeamSequence.java 253 2008-06-24 18:25:24Z dwchin $
 */

package edu.harvard.lroc.eclipseplan;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import com.archimed.dicom.*;

// These beams will share some information, and each beam
//     will also have their own information (gantry angle, field size, etc.)

// Structure of the Plan DICOM file
// --------------------------------
// Plan file contains:
//    1 plan which contains:
//       1 beam sequence which contains:
//          N beams, each of which contains: 
//             information about beam
//             1 beam limiting device sequence, which contains:
//                information about number of jaws, and number and size of
//                MLC leaves
//             1 control point sequence, which contains: 
//                location of leaves at each time step; the first
//                of these also contain jaw positions.
//                each 
//
//

/**
 * Container class: encapsulates all beams in a patient plan.
 * @author David Chin
 * @version $Revision: 253 $
 */
public class BeamSequence {   
    /**
     * Creates a new instance of BeamSequence
     * @param planDcm Dicom object containing an Eclipse plan.
     * @throws EclipsePlanException
     */
    public BeamSequence(DicomObject planDcm) throws EclipsePlanException {
        this.planDcm = planDcm;
        this.readDicomInfo();
    }
    
    /**
     * 
     * @param planFile Plan file
     * @throws EclipsePlanException
     */
    public BeamSequence(File planFile) throws EclipsePlanException {
        this.planFile = planFile;
        this.readPlanFile();
        this.readDicomInfo();
    }
    
    /**
     * @param planFileName absolute path of plan file
     * @throws EclipsePlanException
     */
    public BeamSequence(String planFileName) throws EclipsePlanException {
        this(new File(planFileName));
    }
    
    
    /**
     * Read from plan file
     */
    private void readPlanFile() throws EclipsePlanException {
        FileInputStream fin = null;
        
        try {
            fin = new FileInputStream(this.planFile);
            BufferedInputStream bis = new BufferedInputStream(fin);
            this.planDcm = new DicomObject();
            this.planDcm.read(bis, false);
        } catch (IOException ex) {
            Logger.getLogger(BeamSequence.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DicomException ex) {
            Logger.getLogger(BeamSequence.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Logger.getLogger(BeamSequence.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        
    }
    
    /**
     * Read plan information 
     */
    private void readDicomInfo() throws EclipsePlanException {        
        try {
            // check that this is actually an RTPLAN
            if (this.planDcm.get(DDict.dModality) == null || !this.planDcm.get(DDict.dModality).toString().equalsIgnoreCase("rtplan")) {
                throw new EclipsePlanException("Not an RTPLAN");
            }

            // extract patient's name
//            String patientName = this.planDcm.getS(DDict.dPatientName);
//            System.out.println("patientName = " + patientName);
//            this.patientFirstName = patientName.split("\\^")[1];
//            this.patientLastName = patientName.split("\\^")[0];
            
            // anonymize patient name
            this.patientID = this.planDcm.getS(DDict.dPatientID);
            
            this.patientFirstName = "PATIENT";
            this.patientLastName = this.patientID;

            int nBeams = this.planDcm.getSize(DDict.dBeamSequence);
            DicomObject tmpBeamDcm;

            for (int i = 0; i < nBeams; ++i) {
                try {
                    tmpBeamDcm = this.planDcm.getSequenceItem(DDict.dBeamSequence, i);
                    this.beamMap.put(new String(tmpBeamDcm.getS(DDict.dBeamName)), 
                            new Beam(this.patientID, tmpBeamDcm));
                } catch (DicomException ex) {
                    Logger.getLogger(BeamSequence.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            // infer if this is a dynamic beam from each individual beam type.
            // if any beam is dynamic,
            //       this whole sequence is dynamic, and
            //       throw out the static beams (usually QA and imaging)
            // else
            //       this whole sequence is static
            this.dynamic_p = false;
            for (String beamName : this.beamMap.navigableKeySet()) {
                if (this.beamMap.get(beamName).isDynamic()) {
                    this.dynamic_p = true;
                }
            }

            // throw out static beams if necessary
            if (this.dynamic_p) {
                NavigableSet<String> beamNames = new TreeSet<String>(this.beamMap.navigableKeySet());
                for (String bn : beamNames) {
                    if (this.beamMap.get(bn).isDynamic() != this.isDynamic()) {
                        this.beamMap.remove(bn);
                    }
                }
            }
        } catch (DicomException ex) {
            Logger.getLogger(BeamSequence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** 
     * @return predicate: this beam is dynamic delivery
     */
    public boolean isDynamic() {
        return dynamic_p;
    }
    
    /**
     * 
     * @return predicate: this beam has MLC
     */
    public boolean hasMLC() {
        return this.isDynamic();
    }
    
    /** This beam sequence is dynamic, i.e. has MLC */
    private boolean dynamic_p;
    
    /**
     * Returns an info string.
     * @return String containing summary info about the beam sequence.
     */
    @Override
    public String toString() {
        String infostr = "Beam Sequence information:\n";
        infostr       += "==========================\n";
        infostr       += "Number of beams: " + this.getNBeams();
        
        StringBuffer strBuff = new StringBuffer("Beam Sequence information:\n");
        strBuff.append("==========================\n");
        strBuff.append("Patient name: ");
        strBuff.append(this.patientFirstName);
        strBuff.append(" ");
        strBuff.append(this.patientLastName);
        strBuff.append("\n");
        strBuff.append("Number of beams: " + this.getNBeams());
        
        // TODO: iterate over the beams and add the string representations
        
        return strBuff.toString();
    }
    
      
    /**
     * Returns number of beams in the beam sequence.
     * @return Number of beams
     */
    public int getNBeams() {
        return this.beamMap.size();
    }
    
    /** DICOM object of  the Eclipse plan containing beam sequence */
    private DicomObject planDcm;
    
    /** Dicom file containing export Eclipse plan */
    private File planFile;
    
    /** Get plan file 
     * @return plan file
     */
    public File getPlanFile() {
        return this.planFile;
    }
    
    /**
     * A sorted map of the beams in this plan. The key is the beam name,
     * and the beam is the value.
     */
    private TreeMap<String, Beam> beamMap = new TreeMap<String, Beam>();

    /**
     * 
     * @return map of beams
     */
    @SuppressWarnings("unchecked")
    public TreeMap<String, Beam> getBeamMap() {
        return (TreeMap<String, Beam>) beamMap.clone();
    }
    
    /** Patient's first name */
    private String patientFirstName;
    
    /** Patient's last name */
    private String patientLastName;
    
    /** Patient ID */
    private String patientID;
    
    /** Debug flag */
    private boolean debug_p = true;
    
    /** Simple test program
     * @param args Commandline arguments
     */
    public static void main(String[] args) {

        try {
            File planFile = new File("/home/dwchin/A068331/RP_A068331_IMRT_FC_QA.dcm");
            //File planFile = new File("/home/dwchin/A068331/RS_A068331_.dcm");
            BeamSequence bs = new BeamSequence(planFile);

            System.out.println(bs);
            System.out.println(bs.getBeamMap().toString());
            
        } catch (Exception ex) {
            Logger.getLogger(BeamSequence.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

