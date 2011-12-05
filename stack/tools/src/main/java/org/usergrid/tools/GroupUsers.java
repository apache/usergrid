package org.usergrid.tools;

import static org.usergrid.utils.MapUtils.hashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.Entity;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.SimpleEntityRef;
import org.usergrid.persistence.entities.User;

public class GroupUsers extends ToolBase {

	private static final Logger logger = LoggerFactory
			.getLogger(GroupUsers.class);

	@Override
	@SuppressWarnings("static-access")
	public Options createOptions() {

		Option appOption = OptionBuilder.withArgName("app").hasArg()
				.withDescription("Usergrid app").create("app");

		Options options = super.createOptions();
		options.addOption(appOption);

		return options;
	}

	@Override
	public void runTool(CommandLine line) throws Exception {
		startSpring();

		setVerbose(line);

		String appName = line.getOptionValue("app");
		sampleAllUsers(appName, 1000);
	}

	public void sampleAllUsers(String appName, int sample_count)
			throws Exception {

		UUID applicationId = emf.lookupApplication(appName);
		EntityManager em = emf.getEntityManager(applicationId);

		int total_count = em.getRelationManager(em.getApplicationRef())
				.getCollectionSize("users");

		if (total_count == 0) {
			logger.info("No results found, probably offline");
			return;
		}

		logger.info("Total count: " + total_count);

		int sample_interval = total_count / sample_count;
		logger.info("Skip count: " + sample_interval);

		int retrieve_count = ((10000 / sample_interval) + 1) * sample_interval;
		logger.info("Retrieve count count: " + retrieve_count);

		List<UUID> users = new ArrayList<UUID>(sample_count);
		int i = 0;
		Results r = null;
		do {
			r = em.getCollection(em.getApplicationRef(), "users",
					r != null ? r.getNextResult() : null, retrieve_count,
					Results.Level.IDS, false);
			for (int j = 0; j < r.size(); j += sample_interval) {
				UUID uuid = r.getIds().get(j);
				if (i < sample_count) {
					logger.info(i + ":" + uuid);
					users.add(uuid);
					i++;
				}
			}
		} while (r.hasMoreResults());

		String group_name = "random_" + sample_count + "_"
				+ System.currentTimeMillis();
		logger.info("Group: " + group_name);
		Entity group = em.create("group", hashMap("path", (Object) group_name));

		for (UUID uuid : users) {
			em.addToCollection(group, "users", new SimpleEntityRef(
					User.ENTITY_TYPE, uuid));
		}
	}

}
