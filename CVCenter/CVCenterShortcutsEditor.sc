CVCenterShortcutsEditor {
	classvar <window;

	*dialog {
		var tabs, cvCenterFlow, cvWidgetFlow, cvWidgetEditorFlow, globalShortcutsFlow, keyCodesAndModsFlow;
		var cvCenterTab, cvWidgetTab, cvWidgetEditorTab, globalShortcutsTab, keyCodesAndModsTab;
		var cvCenterEditor, cvWidgetEditor, cvWidgetEditorEditor, globalShortcutsEditor, keyCodesAndModsEditor;
		var tabFont, staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg, tabsBg;
		var saveCancel, saveCancelFlow;
		var fFact, shortcuts;

		if(GUI.id === \cocoa, { fFact = 0.9 }, { fFact = 1 });

		tabFont = Font("Arial", 12, true);
		staticTextFont = Font("Arial", 12 * fFact);
		staticTextColor = Color(0.2, 0.2, 0.2);
		textFieldFont = Font("Andale Mono", 12);
		textFieldFontColor = Color.black;
		textFieldBg = Color.white;
		tabsBg = Color(0.8, 0.8, 0.8);

		if(window.isNil or:{ window.isClosed }) {
			window = Window("edit temporary shortcuts", Rect(
				Window.screenBounds.width/2-249,
				Window.screenBounds.height/2-193,
				498, 385
			));

			tabs = TabbedView2(window, Rect(0, 1, window.bounds.width, window.bounds.height-33))
				.tabHeight_(17)
				.tabCurve_(3)
				.labelColors_(Color.white!2)
				.unfocusedColors_(Color.red!2)
				.stringColors_(Color.white!2)
				.stringFocusedColors_(Color.red!2)
				.dragTabs_(false)
				.font_(tabFont)
			;

			cvCenterTab = tabs.add("CVCenter", scroll: false);
			// cvWidgetTab = tabs.add("CVWidget", scroll: false);
			cvWidgetEditorTab = tabs.add("CVWidget(MS)Editor", scroll: false);
			globalShortcutsTab = tabs.add("global shortcuts", scroll: false);
			// keyCodesAndModsTab = tabs.add("keycodes and modifiers", scroll: false);

			cvCenterEditor = KeyDownActionsEditor(
				cvCenterTab, nil, cvCenterTab.bounds, CVCenter.shortcuts, false
			);
			// cvCenterEditor.shortcutFields.collect(_.val).postln;
			// CVWidgets should go here but...
			cvWidgetEditorEditor = KeyDownActionsEditor(
				cvWidgetEditorTab, nil, cvWidgetEditorTab.bounds, AbstractCVWidgetEditor.shortcuts, false
			);
			globalShortcutsEditor = KeyDownActionsEditor(
				globalShortcutsTab, nil, globalShortcutsTab.bounds, KeyDownActions.globalShortcuts, false, false, false
			);

			// keyCodesAndModsEditor = KeyCodesEditor(
			// 	keyCodesAndModsTab, nil, false
			// );

			saveCancel = CompositeView(window, Rect(0, window.bounds.height-32, window.bounds.width, 32))
				.background_(tabsBg)
			;

			saveCancel.decorator = saveCancelFlow = FlowLayout(saveCancel.bounds, 7@2, 1@0);

			Button(saveCancel, saveCancelFlow.bounds.width/2-10@23)
				.states_([["Cancel", Color.black, Color.white]])
				.font_(Font("Arial", 14, true))
				.action_({ window.close })
			;

			Button(saveCancel, saveCancelFlow.indentedRemaining.width@23)
				.states_([["set shortcuts", Color.white, Color.red]])
				.font_(Font("Arial", 14, true))
				.action_({
					// "KeyDownActionsEditor.cachedScrollViewSC: %\n".postf(KeyDownActionsEditor.cachedScrollViewSC);
					KeyDownActionsEditor.cachedScrollViewSC !? {
						ScrollView.globalKeyDownAction_(KeyDownActionsEditor.cachedScrollViewSC);
					};
					CVCenter.shortcuts_(cvCenterEditor.result);
					AbstractCVWidgetEditor.shortcuts_(cvWidgetEditorEditor.result);
					// KeyDownActions.keyCodes_(keyCodesAndModsEditor.result);
					CVCenter.setShortcuts;
					// shortcuts = (cvcenter:  cvCenterEditor.result, cvwidgeteditor: cvWidgetEditorEditor.result);
					// 	// "shortcuts.cvcenter['fn + F1']: %\n".postf(shortcuts.cvcenter['fn + F1']);
					// 	// "cvCenterEditor.result: %\n".postf(cvCenterEditor.result);
					// 	// "cvCenterKeyCodesEditor.result: %\n".postf(cvCenterKeyCodesEditor.result);
					// 	this.writePreferences(
					// 		shortcuts: shortcuts,
					// 		globalShortcuts: globalShortcutsEditorTab.result,
					// 		keyCodesAndMods: keyCodesAndModsEditor.result(false)
					// 	);
					window.close;
				})
			;
		};
		window.front;
	}

}