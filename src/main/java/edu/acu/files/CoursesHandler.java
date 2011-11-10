
package edu.acu.files;

import com.xythos.common.api.VirtualServer;
import com.xythos.common.api.XythosException;
import com.xythos.security.api.Context;
import com.xythos.security.api.PrincipalManager;
import com.xythos.security.api.UserBase;
import com.xythos.storageServer.api.FileSystem;
import com.xythos.storageServer.api.FileSystemDirectory;
import com.xythos.storageServer.api.FileSystemEntry;
import com.xythos.storageServer.api.FileSystemUtil;
import com.xythos.storageServer.permissions.api.AccessControlEntry;
import com.xythos.storageServer.permissions.api.DirectoryAccessControlEntry;
import org.apache.log4j.Logger;

/**
 *
 * @author cjs00c
 */
public class CoursesHandler {
	
	/**
	 * Directory where dropbox entries will be monitored
	 */
	public static String dropboxDir = "Dropbox";
	
	/**
	 * Directory where returned entries will be monitored
	 */
	public static String returnDir = "Return";
	
	/**
	 * Directory where student boxes are (including '/'; ex: '@ Students/')
	 */
	public static String studentsDir = "Students/";
	
	/**
	 * Text to be appended to a file when it is returned
	 */
	public static String returnedStatus = "_RETURNED";
	
	/**
	 * Custom logger for this class
	 */
	public static Logger log = Logger.getLogger(CoursesHandler.class);
	
	public static VirtualServer vs = null;
	/**
	 * Handles entries in the dropbox root folder. 
	 * 
	 * @param adminContext
	 * @param entry
	 */
	public static void handle(VirtualServer virtualServer, Context adminContext, FileSystemEntry entry, UserBase user) {

		vs = virtualServer;
		
		// Break up the entry path into sections
		String[] entryParts = entry.getName().split("/");
		
		// Only handle entries in the dropbox and return root folders
		// Which means the entry path will have 5 parts:
		// 0		1			2				3						4
		//		/courses	/<course_title>	/<dropbox or return>	/<entry_name>
		if (entryParts.length == 5){
			
			// Check if the entry is in the dropbox directory
			if (entryParts[3].equals(dropboxDir)){
				dropboxHandler(adminContext, entry, user);
			}
			
			// Check if the entry is in the returnbox directory
			else if (entryParts[3].equals(returnDir)){
				returnHandler(adminContext, entry);
			}
		}
	}
	
	

	private static void dropboxHandler(Context adminContext, FileSystemEntry entry, UserBase user) {
		log.info("** Handling dropbox entry: " + entry.getName() + " **");
		
		// Get the student user from entry name when context user is admin (nobody)
		if (adminContext.getContextUser().getPrincipalID().equals(user.getPrincipalID())) {
			log.debug("Admin move; trying to get the userID from the entry name");
			if (entry.getName().contains("_")) {
				String userID = FileSystemUtil.getBaseName(entry.getName()).substring(0, FileSystemUtil.getBaseName(entry.getName()).indexOf("_"));
				log.debug("Using userID: " + userID);
				UserBase student = null;
				try {
					student = PrincipalManager.findUser(userID, VirtualServer.getDefaultVirtualServer().getName());
				} catch (XythosException e) {
					log.error("Problem looking up user: " + userID, e);
				}
				if (student != null)
					user = student;
			}
		}
		
		// Give user r,w,d perms
		try {
			if (entry instanceof FileSystemDirectory) {
				log.debug("Entry is a directory: " + entry.getName());
				log.debug("Giving student " + user.getID() + " READ/INHERIT READ, WRITE/INHERIT WRITE, DELTE/INHERIT DELETE permissions");
				DirectoryAccessControlEntry accessControl = (DirectoryAccessControlEntry) entry.getAccessControlEntry(user.getPrincipalID());
				accessControl.setAccessControlEntry(true, true, true, true, false, true, true, true, false);
			} else {
				log.debug("Entry is a file: " + entry.getName());
				log.debug("Giving student " + user.getID() + " READ, WRITE, DELETE permissions");
				AccessControlEntry accessControl = entry.getAccessControlEntry(user.getPrincipalID());
				accessControl.setAccessControlEntry(true, true, true, false);
			}
		} catch (XythosException e) {
			log.error("Unable to give student r,w,d permissions on entry", e);
		}
		
		// Make sure entry name is correct
		try {
			// Get a correctly formatted entry name
			String formattedName = dropboxEntryFormat(FileSystemUtil.getBaseName(entry.getName()), user.getID());

			// Move entry if the name isn't correct
			if (!formattedName.equals(FileSystemUtil.getBaseName(entry.getName()))) {
				log.info("Renaming entry to: " + formattedName);
				entry.move(FileSystemUtil.getParentFullName(entry.getName()), formattedName, true);
			} else {
				log.debug("Entry name is already formatted");
			}
		} catch (XythosException e) {
			log.error("Problem formatting the entry name", e);
		}
	}
	
	

