package net.sf.jgcs.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

public class FactoryUtil {

	Logger logger = Logger.getLogger(FactoryUtil.class.getName());

	private Properties properties = null;

	public FactoryUtil(String propertiesFile) throws FileNotFoundException,
			IOException {
		properties = new Properties();
		properties.load(new FileInputStream(propertiesFile));
	}

	@SuppressWarnings("unchecked")
	public Object getInstance(String property) {

		String startSequence = property + ".";

		String constructorParam = properties.getProperty(property);

		boolean useStringConstructor = constructorParam != null;

		String typeProperty = startSequence + "type";
		String type = properties.getProperty(typeProperty);

		if (type == null) {
			logger.info("Class not found in property file. Return null...");
			return null;
		}

		Class clazz;
		Object o = null;
		try {
			clazz = Class.forName(type);

			Constructor c = null;

			if (useStringConstructor) {
				c = clazz.getConstructor(String.class);
				o = c.newInstance(constructorParam);
			} else {
				c = clazz.getConstructor();
				o = c.newInstance();
			}

			Enumeration<String> propertyNames = (Enumeration<String>) properties
					.propertyNames();
			while (propertyNames.hasMoreElements()) {
				String propertyName = propertyNames.nextElement();
				if (propertyName.startsWith(startSequence)
						&& !propertyName.equals(typeProperty)) {
					String attr = propertyName.substring(
							startSequence.length(), propertyName.length());
					String value = properties.getProperty(propertyName);

					String setMethod = "set"
							+ attr.substring(0, 1).toUpperCase()
							+ attr.substring(1);

					Method method = clazz.getMethod(setMethod, String.class);
					method.invoke(o, value);

				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return o;
	}
}
