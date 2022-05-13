#!/bin/bash
# echo "format: command [query]"
query=$1
pid=(`adb shell ps | grep $query  | tr -s [:space:] ' ' | cut -d' ' -f2`)
echo "pid: $pid"
adb logcat | grep -F $pid