package sa.elm.ob.utility.email;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

/**
 * This class reads Free Marker templates for Sending Emails
 * @author mrahim
 *
 */

public class EmailTemplateReader implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8645766539043107227L;
	private static final String TEMPLATE_DIR = "C:/ELMWORK" ; 
	
	/**
	 * Returns the Formatted message against free marker template
	 * @param templateId
	 * @param input
	 * @return
	 * @throws IOException 
	 */
	public String getFormattedMessage (String templateId, Map<String, String> input) throws Exception {
		
		// Get the configuration
		Configuration cfg = getFreeMarkerConfiguration(TEMPLATE_DIR);
		// Get the Template
		Template template = cfg.getTemplate(templateId);
		// Get String writer to get output as String
		StringWriter stringWriter = new StringWriter();
		template.process(input, stringWriter);
		
		return stringWriter.toString();
		
	}
	
	/**
	 * Get the Free Marker Configuration
	 * @param templateDirectory
	 * @return
	 * @throws IOException
	 */
	private  Configuration getFreeMarkerConfiguration (String templateDirectory) throws IOException {
		
		Configuration cfg = new Configuration();
		//cfg.setDirectoryForTemplateLoading(new File(templateDirectory));
		cfg.setClassForTemplateLoading(this.getClass(), "");
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);	
	    
		return cfg;
	}
	
	
	public static void main (String [] args) throws Exception{
		
		//EmailTemplateReader.getFormattedMessage("test.ftl", null);
	}

}
