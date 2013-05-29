/*
 *  This file is part of EclipsePlan.
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
   You should have received a copy of the GNU General Public License
    along with EclipsePlan.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * EgsPhant.java
 *
 * Created on May 18, 2007, 1:41 PM
 *
 * $Id: EgsPhant.java 262 2008-06-26 19:29:34Z dwchin $
 */

package edu.harvard.lroc.eclipseplan;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point3d;

// TODO: resample image to have 2.5mm cube voxels

/**
 * An EGSnrc-format phantom, derived from CT scans
 * @author dwchin
 */
public class EgsPhant {
    /**
     * Default constructor
     * @param patientID 
     */
    public EgsPhant(String patientID) {
        this.phant = new Phantom(patientID);
        
        this.init();
    }
    
    /**
     * 
     * @param phant 
     */
    public EgsPhant(Phantom phant) {
        this.phant = new Phantom(phant);
        
        this.init();
    }
    
    /**
     * 
     * @param patient Patient data
     */
    public EgsPhant(Patient patient) {
        this.phant = patient.getPhantom();
        
        this.init();
    }
    
    /**
     * Initialize stuff
     */
    private void init() {
        this.patientID = this.phant.getPatientID();
        this.size = this.phant.getSize();
        
        // put egsphant file in ~/patient_id/patient_id.egsphant
        this.phantfile = new File(System.getProperty("user.home")
                + "/" + this.patientID 
                + "/" + this.patientID + ".egsphant");
        
        // ensure the singleton MaterialMap has been instantiated
        MaterialMap.getInstance();
    }
    
    /**
     * 
     * @param egsphantfile
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    public void writeFile(File egsphantfile) throws IOException, Exception {
        if (!egsphantfile.canWrite()) {
            throw new IOException("no write permission");
        }
        
        this.phantfile = egsphantfile;
        
        this.writeFile();
    }
    
    /**
     * 
     * @param filename name of egsphant file to write to
     * @throws java.io.IOException
     * @throws Exception 
     */
    public void writeFile(String filename) throws IOException, Exception {
        // make sure filename has .egsphant suffix
        if (!filename.endsWith(".egsphant")) {
            filename = filename + ".egsphant";
        }
        
        // put egsphant file in current working directory
        this.phantfile = new File(System.getProperty("user.dir"), filename);
        
        if (!this.phantfile.canWrite()) {
            this.phantfile.setWritable(true, true);
        }
        
        this.writeFile();
    }
    
    /**
     * Write EGS-format phantom to file
     * @throws IOException
     * @throws Exception 
     */
    public void writeFile() throws IOException, Exception {
        if (this.phantfile == null) {
            throw new Exception("phantfile is not defined");
        }
        
        if (this.debug_p) {
            System.out.println("writing to file: " + this.phantfile.toString());
        }
        
        this.phantwriter = new PrintWriter(new FileWriter(this.phantfile), true);
        
        this.writeHeader();

        this.writeVoxelEdges();
        
        this.writeMaterialRaster();
        
        this.writeDensityRaster();
        
        this.phantwriter.close();
    }

   
    /**
     * write header of egsphant file
     */
    private void writeHeader() {
        // number of materials
        this.phantwriter.printf("%2d\n", MaterialMap.size());
        
        // list of materials -- must be in order of the material number
        for (String matname : MaterialMap.keySet())
            this.phantwriter.println(matname);
        
        // dummy estepe values, one for each material
        for (int i = 0; i < MaterialMap.size(); ++i) {
            this.phantwriter.printf("  %.7E", 1.0);
        }
        this.phantwriter.println();
    }
    
    private void writeVoxelEdges() {
        // voxel size/position data must be converted from mm to cm.
        if (this.debug_p) { System.out.println("writeVoxelEdges()..."); }
        
        if (this.debug_p) {
            System.out.println("       phantom size = " + this.size[0]
                               + ", " + this.size[1] + ", " + this.size[2]);
        }
        
        for (Integer n : this.size) {
            this.phantwriter.printf("%5d", n);
        }
        this.phantwriter.printf("\n");
        
        //
        // voxel edges -- no. of edges == no. of voxels + 1
        //
        Point3d edgeStart = this.phant.getPosition();
        Point3d voxelSize = this.phant.getVoxelSize();
        
        // x edges
        for (int i = 0; i < this.size[0] + 1; ++i) {
            this.phantwriter.printf("  % .6f    ", (edgeStart.x + i * voxelSize.x)/10.);
            if ((i+1) % write_width == 0)
                this.phantwriter.println();
        }
        this.phantwriter.println();
        
        // y edges
        for (int i = 0; i < this.size[1] + 1; ++i) {
            this.phantwriter.printf("  % .6f    ", (edgeStart.y + i * voxelSize.y)/10.);
            if ((i+1) % write_width == 0)
                this.phantwriter.println();
        }
        this.phantwriter.println();
        
        // z edges
        for (int i = 0; i < this.size[2] + 1; ++i) {
            this.phantwriter.printf("  % .6f    ", (edgeStart.z + i * voxelSize.z)/10.);
            if ((i+1) % write_width == 0)
                this.phantwriter.println();
        }
        this.phantwriter.println();
    }
    
