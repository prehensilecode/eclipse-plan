/* MLCSequence.java */

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

/* $Id: MLCSequence.java 253 2008-06-24 18:25:24Z dwchin $ */

package edu.harvard.lroc.eclipseplan;

import com.archimed.dicom.*;
import java.io.*;
import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

// NOTE: one can never get a DICOM sequence object, only "atomic" DICOM
//       objects. So, to get the constructor to work, we need to pass it
//       the DicomObject of the parent to the sequence, so that the constructor
//       can iterate over the individual sequence members.

// For BEAMnrc:
//    the MLC sequence is split into two portions:
//    1. the leaf geometries and the initial leaf positions are embedded in the
//       egsinp file of the BEAM run
//    2. the subsequent leaf positions are put into a .mlc file. this file is 
//       NOT in Shaper .mlc format.

/** MLC motion sequence for a single treatment field (Beam).
 *
 * @author David Chin
 * @version $Revision: 253 $
 */
public class MLCSequence {
    public MLCSequence() {
        this.nSteps = 0;
        this.mlc = new Vector<MLCPosition>(); 
    }
    
    /**
     * 
     * @param nSteps number of time steps in the MLCPosition sequence
     */
    public MLCSequence(int nSteps) {
        this.nSteps = nSteps;
        this.mlc = new Vector<MLCPosition>(this.nSteps);
    }
    
    /** 
     * 
     * @param patientID 
     * @param beamDcm Dicom object specifying the MLC motion
     * @throws DicomException
     * @throws EclipsePlanException 
     */
    public MLCSequence(String patientID, DicomObject beamDcm) throws DicomException, EclipsePlanException {
         // TODO: implement
        if (this.debug_p && false) {
            System.out.println("MLCSequence constr. -- nCtrlPts = " + beamDcm.getSize(DDict.dControlPointSequence));
            DumpUtils dumper = new DumpUtils(60, 60, 2, true);
            try {
                BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream("dump.txt"));
                dumper.dump(beamDcm, fos);
            } catch (IOException ex) {
                Logger.getLogger(MLCSequence.class.getName()).log(Level.SEVERE, null, ex);
            } 
                    
        }
    
        // make sure this is a dynamic beam
        String beamType = beamDcm.getS(DDict.dBeamType);
        if (!beamType.equalsIgnoreCase("dynamic")) {
            throw new EclipsePlanException("ERROR: MLC sequence requires a dynamic beam");
        }
       
        this.patientID = patientID;
        this.patientFirstName = "PATIENT";
        this.patientLastName = this.patientID;
        
        this.beamName = beamDcm.getS(DDict.dBeamName);
                   
