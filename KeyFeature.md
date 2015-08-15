# New Download #
windows\_x86\_64 version:

  * 20140929 add drag/drop support
https://drive.google.com/file/d/0B0zLAKG6DFqCanNFZTlnWVN2WkE/edit?usp=sharing
  * 20140925, add search, previous/next hotkey. x86\_64 version
https://drive.google.com/file/d/0B0zLAKG6DFqCMC1Jb01MM08xRGc/edit?usp=sharing


# Introduction #

This is a desktop tool for reading Android logcat format text files. General users can use it the same as DDMS.(DDMS is an online tool, but this is offline tool).

Advanced users can use more features.
  * Support multi logcat format and DDMS save/copy log format.
  * Add Advanced filter(show two or more Tags).
  * Time sync between log(main/events/radio) buffer.

# Main UI Overview #

![http://androidlogcatviewer.googlecode.com/svn/wiki/main.png](http://androidlogcatviewer.googlecode.com/svn/wiki/main.png)


# Details #

File Menu:
  * Open File --> Open a log file.
  * Open bugreport(dumpstate) file.
  * Open log folder --> a folder contains main/events/radio log files, tool will open it together.

Main UI:
  * Three log view, use log time stamp(04-08 13:09:20.490) to synchronize. click one, other will jump to nearest line.
    1. Toolbar(filter/save select log).
  * Context menu(right click in log table), some of fast-filter options.

Add filter:
![http://androidlogcatviewer.googlecode.com/svn/wiki/multi-tag-filter.png](http://androidlogcatviewer.googlecode.com/svn/wiki/multi-tag-filter.png)
> Two types to choose one:
  * Advanced filter
    1. Select multi Tag to show. for each filter, tool maintain a white list of Tag for each filter.
  * Original filter
    1. The same as DDMS: show typed PID and Tag, blank means all.