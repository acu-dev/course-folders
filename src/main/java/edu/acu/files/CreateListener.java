
package edu.acu.files;

import com.xythos.common.api.XythosException;
import com.xythos.security.api.Context;
import com.xythos.storageServer.admin.api.AdminUtil;
import com.xythos.storageServer.api.FileSystem;
import com.xythos.storageServer.api.FileSystemDirectory;
import com.xythos.storageServer.api.FileSystemEntry;
import com.xythos.storageServer.api.FileSystemEntryCreatedEvent;
import com.xythos.storageServer.api.FileSystemEntryCreatedListener;
import com.xythos.storageServer.api.FileSystemUtil;
import com.xythos.storageServer.permissions.api.DirectoryAccessControlEntry;
import org.apache.log4j.Logger;

/**
 *
 * @author cjs00c
 */
public class CreateListener implements FileSystemEntryCreatedListener {
		
	/**
	 * Top-level directory that courses are created in
	 */
	public static String coursesDir = "/courses";
	
	/**
	 * Custom logger for this class
	 */
	public static Logger log = Logger.getLogger(CreateListener.class);
	
	
	/**
	 * Executed whenever an entry is created (including copy). Creates within course
	 * folders are sent to CoursesHandler.handle() to handle specific dropbox
	 * and return scenarios.
	 *
	 * @param moveContext
	 * @param createEvent
	 */
	public void fileSystemEntryCreated(Context createContext, FileSystemEntryCreatedEvent createEvent) throws XythosException {
		log.info("** Entry Created: " + createEvent.getFileSystemEntryName() + " **");
		log.info("Entry source class: "+createEvent.getFileSystemEntryClass().getName());
		
		// Delete mac hidden files (.DS_Store)
		/*
		if (FileSystemUtil.getBaseName(createEvent.getFileSystemEntryName()).equals(".DS_Store") ||
				FileSystemUtil.getBaseName(createEvent.getFileSystemEntryName()).substring(0, 2).equals("._")) {
			try {
				FileSystemEntry createdEntry = FileSystem.findEntry(createEvent.getVirtualServer(), createEvent.getFileSystemEntryName(), false, createContext);
				if (createdEntry != null && !(createdEntry instanceof FileSystemDirectory)) {
					log.debug("Entry is a mac hidden file (.DS_Store or ._filename); deleting it (sending it to user's trash)");
					createdEntry.delete();
				}
			} catch (XythosException e) {
				log.error("Unable to delete entry: " + createEvent.getFileSystemEntryName(), e);
			}
		}
		*/
		 
		// Check for course entries
		//else if (FileSystemUtil.getTopLevelDirectory(createEvent.getFileSystemEntryName()).equals(coursesDir)) {
		if (!FileSystemUtil.getTopLevelDirectory(createEvent.getFileSystemEntryName()).equals(coursesDir)) {
			return;
		}


		// Setup context
		Context adminContext = null;
		try {
			// If current context is admin then just use it
//			if (createContext.isAdminSecurityManager()) {
//				log.debug("Context is admin already, using it");
//				adminContext = createContext;
//			} else {
//				// Commit the create context first
//				log.debug("Commiting the initial create context");
//				createContext.commitContext();

				// Get an admin context
				log.debug("Getting a new admin context");
				adminContext = AdminUtil.getContextForAdmin("CreateListener");
			//}
		} catch (XythosException e) {
			log.error("Problem setting up the context", e);
		}



		FileSystemEntry createdEntry = null;
		try {
			// Get created entry
			createdEntry = FileSystem.getEntry(createEvent.getVirtualServer(), createEvent.getFileSystemEntryName(), false, adminContext);
			//log.debug("Created entry has locks: " + createdEntry.hasExistingLocks());
			//log.debug("Created entry size: " + createdEntry.getEntrySize());
		} catch (XythosException e) {
			log.error("Problem getting the created entry", e);
		}

		// Only continue if the entry exists
		if (createdEntry == null) {
			log.warn("Created entry is null; something is wrong");
			return;
		}

//		// Handle trashcans separately
//		String[] createdEntryPaths = createdEntry.getName().split("/");
//		if (createdEntryPaths.length == 4) {
//			// Must make sure the parent directory is the course/dept trashcan and not
//			// a user created "trash" folder. Which means checking the path depth (should
//			// be at a depth of 3). Ex:
//			//	0		1			2								3
//			//		/courses	/0910_ENGL11001 - course name	/Trash
//			//		/departments/WIP							/Trash
//			if (createdEntryPaths[3].equals("Trash")) {
//				log.info("Trashcan created, applying permissions");
//				// Get parent dir's permissions
//				FileSystemDirectory parentDir = null;
//				DirectoryAccessControlEntry[] parentDirAccessControls = null;
//				try {
//					// Get trashcan's parent dir
//					String parentName = FileSystemUtil.getParentFullName(createdEntry.getName());
//					parentDir = (FileSystemDirectory) FileSystem.findEntry(createEvent.getVirtualServer(), parentName, false, adminContext);
//
//					// Get entry's parent dir access controls
//					parentDirAccessControls = (DirectoryAccessControlEntry[]) parentDir.getPrincipalAccessControlEntries();
//
//				} catch (XythosException e) {
//					log.error("Problem occured getting the parent directory access controls", e);
//				}
//
//				// Go through each of the parent dir's access controls and apply it to the moved entry
//				for (DirectoryAccessControlEntry parentAccessControl : parentDirAccessControls) {
//					try {
//						if (!parentAccessControl.getPrincipalID().equals("ALL_PRINCIPALS@PUBLIC")) {
//							// Get access control on moved directory
//							DirectoryAccessControlEntry accessControl = (DirectoryAccessControlEntry) createdEntry.getAccessControlEntry(parentAccessControl.getPrincipalID());
//							// Set perms (recursive, read, write, delete, admin, inherit read, inherit write, inherit delete, inherit admin)
//							accessControl.setAccessControlEntry(false, true, true, true, false, false, true, true, false);
//						}
//					} catch (XythosException e) {
//						log.error("Problem setting access control on moved entry", e);
//					}
//				}
//			}
//		}
//
//		// Created entry is not a trashcan so pass to CoursesHandler
//		else {
			log.info("Passing to CoursesHandler.handle()");
			CoursesHandler.handle(createEvent.getVirtualServer(), adminContext, createdEntry, createContext.getContextUser());
//		}

		// Commit the permission changes
		try {
			log.debug("Commiting the admin context");
			adminContext.commitContext();
		} catch (XythosException e) {
			log.error("Problem committing the admin context", e);
		}
	}
}
