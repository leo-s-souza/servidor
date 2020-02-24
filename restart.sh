pid=`ps aux | grep capp.jar | awk '{print $2}'`
kill -9 $pid

ps aux | grep 'java'

sleep 5

cd /opt/capp2/

echo "" > nohup.out

nohup /bin/bash start.sh &

tail -f nohup.out
