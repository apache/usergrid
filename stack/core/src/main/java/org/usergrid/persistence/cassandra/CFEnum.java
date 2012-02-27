package org.usergrid.persistence.cassandra;

import java.util.List;

import me.prettyprint.hector.api.ddl.ColumnDefinition;

public interface CFEnum {

	public String getColumnFamily();

	public String getComparator();

	public String getValidator();

	public boolean isComposite();

	public List<ColumnDefinition> getMetadata();

	public boolean create();

}