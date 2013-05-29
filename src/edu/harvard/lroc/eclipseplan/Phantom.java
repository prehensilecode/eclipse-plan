/*
 * Phantom.java
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

/* $Id: Phantom.java 255 2008-06-25 20:14:51Z dwchin $ */

package edu.harvard.lroc.eclipseplan;

import com.archimed.dicom.DicomObject;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.File;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.j3d.BoundingBox;
import javax.vecmath.Point3d;
import javax.vecmath.Point2d;

// TODO: add a resample method to resample to a coarser pixel size.


/**
 * This encapsulates the set of CT ctImages, and converts them to EGS egsphant
 * format if desired.
 * 
 * For now, only deal with regular voxelization in each dimension.
 * 
 * @author David Chin
 * @version $Revision: 255 $
 */
public class Phantom {
    public Phantom(CTImageList imageList) {
        this.ctImages = imageList;
        this.patientID = this.ctImages.getPatientID();
        this.planDir = this.ctImages.getPlanDir();
        
        try {
            this.init();
        } catch (CTImageListException ex) {
            Logger.getLogger(Phantom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Phantom(String patientID) {
        this(new CTImageList(patientID));
    }
    
    /** Copy constructor
     * @param source phantom to copy from
     */
    public Phantom(Phantom source) {
        this.ctImages = source.ctImages;
        
        // slice set
        this.sliceSet = new TreeSet<PhantomSlice>(source.sliceSet);
        
        // size
        for (int i = 0; i < 3; ++i) {
            this.size[i] = source.getSize()[i];
        }
        
    }
    
    /**
     * 
     * @param imagesDir directory containing CT images
     */
    public Phantom(File imagesDir) {
        this.ctImages = new CTImageList(imagesDir);
        this.patientID = this.ctImages.getPatientID();
        this.planDir = this.ctImages.getPlanDir();
        
        try {
            this.init();
        } catch (CTImageListException ex) {
            Logger.getLogger(Phantom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Read in CT images and construct phantom slices
     * @throws edu.harvard.lroc.eclipseplan.CTImageListException
     */
    private void init() throws CTImageListException {
        // generate the phantom slices from the CT slices
        for (CTImage ctimage : this.ctImages.getCTImageSet()) {
            this.sliceSet.add(new PhantomSlice(ctimage));
        }
        
        this.size[0] = this.ctImages.getImageSize().width;
        this.size[1] = this.ctImages.getImageSize().height;
        this.size[2] = this.ctImages.getNImages();
        
        // paranoid checking of slice sizes:
        Raster rast = this.sliceSet.first().getDensityRaster();
        SampleModel sm = rast.getSampleModel();
        if (sm.getWidth() != this.size[0] || sm.getHeight() != this.size[1]) {
            throw new CTImageListException();
        }
        
        //
        // read in structures
        //
        File[] structureFiles = this.planDir.listFiles(new RSFilenameFilter());
        if (structureFiles.length == 0) {
            System.out.println("ERROR");
            System.exit(2);
        }
        
        this.structureFile = structureFiles[0];
        
        this.readStructureDicom();
    }
    
    /** Read in data from structure Dicom file */
    private void readStructureDicom() {
        this.structures = new StructureSet(this.structureFile);
        this.structureDcm = this.structures.getStructureDcm();
    }
    
    
    /** size of phantom in number of voxels */
    private Integer[] size = new Integer[3];
    
    /**
     * 
     * @return number of voxels in each axis of the phantom
     */
    @SuppressWarnings("unchecked")
    public Integer[] getSize() {
        return this.size.clone();
    }
    
    
    /**
     * 
     * @return bounding box covering the whole phantom
     */
    public BoundingBox getBoundingBox() {
       Point3d lowerCorner = this.getPosition();
       Double dx = this.getSize()[0] * this.getVoxelSize().x;
       Double dy = this.getSize()[1] * this.getVoxelSize().y;
       Point3d upperCorner = this.sliceSet.last().getPosition();
       upperCorner.add(new Point3d(dx, dy, 0.));
       
       return new BoundingBox(lowerCorner, upperCorner);
    }
    
    /**
     * 
     * @return position of "first" corner of phantom volume
     */
    public Point3d getPosition() {
        return new Point3d(this.sliceSet.first().getPosition());
    }
    
    /**
     * 
     * @return size of voxels
     */
    public Point3d getVoxelSize() {
        return new Point3d(this.sliceSet.first().getVoxelSize());
    }
    
    public void resize(BoundingBox bbox) {
        Point3d lower = new Point3d();
        bbox.getLower(lower);
        
        Point3d upper = new Point3d();
        bbox.getUpper(upper);
        
        Rectangle2D.Float newSize = new Rectangle2D.Float();
        newSize.setRect(lower.x, lower.y, upper.x - lower.x, upper.y - lower.y);
        
        // need to toss out slices outside the z limits
        // need to crop the slices which remain
        TreeSet<PhantomSlice> newSliceSet = new TreeSet<PhantomSlice>();
        for (PhantomSlice slice : this.sliceSet) {
            // retain slices which are within the bouding box to the new slice set
            if (slice.getPosition().z >= lower.z && slice.getPosition().z <= upper.z) {
                // crop them
                slice.resize(newSize);
                
                // add them to the new slice set
                newSliceSet.add(slice);
            }
        }
        
        this.sliceSet.clear();
        this.sliceSet.addAll(newSliceSet);
        
        // set the new size info
        this.size[0] = this.sliceSet.first().getSize().width;
        this.size[1] = this.sliceSet.first().getSize().height;
        this.size[2] = this.sliceSet.size();
    }
    
    
    /**
     * Resize down to the size dictated by BoundingBox of struct
     * @param structureName name of structure which defines bounding box for resizing
     * @throws Exception 
     */
    public void resize(String structureName) throws Exception {
        // TODO implement
        
        // check that structure exists
        if (! this.structures.getStructureMap().containsKey(structureName)) {
            throw new Exception("No such structure: " + structureName);
        }
        
        BoundingBox bbox = this.structures.getStructureMap().get(structureName).getBoundingBox();
        
        this.resize(bbox);
    }
    
    
    // It is possible to resample in the z-axis, but a lot more complicated. So,
    // leave it for now.
    // TODO: maybe implement z-resampling in the future
    /**
     * Resample the phantom to have pixels of newPixelSize. Cannot resample
     * in the z-axis because we don't want to interpolate between CT slices.
     * @param newPixelSize desired new pixel size
     */
    public void resample(Point2d newPixelSize) {
        // TODO: implement
        
        // for each slice:
        //     resample raster of slice
    }
    
    
    /** plan directory */
    private File planDir;
    
    /** patient ID */
    private String patientID;
   
    /**
     * 
     * @return patient ID
     */
    public String getPatientID() {
        return this.patientID;
    }
    
    /**
     * 
     * @return number of slices which make up this phantom
     */
    public int getNSlices() {
        return this.sliceSet.size();
    }
    
    /**
     * 
     * @return Vector of BufferedImages
     * @throws CTImageListException 
     */
    @SuppressWarnings("unchecked")
    public Vector<BufferedImage> getImageSet() throws CTImageListException {
        return new Vector<BufferedImage>(this.ctImages.getImageList());
    }
    
    /** Set of ctImages which comprise the Phantom */
    private CTImageList ctImages;
    
    /** TreeSet of PhantomSlice objects */
    private TreeSet<PhantomSlice> sliceSet = new TreeSet<PhantomSlice>();

    /**
     * 
     * @return set of slices
     */
    @SuppressWarnings("unchecked")
    public NavigableSet<PhantomSlice> getSliceSet() {
        return (NavigableSet<PhantomSlice>) sliceSet.clone();
    }
    
    
    /** Structure file. */
    private File structureFile;
    
    /** Structures Dicom data. */
    private DicomObject structureDcm;
    
    /** Structures/ROIs. */
    private StructureSet structures;
    
    
    /**
     * 
     * @return Dicom object of all structures
     */
    public DicomObject getStructureDcm() {
        return structureDcm;
    }

    /**
     * 
     * @return File containing structure data
     */
    public File getStructureFile() {
        return structureFile;
    }

    /**
     * 
     * @return Structures defined for patient
     */
    public StructureSet getStructures() {
        return structures;
    }
    
    
    /** debug flag */
    private boolean debug_p = false;

    /** check that voxels have actual material associated */
    public void checkMaterialVoxels() {
        int nGood = 0;
        for (PhantomSlice slice : this.sliceSet) {
            Raster matrast = slice.getMaterialRaster();
            DataBuffer matdb = matrast.getDataBuffer();
            SampleModel matsm = matrast.getSampleModel();
            
            int width = matsm.getWidth();
            int height = matsm.getHeight();
            int total = width * height;
            int[] matpixels = new int[total];
            System.out.println("no. of pixels = " + total);
            matsm.getPixels(0, 0, width, height, matpixels, matdb);
            for (int i = 0; i < total; ++i) {
                if (matpixels[i] > 0) 
                    ++nGood;
            }
        }
        
        System.out.println("no. of good pixels = " + nGood);
    }
  
    /** Simple test program
     * @param args 
     */
    public static void main(String[] args) {
        CTImageList ctlist = new CTImageList("A085028");
        Phantom p = new Phantom(ctlist);
        System.out.println("no. of slices = " + p.getSize().toString());
        NavigableSet<PhantomSlice> slices = p.getSliceSet();
        System.out.println("no. of slices = " + slices.size());
        
        System.out.println("Structures: " + p.getStructures().getStructureMap().keySet());
        StructureSet structs = p.getStructures();
        //System.out.println("rkidney size: " + structs.getStructureMap().get("rkidney").getBoundingBox());
        
        try {
            System.out.println("phantom sizex: " + p.getSize()[0] + ", " + p.getSize()[1] + ", " + p.getSize()[2]);
            System.out.println("phantom position: " + p.getPosition());
            //p.checkMaterialVoxels();
            //p.resize("rkidney");
            
            // the following bounding box is for the dental MC
            // lower corner: (-7.29cm, -10.0cm, 1.03cm)
            // upper corner: (-0.61cm, -0.50cm, 4.03cm)
            // need it in mm.
            BoundingBox bbox = new BoundingBox(new Point3d(-72.9, -100., 10.3), new Point3d(-6.1, -5.0, 40.3));
            p.resize(bbox);
            System.out.println("phantom size, after resize: " + p.getSize()[0] + ", " + p.getSize()[1] + ", " + p.getSize()[2]);
            System.out.println("phantom position, after resize: " + p.getPosition());
            //p.checkMaterialVoxels();
            
            EgsPhant egsphant = new EgsPhant(p);
            egsphant.writeFile("A085028.egsphant");
        } catch (Exception ex) {
            Logger.getLogger(Phantom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
