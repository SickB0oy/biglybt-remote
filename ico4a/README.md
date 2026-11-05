# ico4a
This is a library to decode ICO files into a list of Bitmap-objects - based on [image4j project](https://github.com/imcdonagh/image4j "image4j") (I took the liberty to use a similar structure, classes and methods, so credits go to the authors of image4j as well!).
There is a demo android application included, which shows off the library's functionality. This app is not bullet-proofed so take everything with caution.

## Usage
### Add dependency to your project and module
The easiest way to include ico4a in your project, is to include the jcenter repository in your **project's _build.gradle_** and the following line in your **module's _build.gradle_**'s dependencies-section:
```Gradle
compile 'divstar:ico4a:v1.0'
```
Alternatively you may download the AAR from [ico4a's gitHub releases site](https://github.com/divStar/ico4a/releases), create a new module (chose "import AAR" in the following dialog) and add the dependency to your app's project manually.

### Use the ICODecoder.read(InputStream) method
After having done so, use the following line anywhere to decode an ICO-InputStream into a List of Bitmap-objects:
```Java
List<Bitmap> images = ICODecoder.read(SOME_INPUTSTREAM);
```

I suggest creating an AsyncTask to download and read the file, because while reading the file is a rather quick task, it still might result in UI thread blocking.

See the sample app included in the gitHub repository to get an idea of how to use it.

![Screenshot showing the sample application and the default icons](http://abload.de/img/screenshot_20160311-0o3oyq.png)

## Limitations
ico4a cannot write ICO files, because I did not need it myself. Some methods for writing an ICO file are already present, others are not. In particular I have avoided coding BMPEncoder and ICOEncoder, which you will find in the [image4j project](https://github.com/imcdonagh/image4j "image4j library"). Thus you will have to implement them yourself if you need them.
You can however save each of the resulting Bitmap-objects easily using Android's built-in mechanisms. Here is an example:
```Java
    File file = new File(dir, "output.png");
    FileOutputStream fOut = new FileOutputStream(file);

    bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
    fOut.flush();
    fOut.close();
```

## Stability
ico4a has been tested with various single- and multi-image ICO files. It has been tested to support 1-, 4-, 8-, 24- and 32-bit uncompressed images with and without alpha transparency. In addition 24- and 32-bit PNG-compressed images with and without alpha transparency have been tested. All of them loaded without any problems.

## Final word
Please report any problems if you encounter them and I will see if I can help solve them.
