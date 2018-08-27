# Multimedia-Image-Processor

The algorithms here implements the how the changing the quantization affects the image rendering and also ellicits of the vaious formats are important to for image rendering (YUV and RGB)

Further more the second project simulates the components of multimedia pipeline namely - compression and methods of streaming the data over network and rendering the same

**Image -> JPEG Compression -> Stream -> JPEG Decompression -> Render**

**JPEG Compression**
* Extract R, G and B of the image
* Discrete Cosine Transform

**Streaming**
* Spectral selection
* Successive bit approximation
* Sequential transfer


**Spectral Selection**

![Alt Text](https://github.com/shivneshr/Multimedia-Processing/blob/master/JPEG_Compression/readme_resource/spectralSelection.gif)


**Successive Bit approximation**

![Alt Text](https://github.com/shivneshr/Multimedia-Processing/blob/master/JPEG_Compression/readme_resource/successiveBitApprox.gif)

**Sequential Transfer**

![Alt Text](https://github.com/shivneshr/Multimedia-Processing/blob/master/JPEG_Compression/readme_resource/sequentialAccess.gif)
