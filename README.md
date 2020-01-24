MSM - Multi Source Monitor 
=======

Contents
========

The contents of the module are the following:

    + pom.xml                 maven pom file which deals with everything related to compilation and execution of the module
    + src/                    java source code of the module
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/                 it contains binary executable and other directories


INSTALLATION
============

Installing MSM requires the following steps:

If you already have installed in your machine JDK8 and MAVEN 3, please go to step 3
directly. Otherwise, follow these steps:

1. Install JDK 1.8
-------------------

If you do not install JDK 1.8 in a default location, you will probably need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java18
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java18
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

You should now see that your jdk is 1.8

2. Install MAVEN 3
------------------

Download MAVEN 3 from

````shell
wget http://apache.rediris.es/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
````

Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/home/myuser/local/apache-maven-3.0.5
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.5
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK 7 that is using.

2. Get module source code
--------------------------

````shell
git clone https://github.com/Elhuyar/MSM
````

3. Installing using maven
---------------------------

````shell
cd MSM
mvn clean package
````

This step will create a directory called target/ which contains various directories and files.
Most importantly, there you will find the module executable:

elh-MSM-1.3.8.jar

This executable contains every dependency the module needs, so it is completely portable as long
as you have a JVM 1.8 installed.

To install the module in the local maven repository, usually located in ~/.m2/, execute:

````shell
mvn clean install
````

USING MSM
=========================


