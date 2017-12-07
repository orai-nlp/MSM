#!/bin/bash

# 1 - talaia config file name without .cfg extension (no path) and without 'MSM-' prefix.
#     e.g. if the config file name is  MSM-talaia_example.cfg the parameter should be 'talaia_example'


. /etc/profile

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

data=`date +"%Y%m%d%H%M"`;

# crawlerra martxan dagoen begiratu eta ez badago jarri martxan
pid=`pgrep -f "twitter .*MSM-$1.cfg"`
if [ -z "$pid" ]
then 
    echo "MSM-twitterCrawler.sh : $1 instantziaren crawlerra erorita, berrabiarazi -- $data --"
    java -jar $DIR/MSM-1.2.jar twitter -c $DIR/MSM-$1.cfg -s db -p all 1>> $DIR/log/MSM-twitter.log 2>> $DIR/log/MSM-twitter.log &
else
    noconn=`tail -3 $DIR/log/MSM-twitter.log | grep "Shutting down httpclient connection manager" | wc -l`
    if [ $noconn -gt 0 ]
	then
	    echo "MSM-twitterCrawler.sh : $1 twitter crawlerra konexio gabe, berrabiarazi -- $data --"
	    kill -9 $pid
	    java -jar $DIR/MSM-1.2.jar twitter -c $DIR/MSM-$1.cfg -s db -p all 1>> $DIR/log/MSM-twitter.log 2>> $DIR/log/MSM-twitter.log &
    else
	noconn=`tail -10 $DIR/log/MSM-twitter.log | grep "status code - 420" | wc -l`
	if  [ $noconn -gt 0 ]
	then
	    echo "MSM-twitterCrawler.sh : $1 twitter crawlerra blokeatuta(420), berrabiarazi -- $data --"
	    kill -9 $pid
	    java -jar $DIR/MSM-1.2.jar twitter -c $DIR/MSM-$1.cfg -s db -p all 1>> $DIR/log/MSM-twitter.log 2>> $DIR/log/MSM-twitter.log &	
	#else
	#    echo "MSM-twitterCrawler.sh : $1 twitter crawlerra ongi dagoela dirudi. -- $data --"
	fi
    fi
fi



