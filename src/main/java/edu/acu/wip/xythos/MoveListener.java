
package edu.acu.wip.xythos;

import com.xythos.common.api.XythosException;
import com.xythos.security.api.Context;
import com.xythos.storageServer.admin.api.AdminUtil;
import com.xythos.storageServer.api.FileSystem;
import com.xythos.storageServer.api.FileSystemDirectory;
import com.xythos.storageServer.api.FileSystemEntry;
import com.xythos.storageServer.api.FileSystemEntryMovedEvent;
import com.xythos.storageServer.api.FileSystemEntryMovedListener;
import com.xythos.storageServer.api.FileSystemUtil;
import com.xythos.storageServer.permissions.api.AccessControlEntry;
import com.xythos.storageServer.permissions.api.DirectoryAccessControlEntry;
import org.apache.log4j.Logger;

/**
 * This class gets added as an ASYNCHRONOUS listener in the Xythos admin interface.
 * It's purpose is to listen for entry moves and perform custom code on the moved entry.
 * 
 * @author cjs00c
 */
public class MoveListener implements FileSystemEntryMovedListener {
	
	/**
	 * Top-level directory that courses are created in
	 */
	public static String coursesPath = "/courses";

	/**
	 * Top-level directory where department shares are located
	 */
	public static String departmentsPath = "/departments";

	/**
	 * Top-level trashcan
	 */
	public static String trashPath = "/trash";
	
	/**
	 * Custom logger for this class
	 */
	public static Logger log = Logger.getLogger(MoveListener.class);

