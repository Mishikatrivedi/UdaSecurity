package com.udacity.catpoint.images.services;
import java.awt.image.BufferedImage;
/**
 * Hello world!
 */
public interface ImageService {
    boolean imageHasCat(BufferedImage image , float threshConfidence);
}