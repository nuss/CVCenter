/* (c) 2010-2013 Stefan Nussbaumer */
/*
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
*/

CVCenter {

	classvar <all, nextCVKey, <cvWidgets, <window, <childViews, /*<windowStates, */<tabs, prefPane, removeButs;
	classvar <>midiMode, <>midiResolution, <>ctrlButtonBank, <>midiMean, <>softWithin;
	classvar <>shortcuts, <scv;
	classvar <alwaysOnTop=false;
	classvar <>guix, <>guiy, <>guiwidth, <>guiheight;
	classvar <widgetStates;
	classvar <tabProperties, colors, nextColor;
	classvar widgetwidth, widgetheight=160, colwidth, rowheight;
	classvar nDefWin, pDefWin, pDefnWin, tDefWin, allWin, historyWin, eqWin;
	classvar prefs, boundsOnShutDown;
	// CVWidgetMS: how many slots at max for one column
	classvar <>numMsSlotsPerColumn = 15;

	*initClass {
		var newPrefs, newBounds;
		var scPrefs = false;
		var shutDownFunc;
		var scFunc;

		Class.initClassTree(CVCenterPreferences);
		Class.initClassTree(CVWidget);
		Class.initClassTree(KeyDownActions);
		// windowStates = ();
		prefs = CVCenterPreferences.readPreferences;

		prefs !? {
			prefs[\saveGuiProperties] !? {
				shutDownFunc = {
					// "shutdown action triggered".postln;
					newPrefs = CVCenterPreferences.readPreferences;
					CVCenterPreferences.writePreferences(
						newPrefs[\saveGuiProperties],
						boundsOnShutDown ?? { newPrefs[\guiProperties] },
						newPrefs[\saveClassVars],
						newPrefs[\midiMode],
						newPrefs[\midiResolution],
						newPrefs[\midiMean],
						newPrefs[\softWithin],
						newPrefs[\ctrlButtonBank],
						newPrefs[\removeResponders],
						newPrefs[\initMidiOnStartUp],
						newPrefs[\shortcuts],
						newPrefs[\globalShortcuts],
						newPrefs[\keyCodesAndMods]
					)
				};
				if(prefs[\saveGuiProperties] == 1 or:{
					prefs[\saveGuiProperties] == 2
				}, {
					this.guix_(prefs[\guiProperties] !? { prefs[\guiProperties].left });
					this.guiy_(prefs[\guiProperties] !? { prefs[\guiProperties].top });
					this.guiwidth_(prefs[\guiProperties] !? { prefs[\guiProperties].width });
					this.guiheight_(prefs[\guiProperties] !? { prefs[\guiProperties].height });
				});
				if(prefs[\saveGuiProperties] == 1, {
					if(\UI.asClass.isNil, {
						ShutDown.add(shutDownFunc);
					}, {
						UI.registerForShutdown(shutDownFunc);
					})
				})
			};
			prefs[\initMidiOnStartUp] !? {
				if(prefs[\initMidiOnStartUp], {
					if(MIDIClient.initialized.not, {
						Class.initClassTree(MIDIClient);
						Class.initClassTree(MIDIEndPoint);
						MIDIClient.init;
						MIDIIn.connectAll;
					})
				})
			};
			prefs[\saveClassVars] !? {
				if(prefs[\saveClassVars], {
					prefs[\midiMode] !? { this.midiMode_(prefs[\midiMode]) };
					prefs[\midiResolution] !? { this.midiResolution_(prefs[\midiResolution]) };
					prefs[\midiMean] !? { this.midiMean_(prefs[\midiMean]) };
					prefs[\softWithin] !? { this.softWithin_(prefs[\softWithin]) };
					prefs[\ctrlButtonBank] !? { this.ctrlButtonBank_(prefs[\ctrlButtonBank]) };
				})
			};
			prefs[\removeResponders] !? { CVWidget.removeResponders_(prefs[\removeResponders]) };
		};

		this.shortcuts = IdentityDictionary.new;
		#all, cvWidgets, widgetStates, removeButs, tabProperties, childViews = IdentityDictionary.new!6;

		// shortcuts
		scv = (); // environment holding various variables used in shortcut-functions;,

		// "prefs[\shortcuts]: %\n".postf(prefs[\shortcuts]);
		prefs !? { prefs[\shortcuts] !? { prefs[\shortcuts][\cvcenter] !? { scPrefs = true }}};

		if(scPrefs == false, {
			scFunc =
			"// next tab
			{ CVCenter.tabs.focus(
				(CVCenter.tabs.activeTab.index+1).wrap(0, CVCenter.tabs.tabViews.size-1)
			)}";
			this.shortcuts.put(
				'arrow right',
				(func: scFunc, keyCode: KeyDownActions.keyCodes['arrow right'])
			);
			scFunc =
			"// previous tab
			{ CVCenter.tabs.focus(
				(CVCenter.tabs.activeTab.index-1).wrap(0, CVCenter.tabs.tabViews.size-1)
			)}";
			this.shortcuts.put(
				'arrow left',
				(func: scFunc, keyCode: KeyDownActions.keyCodes['arrow left'])
			);
			scFunc =
			"// select first widget
			{
				var labels = CVCenter.cvWidgets.order;
				CVCenter.cvWidgets[labels.first].parent.front.focus;
				CVCenter.cvWidgets[labels.first].label.focus;
			}";
			this.shortcuts.put(
				'alt + arrow right',
				(
					func: scFunc,
					keyCode: KeyDownActions.keyCodes['arrow right'],
					modifierQt: KeyDownActions.arrowsModifiersQt[\alt],
					modifierCocoa: KeyDownActions.arrowsModifiersCocoa[\alt]
				)
			);
			scFunc =
			"// select last widget
			{
				var labels = CVCenter.cvWidgets.order;
				CVCenter.cvWidgets[labels.last].parent.front.focus;
				CVCenter.cvWidgets[labels.last].label.focus;
			}";
			this.shortcuts.put(
				'alt + arrow left',
				(
					func: scFunc,
					keyCode: KeyDownActions.keyCodes['arrow left'],
					modifierQt: KeyDownActions.arrowsModifiersQt[\alt],
					modifierCocoa: KeyDownActions.arrowsModifiersCocoa[\alt]
				)
			);
			scFunc =
			"// OSCCommands gui
			{ OSCCommands.makeWindow }";
			this.shortcuts.put(
				\c,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$c])
			);
			scFunc =
			"// CVCenterControllersMonitor OSC
			{ CVCenterControllersMonitor(1) }";
			this.shortcuts.put(
				\o,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$o])
			);
			scFunc =
			"// set temporary shortcuts
			{ CVCenterShortcutsEditor.dialog }";
			this.shortcuts.put(
				'alt + s',
				(
					func: scFunc,
					keyCode: KeyDownActions.keyCodes[$s],
					modifierQt: KeyDownActions.modifiersQt[\alt],
					modifierCocoa: KeyDownActions.modifiersCocoa[\alt]
				)
			);
			scFunc =
			"// CVCenterControllersMonitor MIDI
			{ CVCenterControllersMonitor(0) }";
			this.shortcuts.put(
				\m,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$m])
			);
			scFunc =
			"// close CVCenterControllersMonitor
			{ if(CVCenterControllersMonitor.window.notNil.and(
				CVCenterControllersMonitor.window.isClosed.not)
			) { CVCenterControllersMonitor.window.close }}";
			this.shortcuts.put(
				\esc,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[\esc])
			);
			scFunc =
			"// History GUI: start History and open History window
			{ if(History.started === false) { History.start };
			if(CVCenter.scv.historyWin.isNil or:{
				CVCenter.scv.historyWin.isClosed
			}) {
				CVCenter.scv.historyGui = History.makeWin(
					Window.screenBounds.width-300@Window.screenBounds.height
				);
				CVCenter.scv.historyWin = CVCenter.scv.historyGui.w
			};
			if(CVCenter.scv.historyWin.notNil and:{
				CVCenter.scv.historyWin.isClosed.not
			}) { CVCenter.scv.historyWin.front }}";
			this.shortcuts.put(
				\h,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$h])
			);
			scFunc =
			"// NdefMixer
			{ if(CVCenter.scv.nDefWin.isNil or:{ CVCenter.scv.nDefWin.isClosed }) {
				CVCenter.scv.nDefGui = NdefMixer(Server.default);
				CVCenter.scv.nDefWin = CVCenter.scv.nDefGui.parent
			};
			if(CVCenter.scv.nDefWin.notNil and:{
				CVCenter.scv.nDefWin.isClosed.not
			}) {
				CVCenter.scv.nDefWin.front
			}}";
			this.shortcuts.put(
				\n,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$n])
			);
			scFunc =
			"// save setup
			{ CVCenter.saveSetup }";
			this.shortcuts.put(
				\s,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$s])
			);
			scFunc =
			"// load setup
			{ CVCenterLoadDialog.new }";
			this.shortcuts.put(
				\l,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$l])
			);
			scFunc =
			"// open the preferences dialog
			{ CVCenterPreferences.dialog }";
			this.shortcuts.put(
				\p,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$p])
			);
			scFunc =
			"// PdefAllGui
			{ if(CVCenter.scv.pDefWin.isNil or:{ CVCenter.scv.pDefWin.isClosed }) {
				CVCenter.scv.pDefGui = PdefAllGui();
				CVCenter.scv.pDefWin = CVCenter.scv.pDefGui.parent
			};
			if(CVCenter.scv.pDefWin.notNil and:{
				CVCenter.scv.pDefWin.isClosed.not
			}) {
				CVCenter.scv.pDefWin.front
			}}";
			this.shortcuts.put(
				'shift + p',
				(
					func: scFunc,
					keyCode: KeyDownActions.keyCodes[$p],
					modifierQt: KeyDownActions.modifiersQt[\shift],
					modifierCocoa: KeyDownActions.modifiersCocoa[\shift]
				)
			);
			scFunc =
			"// PdefnAllGui
			{ if(CVCenter.scv.pDefnWin.isNil or:{ CVCenter.scv.pDefnWin.isClosed }) {
				CVCenter.scv.pDefnGui = PdefnAllGui();
				CVCenter.scv.pDefnWin = CVCenter.scv.pDefnGui.parent
			};
			if(CVCenter.scv.pDefnWin.notNil and:{
				CVCenter.scv.pDefnWin.isClosed.not
			}) {
				CVCenter.scv.pDefnWin.front
			}}";
			this.shortcuts.put(
				'alt + p',
				(
					func: scFunc,
					keyCode: KeyDownActions.keyCodes[$p],
					modifierQt: KeyDownActions.modifiersQt[\alt],
					modifierCocoa: KeyDownActions.modifiersCocoa[\alt]
				)
			);
			scFunc =
			"// TdefAllGui
			{ if(CVCenter.scv.tDefWin.isNil or:{ CVCenter.scv.tDefWin.isClosed }) {
				CVCenter.scv.tDefGui = TdefAllGui();
				CVCenter.scv.tDefWin = CVCenter.scv.tDefGui.parent
			};
			if(CVCenter.scv.tDefWin.notNil and:{
				CVCenter.scv.tDefWin.isClosed.not
			}) {
				CVCenter.scv.tDefWin.front
			}}";
			this.shortcuts.put(
				\t,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$t])
			);
			scFunc =
			"// AllGui
			{ if(\\AllGui.asClass.notNil) {
				if(CVCenter.scv.allWin.isNil or:{ CVCenter.scv.allWin.isClosed }) {
					CVCenter.scv.allGui = \\AllGui.asClass.new;
					CVCenter.scv.allWin = CVCenter.scv.allGui.parent;
				};
				if(CVCenter.scv.allWin.notNil and:{
					CVCenter.scv.allWin.isClosed.not
				}) { CVCenter.scv.allWin.front };
			}}";
			this.shortcuts.put(
				\a,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$a])
			);
			scFunc =
			"// MasterEQ
			{ if(\\MasterEQ.asClass.notNil) {
				if(CVCenter.scv.eqWin.isNil or:{ CVCenter.scv.eqWin.isClosed }){
					CVCenter.scv.eqGui = \\MasterEQ.asClass.new(
						Server.default.options.firstPrivateBus, Server.default
					);
					CVCenter.scv.eqWin = CVCenter.scv.eqGui.window;
				};
				if(CVCenter.scv.eqWin.notNil and:{
					CVCenter.scv.eqWin.isClosed.not
				}) { CVCenter.scv.eqWin.front };
			}}";
			this.shortcuts.put(
				\e,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$e])
			);
			(0..9).do({ |i|
				scFunc =
				"// focus tab "++i++"
				{ CVCenter.tabs.tabViews["++i++"] !? { CVCenter.tabs.focus("++i++") }}";
				this.shortcuts.put(
					i.asSymbol,
					(func: scFunc, keyCode: KeyDownActions.keyCodes[i.asString[0]])
				);
			});
			scFunc =
			"// end History and open in new Document (Cocoa-IDE only)
			{
				History.end;
				if(Platform.ideName != \"scqt\") { History.document };
				if(CVCenter.scv.historyWin.notNil and:{
					CVCenter.scv.historyWin.isClosed.not
				}) { CVCenter.scv.historyWin.close }
			}";
			this.shortcuts.put(
				'shift + h',
				(
					func: scFunc,
					keyCode: KeyDownActions.keyCodes[$h],
					modifierQt: KeyDownActions.modifiersQt[\shift],
					modifierCocoa: KeyDownActions.modifiersCocoa[\shift]
				)
			);
			scFunc =
			"// detach the currently focused tab from the main window
			{ CVCenter.tabs.activeTab.detachTab }";
			this.shortcuts.put(
				\d,
				(func: scFunc, keyCode: KeyDownActions.keyCodes[$d])
			);
		}, {
			this.shortcuts = prefs[\shortcuts][\cvcenter];
		});
	}

	*new { |cvs...setUpArgs|
		var r, g, b;

		if(all.size == 0, {
			r = g = b = (0.6, 0.65 .. 0.75);
			colors = List();

			if(setUpArgs.size > 0, {
				this.prSetup(setUpArgs);
			});

			r.do({ |red|
				g.do({ |green|
					b.do({ |blue|
						colors.add(Color(red, green, blue));
					})
				})
			});

			nextColor = Pxrand(colors, inf).asStream;

			if(cvs.isNil, {
				nextCVKey = 1;
			}, {
				if(cvs.isKindOf(Dictionary).not and:{
					cvs.isKindOf(IdentityDictionary).not and:{
						cvs.isKindOf(Event).not
					}
				}, {
					Error("Arguments for CVCenter have to be either a Dictionary, an IdentityDictionary or an Event.").throw;
				}, {
					cvs.keysValuesDo({ |k, v|
						if("^cv[0-9]".matchRegexp(k.asString).not, {
							all.put(k.asSymbol, v.asSpec);
						}, {
							"Your given key-name matches the reserved names for new keys in the CVCenter. Please choose a different name.".warn;
						})
					});
				})
			})
		})
	}

	*makeWindow { |tab...cvs|
		var flow;
		// var cvTabIndex, order, orderedCVs, msSize;
		var updateRoutine, lastUpdate, lastUpdateBounds, lastSetUp, lastCtrlBtnBank, removedKeys, skipJacks;
		var lastCtrlBtnsMode, swFlow;
		var allTabs, thisTabLabel;
		var prefBut, saveBut, loadBut, shortcutsBut, activateGlobalShortcuts;
		var tmp, doMakeWdgt;
		// var nDefGui, pDefGui, pDefnGui, tDefGui, allGui, historyGui, eqGui;
		var prefs, newPrefs;
		var buildCheckbox;
		// TabbedView2 specific

		// "adding tab within *makeWindow: %\n".postf(tab);

		// function for building cross-platform checkboxes
		buildCheckbox = { |view, active, action|
			var cBox;
			if(GUI.id === \cocoa, {
				cBox = Button(view, 15@15)
					.states_([
						["", Color.white, Color.white],
						["X", Color.black, Color.white],
					])
					.font_(Font(Font.available("Arial Black") ? Font.defaultSansFace, 10, true))
				;
				if(active, { cBox.value_(1) }, { cBox.value_(0) });
			}, {
				cBox = \CheckBox.asClass.new(view, 15@15).value_(active);
			});
			cBox.action_(action);
		};


		tab !? { thisTabLabel = tab };
		cvs !? { this.put(*cvs) };
		prefs = CVCenterPreferences.readPreferences;

		this.guix ?? { this.guix_(prefs !? { prefs[\guiProperties] !? { prefs[\guiProperties].left }} ?? { 0 }) };
		this.guiy ?? { this.guiy_(prefs !? { prefs[\guiProperties] !? { prefs[\guiProperties].top }} ?? { 0 }) };
		this.guiwidth ?? { this.guiwidth_(prefs !? { prefs[\guiProperties] !? { prefs[\guiProperties].width }} ?? { 500 }) };
		this.guiheight ?? { this.guiheight_(prefs !? { prefs[\guiProperties] !? { prefs[\guiProperties].height }} ?? { 265 }) };

		if(window.isNil or:{ window.isClosed }, {
			window = Window("CVCenter", Rect(this.guix, this.guiy, this.guiwidth, this.guiheight)).alwaysOnTop_(alwaysOnTop).acceptsMouseOver_(true);
			// windowStates.put(window, true);
			// window.toFrontAction_({
			// 	windowStates[window] = true;
			// 	"main window focused: %\n".postf([windowStates[window], windowStates]);
			// }).endFrontAction_({
			// 	"window.endFrontAction triggered".postln;
			// 	windowStates[window] = false;
			// });
			if(Quarks.isInstalled("wslib") and:{ GUI.id !== \swing }, { window.background_(Color.black) });
			window.view.background_(Color.black);
			flow = FlowLayout(window.bounds.insetBy(4));
			window.view.decorator = flow;
			flow.margin_(Point(4, 0));
			flow.gap_(Point(0, 4));
			flow.shift;

			tabs = TabbedView2(window, Rect(0, 0, flow.bounds.width, flow.bounds.height-40))
				.tabCurve_(3)
				.labelPadding_(10)
				.alwaysOnTop_(alwaysOnTop)
				.resize_(5)
				.tabHeight_(15)
				.clickbox_(15)
				.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 12, true))
				.dragTabs_(true)
				.refreshAction_({ |me|
					if(tabProperties.size == me.tabViews.size, {
						me.tabViews.do({ |tab, i|
							if(tabProperties[tab.label.asSymbol].notNil, {
								tabProperties[tab.label.asSymbol].index = i
							})
						})
					})
				})
			;

			flow.shift(0, 0);

			prefPane = ScrollView(window, Rect(0, 0, flow.bounds.width, 40)).hasBorder_(false);
			prefPane.decorator = swFlow = FlowLayout(prefPane.bounds, Point(0, 0), Point(0, 0));
			prefPane.resize_(8).background_(Color.black);

			// prefPane.bounds.postln;

			prefBut = Button(prefPane, Point(70, 20))
				.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 11))
				.states_([["preferences", Color.white, Color(0.3, 0.3, 0.3)]])
				.action_({ |pb| CVCenterPreferences.dialog })
				.acceptsMouseOver_(true)
			;

			if(GUI.id !== \cocoa, {
				prefBut.toolTip_("Edit the global preferences for CVCenter (resp.\nCVWidget). Preferences will be written to disk\nand become active upon library-recompile.")
			});

			swFlow.shift(1, 0);

			saveBut = Button(prefPane, Point(70, 20))
				.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 11))
				.states_([["save setup", Color.white, Color(0.15, 0.15, 0.15)]])
				.action_({ |sb| this.saveSetup })
			;

			if(GUI.id !== \cocoa, {
				saveBut.toolTip_("Save the current setup of CVCenter,\nincluding currently active OSC-/MIDI-\nresponders and actions.")
			});

			swFlow.shift(1, 0);

			loadBut = Button(prefPane, Point(70, 20))
				.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 11))
				.states_([["load setup", Color.white, Color(0.15, 0.15, 0.15)]])
				.action_({ |pb|
					CVCenterLoadDialog.new;
				})
			;

			if(GUI.id !== \cocoa, {
				loadBut.toolTip_("Load a CVCenter-setup from disk. You\nmay load OSC-/MIDI-responders and\nactions if the corresponding checkboxes\nto the right are checked accordingly.")
			});

			swFlow.shift(1, 0);

			shortcutsBut = Button(prefPane, Point(70, 20))
				.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 11))
				.states_([["shortcuts", Color.white, Color.red]])
				.action_({ |scb|
					CVCenterShortcutsEditor.dialog;
				})
			;

			swFlow.shift(5, 2);

			activateGlobalShortcuts = buildCheckbox.(prefPane, KeyDownActions.globalShortcutsEnabled, { KeyDownActions.globalShortcutsEnabled_(activateGlobalShortcuts.value) });

			swFlow.shift(3, -2);

			StaticText(prefPane, Point(140, 20))
				.string_("enable global shortcuts")
				.stringColor_(Color.white)
				.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 11))
			;

			this.setShortcuts;

			window.onClose_({
				if(childViews.size > 0, {
					childViews.keysDo(_.close)
				});
				childViews.clear;
				// windowStates.removeAt(window);
				// tabProperties.do({ |prop| prop.nextPos_(Point(0, 0)) });
				tabProperties.clear;
				prefs !? {
					if(prefs[\saveGuiProperties] == 1, {
						newPrefs = CVCenterPreferences.readPreferences;
						if(newPrefs[\saveGuiProperties] == 1, {
							this.guix_(prefs[\guiProperties].left)
							.guiy_(prefs[\guiProperties].top)
							.guiwidth_(prefs[\guiProperties].width)
							.guiheight_(prefs[\guiProperties].height)
							;
							newPrefs.put(\guiProperties, prefs[\guiProperties]);
							CVCenterPreferences.writePreferences(
								newPrefs[\saveGuiProperties],
								newPrefs[\guiProperties],
								newPrefs[\saveClassVars],
								newPrefs[\midiMode],
								newPrefs[\midiResolution],
								newPrefs[\midiMean],
								newPrefs[\softWithin],
								newPrefs[\ctrlButtonBank],
								newPrefs[\removeResponders],
								newPrefs[\initMidiOnStartUp],
								informString: "Your CVCenter-preferences have successfully been written to disk."
							)
						})
					})
				};
				AbstractCVWidgetEditor.allEditors.pairsDo({ |editor, val|
					switch(cvWidgets[editor].class,
						CVWidgetKnob, {
							val.window.close;
						},
						CVWidget2D, {
							#[lo, hi].do({ |hilo|
								val[hilo] !? { val[hilo].window.close };
							})
						},
						CVWidgetMS, {
							cvWidgets[editor].msSize.do({ |sl|
								val[sl] !? { val[sl].window.close };
							});
							cvWidgets[editor].editor.msEditor !? {
								cvWidgets[editor].editor.msEditor.window.close;
							}
						}
					)
				})
			});

			if(cvWidgets.collect({ |w| w.notNil and:{ w.isClosed.not } }).size > 0, {
				allTabs = widgetStates.collectAs(_.tabKey, Array);
			}, {
				allTabs = [];
				if(tab.notNil, {
					allTabs = allTabs.add(tab.asSymbol)
				}, { allTabs = allTabs.add(\default) })
			});

			allTabs.do({ |label|
				this.prAddTab(thisTabLabel = label)
			});

			all.pairsDo({ |key, cv|
				// [key, cv].postln;
				if((cvWidgets[key].notNil and:{ cvWidgets[key].isClosed }).or(
					cvWidgets[key].isNil
				), {
					widgetStates[key] !? {
						widgetStates[key].tabKey !? { thisTabLabel = widgetStates[key].tabKey }
					};
					if(all[key].class == Event, {
						#[lo, hi].do({ |slot|
							tmp = all[key][slot].value;
							this.prAddWidget(
								thisTabLabel,
								(key: key, slot: slot, spec: all[key][slot].spec),
								key
							);
							this.at(key)[slot].value_(tmp);
						})
					}, {
						this.prAddWidget(thisTabLabel, key: key)
					})
				})
			})
		});

		window.front;

		skipJacks = SkipJack.all.collect({ |r| r === updateRoutine });
		if(skipJacks.includes(true).not, {
			updateRoutine = SkipJack({
				lastUpdate ?? { lastUpdate = all.size };
				lastSetUp !? {
					if(this.setup != lastSetUp, {
						this.prSetup(this.setup);
					})
				};
				if(all.size != lastUpdate, {
					if(all.size > lastUpdate and:{ cvWidgets.size <= lastUpdate }, {
						this.prAddWidget;
					});
					if(all.size < lastUpdate, {
						removedKeys = cvWidgets.keys.difference(all.keys);
						removedKeys.do({ |k|
							this.removeAt(k);
						});
						([tabs.activeTab]++childViews.collect({ |view| view.tabs.keys.asArray })).flat.do({ |view| this.prRegroupWidgets(view) });
						// tmp = tabs.tabViews[0].label;
					});
					lastUpdate = all.size;
				});
				if(lastUpdateBounds.notNil and:{ window.bounds.width != lastUpdateBounds.width }, {
					this.prRegroupWidgets(tabs.activeTab);
				});
				if(childViews.size > 0, {
					childViews.pairsDo({ |child, childProps|
						// "child, childProps: %, %\n".postf(child, childProps);
						// [child.bounds, childProps.lastUpdateBounds].postln;
						if(child.bounds.width != childProps.lastUpdateBounds.width, {
							childProps.tabs.keysDo({ |tab| this.prRegroupWidgets(tab) })
						})
					})
				});
				if(window.bounds != lastUpdateBounds, {
					prefs !? {
						if(prefs[\saveGuiProperties] == 1, { prefs[\guiProperties] = window.bounds });
					};
					// prefs[\guiProperties].postln;
				});
				lastUpdateBounds = window.bounds;
				if(childViews.size > 0, {
					childViews.pairsDo({ |child, childProps|
						childProps.put(\lastUpdateBounds, child.bounds)
					})
				});
				prefs !? {
					if(prefs[\saveGuiProperties] == 1, { boundsOnShutDown = lastUpdateBounds });
				};
				lastSetUp = this.setup;
			}, 0.5, { window.isClosed }, "CVCenter-Updater");
		});
	}

	*setShortcuts {
		var modsDict, arrModsDict;

		switch(GUI.id,
			\cocoa, {
				modsDict = KeyDownActions.modifiersCocoa;
				arrModsDict = KeyDownActions.arrowsModifiersCocoa;
			},
			\qt, {
				modsDict = KeyDownActions.modifiersQt;
				arrModsDict = KeyDownActions.arrowsModifiersQt;
			}
		);

		[tabs.view, tabs.views, prefPane].flat.do({ |v|
			// reset keyDownAction - it's getting reassigned

			v.keyDownAction_(nil);

			this.shortcuts.do({ |keyDowns|
				v.keyDownAction_(
					v.keyDownAction.addFunc({ |view, char, modifiers, unicode, keycode|
					// window.view.keyDownAction_(
					// window.view.keyDownAction.addFunc({ |view, char, modifiers, unicode, keycode|
						var thisMod, thisArrMod;

						// [view.cs, char.cs, modifiers.cs, unicode.cs, keycode.cs].postln;

						switch(GUI.id,
							\cocoa, {
								thisMod = keyDowns.modifierCocoa;
								thisArrMod = keyDowns.arrowsModifierCocoa;
							},
							\qt, {
								thisMod = keyDowns.modifierQt;
								thisArrMod = keyDowns.arrowsModifierQt;
							}
						);

						case
							{ modifiers == modsDict[\none] or:{ modifiers == arrModsDict[\none] }} {
								// "no modifier: %\n".postf(modifiers);
								if(keycode == keyDowns.keyCode and:{
									thisMod.isNil and:{ thisArrMod.isNil }
								}, {
									// "thisMod: %, thisArrMod: %\n".postf(thisMod, thisArrMod);
									keyDowns.func.interpret.value(view, char, modifiers, unicode, keycode)
								});
							}
							{ modifiers != modsDict[\none] and:{ modifiers != arrModsDict[\none] }} {
								// "some modifier: %\n".postf(modifiers);
								if(keycode == keyDowns.keyCode and:{
									(modifiers == thisArrMod).or(modifiers == thisMod)
								}, {
									// "thisMod: %, thisArrMod: %\n".postf(thisMod, thisArrMod);
									keyDowns.func.interpret.value(view, char, modifiers, unicode, keycode)
								})
							}
						;
					})
				)
			})
		})
	}

	*prAddTab { |label|
		var labelColor, unfocusedColor;
		var modsDict, arrModsDict;
		var thisTab, thisTabLabel, thisIndex;
		var cachedView, oldChildView;

		if(label.notNil, { thisTabLabel = label.asSymbol }, {
			Error("*prAddTab has been called without providing a label for the tab").throw;
		});

		switch(GUI.id,
			\cocoa, {
				modsDict = KeyDownActions.modifiersCocoa;
				arrModsDict = KeyDownActions.arrowsModifiersCocoa;
			},
			\qt, {
				modsDict = KeyDownActions.modifiersQt;
				arrModsDict = KeyDownActions.arrowsModifiersQt;
			}
		);

		if(tabProperties[thisTabLabel].notNil, {
			labelColor = tabProperties[thisTabLabel].tabColor;
		}, {
			labelColor = nextColor.next;
		});
		unfocusedColor = labelColor.copy.alpha_(0.3);

		tabProperties[thisTabLabel] ?? {
			thisTab = tabs.add(thisTabLabel, scroll: true)
				.focusAction_({ |tab|
					this.prRegroupWidgets(tab)
				})
				.useDetachIcon_(true)
				.background_(Color.black)
				.labelColor_(labelColor)
				.unfocusedColor_(unfocusedColor)
				.stringColor_(Color.white)
				.stringFocusedColor_(Color.black)
				.onChangeParent_({ |view|
					childViews[view.parent.parent] !? {
						oldChildView = childViews[view.parent.parent][\tabs][view];
					};
					if(tabs.tabViews.includes(view), {
						cachedView = (widgets: this.widgetsAtTab(thisTabLabel));
					}, {
						childViews.do({ |child|
							cachedView = child[\tabs][view];
							child[\tabs].removeAt(view);
						});
					});
					this.shortcuts.do({ |keyDowns|
						// "onChangeParent view: %\n".postf(view.parent.parent);
						view.keyDownAction_(
							view.keyDownAction.addFunc({ |view, char, modifiers, unicode, keycode|
								var thisMod, thisArrMod;
								thisMod = keyDowns.modifierQt;
								thisArrMod = keyDowns.arrowsModifierQt;

								case
									{ modifiers == modsDict[\none] or:{ modifiers == arrModsDict[\none] }} {
										// "no modifier".postln;
										if(keycode == keyDowns.keyCode and:{
											thisMod.isNil and:{ thisArrMod.isNil }
										}, { keyDowns.func.interpret.value(view, char, modifiers, unicode, keycode) });
									}
									{ modifiers != modsDict[\none] and:{ modifiers != arrModsDict[\none] }} {
										// "some modifier...".postln;
										if(keycode == keyDowns.keyCode and:{
											(modifiers == thisArrMod).or(modifiers == thisMod)
										}, { keyDowns.func.interpret.value(view, char, modifiers, unicode, keycode) })
									}
								;
							})
						)
					})
				})
				.onAfterChangeParent_({ |view|
					view.tabbedView.window !? {
						view.tabbedView.window.background_(Color.black).alwaysOnTop_(alwaysOnTop);
					};
					cachedView !? {
						if(tabs.tabViews.includes(view).not, {
							if(childViews[view.parent.parent].isNil, {
								childViews.put(view.parent.parent, ());
							});
							childViews[view.parent.parent][\tabs] ?? {
								childViews[view.parent.parent].put(\tabs, ());
							};
							childViews[view.parent.parent][\tabs].put(view, cachedView);
						});
						window.name_("CVCenter: "++tabs.tabViews.collect(_.label));
						childViews.pairsDo({ |child, childProps|
							child.name_("CVCenter: "++childProps[\tabs].keys.collectAs({ |tab| tab.label }, Array));
						});
					};
					childViews.pairsDo({ |child, childProps| if(childProps.tabs.size < 1, { childViews.removeAt(child) }) });
				})
			;

			tabs.labelPadding_(10).refresh;

			thisTab.view.hasBorder_(false);

			this.window.name_("CVCenter: "++tabs.tabViews.collectAs(_.label, Array));

			this.shortcuts.do({ |keyDowns|
				thisTab.keyDownAction_(
					thisTab.keyDownAction.add({ |view, char, modifiers, unicode, keycode|
						var thisMod, thisArrMod;

						switch(GUI.id,
							\cocoa, {
								thisMod = keyDowns.modifierCocoa;
								thisArrMod = keyDowns.arrowsModifierCocoa;
							},
							\qt, {
								thisMod = keyDowns.modifierQt;
								thisArrMod = keyDowns.arrowsModifierQt;
							}
						);

						case
						{ modifiers == modsDict[\none] or:{ modifiers == arrModsDict[\none] }} {
							// "no modifier".postln;
							if(keycode == keyDowns.keyCode and:{
								thisMod.isNil and:{ thisArrMod.isNil }
							}, { keyDowns.func.interpret.value(view, char, modifiers, unicode, keycode) });
						}
						{ modifiers != modsDict[\none] and:{ modifiers != arrModsDict[\none] }} {
							// "some modifier...".postln;
							if(keycode == keyDowns.keyCode and:{
								(modifiers == thisArrMod).or(modifiers == thisMod)
							}, { keyDowns.func.interpret.value(view, char, modifiers, unicode, keycode) })
						}
						;
					})
				)
			});
			tabProperties[thisTabLabel] ?? {
				tabProperties.put(thisTabLabel, (nextPos: Point(0, 0), index: tabProperties.size, tabColor: labelColor, detached: false));
			};
			thisTab.focus;
			^thisTab;
		}
	}

	*prAddWidget { |tab, widget2DKey, key|
		var allCVKeys, widgetKeys, thisKeys;
		var rowwidth, colcount;
		var cvTabIndex, tabLabels;
		var labelColor, unfocusedColor;
		var cvcArgs, btnColor;
		var msSize, tmp;
		var allTabs;
		var thisTab, thisTabLabel, thisTabColor, thisNextPos;
		var modsDict, arrModsDict;

		// "prAddWidget called: %, %, %\n".postf(tab, widget2DKey, key);

		if(tabProperties.notNil, {
			allTabs = (tabs.tabViews++childViews.collect({ |view| view.tabs.keys.asArray })).flat;
		}, { allTabs = [] });

		// "allTabs: %\n".postf(allTabs);

		if(tab.notNil, { thisTabLabel = tab.asSymbol }, {
			if(tabs.activeTab.notNil, { thisTabLabel = tabs.activeTab.label }, { thisTabLabel = \default });
		});

		// "tabProperties: %\n".postf(tabProperties);

		thisTab = allTabs.detect({ |ttab| ttab.label.asSymbol == thisTabLabel }) ?? {
			thisTab = this.prAddTab(thisTabLabel);
		};
		cvTabIndex = tabProperties[thisTabLabel][\index];
		thisNextPos = tabProperties[thisTabLabel].nextPos;

		rowheight = widgetheight+1+15; // add a small gap between rows

		if(cvWidgets[key].notNil, {
			if(cvWidgets[key].midiOscEnv.notNil, {
				cvcArgs = (midiOscEnv: cvWidgets[key].midiOscEnv);
			}, {
				cvcArgs = true;
			});
		}, { cvcArgs = true });

		// "cvcArgs: %\n".postf(cvcArgs);

		case
			{ all[key].class === Event and:{
				all[key].keys.includesAny(#[lo, hi])
			}} {
				tmp = (
					setup: (
						lo: (
							midiMode: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiMode(\lo) }, { this.midiMode }),
							midiResolution: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiResolution(\lo) }, { this.midiResolution }),
							midiMean: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiMean(\lo) }, { this.midiMean }),
							ctrlButtonBank: if(cvWidgets[key].notNil, { cvWidgets[key].getCtrlButtonBank(\lo) }, { this.ctrlButtonBank }),
							softWithin: if(cvWidgets[key].notNil, { cvWidgets[key].getSoftWithin(\lo) }, { this.softWithin }),
							calibrate: if(cvWidgets[key].notNil, { cvWidgets[key].getCalibrate(\lo) }, { true })
						),
						hi: (
							midiMode: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiMode(\hi) }, { this.midiMode }),
							midiResolution: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiResolution(\hi) }, { this.midiResolution }),
							midiMean: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiMean(\hi) }, { this.midiMean }),
							ctrlButtonBank: if(cvWidgets[key].notNil, { cvWidgets[key].getCtrlButtonBank(\hi) }, { this.ctrlButtonBank }),
							softWithin: if(cvWidgets[key].notNil, { cvWidgets[key].getSoftWithin(\hi) }, { this.softWithin }),
							calibrate: if(cvWidgets[key].notNil, { cvWidgets[key].getCalibrate(\hi) }, { true })
						),
					),
					wdgtActions: cvWidgets[key] !? { cvWidgets[key].wdgtActions }
				);
				if(cvWidgets[key].isNil or:{ cvWidgets[key].isClosed }, {
					cvWidgets[key] = CVWidget2D(
						thisTab,
						(lo: all[key].lo, hi: all[key].hi),
						key,
						Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 105, widgetheight),
						setup: tmp.setup,
						controllersAndModels: cvWidgets[key] !? {
							(lo: cvWidgets[key].wdgtControllersAndModels.lo, hi: cvWidgets[key].wdgtControllersAndModels.hi)
						},
						cvcGui: cvcArgs
					);
					removeButs.put(key,
						Button(thisTab, Rect(thisNextPos.x, thisNextPos.y+widgetheight, widgetwidth, 15))
							.states_([["remove", Color.white, Color(0.0, 0.15)]])
							.action_({ |b| this.removeAt(key) })
							.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 10))
						;
					);
					if(widgetStates[key].isNil, {
						widgetStates.put(key, (tabIndex: cvTabIndex, tabKey: thisTabLabel));
					}, {
						widgetStates[key].tabIndex = cvTabIndex;
						widgetStates[key].tabKey = thisTabLabel;
					});
					cvWidgets[key].background_(tabProperties[thisTabLabel].tabColor);
				});
				tmp.wdgtActions !? { cvWidgets[key].wdgtActions = tmp.wdgtActions };
			}
			{ #[minval, maxval, step, default].select({ |prop| all[key].spec.perform(prop).isArray }).size > 0} {
				msSize = #[minval, maxval, step, default].collect({ |prop| all[key].spec.perform(prop).size }).maxItem;
				tmp = (
					setup: msSize.collect({ |sl|
						(
							midiMode: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiMode(sl) }, { this.midiMode }),
							midiResolution: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiResolution(sl) }, { this.midiResolution }),
							midiMean: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiMean(sl) }, { this.midiMean }),
							ctrlButtonBank: if(cvWidgets[key].notNil, { cvWidgets[key].getCtrlButtonBank(sl) }, {this.ctrlButtonBank }),
							softWithin: if(cvWidgets[key].notNil, { cvWidgets[key].getSoftWithin(sl) }, { this.softWithin }),
							calibrate: if(cvWidgets[key].notNil, { cvWidgets[key].getCalibrate(sl) }, { true })
						)
					}),
					wdgtActions: cvWidgets[key] !? { cvWidgets[key].wdgtActions }
				);

				if(msSize <= numMsSlotsPerColumn, { widgetwidth = 106 }, {
					widgetwidth = (52*(msSize/numMsSlotsPerColumn).ceil)+1
				});

				if(cvWidgets[key].isNil or:{ cvWidgets[key].isClosed }, {
					cvWidgets[key] = CVWidgetMS(
						thisTab,
						all[key],
						key,
						Rect(thisNextPos.x, thisNextPos.y, widgetwidth, widgetheight),
						setup: tmp.setup,
						controllersAndModels: cvWidgets[key] !? { cvWidgets[key].wdgtControllersAndModels },
						cvcGui: cvcArgs
					);
					removeButs.put(key,
						Button(thisTab, Rect(thisNextPos.x, thisNextPos.y+widgetheight, widgetwidth, 15))
							.states_([["remove", Color.white, Color(0.0, 0.15)]])
							.action_({ |b| this.removeAt(key) })
							.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 10))
						;
					);
					if(widgetStates[key].isNil, {
						widgetStates.put(key, (tabIndex: cvTabIndex, tabKey: thisTabLabel));
					}, {
						widgetStates[key].tabIndex = cvTabIndex;
						widgetStates[key].tabKey = thisTabLabel;
					});
					cvWidgets[key].background_(tabProperties[thisTabLabel].tabColor);
				});
				tmp.wdgtActions !? { cvWidgets[key].wdgtActions = tmp.wdgtActions };
			}
			{
				tmp = (
					setup: (
						midiMode: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiMode }, { this.midiMode }),
						midiResolution: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiResolution }, { this.midiResolution }),
						midiMean: if(cvWidgets[key].notNil, { cvWidgets[key].getMidiMean }, { this.midiMean }),
						ctrlButtonBank: if(cvWidgets[key].notNil, { cvWidgets[key].getCtrlButtonBank }, { this.ctrlButtonBank }),
						softWithin: if(cvWidgets[key].notNil, { cvWidgets[key].getSoftWithin }, { this.softWithin }),
						calibrate: if(cvWidgets[key].notNil, { cvWidgets[key].getCalibrate }, { true }),
					),
					wdgtActions: cvWidgets[key] !? { cvWidgets[key].wdgtActions }
				);
				if(cvWidgets[key].isNil or:{ cvWidgets[key].isClosed }, {
					cvWidgets[key] = CVWidgetKnob(
						thisTab,
						all[key],
						key,
						Rect(thisNextPos.x, thisNextPos.y, widgetwidth = 52, widgetheight),
						setup: tmp.setup,
						controllersAndModels: cvWidgets[key] !? { cvWidgets[key].wdgtControllersAndModels },
						cvcGui: cvcArgs
					);
					removeButs.put(key,
						Button(thisTab, Rect(thisNextPos.x, thisNextPos.y+widgetheight, widgetwidth, 15))
							.states_([["remove", Color.white, Color(0.0, 0.15)]])
							.action_({ |b| this.removeAt(key) })
							.font_(Font(Font.available("Arial") ? Font.defaultSansFace, 10))
						;
					);
					if(widgetStates[key].isNil, {
						widgetStates.put(key, (tabIndex: cvTabIndex, tabKey: thisTabLabel));
					}, {
						widgetStates[key].tabIndex = cvTabIndex;
						widgetStates[key].tabKey = thisTabLabel;
					});
					cvWidgets[key].background_(tabProperties[thisTabLabel].tabColor);
				});
				tmp.wdgtActions !? { cvWidgets[key].wdgtActions = tmp.wdgtActions };
			}
		;

		switch(cvWidgets[key].class,
			CVWidgetKnob, {
				cvWidgets[key].wdgtControllersAndModels.midiDisplay.model.value_(
					cvWidgets[key].wdgtControllersAndModels.midiDisplay.model.value
				).changedKeys(cvWidgets[key].synchKeys);
				cvWidgets[key].wdgtControllersAndModels.oscDisplay.model.value_(
					cvWidgets[key].wdgtControllersAndModels.oscDisplay.model.value
				).changedKeys(cvWidgets[key].synchKeys);
				cvWidgets[key].wdgtControllersAndModels.actions.model.value_((
					numActions: cvWidgets[key].wdgtActions.size,
					activeActions: cvWidgets[key].wdgtActions.select({ |v| v.asArray[0][1] == true }).size
				)).changedKeys(cvWidgets[key].synchKeys);
			},
			CVWidget2D, {
				#[lo, hi].do({ |hilo|
					cvWidgets[key].wdgtControllersAndModels[hilo].midiDisplay.model.value_(
						cvWidgets[key].wdgtControllersAndModels[hilo].midiDisplay.model.value
					).changedKeys(cvWidgets[key].synchKeys);
					cvWidgets[key].wdgtControllersAndModels[hilo].oscDisplay.model.value_(
						cvWidgets[key].wdgtControllersAndModels[hilo].oscDisplay.model.value
					).changedKeys(cvWidgets[key].synchKeys);
					cvWidgets[key].wdgtControllersAndModels[hilo].actions.model.value_((
						numActions: cvWidgets[key].wdgtActions[hilo].size,
						activeActions: cvWidgets[key].wdgtActions[hilo].select({ |v| v.asArray[0][1] == true }).size
					)).changedKeys(cvWidgets[key].synchKeys);
				})
			},
			CVWidgetMS, {
				cvWidgets[key].msSize.do({ |sl|
					cvWidgets[key].wdgtControllersAndModels.slots[sl].midiDisplay.model.value_(
						cvWidgets[key].wdgtControllersAndModels.slots[sl].midiDisplay.model.value
					).changedKeys(cvWidgets[key].synchKeys);
					cvWidgets[key].wdgtControllersAndModels.slots[sl].oscDisplay.model.value_(
						cvWidgets[key].wdgtControllersAndModels.slots[sl].oscDisplay.model.value
					).changedKeys(cvWidgets[key].synchKeys);
				});
				cvWidgets[key].wdgtControllersAndModels.actions.model.value_((
					numActions: cvWidgets[key].wdgtActions.size,
					activeActions: cvWidgets[key].wdgtActions.select({ |v| v.asArray[0][1] == true }).size
				)).changedKeys(cvWidgets[key].synchKeys);
			}
		);

		colwidth = widgetwidth+1; // add a small gap between widgets
		rowwidth = thisTab.bounds.width-15;
		if(thisNextPos.x+colwidth >= (rowwidth-colwidth-15), {
			// jump to next row
			thisNextPos = tabProperties[thisTabLabel].nextPos; //old
		}, {
			// add next widget to the right
			tabProperties[thisTabLabel].nextPos = thisNextPos = thisNextPos.x+colwidth@(thisNextPos.y);
		});

		widget2DKey !? {
			cvWidgets[widget2DKey.key].setSpec(widget2DKey.spec, widget2DKey.slot);
		};

		tabs.activeTab !? {
			if(tabs.activeTab.index == cvTabIndex, {
				// "tabs.activeTab.index == cvTabIndex".postln;
				this.prRegroupWidgets(tabs.activeTab)
			}/*, {
				"tabs.activeTab.index != cvTabIndex".postln;
				tabs.focus(cvTabIndex)
			}*/);
		};
		window.front;
	}

	*put { |...args|
		var inputArgs, overwrite=false, tmp;
		inputArgs = args;
		if(inputArgs.size.odd, {
			overwrite = inputArgs.pop;
			if(overwrite.isKindOf(Boolean).not, {
				overwrite = nil;
			});
		});
		this.new;
		inputArgs.pairsDo({ |key, cv|
			if(cv.isKindOf(CV).not and:{ cv.isKindOf(Array).not }, {
				Error("CVCenter expects a single CV or an array of CVs as input!").throw;			});
			[cv].flat.do({ |cv|
				if(cv.isKindOf(CV).not, {
					Error("The value provided for key '"++key.asString++"' doesn't appear to be a CV.\nPlease choose a valid input!").throw;
				})
			});
			if(cv.isKindOf(Array) and:{ cv.size == 2 }, {
				tmp = cv.copy;
				#[lo, hi].do({ |key, i|
					cv.isKindOf(Event).not.if { cv = () };
					cv.put(key, tmp[i]);
				})
			});

			if(overwrite, {
				all.put(key.asSymbol, cv);
			}, {
				if(all.matchAt(key.asSymbol).isNil, {
					all.put(key.asSymbol, cv);
				}, {
					("There is already a CV stored under the name '"++key.asString++"'. \nPlease choose a different key-name!").warn;
				})
			})
		})
	}

	*removeAt { |key|
		var lastVal, thisKey, thisTabIndex, thisTabKey;
		thisKey = key.asSymbol;
		thisTabKey = widgetStates[thisKey].tabKey;
		all.removeAt(thisKey);
		cvWidgets[thisKey].class.switch(
			CVWidgetKnob, {
				if(cvWidgets[thisKey].editor.notNil and:{ cvWidgets[thisKey].editor.isClosed.not }, {
					cvWidgets[thisKey].editor.close;
				});
				cvWidgets[thisKey].midiOscEnv.cc !? {
					cvWidgets[thisKey].midiOscEnv.cc.remove;
					cvWidgets[thisKey].midiOscEnv.cc = nil;
				};
				cvWidgets[thisKey].midiOscEnv.oscResponder !? {
					cvWidgets[thisKey].midiOscEnv.oscResponder.remove;
					cvWidgets[thisKey].midiOscEnv.oscResponder = nil;
				};
			},
			CVWidget2D, {
				#[lo, hi].do({ |hilo|
					if(cvWidgets[thisKey].editor[hilo].notNil and:{ cvWidgets[thisKey].editor[hilo].isClosed.not }, {
						cvWidgets[thisKey].editor[hilo].close;
					});
					cvWidgets[thisKey].midiOscEnv[hilo].cc !? {
						cvWidgets[thisKey].midiOscEnv[hilo].cc.remove;
						cvWidgets[thisKey].midiOscEnv[hilo].cc = nil;
					};
					cvWidgets[thisKey].midiOscEnv[hilo].oscResponder !? {
						cvWidgets[thisKey].midiOscEnv[hilo].oscResponder.remove;
						cvWidgets[thisKey].midiOscEnv[hilo].oscResponder = nil;
					}
				})
			},
			CVWidgetMS, {
				if(cvWidgets[thisKey].editor.msEditor.notNil and:{
					cvWidgets[thisKey].editor.msEditor.isClosed.not
				}, {
					cvWidgets[thisKey].editor.msEditor.close;
				});
				cvWidgets[thisKey].msSize.do({ |sl|
					if(cvWidgets[thisKey].editor.editors[sl].notNil and:{
						cvWidgets[thisKey].editor.editors[sl].isClosed.not
					}, {
						cvWidgets[thisKey].editor.editors[sl].close
					});
					cvWidgets[thisKey].midiOscEnv[sl].cc !? {
						cvWidgets[thisKey].midiOscEnv[sl].cc.remove;
						cvWidgets[thisKey].midiOscEnv[sl].cc = nil;
					};
					cvWidgets[thisKey].midiOscEnv[sl].oscResponder !? {
						cvWidgets[thisKey].midiOscEnv[sl].oscResponder.remove;
						cvWidgets[thisKey].midiOscEnv[sl].oscResponder = nil;
					}
				})
			}
		);
		cvWidgets[thisKey].remove;
		cvWidgets.removeAt(key);
		removeButs[thisKey].remove;
		removeButs.removeAt(thisKey);

		if(window.notNil and:{
			window.isClosed.not
		}, {
			if(this.widgetsAtTab(widgetStates[thisKey][\tabKey]).size == 0, {
				this.prRemoveTab(thisTabKey);
			})
		});

		if(this.widgetsAtTab(widgetStates[thisKey][\tabKey]).size == 0, {
			if(tabProperties.size > 1, {
				widgetStates.do({ |ws|
					if(ws.tabIndex > widgetStates[thisKey].tabIndex, { ws.tabIndex = ws.tabIndex-1 });
				})
			})
		});

		widgetStates.removeAt(thisKey);
	}

	*removeAll { |...keys|
		if(keys.size < 1, {
			all.keys.do(this.removeAt(_));
		}, {
			keys.do(this.removeAt(_));
		});
	}

	*removeAtTab { |label|
		var wdgts;
		wdgts = this.widgetsAtTab(label);
		this.removeAll(*wdgts);
	}

	*at { |key|
		^all.at(key.asSymbol);
	}

	*add { |key, spec, value, tab, slot|
		var thisKey, thisSpec, thisVal, thisSlot, thisTab, widget2DKey;
		var specName;

		key ?? { Error("You cannot use a CV in CVCenter without providing key").throw };
		thisKey = key.asSymbol;

		// "add called with key '%' and tab '%'\n".postf(key, tab);

		// if a 2D-widget under the given key exists force the given slot to become
		// the other slot of the already existing widget
		// also prevents misbehaviour in case of bogus slots
		if(cvWidgets.notNil and:{
			cvWidgets[thisKey].notNil and:{
				cvWidgets[thisKey].class == CVWidget2D
			}
		}, {
			block { |break|
				#[lo, hi].do({ |hilo|
					if(widgetStates[thisKey].notNil and:{
						widgetStates[thisKey][hilo].isNil
					}, { break.value(thisSlot = hilo) })
				})
			}
		});

		// above test didn't apply. so we can assume no widget exists under the given key
		if(slot.notNil and:{ thisSlot.isNil }, {
			thisSlot = slot.asString.toLower.asSymbol;
			if(#[lo, hi].detect({ |sbl| sbl === thisSlot }).class !== Symbol, {
				Error("Looks like you wanted to create a multi-dimensional widget. However, the given slot-value '%' is not valid!".format(slot)).throw;
			})
		});

		// "thisSlot: %\n".postf(thisSlot);

		// CVWidgetMS
		if(spec.isArray.not, { thisSpec = spec.asSpec }, {
			if(spec.select({ |sp| sp.asSpec.class == ControlSpec }).size > 0, {
				thisSpec = ControlSpec(
					spec.collect({ |sp| sp.asSpec.minval }),
					spec.collect({ |sp| sp.asSpec.maxval }),
					spec[0].asSpec.warp,
					spec.collect({ |sp| sp.asSpec.step }),
					spec.collect({ |sp| sp.asSpec.default })
				);
				if(thisSpec.hasZeroCrossing, { thisSpec.warp_(\lin) });
				if(spec.asBag.contents.size == 1, {
					if((specName = Spec.specs.findKeyForValue(spec[0].asSpec)).notNil, {
						Spec.add((specName++"_"++spec.size).asSymbol, thisSpec);
					})
				})
			})
		});

		case
			{ tab.notNil } { thisTab = tab }
			{ tab.isNil and:{ tabs.notNil and:{ tabs.activeTab.notNil }}} { thisTab = tabs.activeTab.label }
			{ tab.isNil and:{ tabs.notNil and:{ tabs.activeTab.isNil }}} { thisTab = \default }
			{ tab.isNil and:{ tabs.isNil }} { thisTab = \default }
		;

		this.new;

		thisSlot !? {
			widgetStates[thisKey] ?? { widgetStates.put(thisKey, ()) };
			widgetStates[thisKey][thisSlot] ?? { widgetStates[thisKey].put(thisSlot, ()) };
		};

		if(value.notNil, {
			case
				{ value.isNumber } { thisVal = value }
				{ value.isArray and:{ value.select(_.isNumber).size == value.size }} { thisVal = value }
				{ thisVal = thisSpec.default }
			;
		}, {
			thisVal = thisSpec.default;
		});

		// make sure the default value is suitable for multidimensional ControlSpecs
		if([thisSpec.minval, thisSpec.maxval, thisSpec.step, thisSpec.default].select(_.isArray).size > 0, {
			thisVal = thisVal.asArray;
		});

		if(thisSlot.notNil and:{ (thisSlot === \lo).or(thisSlot === \hi) }, {
			if(cvWidgets[thisKey].notNil and:{ cvWidgets[thisKey].isClosed.not }, {
				if(widgetStates[thisKey][\hi][\made] == true, {
					cvWidgets[thisKey].setSpec(thisSpec, thisSlot);
					this.at(thisKey)[thisSlot].value_(thisVal);
				});
				if(widgetStates[thisKey][\lo][\made] == true, {
					cvWidgets[thisKey].setSpec(thisSpec, thisSlot);
					this.at(thisKey)[thisSlot].value_(thisVal);
				});
				widgetStates[thisKey][thisSlot][\made] = true;
				^all[thisKey][thisSlot];
			}, {
				all[thisKey] ?? { all.put(thisKey, (lo: CV.new, hi: CV.new)) };
				all[thisKey][thisSlot].spec_(thisSpec);
				widget2DKey = (key: thisKey, slot: thisSlot, spec: thisSpec);
				widgetStates[thisKey][thisSlot].made = true;
			})
		}, { all[thisKey] ?? { all.put(thisKey, CV.new(thisSpec, thisVal)) }});

		if(window.isNil or:{ window.isClosed }, {
			// "makeWindow: %, key: %\n".postf(thisTab, thisKey);
			this.makeWindow(thisTab);
		}, {
			// "prAddWidget: %\n".postf(thisKey);
			this.prAddWidget(thisTab, widget2DKey, thisKey);
		});

		if(slot.notNil, {
			^all[thisKey][thisSlot];
		}, {
			^all[thisKey];
		})
	}

	// spec inference - if it does not find the name, zaps all the non-alpha and looks again
	// This allows "freq 1" to resolve to \freq
	*findSpec { |name|
		var spec = name.asSymbol.asSpec;
		spec ?? { spec = name.asString.select({ |c| c.isAlpha }).asSymbol.asSpec };
		^spec;
	}

	// add a CV using spec inference
	*use { |key, spec, value, tab, slot|
		^this.add(key, spec ?? { this.findSpec(key) }, value, tab, slot)
	}

	*setup {
		^(
			midiMode: this.midiMode,
			midiResolution: this.midiResolution,
			midiMean: this.midiMean,
			ctrlButtonBank: this.ctrlButtonBank,
			softWithin: this.softWithin
		)
	}

	*alwaysOnTop_ { |bool|
		alwaysOnTop = bool.asBoolean;
		window !? { window.alwaysOnTop_(alwaysOnTop) };
		if(childViews.size > 0, {
			childViews.keys.do(_.alwaysOnTop_(alwaysOnTop));
		})
	}

	*guiMoveTo { |point|
		if(point.isKindOf(Point).not, {
			Error("guiMoveTo expects a Point in the form of e.g. Point(0, 0)").throw;
		});
		this.guix_(point.x);
		this.guiy_(point.y);
		window.bounds_(Rect(this.guix, this.guiy, this.guiwidth, this.guiheight));
	}

	*guiChangeDimensions { |point|
		if(point.isKindOf(Point).not, {
			Error("guiMoveTo expects a Point in the form of e.g. Point(0, 0)").throw;
		});
		this.guiwidth_(point.x);
		this.guiheight_(point.y);
		window.bounds_(Rect(this.guix, this.guiy, this.guiwidth, this.guiheight));
	}

	*bounds {
		^window.bounds;
	}

	*bounds_ { |rect|
		this.guix_(rect.left);
		this.guiy_(rect.top);
		this.guiwidth_(rect.width);
		this.guiheight_(rect.height);
		window.bounds_(rect);
	}

	*isClosed {
		if(this.childViews.size == 0 and:{
			this.window.isNil.or(this.window.notNil and:{ this.window.isClosed })
		}, { ^true });
		^false;
	}

	*renameTab { |oldName, newName|
		var index;
		index = tabs.tabViews.detectIndex({ |tab, i| tab.label == oldName.asString });
		tabs.tabAt(index).label = newName.asString;
		tabs.refresh;
		tabProperties.flipKeys(oldName.asSymbol, newName.asSymbol);
	}

	*addActionAt { |key, name, action, slot, active=true|
		key ?? { Error("You have to provide the CV's key in order to add an action!").throw };
		cvWidgets[key.asSymbol].addAction(name, action, slot, active);
	}

	*removeActionAt { |key, name, slot|
		key ?? { Error("You have to provide the CV's key in order to remove an action!").throw };
		cvWidgets[key.asSymbol].removeAction(name, slot);
	}

	*activateActionAt { |key, name, activate, slot|
		key ?? { Error("You have to provide the CV's key in order to activate or deactivate an action!").throw };
		cvWidgets[key.asSymbol].activateAction(name, activate, slot);
	}

	*widgetsAtTab { |label|
		var index, wdgts = [];
		all.keys.do({ |key|
			widgetStates[key] !? {
				if(widgetStates[key].tabKey == label.asSymbol, { wdgts = wdgts.add(key) });
			}
		});
		^wdgts;
	}

	*saveSetup { |path|
		var lib, successFunc;
		successFunc = { |f|
			lib = Library();
			lib.put( \all, ());
			all.pairsDo({ |k, cv|
				lib[\all].put(k, ());
				switch(cvWidgets[k].class,
					CVWidget2D, {
						lib[\all][k].wdgtClass = CVWidget2D;
						#[lo, hi].do({ |hilo|
							lib[\all][k][hilo] = (
								spec: cvWidgets[k].widgetCV[hilo].spec,
								val: cvWidgets[k].widgetCV[hilo].value,
								actions: cvWidgets[k].wdgtActions !? { cvWidgets[k].wdgtActions[hilo] },
								osc: (
									addr: cvWidgets[k].midiOscEnv[hilo].oscResponder !? {
										cvWidgets[k].midiOscEnv[hilo].oscResponder.addr
									},
									cmdName: cvWidgets[k].midiOscEnv[hilo].oscResponder !? {
										cvWidgets[k].midiOscEnv[hilo].oscResponder.cmdName
									},
									msgIndex: cvWidgets[k].midiOscEnv[hilo].oscMsgIndex,
									calibConstraints: cvWidgets[k].getOscInputConstraints(hilo),
									oscMapping: cvWidgets[k].getOscMapping(hilo)
								),
								midi: (
									src: cvWidgets[k].midiOscEnv[hilo].midisrc,
									chan: cvWidgets[k].midiOscEnv[hilo].midichan,
									num: cvWidgets[k].midiOscEnv[hilo].midiRawNum,
									midiMode: cvWidgets[k].getMidiMode(hilo),
									midiMean: cvWidgets[k].getMidiMean(hilo),
									softWithin: cvWidgets[k].getSoftWithin(hilo),
									midiResolution: cvWidgets[k].getMidiResolution(hilo),
									ctrlButtonBank: cvWidgets[k].getCtrlButtonBank(hilo)
								)
							)
						})
					},
					CVWidgetKnob, {
						lib[\all][k] = (
							spec: cvWidgets[k].widgetCV.spec,
							val: cvWidgets[k].widgetCV.value,
							actions: cvWidgets[k].wdgtActions,
							osc: (
								addr: cvWidgets[k].midiOscEnv.oscResponder !? {
									cvWidgets[k].midiOscEnv.oscResponder.addr
								},
								cmdName: cvWidgets[k].midiOscEnv.oscResponder !? {
									cvWidgets[k].midiOscEnv.oscResponder.cmdName
								},
								msgIndex: cvWidgets[k].midiOscEnv.oscMsgIndex,
								calibConstraints: cvWidgets[k].getOscInputConstraints,
								oscMapping: cvWidgets[k].getOscMapping
							),
							midi: (
								src: cvWidgets[k].midiOscEnv.midisrc,
								chan: cvWidgets[k].midiOscEnv.midichan,
								num: cvWidgets[k].midiOscEnv.midiRawNum,
								midiMode: cvWidgets[k].getMidiMode,
								midiMean: cvWidgets[k].getMidiMean,
								softWithin: cvWidgets[k].getSoftWithin,
								midiResolution: cvWidgets[k].getMidiResolution,
								ctrlButtonBank: cvWidgets[k].getCtrlButtonBank
							),
							wdgtClass: CVWidgetKnob
						)
					},
					CVWidgetMS, {
						lib[\all][k] = (
							spec: cvWidgets[k].widgetCV.spec,
							val: cvWidgets[k].widgetCV.value,
							actions: cvWidgets[k].wdgtActions,
							wdgtClass: CVWidgetMS,
							osc: ()!cvWidgets[k].msSize,
							midi: ()!cvWidgets[k].msSize
						);
						cvWidgets[k].msSize.do({ |sl|
							// osc
							cvWidgets[k].midiOscEnv[sl].oscResponder !? {
								lib[\all][k].osc[sl].addr = cvWidgets[k].midiOscEnv[sl].oscResponder.addr;
								lib[\all][k].osc[sl].cmdName = cvWidgets[k].midiOscEnv[sl].oscResponder.cmdName;
							};
							lib[\all][k].osc[sl].msgIndex = cvWidgets[k].midiOscEnv[sl].oscMsgIndex;
							lib[\all][k].osc[sl].calibConstraints = cvWidgets[k].getOscInputConstraints(sl);
							lib[\all][k].osc[sl].oscMapping = cvWidgets[k].getOscMapping(sl);
							// midi
							lib[\all][k].midi[sl].src = cvWidgets[k].midiOscEnv[sl].midisrc;
							lib[\all][k].midi[sl].chan = cvWidgets[k].midiOscEnv[sl].midichan;
							lib[\all][k].midi[sl].num = cvWidgets[k].midiOscEnv[sl].midiRawNum;
							lib[\all][k].midi[sl].midiMode = cvWidgets[k].getMidiMode(sl);
							lib[\all][k].midi[sl].midiMean = cvWidgets[k].getMidiMean(sl);
							lib[\all][k].midi[sl].softWithin = cvWidgets[k].getSoftWithin(sl);
							lib[\all][k].midi[sl].midiResolution = cvWidgets[k].getMidiResolution(sl);
							lib[\all][k].midi[sl].ctrlButtonBank = cvWidgets[k].getCtrlButtonBank(sl);
						})
					}
				);
				lib[\all][k].notes = cvWidgets[k].nameField.string;
				lib[\all][k].tabLabel = widgetStates[k].tabKey;
				// lib[\all][k].tabLabel = tabProperties.detectKey({ |prop| prop.index == widgetStates[k].tabIndex });
				// lib[\all][k].tabLabel = tabProperties[widgetStates[k].tabIndex].tabLabel;
				// if(cvWidgets[k].background != tabProperties[widgetStates[k].tabIndex].tabColor, {
				// 	lib[\all][k].background = cvWidgets[k].background;
				// });
				// if(cvWidgets[k].stringColor != Color.black, {
				// 	lib[\all][k].stringColor = cvWidgets[k].stringColor;
				// })
			});

			if(GUI.id === \cocoa, {
				lib.writeTextArchive(*f);
			}, {
				lib.writeTextArchive(f);
			});
			lib = nil;
		};
		if(path.isNil, {
			if(GUI.id !== \qt, {
				File.saveDialog(
					prompt: "Save your current setup to a file",
					defaultName: "Setup",
					successFunc: successFunc
				)
			}, { QDialog.savePanel(successFunc) })
		}, {
			successFunc.(path);
		});
	}

	*loadSetup {
		|
			path, addToExisting=false,
			autoConnectOSC=true, oscConnectToIP=true, oscRestrictToPort=false, activateCalibration=false, resetCalibration=false,
			autoConnectMIDI=true, midiConnectSrc=false, midiConnectChannel=false, midiConnectCtrl=true,
			loadActions=true, midiSrcID, oscIPAddress
		|
		var lib, successFunc;

		successFunc = { |f|
			if(GUI.id === \qt, {
				lib = Library.readTextArchive(*f);
			}, {
				lib = Library.readTextArchive(f);
			});
			if(this.childViews.size > 0, { childViews.keysDo(_.close) });

			{
				all !? {
					if(addToExisting === false, {
						this.removeAll;
					})
				};
				lib[\all].pairsDo({ |key, v|
					switch(v.wdgtClass,
						CVWidget2D, {
							#[lo, hi].do({ |hilo|
								this.add(key, v[hilo].spec, v[hilo].val, v.tabLabel, hilo);
								cvWidgets[key].setMidiMode(v[hilo].midi.midiMode, hilo)
									.setMidiMean(v[hilo].midi.midiMean, hilo)
									.setSoftWithin(v[hilo].midi.softWithin, hilo)
									.setMidiResolution(v[hilo].midi.midiResolution, hilo)
									.setCtrlButtonBank(v[hilo].midi.ctrlButtonBank, hilo)
								;
								if(loadActions, {
									v[hilo].actions !? {
										v[hilo].actions.pairsDo({ |ak, av|
											this.addActionAt(key, ak, av.asArray[0][0], hilo, av.asArray[0][1]);
										})
									}
								});
								if(autoConnectOSC, {
									if(v[hilo].osc.notNil and:{ v[hilo].osc.cmdName.notNil }, {
										cvWidgets[key].oscConnect(
											if(oscIPAddress.isNil, {
												oscConnectToIP !? {
													if(oscConnectToIP, { v[hilo].osc.addr !? { v[hilo].osc.addr.ip }})
												}
											}, {
												oscIPAddress.asString.split($:)[0]
											}),
											if(oscIPAddress.isNil, {
												oscRestrictToPort !? {
													if(oscConnectToIP and:{ oscRestrictToPort }, {
														v[hilo].osc.addr !? { v[hilo].osc.addr.port }
													})
												}
											}, {
												oscIPAddress.asString.split($:)[1]
											}),
											v[hilo].osc.cmdName,
											v[hilo].osc.msgIndex,
											hilo
										);

										cvWidgets[key].setOscMapping(v[hilo].osc.oscMapping, hilo);
										if(activateCalibration and:{ resetCalibration }, {
											cvWidgets[key].setOscInputConstraints(Point(0.0001, 0.0001), hilo);
											cvWidgets[key].wdgtControllersAndModels[hilo].oscInputRange.model.value_(
												[Point(0.0001, 0.0001)]
											).changedKeys(cvWidgets[key].synchKeys);
										}, {
											cvWidgets[key].setOscInputConstraints(
												v[hilo].osc.calibConstraints.lo @ v[hilo].osc.calibConstraints.hi, hilo
											);
											cvWidgets[key].wdgtControllersAndModels[hilo].oscInputRange.model.value_(
												[v[hilo].osc.calibConstraints.lo, v[hilo].osc.calibConstraints.hi]
											).changedKeys(cvWidgets[key].synchKeys)
										});
										if(activateCalibration, { cvWidgets[key].setCalibrate(true, hilo) });
									})
								});
								if(autoConnectMIDI, {
									if(v[hilo].midi.notNil and:{ v[hilo].midi.num.notNil }, {
										try {
											cvWidgets[key].midiConnect(
												if(midiSrcID.isNil, {
													if(midiConnectSrc, { v[hilo].midi.src })
												}, {
													midiSrcID.asInt
												}),
												if(midiConnectChannel, { v[hilo].midi.chan }),
												if(midiConnectCtrl, { v[hilo].midi.num }),
												hilo
											)
										}
									})
								})
							})
						},
						CVWidgetKnob, {
							this.add(key, v.spec, v.val, v.tabLabel);
							cvWidgets[key].setMidiMode(v.midi.midiMode)
								.setMidiMean(v.midi.midiMean)
								.setSoftWithin(v.midi.softWithin)
								.setMidiResolution(v.midi.midiResolution)
								.setCtrlButtonBank(v.midi.ctrlButtonBank)
							;
							if(loadActions, {
								v.actions !? {
									v.actions.pairsDo({ |ak, av|
										this.addActionAt(key, ak, av.asArray[0][0], active: av.asArray[0][1]);
									})
								}
							});
							if(autoConnectOSC, {
								v.osc.cmdName !? {
									cvWidgets[key].oscConnect(
										if(oscIPAddress.isNil, {
											oscConnectToIP !? {
												if(oscConnectToIP, { v.osc.addr !? { v.osc.addr.ip }})
											}
										}, {
											oscIPAddress.asString.split($:)[0]
										}),
										if(oscIPAddress.isNil, {
											oscRestrictToPort !? {
												if(oscConnectToIP and:{ oscRestrictToPort }, {
													v.osc.addr !? { v.osc.addr.port }
												})
											}
										}, {
											oscIPAddress.asString.split($:)[1]
										}),
										v.osc.cmdName,
										v.osc.msgIndex
									);
									cvWidgets[key].setOscMapping(v.osc.oscMapping);
									if(activateCalibration and:{ resetCalibration }, {
										cvWidgets[key].setOscInputConstraints(
											Point(0.0001, 0.0001)
										);
										cvWidgets[key].wdgtControllersAndModels.oscInputRange.model.value_(
											[Point(0.0001, 0.0001)]
										).changedKeys(cvWidgets[key].synchKeys);
									}, {
										cvWidgets[key].setOscInputConstraints(
											Point(v.osc.calibConstraints.lo, v.osc.calibConstraints.hi)
										);
										cvWidgets[key].wdgtControllersAndModels.oscInputRange.model.value_(
											[v.osc.calibConstraints.lo, v.osc.calibConstraints.hi]
										).changedKeys(cvWidgets[key].synchKeys);
									});
									if(activateCalibration, { cvWidgets[key].setCalibrate(true) });
								}
							});
							if(autoConnectMIDI, {
								v.midi.num !? {
									try {
										cvWidgets[key].midiConnect(
											if(midiSrcID.isNil, {
												if(midiConnectSrc, { v.midi.src })
											}, {
												midiSrcID.asInt
											}),
											if(midiConnectChannel, { v.midi.chan }),
											if(midiConnectCtrl, { v.midi.num }),
										)
									}
								}
							})
						},
						CVWidgetMS, {
							this.add(key, v.spec, v.val, v.tabLabel);
							cvWidgets[key].msSize.do({ |sl|
								cvWidgets[key].setMidiMode(v.midi[sl].midiMode, sl)
									.setMidiMean(v.midi[sl].midiMean, sl)
									.setSoftWithin(v.midi[sl].softWithin, sl)
									.setMidiResolution(v.midi[sl].midiResolution, sl)
									.setCtrlButtonBank(v.midi[sl].ctrlButtonBank, sl)
								;
							});
							if(loadActions, {
								v.actions !? {
									v.actions.pairsDo({ |ak, av|
										this.addActionAt(key, ak, av.asArray[0][0], active: av.asArray[0][1]);
									})
								}
							});
							if(autoConnectOSC, {
								cvWidgets[key].msSize.do({ |sl|
									v.osc[sl].cmdName !? {
										cvWidgets[key].oscConnect(
											if(oscIPAddress.isNil, {
												oscConnectToIP !? {
													if(oscConnectToIP, { v.osc[sl].addr !? { v.osc[sl].addr.ip }})
												}
											}, {
												oscIPAddress.asString.split($:)[0]
											}),
											if(oscIPAddress.isNil, {
												oscRestrictToPort !? {
													if(oscConnectToIP and:{ oscRestrictToPort }, {
														v.osc[sl].addr !? { v.osc[sl].addr.port }
													})
												}
											}, {
												oscIPAddress.asString.split($:)[1]
											}),
											v.osc[sl].cmdName,
											v.osc[sl].msgIndex,
											sl
										);
										cvWidgets[key].setOscMapping(v.osc[sl].oscMapping, sl);
										if(activateCalibration and:{ resetCalibration }, {
											cvWidgets[key].setOscInputConstraints(
												Point(0.0001, 0.0001), sl
											);
											cvWidgets[key].wdgtControllersAndModels.slots[sl].oscInputRange.model.value_(
												[Point(0.0001, 0.0001)]
											).changedKeys(cvWidgets[key].synchKeys);
										}, {
											cvWidgets[key].setOscInputConstraints(
												v.osc[sl].calibConstraints.lo @ v.osc[sl].calibConstraints.hi, sl
											);
											cvWidgets[key].wdgtControllersAndModels.slots[sl].oscInputRange.model.value_(
												[v.osc[sl].calibConstraints.lo, v.osc[sl].calibConstraints.hi]
											).changedKeys(cvWidgets[key].synchKeys);
										});
										if(activateCalibration, { cvWidgets[key].setCalibrate(true, sl) });
									}
								})
							});
							if(autoConnectMIDI, {
								cvWidgets[key].msSize.do({ |sl|
									v.midi[sl].num !? {
										try {
											cvWidgets[key].midiConnect(
												if(midiSrcID.isNil, {
													if(midiConnectSrc, { v.midi[sl].src })
												}, {
													midiSrcID.asInt
												}),
												if(midiConnectChannel, { v.midi[sl].chan }),
												if(midiConnectCtrl, { v.midi[sl].num }),
												slot: sl
											)
										}
									}
								})
							})
						}
					);
					cvWidgets[key].nameField.string_(v.notes);
					if(GUI.id !== \cocoa, { cvWidgets[key].label.toolTip_(v.notes) });
					if(CVCenterLoadDialog.window.notNil and:{ CVCenterLoadDialog.window.isClosed.not }, {
						CVCenterLoadDialog.window.close;
					})
				})
			}.defer(0.1)
		};

		if(path.isNil, {
			if(GUI.id === \qt, {
				QDialog.getPaths(successFunc, allowsMultiple: false);
			}, {
				File.openDialog(
					prompt: "Please choose a setup",
					successFunc: successFunc
				)
			})
		}, {
			successFunc.(path);
		})
	}

	// private Methods - not to be used directly

	*prSetup { |setupDict|
		setupDict[\midiMode] !? { this.midiMode_(setupDict[\midiMode]) };
		setupDict[\midiResolution] !? { this.midiResolution_(setupDict[\midiResolution]) };
		setupDict[\midiMean] !? { this.midiMean_(setupDict[\midiMean]) };
		this.ctrlButtonBank_(setupDict[\ctrlButtonBank]);
//		setupDict[\ctrlButtonBank] !? { this.ctrlButtonBank_(setupDict[\ctrlButtonBank]) };
		setupDict[\softWithin] !? { this.softWithin_(setupDict[\softWithin]) };
		if(window.notNil and:{ window.notClosed }, {
			cvWidgets.pairsDo({ |k, wdgt|
				switch(wdgt.class,
					CVWidgetKnob, {
						this.midiMode !? { wdgt.setMidiMode(this.midiMode) };
						this.midiResolution !? { wdgt.setMidiResolution(this.midiResolution) };
						this.midiMean !? { wdgt.setMidiMean(this.midiMean) };
						this.ctrlButtonBank !? { wdgt.setCtrlButtonBank(this.ctrlButtonBank) };
						this.softWithin !? { wdgt.setSoftWithin(this.softWithin) };
					},
					CVWidget2D, {
						#[lo, hi].do({ |hilo|
							this.midiMode !? { wdgt.setMidiMode(this.midiMode, hilo) };
							this.midiResolution !? { wdgt.setMidiResolution(this.midiResolution, hilo) };
							this.midiMean !? { wdgt.setMidiMean(this.midiMean, hilo) };
							this.ctrlButtonBank !? { wdgt.setCtrlButtonBank(this.ctrlButtonBank, hilo) };
							this.softWithin !? { wdgt.setSoftWithin(this.softWithin, hilo) };
						});
					},
					CVWidgetMS, {
						wdgt.msSize.do({ |sl|
							this.midiMode !? { wdgt.setMidiMode(this.midiMode, sl) };
							this.midiResolution !? { wdgt.setMidiResolution(this.midiResolution, sl) };
							this.midiMean !? { wdgt.setMidiMean(this.midiMean, sl) };
							this.ctrlButtonBank !? { wdgt.setCtrlButtonBank(this.ctrlButtonBank, sl) };
							this.softWithin !? { wdgt.setSoftWithin(this.softWithin, sl) };
						});
					}
				);
			})
		})
	}

	*prRegroupWidgets { |tab|
		var rowwidth, rowheight, colcount, colwidth, thisNextPos, order, orderedWidgets, orderedRemoveButs;
		var widgetwidth, widgetheight=160;
		var wdgtMaxWidth;
		var thisTabKey;

		// "*prRegroupWidgets called: %\n".postf(tab.label);

		wdgtMaxWidth = cvWidgets.collect({ |wdgt|
			if(wdgt.isClosed.not, { wdgt.widgetProps.x+1 })
		}).maxItem;

		rowheight = widgetheight+1+15;
		thisNextPos = Point(0, 0);

		tab !? {
			thisTabKey = tab.label.asSymbol;
			order = cvWidgets.order;
			orderedWidgets = cvWidgets.atAll(order);
			orderedRemoveButs = removeButs.atAll(order);
			order.do({ |k, i|
				if(cvWidgets[k].window === tab, {
					if(thisNextPos != (Point(0, 0)), {
						thisNextPos = tabProperties[thisTabKey].nextPos;
					});
					orderedWidgets[i].widgetXY_(thisNextPos);
					orderedRemoveButs[i].bounds_(Rect(
						thisNextPos.x,
						thisNextPos.y+widgetheight,
						orderedRemoveButs[i].bounds.width,
						orderedRemoveButs[i].bounds.height
					));
					colwidth = orderedWidgets[i].widgetProps.x+1; // add a small gap to the right
					rowwidth = tab.bounds.width/*-15*/;
					if(thisNextPos.x+colwidth >= (rowwidth-colwidth/*-15*/), {
						// jump to next row
						tabProperties[thisTabKey].nextPos = thisNextPos = Point(0, thisNextPos.y+rowheight);
					}, {
						// add next widget to the right
						tabProperties[thisTabKey].nextPos = thisNextPos = Point(thisNextPos.x+colwidth, thisNextPos.y);
					})
				})
			})
		}
	}

	*prRemoveTab { |key|
		var index;

		if(window.isClosed.not and:{
			tabs.tabViews.detect({ |tab| tab.label.asSymbol == key.asSymbol }).notNil
		}, {
			index = tabs.tabViews.detect({ |tab| tab.label.asSymbol == key.asSymbol }).index;
			tabs.removeAt(index)
		});
		childViews.pairsDo({ |child, childProps|
			childProps.tabs.keysDo({ |view|
				if(view.label.asSymbol == key.asSymbol, {
					child.close;
					index = tabProperties[key].index;
					tabs.removeAt(index);
				});
			})
		});
		tabProperties.do({ |prop|
			if(prop.index > index, { prop.index = prop.index-1 })
		});
		tabProperties.removeAt(key);
		widgetStates.do({ |w| if(w.tabIndex > index, { w.tabIndex = w.tabIndex-1 }) });
	}

	/* utilities */

	// key/value array way to connect CV's to a node
	// this allows a number of variants documented in the Conductor help file (see below)
	*connectToNode { |node, kvArray, environment|
		var cvcKeys = [], nodeVars, activate;

		if(node.class !== Symbol and:{ node.class !== String }, {
			nodeVars = node.getObjectVarNames(environment)
		});

		// "nodeVars: %\n".postf(nodeVars);
		forBy(1, kvArray.size - 1, 2, { |i|
			if(kvArray[i].isArray and:{ kvArray[i].isString.not }, {
				cvcKeys = cvcKeys.add(kvArray[i]);
				kvArray.put(i, kvArray[i].collect({ |key| this.at(key.asSymbol) }));
			}, {
				kvArray.put(i, this.at(kvArray[i].asSymbol));
			});
		});
		if(nodeVars.notNil and:{ nodeVars.size > 0 }, {
			nodeVars.do({ |n, i|
				if(i == 0, { activate = true }, { activate = false });
				kvArray.cvCenterBuildCVConnections(n.asString.interpret.server, n.asString.interpret.nodeID, n, cvcKeys, activate);
			})
		}, {
			if(node.class == String or:{ node.class == Symbol }, {
				kvArray.cvCenterBuildCVConnections(node.interpret.server, node.interpret.nodeID, node, cvcKeys)
			}, {
				kvArray.cvCenterBuildCVConnections(node.server, node.nodeID)
			})
		})
	}

	// not to be called directly - called internally by Synth:-cvcGui resp. NodeProxy:-cvcGui
	*finishGui { |obj, ctrlName, environment, more|
		// var interpreterVars, varNames = [], envs = [], thisSpec;
		// var pSpaces = [], proxySpace;
		var varNames, thisSpec;
		var activate = true;
		var actionName = "default";
		var wms, addActionFunc;

		// [obj, ctrlName, environment, more].postln;

		varNames = obj.getObjectVarNames(environment);

		if(more.specEnterText.notNil and:{
			more.specEnterText.interpret.asSpec.isKindOf(ControlSpec)
		}, {
			thisSpec = more.specEnterText.interpret.asSpec;
		}, {
			thisSpec = more.specSelect;
		});

//		"pSpaces: %\n".postf(pSpaces);
//		"varNames: %\n".postf(varNames);
//		"more: %\n".postf(more);

		if(more.type.notNil, {
			if(more.type === \w2d or:{ more.type === \w2dc }, {
				#[lo, hi].do({ |slot, i|
					this.add(more.cName, thisSpec, more.slots[i], more.enterTab, slot);
					if(more.type == \w2d, {
						if(slot === \lo, {
							wms = "cv.value, CVCenter.at('"++more.cName++"').hi.value";
						}, {
							wms = "CVCenter.at('"++more.cName++"').lo.value, cv.value";
						})
					});
					if(varNames.size > 0, {
						varNames.do({ |v, j|
							actionName = "default"++(j+1);
							if(j == 0, { activate = true }, { activate = false });
							switch(more.type,
								\w2d, {
									this.addActionAt(more.cName, actionName, "{ |cv|"+v+"!? {"+v++".setn('"++ctrlName++"', ["++wms++"]) }}", slot, activate);
								},
								\w2dc, {
									this.addActionAt(more.cName, actionName, "{ |cv|"+v+"!? {"+v++".set('"++more.controls[i]++"', cv.value) }}", slot, activate);
								}
							)
						})
					}, {
						switch(more.type,
							\w2d, {
								this.addActionAt(more.cName, actionName, "{ |cv| Server('"++obj.server++"').sendBundle("++obj.server.latency++", ['/n_setn', "++obj.nodeID++", '"++ctrlName++"', 2, "++wms++"]) }", slot);
							},
							\w2dc, {
								this.addActionAt(more.cName, actionName, "{ |cv| Server('"++obj.server++"').sendBundle("++obj.server.latency++", ['/n_setn', "++obj.nodeID++", '"++more.controls[i]++"', 1, cv.value]) }", slot);
							}
						)
					})
				})
			}, {
				if(more.type === \wms, {
					// "varNames: %, more: %\n".postf(varNames, more);
					this.add(more.cName, thisSpec!more.slots.size, more.slots, more.enterTab);
					if(varNames.size > 0, {
						varNames.do({ |v, j|
							actionName = "default"++(j+1);
							if(j == 0, { activate = true }, {activate = false });
							this.addActionAt(more.cName, actionName, "{ |cv|"+v+"!? {"+v++".setn('"++ctrlName++"', cv.value) }}", active: activate);
						})
					}, {
						this.addActionAt(more.cName, \default, "{ |cv| Server('"++obj.server++"').sendBundle("++obj.server.latency++", ['/n_setn', "++obj.nodeID++", '"++ctrlName++"', "++more.slots.size++", cv.value]) }");
					})
				})
			})
		}, {
			// "varNames: %, more: %\n".postf(varNames, more);
			addActionFunc = {
				if(varNames.size > 0, {
					varNames.do({ |v, j|
					// "varNames: %\n".postf(v);
						actionName = "default"++(j+1);
						if(j == 0, { activate = true }, { activate = false });
						if(more.controls.notNil and:{ more.controls.size > 1 }, {
							this.addActionAt(more.cName, actionName, "{ |cv|"+v+"!? {"+v++".set('"++ctrlName++"', cv.value) }}", active: activate);
						}, {
							wms = [];
							more.slots.size.do({ |i|
								if(this.at((more.cName.asString++(i+1)).asSymbol) === this.at((more.cName.asString++(i+1)).asSymbol), {
									wms = wms.add("cv.value");
								}, {
									wms = wms.add("CVCenter.at('"++more.cName.asString++(i+1)++"').value")
								})
							});
							this.addActionAt(more.cName.asString++(j+1), actionName, "{ |cv|"+v+"!? {"+v++".setn('"++ctrlName++"', ["++(wms.join(", "))++"]) }}", active: activate);
						})
					}, {
						this.addActionAt(more.cName, \default, "{ |cv| Server('"++obj.server++"').sendBundle("++obj.server.latency++", ['/n_setn', "++obj.nodeID++", '"++ctrlName++"', 1, cv.value]) }");
					})
				})
			};

			case
				{ more.slots.size == 1 } {
					this.add(more.cName, thisSpec, more.slots[0], more.enterTab);
					if(varNames.size > 0, {
						varNames.do({ |v, j|
							if(j == 0, { activate = true }, { activate = false });
							this.addActionAt(more.cName, \default++(j+1), "{ |cv|"+v+"!? {"+v++".set('"++ctrlName++"', cv.value) }}", active: activate);
						})
					}, {
						this.addActionAt(more.cName, \default, "{ |cv| Server('"++obj.server++"').sendBundle("++obj.server.latency++", ['/n_setn', "++obj.nodeID++", '"++ctrlName++"', 1, cv.value]) }");
					})
				}
				{ more.slots.size == 2 } {
					// more.slots.postln;
					[\Lo, \Hi].do({ |sl, k|
						this.add(more.cName++sl, thisSpec, more.slots[k], more.enterTab);
					});
					addActionFunc.value;
				}
				{ more.slots.size > 2 } {
					more.slots.size.do({ |sl|
						this.add(more.cName++sl, thisSpec, more.slots[sl], more.enterTab);
					});
					addActionFunc.value;
				}
			;
		});

		^obj;
	}

}