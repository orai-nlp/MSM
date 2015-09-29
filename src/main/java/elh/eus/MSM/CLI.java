/*
 * Copyright 2015 Elhuyar Fundazioa

This file is part of MSM.

    EliXa is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    MSM is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MSM.  If not, see <http://www.gnu.org/licenses/>.
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
			"elh-MSM-" + version + ".jar").description(
					"elh-MSM-" + version
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
	 * The parser that manages the feed reader sub-command.
	 */
	private Subparser feedReaderParser;
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
		feedReaderParser = subParsers.addParser("feed").help("Feed reaader CLI");
		loadFeedReaderParameters();
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
			} 
			else if (args[0].equals("feed")) {
				feedReader();
			} 
			else if (args[0].equals("influence")) {
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
		String store = parsedArguments.getString("store");
		
		try {
			TwitterStreamClient twitterClient = new TwitterStreamClient(cfg, store);
		} catch (Exception e) {			
			e.printStackTrace();
		} 		
	}
	
	
	public final void feedReader()
	{
		String cfg = parsedArguments.getString("config");
		String store = parsedArguments.getString("store");
		String url = parsedArguments.getString("url");
		
		if (! url.isEmpty())
		{		
			FeedReader feedReader = new FeedReader(url);
		}
		else
		{
			try {
				FeedReader feedReader = new FeedReader(cfg, store);
			} catch (Exception e) {			
				e.printStackTrace();
			}
		}
	}
	
	public final void tagInfluence()
	{
		String cfg = parsedArguments.getString("config");	
		String sources = parsedArguments.getString("sources");	
		boolean db = parsedArguments.getBoolean("database");
		
		try {
			InfluenceTagger infTagger = new InfluenceTagger(cfg, db);
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
		tweetCrawlParser.addArgument("-s", "--store")		
		.choices("stout", "db", "solr")
		.setDefault("stout")
		.help("Whether tweets shall be stored in a database, an Apache solr Index or printed to stdout (default).\n"
				+ "\t - \"stout\" : standard output"
				+ "\t - \"db\" : standard output"
				+ "\t - \"solr\" : standard output\n");
	}
	
	public final void loadFeedReaderParameters()
	{
		feedReaderParser.addArgument("-c", "--config")
		.required(false)
		.help("Configuration file that contains the necessary parameters to connect to the twitter public "
				+ "stream for crawling.\n");

		feedReaderParser.addArgument("-u", "--url")
		.required(false)
		.help("URL of the feed we want to crawl.\n");
		feedReaderParser.addArgument("-s", "--store")		
		.choices("stout", "db", "solr")
		.setDefault("stout")
		.help("Whether tweets shall be stored in a database, an Apache solr Index or printed to stdout (default).\n"
				+ "\t - \"stout\" : standard output"
				+ "\t - \"db\" : standard output"
				+ "\t - \"solr\" : standard output\n");
	}
	
	public final void loadInfluenceTaggerParameters()
	{
		influenceTaggerParser.addArgument("-s", "--sources")
		.required(false)
		.help("Configuration file that contains the necessary parameters to connect to the twitter public "
				+ "stream for crawling.\n");
		influenceTaggerParser.addArgument("-c", "--config")		
		.required(true)
		.help("Whether tweets shall be stored in a database or printed to stdout (default).\n");
		influenceTaggerParser.addArgument("-db", "--database")		
		.action(Arguments.storeTrue())
		.help("Whether tweets shall be stored in a database or printed to stdout (default).\n");
	}
	

	
}
