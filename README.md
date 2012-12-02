CVCenter is a container of CVs as well as it provides a graphical control-interface that lets you connect synth-controls or controls within patterns to hardware midi-sliders or OSC-controllers. A CV models a value constrained by a ControlSpec. Within CVCenter they get represented within CVWidgets that can hold one or more CVs. Each of them can be connected to an unlimited number GUI-elements or/and control parameters in running Synths or set values in Patterns. As CV inherits from Stream any instance can be used as any other Pattern (Pseq, Prand, Pwhite etcetc.).

For more info check out the CVCenter- resp. CVWidget-helpfile.

Installation
------------
1. get SuperCollider from http://sourceforge.net/projects/supercollider/files/
2. install the required extensions via the Quarks-mechanism:
	- Conductor
	- TabbedView
	- wslib (optional)  
	see the Quarks-helpfile for more information on how to do this.
2. after installing SuperCollider and the required extensions put all content of CVCenter in your user-app-support directory. Execute the following line SuperCollider to see where that is:

		Platform.userAppSupportDir

Under OSX this will resolve to:

	~/Library/Application Support/SuperCollider

Under Linux, using the latest Developer-Version of SuperCollider this will resolve to

	~/.local/share/SuperCollider

If not present yet create a directory 'Extensions' and put CVCenter in there.

Note: if you're on Linux you will need to have installed SuperCollider >= 3.5 as CVCenter depends on QtGUI. Under MacOSX CVCenter *should* be compatible with SC >= 3.4 resp. QtGUI as well as Cocoa.

