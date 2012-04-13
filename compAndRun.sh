cd test
javac -classpath ../classes Test.java
cd ..
ant
clear
rrrun -tool=FT test.Test > rrout
mkdir classes/test
cp test/tmp/Test.class classes/test/
cp test/tmp/ShadowThread.class classes/rr/state/
cp test/tmp/FastTrackTool.class classes/tools/fasttrack
echo "===================================="
echo "===================================="
echo "===================================="
echo ""
echo ""
java -classpath classes/:jars/java-cup-11a.jar test.Test 2>&1 | tee output 
