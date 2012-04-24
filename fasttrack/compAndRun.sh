#cd test
#javac -classpath ../classes Test.java
#cd ..
ant
clear
unset RR_META_DATA
rrrun -tool=FT test.Test > rrout
mkdir classes/test
#mkdir classes/updaters
cp test/tmp/Test.class classes/test/
cp test/tmp/ShadowThread.class classes/rr/state/
cp test/tmp/FastTrackTool.class classes/tools/fasttrack
#cp test/__\$rr_test_Test__\$rr__Update_y.class classes/updaters 
#cp test/__\$rr_test_Test__\$rr__Update_y.class classes
export RR_META_DATA=`pwd`/dump
#java -classpath classes/:jars/java-cup-11a.jar test.Test 2>&1 | tee output 
java -classpath classes/:jars/java-cup-11a.jar test.Test > output
