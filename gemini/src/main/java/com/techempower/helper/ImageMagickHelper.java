/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package com.techempower.helper;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;

import org.im4java.core.*;
import org.im4java.process.*;

import com.techempower.io.image.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides simple utility methods for dealing with images.
 */
public final class ImageMagickHelper 
  extends ImageHelper
{
  private final Logger log = LoggerFactory.getLogger(getClass());
  
  public ImageMagickHelper()
  {
    this.log.info("ImageMagickHelper instantiated.");
  }

  @Override
  public BufferedImage createBufferedImage(TransformParams transformParams, 
      ImageHelper.ImageTransform imageTransform)
  {
    return this.createBufferedImage((MagickParams) transformParams, (MagickTransform)imageTransform);
  }

  /**
   * Method of creating a BufferedImage object instead of doing
   * simpler IO.<br><br>
   * <strong>Note</strong>: when a BufferedImage is created from
   * lossy file types (i.e. JPEG), it is compressed at the time 
   * the Raster is set (construction) and the default compression 
   * is set to 75 (1-100).<br><br>
   * If different compression is desired, then do not use this
   * method or use lossless filetypes.
   * @see ImageMagickHelper#transformImage(
   *   ImageMagickHelper.MagickParams,
   *   ImageMagickHelper.MagickTransform)
   *
   * @return The transformed BufferedImage.
   */
  public BufferedImage createBufferedImage(MagickParams params,
      MagickTransform magickTransform)
  {
    try
    {
      byte[] outBytes = transformImage(params, magickTransform);
      if (outBytes == null)
      {
        return ImageIO.read(params.getDestFile());
      }

      return ImageIO.read(ImageIO.createImageInputStream(outBytes));
    }
    catch (IOException ioe)
    {
      this.log.warn("Exception while trying to transform image.", ioe);
    }
    return null;
  }

  /**
   * Takes a MagickParams object and calls *Magick based on the params and
   * imageTransform. IO is determined by presence of certain values in the
   * MagickParams object:
   * <ol>
   *  <li>If source file is present, its path is passed to *Magick (IO is
   *     done by *Magick, not Java; more efficient).</li>
   *  <li>If source file is not present, byte data is passed to *Magick via
   *     stdio streams (less efficient than #1).</li>
   *  <li>If destination file is present, its patch is passed to *Magick (IO
   *     is done by *Magick, not Java; more efficient).</li>
   *  <li>If destination file is not present, then the byte[] is returned from
   *     *Magick via stdio and must be handled by code (less efficient than
   *     #3 iff the byte[] will eventually be output to a file without any
   *     alteration).</li>
   * </ol>
   * Normally, this byte array is something to ultimately be written to disk; 
   * here is an example of how to do just that:<br>
   * 
   * <pre>
   * byte[] thumbBytes = ImageMagickHelper.transformImage(params, transform);
   * FileOutputStream fos = new FileOutputStream(outFile);
   * for(int b : thumbBytes)
   * {
   *   fos.write(b);
   * }</pre>
   *
   * @return The byte array of the transformed image iff no destination file
   * is specified.
   */
  public byte[] transformImage(MagickParams params,
      MagickTransform magickTransform)
  {
    ConvertCmd cmd;
    ByteArrayInputStream bais = null;
    try
    {
      GMOperation op = new GMOperation();

      // Input is source file if present, else tries to use bytes (stdin).
      if (params.getSourceFile() != null)
      {
        op.addImage(params.getSourceFile().getAbsolutePath());
      }
      else if(params.getImageFileData() != null)
      {
        op.addImage("-"); // input: stdin
      }

      // im4java omits null Integers from the parameters, so if we have a
      // newWidth or newHeight of 0, just leave their respective Integers
      // null.
      Integer w = magickTransform.getNewWidth();
      Integer h = magickTransform.getNewHeight();

      // Don't do any cropping or resizing if we have no dimensions.
      if (w != null || h != null)
      {
        if (!magickTransform.isPreserveAspect())
        {
          // Adjust aspect ratio as necessary to exactly reach desired
          // dimensions.
          op.resize(w, h, '!');
        }
        else if (magickTransform.isCropToFit())
        {
          // Resize until smallest side is smallest of w or h.
          op.resize(w, h, '^');

          // Try and center the cropping.
          int offsetY = 0;
          int offsetX = 0;
          
          Integer oldW = magickTransform.getOldWidth();
          Integer oldH = magickTransform.getOldHeight();

          if(oldW != null && oldH != null)
          {
            // We need to mathematically figure out the new width and height that
            // op.resize(w, h, '^') gives us for free.
            int    newWidth = w;
            int    newHeight = h;
            if( w < h )
            {
              if(oldH >= oldW)
              {
                newWidth = (int)(((double)oldW * (double)h) / (double)oldH);
              }
              if(oldH <= oldW)
              {
                newHeight = (int)(((double)oldH * (double)w) / (double)oldW);
              }
            }
            else if( h < w )
            {
              if(oldH >= oldW)
              {
                newHeight = (int)(((double)oldH * (double)w) / (double)oldW);
              }
              else if(oldH <= oldW)
              {
                newWidth = (int)(((double)oldW * (double)h) / (double)oldH);
              }
            }
            
            if(newWidth > w)
            {
              offsetX = (newWidth - w) / 2;
            }
            if(newHeight > h)
            {
              offsetY = (newHeight - h) / 2;
            }
          }
          
          // Now split the difference to "center" the crop
          op.crop(w, h, offsetX, offsetY);
        }
        else
        {
          // Resize until largest side is largest of w or h. Could be smaller
          // than desired dimensions.
          op.resize(w, h);
        }
      }

      // Flatten onto a white background image
      if (magickTransform.isFlatten())
      {
        op.flatten();
      }

      // JPEG quality settings can make a big difference.
      if (magickTransform.getQuality() != null)
      {
        op.quality(magickTransform.getQuality());
      }

      // Gets rid of any ICM, EXIF, IPTC, or other meta-data profiles to save
      // a bit more space.
      op.p_profile("*");

      // Output is stdout unless we write directly to a file for higher JPEG
      // quality.
      String outPath = "-"; // output: stdout
      if (params.getDestFile() != null)
      {
        outPath = params.getDestFile().getAbsolutePath();
      }

      // If no newFormat is specified, don't change the format.
      if (StringHelper.isEmptyTrimmed(magickTransform.getNewFormat()))
      {
        op.addImage(outPath);
      }
      else
      {
        op.addImage(magickTransform.getNewFormat().trim() + ":" + outPath);
      }

      // Set up command
      cmd = new ConvertCmd(params.usesGraphicsMagick());

      if(params.getSourceFile() == null)
      {
        // Pipe the fileData to stdin, to avoid writing to a file first.
        bais = new ByteArrayInputStream(params.getImageFileData());
        Pipe pipeIn = new Pipe(bais, null);
        cmd.setInputProvider(pipeIn);
      }
      
      ByteArrayOutputConsumer out = null;
      if(params.getDestFile() == null)
      {
        out = new ByteArrayOutputConsumer();
        cmd.setOutputConsumer(out); 
      }

      // Run the commend.
      cmd.run(op);

      if (out == null)
      {
        return null; // We wrote the output to dest, so nothing to return.
      }
      else
      {
        // Return the resulting image.
        return out.getRawBytes();
      }
    }
    catch (IOException | IM4JavaException | InterruptedException e)
    {
      this.log.warn("Exception while trying to transform image.", e);
    }
    finally
    {
      if (bais != null)
      {
        try
        {
          bais.close();
        }
        catch (IOException ioe)
        {
          this.log.warn("Exception trying to close input stream.", ioe);
        }
      }
    }
    return null;
  }

  @Override
  public byte[] transformImage(TransformParams params,
                               ImageHelper.ImageTransform imageTransform)
  {
    return transformImage((MagickParams)params, (MagickTransform)imageTransform);
  }

  /**
   * Describes a transformation to be done to an image. Passed as a parameter to
   * ImageMagickHelper.resizeImage(). This is broken out into a separate class so that
   * transformations can be defined once and reused in multiple places.
   *
   */
  public static class MagickTransform extends ImageHelper.ImageTransform
  {
    private Integer oldWidth;
    private Integer oldHeight;
    private boolean flatten        = false;

    /**
     * Constructor.
     *
     * @param newFormat The target format you'd like the image to end up in.
     * E.g., "jpg", "png".
     * @param newWidth Desired width. Ignored if less than 1.
     * @param newHeight Desired height. Ignored if less than 1.
     * @param preserveAspect If true, will not squish image. If image is not
     * square, then one of the dimensions will be shorter than requested.
     * @param cropToFit If true, the target dimensions will be reached by
     * cropping out the part of the image that exceeds them, after resizing.
     */
    /*public ImageTransform(String newFormat, int newWidth, int newHeight,
        boolean preserveAspect, boolean cropToFit)
    {
      this(newFormat, newWidth, newHeight, 0, 0, preserveAspect, cropToFit);
    }*/

    public MagickTransform(String newFormat, int newWidth, int newHeight,
                           int oldWidth, int oldHeight, boolean preserveAspect, boolean cropToFit)
    {
      super(newFormat, newWidth, newHeight, preserveAspect, cropToFit);
      this.oldWidth = oldWidth > 0 ? oldWidth : null;
      this.oldHeight = oldHeight > 0 ? oldHeight : null;
    }

    /**
     * @param newFormat The target format you'd like the image to end up in.
     * E.g., "jpg", "png".
     * @param newWidth Desired width. Ignored if less than 1.
     * @param newHeight Desired height. Ignored if less than 1.
     * @param preserveAspect If true, will not squish image. If image is not
     * square, then one of the dimensions will be shorter than requested.
     * @param cropToFit If true, the target dimensions will be reached by
     * cropping out the part of the image that exceeds them, after resizing.
     * @param flatten If true, the image(s) will compose on to a background
     * to form one single image. Default is onto a white canvas.
     */
    public MagickTransform(String newFormat, int newWidth, int newHeight,
                           boolean preserveAspect, boolean cropToFit, boolean flatten)
    {
      this(newFormat, newWidth, newHeight, 0, 0, preserveAspect, cropToFit,
          flatten);
    }

    public MagickTransform(String newFormat, int newWidth, int newHeight,
                           int oldWidth, int oldHeight, boolean preserveAspect, boolean cropToFit,
                           boolean flatten)
    {
        this(newFormat, newWidth, newHeight, oldWidth, oldHeight, preserveAspect, cropToFit);
        this.flatten = flatten;
    }

    public Integer getOldWidth()
    {
      return this.oldWidth;
    }

    public void setOldWidth(Integer oldWidth)
    {
      this.oldWidth = oldWidth;
    }

    public Integer getOldHeight()
    {
      return this.oldHeight;
    }

    public void setOldHeight(Integer oldHeight)
    {
      this.oldHeight = oldHeight;
    }

    public boolean isFlatten()
    {
      return this.flatten;
    }

    public void setFlatten(boolean flatten)
    {
      this.flatten = flatten;
    }
  }

  /**
   * Describes all the various parameters associated with *Magick execution.
   * Houses destination file, source file, source data, and whether Graphics
   * or ImageMagick should be used.
   */
  public static class MagickParams extends ImageHelper.TransformParams
  {
    private final boolean useGraphicsMagick;

    /**
     * Simple constructor that sets the imageFileData and specifies
     * whether GraphicsMagick or ImageMagick is to be used by ImageMagickHelper.
     */
    public MagickParams(byte[] imageFileData, boolean useGraphicsMagick)
    {
      super(imageFileData);
      this.useGraphicsMagick = useGraphicsMagick;
    }

    /**
     * Constructor that sets the imageFileData to be piped to *Magick
     * and specifies an output File destination to which *Magick will
     * output directly (no Java file-writing).
     */
    public MagickParams(byte[] imageFileData, File destFile,
        boolean useGraphicsMagick)
    {
      super(imageFileData, destFile);
      this.useGraphicsMagick = useGraphicsMagick;
    }

    /**
     * Constructor that sets the input and output files for *Magick to
     * use as input and output for the process.
     */
    public MagickParams(File sourceFile, File destFile,
        boolean useGraphicsMagick)
    {
      super(sourceFile, destFile);
      this.useGraphicsMagick = useGraphicsMagick;
    }

    /**
     * Constructor that sets the input file for *Magick to use. This
     * will tell <code>transformImage</code> to send the resulting
     * bytes from *Magick back to the application to be turned into
     * a byte array only.<br>
     * Note: nothing will be done with the byte[] from *Magick. This
     * must be handled in code directly.
     */
    public MagickParams(File sourceFile, boolean useGraphicsMagick)
    {
      super(sourceFile);
      this.useGraphicsMagick = useGraphicsMagick;
    }

    public boolean usesGraphicsMagick()
    {
      return useGraphicsMagick;
    }
  }
  
}  // End ImageMagickHelper.
