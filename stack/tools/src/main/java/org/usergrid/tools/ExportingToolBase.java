package org.usergrid.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.impl.DefaultPrettyPrinter;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author zznate
 */
public abstract class ExportingToolBase extends ToolBase {

  protected Logger logger = LoggerFactory.getLogger(ExportingToolBase.class);

  protected static File outputDir;

 	/** Output dir option: -outputDir */
 	protected static final String OUTPUT_DIR = "outputDir";

  protected String baseOutputDirName = "export";

  JsonFactory jsonFactory = new JsonFactory();

  @Override
 	@SuppressWarnings("static-access")
 	public Options createOptions() {

    Options options = super.createOptions();

 		Option outputDir = OptionBuilder.hasArg()
 				.withDescription("output file name -outputDir")
 				.create(OUTPUT_DIR);

 		options.addOption(outputDir);

 		return options;
 	}

  protected void prepareBaseOutputFileName(CommandLine line) {

 		boolean hasOutputDir = line.hasOption(OUTPUT_DIR);

 		if (hasOutputDir) {
 			baseOutputDirName = line.getOptionValue(OUTPUT_DIR);
 		}
 	}



  /**
   * Write the string onto the writer and check if verbose is enabled to log
   * also an echo of what is being written to the writer.
   *
   * @param out
   *            PrintWriter
   * @param content
   *            string to be written
   */
  @SuppressWarnings("unused")
  protected void writeOutput(PrintWriter out, String content) {
    echo(content);
    out.print(content);

  }

  protected File createOutputParentDir() {
    return createDir(baseOutputDirName);
  }

  protected File createOutputFile(String type, String name) {
    return new File(outputDir, prepareOutputFileName(type, name));
  }

  @SuppressWarnings("unused")
  protected File createOutputFile(File parent, String type, String name) {
    return new File(parent, prepareOutputFileName(type, name));
  }

  @SuppressWarnings("unused")
  protected File createCollectionsDir(String applicationName) {
    return createDir(outputDir, "application." + applicationName
            + ".collections");
  }

  protected File createDir(String dirName) {
    return createDir(null, dirName);
  }

  protected File createDir(File parent, String dirName) {
    File file = null;

    if (parent == null) {
      file = new File(dirName);
    } else {
      file = new File(parent, dirName);
    }

    boolean wasCreated = false;

    if (file.exists() && file.isDirectory()) {
      wasCreated = true;
    } else {
      wasCreated = file.mkdir();
    }

    if (!wasCreated) {
      throw new RuntimeException("Unable to create directory:" + dirName);
    }

    return file;
  }

  /**
   *
   * @param type
   *            just a label such us: organization, application.
   * @param name
   * @return the file name concatenated with the type and the name of the
   *         collection
   */
  protected String prepareOutputFileName(String type, String name) {
    // Add application and timestamp
    StringBuilder str = new StringBuilder();
    // str.append(baseOutputFileName);
    // str.append(".");
    str.append(type);
    str.append(".");
    str.append(name);
    str.append(".");
    str.append(System.currentTimeMillis());
    str.append(".json");

    String outputFileName = str.toString();

    logger.info("Creating output filename:" + outputFileName);

    return outputFileName;
  }

  protected JsonGenerator getJsonGenerator(File outFile) throws IOException {
 		PrintWriter out = new PrintWriter(outFile, "UTF-8");
 		JsonGenerator jg = jsonFactory.createJsonGenerator(out);
 		jg.setPrettyPrinter(new DefaultPrettyPrinter());
 		jg.setCodec(new ObjectMapper());
 		return jg;

 	}

}
