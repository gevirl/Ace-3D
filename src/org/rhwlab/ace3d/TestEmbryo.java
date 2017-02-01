/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.util.Random;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.region.hypersphere.HyperSphere;
import net.imglib2.algorithm.region.localneighborhood.EllipsoidCursor;
import net.imglib2.algorithm.region.localneighborhood.EllipsoidNeighborhood;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

/**
 *
 * @author gevirl
 */
public class TestEmbryo {
    public TestEmbryo(){
		img
			= new ImagePlusImgFactory< UnsignedByteType >().create(
				new long[] { 256, 256, 256 }, new UnsignedByteType() );

		// draw a small sphere for every pixel of a larger sphere
		drawSpheres( img, 0, 255 );        
    }
    
	/**
	 * Draws a sphere that contains lots of small spheres into the center of the interval
	 *
	 * @param randomAccessible - the image data to write to
	 * @param minValue - the minimal intensity of one of the small spheres
	 * @param maxValue - the maximal intensity of one of the small spheres
	 */
	public < T extends RealType< T > > void drawSpheres(
		final RandomAccessibleInterval< T > randomAccessible,
		final double minValue, final double maxValue )
	{
		// the number of dimensions
		int numDimensions = randomAccessible.numDimensions();

		// define the center and radius
		Point center = new Point( randomAccessible.numDimensions() );
                long[] ellipseCenter = new long[numDimensions];
                
		long minSize = randomAccessible.dimension( 0 );

		for ( int d = 0; d < numDimensions; ++d )
		{
			long size = randomAccessible.dimension( d );

			center.setPosition( size / 2 , d );
			minSize = Math.min( minSize, size );
                        ellipseCenter[d] = minSize/2;
		}

		// define the maximal radius of the small spheres
		int maxRadius = 5;

		// compute the radius of the large sphere so that we do not draw
		// outside of the defined interval
		long radiusLargeSphere = minSize / 2 - maxRadius - 1;
                long[] ellipseRadii = new long[numDimensions];
                ellipseRadii[0] = (long)(1.0*radiusLargeSphere);
                for ( int d = 1; d < numDimensions; ++d ){
                    ellipseRadii[d] =   (long)(0.5*ellipseRadii[d-1]);
                }
                
		// instantiate a random number generator
		Random rnd = new Random( System.currentTimeMillis() );

		// define a hypersphere (n-dimensional sphere)
		HyperSphere< T > hyperSphere =
			new HyperSphere<T>( randomAccessible, center, radiusLargeSphere );
                EllipsoidNeighborhood<T> ellipse = new EllipsoidNeighborhood<T>(randomAccessible,ellipseCenter,ellipseRadii);
                
		// create a cursor on the hypersphere
//		HyperSphereCursor< T > cursor = hyperSphere.cursor();
                EllipsoidCursor <T> cursor = ellipse.cursor();
                double max = Double.MIN_VALUE;
		while ( cursor.hasNext() )
		{
			cursor.fwd();
                            double r = cursor.getDistanceSquared();
                            if (r > max ){
                                max = r;
                            } 
                      
			// the random radius of the current small hypersphere
			int radius = rnd.nextInt( maxRadius ) + 1;

			// instantiate a small hypersphere at the location of the current pixel
			// in the large hypersphere
			HyperSphere< T > smallSphere =
				new HyperSphere< T >( randomAccessible, cursor, radius );

			// define the random intensity for this small sphere
			double randomValue = rnd.nextDouble();

			// take only every 4^dimension'th pixel by chance so that it is not too crowded
                        
			if ( Math.round( randomValue * 100 ) % Util.pow( 8, numDimensions ) == 0 )
			{
				// scale to right range
				randomValue = rnd.nextDouble() * ( maxValue - minValue ) + minValue;

				// set the value to all pixels in the small sphere if the intensity is
				// brighter than the existing one
				for ( final T value : smallSphere )
					value.setReal( Math.max( randomValue, value.getRealDouble() ) );
			}
		}
	} 
        public ImagePlusImg getImage(){
            return this.img;
        }
        ImagePlusImg< UnsignedByteType, ?> img;
}
