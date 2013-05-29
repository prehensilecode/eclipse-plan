/*
 * Patient.java
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

/* $Id: Patient.java 253 2008-06-24 18:25:24Z dwchin $ */

/*
 * A Patient is a complete exported Eclipse patient, which includes
 * a Plan file (containing at least one treatment or QA plan), a
 * Structure file, and all the CT image slices.
 */

package edu.harvard.lroc.eclipseplan;

import java.io.*;

import com.archimed.dicom.*;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Class aggregating Eclipse patient CT images, plan, and structures
 *
 * @author David Chin
 * @version $Revision: 253 $
 */
public class Patient {

    /**
     * Default constructor.
     */
    public Patient() {
        this.userDir = new File(System.getProperty("user.home"));
    }

    /**
     * Construct with given patient ID.
     * @param id Patient ID.
     */
    public Patient(String id) {
        this();
        this.id = id;
        this.planDir = new File(this.userDir, this.id);
        
        this.init();
    }

    public Patient(File planDir) {
        this();
        this.planDir = planDir;
        
        this.init();
    }
    
    /**
     * 
     * @return Directory in which the patient data is stored
     */
    public File getDataDir() {
        return planDir;
    }

    /**
     * 
     * @return First name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * 
     * @return Patient ID.
     */
    public String getId() {
        return id;
    }

    /**
     * 
     * @return Last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * 
     * @return Treatment/QA plan
     */
    public Plan getPlan() {
        return plan;
    }

    /**
     * 
     * @return Dicom object of the plan file
     */
    public DicomObject getPlanDcm() {
        return planDcm;
    }

    /**
     * 
     * @return Dicom object of all structures
     */
    public DicomObject getStructureDcm() {
        return this.phantom.getStructureDcm();
    }

    /**
     * 
     * @return File containing structure data
     */
    public File getStructureFile() {
        return this.phantom.getStructureFile();
    }

    /**
     * 
     * @return Structures defined for patient
     */
    public StructureSet getStructures() {
        return this.phantom.getStructures();
    }

    /**
     * 
     * @return CT image slices.
     */
    public CTImageList getCTSlices() {
        return ctSlices;
    }

    /**
     * 
     * @return phantom created from the CT slices
     */
    public Phantom getPhantom() {
        return phantom;
    }

    /** 
     * Anonymize identifying info.
     */
    public void anonymize() {
        this.lastName = this.id;
        this.firstName = "PATIENT";
    }

    /** Initialize */
    private void init() {
        //
        // read in plan
        //
        File[] planFiles = this.planDir.listFiles(new RPFilenameFilter());
        if (planFiles.length == 0) {
            System.out.println("ERROR");
            System.exit(1);
        }

        this.planFile = planFiles[0];

        this.readPlanDicom();

        //
        // read in structures
        //
        File[] structureFiles = this.planDir.listFiles(new RSFilenameFilter());
        if (structureFiles.length == 0) {
            System.out.println("ERROR");
            System.exit(2);
        }

        //
        // read in CT images
        //
        this.readCTImages();

        this.phantom = new Phantom(this.ctSlices);
    }
    
    /** Read in data from plan Dicom file */
    private void readPlanDicom() {
        this.plan = new Plan(this.planFile);
        this.planDcm = this.plan.getPlanDcm();
        try {
            this.id = this.planDcm.getS(DDict.dPatientID);
        } catch (DicomException ex) {
            Logger.getLogger(Patient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Read in all CT images */
    private void readCTImages() {
        this.ctSlices = new CTImageList(this.id);
    }

    /** Check consistency of Patient ID number */
    private boolean idConsistent_p() {
        // TODO: implement this
        return true;
    }

    /** Prompt user for desired structure, and resize if requested. */
    public void promptForStructureAndResize() {
        Integer structNum = 0;
        String structName = "";
        System.out.println("Tell me which structure you want to use for cropping: ");
        System.out.print("    Enter number (or 0 if no cropping desired): ");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
            structNum = Integer.decode(br.readLine());

            if (structNum != 0) {
                structName = this.getStructures().getNumberNameMap().get(structNum);
                System.out.println("You selected: " + structName);

                try {
                    this.phantom.resize(structName);
                } catch (Exception ex) {
                    Logger.getLogger(Patient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Patient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /** Print nice list of structures available */
    public void printStructureList() {
        TreeMap<String, Structure> structMap = this.phantom.getStructures().getStructureMap();
        String listitem = "";
        System.out.println("Available Structures:");
        System.out.println("---------------------");
        for (String structureName : structMap.navigableKeySet()) {
            if (structMap.get(structureName).getNumber() < 10) {
                listitem += " ";
            }
            listitem += structMap.get(structureName).getNumber() + ") ";
            listitem += structMap.get(structureName).getName();
            System.out.println(listitem);
            listitem = "";
        }
    }
    /** Patient ID. */
    private String id;
    /** Patient first name. */
    private String firstName;
    /** Patient last name. */
    private String lastName;
    /** Patient data dir. */
    private File planDir;
    /** Plan file. */
    private File planFile;
    /** Plan Dicom data. */
    private DicomObject planDcm;
    /** Plan(s) data. */
    private Plan plan;
    /** CT images. */
    private CTImageList ctSlices;
    /** Phantom created from CT slices */
    private Phantom phantom;
    /** User home directory */
    private File userDir;
    /** debug flag */
    private boolean debug_p = false;

    @Override
    public String toString() {
        // TODO: implement this
        StringBuffer strbuf = new StringBuffer();
        strbuf.append("Patient ID: " + this.id);
        
        return strbuf.toString();
    }

    /**
     * Simple test program.
     * @param args
     */
    public static void main(String[] args) {
        String patientID = "A047486";
        Patient p = new Patient(patientID);

        System.out.println(p.getPlan().toString());
        System.out.println(p.getStructures().toString());
        System.out.println(p.getCTSlices().toString());

        System.out.println(p.toString());
        p.printStructureList();

        p.promptForStructureAndResize();
    }
}
