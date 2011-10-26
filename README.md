This project requires a persistence.xml file be installed at {xythos-base}/custom/classes/META-INF/persistence.xml.
Example file:

	<?xml version="1.0" encoding="UTF-8"?>
	<persistence version="1.0" xmlns="http://java.sun.com/xml/ns/persistence"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd">

		<persistence-unit name="acu-model-custom" transaction-type="RESOURCE_LOCAL">
			<provider>org.hibernate.ejb.HibernatePersistence</provider>
			<class>edu.acu.wip.model.Account</class>
			<class>edu.acu.wip.model.BlackboardCopy</class>
			<class>edu.acu.wip.model.CalendarCategory</class>
			<class>edu.acu.wip.model.CampusCalendar</class>
			<class>edu.acu.wip.model.Course</class>
			<class>edu.acu.wip.model.CourseContact</class>
			<class>edu.acu.wip.model.CourseRank</class>
			<class>edu.acu.wip.model.CourseTool</class>
			<class>edu.acu.wip.model.CourseToolCopy</class>
			<class>edu.acu.wip.model.Dropbox</class>
			<class>edu.acu.wip.model.OfficeHours</class>
			<class>edu.acu.wip.model.OfficeLocation</class>
			<class>edu.acu.wip.model.Person</class>
			<class>edu.acu.wip.model.Role</class>
			<class>edu.acu.wip.model.Tool</class>
			<properties>
				<property name="hibernate.connection.url" value="jdbc:mysql://host:3306/database"/>
				<property name="hibernate.connection.driver_class" value="com.mysql.jdbc.Driver"/>
				<property name="hibernate.connection.username" value="user"/>
				<property name="hibernate.connection.password" value="pass"/>
				<property name="hibernate.cache.provider_class" value="org.hibernate.cache.NoCacheProvider"/>
				<property name="hibernate.dialect" value="org.hibernate.dialect.MySQLDialect"/>
			</properties>
		</persistence-unit>
		<persistence-unit name="acu-banner" transaction-type="RESOURCE_LOCAL">
			<provider>org.hibernate.ejb.HibernatePersistence</provider>
			<class>edu.acu.wip.model.banner.CurrentTerm</class>
			<class>edu.acu.wip.model.banner.Schedule</class>
			<properties>
				<property name="hibernate.connection.url" value="jdbc:oracle:thin:@host:1531:database"/>
				<property name="hibernate.connection.driver_class" value="com.mysql.jdbc.Driver"/>
				<property name="hibernate.connection.username" value="user"/>
				<property name="hibernate.connection.password" value="pass"/>
				<property name="hibernate.cache.provider_class" value="org.hibernate.cache.NoCacheProvider"/>
				<property name="hibernate.dialect" value="org.hibernate.dialect.OracleDialect"/>
			</properties>
		</persistence-unit>
	</persistence>

That is all.