/*
 * Copyright 2014 Iñaki San Vicente

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */


package elh.eus.MSM;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

//import org.jdom2.JDOMException;




/**
 * Main class of elh-crawler, the elhuyar crawling module
 * 
 *
 * @author isanvi
 * @version 2015-04-24
 *
 */
public class CLI {
	/**
	 * Get dynamically the version of elh-eus-absa-atp by looking at the MANIFEST
	 * file.
	 */
	private final String version = CLI.class.getPackage()
			.getImplementationVersion();
	/**
	 * Name space of the arguments provided at the CLI.
	 */
	private Namespace parsedArguments = null;
	/**
	 * Argument parser instance.
	 */
	private ArgumentParser argParser = ArgumentParsers.newArgumentParser(
			"elh-crawler-" + version + ".jar").description(
					"elh-crawler-" + version
					+ " is a crawling module developed by Elhuyar Foundation.\n");
	/**
	 * Sub parser instance.
	 */
	private Subparsers subParsers = argParser.addSubparsers().help(
			"sub-command help");
	/**
	 * The parser that manages the twitter crawler sub-command.
	 */
	private Subparser tweetCrawlParser;
	/**
	 * The parser that manages the influence tagger sub-command.
	 */
	private Subparser influenceTaggerParser;

	/**
	 * Construct a CLI object with the three sub-parsers to manage the command
	 * line parameters.
	 */
	public CLI() {
		tweetCrawlParser = subParsers.addParser("twitter").help("Twitter Stream crawling CLI");
		loadTwitterCrawlerParameters();
		influenceTaggerParser = subParsers.addParser("influence").help("Influence tagger CLI");
		loadInfluenceTaggerParameters();
	}
	
	
	/**
	 * Main entry point of elh-crawler.
	 *
	 * @param args
	 * the arguments passed through the CLI
	 * @throws IOException
	 * exception if input data not available
	 */
	public static void main(final String[] args) throws IOException {
		CLI cmdLine = new CLI();
		cmdLine.parseCLI(args);
	}
	
	
	/**
	 * Parse the command interface parameters with the argParser.
	 *
	 * @param args
	 * the arguments passed through the CLI
	 * @throws IOException
	 * exception if problems with the incoming data
	 */
	public final void parseCLI(final String[] args) throws IOException {
		try {
			parsedArguments = argParser.parseArgs(args);
			System.err.println("CLI options: " + parsedArguments);
			if (args[0].equals("twitter")) {
				twitterCrawler();
			} else if (args[0].equals("influence")) {
				tagInfluence();
			}
		} catch (ArgumentParserException e) {
			argParser.handleError(e);
			System.out.println("Run java -jar target/elh-crawler-" + version
					+ ".jar (twitter) -help for details");
			System.exit(1);
		}
	}
	
	public final void twitterCrawler()
	{
		String cfg = parsedArguments.getString("config");			
		
		try {
			TwitterStreamClient twitterClient = new TwitterStreamClient(cfg);
		} catch (Exception e) {			
			e.printStackTrace();
		} 		
	}
	
	public final void tagInfluence()
	{
		String cfg = parsedArguments.getString("config");	
		String sources = parsedArguments.getString("sources", );	
		boolean db = parsedArguments.getBoolean("database");
		
		try {
			InfluenceTagger infTagger = new InfluenceTagger(cfg);
		} catch (Exception e) {			
			e.printStackTrace();
		} 
		
	}

	
	public final void loadTwitterCrawlerParameters()
	{
		tweetCrawlParser.addArgument("-c", "--config")
		.required(true)
		.help("Configuration file that contains the necessary parameters to connect to the twitter public "
				+ "stream for crawling.\n");
		tweetCrawlParser.addArgument("-db", "--database")		
		.action(Arguments.storeTrue())
		.help("Whether tweets shall be stored in a database or printed to stdout (default).\n");
	}
	
	
	public final void loadInfluenceTaggerParameters()
	{
		influenceTaggerParser.addArgument("-s", "--sources")
		.required(false)
		.help("Configuration file that contains the necessary parameters to connect to the twitter public "
				+ "stream for crawling.\n");
		influenceTaggerParser.addArgument("-c", "--config")		
		.action(false)
		.help("Whether tweets shall be stored in a database or printed to stdout (default).\n");
		influenceTaggerParser.addArgument("-db", "--database")		
		.action(Arguments.storeTrue())
		.help("Whether tweets shall be stored in a database or printed to stdout (default).\n");
	}
	

	
}
