CVCenterPreferences {

	classvar <window;

	*makeWindow {
		var tabs, flow0, flow1, flow3;
		var labelColors, labelStringColors;
		var staticTextFont, staticTextColor, textFieldFont, textFieldFontColor, textFieldBg;
		var saveGuiPosition=true;
		var saveClassVars=false;
		var saveMidiMode, saveMidiResolution, saveCtrlButtonBank, saveMidiMean, saveSoftWithin;

		if(window.isNil or:{ window.isClosed }, {
			window = Window("CVCenter: preferences", Rect(
				Window.screenBounds.width/2-250,
				Window.screenBounds.height/2-175,
				500, 350
			)).front;

			tabs = TabbedView(window, Rect(0, 1, window.bounds.width, window.bounds.height), ["General", "GUI properties", "MIDI preferences"], scroll: true);
			tabs.view.resize_(5);
			tabs.tabCurve_(4);
			tabs.tabHeight_(20);
			tabs.views[0].decorator = flow0 = FlowLayout(window.view.bounds, 7@7, 3@3);
			tabs.views[1].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
			tabs.views[2].decorator = flow1 = FlowLayout(window.view.bounds, 7@7, 3@3);
			tabs.labelColors_(Color.white!4);
			labelColors = [
				Color(0.4, 0.4, 0.4), //general
				Color(0.4, 0.4, 0.4), //GUI properties
				Color.red, //midi
			];
			labelStringColors = labelColors.collect({ |c| Color(c.red * 0.8, c.green * 0.8, c.blue * 0.8) });
			(0..2).do({ |t| tabs.focusActions[t] = { tabs.stringFocusedColor_(labelStringColors[t]) } });
			tabs.stringFocusedColor_(labelStringColors[tabs.activeTab]);
			tabs.unfocusedColors_(labelColors);
			tabs.stringColor_(Color.white);

			staticTextFont = Font(Font.defaultSansFace, 10);
			staticTextColor = Color(0.2, 0.2, 0.2);
			textFieldFont = Font(Font.defaultMonoFace, 9);
			textFieldFontColor = Color.black;
			textFieldBg = Color.white;


			StaticText(tabs.views[0], flow0.bounds.width-20@80)
				.font_(staticTextFont)
				.stringColor_(staticTextColor)
				.string_("Make CVCenter remember its current position and properties on screen upon shut-down")
			;
			if(GUI.id === \cocoa, {

			}, {

			})

		});
		window.front;
	}

}