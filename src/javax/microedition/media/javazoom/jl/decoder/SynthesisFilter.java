/*
 * 11/19/04 1.0 moved to LGPL.
 * 
 * 04/01/00 Fixes for running under build 23xx Microsoft JVM. mdm.
 * 
 * 19/12/99 Performance improvements to compute_pcm_samples().  
 *			Mat McGowan. mdm@techie.com. 
 *
 * 16/02/99 Java Conversion by E.B , javalayer@javazoom.net
 *
 *  @(#) synthesis_filter.h 1.8, last edit: 6/15/94 16:52:00
 *  @(#) Copyright (C) 1993, 1994 Tobias Bading (bading@cs.tu-berlin.de)
 *  @(#) Berlin University of Technology
 *
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */
package javazoom.jl.decoder;

import java.io.IOException;

/**
 * A class for the synthesis filter bank.
 * This class does a fast downsampling from 32, 44.1 or 48 kHz to 8 kHz, if ULAW is defined.
 * Frequencies above 4 kHz are removed by ignoring higher subbands.
 */
final class SynthesisFilter
{
  private float[] 			 v1;
  private float[]		 	 v2;
  private float[]			 actual_v;			// v1 or v2
  private int 			 	 actual_write_pos;	// 0-15
  final float[]			     samples  = new float[32];	// 32 new input subband samples
  private final int		     channel;
	
	/**
	 * Quality value for controlling CPU usage/quality tradeoff. 
	 */
	/*
	private int				quality;
	
	private int				v_inc;
	
	
	
	public static final int	HIGH_QUALITY = 1;
	public static final int MEDIUM_QUALITY = 2;
	public static final int LOW_QUALITY = 4;
	*/
	
  /**
   * Contructor.
   * The scalefactor scales the calculated float pcm samples to short values
   * (raw pcm samples are in [-1.0, 1.0], if no violations occur).
   */
  public SynthesisFilter(int channelnumber)
  {  	 
	  d16 = splitArray(d, 16);
	  
	 v1 = new float[512];
	 v2 = new float[512];
     channel = channelnumber;
	 //setQuality(HIGH_QUALITY);
	 
     reset();
  }

	/*
	private void setQuality(int quality0)
	{
	  	switch (quality0)
	  	{		
		case HIGH_QUALITY:
		case MEDIUM_QUALITY:
		case LOW_QUALITY:						  
			v_inc = 16 * quality0;			
			quality = quality0;
			break;	
		default :
			throw new IllegalArgumentException("Unknown quality value");
	  	}				
	}
	
	public int getQuality()
	{
		return quality;	
	}
	*/
  
  /**
   * Reset the synthesis filter.
   */
  public void reset()
  {
     //float[] floatp;
	 // float[] floatp2;

     // initialize v1[] and v2[]:
     //for (floatp = v1 + 512, floatp2 = v2 + 512; floatp > v1; )
	 //   *--floatp = *--floatp2 = 0.0;
	 for (int p=0;p<512;p++) 
		 v1[p] = v2[p] = 0.0f;

     // initialize samples[]:
     //for (floatp = samples + 32; floatp > samples; )
	 //  *--floatp = 0.0;
	 for (int p2=0;p2<32;p2++) 
		 samples[p2] = 0.0f;

     actual_v = v1;
     actual_write_pos = 15;
  }


  /**
   * Inject Sample.
   */
  public void input_sample(float sample, int subbandnumber)
  {	 	 		  
	  samples[subbandnumber] = sample;
  }
  
