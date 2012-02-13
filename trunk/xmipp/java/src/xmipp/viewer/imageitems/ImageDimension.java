/***************************************************************************
 * Authors: Juanjo Vega     
 * 			J.M. de la Rosa Trevin (jmdelarosa@cnb.csic.es)
 *
 *
 * Unidad de  Bioinformatica of Centro Nacional de Biotecnologia , CSIC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307  USA
 *
 *  All comments concerning this program package may be sent to the
 *  e-mail address 'xmipp@cnb.csic.es'
 ***************************************************************************/

package xmipp.viewer.imageitems;

import ij.IJ;
import xmipp.jni.ImageGeneric;

/**
 * Simple class to store image dimensions
 */
public class ImageDimension {

    private int width, height, depth;
    private long nimages;

    public ImageDimension() {
    }

    public ImageDimension(int width, int height, int depth, long nimages) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.nimages = nimages;
    }

    public ImageDimension(ImageGeneric image) {
        try {
            width = image.getXDim();
            height = image.getYDim();
            depth = image.getZDim();
            nimages = image.getNDim();
        } catch (Exception ex) {
            IJ.error("Retrieving image dimensions: "+ image);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public long getNimages() {
        return nimages;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setNImages(long nimages) {
        this.nimages = nimages;
    }
}
