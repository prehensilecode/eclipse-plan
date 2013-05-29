/* Plan.java */

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
 
/* $Id: Plan.java 253 2008-06-24 18:25:24Z dwchin $ */

package edu.harvard.lroc.eclipseplan;


import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.archimed.dicom.*;

/**
 * A Plan consists of the stuff in the Plan file (RP.*.dcm)
 * @author David Chin
 * @version $Revision: 253 $
 */
public class Plan {
    
    /**
     * Simple exceptions.
     */
    public class PlanException extends EclipsePlanException {
        PlanException() {}
        
        PlanException(String msg) {
            super(msg);
        }
    }
    
    /** Creates a new instance of Plan */
    public Plan() {
        this.userHome = new File(System.getProperty("user.home"));
    }
    
    public Plan(File planFile) {
        this();
        
        try {
            this.planFile = planFile;
            this.planDir = this.planFile.getParentFile();
            
            this.read();
        } catch (Exception ex) {
            Logger.getLogger(Plan.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 
     * @param patientID
     * @param planDir
     */
    public Plan(String patientID, File planDir) {
        this();
        
        try {
            this.planDcm = new DicomObject();
            
            this.patientID = patientID;
            this.planDir = planDir;
            //this.phantom = new Phantom(this.patientID, this.planDir);

            
            // get the plan file
            File[] listing = planDir.listFiles(new RPFilenameFilter());

            // there should be only one RP file per plan
//            if (listing.length == 0) {
//                throw new PlanException("There are no RP plan files.");
//            } else if (listing.length > 1) {
//                throw new PlanException("There must be only one RP plan file.");
//            }

            this.planFile = listing[0];
            
            this.read();
            
            this.beamList = new BeamSequence(this.planDcm);

            if (debug_p) {
                System.out.println(this.beamList.toString());
                System.out.println();
            }
        } catch (Exception ex) {
            Logger.getLogger(Plan.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * 
     * @param patientID
     * @param planDir
     */
    public Plan(String patientID, String planDir) {
        this(patientID, new File(planDir));
    }
    
    /**
     * 
     * @param patientID
     */
    public Plan(String patientID) {
        this(patientID, new File(System.getProperty("user.home"), patientID));
    }
    
    /**
     * 
     * @return predicate: Patient ID is consistent with directory name.
     * @throws com.archimed.dicom.DicomException
     */
    private boolean patientIDConsistent_p() throws DicomException {
        return this.patientID.equalsIgnoreCase(this.planDcm.getS(DDict.dPatientID));
    }
    
    /**
     * Reads in plan data from the RP file.
     */
    private void read() throws EclipsePlanException {
        FileInputStream fin = null;
        
        // read in the plan file
        try {
            fin = new FileInputStream(this.planFile);
            BufferedInputStream bis = new BufferedInputStream(fin);
            
            DicomReader dcmReader = new DicomReader();
           
            this.planDcm = dcmReader.read(fin, true);
            this.patientID = this.planDcm.getS(DDict.dPatientID);
            
            if (this.debug_p) {
                this.planDcm.dumpVRs(System.out);
            }
            
            if (!this.patientIDConsistent_p()) {
                System.err.println("DICOM patient ID: " + this.planDcm.getS(DDict.dPatientID));
                throw new EclipsePlanException("Inconsistent Patient ID and directory name");
            }
            
            this.beamList = new BeamSequence(this.planDcm);   
            
        } catch (DicomException ex) {
            Logger.getLogger(Plan.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Plan.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Logger.getLogger(Plan.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Create DOSXYZ input file.
     */
    public void makeDosxyzEgsinp() {
        return;
    }
    
    
    @Override
    public String toString() {
        StringBuffer strbuff = new StringBuffer();
        strbuff.append("RT Plan Information:\n");
        strbuff.append("====================\n");
        strbuff.append("Patient ID: ");
        strbuff.append(this.patientID);
        strbuff.append("\n");
        strbuff.append("Treatment type: ");
        if (this.mlc_p)
            strbuff.append("DYNAMIC");
        else
            strbuff.append("STATIC");
        strbuff.append("\n");
        strbuff.append("No. of beams: ");
        strbuff.append(this.beamList.getNBeams());
        strbuff.append("\n");
        
        return strbuff.toString();
    }

    

    /** 
     * 
     * @return beam sequence for this plan
     */
    public BeamSequence getBeamSequence() {
        return beamList;
    }

    /**
     * 
     * @return Dicom object representing this plan
     */
    public DicomObject getPlanDcm() {
        return planDcm;
    }

    // FIXME: in principle, a plan can have both types of beams: those
    //        which use MLC (either static or dynamic), and those which don't
    /**
     * 
     * @return predicate: this plan uses MLC
     */
    public boolean hasMLC_p() {
        return mlc_p;
    }

    /**
     * 
     * @return the "phantom" created from the CT images associated with this plan
     */
//    public Phantom getPhantom() {
//        return phantom;
//    }

    /**
     * 
     * @return directory in which the exported plan files reside
     */
    public File getPlanDir() {
        return planDir;
    }

    /**
     * 
     * @return plan file
     */
    public File getPlanFile() {
        return planFile;
    }

    /**
     * 
     * @return user's home directory
     */
    public File getUserHome() {
        return userHome;
    }
    
    /** Debug flag. */
    private boolean debug_p = false;
    
    /** Phantom created from CT images */
//    private Phantom phantom;

    /** Beam sequence. */
    private BeamSequence beamList;
    
    /** DICOM object that holds all info from the RP plan file. */
    private DicomObject planDcm;
    
    /** User's home directory. */
    private File userHome;

    /** Exported plan directory */
    private File planDir;

    /** The plan file */
    private File planFile;

    /** Patient ID string. */
    private String patientID;
    
    /**
     * Getter for property patientID.
     * @return Patient ID string.
     */
    public String getPatientID() {
        return this.patientID;
    }

    // anonymize 
    
    /**
     * 
     * @return patient's first name
     */
    public String getPatientFirstName() {
        return "PATIENT";
    }
    
    /**
     * 
     * @return patient's last name
     */
    public String getPatientLastName() {
        return this.patientID;
    }
    
    /** Plan uses MLC? */
    private boolean mlc_p;
    
    /** Does this plan use MLC? 
     *  @return predicate: MLC plan?
     */
    public boolean getMlc_p() {
        return this.mlc_p;
    }

    

    
    //
    // Parameters for DOSXYZ 
    //
    
    
    
    //
    // Parameters for BEAMnrc
    //
    
    
    
    
    /**
     * Simple test program
     * @param args
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) throws FileNotFoundException {
        boolean debug_p = true;
        Plan plan;
        
        if (debug_p)
            System.getProperties().list(System.out);
        

        plan = new Plan("A068331");
        System.out.println(plan.toString());
        
    }
}
