package org.jitsi.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.IncompatibleSourceException;
import javax.media.MediaHandler;
import javax.media.MediaProxy;
import javax.media.NoProcessorException;
import javax.media.PackageManager;
import javax.media.Processor;
import javax.media.protocol.DataSource;

import net.sf.fmj.utility.LoggerSingleton;

public class ForbiddenManager {

	public static final boolean RETHROW_IO_EXCEPTIONS = true;

	private static final Logger logger = LoggerSingleton.logger;

	private static final boolean USE_MEDIA_PREFIX = false;

	private static String[] forbiddenClassNames = { "javax.media.processor.raw.Handler",
			"com.sun.media.processor.raw.Handler", "com.ibm.media.processor.raw.Handler",
			"net.sf.fmj.media.processor.raw.Handler", "javax.media.processor.unknown.Handler" };

	@SuppressWarnings("unchecked")
	public static Vector<String> getProcessorClassList(String contentName) {
		return getClassList(toPackageFriendly(contentName), PackageManager.getContentPrefixList(), "processor",
				"Handler");
	}

	public static Vector<String> getClassList(String contentName, Vector<Object> packages, String component2,
			String className) {
		final Vector<String> result = new Vector<String>();
		if (USE_MEDIA_PREFIX) {
			result.add("media." + component2 + "." + contentName + "." + className);
		}

		for (Object aPackage : packages) {
			// We will do forbidding here!
			String packageName = aPackage + ".media." + component2 + "." + contentName + "." + className;
			if (!Arrays.asList(forbiddenClassNames).contains(packageName)) {
				result.add(aPackage + ".media." + component2 + "." + contentName + "." + className);
			}
		}

		return result;
	}

	private static String toPackageFriendly(String contentName) {
		final StringBuffer b = new StringBuffer();
		for (int i = 0; i < contentName.length(); ++i) {
			final char c = contentName.charAt(i);
			b.append(toPackageFriendly(c));
		}
		return b.toString();
	}

	private static char toPackageFriendly(char c) {
		if (c >= 'a' && c <= 'z')
			return c;
		else if (c >= 'A' && c <= 'Z')
			return c;
		else if (c >= '0' && c <= '9')
			return c;
		else if (c == '.')
			return c;
		else if (c == '/')
			return '.';
		else
			return '_';
	}

	public static Processor createProcessor(DataSource source) throws java.io.IOException, NoProcessorException {
		try {
			return createProcessor(source, source.getContentType());
		} catch (IOException e) {
			logger.log(Level.FINE, "" + e, e);
			if (RETHROW_IO_EXCEPTIONS)
				throw e;

		} catch (NoProcessorException e) { // no need to log, will be logged by call to createProcessor.
		} catch (Exception e) {
			logger.log(Level.FINE, "" + e, e);
		}
		return createProcessor(source, "unknown");

	}

	private static Processor createProcessor(DataSource source, String contentType)
			throws java.io.IOException, NoProcessorException {
		for (String handlerClassName : getProcessorClassList(contentType)) {
			try {
				final Class<?> handlerClass = Class.forName(handlerClassName);
				if (!Processor.class.isAssignableFrom(handlerClass) && !MediaProxy.class.isAssignableFrom(handlerClass))
					continue; // skip any classes that will not be matched
								// below.
				final MediaHandler handler = (MediaHandler) handlerClass.newInstance();
				handler.setSource(source);
				if (handler instanceof Processor) {
					return (Processor) handler;
				} else if (handler instanceof MediaProxy) {
					final MediaProxy mediaProxy = (MediaProxy) handler;
					return createProcessor(mediaProxy.getDataSource());
				}
			} catch (ClassNotFoundException e) {
				logger.finer("createProcessor: " + e); // no need for call stack
				continue;
			} catch (IncompatibleSourceException e) {
				logger.fine("createProcessor(" + source + ", " + contentType + "): " + e); // no need for call stack
				continue;
			} catch (NoProcessorException e) {
				continue; // no need to log, will be logged by call to
							// createProcessor.
			} catch (IOException e) {
				logger.log(Level.FINE, "" + e, e);
				if (RETHROW_IO_EXCEPTIONS)
					throw e;
				else
					continue;
			} catch (NoClassDefFoundError e) {
				logger.log(Level.FINE, "" + e, e);
				continue;
			} catch (Exception e) {
				logger.log(Level.FINE, "" + e, e);
				continue;
			}
		}
		throw new NoProcessorException();
	}
}
