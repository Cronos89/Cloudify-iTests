package framework.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;


public class MailReporterProperties {

    final static String USERNAME_PROP    = "username";
    final static String PASSWORD_PROP    = "password";

    final static String MAIL_HOST = "mailHost";
    final static String RECIPIENTS = "recipients";
    final static String WEBUI = "webui";
    final static String SG = "sg";
    final static String ESM = "esm";
    final static String SECURITY = "security";
    final static String CLOUDIFY = "cloudify";
    final static String WAN = "wan";
    
    private final Properties props;

    public MailReporterProperties(Properties props) {
        this.props = props;
    }

    public String getUsername() {
        return props.getProperty(USERNAME_PROP);
    }

    public String getPassword() {
        return props.getProperty(PASSWORD_PROP);
    }

    public String getMailHost() {
        return  props.getProperty(MAIL_HOST);
    }

    public List<String> getRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(RECIPIENTS), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }
    
    public List<String> getWebUIRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(WEBUI), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }
    
    public List<String> getSGRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(SG), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }
    
    public List<String> getESMRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(ESM), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }
    
    public List<String> getSecurityRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(SECURITY), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }
    
    public List<String> getCloudifyRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(CLOUDIFY), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }
    
    public List<String> getWanRecipients() {
        List<String> _recipients = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(props.getProperty(WAN), ",");
        while(st.hasMoreTokens() == true){
            _recipients.add(st.nextToken());
        }

        return _recipients;
    }

}
