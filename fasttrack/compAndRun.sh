cd test
javac -classpath ../classes Test.java
cd ..
ant
clear
rrrun -tool=FT test.Test > rrout
mkdir classes/test
cp test/tmp/Test.class classes/test/
echo "===================================="
echo "===================================="
echo "===================================="
echo ""
echo ""
java -classpath classes/ test.Test 2>&1 | tee runout