MSM provides 6 main funcionalities:
   - **twitter**: Twitter Public stream crawling.
   - **feed**: Syndication feed crawling (RSS, Atom, ...). Feed types supported by ROME tools (http://rometools.github.io/rome/)  
   - **influence**: looks for the influence of a given list of sources. Klout index for twitter users and PageRank for websites. As of May 2018 Klout index is no longer available. 
   
   - **twtUser**: asks Twitter for the user profiles of a given list of Twitter users and return their follower and friend information.
   - **langid**: Language detection for sentences. Used mainly to evaluate langid and optimaize.
   - **geocode**: Geocoding wrapper for several geocoding APIs (access keys needed for some of them). Given a string it returns its geolocation coordinates.

Command line examples
==========================

   - You can get general help by calling:
````shell
java -jar MSM-1.3.8.jar -help
```` 

   - **twitter**: Twitter Public stream crawling. Call Example:
````shell
java -jar MSM-1.3.8.jar twitter -c config.cfg -s stout 2>> MSM-twitter.log
````

Parameters:
````shell
java -jar target/MSM-1.3.8.jar twitter -h
usage: MSM-1.3.8.jar twitter [-h] -c CONFIG [-p {terms,users,geo,all}] [-tl] [-s {stout,db,solr}] [-cn CENSUS]

named arguments:
  -h, --help             show this help message and exit
  -c CONFIG, --config CONFIG
                         Configuration file that contains the necessary parameters to connect to the twitter public stream for crawling.
                         
  -p {terms,users,geo,all}, --params {terms,users,geo,all}
                         Search parameters to use when connecting to the Twitter Streaming API. Parameter values areeither stored in the database or specified in the configuration file.
                         	 - "terms" : terms to track.
                         	 - "users" : users to follow.
                         	 - "geo" : bounding boxes defining geographic areas
                         	 - "all" : check for all of the previous parameters
                         
  -tl, --twitterLangid   Whether the crawler shall trust twitter to filter languages or not. Default is no.
                         NOTE 1: languages are defined in the config file.
                         NOTE 2: before activating this option make sure twitter identifies all languages you are  working with, especially in case of less-resourced languages.NOTE 3: even if this option is
                         active MSM will perform its own language identification, and leverage it with Twitter info.
                         
  -s {stout,db,solr}, --store {stout,db,solr}
                         Whether tweets shall be stored in a database, an Apache solr Index or printed to stdout (default).
                         	 - "stout" : standard output
                         	 - "db" : database
                         	 - "solr" : solr (not implemented yet)
                         
  -cn CENSUS, --census CENSUS
                         Census is used to store tweets by users in a certain geographic area. A path to a file  must  be given as an argument. The file contains a list of Twitter users. The users and their
                         tweets will be marked in the database as 'local'. The file must contain one user per line in the following format:
                         	 userId<tab>screenName[<tab>Additional Fields]
                         NOTE: if the value 'db' is give instead of a file path MSM will try to generate the census from the database.
                         
Run java -jar target/MSM-1.3.8.jar (twitter|feed|influence|twtUser|langid|geocode) -help for detail
````

   - **feed**: Syndication feed crawling (RSS, Atom, ...). Feed types supported by ROME tools (http://rometools.github.io/rome/)  
````shell
java -jar MSM-1.3.8.jar feed -c config.cfg -u http://feeds2.feedburner.com/example
````

Parameters:
````shell
java -jar target/MSM-1.3.8.jar feed -h
usage: MSM-1.3.8.jar feed [-h] [-c CONFIG] [-t {press,multimedia}] [-ff FFMPEG] [-u URL] [-s {stout,db,solr}] [-cn CENSUS]

named arguments:
  -h, --help             show this help message and exit
  -c CONFIG, --config CONFIG
                         Configuration file that contains the necessary parameters to connect to the feed stream for crawling.Only needed if no url is given through the --url parameter.
                         
  -t {press,multimedia}, --type {press,multimedia}
                         The type of feed we are dealing with.
                         	 - "press" : written digital press feeds. Standard rss feed parsers are used
                         	 - "multimedia" : feed coming from multimedia source (tv/radio) a custom parser is activated in this case.
                         
  -ff FFMPEG, --ffmpeg FFMPEG
                         The path to the ffmpeg executable (the directory containing ffmpeg and ffprobe binaries).  Only  needed if for multimedia source feeds. Default value is "/usr/bin". Change the value
                         to match your installation path
                         
  -u URL, --url URL      URL(s) of the feed(s) we want to crawl. Feeds must be separated with ',' chars.
                         e.g. : java -jar MSM-1.0.jar feed -u 'url1,url2,...'
  -s {stout,db,solr}, --store {stout,db,solr}
                         Whether mentions shall be stored in a database, an Apache solr Index or printed to stdout (default).
                         	 - "stout" : standard output
                         	 - "db" : database
                         	 - "solr" : solr (not implemented yet)
                         
  -cn CENSUS, --census CENSUS
                         Census is used to store mentions from sources in a certain geographic area. A path to a file  must  be given as an argument. The file contains a list of source ids (as stored in the
                         database). The sources and their mentions will be marked in the database as 'local'. The file must contain one user per line in the following format:
                         	 userId<tab>sourceName[<tab>Additional Fields]
                         
Run java -jar target/MSM-1.3.8.jar (twitter|feed|influence|twtUser|langid|geocode) -help for details          
````
   - **influence**: looks for the influence of a given list of sources. Klout index for twitter users and PageRank for websites.
````shell
java -jar MSM-1.3.8.jar influence -c config.cfg -db 2>> MSM-Influence.log
````

Parameters:
````shell
java -jar target/MSM-1.3.8.jar influence -h
usage: MSM-1.3.8.jar influence [-h] [-s SOURCES] -c CONFIG [-db] [-t {twitter,feed,all}] [-w {null,error,all}] [-l LIMIT]

named arguments:
  -h, --help             show this help message and exit
  -s SOURCES, --sources SOURCES
                         web domain (pageRank) or twitter screen name (KloutIndex) to look for its influence  for.Many  sources  may  be introduced separated by ',' char.If you want to retrieve sources from
                         the database left this option empty or use the 'db' value
                         
  -c CONFIG, --config CONFIG
                         Configuration file that contains the necessary parameters to connect to Influence APIs and Database you want to interact with the database
                         
  -db, --database        Whether influences shall be stored in a database or printed to stdout (default). Database parameters must be given in the configuration file.
                         
  -t {twitter,feed,all}, --type {twitter,feed,all}
                         type of the sources to look for its influence for:	 - "twitter" : sources are twitter user screen names
                         	 - "domain" : sources are web domains
                         	 - "all" : sources are mixed (default) system will detect the source type for each source
                         
  -w {null,error,all}, --which {null,error,all}
                         which sources to look for its influence for (only for database interaction):
                         	 - "null" : sources that have not been yet processed at all
                         	 - "error" : sources that have been processed but no influence could be retrieved
                         	 - "all" : all sources.
                         		WARNING: this will override values in the database.
                         		WARNING2:Depending on the number of sources in the database this could take a very long time.
                         
  -l LIMIT, --limit LIMIT
                         limit the number of sources processed in the execution (only for database interaction): default is 500
                         --limit = 0 means no limit is established, and thus the command will atempt to process all sources found in the db (not processed yet).
                         This parameter is important not to exceed the twitter api rate limits. Increase it at your own risk.
                         
Run java -jar target/MSM-1.3.8.jar (twitter|feed|influence|twtUser|langid|geocode) -help for details
````
   - **twtUser**: asks Twitter for the user profiles of a given list of Twitter users and return their follower and friend information.
````shell
java -jar MSM-1.3.8.jar twtUser -h
 usage: MSM-1.3.8.jar twtUser [-h] -c CONFIG [-s {stout,db,solr}] [-l LIMIT] [-o]

named arguments:
  -h, --help             show this help message and exit
  -c CONFIG, --config CONFIG
                         Configuration file that contains the necessary parameters to connect to the twitter REST API 1.1.
                         
  -s {stout,db,solr}, --store {stout,db,solr}
                         Whether tweets shall be stored in a database, an Apache solr Index or printed to stdout (default).
                         	 - "stout" : standard output
                         	 - "db" : database
                         	 - "solr" : solr (not implemented yet)
                         
  -l LIMIT, --limit LIMIT
                         limit the number of users processed in the execution (only for database interaction): default is 500
                         --limit = 0 means no limit is established, and thus the command will atempt to process all sources found in the db (not processed yet).
                         This parameter is important depending on the number of APIs you have available and your usage rate limits.
                         
  -o, --onlyffCount      If this flag is active only follower and friend info will be returned, but  no  follower  and  friends lists. Returning only follower and friends count is much faster because of the
                         higher rate limit of the API.  
````

   - **langid**: Language detection for sentences. Used mainly to evaluate langid and optimaize.
````shell
java -jar MSM-1.3.8.jar langid -h
usage: MSM-1.3.8.jar langid [-h] [-a {langid,optimaize}] -s STRINGS [-l LANGS] [-tl] [-o] [-t {twitter,longtext}] [-tr] [-c CONFIDENCETHRESHOLD] [-lc]

named arguments:
  -h, --help             show this help message and exit
  -a {langid,optimaize}, --algorithm {langid,optimaize}
                         algorithm to use for language identification, optimaize or langid. default is langid.
                         
  -s STRINGS, --strings STRINGS
                         string to look for its language, or file containing strings. The language detection  unit  is  the whole fileMany locations may be introduced separated by '::' string (semicolon may
                         be used inside the location string, that is why they are not used as separators).
                         
  -l LANGS, --langs LANGS
                         list of accepted langs. Use iso-639 codes separated by commas (e.g. --langs=es,eu,en,fr) Default is 'eu,es,en,fr'.
                         NOTE 1: before activating this option make sure twitter identifies all languages you are working with, especially in case of less-resourced languages.
                         NOTE 2: even if this option is active MSM will perform its own language identification, and leverage it with Twitter info.
                         
  -tl, --twitterLangid   Whether the crawler shall trust twitter to filter languages or not. Default is no.
                         NOTE 1: before activating this option make sure twitter identifies all languages you are working with, especially in case of less-resourced languages.
                         NOTE 2: even if this option is active MSM will perform its own language identification, and leverage it with Twitter info.
                         
  -o, --onlySpecificLanguageProfiles
                         Do not load all language profiles, only those specified in --langs argument.
                         
  -t {twitter,longtext}, --type {twitter,longtext}
                         which type of texts are we dealing with:
                         	 - "twitter" : microbloging messages or short messages
                         	 - "longtext" : paragraphs or longer sentences
                         		WARNING: Nothing.
                         
  -tr, --train           train niew model with the given files in the --strings parameter
                         		WARNING: langs argument value is used as the language name to store the  new  language profile.		WARNING: type is sued to generate short or standard text profile	WARNING: profile
                         is stored in the same place of the input file, with the lang name and "ld_profile" string. e.g. input_es_ldprofile
                         
  -c CONFIDENCETHRESHOLD, --confidenceThreshold CONFIDENCETHRESHOLD
                         Confidence threshold for language identification:
                         	If no candidate achieves the required threshold 'unk' is returned.
                         	If more than one candidate achieves the required threshold the one with the highest probability is returned.
                         
  -lc, --lowerCase       Convert everything to lower case before doing language identification.
````

   - **geocode**: Geocoding wrapper for several geocoding APIs (access keys needed for some of them). Given a string it returns its geolocation coordinates.
````shell
java -jar MSM-1.3.8.jar geocode -h
usage: MSM-1.3.8.jar geocode [-h] [-s SOURCES] -c CONFIG [-db] [-t {twitter,feed,all}] [-w {unknown,error,all}] [-a {mapquest,mapquest-open,openstreetmaps,googlemaps,LocationIQ,OpenCage,all}] [-l LIMIT]

named arguments:
  -h, --help             show this help message and exit
  -s SOURCES, --sources SOURCES
                         location to look for its geo-coordinate info.Many locations may be introduced separated by '::'  string  (semicolon  may be used inside the location string, that is why they are not
                         used as separators).If you want to retrieve sources from the database left this option empty or use the 'db' value
                         
  -c CONFIG, --config CONFIG
                         Configuration file that contains the necessary parameters to connect to the corresponding geocoding service APIsand Database you want to interact with the database
                         
  -db, --database        Whether influences shall be stored in a database or printed to stdout (default). Database parameters must be given in the configuration file.
                         
  -t {twitter,feed,all}, --type {twitter,feed,all}
                         type of the sources to look for its geolocation for:	 - "twitter" : sources are twitter user screen names
                         	 - "domain" : sources are web domains
                         	 - "all" : sources are mixed (default) system will detect the source type for each source
                         
  -w {unknown,error,all}, --which {unknown,error,all}
                         which sources to look for its influence for (only for database interaction):
                         	 - "unknown" : sources that have not been yet processed at all
                         	 - "error" : sources that have been processed but no geolocation could be retrieved
                         	 - "all" : all sources.
                         		WARNING: this will override values in the database.
                         		WARNING1:Depending on the number of sources in the database this could take a very long time. Also be careful about your API key rate limits!
                         
  -a {mapquest,mapquest-open,openstreetmaps,googlemaps,LocationIQ,OpenCage,all}, --api {mapquest,mapquest-open,openstreetmaps,googlemaps,LocationIQ,OpenCage,all}
                         Geocoding is by default multi API, using all the APIs for which the user obtains keys (they must be specified in the config file).
                         Through this parameter the user may specify a single API to use for geocoding.
                         BEWARE to set --limit option according to your usage rate limits.
                         
  -l LIMIT, --limit LIMIT
                         limit the number of sources processed in the execution (only for database interaction): default is 1000
                         --limit = 0 means no limit is established, and thus the command will atempt to process all sources found in the db (not processed yet).
                         This parameter is important depending on the number of APIs you have available and your usage rate limits.

````


GENERATING JAVADOC
==================

You can also generate the javadoc of the module by executing:

````shell
mvn javadoc:jar
````

Which will create a jar file core/target/elh-MSM-1.3.8-javadoc.jar


Contact information
===================

````shell
Iñaki San Vicente and Xabier Saralegi
Elhuyar Foundation
{i.sanvicente,x.saralegi}@elhuyar.eus
````
