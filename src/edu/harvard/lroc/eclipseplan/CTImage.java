/*
 * CTImage.java
 * 
 * Represents a CT image for conversion to EGS-acceptable format.
 * 
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
 * 
 * $Id: CTImage.java 253 2008-06-24 18:25:24Z dwchin $
 */
package edu.harvard.lroc.eclipseplan;

import java.io.*;
import java.math.*;
import java.util.logging.*;

import com.archimed.dicom.*;
import com.archimed.dicom.iioplugin.*;
import com.archimed.dicom.image.*;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import javax.imageio.spi.*;
import javax.imageio.ImageIO.*;
import javax.vecmath.*;

/**
 * Simple class encapsulating a CT image for translation to EGSnrc-compatible 
 * DICOM image.  EGSnrc expects no metadata.
 * 
 * @author David Chin
 * @version $Revision: 253 $
 */
public class CTImage implements Comparable<CTImage> {

    /**
     * Creates new instance of CTImage
     * @param patientID
     * @param ctImageFile
     * @param imagesDir
     * @param strippedImagesDir 
     */
    public CTImage(String patientID, File ctImageFile, File imagesDir, File strippedImagesDir) {

        this.patientID = patientID;
        this.ctImageFile = ctImageFile;
        this.imagesDir = imagesDir;
        this.strippedImagesDir = strippedImagesDir;

        try {
            this.read();

            if (debug_p) {
                System.out.println("CTImage: patientID = " + patientID);
                System.out.println("CTImage: ctImageFile = " + ctImageFile.toString());
                System.out.println("CTImage: imagesDir = " + imagesDir.toString());
                System.out.println("CTImage: strippedImagesDir = " + strippedImagesDir);
            }
        } catch (DicomException e) {
            Logger.getLogger(CTImage.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /** 
     *  Reads in CT image data. Really, we are interested only in
     *  the image location, identified by DICOM tag (0x0020, 0x0032)
     *  named ImagePositionPatient.
     * @throws DicomException
     * @throws IOException 
     */
    private void read() throws DicomException, IOException {
        if (debug_p) {
            System.out.println("Trying to read " + ctImageFile.toString());
        }
        
        FileInputStream fin = null;
        
        try {
            fin = new FileInputStream(ctImageFile);
            DicomReader dcmReader = new DicomReader();

            this.imageDcm = dcmReader.read(fin, true);
            
            
            // read voxel size
            BigDecimal x = new BigDecimal(0.);
            BigDecimal y = new BigDecimal(0.);
            BigDecimal z = new BigDecimal(0.);
            
            

            x = this.imageDcm.getBigDecimal(DDict.dPixelSpacing, 0);
            y = this.imageDcm.getBigDecimal(DDict.dPixelSpacing, 1);
            z = this.imageDcm.getBigDecimal(DDict.dSliceThickness, 0);

            this.voxelSize.set(x.doubleValue(), y.doubleValue(), z.doubleValue());
            
            // read image position
            x = this.imageDcm.getBigDecimal(DDict.dImagePositionPatient, 0);
            y = this.imageDcm.getBigDecimal(DDict.dImagePositionPatient, 1);
            z = this.imageDcm.getBigDecimal(DDict.dImagePositionPatient, 2);
            
            this.position.set(x.doubleValue(), y.doubleValue(), z.doubleValue());
            
            // read image size
            this.imageSize.setSize(this.imageDcm.getI(DDict.dRows), this.imageDcm.getI(DDict.dColumns));
            
            // read window center and width
            x = this.imageDcm.getBigDecimal(DDict.dWindowCenter, 0);
            y = this.imageDcm.getBigDecimal(DDict.dWindowWidth, 0);
            
            this.windowCenter = x.intValue();
            this.windowWidth = y.intValue();
            
            // read orientation
            x = this.imageDcm.getBigDecimal(DDict.dImageOrientationPatient, 0);
            y = this.imageDcm.getBigDecimal(DDict.dImageOrientationPatient, 1);
            z = this.imageDcm.getBigDecimal(DDict.dImageOrientationPatient, 2);
            
            this.orientation[0] = x.doubleValue();
            this.orientation[1] = y.doubleValue();
            this.orientation[2] = z.doubleValue();
            
            x = this.imageDcm.getBigDecimal(DDict.dImageOrientationPatient, 3);
            y = this.imageDcm.getBigDecimal(DDict.dImageOrientationPatient, 4);
            z = this.imageDcm.getBigDecimal(DDict.dImageOrientationPatient, 5);
            
            this.orientation[3] = x.doubleValue();
            this.orientation[4] = y.doubleValue();
            this.orientation[5] = z.doubleValue();
            
            // rescale parameters
            x = this.imageDcm.getBigDecimal(DDict.dRescaleIntercept, 0);
            y = this.imageDcm.getBigDecimal(DDict.dRescaleSlope, 0);
            
            this.rescaleIntercept = x.intValue();
            this.rescaleSlope = y.intValue();
            
            // anonymize
            this.imageDcm.set(DDict.dPatientName, "");
            
            // read image pixels
            DicomImageInfo dii = new DicomImageInfo(this.imageDcm);
            IIORegistry.getDefaultInstance().registerServiceProvider(new DicomImageReaderSpi());
            Iterator iter = javax.imageio.ImageIO.getImageReadersBySuffix("dcm");
            DicomImageReader imageReader = (DicomImageReader)iter.next();
            imageReader.setInput(this.imageDcm);
            this.image = imageReader.read(0);
            
            imageReader.dispose();
            
            if (debug_p) {
                System.out.println(dii.toString());
            }
            
        } catch (Exception e) {
            Logger.getLogger(CTImage.class.getName()).log(Level.SEVERE, null, e);
        } finally {
            try {
                if (fin != null)
                    fin.close();
            } catch (IOException e) {
                Logger.getLogger(Beam.class.getName()).log(Level.SEVERE, null, e);
            }

        }
    }

    /**
     * 
     * @return Patient ID
     */
    public String getPatientID() {
        return patientID;
    }

    /**
     * 
     * @return Window center
     */
    public Integer getWindowCenter() {
        return windowCenter;
    }

    /**
     * 
     * @return Window width
     */
    public Integer getWindowWidth() {
        return windowWidth;
    }
    
    /**
     * 
     * @return Rescale intercept
     */
    public Integer getRescaleIntercept() {
        return rescaleIntercept;
    }
    
    /**
     * 
     * @return Rescale slope
     */
    public Integer getRescaleSlope() {
        return rescaleSlope;
    }
    
    /** 
     * File containing CT image
     */
    private File ctImageFile;
    
    /**
     * Directory into which the original CT image is moved for backup.
     */
    private File imagesDir;
    
    /**
     * Directory for the DICOM CT image files, stripped of metadata.
     */
    private File strippedImagesDir;
    
    /**
     * Dicom object which holds CT image
     */
    private DicomObject imageDcm;
    
    /**
     * Image raster
     */
    private BufferedImage image;

    /**
     * 
     * @return image
     */
    public BufferedImage getImage() {
        return image.getSubimage(0, 0, image.getWidth(), image.getHeight());
    }
    
    /**
     * 
     * @param rect Rectangle specifying physical position and dimensions
     * @return Sub image, i.e. cropped image
     */
    public BufferedImage getSubImage(Rectangle2D.Double rect) {
        // compute pixel origin of rect
        int x0 = (int)((rect.x - this.getPosition().x) / this.voxelSize.x);
        int y0 = (int)((rect.y - this.getPosition().y) / this.voxelSize.y);
        
        // compute pixel dimensions of rect
        int w0 = (int)(rect.width / this.voxelSize.x);
        int h0 = (int)(rect.height / this.voxelSize.y);
        
        if (this.debug_p) {
            System.out.println(this.getClass().toString() + ".getSubImage(): " +
                    x0 + ", " + y0 + ", " + w0 + ", " + h0);
        }

        return image.getSubimage(x0, y0, w0, h0);
    }
    
    /**
     *  Patient ID number.
     */
    private String patientID;
    
    /** Z-coordinate of CT image */
    private Double zPosition;
    
    /** Position of CT image */
    private Point3d position = new Point3d();
    
    /** Orientation of CT image */
    private Double[] orientation = new Double[6];
    
    /** Voxel size */
    private Point3d voxelSize = new Point3d();
    
    /** Image size [rows, columns] */
    private Dimension imageSize = new Dimension();
    
    /** Window center */
    private Integer windowCenter;
    
    /** Window width */
    private Integer windowWidth;
    
    /** Rescale intercept */
    private Integer rescaleIntercept;
    
    /** Rescale slope */
    private Integer rescaleSlope;
    
    /** Debug flag */
    private boolean debug_p = false;
    
    /** Returns the position of the image. (ImagePositionPatient)
     * @return Position of image
     */
    public Point3d getPosition() {
        return (Point3d) position.clone();
    }
    
    /**
     * 
     * @return Physical size of image in mm
     */
    public Point3d getPhysicalSize() {
        Double x = this.imageSize.width * this.voxelSize.x;
        Double y = this.imageSize.height * this.voxelSize.y;
        Double z = this.voxelSize.z;
        
        return new Point3d(x, y, z);
    }

    /** 
     * Returns file associated with the CT image
     *
     * @return File associated with CT image slice
     */
    public File getCTImageFile() {
        return this.ctImageFile;
    }

    /** Returns array containing voxel size in cm
     * @return voxel size
     */
    public Point3d getVoxelSize() {
        return (Point3d) this.voxelSize.clone();
    }
    
    
    /**
     * 
     * @return Image size as [rows, columns] i.e. [width, height]
     */
    public Dimension getImageSize() {
        Dimension ret = new Dimension();

        try {
            ret.setSize(this.imageDcm.getInteger(DDict.dRows, 0),
                        this.imageDcm.getInteger(DDict.dColumns, 0));
        } catch (DicomException ex) {
            Logger.getLogger(CTImage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return ret;
    }
    
    /**
     * 
     * @return width of image in pixels
     */
    public int getWidth() {
        return this.getImageSize().width;
    }
    
    /**
     * 
     * @return height of image in pixels
     */
    public int getHeight() {
        return this.getImageSize().height;
    }
    

    /**
     * Backup CT image to imagesDir. Strip off metadata and write to strippedImagesDir.
     * @throws IOException 
     */
    public void writeStripped() throws IOException {
        try {
            // move original CT image into images directory
            if (!imagesDir.isDirectory()) {
                imagesDir.mkdir();
            }

            ctImageFile.renameTo(new File(imagesDir, ctImageFile.getName()));

            // write stripped CT image into stripped images directory
            if (!strippedImagesDir.isDirectory()) {
                strippedImagesDir.mkdir();
            }

            File newctImageFile = new File(strippedImagesDir, "MC_" + ctImageFile.getName());
            FileOutputStream newctImageStream = new FileOutputStream(newctImageFile);

            // write out CT image: use implicit little endian transfer syntax 
            // because that is what EGS's ctcreate utility expects; also,
            // do not write out the metadata
            imageDcm.write(newctImageStream, true, TransferSyntax.ImplicitVRLittleEndian, false);

        } catch (Exception e) {
            System.err.println("Exception in CTImage.writeStripped(): " + e.getMessage());
        }
    }

    /**
     * 
     * @param otherImg image to be compared to (using Z-coordinate)
     * @return -1 if "less than" otherImg, 0 if "equalt to" otherImg, 1 if "greater than" otherImg
     */
    public int compareTo(CTImage otherImg) {
        Double z1 = this.position.z;
        Double z2 = otherImg.getPosition().z;
        int ret = 0;
        
        if (z1 < z2)
            ret = -1;
        else if (z1 > z2)
            ret = 1;

        return ret;
    }
    
    @Override
    public int hashCode() {
        int result;
        long intermediate = Double.doubleToLongBits(this.position.z);
        result = (int) (intermediate ^ (intermediate >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CTImage other = (CTImage) obj;
        if (this.zPosition != other.zPosition && (this.zPosition == null || !this.zPosition.equals(other.zPosition))) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        String rep = "";
        rep += "File: " + this.ctImageFile.getAbsolutePath() + "; ";
        rep += "Position: " + this.position.toString() + "; ";
        rep += "Image size: (w=" + this.imageSize.width + ", h=" + this.imageSize.height + "); ";
        rep += "Voxel size: " + this.voxelSize.toString() + "; ";
        return rep;
    }
    
    
    /**
     * A simple test routine
     */
    private void doMain() {
        System.out.println("CTImage test");
        System.out.println("============");
        System.out.println("    " + this.toString());
    }

    /** Simple test program
     * 
     * @param args
     */
    static public void main(String[] args) {
        String patientID = "A068331";
        File planDir = new File("/home/dwchin/" + patientID + ".orig");
        File imgFile = new File(planDir, "CT_" + patientID + "_65.dcm");
        File imagesDir = new File(planDir, "images");
        File strippedImagesDir = new File(planDir, "MC_" + patientID);

        CTImage ctimage = new CTImage(patientID, imgFile, imagesDir, strippedImagesDir);
        try {
            ctimage.imageDcm.dumpVRs(System.out);
        } catch (IOException ex) {
            Logger.getLogger(CTImage.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            ctimage.doMain();
            ctimage.writeStripped();
            System.out.println(ctimage.toString());
            //System.out.println(ctimage.getImage().toString());
            //System.out.println(ctimage.getImage().getColorModel().toString());
            //System.out.println("hash code = " + ctimage.hashCode());
                        
            Rectangle2D.Double rect = new Rectangle2D.Double(-125., -125., 125., 125.);
            BufferedImage subimage = ctimage.getSubImage(rect);
            
            // expect the following to be 128, 128
            System.out.println("Subimage size: " + subimage.getWidth() + ", " + subimage.getHeight());
            
            // try with assym. rectangle
            rect.setRect(-45., 23., 169., 76.);
            subimage = ctimage.getSubImage(rect);
            System.out.println("Subimage size: " + subimage.getWidth() + ", " + subimage.getHeight());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    
}
