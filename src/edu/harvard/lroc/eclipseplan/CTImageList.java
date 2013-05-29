/* CTImageList.java
 * A list of CT images
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
 * $Id: CTImageList.java 253 2008-06-24 18:25:24Z dwchin $
 */

package edu.harvard.lroc.eclipseplan;

import java.awt.Dimension;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.awt.image.BufferedImage;


/**  A list of CT images
 * @author David Chin
 * @version $Revision: 253 $
 */
public class CTImageList {
    
    /**
     * 
     * @param patientID Patient ID
     * @param planDir   Exported plan directory
     * @param imagesDir Directory containing CT images
     * @param strippedImagesDir Directory which will contain the stripped CT images
     */
    public CTImageList(String patientID, File planDir, File imagesDir, File strippedImagesDir) {
        this.patientID = patientID;
        this.planDir = planDir;
        this.imagesDir = imagesDir;
        this.strippedImagesDir = strippedImagesDir;
        
        initImagesDirsAndReadCT();
        
        try {
            writeFileList();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public CTImageList(String patientID, String planDir, String imagesDir, String strippedImagesDir) {
        this(patientID, new File(planDir), new File(imagesDir), new File(strippedImagesDir));
    }
    
    public CTImageList(String patientID, File planDir, File strippedImagesDir) {
        this(patientID, planDir, new File(planDir, "images"), strippedImagesDir);
    }
    
    public CTImageList(String patientID, File planDir) {
        // sets default images dir, and strippedImagesDir
        this(patientID, planDir, new File(planDir, "images"), new File(planDir, "MC_" + patientID));
    }
    
    public CTImageList(String patientID) {
        this(patientID, new File(System.getProperty("user.home"), patientID));
    }
    
    public CTImageList(File planDir) {
        this("000000A", planDir, new File(planDir, "images"), new File(planDir, "stripped"));
    }
    
    /** Copy constructor
     * @param source CT image list to copy from 
     */
    public CTImageList(CTImageList source) {
        this(source.patientID, source.planDir, source.imagesDir, source.strippedImagesDir);
    }
    
    
    /**
     * Creates images and stripped images directories, if needed. Reads
     * in the CT images.
     */
    private void initImagesDirsAndReadCT() {
        if (!this.planDir.isDirectory()) {
            System.err.println("CTImagesList: plan directory " + this.planDir.getAbsolutePath() +
                    " does not exist.");
            System.exit(5);
        }
        
        if (debug_p) {
            System.out.println("patientID = " + patientID);
            System.out.println("imagesDir = " + imagesDir.toString());
            System.out.println("strippedImagesDir = " + strippedImagesDir.toString());
        }
        
        this.imagesDir.mkdir();
        this.strippedImagesDir.mkdir();
            
        this.namesfile = new File(strippedImagesDir, "File_names");
       
        File[] listing = planDir.listFiles(new CTFilenameFilter());
        
        // add CT images to the map of CT locations and files
        CTImage ctimage = null;
        for (int i = 0; i < listing.length; ++i) {
            if (debug_p) {
                System.out.printf("listing[%3d] = %s\n", i, listing[i].toString());
            }
            
            ctimage = new CTImage(this.patientID, listing[i],
                    this.imagesDir, this.strippedImagesDir);
            
            this.ctLocationsAndFiles.put(ctimage.getPosition().z,
                    ctimage.getCTImageFile());
            
            this.ctImageSet.add(ctimage);
            
            try {
                ctimage.writeStripped();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        this.nImages = this.ctImageSet.size();
        this.imageSize = this.ctImageSet.first().getImageSize();
        
        try {    
            this.writeFileList();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    
    /** 
     *  Writes the list of CT image files, sorted by Z-position. The filename
     *  is hardcoded to be "File_names". This may or may not change in the 
     *  future.
     */
    private void writeFileList() throws IOException {
        if (debug_p)
            System.out.printf("SortedMap has %d entries\n", ctLocationsAndFiles.size());
  
        try {
            PrintWriter namesfileWriter = new PrintWriter(new FileWriter(namesfile));
            
            for (SortedMap.Entry<Double, File> entry : ctLocationsAndFiles.entrySet()) 
                namesfileWriter.println("MC_" + entry.getValue().getName());
            
            namesfileWriter.flush();
            namesfileWriter.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
    
    
    /**
     * Returns a String with formatted information about this object.
     * 
     * @return printable information about the list of images
     */
    @Override
    public String toString() {
        String retval;
        
        retval  = "CTImageList Info (TBA)\n";
        retval += "======================\n";
        retval += "Patient ID: " + this.patientID + "\n";
        retval += "Images dir: " + this.getImagesDir().toString() + "\n";
        retval += "Stripped images dir: " + this.getStrippedImagesDir().toString() + "\n";
        retval += "File list: " + this.namesfile + "\n";
        retval += "No. of images: " + this.ctLocationsAndFiles.size() + "\n";
        
        return retval;
    }
    


    /**
     * 
     * @return map of CT image files, keyed by Z-coordinate
     */
    @SuppressWarnings("unchecked")
    public SortedMap<Double, File> getCtLocationsAndFiles() {
        return (SortedMap<Double, File>) ctLocationsAndFiles.clone();
    }

    /**
     * 
     * @return sorted set of CT images
     */
    @SuppressWarnings("unchecked")
    public SortedSet<CTImage> getCTImageSet() {
        return (SortedSet<CTImage>) ctImageSet.clone();
    }

    
    /**
     * 
     * @return list of BufferedImage images of the CT
     * @throws edu.harvard.lroc.eclipseplan.CTImageListException
     */
    public List<BufferedImage> getImageList() throws CTImageListException {
        Vector<BufferedImage> imgSet = new Vector<BufferedImage>(this.nImages);
        
        for (CTImage img : this.ctImageSet.tailSet(this.ctImageSet.first())) {
            imgSet.add(img.getImage());
        }
        
        if (this.debug_p) {
            System.out.println("CTImageList: nImages = " + this.nImages);
            System.out.println("CTImageList: imgSet.size() = " + imgSet.size());
        }
        
        if (imgSet.size() != this.nImages) {
            throw new CTImageListException("Mismatch in number of images");
        }
        
        return (List<BufferedImage>) imgSet;
    }
    
    /**
     * 
     * @return file containing sorted list of CT image filenames
     */
    public File getNamesfile() {
        return namesfile;
    }

    /**
     * 
     * @return patient ID
     */
    public String getPatientID() {
        return patientID;
    }

    /**
     * 
     * @return plan directory
     */
    public File getPlanDir() {
        return planDir;
    }
    
    
    /** Number of images */
    private int nImages;
    
    /** 
     * 
     * @return number of images 
     */
    public int getNImages() {
        return this.nImages;
    }
    
    /** size of images in pixels */
    private Dimension imageSize;
    
    /**
     * 
     * @return size of every image in pixels
     */
    public Dimension getImageSize() {
        return (Dimension) this.imageSize.clone();
    }
    
    /** 
     *  A SortedMap of CT scan Z-coordinates and corresponding files. We want to 
     *  sort on the Z-coordinate.
     */
    private TreeMap<Double, File> ctLocationsAndFiles = new TreeMap<Double, File>();
    
    /**
     * A TreeSet of CT images, sorted by Z-coordinate.
     */
    private TreeSet<CTImage> ctImageSet = new TreeSet<CTImage>();
    
    /**
     * User's home directory
     */
    private File userHome = new File(System.getProperty("user.home"));

    /** 
     * Exported plan directory
     */
    private File planDir; 
    
    /**
     * Images directory, into which the original CT images are moved.
     */
    private File imagesDir;
    
    /**
     * Gives the directory where the original DICOM CT images are located.
     *
     * @return Directory where original DICOM CT images are located.
     */
    public File getImagesDir() {
        return this.imagesDir;
    }
    
    /**
     * Directory for the image files, stripped of DICOM metadata. EGSnrc's
     * DICOM reader cannot handle metadata.
     */
    private File strippedImagesDir;
    
    /**
     * Gives the directory where the processed/stripped DICOM CT images
     * are located.
     *
     * @return Directory where processed/stripped DICOM CT images are located.
     */
    public File getStrippedImagesDir() {
        return this.strippedImagesDir;
    }
    
    /** 
     * File containing CT image filenames sorted by Z
     */
    private File namesfile; 

    /**
     *  Patient ID number.
     */
    private String patientID;

    /**
     * Debug flag.
     */
    private boolean debug_p = false;
    

    /**
     * Simple test program.
     * @param args Commandline arguments
     */
    public static void main(String[] args) {
        boolean debug_p = false;
        CTImageList ctimagelist = null;
        Vector<BufferedImage> imgSet = null;
        
        if (debug_p) {
            System.out.println("Args: ");
            for (String s : args)
                System.out.println(s);
        }
        
        if (args.length == 0) {
            // simple test case for debugging
            ctimagelist = new CTImageList("A086160");
        } else {
            ctimagelist = new CTImageList(args[0]);
        }
        
        try {
            imgSet = (Vector<BufferedImage>) ctimagelist.getImageList();
        } catch (CTImageListException ex) {
            Logger.getLogger(CTImageList.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        for (int i = 0; i < imgSet.size(); ++i) {
            System.out.println(imgSet.get(i).toString());
        }
        
        if (debug_p) {
            System.out.println(ctimagelist.toString());
            
        }
    }
}
