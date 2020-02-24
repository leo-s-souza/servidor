pid=`ps aux | grep capp.jar | awk '{print $2}'`
kill -9 $pid