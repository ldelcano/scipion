/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package browser.imageitems;

import browser.Cache;
import browser.ICONS_MANAGER;
import browser.files.FileBrowser;
import browser.imageitems.listitems.FileItem;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import java.awt.Image;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import xmipp.Spider_Reader;

/**
 *
 * @author Juanjo Vega
 */
public abstract class ImageItem extends FileItem {

    public int width, height, nslices = 1;
    public int slice;
    public Cache cache;
    protected static Spider_Reader sr = new Spider_Reader();

    public ImageItem(File file, Cache cache) {
        this(file, cache, Spider_Reader.MID_SLICE);
    }

    public ImageItem(File file, Cache cache, int slice) {
        super(file);

        this.cache = cache;
        this.slice = slice;

        loadImageData();    // Loads size at start up.
    }

    protected void loadImageData() {
        if (!loadXmippImageData()) {
            if (!loadStandardImageData()) {
                IJ.error("File " + getFileName() + " can't be read.");
            }
        }
    }

    protected boolean loadStandardImageData() {
        try {
            // Tries to get a reader for the given file...
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            Iterator readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {
                // Gets the reader.
                ImageReader imgReader = (ImageReader) readers.next();

                // Creates input stream to read.
                ImageInputStream imageInputStream = ImageIO.createImageInputStream(new BufferedInputStream(new FileInputStream(file)));
                imgReader.setInput(imageInputStream);

                // Stores image info.
                width = imgReader.getWidth(0);
                height = imgReader.getHeight(0);

                // Closes input stream.
                imageInputStream.close();
            }
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    protected boolean loadXmippImageData() {
        try {
            sr.parseSPIDER(getDirectory(), getFileName());

            FileInfo fi = sr.getFileInfo();

            // Stores image info.
            width = fi.width;
            height = fi.height;
            nslices = fi.nImages;
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    protected abstract String getKey();

    @Override
    public ImagePlus getPreview(int w, int h) {
        ImagePlus preview;

//        System.out.println("[" + getFileName() + "] w: " + width + " / h: " + height);

        if (width > 0 && height > 0) {
            // Tries to load from cache.
            preview = (ImagePlus) cache.get(getKey());

            // If not in cache.
            if (preview == null) {
                // Tries to load it as xmipp image...
                if (isXmippFile(file)) {
                    preview = loadXmippPreview(w, h, slice);
                } else {    // ...otherwise, opens it as a normal one.
                    preview = getStandardPreview(w, h);
                }

                if (preview != null) {
                    cache.put(getKey(), preview);
                }
            }
        } else {    // Null preview.
            preview = new ImagePlus("", ICONS_MANAGER.MISSING_ITEM.getImage());
        }

        return preview;
    }

    protected static boolean isXmippFile(File file) {
        if (FileBrowser.isSpiderImage(file) || FileBrowser.isSelFile(file)) {
            return true;
        }

        return false;
    }

    protected ImagePlus getStandardPreview(int w, int h) {
        Image preview = null;

        try {
            // Tries to get a reader for the given file...
            ImageInputStream iis = ImageIO.createImageInputStream(file);
            Iterator readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {
                // Gets the reader.
                ImageReader imgReader = (ImageReader) readers.next();

                // Creates input stream to read.
                ImageInputStream imageInputStream = ImageIO.createImageInputStream(new BufferedInputStream(new FileInputStream(file)));
                imgReader.setInput(imageInputStream);

                // Calculates and sets parameters for subsampling.
                int longEdge = (Math.max(width, height));
                int subSampleX = (int) (longEdge / w * 2.f);
                int subSampleY = (int) (longEdge / h * 2.f);

                final ImageReadParam readParam = imgReader.getDefaultReadParam();

                if (subSampleX > 1) {
                    readParam.setSourceSubsampling(subSampleX, subSampleY, 0, 0);
                }

                preview = imgReader.read(0, readParam);

                // Closes input stream.
                imageInputStream.close();
            }
        } catch (Exception ex) {
        }

        return preview != null ? new ImagePlus("", preview) : null;
    }

    protected ImagePlus loadXmippPreview(int w, int h, int slice) {
        ImagePlus ip = null;

        try {
            loadImageData();

            ip = sr.loadThumbnail(getDirectory(), getFileName(), w, h, slice);
        } catch (Exception e) {
            e.printStackTrace();
            IJ.write(e.getMessage());
        }

        return ip;
    }

    public ImagePlus[] loadPreviews(int w, int h) {
        ImagePlus previews[] = null;

        try {
            loadImageData();

            previews = sr.loadVolumeSlices(file.getParent(), getFileName(), w, h);
        } catch (Exception e) {
            e.printStackTrace();
            IJ.write(e.getMessage());
        }

        return previews;
    }
}
