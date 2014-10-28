---
layout: page
---

<div class="iconheader">
  <img src="{{ site:baseurl }}/images/ic_launcher.png" alt="logo" />
  <h1>StorageTrac</h1>
</div>

An external storage tracking app for Android


### Plot usage of external storage

Tap once to select a point.

<div class="screenshot">
<table><tr><td>
<img src="{{ site:baseurl }}/images/screen1.png">
</td></tr></table>
</div>


### View activity at selected point

Tap selected point again for a popup showing recorded activity. Swipe between created, deleted and modified categories.

<div class="screenshot">
<table>
<tr>
<td>
<img src="{{ site:baseurl }}/images/screen2-1.png"></img>
</td>
<td>
<img src="{{ site:baseurl }}/images/screen2-2.png"></img>
</td>
<td>
<img src="{{ site:baseurl }}/images/screen2-3.png"></img>
</td>
</tr>
</table>
</div>

### Search activity history

Use the search dialog to search all recorded activity with a file name, or a part of it.

<div class="screenshot">
<table>
<tr>
<td>
<img src="{{ site:baseurl }}/images/screen3-1.png"></img>
</td>
<td>
<img src="{{ site:baseurl }}/images/screen3-2.png"></img>
</td>
</tr>
</table>
</div>

### Download

* [F-Droid](https://f-droid.org/repository/browse/?fdid=com.nma.util.sdcardtrac)
* [Play store](https://play.google.com/store/apps/details?id=com.nma.sdcardtrac) ([incompatible](#install_note) with others)
* [XDA Dev DB](http://forum.xda-developers.com/android/apps-games/storagetrac-record-plot-changes-t2860608)

Only the primary external storage device is supported as of now. There is a plan to extend to all available storage devices as exposed by Android framework.
Allow a day or two after installing the app for it collect some data to display.

<a name="install_note"></a>
The app in Play store is signed using a different key than that in F-Droid and XDA, and also differs in the package name. 
Hence, data *cannot be migrated* from a F-Droid installed app to an installation through Play store.

### <a name="donate"></a> Donate

You can donate by purchasing the app on [Google Play](https://play.google.com/store/apps/details?id=com.nma.sdcardtrac), sending Bitcoins or [Flattr](https://flattr.com/thing/3457864).
My Bitcoin address is <b>16bxTv1fP8X2QN5SWXc1AcKhhA1tJQKcTa</b>.


Credits
-------
* [GraphView](https://github.com/jjoe64/GraphView) for plotting.
* [Icons](http://findicons.com) used in the app.

App licensed under [GPLv3](http://www.gnu.org/copyleft/gpl.html).
