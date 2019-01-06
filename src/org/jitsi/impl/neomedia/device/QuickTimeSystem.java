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
package org.jitsi.impl.neomedia.device;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.format.RGBFormat;

import org.jitsi.impl.neomedia.MediaServiceImpl;
import org.jitsi.impl.neomedia.codec.FFmpeg;
import org.jitsi.impl.neomedia.codec.video.AVFrameFormat;
import org.jitsi.impl.neomedia.quicktime.QTCaptureDevice;
import org.jitsi.impl.neomedia.quicktime.QTFormatDescription;
import org.jitsi.impl.neomedia.quicktime.QTMediaType;
import org.jitsi.service.neomedia.MediaType;
import org.jitsi.util.Logger;

/**
 * Discovers and registers QuickTime/QTKit capture devices with JMF.
 *
 * @author Lyubomir Marinov
 */
public class QuickTimeSystem extends DeviceSystem {

	/**
	 * The <tt>Logger</tt> used by the <tt>QuickTimeSystem</tt> class and its
	 * instances for logging output.
	 */
	private static final Logger logger = Logger.getLogger(QuickTimeSystem.class);

	/**
	 * The protocol of the <tt>MediaLocator</tt>s identifying QuickTime/QTKit
	 * capture devices.
	 */
	private static final String LOCATOR_PROTOCOL = LOCATOR_PROTOCOL_QUICKTIME;

	/**
	 * Initializes a new <tt>QuickTimeSystem</tt> instance which discovers and
	 * registers QuickTime/QTKit capture devices with JMF.
	 *
	 * @throws Exception if anything goes wrong while discovering and registering
	 *                   QuickTime/QTKit capture defines with JMF
	 */
	public QuickTimeSystem() throws Exception {
		super(MediaType.VIDEO, LOCATOR_PROTOCOL);
	}

	@Override
	protected void doInitialize() throws Exception {
		QTCaptureDevice[] inputDevices = QTCaptureDevice.inputDevicesWithMediaType(QTMediaType.Video);
		boolean captureDeviceInfoIsAdded = false;

		for (QTCaptureDevice inputDevice : inputDevices) {
			CaptureDeviceInfo device = new CaptureDeviceInfo(inputDevice.localizedDisplayName(),
					new MediaLocator(LOCATOR_PROTOCOL + ':' + inputDevice.uniqueID()),
					new Format[] { new AVFrameFormat(FFmpeg.PIX_FMT_ARGB), new RGBFormat() });

			if (logger.isInfoEnabled()) {
				for (QTFormatDescription f : inputDevice.formatDescriptions()) {
					logger.info("Webcam available resolution for " + inputDevice.localizedDisplayName() + ":"
							+ f.sizeForKey(QTFormatDescription.VideoEncodedPixelsSizeAttribute));
				}
			}

			CaptureDeviceManager.addDevice(device);
			captureDeviceInfoIsAdded = true;
			if (logger.isDebugEnabled())
				logger.debug("Added CaptureDeviceInfo " + device);
		}
		if (captureDeviceInfoIsAdded && !MediaServiceImpl.isJmfRegistryDisableLoad())
			CaptureDeviceManager.commit();
	}
}
