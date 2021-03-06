package com.meidusa.amoeba.config;

import java.util.Properties;

/**
 * text是一个类似${user.dir}/conf/access_list.conf这样的字符串，
 * 对这个目录进行处理，最终得到对应文件的在工程文件中的路径
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 * @author hexianmao
 */
public class ConfigUtil {
	/**
	 * 得到text中保持的文件在工程文件中的路径
	 * @param text
	 * @return
	 * @throws ConfigurationException
	 */
    public static String filter(String text) throws ConfigurationException {
        return filter(text, System.getProperties());
    }
    /**
     * 得到text中保持的文件在工程文件中的路径
     * @param text
     * @param properties
     * @return
     * @throws ConfigurationException
     */
    public static String filter(String text, Properties properties) throws ConfigurationException {
        // String result = "";
        StringBuilder result = new StringBuilder();
        int cur = 0;
        int textLen = text.length();
        int propStart = -1;
        int propStop = -1;
        String propName = null;
        String propValue = null;
        for (; cur < textLen; cur = propStop + 1) {
            propStart = text.indexOf("${", cur);
            if (propStart < 0) {
                break;
            }
            result.append(text.substring(cur, propStart));
            // result = result + text.substring(cur, propStart);
            propStop = text.indexOf("}", propStart);
            if (propStop < 0) {
                throw new ConfigurationException("Unterminated property: " + text.substring(propStart));
            }
            propName = text.substring(propStart + 2, propStop);
            propValue = properties.getProperty(propName);
            if (propValue == null) {
                throw new ConfigurationException("No such property: " + propName);
            }
            result.append(propValue);
            // result = result + propValue;
        }

        // result = result + text.substring(cur);
        return result.append(text.substring(cur)).toString();
    }
}
