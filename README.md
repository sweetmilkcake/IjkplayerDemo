# IjkplayerDemo
Just a Ijkplayer Demo App.

## Screenshots
![demo1](https://github.com/sweetmilkcake/IjkplayerDemo/blob/master/Screenshots/demo1.gif)

## Download
https://github.com/sweetmilkcake/IjkplayerDemo/releases

## Features
* Friendly Play Interface
* SeekBar for Forward and Backward
* Gestures for Forward and Backward
* Gestures for Volume Control
* Gestures for Brightness Control

## Libraries
* Ijkplayer - k0.8.8 - https://github.com/Bilibili/ijkplayer

## How to Port ijkplayer
* Android Studio 3.1.2 (My Port Environment)
* Gradle
```
dependencies {
    implementation 'tv.danmaku.ijk.media:ijkplayer-java:0.8.8'
    implementation 'tv.danmaku.ijk.media:ijkplayer-armv7a:0.8.8'
    implementation 'tv.danmaku.ijk.media:ijkplayer-armv5:0.8.8'
    implementation 'tv.danmaku.ijk.media:ijkplayer-arm64:0.8.8'
    implementation 'tv.danmaku.ijk.media:ijkplayer-x86:0.8.8'
    implementation 'tv.danmaku.ijk.media:ijkplayer-x86_64:0.8.8'
    implementation 'tv.danmaku.ijk.media:ijkplayer-exo:0.8.8'
}
```
* Copy to your Project
```
ijkplayer-k0.8.8\android\ijkplayer\ijkplayer-example\src\main\java\tv\danmaku\ijk\media\example\widget\*
ijkplayer-k0.8.8\android\ijkplayer\ijkplayer-example\src\main\java\tv\danmaku\ijk\media\example\services\MediaPlayerService.java
ijkplayer-k0.8.8\android\ijkplayer\ijkplayer-example\src\main\java\tv\danmaku\ijk\media\example\application\Settings.java
```
* Copy and Fix the resource error
```
layout, drawable, values, and so on.
```
* Use IjkVideoView to Play Video
```xml
<com.sweetmilkcake.ijkplayerdemo.widget.media.IjkVideoView
    android:id="@+id/video_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
```
```java
// Init Ijkplayer
mVideoView = (IjkVideoView) findViewById(R.id.video_view);
// IjkMediaPlayer.loadLibrariesOnce(null); // not need due to use gradle
// IjkMediaPlayer.native_profileBegin("libijkplayer.so"); // not need due to use gradle
mVideoView.setVideoURI(Uri.parse(mUrl));
mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {
        mVideoView.start();
    }
});
```
* About IjkplayerDemo App

  IjkplayerDemo is a simple demo for showing how to use ijkplayer. Just implement the PlayActivity.

## Thanks
* https://github.com/googlesamples/android-UniversalMusicPlayer
* ......

## License
    Copyright 2018 SweetMilkCake

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
