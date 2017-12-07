#!/bin/bash

# 1 - talaia config file name without .cfg extension (no path) and without 'MSM-' prefix.
#     e.g. if the config file name is  MSM-talaia_example.cfg the parameter should be 'talaia_example'


. /etc/profile

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

data=`date +"%Y%m%d%H%M"`;

pid=`pgrep -f " twitter .*MSM-$1.cfg"`
echo "MSM-twtCrawler-restart.sh : $1 twitter crawlerra ITZALTZEN ... -- $data --"
kill -9 $pid
java -jar $DIR/MSM-1.2.jar twitter -c $DIR/MSM-$1.cfg -s db -p all 1>> $DIR/log/MSM-twitter.log 2>> $DIR/log/MSM-twitter.log &
echo "MSM-twtCrawler-restart.sh : $1 twitter crawlerra BERRABIARAZITA ... -- $data --"