	private static void returnHandler(Context adminContext, FileSystemEntry entry) {
		log.info("** Handling return entry: " + entry.getName() + " **");
		
		// Try to get student box
		String studentBoxPath = getStudentBox(adminContext, entry.getName());
		
		// Only continue if there's somewhere to move the entry to
		if (studentBoxPath != null) {
			
			// Append the returned status to the entry name
			String formattedEntryName = returnEntryFormat(FileSystemUtil.getBaseName(entry.getName()));
			
			// Move the entry to the formatted name in the student box
			log.info("Moving to " + studentBoxPath + "/" + formattedEntryName);
			try {
				entry.move(studentBoxPath, formattedEntryName, true);
			} catch (XythosException e) {
				log.error("Problem moving entry to student box", e);
			}
		}
	}
	
	
	/**
	 * Formats an entry name correctly. Entries should be named with the
	 * owner ID prepending the entry name. For example: cjs00c_myfile.txt
	 * 
	 * @param event
	 * @param entryName
	 * @return
	 */
	private static String dropboxEntryFormat(String entryName, String userID) {
		/**
		 * userID may be NOBODY once the owner has changed.... how can I deal with that?
		 */
		
		// Append the userID to the front of the entry name
		String formattedName = userID + "_" + entryName;

		// Check if the entry name is long enough to already contain a userID
		if (entryName.length() > 7){
			
			// Find out if the entry name contains an underscore
			if (entryName.contains("_")){

				String subEntryName = entryName.substring(0,entryName.indexOf("_"));
				log.debug("Possible user ID: "+subEntryName);
			
				// Compare the entry name against the userID
				if (subEntryName.equals(userID)){
					log.debug("User ID is correct");
					// If the entry name already has a userID then don't change
					formattedName = entryName;
				}
			}
		}
		log.debug("Formatted entry name: "+formattedName);

		return formattedName;
	}
	
	
	/**
	 * Formats an entry name to contain the returned status between
	 * the entry name and the extension (if it has one)
	 * 
	 * @param entryName
	 * @return
	 */
	private static String returnEntryFormat(String entryName){
		
		// Setup entry parts
		String begEntryName = entryName;
		String endEntryName = "";
		
		// Look for a file exention
		if (entryName.contains(".")){
			begEntryName = entryName.substring(0, entryName.lastIndexOf("."));
			endEntryName = entryName.substring(entryName.lastIndexOf("."), entryName.length());
			log.debug("Entry name: "+begEntryName+" has extention: "+endEntryName);
		}
		
		// Look for a pre-existing returned status
		if (begEntryName.substring(begEntryName.length()-returnedStatus.length(), begEntryName.length()).equals(returnedStatus)){
			log.debug("Entry name already has a returned status; returning the original entry name");
			return entryName;
		} else {
			log.debug("Modifying the entry name to included the returned status");
			return begEntryName+returnedStatus+endEntryName;
		}
	}
	
	
	
	public static String getStudentBox(Context adminContext, String entryPath) {
		log.debug("Looking up the student box for: " + entryPath);
		
		// Break up the entry path into sections
		String[] entryParts = entryPath.split("/");
		String entryName = entryParts[entryParts.length-1];
		
		if (entryName.contains("_")) {
			
			// Get prepended username from entry name
			String userID = entryName.substring(0, entryName.indexOf("_"));
			log.debug("Using userID: " + userID);
			
			// Look up student's name
			String studentName = null;
			try {
				UserBase student = PrincipalManager.findUser(userID, VirtualServer.getDefaultVirtualServer().getName());
				if (student != null){
					studentName = student.getDisplayName();
					log.debug("Student user found; student's name: " + studentName);
				} else {
					log.info("Unable to find user " + userID + " in system; is userID correct?");
					return null;
				}
			} catch (XythosException e) {
				log.error("Unable to look up student's name", e);
			}
			
			// Create student box path (ex: '/courses/<course_name>/<student_dir>/cjs00c - Chris Gibbs')
			String studentBoxPath = "/"+entryParts[1]+"/"+entryParts[2]+"/"+studentsDir+userID+" - "+studentName;
			log.debug("Using student box path: " + studentBoxPath);
			
			// Look up student box
			try {
				FileSystemEntry studentBox = FileSystem.findEntry(vs, studentBoxPath, false, adminContext);
				if (studentBox != null) {
					log.debug("Path is good; returning it");
					return studentBoxPath;
				} else {
					log.info("Unable to locate student box; is the student a member of this class?");
					return null;
				}
			} catch (XythosException e) {
				log.error("Problem finding student box", e);
				return null;
			}
			
		} else {
			log.info("Entry name is malformatted; unable to find a username");
			return null;
		}
	}
}
