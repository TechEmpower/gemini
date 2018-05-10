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

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.imageio.*;
import javax.imageio.plugins.jpeg.*;
import javax.imageio.stream.*;

import com.mortennobel.imagescaling.*;
import com.techempower.gemini.*;
import com.techempower.log.*;

/**
 * Provides a Pure JVM method for dealing with images.  A pure Java 
 * alternative to {@link ImageMagickHelper}.
 */
public final class JvmImageHelper 
  extends ImageHelper
{
  private final ResampleFilter    resampleFilter;
  private final GeminiApplication app;
  private final ComponentLog      log;

  public JvmImageHelper(GeminiApplication application) 
  {
    app = application;
    log = app.getLog("JvIH");
    resampleFilter = ResampleFilters.getLanczos3Filter();
  }

  /**
   * Mimics {@link ImageMagickHelper#transformImage}.
   *
   * Instead of using ImageMagick, uses a pure JVM implementation
   * with no external dependencies.
   *
   * @param transformParams An {@link com.techempower.helper.ImageHelper.TransformParams}
   *                        object containing input and output data
   * @param imageTransform An {@link com.techempower.helper.ImageHelper.ImageTransform}
   *                       object containing transform details.
   * @return The byte array of the transformed image iff no destination file
   * is specified.
   */
  @Override
  public byte[] transformImage(TransformParams transformParams, 
      ImageTransform imageTransform)
  {
    try
    {
      BufferedImage bufferedImage = createBufferedImage(transformParams, imageTransform);
      bufferedImage = transformImage(bufferedImage, imageTransform);
      try (OutputStream os = (transformParams.getDestFile() != null) 
          ? new FileOutputStream(transformParams.getDestFile())
          : new ByteArrayOutputStream()
          )
      {
        // Write the image
        writeToOutputStream(bufferedImage, os, imageTransform.getNewFormat());

        if (os instanceof ByteArrayOutputStream)
        {
          return ((ByteArrayOutputStream) os).toByteArray();
        }
      }
      finally
      {
        bufferedImage.flush();
      }
    }
    catch (IOException e)
    {
      this.log.log("Exception while trying to transform image.", LogLevel.ALERT, e);
    }
    return null;
  }

  private BufferedImage transformImage(BufferedImage bufferedImage, 
      ImageTransform imageTransform)
  {
    int newHeight = imageTransform.getNewHeight();
    int newWidth = imageTransform.getNewWidth();

    int width = bufferedImage.getWidth(null);
    int height = bufferedImage.getHeight(null);
    double widthToHeight = (double)width / (double)height;
    DimensionConstrain dimensionConstrain = DimensionConstrain
        .createAbsolutionDimension(newWidth, newHeight);

    CropParameters cropParameters = null;
    if (imageTransform.isCropToFit())
    {
      cropParameters = generateCropParameters(bufferedImage, imageTransform);
      dimensionConstrain = DimensionConstrain.createAbsolutionDimension(
          cropParameters.getNewWidth(), cropParameters.getNewHeight());
    }
    else if (imageTransform.isPreserveAspect()) // We can't have cropToFit AND preserveAspect
    {
      // determine the proper width and height to preserve the aspect ratio
      if (newHeight == 0)
      {
        newHeight = (int) (newWidth / widthToHeight);
      }
      else if (newWidth == 0)
      {
        newWidth = (int) (newHeight * widthToHeight);
      } else
      {
        if (widthToHeight >= 1.0D)
        {
          newHeight = (int) (newWidth / widthToHeight);
        }
        else
        {
          newWidth = (int) (newHeight * widthToHeight);
        }
      }

      dimensionConstrain = DimensionConstrain.createAbsolutionDimension(newWidth, newHeight);
    }

    // Resize the image
    ResampleOp imageOp = new ResampleOp(dimensionConstrain);
    imageOp.setFilter(resampleFilter);
    BufferedImage processed = imageOp.filter(bufferedImage, null);

    if (imageTransform.isCropToFit())
    {
      processed = processed.getSubimage(cropParameters.getOffsetX(), cropParameters.getOffsetY(), newWidth, newHeight);
    }

    return processed;
  }

  /**
   * Returns a CropParameters object to determine the height and width to resize and crop the image.
   * This method will find the dimensions that are smallest yet still preserve the aspect ratio of
   * the original image. Then it will compute the offsets of the image to crop to fit in the given dimensions
   * @return CropParameters object
   */
  private CropParameters generateCropParameters(BufferedImage bufferedImage, ImageTransform imageTransform)
  {
    int offsetY = 0;
    int offsetX = 0;
    int newWidth, newHeight;
    int expectedWidth = imageTransform.getNewWidth();
    int expectedHeight = imageTransform.getNewHeight();
    int oldWidth = bufferedImage.getWidth();
    int oldHeight = bufferedImage.getHeight();

    double originalAspectRatio = (double)oldWidth / (double)oldHeight;

    int potentialNewHeight = (int)(expectedWidth / originalAspectRatio);
    int potentialNewWidth = (int)(expectedHeight * originalAspectRatio);

    if (potentialNewHeight > expectedHeight)
    {
      newHeight = potentialNewHeight;
      newWidth = expectedWidth;
      offsetY = (newHeight - expectedHeight) / 2;
    }
    else // if (potentialNewWidth > expectedWidth)
    {
      newWidth = potentialNewWidth;
      newHeight = expectedHeight;
      offsetX = (newWidth - expectedWidth) / 2;
    }
    return new CropParameters(offsetX, offsetY, newWidth, newHeight);
  }

  @Override
  public BufferedImage createBufferedImage(TransformParams transformParams, 
      ImageTransform imageTransform)
  {
    Image theImage;
    if(transformParams.getSourceFile() != null)
    {
      theImage = Toolkit.getDefaultToolkit().createImage(transformParams
          .getSourceFile().getAbsolutePath());
    }
    else
    {
      theImage = Toolkit.getDefaultToolkit().createImage(transformParams
          .getImageFileData());
    }
    try
    {
      return correctImageColor(theImage);
    } catch (InterruptedException e)
    {
      this.log.log("Exception while trying to create buffered image.", LogLevel.ALERT, e);
    }
    return null;
  }

  private BufferedImage correctImageColor(Image theImage) throws InterruptedException
  {
    // Wait for the image to load
    MediaTracker tracker = new MediaTracker(new Canvas());
    tracker.addImage(theImage, 0);
    tracker.waitForID(0);

    // Convert to type RGB so that the color is accurate
    BufferedImage bufferedImage = new BufferedImage(theImage.getWidth(null), theImage.getHeight(null), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = bufferedImage.createGraphics();
    graphics2D.drawImage(theImage, 0, 0, null);
    graphics2D.dispose();
    theImage.flush();
    return bufferedImage;
  }

  /**
   * This method does not close the given output stream or flush the BufferedImage.
   * The caller is expected to do so.
   */
  private void writeToOutputStream(BufferedImage bufferedImage, OutputStream outputStream, String format) throws IOException
  {
    if (format.matches("jpe?g")) // All this trouble is only really worth it for jpegs
    {
      ImageWriter writer;
      Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
      if (writers.hasNext())
      {
        writer = writers.next();
      }
      else
      {
        throw new IllegalArgumentException("Invalid image format");
      }
      
      try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream))
      {
        writer.setOutput(ios);
        ImageWriteParam iwp = new JPEGImageWriteParam(Locale.getDefault());
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionQuality(1.0f);
        writer.write(null, new IIOImage(bufferedImage, null, null), iwp);
        ios.flush();
      }
      finally
      {
        writer.dispose();
      }
    }
    else
    {
      ImageIO.write(bufferedImage, format, outputStream);
    }
  }
  
  private class CropParameters
  {
    private final int offsetX;
    private final int offsetY;
    private final int newHeight;
    private final int newWidth;

    public int getOffsetX()
    {
      return offsetX;
    }

    public int getOffsetY()
    {
      return offsetY;
    }

    public int getNewHeight()
    {
      return newHeight;
    }

    public int getNewWidth()
    {
      return newWidth;
    }

    public CropParameters(int offsetX, int offsetY, int newWidth, int newHeight)
    {
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.newHeight = newHeight;
      this.newWidth = newWidth;
    }
  }
}