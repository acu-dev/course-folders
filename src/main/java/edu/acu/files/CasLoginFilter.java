
package edu.acu.files;

import com.opensymphony.xwork2.ActionContext;
import com.xythos.common.api.XythosException;
import com.xythos.security.api.PrincipalManager;
import com.xythos.security.api.SessionManager;
import com.xythos.security.api.UserBase;
import com.xythos.webui.WebuiUtil;
import com.xythos.webview.WebviewUtil;
import com.xythos.webview.XythosAction;
import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * Looks for CAS login credentials. If none are found user is redirected to CAS login.
 * If credentials are found, a Xythos session is created for the user.
 *
 * @author cjs00c
 */
public class CasLoginFilter implements Filter {
	
	/**
	 * Custom logger will output to /usr/local/xythos/logs/cas_login.log
	 */
	public static Logger log = Logger.getLogger(CasLoginFilter.class);
	
	public void init(FilterConfig config){}
	
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws ServletException, IOException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		
		String username = request.getRemoteUser();
		request.getSession().setAttribute("edu.acu.wip.xythos.username", username);
		log.debug("Found CAS username: "+username);

		// User is authenticated with CAS
		if (username != null) {
			
			// Find the Xythos user
			UserBase user = null;
			try {
				user = PrincipalManager.findUser(username, null);
			} catch (XythosException e) {
				log.error("Problem finding user: "+username, e);
			}

			
			if (user != null) {
				log.debug("Found Xythos user: "+username);
				
				try {
					if (AcuXythosUtil.isNewUser(user)){
						AcuXythosUtil.setupNewUser(user);
					}
				} catch (XythosException e){
					log.error("Problem setting up new user", e);
				}
				
				String sessionID = null;
				try {
					sessionID = SessionManager.findValidSession(user);
					if (sessionID == null)
						sessionID = SessionManager.createSession(user, request);
				} catch (XythosException e) {
					log.error("something bad happened: "+e);
				}
				SessionManager.addSessionToResponse(request, response, sessionID);
				ActionContext.getContext().put(XythosAction.USER_BASE, user); 
				ActionContext.getContext().put(XythosAction.SESSION_ID, sessionID); 
				ActionContext.getContext().put(XythosAction.SECURITY_TOKEN, WebuiUtil.CALC_SECURITY_TOKEN_BE_CAREFUL(sessionID));
				String sToken = WebviewUtil.getSecurityToken(sessionID);

				// Redirect the User
				try {
					String location = request.getParameter("location");
					String vars = "?stk="+sToken;
					if (location != null && !location.equals("")) {
						location = URLEncoder.encode(location, "UTF-8");
						vars = "?entryName="+location+"&stk="+sToken;
					}
					response.sendRedirect("/xythoswfs/webview/fileManager.action"+vars);
					log.info("Successfully authenticated: "+username);
				} catch (IOException e){
					log.error("Problem redirecting...");
				}
			} else {
				log.error("Could not find user: "+username);
			}
		} 

		// User is not authenticated with CAS
		else {
			try {
				response.sendRedirect("/");
			} catch (IOException e){
				log.error("Problem redirecting...");
			}
		}
	}

	public void destroy(){}
}
