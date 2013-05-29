/* StructureSet.java */

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

/* $Id: StructureSet.java 253 2008-06-24 18:25:24Z dwchin $ */


/*
 * Aggregates all the structures from one RS file, i.e. all those
 * associated with one patient. Despite the name, it's not technically a list.
 */

package edu.harvard.lroc.eclipseplan;

import java.io.*;
import java.util.*;

import com.archimed.dicom.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.*;
import java.awt.geom.Rectangle2D;

/** Container class: enccapsulates all the structures defined for a particular 
 *  patient.
 * @author David Chin
 * @version $Revision: 253 $
 */
public class StructureSet {
    
    /**
     * 
     * @param rsfile Eclipse-generated RS file.
     */
    public StructureSet(File rsfile) {
        this.structureFile = rsfile;
        this.planDir = this.structureFile.getParentFile();
        read();
    }
    
    /**
     * 
     * @return File containing structure data
     */
    public File getStructureFile() {
        return structureFile;
    }

    @Override
    public String toString() {
        String rep = "";
        rep += "Patient ID: " + this.patientID + "\n";
        rep += "Number of structures: " + this.structureMap.entrySet().size() + "\n";
        
        for (String name : this.structureMap.navigableKeySet()) {
            rep += "    Structure: " + this.structureMap.get(name);
        }
        
        return rep;
    }

    /**
     * 
     * @return Patient ID.
     */
    public String getPatientID() {
        return patientID;
    }

    /**
     * 
     * @return Dicom object from structure file.
     */
    public DicomObject getStructureDcm() {
        return structureDcm;
    }
    
    
    // What the RS file looks like -- see STRUCTURE.txt at the top of this
    // source tree. See also Structure.main()
    // Roughly:
    //   * StructureSetROISequence
    //     - ROINumber
    //     - ROIName
    //     
    //     - ROINumber
    //     - ROIName
    //         .
    //         .
    //         .
    //  * ROIContourSequence
    //    - ROIDisplayColor
    //    - ContourSequence
    //      - ContourImageSequence
    //      - ContourGeometricType
    //      - NumberOfContourPoints
    //      - ContourData
    //
    //      - ContourImageSequence
    //      - ContourGeometricType
    //      - NumberOfContourPoints
    //      - ContourData
    //    - ReferencedROINumber
    //
    //    - ROIDisplayColor
    //    - ContourSequence
    //    - ReferencedROINumber
    //
    //  * RTROIObservationsSequence
    //    - ObservationNumber
    //    - ReferencedROINumber
    //    - ROIObservationLabel
    //    - RTROIInterpretedType
    //    - ROIInterpreter
    //
    //    - ObservationNumber
    //       ......
    
    // The "ReferencedROINumber is what ties all the disparate data to their
    // corresponding physical ROI. So, to aggregate all the data about one
    // structure/ROI, need to read the whole file, and then refer back to the
    // appropriate structure using the ReferencedROINumber. To do so, we 
    // introduce a mapping between ROINumber and the ROIName, viz. numberToNameMap
              