    private void writeMaterialRaster() {
        if (this.debug_p) { System.out.println("writeMaterialRaster()..."); }
        
        Raster materialRaster;
        DataBuffer db;
        SampleModel sm;
        for (PhantomSlice slice : this.phant.getSliceSet()) {
            System.out.printf(".");  // progress indicator
            materialRaster = slice.getMaterialRaster();
            db = materialRaster.getDataBuffer();
            sm = materialRaster.getSampleModel();
                        
            short[] pixels = new short[db.getSize()];
            sm.getDataElements(0, 0, sm.getWidth(), sm.getHeight(), pixels, db);
            
            for (int j = 0; j < this.size[1]; ++j) {
                for (int i = 0; i < this.size[0]; ++i) {
                    this.phantwriter.printf("%d", pixels[i + j * this.size[0]]);
                }
                this.phantwriter.println();
            }
            this.phantwriter.println();
        }
        System.out.println(); // end progress indicators
    }
    
    private void writeDensityRaster() {
        if (this.debug_p) { System.out.println("writeDensityRaster()..."); }
        
        Raster densityRaster;
        DataBuffer db;
        SampleModel sm;
        for (PhantomSlice slice : this.phant.getSliceSet()) {
            System.out.printf(".");  // progress indicator
            densityRaster = slice.getDensityRaster();
            db = densityRaster.getDataBuffer();
            sm = densityRaster.getSampleModel();
            
            float[] pixels = new float[db.getSize()];
            sm.getDataElements(0, 0, sm.getWidth(), sm.getHeight(), pixels, db);
            for (int j = 0; j < this.size[1]; ++j) {
                for (int i = 0; i < this.size[0]; ++i) {
                    this.phantwriter.printf("  % .6f    ", pixels[i + j * this.size[0]]);
                    if ((i+1) % write_width == 0)
                        this.phantwriter.println();
                }
                this.phantwriter.println();
            }
            this.phantwriter.println();
        }
        System.out.println();  // end progress indicator
    }
    
    /** 
     * 
     * @return size of phantom in number of voxels (width, height, depth) == (x, y, z)
     */
    public Integer[] getSize() {
        return this.size.clone();
    }
    
    /**
     * 
     * @return absolute pathname of the phant file
     */
    public String getPhantFilename() {
        return this.phantfile.getAbsolutePath();
    }
    
    /** Associated phantom */
    private Phantom phant;
    
    /** egsphant file */
    private File phantfile;
    
    /** output stream for phantfile */
    private PrintWriter phantwriter;
    
    /** size of phantom in number of voxels in each axis */
    private Integer[] size;

    /** patient ID */
    private String patientID;
    
    /** width of row to use when writing out egsphant file */
    private static final int write_width = 5;
    
    /** debug flag */
    private boolean debug_p = false;
    
    /**
     * see if phantom has sensible data
     */
    public void checkData() {
        int sliceCounter = 0;
        for (PhantomSlice slice : this.phant.getSliceSet()) {
            // density
            Raster rast = slice.getDensityRaster();
            DataBuffer db = rast.getDataBuffer();
            int nGood = 0;
            for (int i = 0; i < db.getSize(); ++i) {
                if (db.getElem(i) > 0.) 
                    ++nGood;
            }
            System.out.println("no. of good density pixels in slice[" + sliceCounter + "] = " + nGood);
            
            // material
            rast = slice.getMaterialRaster();
            db = rast.getDataBuffer();
            nGood = 0;
            for (int i = 0; i < db.getSize(); ++i) {
                if (db.getElem(i) > 0) 
                    ++nGood;
            }
            System.out.println("no. of good material pixels in slice[" + sliceCounter + "] = " + nGood);
            ++sliceCounter;
        }
    }
    
    /**
     * simple test program
     * @param args
     */
    public static void main(String args[]) {
        EgsPhant ph = new EgsPhant("A047486");
        System.out.println("AIR700ICRU is material number " + MaterialMap.get("AIR700ICRU"));
        
        try {
            ph.checkData();
            ph.writeFile();
        } catch (Exception ex) {
            Logger.getLogger(EgsPhant.class.getName()).log(Level.SEVERE, null, ex);
        } 
            
    }

    
}
