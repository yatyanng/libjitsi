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
package org.jitsi.impl.neomedia;

import java.util.Random;

import org.jitsi.service.neomedia.SSRCFactory;

import net.sf.fmj.media.rtp.GenerateSSRCCause;

/**
 * An <tt>SSRCFactory</tt> implementation which allows the first generated SSRC
 * to be set by the user.
 *
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public class SSRCFactoryImpl implements SSRCFactory {
	private int i = 0;
	private long initialLocalSSRC = -1;

	/**
	 * The <tt>Random</tt> instance used by this <tt>SSRCFactory</tt> to generate
	 * new synchronization source (SSRC) identifiers.
	 */
	private final Random random = new Random();

	public SSRCFactoryImpl(long initialLocalSSRC) {
		this.initialLocalSSRC = initialLocalSSRC;
	}

	private int doGenerateSSRC() {
		return random.nextInt();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long generateSSRC(String cause) {
		// XXX(gp) the problem here is that if the initialLocalSSRC changes,
		// the bridge is unaware of the change. TAG(cat4-local-ssrc-hurricane).
		if (initialLocalSSRC != -1) {
			if (i++ == 0)
				return (int) initialLocalSSRC;
			else if (cause.equals(GenerateSSRCCause.REMOVE_SEND_STREAM.name()))
				return Long.MAX_VALUE;
		}
		return doGenerateSSRC();
	}
}
