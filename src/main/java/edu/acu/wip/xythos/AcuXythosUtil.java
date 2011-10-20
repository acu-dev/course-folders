/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.acu.wip.xythos;

import com.xythos.common.api.VirtualServer;
import com.xythos.common.api.XythosException;
import com.xythos.security.api.Context;
import com.xythos.security.api.UserBase;
import com.xythos.storageServer.admin.api.AdminUtil;
import com.xythos.storageServer.api.FileSystemDirectory;
import com.xythos.storageServer.api.directoryService.DirectoryServiceUser;
import com.xythos.storageServer.api.userTemplates.NewUserTemplate;
import com.xythos.util.api.ServletUtil;

/**
 *
 * @author cjs00c
 */
public class AcuXythosUtil {
	
	public static boolean debug = false;
	
	public static boolean isNewUser(UserBase user) throws XythosException {		
		String f = "AcuXythosUtil: isNewUser: ";
		
		// Create a context for making changes
		Context context = AdminUtil.getContextForAdmin("Checking if " + user.getID() + " is a new user");
		if (debug) System.out.println(f+"looking up home dir: "+user.getHomeDirectoryName());
		FileSystemDirectory homeDir = user.getHomeDirectory(context);
		
		if (homeDir == null){
			if (debug) System.out.println(f+"home dir IS null; user is new");
			return true;
		} else {
			if (debug) System.out.println(f+"home dir is NOT null; user is NOT new");
			return false;
		}
	}
	
	public static void setupNewUser(UserBase user) throws XythosException {
		
		String f = "AcuXythosUtil: setupNewUser: ";
		Context context = null;
		try {
			// Create a context for making changes
			context = AdminUtil.getContextForAdmin("Create user home " + user.getID());
			
			NewUserTemplate userTemplate = null;
			String userStatus = getUserStatus(user);
			if (debug) System.out.println(f+"User status found: "+userStatus);
			if (userStatus.equalsIgnoreCase("facstaff") || userStatus.equalsIgnoreCase("faculty") || userStatus.equalsIgnoreCase("staff")){
				// Use the 'Employees' new user template if the user has the role of 'facstaff'
				if (debug) System.out.println(f+"User is an employee, using the 'Employees' template");
				userTemplate = NewUserTemplate.find("Employees", context);
			} else {
				// Otherwise use the default new user template
				if (debug) System.out.println(f+"User is not an employee, using the default template");
				userTemplate = NewUserTemplate.getDefaultTemplate(context);
			}
			
			String homeDir = user.getHomeDirectoryName();
			if (debug) System.out.println(f+user.getID()+" home dir is: "+homeDir);
			//VirtualServer homeDirVirtualServer = user.getHomeDirectoryVirtualServer();
			VirtualServer vs = VirtualServer.getDefaultVirtualServer();
			if (debug) System.out.println(f+"virtual server: "+vs.getName());

			// Setup user with default user template
			if(userTemplate != null){
				
				if (debug) System.out.println(f+"new user default template found, using it for the new user");
				String parentPath = userTemplate.getHomeDirectoryParentPath();
				
				// Create home dir if it doesn't exist
				if (homeDir == null){
					homeDir = parentPath+"/"+user.getID();
					if (debug) System.out.println(f+"home dir was null; setting it to: "+homeDir);
				}
				
				String homeDirPath = ServletUtil.getBaseName(homeDir);
				if (debug) System.out.println(f+"parent path: "+parentPath);
				if (debug) System.out.println(f+"home path: "+homeDirPath);
				
				// Add a slash at the beginning if its missing
				if(!parentPath.startsWith("/")){
					parentPath = "/" + parentPath;
				}
				
				
				String fullHomePath = parentPath + "/" + homeDirPath;
				if (debug) System.out.println(f+"full home path: "+fullHomePath);
				
				
				// Set the users homedir to the real path
				if(!homeDir.equals(fullHomePath)) {
					if (debug) System.out.println(f+"setting user's home dir to: "+fullHomePath);
					user.setHomeDirectory(fullHomePath, vs);
				}
				
				// Create the default directory for a new user
				if (debug) System.out.println(f+"creating default directory for new user");
				userTemplate.getHomeDirectoryTemplate().createForNewUser(vs, user, homeDirPath, context, parentPath);
			}
			
			// No default user template; use basic settings
			else {
				//Create the home directory for the user
				if (debug) System.out.println(f+"no new user default template found, using basic settings");				
				/*CreateTopLevelDirectoryData createData = 
					new CreateTopLevelDirectoryData(
									homeDirVirtualServer, homeDir, Parameters.getNewUserDocumentStore(), 
									user.getPrincipalID());
				createData.setBandwidthQuota(Parameters.getNewUserBandwidthQuotaBytes());
				createData.setQuota(Parameters.getNewUserDirectoryQuotaMBytes()*1024*1024);
				FileSystem.createTopLevelDirectory(createData, context);*/
			}

			if (debug) System.out.println(f+"Commiting the context");
			context.commitContext();
			context = null;
		} finally {
			if (context != null) {
				context.rollbackContext();   
			}
		}
		if (debug) System.out.println(f+"Home directory created for user " + user.getID());
	}
	
	private static String getUserStatus(UserBase user) {
		if (debug) System.out.println("Getting user status...");
		
		String userStatus = null;
		
		if (user instanceof DirectoryServiceUser){
			try {
				DirectoryServiceUser dsUser = (DirectoryServiceUser) user;
				userStatus = dsUser.getNewUserTemplateMappingValue();
				if (debug) System.out.println("User status: "+userStatus);
			} catch (XythosException e) {
				System.out.println("ERROR: AcuXythosUtil.getUserStatus(): Problem getting the user's template mapping value - Giving the user student status");
			}
		} else {
			System.out.println("ERROR: AcuXythosUtil.getUserStatus(): User is not a directory service user - Giving the user student status");
		}

        // Check for valid status
        if (userStatus == null || userStatus.equals(""))
            userStatus = "student";
		
		return userStatus;
	}
}