  /**
	 * Compute new values via a fast cosine transform.
	 */
	private void compute_new_v()
	{	  
		final float new_v0, new_v1, new_v2, new_v3, new_v4, new_v5, new_v6, new_v7, new_v8, new_v9;
		final float new_v10, new_v11, new_v12, new_v13, new_v14, new_v15, new_v16, new_v17, new_v18, new_v19;
		final float new_v20, new_v21, new_v22, new_v23, new_v24, new_v25, new_v26, new_v27, new_v28, new_v29;
		final float new_v30, new_v31;

		final float[] s = samples;

		final float s0 = s[0];
		final float s1 = s[1];
		final float s2 = s[2];
		final float s3 = s[3];
		final float s4 = s[4];
		final float s5 = s[5];
		final float s6 = s[6];
		final float s7 = s[7];
		final float s8 = s[8];
		final float s9 = s[9];
		final float s10 = s[10];	
		final float s11 = s[11];
		final float s12 = s[12];
		final float s13 = s[13];
		final float s14 = s[14];
		final float s15 = s[15];
		final float s16 = s[16];
		final float s17 = s[17];
		final float s18 = s[18];
		final float s19 = s[19];
		final float s20 = s[20];	
		final float s21 = s[21];
		final float s22 = s[22];
		final float s23 = s[23];
		final float s24 = s[24];
		final float s25 = s[25];
		final float s26 = s[26];
		final float s27 = s[27];
		final float s28 = s[28];
		final float s29 = s[29];
		final float s30 = s[30];	
		final float s31 = s[31];

		float p0 = s0 + s31;
		float p1 = s1 + s30;
		float p2 = s2 + s29;
		float p3 = s3 + s28;
		float p4 = s4 + s27;
		float p5 = s5 + s26;
		float p6 = s6 + s25;
		float p7 = s7 + s24;
		float p8 = s8 + s23;
		float p9 = s9 + s22;
		float p10 = s10 + s21;
		float p11 = s11 + s20;
		float p12 = s12 + s19;
		float p13 = s13 + s18;
		float p14 = s14 + s17;
		float p15 = s15 + s16;

		float pp0 = p0 + p15;
		float pp1 = p1 + p14;
		float pp2 = p2 + p13;
		float pp3 = p3 + p12;
		float pp4 = p4 + p11;
		float pp5 = p5 + p10;
		float pp6 = p6 + p9;
		float pp7 = p7 + p8;
		float pp8 = (p0 - p15) * cos1_32;
		float pp9 = (p1 - p14) * cos3_32;
		float pp10 = (p2 - p13) * cos5_32;
		float pp11 = (p3 - p12) * cos7_32;
		float pp12 = (p4 - p11) * cos9_32;
		float pp13 = (p5 - p10) * cos11_32;
		float pp14 = (p6 - p9) * cos13_32;
		float pp15 = (p7 - p8) * cos15_32;

		p0 = pp0 + pp7;
		p1 = pp1 + pp6;
		p2 = pp2 + pp5;
		p3 = pp3 + pp4;
		p4 = (pp0 - pp7) * cos1_16;
		p5 = (pp1 - pp6) * cos3_16;
		p6 = (pp2 - pp5) * cos5_16;
		p7 = (pp3 - pp4) * cos7_16;
		p8 = pp8 + pp15;
		p9 = pp9 + pp14;
		p10 = pp10 + pp13;
		p11 = pp11 + pp12;
		p12 = (pp8 - pp15) * cos1_16;
		p13 = (pp9 - pp14) * cos3_16;
		p14 = (pp10 - pp13) * cos5_16;
		p15 = (pp11 - pp12) * cos7_16;


		pp0 = p0 + p3;
		pp1 = p1 + p2;
		pp2 = (p0 - p3) * cos1_8;
		pp3 = (p1 - p2) * cos3_8;
		pp4 = p4 + p7;
		pp5 = p5 + p6;
		pp6 = (p4 - p7) * cos1_8;
		pp7 = (p5 - p6) * cos3_8;
		pp8 = p8 + p11;
		pp9 = p9 + p10;
		pp10 = (p8 - p11) * cos1_8;
		pp11 = (p9 - p10) * cos3_8;
		pp12 = p12 + p15;
		pp13 = p13 + p14;
		pp14 = (p12 - p15) * cos1_8;
		pp15 = (p13 - p14) * cos3_8;

		p0 = pp0 + pp1;
		p1 = (pp0 - pp1) * cos1_4;
		p2 = pp2 + pp3;
		p3 = (pp2 - pp3) * cos1_4;
		p4 = pp4 + pp5;
		p5 = (pp4 - pp5) * cos1_4;
		p6 = pp6 + pp7;
		p7 = (pp6 - pp7) * cos1_4;
		p8 = pp8 + pp9;
		p9 = (pp8 - pp9) * cos1_4;
		p10 = pp10 + pp11;
		p11 = (pp10 - pp11) * cos1_4;
		p12 = pp12 + pp13;
		p13 = (pp12 - pp13) * cos1_4;
		p14 = pp14 + pp15;
		p15 = (pp14 - pp15) * cos1_4;

		// this is pretty insane coding
		float tmp1;
		new_v19/*36-17*/ = -(new_v4 = (new_v12 = p7) + p5) - p6;
		new_v27/*44-17*/ = -p6 - p7 - p4;
		new_v6 = (new_v10 = (new_v14 = p15) + p11) + p13;
		new_v17/*34-17*/ = -(new_v2 = p15 + p13 + p9) - p14;
		new_v21/*38-17*/ = (tmp1 = -p14 - p15 - p10 - p11) - p13;
		new_v29/*46-17*/ = -p14 - p15 - p12 - p8;
		new_v25/*42-17*/ = tmp1 - p12;
		new_v31/*48-17*/ = -p0;
		new_v0 = p1;
		new_v23/*40-17*/ = -(new_v8 = p3) - p2;

		p0 = (s0 - s31) * cos1_64;
		p1 = (s1 - s30) * cos3_64;
		p2 = (s2 - s29) * cos5_64;
		p3 = (s3 - s28) * cos7_64;
		p4 = (s4 - s27) * cos9_64;
		p5 = (s5 - s26) * cos11_64;
		p6 = (s6 - s25) * cos13_64;
		p7 = (s7 - s24) * cos15_64;
		p8 = (s8 - s23) * cos17_64;
		p9 = (s9 - s22) * cos19_64;
		p10 = (s10 - s21) * cos21_64;
		p11 = (s11 - s20) * cos23_64;
		p12 = (s12 - s19) * cos25_64;
		p13 = (s13 - s18) * cos27_64;
		p14 = (s14 - s17) * cos29_64;
		p15 = (s15 - s16) * cos31_64;


		pp0 = p0 + p15;
		pp1 = p1 + p14;
		pp2 = p2 + p13;
		pp3 = p3 + p12;
		pp4 = p4 + p11;
		pp5 = p5 + p10;
		pp6 = p6 + p9;
		pp7 = p7 + p8;
		pp8 = (p0 - p15) * cos1_32;
		pp9 = (p1 - p14) * cos3_32;
		pp10 = (p2 - p13) * cos5_32;
		pp11 = (p3 - p12) * cos7_32;
		pp12 = (p4 - p11) * cos9_32;
		pp13 = (p5 - p10) * cos11_32;
		pp14 = (p6 - p9) * cos13_32;
		pp15 = (p7 - p8) * cos15_32;


		p0 = pp0 + pp7;
		p1 = pp1 + pp6;
		p2 = pp2 + pp5;
		p3 = pp3 + pp4;
		p4 = (pp0 - pp7) * cos1_16;
		p5 = (pp1 - pp6) * cos3_16;
		p6 = (pp2 - pp5) * cos5_16;
		p7 = (pp3 - pp4) * cos7_16;
		p8 = pp8 + pp15;
		p9 = pp9 + pp14;
		p10 = pp10 + pp13;
		p11 = pp11 + pp12;
		p12 = (pp8 - pp15) * cos1_16;
		p13 = (pp9 - pp14) * cos3_16;
		p14 = (pp10 - pp13) * cos5_16;
		p15 = (pp11 - pp12) * cos7_16;


		pp0 = p0 + p3;
		pp1 = p1 + p2;
		pp2 = (p0 - p3) * cos1_8;
		pp3 = (p1 - p2) * cos3_8;
		pp4 = p4 + p7;
		pp5 = p5 + p6;
		pp6 = (p4 - p7) * cos1_8;
		pp7 = (p5 - p6) * cos3_8;
		pp8 = p8 + p11;
		pp9 = p9 + p10;
		pp10 = (p8 - p11) * cos1_8;
		pp11 = (p9 - p10) * cos3_8;
		pp12 = p12 + p15;
		pp13 = p13 + p14;
		pp14 = (p12 - p15) * cos1_8;
		pp15 = (p13 - p14) * cos3_8;


		p0 = pp0 + pp1;
		p1 = (pp0 - pp1) * cos1_4;
		p2 = pp2 + pp3;
		p3 = (pp2 - pp3) * cos1_4;
		p4 = pp4 + pp5;
		p5 = (pp4 - pp5) * cos1_4;
		p6 = pp6 + pp7;
		p7 = (pp6 - pp7) * cos1_4;
		p8 = pp8 + pp9;
		p9 = (pp8 - pp9) * cos1_4;
		p10 = pp10 + pp11;
		p11 = (pp10 - pp11) * cos1_4;
		p12 = pp12 + pp13;
		p13 = (pp12 - pp13) * cos1_4;
		p14 = pp14 + pp15;
		p15 = (pp14 - pp15) * cos1_4;


		// manually doing something that a compiler should handle sucks
		// coding like this is hard to read
		float tmp2;
		new_v5 = (new_v11 = (new_v13 = (new_v15 = p15) + p7) + p11)
				+ p5 + p13;
		new_v7 = (new_v9 = p15 + p11 + p3) + p13;
		new_v16/*33-17*/ = -(new_v1 = (tmp1 = p13 + p15 + p9) + p1) - p14;
		new_v18/*35-17*/ = -(new_v3 = tmp1 + p5 + p7) - p6 - p14;

		new_v22/*39-17*/ = (tmp1 = -p10 - p11 - p14 - p15)
				- p13 - p2 - p3;
		new_v20/*37-17*/ = tmp1 - p13 - p5 - p6 - p7;
		new_v24/*41-17*/ = tmp1 - p12 - p2 - p3;
		new_v26/*43-17*/ = tmp1 - p12 - (tmp2 = p4 + p6 + p7);
		new_v30/*47-17*/ = (tmp1 = -p8 - p12 - p14 - p15) - p0;
		new_v28/*45-17*/ = tmp1 - tmp2;

		// insert V[0-15] (== new_v[0-15]) into actual v:	
		// float[] x2 = actual_v + actual_write_pos;
		float dest[] = actual_v;

		int pos = actual_write_pos;

		dest[0 + pos] = new_v0;
		dest[16 + pos] = new_v1;
		dest[32 + pos] = new_v2;
		dest[48 + pos] = new_v3;
		dest[64 + pos] = new_v4;
		dest[80 + pos] = new_v5;
		dest[96 + pos] = new_v6;
		dest[112 + pos] = new_v7;
		dest[128 + pos] = new_v8;
		dest[144 + pos] = new_v9;
		dest[160 + pos] = new_v10;
		dest[176 + pos] = new_v11;
		dest[192 + pos] = new_v12;
		dest[208 + pos] = new_v13;
		dest[224 + pos] = new_v14;
		dest[240 + pos] = new_v15;

		// V[16] is always 0.0:
		dest[256 + pos] = 0.0f;

		// insert V[17-31] (== -new_v[15-1]) into actual v:
		dest[272 + pos] = -new_v15;
		dest[288 + pos] = -new_v14;
		dest[304 + pos] = -new_v13;
		dest[320 + pos] = -new_v12;
		dest[336 + pos] = -new_v11;
		dest[352 + pos] = -new_v10;
		dest[368 + pos] = -new_v9;
		dest[384 + pos] = -new_v8;
		dest[400 + pos] = -new_v7;
		dest[416 + pos] = -new_v6;
		dest[432 + pos] = -new_v5;
		dest[448 + pos] = -new_v4;
		dest[464 + pos] = -new_v3;
		dest[480 + pos] = -new_v2;
		dest[496 + pos] = -new_v1;

		// insert V[32] (== -new_v[0]) into other v:
		dest = (actual_v==v1) ? v2 : v1;

		dest[0 + pos] = -new_v0;
		// insert V[33-48] (== new_v[16-31]) into other v:
		dest[16 + pos] = new_v16;
		dest[32 + pos] = new_v17;
		dest[48 + pos] = new_v18;
		dest[64 + pos] = new_v19;
		dest[80 + pos] = new_v20;
		dest[96 + pos] = new_v21;
		dest[112 + pos] = new_v22;
		dest[128 + pos] = new_v23;
		dest[144 + pos] = new_v24;
		dest[160 + pos] = new_v25;
		dest[176 + pos] = new_v26;
		dest[192 + pos] = new_v27;
		dest[208 + pos] = new_v28;
		dest[224 + pos] = new_v29;
		dest[240 + pos] = new_v30;
		dest[256 + pos] = new_v31;

		// insert V[49-63] (== new_v[30-16]) into other v:
		dest[272 + pos] = new_v30;
		dest[288 + pos] = new_v29;
		dest[304 + pos] = new_v28;
		dest[320 + pos] = new_v27;
		dest[336 + pos] = new_v26;
		dest[352 + pos] = new_v25;
		dest[368 + pos] = new_v24;
		dest[384 + pos] = new_v23;
		dest[400 + pos] = new_v22;
		dest[416 + pos] = new_v21;
		dest[432 + pos] = new_v20;
		dest[448 + pos] = new_v19;
		dest[464 + pos] = new_v18;
		dest[480 + pos] = new_v17;
		dest[496 + pos] = new_v16; 			
	}

