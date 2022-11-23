package org.pygisserver.render.entity;

import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBicubic2;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.InterpolationNearest;

import org.apache.commons.lang3.StringUtils;

/**
 * @Description: 
 * @author:  
 * @date: 2021-03-17
 */
public enum PyInterpolationEnum {
	
	Nearest{
		public Interpolation getInterpolation() {
			return new InterpolationNearest(); 
		}
	},

    Bilinear{
		public Interpolation getInterpolation() {
			return new InterpolationBilinear(); 
		}
	},
    
    Bicubic{
		public Interpolation getInterpolation() {
			return new InterpolationBicubic2(0); 
		}
	};
	
	public abstract Interpolation getInterpolation();
	
	public static String NEAREST_INTERPOLATION = "nearest";
	
	public static String BILINEAR_INTERPOLATION = "bilinear";

	public static String BICUBIC_INTERPOLATION = "bicubic";
	
	
	public static Interpolation getInterpolationByInterStr(String interpostr) {
		if (StringUtils.isBlank(interpostr)) {
			interpostr = "nearest";
		}
		switch (interpostr) {
		case "nearest":
			return PyInterpolationEnum.Nearest.getInterpolation();
		case "bilinear":
			return PyInterpolationEnum.Bilinear.getInterpolation();
		case "bicubic":
			return PyInterpolationEnum.Bicubic.getInterpolation();
		default:
			return PyInterpolationEnum.Nearest.getInterpolation();
		}
	}

}
