/*
 * Beam.java
 */

/*
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
 * $Id: Beam.java 253 2008-06-24 18:25:24Z dwchin $
 */
package edu.harvard.lroc.eclipseplan;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.archimed.dicom.*;

// TODO: collimator rotation -- BeamLimitingDeviceAngle can vary from beam to beam

/**
 * A single therapy or QA beam.
 * @author David Chin
 * @version $Revision: 253 $
 */
public class Beam {

    /** Basic constructor */
    public Beam() {
        this.userHome = new File(System.getProperty("user.home"));
        this.userEgsDir = new File(userHome, "egsnrc");
    }

    /** Creates a new instance of Beam.
     * 
     * @param patientID 
     * @param beamDcm Dicom object containing an Eclipse plan
     */
    public Beam(String patientID, DicomObject beamDcm) {
        this.patientID = patientID;
        this.readDicomInfo(beamDcm);
    }
    
    /** Debug flag */
    private boolean debug_p = false;
    
    /** User's home directory */
    private File userHome;
    
    /** User's EGS directory */
    private File userEgsDir;
    
    /** Patient's first name */
    private String patientFirstName = "";
    
    /** Patient's last name */
    private String patientLastName = "";
    
    /** Patient ID */
    private String patientID = "";
    
    /** Name of beam */
    private String name;
    
    /** Index number of beam */
    private Integer number;
    
    /** Type of beam -- static or dynamic */
    private String type;
    
    /** Radiation type */
    private String radiationType;
    
    /** Nominal beam energy */
    private Double nominalBeamEnergy;
    
    /** Dose rate  (MU/min) */
    private Double doseRate;
    
    // Assume we will always have ASYMX and ASYMY jaws, i.e. asymmetric
    
    /** X- and Y- jaws */
    private Jaws jaws;
    
    /** Gantry angle */
    private Double gantryAngle;
    
    /** Isocenter position */
    private Double isocenter[] = new Double[3];
    
    /** Source-surface distance */
    private Double ssd;
    
    /** This beam uses MLC */
    private boolean hasMLC_p;
    
    /** MLC sequence */
    private MLCSequence mlcSequence;

    /** @return User's EGS directory */
    public File getUserEgsDir() {
        return userEgsDir;
    }

    /** @return User's home directory */
    public File getUserHome() {
        return userHome;
    }

    
    
    /** @return dose rate */
    public Double getDoseRate() {
        return doseRate;
    }

    /** @return gantry angle */
    public Double getGantryAngle() {
        return gantryAngle;
    }

    /** @return isocenter position */
    public Double[] getIsocenter() {
        return isocenter;
    }

    /** @return name of beam */
    public String getName() {
        return name;
    }

    /** @return nominal beam energy */
    public Double getNominalBeamEnergy() {
        return nominalBeamEnergy;
    }

    /** @return index number of beam */
    public Integer getNumber() {
        return number;
    }

    /** @return type of radiation */
    public String getRadiationType() {
        return radiationType;
    }

    /** @return source-surface distance */
    public Double getSsd() {
        return ssd;
    }

    /** @return type of beam */
    public String getType() {
        return type;
    }

    /** 
     * @return predicate: this beam is dynamic
     */
    public boolean isDynamic() {
        return this.hasMLC_p;
    }
    
    /** Populates the Beam object with info from a Dicom object */
    private void readDicomInfo(DicomObject beamDcm) {
        if (this.debug_p) {
            try {
                beamDcm.dumpVRs(System.out);
            } catch (IOException ex) {
                Logger.getLogger(Beam.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        try {
            this.name = beamDcm.getS(DDict.dBeamName);
            this.number = beamDcm.getI(DDict.dBeamNumber);
            this.type = beamDcm.getS(DDict.dBeamType);
            
            if (this.type.equalsIgnoreCase("dynamic"))
                this.hasMLC_p = true;
            else
                this.hasMLC_p = false;
            
            this.radiationType = beamDcm.getS(DDict.dRadiationType);
            
            // the jaw positions are only in the first control point
            DicomObject ctrlPtDcm = (DicomObject) beamDcm.get(DDict.dControlPointSequence, 0);
            this.gantryAngle = new Double(ctrlPtDcm.getS(DDict.dGantryAngle, 0));
            this.jaws = new Jaws(ctrlPtDcm);
            
            if (this.hasMLC_p)
                this.mlcSequence = new MLCSequence(this.patientID, beamDcm);
        } catch (DicomException ex) {
            Logger.getLogger(Beam.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EclipsePlanException ex) {
            Logger.getLogger(Beam.class.getName()).log(Level.SEVERE, null, ex);
        }  
    }

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
    
    /**
     * 
     * @return patient ID
     */
    public String getPatientID() {
        return this.patientID;
    }


    
    
    
    @Override
    public String toString() {
        StringBuffer strbuff = new StringBuffer();
        strbuff.append("(Patient ID: ");
        strbuff.append(this.patientID);
        strbuff.append(", Beam name: ");
        strbuff.append(this.name);
        strbuff.append(", ");
        strbuff.append("Beam number: ");
        strbuff.append(this.number);
        
        if (this.hasMLC_p)
            strbuff.append(", DYNAMIC, ");
        else
            strbuff.append(", STATIC, ");
        
        strbuff.append("Gantry angle: ");
        strbuff.append(this.gantryAngle);
        strbuff.append("Jaws: [");
        strbuff.append(this.jaws.toString());
        strbuff.append("])");
        
        return strbuff.toString();
    }
    
    /**
     * Simple test routine.
     * @param args
     */
    public static void main(String[] args) {
        File planFile = new File("/home/dwchin/A068331/RP_A068331_IMRT_FC_QA.dcm");
        FileInputStream fin = null;
        DicomObject planDcm = null;
        DicomObject tmpDcm = null;
        Beam beam = null;
        
        // NB it seems not possible to get a sequence item as a new DicomObject,
        // i.e.
        // DicomObject foo = planDcm.get(DDict.dBeamSequence)
        
        try {            
            fin = new FileInputStream(planFile);
            DicomReader dcmReader = new DicomReader();
            planDcm = dcmReader.read(fin);
            String patientID = planDcm.getS(DDict.dPatientID);
            System.out.println("FOOBAR: patientID = " + patientID);
            int nBeams = planDcm.getSize(DDict.dBeamSequence);
            System.out.println("seqLen = " + nBeams);

            for (int i = 0; i < nBeams; ++i) {
                tmpDcm = planDcm.getSequenceItem(DDict.dBeamSequence, i);
                beam = new Beam(patientID, tmpDcm);
                System.out.println("");
                System.out.println(beam.toString());
                System.out.println("* * * * * * * * *");
            }
        } catch (IOException ex) {
            Logger.getLogger(Beam.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DicomException ex) {
            Logger.getLogger(Beam.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Logger.getLogger(Beam.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
    }
    
}