	/**
	 * Executed whenever an entry is moved (including renames and trashes) and forces the entry
	 * to inherit the permissions of it's new parent directory - with a few exceptions. In addition,
	 * moves within course folders are sent to CoursesHandler.handle() to handle specific dropbox
	 * and return scenarios.
	 *
	 * @param moveContext
	 * @param moveEvent
	 */
	public void fileSystemEntryMoved(Context moveContext, FileSystemEntryMovedEvent moveEvent) {
		
		/**
		 * Setup
		 */

		Context adminContext = null;
		FileSystemEntry movedEntry = null;
		try {
			// Get an admin context
			adminContext = AdminUtil.getContextForAdmin("MoveListener");
			// Get moved entry
			movedEntry = FileSystem.findEntry(moveEvent.getVirtualServer(), moveEvent.getToName(), true, adminContext);
		} catch (XythosException e) {
			log.error("Problem occured during setup", e);
		}

		/**
		 * Ignore cases
		 */

		// Ignore permanent deletes
		if (movedEntry == null) {
			log.info("Entry permanently deleted");
			return;
		}
		// Ignore moves to top-level trash
		if (FileSystemUtil.getTopLevelDirectory(movedEntry.getName()).equals(trashPath)) {
			return;
		}
		// Ignore moves to a courses/department trash can
		if (FileSystemUtil.getTopLevelDirectory(movedEntry.getName()).equals(coursesPath) ||
			FileSystemUtil.getTopLevelDirectory(movedEntry.getName()).equals(departmentsPath)) {

			// Must make sure the parent directory is the course/dept trashcan and not
			// a user created "trash" folder. Which means checking the path depth (should
			// be at a depth of 3). Ex:
			//	0		1			2								3
			//		/courses	/0910_ENGL11001 - course name	/Trash
			//		/departments/WIP							/Trash
			String[] movedEntryPaths = movedEntry.getName().split("/");
			if (movedEntryPaths.length >= 3) {
				if (movedEntryPaths[3].equals("Trash")) {
					log.info("Entry trashed: "+movedEntry.getName());
					return;
				}
			}
		}

		/**
		 * Force Inherited Permissions
		 */

		log.info("** Entry moved from " + moveEvent.getFileSystemEntryName() + " to " + moveEvent.getToName() + " **");
		log.info("Forcing Inherited Permissions");

		// Remove entry's current permissions
		removeEntryPermissions(movedEntry);

		// Get parent dir's permissions
		FileSystemDirectory parentDir = null;
		DirectoryAccessControlEntry[] parentDirAccessControls = null;
		try {
			// Get moved entry's parent dir
			String parentName = FileSystemUtil.getParentFullName(moveEvent.getToName());
			parentDir = (FileSystemDirectory) FileSystem.findEntry(moveEvent.getVirtualServer(), parentName, false, adminContext);
			
			// Get entry's parent dir access controls
			parentDirAccessControls = (DirectoryAccessControlEntry[]) parentDir.getPrincipalAccessControlEntries();
			
		} catch (XythosException e) {
			log.error("Problem occured getting the parent directory access controls", e);
		}

		// Go through each of the parent dir's access controls and apply it to the moved entry
		for (DirectoryAccessControlEntry parentAccessControl : parentDirAccessControls) {
			setEntryPermissions(movedEntry, parentAccessControl);
		}

		// Set the entry owner to be the owner of the parent dir
		try {
			movedEntry.setOwner(parentDir.getEntryOwnerPrincipalID());
		} catch (XythosException e) {
			log.error("Problem setting entry owner", e);
		}

		/**
		 * Handle course folder moves
		 */

		// Check for course entries and call CoursesHandler, it takes care of dropbox/returnbox formatting
		if (FileSystemUtil.getTopLevelDirectory(movedEntry.getName()).equals(coursesPath)) {
			log.info("Entry moved in "+coursesPath+" - calling CoursesHandler");
			CoursesHandler.handle(moveEvent.getVirtualServer(), adminContext, movedEntry, moveContext.getContextUser());
		}

		/**
		 * Commit the permission changes
		 */

		try {
			adminContext.commitContext();
		} catch (XythosException e) {
			log.error("Problem committing the admin context", e);
		}
	}
	
	
	public void removeEntryPermissions(FileSystemEntry movedEntry) {
		log.debug("Removing access controls on: " + movedEntry.getName());
		
		try {
			// Get entry's access controls
			AccessControlEntry[] entryAccessControls = movedEntry.getPrincipalAccessControlEntries();
			
			// Remove all the access controls except for the owner
			for (AccessControlEntry accessControl : entryAccessControls) {
				if (!accessControl.getPrincipalID().equals("OWNER@PUBLIC")) {
					//log.debug("- removing entry for: " + accessControl.getPrincipalID());
					movedEntry.deleteAccessControlEntry(accessControl.getPrincipalID());
				}
			}
		} catch (XythosException e) {
			log.error("Problem removing entry's current permissions: " + movedEntry.getName(), e);
		}
	}
	
	
	public void setEntryPermissions(FileSystemEntry movedEntry, DirectoryAccessControlEntry parentAccessControl) {
		
		// Perms moved entry should inherit
		Boolean isReadable = parentAccessControl.isChildInheritReadable();
		Boolean isWritable = parentAccessControl.isChildInheritWriteable();
		Boolean isDeletable = parentAccessControl.isChildInheritDeleteable();
		Boolean isPermissionable = parentAccessControl.isChildInheritPermissionable();
		
		try {
			// If moved entry is a directory it needs to have inherit perms as well
			if (movedEntry instanceof FileSystemDirectory) {
				//log.debug("Setting permissions on the moved entry for: " + parentAccessControl.getPrincipalID());
				//log.debug("- setting READ/INHERIT READ: " + isReadable);
				//log.debug("- setting WRITE/INHERIT WRITE: " + isWritable);
				//log.debug("- setting DELETE/INHERIT DELETE: " + isDeletable);
				//log.debug("- setting ADMIN/INHERIT ADMIN: " + isPermissionable);
				
				// Get access control on moved directory
				DirectoryAccessControlEntry accessControl = (DirectoryAccessControlEntry) movedEntry.getAccessControlEntry(parentAccessControl.getPrincipalID());
				// Set perms
				accessControl.setAccessControlEntry(true, isReadable, isWritable, isDeletable, isPermissionable,
											isReadable, isWritable, isDeletable, isPermissionable);
			}
			// If moved entry is a file it just gets inherited perms from the parent dir
			else {
				//log.debug("Setting permissions on the moved entry for: " + parentAccessControl.getPrincipalID());
				//log.debug("- setting READ: " + isReadable);
				//log.debug("- setting WRITE: " + isWritable);
				//log.debug("- setting DELETE: " + isDeletable);
				//log.debug("- setting ADMIN: " + isPermissionable);
				
				// Get access control on moved file
				AccessControlEntry accessControl = movedEntry.getAccessControlEntry(parentAccessControl.getPrincipalID());
				// Set perms
				accessControl.setAccessControlEntry(isReadable, isWritable, isDeletable, isPermissionable);
			}
		} catch (XythosException e) {
			log.error("Problem setting access control on moved entry", e);
		}
	}
}
