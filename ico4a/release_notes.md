In this release everything works as supposed (at least to me): ICO files can be decoded from any InputStream and one receives a List<Bitmap>.

If you need the actual release, go into the ico4a build folder and grab the AAR-library, create a new module in Android Studio, chose "import AAR" and select the file. It will create a module in your project and you should add it as a dependency to your project.

Currently it seems the file cannot be retrieved via jcenter (I have to fiddle with it some more).
