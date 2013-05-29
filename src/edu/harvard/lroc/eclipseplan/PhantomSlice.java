/* PhantomSlice.java */

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

/* $Id$ */

package edu.harvard.lroc.eclipseplan;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

/**
 * One slice of a phantom, corresponding to a CT slice.
 * @author David Chin
 * @version $Revision$
 */
public class PhantomSlice implements Comparable<PhantomSlice> {

    /**
     * Converts a CT image into a phantom slice
     * @param ctimage CT image to be converted
     */
    public PhantomSlice(CTImage ctimage) {
        MaterialMap.getInstance();
        this.position = ctimage.getPosition();
        this.size = ctimage.getImageSize();
        this.voxelSize = ctimage.getVoxelSize();

        // extract hounsfield data from the ctimage
        Raster ctRaster = ctimage.getImage().getData();
        DataBuffer hounsfieldData = ctRaster.getDataBuffer();
        SampleModel ctSM = ctRaster.getSampleModel();

        // for the material raster
        BandedSampleModel materialSM = new BandedSampleModel(DataBuffer.TYPE_USHORT,
                ctSM.getWidth(), ctSM.getHeight(), ctSM.getNumBands());
        DataBufferUShort materialDB = (DataBufferUShort) materialSM.createDataBuffer();

        // for the mass density raster
        BandedSampleModel rhoSM = new BandedSampleModel(DataBuffer.TYPE_FLOAT,
                ctSM.getHeight(), ctSM.getWidth(), ctSM.getNumBands());
        DataBufferFloat rhoDB = (DataBufferFloat) rhoSM.createDataBuffer();

        // compute material number and mass density data, and fill in the data buffers
        DensMat tmpdensmat = new DensMat();
        for (int i = 0; i < hounsfieldData.getSize(); ++i) {
            try {
                tmpdensmat = this.hounsToDens(hounsfieldData.getElem(i));
                if (this.debug_p) {
                    if (tmpdensmat.density <= 0.) {
                        System.out.println("BOO! zero density!");
                    }
                }
                materialDB.setElem(i, tmpdensmat.material);
                rhoDB.setElemDouble(i, tmpdensmat.density);
            } catch (Exception ex) {
                Logger.getLogger(PhantomSlice.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        // generate the rasters
        this.materialRaster = Raster.createRaster(materialSM, materialDB, null);
        this.densityRaster = Raster.createRaster(rhoSM, rhoDB, null);

        if (this.debug_p && false) {
            // moderate testing -- why is it, when i pass either raster out to 
            // another object, i cannot see any of the data?
            SampleModel sm = this.materialRaster.getSampleModel();
            DataBuffer db = this.materialRaster.getDataBuffer();
            int[] pixels = new int[sm.getWidth() * sm.getHeight()];
            sm.getPixel(0, 0, pixels, db);
            for (int i = 0; i < pixels.length; ++i) {
                if (pixels[i] > 0) 
                    System.out.println("non-zero pixel = " + pixels[i]);
            }
            
            // another way
            for (int i = 0; i < db.getSize(); ++i) {
                if (db.getElem(i) > 0) 
                    System.out.println("db elem > 0: " + db.getElem(i));
            }
        }
        
        // <editor-fold defaultstate="collapsed" desc=" debugging statements ">
        if (this.debug_p) {
            if (false) {
                // more extensive tests
                System.out.println("   data buffer types: TYPE_BYTE = " + DataBuffer.TYPE_BYTE);
                System.out.println("                      TYPE_USHORT = " + DataBuffer.TYPE_USHORT);
                System.out.println("                      TYPE_INT = " + DataBuffer.TYPE_INT);
                System.out.println("                      TYPE_SHORT = " + DataBuffer.TYPE_SHORT);
                System.out.println("                      TYPE_FLOAT = " + DataBuffer.TYPE_FLOAT);
                System.out.println("                      TYPE_DOUBLE = " + DataBuffer.TYPE_DOUBLE);
                System.out.println("                      TYPE_UNDEFINED = " + DataBuffer.TYPE_UNDEFINED);
                System.out.println("num. of elems in material raster = " + materialRaster.getNumDataElements());
                System.out.println("num. of elems in density raster = " + densityRaster.getNumDataElements());

                System.out.println("mat raster height = " + materialRaster.getHeight());
                System.out.println("mat raster width = " + materialRaster.getWidth());

                System.out.println("rho raster height = " + densityRaster.getHeight());
                System.out.println("rho raster width = " + densityRaster.getWidth());

                // check values of the pixels
                Integer[] pixelArray = new Integer[hounsfieldData.getSize()];
                for (int i = 0; i < pixelArray.length; ++i) {
                    pixelArray[i] = hounsfieldData.getElem(i);
                }
                TreeSet<Integer> sortedPixels = new TreeSet<Integer>(Arrays.asList(pixelArray));
                System.out.println("smallest Hounsfield value = " + sortedPixels.first());
                System.out.println("largest Hounsfield value = " + sortedPixels.last());

                // material data
                sortedPixels.clear();
                for (int i = 0; i < pixelArray.length; ++i) {
                    sortedPixels.add(materialDB.getElem(i));
                }
                System.out.println("smallest material number = " + sortedPixels.first());
                System.out.println("largest material number = " + sortedPixels.last());

                // mass density
                SortedSet<Float> densityPixels = new TreeSet<Float>();
                for (int i = 0; i < rhoDB.getSize(); ++i) {
                    densityPixels.add(rhoDB.getElemFloat(i));
                }
                System.out.println("smallest density = " + densityPixels.first());
                System.out.println("largest density = " + densityPixels.last());
            }
        }

// </editor-fold>
    }
    
    /** Debug flag */
    private boolean debug_p = false;
    
    /** Position */
    private Point3d position;
    
    /** Size of slice in number of pixels (width, height) == (x,y) */
    private Dimension size;

    /** 
     * 
     * @return size of slice in number of pixels (x,y)
     */
    public Dimension getSize() {
        return (Dimension) this.size.clone();
    }

    /** 
     * 
     * @return Position of slice
     */
    public Point3d getPosition() {
        return (Point3d) position.clone();
    }
    
    /** Size of a voxel */
    private Point3d voxelSize;

    /** 
     * 
     * @return voxel size
     */
    public Point3d getVoxelSize() {
        return (Point3d) voxelSize.clone();
    }
    
    /** Raster of mass density */
    private Raster densityRaster;
    
    /** Raster of material numbers */
    private Raster materialRaster;

    /**
     * Converts the pixel Hounsfield number to a mass density.
     * @param pixelHouns Hounsfield number of pixel
     * @param houns1 lower limit Hounsfield number of ramp
     * @param houns2 upper limit Hounsfield numbre of ramp
     * @param dens1 lower limit mass density of ramp
     * @param dens2 upper limit mass density of ramp
     * @return (pixelHouns - houns1) * (dens2 - dens1) / (houns2 - houns1)
     */
    private float ramp(Integer pixelHouns, Integer houns1, Integer houns2,
            Double dens1, Double dens2) {
        Double ret;

        Double denom = (double) houns2 - (double) houns1;
        Double num = dens2 - dens1;
        Double hounsOffset = (double) pixelHouns - (double) houns1;

        ret = hounsOffset * num / denom + dens1;

        return ret.floatValue();
    }

    /**
     * 
     * @param houns Hounsfield number
     * @return mass density and material number
     * @throws java.lang.Exception
     */
    private DensMat hounsToDens(Integer houns) throws Exception {
        // Make sure our MaterialMap has been instantiated:
        if (MaterialMap.get("AIR700ICRU") == null) {
            MaterialMap.getInstance();
        }

        DensMat densMat = new DensMat();

        // convert hounsfield numbers to density and material based
        // on a fixed map (or ramp) as per EGSnrc, which is piecewise linear

        // Hounsfield number range    Density range   Material
        // 1-50                       0.001-0.044     Air
        // 50-300                     0.044-0.302     Lung
        // 300-1125                   0.302-1.101     Tissue
        // 1125-3000                  1.101-2.088     Bone
        
        // XXX because actual patient CTs have Hounsfield numbers
        //     >3000, and I don't know how to deal with them, 
        //     we'll consider them bone. EGSnrc's ctcreate program
        //     only accepts numbers <=3000
        
        int airLo = 1;
        int airHi = 50;
        int lungLo = 50;
        int lungHi = 300;
        int tissueLo = 300;
        int tissueHi = 1125;
        int boneLo = 1125;
        int boneHi = 5000;
        
        double rhoAirLo = 0.001;
        double rhoAirHi = 0.044;
        double rhoLungLo = 0.044;
        double rhoLungHi = 0.302;
        double rhoTissueLo = 0.302;
        double rhoTissueHi = 1.101;
        double rhoBoneLo = 1.101;
        double rhoBoneHi = 3.1408;
        
        if (airLo <= houns && houns < airHi) {
            // air
            densMat.material = MaterialMap.get("AIR700ICRU");
            densMat.density = this.ramp(houns, airLo, airHi, rhoAirLo, rhoAirHi);
        } else if (lungLo <= houns && houns < lungHi) {
            // lung
            densMat.material = MaterialMap.get("LUNG700ICRU");
            densMat.density = this.ramp(houns, lungLo, lungHi, rhoLungLo, rhoLungHi);
        } else if (tissueLo <= houns && houns < tissueHi) {
            // tissue
            densMat.material = MaterialMap.get("ICRUTISSUE700ICRU");
            densMat.density = this.ramp(houns, tissueLo, tissueHi, rhoTissueLo, rhoTissueHi);
        } else if (boneLo <= houns && houns < boneHi) {
            // bone
            densMat.material = MaterialMap.get("ICRPBONE700ICRU");
            densMat.density = this.ramp(houns, boneLo, boneHi, rhoBoneLo, rhoBoneHi);
        } else {
            // out of bounds
            throw new Exception("Hounsfield number " + houns + " out of bounds.");
        }

        return densMat;
    }

    /**
     * 
     * @param rect rectangle specifiying crop of this slice
     * @return sub-slice specified by rect
     * @throws Exception 
     */
    public PhantomSlice getSubSlice(Rectangle2D.Float rect) throws Exception {
        throw new Exception("FOO");
    }

    /**
     * 
     * @return raster of mass density (g/cm^3)
     */
    public Raster getDensityRaster() {
        return this.densityRaster;
    }

    /**
     * 
     * @return raster of material ID numbers
     */
    public Raster getMaterialRaster() {
        return this.materialRaster;
    }

    /**
     * Resize the slice to the given rectangle. Rectangle specifies location 
     * and dimensions in mm.
     * @param newSize
     */
    public void resize(Rectangle2D.Float newSize) {
        // compute the cropping rectangle (location and size in number of pixels)
        Rectangle cropRect = new Rectangle();
        cropRect.setLocation((int)((newSize.x - this.position.x)/ this.voxelSize.x), 
                             (int)((newSize.y - this.position.y)/ this.voxelSize.y));
        cropRect.setSize((int)(newSize.width / this.voxelSize.x),
                         (int)(newSize.height / this.voxelSize.y));

        
        if (this.debug_p && false) {
            System.out.println("Before resize, raster bounds = " 
                    + this.densityRaster.getBounds().toString());
        }
        
        //
        // first, deal with material raster
        //
        
        // make an initialized (empty) raster of appropriate size
        WritableRaster tmpRast = this.materialRaster.createCompatibleWritableRaster(cropRect);
        DataBuffer tmpRastDB = tmpRast.getDataBuffer();
        SampleModel tmpRastSM = tmpRast.getSampleModel();
        
        // <editor-fold defaultstate="collapsed" desc="debug">
        if (this.debug_p) {
            System.out.println("Requested crop: " + newSize.toString());
            System.out.println("cropRect = " + cropRect.toString());
            int nGood = 0;
            DataBuffer matDB = this.materialRaster.getDataBuffer();
            System.out.println("matDB.getSize() = " + matDB.getSize());
            for (int i = 0; i < matDB.getSize(); ++i) {
                if (matDB.getElem(i) > 0) {
                    ++nGood;
                }
            }
            System.out.println("no. of good pixels in original material raster = " + nGood);
            
            nGood = 0;
            DataBuffer rhoDB = this.densityRaster.getDataBuffer();
            System.out.println("rhoDB.getSize() = " + rhoDB.getSize());
            for (int i = 0; i < rhoDB.getSize(); ++i) {
                if (rhoDB.getElem(i) > 0.) {
                    ++nGood;
                }
            }
            System.out.println("no. of good pixels in original density raster = " + nGood);
            
            System.out.println("tmpRastDB.getSize() = " + tmpRastDB.getSize());
            
            nGood = 0;
            for (int i = 0; i < tmpRastDB.getSize(); ++i) {
                if (tmpRastDB.getElem(i) > 0) {
                    ++nGood;
                }
            }
            System.out.println("PhantomSlice.resize(): no. of good pixels = " + nGood);
        }
        // </editor-fold>

        DataBuffer db = this.materialRaster.getDataBuffer();
        SampleModel sm = this.materialRaster.getSampleModel();
        
        short[] tmpmat = new short[cropRect.width * cropRect.height];
        sm.getDataElements(cropRect.x, cropRect.y, cropRect.width, cropRect.height, tmpmat, db);
        
        // <editor-fold defaultstate="collapsed" desc=" debug ">
        // check if tmpmat actually got filled with stuff
        if (this.debug_p) {
            System.out.println("tmpmat.length = " + tmpmat.length);
            int nGood = 0;
            for (int i = 0; i < tmpmat.length; ++i) {
                if (tmpmat[i] > 0) {
                    ++nGood;
                }
            }
            System.out.println("no. of good pixels in tmpmat = " + nGood);
        }
        // </editor-fold>
        
        tmpRastSM.setDataElements(0, 0, cropRect.width, cropRect.height, tmpmat, tmpRastDB);
        
        // <editor-fold defaultstate="collapsed" desc=" debug ">
        if (this.debug_p) {
            System.out.println("tmpRastDB.getSize() = " + tmpRastDB.getSize());
            int nGood = 0;
            for (int i = 0; i < tmpRastDB.getSize(); ++i) {
                if (tmpRastDB.getElem(i) > 0) {
                    ++nGood;
                }
            }
            System.out.println("no. of good pixels in resized tmp material raster  = " + nGood);
            System.out.println("tmpRast.getBounds().getLocation() = " + tmpRast.getBounds().getLocation().toString());
        }
        // </editor-fold>
        
        this.materialRaster = WritableRaster.createRaster(tmpRast.getSampleModel(), tmpRast.getDataBuffer(), tmpRast.getBounds().getLocation());
        
        // <editor-fold defaultstate="collapsed" desc=" debug ">
        if (this.debug_p) {
            db = this.materialRaster.getDataBuffer();
            sm = this.materialRaster.getSampleModel();
            int nGood = 0;
            for (int i = 0; i < sm.getWidth() * sm.getHeight(); ++i) {
                if (db.getElem(i) > 0) {
                    ++nGood;
                }
            }
            
            System.out.println("No. of good pixels in resized material raster = " + nGood);
        }
        // </editor-fold>
    
        
        //
        // then, deal with density raster
        //
        db = this.densityRaster.getDataBuffer();
        sm = this.densityRaster.getSampleModel();
        
        // make an initialized (empty) raster of the appropriate size
        tmpRast = this.densityRaster.createCompatibleWritableRaster(cropRect);
        tmpRastDB = tmpRast.getDataBuffer();
        tmpRastSM = tmpRast.getSampleModel();
        
        // fill tmp raster with cropped density data
        float[] tmprho = new float[tmpRastSM.getWidth() * tmpRastSM.getHeight()];
        sm.getDataElements(cropRect.x, cropRect.y, cropRect.width, cropRect.height, tmprho, db);
        
        // did tmprho get filled in right?
        if (this.debug_p) {
            int nGood = 0;
            for (int i = 0; i < tmprho.length; ++i) {
                if (tmprho[i] > 0.)
                    ++nGood;
            }
            System.out.println("no. of good pixels in tmprho = " + nGood);
        }
        
        tmpRastSM.setDataElements(0, 0, cropRect.width, cropRect.height, tmprho, tmpRastDB);
        this.densityRaster = WritableRaster.createRaster(tmpRast.getSampleModel(), tmpRast.getDataBuffer(), tmpRast.getBounds().getLocation());
        
        // <editor-fold defaultstate="collapsed" desc=" debug ">
        if (this.debug_p) {
            db = this.densityRaster.getDataBuffer();
            sm = this.densityRaster.getSampleModel();
            int nGood = 0;
            for (int i = 0; i < sm.getWidth() * sm.getHeight(); ++i) {
                if (db.getElem(i) > 0) {
                    ++nGood;
                }
            }
            
            System.out.println("No. of good pixels in resized density raster = " + nGood);
        }
        // </editor-fold>
        
        if (this.debug_p && false) {
            System.out.println("After resize, raster bounds = " + this.densityRaster.getBounds().toString());
        }
        
        this.size.setSize(this.densityRaster.getWidth(), this.densityRaster.getHeight());
        this.position.setX(newSize.x);
        this.position.setY(newSize.y);
    }
    
    /**
     * Resample the raster to the desired newPixelSize
     * @param newPixelSize desired new pixel size (in mm)
     */
    public void resample(Point2d newPixelSize) {
        // TODO implement
    }
    
    
    /**
     * 
     * @param otherSlice
     * @return -1 if "less than" otherSlice, 0 if "equal to" other slice, 1 if "greater than" otherSlice
     */
    public int compareTo(PhantomSlice otherSlice) {
        int result = 0;

        if (this.position.z < otherSlice.getPosition().z) {
            result = -1;
        } else if (this.position.z > otherSlice.getPosition().z) {
            result = 1;
        }

        return result;
    }

    /**
     * 
     * @return printable representation
     */
    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer("");
        ret.append("PhantomSlice -- position (mm): " + this.position.toString() + "; ");
        ret.append("size (mm): (" + this.size.width * this.voxelSize.x + ", " + 
                this.size.height * this.voxelSize.y + "); ");
        ret.append("dimensions (no. of voxels): " + this.size.toString() + "; ");
        ret.append("voxel size (mm): " + this.voxelSize.toString());

        return ret.toString();
    }

    /**
     * Convenience class to aggregate mass density and material number
     */
    private class DensMat {
        /**
         * Mass density (g/cm^3)
         */
        public float density = (float) 0.;
        
        /**
         * Material number
         */
        public int material = 0;

        @Override
        public String toString() {
            String ret = "DensMat: density = " + this.density +
                    "; material = " + this.material;
            return ret;
        }
    }

    /**
     * Simple test program
     * @param args commandline arguments
     */
    public static void main(String[] args) {
        CTImage ctimage = new CTImage("A047486", new File("/home/dwchin/A047486/CT_A047486_123.dcm"),
                new File("/home/dwchin/A047486/"), new File("/tmp"));
        PhantomSlice p = new PhantomSlice(ctimage);
        System.out.println(p.toString());
        
        Rectangle2D.Float newsize = new Rectangle2D.Float();
        newsize.setRect(-325., -325., 650., 650.);
        p.resize(newsize);
        System.out.println();
        System.out.println();
        newsize.setRect(0., 0., 320., 320.);
        p.resize(newsize);
        
        System.out.println(p.toString());
    }
}