	/**
	 * Compute PCM Samples.
	 */
	private final float[] _tmpOut = new float[32];

	private void compute_pcm_samples0( )
	{
		final float[] vp = actual_v;	
		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			float pcm_sample;
			final float[] dp = d16[i];
			pcm_sample = (float)(((vp[0 + dvp] * dp[0]) +
					(vp[15 + dvp] * dp[1]) +
					(vp[14 + dvp] * dp[2]) +
					(vp[13 + dvp] * dp[3]) +
					(vp[12 + dvp] * dp[4]) +
					(vp[11 + dvp] * dp[5]) +
					(vp[10 + dvp] * dp[6]) +
					(vp[9 + dvp] * dp[7]) +
					(vp[8 + dvp] * dp[8]) +
					(vp[7 + dvp] * dp[9]) +
					(vp[6 + dvp] * dp[10]) +
					(vp[5 + dvp] * dp[11]) +
					(vp[4 + dvp] * dp[12]) +
					(vp[3 + dvp] * dp[13]) +
					(vp[2 + dvp] * dp[14]) +
					(vp[1 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples1( )
	{
		final float[] vp = actual_v;	
		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (((vp[1 + dvp] * dp[0]) +
					(vp[0 + dvp] * dp[1]) +
					(vp[15 + dvp] * dp[2]) +
					(vp[14 + dvp] * dp[3]) +
					(vp[13 + dvp] * dp[4]) +
					(vp[12 + dvp] * dp[5]) +
					(vp[11 + dvp] * dp[6]) +
					(vp[10 + dvp] * dp[7]) +
					(vp[9 + dvp] * dp[8]) +
					(vp[8 + dvp] * dp[9]) +
					(vp[7 + dvp] * dp[10]) +
					(vp[6 + dvp] * dp[11]) +
					(vp[5 + dvp] * dp[12]) +
					(vp[4 + dvp] * dp[13]) +
					(vp[3 + dvp] * dp[14]) +
					(vp[2 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples2( )
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[2 + dvp] * dp[0]) +
					(vp[1 + dvp] * dp[1]) +
					(vp[0 + dvp] * dp[2]) +
					(vp[15 + dvp] * dp[3]) +
					(vp[14 + dvp] * dp[4]) +
					(vp[13 + dvp] * dp[5]) +
					(vp[12 + dvp] * dp[6]) +
					(vp[11 + dvp] * dp[7]) +
					(vp[10 + dvp] * dp[8]) +
					(vp[9 + dvp] * dp[9]) +
					(vp[8 + dvp] * dp[10]) +
					(vp[7 + dvp] * dp[11]) +
					(vp[6 + dvp] * dp[12]) +
					(vp[5 + dvp] * dp[13]) +
					(vp[4 + dvp] * dp[14]) +
					(vp[3 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples3( )
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[3 + dvp] * dp[0]) +
					(vp[2 + dvp] * dp[1]) +
					(vp[1 + dvp] * dp[2]) +
					(vp[0 + dvp] * dp[3]) +
					(vp[15 + dvp] * dp[4]) +
					(vp[14 + dvp] * dp[5]) +
					(vp[13 + dvp] * dp[6]) +
					(vp[12 + dvp] * dp[7]) +
					(vp[11 + dvp] * dp[8]) +
					(vp[10 + dvp] * dp[9]) +
					(vp[9 + dvp] * dp[10]) +
					(vp[8 + dvp] * dp[11]) +
					(vp[7 + dvp] * dp[12]) +
					(vp[6 + dvp] * dp[13]) +
					(vp[5 + dvp] * dp[14]) +
					(vp[4 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples4()
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[4 + dvp] * dp[0]) +
					(vp[3 + dvp] * dp[1]) +
					(vp[2 + dvp] * dp[2]) +
					(vp[1 + dvp] * dp[3]) +
					(vp[0 + dvp] * dp[4]) +
					(vp[15 + dvp] * dp[5]) +
					(vp[14 + dvp] * dp[6]) +
					(vp[13 + dvp] * dp[7]) +
					(vp[12 + dvp] * dp[8]) +
					(vp[11 + dvp] * dp[9]) +
					(vp[10 + dvp] * dp[10]) +
					(vp[9 + dvp] * dp[11]) +
					(vp[8 + dvp] * dp[12]) +
					(vp[7 + dvp] * dp[13]) +
					(vp[6 + dvp] * dp[14]) +
					(vp[5 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples5()
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[5 + dvp] * dp[0]) +
					(vp[4 + dvp] * dp[1]) +
					(vp[3 + dvp] * dp[2]) +
					(vp[2 + dvp] * dp[3]) +
					(vp[1 + dvp] * dp[4]) +
					(vp[0 + dvp] * dp[5]) +
					(vp[15 + dvp] * dp[6]) +
					(vp[14 + dvp] * dp[7]) +
					(vp[13 + dvp] * dp[8]) +
					(vp[12 + dvp] * dp[9]) +
					(vp[11 + dvp] * dp[10]) +
					(vp[10 + dvp] * dp[11]) +
					(vp[9 + dvp] * dp[12]) +
					(vp[8 + dvp] * dp[13]) +
					(vp[7 + dvp] * dp[14]) +
					(vp[6 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples6()
	{
		final float[] vp = actual_v;	
		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[6 + dvp] * dp[0]) +
					(vp[5 + dvp] * dp[1]) +
					(vp[4 + dvp] * dp[2]) +
					(vp[3 + dvp] * dp[3]) +
					(vp[2 + dvp] * dp[4]) +
					(vp[1 + dvp] * dp[5]) +
					(vp[0 + dvp] * dp[6]) +
					(vp[15 + dvp] * dp[7]) +
					(vp[14 + dvp] * dp[8]) +
					(vp[13 + dvp] * dp[9]) +
					(vp[12 + dvp] * dp[10]) +
					(vp[11 + dvp] * dp[11]) +
					(vp[10 + dvp] * dp[12]) +
					(vp[9 + dvp] * dp[13]) +
					(vp[8 + dvp] * dp[14]) +
					(vp[7 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples7()
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[7 + dvp] * dp[0]) +
					(vp[6 + dvp] * dp[1]) +
					(vp[5 + dvp] * dp[2]) +
					(vp[4 + dvp] * dp[3]) +
					(vp[3 + dvp] * dp[4]) +
					(vp[2 + dvp] * dp[5]) +
					(vp[1 + dvp] * dp[6]) +
					(vp[0 + dvp] * dp[7]) +
					(vp[15 + dvp] * dp[8]) +
					(vp[14 + dvp] * dp[9]) +
					(vp[13 + dvp] * dp[10]) +
					(vp[12 + dvp] * dp[11]) +
					(vp[11 + dvp] * dp[12]) +
					(vp[10 + dvp] * dp[13]) +
					(vp[9 + dvp] * dp[14]) +
					(vp[8 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples8()
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[8 + dvp] * dp[0]) +
					(vp[7 + dvp] * dp[1]) +
					(vp[6 + dvp] * dp[2]) +
					(vp[5 + dvp] * dp[3]) +
					(vp[4 + dvp] * dp[4]) +
					(vp[3 + dvp] * dp[5]) +
					(vp[2 + dvp] * dp[6]) +
					(vp[1 + dvp] * dp[7]) +
					(vp[0 + dvp] * dp[8]) +
					(vp[15 + dvp] * dp[9]) +
					(vp[14 + dvp] * dp[10]) +
					(vp[13 + dvp] * dp[11]) +
					(vp[12 + dvp] * dp[12]) +
					(vp[11 + dvp] * dp[13]) +
					(vp[10 + dvp] * dp[14]) +
					(vp[9 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples9()
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[9 + dvp] * dp[0]) +
					(vp[8 + dvp] * dp[1]) +
					(vp[7 + dvp] * dp[2]) +
					(vp[6 + dvp] * dp[3]) +
					(vp[5 + dvp] * dp[4]) +
					(vp[4 + dvp] * dp[5]) +
					(vp[3 + dvp] * dp[6]) +
					(vp[2 + dvp] * dp[7]) +
					(vp[1 + dvp] * dp[8]) +
					(vp[0 + dvp] * dp[9]) +
					(vp[15 + dvp] * dp[10]) +
					(vp[14 + dvp] * dp[11]) +
					(vp[13 + dvp] * dp[12]) +
					(vp[12 + dvp] * dp[13]) +
					(vp[11 + dvp] * dp[14]) +
					(vp[10 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples10()
	{
		final float[] vp = actual_v;	
		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[10 + dvp] * dp[0]) +
					(vp[9 + dvp] * dp[1]) +
					(vp[8 + dvp] * dp[2]) +
					(vp[7 + dvp] * dp[3]) +
					(vp[6 + dvp] * dp[4]) +
					(vp[5 + dvp] * dp[5]) +
					(vp[4 + dvp] * dp[6]) +
					(vp[3 + dvp] * dp[7]) +
					(vp[2 + dvp] * dp[8]) +
					(vp[1 + dvp] * dp[9]) +
					(vp[0 + dvp] * dp[10]) +
					(vp[15 + dvp] * dp[11]) +
					(vp[14 + dvp] * dp[12]) +
					(vp[13 + dvp] * dp[13]) +
					(vp[12 + dvp] * dp[14]) +
					(vp[11 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples11()
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[11 + dvp] * dp[0]) +
					(vp[10 + dvp] * dp[1]) +
					(vp[9 + dvp] * dp[2]) +
					(vp[8 + dvp] * dp[3]) +
					(vp[7 + dvp] * dp[4]) +
					(vp[6 + dvp] * dp[5]) +
					(vp[5 + dvp] * dp[6]) +
					(vp[4 + dvp] * dp[7]) +
					(vp[3 + dvp] * dp[8]) +
					(vp[2 + dvp] * dp[9]) +
					(vp[1 + dvp] * dp[10]) +
					(vp[0 + dvp] * dp[11]) +
					(vp[15 + dvp] * dp[12]) +
					(vp[14 + dvp] * dp[13]) +
					(vp[13 + dvp] * dp[14]) +
					(vp[12 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples12()
	{
		final float[] vp = actual_v;	
		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[12 + dvp] * dp[0]) +
					(vp[11 + dvp] * dp[1]) +
					(vp[10 + dvp] * dp[2]) +
					(vp[9 + dvp] * dp[3]) +
					(vp[8 + dvp] * dp[4]) +
					(vp[7 + dvp] * dp[5]) +
					(vp[6 + dvp] * dp[6]) +
					(vp[5 + dvp] * dp[7]) +
					(vp[4 + dvp] * dp[8]) +
					(vp[3 + dvp] * dp[9]) +
					(vp[2 + dvp] * dp[10]) +
					(vp[1 + dvp] * dp[11]) +
					(vp[0 + dvp] * dp[12]) +
					(vp[15 + dvp] * dp[13]) +
					(vp[14 + dvp] * dp[14]) +
					(vp[13 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples13()
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (float)(((vp[13 + dvp] * dp[0]) +
					(vp[12 + dvp] * dp[1]) +
					(vp[11 + dvp] * dp[2]) +
					(vp[10 + dvp] * dp[3]) +
					(vp[9 + dvp] * dp[4]) +
					(vp[8 + dvp] * dp[5]) +
					(vp[7 + dvp] * dp[6]) +
					(vp[6 + dvp] * dp[7]) +
					(vp[5 + dvp] * dp[8]) +
					(vp[4 + dvp] * dp[9]) +
					(vp[3 + dvp] * dp[10]) +
					(vp[2 + dvp] * dp[11]) +
					(vp[1 + dvp] * dp[12]) +
					(vp[0 + dvp] * dp[13]) +
					(vp[15 + dvp] * dp[14]) +
					(vp[14 + dvp] * dp[15])
					) );

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples14()
	{
		final float[] vp = actual_v;

		//int inc = v_inc;
		final float[] tmpOut = _tmpOut;
		int dvp =0;

		// fat chance of having this loop unroll
		for( int i=0; i<32; i++)
		{
			final float[] dp = d16[i];
			float pcm_sample;

			pcm_sample = (vp[14 + dvp] * dp[0]) +
					(vp[13 + dvp] * dp[1]) +
					(vp[12 + dvp] * dp[2]) +
					(vp[11 + dvp] * dp[3]) +
					(vp[10 + dvp] * dp[4]) +
					(vp[9 + dvp] * dp[5]) +
					(vp[8 + dvp] * dp[6]) +
					(vp[7 + dvp] * dp[7]) +
					(vp[6 + dvp] * dp[8]) +
					(vp[5 + dvp] * dp[9]) +
					(vp[4 + dvp] * dp[10]) +
					(vp[3 + dvp] * dp[11]) +
					(vp[2 + dvp] * dp[12]) +
					(vp[1 + dvp] * dp[13]) +
					(vp[0 + dvp] * dp[14]) +
					(vp[15 + dvp] * dp[15]);

			tmpOut[i] = pcm_sample;

			dvp += 16;
		} // for
	}
	private void compute_pcm_samples15()
	{
		final float[] vp = actual_v;
		final float[] tmpOut = _tmpOut;
		int dvp =0;
		for( int i=0; i<32; i++)
		{
			float pcm_sample;
			final float dp[] = d16[i];
			pcm_sample = (vp[15 + dvp] * dp[0]) +
					(vp[14 + dvp] * dp[1]) +
					(vp[13 + dvp] * dp[2]) +
					(vp[12 + dvp] * dp[3]) +
					(vp[11 + dvp] * dp[4]) +
					(vp[10 + dvp] * dp[5]) +
					(vp[9 + dvp] * dp[6]) +
					(vp[8 + dvp] * dp[7]) +
					(vp[7 + dvp] * dp[8]) +
					(vp[6 + dvp] * dp[9]) +
					(vp[5 + dvp] * dp[10]) +
					(vp[4 + dvp] * dp[11]) +
					(vp[3 + dvp] * dp[12]) +
					(vp[2 + dvp] * dp[13]) +
					(vp[1 + dvp] * dp[14]) +
					(vp[0 + dvp] * dp[15]);

			tmpOut[i] = pcm_sample;			
			dvp += 16;
		}
	}

	private void compute_pcm_samples(Obuffer buffer)
	{
		switch (actual_write_pos)
		{
		case 0: 
			compute_pcm_samples0();
			break;
		case 1: 
			compute_pcm_samples1();
			break;
		case 2: 
			compute_pcm_samples2();
			break;
		case 3: 
			compute_pcm_samples3();
			break;
		case 4: 
			compute_pcm_samples4();
			break;
		case 5: 
			compute_pcm_samples5();
			break;
		case 6: 
			compute_pcm_samples6();
			break;
		case 7: 
			compute_pcm_samples7();
			break;
		case 8: 
			compute_pcm_samples8();
			break;
		case 9: 
			compute_pcm_samples9();
			break;
		case 10: 
			compute_pcm_samples10();
			break;
		case 11: 
			compute_pcm_samples11();
			break;
		case 12: 
			compute_pcm_samples12();
			break;
		case 13: 
			compute_pcm_samples13();
			break;
		case 14: 
			compute_pcm_samples14();
			break;
		case 15: 
			compute_pcm_samples15();
			break;
		}

		if (buffer!=null)
			buffer.appendSamples(channel, _tmpOut);
	}

  /**
	 * Calculate 32 PCM samples and put the into the Obuffer-object.
	 */
	public void calculate_pcm_samples_layer_iii(Obuffer buffer)
	{
		compute_new_v();	
		compute_pcm_samples(buffer);

		actual_write_pos = (actual_write_pos + 1) & 0xf;
		actual_v = (actual_v == v1) ? v2 : v1;
	}

	public void calculate_pcm_samples_layer_i_ii(Obuffer buffer)
	{
		calculate_pcm_samples_layer_iii(buffer);
		// MDM: this may not be necessary. The Layer III decoder always
		// outputs 32 subband samples, but I haven't checked layer I & II.
		for (int p=0;p<32;p++) 
			samples[p] = 0.0f;
	}
  
  
  private static final double MY_PI = 3.14159265358979323846;
  private static final float cos1_64  =(float) (1.0 / (2.0 * Math.cos(MY_PI        / 64.0)));
  private static final float cos3_64  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 3.0  / 64.0)));
  private static final float cos5_64  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 5.0  / 64.0)));
  private static final float cos7_64  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 7.0  / 64.0)));
  private static final float cos9_64  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 9.0  / 64.0)));
  private static final float cos11_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 11.0 / 64.0)));
  private static final float cos13_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 13.0 / 64.0)));
  private static final float cos15_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 15.0 / 64.0)));
  private static final float cos17_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 17.0 / 64.0)));
  private static final float cos19_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 19.0 / 64.0)));
  private static final float cos21_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 21.0 / 64.0)));
  private static final float cos23_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 23.0 / 64.0)));
  private static final float cos25_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 25.0 / 64.0)));
  private static final float cos27_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 27.0 / 64.0)));
  private static final float cos29_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 29.0 / 64.0)));
  private static final float cos31_64 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 31.0 / 64.0)));
  private static final float cos1_32  =(float) (1.0 / (2.0 * Math.cos(MY_PI        / 32.0)));
  private static final float cos3_32  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 3.0  / 32.0)));
  private static final float cos5_32  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 5.0  / 32.0)));
  private static final float cos7_32  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 7.0  / 32.0)));
  private static final float cos9_32  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 9.0  / 32.0)));
  private static final float cos11_32 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 11.0 / 32.0)));
  private static final float cos13_32 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 13.0 / 32.0)));
  private static final float cos15_32 =(float) (1.0 / (2.0 * Math.cos(MY_PI * 15.0 / 32.0)));
  private static final float cos1_16  =(float) (1.0 / (2.0 * Math.cos(MY_PI        / 16.0)));
  private static final float cos3_16  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 3.0  / 16.0)));
  private static final float cos5_16  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 5.0  / 16.0)));
  private static final float cos7_16  =(float) (1.0 / (2.0 * Math.cos(MY_PI * 7.0  / 16.0)));
  private static final float cos1_8   =(float) (1.0 / (2.0 * Math.cos(MY_PI        / 8.0)));
  private static final float cos3_8   =(float) (1.0 / (2.0 * Math.cos(MY_PI * 3.0  / 8.0)));
  private static final float cos1_4   =(float) (1.0 / (2.0 * Math.cos(MY_PI / 4.0)));
  
  // Note: These values are not in the same order
  // as in Annex 3-B.3 of the ISO/IEC DIS 11172-3 
  // private float d[] = {0.000000000, -4.000442505};
  
  private static float d[] = {
	0.0f, -4.42505E-4f, 0.003250122f, -0.007003784f, 0.031082153f, -0.07862854f, 0.10031128f, -0.57203674f, 
	1.144989f, 0.57203674f, 0.10031128f, 0.07862854f, 0.031082153f, 0.007003784f, 0.003250122f, 4.42505E-4f, 
	-1.5259E-5f, -4.73022E-4f, 0.003326416f, -0.007919312f, 0.030517578f, -0.08418274f, 0.090927124f, -0.6002197f, 
	1.1442871f, 0.54382324f, 0.1088562f, 0.07305908f, 0.03147888f, 0.006118774f, 0.003173828f, 3.96729E-4f, 
	-1.5259E-5f, -5.34058E-4f, 0.003387451f, -0.008865356f, 0.029785156f, -0.08970642f, 0.08068848f, -0.6282959f, 
	1.1422119f, 0.51560974f, 0.11657715f, 0.06752014f, 0.03173828f, 0.0052948f, 0.003082275f, 3.66211E-4f, 
	-1.5259E-5f, -5.79834E-4f, 0.003433228f, -0.009841919f, 0.028884888f, -0.09516907f, 0.06959534f, -0.6562195f, 
	1.1387634f, 0.48747253f, 0.12347412f, 0.06199646f, 0.031845093f, 0.004486084f, 0.002990723f, 3.20435E-4f, 
	-1.5259E-5f, -6.2561E-4f, 0.003463745f, -0.010848999f, 0.027801514f, -0.10054016f, 0.057617188f, -0.6839142f, 
	1.1339264f, 0.45947266f, 0.12957764f, 0.056533813f, 0.031814575f, 0.003723145f, 0.00289917f, 2.89917E-4f, 
	-1.5259E-5f, -6.86646E-4f, 0.003479004f, -0.011886597f, 0.026535034f, -0.1058197f, 0.044784546f, -0.71131897f, 
	1.1277466f, 0.43165588f, 0.1348877f, 0.051132202f, 0.031661987f, 0.003005981f, 0.002792358f, 2.59399E-4f, 
	-1.5259E-5f, -7.47681E-4f, 0.003479004f, -0.012939453f, 0.02508545f, -0.110946655f, 0.031082153f, -0.7383728f, 
	1.120224f, 0.40408325f, 0.13945007f, 0.045837402f, 0.03138733f, 0.002334595f, 0.002685547f, 2.44141E-4f, 
	-3.0518E-5f, -8.08716E-4f, 0.003463745f, -0.014022827f, 0.023422241f, -0.11592102f, 0.01651001f, -0.7650299f, 
	1.1113739f, 0.37680054f, 0.14326477f, 0.040634155f, 0.03100586f, 0.001693726f, 0.002578735f, 2.13623E-4f, 
	-3.0518E-5f, -8.8501E-4f, 0.003417969f, -0.01512146f, 0.021575928f, -0.12069702f, 0.001068115f, -0.791214f, 
	1.1012115f, 0.34986877f, 0.1463623f, 0.03555298f, 0.030532837f, 0.001098633f, 0.002456665f, 1.98364E-4f, 
	-3.0518E-5f, -9.61304E-4f, 0.003372192f, -0.016235352f, 0.01953125f, -0.1252594f, -0.015228271f, -0.816864f, 
	1.0897827f, 0.32331848f, 0.1487732f, 0.03060913f, 0.029937744f, 5.49316E-4f, 0.002349854f, 1.67847E-4f, 
	-3.0518E-5f, -0.001037598f, 0.00328064f, -0.017349243f, 0.01725769f, -0.12956238f, -0.03237915f, -0.84194946f, 
	1.0771179f, 0.2972107f, 0.15049744f, 0.025817871f, 0.029281616f, 3.0518E-5f, 0.002243042f, 1.52588E-4f, 
	-4.5776E-5f, -0.001113892f, 0.003173828f, -0.018463135f, 0.014801025f, -0.1335907f, -0.050354004f, -0.8663635f, 
	1.0632172f, 0.2715912f, 0.15159607f, 0.0211792f, 0.028533936f, -4.42505E-4f, 0.002120972f, 1.37329E-4f, 
	-4.5776E-5f, -0.001205444f, 0.003051758f, -0.019577026f, 0.012115479f, -0.13729858f, -0.06916809f, -0.89009094f, 
	1.0481567f, 0.24650574f, 0.15206909f, 0.016708374f, 0.02772522f, -8.69751E-4f, 0.00201416f, 1.2207E-4f, 
	-6.1035E-5f, -0.001296997f, 0.002883911f, -0.020690918f, 0.009231567f, -0.14067078f, -0.088775635f, -0.9130554f, 
	1.0319366f, 0.22198486f, 0.15196228f, 0.012420654f, 0.02684021f, -0.001266479f, 0.001907349f, 1.06812E-4f, 
	-6.1035E-5f, -0.00138855f, 0.002700806f, -0.02178955f, 0.006134033f, -0.14367676f, -0.10916138f, -0.9351959f, 
	1.0146179f, 0.19805908f, 0.15130615f, 0.00831604f, 0.025909424f, -0.001617432f, 0.001785278f, 1.06812E-4f, 
	-7.6294E-5f, -0.001480103f, 0.002487183f, -0.022857666f, 0.002822876f, -0.1462555f, -0.13031006f, -0.95648193f, 
	0.99624634f, 0.17478943f, 0.15011597f, 0.004394531f, 0.024932861f, -0.001937866f, 0.001693726f, 9.1553E-5f, 
	-7.6294E-5f, -0.001586914f, 0.002227783f, -0.023910522f, -6.86646E-4f, -0.14842224f, -0.15220642f, -0.9768524f, 
	0.9768524f, 0.15220642f, 0.14842224f, 6.86646E-4f, 0.023910522f, -0.002227783f, 0.001586914f, 7.6294E-5f, 
	-9.1553E-5f, -0.001693726f, 0.001937866f, -0.024932861f, -0.004394531f, -0.15011597f, -0.17478943f, -0.99624634f, 
	0.95648193f, 0.13031006f, 0.1462555f, -0.002822876f, 0.022857666f, -0.002487183f, 0.001480103f, 7.6294E-5f, 
	-1.06812E-4f, -0.001785278f, 0.001617432f, -0.025909424f, -0.00831604f, -0.15130615f, -0.19805908f, -1.0146179f, 
	0.9351959f, 0.10916138f, 0.14367676f, -0.006134033f, 0.02178955f, -0.002700806f, 0.00138855f, 6.1035E-5f, 
	-1.06812E-4f, -0.001907349f, 0.001266479f, -0.02684021f, -0.012420654f, -0.15196228f, -0.22198486f, -1.0319366f, 
	0.9130554f, 0.088775635f, 0.14067078f, -0.009231567f, 0.020690918f, -0.002883911f, 0.001296997f, 6.1035E-5f, 
	-1.2207E-4f, -0.00201416f, 8.69751E-4f, -0.02772522f, -0.016708374f, -0.15206909f, -0.24650574f, -1.0481567f, 
	0.89009094f, 0.06916809f, 0.13729858f, -0.012115479f, 0.019577026f, -0.003051758f, 0.001205444f, 4.5776E-5f, 
	-1.37329E-4f, -0.002120972f, 4.42505E-4f, -0.028533936f, -0.0211792f, -0.15159607f, -0.2715912f, -1.0632172f, 
	0.8663635f, 0.050354004f, 0.1335907f, -0.014801025f, 0.018463135f, -0.003173828f, 0.001113892f, 4.5776E-5f, 
	-1.52588E-4f, -0.002243042f, -3.0518E-5f, -0.029281616f, -0.025817871f, -0.15049744f, -0.2972107f, -1.0771179f, 
	0.84194946f, 0.03237915f, 0.12956238f, -0.01725769f, 0.017349243f, -0.00328064f, 0.001037598f, 3.0518E-5f, 
	-1.67847E-4f, -0.002349854f, -5.49316E-4f, -0.029937744f, -0.03060913f, -0.1487732f, -0.32331848f, -1.0897827f, 
	0.816864f, 0.015228271f, 0.1252594f, -0.01953125f, 0.016235352f, -0.003372192f, 9.61304E-4f, 3.0518E-5f, 
	-1.98364E-4f, -0.002456665f, -0.001098633f, -0.030532837f, -0.03555298f, -0.1463623f, -0.34986877f, -1.1012115f, 
	0.791214f, -0.001068115f, 0.12069702f, -0.021575928f, 0.01512146f, -0.003417969f, 8.8501E-4f, 3.0518E-5f, 
	-2.13623E-4f, -0.002578735f, -0.001693726f, -0.03100586f, -0.040634155f, -0.14326477f, -0.37680054f, -1.1113739f, 
	0.7650299f, -0.01651001f, 0.11592102f, -0.023422241f, 0.014022827f, -0.003463745f, 8.08716E-4f, 3.0518E-5f, 
	-2.44141E-4f, -0.002685547f, -0.002334595f, -0.03138733f, -0.045837402f, -0.13945007f, -0.40408325f, -1.120224f, 
	0.7383728f, -0.031082153f, 0.110946655f, -0.02508545f, 0.012939453f, -0.003479004f, 7.47681E-4f, 1.5259E-5f, 
	-2.59399E-4f, -0.002792358f, -0.003005981f, -0.031661987f, -0.051132202f, -0.1348877f, -0.43165588f, -1.1277466f, 
	0.71131897f, -0.044784546f, 0.1058197f, -0.026535034f, 0.011886597f, -0.003479004f, 6.86646E-4f, 1.5259E-5f, 
	-2.89917E-4f, -0.00289917f, -0.003723145f, -0.031814575f, -0.056533813f, -0.12957764f, -0.45947266f, -1.1339264f, 
	0.6839142f, -0.057617188f, 0.10054016f, -0.027801514f, 0.010848999f, -0.003463745f, 6.2561E-4f, 1.5259E-5f, 
	-3.20435E-4f, -0.002990723f, -0.004486084f, -0.031845093f, -0.06199646f, -0.12347412f, -0.48747253f, -1.1387634f, 
	0.6562195f, -0.06959534f, 0.09516907f, -0.028884888f, 0.009841919f, -0.003433228f, 5.79834E-4f, 1.5259E-5f, 
	-3.66211E-4f, -0.003082275f, -0.0052948f, -0.03173828f, -0.06752014f, -0.11657715f, -0.51560974f, -1.1422119f, 
	0.6282959f, -0.08068848f, 0.08970642f, -0.029785156f, 0.008865356f, -0.003387451f, 5.34058E-4f, 1.5259E-5f, 
	-3.96729E-4f, -0.003173828f, -0.006118774f, -0.03147888f, -0.07305908f, -0.1088562f, -0.54382324f, -1.1442871f, 
	0.6002197f, -0.090927124f, 0.08418274f, -0.030517578f, 0.007919312f, -0.003326416f, 4.73022E-4f, 1.5259E-5f
};
  
  /** 
   * d[] split into subarrays of length 16. This provides for
   * more faster access by allowing a block of 16 to be addressed
   * with constant offset. 
   **/
  private static float d16[][] = null;	
	
	/**
	 * Converts a 1D array into a number of smaller arrays. This is used
	 * to achieve offset + constant indexing into an array. Each sub-array
	 * represents a block of values of the original array. 
	 * @param array			The array to split up into blocks.
	 * @param blockSize		The size of the blocks to split the array
	 *						into. This must be an exact divisor of
	 *						the length of the array, or some data
	 *						will be lost from the main array.
	 * 
	 * @return	An array of arrays in which each element in the returned
	 *			array will be of length <code>blockSize</code>.
	 */
	static private float[][] splitArray(final float[] array, final int blockSize)
	{
		int size = array.length / blockSize;
		float[][] split = new float[size][];
		for (int i=0; i<size; i++)
		{
			split[i] = subArray(array, i*blockSize, blockSize);
		}
		return split;
	}
	
	/**
	 * Returns a subarray of an existing array.
	 * 
	 * @param array	The array to retrieve a subarra from.
	 * @param offs	The offset in the array that corresponds to
	 *				the first index of the subarray.
	 * @param len	The number of indeces in the subarray.
	 * @return The subarray, which may be of length 0.
	 */
	static private float[] subArray(final float[] array, final int offs, int len)
	{
		if (offs+len > array.length)
		{
			len = array.length-offs;
		}
		
		if (len < 0)
			len = 0;
		
		float[] subarray = new float[len];
		for (int i=0; i<len; i++)
		{
			subarray[i] = array[offs+i];
		}
		
		return subarray;
	}
  
}
