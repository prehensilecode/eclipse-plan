/* MLCPosition.java */

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
 * $Id: MLCPosition.java 253 2008-06-24 18:25:24Z dwchin $
 */

package edu.harvard.lroc.eclipseplan;

import com.archimed.dicom.*;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Represents the positions of all MLCPosition leaves at one time.
 *
 * @author David Chin
 * @version $Revision: 253 $
 */
public class MLCPosition {
    /**
     * 
     */
    public MLCPosition() {
        this.nLeafPairs = 60;  // default no. of leaf pairs (for Varian)
        this.allocateLeaves();
        
        this.init();
    }
    
    /**
     * 
     * @param nLeafPairs Number of leaf pairs
     */
    public MLCPosition(int nLeafPairs) {
        this.nLeafPairs = nLeafPairs;
        this.allocateLeaves();
        
        this.init();
    }
    
    /**
     * 
     * @param ctrlPtDcm
     * @param beamName 
     */
    public MLCPosition(DicomObject ctrlPtDcm, String beamName) {
        DicomObject beamLimitingDevicePosition = null;

        // if this control point is the first one of the control point sequence,
        // it will contain jaws data which needs to be ignored.
        // so, search for the MLCPosition data
        // Actually, this works even if the jaws data isn't there, so just
        // do it for all cases
        int nDevices = ctrlPtDcm.getSize(DDict.dBeamLimitingDevicePositionSequence);
        String devName;
        for (int i = 0; i < nDevices; ++i) {
            try {
                beamLimitingDevicePosition = ctrlPtDcm.getSequenceItem(DDict.dBeamLimitingDevicePositionSequence, i);
                devName = beamLimitingDevicePosition.getS(DDict.dRTBeamLimitingDeviceType);
                if (devName.startsWith("MLC")) {
                    break;
                }
            } catch (DicomException ex) {
                Logger.getLogger(MLCPosition.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
                
        this.nLeafPairs = beamLimitingDevicePosition.getSize(DDict.dLeafJawPositions)/2;
        this.allocateLeaves();
        
        try {
            for (int i = 0; i < this.nLeafPairs; ++i) {
                this.aLeaves.add(new Double(beamLimitingDevicePosition.getS(DDict.dLeafJawPositions, i)));
            }
                    
            for (int i = this.nLeafPairs; i < this.nLeafPairs * 2; ++i) {        
                this.bLeaves.add(new Double(beamLimitingDevicePosition.getS(DDict.dLeafJawPositions, i)));
            }
        
            this.index = new Double(ctrlPtDcm.getS(DDict.dCumulativeMetersetWeight));
                      
            this.fieldName = beamName + "." + ctrlPtDcm.getS(DDict.dControlPointIndex);
            
            
        } catch (DicomException ex) {
            Logger.getLogger(MLCPosition.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /** Make vectors for leaves */
    private void allocateLeaves() {
        this.aLeaves = new Vector<Double>(this.nLeafPairs);
        this.bLeaves = new Vector<Double>(this.nLeafPairs);
    }

    /** Initialize arrays */
    private void init() {
        for (int i = 0; i < nLeafPairs; ++i) {
            this.aLeaves.add(0.);
            this.bLeaves.add(0.);
        }
    }

    /** 
     * 
     * @return Number of leaves
     */
    public Integer getNLeafPairs() {
        return this.nLeafPairs;
    }
    
    /**
     * 
     * @return Vector of A leaves positions
     */
    @SuppressWarnings("unchecked")
    public Vector<Double> getALeaves() {
        return (Vector<Double>) aLeaves.clone();
    }

    /**
     * 
     * @return Vector of B leaves positions
     */
    @SuppressWarnings("unchecked")
    public Vector<Double> getBLeaves() {
        return (Vector<Double>) bLeaves.clone();
    }
    
    @Override
    public String toString() {
        String ret = "MLC " + this.fieldName + " with " + this.nLeafPairs + " leaf pairs:\n";
        ret += "    " + this.aLeaves.toString() + "\n";
        ret += "    " + this.bLeaves.toString() + "\n";
        return ret;
    }
    
    /**
     * 
     * @return time index [0.0 -- 1.0]
     */
    public Double getIndex() {
        return this.index;
    }
    
    /**
     * 
     * @return a single field specification for a shaper-format .mlc file
     */
    public String shaperFormat() {
        StringBuffer sbuff = new StringBuffer("Field = ");
        sbuff.append(this.fieldName);
        sbuff.append("\n");
        sbuff.append("Index = ");
        sbuff.append(String.format("% 10.4f\n", this.index));
        sbuff.append(String.format("Carriage Group = %d\n", this.carriageGroup));
        sbuff.append(String.format("Operator = %s\n", this.operator));
        sbuff.append(String.format("Collimator = % 7.2f\n", this.collimator));
        
        // A leaves
        for (int i = 0; i < this.nLeafPairs; ++i) {
            sbuff.append("Leaf ");
            sbuff.append(String.format("%2dA = % 7.2f\n", i+1, this.aLeaves.get(i)));
        }
        
        // B leaves
        for (int i = 0; i < this.nLeafPairs; ++i) {
            sbuff.append("Leaf ");
            sbuff.append(String.format("%2dB = % 7.2f\n", i+1, this.bLeaves.get(i)));
        }
        
        sbuff.append(String.format("Note = %d\n", this.note));
        sbuff.append(String.format("Shape = %d\n", this.shape));
        sbuff.append(String.format("Magnification = %4.2f\n\n", this.magnification));
        
        return sbuff.toString();
    }
    
    // EGSnrc's DYNVMLC module wants the leaf motion data to be in 2 separate
    // files, each with a slightly different format.
    //    a) the initial leaf positions are embedded in the egsinp file
    //    b) the subsequent leaf positions are in a separate .mlc file 
    //       (NOT in Shaper format)
    
    // an MLCSequence object has to be careful to call either 
    // egsnrcInitialFormat() or eegsnrcFormat() 
    
    /** Gives egsnrc-format for the egsnrc .mlc file
     * 
     * @return a single field specification for an EGSnrc-input format
     */
    public String egsnrcFormat() {
        StringBuffer strbuff = new StringBuffer();
        
        int fieldNo = Integer.parseInt(this.fieldName.split("\\.")[1]) + 1;
        
        for (int i = 0; i < this.nLeafPairs; ++i)
            strbuff.append(String.format("%d, %f, %f, %f\n", fieldNo, 
                    this.index, this.aLeaves.get(i), this.bLeaves.get(i)));
        
        return strbuff.toString();
    }
    
    /** Gives egnsrc-format for the egsinp file
     * 
     * @return the initial positions of the leaves
     */
    public String egsnrcInitialFormat() {
        StringBuffer strbuff = new StringBuffer();
        
        for (int i = 0; i < this.nLeafPairs; ++i)
            strbuff.append(String.format("%10.6f, %10.6f, %d,\n", 
                    this.aLeaves.get(i), this.bLeaves.get(i), 1));
        
        return strbuff.toString();
    }
    
    /** Number of leaves in MLC */
    private Integer nLeafPairs;
    
    /** Positions of the A leaves (in mm) */
    private Vector<Double> aLeaves;
    
    /** Positions of the B leaves (in mm) */
    private Vector<Double> bLeaves;
    
    /** Field name: beamName.ctrlPointIndex */
    private String fieldName;
    
    /** Index -- the "time index" of when this MLCPosition position is in place */
    private Double index = 0.0;
    
    
    
    // the following variables are required to be in the output 
    // Shaper file, but they seem to be unused, i.e. they're not related to 
    // anything in the dicom file, and they always have the default values 
    // as initialized below

    /** Carriage group */
    private Integer carriageGroup = 0;
    
    /** Operator */
    private String operator = "";   
    
    /** Collimator */
    private Double collimator = 0.0;  
    
    /** Note */
    private Integer note = 0;
    
    /** Shape */
    private Integer shape = 0;
    
    /** Maginification */
    private Double magnification = 1.0;
    
    /** debug flag */
    private boolean debug_p = true;
    
    /**
     * Simple test program
     * @param args
     */
    public static void main(String[] args) {
        MLCPosition mlc = new MLCPosition(60);
        System.out.println("Number of leaf pairs = " + mlc.getNLeafPairs());
        
        Vector<Double> aleaves = mlc.getALeaves();
        System.out.println("No. leaf pairs = " + aleaves.size());
        System.out.println(mlc);
        
        mlc.fieldName = "01_0.0";
        System.out.println(mlc.shaperFormat());
        
        System.out.println(mlc.egsnrcInitialFormat());
        System.out.println(mlc.egsnrcFormat());
    }
}
