rm classes.dex
dx --dex --output=classes.dex MyInjectEventApp.jar
aapt add MyInjectEventApp.jar classes.dex
adb push MyInjectEventApp.jar /data/
rm classes.dex
mv MyInjectEventApp.jar ../AndroidScreencast/
cp ../AndroidScreencast/MyInjectEventApp.jar ../AndroidScreencast/src