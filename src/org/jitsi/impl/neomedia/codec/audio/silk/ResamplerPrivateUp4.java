/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.impl.neomedia.codec.audio.silk;

import static org.jitsi.impl.neomedia.codec.audio.silk.Macros.SKP_SMLAWB;
import static org.jitsi.impl.neomedia.codec.audio.silk.Macros.SKP_SMULWB;

/**
 * Upsample by a factor 4. Note: very low quality, only use with output sampling
 * rates above 96 kHz.
 *
 * @author Jing Dai
 * @author Dingxin Xu
 */
public class ResamplerPrivateUp4 {
	/**
	 * Upsample by a factor 4. Note: very low quality, only use with output sampling
	 * rates above 96 kHz.
	 * 
	 * @param S          State vector [ 2 ].
	 * @param S_offset   offset of valid data.
	 * @param out        Output signal [ 4 * len ].
	 * @param out_offset offset of valid data.
	 * @param in         Input signal [ len ].
	 * @param in_offset  offset of valid data.
	 * @param len        Number of INPUT samples.
	 */
	static void SKP_Silk_resampler_private_up4(int[] S, /* I/O: State vector [ 2 ] */
			int S_offset, short[] out, /* O: Output signal [ 4 * len ] */
			int out_offset, short[] in, /* I: Input signal [ len ] */
			int in_offset, int len /* I: Number of INPUT samples */
	) {
		int k;
		int in32, out32, Y, X;
		int out16;

		assert (ResamplerRom.SKP_Silk_resampler_up2_lq_0 > 0);
		assert (ResamplerRom.SKP_Silk_resampler_up2_lq_1 < 0);

		/* Internal variables and state are in Q10 format */
		for (k = 0; k < len; k++) {
			/* Convert to Q10 */
			in32 = in[in_offset + k] << 10;

			/* All-pass section for even output sample */
			Y = in32 - S[S_offset + 0];
			X = SKP_SMULWB(Y, ResamplerRom.SKP_Silk_resampler_up2_lq_0);
			out32 = S[S_offset + 0] + X;
			S[S_offset + 0] = in32 + X;

			/* Convert back to int16 and store to output */
			out16 = (short) SigProcFIX.SKP_SAT16(SigProcFIX.SKP_RSHIFT_ROUND(out32, 10));
			out[out_offset + 4 * k] = (short) out16;
			out[out_offset + 4 * k + 1] = (short) out16;

			/* All-pass section for odd output sample */
			Y = in32 - S[S_offset + 1];
			X = SKP_SMLAWB(Y, Y, ResamplerRom.SKP_Silk_resampler_up2_lq_1);
			out32 = S[S_offset + 1] + X;
			S[S_offset + 1] = in32 + X;

			/* Convert back to int16 and store to output */
			out16 = (short) SigProcFIX.SKP_SAT16(SigProcFIX.SKP_RSHIFT_ROUND(out32, 10));
			out[out_offset + 4 * k + 2] = (short) out16;
			out[out_offset + 4 * k + 3] = (short) out16;
		}
	}
}