    /**
     * Read in structures and populate the map of structures.
     */
    private void read() {
        FileInputStream fin = null;
        BufferedInputStream bis = null;
        
        DicomObject struct = null;
        DicomObject contourSeq = null;

        // These two maps are keyed by ROI Number. The nameNumberMap
        TreeMap<Integer, DicomObject> structMetaData = new TreeMap<Integer, DicomObject>();
        TreeMap<Integer, DicomObject> structContours = new TreeMap<Integer, DicomObject>();
        
        try {
            fin = new FileInputStream(this.structureFile);
            bis = new BufferedInputStream(fin);
            this.structureDcm = new DicomObject();
            this.structureDcm.read(bis, false);
            this.patientID = this.structureDcm.getS(DDict.dPatientID);
            
            int seqlen = this.structureDcm.getSize(DDict.dStructureSetROISequence);
            for (int i = 0; i < seqlen; ++i) {
                struct = this.structureDcm.getSequenceItem(DDict.dStructureSetROISequence, i);
                structMetaData.put(struct.getI(DDict.dROINumber), struct);
                nameNumberMap.put(struct.getS(DDict.dROIName), struct.getI(DDict.dROINumber));
                contourSeq = this.structureDcm.getSequenceItem(DDict.dROIContourSequence, i);
                structContours.put(struct.getI(DDict.dROINumber), contourSeq);                
            }
            
            for (String roiName : nameNumberMap.navigableKeySet()) {
                Integer roiNumber = nameNumberMap.get(roiName);
                structureMap.put(roiName, 
                        new Structure(structMetaData.get(roiNumber),
                                      structContours.get(roiNumber)));
            }
            
            this.fillNumberNameMap();
            
            if (this.debug_p)
                System.out.println(structureMap);
            
        } catch (IOException ex) {
            Logger.getLogger(StructureSet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DicomException ex) {
            Logger.getLogger(StructureSet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Logger.getLogger(StructureSet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Check that patient ID number is consistent with directory name.
     * @return predicate: Patient ID is consistent with directory name.
     * @throws com.archimed.dicom.DicomException
     */
    private boolean patientIDConsistent_p() throws DicomException {
        return this.patientID.equalsIgnoreCase(this.structureDcm.getS(DDict.dPatientID));
    }
    
    /**
     * 
     * @return TreeMap of the structures defined for patient
     */
    @SuppressWarnings("unchecked")
    public TreeMap<String, Structure> getStructureMap() {
        return (TreeMap<String, Structure>) structureMap.clone();
    }
     
    /** Structures/ROIs. */
    private TreeMap<String, Structure> structureMap = new TreeMap<String, Structure>();
    
    /** Structure file. */
    private File structureFile;
    
    /** Structures Dicom data. */
    private DicomObject structureDcm;
    
    // the structures are referred to by their number within the Dicom file, 
    // so create a dictionary to map from name to number. See long note above
    // about the format of the RS structure file. 
    
    /** Mapping between ROI Name and ROI Number */
    private TreeMap<String, Integer> nameNumberMap = new TreeMap<String, Integer>();
    
    /**
     * 
     * @return name-to-number map
     */
    @SuppressWarnings("unchecked")
    public TreeMap<String, Integer> getNameNumberMap() {
        return (TreeMap<String, Integer>) this.nameNumberMap.clone();
    }
    
    // and do the "reverse" map
    /** maps structure number to name */
    private TreeMap<Integer, String> numberNameMap = new TreeMap<Integer, String>();
    
    /**
     * 
     * @return number-to-name map
     */
    @SuppressWarnings("unchecked")
    public TreeMap<Integer, String> getNumberNameMap() {
        return (TreeMap<Integer, String>) this.numberNameMap.clone();
    }
    
    
    /** fills in the number-to-name map from the name-to-number map */
    private void fillNumberNameMap() {
        for (String s : this.nameNumberMap.navigableKeySet()) {
            this.numberNameMap.put(this.nameNumberMap.get(s), s);
        }
    }
    
    /** Directory containing the structure file */
    private File planDir;
    
    /** Patient ID. */
    private String patientID;
    
    /** Debug flag */
    private boolean debug_p = false;

    
    /** Simple test program.
     * @param args Commandline arguments
     */
    public static void main(String[] args) {
        File rsfile = new File("/home/dwchin/testdicom/RS.1.2.246.352.71.4.1039211570.2388931.20080402133708.dcm");
        StructureSet structs = new StructureSet(rsfile);
        System.out.println(structs.toString());
        
        for (String s : structs.getStructureMap().navigableKeySet())
            System.out.println(structs.structureMap.get(s).getBoundingBox());
        
        Structure bladder = structs.getStructureMap().get("bladder");
        System.out.println("Bladder bounding box: " + bladder.getBoundingBox().toString());
        
        Rectangle2D.Double rect = new Rectangle2D.Double();
        Point3d lower = new Point3d();
        Point3d upper = new Point3d();
        bladder.getBoundingBox().getLower(lower);
        bladder.getBoundingBox().getUpper(upper);
        rect.setRect(lower.x, lower.y, upper.x - lower.x, upper.y - lower.y);
        System.out.println("Rectangle: " + rect.toString());
    }
}
