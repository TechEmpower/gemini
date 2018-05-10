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

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Common interface for image scaler implementations. All methods will work 
 * on BufferedImage and byte arrays.
 */
public abstract class ImageHelper
{
  public abstract byte[] transformImage(TransformParams transformParams, 
      ImageTransform imageTransform);
  public abstract BufferedImage createBufferedImage(TransformParams transformParams, 
      ImageTransform imageTransform);

  /**
   * Describes a transformation to be done to an image. Passed as a parameter
   * to ImageHelper.resizeImage(). This is broken out into a separate class so
   * that transformations can be defined once and reused in multiple places.
   */
  public static class ImageTransform
  {
    private String  newFormat;
    private Integer newWidth;
    private Integer newHeight;
    private boolean preserveAspect = false;
    private boolean cropToFit      = false;
    private Double  quality;

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

    public ImageTransform(String newFormat, int newWidth, int newHeight,
                          boolean preserveAspect, boolean cropToFit)
    {
      this.newFormat = newFormat;
      this.newWidth = newWidth > 0 ? newWidth : null;
      this.newHeight = newHeight > 0 ? newHeight : null;
      this.preserveAspect = preserveAspect;
      this.cropToFit = cropToFit;
    }

    public String getNewFormat()
    {
      return this.newFormat;
    }

    public void setNewFormat(String newFormat)
    {
      this.newFormat = newFormat;
    }

    public Integer getNewWidth()
    {
      return this.newWidth;
    }

    public void setNewWidth(Integer newWidth)
    {
      this.newWidth = newWidth;
    }

    public Integer getNewHeight()
    {
      return this.newHeight;
    }

    public void setNewHeight(Integer newHeight)
    {
      this.newHeight = newHeight;
    }

    public boolean isPreserveAspect()
    {
      return this.preserveAspect;
    }

    public void setPreserveAspect(boolean preserveAspect)
    {
      this.preserveAspect = preserveAspect;
    }

    public boolean isCropToFit()
    {
      return this.cropToFit;
    }

    public void setCropToFit(boolean cropToFit)
    {
      this.cropToFit = cropToFit;
    }

    public Double getQuality()
    {
      return this.quality;
    }

    public void setQuality(Double quality)
    {
      this.quality = quality;
    }
  }

  public static class TransformParams
  {
    private byte[]  imageFileData;
    private File    sourceFile;
    private File    destFile;

    public TransformParams(byte[] imageFileData)
    {
      this.imageFileData = imageFileData.clone();
    }

    public TransformParams(byte[] imageFileData, File destFile)
    {
      this(imageFileData);
      this.destFile = destFile;
    }

    public TransformParams(File sourceFile, File destFile)
    {
      this.sourceFile = sourceFile;
      this.destFile = destFile;
    }

    public TransformParams(File sourceFile)
    {
      this.sourceFile = sourceFile;
    }

    public byte[] getImageFileData()
    {
      return this.imageFileData.clone();
    }
    
    public File getSourceFile()
    {
      return this.sourceFile;
    }
    
    public File getDestFile()
    {
      return this.destFile;
    }
  }
}
