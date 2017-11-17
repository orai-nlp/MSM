#!/bin/bash

# 1 - talaia config file name without .cfg extension (no path)
# 2 - pass

#. /mnt/ebs/talaia_eupol/crawler/variables.sh
. /etc/profile

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

data=`date +"%Y%m%d%H%M"`;

# crawlerra martxan dagoen begiratu eta ez badago jarri martxan
ps=`ps aux | grep "MSM.*\.jar" | grep "twitter" | grep "$1" | wc -l `
if [ $ps -lt 1 ]
then 
    echo "MSM-twitterCrawler.sh : $1 instantziaren crawlerra erorita, berrabiarazi -- $data --"
    java -jar $DIR/MSM-1.2.jar twitter -c $DIR/$1.cfg -s db -p all 1>> $DIR/log/MSM-twitter.log 2>> $DIR/log/MSM-twitter.log &
else
    pid=`pgrep -f "MSM-eupol.cfg"`
    noconn=`tail -3 $DIR/log/MSM-twitter.log | grep "Shutting down httpclient connection manager" | wc -l`
    if [ $noconn -gt 0 ]
	then
	    echo "MSM-twitterCrawler.sh : $1 twitter crawlerra konexio gabe, berrabiarazi -- $data --"
	    kill -9 $pid
	    java -jar $DIR/MSM-1.2.jar twitter -c $DIR/$1.cfg -s db -p all 1>> $DIR/log/MSM-twitter.log 2>> $DIR/log/MSM-twitter.log &
    else
	noconn=`tail -10 $DIR/log/MSM-twitter.log | grep "status code - 420" | wc -l`
	if  [ $noconn -gt 0 ]
	then
	    echo "MSM-twitterCrawler.sh : $1 twitter crawlerra blokeatuta(420), berrabiarazi -- $data --"
	    kill -9 $pid
	    java -jar $DIR/MSM-1.2.jar twitter -c $DIR/$1.cfg -s db -p all 1>> $DIR/log/MSM-twitter.log 2>> $DIR/log/MSM-twitter.log &	
	#else
	#    echo "MSM-twitterCrawler.sh : $1 twitter crawlerra ongi dagoela dirudi. -- $data --"
	fi
    fi
fi