        try {
            this.nSteps = beamDcm.getI(DDict.dNumberOfControlPoints);
        } catch (DicomException ex) {
            Logger.getLogger(MLCSequence.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        this.mlc = new Vector<MLCPosition>(this.nSteps);

        // the beam dicom contains a control point sequence
        // each control point contains the leaf/jaw positions
        for (int i = 0; i < this.nSteps; ++i) {
            this.mlc.add(new MLCPosition(beamDcm.getSequenceItem(DDict.dControlPointSequence, i), this.beamName));         
        }
    }
    
    /** Number of time steps */
    private Integer nSteps;
    
    /** Vector of MLC positions */
    private Vector<MLCPosition> mlc;
    
    /** Beam name */
    private String beamName;
    
    /** Patient ID */
    private String patientID = "";
    
    /** Patient's first name */
    private String patientFirstName = "";
    
    /** Patient's last name */
    private String patientLastName = "";
    
    /** debug flag */
    private boolean debug_p = true;
    
    /** 
     * @return Number of leaves
     */
    public Integer getNLeafPairs() {
        Integer nLeaves = 0;
        
        if (this.debug_p) {
            System.out.println("getNLeafPairs: mlc.size = " + this.mlc.size());
        }
        
        if (this.mlc.size() > 0) {
            nLeaves = this.mlc.get(0).getNLeafPairs();
            
            System.out.println(this.mlc.get(0).toString());
        } 
        
        return nLeaves;
    }
    
    /**
     * 
     * @return MLCPosition sequence in Shaper format
     */
    public String shaperFormat() {
        StringBuffer strbuff = new StringBuffer();
        strbuff.append("File Rev = G\n");
        strbuff.append("Treatment = Dynamic Dose\n");
        strbuff.append("Last Name = ");
        strbuff.append(this.patientLastName);
        strbuff.append("\n");
        strbuff.append("First Name = ");
        strbuff.append(this.patientFirstName);
        strbuff.append("\n");
        strbuff.append("Patient ID = ");
        strbuff.append(this.patientID);
        strbuff.append("\n");
        strbuff.append(String.format("Number of Fields = %d\n", this.nSteps));
        strbuff.append(String.format("Number of Leaves = %d\n", this.getNLeafPairs() * 2));
        
        // the tolerance here is not from the Dicom: it's the same in every
        // Eclipse-export shaper mlc file
        strbuff.append(String.format("Tolerance = % 10.4f\n\n", 0.2)); 
        
        for (MLCPosition m : this.mlc) {
            strbuff.append(m.shaperFormat());
        }
        
        // dunno how to compute the actual CRC. for EGS, it's ignored
        strbuff.append("CRC = 0000");
        
        return strbuff.toString();
    }
    
    public void writeShaperFile(String filename) throws IOException {
        File shaperFile = new File(filename);
        shaperFile.createNewFile();
        shaperFile.setWritable(true);
        
        this.writeShaperFile(shaperFile);
    }
    
    /**
     * 
     * @param file
     * @throws java.io.IOException
     */
    public void writeShaperFile(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        
        if (!file.canWrite()) {
            throw new IOException("file not writable");
        }
        
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(this.shaperFormat().getBytes());
        fos.close();
    }
    
    
    
    
    @SuppressWarnings("unchecked")
    public Vector<MLCPosition> getMLC() {
        return (Vector<MLCPosition>) mlc.clone();
    }
    
    @Override
    public String toString() {
        String rep = "No. of steps: " + this.nSteps + "; ";
        rep += "No. of leaf pairs: " + this.getNLeafPairs();
        
        return rep;
    }
    
    /**
     * Simple test program
     * @param args
     * @throws FileNotFoundException
     * @throws IOException
     * @throws DicomException 
     */
    public static void main(String[] args) throws FileNotFoundException, IOException, DicomException {
        MLCSequence seq = new MLCSequence(39);
        System.out.println(seq.toString());
        
        System.out.println("================================================");
        
        File planFile = new File("/home/dwchin/A047486/RP_A047486_FINAL_REPLAN1.dcm");
        FileInputStream fin = null;
        DicomObject planDcm = null;
        
        // read in the plan file
      
        fin = new FileInputStream(planFile);
        BufferedInputStream bis = new BufferedInputStream(fin);
        BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream("mlcseqdump.txt"));    
        DicomReader dcmReader = new DicomReader();
        
        planDcm = dcmReader.read(bis, true);
        
        DumpUtils dumper = new DumpUtils(60, 60, 4, true, false);
        
        dumper.dump(planDcm, fos);
        fos.close();
        
        DicomObject beamDcm = null;
        int nBeams = planDcm.getSize(DDict.dBeamSequence);
        String patientID = planDcm.getS(DDict.dPatientID);
        System.out.println("Patient ID: " + patientID);
        System.out.println("No. of beams: " + nBeams);
        
        // just do the first 3 beams
        for (int i = 0; i < 3; ++i) {
            beamDcm = planDcm.getSequenceItem(DDict.dBeamSequence, i);
            
            fos = new BufferedOutputStream(new FileOutputStream(patientID + "_" 
                    + beamDcm.getS(DDict.dBeamName) + "_dump.txt"));
            
            dumper.dump(beamDcm, fos);
            fos.close();
            
            System.out.println("Beam no. " + i);
            System.out.println("Beam name: " + beamDcm.getS(DDict.dBeamName));
            System.out.println("No. of ctrl pts = " + beamDcm.getI(DDict.dNumberOfControlPoints));
            System.out.println("   also         = " + beamDcm.getSize(DDict.dControlPointSequence));
            
            MLCSequence ms = null;
            try {
                ms = new MLCSequence(patientID, beamDcm);
            } catch (EclipsePlanException ex) {
                Logger.getLogger(MLCSequence.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            System.out.println(ms.toString());
            
            for (MLCPosition mlc : ms.mlc) {
                System.out.println(mlc.shaperFormat());
            }
            
            System.out.println(".....................");
            
            System.out.println(ms.shaperFormat());
            
            ms.writeShaperFile("/tmp/foobar.mlc");
            
            System.out.println("= = = = = = = = = = =");
        }
        
    }
}
