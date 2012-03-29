cd test
javac -classpath ./rr.jar Test.java
cd ..
ant
clear
rrrun -tool=FT test.Test > myout
mkdir classes/test
cp test/tmp/Test.class classes/test/
echo "===================================="
echo "===================================="
echo "===================================="
echo ""
echo ""
java -classpath classes/ test.Test 
